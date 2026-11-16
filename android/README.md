# 寻迹 Android

原生 Java Android 工程，最低 Android 8.0（API 26），面向 Android 15（API 35）。简体中文、青绿色主题，包含发现、地图、我的三个入口，西湖、雷峰塔、拱宸桥的详情与文字讲解。无登录、业务后台、定位或存储权限。

## 下载与安装

初次体验使用 `downloads/trace-android-debug.apk`（仓库根目录，构建完成后提供）。在 **Android 手机**浏览器中下载原始 APK 文件，允许该浏览器“安装未知应用”，然后打开 APK 安装。它不能安装在 iPhone 上。

也可从 GitHub Actions 的 **Android APK** 工作流下载 `trace-android-debug` 工件，解压后安装 `app-debug.apk`（下载 Actions 工件需要登录 GitHub）。

这是自签名的调试版本，无需付费开发者会员，不存在 iOS 免费签名的七天限制。调试版本可用于个人体验，不适合正式分发。不同构建机器生成的调试签名可能不同，若提示签名不一致，卸载旧版后再安装。正式持续更新应使用单独保存的长期 release 签名，私钥和密码不应提交到 Git。

## 开发与构建

1. 安装 Android Studio、JDK 17（也可使用 JDK 21）、Android SDK Platform 35 和 Build Tools 35.0.0。
2. 在 Android Studio 中打开本目录 `android/`，等待 Gradle 同步；IDE 会生成本机 `local.properties`。
3. 选择 Android 8.0+ 真机或模拟器，运行 `app`。

命令行在本目录执行（SDK 路径通过 `ANDROID_HOME` 或本地 `local.properties` 配置）：

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
# 连接已允许 USB 调试的 Android 手机后：
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 运行导航冒烟测试（需要启动模拟器或连接真机）：
./gradlew connectedDebugAndroidTest
```

Gradle Wrapper 固定为 8.11.1，Android Gradle Plugin 为 8.9.1。下载依赖需要联网。

## 地图与离线行为

地图使用 osmdroid 6.1.20 和 OpenStreetMap 标准 HTTPS 瓦片，展示 WGS84 景点坐标，无需 API Key。不读取用户位置，不显示定位按钮。地图提供方会收到加载瓦片的网络请求，具体可用性受网络影响。遵守 OpenStreetMap 瓦片使用政策，不做批量下载或离线预取；地图底部保留贡献者署名。生产规模使用时应选择有服务保障的瓦片服务，osmdroid 为基础版本依赖，后续应评估维护方案。

所有景点文本内置，可离线阅读。地图不可用时仍可通过地图页底部景点按钮进入详情。

## 验证范围

- JUnit 校验示例景点数量、唯一 ID、讲解内容和杭州坐标范围。
- Android Lint 与 Debug APK 构建。
- Espresso 在模拟器上验证发现 → 景点详情 → 返回 → 地图 → 拱宸桥详情 → 我的。
- GitHub Actions 在 Linux 构建 APK，并在 Android 15 模拟器运行上述导航测试。

手工体验时建议额外检查：无网络下阅读讲解、地图缩放与标记点击、系统返回、旋转屏幕、放大系统字体。地图服务实时可用性与真机安装需在目标设备上确认。
