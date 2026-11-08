import json
import tempfile
import unittest
from pathlib import Path

from observation_reports import compare_reports, format_comparison, load_report, load_series, validate_report
from observation_trend import render_trend


def legacy_report(root, files=2, species=None):
    return {
        "root": str(root),
        "files_observed": files,
        "directories_observed": 1,
        "symbolic_links_not_followed": 0,
        "metadata_errors": 0,
        "bytes_observed": 12,
        "maximum_depth": 1,
        "species_count": 2,
        "common_species": species or [[".txt", 1], [".py", 1]],
        "chaos_index": 2.5,
        "truncated": False,
    }


def current_report(label="field station", files=2, species=None):
    counts = species or {".py": 1, ".txt": 1}
    return {
        "schema_version": 1,
        "root_label": label,
        "files_observed": files,
        "directories_observed": 1,
        "symbolic_links_not_followed": 0,
        "metadata_errors": 0,
        "bytes_observed": 12,
        "maximum_depth": 1,
        "species_count": len(counts),
        "species_counts": counts,
        "chaos_index": 2.5,
        "truncated": False,
    }


class ObservationReportsTest(unittest.TestCase):
    def test_legacy_and_current_snapshots_compare_without_copying_paths(self):
        before = legacy_report("/private/machine/repository")
        after = current_report(files=4, species={".py": 1, ".txt": 2, ".rs": 1})

        report = compare_reports(before, after)
        text = format_comparison(report)
        encoded = json.dumps(report)

        self.assertEqual(report["metric_deltas"]["files_observed"], 2)
        self.assertEqual(report["species_deltas"], {".py": 0, ".rs": 1, ".txt": 1})
        self.assertFalse(report["species_counts_complete"])
        self.assertNotIn("/private/machine", encoded + text)
        self.assertIn("不完整", text)

    def test_same_snapshot_has_zero_deltas(self):
        report = compare_reports(current_report(), current_report())
        self.assertTrue(all(value == 0 for value in report["metric_deltas"].values()))
        self.assertTrue(all(value == 0 for value in report["species_deltas"].values()))
        self.assertTrue(report["species_counts_complete"])

    def test_trend_page_escapes_hostile_labels_and_has_no_scripts(self):
        hostile = current_report(label='</th><script>alert("x")</script>\u202e')
        page = render_trend([validate_report(hostile), validate_report(current_report(label="later"))])
        self.assertNotIn("<script>alert", page)
        self.assertIn("&lt;script&gt;", page)
        self.assertIn("\\u202e", page)

    def test_load_series_and_invalid_json(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first, second = root / "before.json", root / "after.json"
            first.write_text(json.dumps(legacy_report("/private/path")), encoding="utf-8")
            second.write_text(json.dumps(current_report()), encoding="utf-8")
            series = load_series([first, second])
            self.assertEqual([item["schema_version"] for item in series], [0, 1])
            with self.assertRaises(ValueError):
                load_series([first])
            second.write_text("{bad json", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "could not read"):
                load_report(second)

    def test_malformed_snapshots_are_rejected(self):
        cases = [
            {"schema_version": 9},
            {**current_report(), "files_observed": -1},
            {**current_report(), "bytes_observed": float("nan")},
            {**current_report(), "truncated": 0},
            {**current_report(), "species_counts": {".py": -1}},
            {**current_report(), "species_count": 5},
        ]
        for case in cases:
            with self.subTest(case=case), self.assertRaises(ValueError):
                compare_reports(case, current_report())


if __name__ == "__main__":
    unittest.main()
