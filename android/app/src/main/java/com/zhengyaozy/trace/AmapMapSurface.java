package com.zhengyaozy.trace;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import java.util.*;
import java.util.function.Consumer;

/** GCJ-02 surface; SDK logos remain visible, POI bubbles have no numbers or labels. */
final class AmapMapSurface implements MapSurface {
    private static final int TEAL=Color.rgb(19,105,88);
    private final MapView view;
    private final AMap map;
    private final Context context;
    private final Map<String,Marker> markers=new HashMap<>();
    private final Map<String,BitmapDescriptor[]> icons=new HashMap<>();
    private final List<BitmapDescriptor> bitmaps=new ArrayList<>();
    private boolean loaded,destroyed;
    private Runnable loadedAction;
    private Camera desired=new Camera(MapPoint.gps(30.24,120.15),15);
    AmapMapSurface(Context context){
        this.context=context;view=new MapView(context);view.onCreate(new Bundle());map=view.getMap();
        map.getUiSettings().setZoomControlsEnabled(false);map.getUiSettings().setMyLocationButtonEnabled(false);
        map.showMapText(false);map.showIndoorMap(false);map.showBuildings(false);
        map.setOnMapLoadedListener(()->{loaded=true;if(!destroyed&&loadedAction!=null)loadedAction.run();});
    }
    public View view(){return view;}
    public MapPoint project(MapPoint p){return p.toGcj02(gps->{LatLng b=new CoordinateConverter(context).from(CoordinateConverter.CoordType.GPS).coord(new LatLng(gps.latitude,gps.longitude)).convert();return new MapPoint(b.latitude,b.longitude,"GCJ02");});}
    private LatLng latLng(MapPoint p){MapPoint b=project(p);return new LatLng(b.latitude,b.longitude);}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public void camera(MapPoint p,Float zoom){
        MapPoint b=project(p);float z=zoom==null?15:zoom;desired=new Camera(b,z);
        loadedAction=()->{if(!destroyed)map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng(b),z));};loadedAction.run();
    }
    public Camera snapshot(){CameraPosition p=map.getCameraPosition();return !loaded||p==null?desired:new Camera(new MapPoint(p.target.latitude,p.target.longitude,"GCJ02"),p.zoom);}
    public void fitRadius(MapPoint gps,int meters){
        if(!"WGS84".equals(gps.coordinateSystem))throw new IllegalArgumentException("GPS center required");
        desired=new Camera(project(gps),Float.NaN);
        double dLat=Math.toDegrees(meters/6371008.8),dLon=dLat/Math.max(.01,Math.cos(Math.toRadians(gps.latitude)));
        bounds(new LatLngBounds.Builder().include(latLng(MapPoint.gps(Math.min(89.9,gps.latitude+dLat),gps.longitude+dLon)))
            .include(latLng(MapPoint.gps(Math.max(-89.9,gps.latitude-dLat),gps.longitude-dLon))).build(),dp(16));
    }
    private void bounds(LatLngBounds b,int padding){
        loadedAction=()->{if(!destroyed&&view.getWidth()>padding*2&&view.getHeight()>padding*2)map.moveCamera(CameraUpdateFactory.newLatLngBounds(b,view.getWidth(),view.getHeight(),padding));};
        view.post(loadedAction);if(loaded)loadedAction.run();
    }
    public void features(List<Attraction> entries,Consumer<String> select){
        map.clear();markers.clear();recycle();icons.clear();
        for(Attraction a:entries){Double score=a.metrics==null?null:a.metrics.score;BitmapDescriptor[] pair={dot(score,false,TEAL),dot(score,true,TEAL)};icons.put(a.id,pair);
            Marker marker=map.addMarker(new MarkerOptions().position(latLng(new MapPoint(a.latitude,a.longitude,a.coordinateSystem))).icon(pair[0]).anchor(.5f,.5f).zIndex(10));
            marker.setObject(a.id);markers.put(a.id,marker);
        }
        map.setOnMarkerClickListener(m->{if(m.getObject() instanceof String&&markers.containsKey(m.getObject())){select.accept((String)m.getObject());return true;}return true;});
    }
    private BitmapDescriptor dot(Double score,boolean selected,int color){
        Bitmap bitmap=Bitmap.createBitmap(dp(HIT_DP),dp(HIT_DP),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(color);
        float center=dp(HIT_DP)/2f,radius=MarkerStyle.diameterDp(score)*context.getResources().getDisplayMetrics().density/2f;canvas.drawCircle(center,center,radius,paint);
        if(selected){paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(dp(2));canvas.drawCircle(center,center,radius-dp(2),paint);}
        BitmapDescriptor icon=BitmapDescriptorFactory.fromBitmap(bitmap);bitmaps.add(icon);return icon;
    }
    public void selected(String uid){for(Map.Entry<String,Marker> e:markers.entrySet())e.getValue().setIcon(icons.get(e.getKey())[e.getKey().equals(uid)?1:0]);}
    public void currentPosition(MapPoint gps){map.addMarker(new MarkerOptions().position(latLng(gps)).icon(dot(null,true,Color.rgb(40,116,220))).anchor(.5f,.5f).zIndex(20));}
    public void route(List<Attraction.Waypoint> waypoints){
        List<LatLng> points=new ArrayList<>();LatLngBounds.Builder bounds=new LatLngBounds.Builder();
        for(Attraction.Waypoint w:waypoints){LatLng p=latLng(MapPoint.gps(w.latitude,w.longitude));points.add(p);bounds.include(p);map.addMarker(new MarkerOptions().position(p).icon(dot(null,false,TEAL)).anchor(.5f,.5f));}
        if(points.size()>1){map.addPolyline(new PolylineOptions().addAll(points).color(TEAL).width(dp(3)));bounds(bounds.build(),dp(24));}
    }
    public void resume(){view.onResume();}public void pause(){view.onPause();}
    private void recycle(){for(BitmapDescriptor b:bitmaps)b.recycle();bitmaps.clear();}
    public void destroy(){destroyed=true;loadedAction=null;map.clear();view.onDestroy();recycle();}
}
