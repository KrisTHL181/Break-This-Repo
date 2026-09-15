#!/usr/bin/env python3
"""An offline atlas of tracked Git entries; never walks checkout paths."""

import argparse
from collections import Counter
import html
import math
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import tempfile

from meow_museum import TEMPLATE, git, resolve


def tree_entries(root, revision, limit=100_000):
    """Bound the sample while streaming NUL-delimited, literal Git paths."""
    if not 1 <= limit <= 1_000_000:
        raise ValueError("Entry limit must be between 1 and 1000000")
    records = []
    truncated = False
    with tempfile.TemporaryFile() as errors:
        process = subprocess.Popen(
            ["git", "--no-pager", "-C", str(root), "ls-tree", "--full-tree", "-r", "-z", revision],
            env={**os.environ, "GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"},
            stdout=subprocess.PIPE, stderr=errors)
        try:
            pending = b""
            while True:
                block = process.stdout.read1(65536)
                if not block:
                    break
                pending += block
                while b"\0" in pending:
                    record, pending = pending.split(b"\0", 1)
                    if len(records) == limit:
                        truncated = True
                        break
                    meta, path = record.split(b"\t", 1)
                    mode, kind, oid = meta.decode("ascii").split()
                    records.append({"mode": mode, "kind": kind, "oid": oid,
                                    "path": path.decode("utf-8", "surrogateescape")})
                if truncated:
                    break
                if len(pending) > 2_000_000:
                    raise ValueError("A Git path record exceeds the 2 MB observation limit")
            if not truncated and process.wait(timeout=60):
                errors.seek(0)
                raise ValueError(errors.read().decode("utf-8", "replace"))
        finally:
            if process.poll() is None:
                process.terminate()
            process.wait(timeout=10)
            process.stdout.close()
    return records, truncated


def local_sizes(root, records):
    ids = sorted({r["oid"] for r in records if r["kind"] == "blob" and r["mode"] != "120000"})
    if not ids:
        return {}
    result = subprocess.run(
        ["git", "-C", str(root), "cat-file", "--batch-check=%(objectname) %(objectsize)"],
        input=("\n".join(ids) + "\n").encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        env={**os.environ, "GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"}, timeout=60)
    if result.returncode:
        raise ValueError(result.stderr.decode("utf-8", "replace"))
    return {oid: int(size) for oid, size in (line.split() for line in result.stdout.decode().splitlines()) if size != "missing"}


def atlas(root, revision="HEAD", limit=100_000):
    revision = resolve(root, revision)
    records, truncated = tree_entries(root, revision, limit)
    sizes = local_sizes(root, records)
    groups = {}
    for record in records:
        path = record["path"]
        name = path.split("/", 1)[0] if "/" in path else "[根目录文件]"
        group = groups.setdefault(name, {"name": name, "files": 0, "symlinks": 0, "submodules": 0,
                                        "bytes": 0, "unknown": 0, "types": Counter()})
        if record["mode"] == "160000":
            group["submodules"] += 1
        elif record["mode"] == "120000":
            group["symlinks"] += 1
        else:
            group["files"] += 1
            group["types"][PurePosixPath(path).suffix.lower() or "[无扩展名]"] += 1
            if record["oid"] in sizes:
                group["bytes"] += sizes[record["oid"]]
            else:
                group["unknown"] += 1
    return {"revision": revision, "entries": len(records), "truncated": truncated,
            "groups": sorted(groups.values(), key=lambda x: (-x["files"], x["name"]))}


def shell(title, body, script=""):
    style = re.search(r"<style>(.*?)</style>", TEMPLATE, re.S)[1]
    extra = '''a{color:inherit}nav{display:flex;gap:22px;flex-wrap:wrap;margin:22px 0}h1{font-size:48px;letter-spacing:-1px;margin:24px 0}h3{overflow-wrap:anywhere}summary{cursor:pointer}table{border-collapse:collapse;width:100%;font-size:14px}td,th{padding:10px;border-bottom:1px solid var(--line);text-align:left;overflow-wrap:anywhere}.tile{border:1px solid var(--line);padding:22px;background:#fffdf7;border-radius:8px;min-width:0}.tile p{font-size:14px}.meter{height:6px;background:#d6f176;margin:14px 0}.note{padding:18px;background:#e9ecdf;margin:20px 0}.meta{font-size:13px;color:var(--muted);overflow-wrap:anywhere}.table-wrap{overflow:auto}#results{margin-bottom:20px}'''
    return ('<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
            f'<title>{html.escape(title)} · 仓库生态博物馆</title><style>{style}{extra}</style></head><body><main>'
            '<header><b>CHAOS OBSERVATORY / 仓库生态博物馆</b><span>持续形成中的展览</span></header>'
            '<nav aria-label="展区导航"><a href="index.html">猫爪足迹</a><a href="diary.html">消退日报</a><a href="atlas.html">仓库地图</a><a href="paths.html">奇葩路径</a></nav>'
            f'{body}<footer>只观察，不执行展品。数据来自指定 Git 提交。</footer></main><script>{script}</script></body></html>')


def render_atlas(report):
    esc = lambda value: html.escape(str(value), quote=True)
    groups = report["groups"]
    cards = []
    maximum = max((g["files"] for g in groups), default=1) or 1
    for group in groups:
        types = "".join(f'<tr><td>{esc(kind)}</td><td>{count}</td></tr>' for kind, count in group["types"].most_common())
        known = f'{group["bytes"]:,} B'
        width = max(2, round(100 * math.log1p(group["files"]) / math.log1p(maximum)))
        cards.append(f'<article class="tile" data-files="{group["files"]}" data-bytes="{group["bytes"]}"><h3>{esc(group["name"])}</h3>'
                     f'<div class="meter" style="width:{width}%" aria-hidden="true"></div><p><b>{group["files"]:,}</b> 个普通文件 · {len(group["types"])} 种扩展名</p>'
                     f'<p>已知体积 {known}<br>体积未知 {group["unknown"]:,} 个文件<br>符号链接 {group["symlinks"]} · 子模块 {group["submodules"]}</p>'
                     f'<details><summary>查看文件类型</summary><table><thead><tr><th>扩展名</th><th>文件数</th></tr></thead><tbody>{types}</tbody></table></details></article>')
    warning = "采样已截断：仅展示 Git 树遍历顺序中的前一部分条目，不能据此比较整个仓库。" if report["truncated"] else "已完整遍历该提交的 Git 树。"
    body = (f'<h1>仓库地图</h1><p>从源码沉积层到壁纸大陆，看看每片区域住着什么。</p><p class="meta">观测提交 {report["revision"]}</p>'
            f'<div class="note">{warning}<br>本次观察 {report["entries"]:,} 个 Git 条目、{len(groups)} 个分组。体积是文件的未压缩逻辑字节数；重复引用按路径计数，不代表磁盘占用。未知体积不计为零。条形长度采用文件数的对数尺度。</div>'
            '<div class="controls" hidden><input id="region" type="search" aria-label="搜索目录" placeholder="搜索目录…"><select id="order" aria-label="排序方式"><option value="files">文件数优先</option><option value="bytes">已知体积优先</option><option value="name">名称顺序</option></select></div>'
            f'<p id="results" aria-live="polite">{len(groups)} 个分组</p><section class="grid">'+"".join(cards)+'</section>'
            '<aside class="method"><h2>地图边界</h2><p>仅统计已跟踪的 Git 树条目，不受工作区缺失文件影响。符号链接和子模块单独计数，不跟随、不下载。缺失对象的体积标记为未知；扩展名分类不等同于编程语言识别。快照不自动更新。</p></aside>')
    script = '''const input=document.querySelector('#region'),order=document.querySelector('#order'),grid=document.querySelector('.grid'),tiles=[...document.querySelectorAll('.tile')];document.querySelector('.controls').hidden=false;function update(){const key=order.value;tiles.sort((a,b)=>key==='name'?a.querySelector('h3').textContent.localeCompare(b.querySelector('h3').textContent):Number(b.dataset[key])-Number(a.dataset[key]));let count=0;for(const tile of tiles){tile.hidden=!tile.querySelector('h3').textContent.toLocaleLowerCase().includes(input.value.toLocaleLowerCase());if(!tile.hidden)count++;grid.append(tile);}document.querySelector('#results').textContent=`显示 ${count} / ${tiles.length} 个分组`;}input.addEventListener('input',update);order.addEventListener('change',update);'''
    return shell("仓库地图", body, script)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--limit", type=int, default=100_000)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        report = atlas(args.root, args.revision, args.limit)
        with args.output.open("x", encoding="utf-8", errors="replace") as output:
            output.write(render_atlas(report))
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"Atlas could not open: {error}\n")
    print(f"Observed {report['entries']} entries, {len(report['groups'])} groups; truncated={report['truncated']}")


if __name__ == "__main__":
    main()
