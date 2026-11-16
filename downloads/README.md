# Android 安装包

[下载寻迹 APK](https://github.com/zhengyaoZY/trace/raw/refs/heads/feat/android-bootstrap/downloads/trace-android-debug.apk)

适用于 Android 8.0 及以上手机，**不能安装到 iPhone**。下载后打开 APK，按系统提示允许浏览器安装未知应用即可。不需要 Android Studio、开发者会员或连接电脑。

- 应用：寻迹 0.1.0（版本号 1）
- 包名：`com.zhengyaozy.trace`
- 类型：可独立安装的 Debug APK，使用开发调试证书签名
- 源码提交：`ddacfd4`
- 构建：Gradle 8.11.1、AGP 8.9.1、JDK 17、SDK 35
- 签名校验：`apksigner verify --verbose` 通过（APK Signature Scheme v2）
- 签名证书 SHA-256：`5bfd7f21b91cd5ae0f8515cc153c1da3f4c15975042b44983ff3d2bc7922732f`

从仓库根目录运行 `sha256sum -c downloads/SHA256SUMS` 可检查文件完整性。源码构建的 APK 因签名、构建元数据等差异可能具有不同的校验和。

本包用于个人试用；后续包若更换签名，需要先卸载旧版。私钥不随 APK 或仓库分发。正式版本应使用长期维护的 Release 签名。

开发与测试说明见 [Android 文档](../android/README.md)。
