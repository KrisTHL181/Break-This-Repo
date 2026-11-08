#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Fast extractor: turn the offline GTNH wiki HTML mirror into a searchable JSONL knowledge base.

Output: kb/wiki/ns_<namespace>.jsonl  (one JSON object per page)

2026-09-17：表格改为输出 markdown 表格（表头 + 分隔行 + 数据行）。
原先 wiki HTML 的 <td> 之间带换行，`<td>`→' | ' 后每个单元格各自成行，
表格被压成一串孤立数字（例如「硅岩 / 131072 / ZPM / 2 / 4」看不懂是什么），
现在连同表头里的单位（最大电压/V、线损/(V/m·A)…）一起保留。
"""
import os, re, sys, json, html

PKG = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# 维基 HTML 镜像目录（可选，自行准备），用环境变量 GTNH_KB_WIKI_HTML 覆盖
ROOT = os.environ.get('GTNH_KB_WIKI_HTML', os.path.join(PKG, 'wiki-html', 'pages'))
OUT = os.environ.get('GTNH_KB_WIKI_OUT', os.path.join(PKG, 'kb', 'wiki'))
NS = ['main', 'data', 'template', 'module', 'category', 'project', 'html', 'form',
      'gadget', 'mediawiki', 'property', 'help']

SCRIPT_STYLE = re.compile(r'<(script|style)\b[^>]*>.*?</\1>', re.S | re.I)
BLOCK_END = re.compile(r'</?(?:br|p|div|li|ul|ol|tr|table|h[1-6]|tbody|thead|dd|dt|blockquote|pre)\b[^>]*>', re.I)
CELL = re.compile(r'</?(?:td|th)\b[^>]*>', re.I)
ANYTAG = re.compile(r'<[^>]{0,4000}>')
WS = re.compile(r'[ \t\u00a0]+')
NL = re.compile(r'\n\s*\n\s*\n+')
TITLE = re.compile(r'<h1[^>]*id="firstHeading"[^>]*>(.*?)</h1>', re.S | re.I)
IMAGE = re.compile(r'!\[[^\]]*\]\([^)]*\)')
NOISE_LINE = re.compile(r'^[\|\-–—•·\s]{1,6}$')

TABLE = re.compile(r'<table\b[^>]*>.*?</table>', re.S | re.I)
ROW = re.compile(r'<tr\b[^>]*>(.*?)</tr>', re.S | re.I)
CELLS = re.compile(r'<(t[hd])\b[^>]*>(.*?)</\1>', re.S | re.I)
SEP_ROW = re.compile(r'^\|(?:\s*:?-{2,}:?\s*\|)+$')
TABLE_SKIP = ('navbox', 'metadata', 'ambox', 'messagebox', 'toccolours')
MAX_ROWS = 400
PH = '\x00T%d\x00'
PH_RE = re.compile(r'\x00T(\d+)\x00')


def strip_json(s: str) -> str:
    """去掉物品模板残留的 {"tooltips":…} 之类的 JSON 块。"""
    out, i, n = [], 0, len(s)
    while i < n:
        if s[i] == '{':
            depth, j = 0, i
            while j < n:
                if s[j] == '{':
                    depth += 1
                elif s[j] == '}':
                    depth -= 1
                    if depth == 0:
                        break
                j += 1
            i = j + 1
            out.append(' ')
            continue
        out.append(s[i])
        i += 1
    return ''.join(out)


def cell_text(frag: str) -> str:
    s = re.sub(r'<br\s*/?>', ' / ', frag, flags=re.I)
    s = SCRIPT_STYLE.sub(' ', s)
    s = ANYTAG.sub('', s)
    s = html.unescape(s)
    s = strip_json(s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s.replace('|', '/')


def table_to_md(tbl: str) -> str:
    """<table> -> markdown 表格（首行当表头，第二行是分隔行，列数按最宽行补齐）。"""
    raw_rows = []
    for r in ROW.findall(tbl):
        cells = [cell_text(c[1]) for c in CELLS.findall(r)]
        if not cells:
            continue
        if len(cells) == 1 and len(cells[0]) > 120:
            continue                      # 物品图集那种超长单格，属于噪声
        raw_rows.append(cells)
    if len(raw_rows) < 2:
        return ''
    width = min(max(len(r) for r in raw_rows), 12)
    rows = [r + [''] * (width - len(r)) for r in raw_rows]
    width = len(rows[0])
    nz = lambda r: sum(1 for x in r if x.strip())
    caption = None
    # 折叠标题 / 分组表头（「折叠导线与线缆一览表（2.8.x）」或「现实 | Gregtech」）：
    # 真正的表头在下一行，把它提上来当表头，原标题单独成行放在表格上方。
    if width >= 4 and nz(rows[0]) <= 2 and nz(rows[1]) > nz(rows[0]):
        caption = ' '.join(x for x in rows[0] if x.strip())
        rows = rows[1:]
    out = ([caption, ''] if caption else []) + [
        '| ' + ' | '.join(rows[0]) + ' |',
        '| ' + ' | '.join('---' for _ in range(width)) + ' |']
    body = [r for r in rows[1:] if any(x.strip() for x in r)]
    for r in body[:MAX_ROWS]:
        out.append('| ' + ' | '.join(r) + ' |')
    if len(body) > MAX_ROWS:
        out.append(f'| ……（共 {len(body)} 行，此处显示前 {MAX_ROWS} 行） |')
    return '\n'.join(out)


def to_text(body: str) -> str:
    body = SCRIPT_STYLE.sub(' ', body)
    body = re.sub(r'<!--.*?-->', ' ', body, flags=re.S)

    # 先把表格抽出来转成 markdown，原地留占位符（通用标签剥离会破坏单元格结构）
    tables = []

    def _grab(m):
        t = m.group(0)
        head = t[:300].lower()
        tables.append('' if any(k in head for k in TABLE_SKIP) else table_to_md(t))
        return '\n\n' + (PH % (len(tables) - 1)) + '\n\n'

    body = TABLE.sub(_grab, body)

    body = BLOCK_END.sub('\n', body)
    body = CELL.sub(' | ', body)          # 残留的孤立单元格（不属 <table>）
    body = ANYTAG.sub('', body)
    body = html.unescape(body)
    body = WS.sub(' ', body)
    lines = []
    for ln in body.split('\n'):
        s = ln.strip()
        if not s:
            continue
        if s.startswith('\x00T'):         # 表格占位符原样保留
            lines.append(s)
            continue
        s = s.strip(' |').strip()
        if not s or NOISE_LINE.match(s):
            continue
        lines.append(s)
    txt = '\n'.join(lines)
    txt = NL.sub('\n\n', txt)

    # 回填表格：前后各留一个空行，Obsidian / 前端才认得出是表格
    def _put(m):
        md = tables[int(m.group(1))]
        return ('\n' + md + '\n') if md else '\n'

    txt = PH_RE.sub(_put, txt)
    txt = NL.sub('\n\n', txt)
    return txt.strip()


def one(args):
    ns, path = args
    try:
        with open(path, 'r', encoding='utf-8', errors='replace') as fh:
            raw = fh.read()
    except Exception:
        return None
    i = raw.find('mw-parser-output')
    if i < 0:
        return None
    gt = raw.find('>', i)
    if gt > 0:
        i = gt + 1
    end = raw.find('class="printfooter"', i)
    if end < 0:
        end = raw.find('id="catlinks"', i)
    if end < 0:
        end = min(len(raw), i + 900_000)
    body = raw[i:end]
    m = TITLE.search(raw)
    title = html.unescape(ANYTAG.sub('', m.group(1))).strip() if m else os.path.splitext(os.path.basename(path))[0]
    txt = to_text(body)
    txt = re.sub(r'<[^>\n]{0,60}>\s*$', '', txt).strip()
    if len(txt) < 20:
        return None
    return {'ns': ns, 'title': title, 'file': os.path.basename(path), 'text': txt[:12000]}


def do_ns(ns):
    d = os.path.join(ROOT, ns)
    if not os.path.isdir(d):
        return ns, 0, 0
    jobs = [(ns, os.path.join(d, f)) for f in sorted(os.listdir(d)) if f.endswith('.html')]
    n = 0
    chars = 0
    with open(os.path.join(OUT, f'ns_{ns}.jsonl'), 'w', encoding='utf-8') as out:
        for job in jobs:
            rec = one(job)
            if rec:
                out.write(json.dumps(rec, ensure_ascii=False) + '\n')
                n += 1
                chars += len(rec['text'])
    return ns, n, chars


def main():
    os.makedirs(OUT, exist_ok=True)
    only = sys.argv[1:] or NS
    for ns in only:
        if ns not in NS:
            continue
        ns, n, chars = do_ns(ns)
        print(f'{ns}: pages={n} chars={chars//1024}KB', flush=True)


if __name__ == '__main__':
    main()
