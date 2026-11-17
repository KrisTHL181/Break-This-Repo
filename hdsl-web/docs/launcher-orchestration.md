# 启动器编排：创建即安装、启动即补装

这篇文档说的是 HDSL-web 从「REST 接口各管一段」变成「像启动器一样办事」的那次改动：
创建实例就自动装好它要的 dsh，没装的实例点「启动」会先装再启，安装过程和失败原因都在面板上。

## 背景：原来坏在哪

桌面版 HDSL 有一条龙的 `InstallTask`（`ui/dsh/install/DshInstallWizardProvider`）：创建 → 安装 dsh →
装预设插件，失败就 `discardPartial` 删掉半成品。Web 版把这条编排拆成了若干需要用户分步触发的接口，
于是出现四个可观察的问题：

| 症状 | 根因 |
| --- | --- |
| 「快速创建」建出来的实例从来不自动安装 | `POST /api/instances` 只写 `instance.json`，向导也只调它；全仓没有 `autoInstall` 之类的入口。现场 `instances/dsh-0.0.1-rc.1/` 只有 `home/` + `instance.json`，没有 `dsh/` |
| 新实例显示成「已停止」，一点启动就失败 | 前端 6 处按 `NOT_INSTALLED` / `INSTALLING` 做门禁，而后端 `stateOf` 只会返回 STOPPED/STARTING/RUNNING/STOPPING/FAILED，永远不发这两个状态；启动被后端 409 `not installed` 挡住，只弹 4.5 秒英文 toast |
| `instance.json` 的版本和磁盘上的实装版本对不上 | 前端详情页的版本选择器状态不随实例重置（路由不重挂组件），会把上一个实例的版本提交给下一个实例；后端 `/install` 成功后无条件把 `instance.json` 改写成「请求的版本」，从不回读磁盘 |
| 安装失败没有任何痕迹 | pnpm 输出只进内存任务表，容器日志里只有 `Installing …`；任务表不持久化，面板一重启连失败原因也没了 |

## 现在的编排

### 状态机

实例状态在原来的五态之上补了两个，前端早就写好的卡片这下真的会显示：

```
NOT_INSTALLED ──install──▶ INSTALLING ──▶ STOPPED ──launch──▶ STARTING ──▶ RUNNING
      ▲                         │                                          │
      └──────── 失败/取消 ───────┘                             stop ──▶ STOPPED
```

判定顺序（`InstanceRuntime.stateOf`）：活进程 > 停止中 > 安装任务 > 未安装 > 失败 > 已停止。
活进程排在安装任务前面，是因为「启动即补装」把安装和启动放在同一个任务里：刚装完的进程正在起来时，
诚实的答案是 STARTING 而不是 INSTALLING。

### 三个入口

1. **创建即安装** —— `POST /api/instances` 默认 `autoInstall: true`：创建成功后立刻提交安装任务，
   响应 `201` 带 `installTaskId`。传 `"autoInstall": false` 只要一个空壳（脚本/测试用）。
   `InstancesApiServlet.submitInstall` 是三个入口共用的那一个任务体。
2. **安装** —— `POST /api/instances/{id}/install`，202 + `{taskId}`；实例在运行/启动/停止中会被拒绝
   （别在进程跑着的时候换文件）。
3. **启动即补装** —— `POST /api/instances/{id}/launch` 发现没装时不再 409：先装，装完在同一个任务里
   继续启动，202 返回 `{"state":"installing","taskId":…}`。传 `"autoInstall": false` 退回旧的
   409 `not installed` 语义。

### manifest 描述磁盘，不描述请求

安装成功后回读 `<instance>/dsh/node_modules/@deepseek-ai/dsh/package.json` 的 `version`：

- 与请求一致 → 正常完成；
- 不一致（registry 给了别的东西、store 出了怪事）→ **把磁盘上的真实版本记进 `instance.json`**，
  并把任务判为失败，错误里写清「请求了 X，装上来的是 Y」。
  宁可让实例如实地说"我装的是个不该装的东西"，也不要让它谎称自己是指定版本。

### 失败看得见

- 安装任务失败时，`Logger.LOG.warning` 会把「哪个实例、哪个版本、为什么」加上 pnpm 最后 20 行
  输出写进容器日志（`docker compose logs hdsl-web` 就能看见）。
- 同一段文字进任务记录（`GET /api/tasks/{id}`、`GET /api/instances/{id}` 的 `installProgress`），
  面板的「未安装」卡片直接把它显示出来。
- 两类**重试无用**的失败会多一行提示（`InstancesApiServlet.registryHint`）：`ERR_PNPM_FETCH_404`
  会点名上游已删除的那个包（例：`dsh 0.0.1-rc.1` 依赖的 `@deepseek-ai/dsh-compact-tool-result-prune`
  被删，该版本再也装不上），`ERR_PNPM_NO_MATCHING_VERSION` 会说明配套包没有对应版本。
  面板的版本列表看不到这类问题：它对"早于配套包机制"的版本一律乐观地标成可安装。
- 任务表仍在内存里（面板重启会丢历史），但原因不会再只存在于内存。

## REST 契约变化一览

| 接口/字段 | 变化 |
| --- | --- |
| `POST /api/instances` | 新增可选 `autoInstall`（默认 true）；响应新增 `installTaskId` |
| `POST /api/instances/{id}/install` | 运行中拒绝；安装结束会把 `instance-state` 广播到 WS，前端不用等轮询 |
| `POST /api/instances/{id}/launch` | 未安装时自动补装再启动；`autoInstall:false` 可选退出 |
| `state` 取值 | 新增 `NOT_INSTALLED`、`INSTALLING` |
| `GET /api/tasks`、任务快照 | 新增 `instanceId`（WS 的 `task` 事件早就有 `instance` 字段） |

## 取舍与已知限制

- 未安装的实例点「启动」会等一次真实的 pnpm 安装（分钟级，可取消）。这是要的启动器行为，
  等待感由「安装中」卡片（进度 + 消息 + 取消）承担。
- 面板的版本选择器现在跟着实例走：换实例、实例版本变了都会重置，不再可能把 A 的版本装到 B 上。
- 对齐桌面版但**没做**的两件事：升级后按 profile 重装插件、就绪后把实际端口回写。
  端口目前仍是每次读取时临时解析（`InstanceRuntime.portOf`）。
- 桌面版共享的领域层文件（`DshVersionManager`、`DshPaths` 等）没有为了这次改动分叉，改动全部落在
  `org.jackhuang.hmcl.web.*` 与 `web/`（面板）里。

## ACP 控制台也拿实例的账户

面板的「控制台」是**另一个 dsh 进程**：`node <实例>/dsh/... --profile acp`，与实例的 web 进程
各活各的。账户注入原本只做在启动 web 进程那条路上，于是控制台既没有供应商路由也没有密钥，
每次对话都止于 dsh 自己那句
`no API key for provider route "deepseek-official"`——点名一条用户从没选过的路由。

现在同一条注入也做给控制台的 profile（`AcpAccountBridge`，web 层）：

| | 启动 web 进程（`DshLauncher.plan`） | ACP 控制台（`AcpAccountBridge`） |
| --- | --- | --- |
| 供应商路由写进 | `profiles/<实例 profile>` 的补丁层 | `profiles/acp` 的补丁层 |
| 密钥走 | 子进程环境（路由同名变量 + DeepSeek 的 `DEEPSEEK_API_KEY`） | 同上 |
| 默认模型 | 仅当账户不是启动器自家厂商时写进 `settings.yaml` | 同上，同一文件、同一条件 |
| 生命周期 | 启动开始写到启动结束整段 | 控制台起来时写，`acp-stop`/进程退出时删 |

两处刻意的差别：

- **不碰 `settings.yaml` 的注记**（`.hdsl-injected.json`）。那是每个 home 一份、正在运行的 web
  实例在用的东西；控制台再去 capture/settle 会把它覆盖掉。控制台只动自己 profile 的补丁层、
  子进程环境，以及上面那一个条件性写入。
- **补丁层里别的账号留下的路由会被收回**：只按启动器知道的账户名（`SettingsManager` 里的账户）
  收回，和一次启动收回的集合一致。

推论与边界：

- 官方 DeepSeek 账户：控制台用 dsh 自带的官方路由 + 账户的密钥（`DEEPSEEK_API_KEY`），
  与 dsh 网页用的是同一把钥匙但不是同一条路由；第三方账户：路由与默认模型都按账户走。
- 实例没指定账户（`accountKey` 为空）时，控制台完全按老样子启动，不做任何注入。
- 账户注入失败（供应商拉不到模型列表等）只记 `[acp]` 警告并退回"无账户"的启动方式，
  不会让控制台起不来；这时候对话仍会报缺密钥，原因在容器日志里。

