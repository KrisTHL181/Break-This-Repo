#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把 data/pages/*.txt 打包成单个 pages.bin（服务端用 os.pread 随机读取）。"""
import os, sys
sys.stdout.reconfigure(encoding='utf-8')
DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'kbweb', 'data')
PDIR = os.path.join(DATA, 'pages')
OUT = os.path.join(DATA, 'pages.bin')
IDX = os.path.join(DATA, 'pages.idx')

titles = []
for line in open(IDX, encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    if len(p) == 2:
        titles.append(p[0])

off = 0
lines = []
with open(OUT, 'wb') as out:
    for t in titles:
        p = os.path.join(PDIR, t + '.txt')
        if os.path.exists(p):
            b = open(p, 'rb').read()
        else:
            b = b''
        out.write(b)
        lines.append(f'{t}\t{off}\t{len(b)}')
        off += len(b)
with open(IDX, 'w', encoding='utf-8') as fh:
    fh.write('\n'.join(lines))
print(f'packed {len(titles)} pages -> {OUT} ({off/1048576:.1f} MB)')
