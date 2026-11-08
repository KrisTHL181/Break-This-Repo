import unittest
from unittest.mock import patch

import test_meow_museum as fixtures
from repo_atlas import atlas, render_atlas, tree_entries


class AtlasTest(unittest.TestCase):
    setUp = fixtures.MuseumTest.setUp
    git = fixtures.MuseumTest.git
    commit = fixtures.MuseumTest.commit

    def seed(self):
        (self.root / "src").mkdir()
        (self.root / "src" / "a.py").write_bytes(b"1234")
        (self.root / "src" / "b.py").write_bytes(b"1234")
        (self.root / "src" / "alias").symlink_to("/missing/external")
        (self.root / "root.txt").write_bytes(b"hi")
        return self.commit("specimens")

    def test_counts_bytes_duplicates_links_and_missing_checkout(self):
        sha = self.seed()
        (self.root / "src" / "a.py").unlink()
        report = atlas(self.root, sha)
        group = next(g for g in report["groups"] if g["name"] == "src")
        self.assertEqual((group["files"], group["bytes"], group["symlinks"], group["unknown"]), (2, 8, 1, 0))
        self.assertEqual(group["types"], {".py": 2})
        self.assertEqual(report["entries"], 4)
        self.assertEqual((report["duplicate_objects"], report["duplicate_path_references"],
                          report["duplicate_logical_bytes"]), (1, 1, 4))
        self.assertEqual(report["largest_files"][0]["bytes"], 4)
        self.assertIn("重复对象", render_atlas(report))

    def test_literal_path_prefix_scopes_a_large_tree(self):
        (self.root / "literal[1]").mkdir()
        (self.root / "literal[1]" / "one.txt").write_text("1")
        (self.root / "literal1").mkdir()
        (self.root / "literal1" / "two.txt").write_text("22")
        sha = self.commit("literal prefix specimens")

        report = atlas(self.root, sha, path_prefix="literal[1]")

        self.assertEqual(report["entries"], 1)
        self.assertEqual(report["path_prefix"], "literal[1]")
        self.assertEqual(report["groups"][0]["name"], "literal[1]")
        self.assertEqual(atlas(self.root, sha, path_prefix="literal[1]/one.txt")["entries"], 1)
        with self.assertRaises(ValueError):
            atlas(self.root, sha, path_prefix="../outside")

    def test_unknown_sizes_and_limits_are_explicit(self):
        sha = self.seed()
        with patch("repo_atlas.local_sizes", return_value={}):
            report = atlas(self.root, sha)
        self.assertEqual(sum(g["unknown"] for g in report["groups"]), 3)
        self.assertIn("体积未知", render_atlas(report))
        entries, truncated = tree_entries(self.root, sha, 2)
        self.assertEqual(len(entries), 2)
        self.assertTrue(truncated)
        self.assertFalse(tree_entries(self.root, sha, 4)[1])
        with self.assertRaises(ValueError):
            tree_entries(self.root, sha, 0)

    def test_submodules_not_traversed_and_names_escaped(self):
        sha = self.seed()
        self.git("update-index", "--add", "--cacheinfo", f"160000,{sha},vendor/core")
        self.git("-c", "core.hooksPath=/dev/null", "commit", "-qm", "gitlink")
        report = atlas(self.root)
        group = next(g for g in report["groups"] if g["name"] == "vendor")
        self.assertEqual((group["submodules"], group["files"]), (1, 0))
        group["name"] = "<script>alert(1)</script>"
        self.assertNotIn("<script>alert", render_atlas(report))
        report["largest_files"] = [{"path": "name\u202e.txt", "bytes": 1, "oid": sha}]
        self.assertIn("\\u202e", render_atlas(report))


if __name__ == "__main__":
    unittest.main()
