# 从零重建 HDSL-web：给模型的任务提示词

> ## ⚠️ 只给"从零新建的项目"用
>
> **如果你是在已有的 HDSL-web 仓库里接手工作，不要按本文重建。** 代码已经在这里了，照本文动手
> 只会覆盖真实实现、丢掉这些修复的来龙去脉。那种情况请读：
>
> - [`CHANGELOG.md`](../CHANGELOG.md) 最近一节 —— 当前状态与最近一轮改动；
> - 最新的 `session-handoff-*.md` —— 接手时的现场、发版流程、踩过的坑；
> - [`plan.md`](plan.md) —— 原始设计；
> - 各模块文档（`web-shell.md`、`brands.md`、`octop.md`、`deployment.md`、`design-notes.md`、
>   `panel-agent.md`、`system-monitor.md`）与施工图 [`ui-spec.md`](ui-spec.md)。
>
> 本文的唯一用途：**仓库被清空、或换一个模型从空目录重写时**，把「要求」和「坑」一次性交代清楚。
> 判断标准很简单——你所在的目录里已经有 `src/`、`web/`、`plugins/` 了吗？有，就关掉本文。

> 这份文件本身就是一段提示词：把下面「任务」整段交给一个模型，它可以不知道本项目的历史，只凭它
> 从空仓库把项目建出来。写的是**要求与坑**，不是实现讲解——实现方式可以不同，坑不能踩第二遍。
> 维护约定：每修一个"只有跑起来才看得见"的问题，就往 §5 加一条，并写清「现象 / 根因 / 正确做法」。

---

## 任务

从**空仓库**开始，实现 **HDSL-web**：一个把 DeepSeek Harness（简称 dsh）以及若干第三方 AI 客户端
**以 web 面板的形式管起来**的自托管服务。它必须做到：

1. 一条命令产出**单个可执行 jar**（`java -jar hdsl-web.jar`），面板自带前端静态资源、自带两个第一方
   TUI 插件包，**离线可用**：目标机器上没有 npm、没有 registry 也能装。
2. 能把若干「实例」跑起来并反代它们：每个实例是一份独立的 home，只监听回环，浏览器经面板的
   `/i/<id>/` 访问；面板负责会话鉴权、`Host` 透传、Cookie 路径隔离、WebSocket 中继、页面挂载点改写。
3. 提供**网页终端**：在浏览器里跑实例自己的 TUI（PTY 在服务端，浏览器只是查看器），带开发者密码二次确认。
4. 提供一套**品牌目录**（dsh / Kimi Code / OpenCode / Claude Web / ZCode / Octop），每个品牌的安装、
   版本选择、启动、策略、终端入口都要能用。
5. 提供**开发者模式**：开发者密码 + 限时提权；可选的作用域守卫（反代 / 实例写操作 / 全部写操作）与
   **紧急停机**（E-Stop）。
6. 产出一个 Go 客户端（`client/hdsl-tui`），能在服务器上交互式挑实例、开终端，5 平台交叉编译。
7. 全套文档：部署、协议、设计笔记、安全模型、会话交接稿，中文，与实现一致。
8. **前端界面是桌面版的像素级复刻**（§2.10）：桌面与手机都能用；品牌实例在实例列表页**原地切换**，
   不开第三层页面；不可用功能一律变灰并写明原因。
9. **面板自己的 agent 票**（§2.11）：给跑在面板里的 agent 一把限权的钥匙（只读列表 / 启停 / 终端 /
   MC 控制台等），与管理员会话、开发者密码都分开；带「自杀闸」。

技术栈固定为：**Java 17**（`options.release = 17`，允许用更高 JDK 编译，但**不得使用 JDK 18+ API**）、
Gradle（shadow 插件打单 jar）、Jetty 12（ee10）、Gson、JNA（仅 PTY）；前端 React 18 + react-router 6 +
TypeScript（strict）+ Vite 6 + xterm 6 + `@material/material-color-utilities`（Monet 取色）、`es-check`
做语法下限检查；客户端 Go 1.24。**不引 UI 组件库、不引 CSS 框架**。许可证 GPL-3.0-or-later。

---

## 1. 硬约束（违反任何一条都算不合格）

- **单 jar**：`shadowJar` 产物包含 `web/` 构建出的静态资源与 `plugins/` 打出的 npm tarball；不含任何
  需要联网下载的运行时依赖。
- **插件零外部依赖**：`plugins/` 下两个第一方 TUI 插件（进程内 / ACP）必须 **不引任何 npm 依赖**
  （只用 dsh 的 peer 包与 node 内置模块），因为它们要能被离线安装进别人的实例。
- **不许引入新的三方库**除非确实无法避免；现有依赖只有上面列的这些。HTTP 客户端用 `java.net.http`，
  不引 OkHttp/Netty。
- **隐私**：仓库里不写真实域名、内网地址、宿主机路径、任何口令。文档举例一律用 `example.com` / `/data`。
- **不可用要变灰、不隐藏**：功能因配置/权限不可用时，按钮保留、置灰、并写明原因（例如"需要先启用
  HTTPS"）。隐藏按钮会让人以为没有这个功能。
- **不做假防线**：见 §5.9。面板内检查在同 uid 下都是剧场，别写。
- **错误码约定**：所有 API 错误体是 `{"error":"<code>: <人能照着做的一句话>"}`；前端与 Go 客户端按
  `<code>` 前缀分流。

---

## 2. 功能清单与验收点

### 2.1 服务与静态资源
- 一个 Jetty 服务，端口默认 3080；`/` 出 SPA（`<base href>` 由服务端按挂载点注入），`/api/*` REST，
  `/ws` 事件流，`/i/<id>/` 反代，`/ws/terminal/<ticket>` 终端。
- 支持子路径部署（`HDSL_BASE_PATH=/hdsl`）：所有前端资源与 API 路径都要跟着走；`<base href>`、
  运行时注入的 `window.__HDSL_BASE__`、WebSocket URL 都必须带挂载点。
- 首次启动无用户时提供 `POST /api/auth/setup`；`HDSL_ADMIN_PASSWORD` 可用于首启建号。

### 2.2 认证与提权
- 单管理员；PBKDF2WithHmacSHA256，per-user 盐，21 万次迭代；`users.json` 0600。
- 会话 = `HttpOnly` + `SameSite=Strict` cookie（HTTPS 下加 `Secure`），`Path` 跟挂载点走，
  滑动过期（默认 168h）；改密码/改用户名让同用户其它会话立即失效。
- `auth.disabled: true` 仅当绑定回环时允许。
- **开发者密码**（第二把钥匙，`developer.json` 0600）：会话级提权 30 分钟滑动、可延长一次到 3 小时、
  按 **session token** 记账（不是用户名）、10 分钟内 3 次错误锁 10 分钟；`POST /api/developer/elevate`
  响应里额外返回一个 32 字节 **bearer token**（内存、同 TTL、改密码即失效），供无法持 cookie 的脚本
  用 `Authorization: Bearer` 调接口。
- **出站 TLS 的信任窗口**（`/api/network/trust`）：面板自己连 npm/GitHub 时，机器上的 TLS 检查代理
  （杀毒、网关、FastGithub 一类）会用自己的根证书签名，`cacerts` 不认——浏览器打得开、面板报
  `PKIX path building failed`，而用户看到的是"版本列表是空的"。所以：失败要说人话（谁签的、为什么、
  两条出路），默认严格，放宽必须由人逐字回传一句确认语、1–1440 分钟、写进 `server.yaml`、可撤销、
  进审计。窗口内也不是直接跳过：先按 `cacerts`、再按机器自己的根证书库校验，两张都不认才接受并记日志。
  **默认选最新稳定版 Node 的那个坑（§5.15）和这个坑是邻居**：都在"装东西"这条路上。
- **开发者面只在 HTTPS 下可用**（读主连接器真实状态，尊重 `https.trust_forwarded_proto`）：
  明文 HTTP 下开发者页只给提示与「去 HTTPS 证书」，终端 403 `insecure_transport`，提权/守卫
  409/403 `tls_required`；`/i/*` 的拦截页说明"开发者模式不可用"。**不要**在 HTTP 上放开它。

### 2.3 反代 `/i/<id>/`
- 剥掉前缀、`Host` 原样透传（实例靠 `Origin == Host` 判同源）、hop-by-hop 头双向剥离、
  `Set-Cookie` 的 `Path=/` 改写成 `/i/<id>/`（多实例同名 cookie 不互相覆盖）。
- 页面改写：注入 `<base href>`、把根路径资源引用改到挂载点、给按 `location.pathname` 路由的客户端
  注入 router shim；**CSP 严格的实例不能用内联脚本**，脚本要以同源 `<script src>` 提供。
- WebSocket 中继：`/i/*` 的升级请求要在 WebSocket 层处理（servlet 过滤器看不到升级），
  逐字转发 cookie/headers，双向 relay，消息上限要调高（一条会话历史就能超过 64 KiB）。
- `GET /i/<id>`（无斜杠）301 到 `/i/<id>/`；未运行 502、不存在 404，错误体是 JSON。
- 只对 gzip/deflate 做解压改写：遇到 br/zstd 要么把上行 `Accept-Encoding` 归一成 `identity`，
  要么干脆不改写原样透传，**不要**把压缩字节当文本处理。

### 2.4 网页终端
- POSIX 用 `posix_spawn` 起 PTY（不经 shell），子进程自己的进程组；Windows 从 10 1809 起用
  `CreatePseudoConsole` 起同样的浏览器 PTY（更早的系统明确 409 拒绝，不回落桌面窗口）。
- 一次性 ticket（32 字节随机、30 秒 TTL、绑定会话与实例）桥接「POST 开终端」与「WS 挂载」；
  每实例至多一个终端；`setMaxIdleTimeout(0)`（用户发呆时不能被踢）。
- 退出顺序：关输入 → 有界 drain → 关 master → `SIGKILL` 进程组；面板退出时全关。
- 浏览器侧 xterm 6：CJK 等宽字体栈、整帧与光标都没变时**一个字节都不写**、组词期间缓冲输出
  （见 §5.4）。

### 2.5 第一方 TUI 插件（`plugins/`）
- 两个包，各是一个 npm 包（`package.json` + `files` 白名单），要能被 `dsh plugin add file:<tarball>`
  离线装进实例：**进程内版**叠在 dsh 之上（peer 依赖 harness 包，运行时动态 import）与 **ACP 版**
  （独立进程，零依赖）。两者的能力差异要在 UI 与文档里说清（ACP 没有命令运行时）。
- 版本号与后端里的清单（`DshTuiPlugins.ENTRIES`）保持一致；**内容变了但版本没变也要能被发现**——
  面板按安装包摘要判断"有新版本"，记录在 `<实例 home>/.hdsl-web/first-party-installs.json`。
- 半安装态（目录在、文件不在）要显示"安装未完成"并给「重新安装」，不能显示成已安装。
- 终端内的交互约定：审批对话框四选项（数字 1-4）、`/` 命令菜单画在输入框**下方**、Tab 补全、
  `ctrl+j` 换行、`ctrl+s` 插话、`shift+tab` 计划模式、状态栏（模型 · 档位 · 计划 · cwd · 会话 · 上下文）。

### 2.6 品牌目录
- **五个品牌**（Kimi Code、OpenCode、Claude Web、Octop、Hermes）加 **dsh** 与 **ZCode** 两个独立实例
  家族；安装源分四种：npm（Kimi、OpenCode）、GitHub Release（Octop）、GitHub 源码构建（Claude Web、
  Hermes）、自建分发（ZCode）。dsh 自己也是 GitHub Release 安装。
- Claude Web 有两个特殊处：官方内核在**不受支持地区会被 Anthropic 直接 403**（要给出人话提示并允许
  改走社区引擎），以及实例级 `ANTHROPIC_BASE_URL` 覆盖（网关/中转，界面里能改）。
  每个品牌的：版本目录、安装（含进度与日志）、启动/停止、端口分配（回环）、策略、终端入口、页面反代。
- **安装目录必须有完成标记**：安装成功后写 marker（Octop 是 `octop-install.json`），只有带 marker
  的目录才算"已安装"；`*.previous` 这类暂存名不是版本（§5.7）。
- **运行中的版本不能重装**（任何安装路径都要 409）。
- Octop 额外两条：portable 包 + 本地源码编译（Linux）；前端要注入挂载点补丁（见 `docs/octop.md`）。
- 品牌策略（开发者模式）：单实例（其余灰掉、数据保留，**停止仍允许**）、固定反代 `/i/octop/`
  （开启后「打开」按钮与 open 接口都要返回这个固定路径）、源码编译开关。策略存
  `<data>/brands/policy.json`。
- **图标**：只用许可清晰的标志文件（本项目自己的 `NOTICE` 里有逐个来源）。**商标不随代码许可授予**：
  上游是 MIT 不等于可以用它的 logo；没有明确授权就用中性图标 + 文字标签。

### 2.7 开发者守卫与紧急停机
- 守卫 `developer_guard.scope = off | proxy | instance | all`（默认 off，存 `server.yaml`，
  环境变量 `HDSL_DEVELOPER_GUARD`）。逐级包含：`proxy` 管 `/i/*`（HTTP + WS）；`instance` 再加实例族
  的写操作（`/api/instances|brands|zcode|packs|settings|node` 的 POST/PUT/PATCH/DELETE）；
  `all` 再加实例族之外的写操作。
- **三档都不拦读**：前端要靠 `/api/*` 的 GET 渲染，拦了只会让面板看起来"刚装好、什么都没有"。
- 守卫是**门禁不是断路器**：只挡新请求，已加载的页面、已附着的终端、正在跑的 agent 都不动。
- 守卫挡住操作时，前端要**弹开发者密码框**并重试一次，而不是甩一句错误；`/i/*` 的拦截页也要带密码表单
  （传输策略拒绝时不给表单，因为填了也没用）。
- **紧急停机**：三次确认（先 `GET /api/developer/estop/preview` 列出会杀掉哪些实例/终端）→
  `POST .../engage` 先翻转状态、再关所有终端、再强杀所有实例的整棵进程树（先收集 descendants，再
  `destroyForcibly`，**不跑用户配置的 post-exit 脚本**）；之后 `/i/*`、实例写操作、终端一律 503，
  登录/健康检查/开发者页/只读列表与日志/`/ws` 照常。冷却 1 小时，未到点 409 `estop_cooldown`。
  **状态只在内存**：重启进程/容器即解除（唯一的物理恢复方式）。重复按下不重置计时。

### 2.8 Go 客户端（`client/hdsl-tui`）
- 登录（保存会话、401 自动清）、开发者提权、实例列表、终端（WS + 本地渲染输入框）、
  错误码 → 可执行文案的映射；5 平台交叉编译，CI 把二进制挂到 Release。

### 2.9 配置与部署
- `server.yaml` 全部键可选，非法值启动失败并指出字段；环境变量优先级高于文件。至少包含：
  `bind_host`、`port`、`public_base_url`、`https.*`（cert/key/pkcs12/redirect/trust_forwarded_proto）、
  `auth.*`、`web_shell.enabled`、`developer_guard.scope`、`zcode.*`。
- HTTPS：支持 PEM / PKCS#12 / 自签生成 / 运行时热切换；`redirect_http`、`redirect_same_port`；
  反代终结 TLS 时用 `https.trust_forwarded_proto` 显式打开信任。
- Dockerfile：多阶段（node 构建 SPA → gradle 打 jar → runtime），tini 转信号，健康检查打 `/api/health`。
  **runtime 阶段的底座是构建参数**（`HDSL_BASE`，默认 `debian:trixie-slim`，也可 Fedora / openSUSE），
  包管理器按 `/etc/os-release` 分派；`tini` 软链到 `/usr/local/bin/tini`（openSUSE 在 `/usr/sbin`）；
  架构用 `uname -m` 判断而不是 `dpkg`；ffmpeg 的 x264/x265 断言由 `HDSL_ENCODERS=required|optional`
  控制（非 Debian 底座只有专利-free 构建，必须只告警）。细节见 `docs/deployment.md §4.1`。
- **容器里的身份是两段**：面板 root（持有 `auth/`、`certs/`、`logs/`、`server.yaml`），agent 进程用
  `setpriv` 降到 uid 1000；入口负责属主校正与 `umask 002`。裸部署没有这一层。见 §5.9。
- 仓库自带 `docker-install.sh`（原地构建向导：数据/工作区/MC 目录、端口、镜像源、`--base`），
  它**不整卷 chown**（那会把秘密交回去），属主交给容器入口。
- 裸部署（systemd）文档：`HOME` 与 `HDSL_DATA` 必须指向服务用户可写的目录。

### 2.10 前端界面（SPA）

界面不是"够用的管理后台"，是**桌面版 HDSL 的像素级复刻**：同一套装饰边框、左侧导航、内页骨架、
配色与字号。施工图纸是 [`ui-spec.md`](ui-spec.md)，颜色 / 间距 / 字号以它为准。

- **颜色只走变量**：`root.css` 是唯一色源（Monet 从壁纸取色 + 深浅色），组件里**不许写死色值**。
- **页面清单**（每个都要有加载 / 空 / 错误三态）：主页（当前实例 + 启动面板）、实例列表、实例详情
  （启动 / 浏览 / 管理三组）、品牌实例层、下载页、设置页、账户页、新建实例向导、性能页。
- **品牌层是实例列表页的一个视图，不是第三层页面**：点品牌**原地**把 `/instances` 换成品牌卡片，
  不新增路由层级；只有进入某个实例详情才用路由。手机端靠这条活命，三层返回没人受得了。
- **不可用要变灰，不许隐藏**（§1 已列为硬约束）：置灰的按钮旁边写「为什么 + 去哪儿解决」，例如
  「需要 HTTPS → 去证书页」。开发者面在明文 HTTP 下整页只给提示与「去 HTTPS」入口。
- **危险动作的视觉要分开**：停止实例是**红色药丸**按钮；删除、紧急停机各要二次 / 三次确认；终端的
  「关闭」与「停止实例」是两个动作，文案不许混。
- **终端**（xterm 6）：CJK 等宽字体栈；整帧未变不写字节、组词期间缓冲（§5.4）；工具栏显示「已接收
  N KB」；WS 断开要有可见原因与重连；后端 409 的原文要展示，不许留白屏。
- **手机版式**（两处判定必须逐字一致：`styles/mobile.css` 与 `hooks.ts`）：
  `(max-width: 760px), (max-height: 520px)` 是布局断点，`(hover: none) and (pointer: coarse)` 是触摸
  优化——两者独立。侧栏变抽屉、子侧栏变横向标签条、启动面板变贴底操作栏、`100dvh` +
  `env(safe-area-inset-*)`、可点区域 44px、输入框 16px（iOS 聚焦不缩放）。桌面端样式**零变化**。
- **构建**：`base` 必须是 `"/"`（相对 base 会让 `/instances/x` 刷新白屏）；`minify` 必须为 `false`
  （§5.4.2）；`npm run compat` 跑 `es-check` ES2020，防止写出旧浏览器打不开的语法。
- **图标与法务**：品牌 logo 只在「许可证 + 商标」都允许时使用，来源逐个记进 `NOTICE`；上游是 MIT
  **不等于**可以用它的 logo，没有明确授权就用中性图形 + 文字标签。
- **文案**：中文优先、动词开头、短句；API 错误统一 `{"error":"<code>: <照着做的一句话>"}`，前端按
  `<code>` 前缀分流（如 `tls_required` → 弹 HTTPS 提示），不要把后端英文原话直接甩给用户。
- **验收**：改版前后各留一张截图（`docs/screenshots/`）；真机至少过一遍 §6 的界面四条。

### 2.11 面板 agent 的票（`panel-agent`）

- 目的：让跑在面板里的 agent 能查实例、必要时启停，而**不把管理员会话**交给它。
- 两把锁：agent 密码换票；票带 scope（只读列表 / 启停 / 开终端 / MC 控制台 / 聊天读写信 / 搜索等**十二类**），scope 在
  **服务端**定，客户端传什么都不算数。agent 密码只在需要写的那几档有效。
- 票默认只认**本机回环**来源；非回环要显式打开，并要求已登录会话。
- **自杀闸**：票不许对「自己所在的那个实例」做停 / 删 / 开终端；判定靠**内核身份推导**
  （socket inode → 持有进程 → 父链），不靠 agent 自报。dsh / 品牌 / ZCode 三个家族与 launch 路径
  都要覆盖——漏一条就是「agent 把面板里自己在跑的实例停掉」。
- 与开发者密码分开：开发者密码是**人**的第二把钥匙（§2.2），票是 agent 的限权钥匙，不要互相替代。
- 审计里每条票操作都要有来源与 scope。

---

## 3. 对外契约（最小集，名字固定）

| 面 | 契约 |
|---|---|
| 健康检查 | `GET /api/health` → `{"status":"ok","version":"x.y.z"}` |
| 登录 | `POST /api/auth/login`、`GET /api/auth/status`、`POST /api/auth/setup`（仅无用户） |
| 实例 | `/api/instances/*`（dsh）、`/api/brands/<brand>/instances/*`（其它品牌） |
| 反代 | `/i/<id>/`、`/i/octop/`（可选固定别名） |
| 终端 | `POST .../terminal` → `{mode,protocol,ticket,ws}`，`WSS /ws/terminal/<ticket>` |
| 开发者 | `GET /api/developer/status`（含 `https`、`guardScope`、`estop`）、`POST .../elevate`（返回 `token`）、`.../extend`、`.../revoke`、`.../web-shell`、`.../guard`、`.../estop/preview|engage|release` |
| 事件 | `WS /ws`：实例状态、任务进度、日志行、外部实例变化 |

审计：`<data>/logs/audit-*.jsonl`，每行一条 JSON，含时间、channel、动作、对象、结果、操作者、来源地址。
里面会有用户名与地址——**别把它读进 UI**，它对 agent 也不可写（`0600`，容器里属主是 root）。

---

## 4. 构建与验证（缺一不可）

```bash
# 后端 + 两个插件（用 flock 串行，避免并发 Gradle 争锁）
flock /tmp/hdsl-gradle.lock ./gradlew --no-daemon check

# 前端三连
cd web && npm run typecheck && npm run build && npm run compat

# 插件（零依赖 + 行为测试）
node plugins/dsh-inproc-tui/scripts/check.mjs && node plugins/dsh-acp-tui/scripts/check.mjs
(cd plugins/dsh-inproc-tui && node --test) && (cd plugins/dsh-acp-tui && node --test)

# Go 客户端
cd client/hdsl-tui && gofmt -l . | grep -v '^vendor/' ; go vet ./... ; go test ./...

# 单 jar
flock /tmp/hdsl-gradle.lock ./gradlew --no-daemon shadowJar -PreleaseVersion=<版本>
unzip -l build/libs/hdsl-web-<版本>.jar | grep -c first-party-plugins   # 必须是 2 个 tgz + index.json

# 入口脚本：它是镜像里 root 阶段跑的那段 shell（Dockerfile 里 printf 的那串单引号），
# 构建前先在本地渲染出来做语法检查——否则要等构建到那一层才发现少一个引号（§5.20）。
python3 - <<'PY' > /tmp/entrypoint.sh
import re
src = open("Dockerfile", encoding="utf-8").read()
end = src.index("&& chmod 0755 /usr/local/bin/hdsl-entrypoint")
start = src.rindex("RUN printf '%s\\n' \\", 0, end)
print("\n".join(re.findall(r"^\s*'((?:[^'\\]|\\.)*)'\s*\\?$", src[start:end], re.M)))
PY
sh -n /tmp/entrypoint.sh && echo "entrypoint: 语法 OK, $(wc -l < /tmp/entrypoint.sh) 行"
```

**然后真的启动一次**（这条最重要，静态检查看不出顺序类问题）：

```bash
HDSL_DATA=/tmp/panel HDSL_PORT=<随机空闲端口> HDSL_BIND_HOST=127.0.0.1 \
  java -jar build/libs/hdsl-web-<版本>.jar     # 不加任何 -D 参数
# 断言：/api/health 200；带会话请求 /i/whatever/ 是 404 而不是 500；启动日志里没有
# "restricted header name" / IllegalStateException
```

**接口级冒烟**（十几秒，`scripts/api-smoke.py`）：自己起一个临时面板（临时数据目录、随机端口、
HTTP），把"一个人会点到的路径"念一遍——`/api/node` 的字段、品牌版本（或它为什么为空）、doctor、
出站证书的信任窗口、ZCode 工作区、dsh 半安装的拒绝、终端四道闸、静态资源。**发版前必须 39/39**。
它不点浏览器、不装包，所以能跑在任何 CI 上。

**容器验收**（`scripts/verify-privilege-split.sh`）：以 uid 1000 跑，断言 `auth/`、`logs/` 读写
都被拒，而实例目录与 pnpm store 可写。**镜像能构建不等于隔离生效**——这条只有真容器能回答。

测试纪律：**先红后绿**。每条"修了什么"的用例，先把实现改回去确认它会红，再改回来。断言要写清
"只有这个修复才会给的理由"，不要写"能跑就算过"。
构建产物目录若出现 `Truncated class file` / 成批 `NoClassDefFoundError`，先 `rm -rf build/classes`
重编：半残的增量编译会给出成片假失败，别去改代码。

---

## 5. 已知的坑（照抄，别重新发现一遍）

### 5.1 `Host` 透传的属性只能在第一个 HttpClient 之前设
- **现象**：一切正常启动，但所有 `/i/*` 回 `500 java.lang.IllegalArgumentException: restricted header name: "Host"`；
  加上 `-Djdk.httpclient.allowRestrictedHeaders=host` 立刻恢复。
- **根因**：JDK 在**第一次构造 `HttpClient`** 时就把受限头允许列表定死；之后 `System.setProperty` 无效。
  只要启动路径上先碰到任何带 `static HttpClient` 的类（例如某个安装器），反代就永远发不出 `Host`。
- **做法**：`main` 的**第一行**设属性；装配完最后一个 servlet 后做一次**真探针**（构造一个带 `Host`
  的请求）自检，失败即启动失败并提示 `-D` 补救。测试要用**独立 JVM** 跑（测试 JVM 往往早已设好属性，
  进程内测试会给出假安全感）。

### 5.2 Dockerfile 必须 `COPY plugins`，并断言 jar 里有插件
- **现象**：镜像里的 jar 一个插件都没有，面板"假装"能装终端，点开才失败；CI 全绿。
- **根因**：构建阶段只 `COPY src`，而 `shadowJar` 依赖的打包任务要从 `plugins/` 取输入。
- **做法**：构建阶段加 `COPY plugins ./plugins`，并在打完后断言
  `jar tf … | grep -c '^first-party-plugins/.*\.tgz$' >= 1`，否则构建失败。

### 5.3 同一版本号、不同内容
- **现象**：改了插件代码没升版本，用户那边永远显示"已安装"，新行为永远不生效。
- **做法**：安装成功时记录**安装包摘要**（`first-party-installs.json`），列表按摘要比；没有记录也算
  "有新版本"，给「更新」按钮。升级插件时不必强求改版本号。

### 5.4 xterm 的输入法与重绘
- **现象**：中文候选框突然跑到别处；按错一个键时前面输入的一整段又被复制进输入框；"输入法被吞"。
- **根因**：xterm 6 在 composition 期间每次写屏都会重定位候选框；隐藏 textarea 还残留大写字母与空格，
  229 键会把整段旧值重发（xtermjs/xterm.js#6078）。此外每秒重绘本身就会打断输入。
- **做法**：整帧与光标都没变时**不写任何字节**；组词期间缓冲 PTY 输出、`compositionend` 后再刷；
  keyup/compositionend 之后延迟清理 textarea（`screenReaderMode` 除外）。字体栈要有 CJK 等宽字体。

### 5.4.1 测试里别用「先声明 holder，再 submit」的写法
- **现象**：`TaskApiTest.cancelMarksTheTaskAndTheWorkStops` 在 JDK 21 的 CI 上偶发失败
  （`assertTrue(tasks.cancel(...))` 得到 false），本地反复跑却是绿的。
- **根因**：`TaskService.submit` 可能在**返回句柄之前**就把 work 跑起来了；work 里读
  `holder[0].isCancelled()` 时它还是 null → NPE → 任务变失败 → cancel 返回 false。
- **做法**：work 需要句柄就用 `TaskWork` 重载（`submit(kind, id, own -> …)`，句柄直接传进来），
  不要靠调用方事后赋值；测试里的「等它跑起来」一律给 30 秒（CI 机器比本机慢得多）。

### 5.4.2 Vite 的 `minify` 会打坏 xterm 6

- **现象**：WS 已连上、后端日志显示字节已发，浏览器里终端**从第一屏就是空白**；控制台一条
  `ReferenceError: i is not defined`，堆栈落在 xterm 的 `requestMode`。
- **根因**：xterm 6 的 `requestMode` 用了 `return f=…,p=…; var f,p` 这种写法，esbuild 的二次压缩会把
  它改坏；异常发生在 xterm 的写队列里，队列随后**整体停摆**——之后所有输出都不再解析。最初的触发者
  是 opencode：它启动时连发 6 个 DECRQM 查询（`\x1b[?2026$p` 一类）。
- **做法**：`vite.config.ts` 里 `build.minify: false`（体积只多约 9 KB gzip）；升级 xterm 或换打包器
  前，先做一次最小复现：发一屏文本，再发一个 `\x1b[?2026$p`，看那屏文本还在不在。

### 5.5 TUI 自己的输入细节
- composer 行首是 `│ ` + `❯ `（四格），光标列按 3 格算会压住最后两个字；Tab 需要在按键解码表里有
  `0x09`，否则"补全坏了"其实是键从来没到应用；`ctrl+j` 是换行，别解码成 enter。

### 5.6 会话列表（`/sessions`）
- dsh 的 `sessionPersistence` header **没有 `updatedAt`**：排序只能靠日志文件 mtime。
- 日志布局 `<root>/<workspace>/<sessionId>/session[.vN].jsonl[.zstd]`；一个目录可能同时有多代，
  取**最新一代**（取最大 mtime，别按 `readdir` 顺序取最后一个）。
- 根的顺序：`DSH_TUI_SESSION_ROOT` → `$DSH_HOME/sessions` → `~/.dsh/sessions` → `~/.dsh-tui/sessions`。
- 读事件用 `open(id,'read')` + `read(offset, length)`，offset/length 是**事件条数**；短读不是 EOF，
  空切片才算读到尾。
- 首条人类提示：`user/message` 且 `source.kind === 'user'`；`source` 缺失/null 的老日志也算人类；
  提示也可能先以 `agent/inbox/spliced` 落盘；`content` 可能是数组也可能直接是字符串。
- **纪律**：读不出、扫到上限没找到 → **保留**并标「标题未读出」；只有确认读到日志结尾才过滤。

### 5.7 品牌安装的半安装状态
- **现象**：源码构建被中断后面板把 `.previous` 当成已安装版本，`default` 指向一个永远装不上的名字。
- **做法**：枚举只认带 marker 的目录、忽略 `*.previous`；启动时对账（半成品改名 `.broken-<ts>`，
  把**带 marker 的** `.previous` 挪回原名）；`.broken-*` 只留最新一份；运行中的版本禁止重装。

### 5.7.1 品牌安装的下载源：镜像怎么配、uv 怎么取
- **现象**：源码编译卡在 `Downloading uv from PyPI https://files.pythonhosted.org/...`，几十 KB/s；
  Octop 的 portable 包（约 260 MB）走 GitHub Releases 直接超时。同一台机器上阿里云 PyPI 有
  6.6 MB/s、南大 `github-release` 镜像 6.45 MB/s。
- **根因**：默认全部指向境外源，而且 `ensureUv()` 原来硬编码 `pypi.org` 的 **JSON API**
  ——大多国内镜像不提供这个 API，只提供 simple 索引页。
- **做法**：四类源分开配（设置 → 品牌实例）：PyPI 索引（`UV_DEFAULT_INDEX` + `PIP_INDEX_URL`）、
  CPython 镜像（`UV_PYTHON_INSTALL_MIRROR`，形状 `<base>/<tag>/<file>`）、版本源码归档
  （`CODELOAD_BASE`，形状 `<base>/<slug>/zip/refs/tags/<tag>`；不要用 git 传输，它在这里会被切断）、
  每品牌 release base（如 `OCTOP_RELEASE_BASE`）。取 uv 要**读镜像自己的 `<index>/uv/` simple 页并
  解析 href**（注意 `#sha256=` 片段与 `../../packages/…` 相对链接），失败再回落 PyPI JSON 与 GitHub。
  优先级固定为 **设置 > 环境变量 > 上游默认**，别反过来，否则升级会踩掉运维配的镜像。

### 5.7.2 设置字段必须同时加进 `Snapshot`
- **现象**：接口返回保存成功，`launcher-settings.json` 里却找不到新字段，重启即丢。
- **根因**：`SettingsManager` 落盘不是把对象直接序列化，而是 `Snapshot.of(settings())` 的**显式白名单**；
  只加 `LauncherSettings` 字段（哪怕带 `@SerializedName`）不会出现在快照里。
- **做法**：新增设置要在三处一起改——字段 + `Snapshot.of()` 抄值 + `Snapshot.applyTo()` 回填；
  测试里直接断言 `launcher-settings.json` 的文本包含该键。

### 5.7.3 源码构建 dashboard 要用会做类型检查的脚本
- **现象**：安装成功、实例能启动，用户点进页面却是白屏或整片破图；控制台里缺 `Link`、某张图走的是
  根绝对路径。
- **根因**：上游 `dashboard/package.json` 有两个构建脚本——`build`（`tsc -b && vite build`）与
  `build:docker`（只有 `vite build`，给容器省时间）。面板源码构建用了后者，`tsconfig` 里的
  `strict` + `noUnusedLocals` + `noUnusedParameters` 全被跳过，补丁里少一个 import、多一个未使用
  变量都不会报错。
- **做法**：跑 `npm run build`。多花 30–60 秒，换来「缺 import / 类型不匹配 / 未使用变量」在安装期
  就失败。前提是 devDependencies 装了：`npm ci` 别带 `--omit=dev`，宿主别设 `NODE_ENV=production`，
  否则 `typescript` 不在（已实测：干净 tag 树 + 补丁，`npm ci` 8 秒、`tsc + vite build` 41 秒）。

### 5.7.4 挂载点逃逸：一次查完，并用闸门挡住新增
- **现象**：`<base href>` 明明注入了，还是有整片破图；点「没有可用模型」跳回面板首页；聊天/终端
  的 WebSocket 连不上（连到的是面板自己的 `/api`）；登录后回到面板外壳而不是实例。
- **根因**：`<base>` 只影响**相对**路径。上游到处都是根绝对的写法——`<a href="/admin/models">`、
  `window.location.replace("/login"｜"/setup")`、`const base = ws://host/api/...`。
  静态资源命中面板的 SPA 外壳（200 + `text/html`，浏览器按图片解码失败）；socket 撞上不升级的
  `/api`。
- **做法**（三类，改动只在补丁里）：
  1. 硬跳转 → `publicUrl("chat")`；
  2. 裸链接 → `<Link to>`（router basename 生效）；
  3. 手拼 socket → `getWsUrl("/…")`（`api/config.ts` 已经处理 basename 与 ws/wss）。
- **闸门**：`integrations/octop/check-mount-escapes.sh` 对打完补丁的 `dashboard/src` 扫六种模式
  （硬跳转、`href="/`、`src="/`、`location.host}/api`、`fetch("/api`、`window.open("/`），
  `build-dashboard.sh` 里先跑它再 `npm ci`，命中就让打包失败。新增的后端响应 URL、静态资源、
  新 socket 都不要再手工拼 `/api`。

### 5.8 子进程与超时
- **现象**：`octop init` 卡住一小时，面板永远起不来；设置了 120 秒超时却从不触发。
- **根因**：先 `readAllBytes()` 再 `waitFor(timeout)`，读永不返回。
- **做法**：独立线程**有界**排水（例如保留最后 64 KiB）+ `waitFor(timeout)`；超时要
  `descendants().forEach(destroyForcibly)` 再杀父进程，并把已捕获输出写进错误信息。

### 5.9 同 uid 不是安全边界；两段身份才是

**先记住这条**：agent 与面板同 uid 时，它能直接改写 `users.json` / `developer.json` / 证书私钥
（0600 只挡别的用户），也能读 `/proc/<pid>/environ`。校验和、签名、内存基线告警**全都挡不住**，
不要做这些假防线。

因此容器里分两段（`util/platform/Privileges.java`）：

- 面板是 PID 1、uid 0，`auth/`、`certs/`、`logs/`、`server.yaml` 归 root `0700/0600`；
- 每个 agent 进程派生时前置 `setpriv --reuid=<agent> --regid=<agent> --init-groups --`，
  跑成镜像里的 `hdsl`。**所有**派生点都要覆盖：dsh 主进程、实例内命令（npm/pnpm）、ACP、
  品牌实例、ZCode 实例、Octop 的口令操作、MC 的 pipe 控制台、以及 PTY 终端
  （`posix_spawn` 那条也要，`setpriv` 是 exec 原地替换，进程组与 PTY 会话都不变）。
- 实例子目录 `1000:1000 2775`（setgid）+ 入口 `umask 002`，所以**面板（root）写进实例目录的文件
  仍是 `root:hdsl 0664`，agent 照样能改**。不做这一步，root 会把 agent 自己的目录写死，
  表现是"实例装不了包 / 存档写不进去"。**但这条在 `instances/<id>` 与 `home` 这一层不成立**：
  `Privileges#prepareAgentDirectory` 用 `0775`，`chmod` 会清掉 setgid 位，home 内部从第一次启动起
  就不在 setgid 链上。所以「面板往 home 里写」必须显式交接属主（§5.24），不能指望挂载参数。
- **只在容器里降权**：宿主的 uid 1000 可能是另一个人的账户，塞过去是权限 bug。裸部署不降权，
  以 root 启动也不拦（拒绝只会换来"那我不用了"），但横幅要写明"你派生的每个 agent 也是 root"。
- agent uid 是**构建参数**（`HDSL_AGENT_UID`，默认 1000）：见 §5.20 的 ACL 存储。

**ACL 管理的存储会让这一层形同虚设**（QNAP 的 trimacl 是典型）：共享属主被当管理员放行，
uid 1000 能穿过 `0700`；`chmod`/`chown` 照样"成功"、`stat` 照样显示 `700`，只有真访问才暴露。
**镜像里绕不过去**——所以入口要自检（以 agent 身份试写一次 `auth/`），写得进去就打
`WARNING: this filesystem does not enforce mode bits for uid <n>`。**假的隔离比没有隔离更糟**：
让用户以为有防护，比告诉他"这台机器上没有"危险得多。

### 5.10 反代的其它细则
- 升级请求（WebSocket）不走 servlet 过滤器：`/i/*` 的守卫与停机检查必须在升级层再做一次。
- 实例的 `Set-Cookie: … Path=/` 必须改写成挂载点，否则两个实例的同名 cookie 互相覆盖。
- 实例响应若是 br/zstd，改写页面会把字节弄坏：见 §2.3。

### 5.11 Windows
- 没有 POSIX PTY：实例终端走 ConPTY（Win10 1809 / build 17763 起），进程树靠 Job Object。
  低于门槛的机器 409 并带 build 号，**不要**回落桌面窗口。
- ConPTY 的子进程必须用 `STARTF_USESTDHANDLES` + 三个 NULL 启动，否则 `CreateProcess` 会把
  面板自己的 stdout 拷给 agent：画面写进面板日志，浏览器里一片空白，而 `exit` 码一切正常
  （这条只有真机跑得出来，Linux 上编译一次都没有）。
- 没有 0600 语义：凭据文件可能被同机其它账户读到，文档里写清楚。
- 路径与 `chmod` 相关代码要 `try { … } catch (UnsupportedOperationException) {}`，别让面板在这些地方崩。

### 5.12 Java 17 兼容
`options.release = 17` 时不能用 `SequencedCollection`、`List.getFirst/getLast` 等 JDK 18+ API；
`ArrayDeque` 的同名方法会掩盖这个错误，写代码时注意声明类型。`HttpClient.close()` 要到 21 才有，
需要反射或交给 GC。

### 5.13 CI 与发布
- 三条工作流：`build`（JDK 17/21，push main 触发）、`client`（tag 触发，交叉编译并上传 Release 资产）、
  `docker`（tag 触发，多架构推 ghcr）。
- `docker` 有一道"只有最新版本标签才移动 `latest`"的闸门：tag 刚推上去时 tags API 可能还没可见，
  闸门会判成非最新 → `latest` 不动。**重跑一次该 tag 的 workflow** 即可，不必改代码。
- **偶发红先看是不是超时**：`ZcodeBuilderTest` 的用例带 120 秒上限、且共享静态构建状态——CI 慢下来
  超时一次，后面三条会连带报「a ZCode build is already running」。先 `gh run rerun <id> --failed`
  确认，再动代码；根治要把「超时后复位状态」做进测试隔离。
- `github.com:443` 会间歇不可达（`GnuTLS recv error` / 连接超时），而 `api.github.com` 正常：
  推送失败先重试；实在不行可以用 Git API 重放提交，然后 `git fetch` 把本地对齐（sha 会不同，内容一致）。
- 发布流程：`web-shell` → **`--no-ff` 合 `main`** → `shadowJar -PreleaseVersion=<版本>` →
  `sha256sum` 写 `SHA256SUMS.txt` → `git tag -a` → 推 `main`/`web-shell`/tag → `gh release create`
  （正文写给用户看，不抄 CHANGELOG）→ 核三条 CI 与 ghcr 镜像标签。
- 合并必须留合并点：**不要用 `--ff-only` 发布**，否则 `main` 上是一条直线，看不出哪批提交属于
  哪个版本。换机器后先设 `git config merge.ff false` 与 `git config pull.ff only`（`pull` 仍只
  允许快进，避免日常拉取意外产生合并提交），发布时仍显式写 `--no-ff`。


### 5.14 `user.home` 不等于 `HOME`（root 跑面板时尤其）
- **现象**：容器里升级到"面板 root"之后，默认工作区变成 `/root/workspace`，PTY 起来 44 ms 就
  `exit 1`，WS 报 "no running terminal"；pnpm 的全局配置写进 `/root/.config`，设置页改的 registry
  agent 根本读不到。
- **根因**：JVM 的 `user.home` 来自 **passwd**，不是环境变量。面板以 root 跑时它答 `/root`，
  而 agent（uid 1000）看到的是 `HOME=/home/hdsl`，`/root` 是 0700，连 `cd` 都进不去。
- **做法**：所有"用户家目录"的默认值统一走一个 `UserHome.directory()`——**`HOME` 优先**，
  没有才回落 `user.home`（Windows 上没有 HOME，回落正好）。要覆盖的地方不止 workspace：
  pnpm 的全局配置文件、整合包安装给的默认工作区、以及任何拼 `~/.xxx` 的地方。

### 5.15 托管 Node"装得上"不等于"跑得起来"
- **现象**：一键安装 Node 的任务显示 done，`/api/node` 显示 `suitable: true`，但 node 一执行就是
  `error while loading shared libraries: libatomic.so.1`，pnpm 安装 `exit 127`，日志里只有一行 warning。
- **根因**：只有一个"文件在不在"的检查，而 Node 26 起的官方二进制动态链接 `libatomic.so.1`，
  `debian:*-slim`、`ubuntu:*-minimal` 这类镜像不带它。
- **做法**：解包后**真跑一次** `node --version`（限时、输出有界），跑不起来就删掉那份运行时、
  任务 failed、把 loader 的原话带进错误；`suitable` 也要表示"能跑"而不只是"版本在范围内"
  （结果按目录 + 二进制 mtime 缓存，别每次轮询都起进程）。镜像侧顺手装 `libatomic1`。
- 推论：凡是"下载一个东西然后执行它"的路径，都要有一步"执行一下试试"，而不是只校验文件存在。

### 5.16 Gson 默认会丢掉 `null` 成员
- **现象**：没有系统 Node 的机器上，实例页整片白屏，控制台报 `Cannot read properties of undefined
  (reading 'pnpm')`。带 node 的开发机永远复现不了。
- **根因**：接口的 `system` 字段是 `null`，Gson 的默认 writer **把 null 成员整个删掉**；前端先判
  `system !== null`，拿到的是 `undefined`，于是穿过判断去读 `.pnpm`。
- **做法**：允许多个字段为 null 的接口统一用 `writePreservingNulls`（账户/实例/目录那些早就这么做了）；
  前端同时把"缺键"当 null/空数组处理。**回归测试要在"没有这个工具"的机器上才红**——所以要么给
  接口留一个可注入的探测点，要么在测试里直接调 payload 构造函数造 null。

### 5.17 TUI 插件的版本上界必须写插件的真实 peer
- **现象**：在最新的 dsh 上装面板推荐的 TUI 插件，被 dsh 以 `incompatible with dsh 0.2.1-rc.5`
  拒绝并回滚；用户看到的是"最新版没有终端可用"。
- **根因**：面板的兼容范围写成了 `<0.2.2-0`（"0.2.2 之前、包含预发布"），于是 `0.2.1-rc.4/rc.5`
  也算"适配"；而插件自己的 `peerDependencies` 只枚举到 `0.2.1-rc.3`。
- **做法**：上界照抄插件 peer 里的最后一个（`<0.2.1-rc.4`），不匹配时走**面板侧的解释**（给 ACP
  备选），别把 dsh 的原话甩给用户。要让新 rc 有终端，得发布一个对那条线真测过的插件版本——
  **改个范围不算验证**。

### 5.18 启动窗口里不能报 `STOPPED`
- **现象**：`POST /launch` 回 202，紧接着 `GET /api/instances/<id>` 读到 `STOPPED`，再下一次是
  `RUNNING`；前端轮询会闪一下"已停止"，自动化脚本会误判。
- **根因**：启动在任务池里跑（要先做两次探测），进程注册之前 `stateOf` 没有东西可报，落到兜底
  `return "STOPPED"`。
- **做法**：接受启动请求时就打一个"launch 在途"标记（在**排队之前**，因为 202 已经发出去了），
  窗口内回答 `STARTING`；进程注册后清掉。

### 5.19 Dockerfile 的 `ARG` 只有在**首个** `FROM` 之前才对 `FROM` 生效
- **现象**：镜像构建直接失败：`base name (${HDSL_BASE}) should not be blank`。
- **根因**：`ARG HDSL_BASE=…` 写在了第三个 `FROM` 前面——那个位置它属于上一阶段的残留作用域，
  `FROM ${HDSL_BASE}` 展开成空串。
- **做法**：要在 `FROM` 里用的 `ARG` 一律放文件顶部（第一个 `FROM` 之前）；阶段内用的 `ARG`
  放在 `FROM` 之后。改了 Dockerfile 就**至少让 CI 构建一次镜像**——shell 语法检查看不出这类问题。
- 同类的第二个错：构建期断言（比如 ffmpeg 的编码器）**必须在装包之后**，否则那个二进制还不存在。

### 5.20 入口脚本的属主校正：先建目录，再 chown
- **现象**：首启 `logs/` 是 `root:hdsl 2775`（组可写），要重启一次才变成 `root:root 0700`；
  另外 `auth/` 也晚一步。于是"刚部署完"的那几分钟里，agent 能写审计目录。
- **根因**：属主校正只修"已经存在"的路径，而 `auth/`、`logs/` 是**面板启动之后**才建的，
  它们继承 volume 的 setgid + `umask 002`。
- **做法**：入口先 `mkdir -p` 那些目录再 chown/chmod。顺序也要对：**整个 `/data` 先归 agent，
  再把秘密路径抠回 root**——反过来的话，新出现的目录（MC 数据、壁纸、品牌实例）会跟着变 root，
  agent 就写不动了。
- 校正要**幂等且便宜**：只在属主不对时递归，之后每次启动只 `stat`。别每次开机遍历几百 GB 的卷。
- 有些文件系统不认 mode 位（§5.9），所以 chown 之后要**自检**并在日志里喊出来。
- 改完入口脚本别忘了 §4 里那段：把它从 Dockerfile 渲染出来 `sh -n` 一遍再构建镜像。

### 5.21 解包用 Java，不要 shell 出去调 `tar`
- **现象**：裸部署（只有 jar）上某条安装路径失败，容器里却好好的；错误是 `tar: not found` 或
  Windows 上 GNU tar 把 `C:\` 当成远程主机（`Cannot connect to C: resolve failed`）。
- **根因**：几行 `new ProcessBuilder("tar", "-xzf", …)`——容器里装了 tar，所以谁也没发现。
- **做法**：解包一律用 Java 库（zip / tar / ar / xz / gzip 都够用），**不依赖宿主**有任何压缩工具；
  需要 7z/rar 之前先问"这功能真的需要吗"（现实里进面板的归档只有 zip / tar.gz / tar.xz）。
  容器里装着那套工具是给人工排障用的，**不是**给代码用的——混了就会出现"镜像里能跑、裸部署坏掉"。

### 5.22 面板的 jar 在运行期间被换掉
- **现象**：替换 jar 之后面板开始报一堆 `NoClassDefFoundError` / 找不到资源，用户完全不知道发生了什么。
- **根因**：JVM 已经加载的类在内存里，没加载的才去磁盘读——新旧两半混着用。
- **做法**：启动时记录 jar 的路径与 mtime/大小，运行中检测到变化就在页面与日志里说清"jar 被替换了，
  重启即可"，而不是让 ClassNotFound 自己冒出来。

### 5.23 两段身份之后，"面板写、agent 读"的文件要交还属主
- **现象**：两段身份的第一版镜像里所有实例安装全挂，日志只有一行
  `EACCES: permission denied, open '/home/hdsl/.config/pnpm/config.yaml'`；升过级的机器上**老实例也一起坏**。
- **根因**：面板是 root 了，而这个配置是它用"临时文件 + 0600 + 原子 rename"写的——写出来
  `root:root 0600`，agent 的 `pnpm` 连读都读不了。之前两者同 uid 时这行代码是对的，分成两段身份后
  就变成了"面板把 agent 的配置文件锁在自己手里"。`umask 002` 和共享组救不了它：组共享管不到 0600。
- **做法**：凡是 agent 要读（甚至要改：`pnpm config set` 会重写它）的文件，落盘后连属主带 mode 一起
  交给 agent uid —— `Privileges.handToAgent(path, "rw-rw-r--")`，父目录也给（`rwxrwxr-x`，pnpm 的
  重写是在同目录里换文件）。同一处必须顺手堵住符号链接：该目录归 agent，agent 可以放一个
  `config.yaml.tmp-…` 指向 `/data/server.yaml`，面板要是顺着写就是**用 root 的权限替 agent 写文件**。
  所以临时文件名带随机后缀、用 `CREATE_NEW` 建（自己建的 inode 不可能是预先埋好的链接），目标若是
  符号链接就删掉不跟随。
- **两个 Java 层的坑**：`PosixFilePermissions.fromString` 只认 `rwxst` 里的 `rwx`，给个 `rwxrwsr-x`
  直接 `IllegalArgumentException`；`chown` 会清掉 setuid/setgid 位，所以**先 chown 再 chmod**，
  顺序反了文件仍然打不开。
- **验证**：容器里 `docker exec -u 1000 <容器> pnpm --version` 与 `pnpm config get registry` 都要能跑；
  再建一个实例装一次。

### 5.24 「面板写、agent 也写」的树：写完就交接，别用 root 跑"看似只读"的命令

- **现象**：某个实例启动即失败，日志是
  `EACCES: permission denied, open '/data/hdsl/instances/<id>/home/profiles/web/cordis.yml'`；同一台
  机器上别的实例正常，**重启容器也不修**（入口的递归 chown 只在顶层属主不对时才跑）。
- **根因 1（触发器）**：`dsh --profile <p> --help` 不是只读——它先**组合 profile**，把 `cordis.yml`、
  `cordis.patch.yml`、`package.json` 写进 `$DSH_HOME/profiles/<p>/`。这个能力探测以面板身份（容器里
  root）跑，文件于是归 root；之后每次启动都降权到 agent，写同一个 `cordis.yml` 就被拒。**凡是"问一句
  这个程序支不支持某参数"的命令，都可能带副作用。**
- **根因 2（为什么只有 home 里坏）**：启动前只 chown 了 `instances/<id>/` 与 `home/` 两个顶层目录；
  `profiles/` 是面板后来以 root 建的，而 `prepareAgentDirectory` 的 `0775` 会把 setgid 清掉（§5.9），
  home 内部从此不再继承 hdsl 组。`/data` 的 `2775` 只兜得住这两个目录之外的兄弟。
- **做法**：
  1. 所有"替实例跑"的命令（探测、安装、打包校验）一律 `Privileges.asAgent`；
  2. 面板往实例树写文件后，**文件连同父目录**交给 agent
     （`Privileges.handWrittenFile(file, dataRoot)`，只对数据目录内生效——`CUSTOM` home 指向用户自己的
     目录时不许动）。覆盖：账户路由、插件补丁、注入设置、默认模型、`.env`、`pnpm-workspace.yaml`、
     安装清单、插件包解包、技能包安装；
  3. 启动前做一次「关键路径属主检查」（`home`、`profiles`、`profiles/<p>/cordis.patch.yml`、
     `storages`、`settings.yaml`、`.env`），不对就整棵 home 交接（`handTreeToAgent`，幂等）——
     健康实例只付几次 stat，被写坏的老实例下次启动自愈；
  4. 品牌安装（npm / GitHub / Hermes / Octop）完成后把 `releases/<version>` 整树交接；
  5. 自检脚本要**递归**查 home（顶层的 `touch` 看不见 `profiles/web`），并反过来断言 `runtimes/`
     对 agent **不可写**——托管 Node 是解释器，只读执行才对；agent 能改写解释器等于给持久化留门。
- **验证**：新实例、被 `--help` 污染过的老实例各启动一次；容器里
  `docker exec -u 1000 <容器> sh /tmp/verify-privilege-split.sh` 全绿（含递归与 runtimes 两项）。

---

## 6. 人工验收清单（自动化测不到的部分）

1. 独立数据目录起一个面板（**不要**在生产面板上试 E-Stop），装一个实例，打开页面、登录、发一条消息。
2. 网页终端：中文输入法连续输入、粘贴多行、`ctrl+o` 展开、终端退出后重连。
3. 手机：品牌实例列表可竖向滚动、行内按钮可横向滑动、长 url 不撑破卡片。
4. 反代：子路径部署（`HDSL_BASE_PATH`）下页面与 WebSocket 都通。
5. 开发者模式：提权 → 开守卫（proxy / instance / all 各试一次）→ 读仍然可用、写被要求密码 →
   E-Stop 三次确认 → 冷却期内解除被拒 → 重启面板后解除。
6. 品牌安装：portable 与源码构建各一次；中途杀面板，确认重启后会自动恢复/清理半安装。
7. **容器的身份划分**：`docker cp scripts/verify-privilege-split.sh <容器>:/tmp/` 之后
   `docker exec -u 1000 <容器> sh /tmp/verify-privilege-split.sh` ——
   `auth/`、`logs/` 读写都被拒，实例目录与 pnpm store 可写。**注意 `docker exec` 默认用户不再是
   agent**，不写 `-u 1000` 等于用 root 去验"agent 能不能写"，什么都验不出来。
8. **ACL 存储**：`docker logs <容器> | grep "does not enforce mode bits"`。有输出就说明这台机器的
   文件系统不认 mode 位（QNAP trimacl 一类），隔离在本机不成立——**写进部署记录，别对外声称有**。
9. **从"单 uid"升级到"两段身份"**：用老卷起一次新镜像，确认入口把 `/data` 校正到 agent、把
   `auth/`/`certs/`/`logs/`/`server.yaml` 收回 root，且实例仍能启动、终端能开（默认 workspace
   必须是 `/home/hdsl/workspace` 或显式设了 `HDSL_WORKSPACE`）。
10. **PTY 身份**：`ps -o uid= -p <tui pid>` 应是 agent uid，不是 0。终端里跑起来的 agent 也碰不到
   面板的凭据，这是 §5.9 的验收面。
11. **agent 侧的 pnpm**：`docker exec -u 1000 <容器> pnpm --version` 与 `pnpm config get registry`
   都要正常返回；`ls -l /home/hdsl/.config/pnpm/config.yaml` 的属主应是 agent uid 而不是 root。
   这一条不查，坏掉的形态是"面板一切正常，所有安装 `EACCES`"（§5.23）。
12. **界面**：品牌层在 `/instances` 原地切换（不跳第三层）；停止是红色药丸；开发者面在 HTTP 下只给
    提示 + 「去 HTTPS」；手机竖屏与横屏（844×390）各看一眼：抽屉、标签条、贴底操作栏。
13. **终端时长**：提权 30 分钟滑动——连续操作时不会中途被踢；放着不动半小时回来，终端仍在
    （`setMaxIdleTimeout(0)`），只是提权可能过期、需要重新确认。
14. **属主交接**：老实例（被旧面板写坏过的）启动一次后 `home` 整树归 agent；新实例从第一次启动起
    就不该出现 root 文件（§5.24）。

---

## 7. 明确不要做的事

- 不要在 HTTP 上放开开发者模式（没有 TLS 时"提权"就是明文传密码）。
- 不要给进程内检查加"哈希文件保护"之类的假防线（§5.9）。
- 不要隐藏不可用功能，要变灰并写原因。
- 不要用"顶层 chown"冒充"交接"：`profiles/` 这类子目录是面板后来建的，顶层检查看不见（§5.24）。
- 不要把"问一句 `--help`"当只读：它会组合 profile、会写文件（§5.24）。
- 不要引 UI 组件库或 CSS 框架，也不要在组件里写死颜色（§2.10）。
- 不要用未经授权的商标做图标；没有授权就用中性图形 + 文字。
- 不要在仓库里写真实域名 / 内网地址 / 宿主机路径 / 口令。
- 不要 force push 远端历史、不要移动已发布的 tag（除非用户明确要求"同版本重发"）。
- 不要把"测试全绿"当成发布依据：本文里至少 §5.1、§5.7、§5.8、§5.19、§5.20 都是**只有跑起来**
  才看得见的；镜像相关的改动还要真的构建一次镜像。
- 不要把"镜像构建成功"当成"隔离生效"：前者是 Dockerfile，后者要靠 §6 第 7、8 条去验。两者之间
  隔着文件系统语义（ACL 存储）、入口的属主顺序、以及 `docker exec` 的默认用户。
- 不要在 ACL 管理的存储上声称有隔离。**接受**是合法选择——只要在部署记录里写明"这台机器没有隔离"，
  而不是让文档和实际相反。
