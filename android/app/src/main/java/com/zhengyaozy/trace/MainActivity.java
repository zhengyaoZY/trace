package com.zhengyaozy.trace;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.io.File;

public class MainActivity extends Activity {
    private static final int TEAL = Color.rgb(0, 122, 110);
    private static final int INK = Color.rgb(25, 48, 45);
    private LinearLayout root, body, navigation;
    private TextView heading;
    private MapView map;
    private int tab = 0;
    private String detailId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Configuration.getInstance().setUserAgentValue(getPackageName()+"/0.1.0");
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "maps"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "maps/tiles"));
        root = column(); root.setBackgroundColor(Color.rgb(245, 250, 248));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        heading = text("寻迹", 26, true); heading.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.addView(heading);
        body = column(); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        navigation = new LinearLayout(this); navigation.setBackgroundColor(Color.WHITE);
        root.addView(navigation); setContentView(root);
        if (state != null) { tab=state.getInt("tab"); detailId=state.getString("detail"); }
        render();
    }

    private void render() {
        disposeMap(); body.removeAllViews(); navigation.removeAllViews();
        String[] tabs={"发现", "地图", "我的"};
        for (int i=0;i<tabs.length;i++) {
            final int index=i;
            Button b = button(tabs[i]); b.setTextColor(i==tab?TEAL:Color.DKGRAY);
            b.setTypeface(null, i==tab?Typeface.BOLD:Typeface.NORMAL);
            b.setContentDescription(tabs[i]+(i==tab?"，已选中":""));
            navigation.addView(b,new LinearLayout.LayoutParams(0,dp(60),1));
            b.setOnClickListener(v->{tab=index;detailId=null;render();});
        }
        Attraction selected=Attraction.SAMPLES.stream().filter(a->a.id.equals(detailId)).findFirst().orElse(null);
        if (selected!=null) { showDetail(selected); return; }
        heading.setText(tab==0?"寻迹":tab==1?"杭州地图":"我的");
        if(tab==0) showDiscover(); else if(tab==1) showMap(); else showProfile();
    }

    private LinearLayout scrollContent() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content=column(); content.setPadding(dp(20),dp(8),dp(20),dp(24));
        scroll.addView(content); body.addView(scroll,new LinearLayout.LayoutParams(-1,-1)); return content;
    }
    private void showDiscover() {
        LinearLayout c=scrollContent();
        addText(c,"在杭州，循着故事出发",28,true);
        addText(c,"看一处风景，读一段城市往事。",16,false);
        TextView city=text("杭州 · 精选示例景点",14,true); city.setTextColor(TEAL); c.addView(city);
        for (Attraction a:Attraction.SAMPLES) {
            LinearLayout card=column(); card.setPadding(dp(20),dp(20),dp(20),dp(20));
            card.setBackground(rounded(Color.WHITE,20));
            TextView illustration=text(a.name.equals("西湖")?"山水之间":a.name.equals("雷峰塔")?"夕照寻踪":"运河漫步",24,true);
            illustration.setTextColor(TEAL); illustration.setGravity(Gravity.CENTER);
            illustration.setBackground(rounded(Color.rgb(220,240,234),14));
            card.addView(illustration,new LinearLayout.LayoutParams(-1,dp(110)));
            addText(card,a.name,22,true); addText(card,a.subtitle,16,false);
            Button open=button("探索景点  →"); open.setContentDescription("探索"+a.name); card.addView(open); open.setOnClickListener(v->open(a));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(18); c.addView(card,p);
        }
        addText(c,"示例内容；开放时间与票务以景区公告为准。",13,false);
    }
    private void open(Attraction a) { detailId=a.id; render(); }
    private void showDetail(Attraction a) {
        heading.setText(a.name); LinearLayout c=scrollContent();
        Button back=button("‹ 返回" ); c.addView(back); back.setOnClickListener(v->{detailId=null;render();});
        addText(c,a.subtitle,22,true); addText(c,a.introduction,17,false);
        addText(c,"文字讲解",22,true); TextView narration=addText(c,a.narration,18,false);
        narration.setTextIsSelectable(true); narration.setLineSpacing(dp(7),1);
        Button location=button("在地图中查看"); c.addView(location);
        location.setOnClickListener(v->{tab=1;detailId=null;render();map.getController().setZoom(15.0);map.getController().setCenter(new GeoPoint(a.latitude,a.longitude));});
        addText(c,"讲解为内置示例文本，不含音频。",13,false);
    }
    private void showMap() {
        addText(body,"点击景点标记查看详情。地图需联网加载。",14,false);
        map=new MapView(this); map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true); map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(30.275,120.145));
        for(Attraction a:Attraction.SAMPLES) {
            Marker m=new Marker(map); m.setPosition(new GeoPoint(a.latitude,a.longitude));
            m.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM); m.setTitle(a.name);
            m.setOnMarkerClickListener((marker,view)->{open(a);return true;}); map.getOverlays().add(m);
        }
        body.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        TextView credit=text("© OpenStreetMap contributors · 数据遵循 ODbL",12,false);
        credit.setPadding(dp(12),dp(8),dp(12),dp(8)); body.addView(credit);
        // Text links also make every destination accessible without interacting with the map.
        LinearLayout places=new LinearLayout(this);
        for(Attraction a:Attraction.SAMPLES){ Button b=button(a.name);places.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->open(a)); }
        body.addView(places); map.onResume();
    }
    private void showProfile() {
        LinearLayout c=scrollContent(); addText(c,"你好，城市探索者",26,true);
        addText(c,"从一段步行开始，发现身边的故事。",18,false);
        addText(c,"关于寻迹",22,true); addText(c,"当前城市：杭州\n版本：0.1.0",17,false);
        addText(c,"本版本展示内置示例景点与文字讲解，无需登录，不接入业务后台，不请求定位权限。",17,false);
        addText(c,"地图通过 HTTPS 从 OpenStreetMap 加载，会向地图服务发送网络请求。离线仍可阅读全部景点文字，地图可用性取决于缓存。",16,false);
    }
    @Override public void onBackPressed() { if(detailId!=null){detailId=null;render();}else if(tab!=0){tab=0;render();}else super.onBackPressed(); }
    @Override protected void onSaveInstanceState(Bundle s){s.putInt("tab",tab);s.putString("detail",detailId);super.onSaveInstanceState(s);}
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
    @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
    @Override protected void onDestroy(){disposeMap();super.onDestroy();}
    private void disposeMap(){if(map!=null){map.onPause();map.onDetach();map=null;}}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int sp,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(INK);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private TextView addText(LinearLayout parent,String value,int sp,boolean bold){TextView t=text(value,sp,bold);t.setPadding(0,dp(14),0,dp(12));parent.addView(t);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(TEAL);b.setAllCaps(false);b.setMinHeight(dp(48));return b;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
