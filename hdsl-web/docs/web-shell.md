# Web Shell：实例级原地终端

> 状态：已上线。v0.5.3 起它是面板里唯一的 TUI 入口（SSH 通道已移除）。v0.5.71 起覆盖
> 五个品类：**dsh**、**Kimi Code**、**OpenCode**、**Claude Web**（社区引擎 / 官方
> Claude Code 两个内核由用户选）与 **ZCode**（实验性品类）。

## 1. 是什么

在实例上点「终端」，**就地**在当前界面打开该实例的原生 TUI，走现有 WebSocket。
不新增端口、不引入 SSH 协议栈、不需要反向代理。

终端以**实例**为单位启动：同一份 home、账号/模型路由、工作区，所以 TUI 里的会话记录、
能 resume 的会话，与网页模式看到的是同一份数据。

## 2. 不是什么

- 不是通用 shell：没有自由命令、文件上传下载、端口转发、切换用户。
- 不是 SSH 的替代协议：SSH 通道已在 v0.5.3 移除，面板里唯一的终端入口就是这个 Web Shell。
- 不是沙箱：agent 以面板进程的用户身份运行。

## 3. 使用流程

```
实例卡片/详情页  [终端]
      │  web 运行中则先停 web（记 terminal.switch）
      ▼
  原地覆盖层（桌面）/ 全屏 sheet（手机）
      │  xterm.js ←WSS→ 服务端 PTY → kimi / opencode / dsh（已装的 TUI 插件）
      ▼
  底栏：实例状态 + [启动] / [停止]
```

- `/exit`、进程退出、崩溃 → 清屏关窗，恢复之前的 web 模式。
- 关窗 / 刷新 / 断网 → 只 detach，进程继续跑；重新打开即 attach 并回放最近输出。
  退出登录同样**只掐断这个会话挂着的终端连接**（1008 `logged out`）——已建立的 WS 不
  再复查会话，不掐的话登出后的浏览器还留着一条活动的终端通道；PTY 与 agent 不受影响。
- 底栏「停止」= 紧急杀进程（复用实例停止机制），不是关窗。
- 手机形态（窄屏或触屏）：覆盖层全屏铺满，窗口高度钉在 `visualViewport` 上——软键盘
  起落、地址栏伸缩都不遮挡输入行（iOS 上软键盘只改 visualViewport）；刘海与手势条走
  `env(safe-area-inset-*)`。终端与底栏之间有一排功能键（Esc、Tab、Ctrl+C、Ctrl+D、
  Ctrl+Z、方向键），按下即发对应 PTY 字节——手机软键盘发不出这些键，而 WS 二进制帧
  本来就是原始字节（terminal-protocol.md §3），协议与服务端都不用动。
- 终端头部显示**提权剩余时间倒计时**（每 30 秒与面板对表）。提权过期**不影响**已连接
  的终端——它只 gate 开新终端/重连，agent 该跑还跑；掉线后点「重连」会重新走一次密码
  对话框，验证接上即可。
- 不做空闲超时；终端一直挂着不自动回收。终端 WebSocket 显式 `setMaxIdleTimeout(0)`——Jetty 默认的
  30 秒曾把静默终端以 `1001 Connection Idle Timeout` 关掉，看起来就像"终端自己断了"（v0.5.81 修）。

## 4. 安全不变量

1. **不经过 shell 启动**：`posix_spawn` 直接 exec `node <品牌入口 JS>`，不复用 `.bin` shim；
   启动后断言根进程不是 `sh`/`cmd`。
2. **根进程就是 agent**：`waitpid` 返回后的顺序是**先拒绝输入**（`inputClosed`）、
   给泵一个有界的时间排空最后几个字节（排到 EOF 即止，超时只是兜底），**再 `close(master)`**
   ——此后不再有输出可被观察到——最后 `kill(-pgid)`、移除会话、通知 WS、审计。
   排空之所以在关 master 之前，是因为泵手里那块已经读出、还没交给读者的字节属于读者，
   关掉就丢尾（旧文写的"关 master 之前不做任何别的事"与实现不符，此处按实现纠正）。
   master 的**描述符**另有规矩：任何调用都不直接用它，每次 `poll`/`read`/`write`/`ioctl`
   各持一份副本（`fcntl(F_DUPFD_CLOEXEC)`，一次调用完成复制并带 CLOEXEC）——描述符号会被内核复用，
   一个已经通过检查、正在内核里的调用若继续用原号，读到的可能是**另一个实例**终端的输出，
   或者把按键写进别人的 PTY。两个描述符都在 `openpty` 返回后**立刻**带上 `FD_CLOEXEC`，
   子进程的文件动作里也关掉它手里的 slave 副本：否则一个孙进程会一直握住 pty，
   master 就永远等不到 EOF（排空会退化成等超时）。
3. **WS 只搬运字节**：没有 exec / send-keys / 切换程序的接口。
4. **进程组隔离**：`POSIX_SPAWN_SETPGROUP`（pgroup=0），杀树用 `kill(-pgid)`；
   窗口尺寸写 `TIOCSWINSZ`，**SIGWINCH 由面板自己发**（`resize` 里对根进程 `kill(SIGWINCH)`）——
   子进程没有控制终端（未做 `TIOCSCTTY`），内核没有可通知的前台进程组，只写 `TIOCSWINSZ`
   会改尺寸但不通知 TUI（旧文写成"由内核发送"，与实现不符）。
5. **权限**：打开终端需要会话 + 开发者密码提权（会话级 30 分钟滑动）；argv 白名单；
   单实例单会话；审计 `terminal.open` / `terminal.exit` / `terminal.kill` /
   `terminal.desktop` / `desktop.reveal`。
6. **ticket**：WebSocket 用一次性的 `/ws/terminal/{ticket}` 鉴权，ticket 只对一次 attach、
   一个实例有效，30 秒后失效；会话 Cookie 不进 URL，避免落进历史记录与代理日志。

**做不到的**：agent 自带 shell 功能不归本层管；`setsid` 逃出进程组的孤儿 `kill(-pgid)`
清不掉（master 已关，无法再交互），彻底清理需 cgroup v2，列为后续加固。

### 4.1 威胁模型：入口即 RCE，本地权限不是边界

> 完整的安全说明（威胁清单、误配、免责）在 [`security.md`](security.md)；这一节只讲终端这一条。

终端这条路**在效果上等价于远程代码执行**：`POST /api/brands/{brand}/instances/{id}/terminal`
能启动一个执行 agent 的进程，屏幕还能交互。因此它的安全全压在入口上，而不是"Windows 有没有
桌面"这种平台差异上——ConPTY 换掉桌面窗口之后，这条不变：

- 端点仍走 `/api/*` 的会话门（`AuthFilter`），没有会话一律 401；
- 还要开发者密码提权（会话级 30 分钟）；没有提权 403 `developer_elevation_required`；
- 启动的永远是适配器解析出来的 argv（程序 + 参数），**不经 shell**：Windows 上
  `.cmd`/`.bat`/`.ps1`/`.com` 一律拒绝，因为 `CreateProcess` 会把它们交给 `cmd.exe`；
  请求体里没有任何"我要跑什么"的位置，两端都只认那条 argv；
- **部署前提**：面板只监听 loopback 或可信网络；要暴露公网，必须在网络层加保护
  （反代鉴权/VPN/白名单），或者干脆不开终端按钮。文档明确写这条，而不是拿
  「有终端」本身当安全边界。绑非回环又确实需要把实例网页/实例操作放出去时，开
  `developer_guard.scope: instance`（见 §4.2）：实例网页与启动/停止/装包那一层会要求开发者密码；
- 反过来，Linux/macOS 的 PTY 通道同样受这套入口约束：会话 + 提权 + 单实例单会话。

> 这一节的门只覆盖**面板自己的终端**（`POST /api/brands/{brand}/instances/{id}/terminal`
> 及面板自己的 TUI）。品牌工具自带的终端（PI WEB 的浏览器终端、ZCode 的网页终端，以及以后
> 任何带终端的品牌 UI）是另一条入口：它由该工具自己鉴权，不受这五道门约束；`/i/<id>/` 也
> 不经过面板终端门。要门禁就把 `developer_guard.scope` 设为 `proxy`（§4.2）。见
> [`brands.md`](brands.md) 的「PI WEB」、[`zcode-experimental.md`](zcode-experimental.md)
> 与 [`security.md`](security.md) §2.4。

### 4.2 总开关与开发者模式

设置页的「开发者模式」页本身也要开发者密码，进去后有这些控件：

- **Web Shell 总开关**（写入 `server.yaml` 的 `web_shell.enabled`，环境变量
  `HDSL_WEB_SHELL_ENABLED` 可覆盖）：关掉后 `POST .../terminal` 返回
  403 `web_shell_disabled`，实例列表不再显示终端按钮；已经在运行的终端不受影响，
  可以从自己的窗口里停止。开关本身要求开发者密码提权 + HTTPS 才会被接受。
- **开发者守卫**（`developer_guard.scope`，环境变量 `HDSL_DEVELOPER_GUARD`，**默认 `off`**）：
  决定提权还管到哪。四档逐级包含：

  | 取值 | 管什么 |
  |---|---|
  | `off` | 和没有守卫时一样，只有终端要提权 |
  | `proxy` | `/i/<id>/*` 的 HTTP 与 WebSocket 都要提权 |
  | `instance` | 再加 `/api/instances/*`、`/api/brands/*`、`/api/zcode/*`、`/api/packs/*`、`/api/settings/*` 的 POST/PUT/PATCH/DELETE；GET 仍放行 |
  | `all` | 除 `/api/health`、`/api/auth/*`、`/api/developer/*`、`/api/version` 外，全部 `/api/*` 都要提权 |

  **`all` 档连读都要提权**，所以未提权时面板读不到实例列表——这时顶部会显示一条
  「『全部 API』档需要开发者密码才能读取数据」的提示和「输入开发者密码」按钮，页面不会假装"没有实例"。
  另外两档只挡写操作，列表照常可见。
  **它是门禁，不是断路器**：只挡新请求/新连接。已经加载的实例网页继续跑（它自己的接口请求开始
  403），已开着的终端与正在跑的 agent 一律不动——重新进入才要密码。要立刻掐断一切，用下面的
  紧急停机。
- **给脚本/巡检 agent 的口子**：`POST /api/developer/elevate` 的响应里多一个 `token`（32 字节
  随机串，只在内存、与提权同寿命、改密码或注销即失效）。拿它当
  `Authorization: Bearer <token>` 就能在没有 cookie 的情况下通过守卫。别把它写进脚本文件或用
  `ps` 能看到的命令行参数里。
- **修改开发者密码**：需要当前开发者密码 + 面板密码（与设置时同一套确认）；成功后
  所有已提权的会话立即失效，需要重新验证。
- **提权时长**：提权默认 30 分钟滑动续期；开发者模式页和 hdsl-tui 的「开发者提权」入口
  都可以把它**延长到 3 小时后**（`POST /api/developer/extend`，要求当前已提权，**每张票
  限一次**，重新验证密码可再延长），也可以**随时停止**（`POST /api/developer/revoke`，
  幂等，不等到期）。`GET /api/developer/status` 的 `expiresAt` 告诉两端还剩多久。

### 4.2.1 紧急停机（E-Stop）

设置页「开发者模式」里的红色区块，一次点击把面板对外的一切冻住。它要三次确认，第二次输入
`紧急停机` 四个字，第三次声明不可逆。

- **按下前先看清单**（`GET /api/developer/estop/preview`）：此刻在跑的实例（id / 状态 / 进程数）
  与终端（品牌 / 实例）逐条列出，并写明**面板里正在跑的巡检/修复 agent 也会被杀掉**——那个 agent
  往往就在某个实例的进程树或某个终端里。
- **按下时**（`POST /api/developer/estop/engage`，要求提权 + `confirm` 短语）按这个顺序：
  1. 先翻转状态，所有闸门立刻关上（之后再起来的实例也会被拦住，不会漏）；
  2. 关掉所有终端（POSIX 走进程组 SIGKILL，Windows 走 Job Object，两平台都是整棵树；终端进程
     本来就在面板的注册表里，没有"够不着的窗口"）；
  3. 强杀所有实例的整棵进程树：先收集 `descendants()`，再 `destroyForcibly()`，有界等待后再补一轮。
     **不跑 `post-exit` 自定义命令**——紧急停机不该再去执行用户代码。会话尾部可能丢几行。
  4. 审计 `estop.engage`（谁、终端数、进程数、原因）。
- **停机期间**：`/i/*` 与实例的写操作类 API 一律 `503 emergency_stop`；终端接口同样 503。
  登录、`/api/health`、`/api/developer/*`、只读列表与日志、`/ws` 事件流照常——否则界面看不到状态、
  也解除不了。
- **解除**（`POST /api/developer/estop/release`）：提权 + **冷却满 1 小时**。未到点返回
  `409 estop_cooldown` 并带 `remainingSeconds`。没有任何"强制解除"接口。
- **状态只在内存**：重启面板进程或容器就清空——这是唯一的物理恢复方式，也是本次的明确选择。
  被杀掉的实例不会自动重启。重复按下不会重置计时。

**已知取舍（写在这里，免得以后当 bug 报）**：

- 重启即解除是**本次的明确选择**：任何能重启容器的人都等于握着恢复键，所以宿主机权限仍然是最高权限。
- 守卫只挡新进门；E-Stop 才动正在跑的东西（终端、进程树）。两者不是一回事。
- 预览清单是**按下那一刻**的快照——按之前最后几秒新起的实例也会被杀，因为状态闸门在 engage 第一步就关上，不会漏。
- `all` 档会让面板日常操作频繁要密码，这是它存在的意义；默认必须是 `off`，公网部署建议 `instance`。
- E-Stop 期间**不能装整合包**（装包会起进程，算"启动"）；它也不能用来"清空正在装的包"，等解除。
- 实例进程与终端的强杀两平台一致；面板进程自己被 `taskkill /F` 时，终端子进程随 Job Object 一起消失。
- **验证纪律**：E-Stop 只能在独立起的临时面板（临时 `HDSL_DATA`、`HDSL_PORT=0`）上按。生产面板里往往
  正跑着干活的 agent（包括用来修面板的那个），按下去会一起杀掉，而且一小时内回不来。

### 4.3 开发者模式只在 HTTPS 下可用

开发者模式、终端、守卫与紧急停机都只在面板**此刻真的在讲 TLS** 时可用（读的是主连接器，不是配置里的
`https.enabled`）。裸 HTTP 部署就随它去：面板不会在那种状态下开开发者模式，也不会开守卫——没有 TLS 时
"提权"本身就是明文传密码，与其给一把假锁，不如不给。这也意味着**裸 HTTP 下不存在"先提权再放行"这条路**，
任何 `developer_guard.scope` 都只会让实例网页更难打开，而不会真正变安全。

- HTTP 下进入「开发者模式」会弹出「当前环境不安全」的提示，并指引到「HTTPS 证书」；终端按钮在实例列表里
  隐藏，直接调 `POST .../terminal` 返回 403 `insecure_transport`。
- **开发者守卫与紧急停机同样只在 HTTPS 下可用**：它们都建立在提权之上，拿不到提权时 `/i/*` 的回答是
  `tls_required`（提权页会说明"开发者模式不可用"），开发者页把两组控件变灰并写明原因。
- 威胁模型与责任划分（哪些归面板、哪些归部署者）见 [`design-notes.md` §7](design-notes.md#7-安全模型)。
- 没有证书时可以选择生成自签名证书：`POST /api/settings/tls/self-signed` 生成（或复用）
  `<data>/certs/self-signed*.pem`，把 `https.enabled` 写进 `server.yaml`，在同一端口热切换
  连接器。浏览器会提示证书不受信任，这是预期行为；它只适合内网或临时使用。
- 「HTTPS 证书」页顶部同样有红框说明，并在 HTTPS 未启用时提供同一个按钮；已经配置了真实
  证书（`https.cert_file` / `https.pkcs12_file`）时，后端拒绝回退到自签名
  （409 `certificate_already_configured`），避免误把线上身份换掉。
- **反代终结 TLS** 也算：打开 `https.trust_forwarded_proto` 后，请求带
  `X-Forwarded-Proto: https`（nginx/Caddy 的常规配置）即视为 HTTPS，开发者模式、终端和
  `Secure` 会话 Cookie 都按 HTTPS 处理。这个信任是显式开关，默认关闭——直接能连到面板
  HTTP 端口的客户端可以自己写这个头。已经是这种部署时，不要再生成面板自己的自签名证书，
  接口会以 409 `tls_terminated_upstream` 拒绝，免得把反代到面板这段改坏。


## 5. 架构

```
web/terminal/                 常置包，不依赖 MINA
  TerminalTarget              实例 → home/cwd/env/argv
  BrandTui                    KimiTui / OpencodeTui；ClaudeWebTui 带内核参数（BrandTui.forBrand 故意不返回它）
  ZcodeTui                    ZCode 的官方 TUI（不带 --web 的 bin/zcode.mjs）
  InstanceTerminal            三个品类共用的门禁 / 平台预检 / spawn / ticket
  TerminalSink                一个挂载者（生产是 WebSocket，测试是收集器）
  TerminalSession             PTY 进程 + 回放缓冲 + attach/detach + 退出
  TerminalManager             按 brand + instanceId 互斥、open/terminate、审计
  TerminalBusyException       同实例第二个终端
  pty/                        伪终端胶水（JNA）
    Pty / PtySize / PtyException / PosixPty / ConPty
web/http/InstancesApiServlet   dsh 的 POST/DELETE /api/instances/{id}/terminal
web/brand/BrandApiServlet      品牌的 /api/brands/{brand}/instances/{id}/terminal 与 …/terminals（内核清单）
web/zcode/ZcodeApiServlet      ZCode 的 /api/zcode/instances/{id}/terminal
web/http/DshApiServlet         GET /api/dsh/tui-plugins（精选 TUI 终端插件清单，见 DshTuiPlugins）
web/http/InstancesApiServlet   /api/instances/{id}/tui-plugins（每个插件自己的 profile 上的状态与安装/卸载）
web/ws/TerminalWebSocket       WSS /ws/terminal/{ticket}
```

### 5.1 反代的 `Host` 透传：一个只能设一次的 JDK 开关

`/i/*` 必须把浏览器带来的 `Host` 原样转给实例（dsh 用 `Origin == Host` 判同源），而 `java.net.http`
默认拒绝设置 `Host`，要先设 `jdk.httpclient.allowRestrictedHeaders=host`。麻烦在于**JDK 在第一次创建
`HttpClient` 时就把这份允许列表定死了**，之后再设属性没有任何效果。

2026-10-28 踩过一次：启动流程里先碰了一个带静态 `HttpClient` 的类（Octop 安装器），再构造反代
servlet，于是所有 `/i/*` 都回 `500 restricted header name: "Host"`；加 `-D` 参数立刻恢复 200，
说明纯粹是顺序问题。

现在三道保险，别再拆掉任何一道：

1. `main` 的**第一行**调用 `HostHeaderSupport.allow()`，在建任何 `HttpClient` 之前；
2. `InstanceProxyServlet` 的静态块保留同样的调用作兜底（只对"还没有 HttpClient"的场合有效）；
3. `HdslServer` 装配完最后一个 servlet 后调 `HostHeaderSupport.verify()`——它**直接探一次请求**，
   而不是回读属性（设晚了的属性读起来一切正常，这正是要抓的错），失败即启动失败并给出 `-D` 补救。

对应测试 `HostHeaderSupportTest` 用**独立 JVM** 跑 `HostHeaderProbe`：先加载带静态 `HttpClient` 的类
再 verify 必须被拒绝；先设属性则放行。普通进程内测试抓不到这个回归（测试 JVM 的 `TestSupport`
早就把属性设好了，这正是以前测试全绿而线上 500 的原因）。

## 6. PTY 胶水（JNA）

- 依赖只有 JNA（LGPL-2.1/Apache-2.0 双许可，按 Apache-2.0 使用，与 GPL-3.0 兼容）。
  `build.gradle.kts` 头部的「no JNA」改为「唯一原生依赖是 JNA，仅用于 PTY 胶水」。
- Linux/macOS：`openpty` + `posix_spawn`；
  file actions：`adddup2(slave,0/1/2)`、`addclose(master)`、`addchdir_np(cwd)`；
  attr：`POSIX_SPAWN_SETPGROUP` + pgroup=0。
- 不在 JVM 里 `fork()`；不注册 JNA 回调；泵线程 `poll` + 关闭标志；
  `read` 遇 `EIO` 视为子进程结束；`waitpid` 独立线程收尸。
- Windows：`CreatePseudoConsole` + `CreateProcessW`（见 §7.1）。进程树靠 Job Object
  （`JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`）加 `CREATE_SUSPENDED` → 入 Job → `ResumeThread`；
  `Pty.spawn` 按平台分流，上层一行不用改。
- FreeBSD：复用同一条 `PosixPty` 路径（`openpty` 在 base 的 libutil，`posix_spawn` 在 libc）。
  两个平台常量不同：`F_DUPFD_CLOEXEC` = 17；`TIOCSWINSZ` 与 macOS 相同（`0x80087467`）。
  已在 FreeBSD 15.1 真机验证 openpty、resize、stdin 与退出码。Minecraft 控制台仍走 pipe。

## 7. 实施阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 分支、基线、kimi/opencode 会话存储 spike | 完成（源码核对） |
| 1 | 品牌无关的终端核心（`TerminalTarget` + `BrandTui`） | 完成 |
| 2 | PTY 胶水（JNA） | 完成 |
| 3 | `TerminalSession` / `TerminalManager` | 完成 |
| 4 | HTTP 与 WebSocket（ticket、`/ws/terminal`） | 后端完成，见 §10 缺口 |
| 5 | xterm 前端 + 底栏启停 | 完成 |
| 6 | dsh 适配 | 完成（dsh 终端） |
| 7 | Windows ConPTY | 完成：`ConPty`（Win10 1809 起，见 §7.1）；桌面终端已删除 |
| 8 | 移除 SSH 通道（v0.5.3） | 完成：`web/ssh`、MINA 与 eddsa 已删除 |
| 9 | ZCode 终端（v0.5.71） | 完成：`ZcodeTui`、`/api/zcode/instances/{id}/terminal` |
| 10 | Claude Web 双内核（v0.5.71） | 完成：社区引擎 / 官方 Claude Code，`…/terminals` + `?kernel=` |
| 11 | 门禁收口与打开顺序（v0.5.71） | 完成：`InstanceTerminal`；先密码对话框、必要时选内核，再出终端窗口 |

品类覆盖顺序：**kimi → opencode → dsh → zcode → claude-web（双内核）**。

### 7.1 Windows：ConPTY

Windows 上也走浏览器 PTY，用的是系统自带（Win10 1809 / build 17763 起）的
`CreatePseudoConsole`：不随包发任何二进制，也不需要桌面会话。服务形态（session 0）
同样可用——这正是换掉桌面终端的主要原因。

- **门槛**：build < 17763 或符号探测失败 → 终端接口 409 `terminal_unsupported_platform`，
  文案里带实际 build 号；**不回落任何桌面窗口**。Win7/8.1 明确不支持（不引 winpty）。
- **argv-only**：`lpApplicationName` 单独给 `argv[0]`，命令行按 MSVCRT 规则拼（引号、反斜杠
  加倍），`bInheritHandles` 为 FALSE，伪控制台经属性列表传给子进程；子进程的 stdio 用
  `STARTF_USESTDHANDLES` + 三个 NULL 交给控制台自己分配（否则 `CreateProcess` 会把面板
  自己的 stdout 拷给 agent，画面写进面板日志、终端一片空白）。
- **拒绝 shim**：`command[0]` 以 `.cmd` / `.bat` / `.ps1` / `.com` 结尾直接拒绝——`CreateProcess`
  会悄悄把它交给 `cmd.exe`，等于在链路里塞进一个 shell；`argv[0]` 还必须是存在的绝对路径。
- **进程树**：`CREATE_SUSPENDED` → `AssignProcessToJobObject` → `ResumeThread`，
  Job Object 带 `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`。终端结束时整棵树一起走；面板进程
  被强杀时也一样（Job 句柄关闭即杀）。
- **与 POSIX 的语义差异**：Windows 没有能打到伪控制台子进程的"礼貌信号"
  （`GenerateConsoleCtrlEvent` 不行），所以 `terminate` 是"给一个自退窗口，然后
  `TerminateJobObject`"，不等价于 SIGTERM→SIGKILL。这是平台事实，不是缺陷。
- **平台能力**：一个实例终端 = 一个 `conhost.exe`（数 MB）。只承诺 64 位 JVM；arm64 未实测。
- **身份**：容器里面板是 root，终端进程和别的 agent 一样降权到 uid 1000（`PosixPty.spawn` 里前置
  `setpriv`，exec 原地替换，进程组与 PTY 会话不变）。所以终端里跑起来的 agent 也碰不到面板的
  `auth/`、`certs/`、审计日志——见 `docs/design-notes.md §7.5`。
- **MC 控制台**不动：仍走 pipe（见 `docs/minecraft.md` §15.2 的后续顺序）。

桌面终端（`DesktopTerminal`、`{"mode":"desktop"}`、`open-desktop-terminal` 确认短语）已整类
删除；前端与 Go 客户端的 `desktop` 分支保留，只对旧面板有意义。

## 8. 验收

- kimi/opencode 终端里的会话列表与网页模式一致（spike 结论落文档）。
- `/exit`、崩溃、底栏停止、实例停止四条路径都杀净进程树；进程树无 `sh`/`cmd`。
- 关窗/刷新后进程仍在，重开能接上并看到历史输出。
- agent 退出后写 PTY 不会到达 WS；会话移除，attach 返回 410。
- 同实例第二个终端 409；未提权 403。
- jar 里没有任何 SSH 代码与依赖；终端功能可用。

## 9. 品牌实测（阶段 0 spike）

已从源码核对（`kimi-code` / `opencode` 的仓库，以及 dsh 工作区里的 `zcode`、`claude-web-main` 两份上游源码）：

| 品牌 | TUI 命令 | 会话/配置目录 | 与 web 是否同一份 | resume 参数 |
|---|---|---|---|---|
| kimi | `node <包>/dist/main.mjs`（`kimi` 无子命令） | `KIMI_CODE_HOME` 或 `~/.kimi-code`；会话在 `<home>/sessions` | 是：`kimi web` 用同一 home | `-r/--resume [id]`、`-c/--continue` |
| opencode | 1.19+：`opencode-ai/bin/opencode.exe`（POSIX 也叫 .exe，ELF）；旧版：`opencode-<os>-<arch>/bin/opencode` 或 `@opencode/cli-*`。`bin/opencode.cjs` 只是启动器 | XDG：data `~/.local/share/opencode`、config `~/.config/opencode` | 是：同一 data 目录 | `--session <id>`、`--continue` |
| dsh | 终端由社区插件提供（dsh 自身只有 WEB/HEADLESS/ACP/SDK 四种 surface）：**启动插件自己的 profile**，`dsh --profile tui`（上游把 `dsh tui` 也展开成它），bin 型可优先直接跑其入口。详见下方 | 实例 `DSH_HOME` | 是（已有 SessionCatalog） | `--resume <id>`（bin 型与纯插件都支持） |
| zcode | `node <发行包>/bin/zcode.mjs`（不带参数；CLI 默认命令是 `tui`） | `ZCODE_DATA_BASE_DIR` 下的 `.zcode/`；会话库跟 `homedir()` | 是：与 web 进程同一个 `ZCODE_DATA_BASE_DIR` 与 `HOME` | 第一版不做 |
| claude-web | 社区引擎 `node_modules/claude-web-engine/dist/cli-node.js`；官方 `@anthropic-ai/claude-agent-sdk-<platform>-<arch>/claude[.exe]`（缺失退回 PATH 上的 `claude`） | `~/.claude` 与 `~/.claude-web`（实例 home） | 是：网页工作台写的就是这两处 | 第一版不做 |

实现约定：这几家都由适配器解析成「node + 入口 JS」或「原生二进制」，**不经过
`.bin` shim / `.cjs` 启动器**；环境里 `HOME`（和 Windows 的 `USERPROFILE`）指向实例
home，因此 TUI 读到的就是网页模式写下的那份会话。Windows 上这条同样成立：ConPTY 直接
`CreateProcess` 原生 exe 或 `node.exe` + JS，`.cmd`/`.bat`/`.ps1`/`.com` 一律拒绝——
交给 `cmd.exe` 就等于在链路里塞进一个 shell。

ZCode 是唯一的例外：它的会话库跟 `homedir()` 走，而凭据与供应商配置跟
`ZCODE_DATA_BASE_DIR` 走。为了让终端与 web 进程看到同一批会话，`HOME` **保持与 web
进程一致**（不指到实例目录），只额外设 `ZCODE_DATA_BASE_DIR`。

- kimi：pnpm 的 `.bin/kimi` 是 `#!/bin/sh` shim（不是软链），所以直接取
  `node_modules/@moonshot-ai/kimi-code/dist/main.mjs`；只有 npm 的软链布局才回退到
  `.bin/kimi`。
- opencode：1.19 起平台二进制由 `opencode-ai` 自己带（`bin/opencode.exe`，POSIX 同样
  叫 .exe 的 ELF）；先找它，再退回无 scope 的平台包与旧的 `@opencode/cli-*`。
- dsh（`DshTui` + `DshTuiPlugins`）：**终端是插件，跑在它自己的 profile 上**。上游把
  `dsh <name>` 展开成 `dsh --profile <name>`，所以 `dsh tui` 确实是上游入口——但 `tui` 不是
  内置模板名（模板只有 acp/web/headless/sdk/sdk-minimal），它要先被插件建出来。于是：
  - **装**：`POST /api/instances/{id}/tui-plugins/install` `{package}`，服务端按清单用
    `dsh plugin --profile <清单里的 profile> add <包@锁定版本>`（`GET …/tui-plugins` 读状态，
    每个插件从**它自己的 profile** 读，实例的 web profile 里本来就看不到它们）。
    **装完要验**：`pnpm` 先写依赖再取包，取不到时会留下"manifest 说有、`node_modules` 里没有"
    的半装状态——那正是"打开终端 0.4 秒死掉、一个字节都没有"的样子。所以 installer 在
    `dsh plugin add` 退出 0 之后还要看 `node_modules/<包>` 是否真的在（`DshPluginInstaller.requireLanded`；
    只验 registry 型 spec，`file:`/`link:`/`github:`/URL 的名字从文本里读不出来，误判会打断正常安装），
    不在就把任务判失败。这个状态**不再被当成已安装**：`terminalPlugin` 同时要求 manifest 声明
    与文件真的在——**目录里还要有 `package.json`**（`pnpm` 在建目录之后才拉包，中途失败或被删
    内容会留下一个空壳，而纯 bundle 是从清单指定的 patch 组合出来的，没有清单就什么都引导不了），
    有 bin 的包还要求它声明的入口存在，否则用同一句可行动的中文拒绝
    （"声明了 X 但文件不在，请重新安装"），而不是去启动一个空 profile。
    列表接口把两件事分开说：`installed` 是 manifest 的答案，`filesPresent` 是磁盘的答案；
    还有第三个 `outdated` —— 第一方插件比安装包摘要（安装成功时记进 `<实例 home>/.hdsl-web/first-party-installs.json`），
    社区插件比清单锁定的版本；版本号相同但内容不同、或老面板留下的"没有记录"，都会让页面给「更新」按钮；
    `installed && !filesPresent` 的那一行在页面上标「安装未完成」（变灰说明原因，不隐藏），
    按钮同时给「重新安装」与「卸载」——先卸再装不是用户该知道的步骤。
  - **还要配对宿主版本**：终端界面的**工具面来自 agent-preset 层**，而这一层是 dsh **0.2.0**
    才有的（`dsh-agent-preset-registry` + 每条 `preset` 一行）。写给它的插件（天枢 / tomowang /
    pi-tui）自带这一层，装在 0.1.x 实例上**装得上、起得来、但会话没有工具**——插件自己的原话是
    `agent-presets 未装配：本会话没有工具面`。所以清单每项都有 `requiresDsh`
    （天枢 `>=0.2.0-rc.1`、tomowang `>=0.2.1-rc.3`、pi-tui `>=0.2.1-rc.2`；`@deepseek-harness-tui/dsh-tui`
    的 peer 跨两条线，故为空=任意线），安装路由在提交任务**之前**就拒（`409` + 一句说清
    "需要 X、你的实例是 Y、工具面为什么没了、可以改用哪个"），`terminalPlugin` 在开终端时也拒
    （已装的旧组合）。清单行不会消失，只是**变灰不可装**并写明原因——隐藏会让人以为没这个功能。
  - **绝不能装进 web**：TUI 插件与 `dsh-web-app` 都是 app 插件，各按自己的语法解析同一份
    argv，上游不仲裁；同 profile 装一起会让应用遇不认识的旗标直接退出（社区实测
    `error: unknown option '--no-open'`），实例起不来。守卫在 `DshPluginInstaller.installSpecs`
    （拒绝发生在解析运行时之前，所有安装路径共用），`DshTuiPlugins.refuseAppProfile` 负责说明；
    卸载永不拒绝（那是把实例修回来）。
  - **起**：先按 `<DSH_HOME>/profiles/<插件 profile>/node_modules/<包>/<其 package.json 声明的
    bin>` 直接跑（bin 型，先读包自己的清单、再退回 `node_modules/.bin/<bin>`，且只接受解析后是
    JS 文件的软链——pnpm 写的是 `#!/bin/sh` shim，不能当根进程，理由同 kimi）；没有 bin
    （纯 bundle）就启动那个 profile：`node <bin.js> --profile <插件 profile>`。argv 里出现
    `--profile web`（例如用户在实例启动参数里自己写的）一律拒绝。
  - 一个都没装 → 明确报错并指向实例「安装」页；**声明了但文件不在** → 同样拒绝，但说的是
    "重新安装这个"。清单与许可证/仓库/profile 由 `GET /api/dsh/tui-plugins` 提供
    （`web/http/DshApiServlet`）；许可证含 Apache-2.0（`@huiliyi37/dsh-tianshu-tui`）。
    `@ruhooai/dsh-tui`（bin 需要 Bun + 外部 `cordis.yml`）与 `@zhangweiii/dsh-tui` 已不在清单里。
  - **两个第一方终端可以同时装，但只有一个在跑**：`dsh-inproc-tui` 与 `dsh-acp-tui` 是同一个
    终端面的两种实现，清单给两者同一个 `runtimeGroup`（`dsh-terminal`，社区插件不分组）。
    装两个是**允许且有用**的——ACP 那个是实例版本变来变去时的退路。启动时按清单顺序取第一个
    「已装 + 文件在 + fitsHost」的条目：0.2.x 用进程内那个，ACP 待命；版本降下去之后进程内
    那个不再匹配，ACP 自动接管。`GET …/tui-plugins` 每行多一个 `active`，页面据此标
    「当前生效 / 备用」。卸载永远不拒。
  - **面板自带的第一方终端（进程内）**：`plugins/dsh-inproc-tui`（`@nas200-timmy/dsh-inproc-tui`，
    profile `dsh-inproc-tui`）。它是 mode bundle：清单里没有 bin，安装后 `dsh plugin add` 会把它
    加进该 profile 的 `dsh.profile.bundles`，面板按纯 bundle 的路径启动
    `node <bin.js> --profile dsh-inproc-tui`。它跑在 harness 进程里，所以有 token 级流式、审批
    与追问、进程内模型切换；`requiresDsh = ">=0.2.0-rc.2 <0.3.0-0"`（实测 0.2.0-rc.2 / 0.2.1-rc.3 /
    0.2.2-rc.3；范围内按整条 0.2 线放行，0.3 才要求重新验证），
    旧宿主由清单跳过、退回下面的 ACP 终端。渲染层与 ACP 终端共用 `plugins/shared`
    （打包时随 tarball 一起进入 `package/shared`）。
  - **面板自带的第一方终端（ACP，兼容备用）**：`plugins/dsh-acp-tui`（包名
    `@nas200-timmy/dsh-acp-tui`，profile `dsh-acp-tui`）。它和其它条目走同一条安装与启动路径，
    只有两点不同：**装**的时候 `FirstPartyPlugins` 把 jar 里的 tarball 写到
    `<DSH_HOME>/.first-party-plugins/`，安装规格是 `file:<tarball>`（离线可用，没有 registry
    也不失败）；**起**的时候因为它是 bin 型，面板跑
    `node <profile>/node_modules/@nas200-timmy/dsh-acp-tui/bin/dsh-acp-tui.js`，并把
    `HSDL_DSH_ENTRY=<instance.dshEntryPoint()>` 与 `HSDL_TUI_ACP_PROFILE=acp` 放进环境——它自己
    spawn 该实例的 `dsh --profile acp`，用 ACP v1 通信，不 import 任何 harness 包。`requiresDsh` 是
    `>=0.1.2-rc.1`：ACP 的 profile 模板从那一版才有（实测 0.1.2-rc.1 / 0.1.7-rc.2 / 0.2.1-rc.3
    都能跑）。它是**绝对的兼容选项**，也是同实例版本来回变时的退路。新增第一方插件见
    [plugins/README.md](../plugins/README.md)。
    **两个 profile 别混**：它自己的 `dsh-acp-tui` 只存放文件，真正引导的是 harness 自己的 `acp`
    模板。账户的供应商路由/default model 由 launcher 写进"这次启动引导的 profile"，所以开终端时
    的启动计划是按 `acp` 建的（`DshTui.runProfile`），环境里也把同一个名字告诉它
    （`HSDL_TUI_ACP_PROFILE`）——写在存储 profile 里的路由，运行中的 harness 是看不见的。
  - **第一方终端的能力矩阵**（v0.5.92 起，P0/P1 继续打磨）：审批三档（`/yolo`：`ask` **默认** /
    `critical` / `full`，由 `plugins/shared/approval/policy.js` 判定，自动放行不绕过审计也不放宽沙箱）；
    默认 `ask` 只读自动、写文件/跑命令/敏感文件/计划都问，敏感文件含 `.env*`、`.ssh/`、`*.pem` 等且**任何档位都问**
    （`full` 也一样）；`web_fetch` 不按"只读"放行（URL 由模型拼，可以带数据出去）；
    路径先 resolve 再判断，`..` 不能绕过工作区；会话白名单键是"工具 + 完整命令/路径"，危险/敏感/计划
    不受白名单影响；`/allow` `/deny` `/rules` 是永久规则（实例 home 的 `.hdsl-tui/permissions.json`，随 `/data` 保留）。审批对话框
    四个选项（1 运行 · 2 当前会话默认运行 · 3 拒绝 · 4 拒绝并说明原因，理由会作为一条用户消息交给模型）。
    `shift+tab` / `/plan` 切换 Plan mode（进程内交给 harness，ACP 明说不可用）；`ctrl+s` 中途插入
    （进程内 `agent.steer()`，ACP 留在队列）；`/sessions` 是 `/resume` 别名，且只列有首条人类
    提示的会话（空会话不出现，列表标签就是那条提示；读不出的会话保留并标「标题未读出」，
    排序按 JSONL mtime）；`/yolo`、`/permission`
    无参时是列表选择（当前项标「当前」）；提示、补全候选与 `/` 命令菜单都画在**输入框下方**
    （菜单是底部内联列表，不是居中弹窗，不遮记录区也不隐藏光标）；Tab 补全覆盖命令、参数与
    `@` 路径（`@` 有界递归且不离开工作区）；空闲时终端不写任何字节（避免打断输入法组词）；
    面板 xterm 的字体栈带 CJK 等宽字体，全角字符与标点正好两格；`/provider` 在终端里增删 `llm-pi-ai` 的供应商（写 settings、
    密钥走 credentials，`--yes` 才落盘）。启动页是 dsh-TUI README 像素鲸鱼的静态帧（原 SVG 调色板）+ 模型/档位/计划/cwd/会话 +
    Kimi 风格按键提示。ACP 线路只保留协议允许的部分，其余命令会明确说明不可用，而不是静默失败。
  - **中文输入法**：xterm 6 在 composition 期间写屏会移动候选框；隐藏 textarea 的残留还会让
    229 键把旧输入整段重发（xtermjs/xterm.js#6078）。面板终端在组词期间缓冲 PTY 输出、结束后再刷，
    并在按键/组词结束后延迟清理 textarea 残留（`screenReaderMode` 除外），所以候选框不会漂，
    也不会突然把前面的文字复制进输入框。
  - **第一方终端的可靠性约定**（两个都适用）：粘贴走 bracketed paste，由 `keys.js` 解出
    `{name:'paste', text}` 交给 composer（CRLF 归一化后插入；对话框打开时忽略，不写进看不见的输入框）；
    握手/子进程死亡/信号退出都会还原终端并清掉定时器，**失败的启动不会留下一个常驻进程**；
    两个终端都会在最外层兜异常——进程内那个尤其重要，它跑在 harness 进程里，一个未捕获异常
    会把整个实例的 web 进程一起带走；审批与追问按 **waterfall + `request.agent`** 只答本会话的
    agent，别人的请求 `next()` 交给下游；`quit()` 先把未决的审批/追问以 cancelled 结算、再取消回合、
    最后有界等待关闭，所以在对话框前退出不会把 harness 挂住。ACP 那边额外有握手超时与单行长度上限
    （没有换行的垃圾行不会既吃内存又让首条消息永远解析不出来）——见 `plugins/README.md`。
  - **终端死了要留话**：子进程非零退出且**一个字节都没输出**时，应用日志写一条
    （`TerminalSession`，实例 / 退出码 / 没有输出），退出帧带 `failedWithoutOutput`
    （`docs/terminal-protocol.md §3`），前端在"会话已结束 (N)"下面补一句可行动的提示。
    面板自己结束的会话不算——那个非零码就是要来的（143 = SIGTERM），诊断它只会变成噪音。

## 10. 已知缺口

- **web ↔ 终端互斥**：**两个方向都成立**——开终端会同步停掉同实例的 web 运行时（品牌
  `BrandRuntime.stop` / ZCode `ZcodeRuntime.stop` / dsh `DshProcessManager.stop` 都等
  进程树退出），**启动**/停止/删除实例也会先结束它的终端（品牌与 ZCode 一直如此，dsh
  原先只做了"开终端→停 web"这半个方向，现已补齐，顺序与理由见 §4）。
  停掉 web 后不会自动恢复——恢复动作交给底栏「启动」（实例启动按钮）。
- **refresh 重连**：运行中的会话会直接发新票据，浏览器刷新后能接回同一进程并回放；
  但两个窗口同时 attach 目前都会收到广播、输入也会同时写入——v1 不做单控制端仲裁。
- **Windows 便利功能**：`POST .../reveal`（`target=home|workspace|log`）用
  `explorer.exe` / `notepad.exe` 打开，路径由服务端按实例记录计算，请求只能点名
  三个目标之一；dsh 的日志在内存里，`log` 返回 400。
- **ZCode / Claude Web 的真机终端**：两个适配器的命令形状有单测锁定，但「TUI 到底能不能
  在 PTY 里起来、中文与 resize 是否正常」只能在对着真实发行包时验证；Windows 现在也有
  PTY（ConPTY，Win10 1809 起），但品牌 TUI 在 ConPTY 上的长时间运行还没有人肉证据。
- **Claude Web 的官方内核是否存在**取决于发行包里有没有 SDK 的平台可选依赖
  （`@anthropic-ai/claude-agent-sdk-<platform>-<arch>`），没有时退回 PATH 上的 `claude`；
  `…/terminals` 端点会把可用性与缺什么如实报出来。
