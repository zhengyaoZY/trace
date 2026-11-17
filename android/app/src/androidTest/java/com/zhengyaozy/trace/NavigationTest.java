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

    private MainActivity.TestDependencies deps;
    private final java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
    @org.junit.Before public void fixtures(){
        FakeMapSurface.created.clear();deps=new MainActivity.TestDependencies();deps.location=()->fix(30.24,120.15,false);deps.maps=FakeMapSurface::new;deps.pois=provider(null);MainActivity.tests=deps;
    }
    @org.junit.After public void clear(){MainActivity.tests=null;FakeMapSurface.created.clear();}
    private LocationSource fix(double lat,double lon,boolean approximate){return new LocationSource(){public void start(Callback cb){cb.ready(lat,lon,approximate);}public void cancel(){}};}
    private PoiProvider provider(PoiProvider.Failure error){return (center,section,category,cb)->{
        requests.incrementAndGet();
        if(error!=null)cb.failure(error);else{
            java.util.List<Attraction> entries=new java.util.ArrayList<>();
            if(category.equals("全部")||category.equals("历史")||section.equals("自然"))entries.add(Attraction.poi("near",section.equals("自然")?"模拟自然景点":"模拟历史景点","测试地址","",center.latitude+.001,center.longitude,section,section.equals("自然")?"自然景区":"历史"));
            if(section.equals("人文")&&(category.equals("全部")||category.equals("美食")))entries.add(Attraction.poi("food","模拟美食地点","测试餐饮地址","美食",center.latitude+.002,center.longitude,section,"美食"));
            entries.add(Attraction.poi("far","超出一公里地点","测试","",center.latitude+.1,center.longitude,section,category));
            cb.success(entries);
        }
        return ()->{};
    };}
    private FakeMapSurface currentMap(){return FakeMapSurface.created.get(FakeMapSurface.created.size()-1);}
    private void assertCenter(double lat,double lon){onView(withContentDescription("探索地图")).check((v,e)->{if(e!=null)throw e;MapSurface.Camera c=currentMap().snapshot();org.junit.Assert.assertEquals(lat+.006,c.point.latitude,.00001);org.junit.Assert.assertEquals(lon+.006,c.point.longitude,.00001);});}
    @Test public void onlineCategoriesDotsAndDetailState(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("模拟历史景点")).perform(scrollTo()).check(matches(isDisplayed()));assertCenter(30.24,120.15);screenshot("culture-test-data");
            org.junit.Assert.assertEquals(1000,FakeMapSurface.lastFitRadius);
            onView(withContentDescription("景点圆点:baidu:near")).perform(click());onView(withText("模拟历史景点")).check(matches(withParent(isSelected())));
            onView(withContentDescription("查看模拟历史景点")).perform(scrollTo(),click());onView(withText("地点介绍")).perform(scrollTo());onView(withText("暂无介绍")).check(matches(isDisplayed()));Espresso.pressBack();
            onView(withText("美食")).perform(click());onView(withText("模拟美食地点")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withContentDescription("查看模拟美食地点")).perform(scrollTo(),click());scenario.recreate();onView(withText("测试餐饮地址")).check(matches(isDisplayed()));Espresso.pressBack();onView(withText("美食")).check(matches(isSelected()));
            onView(withText("自然")).perform(click());onView(withText("自然景区")).perform(scrollTo(),click());onView(withText("模拟自然景点")).perform(scrollTo()).check(matches(isDisplayed()));screenshot("nature-test-data");
            onView(withText("自驾路线")).perform(scrollTo(),click());onView(withContentDescription("查看西湖外围自驾示意")).perform(scrollTo(),click());
            onView(withContentDescription("路线预览地图")).perform(scrollTo()).check(matches(isDisplayed()));org.junit.Assert.assertEquals(3,currentMap().routePoints.size());screenshot("route-test-data");Espresso.pressBack();
            onView(withText("徒步路线")).perform(scrollTo(),click());onView(withText("一公里内暂无可显示的景点")).check(matches(isDisplayed()));
            onView(withText("我的")).perform(click());onView(withText("应用说明")).check(matches(isDisplayed()));screenshot("documentation");
        }
    }
    @Test public void markerSizeBoundsAndCameraDoNotChangeSearchRegion(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("模拟历史景点")).perform(scrollTo());final int before=requests.get();
            scenario.onActivity(a->{FakeMapSurface map=currentMap();org.junit.Assert.assertEquals(2,map.dots.size());for(android.view.View v:map.dots.values()){android.view.View dot=v.findViewWithTag("visible-dot");org.junit.Assert.assertEquals(Math.round(14*a.getResources().getDisplayMetrics().density),dot.getWidth());org.junit.Assert.assertEquals(Math.round(44*a.getResources().getDisplayMetrics().density),v.getWidth());}map.camera(new MapPoint(40,116,true),17f);});
            org.junit.Assert.assertEquals(before,requests.get());scenario.recreate();onView(withText("模拟历史景点")).perform(scrollTo());
            org.junit.Assert.assertEquals(40,currentMap().snapshot().point.latitude,.0001);
            onView(withContentDescription("回到当前位置")).perform(click());onView(withText("模拟历史景点")).perform(scrollTo());assertCenter(30.24,120.15);org.junit.Assert.assertTrue(requests.get()>before);
        }
    }
    @Test public void networkAuthQuotaErrorsStayExplicitAndRetry(){
        for(PoiProvider.Failure reason:new PoiProvider.Failure[]{PoiProvider.Failure.NETWORK,PoiProvider.Failure.AUTH,PoiProvider.Failure.QUOTA}){
            deps.pois=provider(reason);
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                onView(withText(PoiRepository.errorText(reason))).perform(scrollTo()).check(matches(isDisplayed()));
                onView(withText("重试检索")).perform(scrollTo(),click());screenshot("error-"+reason);
                onView(withText("查看杭州示例")).perform(scrollTo(),click());onView(withText("杭州示例区域 · 非当前位置")).check(matches(isDisplayed()));
                onView(withText("雷峰塔")).perform(scrollTo()).check(matches(isDisplayed()));
            }
        }
    }
    @Test public void missingKeyAndDeclinedConsentDoNotInitializeSdk(){
        deps.online=false;deps.consent=false;deps.maps=null;
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("暂不使用")).perform(click());org.junit.Assert.assertEquals(0,requests.get());org.junit.Assert.assertFalse(BaiduRuntime.initialized);
            onView(withText("我的")).perform(click());onView(withText("应用说明")).check(matches(isDisplayed()));
            onView(withText("百度地图使用与配置")).perform(scrollTo(),click());onView(withText("同意并使用地图")).perform(click());
            onView(withText("人文")).perform(click());onView(withText("一公里内暂无可显示的景点")).check(matches(isDisplayed()));org.junit.Assert.assertFalse(BaiduRuntime.initialized);screenshot("missing-ak");
        }
    }
    @Test public void locationFailureHasClearlyLabeledOfflineExamples(){
        deps.location=()->new LocationSource(){public void start(Callback cb){cb.failed(ExploreState.Status.DENIED);}public void cancel(){}};
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("定位权限未开启 · 杭州示例区域")).check(matches(isDisplayed()));
            onView(withText("雷峰塔")).perform(scrollTo()).check(matches(isDisplayed()));org.junit.Assert.assertEquals(0,requests.get());screenshot("location-denied");
        }
    }
}
