#!/usr/bin/env python3
"""Exhibit portability hazards in Git paths without opening those paths."""

import argparse
from collections import Counter, defaultdict
import html
import json
from pathlib import Path
import re
import subprocess
import unicodedata

from meow_museum import resolve
from repo_atlas import shell, tree_entries

REASONS = {
    "long": ("长途旅行家", "相对路径超过 240 个 UTF-8 字节；不是所有平台的统一限制。"),
    "component": ("单名巨兽", "单个路径组件超过 255 个 UTF-8 字节。"),
    "deep": ("地心探险家", "文件位于至少 10 层目录之下。"),
    "reserved": ("Windows 天敌", "组件使用 Windows 常见设备保留名称。"),
    "invalid": ("字符越界者", '组件含 Windows 常见禁用字符或 ASCII 控制字符。'),
    "trailing": ("尾巴消失术", "组件以空格或句点结尾，部分 Windows 工具会规范化或拒绝。"),
    "invisible": ("隐形斗篷", "路径含控制字符或格式字符；展览以转义形式显示。"),
    "collision": ("平行宇宙双胞胎", "NFC 规范化并忽略大小写后与另一条路径相同。"),
    "encoding": ("字节异乡人", "路径含无法解码为 UTF-8 的字节。"),
}
RESERVED = re.compile(r"^(CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³])$", re.I)


def visible(path):
    parts = []
    for char in path:
        if char == "\\":
            parts.append("\\\\")
        elif unicodedata.category(char).startswith("C"):
            parts.append(f"\\u{ord(char):04x}")
        else:
            parts.append(char)
    return "".join(parts)


def inspect(path):
    components = path.split("/")
    reasons = []
    if len(path.encode("utf-8", "surrogateescape")) > 240:
        reasons.append("long")
    if any(len(c.encode("utf-8", "surrogateescape")) > 255 for c in components):
        reasons.append("component")
    if len(components) - 1 >= 10:
        reasons.append("deep")
    if any(RESERVED.fullmatch(c.rstrip(" .").split(".")[0]) for c in components):
        reasons.append("reserved")
    if any(any(ord(char) < 32 or char in '<>:"\\|?*' for char in c) for c in components):
        reasons.append("invalid")
    if any(c.endswith((" ", ".")) for c in components):
        reasons.append("trailing")
    if any(unicodedata.category(c) in {"Cc", "Cf"} for c in path):
        reasons.append("invisible")
    if any(0xDC80 <= ord(c) <= 0xDCFF for c in path):
        reasons.append("encoding")
    return reasons


def gallery(root, revision="HEAD", limit=100_000, exhibits=100):
    if not 1 <= exhibits <= 1000:
        raise ValueError("Exhibit limit must be between 1 and 1000")
    revision = resolve(root, revision)
    records, truncated = tree_entries(root, revision, limit)
    aliases = defaultdict(set)
    for record in records:
        # Include implicit directories, so A/x and a/y also expose a collision.
        parts = record["path"].split("/")
        for i in range(1, len(parts) + 1):
            prefix = "/".join(parts[:i])
            aliases[unicodedata.normalize("NFC", prefix).casefold()].add(prefix)
    collisions = {key for key, paths in aliases.items() if len(paths) > 1}
    entries = []
    counts = Counter()
    for record in records:
        path = record["path"]
        reasons = inspect(path)
        parts = path.split("/")
        if any(unicodedata.normalize("NFC", "/".join(parts[:i])).casefold() in collisions for i in range(1, len(parts) + 1)):
            reasons.append("collision")
        if reasons:
            counts.update(reasons)
            entries.append({"path": path, "reasons": reasons, "depth": len(parts) - 1,
                            "bytes": len(path.encode("utf-8", "surrogateescape"))})
    entries.sort(key=lambda e: (-len(e["reasons"]), -e["bytes"], e["path"]))
    # Preserve variety: each observed category gets a representative before filling.
    chosen = []
    for reason in REASONS:
        representative = next((e for e in entries if reason in e["reasons"] and e not in chosen), None)
        if representative and len(chosen) < exhibits:
            chosen.append(representative)
    for entry in entries:
        if len(chosen) == exhibits:
            break
        if entry not in chosen:
            chosen.append(entry)
    return {"revision": revision, "scanned": len(records), "truncated": truncated,
            "flagged": len(entries), "counts": dict(counts), "entries": chosen,
            "exhibits_truncated": len(entries) > len(chosen)}


def render_gallery(report):
    esc = html.escape
    legend = "".join(f'<details><summary>{esc(title)} · {report["counts"].get(key, 0):,} 条</summary><p>{esc(description)}</p></details>' for key, (title, description) in REASONS.items())
    options = "".join(f'<option value="{key}">{esc(value[0])}</option>' for key, value in REASONS.items())
    cards = []
    for index, entry in enumerate(report["entries"], 1):
        path = visible(entry["path"])
        tags = " · ".join(REASONS[r][0] for r in entry["reasons"])
        cards.append(f'<article class="tile" data-reasons="{" ".join(entry["reasons"])}"><p class="meta">标本 {index:03d} · {entry["bytes"]:,} 字节 · {entry["depth"]} 层目录</p>'
                     f'<h3>{esc(tags)}</h3><p class="path" style="overflow-wrap:anywhere"><code>{esc(path[:240])}{"…" if len(path)>240 else ""}</code></p>'
                     f'<details><summary>完整路径（控制字符已转义）</summary><p class="full-path" style="overflow-wrap:anywhere;white-space:pre-wrap"><code>{esc(path)}</code></p></details></article>')
    coverage = "扫描达到上限；未扫描部分可能还有更多标本。" if report["truncated"] else "本次完整扫描了指定提交的 Git 树。"
    body = (f'<h1>奇葩路径展览</h1><p>名字也是作品。跨平台工具则未必欣赏得来。</p><p class="meta">观测提交 {report["revision"]}</p>'
            f'<div class="note">{coverage}<br>扫描 {report["scanned"]:,} 条路径，发现 {report["flagged"]:,} 条带风险特征的路径；展出其中 {len(report["entries"])} 条。类别计数可重叠，筛选只作用于已展出的标本。</div>'
            f'<details class="method"><summary>判断依据与类别统计</summary>{legend}<p>这里只报告启发式风险，不执行文件、不尝试检出路径，也不保证某个系统一定失败。大小写及 Unicode 规范化行为因文件系统和工具而异。字节长度不等于 Windows UTF-16 路径长度。</p></details>'
            f'<div class="controls" hidden><input id="path-search" type="search" aria-label="搜索路径" placeholder="搜索已展出的路径…"><select id="reason" aria-label="风险类别"><option value="all">全部类别</option>{options}</select></div>'
            f'<p id="results" aria-live="polite">{len(report["entries"])} 件标本</p><section class="grid">'+"".join(cards)+'</section>')
    script = '''const query=document.querySelector('#path-search'),reason=document.querySelector('#reason'),cards=[...document.querySelectorAll('.tile')];document.querySelector('.controls').hidden=false;function filter(){let n=0;for(const card of cards){const match=card.querySelector('.full-path').textContent.toLocaleLowerCase().includes(query.value.toLocaleLowerCase())&&(reason.value==='all'||card.dataset.reasons.split(' ').includes(reason.value));card.hidden=!match;if(match)n++;}document.querySelector('#results').textContent=`显示 ${n} / ${cards.length} 件标本`;}query.addEventListener('input',filter);reason.addEventListener('change',filter);'''
    return shell("奇葩路径展览", body, script)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--limit", type=int, default=100_000)
    parser.add_argument("--exhibits", type=int, default=100)
    parser.add_argument("--format", choices=["html", "json"], default="html")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        report = gallery(args.root, args.revision, args.limit, args.exhibits)
        content = render_gallery(report) if args.format == "html" else json.dumps(report, ensure_ascii=True, indent=2)
        with args.output.open("x", encoding="utf-8", errors="replace") as output:
            output.write(content)
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"Gallery could not open: {error}\n")
    print(f"Scanned {report['scanned']}; flagged {report['flagged']}; exhibited {len(report['entries'])}; truncated={report['truncated']}")


if __name__ == "__main__":
    main()
