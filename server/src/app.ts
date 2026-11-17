import Fastify, { type FastifyError } from 'fastify';
import rateLimit from '@fastify/rate-limit';
import { ApiError, type Query } from './types.js';
import { NearbyService } from './service.js';
export async function createApp(service: NearbyService,logging=false){
  const app=Fastify({logger:logging?{serializers:{req:r=>({id:r.id,method:r.method}),res:r=>({statusCode:r.statusCode})}}:false,bodyLimit:4096,trustProxy:false,ajv:{customOptions:{removeAdditional:false,coerceTypes:false}}});
  app.addHook('onSend',async(_,reply,payload)=>{reply.header('Cache-Control','no-store');return payload;});
  await app.register(rateLimit,{max:30,timeWindow:60_000,errorResponseBuilder:()=>({statusCode:429,code:'RATE_LIMITED'})});
  app.setErrorHandler<FastifyError|ApiError>((error,request,reply)=>{
    const detail=error as FastifyError;
    const status=detail.statusCode??500;
    const invalid=!!detail.validation||status===400;
    const failure=error instanceof ApiError?error:new ApiError(
      status===429?'RATE_LIMITED':status===413?'PAYLOAD_TOO_LARGE':invalid?'INVALID_REQUEST':'INTERNAL_ERROR',
      status===429?429:status===413?413:invalid?400:500,status===429?60:undefined);
    if(failure.retryAfter)reply.header('Retry-After',String(failure.retryAfter));
    // Only controlled error codes are logged; never raw error objects, payloads or upstream URLs.
    request.log.warn({code:failure.code},'request failed');
    reply.code(failure.status).send({error:{code:failure.code,message:failure.code},requestId:request.id});
  });
  app.get('/healthz',{config:{rateLimit:false}},async()=>({status:'ok'}));
  app.get('/readyz',{config:{rateLimit:false}},async(_,reply)=>reply.code(service.upstream.configured?200:503).send({ready:service.upstream.configured}));
  app.post<{Body:Query}>('/api/v1/places/nearby',{
    schema:{body:{type:'object',additionalProperties:false,required:['center','section','category'],properties:{
      center:{type:'object',additionalProperties:false,required:['latitude','longitude','coordinateSystem'],properties:{latitude:{type:'number',minimum:-90,maximum:90},longitude:{type:'number',minimum:-180,maximum:180},coordinateSystem:{const:'WGS84'}}},
      section:{enum:['人文','自然']},category:{enum:['全部','历史','美食','自然景区']},radiusMeters:{const:1000},refresh:{type:'boolean',default:false}
    }}}
  },async(request)=>{
    const q=request.body;
    if((q.section==='人文'&&q.category==='自然景区')||(q.section==='自然'&&!['全部','自然景区'].includes(q.category)))throw new ApiError('INVALID_CATEGORY',400);
    return service.load(q);
  });
  return app;
}
