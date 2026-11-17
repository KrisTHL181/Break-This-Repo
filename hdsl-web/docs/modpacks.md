# 整合包（.dspack）：我们实现的是哪一版契约

这一篇记录 HDSL-web 的整合包实现**与 DSH-PackForge 规范的对齐点、刻意不做的部分，以及为什么**。
用户视角的用法见 README；改这条线之前先读这一篇。

规范仓库：[DSH-PackForge](https://github.com/DSH-PackForge/DSH-PackForge)（MIT），现行版本
**manifest v5** / **pack-structure v3（`.dspack`）** / **index `schemaVersion 2`**；历史版本
「只标记、不删除」，启动器需**向后兼容导入**。本地克隆在 `../DSH-PackForge`（`specs/` 是契约，
`docs/launcher-integration.md` 是启动器集成指南）。

## 1. 现在支持什么

| 能力 | 位置 | 状态 |
|---|---|---|
| 写 `.dspack` 容器 v3 + manifest v5（`type:"profile"`） | `dsh/DshPackForge` | 现行主线 |
| 读容器 3↔manifest 5、容器 2↔manifest 4 | `dsh/DshPackInstaller.containerFor` | ✅ |
| 读 `.tgz`（pack-structure v1）+ manifest v2/v3 的兼容导入 | 同上（gzip 魔数识别 → tar 读取） | ✅ |
| manifest v1 拒绝 | 同上 | ✅（并说清它被压平了什么） |
| index `schemaVersion` 硬校验（= 2） | `dsh/DshPackMarket` | ✅ |
| 导出 `home/` 白名单（全局 `skills/`、`.agent-presets/`、`AGENTS.md`） | `DshPackForge.export(…, homeInclude, …)` | ✅ 不勾选则什么都不随包走 |
| `dshVersions` 兼容集 | 导出写实测集；导入做 `∩ 本机已装` 的确定性选择 | ✅ |
| `launchers` 声明 | `DshPackInstaller.launcherClaims/launcherWarnings` | ✅ 只读 + 警告放行 |
| 导出/导入的凭据与越界过滤 | `DshPackForge.DENY_*`、`DshPackInstaller.refuseIfUnsafe` | ✅ 两侧各守一半（导出守作者，导入守本机） |

**装的时候选哪个版本**（`pack-structure/v3.md §9.1(2)`）：先取 manifest 的 `dshVersions` 与本机已装
版本的交集，命中多个时优先 manifest 自己的 `dshVersion`、其次取交集里最新的；没有交集就退回 pin 的
版本（去装）；什么都没声明就用本机最新。**绝不因为 pin 的那个版本本机没有就整包失败。**

**`launchers` 怎么判**（`manifest/v5.md §13` + `launcher-registry.md`）：本启动器的 canonical id 是
**`hdsl`**（别和 PCL 系的 `dshl` 混——一个字母之差）。判定按 `pack-structure/v3.md §9.1(1)`：
**只警告放行，绝不硬拒**；版本比较是逐段数值（缺段视为 0），不做预发布语义。

## 2. 刻意不做的

- **导出不写 `launchers`**：HDSL 导出的包保持"通用包"，谁都能装。读侧完整实现（否则别人的包声明
  了支持范围我们看不见）。
- **`dshhome` 形态仍被 web API 拒绝**：导入侧能识别（`Container.wholeHome`），但安装/列表/导出的
  三条路径都显式拒绝——市场会列出这类包、点安装才报错，这是已知的不体面处，宁可不装也不要装错。
- **`files[]`（重内容指针）与 `skills[]` 未实现**：两侧都没有下载/校验逻辑。需要外置大资产时再做。
- **`.dshpkcfg` 工作区快照未实现**：导出参数目前每次现填，不落盘。

## 3. 三条容易踩的边界

- **导出侧的白名单是"显式才走"**：`export(..., homeInclude)` 传空集合时 `home/` 一个条目都不写。
  这不是遗漏，是决策——全局内容可能含凭据气味的东西，默认不外流。
- **`.tgz` 的成员名同样要过安全过滤**：老格式没有 `overrides/` 概念（压平到 profile 根），但
  `refuseIfUnsafe` 与凭据/嵌套压缩包过滤一样要跑。
- **`dshVersions` 里必须含 pin 的版本**（规范 §12）：所以导出时实例自身版本永远在首位，作者额外
  加的版本只能往后排、且去重、非版本字符串丢弃（并说明丢弃了）。

## 4. 上游对照

上游 HDSL 的 `com.mcxcc303.hdsl.pack.*`（`DshPackForge` / `DshPackMarket` / `DspackContainer` /
`DspackLander` / `DspackSafety`）与本仓库的 `dsh/DshPack*` **契约上同源、停在同一点**：同样写
manifest v5 / 容器 3，同样不写 `files`/`icon`/`dshVersions`/`launchers`/`home/`。所以这一块**没有
"等待移植"的上游改动**；本仓库反而多了 G5/G8/G9/G1·G2 四项。

## 5. 验证入口

```bash
./gradlew --no-daemon test --tests 'org.jackhuang.hmcl.dsh.DshPack*' \
  --tests 'org.jackhuang.hmcl.web.PackApiTest'
```

`DshPackMarketTest` 覆盖老契约（真 tar.gz 的 identify/land/list、凭据拒绝、v1 拒绝理由、
`dshVersions` 选择、`launchers` 五行判定表、`dshl` ≠ `hdsl`）；`PackApiTest` 覆盖 HTTP 面
（导出 choices、白名单真的随包走、上传 `.tgz`、不支持本启动器时仍然安装并说明原因）。