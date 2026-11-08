#!/usr/bin/env python3
"""vigil.py -- manufacture a content-free commit history, and price it.

Companion to clock.py.  clock.py measures what a history *claims*; this one
builds a history that claims a great deal and contains nothing.

The observation it exists to price: a commit is roughly 120 bytes of pack
once delta-compressed against its neighbour, and it does not have to carry
any file change at all.  So "number of commits" is a quantity you can buy.
Importing a real project buys it at ~4,400 bytes per commit; this buys it
at ~120.  Same number, 35x cheaper, and nothing is duplicated.

    python vigil.py measure --n 100000      # build offline, pack, report
    python vigil.py emit --n 1483510 --base <sha> > chain.fi
    git -C repo fast-import --quiet < chain.fi

The chain produced by `emit` modifies nothing: every commit reuses its
parent's tree, so fast-import never has to load a tree object and a
--filter=tree:0 clone is enough to build the whole thing.  Verified.
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# --- the vigil ------------------------------------------------------------
# A watchman, one entry per second, all of them identical, none of them
# reporting anything, because there is nothing to report: the entries carry
# zero bytes of change.
KEEPER = b"vigil"
KEEPER_MAIL = b"vigil@invalid.example"
CHIME = "一切正常。".encode("utf-8")          # "all normal."
ROOT_NOTE = (
    "这里没有别的东西。\n\n"
    "从这条提交开始，往后每一条都说同一句话，一秒一条，一共 1,483,510 条。\n"
    "它们不改动任何一个文件 —— 这一整条链共用同一棵树。\n"
    "见 forged-clock/VIGIL.md。\n"
).encode("utf-8")

LINUX_KERNEL_COMMITS = 1483509      # torvalds/linux, measured 2026-09-23


def emit(n: int, base: str, t_end: int, out, branch: str = "vigil") -> None:
    """Write a fast-import stream for n commits, one second apart, to `out`.

    `base` is the parent of the first commit.  Pass None (or all zeros) to
    make the chain a standalone root -- which is only useful for measuring,
    since a history with no merge base against main cannot be auto-merged.
    """
    rooted = bool(base) and set(base) != {"0"}
    w = out.write
    ref = b"commit refs/heads/" + branch.encode("ascii") + b"\n"
    t0 = t_end - (n - 1) if n > 1 else t_end
    for i in range(n):
        ts = t0 + i
        msg = ROOT_NOTE if i == 0 else CHIME
        w(ref)
        w(b"mark :%d\n" % (i + 1))
        w(b"author %s <%s> %d +0000\n" % (KEEPER, KEEPER_MAIL, ts))
        w(b"committer %s <%s> %d +0000\n" % (KEEPER, KEEPER_MAIL, ts))
        w(b"data %d\n" % len(msg))
        w(msg)
        w(b"\n")
        # no M/D lines, ever: the tree is inherited, so fast-import never
        # reads a tree object and this works in a :tree:0 partial clone
        if i == 0:
            if rooted:
                w(b"from %s\n" % base.encode())
        else:
            w(b"from :%d\n" % i)
        w(b"\n")
    w(b"done\n")
    out.flush()


def measure(n: int) -> dict:
    """Build the chain for real, pack it, and report what it costs."""
    tmp = Path(tempfile.mkdtemp(prefix="vigil-"))
    try:
        repo = tmp / "probe.git"
        subprocess.run(["git", "init", "-q", "--bare", str(repo)], check=True)
        t0 = time.time()
        fi = tmp / "chain.fi"
        with open(fi, "wb") as fh:
            emit(n, "0" * 40, int(time.time()), fh)
        gen_s = time.time() - t0

        t0 = time.time()
        with open(fi, "rb") as fh:
            subprocess.run(["git", "-C", str(repo), "fast-import", "--quiet"],
                           stdin=fh, check=True)
        import_s = time.time() - t0

        # a root chain (no base) is what we measure: it keeps the probe
        # self-contained, and the per-commit delta cost is the same either way
        t0 = time.time()
        subprocess.run(["git", "-C", str(repo), "repack", "-adf",
                        "--window=250", "--depth=50"], check=True)
        pack_s = time.time() - t0

        out = subprocess.run(["git", "-C", str(repo), "count-objects", "-v"],
                             capture_output=True, text=True).stdout
        stats = dict(
            line.split(": ") for line in out.strip().splitlines() if ": " in line)
        in_pack = int(stats.get("in-pack", 0))
        # `count-objects -v` reports size-pack in KiB, not bytes
        size_pack = int(stats.get("size-pack", 0)) * 1024
        commits = int(subprocess.run(
            ["git", "-C", str(repo), "rev-list", "--count", "vigil"],
            capture_output=True, text=True).stdout.strip() or 0)
        return {
            "commits": commits,
            "objects_in_pack": in_pack,
            "size_pack_bytes": size_pack,
            "bytes_per_commit": round(size_pack / max(commits, 1), 2),
            "generate_seconds": round(gen_s, 2),
            "import_seconds": round(import_s, 2),
            "repack_seconds": round(pack_s, 2),
            "stream_bytes": fi.stat().st_size,
            "kernel_commits": LINUX_KERNEL_COMMITS,
            "cost_to_match_kernel": round(
                size_pack / max(commits, 1) * LINUX_KERNEL_COMMITS),
        }
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def self_test() -> int:
    ok = True

    def check(label, got, want):
        nonlocal ok
        if got != want:
            ok = False
            print("FAIL %-38s got %r want %r" % (label, got, want))
        else:
            print("ok   %-38s %r" % (label, got))

    import io
    buf = io.BytesIO()
    emit(3, "a" * 40, 1_700_000_000, buf)
    s = buf.getvalue()
    check("one commit header per commit", s.count(b"commit refs/heads/vigil"), 3)
    check("chime appears twice", s.count(CHIME), 2)
    check("first commit roots on base", b"from " + b"a" * 40 in s, True)
    check("later commits chain by mark", b"from :1\n" in s and b"from :2\n" in s, True)
    check("never writes a file", b"\nM " in s or s.startswith(b"M "), False)
    check("stream terminates", s.endswith(b"done\n"), True)

    buf = io.BytesIO()
    emit(1, "b" * 40, 1_700_000_000, buf)
    check("single commit uses the root note", ROOT_NOTE in buf.getvalue(), True)

    print("\n%s" % ("self-test passed" if ok else "self-test FAILED"))
    return 0 if ok else 1


def main() -> int:
    ap = argparse.ArgumentParser(description="manufacture a content-free history")
    ap.add_argument("mode", nargs="?", choices=["measure", "emit"], default=None)
    ap.add_argument("--n", type=int, default=100000)
    ap.add_argument("--base", default="0" * 40, help="parent sha for the first commit")
    ap.add_argument("--end", type=int, default=None, help="epoch of the LAST commit")
    ap.add_argument("--out", default=None)
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()

    if args.self_test:
        return self_test()
    if args.mode is None:
        ap.print_help()
        return 2

    if args.mode == "emit":
        end = args.end or int(time.time())
        if args.out:
            with open(args.out, "wb") as fh:
                emit(args.n, args.base, end, fh)
            print("wrote %s (%d commits)" % (args.out, args.n), file=sys.stderr)
        else:
            emit(args.n, args.base, end, sys.stdout.buffer)
        return 0

    res = measure(args.n)
    print("\n=== %s 个零内容提交 ===" % "{:,}".format(res["commits"]))
    print("  打包后            %s B  (%.2f MiB)"
          % ("{:,}".format(res["size_pack_bytes"]),
             res["size_pack_bytes"] / 2**20))
    print("  每个提交          %.2f B" % res["bytes_per_commit"])
    print("  生成 / 导入 / 打包  %.1fs / %.1fs / %.1fs"
          % (res["generate_seconds"], res["import_seconds"], res["repack_seconds"]))
    print("\n  对照 Linux 内核（%s 个提交，6,068 MiB）："
          % "{:,}".format(res["kernel_commits"]))
    print("    若照此法买同样多的提交，需 %s B (%.1f MiB)"
          % ("{:,}".format(res["cost_to_match_kernel"]),
             res["cost_to_match_kernel"] / 2**20))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
