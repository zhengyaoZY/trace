package com.zhengyaozy.trace;

interface LocationSource {
    interface Callback {
        void ready(double latitude,double longitude,boolean approximate);
        void failed(ExploreState.Status reason);
    }
    void start(Callback callback);
    void cancel();
}
