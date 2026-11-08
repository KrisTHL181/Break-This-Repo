#!/usr/bin/env python3
"""Sample the subject repository so the ledger in README.md is checkable.

Two modes, both read-only and neither needs a clone:

    python sample_repo.py ext  novel wallpapers            # 扩展名字节普查
    python sample_repo.py comp novel --n 14                # 分层抽样实测压缩比

`ext` answers "what format is this bucket actually made of" -- it is the
cheap step that stops you from extrapolating a text compression ratio
over a pile of PNGs.  `comp` fetches a size-stratified sample of real
blobs and measures gzip/lzma on them.

Everything goes through the `gh` CLI.  Deliberately not urllib: on a host
behind a MITM proxy, Python's ssl rejects the re-signed certificate
("Missing Authority Key Identifier") while gh is fine.
"""

from __future__ import annotations

import argparse
import collections
import gzip
import json
import lzma
import re
import subprocess
import sys
import time
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

UP = "KrisTHL181/Break-This-Repo"
MAX_BLOB = 4 * 1024 * 1024      # skip anything bigger than this in a sample
RE_EXT = re.compile(r"(\.[A-Za-z0-9]{1,8})$")


def gh(path: str, raw: bool = False, tries: int = 4) -> bytes:
    cmd = ["gh", "api", path]
    if raw:
        cmd += ["-H", "Accept: application/vnd.github.raw"]
    for attempt in range(tries):
        p = subprocess.run(cmd, capture_output=True)
        if p.returncode == 0:
            return p.stdout
        err = p.stderr.decode("utf-8", "replace")
        # a ~7 MB tree listing gets reset now and then; it is transient
        if attempt < tries - 1 and any(t in err for t in (
                "forcibly closed", "connection reset", "unexpected EOF",
                "i/o timeout", "502", "503", "504")):
            time.sleep(2 ** attempt)
            continue
        raise RuntimeError("%s -> %s" % (path, err[:300]))
    raise RuntimeError(path)


def head_sha() -> str:
    return json.loads(gh("repos/%s/commits/main" % UP))["sha"]


def list_tree(sha: str, root: str) -> "tuple[list, bool]":
    """Return (blob entries, truncated?)."""
    t = json.loads(gh("repos/%s/git/trees/%s:%s?recursive=1" % (UP, sha, root)))
    blobs = [e for e in t.get("tree", []) if e["type"] == "blob"]
    return blobs, bool(t.get("truncated"))


def census(sha: str, roots: "list[str]") -> dict:
    per_root = {}
    agg = collections.defaultdict(lambda: [0, 0])
    grand = 0
    for root in roots:
        try:
            blobs, trunc = list_tree(sha, root)
        except RuntimeError as exc:
            print("%-28s UNREADABLE (%s)" % (root, str(exc)[:60]))
            continue
        sub = 0
        for e in blobs:
            ext = (RE_EXT.search(e["path"]) or [None, "(none)"])[1].lower()
            agg[ext][0] += 1
            agg[ext][1] += e.get("size", 0)
            sub += e.get("size", 0)
        grand += sub
        per_root[root] = {"bytes": sub, "blobs": len(blobs), "truncated": trunc}
        print("%-28s %12d B  %s" % (root, sub, "TRUNCATED" if trunc else ""))

    print("\n%-12s %8s %14s %8s" % ("ext", "files", "bytes", "share"))
    for ext, (n, b) in sorted(agg.items(), key=lambda kv: -kv[1][1])[:24]:
        print("%-12s %8d %14d %7.2f%%" % (ext, n, b, 100 * b / grand if grand else 0))
    print("\ncensus total %d B" % grand)

    return {
        "head": sha,
        "mode": "ext",
        "per_root": per_root,
        "by_extension": {k: {"files": v[0], "bytes": v[1]} for k, v in agg.items()},
        "total_bytes": grand,
        "note": "TRUNCATED 表示该目录递归 listing 被 GitHub 截断（10 万条目/7 MB），"
                "此时百分比是有偏的样本而非全量。",
    }


def compress(sha: str, root: str, n: int) -> dict:
    blobs, trunc = list_tree(sha, root)
    total = sum(e.get("size", 0) for e in blobs)
    print("HEAD %s  %s: %d blobs, %d B (%.2f GiB)%s"
          % (sha[:12], root, len(blobs), total, total / 2**30,
             "  !! TRUNCATED" if trunc else ""))

    ext_agg = collections.defaultdict(lambda: [0, 0])
    for e in blobs:
        ext = (RE_EXT.search(e["path"]) or [None, "(none)"])[1].lower()
        ext_agg[ext][0] += 1
        ext_agg[ext][1] += e.get("size", 0)
    top = sorted(ext_agg.items(), key=lambda kv: -kv[1][1])[:5]
    print("  构成: " + "  ".join("%s %.1f%%" % (k, 100 * v[1] / total)
                                 for k, v in top if total))

    # size-stratified: walk the size-sorted list at even intervals, so the
    # heavy end of the distribution is represented rather than oversampled
    # by thousands of tiny files
    pool = sorted((e for e in blobs
                   if 0 < e.get("size", 0) <= MAX_BLOB),
                  key=lambda e: e["size"])
    if not pool:
        raise RuntimeError("no usable blobs")
    step = max(1, len(pool) // n)
    picks = pool[::step][:n]

    rows, raw, gz, xz = [], 0, 0, 0
    print("\n%-46s %10s %9s %9s" % ("path", "bytes", "gzip", "lzma"))
    for e in picks:
        api = "repos/%s/contents/%s/%s?ref=%s" % (
            UP, root, e["path"].replace("#", "%23"), sha)
        try:
            data = gh(api, raw=True)
        except RuntimeError as exc:
            print("%-46s  SKIP %s" % (e["path"][-46:], str(exc)[:40]))
            continue
        if not data or len(data) > MAX_BLOB:
            continue
        g = len(gzip.compress(data, 9, mtime=0))
        x = len(lzma.compress(data, preset=6))
        raw += len(data); gz += g; xz += x
        rows.append({"path": e["path"], "bytes": len(data), "gzip_bytes": g,
                     "lzma_bytes": x, "gzip_ratio": round(g / len(data), 4),
                     "lzma_ratio": round(x / len(data), 4)})
        print("%-46s %10d %9.4f %9.4f" % (e["path"][-46:], len(data),
                                          g / len(data), x / len(data)))

    res = {
        "head": sha, "mode": "comp", "root": root,
        "total_bytes": total, "blobs": len(blobs), "truncated": trunc,
        "sample": rows, "sample_raw_bytes": raw,
        "sample_gzip_bytes": gz, "sample_lzma_bytes": xz,
        "sample_gzip_ratio": round(gz / raw, 4) if raw else None,
        "sample_lzma_ratio": round(xz / raw, 4) if raw else None,
        "caveat": "抽样为分层采样（按 size 排序等间隔取）。未抽样部分不可据此断言。"
                  "gzip 主口径 —— git pack 用 zlib，与 gzip 同族。",
    }
    if raw:
        print("\nsample raw=%d  gzip=%.4f  lzma=%.4f"
              % (raw, gz / raw, xz / raw))
    return res


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("mode", choices=["ext", "comp"])
    ap.add_argument("roots", nargs="+")
    ap.add_argument("--n", type=int, default=14, help="sample size for comp")
    ap.add_argument("--out", default=None)
    args = ap.parse_args()

    sha = head_sha()
    if args.mode == "ext":
        res = census(sha, args.roots)
        default = "census.json"
    else:
        res = compress(sha, args.roots[0], args.n)
        default = "compress-%s.json" % args.roots[0].replace("/", "_")

    out = Path(args.out or default)
    out.write_text(json.dumps(res, ensure_ascii=False, indent=2), "utf-8")
    print("\n-> %s" % out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
