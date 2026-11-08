#!/usr/bin/env python3
"""Read-only repository field survey using names and metadata only."""

from __future__ import annotations

import argparse
import json
import math
import os
import stat
from collections import Counter
from pathlib import Path
from typing import Any, Optional

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


def survey(
    root: Path,
    max_files: int = 100_000,
    root_label: Optional[str] = None,
) -> dict[str, Any]:
    """Survey a directory without opening files or following symbolic links.

    The supplied root must itself be a real directory, not a symbolic link.
    ``root_label`` is intended for display and is deliberately separate from
    the filesystem path so reports do not disclose a machine-local location.
    """
    if isinstance(max_files, bool) or not isinstance(max_files, int) or max_files < 1:
        raise ValueError("max_files must be positive")
    if root_label is not None and not isinstance(root_label, str):
        raise ValueError("root_label must be a string")
    label = root_label.strip() if root_label is not None else ""
    if not label:
        label = "observed repository"

    # abspath normalizes the path lexically without dereferencing its final
    # component. resolve() here would silently follow a symlink root.
    root = Path(os.path.abspath(os.fspath(root)))
    root_stat = os.lstat(root)
    if stat.S_ISLNK(root_stat.st_mode):
        raise ValueError("root must not be a symbolic link")
    if not stat.S_ISDIR(root_stat.st_mode):
        raise ValueError("root must be a directory")

    species: Counter[str] = Counter()
    files = directories = symlinks = errors = total_bytes = max_depth = 0
    truncated = False
    stack: list[tuple[Path, int]] = [(root, 0)]

    while stack:
        current, depth = stack.pop()
        max_depth = max(max_depth, depth)
        try:
            with os.scandir(current) as directory:
                entries = sorted(directory, key=lambda entry: os.fsencode(entry.name))
        except OSError:
            errors += 1
            continue

        child_directories: list[tuple[Path, int]] = []
        for entry in entries:
            try:
                if entry.is_symlink():
                    symlinks += 1
                elif entry.is_dir(follow_symlinks=False):
                    directories += 1
                    if entry.name not in SKIP_DIRECTORIES:
                        child_directories.append((Path(entry.path), depth + 1))
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
        if truncated:
            break
        # The stack is LIFO; reverse insertion preserves sorted directory
        # order across runs and makes a capped observation reproducible.
        stack.extend(reversed(child_directories))

    diversity = len(species)
    chaos_score = round(
        min(100.0, 8 * math.log10(files + 1) + 3 * diversity + 2 * max_depth),
        1,
    )
    return {
        "schema_version": 1,
        "root_label": label,
        "files_observed": files,
        "directories_observed": directories,
        "symbolic_links_not_followed": symlinks,
        "metadata_errors": errors,
        "bytes_observed": total_bytes,
        "maximum_depth": max_depth,
        "species_count": diversity,
        "common_species": species.most_common(12),
        "species_counts": dict(sorted(species.items())),
        "chaos_index": chaos_score,
        "truncated": truncated,
        "conclusion": "The site remains in an active state of formation.",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", type=Path, default=Path(".."))
    parser.add_argument("--max-files", type=int, default=100_000)
    parser.add_argument("--label", help="safe display label for this observation")
    args = parser.parse_args()
    if args.max_files < 1:
        parser.error("--max-files must be positive")
    try:
        report = survey(args.root, args.max_files, args.label)
    except (OSError, ValueError) as error:
        parser.exit(1, f"Observation could not open: {error}\n")
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
