package com.zhengyaozy.trace;

import java.util.*;
import java.util.function.LongSupplier;

/** Main-thread coordinator; bounds, limits, cache and stale-callback rules are independently tested. */
final class PoiRepository {
    static final int RADIUS_METERS=1000,MAX_RESULTS=100,PAGE_SIZE=20,MAX_PAGES=5;
    static final long CACHE_MILLIS=300_000;
    private final PoiProvider provider;
    private final LongSupplier clock;
    private final String namespace;
    private long generation;
    private PoiProvider.Request active;
    private static class Entry { final long expiresAt;final List<Attraction> places;Entry(long expiresAt,List<Attraction> places){this.expiresAt=expiresAt;this.places=places;} }
    static final class Cache {
        final Map<String,Entry> entries=new LinkedHashMap<String,Entry>(32,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<String,PoiRepository.Entry> e){return size()>32;}
        };
    }
    private final Map<String,Entry> cache;
    PoiRepository(PoiProvider provider,LongSupplier clock){this(provider,clock,new Cache());}
    PoiRepository(PoiProvider provider,LongSupplier clock,Cache cache){this(provider,clock,cache,"baidu");}
    PoiRepository(PoiProvider provider,LongSupplier clock,Cache cache,String namespace){this.provider=provider;this.clock=clock;this.cache=cache.entries;this.namespace=namespace;}
    void cancel(){generation++;if(active!=null)active.cancel();active=null;}
    void load(MapPoint center,String section,String category,boolean refresh,PoiProvider.Callback callback){
        if(!"WGS84".equals(center.coordinateSystem))throw new IllegalArgumentException("Backend queries require WGS84");
        cancel();final long token=generation;
        String key=namespace+"/"+String.format(Locale.ROOT,"%.6f/%.6f/%s/%s",center.latitude,center.longitude,section,category);
        Entry hit=cache.get(key);
        if(!refresh&&hit!=null&&clock.getAsLong()<hit.expiresAt){callback.success(new PoiProvider.Result(hit.places,hit.expiresAt));return;}
        PoiProvider.Request request=provider.search(center,section,category,refresh,new PoiProvider.Callback(){
            public void success(PoiProvider.Result data){if(token!=generation)return;active=null;List<Attraction> result=filter(data.places);long expires=Math.min(data.expiresAt,clock.getAsLong()+CACHE_MILLIS);cache.put(key,new Entry(expires,result));callback.success(new PoiProvider.Result(result,expires));}
            public void failure(PoiProvider.Failure reason){if(token!=generation)return;active=null;callback.failure(reason);}
        });
        // A provider may complete synchronously; cancellation remains harmless for a completed request.
        if(token==generation)active=request;else if(request!=null)request.cancel();
    }
    static List<Attraction> filter(List<Attraction> places){
        Map<String,Attraction> unique=new LinkedHashMap<>();
        for(Attraction a:places)if(a.online&&("BD09".equals(a.coordinateSystem)||"GCJ02".equals(a.coordinateSystem))&&Double.isFinite(a.latitude)&&Double.isFinite(a.longitude)
            &&Math.abs(a.latitude)<=90&&Math.abs(a.longitude)<=180)unique.putIfAbsent(a.id,a);
        List<Attraction> result=new ArrayList<>(unique.values());
        return Collections.unmodifiableList(new ArrayList<>(result.subList(0,Math.min(MAX_RESULTS,result.size()))));
    }
    static String errorText(PoiProvider.Failure reason){
        switch(reason){case SERVER_CONFIG:return "当前地图的景点服务尚未配置服务端凭据";case CONFIG:return "尚未配置景点服务地址";case AUTH:return "景点数据服务配置异常，请稍后重试";case QUOTA:return "景点数据服务配额不足，请稍后重试";case RATE_LIMIT:return "请求过于频繁，请稍后重试";case TIMEOUT:return "景点服务请求超时，请重试";case NETWORK:return "网络连接失败，请重试";default:return "景点数据服务暂不可用，请重试";}
    }
}
