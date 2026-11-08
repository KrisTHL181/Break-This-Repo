#!/usr/bin/env python3
"""Create an offline, path-free trend page from saved observations."""

import argparse
import html
from pathlib import Path
import unicodedata

from observation_reports import load_series

METRICS = (
    ("files_observed", "文件数", "个"),
    ("bytes_observed", "已知体积", "B"),
    ("maximum_depth", "最大深度", "层"),
    ("chaos_index", "混沌指数", "分"),
)


def render_trend(snapshots):
    """Render an accessible static page; snapshot file paths are never copied."""
    def esc(value):
        visible = []
        for char in str(value):
            codepoint = ord(char)
            if 0xDC80 <= codepoint <= 0xDCFF:
                visible.append(f"\\x{codepoint - 0xDC00:02x}")
            elif unicodedata.category(char) in {"Cc", "Cf", "Cs"}:
                visible.append(f"\\u{codepoint:04x}")
            else:
                visible.append(char)
        return html.escape("".join(visible), quote=True)
    labels = [snapshot["label"] or f"快照 {index}" for index, snapshot in enumerate(snapshots, 1)]
    rows = []
    charts = []
    width, height, margin = 720, 180, 34
    for key, title, unit in METRICS:
        values = [snapshot[key] for snapshot in snapshots]
        low, high = min(values), max(values)
        span = high - low or 1
        points = []
        for index, value in enumerate(values):
            x = margin + index * (width - 2 * margin) / max(1, len(values) - 1)
            y = height - margin - (value - low) * (height - 2 * margin) / span
            points.append((x, y))
        polyline = " ".join(f"{x:.1f},{y:.1f}" for x, y in points)
        dots = "".join(
            f'<circle cx="{x:.1f}" cy="{y:.1f}" r="5"><title>{esc(labels[index])}: '
            f'{values[index]:g} {esc(unit)}</title></circle>'
            for index, (x, y) in enumerate(points)
        )
        y_labels = f'<text x="{margin}" y="18">{high:g} {esc(unit)}</text><text x="{margin}" y="{height - 4}">{low:g} {esc(unit)}</text>'
        charts.append(
            f'<section class="chart"><h2>{esc(title)}</h2><svg viewBox="0 0 {width} {height}" role="img" '
            f'aria-label="{esc(title)}趋势，从 {esc(labels[0])} 到 {esc(labels[-1])}">'
            f'<line x1="{margin}" y1="{margin}" x2="{width-margin}" y2="{margin}" class="gridline"/>'
            f'<line x1="{margin}" y1="{height-margin}" x2="{width-margin}" y2="{height-margin}" class="gridline"/>'
            f'{y_labels}<polyline points="{polyline}"/>{dots}</svg></section>'
        )
        cells = "".join(f'<td>{values[index]:g} {esc(unit)}</td>' for index in range(len(values)))
        rows.append(f'<tr><th scope="row">{esc(title)}</th>{cells}</tr>')

    headers = "".join(f"<th scope=\"col\">{esc(label)}</th>" for label in labels)
    snapshot_notes = "".join(
        f'<li>{esc(labels[index])}: {"扫描达到上限" if item["truncated"] else "完整扫描"}</li>'
        for index, item in enumerate(snapshots)
    )
    return (
        '<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        '<meta name="color-scheme" content="light">'
        '<title>喵气象 · 仓库趋势</title><style>'
        ':root{color-scheme:light;--paper:#f6f3e9;--ink:#223d35;--muted:#62756c;--line:#d4dace;--accent:#728566}'
        '*{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);font:16px/1.6 system-ui,sans-serif}'
        'main{max-width:1000px;margin:auto;padding:32px}header,footer{border-bottom:1px solid var(--line);padding:12px 0}'
        'h1{font-size:clamp(38px,8vw,72px);line-height:1.1;letter-spacing:-3px}h2{font-size:20px}'
        '.charts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.chart{background:#fffdf7;border:1px solid var(--line);padding:16px;border-radius:8px;min-width:0}'
        'svg{width:100%;overflow:visible;font:12px system-ui,sans-serif;fill:var(--muted)}polyline{fill:none;stroke:var(--accent);stroke-width:3}circle{fill:var(--accent)}.gridline{stroke:var(--line)}'
        'table{width:100%;border-collapse:collapse;margin:28px 0}th,td{text-align:left;border-bottom:1px solid var(--line);padding:10px;overflow-wrap:anywhere}.note{padding:18px;background:#e9ecdf}'
        '@media(max-width:680px){main{padding:20px}.charts{grid-template-columns:1fr}table{font-size:13px}}'
        '</style></head><body><main><header>CHAOS OBSERVATORY / 喵气象</header>'
        '<h1>仓库天气，<br>持续变化。</h1>'
        '<p>把多个离线观测快照排成一条时间线。页面只包含展示标签和聚合指标，不包含扫描路径或快照文件名。</p>'
        f'<section class="charts" aria-label="观测趋势">{"".join(charts)}</section>'
        f'<table><thead><tr><th scope="col">指标</th>{headers}</tr></thead><tbody>{"".join(rows)}</tbody></table>'
        f'<aside class="note"><h2>观测范围</h2><ul>{snapshot_notes}</ul>'
        '<p>文件数、已知字节数、最大深度和混沌指数来自各快照。达到扫描上限的样本不代表完整仓库。趋势图是静态 SVG，无脚本、服务器或外部资源。</p></aside>'
        '<footer>离线可读 · 只比较元数据</footer></main></body></html>'
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshots", nargs="+", type=Path, help="observation JSON files in chronological order")
    parser.add_argument("--output", type=Path, required=True, help="new HTML file; existing files are not overwritten")
    args = parser.parse_args()
    try:
        snapshots = load_series(args.snapshots)
        with args.output.open("x", encoding="utf-8", errors="replace") as output:
            output.write(render_trend(snapshots))
    except (ValueError, OSError) as error:
        parser.exit(1, f"Trend page could not be created: {error}\n")
    print(f"Trend page written with {len(snapshots)} snapshots.")


if __name__ == "__main__":
    main()
