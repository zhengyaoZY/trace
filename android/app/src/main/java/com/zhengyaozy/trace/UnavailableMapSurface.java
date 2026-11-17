package com.zhengyaozy.trace;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;

/** No substitute basemap or fabricated online result when SDK setup is incomplete. */
final class UnavailableMapSurface implements MapSurface {
    private final TextView view;
    private Camera camera=new Camera(MapPoint.gps(30.24,120.15),15);
    UnavailableMapSurface(Context context,String message){view=new TextView(context);view.setText(message);view.setTextSize(14);view.setTextColor(Color.rgb(60,100,84));view.setGravity(Gravity.CENTER);view.setPadding(20,12,20,12);view.setBackgroundColor(Color.rgb(230,239,231));}
    public View view(){return view;}
    public MapPoint project(MapPoint p){return p;}
    public void camera(MapPoint p,Float zoom){camera=new Camera(p,zoom==null?15:zoom);}
    public Camera snapshot(){return camera;}
    public void fitRadius(MapPoint p,int meters){camera(p,null);}
    public void features(List<Attraction> entries,Consumer<String> select){}
    public void selected(String id){}
    public void currentPosition(MapPoint p){}
    public void route(List<Attraction.Waypoint> points){}
    public void resume(){}public void pause(){}public void destroy(){}
}
