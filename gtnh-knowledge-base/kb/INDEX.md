# GTNH 知识库总索引

本文件由 `_tools/build_index_md.py` 自动生成，列出知识库全部可检索内容。

## 一、任务书（游戏内 BetterQuesting 全量任务）

来源：`GT New Horizons daily/.minecraft/config/betterquesting/DefaultQuests`（3805 个任务，pack_version 2141）。
检索前缀：`quest.<任务线名>`，例如 `--ns quest.Tier1LV`。

| 任务线（文件） | 任务数 | 说明 |
|---|---|---|
| `line_extra_MultipleQuestLine.txt` | 196 | ### [2888] §4§l「 」 |
| `line_Be(e) Breeding.txt` | 188 | Be(e) Breeding |
| `line_Applied Energistics.txt` | 184 | Applied Energistics |
| `line_The Green Revolution.txt` | 162 | The Green Revolution |
| `line_Tier 1 - LV.txt` | 143 | Tier 1 - LV |
| `line_Tier 2 - MV.txt` | 141 | Tier 2 - MV |
| `line_Space Race.txt` | 140 | Space Race |
| `line_How to Generate Power.txt` | 136 | How to Generate Power |
| `line_Tier 5 - IV.txt` | 130 | Tier 5 - IV |
| `line_Multiblock Goals.txt` | 116 | Multiblock Goals |
| `line_Tier 3 - HV.txt` | 108 | Tier 3 - HV |
| `line_Novice Thaumaturgy.txt` | 102 | Novice Thaumaturgy |
| `line_Tier 0.5 - Steam.txt` | 99 | Tier 0.5 - Steam |
| `line_...Without Dying.txt` | 98 | ...Without Dying |
| `line_Look to the Edges.txt` | 92 | Look to the Edges |
| `line_Tier 4 - EV.txt` | 85 | Tier 4 - EV |
| `line_Kill All the Things.txt` | 83 | Kill All the Things |
| `line_How to Be(e).txt` | 82 | How to Be(e) |
| `line_Building Better Bases.txt` | 81 | Building Better Bases |
| `line_Tier 6 - LuV.txt` | 77 | Tier 6 - LuV |
| `line_Adept Thaumaturgy.txt` | 76 | Adept Thaumaturgy |
| `line_Paying the Highest Price.txt` | 73 | Paying the Highest Price |
| `line_Tier 0 - Stone Age.txt` | 71 | Tier 0 - Stone Age |
| `line_Forestry and Multifarms.txt` | 67 | Forestry and Multifarms |
| `line_extra_NoQuestLine.txt` | 66 | ### [2159] Bees Template |
| `line_Mass Processing.txt` | 65 | Mass Processing |
| `line_Getting Around....txt` | 64 | Getting Around... |
| `line_Tier 7 - ZPM.txt` | 64 | Tier 7 - ZPM |
| `line_Storing and Transforming EU.txt` | 62 | Storing and Transforming EU |
| `line_Powerful Nuclear Physics.txt` | 56 | Powerful Nuclear Physics |
| `line_Tier 10 - UEV.txt` | 54 | Tier 10 - UEV |
| `line_SFM and Computers.txt` | 53 | SFM and Computers |
| `line_Bio for the Masses.txt` | 47 | Bio for the Masses |
| `line_Feeding Yourself.txt` | 47 | Feeding Yourself |
| `line_Tier 11 - UIV.txt` | 45 | Tier 11 - UIV |
| `line_Tier 12 - UMV.txt` | 44 | Tier 12 - UMV |
| `line_Working with Oil.txt` | 44 | Working with Oil |
| `line_Flower Power.txt` | 43 | Flower Power |
| `line_Tier 8 - UV.txt` | 42 | Tier 8 - UV |
| `line_Hardcore End(er) Expansion.txt` | 40 | Hardcore End(er) Expansion |
| `line_Focus on Wand Foci & EMT.txt` | 39 | Focus on Wand Foci & EMT |
| `line_Kaaami, Haaaami, ... HA!.txt` | 36 | Kaaami, Haaaami, ... HA! |
| `line_Basic Automation.txt` | 34 | Basic Automation |
| `line_Tier 9 - UHV.txt` | 34 | Tier 9 - UHV |
| `line_And So, It Begins.txt` | 32 | And So, It Begins |
| `line_Handle Logistics with Pipes.txt` | 30 | Handle Logistics with Pipes |
| `line_Endgame Goals.txt` | 29 | Endgame Goals |
| `line_Tips and Tricks.txt` | 5 | Tips and Tricks |

## 二、离线维基（灰机 wiki 镜像，2.8.4 版内容）

来源：`gtnh-wiki-archive/out/pages`（原 HTML 全量保留），已抽取为可检索 JSONL：`kb/wiki/ns_*.jsonl`。
检索前缀：`wiki.<命名空间>`，例如 `--ns wiki.main`（正文）、`--ns wiki.data`（数据表）。

| 文件 | 页数 | 说明 |
|---|---|---|
| `ns_category.jsonl` | 91 | 分类页 |
| `ns_data.jsonl` | 896 | 数据表（tabx）：机器参数、配方数据等批量表格 |
| `ns_form.jsonl` | 16 | 表单定义 |
| `ns_gadget.jsonl` | 86 | 小工具源码（代码展示、正则生成器） |
| `ns_help.jsonl` | 1 | 帮助页 |
| `ns_html.jsonl` | 26 | HTML 元素文档 |
| `ns_main.jsonl` | 6305 | 正文条目：材料/机器/多方块/教程/产线/版本 |
| `ns_mediawiki.jsonl` | 22 | 系统页面 |
| `ns_module.jsonl` | 175 | Lua 模块源码说明 |
| `ns_project.jsonl` | 64 | 维基项目页（编写规范、版权） |
| `ns_property.jsonl` | 5 | 语义属性定义 |
| `ns_template.jsonl` | 791 | 模板 |

### 2.1 正文重点条目（自动筛选）

**教程/攻略类**（158 条，节选）

GTNL_进阶超能硅岩反应堆、IC门教程、Matter_Manipulator_使用教程、Matter_Manipulator_（MM）使用教程、亚稳态黑洞遏制场自动维持教程、低压时代教程、元始手套使用教程、半稳定反物质数据研究及路线指导、各阶段机器及教程、培养灵气节点教程、基于村民交易的蒸汽阶段神秘时代教程、基于超吟附魔台的突破附魔等级上限教程、复制类AE样板制作教程、大型涡轮机发电机制指南、官服游玩指南、封装进阶贴片二极管、封装进阶贴片晶体管、封装进阶贴片电容、封装进阶贴片电感、封装进阶贴片电阻、崩溃报告分析教程、开放式电脑_快速入门、开放式电脑（OpenComputers）实验教程、教程导读、新手建议、新手贡献指南、新手魔法能源吸收器、新手魔法能源转换器、物质操纵者使用教程、特大涡轮机发电机制指南、石器时代教程、矿典过滤卡的教程与使用、神秘时代基础教程、神秘时代进阶教程、蒸汽时代教程、论新手如何开始养蜂、论新手如何开始养蜂_历史、进阶UU增幅液生产器、进阶世界加速器、进阶两极磁化机、进阶作物合成机、进阶作物合成机_V、进阶作物合成机_VI、进阶作物合成机_VII、进阶作物合成机_VIII、进阶作物基因提取机、进阶作物基因提取机_V、进阶作物基因提取机_VI、进阶作物基因提取机_VII、进阶作物基因提取机_VIII、进阶作物复制机、进阶作物复制机_V、进阶作物复制机_VI、进阶作物复制机_VII、进阶作物复制机_VIII、进阶兰波顿电池背包、进阶内燃发电机、进阶冲压机床、进阶化学反应釜、进阶化学浸洗机、进阶半流质发电机、进阶卷板机、进阶压模机、进阶压缩机、进阶发酵槽、进阶合金炉、进阶回收机、进阶地震勘探者、进阶地震勘探者_EV、进阶地震勘探者_HV、进阶地震勘探者_LV、进阶地震勘探者_MV、进阶复制机、进阶太阳能板、进阶存储输入仓(ME)、进阶存储输入仓室、进阶存储输入仓（ME）、进阶存储输入总线(ME)、进阶存储输入总线（ME）、进阶微波炉、进阶打包机、进阶打印机、进阶打印机_V、进阶打印机_VI、进阶打印机_VII、进阶扫描仪、进阶提取机、进阶搅拌机、进阶敌对生物驱逐器、进阶敌对生物驱逐器_V、进阶敌对生物驱逐器_VI、进阶敌对生物驱逐器_VII、进阶数据访问仓、进阶板材切割机、进阶污染清洗机、进阶泵、进阶洗矿厂、进阶流体加热器、进阶流体固化器、进阶流体提取机、进阶流体灌装机、进阶消声仓、进阶混凝土回填机、进阶火箭引擎、进阶热力离心机、进阶燃气轮机、进阶电力喷气背包、进阶电动唱片机、进阶电弧炉、进阶电炉、进阶电烤炉、进阶电磁离析机、进阶电解机、进阶电路基板、进阶电路组装机、进阶电路组装机_V、进阶电路组装机_VI、进阶电路组装机_VII、进阶研磨机、进阶碎石机

**产线/工艺流程**（19 条，节选）

LuV阶段适用的百万级岩浆发电产线、下单产线、产线、亿级高辛烷值汽油产线、净化水产线、十万级高辛烷值汽油产线、大型源质发电产线、工艺流程、工艺流程与线性代数、工艺流程图、工艺流程计算器、氟产线、氟钍反应堆燃料产线、氢产线、氧产线、火箭燃料产线、环氧树脂-高十六烷柴油联合产线、硅氧产线、铟产线

**多方块结构**（18 条，节选）

LuV后UHV前多方块机器通用下单结构、在线结构工作台、基于季节变迁改变群系捕捉贫铀合金结构的方案、基于巫术季节变迁改变群系捕捉贫铀合金结构量产铀238和钛的方案、基于空间塔的鸿蒙结构共用、多方块机器、多方块机器全息投影仪、多方块机器共用、多方块机器功能性仓室最大可用数量参考、多方块电力机器、多方块结构、多方块结构中英对照表、多方块结构的搭建和使用、多方块蒸汽机器、应用能源2_自动合成的原材料发配结构、林业与多方块农场、结构拆解扳手、结构玻璃


## 三、参考数据（config 原文件副本）

| 文件 | 大小 | 内容 |
|---|---|---|
| `GTNH_CustomToolTips.xml` | 20 KB | GTNH 自定义物品提示 |
| `GTNH_HazardousItems.xml` | 3 KB | 危险物品（辐射/毒性）定义 |
| `GTNH_dreamcraft.cfg` | 6 KB | GTNH 核心模组 dreamcraft 配置 |
| `GT_MachineStats.cfg` | 7 KB | GregTech 机器参数（电压/耗时/功耗倍率） |
| `GT_Pollution.cfg` | 17 KB | 污染机制参数（排放、扩散、效果阈值） |
| `GT_WorldGeneration.cfg` | 1 KB | 矿脉世界生成开关与参数 |
| `changelog_703_to_704.md` | 3 KB | Daily 703 → 704 变更日志 |

## 四、物品名映射

`kb/data/item_names.tsv`：25846 条 `内部ID → 名称`（从 248 个模组 jar 的 lang 文件中提取，优先中文）。
用法：`python kb/search.py --item 真空冷冻机`（反查内部 ID）。

## 五、检索用法速查

最省事的方式（工作区根目录，自动处理中文编码）：

```powershell
cd <知识库目录>
.\search.cmd 真空冷冻机                # = kb\kb.cmd，等价于 python kb\search.py
.\search.cmd --exact 钨                # 只看标题命中的条目（查目录）
```

等价的 python 调用：

```powershell
$env:PYTHONIOENCODING="utf-8"

python kb\search.py 真空冷冻机            # 按关键词搜（中文/英文/ID 均可）
python kb\search.py "钨 采矿场" --top 8    # 多关键词
python kb\search.py 超频 --ns wiki.main    # 只看维基正文
python kb\search.py 多方块 --ns quest.Multiblock\ Goals
python kb\search.py 污染 --full            # 输出完整命中段落
python kb\search.py --item 真空冷冻机       # 物品名反查内部 ID
python kb\search.py --ask "怎么处理钨矿"    # 生成给 AI 的上下文包
python kb\search.py --stats               # 知识库统计
python kb\search.py --rebuild             # 数据更新后重建索引
```

索引缓存：`kb/data/index.pkl`（首次 ~20 秒建立，之后查询 <2 秒；新增/修改数据后需 `--rebuild`）。
