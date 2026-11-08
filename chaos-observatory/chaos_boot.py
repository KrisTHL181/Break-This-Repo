#!/usr/bin/env python3
"""Render an observation snapshot as a clearly fictional kernel boot log."""

from __future__ import annotations

import argparse
from pathlib import Path

from observation_reports import _safe_terminal_text, load_report


def boot_log(report: dict) -> str:
    """Turn validated aggregate metadata into a harmless boot-log parody."""
    label = _safe_terminal_text(report["label"] or "observed repository")
    scan_state = "PARTIAL SAMPLE" if report["truncated"] else "COMPLETE SAMPLE"
    return "\n".join(
        [
            "BREAK-THIS-REPO KERNEL 0.1.0-chaos (SIMULATION ONLY)",
            "[    0.000000] boot: no Linux kernel is running; this is a text-only parody",
            f"[    0.013337] chaosfs: mounting '{label}' read-only",
            (
                f"[    0.023000] chaosfs: {report['files_observed']} inodes, "
                f"{report['directories_observed']} directories, "
                f"{report['bytes_observed']} known bytes"
            ),
            (
                f"[    0.031415] dentry: {report['species_count']} file species; "
                f"maximum depth {report['maximum_depth']}"
            ),
            (
                f"[    0.042000] chaos: index {report['chaos_index']:g}/100; "
                f"{scan_state.lower()}"
            ),
            (
                f"[    0.050000] safety: {report['symbolic_links_not_followed']} "
                "symlinks left unexplored; observed contents remain unread"
            ),
            "[    0.100000] init: the site remains in an active state of formation.",
            "NOTICE: no Linux source was copied, no command was run, and no files were changed.",
        ]
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path, help="JSON output from observe.py")
    args = parser.parse_args()
    try:
        report = load_report(args.snapshot)
    except ValueError as error:
        parser.exit(1, f"Could not read observation: {error}\n")
    print(boot_log(report))


if __name__ == "__main__":
    main()
