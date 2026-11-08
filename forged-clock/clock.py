#!/usr/bin/env python3
"""clock.py -- forensic analysis of a git history's timestamps.

Git commit dates are supplied by the client.  Nothing verifies them.
`GIT_AUTHOR_DATE` / `GIT_COMMITTER_DATE` accept any value, and so does the
Git Data API's `author.date` field.  Every timestamp-derived surface a forge
like GitHub shows you -- the contribution graph, the punch card, "activity
over time", the age of a file -- is built on that unchecked field.

This tool reads a repository's commit graph and reports what the timestamps
actually say.  It is read-only and needs no blobs:

    python clock.py collect [--repo owner/name] [--dir scratch]
    python clock.py analyze [--log scratch/commits.psv]

`collect` does a `--filter=tree:0 --bare` clone: commits only, no trees, no
blobs.  Measured on the subject repo: 153,426 commits in 49 MB / 42 s.
`--filter=blob:none` would not do -- that still fetches every tree, and this
repo has 576k files.
"""

from __future__ import annotations

import argparse
import collections
import json
import re
import statistics
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO = "KrisTHL181/Break-This-Repo"
GIT_BIRTH = datetime(2005, 4, 3, tzinfo=timezone.utc)   # first git commit, ever
UNIX_EPOCH = datetime(1970, 1, 1, tzinfo=timezone.utc)
US = "\x1f"
RE_ISO = re.compile(r"^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2}) ([+-])(\d{2})(\d{2})$")


# ---------------------------------------------------------------- collect
def run(cmd, cwd=None):
    p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if p.returncode != 0:
        raise RuntimeError("%s\n%s" % (" ".join(cmd[:4]), p.stderr[:400]))
    return p.stdout


def collect(repo: str, scratch: Path) -> Path:
    scratch.mkdir(parents=True, exist_ok=True)
    git_dir = scratch / "graph.git"
    # --filter=tree:0 is the whole trick: commits and nothing else.  A
    # blob:none clone still downloads every tree, which for a 576k-file
    # repo is most of the cost we are trying to avoid.
    if not (git_dir / "HEAD").exists():
        run(["git", "clone", "--bare", "--filter=tree:0", "--single-branch",
             "--branch", "main", "https://github.com/%s.git" % repo, str(git_dir)],
            cwd=str(scratch))
    total = run(["git", "rev-list", "--count", "main"], cwd=str(git_dir)).strip()
    first = run(["git", "rev-list", "--count", "--first-parent", "main"],
                cwd=str(git_dir)).strip()
    head = run(["git", "rev-parse", "main"], cwd=str(git_dir)).strip()
    print("%s  head=%s  reachable=%s  first-parent=%s"
          % (repo, head[:12], total, first))

    fmt = US.join(["%H", "%at", "%ct", "%ai", "%ci", "%an", "%ae", "%cn", "%ce"])
    log = run(["git", "log", "--format=" + fmt, "main"], cwd=str(git_dir))
    (scratch / "commits.psv").write_text(log, "utf-8")

    fp = run(["git", "rev-list", "--first-parent", "main"], cwd=str(git_dir))
    (scratch / "firstparent.txt").write_text(fp, "utf-8")

    (scratch / "collect-meta.json").write_text(json.dumps({
        "repo": repo, "head": head,
        "reachable_commits": int(total), "first_parent_commits": int(first),
        "collected_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "method": "git clone --bare --filter=tree:0 --single-branch --branch main "
                  "(commits only; no trees, no blobs)",
    }, ensure_ascii=False, indent=2), "utf-8")

    size = sum(f.stat().st_size for f in git_dir.rglob("*") if f.is_file())
    print("graph on disk: %.1f MB   log: %.1f MB"
          % (size / 1e6, (scratch / "commits.psv").stat().st_size / 1e6))
    return scratch


# ---------------------------------------------------------------- parse
def parse_log(path: Path):
    rows = []
    for line in path.read_text("utf-8").splitlines():
        if not line.strip():
            continue
        f = line.split(US)
        if len(f) != 9:
            continue
        rows.append({
            "sha": f[0], "at": int(f[1]), "ct": int(f[2]),
            "ai": f[3], "ci": f[4],
            "an": f[5], "ae": f[6], "cn": f[7], "ce": f[8],
        })
    return rows


def offset_minutes(iso: str):
    """'+0800' -> 480.  Returns None if the date is not in the expected form."""
    m = RE_ISO.match(iso)
    if not m:
        return None
    sign = 1 if m.group(7) == "+" else -1
    return sign * (int(m.group(8)) * 60 + int(m.group(9)))


def local_hour(iso: str):
    m = RE_ISO.match(iso)
    return int(m.group(4)) if m else None


def year_of(iso: str):
    m = RE_ISO.match(iso)
    return int(m.group(1)) if m else None


# ---------------------------------------------------------------- analyze
def iso(ts: int) -> str:
    return datetime.fromtimestamp(ts, timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def hist(values, key):
    return dict(sorted(collections.Counter(key(v) for v in values).items()))


def analyze(scratch: Path) -> dict:
    rows = parse_log(scratch / "commits.psv")
    meta = json.loads((scratch / "collect-meta.json").read_text("utf-8"))
    fp = set((scratch / "firstparent.txt").read_text("utf-8").split())
    fpr = [r for r in rows if r["sha"] in fp]

    def block(rs):
        at = [r["at"] for r in rs]
        ai = [r["ai"] for r in rs]
        offs = collections.Counter(offset_minutes(r["ai"]) for r in rs)
        return {
            "commits": len(rs),
            "earliest_author_date": iso(min(at)),
            "latest_author_date": iso(max(at)),
            "span_years": round((max(at) - min(at)) / (365.2425 * 86400), 2),
            "years": hist(rs, lambda r: year_of(r["ai"])),
            "hours_in_author_local_time": hist(rs, lambda r: local_hour(r["ai"])),
            "hours_in_utc": hist(rs, lambda r: datetime.fromtimestamp(
                r["at"], timezone.utc).hour),
            "utc_offsets": {str(k): v for k, v in sorted(offs.items())},
        }

    author = block(rows)
    committer = {
        "earliest": iso(min(r["ct"] for r in rows)),
        "latest": iso(max(r["ct"] for r in rows)),
        "utc_offsets": {str(k): v for k, v in sorted(
            collections.Counter(offset_minutes(r["ci"]) for r in rows).items())},
    }

    # a commit whose author and committer timestamps disagree by a lot has
    # been rewritten, cherry-picked or rebased somewhere along the way
    deltas = [r["ct"] - r["at"] for r in rows]
    nonzero = [d for d in deltas if d != 0]

    pre_git = [r for r in rows if r["at"] < GIT_BIRTH.timestamp()]
    pre_epoch = [r for r in rows if r["at"] < UNIX_EPOCH.timestamp()]
    future = [r for r in rows if r["at"] > time.time()]

    by_author = collections.defaultdict(
        lambda: {"commits": 0, "lo": 1 << 62, "hi": 0, "offsets": collections.Counter()})
    for r in rows:
        a = by_author[r["an"]]
        a["commits"] += 1
        a["lo"] = min(a["lo"], r["at"])
        a["hi"] = max(a["hi"], r["at"])
        a["offsets"][offset_minutes(r["ai"])] += 1

    top = sorted(by_author.items(), key=lambda kv: -kv[1]["commits"])[:25]
    top_authors = [{
        "name": n, "commits": v["commits"],
        "first": iso(v["lo"]), "last": iso(v["hi"]),
        "span_years": round((v["hi"] - v["lo"]) / (365.2425 * 86400), 2),
        "modal_offset_min": v["offsets"].most_common(1)[0][0],
    } for n, v in top]

    # authors whose own commits span more than the repo has existed
    immortal = sorted(
        ({"name": n, "commits": v["commits"], "first": iso(v["lo"]),
          "last": iso(v["hi"]),
          "span_years": round((v["hi"] - v["lo"]) / (365.2425 * 86400), 2)}
         for n, v in by_author.items()
         if (v["hi"] - v["lo"]) > 365 * 86400 * 60),
        key=lambda d: -d["span_years"])[:15]

    # occupancy: how many of the 168 weekday/hour slots in GitHub's punch
    # card actually carry at least one commit, and how flat is it
    pc_min = min(author["hours_in_utc"].values())
    pc_max = max(author["hours_in_utc"].values())

    return {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "subject": meta,
        "graph": {
            "reachable_commits": meta["reachable_commits"],
            "first_parent_commits": meta["first_parent_commits"],
            "imported_commits": meta["reachable_commits"] - meta["first_parent_commits"],
            "distinct_author_names": len(by_author),
            "distinct_author_emails": len(set(r["ae"] for r in rows)),
            "distinct_committer_emails": len(set(r["ce"] for r in rows)),
            "import_ratio": round(
                (meta["reachable_commits"] - meta["first_parent_commits"])
                / meta["reachable_commits"], 4),
        },
        "author_dates": author,
        "committer_dates": committer,
        "author_minus_committer": {
            "identical": len(deltas) - len(nonzero),
            "differing": len(nonzero),
            "median_abs_seconds": (int(statistics.median(abs(d) for d in nonzero))
                                   if nonzero else 0),
            "max_abs_seconds": max((abs(d) for d in nonzero), default=0),
        },
        "anachronisms": {
            "before_git_existed_2005_04_03": len(pre_git),
            "before_unix_epoch_1970": len(pre_epoch),
            "in_the_future": len(future),
            "earliest_examples": sorted(
                ({"sha": r["sha"][:12], "date": r["ai"], "author": r["an"],
                  "subject": "(see commit)"} for r in
                 sorted(pre_git, key=lambda r: r["at"])[:10]),
                key=lambda d: d["date"]),
        },
        "first_parent_only": block(fpr),
        "top_authors": top_authors,
        "widest_personal_spans": immortal,
        "punch_card_flatness": {
            "hour_slots_in_utc": 24,
            "min_commits_per_hour": pc_min,
            "max_commits_per_hour": pc_max,
            "zero_hours": [h for h, n in author["hours_in_utc"].items() if n == 0],
        },
    }


# ---------------------------------------------------------------- attribute
def attribute(scratch: Path) -> dict:
    """Which first-parent step introduced which commits.

    Do NOT read `%P` out of `git log --first-parent`: history simplification
    rewrites the parent list, and on this repo that shortcut under-attributed
    ~127,000 commits.  Read the raw DAG with `rev-list --parents` instead.

    Label every commit x with j(x) = max{ i : x is an ancestor-or-self of the
    i-th first-parent commit c_i }.  reach(c_i) shrinks as i grows and
    reach(c_i) \\ reach(c_{i+1}) is exactly what step i brought in -- so the
    import size at step i is #{x : j(x) = i}.
    """
    git_dir = scratch / "graph.git"
    parents = {}
    for line in run(["git", "rev-list", "--parents", "main"], cwd=str(git_dir)).splitlines():
        f = line.split()
        if f:
            parents[f[0]] = f[1:]

    head = run(["git", "rev-parse", "main"], cwd=str(git_dir)).strip()
    chain, seen, cur = [], set(), head
    while cur and cur not in seen:
        seen.add(cur)
        chain.append(cur)
        ps = parents.get(cur, [])
        cur = ps[0] if ps else ""

    children = collections.defaultdict(list)
    for sha, ps in parents.items():
        for p in ps:
            children[p].append(sha)

    order = run(["git", "rev-list", "--topo-order", "main"], cwd=str(git_dir)).split()
    chain_index = {sha: i for i, sha in enumerate(chain)}

    j = {}
    for sha in order:                       # descendants are emitted first
        best = chain_index.get(sha, -1)
        for ch in children.get(sha, ()):
            v = j.get(ch, -1)
            if v > best:
                best = v
        j[sha] = best
    at = collections.Counter(j.values())

    meta = {}
    raw = run(["git", "log", "--no-walk", "--format=%H" + US + "%ai" + US + "%an" + US + "%s",
               *chain], cwd=str(git_dir))
    for line in raw.splitlines():
        f = line.split(US)
        if len(f) == 4:
            meta[f[0]] = {"date": f[1], "author": f[2], "subject": f[3]}

    rows = []
    for i, sha in enumerate(chain):
        ps = parents.get(sha, [])
        m = meta.get(sha, {})
        rows.append({"step": i, "sha": sha, "is_merge": len(ps) > 1,
                     "second_parent": ps[1] if len(ps) > 1 else None,
                     "imported": at.get(i, 0),
                     "date": m.get("date"), "author": m.get("author"),
                     "subject": m.get("subject")})

    importers = sorted(rows, key=lambda r: -r["imported"])
    by_author = collections.Counter()
    for r in rows:
        if r["imported"]:
            subj = r["subject"] or ""
            who = subj.split(" from ")[-1].split("/")[0] if " from " in subj else (r["author"] or "")
            by_author[who] += r["imported"]

    return {
        "head": head,
        "reachable_commits": len(parents),
        "first_parent_chain": len(chain),
        "merges": sum(1 for r in rows if r["is_merge"]),
        "imported_total": len(parents) - len(chain),
        "top_importers": importers[:60],
        "imports_by_pr_branch_owner": by_author.most_common(20),
        "zero_import_merges": sum(1 for r in rows if r["is_merge"] and not r["imported"]),
        # the full strip, step order = newest first; the renderer needs it
        "chain_imported": [r["imported"] for r in rows],
        "chain_date": [(r["date"] or "")[:19] for r in rows],
        "chain_subject": [(r["subject"] or "") for r in rows],
        "chain_is_merge": [r["is_merge"] for r in rows],
        "chain_size_histogram": dict(sorted(
            collections.Counter(min(r["imported"], 100) for r in rows).items())),
        "note": "step 0 is HEAD; higher steps are older. imported = commits this "
                "step introduced that no earlier step had.",
    }


# ---------------------------------------------------------------- report
def summarise(res: dict) -> None:
    g, a, fp = res["graph"], res["author_dates"], res["first_parent_only"]
    an = res["anachronisms"]
    print("\n=== 提交图 ===")
    print("  可达提交          %s" % "{:,}".format(g["reachable_commits"]))
    print("  首父链            %s" % "{:,}".format(g["first_parent_commits"]))
    print("  随 merge 灌入     %s  (%.1f%%)" % (
        "{:,}".format(g["imported_commits"]), 100 * g["import_ratio"]))
    print("  不同作者名        %s" % "{:,}".format(g["distinct_author_names"]))
    print("  不同作者邮箱      %s" % "{:,}".format(g["distinct_author_emails"]))

    print("\n=== 作者日期（全量）===")
    print("  最早 %s" % a["earliest_author_date"])
    print("  最晚 %s" % a["latest_author_date"])
    print("  跨度 %.2f 年" % a["span_years"])
    yrs = a["years"]
    print("  年份分布: %s" % ", ".join(
        "%s=%s" % (y, "{:,}".format(n)) for y, n in sorted(yrs.items())))

    print("\n=== 首父链（真正的线性历史）===")
    print("  提交 %s   最早 %s   最晚 %s   跨度 %.2f 年"
          % ("{:,}".format(fp["commits"]), fp["earliest_author_date"][:10],
             fp["latest_author_date"][:10], fp["span_years"]))

    print("\n=== 时代错误 ===")
    print("  git 诞生(2005-04-03)之前: %s" % "{:,}".format(
        an["before_git_existed_2005_04_03"]))
    print("  Unix epoch(1970-01-01)之前: %s" % "{:,}".format(an["before_unix_epoch_1970"]))
    print("  未来: %s" % "{:,}".format(an["in_the_future"]))
    for e in an["earliest_examples"][:5]:
        print("    %s  %s  %s" % (e["sha"], e["date"], e["author"]))

    print("\n=== 昼夜 ===")
    hu = a["hours_in_utc"]
    print("  UTC 小时直方图: %s" % " ".join(
        "%s" % ("%2d" % hu.get(h, 0)) for h in range(24)))
    print("  零提交的小时: %s" % (res["punch_card_flatness"]["zero_hours"] or "无"))

    print("\n=== 时区偏移（作者）===")
    total_off = sum(v for k, v in a["utc_offsets"].items() if k != "None")
    offs = sorted(((int(k), v) for k, v in a["utc_offsets"].items()
                   if k != "None"), key=lambda kv: -kv[1])[:8]
    for k, v in offs:
        print("  %+05d  %8s  %5.1f%%" % (k, "{:,}".format(v),
                                         100 * v / max(total_off, 1)))
    print("  （共 %d 种偏移，合计 %s 个提交）"
          % (len([k for k in a["utc_offsets"] if k != "None"]),
             "{:,}".format(total_off)))

    print("\n=== 提交最多的作者 ===")
    for t in res["top_authors"][:12]:
        print("  %-30s %8s  %s .. %s  (%.1f 年)"
              % (t["name"][:30], "{:,}".format(t["commits"]),
                 t["first"][:10], t["last"][:10], t["span_years"]))

    print("\n=== 跨度最离谱的作者 ===")
    for t in res["widest_personal_spans"][:8]:
        print("  %-30s %8s  %s .. %s  (%.1f 年)"
              % (t["name"][:30], "{:,}".format(t["commits"]),
                 t["first"][:10], t["last"][:10], t["span_years"]))


def rows_denominator(res):
    return [None] * res["author_dates"]["commits"]


# ---------------------------------------------------------------- self-test
def self_test() -> int:
    ok = True

    def check(label, got, want):
        nonlocal ok
        if got != want:
            ok = False
            print("FAIL %-40s got %r want %r" % (label, got, want))
        else:
            print("ok   %-40s %r" % (label, got))

    check("offset +0800", offset_minutes("2026-09-22 19:51:39 +0800"), 480)
    check("offset -0430", offset_minutes("2026-09-22 19:51:39 -0430"), -270)
    check("offset +0000", offset_minutes("2026-09-22 19:51:39 +0000"), 0)
    check("offset garbage", offset_minutes("not a date"), None)
    check("local hour", local_hour("2026-09-22 19:51:39 +0800"), 19)
    check("year", year_of("1971-09-01 01:30:01 +0000"), 1971)

    tmp = Path(__file__).resolve().parent / "_selftest.psv"
    tmp.write_text(US.join(["a" * 40, "0", "0", "1970-01-01 00:00:00 +0000",
                            "1970-01-01 00:00:00 +0000", "Ann", "a@x",
                            "Com", "c@x"]) + "\n", "utf-8")
    r = parse_log(tmp)
    check("parse row count", len(r), 1)
    check("parse sha", r[0]["sha"], "a" * 40)
    check("parse author", r[0]["an"], "Ann")
    tmp.unlink()

    check("git birth is 2005", GIT_BIRTH.year, 2005)

    print("\n%s" % ("self-test passed" if ok else "self-test FAILED"))
    return 0 if ok else 1


def summarise_attr(res: dict) -> None:
    print("\n=== 首父链归因 ===")
    print("  chain steps %d   merges %d" % (res["first_parent_chain"], res["merges"]))
    print("  imported total %s  (= reachable %s - chain %d)"
          % ("{:,}".format(res["imported_total"]),
             "{:,}".format(res["reachable_commits"]), res["first_parent_chain"]))
    print("\n%s" % "  带入提交最多的 merge")
    print("  %-6s %-9s %-20s %s" % ("step", "imported", "date", "subject"))
    for r in res["top_importers"][:14]:
        if not r["imported"]:
            break
        print("  %-6d %-9s %-20s %.54s"
              % (r["step"], "{:,}".format(r["imported"]),
                 (r["date"] or "")[:19], r["subject"] or ""))
    print("\n%s" % "  按 PR 分支所属人汇总（从 merge 标题里解析 'from X/branch'）")
    for who, n in res["imports_by_pr_branch_owner"][:10]:
        print("  %-24s %10s  %5.1f%%" % (who[:24], "{:,}".format(n),
                                         100 * n / max(res["reachable_commits"], 1)))


def main() -> int:
    ap = argparse.ArgumentParser(description="git timestamp forensics")
    ap.add_argument("mode", nargs="?", choices=["collect", "analyze", "attribute"],
                    default=None)
    ap.add_argument("--repo", default=REPO)
    ap.add_argument("--dir", default=str(Path(__file__).resolve().parent / "_graph"))
    ap.add_argument("--log", default=None)
    ap.add_argument("--out", default=None)
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()

    if args.self_test:
        return self_test()
    if args.mode is None:
        ap.print_help()
        return 2

    here = Path(__file__).resolve().parent
    # resolve: collect() runs git with cwd=<dir>, so a relative --dir would
    # nest one level deeper ("./_graph" + cwd ./_graph -> ./_graph/_graph)
    scratch = Path(args.dir).resolve()
    if args.mode == "collect":
        collect(args.repo, scratch)
        return 0

    if args.mode == "attribute":
        res = attribute(scratch)
        out = Path(args.out) if args.out else here / "attribution.json"
        out.write_text(json.dumps(res, ensure_ascii=False, indent=2), "utf-8")
        summarise_attr(res)
        print("\n-> %s" % out)
        return 0

    if args.log:
        scratch = Path(args.log).parent
        (scratch / "commits.psv").write_text(Path(args.log).read_text("utf-8"), "utf-8")
    res = analyze(scratch)
    out = Path(args.out) if args.out else here / "timestamps.json"
    out.write_text(json.dumps(res, ensure_ascii=False, indent=2), "utf-8")
    summarise(res)
    print("\n-> %s" % out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
