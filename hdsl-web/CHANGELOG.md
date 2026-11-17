# 更新日志

版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)；每条记录「改了什么」与「为什么」。

## 未发布 — legacy 表面与 SSR 调优

- SSR 并发从写死 2 改为默认 4，并允许在 `server.yaml` 的 `plain.ssr_workers` 调到 1–16。
  每个渲染仍是短生命周期 Node 进程，watchdog 10 秒强杀；之前 2 个 worker 在长会话里
  预渲染偏慢，而旧实现的无界队列才是会打爆后端的原因。
- IE11/legacy 外壳对齐 HDSL-web 自己的 `monet.css`（与桌面版 `blue.css` 同源）：`#4352A5`
  主色、`#F5F2FA` 80% 卡片、4px 圆角、HDSL 字体栈、填充式输入框与 36px raised 按钮；
  登录/错误页改成 420px 居中卡片，补上 focus/active/disabled 状态。

## v0.6.91 — 2026-11-16 · 纯文本版增强、SSR 可靠性与终端聊天修复

> `/plain` 从保底表单页升级成真正可用的 legacy 表面：fragment 长轮询、服务端 markdown、
> 提权闸与 HMCL 外壳；同时把 SSR 和终端聊天里几处会误伤体验的问题修掉。
> 设计见 [docs/plain-surface.md](docs/plain-surface.md) 与 [docs/chat-tui.md](docs/chat-tui.md)。

### 纯文本版（legacy v1）

- `plain.enhanced`（`server.yaml`，默认开）＋ `GET/PUT /api/settings/plain`（写要开发者
  提权）＋ 设置 → 通用里的开关；下一次 `/plain` 请求立即生效。
- 增强模式下会话列表与转录由 `/plain/f/...` fragment 提供，`?wait=1&rev=` 长轮询，
  `poll.js`（ES5）换入新 fragment；发送区只在 markup 变化时替换，避免输入被刷掉。
- HMCL 风格外壳：`web-legacy/legacy.css` + 默认壁纸；CSP 分“纯 HTML”与“轮询器”两档，
  script-src 只允许 self；链接跟随 `HDSL_BASE_PATH`。
- 发送与新建会话要求活跃的开发者提权：`/plain/elevate` 密码表单、一次性延期到 3 小时、
  `stop` 结束；提权只走 HTTPS。未提权时发送区变成提示，fragment 带 `data-elev` 让轮询器停。
  `auth.disabled`（仅回环）没有登录也就没有提权，按自身证明处理。
- 服务端 markdown 用面板自己的前端渲染器（react-markdown + 同一份 sanitize schema +
  rehype-highlight），与 SPA 同观感；代码块只保留语言标签，不渲染复制/下载按钮。

### SSR 可靠性

- Node 解析改为「面板托管运行时优先，PATH 兜底」：`<data>/hdsl/runtimes/*/node` 按
  mtime 取最新，Windows 裸部署的托管 Node 不再因为不在 PATH 而静默退回 `<pre>`。
- 渲染改为异步：`ChatTurnManager` 在 turn 持久化后触发 finalizer，`MarkdownSsr.prefetch`
  在后台池里渲染；`render()` 只读缓存，请求不再为每条消息等待 Node 进程。
- 进程并发上限 2；watchdog 在 10 秒后强杀渲染进程，修掉「先读 stdout 再 waitFor」导致的
  无限阻塞（stdin 写入也在同一超时保护内）；任何失败仍降级为转义 `<pre>`。
- `GET /api/settings/plain` 返回 `ssrAvailable`，设置卡片显示「Markdown 预渲染：可用/不可用」，
  不再静默降级。

### 终端客户端（`hdsl-tui`）

- DeepSeek 预设的 `blocks` 时间线终于能渲染：思考可展开，服务端工具显示
  「搜索：query · N 条」，来源按 `tool_use_id` 配对；之前只读 `parts`，Ctrl-O 没有任何
  效果，思考与来源也完全不显示。
- 命令反馈不再被补全遮住：Enter 执行后清空 completion，`/search` 先检查搜索配置与
  DeepSeek 预设；补全列表补上 `/clear`、`/resume`、`/rename`、`/config` 等别名。
- 聊天页改用客户端一致的蓝底；用户行 `❯` 青色、助手行 `●` 绿色，摘要行带 `▸/▾`。
- 折叠状态默认露出最后两行思考，生成中标题带 `生成中 Ns`，长思考不再像卡住；
  `Ctrl-O` 展开全文。

### 安全

- 修掉 `/plain/elevate` 的开放重定向：`next` 只接受 `<basePath>/plain` 前缀，其余回落
  `/plain`；测试断言外部 `next` 的 Location 必须是 `/plain`。
- 安全文档补充：品牌自带终端（PI WEB、ZCode）不受面板终端门约束。

### 验证

- `./gradlew test` 全量通过；`npm run typecheck` / `compat` / `build` 通过。
- `scripts/api-smoke.py` 43/43。
- 终端聊天：假 provider 每 0.3 秒发推理增量，PTY 确认中途出现「思考中」+ 思考行、
  结束后只剩最终回答；DeepSeek blocks 会话确认 Ctrl-O 展开思考与来源。
- 纯文本版：提权闸、fragment 版本推进、轮询踢出、SSR 与降级均有测试覆盖。

## v0.6.90## v0.6.90 — 2026-11-16 · 终端客户端聊天（P0 + P1）

> `hdsl-tui` 的主菜单第一项现在是聊天：单列消息流、贴底输入框、斜杠命令、Markdown、
> 思考/搜索折叠，交互参考 Kimi Code / Qwen Code 这类终端 agent。服务端没有改动，
> 面板 jar 与容器只跟版本号走。设计见 [docs/chat-tui.md](docs/chat-tui.md)。
>
> **2026-11-16 同版本重发（仅客户端）**：修长会话读取截断与品牌列表写死。面板 jar、
> Release 里的 jar、容器镜像都不变；只替换 Release 里的 `hdsl-tui` 二进制与源码包。

- 修复长会话打不开：`internal/panel` 的 HTTP 读取把**错误体**的 64 KiB 上限套到了所有
  响应上，会话 JSON 超过 64 KiB 就被截断，解码报 `unexpected end of JSON input`。
  成功响应改为 64 MiB 上限，超过时给出明确错误；`--verbose` 只打印响应前 512 字节。
- 品牌实例列表不再写死：已知品牌保留手写顺序，`/api/brands/capabilities` 报出的新品牌
  自动追加到菜单；能力接口缺失的旧面板用 `qwen/mimo/minimax` 的纯 TUI 兜底，
  不再把它们当网页实例显示“启动/停止”。
- 测试：>64 KiB 会话解码、品牌顺序与 TUI 兜底；`go test ./...` 通过。
- 手工验收：数据库里放一个 460 KB 的会话，客户端聊天页正常打开；修复前同一会话报
  `unexpected end of JSON input`。

- 主菜单新增「聊天」；没有会话时选助手新建，有会话时打开最近更新的一条。
- `internal/panel` 新增聊天 REST 方法（助手、会话、发送、停止、重新生成、编辑、标题、设置）。
- 新增通用 `/ws` 网关客户端：cookie 握手、`subscribe`、心跳、事件通道；聊天订阅
  `chat` 与 `chat:<id>`。
- 新增 Markdown → ANSI 渲染器：标题、粗体、行内代码、代码块、列表、引用、链接、分隔线；
  只改前景/属性、不动背景，`VisibleWidth` 已有的宽字符与 SGR 处理直接复用；控制字符会被丢弃。
- 新增多行输入组件：光标、`Ctrl-J` 换行、`↑/↓` 在首/末行走历史、`Ctrl-P/N` 额外历史键；`Enter` 发送。
- 聊天页：流式增量、停止、思考/搜索折叠（`Ctrl-O`）、会话切换（`Ctrl-B`）、
  斜杠命令（`/new /sessions /assistant /model /title /search /stop /regen /edit /copy /settings /status /help`）。
- 键位与命令对齐 Kimi Code 的习惯：`Ctrl-J` 换行、`Ctrl-O` 展开、`↑/↓` 在首/末行走历史、
  `Ctrl-C` 停止/双击退出、`Ctrl-D` 退出、`Esc` 先取消再返回；命令补上别名
  `/clear`（= /new）、`/resume`（= /sessions）、`/rename`（= /title）、
  `/config`（= /settings）、`/h` `/?`（= /help）。
- 提权：进入聊天时询问一次开发者密码；读取聊天设置（默认模型、搜索、每日限额）需要它，
  聊天本身只需要登录。`Ctrl-E` 可随时重新提权。
- 识别开发者守卫 `all` 档：从 `/api/developer/status` 读到 `guardScope=all` 时，聊天 API
  全部需要开发者密码，客户端在进入聊天前强制要求提权，拒绝就说明原因；会话恢复遇到 403
  不再误判为登录失效（用白名单里的 developer status 确认 cookie 仍有效）。
- 不做图片渲染与附件上传/下载；消息里的附件只显示文件名。
### 验证

- `go test ./...`、`go vet ./internal/... ./cmd/...` 通过。
- 新增测试：Markdown（样式/宽字符/代码块/控制字符）、TextArea（换行/光标/历史/
  首末行箭头）、聊天 REST 往返、WebSocket 握手 cookie/订阅/投递、守卫 all 档判定。
- 手工验收：本地面板 + PTY 打开聊天，确认会话标题、输入框、发送请求到达服务端与错误回显；
  另用 `developer_guard.scope=all` + TLS 验证：恢复会话不再重问面板密码 → 进聊天 →
  开发者密码 → 提权成功 → 会话列表/标题正常。

## v0.6.89 — 2026-11-15 · 无 JavaScript 保底页与浏览器能力闸门

> 这一版的主题是“极致兼容”：现代聊天页之外，再给 w3m / lynx 和 Windows 7 上的旧浏览器
> 留一条能登录、看会话、发消息、下载附件的路。发布物：fat jar 与容器镜像同步更新。
> 设计见 [docs/plain-surface.md](docs/plain-surface.md)。

- 新增 `/plain` 纯文本界面：登录表单、会话列表、历史、发送、附件下载；发送后 303 回会话页，
  手动刷新或 `?auto=1` 的 5 秒 meta refresh 看结果。附件只给下载链接，P0 不做上传。
- 登录复用 `AuthService` 与 `AuthFilter.sessionCookie`（`HttpOnly` + `SameSite=Strict`），
  失败限流与审计同 `/api/auth/login`；所有 POST 校验 `Origin`/`Referer` 与 `Host` 一致。
- 入口能力闸门：`web/index.html` 先跑一段 ES3 脚本，缺 `fetch` / `WebSocket` / `flex` 就跳
  `/plain/old`，不再先下载 bundle 再白屏；`<noscript>` 直接去 `/plain`。
- 服务端 UA 兜底：w3m / lynx / links 打开 `/` 时直接 302 到 `/plain`，`MSIE` / `Trident/`
  302 到 `/plain/old`——IE ≤ 9 不解析 head 里的 `<noscript>`，只靠 meta refresh 不可靠；
  body 里另有一个可见的纯文本版链接。其他浏览器仍走能力闸门，不被 UA 猜测误伤。
- `/plain/old` 是友善提醒页：说明不是服务器故障，给出「继续用纯文本版」与「换现代浏览器」
  两条路，附 Windows 7 的 Firefox ESR / Chrome 建议版本。
- 页面不带脚本，动态内容全部转义；响应头带 `no-store`、`nosniff`、`X-Frame-Options: DENY`
  与 `default-src 'none'` 的 CSP。所有链接跟随 `HDSL_BASE_PATH`。
### 验证

- `./gradlew test` 全量通过（1533 个用例）；`npm run typecheck` / `npm run compat` 通过。
- `scripts/api-smoke.py build/libs/hdsl-web-0.6.89.jar`：43/43，新增 `/plain` 与
  `/plain/old` 两项。
- `PlainChatTest`：登录表单发 cookie、公开提醒页、SPA 入口带闸门、w3m/lynx/IE 的 302
  与 Chrome 的 200、`/plain/new` → 表单发送 → 同一轮回复在页面出现。
- 手工起 jar 用 curl 走通登录、新建会话、会话页与提醒页。

## v0.6.88 — 2026-11-15 · 聊天时间线、附件预览、搜索源选择与 ZCode 版本管理

> 聊天页把思考与搜索收进统一时间线（普通协议和流式也走），附件从“新标签页”换成侧栏预览，
> 联网搜索可以按消息选服务商；ZCode 品牌补上版本目录、实例最低版本、credential-secret 与
> 密钥轮换。发布物：fat jar 与容器镜像同步更新。这一轮提交里不少是同一功能的连续迭代，
> 下面按**最终行为**归纳，不再按提交逐条记录。

### 聊天时间线

- 普通 OpenAI 协议也按轮记录思考与工具调用（`thinking` / `tool_calls` / `tool`，带耗时、
  查询词、命中数和来源），前端用统一的 `Timeline` 渲染，DeepSeek 官方协议继续用
  `BlockTimeline`。
- 所有步骤标题默认列出、各自折叠；流式时最后一段思考默认展开。点开某一步才显示推理
  正文或来源列表，正文始终在时间线下方。
- 流式阶段与落库后的消息复用同一条时间线：思考/搜索一出现就分段，不再先显示「思考 N 字」
  再整段跳成卡片；流式消息已落到 `session.nodes` 时也不会回退到旧布局。
- 交互收敛：展开/收起固定在行尾同一列，按钮不再左右跳；点击思考正文也能收起；消息宽度
  不随展开状态变化；标题行的双击保护去掉。
- 旧消息没有 `parts` 时回退到原来的 `reasoning + tools` 布局。

### 引用与正文

- 引用同时支持 `[citation](6 位短 id)`、`[citation](1)` 数字索引和 `[citation](https://…)`
  直接 URL；两个短 id 连写（`8e788c→9e788c`）时取第一个。
- 模型只是在正文里讨论 `<search>` 标签、没有可解析的 `<query>` 时，不再从 `<search`
  起把后面的回答整段截断。
- 模型把 `search_web` 写成 `<search><query>…</query></search>` 时识别为伪工具调用并从
  正文剥离；流被截断时也会解析已经出现的 query，不再把这类标签当答案展示。

### 搜索

- 输入区放大镜改成选源菜单：先选服务商（博查 / DeepSeek / 智谱 / Tavily / Brave / Exa），
  选中后按钮才亮起并显示当前来源；未配置 key 的服务商置灰。选择随消息以 `searchProvider`
  发送，覆盖聊天设置里的默认源，并记在 `localStorage`。
- 重新生成（就地 / 截断后重发）使用当前搜索开关与来源，不再沿用会话上一次发送的设置。
  之前打开搜索再点重新生成仍然不搜，因为发送记录里还是上一次的 `false`。
- 工具轮次更耐用：连续调用到上限后不再直接报 `tool_rounds_exhausted`，而是最后一轮不下发
  tools，附「预算已用完，请基于已有结果回答」再请求一次，避免丢掉已经检索到的内容。
- 提示词改为「先搜一次，结果够就回答；最多补搜一次，不要重复 query；`scrape_web`
  每轮最多一次」，不再鼓励模型并行多搜、连续三轮。
- 搜索卡片能展开来源：工具结果前面有一句中文提示，之前直接 `JSON.parse` 全部失败；
  后端改为截取首尾花括号解析 JSON，写入 `hits` / `sources`。
- 账户 key 带旧 vendor 前缀（如 `alibaba-cn|alibaba-token-plan`）时按 label 唯一兜底，
  避免旧缓存 key 找不到账户。

### 附件预览

- 聊天页新增右侧预览栏（窄屏为覆盖式抽屉）：图片、音频、视频、PDF 原生渲染，文本与
  Office 抽取文本分页预览；支持下载、新标签页打开、Esc 关闭。
- CSV 默认按表格渲染（RFC 4180 的引号、转义引号与字段内换行；自动识别 `,` `;` 制表符），
  首屏 200 行 × 30 列，可切回原始文本；只在预览层解析，发送内容不变。
- `GET /api/chat/attachments/{id}` 支持 `?download=1` 与单区间 `Range`（206），PDF 查看器
  和视频拖动不再整段下载；新增 `GET …/text` 按字符分页读取文本或抽取文本（默认 64 Ki 字符，
  `offset` / `limit` 可调）。
- 开关在「聊天设置 → 数据与备份 → 附件预览」，默认开启；关闭后回到原来的新标签页行为。
  偏好存 `localStorage`，不依赖需要开发者提权的设置接口。

### 聊天设置

- 导航从 7 个 tab 收到 5 个：个人资料 / 模型（模型服务 + 默认模型 + 预设助手）/
  联网搜索 / 数据与备份 / 安全与网络；切换时内容区回到顶部。
- 设置页恢复铺满整页：导航贴左、卡片用满内容区，只给说明文字限宽 76ch，长句不再横跨
  整屏；内边距收紧到 16/18px。
- 布尔项统一成开关（44×24、`role="switch"`）：附件预览、保留检索记录、DeepSeek 预设
  助手、每日备份；模型能力矩阵仍用勾选框，因为那是多选列表。
- 联网搜索拆成「联网搜索 / 测试搜索」两张卡片，工具说明改成浅色 callout，服务商与返回
  条数两列对齐，API key 独占一行；供应商选择改为单列，收住窄屏横向溢出。

### ZCode

- 新增版本目录 `GET /api/zcode/catalog`：GitHub Release、只有 tag 没有 Release 的版本、
  未发布的 `main@<sha>`，每项带凭据格式（v1 / v2）、是否需要 credential-secret、是否已构建、
  是否已安装。
- 实例新增 `version` / `minVersion` / `minVersionAt` / `credentialFormat`：创建时可指定
  已构建的 ref 或跟随 `current`；成功启动后下限前移。切换版本要求实例已停止、目标已构建，
  且不能低于下限或倒退凭据格式；低于下限的项在页面上置灰并给出原因，后端返回 409。
- 3.18.x 的 credential-secret：面板自动生成一次 32 字节 base64url 根密钥（0600），已存在
  不覆盖，也支持 `ZCODE_CREDENTIAL_SECRET(_FILE)` 指定外部密钥。已有 `enc:v2:` 密文但
  密钥缺失时拒绝启动并提示恢复原密钥或重置凭据，而不是生成一个替代密钥。
- 实例菜单新增「更换密钥」（reset）：要求实例已停止；删除旧 `credentials.json` /
  `credential-source` / `credential-secret`，可自动生成或从种子派生，可选同时清除已保存
  的 API Key。旧密文不重新加密，之后需要重新登录 / 重填 key。
- 启动探活：`Local:` 就绪行之后再发一次 HTTP 请求，失败时把实例标为 error 并附日志尾部，
  不会先报 running。
- 构建对话框不再预填 `main`：Chrome 的 `<datalist>` 会按当前值过滤，预填后下拉只剩一项；
  改为空值 + 前 10 个可点击版本按钮，标注未发布 / 仅 tag / 已装。

### 其他

- 入口 HTML 改为 `no-store`，带 hash 的 `assets/*` 改为长缓存 + `immutable`：部署新版本后
  不会继续加载旧 bundle。
- Agent 票新增 `chat-read` / `chat-write` / `chat-search` 三个 scope：可以读会话、发消息、
  跑搜索测试，但删除、设置、备份、账户、头像和助手的写操作仍对 agent 关闭。
- 通用文案「游戏」统一改为「实例」。

### 验证

- `./gradlew test` 全量通过（JDK 17 / 21 由 CI 矩阵覆盖）；`npm run typecheck`、
  `npm run compat`、`npm run build` 通过。
- `scripts/api-smoke.py build/libs/hdsl-web-0.6.88.jar`：41/41。
- 覆盖这些改动的用例：`ChatApiIntegrationTest`（强制下载、Range 206、文本分页、chat scope）、
  `ChatContractTest`（`/text` 的 `chat-read` scope）、`ZcodeCatalogTest` /
  `ZcodeCredentialsTest`（版本目录分类、下限与凭据格式闸门、secret 生成 / 覆盖 / 轮换）、
  `ChatSettingsTest` / `ChatToolCallTest`（搜索源设置与伪工具调用解析）。
- 已知边界：CSV 预览首屏 200 行 × 30 列，完整内容走下载；附件文本分页默认 64 Ki 字符；
  ZCode 的版本下限是**按实例**的锁，新建实例仍可选旧版本。

## v0.6.87 — 2026-11-15 · latest_reminder、附件预览与终端兼容性修复

> 修的是 v0.6.86 上线后真实踩到的三类问题：DeepSeek 官方助手在中文环境直接 400、长文本
> 粘贴阈值过低、文本附件预览乱码；同时把进程内终端的版本闸门从「逐个枚举 rc」改成「放行整条
> 0.2 线」，让 `dsh@next` 0.2.2-rc.3 能重新装上终端。

### DeepSeek 官方助手

- `latest_reminder` 的语言字段只发服务端接受的名字（`中文` / `English`），星期和地区跟随同一
  语种。此前中文环境发送 `Chinese`，服务端按语言名白名单校验，整轮返回 400。
- 回放历史时清洗旧快照：语言名不合法就按原日期重建。只改生成逻辑的话，数据库里已经存下的
  `…,Web,Chinese` 会让老会话每次发送都继续 400。

### 聊天附件与粘贴

- 长文本转附件的阈值从 100 字提高到 **1800 字符**；100 字会把正常长度的粘贴也拆成附件。
- 文本附件下载显式声明 charset，并按内容嗅探 UTF-8 / GB18030：中文 Windows 的浏览器不再把
  UTF-8 粘贴文本按 GBK 猜成乱码，旧的 GBK 文本文件也能正常预览。图片、PDF 等二进制不加。

### 进程内终端（dsh-inproc-tui 0.2.3）

- 不再逐个枚举 dsh rc：插件 peer 改为 `^0.2.0-rc.1`，面板闸门改为
  `>=0.2.0-rc.2 <0.3.0-0`。此前枚举到 `0.2.2-rc.1` 为止，`dsh@next`（0.2.2-rc.3）既被面板
  判成不匹配，也会被 dsh 以 `incompatible` 拒绝安装。
- 0.2.2-rc.3 实测：`file:` tarball 安装、PTY 启动、会话创建与 `/help` 正常。0.3 是新一条线，
  接缝要重新验证，所以上限仍卡在 0.3 之前（用 `<0.3.0-0`，避免 `includePrerelease` 放进
  `0.3.0-rc.1`）。
- 测试：`DshTuiPluginsTest`、`DshPluginRequirementsTest` 覆盖整条 0.2 线与 `0.3.0-rc` 边界。

### 验证

- `./gradlew test` 全量通过；`npm run typecheck` / `npm run compat` / `npm run build` 通过。
- 新增 `ChatAttachmentsTest`（编码嗅探）和 `ChatApiIntegrationTest` 的上传-下载 charset 断言。

## v0.6.86 — 2026-11-14 · FreeBSD 终端、平台声明与 DeepSeek 官方预设助手

> FreeBSD 上面板能跑，但 PTY 终端一直是缺口；这一版在 **FreeBSD 15.1 真机**上补齐并验证，
> 同时把「哪些平台/功能有已知差异」做成面板里的声明。dsh / opencode / ZCode 在 FreeBSD 上
> 仍不可用（上游限制），面板会直接拒绝安装，而不是让人等到失败。
>
> 发布物：fat jar 仍是 69 MB（v0.6.85 起就没有再变大）；容器镜像同步更新。
>
> **2026-11-14 同版本重切**：在 FreeBSD 发布的基础上补入 DeepSeek 官方预设助手与官方搜索，
> 覆盖原 v0.6.86 的 jar、Release 与容器；当时未发布 v0.6.87，后续修复由下面的 v0.6.87 接续。

### DeepSeek 官方预设助手

- 新协议 `deepseek-anthropic`：`/anthropic/v1/messages`，`thinking` 开关、服务端工具
  `deepseek_web_search` / `deepseek_read_url`、`safety_check`、服务端标题与
  `latest_reminder` 角色；搜索不再走面板的客户端工具链。
- 内置「DeepSeek 官方」角色，复制出来的角色继承 preset/protocol；只接受 DeepSeek 账户与模型，
  没有账户时给出添加入口，后端硬拒绝跨供应商回退。
- 聊天设置 → 预设助手：`deepseekPresetEnabled` 开关；关闭后隐藏预设及复制角色，
  会话保留，新发送返回 `preset_disabled`。
- schema v4：助手 `preset/protocol`、会话 `title_auto`、消息 `blocks_json/sources_json/
  latest_reminder`、附件 Files API 缓存字段。
- 会话 API 暴露 `blocks_json`，前端按 thinking / server_tool_use 的原始顺序渲染多段思考与
  搜索时间线；流式新增 `chat-block` 边界事件。
- 正文里的 `[citation:N]` 渲染成来源链接；来源复用现有 `sources` / `tools` 结构。
- 图片走 Files API（`file_id` 缓存）；文档仍走文本抽取。
- 兼容 DeepSeek 在 OpenAI 协议下把工具调用写成 DSML 文本的情况：能解析就转成 ToolCall 并从
  正文剥离；解析失败或工具被禁用时返回明确错误，不再保存空消息。
- 真 key 已跑过请求校验阶段：根据服务端 422/400 反馈修正了 user `content` 形状、
  `latest_reminder` 五段格式与 DSML 块匹配。
- 手机端消息改为「头像列 + 气泡列」，会话输入框跟随消息列左对齐。

### FreeBSD 终端与平台声明

- `Pty.spawn` 增加 FreeBSD 分支，复用 `PosixPty`（`openpty` + `posix_spawn`），没有新增依赖：
  `openpty` 在 base 的 libutil，`posix_spawn` 与 `posix_spawn_file_actions_addchdir_np`
  在 libc（FBSD_1.7）。
- 两个平台常量：`F_DUPFD_CLOEXEC` FreeBSD = 17；`TIOCSWINSZ` 与 macOS 相同（`0x80087467`）。
- 真机验证：openpty/spawn、`stty size`、`TIOCSWINSZ` resize（80×24 → 120×40）、stdin 回显、
  退出码透传（`exit 3` → 3）全部通过；面板服务换 jar 重启后健康检查正常。
- **托管 Node 不再装错**：`NodeRuntimeManager.platformTag` 原先对 FreeBSD 落到 `linux-x64`，
  会下载一个跑不起来的 ELF 并登记为已安装。现在明确拒绝并提示用 `pkg install node npm`
  的系统 Node（品牌安装器本来就会回落到 PATH）。
- Minecraft 控制台仍保持 pipe（日志 + 命令框）；启动横幅从「no PTY on this platform」
  改成「PTY console not enabled on this platform」，不再把策略说成平台能力。
- **dsh 实例在 FreeBSD 上不可用（上游硬限制）**：`dsh-app-boot` 启动时会 require
  `node-addon-require-builtin`，这个原生 addon 没有 FreeBSD 二进制；上游源码的
  `platform.cc` 对 FreeBSD 返回 `UnsupportedContextReader`，所以自己编译也补不上。
  0.2.x 全系（含 rc.1/rc.2/rc.3）都依赖它，面板现在在 FreeBSD 上直接拒绝安装，
  不再下载一套永远起不来的依赖树。
  试过「从源码构建 addon」这条最后手段：addon 能在 FreeBSD 上编出来并跑通，但
  `dsh-session-persistence-jsonl` 的写租约调用 `node-addon-system/flock`，它在
  FreeBSD 上**主动抛错**（`ERR_FLOCK_UNSUPPORTED_PLATFORM`），不是缺库。继续往下
  就要拆 dsh 的锁语义，不做；该实验开关已撤除，只保留结论。
- **opencode 在 FreeBSD 上直接标为不可安装**：它只发布 Linux / macOS / Windows 的
  二进制包，没有 FreeBSD 构建，安装会在 postinstall 失败；品牌页现在会提前说明。
- **ZCode 在 FreeBSD 上直接拒绝构建**：构建会安装 Electron 桌面端，Electron 没有
  FreeBSD 构建；与其跑十分钟再死在 postinstall，不如一开始就说清楚。
- **面板里能看到平台声明**：设置 → 通用新增「平台兼容性」（`GET /api/system/platform`），
  显示内核版本、整体结论（支持 / 受限 / 不支持）和逐项差异；`procfs` 在运行时探测，
  挂载了就不再报「系统指标为空」。只有未纳入发布验收的平台会展开这张卡。
**转码页面：手机端布局**

- 参数面板在窄屏拆成单栏，左侧分区导航改成横向 chip 条，编辑器与实时命令行上下排列；
  参数网格单列，预设管理列表改为上下布局。
- 准备文件两列改整行堆叠，媒体浏览器路径与「添加当前文件夹」不再挤出屏幕；待转码列表
  标题与操作按钮允许换行。
- 队列任务行在窄屏重排为三行，详情卡移到列表下方；工具页卡片整行；目录选择器头部换行。
- 子侧栏横向标签条在切换后自动把当前项滚到中间，不再只露出半截。

- 已知限制（写进 `docs/deployment.md §12`）：系统指标页大部分为空（依赖 `/proc`、`/sys`）、
  没有 GPU、uid 分离未实现、MC 控制台不是 PTY。
- 验证：`./gradlew test` 全绿（1486 用例）；`npm run typecheck` / `build` 通过；
  `python3 scripts/api-smoke.py` 43/43；FreeBSD 15.1 真机验证终端、平台声明与
  dsh 的拒绝路径。

## v0.6.85 — 2026-11-14 · 面板聊天、三个 TUI 品牌与终端加固

> 这一版把面板从「管理实例」扩成「自己也能用」：内置聊天页（多助手、流式、附件、联网搜索），
> 以及 Qwen Code / MiMo Code / MiniMax Code 三个纯终端品牌。聊天数据只在 `<data>/chat`；
> 不支持 SQLite 原生库的平台会让聊天降级为 404，面板照常启动。
>
> 发布物注意：fat jar 从 33 MB 涨到 **69 MB**（SQLite 原生库 + PDFBox/POI），容器镜像同步变大；
> 聊天备份包含 API key，按敏感数据对待。
>
> 场景：不想为了和模型说几句话再开一个网页聊天；面板里就有账户与 key，缺的只是一个
> 会流式输出、能保留历史、能按助手隔离提示词的聊天页。设计见 [docs/chat.md](docs/chat.md)。

- **聊天页**：主侧栏新增「聊天」，`/chat`；左侧助手切换器 + 会话列表，右侧消息流与输入区。
  默认助手不可删；用户可新建助手，各自持有系统提示词、开场白、默认账户/模型与常用短语，
  会话按助手过滤，切换助手从新会话开始。
- **独立设置页**：`/chat/settings`。模型服务页有服务列表、API Key/地址/协议、测试连接、
  拉取模型、模型列表（搜索、默认星标、能力徽章、能力覆盖）；另有默认模型、数据与备份、
  网络四个分区。消息操作常驻在回答下方，用户/助手消息分别靠右/靠左，助手头像旁显示
  实际模型名；代码块按语言高亮并可复制/下载。
- **流式输出**：命令走 REST，token 走既有 `/ws` 的 `chat:<sessionId>` topic；支持停止、
  两种重新生成（只重生成当前回复 / 丢弃后续）、编辑用户消息、variant 分支切换、删除节点。
- **协议**：首批 `openai-completions` 与 `anthropic-messages`；端点路径是数据（内置默认表 +
  聊天设置覆盖），不运行时猜 `base + /v1/chat/completions`；支持 probe 分阶段检查、拉模型、
  手动能力覆盖（图片/文件/思考/工具/上下文/输出）。
- **模型选择**：输入区的模型按钮按服务分组，列出所有已配置服务的模型（可搜索、带能力
  徽章），选中时同时切换会话的 `account` 与 `model`；底部可直接「添加 / 管理模型服务」。
- **文件**：图片走原生 image block；Anthropic 的 PDF 走原生 document；OpenAI 兼容服务在
  模型能力里打开「原生文件（PDF）」后走 `file` part，否则 PDF/Office 走抽取文本（单附件
  超 3 万字符会截断并提示）。Office 的 docx/pptx/xlsx 目前没有可移植的原生块，仍以抽取
  文本发送；音频/视频只保存不送模型。
- **附件**：图片、PDF、DOC/DOCX、PPT/PPTX、XLS/XLSX、文本；PDFBox/POI 抽文本，图片校验并
  重编码去 EXIF；宏文档、SVG/HTML/可执行文件拒绝；长文本超阈值自动转 `.txt` 附件。
- **存储**：`<data>/chat/chat.db`（SQLite，WAL），附件按 sha256 去重；保留策略默认永久，
  可设 30/90/180/365/自定义天；备份到 `/data/backup/chat-backup-<时间>/`，含 db 快照、
  附件与 manifest，默认每天 03:17、保留 7 份。
- **修复**：代码块内层 `<code>` 被行内样式覆盖导致每行白块/看不清；消息区从居中列改为
  用户靠右、助手靠左；回答下方操作栏常驻；流式响应晚于 turn 事件时不再覆盖已收到的 delta。
- **思考过程**：原先 CSS 把思考内容限制在 220px 高度，滚动条又不显眼，长思考看起来像被
  截断。现在折叠头显示字数，可「展开 / 收起」查看全文；流式时自动展开并跟随到底部。
  后端一直保存完整 reasoning，没有截断。
- **附件上传 413**：`ApiGuardFilter` 的 1 MiB JSON 闸门没把 `/api/chat/attachments`
  列进流式路由，任何超过 1 MiB 的图片/文档都会在进入聊天上传处理器之前得到
  `413 payload too large`（粘贴和文件选择都一样）。已把该路径加入放行名单；
  聊天自己的 multipart 上限仍是单请求 60 MB、图片 20 MB、文档 50 MB。
- **非视觉模型的历史图片**：换到 `vision=false` 的模型后，上文里的历史图片不再让
  整轮发送失败；它们会以「历史图片，当前模型不支持图片，已省略」的文字注记跳过。
  只有**当前这条消息**里的图片仍然返回 `model_no_vision`，并明确提示移除图片或换模型。
  输入区在模型不支持图片且上文/待发送里有图片时会显示提示。
- **手机上的模型弹层**：输入区在屏幕底部，弹层原本按 `window.innerWidth/Height` 定位，
  并且监听 `resize` 就关闭。手机上点「切换模型」时，搜索框自动聚焦唤起输入法，视口一
  缩小就触发 `resize`，弹层当场被关掉，模型切不过去；键盘收起后又回到旧尺寸。现在弹层
  按 `visualViewport` 定位并限高，视口变化只重新贴合不再关闭；触屏上搜索框不自动聚焦，
  点模型按钮先收起输入法，需要搜索时再点搜索框。
- **模型能力默认判断**：能力表从「一堆子串 if」换成按词元序列打分的规则表
  （`ChatModelRules`，做法同 RikkaHub `ModelRegistry`）。以前 `qwen3.8-flash`、`glm-5.3-flash`、
  `kimi-k3`、`deepseek-v4.1-flash`、`doubao-1.6` 这类能识图的新型号都落到「没有图片能力」，
  必须手动改；现在具体型号压过家族通配，默认就带图片/思考/工具声明。`qwen3-8b` 的 `8b`
  是参数量不是版本号，不会被误判成 3.8；认不出来的模型仍然全关，不瞎猜。
  模型列表和选择器把徽章换成文字（图片 / 文件 / 思考 / 工具）并标出来源
  （内置识别 / 供应商预设 / 手动覆盖 / 未识别），设置页顶部写明「默认自动识别，可手动覆盖」。
- **测试对话与识图实测**：模型服务里新增「测试对话」，真的发一条「只回复 OK」，
  验证 key、协议和 chat path（不只是 `/models` 能列出来），消耗几个 token。模型能力
  弹窗里新增「识图测试」：把 jar 内置的白底黑字图（`7314`）发给模型，`max_tokens=24`；
  读出数字 = 支持并写库，4xx 说 image/vision 不支持或正文明确说看不到 = 不支持并写库，
  答非所问 = 存疑不写库（写错一次下一轮发图就是 400）。来源记作「识图实测」。
- **models.dev 目录**：能力判断多一层 `https://models.dev/api.json` 的精简缓存
  （`<data>/chat/models.dev.json`，约 450 KB、4000+ 型号）。启动后检查一次、之后每天
  检查，缓存不足 7 天才重新拉；拉不到保持旧缓存，再没有就回到内置规则表。优先级：
  手动覆盖 > 识图实测 > 供应商预设 > 模型目录 > 内置规则 > unknown。同一型号在多个
  供应商下的能力按多数票，上下文/输出取最小正值（按最大值算会把超长上下文发出去）。
- **思考挡位**：输入区新增「思考·默认 / 关 / 低 / 中 / 高」，写会话级 `reasoning_level`
  （NULL = 跟随助手），刷新后保持。不选就不下发，选了才按厂商翻译：OpenAI 兼容
  `reasoning_effort`；百炼 `enable_thinking` + `thinking_budget`；智谱 `thinking.type`；
  Anthropic `thinking.budget_tokens`（并自动保证 `max_tokens > budget`、去掉 temperature）；
  DeepSeek 不下发（由模型名决定）；认不出的端点 `off` 时不乱发字段，避免把不认识的参数
  丢给中转站换 400。助手设置里的「思考强度」也补齐了「关」，并用中文档位。
- **思考挡位按模型声明生成**：models.dev 的 `reasoning_options` 现在会解析进缓存，
  分 `toggle` / `effort`（英文档位）/ `budget_tokens`（数字，带 min/max）三种。
  输入区菜单只显示模型真有的档：只声明 `high/max` 的模型（如 GLM-5.2）只剩「高 / 极高」，
  没有「关」；声明 `toggle` 的只有「开 / 关」；`budget` 的选项里直接标出 tokens。
  同一 id 在多个供应商下的声明取出现最多的那份，不再取并集（并集会把只能高强度的
  模型误判成能关）。下发时按声明夹取：`reasoning_effort` 向邻近档靠，Anthropic 预算
  夹在 min/max 并保证 `max_tokens > budget`；`forced` 模型（DeepSeek reasoner）一个字段都不发。
  顺带修了 models.dev 刷新在慢网络下可能把维护线程拖住的问题：改成 ofByteArray + 90s 总时限。
- **对话测试误判空响应**：思考模型会把很小的 `max_tokens` 全花在 reasoning 上，正文为空；
  旧逻辑把「正文空」当成失败（`deepseek-flash` 就是这样报的）。现在对话测试的预算是 64、
  识图测试是 1024，并统计 `reasoningChars`：只有思考也算联通，识图判定也会在思考里找数字。
  真的正文和思考都空时提示「端点没按流式回内容」，而不是直接判失败。
- **欢迎页换挡位没反应**：没有会话时「思考·…」只改本地状态，之前直接 return，看着像点不动。
  现在欢迎页会立刻更新，并在第一次发送创建会话时把挡位带进去（`reasoningLevel`）。
- **添加供应商报 `unknown vendor`**：`/api/models/providers` 会把 catalogue 里能路由的供应商
  （`xiaomi` 等）标成 known，前端因此不再让用户填地址；但账户创建只查 `offered()`，于是选小米
  就会得到 `unknown vendor \`xiaomi\`; pass \`endpoint\` to add a supplier by address`。
  现在 `DshVendor.byId` 也认 catalogue 中**有地址且有协议**的条目（没有地址/协议的仍要求手填），
  账户地址回落到供应商自己的 baseUrl。新增用例覆盖这个路径。
- **供应商目录刷新不动**：`/api/models/providers` 的目录快照在生产已经 10 天没更新——
  未压缩的 api.json 约 6 MB，而下载总时限只有 20 秒，慢网络下每次刷新都超时。
  现在请求带 `Accept-Encoding: gzip`（约 460 KB）并在本地解压，总时限放宽到 120 秒
  （聊天自己的 models.dev 缓存同样处理）。添加供应商对话框显示快照日期/来源，可手动
  「刷新目录」；卡片上的模型数标明只是目录快照，实际以「拉取模型」为准。
- **目录下载时限**：models.dev 的 api.json 压缩后约 460 KB，但慢网络实测只有个位数 KB/s，
  原来的 20s / 90s 总时限永远拉不完，快照就一直冻着。供应商目录与聊天能力缓存的总时限
  都放宽到 5 分钟（后台线程跑，不挡页面）。
- **DeepSeek 的思考挡位**：之前把 `api.deepseek.com` 当成「思考由模型名决定」，一个参数都不发，
  于是 deepseek-v4.1-flash 这类模型只能按默认（high）思考，token 哗哗流。官方文档其实认
  `reasoning_effort = none/low/high/max`（minimal→low、medium/xhigh→high）。现在挡位菜单对它
  开放：关 = `none`，低/中/高/极高 按官方词表映射；目录没写 `none` 也照样给「关」，因为文档
  保证它存在。老的 `deepseek-reasoner` 仍是强制思考，不给选择器。
- **头像与个人资料**：聊天设置新增「个人资料」，可改显示名和头像（留空名字 = 面板登录名）；
  助手头像也支持上传图片。上传前用 canvas 做圆形预览、拖动和缩放，导出 256×256 PNG；
  服务端按魔数只收 PNG/JPEG/WebP 静态位图，ImageIO 解码后中心裁切、重编码成 512×512 PNG
  再落盘——EXIF、动画块和附加数据都不会保留，4 MB / 4096 像素以上直接拒绝。头像 id 只认
  64 位十六进制，按内容哈希存储，换头像/删助手时清理未引用的旧文件。
- **长文本粘贴**：粘贴超过 100 字的纯文本不再灌满输入框，而是自动变成
  `粘贴文本-N字.txt` 附件条，和图片附件一样随消息发送。
- **编辑消息改为内联**：不再弹窗；点「编辑」把原文放进输入框，上方显示「编辑中」，
  Enter 保存、Esc 取消，被编辑的气泡会高亮。保存后照旧重新生成后续回复。
- **主侧栏顺序**：改成「账户 → 游戏 → 聊天 → 媒体（转码启用时）→ 设置」。原来聊天夹在
  游戏和设置中间还行，但转码排在设置之后，启用后看着像第二个设置项。
- **上下文占用显示**：标题栏加了占用环 + `12.3K / 128K`（最近一轮 input tokens / 模型
  maxContext），80% 变黄、95% 变红；点开显示当前占用与百分比、本轮输出、会话累计输入/输出。
  流式期间接 `chat-usage` 事件实时更新。输入按每轮完整上下文计费，累计输入接近账单口径，
  不等于当前占用；端点不回 usage 时显示「上下文 —」。
- **联网搜索**：输入区放大镜按条开启。两条路并存：模型声明支持工具时，面板下发只读工具
  （`search_web`，DeepSeek/Exa 另有 `scrape_web` 抓正文），模型自己决定搜什么、搜几次
  （每轮上限 3 次），结果作为 tool 消息回灌，回答可以跨多段；模型不会用工具时，退回
  「发送前先搜一次、结果拼进系统提示」。工具集与参数 schema 按服务商生成：博查只有
  `search_web`，只配一个 key 也能完整使用。
  每次请求都带一段「面板能力」声明（有没有搜索、当前服务商、这轮能不能用），模型因此知道
  面板能搜。OpenAI 走 `tool_calls`/`tool`，Anthropic 走 `tool_use`/`tool_result`；端点不认
  `tools` 字段回 400 时自动降级成纯提示词重试一次。支持博查、DeepSeek Search
  （key 可回落到 DeepSeek 账户的 key）、智谱 Web Search、Tavily、Brave Search、Exa；
  key 按服务商分开存，切换不互相覆盖；端点可用 `-Dhdsl.search.<provider>` 覆盖。结果缓存
  1 小时，失败只影响这一轮的工具调用，空结果不计费。搜索记录进 `usage_json.tools[]`，
  前端在回答下方渲染「搜索」条，可展开来源；流式期间另推 `chat-tool` 事件显示「正在搜索」。
  设置页新增「联网搜索」：服务商、key、条数、测试搜索，以及「保留检索记录」开关。
- **检索记录**：开启时把这一轮的 `text` / `tool_calls` / `tool` 写进
  `chat_message.parts_json`（schema v3），下一轮追问还原成协议消息，模型能看到上一轮搜过
  什么；关闭后只留最终文本。历史 tool 结果截到 2000 字，上下文超预算时 assistant 的
  tool_calls 与后续 tool 消息成对删除，不留悬空消息。引用格式改为
  `[citation,domain](id)`，前端按 id 换成真实 URL。
- **权限**：所有 `/api/chat/*` 对 agent ticket 关闭（放在 `GET → READ` 之前）；设置、备份、
  端点探测与能力覆盖要求开发者提权；聊天只向模型暴露一个只读的 `web_search`，模型无法
  触发面板的其它操作。

**三个 TUI 品牌（Qwen Code / MiMo Code / MiniMax Code）**

- `Brand.Kind` 新增 `WEB / TUI / HYBRID`：TUI 品牌实例是 PTY，没有网页进程、端口和
  `/i/<id>/` 挂载；`/api/brands/capabilities` 下发 `kind / web / terminal`，网页与终端客户端
  都按它渲染，不再按品牌 id 猜。
- 安装与启动优先面板托管的 Node（它们的 `engines.node` 很窄，宿主的旧 Node 会直接跑不起来）；
  包里有 `engines.node` 以包为准，没有才用面板已知范围。Windows 上 MiMo 走 `mimo.js` 启动器，
  避免直接执行 `vendor/mimo.exe` 锁住安装目录。
- 每次 attach 前重新把实例 home / workspace 交给 agent uid，旧构建建的实例或被 root 持有的
  home 也不会在不可写目录里跑 TUI。
- 网页侧：品牌页、实例列表、启动面板、Go 客户端都识别 TUI 形态；实例卡只给「终端」，
  「启动 / 停止 / 打开网页」按 `tui_only` 明确拒绝。

**终端：不再回落到 shell**

- `TerminalTarget` 构造时拒绝 `sh/bash/zsh/fish/dash/ksh/csh/tcsh`、`cmd/powershell/pwsh`
  以及 `.sh/.bash/.zsh/.fish/.cmd/.bat/.ps1`；TUI 退出后不会再留下一个 `$` 提示符。
- `.com` 按「DOS 原生可执行」放行（曾被误判成 shell，误伤合法程序），`command.com` 按名字拒绝。
- PTY 始终以 argv 数组 spawn，不经过 shell；回归测试覆盖 shell 名、脚本后缀与原生 `.exe/.com`。

**主侧栏与品牌导航**

- 顺序改成「账户 → 游戏 → 聊天 → 媒体（转码启用时）→ 设置」；TUI 品牌在实例列表里有独立导航栏，
  不再和网页品牌混在同一组。

**合并前的守门修复**

- NOTICE 补登 sqlite-jdbc（含公有领域 SQLite 原生库）、PDFBox、POI + XMLBeans、curvesapi、
  SparseBitSet、commons-*、log4j-api 的许可证与来源。
- SQLite 原生库按平台打包在 jar 内，首次连接时解压到 `java.io.tmpdir`；加载失败（不支持架构、
  只读/noexec 的 tmpdir）会抛 `UnsatisfiedLinkError`，现在聊天模块降级为不注册 `/api/chat/*`
  （404），面板其余部分照常启动，半开的 store 会被关闭。
- 先搜后答移出 `ChatTurnManager` 的全局锁：一次慢搜索不再把其它会话的发送 / 重生成 / 编辑卡满
  20 秒，搜索完成后重新校验 busy 与每日额度。
- 前端 TUI 品牌列表改为登录后读 `/api/brands/capabilities`，硬编码列表只做首屏兜底。
- CI 的 build 增加 `web-shell` 分支触发，后续开发分支推送即跑 JDK 17/21。

**已知限制**

- 模型服务的 ON/OFF 开关、模型家族分组、拖拽排序未做；当前是状态点 + 默认星标。
- OpenAI 原生 PDF `file` part 默认关闭，需要「模型能力」里显式打开。
- 备份恢复仍是手动流程（停面板 → 替换 `<data>/chat` → 启动迁移），没有 UI。
- 每日 token 上限是进程内按自然日计数，重启归零，且依赖供应商返回 usage。
- 音频 / 视频附件只保存、不送模型；`openai-responses`、fork、KaTeX/Mermaid 未做。
- 自动降级每个转码任务只走一格；Windows 上仍只有 NVIDIA 的 GPU 摘要（AMD/Intel 进程表未做）。

**验证**

- `./gradlew test`：1483 个用例、0 失败（254 个测试类；新增聊天 SQLite/SSE/路径/权限、
  真实 SSE turn、TUI 适配器与 shell 拒绝等用例）。
- `python3 scripts/api-smoke.py`：39 项全 PASS（含聊天初始化、终端四道闸、品牌能力）。
- `npm run typecheck` / `npm run build` / `npm run compat`：绿。
- `./gradlew shadowJar -PreleaseVersion=0.6.85`：成功，jar 69 MB。

## v0.6.84 — 2026-11-11 · 转码队列工作台：勾选、批量、排序、实时推送

**队列页：先勾选，再动作**

- 控制条拆成两行：第一行是整队列的「开始 / 暂停 / 停止全部 / 清除已完成」与计数，
  第二行是作用于勾选集合的「开始 / 暂停 / 恢复 / 停止 / 移除 / 重置预设」，
  另有全选、选中错误、上移 / 下移与 Enter / Delete / 空格的快捷键；行内只保留「日志」。
- 任务详情补 ETA、每一遍的命令与状态（steps）、编码器切换记录（谁触发、从什么到什么、原因）。

**服务端：按钮能不能点由服务端说了算**

- 快照、单任务 GET 与 WebSocket 事件统一下发 `caps`（start / pause / resume / stop /
  remove / reset / reorder）；前端删掉自己那份状态机副本，两边不会再各说各话。
- 新接口：`POST /queue/jobs/{id}/start`（置顶并启动，仍受并发上限）、`POST /queue/action`
  （批量动作，逐条返回成败）、`POST /queue/reorder`（必须列出全部任务，正在跑 / 已暂停 /
  等待决定的位置钉死）、`POST /queue/sync-preset`（只改还没开始的任务，返回各项计数）、
  `POST /queue/clear`、`DELETE /queue/jobs/{id}`。
- 队列页订阅 `/ws` 的 `transcode:queue` 事件，原先 1 秒一次的全量轮询降为 30 秒兜底。

**修复**

- **逐任务恢复从代码里就不可能成功**：`resume()` 原先要求状态是 `RUNNING`，而暂停后是
  `PAUSED`，因此按钮永远是 409。改为「预期状态 → 目标状态」校验。
- **待决定任务的移除/换预设会把队列卡在暂停**：硬件失败会把队列停在 `PAUSED` 等决定；
  把那个任务移除、取消或换预设后，暂停的理由已经不存在，剩余排队任务却不会动。
  现在队列记住了暂停原因，最后一个待决定任务离开时自动恢复派发；手动「队列暂停」不受影响。
- **重试会毁掉工具任务**：`resetForRetry()` 会清空 `tool` / `command` / `encoder`，重试
  merge / mux / extract 时会把「提取 0:v:0 · input.mp4」当成文件名。现在先保存再还原，
  输出名也重新预留，避免和后来提交的任务撞车。
- **上移 / 下移不再跨过不能移动的任务**：相邻任务是运行中 / 已暂停 / 等待决定时，
  「上移 / 下移」直接置灰并说明原因，不再让整单 reorder 被 409 拒绝。
- **日志读取**：`log()` 原先每次 `readAllLines` 整个文件，编码几小时后会越来越慢；
  改为从文件末尾读 256 KB 窗口。

**验证**

- `./gradlew test` 全量绿；`TranscodeQueueTest` 11 个用例（新增「移除待决定任务后队列自动恢复」）。
- `npm run typecheck` / `npm run build` 绿。
- `python3 scripts/transcode-smoke.py` 16 项全 PASS（含 reset-preset / remove / clear）。

## v0.6.83 — 2026-11-11 · 转码模块：批量编码、硬件加速与 GPU 监控

> 首个把「转码」当作一等功能的版本：默认关闭，关闭时与 MC 一样「当它不存在」；
> 启用要开发者密码 + HTTPS + 阅读须知。设计取舍、剩余项与实机测试记录见
> [`docs/transcode.md`](docs/transcode.md)。

**参数工作台与队列（2026-11-11，UI 第二轮）**

- 取值不再直接暴露裸 ffmpeg 名：参数总览、预设预览、准备页摘要和队列详情都显示中文短名
  （如 `libx264` → H.264 软件编码（x264）、`copy` → 保留原音轨），悬停可看原始取值与完整说明。
- 下拉框改成仿 3FUI 的浮层列表：每一项是「中文名 + 原始取值 + 一行解释」，收起后显示中文名；
  键盘 ↑↓/回车/Esc 可用。字段名旁新增 ⓘ 悬停浮窗，显示这一项的完整说明。
- 下拉列表单行铺开、不再内部滚动；选项过多时自动分栏，硬件编码器（NVENC/VAAPI/QSV）全部可见。
- 硬件编码器补上速度档，并随编码器自动切换：NVENC `p1–p7`（默认 p4）、QSV `veryfast–veryslow`
  （默认 medium）、H.264 VAAPI `-quality 0–8`（越大越快）；HEVC VAAPI 没有速度控制，字段置灰并说明。
  切换编码器时旧的档位会映射到新家族的默认值，不再一律清空。
- 编码器/容器联动补齐（对齐 3FUI 的参数数据库）：
  - 编码速度档按编码器家族切换：x264/x265 命名档、VP9/AV1 数值档（`-cpu-used`）、NVENC p1–p7、
    QSV 命名档、VAAPI 0–8；VP9/AV1 不再错误地拿到 `-preset`。
  - 质量模式随编码器变化：软件保留 CRF/CQP/码率，硬件只留 CQP/码率，VP9/AV1 只留 CRF/码率；
    切到固定码率时质量值自动换成 `2500k`，切回质量模式时换成 `20`。
  - `profile` / `tune` / `pixelFormat` 从手填改成下拉，选项按编码器家族给出（VP9 无 profile、
    QSV/VAAPI 无 tune、VAAPI 只列 nv12/p010le 等）；切编码器时清掉新编码器不接受的旧值。
  - 音频编码器按容器过滤（WebM 只能 Opus，MP4/MOV/M4A 不能 FLAC/Opus）；切容器时自动换成兼容值。
  - 视频编码器按容器过滤（WebM 只留 VP9/AV1，M4A 只留“丢弃视频”）；切容器时自动换编码器并清空
    不再适用的像素格式/profile/tune。
  - 质量值标签随模式变成「CRF 值 / CQP 值 / 目标码率」；指定像素格式后 10-bit 开关置灰，
    自动命名不是“追加后缀”时后缀输入置灰，字幕/音频 map 在不需要时置灰，受限容器的附件选项置灰并自动关闭。
- 后端：硬件编码器现在真正输出 `-preset`（NVENC/QSV）与 `-quality`（H.264 VAAPI）；
  QSV 的恒定质量改用 `-global_quality`，不再生成它不认识的 `-qp`。
- 依赖项自动变灰并说明原因：视频「直接复制/丢弃」时质量、编码档位、画面、色彩等参数不可调；
  音频非 `encode` 时编码器/码率/声道/采样率不可调；字幕非 `burn` 时字体/字号/字幕文件不可调。
- 移植 FFmpegFreeUI / 3FUI 的常用参数页：
  - 剪辑区间：开始/结束时间，后端生成 `-ss`/`-to`（输入侧定位）。
  - 画面增强：降噪（hqdn3d/nlmeans/atadenoise/bm3d）、锐化（cas/unsharp）、去色带、
    简易调色（brightness/contrast/saturation/gamma），参数留空用 ffmpeg 默认；降噪/锐化/
    去色带的参数框只在启用后可用。
  - 音频处理：音量（倍数或 dB）、EBU R128 响度标准化（I/TP/LRA）；只在音频重新编码时可用。
  - 字幕烧录：字体/字号/额外 `force_style` 现在真正写进 filter；字幕文件按媒体根解析成绝对路径，
    不再因为面板 cwd 不同而找不到文件。
  - 元数据：新增“额外元数据（每行 key=value）”，可写任意 ffmpeg metadata 键。
- 后端：`CommandBuilder` 增加 `-af`（音量/loudnorm）、画面增强滤镜链、trim 输入参数与额外元数据。
- 两遍编码（x264/x265 + 固定码率）：参数面板新增「两遍编码」开关，第一遍只分析、第二遍出片；
  队列按两遍执行并把进度折算成 0–100%，passlog 在成功/失败/取消后清理。不支持的编码器或
  CRF/CQP 组合会在保存、预览和提交任务时直接拒绝，不会跑到一半才失败。
- 补齐编码器：NVENC AV1、QSV VP9/AV1、VAAPI VP9/AV1，以及软件 AV1 的 SVT-AV1 与 rav1e；
  WebM 容器现在也能选 VP9/AV1 的硬件编码器。下拉列表按 H.264 / H.265 / VP9 / AV1 分组，
  同格式的软件与硬件编码器排在一起。速度档、profile、tune、像素格式按编码器家族联动，
  VAAPI VP9/AV1 不生成它们没有的 `-quality`，rav1e 用 `-speed`/`-qp` 而不是通用的
  `-preset`/`-crf`。
- 修复 `libaom-av1` 被误判成图片编码器的问题：此前选它会强制单帧并丢掉音轨。
- 增加 Windows 侧硬件编码器：AMD AMF（H.264/H.265/AV1）、D3D12VA（H.264/H.265/AV1）与
  Vulkan（H.264/H.265/AV1）。AMF 的速度档走 `-quality`、场景优化走 `-usage`，D3D12VA 用
  `-rc_mode`，不会像普通硬件编码器那样误发 `-preset`/`-tune`；降级链回落到 x264/x265/SVT-AV1。
- 编码器下拉按当前 ffmpeg 的 `-encoders` 置灰：构建里没有的编码器显示为「不可用」，不再等到
  提交后才报错。
- 质量页补 VBR/CBR 与 `none`：可填 `minrate` / `maxrate` / `bufsize`；旧的 `bitrate` 保留兼容。
- 字幕新增「转换格式」：MKV 可转 ASS/SRT/WebVTT，MP4/MOV 自动用 mov_text；不再只能 copy 或烧录。
- 流控制支持「保留其它视频/音频/字幕轨」：map 全部轨道，只对索引指定的那条重新编码/转换，
  其余直接 copy；默认行为仍是全部保留、音频/字幕 copy。
- 滤镜顺序修正：裁剪和反交错移到缩放之前，裁剪坐标按源画面计算。
- 后端在保存、预览、提交前校验编码器与质量模式、容器与编码器、MP4 字幕格式、流控制组合；
  MP4/AVI/TS/WebM 等装不下的附件/章节会直接跳过，不再让 ffmpeg 跑到一半才失败。
- 准备文件支持按文件夹递归加入准备列表并批量入队；准备列表逐项显示服务器上的绝对输出路径，
  输出目录留空 = 每个源文件旁边，填相对路径 = 统一写到媒体根下的该子目录。
- 准备页改为「待转码列表」：加入的文件/文件夹只是暂存，点「加入队列」才按当前预设提交；
  输出目录新增目录选择器，可明确选「源目录」或媒体根下任意文件夹，并实时显示绝对路径。
- 后端：视频 `copy` 时不再输出 `-vf` 与色彩标记，避免生成 ffmpeg 直接报错的命令。
- 参数面板改成三栏工作台：预设列表（单击应用）/ 分区导航 / 字段区 + 右侧参数总览与实时命令行；
  字段旁直接显示一行说明，下拉逐项解释（schema 新增 `hint` / `optionHints` / `important`）。
- `/api/transcode/preview` 允许不带输入文件，用 `<输入文件>` / `<输出文件>` 占位生成；
  参数面板改完 600ms 自动刷新命令行，不必先选片。
- 参数总览把容器/编码器/质量/音频/字幕/命名集中显示，并把「MP4 装不下字幕/附件」「烧录字幕缺文件」
  「硬件编码器带软件 preset」等常见错误就地列成可点击的警告。
- 队列页补当前/下一个徽标、行内进度与速度/大小、任务详情 + 实时输出尾巴、日志对话框
  （全部 / 只看错误）、复制输出路径与命令行；失败任务仍走降级/停止决策卡。
- 概览页补 4 步新手流程与内置预设快捷入口；准备文件页记住上次预设并自动预览命令。
- 预设不再占一列常驻侧栏：「预设管理」收进纵向导航，单击只预览、双击或「应用」才读进参数面板。
- ffprobe 媒体信息独立成「媒体信息」子页：封装信息（容器/时长/大小/总码率）+ 流信息表
  （类型/编码/规格/帧率/码率/语言·标题）+ 可折叠的原始 JSON；工具页只留合并/混流/抽流/质量评测。
- 补页级与分区级说明（默认展开，可折叠）：参数面板、预设管理、队列决策、准备文件、
  媒体信息列、工具用法与设置项含义；schema 的分区新增 `hint`，字段 `hint/optionHints` 一起渲染。
- 参数总览只显示总览与警告，字段按分区进入（新增「全部参数」兜底）；预设管理页不再显示
  当前预设的编辑框，命令行统一收到右栏「预设命令行」，避免同一页两个命令行。

**功能门：默认关闭，关闭即不存在**

- 转码状态存 `<data>/transcode/feature.json`，`server.yaml` 的 `transcode:` 只放运行参数；
  关闭时 `/api/transcode/*` 除 `feature/terms/enable/decline` 外一律 404，导航不出现。
- 启用要求开发者密码 + HTTPS + 须知确认；记录条款版本/哈希/接受人/来源/UA。
- 急停联动：暂停队列并终止 ffmpeg 子进程。

**服务端队列与失败策略**

- `TranscodeQueue` 在服务端调度：第一次显式开始后自动补位，关网页/断 WS 不影响；
  `.part` → 原子改名，stderr/进度/日志持久化；暂停/恢复走 SIGSTOP/SIGCONT。
- 失败三档：`ask`（默认）/ `timeout-auto` / `full-auto`；硬件失败按 QSV→VAAPI→CPU
  降级，每任务最多一次；运行中任务不会被自动重启。
- `/queue/jobs/{id}/decision` 支持停止/仅此任务降级/降级并应用队列/完全自动化；
  策略可写回 `server.yaml`，重启仍生效。

**预设、命令与互通**

- 面板自己的 schema（容器/视频/画面/音频/字幕/元数据/章节/附件/滤镜/自定义参数），
  命令生成只产出 argv，不拼 shell；内置预设只读，用户预设原子写。
- 元数据、附件复制、章节文件、滤镜选择器、自定义参数编辑器；`.3fui` 双向尽力映射，
  支持导入/导出。
- 图片编码器强制跳过音/字幕；QSV 自动 `-low_power 1`；VAAPI 注入设备与 hwupload；
  切硬件编码器清理软件 preset/profile/tune。

**工具与质量评测**

- 合并（concat+copy）、混流（多输入 `-map`）、抽流（单流 copy）复用队列的日志/进度/取消。
- PSNR/SSIM/XPSNR/VMAF 后台评测，逐帧 stats 曲线、平均值、取消；`inf` 封顶 100，
  没有 `libvmaf` 时 VMAF 由后端拒绝。

**GPU 监控**

- 性能页 GPU tab 增加任务管理器式四图（总利用率/编码/解码/显存）与多卡下拉。
- Linux：NVIDIA `nvidia-smi`、AMD sysfs+hwmon、Intel sysfs；macOS 不可用。
- Windows 目前只有 nvidia-smi 摘要；AMD/Intel 的 PDH 每进程 GPU 引擎计数仍未做。

**容器、媒体根与 ffmpeg 安装**

- 容器媒体根固定 `/home/hdsl/video`（`HDSL_VIDEO_DIR` 决定宿主挂载）；入口校正 uid 1000，
  并按 `/dev/dri/renderD128` 的 GID 给 `hdsl` 加组；compose/安装脚本增加
  `--video` / `--hw auto|vaapi|nvidia|none`。
- 设置页可在线安装 ffmpeg：静态包（https + 必填 SHA-256）、apt-get/dnf/zypper、
  源码编译；任务有进度/日志/取消，下载拒绝私网/环回。
- **Docker apt 注意**：`debian:trixie-slim` 第一次 `apt-get update` 时没有
  `ca-certificates`，`APT_MIRROR=https://…` 会证书校验失败；apt 这一段用 `http://`。
- **Java truststore 显式化**：镜像显式安装 `ca-certificates-java` 并断言
  `/etc/ssl/certs/java/cacerts` 存在；它和 apt 的系统 CA 是两套东西，缺了会在运行期
  报 PKIX。

**Agent**

- 新增 `transcode` / `transcode-raw` scope；媒体根浏览要求 raw，其余转码操作要求 transcode。
- `/api/transcode/feature*`、`/settings*`、`/runtime/*` 对 agent 始终关闭：启用/关功能、
  改失败策略、装运行时是操作员动作。

**防呆（补 3FUI §7 的一部分）**

- 入队前检查编码器/容器；限制性容器 + 保留字幕/附件/章节时提醒改 MKV。
- 设置页“测试 1 帧”真实跑 testsrc→null，区分“ffmpeg 列出”与“本机驱动能编”。
- 队列显示任务实际 argv；原子写、队列持久化、媒体根路径限制沿用。
- 未全搬：容器兼容三选、编码器参数值逐项校验、三层故障探测、Agent 写确认、
  应急上传三重校验。详见交接文档。

**界面修订（真实测试反馈）**

- “预设”页面改为 **参数面板**：左参数集、中分区字段（两列）、右粘性
  命令行预览；准备页不再默认选中第一个内置预设，避免一进来就无声地
  用 h264-quality。
- GPU 性能页改成四个紧凑指标卡 + 2×2 折线图；没有数据时也保留空网格，
  不再整块替换成“没有 GPU”。Linux 加 DRM 回退：vendor 读不到给 unknown
  卡、只有 renderD* 时也列、/dev/dri 已挂载但 sysfs 不可读时给占位卡。
- ffmpeg 安装页增加常用来源预设（Linux 源码/静态包、Windows、macOS），
  SHA-256 可留空自动读侧车，读不到才要求手填。

**测试容器反馈后的修复**

- **P0 输出覆盖**：同输入同预设连续提交时，三个任务都拿到同一个输出名，
  最后只剩最后一个产物。现在队列在**提交时**预留输出路径（`reservedOutputs`），
  `CommandBuilder.outputPath` 同时避开“已存在”和“已预留”；工具任务同样
  用 `uniqueOutput()` 避让；加载队列时从非终态任务重建预留集合，完成/失败/
  取消/停止时释放，retry 重新预留。
- **P1 无音轨误判**：视频预设 + 无音轨输入原本会被 `validOutput` 判成
  “ffmpeg 没有产出”。现在只有输入本身有音频时，音频 copy/encode 预设才要求
  输出有音频；错误文案区分空文件/无视频流/纯音频预设/音频缺失。测试覆盖
  视频-only 源与 h264-quality。
- 真实 1080p HEVC 10bit + FLAC 25:45 测试：mp4-h264 预设约 5 分 08 秒
  （5.04×），426 MB → 360 MB，SSIM All 0.978 / PSNR 平均 29.6 dB。

**终端客户端：补上 Hermes 分类**

- 面板早就有 `hermes --tui` 适配器，客户端菜单却只列 kimi/opencode/claude-web/pi-web/zcode，
  Hermes 实例必须手敲 `--brand hermes`。现在菜单、`--brand` 文档与 404 提示都同步到
  这六个品牌；Octop 仍然不列——它没有终端，列出来只是死路。
- 测试夹具补 hermes；离线（`GOFLAGS=-mod=vendor GOPROXY=off`）`go build` / `go vet` /
  `go test ./...` 全绿。

**验证**

- `./gradlew test` 全量绿；`npm run typecheck` / `npm run build` 绿；
  `./gradlew shadowJar -PreleaseVersion=0.6.83` 成功。
- `python3 scripts/transcode-smoke.py` 13 项全 PASS（真实 ffmpeg：capabilities / media /
  presets / preview / submit / encode / log / extract / quality / 404）。
- 本机无 Docker，镜像未实际构建；`docker-install.sh` 只做了语法检查。

## v0.6.82 — 2026-11-10 · 创建实例先选品牌；GitHub 版本目录缓存与配额

**第三方核心下载：慢源自动换、删除实例不再留下孤儿任务**

- 官方 Purpur 从部分网络先回 200，再以个位数 KiB/s 滴流 50 MB；面板原来只看
  HTTP 状态，任务会在 30 分钟请求超时里一直挂着。现在按 10 秒窗口检查吞吐，
  低于 64 KiB/s 即判定该源不可用，按源顺序换下一个（官方 → MSL/BMCLAPI），
  并且只在下载校验成功后写 artifact 缓存，失败源不会污染下一次。
- 删除实例时会取消仍在跑的安装任务，不再出现「实例已删、任务还 running」。
- 说明：`CORE_FETCH_TIMEOUT` 仍会让慢源的版本列表在 12 秒后以 interrupted
  收尾，这是目录请求的预算，不是下载失败；下载路径现在独立处理。

**创建实例：先选品牌；当前实例全局唯一**

- `/instances/new` 现在是一个选品牌页：dsh 继续走原来的版本向导，Kimi / OpenCode /
  Claude Web / Octop / Hermes / PI WEB 跳到品牌页并展开安装/创建，不再默认只服务 dsh。
- 主侧栏的「当前实例」认全局选择：dsh 与品牌实例共用一个 radio，切换后主界面卡片、
  启动面板与侧栏一起跟随；品牌实例显示品牌图标，点击回到对应品牌页。
- 旧深链 `/instances/new?version=…` 仍直达 dsh 向导；空态文案不再提「实例文件夹」。

**实例列表：去掉无头部署里不存在的文件夹入口**

- 删掉「添加实例文件夹」以及旁边那个只会弹提示的伪删除按钮：网页版本来就用服务端
  实例目录，没有桌面文件夹选择器。
- 该入口改用 dsh 的英文全名 **DeepSeek Harness** 与 dsh 标志，副标题写明「dsh 实例」。

**GitHub 版本目录：磁盘缓存、配额守护与令牌**

- 版本列表先读内存、再读磁盘（`cache/brand-catalog.json`），页面不再等网络；刷新失败
  保留旧列表，不再清空。
- GitHub 品牌改为 1 小时 TTL + `If-None-Match`（304 不消耗配额）；npm 品牌 30 分钟。
  每品牌单飞，手动刷新 10 分钟冷却，连续失败按 1h→2h→4h 退避。
- 读取 `X-RateLimit-*`：剩余 ≤10 时自动刷新暂停；额度用尽/限流时显示重置时间；
  403/429 不再重试。面板日志与页面都能看到数据时间、来源、下次刷新和额度。
- 新增面板托管的 GitHub 令牌：存在 `<data>/auth/github.json`（0600，面板属主，
  agent 读不到），保存前用 `/rate_limit` 校验，只回传是否配置与登录名；令牌把
  匿名 60 次/小时提高到 5000 次/小时。安装时的 commit 解析也共用它。
- 启动不再预热版本目录：启动路径只读磁盘缓存，后台空闲刷新在服务就绪 10 分钟后
  才启动；无人访问时每 3 小时确认一次，有人访问时回到 1 小时 TTL。

## v0.6.81 — 2026-11-08 · 体验优化：MC 安装进度、Hermes Node 26 提示

**界面：MC 服务端安装与版本切换的进度可见**

- 实例列表里直接显示下载/安装进度条、百分比与当前阶段；装完之前「启动」按钮禁用，
  不再出现「点了启动才发现服务端还没装好」。
- 创建向导的「下载与安装」步骤加「后台继续」按钮；关掉向导后进度留在列表里，安装任务
  照常在服务端跑，WebSocket 断开时列表也会每 3 秒补拉一次任务状态。
- 实例页的「服务端版本」下拉补上进度条、百分比与取消：原版取 Mojang 版本目录，第三方
  核心取各自核心的版本目录（原来一律显示 Mojang 版本，Paper/Fabric 等会装错或失败）。

**Hermes：新版本需要 Node 26 的提示与拦截**

- 0.22.6 起 Hermes 把 `engines.node` 收紧到 `>=26`，且上游 `.npmrc` 开了
  `engine-strict`——Node 22/24 上 `npm ci` 会直接失败，看起来像网络/源码问题。
  版本卡现在给这类版本标「需 Node 26」，当前 Node 不满足时给出两步处理入口
  （先在「设置 → Node.js」装 26，再在「设置 → 品牌实例」把 Hermes 的默认 Node 指到 26）。
- 安装器在下载源码前就按品牌解析到的 Node 预检：不满足时直接失败，并说明当前用的是
  哪个 Node、面板里哪些 Node 满足、去哪里切换——不再让用户等完下载才看到 `npm ci` 报错。
- 启动时的 `engines.node` 不匹配提示补上处理位置。

## v0.6.80 — 2026-11-08 · PI WEB 品牌（终端为主、网页是添头）；MC 插件管理收尾

**里程碑：MC 服务端 v1**

MC 服务端第一版功能完成，作为一条明确的分界。此后 MC 相关改动进入维护与增量阶段。

- 原版 + 第三方核心（Paper / Folia / Purpur / Fabric / Forge / NeoForge），
  多源下载（官方 / BMCLAPI / MSL）与配额守卫；
- 面板托管 JDK（Temurin / Microsoft / OpenJDK），安装向导 + MC 版本对照表；
- 插件/模组：启停、上传、删除、批量管理，Modrinth 搜索（中文词表、分页）与安装；
- 第二道关（第三方核心）、免责与审计、面向 agent 的权限分离；
- 实例高级设置：内存、RCON、按实例 Java；世界导入导出与备份。

**PI WEB 品牌（第五个品牌，实验）：终端为主，网页是添头**

- 一个 release 是一个**配对**：`@chainingintention/pi-web-cn` + 它验证过的
  `@earendil-works/pi-coding-agent`（当前 2.202611.8 + 1.4.0）。安装器一次
  `pnpm add` 两个包；版本选择器只列已验证配对，更早的已装 release 继续可用。
- 终端与网页共用同一个实例 HOME/agent 目录：`pi-web-sessiond` + `pi-web-server`
  由生成的 Node wrapper 启动（先 daemon，等 `/health` 再起 web），面板轮询两端
  就绪才把实例标成 running。
- 实例级形态：终端+网页（默认）/ 仅终端（不占端口、不挂载 `/i/<id>/`）；版本锁
  让实例不跟随品牌升级。安装失败（node-pty 未编译、web 包装不上）在版本卡原位
  给出「只装 Pi 终端」的降级，实例卡与「运行与版本…」对话框展示降级原因。
- 反代直接挂 `/i/<id>/`：pi-web 的客户端按 `document.baseURI` 解析所有请求、
  路由走查询串，所以不做页面改写、不注入 runtime shim。
- 安装后自检 `pi-web-sessiond --check-runtime` 判定浏览器终端（`node-pty`）是否
  可用，结果写入 release.json；Pi TUI 不受影响。
- 属主：`PI_WEB_DATA_DIR` 不预建——默认 `$HOME/.pi-web` 由 agent 自己创建；wrapper
  写在已交接给 agent 的实例 home 下并显式 0644，避免 root 属主目录/文件在干净容器里
  造成 `EACCES`（与 v0.6.45 的 pnpm 属主问题同一类）。
- 就绪探测统一走 HTTP/1.1：pi-web 的 Fastify 对 JDK HttpClient 默认的明文
  HTTP/2 升级（`Upgrade: h2c`）回 400，面板把 400 当成「没起来」，等满 180 秒
  后判定失败——实例端口不落盘，「打开网页」报 not running，`/i/<id>/` 回 502。
  失败路径同时改为收掉整棵进程树，并在下次启动前清理 marker 指向的旧 sessiond，
  避免 `SessiondStateOwnershipConflictError` 把重试也堵死。

- 文档：PI WEB 的浏览器终端是上游必需插件，门禁只有它自己的访问密码；在
  `docs/security.md`、`docs/brands.md`、`docs/web-shell.md` 写明这条边界。

**终端客户端：补上 PI WEB 分类**

- `hdsl-tui` 的 `--brand`、实例选择器与帮助文本增加 `pi-web`，终端仍走同一份
  `/api/brands/pi-web/instances/<id>/terminal` 契约；`--resume` 照旧透传（服务端
  的 Pi TUI 暂未接恢复参数，不报错）。

**插件/模组：中文搜索、分页、批量管理与安装元数据**

- 搜索只写 `categories:paper`，把 Bukkit/Spigot 插件全滤掉了；改成 OR
  （paper/spigot/bukkit/purpur/folia）。
- 中文查询先过一张常用词表（经济→economy、领地→claim、优化→optimization…），
  否则 Modrinth 上中文搜索几乎总是空结果。
- 结果从固定 20 条改为分页（offset/limit + totalHits，界面「加载更多」）；
  严格版本过滤 0 条时自动放宽并标注 `relaxed`，安装时仍会再校验。
- 官方 API 失败时自动回落 `mod.mcimirror.top/modrinth/v2`（只换 host，不换校验）。
- 安装后写 `<jar>.panel.json`（项目、标题、版本），列表显示标题与版本号；
  删除文件时一并清理。
- 管理面补齐：按名字/标题/版本过滤、全选、批量启用/停用/删除。

**Forge 安装：国内源被静默跳过、慢在官方 Maven**

- **BMCLAPI 302 不跟随**：`McForge` 的 HttpClient 只有 `connectTimeout`，JDK 默认
  `Redirect.NEVER`；BMCLAPI 的 Maven 树是 302（→ CERNET/USTC），元数据读取直接抛异常，
  源被记为失败后回退官方 `maven.minecraftforge.net`（实测 24 KB/s–2.8 MB/s 波动）。
  改为 `followRedirects(NORMAL)`。
- **installer 的 libraries 也走镜像**：只要源列表里有 `bmclapi`，就追加
  `--mirror https://bmclapi2.bangbang93.com/maven/`。**结尾斜杠必需**——installer 字符串
  拼接，少了会变成 `mavencom/...` 并回退官方。
- **取消真正生效**：`TaskService#cancel` 现在会 `interrupt()` worker；三处下载循环
  （核心 jar / JDK / installer）按块检查中断。原来没有子进程的下载"取消失败"。
- **重试清理**：安装前删除 `.installer/*.part`，不再堆积被杀留下的半截文件。
- **阶段文案**：`Progress.stage()` 区分下载与安装，慢速下载不再显示成"正在安装"。
- **源失败告警**：每个源失败都打 WARNING，不再静默跳过。

**Java 托管：三家发行版 + 安装向导**

- 下载源从单一 Temurin 扩成三家：`temurin`（Adoptium API）、`microsoft`、`openjdk`
  （后两家经 Foojay DisCo 取元数据）。校验和仍以厂商伴随文件为准，读不到就跳过该包。
- 安装改成向导：先选 LTS 主版本（8/11/17/21/25，旁边是「MC 版本 → 最低 Java」对照表），
  再选发行版（许可、平台覆盖、更新节奏各一段说明），最后确认安装。某个发行版没有该主版本时
  按钮禁用并写明（实测 Microsoft 没有 8，OpenJDK 只有 17/21/25）。
- 托管列表显示发行版与完整版本；`runtime.json` 记录 `vendor`，旧记录按 Temurin 处理。
- **修卡片不显示**：设置页从不加载 MC 功能快照，而卡片只在"已开启"时才去刷新——
  直接打开 /settings 时永远看不到它。改为挂载即刷新；功能未开启时给一行说明，
  而不是静默消失。
- API：`POST /api/mc/java/install` 增加 `vendor`；`GET .../runtimes` 的 `available` 带
  `vendor`/`vendorLabel`，并新增 `vendors`（每个目录是否读到）。Oracle 仍被下载前的
  主机检查挡住。

**Java 托管：换成 JDK，并且把"装好却执行不了"的可执行位修掉**

- **解包丢执行位**：`Files.newOutputStream` 创建的文件按 umask 落成 `0644`，`bin/java`
  因此谁都执行不了——包括 root（`execve` 至少要一个执行位）。安装完成后统一规范化：
  目录 `0755`、`bin/*` 与 `lib/jspawnhelper` `0755`、其余 `0644`；`<data>/minecraft/java`
  也显式 `0755`，否则 umask 077 的镜像里 agent 连目录都进不去。属主仍归面板，不交给 agent：
  agent 只读、可执行，不能改 `java`。加了一条 POSIX 回归用例。
- **JRE → JDK**：`image_type=jdk`。空间换省事：多出来的 `javac` 与工具解包后约 300 MB/版本，
  但省掉"某个插件工具链非要 JDK"的二次往返。
- **设置页加独立页签**：侧栏「Node.js」下面新增「Java 运行时」（`?tab=java`），不和 Node
  混在同一页；只在 MC 功能开启后可用。托管列表（发行版 / 完整版本 / 占用 / 路径 / 删除）、
  安装向导。
- **只读展示**：面板自身 JVM、PATH 上的 `java`、MC 当前生效的 Java 三行只显示不给按钮——
  这三样面板一律不碰。
- **删除保护**：有实例把 `javaPath` 钉在该托管版本上时 `POST /api/mc/java/{feature}/remove`
  回 409，先去实例高级设置改掉。
- **不下载 Oracle JDK**：`install` 在下载前检查解析出的包 URL 主机（`oracle.com` 及其子域、
  `oraclecloud.com`、`java.com`），命中即拒绝并说明原因。Oracle JDK 的许可允许免费使用、
  不允许再分发，面板代下载即再分发。自己装的 Oracle JDK 仍可经 `mc.java_path` 使用。
- `GET /api/mc/java/runtimes` 增加 `panel` / `system` / `consumers` 与托管项的 `version`/`bytes`。

**MC 第三方核心下载源：官方直连已不可用，改成多源 + 配额守卫 + 手动导入**

- **PaperMC v2 已官方下线**（HTTP 410 `{"error":"sunset"}`）。面板写死 v2，就算网络通也拿不到
  Paper/Folia 的版本——这是核心目录"看起来是空"的直接原因。迁到 `fill.papermc.io/v3`，
  build 里直接带下载 URL 与 sha256。
- **新增 `McSources` 源层**（`<data>/minecraft/sources.json`）：每个核心是一张**有序源表**，
  官方源永远保留；国内镜像（MSL，可选 BMCLAPI）在官方不可达时接管。同一个核心的多个源
  **并行竞速**，官方源超时不阻塞镜像——实测核心目录总耗时从 12.7 秒降到 1.0 秒，
  Paper/Purpur/Folia/Fabric/Forge/NeoForge 六家全部有版本。判定依据是实测：
  从国内网络看 `meta.fabricmc.net` / `api.purpurmc.org` / `maven.minecraftforge.net` 直接超时，
  而 `file.mslmc.cn` 42 MB 的 jar 1.35 秒下完（约 32 MB/s）。
- **配额守卫**：MSL 的下载地址接口是 30 次/小时、60 次/天（响应头
  `x-ratelimit-limit|remaining|reset` 会落盘）。解析出的 URL 与哈希写进
  `cores/<core>/<version>/artifact.json`，同一个「核心 / 版本」只问一次；用满后跳过该源，
  而不是继续撞。
- **Forge / NeoForge 回退**：官方 maven 超时时改从 BMCLAPI 的 maven 元数据构建版本表，
  再不行用 MSL 的 installer 地址（loader 版本从文件名解析）。安装流程不变。
- **界面拆成两栏**：「MC 原版服务器实例」（`?view=mc`）与「MC 第三方服务器实例」
  （`?view=mc-third`），后者只在第二道关开启后出现；列表按 `scope` 隔离，原版向导不再显示
  第三方核心选项。
- **第三方独立向导**：核心 → 版本（可搜索）→ 运行环境（内存推荐值、Java 推荐、端口）→
  实例信息 → 安装；失败时可直接**上传 server.jar**（`PUT /api/mc/instances/{id}/core`）。
- **全局设置**：第三方那一栏新增下载源设置（顺序、自定义基址、配额余量、MSL 署名），
  保存走 `POST /api/mc/third-party/sources`（提权 + 审计）。
- 文档同步：`docs/minecraft.md` §19.2 重写为源表契约，新增 §19.6（界面与全局设置）。

**MC 上线前排查：上传被 1 MiB 截断、Forge 安装器以 root 运行、官方/第三方没分开**

- **插件/模组上传**：正文是 jar，走的 `PUT /api/mc/instances/<id>/files/<kind>` 不在
  `ApiGuardFilter` 的流式白名单里，于是 >1 MiB 的插件在到达处理器前就被 413 拒绝（`McMods`
  自己的上限是 256 MiB）。白名单补上这条路径，并加一条 2 MiB PUT 的回归用例。
- **Forge/NeoForge 安装器**：以前是裸 `ProcessBuilder`，容器里面板是 root，于是第三方
  installer 以 root 跑，能读 `auth/`、`certs/`、`server.yaml`；现在走 `Privileges#asAgent`，
  安装前把实例目录交给 agent，装完再交接整棵树。`.sha1` 取不到时不再静默跳过，日志写明
  "只剩 zip 结构校验"。安装器的 20 分钟超时也修了——原来是先读到 EOF 再 `waitFor`，安装器
  挂住时超时永远不生效，现在是独立线程排水 + 有界 `waitFor` + 超时杀进程树。
- **Forge/NeoForge 装好却起不来**：启动前置检查只看 `server.jar`，而这两个核心根本没有这个
  文件。改为 `server.jar 或 launch.json`，并让属主交接用同一个判据（以前会跳过 Forge 实例，
  然后以 "server.jar is not readable" 拒绝）。
- **官方 / 第三方分开**：实例 JSON 增加 `thirdParty`，列表与详情页显示「官方 / 第三方」徽章；
  第二道关同时管启动——关掉第三方后，已建实例也不能启动（404），不只是不能再新建。
- **RCON 密码不再从 GET 回显**：`GET .../properties` 以前把 `rcon.password` 一起返回；现在
  读接口剔除它。属性写入拒绝换行（`motd` 里塞 `
` 能写出额外的 `key=value` 行）。
  世界种子同样拒绝换行。
- **子进程环境收紧**：Minecraft 子进程（PTY 与 pipe 两条路径）以前继承面板全部环境变量，
  包括 `HDSL_ADMIN_PASSWORD` / `HDSL_AGENT_PASSWORD` 一类；现在只给
  `PATH/HOME/LANG/TZ/TEMP/SystemRoot…` 白名单，面板的 `HDSL_*` 不透传。
- 文档同步：备份要求停机（不是热备）、RCON 端口不要发布、第三方核心六个、安装器身份与
  `.sha1` 缺失时的真实保证。
- **MC 高级设置**：实例页新增「高级设置」，只放默认不用动、需要时才找的项——内存、端口、
  RCON 开关与当前实例的 Java 运行时（`enable-rcon` 之前没有界面入口，玩家页的命令和在线列表
  因此一直是「默认关闭」；Java 之前只能靠 `mc.java_path` 全局指定，实例级选择是新增的）。
  内存范围来自 `mc.max_memory_mb`（默认 8192、硬顶 16384），`GET /api/mc/feature` 新增
  `limits`；加一个一次性的「推荐值」按钮，按宿主可用内存、运行中实例数与核心类型估算，
  填完仍由人确认。MOTD、正版验证、视距等仍留在「服务端设置」。顺带修掉创建时只检查上限、
  不检查下限的口径，和 `mc.max_memory_mb` 大于硬顶时的错位。Java 选择写进 `instance.json`
  的 `javaPath`（空＝自动），只能从 `GET .../java` 的菜单里选，停服才能改；选中的 JDK
  消失时启动明确报错，不静默换一个。
- **第三方核心列表**：`/api/mc/cores` 原来串行等六个上游，Paper/Forge 不可达时整页要两分多钟，
  反代一超时就只剩原版；改为并行读取 + 整体 12 秒封顶 + 成功 10 分钟 / 失败 2 分钟缓存，
  `?refresh=1` 供向导的「重试」强制重读。创建向导增加加载中、读取失败、第三方未开启三种提示，
  不再把「第二道关没开」或「上游超时」静默伪装成只有 Vanilla 可选。
- **大世界导入/导出**：上传上限从 2 GB 提到 **128 GiB**；解压上限取 zip 声明的解压总大小
  （最高 512 GiB），并在解压前按"归档 + 解压后大小 + 旧世界 + 它的备份"检查空闲空间——50 GB
  的世界首次导入约需 100 GB、覆盖旧世界约需 200 GB，不够就拒绝，而不是解到一半填满盘。
  上传改走 XHR，页面显示字节与百分比；正文落盘后转成 `mc-world-import` 后台任务（进度按
  已解压字节对总字节上报），避免代理 60 秒超时把已完成的导入变成 504；导出的 `res.blob()`
  改成浏览器直接流式下载——5 GB 的 zip 不再先读进页面内存。zip64 路径用 5 GiB 全零、
  50 GiB 全零与 4 GiB 不可压缩世界实测过。

## v0.6.76 — 2026-11-07 · MC：Forge/NeoForge 安装器（安装式核心）

**Forge 与 NeoForge 不是下载一个 jar 就能跑：面板现在会跑安装器，再用它写出的启动参数启动**

- 新增 `McForge`：Forge 用 `promotions_slim.json` 选构建（`recommended` 优先，否则
  `latest`），NeoForge 用 Maven metadata（前两个数字编码游戏版本，跳过 beta/alpha）；
  三个上游地址都可换镜像（`HDSL_FORGE_MAVEN` / `HDSL_FORGE_PROMOTIONS` / `HDSL_NEOFORGE_MAVEN`）。
- 安装流程：下载 installer（校验上游 `.sha1`，镜像没有时退化为 zip 校验）→
  `java -jar <installer> --installServer`（cwd = 实例目录，输出进任务日志，20 分钟超时）→
  找 `libraries/**/unix_args.txt`（老版本退回启动器 jar）→ 写 `launch.json` → 删除 installer。
  安装器输出与退出码会带进错误信息，失败时看得到"卡在哪"。
- 启动：`McRuntime` 先读 `launch.json`，用 `@unix_args.txt nogui`（或 `-jar forge-….jar nogui`），
  没有才回到 `-jar server.jar`。安装器跑的 java 与实例实际启动用的是同一个（托管 JRE 优先）。
- 插件/模组：Forge/NeoForge 归入 `mods/`（loader = forge / neoforge），Modrinth 搜索与安装
  自动带上 loader 与游戏版本过滤；Paper 系的 `plugins/` 不变。
- 前端：核心下拉出现 Forge / NeoForge；向导按核心给出提示（安装器耗时、libraries 占
  100–300 MB 且跟实例走）；实例列表与详情页加核心徽章；完成页文案从"已下载"改成"已就绪"。
- 测试：`McForgeTest` 覆盖 promotions/Maven 解析、安装编排与 `launch.json`、
  安装器失败输出、`.sha1` 不匹配拒绝、老版本启动器 jar 回退、`launch.json` 路径逃逸拒绝。
- 仍未做：mrpack 整合包、混合服（Mohist/Arclight）。

## v0.6.75 — 2026-11-07 · MC 第三方核心：Paper/Folia/Purpur/Fabric、托管 Java、插件/模组

**MC 服务端从"只跑原版"变成"能跑插件服"，但第三方核心是一道独立的关**

- **第二道关**：`feature.json` 增 `thirdParty` 状态与独立同意记录（`McThirdPartyConsent`），
  条款文本 `third-party-terms-v1.txt` 随包、hash 回显；只有主功能开启后才能开，
  关闭主功能时级联关闭，有第三方实例在跑时拒绝关闭第三方核心，
  关闭主功能前必须先关第三方（回 409）。关闭时 `/third-party/*` 随主功能一起 404。
  审计 `mc.thirdparty.*`。
- **核心目录**：新增 `McCores`，直连上游公开 API——Paper/Folia（`api.papermc.io/v2`，SHA-256）、
  Purpur（`api.purpurmc.org/v2`，MD5）、Fabric（`meta.fabricmc.net/v2` 启动器 jar，上游无哈希，
  只校验 zip 并如实标注）；三个 base URL 都可换镜像，不接聚合单点。
  共享 `cores/<core>/<version>/server.jar`，实例里硬链接。实例清单新增 `core` 字段，
  旧清单缺省 vanilla，零迁移。启动参数四种核心统一 `-jar server.jar nogui`，
  进程/终端/端口/审计全部复用。
- **面板自托管 Java**：`McJavaRuntimes` 走 Adoptium API v3（`HDSL_JAVA_API` 可换镜像），
  装到 `<data>/minecraft/java/<feature>/`，sha256 校验后才解包（zip/tar.gz 都支持），
  先解临时目录再原子落位。选 Java 顺序：`mc.java_path` > 面板托管的最低满足版本 >
  宿主机 JDK > 面板自身 JVM；版本要求缺失时按内置表（1.20.5+→21、1.18+→17、≤1.16.5→8、26.x→25）。
  装系统目录的方案被否：免 root、随面板清理、Win/Linux 一致，`mc.java_path` 仍可指系统 Java。
- **插件/模组**：`McMods` 按核心映射 `plugins/`（Paper 系）与 `mods/`（Fabric）；
  停用=重命名 `.jar.disabled`；上传只收 jar、zip 校验、不覆盖；Modrinth 搜索按 loader
  与游戏版本过滤，安装取最新兼容版本并校验 SHA-512 才落盘；客户端专用 mod 只按元数据
  （`fabric.mod.json`/`mods.toml`/`quilt.mod.json`）标注，不自动禁用。
- **前端**：创建向导新增核心选择器（版本列表跟着核心走）；设置页 MC 卡片新增
  "第三方核心"开关（条款正文 + 勾选 + 提权）与托管 JRE 展示；实例页新增「插件/模组」页签
  （上传、启停、删除、Modrinth 搜索安装）。
- **契约文档**：`docs/minecraft.md` 新增 §19（第二道关、核心目录、托管 Java、插件/模组、
  仍未做清单），§2/§4.2 的"只做原版"表述已同步。
- 未做（明确不在本版）：Forge/NeoForge 安装器、mrpack 整合包部署。

## v0.6.74 — 2026-11-07 · TUI 插件跟上 dsh 0.2.2-rc.1；审批详情修复

**TUI 插件声明跟上 dsh 0.2.2-rc.1；审批详情不再显示 `[object Object]`**

- `plugins/dsh-inproc-tui` 升到 `0.2.2`，三个 `peerDependencies` 追加 `|| 0.2.2-rc.1`——按版本
  精确列出，不写 `<0.3`：semver 的预发布规则会让通配漏掉 rc，写宽了反而装不上。面板的
  `DshTuiPlugins.Entry` 同步版本与 `requiresDsh`，所以 0.2.2-rc.1 的实例能装、能开终端，而没跑过的
  `0.2.2-alpha.2` / 后来的 `0.2.2-rc.2` 仍然被拒（dsh 自己也会拒绝加载，面板不该假装可以）。
- 审批对话框的详情行把 `request.displayReason` 当字符串拼接，而它在 0.2.x 是本地化对象
  `{ en, zh, … }`，于是显示成 `[object Object]`。新增 `shared/i18n.js#textOf`：先取界面语言、再回落
  到其它语言，字符串原样通过；追问的 `question` / `detail` 同样走它。旧的 0.2.1-rc.3 上就存在这个问题，
  与 0.2.2 的接缝无关。

## v0.6.73 — 2026-11-07 · 实例 home 属主修复（cordis.yml EACCES）；构建换源提速

**实例 home 里 root 属主的文件：`EACCES …/profiles/web/cordis.yml` 的真正原因**

- `--help` 能力探测以前以面板身份（容器里是 root）运行。`dsh --profile <p> --help` 并不只读
  帮助：它先组合 profile，把 `cordis.yml`、`cordis.patch.yml`、`package.json` 写进
  `$DSH_HOME/profiles/<profile>/`。这些文件从此属于 root，而之后每次启动都降权到 uid 1000，
  于是写同一个 `cordis.yml` 得到 `EACCES`——报错长得像上游 bug，其实是面板自己留下的。
  探测现在包一层 `Privileges#asAgent`，与真正的启动同一个身份。
- 面板往实例 home 写的每一处，写完都把**文件连同目录**交接给 agent
  （`Privileges#handWrittenFile`）：账户路由与插件开关的 `cordis.patch.yml`、注入的
  `settings.yaml`、默认模型、`.env`、`pnpm-workspace.yaml`、安装清单，以及插件包解包出来的
  每个文件。只对面板数据目录**以内**的路径生效——实例的 home 可以指向用户自己的目录
  （`DshHomeMode.CUSTOM`），那里的属主不是面板能改的。
- `DshLauncher#prepareInstanceDirectories` 不再只 chown `instances/<id>/` 和 `home/` 两个顶层
  目录：它会检查 `profiles/`、`storages/`、`profiles/<p>/cordis.patch.yml` 这些 harness 要写的
  路径，发现属主不是 agent 就把整棵 home 交接一次。被旧面板写坏的老实例，下次启动自愈；健康
  的实例只付每路径一次 stat，不做遍历。
- 同类一起修掉：品牌安装在解包/构建完成后把 `releases/<version>` 整棵树交接给 agent（Hermes
  的 venv、opencode 的二进制目录都属于这种"安装器 root 写、运行时 agent 用"的树）；插件包
  解包逐文件交接，并在 pnpm 之前先准备好 profile 目录，否则 `pnpm install` 直接 EACCES。
- 技能包安装（`DshSkills#install`）同样以 root 拷贝，现在拷完把 `home/skills` 连同目标一起
  交接给 agent——harness 要重命名、禁用、删除自己技能目录里的东西；面板切换启用状态时写回的
  `SKILL.md` 也一并交接。
- 启动自检不再把 `hdsl/runtimes` 列为"漏交接"。托管 Node 是 agent 的解释器，**只读执行**，
  root 属主 `0755` 是设计而不是缺陷——agent 能改写自己的解释器，等于给持久化留门；真要做
  横向隔离是 per-instance uid 的事，不是给这棵树换属主。
- `scripts/verify-privilege-split.sh` 补两项：递归扫描实例 home 里非 agent 属主的条目（顶层
  检查看不见 `profiles/web`）、断言托管运行时对 agent **不可写**。
- 文档补一条容易被忘掉的事实：`Privileges#prepareAgentDirectory` 用 `0775`，`chmod` 会清掉
  setgid 位，所以 `instances/<id>` 与 `home` 从第一次启动起就不在 `/data` 的 setgid 链上。
- 测试：`PrivilegesTest` 增加两条——数据根内的写入会交接文件与目录、数据根外的写入保持原样。

**本地构建提速：apt 换源 + 缓存挂载**

- 新增 `--build-arg APT_MIRROR=<scheme://host>`：构建期把 Debian/Ubuntu 的 apt sources
  主机换掉（路径不变，`/debian` 与 `/debian-security` 都对得上）。换底座仓库和换 apt 源
  是两件事——底座走镜像站时 `apt` 仍然直连 `deb.debian.org`，而国内网络上那一步要拉
  ~10 MB 索引 + 几百 MB 包体。同一台机器实测：`deb.debian.org` 20 秒只下到 2.86 MB
  （142 KB/s），TUNA 1.09 秒下完 9.68 MB。
- `docker-install.sh` 的镜像源菜单现在连它一起定：国内模式带上
  `http://mirrors.tuna.tsinghua.edu.cn`，官方源清掉；另有 `--apt-mirror` / `--no-apt-mirror`
  单独控制，选择记进状态文件（`INSTALL_APT_MIRROR`），`--rebuild` 不用再写一遍。
- `/var/cache/apt` 与 `/var/lib/apt/lists` 改成 BuildKit cache mount：索引与 `.deb` 留在
  构建器里、不进镜像，改一行 Dockerfile 不再重新下载整个工具链。
- 构建失败的提示分成两种：以前只要日志里有 `dial tcp` 就说"卡在拉底座镜像"，而
  `apt-get` 连不上 `deb.debian.org` 时给出的正是这条建议——照做没用。现在按
  `load metadata` 与 `apt`/`Err:` 分开，各自给对应的开关，并说明中断后重跑会从断点继续。

## v0.6.72 — 2026-11-06 · 补上类型检查漏掉的那个联合类型

- `McJavaInfo.source` 的联合类型补上 `installed`。v0.6.71 里 `McRuntime` 已经会按
  MC 版本挑本机另一份 JDK 并把来源记成 `installed`，前端类型还停在
  `panel` / `configured` / `none`，于是 `tsc --noEmit` 报 TS2367、`build` 工作流
  两条腿都红——本地 `npm run build` 走的是 vite，不做类型检查，所以没在发布前拦住。
  构建产物与 v0.6.71 相同，这一版只是让发布对应的检查是绿的。

## v0.6.71 — 2026-11-06 · 镜像内置 JDK 25；审计修复一轮

**容器镜像内置 JDK 25，MC 按版本挑 JVM**

- 三个发行版分支都多装一份 JDK 25（Debian `openjdk-25-jdk-headless`、Fedora
  与 openSUSE `java-25-openjdk-headless`；拿不到时只告警，不中断构建）。
  起因是审计里那条：官方镜像装得上 `26.x`，却因为只有 Java 21 而启动不了——
  `Minecraft 26.3 needs Java 25 or newer`。
- 默认 `java` 仍钉在 21（Dockerfile 里用 `update-alternatives` 钉回来并打印
  实际默认值）：面板自己、以及 1.16 那批老版本，都留在实测过的组合上。
- 新版本要更高 Java 时由面板按实例挑：`McRuntime#javaFor` 在版本要求的主版本
  高于面板自身 JVM 时，扫本机已装的 JDK（Linux `/usr/lib/jvm`、`/usr/lib64/jvm`；
  macOS `/Library/Java/JavaVirtualMachines`）并选**满足要求的最低那个**。
  `mc.java_path` 与 `JAVA_HOME` 仍然一票否决。`java.source` 新增 `installed`，
  MC 卡片上据此说明"来自本机已安装的 JDK"。
- 启动前的拒绝文案同步：点名实际会跑的那个 java，并指出镜像里 25 的位置。

**审计修复：口径、崩溃与一处性能洼地**

- 开发者接口收到畸形 JSON 时答 500 而不是 400。`{"password":{}}`（elevate /
  agent-auth / agent-password）与 `"ttlSeconds":"abc"` 会一路走到 Gson 的
  `getAsString()` / `getAsLong()` 抛出来，被容器兜成 `internal error`：提权链
  的入口把「你发错了」说成「面板坏了」。现在按字段类型判定，必填项答 400，
  可省略项忽略。同类写法一并收紧：MC 的 `server.properties` 提交、品牌设置的
  `nodeRuntime`、Node 页的 `source` / `version` / `keepPerMajor`。
- 容器以非 root 运行时，启动横幅明说「UID 隔离没生效」。镜像设了 `USER` 或
  compose 写了 `user:` 时面板不再是 root，`setpriv` 那一层不存在，实例和
  agent 与面板同 uid、能读 `auth/` `certs/` `logs/`——而这样的部署一切照常
  启动，没人会发现自己少了这层保护。`docs/security.md §2.7` 补同一条。
- `GET /api/system/stats?history=240` 曾经是压测里唯一的洼地（同机 811 rps，
  其它端点 9k；p99 301 ms）。整份响应现在按采样序号缓存，只在采样器前进时
  重建，其余请求直接写缓存字节。
- `PnpmConfigFileTest` 的期望路径改用 `$HOME`，与产品代码同源。原先用
  `user.home`（passwd 那条），在 systemd / sudo / 容器把 `HOME` 指到别处的
  环境里必挂——审计里唯一失败的用例就是它。
- 文档：`docs/security.md §2.9` 补上 agent 密码只在 `instance`/`all` 两档有效、
  scope 实为七类；`docs/windows-bare-deploy.md` 补上「别用 SSH 会话里的
  `Start-Process` 常驻面板」（OpenSSH 的 job object 会随会话断掉带走整棵树）。
- `/api/versions` 与插件版本列表不再被「PATH 上有 node、却没有 npm」卡住。
  Debian/Ubuntu 把两者拆成两个包，这类主机以前会答 "npm was not found on
  PATH"，即使面板已经装好带 npm 的托管运行时；现在 PATH 那个查不了 registry
  就让位给托管运行时（PATH 优先，只要它能干活）。

## v0.6.70 — 2026-11-05 · 通用页性能面板；Windows 原生读数；品牌最后手段收敛

**通用页补全：系统资源、面板坐标、数据目录与日志**

- 新增「系统资源」卡：总览 / CPU / 内存 / GPU / 硬盘 / 网络 六个标签页，
  默认总览；每页有大号当前值、面积折线图（最近 60 个采样点）与明细。
  CPU 页是逐逻辑处理器的热力块；内存页用「使用中 / 已缓存 / 可回收 / 空闲」
  四段条区分 Linux 的页缓存，折线图按 0 到总量的绝对刻度画。
  带 1/2/5 秒刷新（可暂停；页面隐藏即停）。
- 性能页的说明按平台与运行环境切换：快照带 `host.os` 与 `host.environment`
  （container / wsl / native），「容器里…」只在容器里说；Windows 上明说
  缓存/每核等暂未采集，不再出现 Linux 专属文案或空网格。
- Windows 原生读数（JNA，无新依赖）：每核 CPU 走
  `NtQuerySystemInformation`，内存走 `GlobalMemoryStatusEx`（总量与可用；
  Windows 的 standby 缓存暂未采集），磁盘/网络走 PDH 计数器；任何一项取不到
  就降级为能力位 false，不编 0。
  超过 64 个逻辑处理器时只覆盖当前处理器组。
- 采样器两档：有人用面板时 1 秒一帧，没人时 30 秒；**不接受客户端指定频率**。
  240 帧历史只在内存，重启即丢，不写日志；首次打开带历史，曲线不用等。
- `GET /api/system/stats`、`/api/system/info`、`/api/system/storage`；
  存储按数据目录顶层子目录分桶（品牌/dsh/ZCode/MC/运行时/日志/凭据），
  缓存 60 秒，可手动重新统计。
- 概览卡新增只读的监听地址、访问基址、数据目录、公开地址与 Java 版本
  （端口取连接器实际端口，`port: 0` 时也正确）。
- agent 票新增 `system` scope（默认不给）；`/api/system/*` 加入开发者守卫
  白名单，`all` 档下监控页也只需登录会话。
- 异常才写日志：CPU/内存/磁盘/load 的持续阈值、10 分钟冷却、恢复行。
- GPU 仍是预留：`gpu: []` + 能力位 false，前端只留空分支；
  Windows 的 standby 缓存暂未采集，其余指标两端都是真数据
  （见 `docs/system-monitor.md`）。
- 「通用」页新增语言占位（简体中文，置灰）与日志保留卡（从「关于」搬来）。
- 磁盘列表不再列出光驱（`sr*`）与网络块设备（`nbd*`），避免多出全 0 的噪声。


**最后手段只留给 Octop 与 OpenCode**

- 单实例 + 固定反代只对 Octop、OpenCode 开放（`Brand#supportsLastResort`）；其余品牌的策略卡
  不再显示这两个开关，API 也拒绝；老 `policy.json` 里的残留字段读取时丢弃，不会让实例一直
  灰着又没有开关可关。
- 固定反代按品牌通用：`/i/octop/` 与 `/i/opencode/` 各自指向自己的保留实例，页面、反代、
  WebSocket 中继都用同一套别名解析。OpenCode 的路由补丁按新挂载名工作，原有 token 路径不受影响。
- `/i/opencode/` 加入实例 id 保留字，避免 dsh 实例与固定路径重名。

**agent 票默认只认本机回环**

- 新增 `agent.loopback_only`（默认开），设置页「开发者模式 → agent 访问 → 票的来源」
  可以切换：打开时非回环地址使用票一律 403 `agent_loopback_only`；关掉后才轮到
  `agent.remote_needs_session` 那条规则。
- `agent-auth` 在回环限制开着时直接拒绝远程换票，不再发一张注定用不了的票。
- 反代部署看到的来源恒为 `127.0.0.1`，这条限制等于不生效；那种部署要在反代上限制来源
  （`docs/panel-agent.md` §4）。

**MC 控制台成为 agent 的独立权限（`mc-console`）**

- 新增 scope `mc-console`：勾上才允许 agent 用
  `POST /api/mc/instances/{id}/console/ticket` 换 PTY 票据并附着 `/ws/terminal/{ticket}`；
  不勾时 `mc` 保持原样（启停、`console/input`、日志），行为与之前一致。
- agent 的 ticket 绑定**票 id**而不是会话 cookie：WebSocket 握手必须带同一张票的
  `Authorization: Bearer`，URL 里的 ticket 单独 attach 不了任何东西。
- 控制台每 30 秒回查一次票是否还活着；掐票、票到期、最后一次开发者提权结束都会关掉这条
  连接（显式掐票立即关）。
- 设置页「它最多能做什么」与 hdsl-tui「给 agent 签票」都多了这个勾选项；
  `docs/panel-agent.md` §3 与 §7.1、`docs/minecraft.md` 接口表同步。

**模型目录拉不到时，账户页不再被拖死**

- `GET /api/models/providers` 在没有目录副本时不再回 502：先返回内置供应商
  （`source: "builtin"`），账户页仍能选 DeepSeek、Moonshot、智谱等；目录在后台抓，
  抓到后下一次刷新就是完整列表。
- 有磁盘副本时立即用副本回答，刷新在后台；只有 `?refresh=1` 才同步抓。
- 目录下载的超时覆盖整个响应（含约 5.9 MB 的 body），不再出现“卡几分钟再报超时”。
- 账户页在 `builtin` 模式下显示一行说明与刷新入口。

**README 重排与截图**

- 结构改为 定位 → 主图 → 目录 → 亮点 → 截图 → 功能速览；`### 一些细节` 拆成截图表与
  功能速览表，目录与正文标题对齐。
- 截图换成 v0.6.64 的当前界面（8 张），新增 Node.js 运行时与 Minecraft 入口；
  演示数据集为 5 个停止态实例，无真实账号、供应商或口令。

## v0.6.65 — 2026-11-05 · 模型目录不可达时账户页降级；README 重排与截图

**agent 票的来源限制：非回环要有人**

- 新增开关 `agent.remoteNeedsSession`（默认关）。打开后，从**非回环**地址使用 agent 票时：
  - 浏览器签发的票（`POST /api/developer/agent-token`）照常可用——它本来就意味着有人
    打开面板、把 token 复制出去；
  - agent 密码换来的票（`POST /api/developer/agent-auth`）必须同时带一个有效的登录会话，
    否则 403 `agent_remote_session_required`。长期密码泄漏不再等于远程可用；
  - 本机回环不受影响。
- 票现在记录签发方式（`MintedBy`），`AuthFilter` 先验票、再判来源。带票又带 Cookie 的
  请求按票处理：票的 scope 和 `agent:<label>` 审计身份不会被会话放大。
- 反代部署时 `getRemoteAddr()` 是反代地址（通常是 `127.0.0.1`），这条规则会当成回环
  放行；那种部署要在反代上限制来源（见 `docs/panel-agent.md`）。

**hdsl-tui：能签 agent 票，实例能启停**

- 主菜单多一项「给 agent 签票」：勾选范围（读取 / MC / 实例 / 配置 / 插件），用当前
  提权会话签一张票，生成一段可直接粘进 agent 输入框的话（`127.0.0.1` 地址、token、
  范围、与提权同生命周期），并用 OSC 52 请求终端复制。
- token 单独占一页、单独一行：桌面终端能框选复制就用提示词；不能就手抄这一行输进
  agent 的 TUI，不必从整段话里挑。
- 实例菜单多「启动实例」「停止实例」；详情显示版本、端口、挂载地址、失败原因、账户、
  终端是否在跑、是否被策略禁用。旧面板缺字段时留空，不报错。
- 主菜单多「Minecraft 控制台」：列出 MC 实例、启动/停止、附着服务器自己的控制台。
  进入前单独声明退出方式（`Ctrl-] q`，服务器继续跑），并提示 `stop` 是停服命令而不是
  退出键；Windows 管道模式的服务器明确说“只能在网页看”。创建/配置/备份留在网页。
- 不列 Octop：它没有终端，客户端只为 attach 而存在，列出来是个死胡同。
- Node 运行时不在客户端设置：它是面板的安装/配置面，TUI 只负责访问实例。

**Node 页的下拉框不再裁字；窄屏可读**

- `select.input` 不再写死高度，Node 页三个下拉改用 `min-height` 与按内容宽度；
  “国内镜像（npmmirror）”不再显示不全。
- 手机/窄屏：设置行改上下两行，Node 下拉占满整行。

## v0.6.63 — 2026-11-04 · Node 运行时管理：大版本通道、校验、回退与 GC

> 这一版把 Node.js 从“安装前提”变成面板托管的运行时：按大版本看、下载校验、
> last-known-good 回退、每大版本保留两个版本的 GC，以及从已安装 release 读
> `engines.node`。

### 运行时管理

- **大版本通道**：`22` / `26` / `lts` / `current`；设置页列出 18–26 每个大版本，
  未安装的也显示卡片。`auto` 先用 PATH 上可用的 Node，再回落到最新托管运行时；
  `system` 是 `auto` 的别名，旧 manifest 的 `global` / `inherit` 继续可用。
- **校验**：下载按发布版 `SHASUMS256.txt` 校验；镜像缺校验文件时回退官方源，
  两源都没有则记录“未校验”；哈希不匹配直接失败。
- **last-known-good**：dsh 就绪、品牌拿到端口时记录；消费者可一键回退到上一次
  成功启动的运行时。
- **GC**：每个大版本保留 2 个运行时，跳过使用中、默认和回退锚点。
- **健康与支持分开**：低版本（18/20）装得上、能运行，但不在 dsh 支持的范围内，
  页面分别显示 `health` 和 `supported`，不再当成“损坏”。
- **消费者直接切换**：设置页的消费者行可以改实例/品牌的 Node 选择，
  下次启动生效；实例与品牌下拉只列已安装的通道。

### 品牌与实例

- 品牌要求直接读已安装 release 的 `engines.node`；Hermes 新版声明的 `>=26`
  会在启动前被拒，而不是装完才发现跑不起来。
- 实例与品牌各自选择 Node；品牌支持全局默认值。
- `/api/node` 增加 `channels` / `consumers` / `resolve` / `default` / `disable` /
  `channels/update` / `revert` / `gc`。

## v0.6.62 — 2026-11-03 · 容器里的身份推导修好：由 agent uid 的子进程读 /proc

> v0.6.61 的自杀闸在容器里恒为「推不出身份」：面板是 root、实例是 uid 1000，而镜像的
> 默认 capability 集没有 `CAP_SYS_PTRACE`，root 读不了 agent 的 `/proc/<pid>/fd`。于是
> 未绑定的票对停 / 删 / 开终端一律被拒——连指向别的实例的操作也拒。

### 修复

- 新增 `web/auth/SocketOwnerLookup`：`inode → pid` 的扫描在有 uid 分离的部署里交给一个
  `setpriv` 到 agent uid 的探针子进程（与实例同一 uid，内核允许读对方的 fd 表），面板
  自身不被授予 `CAP_SYS_PTRACE`。无 uid 分离的裸部署仍走进程内扫描，行为不变。
- 探针有并发上限（4）与 4 秒超时；取不到 `java`、无 classpath、非零退出、输出不可解析、
  超时——一律 `null`，闸继续 fail-closed，不猜。
- `PeerIdentity` 的 `socketOwner` 改为调用它；其余链条（`/proc/net/tcp{,6}` 找 inode、
  父链上行、命中后成员校验）不变。

### 验证

- 容器内实测：root 读 `/proc/<uid 1000>/fd` 为 `EACCES`；同一个 inode 由 uid 1000 的
  探针正确解析出 pid。
- 新增 `SocketOwnerLookupTest` 5 项（进程内扫描、探针、fail-closed、无 uid 分离分支）。
- 后端 1223 项，仅 2 个已知的 `BrandTuiTest` 失败（宿主机没有 node）。

文档：`docs/panel-agent.md`、`docs/security.md` 同步说明容器里的 uid 分离与探针。

## v0.6.61 — 2026-11-03 · agent 自杀闸改用内核身份推导；Windows 覆盖；一键签票回归

> 这一版换掉「票上自报 instanceId」这条判断：面板不再相信调用者说自己在哪，而是从内核
> 事实推导它属于哪个实例。Linux 走 `/proc`，Windows 走 `GetExtendedTcpTable`。在能推出
> 身份的地方，未绑定的票也不再能停掉跑着自己的实例。

### 自杀闸：从「自报」到「内核身份」

- 新增 `web/auth/PeerIdentity`：
  - **Linux**：请求的对端端口 → `/proc/net/tcp{,6}` 里 ESTABLISHED 的对端 socket inode →
    `/proc/<pid>/fd` 的持有者 → 父链 → 实例根；
  - **Windows**：`GetExtendedTcpTable(TCP_TABLE_OWNER_PID_ALL)` 直接给出连接与 owner pid，
    父链与成员校验用 JDK 的进程快照；
  - 只有同 netns 的 loopback 调用能推出身份；其余情况答 `null`，不猜。
- **优先级是内核的**：推导出的身份压过票上的绑定，两个方向都压。证明在目标实例里 → 409
  `self_operation_refused`；证明在别处 → 放行（即使票绑的是目标）。
- **推不出身份时 fail-closed**：票绑了目标 → 拒；票未绑定 → 停 / 删 / 开终端一律拒；
  `launch` 只在目标终端正开着时拒（停着的实例里没人可杀）。
- 覆盖 dsh / 品牌 / ZCode 三族的停、删、终端，以及 `launch`。
- 3 秒缓存（按调用方端口）；读不到 `/proc` 或 Windows API 失败都退化为 `null`，不抛错。

### 一键签票回归，未绑定也安全

- 设置页恢复「生成票并复制给 agent」与 `POST /api/developer/agent-token`。因为身份判断
  来自内核，未绑定的票在推不出身份时对致命操作 fail-closed，不再是绕过自杀闸的后门。
- 提示词里的面板地址改由服务端自报，不再用 `window.location.port || 3080` 猜端口。

### 边界（写进文档）

- **反代 / 跨 netns**：身份推不出 → 未绑定票不能停 / 删 / 开终端；绑定票仍可管别的实例。
- **进程脱管**（double-fork 到 init）会走不到实例根；未绑定票因此被拒，但「绑定到别的
  实例」的票停自己仍会放行——需要故意脱管 + 绑错同时发生。
- **Windows** 已有实现与假表单测，真机验收仍建议补一次。

文档：`docs/panel-agent.md`、`docs/security.md` §2.9、`README` 同步。

## v0.6.60 — 2026-11-03 · agent 访问收口为「只用 agent 密码换票」；启动第一行立刻打印

> 上一轮加的「面板生成票并一键复制」整套撤掉：那条路给出的是不绑定实例的票，agent 拿它
> 停掉跑着自己的实例，就是 dsh 自杀的来源。agent 现在只用自己的密码换票，权限模型回到
> 当时：五类 scope，加上可选的实例绑定。

**agent 访问：只保留 agent 密码**

- 只有一条门：`POST /api/developer/agent-auth`。agent 用自己的密码换一张 bearer 票；免会话
  （curl 可用），但**必须有人正在提权**，票的寿命被这次提权截断，操作员登出 /「停止提权」
  后票当场失效。
- agent 密码可以单独设上限（`read` / `mc` / `instances` / `config` / `plugins`），调用方只能
  要得更少；默认只给 `read`。
- **删除了**面板侧的 `POST /api/developer/agent-token` 与设置页的「生成票并一键复制」：
  那套能签出不带 `instanceId` 的票，等于绕过「别停自己」的自检。面板现在不提供任何
  生成票的入口，票只能由 agent 用密码换。
- 实例绑定仍在：换票时带 `instanceId`，面板会拒绝停 / 删 / 开该实例的终端
  （`409 self_operation_refused`）。**不带绑定时面板无法判断「这是不是我」，会放行**——
  这是当时的真实边界：防的是模型犯傻，不是恶意。
- 面板桥插件 `plugins/dsh-panel-agent` 不随面板发布；`/i/*` 与 `/ws/*` 仍是浏览器通道，
  任何 scope 都不放行；凭据类路由（`admin-password`、`/open`、`/exports`、`/developer/*`、
  `/auth/*`）继续在网关拒绝。
- 保留上一轮的正确性修复：登出清提权、守卫对 agent 让路但 agent 票 ≠ 开发者令牌、
  按解密后的路由路径判断、响应里摘掉 `url` / `token`。

**启动第一行立刻打印**

- 换 jar 后如果数据目录里有上次删除失败的残留树，清扫会花几十秒且没有任何输出。
  现在启动第一件事打印 `HDSL-web starting (pid N)`；清扫超过 1 秒补一行耗时。

## v0.6.57 — 2026-11-02 · ACP 中文不再乱码；Windows 裸部署全功能冒烟

> 这一版只修一处真缺陷：ACP 的 stdio 被按控制台代码页解码，中文 Windows 上回复变成乱码还会
> 丢字。顺手把这次真机冒烟的结果写进文档，并修掉冒烟脚本里那条自己会误报的版本断言。

### ACP 控制台：固定 UTF-8，不再跟随控制台代码页

- `ManagedProcess.pumpInputStream` / `pumpErrorStream` 原来固定用
  `OperatingSystem.NATIVE_CHARSET`。中文 Windows 上是 GBK，而 ACP 的 JSON-RPC 行按规范是
  UTF-8——于是 ACP 控制台里的中文回复显示成「浣犲ソ」这类乱码，个别字节还会被替换掉
  （实测「你好，很高兴见到你，祝你今天一切顺利！」里的「一」直接丢了）。
- `ManagedProcess` 增加带 `Charset` 的重载；`DshAcpClient` 的 stdout / stderr 明确用 UTF-8。
  POSIX 行为不变（`NATIVE_CHARSET` 本来就是 UTF-8），dsh 主进程与控制台命令的日志口径
  也不动——只把「协议流」这一条从控制台代码页里摘出来。
- 复验：Windows 10 + JDK 21 裸部署，同一句问候返回「你好，很高兴见到你！😊」。
  全量测试 1182 项 / 2 失败（`BrandTuiTest`，宿主机没 node）/ 74 跳过，与基线一致。

### 文档：Windows 裸部署的全功能冒烟记录

- `docs/windows-bare-deploy.md` §9.7：干净 PATH（无 Node）下，接口 42 项、一键装托管运行时
  `26.13.0`、dsh / kimi / opencode 三个 agent 的 ACP 问候、MC 起服到 `Done`、ConPTY 终端
  字节流的实测结果，以及上面这条乱码缺陷的定性与修法。
- `docs/minecraft.md` §15.2：补「MC 版本 ↔ Java」对应——`26.x` 要 Java 25+，只有 JDK 21 的
  机器选 `1.21.x`（`1.21.11` 已验证）；面板会在启动前拒绝并报出实际 Java 版本。
- `docs/deployment.md` §4：写明**本地 `RELEASE_VERSION` 构建出来的镜像不等于发布产物**，
  版本号只决定 jar 里的字符串；核对 jar 哈希要跟 Release 的 `SHA256SUMS.txt` 比。

### 验证脚本

- `scripts/api-smoke.py` 不再把 claude-web 的版本写死成 `0.4`（latest 已是 `0.5.0`，会让脚本
  误报）；改成只校验版本号的形状。

## v0.6.56 — 2026-11-02 · 部署脚本能体检、能开 MC 端口；MC 的 Windows 裸部署收口

> 这一版两件事：把「照文档做完还是跑不通」的几处根因修掉（加组判定、提示缺 buildx、
> 脚本权限位），并把 Windows 裸部署跑 MC 的收尾补齐。功能面不变，默认行为不变。

### 部署脚本：MC 端口可映射；缺什么软件按发行版给准确命令

> 出发点是一次「照着文档在 Ubuntu 上做完，还是没跑通」：一是加组判定看起来在说
> 「没过」，二是提示里给的安装命令本身缺东西（Ubuntu 的 `docker.io` 不带 buildx）。
> 脚本自身不改行为的时候，至少要把这两件事查干净、说清楚。

**MC 端口：默认仍然不开，但要开不必再手改命令**

- `docker-install.sh` 在「配置」里多问一项 **MC 游戏端口**（`25565` / `25565-25575` /
  `25565,25566`，最多 128 个），回车 = 不开；非交互用 `--mc-ports` / `--no-mc-ports`。
  宿主与容器同号，会校验数字、范围方向和数量，`docker run` 里逐条变成 `-p 宿主:容器`。
- 没开 MC 时行为和以前完全一致；开了之后安装结束的摘要会列出映射了哪几个端口，
  并提醒实例的 `server-port` 要与之一致。

**`--doctor`：只体检，不改任何东西**

- 一条命令过一遍 docker CLI、守护进程与权限、buildx、compose、构建所需磁盘、面板端口、
  MC 端口；每一条给「现在是什么」和「这台机器上能用的那条修法」。
- 面板端口被**本脚本自己的容器**占着时按「升级时正常」处理，不再当成冲突告警。

**加组判定：三种状态说清楚，不再把「已加好」报成失败**

- `group_db_has_user` 现在也认「主组就是 docker」这种配法；读不到 `getent` 时退回直接读
  `/etc/group`。
- 出问题时打印三行：你是谁、这个会话有什么组、`/etc/group` 里的 docker 成员是谁——
  加到 `root` 身上、或加的是另一个账号，一眼能看出来。
- `usermod` 之后立刻复核并明说「已写入 /etc/group」；仍然拿不到组时明确写「不算失败」，
  并给出 `newgrp docker` / `sg` / 重新登录三条路。

**提示里的命令按发行版现算（apt / dnf / zypper / pacman / apk）**

- 装 docker、装 buildx 各一段，包名按本机包管理器给；老发行版没有 `docker-buildx`
  这个包名时，给 Docker 官方源里的 `docker-buildx-plugin`，以及任何发行版都能用的兜底：
  用 `docker/buildx-bin` 镜像把插件二进制拷进 `~/.docker/cli-plugins/`。
- `--install-docker` 装完 docker 会顺手补 buildx；补不上只告警（`--pull` 不需要它）。
- **`docker-install.sh` 在仓库里的权限位是 `100644`**——新克隆下来 `./docker-install.sh` 直接
  `Permission denied`。改成 `100755`，`scripts/install-hdsl-tui.sh` 与
  `scripts/verify-privilege-split.sh` 一并修正。
- `README` 与 `docs/deployment.md` 同步：MC 端口三种开法、`--doctor`、各发行版的 buildx 装法。

### MC 的 Windows 裸部署收口；Java/存储位置可见；Hermes 平台提醒前置

> 这一轮不新增功能面，只把「Windows 裸部署跑 MC」从“代码里有”变成“能解释、能收尾”。
> 顺序上先保证进程与端口不会留下孤儿，再把 Java 和目录摊开给用户看，最后把 Hermes
> 的 Windows 限制从品牌页提前到页签与创建/启动入口。

**MC：面板退出时不再留下 Windows 孤儿服务端**

- `McRuntime.stopAll()` 现在会遍历所有运行中的实例：先写 `stop` 进 stdin，共享一个截止时间等
  优雅退出，超时才 `ProcessTree.terminate`（Windows 上会连子进程一起收）。`HdslServer.stop()`
  在关终端之前调用它。
- 之前 Windows 的 pipe 进程只存在 `McRuntime.pipes` 里，`HdslServer.stop()` 只停了品牌和
  `terminals`，于是关面板/重启服务后 `java.exe` 会继续占着 25565 和世界文件，新面板却显示
  `STOPPED`。这就是“裸跑 Windows 全是问题”里最典型的一条。

**MC：启动前先看端口，删除前先清残留**

- `McPorts.isFree()` 做真实 bind 探测（Linux 允许 `SO_REUSEADDR` 以忽略 `TIME_WAIT`；
  Windows/macOS 不允许，否则旁边有监听也会误报空闲）。启动前检查游戏端口，RCON 只在
  `server.properties` 的 `enable-rcon=true` 时才检查。
- `/api/mc/ports` 和创建时的自动分配现在同时避开“实例台账占用”和“操作系统上真的有人监听”。
- 实例的 `server.jar` 参数改成绝对路径；删除实例前调用 `ProcessTree.killReferencing`，这样
  面板被硬杀后留下的、命令行里带着该实例目录的 `java` 进程能在 Windows 上被收掉。

**MC：控制台入口在 Windows 上是真的可点**

- `consoleAvailable` 原来只看 PTY 会话，pipe 服务端永远显示“不可用”。现在按
  `consoleMode()` 判断；Windows 上按钮文案是“打开控制台（日志 + 命令）”，并在打开前
  就说明本平台没有 PTY，不再先假装能开交互终端。

**MC：Java 与编码**

- 默认 `mc.java_path` 继续解析到面板自己的 JVM（裸部署能跑起来就证明至少有一个 Java），
  新增 `GET /api/mc/feature` 的 `java` 字段：路径、`java -version`、主版本、来源
  （`panel` / `configured`）以及 `ok/error`。设置页在功能关闭时也能看到这些。
- 配置了不存在/跑不起来的 Java 时，启动前直接报出路径和原因，不再静默跳过检查后让
  Minecraft 自己失败；`java -version` 的 `1.8.0_x` 现在解析成 8，而不是 1。
- 子进程加 `-Dfile.encoding=UTF-8` / `-Dsun.stdout.encoding=UTF-8` /
  `-Dsun.stderr.encoding=UTF-8`：JDK 17 + 中文 Windows 的默认 GBK 与 pipe pump 的 UTF-8
  不再互相打架。
- `-Xms` 不再固定 512M：创建表单允许 256M，固定值会让 JVM 在 Minecraft 打印第一行前
  就以“初始堆大于最大堆”退出。`-Xms` 取 `min(512, -Xmx)`。

**MC：把服务器目录摊开**

- `GET /api/mc/feature` 新增 `storage`：实际根目录、来源（`HDSL_MC_DIR` / `mc.dir` /
  `$HOME/mc`）、是否可写、旧位置。设置页的 MC 卡片无论开关状态都显示
  “服务器目录 + Java + 控制台形态”，并说明每个实例在 `<root>/<id>/`、改配置不会搬已建实例。
- Windows 启动时额外检查：目录落在 `systemprofile` / `C:\Windows` 下（SYSTEM 服务账户）、
  路径超过 120 字符、目录不可写，各打一行警告；这些是“按钮能点但服务端起不来”的常见根因。

**Hermes：平台限制前置**

- 新增 `GET /api/brands/capabilities`（纯本地、不请求网络）：每个品牌在这台机器上能不能装、
  不能装的原因。品牌页签先渲染“不可用”标签，`title` 里是同一句原因。
- `POST /api/brands/<brand>/instances` 与 `BrandRuntime.launch` 也检查
  `Brand.installableOn`：从 Linux 拷过来的 `/data` 里若已有 Hermes 实例，在 Windows 上会得到
  “Hermes 只能在 Linux / macOS 上安装…”而不是 `error 193`。
- 启动横幅在 Windows 上打印平台受限的品牌与 MC 控制台形态；文案仍只从
  `Brand.notInstallableReason` 出，不在前端复制判断。

**验证**：新增 `McRuntimePipeTest`（在 Linux 上跑 Windows 的 pipe 路径：启停、命令、stopAll、
端口占用）、`McPorts.isFree`、`McRuntimeTest` 的 Java/堆参数用例、`BrandApiTest` 的
capabilities 用例；`npm run typecheck` 通过。

## v0.6.55 — 2026-11-01 · 小版本：把「root 建、agent 写」查完；`docker-install.sh` 能换源、能对 buildx 说话

> 起因：一次演示翻车——Octop 实例起不来（`unable to open database file`），MC 实例起不来
> （`code 1`、控制台 0 字节），而同一条命令在面板外面跑得好好的。两个都是同一个形状：
> **面板（容器里是 root）建的东西，交给以 agent（uid 1000）运行的子进程去写**。这类 bug 在
> setgid 数据卷上完全看不见，一旦 `/data` 是普通 bind mount 就全炸。所以这一版不只修撞上的
> 两个，而是按这条规则把同类路径全查了一遍。
>
> 另：`docker-install.sh` 在 Docker Hub 不可达、没装 buildx 的机器上也走不通，一起补了。

**把「root 建、agent 写」这条规则一次查完**

- 上一次只修了撞上的那两个（Octop、MC）。按同一条规则又发现三处：
  - **dsh 实例**：`instances/<id>/` 与 `home/` 由面板（root）创建，harness 和它的 `pnpm`
    安装都以 agent 身份写进去——启动与安装前现在都会交接（`DshLauncher#prepareInstanceDirectories`，
    只接管 `<data>/hdsl` 下面的路径，实例指到操作者自己目录的 home/workspace 不动）。
  - **dsh 安装目标** `instances/<id>/dsh`：面板建目录、`pnpm install` 以 agent 写入。
  - **品牌版本目录** `brands/<brand>/releases/<version>`：`pnpm add` 以 agent 写入，面板建目录。
  - **ZCode 的 `data/`**（`ZCODE_DATA_BASE_DIR`）：原来只交接了 `home/`，而 `tasks-index.sqlite`
    和 provider 状态都在 `data/` 下，同样是 agent 写。
- 新增启动自检：`Privileges#auditAgentRoots` 在面板启动时把「agent 要写、却还是 panel 属主」
  的目录各打一行警告——这一类 bug 在 setgid 数据卷上完全看不见，换成普通 bind mount 才炸，
  所以需要在启动时就说出来。
- `scripts/verify-privilege-split.sh` 补成一张完整清单（以 agent 身份 `touch` 实测可写性）：
  dsh 实例目录与 home、ZCode 的 `data/`、MC 实例目录、品牌 `releases/` 的每个版本、
  `pnpm-store`、`corepack`。

**根建的文件交给 agent：Octop 打不开自己的库，MC 服务器起不来**

- 症状一（Octop 品牌）：实例启动即死，日志里是
  `sqlite3.OperationalError: unable to open database file`。
- 症状二（MC 实例）：点启动立刻变成 `failed: the server exited before it was ready (code 1)`，
  控制台 0 字节，`logs/`、`world/` 一个都没生成；把同一条 `java -jar server.jar nogui` 拿到
  面板外面跑，18 秒就 `Done`。
- 同一个原因：**面板（root）创建的东西，交给 agent（uid 1000）的进程去用**。
  - Octop：`octop init` 原来是面板以 root 跑的，`octop.db`、`credential.txt`、`plugins/` 全成了
    `root:root 600`，而运行时是 hdsl——它开机第一件事就是打开那个库。
  - MC：实例目录与 `server.jar` 同样是 root 建的；`/data` 若不是 setgid（宿主 bind mount、
    umask 022），子进程连 `logs/` 都建不出来，Java 在 Minecraft 写第一行日志之前就退出。
- 修法：
  - `octop init` 改为以 agent 身份执行（`Privileges.asAgent`），新建的实例一开始就是对的；
    老实例在下次启动时由 `handOctopState` 把整棵树交给 agent（只改属主、不动 mode）。
  - MC 启动前调 `prepareDirectories`：先看 `server.jar` 的属主，不是 agent 就把实例树交过去；
    交完仍读不到 jar 就直接拒绝并点名文件，而不是让孩子自己退出。
  - 子进程一个字节都没输出就退出时，错误信息补一句"它什么都没打印"和该查什么（jar 可读性 /
    目录可写性），而不是只给一个 `code 1`。
  - `Privileges.handTreeToAgent`：整树只改属主（保留 mode，`.venv/bin` 的可执行位不能被抹），
    且跳过符号链接（`chown` 会跟随链接，实例里的链接可能指向外面）。

**`docker-install.sh`：Docker Hub 拉不到底座时，现在有两条路**

- 症状：`docker-install.sh` 在构建第一步就死——`failed to resolve source metadata for
  docker.io/library/node:22-bookworm-slim: … dial tcp … connect: connection refused`。
  脚本此时只说"构建失败"，没有提它其实还有别的走法。
- 新增 `--base-registry <前缀>`：三个底座镜像（前端用的 `node`、编译用的 `eclipse-temurin`、
  运行时底座）一起换到指定仓库，构建仍在本地做。Dockerfile 里这三个底座原先只有运行时那个是
  ARG，另两个写死在 `FROM` 里。
- 新增 `--pull <版本|镜像>`：不构建，直接拉 `ghcr.io/nas200-timmy/hdsl-web:<版本>`（多架构，
  amd64/arm64 自动挑）并按本地镜像名打标签，后续 `--no-build` / `--recreate` 照旧认它。
- 构建失败时识别「拉不到底座」这一类错误，并把这两条路印出来。
- 顺手修一个真 bug：`--base fedora` / `--base opensuse` 的 `HDSL_BASE`、`HDSL_ENCODERS`
  **只在 dry-run 里打印过，真构建时一个都没传**——选 Fedora 会静默构建出 Debian 的镜像。

**`docker-install.sh`：默认走国内镜像站，镜像源改成一个上下键菜单**

- 底座仓库默认改成 `docker.m.daocloud.io`（三个底座的多架构 manifest 实测都在），
  不用再每次补 `--base-registry`；要回官方用 `--no-mirror` 或 `--base-registry docker.io`。
- 「国内镜像源」这一问改成菜单，一次把 npm、Node、底座三件事一起定下来：
  国内（推荐，底座 DaoCloud）/ 国内（备选，底座 `docker.1ms.run`）/ 官方源。
  默认项跟着上次走；`--yes` 或非 TTY 时直接取默认项并在 stderr 说明选了哪个。
- `--base-registry` 记进状态文件（`INSTALL_BASE_REGISTRY`），重跑不用再给；`--no-mirror` 清掉它。
  显式给了 `--base-registry` 时不再弹那个菜单的仓库部分（否则会把答案盖掉）。

**`docker-install.sh`：先探 buildx，缺了直接给装法**

- 症状：`ERROR: BuildKit is enabled but the buildx component is missing or broken`——Docker 23 起
  `docker build` 本身就是 buildx，而发行版的 `docker.io` 包不一定带插件（Ubuntu 24.04 上
  `docker-buildx` 是独立包）。原来的脚本只会把这一行原样吐出来，然后"镜像构建失败"。
- 「检查 docker」多探一次 `docker buildx version` 并打印结果；真到构建时缺 buildx 就给出按来源
  分类的装法（`docker-buildx-plugin` / `docker-buildx` / dnf），并提示 `--pull <版本>` 这条
  完全不需要 buildx 的路。
- `--pull` 不受影响：它只做 `docker pull` / `tag` / `run`。

**升级提示**

- 换 jar / 重建容器即可，卷不用迁。老实例的属主问题在这几处修法的第一次启动时自动纠正
  （都是幂等的），不需要手工 `chown`。
- 启动日志里如果出现 `Agent-facing directory is owned by uid 0 (expected 1000): …`，那就是
  这一版修的那一类；下次启动会自动纠正，日志只是告诉你**这个部署的数据目录不是 setgid**。
- 容器里建议跑一次 `scripts/verify-privilege-split.sh`（以 uid 1000）：它现在把
  dsh 实例目录与 home、ZCode `data/`、MC 实例目录、品牌 `releases/`、`pnpm-store`、`corepack`
  逐条 `touch` 实测一遍。

**验证**：全量 1173 用例 0 失败 / 26 跳过；`McRuntimeTest` 新增"子进程零输出退出时说清楚"，
`PrivilegesTest` 新增"整树交接保住 mode、不跟随链接"。

## v0.6.54 — 2026-11-01 · 小版本：终端可以同时开了（虚拟线程把 carrier 钉光）

> 起因：开一个终端正常，开第二个（另一个品牌）只有闪烁的光标。查下来不是"只能单线程"，
> 是虚拟线程被**本地调用**钉在 carrier 上——每个终端两条，4 核机器两个终端正好占满。

**两个终端不能同时用：PTY 的阻塞读取跑在虚拟线程上，把 carrier 钉光了**

- 症状：开一个终端正常，再开第二个（另一个品牌）只有一个闪烁的光标——进程在跑、WebSocket
  连着，一个字节都没到屏幕。
- 原因：`PosixPty` 的 pump 与 reaper、`TerminalSession` 的 pump 用的是虚拟线程。这三个循环
  都阻塞在**本地调用**里（`poll` + `read`、`waitpid`），而虚拟线程在 FFI 下调期间**不能从
  carrier 上卸载**——每个被钉住的 carrier 就是一颗核。每终端两个、第二个终端再来两个，4 核
  机器上 carrier 正好被占满，第二个终端的 pump 排不上队。
- 现在这三个循环走新增的 `VirtualThreads.startPlatform`（daemon 平台线程）。这条规则别处早就
  是这么写的：`BrandRuntime`、`ZcodeRuntime`、`DshAcpClient` 的子进程输出泵一直是
  `new Thread(...)`，PTY 是唯一用了虚拟线程的那个。
- **同一条规则一次收干净**（否则只是把同一颗雷往别处挪）：
  - `TaskService` 的任务线程：安装/启动全程在等子进程（`Process.waitFor`）和读管道，一个任务
    就钉住一个 carrier 几分钟。改回平台线程池——这也正是 JDK 17 上本来就走的路径，21 上"顺手
    变成虚拟线程"才是那次偏差。
  - MC 服务器的管道泵（`McRuntime.pumpPipe`）：一个服务器进程跑多久，它就在本地 read 里等多久。
  - 这样"会阻塞在本地调用里"的活（PTY、子进程管道、`waitpid`）全在平台线程上；虚拟线程只留给
    真正会被 JDK 卸载的 I/O（HTTP、socket、`Thread.sleep`）。
- 核数无关：每个终端固定 2 条平台线程，不再占用 carrier。1 核能开两个终端（只是慢），24/64 核
  更不会有原来的悬崖。
- 测试：`VirtualThreadsPlatformTest`（`startPlatform` 必须是 daemon 且不是虚拟线程）、
  `TerminalManagerTest.twoTerminalsOfTwoBrandsBothProduceTheirOwnOutput`（两个品牌同时开、
  各收各的字节）。

## v0.6.53 — 2026-11-01 · 小版本：排队中的安装有状态了，三个终端也按版本挑 node

> 起因：ARM 冒烟同时提交三个品牌安装，第三个一直显示 `running`、消息空、进度 -1，被
> 判成"失败"。查下去发现跟 ARM 无关——两个安装槽位写死是 2，第三个在排队，而排队这件事
> 在 API 里根本不存在。

**装的活排队时不再是「running 加一条空消息」**

- 症状：同时提交三个品牌安装，第三个在 `/api/tasks` 里一直 `running`、`message` 为空、
  `fraction` 为 -1，看起来就像卡死；只有 `jcmd` 的线程栈才看得出它停在
  `Semaphore.acquire()`。
- 原因：安装槽位（同时最多 2 个）只有"拿到"和"没拿到"两种表现，没拿到的那段等待既没有
  状态也没有说明。
- 现在排队中的任务报 `queued`，并带一条说明（"等待安装槽位（同时进行 2 个安装，轮到就
  自动开始）"）；拿到槽位时回到 `running` 并清掉那行说明。
- 排队中的任务**可以取消**了：原来的 `cancel` 只看 `running` / `waiting_approval`，停在
  `acquire()` 里的任务根本不看取消标志。
- 顺带把它从「会不会被当成已完成而清理」「会不会被当成"最近一次安装"」两处排除掉
  （`pruneTerminal` / `lastInstall` 现在按"是否终态"判断，不再逐个枚举状态）。

**Kimi / Claude Web / Hermes 终端也跟着按版本挑 node**

- 上一版把「挑工具链按能不能用」落到了 dsh 安装/启动、品牌 web 模式和 Hermes 的运行时；
  三个 TUI 的 node 选择还是 `detect().or(detectManaged)`，系统 Node 20 一样会让 kimi 的
  终端崩在 ESM loader 里。现在统一走 `detectSupported()`。

**验证**：全量 1169 用例 0 失败 / 26 跳过；新增 `TaskServiceQueueTest` 两条（第三个安装是
`queued` 且带原因、排队中可取消且不占槽位）。

## v0.6.52 — 2026-11-01 · 小版本：挑工具链时按「能不能用」，不按「PATH 上有没有」

> 起因：在一台干净的阿里云 ECS 上裸部署，`apt install nodejs` 给的是 Node 20，再
> `npm i -g pnpm` 把 pnpm 补齐——"有 node 有 pnpm"全齐，于是撞出两层同一个坑：面板挑工具链
> 只看 `PATH` 上**有没有**，不看它**能不能用**。

**第一层：要 pnpm 的路径不看 `PATH` 上的 node 有没有 pnpm**

- 症状：`Installing DSH 0.2.1-rc.5 … failed: pnpm was not found on PATH`。而这台机器上，
  面板的托管运行时（自带 pnpm）就装在数据目录里。
- 原因：装版本、改启动库、装插件、装品牌工具都走 `pnpm`，但挑工具链的逻辑是"`PATH` 上有
  node 就用它，没有才看托管运行时"。于是"系统里有 node、没装 pnpm"的机器永远失败在那一句，
  而实例页的「缺少工具链」提示早就写着"有带 pnpm 的托管运行时就不该提示"——前后端对同一件
  事有两套判断。
- 现在这几条路径用 `DshNodeRuntime.detectWithPnpm()`：`PATH` 上的 node 带 pnpm 就用它，
  否则用托管运行时，都没有才报错。报错点名是哪个 node、缺 pnpm、以及两条出路
  （`npm install -g pnpm`，或「设置 → Node.js」装一份带 pnpm 的托管运行时）。
- 启动 dsh 本身仍然只要求 node（`PATH` 优先，其次托管运行时），那一步不需要 pnpm，行为不变。
- 托管运行时的挑选也改成优先选带 pnpm 的那一个；品牌安装里"没有 pnpm 就拿裸名字
  `pnpm` 去跑"的兜底改成明确报错——那是同一个坑的另一半。

**第二层：`PATH` 上的 node 版本太低**

- 症状一：品牌实例启动后立刻死在自己的 ESM loader 里——`node:zlib does not provide an
  export named 'createZstdDecompress'`（kimi-code 2.4.1 需要 Node 22.15+）。症状二：dsh 实例
  一启动就被拒——`The system Node.js 20.19.2 is outside the supported range`。两个症状、
  同一个原因：`PATH` 上有 node 14/20 就算数，面板那份**版本够**的托管运行时没人问。
- 品牌启动现在按"能不能跑这个工具"挑：`PATH` 的 node 不在支持范围内就改用托管运行时，并
  在日志里说明换了哪个、为什么；连托管运行时都没有时至少留一条点名的警告，而不是让它崩在
  loader 里。dsh 的运行时解析同理（`detectSupported()`）。
- dsh 的**安装**现在会先拒绝版本不够的 node，而不是装完几分钟后由启动再拒——那几分钟的等待
  换来的只会是"装好了却起不来"。
- 启动横幅在 node 版本低于 `^22.19.0 || >=24` 时单独加一行说明（分发版仓库的 node 常常落后
  两三个大版本，而这两条报错都不提版本号）。
- 容器不受影响：镜像里 node 是 22.23.1、pnpm 由 corepack 装在 `/opt/node/bin`，两条新判据
  在容器里都成立，走的还是原来那条路。

**验证**：全量 1167 用例 0 失败 / 26 跳过；新增 `DshNodeRuntimeChoiceTest` 六条，其中两条
直接钉住这次的两个坑（Node 20 带 pnpm 要让位给托管运行时、安装前就拒绝太旧的 node）。

## v0.6.51 — 2026-11-01 · 小版本：ZCode 会话不再随容器重建消失

> 起因：这一版本来只是一条 bug——容器换镜像重建之后，ZCode 的会话列表还在，点开却是
> `Session not found`。查下来是面板把实例的 `HOME` 一直指向自己的 `/home/hdsl`，而那是容器的
> 可写层。顺手把这次暴露出来的属主交接重复也收了（见末尾）。

**ZCode 会话不再随容器重建消失（实验品类）**

- 会话库跟着 `homedir()` 走（上游 `adapters/.../session-store/paths.ts`），而实例的 `HOME`
  一直是面板自己的 `/home/hdsl`——容器的可写层，换镜像重建就整层换掉；`/data` 里还留着列出
  这些会话的任务索引，于是列表里有、点开报 `Session not found`。
- 现在每个实例有自己的 `home/`，web 模式与 TUI 都用它：会话落在数据卷上，实例之间也不再有
  同一份 `~/.zcode/config.json` 互相覆盖。`HOME` 之外没有别的开关能挪这个库。
- 新建实例时把面板 `HOME` 下的 `.gitconfig`／`.git-credentials`／`.netrc`／`.npmrc`／`.ssh`
  软链进实例 home，agent 的 `git`/`ssh` 不会因为换了 HOME 突然不能推送。软链而不是拷贝：
  换凭据仍是一次生效，私钥也不会被复制进可能被转手的卷里。
- 旧会话没有迁移：本次之前的会话写在旧位置，改版后不再列出（本部署里那个库当时已是空的）。

**整理：属主交接只写一处**

- 「面板建目录、再把它交给 agent」原来是四处各写一遍（品牌的 `home/`、`workspace/`，Hermes 的
  `panel//locks//run//tools/`，ZCode 的 `home/`，以及 pnpm config 的父目录）。现在统一走
  `Privileges.prepareAgentDirectory`，调用点只保留目录与模式的清单——Hermes 那次
  `unsafe_control_path` 正是"只写了一半"留下的。

## v0.6.50 — 2026-11-01 · 中版本：新增品牌 Hermes Agent（第一个双进程品牌），MC 实例列表并进实例页

> 起因：把 Hermes Agent（Nous Research，MIT）接成第四个品牌。它是这批里第一个**两个进程**的品牌
> （gateway + dashboard），也是第一个**自己认挂载点**的（反代不再改写它的页面）——两条都要求面板
> 在原来为"单进程品牌"长的骨架上多长出一块。

**Hermes Agent（实验）**

- 从 tag 源码装，`uv sync --frozen --no-dev --extra all` 建环境；Python 3.14.9 与 uv 0.13.5 都按
  该版本自己的 `pm/lock.json` 取（不写死，版本一换就跟着换），产物走 Nous 的内容寻址 CDN、
  失败回落 lock 里的 GitHub URL，两边都校验 sha256。前端两个产物（web / TUI）本地构建。
  单版本 570 MB，装一次 2–4 分钟。
- **一个 wrapper 带两个进程**：`gateway run` 先起并等 `/health`，再起 `dashboard --skip-build`；
  面板仍只持有一个进程句柄，谁死了另一个跟着收尾。就绪判据换成轮询 `/api/status`
  （`gateway_running=true`，不看 `overall`——它把各消息平台也算进去）。停止窗口 40 s，
  面板关停时 `stopAll` 用共享 deadline 收全部实例（`docker stop` 只给 PID 1 发信号）。
- **反代按品牌特判**：它读 `X-Forwarded-Prefix` 自己生成全部资源地址，所以面板补前缀、去掉
  `Host`（它只接受回环），并且**不再改写它的页面**——`brandClient=false` 关不掉页面改写，
  再改一次就是 `/i/<id>/i/<id>/assets/…`；它的 bundle 只做一处定点修改（Vite 的资源基址）。
  WebSocket 侧同理丢掉 `Host` 与 `Origin`（上游 WS 自己会查这两项）。
- **API 端点**：`GET /api/brands/hermes/instances/<id>/endpoint` 给出 OpenAI 兼容的 `baseUrl`、
  key 与运行状态；实例卡片的「API 端点」条可一键「添加到账户」（`openai-completions`）。
- **交互 TUI**：实例的终端入口接上它自带的 Ink TUI（用实例自己 `home/tools` 里那份 Node）。
  打开终端会先停掉 web 模式——同一个 HOME 上两套 agent 会抢状态。
- **Windows 不提供**：装不了不是点了才报错——品牌页的安装入口置灰并写出原因（要走 WSL2）。
- **权限**：面板替 agent 写的文件集中成五套模式（共享 0664/0775、私有 0600/0700、可执行 0700），
  从 `PnpmConfigFile` 提到 `Privileges`；实例的 `home/` 与 `workspace/` 在创建与每次启动时
  都交给 agent。

**真机上才暴露的六个坑（都已修，写在这里是因为它们都是"两个工件没对上"的形状）**

1. `toolchain/` 目录没建，uv 阶段 `createTempFile` 直接 `NoSuchFileException`——94 MB 下载完才炸。
2. wrapper 收 5 个位置参数，启动行一个都没传，`set -u` 报 `1: parameter not set`。
3. 实例 `home/` 是 root 的，Hermes 的 gateway 发现拒绝这种 control path
   （`unsafe_control_path: … owned by uid 0, expected uid 1000`），症状是聊天面板一直
   `retrying in 30s`、消息堆在 `queued`。别的品牌不检查属主，看着没事——只因为 `/data` 是
   setgid 组可写，和 v0.6.45 的 pnpm 是同一个形状。
4. 启动重写 `.env`，把操作者在 Hermes「密钥」页加的 provider key 抹掉，聊天随即
   `No usable credentials found`。
5. 前端 `refreshExternal()` 漏了 hermes：实例建出来不进列表，按钮一直转，看着像卡死。
6. 前缀感知品牌被当成普通品牌做脚本补丁，Vite 的懒加载基址错位。

**其它**

- **MC 实例列表并进实例页**（`?view=mc`）：和 dsh 实例、品牌实例同级原地切换，不再跳独立页面；
  顺手修掉一个真实崩溃（`useMemo` 写在早退之后，功能开关一开就是 React #310）。
- **退出登录**从实例页的子侧栏撤掉（主侧栏与「设置 → 服务器信息」各一处）。
- 测试：**1155 用例**。新增 `HermesWrapperSmokeTest`——用假 release 真的执行 `/bin/sh wrapper`，
  断言两个子进程、pid 文件、TERM 后全部收干净。这是第一次有测试跨过 Java ↔ shell 这条边界。

## v0.6.45 — 2026-10-31 · 修复：MC 功能开启按钮的三条死路；ZCode 的 owner 补丁改成全平台

> 起因：0.6.44 上线后真机反馈——MC 的「开启」按钮按下去像摆设，关掉功能重启后再也开不起来；
> Linux 上的 ZCode 列不出目录、建不了项目。

- **MC 功能开启按钮像摆设**（三处都是真的坏）：
  - 拉不到 Mojang 时向导里**一个能点的都没有**——「我已知晓风险，暂不启用」被写在
    `eula` 存在才渲染的分支里。现在这个出口在任何状态下都在，报错旁边加「重试」。
  - **EULA 成功抓取过的那一份现在落盘**（`<data>/minecraft/eula.json`）。国内网络连不上
    `www.minecraft.net` 时，回退到这份副本、连拉取时间一起照原样展示，于是「关掉功能 →
    重启 → 想再开」不会卡死在"协议拉不到，同意不了"。同意记录里存的仍是这份文本的哈希，
    所以不会把没看过的内容算成看过；副本也没有时才 503（看不到协议就不能同意，这条没变）。
  - 条款短到不需要滚动时，`onScroll` 永远不触发，两个勾选框永远是灰的——按高度量一次，
    装得下就算读过了；另外「开启」失败不再只靠一闪而过的 toast，错误也写在按钮旁边。
- **Linux 的 ZCode 建不了项目文件夹**：项目库 owner 的补丁原先只在 Windows 上打，理由是
  「Linux 还可能有 ZCode 桌面程序当第二客户端」。代价是不用桌面端的人在 Linux 上一样什么都
  打不开（目录列不出、项目建不了、会话起不来）。现在默认**全平台**打；确实拿桌面程序当 host
  的部署设 `HDSL_ZCODE_KEEP_REMOTE=1` 退回上游 owner。
  **注意**：已经装好的发行包是旧字节，升级面板后要在版本卡片里**重建一次 ZCode 发行包**。

## v0.6.44 — 2026-10-31 · 小版本：容器里面板 root 持有秘密、agent 跑成 uid 1000

> 起因：同 uid 的容器里，agent 能改面板的 `auth/`、`certs/`、`server.yaml` 和审计日志——
> 0600 保护的是"别的账户"，而 agent 就是同一个账户。要求"agent 改不动口令文件"就必须分成
> 两个身份，单 uid 下没有别的办法。

- **容器里两段身份**：面板是 PID 1、uid 0；每个 agent 进程派生时前置
  `setpriv --reuid=1000 --regid=1000 --init-groups --`（`util/platform/Privileges.java`），
  跑成镜像里的 `hdsl`。入口把 `auth/`、`certs/`、`logs/`、`server.yaml` 归 root 0700/0600；
  实例子目录 `1000:1000 2775`（setgid）且入口 `umask 002`，所以面板写进去的文件仍是
  `root:hdsl 0664`，agent 照常能改。`setpriv` 是 exec 原地替换，进程组与 PTY 会话不变。
- **裸部署不变**：不在容器里就不降权（宿主的 uid 1000 可能是别人，塞过去是权限 bug）；
  面板与 agent 同 uid。以 root 启动不拦，但启动横幅写明"你派生的每个 agent 也是 root"。
  systemd 模板补了 `ProtectSystem=strict`、`NoNewPrivileges=yes` 等六条硬化。
- `docker-install.sh` 不再把整个数据卷 chown 给 1000——那样每次启动都会把秘密交还回去；
  属主改由容器入口校正（只在属主不对时递归，避免每次开机遍历大卷）。
- 新增 `scripts/verify-privilege-split.sh`：在容器里以 uid 1000 跑，断言 `auth/`、`logs/`
  读写被拒，而实例目录与 pnpm store 照常可写。
- **文档写清这一层盖不住什么**（`design-notes §7.5.1`）：供应商 API key 在
  `launcher-settings.json` 里，agent 读得到（ACP 要用它调模型），这是有意保留的暴露——
  缓解在部署侧（一台机器一套 key、能撤销能轮换），不在面板里。同类的还有"实例之间互相可写"
  与"面板的 HTTP 面没变"。

### 真机验收暴露的问题，本版一并修掉

> 起因：宿主在 QNAP NAS 上跑真机验收，装实例这一步就过不去。这些问题都是两段身份带来的，
> 也就都在这一版里一起修，不存在"先发一个有问题的版本再补"。

- **默认工作区变成 `/root/workspace`**：面板是 root 后 JVM 的 `user.home` 是 `/root`，而
  agent 是 uid 1000，连 `cd /root` 都进不去（0700）——终端起来 44 ms 就 exit 1。
  新增 `util/platform/UserHome`：**`HOME` 优先于 `user.home`**（systemd 与容器都会设 HOME；
  `user.home` 取的是 passwd 里的家目录，换用户跑就是错的）。workspace、pnpm 全局配置目录、
  整合包安装的默认工作区三处都改用它的同一套规则。
- **agent 读不到 pnpm 的配置（最致命的一条）**：`~/.config/pnpm/config.yaml` 仍按老办法
  「临时文件 + 0600 + 原子 rename」写，面板是 root 了，写出来就是 `root:root 0600`。`pnpm`
  是 agent 的子进程，连读都读不了——所有实例的安装与插件安装一律 `EACCES: permission
  denied, open '/home/hdsl/.config/pnpm/config.yaml'`。现在落盘后连属主带 mode 一起交给
  agent uid（`Privileges.handToAgent`），父目录同样（`pnpm config set` 要重写它）；启动时
  也校正一次，写坏的那份重启就好，不用改设置、不用重建卷。**同一处堵了符号链接**：这个目录
  归 agent，agent 能放一个 `config.yaml.tmp-…` 指向 `/data/server.yaml`，面板顺着写就是拿
  root 的权限替 agent 写文件；临时文件改为随机后缀 + `CREATE_NEW`，目标若是链接则删除不跟随。
- **首启 `logs/` 是组可写**：入口只修"已经存在"的路径，而 `auth/`、`logs/` 是面板启动后才建的，
  继承了 volume 的 setgid + umask 002（`root:hdsl 2775`），要下次重启才纠正。入口改为先
  `mkdir -p` 四个秘密路径再改属主。
- **agent uid 可在构建期改**：`--build-arg HDSL_AGENT_UID=1001`。有些存储（QNAP 的 trimacl）
  把共享属主当管理员放行，mode 位形同虚设——那里唯一的镜像内办法是换一个不是属主的号。
  镜像里的 `hdsl` 用户、入口的 chown、`Privileges` 的 `setpriv` 读同一个值。
- **入口自检"隔离是否真的成立"**：以 agent 身份试写一次 `auth/`，成功就打一条 WARNING
  （这台机器的文件系统不认 mode 位）。
- **文档写死这件事程序管不了**：`security.md §2.8` 单列一节，讲清 ACL 管理的共享
  （QNAP trimacl 一类）为什么让 `0700` 形同虚设、为什么 `chmod`/`stat` 会被骗、三条出路
  （换存储 / 换 uid 重建 / **接受，并在安全评估里如实说没有隔离**）。

## v0.6.42 — 2026-10-31 · 修复：v0.6.41 的镜像构建失败（Dockerfile 的 ARG 作用域）

> v0.6.41 的 jar 与客户端都正常，但 docker 流水线挂了：
> `failed to solve: base name (${HDSL_BASE}) should not be blank`。

- **`ARG HDSL_BASE` 挪到文件顶部**（首个 `FROM` 之前）。Docker 只认首个 `FROM` 之前声明的
  ARG，写在第三个 `FROM` 前面它根本不在作用域里，`FROM ${HDSL_BASE}` 拿到的是空串。注释
  里写清了这条，免得下次再挪回去。
- **ffmpeg 的编码器断言移回装包之后**：它被上一条改动一起挪到了 `FROM` 之后、`apt-get`
  之前，那个位置 `ffmpeg` 还不存在——即使第一处修好，也会在这里失败。
- 其余内容与 v0.6.41 相同。

## v0.6.41 — 2026-10-31 · 小版本：Node 装完自检、TUI 插件上界、启动窗口不再假 STOPPED；运行时底座可换（Fedora / openSUSE）

> 起因：底座兼容性实测（`report-2026-10-31-base-compat.md`）。在只有 JRE 的
> `debian:trixie-slim` 上点一键安装，面板默认挑最新稳定版 **Node 26.13.0**，而它的官方
> 二进制链接 `libatomic.so.1`——那个镜像没有。结果是任务显示 done、`/api/node` 显示
> `suitable: true`，但 node 一执行就 `error while loading shared libraries`，随后 pnpm
> 安装 `exit 127`，日志里只有一行 warning。

- **装完真跑一次**：`NodeRuntimeManager.install` 解包后执行 `node --version`（10 秒上限），
  跑不起来就删掉那份运行时、任务 failed，错误里带 loader 的原话与"基础镜像可能缺 libatomic1"。
  同一套检查也用在新加的「采用已有安装」路径上——拷贝坏了的树不该等到第一次安装才暴露。
- **`suitable` 改为"能跑"**：原来只看版本号在不在范围内；现在还要二进制真能回答。结果按
  目录 + 二进制 mtime 缓存，设置页轮询不会每次都起进程。
- **doctor 认托管运行时**：与启动路径一致走 `detect().or(detectManaged)`，并标
  `[panel-managed runtime]`。以前裸部署装好托管 Node、实例跑得好好的，doctor 仍然报红。
- **生产镜像补 `libatomic1`**：0.1 MB，换掉"一键装最新版装出个不能用的 node"。
- **TUI 插件不再对最新的 dsh 撒谎**：`dsh-inproc-tui` 的上界原写作 `<0.2.2-0`，那会把
  `0.2.1-rc.4` / `-rc.5` 也算成适配——而它们正是 `dsh@latest` 发布的版本；安装会被 dsh 以
  "incompatible with dsh 0.2.1-rc.5" 拒绝并回滚，用户拿到的现象是"最新版没有终端可用"。
  上界收窄到插件 peer 里真正声明的最后一个（`<0.2.1-rc.4`），不匹配时走既有的面板侧说明
  （含 ACP 备选），而不是把 dsh 的原话抛出来。
- **启动窗口不再报假 STOPPED**：`launchAsync` 在任务排队前就标记"launch 在途"，`stateOf`
  在进程出现之前回答 STARTING。`POST /launch` 的 202 与紧接着的 `GET` 之间那一瞬，以前会
  读到一个实例早已离开的状态，前端轮询会闪一下"已停止"，自动化脚本会误判。
- **换运行时底座**：Dockerfile 的 runtime 阶段按 `/etc/os-release` 分派（Debian / Fedora /
  openSUSE），**只有一个 Dockerfile**；`tini` 统一软链到 `/usr/local/bin`（openSUSE 在
  `/usr/sbin`），架构判断改用 `uname -m`，ffmpeg 的 x264/x265 断言由 `HDSL_ENCODERS` 控制
  （Debian 必须、其它底座只告警，因为完整编解码器在 RPM Fusion / Packman）。
  `docker-install.sh` 随之进仓库并新增 `--base debian|fedora|opensuse`，原地构建、镜像标签带底座。
- 文档：`docs/deployment.md` 的裸部署一节写明精简基础镜像要补 `libatomic1`；新增 §4.1
  说明三个底座的差异（tini 路径、ffmpeg 编码器、Fedora 44 没有 JDK 21）。

## v0.6.40 — 2026-10-31 · 中版本：Windows 实例终端改浏览器 PTY（ConPTY）、出站 TLS 信任策略、一批 Windows 冒烟修复

### Claude Code 要的 Git Bash 由面板指明

> 起因：Windows 上开 Claude Code 的终端直接结束，`会话已结束 (1)`，唯一线索是它自己的
> 那句 "Claude Code on Windows requires git-bash … set `CLAUDE_CODE_GIT_BASH_PATH`"。
> 机器上装着 Git for Windows，只是它不会自己去找——这跟当年 kimi-code 要
> `KIMI_SHELL_PATH` 是同一件事，当时只给了 kimi 一家。

- **`GitBash.applyTo(environment, variable)`**（新）：把找到的 `bash.exe` 写进子进程环境，
  规则只有一条——操作者自己指过的（面板环境或实例环境里的同名变量）一律不动；这台机器
  没有 Git for Windows 就什么都不写（写个错的路径比不写更糟）。
- **kimi 与 Claude Code 都走它**：`KimiTui.applyShell` 改为委托（行为不变），
  `ClaudeWebTui.applyShell` 写 `CLAUDE_CODE_GIT_BASH_PATH`。终端与网页两处都设：
  网页模式里真正要 bash 的是 claude-web 服务自己拉起的 CLI 子进程，所以
  `BrandRuntime` 的 claude-web 启动计划里也带上了这条。
- 实测：`%ProgramFiles%\Git\bin\bash.exe` 被优先命中（这台机器 `%ProgramFiles%` 在 G: 盘，
  所以不能靠 `C:\Program Files` 写死），`System32` 与 `WindowsApps` 里那两个 WSL shim 仍然拒绝。
- 回归覆盖：`ClaudeWebTuiTest` 新增「面板会指明 Git Bash」与「操作者指过的不覆盖」两条。

### 删除实例先拿走名字，再删内容

> 起因：Windows 上删实例删到一半失败——`<id>/dsh` 已经被删空，`instance.json` 却还在，
> 于是列表里那个实例点开就错。根因是老顺序「先递归删，失败了才改名」：只要中间有一个文件
> 被别的进程握着，删掉的就已经是安装，留下的却是那个描述它的清单。

- **`FileUtils.deleteByTombstone`**（新）：先 `Files.move` 到 `<id>.deleting-<millis>`，再递归删。
  列表按目录名找实例，所以改名一成功实例就不在列表里了；**改名失败才抛**（此时内容一点没动，
  说"删不掉"不是撒谎）；删不掉的残留返回给调用方记日志，由启动清理再删一次。
- **dsh / 品牌 / ZCode 三处 `delete` 都改走它**，并把「先杀掉还引用该目录的进程」
  （`ProcessTree.killReferencing`）补到品牌与 ZCode——Windows 上进程把实例目录当 cwd 时，
  连改名都会失败（实测），这一步以前只有 dsh 有。
- **tombstone 清理扩到品牌与 ZCode**：两个 manager 各加 `sweepTombstones()`，在启动路径
  （`BrandApiServlet` / `ZcodeApiServlet` 构造时）各跑一次，与 dsh 在 `Main` 里那次同性质。
  没有新起定时任务。
- 回归覆盖：`FileUtilsTombstoneTest`（正常删干净 / 目录不存在 / **内容删不掉时留 tombstone 而不是
  半个实例** / 连改名都做不到时一点没动）与 `DshInstanceDeleteTest`（`find(id)` 为 null、
  `instance.json` 与目录名都不在、`delete` 不抛、启动清理能收尾；以及「删失败时安装与清单都还在」
  ——这条在旧代码上是红的：改名被拒时 `dsh/` 已经被删空，只剩 `instance.json`）。
  实测补充：Windows 上**活着的文件句柄连目录改名也一起挡住**，所以真正的残留来自「删除被拒、改名还行」
  （只读文件、杀软/索引器的瞬时占用这类），而不是「文件开着」本身。

### OpenCode 1.19.5 用 chunked 发资源，bundle 补丁静默失效

> 起因：挂 `/i/<id>/` 的 OpenCode 面板里 markdown 高亮报
> `Error: Markdown highlighting worker failed`——bundle 里 `new Worker("/assets/…")` 的根绝对路径
> 没被改写，worker 去面板根取了东西。

- **实例不给 `content-length` 也要补 bundle**（`InstanceProxyServlet`）：补丁原先要求"实例先把长度
  说出来"，而 OpenCode 1.19.5 的资源响应是 `Transfer-Encoding: chunked`、没有 `content-length`，
  于是整份 bundle 原样转发。现在长度只用于**提前放弃**（已知超过 16 MB 才不读），未知长度照补；
  `probeRouterBundle` 同样去掉长度前提（否则这类客户端的整页路由会被判成"没有路由读法"、退回地址
  还给面板根）。真超过上限时**整份原样发出**：不打补丁，也**不截断**——以前那条路会静默只发前缀。
- 回归覆盖是端点级的（`BrandAssetChunkedPatchTest`：桩 upstream 手写响应头，`chunked` 且不带
  `content-length`，断言浏览器收到的 body 里是 `<挂载点>assets/worker-x.js` 且带
  `Cache-Control: no-store`），外加超上限时不丢字节（含 gzip 解码后的尾巴）。

### GitHub 在面板里"搜不到 / 下不下来"；覆盖 jar 后自带插件读失败

> 起因：Windows 上装 claude-web / octop / ZCode 全部失败——品牌版本列表是空的，
> ZCode 源码"could not be downloaded"，而浏览器打开同一批地址一切正常。

- **证书不受信任时说清原因，并且要人确认一次**（`OutboundTls` + `/api/network/trust`）：
  浏览器能打开的站点面板报 `PKIX path building failed`，原因是中间人代理（杀毒、企业网关、
  FastGithub 这类加速器）把自己的根证书装在 **Windows 证书库**里，而 Java 只认自带的
  `cacerts`。以前的表现是"版本列表突然空了，没有任何原因"。现在：
  - 品牌版本列表因为证书失败而空时，接口回 409 并把人话写进去（谁签的、为什么会这样、
    两条出路）；ZCode 源码下载失败同样附上这段；
  - **不自动放宽**：要操作者在「设置 → 下载源 → 出站证书」（或版本卡错误块里的"我确认"按钮）逐字回传
    `我确认这是我自己的网络代理`，才开一个**一次性、最长 24 小时**的窗口（写进
    `server.yaml` 的 `network.accept_unverified_until`，重启仍在期内生效，到期自动收紧，
    随时可撤销），并记审计；
  - 即便在窗口里也不是直接跳过：先按 Java 的 cacerts、再按机器自己的根证书库（代理的根
    通常在那儿）校验，两张都不认才接受，并把证书主题/签发者写进日志。
  - 实测：接口闭合/授权/撤销/重启保持全部按预期；本机当时能复现（`openssl` 看到
    `CN=FastGithub`），写这段时那台机器上代理已停止拦截（证书变回 Sectigo），所以 409
    那条只用单测覆盖，没能现场再触发一次。
- **覆盖运行中的 jar 时把话说清**：Windows 上覆盖一个正在运行的 jar 是能成功的，JVM 继续
  握着旧句柄，直到下一次读资源——`ZipException: invalid LOC header (bad signature)`，
  现象是"文件明明在（`unzip -t` 通过）却读不出来"：自带插件的 tgz 装不上、页面加载不出
  资源。现在启动时记下自己那个 jar 的大小与时间，读失败且文件已变时直接说"重启面板即可"。

### Windows 冒烟测试：kimi 找不到 bash；进程内 TUI 插件对 dsh 0.2.2 的口径

> 起因：在裸 Windows 面板上验终端时撞到两条。一条是面板该做而没做的（把 Git Bash 告诉
> kimi-code），一条是面板说了不该说的话（宣称支持一个插件自己都不接受的 dsh 版本）。

- **kimi-code 在 Windows 上不会自己找 bash**：第一次要跑命令就
  `failed to start shell: Git Bash was not found on this Windows host`，哪怕 Git for Windows
  装得好好的。面板现在找到它（`ProgramFiles` / `ProgramFiles(x86)` / `LOCALAPPDATA\Programs`
  下的 `Git\bin\bash.exe`，其次 `PATH`），以 `KIMI_SHELL_PATH` 交给实例——终端与网页模式都设；
  操作者自己设过就不覆盖。**不采用** `System32\bash.exe` 与 `WindowsApps\bash.exe`：那是 WSL
  启动器，交给 agent 等于让它跑到另一个系统里执行。
- **进程内 TUI 插件的 `requiresDsh` 少了上界**：面板只写了 `>=0.2.0-rc.2`，于是一个 dsh
  `0.2.2-alpha.2` 的实例被判为"适配"，装完之后 dsh 自己拒绝加载（`incompatible with dsh
  0.2.2-alpha.2`，插件的 `peerDependencies` 只到 `0.2.1-rc.3`）。现在按插件的真实范围写成
  `>=0.2.0-rc.2 <0.2.2-0`——`-0` 是 semver 的"含预发布"写法，裸 `<0.2.2` 会按数值比较把
  `0.2.2-alpha.2` 也放进来。不适配时面板直说，并推荐 ACP 版终端（它跨两条线都能跑）。

### dsh 半安装的树会被当成已安装；ZCode 的工作区不能选

> 起因：新建会话失败，错误是一长串 `agent-preset/invalid … waiting for workingDirectory`。
> 那串话没有一句提到真正的原因：实例里那份 dsh 的 `node_modules` 是**半成品**。

- **半安装检测**（`DshVersionManager.emptyPackages`）：pnpm 的虚拟仓库被中断时，包目录会
  留下空壳——目录在、`package.json` 不在。树看起来完全正常（`bin.js` 也在），直到 harness
  启动、某个插件 import 不到依赖，于是依赖 `workingDirectory` 服务的一整排插件全部
  "waiting for"，报错里全是下游症状。现在两处都查：安装成功后校验整棵仓库（有空壳就回滚并
  说明），启动前再查一次（报"安装不完整，请重装这个版本"），不再让用户对着
  `waiting for workingDirectory` 猜。
  实测：这台机器上那个 `dsh-0.2.1-rc.1` 实例 **132 个包目录是空的**（2930 个正常）。
- **ZCode 选工作区**：创建实例时可以填一个绝对路径，实例就在那个项目里干活（留空仍是
  `<实例>/workspace`）。路径写进实例记录，启动与终端读同一份；相对路径直接拒绝（它取决于
  面板自己的工作目录，不是操作者该知道的东西）。建好之后只读——换工作区等于换一个项目。

### 启动 ZCode 实例要求系统自己装 node

> 起因：裸 Windows 部署上点「启动」ZCode 实例，`Failed to start the ZCode process:
> Cannot run program "node" … CreateProcess error=2`。面板自己在
> `<data>/hdsl/runtimes` 装了一个 Node，但它启动实例时写的是裸 `node`——只有 PATH 上
> 有的机器才成立，而那正是"面板帮你装好、系统里没有"的反面。

- ZCode 实例与 ZCode 终端统一走 `ZcodeTui.resolveNode`：`zcode.nodePath` →
  PATH 上的 `node` → **面板托管运行时**；一个都没有时给 409 + 可行动的说明（去
  「设置 → Node.js」一键安装），而不是把 `CreateProcess error=2` 原样扔出来。
  启动时把 node 所在目录放进子进程 PATH（ZCode 自己的工具也会 spawn node）。
- 同一类问题顺手堵在品牌启动上：kimi / claude-web 这类 JS 工具由 `.cmd` shim 启动，
  shim 从 PATH 找 `node`。现在**只在 PATH 上没有 node 时**把面板托管的运行目录放到最前，
  并写一条日志（octop 是 Python、opencode 是原生二进制，不受影响）。

### ZCode 源码解包在中文文件名上整包失败

> 起因：Windows 上点「构建 ZCode」，在"解包源码"这一步失败：
> `docs/ZCode 项目架构分析与改进建议.md: Invalid empty pathname` 加
> `tar.exe: Error exit delayed from previous errors`——树其实已经被写了大半。

- 根因是**文件名编码**，不是权限或磁盘：ZCode 仓库里有一个中文名文档，GitHub 的 tarball 用
  **UTF-8** 存条目名，而 Windows 自带的 `tar.exe`（libarchive）按 **ANSI 代码页**（这台机器是
  GBK）转换，转不出来的条目它就当成"没有名字"，于是整包以
  `Invalid empty pathname` 结束。
- 修法：面板解包源码不再调用系统 tar，改用自带的 kala-compress 读取器（`TarArchiveInputStream`，
  显式 `UTF-8`），流式解包（90 MB 压缩、解出几百 MB，不落临时文件），保留
  `--strip-components=1` 的语义、拒绝爬出目标目录的名字、符号链接失败只记日志不中断。
  安装构建产物那一步用的是同一个函数。
- 实测：把 `F:\dsh\data\zcodeuild\source.tar.gz`（91 MB、18341 个条目）整个解一遍，
  14 秒完成，`docs/ZCode 项目架构分析与改进建议.md`（19552 字节）名字正常。
- 构建脚本自己的裸 `tar` 仍走 `bin/tar.bat` → `System32	ar.exe`，那条没变（脚本里的调用
  只碰 ASCII 文件名）。

### 没有系统 Node.js 的机器上实例页白屏

> 起因：裸部署（`java -jar`，机器上没有 node）打开面板，实例页直接报
> `Cannot read properties of undefined (reading 'pnpm')`，整页不渲染。带 node 的开发机和 CI
> 都看不出来——只有"这台机器没有 Node.js"这一种状态才会撞上。

- `GET /api/node` 的 `system` 字段在"没有系统 Node.js"时是 `null`，但 Gson 的默认写入器**丢掉**
  `null` 成员，响应里干脆没有这个键。实例页的工具链提示条读 `status.system.pnpm`（先判
  `system !== null`），丢键之后拿到的是 `undefined`：`system !== null` 为真，`undefined.pnpm`
  把整页炸掉。
- 修法：该接口改用 `Json.writePreservingNulls`——账户、实例、目录等"字段允许为 null"的接口本来
  就走这个写入器，这里是漏掉的一个。
- 前端同时加固：`system`/`managed` 缺键按 `null` / 空数组处理，读不到也不再炸（这条要等下次
  用 npm 重建 SPA 才生效；后端那一条对已有的 `web/dist` 立刻有效）。
- 回归测试 `NodeApiTest.theSystemFieldIsPresentEvenWhenThereIsNoNode`：改回 `Json.write` 即红。

### Windows 面板的实例终端改为浏览器 PTY（ConPTY）；删除桌面控制台回落

> 起因：Windows 上点「终端」开的不是 PTY，而是一个宿主机桌面窗口（`cmd.exe /c start …
> cmd.exe /k <脚本>`）。它的四个后果都不是能修的细节：脚本里的 `/k` 让 TUI 退出后用户站在
> cmd 提示符上；远程用户根本看不到这个窗口；面板装成服务（session 0）时没有桌面，终端直接
> 409；E-Stop 也杀不掉它。

- 新增 `web/terminal/pty/ConPty.java`：`CreatePseudoConsole` + `CreateProcessW`，系统自带，
  不随包发任何二进制、不加依赖。门槛是 **Win10 1809（build 17763）**，同时探测
  `kernel32!CreatePseudoConsole` 是否真的在——版本号是给人看的原因，符号是事实。
  低于门槛的机器终端接口 409 `terminal_unsupported_platform`，文案带实际 build 号，
  **不回落任何桌面窗口**；Win7/8.1 明确不支持（不引 winpty）。
- 启动是 **argv-only**：`lpApplicationName` 单独给程序，命令行按 MSVCRT 规则拼；
  `.cmd`/`.bat`/`.ps1`/`.com` 一律拒绝，因为 `CreateProcess` 会悄悄把它们交给 `cmd.exe`，
  等于在链路里塞回一个 shell；`argv[0]` 还必须是存在的绝对路径。
- 进程树归 Job Object（`JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`），`CREATE_SUSPENDED` → 入 Job →
  `ResumeThread`：终端结束时整棵树一起走，面板进程被强杀时也一样。
- 删除 `DesktopTerminal`（以及它的 `.cmd` 脚本、`{"mode":"desktop"}` 响应、`open-desktop-terminal`
  确认短语和它的测试）：能重新打开 `cmd /k` 的开关就是这次要消掉的那条攻击面，所以不留配置。
  前端与 Go 客户端的 `desktop` 分支保留，只对旧面板有意义。
- 启动横幅与 `-doctor` 改成报告能力：`terminal : ConPTY (Windows 10.0 build 19045)`，或者
  `unsupported: needs build 17763, this is <n>`。
- 平台语义差异（写进 `docs/web-shell.md` §7.1）：Windows 没有能打到伪控制台子进程的礼貌信号，
  `terminate` 是「宽限后 `TerminateJobObject`」，不等价于 SIGTERM；一个终端 = 一个 `conhost.exe`；
  只承诺 64 位 JVM。
- Linux/macOS 一个字节都没改；上位 `TerminalSession` / `TerminalManager` / WebSocket / 前端 / Go 客户端
  一行都没改。

## v0.6.36 — 2026-10-31 · 容器内置 ffmpeg；MC 复用旧存档；eula 兼容

> 起因三件：实例里的 agent 要截取视频帧，发现容器没有 ffmpeg，正准备从 npm 拉
> `ffmpeg-static`——那是每个实例各下一份、版本不一、还绕过了镜像的可复现性；
> 以及“把老服搬进来”这件事需要一个明确边界：搬存档可以，搬代码不行。

- 运行时镜像装 `ffmpeg` + `libavcodec-extra`（Debian 把受专利影响的编码器拆在
  这个变体里：Xvid、AMR/AMR-WB、JPEG XL、codec2、GSM、Speex、twolame 等）。
  基础包本身已带 x264 / x265 / SVT-AV1 / rav1e / aom / VP8/VP9 / WebP / Opus / MP3。
- 构建期断言 `ffmpeg`、`ffprobe` 与 `libx264`/`libx265` 编码器都在，打包回退
  当场失败，而不是等 agent 截帧时才发现。
- 边界：Debian 不发 non-free 的 `fdk-aac` 与 `NVENC`（镜像里没有 CUDA）。需要
  这些的项目应自带静态构建，不要指望镜像补齐。
- **`eula.txt` 解析**：MC 自己写的文件带注释头，以前只有“整文件等于 `eula=true`”才算接受，
  于是任何被 MC 启动过一次的服都会在面板里被判为“未接受”。现在逐行解析、跳过注释；
  同一个键出现多次时**最后一个值生效**（和 Java Properties 一致，也就是 MC 自己的读法）。
- **复用现有存档**（P1）：创建向导第一步可以直接选旧存档 zip——创建 → 导入存档 →
  装服务端 → 完成，不用先建再跳页；实例页「世界」仍可随时导出/导入。导入前自动备份、
  整体替换；顶层目录名和实例的世界目录名不一致会被拒绝（以前会静默套成
  `world/oldworld/…`，服务器当新世界、存档等于没搬进来）。
- **玩家名单导入**（P1）：停服时在「玩家」页导入一个 zip，`whitelist.json`、`ops.json`、
  `banned-players.json`、`banned-ips.json` 放在 zip 根上；只认这四个名字，其它条目
  （包括 jar）一律忽略，先校验是 JSON 数组再原子替换。
- **导入自带 `server.jar`（P2）只留骨架**：`McServerImport` 整份注释、`ENABLED = false`，
  记录威胁模型（同 uid、读得到 `/data` 的口令哈希与证书私钥）、确认流程、SHA-1/SHA-256
  的验证边界与“未验证”标记的启用条件。见 `docs/minecraft.md §7.4`。
- `docker-compose.yml`：MC 端口不再默认发布（25565 改成注释示例，写明容器端口必须等于
  该实例的 `server-port`）；`HDSL_MC_DIR` 可切宿主目录，并提示 `chown 1000:1000`。
  `.env` 进 `.gitignore` / `.dockerignore`。

## v0.6.35 — 2026-10-30 · 部署向导、裸部署工具链提示、MC 服务端正式可用

> 起因三件：容器部署要手抄一长串 `docker run`，且 NAS 上常常没有 compose；
> 只放一个 jar 的空机器（JRE 之外什么都没有）能启动面板，但创建实例只会得到
> 「Node.js was not found」，而一键安装的入口藏在设置深处；以及 Minecraft 服务端
> 从实验形态收尾——创建向导、端口绑定、存档搬出 `/data`。

- **`docker-install.sh`（仓库根，非交付物）**：问数据目录 / 工作区 / 端口 / 监听地址 /
  口令 / 镜像源，原地 `docker build` + `docker run`，不依赖 compose。同名容器先问
  「重启 / 重建容器（不重编）/ 重新构建镜像」；端口占用会放行「占着它的是自己的容器」。
  宿主数据目录用镜像自身跑 `chown 1000:1000`，不需要宿主 sudo。
  没有任何 docker 时按发行版给装法（`--install-docker` 可代装），`--dry-run` 只打印计划。
  本地构建的镜像名是 `<最后标签>-build-<系统版本>-<时间戳>`。
- **向导在真实 Docker 上测出的三个 bug 已修**：① `docker inspect` 找不到容器时会先往
  stdout 写一个空行，`|| printf missing` 因此得到 `"\nmissing"`，每次全新安装都被当成
  「容器已存在」而直接退出；② `--restart` 把按时间戳算出的、并不存在的镜像名写进状态文件，
  下一次 `--no-build` 会拿它去重建——现在从容器读实际镜像；③ `--dry-run` 会写口令文件，
  和「只打印计划」的承诺矛盾（口令曾经落盘）。同类空行问题在健康检查与 `--recreate` 的
  镜像回退里一并修掉。
- 顺带：`--release` 记进状态文件（`--rebuild` 不会静默掉回 0.1.0-dev，`--no-release` 显式关）；
  `--recreate` 在容器已被删除时复用状态里记录的镜像而不是重编；镜像里 `/opt/corepack-seed`
  会在 `/data` 被宿主空目录遮蔽时把 pnpm 播种回去，首次启动不再把「下载 pnpm」印成版本号。
- **实例页的「缺少 Node.js 工具链」提示条**：只在真的缺（没 node、没 pnpm、或系统 node
  不在 `^22.19 || >=24`）时出现，可直接一键安装（国内镜像 / 官方源），装完自动消失。
  创建实例失败时那句「Open Settings → Node.js」不再需要用户自己找路。
- **一键安装不再装预发布版**：`nodejs.org/dist` 把 `27.0.0-alpha.*` 排在所有稳定版前面，
  原来「最新可用版本」会直接装 alpha——实测装出来的是 `27.0.0-alpha.0.0.0`。现在自动挑选
  跳过带 `-alpha` / `-rc` 的条目；要预发布仍可显式传版本号。
- 文档：README「方式二」写清 JRE 之外还需要什么；`docs/deployment.md` 新增 §10.1
  （只有 jar 的机器上 Node 与 pnpm 的三条来源、启动日志里那一行怎么读）。

### MC 服务器

- **创建改成向导**：基本信息（名称、游戏端口、RCON 端口、内存、世界名、种子）→ 版本 →
  存储位置 → 下载服务端 → 完成。最后一步的按钮就是「立即启动」，不用再进实例页点一次安装。
  下载走任务，关掉对话框也会继续。
- **每个实例都能改端口**：实例设置里新增「端口与内存」卡片，停服时可改游戏端口、RCON 端口与堆；
  改端口同时写 `instance.json` 与 `server.properties`（只写一处＝面板说 25566、游戏还在 25565 上听）。
  占用检查会忽略实例自己，避免「保存自己等于和自己冲突」。
- **服务器搬出 `/data`**：默认根是 `$HOME/mc`（容器里 `/home/hdsl/mc`），世界存档、`server.jar`、
  日志都在那儿；`server.yaml` 的 `mc.dir` 或 `HDSL_MC_DIR` 可改。旧位置
  `<data>/minecraft/instances` 仍被扫描（升级不丢服），也可在向导里作为存储选项。
- **容器侧默认持久化**：镜像建好 `/home/hdsl/mc` 并归 `hdsl`；compose 默认挂 `hdsl-mc` 卷，
  `docker-install.sh` 问一次后挂上（`--mc /srv/mc` 换宿主目录，`--mc none` 明确不挂）。
  实例页与向导都显示服务器目录的绝对路径——「存档去哪了」由界面回答。
- 测试：管理器补「两个根互不遮挡 / id 跨根唯一 / 端口排除自身 / 改端口两处都写」，
  接口补 `/api/mc/storage` 与 `/api/mc/ports`。

## v0.6.34 — 2026-10-30 · 修复：读不到模型列表时，启动失败要说清原因

> 起因：一条自建供应商的路由，供应商的 `/models` 一失败，启动就中止，而界面只留下
> 「启动失败」；账户上没填模型名时更是连兜底都没有。

- **拒绝里带上供应商的原话**：`HTTP 401`、响应正文摘要（key 已抹掉）、超时，并直接指路
  到「账户 → 模型」那一栏。以前只有一句 “Could not read the models of …”。
- **降级不再静默**：列表读不到但账户上存过模型名时，照旧用那个名字启动，同时在面板日志里
  写明“为什么路由只有一条模型”。
- **验证接口带回原因**：`GET /api/accounts/{name}/verify` 在“通过但一个模型都列不出来”时
  返回 `note`；账户页显示「读不到模型列表（HTTP 401: invalid_api_key）；启动前请在账户里填一个模型名」。
- **实例指定的账户已被删掉**：启动照旧（不带 key 也比带错 key 好），但面板日志会写明；
  实例详情页的账户选择器下面直接提示「这个账户已经不在」，并说清后果与修法。
- 工程：`jdk.httpclient.allowRestrictedHeaders=host` 提到测试 JVM 启动参数。面板自己的
  服务端测试要求这个属性在任何 `HttpClient` 之前设好；只要有一个测试类先建过客户端，
  `AccountApiTest` 就会集体红——这次就踩到了。

## v0.6.33 — 2026-10-30 · 关于页、日志保留与正文上限（实验小版本）

> 一批「把面板自己讲清楚」的改动：关于页补齐、日志有了保留策略、请求体上限按路径判定。
> 日常功能行为不变；配置不写就用默认值。

### 关于页

- 版本、构建号（版本串带 `+g<sha>` 时单列）、**Java 大版本**、版权、无担保、第三方组件、
  数据与隐私、反馈入口；新增「合规与声明」两张卡（数据与隐私 / 可接受使用）。
- 同意记录回看：功能关闭后仍显示同意时间、账号、条款版本、确认方式与文本指纹；
  条款原文可展开（走 `/api/mc/terms`，与开启向导同一份文本与哈希）。**关功能不等于删证据。**
- 发行物开始带 `LICENSE` 与 `NOTICE`：GPLv3 要求随二进制传递许可证，HMCL 的附加条款
  要求保留其版权与修改声明，依赖的 `META-INF` 里没有本项目这一份。

### 日志与保留

- `logs.audit_days` / `logs.panel_days`：审计与面板日志各 180 天起、最长四年。
  启动清一次、之后每 24 小时一次；审计按文件名里的日期算年龄（整体拷数据目录不会洗旧），
  面板日志按 mtime，**最新一份永不删除**（时钟被调错时它是唯一还在写的文件）。
- 「关于 → 日志」里可以直接看到并**调大**这两个窗口（需开发者密码；只升不降，
  要调小请编辑 `server.yaml`）。连点同一个值幂等，真改值同会话 3 秒一次，改动写审计。
- 同一页列出**最近 3 天**的日志并可看最后 200 行。**3 天是服务端写死的**，不随保留窗口变：
  界面能看多久与磁盘留多久是两件事。更早的日志、以及 `.xz` 归档，请走终端或 `curl`。
- **时钟不可信时一个文件都不删**：每 30 分钟拿单调钟（`System.nanoTime`）跟墙钟对一次，
  两次采样之间偏差超过 5 秒就判定“跳变”，清理停到面板重启为止（**粘住，不自动恢复**——
  删掉的日志回不来，一次校时把时间拨回来也不该把这件事洗掉）。另外「某份日志的 mtime 在未来」
  也算墙钟往回跳过的证据，这条**跨重启也成立**（单调钟只能抓运行中的跳变）。
  「关于 → 日志」显示时钟状态、上次检查时间与最近一轮清理结果；被跳过的那轮在界面和
  面板日志里都写明原因。首轮清理等过 90 秒观察窗再跑，否则第一次采样撞不上跳变。
- `/api/health` 的 `java` 只回大版本号：这是免认证探针，没必要把发行版与补丁号送出去。

### 安全

- 请求体上限改按**路径**判定，不再看 `Content-Type`（把 JSON 端点标成 `multipart` 就能绕过 1 MiB）；
  没有 `Content-Length` 的 chunked 正文在 JSON 路由上一律 411；免闸门名单只剩真正流式落盘的
  路由（TLS 上传、壁纸、packs、plugins、世界导入、实例插件安装）。

## v0.6.32 — 2026-10-30 · 修复：MC 服务端起不来（同版本重发）

> v0.6.31 的 MC 启动在容器里必然失败。原因不在启动逻辑，而在**启动方式和默认配置的错配**。
> 首次发布后又补了一条更清楚的报错文案，因此同版本重发一次。

- **根因**：控制台是一个 PTY，PTY 用 `posix_spawn` 拉进程，而 `posix_spawn` 与 shell /
  `ProcessBuilder` 不同——**不搜 `PATH`**（glibc 里它就是 `execve`，会搜的是 `posix_spawnp`）。
  默认的 `mc.java_path: java` 因此以 ENOENT 死在服务器打印第一个字节之前，界面上只留下
  「failed to start the server process」。现在 `McRuntime.resolveJava` 先把配置解析成绝对路径：
  显式路径原样用，`java` 取面板自己运行的 JVM，其余去 `PATH` 上找，都找不到才原样传下去
  （这样报错文案里仍然是操作者配置的值）。
- **首启就绪超时 120 秒 → 300 秒**：第一次启动要解开 bundler 的 50 MB 库并生成世界，就绪标记
  `Done (` 出现得比两分钟晚；旧超时会把一次正常的首启判成失败并杀掉进程。
- `docs/minecraft.md` 补上这条约束（§6 表格后）。
- `PosixPty` 的 spawn 失败文案点名这类错误：程序名里没有 `/` 时直接说「PTY 不搜 PATH，
  请给绝对路径」，而不是让人去怀疑二进制没装。

## v0.6.31 — 2026-10-30 · MC 功能：入口归位、设置中文化、EULA 正文

> 只动那个默认关闭的实验性功能；开关没开的面板与 v0.6.30 逐字节一致。

### MC 服务端

- **功能开关挪进「设置 → 品牌实例」**：和品牌策略同一层。实例列表里的入口是
  「品牌实例」下面的一项（`MC 服务器实例`），**功能关闭时这一项不存在**；
  侧栏原先那个独立页签已删除。
- **实例有了自己的二级页面**（`/mc/:id`）：概览 / 控制台 / 服务端设置 / 世界备份 /
  玩家 / 世界 / 日志。列表页只负责建服、启停与进去；原来的弹窗改成页面里的分块。
- **`server.properties` 表单中文化**：十二个受管字段都有中文名与一句解释，
  布尔值显示「开启/关闭」，难度与游戏模式用中文选项（值仍是文件里的 `true`/`normal` 等）。
- **EULA 拉取改成序列化正文**：Mojang 的页面是营销外壳套着协议，之前把原始 HTML
  整块贴进阅读器，看到的是一堆标签与导航。现在服务端剥掉 `script/style/head`、
  按块级标签断行、反转义实体，并**从协议自己的标题开始截取**（`McHtml`），
  hash 仍按展示出来的正文计算——开启记录对应的就是用户真正读到的那份文本。


## v0.6.30 — 2026-10-30 · 小更新：内部维护与实验性功能落地

> 这一版主要是内部维护，外加一个**默认关闭**的实验性功能。**本版不主动宣传**；
> 未开启时面板行为与 v0.6.22 完全一致。

### 实验性：Minecraft 服务端管理（默认关闭）

- 把原版 MC 服务端当作第三种 surface：复用现有终端、实例模型、端口分配与审计。
- **默认关闭**：无页签、`/api/mc/*` 一律 404。开启需开发者提权 + EULA/条款双确认；
  同意记录（条款版本/文本哈希/EULA 哈希/时间/账号/IP/UA/构建号）写入 `feature.json`。
- 实例：创建/起停/强杀/日志。`server.jar` 运行时从 Mojang 下载并校验：manifest 的
  SHA-1 + 面板自算的 SHA-256 + zip 魔数，运维可用 `trusted.sha256` 把版本钉死。
- 备份/恢复：停机快照；恢复前自动再备份；删除实例前强制备份，拍不出来就拒绝删除。
- 世界：导出/导入 zip（先备份、staging 后整体替换、拒绝 zip-slip）。
- 玩家：RCON `list` + kick/op/deop/ban；`enable-rcon=false` 时明确拒绝而不是连接失败。
- 启动前做 JRE 预检：版本元数据要求 vs `java -version`，太旧直接提示 `mc.java_path`。
- 「停止 MC 功能」后的一小时锁只存在内存里：重启进程即消失；「我不想关掉功能」不解锁
  （见 `docs/minecraft.md` §3.5）。
- **已知未验证**：Windows 的管道控制台只实现了代码路径，**没有真机测试**。

### 工程

- `ApiGuardFilter` 放行 `application/zip` 的流式上传（世界包）；JSON 仍保持 1 MiB 上限。
- 安装类下载改为任务：按字节上报进度，前端轮询任务状态，不再占着请求十分钟。
- 实例删除前自动备份；备份失败即拒绝删除。

## v0.6.22 — 2026-10-30 · 外观：自定义壁纸与种子色，导航更顺手

> 小版本，只动外观与导航；后端只新增一个壁纸接口。

- **壁纸**：外观页可选内置壁纸，或上传自己的（JPEG / PNG，最大 12 MiB）。服务端不看文件名和
  客户端 `Content-Type`，只看字节：魔数、动图特征（GIF/APNG 拒绝）、真的读一遍图片头确认能解码，
  再卡尺寸（12000px 边 / 4000 万像素）。SVG 直接拒——它是能带脚本的文本容器。
  上传的图存 `<data>/wallpapers/<随机 id>.<kind>`（`0600`），列表在会话门后面，登录页固定用内置壁纸。
- **WebP 暂不支持**：JDK 的 `ImageIO` 没有 WebP 解码器，收下来也读不出宽高；与其留一个"看着能用、
  一传就失败"的入口，不如明确拒绝并说明原因。要支持需引入 ImageIO 的 WebP 插件。
- **种子色可改**：取色器或直接填 `#RRGGBB`，运行时重算 Material You 的亮/暗两套调色板，
  整站 `--monet-*` 跟着变；存在本机，换浏览器回默认。
- **返回按钮改成"上一级"**：按路由层级走（实例详情→列表、品牌实例→实例列表、二级页→主界面），
  不再用 `history.back()`——历史回退给的是"上一个看过的页面"，不是"装着这一页的地方"。
- **标题栏图标可点**，任何时候回主界面；**侧栏底部补了退出登录**，不用再绕到设置页。
- **子页背景跟随壁纸**：以前只有主页读壁纸，点进任何子页就打回那张写死的内置图。
- 说明：选中的壁纸记在本机（localStorage），上传的壁纸存在服务端、对所有登录用户可见；
  删除后文件不可恢复，若正被选中会自动回到默认壁纸。

## v0.6.21 — 2026-10-29 · 手机端密度：控件收窄、页签横滑、版本名不再被截

> 没有新功能，只动触摸环境的尺寸与品牌页签的排布；桌面端不受影响。

- **触摸环境控件统一下调**：`.btn` 44→38、`.tool-btn` 44→36（13px 字）、输入框 44→40
  （字号仍 16px，iOS 聚焦不会放大整页）、实例行 48→42、菜单项/侧栏项 44→40。
- **品牌页签行**：触摸设备单行排列、横向滑动；窄屏用短名（去掉「（实验）」后缀，
  下方警示条仍带全名）；图标 16→14、页签 34px/12px、计数徽标 10px、卡片内边距 8→4。
- **品牌实例行**：操作组独占一行——版本选择靠左、四个图标靠右，选择器留足宽度，
  「2.2.0 已装」能完整显示（试过并进标题行，版本名会被压成裸版本号，故放弃）；
  选择点与标题行对齐，行间加一条细分界线标出点击范围。
- **新建实例那一行**：窄屏收窄名称输入与版本下拉，让「实例 / 名称 / 版本 / 创建」
  尽量留在同一行，不再把创建按钮挤到第二行。
- 代价写清楚：34–40px 低于 Material/Apple 的 44px 建议值（WCAG 2.5.8 的 24px 底线仍满足），
  是"可点面积换内容密度"的取舍；真机上若觉得滑手，回滚 `.btn` / `.tool-btn` / 页签三条即可。

## v0.6.20 — 2026-10-29 · 维护版：长期维护前的一轮审计落地

> 没有新功能。这一版把「进长期维护前」的一轮代码审计做完，并把审计补丁自己带进来的问题一起收口。

- **`/api/*` 的请求体上限**：非 multipart 的请求体超过 1 MiB 直接 413。每个 JSON handler 都是把 body
  整个读进内存，这条挡的是那条路上的 OOM；整合包（200 MiB）、插件（50 MiB）、证书这些 multipart
  上传流到暂存文件，不受影响。
- **HTTP 安全头**：面板静态资源补 `X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`、
  `Referrer-Policy: same-origin`。注意 `DENY` 连**同源**嵌入也禁——如果将来要把面板 iframe 进导航页，
  改成 `SAMEORIGIN`。
- **实例清单原子写**：先写临时文件再 move，另一个读方不会再读到半截 manifest。
- **排队中的安装能被守卫看见**：队列里的 `pending` 哨兵原来查不到任务，删除/状态守卫会放行；
  现在有独立判断，实例在安装排队期间不会被删。
- **安装轮询离开页面就停**，开发模式（StrictMode 的模拟卸载）下也不再一亮就停。
- **实例页离开后重新向 `/ws` 发 `unsubscribe`**：服务端（`WsGateway` 本来就有这套语义）不再继续
  推已离开实例的 log/state。
- **反代重试继续用「从未连过该实例」的新 client**：共享一个 client 会让重试落回实例 5 秒前就关掉的
  池化连接，那正是之前「保存 API Key 的 PUT 失败」的成因。
- **E-Stop 第二次按下**不再显示「关闭终端 undefined 个」：已停机时单独提示，冷却时间不重算（后端
  语义如此）。
- 日志视图的 React key 回到下标：`time` 只有秒级精度，同一秒内的重复进度行会撞 key。

## v0.6.12 — 2026-10-29 · Octop dashboard 的挂载点收口：跳转、WebSocket、构建期闸门

> v0.6.11 修的是「静态资源拿回 SPA 外壳变破图」；这一版把同一类问题（根绝对路径逃出
> `/i/<token>/`）**一次收口**，并把闸门放进构建期，不再一个个补。

- **对话的 WebSocket 一直是连到面板根的**：聊天、终端、ADB shell、桌面/手机/浏览器串流共 6 处
  都写成 `ws://host/api/...`，挂载后连的是面板自己的 `/api`，握手必然失败——表现就是"发消息没反应"，
  实例日志里连一条 POST 都看不到。现在统一走 `getWsUrl(...)`（`api/config.ts` 本来就会带上挂载点）。
- **硬跳转**：401 处理器与初始化流程里的 `location.replace("/login"｜"/setup")`、登录回跳的
  `"/chat"` 都改 `publicUrl(...)`；站内「去配置模型」的裸链接改 `<Link to>`。
- **构建期闸门**：新增 `integrations/octop/check-mount-escapes.sh`，打补丁后扫六种模式
  （硬跳转、`href="/`、`src="/`、`location.host}/api`、`fetch("/api`、`window.open("/`），
  命中就让打包失败；对未打补丁的 1.0.3b1 实测能抓出 8 条。
- **构建期就查类型**：`build-dashboard.sh` 与面板源码构建都改用 `npm run build`（`tsc -b && vite build`）。
  多 30–60 秒，换来缺 import / 未使用变量 / 类型不匹配在安装期失败——`TeamDrawer` 缺 `Link` 正是这类；
  宿主需保证 `npm ci` 装 devDependencies（别设 `NODE_ENV=production`）。
- 资源：补丁 20 个文件（原 13 + 新增 7），`octop-dashboard-1.0.3b1.zip` 重建；产物里
  `location.host}/api` 与根 `location.replace` 均为 0。
- 文档：`docs/octop.md` 新增「挂载点不变量」清单，`docs/build-from-scratch-prompt.md` 增 §5.7.4，
  `integrations/octop/README.md` 写清闸门。

## v0.6.11 — 2026-10-29 · Octop 实例的登录密码在面板里就能看/重置

### 同版本补丁（已随 v0.6.11 重发）：dashboard 的静态资源走运行时挂载点

- 现象：实例页面能打开，但空状态吉祥物、欢迎页动画（peek/type）、侧栏收起时的图标、专家/团队
  卡通头像全是破图；`curl -D- https://<面板>/octop-mascot-empty.png` 回的是 `200 text/html`。
- 原因：dashboard 里有 6 处硬编码的根绝对路径（`/octop-mascot-*.png|webp`、`/pwa-192.png`、
  `/experts/avatars/*.svg`）。`<base href="/i/<token>/">` 对以 `/` 开头的路径无效，请求落到面板根，
  面板的静态路由对未知路径回 SPA 外壳（200 + `text/html`），浏览器把 HTML 当图片解码 → 破图。
- 修法：这些路径改走补丁里已有的 `publicUrl()`（运行时读 `<base>` / `window.__OCTOP_BASE__`）；
  `presetIdFromExpertAvatarUrl` 先剥掉挂载前缀再解析预设 id；`teamPortraitUrl` 的默认值比较同时
  接受带前缀与不带前缀两种历史值，避免已存的 `icon_url` 变成"自定义照片"。
- 资源：`integrations/octop/patches/0001-…patch`、`src/main/resources/octop/octop-runtime-base.patch`
  与 `octop-dashboard-1.0.3b1.zip` 一并重建（同版本重发，镜像与 jar 覆盖 `v0.6.11`）。

- 实例行 → 更多 → **登录密码…**：读的是 Octop 自己的 `<OCTOP_HOME>/credential.txt`，其次面板
  首次启动记录的 `octop-admin.txt`；两边都没有明文时返回「不可读」（Octop 只存哈希）并引导重置，
  不再是一句英文错误。
- 重置走 Octop 官方 CLI `octop user passwd <用户名> --password <新密码>`（portable 走
  `runtime/bin/python3.12 launch.py`），成功后把新密码写回两个文件（0600），格式与 Octop entrypoint
  一致；密码策略同步为 ≥8 位且含字母数字，且**对齐 Octop 的常见口令黑名单**（它的 `user passwd`
  本身不校验策略，只有网页端校验，面板补上这层以免设出一个网页端拒绝的密码）。
- 面板第一次 `octop init` 之后也会写 `credential.txt`（Octop 的格式），所以文档里让人
  `cat credential.txt` 的做法在面板实例上同样成立。
- `credential.txt` 也可能是 Octop 的 `Setup Required` 提示块：此时返回的是**向导口令**
  （`mode: wizard`），页面说明「打开实例网页完成向导」，不把它当成管理员密码。
- 两处文件都没有明文时，用 `octop --json user list` 查真实管理员用户名（向导里可改名）；
  Octop 只存哈希，所以「不可读」是正常答案，页面直接引导重置。
- 实例正在运行时重置可能撞 SQLite 锁：面板回 409 并提示「先停实例再试」。
- 门禁：创建后 15 分钟内可直接看；之后查看/重置要开发者密码；明文 HTTP 下面板无法提权，改为
  面板密码（同地址限速，和开发者页一致）。重置不会让已登录的 Octop 会话失效。
- 测试：`BrandApiTest` 覆盖「没有明文时不可读」「从 credential.txt 读到」「重置缺第二因子 → 401」。
- 文档：`docs/octop.md` 新增「登录密码」一节，写清读写来源、CLI 形态与门禁。

## v0.6.10 — 2026-10-29 · 品牌实例管理、下载源，与"不再假死"的安装进度

- 设置页新增 **「品牌实例」**（与「下载源」「开发者模式」平级）：未提权**只读可见**（品牌、已装
  版本、当前生效的源、实例状态），写操作变灰并在点击时弹开发者密码框；明文 HTTP 下写操作全灰并
  说明「需要 HTTPS」。读接口不要求提权——页面不会因为没密码就假装没有实例。
- **品牌策略迁到这一页**（单实例 / 固定反代 / 本地源码编译）。页面按品牌的 `capabilities` 渲染开关，
  以后接新品牌只改后端能力位；`GET /api/settings/brands`、`PUT /api/settings/brands/policy/<brand>`
  取代 `GET/PUT /api/developer/brand-policy`（Go 客户端未使用该接口）。
- 新增**四类下载源**设置：PyPI 索引（uv 本体 + `uv sync`）、CPython 运行时镜像
  （`UV_PYTHON_INSTALL_MIRROR`）、版本源码归档（codeload 形态 `<base>/<slug>/zip/refs/tags/<tag>`，
  环境变量 `CODELOAD_BASE` / `HDSL_CODELOAD_BASE`）、每个走 GitHub Releases 的品牌各自一行（当前
  Octop）。优先级 **面板设置 > 环境变量 > 上游默认**，老部署用 `UV_DEFAULT_INDEX`/`OCTOP_RELEASE_BASE`
  配好的镜像继续有效。npm 不进来：它仍是全局的「下载源」页。
- **测速 vs 保存**：点「测速」会从面板实测该组每个候选、列出毫秒并给最快者标「推荐」，只有点保存
  才写入；一次提交里任何一个地址非法则整体 400 且不落盘。地址限 `http(s)`、去尾斜杠、拒绝控制符与
  `"'\`\\#$;|&@`（这些值会进子进程环境）。
- `BrandInstaller` 接上这些源：Octop portable 的 base 取设置；`uv sync` 注入
  `UV_DEFAULT_INDEX` / `PIP_INDEX_URL` / `UV_PYTHON_INSTALL_MIRROR`；版本源码 zip（Claude Web 与
  Octop 源码编译）走「源码归档」那个 base 而不是硬编码的 `codeload.github.com`；`ensureUv()` 先读
  **镜像自己的 simple 页**解析 wheel（镜像大多没有 PyPI 的 JSON API），失败才回落 pypi.org JSON 与
  GitHub。
- 下载源写进 `launcher-settings.json` 的显式快照（`brandPypiIndexPreset` 等），重启后仍生效。
- **安装进度改成阶段制**：源码编译分成 7 段（下载源码 → 补丁 → `npm ci` → 构建 dashboard → 准备 uv →
  `uv sync` → 收尾），每段带权重，面板显示 `[3/7] 构建 dashboard · 55%` 的文案、进度条、
  已用时间与**预计剩余**（按已完成比例与耗时反推）；portable 安装是 2 段。
- **阶段内还有一条细进度**：能算的先算——下载按字节（`12.3 MB / 260.0 MB · 6.1 MB/s · 剩约 40 秒`，
  取自 `Content-Length`），`npm ci` 按包数（`已安装 762/1245 个包 · 剩约 1 分 20 秒`，拿
  `package-lock.json` 的声明数当分母、数 `node_modules` 当分子）；算不出来的步骤退回按耗时插值。
  两条进度都经任务接口的 `stage` / `stageFraction` 给到页面。
- **`uv sync` 有自己的阶段识别**：解析 uv 输出，显示 `uv：下载 12 个包 · 68.4 MB · 6.1 MB/s`，
  下载结束会明确写一行 **`uv：下载完成（N 个包 · XX MB），开始编译本地依赖`**，编译时显示
  `uv：编译依赖 2 个完成，进行中 1（evdev）`，最后 `uv：安装完成 N 个包`——不再让"下载还差多少 /
  是不是卡了"靠猜。
- **uv 不说话的阶段，就用磁盘信号推**：uv 的进度条只在 TTY 下画，管道里解包/安装阶段一行都不打
  （这台慢盘上能静默几十分钟，看着和死锁一样）。现在每 1.5 秒数一次
  `.venv/lib/python3*/site-packages/*.dist-info`（没有 venv 时退到 uv 缓存里的 `archive-v0` 条目数），
  阶段标签随之变成 **`下载依赖` → `编译本地依赖` → `解包写入磁盘` → `安装完成`**，文案是
  `uv：下载完成（37 个包 · 268.4 MB）· 解包写入 197/345 个包 · 已 42 秒无新输出`。
- **任何阶段沉默超过 15 秒都会自己说出来**（`· 已 N 秒无新输出`），不再需要靠外部 `ps` 判断死活。
- **"下载结束"是一个独立信号，不只是文案**：任务接口多了 `stageKey`（`download` / `write` /
  `build` / `install` / `done`）。下载阶段一结束，页面**弹一次提示**（"接下来是编译/写盘，这台机器上
  可能很久——可以离开去喝杯咖啡"），并在安装卡片里**常驻一条横幅**「✅ 下载阶段已结束 · 正在编译 ·
  慢盘/慢网上可能很久」，直到安装完成。不必再从日志里找那行 `下载完成` 去确认。
- **仿 portage 的"每装一个包打一行"**：解包/安装阶段每 1.5 秒数一次 `.dist-info`，新出现的包写成
  `>>> 已安装 (199 of 345) cryptography-46.0` 追加进 `install.log`（开头还有一行 `>>> 开始安装依赖
  （共 345 个包）：uv 在管道里不出声，这里每装好一个就报一行`）。日志面板打开时每 3 秒刷新尾巴，
  所以它是真的在滚，不是快照。这是 Gentoo 观感的来源——portage 的外壳，而不是源码本身。
- **不再让估算盖掉真实输出**：阶段计时器只在工具安静 2 秒后才写那行 `[6/7] … 93%`，`vite` / `npm` /
  `uv` 的输出能一直看见；日志面板从"一次性快照"改成**边跑边追加**最新状态行。
- **预计剩余不再瞎算**：前端那个用伪造进度反推的「预计剩余 45 秒」删掉了；只有后端知道总量时
  （下载字节、npm 包数）才在消息里写「剩约 …」。
- **安装可以停止**：面板上的「停止」调 `POST /api/tasks/<id>/cancel`；子进程由任务服务整棵杀掉，
  下载与解包循环自己看 `isCancelled()`（它们不是进程）。半成品由启动时的对账清理（见 `octop.md`）。
  下载是 `HttpClient` 的阻塞读，取消标志看不见——现在有一个守护线程在取消时**关掉这条
  socket**，所以「停止」不会等到 5 分钟 HTTP 超时才生效。
- **重复请求同一版本不再报错**：正在装 A 时再请求装 A，返回 202 并给出同一个 `taskId`
  （以前是一句 409「already running」，前端只能显示成「安装失败」）；装的是**另一个**版本才 409，
  文案里带当前步骤。创建实例时如果所选版本正在安装，返回 409 并说明「装完再创建」。
- 品牌页挂载时会**认领正在跑的安装**（`installStatus` 带 `taskId`）：刷新页面、从别的入口进来也能
  看到进度与「停止」，不用等下一次点安装才知道有任务在跑。
- **装完却创建不了的那类不一致**：源码编译成功后现在会更新 `current` 指针（以前停在刚被删掉的旧版本
  上，页面因此不显示「当前」）；创建实例时版本号先去空白；若所选版本已不在已装列表（重装换掉了目录、
  别的页面刷新过版本），前端会重新读取列表并提示再选一次，而不是只甩一句英文错误。
  新增 `BrandApiTest.aReleaseIsCreatableExactlyWhenItsMarkerIsThere` 钉住判据：**只有带着安装标记的
  目录才算已装，也才能建实例**。
- 安装日志面板重做：等宽、自动滚到最新一行、阶段行高亮，失败时仍保留最后 300 行可翻。
- 测试：`BrandSourcesTest`（地址校验、预设、品牌默认 base）、`BrandSettingsApiTest`（只读可见、
  未提权三条写接口全拒、保存与回读、坏地址整体不落盘、单实例必须有保留实例、非 Octop 不许
  `lockedRoute`、未知 kind 的测速被拒）。

### 文档纠错：v0.6.0 已交付、交接稿状态、品牌索引

- `docs/session-handoff-2026-10-27.md` §0 曾写「v0.6.0 那个版本从来没交付过、别再添加 v0.6.0 小节」——
  那是 v0.5.92 之前**撤回**的那一次；正式的 v0.6.0（Octop）已于 2026-10-28 发布。改成准确表述，
  并补上 `main` 的当前状态、tag 名是 `v0.6.0`（不存在 `v0.6.00`）、测试基线 859 用例（2 skipped）。
- 交接稿同时记下**分支血缘分叉与收敛**：v0.6.0 后的三个提交当时因 git 端点（https 443）不通，
  改用 Git API 重放，远端 `web-shell` 拿到和 `main` 内容相同、sha 不同的平行历史；现已把
  `web-shell` 强制对齐到 `fac7023`，compare 恢复 `identical`。下次 push 超时优先重试/换协议，别再重放。
- CHANGELOG 里原本挂在 `Unreleased` 的三节（Octop 接入细节、`all` 档空面板提示、同 uid 边界）实际
  随 v0.6.0 交付，改为「v0.6.0 细节补记」。
- `docs/brands.md` 开头补一句「品牌共五个，Octop 不在那三类里」并链到 `octop.md`；README 的文档
  索引加上 `docs/octop.md`，镜像示例版本号更新到 `0.6.0`。

## v0.6.0 细节补记 — 2026-10-28 · Octop 接入、all 档提示与同 uid 边界

> 下面三节是 v0.6.0 发布当天的细节记录（原先挂在 `Unreleased` 下，实际已随 v0.6.0 交付）；
> 面向用户的总览见下方 `## v0.6.0` 小节。

- 新增品牌 **Octop**（TencentCloud/Octop，MIT）：按普通品牌实例管理，实例 id 是随机 token，
  挂载在 `/i/<token>/`；日常与 Kimi / OpenCode / Claude Web 同一套创建、启动、停止、打开、日志。
- 安装走 GitHub Release 的 **portable 包**（`Octop-portable-<plat>-<version>.zip`），内含 CPython 3.12
  与全部 Python 依赖，面板不需要系统 Python/uv；只把 `packages/octop/dashboard` 换成随面板分发的
  补丁版（`src/main/resources/octop/octop-dashboard-<version>.zip`），Windows 同样直接可用。
- Octop 前端补丁：运行时读代理注入的 `<base>`/`window.__OCTOP_BASE__`，构建用相对 base；
  `auth_token`、`active-agent` 等 localStorage key 按挂载点命名空间化——多个实例同 origin 登录互不踢。
- 代理：Octop 页面只注入 `<base>` 与全局变量，不跑 Kimi/OpenCode 的 JS/router 改写；
  WS/SSE、Cookie Path、Redirect 沿用现有 `/i/*` 实现。
- 开发者模式新增 **品牌实例管理**（需要开发者密码）：单实例模式必须先指定保留实例，其余实例
  灰掉并拒绝启动/打开/修改/删除（数据保留）；可选固定 `/i/octop/` 反代。
- **本地源码编译**（Linux）：下载 tag 源码，应用 dashboard 补丁，`npm ci && build`，再优先从 PyPI
  取 uv、下载 Python 3.12 并 `uv sync`；失败会保留原预编译 release，成功后产出 `.venv/bin/octop`。
  构建完删除 4+ GiB 的 `node_modules`，源码 release 约 2.8 GiB。
- 设置页策略存 `<data>/brands/policy.json`，按品牌分节；默认全关。面板重启后固定路径会自动恢复。
- 版本选择器只列出本面板真正带补丁 dashboard 的 Octop 版本，避免选到渲染不出挂载点的上游 tag。
- 测试：`BrandPolicyTest`、Octop 页面改写用例；插件/Java 基线照旧。

### 「全部 API」档的空面板给个说法

- `all` 档的行为不变（连读都要提权，这是它区别于 `instance` 的地方），但**前端不再让人对着空列表猜**：
  顶部出现一条提示「『全部 API』档：未提权时连实例列表都读不到，所以页面是空的」，
  旁边一个「输入开发者密码」按钮，提权成功后整页重载。横幅复用已有的 `/api/developer/status` 轮询。
- `proxy` / `instance` 两档读本来就开放，不显示这条提示。
- 文档：`docs/web-shell.md` §4.2 补一句"`all` 档连读要提权，界面会明说，不会假装没有实例"。

### 文档：把"同 uid 的 agent"这条边界写清楚

- `docs/design-notes.md` §7.2 增加一条**结构性事实**：`users.json` / `developer.json` / 证书私钥的
  0600 只挡别的用户；agent 与面板同 uid 时可以直接重写它们，也能从 `/proc/<pid>/environ` 读到面板的
  环境变量。面板侧的任何检查都挡不住（校验和与签名密钥也在它读得到的地方），所以不做假防线。
- `docs/deployment.md` 新增 §11「把 agent 当对手：三条真边界」：外层再认证（便宜、只解决一半）、
  拆容器（推荐给 Docker）、不同 uid 跑实例（Linux 最彻底，Windows 无等价物），以及明确列出"不做的事"。
- 没有代码改动：这是边界说明，不是功能。

## v0.6.0 — 2026-10-28 · 新增一个品牌：Octop（腾讯云）

> 这一版的主题只有一个：面板多了一个可以管理的品牌。

- **Octop（腾讯云的开源自托管 AI 助手）**：一个新的品牌分类，和 kimi / opencode / claude-web /
  zcode 并列。安装在品牌页里完成，实例是一份独立的 `OCTOP_HOME`，端口只绑回环、经面板
  `/i/<token>/` 反代访问；面板会话门在前、Octop 自己的 JWT 在后，两层都能单独拦住。
  安装有两条路：官方 portable 包（默认），或开发者模式里的**本地源码编译**（Linux）。
- **仪表盘挂载点补丁**：Octop 的前端按绝对路径取资源，直接挂在 `/i/<id>/` 下会全是 404；
  面板给它注入 `<base href>` 与 `__OCTOP_BASE__` 的运行时补丁（源码在 `integrations/octop/`），
  这样同一份上游产物在子路径挂载下也能跑。
- **品牌策略（开发者模式）**：单实例模式（其余实例灰掉但数据保留）、固定反代 `/i/octop/`
  （开启后「打开」按钮与 open 接口都返回这个固定路径）、源码编译开关，存
  `<data>/brands/policy.json`。
- **品牌实例在手机上不再挤成一团**：窄屏/触摸设备下，实例行的版本选择与操作按钮折到第二行、
  长 url 自动断行，新建行也各自换行（纯前端样式，逻辑未动）。
- **图标与商标**：Octop 一节**不**使用腾讯/Octop 的标志——上游仓库是 MIT，但许可证不覆盖商标；
  所以走中性图标 + 文字标签，理由写在 `NOTICE` 的 TRADEMARKS 与 `BrandIcon` 注释里。
- **顺带修掉的 P0**：`/i/*` 反代曾在正常启动后全 500（`restricted header name: "Host"`）——
  Octop 安装器的静态 `HttpClient` 在反代 servlet 之前初始化，把 JDK 的受限头允许列表定死了。
  现在 `main` 第一行就设属性、装配完再做一次**真探针**自检，顺序错就启动失败而不是静默 500。

## v0.5.95 — 2026-10-28 · 需要开发者密码时弹框 + 安全说明补齐

- **守卫挡住操作时弹密码框，不再干拒**：`启动 / 停止 / 装版本 / 打开实例` 被
  `developer_elevation_required` 挡住时，面板直接弹「需要开发者密码」，输对就重试原操作一次
  （`web/src/elevation.ts` + `components/ElevationDialog.tsx`，`actions.ts` 的 `withElevation`）。
  提权 30 分钟有效，不缓存密码。
- **`/i/<id>/` 的拦截页也带密码框**：守卫要求提权时，那一页直接给「开发者密码 + 提权并继续」，
  成功后整页重载；密码错、没设密码、被锁定各有提示。传输策略的拒绝（`tls_required`）**故意不给框**——
  面板不在 HTTPS 下时密码填了也是明文，只会再被拒一次。
- **错误文案不再只吐英文码**：`errMsg` 会把 `tls_required`、`insecure_transport`、`emergency_stop`、
  `estop_cooldown`、`web_shell_disabled`、`developer_password_not_set` 翻成"先去做另一件事"的一句话。
- 文档：`design-notes.md` §7 扩成完整安全说明（保护什么 / 不负责的边界 / 三层门 + 一个刹车 /
  明文 HTTP 的政策 / 其余不变量）；`plan.md` 认证设计补「第二把钥匙」；`web-shell.md` §4.3 改成
  "开发者模式只在 HTTPS 下可用" 并写清理由，§4.2.1 补「已知取舍」与「验证纪律」；
  `deployment.md` 症状表补守卫/停机两条。
- **行为没有变化**：明文 HTTP 下开发者模式与守卫依旧不开（v0.5.94 的规矩）。

## v0.5.94 — 2026-10-27 · 开发者守卫（反代/实例/全部 API）+ 紧急停机

> 这一版把"开发者密码"从终端门口扩展到别的入口，并加了一个真正能刹住车的按钮。
> 两个开关都**默认关闭**：不开就跟以前完全一样。

### 开发者守卫：提权管到哪，你自己选

- `developer_guard.scope`（`HDSL_DEVELOPER_GUARD`）四档逐级包含：`off`（默认）→ `proxy`（实例网页
  `/i/<id>/*` 的 HTTP 与 WebSocket 都要密码）→ `instance`（再加启动/停止/删除/装包/改设置的写操作，
  读仍开放）→ `all`（除登录、健康检查、开发者页外，全部 `/api/*`）。
- **它是门禁，不是断路器**：只挡新请求。已经开着的实例网页、已附着的终端、正在跑的 agent 都不动，
  重新进入才要密码——要立刻掐断，用紧急停机。
- **给脚本留了正门**：`POST /api/developer/elevate` 现在返回 `token`，用
  `Authorization: Bearer <token>` 就能在没有 cookie 的情况下通过守卫。token 只在内存、与提权同寿命、
  改密码或注销即失效。
- 非 HTTPS 时守卫与紧急停机的控件变灰并写明"需要先启用 HTTPS"——沿用"开发者模式在明文 HTTP 下
  不给用"的既有约定，而不是偷偷放行。

### 紧急停机（E-Stop）

- 三次确认（影响清单 → 输入「紧急停机」→ 不可逆声明），按下前先 `GET /api/developer/estop/preview`
  把**此刻会杀掉什么逐条列出来**：实例、进程数、终端，并写明面板里正在跑的巡检/修复 agent 也会被一起
  杀掉。
- 按下后的顺序是"先关门再清理"：翻转状态 → 关掉所有终端 → 强杀所有实例的整棵进程树
  （先收集 descendants，再 `destroyForcibly`，有界等待后补一轮；**不跑 `post-exit` 自定义命令**）。
- 停机期间：`/i/*` 与实例的写操作一律 `503 emergency_stop`，终端同样 503；登录、健康检查、
  开发者页、只读列表、日志与 `/ws` 照常，否则看不到状态也解除不了。
- **冷却 1 小时**，期间没有人能解除（开发者密码也不行）；状态只在内存——重启面板进程或容器才会
  清空，这是唯一的物理恢复方式，也是本次的明确选择。被杀掉的实例不会自动重启。

### 面板与客户端

- 设置 → 开发者模式：新增守卫四选一与红色 E-Stop 区块（含清单、三次确认、冷却倒计时）；
  停机期间所有页面顶部有红色横幅与剩余时间。
- `GET /api/developer/status` 增加 `guardScope` 与 `estop{engaged,engagedAt,releasesAt,remainingSeconds}`；
  新增 `POST /api/developer/guard`、`GET /api/developer/estop/preview`、
  `POST /api/developer/estop/engage`、`POST /api/developer/estop/release`。
- Go 客户端（hdsl-tui）：新增 `emergency_stop` / `estop_cooldown` 的可执行文案与策略类退出码，
  `DeveloperStatus` 读得出 `guardScope` 与停机状态；旧面板缺字段时按"未停机/守卫关闭"降级。

### 验证

- 新增 `DeveloperGuardScopeTest`、`DeveloperGuardFilterTest`、`EmergencyStopTest`（含"第二次按下不重置计时"
  "冷却未到谁都不能解除""新对象即未停机"三条语义钉子）与 `ConfigLoaderTest` 的四条配置用例。
- `./gradlew --no-daemon check`、`web` 三连、Go 的 `gofmt`/`go vet`/`go test`、插件两条全部绿。
- 冷启动 / E-Stop 的真实效果只能在**独立起的临时面板**上验证，绝不在跑着 agent 的生产面板上按。

## v0.5.93 — 2026-10-27 · 输入框光标对齐

- **TUI 输入框光标少算一格**：行首 `│ ` + `❯ ` 是四格，光标列却按三格算，光标永远压在最后一个
  字符上；中文宽字符时正好落在右半格，看起来就像"光标跑到文本中间"。改成 `4 + 显示宽度`。
  网页终端与 Go 客户端共用同一套 `renderFrame`，两边一起修。
- **Go 客户端自己的输入框也偏**：`cursorCell` 在光标位于末尾时把反色块拼到了文本**前面**，
  中间位置则偏到下一个字符。改为按显示宽度定位，末尾补一个反色空格。
- 走 Go 客户端看面板的，需要换这一版的 `hdsl-tui` 二进制（它的菜单输入框是本地渲染，不随面板更新）。
- 验证：Go `go test ./...` 全绿；插件进程内 59、ACP 105（新增光标对齐测试）；后端 816 用例基线不变。

## v0.5.92 — 2026-10-27 · 第一方终端补完：真审批、Plan mode、内联命令、会话列表

> 面板从这一版起**自带第一方终端插件**（`plugins/` 下两个：进程内与 ACP），随 jar 一起分发、
> 离线安装、零 npm 依赖。这一版把它们的日常可用性补齐：审批默认变严格、命令与补全照 Kimi Code
> 的手感、`/sessions` 能认出是哪个会话。面板的 Java 侧与前端只动了插件相关的那一小块。

这一版继续打磨 P0/P1，不碰 MCP/技能/媒体/主题这些 P2。重点是把"安全默认"和"用起来顺手"补齐。

### 审批

- **默认档位改成 `ask`**：只读工具直接跑，写文件、跑命令、未知工具、敏感文件和计划都发问——
  和 Kimi Code 的 Always Ask 对齐。`critical` 才是"日常编辑和命令直接跑"，`full` 是 Never Ask。
- **敏感文件保护**：`.env*`、`.ssh/`、`id_rsa`、`.npmrc`、`.netrc`、`*.pem`、`*.key`、`printenv`/裸 `env`
  **在任何档位都会问**，`full` 也不例外——把密钥打进回放不可逆。`env FOO=bar cmd` 不算环境转储。
  `web_fetch` 不再按"只读"自动放行：URL 由模型拼，可以把工作区数据带出去。
- **路径先 resolve 再判断**：`..`、相对路径、`/workspace/../etc/passwd` 都不能绕过工作区或系统路径检查；
  写操作读不出路径时发问，而不是当作"工作区内写入"。
- **会话允许名单不再吞掉危险操作**：危险命令、敏感文件、计划始终弹窗；白名单键改成"工具 + 完整命令/路径"，
  `git status` 不会再顺带允许 `git push`，一次文件写入也不会覆盖所有写入。
- **永久规则**：`/allow` `/deny` `/rules [clear]`，存在**实例 home** 的 `.hdsl-tui/permissions.json`
  （随 `/data` 保留、不跨实例共享；`HDSL_TUI_RULES` 可覆盖），deny 优先。
- `/new` `/resume` 之后档位和计数回到默认，不会把上一个会话的 `full` 带过去。
- **审批对话框四个选项，数字键 1-4 与编号一致**：**1 运行** · **2 当前会话内默认运行** · **3 拒绝** ·
  **4 拒绝并说明原因**。第 4 项在对话框里展开一行输入，理由作为一条用户消息交给模型（进程内走
  `followup`；ACP 不能并发 prompt，所以排在当前回合结束后发）。自动放行仍然写 harness 的审计，
  也不放宽沙箱：`workspace-write` 之外照样被拒。

### Plan mode 与输入

- `shift+tab` / `/plan [on|off]` 切换 Plan mode；状态栏和启动页显示 `▣计划`。进程内把
  `/plan on|off` 交给 harness；ACP 明确说这条线没有。
- `ctrl+s` 把输入框插进正在跑的回合（进程内 `agent.steer()`）；ACP 不能并发 prompt，会说明并留在队列。
- `ctrl+o` 仍然是工具展开/折叠；`shift+tab` 不再抢这个键。
- 输入框以 `!` 开头时在工作区直接执行命令，输出是一张 shell 工具卡；退出终端会收掉仍在跑的 shell。

### 会话、补全与渲染

- `/sessions` 作为 `/resume` 的别名；`/compact` 明确是本地压缩，不再写成"交给 harness"。
- **命令能用 harness 的就不自己写**：进程内把 `/permission`、`/plan`、`/goal`、`/feedback`、`/export`、
  `/compact` 直接交给 `ctx.commands.execute`，结果原样显示，所以这些命令随实例的 dsh 版本自动跟进；
  未知命令先问 harness，它也不认识才报错。ACP 没有命令运行时，这些名字会得到一句"这条线跑不了"，
  不是静默失败。
- **`/provider` 增删查测试（仅进程内）**：写 `llm-pi-ai` 的 settings 段（与网页「模型」页同一份，
  用 `mutate` 按路径写，避免把已 redact 的 key 清空），密钥走 `credentials.set`；`--key-env` 只写
  变量名，`--key` 不回显、不写日志、不进 settings；不加 `--yes` 只打印将要写入的块。
- **ACP 的 `/resume` 先关掉要离开的会话**（此前只有 `/new` 关，切几次就在服务端堆几个开着的会话），
  切换失败时明说"仍在原会话"。
- **`/sessions` 只列真正聊过的会话**：读每条持久化会话的首条人类提示，没有提示的空会话
  （误按 `/new`、启动失败、被杀掉的会话）不再出现；列表行用这条提示做标签，会话 id 退到
  后面的元信息，不再只看到一个哈希。
- **读不出的会话不再被静默删掉**：open/read 失败或扫到上限仍没找到提示时，保留一行并标
  「标题未读出」；只有确实读到日志结尾且没有人类提示才过滤。判据放宽到 source 缺失/null
  也算人类、认 `agent/inbox/spliced`（首条提示可能在 turn 认领前先落盘）、content 直接是
  字符串。排序改用会话 JSONL 的 mtime（header 没有 `updatedAt`），不再按 createdAt 把
  天天在聊的老会话排到 200 条之外；`eventCount` 不再当作“空会话”的契约。
- **mtime 取同一会话目录里最新的那份日志**，并按 harness 的根顺序找（`DSH_TUI_SESSION_ROOT`
  → `$DSH_HOME/sessions` → `~/.dsh/sessions` → `~/.dsh-tui/sessions`）：一个目录可能同时留着
  `session.v2.jsonl` 和刚写的 `session.v10.jsonl`，按目录列举顺序取最后一个会拿到旧代的时间。
- **首条提示词只读一次**：读到的提示存在进程内（按 persistence 服务隔离），再次打开
  `/sessions` 不再为每条会话重开日志；空会话与读不出的仍然每次重查，因为它们可能刚变成真会话。
- `@` 空片段做有界递归搜索（跳过 `.git`、`node_modules` 等），带 `/` 列下一层，限制在工作区内。
- Markdown 支持管道表格；审批详情里识别 unified diff 并用 diff 颜色渲染；工具卡对 plan 调用显示 `▣计划`。
- 启动页换成 dsh-TUI README 里像素鲸鱼的静态帧（保留原 SVG 调色板 #142660 / #4e6fff / #bee1ff / #ffffff）+ 模型/档位/计划/cwd/会话 + Kimi 风格按键提示；宽终端两栏，窄终端堆叠。

### 输入与渲染（接手后的修正）

- **Tab 之前根本没到应用**：按键解码表里没有 0x09，`\t` 落到"未知控制字符"分支被直接丢掉——
  补全代码没问题，是键从来不存在。现在 Tab 正常解码，补全与候选行才真的能用。
- **提示行挪到输入框下方**：补全候选、`esc 打断`、`/ 命令 · @ 文件 · ctrl+j · ctrl+o` 现在画在
  输入框**下面**、状态栏上面（Kimi Code 的位置），不再挤在输入框上方。
- **`/` 命令菜单也不再是弹窗**：上一版只挪了提示行，`/` 打开的命令列表仍走 `overlay`，会被居中
  合成到记录区上（带边框、盖住输入框光标）——看起来还是"弹了个窗口"。现在它是输入框下方的内联
  列表：编号 + 描述、↑↓/数字选择、enter 运行、esc 关闭，菜单占的正是底部块的高度，不再遮住对话
  和光标；进程内与 ACP 两条终端共用同一套渲染。
- **`/plan` 不再把 "on" 当成消息发给模型**：harness 的 `/plan` 处理器会把任何非空、非 `off` 的参数
  `steer()` 进当前回合，所以进入计划模式改发裸 `/plan`（离开仍是 `/plan off`）——否则模型会收到一条
  内容为 `on` 的用户消息。
- **`/yolo` 与 `/permission` 改成列表选择**：无参时打开 picker（↑↓ / 数字 / 回车），当前档位标记
  「当前」，不再"点一下换一档"；带参数仍然直接设置。`/permission` 的列表来自 harness 的预设目录。
- **空闲不再重绘**：`Screen#paint` 在整帧与光标位置都没变时**一个字节都不写**；兜底的全量重绘
  从每秒降到每两秒。浏览器终端每次写入都会重排它的输入法辅助框，这正是中文输入法被"吞掉"的原因。
- **状态栏的会话号不再是 `#sessio`**：harness 的会话 id 是 `session-<hex>`，之前直接截前 6 个字符，
  正好截到无用的前缀。改用通知里同一套 `shortId`（去掉 `session-` 再缩写）。
- **中文按等宽渲染**：面板 xterm 的字体栈补上 CJK 等宽字体（Sarasa Mono SC / Noto Sans Mono CJK /
  Source Han Mono / 微软雅黑等宽 / WenQuanYi），全角字符与标点因此正好占两格，不再被裁切或错位。
- **中文输入法不再漂移 / 重发旧文本**：xterm 6 在 composition 期间每次写屏都会重定位候选框，
  而它的隐藏 textarea 还会残留大写字母与空格，遇到 229 键会把整段旧内容重新发进 `onData`
  （xtermjs/xterm.js#6078）。面板终端现在在 composition 期间缓冲 PTY 输出，compositionend
  后再刷；并在 keyup/compositionend 之后延迟清理 textarea 残留（`screenReaderMode` 除外）。
  这解决的是“输入法候选框突然跑到别处”和“按错一个键，前面输入的一段文字又被复制一遍”。

### 面板：告诉你"装着的不是这一版"

- **`GET …/tui-plugins` 每行多一个 `outdated`**：第一方插件比的是**安装包的摘要**（安装成功时记进
  `<实例 home>/.hdsl-web/first-party-installs.json`），社区插件比的是清单锁定的版本。版本号相同但内容
  不同（改代码忘了升版本）从此不再显示为"已安装、无事可做"。
- **没有记录 = 需要重装一次**：老面板装下的插件没有这条记录，页面会标「有新版本」并给「更新」按钮，
  点一下就是同一条安装路由（`file:` tarball），装完记录就补上了。
- **卸载会清掉记录**，重装会重新写入；记录按实例 home 隔离。

### 验证

- 两个第一方插件版本升到 **0.2.1**（`DshTuiPlugins.ENTRIES` 与 `package.json` 同步）；旧 0.1.0/0.2.0
  需要在面板里重新安装——现在会显示「有新版本」并给「更新」按钮（见上一节），不会再"装过就算最新"。
- 插件包补 `NOTICE`：启动页的像素鲸鱼来自 dsh-TUI（MIT）→ dsh-ui-whale（BSD-3），随 tarball 一起分发署名。
- 插件测试：进程内 **59**、ACP **104**（含分类器 18 条、规则/模式重置/steer/plan/表格/鲸鱼启动页/内联命令菜单/空会话过滤/容错读取/最新日志代次/标题缓存）。
- `./gradlew --no-daemon build` 全绿（后端 816 用例基线）。
- 仍未做：后台任务 `/tasks`、`/fork` `/undo` `/title`、图片/视频、MCP/技能/Hook/主题（P2）。

## v0.5.91 — 2026-10-26 · 面板自带两个第一方终端（进程内 / ACP）

面板开始带自己的终端插件：仓库根下新增 `plugins/`，一个目录一个 npm 包；构建时打成 npm
布局的 tarball 塞进 jar（`first-party-plugins/index.json` + `.tgz`），安装时由
`FirstPartyPlugins` 从 jar 写出、以 `file:` 规格交给 `dsh plugin add`——**离线可装**，
不需要 registry，也不需要构建机上有 npm。

两个终端，两种形状：

- **`@nas200-timmy/dsh-inproc-tui`**（profile `dsh-inproc-tui`）：**mode bundle**，叠在
  `dsh-base` 上、跑在 harness 进程里。因此拿到 ACP 给不了的东西：`agent/assistant-stream`
  的 token 级文本与思考增量、`tool/call`+`tool/result` 的工具卡片（工具本身仍是 dsh 的）、
  `approval/request` 审批、`user-questions/request` 追问、`model/selection` 进程内切换、
  `/resume` 与 `/compact`。`requiresDsh >=0.2.0-rc.2`（实测 0.2.0-rc.2 / 0.2.1-rc.3）。
- **`@nas200-timmy/dsh-acp-tui`**（profile `dsh-acp-tui`，bin `dsh-acp-tui`）：**独立程序**，
  不 import 任何 harness 包，用 ACP v1 与 `dsh --profile acp` 通信。**绝对的兼容选项**，
  `>=0.1.2-rc.1` 起都能跑（实测 0.1.2-rc.1 / 0.1.7-rc.2 / 0.2.1-rc.3），也是同实例版本
  来回变时的退路。

安装与运行规则：

- **两个可以同时装，但只有一个在跑**：清单顺序是偏好（0.2.x 优先进程内），启动时取第一个
  「已装 + 文件在 + `fitsHost`」的条目；版本降下去后进程内那个不再匹配，ACP 自动接管。
  列表每行多一个 `active`，页面标「当前生效 / 备用」。
- **安装期只有一条硬规则**：低版本实例装不了要求更高版本的插件（按钮灰掉 + 服务端 409，
  不隐藏该行）；装第一个方终端不再被拒。

渲染层抽到 `plugins/shared` 给两个终端共用。它在仓库里位于插件目录之外、安装后又必须在包内
（`package/shared`）——Gradle 的 staging 任务负责搬运并把 import 路径改写过去，源码保持可读、
tarball 保持自包含。

测试：`FirstPartyPluginsTest`（索引、摘要、npm 布局、目录与构建版本一致）、
`DshTuiPluginsTest`（首方条目、运行分组、宿主区间）、`DshTuiTest`（两版同装时当前线跑进程内、
旧线 ACP 接管）、`PluginApiTest`（清单带 `bundled`/`active`、首方安装走本地 tarball 且落进
自己的 profile）。

### 第一方终端：发布前的加固（审查发现）

代码评审在推发布之前挑出一批问题，下面是修掉的部分——**两个包仍然零 npm 依赖**
（`dependencies` 为空，唯一的 peer 是 dsh 自己的包，且全部 optional），所有检查离线可跑。

- **粘贴根本进不来**：`keys.js` 早就把 bracketed paste 解成 `{name:'paste', text}`，两个 app 却
  没有这个分支，于是往面板里粘贴文字是静默丢失。现在交给 `Composer#paste()`（CRLF 归一化后插入
  光标处）；对话框打开时忽略——它拥有键盘，写进看不见的输入框比丢弃一次粘贴更意外。
- **`/new` 每次必抛**：先清空 `blocks` 再 `adoptSession`，而 `adoptSession` 无条件 `bump(blocks[0])`，
  `block.rev += 1` 在 `undefined` 上抛 TypeError——会话其实建好了，用户看到的是假错误。
- **启动失败会留下常驻进程**：ACP 终端的 `start()` 在握手前就起了 interval，握手/`session/new`
  失败只写一条通告，`main` 的 catch 永远等不到 → 进程（连同已 resume 的 stdin）永远不退。
  现在失败路径还原屏幕、清定时器、关子进程，并把错误抛给 `main`；`stop()` 幂等。
- **`acp.js` 的四处脆弱**：`stdin` 没有 `'error'` 监听（子进程死后写入变成未捕获的 EPIPE）；
  `'error'` 分支不置 closed 让 `close()` 白等；没有握手超时；单行没有长度上限（一条没有换行的
  垃圾既吃内存又让首条消息永远解析不出来）。现在都有：stdin error → 统一 finalize、30s 握手超时、
  16Mi 行长上限、非 JSON 行只报一次协议错误且 reader 继续。
- **两个审批请求互相覆盖**：第二个 `session/request_permission` 会顶掉第一个，前者永远不会被应答
  而 harness 干等。现在排队，应答完一个再显示下一个。
- **进程内那侧的四个真问题**：顶层 `import` 了一个 **optional** peer（`dsh-user-questions`），
  缺这个包的组合会在模块加载阶段就失败 → 改成按 `name`/`code` 形状识别；code-level `inject` 硬等
  `agentDefaultModel`/`sessions` 两个可选服务（缺一个就永久 pending、终端根本不出现）→ 收窄成
  `['agents']`，可选项移到行级 `inject`；`cordis.patch.yml` override 了一个 0.2.x 里不存在的
  `hmr` 行 → 删掉；审批/追问在 0.2.x 是 **waterfall**，现在按 `request.agent` 只答本会话的 agent，
  其余 `next()`。
- **进程内插件的异常会带走整个 harness**（也就是该实例的 web 进程）：事件回调、`tick()`、`render()`
  现在都兜住，坏事件降级成一条（2 秒内合并的）通告，渲染失败退回一行兜底文本。
- **退出会挂**：`quit()` 原来直接 `session.close()`，而回合可能卡在没人答复的审批上。现在先把未决
  的审批/追问以 cancelled 结算、取消当前回合，再**有界**等待关闭；`--help` 也真的退出了（原来只打印
  不 leave，`dsh --profile dsh-inproc-tui --help` 会挂住）。
- **没有 TTY 就不该开屏**：进程内那侧补上 TTY 检查（与 ACP 一致），非 PTY 直接拒绝。
- **包元数据**：ACP 的 `exports["./ui/render"]` 指向不存在的文件、`files` 漏了 `shared` → 修正并让
  `check.mjs` 校验（exports 目标在 staging 后必须存在、`files` 必须覆盖 import）；两个包都补上
  `LICENSE`（声明 GPL-3.0-or-later 却没有许可证文本）。
- **peer 范围必须逐个枚举**：dsh 只发预发布版，而 npm 的 semver 里 `>=0.2.0-rc.2 <0.3` **不匹配**
  `0.2.1-rc.3`（比较符的预发布只承认自己那组 `major.minor.patch`）。所以两个包都列实测过的版本，
  稳定版出现后再换区间——理由写进 `plugins/README.md`。

测试相应补齐：ACP 从 19 条到 **38 条**（协议层从零覆盖到九条：一次 read 两条消息、半行、非 JSON、
超长行、stdin EPIPE、spawn error、握手超时、迟到 id、真实子进程握手），进程内从零到 **9 条**
（可选 peer 的形状识别、waterfall 的归属判定与降级、参数解析、patch 行与 code-level inject 的
静态断言）。把 ACP 的源码换回修复前再跑，18 条新测试里 17 条会红。

### 第一方终端：第二轮审查（输入、会话切换、半安装）

第二轮按"两个终端都是我们的"逐条核对输入路径与会话生命周期。零依赖、离线构建的约束不变。

- **斜杠命令只能从列表里选**：命令面板是 overlay，而 overlay 一律吃掉键盘，所以 `/` 之后的每个
  字母都进了对话框——无法逐字输入命令，Tab 补全也失去意义。现在面板自己声明 `live`：编辑键
  （字符、退格、删除、粘贴、左右、home/end、ctrl+a/e/u/k/w）照常改正文，面板跟着重新过滤，方向键
  与回车仍归它；文本一旦不再是命令前缀（空格、换行、无匹配）面板自动收起。共用逻辑在
  `plugins/shared/ui/edit.js`（`applyEditKey` / `isLiveOverlay`），两个终端同一份。
- **进程内终端的粘贴是静默丢失的**：ACP 那侧有 `paste` 分支，进程内没有（上一轮只修了一半）。
  现在两边都把剪贴板交给 `Composer#paste()`。
- **`Ctrl+J` 会发送消息**：解码层把 `\n` 解成 `enter + ctrl`（裸换行是控制字节，没有跨终端一致的
  键名），而两个 app 只把 `shift/alt + enter` 当换行。现在 `ctrl` 也算换行。
- **`Ctrl+O` 展不开工具输出**：工具卡创建时写了 `expanded: false`，而渲染层的全局开关判据是
  `block.expanded !== false`——卡片把自己钉死成折叠。现在创建时不写这个字段，展开与否由开关决定。
- **`/model`、`/effort` 只改了显示**：`select()` 只往会话日志追加 `model/selection`，而 harness 的
  `system-prompt/assemble` 每一步读的是 `installModelSelection` 拿到的那个 state 对象里的
  `current`——选择器说切了，下一次请求还走旧路由。现在 state 留在 `ChatSession` 上，`select()`
  同时写它（并继续写事件，恢复会话仍读得到）。
- **切换会话不隔离**：`/new` 与 `/resume` 在回合运行时直接换 agent，旧回合的事件落进新会话的记录，
  排队里的消息会被发给新会话。现在两个终端都先 `detachTurn()`：取消当前回合并**有界**等 idle、
  清空队列与流式块，再换；ACP 侧另外按 `sessionId` 丢弃旧会话迟到的 `session/update`。
- **`/resume` 失败却说"已恢复"**：注册表拒绝该 id 时 `ChatSession` 会静默建一个新会话，交互式入口
  没有看 `resumeFailure`。现在按 `resumed` 分支：失败给一条带原因的红色通告并重画 banner；
  成功则把会话日志（最近 400 条）回放成用户/助手/工具卡片——否则"恢复"只是换了个空白屏。
- **半安装仍显示"已安装"**：列表的 `installed` 只读 profile 清单，而 pnpm 先写清单再取包，
  取包失败的 profile 会声称有插件、磁盘上却没有——开终端立刻退出且零输出。列表现在多给
  `filesPresent`（磁盘判据，复用 `DshTui.filesPresent`），页面把这一行标成「安装未完成」（变灰说明
  原因，不隐藏），并同时给「重新安装」与「卸载」。
- 测试：ACP 44 条（新增：面板跟随输入、ctrl+enter 换行、跨会话通告丢弃、`/new` 先取消、
  编辑键白名单、工具卡跟随全局开关），进程内 24 条（新增三条静态断言：粘贴/面板/换行接线、
  工具卡不钉死展开、切换会话先收尾并如实报告恢复失败——进程内的终端循环需要 harness 在进程内，
  行为测试在 ACP 侧跑同一份 `plugins/shared`）。`PluginApiTest` 补「删掉包目录后 `installed` 仍真、
  `filesPresent` 变假、重新安装把文件带回」。

### 第一方终端：第三轮审查（ACP 账户交接、切换竞态、启动恢复）

第三轮的两条高危都在"路由/生命周期写进哪个东西"上：一条把密钥与 profile 对错了，
一条把"发个通知"当成了"回合结束"。

- **ACP 终端的账户路由写错了 profile**：`DshLauncher.plan` 把第三方账户的供应商路由与
  default model 写进"这次启动引导的 profile"，而开终端时计划是按插件的**存放** profile
  （`dsh-acp-tui`）建的——真正跑起来的进程却引导 harness 自己的 `acp` 模板，于是路由在
  运行中的 harness 里根本不存在，账户不可用或落回别的路由。现在计划按 `acp` 建
  （`DshTui.runProfile` + `DshTuiPlugins.harnessProfile`），并把同一个名字放进
  `HSDL_TUI_ACP_PROFILE`，两处不可能再不一致；纯 bundle 依旧按它自己的 profile 引导。
- **任意账户密钥被复制成 `DEEPSEEK_API_KEY`**：ACP 终端在 `DEEPSEEK_API_KEY` 为空时，按
  对象键顺序取第一把非空的 `HSDL_LAUNCH_API_KEY_*` 复制过去——第三方的密钥因此被交给
  DeepSeek 的模型与搜索入口。这段改名逻辑整个删掉：launcher 本来就是按路由名给密钥
  （`HSDL_LAUNCH_API_KEY_<route>`），路由自己读这个名字；`DEEPSEEK_API_KEY` 只由 launcher
  给 DeepSeek 账户设置。
- **`/new`、`/resume` 没有等旧回合结束**：`cancelTurn()` 只是发 `session/cancel` 通知，随即
  返回，切换于是发生在旧 `session/prompt` 还在飞的时候——它回来时会 `finishStreaming()`、
  把 `status` 改成 `ready`、`drainQueue()`，对着**新会话**做这些事。现在 `runPrompt` 把请求
  留在 `pendingTurn` 里，`detachTurn()` 取消后**有界等待**（2s，审批卡住时也要能走），并
  推进 `turnSerial`；晚到的答复因此是空操作（`ownsTurn` 同时比 serial 与 sessionId）。
- **启动时 `--resume` 不回放历史**：交互式 `/resume` 上一轮已经会回放，但面板带 `?resume=`
  进来的路径只印了一条"已恢复"。现在两条路都走 `replayHistory()`。
- **纯 bundle 的空目录仍算已安装**：`filesPresent` 只看 `node_modules/<包>` 是否存在，而
  `pnpm` 是先建目录再拉包，中途失败会留下空壳，纯 bundle 又是从清单指定的 patch 组合出来的
  ——现在要求目录里真的有 `package.json`；有 bin 的包照旧还要入口文件在。
- 测试：ACP **46 条**（新增"切换要等旧回合"与"活过切换的回合不能碰新会话"两条，都先在
  修复前的代码上跑红）；进程内 **20 条唯一测试**（`node --test` 之前把 `test/index.js` 与它
  导入的 `unit.test.js` 各跑一遍，所以"24 条"其实是 12×2 —— `index.js` 已删）。进程内也不再
  用源码正则：新增 `test/app.test.js` 直接驱动 `TerminalApp`/`ChatSession`（粘贴、面板跟随、
  ctrl+enter、工具卡、`/model` 写 `modelState.current`、恢复回放、`/new` 先收尾），
  `agent.js` 的两个 peer 改成 `open()` 里动态 import 才让这个模块可以脱离 harness 加载。
  Java 侧新增 `DshTuiTest` 五条：ACP 计划引导 `acp`、bundle 用自己的、环境里告诉程序同一个
  名字、空目录/缺入口都不算已安装；`PluginApiTest` 补"目录还在但清单没了"。

## v0.5.84 — 2026-10-26 · 开发者提权升级、终端会话生命周期、手机窄屏适配

- **提权可延长、可撤销**：开发者提权不再锁死 30 分钟——「设置 → 开发者」页可以主动停止
  当前提权，所有会话立即失去终端权限；有效期也可按要求延长（最长 3 小时）。登出确认不再
  要求二次输入。
- **登出即掐终端**：主界面侧边栏登录退出时，该会话名下所有终端 WebSocket 立刻断开
  （`TerminalSockets` 统一登记与吊销）；终端连接一律严格一次性，重连必须重新开票。
- **终端头部提权倒计时**：Web Shell 标题栏显示提权剩余时间，临期给出续期入口。
- **手机/窄屏适配**：终端视图钉住 `visualViewport` 并新增功能键条，窄屏（手机 SSH 场景）
  可以缩放、滚动、呼出控制键，不再只靠物理键盘。
- 验证：新增 `TerminalSocketsTest` / `DeveloperApiServletTest`（+95/+70 行），
  相关终端与开发者用例全绿；web 侧 `tsc --noEmit` 通过。

### TUI 插件要配对宿主版本：装得上不等于能用

现场症状：把天枢（`@huiliyi37/dsh-tianshu-tui`）装进 0.1.x 的实例，终端起得来，但会话报
`⚠ agent-presets 未装配：本会话没有工具面`——**窗口能用，agent 一个工具都没有**。

根因不在插件也不在我们：终端界面的**工具面就是 agent-preset 层**，而这一层是 dsh **0.2.0**
才有的架构（`dsh-agent-preset-registry` 注册表 + 每条 `preset` 一行声明）。三套社区 TUI 全是写给
0.2.x 的，各自把这一层写进自己的 bundle patch：

| 包 | 要求 | 依据 |
|---|---|---|
| `@huiliyi37/dsh-tianshu-tui@1.0.0-rc.2` | `>=0.2.0-rc.1` | README 明写；peers 全 `^0.2.0-rc.1` |
| `@tomowang/dsh-tui@0.10.2` | `>=0.2.1-rc.3` | peers 全 `^0.2.1-rc.3` |
| `@xmoon76/dsh-pi-tui@0.5.3` | `>=0.2.1-rc.2` | `engines.dsh` 直接写着 |
| `@deepseek-harness-tui/dsh-tui@0.17.0` | 任意线 | peers 跨 `^0.1.0-rc.6 … 0.2.1-rc.3` |

面板原来把这四条当通用货装给任何实例，于是 0.1.x 上除了最后一条都会得到"没有工具的会话"。现在：

- 清单每项带 `requiresDsh`（`DshTuiPlugins.Entry`，`null` = 任意已发布线），
  `fitsHost` 用已有的 `DshPluginRequirements.satisfies` 判——不新写一个版本比较器；
- **安装路由在提交任务之前就拒**（`409`），而不是让用户等 pnpm 装完再发现不能用；
- **开终端时也拒**（`DshTui.terminalPlugin`）——已经装上的旧组合不该继续产出空工具会话；
  若清单里另有真能跑的条目，仍优先用它；
- 两个路由共用一句拒绝文案（`hostMismatch`）：需要哪个版本、实例是哪个版本、为什么工具面会没、
  可以改用哪个（自动取"跨两条线"的那条）；
- `GET …/tui-plugins` 多给 `fitsHost` 与 `requiresDsh`，界面**变灰不可装**并写明原因——
  按既有约定不隐藏，隐藏会让人以为没这个功能；
- 未知的实例版本不拒（版本来自实例记录，猜错会拦下本来能用的实例）。

顺带一条同源的发现：`@deepseek-ai/dsh-typert-protocol` 的 0.1.x 只发过预发布版，而 dsh 的依赖树里
有一处写的是**稳定范围** `^0.1.1`——semver 下稳定范围永远匹配不到预发布，所以在空 profile 上
`pnpm` 解析会直接 `ERR_PNPM_NO_MATCHING_VERSION`（关掉 peer 自动补装即可绕过）。那是上游 manifest
的问题，记在这里备查。

## v0.5.83 — 2026-10-25 · 一批 bug 修复：实例生命周期与终端、dsh TUI 插件装完即验与宿主配对、容器补真编辑器

这一版是一大批 **bug 修复**：实例生命周期与终端这条线上的两处高危、"装了一个 TUI 插件却打不开终端"
（dsh 侧）、**TUI 插件与宿主版本的配对**（装得上但会话没有工具面），反代与 PTY 的几处加固，以及容器
运行镜像补上真编辑器与解压件。按语义化版本仍是**补丁号递增**——没有新功能，也没有破坏性变更
（终端退出帧新增的字段是可选的，老客户端无感）。

### 容器运行镜像补真编辑器与解压件

- **真编辑器与解压件**：`vim-tiny`（提供 `/usr/bin/vi`）与 `nano`——否则"进容器改个源"会卡在"没有编辑器"；
  外加 `unzip` / `zip` / `bzip2` / `7zip`（tar/gzip 在基础镜像里，`xz-utils`、`zstd` 本就在），
  覆盖 `.zip` / `.bz2` / `.7z` 这三种否则只会回一句 `command not found` 的格式。

### 实例生命周期 ↔ 终端：删/停/启动都要收尾终端（审查发现的两处高危）

外部审查报告指出这条线上有两处"文档承诺了、实现只做了一半"。两处都在 dsh 侧——品牌与 ZCode 一直是
对的，dsh 漏了：

- **删除 dsh 实例不结束它的终端**：`InstanceRuntime.stateOf` 只看 web 进程、不认 PTY 会话，所以
  终端开着时实例仍读作 `STOPPED`，删除判定放行，目录被删而 agent 进程活着。更糟的是**杀入口一起没了**：
  `DELETE .../terminal` 先查实例、删后必然 404，此后再没有任何路由能碰到那个进程。现在删除/停止/启动
  三条路径**一律先** `instanceTerminal.close("dsh", id)`（与品牌、ZCode 逐字同序）。刻意**没有**去改
  `stateOf` 把终端算成"运行中"：终端是实例的资源，正确做法是"操作前无条件收尾"，而不是给状态机加一个
  跨进程判据——那会牵动 UI 徽标与所有既有判定。
- **web ↔ 终端互斥只做了半个方向**：开终端会停掉 web（原有），但**启动 web 不停终端** → 两个进程同时写
  同一份 `DSH_HOME`；而终端在跑时按「停止」反而得到 `409 instance not running`（判定只看 web 进程）。
  现在启动前先收尾终端；停止先收尾终端，**只有终端在跑时回答 `stopped` 而不是 409**（什么都没在跑仍然
  是 409，口径没有放宽）。
- **PTY 主描述符的持有方式**：`closeMaster()` 与 `poll`/`read`/`write` 之间没有共享锁，而描述符号会被
  内核回收——一个已经通过检查、正在内核里的调用，可能读到**另一个实例**终端的输出或把按键写进它的 PTY。
  改为**每次调用各持一份 `dup` 出来的副本**（只锁账本，不把系统调用塞进锁里），关闭只回收主号；
  退出排空同时改成"排到 EOF、超时兜底"，`Buffer` 在收到结束标记后**仍然收下已读出的那一块**（原先直接
  丢弃，丢的是屏幕最后几行）。
- **反代的两处语义**：客户端断开时**真正在飞的那个上游请求**才会被取消（此前取消的是 `compose` 派生
  出来的 future，SSE 会滞留到自然结束，与类注释不符）；**首次失败的重试只限幂等方法**（GET/HEAD），
  POST/PUT 不再可能被静默发两次；`Set-Cookie` 路径改写对 mount 做 `quoteReplacement` 转义。
- **任务进度行的收发方式**：`holder[0]` 单格种子全部换成 `TaskService.TaskWork`（提交时就把任务交给
  任务体）。`submit` 是先交给执行器再返回句柄的，那个窗口里到达的进度行原先没有收件人——丢的正是
  pnpm 的第一行，也就是用户盯着等的那一行。

### dsh 的 TUI 插件：装完要验，"没装好"不再装成装好了

第二份现场审查把"打开终端 0.4 秒死掉、零输出"的根因指到了插件本身：`pnpm` **先写依赖再取包**，
取不到时（同一台机器的审计日志里有 6 次 `ERR_PNPM_FETCH_404`）会留下"manifest 说装了、
`node_modules` 里什么都没有"的半装状态，而面板只看 manifest，于是照常开终端、照常起一个空 profile。

- **装上就验**：`dsh plugin add` 退出 0 之后还要看 `node_modules/<包>` 到底在不在
  （`DshPluginInstaller.requireLanded`）；不在就把这个安装任务判**失败**，说清"依赖写进了 manifest、
  文件没落地"。只验 registry 型 spec——`file:`/`link:`/`github:`/URL 的包名从文本里读不出来，
  误判会打断正常的本地安装。
- **半装状态不再算已安装**：`DshTui.terminalPlugin` 现在同时要求 manifest 声明**与**文件真的在
  （有 bin 的包还要求它声明的入口存在）。声明了但文件不在 → 拒绝，并且文案是"重新安装这个"，
  而不是"去装一个"。清单里靠前的那条声明了却没文件时，不再挡住后面真的装好的那条。
- **拒绝也进审计**：`terminal.open` 的这条拒绝原先不落审计，于是"被拒绝"和"开了又秒死"在日志里
  长得一样——现在分得开。
- **真死了要留话**：子进程非零退出且**一个字节都没输出**时，应用日志写一条（实例 / 退出码 / 没有
  输出），退出帧带 `failedWithoutOutput`（`docs/terminal-protocol.md §3`），终端窗口在
  "会话已结束 (N)"下面补一句可行动的提示。面板自己结束的会话（停止/删除实例连带收尾终端时那条
  143=SIGTERM）不算——诊断它只会把日志变成噪音。

### 反代、PTY 与包安装的三处加固

- **反代不再要求它解不开的编码**：请求侧把上行的 `Accept-Encoding` 收敛成重写器认得的
  `gzip, deflate`（没有就干脆不发，走 identity），而不是原样透传 `br`/`zstd`、再把压缩字节
  当 UTF-8 拼回浏览器；兜底分支现在会记一条警告，不再静默。代价是体积大的流式响应拿不到 br——这是
  有意取的（loopback 实例上那点体积换"页面不会变乱码"）。
- **PTY 的两个描述符在 `openpty` 之后就带 `FD_CLOEXEC`**，子进程的文件动作里也关掉它手里的
  slave 副本，租约改成一次 `fcntl(F_DUPFD_CLOEXEC)`。原来 master 要等 `posix_spawn` 返回才加标志，
  slave 从来不加：一个并发 spawn 的其它子进程会继承这份 pty（能读该实例终端、也能往里打字），
  而子进程自己留着 slave 副本会让 grandchildren 一直握住它——master 永远等不到 EOF，
  "排空到 EOF"会退化成等 2 秒超时。
- **越界写不再怕已存在的软链**：`DshPackInstaller`/`DshPluginBundle`/`NodeRuntimeManager` 的
  路径检查原本只做字面比较，目的地里一个**已存在**的、指向外部的链接目录就能被穿过写出去
  （包本身种不了链接，但 `link:`/`file:` 装过的本地插件会留下这种链接）。新增 `util/io/SafePaths`
  一处实现：按**已存在的最深祖先**取真实路径比较、逐段建目录时不穿过离开根的链接；指回根内部的
  链接照旧放行（`$DSH_HOME/skills` 挂另一块盘、pnpm 的 `node_modules` 链接都是合法的）。
- **凭据名单收敛成一份**：导出侧的拒绝集合改成由共享常量构成（导出 ⊇ 导入），补上
  `.netrc`/`.pypirc`/`.yarnrc`/`.yarnrc.yml`、`id_ed25519`/`id_ecdsa`/`id_ed448` 前缀与
  `.jks`/`.keystore`——这些原先**导入拒收、导出照发**，一个无扩展名的 `id_ed25519` 能随包出去。
  两侧各自独有的更严规则一条没删，并加了一条测试把两侧钉在一起（往一边加名字而忘了另一边会红）。

### 两处并发老毛病与日志权限

- **安装失败的"尾巴"不再跨线程裸读**：`tail` 由进程泵线程写、任务线程在报错时读，中间没有
  happens-before，原来的 `ArrayDeque` 可能丢行、也可能抛 `ArrayIndexOutOfBoundsException` 把真正的
  失败原因顶掉。改成 `ConcurrentLinkedDeque`。
- **`onExit` 真的只跑一次**：状态写入与 listener 快照原本分两步，中间注册的 listener 可能被跑两遍
  （后果是 `terminal.exit` 审计重复一条；现场没撞上，但压测能复现）。现在两步在同一把锁里。
- **日志文件 0600**：应用日志与轮转归档原先跟着 umask 走（容器里 0644，里面有用户名、来源地址、
  实例名、代理 URL）。照 `AuditLog`/`Main` 的既有写法在创建时收紧，非 POSIX 文件系统仍照常工作。

## v0.5.82 — 2026-10-25 · 容器补齐构建工具链（JDK / go / rust / python / busybox）

- **容器里补齐构建工具链**（Dockerfile 运行阶段）：`openjdk-21-jre-headless` 换成
  **`openjdk-21-jdk-headless`**——要的是 JDK 而不是 JRE，`javac` / `jar` / `jcmd` 正是排查时真会伸手去拿的
  东西；并补上 `golang-go`（Go 1.24，与 `client/hdsl-tui/go.mod` 同一条线）、`rustc` + `cargo`、
  `build-essential`（gcc / g++ / make / libc6-dev：node-gyp、rustc 的链接器、`make install` 都要它）、
  `pkg-config`、`wget`，以及 `python-is-python3`——Debian 只装 `python3` 时**没有裸 `python` 命令**，
  而不少构建脚本调的就是它。
- **加上 `busybox-static` 急救箱**：一个**静态链接**的 busybox（`busybox sh` / `vi` / `wget` …）。特意用静态版——
  动态链接的 busybox 会跟着它依赖的 loader 或 libc 一起坏掉，而那恰恰是它被留在镜像里要修的状态。
  注意它**不**提供 PATH 上的 `vi`：这个包只装 `/usr/bin/busybox`，applet 一律要写成 `busybox vi`。
- 容器是"医者自医"的现场，缺一把手术刀就白搭；代价是镜像明显变大，且 arm64 那条腿要在 QEMU 下装这些包，
  这一阶段会慢一些。（真编辑器与解压件是下一个版本进的，见 v0.5.83。）

## v0.5.81 — 2026-10-25 · 终端入口、整合包契约补齐、TUI 插件注入

三条线一起推：面板里的**终端入口**变得名副其实；**整合包**补齐可选能力与向后兼容；**dsh 的终端**
从"假设上游有 `tui`"改成"装一个 TUI 插件"。

### 终端入口统一为「终端窗口」

四处终端入口（实例列表、实例详情、品牌实例、ZCode）此前都用 `DeployedCodeIcon`（一个代码立方体），
既不统一也不像终端。现在统一换成 **Material Symbols 的 `terminal` 窗口图标**（圆角窗口 + `>` + `_`）；
`makeIcon` 增加可选 `viewBox` 以承载该图标的新视口。不可用时**变灰禁用而不是整个消失**——隐藏会让
人以为没有这个功能。

### 整合包：可选能力与向后兼容（对齐 DSH-PackForge 规范）

规范仓库把现行版本定为 manifest v5 / `.dspack` v3 / index `schemaVersion 2`；本版补齐四处：

- **导出 `home/`**（`pack-structure/v3.md §5`）：可选白名单，把全局 `DSH_HOME` 内容（`skills/`、
  `.agent-presets/`、`AGENTS.md`）随 profile 包分发——**不勾选则什么都不随包走**，凭据/压缩包过滤
  原样保留。导出弹窗新增勾选列表（默认全不勾），`GET /api/instances/{id}/export` 提供可选项。
- **`dshVersions` 兼容集**（`manifest/v5.md §12`）：导出写实测集（实例自身版本在首位）；导入按
  `pack-structure/v3.md §9.1(2)` 做 `∩ 本机已装` 的确定性选择（优先 pin、其次最新），**不再因为
  那个版本本机没有就整包失败**；市场条目也读该字段。
- **`launchers` 声明**（`manifest/v5.md §13`、`launcher-registry.md`）：简式与全式都解析，归一化后
  查本启动器 canonical id `hdsl`，逐段数值比较，**只警告放行、绝不硬拒**；警告随导入结果回报。
  导出侧刻意**不写** `launchers`（HDSL 导出的包保持通用）。
- **向后兼容导入**：`.tgz`（pack-structure v1）按 gzip 魔数识别并导入，manifest v2/v3 可导入，
  **v1 拒绝并说清它被压平了什么**。

### dsh 的终端：从 `dsh tui` 到「装一个 TUI 插件」

上游 `dsh <name>` 就是 `dsh --profile <name>`，但 `tui` **不在**自动初始化的 profile 模板里——所以
"没装插件就打开终端"会以 `profile "tui" does not exist` 在 0.4 秒内退出（审核实测）。本版：

- 面板提供**精选 TUI 插件清单**（`DshTuiPlugins`）与逐实例安装/卸载
  （`/api/instances/{id}/tui-plugins`；`profile` 由服务端按清单决定，请求无法指定）；
- **硬约束**：TUI 插件**绝不能装进实例的 `web` profile**——它和 `dsh-web-app` 都是 app 插件、
  各按自己的语法解析同一份 argv，混装会让应用遇未知旗标直接退出。安装器新增 profile 形参与
  `refuseAppProfile` 守卫（拒绝发生在解析运行时之前，所有安装路径共用）；
- `DshTui` 改为启动**插件自己的 profile**；没装插件时给出可行动的错误，不再跑坏命令；
- 实例「安装」页新增 TUI 插件区段与**醒目免责声明**：面板只做 npm 安装 / 版本锁定 / 挂到插件自己的
  profile，**不修改 dsh 任何源码**，插件是第三方社区作品、功能与安全归其上游，并展示实际包名 /
  版本 / 来源仓库 / 许可证与一键卸载。

### 终端 WS 不再被 30 秒"静默"掐断

`docs/web-shell.md §3` 一直写着「无空闲超时」，但 `TerminalWebSocket` 从未设置过——终端 WS 因此吃
Jetty 默认的 30 秒，agent 一静默就被 `1001 Connection Idle Timeout` 关掉（审核实测）。现在显式
`setMaxIdleTimeout(0)`；客户端同时加了心跳（20 秒 ping、10 秒 pong 期限），半死连接不再干挂。
（客户端要求对端回 pong；仓库 pin 的 Jetty 12.0.39 会回，已反编译确认。）

### 顺手修掉一处导出竞态（CI 的 JDK 17 那条腿先撞上）

`TaskService.submit` 立刻返回、任务体在另一线程上跑，而三处调用方写的是「先 submit、再把句柄塞进
`holder[0]`」——任务体跑到 `holder[0].attachResult(...)` 时可能还是 null，导出会偶发失败并报
`Cannot invoke … because "holder[0]" is null`。现在任务体**直接拿到自己所属的 Task**
（`TaskService.TaskWork`），竞态从构造上消失；其余 `holder[0]` 读取本来就带 null 判断（只用于进度
显示），保持原样。这条不是新引入的缺陷——是既有写法，只是本版新增的快速导出用例把它踩出来了。

### 客户端（hdsl-tui）对齐

- 终端失败时**保留服务端给的原因**（不再被通用文案盖掉）；提权顺序改为「状态 → 密码 → 内核 → 终端」，
  与网页一致（用早已存在、此前从未调用的 `GET /api/developer/status`）；
- 选项可以放在地址**前后**（Go 标准 flag 有"首个位置参数处停止解析"的陷阱）；
- 补上帮助里早已承诺的 `j/k` 菜单导航；
- `terminal_failed` 的退出码统一为 **4（策略拒绝）**——此前两条路径上一个 3 一个 4。

验证：**741 用例 / 0 失败 / 2 跳过**（Linux；含新增的整合包与 TUI 插件用例）；前端 `tsc --noEmit`、
`vite build`、兼容闸门全过；客户端 `gofmt` / `go vet` / `go test ./...` 全绿，5 平台交叉编译通过。

## v0.5.72 — 2026-10-24 · ZCode 终端修复、客户端证书非交互入口、Claude Web 端点覆盖与官方内核预检

**ZCode 终端起不来**：报 `terminal_failed: zcode.nodePath does not point at a file: node`。

- **原因**：`ZcodeApiServlet` 把 `zcode.nodePath` 原样传给适配器，而 `node` 正是它的默认值；
  适配器却先 `Path.of(...)` 再要求那是个文件——默认路径必然抛错，等于所有没显式配绝对路径的
  部署，ZCode 终端全坏。网页模式不受影响，因为它走的是 `Executables.startableProgram`。
- **修法**：适配器改用同一个 `Executables.resolve`——带目录的值当路径（必须是可启动文件），
  裸名字按 `PATH` 查找（Windows 会补 `.exe`/`.cmd`）。与网页模式行为一致，`node` 默认值恢复。
- 测试：`ZcodeTuiTest` 新增两条——裸名字按 PATH 解析；从 servlet 的默认值走完整入口能产出
  可执行 argv。

**客户端 `--insecure` 补非交互入口**：以前它一律要求手输 `yes`，而 stdin 不是终端时直接报
「需要交互确认，但当前无法读取输入」——脚本 / CI / ECS 控制台里等于用不了（报告里点到的
那一条）。现在：stdin 是终端时行为不变（打印指纹、要求 `yes`）；不是终端时只打印那句风险
警告，继续连接。显式的 `--insecure` 本身就是那次同意。

**部署文档补 systemd 单元**：`docs/deployment.md` 新增 §10「裸部署（systemd）」，示例单元里
写了 `Environment=HOME=…` 及原因（systemd 系统服务默认不注入 `HOME`，pnpm/node 生命周期
脚本会因此失败，而 pnpm 只报 `ERR_PNPM_EXECUTOR_LIFECYCLE_SCRIPT_FAILED`）。面板本身已有
`user.home` 兜底，这行是双保险。

**Claude Web 端点覆盖与官方内核预检**：Anthropic 按地区封禁——从不受支持地区（含中国大陆）
访问 `api.anthropic.com` 一律 403，官方 Claude Code 打印 `App unavailable in region` 后退出。
面板为这条路补上两个能力。

- **实例级端点覆盖**：`PATCH /api/brands/claude-web/instances/{id}` 接受 `baseUrl`（`http(s)://`
  或空串清除，其它品牌 400），值存进 `instance.json`；实例菜单新增「Anthropic 端点…」对话框。
  `ANTHROPIC_BASE_URL` 同时进**终端**与**网页模式**的 child 环境——同一个 workbench、同一份 CLI
  配置，不能让两边指向不同端点。凭据仍走面板进程环境（`ANTHROPIC_API_KEY` 等由子进程继承），
  面板不新增存密钥的地方。
- **官方内核预检**：选官方内核时先 `GET <端点>/v1/models`——不受支持地区回 403，受支持地区没带
  key 回 401。只有 403 会被拦下，回一句「改用社区引擎，或在这个实例上配置 ANTHROPIC_BASE_URL」；
  401、超时、DNS/TLS 失败都放行，交给 CLI 自己解释。探针结论用 `regionBlocked(status, body)` 判定，
  403 且响应体缺region 字样时仍按拒绝处理。
- 文档：`docs/brands.md` 新增「Claude Web：官方内核与 ANTHROPIC_BASE_URL」，写清政策、两条出路与
  第三方中转的合规风险；README 的 Claude Web 段落同步。

验证：**714 用例 / 0 失败 / 2 跳过**；客户端 `gofmt` / `go vet` / `go test` 全绿；真实面板 +
假 ZCode 发行包，`HDSL_ZCODE_NODE=node` 下终端接口返回 pty 票据，`node <pkg>/bin/zcode.mjs`
子进程确实起来了。Claude Web 两处新增 5 条用例（端点覆盖存取/清除/校验、其它品牌拒绝、plan
带环境变量、探针与判定、终端环境注入）全绿。

## v0.5.71 — 2026-10-24 · 终端：ZCode、Claude Web 双内核、证书应急

> 版本号说明：v0.5.21 当初把补丁号写成了两位（本意是 v0.5.2.1），`sort -V` 会把它排在
> 0.5.6 之后，docker 的 `latest` 闸门于是一直指着它。这次直接取 **0.5.71**——数字上明确
> 大于 0.5.21 与 0.5.6，把那个错号盖过去，不需要再挪旧标签。

这一版把「终端」这条线补齐：ZCode 接进来、Claude Web 的内核交给用户选、证书不匹配时可以
只对本次放行；顺带把打开终端的顺序改成「先密码、再终端」，并修掉子路径部署下的两处隐患。

### 打开终端：先密码，再终端（全品类）

原来点「终端」先出一个空终端窗口，再在窗口里要开发者密码——先看见终端再输密码，反直觉。
现在申请中、密码、首次设置、选内核都是**对话框**，验证通过、拿到票据之后才换成终端窗口；
xterm 在窗格挂载后才接 WebSocket（票据先暂存，不会因为对话框还在就丢连接）。断线重连沿用
刚才选过的内核，不再问第二遍。

### Claude Web：两个内核，用户自己选

Claude Web 的内核本来就是 Claude Code CLI，只是它带了两份：

- **社区引擎** `node_modules/claude-web-engine/dist/cli-node.js`（用 `node` 起）——网页里
  配置的第三方/中转供应商只对它生效；
- **官方 Claude Code** `@anthropic-ai/claude-agent-sdk-<platform>-<arch>/claude[.exe]`
  （原生二进制，发行包没带时退回 `PATH` 上的 `claude`）。

新增 `GET /api/brands/claude-web/instances/{id}/terminals` 列出可用内核（含缺什么），
终端接口多了 `?kernel=community|official`；**不静默替换**——选了没装的那个直接报错说明
缺什么。网页只在两个内核都可用时才问，Go 客户端进入终端前也加了上下选择（`--kernel`
可跳过）。

### ZCode：官方 TUI 接进 Web Shell

ZCode 的发行包不带参数启动就是官方 TUI（`runner.mjs` 会 import `agent/zcode.cjs`，CLI 的
默认命令是 `tui`）。新增 `ZcodeTui`，起 `node <发行包>/bin/zcode.mjs`，**不传任何参数**——
`--web` 一套是 web 模式专属，`--workspace` 也只有 web 分支解析，工作区靠进程 cwd；缺
`bin/zcode.mjs` 或 `agent/zcode.cjs` 时提前给出可读原因。

状态按上游源码分工：`ZCODE_DATA_BASE_DIR` 指实例 `data/`（凭据与供应商配置都在它下面），
会话库跟 `homedir()` 走，所以 `HOME` 保持与 web 进程一致——否则网页与终端会看到两套会话。

### 一份门禁，一套生命周期

dsh / 品牌 / ZCode 三处逐行重复的终端逻辑收进 `InstanceTerminal`（Web Shell 开关、HTTPS、
开发者密码、提权、Windows 桌面分支、spawn、一次性 ticket）。开 TUI 前同步停 web（等进程树
退出），启动 web 前先停 TUI，停止/删除实例连带结束终端；dsh 打开终端时的 web 停止也从异步
改成阻塞式，避免两个进程抢一个 `DSH_HOME`。终端注册表改按 `brand + instanceId` 存，
三个品类的 id 空间不再互相串会话。

### 客户端：证书不匹配时可以「仅本次」继续

面板证书只签了域名、客户端却用局域网 IP 连时，`hdsl-tui` 原来只有 `--insecure`（全局关掉
校验）或连不上两条路。现在按类型识别证书错误（`x509.HostnameError` /
`UnknownAuthorityError` / `CertificateInvalidError` / `tls.CertificateVerificationError`，
穿过 `TransportError` 与 `*url.Error` 的包装，不认字符串），在菜单里给「← 返回（推荐）」
与「仅本次忽略风险并连接」两条；默认停在返回。接受后 REST 与 WebSocket 一起切换、状态栏
常驻「证书未验证」，且**只对本次运行有效**（不写配置、换服务器即恢复）。非证书失败
（DNS、拒绝连接、超时、403/404）不会出现这个入口。

### 子路径部署加固

- 会话 Cookie 的 `Path` 跟着挂载点走：同一个域名下挂两个面板时不再互相踢下线；
- 终端 WebSocket 接受服务端返回的绝对 `ws://`/`wss://`，不再往上拼挂载点；
- `docs/deployment.md §1` 重写为「谁终结 TLS」的两套 nginx 配置（根路径 / `/hdsl/` 子路径）、
  排错对照表与验收命令；README 开头补了「现在能做什么」。

### 验证

- 后端：`web.{auth,developer,brand,http,terminal,zcode}` 138 个用例，2 个 Windows 专属
  POSIX 权限用例失败（既存问题）、20 个 Linux-only 用例跳过；
- 前端：`tsc --noEmit` 与 `vite build` 通过；
- 客户端：`go vet` / `go test ./...` 通过，5 个平台交叉编译通过（含真实 TLS 握手的用例）。

## v0.5.6 — 2026-10-24 · 子路径反代下的终端连接

面板挂在反向代理的子路径（`HDSL_BASE_PATH=/hdsl`）下时，网页、登录、实例列表都正常，
只有终端连不上：客户端把服务端返回的根相对路径 `/ws/terminal/{ticket}` 直接拼到主机上，
挂载点前缀丢了。根路径部署不受影响，所以这个问题一直没暴露。

- **网页**：`TerminalWindow` 改用项目已有的 `withBase()` 生成 WebSocket 地址，与状态通道、
  其他 API 走同一个入口；`withBase()` 同时挡住三种边界——挂载点带尾斜杠、路径不带前导斜杠、
  服务端已经带上前缀（不再出现 `/hdsl/hdsl/…`）。
- **Go 客户端**：`panel.Client.WebSocketURL` 不再用 `ResolveReference`（根相对路径会替换整个
  path），改为把 `ws` 当作面板内部路径、显式拼上连接地址的 path 前缀；完整 `ws://`/`wss://`
  地址仍按原样使用，`query` 与 ticket 的转义保持不变。
- **错误阶段**：WebSocket 在 `ready` 之前断开时，网页显示「终端票据已获取，但 WebSocket
  通道连接失败……请检查反向代理的子路径与 Upgrade 配置」，不再与「密码错误/面板离线」混在
  一起；`onerror` 不再抢在 `onclose` 前面把阶段改成笼统错误。
- **裸部署的 `HOME`**：`DshCommand` 在子进程环境里没有 `HOME` 时用 JVM 的 `user.home` 兜底。
  systemd 不给服务注入 `HOME`，pnpm 装的 koffi 会因此 `Missing HOME environment variable`
  失败，而 pnpm 只报 `ERR_PNPM_EXECUTOR_LIFECYCLE_SCRIPT_FAILED`，看不出原因。显式传入或
  继承到的 `HOME` 仍然优先。
- 协议文档补上 `ws` 字段的约定：它是面板内部路径，挂载点由客户端负责加。
- 验证：Go `gofmt` / `go vet` / `go test ./...` 通过（新增 15 组 base path 用例）；
  前端 `tsc --noEmit` 通过；`DshCommandHomeTest` 与 `PosixPtyTest` 全通。

## v0.5.5 — 2026-10-23 · 终端客户端改成全屏菜单

`hdsl-tui` 原来的界面是逐行问答：先要地址，再要两个密码，然后一个编号列表，agent 一退出
整个程序就没了。手机 SSH 上这套很别扭。这次把它改成 Debian 安装器那样的全屏菜单，并把
「一次登录、反复使用」做进去。

- **默认裸输入 `hdsl-tui` 进菜单**：先是服务器选择（用过的排在前面，也能当场输入新地址），
  再是登录。没有地址时不再要求先写参数；带地址只是跳过选择这一步。
- **层级与回退**：服务器 → 主菜单 → 分类（dsh / kimi / opencode / claude-web）→ 实例 →
  动作。每层都有 `← 返回`，`Esc` 逐级回退，界面底部常驻键位说明。空分类显示为禁用行而不是
  消失，免得用户以为列表坏了。
- **Ctrl-C 真的能停**：菜单和输入页里 `Ctrl-C` 取消当前动作（输入页退回上一级，根层退程序），
  `Ctrl-U` 清空输入框。改了 raw 模式下的按键解码：`\x03` 作为一个事件处理，不再依赖信号。
- **不再每次两道密码**：登录会话存 `~/.config/hdsl-tui/config.json`（`0700` 目录 / `0600`
  文件，`HDSL_TUI_CONFIG` 可改），下次对同一面板直接复用；开发者密码只在面板回
  `developer_elevation_required` 时问一次，之后 30 分钟内不再问。主菜单里可以主动提权、
  看会话信息、清除保存的登录。
- **退出 agent 不再退出程序**：`Ctrl-] q` 离开终端、`Ctrl-] k` 停掉终端、agent 自己退出，
  三种情况都回到实例动作菜单，登录和连接都还在；agent 的退出码在菜单里明说。
- **修掉一个真实竞态**：界面层原来在 `Suspend()` 之后仍有一个协程阻塞在 stdin 的 `Read` 上，
  attach 后的第一个按键会被它吞掉——通常正好是 `Ctrl-] q`。现在全进程只有一个终端读取者，
  attach 期间把字节原样转交给终端泵（`internal/rtty`），界面恢复后不会丢键。
- **`--plain`**：逐行问答的旧界面保留给脚本和无终端环境（没有 TTY 时自动启用），行为不变。
- **面板：resize 之后补发 `SIGWINCH`**。PTY 的子进程没有 controlling terminal（没有
  `TIOCSCTTY`），所以 `TIOCSWINSZ` 只改尺寸、内核没有前台进程组可通知——已经在跑的 TUI
  永远按旧宽度画。重新 attach 到一个更窄的窗口时，TUI 画得比终端宽，每一行都被折行，
  屏幕上就是越滚越乱的「刷新问题」。现在 `PosixPty.resize` 在 ioctl 之后自己
  `kill(pid, SIGWINCH)`；客户端也在 `ready` 之后再补发一次尺寸作为兜底。
- 验证：客户端 `gofmt` / `go vet` / `go test ./...` 通过（新增 `ui` / `store` / `app` 的
  按键解码、排版宽度、配置读写、错误文案等用例）；5 个平台交叉编译通过；真二进制在 pty 里
  对着假面板跑完 42 项端到端检查（登录失败重试、四级菜单回退、attach/detach、agent 退出码、
  第二次启动免密、Ctrl-C 退出）。

## v0.5.4 — 2026-10-23 · 终端客户端 hdsl-tui

Web Shell 多了一个官方终端客户端：手机 SSH、服务器 console、没有图形界面的环境也能接进
实例的原生 TUI。它只调用面板现有的 REST 与 WebSocket，不新增端口、不新增鉴权面。

- **客户端**（`client/hdsl-tui`，Go）：登录 → 开发者密码提权 → 选实例 → 打开终端；二进制帧
  直接对接 PTY。Unix 用 `SIGWINCH`、Windows 轮询窗口尺寸；Windows 下自动打开 VT 处理与
  UTF-8 代码页，退出时恢复。`Ctrl-] q` 断开而 agent 继续跑，`Ctrl-] Ctrl-]` 发送字面量，
  `Ctrl-C` 原样送给 agent。
- **分发**：与面板同版本号发布。Release 附带
  `hdsl-tui-<版本>-<os>-<arch>[.exe]`（linux / darwin / windows × amd64 / arm64）、含 vendor
  的源码包、`SHA256SUMS-client.txt`，以及 `scripts/install-hdsl-tui.sh` / `.ps1`
  一键下载校验安装；`--build` 可用本机 Go 离线编译。
- **依赖源码随仓库提交（vendor）**：构建全程 `GOPROXY=off`，CI 里有一条专门的离线构建步骤
  作为证明；服务端与客户端都不需要联网取依赖。
- **协议成文**：新增 [docs/terminal-protocol.md](docs/terminal-protocol.md) 固定 REST/WS 契约；
  `ready` 帧新增 `protocol` 与 `brand`，打开终端的响应也带 `protocol`。
- **Windows 面板不变**：终端仍只开在面板所在机器的桌面上，客户端收到 `mode=desktop` 时打印
  服务端给的提示并退出（远程 PTY 需要 ConPTY，另议）。
- 验证：服务端 **684 用例 / 0 失败 / 2 跳过**；客户端 `gofmt` / `go vet` / `go test ./...`
  通过，`GOFLAGS=-mod=vendor GOPROXY=off go build` 与 5 个平台交叉编译通过（Go 1.24.4）；
  真面板 + 假 kimi TUI 的端到端（PTY 字节、detach/reattach 回放、agent 退出码、
  `web_shell_disabled` 与 `insecure_transport` 两种拒绝）跑通。

## v0.5.3 — 2026-10-23 · 移除 SSH 终端通道

SSH 通道整个拿掉，Web Shell 成为面板里唯一的 TUI 入口。这不是「再关一个开关」，而是把
协议栈、认证面和监听端口一起从二进制里删掉。

- **为什么删**：TUI 的需求（手机或终端里接回实例、恢复会话）已经由 Web Shell 覆盖；
  SSH 是面板里唯一自带协议 CVE 历史的组件，还叠着公钥/口令/开发者密码三套认证和一套
  自研门禁。留着它只有负担。
- **删了什么**：`org.jackhuang.hmcl.web.ssh` 整包（服务、门禁、菜单、公钥、会话目录）、
  `SshBridge`、`/api/ssh/*`、设置页「SSH 终端」整页、`HDSL_SSH_ENABLED` /
  `HDSL_SSH_BIND` / `HDSL_SSH_PORT`、`server.yaml` 的 `ssh:` 段、Apache MINA SSHD 与
  eddsa 依赖、`docker-compose.yml` 的 3022 端口映射、`-PwithSsh` 构建开关与
  `-full.jar` 产物。环境体检不再有 SSH 两行。
- **没变**：开发者密码与 30 分钟提权、Web Shell 的打开/回放/停止语义、实例级互斥、
  审计日志（web / terminal 两个通道）。
- **设置页换页**：「SSH 终端」整页改为「开发者模式」，进入同样需要开发者密码（30 分钟
  滑动）。页内有两个控件：Web Shell 总开关（写 `server.yaml` 的 `web_shell.enabled`，
  也可用 `HDSL_WEB_SHELL_ENABLED` 覆盖；关掉后终端按钮隐藏、接口返回 403
  `web_shell_disabled`，已运行的终端不受影响）和修改开发者密码（当前开发者密码 +
  面板密码确认，成功后所有提权立即失效）。
- **开发者模式与终端强制 HTTPS**：两者只在主连接器此刻讲 TLS 时可用。HTTP 下开发者模式
  直接弹「当前环境不安全」并指引到 HTTPS 设置，终端按钮隐藏、接口返回 403
  `insecure_transport`。没有证书时可以生成自签名证书（`POST /api/settings/tls/self-signed`，
  同端口热切换，浏览器会告警）；「HTTPS 证书」页也加了红框说明和同一个按钮，已配置真实
  证书时后端拒绝回退到自签名。
- **反代终结 TLS 也认**：`https.trust_forwarded_proto`（`HDSL_TRUST_FORWARDED_PROTO`，
  默认关）打开后，`X-Forwarded-Proto: https` 即视为 HTTPS，开发者模式、终端和 `Secure`
  会话 Cookie 在 nginx/Caddy 后面照常工作。默认关闭是因为直接连到 HTTP 端口的客户端可以
  伪造该头；这种部署下生成自签名证书会被 409 `tls_terminated_upstream` 拒绝。
- **升级说明**：旧 `server.yaml` 里的 `ssh:` 段会被忽略并在启动日志里打一条
  `unknown key` 警告，不需要手工删除；3022 不再监听；以前下载的 `-full.jar` 不再产出，
  从源码构建也只需要 `./gradlew build`。

## v0.5.21 — 2026-10-23 · 终端空白屏与连接稳定性

- **opencode 终端永远空白**：xterm 6.0.0 的 `requestMode` 在 esbuild 压缩后把
  `return f=…, p=…; var f,p` 改坏，DECRQM（`CSI ? Ps $ p`）抛 `ReferenceError`，
  xterm 的写入队列停摆——WS 已 attach、字节也到了浏览器，画面却不再解析。构建改为
  `minify: false`（体积 +约 70 KB gzip），供应商代码原样执行。
- **终端窗口反复重连**：父组件每次渲染都传入新的 `onClose`，挂载 effect 的依赖因此
  不断变化；改用 ref 兜住回调，并给每次 `openSession` 加 generation 丢弃过期结果。
- 其余可见性修复：WS 打开后补发真实尺寸（PTY 不再停在 80×24）、WS 错误与异常关闭
  不再静默、标题栏显示「已接收 N KB」、Blob 帧兜底；后端补 attach/发送失败日志。

## v0.5.2 — 2026-10-22 · Web Shell：实例级原地终端

实例上的「终端」按钮现在就地打开该实例的原生 TUI（`kimi` / `opencode` / `dsh tui`），
走现有 WebSocket，不新增端口、不引入 SSH 协议栈。终端以实例为单位启动：同一份 home、
账号与模型路由，TUI 里的会话记录与网页模式一致。

- **终端核心**：常置包 `web/terminal`（`TerminalTarget`、`BrandTui` + `KimiTui` /
  `OpencodeTui` / `DshTui`、`TerminalSession`、`TerminalManager`）。PTY 用 JNA 自写
  （`openpty` + `posix_spawn`，不 fork、无 JNA 回调），每个终端一个进程组；子进程退出后
  先关 master 再杀进程组，屏幕不可能再出现新内容。
- **会话**：关窗/刷新只 detach，agent 继续跑；重开接回同一会话并回放最近 256 KB；
  `/exit`、底栏「停止」、实例停止才杀进程树。同一实例单会话。
- **授权**：打开终端需要会话 + 开发者密码提权（15 分钟滑动）；Windows 桌面启动额外要求
  `{"confirm":"open-desktop-terminal"}` 并记审计。入口等价于 RCE，威胁模型见
  [docs/web-shell.md §4.1](docs/web-shell.md)。
- **Windows**：不做 ConPTY，直接在桌面会话里开控制台；`reveal`（home/workspace/log）用
  `explorer` / `notepad`，路径由服务端按实例记录计算，只接受三个目标名。面板以服务运行
  时没有交互桌面，桌面功能返回 409。
- **构建**：SSH 改为 opt-in——默认 jar 不含 `web/ssh`、MINA 与 eddsa，`-PwithSsh` 才带上；
  日常 TUI 用 Web Shell。
- **前端**：xterm 覆盖层（桌面）/全屏（手机）、底栏启停、断线提示与重连；`compat` 闸门
  给 es-check 加 `--module`（动态 import 产生的 chunk 是 ES module）。
- 验证：**725 用例 / 0 失败 / 2 跳过**（JDK 21 与 17，默认构建）；默认 jar `web/ssh=0`、
  `web/terminal=28`、MINA=0；`-PwithSsh` jar `web/ssh=21`、MINA=1173；前端
  typecheck / build / compat 通过。

## v0.5.1 — 2026-10-22 · SSH 门禁与提权加固（v0.5.0 审阅 P1）

- **门禁每次错答都计数**：此前只有「一条连接错满 3 次」才给节流记一条，客户端错 1–2 次就
  挂断可以无限重连白拿猜测，15 条的封禁永远攒不满。现在每次错答进同一个全局预算：15 次
  花光后，第 16 次起门禁封 30 分钟，且**换 IP 也一样**（预算是单个全局键）；同时把触发者
  的地址加入 SSH 登录拒绝名单 30 分钟——公钥、口令都不放行，审计 `ssh.auth-blocked`。
- **提权按会话而不是用户名**：`elevations` 的键改成会话 token，登出/撤销会话即失效，
  每次敏感调用还会确认 token 仍存活。此前同一账号的任意会话共享提权，登出也不清，偷到
  任意网页会话的人只要真用户在 15 分钟内提过权，就能靠一次合法提权无限续期。
- **开发者密码的二次确认接上预算**：`/api/ssh/dev-password` 与 `.../clear` 用面板口令
  二次确认时走 per-IP `LoginThrottle`（15 次失败后拒 10 分钟，429 + `Retry-After`）。
  此前这条路对面板口令是没有预算的 oracle，每次还白烧一次 PBKDF2。
- 验证：全量 **698 用例 / 0 失败 / 0 错误 / 2 跳过**（JDK 21 与 17 各一遍）。新增 4 条
  单测（单答挂断仍计数、封禁对换 IP 生效、提权不跨会话、二次确认锁定）与 1 条真 SSH
  集成测试（花光预算后，同一地址用有效公钥也登录不上）。

## v0.5.0 — 2026-10-22 · SSH 终端通道（M0–M4）

`ssh -p 3022 hdsl@主机` → TUI 门禁（独立开发者密码）→ 编号菜单（dsh 实例 / 品牌实例 /
恢复 dsh 会话）→ 选定项的 `dsh tui [--resume]` 终端直接接管当前 SSH 会话。该通道已在
v0.5.3 整体移除（原因是 Web Shell 已覆盖同一需求，且 SSH 自带协议 CVE 面）。

- **审计设施（M0）**：append-only JSONL 审计日志（`/data/logs/audit.jsonl`，0600、按日轮转），
  事件 `{ts, actor, channel, action, object, outcome, ip}`；登录/实例/TLS 等既有操作全部接线。
  server.yaml 写入收紧 0600。
- **SSH 服务骨架（M1）**：嵌入式 Apache MINA SSHD；`ssh.enabled` 默认关（不开关 = 无监听
  端口，有集成测试钉死）；Ed25519 host key 首启生成、持久化、重启不换 fingerprint；
  平台门（Linux ✓ / macOS 放行标未实测 / Windows 三层封禁）；开发者密码（PBKDF2 0600 独立
  存储，网页开发者模式提权 + TUI 门禁双角色）；authorized_keys 管理（粘贴上传/服务端生成
  密钥对、私钥一次性展示永不落盘）；HTTPS 前置守卫 + 15 分钟滑动提权；口令登录开关
  （默认关、开带警告、热应用）。
- **TUI 门禁 + 编号菜单（M2）**：SSH 认证后再输开发者密码（错 3 次断开+审计）；三区分区
  菜单全局连续编号；「恢复 dsh 会话」只读 sidecar JSON 拿标题、按 `session.lock` 活性标
  「web 使用中」，不解压 zstd 主日志；实例名/会话标题回显前过 `sanitize()` 控制字符过滤
  （转义注入用例钉死）。服务端无 PTY，密码回显已在提示行标注。
- **TUI attach（M3）**：菜单选定后 spawn `dsh tui [--resume]`——env 注入零拷贝复用
  web 启动链路（`DshLauncher.plan`：账户路由、provider key、实例 Node 运行时、DSH_HOME），
  cwd=实例 home；双向字节泵（虚拟线程），channel 断开 `ProcessTree` 杀整棵进程树；
  TERM/COLUMNS/LINES 透传、window-change 转 SIGWINCH；同 home 单 attach；品牌实例
  attach 明确延后（回一行说明）；未安装实例友好报错。
- **加固收尾（M4）**：门禁阶段 120 秒空闲超时（审计 `ssh.idle-timeout`）；门禁失败按
  per-IP 跨连接退避（一条耗尽连接 = 一条节流记录，15 条换 10 分钟封禁）；依赖修复
  `net.i2p.crypto:eddsa`（sshd-common 的 optional 依赖不传递，缺它 Ed25519 运行期必炸）
  与 host key 的 i2p provider 对齐（重载路径钉死 sshd 自家解析器，防 BC 解析换 key）；
  `-PnoSsh` 构建变体（shadowJar 剔除 ssh 类与 MINA/eddsa，`SshBridge` 反射加载、缺类
  即降级为「无 SSH 能力」并明示）。
- 修复（同分支早前提交顺带，容器内无 JDK、从未被编译器核过）：M0 权限迁移段里 Main
  漏掉的 `IOException` import；SshService/SshApiServlet 漏 catch 的受检异常（`loadKeys` 的
  `GeneralSecurityException`、`getInstance` 的 `NoSuchAlgorithmException`）、`audit()` 少传
  实参、POST 分支把 `switch` 语句直接挂在 `->` 后面。另外两处（`ApiGuardFilter` 的 rethrow
  越界、`HdslServer` 缺 import）在 v0.4.8 已修并发过版，与本分支无关。
- 验证：全量 **694 用例 / 0 失败 / 0 错误 / 2 跳过**（JDK 21 与 17 各跑一遍）；前端
  typecheck / build / compat 三道闸门通过；`-PnoSsh` 的 jar 里 SSH 类与 MINA/eddsa 均为 0，
  起服务 `/` 回 200 且日志明示「此构建不含 SSH 终端通道」。合并时另修两处：attach 的字节泵
  改走 `VirtualThreads.start`（原 `Thread.startVirtualThread` 是 21 API，`--release 17` 下
  编译不过）；`-PnoSsh` 不带值时被读成 false，文档里的命令实际会打出全量 jar。

## v0.4.9 — 2026-10-21 · 前端健壮性与动画体验

- **ErrorBoundary**：任何子组件的渲染/生命周期崩溃都被拦成可恢复的错误页
  （出错信息 + 刷新按钮），不再整棵 React 树卸载白屏。已登录区（Outlet）与
  登录/初始化页各挂一道。
- **旧页退出动画**（ui-spec §11 补票）：路由切换走 View Transitions API——
  旧页整页快照 100ms 淡出 + 左移 30px，新页沿用 page-in 入场。不认识该 API 的
  浏览器（Firefox 115 在列）自动维持原状，只有新页入场。
- **断网提示条**：浏览器网络断开时贴顶显示红色提示（`offline-banner`），
  恢复即消失。检测的是浏览器网络状态；面板服务器不可达仍由各请求报错承担。

## v0.4.8 — 2026-10-21 · API 契约与安全小修一批

- **未捕获异常不再撕毁 API 契约**：`/api/*` 新增 `ApiGuardFilter`，servlet 漏出的
  任何异常（比如一个 NPE）统一回 `{"error":"internal error"}` JSON 500——此前 Jetty
  的默认 HTML 错误页会让前端拿到不符合契约的响应。已提交的响应无法重写，照旧上抛。
- **`server.yaml` 收紧为 0600**：写入时 staging 文件先设 owner-only 权限再原子移动。
  该文件里躺着 PKCS#12 密码，此前是 umask 默认的 0644——同目录其他敏感文件全是
  0600，唯独它 world-readable。
- **任务历史有界**：`TaskService` 终态任务（done/failed）最多保留 400 条，超出按
  提交顺序淘汰最旧；运行中与等待审批的任务永不淘汰。此前任务表随运行时长只增不减，
  长跑的面板内存缓慢上涨。
- **插件工具栏按钮文案与行为对齐**：「打开配置文件夹」按钮实际跳转插件市场下载页，
  文案改为「去插件市场」（`dsh.instance.plugins.to_market`）。

## v0.4.7 — 2026-10-20 · 上游 P0 移植 + 网页版差距首批 + 登录防爆破

上游 HDSL 分叉后把包名改成 `com.mcxcc303.hdsl.*` 又把单体类拆成主题包，cherry-pick 的路径
已经断了；这一批全部是**逐笔手工移植**。类名对应表、每月同步流程、已移植与不移植的清单见
[docs/upstream-sync.md](docs/upstream-sync.md)。

- **解压越界（安全）**：归档条目名整条预检——含 `..` 段、`\` 或 NUL 的条目不进解压树
  （`03d9f34`，只记一条 warning，归档其余部分照常解压）；插件包成员 normalize 后必须仍在插件
  目录内，否则记录条目名并让整步失败（`43401ca`）；Node 运行时解压里的软链接只在「非绝对路径
  且解析后仍落在 root 之内」时重建（同笔）——`bin/npm -> /etc/passwd` 这种链接一旦建出来，
  之后每次读它都等于把那个文件读进来。
- **设置文件不再被截断**（`8760af8`）：写进同目录临时文件 → 收紧权限 → `ATOMIC_MOVE` 顶替，
  不支持原子移动的文件系统降级为普通 move。此前是先截断再写，读到中间态就是「没有账号、没有
  API Key」。
- **实例目录四处**：改名不再删除占用新名称的目录（`8062b6e`）；创建不再认养已存在的目录
  （`66415a6`，web 侧 `uniqueId` 配套改成「目录存在也算占用」，否则删除中断留下的目录会让创建
  从「自动加后缀」变成失败）；运行中/停止中的实例拒绝改名（`4842f45`）；复制到自身被拒绝
  （`e3ec345`）。
- **整合包导出**：不再带走 `link:`／目录安装的插件（`08e72c3`＋`fdc8349`＋`ad517a1`）；导出前
  先把这类插件逐个点名（stage 行直接显示在导出弹窗里），拒绝消息也改成说清原因。
- **插件适配判定对齐 npm 的 semver**（`9b47a36`＋`8b56869`）：`workspace:^|~|*`、连字符区间、
  `~>`、运算符与版本之间的空白、`v` 前缀与 `+build` 不参与比较、`includePrerelease` 语义，以及
  harness 自身版本补进依赖表。判定用 node-semver 自己的语料钉住（167 条用例 + 62 条「按设计
  不同」的清单，两个方向都钉）。
- **安装要求 pnpm 10.9**（`e146c5b`）：门槛只落在 dsh harness 的安装路径与体检显示上。品牌区
  与 dsh 共用 `DshNodeRuntime`，所以门槛**不进** `canManagePlugins()`——品牌安装行为不变。
- **命令有界运行**（`8d17e2e`）：`DshCommand` 新增带期限的 `run`、强杀整棵进程树的 `killTree`
  与 `Result.timedOut`。本项目今天没有需要它的调用点（上游的动机是桌面版新增的端口占用探测），
  所以这一笔只落能力与测试。
- **登录防爆破**：同一来源地址五分钟内 15 次失败之后，第 16 次起拒绝十分钟（响应带 `Retry-After`），
  期间口令正确也一样进不来，一次成功即清零。预算没花完之前页面上不提示任何计数，只有真的被挡住时
  才弹出来（表单里留一条 + 一条会消失的提示）。拒绝发生在核对口令之前——核对口令本身就是攻击者想
  花掉的开销。时间由调用方传入，所以窗口边缘、拒绝的最后一秒都在单元测试里钉住，不必等十分钟。
- 验证：**636 用例 / 0 失败 / 0 错误 / 1 跳过**（跳过的是 Windows 专有的 junction 用例）；前端
  `typecheck` / `build` / `compat` 三道闸门通过；品牌区与 `/i/*` 反代目录未改动（diff 为空），
  也不引用本批改过的领域类。

### 网页版与桌面版的差距（首批）

- **整合包安装的进度弹窗加取消**：此前「进行中」分支只有进度条和一行文字，而弹窗在安装期间
  拒绝关闭——点下安装就只能看着，选错了版本也没有退路。现在接 `POST /api/tasks/{id}/cancel`
  （实例详情页与主页早就在用），取消之后显示「安装已取消」，而不是把后端那句英文 `cancelled`
  当故障报出来。
- **版本行的上游发布页按钮**：桌面版版本行上的那颗地球（`DownloadPage.java:423`）。地址形状与
  桌面版一致（`…/releases/tag/dsh-v<版本>`）；列出来的版本来自 npm，上游若没打过对应的 tag
  会落到 GitHub 404，要换成 npm 页面就改 `web/src/constants.ts` 一处。
- **实例名称输入加清除钮与空名提示**：此前要点「创建」之后才 toast 一句「名称不能为空」。
  桌面版那两个对话框（非法字符、重名）没有照搬——实例目录是后端从名字派生的 id，`uniqueId`
  会净化非法字符并在重名时自动加后缀，这里没有对应的失败模式。
- **窄屏标签条的横向滚动**：手机上滑不动、桌面 DevTools 窄窗里用鼠标拖也滑不动。实测这条栏本身是
  可滚的（`scrollWidth 708` 对 `clientWidth 378`），问题在手势归属与「鼠标没有横向平移」：加
  `touch-action: pan-x` + `overscroll-behavior-x: contain`，去掉 iOS 13 之前那条
  `-webkit-overflow-scrolling: touch`（已被移除、留着会和原生惯性滚动打架），并给鼠标加了拖动平移
  （只接管鼠标、只在窄屏；且只有真的滚动了才吃掉那次点击，手抖几像素的正常点击照样点得开）。
- **开发服务器上的两级深链**：`npm run dev` 下打开或刷新 `/instances/<id>`、`/instances/new` 会整页
  被错误边界接住（响应是 `200 /instances/api/auth/whoami`——相对路径按当前地址解析，落到 vite 的
  SPA 兜底页）。模板补一条根挂载的 `<base href>`；服务端注入的那条仍排在它前面，`/panel` 子路径
  挂载不受影响。
- 这几条都过过真东西：标签条用 playwright 在 390×844 下真触摸与鼠标各拖一次（`scrollLeft` 0→330 /
  0→308）、点标签仍能切换；移动端信息架构跑了仓库自带的 `scripts/mobile-acceptance.py`（11 条 PASS，
  剩 1 条需要存在一个品牌实例）；防爆破在临时面板上用真 HTTP 与真浏览器验过（第 16 次 429 +
  `Retry-After: 600`，被拒期间对的口令也 429，页面上两条提示）。**仍未在真机上点过**——这台机器没有
  iOS / Android 设备，也没有浏览器可以直接点。

## v0.4.6 — 2026-10-19 · 最低运行时降到 JDK 17

- **最低运行时降到 JDK 17**：jar 用 `--release 17` 编译（class 61），OpenJDK 17 可以直接
  `java -jar` 启动；21+ 照常。为此补了三处兼容：
  - 虚拟线程（`Executors.newVirtualThreadPerTaskExecutor`，4 处）收进
    `util/platform/VirtualThreads`：21+ 反射用虚拟线程，17 回退 cached daemon 线程池；
  - `HttpClient` 的显式关闭（7 处）收进 `util/io/HttpClients`：21+ 反射调用 `close()`，
    17 上 no-op（连接池由 GC 回收）；
  - `List.reversed()`（1 处）改为反向下标循环。
- **CI 加 JDK 17 矩阵**：build 任务在 17 与 21 上各跑一遍 SPA 构建、兼容闸门与全量测试。
- 验证时顺手修掉一个构建状态竞态：`ZcodeBuilder` 原先在清理 scratch 目录**之前**就把状态
  置为 DONE，测试（以及任何看到 DONE 立刻读目录的调用方）会和删除动作抢时间；现在清理
  完成后才发布 DONE。
- 镜像仍内置 OpenJDK 21（17 字节码在 21 上照常跑），Dockerfile 不变；README 的
  「JDK 21+」改为「JDK 17+」。

## v0.4.5 — 2026-10-19 · 浏览器兼容底线：Firefox 115 ESR（Win7）

- **声明兼容底线**（`web/package.json` 的 `browserslist`）：Firefox 115、Chrome/Edge 109、
  Safari/iOS 15.6——Windows 7/8.1 上能用的最后一条浏览器线。`npm run typecheck` 并入
  SPA 构建。
- **CI 兼容闸门**（`build.yml` 的 Compatibility gate / `npm run compat`）：
  `es-check es2020` 钉住产物 JS 语法；`web/scripts/compat-check.mjs` 扫描产物 CSS，
  把 `:has()`（FF121+）、`text-wrap`（FF121+）、`[popover]`（FF125+）这些
  Firefox 115 不支持、Vite/esbuild 又不转译的特性挡在构建前。
- **复核修正**：最初的排查稿写成「`env()` Firefox 136 才支持、115 会整条作废」，并据此
  给 `mobile.css` 加了 5 处常量兜底和一条 env() 配对闸门。MDN browser-compat-data 证实
  `env()` 与 `safe-area-inset-*` 自 **Firefox 65 / Chrome 69 / Safari 11** 起支持——115
  不是缺口。兜底声明与配对闸门已撤掉，文档改为真实口径。
- **责任边界**（`docs/compatibility.md`）：底线只覆盖面板自身；`/i/<id>/` 反代里的
  dsh / Kimi / OpenCode / Claude Web / ZCode 页面是上游产物，兼容性归上游。

## v0.4.4 — 2026-10-18 · ZCode 在 Windows 上能一键构建了

- **ZCode 面板内构建在 Windows 上跑通（v3.14.3）**：缺的不是编译器，而是三样非编译器的东西——
  Node 24+、pnpm、以及一个能正确处理 `C:\` 的 `tar`。构建前由面板自动准备：优先镜像
  `/opt/node24`，其次托管运行时，缺失则装进 `<data>/hdsl/runtimes`（与设置页同一个目录）；
  运行时没有 pnpm 就用它的 npm 装进去；Windows 写 `<data>/zcode/build/bin/tar.bat` 指向
  `System32\tar.exe` 并前置 `PATH`（Git 的 GNU tar 会把盘符当远程主机）。
- **Windows 上 pnpm 改 `node-linker=hoisted`**（品牌安装与 ZCode 构建）：isolated 布局下
  kimi-code 启动即 `ERR_MODULE_NOT_FOUND(ws)`、opencode 的 postinstall 拿不到平台包。
  POSIX 仍用 isolated。
- **清理 pnpm 树**：Windows 用 `robocopy /MIR /XJ`（`rmdir /s` 在 2 GB 树上十分钟未完），
  junction 目标消失不再让构建失败；`current` 指针按链接/文件分别删除，绝不走进它指向的发行包。
  删除不干净时明确失败，不把旧树留给下一次安装。
- **品牌安装日志**此前从未写入 `install.log`，改为逐行落盘并 flush。
- 复核时补的护栏：`samePath` 改用 `Files.isSameFile`（Windows 的大小写/8.3 短名/`\\?\`
  前缀不再影响「当前版本」判定）；构建工具链在日志里写明来源；并发安装 Node 的竞态转化为复用。
- 测试 **584 / 0 失败 / 0 错误 / 0 跳过**（新增 3 条：文本指针的 current 判定、清理指针不碰发行包 ×2）。
- **真机复验（2026-10-18）**：同一台 Windows 10 上从空数据目录连跑两次（全新 + 重建）都成功；
  Node 26.12.0 与 pnpm 自动装进 `<data>/hdsl/runtimes`，`tar.bat` shim 生效，实例反代 200。
  `cpu-features`（ssh2 的可选依赖）在没有 MSVC 时会报 `Unable to detect compiler type`，
  pnpm 按可选依赖继续——**不需要编译器即可构建**；`node-pty` 用预编译。详见
  `docs/report-2026-10-18-zcode-windows-build.md` §8.1。
- **编译链预检**：Windows 构建前用 `vswhere` 找 `cl.exe`；没有就明确说明
  `cpu-features` 是可选依赖、会被跳过，并给出最小 VS Build Tools 命令。面板不自动下载
  编译器（需管理员 + 2.5–3 GB），也不在构建里弹 UAC。

## v0.4.3 — 2026-10-18 · 容器镜像加 `linux/arm64`

- **容器镜像加 `linux/arm64`**：CI 现在产 `linux/amd64` + `linux/arm64` 双架构 manifest，
  `docker pull` 会按机器自动选择。Dockerfile 的 web/build 两个阶段钉在 `$BUILDPLATFORM`
  （产物与架构无关），只有 runtime 阶段走 QEMU 目标架构；Dockerfile 本身早已按
  `dpkg --print-architecture` 选 Node tarball，这次只是把发布侧补齐。
- jar 与 v0.4.2 相同，没有代码改动；本版只动交付（CI、镜像、文档）。

## v0.4.2 — 2026-10-17 · Windows 裸部署：从「文档说可以」到「真能跑」

> 起因：`docs/windows-bare-deploy.md`（同日只读排查）证明 README/CHANGELOG 把 Windows
> 裸部署当「已加固」，而代码里**没有任何一处**针对 Windows 的平台分支——v0.3.6 那份实测
> 只覆盖「清空 PATH 的容器内」。本版在真 Windows 10（JDK 21 + Node 24）上逐层补齐，
> 并按排查稿的 §6.1 落单元测试（`NodeArtifactNamingTest` / `RuntimeLayoutTest` /
> `ExecutablesTest` / `DshCustomCommandsTest` / `DshVersionManagerRollbackTest`），
> §6.2 的真机清单逐条跑过。

### 为什么：真机基线（不是推测）

- 在那台机器上跑全量测试：**559 用例 / 31 失败**（文档基线是容器内 527/0/0）。31 个失败
  归到 7 个根因，其中 **15 个出自同一个**：`DshNodeRuntime.which()` 按精确名字找文件，
  而 Windows 上 `node` 是 `node.exe`、`npm` 是 `npm.cmd`——一个都找不到，于是面板判定
  「本机没有 Node」，把用户推向那个坏掉的托管运行时，**连手动装官方 Node 都白装**。
- **项目自己的构建在 Windows 上就跑不起来**：`build.gradle.kts` 用
  `File(it, "npm").canExecute()` 判断 npm 可用——Node 安装包同时放了无扩展名的 `npm`
  （POSIX 脚本，文件存在所以判断为真）和 `npm.cmd`，紧接着 `Exec("npm")` 必然
  `CreateProcess error=2`，`./gradlew test` 直接 BUILD FAILED。
- **`.cmd` 不需要 `cmd /c` 包壳**（推翻了排查稿 §4.3 的建议，实测 JDK 21 + Windows 10）：
  裸 `npm` → error 2；**裸 `npm.cmd` 与全路径 `npm.cmd` 都能跑**；无扩展名的全路径 `npm`
  → error 193。正确的修法是**把扩展名补对**，不是套 `cmd.exe /c`——后者会引入参数二次
  转义（那正是 `CommandBuilder.toBatchStringLiteral` 存在的理由），前者参数原样透传。

### 改了什么

- **L4 派生解析**（新增 `util/platform/Executables`）：`resolve()` 按 `PATH` + `PATHEXT`
  补全可执行文件——Windows 上优先 `.cmd`，无扩展名的文件退到最后（那是一份 POSIX 脚本，
  `CreateProcess` 会以「不是有效的 Win32 应用程序」拒绝）；`startable()` 把 argv[0] 补成
  能启动的形式、参数一字不动。接入 `DshCommand.run`（所有 npm/node/pnpm 的唯一漏斗）、
  `BrandInstaller`、`ZcodeBuilder`、`ZcodeRuntime`、`Main.toolVersion`、`SystemUtils`、
  `DshSessions`、`NodeRuntimeManager`；`DshNodeRuntime.which()` / `SystemUtils.which()` 改走它。
  品牌实例的入口（`node_modules/.bin/<tool>`）与 `tsx` 同样补全到 `.cmd`。
- **L3 运行时布局**（新增 `dsh/RuntimeLayout`）：POSIX（`bin/node`、`bin/npm`、`bin/` 进
  `PATH`）对 Windows（根目录 `node.exe`/`npm.cmd`/`npx.cmd`/`pnpm.cmd`，根目录进 `PATH`）。
  `NodeRuntime`、`DshNodeRuntime.fromManaged`、`NodeRuntimeManager`、`NodeApiServlet`
  全部改走它。可执行判定按平台分开：**POSIX 仍然要求执行位、答案仍然是 realpath**
  （`Executables.startableAmong`，`node_modules/.bin/tsx` 这类软链照旧解析到目标）——这一半
  不因为 Windows 适配而改变；Windows 上 NTFS 没有执行位，改按扩展名 + `PATHEXT` 补全，
  并且**不套 `cmd /c`**（`CreateProcess` 会自己把点名的批处理转给 `cmd.exe`，套壳反而多一层
  转义；实测见下）。
- **L2 发行物解析**：`platformTag` / `archiveExtension` / `indexTag` 补 `win-x64` / `win-arm64`、
  `.zip`、`win-x64-zip`，并改成**接收 `Platform` 参数的纯函数**——于是 Windows 的产物名
  在任意机器上都能被测试钉住，不需要 Windows CI；解包加 zip 分支（Windows 包没有软链，
  但加条目越界拒绝）。
- **L5 文件系统语义**（新增 `util/io/Links`）：探测符号链接能力（试建一次临时链接，结果
  缓存），不可用时按用途降级——解包与复制里的软链（`bin/npm`）**改成复制**；`current`
  这类**指针改成写一个文本指针文件**，读侧两种形式都认，所以同一目录从一台机器拷到
  另一台仍然可读。`ATOMIC_MOVE` 补普通 move 兜底（跨卷时 Windows 直接拒绝）；
  删除/改名加短暂重试（杀软、索引器、刚退出的子进程都会占用几毫秒，一次失败就报
  「安装失败」太重）——重试落在共享的 `FileUtils.deleteQuietly` 上，实例安装清旗标与
  运行时安装清暂存目录用的是同一份，不再一个有重试、一个没有。
- **实例安装不再「先暂存再搬」**（真机实测挖出的 Windows 专属致命项）：原来 pnpm 在
  `dsh.installing/` 里装完，再把整棵树 `Files.move` 到 `dsh/`。POSIX 上 pnpm 写的是**相对**
  软链，搬了也没事；Windows 上 pnpm 用的是 **junction**，其目标**必须是绝对路径**——于是搬过去
  的树里每一条链接都还指着刚被删掉的 `dsh.installing/`，`package.json` 读不出来，面板报
  「pnpm reported success, but the installed version could not be read back」。现在 pnpm 直接写在
  实例最终要用的路径上，`dsh.installing` 改为「还在写」的旗标（`isInstalled()` 见到它就答「没装」，
  `discardPartial()` 会连同半成品目录一起清掉），中断的安装仍然不会被误当成可用版本。
  **就地安装不能把「失败不毁旧版」这条保证丢掉**（旧流程靠暂存目录天然做到，且这一条对所有
  平台都重要）：安装前把已有的树**挪到 `dsh.previous`**，pnpm 失败或产物不全时删掉半成品、
  **把旧树挪回原路径**（Windows 的 junction 是绝对目标，只有回到原路径才重新有效），
  发布成功才删备份。`DshVersionManagerRollbackTest` 钉住四种状态（失败/中断/已发布/无备份）。
- **L6 路径与环境**：`ZcodeBuilder` 硬编码的 `":"` → `File.pathSeparator`；
  `DshCustomCommands` 的 `/bin/sh -c` → Windows `cmd /c`（启动前命令失败会**阻止实例启动**，
  这条在 Windows 上等于每个带命令的实例都起不来）；`DshInstanceTerminal` 生成的 bash 脚本里
  `File.pathSeparator` → 字面 `:`（bash 到哪都是 `:`，用宿主分隔符会写出一个只含一个
  超长目录的 `PATH`）；`BrandRuntime` 在 Windows 同时设 `USERPROFILE`——Node 认它不认
  `HOME`，否则实例隔离静默失效、品牌凭据落进真实用户目录。
- **L7 声明层**：`DshDoctor` 加 Windows 专检（符号链接能力、启动器用户目录长度、控制台编码、
  实例终端不可用），并把 inotify 那一段在 Windows 上标为「不适用」；启动横幅加一条终端
  不可用的提示；实例终端在 Windows 上抛**说明为什么**的错并给出三个可用路径
  （工作目录、`DSH_HOME`、运行时 `bin`），而不是「找不到终端模拟器」——那对一台装着
  Windows Terminal 的机器既真实又无用。
- **测试夹具平台化**：两个 fake `pnpm` 改成「一份 `pnpm.mjs` + 每平台一行包装」
  （Windows 那边**必须**有 `pnpm.cmd`，否则面板的可执行文件解析会跳过去、找到真正的 pnpm，
  测试就会去打网络——这正是这个夹具要防的事）；5 处 `Files.getPosixFilePermissions`
  断言收进 `PosixPermissions`（POSIX 上仍然断 0600，Windows 上该断言不适用而不是
  把整个用例跳过）；`DshAcpClientTest` 硬编码的 `/tmp` → 系统临时目录。

### 真机复验（2026-10-17，v0.4.2 候选 jar）

在那台 Windows 10 上把 jar 从零跑了一遍——不是单元测试，是 `java -jar` 裸部署：

- 面板：首启建号、登录、`/api/doctor` 全部通过；横幅 `runtime: node v24.22.0, pnpm 11.28.6`
  （pnpm 不再误报 MISSING）；
- 托管运行时：`POST /api/node/install` 装出 **win-x64 26.12.0**——根目录 `node.exe`/`pnpm.cmd`、
  没有 `bin/`，`node -v` 与 `pnpm -v` 都能跑；
- dsh：`0.2.1-rc.1` 的 626 个包**就地**装进实例目录（`dsh.installing` 只是旗标、装完即消失），
  启动后经 `/i/<id>/?token=…` 反代 **200**、停止后端口无监听；
- 品牌：opencode 1.19.3 的装、启、反代（`<title>OpenCode</title>`）全通；停止后状态停在
  `stopped`、端口释放、`opencode.exe` 无残留（这一条见下）；
- **中文 + 空格**数据目录（`C:\Users\timmy\hdsl-测试 空格`）：装、启、反代同样通过；
- HTTPS：自签证书打开后，裸 IP 同端口 `http://192.168.1.11:3080/` 答 **308** →
  `https://192.168.1.11:3080/`；
- 实例自定义命令：启动前命令经 `cmd /c` 执行，工作目录里的 `pre-win.txt` 写入成功。
- 未复跑：ZCode 面板内构建（耗时长）、kimi 品牌（需要账号）。

### 停止实例必须带走整棵进程树

真机复验挖出的最实在的一个问题：opencode 的 `stop()` 只 `destroy()` 了面板持有的那个
`Process`，真正占着端口的是它派生出来的 `opencode.exe`——面板报 `stopped`，端口却还在
LISTENING，再次启动就撞「port already in use」。新增 `util/ProcessTree`：**先**收集后代
（父进程一死，子进程就被重新挂到系统进程之下，`descendants()` 再也找不到它们），再按
「先礼后兵」的顺序终止；只杀父进程的写法在 Linux 上同样会留下 npm/tsx 的 shim 子进程。
`BrandRuntime.stop()` 与 `ZcodeRuntime.stop()` 都改走它。`STOPPING` 旗标改由输出泵消费，
不再由 `stop()` 提前移除——否则「请求停止」和「泵读到 EOF」会竞争同一个答案，把主动停止
误报成「进程意外退出」。真机复验：停止后 `listeners=0`、`opencode=0`、状态保持 `stopped`。

启动日志也不再为 Windows 上必然发生的 `setPosixFilePermissions` 打一条带堆栈的 WARNING
（横幅已经写明 NTFS 没有 0600 语义，warning 只会像故障）。

### pnpm 配置：实测修正了排查稿的猜测

排查稿写「Windows 的 pnpm 读 `%LOCALAPPDATA%\pnpm\config\rc`」，并标注「需实测确认」。
在 Windows 10 + pnpm 11.28.6 上实测（`pnpm config set` 之后看哪个文件变了）：

| 内容 | pnpm 真正读的位置 |
|---|---|
| registry | `%LOCALAPPDATA%\pnpm\config\auth.ini`，形式 `registry=<地址>` |
| 其余（`storeDir`） | `%LOCALAPPDATA%\pnpm\config\config.yaml` |

`%LOCALAPPDATA%\pnpm\config\rc` 与 `~/.config/pnpm/config.yaml` **都不读**——面板写进
后者的国内镜像因此被静默忽略、一路打到 npmjs.org。`PnpmConfigFile` 现在按平台分派：
POSIX 仍是 `$XDG_CONFIG_HOME/pnpm/config.yaml`（YAML、值带引号），Windows 是两个文件
（INI 给 registry、YAML 给 storeDir），语法由文件名决定。

### 仍然不支持（明确声明，不再只是报错）

- **实例终端**：它开的是 POSIX shell、走的是 Linux 终端模拟器列表，两端在 Windows 上
  都不存在。现在抛的错会说清原因并给出可以照抄的替代路径。要真做就是接 `wt`/`powershell`。
- **`flock` 会话租约**：util-linux 的命令，Windows 没有；面板在 Windows 上直接答
  「观察不到」而不是每次去 spawn 一个必然失败的进程。
- POSIX `0600` 语义（旧限制，未变）：NTFS 无 umask，凭据文件可能被同机其他账户读到。


## v0.4.1 — 2026-10-15 · 开山：品牌实例在实例列表页内原地成层，实例成为一等对象

> 版本号：原计划的 **v0.4 不单独发 release**——本版把「开山」与随后的访问宽容化
> （HTTPS 下裸 IP 也能用）合在一起发。

**「开山」**：品牌区从实例列表页底部那排挤成一堆的页签里整块搬出来，成为**同一页内的第二层**
（左侧选「dsh 实例 / 品牌实例」，右侧内容区原地换），并且第一次真正按**实例**来组织——
不再是一个"装完版本就没下文"的一次性安装工具。

> 路线说明：中途试过把品牌区独立成 `/brands/<品牌>` 一页，最终按"不要第三层跳转"的意见
> 收回——现在是实例列表页内的视图切换：URL 只换查询串（`?view=brands&brand=kimi`），
> 页面不跳转、不重挂载，`/brands/<品牌>` 旧深链重定向到这里。

### 为什么（改之前的实测数据，1440×900，9 个 dsh 实例 + 1 kimi + 1 zcode）

`.page-content` 一列里 dsh 列表与品牌区抢同一块纵向空间：dsh 列表 529px，品牌区 266px——
页签 45 + 警示 30 + 版本条 53 + 已安装行 26 + 实例列表卡 112，而卡里真正要操作的 `list-body`
只剩 **48px**：一行半，最后一行被压到屏幕边缘截断。

版本切换为什么没人用：`PATCH /api/brands/<brand>/instances/{id}` 对未安装的版本直接回
400 `... is not installed`，而行内下拉又**只列已装版本**——"想换个版本"于是变成
先停实例 → 回品牌级装版本 → 再回来切。

### 改了什么

- **实例列表页（`/instances`）**：改成**两层原地切换**——左侧子侧栏两个分区
  「dsh 实例」（instances 文件夹项）与「第三方品牌」（品牌实例项），点哪层右侧内容区就换成哪层；
  切 dsh 层时列表拿到整屏（529px → **795px**）。切换只换查询串，页面不重挂载
  （移动端 `side-category` 隐藏，两层自动变顶部 chip；实测点击后 `page-root` 探针仍在）。
  旧链接 `/instances?brand=x` 与 `/brands/<品牌>` 都落到同一处的品牌视图。
- **品牌视图**（内容区两卡 + 顶部页签行）：页签行是 4 个品牌（带实例数），下面是：
  - 卡片**「实例」**（主卡，吃剩余高度，桌面实测 672~725px、移动端 469px）：新建行
    （**该品牌还没有实例时名称预填品牌名**——Kimi Code / OpenCode / Claude Web / ZCode，
    点一下「创建」就行）、radio（= 启动面板的「当前实例」，与 dsh 实例列表同一套选择）、
    状态徽标、`id · 版本 · 端口/打开地址`，加行内版本选择器与启动/停止、打开、更多
    （信息 / 重命名 / 插件 / 日志 / 删除）。
  - 卡片**「版本」**（辅卡，默认折叠 **69~77px**）：已安装的版本构建 tag + 「安装新版本」。
- **版本切换做到能用**（三条路径，都在行内完成）：已装 + 已停 → 直接 `PATCH`；
  已装 + 运行中 → 确认「停止并切换」→ 停止 → `PATCH`；**未装 → 确认后走品牌级安装
  （行内进度条 + 失败原因）→ 装完自动 `PATCH`**。行内下拉改成**全量版本目录**，
  已装/未装都打标；版本构建仍是品牌级共享（装一次，同品牌实例都能切过去），界面把这层
  心智模型写在卡片里。后端那条 400 校验原样保留（安全网），编排放在前端——
  **不新增版本切换接口**，少一个会腐坏的状态机。
- **插件通道（界面 + 后端各留一手）**：新增 `GET /api/brands/<brand>/instances/{id}/plugins`
  与 `GET /api/zcode/instances/{id}/plugins`，同一形状 `{supported, plugins[]}`；
  能力位来自 `Brand.supportsPlugins()`（现在一律 `false`）。前端只按这个响应渲染——
  将来某品牌有了真插件，后端置 `supported: true` 并填 `plugins` 即可，界面不用改一行。
- **品牌图标（并按法律风险分档）**：页签行、启动面板的跨品牌切换器、主页品牌卡与运行浮层
  都显示各家标志，**dsh 自己的默认图标也从 HMCL 草方块换成 DeepSeek 鲸鱼**——
  但只上**许可清晰且不触商标红线**的：dsh 与 ZCode 取 Simple Icons 的 CC0 文件
  （后者**不用 ZCode 仓库自己那份**：它的 `NOTICE` 对"图标"有笼统保留条款），
  Kimi Code 与 OpenCode 取各自 MIT 仓库里的原文件（单色的走 CSS mask + `currentColor`
  跟主题明暗自适应）。**Claude Web 刻意不上图**：社区仓库的图标就是 Anthropic 的珊瑚色星芒标，
  而 Anthropic 商标条款写明"须事先书面许可、不得改动"。顺手删掉 `InstanceIcon` 里那段
  `grad-a`…`grad-d` 死代码——那四个类在 CSS 里从来没有定义过。
  来源、许可与商标归属见 [NOTICE](NOTICE) 的 TRADEMARKS 一节与 `docs/brands.md`。
- **HTTPS 打开后，裸 IP 也能用**（没有域名、没有反代的部署本来就该这样）：`https.redirect_same_port`
  （默认开）让 **TLS 端口同时接明文**，一律 308 跳到同主机同端口的 HTTPS。浏览器在地址栏里只看到
  `192.168.1.6` 时会按 http 发请求，过去直接撞上 TLS 监听、只报 `ERR_INVALID_HTTP_RESPONSE`
  ——既没页面也没线索；现在自动跳过去，而且内容绝不走明文（非 TLS 请求只得到这个 308，会话 Cookie
  是 `Secure`，明文请求里也不会被带上）。自签证书的 SAN 同时补上**本机所有网卡地址与主机名**，
  `https://192.168.1.6:3080` 从此只是"自签不受信任"这一条告警，不再是"证书主机名不匹配"。
  想要纯 TLS 端口就设 `redirect_same_port: false`（那条老报错的行为有测试钉着）。
- **删死代码**：`BrandsSection.tsx`（页签行 + 移动端手风琴 `BrandSummaryBar` + `EXPANDED_KEY`
  + 深链白名单）整块删除；`mobile.css` 里随之失效的 `.brand-summary*` 规则一并清掉。

### 验证

`tsc --noEmit` 与 `vite build` 通过；全量 **556 用例绿**（新增两条插件通道用例：形状 + 未知实例 404）。
真后端 + 真浏览器（Playwright，1440×900 与 390×844）：dsh 列表 795px、品牌视图版本卡 69~77px /
实例卡 469~725px、行内启动按钮零滚动可达、插件弹窗真调后端、原地切换不重挂载（`page-root` 探针）、
`?brand=` 与 `/brands/<品牌>` 旧链接落到同一处、首个实例名称预填品牌名；**按需安装真跑通**：
选未装 2.1.0 → 真 pnpm 安装 15 秒（行内进度）→ 自动 `PATCH` 切过去 → 切回已装 2.1.1 走直接 `PATCH`。

### 没动的部分（有意）

反代与被反代模块**一行没动**：`InstanceProxyServlet`（页面改写 / 两个 shim 资源 / 路由修正）、
`InstanceProxyTargets`、`BrandRuntime`（进程运行时）、`BrandInstaller`、`BrandInstanceManager`、
`BrandCatalog`、`ZcodeRuntime`，以及 `HdslServer` 的 servlet 装配全部原样。本次后端只新增了
两个**只读** GET 端点，挂在既有路径分支上。（zcode 分支的实例行按钮排版变了：五个平铺图标
收成 启动/停止 + 打开 + 更多菜单——处理函数与弹窗一字未改。）

## v0.3.9 — 2026-10-15 · 品牌实例也有运行中浮层

dsh 实例运行中时主界面有一张信息浮层（名称 + `运行中` + `id · 端口 · /i/<id>/?token=…` + [打开]），
品牌实例（Kimi Code / OpenCode / Claude Web / ZCode）此前**完全没有对应物**：主界面选中品牌目标后，
运行中既看不到端口也拿不到打开地址——启动那一刻自动开的标签页一关，就再无入口。

- `MainPage` 新增 `RunningPaneBrand`：品牌 Tag + 名称 + 状态，运行中显示 `id · 端口 · 打开地址`
  与 [打开]、复制地址；启动中也显示同款浮层（无地址行），补上品牌启动最长 60 秒里主界面的反馈。
  打开规则与手机品牌卡一致：zcode 走 `api.zcodeOpen`，其余直接用后端返回的 url（已带挂载点前缀），
  兜底 `withBase("/i/<id>/")`。
- `refreshExternal` 把后端**一直有返回、前端却丢掉**的 `lastPort` 映射进 `ExternalInstance.port`
  （品牌三件套与 zcode 都带）。
- 无选中目标且没有任何 dsh 实例时，浮层退回第一个运行中的品牌实例——只用品牌实例的用户
  主界面也能直接拿到打开链接。

验证：`tsc --noEmit`、`vite build` 通过；全量 554 用例绿（Java 侧无改动）。纯增量：
不涉及进程运行时、反代或被反代模块的任何代码。

## v0.3.8 — 2026-10-12 · Claude Web 品牌（首个 git 源品牌）

品牌区新增第三个第三方品牌 **Claude Web**（[ypyik0669/claude-web](https://github.com/ypyik0669/claude-web)，
MIT）——社区开源的 Claude Code 可视化工作台（**不是** Anthropic 官方的托管网页版，那是 SaaS，无法自托管）。
与 Kimi Code / OpenCode 同款待遇：装指定版本 → 拉起自带网页端 → 经 `/i/<id>/` 反代（同端口、同证书、
同登录门）；界面上标为**实验性接入、不保证可用性**（社区引擎 + 可选第三方中转，凭据与计费归上游/服务商）。

**`Brand` 由此分化出两种来源**。NPM（现有两品牌不变）与 **GITHUB**：

- 安装：**不走 git**——`github.com:443` 的 git 传输在本机网络下会被拦腰切断（实测 `expected flush
  after ref listing`、GnuTLS 重置、130 秒超时，成功率约三成），而 `codeload.github.com` 的源码 zip
  1.5 秒下完 11 MB。改为 `codeload.github.com/<slug>/zip/refs/tags/<tag>` 下载源码 zip（**3 次重试**）
  → 解包（剥掉 `<repo>-<tag>/` 根目录，含 zip-slip 防护）→ `npm ci -w server -w web
  --include-workspace-root`（只装网页端需要的两个 workspace + 根项目——desktop 的 Electron 二进制
  不下载，`ELECTRON_SKIP_BINARY_DOWNLOAD=1` 双保险）→ `npm run build -w web`。node-pty 等原生模块
  用镜像里现成的工具链编译；engine tgz 随 npm 依赖从 GitHub Releases 拉取。镜像无需任何变动
  （也不需要 `git` 二进制）。
- **`--include-workspace-root` 是必需的**：网页端的运行内核 `claude-web-engine`（ccb，社区构建，
  也是唯一听得懂 OpenAI/Gemini 供应商协议的内核）是**根项目**的依赖而不是 server workspace 的。
  只装两个 workspace 会让它缺席，应用静默回退到官方 Claude Code，于是**所有 OpenAI 类型供应商都
  用不了**（实测 0.2.1：启动行的 `runtime: claude 2.1.283` 就是那个回退）。npm 步骤同样带
  3 次重试与放宽的 fetch 超时（引擎 tgz 是绝对 GitHub URL，同样受网络抽风影响）。
- 版本列表：GitHub REST `/repos/<slug>/tags`（剥离 `v` 前缀、滤掉 `engine-*`/`nightly` 等非发行 tag），
  复用 npm 通道的缓存与降噪逻辑；**空结果永不缓存**——网络抽风时抓空是常态，缓存空列表会让下拉框
  在网络恢复后仍空白 30 分钟（首次上线后「读取不出来」的原因）。
- 启动：`tsx server/src/index.ts` 直跑 TS 源码（无内置入口）；**端口走 `PORT` 环境变量**预分配
  （源码核实 `PORT ?? 3090`，它没有端口参数）——`BrandRuntime` 新增 `LaunchPlan`（命令+环境）
  携带；就绪行 `listening on http://127.0.0.1:<port>`。

**子路径兼容经源码核实、零补丁**：前端 `base: './'`（资源全相对路径，上游为「手机壳跑子文件夹」所设）、
前端与服务器只通一条 WebSocket（标准 shim 改写其绝对 `/ws`）、无 history 路由；WS 握手的 Origin
校验（Origin==Host 或回环）与面板反代一贯的 **Host 透传契约**吻合——不需要给上游提 PR。

验证：新增用例（GitHub tag 解析降噪/排序、源码 zip 解包与 zip-slip 防护、`LaunchPlan` 的 tsx+PORT
组装与缺失入口拒绝、GitHub 品牌安装标记）；全量测试绿；`tsc --noEmit` 通过。生产镜像内实测：
API 取 tags 0.6 秒 200、源码 zip 1.5 秒下完 11 MB、解包 → npm ci（engine 与原生模块齐）→ vite 构建
→ `PORT=<n>` 启动 → 就绪行 `claude-web 0.2.1 listening on http://127.0.0.1:<n>` → GET / 200。
面板经 `/i/<id>/` 的浏览器全链路待人工复验。

### 「打开文件夹」在容器里的绕法

Claude Web 的 `打开文件夹…` 会叫**服务器端的原生目录对话框**（zenity / osascript / PowerShell）——
容器里没有桌面，它静默返回 null，点了没反应（上游设计假定跑在有桌面的机器上，不是面板的问题）。
面板改用它自己的协议：实例就绪后向 `ws://127.0.0.1:<端口>/ws` 发一条 `workspaces.add`，把实例的
工作区目录登记成项目——页面里的「项目」菜单从此直接可选，无需对话框。实测回复 `ok:true` 并伴随
`meta.changed` 事件。

## v0.3.7 — 2026-10-12 · 子路径反代下品牌代理修复（Kimi Code / OpenCode / ZCode）

面板自身被反代在子路径（`HDSL_BASE_PATH`）时，dsh 实例正常但品牌实例全崩：
zcode 显示 "Web Remote Control Failed"、kimi 报 `WebSocket connection failed:
ws://host/ws`。根挂载不受影响（本轮所有改动在根侧均为恒等路径，根/子路径双场景
18+9 项验收全绿）。

- **上游根因**：Kimi Code / ZCode 的 web 端原生认识 `/i/<id>/` 挂载，API base 来自
  `location.pathname.match(/^\/i\/[^/]+\//)`——正则锚在路径开头。浏览器地址是
  `/hdsl-web/i/<id>/` 时检测失配，API/WS 打到源站根（`/api/server-info` 404、
  `ws://host/ws` 被宿主 nginx 404）。
- **修复**：新 `patchZcodeScript` 把检测正则放宽为 `/^.*?\/i\/[^/]+\//`（从路径
  开头非贪婪匹配到第一个 `/i/<id>/`；`match()` 返回匹配子串，只去 `^` 锚会丢
  base 前缀——已实测修正一次）。zcode 走这个最小补丁（品牌那套路由读法补丁会把
  它原生按 `/i/<id>/` 的路由读坏）；kimi 走品牌补丁通道，同一放宽覆盖。
- **打开链接双前缀**：后端已在 REST 边界给 open/list 的 url 统一加挂载点前缀，
  前端两处仍 `withBase(inst.url)` → `/hdsl-web/hdsl-web/i/<id>/` 落回主面板。
  契约钉死：API 返回的 url 直接用，仅本地兜底路径 `withBase`。

验证：锚点逐字节比对真实 bundle（zcode 3.15.1）；新增单测；VM（nginx + 
`HDSL_BASE_PATH=/hdsl-web`）真浏览器 zcode/kimi/opencode 9/9 PASS（地球图标打开
单前缀落点）；全量 528 用例绿。

## v0.3.6 — 2026-10-11 · 裸部署三道防线 + 面板托管 Node 运行时一键供给

围绕「拦不住的 Windows/macOS 裸部署」体验的一轮加固，全部带反例实测：

- **面板托管 Node.js 运行时**：裸部署没装 Node.js 时，「设置 → Node.js」可一键安装
  托管运行时（落 `<data>/hdsl/runtimes`，默认国内镜像源，官方源可选），装完后版本安装、
  插件、品牌工具全部自动回退使用；也保留用户自行安装 Node.js 的权利（手动指引 +
  nodejs.org 链接）。关键坑：npm/pnpm 是 `#!/usr/bin/env node` 脚本，探测与 spawn
  必须把运行时自己的 bin 塞进 PATH，全部调用点已接上。新端点
  `GET/POST /api/node(/install|/uninstall)`，安装走任务系统带进度。
- **启动横幅升级成体检报告**：open 行直接给访问地址并注明首跑建号；data dir 带
  内容清单与备份/别上公网提示；runtime 缺失给两条修复路；Windows 平台单独提示
  0600 无 NTFS 语义、凭据文件可能被同机其他账户读到（README 安全说明同款）。
- **构建期哨兵**：没有 npm 且没有可用的 web/dist 时构建直接报错（"this jar would
  ship without the web frontend"），不再静默产出缺前端的 jar——Windows 上装了 JDK
  没装 Node 时 33 秒"成功"、跑起来全站 not found 的事故根源。
- **运行期可诊断**：jar 缺 index.html 时日志明确写「web/index.html 不在 classpath，
  请用 release jar」，不再只丢光秃秃的 not found。

实测：清空环境变量（PATH 无 node/npm/pnpm）下装一枚托管运行时后版本列表成功返回；
全量 528 用例绿；设置页 UI 断言五项全 PASS。

## v0.3.5 — 2026-10-11 · 审查拆雷（8 颗）+ 死代码清理 + 反向代理挂载支持

一次四路交叉审查定位到 8 颗确认的雷，全部拆除（每颗独立提交、逐个验证）：

- **取消任务只杀自己的进程树**（最危险的一颗）：`MAX_INSTALLS=2` 下取消实例 A 的安装
  会全局杀死实例 B 健康进行的 pnpm。进程改按工作线程归属，新增回归用例（旧行为实测失败）。
- **compareVersions 换用 VersionNumber**：同前缀 prerelease 曾按字典序排
  （`alpha.10 < alpha.2`），且 Node 的 `v20` 前缀被解析成 0。喂给版本选择器、lockstep
  最老判定、插件/Node 排序的比较器从此按 Maven 优先级。
- **Host 头解析收敛 HostHeaders**：品牌 allowlist 的 `[::1]:8080` 曾被砍成 `"["`。
- **过时安全文档对齐现状**：Zcode 类文档/Servlet/API 文档/前端按钮还活在「独立端口
  明文 HTTP」时代，与「回环绑定 + `/i/<id>/` 反代」的实际姿态矛盾——文档撒谎是最阴的雷。
- **移动断点统一**：layout.css 的孤儿 `(max-width:800px),(max-height:490px)` 与
  `MOBILE_QUERY` 对齐，760–800px 窗口不再 JS/CSS 各说各话。
- **插件面板任务归属**：删掉被推翻的 `kind.includes(id)` 启发式（恒 false），改用
  `t.instance` 精确匹配。
- **品牌实例状态三份真相收敛为 `store.external` 唯一源**（删 `zcodeInstances` 双拷贝）。
- **死代码**：DshCli（1266 行零引用）、「浏览」菜单四项死 UI、`api.vendors()` 死端点、
  15 个死图标。

**反向代理挂载支持（面板自身被反代）**：新增 `HDSL_BASE_PATH`——面板架在 nginx/Caddy
子路径（如 `/panel/`）下时，index.html 注入 `<base href>` + `window.__HDSL_BASE__`，SPA
的资产/API/WS/路由链接、`/i/<id>/` 打开地址、实例反代的页面改写/品牌脚本补丁/重定向
Location/Cookie Path 全部按浏览器眼里的完整挂载点重写；反代只需剥前缀转发（与面板反代
dsh 实例同款挂法）。根路径挂载零配置、行为不变。两种挂法均端到端实测（SPA/登录/API/
WS/品牌页/资产全通），README 新增「反向代理」部署章节。

## v0.3.4 — 2026-10-10 · 修复桌面缩窄窗口时品牌摘要条样式失效

v0.3.3 的摘要条/长说明降级样式被误放进了 `(hover: none) and (pointer: coarse)`
（触摸）媒体查询块里——真机（粗指针）正常，但桌面浏览器把窗口缩窄触发移动版式时
（`max-width: 760px` 命中、粗指针不命中）这些样式完全不生效：摘要条退化成块级
堆叠，「全部展开」和「启动」掉到第二行。已把整块移回 `(max-width: 760px),
(max-height: 520px)` 布局查询（与 `useMobileLayout()` 判定同域）。

教训：mobile.css 有两个媒体查询块，加规则时必须认准布局块；验收脚本此前全部用
`has_touch=True`（粗指针）跑，漏掉了细指针路径——已补回归用例 ⑨（无触摸 500px
视口断言摘要条单行、药丸 32px、按钮组右对齐），18 项全 PASS。

## v0.3.3 — 2026-10-10 · 移动端首屏还给 dsh 实例 + 主页品牌实例卡

手机访问时品牌区（ZCode / Kimi Code / OpenCode 三个面板，桌面版式 6~7 个横条原样搬进
390px 全部折行）整块约 678px 高，把 dsh 实例列表挤出首屏——「挡着 dsh 实例管理」是
信息架构问题，不是控件大小问题。同时修掉主页「双真相」：卡片显示一个 dsh 实例、
贴底启动栏操作另一个品牌实例。

### 改了什么

- **品牌面板移动端默认折叠**（最大杠杆）：页签行 + 摘要条并进一张卡，摘要条版式
  `Kimi Code · 已安装 2.1.1 · 1 实例 · 已停止　[全部展开▾] [启动]`——右侧成组：明显的
  「全部展开▾」药丸按钮（secondary-container 底、一眼可见）+ 默认操作按钮（有运行中
  的实例显示「停止」，否则「启动」第一个，行为与启动面板主按钮一致，真实拉起/停掉
  验证过）；左侧文字区点按同样展开。点当前页签（带 ▾ 箭头引导）也可展开收起，
  手风琴天然同时只开一个；展开状态记 localStorage，刷新后页签跟随上次展开的
  品牌。折叠态整块 105px（两个 44px 触控目标 + 分隔线）vs 基线 678px。桌面端一字
  不动。判定与 `mobile.css` / `hooks.ts` 的 `MOBILE_QUERY` 逐字一致。
- **长说明移动端降级**：品牌第三方警示条、ZCode「纯实现性验证……」长文，手机上收成
  一行摘要 + 「详情」展开（新组件 `CollapsibleNote`，桌面端原样渲染）。
- **主页卡片跟随启动目标**：`MainPage` 的数据源从「第一个运行中的 dsh」改成与贴底
  启动栏同一个 `useLaunchEntry()`——品牌实例从此和 dsh 平权：卡片渲染
  [品牌 Tag] 实例名、版本、状态徽标，操作 = 启动/停止、打开、管理，全屏只剩一个
  「当前实例」。细节：品牌实例打开地址就是 `/i/<id>/`（会话门就是门，不拼 dsh 的
  `?token=`）；运行时长 BrandRuntime 不记 startedAt，品牌卡不显示这项（不填假的）；
  地址行截断 + 复制按钮。
- **`/instances?brand=kimi|opencode` 深链**：主页品牌卡「管理」跳实例列表并自动切到
  对应品牌页签。

### 验证

- 新验收脚本 `scripts/mobile-acceptance.py`（headless Chromium 390×844，起静态 dist +
  /api 反代）：首屏可见 dsh 行 4 ≥ 3 ✓、首行启动按钮零滚动 ✓、品牌折叠态 105 < 110px ✓、
  摘要卡默认操作按钮 + 「全部▾」提示 ✓、摘要卡**真实启动/停止一轮**（状态翻到运行中再
  停回）✓、展开/收起/刷新保持 ✓、长说明一行降级 ✓、主页品牌卡 + ?brand= 深链 ✓、
  桌面端 1146px 无摘要卡/面板原样 ✓——全 PASS。
- `tsc --noEmit` 通过；全量 528 用例 + shadowJar 构建绿。

## v0.3.2 — 2026-10-09 · 品牌面板实例行的版本下拉文字被上下裁掉

修复 Kimi Code / OpenCode 面板**实例行**里的版本下拉渲染成「几个灰点」——数字的上下边缘被
裁掉，叠上运行中 `disabled` 的半透明后几乎看不清。

根因：这行下拉写死 `height: 28`。全局 `box-sizing: border-box`，而 `.input` 的
`padding: 8px` 在 border-box 下也占高度，内容盒只剩 28 − 16 = 12px，装不下 13px 字体约
15–16px 的行盒；`<select>` 默认 `overflow: hidden`，多出来的部分被上下裁掉。v0.3.1 修
「版本行 / 实例列表头」两处下拉时只验证了**对齐**，没验证**文字装不装得下**——36 的内容盒
是 20px（装得下），28 的只有 12px（装不下），实例行这处遂漏网。

修法：实例行下拉同样提到显式 `height: 36`，与组件内另外两处一致（行容器 `.tlli` 是
`align-items: center` 的 flex，高度变大由居中自然消化，不会再上浮）。

## v0.3.1 — 2026-10-09 · 品牌面板工具行的下拉对齐加固

修复 Kimi Code / OpenCode 品牌面板两处下拉在个别浏览器里**上浮、与旁边的按钮不齐**
（「版本行」的版本下拉 vs 「已安装」，「实例列表」头的版本下拉 vs 「创建」）。

根因：不设高度的 `<select>` 作 flex 子项时，个别引擎对它的高度测量有偏差——
`align-items: center` 按被夸大的盒模型居中，框的实际绘制位置偏上。修法很简单：
这两处下拉给出显式 `height: 36`，与相邻 `.btn` 按钮（36px）齐平。旁证是实例行内
那个一直显式 `height: 28` 的下拉在同样的环境里从未错位。

Chromium / Firefox / WebKit 三引擎实测：三行（版本行、实例列表头、实例行）内所有
控件中心线一致（±0.1px 内）。

## v0.3.0 — 2026-10-08 · 第三方品牌实例：Kimi Code 与 OpenCode

实例列表页底部的实验框升级为「品牌区」：除 ZCode（实验声明保留）外，新增 **Kimi Code** 与
**OpenCode** 两个第三方品牌——面板从 npm registry 安装指定版本、拉起它们自带的网页端
（`kimi web` / `opencode web`），并像 dsh 一样经 `/i/<id>/` 反代（同端口、同证书、同登录门）。
同时把主页启动按钮旁的实例选择菜单改为**跨品牌实例切换器**：点选 = 切换主按钮的目标实例
（不再跳进详情页），dsh / Kimi Code / OpenCode / ZCode 混合列出并带品牌标签。

### 新增

- **品牌实例后端**（`web/brand/`）：`Brand` 描述符（npm 包、可执行名、就绪行、端口策略）、
  `BrandCatalog`（registry 版本目录，过滤 snapshot/ci 噪音 tag，只留 `x.y.z`，缓存 30 分钟）、
  `BrandInstaller`（`pnpm add <pkg>@<version>` 装进 `<数据目录>/brands/<品牌>/releases/`，
  沿用 `--config.dangerously-allow-all-builds=true`——pnpm≥10 默认不跑依赖 postinstall，而 Kimi
  Code 的 postinstall 要拉原生二进制）、`BrandRuntime`（进程池 + 就绪行解析 + ANSI 剥除）、
  `BrandApiServlet`（`/api/brands/{kimi,opencode}/*`：版本/安装/实例 CRUD/启动/停止/打开/日志）。
- **两个品牌的实测差异**（详见 [docs/brands.md](docs/brands.md)）：Kimi `--port 0` 可用；
  OpenCode 的 `--port 0` 会静默回落默认 4096 → 面板预分配端口；Kimi 起服务时带
  `--allowed-host <面板域名>` + `--dangerous-bypass-auth`（其官方文档写明用于 "behind your own
  authenticating proxy"，面板会话门正是那个代理）；OpenCode 不设 basic auth（面板门即门）。
- **反代泛化**：`InstanceProxyServlet` 的页面改写从「dsh 精确形状」扩展为「任意根绝对 SPA」——
  非 dsh 页面里所有 `(src|href|action)="/…"` 加挂载点前缀、`<head>` 内引用运行期 shim
  （fetch/XHR/WebSocket/EventSource 包装）。e2e 挖出三个坑都钉成了用例：shim 必须作为**同源外链
  脚本**由面板在每个挂载点自服务（OpenCode 的 `script-src 'self'` CSP 拒内联脚本，而「shim 在
  HTML 里却从没执行」是静默失败）；manifest 链接要剥掉（浏览器不带凭证抓它，过会话门必 401）；
  「就绪」以**端口真的在监听**为准（OpenCode 的就绪行比监听早一拍）。dsh 页面的既有处理一字未改，
  回归夹具是 `src/test/resources/brand-pages/` 下两个品牌的真实首页 HTML。
- **e2e 后续实测修正（最终形态：单端口 + 地址栏留在挂载点）**：OpenCode 的客户端**按 URL 路径
  路由**（bundle 直读 `window.location.pathname`，全发行包无 base-path 开关），挂在子路径下只剩壳。
  试过「独立 origin 端口」一版后按「只留一个端口」的要求收回；最终做法是**读时剥、写时加**，两半
  必须成对：
  - 面板在反代它的 JS 时，把路由读取地址的那一处表达式补成「先剥挂载点」
    （`window.location.pathname.replace(/^\/+/,"/")` → 先经 `window.__hdslUnmount`；锚点实测
    1.18.32 与 1.18.35 一致），补过的字节带 `no-store` —— 缓存里留一份没补丁的旧 bundle，会让客户端
    按挂载点本身路由、匹配不到任何路由；
  - `/i/<id>/__hdsl-router.js`（同源外链、CSP 友好）把客户端写回的地址重新加上挂载点
    （`pushState/replaceState`；已经在挂载点里的地址不会重复加）。
  于是地址栏停在 `/i/<id>/…`、刷新与深链回到应用内的原处（上游对未知路径返回它自己的 index 页）、
  地址也可以直接分享——不再是「地址栏显示面板根、刷新回面板」。**兜底**：处理页面请求前先探一次
  客户端的 bundle（按挂载点 + 资源名缓存；抓不到、非 200 或过大都算探不到），探不到就改注入
  `?mode=root` 的那份 shim ——把地址还给面板根（即旧行为），应用照常可用，而不是只剩一个壳。
  Kimi Code 认子路径、不需要这段脚本（按品牌开关，注入反而与它自己的路由打架）。端口段
  （3091–3100）、边过滤器与端口注册表全部删除，compose 恢复只发布一个端口。全量 **528 用例**全绿；
  e2e 无头浏览器与「直连实例」逐项对照（渲染、点击、pushState/popstate、深链、刷新）逐字一致，
  零控制台错误、零面板侧失败请求。
- **用起来才暴露的三处代理缺陷**（用户实测「加 DeepSeek key 点继续就失败」触发，全部修掉并复验）：
  - **客户端自己拼的根绝对 URL**：页面改写只看得见 HTML 里的引用；客户端运行时拼的 chunk、
    worker、图标（bundle 里 `"/assets/…"` 字面量与 Vite preload 助手的 `"/"+dep` 基址）与 CSS 里的
    `url(/…)` 全都打到面板根，拿到的是面板自己的 HTML——OpenCode 的 Settings / Providers / 所有
    对话框都是懒加载 chunk，于是「首页能看、点进任何面板都是空的」。现在反代 JS/CSS 时一并改写
    这三类引用（见 [docs/brands.md](docs/brands.md)）；CSS 字体回退的问题也一并消失。
  - **shim 重包 `Request` 的方式**：`new Request(url, 原Request)` 会把请求体变成流，Chrome 对这种
    带 body 的 `PUT` 直接以 `net::ERR_ALPN_NEGOTIATION_FAILED` 失败——面板连请求都收不到。改成从
    原 Request 的字段显式重建、body 从 clone 读出，`PUT /auth/{provider}` 由此走通。
  - **上游连接复用**：实例（Node/Bun）5 秒就掐掉空闲 keep-alive，池里那条死连接让请求静默失败
    （JDK 自己抛 `Http1RequestBodySubscriber` NPE，面板对它答 502）。现在空闲连接 4 秒退休；
    可重放的请求失败后用「从未连过实例」的 client 重试一次；带 body 的请求改为「放得下就缓冲、
    放不下才流式」，顺带修掉「无 body 的 GET 被当成带 body」这件旧事。

- **界面上的能力边界声明**：OpenCode 的页签改名「OpenCode（实验）」，品牌页在原有的第三方警示下面
  多一行——面板对它的接入**不保证可用性**，只做到「装指定版本 → 拉起它自带的网页端 → 反代到
  `/i/<id>/`」，再往下遇到的问题（模型目录与模型 id 对不上、会话与 agent 的行为、凭据与计费）都是它
  自己的，面板修不了。`docs/brands.md` 与新增的诊断报告
  [docs/report-2026-10-08-opencode-web.md](docs/report-2026-10-08-opencode-web.md) 写明了同一条边界。

### 刻意没做

- **插件管理**：两个品牌都没有可用生态。
- **凭据注入**：登录/供应商/计费都在工具自己的网页里完成；实例状态目录（`~/.kimi-code` /
  `~/.config/opencode`）落在实例 `home/` 里随 `/data` 持久化。第三方工具，功能/安全/行为由各官方负责。

### 验证

- 全量 **528 用例 / 0 失败 / 0 错误 / 0 跳过**（相对上一版净增 29 条：目录解析/过滤、启动命令组装、
  ANSI 剥除、就绪行、release 簿记、API 全生命周期（fake 可执行文件真绑定端口走完 创建→启动→就绪→
  打开→守卫→停止→删除）、真实 HTML 夹具的页面改写、shim 资源端点、bundle 补丁（路由锚点、拒绝歧义、
  不碰客户端那处 `auth_token` 清理、preload 基址与 `/assets/` 字面量、CSS `url(/…)`）与入口脚本解析，
  外加一条「带 body 的请求穿过反代」（小 body 缓冲、512 KB 走流式，两端都要求逐字节到达））。
- spike 实测：pnpm 安装两品牌（kimi postinstall 正常）、`--port 0` 行为、就绪行、反代旗标；
  OpenCode 路由读取地址的那一处表达式（1.18.32 与 1.18.35 各抓一份 bundle 核对，两版一致）。
- e2e（临时容器，无头 Chromium）：真实面板装两品牌 → 启动 → 经 `/i/<id>/` 打开——Kimi Code
  完整渲染出引导页（0 失败请求）；OpenCode 经反代建会话成功、UI 可交互（0 控制台错误、0 失败
  请求）；期间挖出并修掉 CSP 内联拦截、manifest 401、就绪行早于监听三个真实坑。
- e2e 复验（2026-10-08 晚，同一套无头浏览器，单端口）：地址栏在 `/i/<id>/` 与
  `/i/<id>/new-session` 下都留在挂载点；页内 `pushState('/zzz')` 得到 `/i/<id>/zzz`，把带挂载点的
  地址再写一次也不会双前缀；深链 `/server/…/session/…`、失效会话、刷新各项与「直连实例」对照逐字
  一致（含应用自身那 3 个 500 `/api/reference`——直连也有，与面板无关）。
- e2e 复验（2026-10-08 晚第二轮，同一天同一套无头浏览器）：Settings 的 Servers / Providers / Models
  面板**完整渲染**（此前只有首屏，懒加载 chunk 全 404）；走完真实流程「Providers → Show more
  providers → 搜索 DeepSeek → 填 key → Continue」——`PUT /auth/deepseek` 200、对话框关闭、key 落进
  实例的 `auth.json`；**零控制台错误、零失败请求、面板日志零上游失败**，连跑两轮结果一致。修之前
  同一套流程稳定复现两个症状：chunk 打到面板根（`Failed to load module script … text/html`）与那条
  `PUT` 的 `net::ERR_ALPN_NEGOTIATION_FAILED`（面板根本没收到请求）。

### 顺手修掉的一处类型错误

默认壁纸一直以 `application/octet-stream` 下发：`StaticServlet` 的内容类型表里有 `svg`、`png`、`ico`，
唯独没有 `jpg`/`jpeg`，而壁纸正是 `.jpg`。浏览器会按内容嗅探渲染，所以画面从来是对的——这也正是它
一直没人报的原因；但类型是错的，且哪天加上 `X-Content-Type-Options: nosniff`，图片就会真的被拦掉。

- 补上 `jpg`/`jpeg` 两个键。
- 新增用例钉住：`/assets-img/wallpapers/2021-08-26.jpg` 必须回 `image/jpeg`、且是**它自己的字节**
  （长度 > 1KB），而不是 SPA 兜底返回的 HTML 外壳——坏的时候状态码也是 200，只看状态码看不出来。

## v0.2.5 — 2026-10-06 · 容器镜像里丢失的壁纸与图标

独立部署（`docker pull` 起容器）第一次打开网页就能看见：**默认壁纸不见了**，启动器与实例的图标也是坏的。
同一份代码本机 `./gradlew shadowJar` 出来的 jar 却一切正常。

根因不在前端、也不在静态服务，而是镜像构建的 web 阶段少带了一份源码目录。前端请求的
`/assets-img/wallpapers/2021-08-26.jpg` 不是源码里现成的文件，而是 `web/scripts/sync-assets.mjs`
（`package.json` 的 `prebuild` 钩子）从 `../src/main/resources/assets/img` 同步到 `web/public/assets-img/`
的产物。web 阶段只 `COPY web/ ./`，那份源目录不在阶段里，脚本于是走了它自己的「资源目录不存在，跳过」分支：
`console.warn` 之后 `process.exit(0)` —— **构建依然是绿的**。

- 实测账：镜像内 jar 的 `assets-img` **0 条**，同版本 Release jar **84 条**
  （3 张壁纸 + 约 79 个图标：`icon.png`/`icon-title.png`/`icon@4x`/`icon@8x`、`unknown_pack.png`，
  以及 `8mi-tech`、`ShulkerSakura`、`april_fools`、`chest`、`chicken` 等实例图标）。
- 为什么一直没被发现：这些路径**不返回 404**，而是被 SPA 外壳兜底接管 ——
  回的是 `200 + Content-Type: text/html` 的 `index.html`（1234 字节）。浏览器拿 HTML 当图片用，
  背景自然不显示；而只看状态码的检查会认为一切正常。
- 只有镜像坏：CI 的 `./gradlew` 在完整仓库里构建，`sync-assets` 读得到源目录，所以 Release jar 一直是好的。

### 修法

- web 阶段按脚本期望的相对路径把资源源目录带上：
  `COPY src/main/resources/assets/img /src/main/resources/assets/img`。
- 构建末尾加一条断言 `RUN test -f /web/dist/assets-img/wallpapers/2021-08-26.jpg`：
  资源缺失就当场失败，不让这类回归留到部署之后才被发现。

### 验证

- 构建上下文用 `git ls-files` 构造（等同于全新 clone，`assets-img` 0 条），在真实 docker 上双向跑：
  带修复 `BUILD_RC=0`、`[sync-assets] 82 个文件 → public/assets-img`、阶段镜像里 `dist/assets-img` 84 条、
  三张壁纸俱在；去掉修复则在断言处 `BUILD_RC=1`（`exit code 1`）。
- 另有一条不需要 docker 的复现：按 web 阶段的目录布局单独跑 `npm run build`，`vite` 产出的 `dist/`
  只有 `assets/` + `index.html`，与镜像内 jar 的内容逐项一致。
- 全量 **499 用例 / 0 失败 / 0 错误 / 0 跳过**；`hdsl-web-0.2.5.jar` 内 `assets-img` **84 条**。

## v0.2.4 — 2026-10-06 · 环境体检：内核文件监视器配额

给「实例起不来」补一个能看的病因。症状是**两个实例不能共存**：一个在另一个起来之后起不来，反过来
也一样；而两种表现都不指向病因——实例要么起来就消失，要么进程活着、端口在监听，却一直停在「启动中」、
`/i/<id>/` 返回 502。

根因不在反向代理，也不在 dsh 之间，而在一个共享的内核数字：`fs.inotify.max_user_watches`
**按 uid 计、容器之间不隔离**。面板容器以 `hdsl`(1000) 运行，宿主上任何同样以 uid 1000 运行的程序
都从这个数字里扣；而 dsh 启动时要用 chokidar 监视自己的 `profiles/<profile>`。

- 实测账：上限 65536（内核默认值），而宿主上**同一个 uid 的另一个递归监视程序**就能把它吃到几乎
  全满——用正确测法（连续申请并保持打开）只剩 **1 个**可注册；一个 dsh 实例实际只要 ≈9 个。
- 拿不到监视器时 node 要么当场崩在
  `Error: ENOSPC: System limit for number of file watchers reached, watch '…/profiles/web'`，要么那个
  启动项永不结束、就绪行一直不打印。两种失败都在一次性容器里复现过。
- 「谁被拒」由启动那一刻还剩几个空位决定，与实例身份无关：`instance` 的 pokemon profile 实测只占
  0 个 watch（`@hellosz/dsh-pets` 与 `dsh-wildmon` 都不用文件监视器），它不是吃配额的那个。
- 把那个占着配额的程序停掉、配额腾空之后，同一个一次性容器里两个实例（0.2.0-rc.2 与 0.1.5-alpha.2）
  可同时 RUNNING 并稳定运行，两个挂载点各自 200、页面 `<base href>` 各指向自己的挂载点、互不串台。

### 新增：环境体检里的「File watchers」

**只加诊断，不动启动流程。**

- 新增 `util/platform/WatcherBudget`：读上限，并用「真实注册目录」探测还剩多少（每个 watch 对应一个
  自己的目录，与 `inotify_add_watch` 同语义；用完即删；`-1` 表示问不出来，绝不当成「没有了」）。
- `DshDoctor` 新增 `File watchers` 段：充足时 `at least 64 free of 65536  [ok]`，不足时
  `1 free of 65536  [SHORTAGE]` 并打出可直接复制的 `sysctl -w …=524288`。探测上限是 64，所以充足时
  写「at least」——把 64 说成总数会读成「快满了」，正好相反。
- `DoctorApiServlet`：`[SHORTAGE]` 映射为 `warn`。

### 顺手修掉一个 JDK 坑

`/proc` 文件报告 `st_size=0`，而 `Files.readString` 走按 size 读的路径，对 `65536` **只返回首字节
`"6"`**（同一容器里 `readLine` 得到 `65536`）。`WatcherBudget` 因此按行读，并有用例钉住「读完整的数、
不是首字节」——一个看起来合理的错数字比没有数字更糟。

### 验证

- 全量 **497 用例 / 0 失败 / 0 错误 / 0 跳过**（新增 11 条：探测读数与清理、三种行文案、状态映射）。
- 临时容器里换入新 jar 的面板打 `GET /api/doctor`，实到
  `file-watchers  status=ok  inotify watches: at least 64 free of 65536  [ok]`。
- 未验到「真·配额不足时该行在 API 里的渲染」：要验就得把那台机器的配额喂满。该分支由单测覆盖，而
  「配额满时内核确实拒绝」另有实测。

### 文档

- `README.md`「常见问题」加这条症状的入口。
- `docs/deployment.md` 新增 §8「宿主 inotify 配额」（配额怎么算、两种现象、怎么确认、怎么调大，以及
  不想调配额时的两条替代路——其中「换 uid」按实测写了除 compose 之外还要解决的写权限），并补上此前
  漏掉的 §7/§8 目录项。

## v0.2.3 — 2026-10-05 · 反代挂载下 dsh 网页能打开了

修一个用不了的功能：实例改走 `/i/<id>/` 反代之后（v0.2.2），**点「打开 dsh」进不去**——
按钮看着像在原地刷新，手动敲地址则是白屏。根因是 dsh 的网页端假定自己拥有整个 origin，
而代理只做了上游那份参考实现里的那几件事。

### 修复

- **`Location` 没有重写**：dsh 的 token 兑换回 `303 Location: /`，浏览器跟着它离开挂载点、落到
  面板首页——就是「原地跳转」。现在改回挂载点内；指向上游回环端口的绝对地址也一并改写，
  外部地址原样放行。
- **dsh 生成的页面把根路径写死**：页面开头是 `<base href="/">`，另有 52 处根绝对的 `/plugins/...`
  （`<link rel=preload>`、`<script src>`，以及内联 `__DSH_BOOT__` 里每个插件条目的 `url`）。经挂载点
  访问时它们指向面板，面板的 SPA 兜底把 `index.html` 当 JS 回，浏览器报
  `Failed to load module script: … MIME type of "text/html"`，一个脚本都加载不了——白屏。现在把这两处
  加上挂载点前缀，并去掉那行取不到凭证的 manifest 链接；正文是 gzip 的，**先解压再改写**，回给浏览器时
  丢掉 `content-encoding` / `content-length`。
- **应用运行期自己拼的 URL 也是根绝对**（`fetch("/api/…")`、`ws://<host>/api/remote.mux`、
  `/plugins/events` 的 SSE）：改写后的页面里注入一段小 shim，把 `fetch` / `XMLHttpRequest` /
  `WebSocket` / `EventSource` 包一层，同 host 且不在 `/i/` 下的 URL 加上挂载点（字符串、`URL` 对象、
  `Request` 三种入参都处理）。SSE 与 WebSocket 仍然流式转发，不受影响。
- **`ProxyBehaviorTest` 加 3 条回归用例**：页面改写（含 gzip、含「不认识就一字不改」）、`Location`
  的四种形状。把实现换回修复前的版本跑同一批用例，新用例如期失败——测试确实钉住了这个 bug。

### 验证

- 全量 **486 用例 / 0 失败 / 0 错误 / 0 跳过**（新增 3 条）。
- 临时容器里用无头 Chromium 真跑整条链路：dsh **0.1.5-alpha.2** 与 **0.2.1-alpha.1** 两个版本都能打开，
  最终地址停在 `/i/<id>/`，所有接口变成 `/i/<id>/api/…` 并返回 200，`/i/<id>/plugins/events` 是
  `text/event-stream`，DOM 里有完整界面，**控制台零报错、无 401/404**。

### 其他

- 对照上游的结论（免得以后以为我们擅自偏离）：`deepseek-ai/deepseek-harness`（**默认分支是 `master`，
  不是 `main`**）的 `apps/web/tests/prefix-proxy.ts` 是给**它自己的测试客户端**（`scaffold.ts`：知道前缀、
  自己处理 303、用 `baseUrl` 拼路径）用的最小件，**它本身也不重写 `Location`、不碰页面**；全仓没有
  `X-Forwarded-Prefix`，dsh 也没有任何 base/prefix 旗标。上面那三层是本启动器自己的。

## v0.2.2 — 2026-10-05 · ZCode 实验品类 + 面板可选下载源

v0.2.0 之后的第一个发布，两块内容合在一起：**ZCode**（第二个可启动品类，实验性）从零接进来——
面板自己下载上游源码、打反代补丁、`pnpm` 构建、装成版本，实例只绑回环、经 `/i/<id>/` 反代访问；
以及**面板里可选下载源**，把「换源要改环境变量、还得重启容器」变成一等设置，并顺手拆掉 ZCode
构建链上几个必然失败的坎。（v0.2.1 未单独发布，其内容并入本版。）

### 新增：ZCode 实验品类（实验性）

- **独立品牌分区**：实例页里 ZCode 与 dsh 完全隔离，文案与 `docs/zcode-experimental.md` 都写明
  「纯实现性验证功能：不保证可用，也不承诺与 dsh 同等的功能与兼容」。
- **面板内构建** `POST /api/zcode/build`：下载上游 tag/分支的源码 tarball → 打 5 处反代补丁
  （vite `base: "./"`、`main.tsx` 的 `hdslBasePath()` 与 WS origin、`/api/server-info`、OAuth
  `tokenUrl`）→ `pnpm install` → `pnpm build:zcode` → 校验产物记录的 sha256 → 装成
  `<数据目录>/zcode/releases/<版本>/` 并维护 `current`。界面有步骤、进度与实时日志；上游把锚点
  挪走时构建**明确失败并点名文件**，而不是产出一个指向错误地址的发行包。
- **反代**：实例只绑 `127.0.0.1`、`--no-token`，浏览器经面板的 `/i/<id>/` 进来——同端口、同证书、
  同样要登录；HTTP 代理与 WebSocket 中继共用一套目标解析（`InstanceProxyTargets`），
  未知 id 404、已知未运行 502 的语义与 dsh 一致。
- **凭证**：保存的 API Key 按 ZCode 的 `provider_config.json` 格式写入实例目录（未文档化格式，
  尽力而为；上游一改就可能失效）。
- 镜像里加 Node 24（`/opt/node24`，只有构建用它，面板与 dsh 仍是 Node 22），发行包与限制见
  `docs/zcode-experimental.md`。

### 新增：面板可选下载源

- **设置 → 下载源**（`web/src/pages/SettingsPage.tsx` 的 `DownloadSourceTab`）：预设下拉（跟随部署环境（默认）/
  官方 / npmmirror / 中科大 / 腾讯云 / 华为云）+「自定义…」时才出现的手填 URL，输入即时行内校验（红字 + 保存置灰），
  另有一行显示**当前生效的源与它的来源**（面板设置 / 环境变量 / 默认官方）；保存成功后回读服务端值。
  此前这张卡片是个只写 `localStorage` 的摆设——无 API、无校验，刷新后「当前」还会显示错，卡片自己写着
  「网页版暂未接入服务端下载源设置」。
- **`GET/POST /api/settings/registry`**（`web/http/RegistrySettingsApiServlet`）：`GET` 回
  `preset` / `registry` / `effective` / `source` / `presets[]`；`POST` 收预设 id 或自定义 URL，
  非法输入 400 **且设置保持不变**。另有 `POST /api/settings/registry/test`：拿生效或指定的地址取一次
  `is-number` 的元数据，回 `{ok, millis, status}`（5 秒超时）——「源通但慢得像卡死」正是这个设置要解决的问题，
  光看界面分不出来；卡片上就有一个「测速」按钮，测的是当前选中的那个地址（选着「自定义…」时测输入框里填的，
  否则测那个预设自己的），结果旁会回显服务端实际测的地址。`DshDoctor` 的 Settings 段顺带加一行
  `npm registry: <有效值> (<来源>)`——卡住时不用 `docker exec` 就能判断实际用的哪个源。
- **设置落盘**（`dsh/LauncherSettings` + `setting/SettingsManager`，落 `/data/hdsl/launcher-settings.json`，
  0600 原子写）：新增 `npmRegistryPreset` 与 `npmRegistry` 两项。此前启动器设置没有 REST 通道，前端也没有对应控件；
  这份快照是 `save()` 唯一的写入面、`load()` 唯一的读取面——不登记进来的设置就是「能改、下次启动就丢」，所以一次补齐。
- **防注入校验**（`dsh/NpmRegistry.normalize`，REST 与子进程/文件写入共用）：只允许 `http`/`https`、
  必须能解析出主机名、禁止用户名密码 / 查询串 / 片段，以及空白、控制字符与 `"` `'` `` ` `` `\` `#` `$` `;` `|` `&` `@`，
  长度 ≤ 200，尾斜杠归一化。自定义源是管理员填的，但它要进 argv 与配置文件，写时校验、用时再规范化，两道都要。

### 修复

- **pnpm 系根本不读环境变量，光注入 env 覆盖不到它**：pnpm 11 的 registry 只来自它自己的全局配置
  `~/.config/pnpm/config.yaml`——容器里没有任何 `.npmrc`，`npm_config_*` 对它完全无效
  （实测 `NPM_CONFIG_REGISTRY=… pnpm config get registry` 被忽略）。所以面板在启动时与每次保存后**重写这份文件**
  （`dsh/PnpmConfigFile`）：逐行保留其它设置（`storeDir` 不会丢，本来缺了还会按 `PNPM_STORE_DIR` 补一行）、
  只重写 `registry` 一行，值经 YAML 引号、临时文件 + 原子替换、权限 0600。同时给子进程注入
  `npm_config_registry` / `NPM_CONFIG_REGISTRY`（npm 系读它），并给面板自己起的 pnpm 显式加 `--registry=`
  （dsh 安装、ZCode 构建，日志里看得见）。优先级定为**面板设置 > 环境变量 `NPM_CONFIG_REGISTRY` > 默认官方**，
  预设「跟随部署环境」就是显式回退到后两者。
- **ZCode 构建报 `Could not find any Python installation` / `Unable to detect compiler type`**：
  `Dockerfile` 运行时阶段补上 `python3` / `make` / `g++`——ZCode 依赖里的 `node-pty`、`cpu-features`
  要现场编译。这不是用户环境的问题，镜像里没编译链，面板自己那次也栽在同一处。
- **Electron 的 postinstall 卡死**：构建器按所选源设置 `ELECTRON_MIRROR`（`npmmirror` 预设带
  `https://npmmirror.com/mirrors/electron/`）——它要从 GitHub releases 下约 100 MB 二进制，实测直连 20 秒 0 字节。
- **构建里还有两个环节不在 registry 的覆盖范围内，都不是「换源」能解决的**：
  ① node-gyp 编译原生模块前要从 `nodejs.org` 下一份 10 MB 的 Node 头文件，那个域名跟 npmjs 一样慢
  （实测 104 KB/s，npmmirror 的 Node 镜像 5.7 MB/s），而头文件缓存在 `~/.cache/node-gyp` —— **不在卷里**，
  所以每次重建容器后第一次构建都要重下；现在用镜像源时会同时设 `NODEJS_ORG_MIRROR` / `npm_config_disturl`
  指向镜像（用官方源的机器保持 node-gyp 默认——能连 npmjs.org 的机器也能连 nodejs.org）。
  ② node-gyp 默认**不给 `make` 加 `-j`**，而构建跑在一台 12 核的机器上：现在按 CPU 数设
  `JOBS` / `npm_config_jobs`。顺带，ZCode 构建的子进程现在会继承启动器给**所有**子进程的那套环境
  （代理、registry、上面这些旋钮）——此前它一个都没继承，面板里配的代理对构建完全无效。
- **构建失败的证据被删掉**：`finally` 里改成失败时**保留**源码树与 `build.log`（原来失败也照样删干净），
  排查完不用重下，下一次构建开始时才清。
- **ZCode 干净检出必然失败于 `Missing @zcode/shared dist files`**：上游 `packages/shared` 没有 build 脚本，
  报错让你跑的 `pnpm build` 也不会编译它（`pnpm -r build` 跳过没有该脚本的包），只有项目引用构建（`tsc -b`）
  会产出 `dist/index.js`——上游开发者本地「碰巧」跑过根 `typecheck` 脚本（它列了 `packages/shared`）才有这个文件。
  构建器因此在装完依赖后补一步 `pnpm exec tsc -b packages/shared` 并检查产物存在，再继续打包。
- **删掉了构建器往源码目录写 `supportedArchitectures` 的「平台过滤」**：pnpm 是从 `pnpm-workspace.yaml`
  读这个设置、**不看 `.npmrc`**，所以它从未生效；而一旦生效就会把构建搞坏——组包阶段要为 6 个平台
  （darwin/linux/win × x64/arm64）收集原生资源，缺一个直接失败。上游 `pnpm-workspace.yaml` 已经写着它需要的平台。
- 文档修正：README 原来说「pnpm 11 只从 `.npmrc` 读认证与 registry」，实测 registry 来自 pnpm 自己的
  `config.yaml`，且 `npm_config_*` 对 pnpm 无效——README 与 `docs/zcode-experimental.md` 一并改正，
  并把「面板设置优先、环境变量只是回退」写进环境变量表、常见问题、功能清单与部署说明（`docs/deployment.md`
  新增「运行期下载源（面板设置）」一节，讲清与构建期 `NPM_REGISTRY` 的区别与改写 `config.yaml` 的后果）；
  常见问题里「要用编译器就自己加 `build-essential`」一条随编译链内置一并更新，`docs/ui-spec.md` 把下载源卡片
  从占位补成实际控件（含窄屏行为），差异清单登记为第 4 处。
- **Node 探测失败被当成「没装 Node」**（`dsh/DshNodeRuntime`）：探测内部要 spawn `node --version`，
  负载高时那一次 spawn 失败就报成「Node.js was not found on PATH」——CI 上偶发红、重跑即过的那两条
  插件用例正是同一个原因。现在版本探针有一次重试，成功的探测结果缓存 60 秒（失败不缓存，装好工具链
  立刻可用），顺带每次启动少 spawn 两三个进程。
- **测试不再依赖 stub 的可执行位**：本仓库 `core.fileMode=false`，git 根本不记录文件模式，CI 检出后
  假 pnpm 没有可执行位就卡在「起不来」；测试改为复制到临时目录再 `setExecutable`，索引里也用
  `update-index --chmod=+x` 把 100755 钉住。

### 验证

- 全量 **483 用例 / 0 失败 / 0 错误 / 0 跳过**（含新增的 `NpmRegistryTest`、`PnpmConfigFileTest`、
  `RegistrySettingsApiTest`，以及 `SettingsPersistenceTest` 与 `ZcodeBuilderTest` 的扩展）。
- `npm run typecheck` 与 SPA 构建通过（`vite build` 不做类型检查，单独跑过）。
- 独立端口 + 独立卷的临时容器（HTTP）跑完整验收：
  - **界面**：预设下拉列出 6 项（含服务端下发的实测速度注记「本机实测约 25 MB/s」）；切到「自定义…」后手填框
    才出现；非法地址 `https://a b/` 当场红字「地址里不能有空格」且保存置灰；测速按钮回「约 53 ms」；
    保存后「当前生效」显示 *面板设置* + `https://registry.npmmirror.com`；桌面 1440×900 与手机 390×844
    均无横向溢出（`scrollWidth === innerWidth`）。
  - **接口**：四条注入尝试（换行注入 `https://x/\nstoreDir: /etc`、`javascript:alert(1)`、带空格、
    带用户名密码）全部 **400** 且设置保持不变；自定义地址的尾斜杠被归一化；`POST /test` 回
    `{"ok":true,"status":200,"millis":43}`。
  - **落盘**：`launcher-settings.json` 出现 `npmRegistryPreset` / `npmRegistry`；容器内
    `pnpm config get registry` 回**新值**（pnpm 不读环境变量，所以这只能来自重写的那份配置）、
    `pnpm config get store-dir` 仍是共享仓库（`storeDir` 没被重写丢掉）、配置文件权限 0600；
    `DshDoctor` 报 `npm registry: https://registry.npmmirror.com  (setting)`。
  - **ZCode**：从面板设置取源（`pnpm install --registry=https://registry.npmmirror.com`）跑完整条构建，
    **6 分半**出 `zcode-3.14.3.tar.gz`（带 sha256）并装成 release；`/data/zcode/current` 指向它、
    `bin/zcode.mjs` 可执行、面板 `GET /api/zcode/releases` 列出 `3.14.3  current=true`。
- **ZCode 反代**（本机实跑面板 + stub 发行包）：未登录 `/i/<id>/` → **401**；登录后 → **200**（实例页面）；
  `/i/<id>/some/route?x=1` 路径与查询串原样透传；裸挂载 `/i/<id>` → **301**；未知 id → **404**；
  停止后 → **502**；实例进程 argv 确认为 `--host 127.0.0.1 --no-token`（对外只有面板这一条路）。
- **界面**：桌面 1440×900 与手机 390×844 截图核对 ZCode 分区（发行包识别、构建对话框的状态与实时日志、
  实例卡片操作），移动端 `scrollWidth === innerWidth` 无横向溢出。

## v0.2.0 — 2026-10-05 · models.dev 模型目录

中等规模的功能更新（minor：0.1 → 0.2）。账户页接上 [models.dev](https://models.dev)
目录（226 家供应商 / 约 3500 个模型）：供应商可搜索、可刷新，选定后端点与协议自动
带出；「默认模型」从手写文本框变成可搜索下拉（副标题带上下文与价格），手输一直保留。

### 新增

- **模型目录** `dsh/DshModelCatalog`：读 `https://models.dev/api.json`（`-Dhdsl.modelCatalog`
  可指向别处，测试与内网代理用它），先解析再落盘——读不动的文档不会覆盖可用副本；
  缓存 `/data/hdsl/catalog/models-dev-<sha256(url) 前 16 位>.json`（与插件目录同一套
  命名与目录，尊重自定义缓存目录）。协议只认能映射到 `DshVendor.APIS` 三种的，
  认不出返回 null——界面禁用该卡片，不假装可用。
- **`/api/models/*`**（`ModelsApiServlet`）：
  - `GET /api/models/providers[?refresh=1]`：dsh 自家目录（offered 13 家 + harness 37 家
    + 本机自定义）与 models.dev 合并，**在服务端合并**；TTL 12 小时，`?refresh=1` 绕过；
    整个目录拉不到且磁盘也没有副本时 502，有副本时降级为 `source:"cache"` 并只保留
    5 分钟（过后再试上游）。
  - `GET /api/models/providers/{id}`：单家供应商的模型（窗口 / 输出上限 / 推理档位 /
    能力 / 价格）；dsh 认识而目录没有 → 200 + 空模型数组，界面退回手输，不是 404。
- **`ModelSelect`** 组件：受控的「输入框 + 过滤下拉」，首项固定「留空（由 harness
  决定）」——不填才是安全默认，写错模型名会让 harness 拒绝启动；目录不可用 / 这家
  没有模型时退化成普通输入框，不挡路、不弹错；列表异步到齐时会自己弹出。
- **账户页**：供应商步骤加搜索框 + 刷新按钮 + 计数；卡片网格去掉写死的 `minWidth: 420`
  （360px 屏必然横向溢出）改自适应 + 内部滚动；编辑弹窗同样列出模型。
- 数据文件 `assets/models-dev-aliases.txt`（dsh id ↔ models.dev id 别名表），其余靠
  host 自动命中（fireworks→fireworks-ai、kimi-coding→kimi-code-plan-cn 等）。
- 测试 16 条：`DshModelCatalogTest`（纯解析 + 容错 + 别名/host/id 三条匹配路）、
  `ModelsApiTest`（真服务器 + JDK `HttpServer` 假目录：合并名单、刷新真重抓、失败退回
  磁盘副本、无副本 502、字段裁剪）、`AccountApiTest` 三个新用例。

### 改动

- `POST /api/accounts` 新增可选 `protocol`，只对目录里**新发现**的供应商生效（dsh 自带
  的以 dsh 的为准；已有同 id 的自定义厂商原样返回）——否则 `@ai-sdk/anthropic` 那几家
  会被按 `openai-completions` 写进路由，而错误协议是启动时才失败的静默故障。
- `DshVendor`：新增 `keyVariableOf(id)` 与 `discovered(id, name, baseUrl, api)` 重载；
  密钥环境变量名仍按 harness 的推导，models.dev 的 `env` 只作界面提示（两者确实不同，
  如 ZHIPU_API_KEY vs ZAI_API_KEY）。
- 刻意**不碰启动链路**：`DshAccountRoute` 里的硬编码与 ACP 注入一行未动，本版只动界面
  与它背后的目录。
- 文档：`docs/ui-spec.md` 账户页一节改写 + 新增「模型目录（models.dev）」小节；
  README 补 `/data/hdsl/catalog/` 一行与功能清单；新增
  [`docs/session-handoff-2026-10-05.md`](docs/session-handoff-2026-10-05.md)。

### 验证

- 后端 **443 用例 / 0 失败 / 0 错误 / 0 跳过**（基线 427 + 新增 16）；`npm run typecheck`
  与 SPA 生产构建通过。
- 独立端口 + 独立卷的临时容器真拉 models.dev：名单 **228 家**（dsh 认识 29 + 目录独有
  199，17 家因签名体制类协议禁用）；`?refresh=1` 后 `fetchedAt` 变化；openrouter
  390 个模型；走完「搜 Moonshot → 选供应商（端点自动带出）→ 下拉选 kimi-k2.6 → 保存」
  全流程，落盘与卡片显示均正确；手机 390×844 供应商弹窗与模型下拉无横向溢出。

## v0.1.61 — 2026-10-04 · 关于页改讲本项目

小改动：关于页不再照搬上游（HMCL）的版权与作者，改讲 HDSL-web 自己；
版本号随构建写入「项目」一行——本版起显示 v0.1.61。

### 修复

- **关于页不再冒充上游**：原来照搬桌面版 HDSL 的关于页，显示的全是 HMCL 的数据——
  版权「© 2013-2026 huangyuhui 及贡献者」、作者「bilibili @huanghongxun」、
  开源链接指向 HMCL 仓库，等于把别的项目的数据当成自己的。现在讲本项目：
  项目（`HDSL-web v<服务端版本>`，取自 `GET /api/health`）、说明、仓库链接、
  许可证（GPL-3.0）、上游（HDSL · HMCL，完整声明见仓库 NOTICE）、技术栈；
  顺手修掉上游行重复的 "HDSL" 与窄屏下仓库链接断行。

### 其他

- CI 测试失败时打印完整异常（`exceptionFormat=FULL`）：上次 `PluginApiTest` 偶发失败
  （本地 10/10 通过、重跑即过），日志里只有一行行号、断言消息被吞，无法定位；
  此后失败会带上完整消息。

### 验证

- 全量 **427 用例 / 0 失败 / 0 跳过**；`npm run typecheck` 与 SPA 构建通过。
- 实机截图：本机构建的 v0.1.61 起服务，桌面 1440×900 与手机 390×844 的关于页均显示
  `HDSL-web v0.1.61`，各条目与仓库信息一致。

## v0.1.6 — 2026-10-04 · 手机端适配

面板现在可以拿手机用了：侧栏抽屉、横向标签条、贴底启动栏，主屏一屏启停；
顺带修好深链接白屏，以及 ACP 控制台「对话报缺密钥」。

### 新增

- **手机端适配**（`web/src/styles/mobile.css`，桌面端零变化）：
  - 移动判定 `(max-width: 760px), (max-height: 520px)`——后者覆盖横屏手机；
    触摸优化判定 `(hover: none) and (pointer: coarse)`——与宽度无关，平板同享。
  - 主页侧栏收进抽屉（汉堡唤出、遮罩，点条目 / 遮罩 / 路由变化都收起）；
    内容页子侧栏改为顶部横向可滑标签条；启动面板改为贴底整条操作栏
    （主钮 56px 高、避让安全区，进度浮层改为底部卡片）。
  - 主页补「当前实例」卡片：手机上一屏看到状态并启停 / 打开 dsh。
  - 支持添加到主屏幕：`manifest.json` + apple-mobile-web-app 元信息、`viewport-fit=cover`。
- `useMobileLayout()`（`useSyncExternalStore` 订阅 `matchMedia`），判定条件与 CSS 两处一致。

### 修复

- **深链接白屏**：vite 的 `base` 由相对 `./` 改为 `/`——此前直接打开或刷新 `/instances/xxx`
  时 `./assets/…` 解析成 `/instances/assets/…`，命中 SPA 兜底返回 HTML，模块脚本被浏览器
  拒绝执行，整页空白。
- **控制台对话报缺密钥**：ACP 控制台启动时也把实例指定的账户交接给它——路由写进
  `profiles/acp` 自己的补丁层、密钥放进子进程环境，停止 / 失败 / 退出时收回；与启动路径
  共用同一份账号解析。刻意不碰每个 home 一份的 `settings.yaml` / `.hdsl-injected.json`。
- `StaticHandlerTest` 的资源断言跟随 `base: "/"`（此前写死了 `./assets/`）。

### 验证

- 全量 **427 用例 / 0 失败 / 0 跳过**；`npm run typecheck`（noUnusedLocals）与 SPA 构建通过。
- 4 个视口 11 张截图逐张核对（390×844 / 360×800 / 844×390 / 1440×900），每张断言无横向
  溢出；桌面版式与改造前一致。

## v0.1.5 — 2026-10-03 · 修改用户名

用户系统补上「修改用户名」：改完不用重新登录，本设备的会话直接切到新名字，
其它设备上的会话照旧退出——与改密码同一套「本机保留、其余下线」的语义。

### 新增

- `POST /api/auth/username`：修改当前登录用户的用户名。口令、盐、PBKDF2 迭代次数与创建时间都不动，
  只改 `users.json` 里的名字（仍是原子 0600 写入）；发起改名的这个会话保留并切换到新用户名，
  该用户的其它会话全部吊销。未登录 401，非法名字（空白 / 超长 / 空）400。
- 设置页新增「修改用户名」卡片（在「修改密码」下方）：1–32 位且不含空白字符，
  成功后面板右上角的用户名即时更新，失败弹出原因。
- 回归测试 `usernameChangeRenamesLoginAndRevokesOtherSessions`：覆盖匿名 401、非法名字 400、
  改名后 `whoami` 返回新名、另一个会话被吊销、旧名不再能登录、新名用原密码可以登录、新名字已落盘。

### 验证

- 全量 **422 用例 / 0 失败 / 0 跳过**（较 v0.1.4 新增 1 条）；`npm run typecheck` 与 SPA 构建通过。

## v0.1.4 — 2026-10-03 · 容器发布版

把项目做成**标准容器发布**：镜像在 GitHub 官方 runner 上自动构建并推送到 GitHub Container Registry。
本版与 v0.1.3（暂时稳定版）**代码等价**，差异只在发布设施与文档。

### 新增

- [`.github/workflows/docker.yml`](.github/workflows/docker.yml)：打 `v*` 标签时自动构建并推送
  `ghcr.io/nas200-timmy/hdsl-web`（语义化标签 + `latest`）；手动 dispatch 产出 `edge` 用于验证/尝鲜。
  认证用仓库自带的 `GITHUB_TOKEN`（`packages: write`），任何机器都不需要登录或存放 PAT；
  构建带 GHA 层缓存，后续构建明显加速。版本号透传进 jar，镜像里 `-version` 与镜像标签一致。
- Dockerfile 增加 OCI 标签：`org.opencontainers.image.source`（让 ghcr 上的包自动关联本仓库，
  也是 `GITHUB_TOKEN` 有权推送同名包的前提）、`description`、`licenses`（GPL-3.0-only）。
- README 增加 ghcr 徽章与一行式拉取指引：`docker pull ghcr.io/nas200-timmy/hdsl-web:latest`。

### 验证

- `:edge` 构建通过（Actions run 成功），并以**匿名令牌**校验仓库清单：`GET /v2/nas200-timmy/hdsl-web/manifests/edge` → **HTTP 200**（公开可拉取）。

## v0.1.3 — 2026-10-03 · 暂时稳定版

修好 dsh 界面「历史加载失败、界面一直闪」，并整理一份会话交接文档。**本版为当前暂时稳定版**，日常使用选它。

### 修复

- **反代 WebSocket 中继的 64 KiB 上限**：面板把 `/i/*` 的 WebSocket 升级中继到实例端口，
  用的是 Jetty 默认上限（frame / text / binary 均 65536 字节）。dsh 的一份会话历史、一条
  工具结果、一个粘贴的附件都是一整条 WS 消息，轻易超过 64 KiB——一超，中继这一跳就以
  1009（message too large）失败、随即以 1011 掐断两端，dsh 客户端报
  `api gateway: Remote stream WebSocket closed`（gateway/internal）并反复重连，
  表现为「界面一直闪、历史永远加载不出来」。dsh 自己（node 的 ws）默认允许 100 MiB，
  瓶颈一直在面板这一跳。现在两侧——服务端 upgrade 容器（浏览器→中继）与转发用的
  WebSocketClient（中继→dsh）——的 frame / text / binary 都抬到 32 MiB（覆盖 dsh 实际
  产生的 payload，同时仍给"让面板无限分配内存"的对端一个上限）；中继非正常关闭时写一行
  日志（实例、状态码、原因），此类失败不再只能从浏览器控制台猜。

### 文档

- 新增 [`docs/session-handoff-2026-10-03.md`](docs/session-handoff-2026-10-03.md)：
  当天的会话交接文档——问题、排查结论、五次改动、关键环境事实与复现配方、刻意没做的事、
  当前 git 状态与待办。
- 文档与测试里的示例域名 / 内网地址 / 宿主路径统一为示例值（`dsh.example.com`、
  `192.0.2.x`、`/srv/...`）；`DshReadinessLineTest` 的示例私网 IP 换成 TEST-NET 段。

### 验证

- WS 上限 A/B（假 dsh 单发 1 MiB 帧）：修复前两个方向都 `CLOSE 1011 bytes=0`，
  修复后都 `RECV frame #1 1048576 bytes` 完整送达。
- 全量 **421 用例 / 0 失败 / 0 跳过**。

## v0.1.2 — 2026-10-03

上手配置时暴露的两个问题：账户加不进去，dsh 的工作区落在容器自己的目录里。

### 修复

- **账户添加必然失败**：`GET /api/vendors` 的 `kinds` 按设计报的是「供应商说的协议」
  （`openai-completions` 之类），面板却把它当账户类型随创建请求发了出去，被
  `/api/accounts` 以 `kind must be one of official, third-party, offline` 拒绝——
  从列表里选供应商添加 100% 失败。现在面板不再发 `kind`（类型由后端按供应商推断
  official / third-party），协议只作只读信息显示；后端拒收时点名收到的值。
- **工作区是启动器自己的实例目录**：此前新建实例的「工作区」（会话读写文件、跑命令
  的目录，也是 dsh 进程的工作目录）取的是 `<data>/hdsl/instances` —— 启动器放实例
  清单与安装的地方。现在启动时把 `<home>/workspace`（镜像里 `/home/hdsl/workspace`，
  可用 `HDSL_WORKSPACE` 覆盖）注册成选中目录，新实例默认落在它上面；该目录一直存在，
  挂不挂由部署决定：不挂是容器内目录，挂了就是宿主目录或命名卷。
- **dsh 界面「历史加载失败」并一直闪**：报错是 dsh 客户端的
  `api gateway: Remote stream WebSocket closed（gateway/internal）`。根因在面板的
  WebSocket 中继：它用的是 Jetty 的默认上限——**单帧 / 单消息 64 KiB**，而 dsh 一份
  会话历史、一条工具结果轻易超过；超限时中继把整条连接掐断（1009 → 继电器自己报
  1011），dsh 客户端于是重连、重试，界面就一直闪而历史永远加载不出来。现在两侧
  （服务端 upgrade 容器与转发用的 WebSocketClient）统一提到 32 MiB（dsh 自己的服务端
  上限是 100 MiB），并让中继在非正常关闭时写一行日志，下次同类问题在容器日志里可见。

### 部署

- compose 里两个挂载点写在明面上：`/data`（必须持久化：口令 / 配置 / 证书 / 实例 /
  pnpm store）与 `/home/hdsl/workspace`（可选，工作区；注释给了宿主目录与命名卷两种写法）。
- README「数据与挂载点」补上两个挂载点的分工与工作区说明，环境变量表新增
  `HDSL_WORKSPACE`。

### 工程

- 测试：**421 用例 / 0 失败 / 0 跳过**。新增工作区解析规则用例、「新建实例取选中
  目录当工作区」的接口用例，以及账户契约回归用例（供应商路径不带 `kind` → official；
  协议当 `kind` → 400 且点名）。
- 实机验证：绑定挂载宿主目录后，新建实例记录 `workspace=/home/hdsl/workspace`，
  启动的 dsh 进程 `cwd` 即该目录。
- 中继上限的 A/B 实测（假 dsh 只吐一个大帧）：修复前单个 1 MiB 帧直接断开
  （`1011 upstream connection failed`，客户端收到 0 字节）；修复后两个方向都完整
  收到 `1048576` 字节。

## v0.1.1 — 2026-10-03

修复实机使用中暴露的"实例经常无法启动"系列问题，并把安装编排补成启动器该有的样子。
排查结论：dsh 的版本管理内核（npm 取版本、pnpm 按实例安装、pnpm store 复用）没有坏，
坏的是 Web 层的编排逻辑与前后端状态契约。

### 新增

- **创建即安装 / 启动补装**：`POST /api/instances` 默认 `autoInstall:true` 并返回
  `installTaskId`；对未安装实例点「启动」会先安装再启动（202 `installing`），传
  `autoInstall:false` 可退回旧语义。
- **状态契约补全**：实例状态新增 `NOT_INSTALLED` 与 `INSTALLING`（判定顺序：活进程 >
  停止中 > 安装任务 > 未安装 > 失败 > 已停止；活进程优先，让"装完自动启动"的窗口期
  报 `STARTING` 而不是 `INSTALLING`），安装开始/结束时广播状态事件——此前前端 6 处
  按这两个状态写的引导 UI 全是死代码。
- 新增 [docs/launcher-orchestration.md](docs/launcher-orchestration.md)：诊断过程、
  状态机、契约变化与取舍。

### 修复

- **版本漂移**：详情页版本选择器不随实例重置（路由不重挂组件），会把 A 实例的选择
  提交给 B；改为随实例重置。后端 `/install` 成功后不再无条件把清单改写成"请求的
  版本"，而是**回读磁盘**上的 `package.json`，与请求不符时按真实版本记录并把任务
  判失败（宁如实，不谎报）。
- **安装失败不可见**：pnpm 输出此前只进内存任务表，失败原因随面板重启消失；现在写入
  容器日志与任务记录，并对 `ERR_PNPM_FETCH_404` / `ERR_PNPM_NO_MATCHING_VERSION`
  追加中文提示（点名上游已删除的包，说明重试无用）。
- **容器一直 unhealthy**：HTTPS 下探针访问 `https://127.0.0.1` 触发 Jetty 的 SNI
  主机校验（400 Invalid SNI），服务本身是好的；两个 TLS 连接器关闭该校验（单证书
  面板，校验只会误伤本机探针）。现在 `curl -k https://127.0.0.1:3080/api/health`
  直接 200。
- **npmmirror 尾斜杠导致 corepack 404**：`NPM_REGISTRY` 带尾斜杠时 corepack 拼出
  `mirror//pnpm/latest` → 404，而 compose 注释里推荐的就是带斜杠的写法；入口脚本
  归一化后再写 pnpm 配置与 `COREPACK_NPM_REGISTRY`。
- **中文日志在 `docker logs` 显示 `?????`**：镜像未设 `LANG`，JVM 的
  `stdout.encoding` 退化成 ASCII；镜像加 `LANG=C.UTF-8`。
- 实例运行中禁止切换版本；安装中禁止改名。

### 工程

- 任务 REST 契约补 `instanceId` 字段（WS 事件早就有 `instance` 字段，只有 REST 与
  前端类型缺它，导致前端任务匹配退化成"最近活跃任务"）。
- **构建产物不再入库**：`src/main/resources/web/` 是 `web/dist` 经 `syncWebDist`
  生成的副本，仅因历史原因被跟踪，且会随源码更新而陈旧；改为生成物并加入
  `.gitignore`（Docker 构建、CI、本地构建都会重建它）。
- 测试：**415 用例 / 0 失败 / 0 跳过**（挂 node + pnpm 实跑，含原本被跳过的 21 个）；
  fake-pnpm 支持 `9.9.9-slow`、`9.9.9-mismatch` 两个特殊版本；新增
  `InstallFailureHintTest` 及"创建即安装 / 启动补装 / INSTALLING 可见 / 回读不符
  判失败"等用例。
- 实机端到端验证：创建 → `NOT_INSTALLED`，启动 → 202 `installing` → `INSTALLING` →
  `STARTING` → `RUNNING`；复现上游删包场景（`0.0.1-rc.1`）确认中文提示落到任务错误
  与容器日志。

## v0.1.0 — 2026-10-03

首个版本：把 [HDSL](https://github.com/MCXCC303/HDSL) 桌面版搬进浏览器的单容器实现。

- 登录门 + 首次启动初始化引导（自建用户名/密码），面板内支持修改密码
- 界面按桌面版像素级复刻（Material You 主题、同款图标与动画、亮暗跟随系统）
- 创建实例 → 一键启动 → 面板内进度条与实时日志 → 自动开新标签页
- 同端口、同证书反代 dsh 网页（剥前缀 / Host 透传 / Cookie Path 改写 / WebSocket 中继）
- 完整管理功能：版本 / 实例 / 插件 / 账户 / 整合包 / 会话 / 技能 / 体检 / ACP 控制台
- 声明式 HTTPS：`server.yaml` / 环境变量，首启自签，面板上传证书热切换
- 交付：单可执行 fat jar + Debian 容器镜像（内置 OpenJDK 21、Node 22、pnpm）
