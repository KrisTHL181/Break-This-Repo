#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""生成两套索引：
  1) kb/TITLE_INDEX.md   —— 按 A-Z 字母/拼音分区的可查目录（维基页面 + 任务书任务 + 参考数据）
  2) kb/TOPIC_INDEX.md   —— 按主题成组的索引（阶段教程/材料/机器/多方块/产线/机制/专题任务线）
另附机器可读的 kb/data/title_index.tsv（类型 / 标题 / 来源命名空间 / 文件），便于脚本二次检索。
"""
import os, re, json, io
from collections import defaultdict, Counter

HERE = os.path.dirname(os.path.abspath(__file__))
KB = os.path.join(os.path.dirname(HERE), 'kb')
WIKI = os.path.join(KB, 'wiki')
QUESTS = os.path.join(KB, 'quests')


def read_jsonl(p):
    with open(p, encoding='utf-8') as fh:
        for line in fh:
            line = line.strip()
            if line:
                yield json.loads(line)


def quest_entries():
    out = []
    for fn in sorted(os.listdir(QUESTS)):
        if not fn.startswith('line_') or not fn.endswith('.txt'):
            continue
        line = fn[5:-4]
        txt = open(os.path.join(QUESTS, fn), encoding='utf-8').read()
        for m in re.finditer(r'^### \[([^\]]*)\]\s*(.*)$', txt, re.M):
            name = re.sub(r'§.', '', m.group(2)).strip()
            if not name:
                continue
            # 属性任务（"XXX Template"/占位）单独归到"任务模板"
            kind = '任务模板' if re.search(r'Template|template|^\W*$', name) else '任务'
            out.append((kind, name, 'quest.' + line, fn, m.group(1)))
    return out


def wiki_entries():
    out = []
    for fn in sorted(os.listdir(WIKI)):
        if not fn.endswith('.jsonl'):
            continue
        ns = fn[3:-6]
        for r in read_jsonl(os.path.join(WIKI, fn)):
            out.append(('维基' if ns == 'main' else '维基:' + ns, r['title'], 'wiki.' + ns, r['file'], ''))
    return out


def classify(title, src):
    t = title
    if src.startswith('wiki') and re.search(r'教程|攻略|指南|路线|速查|入门', t):
        return '阶段教程与攻略'
    if re.search(r'^(第[\d\.]+阶|Tier)', t):
        return '阶段总览'
    if re.search(r'多方块|结构|机器$|机$|仓$|总线$|发电机|反应堆|高炉|塔$|压缩机|搅拌机|电解机|离心机|锻造|装配|电路|涡轮', t):
        return '机器与多方块'
    if re.search(r'处理$|线$|产线|工艺|提纯|精炼', t):
        return '产线与工艺流程'
    if re.search(r'污染|电力|电压|超频|并行|矿脉|探矿|爆炸|辐射|危险|温度|热容|线损|EU|物流|AE|管道', t):
        return '机制与系统'
    if re.search(r'[锭粉板箔环杆粒块齿轮螺丝钉螺丝]$|粉$|锭$|材料$|合金$', t):
        return '材料'
    if re.search(r'任务线|任务书|任务$', t):
        return '任务书相关'
    return '其他'


def main():
    entries = wiki_entries() + quest_entries()
    refs = [('参考数据', os.path.splitext(f)[0], 'ref.' + os.path.splitext(f)[0], f, '')
            for f in sorted(os.listdir(os.path.join(KB, 'ref')))]

    # ---------- TITLE_INDEX.md ----------
    zh = [e for e in entries if re.match(r'^[\u4e00-\u9fff]', e[1])]
    en = [e for e in entries if not re.match(r'^[\u4e00-\u9fff]', e[1])]
    zh.sort(key=lambda e: e[1])
    en.sort(key=lambda e: e[1].lower())

    buf = io.StringIO()
    buf.write('# 知识库标题总目录（TITLE INDEX）\n\n')
    buf.write(f'共 **{len(entries)}** 个条目：维基页面 + 任务书任务。检索用 `q.cmd <关键词>`，'
              '想只看标题命中用 `q.cmd --exact <关键词>`。\n\n')
    buf.write('| 分区 | 条目数 |\n|---|---|\n')
    buf.write(f'| 中文标题 | {len(zh)} |\n| 英文/数字标题 | {len(en)} |\n| 参考数据 | {len(refs)} |\n\n')
    buf.write('> 机器可读版本：`kb/data/title_index.tsv`（类型 / 标题 / 检索来源 / 文件名 / 任务ID）\n\n')

    def dump(title, items):
        buf.write(f'## {title}\n\n')
        for e in items:
            kind, name, src, ref, qid = e
            suffix = f' — `{qid}`' if qid else ''
            buf.write(f'- **{name}**{suffix} · _{kind}_ · `{src}` · {ref}\n')
        buf.write('\n')

    dump(f'中文标题（{len(zh)}）', zh)
    dump(f'英文 / 数字标题（{len(en)}）', en)
    dump('参考数据', refs)
    with open(os.path.join(KB, 'TITLE_INDEX.md'), 'w', encoding='utf-8') as fh:
        fh.write(buf.getvalue())

    # ---------- TOPIC_INDEX.md ----------
    groups = defaultdict(list)
    for e in entries + refs:
        kind, name, src, ref, qid = e
        if kind == '任务' or kind == '任务模板':
            continue
        groups[classify(name, src)].append(e)
    buf2 = io.StringIO()
    buf2.write('# 主题索引（TOPIC INDEX）\n\n')
    buf2.write('按“我要做什么”分组。每组给出可直接检索的条目名，'
               '命令示例：`q.cmd 铂处理`、`q.cmd --ns wiki.main 超频`。\n\n')
    order = ['阶段教程与攻略', '阶段总览', '产线与工艺流程', '机器与多方块', '机制与系统',
             '材料', '任务书相关', '其他', '参考数据']
    buf2.write('| 主题 | 条目数 |\n|---|---|\n')
    for k in order:
        if groups.get(k):
            buf2.write(f'| [{k}](#{k}) | {len(groups[k])} |\n')
    buf2.write('\n')
    for k in order:
        items = groups.get(k)
        if not items:
            continue
        buf2.write(f'## {k}\n\n')
        seen = set()
        n = 0
        for kind, name, src, ref, qid in sorted(items, key=lambda e: e[1]):
            if name in seen:
                continue
            seen.add(name)
            buf2.write(f'- **{name}** · `{src}`\n')
            n += 1
            if n >= 400:
                buf2.write(f'- …（本组共 {len(items)} 条，此处截断；完整清单见 TITLE_INDEX.md）\n')
                break
        buf2.write('\n')
    buf2.write('## 专题任务线（不在 Tier 主线的方向）\n\n')
    for fn in sorted(os.listdir(QUESTS)):
        if not fn.startswith('line_') or not fn.endswith('.txt'):
            continue
        line = fn[5:-4]
        if re.match(r'^(Tier|And So|Tips|line)', line):
            continue
        cnt = len(re.findall(r'^### \[', open(os.path.join(QUESTS, fn), encoding='utf-8').read(), re.M))
        buf2.write(f'- **{line}**（{cnt} 个任务）· `quest.{line}`\n')
    buf2.write('\n')
    with open(os.path.join(KB, 'TOPIC_INDEX.md'), 'w', encoding='utf-8') as fh:
        fh.write(buf2.getvalue())

    # ---------- title_index.tsv ----------
    with open(os.path.join(KB, 'data', 'title_index.tsv'), 'w', encoding='utf-8') as fh:
        for e in sorted(entries + refs, key=lambda x: (x[0], x[1])):
            fh.write('\t'.join(e) + '\n')

    print('TITLE_INDEX.md', len(buf.getvalue()) // 1024, 'KB;',
          'TOPIC_INDEX.md', len(buf2.getvalue()) // 1024, 'KB;',
          'entries', len(entries) + len(refs))


if __name__ == '__main__':
    main()
