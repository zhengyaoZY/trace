package com.zhengyaozy.trace;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** Instrumentation-only rendering: explicitly marked, no SDK, map tiles or credentials. */
final class FakeMapSurface implements MapSurface {
    static int lastFitRadius;
    static final List<FakeMapSurface> created=new ArrayList<>();
    final FrameLayout root;
    final Context context;
    final List<Attraction.Waypoint> routePoints=new ArrayList<>();
    final Map<String,View> dots=new HashMap<>();
    Camera camera=new Camera(new MapPoint(30.246,120.156,true),15);
    int radius;
    FakeMapSurface(Context context){this.context=context;root=new FrameLayout(context);root.setBackgroundColor(Color.rgb(226,238,229));created.add(this);root.addView(new View(context){
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override protected void onDraw(Canvas c){super.onDraw(c);p.setColor(Color.rgb(201,219,207));p.setStrokeWidth(1);for(int x=0;x<getWidth();x+=32)c.drawLine(x,0,x,getHeight(),p);for(int y=0;y<getHeight();y+=32)c.drawLine(0,y,getWidth(),y,p);p.setColor(Color.rgb(76,110,90));p.setTextSize(dp(10));c.drawText("测试地图 · 非百度底图",dp(8),getHeight()-dp(8),p);if(routePoints.size()>1){p.setStrokeWidth(dp(2));p.setPathEffect(new DashPathEffect(new float[]{dp(6),dp(4)},0));c.drawLine(getWidth()*.2f,getHeight()*.65f,getWidth()*.8f,getHeight()*.25f,p);p.setPathEffect(null);}}
    },new FrameLayout.LayoutParams(-1,-1));}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    public View view(){return root;}
    public MapPoint project(MapPoint p){return p.toBd09(gps->new MapPoint(gps.latitude+.006,gps.longitude+.006,true));}
    public void camera(MapPoint p,Float zoom){camera=new Camera(project(p),zoom==null?15:zoom);}
    public Camera snapshot(){return camera;}
    public void fitRadius(MapPoint gps,int meters){radius=meters;lastFitRadius=meters;camera(gps,null);}
    private void clearDots(){for(View v:dots.values())root.removeView(v);dots.clear();}
    private void addDot(String uid,int index,Consumer<String> select){
        FrameLayout hit=new FrameLayout(context);hit.setContentDescription("景点圆点:"+uid);hit.setOnClickListener(v->select.accept(uid));
        View circle=new View(context);GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(Color.rgb(19,105,88));circle.setBackground(bg);circle.setTag("visible-dot");hit.addView(circle,new FrameLayout.LayoutParams(dp(DOT_DP),dp(DOT_DP),Gravity.CENTER));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(HIT_DP),dp(HIT_DP));p.leftMargin=dp(16+index*72);p.topMargin=dp(20+index%2*16);root.addView(hit,p);dots.put(uid,hit);
    }
    public void features(List<Attraction> entries,Consumer<String> select){clearDots();int i=0;for(Attraction a:entries)addDot(a.id,i++,select);}
    public void selected(String uid){for(Map.Entry<String,View> e:dots.entrySet()){e.getValue().setSelected(e.getKey().equals(uid));View circle=e.getValue().findViewWithTag("visible-dot");GradientDrawable bg=(GradientDrawable)circle.getBackground();bg.setStroke(dp(e.getKey().equals(uid)?2:0),Color.WHITE);}}
    public void currentPosition(MapPoint gps){}
    public void route(List<Attraction.Waypoint> points){routePoints.addAll(points);int i=0;for(Attraction.Waypoint p:points)addDot(p.name,i++,id->{});root.invalidate();}
    public void resume(){}public void pause(){}public void destroy(){}
}
