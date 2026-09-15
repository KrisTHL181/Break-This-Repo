#!/usr/bin/env python3
"""Build an offline, dependency-free exhibition of the meow commit's footprint."""

from __future__ import annotations

import argparse
import html
import os
import re
from pathlib import Path
import subprocess

BASELINE = "ac421f58da0ec26c820a87ed7d0a0f63df8d2221"
LABELS = {"retained": "猫爪仍在", "revisited": "后来者到访", "removed": "目录已消失"}


def git(root: Path, *args: str) -> bytes:
    """Read local Git objects; never trigger a partial clone's lazy download."""
    result = subprocess.run(
        ["git", "--no-pager", "--literal-pathspecs", "-C", str(root), *args],
        env={**os.environ, "GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"},
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False, timeout=60,
    )
    if result.returncode:
        raise ValueError(result.stderr.decode("utf-8", "replace").strip() or "Git history check failed")
    return result.stdout


def resolve(root: Path, revision: str) -> str:
    return git(root, "rev-parse", "--verify", "--end-of-options", revision + "^{commit}").decode().strip()


def observe(root: Path, baseline: str = BASELINE, revision: str = "HEAD") -> dict:
    baseline = resolve(root, baseline)
    revision = resolve(root, revision)
    if git(root, "rev-parse", "--is-shallow-repository").strip() == b"true":
        raise ValueError("A shallow clone cannot provide reliable footprint history; use complete history.")
    git(root, "merge-base", "--is-ancestor", baseline, revision)
    additions = git(root, "diff-tree", "--root", "--no-commit-id", "--no-renames",
                    "--diff-filter=A", "--name-only", "-r", "-z", baseline).split(b"\0")
    directories = sorted({p[:-8].decode("utf-8", "surrogateescape") for p in additions
                          if p.endswith(b"/MEOW.md") and p.count(b"/") == 1})
    if not directories:
        raise ValueError("The baseline did not add any top-level directory MEOW.md notes.")
    entries = []
    for directory in directories:
        tree = git(root, "ls-tree", "-z", revision, "--", directory)
        is_directory = bool(tree) and tree.split(b"\t", 1)[0].split()[1] == b"tree"
        latest = git(root, "log", "--no-renames", "-1", "--format=%H", revision, "--", directory).decode().strip()
        status = "removed" if not is_directory else "retained" if latest == baseline else "revisited"
        entries.append({"directory": directory, "latest": latest, "status": status})
    return {"baseline": baseline, "revision": revision,
            "date": git(root, "log", "-1", "--no-patch", "--format=%cI", revision).decode().strip(),
            "entries": entries}


def render(report: dict, events: str = "") -> str:
    entries = report["entries"]
    counts = {key: sum(e["status"] == key for e in entries) for key in LABELS}
    rate = round(100 * counts["retained"] / len(entries)) if entries else 0
    esc = lambda value: html.escape(str(value), quote=True)
    cards = "\n".join(
        f'<article class="specimen" data-status="{e["status"]}">'
        f'<span class="number">EXHIBIT {i:02d}</span><span class="tag {e["status"]}">{LABELS[e["status"]]}</span>'
        f'<h3>{esc(e["directory"])}</h3><p>最近改动 <code>{esc(e["latest"][:10])}</code></p></article>'
        for i, e in enumerate(entries, 1)
    )
    values = {"CARDS": cards, "EVENTS": events, "TOTAL": str(len(entries)), "RATE": str(rate),
              "RETAINED": str(counts["retained"]), "REVISITED": str(counts["revisited"]),
              "REMOVED": str(counts["removed"]), "BASELINE": esc(report["baseline"]),
              "REVISION": esc(report["revision"]), "DATE": esc(report["date"])}
    return re.sub(r"\{\{([A-Z]+)\}\}", lambda match: values[match[1]], TEMPLATE)


TEMPLATE = r'''<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="color-scheme" content="light"><title>喵化足迹 · 仓库生态博物馆</title>
<style>
:root{--paper:#f6f3e9;--ink:#223d35;--muted:#62756c;--line:#d4dace;--accent:#d6f176}
*{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);font:16px/1.7 system-ui,sans-serif}
main{max-width:1160px;margin:auto;padding:32px 32px 64px}header{display:flex;justify-content:space-between;border-bottom:1px solid var(--ink);padding-bottom:18px;font-size:12px;letter-spacing:2px}
.hero{display:grid;grid-template-columns:1.5fr 1fr;gap:48px;align-items:center;padding:62px 0 44px}.eyebrow{font-size:12px;letter-spacing:3px;color:var(--muted)}h1{font-size:clamp(52px,8vw,96px);line-height:1.1;letter-spacing:-5px;margin:18px 0 26px}h1 span{color:#728566} .intro{max-width:540px;color:var(--muted)}
.seal{aspect-ratio:1;border:1px solid var(--line);border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center;position:relative;background:radial-gradient(circle,#e8edda 0,transparent 68%)}
.seal:before{content:"";position:absolute;inset:15px;border:1px dashed #a7b49a;border-radius:50%}.cat{font:44px monospace;white-space:pre;text-align:center;line-height:1.2}.seal strong{font-size:48px;margin-top:20px;line-height:1.2}.seal small{color:var(--muted)}
.stats{display:grid;grid-template-columns:repeat(4,1fr);border-top:1px solid var(--ink);border-bottom:1px solid var(--ink);margin:15px 0 44px}.stat{padding:22px;border-right:1px solid var(--line)}.stat:last-child{border:0}.stat strong{display:block;font-size:36px;line-height:1.2}.stat span{font-size:13px;color:var(--muted)}
h2{font-size:25px;margin:0}.section-head{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:20px}.section-head p{margin:0;color:var(--muted);font-size:13px}
.controls{display:flex;gap:12px;flex-wrap:wrap;margin:20px 0}input,select{font:inherit;padding:10px 14px;border:1px solid var(--line);background:#fffdf6;color:var(--ink);border-radius:6px}input{flex:1;min-width:180px}input:focus,select:focus{outline:2px solid #628641;outline-offset:2px}
.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.specimen{border:1px solid var(--line);border-radius:8px;padding:20px;background:#fffdf7}.number{font:10px monospace;color:var(--muted)}.tag{float:right;font-size:10px;padding:2px 7px;border-radius:20px}.retained{background:var(--accent)}.revisited{background:#f3dfb8}.removed{background:#e5e5e5}.specimen h3{font-size:17px;line-height:1.5;overflow-wrap:anywhere;margin:25px 0 18px}.specimen p{font-size:11px;color:var(--muted);margin:0}code{font-family:monospace;overflow-wrap:anywhere}
[hidden]{display:none!important}.method{margin-top:44px;padding:26px;background:#e9ecdf;border-radius:8px}.method p{font-size:13px;margin-bottom:0}.provenance{display:grid;grid-template-columns:1fr 1fr;gap:20px;font-size:11px;margin-top:24px;color:var(--muted)}footer{margin-top:32px;padding-top:18px;border-top:1px solid var(--line);font-size:12px;color:var(--muted)}
@media(max-width:700px){main{padding:20px}.hero{grid-template-columns:1fr;padding:35px 0;gap:24px}.seal{width:230px;justify-self:center}.cat{font-size:30px}.seal strong{font-size:34px}.stats{grid-template-columns:repeat(2,1fr)}.stat:nth-child(2){border:0}.grid{grid-template-columns:1fr}.provenance{grid-template-columns:1fr}header{letter-spacing:0}.section-head{display:block}h1{letter-spacing:-2px}}
</style></head><body><main>
<header><b>CHAOS OBSERVATORY / 仓库生态博物馆</b><span>展区 001 · 猫爪巡游</span></header><nav aria-label="展区导航" style="display:flex;gap:24px;flex-wrap:wrap;margin:20px 0"><a href="index.html">猫爪足迹</a><a href="diary.html">消退日报</a><a href="atlas.html">仓库地图</a><a href="paths.html">奇葩路径</a></nav>
<section class="hero"><div><div class="eyebrow">A SMALL TRACE IN A LIVING REPOSITORY</div><h1>猫来过。<br><span>然后呢？</span></h1><p class="intro">一条「喵～」提交，走过 {{TOTAL}} 个目录。这里记录猫爪如何留在历史里，又如何被后来者的创作轻轻覆盖。</p></div>
<div class="seal"><div class="cat" aria-label="一只猫"> /\_/\\
( o.o )
 &gt; ^ &lt;</div><strong>{{RATE}}%</strong><small>最近改动仍停留在猫爪巡游</small></div></section>
<section class="stats" aria-label="足迹统计"><div class="stat"><strong>{{TOTAL}}</strong><span>最初到访目录</span></div><div class="stat"><strong>{{RETAINED}}</strong><span>猫爪仍在</span></div><div class="stat"><strong>{{REVISITED}}</strong><span>后来者到访</span></div><div class="stat"><strong>{{REMOVED}}</strong><span>目录已消失</span></div></section>
{{EVENTS}}
<section><div class="section-head"><h2>足迹标本</h2><p id="count" aria-live="polite">共 {{TOTAL}} 件标本 · 每个目录都是一个展柜</p></div>
<div class="controls" hidden><input id="search" type="search" placeholder="搜索目录名称…" aria-label="搜索目录名称"><select id="status" aria-label="按足迹状态筛选"><option value="all">全部状态</option><option value="retained">猫爪仍在</option><option value="revisited">后来者到访</option><option value="removed">目录已消失</option></select></div>
<div class="grid">{{CARDS}}</div><p id="empty" hidden>这片展区暂时没有标本，换个关键词试试。</p></section>
<aside class="method"><h2>猫爪会褪色，历史不会。</h2><p>本展以起点提交新增的顶层目录 MEOW.md 为固定样本。目录存在且 Git 路径历史的最近提交仍为起点，记为「猫爪仍在」；目录存在但最近提交不同，记为「后来者到访」；目录不再是普通 Git 树，记为「目录已消失」。目录更名按原路径消失计算。这里衡量最近改动，不衡量便签内容是否仍在。</p><p>这是指定提交上的静态快照，不会自动更新，也不承诺与 GitHub 页面缓存及合并历史展示完全一致。读取本地 Git 树和提交历史，不执行被观察仓库的代码，不采集作者信息，不请求网络。</p></aside>
<div class="provenance"><div>起点提交<br><code>{{BASELINE}}</code></div><div>观测提交<br><code>{{REVISION}}</code><br>提交时间 {{DATE}}</div></div>
<footer>混沌仍在形成中。下一位贡献者，欢迎留下你的脚印。</footer>
</main><script>
const search=document.querySelector('#search'),status=document.querySelector('#status'),cards=[...document.querySelectorAll('.specimen')];
document.querySelector('.controls').hidden=false;
function filter(){let visible=0;for(const card of cards){const match=card.querySelector('h3').textContent.toLocaleLowerCase().includes(search.value.toLocaleLowerCase())&&(status.value==='all'||card.dataset.status===status.value);card.hidden=!match;if(match)visible++;}document.querySelector('#count').textContent=`显示 ${visible} / ${cards.length} 件标本`;document.querySelector('#empty').hidden=visible!==0;}
search.addEventListener('input',filter);status.addEventListener('change',filter);
</script></body></html>'''


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--baseline", default=BASELINE)
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--output", required=True, type=Path, help="New HTML file (existing files are not overwritten)")
    args = parser.parse_args()
    try:
        report = observe(args.root, args.baseline, args.revision)
        with args.output.open("x", encoding="utf-8", errors="replace") as output:
            output.write(render(report))
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"Museum could not open: {error}\n")
    print(f"Exhibition written to {args.output} ({len(report['entries'])} specimens)")


if __name__ == "__main__":
    main()
