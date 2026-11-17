package com.zhengyaozy.trace;

/** Coordinate datum travels with every point to prevent applying GPS conversion twice. */
final class MapPoint {
    final double latitude,longitude;
    final boolean bd09;
    MapPoint(double latitude,double longitude,boolean bd09){this.latitude=latitude;this.longitude=longitude;this.bd09=bd09;}
    MapPoint toBd09(java.util.function.Function<MapPoint,MapPoint> converter){
        if(bd09)return this;
        MapPoint converted=converter.apply(this);
        if(!converted.bd09)throw new IllegalArgumentException("converter must return BD-09");
        return converted;
    }
    static MapPoint gps(double lat,double lon){return new MapPoint(lat,lon,false);}
}
