#!/usr/bin/env python3
"""Read-only repository field survey using names and metadata only."""

from __future__ import annotations

import argparse
import json
import math
import os
from collections import Counter
from pathlib import Path
from typing import Any

SKIP_DIRECTORIES = {
    ".git",
    ".hg",
    ".svn",
    "node_modules",
    "__pycache__",
    ".venv",
    "venv",
}


def classify(name: str) -> str:
    """Return a display-friendly species name for a filename."""
    suffix = Path(name).suffix.lower()
    return suffix if suffix else "[no extension]"


def survey(root: Path, max_files: int = 100_000) -> dict[str, Any]:
    """Survey *root* without opening files or following symbolic links."""
    root = root.resolve()
    species: Counter[str] = Counter()
    files = directories = symlinks = errors = total_bytes = max_depth = 0
    truncated = False
    stack: list[tuple[Path, int]] = [(root, 0)]

    while stack:
        current, depth = stack.pop()
        max_depth = max(max_depth, depth)
        try:
            entries = list(os.scandir(current))
        except OSError:
            errors += 1
            continue

        for entry in entries:
            try:
                if entry.is_symlink():
                    symlinks += 1
                elif entry.is_dir(follow_symlinks=False):
                    directories += 1
                    if entry.name not in SKIP_DIRECTORIES:
                        stack.append((Path(entry.path), depth + 1))
                elif entry.is_file(follow_symlinks=False):
                    files += 1
                    species[classify(entry.name)] += 1
                    total_bytes += entry.stat(follow_symlinks=False).st_size
                    if files >= max_files:
                        truncated = True
                        stack.clear()
                        break
            except OSError:
                errors += 1

    diversity = len(species)
    chaos_score = round(
        min(100.0, 8 * math.log10(files + 1) + 3 * diversity + 2 * max_depth),
        1,
    )
    return {
        "root": str(root),
        "files_observed": files,
        "directories_observed": directories,
        "symbolic_links_not_followed": symlinks,
        "metadata_errors": errors,
        "bytes_observed": total_bytes,
        "maximum_depth": max_depth,
        "species_count": diversity,
        "common_species": species.most_common(12),
        "chaos_index": chaos_score,
        "truncated": truncated,
        "conclusion": "The site remains in an active state of formation.",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", type=Path, default=Path(".."))
    parser.add_argument("--max-files", type=int, default=100_000)
    args = parser.parse_args()
    if args.max_files < 1:
        parser.error("--max-files must be positive")
    print(json.dumps(survey(args.root, args.max_files), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
