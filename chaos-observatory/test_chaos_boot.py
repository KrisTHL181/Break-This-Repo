import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from chaos_boot import boot_log
from observation_reports import validate_report


def snapshot(**overrides):
    report = {
        "schema_version": 1,
        "root_label": "field station",
        "files_observed": 12,
        "directories_observed": 4,
        "symbolic_links_not_followed": 2,
        "metadata_errors": 0,
        "bytes_observed": 2048,
        "maximum_depth": 3,
        "species_count": 2,
        "species_counts": {".py": 7, ".md": 5},
        "chaos_index": 23.5,
        "truncated": False,
    }
    report.update(overrides)
    return report


class ChaosBootTest(unittest.TestCase):
    def test_log_uses_aggregate_metrics_and_disclaims_real_kernel(self):
        output = boot_log(validate_report(snapshot()))

        self.assertIn("SIMULATION ONLY", output)
        self.assertIn("12 inodes, 4 directories, 2048 known bytes", output)
        self.assertIn("index 23.5/100; complete sample", output)
        self.assertIn("2 symlinks left unexplored", output)
        self.assertIn("no Linux source was copied", output)

    def test_hostile_label_is_escaped_and_truncation_is_visible(self):
        hostile = snapshot(root_label="station\n\u001b[31mALARM\u202e")
        hostile["truncated"] = True
        output = boot_log(validate_report(hostile))

        self.assertNotIn("\u001b", output)
        self.assertNotIn("\u202e", output)
        self.assertIn("\\u001b[31mALARM\\u202e", output)
        self.assertIn("partial sample", output)

    def test_cli_reads_snapshot_and_rejects_invalid_json(self):
        script = Path(__file__).with_name("chaos_boot.py")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "snapshot.json"
            path.write_text(json.dumps(snapshot()), encoding="utf-8")
            good = subprocess.run(
                [sys.executable, str(script), str(path)],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(good.returncode, 0, good.stderr)
            self.assertIn("BREAK-THIS-REPO KERNEL", good.stdout)

            path.write_text("not json", encoding="utf-8")
            bad = subprocess.run(
                [sys.executable, str(script), str(path)],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(bad.returncode, 1)
            self.assertIn("Could not read observation", bad.stderr)
            self.assertNotIn("Traceback", bad.stderr)


if __name__ == "__main__":
    unittest.main()
