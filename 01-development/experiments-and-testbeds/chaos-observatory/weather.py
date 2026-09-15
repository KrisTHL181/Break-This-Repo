#!/usr/bin/env python3
"""Turn a read-only repository survey into a digital weather report."""

from __future__ import annotations

import argparse
from pathlib import Path

from observe import survey


def forecast(report: dict) -> str:
    common = report["common_species"]
    dominant = common[0][0] if common else "unidentified files"
    index = report["chaos_index"]
    if index >= 80:
        conditions = "Severe README convection with scattered binary precipitation."
    elif index >= 50:
        conditions = "Mixed-language cloud cover and occasional archive showers."
    else:
        conditions = "Light metadata haze; unusual order cannot be ruled out."
    visibility = "limited by the survey cap" if report["truncated"] else "surprisingly adequate"
    return "\n".join(
        [
            "CHAOS OBSERVATORY — REPOSITORY WEATHER",
            f"Dominant air mass: {dominant}",
            f"Chaos index: {index}/100",
            f"Conditions: {conditions}",
            f"Visibility: {visibility}.",
            "Long-range outlook: The site remains in an active state of formation.",
        ]
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", type=Path, default=Path(".."))
    parser.add_argument("--max-files", type=int, default=100_000)
    args = parser.parse_args()
    if args.max_files < 1:
        parser.error("--max-files must be positive")
    print(forecast(survey(args.root, args.max_files)))


if __name__ == "__main__":
    main()
