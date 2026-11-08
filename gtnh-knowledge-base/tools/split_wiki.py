#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把过大的 kb/wiki/ns_*.jsonl 切成纯文本分片，规避 GitHub 单请求体积上限。

背景：GitHub 的 Git Blobs API 拒收过大的 JSON 请求体 —— 64 MB 的 jsonl
base64 编码后约 86 MB，会返回 `422 input was too large to process`。
切成 20 MB 左右的分片后即可正常推送（普通 `git push` 没有这个限制）。

分片命名：ns_main.jsonl -> ns_main_part1.jsonl, ns_main_part2.jsonl, …
读取端（kb/search.py、kbweb/build_site.py、kbweb/build_index.py）已透明支持，
`--ns wiki.main` 之类的来源前缀不受影响。

用法:
  python tools/split_wiki.py                     # 默认 20 MB 阈值，全量扫描
  python tools/split_wiki.py --dry-run           # 只看会切哪些文件
  python tools/split_wiki.py --mb 15             # 自定义分片大小
  python tools/split_wiki.py --merge             # 反向操作：合并回单文件
"""
import argparse
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PKG = os.path.dirname(HERE)
WIKI = os.path.join(PKG, 'kb', 'wiki')
PART = re.compile(r'^(ns_[\w\-]+?)_part\d+\.jsonl$')


def split_one(path, mb, dry):
    """按行边界切分，保证每行 JSON 不被截断。"""
    limit = int(mb * 1024 * 1024)
    base = os.path.splitext(os.path.basename(path))[0]
    if PART.match(base + '.jsonl'):
        return []
    chunks, cur, size, n = [], [], 0, 0
    with open(path, encoding='utf-8') as fh:
        for line in fh:
            cur.append(line)
            size += len(line.encode('utf-8'))
            if size >= limit:
                n += 1
                chunks.append((f'{base}_part{n}.jsonl', cur))
                cur, size = [], 0
    if cur:
        n += 1
        chunks.append((f'{base}_part{n}.jsonl', cur))
    if n <= 1:
        print(f'  {os.path.basename(path)}: 无需切分')
        return []
    print(f'  {os.path.basename(path)} -> {n} 片')
    written = []
    for name, lines in chunks:
        out = os.path.join(WIKI, name)
        sz = sum(len(x.encode('utf-8')) for x in lines)
        print(f'      {name}  {sz/1048576:6.1f} MB  {len(lines)} 行')
        if not dry:
            with open(out, 'w', encoding='utf-8', newline='') as fh:
                fh.writelines(lines)
        written.append(out)
    if not dry:
        os.remove(path)
        print(f'      已删除原文件 {os.path.basename(path)}')
    return written


def merge(mb, dry):
    for first in sorted(glob.glob(os.path.join(WIKI, 'ns_*_part1.jsonl'))):
        m = PART.match(os.path.basename(first))
        if not m:
            continue
        base = m.group(1)
        parts = sorted(glob.glob(os.path.join(WIKI, f'{base}_part*.jsonl')),
                       key=lambda p: int(re.search(r'_part(\d+)', p).group(1)))
        out = os.path.join(WIKI, f'{base}.jsonl')
        total = sum(os.path.getsize(p) for p in parts)
        print(f'  {" ".join(os.path.basename(p) for p in parts)} -> {base}.jsonl  '
              f'({total/1048576:.1f} MB)')
        if not dry:
            with open(out, 'w', encoding='utf-8', newline='') as fh:
                for p in parts:
                    with open(p, encoding='utf-8') as src:
                        for line in src:
                            fh.write(line)
            for p in parts:
                os.remove(p)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--mb', type=float, default=20.0, help='单片大小上限（MB，默认 20）')
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--merge', action='store_true', help='反向：把分片合并回单文件')
    a = ap.parse_args()

    if a.merge:
        print('合并分片：')
        merge(a.mb, a.dry_run)
        return

    print(f'扫描 {WIKI}（阈值 {a.mb} MB）')
    targets = [p for p in sorted(glob.glob(os.path.join(WIKI, 'ns_*.jsonl')))
               if not PART.match(os.path.basename(p))
               and os.path.getsize(p) > a.mb * 1024 * 1024]
    if not targets:
        print('  没有超过阈值的文件，无需切分。')
        return
    for p in targets:
        split_one(p, a.mb, a.dry_run)
    if a.dry_run:
        print('\n这是预览；去掉 --dry-run 才会真正切分。')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
