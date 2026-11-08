#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Extract readable text from the offline GTNH wiki archive (灰机wiki mirror)."""
import os, re, sys, html
from bs4 import BeautifulSoup, NavigableString, Tag

PKG = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# 维基 HTML 镜像目录（可选，自行准备），用环境变量 GTNH_KB_WIKI_HTML 覆盖
ROOT = os.environ.get('GTNH_KB_WIKI_HTML', os.path.join(PKG, 'wiki-html', 'pages'))
OUT = os.environ.get('GTNH_KB_WIKI_OUT', os.path.join(PKG, 'kb', 'wiki'))
NS_ORDER = ['main', 'data', 'template', 'module', 'category', 'project', 'html', 'form', 'gadget', 'mediawiki', 'property', 'help']

SKIP_TAGS = {'script', 'style', 'link', 'meta', 'noscript', 'sup'}
DROP_CLASSES = re.compile(r'(navbox|toc|mw-editsection|reference|noprint|mw-empty-elt|mw-cite-backlink|nav|sidebar|infobox-image|cargo-table-|mw-collapsible-toggle)')


def clean_soup(soup):
    for t in soup.find_all(list(SKIP_TAGS)):
        t.decompose()
    for t in soup.find_all(class_=DROP_CLASSES):
        t.decompose()
    for t in soup.find_all(id=re.compile(r'^(toc|mw-toc|catlinks|siteSub|jump-to-nav|footer)')):
        t.decompose()
    return soup


def conv_table(tbl, out):
    rows = tbl.find_all('tr', recursive=True)
    out.append('\n[表格]')
    for tr in rows[:40]:
        cells = []
        for c in tr.find_all(['th', 'td'], recursive=False):
            txt = ' '.join(c.get_text(' ', strip=True).split())
            if txt:
                cells.append(txt[:120])
        if cells:
            out.append(' | '.join(cells))
    out.append('[/表格]\n')


def conv(node, out):
    if isinstance(node, NavigableString):
        s = str(node)
        if s.strip():
            out.append(s)
        return
    if not isinstance(node, Tag):
        return
    name = node.name
    if name in ('script', 'style', 'link', 'meta', 'noscript'):
        return
    if name == 'table':
        conv_table(node, out)
        return
    if name == 'br':
        out.append('\n')
        return
    if name in ('p', 'div', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'tr', 'ul', 'ol', 'dt', 'dd', 'pre', 'blockquote'):
        out.append('\n')
        if name in ('h1', 'h2', 'h3', 'h4', 'h5'):
            lvl = '#' * int(name[1])
            txt = ' '.join(node.get_text(' ', strip=True).split())
            out.append(f'\n{lvl} {txt}\n')
            return
        if name == 'li':
            out.append('* ')
    for ch in node.children:
        conv(ch, out)
    if name in ('p', 'div', 'li', 'tr', 'ul', 'ol'):
        out.append('\n')


def extract(path):
    with open(path, 'r', encoding='utf-8', errors='replace') as fh:
        soup = BeautifulSoup(fh.read(), 'lxml')
    title = soup.find('h1', id='firstHeading')
    title_txt = title.get_text(' ', strip=True) if title else os.path.splitext(os.path.basename(path))[0]
    body = soup.find('div', class_='mw-parser-output') or soup.find(id='mw-content-text')
    if body is None:
        return title_txt, None
    clean_soup(body)
    out = []
    conv(body, out)
    txt = ''.join(out)
    txt = re.sub(r'[ \t\u00a0]+', ' ', txt)
    txt = re.sub(r'\n\s*\n\s*\n+', '\n\n', txt)
    lines = [ln.rstrip() for ln in txt.split('\n')]
    # drop pure-noise lines
    keep = []
    for ln in lines:
        s = ln.strip()
        if not s:
            keep.append('')
            continue
        if s in ('[表格]', '[/表格]'):
            keep.append(s); continue
        if re.fullmatch(r'[\|\-–—•·\s]+', s):
            continue
        if len(s) <= 2 and not re.match(r'^#', s):
            continue
        keep.append(s)
    txt = '\n'.join(keep)
    txt = re.sub(r'\n{3,}', '\n\n', txt).strip()
    return title_txt, txt


def main():
    os.makedirs(OUT, exist_ok=True)
    total = 0
    index = []
    for ns in NS_ORDER:
        d = os.path.join(ROOT, ns)
        if not os.path.isdir(d):
            continue
        files = sorted(os.listdir(d))
        chunks = []
        for fn in files:
            if not fn.endswith('.html'):
                continue
            p = os.path.join(d, fn)
            try:
                title, txt = extract(p)
            except Exception as e:
                txt = f'<extract error: {e}>'
                title = fn
            if not txt:
                continue
            txt = txt[:9000]
            chunks.append(f'\n===== [{ns}] {title} ({fn}) =====\n{txt}\n')
            index.append((ns, title, fn, len(txt)))
            total += 1
        with open(os.path.join(OUT, f'ns_{ns}.txt'), 'w', encoding='utf-8') as fh:
            fh.write(''.join(chunks))
        print(f'{ns}: {len(chunks)} pages, {sum(len(c) for c in chunks)//1024} KB')
    with open(os.path.join(OUT, '_index.tsv'), 'w', encoding='utf-8') as fh:
        for ns, title, fn, n in index:
            fh.write(f'{ns}\t{title}\t{fn}\t{n}\n')
    print('total pages', total)


if __name__ == '__main__':
    main()
