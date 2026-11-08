# worktree — 工作区仓库镜像索引

本目录以 **git submodule** 的形式收录了 `C:\Users\华硕\Documents\codex` 工作区内的
(非敏感) GitHub 仓库。**只记录指针（仓库地址 + 提交 SHA），不复制文件内容**，
因此主仓库保持轻量。

## 如何拉取

```powershell
# 拉取全部
git submodule update --init --recursive

# 只拉取某一个
git submodule update --init worktree/<name>
```

> 若直连 `github.com` 不稳定，可把子模块 URL 临时替换为镜像前缀
> （例如 `https://gh-proxy.net/https://github.com/...`）。

## 收录清单

| 目录 | 仓库 | 提交 |
|---|---|---|
| `admin-button-2` | https://github.com/i-drink-gasoline/admin-button-2 | `f6959b20f795` |
| `AdvancedReplace` | https://github.com/DeterMination-Wind/AdvancedReplace | `f6cd5f9baa43` |
| `agzam-smod` | https://github.com/Agzam4/Mindustry-mod-v8 | `85f8f37f0fe1` |
| `Arc` | https://github.com/Anuken/Arc | `68a04fab6eb7` |
| `ARC-Table` | https://github.com/DeterMination-Wind/ARC-Table | `42a77d14c442` |
| `AreaCleaner` | https://github.com/Wind-DeterMination-backup/AreaCleaner | `b3da46928984` |
| `Asthosus` | https://github.com/Catana791/Asthosus | `df92f56a456a` |
| `BEK-Tools` | https://github.com/Wind-DeterMination-backup/Neon | `496b4d4228a6` |
| `BetterBlueprints` | https://github.com/DeterMination-Wind/BetterBlueprints | `57e1a11388ca` |
| `betterHotKey` | https://github.com/DeterMination-Wind/betterHotKey | `ec9ef50c8836` |
| `betterLogisticsSpeed` | https://github.com/DeterMination-Wind/betterLogisticsSpeed | `d58e7cc83b5e` |
| `BetterMapEditor` | https://github.com/DeterMination-Wind/BetterMapEditor | `f6e5eb8ca002` |
| `betterMiniMap` | https://github.com/DeterMination-Wind/betterMiniMap | `f52812cc80cb` |
| `BetterPolyAi` | https://github.com/DeterMination-Wind/BetterPolyAi | `39a2e1687477` |
| `BetterProjectorOverlay` | https://github.com/DeterMination-Wind/BetterProjectorOverlay | `81f51833ee78` |
| `BetterRTSFormation` | https://github.com/DeterMination-Wind/BetterRTSFormation | `6db2a0769f60` |
| `BetterSchematicName` | https://github.com/DeterMination-Wind/BetterSchematicName | `ad84398556dc` |
| `BetterScreenShot` | https://github.com/DeterMination-Wind/BetterScreenShot | `94a4632a5f08` |
| `BetterTerrainGen-V2` | https://github.com/DeterMination-Wind/BetterTerrainGen-V2 | `07924fc04155` |
| `Color-the-duct-remote-inspect` | https://github.com/Wind-DeterMination-backup/Color-the-duct | `f9c44dc74c90` |
| `Color-the-ducts` | https://github.com/DeterMination-Wind/Color-the-duct | `bcbbdf287dcf` |
| `customMarker` | https://github.com/Wind-DeterMination-backup/customMarker | `ba376a754092` |
| `desktop` | https://github.com/TurboWarp/desktop | `62817d5431c0` |
| `DpsHeatmap` | https://github.com/Dustdustry/DpsHeatmap | `0a617acbacef` |
| `ForeignServerTranslator` | https://github.com/DeterMination-Wind/ForeignServerTranslator | `3d7d8d8081d1` |
| `Helium` | https://github.com/EB-wilson/Helium | `df86357852fc` |
| `HideWhatProcessorsShow` | https://github.com/DeterMination-Wind/HideWhatProcessorsShow | `744f3fb06690` |
| `learn-mindustry-logic` | https://github.com/A4-Tacks/learn-mindustry-logic | `62c0c2438a54` |
| `learn-mindustry-mod.github.io-kt-snippets-20260428` | https://github.com/DeterMination-Wind/learn-mindustry-mod.github.io | `f853936818ec` |
| `learn-mindustry-mod.github.io-pr3-20260428` | https://github.com/DeterMination-Wind/learn-mindustry-mod.github.io | `f853936818ec` |
| `LineOverwrite` | https://github.com/DeterMination-Wind/SmartPlacement | `8eed10bf665d` |
| `LockAttack` | https://github.com/DeterMination-Wind/LockAttack | `c4db4cb0dca2` |
| `logic-assist` | https://github.com/nosbhghggg/logic-assist | `4ae32c72cafe` |
| `LogicSugar` | https://github.com/DeterMination-Wind/LogicSugar | `2536bbf633f9` |
| `MI2-Utilities-Java` | https://github.com/BlackDeluxeCat/MI2-Utilities-Java | `015f3647a9cd` |
| `mindustry_logic_bang_lang` | https://github.com/A4-Tacks/mindustry_logic_bang_lang | `a6739e9bf18f` |
| `mindustry-client` | https://github.com/mindustry-antigrief/mindustry-client | `d54e8066e836` |
| `Mindustry-issue-6515` | https://github.com/Anuken/Mindustry | `254fd3e28d3a` |
| `Mindustry-master` | https://github.com/Anuken/Mindustry | `254fd3e28d3a` |
| `Mindustry-Wiki-Generator` | https://github.com/Anuken/Mindustry-Wiki-Generator | `698f0ea5c1dd` |
| `MindustryJavaModTemplate` | https://github.com/Anuken/MindustryJavaModTemplate | `37cea2b2006f` |
| `MindustryX-main` | https://github.com/TinyLake/MindustryX | `92471dd2224b` |
| `MlogBlockly` | https://github.com/DeterMination-Wind/MlogBlockly | `75fe859040df` |
| `MlogChecker` | https://github.com/DeterMination-Wind/MlogChecker | `22059895c6e9` |
| `MlogCopilot` | https://github.com/DeterMination-Wind/MlogCopilot | `5621c5ee7469` |
| `MlogScratchStudio` | https://github.com/DeterMination-Wind/MlogScratchStudio | `71963882f3e4` |
| `MlogScratchTW-newTui` | https://github.com/DeterMination-Wind/MlogScratchTW | `dd3ecfa9e777` |
| `MlogSugar` | https://github.com/DeterMination-Wind/MlogSugar | `f0815bef2e5e` |
| `MobileJoystickControl` | https://github.com/DeterMination-Wind/MobileJoystickControl | `bffcb2dd429c` |
| `MonoPolySelect` | https://github.com/DeterMination-Wind/MonoPolySelect | `8185f432cada` |
| `MsavStudio` | https://github.com/DeterMination-Wind/MsavStudio | `efc1a054d033` |
| `Neon` | https://github.com/DeterMination-Wind/Neon | `2d7f72f36d67` |
| `Neon-wt-backup-20260722` | https://github.com/DeterMination-Wind/Neon | `2d7f72f36d67` |
| `Neon-wt-overlaycompat-fix` | https://github.com/DeterMination-Wind/Neon | `2d7f72f36d67` |
| `NewHorizonMod` | https://github.com/Yuria-Shikibe/NewHorizonMod | `d34eb9ffbb92` |
| `OverlayCompatBridge` | https://github.com/DeterMination-Wind/OverlayCompatBridge | `34b8a5f6c867` |
| `OverlayCompatBridge-wt-neon-overlay-fix` | https://github.com/DeterMination-Wind/OverlayCompatBridge | `34b8a5f6c867` |
| `OvulamTools` | https://github.com/Ovulam5480/OvulamTools | `e39d9dddf057` |
| `PatchEditor` | https://github.com/Dustdustry/PatchEditor | `da610e2cd405` |
| `PatchViewer` | https://github.com/DeterMination-Wind/PatchViewer | `da4d38fa4668` |
| `PatchViewer-wt-vanilla-stats` | https://github.com/DeterMination-Wind/PatchViewer | `da4d38fa4668` |
| `PatchViewer2` | https://github.com/DeterMination-Wind/PatchViewer | `da4d38fa4668` |
| `PatrolCancel` | https://github.com/DeterMination-Wind/PatrolCancel | `01700a8755c5` |
| `pi-zh-pi-coding-agent` | https://github.com/509992828/pi-zh-pi-coding-agent | `127cbfe7fed8` |
| `PinyinSearchSupport-repo` | https://github.com/DeterMination-Wind/PinyinSearchSupport | `11c040ca1b3f` |
| `PinyinSerchSupport` | https://github.com/Wind-DeterMination-backup/PinyinSearchSupport | `4d4a8697c17d` |
| `Power-Grid-Minimap-repo-clone` | https://github.com/DeterMination-Wind/Power-Grid-Minimap- | `966244bdfc37` |
| `Ra2Announcer` | https://github.com/DeterMination-Wind/Ra2Announcer | `04c66734af5a` |
| `Radial-Build-Menu-hud` | https://github.com/DeterMination-Wind/Radial-Build-Menu-hud- | `8409a4a0bee2` |
| `Random` | https://github.com/DeterMination-Wind/Random | `195d6bf7f965` |
| `Saturation-Firepower` | https://github.com/RA2EXE/Saturation-Firepower | `db983066cd47` |
| `SchemeSaver` | https://github.com/Wind-DeterMination-backup/SchemeSaver | `8f9defe4c419` |
| `scratch-gui-fork-temp` | https://github.com/DeterMination-Wind/VisualMlog | `a3f270118adf` |
| `StealthPath` | https://github.com/DeterMination-Wind/StealthPath | `fd738047b75d` |
| `StealthPath-threat-model` | https://github.com/DeterMination-Wind/StealthPath | `fd738047b75d` |
| `tmp-Arc-origin-master` | https://github.com/Anuken/Arc | `68a04fab6eb7` |
| `tmp-mindustry-origin-master` | https://github.com/Anuken/Mindustry | `254fd3e28d3a` |
| `TooManyItems` | https://github.com/EB-wilson/TooManyItems | `dccf86bed7e7` |
| `Tripwire` | https://github.com/DeterMination-Wind/Tripwire | `b622e36bef06` |
| `UpdateScheme` | https://github.com/Wind-DeterMination-backup/UpdateScheme | `8de9c5b44f2b` |
| `uuidManager_repo` | https://github.com/Wind-DeterMination-backup/uuidManager | `2e89f1bbe393` |
| `WhoUsesThisBuilding` | https://github.com/DeterMination-Wind/WhoUsesThisBuilding | `80c6fd3bae33` |
| `wiki` | https://github.com/MindustryGame/wiki | `d33d80c488e6` |
| `Xenon-fork` | https://github.com/DeterMination-Wind/Xenon | `c17dd0ed033d` |
| `ZalithLauncher2` | https://github.com/DeterMination-Wind/Xenon-Mobile | `b6393ef6d29f` |

## 明确排除（敏感 / 特殊）

| 目录 | 原因 |
|---|---|
| `hzx-chat` | 用户明确要求排除 |
| `ServerPlayerDataBase` / `ServerPlayerDataBase_git` / `ServerPlayerDataBase_release_repo` | 含玩家聊天数据库（`chats.sqlite`）等个人数据 |

- `Mindustry-v157` 无 origin 远端，无法作为子模块引用，未收录。