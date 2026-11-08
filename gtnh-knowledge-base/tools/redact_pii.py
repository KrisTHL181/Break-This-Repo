#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""可选的隐私脱敏：把维基/任务书文本里的第三方联系方式替换成占位符。

默认 **dry-run**，只报告将要改什么，不落盘。

  python tools/redact_pii.py                 # 预览
  python tools/redact_pii.py --apply         # 实际改写 kb/ 下的文本

覆盖：QQ 号 / QQ 群号 / 微信号 / 手机号 / 邮箱 / Discord 邀请

**白名单（机构与官方联系方式，不属于个人隐私，原样保留）**
  discord.gg/gtnh     GTNH 项目官方 Discord（整合包任务书自身就在引用）
  support@huiji.wiki  灰机 wiki 托管方的站点支持邮箱（公开发布的机构信箱）

改写属于对原文的修改，CC BY-NC-SA 要求分发时注明"已修改"。
本脚本在 --apply 后会提示你同步更新 NOTICE.md。
"""
import argparse
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PKG = os.path.dirname(HERE)
TARGETS = [os.path.join(PKG, 'kb')]

RULES = [
    ('QQ号', re.compile(r'(?i)\bqq\s*[:：]?\s*(?<!\d)[1-9]\d{4,11}(?!\d)'), 'QQ<已脱敏>'),
    ('QQ群号', re.compile(r'(?i)(?:qq\s*)?群\s*(?:号|号码)?\s*(?:[:：]\s*|[（(]\s*)'
                      r'(?<!\d)\d{5,12}(?!\d)\s*[)）]?'), '群<已脱敏>'),
    ('微信号', re.compile(r'(?i)(?:微信|wechat|weixin)\s*(?:号)?\s*[:：]\s*[A-Za-z][A-Za-z0-9_\-]{5,19}'),
     '微信：<已脱敏>'),
    # 手机号必须带上下文关键词：裸 11 位数字会误伤数据表数值（如材料性能表里的 16777216000）
    ('手机号', re.compile(r'(?:手机|电话|联系方式|tel|phone|call)\s*(?:号|号码)?\s*[:：]?\s*'
                      r'(?<!\d)1[3-9]\d{9}(?!\d)'), '<手机号已脱敏>'),
    # 邮箱排除"文件扩展名 TLD"：Java 的 @argfile 语法（@java9args.txt）会被误判成邮箱
    ('邮箱', re.compile(r'\b[A-Za-z0-9._%+\-]+@(?!Side\.|ae2\.net)[A-Za-z0-9.\-]+\.'
                     r'(?!txt\b|md\b|json\b|jsonl\b|cfg\b|log\b|lua\b|jar\b|zip\b|xml\b|yml\b|yaml\b|'
                     r'ini\b|py\b|js\b|ts\b|csv\b|exe\b|dll\b|bak\b|old\b|tmp\b|png\b|jpg\b)'
                     r'[A-Za-z]{2,}\b'), '<邮箱已脱敏>'),
    ('Discord邀请', re.compile(r'(?i)discord(?:app)?\.com/invite/[A-Za-z0-9\-_]+|discord\.gg/[A-Za-z0-9\-_]+'),
     'discord.gg/<已脱敏>'),
]
SKIP_EXT = ('.pyc', '.pkl', '.bin', '.png', '.jpg')
KEEP = re.compile(r'(?i)discord\.gg/gtnh\b|support@huiji\.wiki\b')
SENT = re.compile(r'\x00(\d+)\x00')


def protect(text, store):
    """先把白名单片段换成哨兵，避免被规则命中。"""
    def repl(m):
        store.append(m.group(0))
        return f'\x00{len(store) - 1}\x00'
    return KEEP.sub(repl, text)


def restore(text, store):
    return SENT.sub(lambda m: store[int(m.group(1))], text)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    a = ap.parse_args()

    kept = {}
    total = 0
    for base in TARGETS:
        for root, dirs, files in os.walk(base):
            dirs[:] = [d for d in dirs if d != '__pycache__']
            for fn in sorted(files):
                if fn.lower().endswith(SKIP_EXT):
                    continue
                p = os.path.join(root, fn)
                try:
                    s = open(p, encoding='utf-8').read()
                except (OSError, UnicodeDecodeError):
                    continue
                store = []
                new = protect(s, store)
                for w in store:
                    kept[w] = kept.get(w, 0) + 1
                hits = []
                for name, rx, rep in RULES:
                    found = rx.findall(new)
                    if found:
                        hits.append(f'{name}×{len(found)}')
                        new = rx.sub(rep, new)
                new = restore(new, store)
                if new != s:
                    total += sum(int(h.split('×')[1]) for h in hits)
                    print(f'{"改写" if a.apply else "将改写"} '
                          f'{os.path.relpath(p, PKG)}  ({", ".join(hits)})')
                    if a.apply:
                        open(p, 'w', encoding='utf-8', newline='').write(new)

    print(f'\n合计 {total} 处联系方式。')
    if kept:
        print('白名单保留（机构/官方，非个人隐私）：')
        for k, v in sorted(kept.items()):
            print(f'   {k}  ×{v}')
    if a.apply:
        print('\n已落盘。请同步在 NOTICE.md 注明"已对文本作脱敏修改"（CC BY 要求）。')
    else:
        print('这是预览；加 --apply 才会真正改写。')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
