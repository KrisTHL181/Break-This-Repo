import unittest
from pathlib import Path
from unittest.mock import Mock

from PIL import Image
from controller import PlanTracker
from motion_guard import matches_state
from recognition import Recognizer, fingerprint
from solver import solve

FIXTURE=Path(__file__).resolve().parents[1]/'examples/level25'


class BeachRecognitionTests(unittest.TestCase):
    def test_failed_beach_frame_has_complete_plan_and_click_preflight(self):
        board=Recognizer().read(FIXTURE/'initial.png')
        self.assertEqual(sum(p['kind']=='pig' for p in board['objects']),93)
        self.assertEqual(sum(p['kind']=='duck' for p in board['objects']),11)
        self.assertEqual(sum(p.get('length')==3 for p in board['objects']),2)
        plan=solve(board['objects'])
        self.assertEqual(plan['states'][-1],[])
        self.assertEqual(sum(a['type']=='exit' for a in plan['actions']),93)
        self.assertEqual(sum(len(a['duck_exits']) for a in plan['actions']),11)
        self.assertTrue(matches_state(Image.open(FIXTURE/'initial.png'),plan['states'][0],board['grid']))

    def test_animation_sequence_keeps_board_and_cached_plan(self):
        r=Recognizer()
        first=r.read(FIXTURE/'initial.png')
        solve_fn=Mock(wraps=solve)
        tracker=PlanTracker(solve_fn=solve_fn)
        tracker.observe(first)
        plan=tracker.plan
        for path in sorted(FIXTURE.glob('frame-*.png')):
            with self.subTest(frame=path.name):
                board=r.read(path)
                self.assertEqual(fingerprint(board['objects']),fingerprint(first['objects']))
                self.assertTrue(matches_state(Image.open(path),plan['states'][0],tracker.grid))
                self.assertEqual(tracker.observe(board)['step'],1)
                self.assertIs(tracker.plan,plan)
        self.assertEqual(solve_fn.call_count,1)


if __name__=='__main__':unittest.main()
