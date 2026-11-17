# 会话交接：HDSL-web（2026-10-08，品牌实例：Kimi Code / OpenCode）

> 又一次 AI 协作会话的压缩交接：这一轮做了什么、查清了哪些机制、环境上踩过什么坑、
> 还剩什么没做。目标是**不用回看会话原文就能接着干**。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一用
> `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。README 徽章与 ghcr 包名里的仓库账号名
> `nas200-timmy` 属公开信息（仓库地址本身就是它）。

## 0. 当前状态

- 分支 `main`，HEAD `1694fdf`；**本地领先 `origin/main`（`9516798`）4 个提交，未推送**：
  `82f1642` 品牌实例特性 → `713a2e3` / `dfd89df`（中途方案，已被最后一条**全部收回**）→ `1694fdf`
  单端口最终形态。**push 前建议把中间两条 squash 掉**（都是本轮未推送历史，重写安全）。
- **未提交改动的清单以 §6 为准**（本节其余内容是上一轮的快照）：那次口径修正之后，同一会话又做完了
  「地址栏留在挂载点」，`InstanceProxyServlet`、`Brand`（注释）、`GenericPageRewriteTest`、
  `CHANGELOG.md`、`docs/brands.md` 都还在工作树里未提交，测试基线随之变成 **524 用例**。
- 生产容器已按最终代码重建：`v0.2.6`、healthy、**只发布 3080 一个端口**；
  部署会停掉运行中的实例，需回面板重新点「启动」（用户已把 opencode 实例重新跑起来）。
- 测试基线：**521 用例 / 0 失败 / 0 错误 / 0 跳过**。
- `CHANGELOG.md` 里 `v0.2.6` 条目承载本轮全部内容（v0.2.5 已发布，v0.2.6 还没打标签）。

## 1. 这一轮做完的事

| 提交 | 内容 |
| --- | --- |
| `82f1642` | `feat(brands)`：**Kimi Code 与 OpenCode 品牌实例**。`web/brand/`：`Brand` 描述符、`BrandCatalog`（registry 版本目录）、`BrandInstaller`（pnpm 装指定版本）、`BrandRuntime`（进程池/就绪行）、`BrandApiServlet`（`/api/brands/*`）；前端 `BrandsSection`（ZCode 页签保留实验声明，Kimi/OpenCode 带第三方警示条）+ 启动面板**跨品牌实例切换器**（点选=切换主按钮目标，带品牌标签）。插件管理与凭据注入刻意不做 |
| `713a2e3` | 中途方案：OpenCode 独立 origin 端口（3091–3100 端口段 + 边过滤器 + 端口注册表）。**已被 `1694fdf` 收回** |
| `dfd89df` | 中途方案：该端口改 nginx 式纯直通、过滤器提到鉴权之前。**同样已被收回**（其中「过滤器顺序」教训见 §2） |
| `1694fdf` | `refactor(brands)`：**单端口最终形态**——删掉端口段/边过滤器/端口注册表/`publicPort`，OpenCode 回到 `/i/<id>/` 挂载，额外注入一段**路由修正脚本**（见 §2.7）。compose 恢复只发布 3080 |

本轮动过的文件（`9516798..HEAD` 净变化 30 个文件，可用 `git diff --stat 9516798..HEAD` 复核）：

- **新增后端** `src/main/java/org/jackhuang/hmcl/web/brand/`：`Brand`、`BrandException`、
  `BrandInstance`、`BrandInstanceManager`、`BrandCatalog`、`BrandInstaller`、`BrandRuntime`、
  `BrandApiServlet`。
- **改动后端**：`web/proxy/InstanceProxyServlet`（通用页面改写 + `__hdsl-shim.js`/`__hdsl-router.js`
  两个资源端点）、`web/proxy/InstanceProxyTargets`（品牌分支）、`web/server/HdslServer`（挂 servlet +
  `BrandCatalog.warm()`）。
- **新增测试与夹具**：`web/brand/{BrandCatalogTest,BrandInstallerTest,BrandRuntimeTest,BrandApiTest}`、
  `web/proxy/GenericPageRewriteTest`、`src/test/resources/brand-pages/{kimi,opencode}-index.html`。
- **前端**：新增 `web/src/components/BrandsSection.tsx`；改动 `web/src/components/LaunchPane.tsx`
  （跨品牌切换器）、`web/src/actions.ts`、`web/src/api.ts`、`web/src/store.tsx`、`web/src/types.ts`、
  `web/src/i18n.ts`、`web/src/pages/MainPage.tsx`、`web/src/pages/InstancesPage.tsx`。
- **构建与文档**：`docker-compose.yml`（端口段已删）、`CHANGELOG.md`、`docs/brands.md`。
- **中途加过又被 `1694fdf` 删掉的**（别再找）：`BrandEdgeFilter`、`BrandPortRegistry`、
  `BrandPortRegistryTest`、`BrandInstance.publicPort`、`HdslServer.ensureBrandPort/brandsRoot/replay`。

## 2. 查清的机制（别再重查）

### 2.1 三个工具的形态（探索结论，2026-10）

- **Kimi Code**：npm `@moonshot-ai/kimi-code`（82 个干净 semver 版本，旧版实测可装）。分发包 =
  JS + **postinstall 拉原生二进制**（走厂商 CDN，纯离线装不了）。`kimi web --port --host` 起自带
  网页端；`--port 0` 可用（临时端口 + 就绪行播报 `Local: http://127.0.0.1:<port>/`）；启动打印
  bearer token，`--dangerous-bypass-auth` 官方文档写明用于 "behind your own authenticating proxy"
  （面板会话门就是），`--allowed-host <面板域名>` 防 DNS rebinding（对应 dsh 的 `--trusted-host`）。
  客户端**认子路径**。
- **OpenCode**：npm `opencode-ai`（944 个干净版本；registry 里另有 1.2 万个 snapshot/ci tag 要过滤）。
  `opencode web` 自带网页端；**`--port 0` 静默回落默认 4096** → 面板预分配端口（20000–60999 段，
  与 dsh 池 3081–4081 分开）；就绪行 `Web interface: …` **管道输出也带 ANSI 颜色码**（匹配前剥除）；
  **「就绪」要按端口真的在监听判定**（就绪行早于监听一拍，实测过 `/project` 502）；不设
  `OPENCODE_SERVER_PASSWORD` 则服务器裸奔（面板门即门）。
  客户端**按 `location.pathname` 路由**——直连任意子路径都只剩左上角壳（实证），所以必须给它
  路由修正脚本（§2.7）。
- **Claude Code**（仅探索，未接入）：无内置网页端；第三方 ACP 适配器（`@zed-industries/claude-code-acp`）
  可当聊天品类接入控制台。Codex：本地无 HTTP/网页端（`app-server` 只有 stdio/unix/ws 控制口）。

### 2.2 两份「同源外链脚本」是面板自己服务的（CSP 的坑）

- OpenCode 的页面带 `script-src 'self'`（+一个 sha256）→ **任何内联脚本都被浏览器拒**，且失败是
  **静默的**（脚本明明在 HTML 里，`window.fetch` 还是 native，运行期请求全 404 到面板根）。
- 因此 shim 一律由面板在每个挂载点下自服务：`/i/<id>/__hdsl-shim.js`（fetch/XHR/WebSocket/
  EventSource 前缀包装）与 `/i/<id>/__hdsl-router.js`（路由修正，见 §2.7）；页面改写只引用它们。
- 改这两个脚本要同时看 `InstanceProxyServlet`（`SHIM_RESOURCE` / `ROUTER_RESOURCE`）与 CSP 语义。

### 2.3 manifest 与「裸端口直通」的两个教训（代码已删，教训留着）

- 浏览器抓 `site.webmanifest` **不带凭证** → 过面板会话门必 401；通用页面改写里剥掉 manifest 链接。
- 边过滤器（按到达端口直通的方案）当时排错位置：`/api/*` 会话门**在它前面**，而 OpenCode 客户端
  自己用 `/api/...` 路径（`/api/session?limit=…` 等），于是经边缘 401、直连 200。**若以后再做
  「按端口/按 Host 直通」，它必须注册在任何面板路径过滤器之前。**

### 2.4 pnpm≥10 不跑依赖 postinstall

- 必须 `--config.dangerously-allow-all-builds=true`（沿用 dsh 安装同款），否则 Kimi Code 的
  postinstall 不执行 → 装完没有可执行文件。

### 2.5 版本目录与变更守卫

- `BrandCatalog`：`GET {下载源}/{pkg}` → `versions` 过滤 `^\d+\.\d+\.\d+$` 排序；缓存 30 分钟、启动预热。
- `PATCH /instances/{id}`：运行中改版本 409；目标版本未安装 400。

### 2.6 反代泛化（dsh 专用 → 任意根绝对 SPA）

- `InstanceProxyServlet.rewritePage`：dsh 的精确形状（`<base href="/">` + `/plugins/`）一字未改；
  其他页面走通用分支——`(src|href|action)="/…"` 加挂载点前缀、`<head>` 引用两个 shim 脚本、剥 manifest。
- 回归夹具是**真实抓的首页 HTML**：`src/test/resources/brand-pages/{kimi,opencode}-index.html`。

### 2.7 路由修正脚本（当前实现，正是下一步要升级的地方）

- 现状（`1694fdf`）：脚本把浏览器地址**还给根**——初次加载 `replaceState` 到 `/`（保留 hash），
  并让后续 `pushState/replaceState` 也停在 `/`。应用看到的 `location.pathname` 永远是 `/`，
  路由正常；请求仍由 `__hdsl-shim.js` 限制在挂载点内。
- **代价**：该标签页地址栏显示面板根 `/`，手动刷新回面板首页——这是用户最近指出的问题（§4.1）。
- 按品牌开关：`Brand.needsRouterShim()`（OpenCode=true；Kimi 认子路径，**不注入**——注入会与它
  自己的路由打架，有用例钉住这一点）。

## 3. 环境与验证配方（复用，省时间）

**2026-10-08 实际跑出来的记录**（当前交付状态，交接时别重复验证）：

- 测试：容器跑全量 → **521 用例 / 0 失败 / 0 错误 / 0 跳过**（`build/test-results/test/TEST-*.xml`
  共 88 个套件，结果文件比所有 `src/` 文件新；相对 `9516798` 净增 22 条、无删除）。
  `CHANGELOG.md` v0.2.6「验证」段原写的 523/26 是中途含「已删端口注册表用例」的旧口径，已改回 521/22。
- e2e（只发布 3080 的临时容器 + 无头 Chromium）：`/i/<id>/` 下 opencode 完整渲染
  Projects/Add project/Search sessions/Settings/Help；点「+」的行为与「直连实例回环端口」逐字一致；
  零控制台错误、零失败请求。
- 生产：`v0.2.6` healthy、`docker port` 只有 3080；部署 jar 与本地构建**逐字节一致**（python
  `zipfile` 比对），jar 内含 `__hdsl-router` 资源。

**测试**（宿主没有 java/node，一律走容器；基线 521）：

```bash
docker run --rm -v "$PWD":/src -v hdsl-gradle-home:/root/.gradle \
  -v /tmp/hdsl-test-node:/opt/node -v /tmp/hdsl-test-corepack:/corepack \
  -e COREPACK_HOME=/corepack \
  -e PATH=/opt/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  -w /src eclipse-temurin:21-jdk ./gradlew --no-daemon test -x buildWeb
```

- 必须 `-x buildWeb`（宿主 `web/node_modules` 属主不对，esbuild EACCES）；别用 `| head`（SIGPIPE）；
  偶发失败（如 `InstanceApiTest.installRecordsWhatPnpmActuallyLanded`）单跑即过，属负载抖动。

**前端**：`npm run typecheck && npm run build`（容器配方同上一轮交接稿）。

**临时容器 e2e**（独立卷 + 独立口令 + 复用生产 pnpm store）：

```bash
docker run -d --name hdsl-e2e -p 127.0.0.1:18080:3080 \
  -v hdsl-e2e-data:/data -v hdsl-web_hdsl-data:/prod \
  -e PNPM_STORE_DIR=/prod/pnpm-store -e NPM_CONFIG_REGISTRY=https://registry.npmmirror.com/ \
  -e HDSL_ADMIN_PASSWORD='<临时口令>' hdsl-web:e2e
```

- 镜像用 `docker build --build-arg NPM_REGISTRY=… --build-arg NODE_DIST_MIRROR=https://mirrors.cloud.tencent.com/nodejs-release -t hdsl-web:e2e .`（**别覆盖 `hdsl-web:latest`**）。
- 若 e2e 需要发布额外端口段，**宿主侧要错开**（生产可能占着同一批端口：`Bind for 0.0.0.0:3091 failed` 就是撞上了生产容器）。
- **无头浏览器对照法**（判定「代理的问题」还是「客户端的问题」最快）：同一套操作分别在「经 `/i/<id>/`」与
  「`--network container:hdsl-web` + 直连实例回环端口」各跑一遍，逐字比 bodyText/失败请求。
- puppeteer 配方：镜像 `ghcr.io/puppeteer/puppeteer`、`--network host`（或 `container:<面板容器>`）、
  `NODE_PATH=/home/pptruser/node_modules`、输出目录先 chmod 777、cookie 名 `hdsl_session`（值取
  `POST /api/auth/login` 的 Set-Cookie）。`opencode web` 安装包 355MB，pnpm store 命中后 20–40 秒装完。
- **opencode 探针**（查 bundle 用）：容器里 `pnpm add opencode-ai` → 起 `opencode web --port 45678`
  （约 1 分钟才就绪，要循环等日志里的 `Web interface`）→ node fetch 首页拿 `/assets/index-*.js` 再 grep。

**生产验证**：`curl -sk https://127.0.0.1:3080/api/health`；**别用 `grep` 在 jar 里找类内字符串**
（zip 压缩查不到）——用 python `zipfile` 读 class，或比对部署 jar 与本地构建逐字节是否一致。

## 4. 未决与待办

1. **下一步（用户最近的要求；本会话在探到一半时被掐断）**：让「打开」后的标签页地址栏**保持在
   `/i/<id>/…`**（或 `/opencode/` 这类假根），而不是现在的面板根 `/`。方案：**读时剥前缀 + 写时加前缀**——
   - 读：应用取 `location.pathname` 时得到剥掉挂载前缀的路径（它看到 `/session/x`）；
   - 写：应用的 `history.pushState/replaceState` 目标自动加上挂载前缀（地址栏显示 `/i/<id>/session/x`，
     **刷新、深链、后退都可用**）；
   - 实现候选：面板代理层对 `text/javascript` 响应做一次**文本替换**（把路由读取的表达式换成剥前缀版；
     锚点找不到就原样放行 + 记日志），并把 router shim 从「吞掉 pushState」改成「加前缀」。
   - **已探到的事实**：opencode bundle 里 `location.pathname` 只出现 **2 处**；第 1 处是 OAuth/auth
     辅助（`window.location.pathname.replace(/^\/+/,"/")+window.location.search` 那一段）；**第 2 处的
     上下文还没看到**（探针命令被打断）。第 2 处很可能就是路由的路径源——先看它。
   - 兜底：锚点不稳定就保持现状（面板根 + 刷新回面板），把锚点位置与「失效时只剩壳」的判别方法
     写进 `docs/brands.md`。
2. **推送**：4 个提交未推；建议先 squash `713a2e3`/`dfd89df`（死路）再 push。
3. 部署副作用照旧：`docker compose up -d --build` 停所有实例，回面板重启。
4. 上一轮交接仍挂着的旧待办：标题栏文案（`I18N.appFullName`）、实例工作区注册进 `workspace.json`、
   `/i/*` 中继上限 JUnit、`PATCH` 不支持改工作区、`--no-open` 探测超时被当「不支持」缓存、
   容器内那份 kimi 未持久化。
5. 已知限制（`docs/brands.md` 已写）：同 origin 的 `localStorage` 多实例共用；§4.1 完成前
   OpenCode 的地址栏/刷新代价仍在。

## 5. 给下一个功能的提醒

- 领域层 `org.jackhuang.hmcl.dsh` / `setting` 是桌面版逐字拷贝：改动落在 `org.jackhuang.hmcl.web.*`
  与 `web/`；本轮品牌相关全在 `web/brand/` + 前端 `web/src/components/BrandsSection.tsx`。
- 新增品牌 = 往 `Brand` 枚举加一项 + 在 `BrandRuntime.command` 补启动参数/就绪正则 + 前端页签；
  凭据与插件生态按「第三方自管」处理。
- 品牌页面依赖的两个外链脚本（§2.2）与页面改写是同一套机制，动一处要跑 `GenericPageRewriteTest`
  与 `ProxyBehaviorTest`。
- 提交风格：Conventional Commits 前缀 + 中文标题 + 详细 body（写清「改了什么、为什么、怎么验证的」）。

## 6. 2026-10-08 晚 · 追加：地址栏留在挂载点（已完成，未提交）

> 同一会话续做后追加。**当前状态以本节为准**：上面的 §0「未提交改动」与 §4.1 是上一轮的快照。

**做了什么**：让 OpenCode 的标签页地址栏**留在 `/i/<id>/…`**（用户原话：「起码也得有个独立根……
只要新建标签页的网址是正确的就行」）。机制是**读时剥、写时加**，两半必须成对：

- `InstanceProxyServlet` 新增 **bundle 补丁**：反代 JS 时把 Solid Router 读取地址的那一处表达式
  （`window.location.pathname.replace(/^\/+/,"/")`，实测 1.18.32 与 1.18.35 一致）换成先经
  `window.__hdslUnmount` 的版本（查找失败或出现两次都拒绝下刀）；补过的字节带
  `Cache-Control: no-store`——缓存里留一份没补丁的旧 bundle，会让客户端按挂载点本身路由。
- `__hdsl-router.js` 变成一副资源两个面孔（`?mode=root` 选兜底）：默认把
  `history.pushState/replaceState` 的目标**加上挂载点**（已在挂载点内不重复加）；`?mode=root` 是
  旧的「把地址还给面板根」。两份都定义 `__hdslUnmount`，补丁侧也写成带存在性判断的表达式，
  混搭不会 `ReferenceError`。
- 处理页面请求前先**探一次客户端的 bundle**（`entryScriptSrc` + `probeRouterBundle`，按挂载点 +
  资源名缓存；抓不到、非 200、超过 16 MB 都算探不到），探到锚点才注入默认 shim；探不到就注入
  `?mode=root`。这就是 `RouterShim{NONE,MOUNT,ROOT}` 与 `rewritePage(html, mount, RouterShim)` 的由来。
- 顺带确认：上游对未知路径**返回自己的 index 页**（`/myproject`、`/foo/bar/baz`、
  `/server/x/session/y` 都是 200 text/html），所以「刷新 + 深链可用」不需要面板再做什么；只有与
  它自己 API 同名的路径（`/project`、`/session/…`）会回 JSON——上游自己的行为，直连也一样。

**验证**（都可复跑）：

- 全量 **524 用例 / 0 失败 / 0 错误 / 0 跳过**（新增 3 条：补丁锚点与歧义拒绝、入口脚本解析；
  并把「补丁不碰客户端那处 `auth_token` 清理」并进补丁用例）。
- 临时面板（`hdsl-web:e2e` 镜像 + 独立卷 + 复用生产 pnpm store）→ 经 API 装 1.18.35 → 启动 →
  无头 Chromium：地址栏在 `/i/<id>/` 与 `/i/<id>/new-session` 都留在挂载点；页内
  `pushState('/zzz')` 得到 `/i/<id>/zzz`；把带挂载点的地址再写一次不会双前缀；深链
  `/server/…/session/…`、失效会话、刷新各项与「直连实例回环端口」**逐字一致**（连应用自身那 3 个
  500 `/api/reference` 都一样——直连也有，与面板无关）；零控制台错误。
- 代理侧 `curl`：页面带两个 shim 标签（router 不带 `?mode=root` ⇒ 探针命中）、bundle 确实被补、
  深链回 `200 text/html`、补丁响应带 `no-store`。

**未做 / 未决**：

- **未提交**：`InstanceProxyServlet`、`Brand`（仅注释）、`GenericPageRewriteTest`、`CHANGELOG.md`、
  `docs/brands.md` 都在工作树里（`git status` 可见），外加本交接稿本身。
- **未部署**：生产容器仍是上一版代码。重建会停掉运行中的实例，需用户点头后再
  `docker compose up -d --build`（`RELEASE_VERSION` 要与之前一致，否则版本号跳回 dev）。
- **兜底模式没有真实浏览器复验**：`?mode=root` 只有单测 + `curl` 验证。它等价于上一轮 e2e 验过的
  行为外加一行 `__hdslUnmount` 定义，风险低，但要复验就得造一个「bundle 不带锚点」的假实例。
- **已知小瑕疵**：CSS 里的 `url(/assets/…)`（OpenCode 的 `Inter.ttf`）没被改写，字体会静默回退
  （面板 SPA 兜底 200 HTML，不是失败请求）。要修就是在 `pump` 里对 `text/css` 做一次 `url(/…)`
  前缀替换，与 bundle 补丁同一套机制——注意别碰 `data:` URI。
- 上一轮 §4.1 里「写时加前缀 + 读时剥」的设想已按上面落地；`docs/brands.md`、`CHANGELOG.md` 的
  v0.2.6 条目已同步成最终形态。

### 6.1 同日第二轮回访（用户实测反馈触发，已修）

用户报「OpenCode 怎么问服务器 URL」+「加 DeepSeek key 点继续失败」。复现后挖出**三处代理缺陷**，
全部修掉并复验（细节在 `CHANGELOG` 的 v0.2.6 条目，机制与限制在 `docs/brands.md`）：

1. **运行时自己拼的根绝对 URL 没被改写**：页面改写只处理 HTML 里的引用；bundle 里的 `"/assets/…"`
   字面量（worker、图标）、Vite preload 助手的基址（`const <name> = function (dep) { return "/" + dep }`）
   与 CSS 里的 `url(/…)`（字体）全都打到面板根。后果是**首页能看、任何懒加载的对话框都是空白**——
   Settings / Providers / 所有 dialog 全中招。现在反代 JS/CSS 时一并改写（`patchBrandScript`、
   `patchBrandStylesheet`）。
2. **shim 重包 `Request` 的方式**：`new Request(url, 原Request)` 会让请求体变成流，Chrome 对这种带
   body 的 `PUT` 直接 `net::ERR_ALPN_NEGOTIATION_FAILED`（面板连请求都收不到）。改成按字段显式重建、
   body 从 `clone()` 读出。
3. **上游连接复用**：实例是 Node/Bun，5 秒掐掉空闲 keep-alive；池里那条死连接让请求静默失败
   （JDK 抛 `Http1RequestBodySubscriber` NPE，面板对它答 502）。现在空闲连接 4 秒退休、可重放的请求
   换一个「从未连过实例」的 client 重试一次、带 body 的请求「放得下就缓冲、放不下才流式」
   （顺带修掉「无 body 的 GET 被当成带 body」这件旧事）。

复验证据（都可复跑）：Settings 的 Servers / Providers / Models **完整渲染**；真实流程
「Providers → Show more providers → 搜索 DeepSeek → 填 key → Continue」→ `PUT /auth/deepseek` 200、
对话框关闭、key 落进实例的 `auth.json`；**零控制台错误、零失败请求、面板日志零上游失败**（连跑两轮
一致）。全量 **527 用例**全绿（新增的「带 body 的请求穿过反代」用例走的是真实 node stub）。

调查里省时间的两个坑：那串 ALPN 报错与面板无关（是 `Request` 重包的写法触发的 Chrome 行为）；
`curl` 复现不了连接复用类问题（每次都是新连接），必须在同一个浏览器会话里测。
