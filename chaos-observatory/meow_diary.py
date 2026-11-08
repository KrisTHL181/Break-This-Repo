#!/usr/bin/env python3
"""Compare two local commits and export a Markdown diary or an offline exhibit."""

import argparse
from collections import Counter
import html
from pathlib import Path
import re
import subprocess

from meow_museum import BASELINE, git, observe, render, resolve

REPOSITORY = "https://github.com/KrisTHL181/Break-This-Repo"


def changes(root, before, after):
    # Exact renames only: no similarity scan or file-content reads in a partial clone.
    fields = git(root, "diff", "--no-ext-diff", "--no-textconv", "--find-renames=100%",
                 "--name-status", "-z", before, after, "--").split(b"\0")
    result = []
    index = 0
    while index < len(fields) - 1:
        status = fields[index].decode("ascii")
        path = fields[index + 1].decode("utf-8", "replace")
        index += 2
        old_path = None
        if status.startswith("R"):
            old_path = path
            path = fields[index].decode("utf-8", "replace")
            index += 1
        result.append({"status": status[0], "path": path, "old_path": old_path})
    return result


def compare(root, before, after="HEAD", baseline=BASELINE, limit=100):
    if not 1 <= limit <= 1000:
        raise ValueError("Event limit must be between 1 and 1000")
    before, after = resolve(root, before), resolve(root, after)
    git(root, "merge-base", "--is-ancestor", before, after)
    earlier = observe(root, baseline, before)
    later = observe(root, baseline, after)
    # First-parent integration events avoid counting a branch commit AND its merge.
    # Subtract everything already reachable from the start, even when the start
    # arrived via a side branch. The end's first-parent history lists integrations.
    selected = git(root, "rev-list", "--first-parent", before + ".." + after).decode().splitlines()
    event_total = len(selected)
    events = []
    for sha in selected[:limit]:
        parent = resolve(root, sha + "^1")
        delta = changes(root, parent, sha)
        events.append({"sha": sha, "counts": dict(Counter(x["status"] for x in delta)),
                       "subject": git(root, "log", "-1", "--format=%s", sha).decode("utf-8", "replace").strip(),
                       "url": REPOSITORY + "/commit/" + sha})
    previous = {e["directory"]: e["status"] for e in earlier["entries"]}
    lost = [e["directory"] for e in later["entries"] if previous[e["directory"]] == "retained" and e["status"] != "retained"]
    changed = changes(root, before, after)
    touched = sorted({p.split("/", 1)[0] for item in changed for p in [item["path"], item["old_path"]] if p and "/" in p})
    return {"before": before, "after": after, "snapshot": later, "changes": changed,
            "counts": dict(Counter(x["status"] for x in changed)), "lost": lost,
            "touched": touched, "events": events, "event_total": event_total,
            "truncated": event_total > limit}


def bulletin(report):
    return f"本期 {len(report['touched'])} 个顶层目录出现净变化，猫爪保留区减少 {len(report['lost'])} 处。"


def safe_text(value):
    # Escape Markdown structure and HTML; control characters stay visible.
    value = str(value).replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    return re.sub(r"([\\`*_{}\[\]()#+.!|>~-])", r"\\\1", html.escape(value))


def markdown(report):
    counts = report["counts"]
    lines = ["# 猫爪消退日报", "", bulletin(report), "",
             f"观测区间：{report['before']} → {report['after']}", "",
             "按起点与终点的净差异计数：" + "、".join(f"{label} {counts.get(key, 0)}" for key, label in
             [("A", "新增"), ("M", "修改"), ("D", "删除"), ("R", "完整更名"), ("T", "类型变化")]) + "。", "",
             "## 猫爪消退目录", ""]
    lines += ["- " + safe_text(name) for name in report["lost"]] or ["本期没有猫爪消退。"]
    lines += ["", "## 主线近期事件", ""]
    for event in report["events"]:
        counts_text = ", ".join(f"{key}={value}" for key, value in sorted(event["counts"].items())) or "无净文件变化"
        lines += [f"- [{event['sha'][:10]}]({event['url']}) {safe_text(event['subject'])} · {counts_text}"]
    if not report["events"]:
        lines.append("这个区间没有主线事件。")
    if report["truncated"]:
        lines.append(f"仅列最近 {len(report['events'])} / {report['event_total']} 个事件；净变化与猫爪统计仍覆盖完整区间。")
    if "chaos-observatory" in report["lost"]:
        lines += ["", "> 馆藏故事：观测站迎来新改动，也覆盖了自己的最近猫爪提交。研究者成为了标本。"]
    lines += ["", "## 统计口径", "", "这是一份可指定任意区间的静态日报，不是定时任务。新增/修改/删除按端点净差异计算；期间新增后又删除的文件不会计入。完整更名单列，只有内容完全相同的移动识别为更名。事件按第一父链列出，每次合并计一次，避免重复累计分支提交；事件计数不能相加当作区间净变化。", ""]
    return "\n".join(lines)


def event_panel(report):
    esc = html.escape
    items = "".join(f'<li><a href="{e["url"]}">{e["sha"][:10]}</a> {esc(e["subject"])}</li>' for e in report["events"])
    if not items:
        items = "<li>这个区间没有主线事件。</li>"
    detail = "、".join(f"{label} {report['counts'].get(key, 0)}" for key, label in
                       [("A", "新增"), ("M", "修改"), ("D", "删除"), ("R", "完整更名"), ("T", "类型变化")])
    story = "<p>馆藏故事：观测站覆盖了自己的猫爪。研究者成为了标本。</p>" if "chaos-observatory" in report["lost"] else ""
    return (f'<section class="method" style="margin:0 0 32px"><h2>猫爪消退日报</h2><p>{bulletin(report)}</p>'
            f'<p>区间净变化：{detail}。更名仅识别内容完全一致的移动。</p>'
            f'<p><code>{report["before"][:10]} → {report["after"][:10]}</code> · 主线事件 {len(report["events"])} / {report["event_total"]}；按第一父链计数。</p>'
            f'{story}<ul>{items}</ul><p>端点净变化不等于事件计数之和。此页为静态快照。</p></section>')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--from", dest="before", required=True)
    parser.add_argument("--to", default="HEAD")
    parser.add_argument("--baseline", default=BASELINE)
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument("--format", choices=["markdown", "html"], default="markdown")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        report = compare(args.root, args.before, args.to, args.baseline, args.limit)
        content = markdown(report) if args.format == "markdown" else render(report["snapshot"], event_panel(report))
        with args.output.open("x", encoding="utf-8") as output:
            output.write(content)
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"Diary could not open: {error}\n")
    print(bulletin(report))


if __name__ == "__main__":
    main()
