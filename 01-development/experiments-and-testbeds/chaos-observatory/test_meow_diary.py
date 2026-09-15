import unittest

from meow_diary import compare, event_panel, markdown
import test_meow_museum as fixtures


class DiaryTest(unittest.TestCase):
    setUp = fixtures.MuseumTest.setUp
    git = fixtures.MuseumTest.git
    commit = fixtures.MuseumTest.commit
    baseline = fixtures.MuseumTest.baseline

    def test_net_counts_rename_delete_and_merge_once(self):
        base = self.baseline()
        main = self.git("branch", "--show-current")
        self.git("checkout", "-qb", "visitor")
        (self.root / "quiet" / "new.txt").write_text("new")
        self.commit("visitor one")
        (self.root / "visited" / "MEOW.md").write_text("changed")
        self.commit("visitor two")
        self.git("checkout", "-q", main)
        self.git("-c", "core.hooksPath=/dev/null", "merge", "--no-ff", "-qm", "welcome", "visitor")
        (self.root / "quiet" / "new.txt").rename(self.root / "quiet" / "renamed.txt")
        (self.root / "gone" / "MEOW.md").unlink()
        end = self.commit("rename and remove")
        report = compare(self.root, base, end, base)
        self.assertEqual(report["event_total"], 2)
        self.assertEqual(report["counts"], {"D": 1, "A": 1, "M": 1})
        self.assertEqual(report["events"][0]["counts"], {"D": 1, "R": 1})
        self.assertEqual(set(report["lost"]), {"quiet", "visited", "gone"})

    def test_transient_changes_and_limits_are_honest(self):
        base = self.baseline()
        path = self.root / "quiet" / "temporary"
        path.write_text("temporary")
        self.commit("arrive")
        path.unlink()
        self.commit("leave")
        report = compare(self.root, base, baseline=base, limit=1)
        self.assertEqual(report["counts"], {})
        self.assertEqual(report["event_total"], 2)
        self.assertTrue(report["truncated"])
        self.assertIn("1 / 2", markdown(report))
        self.assertEqual(len(report["lost"]), 1)

    def test_snapshot_on_side_branch_compares_without_recounting_files(self):
        base = self.baseline()
        main = self.git("branch", "--show-current")
        self.git("checkout", "-qb", "side")
        (self.root / "quiet" / "note").write_text("side")
        side = self.commit("side")
        self.git("checkout", "-q", main)
        self.git("-c", "core.hooksPath=/dev/null", "merge", "--no-ff", "-qm", "merge", "side")
        report = compare(self.root, side, baseline=base)
        self.assertEqual(report["counts"], {})
        self.assertEqual(report["lost"], [])
        self.assertEqual(report["event_total"], 1)
        self.assertEqual(report["events"][0]["subject"], "merge")

    def test_empty_interval_and_html_subject(self):
        base = self.baseline()
        report = compare(self.root, base, base, base)
        self.assertEqual(report["events"], [])
        (self.root / "quiet" / "note").write_text("new")
        self.commit("<img src=x onerror=alert(1)>")
        report = compare(self.root, base, baseline=base)
        self.assertNotIn("<img", event_panel(report))
        self.assertNotIn("<img", markdown(report))


if __name__ == "__main__":
    unittest.main()
