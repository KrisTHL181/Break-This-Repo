# 会话交接：HDSL-web 实机修复记录（2026-10-03）

> 这份文档是一次 AI 协作会话的压缩交接：用户提了什么、查到了什么、改了什么、怎么验证的、
> 还剩什么没做。目标是**看完它就能接手后续工作**，不必回看会话原文。
>
> **隐私约定（写文档时请继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；
> 示例统一用 `dsh.example.com`、`192.0.2.x`（TEST-NET）、`/srv/...`、`<口令>`。
> 本文里所有地址与路径都是这种占位值。

## 0. 项目背景

- 项目：**HDSL-web**——把桌面版 [HDSL](https://github.com/MCXCC303/HDSL)（DeepSeek Harness 启动器）
  搬进浏览器的单容器实现：一个 fat jar + 一个 Debian 镜像，面板块在 3080，dsh 实例由面板拉起
  并反代到 `/i/<实例id>/`。
- 部署形态：`docker compose`，数据卷 `/data`；面板走 HTTPS（证书可自签或上传），
  实例端口从池子 **3081–4081** 分配、只监听回环、不对外发布。
- 领域层（`org.jackhuang.hmcl.dsh` / `setting`）是桌面版代码的**逐字拷贝**，本次所有改动都
  刻意避开它，落在 `org.jackhuang.hmcl.web.*` 与 `web/`（面板 SPA）里。

## 1. 会话里提出的问题与结论（按时间顺序）

| # | 用户提出的现象 | 排查结论 |
|---|---|---|
| 1 | 「HDSL 版本管理有问题，经常无法启动实例；快速创建不会自动安装」 | 版本管理内核没坏，坏的是 Web 层的**编排**与**前后端状态契约**（见 §2.1） |
| 2 | 「添加失败：kind must be one of official, third-party, offline」 | 供应商列表的 `kinds`（协议）被前端当成账户类型发了出去（见 §2.2） |
| 3 | 「要不要给 dsh 一个存储挂载点？工作区也该能挂」 | 工作区默认落在启动器自己的实例目录，改成 `<home>/workspace`（见 §2.3） |
| 4 | 「`/i/<实例>/` 一直在闪，历史加载失败：`api gateway: Remote stream WebSocket closed（gateway/internal）`」 | 面板 WS 中继用了 Jetty 默认的 64 KiB 上限，dsh 的历史 payload 一超就被掐断（见 §2.4） |
| 5 | 「要上传 GitHub 你还暴露真实 NAS 地址？」 | 文档/注释里有真实域名与路径 → 清洗 + 改写已推送的历史（见 §2.5） |

期间顺手发现并修掉两个部署级问题：容器一直 `unhealthy`（Jetty SNI 主机校验）、
`NPM_REGISTRY` 带尾斜杠导致 corepack 404；以及 `docker logs` 里中文显示成 `?????`（缺 `LANG`）。

## 2. 五次改动（对应本地提交）

### 2.1 实例编排：创建即安装 / 启动补装（提交 `feat(instances): …`）

**问题**：`POST /api/instances` 只写 `instance.json`，前端向导也只调它——"快速安装"建出来的是
空壳（现场 `instances/<id>/` 只有 `home/` 与清单，没有 `dsh/`）。后端 `stateOf` 只发
`STOPPED/STARTING/RUNNING/STOPPING/FAILED`，前端却有 6 处按 `NOT_INSTALLED`/`INSTALLING` 做门禁
（全是死代码）；新实例点「启动」被 409 `not installed` 挡住，只弹 4.5 秒英文 toast。
另有版本漂移：详情页版本选择器不随实例重置（路由不重挂组件），会把 A 的版本提交给 B；
`/install` 成功后无条件把清单改写成"请求的版本"，从不回读磁盘。安装失败则完全不可见
（pnpm 输出只进内存任务表）。

**改动**（按文件）：
- `web/instance/InstanceRuntime.java`：`stateOf` 增补 `NOT_INSTALLED` / `INSTALLING`，
  判定顺序 = 活进程 > 停止中 > 安装任务 > 未安装 > 失败 > 已停止（活进程优先，让"装完自动
  启动"的窗口期报 STARTING）；新增 `announceState(id)` 在安装开始/结束广播状态。
- `web/http/InstancesApiServlet.java`：抽出三个入口共用的 `submitInstall`；创建默认
  `autoInstall:true` 并返回 `installTaskId`；`launch` 遇未安装实例先装再启
  （202 `{state:"installing",taskId}`，`autoInstall:false` 退回旧 409）；`/install` 在运行中拒绝、
  安装中拒绝改名；装完**回读** `node_modules/@deepseek-ai/dsh/package.json`，与请求不符时按
  磁盘真实版本记录并把任务判失败；失败写容器日志（含 pnpm 输出尾 20 行）与任务记录；
  `registryHint()` 对 `ERR_PNPM_FETCH_404` / `ERR_PNPM_NO_MATCHING_VERSION` 追加中文提示。
- `web/task/TaskService.java`：`TaskInfo` 增加 `instanceId`（WS 事件早就有 `instance` 字段，
  只有 REST 契约和前端类型缺它，导致前端任务匹配退化成"最近活跃任务"）。
- `web/server/HdslServer.java`：两个 TLS 连接器关闭 SNI 主机校验（单证书面板，校验只误伤
  本机探针：`curl https://127.0.0.1:3080/api/health` 曾返回 400 Invalid SNI）。
- `Dockerfile`：入口脚本归一化 registry 尾斜杠后再写 pnpm config 与 `COREPACK_NPM_REGISTRY`
  （否则 corepack 拼出 `mirror//pnpm/latest` → 404）；镜像加 `LANG=C.UTF-8`（此前 JVM
  `stdout.encoding` 退化成 ASCII，中文日志在 `docker logs` 里是 `?????`）。
- 前端 `web/src/*`：详情页版本选择器随实例重置；「未安装」卡片显示失败原因并补
  「安装并启动」；store 落库 task 的 `instance`/`error` 并按 `instanceId` 精确匹配；状态徽章
  支持"未安装"；自动安装页在实例运行中禁止换版本；向导提示"已创建，正在自动安装"。
- 测试：`InstanceApiTest` 新增 4 个用例（创建即安装 / 启动补装 / `INSTALLING` 可见 /
  回读不符判失败）；新增 `InstallFailureHintTest`；`fake-pnpm` 支持 `9.9.9-slow`、
  `9.9.9-mismatch` 两个特殊版本，并按清单写入真实版本；其余用例显式传 `autoInstall:false`。
- 文档：新增 `docs/launcher-orchestration.md`（诊断、状态机、契约变化、取舍）。

**实机验证**：创建 → `NOT_INSTALLED`；启动 → 202 `installing` → `INSTALLING`×6 → `STARTING`
→ `RUNNING`；复现上游删包场景（`0.0.1-rc.1`）确认中文提示落到任务错误与容器日志。
任务 REST 契约补 `instanceId` 后，前端进度/取消不再串台。

### 2.2 账户添加必然失败（提交 `fix(accounts): …`）

**根因**：`GET /api/vendors` 的 `kinds` 按 `VendorsApiServlet` 自己的文档报的是**协议**
（`vendor.api()`，如 `openai-completions`）；面板把它当**账户类型**填进表单、随
`POST /api/accounts` 发出；后端只认 `official / third-party / offline` → 400。从列表里选
供应商添加 100% 失败，只有"自定义供应商"（不选类型）那条路能成功。

**改动**：前端不再发 `kind`（类型由后端按供应商推断：首选供应商 → official，其余 →
third-party），协议只作只读信息显示；`AccountsApiServlet` 拒收时点名收到的值；
`AccountApiTest.theVendorPathInfersTheKindAndProtocolsAreNotKinds` 钉住契约。

**契约要点（别混淆）**：`/api/vendors` 的 `kinds` = 协议；`/api/accounts` 的 `kind` = 账户类型。

### 2.3 工作区：放到 `<home>/workspace`（同一提交）

**问题**：`DshInstance.workspace`（会话读写文件、跑命令的目录，也是 dsh 进程 cwd）取
`GameDirectoryManager` 选中的目录，而没有配置时它就是 `GameDirectory.defaultDirectory()`
= `<data>/hdsl/instances` —— 启动器放实例清单与安装的地方。

**改动**：新增 `web/config/WorkspaceDirectory`（web 层，未动共享领域层）：启动时按
`HDSL_WORKSPACE` → 否则 `<user.home>/workspace`（容器里 `/home/hdsl/workspace`）解析，
创建后用 `GameDirectoryManager.add+select` 注册成选中目录，新建实例默认落在它上面；
解析/创建失败只记日志、保留启动器默认。`Main` 接入；`Dockerfile` 预建该目录且属主 hdsl
（否则命名卷首次挂载是 root 属主，dsh 写不进去）；compose 写清两个挂载点（`/data` 必挂；
工作区默认不挂 = 容器内目录，挂了 = 宿主目录或命名卷，注释给了两种写法）。

**语义**：目录一直存在；新实例用新工作区，**已有实例保留创建时记录的那个**（用户的旧实例
仍指向 `<data>/hdsl/instances`）。`PATCH /api/instances/{id}` 目前不支持改工作区。

**实机验证**：绑定挂载宿主目录后，新建实例 `instance.json` 记录
`workspace=/home/hdsl/workspace`，启动的 dsh 进程 `cwd` 即该目录，宿主机目录里能看到文件。

### 2.4 dsh 界面「历史加载失败 + 一直闪」（提交 `fix(proxy): …`）

**根因**：面板把 `/i/*` 的 WebSocket 升级中继到实例端口（`web/proxy/InstanceProxyWebSocket`），
中继用的是 **Jetty 默认上限**：`WebSocketConstants.DEFAULT_MAX_FRAME_SIZE /
DEFAULT_MAX_TEXT_MESSAGE_SIZE / DEFAULT_MAX_BINARY_MESSAGE_SIZE` 都是 **65536**。dsh 一份
会话历史 / 一条工具结果 / 一个附件都是**一整条 WebSocket 消息**，一超 64 KiB，中继这跳就以
1009 失败，继电器随即用 1011 掐断两端；dsh 客户端报
`api gateway: Remote stream WebSocket closed（gateway/internal）`，然后重连重试 → 界面一直闪、
历史永远加载不出来。dsh 自己服务端（node 的 `ws`）默认允许 100 MiB，瓶颈一直在面板这一跳。

**改动**：`InstanceProxyWebSocket.MAX_MESSAGE_BYTES = 32 MiB`（frame / text / binary 一起抬），
`HdslServer` 在**两侧**都设：服务端 `ServerWebSocketContainer`（浏览器→中继）与转发用的
`WebSocketClient`（中继→dsh）；中继**非正常关闭时写日志**（实例、状态码、原因）——此前
1009 这类失败在面板日志里毫无痕迹。

**A/B 实测**（假 dsh 只吐一个大帧，见 §4 复现配方）：
修复前两个方向都 `CLOSE code=1011 reason="upstream connection failed" bytes=0`；
修复后两个方向都 `RECV frame #1 1048576 bytes`。

### 2.5 隐私清理与历史改写（混在前述提交里）

- `docs/deployment.md`：真实域名 → `dsh.example.com`。
- `docker-compose.yml` / `README.md`：真实宿主机路径 → `/srv/dsh-workspace`。
- 全历史扫描（`git grep` 遍历所有提交）确认：新历史里没有真实域名、内网地址、宿主机路径、
  口令。剩下两处判为可保留：README 徽章里的 GitHub 账号名（仓库地址本身就是它）；
  `DshReadinessLineTest` 里的示例 IP 本次也换成了文档专用段（`192.0.2.5`）。
- 已推送的旧提交里含真实域名 → 用 `rebase -i`（`edit` 停在那个提交）把文件改掉再
  `--amend`，后续提交重放。改写前后**文件内容零差异**（`git diff <旧 tip> <新 tip>` 为空），
  所以之前的测试结论仍然有效。改写前的提交留在本地分支 `backup/pre-sanitize`。

## 3. 关键环境事实（避免重复踩坑）

**网络实测**（本机到各源）：Maven Central ≈ 17 KB/s（这是最初构建 41 分钟的主因）；
阿里云 Maven ≈ 1 MB/s；腾讯/阿里的 gradle 与 node 镜像 ≈ 12–14 MB/s；`services.gradle.org`
完全不可用、`nodejs.org` TLS 失败。npm 官方源有时很快（`npm ci` 5.5s）。

**构建**：`NPM_REGISTRY=https://registry.npmmirror.com/ docker compose up -d --build`
（暖缓存 20–40 秒）。Dockerfile 里已有阿里云 Maven 镜像 init script + gradle cache mount。

**测试**（宿主没有 java/node，一律走容器；需要 node+pnpm 才能把 node 相关用例真跑起来）：

```bash
# 从运行中的面板容器里取一套 node+pnpm
docker cp <面板容器>:/opt/node /tmp/hdsl-test-node
docker cp <面板容器>:/data/corepack /tmp/hdsl-test-corepack
docker run --rm -v "$PWD":/src -v <gradle 缓存卷>:/root/.gradle \
  -v /tmp/hdsl-test-node:/opt/node -v /tmp/hdsl-test-corepack:/corepack \
  -e COREPACK_HOME=/corepack \
  -e PATH=/opt/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  -w /src eclipse-temurin:21-jdk ./gradlew --no-daemon test -x buildWeb
```

- 必须 `-x buildWeb`：宿主上的 `web/node_modules` 属主与容器不一致，esbuild 会 EACCES；
  面板构建在镜像构建里做。
- 不带 node 时那 21 个 node 用例会被跳过（`Assumptions`），看起来"全绿"其实没跑。
- 最近一次：**421 用例 / 0 失败 / 0 错误 / 0 跳过**。

**前端检查**（`vite build` 不做类型检查，必须单独 typecheck）：

```bash
docker run --rm -v "$PWD":/src:ro -w /work node:22-bookworm-slim sh -c '
  mkdir -p /work && cd /work &&
  tar -C /src --exclude=web/node_modules --exclude=web/dist --exclude=build --exclude=.git -cf - web src/main/resources/assets | tar -xf - &&
  cd web && npm ci --no-audit --no-fund --registry=https://registry.npmmirror.com/ &&
  npm run typecheck && npm run build'
```

**面板验证**：`curl -k https://127.0.0.1:3080/api/health`（SNI 修复后不需要 `--resolve`；
返回 `{"status":"ok"}` 即服务正常）。登录是 `POST /api/auth/login {username,password}`，
口令以哈希存在 `/data/auth/users.json`（读不出明文）。要端到端驱动面板时，起一个**临时容器**
（自带 `HDSL_ADMIN_PASSWORD`、独立数据卷）比动生产数据安全得多。

**部署的副作用**：`docker compose up -d` 会重建容器，**所有运行中的 dsh 实例会被停掉**
（会话与历史在磁盘上不丢，用户需要回面板重新点「启动」）。部署前/后要提醒用户。

**数据卷布局**：`/data/server.yaml`（面板配置）、`/data/auth/users.json`（口令哈希）、
`/data/certs/`、`/data/hdsl/instances/<id>/{instance.json,dsh/,home/}`、`/data/pnpm-store/`、
`/data/corepack/`。实例的 dsh 与 `DSH_HOME` 都在实例目录里。

**dsh 侧的设计事实**（排查时反复用到）：
- 每个实例一份 dsh 安装 + 一份 `DSH_HOME`；`profiles/web` 的 bundles 是
  `@deepseek-ai/dsh-base` + `@deepseek-ai/dsh-web-app`；启动器按需往 home 里注入
  `settings.yaml` / `.env` / `.hdsl-injected*.json`（面板的"账户"就是这么进到 dsh 的）。
- 浏览器与守护进程之间是 WebSocket（`dsh-client-connection`，channel `/api`，**2 秒 ping**，
  无子协议），历史、工具结果都是**大消息**。
- dsh 的「设置 → 模型」页**在非回环浏览器里按设计不可用**：客户端 `isLoopback` =
  `location.hostname ∈ {localhost,127.x,[::1]}`，非回环时设置镜像 `persistence="memory"`、
  `ensure()` 直接返回，页面显示 `settings are unavailable in this browser`。
  想看那个页面就用 SSH 隧道把浏览器地址变成 `127.0.0.1`；正常配置模型请在**面板的账户页**做。
- 上游会删包：`@deepseek-ai/dsh@0.0.1-rc.1` 依赖的 12 个包在 npmjs 与镜像上都 404，
  该版本**永远装不上**；受影响的只有 `0.0.1-rc.1` / `0.0.1-rc.2`（从 `0.0.1-rc.5` 起依赖齐全）。
  版本列表的 `installable` 判断（配套包 lockstep）拦不住这类，只能靠失败提示。

## 4. 复现配方：面板 WS 中继的上限

用一个"只吐一个大帧"的假 dsh 当上游，再用 node 的 `ws` 当客户端，就能验证中继的上限：

```js
// 假 dsh（给实例的 dsh/node_modules/@deepseek-ai/dsh/lib/bin.js 覆盖成这个）
// 打印就绪行让面板知道端口；WS 一建立就推一个 SIZE 字节的文本帧，每 2 秒 ping。
const http = require("http"), crypto = require("crypto");
const SIZE = Number(process.env.STUB_SIZE || 1048576), GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
if (process.argv.includes("--help")) { console.log("Usage: dsh [--no-open]"); process.exit(0); }
function frame(p) {                       // 服务端帧不掩码；长度用 16/64 位扩展
  const h = p.length < 126 ? Buffer.alloc(2) : p.length < 65536 ? Buffer.alloc(4) : Buffer.alloc(10);
  h[0] = 0x81;
  if (h.length === 2) h[1] = p.length;
  else if (h.length === 4) { h[1] = 126; h.writeUInt16BE(p.length, 2); }
  else { h[1] = 127; h.writeBigUInt64BE(BigInt(p.length), 2); }
  return Buffer.concat([h, p]);
}
const server = http.createServer((q, s) => s.end("stub ok"));
server.on("upgrade", (req, sock) => {
  const accept = crypto.createHash("sha1").update(req.headers["sec-websocket-key"] + GUID).digest("base64");
  sock.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + accept + "\r\n\r\n");
  sock.on("data", () => {}); sock.on("error", () => {});
  sock.write(frame(Buffer.alloc(SIZE, 0x61)));
  setInterval(() => sock.write(Buffer.concat([Buffer.from([0x89, 0x02]), Buffer.from("hb")])), 2000);
});
server.listen(0, "127.0.0.1", () => console.log("dsh web: http://127.0.0.1:" + server.address().port + "/?token=stubtoken"));
setInterval(() => {}, 1 << 30);
```

客户端用实例里现成的 `ws` 包（`.../dsh/node_modules/.pnpm/ws@*/node_modules/ws`）连
`ws://127.0.0.1:3080/i/<实例>/<任意路径>`，带 `Cookie: hdsl_session=<面板会话>`（中继鉴权用的
是面板会话 cookie），收到大帧即证明中继上限足够。

## 5. 刻意没做的事 / 已知问题

- **没做依赖预检**：安装前把整棵依赖树验一遍（要么给每次安装加解析开销，要么动与桌面版共享的
  领域层）。目前的答案是"失败要看得见 + 提示可执行"，不是"提前拦截"。
- **版本列表的 `installable` 对 pre-lockstep 版本偏乐观**（早于配套包机制的版本一律算可安装），
  所以 `0.0.1-rc.1` 这种"上游已删包"的版本仍会被列成可安装。
- **`--no-open` 探测**（`DshLauncher` 内）：5 秒超时，且**超时会被当成"不支持"缓存**；机器忙时
  那次启动就不带 `--no-open`。属共享领域层，本次没改（也是测试在负载高时偶发失败的一个来源）。
- **中继上限目前只有实机 A/B 验证，没有 JUnit 用例**。可补：测试里起一个 Java WS 服务端，
  写一个只打印就绪行的假 dsh，再用 Jetty 客户端连 `/i/<id>/test` 断言 1 MiB 消息能到。
- 已有实例的 `workspace` 仍是旧值；`PATCH /api/instances/{id}` 不支持改工作区（要改只能重建实例）。
- 仓库文档/注释里不要再出现真实域名、内网地址、宿主机路径与口令（示例用 `dsh.example.com`、
  `192.0.2.x`、`/srv/...`、`<口令>`）。README 徽章里的账号名是仓库地址本身，属公开信息。

## 6. 当前 git 状态与待办

本地 `main` 的提交（改写后，历史里已无真实域名/路径）：

```
fix(proxy): 抬高 /i/* 中继的 WebSocket 上限，修「历史加载失败、界面一直闪」
fix(accounts): 修好「添加账户」，并给 dsh 一个 /home 下的工作区
chore(build): untrack the generated web bundle
feat(instances): 让面板像启动器一样编排安装与启动（创建即安装 / 启动补装）
```

待办：

1. **推送剩下的提交**：改写后的历史已经在远端了（本地记录的 `origin/main` 指向改写后的
   `fix(accounts)` 提交），本地还多两个提交（WS 中继修复、本文档），`git push origin main`
   即可，**不需要** `--force`。要确认远端真实状态用 `git ls-remote origin main`。
   背景：改写前推送过的历史里含真实域名，已用 `rebase -i` + `--amend` 清掉；旧提交在 GitHub 上
   仍可能按 SHA 直接访问（直到服务端 GC），若要求彻底清除只能删库重建。
2. 确认无误后删除本地备份分支 `backup/pre-sanitize`（里面还留着改写前的提交）。
3. 可选：给中继上限补 JUnit 回归用例。
4. 部署后提醒用户：容器重建会停掉运行中的实例，需要回面板点「启动」，然后刷新 dsh 页面。
