# Android 安装包

[下载寻迹 0.4.0 接入预览 APK](https://github.com/zhengyaoZY/trace/raw/refs/heads/feat/android-bootstrap/downloads/trace-android-debug.apk)

**当前包未配置百度 AK，真实百度地图与在线周边检索尚不可用、未完成线上验证。**可阅读本地示例与应用文档；不会用模拟地图或数据伪装真实接入。开通与配置步骤见 [Android 文档](../android/README.md#开通百度地图)。

适用于 Android 8.0+，不能安装到 iPhone。下载原始 APK，允许浏览器安装未知应用后打开安装。沿用之前调试签名，可覆盖安装；其他机器的构建可能具有不同签名。

- 版本：0.4.0（4），包名 `com.zhengyaozy.trace`
- 源码提交：`2a5cdd3`
- 构建：JDK 17、Gradle 8.11.1、AGP 8.9.1、SDK 35、百度 Map/Search/Util 9.0.0
- 类型：个人试用的 Debug APK，含 arm64-v8a、armeabi-v7a、x86_64 原生库，约 80 MiB
- 签名：APK v2 校验通过
- 签名证书 SHA-256：`5bfd7f21b91cd5ae0f8515cc153c1da3f4c15975042b44983ff3d2bc7922732f`
- 百度 AK 绑定 SHA-1：`AA:0E:47:B6:82:9F:79:50:60:36:0A:E6:ED:A5:C4:A8:92:34:85:F0`

版本变化：周边范围改为半径一公里；政府管理景区改为自然景区；圆点无序号、等大，保留卡片联动。SDK 无官方排名与近七天热度字段，本阶段不实现热度大小变化。没有业务后台。

从仓库根目录运行 `sha256sum -c downloads/SHA256SUMS` 检查完整性。签名私钥不随仓库分发，正式发布应使用安全保存的 Release 签名。
