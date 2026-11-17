package com.zhengyaozy.trace;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.*;
import com.baidu.mapapi.utils.CoordinateConverter;
import java.util.*;
import java.util.function.Consumer;

final class BaiduMapSurface implements MapSurface {
    private static final int TEAL=Color.rgb(19,105,88);
    private final MapView view;
    private final BaiduMap map;
    private final Map<String,Marker> markers=new HashMap<>();
    private final List<BitmapDescriptor> bitmaps=new ArrayList<>();
    private final Context context;
    private Runnable loadedAction;
    private boolean loaded,destroyed;
    private Camera desired=new Camera(MapPoint.gps(30.24,120.15),15);
    private BitmapDescriptor normalDot,selectedDot;
    BaiduMapSurface(Context context){
        this.context=context;view=new MapView(context);map=view.getMap();view.showZoomControls(false);
        map.showMapPoi(false);map.showMapIndoorPoi(false);map.setBuildingsEnabled(false);
        map.setOnMapLoadedCallback(()->{loaded=true;if(loadedAction!=null&&!destroyed)loadedAction.run();});
    }
    public View view(){return view;}
    public MapPoint project(MapPoint p){
        return p.toBd09(gps->{LatLng converted=new CoordinateConverter().from(CoordinateConverter.CoordType.GPS).coord(new LatLng(gps.latitude,gps.longitude)).convert();return new MapPoint(converted.latitude,converted.longitude,true);});
    }
    private LatLng latLng(MapPoint p){MapPoint b=project(p);return new LatLng(b.latitude,b.longitude);}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public void camera(MapPoint p,Float zoom){
        MapPoint b=project(p);desired=new Camera(b,zoom==null?15:zoom);LatLng center=latLng(b);loadedAction=()->map.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(center,zoom==null?15:zoom));
        loadedAction.run();
    }
    public Camera snapshot(){MapStatus s=map.getMapStatus();if(!loaded||s==null)return desired;return new Camera(new MapPoint(s.target.latitude,s.target.longitude,true),s.zoom);}
    public void fitRadius(MapPoint gps,int meters){
        desired=new Camera(project(gps),Float.NaN);
        if(gps.bd09)throw new IllegalArgumentException("radius bounds require GPS center");
        double dLat=Math.toDegrees(meters/6371008.8),dLon=dLat/Math.max(.01,Math.cos(Math.toRadians(gps.latitude)));
        LatLngBounds bounds=new LatLngBounds.Builder().include(latLng(MapPoint.gps(Math.min(89.9,gps.latitude+dLat),gps.longitude+dLon)))
            .include(latLng(MapPoint.gps(Math.max(-89.9,gps.latitude-dLat),gps.longitude-dLon))).build();
        bounds(bounds,dp(16));
    }
    private void bounds(LatLngBounds b,int padding){loadedAction=()->{if(view.getWidth()>padding*2&&view.getHeight()>padding*2)map.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(b,padding,padding,padding,padding));};view.post(loadedAction);if(loaded)loadedAction.run();}
    public void features(List<Attraction> entries,Consumer<String> select){
        map.clear();markers.clear();recycle();
        normalDot=dot(false);selectedDot=dot(true);BitmapDescriptor normal=normalDot;for(Attraction a:entries){Bundle info=new Bundle();info.putString("uid",a.id);
            Marker m=(Marker)map.addOverlay(new MarkerOptions().position(latLng(new MapPoint(a.latitude,a.longitude,a.bd09)))
                .icon(normal).anchor(.5f,.5f).extraInfo(info).zIndex(10));markers.put(a.id,m);
        }
        map.setOnMarkerClickListener(m->{Bundle info=m.getExtraInfo();if(info!=null){select.accept(info.getString("uid"));return true;}return false;});
    }
    private BitmapDescriptor dot(boolean selected){
        Bitmap bitmap=Bitmap.createBitmap(dp(HIT_DP),dp(HIT_DP),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(TEAL);
        float center=dp(HIT_DP)/2f,radius=dp(DOT_DP)/2f;canvas.drawCircle(center,center,radius,paint);
        if(selected){paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(dp(2));canvas.drawCircle(center,center,radius-dp(2),paint);}
        BitmapDescriptor descriptor=BitmapDescriptorFactory.fromBitmap(bitmap);bitmaps.add(descriptor);return descriptor;
    }
    public void selected(String uid){if(normalDot==null)return;BitmapDescriptor normal=normalDot,active=selectedDot;for(Map.Entry<String,Marker> e:markers.entrySet())e.getValue().setIcon(e.getKey().equals(uid)?active:normal);}
    public void currentPosition(MapPoint p){LatLng b=latLng(p);map.setMyLocationEnabled(true);map.setMyLocationData(new MyLocationData.Builder().latitude(b.latitude).longitude(b.longitude).build());}
    public void route(List<Attraction.Waypoint> waypoints){
        List<LatLng> points=new ArrayList<>();for(Attraction.Waypoint w:waypoints){LatLng p=latLng(MapPoint.gps(w.latitude,w.longitude));points.add(p);map.addOverlay(new MarkerOptions().position(p).icon(dot(false)).anchor(.5f,.5f));}
        if(points.size()>1){map.addOverlay(new PolylineOptions().points(points).color(TEAL).width(dp(3)).dottedLine(true));LatLngBounds.Builder b=new LatLngBounds.Builder();for(LatLng p:points)b.include(p);bounds(b.build(),dp(24));}
    }
    public void resume(){view.onResume();}public void pause(){view.onPause();}
    private void recycle(){for(BitmapDescriptor b:bitmaps)b.recycle();bitmaps.clear();}
    public void destroy(){destroyed=true;loadedAction=null;map.clear();view.onDestroy();recycle();}
}
