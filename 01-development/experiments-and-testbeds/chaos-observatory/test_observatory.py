import tempfile
import unittest
from pathlib import Path

from observe import survey
from weather import forecast


class ObservatoryTest(unittest.TestCase):
    def test_survey_counts_without_reading_contents(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "layer").mkdir()
            (root / "layer" / "fossil.rs").write_text("fn main() {}")
            (root / "note").write_text("classified")

            report = survey(root)

            self.assertEqual(report["files_observed"], 2)
            self.assertEqual(report["species_count"], 2)
            self.assertEqual(report["maximum_depth"], 1)
            self.assertIn("active state of formation", forecast(report))

    def test_survey_limit_is_respected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for number in range(3):
                (root / f"specimen-{number}.txt").touch()

            report = survey(root, max_files=2)

            self.assertEqual(report["files_observed"], 2)
            self.assertTrue(report["truncated"])


if __name__ == "__main__":
    unittest.main()
