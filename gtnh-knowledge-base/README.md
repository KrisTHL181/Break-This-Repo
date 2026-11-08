# GT New Horizons 离线知识库

把 **GT New Horizons（GTNH）** 的中文维基、游戏内任务书、整合包 config 参考值抽取成一套
可离线检索的数据，附带命令行检索器和一个纯文字 Web 站点。

> ⚠️ **本包不附带任何 Minecraft / 模组游戏素材**（贴图、模型、音效、jar、字体）。
> 所有图片文件均已剔除，站点会显示透明占位图。
> 剔除范围与原因见 **[COMPLIANCE.md](COMPLIANCE.md)**。

---

## 1. 快速开始

只依赖 **Python 3.8+ 标准库**，无需 pip 安装任何东西。

### 命令行检索（开箱即用）

```bash
python kb/search.py 真空冷冻机            # 关键词检索
python kb/search.py --exact 钨            # 只列标题含该词的页面
python kb/search.py 铂处理 --ns wiki.main # 限定来源
python kb/search.py --item Vacuum         # 物品名 → 内部 ID（中英模糊）
python kb/search.py --ask "钨矿怎么处理"   # 生成给 AI 用的上下文包
python kb/search.py --stats               # 数据规模统计
```

首次检索会自建索引（约 20 秒，写入 `kb/data/index.pkl`，属生成物，不入库）。

### Web 站点（四步）

```bash
python kbweb/build_site.py     # 从 kb/ 物化站点数据 → kbweb/data/
python kbweb/build_index.py    # BM25 检索索引 + 邻接 + linkmap
python kbweb/site_pack.py      # 打包正文为 pages.bin
python kbweb/serve.py          # http://127.0.0.1:8777
```

Windows 上可直接双击 `start.cmd`（自动完成前三步并起服务）。

链接图**不需要**原始 HTML 镜像：`kb/site/links.tsv` 随包带了 20 万条互链关系，
`build_site.py` 会自动用它重建图谱。原始镜像只在你想重抽正文或补齐图片名时才需要
（放到 `wiki-html/main/`，或用 `GTNH_KB_WIKI_HTML` 指定）。

### 细胞连接图

```bash
python tools/make_graph.py                                   # 默认取度数最高的 700 个条目
python tools/make_graph.py --nodes 1500 --edges 40000 --label-top 120
```

输出到 `graph/`：`graph.svg`（矢量图）、`graph.html`（查看页）、`graph.json`（喂 d3/Gephi）、
`GRAPH.md`（含 GitHub 原生可渲染的 Mermaid 局部图）。纯标准库生成，无 CDN、无游戏素材。

---

## 2. 目录结构

```
gtnh-knowledge-base/
├── README.md            本文件
├── LICENSE              CC BY-NC-SA 4.0 法律文本
├── NOTICE.md            逐数据源署名 + 许可矩阵
├── COMPLIANCE.md        合规改造说明：剔除了什么、为什么、如何自行补齐
├── PRIVACY.md           发布前隐私审查报告（本机痕迹已清理）
├── start.cmd            Windows 一键启动（建站 + 起服务）
├── search.cmd           Windows 检索入口
├── graph/               细胞连接图（svg / html / json / GRAPH.md）
├── kb/
│   ├── search.py        检索引擎（BM25 + 中文 bigram，纯标准库）
│   ├── wiki/            维基抽取结果，每行一个 JSON 页（ns_<命名空间>.jsonl）
│   ├── quests/          48 条任务线的可读全文（line_<任务线>.txt）
│   ├── ref/             整合包 config 参考值（机器参数/污染/矿脉/危险物品/提示）
│   ├── site/            入口层页面 + links.tsv（20 万条互链关系）
│   ├── data/            物品名表、标题索引（机器可读）
│   ├── TITLE_INDEX.md   全部条目标题总目录
│   ├── TOPIC_INDEX.md   按主题分组的索引
│   ├── INDEX.md         数据来源统计
│   └── ENTRY.md         上手导读：常见问题 → 该查哪里
├── kbweb/
│   ├── build_site.py    站点数据管线（kb/ → kbweb/data/）
│   ├── build_index.py   检索索引 / 邻接表 / linkmap
│   ├── site_pack.py     正文打包器
│   ├── serve.py         本地 Web 服务（纯标准库）
│   └── web/             静态前端（纯文字，无游戏素材）
└── tools/
    ├── make_graph.py    细胞连接图生成器
    ├── redact_pii.py    可选的第三方联系方式脱敏（默认 dry-run）
    ├── extract_wiki.py  从维基 HTML 镜像抽取正文（重新生成本包数据）
    ├── wiki_fast.py     同上，快速版
    ├── build_index_md.py   刷新 kb/INDEX.md
    └── build_title_index.py 刷新 kb/TITLE_INDEX.md / TOPIC_INDEX.md
```

---

## 3. 数据规模与版本

| 内容 | 规模 | 检索前缀 | 来源版本 |
|---|---|---|---|
| 维基正文（材料/机器/多方块/教程/产线） | 6305 页 | `wiki.main` | 2.8.4 稳定版为主，部分 2.9.0 前瞻 |
| 维基数据表 | 896 页 | `wiki.data` | 同上 |
| 其余命名空间（模板/模块/分类/项目…） | 约 1276 页 | `wiki.template` 等 | 同上 |
| 游戏内任务书全文 | 3805 个任务 / 48 条任务线 | `quest.*` | 整合包 Daily 704 原始英文 |
| config 参考值 | 7 个文件 | `ref.*` | 整合包 Daily 704 |
| 物品名映射 | 中/英对照 | `--item` | 整合包自带 lang 文件 |

整合包本体为 **GT New Horizons 2.9.x（Daily 704）**。
维基镜像与整合包版本不一致时，以 `quest.*`（随包更新）与 `ref.*`（config 原值）为准。

**已知局限**：维基中由脚本渲染的数值表格抽取可能不全（页面里出现
"加载合成表模板…"字样时），配方数字请以游戏内 NEI 为准；本知识库用于定位
"该做什么、需要什么机器、什么阶段"。

---

## 4. 许可与署名

本包整体按 **CC BY-NC-SA 4.0** 分发（见 `LICENSE`），因为三个数据源都是
**非商业（NC）+ 相同方式共享（SA）** 许可：

| 数据 | 原始许可 | 权利人 |
|---|---|---|
| GTNH 中文维基原创内容 | CC BY-NC-SA 3.0 | 中文维基贡献者（灰机 wiki） |
| 中文维基中译自英文维基的部分 | CC BY-SA 3.0 | GTNH Wiki (EN) 贡献者 |
| 中文维基中来自 MCMOD 的部分 | CC BY-NC-SA 3.0 | MCMOD 及其原作者 |
| 任务书文本、config 参考值 | CC BY-NC-SA 4.0 | GT New Horizons 团队 |

**使用时必须**：保留署名（见 `NOTICE.md` 的推荐署名串）、保留许可标识、注明是否修改、
以相同许可分发衍生作品、**不得用于商业目的**。

> CC BY-SA 3.0 / CC BY-NC-SA 4.0 的 ShareAlike 条款允许按"同一许可的后续版本"再许可，
> 故本包统一以 CC BY-NC-SA 4.0 分发；逐源原始许可仍以 `NOTICE.md` 为准。

---

## 5. 免责声明

本包内容**原样（AS-IS）**提供，不作任何明示或默示担保。数据来自社区维基与整合包，
可能存在错漏或与当前版本不符。GT New Horizons、Minecraft、Mojang、Microsoft 及
各模组作者与本包无隶属关系，也未对本包作出背书。若权利人认为本包内容侵犯其权利，
请提交 issue，我们会移除相应内容。
