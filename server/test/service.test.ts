import test from 'node:test';
import assert from 'node:assert/strict';
import { NearbyService } from '../src/service.js';
import { ApiError, type Point, type Query, type Upstream, type Place } from '../src/types.js';
import { distanceMeters, parseRank, scorePlaces } from '../src/metrics.js';
import { createApp } from '../src/app.js';
import { BaiduUpstream } from '../src/baidu.js';
const query: Query={center:{latitude:30,longitude:120,coordinateSystem:'WGS84'},section:'人文',category:'全部'};
const point:Point={latitude:30.006,longitude:120.006,coordinateSystem:'BD09'};
const raw=(uid:string,offset=0,ranking:unknown=null,heat:unknown=null)=>({uid,name:uid,address:'地址',location:{lat:point.latitude+offset,lng:point.longitude},detail_info:{ranking,d7_hot_value:heat,tag:'历史'}});
class Fixture implements Upstream {
  configured=true;conversions=0;pages=0;
  constructor(public data:unknown[]=[raw('a')],public total=1){}
  async convert(p: Point){assert.equal(p.coordinateSystem,'WGS84');this.conversions++;return point;}
  async page(c: Point,q: Query,page:number){assert.equal(c.coordinateSystem,'BD09');this.pages++;return {items:this.data,total:this.total};}
}
test('strict one-kilometer filtering, deduplication, invalid coordinates and official metrics',async()=>{
  const f=new Fixture([raw('a',0,'杭州景点第1名',100),raw('a'),raw('near',.001,null,20),raw('far',.02),raw('edge',999/111195.08),raw('outside',1001/111195.08),raw('bad',NaN)],7);
  const r=await new NearbyService(f,()=>0).load(query);
  assert.deepEqual(new Set(r.items.map(p=>p.id)),new Set(['a','near','edge']));assert.equal(r.items[0].score,1);assert.equal(r.selectedRankBoard,'杭州景点');assert.equal(r.expiresAt,300000);assert.equal(r.center.coordinateSystem,'BD09');
});
test('bounded pagination and 100 results',async()=>{
  const f=new Fixture([],150);f.page=async()=>{const page=f.pages++;return {items:Array.from({length:20},(_,i)=>raw(`${page}-${i}`,i*.00001)),total:150};};
  const r=await new NearbyService(f).load(query);assert.equal(f.pages,5);assert.equal(r.items.length,100);
});
test('cache expiry, refresh minimum interval and conversion reuse',async()=>{
  let now=0;const f=new Fixture();const service=new NearbyService(f,()=>now);
  const a=await service.load(query);now=20000;assert.equal(await service.load({...query,refresh:true}),a);assert.equal(f.pages,1);
  now=30000;await service.load({...query,refresh:true});assert.equal(f.pages,2);assert.equal(f.conversions,1);
  now=330001;await service.load(query);assert.equal(f.pages,3);assert.equal(f.conversions,2);
});
test('coalesces simultaneous requests and never caches failures',async()=>{
  const f=new Fixture();let resolve!:()=>void;const gate=new Promise<void>(r=>resolve=r);f.convert=async()=>{f.conversions++;await gate;return point;};
  const service=new NearbyService(f);const a=service.load(query),b=service.load(query);resolve();await Promise.all([a,b]);assert.equal(f.pages,1);
  const broken=new Fixture();broken.page=async()=>{broken.pages++;throw new ApiError('UPSTREAM_NETWORK',502);};const s=new NearbyService(broken);
  await assert.rejects(s.load(query),{code:'UPSTREAM_NETWORK'});await assert.rejects(s.load(query));assert.equal(broken.pages,2);
});
test('total timeout aborts upstream and releases in-flight request',async()=>{
  const f=new Fixture();f.convert=async()=>new Promise(()=>{});const s=new NearbyService(f,Date.now,10);
  await assert.rejects(s.load(query),{code:'UPSTREAM_TIMEOUT'});await assert.rejects(s.load(query),{code:'UPSTREAM_TIMEOUT'});
});
const place=(id:string,ranking:unknown,heat:number|null):Place=>({id,name:id,address:'',category:'历史',location:point,distanceMeters:1,rankingRaw:ranking,rank:parseRank(ranking),heat7d:heat,score:null,scoreSource:'none'});
test('rank and heat monotonicity, equal weighting and missing metrics',()=>{
  const items=[place('top','共同榜第1名',100),place('next','共同榜第2名',100),place('low','共同榜第3名',10),place('heat',null,20),place('none','unknown rank',null),place('other','其他榜第1名',null)];
  assert.equal(scorePlaces(items),'共同榜');assert.equal(items[0].score,1);assert.equal(items[1].score,.75);assert.ok(items[1].score!>items[2].score!);assert.equal(items[3].scoreSource,'heat');assert.equal(items[4].score,null);assert.equal(items[5].score,null);
  const rankOnly=[place('r','榜第2名',null)];scorePlaces(rankOnly);assert.equal(rankOnly[0].score,.5);
  const zero=[place('z',null,0)];scorePlaces(zero);assert.equal(zero[0].score,0);
  assert.equal(parseRank('第0名'),null);assert.equal(parseRank('无名次'),null);assert.equal(parseRank({position:1}),null);
  assert.equal(distanceMeters(point,point),0);
});
test('API validates schema, category combinations and does not allow arbitrary upstream input',async()=>{
  const app=await createApp(new NearbyService(new Fixture()));
  try{
    const r=await app.inject({method:'POST',url:'/api/v1/places/nearby',payload:query});assert.equal(r.statusCode,200);assert.equal(r.headers["cache-control"],"no-store");assert.equal(r.json().items.length,1);
    for(const payload of [{...query,radiusMeters:5000},{...query,category:'自驾路线'},{...query,center:{...query.center,latitude:91}},{...query,ak:'must-not-be-used'},{...query,section:'自然',category:'美食'},{...query,center:{...query.center,coordinateSystem:'BD09'}}]){
      const response=await app.inject({method:'POST',url:'/api/v1/places/nearby',payload});assert.equal(response.statusCode,400);
    }
  }finally{await app.close();}
});
test('rate limiting produces 429 while probes remain live; configuration readiness',async()=>{
  const upstream=new Fixture();upstream.configured=false;const app=await createApp(new NearbyService(upstream));
  try{
    assert.equal((await app.inject('/healthz')).statusCode,200);assert.equal((await app.inject('/readyz')).statusCode,503);
    for(let i=0;i<30;i++)assert.equal((await app.inject({method:'POST',url:'/api/v1/places/nearby',payload:query})).statusCode,503);
    const last=await app.inject({method:'POST',url:'/api/v1/places/nearby',payload:query});assert.equal(last.statusCode,429);assert.equal(last.json().error.code,'RATE_LIMITED');assert.equal((await app.inject('/healthz')).statusCode,200);
  }finally{await app.close();}
});
test('Baidu calls are fixed HTTPS endpoints, strict radius and scope, sanitized errors',async()=>{
  const urls:URL[]=[];const http:typeof fetch=async(input)=>{const u=new URL(String(input));urls.push(u);return new Response(JSON.stringify(u.pathname.includes('geoconv')?{status:0,result:[{x:120.006,y:30.006}]}:{status:0,results:[raw('a')],total:1}));};
  const upstream=new BaiduUpstream('test-only-placeholder',http);const result=await new NearbyService(upstream).load(query);assert.equal(result.items.length,1);
  assert.equal(urls[0].hostname,'api.map.baidu.com');assert.equal(urls[0].searchParams.get('from'),'1');assert.equal(urls[1].searchParams.get('radius_limit'),'true');assert.equal(urls[1].searchParams.get('coord_type'),'3');assert.equal(urls[1].searchParams.get('scope'),'2');
  for(const [status,code] of [[5,'UPSTREAM_AUTH'],[4,'UPSTREAM_QUOTA'],[302,'UPSTREAM_QUOTA']] as const){const b=new BaiduUpstream('do-not-leak',async()=>new Response(JSON.stringify({status,message:'do-not-leak'})));await assert.rejects(b.convert(query.center,new AbortController().signal),{message:code});}
  const bad=new BaiduUpstream('do-not-leak',async()=>{throw new Error('url?ak=do-not-leak');});await assert.rejects(bad.convert(query.center,new AbortController().signal),{message:'UPSTREAM_NETWORK'});
});
test('LRU caches are bounded and evicted keys query again',async()=>{
  const f=new Fixture();const s=new NearbyService(f,()=>0);
  for(let i=0;i<257;i++)await s.load({...query,center:{...query.center,latitude:30+i*.00002}});
  assert.equal(f.pages,257);await s.load(query);assert.equal(f.pages,258);
});
test('HTTP TCP endpoint runs with fixture upstream and returns the public contract',async()=>{
  const app=await createApp(new NearbyService(new Fixture([raw('network',0,'杭州榜第1名',10)])));
  const address=await app.listen({host:'127.0.0.1',port:0});
  try{
    const response=await fetch(address+'/api/v1/places/nearby',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(query)});
    const body=await response.json() as {items:Place[];expiresAt:number;fetchedAt:number};assert.equal(response.status,200);assert.equal(body.items[0].location.coordinateSystem,'BD09');assert.equal(body.items[0].score,1);assert.equal(body.expiresAt-body.fetchedAt,300000);
  }finally{await app.close();}
});
