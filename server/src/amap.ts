import { ApiError, type Point, type Query, type Upstream } from './types.js';
export class AmapUpstream implements Upstream {
  readonly source='amap' as const;readonly datum='GCJ02' as const;readonly configured:boolean;
  constructor(private readonly key:string,private readonly http:typeof fetch=fetch){this.configured=!!key;}
  private async call(path:string,params:Record<string,string>,signal:AbortSignal):Promise<any>{
    if(!this.configured)throw new ApiError('UPSTREAM_NOT_CONFIGURED',503);
    const url=new URL(path,'https://restapi.amap.com');url.search=new URLSearchParams({...params,key:this.key,output:'json'}).toString();
    let data:any;
    try{const r=await this.http(url,{redirect:'error',signal:AbortSignal.any([signal,AbortSignal.timeout(5000)])});if(!r.ok)throw new ApiError('UPSTREAM_UNAVAILABLE',502);data=await r.json();}
    catch(e){if(e instanceof ApiError)throw e;if(e instanceof SyntaxError)throw new ApiError('UPSTREAM_BAD_RESPONSE',502);if(signal.aborted||(e instanceof Error&&['TimeoutError','AbortError'].includes(e.name)))throw new ApiError('UPSTREAM_TIMEOUT',504);throw new ApiError('UPSTREAM_NETWORK',502);}
    if(!data||typeof data.status!=='string')throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    if(data.status!=='1'){
      if(['10003','10004','10020','10044'].includes(String(data.infocode)))throw new ApiError('UPSTREAM_QUOTA',503,60);
      if(String(data.infocode).startsWith('100'))throw new ApiError('UPSTREAM_AUTH',503);
      throw new ApiError('UPSTREAM_UNAVAILABLE',502);
    }
    return data;
  }
  async convert(p:Point,signal:AbortSignal):Promise<Point>{
    const r=await this.call('/v3/assistant/coordinate/convert',{locations:`${p.longitude.toFixed(6)},${p.latitude.toFixed(6)}`,coordsys:'gps'},signal);
    const parts:string[]=typeof r.locations==='string'?r.locations.split(','):[];const lon=Number(parts[0]),lat=Number(parts[1]);
    if(parts.length!==2||parts.some(v=>!v.trim())||!Number.isFinite(lat)||!Number.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    return {latitude:lat,longitude:lon,coordinateSystem:'GCJ02'};
  }
  async page(center:Point,q:Query,page:number,signal:AbortSignal){
    const types=q.section==='自然'?'110000':q.category==='美食'?'050000':q.category==='历史'?'110208|110204|140100':'110208|110204|140100|050000';
    const r=await this.call('/v5/place/around',{location:`${center.longitude.toFixed(6)},${center.latitude.toFixed(6)}`,radius:'1000',types,sortrule:'distance',page_size:'20',page_num:String(page+1)},signal);
    if(!Array.isArray(r.pois))throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    const items=r.pois.map((p:any)=>{
      const pair=typeof p.location==='string'?p.location.split(','):[];
      return {uid:p.id,name:p.name,address:typeof p.address==='string'?p.address:'',location:{lat:pair.length===2&&pair[1].trim()?Number(pair[1]):NaN,lng:pair.length===2&&pair[0].trim()?Number(pair[0]):NaN},detail_info:{tag:p.type}};
    });
    const total=Number(r.count);return {items,total:Number.isFinite(total)?total:undefined};
  }
}
