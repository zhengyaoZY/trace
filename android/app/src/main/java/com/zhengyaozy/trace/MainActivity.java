package com.zhengyaozy.trace;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
public class MainActivity extends Activity {
    private static final int TEAL=Color.rgb(19,105,88), INK=Color.rgb(26,48,42), MUTED=Color.rgb(107,123,115);
    private static final int BACKGROUND=Color.rgb(247,249,245), PALE=Color.rgb(230,239,231);
    private LinearLayout root,body,navigation,toolbar;
    private TextView heading,city;
    private MapSurface map;
    private int tab=0;
    private String detailId;
    private Attraction detail;
    private final ExploreState location=new ExploreState();
    private final String[] categories={"全部","全部"},selectedIds={null,null};
    private final double[] cameraLat={Double.NaN,Double.NaN},cameraLon={0,0},cameraZoom={12,12};
    private final boolean[] cameraBd={false,false};
    private final int[] listOffsets={0,0};
    private ScrollView contentList;
    private LinearLayout cards;
    private final java.util.Map<String,LinearLayout> cardViews=new java.util.HashMap<>();
    private LocationSource source;
    private int locationGeneration;
    private long fixTime;
    static final class TestDependencies {
        java.util.function.Supplier<LocationSource> location;
        java.util.function.Function<android.content.Context,MapSurface> maps;
        PoiProvider pois;
        boolean consent=true,online=true;
    }
    static TestDependencies tests;
    private PoiRepository repository;
    private final android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    private String queryKey="";
    private long queryGeneration;
    private boolean loading,forceNext,privacyPrompted;
    private PoiProvider.Failure poiFailure;
    private java.util.List<Attraction> liveEntries=java.util.Collections.emptyList();
    private final Runnable sdkChanged=()->{if(!isFinishing()){if(tab<2&&detailId==null)render();}};



    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        root=column();root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        toolbar=row();toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(22),dp(12),dp(22),dp(12));
        heading=text("寻迹",23,true);toolbar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        city=text("半径 1 km",11,true);city.setTextColor(TEAL);city.setPadding(dp(12),dp(9),dp(12),dp(9));city.setBackground(rounded(PALE,30));toolbar.addView(city);root.addView(toolbar);
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        View divider=new View(this);divider.setBackgroundColor(Color.rgb(226,233,225));root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        navigation=row();navigation.setPadding(dp(16),dp(8),dp(16),dp(8));navigation.setBackgroundColor(Color.WHITE);root.addView(navigation);
        setContentView(root);
        location.sampleRegion=true;
        if(state!=null){
            tab=state.getInt("tab");detailId=state.getString("detail");
            location.status=ExploreState.Status.valueOf(state.getString("locationStatus","IDLE"));
            if(location.status==ExploreState.Status.REQUESTING)location.status=ExploreState.Status.IDLE;
            location.latitude=state.getDouble("fixLat",30.24);location.longitude=state.getDouble("fixLon",120.15);location.approximate=state.getBoolean("approximate");location.sampleRegion=state.getBoolean("sampleRegion",true);fixTime=state.getLong("fixTime");
            for(int i=0;i<2;i++){categories[i]=state.getString("category"+i,"全部");selectedIds[i]=state.getString("selected"+i);cameraLat[i]=state.getDouble("lat"+i,Double.NaN);cameraLon[i]=state.getDouble("lon"+i);cameraZoom[i]=state.getDouble("zoom"+i,15);cameraBd[i]=state.getBoolean("bd"+i);listOffsets[i]=state.getInt("offset"+i);}
        }
        if(state!=null&&state.getBoolean("detailOnline"))detail=Attraction.poi(state.getString("detailUid"),state.getString("detailName"),state.getString("detailAddress"),"",state.getDouble("detailLat"),state.getDouble("detailLon"),state.getString("detailSection"),state.getString("detailCategory"));
        source=tests!=null&&tests.location!=null?tests.location.get():new DeviceLocationSource(this);
        repository=new PoiRepository(tests!=null&&tests.pois!=null?tests.pois:new BaiduPoiProvider(),android.os.SystemClock::elapsedRealtime);
        BaiduRuntime.observe(sdkChanged);
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
                saveViewport();repository.cancel();queryKey="";tab=index;detailId=null;detail=null;
                if(tab<2){cameraLat[tab]=Double.NaN;if(location.status==ExploreState.Status.READY)location.sampleRegion=false;}else stopLocation();
                render();if(tab<2)ensureLocation(false);
            });
        }
        Attraction selected=detail!=null?detail:Attraction.SAMPLES.stream().filter(a->a.id.equals(detailId)).findFirst().orElse(null);
        city.setVisibility(selected==null?View.VISIBLE:View.GONE);
        if(selected!=null){showDetail(selected);return;}
        heading.setText(tab==0?"人文":tab==1?"自然":"我的");
        city.setVisibility(tab==2?View.GONE:View.VISIBLE);
        if(tab<2)showExplore();else showProfile();
    }
    private LinearLayout scrollContent(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setSmoothScrollingEnabled(false);scroll.setFocusableInTouchMode(true);scroll.setDescendantFocusability(android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout c=column();c.setPadding(dp(22),dp(8),dp(22),dp(24));scroll.addView(c);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));scroll.requestFocus();return c;
    }
    private void saveViewport(){
        if(tab<2&&detailId==null){
            if(map!=null){MapSurface.Camera c=map.snapshot();cameraLat[tab]=Float.isNaN(c.zoom)?Double.NaN:c.point.latitude;cameraLon[tab]=c.point.longitude;cameraBd[tab]=c.point.bd09;cameraZoom[tab]=c.zoom;}
            if(contentList!=null)listOffsets[tab]=contentList.getScrollY();
        }
    }
    private void open(Attraction a){saveViewport();detailId=a.id;detail=a;render();}
    private void returnToExplore(){detailId=null;detail=null;render();}
    private void showExplore(){
        LinearLayout status=row();status.setPadding(dp(18),0,dp(18),0);status.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text(location.label(),12,false);label.setTextColor(MUTED);label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);status.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button recenter=button("◎ 定位",false);recenter.setContentDescription("回到当前位置");recenter.setTextSize(12);status.addView(recenter);recenter.setOnClickListener(v->{cameraLat[tab]=Double.NaN;location.sampleRegion=false;queryKey="";forceNext=true;ensureLocation(true);if(location.status==ExploreState.Status.READY)render();});body.addView(status);
        if(location.status!=ExploreState.Status.READY&&location.status!=ExploreState.Status.REQUESTING&&location.status!=ExploreState.Status.IDLE){
            LinearLayout actions=row();actions.setPadding(dp(18),dp(4),dp(18),0);
            Button retry=button("重试定位",false);retry.setTextSize(12);actions.addView(retry,new LinearLayout.LayoutParams(0,-2,1));retry.setOnClickListener(v->ensureLocation(true));
            Button settings=button("定位设置",false);settings.setTextSize(12);actions.addView(settings,new LinearLayout.LayoutParams(0,-2,1));settings.setOnClickListener(v->startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
            Button permission=button("应用权限",false);permission.setTextSize(12);actions.addView(permission,new LinearLayout.LayoutParams(0,-2,1));permission.setOnClickListener(v->startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));body.addView(actions);
        }
        HorizontalScrollView filter=new HorizontalScrollView(this);filter.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();chips.setPadding(dp(14),dp(8),dp(14),dp(8));
        String[] options=tab==0?new String[]{"全部","历史","美食"}:new String[]{"全部","自然景区","自驾路线","徒步路线"};
        for(String option:options){Button chip=button(option,option.equals(categories[tab]));chip.setTextSize(12);chip.setSelected(option.equals(categories[tab]));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.setMargins(dp(4),0,dp(4),0);chips.addView(chip,cp);if(option.equals(categories[tab]))chip.post(()->filter.scrollTo(Math.max(0,chip.getLeft()-dp(14)),0));chip.setOnClickListener(v->{saveViewport();repository.cancel();queryKey="";liveEntries=java.util.Collections.emptyList();poiFailure=null;categories[tab]=option;selectedIds[tab]=null;listOffsets[tab]=0;render();});}filter.addView(chips);body.addView(filter);
        map=newMap();map.view().setContentDescription("探索地图");
        MapPoint gps=MapPoint.gps(location.centerLatitude(),location.centerLongitude());
        prepareSearch(gps);
        java.util.List<Attraction> entries=entries();
        if(Double.isNaN(cameraLat[tab]))map.fitRadius(gps,PoiRepository.RADIUS_METERS);
        else map.camera(new MapPoint(cameraLat[tab],cameraLon[tab],cameraBd[tab]),(float)cameraZoom[tab]);
        map.features(entries,this::selectContent);
        if(location.status==ExploreState.Status.READY)map.currentPosition(MapPoint.gps(location.latitude,location.longitude));
        LinearLayout.LayoutParams frame=new LinearLayout.LayoutParams(-1,0,1);frame.setMargins(dp(14),0,dp(14),0);body.addView(map.view(),frame);addCredit(body);
        contentList=new ScrollView(this);contentList.setContentDescription("分类内容列表");contentList.setFillViewport(true);contentList.setSmoothScrollingEnabled(false);contentList.setFocusableInTouchMode(true);contentList.setDescendantFocusability(android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS);cards=column();cards.setPadding(dp(18),dp(4),dp(18),dp(12));contentList.addView(cards);body.addView(contentList,new LinearLayout.LayoutParams(-1,0,1));contentList.requestFocus();
        if(poiFailure!=null&&!location.sampleRegion){caption(cards,PoiRepository.errorText(poiFailure),13);Button retry=button("重试检索",false);cards.addView(retry);retry.setOnClickListener(v->{queryKey="";forceNext=true;render();});}
        if(entries.isEmpty()){
            title(cards,loading?"正在检索周边…":location.sampleRegion?"一公里内暂无本地示例":"一公里内暂无可显示的景点",18);
            caption(cards,location.sampleRegion?"本地示例仅用于离线阅读，不是实时检索。":onlineReady()?"仅显示当前位置半径 1 公里内的结果，不会自动扩大范围。":"百度在线接入待完成；请先同意 SDK 使用并配置 AK。",13);
            Button sample=button("查看杭州示例",false);cards.addView(sample);sample.setOnClickListener(v->{repository.cancel();queryKey="";poiFailure=null;location.sampleRegion=true;cameraLat[tab]=Double.NaN;listOffsets[tab]=0;render();});
        }
        for(int i=0;i<entries.size();i++){
            Attraction a=entries.get(i);LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(rounded(Color.WHITE,18));
            eyebrow(card,a.category+(a.online?" · 百度周边":" · 本地示例"));title(card,a.name,19);caption(card,a.subtitle,13);
            if(a.isRoute())caption(card,String.format(java.util.Locale.CHINA,"示例路线 · 非导航  /  折线估算 %.1f km",a.distanceKm()),11);
            Button view=button("查看详情  ↗",false);view.setContentDescription("查看"+a.name);card.addView(view);view.setOnClickListener(v->open(a));
            card.setOnClickListener(v->{selectContent(a.id);map.camera(new MapPoint(a.latitude,a.longitude,a.bd09),null);});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);cards.addView(card,p);cardViews.put(a.id,card);
        }
        updateSelection();final ScrollView list=contentList;final int offset=listOffsets[tab];list.post(()->list.scrollTo(0,offset));map.resume();
    }
    void selectContent(String id){selectedIds[tab]=id;updateSelection();LinearLayout card=cardViews.get(id);if(card!=null&&contentList!=null)contentList.smoothScrollTo(0,card.getTop());}
    private void updateSelection(){for(java.util.Map.Entry<String,LinearLayout> e:cardViews.entrySet()){GradientDrawable bg=rounded(Color.WHITE,18);boolean selected=e.getKey().equals(selectedIds[tab]);if(selected)bg.setStroke(dp(2),TEAL);e.getValue().setBackground(bg);e.getValue().setSelected(selected);}}
    private boolean privacyGranted(){return tests!=null?tests.consent:getPreferences(MODE_PRIVATE).getBoolean("baiduPrivacy",false);}
    private boolean onlineReady(){return tests!=null?tests.online&&tests.consent:privacyGranted()&&BaiduRuntime.state==BaiduRuntime.State.READY;}
    private MapSurface newMap(){
        if(tests!=null&&tests.maps!=null)return tests.maps.apply(this);
        if(privacyGranted()&&BaiduRuntime.initialized)return new BaiduMapSurface(this);
        return new UnavailableMapSurface(this,privacyGranted()?BaiduRuntime.message():"百度地图尚未启用\n可阅读文档与本地示例文本");
    }
    private void addCredit(LinearLayout parent){
        TextView credit=text(onlineReady()?"百度地图 · 1 公里周边 · 中性等大圆点":"在线地图未就绪 · 示例与真实检索明确区分",10,false);credit.setTextColor(MUTED);credit.setPadding(dp(18),dp(5),dp(18),dp(5));parent.addView(credit);
        if(tests==null&&BaiduRuntime.initialized&&BaiduRuntime.state!=BaiduRuntime.State.READY)caption(parent,BaiduRuntime.message(),12);
    }
    private String section(){return tab==0?"人文":"自然";}
    private void prepareSearch(MapPoint gps){
        if(!onlineReady()||location.sampleRegion||location.status!=ExploreState.Status.READY||categories[tab].endsWith("路线")){loading=false;return;}
        MapPoint center=map.project(gps);
        final String key=String.format(java.util.Locale.ROOT,"%.6f/%.6f/%s/%s",center.latitude,center.longitude,section(),categories[tab]);
        if(key.equals(queryKey))return;
        queryKey=key;final long generation=++queryGeneration;loading=true;poiFailure=null;liveEntries=java.util.Collections.emptyList();boolean refresh=forceNext;forceNext=false;
        repository.load(center,section(),categories[tab],refresh,new PoiProvider.Callback(){
            public void success(java.util.List<Attraction> places){ui.post(()->{if(generation!=queryGeneration||!key.equals(queryKey)||isFinishing())return;liveEntries=places;loading=false;poiFailure=null;if(tab<2&&detailId==null){saveViewport();render();}});}
            public void failure(PoiProvider.Failure reason){ui.post(()->{if(generation!=queryGeneration||!key.equals(queryKey)||isFinishing())return;liveEntries=java.util.Collections.emptyList();loading=false;poiFailure=reason;if(tab<2&&detailId==null){saveViewport();render();}});}
        });
    }
    private java.util.List<Attraction> entries(){
        java.util.List<Attraction> result=new java.util.ArrayList<>();
        if(location.sampleRegion)return location.nearby(section(),categories[tab]);
        if(onlineReady()&&!categories[tab].endsWith("路线"))result.addAll(liveEntries);
        for(Attraction a:location.nearby(section(),categories[tab]))if(a.isRoute())result.add(a);
        return result;
    }
    private void consentDialog(){
        privacyPrompted=true;
        android.text.Spanned text=android.text.Html.fromHtml("启用百度地图后，SDK 将为地图和周边查询处理位置及必要设备信息，查询会发送至百度，不经过本应用后台。<br/><br/><a href=\"https://lbs.baidu.com/index.php?title=openprivacy\">百度地图开放平台隐私政策</a><br/><br/>定位权限另行申请。暂不使用仍可阅读文档与本地示例。",android.text.Html.FROM_HTML_MODE_LEGACY);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("百度地图使用说明").setMessage(text).setCancelable(false)
            .setPositiveButton("同意并使用地图",(d,w)->{if(tests!=null)tests.consent=true;else{getPreferences(MODE_PRIVATE).edit().putBoolean("baiduPrivacy",true).putBoolean("privacyChoiceMade",true).apply();BaiduRuntime.start(this);}queryKey="";render();ensureLocation(false);})
            .setNegativeButton("暂不使用",(d,w)->{if(tests!=null)tests.consent=false;else getPreferences(MODE_PRIVATE).edit().putBoolean("privacyChoiceMade",true).apply();location.sampleRegion=true;render();}).create();
        dialog.show();android.widget.TextView message=dialog.findViewById(android.R.id.message);if(message!=null)message.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }
    private void showDetail(Attraction a){
        heading.setText(a.name);LinearLayout c=scrollContent();
        Button back=button("‹  返回探索",false);back.setBackground(ripple(Color.TRANSPARENT,16));back.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);c.addView(back);back.setOnClickListener(v->returnToExplore());
        if(a.isRoute()){
            space(c,8);title(c,"示例路线 · 非导航",20);caption(c,String.format(java.util.Locale.CHINA,"折线估算 %.1f km；不代表实际道路或步行距离。",a.distanceKm()),14);
            map=newMap();map.view().setContentDescription("路线预览地图");map.route(a.waypoints);
            c.addView(map.view(),new LinearLayout.LayoutParams(-1,dp(200)));addCredit(c);map.resume();
            for(int i=0;i<a.waypoints.size();i++)caption(c,String.format(java.util.Locale.CHINA,"%d  %s",i+1,a.waypoints.get(i).name),14);
        }else if(!a.online){
        int index=Attraction.SAMPLES.indexOf(a)%3;TraceArt art=new TraceArt(this,index);art.setBackground(rounded(PALE,22));art.setClipToOutline(true);c.addView(art,new LinearLayout.LayoutParams(-1,dp(175)));
        }
        space(c,20);eyebrow(c,a.online?"百度周边 / 实时检索":"杭州示例 / 本地内容");title(c,a.subtitle,24);if(!a.introduction.equals(a.subtitle))caption(c,a.introduction,16);
        space(c,22);LinearLayout article=column();article.setPadding(dp(18),dp(20),dp(18),dp(20));article.setBackground(rounded(Color.WHITE,22));
        eyebrow(article,"READ THE CITY");title(article,a.online?"地点介绍":"文字讲解",20);TextView narration=caption(article,a.narration,17);narration.setTextColor(INK);narration.setLineSpacing(dp(7),1);narration.setTextIsSelectable(true);c.addView(article);
        if(!a.manager.isEmpty()){
            space(c,18);title(c,"管理与来源",18);caption(c,a.manager,14);TextView link=caption(c,a.source,13);android.text.util.Linkify.addLinks(link,android.text.util.Linkify.WEB_URLS);link.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        space(c,12);caption(c,a.online?"来源：百度地图 POI · 排名与热度暂无可用官方数据":"杭州内置示例 · 内容不随位置自动更新",12);
    }
    private void showProfile(){
        LinearLayout c=scrollContent();eyebrow(c,"TRACE / DOCUMENTATION");title(c,"应用说明",28);caption(c,"认识寻迹，开始你的城市探索。",15);
        document(c,"使用指南","人文与自然首页以当前位置为中心展示地图。横向切换分类，地图标记与卡片同步筛选。轻点标记选中卡片，点击查看详情阅读内容。◎ 定位可回到当前位置。点击卡片空白处可在地图中聚焦。");
        document(c,"分类说明","人文包含历史、美食；自然包含自然景区、自驾路线、徒步路线。路线是静态示意，不是实测轨迹或导航，折线距离不代表真实行程距离。");
        Button enable=button("百度地图使用与配置",false);c.addView(enable);enable.setOnClickListener(v->{if(!privacyGranted())consentDialog();else new android.app.AlertDialog.Builder(this).setTitle("百度地图配置").setMessage(tests!=null?"测试数据，非在线验证":BaiduRuntime.message()+"\n配置步骤见仓库 Android 文档。Android AK 必须绑定包名及 APK 签名 SHA-1。定位授权并不等同于 SDK 使用同意。").setPositiveButton("知道了",null).show();});
        document(c,"定位与隐私","只申请前台定位，支持大致位置，不在后台跟踪，不保存定位历史、不上传至业务后台。定位失败时明确标注杭州示例区域。同意百度 SDK 使用后，位置与必要设备信息会由 SDK 向百度发送以提供地图和检索服务；不经过本应用后台。未同意前不初始化 SDK。");
        document(c,"百度周边与圆点", "百度 Android SDK 9.0.0 直接检索当前位置半径 1000 米的周边地点，无业务后台。检索最多显示 100 条，按 UID 去重及距离排序。同一位置与分类缓存五分钟，定位按钮可刷新。拖动和缩放只改变视野，不改变检索中心。SDK 未提供官方榜单排名和近七天热度，本阶段统一使用等大圆点，不用评分、评论数或搜索顺序冒充热度。");
        document(c,"示例数据与地图","真实景点来自百度周边检索；示例模式仅提供杭州本地内容，两者明确标注。自驾、徒步是本地示例路线，按起点是否在一公里内筛选，不是百度规划路线。查看杭州示例只改变探索区域，不表示您已位于杭州。文本可离线阅读，底图来自百度地图，需有效 AK、SDK 使用同意和网络。没有 AK 时仅可阅读示例与文档，在线接入待验证。官方来源链接与景区公告用于核对信息，不提供实时营业、路况或通行保证。");
        document(c,"版本信息","寻迹 Android 0.4.0（4）\n无账号、无业务后台；个人试用的调试版本。地图与检索：百度地图 SDK 9.0.0。");
    }
    private void document(LinearLayout c,String title,String description){space(c,16);LinearLayout panel=column();panel.setPadding(dp(16),dp(10),dp(16),dp(16));panel.setBackground(rounded(Color.WHITE,20));title(panel,title,18);caption(panel,description,15);c.addView(panel);}
    private boolean permitted(){return checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED||checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    private void ensureLocation(boolean explicit){
        if(tab==2||!privacyGranted())return;
        if(location.status==ExploreState.Status.REQUESTING)return;
        if(tests==null&&!permitted()){
            if(explicit||!getPreferences(MODE_PRIVATE).getBoolean("askedLocation",false)){
                location.status=ExploreState.Status.REQUESTING;location.sampleRegion=true;render();getPreferences(MODE_PRIVATE).edit().putBoolean("askedLocation",true).apply();
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},73);
            }else{location.fail(ExploreState.Status.DENIED);render();}return;
        }
        if(tests==null){android.location.LocationManager manager=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);if(manager==null||(!manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)&&!manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))){location.fail(ExploreState.Status.DISABLED);render();return;}}
        if(location.status==ExploreState.Status.READY&&android.os.SystemClock.elapsedRealtime()-fixTime<120_000){if(explicit){location.sampleRegion=false;render();}return;}
        location.status=ExploreState.Status.REQUESTING;location.sampleRegion=true;render();final int generation=++locationGeneration;
        source.start(new LocationSource.Callback(){
            @Override public void ready(double lat,double lon,boolean approximate){runOnUiThread(()->{if(generation!=locationGeneration)return;location.ready(lat,lon,approximate);fixTime=android.os.SystemClock.elapsedRealtime();if(tab<2&&detailId==null)render();});}
            @Override public void failed(ExploreState.Status reason){runOnUiThread(()->{if(generation!=locationGeneration)return;location.fail(reason);if(tab<2&&detailId==null)render();});}
        });
    }
    private void stopLocation(){locationGeneration++;if(source!=null)source.cancel();if(location.status==ExploreState.Status.REQUESTING)location.status=ExploreState.Status.IDLE;}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code!=73)return;location.status=ExploreState.Status.IDLE;if(tab==2)return;if(permitted())ensureLocation(false);else{location.fail(ExploreState.Status.DENIED);render();}}
    @Override protected void onStart(){super.onStart();if(privacyGranted()){if(tests==null)BaiduRuntime.start(this);if(tab<2)ensureLocation(false);}else if(!privacyPrompted&&(tests!=null||!getPreferences(MODE_PRIVATE).getBoolean("privacyChoiceMade",false)))consentDialog();}
    @Override protected void onStop(){saveViewport();repository.cancel();queryKey="";stopLocation();super.onStop();}
    @Override public void onBackPressed(){if(detailId!=null){returnToExplore();}else if(tab!=0){tab=0;cameraLat[0]=Double.NaN;render();ensureLocation(false);}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle s){saveViewport();if(detail!=null&&detail.online){s.putBoolean("detailOnline",true);s.putString("detailUid",detail.id.substring(6));s.putString("detailName",detail.name);s.putString("detailAddress",detail.subtitle);s.putDouble("detailLat",detail.latitude);s.putDouble("detailLon",detail.longitude);s.putString("detailSection",detail.section);s.putString("detailCategory",detail.category);}s.putString("locationStatus",location.status.name());s.putDouble("fixLat",location.latitude);s.putDouble("fixLon",location.longitude);s.putBoolean("approximate",location.approximate);s.putBoolean("sampleRegion",location.sampleRegion);s.putLong("fixTime",fixTime);s.putInt("tab",tab);s.putString("detail",detailId);for(int i=0;i<2;i++){s.putString("category"+i,categories[i]);s.putString("selected"+i,selectedIds[i]);s.putDouble("lat"+i,cameraLat[i]);s.putDouble("lon"+i,cameraLon[i]);s.putDouble("zoom"+i,cameraZoom[i]);s.putBoolean("bd"+i,cameraBd[i]);s.putInt("offset"+i,listOffsets[i]);}super.onSaveInstanceState(s);}
    @Override protected void onResume(){super.onResume();if(map!=null)map.resume();}
    @Override protected void onPause(){if(map!=null)map.pause();super.onPause();}
    @Override protected void onDestroy(){BaiduRuntime.remove(sdkChanged);repository.cancel();stopLocation();disposeMap();super.onDestroy();}
    private void disposeMap(){if(map!=null){map.pause();map.destroy();map=null;}}
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
