import subprocess
import unittest

import test_meow_museum as fixtures
from path_gallery import gallery, inspect, render_gallery, visible


class PathTest(unittest.TestCase):
    setUp = fixtures.MuseumTest.setUp
    git = fixtures.MuseumTest.git

    def seed(self, paths):
        self.git("config", "core.ignorecase", "false")
        self.git("config", "core.precomposeUnicode", "false")
        oid = subprocess.check_output(["git", "-C", str(self.root), "hash-object", "-w", "--stdin"], input=b"specimen").decode().strip()
        for path in paths:
            self.git("update-index", "--add", "--cacheinfo", f"100644,{oid},{path}")
        self.git("-c", "core.hooksPath=/dev/null", "commit", "-qm", "virtual specimens")

    def test_risk_categories_and_benign_names(self):
        self.assertEqual(inspect("normal/readme.md"), [])
        self.assertIn("reserved", inspect("src/CON.txt"))
        self.assertIn("reserved", inspect("COM¹.log"))
        self.assertNotIn("reserved", inspect("COM10.txt"))
        self.assertIn("trailing", inspect("folder./file "))
        self.assertIn("invalid", inspect("tab\tname"))
        self.assertIn("invisible", inspect("file\u202etxt"))
        self.assertIn("component", inspect("猫" * 86))
        self.assertIn("deep", inspect("a/" * 10 + "file"))
        self.assertIn("encoding", inspect("bad\udcff"))

    def test_case_unicode_and_parent_collisions_without_checkout(self):
        self.seed(["A/x", "a/y", "café/file", "cafe\u0301/other", "plain/file"])
        report = gallery(self.root, exhibits=100)
        self.assertEqual(report["counts"]["collision"], 4)
        self.assertEqual(report["flagged"], 4)
        self.assertFalse(report["truncated"])

    def test_limits_and_controls_remain_visible_text(self):
        self.seed(["<img>/CON", "evil\nname", "invisible\u202e.txt"])
        report = gallery(self.root, exhibits=1)
        self.assertEqual(len(report["entries"]), 1)
        self.assertTrue(report["exhibits_truncated"])
        page = render_gallery(gallery(self.root))
        self.assertNotIn("<img>", page)
        self.assertNotIn("\u202e", page)
        self.assertIn("\\u202e", page)
        self.assertEqual(visible("line\nnext"), "line\\u000anext")
        self.assertTrue(gallery(self.root, limit=1)["truncated"])
        with self.assertRaises(ValueError):
            gallery(self.root, exhibits=0)


if __name__ == "__main__":
    unittest.main()
