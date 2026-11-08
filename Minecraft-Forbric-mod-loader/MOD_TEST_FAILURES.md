# Mod 兼容测试：不成功的 mod（待修）

测试日期：2026-09-26 至 2026-09-27
加载器：Forbric main `11ca1ffa`，用 `forbric-kernel-installer` 装进 Mac 官方目录 `~/Library/Application Support/minecraft`（版本 `26.2-forbric`）
下面的历史测试保留当时结果；后续修复及验证单独记录，不回写原始统计。

## 后续修复：Carpet（2026-09-30）

针对 `fabric-carpet-26.2+v260616.jar`，已接回 `fillUpdates` 的两处注入、黑石/深板岩再生，以及 Scarpet 换手与挖方块事件。保留原始 Carpet 回调与取消结果；原生换手、挖方块事件的否决仍然有效。

独立服务器行为测试共 22 项：关闭修复时 7 项通过，启用修复后严格兼容模式下 22 项全部通过，且 Carpet 的已确认兼容损失为零。另有 7 项真实字节码专项测试全部通过。覆盖规则开关、放置和邻居更新、原版流体产物、创造/生存模式的事件次数及取消效果；不代表已验证所有 Carpet 规则或扩展模组。

复现方法见 [Carpet 行为测试](forbric-kernel/canary/carpet/README.md)。


## 最新测试：3 批随机 mod，main 对比 release v0.2.0（2026-09-27）

### 方法

- 从 Modrinth 抽了 **3 批互不相同的随机 mod**。每批 30 个热门（下载量前 200 名里随机抽）+ 50 个随机（全部 26.2 mod 里随机抽），加载器随机，再加上依赖，分别是 110 / 97 / 104 个 jar。3 批之间不重复，也不和下面旧测试的那批重复。
- 每批分别用两个版本各测一轮：main `11ca1ffa`（装在官方目录）和 [release v0.2.0](https://github.com/Ray-T-r/Minecraft-Forbric-mod-loader/releases/tag/v0.2.0)（用它自己的安装器装在单独的目录里）。
- 测法和下面旧测试一样：每个 jar 单独加载，只带它必需的依赖，进同一个原版世界，截图后退出。
- v0.2.0 没有逐 mod 的加载报告，所以两个版本统一用同一个口径判定"加载成功"：进了世界、画面画出来、正常退出，**并且**日志里没有这个 mod 的入口失败、`@Mod` 构造失败或 mixin 应用失败（两个版本打的是同样的日志行）。
- 有少数 mod 缺的依赖在 Modrinth 上按 mod id 找不到，这些 mod 在两个版本里都是缺依赖状态测的。

### 成功率

| | 第 1 批 | 第 2 批 | 第 3 批 | **平均** |
|---|---|---|---|---|
| main 加载成功 | 92/110（83.6%） | 91/97（93.8%） | 93/104（89.4%） | **89.0%** |
| v0.2.0 加载成功 | 87/110（79.1%） | 83/97（85.6%） | 80/104（76.9%） | **80.5%** |
| main 能进世界 | 95/110（86.4%） | 93/97（95.9%） | 97/104（93.3%） | **91.8%** |
| v0.2.0 能进世界 | 89/110（80.9%） | 85/97（87.6%） | 85/104（81.7%） | **83.4%** |

**main 比 v0.2.0 的加载成功率高 8.5 个百分点**（80.5% → 89.0%），能进世界的比例高 8.4 个百分点。

逐个 mod 看：有 30 个在 v0.2.0 上失败、在 main 上成功；有 4 个反过来，在 v0.2.0 上成功、在 main 上失败（见下面"比 v0.2.0 差的"）。

按 main 自己更严格的加载报告算（mod 有任何部分功能未生效都不算），main 的"完全正常"平均是 79.1%（85/110、77/97、84/104）。v0.2.0 没有这个报告，没法比。

### main 上不成功的 mod（35 个）

"批次"是它在哪一批里；"v0.2.0 上"是同一个 jar 在 v0.2.0 上的结果。

| mod | 批次 | 来源 | 加载器 | 版本 | main 上 | v0.2.0 上 |
|---|---|---|---|---|---|---|
| [alexs-mobs-continued](https://modrinth.com/mod/alexs-mobs-continued) | 1 | 随机 | NeoForge | 2.2.2+26.2-neoforge | 崩溃 | 正常 |
| [better-combat](https://modrinth.com/mod/better-combat) | 1 | 热门 | NeoForge | 3.2.2+26.2-neoforge | 崩溃 | 崩溃 |
| [biomes-o-plenty](https://modrinth.com/mod/biomes-o-plenty) | 3 | 热门 | Fabric | 26.2.0.0.28 | 崩溃 | 崩溃 |
| [iris](https://modrinth.com/mod/iris) | 1 | 热门 | NeoForge | 1.11.4+26.2-neoforge | 崩溃 | 崩溃 |
| [itemglintrelight](https://modrinth.com/mod/itemglintrelight) | 3 | 随机 | Fabric | 0.3.0+26.2 | 崩溃 | 崩溃 |
| [lambdynamiclights](https://modrinth.com/mod/lambdynamiclights) | 3 | 热门 | NeoForge | 4.12.4+26.2 | 崩溃 | 崩溃 |
| [mcrpvp](https://modrinth.com/mod/mcrpvp) | 1 | 随机 | Fabric | 1.0.1 | 崩溃 | 崩溃 |
| [particle-core](https://modrinth.com/mod/particle-core) | 3 | 热门 | NeoForge | 0.3.3+26.2+neoforge | 崩溃 | 崩溃 |
| [particledrawing](https://modrinth.com/mod/particledrawing) | 1 | 随机 | NeoForge | 1.0.7-ALPHA | 崩溃 | 崩溃 |
| [player-animation-library](https://modrinth.com/mod/player-animation-library) | 1 | 依赖 | NeoForge | 1.2.6 | 崩溃 | 崩溃 |
| [sodium](https://modrinth.com/mod/sodium) | 1 | 依赖 | NeoForge | mc26.2-0.9.2-neoforge | 崩溃 | 崩溃 |
| [sodium](https://modrinth.com/mod/sodium) | 2 | 依赖 | NeoForge | mc26.2-0.9.2-neoforge | 崩溃 | 崩溃 |
| [sodium-extra](https://modrinth.com/mod/sodium-extra) | 2 | 依赖 | NeoForge | mc26.2-0.9.4+neoforge | 崩溃 | 崩溃 |
| [sodium-extra-information](https://modrinth.com/mod/sodium-extra-information) | 2 | 随机 | NeoForge | 2.9.0 | 崩溃 | 崩溃 |
| [ukulib](https://modrinth.com/mod/ukulib) | 1 | 热门 | Fabric | 2.1.1+26.2-fabric | 崩溃 | 崩溃 |
| [animated-loading-overlay](https://modrinth.com/mod/animated-loading-overlay) | 1 | 随机 | Fabric | 1.0.2 | 卡在加载画面 | 卡在加载画面 |
| [bclib](https://modrinth.com/mod/bclib) | 2 | 热门 | Fabric | 26.201.2 | 卡在加载画面 | 崩溃 |
| [bclib-neoforge](https://modrinth.com/mod/bclib-neoforge) | 3 | 依赖 | NeoForge | 26.2.3 | 卡在加载画面 | 进不了世界 |
| [drippy-loading-screen](https://modrinth.com/mod/drippy-loading-screen) | 1 | 热门 | NeoForge | 3.1.5-26.2-neoforge | 卡在加载画面 | 正常 |
| [fancymenu](https://modrinth.com/mod/fancymenu) | 1 | 依赖 | NeoForge | 3.9.12-26.2-neoforge | 卡在加载画面 | 正常 |
| [bclib](https://modrinth.com/mod/bclib) | 1 | 依赖 | Fabric | 26.201.2 | 进世界后卡住 | 崩溃 |
| [easy-magic](https://modrinth.com/mod/easy-magic) | 3 | 热门 | NeoForge | 26.2.0 | 进不了世界 | 正常 |
| [entitycrosshair](https://modrinth.com/mod/entitycrosshair) | 1 | 随机 | Fabric | 2.3.0-26.2+_fabric | 进不了世界 | 崩溃 |
| [expanded-crossbow-enchantings](https://modrinth.com/mod/expanded-crossbow-enchantings) | 1 | 随机 | Forge | 1.11.1+mod | 进不了世界 | 进不了世界 |
| [mythicquests](https://modrinth.com/mod/mythicquests) | 3 | 随机 | Fabric | 1.1.2-beta | 进不了世界 | 进不了世界 |
| [oneconfig](https://modrinth.com/mod/oneconfig) | 1 | 依赖 | Fabric | v1.2.7 | 进不了世界 | 崩溃 |
| [auto-eat](https://modrinth.com/mod/auto-eat) | 3 | 随机 | NeoForge | 1.6.7 | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [emotesounds](https://modrinth.com/mod/emotesounds) | 3 | 随机 | Fabric | 1.0.0 | 能进世界，mod 加载失败 | 崩溃 |
| [fzzy-config](https://modrinth.com/mod/fzzy-config) | 2 | 热门 | NeoForge | 0.7.7+26.2+neoforge | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [fzzy-config](https://modrinth.com/mod/fzzy-config) | 3 | 依赖 | NeoForge | 0.7.7+26.2+neoforge | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [macebot](https://modrinth.com/mod/macebot) | 2 | 随机 | Fabric | mc26.2-1.3.3-fabric | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [pigeon-chat](https://modrinth.com/mod/pigeon-chat) | 1 | 随机 | Fabric | 0.3.0+26.2 | 能进世界，mod 加载失败 | 崩溃 |
| [simpleguiapi](https://modrinth.com/mod/simpleguiapi) | 3 | 依赖 | NeoForge | 1.5.9 | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [the-elementals](https://modrinth.com/mod/the-elementals) | 1 | 随机 | Fabric | 1.4.0+fabric-26.2 | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |
| [time-weather-changer](https://modrinth.com/mod/time-weather-changer) | 1 | 随机 | Fabric | 1.2.0 | 能进世界，mod 加载失败 | 能进世界，mod 加载失败 |

### 比 v0.2.0 差的（4 个，main 失败而 v0.2.0 正常）

Alex's Mobs Continued（main 崩溃）、Drippy Loading Screen（main 卡在加载画面）、FancyMenu（main 卡在加载画面）、Easy Magic（main 进不了世界）。均为 NeoForge 版。

### 证据位置（本地，不在仓库里）

- 3 批 mod 清单：`forbric-kernel/build/sweep80-mac/v020-rounds/r1..r3/manifest.json`
- main 每轮结果：`forbric-kernel/build/sweep80-mac/per-mod-main-r1..r3/`；v0.2.0：`per-mod-v020-r1..r3/`
- 对比汇总：`forbric-kernel/build/sweep80-mac/compare.json`、`compare.txt`

---

## 发布 v0.3.0 前的闸门（2026-09-28）

全套闸门 53 个里 52 个通过，2 小时的 soak 没跑。**M9 客户端闸门不通过**：179 条检查里挂 1 条，加载报告的"可能的问题"里，fabric-api 的 `HudMixin` 那一条同样的原因写了两遍；单独重跑一次，结果一样。其余检查（进世界、渲染、正常退出等）全部通过。待修。

---

## 旧测试：同一批 mod 测 3 轮（2026-09-26 至 27，只测 main）

### 测试方法

- 从 Modrinth 选了 26.2 的 30 个热门 mod（下载量前 100 名里随机抽）和 50 个随机 mod，加载器随机（Fabric / NeoForge / MinecraftForge），再加上它们的依赖，共 101 个 jar。
- **每个 jar 单独测**，只带它自己必需的依赖。每次都用一个干净的游戏目录，进入同一个零 mod 生成的原版世界，第 100 tick 截图，第 200 tick 退出世界。
- 兼容策略设为"继续"（相当于玩家在提示窗口里点了继续）。
- 同样的测试跑了 3 轮。

"完全正常" = 进了世界、画面画出来了、正常退出，并且这个 mod 在 Forbric 加载报告里是 OK。

### 成功率

| 轮次 | 完全正常 | 能进世界 |
|---|---|---|
| 第 1 轮 | 83/101（82.2%） | 95/101（94.1%） |
| 第 2 轮 | 83/101（82.2%） | 95/101（94.1%） |
| 第 3 轮 | 83/101（82.2%） | 95/101（94.1%） |
| **平均** | **82.2%** | **94.1%** |

三轮结果完全一致，下面 18 个 jar 每轮都不成功。

按分组看（每轮相同）：热门 30 个里 23 个完全正常，随机 50 个里 43 个，依赖库 21 个里 17 个。

### 不成功的 mod

"来源"一列：热门 / 随机 = 抽中的 mod；依赖 = 被抽中的 mod 需要、从 Modrinth 依赖关系带进来的；补装依赖 = mod 自己的元数据要、但 Modrinth 上没标出来、测试时手动补上的。

#### 进不了游戏或世界（6 个）

| mod | 来源 | 加载器 | 版本 | 现象（3 轮一致） |
|---|---|---|---|---|
| [MCA Reborn](https://modrinth.com/mod/minecraft-comes-alive-reborn) | 补装依赖 | NeoForge | 8.1.11+26.2 | 卡在 Mojang 加载画面，不再前进 |
| [Supermarket Life](https://modrinth.com/mod/mca-rebornsupermarket-life)（需要 MCA Reborn） | 随机 | NeoForge | 1.0.1 | 卡在 Mojang 加载画面，不再前进 |
| [Massive Smoke Columns](https://modrinth.com/mod/massive-smoke-columns) | 随机 | NeoForge | 1.5.2 | 启动时崩溃 |
| [Essential](https://modrinth.com/mod/essential) | 热门 | Fabric | 1.5.0.1 | 启动时直接退出 |
| [Roughly Enough Items (REI)](https://modrinth.com/mod/rei) | 热门 | NeoForge | 26.2.820+neoforge | 游戏能启动，打开世界时失败（数据包加载失败） |
| [Visual Workbench](https://modrinth.com/mod/visual-workbench) | 热门 | NeoForge | 26.2.1 | 游戏能启动，打开世界时失败（数据包加载失败） |

#### 能进世界，但 mod 没加载成功（4 个）

| mod | 来源 | 加载器 | 版本 | Forbric 加载报告原文（未核实） |
|---|---|---|---|---|
| [Resourceful Config](https://modrinth.com/mod/resourceful-config) | 热门 | Fabric | 5.0.0 | did not finish loading — its main entrypoint threw |
| [andonium](https://modrinth.com/mod/andonium2) | 随机 | Fabric | 2.2.1+26.2-fabric | did not finish loading — its main entrypoint threw |
| [Knox](https://modrinth.com/mod/knox) | 随机 | Fabric | 1.0.0 | did not finish loading — its client entrypoint threw |
| [minimega](https://modrinth.com/mod/minimega) | 依赖 | Fabric | 7.1.0 | did not finish loading — its client entrypoint threw；它自带的 fantasy 部分功能未生效 |

#### 能进世界，但 mod 部分功能没生效（8 个）

| mod | 来源 | 加载器 | 版本 | Forbric 加载报告原文（未核实） |
|---|---|---|---|---|
| [fabric-api](https://modrinth.com/mod/fabric-api) | 依赖 | Fabric | 0.161.0+26.2 | 4 个模块部分未生效：fabric-block-api-v1、fabric-creative-tab-api-v1、fabric-loot-api-v3、fabric-registry-sync-v0 |
| [Architectury API](https://modrinth.com/mod/architectury-api) | 热门 | Fabric | 21.1.10+fabric | A required injector has no attachment in the actual defined class |
| [Language Reload](https://modrinth.com/mod/language-reload) | 热门 | Fabric | 1.7.7+26.2 | A required injector has no attachment in the actual defined class |
| [Physics Mod](https://modrinth.com/mod/physicsmod) | 热门 | Fabric | 3.2.4 | 3 个 mixin 被跳过：immediatelyfast.MixinSignText、liquid.MixinProgramManager、sodium.MixinVertexTransform |
| [Carpet](https://modrinth.com/mod/carpet) | 补装依赖 | Fabric | 26.2 | A required injector has no attachment；receiveFluidToBlackstone 挂在一个没有调用方的方法上 |
| [Phantom Tweaks](https://modrinth.com/mod/phantom-tweaks) | 随机 | Fabric | 1.1.3+mc26.1 | A required injector has no attachment in the actual defined class |
| [Pack Tools](https://modrinth.com/mod/pack-tools) | 随机 | NeoForge | 1.26.6.2 | GuiMixin 等 mixin 应用失败（InvalidMixinException） |
| [No Too Expensive Anvil](https://modrinth.com/mod/no-too-expensive-anvil) | 随机 | NeoForge | 1.3.1 | RenderLabelsAnvilScreenMixin 被跳过 |

### 其他测试中看到的情况（未单独复测）

- **只在整包里出现**：andonium 和整包一起加载时，服务端生成地形直接崩溃；把 andonium 拿掉后世界能生成。单独测 andonium 时能进世界（但 andonium 自己加载失败，见上表）。
- **缺依赖但照样加载了**：wcopy（chat-copy）需要 Chat Heads，hide-minimega-leaderboards 需要 Legacy4J（没有 26.2 版）。两个都没装依赖，Forbric 照样加载、进了世界、没有报错，所以算作"完全正常"。
- **Modrinth 没标出的依赖**：blockframe 需要 owo-lib、ibcarpet 需要 Carpet、Supermarket Life 需要 MCA Reborn、chunky-friends 需要 Chunky、Peterwolf's Railroads One 需要 Minecart Chain。补上后，除了带着 MCA 的 Supermarket Life，其余都完全正常。

### 证据位置（本地，不在仓库里）

- 每个 jar 每一轮的日志、加载报告、截图、崩溃报告：`forbric-kernel/build/sweep80-mac/per-mod/`、`per-mod-r2/`、`per-mod-r3/`
- 选中的 mod 清单（含版本、SHA-1、下载地址）：`forbric-kernel/build/sweep80-mac/manifest.json`
- 汇总：`forbric-kernel/build/sweep80-mac/summary.json`
