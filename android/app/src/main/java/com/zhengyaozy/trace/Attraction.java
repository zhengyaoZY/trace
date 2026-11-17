package com.zhengyaozy.trace;
import java.util.List;
public final class Attraction {
    public final String id, name, subtitle, introduction, narration;
    public final double latitude, longitude;
    public final boolean online,bd09;
    public final String provider,coordinateSystem;
    public final Metrics metrics;
    static final class Metrics {
        final Double score,heat7d;final String scoreSource,rankingRaw,rankBoard;final Integer rank;
        Metrics(Double score,String scoreSource,Double heat7d,String rankingRaw,String rankBoard,Integer rank){this.score=score;this.scoreSource=scoreSource;this.heat7d=heat7d;this.rankingRaw=rankingRaw;this.rankBoard=rankBoard;this.rank=rank;}
    }
    Attraction withMetrics(Metrics metrics){return new Attraction(id,name,subtitle,introduction,narration,latitude,longitude,section,category,manager,source,waypoints,online,metrics,provider);}
    public final String section, category, manager, source;
    public final java.util.List<Waypoint> waypoints;
    public static final class Waypoint {
        public final String name;
        public final double latitude, longitude;
        public Waypoint(String name,double latitude,double longitude){this.name=name;this.latitude=latitude;this.longitude=longitude;}
    }
    private Attraction(String id, String name, String subtitle, String introduction, String narration, double latitude, double longitude) {
        this(id,name,subtitle,introduction,narration,latitude,longitude,
            id.equals("west-lake")?"自然":"人文",id.equals("west-lake")?"自然景区":"历史",
            id.equals("west-lake")?"杭州西湖风景名胜区管理委员会":"",
            id.equals("west-lake")?"https://westlake.hangzhou.gov.cn/":"",java.util.Collections.emptyList());
    }
    private Attraction(String id,String name,String subtitle,String introduction,String narration,double latitude,double longitude,
            String section,String category,String manager,String source,java.util.List<Waypoint> waypoints){
        this(id,name,subtitle,introduction,narration,latitude,longitude,section,category,manager,source,waypoints,false);
    }
    private Attraction(String id,String name,String subtitle,String introduction,String narration,double latitude,double longitude,
            String section,String category,String manager,String source,java.util.List<Waypoint> waypoints,boolean online){
        this(id,name,subtitle,introduction,narration,latitude,longitude,section,category,manager,source,waypoints,online,null);
    }
    private Attraction(String id,String name,String subtitle,String introduction,String narration,double latitude,double longitude,
            String section,String category,String manager,String source,java.util.List<Waypoint> waypoints,boolean online,Metrics metrics){
        this(id,name,subtitle,introduction,narration,latitude,longitude,section,category,manager,source,waypoints,online,metrics,online?"baidu":"local");
    }
    private Attraction(String id,String name,String subtitle,String introduction,String narration,double latitude,double longitude,
            String section,String category,String manager,String source,java.util.List<Waypoint> waypoints,boolean online,Metrics metrics,String provider){
        this.metrics=metrics;this.provider=provider;
        this.coordinateSystem=online?MapVendor.from(provider).datum:"WGS84";
        this.online=online;this.bd09="BD09".equals(coordinateSystem);
        this.id=id;this.name=name;this.subtitle=subtitle;this.introduction=introduction;this.narration=narration;
        this.latitude=latitude;this.longitude=longitude;this.section=section;this.category=category;
        this.manager=manager;this.source=source;this.waypoints=java.util.Collections.unmodifiableList(waypoints);
    }
    static Attraction poi(String uid,String name,String address,String tag,double lat,double lon,String section,String category){
        return providerPoi(MapVendor.BAIDU,uid,name,address,lat,lon,section,category);
    }
    static Attraction providerPoi(MapVendor vendor,String uid,String name,String address,double lat,double lon,String section,String category){
        String description=address==null||address.isEmpty()?"地址暂缺":address;
        return new Attraction(vendor.id+":"+uid,name,description,description,"暂无介绍",lat,lon,section,category,"",vendor.label+"地图 POI",java.util.Collections.emptyList(),true,null,vendor.id);
    }
    public boolean isRoute(){return !waypoints.isEmpty();}
    public double distanceKm(){double total=0;for(int i=1;i<waypoints.size();i++){Waypoint a=waypoints.get(i-1),b=waypoints.get(i);total+=ExploreState.distanceKm(a.latitude,a.longitude,b.latitude,b.longitude);}return total;}
    public static final List<Attraction> SAMPLES = java.util.Collections.unmodifiableList(java.util.Arrays.asList(
new Attraction("west-lake", "西湖", "湖山之间，读懂杭州", "西湖位于杭州城西。湖面、堤岸与远山相映，是感受杭州山水与城市生活的好去处。", "从湖边慢慢走起，把脚步交给柳荫和水声。苏堤与白堤将湖面分出不同层次，晴日可以看山色，雨天则适合看水面上的薄雾。\n\n西湖的故事里有白居易、苏轼，也有世代居住在湖畔的普通人。你可以在长椅上停一停，看看划过的游船，体会这片山水如何融入杭州人的日常。\n\n建议沿公共步道漫步，爱护湖岸植被；游船与景区服务以现场公告为准。", 30.2400, 120.1500),
new Attraction("leifeng-pagoda", "雷峰塔", "夕照中的湖畔地标", "雷峰塔坐落在西湖南岸夕照山上，与湖光山色共同构成杭州经典的城市记忆。", "站在西湖南岸望向雷峰塔，塔影与山的轮廓叠在一起。雷峰夕照是西湖传统景观之一，傍晚时分的光线尤其柔和。\n\n旧塔始建于吴越国时期，后曾倒塌。如今所见是新建的保护与展示建筑，承载着人们对历史与传说的共同记忆。白蛇传中的故事让这座塔更为人熟知，但传说与史实需要分别理解。\n\n若计划登塔，请先查看景区当日开放和票务信息。", 30.2313, 120.1489),
new Attraction("grand-canal", "拱宸桥", "沿着运河，走进市井", "拱宸桥横跨京杭大运河杭州段，桥畔街巷保留着运河沿岸的生活气息。", "走近拱宸桥，先看看桥下流动的运河。水路曾把货物、手艺和远方的消息带到杭州，也让两岸形成热闹的街市。\n\n石拱桥连接的不只是两岸道路，还有城市的过去与现在。沿桥西街区散步，可以观察老建筑的尺度与临水街巷的布局，想象舟船往来的生活场景。\n\n桥面有台阶，雨后请注意防滑。参观周边场馆前，请查询各场馆的开放安排。", 30.3195, 120.1383),
new Attraction("hefang-food","河坊街风味","从一碗片儿川认识杭州","以河坊街一带为示例，认识杭州面食与街巷饮食文化；坐标为街区参考点，并非具体商家。",
    "片儿川常以雪菜、笋片和肉片入面，滋味里有杭州日常生活的节奏。除了看风景，也可以从一碗热面认识城市。\n\n这里提供的是饮食文化示例，不是餐厅推荐榜单。街区内商铺、菜单和营业时间可能变化，请以现场信息为准，点餐时可先确认食材与过敏原。",30.2381,120.1712,"人文","美食","","",java.util.Collections.emptyList()),
new Attraction("westlake-drive","西湖外围自驾示意","沿山水边缘，慢慢出发","用于演示自驾路线的静态示意，不是可直接跟随的道路导航；不保证连线对应可通行道路。",
    "路线用三个参考点呈现西湖南部至西部的空间关系。可先阅读沿途介绍，再自行查询实际道路、限行与停车信息。\n\n图上连线仅连接示例途经点，不代表道路走向。距离是折线估算，不含道路绕行；请勿依据本示意驾驶。无实时路况、偏航提醒或导航指令。",30.2313,120.1489,"自然","自驾路线","","",java.util.Arrays.asList(
        new Waypoint("南山路参考点",30.2313,120.1489),new Waypoint("虎跑路参考点",30.2185,120.1325),new Waypoint("龙井路参考点",30.2422,120.1190))),
new Attraction("jiuxi-walk","九溪徒步示意","沿溪入山，感受林间","用于演示徒步路线的静态示意，不是经过实测的轨迹；出行前需自行确认开放、天气与道路情况。",
    "九溪一带的溪流、林木与村落构成另一种杭州印象。示例用九溪入口、九溪烟树与龙井村三个参考点展示空间关系，供了解区域使用。\n\n图上折线不是实测步道，不保证点间可以直接通行。距离为示意估算，不代表实际步行距离；无海拔、难度或安全等级评定。不提供实时导航，请按现场开放路线和标识行走。",30.1948,120.1190,"自然","徒步路线","","",java.util.Arrays.asList(
        new Waypoint("九溪入口参考点",30.1948,120.1190),new Waypoint("九溪烟树参考点",30.2086,120.1119),new Waypoint("龙井村参考点",30.2202,120.1038)))
));
}
