"""Shared validation and comparison for saved observatory snapshots."""

from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any, Dict, List, Optional
import unicodedata

METRICS = (
    "files_observed",
    "directories_observed",
    "symbolic_links_not_followed",
    "metadata_errors",
    "bytes_observed",
    "maximum_depth",
    "species_count",
    "chaos_index",
)


def _nonnegative_number(value: Any, name: str) -> Any:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{name} must be a non-negative number")
    try:
        finite = math.isfinite(value)
    except OverflowError:
        finite = False
    if not finite or value < 0:
        raise ValueError(f"{name} must be a non-negative finite number")
    return value


def _species_counts(report: Dict[str, Any], version: int) -> tuple[Dict[str, int], bool]:
    if version == 1:
        counts = report.get("species_counts")
        if not isinstance(counts, dict):
            raise ValueError("schema version 1 requires species_counts")
        normalized: Dict[str, int] = {}
        for species, count in counts.items():
            if not isinstance(species, str):
                raise ValueError("species names must be strings")
            if isinstance(count, bool) or not isinstance(count, int) or count < 0:
                raise ValueError("species counts must be non-negative integers")
            normalized[species] = count
        return normalized, True

    # Version 0 snapshots only kept the twelve most common categories. Keep
    # them comparable, but tell callers that the distribution is incomplete.
    common = report.get("common_species", [])
    if not isinstance(common, list):
        raise ValueError("legacy common_species must be a list")
    normalized = {}
    for item in common:
        if not isinstance(item, (list, tuple)) or len(item) != 2:
            raise ValueError("legacy common_species entries must be pairs")
        species, count = item
        if not isinstance(species, str):
            raise ValueError("species names must be strings")
        if isinstance(count, bool) or not isinstance(count, int) or count < 0:
            raise ValueError("species counts must be non-negative integers")
        normalized[species] = count
    return normalized, False


def validate_report(report: Any) -> Dict[str, Any]:
    """Validate and normalize a current or legacy observation report.

    Legacy reports have no ``schema_version`` and may contain a local ``root``
    path. That path is deliberately ignored and never copied into a result.
    """
    if not isinstance(report, dict):
        raise ValueError("snapshot must be a JSON object")
    version = report.get("schema_version", 0)
    if isinstance(version, bool) or not isinstance(version, int) or version not in (0, 1):
        raise ValueError(f"unsupported observation schema version: {version}")

    normalized: Dict[str, Any] = {"schema_version": version}
    for metric in METRICS:
        value = _nonnegative_number(report.get(metric), metric)
        if metric != "chaos_index" and not isinstance(value, int):
            raise ValueError(f"{metric} must be an integer")
        normalized[metric] = value
    truncated = report.get("truncated")
    if not isinstance(truncated, bool):
        raise ValueError("truncated must be a boolean")
    normalized["truncated"] = truncated

    label = report.get("root_label")
    normalized["label"] = label.strip() if isinstance(label, str) and label.strip() else None
    species, complete = _species_counts(report, version)
    if version == 1 and report["species_count"] != sum(count > 0 for count in species.values()):
        raise ValueError("species_count must match species_counts")
    normalized["species_counts"] = species
    normalized["species_counts_complete"] = complete
    return normalized


def load_report(path: Path) -> Dict[str, Any]:
    """Read one observation JSON file and return its validated form."""
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ValueError(f"could not read observation JSON: {error}") from error
    return validate_report(raw)


def compare_reports(before: Dict[str, Any], after: Dict[str, Any]) -> Dict[str, Any]:
    """Return aggregate deltas without copying source paths or file names."""
    before = validate_report(before)
    after = validate_report(after)
    metrics = {key: after[key] - before[key] for key in METRICS}
    species_keys = sorted(set(before["species_counts"]) | set(after["species_counts"]))
    species = {
        key: after["species_counts"].get(key, 0) - before["species_counts"].get(key, 0)
        for key in species_keys
    }
    return {
        "schema_version": 1,
        "before_label": before["label"] or "Before",
        "after_label": after["label"] or "After",
        "metric_deltas": metrics,
        "species_deltas": species,
        "species_counts_complete": (
            before["species_counts_complete"] and after["species_counts_complete"]
        ),
        "truncated": {"before": before["truncated"], "after": after["truncated"]},
    }


def _safe_terminal_text(value: str) -> str:
    output = []
    for char in value:
        codepoint = ord(char)
        if 0xDC80 <= codepoint <= 0xDCFF:
            output.append(f"\\x{codepoint - 0xDC00:02x}")
        elif unicodedata.category(char) in {"Cc", "Cf", "Cs"}:
            output.append(f"\\u{codepoint:04x}")
        else:
            output.append(char)
    return "".join(output)


def format_comparison(report: Dict[str, Any]) -> str:
    """Render an aggregate comparison as a concise terminal report."""
    report = dict(report)
    before = _safe_terminal_text(str(report["before_label"]))
    after = _safe_terminal_text(str(report["after_label"]))
    lines = [f"观测变化：{before} → {after}", "", "指标变化（后 − 前）："]
    labels = {
        "files_observed": "文件数",
        "directories_observed": "目录数",
        "symbolic_links_not_followed": "未跟随符号链接",
        "metadata_errors": "元数据错误",
        "bytes_observed": "已知字节数",
        "maximum_depth": "最大深度",
        "species_count": "扩展名种类",
        "chaos_index": "混沌指数",
    }
    for metric, label in labels.items():
        delta = report["metric_deltas"][metric]
        rendered = f"{delta:+d}" if isinstance(delta, int) else f"{delta:+g}"
        lines.append(f"- {label}：{rendered}")
    lines.extend(["", "扩展名变化（后 − 前）："])
    species = report["species_deltas"]
    if species:
        for name in sorted(species):
            lines.append(f"- {_safe_terminal_text(name)}：{species[name]:+d}")
    else:
        lines.append("- 无变化")
    if not report["species_counts_complete"]:
        lines.append("- 注意：旧版快照只保存常见类别，扩展名变化不完整。")
    if report["truncated"]["before"] or report["truncated"]["after"]:
        lines.extend(["", "注意：至少一个快照触及扫描上限，指标只代表已观察样本。"])
    return "\n".join(lines)


def load_series(paths: List[Path]) -> List[Dict[str, Any]]:
    if len(paths) < 2:
        raise ValueError("at least two snapshots are required")
    return [load_report(path) for path in paths]
