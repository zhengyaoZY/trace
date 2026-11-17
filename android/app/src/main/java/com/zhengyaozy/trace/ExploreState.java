package com.zhengyaozy.trace;

import java.util.ArrayList;
import java.util.List;

/** Pure state and distance rules; a sample region is never presented as a device fix. */
final class ExploreState {
    enum Status { IDLE, REQUESTING, READY, DENIED, DISABLED, TIMEOUT, UNAVAILABLE }
    Status status=Status.IDLE;
    double latitude=30.24,longitude=120.15;
    boolean approximate, sampleRegion;
    void ready(double lat,double lon,boolean approximate){status=Status.READY;latitude=lat;longitude=lon;this.approximate=approximate;sampleRegion=false;}
    void fail(Status reason){status=reason;sampleRegion=true;}
    double centerLatitude(){return sampleRegion?30.24:latitude;}
    double centerLongitude(){return sampleRegion?120.15:longitude;}
    String label(){
        if(sampleRegion){
            switch(status){
                case IDLE:return "尚未定位 · 杭州示例区域";
                case DENIED:return "定位权限未开启 · 杭州示例区域";
                case DISABLED:return "系统定位已关闭 · 杭州示例区域";
                case TIMEOUT:return "定位超时 · 杭州示例区域";
                case READY:return "杭州示例区域 · 非当前位置";
                case REQUESTING:return "正在定位 · 暂览杭州示例区域";
                default:return "定位暂不可用 · 杭州示例区域";
            }
        }
        return status==Status.READY?(approximate?"当前位置 · 大致位置":"当前位置"):
            "正在获取当前位置…";
    }
    List<Attraction> nearby(String section,String category){
        List<Attraction> result=new ArrayList<>();
        if(!sampleRegion&&status!=Status.READY)return result;
        for(Attraction a:Attraction.SAMPLES)if(a.section.equals(section)
                &&(category.equals("全部")||category.equals(a.category))
                &&distanceKm(centerLatitude(),centerLongitude(),a.latitude,a.longitude)<=1.0)result.add(a);
        return result;
    }
    static double distanceKm(double lat1,double lon1,double lat2,double lon2){
        double dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);
        double a=Math.pow(Math.sin(dLat/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(dLon/2),2);
        return 6371.0088*2*Math.asin(Math.sqrt(Math.min(1,a)));
    }
}
