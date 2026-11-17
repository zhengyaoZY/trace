import { ApiError, type Nearby, type Place, type Point, type Query, type Upstream } from './types.js';
import { distanceMeters, heatValue, parseRank, scorePlaces } from './metrics.js';
function normalize(raw: any,center: Point,q: Query,source: 'amap'|'baidu',datum: 'BD09'|'GCJ02'): Place|null {
  if(!raw || typeof raw.uid!=='string'||!raw.uid||typeof raw.name!=='string'||!raw.name||!Number.isFinite(raw.location?.lat)||!Number.isFinite(raw.location?.lng)||Math.abs(raw.location.lat)>90||Math.abs(raw.location.lng)>180)return null;
  const location: Point={latitude:raw.location.lat,longitude:raw.location.lng,coordinateSystem:datum};
  const distance=distanceMeters(center,location);if(distance>1000)return null;
  const d=raw.detail_info??{}, tag=String(d.tag??raw.tag??'');
  const category=q.category!=='全部'?q.category:q.section==='自然'?'自然景区':/美食|餐|小吃/.test(tag)?'美食':/古迹|历史|博物馆/.test(tag)?'历史':'人文地点';
  const rankingRaw=source==='baidu'?(d.ranking??raw.ranking??null):null;
  return {id:raw.uid,name:raw.name,address:typeof raw.address==='string'?raw.address:'',category,location,distanceMeters:distance,
    rankingRaw,rank:parseRank(rankingRaw),heat7d:source==='baidu'?heatValue(d.d7_hot_value??raw.d7_hot_value):null,score:null,scoreSource:'none'};
}
export class NearbyService {
  private readonly cache=new Map<string,Nearby>();
  private readonly conversions=new Map<string,{point:Point;expires:number}>();
  private readonly pending=new Map<string,Promise<Nearby>>();
  constructor(readonly upstream: Upstream,private readonly now:()=>number=Date.now,private readonly timeoutMs=20_000){}
  async load(q: Query): Promise<Nearby> {
    if(!this.upstream.configured)throw new ApiError('UPSTREAM_NOT_CONFIGURED',503);
    const pointKey=`${q.center.latitude.toFixed(6)}/${q.center.longitude.toFixed(6)}`;
    const key=`${pointKey}/${q.section}/${q.category}`;
    const hit=this.cache.get(key), now=this.now();
    if(hit && hit.expiresAt>now && (!q.refresh||now-hit.fetchedAt<30_000)){
      this.cache.delete(key);this.cache.set(key,hit);return hit;
    }
    const inFlight=this.pending.get(key);if(inFlight)return inFlight;
    const controller=new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    const expired=new Promise<never>((_,reject)=>{timer=setTimeout(()=>{controller.abort();reject(new ApiError('UPSTREAM_TIMEOUT',504));},this.timeoutMs);});
    const work=this.query(q,pointKey,controller.signal).then(result=>{if(!controller.signal.aborted){this.cache.set(key,result);if(this.cache.size>256)this.cache.delete(this.cache.keys().next().value!);}return result;});
    const task=Promise.race([work,expired]).finally(()=>{clearTimeout(timer);this.pending.delete(key);});
    this.pending.set(key,task);return task;
  }
  private async query(q: Query,pointKey: string,signal: AbortSignal): Promise<Nearby> {
    let converted=this.conversions.get(pointKey);
    if(!converted||converted.expires<=this.now()){
      converted={point:await this.upstream.convert(q.center,signal),expires:this.now()+300_000};
      if(!signal.aborted){this.conversions.set(pointKey,converted);if(this.conversions.size>256)this.conversions.delete(this.conversions.keys().next().value!);}
    }
    if(converted.point.coordinateSystem!==(this.upstream.datum??'BD09'))throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    const center=converted.point, unique=new Map<string,Place>();
    for(let page=0;page<5;page++){
      if(signal.aborted)throw new ApiError('UPSTREAM_TIMEOUT',504);
      const result=await this.upstream.page(center,q,page,signal);
      for(const raw of result.items){const item=normalize(raw,center,q,this.upstream.source??'baidu',this.upstream.datum??'BD09');if(item&&!unique.has(item.id))unique.set(item.id,item);}
      if(!result.items.length || (result.total!==undefined&&(page+1)*20>=Math.min(100,result.total)))break;
    }
    const items=[...unique.values()].sort((a,b)=>a.distanceMeters-b.distanceMeters||a.id.localeCompare(b.id)).slice(0,100);
    const selectedRankBoard=scorePlaces(items);
    items.sort((a,b)=>(b.score??-1)-(a.score??-1)||a.distanceMeters-b.distanceMeters||a.id.localeCompare(b.id));
    const fetchedAt=this.now();
    return {items,center,radiusMeters:1000,source:this.upstream.source??'baidu',selectedRankBoard,fetchedAt,expiresAt:fetchedAt+300_000};
  }
}
