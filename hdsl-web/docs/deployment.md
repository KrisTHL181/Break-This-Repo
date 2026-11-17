# HDSL-web 部署细节

面向「已经跑起来，准备接到公网或长期运行」的场景。快速开始与配置字段见 [../README.md](../README.md)。

- [1. 再套一层反向代理（nginx / Caddy）](#1-再套一层反向代理nginx--caddy)
- [2. TLS 证书轮换](#2-tls-证书轮换)
- [3. 备份与恢复](#3-备份与恢复)
- [4. 多架构构建（buildx）](#4-多架构构建buildx)
- [5. 升级与回滚](#5-升级与回滚)
- [6. 运行期下载源（面板设置）](#6-运行期下载源面板设置)
- [7. 自签证书与容器健康检查（`Invalid SNI`）](#7-自签证书与容器健康检查invalid-sni)
- [8. 宿主 inotify 配额（多实例）](#8-宿主-inotify-配额多实例)
- [9. 用 IP 直连（没有域名、没有反代）](#9-用-ip-直连没有域名没有反代)

---

## 1. 再套一层反向代理（nginx / Caddy）

HDSL-web 自带边缘代理、自带证书，可以不用外层代理直接对公网。
但如果你已经有一台 nginx/Caddy（共享 443、统一证书、统一日志），把它放在前面也完全支持。

### 唯一的硬约束：`Host` 必须原样透传

dsh 的浏览器信任栅栏会比较 `Origin` 与 `Host`（以及 `--trusted-host` 白名单，**无通配符**）。
外层代理如果把 `Host` 改写成 `127.0.0.1:3080`，请求会直接被 dsh 以 **403** 拒绝。
`proxy_pass http://127.0.0.1:3080;` 在 nginx 里**默认就会改写 Host**（改成 `$proxy_host`），
所以必须显式写出来。

同时把反代后的公网地址写进 `server.yaml`：

```yaml
public_base_url: https://dsh.example.com
```

HDSL-web 用它给自签证书加 SAN，并在启动 dsh 实例时传 `--trusted-host dsh.example.com`。

### 先定一件事：谁终结 TLS

两条路都支持，但**只能选一条**。混着来就是下面这类事故的根源。

| | A. 外层代理终结（推荐：已有 nginx/Caddy、共享 443） | B. 面板自己终结 |
|---|---|---|
| 证书在谁手里 | nginx/Caddy（Let's Encrypt 等） | HDSL-web（`server.yaml` 的 `https.*`，或面板「设置 → HTTPS 证书」上传 / 生成自签） |
| 面板的 `https.enabled` | **保持 `false`** | `true` |
| `https.trust_forwarded_proto` | **`true`**（否则面板以为自己在 HTTP 上） | 不需要 |
| `proxy_pass` 指向 | `http://127.0.0.1:3080` | `https://127.0.0.1:3080` + `proxy_ssl_verify off;`（自签/内网证书） |
| 面板的 `https.redirect_http` | **必须关闭**（80 归 nginx，别抢） | 不需要（跳转由 nginx 做） |

三条铁律，占一条就可能出现「明明配了 HTTPS，还是走 HTTP」：

1. **只让一方终结 TLS**。nginx 已经 `listen 443 ssl` 了，面板就不要再开 `https.enabled`；
   反过来面板开了 HTTPS，`proxy_pass` 就必须写 `https://`，不能还写 `http://`。
2. **`proxy_pass` 的协议和端口必须等于面板真实监听的**。默认是 `127.0.0.1:3080`——写成
   `http://127.0.0.1:80` 是常见错误：80 通常是 nginx 自己的明文入口，请求要么打到别的站点、
   要么循环回自己，要么 502。面板换过 `HDSL_PORT`/`port` 时这里也要跟着改。
3. **子路径挂载（`/hdsl/`）必须剥前缀**：`proxy_pass http://127.0.0.1:3080/;` 结尾那个
   斜杠就是"剥掉 location 前缀"。漏了它，面板收到的是 `/hdsl/api/health`，直接 404。

### A. nginx 终结 TLS（面板纯 HTTP）

面板侧（`<HDSL_DATA>/server.yaml`，或环境变量）：

```yaml
https:
  enabled: false              # 证书在 nginx，面板不碰
  trust_forwarded_proto: true # 让面板认 X-Forwarded-Proto: https
```

nginx，**根路径挂载**：

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 80;
    server_name dsh.example.com;
    return 308 https://$host$request_uri;      # 80 只做跳转
}

server {
    listen 443 ssl;
    http2 on;
    server_name dsh.example.com;

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    client_max_body_size 210m;                 # 整合包上传上限 200 MiB

    location / {
        proxy_pass http://127.0.0.1:3080;      # 面板的真实地址：不是 :80，也不是 https://
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;      # 必须原样透传（dsh 的信任栅栏会比对 Origin/Host）
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        proxy_buffering off;                   # SSE 与流式输出不要缓冲
    }
}
```

nginx，**子路径挂载 `/hdsl/`**（面板侧设 `HDSL_BASE_PATH=/hdsl`）：

```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name dsh.example.com;
    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;
    client_max_body_size 210m;

    location /hdsl/ {
        proxy_pass http://127.0.0.1:3080/;     # 结尾斜杠 = 剥掉 /hdsl 前缀
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_read_timeout 3600s;
        proxy_buffering off;
    }
}
```

子路径形态下面板自己会带着 `/hdsl` 生成链接、静态资源、`/ws` 和 `/ws/terminal/*`，
nginx 只需要**剥前缀 + 透传升级头**，不需要额外 rewrite。`/i/<id>/` 实例页也在同一个
location 下，同样自动适用。

### B. 面板自己终结 TLS（自签或上传证书）

面板侧：

```yaml
https:
  enabled: true
  # 方式 A：上传证书（面板「设置 → HTTPS 证书」会回写这段）
  cert_file: certs/fullchain.pem
  key_file:  certs/privkey.pem
  # 方式 B：不配证书，首启自动生成自签（SAN 已含 public_base_url 与所有网卡地址）
  redirect_http: false        # 80 交给 nginx，别让面板也去监听 80
```

nginx 到面板这一段也要说 TLS（`proxy_ssl_verify off` 是因为上游是自签/内网证书，
身份由内网保证）：

```nginx
location /hdsl/ {
    proxy_pass https://127.0.0.1:3080/;   # 注意：https，不是 http；端口仍是 3080
    proxy_ssl_verify off;
    proxy_http_version 1.1;
    proxy_set_header Host $http_host;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_read_timeout 3600s;
    proxy_buffering off;
}
```

这种拓扑下 `trust_forwarded_proto` 不需要开：面板自己就在讲 TLS。也不要再在面板里
「生成自签名证书」——如果开了 `trust_forwarded_proto` 又想生成自签，后端会用 409
`tls_terminated_upstream` 拒绝，免得把「代理 → 面板」这段改成 TLS 之后反代连不上。

### Caddy（更少的坑：默认透传 Host）

外层终结 TLS：

```caddyfile
dsh.example.com {
    encode zstd gzip
    reverse_proxy 127.0.0.1:3080 {
        header_up Host {host}
        flush_interval -1          # SSE 立即刷新
    }
}
```

Caddy 自动签发/续期证书，面板侧按 A 配置（`https.enabled: false` +
`trust_forwarded_proto: true`）。子路径挂载用 `handle_path /hdsl/*`（`handle_path` 会剥前缀）：

```caddyfile
dsh.example.com {
    handle_path /hdsl/* {
        reverse_proxy 127.0.0.1:3080
    }
}
```

### 排错对照表

| 症状 | 多半是 |
|---|---|
| `/hdsl/api/health` 直接 404 | 反代没剥前缀（`proxy_pass` 少了结尾斜杠），或面板没设 `HDSL_BASE_PATH` |
| 浏览器打开是 `http://`，证书没生效 | nginx 的 80 段没有 `return 308 https://…`；或者 `proxy_pass` 写成了 `http://127.0.0.1:80` 打到了明文入口 |
| `curl https://…` 报 502 / 连不上 | `proxy_pass` 的协议或端口和面板不一致：面板开了 HTTPS 但写的是 `http://`，或端口不是 3080 |
| 面板自己的自签证书在浏览器里报错，同时 nginx 也有证书 | 两个 HTTPS 叠着用了：要么让 nginx 终结（面板关 HTTPS），要么让面板终结（nginx 用 `https://` + `proxy_ssl_verify off`） |
| 页面能用，但状态不刷新、终端连不上 | 没透传 `Upgrade`/`Connection` |
| 终端与开发者模式报 403 `insecure_transport` | 反代终结了 TLS 但没发 `X-Forwarded-Proto`，或面板没开 `trust_forwarded_proto` |
| 实例网页突然要开发者密码 | 面板开了开发者守卫（`developer_guard.scope`），这是预期；要么在开发者页提权，要么把作用域调回 `off` |
| 实例网页 / 终端全部 503 `emergency_stop` | 有人按了紧急停机；冷却 1 小时或重启面板进程/容器，之后在开发者页解除 |
| 登录 A 面板之后 B 面板掉线 | 同一个域名下挂了两个面板且用了旧版 Cookie（`Path=/` 互相覆盖）；新版按挂载点收敛，重新登录一次即可 |
| `Set-Cookie` 里是 `Path=/hdsl` | 正常：会话 Cookie 跟着挂载点走，两个面板互不覆盖 |

### 验收命令

```bash
# 1) 面板活着，且经过反代可达（子路径时要带前缀）
curl -sS https://dsh.example.com/hdsl/api/health

# 2) 80 是否只做跳转（期望 308 / 301，Location 是 https）
curl -sSI http://dsh.example.com/hdsl/api/health | head -3

# 3) 登录后的 Cookie 作用域（期望 Path=/hdsl）
curl -sS -D- -o /dev/null https://dsh.example.com/hdsl/api/auth/login   -H 'Content-Type: application/json'   -d '{"username":"admin","password":"<口令>"}' | grep -i '^set-cookie'

# 4) WebSocket 能不能升级（没有会话 cookie 时会被拒，但能看出路由到了面板而不是别的站点）
curl -sS -i -N https://dsh.example.com/hdsl/ws   -H 'Connection: Upgrade' -H 'Upgrade: websocket'   -H 'Sec-WebSocket-Version: 13'   -H 'Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==' | head -1
```

### 端口与可达性

- 面板：容器内 `3080`，HTTPS 开启时同端口直接说 TLS（`redirect_same_port` 默认开：同一端口
  收到明文请求会 308 跳到 https）。
- dsh 实例：容器内回环 `3081–4081`，**只监听回环、不要发布**，一切经 `/i/<id>/` 走 3080。
- MC 服务端：**默认一个游戏端口都不发布**。它说的是二进制 TCP，反代帮不上忙，要玩就得把
  宿主端口映射进来，而且 **容器内端口必须等于该实例的 `server-port`**（面板分配范围见
  `mc.port_start` / `mc.port_end`，默认 `25565–25665`）。多实例就是多行。三种开法：
  - `docker-install.sh` 交互时问一次（回车 = 不开），或 `--mc-ports 25565-25575`；
  - compose 里按实例加 `- "25565:25565"`（`docker-compose.override.yml` 是留给这种本机差异的）；
  - 已经起好的容器：`docker run` 那行改不了，只能 `./docker-install.sh --recreate` 重来。
- `https.redirect_http: true` 会让面板额外监听 `redirect_port`（默认 80）做 308→HTTPS 跳转；
  **外层代理已经占着 80 时不要开它**，否则两个进程抢同一个端口。跳转交给 nginx 更省事。


### 容器侧建议

```yaml
services:
  hdsl-web:
    # 只发布到宿主机回环，公网入口交给外层代理
    ports:
      - "127.0.0.1:3080:3080"
    environment:
      HDSL_BIND_HOST: "0.0.0.0"   # 容器内仍需 0.0.0.0 才能被宿主 127.0.0.1 映射访问
    volumes:
      # MC 服务端的存档与服务端程序（docs/minecraft.md §7）。不挂这个，
      # 重建容器就等于删世界——世界是这里唯一不可再生的数据。
      - hdsl-mc:/home/hdsl/mc
```

### 源码部署：`docker-install.sh`

仓库根目录的向导脚本把上面这些事问一遍（数据目录、工作区、MC 数据、**MC 端口**、面板端口、
绑定地址、镜像源），然后在原地 `docker build` + `docker run`。它不依赖 compose，适合 NAS 这类
只装了 docker 的机器。五条容易踩的规则由它代劳：

| 规则 | 为什么 |
|---|---|
| 宿主目录的属主由容器入口校正 | 入口每次启动把 `/data`、`/home/hdsl`（含 `workspace`、`mc` 两个挂载点）交给 uid 1000，再把 `auth/`、`certs/`、`logs/`、`server.yaml` 收回 root；脚本自己不再做整卷 chown（那会把秘密交回去） |
| 端口内外同号 | `HDSL_PORT` 同时决定端口映射、容器内监听和 healthcheck，只改宿主侧会得到「running 但一直 unhealthy」 |
| 工作区只能挂到 `/home/hdsl/workspace` | 挂 `/home/hdsl` 整层会盖掉 entrypoint 每次启动重写的 pnpm 配置；不挂则重建容器时工作区里的文件会丢 |
| MC 默认挂到 `/home/hdsl/mc`（`volume:hdsl-mc`） | 世界存档与 `server.jar` 在那儿；不挂 = 存档随容器一起没。`--mc /srv/mc` 换成宿主目录，`--mc none` 明确不挂 |
| **MC 端口默认不发布** | 开服是暴露面。要开就在配置里填 `25565`（或 `25565-25575`），或者 `--mc-ports 25565`；宿主与容器同号，实例的 `server-port` 要跟它一致 |

同名容器已存在时它先问「重启 / 重建容器（不重编）/ 重新构建镜像」，不会静默覆盖；
`--yes` 加参数即可非交互执行，`--dry-run` 只打印计划。

**先体检**：`./docker-install.sh --doctor` 不改任何东西，只把 docker / 守护进程 / buildx /
compose / 磁盘 / 面板端口 / MC 端口逐条列出来，缺什么就给这台机器上能用的那条命令。
「照着提示做完还是跑不通」多半是提示缺了一步——Ubuntu 的 `docker.io` 不带 buildx 就是
典型，所以提示是按本机包管理器现算的。

> 仓库里这个脚本的权限位曾经记成 `100644`，新克隆下来 `./docker-install.sh` 会直接
> `Permission denied`。如果你的克隆早于这一版，先 `chmod +x docker-install.sh`。

---

## 2. TLS 证书轮换

证书相关的三个位置：

| 文件 | 来源 |
|---|---|
| `/data/certs/self-signed.pem`、`self-signed-key.pem` | 首启自动生成（`https.enabled: true` 且未配证书） |
| `/data/certs/uploaded.pem`、`uploaded.key` | 面板上传 PEM |
| `/data/certs/uploaded.p12` | 面板上传 PKCS#12（**加载优先级最高**，会盖过 PEM） |

### 面板上传：热切换，不重启

「设置 → TLS 证书」上传 PEM（证书链 + 私钥）或 `.p12`（+ 口令）。服务器会：

1. 把选择写回 `/data/server.yaml`（`https.enabled: true` + 对应文件键，并清掉另一种类型的键，避免旧 `pkcs12_file` 盖住新 PEM）；
2. 已经在 HTTPS → `SslContextFactory.reload()`，**新证书立即生效，连接不断**；
3. 还是 HTTP → 运行时把明文连接器换成 TLS 连接器（同主机同端口）；
4. 切换失败 → 回滚成明文，响应带 `restartRequired: true`；`server.yaml` 已经写好，**重启容器即生效**。

### Let's Encrypt / 自动续期

HDSL-web 不做 ACME，续期要么交给外层代理（推荐：Caddy 自动续期，内层不配证书），
要么把续期产物送进容器再重启：

```bash
# 宿主机上 certbot 续期后
docker cp /etc/letsencrypt/live/dsh.example.com/fullchain.pem hdsl-web:/data/certs/uploaded.pem
docker cp /etc/letsencrypt/live/dsh.example.com/privkey.pem  hdsl-web:/data/certs/uploaded.key
# 让 HDSL-web 读取新文件：走面板上传（热重载）或直接重启
docker compose restart hdsl-web
```

> `docker cp` 写入的是 root 属主的文件；面板现在以 root 运行，读取没问题。
> **不要**把 `/data/certs` chown 给 1000——那会把面板的私钥交给 agent（入口下次启动也会
> 把它改回 root 0700）。走面板上传则完全不用操心权限。

### 手工替换 + 重启

也可以直接把新 PEM 覆盖到 `/data/certs/`，或改 `server.yaml` 指到别处（绝对路径/相对 `HDSL_DATA` 都行），
然后 `docker compose restart hdsl-web`。重启会重新加载并校验证书；证书与私钥不匹配、文件缺失都会**启动失败**并指出原因。

---

## 3. 备份与恢复

### 备份：停容器，拷 `/data`

一致性来自「先把进程停下」：运行中的 dsh 实例会写 `home/`、pnpm 会写 store 与 `node_modules`，
热拷贝可能拿到半截状态。

```bash
# 1) 停
docker compose stop hdsl-web

# 2) 打包卷（用镜像自带的 tar，避免权限/uid 漂移）
#    -u 0:0：卷里的 auth/ 等目录是 0700，root 才读得全；产物属主是 root，必要时自行 chown
docker run --rm -u 0:0 \
  -v hdsl-data:/data \
  -v "$PWD":/backup \
  --entrypoint tar hdsl-web:latest \
  czf "/backup/hdsl-data-$(date +%F-%H%M).tgz" -C /data .

# 3) 起回来
docker compose start hdsl-web
```

只要 `/data` 就够了：`server.yaml`、证书、账号哈希、全部实例、pnpm store、corepack 缓存都在里面。
`/data/pnpm-store` 与 `/data/corepack` 可以还原后重建（重新下载），嫌大的话备份时排除它们。

聊天也在这条边界里：`/data/chat/` 是 `chat.db`（SQLite）与按 sha256 去重的附件，`/data/backup/`
是面板自己做的聊天备份（`chat-backup-<时间>/`，含 db 快照、附件与 manifest）。两者都随
`/data` 一起拷走；如果聊天设置里把备份目录改到了别处，那个目录要单独备份。

> 聊天数据里有供应商 API key 与全部会话，按敏感数据对待：目录是 0700，备份产物的属主要
> 自己确认。只想恢复聊天时，可以停面板 → 用备份里的 `chat.db` + `files/` 替换
> `/data/chat/` → 启动（schema 版本向前迁移，不做降级）。

### 恢复：空卷 + 解包 + 修属主

```bash
docker compose stop hdsl-web

# 用一个新的空卷（或先清空现有卷）
docker volume create hdsl-data-restore

docker run --rm -u 0:0 \
  -v hdsl-data-restore:/data \
  -v "$PWD":/backup:ro \
  --entrypoint tar hdsl-web:latest \
  xzf /backup/hdsl-data-2026-10-03-1200.tgz -C /data

# 解包是以 root 做的，把属主对齐镜像里的运行用户 hdsl(1000)
docker run --rm -u 0:0 -v hdsl-data-restore:/data --entrypoint chown hdsl-web:latest -R 1000:1000 /data
```

然后在 `docker-compose.yml` 里把卷指到 `hdsl-data-restore`（或者在原卷上恢复）再启动。
恢复后口令、证书、实例、会话记录原样回来，不需要重新登录配置。

### 迁移到另一台机器

同上：目标机器 `docker compose up -d` 一次（创建卷）→ 停 → 解包覆盖 → 起。
`/data` 的内容基本与 CPU 架构无关（jar 和实例数据都是），唯一例外是 `/data/hdsl/runtimes/` 里
**自下载**的 Node 二进制——镜像已内置 Node，一般没有这层数据；真跨架构迁移时删掉它、让新容器重新下载即可。
镜像本身用 [buildx](#4-多架构构建buildx) 为目标架构构建。

---

## 4. 多架构构建（buildx）

Dockerfile 对 `amd64` 与 `arm64` 都是自适应的：Node 版本按 `uname -m` 选官方 tarball，
校验 `SHASUMS256.txt`；三段里前两段钉在构建机架构（`node:22-bookworm-slim`、`eclipse-temurin:21-jdk`），
第三段的底座由 `HDSL_BASE` 决定（默认 `debian:trixie-slim`，也可换 Fedora / openSUSE，见 §4.1）。CI（`.github/workflows/docker.yml`）发布的就是
`linux/amd64` + `linux/arm64` 双架构 manifest；构建阶段钉在 `$BUILDPLATFORM`，
只有 runtime 阶段在目标架构下执行。下面的命令用于本地复现：

```bash
# 一次性准备 builder
docker buildx create --name hdsl-builder --use --bootstrap

# 构建并推送双架构（--push 才保留多架构 manifest；--load 一次只能装一个平台）
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --build-arg NPM_REGISTRY=https://registry.npmjs.org/ \
  --build-arg RELEASE_VERSION=0.1.0 \
  -t yourname/hdsl-web:0.1.0 \
  -t yourname/hdsl-web:latest \
  --push .
```

要点：

- 非本机架构走 QEMU 模拟，Gradle 与 Vite 都会明显变慢；急的话先在本机架构上验证，再跑双架构。
- **两个架构都必须能构建**：`nodejs.org` 与 npm registry 都要可达；`corepack prepare` 会下载 pnpm。
- `--build-arg RELEASE_VERSION=…` 才会让 jar 带上发布版本号（缺省 `0.1.0-dev`）；它同时写进 jar 的 manifest，
  面板显示的版本来自这里。
- 本地构建出来的镜像**不是发布产物**：`RELEASE_VERSION` 只决定 jar 里的版本字符串，不保证
  jar 与 GitHub Release 的资产一致（拿旧源码配新版本号重建很常见）。核对时比
  `docker exec <容器> sha256sum /app/hdsl-web.jar` 与 Release 里的 `SHA256SUMS.txt`；
  排障或升级优先用 `ghcr.io/nas200-timmy/hdsl-web:<版本>`。
- 想省下 SPA 阶段的重复劳动可以用 registry cache：`--cache-from/--cache-to type=registry,ref=…`。
- 架构用 `uname -m` 判断，不再问 `dpkg`：换底座时同一条指令照样成立。

### 4.1 换运行时底座（Debian / Fedora / openSUSE）

同一颗 jar 在三个底座上都验过（见 `report-2026-10-31-base-compat.md`）：api-smoke 39/39，
实例创建 → 安装 → 启动 → 反代 200 → 停止 → 删除全通，Fedora 上还验了真 PTY。
**只有一个 Dockerfile**：runtime 阶段按 `/etc/os-release` 分派包管理器，其余阶段与底座无关。

```bash
docker build --build-arg HDSL_BASE=fedora:44 \
             --build-arg HDSL_ENCODERS=optional \
             -t hdsl-web:fedora .

./docker-install.sh --base fedora     # 或者用向导，debian 是默认
```

三个**已知差异**，都不是 bug：

| 差异 | 处置 |
|---|---|
| `tini` 路径：Debian/Fedora 在 `/usr/bin`，openSUSE 在 `/usr/sbin` | 镜像里统一软链到 `/usr/local/bin/tini`，`ENTRYPOINT` 只认那个 |
| ffmpeg 编码器：Debian 的 `libavcodec-extra` 带 x264/x265；Fedora 的 `ffmpeg-free`、openSUSE 的 `ffmpeg-7` 只有专利-free 集合（完整版在 RPM Fusion / Packman，镜像不引第三方仓库） | 非 Debian 底座传 `HDSL_ENCODERS=optional`，构建期打印缺哪几个而不是失败 |
| JDK：Fedora 44 没有 OpenJDK 21 | 退到 25（实测跑 0.6.40 无问题）；要钉 21 就用 Fedora 42 或更早 |

体积差距明显：Debian 2.28 GB、Fedora 2.38 GB、openSUSE 3.59 GB（`-devel`/`rust`/`go` 的依赖闭合更大）。

### 4.2 Docker Hub 不可达时（构建拉不到底座）

构建要用三个**底座镜像**：`node:22-bookworm-slim`（前端）、`eclipse-temurin:21-jdk`（编译
jar）、以及运行时底座（默认 `debian:trixie-slim`）。Docker Hub 被墙、被限流、或被出口策略挡掉
时，构建第一步就会失败：

```
ERROR: failed to solve: node:22-bookworm-slim: failed to resolve source metadata for
docker.io/library/node:22-bookworm-slim: … dial tcp 208.77.47.172:443: connect: connection refused
```

**默认已经避开了这条路**：向导在「国内镜像源」这一项上默认选 `docker.m.daocloud.io`，三个底座
都从那里拉（镜像站会代理到 Docker Hub，三个底座的多架构 manifest 实测都在）。向导会把这一项
弹成一个上下键菜单，一次把 npm、Node、底座三件事一起定下来：

```
镜像源（↑/↓ 选择，回车确认）
❯ 国内镜像（推荐）｜npm→npmmirror，Node→腾讯，底座→docker.m.daocloud.io
  国内镜像（备选）｜npm→npmmirror，Node→腾讯，底座→docker.1ms.run
  官方源｜npm→npmjs.org，Node→nodejs.org，底座→Docker Hub
```

其它入口：

```bash
./docker-install.sh                          # 菜单：国内（推荐/备选）/ 官方
./docker-install.sh --no-mirror              # 官方源：npmjs / nodejs.org / Docker Hub
./docker-install.sh --base-registry docker.1ms.run   # 只换底座仓库，npm/Node 照旧
./docker-install.sh --pull 0.6.54            # 不构建，直接拉已发布的镜像

# 手工构建时对应三个 ARG
docker build --build-arg HDSL_WEB_BASE=<仓库>/library/node:22-bookworm-slim \
             --build-arg HDSL_BUILD_BASE=<仓库>/library/eclipse-temurin:21-jdk \
             --build-arg HDSL_BASE=<仓库>/library/debian:trixie-slim .
```

底座仓库会被记进状态文件（`.docker-install.state` 的 `INSTALL_BASE_REGISTRY`），下次重跑不用再
给；`--no-mirror`（官方源）会清掉它。`--pull` 拉的是 `ghcr.io/nas200-timmy/hdsl-web:<版本>`
（多架构 manifest，amd64/arm64 自动挑对），拉下来后按本地镜像名打标签，所以 `--no-build`、
`--recreate` 等后续操作照旧认它。也可以在 `/etc/docker/daemon.json` 配 `registry-mirrors`——
那是全局设置，配一次所有 `docker pull` 都受益（包括构建时的底座）。

### 4.2.1 构建慢：apt 是另一半（`--apt-mirror`）

换掉底座仓库之后**构建仍可能很慢**，因为容器里的 `apt` 还是直连 `deb.debian.org`：
`apt-get update` 要拉 ~10 MB 索引，装包再拉几百 MB（工具链 + 两个 JDK），这一段和底座仓库
是两个完全不同的来源。国内网络下的典型表现是构建停在

```
#28 2.799 Get:4 http://deb.debian.org/debian trixie/main amd64 Packages [9678 kB]
```

现在两项都做了：

- **`--apt-mirror <scheme://host>`**（`--mirror` 在国内模式下自动带上 TUNA，
  `--no-mirror` 清掉，也可单独 `--no-apt-mirror`）：构建期把 Debian/Ubuntu 的 sources 主机
  换成它，路径不变（`…/debian`、`…/debian-security` 都对得上）。只影响 Debian/Ubuntu 底座。
- **apt 缓存挂载**：`/var/cache/apt` 与 `/var/lib/apt/lists` 是 BuildKit cache mount，
  索引和 `.deb` 留在构建器里、不进镜像。改一行 Dockerfile 不会再重新下载整个工具链。

实测口径（一台国内网络机器，`dists/trixie/main/binary-amd64/Packages.xz`）：

```
deb.debian.org   20 s 只下到 2.86 MB（142 KB/s）
TUNA              1.09 s 下完 9.68 MB（8.9 MB/s）   ← 62 倍
```

热缓存之后重复构建这一步直接 `CACHED`。

```bash
./docker-install.sh --mirror …                      # apt + npm + Node + 底座，一次到位
./docker-install.sh --apt-mirror http://mirrors.ustc.edu.cn …
docker build --build-arg APT_MIRROR=http://mirrors.tuna.tsinghua.edu.cn .
```

中断也不白等：已建好的层与 apt 缓存都留着，重跑从断点继续。

### 4.3 构建还需要 buildx

`Dockerfile` 里用了 `RUN --mount=type=cache`（BuildKit 语法），而 **Docker 23 起 `docker build`
本身就是 buildx**——插件不在，命令直接报：

```
ERROR: BuildKit is enabled but the buildx component is missing or broken.
```

发行版的 `docker.io` 包不一定把插件带上（Ubuntu 24.04 上 `docker-buildx` 是独立包；更老的
Ubuntu 连这个包名都没有）。按本机的包管理器装：

```bash
sudo apt-get install -y docker-buildx          # Ubuntu 23.10+ / Debian 12+
sudo dnf install -y docker-buildx-plugin       # Fedora / RHEL（Docker 官方源）
sudo zypper install -y docker-buildx           # openSUSE
sudo pacman -S --needed docker-buildx          # Arch
sudo apk add docker-cli-buildx                 # Alpine
```

老的 Ubuntu / Debian 没有 `docker-buildx` 这个包时，用官方源里的 `docker-buildx-plugin`
（先按 <https://docs.docker.com/engine/install/> 加源），或者把插件二进制直接放进用户目录——
任何发行版通用，不需要包管理器：

```bash
docker create --name buildx-tmp docker/buildx-bin:latest \
  && docker cp buildx-tmp:/buildx ~/.docker/cli-plugins/docker-buildx \
  && docker rm buildx-tmp && chmod +x ~/.docker/cli-plugins/docker-buildx
docker buildx version
```

不想装 buildx 也有一条路：`--pull <版本>` 只做 `docker pull` / `docker tag` / `docker run`，
用不到构建，也就用不到 buildx。

**先体检再动手**：`./docker-install.sh --doctor` 会把 docker、守护进程、buildx、compose、磁盘、
面板端口、MC 端口逐条过一遍，缺什么直接给这台机器上能用的那条命令（只读，不装任何东西）。
`--install-docker` 会在装 docker 时顺带补 buildx。

---

## 5. 升级与回滚

```bash
# 升级：重新构建镜像 → 换容器 → 卷不变
docker compose build --pull
docker compose up -d

# 指定版本（build arg）
docker compose build --build-arg RELEASE_VERSION=0.2.0
```

- 数据全部在 `/data`，换容器（甚至换镜像）不会丢；`HDSL_ADMIN_PASSWORD` 只在**首次**创建账号时有用，
  升级时传不传都行。
- 升级前建议按上面第 3 节做一次备份。
- 回滚就是把旧 tag 的镜像重新 `up -d`；如果新版本写过 `server.yaml` 的新字段，旧版本会给出
  「unknown key … ignored」警告而不影响启动。

### 5.1 容器升级：面板从 `hdsl` 用户切到 root（v0.6.44 起）

v0.6.44 起，镜像不再 `USER hdsl`，面板以 root 跑、每个 agent 进程用 `setpriv` 降到 uid 1000。
目的是把 `auth/`、`certs/`、`logs/`、`server.yaml` 归 root，让 agent 改不动面板的凭据。
**卷不用迁，属主在容器启动时自动校正**；但有四件事要知道：

1. **首次启动会做一次 chown**：入口把 `/data` 与 `/home/hdsl` 交给 1000，再把
   `auth/`、`certs/`、`logs/`、`server.yaml` 收回 root。只在属主不对时递归，之后每次启动
   只做 `stat`，不会遍历大卷。老卷升级后第一次启动会慢几秒（目录大的话更久），属正常。
2. **`docker exec` 默认用户变了**：以前默认是 `hdsl`，现在是 root。脚本里凡是靠"默认用户"
   进容器的都要显式写 `-u 1000`（例如 `docker exec -u 1000 … sh /tmp/verify-privilege-split.sh`）。
3. **`--user 1000` 覆盖仍然可用**，但降权就失效了（面板自己就是 1000），行为退回"裸部署"那一套：
   agent 与面板同 uid，秘密文件彼此可写。要这套隔离就别覆盖用户。
4. **验收**：脚本不在镜像里（它在仓库的 `scripts/`），先拷进去再跑：
   `docker cp scripts/verify-privilege-split.sh <容器>:/tmp/ && \
    docker exec -u 1000 <容器> sh /tmp/verify-privilege-split.sh`。先建一个实例，
   否则"实例目录可写"那条会跳过。期望 `auth/`、`logs/` 读写都被拒，实例目录与 pnpm store 可写。

5. **默认工作区不再跟着 root 跑**：面板是 root 之后 `user.home` 是 `/root`，工作区若按它解析
   会落到 `/root/workspace`（0700，agent 进不去，终端起来 44 ms 就退出）。本版按 `HOME` 解析
   （镜像里是 `/home/hdsl`），与挂载点一致；`HDSL_WORKSPACE` 仍然可以覆盖。
6. **ACL 管理的存储要当心**：入口会自检"agent 能不能写 `auth/`"，能写就打 WARNING——那说明
   这台机器的文件系统不认 mode 位（QNAP trimacl 一类），隔离不成立。要么换存储，要么用
   `--build-arg HDSL_AGENT_UID=1001` 重建镜像，详见 `security.md §2.8`。

供应商 API key 不在这层保护里（agent 与 ACP 要用），见 `docs/design-notes.md §7.5.1`。

---

## 6. 运行期下载源（面板设置）

「设置 → 下载源」改的是**运行期**用哪个 npm registry，和构建镜像时的 `--build-arg NPM_REGISTRY` 是两回事：

| | 构建期 `NPM_REGISTRY`（build-arg） | 运行期「下载源」（面板设置） |
|---|---|---|
| 作用范围 | `docker build` 期间：SPA 阶段的 `npm ci`、corepack 下载 pnpm 本体 | 容器运行期间：dsh 实例安装/升级、插件安装、ZCode 构建 |
| 改完怎么生效 | 重新构建镜像（`docker compose build`） | 保存即生效，不用重启容器 |
| 存在哪 | 只在构建过程里，不进镜像 | `/data/hdsl/launcher-settings.json`（随 `/data` 一起备份） |

**优先级：面板设置 > 环境变量 `NPM_CONFIG_REGISTRY` > 官方默认 `https://registry.npmjs.org/`。**
想在面板之外钉死一个源，就把面板里的下载源设成「跟随部署环境（默认）」——那正是「清空设置、只用环境变量」的开关。

### 怎么改、什么时候生效

预设给的是几个公开镜像，也可以填自己的 URL：

| 预设 id | 地址 |
|---|---|
| `environment` | 跟随部署环境（默认；清空设置，回到环境变量 / 官方） |
| `npmjs` | `https://registry.npmjs.org`（官方） |
| `npmmirror` | `https://registry.npmmirror.com` |
| `ustc` | `https://npmreg.proxy.ustclug.org`（中科大） |
| `tencent` | `https://mirrors.cloud.tencent.com/npm`（腾讯云） |
| `huawei` | `https://repo.huaweicloud.com/repository/npm`（华为云） |

内网的 Nexus、Verdaccio 这类 `http://192.0.2.10:8081/repository/npm/` 是允许的——这一版要防的是**注入**，
不是强制 TLS，所以 `http` 只校验、不拒绝。保存前服务端会校验：只允许 `http`/`https`、必须能解析出主机名、
禁止用户口令 / 查询串 / 片段，以及空白、控制字符、`"` `'` `` ` `` `\` `#` `$` `;` `|` `&` `@`，长度 ≤ 200，
尾斜杠自动去掉；不合法直接 400 且**设置保持不变**。

保存后对**之后产生**的 pnpm/npm 子进程生效：新建或重装的实例、插件安装、面板内构建都会用新源；
已经装好的实例不会因此重装（要重装/升级才会走新源）。容器重启后设置仍在，入口脚本先生成
`config.yaml`、面板启动时再按设置重写一次。

### 它会重写 pnpm 的全局配置

pnpm 11 **不读环境变量**，registry 只来自它自己的全局配置 `~/.config/pnpm/config.yaml`
（容器内是 `/home/hdsl/.config/pnpm/config.yaml`，镜像里没有任何 `.npmrc`）。所以面板在**启动时**和**每次保存**都会
按生效值重写这份文件：逐行保留其它设置（`storeDir` 不会丢）、只重写 `registry` 一行，值经 YAML 引号、
写临时文件后原子替换、权限 0600；若本来没有 `storeDir`，还会按 `PNPM_STORE_DIR` 补一行。
同时面板还给子进程注入 `npm_config_registry` / `NPM_CONFIG_REGISTRY`（npm 系读它），
并在自己起的 pnpm 上显式加 `--registry=`（dsh 安装、ZCode 构建，日志里能看到实际用的源）。

**后果**：手工挂载或手改的 `config.yaml` 里的 `registry` 会被覆盖——即使面板里选的是「跟随部署环境（默认）」，
写进去的也是环境变量或官方默认，而不是你手写的那一行（`storeDir` 等其它行原样保留）。
想自己维护这个键，就得让这个文件别被面板碰：目前没有开关，只能改完文件后别在面板里保存下载源。
另外这份文件在容器 home 下、**不在 `/data` 卷里**：容器重建会回到入口脚本生成的那份，所以面板每次启动都会重写一次。

> corepack 下载 pnpm 本体走的是它自己的 `COREPACK_NPM_REGISTRY`（由入口脚本从 `NPM_CONFIG_REGISTRY` 派生）：
> 面板设置只写 pnpm 那份 config.yaml、不碰这个变量，所以完全离线的内网部署仍需用环境变量把 corepack 也指到位。

---

## 7. 自签证书与容器健康检查（`Invalid SNI`）

面板启用 HTTPS、而证书的主机名是 `dsh.example.com` 这类域名时，`curl https://127.0.0.1:3080/api/health`
这种「按地址访问」的请求不带 SNI；Jetty 默认的 SNI 主机校验会把它判成 `400 Invalid SNI`，
于是容器健康检查一直失败、`docker compose ps` 里永远 `unhealthy`（服务其实是好的）。

HDSL-web 已经把这项校验关掉——单证书面板，这个校验只会误伤本机探针：

```java
// web/server/HdslServer.java：两个 TLS 连接器共用的定制器
customizer.setSniHostCheck(false);   // sniRequired 保持默认 false
```

所以 compose 里的健康检查可以直接写 `https://127.0.0.1:${HDSL_PORT}/api/health`。
若你的部署里探针仍然失败，退一步也可以让探针带上证书里的主机名：

```bash
curl -fsSk --resolve dsh.example.com:3080:127.0.0.1 https://dsh.example.com:3080/api/health
```

排查时先手动跑一次上面的命令：返回 `{"status":"ok"}` 就说明服务没问题，是探针的 SNI 不对。

---

## 8. 宿主 inotify 配额（多实例）

**跑两个以上实例之前值得先看一眼这一节**：实例启动失败的一种常见原因在这里，而它的现象完全不指向这里。

### 它是什么

dsh 启动时会用文件监视器（chokidar）盯着自己的 `profiles/<profile>` 目录。内核为此设了一个上限
`fs.inotify.max_user_watches`（默认 65536），而这个数字**是按用户（uid）算的，容器之间不隔离**：
**实例进程**以 uid 1000 运行（面板自己是 root，配额另算），于是宿主上**任何同样以 uid 1000
运行的程序**——编辑器/IDE 服务、文件同步客户端、别的容器——所监视的每一个目录，都从这个数字里扣。一个递归监视整棵工作树的程序
就能把 65536 吃干。

### 配额用尽时的现象

都不像「配额不足」，所以容易查错方向：

- **实例起来就消失**：进程崩了，它的日志（面板实例页的「日志」标签）里是
  `Error: ENOSPC: System limit for number of file watchers reached, watch '…/profiles/web'`
  ——node 的 `fs.watch` 拿不到监视器就直接退出；
- **实例永远停在「启动中」**：进程活着、端口在监听，但启动项因为拿不到监视器而永不结束，
  于是**就绪行一直不打印**，面板拿不到地址，`/i/<id>/` 返回 502。

两种现象都跟「另一个实例先跑起来了」强相关，因为争夺的是同一个共享数字——谁先谁后决定谁被拒，
不是某个实例本身有问题（`instance`、`dsh-…` 只是运气不同的那一个）。

### 怎么看

面板「设置 → 环境体检」（也就是 `GET /api/doctor`）里的 **File watchers** 一行：
它用一次真实的注册探测给出「还剩多少」，低于阈值会标 `[SHORTAGE]` 并打印下面这条命令。
想看原始数字也可以直接问内核：

```bash
# 上限（所有 uid 1000 的进程共用的那个数字；面板自己是 root，不占这份）
cat /proc/sys/fs/inotify/max_user_watches
# 谁在占（容器里的进程要进各自的容器里数）
docker exec <容器> sh -c 'for p in /proc/[0-9]*; do n=0; for f in $p/fdinfo/*; do \
  [ -f "$f" ] && n=$((n+$(grep -c "^inotify wd:" "$f" 2>/dev/null))); done; \
  [ "$n" -gt 0 ] && echo "$n $(tr -d "\0" < $p/cmdline | cut -c1-60)"; done'
```

### 怎么修

在**宿主**上把上限调大（容器里改不动这个内核参数）：

```bash
echo 'fs.inotify.max_user_watches=524288' | sudo tee /etc/sysctl.d/99-inotify.conf
echo 'fs.inotify.max_user_instances=1024'  | sudo tee -a /etc/sysctl.d/99-inotify.conf
sudo sysctl --system
```

- 524288 是这类工具的常见建议值，够跑若干个实例；宿主上真有一棵几十万目录的树被递归监视时再往上调。
- 已经在监视的文件不受影响，**容器不用重启**；但已经崩掉或卡在「启动中」的实例要回面板重新启动。
- 不想动配额，还有两条路：让那个吃配额的程序少监视一点（多数编辑器/IDE 服务都有「排除监视目录」
  之类的设置，把 `node_modules`、构建产物这类大目录排除掉）；或者**给面板容器换一个独立的 uid**，
  让它有自己完整的一份配额。换 uid 不只是 compose 里加一行 `user:`——实测至少还有两处要可写：
  入口脚本要往 `$HOME`(`/home/hdsl`) 写 pnpm 配置，面板要在 `/data/auth` 下建账号文件，
  所以 `/data` 卷得先 `chown` 给新 uid，`$HOME` 也要 chown 或者干脆挂一个属于该 uid 的卷。

### 顺带一提：两个实例的 localStorage 是共用的

这一条与上面的配额无关，但也是「两个实例互相影响」的一个来源，值得记下来：面板把两个实例放在**同一个
origin** 下，而浏览器只按 origin（协议 + 域名 + 端口）隔离 `localStorage` / `sessionStorage`，
**完全不看路径**——所以两个实例的网页端会读写同一批键（dsh 自己的客户端状态、界面偏好等）。
面板目前只处理了 cookie 的 `Path`，没管 storage。目前没有证据表明它导致过实例起不来；
真出现「界面串台」时再往这个方向查。



---

## 9. 用 IP 直连（没有域名、没有反代）

这是最常见的部署形态——地址就是一串 IP，没有证书里的域名，也没有外层代理。两件事都要成立：

### ① 明文请求自动跳 HTTPS（`https.redirect_same_port`，默认开）

浏览器在地址栏里只看到 `192.168.1.6` 时会**按 http 发请求**；如果那个端口说的是 TLS，浏览器
只会甩一句 `ERR_INVALID_HTTP_RESPONSE`（明文撞上 TLS 监听），既没有页面也没有线索。打开这个
开关后，**同一个 TLS 端口也接明文**，一律回
`308 Location: https://<同主机>:<同端口><同路径>`，浏览器自动跳过去。

内容永远不会以明文发出：非 TLS 请求只会得到这个 308（由 `HdslServer.HttpsRedirectHandler`
统一处理，TLS 请求直接放行）。会话 Cookie 带 `Secure`（HTTPS 打开时由 `AuthFilter` 设置），
也不会在明文请求里被带上。

- 想要一个纯粹的、只认 TLS 的端口：`redirect_same_port: false`。此时明文撞上来仍会失败
  （这是那条报错的原样复现，测试 `RedirectTest.cleartextOnTheTlsPortIsRefusedWhenTheRedirectIsOff` 钉着它）。
- 想再开一个固定的明文端口（容器里另映射 80）：`redirect_http: true` + `redirect_port: 80`。

### ② 自签证书带上本机地址

生成的 SAN 含 `localhost`、`127.0.0.1`、**本机所有网卡地址**与主机名（外加 `public_base_url`
的 host），所以 `https://192.168.1.6:3080` 只会是"自签证书不受信任"这一条告警（点继续即可），
不会变成"证书主机名不匹配"。换了网段/VPN 之后重新生成即可：删掉 `<数据目录>/certs/` 下的自签对，
或从面板「设置 → TLS 证书」重新上传。

> 反向代理放在前面时（§1）另外注意：外层代理必须**原样透传 Host**，否则 dsh 的浏览器信任栅栏会以
> 403 拒掉实例页面——这与面板自己是否开 TLS 无关。实例的信任名单是启动时按当时的访问地址算的，
> 换域名/换地址之后重新启动一次实例即可。

---

## 10. 裸部署（systemd）

不用容器、直接 `java -jar` 的 Linux 部署通常交给 systemd。最小单元：

```ini
# /etc/systemd/system/hdsl-web.service
[Unit]
Description=HDSL-web
After=network-online.target

[Service]
Type=simple
User=hdsl
# systemd 的系统服务默认不注入 HOME。pnpm / node 的生命周期脚本会因此失败，而 pnpm 只报
# ERR_PNPM_EXECUTOR_LIFECYCLE_SCRIPT_FAILED（koffi 的 cnoke.cjs 直接抛 "Missing HOME"），
# 面板日志里只剩截断的堆栈尾部——很难查。面板自己已在 HOME 缺失时退回 JVM 的 user.home
# 兜底（DshCommand），这一行是双保险，也让其它手工命令拿到一致的 HOME。
Environment=HOME=/srv/hdsl
Environment=HDSL_DATA=/srv/hdsl/data
Environment=HDSL_PORT=3080
ExecStart=/usr/bin/java -Xmx1g -jar /srv/hdsl/hdsl-web.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

- `HOME` 与 `HDSL_DATA` 指向的目录要属于 `User=`（面板要在其中写账号、证书、实例与 pnpm store）。
- 参数多时用 `EnvironmentFile=/etc/hdsl-web.env`（一行一个 `KEY=value`）；`HDSL_ADMIN_PASSWORD`
  建议放那里并 `chmod 600`。
- 数据目录里的 `auth/` 是 0700，备份/迁移要用 root 读（见 §3）。
- 升级：换掉 jar 后 `systemctl restart hdsl-web`，`HDSL_DATA` 不动即可。
- 想直接监听 1024 以下的端口（例如 443）要给 `AmbientCapabilities=CAP_NET_BIND_SERVICE`，
  或者照 §1/§9 在前面放反代。
- `journalctl -u hdsl-web -f` 看服务日志；面板自己的日志同时在 `<HDSL_DATA>/logs/` 下。

### 10.0 身份模型：裸部署是单用户，没有隔离

> 威胁、误配清单与免责见 [`security.md`](security.md)。

面板和被它启动的每一个 agent（dsh、品牌工具、MC 服务端、终端）在裸部署里都是**同一个系统
用户**。这意味着 agent 能改面板的配置、凭据与审计日志——同 uid 下权限位不提供保护
（`auth/` 是 0700，但 agent 就是那个 owner）。

这是有意的取舍：要隔离就必须有两个身份，而在宿主机上"面板能 setuid"就等于"面板是 root"，
那比现状危险得多。**要隔离请用容器部署**（镜像里面板是 root、agent 是 uid 1000、秘密文件
0700），或者把不同信任级别的 agent 放到不同机器/容器。

不改身份也能做的硬化（追加到上面的 `[Service]`）：

```ini
ProtectSystem=strict
ReadWritePaths=/srv/hdsl /srv/minecraft
ProtectHome=read-only
NoNewPrivileges=yes
PrivateTmp=yes
RestrictSUIDSGID=yes
```

- `ProtectSystem=strict`：整个文件系统只读，只有 `ReadWritePaths` 可写，面板被攻破也碰不到
  `/etc` 和别人的家目录；
- `NoNewPrivileges=yes` 是这份清单里最值钱的一行：连 sudo/setuid 都堵死；
- 这些限制对 agent 同样生效——它们本来就是面板派生的进程。

容器部署不需要这些参数，镜像自己做（见 `docs/design-notes.md §7.5`）；容器里的验收用
`scripts/verify-privilege-split.sh`（以 uid 1000 跑，先 `docker cp` 进容器）。

**供应商 API key 不在这层保护之内**：它在 `<用户目录>/launcher-settings.json` 的 `accounts`
里，agent 读得到（ACP 会话要用它调模型）。这是有意保留的暴露，缓解手段是"一台机器一套 key、
额度按机器给、能撤销能轮换"。详见 `docs/design-notes.md §7.5.1`。

### 10.1 只有 jar 的机器：Node 与 pnpm 从哪来

**JRE 不能替你完成这件事。** 面板是 Java 写的，但 dsh 本体、插件、品牌工具都是 npm 包，
要用 `pnpm`（它自己又是 `node` 上的包）去装。所以裸部署有两个必须满足的依赖：

| 依赖 | 谁需要它 | 缺了会怎样 |
|---|---|---|
| `node` `^22.19.0 \|\| >=24.0.0` | 装 dsh 版本、插件、品牌工具、ZCode 构建 | 创建实例时安装任务失败，错误里写的是「Node.js was not found on PATH and no panel-managed runtime is installed」；**版本不够**时是另一条：安装会先被拒，启动则报「outside the supported range」，品牌工具会在自己的 ESM loader 里崩（`node:zlib has no export named createZstdDecompress`）——分发版仓库的 `nodejs` 包常常就是 18/20，这两条报错都不会主动点出"版本"这个词，面板启动横幅会 |
| `pnpm` | `pnpm install` / `pnpm add`，插件管理也转发给它 | 任务失败在 `pnpm` 这一步；`npm` 单独存在没用 |

**三条路，按省事排序：**

1. **面板自己装（不用 root）**：实例页顶部会挂一条「缺少 Node.js 工具链」提示，点一键安装；
   等价入口是「设置 → Node.js」。运行时下载到 `<HDSL_DATA>/hdsl/runtimes/<版本>/`，
   pnpm 由该运行时自带的 `npm` 装进同一个目录，面板的其它部分按需把它放到 `PATH` 前面——
   系统里的 node 不受影响。默认走 npmmirror，官方源是备选。自动挑选的是**最新稳定版**
   （`-alpha` / `-rc` 之类的预发布会被跳过；要装预发布得在接口里显式给版本号）。
2. **系统包管理器**：`sudo apt install nodejs npm && sudo npm i -g pnpm`。
   注意 Debian/Ubuntu 仓库里的 node 常常偏旧，低于 22.19 时 dsh 会拒绝安装，
   这时回到第 1 条更省事。
3. **容器**：官方镜像里已经带好 Node 22 + pnpm，见 README「方式一」。

**"系统里有 node" 不等于 "能装东西"。** 装 dsh 版本、改启动库、装插件、装品牌工具走的都是
`pnpm`，所以这几条路径挑工具链时会**优先挑带 pnpm 的那个**：`PATH` 上的 node 有 pnpm 就用它，
没有就用手上面板的托管运行时（它自带 pnpm），两者都没有才报错——报错会点名是哪个 node、
缺的是 pnpm、以及两条出路。原先只看 `PATH` 上的 node 在不在，于是"装了系统 node、没装 pnpm"
的机器一律失败在 "pnpm was not found on PATH"，而数据目录里那个能干活儿的托管运行时根本没人问。
启动 dsh 本身仍然只要求 node（`PATH` 优先，其次托管运行时），那一步不需要 pnpm。

> **精简基础镜像要补 `libatomic1`**。Node 26 起的官方二进制链接 `libatomic.so.1`，而
> `debian:*-slim`、`ubuntu:*-minimal` 这类镜像不带它。缺了的表现是"装得上、跑不起来"：
> 解包正常、版本号正常，一执行就 `error while loading shared libraries`。面板现在会在安装
> 结束时真跑一次 `node --version`，跑不起来就删掉那份运行时并把 loader 原话报出来；生产镜像
> 也预装了 `libatomic1`。自己裁镜像时请加上它（Fedora/openSUSE 的包名是 `libatomic`）。

面板启动时会把检测结果打到控制台（`docker logs` 里同样能看到）：

```
runtime  : node MISSING, pnpm MISSING
!! installing dsh versions / plugins / brand tools needs
!! node + pnpm. Either:
!!   1. one-click a panel-managed runtime (Settings -> Node.js);
!!   2. or install Node.js LTS yourself: https://nodejs.org
```

排查时先看这一行：`node 22.23.3, pnpm 11.x` 就是齐了；只有 node 没有 pnpm，
说明当时是用 `npm i -g pnpm` 之外的方式装的 node（比如 corepack 未启用）。

### 10.2 Windows 裸部署：MC 的 Java、目录与服务形态

MC 服务端是**独立于面板的 Java 进程**，所以裸部署要分清两件事：

- **面板自己的 JRE 已经存在**——你能双击/注册服务把 `hdsl-web.jar` 跑起来，就至少有一个
  Java 运行时。`mc.java_path` 留空时，MC 默认就用这一个（`java.home/bin/java.exe`），
  不需要先装第二份 JDK。
- 但**版本可能不够**：1.20.5+ 需要 Java 21，而面板最低支持 JDK 17。设置页的“MC 服务端”
  卡片会显示当前 Java 的路径、版本与来源；创建实例时如果所选版本要求更高，会当场提示。
  自备 Java 21+ 后，把绝对路径填进 `server.yaml` 的 `mc.java_path`，或设
  `HDSL_MC_JAVA_PATH`。面板**不代管、也不随包分发 JRE**。

**目录**：默认 `%USERPROFILE%\mc`（`$HOME/mc`），可用 `mc.dir` 或 `HDSL_MC_DIR` 改。
设置页在功能关闭时就显示实际根目录与来源；每个实例在 `<root>/<id>/`，存档、
`server.jar`、日志都在那里。**改配置不会搬动已建实例**，旧实例页面仍显示各自的真实路径。
Windows 上注意：

- 不要用 SYSTEM / LocalSystem 跑面板：`%USERPROFILE%` 会落到
  `C:\Windows\system32\config\systemprofile`，启动日志会明确警告。用专用非管理员账户。
- 路径尽量短；超过 120 字符会警告（Windows 默认 `MAX_PATH=260`）。
- 不要放网络盘/FAT；用本地 NTFS。

**服务形态**：NSSM / WinSW / 计划任务。服务停止方式要能让 JVM 走 shutdown hook，面板会
先对所有 MC 实例写 `stop` 再退出；直接 `TerminateProcess` 会跳过世界保存。

**控制台**：Windows 没有 PTY，MC 是“日志 + 命令输入框”，不是交互终端。设置页与实例页
都会在打开前说明；功能本身可用，只是形态与 Linux/macOS 不同。

## 11. 把 agent 当对手：三条真边界

面板的凭据（`auth/users.json`、`developer.json`、证书私钥）都是 0600，**这只挡别的用户**。agent 以面板
用户的身份跑，同一 uid 下它可以直接重写这些文件，也能从 `/proc/<pid>/environ` 读到面板进程的环境变量。
面板侧加任何检查都挡不住（校验和与签名密钥也在它读得到的地方），所以这一节讲的是 OS/容器层的边界。

判断标准很简单：**你信不信任跑在实例里的那个 agent？** 信，就保持现状（默认、单用户、最省事）；
不信，就选下面一条，代价从小到大。

### 11.1 外层再认证一次（最便宜，只解决一半）

面板前面放一层自己的认证（nginx `auth_basic`、Authelia、SSO、VPN 白名单）。
它挡不住"改 `users.json`"这件事，但能保证**偷到面板密码也进不来**，并让"面板被人从网络上摸到"变成
"面板只对内网/VPN 可见"。见 §1 的反代写法，加 `auth_basic` 即可。

### 11.2 拆容器：面板与实例分开（推荐给 Docker 部署）

- 面板容器只挂 `auth/`、`certs/`、`logs/`；实例容器只挂实例目录（`hdsl/instances/`、pnpm store）。
- 两边**不共享** `auth/`，agent 也就没有可改的面板凭据。
- 代价：`/i/<id>/` 反代要跨容器访问实例端口（容器网络里的服务名），实例的 `$DSH_HOME` 与 pnpm store
  要重新规划；两个容器都要有相同的 uid 约定（比如都用 1000）。
- 好处：升级、重启、甚至面板整个换掉，都不影响实例；E-Stop 也只需管自己那半边。

### 11.3 不同 uid 跑实例（Linux 裸部署，最彻底）

- 面板以 `hdsl` 用户跑，`/srv/hdsl/data/auth` 保持 `hdsl:hdsl 0700`；实例进程由面板
  **降权**为 `dsh-agent` uid（或用 systemd 的 `User=dsh-agent` 跑一个每实例的 unit）。
- agent 读写自己的 `$DSH_HOME`、工作区、pnpm store，但进不去 `auth/`，也读不到面板的
  `/proc/<pid>/environ`。
- 代价：`$DSH_HOME`、pnpm store、终端 PTY 的属主都要迁；**Windows 没有等价的 uid 语义**，
  这条路在 Windows 上不可用。
- 目前面板没有内置"实例降权"开关（见 `docs/design-notes.md` §7.2 为什么不做假防线）；
  要用就自己用 `sudo -u dsh-agent` / systemd unit 包一层，并接受终端与文件属主的手工维护。

### 11.4 明确不做的事

- 不做"哈希文件保护"（校验和、签名、内存基线告警）：同 uid 下都是剧场，只会让下一个人误以为
  这里有边界。
- 不做"给 agent 少一点权限但还能干活"的中间态：它要么能写自己的 home（那就一样能写面板数据，
  只要同 uid），要么根本跑不起来。

## 12. FreeBSD（裸 jar，实验）

面板是单 jar，FreeBSD 上用系统 JDK 21 + 系统 Node/pnpm 就能跑；没有官方容器镜像，也不在
CI 的构建矩阵里——属于「能用，但没纳入发布验收」的平台。

已验证（FreeBSD 15.1 + openjdk21，amd64）：

| 能力 | 状态 |
|---|---|
| 面板、登录、设置、品牌列表 | 正常 |
| 交互式终端（Web Shell） | **支持**：`Pty.spawn` 走 `PosixPty`（`openpty` + `posix_spawn`），真机验证 openpty / resize / stdin / 退出码 |
| 转码 | 正常（ffmpeg 软件编码；没有 GPU 直通） |
| 托管 Node | **不可用**：nodejs.org 不发布 FreeBSD 构建，面板会明确拒绝，不会下载 Linux ELF；用系统 Node |
| dsh 实例 | **不可用**：`node-addon-require-builtin` 原生模块没有 FreeBSD 构建，上游源码也没有 FreeBSD 的 context reader，而 `dsh-app-boot` 启动时强依赖它；面板会直接拒绝安装 |
| opencode | **不可安装**：上游只发布 Linux / macOS / Windows 的二进制包，没有 FreeBSD 构建；品牌页会直接给出原因 |
| ZCode | **不可构建**：构建要装 Electron 桌面端，而 Electron 没有 FreeBSD 构建；实验性品类在 FreeBSD 上直接拒绝 |
| Minecraft 控制台 | pipe（日志 + 命令框），不是 PTY；启动横幅会写「PTY console not enabled」 |
| 系统指标页 | 大部分为空：CPU / 磁盘 / 网络 / GPU 依赖 `/proc`、`/sys`，FreeBSD 默认不挂 procfs |
| uid 分离 | 不支持（`Privileges` 只在 Linux/macOS 实现），面板与 agent 同用户 |

这张表在面板里也有一份：**设置 → 通用 → 平台兼容性**（`GET /api/system/platform`），
显示当前内核版本、整体结论和逐项差异；`procfs` 是否挂载会在运行时探测（挂了就不再报
「系统指标为空」）。未纳入发布验收的平台（FreeBSD / UNKNOWN）才会展开这张卡。

准备：

```sh
pkg install openjdk21 node npm python3 ffmpeg gmake
# node 版本由 ports 决定；pnpm 可用 corepack 或 npm i -g pnpm
```

开机自启可以用 rc.d，把下面这段存成 `/usr/local/etc/rc.d/hdsl_web`（`chmod +x`）：

```sh
#!/bin/sh
# PROVIDE: hdsl_web
# REQUIRE: NETWORKING
# KEYWORD: shutdown
. /etc/rc.subr
name=hdsl_web
rcvar=hdsl_web_enable
pidfile="/home/dev/hdsl/hdsl.pid"
load_rc_config $name
: ${hdsl_web_enable:="NO"}

start_cmd="${name}_start"
stop_cmd="${name}_stop"
status_cmd="${name}_status"

hdsl_web_start() {
    /usr/bin/su dev -c "cd /home/dev && HOME=/home/dev \
      PATH=/home/dev/hdsl/bin:/usr/local/bin:/usr/bin:/bin \
      HDSL_DATA=/home/dev/hdsl/data HDSL_PORT=3080 \
      nohup /usr/local/bin/java -jar /home/dev/hdsl-web.jar \
      >> /home/dev/hdsl/hdsl.log 2>&1 & echo \$! > /home/dev/hdsl/hdsl.pid"
}
hdsl_web_stop() {
    [ -s "${pidfile}" ] && kill "$(cat "${pidfile}")" 2>/dev/null
    sleep 2
    [ -s "${pidfile}" ] && kill -9 "$(cat "${pidfile}")" 2>/dev/null
    rm -f "${pidfile}"
}
hdsl_web_status() {
    if [ -s "${pidfile}" ] && kill -0 "$(cat "${pidfile}")" 2>/dev/null; then
        echo "${name} is running as pid $(cat "${pidfile}")."
    else
        echo "${name} is not running."
        return 1
    fi
}
run_rc_command "$1"
```

升级：停服务 → 覆盖 `/home/dev/hdsl-web.jar` → 起服务。`/data` 不动。
