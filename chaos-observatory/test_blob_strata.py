import unittest
from unittest.mock import patch

import test_meow_museum as fixtures
from blob_strata import report_range, render_report


class BlobStrataTest(unittest.TestCase):
    setUp = fixtures.MuseumTest.setUp
    git = fixtures.MuseumTest.git
    commit = fixtures.MuseumTest.commit

    def test_range_counts_unique_new_blobs_without_reading_contents(self):
        (self.root / "old.txt").write_bytes(b"old object")
        start = self.commit("base")
        (self.root / "new-a.bin").write_bytes(b"same payload")
        (self.root / "new-b.bin").write_bytes(b"same payload")
        end = self.commit("duplicate new files")

        report = report_range(self.root, start, end)
        page = render_report(report)

        self.assertEqual(report["known_unique_blobs"], 1)
        self.assertEqual(report["known_unique_blob_bytes"], len(b"same payload"))
        self.assertEqual(report["unknown_objects"], 0)
        self.assertNotIn("new-a.bin", page)
        self.assertNotIn("same payload", page)
        self.assertIn("只使用本地对象和 Git 元数据", page)
        self.assertEqual(report_range(self.root, end, end)["known_unique_blobs"], 0)

    def test_missing_objects_are_explicitly_unknown(self):
        (self.root / "base.txt").write_text("base")
        start = self.commit("base")
        (self.root / "end.txt").write_text("end")
        end = self.commit("end")
        available = self.git("hash-object", "-w", "--stdin")
        missing_from_listing = "a" * 40
        missing_from_batch = "b" * 40

        def fake_git_metadata(_root, *args, input_data=b""):
            if args == ("rev-parse", "--is-shallow-repository"):
                return b"false\n"
            if args[0] == "rev-list":
                return f"{available}\n{missing_from_batch}\n?{missing_from_listing}\n".encode()
            if args[0] == "cat-file":
                return (f"{available} blob 7\n{missing_from_batch} missing\n".encode())
            raise AssertionError(args)

        with patch("blob_strata.git_metadata", side_effect=fake_git_metadata):
            report = report_range(self.root, start, end)

        self.assertEqual(report["known_unique_blobs"], 1)
        self.assertEqual(report["known_unique_blob_bytes"], 7)
        self.assertEqual(report["unknown_objects"], 2)
        self.assertNotIn(missing_from_listing, str(report))

    def test_shallow_ranges_and_invalid_top_limit_fail(self):
        (self.root / "base.txt").write_text("base")
        start = self.commit("base")
        with patch("blob_strata.git_metadata", return_value=b"true\n"):
            with self.assertRaisesRegex(ValueError, "shallow clone"):
                report_range(self.root, start, start)
        with self.assertRaises(ValueError):
            report_range(self.root, start, start, top=501)


if __name__ == "__main__":
    unittest.main()
