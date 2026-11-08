"""Cross-check optimized geometry against the independent public simulator."""
import json
import random
import unittest
from functools import lru_cache
from pathlib import Path

from search_engine import BitBoard
from solver import apply_action, occupied_cells, predict, settle_ducks, solve, validate

HERE = Path(__file__).resolve().parents[1]


class SearchEngineTests(unittest.TestCase):
    def test_level17_full_solution_within_default_budget(self):
        objects = json.loads((HERE/'examples/level17_recognized.json').read_text())['objects']
        plan = solve(objects)
        self.assertEqual(plan['states'][-1], [])
        self.assertEqual(sum(a['type'] == 'exit' for a in plan['actions']), 94)
        self.assertGreater(plan['deadlock_prunes'], 0)
        self.assertLess(plan['expanded'], 3000)

    def test_gapped_cycle_is_impossible_before_every_pig_is_blocked(self):
        objects = [
            {'id':'a', 'kind':'pig', 'd':'E', 'u':7, 'v':30},
            {'id':'b', 'kind':'pig', 'd':'S', 'u':12, 'v':31},
            {'id':'c', 'kind':'pig', 'd':'W', 'u':11, 'v':34},
            {'id':'d', 'kind':'pig', 'd':'N', 'u':8, 'v':33},
        ]
        validate(objects)
        self.assertEqual(predict(objects, objects[0])['type'], 'slide')
        board = BitBoard(objects)
        self.assertTrue(board.deadlocked(board.initial, board.occupancy(board.initial)))
        # Removing the north-facing pig breaks the obstruction; never prune it.
        board = BitBoard(objects[:-1])
        self.assertFalse(board.deadlocked(board.initial, board.occupancy(board.initial)))
        self.assertEqual(solve(objects[:-1])['states'][-1], [])

    def test_compact_positions_and_duck_cascades_match_reference(self):
        objects = json.loads((HERE/'examples/level17_recognized.json').read_text())['objects']
        board = BitBoard(objects)
        for seed in range(12):
            rng = random.Random(seed)
            state, bits, ducks = board.settle(board.initial, board.occupancy(board.initial))
            expected, gone = settle_ducks(objects)
            self.assertEqual(board.decode(state), expected)
            self.assertEqual(sorted(objects[i]['id'] for i in ducks), sorted(gone))
            for _ in range(160):
                choices = [i for i in board.pigs if state[i] >= 0 and board.move(state, bits, i) != state[i]]
                if not choices:
                    break
                i = rng.choice(choices)
                current = next(p for p in expected if p['id'] == objects[i]['id'])
                expected, gone = apply_action(expected, predict(expected, current))
                state, bits, ducks = board.apply(state, bits, i, board.move(state, bits, i))
                self.assertEqual(board.decode(state), expected)
                self.assertEqual(bits, board.occupancy(state))
                self.assertEqual(sorted(objects[j]['id'] for j in ducks), sorted(gone))

    def test_no_solvable_small_board_is_pruned(self):
        rng = random.Random(81)
        for case in range(100):
            objects, occupied = [], set()
            for i in range(6):
                for _ in range(30):
                    duck = rng.random() < .2
                    length = 1 if duck else rng.choice((2, 3))
                    d = '' if duck else rng.choice('NSEW')
                    x, y = rng.randrange(-2, 4), rng.randrange(-2, 4)
                    p = {'id':str(i), 'kind':'duck' if duck else 'pig', 'd':d, 'length':length,
                         'u':2*x+(length-1 if d in ('E', 'W') else 0),
                         'v':2*y+(length-1 if d in ('N', 'S') else 0)}
                    cells = set(occupied_cells(p))
                    if not cells & occupied:
                        objects.append(p)
                        occupied |= cells
                        break
            validate(objects)

            @lru_cache(None)
            def solvable(key):
                state = [{**objects[i], 'u':u, 'v':v} for i,u,v in key]
                if not state:
                    return True
                for p in state:
                    if p['kind'] != 'pig':
                        continue
                    action = predict(state, p)
                    if action['type'] == 'blocked':
                        continue
                    after, _ = apply_action(state, action)
                    after_key = tuple((int(p['id']), p['u'], p['v']) for p in after)
                    if solvable(after_key):
                        return True
                return False

            state, _ = settle_ducks(objects)
            key = tuple((int(p['id']), p['u'], p['v']) for p in state)
            if solvable(key):
                board = BitBoard(state)
                self.assertFalse(board.deadlocked(board.initial, board.occupancy(board.initial)), case)

    def test_empty_board_and_limits(self):
        self.assertEqual(solve([])['states'], [[]])
        with self.assertRaisesRegex(TimeoutError, '上限'):
            solve([{'id':'a', 'kind':'pig', 'd':'E', 'u':1, 'v':0}], max_nodes=0)


if __name__ == '__main__':
    unittest.main()
