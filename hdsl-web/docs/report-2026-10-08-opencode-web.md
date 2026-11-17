# 报告：OpenCode 网页端「对话框打不开 / 加 key 失败 / 没有回复」三件事（2026-10-08）

> 对象：HDSL-web 面板里的第三方品牌实例（OpenCode 1.18.35）。
> 结论先说：**前两件是面板反代的三个缺陷，已经修好并部署验证；第三件是 OpenCode 自己的
> 模型 id 错配，面板改不了，换模型即可绕过。**

## 0. 一页摘要

| # | 现象 | 根因 | 状态 |
|---|---|---|---|
| 1 | 首页能看，点进 Settings / Providers / 任何对话框都是空白 | 面板只改写了 HTML 里的引用，没改客户端**运行时自己拼的根绝对 URL**（懒加载 chunk、worker、图标、CSS 字体） | ✅ 已修（`bc05241`，已部署） |
| 2 | 加 API key 点「继续」失败 | shim 用 `new Request(url, 原Request)` 改写请求，Chrome 对带 body 的 `PUT` 报 `ERR_ALPN_NEGOTIATION_FAILED`，面板连请求都收不到 | ✅ 已修（`bc05241`，已部署） |
| 3 | chunk 偶发 502 | 实例（Node/Bun）5 秒掐空闲 keep-alive，面板 JDK 客户端继续用那条死连接，请求死在它自己的 NPE 上 | ✅ 已修（`bc05241`，已部署） |
| 4 | 会话里发了消息，助手**永远不回复**（用户截图） | OpenCode 客户端请求的模型 id 是 `deepseek/deepseek-flash`（带提供方前缀，来自它内嵌的 models.dev 全量目录），服务器上只有裸 id `deepseek-flash` | ⚠️ **OpenCode 自身问题**，面板无关；改选模型即绕过（用户已用 Kimi 模型验证：一切正常） |

另外还修掉/澄清的：

- 早前那轮把地址栏从「面板根」改回 `/i/<id>/…`（提交 `e5e51b0`，同一晚部署）。
- 「OpenCode 为什么问服务器 URL」——那是它自己的**服务器列表**功能（文档
  [Web 界面 → See Servers](https://opencode.ai/docs/zh-cn/web/)），面板地址本来就在里面；
  之前看着像在逼你填地址，是因为第 1 条把那个面板整个弄空了。
- CSS 里的 `url(/…)`（字体）此前一直静默回退——第 1 条一并修掉，不再是已知限制。

## 1. 时间线与证据链

1. 用户报「加 DeepSeek key 点继续失败」「Settings 里只剩首屏」。
2. 复现（临时面板 + 无头 Chromium）：控制台报
   `Failed to load module script: Expected a JavaScript-or-Wasm module script but the server
   responded with a MIME type of "text/html"`；网络面板显示 Settings 的懒加载 chunk 全部请求
   `/assets/row-*.js` 这类**面板根路径**。
3. 定位到 OpenCode 打包用的 Vite 助手：`const ote="modulepreload",ate=function(e){return"/"+e}`
   —— 它把 chunk 名拼成根绝对路径；bundle 里另有 10 处 `"/assets/…"` 字面量与 CSS 里的
   `url(/assets/…)`，全都在面板根落地。→ 修法：反代 JS/CSS 时一并改写到挂载点下（见 §2 修复一）。
4. 修完第 1 条后 Settings/Providers 能渲染了，但「继续」仍然失败。用页面内四种写法做对照实验：

   | 写法 | 结果 |
   |---|---|
   | `fetch(url, init)` | 200 |
   | `fetch(Request)` 原样 | 200 |
   | `fetch(new Request(url, 原Request))` ← shim 当时这么写 | **ERR_ALPN_NEGOTIATION_FAILED** |
   | `fetch(url, {method, headers, body})` 显式重建 | 200 |

   → 修法：shim 改成从原 `Request` 的字段显式重建、body 从 `clone()` 读出（见 §2 修复二）。
5. 期间还抓到两个 chunk 的 502。面板日志：

   ```
   Upstream fetch failed for mount /i/<id>/: java.util.concurrent.CompletionException:
   java.lang.NullPointerException: Cannot invoke
   "jdk.internal.net.http.Http1Exchange$Http1RequestBodySubscriber.request(long)"
   because "this.bodySubscriber" is null
   ```

   实例是 Node/Bun（空闲 keep-alive 5 秒），面板 JDK 客户端把那条已被对方关掉的连接继续拿去用。
   → 修法：空闲连接 4 秒退休 + 可重放的请求失败后换一个「从未连过实例」的 client 重试一次 +
   带 body 的请求「放得下就缓冲、放不下才流式」（顺带修掉「无 body 的 GET 被当成带 body」）。
6. 用户接着报「对话没有回复」。查用户实例的数据库与日志：

   ```
   level=ERROR message="prompt_async failed" sessionID=ses_…
   cause="ProviderModelNotFoundError: Model not found: deepseek/deepseek-flash.
           Did you mean: deepseek-flash?"
   ```

   用户的三条消息（22:52、22:52、22:59）全是同一个错误：**用户消息存进去了、助手消息一条都没生成**
   —— 因为 agent 在开始前就被这个错误打断。
7. 用同一接口对照复现（只换 model id）：

   | 请求里的 `model.modelID` | 结果 |
   |---|---|
   | `deepseek/deepseek-flash` | **HTTP 500** + 同样的 `Model not found` |
   | `deepseek-flash` | **200** + 正常回复 |

8. 查来源：用户实例的配置文件是空的（`{"$schema": …}`），`/config` 里没有 `model` —— 所以
   不是配置来的。拉 `/provider`（6.6 MB）看到：OpenCode 内嵌 models.dev 全量目录，约 200 个提供方、
   数千个模型，很多 id **本身就带斜杠**，例如 `tensorx → deepseek/deepseek-v4.1-flash`、
   `hpc-ai → deepseek/deepseek-v4-flash`；而用户实际连上的 `deepseek` 提供方只有两个**裸 id**：
   `deepseek-flash`、`deepseek-v4-pro`。选择器里的 "DeepSeek V4.1 Flash" 来自目录里的条目，
   于是 id 对不上。
9. 用户改用 **Kimi 模型**后会话正常回复 —— 印证链路（面板 + OpenCode）没有别的问题，只有那个目录条目
   不匹配。

## 2. 三个已修缺陷（提交 `bc05241`）

### 修复一：反代时改写「运行时自己拼的根绝对 URL」

- **现状**：页面改写只处理 HTML 里的 `(src|href|action)="/…"`；客户端运行时拼的地址没人管。
- **修法**（`InstanceProxyServlet`）：
  - `patchBrandScript`：①把 bundle 里的 `"/assets/…"` 字面量改成挂载点下；②把 Vite preload 助手的
    `=function(<arg>){return"/"+<arg>}` 改成返回挂载点前缀（按形状匹配，两个标识符都是压缩过的）；
    ③顺带保留此前的「路由读取去挂载点」补丁。
  - `patchBrandStylesheet`：CSS 里的 `url(/…)`、`url("/…")`、`url('/…')` 改写到挂载点下
    （`url(//host/…)` 与 `data:` 不动）。
  - 两条资产改写各自独立：锚点找不到就只跳过它；补过的字节带 `Cache-Control: no-store`。

### 修复二：shim 改写 `Request` 的方式

- **现状**：`new Request(url, 原Request)` —— 请求体会变成流，Chrome 对带 body 的 `PUT` 直接
  传输层失败（`net::ERR_ALPN_NEGOTIATION_FAILED`），面板收不到任何东西。
- **修法**（`SHIM_JS`）：从原 `Request` 的字段（method / headers / mode / credentials / cache /
  redirect / referrer / integrity / keepalive / signal）显式重建，body 从 `input.clone().arrayBuffer()`
  读出；没有 body 的就直接重建。clone 失败（body 已被读过）时退回旧写法。

### 修复三：上游连接复用与请求体

- **现状**：实例 5 秒掐空闲连接，面板继续用死连接 → JDK 内部 NPE → 面板对浏览器答 502。
- **修法**：
  - 静态块里 `jdk.httpclient.keepalive.timeout=4`（比对方早一步退休空闲连接）；
  - 可重放的请求（无 body 或 body 已缓冲）失败后，用**新的 client**重试一次（新 client = 新连接，
    不会又踩同一条死连接）；
  - 请求体 ≤ 1 MB 时整体缓冲、带 `content-length` 发出（走 JDK 自带的 publisher），更大才流式；
    顺带修正：「`getContentLengthLong()` 为 -1 且没有 `transfer-encoding`」= 无 body（之前 GET 被当成
    带 body，导致重试被跳过、并给 GET 加了一层 chunked）。

## 3. 未修：OpenCode 自己的模型 id 错配

- **现象**：`ProviderModelNotFoundError: Model not found: deepseek/deepseek-flash`
- **性质**：请求 id 由**客户端**（网页端）根据它内嵌的模型目录拼出来，服务器只认自己 provider 表里的
  裸 id —— 这是 OpenCode 客户端/服务器之间的目录错配，**面板只是把它反代出来，改不了它发什么**。
  同一请求直连实例（不经面板）结果一样。
- **规避**（按成本从低到高）：
  1. 在模型选择器里选**已连接的** `DeepSeek` 提供方下的条目（裸 id 那两个），别选目录里其它提供方的
     同名模型 —— 界面上的差异就是提供方分组；
  2. 换用其它提供方/模型（用户已用 Kimi 模型验证正常）；
  3. 在面板里换一个 OpenCode 版本重启实例，绕开这版客户端的目录。
- **面板侧能做的**（可选，未做）：给实例写一份 `opencode.jsonc` 钉住默认模型。风险是配置里的
  `model` 字段本来就写作 `provider/model`，写不好反而更容易触发同一个错配，所以没有默认开启。

## 4. 启动配置：文档对照（结论：不用改）

对照 [OpenCode Web 文档](https://opencode.ai/docs/zh-cn/web/)：

| 文档项 | 面板现状 | 结论 |
|---|---|---|
| `--port <n>` | 预分配端口后传入 | 保留。文档说"默认随机可用端口"，但实测 `--port 0` 会静默回落 4096 |
| `--hostname` | `127.0.0.1` | 保留。**不给 `0.0.0.0`**：那会绕过面板登录门 |
| `--mdns` | 不用 | 刻意不做：等于开第二条入口 |
| `--cors` | 不用 | 不需要：页面与请求全在挂载点内同源 |
| `OPENCODE_SERVER_PASSWORD` | 不设 | 可选的加固项，见 §6 |
| 配置文件 | 不写 | 与本次问题无关（已确认用户的配置是空的） |

## 5. 验证（都可复跑）

- **单元/行为测试**：全量 **527 用例 / 0 失败 / 0 错误 / 0 跳过**（新增：bundle 补丁三类锚点、
  CSS `url()` 改写、入口脚本解析，以及「带 body 的请求穿过反代」——小 body 走缓冲、512 KB 走流式，
  两端逐字节比对）。
- **无头 Chromium（真实面板 + 真实流程）**：
  - Settings 的 Servers / Providers / Models **完整渲染**（修前只有首屏）；
  - 「Providers → Show more providers → 搜索 DeepSeek → 填 key → Continue」→ `PUT /auth/deepseek` 200、
    对话框关闭、key 落进实例 `auth.json`；
  - **零控制台错误、零失败请求、面板日志零上游失败**，连跑两轮一致。
- **事件流**：经面板抓 `/global/event` 一整段 29 KB，含 `message.part.updated` 与回复文本 ——
  SSE 通道正常。
- **接口对照**：同一 `POST /session/<id>/message`，`modelID=deepseek/deepseek-flash` → 500；
  `modelID=deepseek-flash` → 200 且回复 `pong`。
- **部署**：生产容器 `v0.2.6`、healthy、只发布 `3080`；从容器里取出 jar 核对，
  `patchBrandScript` / `patchBrandStylesheet` / preload 正则 / 重试与 keep-alive 常量都在。

## 6. 仍未做 / 待办

1. **推送**：本地领先 `origin/main` **6 个提交**（含本轮 `e5e51b0`、`bc05241`），一个都还没推。
   推送前建议把中间两条死路提交 `713a2e3` / `dfd89df` squash 掉（都是本轮未推送历史，重写安全）。
2. **可选加固**：给实例加 `OPENCODE_SERVER_PASSWORD`（随机口令），由面板代理往上游注入 Basic 头 ——
   回环端口不再裸奔（同宿主其它容器目前可以直接访问 `127.0.0.1:<实例端口>`）。属于"凭据注入"的例外
   （面板自己→实例这一段，浏览器无感），要不要做由你定。
3. **文档同步**：本报告之外，`CHANGELOG` 的 v0.2.6 条目、`docs/brands.md`、交接稿 §6.1 都已更新。
4. **观察项**：上游换打包方式时，资产改写锚点会失效（各自独立跳过，页面不会变壳）；探针判不出时
   路由 shim 会退回「地址还给面板根」的旧行为。
