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
            self.assertEqual(report["schema_version"], 1)
            self.assertEqual(report["root_label"], "observed repository")
            self.assertEqual(report["species_counts"], {".rs": 1, "[no extension]": 1})
            self.assertNotIn(str(root), str(report))

    def test_root_symlink_is_rejected_and_nested_links_are_not_followed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            target = base / "target"
            target.mkdir()
            (target / "outside.txt").write_text("outside")
            root_link = base / "root-link"
            root_link.symlink_to(target, target_is_directory=True)

            with self.assertRaisesRegex(ValueError, "root must not be a symbolic link"):
                survey(root_link)

            root = base / "root"
            root.mkdir()
            (root / "inside.txt").write_text("inside")
            (root / "outside").symlink_to(target, target_is_directory=True)
            report = survey(root)

            self.assertEqual(report["files_observed"], 1)
            self.assertEqual(report["symbolic_links_not_followed"], 1)

    def test_scan_order_is_stable_and_label_is_explicit(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "z-dir").mkdir()
            (root / "a-dir").mkdir()
            (root / "z-dir" / "last.rs").touch()
            (root / "a-dir" / "first.py").touch()
            (root / "middle.txt").touch()

            first = survey(root, max_files=2, root_label="field station 1")
            second = survey(root, max_files=2, root_label="field station 1")

            self.assertEqual(first, second)
            self.assertEqual(first["root_label"], "field station 1")
            self.assertEqual(first["species_counts"], {".py": 1, ".txt": 1})
            self.assertTrue(first["truncated"])

    def test_invalid_root_and_file_limit_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            ordinary_file = root / "file.txt"
            ordinary_file.touch()
            with self.assertRaisesRegex(ValueError, "root must be a directory"):
                survey(ordinary_file)
            for limit in (0, -1, True, 1.5):
                with self.subTest(limit=limit), self.assertRaises(ValueError):
                    survey(root, max_files=limit)  # type: ignore[arg-type]

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
