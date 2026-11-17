package com.zhengyaozy.trace;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.ArrayList;
import java.util.List;

/** One foreground fix, no Google Play Services dependency and no stored position history. */
final class DeviceLocationSource implements LocationSource,LocationListener {
    private final Context context;
    private final LocationManager manager;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Callback callback;
    private boolean approximate;
    private final Runnable timeout=()->finishFailure(ExploreState.Status.TIMEOUT);
    DeviceLocationSource(Context context){this.context=context;manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);}
    @Override public void start(Callback cb){
        cancel();callback=cb;
        boolean fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        approximate=!fine;
        if(!fine&&context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){finishFailure(ExploreState.Status.DENIED);return;}
        if(manager==null){finishFailure(ExploreState.Status.UNAVAILABLE);return;}
        List<String> providers=new ArrayList<>();
        try{
            if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))providers.add(LocationManager.NETWORK_PROVIDER);
            if(fine&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER))providers.add(LocationManager.GPS_PROVIDER);
            if(providers.isEmpty()){finishFailure(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)?ExploreState.Status.UNAVAILABLE:ExploreState.Status.DISABLED);return;}
            for(String provider:providers){
                Location cached=manager.getLastKnownLocation(provider);
                if(cached!=null&&SystemClock.elapsedRealtimeNanos()-cached.getElapsedRealtimeNanos()<=120_000_000_000L){onLocationChanged(cached);return;}
            }
            handler.postDelayed(timeout,15_000);
            for(String provider:providers)manager.requestLocationUpdates(provider,0,0,this,Looper.getMainLooper());
        }catch(SecurityException e){finishFailure(ExploreState.Status.DENIED);}
        catch(IllegalArgumentException e){finishFailure(ExploreState.Status.UNAVAILABLE);}
    }
    @Override public void onLocationChanged(Location fix){
        if(callback==null)return;Callback cb=callback;cancel();cb.ready(fix.getLatitude(),fix.getLongitude(),approximate);
    }
    private void finishFailure(ExploreState.Status reason){if(callback==null)return;Callback cb=callback;cancel();cb.failed(reason);}
    @Override public void cancel(){handler.removeCallbacks(timeout);if(manager!=null)try{manager.removeUpdates(this);}catch(SecurityException ignored){}callback=null;}
    @Override public void onProviderDisabled(String provider){}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
}
