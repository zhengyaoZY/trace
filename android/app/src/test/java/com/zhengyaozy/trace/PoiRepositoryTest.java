package com.zhengyaozy.trace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.*;
public class PoiRepositoryTest {
    private final MapPoint center=MapPoint.gps(30,120);
    private Attraction place(String id){return Attraction.poi(id,id,"地址","",30.006,120.006,"人文","历史");}
    static class Provider implements PoiProvider {
        int calls,cancelled;boolean refresh;final List<Callback> callbacks=new ArrayList<>();
        public Request search(MapPoint p,String s,String c,boolean refresh,Callback cb){assertFalse(p.bd09);this.refresh=refresh;calls++;callbacks.add(cb);return ()->cancelled++;}
    }
    static class Result implements PoiProvider.Callback {
        List<Attraction> places;PoiProvider.Failure error;int delivered;
        public void success(PoiProvider.Result r){places=r.places;delivered++;}public void failure(PoiProvider.Failure e){error=e;delivered++;}
    }
    @Test public void preservesServerOrderAndNeverMixesLocalExamples(){
        List<Attraction> input=new ArrayList<>(Attraction.SAMPLES);input.add(place("z"));input.add(place("a"));input.add(place("z"));
        List<Attraction> r=PoiRepository.filter(input);assertEquals(2,r.size());assertEquals("baidu:z",r.get(0).id);
    }
    @Test public void cacheHonorsServerExpiryRatherThanExtendingIt(){
        Provider p=new Provider();AtomicLong now=new AtomicLong(100);PoiRepository repo=new PoiRepository(p,now::get);
        repo.load(center,"人文","全部",false,new Result());p.callbacks.get(0).success(new PoiProvider.Result(Arrays.asList(place("a")),150));
        now.set(149);repo.load(center,"人文","全部",false,new Result());assertEquals(1,p.calls);
        now.set(150);repo.load(center,"人文","全部",false,new Result());assertEquals(2,p.calls);
        p.callbacks.get(1).success(new PoiProvider.Result(Collections.emptyList(),200));repo.load(center,"人文","全部",true,new Result());assertTrue(p.refresh);assertEquals(3,p.calls);
    }
    @Test public void cacheSurvivesRecreationAndCapsProviderExpiry(){
        Provider p=new Provider();PoiRepository.Cache cache=new PoiRepository.Cache();
        new PoiRepository(p,()->0,cache).load(center,"人文","全部",false,new Result());p.callbacks.get(0).success(new PoiProvider.Result(Arrays.asList(place("a")),Long.MAX_VALUE));
        new PoiRepository(p,()->299999,cache).load(center,"人文","全部",false,new Result());assertEquals(1,p.calls);
        new PoiRepository(p,()->300000,cache).load(center,"人文","全部",false,new Result());assertEquals(2,p.calls);
    }
    @Test public void oldCategoriesAndCancelledResponsesAreDiscarded(){
        Provider p=new Provider();PoiRepository repo=new PoiRepository(p,()->0);Result old=new Result(),fresh=new Result();
        repo.load(center,"人文","历史",false,old);repo.load(center,"人文","美食",false,fresh);
        p.callbacks.get(0).success(new PoiProvider.Result(Arrays.asList(place("old")),500));p.callbacks.get(0).failure(PoiProvider.Failure.AUTH);assertEquals(0,old.delivered);
        p.callbacks.get(1).success(new PoiProvider.Result(Collections.emptyList(),500));assertEquals(1,fresh.delivered);repo.cancel();assertTrue(p.cancelled>0);
    }
    @Test public void failuresNeverReturnExamples(){
        Provider p=new Provider();PoiRepository repo=new PoiRepository(p,()->0);Result r=new Result();repo.load(center,"人文","全部",false,r);p.callbacks.get(0).failure(PoiProvider.Failure.QUOTA);assertNull(r.places);assertEquals(PoiProvider.Failure.QUOTA,r.error);
    }
    @Test public void gpsRequestAndBd09ResponseRemainDifferent(){
        try{new PoiRepository(new Provider(),()->0).load(new MapPoint(30,120,true),"人文","全部",false,new Result());fail();}catch(IllegalArgumentException expected){}
        AtomicInteger conversions=new AtomicInteger();MapPoint gps=MapPoint.gps(30,120);MapPoint b=gps.toBd09(p->{conversions.incrementAndGet();return new MapPoint(30.006,120.006,true);});assertSame(b,b.toBd09(p->{throw new AssertionError();}));assertEquals(1,conversions.get());
    }
    @Test public void amapIsDefaultAndProviderCoordinatesCannotBeReconverted(){
        assertEquals(MapVendor.AMAP,MapVendor.from(null));assertEquals(MapVendor.BAIDU,MapVendor.from("baidu"));
        MapPoint gcj=MapPoint.gps(30,120).toGcj02(p->new MapPoint(30.003,120.003,"GCJ02"));
        assertSame(gcj,gcj.toGcj02(p->{throw new AssertionError();}));
        try{gcj.toBd09(p->{throw new AssertionError();});fail();}catch(IllegalArgumentException expected){}
        try{new MapPoint(30,120,true).toGcj02(p->{throw new AssertionError();});fail();}catch(IllegalArgumentException expected){}
        try{new PoiRepository(new Provider(),()->0).load(gcj,"人文","全部",false,new Result());fail();}catch(IllegalArgumentException expected){}
        Attraction a=Attraction.providerPoi(MapVendor.AMAP,"a","a","地址",30,120,"人文","历史").withMetrics(new Attraction.Metrics(null,"none",null,null,null,null));
        assertEquals("GCJ02",a.coordinateSystem);assertEquals("amap:a",a.id);assertFalse(a.bd09);assertEquals(1,PoiRepository.filter(Arrays.asList(a)).size());
    }
    @Test public void sharedCacheIsIsolatedByProvider(){
        PoiRepository.Cache cache=new PoiRepository.Cache();Provider a=new Provider(),b=new Provider();
        new PoiRepository(a,()->0,cache,"amap").load(center,"人文","全部",false,new Result());a.callbacks.get(0).success(new PoiProvider.Result(Arrays.asList(Attraction.providerPoi(MapVendor.AMAP,"a","a","",30,120,"人文","历史")),100));
        new PoiRepository(b,()->0,cache,"baidu").load(center,"人文","全部",false,new Result());assertEquals(1,b.calls);
        Result cached=new Result();new PoiRepository(a,()->0,cache,"amap").load(center,"人文","全部",false,cached);assertEquals(1,a.calls);assertEquals("amap:a",cached.places.get(0).id);
    }
    @Test public void bubbleAreaIsMonotonicAndMissingIsNeutral(){
        assertEquals(14,MarkerStyle.diameterDp(null),.001);assertEquals(14,MarkerStyle.diameterDp(0d),.001);assertEquals(40,MarkerStyle.diameterDp(1d),.001);
        float previous=0;for(int i=0;i<=100;i++){float d=MarkerStyle.diameterDp(i/100d);assertTrue(d>=previous);assertTrue(d<=44);previous=d;}
        assertEquals((196+1600)/2d,Math.pow(MarkerStyle.diameterDp(.5),2),.01);
    }
}
