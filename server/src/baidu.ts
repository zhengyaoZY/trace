import { ApiError, type Point, type Query, type Upstream } from './types.js';
import { keywords } from './metrics.js';
export class BaiduUpstream implements Upstream {
  readonly configured: boolean;
  readonly source='baidu' as const;
  readonly datum='BD09' as const;
  constructor(private readonly ak: string,private readonly http: typeof fetch=fetch){this.configured=!!ak;}
  private async call(path: string, params: Record<string,string>, signal: AbortSignal): Promise<any> {
    if(!this.configured)throw new ApiError('UPSTREAM_NOT_CONFIGURED',503);
    const url=new URL(path,'https://api.map.baidu.com');
    url.search=new URLSearchParams({...params,ak:this.ak,output:'json'}).toString();
    let data: any;
    try {
      const response=await this.http(url,{signal:AbortSignal.any([signal,AbortSignal.timeout(5000)])});
      if(!response.ok)throw new ApiError('UPSTREAM_UNAVAILABLE',502);
      data=await response.json();
    } catch(e) {
      if(e instanceof ApiError)throw e;
      if(e instanceof SyntaxError)throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
      if(signal.aborted||(e instanceof Error&&['TimeoutError','AbortError'].includes(e.name)))throw new ApiError('UPSTREAM_TIMEOUT',504);
      // Never log provider exception/cause or URLs, which may contain the AK.
      throw new ApiError('UPSTREAM_NETWORK',502);
    }
    if(!data || typeof data.status!=='number')throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    if(data.status!==0){
      if([4,301,302].includes(data.status))throw new ApiError('UPSTREAM_QUOTA',503,60);
      if([2,3,5,101,102,200,201,202,203,210,211,220,240].includes(data.status))throw new ApiError('UPSTREAM_AUTH',503);
      throw new ApiError('UPSTREAM_UNAVAILABLE',502);
    }
    return data;
  }
  async convert(p: Point, signal: AbortSignal): Promise<Point> {
    const data=await this.call('/geoconv/v1/',{coords:`${p.longitude},${p.latitude}`,from:'1',to:'5'},signal);
    const point=data.result?.[0];
    if(!point || !Number.isFinite(point.x)||!Number.isFinite(point.y)||Math.abs(point.y)>90||Math.abs(point.x)>180)throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    return {latitude:point.y,longitude:point.x,coordinateSystem:'BD09'};
  }
  async page(center: Point,q: Query,page: number, signal: AbortSignal){
    const data=await this.call('/place/v3/around',{query:keywords(q),location:`${center.latitude},${center.longitude}`,coord_type:'3',radius:'1000',radius_limit:'true',scope:'2',page_size:'20',page_num:String(page)},signal);
    if(!Array.isArray(data.results))throw new ApiError('UPSTREAM_BAD_RESPONSE',502);
    return {items:data.results as unknown[],total:typeof data.total==='number'?data.total:undefined};
  }
}
