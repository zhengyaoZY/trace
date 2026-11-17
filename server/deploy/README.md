# 阿里云 ACR 与远程发布

本目录提供只拉取已构建镜像的运行配置。镜像不含 AK、仓库凭据、SSH 私钥或会话代理证书。镜像内应用以非 root 用户运行，运行配置使用只读文件系统和临时 `/tmp`。

## 发布所需信息

- ACR 完整地址：`仓库域名/命名空间/仓库名`、账号类型/地域与登录用户名；私有仓库需要推送权限和服务器拉取权限。
- 服务器：SSH 用户、主机、端口、受信任主机记录及认证方式；发布前检查 CPU 架构、Docker/Compose、现有端口、容器和反向代理，保留现有服务。
- API 域名及 HTTPS 入口。没有域名时只能做受控端口联调，不能宣称 Android Release 的公网 HTTPS 验收完成。
- 默认高德 **Web 服务类型 Key** `AMAP_SERVER_KEY`：启用坐标转换与 POI 搜索 2.0（周边搜索），限制服务器出站 IP、服务范围及配额。Android 另用 Android 平台 / 3D 地图 SDK Key `AMAP_MAP_KEY`。
- 保留百度**服务端 Web 服务类型 AK**：启用地点检索 3.0 和坐标转换；按百度控制台要求配置服务器出站 IP、服务范围及配额。排名和热度字段须用实际账号权限验证。
- Android SDK 类型 AK 只用于手机底图，绑定包名与 APK 签名；不用于本容器。服务端 AK 不进入 APK。

密码、AK、SSH 私钥通过安全挂载或部署 Secret 提供，不在聊天、Git、命令行参数及日志中明文填写。Docker 登录使用 `--password-stdin`，保留现有 Docker 认证配置；远程拉取凭据使用最小权限。不要关闭 SSH 主机校验或 TLS 校验。

## 构建与推送

仓库根目录构建，平台必须匹配远程服务器；下面以 amd64 为例：

```sh
# IMAGE 使用实际 ACR 完整地址；TAG 建议版本号加源代码提交。
# 先用安全凭据完成 docker login --password-stdin，不要将密码写在命令里。
docker build --platform linux/amd64 -t "$IMAGE:$TAG" server
docker push "$IMAGE:$TAG"
```

记录 ACR 返回的不可变 `sha256` digest。不能把 `readyz` 或成功推送等同于真实地图平台调用已通过。本地开发云代理若需要附加 CA，应通过临时 Dockerfile 的 BuildKit secret 提供，不写进镜像、不禁用校验。

## 服务器运行

在已核验的服务器上选定应用目录，复制本目录 `compose.yaml` 和非敏感 `release.env`。将真实服务端 AK 写入**目录之外**权限为 600 的运行环境文件：

```properties
AMAP_SERVER_KEY=通过安全方式填写高德Web服务Key
# 需要保留百度在线切换时另填
BAIDU_SERVER_AK=通过安全方式填写百度服务端AK
PORT=3000
HOST=0.0.0.0
```

运行文件位置填入 `API_ENV_FILE`；`API_IMAGE` 填 ACR 的完整镜像地址及 `@sha256:...`。默认只绑定服务器 loopback，供现有 HTTPS 反向代理访问。不要盲目绑定公网或替换服务器现有的 80/443 服务。

```sh
cd /your/application/directory
docker compose --env-file release.env config --quiet
docker compose --env-file release.env pull
docker compose --env-file release.env up -d --wait --wait-timeout 120
```

API 地址在 Android 构建时通过 `API_BASE_URL` 注入。公网入口需要有效 HTTPS 证书；反向代理应禁用请求体和定位参数日志，配置连接超时大于服务端的二十秒查询预算。应用默认不信任伪造的转发 IP；代理部署需在可信网关做访问限流，不能只依赖被代理后共享地址的进程内限流。

## 验收与回滚

1. 验证运行中的镜像 digest、架构、容器 health 与反向代理入口，不输出容器完整环境配置。
2. `/healthz` 应返回 200；`/readyz` 默认检查高德；`/readyz?provider=baidu` 检查百度，只证明对应 Key 已填写，不证明其有效。
3. 最小功能验收调用一次 `POST /api/v2/places/nearby`（见主文档的杭州样例）；检查状态、坐标类型、一公里过滤和指标来源。未返回指标时保持 null，不以模拟数据冒充。
4. 检查从外部访问的 HTTPS 证书和 API 错误响应；记录版本、digest 与验收结果。
5. 更新前记录旧镜像 digest 和运行配置，并保留旧镜像。失败时将 `API_IMAGE` 恢复为旧 digest，再运行 `up -d --wait`；不使用 `down -v`、不删除其他容器，不清理旧版本镜像。

在仓库/服务器目标或凭据尚未提供时，只能完成镜像准备与本地测试，不能宣称已推送 ACR 或已远程发布。
