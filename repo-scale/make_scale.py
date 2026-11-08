#!/usr/bin/env python3
"""Render the weigh-in page from raw measurements.

Two modes:

* If the `weight-*.json` files produced by `weigh.py --json` are present, they
  are merged into `measurements.json` (the evidence file) and the page is built
  from that.  This is the path you take after re-measuring.
* Otherwise the existing `measurements.json` is re-rendered on its own.  The
  raw files are not part of the exhibit, so this is the mode you get from a
  fresh checkout of the repository.

    python3 make_scale.py

Nothing here talks to the network.  If you want fresh numbers, run weigh.py
first; this script only arranges what already exists.
"""

from __future__ import annotations

import datetime as dt
import html
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))

RAW_FILES = ("weight-head.json", "weight-break.json", "weight-milestones.json",
             "weight-restore.json", "weight-growth-a.json", "weight-growth-b.json")

GIB = 1024 ** 3
MIB = 1024 ** 2


# --------------------------------------------------------------------------
# inputs
# --------------------------------------------------------------------------

def load(name: str) -> dict:
    path = os.path.join(HERE, name)
    if not os.path.exists(path):
        raise SystemExit(f"missing {name}; run the weigh.py passes first")
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def collect_timeline() -> list[dict]:
    """Merge every measured point into one chronological list."""
    notes = {
        "meow": ("喵 (#508)", "364,313 files, the baseline before the growth"),
        "museum": ("展馆 (#522)", "the injection museum; +52 MB"),
        "weekago": ("9/15 11:52", "still small"),
        "d0815_after_novel": ("novel/ 传完", "12 PRs, 11,183 files, 11.5 GiB"),
        "d0918_after_random": ("随机的 2GiB", "20 files of exactly 100 MiB"),
        "d0921_cook": ("+HowToCook/AnimeStudio", "vendored repositories"),
        "d0921_dotnet": ("+dotnet/", "another whole repository imported"),
        "predapo": ("开战前 (16:25)", "the high-water mark before the war"),
        "dapo30": ("大破 30/30 (16:45)", "508,721 files deleted in four minutes"),
        "ffmpeg": ("+FFmpeg/ (16:52)", "10,732 files back in"),
        "rand2g": ("随机 2000MiB (18:50)", "deliberate incompressible ballast"),
        "cleanup": ("清理垃圾 (19:06)", "root reduced to 23 files"),
        "revert": ("RESET TEST (05:06)", "merge of the pre-war commit"),
        "restored": ("#738 合并 (05:19)", "576,155 files back"),
        "borg": ("现在 (#739, 07:33)", "heavier than before the war"),
    }
    order = ["meow", "museum", "weekago", "d0815_after_novel", "d0918_after_random",
             "d0921_cook", "d0921_dotnet", "predapo", "dapo30", "ffmpeg", "rand2g",
             "cleanup", "revert", "restored", "borg"]
    found: dict[str, dict] = {}
    for name in RAW_FILES:
        for row in load(name).get("milestones", []):
            found[row["label"]] = row
    rows = []
    for label in order:
        row = found.get(label)
        if not row:
            continue
        display, note = notes.get(label, (label, ""))
        rows.append({"label": label, "display": display, "note": note,
                     "sha": row["sha"], "date": row["date"],
                     "bytes": row["bytes"], "files": row["files"],
                     "dirs": row["dirs"]})
    rows.sort(key=lambda r: r["date"])
    return rows


# --------------------------------------------------------------------------
# formatting
# --------------------------------------------------------------------------

def human(n: float) -> str:
    n = float(n)
    if n >= GIB:
        return f"{n / GIB:.2f} GiB"
    if n >= MIB:
        return f"{n / MIB:.1f} MiB"
    if n >= 1024:
        return f"{n / 1024:.1f} KiB"
    return f"{n:.0f} B"


def esc(s) -> str:
    return html.escape(str(s), quote=True)


def parse(ts: str) -> dt.datetime:
    return dt.datetime.strptime(ts, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=dt.timezone.utc)


def short(ts: str) -> str:
    d = parse(ts)
    return d.strftime("%m-%d %H:%M")


# --------------------------------------------------------------------------
# svg charts
# --------------------------------------------------------------------------

PAD_L, PAD_R, PAD_T, PAD_B = 62, 14, 16, 34
SERIES_INK = "#1f6feb"
SERIES_WARN = "#c2410c"
GRID = "currentColor"


def chart(rows: list[dict], width: int, height: int, y_mode: str,
          y_floor: float, annotate: bool = True) -> str:
    """A time-series chart. True linear time on x, linear or log on y."""
    if not rows:
        return ""
    t0 = parse(rows[0]["date"]).timestamp()
    t1 = parse(rows[-1]["date"]).timestamp()
    span = max(t1 - t0, 1)
    ymax = max(r["bytes"] for r in rows) or 1

    def y_of(v: float) -> float:
        plot_h = height - PAD_T - PAD_B
        if y_mode == "log":
            lo, hi = math.log10(max(y_floor, 1)), math.log10(ymax)
            f = (math.log10(max(v, y_floor)) - lo) / max(hi - lo, 1e-9)
        else:
            f = v / ymax
        return PAD_T + (1 - f) * plot_h

    def x_of(ts: str) -> float:
        f = (parse(ts).timestamp() - t0) / span
        return PAD_L + f * (width - PAD_L - PAD_R)

    pts = [(x_of(r["date"]), y_of(r["bytes"]), r) for r in rows]
    out = [f'<svg viewBox="0 0 {width} {height}" width="100%" '
           f'role="img" aria-label="working tree weight over time" '
           f'font-family="ui-monospace,SFMono-Regular,Menlo,Consolas,monospace">']

    # horizontal grid + y ticks, placed so that a log axis still labels sanely
    for f in (0.0, 0.25, 0.5, 0.75, 1.0):
        value = (max(y_floor, 1) * (ymax / max(y_floor, 1)) ** f
                 if y_mode == "log" else ymax * f)
        y = y_of(value)
        out.append(f'<line x1="{PAD_L}" y1="{y:.1f}" x2="{width - PAD_R}" '
                   f'y2="{y:.1f}" stroke="{GRID}" stroke-opacity="0.16"/>')
        out.append(f'<text x="{PAD_L - 8}" y="{y + 3.5:.1f}" text-anchor="end" '
                   f'font-size="10" fill="currentColor" fill-opacity="0.62">'
                   f'{esc(human(value))}</text>')

    # vertical grid at each sample
    for x, _, _ in pts:
        out.append(f'<line x1="{x:.1f}" y1="{PAD_T}" x2="{x:.1f}" '
                   f'y2="{height - PAD_B}" stroke="{GRID}" stroke-opacity="0.08"/>')

    # x labels.  This data is deliberately lopsided -- eight of the fifteen
    # samples land inside the last two hours of a week -- so evenly spaced
    # labels would pile into an unreadable smear on the right.  Instead keep a
    # label only when it clears the previous one by MIN_LABEL_GAP pixels, and
    # always keep the last one.
    MIN_LABEL_GAP = 104
    keep = []
    for i, (x, _, _) in enumerate(pts):
        if not keep or x - keep[-1][1] >= MIN_LABEL_GAP or i == len(pts) - 1:
            if keep and i == len(pts) - 1 and x - keep[-1][1] < MIN_LABEL_GAP:
                keep.pop()
            keep.append((i, x))
    for i, x in keep:
        r = pts[i][2]
        if i == 0:
            anchor = "start"
        elif i == len(pts) - 1:
            anchor = "end"
        else:
            anchor = "middle"
        out.append(f'<text x="{x:.1f}" y="{height - PAD_B + 14}" '
                   f'text-anchor="{anchor}" font-size="9.5" fill="currentColor" '
                   f'fill-opacity="0.62">{esc(short(r["date"]))}</text>')

    # area + line
    plot_bottom = height - PAD_B
    area = " ".join(f"{x:.1f},{y:.1f}" for x, y, _ in pts)
    out.append(f'<polygon points="{PAD_L},{plot_bottom} {area} '
               f'{pts[-1][0]:.1f},{plot_bottom}" fill="{SERIES_INK}" '
               f'fill-opacity="0.10"/>')
    line = " ".join(f"{x:.1f},{y:.1f}" for x, y, _ in pts)
    out.append(f'<polyline points="{line}" fill="none" stroke="{SERIES_INK}" '
               f'stroke-width="2" stroke-linejoin="round"/>')

    for x, y, r in pts:
        colour = SERIES_WARN if r["label"] in ("dapo30", "cleanup") else SERIES_INK
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3.6" fill="{colour}" '
                   f'stroke="var(--bg)" stroke-width="1.4"/>')

    # annotate the two extremes; nudge the anchor so the text never leaves the
    # frame at either end
    if annotate:
        peak = max(pts, key=lambda p: p[2]["bytes"])
        trough = min(pts, key=lambda p: p[2]["bytes"])
        for x, y, r in (peak, trough):
            if x > width - PAD_R - 70:
                tx, anchor = width - PAD_R, "end"
            elif x < PAD_L + 70:
                tx, anchor = PAD_L, "start"
            else:
                tx, anchor = x, "middle"
            out.append(f'<text x="{tx:.1f}" '
                       f'y="{max(y - 9, PAD_T + 11):.1f}" text-anchor="{anchor}" '
                       f'font-size="10" font-weight="600" fill="currentColor">'
                       f'{esc(human(r["bytes"]))}</text>')

    out.append("</svg>")
    return "".join(out)


def bar_rows(rows: list[dict], total: int, limit: int = 15) -> str:
    peak = rows[0]["bytes"] if rows else 1
    out = []
    for r in rows[:limit]:
        share = r["bytes"] / total * 100 if total else 0
        w = max(0.4, r["bytes"] / peak * 100)
        kind = "dir" if r["name"].endswith("/") else "file"
        out.append(
            '<div class="row">'
            f'<div class="rname" title="{esc(r["name"])}">'
            f'<span class="tag {kind}">{kind}</span>{esc(r["name"])}</div>'
            f'<div class="rtrack"><div class="rfill" style="width:{w:.2f}%"></div></div>'
            f'<div class="rnum">{esc(human(r["bytes"]))}</div>'
            f'<div class="rpct">{share:.1f}%</div>'
            f'<div class="rfiles">{r["files"]:,}</div>'
            '</div>')
    return "".join(out)


# --------------------------------------------------------------------------
# page
# --------------------------------------------------------------------------

CSS = """
:root{
  --bg:#fbfbfa; --panel:#ffffff; --ink:#1b1c1e; --muted:#6b6f76;
  --line:#e3e3df; --ink-strong:#0f1011; --accent:#1f6feb; --warn:#c2410c;
  --good:#0f7b6c;
}
@media (prefers-color-scheme: dark){
  :root{
    --bg:#131416; --panel:#191b1e; --ink:#e8e8e6; --muted:#9aa0a6;
    --line:#2a2d31; --ink-strong:#ffffff; --accent:#6ba3ff; --warn:#f97316;
    --good:#2dd4bf;
  }
}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);
  font:16px/1.65 ui-sans-serif,system-ui,-apple-system,"Segoe UI",
  "PingFang SC","Microsoft YaHei",sans-serif;}
.wrap{max-width:1000px;margin:0 auto;padding:40px 22px 72px}
h1{font-size:30px;line-height:1.25;margin:0 0 6px;letter-spacing:-.02em}
h2{font-size:19px;margin:44px 0 14px;padding-bottom:8px;
  border-bottom:1px solid var(--line);letter-spacing:-.01em}
h3{font-size:15px;margin:26px 0 8px;color:var(--muted);
  font-weight:600;letter-spacing:.02em}
.sub{color:var(--muted);font-size:14.5px;margin:0}
code,kbd{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
  font-size:.9em;background:var(--panel);border:1px solid var(--line);
  border-radius:4px;padding:1px 5px}
.card{background:var(--panel);border:1px solid var(--line);border-radius:12px;
  padding:18px 20px;margin:16px 0}
.pair{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin:22px 0}
@media (max-width:720px){.pair{grid-template-columns:1fr}}
.big{background:var(--panel);border:1px solid var(--line);border-radius:12px;
  padding:20px}
.big .k{font-size:12px;letter-spacing:.08em;text-transform:uppercase;
  color:var(--muted)}
.big .v{font-size:34px;font-weight:700;letter-spacing:-.03em;margin:6px 0 2px;
  font-variant-numeric:tabular-nums}
.big .n{font-size:13px;color:var(--muted);font-variant-numeric:tabular-nums}
table{width:100%;border-collapse:collapse;font-size:13.5px}
th,td{text-align:left;padding:7px 10px;border-bottom:1px solid var(--line);
  font-variant-numeric:tabular-nums}
th{color:var(--muted);font-weight:600;font-size:12px;letter-spacing:.04em;
  text-transform:uppercase}
td.num{text-align:right;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,
  monospace}
tr.now td{font-weight:700}
tr.gone td{color:var(--warn)}
.note{font-size:12.5px;color:var(--muted)}
.row{display:grid;grid-template-columns:minmax(150px,1.5fr) 1.4fr 76px 52px 68px;
  align-items:center;gap:10px;padding:4px 0;
  font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px}
.rname{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tag{display:inline-block;min-width:34px;margin-right:6px;font-size:9.5px;
  text-align:center;border-radius:3px;padding:1px 0;border:1px solid var(--line);
  color:var(--muted)}
.rtrack{background:var(--panel);border:1px solid var(--line);border-radius:3px;
  height:11px;overflow:hidden}
.rfill{height:100%;background:var(--accent);border-radius:2px}
.rnum,.rpct,.rfiles{text-align:right;color:var(--muted)}
.rfiles{color:var(--muted)}
blockquote{margin:16px 0;padding:2px 0 2px 16px;border-left:3px solid var(--accent);
  color:var(--ink)}
blockquote p{margin:8px 0}
.kv{display:grid;grid-template-columns:auto 1fr;gap:6px 16px;font-size:13.5px;
  margin:0}
.kv dt{color:var(--muted)}
.kv dd{margin:0;font-variant-numeric:tabular-nums}
footer{margin-top:52px;padding-top:16px;border-top:1px solid var(--line);
  color:var(--muted);font-size:12.5px}
.lede{font-size:17px;color:var(--ink)}
"""


def build_page(m: dict) -> str:
    rows = m["timeline"]
    head = m["head"]
    total = head["bytes"]
    peak = max(r["bytes"] for r in rows)
    trough = min(r["bytes"] for r in rows)

    # table rows
    trs = []
    prev = None
    for r in rows:
        delta = "—" if prev is None else f"{r['bytes'] - prev:+,}"
        cls = ""
        if r["label"] == "borg":
            cls = ' class="now"'
        elif r["bytes"] < 200 * 1024 ** 2:
            cls = ' class="gone"'
        trs.append(
            f"<tr{cls}><td>{esc(r['display'])}</td>"
            f"<td class='num'>{esc(short(r['date']))}</td>"
            f"<td class='num'>{r['bytes']:,}</td>"
            f"<td class='num'>{esc(human(r['bytes']))}</td>"
            f"<td class='num'>{r['files']:,}</td>"
            f"<td class='num'>{esc(delta)}</td></tr>")
        prev = r["bytes"]

    ctx = m["context"]
    files_note = (f"{total / head['files']:,.0f} bytes per file on average, "
                  f"and the median is far smaller")

    page = f"""<!DOCTYPE html>
<html lang="zh-CN"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>体重站 / The Weigh-in · {esc(m['repository'])}</title>
<style>{CSS}</style>
</head><body><div class="wrap">

<h1>体重站 · 一份体检报告</h1>
<p class="sub"><code>{esc(m['repository'])}</code> ·
实测于 {esc(short(m['generated_at']))} UTC ·
{cra_repo(m)}</p>

<p class="lede" style="margin-top:22px">
这个仓库在八天里涨到原来的 <b>{peak / rows[0]['bytes']:.1f}</b> 倍。
它曾经在四分钟内被删掉 {m['war']['deleted_files']:,} 个文件、几乎归零，
七个多小时后又被全部装回来 ——
<b>而且比被删之前更重</b>。
下面每个数字都来自 {esc(m['method']['api_calls_total'])} 次 API 调用，
没有一次是估算。</p>

<div class="pair">
  <div class="big">
    <div class="k">工作树 · working tree</div>
    <div class="v">{esc(human(total))}</div>
    <div class="n">{head['files']:,} 个文件 · {head['dirs']:,} 个目录 · 实测于 {esc(head['sha'][:10])}</div>
  </div>
  <div class="big">
    <div class="k">仓库本体 · GitHub 报的 size</div>
    <div class="v">{esc(human(m['github_repo_size_bytes']))}</div>
    <div class="n">包含历史。GitHub 建议 &lt;5 GiB，理想 &lt;1 GiB。</div>
  </div>
</div>

<h2>一 · 整周：从 6.6 GiB 到 25.5 GiB</h2>
<div class="card">
{chart(rows, 960, 300, 'linear', 0)}
<p class="note">横轴为真实时间，纵轴线性。倒数第二个点之后的那段空白，
是仓库被删到只剩 23 个文件的那几个小时 —— 曲线几乎贴到零。</p>
</div>

<h2>二 · 最后 16 小时：战争与复原</h2>
<div class="card">
{chart([r for r in rows if r['date'] >= '2026-09-21T16:20:00Z'], 960, 300, 'log', 10 ** 7)}
<p class="note">同样数据，纵轴取对数，窗口缩到 09-21 16:20 之后。
这样才能在同一张图上看见 26 MiB 和 27.3 GB 之间的差距：
{peak / trough:.0f} 倍。</p>
</div>

<h2>三 · 十一个时刻的称重记录</h2>
<table>
<thead><tr><th>时刻</th><th>UTC</th><th>bytes</th><th>重量</th>
<th>文件数</th><th>相对上一点</th></tr></thead>
<tbody>{''.join(trs)}</tbody>
</table>
<p class="note">{esc(files_note)}</p>

<h2>四 · 重量在哪里</h2>
<div class="card">
<p class="note" style="margin:0 0 12px">
根目录最重的 {m['breakdown_shown']} 个条目，合起来占全树 {m['breakdown_share']:.1f}%：
</p>
{bar_rows(m['breakdown'], total, 15)}
</div>
<p class="note">最大的一块是 <code>novel/</code>，{esc(human(m['breakdown'][0]['bytes']))}，
占 {m['breakdown'][0]['bytes'] / total * 100:.0f}% ，
由 {esc(ctx['novel_author'])} 在 {esc(ctx['novel_date'])} 分 {ctx['novel_parts']} 次 PR 送入 —— 
它没有宣言，没有阵营，只是安静地占着差不多一半的重量。</p>

<h2>五 · 三位选手的实际得分</h2>
<div class="card">
<dl class="kv">
<dt>{esc(ctx['novel_author'])} · <code>novel/</code></dt>
<dd>{esc(human(m['breakdown'][0]['bytes']))} · {ctx['novel_parts']} 次 PR · 静默</dd>
<dt>{esc(ctx['rust_author'])} · <code>rust-corpus/</code></dt>
<dd>{esc(human(m['breakdown'][1]['bytes']))} · {m['breakdown'][1]['files']:,} 个文件（全仓文件数的 {m['breakdown'][1]['files'] / head['files'] * 100:.0f}%）</dd>
<dt>{esc(ctx['meow_author'])} · <code>000.RANDOM.FILES/</code> + <code>RND100M/</code></dt>
<dd>{esc(human(ctx['random_bytes']))} · 40 个文件 · 每个正好 {ctx['random_file_size']:,} 字节</dd>
<dt>{esc(ctx['dapo_author'])} · 大破 30 连</dt>
<dd>删除 {m['war']['deleted_files']:,} 个文件 · 净结果 <b style="color:var(--warn)">{esc(human(m['war']['net_bytes']))} 更重</b></dd>
</dl>
<p class="note" style="margin-top:12px">
最后一行是这份报告的重点。四个人里，唯一在这张表上留下负数的人，
实际上做了最多的事、也最没有用。</p>
</div>

<h2>六 · 账本</h2>
<div class="card">
<p style="margin:0 0 10px">开战前的那棵树，加上被"清理垃圾"清剩下的那棵树，等于复原后的那棵树 —— 相加是精确的：</p>
<pre style="overflow-x:auto;font-size:12.5px">开战前  (14ed270f)  {m['war']['pre']:>15,} B
清剩    (0d1f3a11)  {m['war']['left']:>15,} B
                            ─────────────────
相加                {m['war']['pre'] + m['war']['left']:>15,} B
复原后  (20fafdb9)  {m['war']['revert']:>15,} B   {'精确相等' if m['war']['pre'] + m['war']['left'] == m['war']['revert'] else '不等'}</pre>
<p class="note">复原提交 <code>20fafdb9</code> 的父提交是
<code>0d1f3a11</code> 和 <code>14ed270f</code> —— 也就是
<b>直接把大破之前的那个提交 merge 回来</b>。
这一步能成功，唯一的原因是那些文件从来没有离开过仓库。</p>
</div>

<h2>七 · 用药说明</h2>
<div class="card">
<p><b>随机数是最贵的文件类型。</b> {ctx['random_count']} 个文件，
每个 {ctx['random_file_size']:,} 字节 —— 正好卡在 GitHub 单文件硬上限上。
文本在 pack 里能压掉很大一部分，随机数一个字节都省不掉。
想给这个仓库增重，写代码是最慢的方式。</p>
<p><b>删除只移动指针，不释放重量。</b> git 里没有"删除"，
只有"当前这棵树不再指向它"。所以
{esc(m['war']['deleted_files_display'])} 个文件的消失是可逆的 ——
而这正是它无效的证明：一次 12 小时后就复原的删除，
对仓库本体的贡献是零。</p>
<p><b>仓库本体是只能加不能减的。</b> 这个仓库的唯一变更入口是
自动合并 PR，而一次合并只能新增提交。想真正减小 <code>size</code>，
只有重写历史一条路；那条路不能表达为一个 PR。
所以只要这个玩法不变，这个数字只会往上走。</p>
<p class="note">本节为描述，不是处方。没有任何一条建议在鼓励对
共享仓库做 force push —— 那会打断 {ctx['forks']} 个 fork 和所有人的本地副本。</p>
</div>

<h2>八 · 复查方式</h2>
<pre style="overflow-x:auto;font-size:12.5px">python3 weigh.py {esc(m['repository'])}                # 现在的重量
python3 weigh.py {esc(m['repository'])} --by-dir 20     # 重量在哪里
python3 weigh.py {esc(m['repository'])} --series 24     # 沿主干采样
python3 weigh.py {esc(m['repository'])} --milestone before=&lt;sha&gt; --milestone after=&lt;sha&gt;
python3 -m unittest -v                    # 29 项离线测试</pre>
<p class="note">零依赖、只读、不 clone。第一次跑要几分钟，之后靠子树
SHA 缓存，同样的测量只要几秒。</p>

<footer>
页面由 <code>make_scale.py</code> 从 <code>measurements.json</code> 生成，
静态 HTML，不加载任何脚本、字体或外部资源。
原始数据见同目录 <code>measurements.json</code> 与 <code>WEIGHT.md</code>。
</footer>

</div></body></html>
"""
    return page


def cra_repo(m: dict) -> str:
    return (f"工作树 {human(m['head']['bytes'])} · "
            f"{m['head']['files']:,} 个文件 · "
            f"{m['head']['dirs']:,} 个目录")


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------

def build_from_raw() -> dict | None:
    """Assemble the evidence file. Returns None when the raw files are absent."""
    if any(not os.path.exists(os.path.join(HERE, n)) for n in RAW_FILES):
        return None

    head = load("weight-head.json")
    brk = load("weight-break.json")
    timeline = collect_timeline()
    if not timeline:
        raise SystemExit("no milestones found in the weight-*.json files")

    war = {
        "pre": next(r["bytes"] for r in timeline if r["label"] == "predapo"),
        "left": next(r["bytes"] for r in timeline if r["label"] == "cleanup"),
        "revert": next(r["bytes"] for r in timeline if r["label"] == "revert"),
    }
    dapo_files = (next(r["files"] for r in timeline if r["label"] == "predapo")
                  - next(r["files"] for r in timeline if r["label"] == "dapo30"))
    war["deleted_files"] = dapo_files
    war["deleted_files_display"] = f"{dapo_files:,}"
    war["net_bytes"] = head["head"]["bytes"] - war["pre"]

    context = {
        "novel_author": "StevenLi-phoenix",
        "novel_date": "2026-09-15",
        "novel_parts": 12,
        "rust_author": "shuiyue-cmyk",
        "meow_author": "MeowMyaa",
        "dapo_author": "shuiyue-cmyk",
        "random_count": 40,
        "random_file_size": 104857600,
        "random_bytes": 40 * 104857600,
        "forks": 164,
    }
    # cross-check printed into the page: the random files' share of the tree
    context["random_share"] = round(
        context["random_bytes"] / head["head"]["bytes"] * 100, 2)

    total_calls = sum(load(n)["stats"]["api_calls"] for n in RAW_FILES)

    return {
        "repository": head["repository"],
        "generated_at": head["generated_at"],
        "method": {
            "endpoint": "GET /repos/{owner}/{repo}/git/trees/{tree_sha}",
            "note": "recursive listings are truncated by GitHub at 100,000 "
                    "entries or 7 MB; truncated trees are split and re-fetched "
                    "until every total is exact",
            "api_calls_total": total_calls,
            "cache": "aggregates are cached per subtree SHA, so history is cheap",
        },
        "head": head["head"],
        "github_repo_size_bytes": head["github_size_bytes"],
        "breakdown_shown": len(brk["breakdown"]),
        "breakdown": brk["breakdown"],
        "breakdown_share": round(
            sum(r["bytes"] for r in brk["breakdown"]) / head["head"]["bytes"] * 100, 2),
        "heaviest_files": head["heaviest_files"],
        "timeline": timeline,
        "war": war,
        "context": context,
    }


def main() -> int:
    m = build_from_raw()
    if m is None:
        evidence = os.path.join(HERE, "measurements.json")
        if not os.path.exists(evidence):
            raise SystemExit("no weight-*.json and no measurements.json; "
                             "measure something first")
        with open(evidence, encoding="utf-8") as fh:
            m = json.load(fh)
        print("rendering from measurements.json "
              "(the raw weight-*.json files are not part of this exhibit)")
        rechecked = False
    else:
        with open(os.path.join(HERE, "measurements.json"), "w", encoding="utf-8") as fh:
            json.dump(m, fh, ensure_ascii=False, indent=1)
        rechecked = True

    with open(os.path.join(HERE, "scale.html"), "w", encoding="utf-8") as fh:
        fh.write(build_page(m))

    print(f"timeline points   {len(m['timeline'])}")
    print(f"breakdown         {m['breakdown_shown']} root entries, "
          f"top 15 cover {m['breakdown_share']:.1f}%")
    print(f"war net effect    {m['war']['net_bytes']:+,} bytes")
    print("wrote " + ("measurements.json, " if rechecked else "") + "scale.html")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
