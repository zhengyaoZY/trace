package com.zhengyaozy.trace;

import android.view.View;
import java.util.List;
import java.util.function.Consumer;

interface MapSurface {
    int DOT_DP=14,HIT_DP=44;
    final class Camera {final MapPoint point;final float zoom;Camera(MapPoint point,float zoom){this.point=point;this.zoom=zoom;}}
    View view();
    MapPoint project(MapPoint point);
    void camera(MapPoint point,Float zoom);
    Camera snapshot();
    void fitRadius(MapPoint gps,int meters);
    void features(List<Attraction> entries,Consumer<String> select);
    void selected(String uid);
    void currentPosition(MapPoint gps);
    void route(List<Attraction.Waypoint> waypoints);
    void resume();void pause();void destroy();
}
