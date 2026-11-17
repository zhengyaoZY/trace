package com.zhengyaozy.trace;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class HttpPoiProviderTest {
    static final class LocalServer implements AutoCloseable {
        final ServerSocket server;final Thread thread;final AtomicReference<String> body=new AtomicReference<>();final AtomicReference<Throwable> error=new AtomicReference<>();
        LocalServer(int status,String payload)throws Exception{
            server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));server.setSoTimeout(6000);
            thread=new Thread(()->{try(Socket client=server.accept()){
                client.setSoTimeout(4000);InputStream in=client.getInputStream();ByteArrayOutputStream headers=new ByteArrayOutputStream();int next;
                while((next=in.read())!=-1){headers.write(next);byte[] h=headers.toByteArray();int n=h.length;if(n>=4&&h[n-4]=='\r'&&h[n-3]=='\n'&&h[n-2]=='\r'&&h[n-1]=='\n')break;if(n>8192)throw new IOException();}
                String header=headers.toString("UTF-8");assertTrue(header.startsWith("POST /api/v1/places/nearby HTTP/1.1"));int length=0;
                for(String line:header.split("\r\n"))if(line.toLowerCase().startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                byte[] data=new byte[length];int read=0;while(read<length){int n=in.read(data,read,length-read);if(n<0)throw new EOFException();read+=n;}body.set(new String(data,StandardCharsets.UTF_8));
                byte[] response=payload.getBytes(StandardCharsets.UTF_8);OutputStream out=client.getOutputStream();out.write(("HTTP/1.1 "+status+" Result\r\nContent-Type: application/json\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(response);out.flush();
            }catch(Throwable e){error.set(e);}},"test-http-backend");thread.start();
        }
        String base(){return "http://127.0.0.1:"+server.getLocalPort();}
        public void close()throws Exception{server.close();thread.join(2000);if(error.get()!=null)throw new AssertionError(error.get());}
    }
    private PoiProvider.Result request(LocalServer server,AtomicReference<PoiProvider.Failure> failure)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<PoiProvider.Result> result=new AtomicReference<>();
        HttpPoiProvider provider=new HttpPoiProvider(server.base());
        provider.search(MapPoint.gps(30.24,120.15),"人文","历史",true,new PoiProvider.Callback(){public void success(PoiProvider.Result r){result.set(r);done.countDown();}public void failure(PoiProvider.Failure e){failure.set(e);done.countDown();}});
        assertTrue(done.await(6,TimeUnit.SECONDS));return result.get();
    }
    @Test public void actualHttpUsesWgs84PostAndReadsServerMetricsWithoutMapAk()throws Exception{
        long expiry=System.currentTimeMillis()+170000;
        String response="{\"source\":\"baidu\",\"radiusMeters\":1000,\"expiresAt\":"+expiry+",\"items\":[{\"id\":\"real-contract\",\"name\":\"HTTP测试景点\",\"address\":\"地址\",\"category\":\"历史\",\"location\":{\"latitude\":30.246,\"longitude\":120.156,\"coordinateSystem\":\"BD09\"},\"distanceMeters\":100,\"rankingRaw\":\"杭州榜第1名\",\"rank\":{\"board\":\"杭州榜\",\"position\":1},\"heat7d\":100,\"score\":1,\"scoreSource\":\"rank_heat\"}]}";
        try(LocalServer server=new LocalServer(200,response)){
            AtomicReference<PoiProvider.Failure> failure=new AtomicReference<>();PoiProvider.Result r=request(server,failure);assertNull(failure.get());assertNotNull(r);assertEquals(expiry,r.expiresAt);assertEquals(1,r.places.size());assertTrue(r.places.get(0).bd09);assertEquals(1,r.places.get(0).metrics.score,0);
            JSONObject body=new JSONObject(server.body.get());assertEquals("WGS84",body.getJSONObject("center").getString("coordinateSystem"));assertEquals(30.24,body.getJSONObject("center").getDouble("latitude"),0);assertEquals(1000,body.getInt("radiusMeters"));assertTrue(body.getBoolean("refresh"));assertFalse(body.has("ak"));
        }
    }
    @Test public void apiErrorsAreDistinctAndMalformedDatumDoesNotRenderFakePlaces()throws Exception{
        for(String code:new String[]{"UPSTREAM_AUTH","UPSTREAM_QUOTA","RATE_LIMITED","UPSTREAM_TIMEOUT"})try(LocalServer server=new LocalServer(503,"{\"error\":{\"code\":\""+code+"\"}}")){
            AtomicReference<PoiProvider.Failure> failure=new AtomicReference<>();assertNull(request(server,failure));assertNotNull(failure.get());
        }
        try(LocalServer server=new LocalServer(200,"{\"source\":\"baidu\",\"radiusMeters\":1000,\"expiresAt\":1,\"items\":[{\"location\":{\"coordinateSystem\":\"WGS84\"}}]}")){
            AtomicReference<PoiProvider.Failure> failure=new AtomicReference<>();assertNull(request(server,failure));assertEquals(PoiProvider.Failure.UNAVAILABLE,failure.get());
        }
    }
    @Test public void endpointPolicyRejectsPublicCleartextAndCredentialsInUrl(){
        assertTrue(HttpPoiProvider.allowed("http://10.0.2.2:3000"));assertTrue(HttpPoiProvider.allowed("https://example.com"));assertFalse(HttpPoiProvider.allowed("http://example.com"));assertFalse(HttpPoiProvider.allowed("https://secret@example.com"));assertFalse(HttpPoiProvider.allowed("https://example.com?ak=unsafe"));
    }
}
