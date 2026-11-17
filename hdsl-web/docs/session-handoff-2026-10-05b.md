# 会话交接：HDSL-web（2026-10-05，下半场）

> 同一天的**第二次**会话交接：上一份是 [`session-handoff-2026-10-05.md`](session-handoff-2026-10-05.md)
> （那次交接之前的几轮：改用户名、ACP 控制台账户、手机端适配、关于页）。这一份覆盖它之后的两件事：
> **models.dev 模型目录**与**面板可选下载源 + ZCode 构建修好**。目标是**不用回看会话原文就能接着干**。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一用
> `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。下面出现的镜像源地址都是公开的。
> README 徽章与 ghcr 包名里的仓库账号名 `nas200-timmy` 属公开信息（仓库地址本身就是它）。

## 0. 当前状态

- 分支 `main`，本稿之前 HEAD 是 `61c24e8`（`docs(changelog): v0.2.2`）；**`v0.2.2` 已打标签发布**。
- **本次修复已部署**：`/i/<id>/` 反代下 dsh 网页打不开（「原地跳转」+ 白屏）的 bug —— 见 §1 末行与 §2.7。
- 部署会重建容器、**停掉正在跑的实例**（面板不会自动拉起）。本次部署时有两个在跑
  （`dsh-0.2.1-alpha.1` 的 profile web、`instance` 的 pokemon），部署后要回面板各点一次「启动」。
- ZCode 发行包已装成 release `3.14.3`，`/data/zcode/current` 指向它。
- 测试基线：**486 用例 / 0 失败 / 0 错误 / 0 跳过**。

## 1. 这一轮做完的事

| 提交 | 内容 |
| --- | --- |
| 本次（提交见 `git log`） | `fix(proxy)`：**反代挂载下 dsh 网页能打开了**。`Location` 重写 + 页面正文改写（`<base href="/">`、52 处 `/plugins/…`）+ 注入运行期 shim + gzip 解压；`ProxyBehaviorTest` +3 条回归用例。dsh 0.1.5-alpha.2 与 0.2.1-alpha.1 两个版本实机通过，486 用例全绿 |
| `b8552a2` | `feat(web)`：**models.dev 模型目录**。账户页的供应商列表接上 models.dev（服务端合并 dsh 自家目录与目录里的 226 家），可搜索、可刷新；「默认模型」从手写文本框变成可搜索下拉（带上下文与价格）；新增 `/api/models/*` 与缓存。测试 443 |
| 本次 | `feat(settings)`：**面板可选下载源（npm/pnpm 源）**；`fix(zcode)`：把「构建发行包」补到能真跑通 |

「下载源」那一块的要点：

- 设置页「下载源」卡片从 localStorage 摆设改成真设置：预设（跟随部署环境 / npm 官方 / npmmirror /
  中科大 / 腾讯云 / 华为云，每个带实测速度注记）+「自定义…」手填 + 行内校验 + **测速按钮** +
  「当前生效 + 来源（面板设置/环境变量/默认）」。
- `GET/POST /api/settings/registry`（`RegistrySettingsApiServlet`）：GET 回
  `{preset, registry, effective, source, presets[]}`；POST 收预设或自定义 URL，非法 400 **且设置不变**；
  `POST /api/settings/registry/test` 测一次可达性（5 秒超时）。
- 校验器 `dsh/NpmRegistry`：只允许 http/https、必须有主机名、禁止 userinfo/查询串/片段，以及空白、
  控制字符与 `" ' \` \ # $ ; | & @`，长度 ≤ 200、尾斜杠归一化。**写时拒绝、用时再规范化**。
- 生效三层（见 §2.1）：重写 pnpm 的全局 `config.yaml`（`dsh/PnpmConfigFile`，保留 `storeDir`、YAML 引号、
  原子替换、0600）+ 给子进程注入 `npm_config_registry`/`NPM_CONFIG_REGISTRY` + 面板自己起的 pnpm 显式
  `--registry=`（dsh 安装、ZCode 构建）。
- 设置项落 `launcher-settings.json`（`npmRegistryPreset` + `npmRegistry`）；`DshDoctor` 多一行
  `npm registry: <值> (<来源>)`。

ZCode 构建那一块：`Dockerfile` 补 `python3/make/g++`；构建器补 `pnpm exec tsc -b packages/shared`、
按所选源设 `ELECTRON_MIRROR`、失败时保留源码树、删掉那个从不生效的平台过滤、
给子进程继承启动器环境（代理终于对构建生效）；镜像源时设 `NODEJS_ORG_MIRROR`/`npm_config_disturl` 与 `JOBS`。

## 2. 查清的机制（别再重查）

### 2.1 pnpm 完全不读环境变量（这是「下载源」设计的根）

在运行中的容器里实测（pnpm 11.28.2）：

```
$ NPM_CONFIG_REGISTRY=https://a.example.invalid/ pnpm config get registry
https://registry.npmmirror.com/          ← env 被忽略
$ npm_config_store_dir=/tmp/zzz pnpm config get store-dir
/data/pnpm-store                         ← pnpm 不读 npm_config_*
$ env -u NPM_CONFIG_REGISTRY npm config get registry
https://registry.npmjs.org/              ← npm 读 env
$ pnpm install --registry=https://x/     ← 被接受；但 pnpm list --registry=… 报 Unknown option
```

- pnpm 的 registry 只来自 `${XDG_CONFIG_HOME:-$HOME/.config}/pnpm/config.yaml`（容器里没有任何 `.npmrc`），
  而那份文件是入口脚本按 `NPM_CONFIG_REGISTRY` 在启动时写一次。
- 所以「只给子进程注入 env」对 pnpm 系**无效**：dsh 实例安装、`dsh plugin` 内部起的 pnpm 都覆盖不到。
  要覆盖它们只能重写那份 config.yaml（或每个调用点显式 `--registry=`）。
- `PnpmConfigFile` 是逐行重写：**保留 `storeDir`**（丢了多实例就不共享包仓库了）、只重写 `registry` 一行。

### 2.2 启动器设置没有通用的 REST 通道

- `/api/settings/*` 之前**只有 `/tls`**（`TlsApiServlet`）；launcher 设置（代理、缓存目录、节点源…）
  没有任何端点，前端也没控件。
- 新增一个字符串设置项要动 **5 处**，漏一处就「能改、重启就丢」（`SettingsManager` 的注释里写着历史上丢过 17 个）：
  `LauncherSettings` 字段 → getter/setter（`proxyHost` 是样板）→ `Snapshot` 的 `@SerializedName` 字段 →
  `Snapshot.of` → `applyTo`。setter 会自动落盘（`save` 是变更监听）。
- 新 servlet 挂 `/api/settings/registry/*` 靠**更长的前缀**压过 `/api/settings/*`；`/api/*` 已有鉴权过滤器，
  不用自己写鉴权。

### 2.3 ZCode 构建的四个坎（全部实测过）

1. **镜像缺编译链**：`node-pty`、`cpu-features` 要现场编译，缺 Python/g++ 时报
   `Could not find any Python installation` / `Unable to detect compiler type`——面板自己那次也栽在同一处，
   对谁都跑不通。已补 `python3/make/g++`（约 +250 MB）。
2. **Electron 的 postinstall**：要从 GitHub releases 下约 100 MB 二进制，实测**直连 20 秒 0 字节**；
   `ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/` 实测 206/1MB/0.48s。构建器按所选预设设置它
   （只有 npmmirror 预设带这个镜像）。
3. **上游干净检出必然失败**：`Missing @zcode/shared dist files`。`packages/shared` **没有 build 脚本**，
   报错让你跑的 `pnpm build` 也不会编译它（`pnpm -r build` 跳过没有该脚本的包），**只有 `tsc -b` 会产出
   `dist/index.js`**——上游开发者本地「碰巧」跑过根 `typecheck`（它列了这个包）才有那个文件。
   构建器因此补一步 `pnpm exec tsc -b packages/shared` 并检查产物存在。
4. **两个不在 registry 覆盖范围内的慢点**：node-gyp 编译前要从 `nodejs.org` 下 10 MB 头文件
   （实测 **104 KB/s**，npmmirror 的 Node 镜像 **5.7 MB/s**），而头文件缓存在 `~/.cache/node-gyp`
   ——**不在卷里**，重建容器后第一次构建要重下；node-gyp 默认也**不给 `make` 加 `-j`**（12 核机器上单核编译）。
   用镜像源时设 `NODEJS_ORG_MIRROR`/`npm_config_disturl`，并按 CPU 数设 `JOBS`/`npm_config_jobs`
   （node-gyp 源码：`const jobs = gyp.opts.jobs || process.env.JOBS`，也认 `MAX`）。

### 2.4 那个「平台过滤」既不生效、生效了也是错的

`b2244bf` 曾往源码目录追加 `.npmrc` 的 `supportedArchitectures` 想只下本机架构。两点：

- pnpm 是**从 `pnpm-workspace.yaml` 读这个设置**、不看 `.npmrc`，所以它从未生效
  （证据：`pnpm-store` 里六个 `@mbears/opentui-core-*` 全在，都是那次冷安装下的）；
- 而一旦生效就会把构建搞坏：组包阶段（`scripts/zcode-distribution/assets.mjs`）要为
  **6 个平台**（darwin/linux/win × x64/arm64）收集原生资源，缺一个是**致命**错误。
  上游 `pnpm-workspace.yaml` 已经写着它需要的平台。因此这段已删——要恢复必须默认关。

### 2.5 镜像源实测（从面板容器内，`/typescript` 元数据）

| 源 | 结果 |
|---|---|
| `registry.npmmirror.com` | 0.60s，**25.2 MB/s** |
| `npmreg.proxy.ustclug.org`（中科大） | 0.85s，**17.6 MB/s**（要跟 302） |
| `mirrors.cloud.tencent.com/npm` | 1.60s，**13.6 MB/s** |
| `repo.huaweicloud.com/repository/npm`（华为云） | 1.45s，**10.9 MB/s** |
| `registry.npmjs.org`（官方） | 8 秒收不完，**28 KB/s** |
| `registry.yarnpkg.com` | 200 但 42 KB/s（它镜像官方，一样慢） |
| `mirrors.tuna.tsinghua.edu.cn/npm` | **404**（TUNA 不做 npm 镜像） |
| `r.cnpmjs.org` / `registry.npm.taobao.org` | 连不上 / 已死 |

→ 预设只放前四个 + 官方（官方保留为默认值，非国内部署仍正确）+「跟随部署环境」。

### 2.6 ZCode 的「构建」与「运行」是两件事

`ZcodeBuilder`（面板内构建发行包）与 `ZcodeRuntime`/`ZcodeInstanceManager`（起停实例）互不相干。
构建器把产物装进 `<数据目录>/zcode/releases/<版本>/`（`bin/zcode.mjs` 存在才算数），
并把 `current` 软链指过去；面板按 `HDSL_ZCODE_PACKAGE` → 否则 `<数据目录>/zcode/current` 找包。
构建实际用的工具链是 **Node 24.14（`/opt/node24/bin`，由 `HDSL_ZCODE_BUILD_BIN` 前置到 PATH）+ corepack 按
上游 `packageManager` 拉来的 pnpm 10.33.2**，跟面板自己那个 Node 22 / pnpm 11 不是一套（排查时别用错）。

### 2.7 反代挂载（`/i/<id>/`）与真实 dsh 网页端的三个不兼容

**结论：dsh 的真实网页端假定自己拥有整个 origin，塞进子路径必须由代理补三层。**（三处都实测过）

1. **它生成的页面把根写死**：`GET /i/<id>/` 回来的 HTML 开头是 `<base href="/">`，另有 52 处根绝对的
   `/plugins/??<包名>/client.js&rev=…`（`<link rel=preload>`、`<script src>`，以及内联
   `globalThis["__DSH_BOOT__"]` 里每个插件条目的 `url`）。经挂载点访问时这些请求落到**面板**头上，
   面板的 SPA 兜底把 `index.html` 当 JS 回 → 浏览器报
   `Failed to load module script: … responded with a MIME type of "text/html"` → **白屏**。
   页面是 **gzip** 的，改写前必须先解压，回给浏览器时要去掉 `content-encoding`/`content-length`。
2. **它自己拼的运行期 URL 也是根绝对**：`fetch("/api/…")`、`ws://<host>/api/remote.mux`（实时通道）、
   `/plugins/events`（SSE）。只改页面不够，必须在页面里注入一段 shim，把
   `fetch`/`XMLHttpRequest`/`WebSocket`/`EventSource` 包一层（入参是字符串、`URL` 对象、`Request` 三种都要处理）。
3. **token 兑换回 `303 Location: /`**：代理不重写它，浏览器就离开挂载点落到面板首页——
   「点打开 dsh 像在原地刷新」。`Set-Cookie: Path=/` 本来就会被改写成挂载点（这条一直没有问题）。

**上游那份参考实现不是这个问题的答案**：`deepseek-ai/deepseek-harness` 的默认分支是 **`master`**（不是
`main`，用 `main` 取 raw 会 404），`apps/web/tests/prefix-proxy.ts` 只做「剥前缀 / 保 Host / 去 hop-by-hop /
改写 `Set-Cookie` 的 `Path` / 裸转发 WS」——**它自己也不重写 `Location`、也不碰页面**，因为它伺候的
`apps/web/tests/scaffold.ts` 是个**知道前缀**的测试客户端：把挂载点当 `baseUrl`、自己用
`redirect: 'manual'` 处理 303（把 `Location: './'` 当「原地」）、所有路径用 `new URL(path, baseUrl)` 解析。
全仓没有 `X-Forwarded-Prefix`，dsh 的 Web app 也没有任何 base/prefix 旗标（旗标只有
`--host/--no-open/--port/--trusted-host`）。**所以那三层是启动器自己必须补的，不是移植漏掉的**——
`InstanceProxyServlet` 的类注释里也写了这段。

## 3. 环境与验证配方

**测试**（宿主没有 java/node，一律走容器；基线 483）：

```bash
docker run --rm -v "$PWD":/src -v hdsl-gradle-home:/root/.gradle \
  -v /tmp/hdsl-test-node:/opt/node -v /tmp/hdsl-test-corepack:/corepack \
  -e COREPACK_HOME=/corepack \
  -e PATH=/opt/node/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  -w /src eclipse-temurin:21-jdk ./gradlew --no-daemon test -x buildWeb
```

- 必须 `-x buildWeb`（宿主 `web/node_modules` 属主不对，esbuild EACCES）；不带两个 `/tmp` 挂载时依赖 node
  的用例会被跳过、看着全绿其实没跑；**别用 `| head`**（SIGPIPE 会让 docker 客户端退出而容器继续跑，占住
  gradle 卷锁，下一次报 "Timeout waiting to lock journal cache"）。
- 汇总用例数：`build/test-results/test/*.xml` 里 `tests/skipped/failures/errors` 求和。

**前端**：`npm run typecheck && npm run build`（`vite build` **不做类型检查**，两个都要跑），走
`node:22-bookworm-slim` 容器 + `--registry=https://registry.npmmirror.com/` 的 `npm ci`。

**临时容器做端到端**（独立端口 + 独立卷 + 自带口令）：

```bash
docker run -d --name hdsl-e2e -p 127.0.0.1:18080:3080 \
  -v hdsl-e2e-data:/data -v hdsl-web_hdsl-data:/prod \
  -e PNPM_STORE_DIR=/prod/pnpm-store \
  -e NPM_CONFIG_REGISTRY=https://registry.npmmirror.com/ \
  -e HDSL_ADMIN_PASSWORD='<临时口令>' hdsl-web:e2e
```

- **`-v …:/prod` + `PNPM_STORE_DIR` 是省时间的关键**：临时容器直接复用生产的 pnpm store
  （2.3 G 内容寻址仓库，多进程并发读安全），否则 ZCode 构建要重下几个 GB；顺带还能验证
  「重写 config.yaml 时 `storeDir` 没丢」。
- 临时容器默认**没有 TLS**（数据目录里没有 `server.yaml`）→ 健康检查用 **http**，别用 `https`
  （我会用 `https` 探，白等 60 秒，报 `wrong version number`）。
- 构建临时镜像必须带与 compose 相同的 build args，否则 apt 层缓存失效、去连不可达的 `nodejs.org`：
  `docker build --build-arg NPM_REGISTRY=… --build-arg NODE_DIST_MIRROR=https://mirrors.cloud.tencent.com/nodejs-release -t hdsl-web:e2e .`

**截图**（`ghcr.io/puppeteer/puppeteer`）：`--network host` + `NODE_PATH=/home/pptruser/node_modules`（CJS），
输出目录先 `chmod 777`；手机视口 `{width:390,height:844,isMobile:true,hasTouch:true}`（`hasTouch` 才让
`pointer: coarse` 生效）；每张断言 `documentElement.scrollWidth === innerWidth`；会话 cookie 名
`hdsl_session`，值取 `POST /api/auth/login` 的 `Set-Cookie`。两个坑：`page.$$eval` 里的回调**看不到**外层闭包
变量（要当第三个参数传进去）；`waitForSelector` 等的是「弹窗里的元素」时，得先把弹窗点开。

**在容器里手动跑一遍面板的 ZCode 构建**：用 JDK 容器编译一个小驱动（`javac -cp /app/hdsl-web.jar`）后
`docker cp` 进容器，`java -cp /app/hdsl-web.jar:/tmp/<dir> -Dhdsl.home=/data/hdsl <驱动> <ref>` 跑
`ZcodeBuilder.start(...)`——与面板按钮走同一段代码，但由 shell 触发、参数可控。

## 4. 未决与待办

- **提交与发布**：本次修复 + 回归用例 + 文档已本地提交（`fix(proxy)` 那一条）。版本号按 `v0.2.3` 写进了
  `CHANGELOG.md`（`v0.2.2` 已发布，所以没往那一条里塞）。发布两条路：本机
  `RELEASE_VERSION=v0.2.3 NPM_REGISTRY=https://registry.npmmirror.com/ docker compose up -d --build`，
  或 push 之后打 `v*` 标签，`.github/workflows/docker.yml` 会构建并推到 ghcr（触发条件就是 `v*` 或手动 dispatch）。
- 可选加强：把 node-gyp 的头文件缓存（`devdir`）挪进数据目录，省掉每次重建容器后的那次下载
  （设了镜像之后只有 1.7 秒，所以没做）。
- 可选回退：`b2244bf` 那个「平台过滤」是否要留一个默认关的开关（见 §2.4；现已删除，见 §1）。
- 前端小尾巴：手填框「失焦即报红」（现在是输入过或失焦过才报红）；测速按钮的结果行在 360px 下略挤。
- 已知但无害：挂载下 `<link rel="manifest">` 会被去掉——浏览器取 manifest 不带凭证，留着必然是 401，
  而在别人的路径下也装不成 PWA。

## 5. 给下一个功能的提醒

- 领域层 `org.jackhuang.hmcl.dsh` / `setting` 是桌面版逐字拷贝，改动尽量落在 `org.jackhuang.hmcl.web.*` 与 `web/`。
- **新增任何跑 pnpm/npm 的子进程调用**：registry 与那套环境变量都要给——npm 系读 env，pnpm 系只读
  config.yaml，最省事的做法是让子进程继承 `DshNetworkSettings.environment()`（ZCode 构建器就是这么补上的）。
- 新增设置项记得 **5 处**（§2.2），并想清「什么时候生效」——设置变了要不要立刻写进子系统的配置文件。
- 前端两条判定的媒体查询（`mobile.css` 顶部与 `hooks.ts` 的 `MOBILE_QUERY`）必须逐字一致；
  窄屏别写死 `minWidth`（`.comp-value` 自带换行）。
- 提交风格：Conventional Commits 前缀 + 中文标题 + 详细 body（写清「改了什么、为什么、怎么验证的」）。
