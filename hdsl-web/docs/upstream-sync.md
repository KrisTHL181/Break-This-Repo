# 上游同步：怎么把 HDSL 的修复搬过来

HDSL-web 的领域层（`org.jackhuang.hmcl.dsh|setting|util`）是一次性从桌面版 HDSL 拷来的拷贝层，
此后两仓库各自演进（GPLv3 允许，保留版权头即可——见 `docs/plan.md`）。这份文档写的是**怎么把
上游后来的修复搬进来**，以及搬的时候踩过什么。

## 为什么不能 cherry-pick

- 上游仓库：`https://github.com/MCXCC303/HDSL`（本机 `../HDSL`，分支 `master`）。
- 分叉后不久上游做了两件事：包名 `org.jackhuang.hmcl.*` → `com.mcxcc303.hdsl.*`，以及把单体类拆成
  `plugin/ pack/ instance/ harness/ core/ runtime/ skill/ …` 十来个主题包。上游 diff 的路径与上下文
  在 PORT 里对不上，**只能按方法手工移植**。
- 拷贝基线也不是某一个上游提交：逐文件比对下来，`ArchiveFileTree`、`DshPluginRequirements`、
  `DshPluginSources` 与 `f831c10`（2026-09-27）一致，而 `SettingsManager`、`LauncherSettings`、
  `DshVersionManager` 与 `f831c10`、`45baa6e` 都不同（PORT 侧有自己的 de-JavaFX 与 Windows 适配）。
  所以移植一律**以方法为单位**比对，不要整文件替换。

## 类名对应表（实测）

| 上游（现在） | PORT |
|---|---|
| `plugin/DshPluginBundle`、`plugin/DshPluginSources` | `dsh/DshPluginBundle`、`dsh/DshPluginSources` |
| `harness/HarnessInstaller` | `dsh/DshVersionManager` 的 `install` 等（上游后来才拆出去） |
| `pack/PackExporter`、`pack/PackManifest` | `dsh/DshModpacks`（`export` / `localPlugin` / `Manifest`） |
| `instance/DshInstanceManager` | `dsh/DshInstanceManager` |
| `core/DshCommand` | `dsh/DshCommand` |
| `runtime/DshNodeRuntime`、`runtime/NodeRuntimeManager` | `dsh/DshNodeRuntime`、`dsh/NodeRuntimeManager` |
| `harness/DshDoctor` | `dsh/DshDoctor` |
| `setting/SettingsManager`、`setting/LauncherSettings` | 同名同路径 |
| `util/tree/ArchiveFileTree` | 同路径 |
| `skill/DshSkillSource`（+ `DshSkillSources`/`DshSkillCatalog`） | `dsh/DshSkillSource` |
| `core/Semver`、`harness/VersionOrder` | **无**：范围判定在 `dsh/DshPluginRequirements`，版本比较在 `dsh/DshVersionManager.compareVersions`（委托 `util/versioning/VersionNumber`） |
| `ui/**`（JavaFX） | **无**：面板是 `web/src/**` 的 React SPA，上游的 JavaFX 改动通常不需要对应物 |

## 每月扫一遍

```bash
cd ../HDSL && git fetch origin && git log --oneline --since="5 weeks ago" origin/master --grep='^fix'
# 逐条看它改了什么、落在哪个类
git -C ../HDSL show <sha> --stat
# 再去 PORT 里找对应方法，确认是否已经修过（先 grep 方法名，再看行为）
grep -rn "<方法名>" src/main/java/org/jackhuang/hmcl/
```

判断标准：**落在拷贝层**（`dsh/`、`setting/`、`util/`）的 fix 才需要考虑；产品概念、UI、桌面单用户
语义的改动一律不搬。

## 本批（v0.4.7 未发布）已搬的

| 上游 | 内容 | PORT 落点 |
|---|---|---|
| `03d9f34` | 压缩包条目名整条校验（`..` / `\` / NUL） | `util/tree/ArchiveFileTree.addEntry` + `isOneRelativePath` |
| `43401ca` | 插件包成员与软链不得写到解压目录之外 | `dsh/DshPluginBundle.release`、`dsh/NodeRuntimeManager.extract` |
| `8760af8` | 设置文件经移动落盘，绝不截断 | `setting/SettingsManager.save`/`writeSnapshot`/`move` |
| `8062b6e` | 改名不再删除占用新名称的目录 | `dsh/DshInstanceManager.rename` |
| `66415a6` | 创建不认养已存在的目录（+ PORT 配套 `uniqueId`） | `dsh/DshInstanceManager.create`、`web/http/InstancesApiServlet.uniqueId` |
| `4842f45` | 运行中/停止中拒绝改名 | `dsh/DshInstanceManager.rename` |
| `e3ec345` | 自拷贝被拒绝 | `dsh/DshInstanceManager.duplicate` |
| `08e72c3`+`fdc8349`+`ad517a1` | 导出不带走链接/目录安装的插件，本机来源的插件全部点名 | `dsh/DshPluginBundle.locate`/`isLinked`/`readable`/`namesADirectory`、`dsh/DshModpacks.localPlugin`/`developmentPlugins`、`dsh/DshPluginSources.ARCHIVE` |
| `9b47a36`+`8b56869` | 插件适配判定对齐 dsh 的 semver（含 `workspace:*`、连字符区间、`~>`、`v` 前缀与 `+build`） | `dsh/DshPluginRequirements`（`satisfies` 及其全部辅助方法、`coreVersions`/`coreVersionsOf`） |
| `e146c5b` | 安装要求 pnpm 10.9 | `dsh/DshNodeRuntime.MINIMUM_PNPM`/`requirement`、`dsh/DshVersionManager.install`、`dsh/DshDoctor` |
| `8d17e2e` | lookup 命令有界运行 | `dsh/DshCommand`（带期限的 `run`、`killTree`、`Result.timedOut`） |

## 明确不搬的

| 上游 | 为什么不搬 |
|---|---|
| `8e7d21f` 技能下载源可切换 | 新产品面（新设置项 + JavaFX 设置页 + 技能市场页），不是修复；要做得单独一轮 |
| `665370c` 复制落在源实例所在目录 | 上游的「用户添加的多个实例文件夹」模型 PORT 没有：实例路径恒为 `DshPaths.instanceDirectory(id)`，即 `hdsl.home/instances/<id>` |
| CLI 重构 | PORT 已删 `DshCli` |
| `ui/**`、桌面单用户语义（HOME 锁、运行记录） | 没有对应物 |
| `3ec1edc` + `PortOccupant` | 依赖上游新加的端口占用探测类，PORT 没有这个需求 |

## 实例目录在哪：为什么那个模型不搬，跟着它来的修复却要搬

上游有「用户添加的实例文件夹」这套模型（`instance/InstanceFolders`、`InstanceStore`、
`InstanceListing`），随之而来的修复里有两条和我们这里的目录语义相关。搞清楚哪些适用：

- **实例目录只有一条路径**：`DshPaths.INSTANCES = hdsl.home/instances`（`DshPaths.java:43-46`），
  而 `hdsl.home` 由 `-Dhdsl.home` / `HDSL_USER_HOME` 决定；没设时 `web/Main.java:65-68` 把它设成
  `<HDSL_DATA>/hdsl`——容器里就是 `/data/hdsl`。面板**没有任何入口**能改它（只有 JVM 属性/环境变量）。
- 面板里能选的是**工作区**，不是实例放哪：HMCL 留下的 `GameDirectory`/`GameDirectoryManager`
  模型被改造成承载 workspace 目录（`web/config/WorkspaceDirectory.java:101-102` 添加并选中，
  `web/http/InstancesApiServlet.java:1498-1504` 解析出 workspace 传给 `create`）。dsh 的会话在
  那里读写文件，实例自身仍然在 `instances/<id>` 下。
- 所以：`665370c`（复制要落在源实例所在文件夹）**确实不适用**，`create` 连 folder 形参都没有。
- 但**跟着那个模型来的两条修复要搬，而且在这里更要紧**：`8062b6e`（改名不再删除占用新名称的
  目录）与 `66415a6`（创建不再认养已存在的目录）。差别不在「谁放的文件夹」，而在**那个目录在
  一个会被持久化、被备份恢复、被跨主机拷贝、可能 bind mount 到宿主机的东西里**——删除中断、
  面板被 `kill -9`（交接稿里那条未做项）、从别处恢复的 `/data`，都会留下「没有实例记录的目录」，
  而里面装着实例的全部（`dsh/`、`home/`、`instance.json`）。桌面版是用户看得见的文件夹，这里是
  卷里的一段残留，反而更容易无声命中。
- 因此还有一处**必须配套**的改动（本批已做）：`web/http/InstancesApiServlet.uniqueId` 原先只按
  「有没有实例记录」避重，而 `create` 现在拒绝任何已存在的目录——残留目录会让创建从「自动加
  后缀」变成失败。现在三个网页可达的 create 入口（新建实例、pack 导入两处）都排在 `uniqueId`
  之后。唯一不在其后的是 `duplicate`（PORT 没有 HTTP/UI 入口，只有测试调用它）；将来给它加入口
  时，要让它先选一个空闲名字。
- 留下的设计选择：`create` 遇到残留目录是拒绝（上游口径，面板加后缀补偿），不是「问一句要不要
  清掉」。清理实例/工作区目录这种破坏性动作不该由 create 默认做；真要提供，应当是面板上明确的
  一步。

## 搬的时候的纪律（本批踩出来的）

1. **先红后绿，而且要断言「只有这个修复才会给出的理由」**。不装包管理器的环境里，很多缺陷会以
   别的方式失败（本批实测：修复前 `release` 写越界成员时先撞上 `NoSuchFileException`），只断言
   「抛了异常」的测试两种情况下都绿，等于没有测。断言异常原因/消息里那句只有修复才会说的话。
2. **上游自己的测试也可能是空转的**，别照抄就以为有覆盖。本批两例：
   `DshPluginBundleTest` 的越界用例把载荷 `.tgz` 放在归档最前，而 `release` 一读到载荷就返回，
   越界成员根本没被读到（上游至今如此）；`ArchiveFileTreeTest` 的 zip 用例在本项目的 zip 读取器
   下不成立——kala-compress 在建树之前就滤掉了含 `..`/`\` 的名字，所以 PORT 补了一条 tar 用例。
3. **改到 PORT 主动分叉过的文件，先确认顺序语义**。`NodeRuntimeManager` 用的是 `Links.createOrCopy`，
   它在系统不允许建链接时会复制链接目标的内容——越界判定必须放在它**之前**，否则把「写到外面」
   修成「读到里面」。
4. **品牌区与 `/i/*` 反代是硬约束**（用户明确要求，本批不得影响）：
   `web/brand/**` 只依赖 `NpmRegistry`、`DshNodeRuntime`、`DshPorts`；`web/proxy/**` 只用
   `DshInstanceManager.find` 与 `DshPaths`。所以 pnpm 门槛不能加进 `canManagePlugins()`（品牌安装
   共用它），有界运行的期限不能加到 `DshNodeRuntime` 的探测上（同理）。

## 本批的验收命令

```bash
# 1) 品牌区与反代目录一个字节都没动（期望：无输出）
git diff --stat main...HEAD -- src/main/java/org/jackhuang/hmcl/web/brand src/main/java/org/jackhuang/hmcl/web/proxy

# 2) 相关套件仍全绿
./gradlew test --tests 'org.jackhuang.hmcl.web.brand.*' --tests 'org.jackhuang.hmcl.web.proxy.*' \
  --tests 'org.jackhuang.hmcl.web.ProxyBehaviorTest' --tests 'org.jackhuang.hmcl.web.zcode.*'

# 3) 静态证据：改过的领域类没有被品牌/代理引用（期望：无输出）
grep -rn "DshPluginRequirements\|DshPluginBundle\|DshModpacks" \
  src/main/java/org/jackhuang/hmcl/web/brand src/main/java/org/jackhuang/hmcl/web/proxy

# 4) 全量（照 CI）
(cd web && npm ci --no-audit --no-fund && npm run typecheck && npm run build && npm run compat) && ./gradlew --no-daemon build
```## 2026-10-25 扫描（v0.5.81）：没有要搬的

按「每月扫一遍」的流程跑了一次，结论是**这一批没有需要移植的上游改动**。

- 上游本地克隆停在 `bc60c61`（2026-10-23），并经 GitHub API 确认**就是上游最新提交**——所以不是
  "没拉到"，是真的没有更新的。（本机 `git fetch github.com` 会被掐断：`GnuTLS recv error (-110)`；
  读提交用 `gh api repos/MCXCC303/HDSL/commits` 是通的。）
- 从上一批终点（`8d17e2e` / `e146c5b`，2026-10-18~19）到 `bc60c61`，落在拷贝层且本仓库**确实还没有**
  的只有三条：

| 上游 | 内容 | 判定 |
|---|---|---|
| `b6316b7` | 新实例默认 ISOLATED HOME | **半条**：网页侧已由 `InstancesApiServlet` 硬编码 `ISOLATED` 兜住；领域层 `DshIsolationPolicy.of(null)` 与 `LauncherSettings` 默认值仍是 `WITH_PLUGINS`，而这段在 PORT 里**没有任何调用者**（死代码）。将来真把策略接上时才需要同步，届时一并改三处（含 `SettingsManager` 的 `Snapshot`） |
| `6eda067` | 密钥校验报错搬进对话框 | 适用，但只影响呈现：`dsh/DshAccount` 仍保留 `SAID_LIMIT = 120` 截断与 `vendor_said` / `not_a_listing` 旧文案 |
| `0eb4095` / `bc60c61` | `feat(plugin)` 新类（读 profile 已装插件、插件图标） | **不属于同步范围**：消费者只有上游 CLI 与 JavaFX 页；本仓库的 React 面板走自己的 `GET /api/instances/{id}/plugins` |

- 其余全是：已移植、纯结构重排（改包名 / 拆主题包）、测试与 CI、CLI 专用、JavaFX UI，或本文档
  「明确不搬的」里已列过的桌面单用户语义（`LauncherLock`、`DshRunningRecord`、`PortOccupant`、
  多实例文件夹模型）。
- 一条**残余差异**值得记着（当前无实际影响）：上游 `Semver.compare` 会剥掉 `v` 前缀与 `+build`，
  而 PORT 的 `VersionNumber`（Maven ComparableVersion）按 `+` 切分且不认 `v`。目前进
  `compareVersions` 的都是干净的 npm 版本串，所以排序一致；哪天有带 `v`/`+build` 的串进来就会不一致。
- 整合包那一块另见 [modpacks.md](modpacks.md) §4：上游 `pack/*` 与本仓库 `dsh/DshPack*` 契约同源、
  停在同一点，同样没有"等待移植"的改动。