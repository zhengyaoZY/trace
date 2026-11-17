import { BaiduUpstream } from './baidu.js';
import { NearbyService } from './service.js';
import { createApp } from './app.js';
const app=await createApp(new NearbyService(new BaiduUpstream(process.env.BAIDU_SERVER_AK??'')),true);
await app.listen({host:process.env.HOST??'0.0.0.0',port:Number(process.env.PORT??3000)});
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>{void app.close();});
