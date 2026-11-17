package com.zhengyaozy.trace;

/** Every point carries its datum. Provider coordinates never pass through a GPS converter. */
final class MapPoint {
    final double latitude,longitude;
    final boolean bd09;
    final String coordinateSystem;
    MapPoint(double lat,double lon,boolean bd09){this(lat,lon,bd09?"BD09":"WGS84");}
    MapPoint(double lat,double lon,String datum){
        if(!"WGS84".equals(datum)&&!"BD09".equals(datum)&&!"GCJ02".equals(datum))throw new IllegalArgumentException("datum");
        latitude=lat;longitude=lon;coordinateSystem=datum;bd09="BD09".equals(datum);
    }
    private MapPoint convert(String target,java.util.function.Function<MapPoint,MapPoint> converter){
        if(target.equals(coordinateSystem))return this;
        if(!"WGS84".equals(coordinateSystem))throw new IllegalArgumentException("Cross-provider conversion is forbidden");
        MapPoint converted=converter.apply(this);
        if(!target.equals(converted.coordinateSystem))throw new IllegalArgumentException("Unexpected conversion datum");
        return converted;
    }
    MapPoint toBd09(java.util.function.Function<MapPoint,MapPoint> converter){return convert("BD09",converter);}
    MapPoint toGcj02(java.util.function.Function<MapPoint,MapPoint> converter){return convert("GCJ02",converter);}
    static MapPoint gps(double lat,double lon){return new MapPoint(lat,lon,"WGS84");}
}
