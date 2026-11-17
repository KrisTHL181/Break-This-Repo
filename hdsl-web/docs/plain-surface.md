# 无 JavaScript 保底页（`/plain`）

状态：P1 已实现。目标读者是 w3m / lynx、Windows 7 上的旧浏览器（含 IE11），以及任何
加载 2.8 MB bundle 会白屏的环境。它不替代聊天页，保证“还能登录、看会话、发消息、
下载附件”，并且内容能自己更新、markdown 与完整面板同观感。

三种客户端档位，同一套路由：

| 档位 | 能力 | 刷新方式 |
|---|---|---|
| 无 JS（w3m / lynx / 关 JS） | 表单 + 链接 | 整页 meta refresh（`?auto=1`）或手动刷新 |
| ES5（IE11 等） | 上述 + `poll.js` 长轮询换 fragment | 区域级自动更新，不闪、不丢输入 |
| 现代浏览器 | 走 SPA，不经过这里 | `/ws` 流式 |

## 1. 入口闸门

`web/index.html` 在 `<head>` 里先跑一段 ES3 语法的脚本，早于 bundle：

- 检查 `fetch`、`WebSocket`、`Promise`、`CSS.supports("display", "flex")`；
- 缺任何一项 → `location.replace(<base>/plain/old)`，不下载 SPA；
- `<noscript>` 用 `meta refresh` 直接去 `<base>/plain`（关 JS 的人要的是能聊天，不是看说明）。

`/plain/old` 是友善提醒页：说明“不是服务器挂了”，给出两个选择——继续用纯文本版，
或换现代浏览器。Windows 7 上建议 Firefox ESR 115 / Chrome 109。

服务端还有一层 UA 兜底（`StaticServlet`）：w3m / lynx / links / elinks 发 `/` 时 302 到
`/plain`，`MSIE` / `Trident/` 302 到 `/plain/old`。这层是给“JS 被禁用”准备的——IE ≤ 9 不解析
`<head>` 里的 `<noscript>`，只靠 meta refresh 不可靠；`<body>` 里另有一个可见的纯文本版链接。
其他浏览器一律发 SPA 壳，交给内联闸门判断，避免 UA 猜测误伤现代浏览器。

限制：不 eval 就无法可靠探测“ES2017 语法”，`fetch + WebSocket + flex` 是代理指标。
真要严格，只能靠 `nomodule` 兜底或 UA 判断，代价更高，这一版只做了上面那条窄兜底。

## 2. 路由

所有路径都带 `basePath` 前缀（`HDSL_BASE_PATH`，例如 `/panel`）。

| 方法 | 路径 | 作用 |
|---|---|---|
| GET | `/plain` | 会话列表 + 新建会话；未登录时是登录表单 |
| GET/POST | `/plain/login` | 登录表单 / 表单登录（`username`、`password`） |
| POST | `/plain/logout` | 清 cookie，回登录页 |
| GET | `/plain/old` | 浏览器太旧的提醒页（公开） |
| GET | `/plain/poll.js`、`/plain/legacy.css` | ES5 轮询器与 legacy 样式（公开静态资源） |
| GET/POST | `/plain/elevate` | 提权状态页 / 开发者密码表单 |
| POST | `/plain/elevate/extend` | 花一次性延期券到 3 小时 |
| POST | `/plain/elevate/stop` | 结束本次提权 |
| POST | `/plain/new` | 用 `assistantId` 新建会话，303 到会话页（**需提权**） |
| GET | `/plain/s/{id}` | 会话历史 + 发送表单；`?auto=1` 开启 meta 自动刷新 |
| POST | `/plain/s/{id}/send` | 表单 `text`，提交后 303 回会话页（**需提权**） |
| GET | `/plain/f/sessions` | 会话列表 fragment；`?wait=1&rev=` 长轮询 |
| GET | `/plain/f/s/{id}` | 转录 + 发送区 fragment；`?wait=1&rev=` 长轮询 |

`/plain` 不注册到 `AuthFilter`：过滤器对未认证的 HTML 请求回 JSON 401，文本浏览器会
直接把那段 JSON 显示给用户。Servlet 自己校验会话，未登录时渲染登录表单。

## 3. 观感：HMCL 式外壳

legacy 表面不是白底裸表格：每个页面都是「壁纸 + 双栏表格壳」。

- 壁纸用面板默认那张（`assets-img/wallpapers/2021-08-26.jpg`，与 SPA 的 `DEFAULT_WALLPAPER`
  同一张），由 servlet 以 inline `<style>` 注入 `background-size:cover` 等 IE9+ 就支持的属性；
  SPA 里「选哪张」存在 localStorage，是设备偏好，服务端不知道，所以 legacy 固定穿默认这张。
- 外壳是 `<table class="shell">` 两栏：左栏导航卡片（会话 / 完整面板 / 提权状态 / 退出），
  右栏内容卡片。用表格不用 flex：IE11 的 flexbox 是坏的，而表格在 w3m 里正好渲染成可读的
  文本网格——同一份标记，图形浏览器看到卡片，文本浏览器看到对齐的列。
- 卡片化靠 `border-radius` / `box-shadow` / `rgba()`（均 IE9+）；表格、消息块、代码块、按钮
  都走 `web-legacy/legacy.css` 的 HMCL 配色（蓝色 accent、白卡片、灰蓝描边）。文档里没有任何
  CSS 变量或 flex，`legacy.css` 全文件过不了 es-check 但过得去 IE11 的 CSS2.1+ 子集。
- 两种模式（增强 / 降级，见第 8 节）共用这套观感；开关只决定自动更新与服务端 markdown，
  不决定长什么样。登录页、错误页、提权页同样穿外壳（单栏）。
- 文本浏览器不认样式表，所以布局信息必须写在标记里：导航逐项 `<br>`、提交按钮用
  `<input type="submit">`（w3m/lynx 对 `<button>` 的提交支持不稳）、外壳与会话表带
  `width` 属性。w3m 里看到的是"左栏四行导航 + 右边列对齐的会话表"，不是一串挤在一起
  的链接。
- CSP 因此放行 `img-src 'self'`（壁纸）与 `font-src 'self'`（KaTeX 字体），其余不变。
- **公式**：服务端用与 SPA 相同的 remark-math + rehype-katex 预渲染，HTML+MathML
  一起下发；KaTeX 的样式与字体从 jar 里发（`/plain/katex/…`，IE11 走 woff，
  现代浏览器走 woff2），不需要脚本。没有 JS 的客户端看到的是公式的文本/HTML
  近似——不会退化成 `$…$` 源码。
- **IE11 可用功能清单**（权衡后的边界）：聊天（读写）、自动更新、公式、
  **模型切换**（会话页的 `<select>` 表单，w3m 也能提交）、会话**导出**、
  会话**分叉**、提权；**导入**仍建议在完整面板做（文件选择与错误反馈在旧浏览器
  里不值得再造一套）。模型列表来自会话所属账户；账户或列表拉不到时选择器
  只保留当前模型，不影响发送。
- **打开即到末尾**：会话列表的链接带 `#bottom`，会话页顶部另有「跳到末尾 ↓」，
  发送/分叉后的 303 也带 `#bottom`——w3m 与 IE11 都不会停在最早的消息上、
  看不到输入框。

## 4. 登录与表单安全

- 复用 `AuthService.login` 与 `AuthFilter.sessionCookie`：`HttpOnly`、`SameSite=Strict`、
  子路径挂载时 `Path` 跟着挂载点走。
- 登录失败与 `ApiServlet` 同口径：15 次失败 / 5 分钟窗口 / 封 10 分钟（`LoginThrottle`），
  写 `audit.jsonl` 的 `login` 记录。
- 所有 POST 校验 `Origin`／`Referer` 与 `Host` 一致；两者都没有（w3m、curl）时放行，
  因为 `SameSite=Strict` 已经挡住跨站表单。文本浏览器可能两者都不实现 SameSite 与
  Referer，所以**花钱的动作另有提权闸**（下一节）。
- 响应头固定：`Cache-Control: no-store`、`nosniff`、`X-Frame-Options: DENY`。
- CSP 两档：纯无 JS 页 `default-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self';
  form-action 'self'; base-uri 'none'`；带轮询器的页再加 `script-src 'self'; connect-src 'self'`。
  `img-src` 是壁纸需要的唯一开口；注入进 fragment 的 `<script>` 或内联事件在两档下都跑不起来。
- 所有动态内容经 `PlainHtml.escape` 转义；markdown 走服务端渲染器，输出再过一遍与 SPA
  相同的 rehype-sanitize schema。

## 5. 提权闸与踢出信号

这个表面能从忽略 `SameSite` 的浏览器访问，登录 cookie  alone 不足以花 operator 的 token：

- `/plain/new` 与 `/plain/s/{id}/send` 要求**活跃的开发者提权**（`DeveloperElevation`，
  与 JSON 路由同一个 key：`token:<sessionToken>`）；读视图（列表、转录、附件链接）仍只需登录。
- 提权窗口 30 分钟；`/plain/elevate/extend` 花一次性延期券到 3 小时，第二次延长要重新输
  密码——与 `POST /api/developer/extend` 同语义。提权只走 HTTPS（`RequestSecurity.isSecure`）。
- 未提权时发送表单被换成「需要先提权」的提示与提权页链接；提权页有密码表单、延期与停止按钮。
- **踢出信号**：每个页面与 fragment 的根节点带 `data-elev="ok|no"`。以 `ok` 开始的页面收到
  `no` 的 fragment 时，`poll.js` 换入该 fragment（发送区变成提权提示）并停止轮询；以 `no`
  开始的页面继续轮询（只读自动刷新仍然有用）。
- `config.auth.disabled`（仅回环的零认证模式）没有登录可言，也就没有提权可言：该模式下
  闸直接放行，与“回环即信任”的既有口径一致。

## 6. fragment 与轮询器

- `GET /plain/f/sessions`、`GET /plain/f/s/{id}` 返回 HTML fragment（不是整页），根节点带
  `data-rev`、`data-elev`、`data-poll-url`，响应头带 `X-Rev`。
- `rev` 是该区域渲染载荷的短哈希（会话 `updated_at`/内容 + 提权状态）。带 `wait=1&rev=<旧值>`
  的请求在服务端挂住，每 500ms 重算一次，rev 变了立即返回，20 秒没变回 304。
- `src/main/resources/web-legacy/poll.js` 是纯 ES5（`es-check es5` 门禁）：XHR 长轮询、
  按 id 换区域；`frag-sendform` 只在 innerHTML 真的变化时才替换，所以正在输入的文字不会被
  刷新冲掉；401/403/404 停轮询。
- 无 JS 档位退回整页 meta refresh（`?auto=1`，5 秒），放在 `<noscript>` 里，两种档位互不干扰。

## 7. 服务端 markdown

- `web/scripts/render-markdown.mjs` 在构建期被 esbuild 打成单文件
  （`web/dist-ssr/render-markdown.cjs`，gradle 同步进 `src/main/resources/web-legacy/ssr/`），
  运行时由 `MarkdownSsr` 解包到 `<data>/plain-md/`，用 Node 执行：**优先面板自己托管的
  运行时** `<data>/hdsl/runtimes/*/node`（按 mtime 取最新），找不到才回落到 PATH。
  Windows 裸部署的托管 Node 不在 PATH，这条顺序是它也能预渲染的原因。
- 渲染管线与 SPA 完全同源：react-markdown + remark-gfm + rehype-sanitize（共享
  `web/src/pages/chat/markdown-schema.ts` 的 schema 与 citation 变换）+ rehype-highlight；
  代码块只渲染语言标签，不渲染复制/下载/换行按钮（无 JS 环境无意义）。
- 每条**定稿**的助手消息渲染一次，缓存为 `<data>/plain-md/<messageId>-<sha256(content)>.html`；
  regenerate / edit 改变内容哈希即 miss。流式中的消息仍按转义 `<pre>` 显示。
  渲染是**异步**的：turn 结束的 finalizer 直接触发 prefetch，`render()` 只读缓存、不阻塞请求；
  首次打开才会遇到的旧消息也会在后台补齐，下一次刷新换成渲染后的 HTML。
- 任何失败（无 Node、无 bundle、渲染崩溃、10 秒超时、输出超 4 MB）都降级为转义 `<pre>`：
  legacy 表面不会因为“漂亮渲染失败”而丢掉消息。缓存超过 2000 个文件时按 mtime 清最旧。
- 进程并发默认 4，可在 `server.yaml` 的 `plain.ssr_workers` 调到 1–16；每个进程由 watchdog
  在 10 秒后强杀，stdout 读取不会再无限占住请求线程。
- `GET /api/settings/plain` 返回 `ssrAvailable`，设置卡片显示「Markdown 预渲染：可用/不可用」，
  没有 Node 时不再静默降级。

## 8. 聊天循环

发送直接调 `ChatTurnManager.send(...)`，和 SPA 是同一条 turn、同一份 SQLite 快照：

1. 表单 POST → 303 到 `/plain/s/{id}?auto=1`；
2. 会话页从 `ChatService.sessionJson` 读节点与当前变体；
3. 回复生成中显示“正在生成回答…”；ES5 档由 poll.js 区域更新，无 JS 档靠 meta refresh；
4. 页面上始终有「刷新」与「自动刷新：开/关」链接，手动刷新不依赖 meta refresh；
5. 生成结束（`streaming=false`）后 meta refresh 自动消失。

已知取舍：

- 没有 token 级流式，只有每秒落库的快照。
- 不渲染思考过程与搜索时间线。
- 附件只给下载链接（`/api/chat/attachments/{id}`，走同一会话 cookie），不做上传。
- **终端/TUI 不在本表面**：无 JS 与 IE11 只给聊天的只读/发送能力，实例终端仍要求现代浏览器。

## 9. 增强开关（`plain.enhanced`）

设置 → 通用 的「纯文本版（legacy 表面）」卡片（或 `server.yaml` 的 `plain.enhanced`、
`PUT /api/settings/plain`，写要开发者提权）在两种形态间切换，立即生效、不用重启：

- **开（默认）**：fragment 长轮询自动更新、ES5 轮询器、服务端预渲染 markdown——本文
  第 5、6 节的全部能力；卡片同时显示预渲染是否真的可用（`ssrAvailable`），没有 Node 时
  正文会退回 `<pre>`，但其余增强照常工作；
- **关**：单纯降级页——表单 + 整页 meta 刷新、正文按转义 `<pre>`、`/plain/f/*` 回 404、
  所有页面回到 `default-src 'none'` 的严格 CSP。

两种模式下**发送与建会话都要开发者提权**（第 4 节），开关只决定"好不好用"，不决定
"能不能花钱"。没有 legacy 客户端的部署可以关掉它，把 legacy 表面收成一个安静的保底页。

Markdown 预渲染的并发默认是 4 个 Node 进程，可在 `server.yaml` 的 `plain.ssr_workers`
调到 1–16。每个进程仍是短生命周期，watchdog 10 秒强杀；调高只影响后台预渲染速度，
不会让请求线程排队等 Node。

## 10. 代码位置

- `src/main/java/org/jackhuang/hmcl/web/plain/PlainChatServlet.java`：路由、表单、登录、提权闸、fragment；
- `src/main/java/org/jackhuang/hmcl/web/plain/PlainHtml.java`：HTML 骨架、转义、时间格式；
- `src/main/java/org/jackhuang/hmcl/web/plain/MarkdownSsr.java`：服务端 markdown 与缓存/降级；
- `src/main/java/org/jackhuang/hmcl/web/http/PlainSettingsApiServlet.java`：`/api/settings/plain` 增强开关；
- `web/src/components/PlainSurfaceCard.tsx`：设置 → 通用 的开关卡片；
- `src/main/resources/web-legacy/poll.js`、`legacy.css`：ES5 轮询器与 legacy 样式；
- `web/scripts/render-markdown.mjs`、`web/src/pages/chat/markdown-schema.ts`：SSR 与共享 schema；
- `web/index.html`：能力闸门与 `<noscript>` 跳转；
- `src/main/java/org/jackhuang/hmcl/web/server/HdslServer.java`：`/plain/*` 注册。

## 11. 验证

- `PlainChatTest`：登录表单发 cookie、公开提醒页、SPA 入口带闸门、
  `/plain/new` → 表单发送 → 同一回复在页面出现（假 provider 走完一轮）。
- `PlainLegacyTest`：未提权时发送被闸住并渲染提权页、提权/延期券一次性/停止后 fragment 带
  `data-elev="no"`、fragment rev 与长轮询唤醒、`poll.js`/`legacy.css`/两档 CSP、
  定稿消息走 SSR（有 Node 与 bundle 时）或降级 `<pre>`、增强开关关掉后表面退回降级页且
  开关写进 `server.yaml`。
- `scripts/api-smoke.py`：`/plain` 渲染表单、`/plain/old` 给出提醒。
- 手工：`w3m http://<panel>/plain` 应能看到登录表单与后续页面；IE11 打开应见区域自动更新。
