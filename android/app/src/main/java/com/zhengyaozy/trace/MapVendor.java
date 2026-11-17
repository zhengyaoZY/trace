package com.zhengyaozy.trace;

/** Stable identifiers are shared with the HTTP API; absence of a preference means AMap. */
enum MapVendor {
    AMAP("amap","高德","GCJ02"), BAIDU("baidu","百度","BD09");
    final String id,label,datum;
    MapVendor(String id,String label,String datum){this.id=id;this.label=label;this.datum=datum;}
    static MapVendor from(String id){return "baidu".equals(id)?BAIDU:AMAP;}
}
