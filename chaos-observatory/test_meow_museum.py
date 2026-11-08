import subprocess
import tempfile
import sys
import unittest
from pathlib import Path

from meow_museum import observe, render


class MuseumTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.name", "Museum test")
        self.git("config", "user.email", "museum@example.invalid")
        self.git("config", "commit.gpgsign", "false")

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args]).decode().strip()

    def commit(self, message):
        self.git("add", "-A")
        self.git("-c", "core.hooksPath=/dev/null", "commit", "-qm", message)
        return self.git("rev-parse", "HEAD")

    def baseline(self):
        for name in ["quiet", "visited", "gone", "literal[1]", "literal1"]:
            (self.root / name).mkdir()
            (self.root / name / "MEOW.md").write_text("cat")
        return self.commit("喵～")

    def test_counts_edits_removals_and_literal_pathspecs(self):
        base = self.baseline()
        (self.root / "visited" / "new.txt").write_text("new visitor")
        (self.root / "gone" / "MEOW.md").unlink()
        (self.root / "literal1" / "MEOW.md").write_text("another visitor")
        self.commit("Visitors")
        report = observe(self.root, base)
        self.assertEqual({e["directory"]: e["status"] for e in report["entries"]}, {
            "quiet": "retained", "visited": "revisited", "gone": "removed",
            "literal[1]": "retained", "literal1": "revisited"})
        self.assertTrue(all(e["status"] == "retained" for e in observe(self.root, base, base)["entries"]))

    def test_uncommitted_files_do_not_change_snapshot(self):
        base = self.baseline()
        (self.root / "quiet" / "MEOW.md").unlink()
        self.assertEqual(observe(self.root, base)["revision"], base)
        self.assertTrue(all(e["status"] == "retained" for e in observe(self.root, base)["entries"]))

    def test_shallow_history_and_invalid_baseline_are_rejected(self):
        base = self.baseline()
        with self.assertRaises(ValueError):
            observe(self.root, "--help")
        (self.root / ".git" / "shallow").write_text(base + "\n")
        with self.assertRaisesRegex(ValueError, "shallow"):
            observe(self.root, base)

    def test_hostile_names_are_text_and_template_tokens_are_preserved(self):
        report = {"baseline": "a" * 40, "revision": "b" * 40, "date": "2026-09-14",
                  "entries": [{"directory": '<img src=x onerror=alert(1)>{{RATE}}',
                               "latest": "a" * 40, "status": "retained"}]}
        page = render(report)
        self.assertNotIn("<img", page)
        self.assertIn("&lt;img", page)
        self.assertIn("{{RATE}}", page)

    def test_cli_preserves_existing_output(self):
        base = self.baseline()
        output = self.root / "exhibit.html"
        output.write_text("existing exhibit")
        result = subprocess.run([sys.executable, str(Path(__file__).with_name("meow_museum.py")),
                                 str(self.root), "--baseline", base, "--output", str(output)],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(output.read_text(), "existing exhibit")


if __name__ == "__main__":
    unittest.main()
