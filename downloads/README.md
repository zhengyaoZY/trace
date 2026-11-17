# Android 0.5.0 前后端接入预览

[下载寻迹 APK](https://github.com/zhengyaoZY/trace/raw/refs/heads/feat/android-bootstrap/downloads/trace-android-debug.apk)

**当前包未配置 API 地址和百度 Android AK，后端也没有真实服务端 AK；不代表真实地图、周边及指标已接通。**可阅读本地文本和文档。按[Android 配置文档](../android/README.md)与[后端运行文档](../server/README.md)配置后，再验收真实百度功能。本次仅交付本地与 Docker，不部署公网。

- 版本：0.5.0（5）；Android 8.0+，不能安装到 iPhone。
- 包名：`com.zhengyaozy.trace`；源码提交：`253b6f3`。
- 沿用原调试签名，可覆盖安装；APK v2 签名校验通过。
- 签名 SHA-256：`5bfd7f21b91cd5ae0f8515cc153c1da3f4c15975042b44983ff3d2bc7922732f`。
- Android 地图 AK 绑定 SHA-1：`AA:0E:47:B6:82:9F:79:50:60:36:0A:E6:ED:A5:C4:A8:92:34:85:F0`。
- 包含 arm64-v8a、armeabi-v7a、x86_64 原生库，约80MiB。

Android 定位通过 HTTP POST 请求本应用后端；服务端负责百度坐标转换、POI、缓存、限流与官方指标综合计算。无账号、无数据库，私钥与服务端 AK 不进入 APK。排名和热度缺失时仍显示中性点，不以评分或评论数冒充。

升级后需重新同意位置传到后端的用途。Debug 仅支持本地/内网 HTTP，Release 使用 HTTPS。未知应用安装权限由 Android 系统管理。

从仓库根目录执行 `sha256sum -c downloads/SHA256SUMS` 校验完整性。模拟数据测试不是百度线上鉴权和真机定位验收。
