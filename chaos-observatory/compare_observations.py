#!/usr/bin/env python3
"""Compare two saved repository observations without exposing source paths."""

import argparse
import json
from pathlib import Path

from observation_reports import compare_reports, format_comparison, load_report


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before", type=Path, help="earlier observation JSON")
    parser.add_argument("after", type=Path, help="later observation JSON")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    args = parser.parse_args()
    try:
        report = compare_reports(load_report(args.before), load_report(args.after))
    except ValueError as error:
        parser.exit(1, f"Comparison failed: {error}\n")
    if args.format == "json":
        print(json.dumps(report, ensure_ascii=True, indent=2, allow_nan=False))
    else:
        print(format_comparison(report))


if __name__ == "__main__":
    main()
