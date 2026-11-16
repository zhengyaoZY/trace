package com.zhengyaozy.trace;
import java.util.List;
public final class Attraction {
    public final String id, name, subtitle, introduction, narration;
    public final double latitude, longitude;
    private Attraction(String id, String name, String subtitle, String introduction, String narration, double latitude, double longitude) {
        this.id=id; this.name=name; this.subtitle=subtitle; this.introduction=introduction; this.narration=narration;
        this.latitude=latitude; this.longitude=longitude;
    }
    public static final List<Attraction> SAMPLES = java.util.Arrays.asList(
new Attraction("west-lake", "西湖", "湖山之间，读懂杭州", "西湖位于杭州城西。湖面、堤岸与远山相映，是感受杭州山水与城市生活的好去处。", "从湖边慢慢走起，把脚步交给柳荫和水声。苏堤与白堤将湖面分出不同层次，晴日可以看山色，雨天则适合看水面上的薄雾。\\n\\n西湖的故事里有白居易、苏轼，也有世代居住在湖畔的普通人。你可以在长椅上停一停，看看划过的游船，体会这片山水如何融入杭州人的日常。\\n\\n建议沿公共步道漫步，爱护湖岸植被；游船与景区服务以现场公告为准。", 30.2400, 120.1500),
new Attraction("leifeng-pagoda", "雷峰塔", "夕照中的湖畔地标", "雷峰塔坐落在西湖南岸夕照山上，与湖光山色共同构成杭州经典的城市记忆。", "站在西湖南岸望向雷峰塔，塔影与山的轮廓叠在一起。雷峰夕照是西湖传统景观之一，傍晚时分的光线尤其柔和。\\n\\n旧塔始建于吴越国时期，后曾倒塌。如今所见是新建的保护与展示建筑，承载着人们对历史与传说的共同记忆。白蛇传中的故事让这座塔更为人熟知，但传说与史实需要分别理解。\\n\\n若计划登塔，请先查看景区当日开放和票务信息。", 30.2313, 120.1489),
new Attraction("grand-canal", "拱宸桥", "沿着运河，走进市井", "拱宸桥横跨京杭大运河杭州段，桥畔街巷保留着运河沿岸的生活气息。", "走近拱宸桥，先看看桥下流动的运河。水路曾把货物、手艺和远方的消息带到杭州，也让两岸形成热闹的街市。\\n\\n石拱桥连接的不只是两岸道路，还有城市的过去与现在。沿桥西街区散步，可以观察老建筑的尺度与临水街巷的布局，想象舟船往来的生活场景。\\n\\n桥面有台阶，雨后请注意防滑。参观周边场馆前，请查询各场馆的开放安排。", 30.3195, 120.1383));
}
