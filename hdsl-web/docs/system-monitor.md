# 系统资源监视（通用 → 系统资源）

面板在「设置 → 通用」里给自己和这台机器拍一张体检照，布局照任务管理器的
「性能」页：总览 / CPU / 内存 / GPU / 硬盘 / 网络，默认总览；手机端只留总览。
数据全部来自服务端的一个采样器，浏览器只负责画。

## 1. 采样：两档，永不超过每秒一次

| 档位 | 触发 | 周期 | 采集 |
|---|---|---|---|
| 快 | 最近 5 分钟有已认证请求 | 1 秒 | `/proc/stat` 每核 + meminfo + diskstats + net/dev |
| 慢 | 5 分钟内没有人/agent 使用 | 30 秒 | 内存、磁盘余量、load、进程自身；不解析每核 |

判定用的是**活动**，不是会话是否存在：`AuthService` 每次请求都把会话续到一周，
一个关掉但没登出的浏览器会让 session 活很久。所以 `SystemActivity` 记的是
「最后一次已认证请求」，登录、`AuthFilter` 验过的会话请求、被接纳的 agent 票
都会 touch 它；touch 会立刻唤醒慢档，不用等满 30 秒。

浏览器**不能**指定采样频率：`/api/system/stats` 只返回最新一帧，没有
`?interval=`。前端的 1/2/5 秒只是"多久来取一次"。

## 2. 历史：240 帧，只在内存

- 环形缓冲 240 帧：快档 4 分钟，慢档 2 小时；模式切换不清空，前端按 `at`
  时间戳画，不用序号。
- 面板重启即清空；**不落盘、不写日志**。日志里只有异常事件。
- 首次打开带 `?history=240`，曲线立刻是满的，不用等一分钟。

## 2.1 内存页的四个数

Linux 会把空闲内存拿去当页缓存，所以「总量减空闲」看起来永远是满的。面板按
内核自己的口径拆成四块，四块加起来正好是总量：

| 块 | 含义 | 来源 |
|---|---|---|
| 使用中 | 拿不回来的部分 | `total - available` |
| 已缓存 | 页缓存 + buffers | `Cached + Buffers` |
| 可回收 | 不在页缓存里的可回收内存（slab 等） | `available - 已缓存 - 空闲` |
| 空闲 | 完全没在用的内存 | `MemFree` |

`可用 = 已缓存 + 可回收 + 空闲`（就是 `MemAvailable`）。内存条按这四块上色，
折线图是 `使用中` 从 0 到 `总量` 的绝对刻度，和任务管理器的内存页一致。

cgroup 有限额时，`使用中 = memory.current - memory.stat:file`，`已缓存 = file`，
`空闲 = 限额 - memory.current`；没有 `memory.stat` 的旧内核把缓存算 0，宁可少
显示一类也不编数字。

## 3. 接口

| 方法 | 路径 | 作用 |
|---|---|---|
| `GET` | `/api/system/stats[?history=N]` | 最新一帧 + 最多 240 条历史 |
| `GET` | `/api/system/info` | 监听地址/真实端口/挂载前缀/数据目录/公开地址/版本 |
| `GET` | `/api/system/storage[?refresh=1]` | 数据目录按顶层子目录分桶的占用（缓存 60 秒） |

三者都只要求登录会话；`developerGuard = all` 时也在白名单里（见
`DeveloperGuardScope#alwaysAllowed`）。agent 票需要 `system` scope，`read`
票拿不到——这不是安全边界（容器里的 agent 直接读 `/proc` 就行），是显式的
"允许"声明，方便审计。

## 4. 平台差异与文案

快照里带 `host: {os, environment}`：`os` 是 `linux` / `windows` / `macos` /
`other`，`environment` 是 `container` / `wsl` / `native`（Linux 看
`/.dockerenv`、cgroup、`container` 环境变量与 `/proc/version`）。页面的说明按
这两个字段切换，不再写死 Linux：

- 每核：「容器里显示的是宿主机核心」/「WSL 里是 Linux 虚拟机的核心」/「每个逻辑处理器」；
- 磁盘：「宿主机级读数」/「虚拟机级读数」/「按物理磁盘统计」；
- 网络：「容器自己的网络命名空间」/「WSL 自己的」/「按网络接口统计」；
- 内存：Linux 讲页缓存与 slab，Windows 明说“缓存暂未采集”，只显示使用中与空闲；
- 能力位为 false 时显示「这一版/当前平台暂未采集 X」，而不是假装 0，也不画空网格。



| 值 | Linux | Windows |
|---|---|---|
| 总 CPU | `/proc/stat` 差值 | `NtQuerySystemInformation` 的每核累计值 |
| 每核 | `/proc/stat` 的 `cpuN` 行 | `NtQuerySystemInformation(SystemProcessorPerformanceInformation)`（JNA/ntdll） |
| 内存 | meminfo；有 cgroup 限额时显示限额 | `GlobalMemoryStatusEx`：物理总量、可用；Vista 的 standby 缓存暂未采集 |
| 磁盘吞吐 | `/proc/diskstats`（**宿主机级**） | PDH `\PhysicalDisk(_Total)\Disk Read/Write Bytes/sec` |
| 网络吞吐 | `/proc/net/dev`（容器自己的命名空间） | PDH `\Network Interface(*)\Bytes Received/Sent/sec` |
| 负载 | `/proc/loadavg` | 无（`—`） |
| GPU | 预留：`gpu: []` + `capabilities.gpu=false` | 同左 |

Windows 上任何一个读取失败（性能计数器损坏是常见故障、DLL 不可用、JNA
加载失败）都只会把对应的能力位变成 `false`，页面隐藏那张卡或写“暂未采集”，
不会显示 0。`NtQuerySystemInformation` 一次只覆盖当前处理器组（≤64 个逻辑
处理器），超过 64 核的机器会只显示当前组。

**容器里 CPU 与内存不是命名空间隔离的**：`--cpus=2` 的容器仍然看得到宿主全部
核心，所以框框画的是宿主核心；有 cgroup 配额时会在旁边标「配额 x 核」。磁盘
吞吐同理，是宿主的；网络是容器自己的。

## 5. 异常日志

只在异常时写面板日志，前缀 `[monitor]`，绝不写历史数组：

| 指标 | 阈值 | 持续 | 冷却 |
|---|---|---|---|
| CPU | ≥ 90% | 30 秒 | 10 分钟 |
| 内存 | ≥ 90% | 60 秒 | 10 分钟 |
| 磁盘 | 使用率 ≥ 90% | 立即 | 1 小时 |
| load | > 核数 × 2 | 5 分钟 | 10 分钟 |

条件恢复时再写一条「recovered」。慢档也会跑这套判定，所以凌晨没人看的时候
内存涨到 95% 仍然留得下证据。

## 6. 设计约束

- **一次采样、所有客户端共享**：多个标签页和 agent 看到的是同一帧，数字不会
  互相打架；代价是采样器必须自己决定频率。
- **首帧没有速率**：差值需要两次采样，`-1` 表示"采集中"，不是 0。
- **不引图表库**：sparkline 是 40 行 canvas；每核是 CSS grid 色块。
- **历史不持久**：要长期曲线得接外部监控（这不是本面板的职责）。
