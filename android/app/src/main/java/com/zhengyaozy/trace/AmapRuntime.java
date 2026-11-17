package com.zhengyaozy.trace;

import android.content.Context;
import com.amap.api.maps.MapsInitializer;

/** Called only after this provider's explicit consent; no separate location SDK is used. */
final class AmapRuntime {
    static boolean initialized;
    private static String failure;
    static void start(Context context){
        if(!BuildConfig.AMAP_KEY_CONFIGURED||initialized)return;
        boolean supported=false;
        for(String abi:android.os.Build.SUPPORTED_ABIS)if(abi.equals("arm64-v8a")||abi.equals("armeabi-v7a"))supported=true;
        if(!supported){failure="当前设备架构不支持高德 3D SDK\n请使用 ARM 真机；可手动切换百度地图";return;}
        try{
            MapsInitializer.updatePrivacyShow(context.getApplicationContext(),true,true);
            MapsInitializer.updatePrivacyAgree(context.getApplicationContext(),true);
            MapsInitializer.setProtocol(MapsInitializer.HTTPS);
            MapsInitializer.initialize(context.getApplicationContext());initialized=true;
        }catch(Exception|LinkageError e){failure="高德地图初始化失败，请核对 SDK 配置";}
    }
    static String message(){
        if(!BuildConfig.AMAP_KEY_CONFIGURED)return "尚未配置高德 Android Key\n可阅读应用说明或本地示例，在线接入待验证";
        if(failure!=null)return failure;
        return initialized?"高德地图已启用；鉴权与底图加载以实际运行结果为准":"高德地图尚未启用";
    }
}
