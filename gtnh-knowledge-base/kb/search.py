#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""GTNH 本地知识库检索引擎.

用法:
    python kb/search.py 青铜                       # 关键词检索
    python kb/search.py "钨 采矿场" --top 8         # 多词(OR 加权)
    python kb/search.py 电解 --ns wiki.data         # 限定来源
    python kb/search.py 蒸汽 --full                 # 显示完整命中段落
    python kb/search.py --item 青铜锭                # 物品名反查 id
    python kb/search.py --ask "怎么处理钨矿"         # 打印可粘贴给 AI 的上下文包
    python kb/search.py --rebuild                   # 重建索引

索引缓存: kb/data/index.pkl
"""
import os, re, sys, json, pickle, argparse, math
from collections import defaultdict

try:  # Windows 控制台可能是 GBK，强制 UTF-8 输出
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

ROOT = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(ROOT)
WIKI_DIR = os.path.join(ROOT, 'wiki')
QUEST_DIRS = [os.path.join(ROOT, 'quests'), os.path.join(WS, '_digest')]
IDMAP = os.path.join(ROOT, 'data', 'item_names_zh.tsv')
if not os.path.exists(IDMAP):
    IDMAP = os.path.join(ROOT, 'data', 'item_names.tsv')
if not os.path.exists(IDMAP):
    IDMAP = os.path.join(WS, '_digest', 'idmap.tsv')
DATA = os.path.join(ROOT, 'data')
INDEX_PKL = os.path.join(DATA, 'index.pkl')

CJK = re.compile(r'[\u4e00-\u9fff]')
TOKEN = re.compile(r'[a-z0-9_\.\-]{2,}')
STOP = set('the and for with you your are can use using this that from will not has have its into out any all get got make made ones one two when then than them our you\'ll'.split())


def token_weights(text):
    """返回 {token: weight}；中文二元词权重远高于单字，避免单字噪声。"""
    text = text.lower()
    w = {}
    for t in TOKEN.findall(text):
        if t in STOP:
            continue
        w[t] = max(w.get(t, 0), 2.0)
    cjk = CJK.findall(text)
    for c in cjk:
        w[c] = max(w.get(c, 0), 0.25)
    for i in range(len(cjk) - 1):
        bg = cjk[i] + cjk[i + 1]
        w[bg] = max(w.get(bg, 0), 6.0)
    return w


def tokens(text):
    return list(token_weights(text).keys())


def load_docs():
    docs = []
    # wiki jsonl
    if os.path.isdir(WIKI_DIR):
        for fn in sorted(os.listdir(WIKI_DIR)):
            if not fn.endswith('.jsonl'):
                continue
            # ns_main.jsonl 可能被切成 ns_main_partK.jsonl 分片：命名空间取 _part 之前
            ns = fn[3:-6].split('_part')[0]
            with open(os.path.join(WIKI_DIR, fn), encoding='utf-8') as fh:
                for line in fh:
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        r = json.loads(line)
                    except Exception:
                        continue
                    docs.append({'src': 'wiki.' + ns, 'title': r.get('title', ''),
                                 'ref': r.get('file', ''), 'text': r.get('text', '')})
    # quest digests
    for d in QUEST_DIRS:
        if not os.path.isdir(d):
            continue
        for fn in sorted(os.listdir(d)):
            if not fn.endswith('.txt') or not fn.startswith('line_'):
                continue
            p = os.path.join(d, fn)
            with open(p, encoding='utf-8') as fh:
                txt = fh.read()
            line_name = fn[5:-4] if fn.startswith('line_') else fn
            # split into per-quest chunks
            chunks = re.split(r'\n(?=### \[)', txt)
            for c in chunks:
                c = c.strip()
                if len(c) < 30:
                    continue
                m = re.match(r'### \[[^\]]*\]\s*(.*)', c)
                title = (m.group(1).strip() if m else line_name)[:80]
                docs.append({'src': 'quest.' + line_name, 'title': title,
                             'ref': fn, 'text': c})
    # config/reference data
    refdir = os.path.join(ROOT, 'ref')
    if os.path.isdir(refdir):
        for fn in sorted(os.listdir(refdir)):
            p = os.path.join(refdir, fn)
            if not os.path.isfile(p):
                continue
            with open(p, encoding='utf-8', errors='replace') as fh:
                docs.append({'src': 'ref.' + os.path.splitext(fn)[0], 'title': fn,
                             'ref': fn, 'text': fh.read()[:200000]})
    return docs


def build(docs):
    post = defaultdict(dict)      # term -> doc -> tf
    lengths = []
    for i, d in enumerate(docs):
        tk = tokens(d['title'] + '\n' + d['text'])
        tf = defaultdict(int)
        for t in tk:
            tf[t] += 1
        for t, n in tf.items():
            post[t][i] = n
        lengths.append(len(tk) or 1)
    return {'post': dict(post), 'len': lengths, 'n': len(docs)}


def load_or_build(rebuild=False):
    if not rebuild and os.path.exists(INDEX_PKL):
        try:
            with open(INDEX_PKL, 'rb') as fh:
                cache = pickle.load(fh)
            if cache.get('version') == 4:
                return cache['docs'], cache['idx']
        except Exception:
            pass
    docs = load_docs()
    idx = build(docs)
    os.makedirs(DATA, exist_ok=True)
    with open(INDEX_PKL, 'wb') as fh:
        pickle.dump({'version': 4, 'docs': docs, 'idx': idx}, fh)
    return docs, idx


def strip_question(q):
    """去掉口语化疑问词，保留实体关键词。"""
    for w in ('请问', '怎么样', '怎么做', '怎么', '如何', '为什么', '哪些', '哪个', '什么',
              '需要', '可以', '能不能', '是不是', '多少', '的时候', '的话', '吗', '呢', '啊'):
        q = q.replace(w, ' ')
    return re.sub(r'\s+', ' ', q).strip()


def score(docs, idx, q, ns=None):
    post = idx['post']
    q = q.strip()
    if not q:
        return []

    def keep(di):
        if not ns:
            return True
        s = docs[di]['src']
        return any(s.startswith(n) for n in ns)

    acc = defaultdict(float)
    N = idx['n']
    df_cap = max(60, int(N * 0.25))     # 排除几乎所有文档都含有的超高频词
    for t, tw in token_weights(q).items():
        pl = post.get(t)
        if not pl or len(pl) > df_cap:
            continue
        idf = math.log(1 + N / (1 + len(pl)))
        w = tw * idf
        for di, tf in pl.items():
            if not keep(di):
                continue
            acc[di] += w * (1 + math.log(tf))
    if not acc:
        return []
    # 出现位置加成：标题 > 正文开头 > 正文深处
    for di in list(acc.keys()):
        d = docs[di]
        title, text = d['title'], d['text']
        if len(q) >= 2 and q in title:
            acc[di] *= 6.0
        elif q in text[:300]:
            acc[di] *= 2.0
        elif q in text:
            acc[di] *= 1.4
    maxw = max(w for w in token_weights(q).values()) or 1.0
    return sorted(((di, s / maxw) for di, s in acc.items()), key=lambda kv: -kv[1])
    # exact-substring boost (whole query)
    if len(q) >= 2:
        ql = q.lower()
        for di in list(acc.keys()):
            if ql in docs[di]['text'].lower() or ql in docs[di]['title'].lower():
                acc[di] *= 2.2
            if ql in docs[di]['title'].lower():
                acc[di] *= 1.6
    qw = token_weights(q)
    scale = (max(qw.values()) if qw else 1.0) or 1.0
    return sorted(((di, s / scale) for di, s in acc.items()), key=lambda kv: -kv[1])


def snippet(text, q, width=340, full=False):
    if full:
        return text[:4000]
    low = text.lower()
    ql = q.lower()
    pos = low.find(ql)
    if pos < 0:
        cands = sorted((t for t in token_weights(q) if len(t) >= 2), key=len, reverse=True)
        for t in cands:
            pos = low.find(t)
            if pos >= 0:
                break
    if pos < 0:
        pos = 0
    start = max(0, pos - width // 3)
    end = min(len(text), start + width)
    s = text[start:end].replace('\n', ' ')
    s = re.sub(r'\s+', ' ', s)
    return ('…' if start else '') + s + ('…' if end < len(text) else '')


def clean_text(t):
    """去掉 wiki 抓取残留的 script/style JSON 噪音行."""
    out = []
    for ln in t.split('\n'):
        s = ln.strip()
        if not s:
            continue
        braces = s.count('{') + s.count('}')
        if braces >= 2 and braces * 12 > len(s):
            continue
        if s.startswith('{"') or s.startswith('mw-parser-output'):
            continue
        if re.match(r'^(var|function)\s', s):
            continue
        out.append(s)
    return '\n'.join(out)


def title_match_ratio(title, q):
    """标题与查询的匹配程度 0~1：命中字符占比 + 完全包含奖励。"""
    qc = [c for c in q if CJK.match(c) or c.isalnum()]
    if not qc:
        return 0.0
    hit = sum(1 for c in qc if c in title)
    ratio = hit / len(qc)
    if len(q) >= 3 and q in title:
        ratio = 1.0
    return ratio


def do_search(docs, idx, query, top, ns, full, json_out=False):
    hits = []
    seen_fp = set()
    q = query.strip()
    cands = []
    for di, sc in score(docs, idx, query, ns):
        d = docs[di]
        ratio = title_match_ratio(d['title'], q)
        if ratio >= 0.6:
            sc *= 1.0 + (0.6 * ratio if len(q) >= 2 else 0.15)
        cands.append((sc, d))
        if len(cands) >= 600:
            break
    cands.sort(key=lambda kv: -kv[0])
    for sc, d in cands:
        fp = d['text'][:400]
        if fp in seen_fp:
            continue
        seen_fp.add(fp)
        hits.append((sc, d))
        if len(hits) >= top:
            break
    if json_out:
        print(json.dumps([{'score': round(s, 2), 'src': d['src'], 'title': d['title'],
                           'ref': d['ref'], 'snippet': snippet(clean_text(d['text']), query, full=full)}
                          for s, d in hits], ensure_ascii=False, indent=1))
        return hits
    for sc, d in hits:
        print(f"\n── [{d['src']}] {d['title']}  (score {sc:.1f})  {d['ref']}")
        print('   ' + snippet(clean_text(d['text']), query, full=full))
    print(f"\n共 {len(hits)} 条（查询: {query}）" if hits else f"\n无结果: {query}")
    return hits


def do_item(term):
    if not os.path.exists(IDMAP):
        print('缺少 idmap.tsv，请先运行 _tools/build-idmap2.ps1')
        return
    n = 0
    with open(IDMAP, encoding='utf-8') as fh:
        for line in fh:
            if term.lower() in line.lower():
                k, _, v = line.rstrip('\n').partition('\t')
                print(f'{k}\t{v}')
                n += 1
                if n > 60:
                    print('…(截断)')
                    break
    if n == 0:
        print(f'未找到: {term}')


def do_ask(docs, idx, question, top=6, ns=None):
    print('=== GTNH 知识库上下文包 ===')
    print(f'问题: {question}\n')
    stripped = strip_question(question)
    terms = [question]
    if stripped and stripped != question:
        terms.append(stripped)
    for t in [question, stripped]:
        terms += [x for x in re.split(r'[\s,，。;；、]+', t) if len(x) >= 4 or re.search(r'[A-Za-z]{3,}', x)]
    seen_key, seen_fp = set(), set()
    printed = 0
    for qi in terms:
        qi = qi.strip()
        if len(qi) < 2 or qi in seen_key:
            continue
        seen_key.add(qi)
        got = 0
        for di, sc in score(docs, idx, qi, ns):
            d = docs[di]
            fp = d['text'][:300]
            if fp in seen_fp:
                continue
            seen_fp.add(fp)
            print(f'--- 来源: {d["src"]} / {d["title"]} ({d["ref"]})  相关度 {sc:.0f}')
            print(snippet(clean_text(d['text']), qi, width=1100))
            print()
            got += 1
            printed += 1
            if got >= 3:
                break
    if printed == 0:
        print('（无命中）请换用更具体的实体名，例如材料名/机器名/任务线名。')
    print('=== 上下文包结束 ===')


def do_stats(docs):
    from collections import Counter
    src = Counter(d['src'] for d in docs)
    print(f'知识库文档块: {len(docs)}')
    for k, v in sorted(src.items()):
        print(f'  {k:<28} {v}')
    p = os.path.join(DATA, 'index.pkl')
    if os.path.exists(p):
        print(f'索引缓存: {p}  {os.path.getsize(p)/1048576:.1f} MB')


def do_exact(docs, query, limit=200):
    q = query.strip()
    if not q:
        return
    hits = []
    for d in docs:
        if q in d['title']:
            hits.append(d)
    hits.sort(key=lambda d: (len(d['title']), d['title']))
    if not hits:
        print(f'没有标题包含「{q}」的条目；去掉 --exact 可做全文检索。')
        return
    print(f'标题含「{q}」的条目共 {len(hits)} 条：\n')
    for d in hits[:limit]:
        print(f"  [{d['src']}] {d['title']}   ({d['ref']})")
    if len(hits) > limit:
        print(f'  …还有 {len(hits) - limit} 条')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('query', nargs='*')
    ap.add_argument('--top', type=int, default=6)
    ap.add_argument('--ns', action='append', default=None)
    ap.add_argument('--full', action='store_true')
    ap.add_argument('--exact', action='store_true', help='只列出标题完全命中查询的页面（相当于查词条目录）')
    ap.add_argument('--json', action='store_true')
    ap.add_argument('--item', metavar='TERM')
    ap.add_argument('--ask', metavar='QUESTION')
    ap.add_argument('--rebuild', action='store_true')
    ap.add_argument('--stats', action='store_true')
    a = ap.parse_args()

    if a.item:
        do_item(a.item)
        return
    docs, idx = load_or_build(a.rebuild)
    if not docs:
        print('知识库为空：先运行 python _tools/wiki_fast.py 生成 kb/wiki/*.jsonl')
        return
    if a.stats:
        do_stats(docs)
        return
    if a.ask:
        do_ask(docs, idx, a.ask, a.top, a.ns)
        return
    q = ' '.join(a.query).strip()
    if a.exact:
        do_exact(docs, q)
        return
    if not q:
        ap.print_help()
        return
    do_search(docs, idx, q, a.top, a.ns, a.full, a.json)


if __name__ == '__main__':
    main()

