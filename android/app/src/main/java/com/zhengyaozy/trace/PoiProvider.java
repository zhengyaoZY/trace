package com.zhengyaozy.trace;

import java.util.List;

interface PoiProvider {
    enum Failure { NETWORK, AUTH, QUOTA, UNAVAILABLE }
    interface Callback { void success(List<Attraction> places); void failure(Failure reason); }
    interface Request { void cancel(); }
    /** center must be BD-09; provider returns BD-09 POIs, never local samples. */
    Request search(MapPoint center,String section,String category,Callback callback);
    static String keyword(String section,String category){
        if(section.equals("自然"))return "自然风光$风景区$公园";
        if(category.equals("历史"))return "文物古迹$历史建筑$博物馆";
        if(category.equals("美食"))return "美食";
        return "文物古迹$历史建筑$博物馆$美食";
    }
}
