#!/usr/bin/env python3
"""Build the persistent multi-project GitHub Pages tree.

Each generated WebGAL package is still a fully standalone download artifact.
For Pages only, the large top-level WebGAL build assets are content-addressed
and shared across projects. Project-specific game/ data stays under /<repo>/.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import shutil
from pathlib import Path


PROJECT_RE = re.compile(r"^[A-Za-z0-9._-]+$")
META_FILE = ".code2gal-pages.json"


def hash_tree(root: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix().encode("utf-8")
        digest.update(len(rel).to_bytes(4, "big"))
        digest.update(rel)
        digest.update(path.stat().st_size.to_bytes(8, "big"))
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    return digest.hexdigest()


def rewrite_index(index: Path, engine_key: str) -> int:
    text = index.read_text(encoding="utf-8")
    shared = f"../_engine/{engine_key}/assets/"
    # Vite/WebGAL releases normally use ./assets/. Cover root-relative and
    # bare forms too so a future WebGAL build does not silently break Pages.
    pattern = re.compile(r"(?P<quote>[\"'])(?P<path>(?:\./|/)?assets/)")
    text, count = pattern.subn(lambda m: f"{m.group('quote')}{shared}", text)
    if count == 0:
        raise RuntimeError("index.html contains no rewritable WebGAL assets reference")
    index.write_text(text, encoding="utf-8")
    return count


def install_project(site: Path, store: Path, project: str) -> tuple[str, int]:
    if not PROJECT_RE.fullmatch(project):
        raise ValueError(f"unsafe project name: {project!r}")
    if not (site / "index.html").is_file():
        raise FileNotFoundError(f"missing WebGAL index.html: {site / 'index.html'}")
    assets = site / "assets"
    if not assets.is_dir():
        raise FileNotFoundError(f"missing WebGAL assets directory: {assets}")

    engine_key = hash_tree(assets)[:16]
    shared_assets = store / "_engine" / engine_key / "assets"
    if not shared_assets.exists():
        shared_assets.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(assets, shared_assets)

    target = store / project
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(site, target, ignore=shutil.ignore_patterns("assets"))

    rewrites = rewrite_index(target / "index.html", engine_key)
    (target / META_FILE).write_text(
        json.dumps(
            {
                "project": project,
                "engine": engine_key,
                "asset_rewrites": rewrites,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return engine_key, rewrites


def project_entries(store: Path) -> list[dict[str, str]]:
    entries: list[dict[str, str]] = []
    for path in sorted(store.iterdir(), key=lambda p: p.name.casefold()):
        if not path.is_dir() or path.name.startswith("_"):
            continue
        meta = path / META_FILE
        if not meta.is_file() or not (path / "index.html").is_file():
            continue
        try:
            value = json.loads(meta.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        entries.append(
            {
                "project": str(value.get("project") or path.name),
                "engine": str(value.get("engine") or ""),
            }
        )
    return entries


def prune_engines(store: Path, entries: list[dict[str, str]]) -> None:
    engine_root = store / "_engine"
    if not engine_root.is_dir():
        return
    used = {item["engine"] for item in entries if item["engine"]}
    for path in engine_root.iterdir():
        if path.is_dir() and path.name not in used:
            shutil.rmtree(path)


def write_home(store: Path, entries: list[dict[str, str]]) -> None:
    cards = "\n".join(
        f'<li><a href="./{html.escape(item["project"], quote=True)}/">'
        f'{html.escape(item["project"])}</a>'
        f'<small> WebGAL {html.escape(item["engine"])}</small></li>'
        for item in entries
    )
    page = f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Code2Gal</title>
  <style>
    body{{font-family:system-ui,sans-serif;max-width:760px;margin:48px auto;padding:0 20px;line-height:1.6}}
    h1{{margin-bottom:.25rem}} ul{{padding:0;list-style:none}} li{{margin:12px 0}}
    a{{font-size:1.15rem}} small{{margin-left:.5rem;opacity:.55}}
  </style>
</head>
<body>
  <h1>Code2Gal</h1>
  <p>Generated WebGAL projects</p>
  <ul>{cards}</ul>
</body>
</html>
"""
    (store / "index.html").write_text(page, encoding="utf-8")
    (store / ".nojekyll").touch()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--site", required=True, type=Path)
    parser.add_argument("--store", required=True, type=Path)
    parser.add_argument("--project")
    args = parser.parse_args()

    site = args.site.resolve()
    store = args.store.resolve()
    project = args.project or site.name

    store.mkdir(parents=True, exist_ok=True)
    engine_key, rewrites = install_project(site, store, project)
    entries = project_entries(store)
    prune_engines(store, entries)
    write_home(store, entries)

    total = sum(p.stat().st_size for p in store.rglob("*") if p.is_file())
    print(f"project={project}")
    print(f"engine={engine_key}")
    print(f"asset_rewrites={rewrites}")
    print(f"projects={len(entries)}")
    print(f"published_bytes={total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
