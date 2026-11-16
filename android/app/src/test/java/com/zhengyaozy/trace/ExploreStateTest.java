package com.zhengyaozy.trace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ExploreStateTest {
    @Test public void fiveCategoriesFilterWithoutCrossingSections(){
        ExploreState s=new ExploreState();s.ready(30.25,120.15,false);
        assertEquals(3,s.nearby("人文","全部").size());assertEquals(3,s.nearby("自然","全部").size());
        for(String category:new String[]{"历史","美食","政府管理景区","自驾路线","徒步路线"}){
            String section=category.equals("历史")||category.equals("美食")?"人文":"自然";
            assertFalse(s.nearby(section,category).isEmpty());
            for(Attraction a:s.nearby(section,category))assertEquals(category,a.category);
        }
        Attraction scenic=s.nearby("自然","政府管理景区").get(0);
        assertFalse(scenic.manager.isEmpty());assertTrue(scenic.source.startsWith("https://"));
    }
    @Test public void outsideHangzhouKeepsActualFixUntilExplicitSampleSwitch(){
        ExploreState s=new ExploreState();s.ready(39.9,116.4,true);assertEquals("当前位置 · 大致位置",s.label());
        assertTrue(s.nearby("人文","全部").isEmpty());assertEquals(39.9,s.centerLatitude(),0);
        s.sampleRegion=true;assertFalse(s.nearby("人文","全部").isEmpty());assertTrue(s.label().contains("非当前位置"));
        s.sampleRegion=false;assertEquals(39.9,s.centerLatitude(),0);
        assertEquals(0,ExploreState.distanceKm(30,120,30,120),0.001);
        assertEquals(111.195,ExploreState.distanceKm(0,0,1,0),0.01);
        s.ready(30.25,120.15,false);assertFalse(s.nearby("人文","全部").isEmpty());
        s.ready(30.9,120.15,false);assertTrue(s.nearby("人文","全部").isEmpty());
    }
    @Test public void failuresCannotMasqueradeAsCurrentLocation(){
        for(ExploreState.Status reason:new ExploreState.Status[]{ExploreState.Status.DENIED,ExploreState.Status.DISABLED,ExploreState.Status.TIMEOUT,ExploreState.Status.UNAVAILABLE}){
            ExploreState s=new ExploreState();s.fail(reason);assertTrue(s.sampleRegion);assertTrue(s.label().contains("杭州示例区域"));assertFalse(s.label().startsWith("当前位置"));
            s.ready(30.25,120.15,false);assertFalse(s.sampleRegion);assertEquals("当前位置",s.label());
        }
    }
    @Test public void routeLengthIsSumOfWaypointSegments(){
        for(Attraction a:Attraction.SAMPLES)if(a.isRoute()){
            assertEquals(3,a.waypoints.size());assertTrue(a.distanceKm()>1);assertTrue(a.distanceKm()<20);
            Attraction.Waypoint first=a.waypoints.get(0),last=a.waypoints.get(2);
            assertTrue(a.distanceKm()>=ExploreState.distanceKm(first.latitude,first.longitude,last.latitude,last.longitude));
        }
    }
}
