# HDSL Web 移植计划（单容器网页版启动器）

## 目标

把 `HDSL`（Hello DeepSeek! Launcher，JavaFX 桌面应用）移植为**单 Docker 容器**的网页版：
打开网页 → 登录验证 → 傻瓜式创建 dsh 实例 → 点击启动 → **同端口、同证书**经反向代理直接使用 dsh web 界面。支持 MC 式版本隔离与插件管理，完整复刻 HDSL 全部功能。

## 关键调研结论（决定架构的事实）

- `dsh web` 只绑 `127.0.0.1`（`--host 0.0.0.0` 被 CLI 拒绝），天然适配"容器内多实例 + 边缘代理"。
- dsh web **官方支持路径前缀反代**：构建 base `./` + 服务端注入 `<base href="./">` + API/WS 全部文档相对路径；就绪信号是 stdout 一行 `dsh web: http://127.0.0.1:<port>/?token=<t>`。
- 反代三件套（有 dsh 仓库 e2e `apps/web/tests/public-mount.e2e.ts` 背书）：
  1. 剥 `/i/<实例id>/` 前缀，**Host 头原样透传**（dsh 校验 Origin==Host 与 Host 可信表，改写 → 403）；
  2. 每实例启动加 `dsh web --trusted-host <公网域名> --no-open`（trustedHosts 无通配符）；
  3. Set-Cookie `Path=/` 改写为 `Path=/i/<实例id>/`（cookie 名只由主机名决定，多实例同名会互相覆盖）；
  4. `/i/<id>` → 301 `/i/<id>/`（token 兑换需要目录形态）；WS（`/api/remote.mux`）upgrade 原样转发。
- HDSL 领域层 76 个文件中仅 6 个沾 JavaFX；47 个纯领域 JUnit 测试可直接保留；安装/插件/账户/pack 全部逻辑零 FX。
- nanohttpd 是死代码；HDSL 无任何 TLS 代码（无历史包袱）。

## 产品模型：两个界面，独立管理

- **HDSL 面板**（标签页 A）：纯管理界面——登录、创建/安装实例、启动进度、停止。不负责使用 dsh。
- **dsh web 界面**（标签页 B）：dsh 自己的网页，与面板是**两个独立的浏览器标签页**。同一域名、同一端口、同一证书；面板只管"点火"和"熄火"。

**启动流（傻瓜式）**：面板点"启动" → 面板内出现进度条（阶段：安装中→拉起进程→等待就绪）+ 实时滚动日志 → 解析到就绪行后**自动 `window.open` 新标签页**打开 `/i/<id>/?token=…` → 用户在新标签页里使用 dsh；面板标签页同时显示"运行中"状态。

**停止流（真停止，收缩攻击面）**：面板点"停止"（或关闭 dsh 标签页不影响，dsh 继续跑）→ `DshProcess.stop()`：postExit 命令 → destroy → 宽限 5s → destroyForcibly → 杀整棵进程树。停止的语义是**进程不存在**：端口即刻关闭、`/i/<id>/` 立即 404、token 随进程消亡（每进程内存随机、重启即换）。不运行 = 没有可被打的东西。

## 界面效果：像素级复刻（按用户明确要求更正）

- **dsh web 界面**：零损失（反代原样送达）。
- **HDSL 管理面板**：**像素级复刻桌面版 HDSL**（非"设计语言还原"——此为按用户反馈更正后的目标）。施工图纸见 `docs/ui-spec.md`（从 HDSL 源码提取的色板/尺寸/字号/动画全量规范），视觉基准为 `docs/ui-reference/`（Xvfb 实跑桌面版 + xdotool 点击捕获的真实截图）。主题用 `@material/material-color-utilities` 以种子 #5C6BC0/FIDELITY/Spec2025 生成亮暗双套 Monet 变量，默认跟随系统；图标 path 逐字提取自 HDSL `ui/SVG.java`；文案照抄 `I18N_zh_Hans.properties`。
- 与桌面版的仅有的差异：网页登录页/初始化引导页（HMCL 对话框风格）、**整页铺满视口且不呈现桌面窗口三键（帮助/最小化/关闭）**、新增"HTTPS 证书/日志/控制台/环境体检"入口（沿用 HMCL 组件样式）。

## 总体架构

```
浏览器标签页 A：HDSL 面板          浏览器标签页 B：dsh web
  │  https://host:443（同一张证书）
  ▼
┌─ 单容器 ─────────────────────────────────────────────┐
│  Jetty 边缘（一个 JVM 进程）                          │
│   ├─ 静态 SPA（React 构建产物，打进 jar）              │
│   ├─ /api/*  REST（认证 + 实例/版本/插件/账户/pack）    │
│   ├─ /ws    WebSocket（日志流、状态事件、启动进度条）    │
│   └─ /i/{id}/*  反向代理 → 127.0.0.1:3081-4081         │
│        （剥前缀 / Host 透传 / Cookie Path 改写 / WS 中继）│
│  Java 领域层（移植自 HDSL）：实例/版本/插件/账户/pack    │
│  dsh 实例进程池（node bin.js --profile web …）          │
│  容器内置 Node ^22 + corepack pnpm                    │
└──────────────────────────────────────────────────────┘
  │  volume: /data
```

**技术选型**：嵌入式 Jetty 12（`SslContextFactory.reload()` 一等支持证书热替换；内置 WS；ProxyServlet/WebSocketProxy 可用于反代）。前端 React + Vite（Docker 多阶段构建）。JSON 复用 gson。

## 项目形态：新建独立项目 `HDSL-web`

不动 HDSL 桌面仓库。在 `/home/coder/code/dsh/HDSL-web/` 新建单模块 Gradle 项目：

```
HDSL-web/
  build.gradle.kts          # java + application + shadow，产出单个可执行 fat jar
  settings.gradle.kts
  web/                      # React SPA 源码（Vite 构建，dist 打进 jar）
  src/main/java/org/jackhuang/hmcl/
    dsh/…                   # 自 HDSL 拷贝的领域层（保留原包名与 GPL 头/NOTICE）
    setting/…               # 拷贝，FX 剥离版
    util/…                  # 仅拷贝零 FX 的工具类（ManagedProcess/Logger/Metadata…）
    web/…                   # 新增服务端代码（Jetty 装配/REST/WS/反代/证书/认证/进程池）
  src/main/resources/web/   # SPA 构建产物（gradle 打包前由 web/dist 拷入）
  src/test/java/…           # 领域测试（自 HDSL 拷贝）+ 新增服务端测试
  Dockerfile / docker-compose.yml / README.md
```

- **源码来源**：一次性拷贝 HDSL 的 `dsh/**`、`setting/**`、FX-free `util/**`，拷贝时完成 6 个类的去 FX 化（见下）；此后两仓库独立演进（GPLv3 允许，保留版权头即可），领域层 bug 修复按需手动同步。
- **6 个类去 FX 化（只改拷贝版）**：
  1. `DshInstallProgress`：FX Property → 普通字段 + `Consumer<Progress>` 监听器（解析逻辑不动）；
  2. `LauncherSettings`：Property/ObservableList → POJO + 简易变更监听；
  3. `SettingsManager`：剥掉 `javafx.scene.paint` 主题色段与窗口几何快照；
  4. `DshProcessManager`：删 `Platform.runLater` 崩溃对话框段，改事件发布；
  5. `ui.LogLine` → 迁至 `util.logging`，`DshProcess` 改 import；
  6. `DshInstanceIcon(s)`：FX Image → 图标资源路径字符串。
- 验证：项目编译零 `javafx` import；拷贝的 47 个领域测试全部通过。

## 单 jar 打包：可以，且就是交付形态

- Shadow 插件产出**单个可执行 fat jar** `hdsl-web-<version>.jar`：服务端 + 领域层 + Jetty/gson 等全部依赖 + SPA 静态资源（`web/dist` → `processResources` 前拷入打包）。运行只需 `java -jar hdsl-web.jar`。
- jar 之外唯二的外部依赖：**JRE 21** 和 **Node ^22/≥24 + pnpm**（spawn 子进程用，不进 jar）；Docker 镜像负责提供这两者，裸机部署则要求宿主机已装。
- Docker 运行时镜像是**完整 Debian 系统**（非裸 JRE）：apt 装 OpenJDK 21 JRE，Node 22 官方 tarball 装入 `/opt/node`，corepack 启用 pnpm——容器内就是一个带 apt 的正常 Debian，便于排障和扩展。

## 持久化挂载（单卷 `/data`）

```
/data
  server.yaml            # 声明式配置（auth/https/port/环境）
  certs/                 # 上传的证书（PEM 或 PKCS#12）
  auth/users.json        # 口令哈希（0600）
  hdsl/                  # hdsl.home（-Dhdsl.home=/data/hdsl）
    instances/<id>/{instance.json,dsh/,home/,dsh.installing/,plugins/}
    homes/<version>/     # VERSION_SHARED 隔离模式的共享 home
    runtimes/            # 备用：自下载 node（镜像已内置 node）
    catalog/ logs/ launcher-settings.json
  pnpm-store/            # 共享 pnpm store（多实例安装提速）
```

删除实例（dsh / 品牌 / ZCode 一致）：**名字先拿掉，内容随删**——先把 `<id>` 改名成
`<id>.deleting-<millis>`，再递归删。列表按目录名找实例，所以改名成功那一刻实例就已经不在
列表里了；删不掉的残留（清理时被拒：只读文件、杀软/索引器的瞬时占用这类）留成
`.deleting-<millis>`，由**启动时**的 `DshInstanceManager.sweepTombstones()` / 品牌与 ZCode 各自的
`sweepTombstones()` 再删一次。改名本身失败才报"删不掉"，此时内容一点没动（Windows 上活着的
文件句柄连目录改名都会挡住，这也是删除前先 `ProcessTree.killReferencing` 的原因）。

环境变量覆盖：`HDSL_PORT`（默认 **3080**，HTTPS 启用时同端口说 TLS）、`HDSL_ADMIN_PASSWORD`（未设则首启生成并打印到日志）、`NPM_CONFIG_REGISTRY`（npmmirror 等）、`HDSL_DATA=/data`。dsh 实例池仍占 3081–4081（容器内回环，与 3080 不冲突）；可选 `http_redirect_port: 80` 做 HTTP→HTTPS 跳转。

## 声明式 HTTPS 设计

`server.yaml`：
```yaml
https:
  enabled: true            # false = 纯 HTTP（仅建议回环）
  cert_file: certs/fullchain.pem   # 或 pkcs12_file/pkcs12_password
  key_file:  certs/privkey.pem
  redirect_http: true      # 80→443 跳转（同端口模式则忽略）
```
- 启用但无证书 → 首启自动生成自签证书（可用，浏览器告警），管理页"证书管理"上传 PEM 或 .p12 即替换并**热重载**（`SslContextFactory.reload`），无需重启。
- 同一张证书服务面板与全部 `/i/<id>/` 反代（dsh 强制 127.0.0.1，TLS 终结在边缘，天然满足）。

## 认证设计

- 单管理员：`POST /api/auth/login`（PBKDF2WithHmacSHA256 哈希，盐 per-user，JDK 内置），会话 = HttpOnly + SameSite=Strict cookie（TLS 下加 Secure）。
- 除 `/api/auth/login` 与健康检查外，**一切资源都要会话**：`/api/*`、`/ws`、SPA、`/i/*`（dsh 自身还有 token+cookie 双保险）。
- `auth.disabled: true` 仅在绑定 127.0.0.1 时允许（开发用）。
- **Phase 之后加的第二把钥匙**：开发者密码（`developer.json`，0600）+ 会话级提权，守着终端；
  可选扩到实例网页、实例写操作或全部 API（`developer_guard.scope`），另有一个会把实例进程树
  一起强杀的紧急停机。完整威胁模型、边界与"明文 HTTP 默认只警告"的口径见
  `docs/design-notes.md` §7；操作面见 `docs/web-shell.md` §4.2 / §4.3。

## 反代与实例启动细节

- 实例启动：`node <实例>/dsh/node_modules/@deepseek-ai/dsh/lib/bin.js --profile <p> --no-open --port <N> --trusted-host <公网域名>`；`--trusted-host` 取 `server.yaml public_base_url` 的 host，缺省用触发启动请求的 Host 头。
- 就绪：`DshProcess.WEB_READY` 正则捕获实际端口+token（**以就绪行为准**，不信 manifest 端口——patch 层可改端口）；反代目标读该 URI。
- 用户点"打开"→ 跳转 `/i/<id>/?token=<t>` → dsh 兑换 Set-Cookie（Path 改写后按实例隔离）→ 303 到 `./` 进入界面；WS `wss://host/i/<id>/api/remote.mux` 自动按 base 解析。
- 端口池 3081–4081 终身绑定（`DshPorts` 原逻辑），仅容器内回环，不对外发布。
- 实例停止后进程树消亡，反代无目标 → `/i/<id>/` 即刻 404；token 每进程随机且仅存内存，重启即换——运行时才存在攻击面，停止即归零。

## 实施阶段

**Phase 0 — 项目骨架**：新建 `HDSL-web` Gradle 单模块项目；拷贝领域源码并完成 6 类去 FX 化；拷贝 47 个领域测试；`shadowJar` 产出空跑可启动的骨架 jar。验证：编译零 javafx import、测试全绿。

**Phase 1 — 服务端核心**：Jetty 装配；`server.yaml` 加载与 env 覆盖；CertificateManager（PEM/P12/自签/热重载/HTTP→HTTPS 跳转）；AuthService + AuthFilter；登录 API；SPA 静态服务；健康检查。测试：JUnit + JDK HttpClient（登录流、自签 TLS trust、跳转）。

**Phase 2 — 实例生命周期 + 反代（可用竖切）**：
- TaskService（ExecutorService 包装阻塞 install，取消→`DshCommand.stopRunning`）；EventBus + WS 网关（日志尾部、状态机、安装进度）。
- REST：版本列表（`DshVersionManager.fetchReleases`）、实例 CRUD（`DshInstanceManager`）、异步安装、启动/停止、日志 tail+stream。
- 进程池改造（`DshProcessManager` 事件化）；启动参数注入 `--trusted-host/--no-open`。
- 反代 servlet：/i/* 三件套改写 + 斜杠归一化 + WS 中继（参考 dsh 仓库 `apps/web/tests/prefix-proxy.ts`）。
- SPA：登录页、实例列表、创建向导（选版本/隔离模式/端口）、实例详情。
- **启动进度 UX**：点"启动"后面板内嵌启动视图——进度条按阶段推进（安装 `DshInstallProgress.fraction` / 拉起进程 / 等待就绪行）+ 实时日志滚动；`READY` 事件到达后自动 `window.open('/i/<id>/?token=…')` 开新标签页，面板同步显示运行中（运行时长、实际端口、日志入口）。
- **停止 UX**：运行中实例的停止按钮 = 真杀进程树（宽限 5s 后 destroyForcibly + descendants）；停止后 `/i/<id>/` 即刻 404，面板回到"已停止"。
- 验证：端到端跑通"登录→创建→安装→启动（进度条+日志+自动开标签页）→dsh web 可操作→停止后 /i/ 404"。

**Phase 3 — 插件 + 账户**：插件市场（`DshPluginCatalog`）、安装/移除（`DshPluginInstaller` 驱动 `dsh plugin`）、build-script 审批交互（WS 待批提示）、本地 .tgz 上传（`DshLocalPlugins`）；账户 CRUD（12 家供应商目录）、`fetchModels` 验钥、实例级 active 账户（注入逻辑复用 `DshLauncher` 现成链路）。

**Phase 4 — 完整功能**：PackMarket 搜索/安装（`DshPackMarket`/`DshPackInstaller`）、整合包导入导出（`DshPackForge`/`DshModpacks`）、会话/工作区管理（`DshSessions`/`DshSessionPacks`）、技能（`DshSkills`）、体检页（`DshDoctor`）、ACP 控制台（`AcpTransport` 抽象 + stdio 传输 + WS 桥，JSON-RPC 行帧↔WS 文本消息）。

**Phase 5 — Docker 与交付**：多阶段 Dockerfile（node 阶段构建 SPA → gradle 阶段产出 `hdsl-web` shadowJar → **运行时基于 `debian:trixie-slim`**：apt 装 `openjdk-21-jre-headless` + 基础工具（curl/ca-certificates/tini），Node 22 LTS 按官方 nodejs.org tarball 装入 `/opt/node`（x64/arm64 双架构，对应 HDSL 的 NodeSource 策略），corepack 启用 pnpm；tini 处理信号，面板以 root 跑、agent 进程降权到 uid 1000（v0.6.44 起的身份划分，见 `docs/design-notes.md §7.5`），SIGTERM → shutdown hook `stopAll()`）；`docker-compose.yml` 便捷示例；README（quickstart、`java -jar` 裸跑说明、挂载点表、HTTPS 配置参考、证书上传步骤）。

## 测试策略

保留 47 个纯领域测试；新增：认证流、证书热重载、反代行为（stub 后端断言前缀剥离/Host 透传/Cookie Path 改写/WS 转发）、安装任务取消、**停止即消亡**（stop 后端口不可达 + `/i/` 404 + token 失效）、端到端冒烟（fake dsh 打印就绪行 → 自动开标签页 URL 拼装 → /i/<id>/ 访问）。

## 风险与对策

| 风险 | 对策 |
|---|---|
| Jetty WS 反代细节（`WebSocketProxy`/自写帧中继） | dsh 仓库有参考代理与 e2e 用例；stub 后端先行验证 |
| 与 HDSL 上游分叉、领域修复双份维护 | 拷贝时保留文件来源标记；修复按需手动同步；HDSL 桌面版不受影响 |
| dsh 版本行为差异（旧版本无 `--trusted-host`/`--no-open`） | 复用 HDSL 的 `acceptsNoOpen` 探测-缓存模式；trusted-host 不支持的老版本降级提示 |
| 容器内 npm/pnpm 网络 | 镜像内置 + `NPM_CONFIG_REGISTRY` 透传；pnpm store 挂卷提速 |

## 不做的事

不改 dsh 本体（deepseek-harness）；**HDSL 桌面仓库零改动**（只读拷贝源码）；首版不做多用户角色（单管理员，schema 预留）。
