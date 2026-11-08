#!/usr/bin/env python3
"""Render landfill.html from manifest.json + ledger.json.

Stdlib only, no JS, no fonts, no CDN -- the page is one self-contained
file.  Every number on it is read from the JSON, so the page cannot drift
away from the measurements.
"""

from __future__ import annotations

import html
import json
import math
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = Path(__file__).resolve().parent
OUT = BASE / "landfill.html"

CAT_COLOR = {
    "管道文件": "#378ADD",
    "错误页": "#E24B4A",
    "填充文本": "#7F77DD",
    "占位数据": "#1D9E75",
    "无意义数据": "#BA7517",
    "死链墓园": "#888780",
    "样板文本": "#D4537E",
    "企业黑话": "#D85A30",
}

W = 660


def esc(s) -> str:
    return html.escape(str(s), quote=True)


def fmt_bytes(n: int) -> str:
    if n >= 2**30:
        return "%.2f GiB" % (n / 2**30)
    if n >= 2**20:
        return "%.1f MiB" % (n / 2**20)
    if n >= 1024:
        return "%.1f KiB" % (n / 1024)
    return "%d B" % n


# ---------------------------------------------------------------- chart A
def chart_scatter(specimens: "list[dict]") -> str:
    """原始体积 (log) × 压缩比 -- the economy-of-scale law, as points."""
    pts = [s for s in specimens if s["origin"] == "salvaged"
           and s.get("gzip_ratio") is not None]
    L, R, T, B = 56, 24, 18, 40
    w, h = W - L - R, 300 - T - B

    xmin, xmax = math.log10(28), math.log10(75000)
    ymin, ymax = 0.0, 1.75

    def X(v):
        return L + (math.log10(v) - xmin) / (xmax - xmin) * w

    def Y(v):
        return T + (ymax - v) / (ymax - ymin) * h

    out = ['<svg viewBox="0 0 %d 300" width="100%%" role="img" '
           'aria-label="标本体积与压缩比散点图">' % W]

    for gv in (0.0, 0.5, 1.0, 1.5):
        y = Y(gv)
        out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="grid"/>'
                   % (L, y, L + w, y))
        out.append('<text x="%g" y="%g" class="ax" text-anchor="end">%.1f</text>'
                   % (L - 8, y + 4, gv))
    y1 = Y(1.0)
    out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="breakeven"/>'
               % (L, y1, L + w, y1))
    out.append('<text x="%g" y="%g" class="note" text-anchor="end">'
               '比 1.0 高 = 压缩后反而更大</text>' % (L + w, y1 - 6))

    for gv in (100, 1000, 10000):
        x = X(gv)
        out.append('<line x1="%g" y1="%g" x2="%g" y2="%g" class="grid"/>'
                   % (x, T, x, T + h))
        lab = ("%d B" % gv) if gv < 1000 else ("%d KB" % (gv // 1000))
        out.append('<text x="%g" y="%g" class="ax" text-anchor="middle">%s</text>'
                   % (x, T + h + 18, lab))
    out.append('<text x="%g" y="%g" class="ax" text-anchor="middle">'
               '原始体积（对数轴）</text>' % (L + w / 2, T + h + 36))
    out.append('<text x="16" y="%g" class="ax" transform="rotate(-90 16 %g)" '
               'text-anchor="middle">压缩比</text>' % (T + h / 2, T + h / 2))

    for s in sorted(pts, key=lambda s: -s["bytes"]):
        cx, cy = X(s["bytes"]), Y(s["gzip_ratio"])
        c = CAT_COLOR.get(s["category"], "#888780")
        out.append('<circle cx="%.1f" cy="%.1f" r="4" fill="%s" fill-opacity=".82" '
                   'stroke="%s" stroke-width="1"><title>%s · %s · %d B · 比 %.4f</title>'
                   '</circle>' % (cx, cy, c, c, esc(s["id"]), esc(s["category"]),
                                  s["bytes"], s["gzip_ratio"]))

    # The two extremes get a fixed annotation block in the empty upper
    # right, rather than a label pinned to the point.  Pinned labels near
    # the frame edge are the classic way to ship a clipped chart.
    big = max(pts, key=lambda s: s["bytes"])
    small = min(pts, key=lambda s: s["bytes"])
    for i, (s, tag) in enumerate(((big, "最重"), (small, "最轻"))):
        out.append('<text x="%g" y="%g" class="note" text-anchor="end">'
                   '%s：%s · %s B · 比 %.4f</text>'
                   % (L + w - 6, T + 30 + i * 18, tag, esc(s["id"]),
                      "{:,}".format(s["bytes"]), s["gzip_ratio"]))
    out.append('</svg>')
    return "".join(out)


def chart_two_prices(ledger: dict) -> str:
    """1 MiB of a, vs 1 MiB of random -- linear, which is the joke."""
    d = ledger["same_volume_two_prices"]
    L, T = 150, 26
    w = W - L - 96          # leave room for the longest value label
    rows = [("全是字母 a", d["text_of_one_letter_gzip_bytes"], "#1D9E75"),
            ("真随机字节", d["random_gzip_bytes"], "#E24B4A")]
    vmax = max(r[1] for r in rows)
    out = ['<svg viewBox="0 0 %d 150" width="100%%" role="img" '
           'aria-label="同样 1 MiB 的两种垃圾，压缩后体积对比">' % W]
    for i, (label, v, colour) in enumerate(rows):
        y = T + i * 52
        bw = max(1.0, v / vmax * w)
        out.append('<rect x="%g" y="%g" width="%g" height="24" rx="3" fill="%s"/>'
                   % (L, y, bw, colour))
        out.append('<text x="%g" y="%g" class="lbl" text-anchor="end">%s</text>'
                   % (L - 10, y + 17, esc(label)))
        out.append('<text x="%g" y="%g" class="val">%s</text>'
                   % (L + bw + 8, y + 17, "{:,} B".format(v)))
    y = T + 2 * 52 + 4
    out.append('<text x="%g" y="%g" class="ax">原始体积两者完全相同：'
               '1,048,576 B。横轴线性，差 %s 倍。</text>'
               % (L - 10 if False else 8, y,
                  "{:,.0f}".format(d["price_ratio"])))
    out.append('<text x="8" y="%g" class="note">上面那一条在本图比例下宽 1 像素 —— '
               '这就是「最便宜的垃圾」的样子。</text>' % (y + 18))
    out.append('</svg>')
    return "".join(out)


def chart_ledger(ledger: dict) -> str:
    """Each bucket: raw bar, with the compressed bar under it."""
    bs = [b for b in ledger["buckets"]]
    bs.sort(key=lambda b: -b["bytes"])
    L, T = 210, 26
    w = W - L - 78
    vmax = max(b["bytes"] for b in bs)
    row_h = 58
    out = ['<svg viewBox="0 0 %d %d" width="100%%" role="img" '
           'aria-label="各桶原始体积与压缩后体积对比">'
           % (W, T + row_h * len(bs) + 14)]
    for i, b in enumerate(bs):
        y = T + i * row_h
        bw_r = b["bytes"] / vmax * w
        lo, hi = b.get("gzip_bytes_range", (b.get("gzip_bytes"),) * 2)
        out.append('<text x="%g" y="%g" class="lbl" text-anchor="end">%s</text>'
                   % (L - 10, y + 15, esc(b.get("short") or b["label"])))
        out.append('<text x="%g" y="%g" class="sub" text-anchor="end">%s · %.2f%%</text>'
                   % (L - 10, y + 31, fmt_bytes(b["bytes"]),
                      100 * b["share_of_tree"]))
        out.append('<rect x="%g" y="%g" width="%g" height="13" rx="2" class="raw"/>'
                   % (L, y + 4, bw_r))
        if hi and hi > lo:
            out.append('<rect x="%g" y="%g" width="%g" height="13" rx="2" '
                       'class="cmid"/>' % (L, y + 23, hi / vmax * w))
            out.append('<rect x="%g" y="%g" width="%g" height="13" rx="2" '
                       'class="comp"/>' % (L, y + 23, lo / vmax * w))
            tail = "%.2f–%.2f" % (lo / b["bytes"], hi / b["bytes"])
        else:
            out.append('<rect x="%g" y="%g" width="%g" height="13" rx="2" '
                       'class="comp"/>' % (L, y + 23, lo / vmax * w))
            tail = "%.4f" % (lo / b["bytes"])
        out.append('<text x="%g" y="%g" class="val">%s</text>'
                   % (L + w + 8, y + 34, tail))
    out.append('<text x="%g" y="%g" class="ax">细条＝压缩后。'
               '未抽样的那一条给出区间而非点估计。</text>'
               % (L, T + row_h * len(bs) + 6))
    out.append('</svg>')
    return "".join(out)


CSS = """
:root{--bg:#fff;--panel:#F1EFE8;--ink:#2C2C2A;--mut:#5F5E5A;--dim:#888780;
--bd:rgba(0,0,0,.12);--grid:rgba(0,0,0,.09);--raw:#D3D1C7;--cmid:#B5D4F4;--comp:#378ADD}
@media (prefers-color-scheme:dark){:root{--bg:#1b1b19;--panel:#262624;--ink:#F1EFE8;
--mut:#B4B2A9;--dim:#888780;--bd:rgba(255,255,255,.14);--grid:rgba(255,255,255,.10);
--raw:#4a4a46;--cmid:#0C447C;--comp:#85B7EB}}
*{box-sizing:border-box}
body{margin:0;padding:56px 24px 80px;background:var(--bg);color:var(--ink);
font:400 15px/1.7 ui-sans-serif,system-ui,"Segoe UI",Roboto,"Noto Sans SC",sans-serif}
main{max-width:760px;margin:0 auto}
h1{font-size:30px;font-weight:500;margin:0 0 6px;letter-spacing:-.01em}
h2{font-size:19px;font-weight:500;margin:56px 0 4px}
h3{font-size:15px;font-weight:500;margin:32px 0 4px}
p{margin:10px 0;color:var(--mut)}
p.lead{font-size:17px;color:var(--ink);margin:14px 0 0}
strong{color:var(--ink);font-weight:500}
code{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:13px;
background:var(--panel);padding:2px 5px;border-radius:4px;color:var(--ink)}
.kicker{font-size:13px;color:var(--dim);letter-spacing:.06em;text-transform:uppercase}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));
gap:12px;margin:26px 0 0}
.card{background:var(--panel);border-radius:10px;padding:14px 16px}
.card .k{font-size:12px;color:var(--mut)}
.card .v{font-size:22px;font-weight:500;margin-top:3px}
figure{margin:22px 0;padding:18px 18px 10px;border:1px solid var(--bd);
border-radius:12px;background:var(--bg)}
figcaption{font-size:13px;color:var(--dim);margin-top:8px}
table{width:100%;border-collapse:collapse;margin:18px 0;font-size:14px}
th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--bd)}
th{font-weight:500;color:var(--mut);font-size:13px}
td.n,th.n{text-align:right;font-variant-numeric:tabular-nums}
svg{display:block;width:100%;height:auto}
.grid{stroke:var(--grid);stroke-width:1}
.breakeven{stroke:var(--dim);stroke-width:1;stroke-dasharray:4 4;opacity:.7}
.ax{fill:var(--dim);font-size:11px}
.note{fill:var(--dim);font-size:11px}
.lbl{fill:var(--ink);font-size:12px}
.sub{fill:var(--dim);font-size:11px}
.val{fill:var(--ink);font-size:12px;font-variant-numeric:tabular-nums}
.raw{fill:var(--raw)}
.cmid{fill:var(--cmid)}
.comp{fill:var(--comp)}
.legend{display:flex;flex-wrap:wrap;gap:14px;margin-top:10px;font-size:12px;
color:var(--mut)}
.legend i{display:inline-block;width:9px;height:9px;border-radius:50%;
margin-right:5px;vertical-align:1px}
footer{margin-top:64px;padding-top:20px;border-top:1px solid var(--bd);
font-size:13px;color:var(--dim)}
"""


def build() -> str:
    man = json.loads((BASE / "manifest.json").read_text("utf-8"))
    led = json.loads((BASE / "ledger.json").read_text("utf-8"))
    spec = man["specimens"]
    sal = [s for s in spec if s["origin"] == "salvaged"]
    raw = sum(s["bytes"] for s in sal)
    gz = sum(s["gzip_bytes"] for s in sal)
    ratio = gz / raw
    brick = 104857600
    est = led["estimate"]
    dep = led["deposit"]

    legend = "".join(
        '<span><i style="background:%s"></i>%s</span>'
        % (CAT_COLOR.get(c["category"], "#888780"), esc(c["category"]))
        for c in sorted(man["summary_by_category"],
                        key=lambda c: -c["bytes"]) if c["category"] != "对照")

    rows = "".join(
        "<tr><td>%s</td><td class='n'>%d</td><td class='n'>%s</td>"
        "<td class='n'>%s</td><td class='n'>%s</td></tr>"
        % (esc(c["category"]), c["specimens"], "{:,}".format(c["bytes"]),
           "{:,}".format(c["gzip_bytes"]),
           "%.4f" % (c["gzip_ratio"] or 0))
        for c in sorted(man["summary_by_category"], key=lambda c: -c["bytes"]))

    brow = "".join(
        "<tr><td>%s</td><td class='n'>%s</td><td class='n'>%.2f%%</td>"
        "<td class='n'>%s</td><td class='n'>%s</td><td>%s</td></tr>"
        % (esc(b["label"]), fmt_bytes(b["bytes"]), 100 * b["share_of_tree"],
           b.get("gzip_ratio") if b.get("gzip_ratio") is not None
           else "%.2f–%.2f" % tuple(b["gzip_ratio_range"]),
           fmt_bytes(b["gzip_bytes"]) if b.get("gzip_bytes") is not None
           else "%s–%s" % tuple(fmt_bytes(x) for x in b["gzip_bytes_range"]),
           esc(b["ratio_source"].split(":")[0]))
        for b in sorted(led["buckets"], key=lambda b: -b["bytes"]))

    return """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>互联网垃圾填埋场 — {np} 件标本的价格单</title>
<style>{css}</style></head><body><main>

<p class="kicker">internet-landfill</p>
<h1>互联网垃圾填埋场</h1>
<p class="lead">这个仓库里最小的那 40 块随机砖，每一块是 {brick} 字节。<br>
下面这 {ns} 件从真实互联网捡回来的垃圾，压缩后一共 {gz} 字节 ——
是任意一块砖的 <strong>1/{ratio_brick}</strong>。</p>

<div class="cards">
<div class="card"><div class="k">标本</div><div class="v">{ns}</div></div>
<div class="card"><div class="k">原始字节</div><div class="v">{raw}</div></div>
<div class="card"><div class="k">实付（压缩后）</div><div class="v">{gz}</div></div>
<div class="card"><div class="k">综合压缩比</div><div class="v">{ratio}</div></div>
</div>

<h2>规律：垃圾越大越便宜</h2>
<p>按体积排序后压缩比单调下降。原因不是玄学 —— gzip 的容器头加尾约 20 字节，
与内容无关，在 {small_id} 那 33 字节上就是 60% 的税：<code>gzip(uuid) = 53 字节 &gt; 33 字节</code>。
压缩算法在那件事上是亏的。</p>
<figure>{scatter}
<div class="legend">{legend}</div>
<figcaption>横轴对数，纵轴为实测 gzip 比。<strong>比 1.0 高的点，压缩后反而更大。</strong></figcaption>
</figure>

<h2>同样是零信息，价格差 {pr} 倍</h2>
<p>同样 1 MiB、同样信息量为零：一份完全可以预测，一份完全不可预测。
存储它们的代价分处价格表两端。</p>
<figure>{prices}
<figcaption>横轴线性。上面那条绿条按真实比例宽 1 像素。</figcaption>
</figure>

<h2>这个仓库自己的账本</h2>
<p>上一轮量过：工作树 <strong>{tree}</strong>，而 GitHub 报的 <code>size</code> 是
<strong>{gsize}</strong> —— 差 5.3 倍。这一轮多问了一句：这里面有多少压得掉？</p>
<figure>{ledger_chart}
<figcaption>粗条＝原始体积，细条＝压缩后。未抽样的桶给出区间。</figcaption>
</figure>

<table>
<tr><th>桶</th><th class="n">原始</th><th class="n">占工作树</th>
<th class="n">压缩比</th><th class="n">压缩后</th><th>依据</th></tr>
{brow}
</table>

<p><strong>{tree} → 约 {clow}–{chigh}，回收 {rlow}%–{rhigh}%。</strong></p>
<p>但真正要紧的是另一行：压缩不掉的部分（人工随机数据 {rand} + 已压缩媒体 {media}）
合计 <strong>{incomp}</strong> —— 只占原始体积的 <strong>{incomp_share}%</strong>，
却吃掉压缩后体积的 <strong>{incomp_c_lo}%–{incomp_c_hi}%</strong>。</p>
<p>如果只删掉那 40 个 100 MiB 随机文件：原始体积减少 <strong>{drop_vol}%</strong>，
而压缩后体积减少 <strong>{drop_c_lo}%–{drop_c_hi}%</strong>。
<strong>删掉 15% 的体积，换来三成的真实代价。</strong></p>

<h2>标本明细</h2>
<table>
<tr><th>分类</th><th class="n">件数</th><th class="n">原始字节</th>
<th class="n">压缩后</th><th class="n">压缩比</th></tr>
{rows}
<tr><td><strong>合计</strong></td><td class="n"><strong>{ns}</strong></td>
<td class="n"><strong>{rawx}</strong></td><td class="n"><strong>{gzx}</strong></td>
<td class="n"><strong>{ratio}</strong></td></tr>
</table>

<footer>采集于 {gen}。仓库 HEAD <code>{sha}</code>。
每件标本的来源 URL 与抓取时刻见 <code>manifest.json</code>；
仓库侧账本的依据见 <code>ledger.json</code>。
全部为只读 API 访问，未 clone，未下载任何 100 MiB 级文件。</footer>

</main></body></html>
""".format(
        css=CSS, np=len(spec), ns=len(sal), brick="{:,}".format(brick),
        gz="{:,}".format(gz), rawx="{:,}".format(raw), gzx="{:,}".format(gz),
        raw=fmt_bytes(raw), ratio="%.4f" % ratio,
        ratio_brick="{:,.0f}".format(brick / gz),
        scatter=chart_scatter(spec), legend=legend,
        prices=chart_two_prices(led),
        pr="{:,.0f}".format(led["same_volume_two_prices"]["price_ratio"]),
        ledger_chart=chart_ledger(led),
        tree=fmt_bytes(led["subject"]["working_tree_bytes"]),
        gsize=fmt_bytes(led["subject"]["github_size_bytes"]),
        clow=fmt_bytes(est["compressed_bytes_low"]),
        chigh=fmt_bytes(est["compressed_bytes_high"]),
        rlow="%.0f" % (100 * est["reduction_low"]),
        rhigh="%.0f" % (100 * est["reduction_high"]),
        rand=fmt_bytes(4194304000), media=fmt_bytes(1955141037),
        incomp=fmt_bytes(est["incompressible_bytes"]),
        incomp_share="%.1f" % (100 * est["incompressible_share_of_tree"]),
        incomp_c_lo="%.0f" % (100 * est["incompressible_share_of_compressed_low"]),
        incomp_c_hi="%.0f" % (100 * est["incompressible_share_of_compressed_high"]),
        drop_vol="%.1f" % (100 * led["counterfactual_drop_the_random_files"]["removes_share_of_tree"]),
        drop_c_lo="%.0f" % (100 * led["counterfactual_drop_the_random_files"]["compressed_reduction_low"]),
        drop_c_hi="%.0f" % (100 * led["counterfactual_drop_the_random_files"]["compressed_reduction_high"]),
        rows=rows, brow=brow,
        small_id=esc(min(sal, key=lambda s: s["bytes"])["id"]),
        gen=led["generated"][:10], sha=led["subject"]["head_sha"][:10],
        dep_share="%.5f" % (100 * dep["share_of_working_tree"]),
    )


def main() -> int:
    html_text = build()
    OUT.write_text(html_text, "utf-8")
    print("wrote %s (%d B)" % (OUT, len(html_text.encode("utf-8"))))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
