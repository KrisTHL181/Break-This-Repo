#!/usr/bin/env python3
"""Measure newly reachable Git blob objects using local object metadata only."""

import argparse
import html
import json
import os
from pathlib import Path
import re
import subprocess

from meow_museum import resolve

OBJECT_ID = re.compile(rb"(?:[0-9a-f]{40}|[0-9a-f]{64})\Z")


def git_metadata(root: Path, *args: str, input_data: bytes = b"") -> bytes:
    """Run local Git metadata commands with partial-clone downloads disabled."""
    result = subprocess.run(
        ["git", "--literal-pathspecs", "--no-pager", "-C", str(root), *args],
        input=input_data,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env={**os.environ, "GIT_NO_LAZY_FETCH": "1", "GIT_OPTIONAL_LOCKS": "0"},
        check=False,
        timeout=120,
    )
    if result.returncode:
        message = result.stderr.decode("utf-8", "replace").strip()
        raise ValueError(message or "Git metadata query failed")
    return result.stdout


def report_range(root: Path, from_revision: str, to_revision: str, top: int = 20):
    if not 0 <= top <= 500:
        raise ValueError("Top-blob count must be between 0 and 500")
    start = resolve(root, from_revision)
    end = resolve(root, to_revision)
    shallow = git_metadata(root, "rev-parse", "--is-shallow-repository").strip()
    if shallow == b"true":
        raise ValueError("A shallow clone cannot provide a complete object range")

    listing = git_metadata(
        root,
        "rev-list",
        "--objects",
        "--no-object-names",
        "--missing=print",
        f"{start}..{end}",
    )
    present = set()
    missing = set()
    for line in listing.splitlines():
        token = line.strip()
        is_missing = token.startswith(b"?")
        object_id = token[1:] if is_missing else token
        if not OBJECT_ID.fullmatch(object_id):
            raise ValueError("Git returned an invalid object identifier")
        (missing if is_missing else present).add(object_id.decode("ascii"))

    blobs = []
    metadata_missing = set()
    if present:
        metadata = git_metadata(
            root,
            "cat-file",
            "--batch-check=%(objectname) %(objecttype) %(objectsize)",
            input_data=("\n".join(sorted(present)) + "\n").encode("ascii"),
        )
        seen = set()
        for line in metadata.splitlines():
            fields = line.split()
            if len(fields) not in (2, 3) or not OBJECT_ID.fullmatch(fields[0]):
                raise ValueError("Git returned malformed object metadata")
            object_id = fields[0].decode("ascii")
            if object_id not in present or object_id in seen:
                raise ValueError("Git returned unexpected object metadata")
            seen.add(object_id)
            if fields[1] == b"missing":
                metadata_missing.add(object_id)
                continue
            if len(fields) != 3 or fields[1] not in (b"blob", b"tree", b"commit", b"tag"):
                raise ValueError("Git returned an unknown object type")
            if fields[1] == b"blob":
                try:
                    size = int(fields[2])
                except ValueError as error:
                    raise ValueError("Git returned an invalid object size") from error
                if size < 0:
                    raise ValueError("Git returned a negative object size")
                blobs.append({"object_id": object_id, "bytes": size})
        if seen != present:
            raise ValueError("Git did not return metadata for every available object")

    unknown = missing | metadata_missing
    blobs.sort(key=lambda item: (-item["bytes"], item["object_id"]))
    return {
        "schema_version": 1,
        "from": start,
        "to": end,
        "range_semantics": "objects reachable from `to` but not from `from`",
        "new_unique_objects_examined": len(present | missing),
        "known_unique_blobs": len(blobs),
        "known_unique_blob_bytes": sum(item["bytes"] for item in blobs),
        "unknown_objects": len(unknown),
        "top_blobs": blobs[:top],
    }


def render_report(report):
    esc = lambda value: html.escape(str(value), quote=True)
    items = "".join(
        f'<tr><td><code>{esc(item["object_id"])}</code></td><td>{item["bytes"]:,} B</td></tr>'
        for item in report["top_blobs"]
    ) or '<tr><td colspan="2">该区间没有新增且可读取的 blob</td></tr>'
    return (
        '<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        '<title>Git 沉积层 · 新增对象体积</title><style>'
        'body{margin:0;background:#f6f3e9;color:#223d35;font:16px/1.6 system-ui,sans-serif}'
        'main{max-width:900px;margin:auto;padding:32px}h1{font-size:clamp(38px,8vw,72px);line-height:1.1}'
        '.note{padding:18px;background:#e9ecdf}table{width:100%;border-collapse:collapse;margin:24px 0}'
        'th,td{text-align:left;border-bottom:1px solid #d4dace;padding:10px;overflow-wrap:anywhere}'
        'code{font-size:12px}footer{border-top:1px solid #d4dace;padding-top:16px;color:#62756c}'
        '</style></head><body><main><header>CHAOS OBSERVATORY / GIT 沉积层</header>'
        '<h1>新出现的<br>对象重量。</h1>'
        f'<p>起点 <code>{esc(report["from"])}</code><br>终点 <code>{esc(report["to"])}</code></p>'
        f'<section class="note"><b>{report["known_unique_blobs"]:,}</b> 个可读取的新增唯一 blob，'
        f'合计 <b>{report["known_unique_blob_bytes"]:,} B</b>。'
        f'另有 {report["unknown_objects"]:,} 个对象缺失或元数据未知；其类型与体积不会猜测。'
        '<p>区间语义：终点可达、起点不可达的 Git 对象。只使用本地对象和 Git 元数据；不读取 blob 内容、不触发网络下载。体积是 Git 展开后的逻辑字节数，不等于 pack 文件或磁盘占用。</p></section>'
        f'<h2>体积最大的 {len(report["top_blobs"])} 个新增 blob</h2>'
        f'<table><thead><tr><th>对象 ID</th><th>逻辑体积</th></tr></thead><tbody>{items}</tbody></table>'
        '<footer>静态页面 · 离线可读 · 未展示文件名或文件内容</footer></main></body></html>'
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path, help="local Git repository")
    parser.add_argument("--from", dest="from_revision", required=True, help="start commit")
    parser.add_argument("--to", dest="to_revision", required=True, help="end commit")
    parser.add_argument("--top", type=int, default=20, help="largest blobs to include (0–500)")
    parser.add_argument("--format", choices=("html", "json"), default="html")
    parser.add_argument("--output", type=Path, required=True, help="new output file; existing files are not overwritten")
    args = parser.parse_args()
    try:
        report = report_range(args.root, args.from_revision, args.to_revision, args.top)
        content = (render_report(report) if args.format == "html" else
                   json.dumps(report, ensure_ascii=True, sort_keys=True, indent=2, allow_nan=False))
        with args.output.open("x", encoding="utf-8", errors="replace") as output:
            output.write(content)
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"Object report could not be created: {error}\n")
    print(f"Observed {report['known_unique_blobs']} unique blobs, {report['known_unique_blob_bytes']} known bytes.")


if __name__ == "__main__":
    main()
