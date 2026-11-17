# 会话交接：HDSL-web（2026-10-25）

> **已被 [`session-handoff-2026-10-26.md`](session-handoff-2026-10-26.md) 接续**（v0.5.91：面板自带
> 两个第一方终端与三轮审查修复）。本稿仍是实例生命周期 ↔ 终端、反代、整合包契约、部署与环境
> 备注的权威文本，接续稿只写之后的增量。
>
> 本稿接住 **v0.5.81** 与 **v0.5.82** 两版，外加一轮**外部审查修复**（实例生命周期 ↔ 终端的两处高危、
> PTY 描述符竞态、反代取消/重试、`holder[0]` 清理）。v0.5.82 一版的内容：容器补齐构建工具链与真编辑器；
> v0.5.81：终端入口统一、整合包契约补齐（`home/` / `dshVersions` / `launchers` / 向后兼容）、
> **dsh 终端改成"装一个 TUI 插件"**、终端 WS 的空闲超时、以及 Go 客户端的一批对齐。目标是不看会话
> 原文也能接着干。
>
> **隐私约定（继续遵守）**：真实域名、内网地址、宿主机路径、口令一律不写进仓库；示例统一用
> `dsh.example.com`、`192.0.2.x`、`/srv/...`、`<口令>`。仓库账号 `nas200-timmy` 属公开信息。

## 0. 当前状态

- 分支 **`web-shell`**（已 `merge --ff-only main` 同步到 `c452bce`），本轮改动提交在它上面，
  **`--no-ff` 合并进 `main`** 后发布 **v0.5.83**。
- 版本口径**已定：v0.5.83**（用户拍板）。原本的两条路里选了"新开一版"而不是"撤 v0.5.82 重发"，
  于是 CHANGELOG 重排过：**v0.5.82 段只留它真正含的东西**（构建工具链 + busybox，与已发出的 Release
  说明一致），真编辑器/解压件（`c452bce`）与两轮审查修复都进了 v0.5.83 段。v0.5.82 的 tag 与 Release
  一个字没动。
- **v0.5.83 重切过一次**（同一个版本号，用户要求）：发布后补了「TUI 插件要配对宿主版本」这条闸门
  （见 §1.7 末尾），Release 与 tag 都重新指向含它的提交。要照做时的心得：`gh release delete --yes`
  → `git push origin :refs/tags/v0.5.83` → `git tag -d` → 重打 tag → 推 → `gh release create`
  重新上传 jar 与 `SHA256SUMS.txt`（客户端 5 个资产由 `client.yml` 在 tag 推送后重新挂上）。
- 测试基线（本机 Linux）：后端 **790 用例 / 0 失败 / 2 跳过**（跳过项仍是
  `AuditLogTest.logBeforeInitIsANoOp` 与 Windows 专有项；743 → 790 的差是两轮共新增 49 条、
  删掉 2 条只测已废弃 holder 助手的用例）；前端 `tsc --noEmit` + `vite build` + `compat` 通过；
  客户端 `gofmt` / `go vet` / `go test ./...` 全绿——**本轮没有改客户端**，理由见 §1.6。
- `v0.5.82`（`c0f2544`）的 CI 已确认 `docker` 与 `client` 两条都 success。**v0.5.83 要盯 `docker.yml`**：
  它第一次把 `vim-tiny`/`nano`/`unzip`/`zip`/`bzip2`/`7zip` 装进运行镜像，而 arm64 那条腿要在 QEMU 下
  装这些包（本机只做过 `apt-get -s install` 的模拟，没跑过真构建）。

## 1. 这一版改了什么

### 1.1 终端入口统一为「终端窗口」

四处入口（实例列表 `InstancesPage`、实例详情 `InstanceDetailPage`、品牌 `BrandsView`、ZCode
`ZcodeSection`）统一换成 Material Symbols 的 `terminal` 图标（`icons.tsx` 的 `TerminalIcon`；
`makeIcon` 新增可选 `viewBox`，因为该图标在新版 Symbols 用 `0 -960 960 960` 视口）。
不可用时**变灰禁用而不是隐藏**（新增 `terminal.unavailable` 文案）。

### 1.2 整合包：可选能力与向后兼容

见 [modpacks.md](modpacks.md)。要点：

- **导出 `home/`**：白名单式，不勾选则什么都不随包走；导出弹窗有勾选列表，`GET …/export` 给可选项。
- **`dshVersions`**：导出写实测集；导入做 `∩ 本机已装` 的确定性选择；市场条目也读。
- **`launchers`**：读 + 归一化 `hdsl` + 逐段比较 + **警告放行**；导出侧刻意不写。
- **向后兼容**：`.tgz` 与 manifest v2/v3 可导入；v1 拒绝并说明压平了什么。

### 1.3 dsh 的终端：从 `dsh tui` 到「装一个 TUI 插件」（本版最大的认知修正）

**上游事实**：`dsh <name>` = `dsh --profile <name>`，所以 `dsh tui` 是内置入口；但 `tui` **不在**
自动初始化的 profile 模板里（模板只有 acp/web/headless/sdk/sdk-minimal），非模板名要先
`dsh plugin --profile tui add <pkg>` 建出来。所以"没装插件就打开终端"会以
`profile "tui" does not exist` 在 0.4 秒内退出——**这是审核实测的 dsh 实例崩溃原因**。

**硬约束**：TUI 插件**绝不能装进实例的 `web` profile**。TUI bundle 与 `dsh-web-app` 都是 app 插件，
各按自己的语法解析同一份 argv，上游不仲裁；混装会让应用遇未知旗标直接退出（社区 README 实测
`error: unknown option '--no-open'`）。安装器因此新增 profile 形参与 `refuseAppProfile` 守卫。

**清单**（`dsh/DshTuiPlugins`，每项带它自己的 profile）：

| 包 | 版本 | 许可证 | profile | bin |
|---|---|---|---|---|
| `@deepseek-harness-tui/dsh-tui` | 0.17.0 | MIT | `dsh-tui` | 有（5670★，头部） |
| `@huiliyi37/dsh-tianshu-tui` | 1.0.0-rc.2 | **Apache-2.0** | `tui` | 无 |
| `@tomowang/dsh-tui` | 0.10.2 | MIT | `tui` | 无 |
| `@xmoon76/dsh-pi-tui` | 0.5.3 | MIT | `pi-tui` | 无 |

**剔除**：`@ruhooai/dsh-tui`（bin 需要 Bun + 外部 `cordis.yml`，面板起不来）、`@zhangweiii/dsh-tui`。

**REST**：`GET /api/dsh/tui-plugins`（清单，`DshApiServlet`）、
`GET|POST /api/instances/{id}/tui-plugins[/install|/remove]`（`{package}`；**profile 由服务端按清单
决定，请求无法指定**；安装/卸载复用同一套任务体）。`DshTui` 改为启动插件自己的 profile；没装插件时
给可行动的错误。

**前端**：实例「安装」页新增 TUI 插件区段 + **醒目免责声明**（面板只做 npm 安装 / 版本锁定 / 挂到
插件自己的 profile；不改 dsh 源码；第三方作品不背书；展示实际包名/版本/仓库/许可证 + 一键卸载）。

### 1.4 终端 WS 不再被 30 秒静默掐断

`docs/web-shell.md §3` 一直写着「无空闲超时」，但 `TerminalWebSocket.onOpen` 从未设置过，于是吃
Jetty 默认 30 秒 → 静默终端被 `1001 Connection Idle Timeout` 关掉。现在 `ws.setMaxIdleTimeout(0)`。
客户端加心跳（20s ping / 10s pong 期限）；仓库 pin 的 Jetty 12.0.39 会回 pong（已反编译确认）。

### 1.5 客户端（hdsl-tui）对齐

保留服务端给的原因（`terminal_failed` 不再被通用文案盖掉）；提权顺序「状态 → 密码 → 内核 → 终端」
（新调 `GET /api/developer/status`）；选项可放地址前后；补上 `j/k` 菜单导航；`terminal_failed`
退出码统一为 4。

### 1.6 实例生命周期 ↔ 终端：外部审查的两处高危与同批修复

外部只读审查（按 `review-prompt.md` 那套提示词，见工作目录）指出这条线上有两处"文档承诺了、实现只做
了一半"。**第 0 步先核对，两条都成立**，不是设计取舍：

- **删除 dsh 实例不结束它的终端**（`InstancesApiServlet.deleteInstance`）：`InstanceRuntime.stateOf`
  只看 web 进程、不认 PTY 会话，于是终端开着时实例仍读作 `STOPPED`、删除判定放行，目录被删而 agent
  活着。更糟的是**杀入口一起没了**：`DELETE …/terminal` 先 `find(id)`，删后必然 404，此后没有任何
  路由能碰到那个进程。
- **web ↔ 终端互斥只做了半个方向**：开终端会停 web（原有），但**启动 web 不停终端** → 两个进程写同一份
  `DSH_HOME`；而终端在跑时按「停止」反而得 `409 instance not running`（判定只看 web 进程）。

修法（`InstancesApiServlet.java:417-425` 删除 / `:641` 启动 / `:823-843` 停止）：三条路径**一律先**
`instanceTerminal.close("dsh", id)`，顺序与品牌（`BrandApiServlet:476/511`）、ZCode
（`ZcodeApiServlet:354/375/387`）逐字对齐。**刻意没有改 `stateOf`** 把终端算成"运行中"——终端是实例的
资源，正确做法是"操作前无条件收尾"，而不是给状态机加一个跨进程判据（那会牵动 UI 徽标与所有既有判定）。
停止在"只有终端在跑"时回 `200 {"state":"stopped"}` 并 `announceState`（前端会先乐观置 `STOPPING`，
靠这次广播纠正）；什么都没跑仍然 409，口径没有放宽。

同批清掉审查列出的中低危：

| 项 | 处理 | 用例 |
|---|---|---|
| `PosixPty` 主描述符竞态（关与用无共享锁 + 内核复用 fd 号 → 可能读/写**别的实例**的终端） | 每次调用各持一份 `dup` 租约（只锁账本，系统调用不进锁），关闭只回收主号 | `PosixPtyMasterFdTest`（假内核，改前 2 红） |
| 退出排空丢尾；`Buffer.put` 在 `finished` 后丢弃已读出的块 | `put` 一律收下；排空改成"排到 EOF，`DRAIN_MILLIS` 兜底" | 同上 |
| 反代：客户端断开的 cancel 到不了真正在飞的请求；首包失败无条件重发（含 POST/PUT） | `Upstream` 持有真正在飞的 future，`retrying()` 启动即登记并在已取消时立刻取消；重试只限 `GET`/`HEAD` | `ProxyUpstreamFailureTest`（改前 3 红） |
| `Set-Cookie` 路径改写未转义 mount | `Matcher.quoteReplacement(mount)` | `ProxyBehaviorTest` 新增 `$` 用例 |
| `holder[0]` 残留 ×6（`NodeApiServlet`、`InstancesApiServlet`×4、`BrandApiServlet`） | 全改 `TaskService.TaskWork`（任务体直接拿到自己的 Task）；删掉已成死代码的 `report(holder,…)`，`PluginProgressHolderTest` 换成 `TaskProgressTest` | `TaskProgressTest` |

**改前必红**都留下了证据：撤掉 `InstancesApiServlet` 的改动后恰好 delete/stop/launch 三条红、守卫那条
绿；PTY 与反代两条各有自己的红。注意 PTY 那两条发现**互相掩盖**（丢尾会把串台的字节一起丢掉），
所以竞态断言钉在假内核实录的"调用落在哪台终端"上，而不是"看见了 B 的字节"。

**客户端无需配合**（回答"Go 客户端要不要适配"）：`hdsl-tui` 只走 `login` / `elevate` /
`POST …/terminal` / `DELETE …/terminal`，**不碰停止与删除实例**；终端协议号与票据逻辑本轮未动。

### 1.7 第二轮审查：dsh 插件"装完即验"，以及同批加固

第二份审查（同一套提示词，在**活的**面板上做的）把"打开终端 0.4 秒死掉、零输出"的根因指到插件本身，
而且现场有证据：同一个实例四次 `terminal.open=success`、四次同一秒 `terminal.exit outcome=1`、
回放 0 字节；它的 `profiles/tui` 与 `profiles/dsh-tui` 里 `.pnpm` 条目为 0、`package.json` 的
`dependencies` 为空——**装到一半的骨架**。对照组（deps 有、`.pnpm` 有）同一面板上正常出画。

注意审查那版说法要打个折：它说"deps 为空却照样开终端"——在本版代码里 deps 为空会在预检就
409（`InstancesApiServlet` 的 `terminalPlugin` 预检），所以它现场跑的多半是更早的构建。
**仍然成立的是更窄的那一版**：manifest **声明了**包、`node_modules` 里什么都没有 → 预检放行 →
启动空 profile → 秒死零输出。这**违反 CHANGELOG 自己的承诺**（"没装插件时给出可行动的错误"
只覆盖了一半），而 `DshTui` 里"启动 profile 这条路径永远可用"正是被现场证伪的那句断言。

本轮因此改了四件事（都在 §1.6 那两处高危之后）：

- **装上就验**（`DshPluginInstaller.requireLanded`）：`dsh plugin add` 退出 0 之后还看
  `node_modules/<包>` 在不在，不在就把这个安装任务判失败。只验 registry 型 spec——
  `file:`/`link:`/`github:`/URL 的包名读不出来，误判会打断本地安装。
  **必红证据**：临时撤掉那次调用，`PluginApiTest.anInstallThatLeavesNoFilesBehindFailsTheTask`
  立刻红，任务停在 `"state":"done"` 而盘上什么都没有。
- **半装状态不算已安装**（`DshTui.terminalPlugin`）：同时要求 manifest 声明**与**文件真的在
  （有 bin 的还要求入口存在）；声明了但文件不在 → 用"重新安装这个"拒绝，而不是去启动空 profile；
  清单里靠前的坏了不挡后面真的装好的那条（`aDeclaredButMissingEntryDoesNotShadowAWorkingOne`）。
- **拒绝进审计**：`terminal.open` 的这条拒绝原先不落审计，"被拒"与"开了又秒死"在日志里长得一样。
- **真死了要留话**：非零退出且零输出时写一条应用日志，退出帧带 `failedWithoutOutput`，
  前端在"会话已结束 (N)"下面补一句可行动提示（`terminal.failedWithoutOutput`）。
  **面板自己结束的不算**（停止/删除实例连带收尾终端就是 143=SIGTERM，诊断它只剩噪音）——
  为此 `TerminalSession` 新增 `endedByPanel`，谓词从 `producedNoOutput()` 改名成 `silentFailure()`。

同批的其余加固（都在审查清单里，按"值不值得"取舍后的结果）：

| 项 | 处理 |
|---|---|
| 越界写只做字面比较，目的地里**已存在**的软链目录可被穿过写出去 | 新增 `util/io/SafePaths`：按已存在的最深祖先取真实路径比较 + 逐段建目录不穿过离开根的链接；`DshPackInstaller` / `DshPluginBundle` / `NodeRuntimeManager` 三处写路径共用。指回根内部的链接照旧放行（`$DSH_HOME/skills` 挂另一块盘是合法布置） |
| 导出凭据名单比导入窄（无扩展名的 `id_ed25519` 能随包出去） | 导出集合改成由共享常量构成（导出 ⊇ 导入）；两侧独有规则一条没删；新增测试把两边钉在一起 |
| 上行 `Accept-Encoding` 原样透传，但重写器只懂 gzip/deflate | 请求侧收敛成 `gzip, deflate`（不认的编码就不发），兜底分支记警告 |
| `masterFd` spawn 后才设 CLOEXEC、`slaveFd` 从不设 | 两个 fd 在 `openpty` 返回后立刻带 CLOEXEC；子进程文件动作里也 `addclose(slaveFd)`；租约改 `fcntl(F_DUPFD_CLOEXEC)`。**现场后果**：子进程自己留着 slave 副本会让孙进程握住 pty，master 永远等不到 EOF，"排空到 EOF"退化成等 2 秒超时 |
| `tail` 跨线程裸读（泵线程写、任务线程读） | `ConcurrentLinkedDeque` |
| `onExit` 可能跑两遍 | 状态写入与 listener 快照放进同一把锁（压测能复现 21 次重复；现场没撞上） |
| 应用日志与 `.xz` 归档跟 umask（容器 0644） | 创建时收紧 0600，照 `AuditLog`/`Main` 的既有写法；非 POSIX 文件系统照常工作 |
| `docs/web-shell.md` 那句"SIGWINCH 由内核发送" | 与实现不符（是面板自己发），已改；互斥、描述符规矩也一并跟上 |

**明确决定不改的**（审查提了、但按取舍留着的）：

- **面板 session cookie 透传给实例**：真，但严重度被高估——终端/开发者路由要提权，所以 token
  不等于终端 RCE；而且**只删 Cookie 头关不住**（实例页面就在面板同一 origin，它的 JS 自己会带
  Cookie 调 `/api/*`）。这是"同 origin 挂载"的固有代价，用户决定**不管**，作为已知代价留着。
- **`resize` 只给根 pid 发 SIGWINCH、`terminate` 的 pid 复用窗口**：真但窄，用户判定"没必要"。
- **`freshClient()` 每次重试新建且不 close**：不能提前 close（会掐断正在读的 body），收益小于复杂度。
- **`HDSL_ADMIN_PASSWORD` 常驻进程环境**：命中项目自陈的威胁模型（§4.1"不是沙箱：agent 以面板
  进程的用户身份运行"）；另外 JVM **改不了自己的 environ**，"读完 unset"做不到。

### 1.8 TUI 插件要配对宿主版本（v0.5.83 重切时补的）

现场症状：天枢装进 0.1.x 实例，终端起得来，会话却报 `⚠ agent-presets 未装配：本会话没有工具面`
——**窗口能用，agent 一个工具都没有**。

根因：终端界面的**工具面就是 agent-preset 层**，而这一层是 dsh **0.2.0** 才有的架构
（`dsh-agent-preset-registry` + 每条 `preset` 一行）。三套社区 TUI 全写给 0.2.x，各自把这层写进
自己的 bundle patch；面板原来把它们当通用货装给任何实例。

改动（都在 v0.5.83 里）：

- `DshTuiPlugins.Entry` 新增 `requiresDsh`（天枢 `>=0.2.0-rc.1`、tomowang `>=0.2.1-rc.3`、
  pi-tui `>=0.2.1-rc.2`、`@deepseek-harness-tui/dsh-tui` 为 `null` 因为 peers 跨两条线），
  `fitsHost` 复用 `DshPluginRequirements.satisfies`；
- 安装路由**提交任务前**就 409、开终端时 `terminalPlugin` 也拒（已装的旧组合）；
- `GET …/tui-plugins` 多给 `fitsHost`/`requiresDsh`，前端**变灰不可装**并写明原因（不隐藏）；
- `GET …/tui-plugins` 另给 `filesPresent`（manifest 声明 vs 磁盘真相），`installed && !filesPresent`
  的行标「安装未完成」并给「重新安装」+「卸载」（第二轮审查后补，见 CHANGELOG v0.5.91）；
- 未知实例版本不拒；两个路由共用 `hostMismatch` 一句文案。

**还没解决的**：`0.2.x` 的 dsh 在 npm 上只有 `next`/`alpha` 通道（`latest` 仍是 0.1.7 线），
而面板的版本列表把 `predatesLockstep` 的老版本也标成可装——所以实例仍可能落在 0.1.x，只能用
`@deepseek-harness-tui/dsh-tui`。要不要把"只有 `next` 才有 0.2.x"这件事在 UI 上说清楚，留给下一轮。

## 2. 关键文件

```
src/main/java/org/jackhuang/hmcl/dsh/DshPackForge.java       导出（home/ 白名单、dshVersions）
src/main/java/org/jackhuang/hmcl/dsh/DshPackInstaller.java   导入（launchers、dshVersions、.tgz/v2/v3）
src/main/java/org/jackhuang/hmcl/dsh/DshPackMarket.java      索引（dshVersions / launcherRestricted）
src/main/java/org/jackhuang/hmcl/dsh/DshTuiPlugins.java      精选 TUI 插件清单（含 profile）
src/main/java/org/jackhuang/hmcl/dsh/DshPluginInstaller.java 安装器（profile 形参 + 守卫 + **装完即验**）
src/main/java/org/jackhuang/hmcl/util/io/SafePaths.java      越界写的软链安全（三处写路径共用）
src/main/java/org/jackhuang/hmcl/web/terminal/DshTui.java    起插件自己的 profile；**声明 + 文件都在**才认
src/main/java/org/jackhuang/hmcl/web/terminal/TerminalWebSocket.java  终端 WS（setMaxIdleTimeout(0)；退出帧带 failedWithoutOutput）
src/main/java/org/jackhuang/hmcl/web/terminal/TerminalSession.java  会话（silentFailure/endedByPanel、onExit 恰好一次）
src/main/java/org/jackhuang/hmcl/web/http/DshApiServlet.java  /api/dsh/tui-plugins
src/main/java/org/jackhuang/hmcl/web/http/InstancesApiServlet.java  /tui-plugins 与 /export；**删/停/启动先收尾终端**
src/main/java/org/jackhuang/hmcl/web/terminal/pty/PosixPty.java      PTY（master 描述符改 dup 租约、排空到 EOF）
src/main/java/org/jackhuang/hmcl/web/proxy/InstanceProxyServlet.java 反代（Upstream 持真正在飞的 future、重试限 GET/HEAD）
src/main/java/org/jackhuang/hmcl/web/server/HdslServer.java          `terminals()` 访问口（供生命周期用例观察）
web/src/components/icons.tsx                                 TerminalIcon
web/src/components/TuiPluginSection.tsx                      TUI 插件区段 + 免责声明
web/src/components/ExportInstanceModal.tsx                   导出弹窗（home 白名单 / dshVersions）
client/hdsl-tui/internal/pterm/client.go                     客户端心跳
client/hdsl-tui/internal/app/terminal.go                     提权顺序
docs/modpacks.md                                             整合包契约（本版新增）
```

## 3. 验证到哪一步

- 已做：§0 的全量测试与构建；整合包新增用例（真 tar.gz、v1 拒绝理由、`dshVersions` 选择、
  `launchers` 五行判定）；TUI 插件用例（清单自洽、web 守卫、端到端把 `@tomowang/dsh-tui` 写进
  `profiles/tui/package.json` 而实例 web profile 仍为空）；终端 WS 与客户端心跳的单测；
  §1.6 的三组新用例（生命周期、PTY 描述符、反代）与 §1.7 的四组（插件装完即验、越界写、
  编码协商、退出诊断）——**都有改前必红的记录**。
- 未做：**真机复验**。审核（v0.5.72）报的两处核心故障——品牌 agent 零输出、30 秒断开——后者早已确证
  修复，前者**已被第二轮现场审查改写**（不是 PTY，是插件没装好；见 §1.7 与下面 §4 第一条）。
  本轮 PTY 的 fd 竞态同样是**真机时序**问题，单测用假内核替代（真实内核没法在那一刻按需复用号）。
- 未做：`shadowJar` 按版本号出包（本地只跑过 `0.1.0-dev`）；Docker 镜像（本机**没有 docker/buildx**，
  只能靠 CI 回答）；Windows 桌面终端路径。

## 4. 已知缺口与下一步

- **"品牌 agent 零输出"——结论已改**：第二轮现场审查用同一条 PTY 否证了假设①——kimi 的 TUI 在
  **没有 `TIOCSCTTY`** 的 PTY 下画了 12 分 45 秒、回放满 256 KB、又活了 21 分钟（"没有控制终端就没有
  输出"不成立，代码里的注释本来也只解释"为什么要手动发 SIGWINCH"，没说输出依赖控制终端）。
  现场把根因指向**插件没装好**（`pnpm` 先写依赖再取包，取不到就留半装骨架），本轮已按 §1.7 修：
  装完即验 + 半装状态拒绝 + 死了留话。**下次真机复验要看的**是：装一个 TUI 插件，确认
  `node_modules/<包>` 真的在、终端出画；以及 `DshPluginInstaller` 的安装后校验在真 pnpm 上不误报
  （本轮只在 stub 与单测上验过）。
- **反代的"客户端断开"在真机上可能到不了 `AsyncListener`（本轮实测，存疑）**：Jetty 12.0.39 不会为
  一个**静默**断开的异步请求报告死亡——裸 Jetty + 异步 servlet 探测：客户端 FIN 之后 25 秒内
  `AsyncListener` 什么都没收到（连接器 idle 超时设成 1 秒也一样），`onError` 只在 `server.stop()` 时
  才来；RST 关闭、请求后发垃圾字节同样如此。所以真机上真正可达的收尾是 pump 的写失败 +
  try-with-resources 关闭上游 body；**一个已经在重试、客户端又静默消失的请求可能只是自己拖到结束**。
  这不在本次三条发现范围内，没有改——**下一步值得查清：生产上 `onError`/`onTimeout` 到底可不可达**。
- 澄清一条审查措辞：审查说"SSE 会滞留到自然结束"，实测机制不是那样（SSE 的响应头已在手，
  `sendAsync` 已完成，由 pump 的 catch / try-with-resources 关上游 body）；但**上游 socket 是否真的
  被关闭没有端到端验证**，按"未经证实"记，不算推翻。
- **同实例重复打开终端**卡在「正在打开…」（面板有 `TerminalBusyException`，但表现是悬着）。
- **`dshhome` 形态**仍被 web API 拒绝；`files[]` / `.dshpkcfg` 未实现（见 modpacks.md §2）。
- **客户端**：多服务器会话单槽位（后登录的覆盖前者）；plain 模式无法脚本化；协议号不匹配只在
  `--verbose` 可见；`--resume` 无品牌校验。
- **测试护栏**：`internal/rtty` 整包零测试；`internal/panel/fakeserver` 缺 zcode/kernels 路由，导致
  `TerminalKernels` 路径零覆盖。
- **上游同步**：本次扫描结论是**没有需要移植的改动**（详见 [upstream-sync.md](upstream-sync.md) 的
  v0.5.81 段）。

## 5. 发布动作：v0.5.83（本次执行）

口径已定：**v0.5.83**，不撤 v0.5.82。执行顺序是：提交在 `web-shell` → `--no-ff` 合并进 `main` →
推 `main` → `shadowJar` 出包 → 打 tag → `gh release create`。

```bash
./gradlew shadowJar -PreleaseVersion=0.5.83         # → build/libs/hdsl-web-0.5.83.jar
git push <remote> main web-shell
git tag -a v0.5.83 -m "<一句话>"
git push <remote> v0.5.83
gh release create v0.5.83 build/libs/hdsl-web-0.5.83.jar SHA256SUMS.txt --notes-file <notes>
```

出 Release 说明的固定做法：`sed -n '/^## v<版本>/,/^## v<前一版>/p' CHANGELOG.md | sed '$d'` 存成
文件再 `--notes-file`。客户端 5 个平台资产与 `SHA256SUMS-client.txt` 由 tag 触发的 `client.yml` 上传；
docker `0.5.83` 会拿到 `latest`（`sort -V` 下它最大）。**推完必须核 `docker.yml` 这一条**——v0.5.83
第一次把 `vim-tiny`/`nano`/`unzip`/`zip`/`bzip2`/`7zip` 装进运行镜像，arm64 腿在 QEMU 下装包，是这次
唯一没在本机验证过的环节（本机没有 docker/buildx）。

推送注意：`github.com` 的 git 传输会间歇被掐断（`GnuTLS recv error (-110)`），所以**用重试循环推、
再用 `gh api` 核远端真实 sha**（不要用 `ls-remote` 判成败：ref 已存在会假阳性）。

## 6. 环境备注（下次接手别踩）

- **`git fetch github.com` 在这台机器上会被掐断**（`GnuTLS recv error (-110)`）。读上游提交用
  `gh api repos/MCXCC303/HDSL/commits`；源码 zip 走 `codeload.github.com` 也通。
- **推送 github.com 一律强制 HTTP/1.1**（v0.5.83 那次踩到的）：`git push origin main` 卡在
  "Failed to connect to github.com port 443"，同一时刻 `gh api`（api.github.com）0.3 秒就回。
  实测 `github.com` 的**连接能建立、传满 32 KB 就停**（典型的 HTTP/2 窗口/MTU 症状）；换成
  `git -c http.version=HTTP/1.1 push` **第一次就过**。以后推送都带上它，再加
  `-c http.lowSpeedLimit=1000 -c http.lowSpeedTime=45`，卡住就快速失败，别干等 134 秒。
  另外本机 `~/.ssh/id_ed25519` **没有**注册在账号上（`ssh -T git@github.com` 返回
  Permission denied (publickey)），所以 SSH 这条退路目前不通——只有 HTTPS + gh 凭据一条路。
- **`SHA256SUMS.txt`**：`cd build/libs && sha256sum hdsl-web-<版本>.jar > …`（只含文件名，86 字节），
  放在仓库根但**不提交**，只作为 Release 资产上传。
- 子代理在同一个工作树里并发干活时，**分支会被漂到 `main`**（本次两回），收尾时务必确认
  `git branch --show-current`。
- 本仓库 `origin` 是 HTTPS + `gh` 凭据（`git -c credential.helper='!gh auth git-credential' push …`）。
- `npm run compat` 在 Windows 会报 "Did not find any files"（es-check 不做 glob 展开），CI 的 Linux 上正常。