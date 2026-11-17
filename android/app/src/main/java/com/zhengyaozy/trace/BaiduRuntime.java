package com.zhengyaozy.trace;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.baidu.mapapi.*;
import java.util.*;

/** SDK is not initialized or allowed to issue requests before explicit privacy consent. */
final class BaiduRuntime {
    enum State { MISSING_KEY, WAITING, READY, AUTH_ERROR, NETWORK_ERROR }
    static State state=State.MISSING_KEY;
    static boolean initialized;
    private static final List<Runnable> observers=new ArrayList<>();
    private static final Handler handler=new Handler(Looper.getMainLooper());
    static void observe(Runnable callback){observers.add(callback);}
    static void remove(Runnable callback){observers.remove(callback);}
    private static void publish(State s){state=s;for(Runnable r:new ArrayList<>(observers))r.run();}
    static void start(Context context){
        if(!BuildConfig.BAIDU_KEY_CONFIGURED){state=State.MISSING_KEY;return;}
        if(initialized)return;
        state=State.WAITING;
        SDKInitializer.setAgreePrivacy(context.getApplicationContext(),true);
        SDKInitializer.addAuthResultListener((code,message)->handler.post(()->publish(code==0?State.READY:code==-1?State.NETWORK_ERROR:State.AUTH_ERROR)));
        SDKInitializer.setHttpsEnable(true);SDKInitializer.setCoordType(CoordType.BD09LL);
        SDKInitializer.initialize(context.getApplicationContext());initialized=true;
        handler.postDelayed(()->{if(state==State.WAITING)publish(State.NETWORK_ERROR);},20_000);
    }
    static String message(){switch(state){case MISSING_KEY:return "尚未配置百度 AK\n可阅读应用说明或本地示例，在线接入待验证";case WAITING:return "正在验证百度地图服务…";case AUTH_ERROR:return "百度地图鉴权失败\n请核对 AK、包名与签名";case NETWORK_ERROR:return "百度地图网络连接失败\n请检查网络后重新打开应用";default:return "百度地图";}}
}
