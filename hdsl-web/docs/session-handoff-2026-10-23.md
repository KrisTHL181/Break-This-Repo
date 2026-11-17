# 会话交接：HDSL-web（2026-10-23）

> 本稿接住 **v0.5.2（Web Shell 正式版）与 v0.5.21（终端空白屏修复）** 这段工作：
> 实例级原地终端从设计到上线、SSH 降级为可选、开发者密码迁出 SSH 包、Windows 桌面
> 便利功能，以及一次真实白屏事故的根因修复。目标是**不回看会话原文也能接着干**。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例
> 统一用 `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。仓库账号 `nas200-timmy`
> 属公开信息。

## 0. 当前状态

- 分支 `main`，HEAD `45fb7e0`（`docs(changelog): v0.5.21`）；工作树干净，`origin/main` 同步。
- 标签：**v0.5.21**（`45fb7e0`，Latest）、**v0.5.2**（`4873fd6`）；分支 `web-shell`
  保留在远端（`a773776`），已 `--no-ff` 合进 main。
- 测试基线：**729 用例 / 0 失败 / 0 错误 / 2 跳过**（默认构建，Linux，JDK 21 与 17 各跑一遍；
  跳过的是 Windows 专有的 junction 用例与一个平台用例）。
- 容器镜像：`0.5.21` / `0.5` / `latest` 同一 index digest
  `sha256:8e80688b4d5268127b6146fe3434b9c79a0e1b548f81c03a30d4bc9a38b1263a`，
  `linux/amd64` + `linux/arm64`。**镜像由默认构建产出，不含 SSH 服务端。**
- Release 资产（两个 jar + 校验和）：
  - `hdsl-web-0.5.21.jar`（默认，无 SSH）`7eb37d34…df9668`
  - `hdsl-web-0.5.21-full.jar`（`-PwithSsh`）`e88214da…5ceb3f`
- 人肉测试已通过（opencode 完整界面、kimi、dsh TUI、刷新重连、底栏停止）。

## 1. 版本脉络

| 版本 | 内容 | 备注 |
| --- | --- | --- |
| v0.5.0 | SSH 终端通道 M0–M4 | 默认关闭；门禁/菜单/attach/审计 |
| v0.5.1 | 门禁与提权加固 | 每次错答计数、30 分钟全局封禁、提权按会话、dev-password 预算 |
| v0.5.2 | **Web Shell：实例级原地终端** | 本段主体：JNA PTY、会话常驻、kimi/opencode/dsh 适配、前端 xterm、SSH 默认停用 |
| v0.5.21 | 终端空白屏与连接稳定性 | xterm × esbuild 压缩冲突；重连/尺寸/错误可见性；正式化为补丁版本 |

版本号习惯沿用之前的小版本写法（`0.5.2` 之后是 `0.5.21`，`sort -V` 判定 `0.5.21`
更大，docker 的 `latest` 闸门按这个排序工作）。

## 2. 包与职责

```
web/terminal/                 常置包，不依赖 MINA / web.ssh
  TerminalTarget              实例 → argv / cwd / env
  BrandTui + KimiTui / OpencodeTui
  DshTui                      dsh 的 tui 子命令（不是 web 命令加参数）
  TerminalSink                一个挂载者（生产是 WS，测试是收集器）
  TerminalSession             PTY + 回放环形缓冲 + attach/detach + 退出
  TerminalManager             实例级单会话、open/terminate/close、审计
  TerminalTickets             一次性票据（30 秒、绑定实例与会话 token）
  DesktopTerminal             Windows：桌面控制台
  pty/                        JNA 胶水：Pty / PtySize / PtyException / PosixPty
web/developer/                常置包：第二因子，与 SSH 是否在包里无关
  DeveloperAuth               开发者密码（原 web.ssh，PBKDF2，0600）
  DeveloperElevation          会话 token 记键、15 分钟滑动、3 次失败锁 10 分钟
  DeveloperApiServlet         /api/developer/{status,elevate,password,password/clear}
web/terminal/... 之外：
  BrandApiServlet             /api/brands/{brand}/instances/{id}/{terminal,reveal}
  InstancesApiServlet         /api/instances/{id}/{terminal,reveal}（dsh）
  TerminalWebSocket           /ws/terminal/{ticket}
web/ssh/                      只剩传输与认证；默认构建整包剔除
```

单向依赖：`web.ssh → web.terminal`、`web.developer`；反向都不允许。

## 3. 终端语义（实现口径）

- **以实例为单位**：argv 由实例的安装目录、home、工作区决定；`HOME`/`USERPROFILE`
  指向实例 home，TUI 看到的就是网页模式的那份会话。
- **常驻**：PTY 进程属于 `TerminalSession`，不属于 WebSocket。关窗/刷新只 detach；
  运行中的会话在下次打开时直接发新票据并回放最近 256 KB。
- **终止**：`waitpid` 返回后先 `close(master)`，再 `kill(-pgid)`、移出 registry、发
  `exit{code}`、审计——关 master 之前不再有任何可观察输出。
- **无空闲超时**：人盯着 agent 跑长任务时终端是静止的，不能因此杀；靠断连 + 显式停止。
- **单实例单会话**：第二个 attach 复用同一个进程（v1 不做单控制端仲裁，两个窗口都会
  收到广播、输入都会写入）。

## 4. 授权与威胁模型

- 打开终端：`/api/*` 会话门 → 开发者密码提权（15 分钟滑动）→ Windows 桌面启动额外要求
  `{"confirm":"open-desktop-terminal"}` 并记 `terminal.desktop` 审计。
- **入口等价于 RCE**（它启动 agent 进程）：本地权限不是边界，远程入口才是。部署前提是
  面板只监听回环或可信网络；要暴露公网必须在网络层加保护。详见 `docs/web-shell.md §4.1`。
- 默认构建也能设置开发者密码（`/api/developer/password`，TLS + 面板密码确认 + per-IP 预算），
  不再依赖 SSH 构建。
- SSH 侧仍保留 `/api/ssh/elevate` 与密码路由，行为不变，共用同一个 `DeveloperElevation`。

## 5. 品牌适配（按真实安装布局）

| 品牌 | 入口 | 说明 |
| --- | --- | --- |
| kimi | `node <包>/dist/main.mjs` | pnpm 的 `.bin/kimi` 是 `#!/bin/sh` shim，**不是软链**；只有 npm 软链布局才回退 |
| opencode | `opencode-ai/bin/opencode.exe` | 1.19 起由主包带平台二进制（POSIX 也是 `.exe` 的 ELF）；再退回无 scope `opencode-<os>-<arch>`，最后旧 `@opencode/cli-*` |
| dsh | `dsh tui [--resume]` | `DshLauncher.plan` 给的是 **web** 命令，取前两个元素加 `tui`，不能把 `tui` 追加到 web 参数后 |

`KimiTui` 的 node 优先用面板托管运行时（`<data>/hdsl/runtimes`），没有再回退系统 PATH。

## 6. Windows

- 不做 ConPTY：Windows 有桌面，终端按钮在**桌面会话**里开控制台（`DesktopTerminal` 写
  `.cmd` + `cmd.exe /c start`）。面板必须以控制台或 RDP 会话运行（`SESSIONNAME` 判断）；
  服务（session 0）下 `reveal` 与桌面终端返回 409，不静默失败。
- `reveal`：`explorer.exe` / `notepad.exe` 打开实例 home / workspace / 日志；路径由服务端
  按实例记录计算，请求只能点名三个目标之一。dsh 日志在内存里，`log` 返回 400。
- 会话租约观察仍不支持；NTFS 没有 `0600` 语义。

## 7. v0.5.21 事故：终端空白屏

- **现象**：WS 已 attach、字节也到了浏览器（回传落盘核对 13,745 字节完整），画面永远空白。
- **根因**：xterm 6.0.0 的 `requestMode` 被 esbuild 二次压缩改坏
  （`return f=…, p=…; var f,p`），opencode 启动连发 6 个 DECRQM（`CSI ? Ps $ p`）时抛
  `ReferenceError`，xterm 的写入队列停摆，后续输出不再解析。
- **修法**：`web/vite.config.ts` 的 `build.minify: false`（实测约 +70 KB gzip）。
  备选是钉 xterm 5.x 或改 terser，都未在真机验证；当前取舍以「已实测能完整出画面」为准。
- 同时修复：`onClose` 用 ref + `openSession` 加 generation（父组件重渲染导致的反复重连）、
  WS open 后补发真实尺寸、WS 错误/异常关闭可见、Blob 帧兜底、重连前 teardown 旧视图、
  标题栏「已接收 N KB」；后端补 attach/发送/握手日志。

## 8. 关键提交

- `7d6da02` PTY 胶水（JNA）+ 设计文档
- `9a861f3` 品牌适配器 + `TerminalSession`/`TerminalManager`
- `74cb175` HTTP/WS + Windows 桌面终端
- `9e03e4f` `web.developer`：开发者密码与提权迁出 `web.ssh`
- `aeb5e12` 桌面入口确认 + 审计 + 威胁模型文档
- `14a6b30` 前端 xterm + dsh 适配 + 默认不带 SSH
- `e1be10a` 默认构建可设开发者密码；失败返回 `terminal_failed: <原因>`
- `a773776` 三个入口适配器按真实布局修正（pnpm shim / opencode-ai / dsh tui）
- `8d1b353` 空白屏根因修复 + 连接稳定性
- `4873fd6` v0.5.2 合并；`45fb7e0` v0.5.21

## 9. 构建与发布

- 默认：`./gradlew build` → 不含 `web/ssh`、MINA、eddsa；`web/terminal` 在。
- 含 SSH：`./gradlew shadowJar -PwithSsh`。
- 发布流程沿用：合并 main → CHANGELOG → `-PreleaseVersion=<ver>` 打默认与 full 两个 jar →
  annotated tag → push main/tag → 等 build（JDK 17/21）与 docker → 建 Release（两个 jar +
  `SHA256SUMS.txt`）→ 核对资产 digest 与 ghcr 三标签同一 digest。
- `SHA256SUMS.txt` 里必须是**纯文件名**（不要带 `build/libs/` 前缀）。

## 10. 已知缺口与后续

- **终端布局/UI**：用户对当前覆盖层与底栏形态还不完全满意，属于体验迭代，不影响功能。
- **单控制端仲裁**：两个窗口同时 attach 时输入都会写入；v1 接受，后续可做 controller/observer。
- **dsh 真机端到端**：本工作区没有 dsh harness 安装，`DshTui` 只靠单测锁定命令形状；
  宿主环境已验证过 TUI，但仓库里没有自动化覆盖。
- **cgroup 加固**：`setsid` 逃出进程组的孤儿 `kill(-pgid)` 清不掉（master 已关，无法交互）；
  要「一个都不留」需 Linux cgroup v2。
- **minify:false 的体积**：+约 70 KB gzip；若将来想恢复压缩，先钉 xterm 版本或换 terser 并真机验证。

## 11. 排查入口（下次接手先看这里）

- 终端打不开：界面会显示 `terminal_failed: <原因>`；面板日志搜 `/api/.../terminal` 与
  `Terminal WebSocket`。
- 画面空白但标题栏「已接收 N KB」在涨：渲染层问题（先查 xterm 与压缩），不是传输层。
- 连接反复断开：查 `Terminal WebSocket closed` 的关闭码与浏览器 Network 面板 WS 帧。
- 构建产物核对：`unzip -l <jar> | grep -c 'org/jackhuang/hmcl/web/ssh/'`（默认应为 0）、
  `.../web/terminal/`（应存在）。
