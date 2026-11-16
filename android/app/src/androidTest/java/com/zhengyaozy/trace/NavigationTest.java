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
        android.graphics.Bitmap image = instrumentation.getUiAutomation().takeScreenshot();
        org.junit.Assert.assertNotNull(image);
        java.io.File directory = new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null), "screenshots");
        directory.mkdirs();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(directory, name+".png"))) {
            org.junit.Assert.assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        finally { image.recycle(); }
    }

    @Test public void discoverDetailsMapAndProfileAreReachable() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("在杭州，\n循着故事出发")).check(matches(isDisplayed()));
            screenshot("discover");
            onView(withContentDescription("探索西湖")).perform(scrollTo(), click());
            screenshot("detail");
            onView(withText("文字讲解")).perform(scrollTo()).check(matches(isDisplayed()));
            screenshot("reading");
            Espresso.pressBack();
            onView(withText("地图")).perform(click());
            onView(withText("杭州地图")).check(matches(isDisplayed()));
            screenshot("map");
            onView(withText("拱宸桥")).perform(click());
            onView(withText("沿着运河，走进市井")).check(matches(isDisplayed()));
            onView(withText("我的")).perform(click());
            onView(withText("你好，城市探索者")).check(matches(isDisplayed()));
            screenshot("profile");
        }
    }
}
