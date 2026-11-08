#!/usr/bin/env python3
"""weigh.py -- weigh a GitHub repository without cloning it.

Why this exists
---------------
A repository can grow until cloning it stops being practical.  This one has
(see WEIGHT.md).  Every survey tool that assumes a local checkout is then
useless: the cheap ones need the working tree, the good ones need the objects.
So this tool asks GitHub instead.

What it measures
----------------
The size of the *working tree* at a given commit -- the bytes you would get on
disk if you checked that commit out -- by walking the Git tree objects through
the REST API.  That number is different from, and much smaller than, the size
of the repository on disk (~16 GB here), because a clone also carries history
this tool never touches.

Design notes
------------
* Exact, not sampled.  A recursive tree listing is truncated by GitHub at
  100,000 entries or 7 MB.  When that happens we do not guess: we re-fetch the
  tree one level down and repeat.  Every reported total is the sum of real
  entries, and the one case that cannot be repaired -- a single directory whose
  own direct listing is truncated -- is reported as inexact rather than
  quietly shown as a smaller number.
* Breath-first with a thread pool.  A cold walk of a repository this size is
  hundreds of round trips; done one at a time it is an afternoon, done eight at
  a time it is minutes.  BFS rather than recursive parallelism so a worker is
  never blocked on a task it queued itself.
* Aggregates are cached per *subtree SHA*, so walking history is cheap.  Git
  trees form a content-addressed DAG: an unchanged directory has an unchanged
  SHA, so a second measurement re-downloads only the directories that actually
  changed.  This is what makes a 30-point time series affordable.
* Read-only.  Two endpoints are used, `git/trees` and `commits`.  Nothing is
  written to the repository being weighed, and no file contents are fetched.
* Stdlib only.  Transport is either the `gh` CLI (uses whatever auth you
  already have) or plain urllib with a token.

Usage
-----
    python3 weigh.py owner/repo
    python3 weigh.py owner/repo --ref v1.2.3 --top 20
    python3 weigh.py owner/repo --series 24 --json weight.json
    python3 weigh.py owner/repo --milestone before=abc123 --milestone after=def456
    GITHUB_TOKEN=ghp_xxx python3 weigh.py owner/repo --via http
    python3 weigh.py --self-test

Only public data is read; no token is required for a public repository, though
unauthenticated callers get 60 requests/hour instead of 5,000.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.github.com"
UA = "repo-scale/1.0 (+https://github.com)"
CACHE_VERSION = 1

MIB = 1024 * 1024
GIB = 1024 * MIB


# --------------------------------------------------------------------------
# errors
# --------------------------------------------------------------------------

class WeighError(Exception):
    """Anything that should stop the run with a readable message."""


class NotFound(WeighError):
    pass


# --------------------------------------------------------------------------
# transport
# --------------------------------------------------------------------------

class Transport:
    """Fetches JSON from the GitHub REST API and counts what it spends."""

    def __init__(self, owner: str, repo: str, via: str = "auto",
                 token: str | None = None, max_calls: int = 20000,
                 quiet: bool = False):
        self.owner = owner
        self.repo = repo
        self.calls = 0
        self.bytes_in = 0
        self.cache_hits = 0
        self.max_calls = max_calls
        self.quiet = quiet
        self._json_cache: dict[str, dict] = {}
        self._lock = threading.Lock()
        self._url_locks: dict[str, threading.Lock] = {}
        self.via = self._pick(via, token)
        self.token = token or os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")

        if self.via == "gh":
            self._gh_bin = shutil.which("gh")
            if not self._gh_bin:
                raise WeighError("--via gh requested but the gh CLI was not found on PATH")

    @staticmethod
    def _pick(via: str, token: str | None) -> str:
        if via != "auto":
            return via
        if token or os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN"):
            return "http"
        if shutil.which("gh"):
            # gh already holds a token the user consented to; prefer it over
            # burning the 60/hour anonymous quota.
            return "gh"
        return "http"

    def get(self, path: str, url: str | None = None) -> dict:
        """Fetch JSON, memoised and safe to call from several threads.

        The weigher walks wide trees with a thread pool, so two workers can ask
        for the same tree at the same instant.  A per-URL lock keeps that from
        becoming two identical downloads.
        """
        target = url or f"{API}{path}"
        with self._lock:
            if target in self._json_cache:
                self.cache_hits += 1
                return self._json_cache[target]
            gate = self._url_locks.setdefault(target, threading.Lock())

        with gate:
            with self._lock:
                if target in self._json_cache:
                    self.cache_hits += 1
                    return self._json_cache[target]
                if self.calls >= self.max_calls:
                    raise WeighError(
                        f"stopped at --max-calls={self.max_calls}; raise it if you mean it")
                self.calls += 1
            if self.via == "gh":
                data = self._get_gh(path)
            else:
                data = self._get_http(target)
            with self._lock:
                self._json_cache[target] = data
            return data

    def _get_gh(self, path: str) -> dict:
        """Shell out to `gh api`, retrying the failures that are worth retrying.

        A recursive listing for a huge tree is a ~7 MB response, and the GnuTLS
        stack behind `gh` does occasionally drop those mid-body ("an existing
        connection was forcibly closed").  One retry costs a second; giving up
        costs the whole walk.
        """
        transient = ("forcibly closed", "connection reset", "unexpected EOF",
                     "i/o timeout", "TLS handshake", "EOF", "502", "503", "504")
        last = ""
        for attempt in range(4):
            proc = subprocess.run(
                [self._gh_bin, "api", "-H", "Accept: application/vnd.github+json", path],
                capture_output=True, text=True, encoding="utf-8", errors="replace")
            if proc.returncode == 0:
                self.bytes_in += len(proc.stdout)
                return json.loads(proc.stdout)
            last = (proc.stderr or "").strip()
            head = last.splitlines()[0] if last else "unknown error"
            if "404" in last or "Not Found" in last:
                raise NotFound(head)
            if any(marker.lower() in last.lower() for marker in transient):
                time.sleep(min(20, 1.5 * (attempt + 1) ** 2))
                continue
            raise WeighError(f"gh api {path} failed: {head[:300]}")
        raise WeighError(f"gh api {path} failed after 4 attempts: {last[:300]}")

    def _get_http(self, url: str) -> dict:
        headers = {"User-Agent": UA, "Accept": "application/vnd.github+json",
                   "X-GitHub-Api-Version": "2022-11-28"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        for attempt in range(4):
            req = urllib.request.Request(url, headers=headers)
            try:
                with urllib.request.urlopen(req, timeout=90) as resp:
                    raw = resp.read()
                self.bytes_in += len(raw)
                return json.loads(raw)
            except urllib.error.HTTPError as exc:
                body = exc.read()[:400].decode("utf-8", "replace")
                if exc.code == 404:
                    raise NotFound(f"404 {url}") from exc
                if exc.code in (403, 429):
                    # Secondary rate limits ask for a pause rather than a
                    # failure; primary ones tell us when the hour resets.
                    reset = exc.headers.get("X-RateLimit-Reset")
                    remaining = exc.headers.get("X-RateLimit-Remaining")
                    if remaining == "0" and reset:
                        wait = max(1, int(reset) - int(time.time()))
                        raise WeighError(
                            f"rate limit exhausted; resets in {wait}s. "
                            f"Use --via gh or set GITHUB_TOKEN.") from exc
                    time.sleep(min(30, 3 * (attempt + 1)))
                    continue
                raise WeighError(f"HTTP {exc.code} for {url}: {body}") from exc
            except urllib.error.URLError as exc:
                time.sleep(min(20, 2 * (attempt + 1)))
                if attempt == 3:
                    raise WeighError(f"network error for {url}: {exc}") from exc
        raise WeighError(f"giving up on {url}")

    # -- convenience ------------------------------------------------------

    def repo_json(self) -> dict:
        return self.get(f"/repos/{self.owner}/{self.repo}")

    def commit(self, ref: str) -> dict:
        """Accept a branch, tag, or SHA and return the commit object."""
        return self.get(f"/repos/{self.owner}/{self.repo}/commits/{urllib.parse.quote(ref)}")

    def tree(self, sha: str, recursive: bool = True) -> dict:
        suffix = "?recursive=1" if recursive else ""
        return self.get(f"/repos/{self.owner}/{self.repo}/git/trees/{sha}{suffix}")

    def commit_list(self, ref: str, pages: int) -> list[dict]:
        out: list[dict] = []
        for page in range(1, pages + 1):
            batch = self.get(
                f"/repos/{self.owner}/{self.repo}/commits"
                f"?sha={urllib.parse.quote(ref)}&per_page=100&page={page}")
            if not isinstance(batch, list) or not batch:
                break
            out.extend(batch)
            if len(batch) < 100:
                break
        return out


# --------------------------------------------------------------------------
# cache
# --------------------------------------------------------------------------

class TreeCache:
    """Maps a tree SHA to the exact aggregate of everything beneath it.

    Keys:
        t:<sha> -> {"b": bytes, "f": files, "d": dirs, "l": symlinks,
                    "s": submodules, "ok": 1 if the aggregate is complete,
                    "trunc": 1 if the recursive listing for this tree is known
                    to be truncated}

    Both flags earn their keep:

    * `ok` marks a finished aggregate -- whether it came from one recursive
      listing or from many (see `measure_tree`).  Without it a split tree would
      be split again on every visit, which is the expensive path.
    * `trunc` remembers that the recursive listing for this tree is doomed.
      Without it we would re-request a 7 MB response that GitHub truncates
      anyway, once per sample, forever -- that is exactly the case for the root
      of a repository this size.
    """

    def __init__(self, path: str | None, autosave_every: int = 250):
        self.path = path
        self.data: dict[str, dict] = {}
        self.dirty = False
        self.enabled = path is not None
        self.autosave_every = autosave_every
        self._since_save = 0
        self._lock = threading.RLock()
        if path and os.path.exists(path):
            try:
                with open(path, encoding="utf-8") as fh:
                    blob = json.load(fh)
            except (OSError, ValueError):
                # A corrupt cache is a cache miss, not a failure: the walk can
                # always be redone, it just costs API calls.
                blob = None
            if isinstance(blob, dict) and blob.get("v") == CACHE_VERSION:
                self.data = blob.get("trees", {})

    def get(self, sha: str) -> dict | None:
        with self._lock:
            return self.data.get("t:" + sha)

    def put(self, sha: str, agg: dict) -> None:
        with self._lock:
            node = self.data.get("t:" + sha)
            if node and node.get("trunc") and not agg.get("trunc"):
                # Replacing the record would forget that a recursive listing
                # for this tree truncates, and we would pay for the doomed
                # request again on the next run.
                agg = dict(agg, trunc=1)
            self.data["t:" + sha] = agg
            self.dirty = True
            self._since_save += 1
            due = self.enabled and self._since_save >= self.autosave_every
        if due:
            # A cold walk of a large repository is minutes of API calls.  Losing
            # all of them to one Ctrl-C is a bad trade, so checkpoint as we go.
            self.save()

    def known_truncating(self, sha: str) -> bool:
        with self._lock:
            node = self.data.get("t:" + sha)
            return bool(node and node.get("trunc"))

    def mark_truncating(self, sha: str) -> None:
        with self._lock:
            node = self.data.setdefault("t:" + sha, {})
            if not node.get("trunc"):
                node["trunc"] = 1
                self.dirty = True

    def save(self) -> None:
        with self._lock:
            if not self.enabled or not self.dirty:
                return
            tmp = self.path + ".tmp"
            with open(tmp, "w", encoding="utf-8") as fh:
                json.dump({"v": CACHE_VERSION, "trees": self.data}, fh,
                          separators=(",", ":"))
            os.replace(tmp, self.path)
            self.dirty = False
            self._since_save = 0


def _blank() -> dict:
    return {"b": 0, "f": 0, "d": 0, "l": 0, "s": 0, "ok": 1}


def _accumulate(node: dict, entry: dict) -> None:
    """Fold one tree entry into an aggregate. Blob sizes are real bytes."""
    kind = entry.get("type")
    mode = entry.get("mode", "")
    if kind == "blob":
        if mode == "120000":
            node["l"] += 1
        else:
            node["f"] += 1
            node["b"] += int(entry.get("size") or 0)
    elif kind == "tree":
        node["d"] += 1
    elif kind == "commit":
        # A gitlink: a submodule. Nothing to weigh, but worth reporting.
        node["s"] += 1


def _split_payload(flat: dict) -> dict:
    """Turn a non-recursive listing into direct totals plus child tree SHAs."""
    direct = _blank()
    direct.pop("ok")
    kids: list[str] = []
    for entry in flat.get("tree", []):
        if entry.get("type") == "tree":
            direct["d"] += 1
            kids.append(entry["sha"])
        else:
            _accumulate(direct, entry)
    return {"direct": direct, "kids": kids,
            # A single directory whose own direct listing is truncated is the
            # one case we cannot repair by asking again.  Count it and be loud.
            "inexact": bool(flat.get("truncated"))}


def _blob(prefix: str, entry: dict) -> dict:
    return {"path": prefix + entry["path"],
            "bytes": int(entry.get("size") or 0),
            "mode": entry.get("mode", "")}


# --------------------------------------------------------------------------
# the weigher
# --------------------------------------------------------------------------

class Weigher:
    def __init__(self, owner: str, repo: str, transport: Transport,
                 cache: TreeCache, progress=None, jobs: int = 8):
        self.o = owner
        self.r = repo
        self.t = transport
        self.cache = cache
        self.progress = progress or (lambda *a, **k: None)
        self.jobs = max(1, jobs)
        self.split_trees = 0
        self.inexact_trees = 0

    # -- fetching ---------------------------------------------------------

    def _fetch_one(self, sha: str) -> tuple[str, str, dict]:
        """Resolve one tree into either a leaf aggregate or a split record.

        Returns (sha, kind, payload) with kind in {"leaf", "split"}.

        A recursive listing is one request and answers the question outright --
        unless GitHub truncates it at 100,000 entries or 7 MB, which is the
        normal case for a repository root this size.  Then we ask for the same
        tree one level down and let the caller recurse.  The result is exact
        either way; only the request count differs.
        """
        if self.cache.known_truncating(sha):
            flat = self.t.tree(sha, recursive=False)
            return sha, "split", _split_payload(flat)

        listing = None
        try:
            listing = self.t.tree(sha, recursive=True)
        except NotFound:
            raise
        except WeighError as exc:
            # Big responses fail where small ones do not.  We do not know
            # whether this listing would have truncated, so fall through to the
            # split path: more requests, but the same exact answer.
            self.progress(f"    {sha[:8]}: recursive listing failed "
                          f"({str(exc)[:60]}) -- splitting instead")

        if listing is not None:
            if not listing.get("truncated"):
                agg = _blank()
                for entry in listing.get("tree", []):
                    _accumulate(agg, entry)
                return sha, "leaf", agg
            self.cache.mark_truncating(sha)

        flat = self.t.tree(sha, recursive=False)
        return sha, "split", _split_payload(flat)

    def _fetch_many(self, shas: list[str]) -> list[tuple[str, str, dict]]:
        if len(shas) == 1 or self.jobs == 1:
            return [self._fetch_one(s) for s in shas]
        with concurrent.futures.ThreadPoolExecutor(max_workers=self.jobs) as pool:
            return list(pool.map(self._fetch_one, shas))

    def _fetch_recursive(self, sha: str) -> dict:
        return self.t.tree(sha, recursive=True)

    # -- aggregate walk ---------------------------------------------------

    def measure_tree(self, root: str) -> dict:
        """Exact totals for the tree `root`.

        Breadth-first, one round of parallel requests per level of *unknown*
        trees.  BFS rather than recursion so that the thread pool never has to
        wait on itself -- a worker is never blocked on a task it queued.
        """
        hit = self.cache.get(root)
        if hit and hit.get("ok"):
            return hit

        split: dict[str, dict] = {}
        frontier = [root]
        while frontier:
            need, seen = [], set()
            for sha in frontier:
                if sha in seen or sha in split:
                    continue
                seen.add(sha)
                node = self.cache.get(sha)
                if node and node.get("ok"):
                    continue
                need.append(sha)
            if not need:
                break
            frontier = []
            for sha, kind, payload in self._fetch_many(need):
                if kind == "leaf":
                    self.cache.put(sha, payload)
                else:
                    self.split_trees += 1
                    if payload["inexact"]:
                        # A directory whose own listing is truncated cannot be
                        # measured exactly at all, at any granularity.  Say so
                        # instead of quietly reporting a smaller number.
                        self.inexact_trees += 1
                    split[sha] = payload
                    frontier.extend(payload["kids"])
            self.progress(f"    resolved {len(need):4} trees | {self.split_trees} split "
                          f"| {len(split):4} pending | {len(self.cache.data):,} cached")

        return self._resolve(root, split)

    def _resolve(self, sha: str, split: dict[str, dict]) -> dict:
        """Fold split records bottom-up. Memoised through the cache."""
        node = self.cache.get(sha)
        if node and node.get("ok"):
            return node
        info = split.get(sha)
        if info is None:  # pragma: no cover - would mean the walk was cut short
            raise WeighError(f"tree {sha[:12]} was never resolved")

        agg = dict(info["direct"])
        agg["ok"] = 1
        if info["inexact"]:
            agg["inexact"] = 1
        for kid in info["kids"]:
            child = self._resolve(kid, split)
            for key in ("b", "f", "d", "l", "s"):
                agg[key] += child[key]
            if child.get("inexact"):
                agg["inexact"] = 1
        self.cache.put(sha, agg)
        return agg

    def measure_ref(self, ref: str = "main") -> dict:
        commit = self.t.commit(ref)
        sha = commit["sha"]
        tree_sha = commit["commit"]["tree"]["sha"]
        agg = self.measure_tree(tree_sha)
        return {
            "ref": ref,
            "sha": sha,
            "tree_sha": tree_sha,
            "date": commit["commit"]["committer"]["date"],
            "subject": commit["commit"]["message"].splitlines()[0][:120],
            "bytes": agg["b"],
            "files": agg["f"],
            "dirs": agg["d"],
            "symlinks": agg["l"],
            "submodules": agg["s"],
            "exact": not agg.get("inexact"),
        }

    # -- blob walk (for the heaviest-files table) --------------------------

    def collect_blobs(self, ref: str = "main",
                      max_entries: int = 400000) -> list[dict]:
        """Every tracked file under `ref`, with its real byte size.

        Walks the same way as `measure_tree` but keeps the paths, so it needs a
        prefix per branch.  Listings fetched earlier in the run are served from
        the transport's own cache, so this is nearly free after a measurement.
        """
        commit = self.t.commit(ref)
        blobs: list[dict] = []
        frontier: list[tuple[str, str]] = [(commit["commit"]["tree"]["sha"], "")]
        while frontier and len(blobs) < max_entries:
            batch, frontier = frontier, []
            shas = [sha for sha, _ in batch]
            if len(shas) == 1 or self.jobs == 1:
                listings = [self._fetch_recursive(s) for s in shas]
            else:
                with concurrent.futures.ThreadPoolExecutor(
                        max_workers=self.jobs) as pool:
                    listings = list(pool.map(self._fetch_recursive, shas))

            for (sha, prefix), listing in zip(batch, listings):
                if not listing.get("truncated"):
                    for entry in listing.get("tree", []):
                        if entry.get("type") == "blob":
                            blobs.append(_blob(prefix, entry))
                            if len(blobs) >= max_entries:
                                break
                    continue
                flat = self.t.tree(sha, recursive=False)
                for entry in flat.get("tree", []):
                    if entry.get("type") == "tree":
                        frontier.append((entry["sha"], prefix + entry["path"] + "/"))
                    elif entry.get("type") == "blob":
                        blobs.append(_blob(prefix, entry))
        return blobs

    # -- top-level breakdown ----------------------------------------------

    def breakdown(self, ref: str = "main") -> list[dict]:
        """Weight of each entry in the root of `ref`, heaviest first.

        Nearly free after a measurement: every child tree is already an
        aggregate in the cache, so this costs one small request for the root
        listing itself.  It is also the only view that explains where the bytes
        actually are, which a single total cannot.
        """
        commit = self.t.commit(ref)
        root = commit["commit"]["tree"]["sha"]
        flat = self.t.tree(root, recursive=False)
        if flat.get("truncated"):
            raise WeighError("the repository root itself was truncated; "
                             "a breakdown would be misleading")
        rows = []
        for entry in flat.get("tree", []):
            kind = entry.get("type")
            if kind == "tree":
                agg = self.measure_tree(entry["sha"])
                rows.append({"name": entry["path"] + "/", "kind": "dir",
                             "bytes": agg["b"], "files": agg["f"],
                             "dirs": agg["d"] + 1})
            elif kind == "blob" and entry.get("mode") != LINK:
                rows.append({"name": entry["path"], "kind": "file",
                             "bytes": int(entry.get("size") or 0), "files": 1,
                             "dirs": 0})
        rows.sort(key=lambda r: (-r["bytes"], r["name"]))
        return rows

    # -- time series ------------------------------------------------------

    def first_parent_chain(self, ref: str, limit: int = 300) -> list[dict]:
        """The mainline: follow parents[0] from `ref`. Returns newest first."""
        pool = self.t.commit_list(ref, pages=max(1, (limit + 99) // 100))
        by_sha = {c["sha"]: c for c in pool}
        chain: list[dict] = []
        cur = self.t.commit(ref)
        while cur and len(chain) < limit:
            chain.append(cur)
            parents = cur.get("parents") or []
            if not parents:
                break
            nxt = by_sha.get(parents[0]["sha"])
            if nxt is None:
                nxt = self.t.commit(parents[0]["sha"])
            cur = nxt
        return chain

    def series(self, ref: str = "main", points: int = 24,
               pool: int = 300) -> list[dict]:
        chain = self.first_parent_chain(ref, limit=pool)
        if not chain:
            return []
        if points >= len(chain):
            picked = chain
        else:
            step = (len(chain) - 1) / (points - 1)
            idx = sorted({int(round(i * step)) for i in range(points)})
            picked = [chain[i] for i in idx]
        out = []
        for commit in picked:
            tree_sha = commit["commit"]["tree"]["sha"]
            agg = self.measure_tree(tree_sha)
            out.append({
                "sha": commit["sha"],
                "date": commit["commit"]["committer"]["date"],
                "subject": commit["commit"]["message"].splitlines()[0][:120],
                "bytes": agg["b"], "files": agg["f"], "dirs": agg["d"],
                "symlinks": agg["l"], "submodules": agg["s"],
            })
        out.sort(key=lambda x: x["date"])
        return out

    # -- milestones -------------------------------------------------------

    def milestones(self, specs: list[str]) -> list[dict]:
        """Measure named points in history. `LABEL=REF` or `LABEL:REF`."""
        rows = []
        for spec in specs:
            label, sep, ref = spec.partition("=")
            if not sep:
                label, sep, ref = spec.partition(":")
            if not sep or not ref.strip():
                raise WeighError(f"expected LABEL=REF, got {spec!r}")
            row = self.measure_ref(ref.strip())
            row["label"] = label.strip()
            rows.append(row)
        return rows


# --------------------------------------------------------------------------
# rendering
# --------------------------------------------------------------------------

def human(n: int) -> str:
    if n >= GIB:
        return f"{n / GIB:.2f} GiB"
    if n >= MIB:
        return f"{n / MIB:.1f} MiB"
    if n >= 1024:
        return f"{n / 1024:.1f} KiB"
    return f"{n} B"


def bar(value: int, peak: int, width: int = 44) -> str:
    if peak <= 0:
        return ""
    return "\u2588" * max(1, round(value / peak * width))


def print_report(meta: dict, row: dict, blobs: list[dict] | None,
                 top: int, cache: TreeCache, transport: Transport,
                 splits: int = 0) -> None:
    print(f"repository  {meta['full_name']}")
    print(f"ref         {row['ref']}  ({row['sha'][:12]}, {row['date']})")
    print(f"subject     {row['subject']}")
    print()
    print(f"  working tree   {human(row['bytes'])}   ({row['bytes']:,} bytes)")
    print(f"  files          {row['files']:,}")
    print(f"  directories    {row['dirs']:,}")
    if not row.get("exact", True):
        print("  ⚠ a directory listing was truncated by the API; this total is a")
        print("    lower bound. See the note on inexact trees in this file's header.")
    if row["symlinks"]:
        print(f"  symlinks       {row['symlinks']:,}  (excluded from bytes)")
    if row["submodules"]:
        print(f"  submodules     {row['submodules']:,}  (gitlinks, nothing checked out)")
    verified = meta.get("size")
    if verified:
        print()
        print(f"  GitHub reports the repository on disk as {human(verified * 1024)}.")
        print(f"  That is history. This number is the working tree. A clone carries both.")

    if blobs:
        real = [b for b in blobs if b["mode"] != "120000"]
        real.sort(key=lambda b: -b["bytes"])
        total = sum(b["bytes"] for b in real) or 1
        print()
        print(f"heaviest files (top {top} of {len(real):,})")
        for entry in real[:top]:
            share = entry["bytes"] / total * 100
            print(f"  {human(entry['bytes']):>10}  {share:5.2f}%  {entry['path'][:88]}")
        top_sum = sum(b["bytes"] for b in real[:top])
        shown = len(real[:top])
        print(f"  {human(top_sum):>10}  {top_sum / total * 100:5.2f}%  "
              f"<- these {shown} file{'s' if shown != 1 else ''}")

    print()
    print(f"api calls {transport.calls}  (json-cache hits {transport.cache_hits}, "
          f"subtree splits {splits})")
    if cache.enabled:
        print(f"cache entries {len(cache.data):,} -> {cache.path}")


def print_series(rows: list[dict]) -> None:
    if not rows:
        print("no commits sampled")
        return
    peak = max(r["bytes"] for r in rows)
    first = rows[0]
    print(f"{'date':16} {'tree size':>11}  {'files':>8}  {'delta':>10}  commit")
    print("-" * 100)
    prev = None
    for r in rows:
        delta = "" if prev is None else f"{r['bytes'] - prev:+,}"
        print(f"{r['date'][:16]:16} {human(r['bytes']):>11}  {r['files']:>8,}  "
              f"{delta:>10}  {r['sha'][:8]} {r['subject'][:36]}")
        prev = r["bytes"]
    print()
    print(f"first sample {human(first['bytes'])}   last {human(rows[-1]['bytes'])}   "
          f"net {rows[-1]['bytes'] - first['bytes']:+,} bytes")
    print()
    for r in rows:
        print(f"  {r['date'][:10]}  {human(r['bytes']):>10}  {bar(r['bytes'], peak)}")


def print_breakdown(rows: list[dict], total: int, top: int) -> None:
    peak = rows[0]["bytes"] if rows else 1
    print(f"where the weight is (top {min(top, len(rows))} of {len(rows)} root entries)")
    print(f"{'weight':>11} {'share':>6}  {'files':>9}  name")
    print("-" * 104)
    for r in rows[:top]:
        share = r["bytes"] / total * 100 if total else 0
        print(f"{human(r['bytes']):>11} {share:5.2f}%  {r['files']:>9,}  {r['name'][:72]}")
    rest = rows[top:]
    if rest:
        print(f"{human(sum(r['bytes'] for r in rest)):>11} "
              f"{sum(r['bytes'] for r in rest) / total * 100:5.2f}%  "
              f"{sum(r['files'] for r in rest):>9,}  ({len(rest)} smaller entries)")
    print()
    for r in rows[:top]:
        print(f"  {human(r['bytes']):>10}  {bar(r['bytes'], peak, 40)}  {r['name'][:44]}")


def print_milestones(rows: list[dict], baseline: int | None = None) -> None:
    peak = max(r["bytes"] for r in rows) or 1
    print(f"{'step':10} {'date':16} {'working tree':>13} {'files':>9} {'delta':>12}  ref")
    print("-" * 104)
    prev = baseline
    for r in rows:
        delta = "-" if prev is None else f"{r['bytes'] - prev:+,}"
        print(f"{r['label'][:10]:10} {r['date'][:16]:16} {human(r['bytes']):>13} "
              f"{r['files']:>9,} {delta:>12}  {r['sha'][:8]}")
        prev = r["bytes"]
    print()
    for r in rows:
        print(f"  {r['label'][:16]:16} {human(r['bytes']):>10}  {bar(r['bytes'], peak)}")


# --------------------------------------------------------------------------
# offline self-test
# --------------------------------------------------------------------------

class FakeTransport(Transport):
    """A transport backed by a literal tree graph, for --self-test and tests."""

    def __init__(self, trees: dict, commits: dict, commit_lists: dict | None = None):
        self.owner = "fake"
        self.repo = "fake"
        self.calls = 0
        self.bytes_in = 0
        self.cache_hits = 0
        self.max_calls = 10 ** 9
        self.quiet = True
        self.via = "fake"
        self.token = None
        self._json_cache = {}
        self.trees = trees
        self.commits = commits
        self.commit_lists = commit_lists or {}
        self.requests: list[str] = []

    def tree(self, sha: str, recursive: bool = True) -> dict:
        self.calls += 1
        self.requests.append(f"tree:{sha}:{recursive}")
        if sha not in self.trees:
            raise NotFound(f"no such tree {sha}")
        node = self.trees[sha]
        if not recursive:
            return {"sha": sha, "tree": node["entries"],
                    "truncated": node.get("entries_truncated", False)}
        # Materialise the recursive view if the fixture defines it.
        if "recursive" in node:
            return {"sha": sha, "tree": node["recursive"],
                    "truncated": node.get("truncated", False)}
        return {"sha": sha, "tree": node["entries"], "truncated": False}

    def commit(self, ref: str) -> dict:
        self.calls += 1
        self.requests.append(f"commit:{ref}")
        if ref not in self.commits:
            raise NotFound(f"no such commit {ref}")
        return self.commits[ref]

    def commit_list(self, ref: str, pages: int) -> list[dict]:
        self.calls += 1
        self.requests.append(f"commits:{ref}")
        return self.commit_lists.get(ref, [])


BLOB = "100644"
LINK = "120000"
GITLINK = "160000"


def _self_test() -> int:
    """A tiny tree graph with a truncated root, checked against hand sums."""
    # root -- dir "big" (truncating) -- dir "sub" + 2 blobs ; file "a" 10B ;
    #         dir "small" -- file "b" 5B ; gitlink "vendor"
    trees = {
        "ROOT": {
            "entries": [
                {"path": "big", "type": "tree", "mode": "040000", "sha": "BIG"},
                {"path": "small", "type": "tree", "mode": "040000", "sha": "SMALL"},
                {"path": "a", "type": "blob", "mode": BLOB, "size": 10},
                {"path": "link", "type": "blob", "mode": LINK, "size": 4},
                {"path": "vendor", "type": "commit", "mode": GITLINK, "sha": "V"},
            ],
            "recursive": [{"path": "big", "type": "tree", "mode": "040000", "sha": "BIG"}],
            "truncated": True,
        },
        "BIG": {
            "entries": [
                {"path": "sub", "type": "tree", "mode": "040000", "sha": "SUB"},
                {"path": "p", "type": "blob", "mode": BLOB, "size": 100},
                {"path": "q", "type": "blob", "mode": BLOB, "size": 900},
            ],
            # A real recursive listing carries the subtree entry *and* its
            # contents, so the directory count must not double-count.
            "recursive": [
                {"path": "sub", "type": "tree", "mode": "040000", "sha": "SUB"},
                {"path": "sub/deep.bin", "type": "blob", "mode": BLOB, "size": 50},
                {"path": "p", "type": "blob", "mode": BLOB, "size": 100},
                {"path": "q", "type": "blob", "mode": BLOB, "size": 900},
            ],
            "truncated": False,
        },
        "SUB": {"entries": [{"path": "deep.bin", "type": "blob", "mode": BLOB, "size": 50}]},
        "SMALL": {"entries": [{"path": "b", "type": "blob", "mode": BLOB, "size": 5}]},
    }
    commits = {
        "main": {
            "sha": "C1",
            "commit": {"tree": {"sha": "ROOT"},
                       "message": "synthetic",
                       "committer": {"date": "2026-01-01T00:00:00Z"}},
        }
    }
    failures: list[str] = []

    def check(name, got, want):
        if got != want:
            failures.append(f"{name}: got {got!r}, want {want!r}")

    # 1. exact aggregation across a truncated root
    tr = FakeTransport(trees, commits)
    w = Weigher("fake", "fake", tr, TreeCache(None))
    row = w.measure_ref("main")
    check("bytes", row["bytes"], 10 + 100 + 900 + 50 + 5)
    check("files", row["files"], 5)
    check("dirs", row["dirs"], 3)          # big, sub, small
    check("symlinks", row["symlinks"], 1)  # excluded from bytes
    check("submodules", row["submodules"], 1)
    check("truncated root was split", w.split_trees, 1)

    # 2. the cache is keyed by subtree SHA and short-circuits repeat visits
    tr2 = FakeTransport(trees, commits)
    cache = TreeCache(None)
    w2 = Weigher("fake", "fake", tr2, cache)
    w2.measure_ref("main")
    first_calls = tr2.calls
    w2.measure_ref("main")
    check("second measure of same tree costs 0 extra tree calls",
          tr2.calls - first_calls, 1)  # only the commit lookup

    # 3. a known-truncating root is not re-requested recursively
    check("root is remembered as truncating", cache.known_truncating("ROOT"), True)

    # 4. blob collection flattens nested paths and keeps sizes exact
    tr3 = FakeTransport(trees, commits)
    w3 = Weigher("fake", "fake", tr3, TreeCache(None))
    blobs = w3.collect_blobs("main")
    paths = sorted(b["path"] for b in blobs)
    check("blob paths", paths, ["a", "big/p", "big/q", "big/sub/deep.bin", "link", "small/b"])
    check("blob bytes sum", sum(b["bytes"] for b in blobs), 10 + 100 + 900 + 50 + 5 + 4)

    # 5. peak/bar helpers never divide by zero
    check("bar of empty", bar(0, 0), "")
    check("bar floor", len(bar(1, 10 ** 9, width=44)), 1)

    if failures:
        print("SELF-TEST FAILED")
        for f in failures:
            print("  -", f)
        return 1
    print("self-test OK  (5 groups, exact aggregation + cache + blob walk)")
    return 0


# --------------------------------------------------------------------------
# cli
# --------------------------------------------------------------------------

def split_repo(value: str) -> tuple[str, str]:
    value = value.rstrip("/")
    if value.startswith("http"):
        parts = urllib.parse.urlparse(value).path.strip("/").split("/")
    else:
        parts = value.split("/")
    if len(parts) < 2 or not all(parts[:2]):
        raise WeighError("expected owner/repo")
    return parts[0], parts[1]


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(
        description="Weigh a GitHub repository's working tree without cloning it.")
    ap.add_argument("repo", nargs="?", help="owner/repo (omit with --self-test)")
    ap.add_argument("--ref", default="main", help="branch, tag, or commit (default main)")
    ap.add_argument("--top", type=int, default=15, help="heaviest files to list")
    ap.add_argument("--no-top", action="store_true", help="skip the blob walk")
    ap.add_argument("--by-dir", type=int, default=0, metavar="N",
                    help="also rank the N heaviest entries in the repository root")
    ap.add_argument("--series", type=int, metavar="N",
                    help="sample N points along the first-parent mainline")
    ap.add_argument("--series-pool", type=int, default=300,
                    help="newest commits to consider for --series (default 300)")
    ap.add_argument("--milestone", action="append", default=[], metavar="LABEL=REF",
                    help="measure a named point in history; repeatable. "
                         "LABEL:REF also works, for shells that eat the equals sign")
    ap.add_argument("--json", metavar="FILE", help="write results as JSON")
    ap.add_argument("--cache", metavar="FILE", default=".weigh-cache.json",
                    help="subtree aggregate cache (default .weigh-cache.json)")
    ap.add_argument("--no-cache", action="store_true", help="disable the cache")
    ap.add_argument("--via", choices=["auto", "gh", "http"], default="auto")
    ap.add_argument("--jobs", type=int, default=8,
                    help="parallel tree requests (default 8; 1 for a fixed order)")
    ap.add_argument("--token", help="GitHub token (else $GITHUB_TOKEN)")
    ap.add_argument("--max-calls", type=int, default=20000)
    ap.add_argument("-q", "--quiet", action="store_true")
    ap.add_argument("--self-test", action="store_true",
                    help="run the offline synthetic-tree test and exit")
    args = ap.parse_args(argv)

    if args.self_test:
        return _self_test()
    if not args.repo:
        ap.error("repo is required unless --self-test is given")

    cache = TreeCache(None if args.no_cache else args.cache)
    try:
        owner, name = split_repo(args.repo)
        transport = Transport(owner, name, via=args.via, token=args.token,
                              max_calls=args.max_calls, quiet=args.quiet)

        def progress(msg: str) -> None:
            if not args.quiet:
                print(msg, file=sys.stderr)

        progress(f"transport: {transport.via} | jobs: {args.jobs} | cache: "
                 f"{cache.path or 'off'}")
        weigher = Weigher(owner, name, transport, cache, progress=progress,
                          jobs=args.jobs)
        started = time.time()
        meta = transport.repo_json()
        result: dict = {
            "repository": meta["full_name"],
            "github_size_bytes": (meta.get("size") or 0) * 1024,
            "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "via": transport.via,
        }

        if args.series:
            progress(f"sampling {args.series} points along the mainline of {args.ref} ...")
            rows = weigher.series(args.ref, points=args.series, pool=args.series_pool)
            result["series"] = rows
            print_series(rows)

        if args.milestone:
            ms = weigher.milestones(args.milestone)
            result["milestones"] = ms
            print_milestones(ms)

        row = weigher.measure_ref(args.ref)
        result["head"] = row

        if args.by_dir:
            progress("breaking down the root by directory ...")
            rows = weigher.breakdown(args.ref)
            result["breakdown"] = rows[:args.by_dir]
            print_breakdown(rows, row["bytes"], args.by_dir)
            print()

        blobs = None
        if not args.no_top:
            progress(f"walking blobs under {args.ref} for the heaviest-files table ...")
            blobs = weigher.collect_blobs(args.ref)
            result["heaviest_files"] = sorted(
                (b for b in blobs if b["mode"] != LINK), key=lambda b: -b["bytes"])[:args.top]

        if not args.json:
            print_report(meta, row, blobs, args.top, cache, transport,
                         splits=weigher.split_trees)

        result["stats"] = {
            "api_calls": transport.calls,
            "json_cache_hits": transport.cache_hits,
            "subtree_splits": weigher.split_trees,
            "inexact_trees": weigher.inexact_trees,
            "seconds": round(time.time() - started, 1),
        }
        if args.json:
            with open(args.json, "w", encoding="utf-8") as fh:
                json.dump(result, fh, ensure_ascii=False, indent=1)
            print(f"wrote {args.json}")

        if not args.quiet:
            print(f"\n[{transport.calls} api calls, {weigher.split_trees} splits, "
                  f"{time.time() - started:.1f}s]", file=sys.stderr)
        return 0
    except WeighError as exc:
        print(f"weigh.py: {exc}", file=sys.stderr)
        return 2
    except KeyboardInterrupt:
        print("\nweigh.py: interrupted", file=sys.stderr)
        return 130
    finally:
        # Whatever happened -- error, Ctrl-C -- keep what we already paid for.
        # On a repository this size the walk is minutes of API calls, and the
        # next run is nearly free if this succeeded.
        cache.save()


if __name__ == "__main__":
    sys.exit(main())
