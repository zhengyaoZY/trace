package com.zhengyaozy.trace;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
@RunWith(AndroidJUnit4.class)
public class NavigationTest {
    private void screenshot(String name) {
        android.app.Instrumentation instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        java.util.concurrent.CountDownLatch frames=new java.util.concurrent.CountDownLatch(1);
        instrumentation.runOnMainSync(()->android.view.Choreographer.getInstance().postFrameCallback(t->android.view.Choreographer.getInstance().postFrameCallback(next->frames.countDown())));
        try{org.junit.Assert.assertTrue(frames.await(5,java.util.concurrent.TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
        android.graphics.Bitmap image = instrumentation.getUiAutomation().takeScreenshot();
        org.junit.Assert.assertNotNull(image);
        java.io.File directory = new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null), "screenshots");
        directory.mkdirs();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(directory, name+".png"))) {
            org.junit.Assert.assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        finally { image.recycle(); }
    }

    @org.junit.Before public void locationFixture(){MainActivity.testLocationSource=()->fix(30.25,120.15,false);}
    @org.junit.After public void clearFixture(){MainActivity.testLocationSource=null;}
    private LocationSource fix(double lat,double lon,boolean approximate){return new LocationSource(){public void start(Callback cb){cb.ready(lat,lon,approximate);}public void cancel(){}};}
    private void center(double lat,double lon){onView(withContentDescription("探索地图")).check((v,e)->{if(e!=null)throw e;org.osmdroid.views.MapView m=(org.osmdroid.views.MapView)v;org.junit.Assert.assertEquals(lat,m.getMapCenter().getLatitude(),0.002);org.junit.Assert.assertEquals(lon,m.getMapCenter().getLongitude(),0.002);});}
    @Test public void sectionsFiltersDetailsAndDocumentation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("当前位置")).check(matches(isDisplayed()));center(30.25,120.15);screenshot("culture");
            onView(withText("美食")).perform(click());onView(withText("河坊街风味")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withContentDescription("查看河坊街风味")).perform(scrollTo(),click());
            onView(withText("从一碗片儿川认识杭州")).check(matches(isDisplayed()));Espresso.pressBack();
            onView(withText("美食")).check(matches(isSelected()));center(30.25,120.15);
            onView(withText("历史")).perform(click());
            onView(withContentDescription("探索地图")).perform(new androidx.test.espresso.action.GeneralClickAction(androidx.test.espresso.action.Tap.SINGLE,v->{
                org.osmdroid.views.MapView m=(org.osmdroid.views.MapView)v;android.graphics.Point p=m.getProjection().toPixels(new org.osmdroid.util.GeoPoint(30.2313,120.1489),null);int[] origin=new int[2];v.getLocationOnScreen(origin);return new float[]{origin[0]+p.x,origin[1]+p.y-18*v.getResources().getDisplayMetrics().density};
            },androidx.test.espresso.action.Press.FINGER));
            onView(withContentDescription("探索地图")).perform(new androidx.test.espresso.ViewAction(){
                public org.hamcrest.Matcher<android.view.View> getConstraints(){return isAssignableFrom(org.osmdroid.views.MapView.class);}
                public String getDescription(){return "等待地图单击确认（系统双击判定窗口）";}
                public void perform(androidx.test.espresso.UiController ui,android.view.View v){ui.loopMainThreadForAtLeast(android.view.ViewConfiguration.getDoubleTapTimeout()+50);}
            });
            onView(withText("雷峰塔")).check(matches(withParent(isSelected())));
            onView(withContentDescription("查看雷峰塔")).perform(scrollTo(),click());screenshot("detail");Espresso.pressBack();
            onView(withText("历史")).check(matches(isSelected()));
            onView(withText("自然")).perform(click());center(30.25,120.15);screenshot("nature");
            onView(withText("政府管理景区")).perform(scrollTo(),click());onView(withText("西湖")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("自驾路线")).perform(scrollTo(),click());onView(withContentDescription("查看西湖外围自驾示意")).perform(scrollTo(),click());
            onView(withText("示例路线 · 非导航")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withContentDescription("路线预览地图")).perform(scrollTo()).check((v,e)->{if(e!=null)throw e;org.osmdroid.views.MapView m=(org.osmdroid.views.MapView)v;org.junit.Assert.assertTrue(m.getOverlays().get(0) instanceof org.osmdroid.views.overlay.Polyline);});screenshot("route");Espresso.pressBack();
            onView(withText("徒步路线")).perform(scrollTo(),click());onView(withText("九溪徒步示意")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("我的")).perform(click());onView(withText("应用说明")).check(matches(isDisplayed()));screenshot("documentation");
            onView(withText("定位与隐私")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    private org.osmdroid.views.MapView findMap(android.view.View view){
        if(view instanceof org.osmdroid.views.MapView)return (org.osmdroid.views.MapView)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){org.osmdroid.views.MapView found=findMap(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    @Test public void categoryAndCameraSurviveRecreation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("美食")).perform(click());
            scenario.onActivity(a->{org.osmdroid.views.MapView m=findMap(a.findViewById(android.R.id.content));m.getController().setCenter(new org.osmdroid.util.GeoPoint(30.245,120.16));m.getController().setZoom(13.0);});
            scenario.recreate();onView(withText("美食")).check(matches(isSelected()));center(30.245,120.16);
            onView(withContentDescription("查看河坊街风味")).perform(scrollTo(),click());Espresso.pressBack();center(30.245,120.16);
            onView(withText("自然")).perform(click());center(30.25,120.15);
        }
    }
    @Test public void outsideRegionAndExplicitSampleDoNotReplaceActualPosition(){
        MainActivity.testLocationSource=()->fix(39.9,116.4,true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            center(39.9,116.4);onView(withText("附近暂无示例内容")).check(matches(isDisplayed()));screenshot("outside");
            onView(withText("查看杭州示例")).perform(scrollTo(),click());onView(withText("杭州示例区域 · 非当前位置")).check(matches(isDisplayed()));
            onView(withContentDescription("回到当前位置")).perform(click());center(39.9,116.4);onView(withText("当前位置 · 大致位置")).check(matches(isDisplayed()));
        }
    }
    @Test public void failedFixesStillAllowSampleBrowsing(){
        for(ExploreState.Status reason:new ExploreState.Status[]{ExploreState.Status.DENIED,ExploreState.Status.DISABLED,ExploreState.Status.TIMEOUT}){
            MainActivity.testLocationSource=()->new LocationSource(){public void start(Callback cb){cb.failed(reason);}public void cancel(){}};
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                onView(withText("重试定位")).check(matches(isDisplayed()));onView(withText("美食")).perform(click());
                onView(withText("河坊街风味")).perform(scrollTo()).check(matches(isDisplayed()));screenshot("fallback-"+reason);
                onView(withText("我的")).perform(click());onView(withText("应用说明")).check(matches(isDisplayed()));
            }
        }
    }
}
