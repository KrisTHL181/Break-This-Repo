#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""统计 kb/ 内容并生成 kb/INDEX.md 总索引（供人和 AI 快速定位）。"""
import os, re, json, io
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(HERE)
KB = os.path.join(WS, 'kb')
WIKI = os.path.join(KB, 'wiki')
QUESTS = os.path.join(KB, 'quests')

TUT = re.compile(r'教程|攻略|指南|路线|速查|入门|进阶|新手')
LINE_KIND = [
    ('主线阶段', re.compile(r'^Tier')),
    ('能源', re.compile(r'Power|EU|Nuclear|Oil|Bio for')),
    ('矿物与加工', re.compile(r'Mass Processing|Multiblock|Green Revolution')),
    ('物流与自动化', re.compile(r'Automation|Logistics|SFM|Applied Energistics|Storing')),
    ('基地与生存', re.compile(r'Base|Dying|Around|Feeding|Tips')),
    ('魔法线', re.compile(r'Thaum|Kaaami|Wand|Flower|Edges|Highest|Kill All|End\\(er\\)')),
    ('生物与农业', re.compile(r'Bee|Forestry|Green')),
    ('终局', re.compile(r'Endgame')),
]


def read_jsonl(p):
    with open(p, encoding='utf-8') as fh:
        for line in fh:
            line = line.strip()
            if line:
                yield json.loads(line)


def main():
    out = io.StringIO()
    out.write('# GTNH 知识库总索引\n\n')
    out.write('本文件由 `_tools/build_index_md.py` 自动生成，列出知识库全部可检索内容。\n\n')

    # ---- quest lines ----
    out.write('## 一、任务书（游戏内 BetterQuesting 全量任务）\n\n')
    out.write('来源：`GT New Horizons daily/.minecraft/config/betterquesting/DefaultQuests`（3805 个任务，pack_version 2141）。\n')
    out.write('检索前缀：`quest.<任务线名>`，例如 `--ns quest.Tier1LV`。\n\n')
    order_file = os.path.join(KB, 'data', 'questlines_order.txt')
    order = []
    if os.path.exists(order_file):
        for ln in open(order_file, encoding='utf-8'):
            ln = ln.strip()
            if ln:
                order.append(ln.split(': ', 1))
    sizes = {}
    titles = {}
    for fn in sorted(os.listdir(QUESTS)):
        if not fn.startswith('line_') or not fn.endswith('.txt'):
            continue
        name = fn[5:-4]
        txt = open(os.path.join(QUESTS, fn), encoding='utf-8').read()
        sizes[name] = len(re.findall(r'^### \[', txt, re.M))
        titles[name] = txt.split('\n', 1)[0].replace('# 任务线: ', '').strip()
    out.write('| 任务线（文件） | 任务数 | 说明 |\n|---|---|---|\n')
    for name in sorted(sizes, key=lambda k: -sizes[k]):
        out.write(f'| `line_{name}.txt` | {sizes[name]} | {titles.get(name, "")} |\n')
    out.write('\n')

    # ---- wiki namespaces ----
    out.write('## 二、离线维基（灰机 wiki 镜像，2.8.4 版内容）\n\n')
    out.write('来源：`gtnh-wiki-archive/out/pages`（原 HTML 全量保留），已抽取为可检索 JSONL：`kb/wiki/ns_*.jsonl`。\n')
    out.write('检索前缀：`wiki.<命名空间>`，例如 `--ns wiki.main`（正文）、`--ns wiki.data`（数据表）。\n\n')
    out.write('| 文件 | 页数 | 说明 |\n|---|---|---|\n')
    ns_desc = {
        'main': '正文条目：材料/机器/多方块/教程/产线/版本',
        'data': '数据表（tabx）：机器参数、配方数据等批量表格',
        'category': '分类页',
        'template': '模板',
        'module': 'Lua 模块源码说明',
        'project': '维基项目页（编写规范、版权）',
        'gadget': '小工具源码（代码展示、正则生成器）',
        'mediawiki': '系统页面',
        'html': 'HTML 元素文档',
        'form': '表单定义',
        'property': '语义属性定义',
        'help': '帮助页',
    }
    for fn in sorted(os.listdir(WIKI)):
        if not fn.endswith('.jsonl'):
            continue
        ns = fn[3:-6]
        n = sum(1 for _ in read_jsonl(os.path.join(WIKI, fn)))
        out.write(f'| `{fn}` | {n} | {ns_desc.get(ns, "")} |\n')
    out.write('\n')

    # main namespace highlights
    out.write('### 2.1 正文重点条目（自动筛选）\n\n')
    groups = defaultdict(list)
    for r in read_jsonl(os.path.join(WIKI, 'ns_main.jsonl')):
        t = r['title']
        if TUT.search(t) and not re.search(r'_I{1,3}V?$|_\d+$|^PH_|^TST_', t):
            groups['教程/攻略类'].append(t)
        elif re.search(r'多方块|结构', t):
            groups['多方块结构'].append(t)
        elif re.search(r'产线$|生产线$|工艺流程|教程$', t):
            groups['产线/工艺流程'].append(t)
    for k in ('教程/攻略类', '产线/工艺流程', '多方块结构'):
        lst = sorted(set(groups[k]))
        out.write(f'**{k}**（{len(lst)} 条，节选）\n\n')
        out.write('、'.join(lst[:120]) + '\n\n')
    out.write('\n')

    # ---- ref ----
    out.write('## 三、参考数据（config 原文件副本）\n\n')
    refdir = os.path.join(KB, 'ref')
    out.write('| 文件 | 大小 | 内容 |\n|---|---|---|\n')
    ref_desc = {
        'GT_MachineStats.cfg': 'GregTech 机器参数（电压/耗时/功耗倍率）',
        'GT_Pollution.cfg': '污染机制参数（排放、扩散、效果阈值）',
        'GT_WorldGeneration.cfg': '矿脉世界生成开关与参数',
        'GTNH_CustomToolTips.xml': 'GTNH 自定义物品提示',
        'GTNH_dreamcraft.cfg': 'GTNH 核心模组 dreamcraft 配置',
        'GTNH_HazardousItems.xml': '危险物品（辐射/毒性）定义',
        'changelog_703_to_704.md': 'Daily 703 → 704 变更日志',
    }
    if os.path.isdir(refdir):
        for fn in sorted(os.listdir(refdir)):
            p = os.path.join(refdir, fn)
            out.write(f'| `{fn}` | {os.path.getsize(p)//1024} KB | {ref_desc.get(fn, "")} |\n')
    out.write('\n')

    # ---- item names ----
    im = os.path.join(KB, 'data', 'item_names.tsv')
    n_items = sum(1 for _ in open(im, encoding='utf-8')) if os.path.exists(im) else 0
    out.write('## 四、物品名映射\n\n')
    out.write(f'`kb/data/item_names.tsv`：{n_items} 条 `内部ID → 名称`（从 248 个模组 jar 的 lang 文件中提取，优先中文）。\n')
    out.write('用法：`python kb/search.py --item 真空冷冻机`（反查内部 ID）。\n\n')

    # ---- usage ----
    out.write('## 五、检索用法速查\n\n')
    out.write('最省事的方式（工作区根目录，自动处理中文编码）：\n\n')
    out.write('```powershell\n')
    out.write('cd <知识库目录>\n')
    out.write('.\\q.cmd 真空冷冻机                  # = kb\\kb.cmd，等价于 python kb\\search.py\n')
    out.write('.\\q.cmd --exact 钨                  # 只看标题命中的条目（查目录）\n')
    out.write('```\n\n')
    out.write('等价的 python 调用：\n\n')
    out.write('```powershell\n')
    out.write('$env:PYTHONIOENCODING="utf-8"\n\n')
    out.write('python kb\\search.py 真空冷冻机            # 按关键词搜（中文/英文/ID 均可）\n')
    out.write('python kb\\search.py "钨 采矿场" --top 8    # 多关键词\n')
    out.write('python kb\\search.py 超频 --ns wiki.main    # 只看维基正文\n')
    out.write('python kb\\search.py 多方块 --ns quest.Multiblock\\ Goals\n')
    out.write('python kb\\search.py 污染 --full            # 输出完整命中段落\n')
    out.write('python kb\\search.py --item 真空冷冻机       # 物品名反查内部 ID\n')
    out.write('python kb\\search.py --ask "怎么处理钨矿"    # 生成给 AI 的上下文包\n')
    out.write('python kb\\search.py --stats               # 知识库统计\n')
    out.write('python kb\\search.py --rebuild             # 数据更新后重建索引\n')
    out.write('```\n\n')
    out.write('索引缓存：`kb/data/index.pkl`（首次 ~20 秒建立，之后查询 <2 秒；新增/修改数据后需 `--rebuild`）。\n')

    with open(os.path.join(KB, 'INDEX.md'), 'w', encoding='utf-8') as fh:
        fh.write(out.getvalue())
    print('INDEX.md written,', len(out.getvalue()), 'chars')


if __name__ == '__main__':
    main()
