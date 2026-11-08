# GTNH 知识库导读（先看这个）

本文件不是攻略，而是**导航**：告诉你要查的问题在知识库的哪个角落。所有内容都能用 `kb\kb.cmd <关键词>`（或根目录 `q.cmd <关键词>`）直接搜到。

---

## 零、三种用法

```powershell
cd <知识库目录>

.\search.cmd 真空冷冻机              # 1. 关键词检索（不知道确切名字就随便试）
.\search.cmd --exact 钨              # 2. 查目录：列出所有标题含该词的页面
type kb\TOPIC_INDEX.md           # 3. 按主题翻索引（推荐第一次用先扫这份）
```

* 想按主题找入口 → `kb\TOPIC_INDEX.md`（8 个主题组）
* 想按名字找条目 → `kb\TITLE_INDEX.md`（12290 条，中文区 + 英文区）
* 想知道数据从哪来 → `kb\INDEX.md`
* 想让 AI 回答 → `.\q.cmd --ask "问题"`

---

## 一、知识库里有什么

| 内容 | 覆盖度 | 检索前缀 |
|---|---|---|
| 维基正文（材料 / 机器 / 多方块 / 教程 / 产线 / 版本） | 6305 页，2.8.4 稳定版为主，部分 2.9.0 前瞻 | `wiki.main` |
| 维基数据表（机器参数等 tabx 导出） | 896 页 | `wiki.data` |
| 游戏内任务书全文 | 3805 个任务 / 48 条任务线，Daily 704 原始英文 | `quest.*` |
| config 参考值 | 机器参数、污染、矿脉生成、危险物品、自定义提示 | `ref.*` |
| 物品名映射 | 25846 条内部 ID ↔ 名称（优先中文） | `--item` |

---

## 二、按需求找入口

### 1. 我想知道"这个阶段该干什么"
* 主线顺序：任务书 `And So, It Begins` → `Tier 0 - Stone Age` → `Tier 0.5 - Steam` → `Tier 1 - LV` → … → `Tier 12 - UMV`
* 每个阶段的**成体系教程**（维基正文，中文，推荐先读）：
  * 石器时代 → `python kb\search.py 石器时代教程`
  * 蒸汽时代 → `python kb\search.py 蒸汽时代教程`
  * 低压（LV） → `python kb\search.py 低压时代教程`
  * 更高阶段 → `python kb\search.py "教程导读"` 或 `python kb\search.py "各阶段机器及教程"`
* 阶段总览页：`python kb\search.py 阶段 --top 10`

### 2. 我想知道"某个东西是什么 / 怎么造"
* 材料类：`python kb\search.py <材料名>`（页面含"最早出现阶段、成分、获取、用途、材料属性"）
  例：`python kb\search.py 铋青铜 --full`
* 机器类：`python kb\search.py <机器名>`（页面含"电压等级、结构大小、方块数、仓室、并行、超频种类"）
  例：`python kb\search.py 巨型真空冷冻机 --full`
* 物品中文/英文/内部 ID 互查：`python kb\search.py --item <名称片段>`

### 3. 我想知道"这条产线怎么做"
维基里成文的工艺流程（教程类页面）：`python kb\search.py 铂处理`、`python kb\search.py 钛处理`、`python kb\search.py 硅`、`python kb\search.py 稀土`
> 提示：这类页面标题常带"处理 / 线 / 产线"，直接搜元素名成功率最高。

### 4. 机制类（最容易踩坑的地方，知识库覆盖很好）
| 主题 | 查询 |
|---|---|
| 电压等级与 EU 体系、线损、变压器 | `python kb\search.py 电力 --ns wiki.main --full` |
| 超频 / 并行 / 升压 / 额定功率 | `python kb\search.py 无损超频 --full` |
| 污染（排放、扩散、消声仓、清洗机） | `python kb\search.py 污染`；数值 `kb\ref\GT_Pollution.cfg` |
| 机器基础参数倍率 | `kb\ref\GT_MachineStats.cfg` |
| 矿脉生成与探矿 | `python kb\search.py 矿脉`、`python kb\search.py 地震勘探者`；`kb\ref\GT_WorldGeneration.cfg` |
| 危险物品（辐射/毒性） | `kb\ref\GTNH_HazardousItems.xml` |
| 多方块结构怎么搭 | `python kb\search.py 多方块结构的搭建和使用 --full` |
| 多方块仓室上限 | `python kb\search.py 多方块机器功能性仓室最大可用数量参考` |

### 5. 我想查任务书原话 / 奖励 / 提交物
* 按任务线看全文：直接打开 `kb\quests\line_<任务线>.txt`
* 按关键词搜：`python kb\search.py 匠魂 熔炉 --ns "quest.Tier 0 - Stone Age"`
* 每条任务块格式：`### [任务ID] 名称` + 英文描述 + `[提交]/[合成]/[击杀]` 物品 + `> 奖励`

### 6. 我想让 AI 回答
```powershell
python kb\search.py --ask "钨矿处理需要什么机器和阶段" > ctx.txt
# 把 ctx.txt 内容 + 你的问题一起发给 AI
```
或直接在本会话问「查知识库：……」。

---

## 三、领域缩写与任务线对照（常见困惑）

| 缩写 | 含义 | 对应任务线 |
|---|---|---|
| ULV/LV/MV/HV/EV/IV/LuV/ZPM/UV/UHV/UEV/UIV/UMV | 电压等级：极低压→低压→中压→高压→超高压→绝缘→ ludicrous→ZPM→极限→…→终极 | `Tier 0`–`Tier 12` |
| GT5U | GregTech 5 Unofficial（核心模组） | — |
| NEI | NotEnoughItems，配方查询 | — |
| BQ | BetterQuesting 任务书 | 全部 `quest.*` |
| AE2 | Applied Energistics 2 物流/存储 | `Applied Energistics` |
| SFM | Steve's Factory Manager | `SFM and Computers` |
| MT/多方块 | Multiblock 多方块机器 | `Multiblock Goals` |
| 消声仓 | Muffler Hatch，减污染 | — |
| 蒸汽 / 半流质 / 燃气 / 等离子 | 不同发电路线 | `How to Generate Power` |

非主线专题线（想深入某方向就查这些）：
`Mass Processing`（批量加工）、`Multiblock Goals`（多方块）、`Storing and Transforming EU`（储电与变压）、`Working with Oil`（石油）、`Bio for the Masses`（生物燃料）、`Powerful Nuclear Physics`（核物理）、`Space Race`（太空）、`Basic Automation` / `Handle Logistics with Pipes` / `Applied Energistics`（自动化物流）、`How to Be(e)` / `Be(e) Breeding`（养蜂）、`Forestry and Multifarms`（林业与农场）、`The Green Revolution`（作物）、`Novice/Adept Thaumaturgy` / `Kaaami, Haaaami` / `Focus on Wand Foci & EMT`（神秘时代魔法线）、`Hardcore End(er) Expansion`（HEE）、`Getting Around...` / `...Without Dying`（交通与生存）、`Building Better Bases`（基地建设）、`Tips and Tricks`（技巧）、`Endgame Goals`（终局）。

---

## 四、边界与注意事项

* 维基镜像内容以 **2.8.4 稳定版**为主，少数页面标注 2.9.0/2.8.0，页面开头会写明适用版本；整合包本体是 **Daily 704（2.9.x 线）**，若版本有差异，以 `quest.*`（任务书随包更新）与 `ref.*`（config 原值）为准。
* 维基里大量数值表格是脚本渲染的（页面里出现 `加载合成表模板...` 之类字样时），这类页面的**配方数字**可能抽取不全 → 配方请以游戏内 NEI 为准，知识库用于定位"该做什么、需要什么机器、什么阶段"。
* 任务书描述是英文原文，未翻译；检索支持中文关键词是因为物品名映射与页面标题是中文，任务描述本身需用英文关键词命中。
