import json
from pathlib import Path
import unittest

from controller import PlanTracker, screen_point
from recognition import Recognizer
from visual_trace import TraceRecorder
from solver import occupied_cells, predict, settle_ducks, solve, validate

HERE = Path(__file__).resolve().parents[1]


class GuideTests(unittest.TestCase):
    def test_first_recognized_frame_starts_without_stability_wait(self):
        board = {"objects":[{"id":"a","kind":"pig","d":"E","u":1,"v":0}],
                 "grid":{"x":{"origin":40,"scale":13},"y":{"origin":200,"scale":13}}}
        tracker = PlanTracker()
        self.assertEqual(tracker.observe(board)["step"], 1)
        self.assertEqual(tracker.revision, 1)

    def test_three_adjacent_pigs_hint_offset_and_branch_retries(self):
        board = Recognizer().read(HERE/"examples/level14_regression.png")
        self.assertEqual(sum(p["kind"] == "pig" for p in board["objects"]), 95)
        self.assertEqual(sum(p["kind"] == "duck" for p in board["objects"]), 9)
        self.assertEqual(sum(p.get("length") == 3 for p in board["objects"]), 4)
        self.assertTrue(any(p.get("hint_animation") for p in board["objects"]))
        self.assertEqual(solve(board["objects"])["states"][-1], [])

    def test_larger_sprites_and_dense_board_search(self):
        board = Recognizer().read(HERE/"examples/level13_regression.png")
        self.assertEqual(len(board["objects"]), 78)
        self.assertEqual(sum(p.get("length") == 3 for p in board["objects"]), 6)
        trace = TraceRecorder()
        plan = solve(board["objects"], observer=trace.record)
        self.assertGreaterEqual(len(plan["actions"]), 78)
        self.assertEqual(plan["states"][-1], [])

    def test_long_pigs_screen_and_full_replay(self):
        board = Recognizer().read(HERE/"examples/level11_long_regression.png")
        pigs = [p for p in board["objects"] if p["kind"] == "pig"]
        self.assertEqual(len(pigs), 90)
        self.assertEqual(sum(p["kind"] == "duck" for p in board["objects"]), 18)
        long = [p for p in pigs if p["length"] == 3]
        self.assertEqual(sorted(p["d"] for p in long), ["E", "N", "S", "S", "W"])
        self.assertEqual(sum(len(occupied_cells(p)) for p in pigs), 185)
        self.assertEqual(solve(board["objects"])["states"][-1], [])

    def test_long_pig_tail_blocks_and_stops_at_correct_cell(self):
        long = {"id":"long", "kind":"pig", "d":"N", "length":3, "u":6, "v":2}
        mover = {"id":"mover", "kind":"pig", "d":"E", "u":1, "v":4}
        validate([long, mover])
        self.assertEqual(occupied_cells(long), ((3, 0), (3, 1), (3, 2)))
        action = predict([long, mover], mover)
        self.assertEqual(action["type"], "slide")
        self.assertEqual(action["to"], [3, 4])
        # Shortening the same centre to one cell removes the tail obstruction.
        self.assertEqual(predict([{**long, "length":1}, mover], mover)["type"], "exit")

    def test_visual_trace_preserves_solution_and_records_real_search(self):
        board = json.loads((HERE/"examples/level6_recognized.json").read_text())
        baseline = solve(board["objects"])
        trace = TraceRecorder()
        instrumented = solve(board["objects"], observer=trace.record)
        self.assertEqual(instrumented["actions"], baseline["actions"])
        events = trace.snapshot()["events"]
        self.assertEqual(events[0]["phase"], "begin")
        self.assertEqual(events[-1]["phase"], "done")
        self.assertTrue(any(e["phase"] == "branch" for e in events))
        self.assertTrue(any(e["phase"] == "replay" for e in events))
        self.assertTrue(all(a["elapsed_ms"] <= b["elapsed_ms"] for a, b in zip(events, events[1:])))

    def test_trace_is_bounded_and_does_not_advance_guide(self):
        trace = TraceRecorder(limit=3)
        for i in range(10):
            trace.record({"phase": "visit", "state": [], "elapsed_ms": i})
        snap = trace.snapshot()
        self.assertEqual(len(snap["events"]), 3)
        self.assertEqual(snap["total"], 10)
        self.assertEqual(snap["index"], 0)

    def test_new_level_starts_automatically(self):
        grid = {"x": {"origin": 40, "scale": 13}, "y": {"origin": 200, "scale": 13}}
        first = {"objects": [{"id": "a", "kind": "pig", "d": "E", "u": 1, "v": 0}], "grid": grid}
        tracker = PlanTracker()
        for _ in range(3):
            tracker.observe(first)
        tracker.observe({"objects": [], "grid": grid})
        second = {"objects": [{"id": str(i), "kind": "pig", "d": "W", "u": 3, "v": i*4} for i in range(3)], "grid": grid}
        for _ in range(3):
            marker = tracker.observe(second)
        self.assertEqual(marker["step"], 1)
        self.assertEqual(marker["total"], 3)
        self.assertEqual(tracker.revision, 2)

    def test_new_level_duck_with_different_background(self):
        board = Recognizer().read(HERE/"examples/level7_regression.png")
        self.assertEqual(sum(p["kind"] == "pig" for p in board["objects"]), 90)
        self.assertEqual(sum(p["kind"] == "duck" for p in board["objects"]), 14)
        plan = solve(board["objects"])
        self.assertEqual(plan["states"][-1], [])

    def test_single_pig_can_be_recognized_and_solved(self):
        from PIL import Image
        r = Recognizer()
        image = Image.new("RGB", (379, 835), (150, 220, 50))
        image.paste(Image.fromarray(r.templates["pig_E"][0]), (174, 388))
        board = r.read(image)
        self.assertEqual(len(board["objects"]), 1)
        self.assertEqual(board["objects"][0]["d"], "E")
        self.assertEqual(len(solve(board["objects"])["actions"]), 1)

    def test_current_full_plan_replay(self):
        board = json.loads((HERE/"examples/level6_recognized.json").read_text())
        plan = solve(board["objects"])
        self.assertEqual(len(board["objects"]), 100)
        self.assertGreaterEqual(len(plan["actions"]), 91)
        self.assertEqual(plan["states"][-1], [])
        self.assertEqual(sum(a["type"] == "exit" for a in plan["actions"]), 91)
        self.assertEqual(sum(len(a["duck_exits"]) for a in plan["actions"]), 9)

    def test_marker_waits_for_user_then_advances(self):
        pieces = [{"id": "a", "kind": "pig", "d": "E", "u": 1, "v": 0},
                  {"id": "b", "kind": "pig", "d": "W", "u": 1, "v": 4}]
        grid = {"x": {"origin": 40, "scale": 13}, "y": {"origin": 200, "scale": 13}}
        board = {"objects": pieces, "grid": grid}
        tracker = PlanTracker()
        for _ in range(3):
            marker = tracker.observe(board)
        self.assertEqual(marker["step"], 1)
        for _ in range(10):
            self.assertEqual(tracker.observe(board)["step"], 1)
        after = {"objects": tracker.plan["states"][1], "grid": grid}
        self.assertEqual(tracker.observe(after)["step"], 2)

    def test_slide_state_advances_without_disappearance(self):
        board = json.loads((HERE/"examples/level6_recognized.json").read_text())
        plan = solve(board["objects"])
        i = next(i for i, a in enumerate(plan["actions"]) if a["type"] == "slide")
        tracker = PlanTracker()
        tracker.plan, tracker.grid, tracker.index = plan, board["grid"], i
        before = {"objects": plan["states"][i], "grid": board["grid"]}
        after = {"objects": plan["states"][i+1], "grid": board["grid"]}
        self.assertEqual(tracker.observe(before)["step"], i+1)
        self.assertEqual(tracker.observe(after)["step"], i+2)

    def test_click_through_coordinate_mapping(self):
        mark = {"x": 100, "y": 200}
        self.assertEqual(screen_point(mark, {"x": 1200, "y": 250, "width": 379, "height": 835}), (1300, 450))
        self.assertEqual(screen_point(mark, {"x": 500, "y": 100, "width": 758, "height": 1670}), (700, 500))

    def test_ducks_release_through_opening(self):
        ducks = [{"id": str(i), "kind": "duck", "d": "", "u": u, "v": v}
                 for i, (u, v) in enumerate([(0, 0), (0, 2), (2, 0), (2, 2)])]
        remaining, escaped = settle_ducks(ducks)
        self.assertEqual(remaining, [])
        self.assertEqual(len(escaped), 4)


if __name__ == "__main__":
    unittest.main()
