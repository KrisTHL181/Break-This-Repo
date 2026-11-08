"""Lock rules, independent replay, real recognition and dispatch timing."""
import json
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch
from PIL import Image,ImageDraw
from auto_click import PlanPlayback
from grid_recognition import GridRecognizer
from lock_vision import LockReader,unlocked_visible
from motion_guard import matches_state
from recognition import RecognitionError,fingerprint
from search_engine import BitBoard
from solver import apply_action,predict,solve,validate
from test_auto_click import GRID,PIG,picture

HERE=Path(__file__).resolve().parents[1]

def pig(id,u,v,d='E',lock=0):
    p=dict(id=id,kind='pig',d=d,u=u,v=v)
    if lock:p['lock_remaining']=lock
    return p


class LockTests(unittest.TestCase):
    def test_locked_pig_is_solid_and_cannot_slide_or_exit(self):
        objects=[pig('a',1,0),pig('locked',7,0,lock=2)]
        self.assertEqual(predict(objects,objects[1])['reason'],'locked')
        self.assertEqual(predict(objects,objects[0])['to'],[3,0])
        board=BitBoard(objects);bits=board.occupancy(board.initial)
        self.assertEqual(board.move(board.initial,bits,1),board.initial[1])
        with self.assertRaisesRegex(ValueError,'locked'):apply_action(objects,{'id':'locked','type':'exit'})
        with self.assertRaisesRegex(ValueError,'locked'):board.apply(board.initial,bits,1,-1)

    def test_only_pig_exits_decrement_all_locks(self):
        objects=[pig('a',1,0),pig('stop',9,0,'W'),pig('locked',1,8,lock=2),
                 dict(id='duck',kind='duck',d='',u=20,v=20),
                 dict(id='e',kind='elephant',d='E',width=2,length=3,u=2,v=15)]
        after,ducks=apply_action(objects,predict(objects,objects[0]))
        self.assertEqual(ducks,['duck'])
        self.assertEqual(next(p for p in after if p['id']=='locked')['lock_remaining'],2)
        elephant=next(p for p in after if p['id']=='e')
        after,_=apply_action(after,predict(after,elephant))
        self.assertEqual(next(p for p in after if p['id']=='locked')['lock_remaining'],2)
        free=pig('free',1,24);after.append(free);after.append(pig('lock2',1,28,lock=1))
        after,_=apply_action(after,predict(after,free))
        self.assertEqual([p['lock_remaining'] for p in after if 'lock_remaining' in p],[1,0])

    def test_unlock_chain_and_independent_compact_replay_agree(self):
        objects=[pig('last',1,0,lock=2),pig('second',1,4,lock=1),pig('first',1,8)]
        plan=solve(objects)
        self.assertEqual([a['id'] for a in plan['actions']],['first','second','last'])
        self.assertEqual(plan['states'][-1],[])
        board=BitBoard(objects);state=board.initial;bits=board.occupancy(state)
        for before,after,action in zip(plan['states'],plan['states'][1:],plan['actions']):
            self.assertEqual(board.decode(state),before)
            i=next(i for i,p in enumerate(objects) if p['id']==action['id'])
            state,bits,_=board.apply(state,bits,i,board.move(state,bits,i))
            self.assertEqual(board.decode(state),after)
            self.assertEqual(bits,board.occupancy(state))

    def test_lock_does_not_cause_false_deadlock_prune(self):
        objects=[pig('locked',1,0,lock=1),pig('free',1,4)]
        b=BitBoard(objects)
        self.assertFalse(b.deadlocked(b.initial,b.occupancy(b.initial)))
        self.assertEqual(solve(objects)['states'][-1],[])
        with self.assertRaisesRegex(ValueError,'无解'):solve([pig('locked',1,0,lock=1)])

    def test_remaining_count_is_part_of_state_fingerprint(self):
        self.assertNotEqual(fingerprint([pig('a',1,0,lock=2)]),fingerprint([pig('a',1,0,lock=1)]))
        self.assertEqual(fingerprint([pig('a',1,0)]),fingerprint([dict(pig('a',1,0),lock_remaining=0)]))
        for bad in (-1,True,1.5,'2'):
            with self.subTest(bad=bad),self.assertRaises(ValueError):validate([dict(pig('a',1,0),lock_remaining=bad)])

    def test_real_locks_have_correct_counts_occupancy_and_full_solution(self):
        path=HERE/'examples/level31';image=Image.open(path/'initial.png')
        board=GridRecognizer().read(image);expected=json.loads((path/'board.json').read_text())
        self.assertEqual(fingerprint(board['objects']),fingerprint(expected['objects']))
        locks=[p for p in board['objects'] if p.get('lock_remaining')]
        self.assertEqual([(p['d'],p['length'],p['lock_remaining']) for p in locks],[('N',2,20),('N',2,35)])
        self.assertEqual(len(board['objects']),92)
        self.assertTrue(matches_state(image,board['objects'],board['grid']))
        plan=solve(board['objects']);self.assertEqual(plan['states'][-1],[])
        self.assertEqual(sum(a['type']=='exit' for a in plan['actions']),92)
        for p in locks:
            idx=next(i for i,a in enumerate(plan['actions']) if a['id']==p['id'])
            self.assertGreaterEqual(sum(a['type']=='exit' for a in plan['actions'][:idx]),p['lock_remaining'])
            self.assertFalse(unlocked_visible(image,p,board['grid']))

    def test_ocr_timeout_and_uncertainty_cannot_guess_count(self):
        import numpy as np
        from grid_recognition import measure_grid,pink_mask,clip_board
        im=np.asarray(Image.open(HERE/'examples/level31/initial.png').resize((379,835)))
        axes,_=measure_grid(clip_board(pink_mask(im)))
        with patch('lock_vision.subprocess.run',side_effect=subprocess.TimeoutExpired('ocr',4)):
            with self.assertRaisesRegex(RecognitionError,'超时'):LockReader().detect(im,axes)
        with patch('lock_vision.subprocess.run',return_value=subprocess.CompletedProcess([],1,'{"error":"ambiguous"}')):
            with self.assertRaisesRegex(RecognitionError,'ambiguous'):LockReader().detect(im,axes)

    def test_fast_disjoint_click_waits_for_actual_exit_and_lock_disappearance(self):
        plan=solve([PIG,pig('b',1,12,lock=1)]);now=[0];clicks=[]
        def frame(x,show_lock):
            im=picture(x);d=ImageDraw.Draw(im);d.ellipse((39,444,87,468),fill=(245,150,165))
            if show_lock:d.rectangle((53,445,73,466),fill=(248,210,30))
            return im
        r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        r.start(plan,GRID,0,{'id':1});r.observe(frame(63,True),{'id':1},0)
        now[0]=.1;r.tick();self.assertEqual(len(clicks),1)
        r.observe(frame(150,True),{'id':1},.2);now[0]=.2;r.tick()
        self.assertEqual(len(clicks),1);self.assertIn('解锁',r.status)
        self.assertEqual(next(p for p in r.telemetry()['objects'] if p['id']=='b')['lock_remaining'],1)
        self.assertEqual(plan['states'][1][0]['lock_remaining'],0)
        r.observe(frame(None,True),{'id':1},.3);r.observe(frame(None,True),{'id':1},.4)
        now[0]=.4;r.tick();self.assertEqual(len(clicks),1);self.assertIn('锁消失',r.status)
        r.observe(frame(None,False),{'id':1},.5);now[0]=.5;r.tick();self.assertEqual(len(clicks),2)

    def test_unlock_tracks_completed_exits_even_out_of_order(self):
        from types import SimpleNamespace
        plan=solve([pig('a',1,0),pig('b',1,8),pig('lock',1,16,lock=2)])
        r=PlanPlayback(lambda *a:None,.1,lambda:0)
        r.start(plan,GRID,0,{'id':1});r.index=2;r.preflight=True;r.latest_image=picture();r.latest_at=0
        r.moving=[SimpleNamespace(index=0,complete=False)]
        self.assertFalse(r.safe_to_dispatch(0));self.assertIn('1/2',r.status)

if __name__=='__main__':unittest.main()
