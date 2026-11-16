package com.zhengyaozy.trace;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.io.File;

public class MainActivity extends Activity {
    private static final int TEAL=Color.rgb(19,105,88), INK=Color.rgb(26,48,42), MUTED=Color.rgb(107,123,115);
    private static final int BACKGROUND=Color.rgb(247,249,245), PALE=Color.rgb(230,239,231);
    private LinearLayout root,body,navigation,toolbar;
    private TextView heading,city;
    private MapView map;
    private int tab=0;
    private String detailId;
    private final ExploreState location=new ExploreState();
    private final String[] categories={"全部","全部"},selectedIds={null,null};
    private final double[] cameraLat={Double.NaN,Double.NaN},cameraLon={0,0},cameraZoom={12,12};
    private final int[] listOffsets={0,0};
    private ScrollView contentList;
    private LinearLayout cards;
    private final java.util.Map<String,LinearLayout> cardViews=new java.util.HashMap<>();
    private LocationSource source;
    private int locationGeneration;
    private long fixTime;
    static java.util.function.Supplier<LocationSource> testLocationSource;


    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Configuration.getInstance().setUserAgentValue(getPackageName()+"/0.3.0");
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(),"maps"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(),"maps/tiles"));
        root=column();root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        toolbar=row();toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(22),dp(12),dp(22),dp(12));
        heading=text("寻迹",23,true);toolbar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        city=text("附近 50 km",11,true);city.setTextColor(TEAL);city.setPadding(dp(12),dp(9),dp(12),dp(9));city.setBackground(rounded(PALE,30));toolbar.addView(city);root.addView(toolbar);
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        View divider=new View(this);divider.setBackgroundColor(Color.rgb(226,233,225));root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        navigation=row();navigation.setPadding(dp(16),dp(8),dp(16),dp(8));navigation.setBackgroundColor(Color.WHITE);root.addView(navigation);
        setContentView(root);
        location.sampleRegion=true;
        if(state!=null){
            tab=state.getInt("tab");detailId=state.getString("detail");
            location.status=ExploreState.Status.valueOf(state.getString("locationStatus","IDLE"));
            if(location.status==ExploreState.Status.REQUESTING)location.status=ExploreState.Status.IDLE;
            location.latitude=state.getDouble("fixLat",30.25);location.longitude=state.getDouble("fixLon",120.15);location.approximate=state.getBoolean("approximate");location.sampleRegion=state.getBoolean("sampleRegion",true);fixTime=state.getLong("fixTime");
            for(int i=0;i<2;i++){categories[i]=state.getString("category"+i,"全部");selectedIds[i]=state.getString("selected"+i);cameraLat[i]=state.getDouble("lat"+i,Double.NaN);cameraLon[i]=state.getDouble("lon"+i);cameraZoom[i]=state.getDouble("zoom"+i,12);listOffsets[i]=state.getInt("offset"+i);}
        }
        source=testLocationSource!=null?testLocationSource.get():new DeviceLocationSource(this);
        render();
    }
    private void render(){
        disposeMap();contentList=null;cards=null;cardViews.clear();body.removeAllViews();navigation.removeAllViews();
        String[] tabs={"人文","自然","我的"};
        for(int i=0;i<3;i++){
            final int index=i;Button b=button(tabs[i],false);b.setTextSize(11);b.setTextColor(i==tab?TEAL:MUTED);
            b.setBackground(ripple(i==tab?PALE:Color.WHITE,18));b.setPadding(dp(8),dp(7),dp(8),dp(7));
            TraceArt.Icon icon=new TraceArt.Icon(i,i==tab?TEAL:MUTED);icon.setBounds(0,0,dp(23),dp(23));b.setCompoundDrawables(null,icon,null,null);b.setCompoundDrawablePadding(dp(4));
            b.setContentDescription(tabs[i]+(i==tab?"，已选中":""));b.setSelected(i==tab);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(5),0,dp(5),0);navigation.addView(b,p);
            b.setOnClickListener(v->{
                saveViewport();tab=index;detailId=null;
                if(tab<2){cameraLat[tab]=Double.NaN;if(location.status==ExploreState.Status.READY)location.sampleRegion=false;}else stopLocation();
                render();if(tab<2)ensureLocation(false);
            });
        }
        Attraction selected=Attraction.SAMPLES.stream().filter(a->a.id.equals(detailId)).findFirst().orElse(null);
        city.setVisibility(selected==null?View.VISIBLE:View.GONE);
        if(selected!=null){showDetail(selected);return;}
        heading.setText(tab==0?"人文":tab==1?"自然":"我的");
        city.setVisibility(tab==2?View.GONE:View.VISIBLE);
        if(tab<2)showExplore();else showProfile();
    }
    private LinearLayout scrollContent(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout c=column();c.setPadding(dp(22),dp(8),dp(22),dp(24));scroll.addView(c);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));return c;
    }
    private void saveViewport(){
        if(tab<2&&detailId==null){
            if(map!=null){cameraLat[tab]=map.getMapCenter().getLatitude();cameraLon[tab]=map.getMapCenter().getLongitude();cameraZoom[tab]=map.getZoomLevelDouble();}
            if(contentList!=null)listOffsets[tab]=contentList.getScrollY();
        }
    }
    private void open(Attraction a){saveViewport();detailId=a.id;render();}
    private void returnToExplore(){detailId=null;render();}
    private void showExplore(){
        LinearLayout status=row();status.setPadding(dp(18),0,dp(18),0);status.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text(location.label(),12,false);label.setTextColor(MUTED);label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);status.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button recenter=button("◎ 定位",false);recenter.setContentDescription("回到当前位置");recenter.setTextSize(12);status.addView(recenter);recenter.setOnClickListener(v->{cameraLat[tab]=Double.NaN;location.sampleRegion=false;ensureLocation(true);if(location.status==ExploreState.Status.READY)render();});body.addView(status);
        if(location.status!=ExploreState.Status.READY&&location.status!=ExploreState.Status.REQUESTING&&location.status!=ExploreState.Status.IDLE){
            LinearLayout actions=row();actions.setPadding(dp(18),dp(4),dp(18),0);
            Button retry=button("重试定位",false);actions.addView(retry,new LinearLayout.LayoutParams(0,-2,1));retry.setOnClickListener(v->ensureLocation(true));
            Button settings=button("定位设置",false);actions.addView(settings,new LinearLayout.LayoutParams(0,-2,1));settings.setOnClickListener(v->startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
            Button permission=button("应用权限",false);actions.addView(permission,new LinearLayout.LayoutParams(0,-2,1));permission.setOnClickListener(v->startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));body.addView(actions);
        }
        HorizontalScrollView filter=new HorizontalScrollView(this);filter.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();chips.setPadding(dp(14),dp(8),dp(14),dp(8));
        String[] options=tab==0?new String[]{"全部","历史","美食"}:new String[]{"全部","政府管理景区","自驾路线","徒步路线"};
        for(String option:options){Button chip=button(option,option.equals(categories[tab]));chip.setTextSize(12);chip.setSelected(option.equals(categories[tab]));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.setMargins(dp(4),0,dp(4),0);chips.addView(chip,cp);if(option.equals(categories[tab]))chip.post(()->filter.scrollTo(Math.max(0,chip.getLeft()-dp(14)),0));chip.setOnClickListener(v->{saveViewport();categories[tab]=option;selectedIds[tab]=null;listOffsets[tab]=0;render();});}filter.addView(chips);body.addView(filter);
        java.util.List<Attraction> entries=location.nearby(tab==0?"人文":"自然",categories[tab]);
        map=newMap();map.setContentDescription("探索地图");
        double lat=Double.isNaN(cameraLat[tab])?location.centerLatitude():cameraLat[tab],lon=Double.isNaN(cameraLat[tab])?location.centerLongitude():cameraLon[tab];
        map.getController().setZoom(Double.isNaN(cameraLat[tab])?(location.sampleRegion?11.0:12.0):cameraZoom[tab]);map.getController().setCenter(new GeoPoint(lat,lon));
        if(location.status==ExploreState.Status.READY){Marker you=new Marker(map);you.setPosition(new GeoPoint(location.latitude,location.longitude));you.setTitle(location.approximate?"您的大致位置":"您的当前位置");you.setIcon(markerIcon(0));you.setAnchor(.5f,1);map.getOverlays().add(you);}
        for(int i=0;i<entries.size();i++){Attraction a=entries.get(i);Marker marker=new Marker(map);marker.setPosition(new GeoPoint(a.latitude,a.longitude));marker.setAnchor(.5f,1);marker.setTitle(a.name);marker.setIcon(markerIcon(i+1));marker.setOnMarkerClickListener((m,v)->{selectContent(a.id);return true;});map.getOverlays().add(marker);}
        LinearLayout.LayoutParams frame=new LinearLayout.LayoutParams(-1,0,1);frame.setMargins(dp(14),0,dp(14),0);body.addView(map,frame);addCredit(body);
        contentList=new ScrollView(this);contentList.setContentDescription("分类内容列表");contentList.setFillViewport(true);contentList.setSmoothScrollingEnabled(false);contentList.setFocusableInTouchMode(true);contentList.setDescendantFocusability(android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS);cards=column();cards.setPadding(dp(18),dp(4),dp(18),dp(12));contentList.addView(cards);body.addView(contentList,new LinearLayout.LayoutParams(-1,0,1));contentList.requestFocus();
        if(entries.isEmpty()){
            title(cards,location.status==ExploreState.Status.REQUESTING?"正在获取当前位置…":"附近暂无示例内容",18);
            caption(cards,"首版内置杭州内容；按探索位置周围 50 公里筛选。",13);
            Button sample=button("查看杭州示例",false);cards.addView(sample);sample.setOnClickListener(v->{location.sampleRegion=true;cameraLat[tab]=Double.NaN;listOffsets[tab]=0;render();});
        }
        for(int i=0;i<entries.size();i++){
            Attraction a=entries.get(i);LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(rounded(Color.WHITE,18));
            eyebrow(card,String.format(java.util.Locale.ROOT,"%02d  /  %s",i+1,a.category));title(card,a.name,19);caption(card,a.subtitle,13);
            if(a.isRoute())caption(card,String.format(java.util.Locale.CHINA,"示例路线 · 非导航  /  折线估算 %.1f km",a.distanceKm()),11);
            Button view=button("查看详情  ↗",false);view.setContentDescription("查看"+a.name);card.addView(view);view.setOnClickListener(v->open(a));
            card.setOnClickListener(v->{selectContent(a.id);map.getController().setCenter(new GeoPoint(a.latitude,a.longitude));});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);cards.addView(card,p);cardViews.put(a.id,card);
        }
        updateSelection();final ScrollView list=contentList;final int offset=listOffsets[tab];list.post(()->list.scrollTo(0,offset));map.onResume();
    }
    void selectContent(String id){selectedIds[tab]=id;updateSelection();LinearLayout card=cardViews.get(id);if(card!=null&&contentList!=null)contentList.smoothScrollTo(0,card.getTop());}
    private void updateSelection(){for(java.util.Map.Entry<String,LinearLayout> e:cardViews.entrySet()){GradientDrawable bg=rounded(Color.WHITE,18);boolean selected=e.getKey().equals(selectedIds[tab]);if(selected)bg.setStroke(dp(2),TEAL);e.getValue().setBackground(bg);e.getValue().setSelected(selected);}}
    private MapView newMap(){MapView v=new MapView(this);v.setTileSource(TileSourceFactory.MAPNIK);v.setMultiTouchControls(true);v.setBuiltInZoomControls(false);v.setBackground(rounded(PALE,20));v.setClipToOutline(true);return v;}
    private void addCredit(LinearLayout parent){TextView credit=text("",10,false);credit.setText(android.text.Html.fromHtml("© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a> · ODbL",android.text.Html.FROM_HTML_MODE_LEGACY));credit.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());credit.setLinkTextColor(MUTED);credit.setPadding(dp(18),dp(5),dp(18),dp(5));parent.addView(credit);}
    private void showDetail(Attraction a){
        heading.setText(a.name);LinearLayout c=scrollContent();
        Button back=button("‹  返回探索",false);back.setBackground(ripple(Color.TRANSPARENT,16));back.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);c.addView(back);back.setOnClickListener(v->returnToExplore());
        int index=Attraction.SAMPLES.indexOf(a)%3;TraceArt art=new TraceArt(this,index);art.setBackground(rounded(PALE,22));art.setClipToOutline(true);c.addView(art,new LinearLayout.LayoutParams(-1,dp(175)));
        space(c,20);eyebrow(c,"杭州示例  /  分类内容");title(c,a.subtitle,24);caption(c,a.introduction,16);
        space(c,22);LinearLayout article=column();article.setPadding(dp(18),dp(20),dp(18),dp(20));article.setBackground(rounded(Color.WHITE,22));
        eyebrow(article,"READ THE CITY");title(article,"文字讲解",20);TextView narration=caption(article,a.narration,17);narration.setTextColor(INK);narration.setLineSpacing(dp(7),1);narration.setTextIsSelectable(true);c.addView(article);
        if(a.isRoute()){
            space(c,18);title(c,"示例路线 · 非导航",20);caption(c,String.format(java.util.Locale.CHINA,"折线估算 %.1f km；不代表实际道路或步行距离。",a.distanceKm()),14);
            map=newMap();map.setContentDescription("路线预览地图");java.util.List<GeoPoint> points=new java.util.ArrayList<>();
            for(Attraction.Waypoint point:a.waypoints){GeoPoint geo=new GeoPoint(point.latitude,point.longitude);points.add(geo);Marker m=new Marker(map);m.setPosition(geo);m.setTitle(point.name);m.setIcon(markerIcon(points.size()));m.setAnchor(.5f,1);map.getOverlays().add(m);caption(c,String.format(java.util.Locale.CHINA,"%d  %s",points.size(),point.name),14);}
            org.osmdroid.views.overlay.Polyline line=new org.osmdroid.views.overlay.Polyline();line.setPoints(points);line.getOutlinePaint().setColor(TEAL);line.getOutlinePaint().setStrokeWidth(dp(3));line.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{dp(8),dp(5)},0));map.getOverlays().add(0,line);
            final MapView routeMap=map;map.addOnFirstLayoutListener((v,l,t,r,b)->{if(map==routeMap)routeMap.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(points),false,dp(32));});
            c.addView(map,new LinearLayout.LayoutParams(-1,dp(240)));addCredit(c);map.onResume();
        }
        if(!a.manager.isEmpty()){
            space(c,18);title(c,"管理与来源",18);caption(c,a.manager,14);TextView link=caption(c,a.source,13);android.text.util.Linkify.addLinks(link,android.text.util.Linkify.WEB_URLS);link.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        space(c,12);caption(c,"杭州内置示例 · 内容不随位置自动更新",12);
    }
    private Drawable markerIcon(int number){
        Bitmap image=Bitmap.createBitmap(dp(38),dp(46),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);c.scale(image.getWidth()/38f,image.getHeight()/46f);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(TEAL);Path point=new Path();point.moveTo(12,30);point.lineTo(19,44);point.lineTo(26,30);point.close();c.drawPath(point,p);c.drawCircle(19,18,17,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(19,18,14,p);p.setStyle(Paint.Style.FILL);p.setTextSize(14);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(number==0?"◎":String.valueOf(number),19,23,p);return new BitmapDrawable(getResources(),image);
    }
    private void showProfile(){
        LinearLayout c=scrollContent();eyebrow(c,"TRACE / DOCUMENTATION");title(c,"应用说明",28);caption(c,"认识寻迹，开始你的城市探索。",15);
        document(c,"使用指南","人文与自然首页以当前位置为中心展示地图。横向切换分类，地图标记与卡片同步筛选。轻点标记选中卡片，点击查看详情阅读内容。◎ 定位可回到当前位置。点击卡片空白处可在地图中聚焦。");
        document(c,"分类说明","人文包含历史、美食；自然包含政府管理景区、自驾路线、徒步路线。路线是静态示意，不是实测轨迹或导航，折线距离不代表真实行程距离。");
        document(c,"定位与隐私","只申请前台定位，支持大致位置，不在后台跟踪，不保存定位历史、不上传至业务后台。定位失败时明确标注杭州示例区域。地图瓦片服务会收到网络请求，可据瓦片推断所浏览的区域。");
        document(c,"示例数据与地图","首版仅内置杭州示例，按探索位置周围 50 公里筛选，其他地区可能暂无内容。查看杭州示例只改变探索区域，不表示您已位于杭州。文本可离线阅读，底图来自 OpenStreetMap，需联网加载。官方来源链接与景区公告用于核对信息，不提供实时营业、路况或通行保证。");
        document(c,"版本信息","寻迹 Android 0.3.0（3）\n无账号、无业务后台；个人试用的调试版本。地图：osmdroid / OpenStreetMap，数据遵循 ODbL。");
    }
    private void document(LinearLayout c,String title,String description){space(c,16);LinearLayout panel=column();panel.setPadding(dp(16),dp(10),dp(16),dp(16));panel.setBackground(rounded(Color.WHITE,20));title(panel,title,18);caption(panel,description,15);c.addView(panel);}
    private boolean permitted(){return checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED||checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    private void ensureLocation(boolean explicit){
        if(tab==2)return;
        if(location.status==ExploreState.Status.REQUESTING)return;
        if(testLocationSource==null&&!permitted()){
            if(explicit||!getPreferences(MODE_PRIVATE).getBoolean("askedLocation",false)){
                location.status=ExploreState.Status.REQUESTING;location.sampleRegion=true;render();getPreferences(MODE_PRIVATE).edit().putBoolean("askedLocation",true).apply();
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},73);
            }else{location.fail(ExploreState.Status.DENIED);render();}return;
        }
        if(testLocationSource==null){android.location.LocationManager manager=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);if(manager==null||(!manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)&&!manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))){location.fail(ExploreState.Status.DISABLED);render();return;}}
        if(location.status==ExploreState.Status.READY&&android.os.SystemClock.elapsedRealtime()-fixTime<120_000){if(explicit){location.sampleRegion=false;render();}return;}
        location.status=ExploreState.Status.REQUESTING;location.sampleRegion=true;render();final int generation=++locationGeneration;
        source.start(new LocationSource.Callback(){
            @Override public void ready(double lat,double lon,boolean approximate){runOnUiThread(()->{if(generation!=locationGeneration)return;location.ready(lat,lon,approximate);fixTime=android.os.SystemClock.elapsedRealtime();if(tab<2&&detailId==null)render();});}
            @Override public void failed(ExploreState.Status reason){runOnUiThread(()->{if(generation!=locationGeneration)return;location.fail(reason);if(tab<2&&detailId==null)render();});}
        });
    }
    private void stopLocation(){locationGeneration++;if(source!=null)source.cancel();if(location.status==ExploreState.Status.REQUESTING)location.status=ExploreState.Status.IDLE;}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code!=73)return;location.status=ExploreState.Status.IDLE;if(tab==2)return;if(permitted())ensureLocation(false);else{location.fail(ExploreState.Status.DENIED);render();}}
    @Override protected void onStart(){super.onStart();if(tab<2)ensureLocation(false);}
    @Override protected void onStop(){saveViewport();stopLocation();super.onStop();}
    @Override public void onBackPressed(){if(detailId!=null){returnToExplore();}else if(tab!=0){tab=0;cameraLat[0]=Double.NaN;render();ensureLocation(false);}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle s){saveViewport();s.putString("locationStatus",location.status.name());s.putDouble("fixLat",location.latitude);s.putDouble("fixLon",location.longitude);s.putBoolean("approximate",location.approximate);s.putBoolean("sampleRegion",location.sampleRegion);s.putLong("fixTime",fixTime);s.putInt("tab",tab);s.putString("detail",detailId);for(int i=0;i<2;i++){s.putString("category"+i,categories[i]);s.putString("selected"+i,selectedIds[i]);s.putDouble("lat"+i,cameraLat[i]);s.putDouble("lon"+i,cameraLon[i]);s.putDouble("zoom"+i,cameraZoom[i]);s.putInt("offset"+i,listOffsets[i]);}super.onSaveInstanceState(s);}
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
    @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
    @Override protected void onDestroy(){stopLocation();disposeMap();super.onDestroy();}
    private void disposeMap(){if(map!=null){map.onPause();map.onDetach();map=null;}}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private TextView text(String value,int sp,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(INK);v.setIncludeFontPadding(false);v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return v;}
    private TextView title(LinearLayout parent,String value,int sp){TextView v=text(value,sp,true);v.setPadding(0,dp(10),0,dp(8));v.setLineSpacing(dp(3),1);parent.addView(v);return v;}
    private TextView caption(LinearLayout parent,String value,int sp){TextView v=text(value,sp,false);v.setTextColor(MUTED);v.setLineSpacing(dp(5),1);v.setPadding(0,dp(4),0,dp(4));parent.addView(v);return v;}
    private void eyebrow(LinearLayout parent,String value){TextView v=text(value,10,true);v.setTextColor(TEAL);v.setLetterSpacing(.12f);parent.addView(v);}
    private void space(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:TEAL);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setAllCaps(false);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setMinWidth(0);b.setMinimumWidth(0);b.setStateListAnimator(null);b.setElevation(0);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setBackground(ripple(primary?TEAL:PALE,14));return b;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Drawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(Color.argb(32,19,105,88)),rounded(color,radius),rounded(Color.WHITE,radius));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
