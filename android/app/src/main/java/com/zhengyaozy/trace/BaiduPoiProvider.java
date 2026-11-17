package com.zhengyaozy.trace;

import android.os.Handler;
import android.os.Looper;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.*;
import java.util.*;

final class BaiduPoiProvider implements PoiProvider {
    @Override public Request search(MapPoint center,String section,String category,Callback cb){
        if(!center.bd09)throw new IllegalArgumentException("BD-09 required");
        Search task=new Search(center,section,category,cb);task.next();return task;
    }
    static Failure classify(int status,String code){
        if(status==3||status==5||status==101||status==102||code.contains("KEY_ERROR")||code.contains("PERMISSION")||code.contains("TOKEN"))return Failure.AUTH;
        if(status==4||status==301||status==302||code.contains("UNLIMITED"))return Failure.QUOTA;
        if(code.contains("NETWORK")||code.contains("CONNECT")||code.contains("TIMEOUT"))return Failure.NETWORK;
        return Failure.UNAVAILABLE;
    }
    private static final class Search implements Request,OnGetPoiSearchResultListener {
        final PoiSearch search=PoiSearch.newInstance();
        final MapPoint center;final String section,category;final Callback cb;
        final List<Attraction> results=new ArrayList<>();
        final Handler handler=new Handler(Looper.getMainLooper());
        final Runnable timeout=()->fail(Failure.NETWORK);
        boolean done;int page;
        Search(MapPoint center,String section,String category,Callback cb){this.center=center;this.section=section;this.category=category;this.cb=cb;search.setOnGetPoiSearchResultListener(this);handler.postDelayed(timeout,20_000);}
        void next(){
            if(done)return;
            boolean started=search.searchNearby(new PoiNearbySearchOption().location(new LatLng(center.latitude,center.longitude))
                .radius(PoiRepository.RADIUS_METERS).radiusLimit(true).keyword(PoiProvider.keyword(section,category))
                .scope(2).pageNum(page).pageCapacity(PoiRepository.PAGE_SIZE));
            if(!started)fail(Failure.UNAVAILABLE);
        }
        @Override public void onGetPoiResult(PoiResult r){
            if(done)return;
            if(r==null){fail(Failure.NETWORK);return;}
            if(r.error==SearchResult.ERRORNO.RESULT_NOT_FOUND){finish();return;}
            if(r.error!=SearchResult.ERRORNO.NO_ERROR){fail(classify(r.status,String.valueOf(r.error)));return;}
            List<PoiInfo> list=r.getAllPoi();
            if(list!=null)for(PoiInfo p:list){
                if(p.getUid()==null||p.getUid().isEmpty()||p.getLocation()==null||p.getName()==null)continue;
                String resolved=category;
                if(category.equals("全部")){
                    String tag=p.getTag()==null?"":p.getTag();
                    resolved=section.equals("自然")?"自然景区":(tag.contains("美食")||tag.contains("餐")||tag.contains("小吃")?"美食":(tag.contains("古迹")||tag.contains("历史")||tag.contains("博物馆")?"历史":"人文地点"));
                }
                results.add(Attraction.poi(p.getUid(),p.getName(),p.getAddress(),p.getTag(),p.getLocation().latitude,p.getLocation().longitude,section,resolved));
            }
            page++;
            if(page>=PoiRepository.MAX_PAGES||page>=r.getTotalPageNum()||list==null||list.isEmpty())finish();else handler.post(this::next);
        }
        void finish(){if(done)return;List<Attraction> copy=new ArrayList<>(results);cancel();cb.success(copy);}
        void fail(Failure reason){if(done)return;cancel();cb.failure(reason);}
        public void cancel(){if(done)return;done=true;handler.removeCallbacksAndMessages(null);search.destroy();}
        @Override public void onGetPoiDetailResult(PoiDetailResult result){}
        @Override public void onGetPoiDetailResult(PoiDetailSearchResult result){}
        @Override public void onGetPoiIndoorResult(PoiIndoorResult result){}
    }
}
