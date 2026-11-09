import Foundation
import CoreLocation

struct Attraction: Identifiable, Hashable {
    let id: String
    let name: String
    let subtitle: String
    let symbol: String
    let latitude: Double
    let longitude: Double
    let introduction: String
    let narration: String

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    static let samples: [Attraction] = [
        Attraction(
            id: "west-lake", name: "西湖", subtitle: "湖山之间，读懂杭州", symbol: "water.waves",
            latitude: 30.2400, longitude: 120.1500,
            introduction: "西湖位于杭州城西。湖面、堤岸与远山相映，是感受杭州山水与城市生活的好去处。",
            narration: "从湖边慢慢走起，把脚步交给柳荫和水声。苏堤与白堤将湖面分出不同层次，晴日可以看山色，雨天则适合看水面上的薄雾。\n\n西湖的故事里有白居易、苏轼，也有世代居住在湖畔的普通人。你可以在长椅上停一停，看看划过的游船，体会这片山水如何融入杭州人的日常。\n\n建议沿公共步道漫步，爱护湖岸植被；游船与景区服务以现场公告为准。"
        ),
        Attraction(
            id: "leifeng-pagoda", name: "雷峰塔", subtitle: "夕照中的湖畔地标", symbol: "building.columns",
            latitude: 30.2313, longitude: 120.1489,
            introduction: "雷峰塔坐落在西湖南岸夕照山上，与湖光山色共同构成杭州经典的城市记忆。",
            narration: "站在西湖南岸望向雷峰塔，塔影与山的轮廓叠在一起。雷峰夕照是西湖传统景观之一，傍晚时分的光线尤其柔和。\n\n旧塔始建于吴越国时期，后曾倒塌。如今所见是新建的保护与展示建筑，承载着人们对历史与传说的共同记忆。白蛇传中的故事让这座塔更为人熟知，但传说与史实需要分别理解。\n\n若计划登塔，请先查看景区当日开放和票务信息。"
        ),
        Attraction(
            id: "grand-canal", name: "拱宸桥", subtitle: "沿着运河，走进市井", symbol: "point.topleft.down.to.point.bottomright.curvepath",
            latitude: 30.3195, longitude: 120.1383,
            introduction: "拱宸桥横跨京杭大运河杭州段，桥畔街巷保留着运河沿岸的生活气息。",
            narration: "走近拱宸桥，先看看桥下流动的运河。水路曾把货物、手艺和远方的消息带到杭州，也让两岸形成热闹的街市。\n\n石拱桥连接的不只是两岸道路，还有城市的过去与现在。沿桥西街区散步，可以观察老建筑的尺度与临水街巷的布局，想象舟船往来的生活场景。\n\n桥面有台阶，雨后请注意防滑。参观周边场馆前，请查询各场馆的开放安排。"
        )
    ]
}
