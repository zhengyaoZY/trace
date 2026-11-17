package com.zhengyaozy.trace;
import java.util.List;
interface PoiProvider {
    enum Failure { NETWORK, AUTH, QUOTA, UNAVAILABLE, CONFIG, SERVER_CONFIG, TIMEOUT, RATE_LIMIT }
    final class Result {
        final List<Attraction> places;final long expiresAt;
        Result(List<Attraction> places,long expiresAt){this.places=places;this.expiresAt=expiresAt;}
    }
    interface Callback {void success(Result result);void failure(Failure reason);}
    interface Request {void cancel();}
    /** WGS84 request; BD-09 response. Service owns filtering, scoring, order and freshness. */
    Request search(MapPoint gps,String section,String category,boolean refresh,Callback callback);
}
