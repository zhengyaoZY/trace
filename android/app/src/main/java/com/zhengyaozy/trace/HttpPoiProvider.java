package com.zhengyaozy.trace;

import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class HttpPoiProvider implements PoiProvider {
    private final String base;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private static final ScheduledExecutorService deadlines=Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService workers=new ThreadPoolExecutor(0,4,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16));
    HttpPoiProvider(){this(BuildConfig.API_BASE_URL);}
    HttpPoiProvider(String base){this.base=base;}
    static boolean allowed(String value){
        try{URI u=URI.create(value);String host=u.getHost();if(host==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)return false;
            if("https".equals(u.getScheme()))return true;
            if(!BuildConfig.DEBUG||!"http".equals(u.getScheme()))return false;
            return host.equals("localhost")||host.equals("10.0.2.2")||host.equals("127.0.0.1")||host.matches("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")||host.matches("192\\.168\\.\\d{1,3}\\.\\d{1,3}")||host.matches("172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}");
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public Request search(MapPoint gps,String section,String category,boolean refresh,Callback cb){
        if(gps.bd09)throw new IllegalArgumentException("WGS84 request required");
        Task task=new Task(cb);
        if(!allowed(base)){ui.post(()->task.fail(Failure.CONFIG));return task;}
        try{task.future=workers.submit(()->{
            long deadline=android.os.SystemClock.elapsedRealtime()+25_000;
            try{
                URL url=new URL(base.replaceAll("/+$","")+"/api/v1/places/nearby");
                HttpURLConnection c=(HttpURLConnection)url.openConnection();task.connection=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(25000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");
                JSONObject center=new JSONObject().put("latitude",gps.latitude).put("longitude",gps.longitude).put("coordinateSystem","WGS84");
                byte[] payload=new JSONObject().put("center",center).put("section",section).put("category",category).put("radiusMeters",1000).put("refresh",refresh).toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(payload.length);
                if(task.cancelled)return;
                try(OutputStream out=c.getOutputStream()){out.write(payload);}
                int status=c.getResponseCode();InputStream input=status>=400?c.getErrorStream():c.getInputStream();
                if(input==null){task.fail(Failure.UNAVAILABLE);return;}
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=input){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(task.cancelled)return;if(android.os.SystemClock.elapsedRealtime()>deadline)throw new SocketTimeoutException();bytes.write(buffer,0,n);if(bytes.size()>1_048_576)throw new IOException();}}
                JSONObject response=new JSONObject(bytes.toString("UTF-8"));
                if(status!=200){task.fail(error(response.optJSONObject("error"),status));return;}
                if(response.getInt("radiusMeters")!=1000||!"baidu".equals(response.getString("source")))throw new JSONException("contract");
                JSONArray items=response.getJSONArray("items");List<Attraction> result=new ArrayList<>();
                if(items.length()>100)throw new JSONException("limit");
                for(int i=0;i<items.length();i++){
                    JSONObject item=items.getJSONObject(i),point=item.getJSONObject("location");if(!"BD09".equals(point.getString("coordinateSystem")))throw new JSONException("datum");
                    double lat=point.getDouble("latitude"),lon=point.getDouble("longitude"),distance=item.getDouble("distanceMeters");
                    if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||!Double.isFinite(distance)||distance<0||distance>1000)throw new JSONException("bounds");
                    Double score=number(item,"score"),heat=number(item,"heat7d");if(score!=null&&(score<0||score>1))throw new JSONException("score");
                    JSONObject rank=item.optJSONObject("rank");
                    Attraction a=Attraction.poi(item.getString("id"),item.getString("name"),item.optString("address",""),"",lat,lon,section,item.getString("category"));
                    result.add(a.withMetrics(new Attraction.Metrics(score,item.getString("scoreSource"),heat,item.isNull("rankingRaw")?null:item.optString("rankingRaw"),rank==null?null:rank.getString("board"),rank==null?null:rank.getInt("position"))));
                }
                Result data=new Result(Collections.unmodifiableList(result),response.getLong("expiresAt"));task.success(data);
            }catch(SocketTimeoutException e){task.fail(Failure.TIMEOUT);}catch(IOException e){task.fail(Failure.NETWORK);}catch(JSONException|IllegalArgumentException e){task.fail(Failure.UNAVAILABLE);}finally{if(task.connection!=null)task.connection.disconnect();if(task.deadline!=null)task.deadline.cancel(false);}
        });task.deadline=deadlines.schedule(()->{task.fail(Failure.TIMEOUT);if(task.connection!=null)task.connection.disconnect();if(task.future!=null)task.future.cancel(true);},25,TimeUnit.SECONDS);if(task.finished.get())task.deadline.cancel(false);}catch(RejectedExecutionException e){ui.post(()->task.fail(Failure.UNAVAILABLE));}
        return task;
    }
    private static Double number(JSONObject value,String key)throws JSONException{if(value.isNull(key))return null;double n=value.getDouble(key);if(!Double.isFinite(n))throw new JSONException("number");return n;}
    static Failure error(JSONObject e,int status){String code=e==null?"":e.optString("code");switch(code){case "UPSTREAM_AUTH":return Failure.AUTH;case "UPSTREAM_NOT_CONFIGURED":return Failure.SERVER_CONFIG;case "UPSTREAM_QUOTA":return Failure.QUOTA;case "RATE_LIMITED":return Failure.RATE_LIMIT;case "UPSTREAM_TIMEOUT":return Failure.TIMEOUT;case "UPSTREAM_NETWORK":return Failure.NETWORK;default:return status==429?Failure.RATE_LIMIT:Failure.UNAVAILABLE;}}
    private final class Task implements Request {
        final Callback callback;final java.util.concurrent.atomic.AtomicBoolean finished=new java.util.concurrent.atomic.AtomicBoolean();volatile boolean cancelled;volatile HttpURLConnection connection;Future<?> future;volatile ScheduledFuture<?> deadline;
        Task(Callback cb){callback=cb;}
        void fail(Failure e){if(finished.compareAndSet(false,true))ui.post(()->{if(!cancelled)callback.failure(e);});}
        void success(Result r){if(finished.compareAndSet(false,true))ui.post(()->{if(!cancelled)callback.success(r);});}
        public void cancel(){cancelled=true;finished.set(true);if(deadline!=null)deadline.cancel(false);if(future!=null)future.cancel(true);if(connection!=null)connection.disconnect();}
    }
}
