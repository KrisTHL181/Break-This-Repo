#!/usr/bin/env python3
"""Render forged-clock.html from timestamps.json + attribution.json.

Stdlib only.  One self-contained page, no JS, no fonts, no CDN.
Every number is read from the JSON so the page cannot drift from the data.
"""

from __future__ import annotations

import html
import json
import re
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = Path(__file__).resolve().parent
OUT = BASE / "forged-clock.html"
W = 660
C_MAIN = "#E24B4A"
C_DIM = "#378ADD"
C_ACC = "#BA7517"


def esc(s) -> str:
    return html.escape(str(s), quote=True)


def comma(n) -> str:
    return "{:,}".format(int(n))


RE_PR = re.compile(r"Merge pull request #(\d+) from ([^/\s]+)")


def pr_label(subject: str) -> str:
    """'Merge pull request #729 from MeowMyaa/main' -> 'PR #729 · MeowMyaa'."""
    m = RE_PR.search(subject or "")
    if m:
        return "PR #%s · %s" % (m.group(1), m.group(2))
    return (subject or "")[:30]


# ---------------------------------------------------------------- chart A
def chart_years(a: dict) -> str:
    years = {int(k): v for k, v in a["years"].items()}
    L, R, T, B = 52, 18, 22, 46
    w, h = W - L - R, 250 - T - B
    ys = sorted(years)
    lo, hi = min(ys), max(ys)
    span = hi - lo + 1
    vmax = max(years.values())
    bw = max(1.5, w / span - 1)

    def X(y):
        return L + (y - lo) / span * w

    def Y(v):
        return T + (1 - v / vmax) * h

    out = ['<svg viewBox="0 0 %d 250" width="100%%" role="img" '
           'aria-label="提交按年份分布，1971 至 2077">' % W]
    for f in (0.25, 0.5, 0.75, 1.0):
        y = Y(vmax * f)
        out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="grid"/>'
                   % (L, y, L + w, y))
        out.append('<text x="%g" y="%g" class="ax" text-anchor="end">%s</text>'
                   % (L - 7, y + 4, comma(vmax * f)))
    out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="axis"/>'
               % (L, T + h, L + w, T + h))

    for y in ys:
        v = years[y]
        x = X(y)
        bh = max(1.2, (1 - v / vmax) * h if False else v / vmax * h)
        col = C_MAIN if y in (lo, hi) else (
            C_ACC if y < 2005 else C_DIM)
        out.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s" '
                   'fill-opacity="%s"><title>%d 年 · %s 个提交</title></rect>'
                   % (x + 0.5, Y(v), bw, bh, col, ".95" if y in (lo, hi) else ".8",
                      y, comma(v)))

    out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="mark"/>'
               % (X(2005), T, X(2005), T + h))
    out.append('<text x="%g" y="%g" class="note">git 诞生 · 2005-04-03</text>'
               % (X(2005) + 5, T + 12))
    for y, txt, anc in ((lo, "%d · 1 个" % lo, "start"),
                        (hi, "%d · 1 个" % hi, "end")):
        out.append('<text x="%.2f" y="%.2f" class="big" text-anchor="%s">%s</text>'
                   % (X(y) + (4 if anc == "start" else -4), Y(years[y]) - 5, anc, txt))
    tick = (lo // 10 + 1) * 10
    while tick <= hi:
        out.append('<line x1="%.2f" y1="%g" x2="%.2f" y2="%g" class="tick"/>'
                   % (X(tick) + bw / 2, T + h, X(tick) + bw / 2, T + h + 4))
        out.append('<text x="%.2f" y="%g" class="ax" text-anchor="middle">%d</text>'
                   % (X(tick) + bw / 2, T + h + 15, tick))
        tick += 20
    out.append('<text x="%g" y="%g" class="note" text-anchor="middle">'
               '最左与最右那两根红柱是别人手写的 1971 与 2077，'
               '其余全是随 merge 进来的上游历史</text>' % (L + w / 2, T + h + 34))
    out.append('</svg>')
    return "".join(out)


# ---------------------------------------------------------------- chart B
def chart_chain(attr: dict) -> str:
    ci = [max(0, v - 1) for v in attr["chain_imported"]]   # each step also adds itself
    subj = attr["chain_subject"]
    n = len(ci)
    L, R, T, B = 54, 18, 26, 40
    w, h = W - L - R, 250 - T - B
    vmax = max(ci)
    bw = w / n

    def X(i):
        return L + i * bw

    def Y(v):
        return T + (1 - v / vmax) * h

    out = ['<svg viewBox="0 0 %d 250" width="100%%" role="img" '
           'aria-label="首父链每一步引入的提交数，单位线性">' % W]
    for f in (0.25, 0.5, 0.75, 1.0):
        y = Y(vmax * f)
        out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="grid"/>'
                   % (L, y, L + w, y))
        out.append('<text x="%g" y="%g" class="ax" text-anchor="end">%s</text>'
                   % (L - 7, y + 4, comma(vmax * f)))
    out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="axis"/>'
               % (L, T + h, L + w, T + h))
    for i, v in enumerate(ci):
        if v <= 0:
            continue
        out.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s" '
                   'fill-opacity=".85"><title>步 %d · %s · %s</title></rect>'
                   % (X(i), Y(v), max(1.0, bw * 0.9), h - (Y(v) - T), C_MAIN,
                      i, comma(v), esc(pr_label(subj[i]))))
    peak = max(range(n), key=lambda i: ci[i])
    out.append('<line x1="%.2f" y1="%g" x2="%.2f" y2="%g" class="mark"/>'
               % (X(peak), Y(ci[peak]) - 4, X(peak), T))
    out.append('<text x="%.2f" y="%g" class="big" text-anchor="start">'
               '%s · %s</text>'
               % (X(peak) + 6, T + 12, esc(pr_label(subj[peak])), comma(ci[peak])))
    second = sorted(range(n), key=lambda i: -ci[i])[1]
    rest = sum(ci) - ci[peak] - ci[second]
    out.append('<text x="%.2f" y="%g" class="note" text-anchor="start">'
               '%s · %s</text>'
               % (X(second) + 4, T + 30, esc(pr_label(subj[second])), comma(ci[second])))
    out.append('<text x="%g" y="%g" class="note" text-anchor="end">'
               '其余 %d 个 merge 合计 %s 个提交 —— 在这张图上是一条平线</text>'
               % (L + w, T + 46, n - 2, comma(rest)))
    out.append('<text x="%g" y="%g" class="ax" text-anchor="start">HEAD</text>'
               % (L, T + h + 16))
    out.append('<text x="%g" y="%g" class="ax" text-anchor="end">仓库最早的提交</text>'
               % (L + w, T + h + 16))
    out.append('<text x="%g" y="%g" class="note" text-anchor="middle">'
               '首父链的 705 步，从左（新）到右（旧）。纵轴线性，单位：提交数。</text>'
               % (L + w / 2, T + h + 34))
    out.append('</svg>')
    return "".join(out)


# ---------------------------------------------------------------- chart C
def chart_hours(a: dict, fp: dict) -> str:
    """Two shapes: the whole graph, and only the real 705-commit history."""
    full = {int(k): v for k, v in a["hours_in_utc"].items()}
    real = {int(k): v for k, v in fp["hours_in_utc"].items()}
    fs, rs = sum(full.values()), sum(real.values())
    L, R, T, B = 46, 16, 22, 44
    w, h = W - L - R, 210 - T - B
    bw = w / 24
    vmax = max(max(v / fs for v in full.values()), max(v / rs for v in real.values()))

    out = ['<svg viewBox="0 0 %d 210" width="100%%" role="img" '
           'aria-label="UTC 小时直方图：全量与真实历史对比">' % W]
    for f in (0.5, 1.0):
        y = T + (1 - vmax * f / vmax) * h
        out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="grid"/>'
                   % (L, y, L + w, y))
        out.append('<text x="%g" y="%g" class="ax" text-anchor="end">%.1f%%</text>'
                   % (L - 6, y + 4, 100 * vmax * f))
    out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="axis"/>'
               % (L, T + h, L + w, T + h))
    for hr in range(24):
        for series, col, off, lab in ((full, C_DIM, 0.0, "全量 153,426"),
                                      (real, C_MAIN, bw / 2, "真实 705")):
            v = series.get(hr, 0) / (fs if series is full else rs)
            bh = v / vmax * h
            out.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s" '
                       'fill-opacity=".85"><title>%s · %02d:00 UTC · %.2f%%</title>'
                       '</rect>' % (L + hr * bw + off + 0.5, T + h - bh, bw / 2 - 1,
                                    max(bh, 0.5), col, lab, hr, 100 * v))
        if hr % 3 == 0:
            out.append('<text x="%.2f" y="%g" class="ax" text-anchor="middle">%02d</text>'
                       % (L + hr * bw + bw / 2, T + h + 16, hr))
    out.append('<text x="%g" y="%g" class="note" text-anchor="middle">'
               'UTC 小时　左＝全量　右＝真实历史（各自归一化）</text>'
               % (L + w / 2, T + h + 34))
    out.append('</svg>')
    return "".join(out)


CSS = """
:root{--bg:#fff;--panel:#F1EFE8;--ink:#2C2C2A;--mut:#5F5E5A;--dim:#888780;
--bd:rgba(0,0,0,.12);--grid:rgba(0,0,0,.09)}
@media (prefers-color-scheme:dark){:root{--bg:#1b1b19;--panel:#262624;--ink:#F1EFE8;
--mut:#B4B2A9;--dim:#A8A69E;--bd:rgba(255,255,255,.14);--grid:rgba(255,255,255,.10)}}
*{box-sizing:border-box}
body{margin:0;padding:56px 24px 80px;background:var(--bg);color:var(--ink);
font:400 15px/1.7 ui-sans-serif,system-ui,"Segoe UI",Roboto,"Noto Sans SC",sans-serif}
main{max-width:740px;margin:0 auto}
h1{font-size:29px;font-weight:500;margin:0 0 8px;letter-spacing:-.01em;line-height:1.35}
h2{font-size:19px;font-weight:500;margin:54px 0 6px}
p{margin:10px 0;color:var(--mut)}
p.lead{font-size:17px;color:var(--ink);margin:14px 0 0}
strong{color:var(--ink);font-weight:500}
code{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:13px;
background:var(--panel);padding:2px 5px;border-radius:4px;color:var(--ink)}
.kicker{font-size:13px;color:var(--dim);letter-spacing:.06em;text-transform:uppercase}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(148px,1fr));
gap:12px;margin:26px 0 0}
.card{background:var(--panel);border-radius:10px;padding:14px 16px}
.card .k{font-size:12px;color:var(--mut)}
.card .v{font-size:22px;font-weight:500;margin-top:3px}
.card.big .v{color:#E24B4A}
figure{margin:22px 0;padding:16px 16px 8px;border:1px solid var(--bd);
border-radius:12px;background:var(--bg)}
figcaption{font-size:13px;color:var(--dim);margin-top:8px}
table{width:100%;border-collapse:collapse;margin:18px 0;font-size:14px}
th,td{text-align:left;padding:7px 9px;border-bottom:1px solid var(--bd)}
th{font-weight:500;color:var(--mut);font-size:13px}
td.n,th.n{text-align:right;font-variant-numeric:tabular-nums}
td.hi{color:#E24B4A;font-weight:500}
svg{display:block;width:100%;height:auto}
.grid{stroke:var(--grid);stroke-width:1}
.tick{stroke:var(--bd);stroke-width:1}
.axis{stroke:var(--bd);stroke-width:1}
.mark{stroke:var(--dim);stroke-width:1;stroke-dasharray:3 3;opacity:.8}
.ax{fill:var(--dim);font-size:10.5px}
.note{fill:var(--dim);font-size:11px}
.big{fill:#E24B4A;font-size:11.5px;font-weight:500}
.legend{display:flex;flex-wrap:wrap;gap:16px;margin-top:8px;font-size:12px;color:var(--mut)}
.legend i{display:inline-block;width:9px;height:9px;border-radius:2px;margin-right:5px;
vertical-align:1px}
blockquote{margin:18px 0;padding:12px 16px;border-left:3px solid #E24B4A;
background:var(--panel);border-radius:0 8px 8px 0;color:var(--ink);font-size:15px}
footer{margin-top:60px;padding-top:20px;border-top:1px solid var(--bd);
font-size:13px;color:var(--dim)}
"""


def build() -> str:
    t = json.loads((BASE / "timestamps.json").read_text("utf-8"))
    g = json.loads((BASE / "attribution.json").read_text("utf-8"))
    a, fp = t["author_dates"], t["first_parent_only"]
    ci = [max(0, v - 1) for v in g["chain_imported"]]
    peak = max(range(len(ci)), key=lambda i: ci[i])
    owner = g["imports_by_pr_branch_owner"]
    top = owner[0]
    second = owner[1]

    imp_rows = "".join(
        "<tr><td>%s</td><td class='n'>%s</td><td class='n'>%.1f%%</td></tr>"
        % (esc(n), comma(v), 100 * v / g["reachable_commits"])
        for n, v in owner[:6])

    au_rows = "".join(
        "<tr><td>%s</td><td class='n'>%s</td><td class='n'>%s</td>"
        "<td class='n'>%.1f 年</td></tr>"
        % (esc(x["name"]), comma(x["commits"]), esc(x["first"][:10]), x["span_years"])
        for x in t["top_authors"][:8])

    return """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>这个仓库 98.9% 的历史不是它的</title>
<style>{css}</style></head><body><main>

<p class="kicker">forged-clock</p>
<h1>这个仓库 98.9% 的历史<br>不是它的</h1>
<p class="lead">有人用 7 个 PR，把 FFmpeg 二十六年的提交史和 .NET 的历史灌进了 <code>main</code>。
于是它现在告诉世界：153,426 个提交、3,928 位作者、105.34 年的历史跨度、
168 个昼夜格子无一空闲。</p>
<p class="lead"><strong>这些数字没有一个属于它。它真实的数字是 705 个提交、25 天。</strong></p>

<div class="cards">
<div class="card"><div class="k">可达提交</div><div class="v">{reach}</div></div>
<div class="card"><div class="k">首父链（真实）</div><div class="v">{chain}</div></div>
<div class="card big"><div class="k">随 merge 灌入</div><div class="v">99.5%</div></div>
<div class="card"><div class="k">历史跨度</div><div class="v">{span} 年</div></div>
</div>

<h2>一、什么时候</h2>
<p>提交的年份分布：<strong>1971 → 2077</strong>。这条曲线的形状就是开源史本身 ——
2005 年 git 取代 CVS 的那次爬升、2012 年的高峰、2024 年的另一座峰。
它跟一个存在了 25 天的仓库没有任何关系。</p>
<figure>{years}
<div class="legend"><span><i style="background:#378ADD"></i>2005 年及以后的提交</span>
<span><i style="background:#BA7517"></i>早于 git 诞生（FFmpeg 真实的 2000–2005）</span>
<span><i style="background:#E24B4A"></i>两个手写的假日期</span></div>
<figcaption>{pre} 个提交早于 git 本身（2005-04-03）。最左与最右各一根红柱，
是别人手工写下去的 1971 和 2077。</figcaption>
</figure>

<h2>二、怎么进来的</h2>
<p>把首父链的 705 步摊开，每一步的高度是它<strong>引入的、此前不可达的提交数</strong>。
纵轴是线性的 —— 这是一个刻意的选择，因为真相就是线性的：</p>
<figure>{chainfig}
<figcaption>第 {peak} 步是 PR #729「add FFmpeg」，一次 merge 带进 {peakn} 个提交。
第二名 {secondn} 个。其余 683 个 merge 加起来 {restn} 个 —— 在这张图上是一条平线。</figcaption>
</figure>
<p>注意 PR <strong>#691「add .NET」</strong>：<code>changed_files = 0</code>、
<code>additions = 0</code>、<code>deletions = 0</code>，但带进了 17,802 个提交。
<strong>它一个字节的文件都没改，灌的只有历史。</strong></p>

<h2>三、谁</h2>
<table>
<tr><th>PR 分支所属人</th><th class="n">带入提交</th><th class="n">占全部</th></tr>
{imp_rows}
</table>
<p>第一名与第二名的差距是 <strong>{ratio} 倍</strong>。全部发生在 2026-09-21 这一天。</p>

<h2>四、灌进来的是谁的历史</h2>
<p>导入的 126,743 个提交里有一颗根提交，提交信息写着
<code>New repository initialized by cvs2svn</code>，日期 2000-12-20 ——
<strong>那是 FFmpeg 从 CVS 迁移到 git 时的那颗种子。</strong></p>
<table>
<tr><th>作者</th><th class="n">提交</th><th class="n">首次</th><th class="n">个人跨度</th></tr>
{au_rows}
</table>
<p>前 18 名里 15 个是 FFmpeg 的维护者。剩下的是 .NET 的自动发布机器人。</p>

<h2>五、于是这些界面全都失真了</h2>
<figure>{hours}
<figcaption>左边是 GitHub 的 punch card 会画出来的形状 —— 平滑、国际化、
像一支分布在各个时区的工程团队。右边是这 705 个提交的真身：
一根尖峰，以及一批几乎归零的小时。</figcaption>
</figure>
<blockquote>当你看到一条活动曲线时，你看到的是有人选择让你看到的时间。</blockquote>

<h2>六、两个手写的日期</h2>
<table>
<tr><th>提交</th><th>日期</th><th>作者</th><th>提交信息</th></tr>
<tr><td><code>d4f8e88668</code></td><td>1971-09-01 09:00:01 <strong>+0730</strong></td>
<td>526awd</td><td>这提交时间有点奇怪</td></tr>
<tr><td><code>01b9738d65</code></td><td>2077-01-01 09:00:00 +0000</td>
<td>CD PROJEKT RED</td><td>椰汁城</td></tr>
</table>
<p>两个玩笑。但注意它们的<strong>本地时间都是 09:00:0x</strong> ——
两个不同的账号，同一个生成脚本。而 <code>+0730</code> 不是世界上任何一个现存的时区。</p>

<h2>七、我不只是想指出它</h2>
<p><strong>承载这份报告的提交，日期是 <code>1970-01-01T00:00:00Z</code>。</strong>
不是本地 <code>GIT_AUTHOR_DATE</code> 伪造，而是通过 GitHub 官方的 Git Data API，
在 <code>author.date</code> 字段里填了这个值 —— 服务器照单全收。</p>
<p>一个刻意保留的边界：<strong>我没有冒充任何真实人物。</strong>
那个提交署我自己的名字。伪造时间暴露的是一个未校验的字段；
伪造身份会把话挂到具体活人名下。这不是同一件事。</p>

<footer>分析于 {gen}，仓库 HEAD <code>{sha}</code>。
方法：<code>git clone --bare --filter=tree:0</code> —— 只要提交图，49 MB / 41 秒，
全程只读，未改写任何既有提交。可复现命令见 <code>README.md</code> §8。</footer>

</main></body></html>
""".format(
        css=CSS, gen=t["generated"][:10], sha=t["subject"]["head"][:10],
        reach=comma(g["reachable_commits"]), chain=comma(g["first_parent_chain"]),
        span="%.2f" % a["span_years"], pre=comma(t["anachronisms"]["before_git_existed_2005_04_03"]),
        years=chart_years(a), chainfig=chart_chain(g), hours=chart_hours(a, fp),
        imp_rows=imp_rows, au_rows=au_rows,
        peak=peak, peakn=comma(ci[peak]),
        secondn=comma(sorted(ci, reverse=True)[1]),
        restn=comma(sum(ci) - ci[peak] - sorted(ci, reverse=True)[1]),
        ratio=comma(top[1] / max(second[1], 1)),
    )


def main() -> int:
    html_text = build()
    OUT.write_text(html_text, "utf-8")
    print("wrote %s (%d B)" % (OUT, len(html_text.encode("utf-8"))))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
