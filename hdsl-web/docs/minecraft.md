# Minecraft 服务端管理（MC 服务器）

> **里程碑 v1（2026-11-08）**：MC 服务端功能第一版完成。原版与第三方核心
> （Paper / Folia / Purpur / Fabric / Forge / NeoForge）、面板托管 JDK（三家发行版）、
> 插件/模组管理与 Modrinth 搜索、第二道关与免责流程、实例高级设置都在这一版里。
> 此后 MC 相关改动进入维护与增量阶段，下文是这一版的实现契约。
>
> 状态：**默认关闭**（2026-10-30）。Windows 的管道控制台已有完整代码路径，
> 并新增 `McRuntimePipeTest` 在 Linux 上覆盖启停/命令/`stopAll`/端口占用；**真机 Windows
> 仍待验收**（ConPTY 切换是独立的一步）。本功能不主动宣传。本文是实现契约，不是使用说明。
>
> 2026-11 增量（§19）：**第三方核心**（Paper/Folia/Purpur/Fabric）走**第二道关**——
> 只有主功能开启后才能开启，独立条款与同意记录，关闭主功能前必须先关它；
> 同时新增**面板自托管 JDK**（Adoptium）、**插件/模组管理**（Modrinth 安装、`.jar.disabled`
> 启停）与 **Forge/NeoForge 安装器**（跑上游 installer，解析 `unix_args.txt` 启动）。
> mrpack 整合包仍在后续。
>
> 一句话：把 Minecraft 服务端当作**第三种 surface**接入面板，复用现有的实例模型、
> 进程/终端、端口分配、任务与审计；游戏端口独立暴露，**默认关闭**，开启需提权与免责确认；
> 关闭走「停止功能 → 确认关闭」两步，中间那一小时只活在内存里（重启即消失）。
>
> 落地前先读 §3（设计理念）与 §5（状态机）；§6 是"能复用多少"的关键结论，
> §12 是"哪里会翻车"，§19 是第三方核心的契约。

## 1. 是什么

在面板里管一台原版 Minecraft 服务端，和管一个 agent 实例是同一套姿势：

- **创建**：选版本（Mojang 清单，可换镜像）、端口（指定或自动）、内存、种子/世界类型
- **生命周期**：安装即下载 `server.jar`；启动、优雅停止、强杀、崩溃重启（带退避）
- **控制台**：复用现有 Web Shell（PTY + xterm），`say`/`stop`/`op` 都走这里
- **配置**：`server.properties` 受管字段表单；白名单/OP/封禁
- **世界**：种子开新图、导入已有地图包、导出、备份/恢复
- **开关**：默认关；不开启时面板与日常完全一致；关闭要走"停止功能 → 确认"两步，
  且那一小时只活在内存里（重启即消失）

AI agent 后续可经 **scoped token** 调用同一套原语（破坏性操作走审批），但**功能开关本身只给人类会话**。

## 2. 不是什么

- **不是反代**。MC 客户端说的是二进制 TCP（25565），现有 `/i/*` 是 HTTP/WebSocket，
  转不了。游戏端口必须单独暴露（例外：Dynmap/BlueMap 这类网页地图是 HTTP，可正常反代）。
- **不是模组/插件市场**。第三方核心（Paper/Folia/Purpur/Fabric/Forge/NeoForge）在
  **第二道关**之后可用，插件/模组可管理并从 Modrinth 安装（§19）；整包（mrpack）
  仍不在第一版范围内，面板也不做市场本身——它只调用上游公开接口。
- **不是沙箱**。默认与面板同 uid（§11.3 写明边界）；`runAsUser` 是可选增强。
- **不是编译期开关**。运行时开关，代码始终在包里，前端页签运行时隐藏（§3.8）。
- **不是"面板替玩家开服"**。面板管进程与数据，游戏协议与行为归 Mojang/上游。

## 3. 设计理念（为什么是现在这个样子）

这一节记录的不是功能，而是**当初为什么这么想**。改动设计前先读它。

### 3.1 谱系：MC 不是新品类，是同一类工作负载

HMCL（桌面启动器）→ HDSL（把 dsh 搬进浏览器）→ HDSL-web（多 agent 实例面板）。

走到今天，面板已经是一个**多进程宿主**：分配端口、监督进程、隔离 home、托管终端、做备份。
MC 服务端在架构上的属性与一个 agent 实例高度重合——有版本、有端口、有工作目录、有进程树、
有日志、有需要备份的数据。差别只在两处：它**不说 HTTP**，且它**有状态（世界）**。

所以这不是"硬塞一个游戏功能"，而是"同一个宿主模型换一个载荷"。

### 3.2 反代为什么帮不上

现有 `/i/<id>/` 的三层改写是给"自带网页端的工具"用的。MC 服务端没有网页端，
25565 上跑的是二进制协议，客户端也不走 HTTP。把 MC 塞进反代是**协议错配**，不是配置问题。

这条决定了产品形态：游戏端口必须独立暴露，面板只能做到"管理"，做不到"一个端口全包"。
若要收口，标准做法是前面放 Velocity/BungeeCord（一个公网端口多后端）——那是另一个攻击面，
明确不在第一期。

### 3.3 为什么默认关

每多一个公网入站端口，就多一个攻击面；而面板的默认承诺是"只开 3080 一个门"。
默认关意味着：

- 无页签、`/api/mc/*` 回 **404**（不是 403——看不出这个功能存在）
- 无端口监听、无后台任务、无启动开销
- 不启用时，面板与日常**逐字节一致**

开启是一个显式的、需要提权与免责确认的动作，且留下审计。

### 3.4 两层治理：摩擦跟爆炸半径走，不跟频率走

| 层级 | 典型动作 | 特性 | 治理 |
|---|---|---|---|
| 实例级 | 启动/停止/重启/看日志 | 日常、可逆 | 提权即可，**无冷却**；最坏留下审计 |
| 功能级 | 关掉整个 MC、重新开启 | 一年动一次、影响面大 | 提权 + **一小时承诺装置** |

理由是使用频率：天天用的动作加冷却，只会逼人去绕过；一年动一次的动作，加承诺才有意义。
"实例级开关想开就开想关就关，你犯贱撑死日志上有你逆天操作"——这是刻意的。

### 3.5 承诺装置：一小时，只活在这一次进程里

按下功能级「停止 MC 功能」的那一刻起跑一小时，**无条件**：

- 锚点放在**按钮那一刻**，不是功能真正关闭那一刻——防止靠拖延到最后一秒来偷时间
- 这一小时里：不能建服、不能启服、**也不能重新开启功能**（"谁来了都没有用"）
- **取消关闭不解锁**。「我不想关掉功能」只回答"这个功能还要不要"，不缩短等待；
  按了停止功能就是按了，这一小时照走
- 到点解锁。功能还开着 → 自己手动启服（不会自动拉起）；功能已经关了 →
  重新走一遍开启流程（提权 + EULA/条款确认）
- 计时**只存在内存里**：面板进程或容器重启，倒计时直接消失，功能回到 `feature.json`
  里持久化的那个状态

这条的设计意图是**仪式感**：服主要真的关服，就该有一段"不能立刻反悔"的时间。
但仪式不能变成事故——所以它只活在内存里，重启即清（见下）。

为什么不像 E-Stop 那样落盘？因为这一小时的定位是**决策摩擦**，不是安全边界（§3.6）。
能重启面板的人本来就能改状态文件、能杀进程，把他挡在"一小时后"没有意义；而落盘会带来
一个更坏的副作用：**用户自己的服被打、要立刻处置时，被一个持久化的倒计时锁住**——
那是产品事故，不是纪律。摩擦留给手滑，恢复能力留给事故。

代价要说清：**重启就是唯一的强制恢复手段，而它会同时清掉所有摩擦**。这是刻意的交换，
不是漏洞；也正因为如此，这一小时永远不能拿来当安全承诺写进文案。

### 3.6 仪式的边界：UI 不能替代结构

免责、倒计时、二次确认，对"同 uid 的本地攻击者"零作用——他能直接改状态文件、
改 `server.yaml`、甚至重启面板。真正的边界是：

- 独立 uid + 数据根分离（可选，需宿主权限）
- 游戏端口只绑该绑的接口，RCON 只回环
- 服务端/插件更新、`online-mode`、白名单
- 备份

仪式的作用是**让决定显式、可追溯**。判据很简单：

> 运维很急、很想骂人的时候，这个控制还成立吗？

uid/端口绑定成立（它们不挡操作，只挡越权）；一小时冷却不成立（它挡的正是"我想马上验证"，
而且重启就没了，§3.5）。所以冷却适合当决策摩擦，不适合当安全机制来论证。

### 3.7 审计的定位：检测与追责，不是预防

"日志上有你逆天操作"就是审计的正确定位。验收标准不是"有没有记"，而是
**能不能重建现场**：

- 谁（会话 / agent 身份）、从哪个 IP、什么时候
- 对哪个实例、做了什么
- 动作前后的状态（running → stopped）
- 结果（成功 / 失败 / 超时 / 强杀）
- 可选：操作理由

再配一个"最近高风险操作"视图，否则真出事时没人会去 grep。

### 3.8 为什么不做编译期开关

Java 没有 `#ifdef`。真要"编不进去"只有两条路：source set + SPI，或拆多模块。
而且前端是单产物——页签最终还得运行时隐藏。它和"一个 fat jar"的交付承诺直接冲突，
换来的只是一个管理员门禁本来就该给的东西。

决定：**运行时开关为默认**；`-Pfeature.mc=false` 作为**发行变体**保留（需要先把
"核心不 import MC 包"的 SPI 边界划出来）。做变体时，还要用 Dockerfile 里已有的
`test -f` 断言风格，断言"无 MC 构建里路由/类确实不在"。

注意一个已知现象：配置加载器对未知键是**打警告后忽略**。无 MC 的 jar 遇到 `mc:` 配置会
安静地少一个功能——警告要有，UI 也要明确显示"此构建不含 MC"，否则运维会以为开关坏了。

## 4. 目标与非目标

### 4.1 MVP 目标

- 原版服务端：版本清单（Mojang，支持镜像）、`server.jar` 下载与 SHA1 校验、JRE 选择
- 创建：版本、端口（指定/自动）、内存、种子、世界类型
- 生命周期：启动、优雅停止、强杀、崩溃重启（退避、启动失败不循环）
- 控制台：复用 Web Shell，命令输入，回放
- 配置：`server.properties` 表单（受管字段，保留未知键与注释）
- 玩家：白名单/OP/封禁；在线列表（RCON）
- 世界：导入 zip（流式）、导出、种子开新图
- 备份：手动、恢复、保留策略
- 功能开关：两个持久态 + 一小时内锁（仅内存）+ 审计
- 免责声明与使用条款（版本化、双勾选）
- 实例级提权；功能级承诺装置（重启即清零，§3.5）

### 4.2 非目标（第一版）

- mrpack 整合包部署（§19.5 写明边界）
- TPS 曲线与性能图表（Paper 可后续）
- 定时任务、多世界管理、跨主机编排
- 编译期裁剪（只保留变体设计，不实现）

> 已移出非目标：Paper / Purpur / Folia / Fabric 与插件/模组管理——它们在**第二道关**
> 之后提供（§19），默认仍然不存在。

## 5. 功能开关状态机

### 5.1 状态：两个持久态，一把只活在内存里的锁

| 状态 | 存在哪 | 面板表现 | 允许的动作 |
|---|---|---|---|
| `DISABLED`（默认） | `feature.json` | 与日常完全一致：无页签、`/api/mc/*` 404、无监听、无后台任务 | 开启（提权 + EULA/条款 + 审计） |
| `ENABLED` | `feature.json` | 出现 MC 分类与状态徽章 | 建服 / 启停 / 「停止 MC 功能」 |
| **锁定中**（按下停止功能后 1 小时） | 仅内存；`state` 保持按下当时的值 | 停止按钮显示"已按下"与倒计时；服务器已全部停止 | **禁止建服、启服、重新开启**；只能等（或做下面那个决定） |
| 锁定 + **已决定保留**（「我不想关掉功能」） | 同上 | 停止按钮隐藏；横幅"功能保留，hh:mm:ss 后可启动服务器" | 只能等 |
| 锁定 + **已决定关闭**（「确认关闭功能」） | `state` 落盘为 `DISABLED`，锁定仍在内存 | 面板回到日常（无页签）；开启页显示"锁定中，剩余 mm:ss" | 只能等 |

> 只有 `state` 与同意记录进 `feature.json`。锁定与"功能去留的决定"是进程内的两个字段，
> 重启即消失（§3.5）。**锁是锁，决定是决定**：决定去留不解锁，解锁也不会改回去留。

### 5.2 流转

```
DISABLED ──enable（提权 + EULA/条款）──► ENABLED
ENABLED ──「停止 MC 功能」（提权 + 确认词）──► 锁定 1h（内存；服务器已停）
    锁定 ──「确认关闭功能」──► state=DISABLED，锁定继续（1h 内不能 enable）
    锁定 ──「我不想关掉功能」──► state=ENABLED，锁定继续（1h 内不能启服）
    锁定 ──1h 到期 / 进程重启──► 解锁：state 是哪个就是哪个
```

- **按下就是按下**：这一小时从按钮那一刻起跑，没有撤销，也没有"取消关闭就解锁"。
- **两个按钮只决定功能去留，不解锁**。确认关闭 → `DISABLED`（页签消失、路由 404），
  锁定继续，期间开启被拒并显示剩余时间；保留功能 → `ENABLED`，锁定继续，
  停止按钮隐藏、横幅倒计时，到点后停止按钮恢复、自己启服。
- **锁定期间禁止启服、建服、重新开启**。否则"停服"只是停一下，承诺就是假的。
- **到期不点任何东西** → 保持 `ENABLED`，服保持停止，**不会自动拉起**
  （"关掉的 mc 服务器你要自己重新开"）。
- **关闭功能必须先停服**：先优雅停所有服务器，全部退出后才算按下；有服未停则暂停，
  允许"强制关闭"（单独确认 + 审计），不静默丢存档。
- **重启不改变 `state`**，但会清掉锁定与那个决定（§3.5）。
- **没有服在跑时**，「停止 MC 功能」照旧在（功能级动作，不依赖实例状态）；
  按下后照旧进入锁定并给两个出口，只是没有"停服"这一步可做。

### 5.3 启动链路与按钮

启动链路（入口在实例管理分类里，不新开顶级导航）：

```
实例品牌管理 → 开启功能 → 了解法律风险与权利义务（EULA + 条款）→ 正式启用 MC
```

按钮各自语义不同：

- **开启功能**：提权 → 展示 MC 官方 EULA 原文（滚到底才能勾）→ 勾选 EULA（对 Mojang）
  → 勾选使用条款（对面板）→ 开启（详见 §11.4）。
- **不启用**：同一屏始终保留「我已知晓风险，暂不启用」的出口；点了回日常，
  功能保持关闭，不写 `eula.txt`、不改状态（可选记一条 `mc.feature.declined` 审计）。
  注意它和下面的「我不想关掉功能」不是一回事：一个是"还没开，决定不开"，
  一个是"已经开了，决定留着"。
- **停止 MC 功能**（功能级核按钮）：提权 + 输入确认词 + 明确写"未来 1 小时内不能启服、
  不能重新开启"。点击 = 停所有服 + 起跑 1 小时 + 按钮变"已按下"，
  **旁边出现「确认关闭功能」与「我不想关掉功能」**。
- **我不想关掉功能**：只回答"功能还留着"，**不解锁、不提前启服**；按下后停止按钮隐藏，
  横幅继续走完倒计时，到点停止按钮恢复、自己启服。
- **实例级启停**：日常操作，不碰时钟。

### 5.4 权限矩阵

开发者提权是**30 分钟滑动窗口**（`DeveloperElevation`），所以"要提权"不等于"每次输密码"：
第一次输一次，之后半小时内点一下即可。

| 动作 | 提权 | 锁定 | 额外 |
|---|---|---|---|
| 实例启动 | 否 | 无 | — |
| 实例优雅停止 | 是 | 无 | — |
| 实例强杀（SIGKILL） | 是 | 无 | 显式确认（这条才可能毁世界） |
| 实例删除 / 恢复备份 / 降级 | 是 | 无 | 输入确认词 + 强制先备份 |
| 逐个停掉全部实例 | 是 | 无 | 见 §5.8 |
| 停止 MC 功能（功能级） | 是 | 1 小时（仅内存，不可缩短） | 停所有服 + 起跑倒计时；按钮变"已按下" |
| 确认关闭功能 | 是 | 锁定继续 | 状态变回日常；1 小时内仍不能 enable |
| 我不想关掉功能 | 是 | 锁定继续 | 只表明保留功能；1 小时内仍不能启服 |

> 表中"优雅停止需提权、启动不需要"按讨论确定；若产品上想调整，只改这一处。

### 5.5 闸门顺序与 404

`/api/mc/*` 的过滤链顺序：

```
功能闸门 → AuthFilter → DeveloperGuardFilter → 路由
```

- `DISABLED`：功能闸门直接 **404**（"看不出存在"）。注意这会让未认证请求也得到 404 而不是 401，
  这是"功能不存在"的代价，接受。
- `ENABLED`：未认证仍 **401**（保持现有语义：认证先于守卫拒绝）。
- **锁定期间**（无论去留是否已定）：放行读、「确认关闭功能」「我不想关掉功能」；
  启服/建服一律 423，其余写操作 409。
- 锁定 + 已决定关闭：功能已 `DISABLED`，业务路由一律 404；**只放行 `GET /api/mc/feature` 与
  `POST /api/mc/feature/enable`**，前者用来显示剩余时间、后者回 409 + `remainingSeconds`。
  否则用户只会看到"功能不见了"，不知道是自己按的、还要等多久。
- 所有被拒绝的开启尝试都要记审计——"谁想在这一小时里开"本身是信号。

### 5.6 持久化与时钟

- **持久化**：`state` 与同意记录写 `<data>/minecraft/feature.json`。
- **不持久化**：锁定起止时刻与"功能去留的决定"只在 `McFeatureState` 的内存里，重启清零。
  这也是为什么不再需要"时钟回拨"防御——回拨最多影响这一次运行的显示，重启即重置。
- **倒计时用单调时钟**（`System.nanoTime()`）计算剩余，系统时间/NTP 调整不会让它跳；
  对外只暴露剩余秒数。
- **重启后**：没有锁、没有决定；`state` 是哪个就是哪个；服务器不会自动拉起
  （它随进程一起死了，要不要重开是人的事）。
- 时钟注入保留：测试要能推进时间，否则这一小时没法验。

### 5.7 与 E-Stop 的关系

`EmergencyStop` 已经是同一套 1 小时冷却 + 时钟注入 + "第二次按下不重置"的成熟实现
（`web/emergency/EmergencyStop.java`）。但它**不能直接复用同一个对象**：

- E-Stop 是全局急停（杀终端、杀进程），MC 关闭只作用于 MC
- 语义也不同：E-Stop 到期自动释放；MC 的锁到期只解锁，功能的去留另有一步（可能已经决定）
- **持久性相反**：E-Stop 的截止时间落盘（防重启绕过）；MC 的只活在内存（§3.5）

做法：**照它的形状写一个独立 `McFeatureState`**（持久 `state` + 同意记录；
内存 `lockedSince` / `lockedUntil` / `keepFeature`），共享"时钟注入 + 幂等"的设计。

E-Stop 命中时，MC 服务器也应当被停（因为它已经在 `terminateAll()` 的覆盖范围里）。

### 5.8 已知的绕过口子：逐个停

实例级没有冷却、功能级有。那么"循环调用停服 N 次"就等于拿到了功能级效果，却没有功能级的提权和承诺。

处理二选一，**必须明确写下来**：

1. **接受**（推荐）：反正有审计；实例级操作本来就是日常权限。
2. **阈值升级**：短窗口内停止数量超过阈值（或"停全部"）时，升级为功能级动作。

第一版建议接受，但在 UI 上把"停全部"做成一个显式动作，而不是让用户手动点 N 次。

## 6. 架构落点

### 6.1 第三种 surface

`dsh`、`brands`（Kimi/OpenCode/Claude Web/Octop）、`zcode` 之后，加 `minecraft`。
`TerminalManager` 的 key 已经是 `brand + "\0" + instanceId`，天然容纳。

### 6.2 核心结论：MC 服务端 = 一个终端会话

`TerminalTarget` 是 `(brand, instanceId, command, cwd, environment)`；
`TerminalSession` 提供 `input()`、`onExit()`、`attach(TerminalSink)`、`terminate()`、256KB 回放。

于是：

| 需求 | 用的现成能力 |
|---|---|
| 启动服务端 | `terminals.open(new TerminalTarget("minecraft", id, argv, dir, env), size)` |
| 控制台 | 现有 `/ws/terminal/{ticket}`；`TerminalTickets` 本来就收 `(instanceId, brand, session)` |
| 优雅停机 | 往 `session.input()` 写 `stop\n`，等 `onExit` |
| 强杀 | `session.terminate(grace)` |
| 就绪判定 / 崩溃检测 | `attach` 一个非 WebSocket 的 `TerminalSink`，边收边解析 `Done (` |
| E-Stop 全停 | 现有 `terminateAll()` 自动覆盖 |
| 重连/回放 | 现有 detach/attach + 回放缓冲 |

#### 6.2.1 唯一要改的分工：谁来 spawn

现有 `InstanceTerminal.open()` 的语义是"没有就起，有就复用"（`find` 到 RUNNING 就只发新 ticket）。
对 MC 这不够：**打开控制台不能把服务器启动起来**。所以拆成两半：

| 动作 | 谁做 | 行为 |
|---|---|---|
| `POST /start` | `McRuntime` | `terminals.open(target)`，无 viewer |
| `POST .../console/ticket` | `McConsole` | 只 `find`；没有 RUNNING 就 409；有则发 ticket |
| `POST /stop` | `McRuntime` | 写 `stop\n`，等 `onExit`（超时再 terminate） |
| `POST /kill` | `McRuntime` | `terminate(grace)` |

证据（现有代码，2026-10-29 核对）：

- `InstanceTerminal.open()` 第 171–179 行：`terminals.find(...)` 命中 RUNNING 时**不 spawn**，
  只发一张新 ticket；注释写明 "The process outlives the tab."
- `TerminalWebSocket.onOpen` 第 126–137 行：按 `(brand, instanceId)` 从 manager `find` 并
  `attach`，WS 不拥有进程。
- `onClose` / `onError` → `detach()`；登出 → `closeForSession()`：都只 detach，不杀进程。
- `TerminalSession.input()`：原始字节写进 PTY master，所以 MC 的命令行输入直接可用。
- `broadcast` 遍历 sinks：同一个会话可以挂多个观察者（两个浏览器看同一个控制台）。
- `TerminalTickets`：30 秒、一次性、绑定会话，直接沿用。

所以"终端功能能接入吗"的答案是：**能，而且几乎零改造**；唯一新增的是一个
`attachOnly` 的控制台入口（只 attach、绝不 spawn）。

还有一处要定：`InstanceTerminal.authorize()` 现在同时检查 `webShell.enabled`、HTTPS、
提权、E-Stop。MC 控制台的闸门建议改为 **MC 功能 ENABLED + HTTPS + 提权 + E-Stop**；
`webShell.enabled` 是否继续作为总开关，由产品决定（建议保留，多一道总闸不亏）。

这一条决定了控制台、重连、回放、ticket、进程回收**几乎零成本**。
唯一要新增的是：一个领域层（版本/配置/世界/备份/RCON）和一个功能闸门。

> 待验证（§13 spike）：Paper 的 JLine 在 PTY 下的终端探测行为。原版先验；
> Paper 先设 `TERM=dumb` 或关 jline。若 PTY 不可行，控制台退回 "pipe + 独立 WS"，
> 其余架构不变。

### 6.3 与现有代码的接线

| 现有构件 | 怎么接 |
|---|---|
| `web/server/HdslServer.java` | `build(...)` 里构造 `McService`；注册 `/api/mc/*`；挂 `McFeatureFilter`；`stop()` 时优雅停所有 MC |
| `web/terminal/TerminalManager` / `TerminalSession` / `TerminalTickets` | 直接复用，`brand="minecraft"`；新增一个 sink 做日志解析 |
| `web/terminal/InstanceTerminal` | 控制台开窗的现成闸门（提权 + 单实例 + ticket）；为 MC 提供 `TargetFactory` |
| `web/instance/InstanceRuntime` | 模板：状态/日志/事件/进程视图；MC 写独立的 `McRuntime`，不混用 dsh 的实例表 |
| `web/brand/BrandInstanceManager` | 模板 → `McInstanceManager`（manifest/CRUD/目录/tailLog） |
| `web/brand/BrandRuntime` / `web/zcode/ZcodeRuntime` | 模板 → `McRuntime`（State/Status/READY_TIMEOUT/launch/stop） |
| `dsh/DshPorts` | 模板 → `McPorts`（game + rcon **成对**分配/校验；用户指定 + 自动） |
| `web/task/TaskService` | 新 kind：`mc-install` / `mc-backup` / `mc-restore` / `mc-world-import`；备份用独立信号量，别塞进 `INSTALL_LIKE` |
| `web/ws/WsGateway` + `EventBus` | 新 topic `mc:<id>`（确认 topic 无白名单限制） |
| `web/audit/AuditLog` | `Channel` 加 `MC` |
| `web/emergency/EmergencyStop` | 语义模板 → `McFeatureState`（独立实例） |
| `web/config/ServerConfig` / `Loader` / `Writer` | 加 `McConfig`（enabled / portRange / javaPath / mirror / maxInstances / maxMemoryMb / runAsUser） |
| `util/CacheRepository` / `util/DigestUtils` | 清单与 jar 缓存、SHA1 校验 |
| `util/versioning/GameVersionNumber` | Java 版本要求判定（`atLeast`） |
| `dsh/NodeRuntimeManager` | 模板 → `JavaRuntimeManager` |
| `web/developer/DeveloperGuardFilter` | MC 路由纳入 guard scope |
| `web/http/ApiGuardFilter` | 不动；MC 异常也走它 |
| `web/http/StaticServlet` | 不改；SPA 自带页签，运行时按 feature 隐藏 |

> **`mc.java_path` 必须是路径，不能只写 `java`**（v0.6.31 的启动失败就是这个）。
> 控制台是 PTY，PTY 是用 `posix_spawn` 起的：它和 shell / `ProcessBuilder` 不同，
> **不搜 `PATH`**（glibc 的 `posix_spawn` 就是 `execve`，会搜的是 `posix_spawnp`）。
> 现在 `McRuntime.resolveJava` 会先把配置值解析成绝对路径再交给终端：
> 显式路径原样用，`java` 取面板自己那个 JVM，其余去 `PATH` 上找，找不到才原样返回
> （这样报错里仍然写的是你配置的值）。

## 7. 数据布局

**服务器本体（存档 + `server.jar` + 日志）与面板数据分开放**：默认在 `$HOME/mc`，
镜像里就是 `/home/hdsl/mc`。这样容器重建、换镜像、`docker compose down` 都不会碰到世界——
前提是那个路径挂了卷/宿主目录，compose 与 `docker-install.sh` 默认都会挂。

```
<mc 根：$HOME/mc，容器里 /home/hdsl/mc>   # 0700；入口每次启动把它校正到 uid 1000
  <id>/
    instance.json                      # 端口、rcon 端口、内存、版本、levelName、创建时间
    server.properties                  # 受管字段由面板写，未知键与注释保留
    eula.txt
    server.jar                         # 指向 versions/ 的硬链接（跨文件系统时复制）
    <levelName>/                       # 世界存档（默认 world/）
    logs/latest.log

<data>/minecraft/                      # 面板自己的账本与缓存，跟 /data 一起备份
  feature.json                         # 状态 + 同意记录（锁与去留决定只在内存）
  versions/<mc-version>/
    server.jar
    version.json                       # Mojang 版本 JSON 的裁剪（下载源、sha1、size）
  backups/<timestamp>.zip              # 删除/导入/换版本前的安全备份
  instances/                           # **旧位置**，只读兼容：升级前的服务器还在这儿
```

要点：

- **版本共享**：`versions/` 复用了 `DshVersionManager` 的"一次安装、多实例共享"。
- **世界是用户最贵的数据**：删实例、升级版本、覆盖导入，三处都必须**先备份**。
- **RCON 密码**只写进 `server.properties`（0600），不额外落一份到 `instance.json`。
- **端口**：`port` 与 `rconPort` 成对；改端口同时写 `instance.json` 与 `server.properties`，
  且只能在停服时改（游戏启动那一刻读 properties）。
- 独立 uid 时，MC 根归 mc 用户，面板通过同组权限管理；做不到就文档化同 uid 边界。

### 7.1 存储根：默认独立挂载，旧位置仍可用

解析顺序：`server.yaml` 的 `mc.dir` → 环境变量 `HDSL_MC_DIR` → `$HOME/mc`。
容器镜像把 `/home/hdsl/mc` 建好并归 `hdsl`；`docker-compose.yml` 默认挂 `hdsl-mc` 卷，
`docker-install.sh` 问一次（默认 `volume:hdsl-mc`）后挂上。

| 姿势 | 做法 | 代价 |
|---|---|---|
| 独立卷（默认） | `hdsl-mc:/home/hdsl/mc` | 世界与面板数据分开备份；重建容器不动存档 |
| 宿主目录 | `-v /srv/mc:/home/hdsl/mc` | 入口每次启动会把它校正到 `1000:1000`；好处是宿主机直接看到世界文件 |
| 面板数据目录（旧位置） | 创建实例时选「面板数据目录」 | 跟 `/data` 一起备份，但重建容器/换卷时和面板同生共死 |

创建向导里会显示两个位置的真实路径与可写状态，选哪个就放哪个；已经在旧位置跑着的服务器
不需要搬家——两个根都会被扫描，`instance.json` 里不记录位置，目录在哪就是哪。

### 7.2 空间爆炸的护栏

- **启动/导入/备份前检查可用空间**：低于"预留值 + 预估大小"直接拒绝（默认预留 5 GB 或 10%）。
- **软配额**：单实例超过阈值告警、禁止再备份/导入；硬配额交给文件系统（项目配额/LV）。
- **备份保留**：保留 N 份并按时间淘汰；淘汰永远不删"最近一份可用"。
- **UI 显示世界大小**；世界目录与备份目录分开，避免备份把世界再算一次。
- 磁盘满时的行为要明确：拒绝新任务，但**不杀正在运行的服务**（杀服务救不了磁盘，还会丢存档）。

### 7.3 复用现有服务器的地图（P1，已实现）

能不能把一个已经在跑的老服搬进来，取决于搬的是什么：

| 搬什么 | 支持 | 说明 |
|---|---|---|
| 世界存档 | ✅ | 创建向导可直接选旧存档 zip（创建 → 导入 → 装服务端 → 完成）；实例页「世界」也能随时导出/导入。导入前自动备份，整体替换不合并 |
| 白名单 / OP / 封禁名单 | ✅ | 「玩家」页 →「导入名单 zip…」：`whitelist.json`、`ops.json`、`banned-players.json`、`banned-ips.json` 放在 zip 根上；只认这四个名字，其它条目会被忽略（塞不进 jar） |
| `server.properties` | ➖ | 受管字段在「服务端设置」（12 项）与「高级设置」（RCON 开关）里重填；未知键不迁移 |
| `server.jar` | ❌ | P1 明确不做：改为在面板里选同一个版本，由 Mojang 官方下载并校验 SHA-1 |
| `plugins/`、`mods/` | ❌ | 都是代码，按 P2 的边界处理（见 7.4），P1 一律不带 |

操作顺序：

1. 新建实例，**版本选成和旧服一致**（世界一旦被新版加载就会升格式，旧版回不去）。
   向导第一步可以顺手选上旧存档 zip；也可以之后在实例页导入。
2. 停服（旧服也要停；`session.lock` 还在说明没停干净）。
3. 向导里选了 zip，就会被导入；没选就实例页 →「世界」→ 导入世界 zip。zip 里可以是
   `<level-name>/…`，也可以平铺（世界内容直接在根上）；上传上限 128 GiB，但开始解压前会
   按世界大小检查空闲空间，磁盘不够会先拒绝，不会留下半个世界。
   **顶层目录名和实例的世界目录名不一致会被拒绝**（以前会静默变成 `world/oldworld/…`，
   服务器找不到 `world/level.dat`，就当成新世界——等于存档没搬进来）。遇到这种 zip，
   把向导里的「世界目录名」改成那个名字，或者重新打包。
4. 「服务端设置」把端口、`level-name`、种子、在线模式等按旧服填回去；「玩家」页导入名单 zip
   （停服时才能导入；`whitelist.json` 等四个文件放在 zip 根上）。
5. 启动。第一次启动会按新版本升级世界——**导入时已经自动备份**，不满意可以回滚那份备份。

实现落点：`POST /api/mc/instances/{id}/world/import`（`McApiServlet.java` 的 `case "world"`），
`McWorlds.importZip` 负责 zip-slip 校验、大小与空闲空间检查、导入前备份与整体替换；前端在
`McPanels.tsx` 的 `McWorldPanel`（导出 / 导入按钮）与 `McCreateWizard`（创建时选 zip）。
名单导入：`POST /api/mc/instances/{id}/player-lists`，`McPlayerLists.importZip`，前端在
`McPlayersPanel`。

### 7.4 导入自带 `server.jar` 与插件（P2，**当前禁用**）

> 第三方核心（Paper/Folia/Purpur/Fabric）已改走 §19 的上游目录与哈希校验，
> **与本节的"任意 jar 导入"不是同一件事**：本节说的是操作者自己丢进来的程序，
> 没有任何已知哈希可比对。

这一节是给以后的人看的：代码骨架在
`src/main/java/org/jackhuang/hmcl/web/minecraft/McServerImport.java`，**整份注释掉、常量
`ENABLED = false`**，不参与构建出的行为。要启用它，必须先接受下面这段威胁模型。

**为什么默认关着**：MC 服务端就是一段 Java 程序，导入它等于让它以面板的 uid 运行。它读得到
`/data/auth/users.json`（管理员口令哈希）、`developer.json`、`certs/` 的私钥、所有实例的
凭证与会话。不需要漏洞——正常读文件就能把整台面板的数据面搬走。面板也没有可用的沙箱：
容器里没有 root/docker socket，JDK 21 已移除 SecurityManager，user namespace 在多数 NAS
上不可靠。所以**“隔离运行”承诺不了**，只能把“信任”这件事显式交还给操作者。

启用它至少要做到：

1. **两道门**：开发者密码提权 + 逐字输入确认语（例如 `我理解它将以面板身份运行`），
   不是勾选一个 checkbox。
2. **可验证的只到这一步**：原版 jar 比对 Mojang 清单里的 SHA-1（官方原版 → 通过）；
   其它（Paper/Forge/插件）只记录 SHA-256，标注「未识别」，不假装验证过。
3. **实例永久标记**：带外来程序的实例在列表与详情页都有「未验证的服务器程序」标记，
   重启也不消失；开启提权才看得到“仍然启动”按钮。
4. **审计**：导入的哈希、确认语、操作者、来源路径都进审计日志。
5. **导入根**：只接受容器内挂进来的只读目录（`HDSL_MC_IMPORT_DIR`），拒绝任意路径与 symlink 逃逸；
   复制不移动，大小与可用空间预检，检测到 `session.lock` 就拒绝。
6. **仍然不承诺安全**：文档里要写明“同 uid 运行，等于信任 jar 作者”。

## 8. API 契约

### 8.1 路由

| 方法 | 路径 | 说明 | 闸门 |
|---|---|---|---|
| GET | `/api/mc/feature` | 状态（`state` / `locked` / `lockRemainingSeconds` / `keepFeature`）、条款版本、EULA 哈希、`limits`（`minMemoryMb` / `maxMemoryMb` / `maxInstances`，供高级设置校验） | 功能闸门 |
| GET | `/api/mc/eula` | 官方 EULA 正文（已剥离页面外壳与导航，只留协议本身；带哈希/拉取时间；Mojang 连不上时回退到上次成功抓取的副本，副本也没有才 503） | 人类会话 |
| POST | `/api/mc/feature/enable` | 开启（需 `eulaAcknowledged` + `termsAcknowledged` + `eulaHash`/`termsVersion`/`termsTextHash` + `sawDeclineOption`/`uiVersion` + 提权；锁定中 409 + `lockRemainingSeconds`） | 人类会话 |
| POST | `/api/mc/feature/decline` | 记录"知情后选择不启用"（可选，不改状态） | 人类会话 |
| POST | `/api/mc/feature/stop` | 「停止 MC 功能」：停所有服 + 起跑 1 小时，进入锁定 | 人类会话 |
| POST | `/api/mc/feature/confirm-close` | 锁定期间 → `DISABLED`（锁定继续，走完才能 enable） | 人类会话 |
| POST | `/api/mc/feature/cancel-close` | 「我不想关掉功能」：只表明保留功能 → 保持 `ENABLED`；**锁定继续，1h 内仍不能启服** | 人类会话 |
| GET/POST | `/api/mc/instances` | 列表 / 创建（创建体含 `storage: external\|data`，不传即专用目录） | 提权 |
| GET/PATCH/DELETE | `/api/mc/instances/{id}` | 详情 / 改配置（`name`、`port`、`rconPort`、`memoryMb`、`javaPath`；端口、内存与 Java 在停服时可改，端口会同时写 `server.properties`；`memoryMb` 的范围是 256–`mc.max_memory_mb`，且硬顶 16384；`javaPath` 必须是 `GET .../java` 列出的路径，`null` 表示自动）/ 删除 | 提权 |
| GET | `/api/mc/instances/{id}/java` | 这台实例实际会用的 Java（`effective`）与可选的运行时菜单（`options`）；`selected` 为空表示自动 | 提权 |
| GET | `/api/mc/storage` | 两个存储位置的真实路径与可写状态、默认值（创建向导用） | 提权 |
| GET | `/api/mc/ports` | 当前空闲的一对端口建议（创建向导用） | 提权 |
| POST | `.../start` `.../stop` `.../kill` `.../restart` | 生命周期 | 提权 |
| GET | `.../logs?tail=N` | 日志尾巴 | 提权 |
| POST | `.../console/ticket` | 换终端 ticket（复用 `/ws/terminal/{ticket}`）；agent 的票走 `mc-console`，ticket 绑定**票**而不是会话，握手时要带同一张票的 Bearer | 提权 / `mc-console` |
| GET/PUT | `.../properties` | 受管字段读写 | 提权 |
| GET | `.../players` | 在线列表（RCON） | 提权 |
| POST | `.../players/{name}/kick\|op\|deop\|ban` | 玩家管理 | 提权 |
| POST/GET | `.../world/import` / `.../world/export` | 世界导入（流式）/ 导出 | 提权 |
| GET/POST | `.../backups`；`.../backups/{id}/restore` | 备份与恢复 | 提权 |
| GET | `/api/mc/versions` | 版本清单（镜像感知） | 提权 |

约定：

- 长操作返回 `{ taskId }`，进度走 WS（`mc:<id>`），与现有任务模型一致。
- 创建/启动带 **幂等键**，防止 agent 重试建出三个服。
- 不接受任意 URL/jar；只接受版本清单里的版本 ID。

内存上限由 `mc.max_memory_mb` 决定（默认 8192 MB，硬顶 16384 MB）；`GET /api/mc/feature`
的 `limits` 是界面唯一的来源，避免前端写死一个服务端会拒绝的数字。实例页的「服务端设置 →
高级设置」只放默认不用动、需要时才找的项：内存（手动输入 + 一次性「推荐值」）、端口、
RCON 开关与 Java 运行时；MOTD、正版验证、视距这些仍在「服务端设置」里。推荐值按宿主可用内存、当前运行
中的 MC 实例数与核心类型估算（原版 1024、Paper/Purpur/Folia/Fabric 2048、Forge/NeoForge
4096 的下限），填进输入框后仍由人确认保存，不持续跟踪——堆随宿主负载漂移会让服主难以复现
问题。RCON 默认关闭；开启后「玩家」页的命令与在线列表才可用。Java 默认「自动」（按版本
要求挑），需要固定在某个 JDK/JRE 时选 `GET .../java` 给出的一个路径；选中后该实例的启动
不再走自动挑选，停服才能改。

### 8.2 磁盘数据结构

`feature.json`：

```json
{
  "state": "DISABLED",
  "termsVersion": 3,
  "termsTextHash": "sha256:…",
  "eulaHash": "sha256:…",
  "eulaRetrievedAt": "2026-10-29T00:58:00Z",
  "eulaAcceptedAt": "2026-10-29T01:00:00Z",
  "termsAcceptedAt": "2026-10-29T01:00:00Z",
  "acceptedBy": "nas200",
  "acceptedFrom": "203.0.113.7",
  "ackMethod": "scroll+checkbox",
  "sawDeclineOption": true,
  "uiVersion": "1.4.0",
  "clientUa": "Mozilla/5.0 …",
  "buildCommit": "3f9c2ab",
  "prevAuditHash": "sha256:…"
}
```

> **没有 `closeRequestedAt` / `reEnableNotBefore`**：锁（`lockedSince` / `lockedUntil`）与
> "功能去留的决定"只存在内存里，重启即消失（§3.5、§5.6）。写进来就等于把倒计时钉死，
> 那正是这一版要避免的。

`instance.json`：

```json
{
  "id": "survival-1",
  "name": "生存服",
  "version": "1.21.4",
  "port": 25565,
  "rconPort": 25575,
  "memoryMb": 2048,
  "javaPath": null,
  "levelName": "world",
  "seed": "12345",
  "createdAt": "2026-10-29T01:00:00Z",
  "termsVersion": 3,
  "termsTextHash": "sha256:…",
  "eulaHash": "sha256:…",
  "termsAcceptedAtRef": "feature.json#termsAcceptedAt"
}
```

> **权威记录只有一份**：`feature.json`。`instance.json` 里的条款/EULA 字段只是快照引用，
> 不能独立产生第二个同意时点——两份时间不一致，等于把证据瑕疵送给对方。
> Mojang EULA 是运营者与 Mojang 之间的协议，一次同意覆盖其所有实例，不需要每个实例
> 再确认一次。

### 8.3 事件

- topic `mc:<id>`：`state`、`log`、`players`、`progress`、`exit`
- topic `mc`：功能状态变化（`feature`），供横幅与导航使用

## 9. 后端实现

### 9.1 P1 领域层（2–3 天）

- `McInstance` + `McInstanceManager`：照 `BrandInstanceManager` 的 manifest/CRUD/目录/tailLog
- `McProperties`：`server.properties` 是 **ISO-8859-1 + `\uXXXX`** 语义，不能用 UTF-8 直写；
  保留未知键与注释；受管字段：`server-port`、`level-name`、`level-seed`、`level-type`、
  `generate-structures`、`enable-rcon`、`rcon.port`、`rcon.password`、`online-mode`、
  `white-list`、`max-players`、`view-distance`、`simulation-distance`、`motd`
- `McPorts`：`reserve/resolve/isFree/validate`，game + rcon 成对；用户指定或自动；启动时再探测

### 9.2 P2 版本与 JRE（3–4 天）

- `McVersionSource`：`version_manifest.json → piston-meta → downloads.server`
- 缓存：`CacheRepository` 的 ETag/文件缓存；校验：`DigestUtils` 的 SHA1
- 镜像：官方 / BMCLAPI（照 `NpmRegistry` 的设置模型，保存即生效）
- `JavaRuntimeManager`：优先宿主 `java`；版本不满足（`GameVersionNumber.atLeast`：
  1.18→17、1.20.5→21）再下载 JRE，照 `NodeRuntimeManager`
- **不做**：assets、libraries、natives、Mojang 账号认证

### 9.3 P3 进程与控制台（2–3 天）

- `McRuntime`：照 `BrandRuntime` 的 `State/Status/READY_TIMEOUT/launch/stop`
- 启动：`java -Xms<mem> -Xmx<mem> -jar server.jar nogui`，cwd=实例目录
- 就绪：sink 里匹配 `Done (`
- 优雅停：写 `stop\n`，等 `onExit`；超时 `terminate`
- 崩溃：非主动停止的 `onExit` → 记审计 + 按退避重启（最大次数；启动失败不循环）
- `McRcon`：协议很小（长度/ID/类型/载荷，little-endian），只连 `127.0.0.1`；
  用途：玩家列表、TPS（Paper）、`save-off/save-all`、命令
- 控制台开窗：复用 `InstanceTerminal` 的 `TargetFactory` 与 ticket 流程
- **控制台不提供"停止"键**：现有终端覆盖层底栏的停止按钮映射的是实例强杀。MC 上必须移除或禁用；
  停止服务器只能走详情页的生命周期动作（提权 + 优雅 `stop`）——否则"顺手一点"就把服务器关了，
  还可能丢存档。

### 9.4 P4 世界与备份（4–5 天，最大工程量）

- 流式上传/导出，绕开现有 multipart 的 50/200MB 上限；进度回传
- 安全：zip-slip 复用 `SafePaths`/`ArchiveFileTree`；自动定位 `level.dat`
- 校验：必须有 `level.dat`；世界版本**高于**服务端 → 直接拒绝（MC 不支持降级）；
  低于 → 可升级，但先备份
- 覆盖：同名世界给"覆盖 / 另存为 / 取消"，覆盖前强制备份
- 备份：**运行中直接拒绝**（409），停机后整包；不做"边跑边 zip"，那种包看起来能
  恢复、实际可能少半个 region
- 大世界（几 GB 到几十 GB）的实际链路：浏览器用 XHR 上传并显示字节进度 → 面板把正文落到
  `<实例>/.world-import.zip`（上限 **128 GiB**）→ 立刻返回任务 id，**解压与备份在后台任务里**
  （`mc-world-import`，进度文案是"Unpacking n/N files (x MB / y MB)"，按字节而非文件数推进），
  所以反向代理的 60 秒读超时不会把一个已经传完的导入变成 504。解压上限以 zip 目录里
  **声明的解压总大小**为准（最高 512 GiB；目录没写大小时按归档 × 4、最低 2 GiB 估算）；
  解压前按"归档 + 解压后大小 + 旧世界 + 旧世界的备份 + 1 GiB"检查空闲空间（不可压缩的
  世界：首次导入约 2 倍，覆盖已有世界约 4 倍——50 GB 的世界分别约需 100 GB / 200 GB 空闲）。
  导出仍然是流式响应，浏览器直接下载，不经过页面内存。
  **反代要一起调**：nginx 默认 `client_max_body_size 1m`，不调会把上传挡在面板之外（413）；
  再加 `proxy_request_buffering off`（否则 nginx 先把 5 GB 缓冲到它自己的盘）与足够的
  `proxy_read_timeout` / `client_body_timeout`。
- 保留策略与磁盘配额

### 9.5 P5 功能开关（2 天）

- `McFeatureState`：照 `EmergencyStop` 的时钟注入与幂等，独立实例
- `feature.json` 原子写（照 `SettingsManager.writeSnapshot`）
- `McFeatureFilter`：闸门顺序见 §5.5
- 审计：`AuditLog.Channel.MC`，记 enable/close/confirm/blocked-enable/各实例动作
- **EULA 抓取与缓存**：`McApiServlet.fetchEula` 拉官方页面（`https://www.minecraft.net/en-us/eula`），
  `McHtml.legalText` 剥壳成正文，按正文的 `sha256` + 拉取时间缓存 24 小时；
  **抓成功的那一份同时落盘**（`<data>/minecraft/eula.json`），Mojang 连不上时回退到它、
  连拉取时间一起照原样展示（日志里记一条 WARNING）。这条是为了「关掉功能之后再也开不起来」：
  同意记录里存的就是这份文本的哈希，所以回退不会把用户没看过的内容算成看过。
  连副本都没有时仍然**拒绝开启**（看不到协议就不能同意）。
  **不随包分发 EULA 全文**（版权内容）
- 免责：**两个确认**（对 Mojang 的 EULA、对面板的使用条款）+ `termsTextHash` + `eulaHash`；
  开启时逐项校验，不看布尔。只有两项都确认后才写 `eula.txt` 的 `eula=true`——不要在建实例时预写
- 同一 `eulaHash` 24 小时内跳过滚动，记 `ackMethod="scroll+cached"`；换 hash 重新滚
- `enable` 记 `sawDeclineOption` 与 `uiVersion`，证明用户见过"不启用"出口
- 条款文本进仓库并打 tag；`termsVersion` 与文本版本一一对应，记 `buildCommit`；
  审计哈希链只防局部篡改，**不防宿主 root**（要抗 root 得外置尾 hash）

### 9.6 P6 Web 接线（3–4 天）

- `HdslServer.build(...)` 构造 `McService`，注册路由与过滤器
- `ServerConfig.McConfig` + loader/writer
- `TaskService` 新 kind 与信号量
- `EventBus` 新 topic；`WsGateway` 无需改
- 关闭服务器时：`HdslServer.stop()` 优雅停所有 MC，再退 JVM

## 10. 前端实现

### 10.1 路由与导航：并入实例管理

- **不新开顶级导航**。MC 作为实例管理页里的一个分类，与 dsh、第三方品牌并列：
  `/instances?view=mc`；`/minecraft`、`/minecraft/:id` 只做深链/重定向，与 `/brands` 的处理一致。
- 首屏从 `/api/health` 或 `/api/features` 取 `mcFeature`；`DISABLED` 时分类不渲染。
- 锁定期间分类保留，分类面板顶部横幅（复用 E-Stop 的视觉语言）：
  "停止功能已按下，hh:mm:ss 后解锁；这一小时内不能启服、不能重新开启"。
- **功能级操作（开启功能、停止 MC 功能、确认关闭、我不想关掉功能）放在该分类的管理面板里**，
  不散落到详情页。

### 10.2 状态与数据

- `store.tsx` 新增 `mcFeature / mcInstances / mcTasks / mcPlayers`
- `api.ts` 增加 `api.mc.*` 命名空间
- `ws.ts` 订阅 `mc:<id>` 与 `mc`

### 10.3 页面

- 入口：**实例列表 → 品牌实例**下面的一项「MC 服务器实例」。功能关闭时这一项根本不渲染；
  侧栏没有独立页签（§3.3 的「看不出存在」）。
- 列表（`/instances?view=mc`，`McInstancesView`）：**和 dsh 实例、品牌实例同一页原地切换**，
  工具栏（刷新 / 计数 / 搜索 / 新建服务器）+ 卡内滚动的实例行（启停、打开详情、失败原因）。
  它曾经是独立页面 `/mc`，点进去要"跳走再点返回"，和品牌实例割裂；`/mc` 现在只是重定向。
  点行进详情页。
- 详情页（`/mc/:id`，左侧子页签）：概览（启停/强杀/装版本/重命名/删除）、控制台、
  服务端设置、世界备份、玩家、世界、日志。
- 功能面板（**设置 → 品牌实例**，`McFeatureCard`）：状态、倒计时、开启/停止功能/
  确认关闭/我不想关掉功能。
- 开启向导：提权 → **EULA 阅读器**（正文由服务端剥壳序列化，`McHtml`）→ 滚到底 → 两个勾选 → 开启。
  正文短到不需要滚动时按「已读到底」处理——没有滚动事件，否则勾选框永远是灰的
- 开启向导里始终有次按钮**「我已知晓风险，暂不启用」**：关闭向导、保持 `DISABLED`，
  不写 `eula.txt`、不改状态（文案可以更口语：*我看完了，但我不开*）
- 停止功能按下后：按钮显示"已按下"（不可再按），旁边出现
  **「确认关闭功能」** 与 **「我不想关掉功能」**；倒计时用单调时钟，面板刷新不会跳
- 决定之后：保留功能 → 停止按钮隐藏、横幅继续倒计时（仍不能启服）；
  确认关闭 → 面板回日常（无页签），开启页显示锁定剩余

### 10.4 复用与新建

| 用途 | 复用 | 新建 |
|---|---|---|
| 控制台 | `ConsolePanel` / `TerminalWindow`（本来就是 terminal WS 协议） | — |
| 日志 | `LogView` | — |
| 进度 | `ProgressBar` | — |
| 审批/提权 | `ApprovalDialog` / `ElevationDialog` | — |
| 世界导入 | — | 流式上传 + 进度（现有 `FileDropInput` 可能不够） |
| 属性表单 | — | 受管字段表单 |
| 开关 | E-Stop banner 的视觉 | 功能状态面板（含锁定与两个去留出口） |

> 控制台复用的硬约束：**底栏「停止」必须移除或禁用**。
> `TerminalWindow` 目前把 `stopSession()` 写死在底栏（`TerminalWindow.tsx:575` / `:804`），
> 复用时要加一个 `allowStop` 之类的开关，MC 传 `false`。不能只靠"调用方不传回调"——
> 根本没有这个回调。它原本映射实例强杀；在 MC 上等于绕过提权与优雅停机，误点一次就可能丢存档。
> 停止只从详情页走，且是优雅 `stop`；强杀是另一个独立动作（需提权 + 显式确认）。

## 11. 安全模型

### 11.1 端口不是"通往面板的网络入口"

面板的 HTTP 监听在 3080；MC 是**另一个进程、另一个 socket**，25565 上没有面板的任何 handler。
攻击者连上 25565，面对的是 Minecraft 协议栈。

真正的风险是**进程级**的：

```
25565 上的 RCE（服务端/插件/模组）
  → 以 MC 进程的 OS 用户身份执行
  → 若与面板同 uid：可读 /data（p12 密码、API Key、agent home）
  → 可访问 127.0.0.1:3080 本地 API
  → 可读 /proc/<面板pid>/environ（若密码走环境变量）
  → 可杀/重启面板、写数据目录做持久化
```

注意：面板的**认证本体**其实扛得住本地读文件——会话 token 在内存、落盘是 PBKDF2 哈希。
真正被拿走的是账号 API Key、p12 密码，以及"能不能弄死/替换进程"。

### 11.2 缓解，按性价比

| 措施 | 效果 |
|---|---|
| **MC 用独立 uid**，数据根分离 | 一刀切断整条链；需宿主权限，做不到则文档化 |
| RCON 只绑 `127.0.0.1`、随机密码、绝不暴露 | RCON 是明文协议 |
| 敏感值不走环境变量（`/proc/environ` 同 uid 可读） | 堵常见坑 |
| 服务端/插件更新、`online-mode=true`、白名单 | 历史漏洞教训 |
| 公网游戏端口做防火墙/DDoS 防护 | 面板帮不上，属运维层 |

### 11.3 为什么参照物是 SSH

因为**面板就是宿主**。一旦产品开始托管进程，每个新增工作负载都继承宿主级风险。
MC 服务端与 AI agent 是同一类工作负载：执行第三方代码、不可信。
一致的立场是：放进同一信任层并写清边界，或给它更严的一层（独立 uid）。

### 11.4 免责与 EULA：阅读、同意、授权

开启 MC 功能是一条不可跳过的链路：

```
提权 → 展示 MC 官方 EULA 原文 → 滚到底 → 勾选 EULA（对 Mojang）
     → 勾选使用条款（对面板）→ 开启
```

**EULA 原文**

- 展示的是 **MC 官方协议正文**（`https://www.minecraft.net/en-us/eula`），不是摘要。
  页面本身是营销外壳套着协议：服务端先剥掉 `script/style/head`、按块级标签断行、
  反转义实体，再从协议自己的标题（`Minecraft End(er)-User License Agreement`）
  开始截取（`McHtml.legalText`）。**不要把原始 HTML 直接丢给阅读器**——那正是
  之前"看到的全是标签和导航"的原因。
- 运行时拉取，按内容哈希 + 拉取时间缓存；离线且无快照时**拒绝开启**（看不到协议就不能同意）。
- **不随包分发 EULA 全文**（版权内容）：离线环境由部署者自行准备文本并在同意页导入，
  面板只记录哈希与时间。能不分发就不分发，比标来源安全。
- 记录同意的对象：`eulaHash`、`eulaRetrievedAt`、`eulaAcceptedAt`、`termsAcceptedAt`、
  `acceptedBy`、来源 IP、`ackMethod`（见 §8.2）。
- 只有确认之后才写 `eula.txt` 的 `eula=true`——不要在建实例时预写。

**滚动到底才能勾（法律意义是 496 条，不是"同意证据"）**

- 勾选框默认 `disabled`，只有滚动容器满足 `scrollTop + clientHeight >= scrollHeight - 2` 才解锁。
- 容器高度受限，显示阅读进度；键盘/读屏用户同样要能滚到底（Tab 聚焦 + 方向键）。
- 这是 UI 摩擦，**不是安全边界**，也不能证明"人真的读了"。它的真实价值是：证明你以合理方式
  履行了《民法典》496 条第 2 款的**提示说明义务**。别把"滚到底"写进条款当卖点。
- 同一 `eulaHash` 24 小时内再次进入时，跳过滚动、保留勾选，记 `ackMethod="scroll+cached"`；
  换 hash 则重新滚。每次强制从头滚会让人走过场，反而削弱 496 条的"合理方式"。

**两个勾选框，分开（别合成一段）**

一个是对 **Mojang** 的同意（面板是转达方），一个是对 **面板** 的同意（面板是当事人）。
合成一句，两个法律关系搅在一起，将来谁都能说"我以为我在同意另一个"。

> ☐ 我已阅读并同意受《Minecraft 最终用户许可协议》（sha256:…，拉取于 …）约束。
>   我知悉：面板写入的 `eula=true` 仅为我上述同意的技术记录，不构成面板代我作出意思表示。

> ☐ 我已阅读《Minecraft 服务端管理功能使用条款》v3（sha256:…），
>   并理解其中第二、三部分所述的风险与责任分配。

**第三个出口：不启用（必须显眼）**

- 同一个确认页上，除了"两个勾选 + 开启"，始终保留一个次按钮：
  **「我已知晓风险，暂不启用」**（内部可以叫"劝退按钮/懦夫按钮"，但 UI 文案别带贬义）。
- 点击 = 关闭向导、功能保持 `DISABLED`、不写 `eula.txt`、不改任何状态。
- **不需要提权**（拒绝是安全动作），也不藏在角落；在两个勾选完成前，它与"开启"同等可见。
- 可选记一条 `mc.feature.declined` 审计（含条款版本/哈希、账号、IP），
  用于证明"面板给过拒绝的出口，用户知情后选择不开"。
- 法律意义：一个显眼、无成本的拒绝路径，会让"同意是自愿的"更站得住
  （对应 496 条的提示义务与格式条款的公平性审查）。别把它做成暗色小字。

**使用条款 v3（草案，待律师复核）**

> ⚠️ 产品/法务草案，不构成法律意见；上线前由执业律师按实际主体、是否收费、部署地区复核。
> 文本进仓库并打 tag，版本号与 `termsVersion` 一一对应——**文本改了但版本号没改，等于没有记录。**
>
> **GPLv3 边界**：本条款只做"风险告知 + 责任分配"，不设用途限制、不要求"使用即同意"、
> 不构成使用本软件的前提条件。凡"你必须……"的部署要求一律移到 `docs/deployment.md`（§17）；
> UI 条款超出 GPLv3 §7 清单的部分，下游可依法删除。

```text
《Minecraft 服务端管理功能使用条款》v3 · 2026-10-29

一、性质与定位
1.1 本条款适用于本发行版（HDSL-web vX.Y.Z，commit …）中的《Minecraft 服务端管理》
    功能。本软件整体仍以 GNU GPL v3 授权；本条款仅就该功能作风险告知与责任分配，
    不构成对 GPL 所授予权利的限制，也不构成使用本软件的前提条件。
1.2 本面板是本地部署的进程与文件管理工具，不提供游戏服务、不运营服务器，
    不是 Mojang AB / Microsoft 的代理商、授权代表或合作方。
1.3 本功能仅在你自己的设备上下载、启动、停止、备份 Minecraft 服务端程序；
    对外的游戏服务由你（下称"运营者"）提供并负责。
1.4 你与 Mojang AB 之间就服务端软件的使用，由《Minecraft 最终用户许可协议》
    及《Minecraft 商业使用指南》直接约束；本条款不改变、不免除、不替代该协议
    下的任何权利义务。
1.5 你在向导中勾选同意时，你本人即已作出同意；本面板随后写入的 eula=true
    仅为该同意的技术记录，不构成面板代你作出意思表示，亦不使你与 Mojang
    之间的法律关系经由本面板成立。

二、事实告知（你确认已理解）
2.1 服务端程序、插件、模组由第三方提供，可执行任意代码，历史上存在远程代码
    执行漏洞；是否更新、是否启用白名单与正版验证由你决定。
2.2 开启本功能会在宿主机上新增对外监听端口，攻击面随之增加；端口暴露造成的
    扫描、攻击与流量成本由你承担。
2.3 世界数据保存在你指定的存储位置。备份为尽力而为的辅助功能，不承诺任何恢复点
    目标（RPO）或恢复时间目标（RTO）。建议自行保留至少一份独立于本机的副本。
2.4 若你以收费、捐赠换权益等方式运营，请注意《Minecraft 商业使用指南》对可售
    内容有限制（不得出售影响游戏平衡的内容、不得出售披风等）；相关合规义务由你
    自行承担，本面板不代为判断。同时请自行处理实名、防沉迷、未成年人保护、
    税务与网络接入合规。
2.5 若服务器面向他人开放，你将处理玩家用户名、UUID、IP 等个人信息，你是该等
    个人信息的处理者，请自行履行告知与取得同意的义务。

三、责任分配
3.1 下列后果由你自行承担：
    (a) 服务端、插件、模组自身缺陷或被他人利用导致的损失；
    (b) 你或你的玩家违反 Minecraft EULA、法律法规或第三方权利导致的纠纷与处罚；
    (c) 你配置不当（端口、正版验证、白名单、权限、内存）导致的损失；
    (d) 你未按 2.3 保留独立备份而导致的数据不可恢复；
    (e) 云厂商、运营商、Mojang 的服务中断或政策变化。
3.2 下列情形不属于你自行承担的范围：
    (a) 面板自身的缺陷（含误删、错误覆盖、备份/恢复逻辑错误、解压越界）直接
        造成你的世界数据或设备损失的；
    (b) 面板存在故意或者重大过失的。
3.3 就 3.2(a)，面板承担的责任为：
    (a) 尽合理努力协助恢复受影响的数据（含提供备份、临时回滚、导出中间件）；
    (b) 承担一次专业数据恢复服务所必需的合理费用；
    (c) 前项费用以人民币 1,000 元为限；若你为该功能实际支付过费用，以该费用的
        12 倍为限，取二者较高值。
3.4 在任何情况下，面板均不承担间接损失、可得利益损失、玩家索赔、商誉损失，
    以及世界数据本身的财产价值。本款不适用于 3.2(b)，亦不适用于人身损害。
3.5 你通过 API、scoped token 或 AI agent 发起的操作，视同你本人操作。

四、不可排除
4.1 本条款不排除或限制依照《中华人民共和国民法典》第五百零六条及其他强制性规定
    不得排除或限制的责任。
4.2 本条款不构成对行政处罚、刑事责任或对第三人责任的免除。

五、变更与存证
5.1 本条款版本化。条款或 EULA 文本变更后：
    (a) 已创建的实例继续按变更前的条款运行；
    (b) 下次执行需要提权的操作时，须先确认新版本；
    (c) 若变更涉及第二、三部分的实质性内容（新增风险、加重你的责任），面板将在
        分类面板显示横幅，并在你确认前禁止新建实例。
5.2 每次确认记录：条款版本与文本哈希、EULA 文本哈希与拉取时间、确认时间、
    账号、来源 IP、确认方式、构建 commit，写入审计日志。
5.3 同意记录（条款/EULA 哈希、时间、账号、IP、UA、确认方式）保留 3 年；
    审计日志保留 180 天；到期自动删除。删除前可导出为独立归档文件，由你自行保管。
```

**为什么这么写（法务要点，供复核）**

- 删掉"一切…与面板无关"：概括免责撞《民法典》497 条（不合理免除自己责任）与
  506 条（故意/重大过失造成财产损失不得免责）。
- **3.3 不能写"我担责但赔 0 元"**：那是"名为担责、实为免责"，比直接免责更难看，
  也更容易被按 497 条整体否定。改成"恢复义务 + 非零小额上限 + 排除数据本身价值"：
  先给履约动作，再给上限；数据的情感/玩家投入价值无法计价，排除它更容易被支持。
- **代理说改记录说**：`eula.txt` 只是服务端的启动门槛，不是同意成立要件。用户点击时
  同意已成立，面板写文件只是技术记录。这样既消解"不是代理却代写"的表面矛盾，
  也把责任距离拉远一档。
- **勾选框只做"我读了、我理解"**，不做责任分配：勾选框是 496 条的提示层，正文才是
  497/506 条的审查对象。同一件事写两遍，等于给自己制造两个被攻击点。
- 加"非 Mojang 代理商/授权代表"：切割第三方关系，避免用户误以为面板代表官方。
- 商业使用指南改为告知式（"请注意"而不是"你必须"）：用途限制不在 GPLv3 §7 清单里，
  写了也可能被下游删除，且容易被认定为 further restrictions。
- 加 PIPL 提示：面向他人开放时，运营者是玩家个人信息（用户名/UUID/IP）的处理者。
- 无偿场景下《民法典》929 条 2 款（仅故意/重大过失）是**有利论点，不是确定性抗辩**；
  法院也可能按服务合同或侵权处理，所以不能只靠它。
- 收费（含"赞助解锁"）会叠加《消费者权益保护法》26 条，格式条款审查更严，
  上限条款要重写。

**服务端要做的**

- `enable` 必须同时带两个确认：`eulaAcknowledged=true` + `termsAcknowledged=true`，
  以及 `eulaHash` / `termsVersion` / `termsTextHash`；缺一即 400。
- `enable` 同时记 `sawDeclineOption=true` 与 `uiVersion`：只记 decline 的人，
  等于"给过出口"在庭上没有证据。
- 同一 `eulaHash` 24 小时内再次进入：跳过滚动、保留勾选，`ackMethod="scroll+cached"`；
  换 hash 则重新滚。
- 只有两项都确认后才写 `eula.txt` 的 `eula=true`；写之前再校验一次哈希。
- 条款变更后：已建实例按旧条款继续运行；下次提权操作前要求确认新版本；实质性变更
  显示横幅并在确认前禁止新建实例（**不要借用「锁定」**，那会把停服流程的解释搞混）。
- 记录字段用 `termsVersion` / `termsTextHash`（不要叫 `disclaimer*`，和文本标题对不上
  会在庭上被质疑是不是同一份）；记录 `buildCommit`，把文本钉到 commit / signed tag。
- 审计哈希链只防"局部篡改"，**不防宿主 root**；要抗 root 得把尾 hash 外置
  （只读介质 / 外部 syslog / RFC 3161 时间戳）。文档别宣称它防住了 root。
- 免责文本进仓库并打 tag；**文本改了但版本号没改，等于没有记录**。

### 11.5 agent 的边界

- 实例级原语可给 agent（scoped token：能力集 + 实例前缀 + 过期）
- **功能开关只给人类会话**：agent 不能开公网端口，也不能把实例推进锁定
- 破坏性操作（删服、恢复备份、降级、强杀）走 `TaskService` 的审批闸门

## 12. 现实检查（哪里会翻车）

| # | 风险 | 影响 | 应对 |
|---|---|---|---|
| 1 | **PTY 下 Paper/JLine** 探测终端能力，可能吐转义或卡住 | 控制台不可用 | 先验原版；Paper 设 `TERM=dumb` 或关 jline；不行退回 pipe + 独立 WS |
| 2 | **大文件流式传输**（世界/备份） | 最容易被低估的工程量 | 已实现：XHR 上传进度；落盘后转后台任务解压（任务文案报文件数）；导出走浏览器流式下载。断点续传仍没做 |
| 3 | **热备份一致性** | 备份可能损坏 | 运行中直接拒绝备份（409），停机后整包；不实现"热备看起来能用" |
| 4 | **就绪/崩溃判定靠日志** | 版本差异误判 | 英文日志格式稳定；写多版本匹配；超时兜底 |
| 5 | **配额**（内存/磁盘） | 一个服吃满宿主 | `/proc`、cgroup v2；创建时限额 + 运行中告警 |
| 6 | **`level.dat` 版本检测** | 需要 NBT 解析 | MVP 只校验存在性 + 强制备份；降级交给服务端报错 |
| 7 | **`server.jar` 链接**（Windows 符号链接需权限） | 共享版本失效 | 复制或硬链接回退 |
| 8 | **同 uid 边界** | 妥协即失守 | 文档化；`runAsUser` 可选；独立 uid 是推荐部署 |
| 9 | **测试依赖外网** | CI 不稳 | 造 `fake server.jar`（见 §13） |
| 10 | **SPA 单产物** | MC 代码在包里 | 接受；运行时隐藏；若要"真不存在"，另做 SPA 构建变体 |
| 11 | **端口暴露破坏"一个端口"承诺** | 部署复杂度 | 文档 + docker-compose 示例；可选 Velocity 收口（后续）。**RCON 端口不要发布**（明文协议，面板自己只连回环） |
| 12 | **镜像/仓库误打包 `server.jar` 或 EULA 全文** | 著作权侵权（不是免责问题） | CI 断言 + 文档红线（§17.3）；运行时下载、不随包 |
| 13 | **重启即解锁**：锁与去留决定只在内存（§3.5） | 会重启的人可以绕过那一小时 | 接受。定位是摩擦不是边界，所以不写进安全承诺；每次按下仍记审计 |

## 13. 测试策略

### 13.1 假服务端

造一个 `fake server.jar`（Java 小程序），行为可配：

- 打印 `Done (1.0s)! For help, type "help"`
- 监听指定端口
- 从 stdin 读 `stop` 后退出
- 可选：启动即崩溃 / 卡住不打印 `Done` / 内存缓慢增长

这参照项目里 `src/test/resources/fake-zcode`、`fake-pnpm` 的既有做法，让 CI 不依赖外网。

### 13.2 测试层次

- **单元**：`McProperties` 读写（含未知键/编码）、RCON 编解码、状态机（假时钟）、端口校验
- **集成**：假 jar 的启动/停止/崩溃/重启；控制台 ticket；世界导入解压与 zip-slip；
  备份/恢复
- **并发**：快速 create→delete、并发启停、批量停止
- **压测**：多实例并发启动/停止；控制台多观察者 attach/detach；不针对生产实例施压

### 13.3 验收（每阶段）

- P1：`McProperties` 往返无损；端口成对分配与冲突拒绝
- P2：清单拉取（带缓存）、jar SHA1 校验、JRE 版本判定
- P3：假 jar 的 `Done` 识别、`stop` 优雅退出、强杀、崩溃重启退避
- P4：1GB 世界导入（流式）不 OOM；zip-slip 被拒；覆盖前有备份
- P5：按下停止后进入锁定；**「我不想关掉功能」不解锁**（启服仍被拒、倒计时照走）；
  确认关闭后仍要等锁定走完才能 enable；到期后 `state` 保持决定后的值；
  **重启即清零**（锁与决定消失、持久状态不变）；倒计时用单调时钟（推进假时钟不漂）
- P6：`DISABLED` 下 `/api/mc/*` 404 且无监听；`ENABLED` 下未认证 401、未提权 403

## 14. 交付顺序与预估

| 阶段 | 内容 | 预估（单人） |
|---|---|---|
| P0 | 契约 + PTY/JLine spike | 1 天 |
| P1–P3 | 领域 + 版本/JRE + 进程/控制台/RCON | 7–10 天 |
| P4 | 世界/备份（流式） | 4–5 天 |
| P5–P6 | 开关 + Web 接线 | 5–6 天 |
| P7 | 前端 | 5–7 天 |
| P8 | 测试/硬化/并发 | 3–4 天 |
| P9 | 平台适配（Linux/Windows/容器）+ 存储护栏 | 3–5 天 |

- **L0（Linux + 能开服/停/看控制台/备份）**：约 2 周
- **完整 MVP（含世界导入、玩家/RCON、功能开关、前端全量）**：约 4–5 周
- **跨平台完整版（含 Windows headless 控制台与 §15 的部署适配）**：再 +1 周左右

建议先做 §13.1 的假 jar：它同时服务于 spike、集成测试和演示，是性价比最高的一件事。

## 15. 平台与部署

同一份代码在四种环境里，路径、控制台形态、权限模型都不一样。先把差异写死，免得实现时
默认按 Linux 写、到 Windows 才发现控制台根本接不上。

### 15.1 Linux 裸部署

- **路径**：`HDSL_DATA=/var/lib/hdsl-web`；MC 根默认 `${HDSL_DATA}/minecraft`，
  大服建议 `/srv/minecraft`（`mc.dataDir` 指过去）。
- **用户**：面板跑 `hdsl`；MC 建议独立 `minecraft` 用户 + 同组。
  非 root 的面板无法切用户——要独立 uid 就得 `sudo setpriv/runuser` 或 `systemd-run --uid=`，
  这是部署决策，不是代码默认。做不到就文档化同 uid 边界（§11.3）。
- **权限**：目录 `0750`、文件 `0640`；`server.properties`（含 RCON 密码）`0600`。
- **systemd**：`User=hdsl`、`ProtectSystem=strict`、`ReadWritePaths=/var/lib/hdsl-web /srv/minecraft`、
  `LimitNOFILE=65536`；内存上限用 `MemoryMax`（cgroup 也顺带解决一个服的失控）。
- **文件系统**：ext4/xfs；不要放网络文件系统（锁与性能）；jar 目录不能 `noexec`。
- **配额**：XFS project quota 或独立 LV，配合 §7.2 的软护栏。
- **备份**：面板目录 + `mc.dataDir`（若不是子目录）分别 tar。

### 15.2 Windows 裸部署

- **实例终端已可用，MC 控制台仍是 pipe**。2026-10-31 起 Windows 的实例终端走 ConPTY
  （`docs/web-shell.md` §7.1），浏览器里是真终端；MC 的 `McRuntime` 仍走 `pipes`
  （headless 控制台），未跟着切。后续顺序：先让 `McRuntime.ptySupported()` 在 Windows 上返回
  `ConPty.platformSupported()`、把 `launchPipe` / `consoleMode` 的 `"pipe"` 分支与
  `sendCommand` 的双路径删掉，再单独一轮真机验证（从管道变控制台是行为变化，不跟这次混在一起）。
- **第一版策略**：Windows 上 MC 以 headless 方式运行，浏览器只提供**日志流 + 命令输入**
  （写 stdin 或走 RCON），不承诺原生控制台；原生窗口标注为实验性/后续。设置页的 MC 卡片
  会在功能关闭时也显示“控制台：日志 + 命令输入（本平台没有 PTY）”。
- **退出与孤儿**：`McRuntime.stopAll()` 在面板关闭时先对所有运行中的实例写 `stop`，共享
  一个截止时间，超时才 `ProcessTree.terminate`。Windows 上这是必须的——pipe 进程不是
  `terminals` 的子会话，面板退出不会自动带走它们，会留下占着 25565 的 `java.exe`。
- **端口**：启动前用 `McPorts.isFree()` 做真实 bind 探测；游戏端口总是查，RCON 只在
  `enable-rcon=true` 时查。`/api/mc/ports` 也会避开操作系统上已有人监听的端口。
  Windows 上不做 `SO_REUSEADDR` 探测，否则旁边有监听也会被误判为空闲。
- **Java**：默认复用面板自己的 JVM（裸部署能启动面板就至少有一个 Java）；`java -version`
  的结果与来源通过 `GET /api/mc/feature` 的 `java` 字段暴露。版本不够时启动前报“需要 Java N
  或更新，当前是 M”，并提示 `mc.java_path` / `JAVA_HOME`。系统 Java 面板不碰；
  需要时可以从 Adoptium 装一份**托管 JDK**（见 §19.3），它和系统 JDK 是两个选项。
- **按版本挑 JVM**（v0.6.71 起）：所选版本要求的主版本号高于面板自身 JVM 时，面板会扫本机
  已装的 JDK 并选**满足要求的最低那个**（Linux：`/usr/lib/jvm`、`/usr/lib64/jvm`；
  macOS：`/Library/Java/JavaVirtualMachines`；Windows 不扫，用 `mc.java_path` 指定）。
  选了哪个会体现在 `java.source`：`panel`（面板自己的）、`installed`（本机另一份 JDK）、
  `configured`（`mc.java_path` 指定）。实例在「高级设置」里指定的 Java 优先于自动挑选，
  其次 `mc.java_path`。
  官方容器镜像因此内置 JDK 21 与 25：默认 `java` 钉在 21（面板与老版本 MC 沿用实测组合），
  只有 26.x 这类要 25 的版本才用 25。
- **版本与 Java 的对应**：2026 年的 `26.x` 服务端要求 Java 25+。容器镜像里直接可用；
  裸部署只装了 JDK 21 的机器仍建议在创建实例时选 `1.21.x`（`1.21.11` 已于 Windows 裸部署
  真机跑通），或自己装一份 JDK 25 ——面板会自己找到它，不必改配置。
  面板会按所选版本在启动前拒绝并给出实际 Java 版本，不会拖到 `UnsupportedClassVersionError`
  才说话。
- **编码**：子进程固定 `-Dfile.encoding=UTF-8`（含 `sun.stdout/stderr.encoding`），与 pipe
  pump 的 UTF-8 解码一致；JDK 17 + 中文 Windows 不再出现 GBK 乱码。
- **存储根**：`HDSL_MC_DIR` → `mc.dir` → `$HOME/mc`；`GET /api/mc/feature` 的 `storage`
  给出实际路径、来源与可写性。Windows 服务以 SYSTEM 运行时会落在 `systemprofile` 下，
  启动日志会明确警告；含中文/空格的路径可用，但建议短路径（>120 字符会提示 MAX_PATH）。
- **`server.jar`**：不要用符号链接（需要权限/开发者模式）；同卷用硬链接，跨卷只能复制。
  更新 jar 时文件被 JVM 占用会失败——先停再换，或换版本目录后切换链接。删除实例前会调用
  `ProcessTree.killReferencing`，把命令行里带着该实例目录的残留 `java` 进程收掉。
- **服务形态**：NSSM/WinSW/计划任务。实例终端在 session 0 下也能用（ConPTY 不需要桌面），
  但 MC 的原生窗口方案仍然要相应调整。服务停止要能触发 JVM shutdown hook，否则世界可能
  来不及保存。

### 15.3 macOS

- 默认 `~/Library/Application Support/HDSL-web`；大世界放外置卷（`mc.dataDir`）。
- 后台服务（launchd）没有 GUI，原生目录选择器不可用，走配置。
- TCC/完全磁盘访问：不要默认指到 `~/Documents`、`~/Desktop`、`~/Downloads`——
  后台服务会被权限弹窗挡住或直接失败。
- 不是主要目标平台，保持"配置驱动"即可，不额外做 GUI 适配。

### 15.4 容器

- **默认**：`mc.dataDir=/data/minecraft`，维持"拷走 `/data` 就搬完"。
- **大服**：独立卷 `mc.dataDir=/mc` + `-v hdsl-mc:/mc`。文档和备份脚本必须同时覆盖两个卷。
- 嵌套挂载（在 `/data/minecraft` 上再挂一个卷）也能用，但初始化顺序和镜像内已有目录容易踩坑；
  `/mc` 更干净。
- **资源**：compose 里给 `mem_limit` / `cpus`；MC 内存是常驻的，别让一个服吃满容器。
- **权限**：容器里面板是 root、服务端进程跑成 uid 1000；入口每次启动把 `/data`、`/home/hdsl/mc`
  校正到 `1000:1000`。用 `mc.dataDir` 指到 `/data` 以外时，那个目录要自己保证 uid 1000 可写。
- **健康检查**：不要拿 MC 的 25565 做容器健康检查——它和面板是两条命，面板活着就该 healthy。

### 15.5 部署声明（L2，法律价值最高）

判例里的常见抗辩：**默认不安全 + 文档明确警示 + 提供安全选项 = 用户选择，不是设计缺陷。**
所以下面这段要进 `docs/deployment.md`，不是藏在设计稿里：

```markdown
## Minecraft 功能的安全边界（必读）

默认部署下，Minecraft 服务端与面板以同一个系统用户运行。这意味着：一旦服务端或其
插件/模组被攻破，攻击者可以读取面板数据目录（含各 agent 的凭据）、访问
127.0.0.1:3080 的本地 API、并替换面板进程。

这是已知风险，不是缺陷。若你要把游戏端口暴露到公网，请务必使用独立 uid
（给出 systemd-run --uid= / setpriv 示例）。

本项目是自部署软件，作者不运营任何 Minecraft 服务器，不提供托管、代运维或代开服
服务。你在自己的机器上部署并对外提供服务时，你是该服务的运营者，独立承担运营责任。
```

并在 enable 流程里做一次**部署形态自检**：

```
当前检测：同 uid 部署（面板用户 = hdsl）
本功能将以同一用户运行 Minecraft 服务端。
[ ] 我理解并接受该风险    [查看独立 uid 部署方法]
```

独立 uid 部署时**不显示**这条。这样"默认值"从"我替你决定的"变成"你确认过的"。

## 16. 备份与恢复（含独立卷）

- **默认（MC 在 `/data` 内）**：`tar /data` 仍然完整，`deployment.md` 的"停容器拷 /data"不需要改。
- **独立卷**：必须同时备份面板卷与 `mc.dataDir`；恢复顺序是"先面板数据，再世界"，
  否则实例台账指向的世界目录可能还不存在。
- **一致性**：只做停机备份（运行中 `POST /backups` 回 409）；热备份（`save-off → save-all
  flush → zip → save-on`）**没有实现**，也不假装能一致。备份先写临时文件，再原子改名——
  不要产出半个 zip。
- **位置**：备份不要放在世界所在的同一个卷（盘满时一起丢）；也不要把 `backups/` 放进 `world/`。
- **保留**：按时间淘汰，永远保留最近一份可用的；恢复前先备份当前世界（回滚也要有退路）。

## 17. 许可与再分发（GPLv3 / server.jar）

### 17.1 法律层次：L0 已经替你免责了

| 层 | 载体 | 约束谁 | 强度 |
|---|---|---|---|
| L0 | `LICENSE`（GPLv3 §15/16） | 每个收到代码的人 | 最强：随代码走，无需点击 |
| L1 | 运行时《使用条款》 | 点了确认的部署者 | 中：格式条款，受 497/506 审查 |
| L2 | `README` / `docs/deployment.md` | 不约束，但影响过错认定 | 高 |
| L3 | `NOTICE` / 商标声明 | fork 与商业部署者 | 中 |

L1 是补 L0 的空白，不是重复它。GPLv3 §15/16 已经声明"不担保、不担责"；L1 只该说
GPL 不会说的四件事：开端口增加攻击面、Mojang 对收费的限制、玩家 IP 的 PIPL 义务、
备份可能失败。

### 17.2 GPLv3 §7 是硬边界：UI 条款不能超出清单

§7 只允许六类附加条款；超出即 §10 的 further restrictions，**下游可以合法删除**。
和免责相关的只有 §7(a)：偏离 §15/16 的担保/责任限制。

| 不要写 | 改成 |
|---|---|
| "不得用于商业运营" | "若你收费运营，请注意《商业使用指南》……"（告知式） |
| "你**必须**保留独立备份" | "建议自行保留至少一份独立副本" |
| "你**必须**使用独立 uid" | 移到 `docs/deployment.md` |
| "使用本软件即视为同意本条款" | "启用本功能前，你需确认以下内容"（限定在功能层） |
| "禁止用于违法用途" | 放"事实告知"段，不用"禁止" |

原则：**L1 只写"我不担保什么"和"事实是什么"，不写"你必须做什么"。**

### 17.3 server.jar：绝不能打包

这是著作权问题，不是免责问题——条款写一万字也救不了。

| 动作 | 合法性 |
|---|---|
| 运行时从 Mojang 下载到用户机器 | ✅ 用户自己下载，面板是工具 |
| 仓库/镜像里打包 `server.jar` | ❌ 违反 Mojang EULA 的"不得再分发"；责任落到分发者 |
| 随包 EULA 全文快照 | ⚠️ 版权内容，能不分发就不分发 |

GPL 视角的论证（要写进 docs）：`server.jar` 不是本项目代码，是运行时 fetch 的独立程序，
通过 `fork/exec` 启动，不与面板链接；所以不是 covered work，不触发"对应源代码"义务。
**这个论证只在"不打包"时成立**——一旦打包，两边都说不清。

文档固定三句：

```text
· 本仓库与本镜像均不包含 Minecraft 服务端程序（server.jar）。
· 服务端程序由你在首次创建实例时从 Mojang 官方或你配置的镜像自行下载。
· 本项目不随包分发 Mojang EULA 全文；离线环境请自行准备文本并由你在同意页导入，
  面板只记录哈希与时间。
```

**下载校验（实现契约）**

Mojang 的版本元数据只发布 **SHA-1**（`downloads.server` 只有 `sha1`/`size`/`url`），
所以不能假装有 SHA-256。三层：

1. 以 launchermeta（HTTPS）里拿到的 `sha1` 为主锚——镜像只能改下载地址，改不了这个值；
2. 面板在下载时**自己算一份 SHA-256**，记进 `versions/<v>/version.json`，之后复用共享 jar
   时 SHA-1 与 SHA-256 都要对得上，任一不符就重新下载；
3. 运维可以在 `<v>/trusted.sha256` 放一行哈希把版本钉死，面板会优先按它校验。

另外拒绝非 zip 的文件：反代的错误页被存成 `server.jar` 是经典事故，魔数检查比哈希先做。

CI 断言（沿用 Dockerfile 的 `test -f` 风格）：

- 镜像内不存在 `*.jar`（除自有构建产物）
- `src/main/resources/` 下无 `server.jar` / `eula.txt` / 大段英文法律文本
- 镜像体积 < N MB（异常膨胀说明有人塞了东西）

### 17.4 商标：GPL 不授予

GPLv3 §7(e) 允许"decline to grant rights under trademark law"。可以写：

```text
本项目名称与标识不随 GPLv3 授权。你可以自由分发修改后的版本，但不得使用本项目
名称/标识暗示你的部署或服务获得作者授权、认证或背书；以本项目名义提供商业托管
服务时须注明"非官方部署"。事实性描述（如"基于 HDSL-web 修改"）不受限制。
```

不能写"禁止使用项目名"——超出商标法，也做不到。

### 17.5 不做遥测

上游不收集任何部署方的同意记录。一旦收集，你就成了个人信息处理者，PIPL 义务全压过来，
"我只是个开源项目"的抗辩也没了。建议 README 明写："本功能不向任何人上报数据。"

### 17.6 证据链在多部署场景会腐化

fork 可能改文本却沿用 `termsVersion: 3`。三道防护：

1. 文本里写"本发行版：HDSL-web vX.Y.Z（commit …）"；`feature.json` 记 `buildCommit`。
2. 发布用 signed tag，`buildCommit` 必须能追到已签名 tag。
3. 不做遥测（见 17.5）。

## 18. 待定问题

1. **一小时放哪侧**：已定——放"关闭侧"，锚在「停止 MC 功能」那一刻，
   且**只在内存**（§3.5）。语义是"**按下即锁一小时 + 去留决定**"，不是"不可逆关闭"，
   也不是"取消关闭就解锁"。
2. **停止是否必须提权**：本文按"是"（§5.4）。若嫌频繁，可只对强杀/删除要求。
3. **逐个停绕过**（§5.8）：接受审计，还是阈值升级为功能级。
4. **世界导入上限**：单包大小、磁盘配额、是否分片/断点续传。
5. **热备份是否进 MVP**：已定——**不做**；运行中备份回 409，UI 与文档都说"先停服"。
6. **RCON 是否进 MVP**：不进则玩家列表只能解析日志。
7. **Paper 是否提前**：会引入 JLine 与插件生态两个新变量。
8. **独立 uid 是否默认可做到**：取决于部署方式；做不到就写进"已知边界"。
9. **是否做 SPA 构建变体**（`VITE_MC=off`）：与"单产物"冲突，按需决定。
10. **免责文案定稿**：§11.4 已按"497/506/496/929 条"重写为 v3 草案（恢复义务 + 非零上限、
    记录说、勾选框只做提示），并拆成两个勾选；仍须执业律师按实际主体与部署地区复核，
    文本要进仓库打 tag。
11. **存储根默认**：绑死 `<data>`（搬起来简单），还是首次开启就要求选目录（防炸盘）。
    本文按"默认绑死 + 可配 `mc.dataDir` + §7.2 护栏"。
12. **Windows 控制台策略**：headless + 浏览器日志/命令，还是原生窗口 + 远程不可见。
    本文按前者，原生窗口标实验性。
13. **独立卷是否进默认 compose**：本文按"默认同卷，提供注释掉的独立卷示例"。
14. **EULA 是否随包**：本文按"**不随包**"（版权内容），离线由部署者导入文本；
    这比"随包快照 + 标来源"更安全（§17.3）。
15. **功能是否收费**：免费时 3.3 的非零上限仍保留（1000 元 / 12 倍付费取高）；
    一旦收费或"赞助解锁"，《消保法》26 条叠加，上限条款要重写。
16. **是否发布 Docker 镜像**：发布 → §17.3 的 server.jar 红线与 CI 断言必须落地；
    不发布 → 风险显著下降。
17. **默认部署同 uid 还是独立 uid**：这是"设计缺陷 vs 用户选择"的分水岭。
    本文按"默认同 uid + enable 流程自检确认 + 部署文档警示"（§15.5）。
18. **条款变更时已启用功能怎么处理**：本文按"已建实例继续按旧条款运行、下次提权操作
    前确认新版本、实质性变更禁止新建实例"（§11.4 的 5.1）；**不要借用「锁定」**。
19. **重启清空倒计时的接受度**：本文按"只存内存、重启即消失"（§3.5）。代价是能重启的人
    可以绕过那一小时；收益是用户不会因为一次误按被锁在自己机器外面。若将来想收紧，
    正确方向是"落盘 + 审计 + 显式的强制恢复入口"，而不是悄悄改成不可逆。
20. **"我不想关掉功能"要不要也记审计**：本文按"记"（它是一次明确的去留决定，
    比默认到期更能说明"用户知道自己留着这个功能"）。

## 19. 第三方核心与插件/模组（2026-11 增量）

这一节是新增能力的实现契约。核心原则不变：**默认关闭、404 而非 403、摩擦跟爆炸半径走**。

### 19.1 第二道关：第三方核心

主功能（Mojang 原版）之外，第三方核心（Paper / Folia / Purpur / Fabric）有一个**独立开关**：

| 状态 | 存在哪 | 面板表现 |
|---|---|---|
| `DISABLED`（默认） | `feature.json` 的 `thirdParty` | `/api/mc/cores` 只返回 vanilla；创建第三方核心返回 404；界面上没有第三方卡片 |
| `ENABLED` | 同上 + `thirdPartyConsent` | 核心列表出现六个第三方核心（Paper/Folia/Purpur/Fabric/Forge/NeoForge）；创建向导可选；插件/模组页签可用 |

规则：

- **只有主功能 `ENABLED` 时才能开启**（`McFeatureState#enableThirdParty` 抛 `IllegalStateException`，
  接口回 409）。主功能关闭时 `/third-party/*` 与主功能一起 404。
- **开启 = 提权 + 条款逐字确认 + 审计**。条款文本随包（`third-party-terms-v1.txt`），
  客户端回显面板公布的 `sha256`，服务端比对后才落盘同意记录（`McThirdPartyConsent`）。
- **关闭第三方核心**：有第三方实例在运行时拒绝（409，提示先停）；关闭后已建实例保留，
  新建时不再提供。
- **关闭主功能前必须先关第三方**（`POST /feature/stop` 回 409）。
  `confirmClose` 会级联把 `thirdParty` 落成 `DISABLED`——子功能不能活在被关闭的主功能下面。
- **官方与第三方在界面上分开**：实例 JSON 带 `thirdParty`，列表与详情页显示「官方 / 第三方」徽章；
  「官方」指 Mojang 原版（EULA 约束、SHA-1 校验），其余一律算第三方。第二道关**同时管启动**——
  关掉之后，已建的第三方实例也不能启动（404），不只是不能再新建。
- 审计前缀：`mc.thirdparty.enable|disable|declined`。
- 提示语与文案必须写清"面板不担保这些程序的可用性与安全性"；它们不是 Mojang 的产品。

### 19.2 核心目录与下载源

`McCores`（`web/minecraft/McCores.java`）不再直连单一上游：每个核心有**有序的源列表**，
由 `McSources` 管（`<data>/minecraft/sources.json`）。第一个能应答的源获胜，`official`
永远保留在列表里；镜像不可用或配额用尽时被跳过，而不是硬撞。

| 核心 | 官方源 | 国内镜像 | 校验 |
|---|---|---|---|
| Paper / Folia | `fill.papermc.io/v3` | `mslmc` | 上游公布 SHA-256 |
| Purpur | `api.purpurmc.org/v2` | `mslmc` | 官方 MD5 / 镜像 SHA-256 |
| Fabric | `meta.fabricmc.net/v2` | `mslmc` | 上游无哈希：只校验 zip，并在 UI/文档如实标注 |
| Forge / NeoForge | Forge maven + promotions / NeoForge maven | BMCLAPI maven、`mslmc` | installer `.sha1`；镜像不提供时退化为 zip 校验 |
| Vanilla | Mojang 清单 | —（BMCLAPI 只是 302 回源，无收益） | SHA-1 + 面板自记 SHA-256 + 可选 pin |

为什么换掉直连：从国内网络看，`meta.fabricmc.net` / `api.purpurmc.org` /
`maven.minecraftforge.net` 会直接超时；PaperMC 的 v2 API 已经**官方下线**（HTTP 410
`sunset`），写死在 v2 的核心目录永远拿不到版本。这不是"网慢"，是四个不可达的上游加一个
已退役的接口。

- **配额与缓存**：MSL 的下载地址接口是 30 次/小时、60 次/天（响应头
  `x-ratelimit-limit|remaining|reset` 会被记录）。解析出来的 `url + hash` 写在
  `cores/<core>/<version>/artifact.json`，**同一个「核心 / 版本」只问一次**；进程重启、
  第二个实例、第二次打开向导都命中缓存。配额用尽时该源被跳过，直到 `resetAt`。
- **共享存储**：`<data>/minecraft/cores/<core>/<version>/server.jar` + `version.json`
  （core / version / build / source / url / 哈希 / size / `javaMajor`）。实例里的 `server.jar`
  是硬链接，跨卷时退化为复制。
- **手动导入**：`PUT /api/mc/instances/{id}/core?version=…` 上传一个 jar，落在同一处，
  `artifact.json` 记 `source: manual`。所有源都不可达时这是兜底；原版不接受上传（那条路没坏）。
- **署名**：使用 MSL 时按其条款显示「本服务由 MSL 开服器提供」与官网链接，只在启用该源时出现。
- 实例清单新增 `core` 字段（旧清单缺省 `vanilla`，零迁移）。`/cores` 的版本列表按核心来源给出，
  前端选择核心后切换版本下拉。
- 启动参数对所有第三方核心都是 `java … -jar server.jar nogui`（Paper 系与 Fabric 启动器都吃这个），
  Forge/NeoForge 用安装器写出的 `@unix_args.txt`；进程/终端/端口/审计全部复用，未新增 surface。
- `/api/mc/cores` 的第三方上游是**并行**读取的，整体 12 秒封顶；成功缓存 10 分钟、
  失败缓存 2 分钟，`?refresh=1` 强制重读（创建向导的「重试」）。同一个核心的多个源之间是
  **竞速**而不是排队：官方源超时不阻塞镜像，第一个答上来的列表直接采用（实测目录总耗时
  从 12.7 秒降到 1.0 秒）。某个上游超时时该核心仍会出现在下拉里，只是版本列表为空，
  向导会说明原因——不能因为一个 API 挂住就让界面看起来「只有原版」，也不能让一次上游
  超时把整页拖到代理的读超时。

### 19.3 面板自托管 Java（三家发行版）

- `McJavaRuntimes`（`web/minecraft/McJavaRuntimes.java`）把 **JDK** 装到
  `<data>/minecraft/java/<feature>/`，**不装进系统目录**：免 root、随数据卷持久、Win/Linux 一致。
  选 JDK 而不是 JRE，是因为多出来的 `javac` 与工具不占多少空间（解包后约 300 MB/版本），
  却省掉"某个插件工具链非要 JDK"时的二次往返。
- **三家发行版**，安装向导里选（`/api/mc/java/install` 的 `vendor`）：

  | id | 发行版 | 目录来源 | 说明 |
  |---|---|---|---|
  | `temurin` | Eclipse Temurin | Adoptium API v3 | 默认；平台覆盖最全、更新最稳，API 直接给 sha256 |
  | `microsoft` | Microsoft Build of OpenJDK | Foojay DisCo | 微软构建，Windows/Azure 打磨多；校验和在伴随文件里 |
  | `openjdk` | OpenJDK 官方参考构建 | Foojay DisCo | 最贴近上游、无厂商补丁；只有 GA，补丁版本以新目录发布 |

  Temurin 之外的两家没有自己的目录 API，元数据经 Foojay DisCo 取；**校验和仍以厂商
  发布的 `.sha256`/`.sha256sum.txt` 为准**（Foojay 只给地址，不给结论）。读不到校验和的包直接跳过。
- **向导**（`McJavaInstallWizard`）：先选 LTS 主版本（8/11/17/21/25），旁边是对照表
  「MC 版本范围 → 最低 Java」；再选发行版（每家一段说明：许可、平台、更新节奏）；
  最后确认并安装。某个发行版没有该主版本时按钮禁用并写明（实测 Microsoft 没有 8，
  OpenJDK 只有 17/21/25）。
- 下载后**先校验 SHA-256 再解包**（Temurin 用 Adoptium 给的，另外两家用厂商伴随文件里的），
  zip 与 tar.gz 都支持，先解到临时目录再原子落位；`HDSL_JAVA_API` 只覆盖 Temurin 的 API 基址。
- **权限是显式设的，不是从归档里抄的**：解包用的是面板的 umask，`bin/java` 会落成 `0644`
  ——谁都执行不了，root 也不行（`execve` 至少要一个执行位）。安装完成后统一规范：
  目录 `0755`、`bin/*` 与 `lib/jspawnhelper` `0755`、其余 `0644`。属主**保持面板自己**，
  不交给 agent：和 Node runtime 同一条理由，agent 只该读和执行，不该能改 `java`。
  `<data>/minecraft/java` 本身也显式 `0755`，否则 umask 077 的镜像里 agent 连目录都进不去。
- 启动选 Java 的顺序：实例「高级设置」里指定的 `javaPath` > `mc.java_path` > 面板托管里满足
  要求的最低版本 > 宿主机已装 JDK > 面板自身 JVM。指定的路径必须来自 `GET .../java` 的
  `options`，停服才能改；选中的 JDK 被删掉时启动会明确报错，而不是悄悄换一个。
  版本要求先读 `version.json` 的 `javaMajor`，缺失时用内置推荐表（1.20.5+→21、1.18–1.20.4→17、
  1.17→16、≤1.16.5→8、26.x→25）。
- **不下载 Oracle JDK**：Oracle JDK 17/21 可以在 NFTC 下免费「使用」，但不允许再分发；
  JDK 8/11 生产环境还要付费。面板代下载就是再分发，所以 `install` 在**发出第一个字节之前**
  检查解析出的包 URL 主机（`oracle.com` / `oraclecloud.com` / `java.com` 及其子域），命中即拒绝。
  检查的是 URL 而不是源的名字：把 `HDSL_JAVA_API` 指到一个发 Oracle 包的镜像也绕不过去。
  操作者自己装的 Oracle JDK 仍可通过 `mc.java_path` / 实例 `javaPath` 使用——那是他自己的许可责任。
- API：`GET /api/mc/java/runtimes`（托管列表 + 推荐版本 + Adoptium 可下载项 +
  **只读的面板 JVM / PATH java** + 把实例钉在托管版本上的 consumers）、
  `POST /api/mc/java/install`（任务）、`POST /api/mc/java/{feature}/remove`
  （有实例钉在该版本时 409，先去实例高级设置改掉）。
- UI：设置侧栏「Node.js」下面多一个**独立页签**「Java 运行时」（`?tab=java`），不和
  Node.js 混在同一页。页签内容只在 MC 功能开启后可用（`/api/mc/*` 的门禁）；未开启时
  显示一行说明。面板自身 JVM、PATH 上的 `java`、MC 当前生效的 Java 三行**只读展示**，
  页面上没有任何控件能换掉它们。

### 19.4 插件 / 模组

- 文件夹跟着核心走：Paper/Folia/Purpur → `plugins/`，Fabric/Forge/NeoForge → `mods/`，
  vanilla → 明确拒绝。
- **停用 = 改名**：`x.jar` ⇄ `x.jar.disabled`。没有 sidecar 状态，手改即可恢复。
- 上传：只收 `.jar`，zip 魔数校验，不覆盖同名文件；路径穿越在解析层拦截。
- Modrinth 搜索/安装（`HDSL_MODRINTH_API` 可换镜像，官方不可达时自动回落
  `mod.mcimirror.top/modrinth/v2`）：按核心映射 project_type（plugin / mod）与 loader；
  安装取最新兼容版本，**SHA-512 校验通过才落盘**。
  - **插件不只用 `paper` 一个分类**：Paper 能加载 Bukkit/Spigot 插件，所以分类用
    OR（paper / spigot / bukkit / purpur / folia），否则目录被砍掉一大半。
  - **中文查询先翻译**：内置一张常用词表（经济→economy、领地→claim、优化→optimization…），
    没有这一层，中文搜索几乎总是空结果。
  - **分页**：`?offset=&limit=`，返回 `totalHits`；每次 20 条，界面上「加载更多」。
  - **版本过滤可放宽**：严格过滤 0 条时去掉版本 facet 再查一次，结果标 `relaxed`，
    安装时仍按游戏版本重新校验。
- **装完记元数据**：从 Modrinth 安装的文件旁边写 `<jar>.panel.json`
  （projectId / slug / title / versionId / versionNumber / installedAt）。列表据此显示
  「标题 + 版本」而不是一串文件名；删除时一并清掉，停用改名不影响它。
- 管理面：按名字/标题/版本过滤已装文件、全选后批量启用/停用/删除、单个启停与删除。
- 客户端专用识别：读 `fabric.mod.json#environment`、`META-INF/mods.toml#side`、
  `quilt.mod.json`，只做标注，不自动禁用（误判会关掉能用的东西）。
- API：`GET|PUT|DELETE /api/mc/instances/{id}/files…`、`POST …/{name}/toggle`、
  `GET /api/mc/modrinth/search`、`POST /api/mc/modrinth/install`；写操作提权 + 审计
  （`mc.instance.mod.*`）。

### 19.5 Forge / NeoForge（安装式核心）

- 上游不发"现成可跑的 jar"：`installer.jar` 会下载原版、打补丁、在 `libraries/…` 下写
  `unix_args.txt`（Java 参数文件，含 classpath 与主类）。面板因此**按实例安装**：
  工作目录就是实例目录，安装器下载的 libraries 写在那里；共享会让每个实例共用同一个世界目录。
- 版本来源：Forge 用 `promotions_slim.json`（`recommended` 优先，否则 `latest`）；
  NeoForge 用 maven metadata，前两个数字编码游戏版本（`21.4.123` → 1.21.4，跳过 beta/alpha）。
- 安装流程：下载 installer（校验上游 `.sha1`；镜像没有 `.sha1` 时**记一条警告**，只剩 zip 结构
  校验，不当成已验证）→ 以 **agent 身份**跑 `java -jar <installer> --installServer`（cwd = 实例
  目录，输出进任务日志，20 分钟超时；安装器是第三方代码，不能拿面板的 root）→
  找 `libraries/**/unix_args.txt`；老版本没有则退回启动器 jar → 写 `launch.json`
  （`argsFile` 或 `jar`），删掉 installer。
- 启动：`McRuntime` 先看 `launch.json`，有则用 `java @unix_args.txt nogui`
  （或 `-jar forge-….jar nogui`），没有才退回 `-jar server.jar`。**判"已安装"也不能只看
  `server.jar`**：Forge/NeoForge 根本没有这个文件，只看 jar 会把装好的实例拒在启动门外。
- **重定向**：BMCLAPI 的 Maven 树是 `302`（→ CERNET/USTC 镜像），JDK 默认 `NEVER` 不跟随，
  于是国内源看起来"死了"、每次安装都悄悄回退到官方 `maven.minecraftforge.net`。
  `McForge` 的客户端改 `followRedirects(NORMAL)`；只影响元数据与 installer 的获取。
- **libraries 也走镜像**：installer 自己下载的 libraries 与 installer jar 来自哪里无关，
  所以只要该核心的源列表里有 `bmclapi`，安装时就追加
  `--mirror https://bmclapi2.bangbang93.com/maven/`。**结尾斜杠是必需的**：installer 是
  字符串拼接，少了它会拼成 `mavencom/…` 然后 404 回退官方。
- **中断**：下载循环按块检查 `Thread.interrupted()`，`TaskService#cancel` 除了杀子进程还会
  `interrupt()` worker——没有子进程的下载原来"取消无效"。
- **重试**：安装开始前清掉 `.installer/*.part`（上次被杀留下的半截文件不会续传，只会堆积）。
- **阶段文案**：`Progress.stage(...)` 区分「Downloading …」与「Installing …」，
  慢速下载不会再显示成"正在安装"。
- 每个源失败都打 WARNING（`bmclapi forge: HTTP 302` 这类），不再静默跳过。
- 边界（写进 UI 提示）：安装耗时数分钟、libraries 占 100–300 MB 且跟实例走；
  混合服（Mohist/Arclight）与 mrpack 整合包仍不在范围内。

### 19.6 界面：原版与第三方彻底分开

- 侧栏是**两个入口**，不是同一列表里的标签：
  - 「MC 原版服务器实例」→ `?view=mc`，`GET /api/mc/instances?scope=vanilla`；
  - 「MC 第三方服务器实例」→ `?view=mc-third`，`GET /api/mc/instances?scope=thirdparty`。
  后者只在主功能与第二道关都开启后出现；直接访问旧链接而第二道关关闭时，页面给一句话说明，
  不渲染列表。
- 原版向导（`McCreateWizard`）保持原来的四步，核心选择器被收成只剩原版——多出来的镜像与
  第三方核心不该让原版链路变复杂。
- 第三方向导（`McThirdPartyWizard`）是另一套五步：**核心 → 版本（可搜索）→ 运行环境
  （内存推荐值、Java 推荐、端口）→ 实例信息（名字、世界、存储、存档）→ 安装**。
  安装失败时给出三个出口：重试、检查下载源、**手动导入 server.jar**。
- 第三方那一栏的「全局设置」打开 `McThirdPartySettings`：每个源的说明与配额余量、
  每个核心的尝试顺序（可增删、可上下移动）、自定义镜像基址。保存走
  `POST /api/mc/third-party/sources`（提权 + 审计 `mc.thirdparty.sources`）。
  启用了带署名要求的源时，这一页显示对应署名。

### 19.7 仍未做（明确列出）

- **mrpack 整合包**：Fabric 系可复用现有核心 + 插件/模组能力；Forge 系虽然已有安装器，
  但还需要处理 `overrides/`、`env.server == unsupported` 过滤、每文件 sha512 与
  `dependencies.fabric-loader` 的版本固定，留作下一版。
- 混合服（Mohist/Arclight/Banner）：插件与模组同时加载，需要自己的兼容性说明，暂不做。
