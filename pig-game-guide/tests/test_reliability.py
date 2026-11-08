import threading
import time
import unittest
from unittest.mock import Mock
from controller import PlanTracker
from solve_service import SolveService,SolveCancelled

B={'objects':[{'id':'a','kind':'pig','d':'E','u':1,'v':0}],
   'grid':{'x':{'origin':50,'scale':13},'y':{'origin':300,'scale':13}}}

class ReliabilityTests(unittest.TestCase):
    def test_mixed_orientation_cluster_is_not_sliced_into_horizontal_pigs(self):
        from pathlib import Path
        from recognition import Recognizer
        board=Recognizer().read(Path(__file__).resolve().parents[1]/'examples/level17_cluster_regression.png')
        cluster=[p for p in board['objects'] if 87<p['x']<135 and 250<p['y']<290]
        self.assertEqual(len(cluster),2)
        self.assertTrue(all(p['d'] in 'NS' for p in cluster))

    def test_failed_board_does_not_retry_forever(self):
        solve=Mock(side_effect=TimeoutError('time limit'))
        tracker=PlanTracker(solve_fn=solve)
        with self.assertRaises(TimeoutError):tracker.observe(B)
        for _ in range(10):self.assertIsNone(tracker.observe(B))
        self.assertEqual(solve.call_count,1)
        tracker.reset()
        with self.assertRaises(TimeoutError):tracker.observe(B)
        self.assertEqual(solve.call_count,2)

    def test_transient_mismatch_keeps_the_same_plan(self):
        tracker=PlanTracker();tracker.observe(B);plan=tracker.plan
        other={**B,'objects':[{**B['objects'][0],'u':3}]}
        self.assertIsNone(tracker.observe(other));self.assertIs(tracker.plan,plan)
        self.assertEqual(tracker.observe(B)['step'],1);self.assertEqual(tracker.revision,1)

    def test_child_process_solve(self):
        service=SolveService();plan=service.solve(B['objects'])
        self.assertEqual(plan['states'][-1],[]);self.assertIsNone(service.process)

    def test_child_process_cancel_does_not_wait_for_search(self):
        import json
        from pathlib import Path
        board=json.loads((Path(__file__).resolve().parents[1]/'examples/level14/board.json').read_text())
        service=SolveService();result=[]
        def run():
            try:service.solve(board['objects']);result.append('done')
            except SolveCancelled:result.append('cancelled')
        t=threading.Thread(target=run);t.start()
        deadline=time.monotonic()+2
        while service.process is None and time.monotonic()<deadline:time.sleep(.005)
        start=time.monotonic();service.cancel();t.join(2)
        self.assertFalse(t.is_alive());self.assertLess(time.monotonic()-start,2)
        self.assertEqual(result,['cancelled'])

if __name__=='__main__':unittest.main()
