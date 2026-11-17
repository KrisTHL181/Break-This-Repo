# hdsl-tui — 在终端里进入 HDSL-Web 的实例

`client/hdsl-tui` 是给 HDSL-Web 面板配的终端客户端：在真终端里登录面板、提权、
选实例，然后把 agent 的 PTY 原样搬到你面前。它**只做渲染**——HTTPS 判定、Web Shell
总开关、开发者密码提权全部仍由面板决定，客户端不复制任何安全逻辑。

协议契约见 [`docs/terminal-protocol.md`](../docs/terminal-protocol.md)。

## 安装

从 Release 页下载对应平台的二进制即可，没有运行期依赖（`CGO_ENABLED=0` 静态链接）：

| 平台 | 资产名 |
|---|---|
| Linux x86-64 / arm64 | `hdsl-tui-<版本>-linux-amd64` / `-arm64` |
| macOS Intel / Apple Silicon | `hdsl-tui-<版本>-darwin-amd64` / `-arm64` |
| Windows x86-64 | `hdsl-tui-<版本>-windows-amd64.exe` |

下载后 `chmod +x` 放进 `PATH`，用 `hdsl-tui --version` 确认能跑。
Release 同时提供 `SHA256SUMS-client.txt`，请先核对校验和。

从源码构建（仓库里已带 `vendor/`，**不联网也能编**）：

```sh
cd client/hdsl-tui
GOFLAGS=-mod=vendor GOPROXY=off CGO_ENABLED=0 go build -trimpath -ldflags "-s -w -X main.version=v0.5.71" ./cmd/hdsl-tui
```

需要 **Go 1.24 或更新**（`go.mod` 的 `go 1.24.0`；依赖锁在同时支持 1.24 的版本上，
所以发行版自带的 Go 通常就够）。也可以用 `scripts/install-hdsl-tui.sh --build`，
它会下载源码包并离线编译。

## 用法

```sh
hdsl-tui                      # 全屏菜单：选服务器 → 登录 → 选实例 → 进终端
hdsl-tui https://panel.example.com
hdsl-tui --brand kimi --instance kimi-1 https://panel.example.com
```

不带地址时进入菜单；带地址时跳过「选服务器」那一步，其余一样。选项写在面板地址前面或
后面都可以：`hdsl-tui --insecure https://panel.example.com` 与
`hdsl-tui https://panel.example.com --insecure` 是同一条命令（面板地址只能有一个）。

| 选项 | 说明 |
|---|---|
| `--instance <id>` | 登录后直接进入该实例的终端（菜单仍可用） |
| `--brand <name>` | `kimi` / `opencode` / `claude-web` / `hermes` / `pi-web` / `zcode`，默认走 dsh 实例 |
| `--resume <sessionId>` | 附着到该 harness 会话（kimi / opencode 的会话恢复） |
| `--kernel <name>` | Claude Web 的内核：`community`（社区引擎）或 `official`（官方 Claude Code）；不给就在菜单里选 |
| `--user <name>` | 面板用户名，默认 `admin`；给了就不再提示 |
| `--ca <pem>` | 额外信任的 CA 证书（PEM 文件），用于自签面板 |
| `--insecure` | 全局跳过证书校验；**会先打印 SHA-256 指纹并要求确认**。不需要全局跳过时，菜单会在证书校验失败时当场给「仅本次忽略风险」，见 [TLS](#tls) |
| `--plain` | 用逐行问答的旧界面（无终端时自动启用） |
| `--version` | 打印客户端版本与 `protocol` 后退出，不联网 |
| `--verbose` | 把请求与握手细节写到 stderr |

### 菜单（全屏）

界面照 Debian 安装器做的：蓝底、方框、高亮行、底部键位说明。

```text
╔═ 选择服务器 ══════════════════════════════════════════╗
║  选择要连接的 HDSL-Web 面板。地址会记在本地，下次直接选。 ║
║  https://panel.example.com            上次使用 10-23 21:14 ║
║  ＋ 输入新的服务器地址…                                ║
║  退出程序                                              ║
╟────────────────────────────────────────────────────────╢
║ ↑/↓ 选择    Enter 确定    Esc / Ctrl-C 退出            ║
╚════════════════════════════════════════════════════════╝
```

层级是 **服务器 → 主菜单 → 分类（dsh/kimi/opencode/claude-web/pi-web/zcode）→ 实例 → 动作**。
每一层都有 `← 返回` 项，`Esc` 也逐级回退；`Ctrl-C` 在任意输入页取消当前动作，
在根层（服务器列表）退出程序。退出 agent（`Ctrl-] q`）或 agent 自己退出，都只回到
实例动作菜单——**登录状态和面板连接都还在**，不会连人带会话一起退出。

| 按键 | 行为 |
|---|---|
| `↑` `↓` / `j` `k` | 移动光标 |
| `PgUp` / `PgDn` / `Home` / `End` | 翻页 / 跳到首尾 |
| `Enter` | 确认 |
| `Esc` | 返回上一级（输入页是取消） |
| `Ctrl-C` | 取消当前动作；根层退出程序 |
| `Ctrl-U` | 清空当前输入框 |

`j` `k` 只在菜单里是移动光标；输入页（地址、用户名、密码）里它们照旧是普通字符。

### 凭证与本地状态

- 面板密码用关闭回显的方式读，**不进 argv、不进日志**。
- 登录成功后会话 Cookie 存到 `~/.config/hdsl-tui/config.json`（目录 `0700`、文件 `0600`，
  可用 `HDSL_TUI_CONFIG` 改路径）。下次启动对同一面板直接复用，不再问密码；
  「主菜单 → 清除保存的登录信息」删掉它，或直接删文件。
- 开发者密码不再每次都要：打开终端前先读一次 `GET /api/developer/status`，面板说没提权才
  提示（顺序与网页端一致：密码 → 内核 → 终端），之后 30 分钟（面板的滑动窗口）内不再问；
  面板从来没设过开发者密码时直接说明去哪里设，不问一个不可能对的密码。老面板没有这个端点
  （404）时退回旧路径：开终端遇到 `developer_elevation_required` 再提示。
  也可以在「主菜单 → 开发者提权」主动提权。
- 服务器列表同样存在这个文件里，最近使用的排在最前。

### 会话中的按键

| 按键 | 行为 |
|---|---|
| `Ctrl-]` 然后 `q` | 离开终端，回到实例菜单，agent 继续在后台跑 |
| `Ctrl-]` 然后 `k` | 停掉终端（`DELETE …/terminal`），回到实例菜单 |
| `Ctrl-]` `Ctrl-]` | 给 agent 发一个字面量 `Ctrl-]` |
| 其他按键（含 `Ctrl-C`） | 原样发给 agent，不被本地吞掉 |

窗口大小：Unix 用 `SIGWINCH`；Windows 没有这个信号，改为每 500 ms 采样一次，变化才发
`{"type":"resize"}`。agent 自己退出时，菜单里会提示它的退出码。

连接本身每 20 秒发一次 WebSocket ping。终端安静是常态（agent 在思考或在等输入），所以
「没数据」本身说明不了什么；ping 加 pong 期限才能把**安静**和**半死**（NAT 忘了这条流、
面板没关连接就消失、网线拔了）分开：发出 ping 后 10 秒内既没有 pong 也没有任何数据，就按
断开处理，报「与面板失去心跳」并回到菜单，不会干挂着假装还连着。ping 是控制帧，只在字节
帧之间发出，永远不会切开一个消息；agent 一直在输出时也不会被误判（收到任何数据都会续期）。
面板侧同一条连接已经关掉了空闲超时，所以安静的终端不会被面板主动断开。


## TLS

默认做完整校验。自签或私有 CA 的面板有两条路：

- 推荐 `--ca server.pem`：把这张 CA 证书加进信任池，其余校验照常。
- 兜底 `--insecure`：跳过校验。带了面板地址时，会先把证书的 SHA-256 指纹、颁发对象、
  有效期打印出来；**stdin 是终端**时要求你手输 `yes`，**不是终端**（脚本、CI、ECS 控制台）
  时打印警告后直接继续——那类运行本来就没法回话报错退出等于用不了。不带地址（菜单模式）时
  还没有证书可看，改为在选择服务器之前弹一页风险确认。
  指纹要自己核对（比如从面板的「设置 → HTTPS 证书」抄一份），不然这一步只是让你多敲了
  几个字符。

菜单里连一台面板时，如果证书不能证明它的身份，现在不会直接报错退出，而是给一页说明
**哪里不对**（证书里没有这个地址 / 签发者不被信任 / 已过期），下面是两个选项：

```
 ← 返回（推荐）
   仅本次忽略风险并连接
```

光标默认停在「返回」，回车不会顺手关掉安全校验。选「仅本次」之后：REST 与 WebSocket
一起改为不校验（不会一半校验一半不校验），状态栏常驻「证书未验证」，这个选择只在本次
运行内有效——不写进 `config.json`，换服务器或下次启动都恢复完整校验。

只有证书类失败会出现这一页。DNS 失败、连接被拒、超时、面板返回 403/404 都不会，因为
那些不是「可以用一次风险换通」的问题。

> 证书里没有 IP 是最常见的触发点：面板证书只签了域名，客户端却用 `192.0.2.10` 连。
> 根治办法是让证书包含这个地址——面板「设置 → HTTPS 证书」生成的自签名证书会把本机
> 所有网卡地址写进 SAN——再用 `--ca` 信任它。

## Windows

Windows 客户端连接 **POSIX 面板**是正常的。反过来则不行：Windows 面板上「终端」是在
面板所在机器的桌面上开一个控制台，没有 PTY、没有 ticket，接口返回
`{"mode":"desktop","hint":"…"}`；客户端会把 `hint` 原样打印出来并以退出码 4 结束。这是
平台限制（要做需要 ConPTY，见 `docs/` 里的规划），不是配置问题。

Windows 上客户端会自行打开控制台需要的开关并在退出时恢复原状：

- 输出句柄启用 `ENABLE_VIRTUAL_TERMINAL_PROCESSING`；
- 输入句柄启用 `ENABLE_VIRTUAL_TERMINAL_INPUT`；
- 输入/输出代码页设为 65001（UTF-8）。

conhost 与 Windows Terminal 的行为差异、`Ctrl-C` 语义、窗口缩放属于必须真机验证的部分；
本仓库的 CI 只能覆盖交叉编译，覆盖不了控制台行为。

## 排错

| 现象 | 含义与做法 |
|---|---|
| `面板不在 HTTPS 下…` | 面板要求 HTTPS 才能开终端：在「设置 → HTTPS 证书」里启用，或打开 `https.trust_forwarded_proto` |
| `Web Shell 已在面板里关闭…` | 「设置 → 开发者」里的 Web Shell 开关是关的 |
| `还没设置开发者密码…` | 先在「设置 → 开发者」里设置开发者密码 |
| `需要先提权…` | 提权是 30 分钟滑动过期，重新运行本命令并输开发者密码 |
| `开发者密码不正确` / `用户名或密码不正确` | 密码不对；连续失败会被面板临时锁定 |
| `该实例已经有终端在运行…` | 重连即可，不必另开 |
| `未安装终端界面插件，请先到实例的「安装」页安装一个 TUI 插件…` | dsh 的终端是插件，而**客户端装不了插件**：在面板的实例「安装」页装一个再重跑；这条是策略拒绝（退出码 4），重试命令没有用 |
| `找不到该实例…` | 不带 `--instance` 跑一次拿到列表 |
| `服务端拒绝了这个连接（1008）` | ticket 过期或不属于当前会话，重新运行本命令 |
| `服务端没有正在运行的终端（1011）` | agent 已经不在了，重新运行本命令 |
| `与面板失去心跳…` | 连接安静超过 pong 期限（默认：发出 ping 后 10 秒内既没有 pong 也没有数据），通常是 NAT/防火墙把这条流丢了、或面板进程消失而没关连接；重新运行本命令即可 |
| `这台面板运行在 Windows 上…` | 见上面的「Windows」一节 |
| `--insecure 需要交互确认…` | 只在 stdin 是终端、却读不到输入时出现（例如被 `</dev/null` 重定向）；脚本里直接跑不需要确认 |
| `无法关闭回显读取密码…` | 密码提示需要一个真终端；管道里跑不了 |

## 退出码

| 码 | 含义 |
|---|---|
| 0 | 正常结束：`Ctrl-] q` 离开、`Ctrl-] k` 停掉、agent 以 0 退出 |
| 2 | 认证或提权失败（登录 401、开发者密码 401、被锁定、需要提权、ticket 1008） |
| 3 | 无法连接面板或连接中断（DNS/连接/TLS 失败、socket 断开、心跳超时、1011） |
| 4 | 策略拒绝，重试无用（面板不在 HTTPS、Web Shell 关闭、实例没装 TUI 插件、面板启动终端失败、旧面板的 Windows 桌面终端） |
| 5 | 用法错误（参数、面板地址、读不到密码） |
| N | agent 自己以 N 退出时，客户端原样透传 |

## 目录

```
cmd/hdsl-tui/       参数、选实例、生命周期、TLS 与指纹确认
internal/panel/     REST：登录 / 提权 / 开发者状态 / 实例列表 / 开终端 / 关终端
internal/pterm/     WebSocket：字节通道与控制帧
internal/termx/     raw 模式、窗口尺寸、Windows 控制台（build tags 分平台）
internal/cli/       实例选择器、detach 状态机、错误文案与退出码
```

测试不需要真终端（raw 模式抽成 `termx.Raw`，CI 用 `termx.Nop`）：

```sh
GOFLAGS=-mod=vendor GOPROXY=off go test ./...
```

## 依赖

随仓库提交（`vendor/`），构建完全离线；升级是显式提交：

| 模块 | 版本 | 用途 | 许可 |
|---|---|---|---|
| `github.com/gorilla/websocket` | v1.5.3 | WebSocket 客户端 | BSD-3-Clause |
| `golang.org/x/term` | v0.47.0 | raw 模式、尺寸、无回显读密码 | BSD-3-Clause |
| `golang.org/x/sys` | v0.49.0 | Windows 控制台模式与代码页 | BSD-3-Clause |

升级方式：`go get -u <pkg> && go mod tidy && go mod vendor`，并在 `NOTICE` 里同步版本。