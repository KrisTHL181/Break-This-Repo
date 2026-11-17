# 第三方品牌实例（Kimi Code / OpenCode / Claude Web）

> 与 ZCode 实验品类并列的另外三类「自带网页端的第三方工具」：面板负责**安装指定版本、
> 启动、停止、日志、以及把它们自己的网页端经 `/i/<id>/` 反代出来**（同端口、同证书、同登录门）。
> 安装来源有两种：npm registry（Kimi Code / OpenCode）与 git 仓库 tag（Claude Web，首个 git 源品牌）。
> 界面在**实例列表页内**（左侧子侧栏「第三方品牌 → 品牌实例」，内容区原地换成品牌层，
> 不跳第二层页面）：实例是主对象，版本是品牌级共享构建。面板不提供插件管理（这些工具没有
> 可用的插件生态），但**每实例的插件通道已在界面与后端预留**（见「实例模型」）；也不注入任何
> 凭据——登录、模型供应商、计费都由工具自己的网页界面完成。
>
> 品牌一共五个（另有 ZCode 与 Octop）。**Octop 不在这三类里**：它走 GitHub Release 的 portable
> 预编译包（或开发者模式里的 Linux 源码编译），并需要给它的 dashboard 打挂载点补丁，安装与策略
> 见 [octop.md](octop.md)；ZCode 见 [zcode-experimental.md](zcode-experimental.md)。

## 面板到底做了什么 / 没做什么

**做了**：安装指定版本进品牌目录（`<数据目录>/brands/<品牌>/releases/<版本>/`）。npm 品牌从
registry 拉包（pnpm store 跨实例去重）；git 源品牌（Claude Web）从 **GitHub 发行 tag 下载源码 zip**
（本网络下 `github.com:443` 的 git 传输会被拦腰切断，源码 zip 1.5 秒下完 11 MB——详见 v0.3.8 的
变更记录）后只装 server/web 两个 workspace **加根项目**（`npm ci -w server -w web
--include-workspace-root`——漏掉根项目就少了网页端运行内核 ccb，所有 OpenAI 类型供应商都会静默
回退到官方 Claude Code）再 `npm run build -w web`。从回环端口拉起各自的网页端（`kimi web` /
`opencode web` / claude-web 的 Node 服务）；把网页端改写后挂在 `/i/<id>/`（根绝对资源加挂载点前缀
+ 注入运行期 shim，与 dsh 同一套三层改写；按路径路由的客户端再补一层路由修正，见下）；
启动/停止/日志/删除。

**没做（刻意）**：
- **插件管理**：各家都没有可用生态，不做。
- **凭据注入**：账号、API key、模型供应商都在工具自己的网页里配。每个实例的状态目录
  （Kimi Code 的 `~/.kimi-code`、OpenCode 的 `~/.config/opencode`、Claude Web 的 `~/.claude-web`）
  落在实例自己的 `home/` 里、随 `/data` 卷持久化——凭据明文在容器文件系统里，与 ZCode 品类同等待遇。
- **行为担保**：第三方工具；面板仅负责安装、启动与网页反代，其功能、安全与行为归各自官方/社区。
  上游周更，旗标随时可能漂移。
  **OpenCode 与 Claude Web 在界面上标为实验性接入、不保证可用性**：面板能做的（挂载、改写、反代）
  已经到极限。OpenCode 剩下遇到的问题基本都是它自己的——模型目录与模型 id 对不上（实测：客户端挑到
  `deepseek/deepseek-flash`，而它自己的服务器只有 `deepseek-flash`，于是 agent 起不来、会话里
  只有用户消息没有任何回复）、会话与 agent 行为、凭据与计费。这类问题面板修不了，只能换模型、
  换版本或等上游修；诊断过程见 [report-2026-10-08-opencode-web.md](report-2026-10-08-opencode-web.md)。
  Claude Web 是**社区项目**（不是 Anthropic 官方网页版）：内置社区构建的引擎，登录可用官方账号也可
  在「设置 → 供应商」接第三方中转——引擎、供应商、凭据、计费都归上游/服务商。

## 三个品牌的差异（实测记录，2026-10）

| | Kimi Code | OpenCode | Claude Web |
|---|---|---|---|
| 来源 | npm 包 `@moonshot-ai/kimi-code` | npm 包 `opencode-ai` | **GitHub 仓库** [ypyik0669/claude-web](https://github.com/ypyik0669/claude-web)（MIT，社区项目；不发 npm 包） |
| 版本列表 | npm registry（82 个干净 semver） | npm registry（944 个；1.2 万个 snapshot/ci tag 被滤） | **GitHub REST** `/repos/<slug>/tags`（剥 `v` 前缀；`engine-*`/`nightly` 等非发行 tag 被滤） |
| 安装 | pnpm 拉包 | pnpm 拉包 | **`codeload.github.com/<slug>/zip/refs/tags/<tag>` 下载源码 zip**（带重试）→ 解包（剥掉 `<repo>-<tag>/` 根目录）→ `npm ci -w server -w web`（**不装 desktop**，Electron 二进制不下载）→ `npm run build -w web` |
| 网页端命令 | `kimi web` | `opencode web` | `tsx server/src/index.ts`（Node 直跑 TS 源码，无内置入口） |
| 端口 | `--port 0` 可用（内核给临时端口，就绪行播报） | **`--port 0` 不生效**（静默回落默认 4096）→ 面板预分配 | 无端口参数 → **`PORT` 环境变量**预分配（源码核实 `PORT ?? 3090`） |
| 就绪行 | `Local: http://127.0.0.1:<port>/` | `Web interface: http://127.0.0.1:<port>/`（**带 ANSI 颜色码**，匹配前先剥除） | `listening on http://127.0.0.1:<port>` |
| 反代鉴权 | `--dangerous-bypass-auth`（官方文档写明用于 "behind your own authenticating proxy"，面板会话门就是这个代理）+ `--allowed-host <面板域名>`（防 DNS rebinding，对应 dsh 的 `--trusted-host`） | 不设 `OPENCODE_SERVER_PASSWORD`（启动日志会打印 unsecured 告警——面板门就是门） | 无 token 配置时**信任回环**（源码 `authOk`）；WS 升级校验 Origin==Host——与面板反代「Host 透传」契约一致，实测链路通 |
| 浏览器入口 | `/i/<id>/`（客户端认子路径，与 dsh 同款改写） | `/i/<id>/` + **路由修正脚本**（见下） | `/i/<id>/` 标准 shim：前端 `base: './'` 资源全相对、单 WebSocket、无 history 路由——**零补丁** |
| 离线安装 | ❌ postinstall 要从厂商 CDN 拉原生二进制 | ✅ 平台二进制走 registry optionalDependencies | ❌ engine tgz 从 GitHub Releases 拉；npm 依赖可走镜像 registry |

### 为什么 Claude Web 是「零补丁」

上游的 web 构建用的 `base: './'`（资源全部相对路径——上游注释写明是为「手机壳跑在子文件夹」场景），
前端与服务器只通**一条 WebSocket**（`client.ts` 里 `new WebSocket(...location.host + '/ws')`，无散落的
`fetch('/api…')`），且没有任何 history 路由（刷新即首页）。所以 `/i/<id>/` 挂载时：静态资源浏览器按
相对路径自己拼对；WS 地址是绝对 `/ws`，由面板注入品牌页的标准运行期 shim 改写（与 Kimi/OpenCode 同款
机制）；WS 握手的 Origin 校验（`originOk` 要求 Origin==Host 或回环）正好与面板反代一贯以来的 Host 透传
契约吻合（这个契约本身就是 dsh 的 Origin 防护定的）。结论：**不需要上游改动，用户预备的上游 PR 用不上**。

### 为什么下载源码而不用 `git clone`

实测（2026-10-12，本机网络）：`github.com:443` 的 **git 传输会被拦腰切断**——`expected flush after ref
listing`、GnuTLS `-110`、乃至 130 秒连接超时，成功率大约三成；而同一时刻 `api.github.com` 0.6 秒
200，`codeload.github.com` 的源码 zip 1.5 秒下完 11 MB。于是这一品牌**完全不碰 git**：版本列表走
REST API（未认证 60 次/小时；版本目录有磁盘缓存、ETag 与单飞，见「版本目录：缓存、配额与刷新」），
安装走 zip 下载 + 解包，下载失败自动重试 3 次。
附带两个好处：镜像不需要 `git` 二进制；下载是普通 HTTPS，失败信息是明确的 HTTP 状态码/超时，
比 git 协议一堆含糊的中断好诊断。

版本目录的失败语义见「版本目录：缓存、配额与刷新」：缓存优先、失败保留旧列表，
一次网络抽风不会把下拉框变成空。

### 「打开文件夹…」在容器里为什么不能用

Claude Web 的项目选择器里那个 `打开文件夹…` 叫的是**服务器端的原生目录对话框**（它自己的
`terminal/service.ts` `pickDir`：Linux 用 `zenity`、macOS 用 `osascript`、Windows 用 PowerShell）。
容器里没有桌面、也没装 zenity，`execFile` 失败后它**静默返回 null**，前端 `if (p) setCwd(p)` ——
于是点下去什么都不会发生（不是面板或反代的毛病）。

面板的绕法：**实例就绪后，用它自己的协议 `workspaces.add` 把该实例的工作区目录登记成项目**
（`ws://127.0.0.1:<端口>/ws`，回环免鉴权；失败只损失一次点击）。之后打开页面，
「项目」菜单里就能直接选中 `workspace` 目录，完全不需要那个对话框。若还想用别的目录，
可以先用面板的文件能力把它放进实例工作区，或用它的终端 `cd` 过去。

### Claude Web：官方内核与 `ANTHROPIC_BASE_URL`

Claude Web 有两个内核（社区引擎 `claude-web-engine`、官方 `@anthropic-ai/claude-agent-sdk-*`），
**两个都最终连 Anthropic**：官方内核是官方 Claude Code，社区引擎是它的社区构建。而 Anthropic 的
[支持地区政策](https://www.anthropic.com/legal/supported-regions)把未列出的国家/地区（含中国大陆）
列为不受支持，并明确覆盖「人在不支持地区使用」与「主体注册在不支持地区」两种情形；从这类网络出口
访问 `api.anthropic.com` 一律 **403**，Claude Code 自己会打印
`App unavailable in region` 然后退出。

面板为此做两件事：

- **实例级的端点覆盖**：实例菜单 →「Anthropic 端点…」可以填 `ANTHROPIC_BASE_URL`（留空即恢复默认）。
  值随实例存进 `instance.json`，**网页模式与终端都用它**（同一个 workbench、同一份 CLI 配置），
  面板不校验端点是谁，也不代管凭据——走官方网关还是第三方中转、以及服务条款上的风险，都由使用者判断。
  若网关需要密钥，把 `ANTHROPIC_API_KEY` / `ANTHROPIC_AUTH_TOKEN` 放在面板进程的环境里：子进程继承
  面板的环境，无需在面板里再存一份。
- **官方内核的预检**：选官方内核时，面板先对该端点 `GET /v1/models`——不受支持地区回 **403**，
  受支持地区没带 key 回 **401**。只有 403 会被拦下并给出人话（社区引擎或配置端点二选一），
  而不是把一个起不来的 TUI 丢给用户；401、超时、DNS/TLS 失败都放行，由 CLI 自己解释。

要绕过地区限制，正路是用**受支持地区**的出口或官方支持的云厂商（Bedrock / Vertex）通道；
第三方中转不是 Anthropic 的服务，提示词与密钥都会交给对方，而且在不受支持地区使用本身就落在上面
那条政策里，账号风险自负。


## Hermes Agent（第四个品牌，实验）

[Hermes Agent](https://github.com/NousResearch/hermes-agent)（Nous Research，MIT）是这批里第一个
**不是"一个网页进程"**的品牌：它跑两个进程——`hermes gateway run`（OpenAI 兼容 API + agent 运行时）
和 `hermes dashboard`（浏览器看的 SPA），而 dashboard 要等 gateway 的 `/health` 才肯挂上去。

| | 事实 |
|---|---|
| 来源 | GitHub tag 的源码 zip（`codeload`），不用 PyPI：上游 `setup.py` 在 Nix 之外拒绝构建 wheel/sdist，editable 安装（`uv sync`）才是它认可的路 |
| Python | 版本自己的 `pm/lock.json` 钉的 **3.14.9**（带 SQLite 3.53.1），不是 `uv python install`（会拿到 3.14.0b2，SQLite 3.50 的 WAL-reset 告警会让 state.db 降级） |
| uv | 同一个 lock 钉的 **0.13.5**；面板里那个 0.7.13 解析不了 v0.22.5 的 `uv.lock`（revision 5） |
| 依赖 | `uv sync --frozen --no-dev --extra all`（**不用** `--all-extras`：会拉 `[rl]` 的 torch/wandb 与 `[yc-bench]`） |
| 前端 | `node-deps.mjs --workspace ui-tui --workspace web` → `web.mjs --icons` → `tui.mjs`；面板自带的 Node 22.23.1 就够（源码 `engines.node` 写的是 `^22.22.0 || ^24.11.0 || >=26`） |
| 安装标记 | `hermes_cli/web_dist/index.html`（web 构建最后一步原子发布） |
| 体积 | 单版本 **570 MB**（venv 285 / 源码 274），`toolchain/python` 251 MB 共享，`toolchain/uv` 17 MB |
| 端口 | 两个，落 `<instance>/panel/ports.json`（首次分配，之后复用；被占则重分配一次） |
| 就绪 | 轮询 dashboard 的 `/api/status`，要求 `gateway_running=true`；**不**要求 `overall=ok`（它把 storage 与各消息平台也算进去） |
| 停止窗口 | **40 s**（其余品牌 10 s）：gateway 自己要 drain 30 s |
| 平台 | **Linux / macOS**。Windows 上不提供安装：`Brand.installableOn` 是唯一判断，`/api/brands/hermes/versions` 直接回 `supported:false` + 原因（连 GitHub 都不去请求）；`GET /api/brands/capabilities` 让品牌页签在点进去之前就标“不可用”；`POST /api/brands/hermes/instances` 与 `BrandRuntime.launch` 也会用同一句原因拒绝，避免从 Linux 拷来的 `/data` 在 Windows 上死在 `error 193`。想在 Windows 上用 Hermes，请把**面板本身**装进 WSL2——对面板来说那就是 Linux |
| 为什么 Windows 不行 | 两点都不是小改：安装要从源码建一套 Python 环境（venv 是 `.venv/Scripts/` 而不是 `bin/`），运行时靠一个 `sh` 脚本同时看住 gateway 与 dashboard 两个进程（Windows 没有 `/bin/sh`）。官方 `install.ps1` 解决的是"一台机器一份安装"，与面板"多版本共存 + 每实例一份 HOME"不是同一个问题 |

### 一个 wrapper 管两个进程

面板对每个实例仍然只持有一个进程句柄。启动时生成 `<instance>/panel/wrapper.sh`（每次重写）：

1. `hermes gateway run --replace &`，等 `/health`（90 s，用 venv 里的 python 探，不依赖 curl）；
2. `hermes dashboard --skip-build --no-open --host 127.0.0.1 --port <dash>`；
3. 两个子进程各写一个 pid 到 `<home>/panel/`，`trap` 收尾（TERM → 15 s → KILL），任意一个死掉整组跟着退。

`--replace` 不是随手加的：它和下面「终端与网页互斥」是同一件事的两半。

### 反代：它是前缀感知的，所以不能再改写它的页面

dashboard 读 `X-Forwarded-Prefix`，把 `/assets/`、`/fonts/`、`__HERMES_BASE_PATH__` 全部按挂载点生成。
所以面板做的是**相反**的事：把 `Host` 去掉（它只接受回环，别的值直接 400），补一个
`X-Forwarded-Prefix: <base>/i/<id>`，然后**不再**改写页面——`brandClient=false` 关不掉页面改写，
`pump` 为此多了一个 `prefixClient`。再改写一次会变成 `/i/<id>/i/<id>/assets/…`，并注入一段与 SPA
自己的路由打架的 shim。

WebSocket 同理：上游的 WS 路由不过 HTTP 中间件，自己再查一遍 Host 与 **Origin**，所以转发时
`Host` 与 `Origin` 都丢掉，只补 prefix。

### API 端点（T3）

gateway 是个 OpenAI 兼容服务，所以实例卡片下面有一个「API 端点」折叠条：
`GET /api/brands/hermes/instances/<id>/endpoint` 返回 `baseUrl` / `apiPort` / `key` / `running`，
按钮「添加到账户」直接 `POST /api/accounts`（`protocol: "openai-completions"`）。
key 在 `<home>/.env` 里（0600，agent 属主），面板不轮换它——换了会让已保存的账户静默失效。

### 终端与网页互斥（为什么不是「两个都能开」）

Hermes 的终端（`hermes --tui`）不是一个独立的小程序：它自己会**确保一台主机上有一个 gateway**，
没有就起一个（`gateway_bootstrap.py --start`），而且那个 gateway 是 **detached** 的——终端关掉它
还活着（`--idle-exit`，600 s 无客户端才退出）。面板的 Web 模式也坚持「gateway 是我的子进程」。
两套监督者抢同一个 `HERMES_HOME`，于是单独实现任何一半都会留下另一半的坑：

```
# 打开终端：面板按设计先停 Web（InstanceTerminal.open 的 stopWeb）——
# 这一步本身是对的，Web 的 gateway 与 dashboard 一起收掉。
gateway.run: Gateway stopped by an unexpected signal …
gateway.run: Exiting with code 1 (signal-initiated shutdown without restart request)

# 随后 TUI 起自己的 detached gateway；关掉终端它还在。
gateway.start: main.py gateway run --quiet --idle-exit

# 这时再开网页：wrapper 的 gateway 看到“已有主机 gateway”，attach 后立刻退出
✓ The host gateway already serves profile 'default' — nothing to start.
hermes: gateway exited during startup        → state: error
```

**所以两者只能开一个，且两半都由面板来切**：

- 打开终端 → 面板先 `BrandRuntime.stop(id)`（`InstanceTerminal.open` 的 `stopWeb`），Web 模式整个
  下线（dashboard 与 gateway 都走 wrapper 的收尾）；
- 打开/启动网页 → 面板先关掉该实例的终端会话（`instanceTerminal.close`），wrapper 用
  `gateway run --replace` 把主机 gateway 拿回来——包括 TUI 留下的那个 detached 副本。

代价写在明处：**切到网页，终端会话就结束了**。没丢的是会话本身——它们都在
`<home>/state.db` 里，下一次进终端或网页都还在；丢的只是那个 TUI 进程。反过来也一样：进终端时
网页会下线，浏览器的 `/i/<id>/` 会变成 502/连接失败，重新启动网页即可。

为什么不做成「两个都开」：要么让 TUI 只 attach、绝不启动/替换 gateway（上游目前没有这个开关，
`gateway ensure` 在必要时会替换），要么让面板不再持有 gateway 子进程、改成 attach 模型（那就
放弃「实例状态由面板监督」这条线，进程挂了没人知道）。两条都比互斥贵，而且会把失败模式从
「点了就知道」变成「偶尔聊到一半断线」。

### 两段身份下的属主（v0.6.44 起）

**一条通用规则**：实例的 `home/` 与 `workspace/` 由面板（root）创建，创建完必须交给 agent
（`Privileges.prepareAgentDirectory(dir, 模式)` 一句话做两件事：建目录 + 交属主；品牌侧的列表是
`BrandRuntime.handInstanceDirsToAgent`，`create` 与每次 `launch` 各调一次）。这不是 Hermes
的特殊要求，而是这一层的基本假设：面板是 root，agent 是 uid 1000，**root 造的东西 uid 1000 不该
当成自己的**。

Hermes 是第一个真的检查这一点的品牌——它的 gateway 发现会拒绝 root 拥有的 control path：

```
gateway inaccessible: unsafe_control_path
  …/instances/<id>/home is owned by uid 0, expected uid 1000 (gateway user); chown it to the gateway user
```

症状很隐蔽：dashboard 正常、页面能开，但聊天面板一直 `retrying in 30s (attempt N)`，消息堆在
`queued` 里发不出去。别的品牌（kimi / opencode / claude-web）不检查属主，看起来没事——只是因为
`/data` 是 setgid 2775、root 建的子目录继承了 `root:hdsl 2775` 还组可写。**那是挂载的巧合，不是
任何人验证过的性质**，和 v0.6.45 的 pnpm `config.yaml` 是同一个形状。

容器里自己验：

```
docker exec -u 1000 <容器> sh /tmp/verify-privilege-split.sh   # 第 5 项检查品牌实例目录属主
```

### Hermes 自己的属主表

| 路径 | 属主 / 模式 | 为什么 |
|---|---|---|
| `<home>/.env` | agent, **0600** | gateway 要读 API key；面板是 root，写完交给 uid 1000 |
| `<home>/` | agent, 0775 | hermes 要在这里建 `config.yaml`、`state.db`（SQLite 还会建 `-wal`/`-shm`） |
| `<home>/panel/` | agent, 0700 | wrapper 写的 pid 与锁 |
| `<instance>/panel/` | **root, 0755** | 面板的：wrapper.sh 与 ports.json。agent 能穿过、不能改——能改就等于能改反代指向 |
| `toolchain/**` | root, 0755 | `.venv/bin/python` 是指向这里的绝对链接；任何一级 0700 都会让 `hermes` 起不来 |

这条和 v0.6.45 修 pnpm 的 `config.yaml` 是同一个坑：**面板替 agent 写的文件，必须连属主带模式一起
交过去**（`Privileges.handToAgent`，五套公共模式在 `Privileges` 里）。

## 为什么 OpenCode 需要一段「路由修正脚本」

OpenCode 的网页客户端**按 URL 路径路由**（bundle 里直接读 `window.location.pathname` 匹配路由表，
且整个发行包没有任何 base-path 开关）。挂在 `/i/<id>/` 下时它匹配不到任何路由，界面只剩左上角的
壳（实测：不经反代、直连实例自己的端口加任意子路径，现象完全相同——是客户端本身，不是代理）。
但它仍然走**和 dsh 一样的单端口挂载**，只是这类页面会多拿到两样**必须成对出现**的东西：

- **面板改写它的 bundle**（`assets/index-*.js`，约 2.7 MB）：把路由读取地址的那一处表达式
  （Solid Router 的 `window.location.pathname.replace(/^\/+/,"/")`，实测 1.18.32 与 1.18.35 一致）
  换成先经过挂载点剥离的版本。于是**客户端读到的路径永远是它期望的原生根路径**
  （`/i/<id>/server/k/session/x` → `/server/k/session/x`），路由照常匹配；
- **`/i/<id>/__hdsl-router.js`**（同源外链，CSP 友好）：**把客户端写回地址时的挂载点加回去**——
  `history.pushState/replaceState` 的目标自动带上 `/i/<id>/`（已经在挂载点里的地址不会重复加）。
  于是地址栏停在 `/i/<id>/…`、刷新与深链都回到应用内的同一处（客户端自己的服务器对未知路径返回
  它的 index 页）、地址也可以直接分享；
- **它自己拼的资产地址也算在内**：bundle 里 `"/assets/…"` 字面量（worker、图标）、Vite preload
  助手的基址（`const <name> = function (dep) { return "/" + dep }`，懒加载 chunk 全走它）与 CSS 里的
  `url(/…)`（字体）同样被改写到挂载点下。少了这一步就是**首页能看、点进任何面板都是空白**——
  实测：Settings / Providers 的 chunk 全部 404、控制台 `Failed to load module script … text/html`。
- 请求本身仍全部走挂载点：`/i/<id>/__hdsl-shim.js` 把应用运行时拼的同源绝对 URL（`/assets`、
  `/api/…`、WebSocket、SSE）加上挂载点前缀。

**兜底**：面板处理页面请求时会先探一次客户端的 bundle（按挂载点 + 资源名缓存，只在第一次加载时多
一次回环读取）。如果那个表达式不在（上游换了打包方式或路由库），页面改引用
`/i/<id>/__hdsl-router.js?mode=root`：这一份 shim 把地址**还给面板根**（初次加载替换成 `/`，后续
write 也停在 `/`），应用照常可用，代价是地址栏回到 `/`、刷新落回面板首页——也就是老行为，而不是
只剩一个壳。探针失败（超时、非 200、bundle 过大）同样按兜底走。
只有一个入口端口：`3080`（或你部署时选的那个）——不需要给路由器开任何额外端口。

**实例给不给 `content-length` 不是前提**：已知长度只用来**提前放弃**（说出来就超过 16 MB 的，不读、
按原样转发）；**没给长度（`Transfer-Encoding: chunked`）照样补**。OpenCode 1.19.5 的资源响应就是
这种形状——实测 `content-type: text/javascript` + `Transfer-Encoding: chunked`、**没有
`content-length`**——旧规则把它整份原样转发，markdown worker 于是去面板根取文件、应用报
`Markdown highlighting worker failed`；探针同样不再要求长度，否则这类客户端的整页路由会直接退回
「地址还给面板根」。真正的上限由读取时的 16 MB 兜着：真的超了就**整份原样发出**——不打补丁，也
**不截断**（半截 JS 一行都跑不起来）。

Kimi Code 的客户端认子路径，不需要这段脚本（实验证明：注入反而会与它自己的路由打架，故按品牌开关）。

## 实例模型：页面、版本与插件通道

### 品牌图标（来源与商标）

页签行上的标志取自各家**上游仓库自己发布的文件**，原样未改，只用于标识所管理的工具；
完整声明在仓库根 [NOTICE](../NOTICE) 的 TRADEMARKS 一节：

| 品牌 | 文件 | 来源与许可 |
| --- | --- | --- |
| dsh | `assets/img/brand-dsh.svg` | DeepSeek 的鲸鱼标，Simple Icons 分发的 CC0 文件（标志取自 deepseek.com）。dsh 实例的默认图标从此是鲸鱼，不再是 HMCL 的草方块（单色 → CSS mask + `currentColor`，浅深主题自适应） |
| Kimi Code | `assets/img/brand-kimi.svg` | `MoonshotAI/kimi-code` 的 `apps/vscode/resources/kimi-icon.svg`（MIT）。单色线稿，走 CSS mask + `currentColor`，浅深主题都自适应 |
| OpenCode | `assets/img/brand-opencode.svg` | `anomalyco/opencode` 的 `packages/identity/mark.svg`（MIT，仓库原址 `sst/opencode`）。自带底色与描边，直接 `<img>` |
| ZCode | `assets/img/brand-zcode.svg` | Simple Icons 分发的 Z.ai 标志（文件 CC0，标志本身来自 z.ai 官方 logo）。**故意不用 ZCode 仓库里那份**：它的 `NOTICE` 对"图标"有笼统保留条款，许可明确的那份优先 |
| Claude Web | **不上图** | 社区仓库的图标就是 Anthropic 的珊瑚色星芒标，而 Anthropic 商标条款写明"须事先书面许可、不得改动"。故只保留文字标签 + 中性图标 |
| PI WEB | **不上图** | 上游 pi-web 与本地化分支都没有为标志附明确的可再分发许可；按本仓库惯例回退中性图标，只做文字标识 |

商标与著作权是两件事：上表的文件许可（MIT / CC0）解决的是**分发**，
标志本身仍是各权利人的**商标**——声明里写明了"不表示隶属/赞助/认可"，且未改色改形。

### 实例模型

`/instances?view=brands`（实例列表页内**原地切换**出来的一层：左侧选「品牌实例」，右侧内容区
换成品牌层，URL 只换查询串、页面不跳转、不重挂载；品牌之间用内容区顶部的页签行切换）
一页管完一个品牌的全部实例：

- **实例是主对象**：实例卡吃掉剩余高度，行内有 radio（= 启动面板的「当前实例」，与 dsh 实例列表
  同一套选择）、状态徽标、`id · 版本 · 端口/打开地址`，以及启动/停止、打开、版本、信息、重命名、
  插件、日志、删除。该品牌还没有实例时，新建行的名称框**预填品牌名**（Kimi Code / OpenCode /
  Claude Web / ZCode）——多数人一个品牌就一个实例，点一下「创建」即可。
- **版本是品牌级共享构建**：`brands/<品牌>/releases/<版本>/` 装一次，同品牌所有实例都能切过去；
  实例清单里只记它 pin 的版本号。面板不假装每个实例各装一份。
- **切换版本按需安装**：行内版本下拉列的是**全量版本目录**（已装标「已装」、未装标「未装」）。
  选已装版本 → 直接 `PATCH`；实例在跑 → 先确认「停止并切换」；选未装版本 → 确认后走品牌级安装
  （`POST /api/brands/<brand>/install`，行内显示进度与失败原因），装完再 `PATCH`。后端那条
  「未安装的版本 → 400」的校验原样保留，是安全网；编排放在前端，少一个会腐坏的新接口。
- **插件通道（预留）**：`GET /api/brands/<brand>/instances/<id>/plugins` 与
  `GET /api/zcode/instances/<id>/plugins` 返回同一形状 `{supported, plugins[]}`。现在没有任何品牌
  支持插件，能力位（`Brand.supportsPlugins()`）一律 false、列表为空，界面显示「该品牌暂不支持插件」
  并说明通道已预留。前端只按这个响应渲染：将来某个品牌有了真插件，后端把 `supported` 置真、把
  `plugins` 填上，界面不用改一行。

## 版本目录：缓存、配额与刷新

品牌页的版本选择器不是每次打开都问远端。它按下面的顺序回答，任何一层有数据就
立即渲染：

| 层 | 来源 | 失败时 |
| --- | --- | --- |
| L1 | 内存 | — |
| L2 | `cache/brand-catalog.json` | 用缓存渲染，标「磁盘缓存 · 时间」 |
| L3 | 远端 | 保留 L2，并标注原因；**从不清空已有列表** |

GitHub 品牌（Claude Web / Octop / Hermes）**有人访问时按 1 小时 TTL** 刷新，带
`If-None-Match`：304 不消耗 GitHub 配额，只把 `checkedAt` 往前推。Kimi /
OpenCode / PI WEB 走 npm registry，TTL 30 分钟。同一品牌同时只有一个请求在飞
（单飞），页面加载、后台检查和手动刷新共享它。

**无人访问时**（连续 3 小时没有品牌接口请求）后台每 **3 小时**确认一次 GitHub；
只要上游一整天没发版本，这几次都是 304，缓存始终有效。后台循环在**服务就绪
10 分钟后**才启动第一次检查，启动路径本身只读磁盘缓存、不碰网络——面板的启动
横幅和健康检查永远不等 GitHub。有人一访问，后台立刻让路，回到 1 小时 TTL。

手动刷新按品牌冷却 **10 分钟**，冷却期内按钮禁用并显示倒计时；它绕过 TTL，
但不会绕过限流和退避。

**配额与退避**：每次 GitHub 响应都读 `X-RateLimit-Limit/Remaining/Reset`。
剩余 ≤ 10 时停止自动刷新（手动仍可试），额度用尽时页面显示重置时间；403/429
不重试。连续失败 3 次后自动刷新按 1h → 2h → 4h 退避，成功一次即恢复。

**GitHub 令牌**：`<data>/auth/github.json`，0600、面板属主。它只给面板的
`/tags`、`/commits`、`/rate_limit` 调用使用；品牌进程（agent）不需要也读不到。
设置 → 品牌实例里可以填写/删除，保存前用 `/rate_limit` 校验，界面只显示
「是否配置 + 登录名」。令牌把匿名 60 次/小时提高到 5000 次/小时。
**面板不读 `GITHUB_TOKEN` 环境变量**：`ProcessBuilder` 会把面板环境复制给每个
品牌子进程，那等于把令牌交给 agent；需要令牌请用这里的文件。

`GET /api/brands/<brand>/versions` 的 `catalog` 字段携带这些状态
（`source` / `fetchedAt` / `checkedAt` / `ttlMillis` / `limit` / `remaining` /
`resetAt` / `refreshCooldownUntil` / `rateLimitedUntil` / `backoffUntil` /
`failures` / `tokenConfigured` / `tokenLogin`）；`?refresh=1` 是手动刷新。

## 设置 → 品牌实例（下载源与最后手段）

品牌安装慢，几乎都不是"网速"问题，是**默认从境外源下载**。这一页把这些源显式化：

### 最后手段只属于 Octop 和 OpenCode

单实例 + 固定反代（`/i/octop/`、`/i/opencode/`）不是通用功能：只有这两个工具的 agent
卡死时，面板还需要一扇固定的门能进去（`Brand#supportsLastResort`）。其余品牌的策略卡只剩
默认 Node.js；API 也会拒绝它们的 `singleInstance`/`lockedRoute`，老 `policy.json` 里残留的
这两个字段在读取时被丢弃，不会让实例一直灰着又没有开关可关。

固定路径不是另一套反代：它只是把挂载名从 `/i/<token>/` 换成 `/i/<brand>/`，OpenCode 的
路由补丁、Cookie 路径、`Location` 改写和 WebSocket 中继都按同一个挂载串处理。保留实例同时
还能从 token 路径打开，两扇门指向同一个进程；关掉开关后固定路径立刻失效（404/502 走正常的
"实例不在"分支），token 路径不受影响。

- **读不需要开发者密码**：品牌、已装版本、当前生效的源、每个实例的状态都能看——页面不会因为
  没提权就假装没有实例。改设置、点「测速」要开发者密码（与打开终端同一个，30 分钟滑动）；
  明文 HTTP 下开发者模式本身不可用，写操作全部标灰并写明原因。
- **四类源**：`PyPI 索引`（uv 本体与 `uv sync` 的依赖）、`CPython 运行时`
  （`UV_PYTHON_INSTALL_MIRROR`，系统已有 3.12 时不会下载）、`源码归档`
  （codeload 形态 `<base>/<repo>/zip/refs/tags/<tag>`，Claude Web 与 Octop 源码编译都走它；
  git 传输在这里经常被切断，而这份 zip 通常 1–2 秒下完），以及**每个走 GitHub Releases 的品牌**
  各一行（现在只有 Octop）。npm 不在这里——它是全局的，仍在「设置 → 下载源」。
- **优先级**：面板设置 > 环境变量（`UV_DEFAULT_INDEX`/`PIP_INDEX_URL`、`UV_PYTHON_INSTALL_MIRROR`、
  `CODELOAD_BASE`（或 `HDSL_CODELOAD_BASE`）、各品牌对应的 `…_RELEASE_BASE`）> 上游默认。所以老部署里
  用环境变量配好的镜像不会因为升级失效。
- **测速是"测"不是"改"**：点「测速」会从面板这台机器把该组每个候选都请求一次，列出毫秒并给最快的
  标「推荐」；只有点「保存」才会写入设置。一次提交里有任何一个地址不合法，**整体不保存**。
- **地址校验**：只收 `http(s)`、去尾斜杠、限长 200，拒绝空白/控制符与 `"'\`\\#$;|&@`——这些值会进
  子进程的环境变量，一个换行就能注入第二条设置。

写代码时的一个坑（也写进了 `docs/build-from-scratch-prompt.md`）：`uv` 的 **JSON API 镜像大多没有**，
所以给 uv 找轮子必须读镜像自己的 simple 页（`<index>/uv/`）再解析 `href`，不能照抄
`https://pypi.org/pypi/uv/<version>/json`。实测数字（本部署）：`files.pythonhosted.org` 约 40 KB/s，
阿里云 PyPI 约 6.6 MB/s，南大 `github-release` 镜像上的 CPython 包约 6.45 MB/s，而 `github.com`
的 Release 资产域直接超时——Octop 的 portable 包（约 260 MB）因此没有可用的国内官方镜像，只能自建。

### 安装进度与停止

源码编译按阶段报进度，每段有权重、并给一句 `[3/7] 构建 dashboard · 55%` 的文案：

| # | 阶段 | 权重 | 常耗时 |
|---|---|---|---|
| 1 | 下载源码（codeload） | 5% | 数秒 |
| 2 | 应用 dashboard 补丁 | 2% | 数秒 |
| 3 | `npm ci` 前端依赖 | 35% | 1–5 分钟 |
| 4 | 构建 dashboard | 15% | 1–2 分钟 |
| 5 | 准备 uv | 8% | 首次较慢 |
| 6 | `uv sync`（CPython 3.12 + 依赖） | 30% | 5–55 分钟（冷缓存+慢盘） |
| 7 | 收尾（写 marker） | 5% | 数秒 |

面板显示进度条、已用时间与**预计剩余**（只在知道总量时给，见下）。进度条下面还有一条**阶段内**
细进度，能算的都不靠猜：

- **下载**：按 `Content-Length` 报字节、速度和剩余时间
  （`下载 portable 包：112.4 MB / 260.0 MB · 6.1 MB/s · 剩约 24 秒`），每 0.5 秒一次。
- **`npm ci`**：分母是 `package-lock.json` 里 `packages` 的条目数，分子是现数 `node_modules` 里的
  包目录（`npm ci：已安装 762/1245 个包 · 剩约 1 分 20 秒`）——`npm ci` 本身几乎不打印东西。
- **`uv sync`**：解析 uv 自己的输出，分成四段（阶段标签会跟着变）：
  `下载依赖`（`uv：下载 12 个包 · 68.4 MB · 6.1 MB/s`）→ `解包写入磁盘`
  （**`uv：下载完成（37 个包 · 268.4 MB）· 解包写入 197/345 个包`**）→ `编译本地依赖`
  （`编译 2 个完成，进行中 1（evdev）`）→ `安装完成`。
  为什么会需要磁盘信号：**uv 的进度条是 TTY-only**，管道里它在解包/安装阶段一行都不打；
  这台慢盘上一次安装可以静默几十分钟，看起来和死锁一模一样。所以每 1.5 秒数一次
  `site-packages/*.dist-info`（没有 venv 时退到 uv 缓存的 `archive-v0` 条目），谁都不说话也能看出
  在推进。没有走"PTY 跑 uv"那条路：进度条在慢盘上会疯狂重绘，反而加 I/O；也没有加 `-v`，
  那会把日志淹掉。
- **任何阶段沉默超过 15 秒都会写明** `· 已 N 秒无新输出`，配合速度和已完成数量判断死活。
- **"下载结束"有独立信号**：接口的 `stageKey` 在下载结束时从 `download` 变成 `write` / `build` /
  `install`，页面据此**弹一次提示**并在安装卡片里**常驻横幅**：「✅ 下载阶段已结束 · 正在编译 ·
  慢盘或慢网上可能很久，去喝杯咖啡再回来看也行」。不用去日志里找那行「下载完成」。
- **仿 portage 的逐包日志**：解包/安装阶段每 1.5 秒数一次 `.dist-info`，新出现的包写成
  `>>> 已安装 (199 of 345) cryptography-46.0` 追加进 `install.log`；日志面板打开时每 3 秒刷新尾巴。
  Gentoo 的"话多"来自 portage 那层壳，不是源码——这里补的就是那层壳。
- 其余步骤（构建、收尾）按预计耗时插值，封顶 95%。

阶段计时器只在工具**安静 2 秒**后才用估算值覆盖那行文案；日志面板会边跑边追加最新状态行，不再
是打开时的一张快照。

「停止」走 `POST /api/tasks/<id>/cancel`：子进程由任务服务整棵杀掉，下载/解包这类不是进程的
循环则自己看 `isCancelled()`；已写入的半成品留在安装目录，由**启动时对账**清理（见 `octop.md`
的「半安装与自动修复」）。

## 新增一个品牌要动的地方（这份清单是被漏出来的）

第一次接 Hermes 时，后端全通、品牌页也能看到，但**实例建出来不显示**——因为漏了其中一条，
而那条恰好是唯一把"各品牌实例"合成一个列表的地方。按下面顺序过一遍，每条都有个具体现象验收：

| 改哪 | 漏了会怎样 |
|---|---|
| `Brand.java`：枚举项 + 能力位（`installMarker` / `isReleaseVersion` / `stopWaitSeconds` …） | 版本列表里没有这个品牌，或装完认不出 |
| `BrandInstaller.install` 分支 + `BrandRuntime.plan` | 能选版本、装不上 / 起不来 |
| 反代特判（只有"客户端自己认挂载点或路由"的品牌才需要） | `/i/<id>/` 白屏或双前缀 |
| **`web/src/store.tsx` 的 `refreshExternal()`** | **实例建出来不显示、按钮一直转，看起来像卡死**（Hermes 第一次就是这样：品牌页自己另拉列表，所以截图是好的） |
| `web/src/types.ts` 的 `BrandId`、`BrandsView` 的 `BRAND_KEYS/BRAND_TITLE/BRAND_SHORT`、`i18n.ts` | 品牌页没有这一栏，或标题是 undefined |
| `web/src/actions.ts` 与 `LaunchPane` 的品牌联合类型、`MainPage` 的断言 | 主界面启动/停止按钮传不过去（tsc 会拦，但只在类型收窄的地方） |

## PI WEB（第五个品牌，实验）

### 一个 release 是一个配对

PI WEB 的网页守进程复用宿主的 Pi SDK，而同一个 release 里的 Pi TUI 就是那份 SDK 的
`pi`。因此面板安装的是**一对**版本，不是单独一个包：

```
releases/<pi-web 版本>/
├── node_modules/@chainingintention/pi-web-cn      ← 网页 + sessiond
├── node_modules/@earendil-works/pi-coding-agent   ← Pi TUI + SDK
└── release.json                                   ← 安装审计结果
```

配对表在 `Brand.PI_WEB_PAIRS`（当前 `2.202611.8` ↔ `1.4.0`）。它是**契约**：pi-web 的
peer 范围覆盖多个 Pi 大版本，而按 SDK `VERSION` 分支的适配（例如管理侧 MCP 认证）只对
其中一些存在。所以版本选择器只列已验证配对；更早的 release 已装仍可继续使用（这是
"单档可选、底层可回退"的 B 方案）。

`release.json` 记录 `{piWeb, piSdk, webAvailable, ptyAvailable, installedAt}`。
`webAvailable=false` 表示这个 release 只装了 Pi SDK（仅终端），`ptyAvailable=false`
表示网页能跑但浏览器内终端不可用（`node-pty` 在 Linux 上没有预编译，需要 C++ 工具链）。

### 两个进程，一个 Node wrapper

`pi-web-sessiond`（会话/工具）与 `pi-web-server`（网页/API）是两个进程。web **故意不**
启动 daemon（`SessionDaemonClient` 只连 socket/URL），所以面板生成一个
`<instance>/pi-web-wrapper.mjs`：先起 daemon，等它的 `/health`，再起 web；任一退出就收掉
另一个。就绪判定同时轮询 daemon 的 `/health` 与 web 的 `/api/pi-web/health`——上游明确
说 web 的就绪不等 daemon，只等 web 会把"会话永远加载不出来"标成 running。

端口在面板侧预分配，走回环 TCP 而不是 unix socket，避免品牌实例目录过深触发 socket
路径长度上限。**数据目录不写进环境变量**：pi-web 的默认值是 `$HOME/.pi-web`，而 HOME
就是实例 home（已交给 agent），所以目录由将要用它的那个 uid 自己创建——面板预先建一个
root 属主的目录，正是容器里 `EACCES` 那一类 uid bug。wrapper 也写在实例 home 下
（`~/.pi-web-wrapper.mjs`，0644），因为 home 已交给 agent，`node` 读得到；实例目录本身
不保证 agent 可穿越。

```
PI_WEB_SESSIOND_HOST=127.0.0.1
PI_WEB_SESSIOND_PORT=<daemon 端口>
PI_WEB_SESSIOND_URL=http://127.0.0.1:<daemon 端口>
PI_WEB_HOST=127.0.0.1
PI_WEB_PORT=<web 端口>
PI_WEB_PI_PACKAGE_DIR=<release>/node_modules/@earendil-works/pi-coding-agent
PI_WEB_SKIP_VERSION_CHECK=1
```

`PI_WEB_SKIP_VERSION_CHECK` 是刻意的：版本由面板管，`pi-web update` 会替换实例 TUI
正在执行的目录。

### 反代：相对客户端，零改写

pi-web 的客户端用 `base: "./"` 构建，所有请求经 `appUrl.ts` 解析一次到
`document.baseURI`，路由走查询串。因此它属于"相对客户端"类别（`Brand#relativeClient`）：
`/i/<id>/` 只做既有的剥前缀、cookie `Path` 改写、`Location` 改写与 WebSocket 转发，
**不注入 runtime shim、不改写 HTML/脚本**。这一点和 Hermes 的"前缀感知"不同：Hermes 的
服务端自己读 `X-Forwarded-Prefix`，而 pi-web 的服务端始终只认根路径。

### 形态、版本锁与降级

- **终端 + 网页**（默认）：网页由 wrapper 起；TUI 可从实例卡另开，两者共用同一个 HOME
  与 `~/.pi/agent`，读写同一批会话。与 Hermes 不同，**两者可以同时开**。
- **仅终端**：不启动 web、不分配端口、不挂载 `/i/<id>/`；实例卡隐藏"打开网页"。
  创建时可选定，之后在「运行与版本…」对话框里改回来（改回时若当前 release 是仅终端
  安装，会拒绝并说明先装完整配对）。
- **版本锁**：实例固定在自己的 release，品牌换推荐配对时不跟随；版本选择器禁用，
  `PATCH` 换版本返回 `version_locked`。
- **降级**：安装失败时版本卡原位给出决策——「只装 Pi 终端」（只装 SDK，
  `webAvailable=false`）、「换源重试」或取消；实例卡徽标与「运行与版本…」对话框展示
  `webAvailable` / `ptyAvailable` 与原因。降级实例可以被锁定，避免下一次升级又切到
  一个同样没有网页的 release。

### 已知边界

- **Pi TUI 的语言**：这个分支汉化的是 PI WEB 自己（网页、`pi-web` 命令与服务端提示），
  Pi 本体的 TUI 不在这个包里。
- **node-pty**：`1.1.0` 只带 darwin/win32 预编译；Linux 上走 `node-gyp`（需要
  python3/make/g++）。失败只影响网页内置终端，不影响 Pi TUI 与网页聊天。
- **会话恢复**：`PiTui` 暂不接 resume 参数（Pi CLI 的恢复 flag 未经真机验证），终端
  每次开新会话。
- **浏览器终端（重要）**：`pi-web.terminal` 是上游**必需**插件，普通启动不能禁用
  （`pi-web plugins safe-start set none` 是恢复模式，会同时停掉其它服务端插件）。它给的
  是 daemon 同 uid 的交互式 shell，cwd 为实例 workspace；**面板的五道终端门（E-Stop /
  总开关 / HTTPS / 开发者口令 / 提权）不覆盖它**，它只有 pi-web 自己的访问密码 + 面板
  会话。因此：
  - 不要把 pi-web 的端口直接暴露，实例只能经 `/i/<id>/` 访问；首次打开必须设访问密码；
  - agent uid 能看到所有实例的数据，不应有 sudo / docker 等价权限；
  - 它与 agent 的执行能力是同一个边界：关掉网页终端也不会阻止 agent 运行命令；
  - 需要与面板终端同等门禁时，只能不部署 PI WEB，或把它隔离到独立 uid / 容器。

## 已知限制

- **Claude Web 的引擎与账号**：它内置的引擎是社区构建（claude-code-best 基底），可登官方 Claude
  账号，也可在「设置 → 供应商」里接第三方中转（DeepSeek/Kimi 等 OpenAI 兼容中转都行）。无论哪条路，
  引擎行为、供应商可用性、凭据与计费都归上游/服务商；中转路线另有 Anthropic 服务条款的考量。
- **同一 origin 的 localStorage 共用**：和 dsh 实例一样，多个品牌实例挂在一个 origin 下时浏览器
  storage 按 origin 隔离、不看路径（Kimi Code 的 web UI 实证使用 `kimi-web.*` 键）。同品牌开多个
  实例会互踩界面状态。需要彻底隔离时的方案见部署文档 §8 末尾的讨论。
- **CSP**：OpenCode 的页面带 `script-src 'self'`，**内联脚本会被拒**——所以运行期 shim 不是内联的，
  而是面板在每个挂载点下自己提供的一个同源脚本（`/i/<id>/__hdsl-shim.js`），页面改写只引用它。
  这个坑是 e2e 用无头浏览器实测挖出来的（shim 在 HTML 里、fetch 却是 native、运行期请求全 404）。
- **面板重启后品牌实例显示为已停止**：进程表在内存里（与 ZCode 品类相同语义），重新点启动即可。
- **bundle 补丁锚定上游实现**：OpenCode 路由读取地址的那一处表达式就是补丁锚点（实测 1.18.32 与
  1.18.35 一致）。上游若换掉打包方式或路由库，探针会判为「不可补丁」并自动退回「地址还给面板根」
  的模式（见上），页面不会变成壳——代价只是地址栏回到 `/`、刷新回面板。资产地址那两条改写
  （`"/assets/…"` 字面量与 preload 基址）各自独立：找不到就只跳过它。
- **`/i/<id>/` 未启动时 404**：品牌实例 id 只在面板进程内登记过才解析；磁盘上有、从未启动过的
  实例走品牌区 API 启动。

## TUI 品牌实例（Qwen Code / MiMo Code / MiniMax Code）

这三家是**纯 TUI 品牌**：`Brand.kind() == TUI`，实例只有一个 PTY，没有网页进程、没有端口、
没有 `/i/<id>/` 挂载。实例列表左侧子侧栏新增**独立的一栏「TUI 品牌实例」**（`?view=tui`），
只装 Qwen / MiMo / MiniMax；原来的「品牌实例」仍是七个网页品牌（含 ZCode），两者互不混排。
实例卡只提供「进入终端 / 停终端 / 版本 / 安装日志 / 重命名 / 删除」，不再显示启动、停止、打开网页。

| | Qwen Code | MiMo Code | MiniMax Code |
|---|---|---|---|
| npm 包 | `@qwen-code/qwen-code` | `@mimo-ai/cli` | `@minimax-ai/code` |
| 入口 | `cli-entry.js` | `mimo.js`（或 `vendor/mimo`） | `cli.js` |
| resume | `--resume <id>` | `--session <id>` | `--session <id>` |
| 数据目录 | `QWEN_HOME` | `MIMOCODE_HOME` | `MINIMAX_DATA_DIR` |
| Node 要求 | `>=22` | `>=20.10` | `>=22.19 <23 \|\| >=24.2 <27` |
| 安装注意 | 纯 Node；保留 optional 平台包 | npm 包是 launcher，postinstall/首次运行从 FDS 下载二进制 | 体积大，`better-sqlite3` 需要 build script，POSIX 也走 hoisted |

运行时统一走面板解析出的 Node（TUI 品牌优先托管运行时），并把它的 `bin` 前置到子进程
`PATH`；实例 home 仍由面板创建并交给 agent，`home/` 与 `workspace/` 的隔离与网页品牌相同。
安装仍是 `pnpm add`（带 `--config.dangerously-allow-all-builds=true`），不走 npm。

启动接口对 TUI 品牌回 `409 tui_only`；Go 客户端按 `/api/brands/capabilities` 的 `kind`
隐藏启动/停止，只留终端。网页端同样按这个能力位渲染，不靠品牌 id 硬编码。

## 相关代码

- 后端：`src/main/java/org/jackhuang/hmcl/web/brand/`（`Brand` 描述符 + 插件能力位、`BrandCatalog`
  版本目录、`BrandInstaller` 安装器、`BrandInstanceManager`、`BrandRuntime` 进程运行时、
  `BrandApiServlet` 挂在 `/api/brands/*`）；反代目标解析在 `InstanceProxyTargets`（dsh → 品牌 →
  zcode 顺序）。
- 反代泛化：`InstanceProxyServlet` 的页面改写从「dsh 精确形状」扩展为「任意根绝对 SPA」，回归
  夹具是 `src/test/resources/brand-pages/` 下的真实首页 HTML。
- 前端：`web/src/components/BrandsView.tsx`（品牌层：页签行 + 版本卡 + 实例卡，嵌在实例列表页里）、
  `web/src/components/ZcodeSection.tsx`（zcode 分支的两卡内容）、`LaunchPane.tsx`
  （跨品牌实例切换器）。入口在实例列表页子侧栏（`InstancesPage.tsx`：两层原地切换）。
