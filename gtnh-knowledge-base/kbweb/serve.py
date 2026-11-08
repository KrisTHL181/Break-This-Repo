#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""GTNH 知识库网站服务端。

启动:  python kbweb/serve.py            (默认 http://127.0.0.1:8777)
       python kbweb/serve.py --port 9000 --no-browser
接口:  /api/meta  /api/search?q=&limit=  /api/suggest?q=
       /api/page?t=标题      /api/graph?t=标题&depth=1
       /api/questlines  /api/questline?n=名称  /api/random  /api/item?q=
静态:  /            -> kbweb/web/index.html
       /assets/<file> -> <包根>/assets/<file>
                         本包**不附带**任何 Minecraft / 模组贴图（版权原因）；
                         缺失时返回 1x1 透明占位图，页面其余部分正常。
       首次使用先建站：python kbweb/build_site.py
"""
import os, re, sys, json, math, random, argparse, threading, webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs, unquote

ROOT = os.path.dirname(os.path.abspath(__file__))
WS = os.path.dirname(ROOT)
DATA = os.path.join(ROOT, 'data')
WEB = os.path.join(ROOT, 'web')
ASSETS = os.environ.get('GTNH_KB_ASSETS') or os.path.join(WS, 'assets')
# 1x1 透明 PNG（占位图，非游戏素材）
_PNG_1x1 = bytes.fromhex(
    '89504e470d0a1a0a0000000d494844520000000100000001080600000'
    '01f15c4890000000d4944415478da63f8ffff3f0005fe02fea735c1a2'
    '0000000049454e44ae426082')

sys.stdout.reconfigure(encoding='utf-8')

CJK = re.compile(r'[\u4e00-\u9fff]')
LAT = re.compile(r'[a-z0-9_\.\-]{2,}')
STOP = set('the and for with you your are can use using this that from will not has have its into out any all get got make when then than them our'.split())

TITLES = []          # 有序标题
TITLE_ID = {}        # 标题 -> 序号
TITLE_ROWS = []      # [{t,g,d}]
META = {}
IDX = {}
NEIGH = {}
LINKMAP = {}
LINKMAP_NORM = {}
ITEM_ROWS = []
ITEM_INDEX = {}
QUEST_LINES = []
PAGE_OFFSETS = {}
HEAD = {}


def sanitize(name):
    name = (name or '').replace('&amp;', '&')
    return re.sub(r'[\\/:*?"<>|\[\]#^]', '', name).strip().strip('.') or '\u672a\u547d\u540d'


def tokens(text):
    text = text.lower()
    out = [t for t in LAT.findall(text) if t not in STOP]
    cjk = CJK.findall(text)
    out += cjk
    out += [cjk[i] + cjk[i + 1] for i in range(len(cjk) - 1)]
    return out


def norm_key(t):
    return re.sub(r'[\s_\-]+', '', t).lower()


def load():
    global TITLES, TITLE_ID, TITLE_ROWS, META, IDX, NEIGH, LINKMAP, LINKMAP_NORM, ITEM_ROWS, ITEM_INDEX, QUEST_LINES, HEAD
    print('[kbweb] 读取站点数据 ...', flush=True)
    if not os.path.exists(os.path.join(DATA, 'meta.json')):
        print('[kbweb] 还没有站点数据。请先建站：\n'
              '        python kbweb/build_site.py\n'
              '        （约 1 分钟；生成 kbweb/data/，不含任何游戏素材）', flush=True)
        sys.exit(2)
    META = json.load(open(os.path.join(DATA, 'meta.json'), encoding='utf-8'))
    ti = json.load(open(os.path.join(DATA, 'title_index.json'), encoding='utf-8'))
    TITLE_ROWS = ti
    TITLES = [r['t'] for r in ti]
    TITLE_ID = {t: i for i, t in enumerate(TITLES)}
    TITLE_META = {r['t']: r for r in ti}
    with open(os.path.join(DATA, 'pages.idx'), encoding='utf-8') as fh:
        off = 0
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) != 3:
                continue
            start, n = int(p[1]), int(p[2])
            PAGE_OFFSETS[p[0]] = (start, n)
    NEIGH = json.load(open(os.path.join(DATA, 'neighbors.json'), encoding='utf-8'))
    LINKMAP = json.load(open(os.path.join(DATA, 'linkmap.json'), encoding='utf-8'))
    LINKMAP_NORM = {norm_key(k): v for k, v in LINKMAP.items()}
    print('[kbweb] 读取检索索引（约 95MB，稍等）...', flush=True)
    IDX = json.load(open(os.path.join(DATA, 'search_index.json'), encoding='utf-8'))
    print('[kbweb] 读取物品名表 ...', flush=True)
    items_file = os.path.join(DATA, 'items.tsv')
    if not os.path.exists(items_file):
        items_file = os.path.join(WS, 'kb', 'data', 'item_names.tsv')
    if os.path.exists(items_file):
        with open(items_file, encoding='utf-8') as fh:
            for line in fh:
                k, _, v = line.rstrip('\n').partition('\t')
                if k and v:
                    ITEM_ROWS.append((k, v))
    for k, v in ITEM_ROWS:
        ITEM_INDEX.setdefault(k.lower(), []).append((k, v))
    sfile = os.path.join(DATA, 'search.jsonl')
    if os.path.exists(sfile):
        with open(sfile, encoding='utf-8') as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                r = json.loads(line)
                HEAD[r['t']] = r.get('h', '')
    for fn in sorted(os.listdir(os.path.join(DATA, 'quests'))):
        if fn.endswith('.json'):
            d = json.load(open(os.path.join(DATA, 'quests', fn), encoding='utf-8'))
            QUEST_LINES.append(d)
    META['titleMeta'] = TITLE_META
    print(f"[kbweb] 就绪：{len(TITLES)} 页 / {len(IDX['postings'])} 词 / "
          f"{META['counts']['edges']} 条链接", flush=True)


_pages_fd = None
_pages_lock = threading.Lock()


def page_text(title):
    """从 data/pages.bin 随机读取一页（Windows 无 os.pread，用加锁 seek+read）。"""
    global _pages_fd
    if title not in PAGE_OFFSETS:
        return None
    off, n = PAGE_OFFSETS[title]
    with _pages_lock:
        if _pages_fd is None:
            _pages_fd = open(os.path.join(DATA, 'pages.bin'), 'rb')
        _pages_fd.seek(off)
        return _pages_fd.read(n).decode('utf-8', 'replace')


def resolve_link(name):
    if name in TITLE_ID:
        return name
    n = LINKMAP.get(name) or LINKMAP.get(name.replace('_', ' '))
    if n:
        return n
    return LINKMAP_NORM.get(norm_key(name))


def search(q, limit=24):
    tk = tokens(q)
    if not tk:
        return []
    post = IDX['postings']
    N, avgdl = IDX['N'], IDX['avgdl']
    lengths = IDX['lengths']
    qs = set(tk)
    acc = {}
    for t in qs:
        pl = post.get(t)
        if not pl:
            continue
        df = len(pl)
        idf = math.log(1 + (N - df + 0.5) / (df + 0.5))
        weight = 6.0 if len(t) > 1 and CJK.search(t) else (2.0 if LAT.fullmatch(t) else 0.25)
        for pidstr, tf in pl.items():
            pid = int(pidstr)
            dl = lengths[pid]
            sc = idf * weight * (tf * 2.2) / (tf + 1.2 * (0.25 + 0.75 * dl / avgdl))
            acc[pid] = acc.get(pid, 0.0) + sc
    if not acc:
        return []
    ql = q.lower()
    out = []
    for pid, sc in acc.items():
        t = TITLES[pid]
        if ql and ql in t.lower():
            sc *= 6.0 if len(q) >= 2 else 1.15
        elif ql and ql in (META['titleMeta'][t].get('h') or ''):
            sc *= 1.3
        out.append((sc, t))
    out.sort(key=lambda x: -x[0])
    return out[:limit]


def suggest(q, limit=12):
    q = q.strip()
    if not q:
        return []
    ql = q.lower()
    starts, contains = [], []
    for t in TITLES:
        tl = t.lower()
        if tl.startswith(ql):
            starts.append(t)
        elif ql in tl:
            contains.append(t)
        if len(starts) >= limit:
            break
    merged = starts[:limit] + [t for t in contains if t not in starts][:max(0, limit - len(starts))]
    return merged


class Handler(BaseHTTPRequestHandler):
    server_version = 'kbweb/1.0'

    def log_message(self, fmt, *args):
        pass

    def _json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        self.end_headers()
        self.wfile.write(body)

    def _placeholder(self):
        """本包不含游戏贴图：给缺失的图片返回 1x1 透明 PNG。"""
        body = _PNG_1x1
        self.send_response(200)
        self.send_header('Content-Type', 'image/png')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'public, max-age=86400')
        self.end_headers()
        self.wfile.write(body)

    def _file(self, path, ctype):
        try:
            with open(path, 'rb') as fh:
                body = fh.read()
        except OSError:
            self.send_error(404, 'not found')
            return
        self.send_response(200)
        self.send_header('Content-Type', ctype)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'public, max-age=86400')
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        u = urlparse(self.path)
        p = unquote(u.path)
        qs = parse_qs(u.query)

        if p.startswith('/api/'):
            try:
                return self._api(p, qs)
            except Exception as e:
                return self._json({'error': str(e)}, 500)

        if p.startswith('/assets/'):
            name = os.path.basename(p[len('/assets/'):])
            ext = os.path.splitext(name)[1].lower()
            ctype = {'.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
                     '.gif': 'image/gif', '.webp': 'image/webp'}.get(ext, 'application/octet-stream')
            if not os.path.exists(os.path.join(ASSETS, name)):
                return self._placeholder()
            return self._file(os.path.join(ASSETS, name), ctype)
        rel = p.lstrip('/') or 'index.html'
        if rel == 'favicon.ico':
            return self._json({}, 204)
        target = os.path.normpath(os.path.join(WEB, rel))
        if not target.startswith(WEB):
            return self.send_error(403)
        if not os.path.exists(target):
            target = os.path.join(WEB, 'index.html')
        ext = os.path.splitext(target)[1].lower()
        ctype = {'.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8',
                 '.js': 'application/javascript; charset=utf-8', '.json': 'application/json; charset=utf-8',
                 '.svg': 'image/svg+xml'}.get(ext, 'text/plain; charset=utf-8')
        return self._file(target, ctype)

    def _api(self, p, qs):
        if p == '/api/meta':
            m = dict(META)
            m.pop('titleMeta', None)
            return self._json(m)

        if p == '/api/titles':
            return self._json({'titles': [sanitize(t) for t in TITLES]})

        if p == '/api/titleindex':
            return self._json({'rows': [{'t': r['t'], 'g': r['g'], 'd': r['d']}
                                        for r in TITLE_ROWS]})

        if p == '/api/groups':
            g = qs.get('g', [''])[0]
            rows = [{'t': r['t'], 'd': r['d']} for r in TITLE_ROWS if r['g'] == g]
            rows.sort(key=lambda r: -r['d'])
            return self._json({'g': g, 'rows': rows[:1500]})

        if p == '/api/exists':
            names = (qs.get('names', [''])[0] or '').split('|')
            have, rows = [], []
            for n in names:
                nm = resolve_link(n)
                if nm:
                    have.append(n)
                    rows.append({'t': n, 'g': META['titleMeta'].get(nm, {}).get('g')})
            return self._json({'have': have, 'rows': rows})

        if p == '/api/topgraph':
            size = max(20, min(420, int(qs.get('size', ['120'])[0])))
            focus = qs.get('focus', [''])[0]
            if focus:
                center = resolve_link(focus) or focus
                seeds = [center]
                ranked = sorted(TITLE_ROWS, key=lambda r: -r['d'])
                for r in ranked:
                    if len(seeds) >= size:
                        break
                    if r['t'] != center:
                        seeds.append(r['t'])
            else:
                center = None
                seeds = [r['t'] for r in sorted(TITLE_ROWS, key=lambda r: -r['d'])[:size]]
            keep = set(seeds)
            nodes = [{'t': t, 'g': META['titleMeta'].get(t, {}).get('g')} for t in seeds]
            idx = {t: i for i, t in enumerate(seeds)}
            links, seen = [], set()
            for t in seeds:
                for m in NEIGH.get(str(TITLE_ID.get(t, -1)), [])[:30]:
                    nm = TITLES[m] if 0 <= m < len(TITLES) else None
                    if nm and nm in keep and nm != t:
                        key = (idx[t], idx[nm]) if idx[t] < idx[nm] else (idx[nm], idx[t])
                        if key in seen:
                            continue
                        seen.add(key)
                        links.append([key[0], key[1]])
            if focus and center in idx:
                order = {n['t']: i for i, n in enumerate(nodes)}
                order[center] = 0
                # 让中心节点排在最前，其余顺延
                others = [n for n in nodes if n['t'] != center]
                nodes = [{'t': center, 'g': META['titleMeta'].get(center, {}).get('g')}] + others
                order = {n['t']: i for i, n in enumerate(nodes)}
                links, seen = [], set()
                for n in nodes:
                    t = n['t']
                    for m in NEIGH.get(str(TITLE_ID.get(t, -1)), [])[:30]:
                        nm = TITLES[m] if 0 <= m < len(TITLES) else None
                        if nm and nm in order and nm != t:
                            a2, b2 = order[t], order[nm]
                            key = (a2, b2) if a2 < b2 else (b2, a2)
                            if key in seen:
                                continue
                            seen.add(key)
                            links.append([key[0], key[1]])
            return self._json({'center': center, 'nodes': nodes, 'links': links})

        if p == '/api/suggest':
            return self._json(suggest(qs.get('q', [''])[0]))

        if p == '/api/search':
            q = qs.get('q', [''])[0]
            limit = int(qs.get('limit', ['24'])[0])
            group = qs.get('group', [''])[0]
            res = search(q, limit * 3 if group else limit)
            out = []
            for sc, t in res:
                tm = META['titleMeta'][t]
                if group and tm.get('g') != group:
                    continue
                out.append({'t': t, 'g': tm.get('g'), 'd': tm.get('d', 0), 'score': round(sc, 2),
                            'preview': HEAD.get(t, '')[:220]})
                if len(out) >= limit:
                    break
            return self._json({'q': q, 'count': len(out), 'results': out})

        if p == '/api/page':
            t = qs.get('t', [''])[0]
            name = resolve_link(t) or t
            text = page_text(name)
            if text is None:
                return self._json({'error': 'not found', 't': t}, 404)
            tm = META['titleMeta'].get(name, {})
            bl = []
            blp = os.path.join(DATA, 'backlinks', name + '.txt')
            if os.path.exists(blp):
                bl = [x for x in open(blp, encoding='utf-8').read().split('\n') if x]
            ip = os.path.join(DATA, 'images', name + '.txt')
            imgs = [x for x in open(ip, encoding='utf-8').read().split('\n') if x] if os.path.exists(ip) else []
            nid = str(TITLE_ID.get(name, -1))
            neigh = [TITLES[i] for i in NEIGH.get(nid, []) if 0 <= i < len(TITLES)]
            return self._json({'title': name, 'group': tm.get('g'), 'deg': tm.get('d', 0),
                               'text': text, 'images': imgs,
                               'backlinks': bl[:300], 'backlinkCount': len(bl),
                               'neighbors': neigh, 'requested': t,
                               'resolved': resolve_link(t) is not None or t == name})

        if p == '/api/graph':
            t = qs.get('t', [''])[0]
            depth = min(2, int(qs.get('depth', ['1'])[0]))
            limit = int(qs.get('limit', ['28'])[0])
            start = resolve_link(t) or t
            if start not in TITLE_ID:
                return self._json({'error': 'not found', 't': t}, 404)
            nodes = {start}
            frontier = [start]
            for _ in range(max(1, depth)):
                nxt = []
                for n in frontier:
                    for m in NEIGH.get(str(TITLE_ID[n]), [])[:limit]:
                        nm = TITLES[m]
                        if nm not in nodes:
                            nodes.add(nm)
                            nxt.append(nm)
                            if len(nodes) > 220:
                                break
                    if len(nodes) > 220:
                        break
                frontier = nxt
                if not frontier or len(nodes) > 220:
                    break
            nid = {n: i for i, n in enumerate(nodes)}
            links = []
            for n in nodes:
                for m in NEIGH.get(str(TITLE_ID[n]), [])[:limit]:
                    nm = TITLES[m]
                    if nm in nid and nm != n:
                        links.append([nid[n], nid[nm]])
            return self._json({'center': start,
                               'nodes': [{'t': n, 'g': META['titleMeta'].get(n, {}).get('g')} for n in nodes],
                               'links': links})

        if p == '/api/questlines':
            return self._json({'lines': META['questLines']})

        if p == '/api/questline':
            n = qs.get('n', [''])[0]
            for d in QUEST_LINES:
                if d['name'] == n:
                    return self._json(d)
            return self._json({'error': 'not found', 'n': n}, 404)

        if p == '/api/item':
            q = qs.get('q', [''])[0].strip().lower()
            if not q:
                return self._json({'rows': []})
            rows = []
            for k, v in ITEM_ROWS:
                if q in k.lower() or q in v.lower():
                    rows.append({'id': k, 'name': v})
                    if len(rows) >= 60:
                        break
            return self._json({'rows': rows})

        if p == '/api/random':
            t = random.choice(TITLES)
            return self._json({'t': t})

        return self._json({'error': 'unknown api', 'path': p}, 404)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=8777)
    ap.add_argument('--host', default='127.0.0.1')
    ap.add_argument('--no-browser', action='store_true')
    a = ap.parse_args()
    load()
    srv = ThreadingHTTPServer((a.host, a.port), Handler)
    url = f'http://{a.host}:{a.port}/'
    print(f'[kbweb] 服务已启动: {url}   (Ctrl+C 停止)', flush=True)
    if not a.no_browser:
        threading.Timer(1.0, lambda: webbrowser.open(url)).start()
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print('\n[kbweb] 已停止')


if __name__ == '__main__':
    main()

