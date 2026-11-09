# 寻迹 Trace

一个以杭州为示例的导游型 iOS 应用基础工程，使用 SwiftUI 和 MapKit，支持 iOS 17.0 及以上的 iPhone、iPad。界面为简体中文，采用青绿色主题，支持系统深浅色和动态字体。

## 功能

- **发现**：西湖、雷峰塔、拱宸桥的示例卡片，进入详情阅读景点介绍和文字讲解。
- **地图**：显示三个景点标记，点选标记后可进入详情；详情中也显示景点位置。
- **我的**：展示应用介绍、版本及隐私说明，无登录流程。

景点内容和坐标内置在应用中，无第三方依赖、账号、业务后台、API 密钥或定位权限。MapKit 底图由系统联网加载；离线时仍可浏览景点文本，底图可用性取决于系统缓存。示例讲解不含音频，开放时间、票务以景区公告为准。

## 在 Xcode 中运行

1. 在 macOS 上安装 **Xcode 15 或更新版本**（建议 Xcode 16），并在 Xcode 设置中安装 iOS Simulator 运行时。
2. 克隆仓库，打开根目录的 `Trace.xcodeproj`，无需运行包管理或工程生成命令。
3. 选择共享 Scheme **Trace**，选择 iOS 17+ 的 iPhone 或 iPad 模拟器，按 `⌘R`。
4. 若在真机运行，在 Target → Signing & Capabilities 中选择自己的开发团队，并按需修改 Bundle Identifier。模拟器构建无需配置签名。

命令行构建（仓库根目录）：

```sh
xcodebuild \
  -project Trace.xcodeproj \
  -scheme Trace \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/DerivedData \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## 验证

GitHub Actions 工作流 `.github/workflows/ios-build.yml` 在 `macos-15` 上执行上述模拟器构建，分支推送、面向 main 的 PR 和手动触发均支持。日志作为 `ios-build-log` 工件保留。构建验证不等于 UI 自动化测试。

运行后手动检查：

1. 发现页显示三个杭州景点；逐一进入详情，确认文字讲解可滚动阅读。
2. 地图页显示三个标记；点选后打开对应详情，返回时导航正常。
3. 我的页显示版本和隐私说明，首次启动不弹出登录或定位请求。
4. 切换系统深浅色、放大字体，在 iPhone 和 iPad 模拟器上检查排版。

## 工程结构

- `Trace/TraceApp.swift`：应用入口及语言、主题设置。
- `Trace/Attraction.swift`：景点模型、示例数据与讲解。
- `Trace/ContentView.swift`：导航、发现卡片、详情、地图与我的页面。
- `Trace/Assets.xcassets`：主题色。
- `Trace.xcodeproj`：可直接打开的 Xcode 工程与共享 Scheme。

本工程是开发基础版本，未配置 App Store 分发图标、签名或上架资源。
