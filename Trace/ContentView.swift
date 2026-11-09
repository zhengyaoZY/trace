import SwiftUI
import MapKit

struct ContentView: View {
    var body: some View {
        TabView {
            NavigationStack { DiscoverView() }
                .tabItem { Label("发现", systemImage: "sparkles") }
            NavigationStack { AttractionsMapView() }
                .tabItem { Label("地图", systemImage: "map") }
            NavigationStack { ProfileView() }
                .tabItem { Label("我的", systemImage: "person.crop.circle") }
        }
    }
}

private struct DiscoverView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("在杭州，循着故事出发")
                        .font(.largeTitle.bold())
                    Text("看一处风景，听一段城市往事。")
                        .foregroundStyle(.secondary)
                    Label("杭州 · 精选示例路线", systemImage: "leaf")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.accentColor)
                }
                ForEach(Attraction.samples) { attraction in
                    NavigationLink {
                        AttractionDetailView(attraction: attraction)
                    } label: {
                        AttractionCard(attraction: attraction)
                    }
                    .buttonStyle(.plain)
                }
                Text("以上为示例内容，开放时间与票务请以景区公告为准。")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
        .background(Color.accentColor.opacity(0.04))
        .navigationTitle("寻迹")
    }
}

private struct AttractionCard: View {
    let attraction: Attraction

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(Color.accentColor.opacity(0.12))
                Image(systemName: attraction.symbol)
                    .font(.system(size: 48))
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
            }
            .frame(height: 140)
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(attraction.name).font(.title2.bold())
                    Text(attraction.subtitle).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
            }
        }
        .padding()
        .background(.background, in: RoundedRectangle(cornerRadius: 24))
        .overlay {
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(Color.accentColor.opacity(0.15))
        }
        .accessibilityElement(children: .combine)
    }
}

struct AttractionDetailView: View {
    let attraction: Attraction

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Label(attraction.subtitle, systemImage: attraction.symbol)
                    .font(.headline)
                    .foregroundStyle(Color.accentColor)
                Text(attraction.introduction).font(.title3)
                Map(initialPosition: .region(MKCoordinateRegion(
                    center: attraction.coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
                ))) {
                    Marker(attraction.name, coordinate: attraction.coordinate)
                }
                .frame(height: 220)
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .accessibilityLabel("\(attraction.name)位置地图")
                VStack(alignment: .leading, spacing: 12) {
                    Label("文字讲解", systemImage: "text.book.closed")
                        .font(.title2.bold())
                    Text(attraction.narration)
                        .lineSpacing(7)
                        .textSelection(.enabled)
                }
                Text("讲解为内置示例文本，不含音频。")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
        .navigationTitle(attraction.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AttractionsMapView: View {
    @State private var selectedID: String?

    var body: some View {
        Map(initialPosition: .region(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 30.275, longitude: 120.145),
            span: MKCoordinateSpan(latitudeDelta: 0.13, longitudeDelta: 0.10)
        )), selection: $selectedID) {
            ForEach(Attraction.samples) { attraction in
                Marker(attraction.name, systemImage: attraction.symbol, coordinate: attraction.coordinate)
                    .tag(attraction.id)
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let attraction = Attraction.samples.first(where: { $0.id == selectedID }) {
                NavigationLink {
                    AttractionDetailView(attraction: attraction)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(attraction.name).font(.headline)
                            Text("查看景点详情与文字讲解").font(.subheadline)
                        }
                        Spacer()
                        Image(systemName: "arrow.right.circle.fill").font(.title2)
                    }
                    .padding()
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .padding()
                }
                .buttonStyle(.plain)
            } else {
                Text("轻点景点标记，探索它的故事")
                    .font(.subheadline)
                    .padding()
                    .background(.regularMaterial, in: Capsule())
                    .padding()
            }
        }
        .navigationTitle("杭州地图")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct ProfileView: View {
    var body: some View {
        List {
            Section {
                Label("你好，城市探索者", systemImage: "figure.walk")
                    .font(.headline)
                Text("从一段步行开始，发现身边的故事。")
                    .foregroundStyle(.secondary)
            }
            Section("关于寻迹") {
                LabeledContent("当前城市", value: "杭州")
                LabeledContent("版本", value: "0.1.0")
                Text("寻迹是一款导游型应用。本版本展示内置示例景点与文字讲解，无需登录。")
                Text("应用不请求定位权限，也不接入账号或业务后台。地图底图由 Apple MapKit 提供，加载时需要网络。")
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("我的")
    }
}

#Preview {
    ContentView()
}
