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

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Configuration.getInstance().setUserAgentValue(getPackageName()+"/0.2.0");
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(),"maps"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(),"maps/tiles"));
        root=column();root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        toolbar=row();toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(22),dp(12),dp(22),dp(12));
        heading=text("寻迹",23,true);toolbar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        city=text("杭州  ·  HANGZHOU",11,true);city.setTextColor(TEAL);city.setPadding(dp(12),dp(9),dp(12),dp(9));city.setBackground(rounded(PALE,30));toolbar.addView(city);root.addView(toolbar);
        body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        View divider=new View(this);divider.setBackgroundColor(Color.rgb(226,233,225));root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        navigation=row();navigation.setPadding(dp(16),dp(8),dp(16),dp(8));navigation.setBackgroundColor(Color.WHITE);root.addView(navigation);
        setContentView(root);
        if(state!=null){tab=state.getInt("tab");detailId=state.getString("detail");}render();
    }
    private void render(){
        disposeMap();body.removeAllViews();navigation.removeAllViews();
        String[] tabs={"发现","地图","我的"};
        for(int i=0;i<3;i++){
            final int index=i;Button b=button(tabs[i],false);b.setTextSize(11);b.setTextColor(i==tab?TEAL:MUTED);
            b.setBackground(ripple(i==tab?PALE:Color.WHITE,18));b.setPadding(dp(8),dp(7),dp(8),dp(7));
            TraceArt.Icon icon=new TraceArt.Icon(i,i==tab?TEAL:MUTED);icon.setBounds(0,0,dp(23),dp(23));b.setCompoundDrawables(null,icon,null,null);b.setCompoundDrawablePadding(dp(4));
            b.setContentDescription(tabs[i]+(i==tab?"，已选中":""));b.setSelected(i==tab);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(5),0,dp(5),0);navigation.addView(b,p);
            b.setOnClickListener(v->{tab=index;detailId=null;render();});
        }
        Attraction selected=Attraction.SAMPLES.stream().filter(a->a.id.equals(detailId)).findFirst().orElse(null);
        city.setVisibility(selected==null?View.VISIBLE:View.GONE);
        if(selected!=null){showDetail(selected);return;}
        heading.setText(tab==0?"寻迹":tab==1?"杭州地图":"我的");
        if(tab==0)showDiscover();else if(tab==1)showMap();else showProfile();
    }
    private LinearLayout scrollContent(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout c=column();c.setPadding(dp(22),dp(8),dp(22),dp(24));scroll.addView(c);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));return c;
    }
    private void showDiscover(){
        LinearLayout c=scrollContent();eyebrow(c,"WANDER · 城市漫游");
        title(c,"在杭州，\n循着故事出发",30);caption(c,"看一处风景，读一段城市往事。",15);
        LinearLayout section=row();section.setGravity(Gravity.CENTER_VERTICAL);section.setPadding(0,dp(24),0,dp(14));
        TextView selected=text("精选目的地",18,true);section.addView(selected,new LinearLayout.LayoutParams(0,-2,1));TextView count=text("03 处风景",12,false);count.setTextColor(MUTED);section.addView(count);c.addView(section);
        for(int i=0;i<Attraction.SAMPLES.size();i++){
            Attraction a=Attraction.SAMPLES.get(i);LinearLayout card=column();card.setBackground(rounded(Color.WHITE,22));card.setClipToOutline(true);
            TraceArt art=new TraceArt(this,i);card.addView(art,new LinearLayout.LayoutParams(-1,dp(158)));
            LinearLayout content=column();content.setPadding(dp(18),dp(16),dp(18),dp(16));card.addView(content);
            eyebrow(content,String.format(java.util.Locale.ROOT,"0%d  /  %s",i+1,i==0?"湖畔漫游":i==1?"人文寻踪":"运河日常"));
            title(content,a.name,23);caption(content,a.subtitle,14);
            Button open=button("探索景点   ↗",false);open.setContentDescription("探索"+a.name);open.setOnClickListener(v->open(a));
            LinearLayout.LayoutParams action=new LinearLayout.LayoutParams(-1,-2);action.topMargin=dp(14);content.addView(open,action);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(20);c.addView(card,p);
        }
        caption(c,"慢一点，城市的故事就在身边。\n示例内容 · 开放与票务以景区公告为准",12);
    }
    private void open(Attraction a){detailId=a.id;render();}
    private void showDetail(Attraction a){
        heading.setText(a.name);LinearLayout c=scrollContent();
        Button back=button("‹  返回探索",false);back.setBackground(ripple(Color.TRANSPARENT,16));back.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);c.addView(back);back.setOnClickListener(v->{detailId=null;render();});
        int index=Attraction.SAMPLES.indexOf(a);TraceArt art=new TraceArt(this,index);art.setBackground(rounded(PALE,22));art.setClipToOutline(true);c.addView(art,new LinearLayout.LayoutParams(-1,dp(175)));
        space(c,20);eyebrow(c,"杭州  /  城市故事");title(c,a.subtitle,24);caption(c,a.introduction,16);
        space(c,22);LinearLayout article=column();article.setPadding(dp(18),dp(20),dp(18),dp(20));article.setBackground(rounded(Color.WHITE,22));
        eyebrow(article,"READ THE CITY");title(article,"文字讲解",20);TextView narration=caption(article,a.narration,17);narration.setTextColor(INK);narration.setLineSpacing(dp(7),1);narration.setTextIsSelectable(true);c.addView(article);
        space(c,18);Button location=button("在地图中查看   ↗",true);c.addView(location);location.setOnClickListener(v->{tab=1;detailId=null;render();map.getController().setZoom(15.0);map.getController().setCenter(new GeoPoint(a.latitude,a.longitude));});
        space(c,12);caption(c,"内置示例讲解 · 文字阅读，无音频",12);
    }
    private void showMap(){
        LinearLayout intro=column();intro.setPadding(dp(22),dp(4),dp(22),dp(14));eyebrow(intro,"EXPLORE HANGZHOU");caption(intro,"三处风景，一座城的不同侧面。",14);body.addView(intro);
        map=new MapView(this);map.setTileSource(TileSourceFactory.MAPNIK);map.setMultiTouchControls(true);map.setBuiltInZoomControls(false);map.getController().setZoom(11.0);map.getController().setCenter(new GeoPoint(30.275,120.145));
        map.setBackground(rounded(PALE,22));map.setClipToOutline(true);
        for(int i=0;i<Attraction.SAMPLES.size();i++){
            Attraction a=Attraction.SAMPLES.get(i);Marker marker=new Marker(map);marker.setPosition(new GeoPoint(a.latitude,a.longitude));marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);marker.setTitle(a.name);marker.setIcon(markerIcon(i+1));marker.setOnMarkerClickListener((m,v)->{open(a);return true;});map.getOverlays().add(marker);
        }
        LinearLayout.LayoutParams frame=new LinearLayout.LayoutParams(-1,0,1);frame.setMargins(dp(14),0,dp(14),0);body.addView(map,frame);
        TextView credit=text("",10,false);credit.setText(android.text.Html.fromHtml("© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a> · ODbL",android.text.Html.FROM_HTML_MODE_LEGACY));credit.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());credit.setLinkTextColor(MUTED);credit.setPadding(dp(22),dp(8),dp(22),dp(8));body.addView(credit);
        LinearLayout places=row();places.setPadding(dp(16),0,dp(16),dp(8));for(Attraction a:Attraction.SAMPLES){Button b=button(a.name,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(4),0,dp(4),0);places.addView(b,p);b.setOnClickListener(v->open(a));}body.addView(places);
        TextView hint=text("轻点标记，阅读故事 · 底图需联网",11,false);hint.setTextColor(MUTED);hint.setGravity(Gravity.CENTER);hint.setPadding(0,0,0,dp(12));body.addView(hint);map.onResume();
    }
    private Drawable markerIcon(int number){
        Bitmap image=Bitmap.createBitmap(dp(38),dp(46),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);c.scale(image.getWidth()/38f,image.getHeight()/46f);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(TEAL);Path point=new Path();point.moveTo(12,30);point.lineTo(19,44);point.lineTo(26,30);point.close();c.drawPath(point,p);c.drawCircle(19,18,17,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(19,18,14,p);p.setStyle(Paint.Style.FILL);p.setTextSize(14);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(String.valueOf(number),19,23,p);return new BitmapDrawable(getResources(),image);
    }
    private void showProfile(){
        LinearLayout c=scrollContent();space(c,10);TraceArt.Icon icon=new TraceArt.Icon(0,TEAL);ImageView avatar=new ImageView(this);avatar.setImageDrawable(icon);avatar.setPadding(dp(16),dp(16),dp(16),dp(16));avatar.setBackground(rounded(PALE,24));avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);c.addView(avatar,new LinearLayout.LayoutParams(dp(72),dp(72)));
        space(c,20);eyebrow(c,"YOUR CITY COMPANION");title(c,"你好，城市探索者",26);caption(c,"不赶路，去感受一座城。",15);space(c,24);
        LinearLayout stats=row();stats.setPadding(dp(18),dp(18),dp(18),dp(18));stats.setBackground(rounded(TEAL,22));
        for(String[] pair:new String[][]{{"03","内置景点"},{"杭州","当前城市"}}){LinearLayout v=column();TextView value=text(pair[0],25,true);value.setTextColor(Color.WHITE);v.addView(value);TextView label=text(pair[1],12,false);label.setTextColor(Color.rgb(203,224,209));label.setPadding(0,dp(6),0,0);v.addView(label);stats.addView(v,new LinearLayout.LayoutParams(0,-2,1));}c.addView(stats);space(c,24);
        title(c,"关于寻迹",18);space(c,12);LinearLayout info=column();info.setPadding(dp(18),dp(8),dp(18),dp(16));info.setBackground(rounded(Color.WHITE,22));
        title(info,"轻装出发",16);caption(info,"无需登录，不接入业务后台，不请求定位权限。",14);space(info,8);title(info,"随时阅读",16);caption(info,"景点介绍与文字讲解内置，离线也能读懂杭州。",14);c.addView(info);
        space(c,20);caption(c,"地图由 OpenStreetMap 提供，加载底图时会发送网络请求。离线地图可用性取决于缓存。",12);space(c,20);eyebrow(c,"寻迹 TRACE  /  0.2.0");
    }
    @Override public void onBackPressed(){if(detailId!=null){detailId=null;render();}else if(tab!=0){tab=0;render();}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle s){s.putInt("tab",tab);s.putString("detail",detailId);super.onSaveInstanceState(s);}
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
    @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
    @Override protected void onDestroy(){disposeMap();super.onDestroy();}
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
