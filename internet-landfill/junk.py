#!/usr/bin/env python3
"""junk.py -- salvage genuine internet junk, then weigh it.

Two phases, kept separate so either can be re-run alone:

    python junk.py --fetch      pull specimens from the live internet
    python junk.py --analyze    measure them, write manifest.json
    python junk.py --all        both, in order

Everything this ships is public filler text, placeholder-API output,
error pages and plumbing files (robots.txt / ads.txt / tracking pixels).
No malware, no credentials, no personal data: fetched text is scrubbed
before it touches disk, and every specimen keeps its source URL so the
claim is checkable.

The measurement is the point.  See README.md.
"""

from __future__ import annotations

import argparse
import bz2
import collections
import gzip
import hashlib
import json
import lzma
import math
import os
import re
import subprocess
import sys
import time
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = Path(__file__).resolve().parent
SPECIMEN_DIR = BASE / "specimens"
CONTROL_DIR = BASE / "controls"
RAW_DIR = BASE / "_raw"
MANIFEST = BASE / "manifest.json"

UA = "Break-This-Repo-junk-collector/1.0 (+one polite request per source)"
CAP = 64 * 1024          # per-specimen byte cap for salvaged material
PAUSE = 0.4              # seconds between requests
TIMEOUT = 40


# --------------------------------------------------------------------------
# the salvage list
# --------------------------------------------------------------------------
# origin: "salvaged" = pulled off the live internet, "synthetic" = produced
# locally as a control.  expect: "live" or "dead" -- a "live" source that
# fails is itself a specimen (link rot), so nothing is ever discarded.

SOURCES = [
    # --- 占位数据 ---------------------------------------------------------
    dict(id="placeholder-post", category="占位数据", ext="json", expect="live",
         url="https://jsonplaceholder.typicode.com/posts/1",
         note="教程里被复制粘贴过几百万次的假帖子。二十年前的教学代码至今仍在生产环境里跑。"),
    dict(id="placeholder-comments", category="占位数据", ext="json", expect="live",
         url="https://jsonplaceholder.typicode.com/comments?postId=1",
         note="假评论。五条，每条都长得像真的，但没有一个字有信息。"),
    dict(id="placeholder-photos", category="占位数据", ext="json", expect="live",
         url="https://jsonplaceholder.typicode.com/photos?_limit=5",
         note="指向 via.placeholder.com 的图片记录。图片本身也是占位符。占位符指向占位符。"),
    dict(id="placeholder-users", category="占位数据", ext="json", expect="live",
         url="https://jsonplaceholder.typicode.com/users/1",
         note="虚构用户 Leanne Graham，住在虚构的 Gwenborough。含假邮箱与假电话。"),
    dict(id="placeholder-todos", category="占位数据", ext="json", expect="live",
         url="https://jsonplaceholder.typicode.com/todos?_limit=20",
         note="二十条待办。没有一条会被做。"),

    # --- 填充文本 ---------------------------------------------------------
    dict(id="filler-lipsum-page", category="填充文本", ext="html", expect="live",
         url="https://www.lipsum.com/feed/html",
         note="lorem ipsum 的发源地页面。西塞罗《论至善与至恶》公元前 45 年的段落，"
              "被 1500 年代的排字工人切碎了当填充料，此后五百年一直在被填。"),
    dict(id="filler-bacon", category="填充文本", ext="txt", expect="live",
         url="https://baconipsum.com/api/?type=meat-and-filler&paras=3",
         note="用肉类词汇替换拉丁文的 lorem ipsum。同一个笑话，换了个行业。"),
    dict(id="filler-asdfast", category="填充文本", ext="txt", expect="live",
         url="https://asdfast.beobit.net/api/?type=paragraph&length=5",
         note="又一个填充文本 API。存在本身就说明需求量有多大。"),
    dict(id="filler-httpbin-moby", category="填充文本", ext="html", expect="live",
         url="https://httpbin.org/html",
         note="httpbin 的 HTML 测试页：《白鲸》第一章。公共领域，所以谁都不心疼，"
              "于是它成了全世界 HTTP 客户端的默认靶子。"),

    # --- 企业黑话 ---------------------------------------------------------
    dict(id="corporate-bullshit", category="企业黑话", ext="json", expect="live",
         url="http://corporatebs-generator.sameerkumar.website/",
         note="企业黑话生成器。它存在的合理性在于：它生成的东西和真实会议纪要无法区分。"
              "整个 API 返回 56 字节。"),

    # --- 无意义数据 -------------------------------------------------------
    dict(id="noise-uuid", category="无意义数据", ext="txt", expect="live",
         url="https://httpbin.org/uuid",
         note="一个 UUID。36 个字符，122 bit 熵，指向一个不存在的东西。"),
    dict(id="noise-catfact", category="无意义数据", ext="json", expect="live",
         url="https://catfact.ninja/fact",
         note="一条猫的事实。世界上最好的一条。"),
    dict(id="noise-httpbin-bytes", category="无意义数据", ext="bin", expect="live",
         url="https://httpbin.org/bytes/1024",
         note="1 KiB 真随机字节。这份收藏里唯一一个没有被压缩算法占过便宜的东西。"),
    dict(id="noise-httpbin-links", category="无意义数据", ext="html", expect="live",
         url="https://httpbin.org/links/50/0",
         note="五十个死链接。页面唯一的内容就是五十个死链接。"),

    # --- 错误页 -----------------------------------------------------------
    dict(id="error-httpbin-404", category="错误页", ext="txt", expect="live",
         url="https://httpbin.org/status/404",
         note="一个 404，内容为零字节。零字节也是内容。"),
    dict(id="error-httpbin-503", category="错误页", ext="txt", expect="live",
         url="https://httpbin.org/status/503",
         note="一个 503，同样零字节。这两种错误在互联网上比所有诗歌加起来都常见。"),
    dict(id="error-httpstat-404", category="错误页", ext="html", expect="live",
         url="https://httpstat.us/404",
         note="另一个 404，这次肯给你一点 HTML。"),

    dict(id="error-google-404", category="错误页", ext="html", expect="live",
         url="https://www.google.com/ads.txt",
         note="Google 没有 ads.txt，于是回了一个 404 页面。1,568 字节的 HTML "
              "只为传达「这里没有你要的东西」这一 bit 的信息。"),
    dict(id="error-apple-404", category="错误页", ext="html", expect="live",
         url="https://www.apple.com/ads.txt",
         note="Apple 也没有 ads.txt，于是回了一个 **111 KB** 的 404 —— 本收藏中"
              "最贵的错误页。里面有大段连续空行、内联样式、追踪像素。"
              "为了说「没有」，它传输了 111,090 字节。"),

    # --- 死链墓园 ---------------------------------------------------------
    dict(id="dead-geocities", category="死链墓园", ext="html", expect="dead",
         url="http://www.geocities.com/",
         note="GeoCities：整个第一代个人网页的集体墓地，1994-2009。现在返回 500。"),
    dict(id="dead-catfact-heroku", category="死链墓园", ext="txt", expect="dead",
         url="https://cat-fact.herokuapp.com/facts",
         note="Heroku 2022 年取消免费额度后被拆掉的 API。它留下的唯一遗物是连接错误。"),
    dict(id="dead-loripsum", category="死链墓园", ext="txt", expect="dead",
         url="https://loripsum.net/api/3/medium/plain",
         note="采集当天从这台机器上连不上。注意：这**不能**证明它已经死了，"
              "只证明从这一个观测点看不见它 —— 见 README 的采集偏差说明。"),

    # --- 管道文件 ---------------------------------------------------------
    dict(id="plumbing-robots-google", category="管道文件", ext="txt", expect="live",
         url="https://www.google.com/robots.txt",
         note="全世界被下载次数最多的文本文件之一。内容是对机器的禁令，而机器是唯一读者。"),
    dict(id="plumbing-robots-bing", category="管道文件", ext="txt", expect="live",
         url="https://www.bing.com/robots.txt",
         note="同一份文件的竞争对手版本。两个搜索引擎各自发一份「不许爬我」的公告。"),
    dict(id="plumbing-ads-bing", category="管道文件", ext="txt", expect="live",
         url="https://www.bing.com/ads.txt",
         note="77 KB 的广告转售授权清单。每一行都是一个不可读的发布商 ID，"
              "没有一行是给人看的，也没有一行会被任何人读完。"),
    dict(id="plumbing-ads-cnn", category="管道文件", ext="txt", expect="live",
         url="https://edition.cnn.com/ads.txt",
         note="同一份协议的新闻机构版本，45 KB。这个文件的存在意义是让广告中间商"
              "相信彼此没有被骗。"),
    dict(id="plumbing-ads-imdb", category="管道文件", ext="txt", expect="live",
         url="https://www.imdb.com/ads.txt",
         note="20 KB。它描述的行为是：当你在看某部电影的资料页时，谁有权在你身上卖广告。"),
    dict(id="plumbing-ads-spotify", category="管道文件", ext="txt", expect="live",
         url="https://www.spotify.com/ads.txt",
         note="1.2 KB。整份清单里最克制的一份，值得表扬。"),
    dict(id="plumbing-sitemap-bing", category="管道文件", ext="xml", expect="live",
         url="https://www.bing.com/sitemap.xml",
         note="一份「站点地图的地图」：它自己不指向任何网页，只指向其它站点地图。"),
    dict(id="plumbing-crossdomain-google", category="管道文件", ext="xml", expect="live",
         url="https://www.google.com/crossdomain.xml",
         note="Flash 时代的僵尸文件。Flash Player 已于 2020-12-31 停止支持、"
              "2021-01-12 起拒绝播放内容，而这份为它写的策略文件在 2026 年仍在正常伺服。"),
    dict(id="plumbing-humans-google", category="管道文件", ext="txt", expect="live",
         url="https://www.google.com/humans.txt",
         note="humans.txt 是「给人类看的 robots.txt」——一个从未被广泛采用的约定。"
              "Google 留下了一句话和一个招聘链接。这是整份收藏里唯一一个似乎想跟你说话的标本。"),

    # --- 样板文本 ---------------------------------------------------------
    dict(id="boilerplate-example-domain", category="样板文本", ext="html", expect="live",
         url="https://example.com/",
         note="IANA 保留域。世界上最著名的一句话是「This domain is for use in "
              "illustrative examples in documents」。它说了什么？什么也没说。"),
]


# --------------------------------------------------------------------------
# scrubbing
# --------------------------------------------------------------------------
RE_EMAIL = re.compile(r"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}")
RE_IPV4 = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
RE_LONGTOKEN = re.compile(r"\b[A-Za-z0-9_\-]{32,}\b")
RE_PHONE = re.compile(r"(?<![\d.])\+?\d{1,3}[\s.\-]\d{2,4}[\s.\-]\d{3,4}(?:[\s.\-]\d{3,4})?(?![\d.])")


def scrub(text: str) -> tuple[str, dict]:
    """Strip anything that looks personal or secret out of salvaged text."""
    counts = {}

    def sub(pattern, label, src):
        out, n = pattern.subn("[%s]" % label, src)
        if n:
            counts[label] = n
        return out

    text = sub(RE_EMAIL, "email-redacted", text)
    text = sub(RE_PHONE, "phone-redacted", text)
    text = sub(RE_IPV4, "ip-redacted", text)
    text = sub(RE_LONGTOKEN, "token-redacted", text)
    return text, counts


# --------------------------------------------------------------------------
# measurement
# --------------------------------------------------------------------------
def shannon(data: bytes) -> float:
    """Zero-order byte entropy in bits/byte.

    This is an *upper bound* on real information content: it only looks at
    byte frequencies and ignores all structure between bytes.  It is used
    here to classify junk (flat vs structured), not to price it.  Pricing
    is done with measured compression ratios, which are evidence rather
    than an estimate.
    """
    if not data:
        return 0.0
    counts = collections.Counter(data)
    n = len(data)
    return -sum((c / n) * math.log2(c / n) for c in counts.values())


def gz(data: bytes) -> int:
    return len(gzip.compress(data, compresslevel=9, mtime=0))


def bz(data: bytes) -> int:
    return len(bz2.compress(data, compresslevel=9))


def xz(data: bytes) -> int:
    return len(lzma.compress(data, preset=9 | lzma.PRESET_EXTREME))


RE_WORD = re.compile(r"[0-9A-Za-z']+")


def lexical(data: bytes) -> dict:
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError:
        return {}
    words = RE_WORD.findall(text.lower())
    if not words:
        return {}
    return {
        "words": len(words),
        "unique_words": len(set(words)),
        "type_token_ratio": round(len(set(words)) / len(words), 4),
    }


def measure(path: Path, meta: dict, payload: "bytes | None" = None) -> dict:
    data = path.read_bytes() if payload is None else payload
    n = len(data)
    row = {
        "id": meta["id"],
        "category": meta["category"],
        "origin": meta.get("origin", "salvaged"),
        "url": meta.get("url"),
        "note": meta.get("note", ""),
        "file": str(path.relative_to(BASE)).replace("\\", "/"),
        "bytes": n,
        "sha256_12": hashlib.sha256(data).hexdigest()[:12],
    }
    row["http_status"] = meta.get("http_status")
    row["fetched_at"] = meta.get("fetched_at")
    row["truncated"] = bool(meta.get("truncated"))
    row["scrubbed"] = meta.get("scrubbed") or {}
    if n == 0:
        row.update(entropy_bits_per_byte=0.0, unique_bytes=0, gzip_bytes=20,
                   gzip_ratio=None, lzma_ratio=None, non_ascii_ratio=0.0)
        return row

    row["entropy_bits_per_byte"] = round(shannon(data), 4)
    row["unique_bytes"] = len(set(data))
    row["gzip_bytes"] = gz(data)
    row["gzip_ratio"] = round(gz(data) / n, 4)
    row["bz2_ratio"] = round(bz(data) / n, 4)
    row["lzma_ratio"] = round(xz(data) / n, 4)
    try:
        text = data.decode("utf-8")
        row["non_ascii_ratio"] = round(
            sum(1 for ch in text if ord(ch) > 127) / max(len(text), 1), 4)
    except UnicodeDecodeError:
        row["non_ascii_ratio"] = None
    row.update(lexical(data))
    return row


# --------------------------------------------------------------------------
# fetch
# --------------------------------------------------------------------------
def curl(url: str, out: Path) -> tuple[int, str, str]:
    """Return (http_status, stderr, effective_url).  status 0 means no reply."""
    cmd = [
        "curl", "-sS", "-L", "--compressed", "--ssl-no-revoke",
        "--max-time", str(TIMEOUT),
        "-A", UA, "-o", str(out), "-w", "%{http_code}",
        url,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")
    code = (proc.stdout or "").strip()
    try:
        status = int(code)
    except ValueError:
        status = 0
    return status, (proc.stderr or "").strip(), ""


def truncate_bytes(data: bytes, cap: int, binary: bool = False) -> tuple[bytes, bool]:
    if len(data) <= cap:
        return data, False
    cut = data[:cap]
    if binary:
        return cut, True
    # do not leave a half-decoded multi-byte sequence behind
    while cut and (cut[-1] & 0xC0) == 0x80:
        cut = cut[:-1]
    try:
        cut.decode("utf-8")
    except UnicodeDecodeError:
        cut = cut[:-1]
    return cut, True


def fetch_one(src: dict) -> dict:
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    tmp = RAW_DIR / (src["id"] + ".raw")
    if tmp.exists():
        tmp.unlink()

    status, err, _ = curl(src["url"], tmp)
    raw = tmp.read_bytes() if tmp.exists() else b""

    binary = src["ext"] == "bin"
    if not raw:
        # the corpse of a dead link is an HTTP client's error string
        raw = ("[no response body]\n"
               "curl exit: %s\n"
               "curl stderr: %s\n"
               "url: %s\n" % ("n/a", err or "(empty)",
                              src["url"])).encode("utf-8")

    raw, was_cut = truncate_bytes(raw, CAP, binary=binary)
    scrubbed = {}
    if not binary:
        text = raw.decode("utf-8", errors="replace")
        text = unicodedata.normalize("NFC", text)
        text, scrubbed = scrub(text)
        raw = text.encode("utf-8")
        # redaction can lengthen the payload -- re-apply the cap so the
        # budget stated in manifest.json is actually true of what ships
        raw, was_cut2 = truncate_bytes(raw, CAP)
        was_cut = was_cut or was_cut2

    # ASCII-only filenames keep the whole git/GitHub pipeline boring.
    name = "%s.%s" % (src["id"], src["ext"])
    path = SPECIMEN_DIR / name
    SPECIMEN_DIR.mkdir(parents=True, exist_ok=True)
    path.write_bytes(raw)

    src = dict(src)
    src.update(origin="salvaged", http_status=status, truncated=was_cut,
               scrubbed=scrubbed,
               fetched_at=datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
               file=path)
    return src


def fetch_all(only: "list[str] | None" = None) -> list:
    todo = [s for s in SOURCES if not only or s["id"] in only]
    done = []
    for i, src in enumerate(todo, 1):
        meta = fetch_one(src)
        size = meta["file"].stat().st_size
        flag = "" if meta["http_status"] == 200 else "  <- %s" % (
            meta["http_status"] or "no reply")
        print("[%2d/%d] %-28s %7d B%s" % (i, len(todo), src["id"], size, flag))
        print("        %s" % src["url"])
        done.append(meta)
        time.sleep(PAUSE)
    return done


# --------------------------------------------------------------------------
# synthetic controls
# --------------------------------------------------------------------------
# Same size, opposite price.  These are not salvaged from anywhere; they
# exist so the two extremes of the ledger sit side by side at 1 KiB.

CONTROLS = [
    dict(id="ctrl-zeros-1k", category="对照", ext="bin", size=1024,
         fill=lambda: b"\x00" * 1024,
         note="1 KiB 的 0x00。零信息，零成本。"),
    dict(id="ctrl-letter-a-1k", category="对照", ext="txt", size=1024,
         fill=lambda: b"a" * 1024,
         note="1 KiB 的字母 a。零信息，几乎零成本。"),
    dict(id="ctrl-english-1k", category="对照", ext="txt", size=1024,
         fill=lambda: (b"the quick brown fox jumps over the lazy dog. "
                       b"pack my box with five dozen liquor jugs. "
                       b"how vexingly quick daft zebras jump. " * 8)[:1024],
         note="1 KiB 的普通英文。有结构，所以比零字节贵，但远不如随机数贵。"),
    dict(id="ctrl-letter-a-1M", category="对照", ext="txt", size=1024 * 1024,
         fill=lambda: b"a" * (1024 * 1024),
         note="1 MiB 的字母 a。整个收藏里最大的一份原始体积，也是最便宜的一份。"
              "它存在的唯一目的是量化「体积」和「代价」在这个仓库里是两个不同的量。"),
]


def build_controls() -> list:
    CONTROL_DIR.mkdir(parents=True, exist_ok=True)
    out = []
    for c in CONTROLS:
        path = CONTROL_DIR / ("%s.%s" % (c["id"], c["ext"]))
        path.write_bytes(c["fill"]())
        out.append({k: v for k, v in c.items() if k != "fill"})
    return out


# --------------------------------------------------------------------------
# analysis
# --------------------------------------------------------------------------
def analyze() -> dict:
    rows = []
    for p in sorted(SPECIMEN_DIR.glob("*")):
        if p.is_file():
            rows.append(measure(p, {"id": p.stem, "category": "未分类", "origin": "salvaged"}))
    for p in sorted(CONTROL_DIR.glob("*")):
        if p.is_file():
            cid = p.stem
            note = next((c["note"] for c in CONTROLS if c["id"] == cid), "")
            rows.append(measure(p, dict(id=cid, category="对照",
                                        origin="synthetic", note=note)))

    # re-attach the rich metadata recorded at fetch time
    side = RAW_DIR / "meta.json"
    if side.exists():
        meta = {m["id"]: m for m in json.loads(side.read_text("utf-8"))}
        for r in rows:
            m = meta.get(r["id"])
            if m:
                r["url"] = m.get("url")
                r["category"] = m.get("category", r["category"])
                r["note"] = m.get("note", r["note"])
                r["http_status"] = m.get("http_status")
                r["fetched_at"] = m.get("fetched_at")
                r["truncated"] = m.get("truncated", False)
                r["scrubbed"] = m.get("scrubbed") or {}

    rows.sort(key=lambda r: (r["category"], -r["bytes"]))

    total = sum(r["bytes"] for r in rows)
    total_gz = sum(r.get("gzip_bytes") or 0 for r in rows)

    by_cat = collections.defaultdict(lambda: {"specimens": 0, "bytes": 0, "gzip_bytes": 0})
    for r in rows:
        c = by_cat[r["category"]]
        c["specimens"] += 1
        c["bytes"] += r["bytes"]
        c["gzip_bytes"] += r.get("gzip_bytes") or 0

    return {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "generator": "junk.py",
        "budget": {"specimen_cap_bytes": CAP, "specimens": len(rows),
                   "total_bytes": total, "total_gzip_bytes": total_gz,
                   "packed_ratio": round(total_gz / total, 4) if total else None},
        "method": {
            "entropy_bits_per_byte": "零阶字节熵 H = -Σ p log2 p。按字节频率估，"
                                     "忽略字节间结构，因此是真实信息量的**上界**，"
                                     "只用来区分「扁平」与「有结构」。",
            "gzip_ratio": "gzip.compress(data, level=9, mtime=0) 后长度 / 原始长度。"
                          "这是实测，不是估算。",
            "lzma_ratio": "lzma preset 9|EXTREME，代表现实中归档器能达到的下限。",
            "type_token_ratio": "去重词数 / 总词数，衡量文本的词汇贫乏程度。",
            "caveat": "小样本（<1 KiB）的压缩比会被容器头开销污染，gzip 最少约 20 字节。",
        },
        "summary_by_category": [
            dict(category=k, **_sortkey(v)) for k, v in
            sorted(by_cat.items(), key=lambda kv: -kv[1]["bytes"])
        ],
        "specimens": rows,
    }


def _sortkey(v):
    out = dict(v)
    out["gzip_ratio"] = round(v["gzip_bytes"] / v["bytes"], 4) if v["bytes"] else None
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description="collect and weigh internet junk")
    ap.add_argument("--fetch", action="store_true", help="salvage specimens")
    ap.add_argument("--analyze", action="store_true", help="measure and write manifest")
    ap.add_argument("--all", action="store_true", help="fetch then analyze")
    ap.add_argument("--only", default="", help="comma-separated source ids")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()

    if args.self_test:
        return self_test()
    if not (args.fetch or args.analyze or args.all):
        ap.print_help()
        return 2

    if args.fetch or args.all:
        only = [s for s in args.only.split(",") if s] or None
        done = fetch_all(only)
        RAW_DIR.mkdir(parents=True, exist_ok=True)
        (RAW_DIR / "meta.json").write_text(
            json.dumps([{k: v for k, v in d.items() if k != "file"}
                        for d in done], ensure_ascii=False, indent=2),
            "utf-8")
        (RAW_DIR / "controls.json").write_text(
            json.dumps([{k: v for k, v in d.items() if k != "file"}
                        for d in build_controls()], ensure_ascii=False, indent=2),
            "utf-8")
        print("\ncontrols written -> %s" % CONTROL_DIR)

    if args.analyze or args.all:
        man = analyze()
        MANIFEST.write_text(json.dumps(man, ensure_ascii=False, indent=2), "utf-8")
        b = man["budget"]
        print("\nmanifest -> %s" % MANIFEST)
        print("%d specimens, %d B raw, %d B gzipped (%.4f of raw)"
              % (b["specimens"], b["total_bytes"], b["total_gzip_bytes"],
                 b["packed_ratio"] or 0))
    return 0


def self_test() -> int:
    ok = True

    def check(label, got, want):
        nonlocal ok
        if got != want:
            ok = False
            print("FAIL %-34s got %r want %r" % (label, got, want))
        else:
            print("ok   %-34s %r" % (label, got))

    check("entropy of empty", shannon(b""), 0.0)
    check("entropy of 1 KiB of a", round(shannon(b"a" * 1024), 4), 0.0)
    check("entropy of uniform bytes", round(shannon(bytes(range(256)) * 4), 4), 8.0)
    check("entropy of a single byte", shannon(b"x"), 0.0)

    check("gzip hurts tiny input", gz(b"a") > 1, True)
    check("gzip flattens 1 KiB of a", gz(b"a" * 1024) < 40, True)
    check("lzma flattens 1 MiB of a", xz(b"a" * (1 << 20)) < 1024, True)

    rnd = os.urandom(4096)
    check("random is incompressible", gz(rnd) > 4096 * 0.99, True)

    t, c = scrub("mail me at bob.smith@example.org please")
    check("email scrubbed", "[email-redacted]" in t, True)
    check("scrub counted", c.get("email-redacted"), 1)
    t, _ = scrub("key AKIAIOSFODNN7EXAMPLE0123456789abc")
    check("long token scrubbed", "[token-redacted]" in t, True)

    check("lexical counts words", lexical(b"a b a c")["unique_words"], 3)
    check("lexical ttr", lexical(b"a b a c")["type_token_ratio"], 0.75)
    check("lexical skips binary", lexical(b"\xff\xfe\x00"), {})

    b, cut = truncate_bytes(b"x" * 10, 4)
    check("truncate cuts", (len(b), cut), (4, True))
    b, cut = truncate_bytes("你好世界".encode("utf-8"), 7)
    check("truncate keeps utf8 valid", (b.decode("utf-8"), cut), ("你好", True))

    print("\n%s" % ("self-test passed" if ok else "self-test FAILED"))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
