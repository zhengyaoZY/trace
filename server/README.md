# 寻迹后端 0.5.0

Node.js 24、TypeScript、Fastify 5。无账号、数据库或 Redis。Android 仅发送当前查询坐标，后端调用百度坐标转换与地点检索 3.0，统一过滤、缓存和计算气泡分数。**没有服务端 AK 时进程可启动，但不是线上就绪状态；真实百度数据与权限仍需配置后验证。**

## 本地与 Docker

```sh
cd server
npm ci
cp .env.example .env
# 在忽略提交的 .env 中安全填写 BAIDU_SERVER_AK
npm run build
npm start
```

默认监听 `0.0.0.0:3000`；仅需本机时设置 `HOST=127.0.0.1`。终止进程使用 Ctrl+C。Node 24 默认读取可选 `.env`，不会自动申请 AK。

```sh
cd server
docker compose up --build -d
curl http://127.0.0.1:3000/healthz
curl -i http://127.0.0.1:3000/readyz
docker compose down
```

Compose 默认仅暴露 `127.0.0.1:3000`。Android 模拟器访问宿主机时，Debug API 地址为 `http://10.0.2.2:3000`；真机本地联调可在可信局域网内配置 `BIND_ADDRESS=0.0.0.0`，使用电脑的内网 IP，并自行限制防火墙。不要将开发 HTTP 服务直接暴露公网。

`GET /healthz` 仅表示进程正常；`GET /readyz` 表示服务端 AK 是否已配置，**不代表百度鉴权和指标权限已通过**。探针不消耗百度请求、不参与业务限流。未配置 AK：ready 返回 503，业务接口返回 `UPSTREAM_NOT_CONFIGURED`。

公开部署不在本次范围。部署时需要 HTTPS 网关、域名、服务器出站 IP 和凭据；限流默认不信任转发 IP，避免伪造请求头，代理部署需结合网关限流调整。多副本的内存缓存和限流互不共享，后续再按需求引入共享存储。

## 百度凭据

- `BAIDU_SERVER_AK`：只在后端配置的**服务端／Web 服务类型** AK，开通地点检索 3.0、坐标转换，按百度控制台约束服务器出站 IP、服务范围、配额及授权。
- Android 的 `BAIDU_MAP_AK` 是另一把绑定包名和签名的客户端 AK，仅用于底图。两者不可混用，服务端 AK 不进入 APK。
- 密钥通过 `.env`、部署 Secret 或容器环境变量填写；不提交真实值、不在聊天和日志中粘贴。
- 真实 `ranking`、`d7_hot_value` 是否返回取决于地点数据和账号权限。官方字段缺失时返回 null，不承诺每个景点均有指标，不以评分或评论数替代。

## API 契约

`POST /api/v1/places/nearby`，`Content-Type: application/json`。定位放在请求体中，不放在 URL。

```json
{
  "center": {"latitude": 30.24, "longitude": 120.15, "coordinateSystem": "WGS84"},
  "section": "人文",
  "category": "历史",
  "radiusMeters": 1000,
  "refresh": false
}
```

- 人文：`全部 / 历史 / 美食`；自然：`全部 / 自然景区`。自驾和徒步为 Android 本地示例，不调用本接口。
- 半径只允许 1000，坐标必须是有效 WGS84；不接受自定义百度 URL、AK、关键词或额外字段。历史、美食、自然的关键词由服务端固定映射。
- 后端使用官方 `/geoconv/v1/` 将 WGS84 转 BD-09，之后调用 `/place/v3/around`，`coord_type=3`、`scope=2`、`radius=1000`、`radius_limit=true`，最多五页、每页二十条。
- 返回 `source=baidu`、`center`（BD09）、`radiusMeters=1000`、`fetchedAt/expiresAt`（UTC 毫秒时间戳）、`selectedRankBoard`、`items`。
- 每个 item 包含 `id/name/address/category`、`location={latitude,longitude,coordinateSystem:BD09}`、`distanceMeters`、`rankingRaw`、`rank={board,position}|null`、`heat7d`、`score`、`scoreSource`。
- 原始排名先从 `detail_info.ranking` 读取，兼容顶层同名字段；热度从 `detail_info.d7_hot_value` 或顶层读取。热度必须为非负有限数。当前严格解析“榜单名第N名”（正整数）；未知格式保留原文但不参与计算，真实格式需配置 AK 后联调，不凭搜索顺序推断名次。
- 按 UID 去重、统一 BD09 坐标计算球面距离，仅保留一公里内的最多一百个 POI。优先保留距离近的候选，再计算指标并按综合分数降序、距离、UID 排序。百度分页不保证召回全量附近地点。

### 分数与缓存

排名使用本次结果覆盖最多的榜单；覆盖数相同时按榜单名称稳定选择。其他榜单的原文可以显示，但不混算名次。`R=1/名次`；`H=log(1+七天热度)/log(1+本次最大热度)`，最大为零时 H=0。有两项取均值，仅有一项用该项，均缺失 `score=null`。`scoreSource=rank_heat/rank/heat/none`，是**本应用综合指数，不是百度官方总榜**。

服务端保存最多 256 组查询及 256 组坐标转换的进程内缓存，有效五分钟；合并同查询的并发请求。显式刷新也要距离上次获取至少三十秒；缓存命中保留原始获取和到期时间，不伪装新数据。Android 按服务端到期时间缓存，避免两层缓存叠加变旧。重启后缓存清空，无持久化定位历史。

### 错误、限流与隐私

每 IP 每分钟最多三十次业务请求；总查询二十秒超时，单次上游调用五秒超时；超时不静默返回部分分页数据。Android 总请求二十五秒，取消和旧响应不会覆盖新位置或分类。

错误返回 `{"error":{"code":"…","message":"…"},"requestId":"…"}`：

| HTTP | code | 含义 |
|---|---|---|
|400|INVALID_REQUEST / INVALID_CATEGORY|参数或分类组合错误|
|429|RATE_LIMITED|本服务限流，含 Retry-After|
|503|UPSTREAM_NOT_CONFIGURED / UPSTREAM_AUTH|服务端缺少配置或百度鉴权失败|
|503|UPSTREAM_QUOTA|百度配额不足，含 Retry-After|
|502|UPSTREAM_NETWORK / UPSTREAM_BAD_RESPONSE / UPSTREAM_UNAVAILABLE|上游网络或数据异常|
|504|UPSTREAM_TIMEOUT|超时|

日志只记录请求 ID、方法、受控错误代码、状态及耗时；不记录请求体、坐标、AK、IP 或带 AK 的上游 URL。IP 仅用于短时进程内限流。用户必须重新确认后端接收位置的用途；不得把此前仅百度 SDK 的同意视为允许向新后端发送位置。

## 验证

```sh
npm ci
npm test
docker build -t trace-api .
```

测试注入上游替身，不用真实 AK，也不消耗真实百度配额；覆盖一公里边界、转换、分页、去重、缓存与并发、刷新、超时、指标与排名榜单、参数验证、限流和错误脱敏。CI 额外构建镜像并验证无 AK 的健康与非就绪行为。真实 AK、坐标无偏移、实际排名热度权限及真机联调是后续外部验收前提。
