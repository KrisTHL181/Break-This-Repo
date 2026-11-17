# 会话交接：HDSL-web（2026-10-24）

> 本稿接住 **v0.5.71** 这一版：ZCode 终端接入、Claude Web 双内核、打开终端改成「先密码
> 再终端」、`hdsl-tui` 的证书应急连接，以及子路径反代下的两处加固。目标是不看会话原文
> 也能接着干。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一
> 用 `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。仓库账号 `nas200-timmy` 属公开信息。

## 0. 当前状态

- 分支 `main`，本地已合并 `web-shell`（`--no-ff` 合并提交），**尚未推送**；`web-shell`
  分支保留。
- 版本号：**v0.5.71**（未打标签）。选择理由见 §1。
- 测试基线（本机 Windows，2026-10-24）：后端 `web.{auth,developer,brand,http,terminal,zcode}`
  **138 用例 / 2 失败 / 20 跳过**；两个失败是既存的 `BrandTuiTest` POSIX 权限用例
  （`setPosixFilePermissions` 在 Windows 抛 `UnsupportedOperationException`），20 个跳过是
  Linux-only 的 PTY 用例。前端 `tsc --noEmit` 与 `vite build` 通过。客户端
  `go vet` / `go test ./...` 全绿，5 平台交叉编译通过。
- Release 资产（jar、校验和、客户端二进制）**还没构建**。

## 1. 为什么是 0.5.71

v0.5.21 当初把补丁号写成两位（本意是 0.5.2.1）。`sort -V` 因此把 `v0.5.21` 判得比
`v0.5.5` / `v0.5.6` 大，docker 的 `latest` 闸门（`docker.yml` 里 `sort -V | tail -1`）一直
指着它。这次直接取 **0.5.71**：数字上明确大于 0.5.21 与 0.5.6，把错号盖过去，不需要挪任何
旧标签或重发旧 Release。

顺带把两条没发布过的 CHANGELOG 记录（原 `v0.6.0` 与 `v0.5.7`）合并成一条 `v0.5.71`——
两个版本号都从未出现在 Release 列表里，分开记会让人以为漏了版本。

## 2. 这一版改了什么

### 2.1 打开终端：先密码，再终端（全品类）

`TerminalWindow` 的相位分两类：`opening / password / setup / kernel` 画**对话框**，
`connecting` 起才是终端窗口。挂载后先查 `developerStatus`，据此决定设置页 / 密码页；
密码通过 →（Claude Web 且两个内核都可用时）选内核 → 申请票据 → 终端窗口挂载完成后再接
WebSocket（票据存 `pending`，不会因为 host 尚未渲染就丢连接）。断线重连沿用已选内核。

### 2.2 Claude Web：两个内核由用户选

按上游 `server/src/claude-exe.ts` 的真实布局解析：

| 内核 | 入口 | 启动 |
|---|---|---|
| 社区引擎 | `node_modules/claude-web-engine/dist/cli-node.js` | `node <file>` |
| 官方 | `node_modules/@anthropic-ai/claude-agent-sdk-<platform>-<arch>/claude[.exe]`，缺失退回 PATH 上的 `claude` | 直接执行 |

- `GET /api/brands/claude-web/instances/{id}/terminals` → `{kernels:[{id,label,available,detail}], default}`；
  其他品类 404（它们只有一个 TUI）。
- `POST …/terminal?kernel=community|official`；**不静默替换**，选了没装的那个直接 409
  `terminal_failed: … is not available: …`。
- 网页只在两个都可用时才问；`hdsl-tui` 进入终端前也上下选，`--kernel` 可直接指定。

### 2.3 ZCode 终端

- 发行包不带参数启动就是官方 TUI（`scripts/zcode-distribution/runner.mjs` 会 import
  `agent/zcode.cjs`，CLI 默认命令 `tui`）。适配器只起 `node <发行包>/bin/zcode.mjs`，
  **不传任何参数**；缺 `bin/zcode.mjs` 或 `agent/zcode.cjs` 时提前给出可读原因。
- 状态分两处根：`ZCODE_DATA_BASE_DIR` 装凭据与供应商配置（`…/.zcode/v2/credentials.json`、
  `provider_config.json`），会话库跟 `homedir()`。所以 `HOME` **保持与 web 进程一致**，
  只额外设 `ZCODE_DATA_BASE_DIR`——否则网页与终端是两套会话。

### 2.4 一份门禁、一套生命周期

`InstanceTerminal` 收口 dsh / 品牌 / ZCode 的门禁（Web Shell 开关、HTTPS、开发者密码、
提权）、Windows 桌面分支、spawn 与一次性 ticket。生命周期：开 TUI 前**同步**停 web，
启动 web 前先停 TUI，停止/删除实例连带结束终端；dsh 也从 `stopAsync` 改成阻塞式
`DshProcessManager.stop`。终端注册表按 `brand + instanceId` 存，三个品类不再串会话。

### 2.5 客户端证书应急

`panel.CertificateFailure` 按类型识别 `x509.HostnameError` / `UnknownAuthorityError` /
`CertificateInvalidError` / `*tls.CertificateVerificationError`（穿过 `TransportError` 与
`*url.Error`），菜单给「← 返回（推荐）」与「仅本次忽略风险并连接」，默认停在返回；
接受后 REST 与 WS 一起切换、状态栏常驻「证书未验证」，且只对本次运行有效。非证书失败
不出这个入口。

### 2.6 子路径加固

- 会话 Cookie 的 `Path` 跟挂载点走（`AuthFilter` 新增 `basePath` 参数），同域两个面板
  不再互相踢下线；
- 终端 WebSocket 接受服务端返回的绝对 `ws://`/`wss://`；
- `docs/deployment.md §1` 重写为「谁终结 TLS」的两套 nginx 配置 + 排错表 + 验收命令；
- README 开头补「现在能做什么」。

## 3. 关键文件

```
src/main/java/org/jackhuang/hmcl/web/terminal/InstanceTerminal.java   共用的门禁/生命周期/ticket
src/main/java/org/jackhuang/hmcl/web/terminal/ClaudeWebTui.java      双内核适配器
src/main/java/org/jackhuang/hmcl/web/terminal/ZcodeTui.java          ZCode TUI
src/main/java/org/jackhuang/hmcl/web/brand/BrandApiServlet.java      …/terminal 与 …/terminals
src/main/java/org/jackhuang/hmcl/web/zcode/ZcodeApiServlet.java      /api/zcode/instances/{id}/terminal
web/src/components/TerminalWindow.tsx                                对话框 → 终端、内核选择
client/hdsl-tui/internal/panel/certificate.go                        证书错误分类
client/hdsl-tui/cmd/hdsl-tui/tls.go                                  --ca / --insecure 指纹确认
docs/deployment.md §1                                                反代与 TLS 拓扑
docs/terminal-protocol.md                                            协议（含 kernel 与 terminals）
```

## 4. 验证到哪一步

- 已做：上面 §0 的测试与构建；`ClaudeWebTuiTest` 8 个（含「官方二进制直接执行」「只有 web
  的发行包被拒」）、`ZcodeTuiTest` 4 个、`ZcodeTerminalApiTest` 5 个（门禁顺序）、
  `BrandApiTest` 的内核端点用例、客户端真实 TLS 握手用例。
- 未做：**真机终端**。ZCode 与 Claude Web 的 TUI 能不能在 PTY 里起来、中文/颜色/resize
  是否正常，需要在 Linux/macOS 上对着真实发行包跑（Windows 面板没有 PTY，走桌面控制台）。
- 未做：真实 nginx/Caddy 的 `/hdsl/` 端到端；文档给了可照抄的配置与验收命令。

## 5. 已知缺口

见 [web-shell.md §10](web-shell.md)。摘要：单控制端仲裁未做；ZCode / Claude Web 的 resume
未做；hdsl-tui 的「密码 → 内核」顺序与网页相反（客户端只在 403 时才知道要提权）。

## 6. 发布动作（还没执行）

```bash
./gradlew shadowJar -PreleaseVersion=0.5.71          # → build/libs/hdsl-web-0.5.71.jar
git tag -a v0.5.71 -m "v0.5.71: …"
git push https://github.com/nas200-timmy/HDSL-WEB.git main
git push https://github.com/nas200-timmy/HDSL-WEB.git v0.5.71
gh release create v0.5.71 build/libs/hdsl-web-0.5.71.jar SHA256SUMS.txt --notes-file <notes>
```

客户端 5 个平台的资产与 `SHA256SUMS-client.txt` 由 tag 触发的 `client.yml` 上传；docker
`0.5.71` 会拿到 `latest` 与 `0.5`（`sort -V` 下它最大）。

## 7. 环境备注（下次接手别踩）

- 本仓库 `origin` 是 SSH 地址，这台开发机没有可用 key（`Permission denied (publickey)`）。
  拉取/推送走 HTTPS + `gh` 凭据：
  `git -c credential.helper='!gh auth git-credential' push https://github.com/nas200-timmy/HDSL-WEB.git main`。
- `web/node_modules` 在 Windows 上需要用 npm 重装（Linux 装出来的 `.bin` 没有 `.cmd`，
  `vite` 起不来）；`npm ci` 走 `registry.npmjs.org`，本机 `npmmirror` 会 `ECONNRESET`。
- `npm run compat` 在 Windows 会报 "Did not find any files"（es-check 不做 glob 展开），
  CI 的 Linux 上正常。