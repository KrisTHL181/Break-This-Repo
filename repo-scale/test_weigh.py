#!/usr/bin/env python3
"""Offline tests for weigh.py.

No network, no pytest, no fixtures on disk.  The tree graph below is the whole
world these tests live in, which is the point: the weigher's job is arithmetic
over tree objects, and that arithmetic is testable without GitHub.

    python3 -m unittest discover -v
    python3 test_weigh.py
"""

import unittest

from weigh import (
    FakeTransport,
    GITLINK,
    LINK,
    BLOB,
    TreeCache,
    Weigher,
    WeighError,
    bar,
    human,
    split_repo,
)

# --------------------------------------------------------------------------
# a synthetic repository
#
#   ROOT/                       (recursive listing TRUNCATED by the fake API)
#     big/                      (recursive listing fine)
#       sub/
#         deep.bin    50 B
#       p            100 B
#       q            900 B
#     small/
#       b              5 B
#     a                 10 B
#     link -> /etc      symlink, 4 B of path text
#     vendor            gitlink (submodule)
# --------------------------------------------------------------------------

TREES = {
    "ROOT": {
        "entries": [
            {"path": "big", "type": "tree", "mode": "040000", "sha": "BIG"},
            {"path": "small", "type": "tree", "mode": "040000", "sha": "SMALL"},
            {"path": "a", "type": "blob", "mode": BLOB, "size": 10},
            {"path": "link", "type": "blob", "mode": LINK, "size": 4},
            {"path": "vendor", "type": "commit", "mode": GITLINK, "sha": "V"},
        ],
        "recursive": [
            {"path": "big", "type": "tree", "mode": "040000", "sha": "BIG"},
        ],
        "truncated": True,
    },
    "BIG": {
        "entries": [
            {"path": "sub", "type": "tree", "mode": "040000", "sha": "SUB"},
            {"path": "p", "type": "blob", "mode": BLOB, "size": 100},
            {"path": "q", "type": "blob", "mode": BLOB, "size": 900},
        ],
        "recursive": [
            {"path": "sub", "type": "tree", "mode": "040000", "sha": "SUB"},
            {"path": "sub/deep.bin", "type": "blob", "mode": BLOB, "size": 50},
            {"path": "p", "type": "blob", "mode": BLOB, "size": 100},
            {"path": "q", "type": "blob", "mode": BLOB, "size": 900},
        ],
        "truncated": False,
    },
    "SUB": {"entries": [{"path": "deep.bin", "type": "blob", "mode": BLOB, "size": 50}]},
    "SMALL": {"entries": [{"path": "b", "type": "blob", "mode": BLOB, "size": 5}]},
    # A later commit that touches only `small/`: ROOT's other children keep
    # their SHAs, so the cache must reuse them instead of re-downloading.
    "ROOT2": {
        "entries": [
            {"path": "big", "type": "tree", "mode": "040000", "sha": "BIG"},
            {"path": "small", "type": "tree", "mode": "040000", "sha": "SMALL2"},
            {"path": "a", "type": "blob", "mode": BLOB, "size": 10},
            {"path": "link", "type": "blob", "mode": LINK, "size": 4},
            {"path": "vendor", "type": "commit", "mode": GITLINK, "sha": "V"},
        ],
        "recursive": [],
        "truncated": True,
    },
    "SMALL2": {
        "entries": [
            {"path": "b", "type": "blob", "mode": BLOB, "size": 5},
            {"path": "c", "type": "blob", "mode": BLOB, "size": 500},
        ]
    },
    "EMPTY": {"entries": []},
}

BIG_BYTES = 50 + 100 + 900
TOTAL_BYTES = 10 + BIG_BYTES + 5
TOTAL_BYTES_2 = 10 + BIG_BYTES + 5 + 500


def commit(sha, tree, date, message="msg", parents=()):
    return {
        "sha": sha,
        "parents": [{"sha": p} for p in parents],
        "commit": {
            "tree": {"sha": tree},
            "message": message,
            "committer": {"date": date},
        },
    }


COMMITS = {
    "main": commit("C2", "ROOT2", "2026-09-22T00:00:00Z", "newest", parents=("C1",)),
    "C2": commit("C2", "ROOT2", "2026-09-22T00:00:00Z", "newest", parents=("C1",)),
    "C1": commit("C1", "ROOT", "2026-09-21T00:00:00Z", "older", parents=()),
    "empty": commit("C0", "EMPTY", "2026-09-20T00:00:00Z", "empty"),
}

COMMIT_LISTS = {
    "main": [COMMITS["C2"], COMMITS["C1"]],
}


def build():
    t = FakeTransport(TREES, COMMITS, COMMIT_LISTS)
    c = TreeCache(None)
    return t, c, Weigher("fake", "fake", t, c)


class TestAggregation(unittest.TestCase):
    def test_exact_totals_across_a_truncated_root(self):
        _, _, w = build()
        row = w.measure_ref("C1")
        self.assertEqual(row["bytes"], TOTAL_BYTES)
        self.assertEqual(row["files"], 5)     # a, big/p, big/q, big/sub/deep.bin, small/b
        self.assertEqual(row["dirs"], 3)      # big, big/sub, small
        self.assertEqual(row["symlinks"], 1)  # link
        self.assertEqual(row["submodules"], 1)
        self.assertTrue(row["exact"])

    def test_symlink_bytes_are_not_counted_as_tree_weight(self):
        _, _, w = build()
        row = w.measure_ref("C1")
        # The 4-byte target string lives in a blob; counting it would make the
        # number depend on path length rather than on content.
        self.assertEqual(row["bytes"], 10 + BIG_BYTES + 5)
        self.assertNotIn(4, [row["bytes"]])

    def test_truncated_root_is_split_exactly_once(self):
        _, _, w = build()
        w.measure_ref("C1")
        self.assertEqual(w.split_trees, 1)

    def test_empty_tree_is_zero_not_an_error(self):
        _, _, w = build()
        row = w.measure_ref("empty")
        self.assertEqual((row["bytes"], row["files"], row["dirs"]), (0, 0, 0))

    def test_missing_tree_raises(self):
        t, c, w = build()
        t.trees = dict(TREES)
        with self.assertRaises(Exception):
            w.measure_tree("NOPE")


class TestCache(unittest.TestCase):
    def test_second_measure_reuses_every_subtree(self):
        t, _, w = build()
        w.measure_ref("C1")
        before = t.calls
        w.measure_ref("C1")
        # Only the commit lookup should cost anything the second time.
        self.assertEqual(t.calls - before, 1)

    def test_only_the_changed_subtree_is_refetched(self):
        t, _, w = build()
        w.measure_ref("C1")
        before = t.calls
        row = w.measure_ref("C2")
        # C2 differs only under small/, and ROOT2's own listing is truncated so
        # it must be split again: root + small.  `big/` must come from cache.
        self.assertLessEqual(t.calls - before, 4)
        self.assertEqual(row["bytes"], TOTAL_BYTES_2)
        self.assertNotIn("tree:BIG:True", t.requests[before:])

    def test_known_truncating_tree_skips_the_doomed_recursive_call(self):
        t, c, w = build()
        w.measure_ref("C1")
        self.assertTrue(c.known_truncating("ROOT"))
        t.requests.clear()
        w.measure_tree("ROOT")
        self.assertNotIn("tree:ROOT:True", t.requests)

    def test_cache_survives_a_round_trip(self):
        import json
        import os
        import tempfile

        t, _, w = build()
        w.measure_ref("C1")
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "cache.json")
            disk = TreeCache(path)
            disk.data = w.cache.data
            disk.dirty = True
            disk.save()

            warmed = TreeCache(path)
            t2 = FakeTransport(TREES, COMMITS, COMMIT_LISTS)
            w2 = Weigher("fake", "fake", t2, warmed)
            row = w2.measure_ref("C1")
            self.assertEqual(row["bytes"], TOTAL_BYTES)
            # Everything under the root came back; only the commit lookup and
            # the root split were needed.
            self.assertLessEqual(t2.calls, 3)
            with open(path, encoding="utf-8") as fh:
                self.assertTrue(json.load(fh)["v"])

class TestBlobWalk(unittest.TestCase):
    def test_paths_are_flattened_under_their_directories(self):
        _, _, w = build()
        paths = sorted(b["path"] for b in w.collect_blobs("C1"))
        self.assertEqual(
            paths,
            ["a", "big/p", "big/q", "big/sub/deep.bin", "link", "small/b"])

    def test_sizes_survive_the_flattening(self):
        _, _, w = build()
        got = {b["path"]: b["bytes"] for b in w.collect_blobs("C1")}
        self.assertEqual(got["big/q"], 900)
        self.assertEqual(got["big/sub/deep.bin"], 50)
        self.assertEqual(got["link"], 4)


class TestHistory(unittest.TestCase):
    def test_first_parent_chain_follows_parent_zero(self):
        _, _, w = build()
        chain = w.first_parent_chain("main", limit=10)
        self.assertEqual([c["sha"] for c in chain], ["C2", "C1"])

    def test_series_is_chronological_and_measures_each_point(self):
        _, _, w = build()
        rows = w.series("main", points=5, pool=10)
        self.assertEqual([r["sha"] for r in rows], ["C1", "C2"])
        self.assertEqual(rows[0]["bytes"], TOTAL_BYTES)
        self.assertEqual(rows[1]["bytes"], TOTAL_BYTES_2)

    def test_series_never_returns_more_points_than_history(self):
        _, _, w = build()
        self.assertEqual(len(w.series("main", points=99, pool=10)), 2)

    def test_milestones_carry_their_labels(self):
        _, _, w = build()
        rows = w.milestones(["before=C1", "after=C2"])
        self.assertEqual([r["label"] for r in rows], ["before", "after"])
        self.assertEqual(rows[1]["bytes"] - rows[0]["bytes"], 500)

    def test_milestone_accepts_a_colon_separator(self):
        _, _, w = build()
        self.assertEqual([r["label"] for r in w.milestones(["x:C1"])], ["x"])

    def test_milestone_without_a_label_is_rejected(self):
        _, _, w = build()
        with self.assertRaises(WeighError):
            w.milestones(["C1"])


class TestBreakdown(unittest.TestCase):
    def test_root_entries_are_ranked_by_weight(self):
        _, _, w = build()
        rows = w.breakdown("C1")
        self.assertEqual([r["name"] for r in rows], ["big/", "a", "small/"])
        self.assertEqual(rows[0]["bytes"], BIG_BYTES)
        self.assertEqual(rows[0]["files"], 3)
        self.assertEqual(rows[0]["dirs"], 2)  # itself plus sub/

    def test_breakdown_reconciles_with_the_headline_total(self):
        _, _, w = build()
        total = w.measure_ref("C1")["bytes"]
        self.assertEqual(sum(r["bytes"] for r in w.breakdown("C1")), total)

    def test_symlinks_are_left_out_of_the_ranking(self):
        _, _, w = build()
        self.assertNotIn("link", [r["name"] for r in w.breakdown("C1")])


class TestFormatting(unittest.TestCase):
    def test_human_units(self):
        self.assertEqual(human(0), "0 B")
        self.assertEqual(human(999), "999 B")
        self.assertEqual(human(2048), "2.0 KiB")
        self.assertEqual(human(5 * 1024 ** 2), "5.0 MiB")
        self.assertEqual(human(3 * 1024 ** 3), "3.00 GiB")

    def test_bar_is_safe_and_never_invisible(self):
        self.assertEqual(bar(0, 0), "")
        self.assertEqual(len(bar(1, 10 ** 12, width=44)), 1)
        self.assertEqual(len(bar(10, 10, width=44)), 44)


class TestRepoSpec(unittest.TestCase):
    def test_plain_slug(self):
        self.assertEqual(split_repo("owner/repo"), ("owner", "repo"))

    def test_url_and_trailing_slash(self):
        self.assertEqual(split_repo("https://github.com/owner/repo/"), ("owner", "repo"))

    def test_garbage_is_rejected(self):
        with self.assertRaises(WeighError):
            split_repo("just-a-name")
        with self.assertRaises(WeighError):
            split_repo("/")


class TestTruncationInDepth(unittest.TestCase):
    """GitHub truncates at one level; deeper trees must still add up exactly."""

    TREES = {
        "R": {
            "entries": [{"path": "outer", "type": "tree", "mode": "040000", "sha": "O"}],
            "recursive": [{"path": "outer", "type": "tree", "mode": "040000", "sha": "O"}],
            "truncated": True,
        },
        "O": {
            "entries": [
                {"path": "inner", "type": "tree", "mode": "040000", "sha": "I"},
                {"path": "loose", "type": "blob", "mode": BLOB, "size": 7},
            ],
            "recursive": [],
            "truncated": True,
        },
        "I": {
            "entries": [
                {"path": "x", "type": "blob", "mode": BLOB, "size": 1000},
                {"path": "y", "type": "blob", "mode": BLOB, "size": 2000},
            ]
        },
    }

    def test_two_levels_of_truncation_are_split_twice(self):
        t = FakeTransport(self.TREES, {}, {})
        w = Weigher("fake", "fake", t, TreeCache(None))
        agg = w.measure_tree("R")
        self.assertEqual(agg["b"], 3007)
        self.assertEqual(agg["f"], 3)
        self.assertEqual(agg["d"], 2)  # outer, outer/inner
        self.assertEqual(w.split_trees, 2)
        self.assertEqual(w.inexact_trees, 0)

    def test_a_truncated_direct_listing_is_reported_as_inexact(self):
        trees = {
            "R": dict(self.TREES["R"]),
            "O": {
                "entries": [{"path": "loose", "type": "blob", "mode": BLOB, "size": 7}],
                # Both the recursive and the direct listing are cut, so no
                # amount of asking again can recover the total.
                "recursive": [],
                "truncated": True,
                "entries_truncated": True,
            },
        }
        w = Weigher("fake", "fake", FakeTransport(trees, {}, {}), TreeCache(None))
        agg = w.measure_tree("R")
        self.assertEqual(agg["b"], 7)
        self.assertTrue(agg.get("inexact"))
        self.assertEqual(w.inexact_trees, 1)

    def test_parallel_and_serial_agree(self):
        trees = {
            "R": {
                "entries": [
                    {"path": f"d{i}", "type": "tree", "mode": "040000", "sha": f"D{i}"}
                    for i in range(12)
                ],
                "recursive": [],
                "truncated": True,
            }
        }
        for i in range(12):
            trees[f"D{i}"] = {
                "entries": [{"path": "f", "type": "blob", "mode": BLOB, "size": 10 * (i + 1)}]
            }
        serial = Weigher("fake", "fake", FakeTransport(trees, {}, {}),
                         TreeCache(None), jobs=1).measure_tree("R")
        parallel = Weigher("fake", "fake", FakeTransport(trees, {}, {}),
                           TreeCache(None), jobs=6).measure_tree("R")
        self.assertEqual(serial["b"], parallel["b"])
        self.assertEqual(serial["f"], parallel["f"])
        self.assertEqual(serial["d"], parallel["d"])
        self.assertEqual(parallel["b"], sum(10 * (i + 1) for i in range(12)))


class TestSelfTestEntryPoint(unittest.TestCase):
    def test_builtin_self_test_passes(self):
        import weigh
        self.assertEqual(weigh._self_test(), 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
