# 会话交接：聊天合并与 v0.6.85 发布准备（2026-11-14）

> 本稿接住 **面板聊天合入 `main`、守门修复与 v0.6.85 发布准备**。前稿
> [`2026-11-12`](session-handoff-2026-11-12.md) 的聊天实现结论仍然有效；本稿只写合并之后
> 的状态、验证与发布清单。
>
> **分支状态**：v0.6.85 已发布（`be566a667`）。v0.6.85 之后的 FreeBSD 批次（终端、平台
> 声明、dsh 结论、转码手机端布局）已以 `--no-ff` 合入 `main`（`14cc70579`），并随
> **v0.6.86** 发布；`web-shell` 仍停在 `c11c13c76`，下次开发时重置到新的 `main`。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令、真实 API Key 一律
> 不写进仓库。

## 0. 当前状态

- `main` 包含 v0.6.84 之后的**面板聊天 v1**、**Qwen / MiMo / MiniMax 三个 TUI 品牌**、
  终端拒绝 shell 回落、主侧栏重构，以及合并前的守门修复（见 §2）。
- 文档已按实现回写：`docs/chat.md`、`docs/architecture/{context,full,backend}.md`、
  `docs/architecture/flows/chat.md`、`README.md`、`docs/deployment.md`。
- 本地发布产物：`build/libs/hdsl-web-0.6.85.jar`（69 MB）与 `SHA256SUMS.txt`，
  见 §3。
- 线上/远端仍是 v0.6.84；这次没有推送任何东西。

## 1. 这一版包含什么

- **聊天**：SQLite 会话/节点、OpenAI + Anthropic 协议、流式 turn、停止、两种重生成、
  编辑、variant、附件（图片/PDF/Office/长文本）、助手与模型服务、备份与保留、联网搜索
  （博查 / DeepSeek / 智谱 / Tavily / Brave / Exa）与只读工具 `search_web` / `scrape_web`。
- **TUI 品牌**：`Brand.Kind`（WEB / TUI / HYBRID）、三个纯终端品牌的安装与启动、
  网页/终端/Go 客户端按 `kind` 渲染。
- **终端加固**：`TerminalTarget` 拒绝 shell 与脚本；PTY 始终 argv 直启；attach 前把
  home/workspace 交给 agent uid。
- **界面**：主侧栏「账户 → 游戏 → 聊天 → 媒体 → 设置」；TUI 品牌独立导航。
- **守门修复**：NOTICE、SQLite 降级、预搜索出锁、前端 TUI 列表读服务端、CI 覆盖
  `web-shell`。

## 2. 守门结论与处理（2026-11-14）

| 级别 | 问题 | 处理 |
|---|---|---|
| P1 | NOTICE 未登记 sqlite-jdbc / PDFBox / POI 等新增依赖 | 已补（`73c9c86aa`） |
| P2 | SQLite 原生库加载失败会让整个面板起不来 | 聊天改为可降级：不注册 `/api/chat/*`，面板继续启动（`c19e08c69`） |
| P2 | 先搜后答在全局锁内做网络 I/O，慢搜索卡住所有会话 20 秒 | 移出锁，搜索后重新校验 busy 与额度（`c19e08c69`） |
| P2 | 前端硬编码 TUI 品牌列表，与后端 `Brand.kind()` 重复 | 登录后读 `/api/brands/capabilities`，硬编码只做兜底（`cc0ce67b4`） |
| P2 | `web-shell` 推送不跑 CI | build.yml 增加分支触发（`0d0f9ee3e`） |
| P3 | `Brand.hasTerminal()` 对 Octop 仍返回 true | 未改，影响仅限 capabilities 字段 |
| P3 | 每日 token 上限进程内计数、重启归零 | 已写入 CHANGELOG「已知限制」 |
| P3 | 备份恢复无 UI | 文档写明手动流程 |

审查细节（攻击面、验证项）见当天的守门报告；这里只留结论。

## 3. 验证与本地产物

- `./gradlew test`：**1483 个用例、0 失败**（254 个测试类）。
- `python3 scripts/api-smoke.py`：**39 项全 PASS**（真实起面板、临时数据目录）。
- `npm run typecheck` / `npm run build` / `npm run compat`：绿。
- `./gradlew shadowJar -PreleaseVersion=0.6.85`：成功。
- jar：`build/libs/hdsl-web-0.6.85.jar`，manifest `Implementation-Version: 0.6.85`；
  发布副本与 `SHA256SUMS.txt` 在 `/tmp/release-v0.6.85/`。

**未验证**：浏览器真机点击（已在容器里由维护者走过）、真实供应商流式对话、Docker 镜像构建
（CI 负责）、JDK 17 运行（本地 JDK 21；CI 的 17/21 双腿覆盖）。

## 4. 发布清单（等维护者明确指令后再执行）

```bash
# 1) 确认工作区干净、main 在预期提交
git status --short && git log --oneline -3

# 2) 打 tag（annotated）
git tag -a v0.6.85 -m "v0.6.85 — 面板聊天、三个 TUI 品牌与终端加固"

# 3) 推送（本机 SSH key 不可用，用 gh 凭据走 HTTPS）
git -c credential.helper='!gh auth git-credential' push https://github.com/nas200-timmy/HDSL-WEB.git main v0.6.85

# 4) 建 Release，附 jar 与 SHA256SUMS.txt（client 流水线会把客户端资产挂上去）
gh release create v0.6.85 -R nas200-timmy/HDSL-WEB \
  --title "v0.6.85 — 面板聊天、三个 TUI 品牌与终端加固" \
  --notes-file /tmp/release-v0.6.85/NOTES.md \
  /tmp/release-v0.6.85/hdsl-web-0.6.85.jar /tmp/release-v0.6.85/SHA256SUMS.txt
```

推送后 CI 会跑 `build`（JDK 17/21）、`client`（五平台 + 挂 Release）、`docker`
（amd64/arm64 → ghcr）。发布说明必须写：**jar 33 MB → 69 MB**、聊天数据在
`<data>/chat`、备份含 API key。

## 5. 下次注意

- `docs/session-handoff-2026-11-12.md` 的分支状态已改成「已并入 main」；再合并/发布后
  继续更新这两份交接稿，不要留「未合并」的旧口径。
- `Brand.hasTerminal()` 仍是恒 true；如果以后要按能力隐藏终端按钮，先修它再给前端用。
- 每日 token 上限要做持久化账本时，`chat_setting` 已经能存 JSON，但计数需要新表。
- 聊天备份恢复没有 UI；恢复时注意 schema 只向前迁移，不接受降级。
## 6. FreeBSD 终端验证（同日，v0.6.85 之后）

真机：FreeBSD 15.1-RELEASE-p6 amd64，openjdk21，2 核 4 GB；面板以 rc 服务跑在
`/home/dev`（jar `/home/dev/hdsl-web.jar`，数据 `/home/dev/hdsl/data`，HTTPS 3080）。

- **根因确认**：`Pty.spawn` 只分 LINUX/MACOS 与 WINDOWS，FreeBSD 落到 `PtyException`。
  FreeBSD 本身有 PTY，是代码缺口。
- **改动**：`Pty.spawn` 增加 FREEBSD；`PosixPty` 的 `F_DUPFD_CLOEXEC` 用 17，
  `TIOCSWINSZ` 沿用 BSD 的 `0x80087467`；`NodeRuntimeManager.platformTag` 对 FreeBSD
  明确拒绝（原先会落到 linux-x64，装一个跑不起来的 ELF）。
- **验证方式**：打 dev jar → 用 `PtyProbe` 直接走 `Pty.spawn`（openpty + posix_spawn），
  再跑 `PtyProbe2` 验证 resize / stdin / 退出码；随后把 jar 覆盖到服务、重启、健康检查。
  实测输出：`probe-ok`、`24 80`、resize 后 `40 120`、`GOT:hello`、`exit=3`。
- **服务的启动横幅**：仍显示 `minecraft: console pipe`（MC 控制台保持 pipe），文案已从
  「no PTY on this platform」改成「PTY console not enabled on this platform」。
- **系统指标**：`SystemMetrics` 在 FreeBSD 上 host=other、CPU 0.0、disks/network/gpu 为空
  （依赖 `/proc`、`/sys`），没有抛异常；文档已写成已知限制。
- **未做**：MC 控制台 PTY（需要真实服务端跑一轮）、uid 分离、`/proc` 指标替代实现。
  真机上 `claude-web` 实例仍在，浏览器点「终端」即可验证修复；本次没有猜/复用管理员口令。

### 6.1 安装链路的三个上游问题（同日续查）

真机上按用户提供的日志逐个复现，结论分开处理：

| 包 | 现象 | 根因 | 处理 |
|---|---|---|---|
| dsh | 先 `node-pty` 的 `gmake` 编译失败；pin 到 1.1.0 后又报 `Cannot find module .../napi-v9-freebsd-x64/require_builtin.node` | 两层上游问题：node-pty 1.2.0-beta.x 缺 FreeBSD 定义（1.1.0 可编），但 `node-addon-require-builtin` 根本没有 FreeBSD 构建，上游 `platform.cc` 对 FreeBSD 返回 `UnsupportedContextReader`，而 `dsh-app-boot` 启动时无条件依赖它 | 最终判定 dsh 0.2.x 在 FreeBSD 上不可用：`DshVersionManager.installableOn(FREEBSD)=false`，面板直接拒绝安装；node-pty pin 已撤掉 |
| opencode | postinstall 提示「手动装 opencode-freebsd-x64」 | 上游 1.19.5 的 optionalDependencies 只有 linux/darwin/windows，**没有 freebsd 包**（npm 404）；提示文案是误导 | `Brand.OPENCODE.installableOn(FREEBSD)=false`，品牌页直接给原因 |
| ZCode | `packages/desktop postinstall` 下载 Electron 404 | Electron 没有 FreeBSD 构建，桌面端 postinstall 必然失败 | `ZcodeBuilder.start` 在 FreeBSD 上直接拒绝，避免十分钟后才失败 |

验证：在 VM 上用与面板相同的 manifest 结构手工安装 dsh（overrides: node-pty 1.1.0），
`pnpm install` 成功、`node-pty` 编译出 `pty.node`、`dsh --version` 输出 `0.2.2-rc.3`；
用 dsh 自己的 subprocess-local 包 `require("node-pty")` 并 spawn `/bin/sh`，输出
`pty-ok` / `24 80` / `exit=0`。代码侧用 `DshManifestTest` 固定「FreeBSD 才加 pin」。

### 6.2 dsh 的最终结论：上游 native addon 没有 FreeBSD 实现

`node-pty` 只是第一层。修掉之后 dsh 启动仍会在 `dsh-app-boot` 的
`profile-resolution-bootstrap.js` 里挂掉：

```text
Cannot find module .../node-addon-require-builtin/build/napi/napi-v9-freebsd-x64/require_builtin.node
  at tryRequireLocal (.../node-addon-native-custom-loader/lib/index.js:467)
  at loadEntry (...:536)
```

核对过的事实：

- `node-addon-require-builtin@0.1.9` 的 optionalDependencies 只有 darwin / linux / win32，
  npm 上也没有 `node-addon-require-builtin-freebsd-x64`（404）。
- 上游仓库 `deepseek-ai/dsh-node-addon-require-builtin` 是公开的，源码 `packages/native/
  src/runtime_context/platform.cc` 对 `__FreeBSD__` 没有分支，落到
  `UnsupportedContextReader`；所以「自己编一个」也拿不到可用的 context reader。
- `dsh-app-boot@0.2.0-rc.1` 及以后全部依赖它；之前的 0.1.x 太旧，不作为方案。
- 即便 `--expose-internals` + JS shim 能绕过，也改的是上游内部契约，不值得在面板里做。

因此：**FreeBSD 上面板、品牌实例（kimi / claude-web / Qwen / MiMo / MiniMax 等）与
交互式终端可用；dsh 实例不可用**，面板现在直接拒绝安装并在「设置 → 通用 → 平台兼容性」
里写明原因。

### 6.4 为什么「从源码构建 addon」也救不了 dsh（已撤除）

在 6.3 的基础上继续跑，addon 编译、注入都成功，dsh 也能启动并打印 web URL；但会话
持久化初始化时抛出：

```text
flock is not supported on freebsd-x64
  code: 'ERR_FLOCK_UNSUPPORTED_PLATFORM'
```

来源是 `@deepseek-ai/node-addon-system@0.1.2/lib/flock.js`：

```js
if (platform !== 'linux' && platform !== 'darwin') {
  throw ... `flock is not supported on ${platform}-${arch}`
}
```

调用方 `dsh-session-persistence-jsonl` 用它给会话文件加非阻塞 `flock` 写租约；代码把
「平台不支持」和其他错误一起 rethrow，没有降级成无锁。继续往下只有两条路：改 dsh 的锁
语义（no-op，破坏跨进程互斥），或再补一个 FreeBSD 的 system addon——两者都不值得。

结论：**dsh 在 FreeBSD 上不可用是上游的多层主动拒绝**（native addon + flock 平台门），
不是面板能补的缺口。已撤回实验构建器与 UI 卡片；平台声明、安装门与文档保留结论。

## 7. v0.6.86 同版本重切（同日）

> 这一段记录 DeepSeek 官方预设助手合入后的分支/发布口径。v0.6.87 曾短暂发布，
> 因真实请求形状问题撤回，不再作为版本号出现。

- **基线**：`1f10e17a8`（v0.6.86）。`1f10e17a8` 之后的提交全部属于 `web-shell`，
  `main` 在重切前保持在基线上。
- **重接**：`web-shell` 直接基于 `1ff29d41b`，包含按块时间线、DSML 兼容与捕获组修复；
  旧的 `3e7807125` 合并提交不再使用。
- **合并点**：`37fa97320 merge: web-shell → main（DeepSeek 官方预设助手、官方搜索与 DSML 兼容）`。
  该合并点之后 `main` 才包含聊天新功能。
- **发布**：`v0.6.86` 同版本重切，覆盖原 tag、Release 与容器；`v0.6.87` 的 tag/Release 已删除。
  同版本重切意味着客户端不会看到版本号变化，发布说明与资产以本次为准。
- **已知取舍**：同版本覆盖后，已经拉过旧 v0.6.86 的部署不会因为版本号比较而提示更新；
  需要手动换 jar/镜像。聊天 schema 仍是 v4，可向前迁移。
