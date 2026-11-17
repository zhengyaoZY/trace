# 寻迹 Android 0.6.0 — 后端接口接入预览

Android 8.0+，Java，青绿色扁平 UI。**当前仓库 APK 未配置公网 API 地址或地图 Android Key，后端也未配置真实服务端 Key；不能视为真实地图、POI 和指标已可用。**可浏览本地示例和文档。

## 架构与行为

- 人文：历史、美食；自然：自然景区、自驾路线、徒步路线；我的：应用文档。
- `Android WGS84定位 → 寻迹 HTTP API v2（provider=amap 默认 / baidu）→ 对应坐标转换和周边检索 → 服务端清洗/计算 → Android展示`。默认底图为高德 3D SDK 10.0.600，百度 Map/Util SDK 9.0.0 原样保留可切换；客户端不再使用百度 Search SDK，不保存服务端 AK。
- 手机位置为固定中心、半径 1000 米。手势只改变视野；定位按钮刷新中心和数据。取消请求、迟到响应、分类与详情返回状态均保留。
- 后端控制分类、分页、UID 去重、距离过滤、缓存及排序；Android 保留后端顺序，以服务端 `expiresAt` 控制缓存，不再延长有效期或把 GPS 与 BD09 混算距离。高德 POI 为 GCJ02，百度 POI 为 BD09，地图不重复转换、禁止跨服务商坐标混用；客户端缓存按服务商隔离；本地路线仍为 GPS 示例。
- 官方排名和七天热度各占 50%，一项缺失则使用另一项，全缺失为 14dp 中性圆点。不用评分、评论量或搜索顺序冒充热度。综合指数由本应用计算，并非百度官方总榜。
- 圆点直径 `sqrt(14² + score*(40²-14²)) dp`，点击区至少44dp；无名称、无序号，选中加描边，不放大。保留卡片联动和原始指标说明。
- 自驾/徒步为本地静态示意，按起点一公里范围筛选，附在自然列表后，最多显示一百条内容，不参与官方热度计算。路线非导航，不保证连线对应可通行道路。
- 地图与后端的可用性独立：缺少地图 AK 时仍可请求已配置的后端并展示卡片；缺少后端地址、鉴权/配额/限流/网络失败时明确提示，不以示例冒充真实数据。

## 默认高德与切换

首次安装或旧版本升级默认高德。在「我的 → 地图与服务使用配置」可选择高德或百度；选择持久保存，切换时清空视野、选中项及在途请求，不自动回退到另一家。两套 SDK、后端适配器、文档和百度旧 API 均保留。缺少高德 Key 时明确提示，不会偷偷初始化百度。

高德 POI 2.0 没有与百度相同的官方排名和七天热度字段，因此高德当前为中性小圆点；不将评分或返回次序当作热度。百度真实返回指标时仍按原算法调整圆点大小，两家均不标序号。

## 两类配置

### 高德 Android Key（默认地图）

在高德开放平台创建 **Android 平台 / 3D 地图 SDK** Key，绑定包名 `com.zhengyaozy.trace` 与下方同一签名 SHA-1。在忽略提交的 `android/amap.properties` 填写 `AMAP_MAP_KEY=…`（参考同目录 example），或设置同名环境变量。CI 使用 Secret `AMAP_MAP_KEY`。

后端另配 **Web 服务 Key** `AMAP_SERVER_KEY`，启用坐标转换、POI 搜索 2.0（周边搜索），按控制台要求限制服务器出站 IP。不要将服务端 Key 写入 Android；Android Key 和 Web Key 不可混用。底图不使用高德定位 SDK，手机位置仍由系统提供。


### 百度 Android 地图 AK（保留）

在百度开放平台创建 Android SDK 类型 AK，绑定包名 `com.zhengyaozy.trace` 与当前 APK 签名：

```text
AA:0E:47:B6:82:9F:79:50:60:36:0A:E6:ED:A5:C4:A8:92:34:85:F0
```

在忽略提交的 `android/baidu.properties` 中填写 `BAIDU_MAP_AK=…`，或给构建进程设置同名环境变量。CI 使用 GitHub Actions Secret `BAIDU_MAP_AK`。此客户端凭据会进入 APK，须限制服务与配额，不能当作服务端秘密。

### 本应用 API 地址

在忽略提交的 `android/api.properties` 中填写：

```properties
# Android 模拟器访问电脑上的本地服务，仅 Debug 允许
API_BASE_URL=http://10.0.2.2:3000
```

或设置构建环境变量 `API_BASE_URL`（优先）。CI 使用同名仓库 Variable。协议细节与服务端启动见 [后端文档](../server/README.md)。API 接口不接受客户端传递地图平台密钥。

Debug 只允许 localhost、模拟器宿主地址和私有 IPv4 网段的 HTTP；其他地址必须 HTTPS。Release 全面禁止 HTTP，Manifest 也禁用明文。API 地址不能包含用户名、密码、查询参数或 fragment，**不要把密钥放在 URL**。默认地址为空，不会偷偷连接第三方服务。

真机联调使用电脑局域网 IP，而非10.0.2.2；后端需绑定可信局域网接口并允许防火墙访问。公开部署不在本次范围，应使用域名和 HTTPS 网关。`AMAP_SERVER_KEY` 与 `BAIDU_SERVER_AK` 仅由后端读取，绝不能放进 Android 配置或 APK。

## 隐私与定位

隐私同意升级为第3版，分别按高德/百度记录，老版本 SDK 同意不能授权新后端接收位置：升级首次使用会重新说明位置发送到寻迹后端、再转发当前所选服务商的用途。未同意不初始化 SDK、不请求定位和后端；暂不使用仍可阅读文档和本地示例。

系统 LocationManager 获取一次前台定位，支持大致位置，缓存两分钟；进入后台取消请求，不保存轨迹、不申请后台定位。拒绝、关闭或超时十五秒时使用明确标注的杭州示例区域。所选地图 SDK 的必要设备信息与地图请求、后端短时内存缓存均在应用文档说明。

## 构建、安装和验证

JDK17/21、SDK35、Build Tools35.0.0、Gradle8.11.1、AGP8.9.1。

```sh
cd android
./gradlew assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
./scripts/smoke.sh # x86_64 模拟器使用独立构建参数
```

仓库 `downloads/trace-android-debug.apk` 是同签名0.6.0预览包，可覆盖前版，不能安装到 iPhone。含 arm64-v8a / armeabi-v7a 两个手机架构，约94MiB；双 SDK 全架构 APK 超过 GitHub 单文件限制。无公网服务配置和地图 Key时只能体验本地文本及说明，不能使用真实地图/周边数据。

JUnit 覆盖坐标类型、客户端不重排服务端结果、缓存不过期延长、取消与迟到响应、圆点面积单调性。模拟器 UI 使用明确标注的测试地图/POI，验证大小、联动、详情状态、隐私拒绝、配置缺失、后台取消、错误重试；另用真实回环 HTTP 测试服务验证 POST/WGS84、高德 GCJ02 / 百度 BD09、服务商校验、指标/缓存到期、错误及协议约束。CI 分别用1.0与1.3字体运行，不代表真实地图平台验收。

配置后需要真机确认：升级后重新同意、真实地图鉴权、精确/大致定位、坐标无偏移、一公里POI、官方指标实际返回和格式、缺失指标、重叠圆点点击、断网/限流、后台停止及两家 SDK 署名。禁止把模拟验证当成真实数据已接通。

高德该版本 Maven SDK 仅含 ARM 原生库，真实底图请在 ARM 真机验证；x86_64 UI 测试使用明确标注的测试地图，不认证真实高德 SDK。可用 `./gradlew -PtraceAbis=x86_64 assembleDebug` 构建模拟器包（选择高德时给出架构提示）；`smoke.sh` 自动传入该参数。原生库及地图 assets 都由 Maven 依赖打入 APK，无需手工下载。

官方参考：[高德 POI 2.0](https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch)、[高德隐私政策](https://lbs.amap.com/pages/privacy/)。当前仓库未提供 Key 或公网地址，真实高德/百度鉴权与位置效果需配置后验收。
