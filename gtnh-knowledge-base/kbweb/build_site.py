#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""GTNH 知识库网站的数据管线：把 kb/ 与维基镜像物化成网站可直接消费的静态索引。

输出到 kbweb/data/：
  meta.json          站点元信息、统计、分组、任务线清单
  titles.tsv         标题索引：title \t group \t 入度 \t 出度 \t stub   ← 顺序是全站唯一权威 id 空间
  pages/<title>.txt  每页正文（site_pack.py 再打包成 pages.bin）
  pages.idx          title \t length（site_pack.py 会改写成 title \t offset \t length）
  graph.edges.tsv    src_id \t dst_title \t dst_id(-1=红链)
  backlinks/<title>.txt 反向链接（每行: src_title）
  images/<title>.txt 该页图片文件名列表

输入：
  kb/wiki/ns_main.jsonl        维基正文（主体）
  <包根>/wiki-html/main/       可选的维基 HTML 镜像（抽站内链接与图片名）
                               缺失 → 只建纯文字站（无链接图/无图片名）
  kb/site/extra_pages.jsonl    入口层页面（枢纽/索引/任务）
  kb/quests/line_*.txt         任务线（结构 + 英文）
  <包根>/i18n/bq_quest_zh.tsv  可选的任务书汉化叠加层（缺失 → 任务只有英文）
  kb/data/item_names_zh.tsv    物品中文名表（缺失时退回 item_names.tsv）

用法：
  python kbweb/build_site.py
  python kbweb/site_pack.py        # 把 pages/ 打包成 pages.bin（服务端随机读）
"""
import os, re, sys, json, io, glob, collections

HERE = os.path.dirname(os.path.abspath(__file__))
PKG = os.path.dirname(HERE)
WS = PKG
KB = os.path.join(PKG, 'kb')
OUT = os.path.join(HERE, 'data')
# 可选的维基 HTML 镜像：用于抽取站内链接与图片名。没有就只建"纯文字站"。
WIKI_HTML = os.environ.get('GTNH_KB_WIKI_HTML', os.path.join(PKG, 'wiki-html', 'main'))
WIKI_JSONL = os.path.join(KB, 'wiki', 'ns_main.jsonl')
QUESTS = os.path.join(KB, 'quests')
EXTRA_JSONL = os.path.join(KB, 'site', 'extra_pages.jsonl')
# 预置链接表（纯文本，随包发布）：没有维基 HTML 镜像时用它重建链接图
LINKS_TSV = os.path.join(KB, 'site', 'links.tsv')
# 可选的任务书汉化叠加层（第三方汉化项目）。缺失时任务只显示英文原文。
QUEST_ZH = os.environ.get('GTNH_KB_QUEST_ZH', os.path.join(PKG, 'i18n', 'bq_quest_zh.tsv'))
# 物品名表：优先整合包自带汉化，其次英文表
ITEM_TSV = os.path.join(KB, 'data', 'item_names_zh.tsv')
if not os.path.exists(ITEM_TSV):
    ITEM_TSV = os.path.join(KB, 'data', 'item_names.tsv')

sys.stdout.reconfigure(encoding='utf-8')


def wiki_jsonl_parts():
    """ns_main.jsonl 可能被切成 ns_main_partK.jsonl 分片，读取时透明合并。

    切分原因：GitHub 的 Git Blobs API 拒收过大的 JSON 请求体
    （64 MB 的 jsonl base64 后约 86 MB，会返回 422），故分片推送。
    """
    parts = sorted(glob.glob(os.path.join(KB, 'wiki', 'ns_main*.jsonl')))
    return parts or ([WIKI_JSONL] if os.path.exists(WIKI_JSONL) else [])


def load_pre_links():
    """读取 kb/site/links.tsv（src_title \t dst_title），用于无 HTML 镜像时重建图。"""
    m = collections.defaultdict(list)
    if not os.path.exists(LINKS_TSV):
        return m
    with open(LINKS_TSV, encoding='utf-8', errors='replace') as fh:
        for line in fh:
            s, _, d = line.rstrip('\n').partition('\t')
            if s and d:
                m[sanitize(s)].append(d)
    print(f'[links] 预置链接表: {len(m)} 个源页 / '
          f'{sum(len(v) for v in m.values())} 条链接')
    return m

A = re.compile(r'<a\s[^>]*href="\.\./main/([^"#]+?)\.html(?:#[^"]*)?"[^>]*>(.*?)</a>', re.S | re.I)
IMG = re.compile(r'<img\s[^>]*?src="(?:\.\./)*assets/img/([^"?]+)', re.I)
WID = re.compile(r'width="(\d+)"')
TAG = re.compile(r'<[^>]+>')
BAD = re.compile(r'[\\/:*?"<>|\[\]#^]')
BOILER = {'工作台', '更新日志_2.8.3_-_2.8.4', '特性前瞻_2.8.0'}
SKIP_LINK = {'Main Page', '首页'}

TUT = re.compile(r'教程|攻略|指南|路线|速查|入门|新手')
PROD = re.compile(r'处理$|产线|工艺|提纯|精炼|流程')
MACH = re.compile(r'多方块|机器|机$|仓$|总线$|发电机|反应堆|高炉|塔$|压缩机|搅拌机|电解|离心|锻造|装配|电路|涡轮|钻机|采矿场|泵$')
MECH = re.compile(r'污染|电力|电压|超频|并行|矿脉|探矿|爆炸|辐射|危险|温度|热容|线损|物流|管道|总线|储存|存储|变压|发电|燃料|反应|机制|计算公式|效率|EU')
MAT = re.compile(r'粉$|锭$|板$|箔$|环$|杆$|粒$|块$|合金|材料$|晶体|元素|化合物|酸$|气体$')
GROUPS = [('教程', TUT), ('产线', PROD), ('机器', MACH), ('机制', MECH), ('材料', MAT)]


def sanitize(name):
    name = name.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
    n = name.replace('/', '／').replace('\\', '＼')
    return BAD.sub('', n).strip().strip('.') or '未命名'


def classify(title):
    for g, rx in GROUPS:
        if rx.search(title):
            return g
    return '其他'


def drop_navboxes(html):
    out, i, n = [], 0, len(html)
    while True:
        j = html.find('<table', i)
        if j < 0:
            out.append(html[i:])
            break
        out.append(html[i:j])
        k, depth = j, 0
        while k < n:
            no, nc = html.find('<table', k), html.find('</table>', k)
            if nc < 0:
                k = n
                break
            if 0 <= no < nc:
                depth += 1
                k = no + 6
            else:
                depth -= 1
                k = nc + 8
                if depth <= 0:
                    break
        chunk = html[j:k]
        if 'navbox' not in chunk[:400] and 'Navbox' not in chunk[:400]:
            out.append(chunk)
        i = k
    return ''.join(out)


def html_links_images(body):
    links, seen = [], set()
    for m in A.finditer(body):
        t = sanitize(m.group(1))
        if t in SKIP_LINK or t in seen:
            continue
        seen.add(t)
        label = re.sub(r'\s+', ' ', TAG.sub('', m.group(2))).strip()
        links.append((t, label))
    imgs, seen_i = [], set()
    for m in IMG.finditer(body):
        src = m.group(1)
        tag = m.group(0)
        wm = WID.search(tag)
        w = int(wm.group(1)) if wm else 0
        # 真实像素宽写在文件名里：<hash>-<宽>px-<名字>.png
        fm = re.match(r'^[0-9a-f]{6,10}-(\d+)px-', src)
        src_w = int(fm.group(1)) if fm else 0
        if 'Icon' in src or 'Disambig' in src or 'GridNumbers' in src or 'CSS' in src:
            continue
        if src in seen_i:
            continue
        seen_i.add(src)
        imgs.append((src, max(w, src_w)))
    return links, imgs


def clean_body(text):
    out = []
    for ln in text.split('\n'):
        s = ln.strip()
        if not s or re.fullmatch(r'</?div[^>]*>|<div', s):
            continue
        if s.count('{') + s.count('}') >= 2 and (s.count('{') + s.count('}')) * 12 > len(s):
            continue
        if s.startswith('{"') or s.startswith('mw-parser-output'):
            continue
        if re.match(r'^(var|function)\s', s):
            continue
        out.append(s)
    return '\n'.join(out)


def load_quest_zh():
    """任务书中文（GitHub 汉化项目）：questID -> (name_zh, desc_zh)。与 vault_build.py 同源。"""
    m = {}
    if not os.path.exists(QUEST_ZH):
        print('[quest-zh] 无', QUEST_ZH, '（任务只有英文）')
        return m
    with open(QUEST_ZH, encoding='utf-8') as fh:
        next(fh, None)
        for ln in fh:
            parts = ln.rstrip('\n').split('\t')
            if len(parts) < 2 or not parts[0].lstrip('-').isdigit():
                continue
            m[int(parts[0])] = (parts[1], parts[2] if len(parts) > 2 else '')
    print('[quest-zh] 中文任务:', len(m))
    return m


def clean_zh(s):
    """任务书中文串清理：§颜色码、%n 换行、[warn]/[note]/[url] 标记。"""
    s = re.sub(r'§.', '', s or '')
    s = s.replace('%n%n', '\n').replace('%n', '\n')
    s = re.sub(r'\[warn\](.*?)\[/warn\]', r'⚠️ \1', s, flags=re.S)
    s = re.sub(r'\[note\](.*?)\[/note\]', r'📝 \1', s, flags=re.S)
    s = re.sub(r'\[url\](.*?)\[/url\]', r'\1', s, flags=re.S)
    s = s.replace('\n', ' ')
    return re.sub(r'[ \t]{2,}', ' ', s).strip()


def parse_quests():
    """解析 kb/quests/line_*.txt，并用 _zh/bq_quest_zh.tsv 叠加中文名/中文描述。

    与 vault_build.py 保持同一数据源：任务结构取 kb/quests，中文取 _zh/bq_quest_zh.tsv。
    原英文写进 nameEn / descEn，name / desc 在有中文时显示中文（前端无需改动）。
    """
    zh = load_quest_zh()
    out = collections.OrderedDict()
    n_zh = 0
    for fn in sorted(os.listdir(QUESTS)):
        if not fn.startswith('line_') or not fn.endswith('.txt'):
            continue
        line = fn[5:-4]
        txt = open(os.path.join(QUESTS, fn), encoding='utf-8').read()
        qs = []
        for b in re.split(r'\n(?=### \[)', txt):
            m = re.match(r'### \[([^\]]*)\]\s*(.*)', b.strip())
            if not m:
                continue
            qid = m.group(1)
            name = re.sub(r'§.', '', m.group(2)).strip()
            desc, tasks, rew = [], [], None
            for ln in b.split('\n')[1:]:
                ln = ln.strip()
                if not ln:
                    continue
                if ln.startswith('> 奖励:'):
                    rew = ln[5:].strip()
                elif ln.startswith('- ['):
                    tasks.append(ln[2:].strip())
                else:
                    desc.append(ln)
            desc = ' '.join(desc)
            desc = re.sub(r'\[warn\](.*?)\[/warn\]', r'\1', desc)
            desc = re.sub(r'\[note\](.*?)\[/note\]', r'\1', desc)
            desc = re.sub(r'\[url\](.*?)\[/url\]', r'\1', desc)
            rec = {'id': qid, 'name': name or f'(未命名 {qid})', 'desc': desc,
                   'tasks': tasks, 'reward': rew}
            if qid.lstrip('-').isdigit():
                zn, zd = zh.get(int(qid), ('', ''))
                if zn:
                    rec['nameEn'] = rec['name']
                    rec['name'] = clean_zh(zn)
                if zd:
                    rec['descEn'] = rec['desc']
                    rec['desc'] = clean_zh(zd)
                if zn or zd:
                    n_zh += 1
            qs.append(rec)
        out[line] = qs
    print('[quest-zh] 已中文化任务:', n_zh)
    return out


def load_extra():
    """读取 site_extra.py 导出的入口层页面（枢纽 / 索引 / 任务）。"""
    if not os.path.exists(EXTRA_JSONL):
        print('[extra] 无 kb/site/extra_pages.jsonl，跳过（先跑 _tools/site_extra.py）')
        return collections.OrderedDict()
    out = collections.OrderedDict()
    with open(EXTRA_JSONL, encoding='utf-8') as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            r = json.loads(line)
            if r.get('title'):
                out[r['title']] = r
    print('[extra] 入口层页面:', len(out))
    return out


def main():
    for d in ('pages', 'backlinks', 'images'):
        os.makedirs(os.path.join(OUT, d), exist_ok=True)

    recs = []
    for part in wiki_jsonl_parts():
        with open(part, encoding='utf-8') as fh:
            for line in fh:
                line = line.strip()
                if line:
                    recs.append(json.loads(line))
    print('wiki pages:', len(recs))

    # 标题 -> 记录（去重）
    by_title = {}
    for r in recs:
        t = sanitize(r['title'])
        if t not in by_title:
            by_title[t] = r
    # 入口层页面追加在维基页之后；重名时维基优先，库页加「（组名）」后缀
    extra = load_extra()
    extra_titles = []
    renamed = 0
    for t in list(extra.keys()):
        r = extra[t]
        if t in by_title:
            nt = f'{t}（{r.get("group", "库")}）'
            del extra[t]
            r['title'] = nt
            extra[nt] = r
            extra_titles.append(nt)
            renamed += 1
            print(f'[extra] 重名改标题: {t} -> {nt}')
        else:
            extra_titles.append(t)
    titles = sorted(by_title) + extra_titles
    print(f'[extra] 合并后标题总数: {len(titles)}')

    # 正文 & 索引
    page_meta = {}
    edges = []
    images_map = {}
    text_map = {}
    freq = collections.Counter()
    pre_links = load_pre_links()
    for t in titles:
        if t in extra:
            r = extra[t]
            text = r.get('text', '')
            fn = r.get('file', '')
            links = [(sanitize(x), '') for x in r.get('links', [])]
            imgs = list(r.get('images', []))
            group = r.get('group') or classify(t)
        else:
            r = by_title[t]
            text = clean_body(r['text'])
            fn = r['file']
            path = os.path.join(WIKI_HTML, fn)
            links, imgs = ([], [])
            if os.path.exists(path):
                raw = open(path, encoding='utf-8', errors='replace').read()
                j = raw.find('mw-parser-output')
                if j >= 0:
                    gt = raw.find('>', j)
                    i = gt + 1 if gt > 0 else j
                    end = raw.find('class="printfooter"', i)
                    if end < 0:
                        end = raw.find('id="catlinks"', i)
                    if end < 0:
                        end = len(raw)
                    links, imgs = html_links_images(drop_navboxes(raw[i:end]))
            else:
                # 没有维基 HTML 镜像时，用随包发布的链接表（kb/site/links.tsv）重建图
                links = [(sanitize(d), '') for d in pre_links.get(t, ())]
            group = classify(t)
        text_map[t] = text
        page_meta[t] = {'file': fn, 'group': group, 'len': len(text)}
        images_map[t] = imgs
        for lt, _lbl in links:
            freq[lt] += 1
        edges.append((t, [lt for lt, _ in links if lt not in BOILER and lt != t]))

    # 写正文（site_pack.py 随后打包成 pages.bin）
    idx_lines = []
    off = 0
    for t in titles:
        text = text_map[t]
        b = text.encode('utf-8')
        with open(os.path.join(OUT, 'pages', t + '.txt'), 'wb') as fh:
            fh.write(b)
        idx_lines.append(f'{t}\t{len(b)}')
        off += len(b)
    with open(os.path.join(OUT, 'pages.idx'), 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(idx_lines))

    # 图与反链
    dst_ids = {t: i for i, t in enumerate(titles)}
    bl = collections.defaultdict(list)
    edge_lines = []
    for src, dsts in edges:
        for d in dsts:
            bl[d].append(src)
            edge_lines.append(f'{dst_ids[src]}\t{d}\t{dst_ids.get(d, -1)}')
    with open(os.path.join(OUT, 'graph.edges.tsv'), 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(edge_lines))
    for t, lst in bl.items():
        with open(os.path.join(OUT, 'backlinks', t + '.txt'), 'w', encoding='utf-8') as fh:
            fh.write('\n'.join(sorted(set(lst))))
    for t, imgs in images_map.items():
        if not imgs:
            continue

        def score(it):
            src, w = it
            s = 0.0
            if 150 <= w <= 800:
                s += 5
            elif 120 <= w < 150:
                s += 3
            elif w == 0:
                s += 1.0
            if w and w < 120:
                s -= 12         # 物品小图标一律不进正文图区
            if t in src:
                s += 4
            if re.search(r'结构|多方块|组装|示意|布局|GUI|gui|预览|导出', src):
                s += 3
            return -s

        best = sorted(imgs, key=score)
        keep = [s for s, w in best if not (w and w < 120)][:3]
        if keep:
            with open(os.path.join(OUT, 'images', t + '.txt'), 'w', encoding='utf-8') as fh:
                fh.write('\n'.join(keep))

    # 标题索引（含度数）
    deg_out = collections.Counter()
    for src, dsts in edges:
        deg_out[src] = len(dsts)
    deg_in = collections.Counter({k: len(v) for k, v in bl.items()})
    with open(os.path.join(OUT, 'titles.tsv'), 'w', encoding='utf-8') as fh:
        for t in titles:
            fh.write(f'{t}\t{page_meta[t]["group"]}\t{deg_in[t]}\t{deg_out[t]}\t{0}\n')

    # 搜索语料：标题 + 首段（去掉样式噪声），供服务端做轻量检索/联想
    search_rows = []
    for t in titles:
        text = text_map[t]
        head = ' '.join(text.split('\n')[:12])[:600]
        search_rows.append({'t': t, 'g': page_meta[t]['group'], 'h': head})
    with open(os.path.join(OUT, 'search.jsonl'), 'w', encoding='utf-8') as fh:
        for r in search_rows:
            fh.write(json.dumps(r, ensure_ascii=False) + '\n')

    # 任务线
    quests = parse_quests()
    order = []
    of = os.path.join(KB, 'data', 'questlines_order.txt')
    if os.path.exists(of):
        for ln in open(of, encoding='utf-8'):
            ln = ln.strip()
            if ln and ': ' in ln:
                order.append(ln.split(': ', 1)[1])
    ordered = [n for n in order if n in quests] + [n for n in quests if n not in order]

    # 物品名
    items = []
    if os.path.exists(ITEM_TSV):
        with open(ITEM_TSV, encoding='utf-8') as fh:
            for ln in fh:
                k, _, v = ln.rstrip('\n').partition('\t')
                if k and v:
                    items.append((k, v))
    with open(os.path.join(OUT, 'items.tsv'), 'w', encoding='utf-8') as fh:
        for k, v in items:
            fh.write(f'{k}\t{v}\n')

    meta = {
        'site': 'GTNH 知识库',
        'pack': 'GT New Horizons 2.9.x (Daily 704)',
        'wikiVersion': '2.8.4（离线镜像）',
        'counts': {
            'pages': len(titles),
            'questLines': len(ordered),
            'quests': sum(len(v) for v in quests.values()),
            'edges': len(edge_lines),
            'items': len(items),
            'images': sum(len(v) for v in images_map.values()),
        },
        'groups': {g: sum(1 for t in titles if page_meta[t]['group'] == g)
                   for g in sorted({page_meta[t]['group'] for t in titles})},
        'questLines': [{'name': n, 'count': len(quests[n]),
                        'tier': (re.match(r'^Tier\s+(\d+(?:\.\d+)?)', n).group(1)
                                 if re.match(r'^Tier\s+(\d+(?:\.\d+)?)', n) else None)}
                       for n in ordered],
        'topPages': sorted(((t, deg_in[t]) for t in titles), key=lambda x: -x[1])[:200],
    }
    with open(os.path.join(OUT, 'meta.json'), 'w', encoding='utf-8') as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=1)

    # 任务数据按任务线分文件
    qdir = os.path.join(OUT, 'quests')
    os.makedirs(qdir, exist_ok=True)
    for n, qs in quests.items():
        with open(os.path.join(qdir, sanitize(n) + '.json'), 'w', encoding='utf-8') as fh:
            json.dump({'name': n, 'quests': qs}, fh, ensure_ascii=False)

    print('done:', json.dumps(meta['counts'], ensure_ascii=False))
    print('groups:', json.dumps(meta['groups'], ensure_ascii=False))
    print('top:', [t for t, _ in meta['topPages'][:10]])


if __name__ == '__main__':
    main()
