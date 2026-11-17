package com.zhengyaozy.trace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class PoiRepositoryTest {
    private final MapPoint center=new MapPoint(30,120,true);
    private Attraction place(String id,double lat,double lon){return Attraction.poi(id,id,"地址","",lat,lon,"人文","历史");}
    static final class Provider implements PoiProvider {
        int calls,cancelled;
        final List<Callback> callbacks=new ArrayList<>();
        public Request search(MapPoint p,String s,String c,Callback cb){calls++;callbacks.add(cb);return ()->cancelled++;}
    }
    static class Result implements PoiProvider.Callback {
        List<Attraction> places;PoiProvider.Failure error;int delivered;
        public void success(List<Attraction> p){places=p;delivered++;}
        public void failure(PoiProvider.Failure e){error=e;delivered++;}
    }
    @Test public void filtersDeduplicatesAndSortsWithinOneKilometer(){
        List<Attraction> input=Arrays.asList(place("far",30.02,120),place("near",30.001,120),place("middle",30.004,120),place("near",30.001,120),place("edge",30+Math.toDegrees(999/6371008.8),120),place("outside",30+Math.toDegrees(1001/6371008.8),120));
        List<Attraction> r=PoiRepository.filter(center,input);assertEquals(3,r.size());assertEquals("baidu:near",r.get(0).id);assertEquals("baidu:edge",r.get(2).id);
    }
    @Test public void capsOneHundredAndExcludesSamples(){
        List<Attraction> places=new ArrayList<>(Attraction.SAMPLES);for(int i=0;i<150;i++)places.add(place("p"+i,30+i*.000001,120));
        assertEquals(100,PoiRepository.filter(center,places).size());assertEquals(5,PoiRepository.MAX_PAGES);assertEquals(20,PoiRepository.PAGE_SIZE);
    }
    @Test public void cacheExpiresAtFiveMinutesAndRefreshBypassesIt(){
        Provider p=new Provider();AtomicLong time=new AtomicLong();PoiRepository repo=new PoiRepository(p,time::get);Result first=new Result();
        repo.load(center,"人文","历史",false,first);p.callbacks.get(0).success(Arrays.asList(place("a",30,120)));
        repo.load(center,"人文","历史",false,new Result());assertEquals(1,p.calls);
        time.set(299999);repo.load(center,"人文","历史",false,new Result());assertEquals(1,p.calls);
        time.set(300000);repo.load(center,"人文","历史",false,new Result());assertEquals(2,p.calls);p.callbacks.get(1).success(Collections.emptyList());
        repo.load(center,"人文","历史",true,new Result());assertEquals(3,p.calls);
    }
    @Test public void cacheSurvivesActivityRepositoryRecreation(){
        Provider p=new Provider();PoiRepository.Cache cache=new PoiRepository.Cache();
        PoiRepository first=new PoiRepository(p,()->0,cache);first.load(center,"人文","全部",false,new Result());p.callbacks.get(0).success(Arrays.asList(place("a",30,120)));first.cancel();
        Result restored=new Result();new PoiRepository(p,()->1,cache).load(center,"人文","全部",false,restored);assertEquals(1,p.calls);assertEquals(1,restored.places.size());
    }
    @Test public void lateResultsAndErrorsFromOldCategoriesAreDiscarded(){
        Provider p=new Provider();PoiRepository repo=new PoiRepository(p,()->0);Result old=new Result(),fresh=new Result();
        repo.load(center,"人文","历史",false,old);repo.load(center,"人文","美食",false,fresh);
        p.callbacks.get(0).success(Arrays.asList(place("old",30,120)));p.callbacks.get(0).failure(PoiProvider.Failure.AUTH);assertEquals(0,old.delivered);
        p.callbacks.get(1).success(Collections.emptyList());assertEquals(1,fresh.delivered);assertTrue(p.cancelled>0);
    }
    @Test public void failuresDoNotUseCachedOrLocalSamplesAndCanRetry(){
        Provider p=new Provider();PoiRepository repo=new PoiRepository(p,()->0);Result r=new Result();repo.load(center,"人文","历史",false,r);p.callbacks.get(0).failure(PoiProvider.Failure.NETWORK);
        assertNull(r.places);assertEquals(PoiProvider.Failure.NETWORK,r.error);repo.load(center,"人文","历史",false,new Result());assertEquals(2,p.calls);
        assertTrue(PoiRepository.errorText(PoiProvider.Failure.AUTH).contains("鉴权"));assertTrue(PoiRepository.errorText(PoiProvider.Failure.QUOTA).contains("配额"));
    }
    @Test public void cancelledLifecycleAndChangedLocationDiscardOldResponses(){
        Provider p=new Provider();PoiRepository repo=new PoiRepository(p,()->0);Result old=new Result();repo.load(center,"人文","全部",false,old);repo.cancel();p.callbacks.get(0).success(Collections.emptyList());assertEquals(0,old.delivered);
        repo.load(new MapPoint(31,121,true),"人文","全部",false,new Result());assertEquals(2,p.calls);
    }
    @Test public void gpsIsConvertedOnlyOnceAndQueriesRequireBd09(){
        AtomicInteger conversions=new AtomicInteger();MapPoint gps=MapPoint.gps(30,120);
        java.util.function.Function<MapPoint,MapPoint> converter=p->{conversions.incrementAndGet();return new MapPoint(p.latitude+.006,p.longitude+.006,true);};
        MapPoint bd=gps.toBd09(converter);assertSame(bd,bd.toBd09(converter));assertEquals(1,conversions.get());assertEquals(30.006,bd.latitude,.0000001);
        try{new PoiRepository(new Provider(),()->0).load(gps,"人文","全部",false,new Result());fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void sourceAndKeywordsDoNotInventRankOrHeat(){
        Attraction p=place("x",30,120);assertTrue(p.online);assertTrue(p.bd09);assertEquals("暂无介绍",p.narration);
        assertEquals("美食",PoiProvider.keyword("人文","美食"));assertTrue(PoiProvider.keyword("自然","自然景区").contains("公园"));
        assertEquals(PoiProvider.Failure.QUOTA,BaiduPoiProvider.classify(4,"REQUEST_ERROR"));assertEquals(PoiProvider.Failure.AUTH,BaiduPoiProvider.classify(5,"KEY_ERROR"));
    }
}
