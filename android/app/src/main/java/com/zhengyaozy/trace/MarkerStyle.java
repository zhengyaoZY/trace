package com.zhengyaozy.trace;
final class MarkerStyle {
    static float diameterDp(Double score){return score==null||!Double.isFinite(score)?14f:(float)Math.sqrt(196+Math.max(0,Math.min(1,score))*(1600-196));}
}
