package com.zhengyaozy.trace;

import java.util.*;
import java.util.function.LongSupplier;

/** Main-thread coordinator; bounds, limits, cache and stale-callback rules are independently tested. */
final class PoiRepository {
    static final int RADIUS_METERS=1000,MAX_RESULTS=100,PAGE_SIZE=20,MAX_PAGES=5;
    static final long CACHE_MILLIS=300_000;
    private final PoiProvider provider;
    private final LongSupplier clock;
    private long generation;
    private PoiProvider.Request active;
    private final Map<String,Entry> cache=new LinkedHashMap<String,Entry>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,PoiRepository.Entry> e){return size()>32;}
    };
    private static class Entry { final long time;final List<Attraction> places;Entry(long time,List<Attraction> places){this.time=time;this.places=places;} }
    PoiRepository(PoiProvider provider,LongSupplier clock){this.provider=provider;this.clock=clock;}
    void cancel(){generation++;if(active!=null)active.cancel();active=null;}
    void load(MapPoint center,String section,String category,boolean refresh,PoiProvider.Callback callback){
        if(!center.bd09)throw new IllegalArgumentException("POI searches require BD-09");
        cancel();final long token=generation;
        String key=String.format(Locale.ROOT,"%.6f/%.6f/%s/%s",center.latitude,center.longitude,section,category);
        Entry hit=cache.get(key);
        if(!refresh&&hit!=null&&clock.getAsLong()-hit.time<CACHE_MILLIS){callback.success(hit.places);return;}
        PoiProvider.Request request=provider.search(center,section,category,new PoiProvider.Callback(){
            public void success(List<Attraction> places){if(token!=generation)return;active=null;List<Attraction> result=filter(center,places);cache.put(key,new Entry(clock.getAsLong(),result));callback.success(result);}
            public void failure(PoiProvider.Failure reason){if(token!=generation)return;active=null;callback.failure(reason);}
        });
        // A provider may complete synchronously; cancellation remains harmless for a completed request.
        if(token==generation)active=request;else if(request!=null)request.cancel();
    }
    static List<Attraction> filter(MapPoint center,List<Attraction> places){
        Map<String,Attraction> unique=new LinkedHashMap<>();
        for(Attraction a:places)if(a.online&&a.bd09&&Double.isFinite(a.latitude)&&Double.isFinite(a.longitude)
            &&ExploreState.distanceKm(center.latitude,center.longitude,a.latitude,a.longitude)<=1.0)unique.putIfAbsent(a.id,a);
        List<Attraction> result=new ArrayList<>(unique.values());
        result.sort(Comparator.comparingDouble((Attraction a)->ExploreState.distanceKm(center.latitude,center.longitude,a.latitude,a.longitude)).thenComparing(a->a.id));
        return Collections.unmodifiableList(new ArrayList<>(result.subList(0,Math.min(MAX_RESULTS,result.size()))));
    }
    static String errorText(PoiProvider.Failure reason){
        switch(reason){case AUTH:return "百度地图鉴权失败，请核对 AK、包名和签名";case QUOTA:return "百度检索配额不足，请稍后重试";case NETWORK:return "网络连接失败，请重试";default:return "百度周边检索暂不可用，请重试";}
    }
}
