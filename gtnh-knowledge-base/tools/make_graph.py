#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把知识库的链接图渲染成"细胞连接图"（自包含，无 CDN、无外部依赖、无游戏素材）。

输入（由 kbweb/build_site.py 产出）:
  <data>/titles.tsv        title \t group \t 入度 \t 出度 \t stub
  <data>/graph.edges.tsv   src_id \t dst_title \t dst_id(-1=红链)

输出（默认写到 <包根>/graph/）:
  graph.svg    矢量图，可直接看/嵌网页/打印
  graph.html   带说明的查看页（内嵌 SVG，无 JS 依赖）
  graph.json   {nodes, links} 供 d3 / cytoscape 二次利用
  GRAPH.md     Mermaid 版局部图 + 用法（GitHub 原生渲染 Mermaid）

用法:
  python tools/make_graph.py                          # 用包内数据
  python tools/make_graph.py --data D:\\full\\kbweb\\data --nodes 900
  python tools/make_graph.py --nodes 300 --edges 6000 --label-top 80
"""
import argparse
import collections
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PKG = os.path.dirname(HERE)

# 分类配色（与前端分组一致）
GROUP_COLOR = {
    '教程': '#ff8a3d', '产线': '#ffd23f', '机器': '#3ddc97', '机制': '#4ea8ff',
    '材料': '#c77dff', '任务': '#ff5d73', '枢纽': '#00e5ff', '索引': '#9aa5b1',
    '其他': '#6b7a8f',
}
FALLBACK = '#6b7a8f'


def load(data_dir):
    titles, groups, deg = [], {}, {}
    with open(os.path.join(data_dir, 'titles.tsv'), encoding='utf-8') as fh:
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) < 3 or not p[0]:
                continue
            titles.append(p[0])
            groups[p[0]] = p[1] or '其他'
            try:
                deg[p[0]] = int(p[2])
            except ValueError:
                deg[p[0]] = 0
    adj = collections.Counter()
    with open(os.path.join(data_dir, 'graph.edges.tsv'), encoding='utf-8') as fh:
        for line in fh:
            p = line.rstrip('\n').split('\t')
            if len(p) < 3 or p[2] == '-1':
                continue
            try:
                a, b = int(p[0]), int(p[2])
            except ValueError:
                continue
            if a == b or not (0 <= a < len(titles) and 0 <= b < len(titles)):
                continue
            adj[(a, b) if a < b else (b, a)] += 1
    return titles, groups, deg, adj


def select(titles, deg, adj, max_nodes, max_edges):
    """按度数取核心节点，再按边权取最强连边。"""
    rank = sorted(range(len(titles)), key=lambda i: -deg.get(titles[i], 0))
    keep = set(rank[:max_nodes])
    edges = [(w, a, b) for (a, b), w in adj.items() if a in keep and b in keep]
    edges.sort(key=lambda x: -x[0])
    edges = edges[:max_edges]
    used = set()
    for _, a, b in edges:
        used.add(a)
        used.add(b)
    # 丢掉没连上任何边的孤立点，图更干净
    keep &= used
    edges = [(w, a, b) for w, a, b in edges if a in keep and b in keep]
    return sorted(keep), edges


def layout(nodes, edges, iters=320, seed=7):
    """确定性力导向布局：网格近似斥力 + 边弹簧 + 向心引力。"""
    rnd = random.Random(seed)
    n = len(nodes)
    pos = {}
    R = math.sqrt(n) * 10.0
    for k, i in enumerate(nodes):           # 初始：黄金角螺旋，避免对称塌缩
        t = k * 2.399963
        r = R * math.sqrt((k + 0.5) / n)
        pos[i] = [r * math.cos(t) + rnd.uniform(-1, 1), r * math.sin(t) + rnd.uniform(-1, 1)]

    links = [(a, b, 1.0 / (1.0 + math.log(1 + w))) for w, a, b in edges]
    adj = collections.defaultdict(list)
    for a, b, d in links:
        adj[a].append((b, d))
        adj[b].append((a, d))

    k_rep = R * 0.55
    k_spring = 0.06
    gravity = 0.012
    temp = R * 0.35
    cool = 0.985
    cell = k_rep * 1.6

    for _ in range(iters):
        grid = collections.defaultdict(list)
        for i in nodes:
            x, y = pos[i]
            grid[(int(x / cell), int(y / cell))].append(i)
        disp = {i: [0.0, 0.0] for i in nodes}
        for i in nodes:
            xi, yi = pos[i]
            gx, gy = int(xi / cell), int(yi / cell)
            for ox in (-2, -1, 0, 1, 2):
                for oy in (-2, -1, 0, 1, 2):
                    for j in grid.get((gx + ox, gy + oy), ()):
                        if j == i:
                            continue
                        dx, dy = xi - pos[j][0], yi - pos[j][1]
                        d2 = dx * dx + dy * dy + 0.01
                        if d2 > (cell * 2.5) ** 2:
                            continue
                        f = (k_rep * k_rep) / d2
                        d = math.sqrt(d2)
                        disp[i][0] += dx / d * f
                        disp[i][1] += dy / d * f
        for a, b, d in links:
            dx, dy = pos[a][0] - pos[b][0], pos[a][1] - pos[b][1]
            dist = math.sqrt(dx * dx + dy * dy) + 0.01
            f = (dist - d * R * 0.05) * k_spring
            ux, uy = dx / dist, dy / dist
            disp[a][0] -= ux * f
            disp[a][1] -= uy * f
            disp[b][0] += ux * f
            disp[b][1] += uy * f
        for i in nodes:
            dx, dy = disp[i]
            disp[i][0] -= pos[i][0] * gravity
            disp[i][1] -= pos[i][1] * gravity
            d = math.hypot(*disp[i]) + 1e-9
            step = min(d, temp)
            pos[i][0] += disp[i][0] / d * step
            pos[i][1] += disp[i][1] / d * step
        temp *= cool
    return pos


def render_svg(titles, groups, deg, nodes, edges, pos, label_top, width=1600):
    xs = [pos[i][0] for i in nodes]
    ys = [pos[i][1] for i in nodes]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny) or 1.0
    pad = 70
    scale = (width - pad * 2) / span
    height = int(span * scale + pad * 2)

    def P(i):
        return (pad + (pos[i][0] - minx) * scale, pad + (pos[i][1] - miny) * scale)

    used_groups = {groups.get(titles[i], '其他') for i in nodes}
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
           f'viewBox="0 0 {width} {height}" font-family="Segoe UI,Noto Sans SC,sans-serif">',
           f'<rect width="{width}" height="{height}" fill="#0d1117"/>']
    # 边
    out.append('<g stroke="#7d8ea3" stroke-width="0.6" stroke-opacity="0.13">')
    for w, a, b in edges:
        x1, y1 = P(a)
        x2, y2 = P(b)
        out.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}"/>')
    out.append('</g>')
    # 节点
    rmax = max((deg.get(titles[i], 0) for i in nodes), default=1) or 1
    for i in nodes:
        x, y = P(i)
        d = deg.get(titles[i], 0)
        r = 2.0 + 7.0 * (math.log1p(d) / math.log1p(rmax))
        g = groups.get(titles[i], '其他')
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r:.2f}" '
                   f'fill="{GROUP_COLOR.get(g, FALLBACK)}" fill-opacity="0.88"/>')
    # 标签：度数最高的若干
    top = sorted(nodes, key=lambda i: -deg.get(titles[i], 0))[:label_top]
    for i in top:
        x, y = P(i)
        name = titles[i].replace('&', '&amp;').replace('<', '&lt;')
        out.append(f'<text x="{x:.1f}" y="{y - 8:.1f}" fill="#e6edf3" font-size="11" '
                   f'text-anchor="middle" paint-order="stroke" stroke="#0d1117" '
                   f'stroke-width="3">{name}</text>')
    # 图例
    lx, ly = 18, 20
    out.append(f'<text x="{lx}" y="{ly}" fill="#e6edf3" font-size="15" font-weight="600">'
               f'GTNH 知识库 · 细胞连接图</text>')
    out.append(f'<text x="{lx}" y="{ly + 19}" fill="#8b949e" font-size="11">'
               f'{len(nodes)} 个条目 / {len(edges)} 条互链（按度数取核心子图）</text>')
    for idx, g in enumerate(sorted(used_groups)):
        yy = ly + 44 + idx * 18
        out.append(f'<circle cx="{lx + 5}" cy="{yy - 4}" r="5" '
                   f'fill="{GROUP_COLOR.get(g, FALLBACK)}"/>')
        out.append(f'<text x="{lx + 18}" y="{yy}" fill="#c9d1d9" font-size="12">{g}</text>')
    out.append('</svg>')
    return '\n'.join(out), height


def render_mermaid(titles, deg, nodes, edges, top=22):
    rank = sorted(nodes, key=lambda i: -deg.get(titles[i], 0))[:top]
    keep = set(rank)
    lines = ['```mermaid', 'graph LR']
    for i in rank:
        name = titles[i].replace('"', "'")
        lines.append(f'  N{i}["{name}"]')
    seen = set()
    for w, a, b in edges:
        if a in keep and b in keep and w >= 2:
            key = (a, b) if a < b else (b, a)
            if key in seen:
                continue
            seen.add(key)
            lines.append(f'  N{a} --> N{b}')
            if len(seen) >= 40:
                break
    lines.append('```')
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--data', default=os.path.join(PKG, 'kbweb', 'data'))
    ap.add_argument('--out', default=os.path.join(PKG, 'graph'))
    ap.add_argument('--nodes', type=int, default=700)
    ap.add_argument('--edges', type=int, default=18000)
    ap.add_argument('--label-top', type=int, default=70)
    ap.add_argument('--iters', type=int, default=320)
    a = ap.parse_args()

    titles, groups, deg, adj = load(a.data)
    print(f'载入 {len(titles)} 节点 / {len(adj)} 条无向边')
    nodes, edges = select(titles, deg, adj, a.nodes, a.edges)
    print(f'核心子图：{len(nodes)} 节点 / {len(edges)} 边')
    pos = layout(nodes, edges, iters=a.iters)

    os.makedirs(a.out, exist_ok=True)
    svg, height = render_svg(titles, groups, deg, nodes, edges, pos, a.label_top)
    open(os.path.join(a.out, 'graph.svg'), 'w', encoding='utf-8').write(svg)

    html = f"""<!doctype html>
<html lang="zh-CN"><meta charset="utf-8">
<title>GTNH 知识库 · 细胞连接图</title>
<style>
 body{{margin:0;background:#0d1117;color:#c9d1d9;font:14px/1.6 "Segoe UI",system-ui,sans-serif}}
 header{{padding:18px 24px;border-bottom:1px solid #21262d}}
 h1{{margin:0 0 6px;font-size:18px}} p{{margin:0;color:#8b949e;font-size:13px}}
 .wrap{{padding:16px;overflow:auto}} svg{{max-width:100%;height:auto}}
</style>
<header>
 <h1>GTNH 知识库 · 细胞连接图</h1>
 <p>本图为条目之间的互链关系（非提交谱系）。数据由 kbweb/build_site.py 产出，
    颜色按主题分组，节点大小按入度。用鼠标滚轮/触控板缩放查看。</p>
</header>
<div class="wrap">{svg}</div>
</html>
"""
    open(os.path.join(a.out, 'graph.html'), 'w', encoding='utf-8').write(html)

    json.dump({'nodes': [{'id': i, 't': titles[i], 'g': groups.get(titles[i]),
                          'd': deg.get(titles[i], 0)} for i in nodes],
               'links': [{'s': a2, 't': b2, 'w': w} for w, a2, b2 in edges]},
              open(os.path.join(a.out, 'graph.json'), 'w', encoding='utf-8'),
              ensure_ascii=False)

    md = f"""# 细胞连接图

把知识库里条目的互链关系画成一张图。生成器：`tools/make_graph.py`（纯标准库，无 CDN）。

## 文件

| 文件 | 说明 |
|---|---|
| `graph.svg` | 矢量图（{len(nodes)} 节点 / {len(edges)} 边），可打印、可嵌网页 |
| `graph.html` | 查看页，内嵌 SVG，双击即可看 |
| `graph.json` | `{{nodes, links}}`，喂给 d3 / cytoscape / Gephi |
| `GRAPH.md` | 本文件 |

## 重新生成

```bash
python kbweb/build_site.py     # 先产出 titles.tsv / graph.edges.tsv
python tools/make_graph.py     # 默认取度数最高的 700 个节点
python tools/make_graph.py --nodes 1500 --edges 40000 --label-top 120
```

> 图密度取决于是否安装了维基 HTML 镜像（见 README）。
> 只有纯文字数据时，链接边很少，图会显得稀疏；装上镜像后可达 20 万条边。

## Mermaid 版（GitHub 原生渲染）

GitHub 不提供 Obsidian 那样的内容链接图，但它会渲染 Markdown 里的 Mermaid。
下面是度数最高的 {min(22, len(nodes))} 个条目的局部图：

{render_mermaid(titles, deg, nodes, edges)}
"""
    open(os.path.join(a.out, 'GRAPH.md'), 'w', encoding='utf-8').write(md)

    print('已写出:', a.out)
    for f in ('graph.svg', 'graph.html', 'graph.json', 'GRAPH.md'):
        p = os.path.join(a.out, f)
        print(f'   {os.path.getsize(p)/1024:9.1f} KB  {f}')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
