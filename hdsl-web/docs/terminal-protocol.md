# 终端通道协议（Web Shell）

> 状态：v1（`protocol: 1`）。服务端 `TerminalWebSocket.PROTOCOL` 与本文是同一份契约；
> 改协议必须同时改版本号、本文和客户端，并在 Release 说明里写清楚。

浏览器里的终端覆盖层和仓库里的 Go 客户端（`client/hdsl-tui`）走的是同一套接口。
把它们写下来的原因很简单：这套东西以前只服务一个前端，前端和它一起改不会出事；
有了第二个客户端，一次重构就能悄悄打断它。

## 1. 鉴权与开关

所有 REST 调用都在 `/api/*` 下，走面板会话 Cookie（`hdsl_session`，`HttpOnly` +
`SameSite=Strict`，HTTPS 下带 `Secure`）。

终端相关端点还额外要求：

- **HTTPS**：面板此刻真的在讲 TLS，或打开了 `https.trust_forwarded_proto` 且请求带
  `X-Forwarded-Proto: https`。否则 403 `insecure_transport`。
- **Web Shell 总开关**：`web_shell.enabled` 为真。关掉时 403 `web_shell_disabled`。
- **开发者密码提权**：会话级 30 分钟滑动。没有提权时 403 `developer_elevation_required`。

错误体统一是 `{"error":"<code>: <说明>"}`；客户端按冒号前的 code 分支。

## 2. REST

### `POST /api/auth/login`

```json
{ "username": "admin", "password": "…" }
```

200 → `{"ok":true,"username":"admin"}`，并下发 `Set-Cookie: hdsl_session=…`。
401 → 用户名或密码错误（失败次数有预算，超限时 429 带 `Retry-After`）。

### `GET /api/health`

公开端点，用于版本与连通性检查：`{"status":"ok","version":"0.5.71","uptimeSec":123}`。

### `POST /api/developer/elevate`

```json
{ "password": "…" }
```

200 → `{"elevated":true,"expiresAt":"…"}`。要求 HTTPS，否则 409 `tls_required`。
其他：401 密码错误；409 `developer_password_not_set`（还没设置过）；429 连续失败被锁。

### `POST /api/developer/extend`

已提权的会话把这次提权**延长到 3 小时后**，不需要重新输入密码——长时间维护前用一次，
免得到期重输。**每张提权票只有一次延长机会**：已延长过的票再调本端点，403
`developer_elevation_extended`；重新 `elevate` 验证一次密码会发一张带新延长票的新提权，
且**保留还没走完的有效期**（延长期内续票不会被砍回 30 分钟）。要求 HTTPS + 当前已提权；
未提权时 403 `developer_elevation_required`（与终端入口同一个 code，客户端复用同一分支）。
200 → `{"elevated":true,"expiresAt":"…"}`。

### `POST /api/developer/revoke`

**立即停止**当前会话的提权，不等 30 分钟到期。刻意不要求当前处于提权状态——降级必须
总是可用，对未提权的会话是幂等空操作。要求 HTTPS。200 → `{"elevated":false}`。
网页（开发者模式页）与 hdsl-tui（主菜单 → 开发者提权）共用这两个端点。

### `POST /api/developer/agent-token`

用当前提权会话给 agent 签一张 bearer 票。需要已提权；没提权时 403
`developer_elevation_required`。请求体：

```json
{"label":"hdsl-tui","scopes":["read","mc"]}
```

`token` 只在这一个应答里出现一次：

```json
{"token":"…","id":"AbC123","label":"hdsl-tui","scopes":["read","mc"],
 "expiresAt":"2026-11-04T15:00:00Z"}
```

范围限于 `read|mc|instances|config|plugins`；有效期被当前提权窗口截断，面板侧可随时吊销。
hdsl-tui 把它和 `https://127.0.0.1:<端口>` 拼成一段提示词，并用 OSC 52 请求终端复制。

### `GET /api/developer/status`

开发者模式此刻的状态。两个客户端都在**打开终端之前**读它，好把对话框排成同一个顺序
（密码 → 内核 → 终端），而不是等到 403 才知道要提权：

```json
{ "devPasswordSet": true, "elevated": true, "expiresAt": "…", "webShellEnabled": true, "https": true }
```

- `devPasswordSet`：面板里到底有没有开发者密码。为假时提权永远不可能成功，客户端直接
  说明去哪里设置，不再问一个不存在的密码；
- `elevated`：按当前会话 Cookie 与来源地址算（30 分钟滑动续期）；
- `expiresAt`：这次提权的到期时间；未提权时**不出现**（可选字段，容忍缺失）；
- `webShellEnabled` / `https`：总开关与「面板此刻是否真的在讲 TLS」。

读不到这个端点（老面板 404）不算失败：客户端跳过预检，照旧直接开终端，让下面的
403 / 409 说话——服务端始终是最终权威。

### 实例列表

- dsh：`GET /api/instances` → `{"instances":[{"id","name","state",…}]}`
- 品牌：`GET /api/brands/{brand}/instances`，`brand ∈ {kimi, opencode, claude-web}`，
  元素形状同上（另带 `version` 等字段）
- ZCode（实验性品类）：`GET /api/zcode/instances`，元素形状同上（`state ∈ created /
  starting / running / stopped / error`）

v0.6.x 起元素还可能带（旧面板缺字段就是空/零，客户端据此显示“面板未报告”）：
`version`、`port`（dsh）或 `lastPort`（品牌）、`url`（浏览器挂载点）、`error`（启动失败
原因）、`account`（dsh 的供应商账户）、`terminalOpen`、`disabled`。

### `POST .../launch` 与 `POST .../stop`

三条路由与终端同前缀：dsh `/api/instances/{id}/…`、品牌
`/api/brands/{brand}/instances/{id}/…`、ZCode `/api/zcode/instances/{id}/…`。请求体 `{}`。

- `launch`：202 `{"state":"starting"}`；版本没装时先装再起，回
  `{"state":"installing","taskId":…}`；
- `stop`：202 `{"state":"stopping"}`，或 200 `{"state":"stopped"}`（只有终端在跑时）。

两者都可能先经过开发者守卫；被拒时客户端按 403 `developer_elevation_required` 提示去提权。
hdsl-tui 的实例菜单里就是这两项。

### Minecraft 控制台

MC 功能开启时才存在；关闭时整个 `/api/mc/*` 回 **404**（不是 403）。

- `GET /api/mc/instances` → `{"instances":[{"id","name","version","port","state",
  "consoleAvailable",…}]}`；
- `POST /api/mc/instances/{id}/start` / `.../stop`；
- `POST /api/mc/instances/{id}/console/ticket` → 与 agent 终端同形：
  - PTY：`{"mode":"pty","protocol":1,"ticket":"…","ws":"/ws/terminal/…"}`，客户端按同一套
    WebSocket 协议附着；
  - Windows 管道模式：`{"mode":"pipe"}`，**没有 ticket**，控制台只能在网页看（日志 + 输入框）；
  - 服务器没跑：409 `the server is not running`。

列表与 ticket 都要开发者提权。**离开控制台是 `Ctrl-] q`，不是输入 `stop`**——`stop` 是
服务器命令，会真的停服。hdsl-tui 在附着前把这句话单独显示一次；`Ctrl-] k` 在 MC 上下文里
等同离开（没有“停掉控制台”这个端点）。

`claude-web` 的内核就是 Claude Code CLI（社区引擎 `claude-web-engine` 或官方二进制），
所以它也有终端，只是要先选内核——见下面的 `terminals` 端点与 `?kernel=` 参数。

### `GET /api/brands/{brand}/instances/{id}/terminals`

Claude Web 专用：列出这次能进入的 Claude Code 内核。别的品类只有一个 TUI，回答 404。

```json
{
  "kernels": [
    { "id": "community", "label": "社区引擎（claude-web-engine）", "available": true, "detail": "<路径>" },
    { "id": "official",  "label": "官方 Claude Code",             "available": false, "detail": "缺什么" }
  ],
  "default": "community"
}
```

`available` 为假时 `detail` 说明缺什么，客户端据此只显示能用的那些；只剩一个可用时不必再问。

### `POST /api/instances/{id}/terminal`

品牌版本是 `POST /api/brands/{brand}/instances/{id}/terminal`，ZCode 是
`POST /api/zcode/instances/{id}/terminal`——三条路由的门禁、生命周期与响应体完全相同
（服务端由同一个 `InstanceTerminal` 实现）。可选查询参数：

- `?resume=<sessionId>`：TUI 里恢复某个会话（ZCode 第一版忽略它）；
- `?kernel=community|official`：Claude Web 选哪个 Claude Code 内核。不传按面板推荐的
  默认（社区引擎优先）；传了但那份没装，返回 409 `terminal_failed` 说明缺什么，**不会**
  静默换成另一个内核。

请求体可以是 `{}`。

客户端在界面上把开发者密码、内核选择都画成**终端窗口之前的对话框**：密码通过（必要时先
设置）→ 需要时选内核 → 才换成终端。协议没变，顺序是客户端行为——两个客户端都靠上面那个
`developer/status` 把顺序对齐，而不是等到 403 才知道要提权。

面板返回（四个平台同一种，包括 Windows 10 1809+）：

```json
{
  "mode": "pty",
  "protocol": 1,
  "instance": "DClDN1J3w0C_",
  "brand": "dsh",
  "ticket": "…",
  "ws": "/ws/terminal/<ticket>"
}
```

`mode:"desktop"` 是**旧面板**的形态（Windows 上把控制台开在面板所在机器的桌面，响应体只有
`instance`/`brand`/`hint`，没有 ticket）。当前版本的 Windows 面板也返回 `mode:"pty"`；
客户端保留这个分支只是为了连老面板。

`ws` 是**面板内部路径**（根相对），不是公开地址：面板挂在反向代理的 `/hdsl` 下时它仍是
`/ws/terminal/<ticket>`，由客户端把挂载点前缀加上去（浏览器读 `<base href>` /
`window.__HDSL_BASE__`，Go 客户端用它连接地址的 path 部分）。客户端不要用
“根相对路径替换整个 path”的解析方式，那样会丢掉 `/hdsl`，表现是健康检查、登录、实例列表
都正常，只有终端连不上。服务端也可以返回完整的 `ws://` / `wss://` 地址，此时客户端按原样
使用，不再加前缀。

重复调用不会重开会话：该实例已有运行中的终端时，只是再发一张 ticket（等价于 attach）。

错误：

| HTTP | code | 含义 |
|---|---|---|
| 403 | `insecure_transport` | 面板不在 HTTPS 下（或反代未开 `trust_forwarded_proto`） |
| 403 | `web_shell_disabled` | 总开关关了 |
| 403 | `developer_elevation_required` | 开发者密码提权过期 |
| 409 | `developer_password_not_set` | 还没设置过开发者密码 |
| 409 | `terminal_already_open` | 同实例已有终端（正常路径是重连，不该看到这个） |
| 409 | `terminal_failed: …` | 启动 TUI 失败，冒号后是原因（客户端要把这段原因原样给用户，它是可行动的那半句） |
| 409 | 无 code 前缀 | dsh 实例没装终端插件：整个错误体就是一句中文说明（`未安装终端界面插件，请先到实例的「安装」页安装一个 TUI 插件（推荐 …）。`）。它不带 `code:` 前缀，所以客户端要按这句话本身认出来；语义上是**策略拒绝**，重试无用 |
| 409 | `terminal_unsupported_platform: …` | 这台机器没有伪终端：Windows 上 build < 17763（或 `CreatePseudoConsole` 不在），Win7/8.1 属于此列。冒号后带实际 build 号，**不回落任何桌面窗口** |
| 400 | `confirmation_required` | 仅旧面板的桌面终端模式会回这个；当前版本不会再出现 |
| 404 | `instance not found` / `no such instance` | 实例不存在 |

### `DELETE /api/instances/{id}/terminal`

结束该实例的终端（杀进程树）：`{"ok":true,"killed":true}`。不需要提权（与打开分开）。
品牌与 ZCode 的写法同上：`DELETE /api/brands/{brand}/instances/{id}/terminal`、
`DELETE /api/zcode/instances/{id}/terminal`。停止或删除实例时面板也会连带结束它的终端。

## 3. WebSocket

`GET /ws/terminal/{ticket}`，升级为 WebSocket。握手必须带**与申请 ticket 时同一个**
面板会话 Cookie：ticket 里记录了申请它的会话 token，两者对不上就 1008 关闭。

- **ticket**：一次性、30 秒有效、绑定实例与会话；握手成功即失效。URL 里会出现它，
  所以它只够开一次连接，事后无效——这也是会话 Cookie 不进 URL 的原因。
- **二进制帧**：两端都是 PTY 原始字节，不做任何包装。
- **文本帧**：JSON。客户端发 `{"type":"resize","cols":120,"rows":40}`；
  服务端发：

```json
{"type":"ready","protocol":1,"instance":"…","brand":"…"}
{"type":"exit","code":0}
{"type":"exit","code":1,"failedWithoutOutput":true}
```

- `exit` 的 `failedWithoutOutput` 只在为真时出现（可选字段，见 §4）：子进程**非零退出且一个字节
  都没输出**——那正是"插件没装好/参数被拒"的样子，屏幕空是因为它从没打印过，而不是查看者错过了
  输出。面板自己结束的会话不带这个字段（非零码是要来的）。

- **断开**只代表 detach：agent 继续跑，重新走一次 `POST …/terminal` 拿新 ticket 即可接回，
  并会先收到最近的输出回放（服务端保留 256 KB）。
- 关闭码：1008 ticket 无效/不匹配；1011 该实例没有运行中的终端；1000/1001 正常关闭。
- 客户端应忽略 `ready` 里的未知字段；`protocol` 不是自己认识的版本时警告并继续尝试。

## 4. 版本策略

- `protocol` 只在**不兼容**时自增：新增可选字段不算。
- 服务端先发新字段，客户端后跟上；客户端必须容忍未知字段。
- 客户端与面板同版本号发布；`hdsl-tui --version` 打印自己的版本与 `protocol`。