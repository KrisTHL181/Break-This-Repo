# ZCode（实验性品类）

> **纯实现性验证功能：不保证可用，也不承诺与 dsh 同等的功能与兼容。** 能不能跑通取决于
> ZCode 上游；本页只说明我们怎么接的、怎么放发行包、以及已知限制。

面板的第二个可启动品类，对应 [zai-org/ZCode](https://github.com/zai-org/ZCode)（Z.ai 的
AI 编程工作台，Apache-2.0）。它与 dsh 品类**完全隔离**：实例页里独立品牌分区、独立数据
目录、独立进程；浏览器侧经面板 `/i/<id>/` 反代打开（同端口、同证书、同会话门），实例
本身只绑回环、不开令牌。

## 为什么是「实验性」

- ZCode 官方**没有 CLI 发行渠道**——官网与 GitHub Releases 只有桌面安装包，命令行发行包
  得自己构建（`pnpm build:zcode`，要求 Node 24.14+）。
- ZCode 的 Web 前端把根路径写死（`/assets`、`/api`、`/ws`），所以面板构建时会打上反代补丁
  （见下），让它按 `/i/<id>/` 前缀工作；补丁之外的行为取决于上游，随时可能变。
- 凭证注入写的是 ZCode 的内部配置文件格式（`provider_config.json`），未文档化，上游一改
  就失效——「能注就注，不能就算了」。
- 上游大约周更。

## 怎么得到发行包

### 方式一：面板内构建（默认）

实例页 ZCode 分区里的「构建发行包」按钮：填一个上游 tag（`v3.14.3`）或分支（`main`），
面板会

1. 从 GitHub 下载源码 tarball（`codeload.github.com/zai-org/ZCode`）；
2. 解包后打上[反代补丁](#反代补丁)；
3. 跑 `pnpm install` 与 `pnpm build:zcode`；
4. 校验产物记录的 `sha256`，装到 `<数据目录>/zcode/releases/<版本>/`，并把 `current` 指过去。

界面上能看到当前步骤、进度和日志（日志文件在 `<数据目录>/zcode/build/build.log`）。

要求与代价：容器镜像里已经装好 Node 24（`/opt/node24`，只有构建用它），以及 `python3` / `make` / `g++`
——Linux 容器里 `node-pty`、`cpu-features` 会走现场编译，缺了编译链会直接报
`Could not find any Python installation` / `Unable to detect compiler type`。
**Windows 裸部署不需要编译链**：`node-pty` 随包带 `win32-x64` 预编译，面板会自供
Node 24+、pnpm，并在 `<数据目录>/zcode/build/bin` 写一个 `tar.bat` 指向系统 bsdtar
（PATH 上的 GNU tar 会把 `C:\` 当成远程主机）。构建前还会用 `vswhere` 预检 `cl.exe`：
没有时日志会说明 `cpu-features`（ssh2 的可选依赖）会被跳过、不影响构建；面板**不会**
自动下载编译器（Build Tools 需管理员且约 2.5–3 GB）。实测细节见
[`report-2026-10-18-zcode-windows-build.md`](report-2026-10-18-zcode-windows-build.md)。
要下几百 MB 源码、装 GB 级依赖，**慢是正常的**；构建失败（环境缺失、上游改了锚点）只影响这一次构建，
已装好的版本不受影响——而且**失败时源码树会保留**（`<数据目录>/zcode/build/src` 与同目录的 `build.log`），
排查完不用重下，下一次构建开始时才清掉。

| 环境变量 | 说明 |
| --- | --- |
| `HDSL_ZCODE_SOURCE_URL` | 源码 tarball 模板（`%s` = tag/分支）；也接受 `file:` 或本地路径（离线/内网代理） |
| `HDSL_ZCODE_BUILD_BIN` | 构建时前置到 `PATH` 的目录；镜像里默认 `/opt/node24/bin`。裸部署留空时，面板优先用已装的托管运行时，缺 Node 24+ 会自动装进 `<数据目录>/hdsl/runtimes` |
| `HDSL_ZCODE_PNPM` | 构建用的 pnpm 可执行文件；留空或写 `pnpm` 时用构建运行时的 pnpm，运行时没有则用它的 npm 自动装进去 |
| `HDSL_ZCODE_KEEP_SOURCES` | `true` 时构建**成功**后也保留源码树与依赖（排错用，占几个 GB）；失败时无论如何都保留 |
**慢？** 时间几乎全花在 `pnpm install` 上。三个杠杆：

- **换 registry**：首选面板的「设置 → 下载源」（预设或自填 URL，见
  [deployment.md 的运行期下载源](deployment.md#6-运行期下载源面板设置)），保存即生效不用重启容器。
  构建会把它显式传给 `pnpm install --registry=…`，日志里能看到实际用的是哪个。环境变量 `NPM_CONFIG_REGISTRY`
  只是**回退**：面板没有设置时才用它（镜像默认官方 `registry.npmjs.org`，国内常用 `https://registry.npmmirror.com/`）。
  选 `npmmirror` 预设时构建还会带上 `ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/`——Electron 的
  postinstall 要从 GitHub 下约 100 MB 的二进制，国内直连基本下不动；别的源不带这个镜像，走 GitHub 默认。
- **架构过滤已取消**：曾经有一版会往源码目录追加 `.npmrc` 的 `supportedArchitectures`，只下本机架构的
  可选依赖。它有两个问题：pnpm 是从 `pnpm-workspace.yaml` 读这个设置、**根本不看 `.npmrc`**（所以它从未生效）；
  而一旦生效就会把构建搞坏——组包阶段要为 6 个平台收集原生资源（darwin/linux/win × x64/arm64），缺一个直接失败。
  上游的 `pnpm-workspace.yaml` 已经写着它需要的平台，交给它即可。
- **源码下载**：默认从 `codeload.github.com` 拉 tarball；慢的话用 `HDSL_ZCODE_SOURCE_URL` 指向镜像、
  内网缓存，或本地路径（`/path/to/ZCode.tar.gz`，也接受 `file:`）。
- **native 编译那两段（换源管不到）**：依赖里有要现场编译的原生模块（`node-pty` 等）。node-gyp 编译前会先从
  `nodejs.org` 下 10 MB 头文件——国内实测约 104 KB/s，且缓存在 `~/.cache/node-gyp`（**不在卷里**，重建容器后
  第一次构建要重下）；它默认也不给 `make` 加 `-j`。用镜像源时构建会设 `NODEJS_ORG_MIRROR` /
  `npm_config_disturl` 指向 npmmirror 的 Node 镜像，并按 CPU 数设 `JOBS` / `npm_config_jobs` 并行编译。

安装之后、打包之前，构建器会额外补一步 `pnpm exec tsc -b packages/shared`：上游干净检出下
`pnpm build:zcode` 必然停在 `Missing @zcode/shared dist files`——`packages/shared` 没有 build 脚本，
报错里让你跑的 `pnpm build` 也不会编译它，只有项目引用构建（`tsc -b`）会产出 `dist/index.js`。
上游补好之后这一步可以直接删。

### 方式二：手工放一个发行包

`HDSL_ZCODE_PACKAGE` 指向一个解包好的发行包目录，或直接放进
`<数据目录>/zcode/releases/<版本>/`（需含 `bin/zcode.mjs`）。

面板找发行包的顺序：`HDSL_ZCODE_PACKAGE` → `current` 指向的版本 → `releases/` 里最新的一个。

| 环境变量 | 说明 |
| --- | --- |
| `HDSL_ZCODE_PACKAGE` | 发行包目录（覆盖上面的自动查找） |
| `HDSL_ZCODE_NODE` | 运行 ZCode 的 node：可以写命令名（按 `PATH` 查找，默认 `node`），也可以写绝对路径 |

### 反代补丁

面板构建时把 5 处根路径改成「带 `/i/<id>/` 前缀工作」：vite 的 `base: "./"`、
`main.tsx` 里的 `hdslBasePath()`（从浏览器路径读前缀）、WS origin、`/api/server-info`
fetch、Z.ai OAuth 的 `tokenUrl`。上游把这些锚点挪走时构建会**明确失败**并点名文件，
而不是产出一个指向错误地址的发行包；届时更新 `ZcodePatch` 与本文档。

## 实例怎么跑

- 每实例一个目录 `<数据目录>/zcode/instances/<id>/`：`instance.json`（面板记录）、
  `data/`（作为 `ZCODE_DATA_BASE_DIR`——ZCode 的凭据与供应商配置在其下）、`home/`
  （作为 `HOME`——会话库 `home/.zcode/cli/db/db.sqlite` 在其下，见下节）、
  `logs/zcode.log`。旧的 `<实例>/workspace/` 不再是默认工作区，只在读历史清单时作为回退。
- 默认工作区在 `<数据目录>/zcode/workspaces/<id>-<名字>/`，创建实例时立刻建好，路径记进清
  单并在界面上显示；名字里的非 ASCII 字符会被省略掉，只剩 `<id>`。**删实例不会删它**——那是
  项目目录，不是实例状态。要指向现有项目就在创建时填它的绝对路径，此时行为与以前一样。
- 启动命令：`node <发行包>/bin/zcode.mjs --web --no-open --host 127.0.0.1 --port 0
  --no-token --workspace <实例的工作区>`（默认工作区即上面 `workspaces/` 下那个目录；面板从
  stdout 的就绪行 `Local: http://127.0.0.1:<port>/` 解析真实端口）。**只绑回环、不开令牌**：
  对外只有面板这一条路，令牌认证在这里没有位置，面板的会话门就是唯一的门。
- 「打开」= `/i/<id>/`——和 dsh 同一个挂载点，同端口、同证书、同样要登录；请求由面板剥掉前缀
  转发到实例的回环端口，WebSocket 也走同一条中继。
- 保存 API Key 时写入 `<实例>/data/.zcode/v2/provider_config.json`（ZCode 的个人供应商
  配置；条目 `providerId = hdsl-injected`，协议 `openai-chat-completions`）。

### 3.18.x 的 credential-secret

从 ZCode 3.18.0（当前 `main` 构建为 3.18.2）起，独立 server 在开放服务前要求一个
32–4096 字节的根密钥；没有 Secret Service 的 headless 环境必须显式提供。面板会在
`<instance>/data/.zcode/v2/credential-secret` 自动生成一次（0600，base64url，32 字节），
已存在不覆盖；也可以通过进程环境里的 `ZCODE_CREDENTIAL_SECRET_FILE` 或
`ZCODE_CREDENTIAL_SECRET` 指定外部密钥（二者不能同时设置）。

如果实例已有 `enc:v2:` 密文但密钥缺失，面板不会生成替代密钥，而是拒绝启动并提示恢复
原密钥或重置凭据。密钥丢失 = 加密凭据不可恢复，需要重新登录/重录 API Key；会话和工作区
不受影响。

`Local:` 就绪行之后面板还会做一次 HTTP 探活；3.18.2 会在打印端口后才初始化 host
services，失败时进程会退出，探活会把实例标为 error 并带上日志尾部，而不是先报 running。

### 版本目录与实例下限

`GET /api/zcode/catalog` 返回三类版本：

- `release`：GitHub Releases（正式版；当前 v3.17.2 / v3.17.1 / v3.14.3）；
- `tag`：有 tag 但没有 Release 的版本（如 v3.15.1）；
- `main`：`main@<sha>`，未发布，不保证兼容。

每一项带 `credentialFormat`（1 legacy / 2 system credential storage）、
`requiresCredentialSecret`、`installed` 与 `buildState`。3.18.0 起是 v2；当前只有
`main` 构建会触发。

实例新增 `version` / `minVersion` / `minVersionAt` / `credentialFormat`：

- 创建时可选一个已构建的 ref，或留空跟随 `current`；
- 成功启动后，`minVersion` 前移到该版本，`credentialFormat` 记录已使用的格式；
- 切换版本要求实例已停止、目标版本已构建，并通过两道闸：凭据格式不能倒退，
  semver 不能低于 `minVersion`；从 `main` 回退到更早的正式版也会被拒绝。失败返回
  `409`，前端把低于下限的项置灰并给出原因。
- 这是**按实例**的锁：新实例仍可选旧版本。

### 更换密钥

创建实例时可选「自动生成」（默认）或「使用种子」；种子由面板用
PBKDF2WithHmacSHA256（per-instance salt、210k 次）派生 32 字节写入 `credential-secret`。

实例菜单里的「更换密钥」是可重复的 reset：

- 要求实例已停止；
- 删除 `credentials.json`、`credential-source`、旧 `credential-secret`；
- 可选「同时清除已保存的 API Key」；
- 写入新密钥（自动或种子派生），之后需要重新添加 API Key / 登录。

旧密文不会重新加密；这是产品上明确接受的代价。会话、工作区和任务索引不受影响。

### 终端（TUI）

- 发行包不带参数启动就是官方 TUI（`bin/zcode.mjs` 会 import `agent/zcode.cjs`，CLI 的
  默认命令是 `tui`）。面板起 `node <发行包>/bin/zcode.mjs`，**不传任何参数**：`--web` 一
  套是 web 模式专属，`--workspace` 也只有 web 分支解析，工作区由进程 cwd 决定。
- 环境设两条：`ZCODE_DATA_BASE_DIR=<实例>/data`（凭据与供应商配置）和
  `HOME=<实例>/home`（会话库）。会话库跟着 `homedir()` 走（上游
  `adapters/src/storage/session-store/paths.ts`），没有任何环境变量能把它挪到别处，所以只能
  改 `HOME`；web 模式和终端**用同一个** `home/`，两边看到的是同一套会话。
- 为什么非得有个自己的 home：不设 `HOME` 的时候它继承面板的 `/home/hdsl`，那是容器的可写层，
  `docker compose up -d` 换镜像就整个换掉；而 `/data` 里还留着列出这些会话的任务索引，于是
  列表里有、点开报 `Session not found`（2026-11-01 就是这么丢的）。顺带每个实例有独立的
  `~/.zcode`，两个实例不再共用同一份 `config.json`。
- 新建实例时把面板 `HOME` 下几个凭据条目**软链**进实例 home（`.gitconfig`、
  `.git-credentials`、`.netrc`、`.npmrc`、`.ssh`），否则 agent 的 `git`/`ssh` 会突然变哑；
  面板 home 里没有的就不链，Windows 上建不了链接也不报错。链接只在 home 首次创建时做一次。
- 缺 `bin/zcode.mjs` 或 `agent/zcode.cjs` 时打开终端直接返回可读原因——只有 web 的构建
  产物没有 agent，放进 PTY 才失败会让人以为是面板的问题。
- `POST|DELETE /api/zcode/instances/{id}/terminal`，协议、ticket 与门禁与其他品类完全相同
  （见 `docs/terminal-protocol.md`）。

### web 模式的三个报错，根因是「项目库的 owner 对不上」（2026-10-31 查源码后修正）

早先这里写的是"web 模式需要一个桌面宿主"。查过上游源码后要更正：`/ws/host` 那条连接是
**桌面端主动连服务、去调服务的 channel**，服务从来不会反向问 host 要文件系统。所以补一个
host 解决不了任何问题。真正的机制是一次字符串不相等：

- 独立 server 把项目库的 owner 定成 `remote:server:<serverId>`（源码
  `packages/services/src/node.ts`，`createServerHostServices`）；
- 客户端所有导航默认 `hostId = request.hostId ?? "local"`
  （`packages/services/src/project-workspace-navigation/index.ts`）；
- `project/index.ts` 的 `route()` 拿两者比较：不等且没有 `resolveHost`（独立 server 构造上
  不传）→ 抛 `project.host-offline`。

三个界面症状都由这一个不相等引出：

| 界面上的话 | 服务端（`logs/zcode.log`） | 真实原因 |
|---|---|---|
| 「无法获取系统信息」/「无子目录」 | `project.readDirectory FAIL … project.host-offline` | 浏览器传 `"local"`，server 的 owner 是 `remote:server:x`，被当成"别人的 host" |
| 「创建项目/会话卡住」 | `… project.remote-project-required` | owner 不是 `local` 时，没有 projectId 的会话一律拒绝；而项目又建不出来 |
| 会话索引空 | `… ZCode Agent runtime is not running` | 上面两条都断了，没有任何路径把该 workspace 的 agent runtime 拉起来 |

**面板的做法：把 owner 钉成 `local`，而且是在构建发行包时钉**（`web/zcode/ZcodeServerPatch.java`）。
发行包里的 `server/entry-http.js` 是未压缩的可读 ESM，面板在**安装发行包之前**改这一句：

```js
const projectStorageHostId = remoteHostIdFromWorkspaceIdentity(…)   →   const projectStorageHostId = "local";
```

于是 server 的项目库就是本机的，浏览器也不再是"远端客户端"——这与面板的模型一致（一台机器、
一个进程、浏览器是唯一客户端）。实测：补丁后服务器建库时 `hosts` 表里是 `('local','local')`，
不再是 `remote:server:desktop-hloonil`。

**为什么不放在启动时改**：同一份字节不应该因为"谁启动它"而行为不同。补丁是**本发行版的一部分**
（和构建期改那 5 处前端锚点同性质），装好的 `releases/<版本>/` 因此是确定的字节；启动只做
**校验**，不写文件。上游不接受 PR，所以这条没有"等他们加开关"的退场时间，按 distro patch 维护。

**每个平台都打，除非操作者明确不要**（`ZcodeServerPatch.applies()`）。这条曾经只在 Windows 上打，
理由是「Linux 部署还可能有第二客户端：ZCode 桌面程序连过来当 host，`remote:server:<id>` 正是那套
模型需要的」。代价是：不用桌面端的人（绝大多数）在 Linux 上一样什么都打不开。浏览器才是面板真正
服务的客户端，所以现在默认全平台钉成 `local`；确实拿桌面程序当 host 用的人，设
`HDSL_ZCODE_KEEP_REMOTE=1` 就退回上游的 owner（发行包不被改写，`NOT_APPLICABLE`）。

这条补丁的代价与边界：

- **它是改上游代码**。锚点是上面那句表达式。**构建时**锚点不在 → 构建明确失败并点名锚点，
  不会产出"看起来装好了、其实工作台没有工作目录"的发行包；手工放进 `releases/` 的发行包
  （或 `HDSL_ZCODE_PACKAGE` 指向的）没有被补过，启动时只在日志里说一次「这个发行包不是本面板
  构建的，web 工作台不会有工作目录」，页面照常打开。
- 补丁前建的旧项目记录 `host_id = remote:server:x` 会因为 owner 变了而"隐身"（查询按 owner
  过滤）。旧实例的数据目录删掉即可，那本来是一次性数据。
- `--workspace` 仍然只是"初始 tab 的路径 + `/api/server-info` 里的条目"，**不会自动变成项目**
  （上游 `runner.mjs` 与 `server/src/http.ts` 都是这个行为，README 写的"指定项目"与实现有落差）。
  现在 owner 是本机的了，界面里「新建项目」选那个目录即可；面板要不要替你预置一条项目记录，
  是下一步的事。

## 已知限制（都是有意为之）

- **只有面板这一条路**：实例只绑回环、不开令牌，浏览器经 `/i/<id>/` 进来（面板的会话门就是
  唯一的门）。别去映射 ZCode 自己的端口——它也不监听外面。
- **API Key 明文**：同时写在实例清单与 `<实例>/data/.zcode/v2/provider_config.json`。
- **没有面板侧集成**：会话列表、插件管理、控制台（ZCode 没有 ACP）都只属于 dsh 品类；
  ZCode 这边只有启停、网页反代与终端。
- **终端要有 agent**：只有 web 的构建产物（缺 `agent/zcode.cjs`）不能开 TUI；第一版也不做
  `--resume` 接历史会话。
- **凭证注入尽力而为**：格式取自 ZCode v3.14.3 源码，换成别的版本可能不生效——那就当它
  没有这个功能。
- **深层相对路径可能取不到资源**：补丁用 vite 的 `base: "./"`，资源按文档 URL 解析——
  `/i/<id>/` 下正常，更深的路径（例如上游的分享页）可能失效。

## 相关代码

- 后端：`src/main/java/org/jackhuang/hmcl/web/zcode/`（`ZcodeApiServlet`、`ZcodeRuntime`、
  `ZcodeInstanceManager`），挂在 `HdslServer` 的 `/api/zcode/*`（与其它 `/api/*` 一样过
  会话门）。
- 终端：`web/terminal/ZcodeTui.java`（argv 与两条状态根）、`web/terminal/InstanceTerminal.java`
  （与 dsh、品牌共用的门禁/启动/ticket）。
- 前端：`web/src/components/ZcodeSection.tsx`（实例页里的独立品牌分区，终端复用
  `TerminalWindow`）。
- 客户端：`client/hdsl-tui/internal/panel/client.go` 的 `instancesPath` / `terminalPath`
  按品类解析前缀。
- 测试：`src/test/java/org/jackhuang/hmcl/web/zcode/`，用 `src/test/resources/fake-zcode/`
  这份 stub 发行包跑完整生命周期；终端门禁见 `ZcodeTerminalApiTest`。
