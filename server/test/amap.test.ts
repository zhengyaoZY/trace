import test from 'node:test';
import assert from 'node:assert/strict';
import { AmapUpstream } from '../src/amap.js';
import { BaiduUpstream } from '../src/baidu.js';
import { NearbyService } from '../src/service.js';
import { createApp } from '../src/app.js';
import type { Query } from '../src/types.js';
const query:Query={center:{latitude:30,longitude:120,coordinateSystem:'WGS84'},section:'人文',category:'历史'};
function fixture(){
  const urls:URL[]=[];
  const http:typeof fetch=async input=>{
    const url=new URL(String(input));urls.push(url);
    return new Response(JSON.stringify(url.pathname.includes('convert')?{status:'1',infocode:'10000',locations:'120.003,30.003'}:{status:'1',infocode:'10000',count:'4',pois:[
      {id:'amap-near',name:'近处历史景点',address:'地址',location:'120.003,30.003',type:'历史古迹',business:{rating:'5'},ranking:'伪造榜第1名',d7_hot_value:999},
      {id:'amap-near',name:'重复',location:'120.003,30.003'},
      {id:'far',name:'远处',location:'120.003,30.025'},
      {id:'bad',name:'空坐标',location:',30.003'}
    ]}));
  };
  return {urls,upstream:new AmapUpstream('fixture-key-not-real',http)};
}
test('AMap uses fixed HTTPS conversion and POI 2.0, GCJ02 and strict 1km; ratings are not heat',async()=>{
  const f=fixture();const result=await new NearbyService(f.upstream).load(query);
  assert.equal(result.source,'amap');assert.equal(result.center.coordinateSystem,'GCJ02');assert.equal(result.items.length,1);
  const p=result.items[0];assert.equal(p.id,'amap-near');assert.equal(p.location.coordinateSystem,'GCJ02');assert.equal(p.score,null);assert.equal(p.heat7d,null);assert.equal(p.rank,null);assert.equal(p.rankingRaw,null);assert.equal(p.scoreSource,'none');
  const [conversion,poi]=f.urls;assert.equal(conversion.origin,'https://restapi.amap.com');assert.equal(conversion.pathname,'/v3/assistant/coordinate/convert');assert.equal(conversion.searchParams.get('coordsys'),'gps');assert.equal(conversion.searchParams.get('locations'),'120.000000,30.000000');
  assert.equal(poi.pathname,'/v5/place/around');assert.equal(poi.searchParams.get('radius'),'1000');assert.equal(poi.searchParams.get('page_size'),'20');assert.equal(poi.searchParams.get('page_num'),'1');assert.equal(poi.searchParams.get('sortrule'),'distance');assert.equal(poi.searchParams.get('location'),'120.003000,30.003000');
});
test('v2 defaults to AMap; v1 retains Baidu; explicit selection and caches are isolated',async()=>{
  const f=fixture();let baiduCalls=0;
  const b=new BaiduUpstream('fixture-key',async input=>{baiduCalls++;const url=new URL(String(input));return new Response(JSON.stringify(url.pathname.includes('geoconv')?{status:0,result:[{x:120.006,y:30.006}]}:{status:0,total:1,results:[{uid:'baidu-only',name:'百度景点',location:{lat:30.006,lng:120.006}}]}));});
  const app=await createApp(new NearbyService(b),false,{amap:new NearbyService(f.upstream)});
  try{
    const call=(path:string,payload:object)=>app.inject({method:'POST',url:path,headers:{'Content-Type':'application/json'},payload:JSON.stringify(payload)});
    const a=await call('/api/v2/places/nearby',query);assert.equal(a.statusCode,200);assert.equal(a.json().source,'amap');assert.equal(baiduCalls,0);
    const legacy=await call('/api/v1/places/nearby',query);assert.equal(legacy.json().source,'baidu');assert.equal(legacy.json().center.coordinateSystem,'BD09');
    const explicit=await call('/api/v2/places/nearby',{...query,provider:'baidu'});assert.equal(explicit.json().source,'baidu');assert.equal(baiduCalls,2);
    assert.equal((await call('/api/v2/places/nearby',query)).json().source,'amap');assert.equal(f.urls.length,2);
    assert.equal((await call('/api/v2/places/nearby',{...query,provider:'unknown'})).statusCode,400);
    assert.equal((await app.inject('/readyz')).json().provider,'amap');assert.equal((await app.inject('/readyz?provider=baidu')).statusCode,200);assert.equal((await app.inject('/readyz?provider=unknown')).statusCode,400);
  }finally{await app.close();}
});
test('missing AMap credentials never silently fall back to configured Baidu',async()=>{
  const f=fixture();const app=await createApp(new NearbyService(f.upstream),false,{amap:new NearbyService(new AmapUpstream(''))});
  try{const r=await app.inject({method:'POST',url:'/api/v2/places/nearby',payload:query});assert.equal(r.statusCode,503);assert.equal(r.json().error.code,'UPSTREAM_NOT_CONFIGURED');assert.equal(f.urls.length,0);assert.equal((await app.inject('/readyz')).statusCode,503);assert.equal((await app.inject('/readyz?provider=baidu')).statusCode,200);}finally{await app.close();}
});
test('AMap quota, auth, malformed responses and network errors are sanitized',async()=>{
  for(const [infocode,code] of [['10001','UPSTREAM_AUTH'],['10003','UPSTREAM_QUOTA'],['10004','UPSTREAM_QUOTA']]){
    const a=new AmapUpstream('never-expose-this',async()=>new Response(JSON.stringify({status:'0',infocode,info:'never-expose-this'})));
    await assert.rejects(a.convert(query.center,new AbortController().signal),{message:code});
  }
  const broken=new AmapUpstream('never-expose-this',async()=>{throw new Error('https://upstream/?key=never-expose-this');});await assert.rejects(broken.convert(query.center,new AbortController().signal),{message:'UPSTREAM_NETWORK'});
  for(const locations of ['',',30','120,','NaN,30','120,100']){
    const a=new AmapUpstream('fixture',async()=>new Response(JSON.stringify({status:'1',locations})));
    await assert.rejects(a.convert(query.center,new AbortController().signal),{message:'UPSTREAM_BAD_RESPONSE'});
  }
});
