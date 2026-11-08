#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""网站检索索引：标题前缀表 + 正文倒排（BM25）。

输出 kbweb/data/：
  search_index.json    {N, avgdl, postings:{token:{pid:tf}}, lengths:[...]}
  title_index.json     [{t,g,deg}] 供前端联想与导航
  neighbors.json       {pid:[dst_pid...]} 图谱邻接（按权排序、每页限 140 条）
  linkmap.json         {规范化链接名: 页面标题} 用于红链解析

前置：先跑 kbweb/build_site.py（需要它产出的 titles.tsv / graph.edges.tsv / pages/）。
用法：python kbweb/build_index.py
"""
import os, re, sys, json, glob, collections, math

HERE = os.path.dirname(os.path.abspath(__file__))
PKG = os.path.dirname(HERE)
DATA = os.path.join(HERE, 'data')
WIKI_JSONL = os.path.join(PKG, 'kb', 'wiki', 'ns_main.jsonl')
sys.stdout.reconfigure(encoding='utf-8')

CJK = re.compile(r'[\u4e00-\u9fff]')
LAT = re.compile(r'[a-z0-9_\.\-]{2,}')
STOP = set('the and for with you your are can use using this that from will not has have its into out any all get got make with when then than them our'.split())


def tokens(text):
    text = text.lower()
    out = []
    out += [t for t in LAT.findall(text) if t not in STOP]
    cjk = CJK.findall(text)
    out += cjk
    out += [cjk[i] + cjk[i + 1] for i in range(len(cjk) - 1)]
    return out


def sanitize(name):
    name = name.replace('&amp;', '&')
    return re.sub(r'[\\/:*?"<>|\[\]#^]', '', name).strip().strip('.') or '未命名'


def norm_key(t):
    return re.sub(r'[\s_\-]+', '', t).lower()


def load_titles():
    """标题顺序的**唯一权威来源**：site_pipeline.py 写出的 titles.tsv。

    第 3 列是 site_pipeline 从 graph.edges.tsv 算好的入度，直接复用。
    历史问题：这里曾用 ns_main.jsonl 的原始顺序另建一套 pid，而 graph.edges.tsv /
    neighbors.json 用的是 site_pipeline 的 sorted(titles) 顺序，两套 id 空间在 284 个
    标题上错位（首个 He-3 vs He），导致 title_index 的度数、局部图谱邻接读错页。
    """
    titles, deg_in = [], {}
    with open(os.path.join(DATA, 'titles.tsv'), encoding='utf-8') as fh:
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) >= 3 and p[0]:
                titles.append(p[0])
                try:
                    deg_in[p[0]] = int(p[2])
                except ValueError:
                    deg_in[p[0]] = 0
    return titles, deg_in


def load_texts(titles):
    """正文以 site_pipeline 产出的 pages/<title>.txt 为准（含 extra 页）。

    这样索引与正文同源，不再依赖维基 jsonl 的顺序；个别缺失时回退到 jsonl。
    """
    pdir = os.path.join(DATA, 'pages')
    texts, missing = {}, []
    for t in titles:
        p = os.path.join(pdir, t + '.txt')
        if os.path.exists(p):
            with open(p, encoding='utf-8', errors='replace') as fh:
                texts[t] = fh.read()
        else:
            missing.append(t)
    if missing:
        print(f'pages/ 缺 {len(missing)} 页，回退维基 jsonl：{missing[:3]} …')
        pool = set(missing)
        parts = sorted(glob.glob(os.path.join(PKG, 'kb', 'wiki', 'ns_main*.jsonl')))
        if not parts and os.path.exists(WIKI_JSONL):
            parts = [WIKI_JSONL]
        for part in parts:
            with open(part, encoding='utf-8') as fh:
                for line in fh:
                    line = line.strip()
                    if not line:
                        continue
                    r = json.loads(line)
                    t = sanitize(r['title'])
                    if t in pool and t not in texts:
                        texts[t] = r['text']
    for t in titles:
        texts.setdefault(t, '')
    return texts


def main():
    titles, deg_in_by_title = load_titles()
    text_by_title = load_texts(titles)
    pid = {t: i for i, t in enumerate(titles)}
    N = len(titles)
    print(f'corpus: {N} 页（顺序取自 titles.tsv）')

    postings = collections.defaultdict(dict)
    lengths = []
    for t in titles:
        tk = tokens(t + '\n' + text_by_title[t])
        tf = collections.Counter(tk)
        lengths.append(len(tk) or 1)
        for tok, c in tf.items():
            postings[tok][pid[t]] = c
    avgdl = sum(lengths) / max(1, len(lengths))
    with open(os.path.join(DATA, 'search_index.json'), 'w', encoding='utf-8') as fh:
        json.dump({'N': N, 'avgdl': avgdl, 'postings': postings, 'lengths': lengths}, fh,
                  ensure_ascii=False)
    print('index tokens:', len(postings), 'avgdl:', round(avgdl, 1))

    # 标题索引：度数直接取 titles.tsv 第 3 列（与 graph.edges.tsv 同源同序，不再自算）
    title_rows = []
    for t in titles:
        title_rows.append({'t': t, 'g': None, 'd': deg_in_by_title.get(t, 0)})
    # group 来自 titles.tsv
    groups = {}
    with open(os.path.join(DATA, 'titles.tsv'), encoding='utf-8') as fh:
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) >= 2:
                groups[p[0]] = p[1]
    for r in title_rows:
        r['g'] = groups.get(r['t'], '其他')
    with open(os.path.join(DATA, 'title_index.json'), 'w', encoding='utf-8') as fh:
        json.dump(title_rows, fh, ensure_ascii=False)

    neigh = collections.defaultdict(list)
    with open(os.path.join(DATA, 'graph.edges.tsv'), encoding='utf-8') as fh:
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) >= 3 and p[2] != '-1':
                neigh[p[0]].append(int(p[2]))
    out = {}
    for k, v in neigh.items():
        cnt = collections.Counter(v)
        out[k] = [i for i, _ in cnt.most_common(140)]
    with open(os.path.join(DATA, 'neighbors.json'), 'w', encoding='utf-8') as fh:
        json.dump(out, fh, ensure_ascii=False)

    linkmap = {}
    for t in titles:
        for k in {t, norm_key(t), t.replace('_', ' ')}:
            linkmap.setdefault(k, t)
    with open(os.path.join(DATA, 'linkmap.json'), 'w', encoding='utf-8') as fh:
        json.dump(linkmap, fh, ensure_ascii=False)
    print('titles:', len(titles), 'neighbor nodes:', len(out))


if __name__ == '__main__':
    main()
