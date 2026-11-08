import json
import random
import unittest
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np

from solver import half,occupied_cells,validate,predict,apply_action,solve,settle_ducks
from search_engine import BitBoard
from recognition import Recognizer,RecognitionError,fingerprint
from motion_guard import matches_state,MotionGuard
from tracking import MovingPig,motion_key

HERE=Path(__file__).resolve().parents[1]


class ElephantTests(unittest.TestCase):
    def test_beach_board_recognizes_elephant_and_solves_every_object(self):
        board=Recognizer().read(HERE/'examples/level21/frame.png')
        self.assertEqual(sum(p['kind']=='pig' for p in board['objects']),80)
        elephants=[p for p in board['objects'] if p['kind']=='elephant']
        self.assertEqual(len(elephants),1)
        e=elephants[0]
        self.assertEqual((e['d'],e['width'],e['length']),('S',2,3))
        self.assertEqual(len(occupied_cells(e)),6)
        plan=solve(board['objects'])
        self.assertEqual(plan['states'][-1],[])
        self.assertEqual(sum(a['type']=='exit' for a in plan['actions']),81)
        self.assertTrue(matches_state(Image.open(HERE/'examples/level21/frame.png'),plan['states'][0],board['grid']))

    def test_width_changes_plan_fingerprint(self):
        p={'id':'e','kind':'elephant','d':'S','length':3,'width':2,'u':1,'v':2}
        self.assertNotEqual(fingerprint([p]),fingerprint([{**p,'width':1}]))

    def test_swaying_elephant_direction_and_complete_live_board(self):
        r=Recognizer()
        first=r.read(HERE/'examples/level26/frame-0.png')
        self.assertEqual(sum(p['kind']=='pig' for p in first['objects']),84)
        self.assertEqual(sum(p['kind']=='elephant' for p in first['objects']),1)
        plan=solve(first['objects'])
        self.assertEqual(plan['states'][-1],[])
        self.assertEqual(sum(a['type']=='exit' for a in plan['actions']),85)
        complete_frames=0
        for path in sorted((HERE/'examples/level26').glob('frame-*.png')):
            with self.subTest(frame=path.name):
                frame=Image.open(path).convert('RGB')
                elephants=r._elephants(np.asarray(frame.resize((379,835))))
                self.assertEqual(len(elephants),1)
                self.assertEqual(elephants[0]['d'],'S')
                try:
                    board=r.read(frame)
                except RecognitionError as error:
                    # During the widest ear swing, the neighbouring pig's
                    # appearance is partly occluded. Keep rejecting ambiguity.
                    self.assertIn('猪的方向不清晰',str(error))
                    continue
                complete_frames+=1
                self.assertEqual(fingerprint(board['objects']),fingerprint(first['objects']))
                self.assertTrue(matches_state(frame,plan['states'][0],first['grid']))
        self.assertGreaterEqual(complete_frames,5)

    def test_left_facing_elephant_on_live_beach_board(self):
        frame=HERE/'examples/level24/frame.png'
        board=Recognizer().read(frame)
        self.assertEqual(sum(p['kind']=='pig' for p in board['objects']),73)
        elephants=[p for p in board['objects'] if p['kind']=='elephant']
        self.assertEqual(len(elephants),1)
        self.assertEqual((elephants[0]['d'],half(elephants[0])),('W',(3,2)))
        plan=solve(board['objects'])
        self.assertEqual(plan['states'][-1],[])
        self.assertEqual(sum(a['type']=='exit' for a in plan['actions']),74)
        self.assertTrue(matches_state(Image.open(frame),plan['states'][0],board['grid']))

    def test_user_observed_elephant_stop_matches_predicted_cell(self):
        before=json.loads((HERE/'examples/level21/board.json').read_text())
        after=Recognizer().read(HERE/'examples/level21/after-elephant-slide.png',grid=before['grid'])
        e=next(p for p in before['objects'] if p['kind']=='elephant')
        observed=next(p for p in after['objects'] if p['kind']=='elephant')
        move=predict(before['objects'],e)
        self.assertEqual(move['type'],'slide')
        self.assertEqual(move['to'],[observed['u'],observed['v']])
        self.assertEqual(move['to'],[7,18])

    def test_both_lanes_block_a_wide_horizontal_body(self):
        for direction,u,columns in [('E',2,(6,4)),('W',12,(1,3))]:
            e={'id':'e','kind':'elephant','d':direction,'length':3,'width':2,'u':u,'v':1}
            obstacles=[{'id':str(i),'kind':'pig','d':'N','length':1,'u':x*2,'v':i*2}
                       for i,x in enumerate(columns)]
            state=[e]+obstacles;validate(state);b=BitBoard(state)
            action=predict(state,e)
            self.assertEqual(action['blocker'],'1')
            self.assertEqual(action['to'],[4 if direction=='E' else 10,1])
            after,_,_=b.apply(b.initial,b.occupancy(b.initial),0,b.move(b.initial,b.occupancy(b.initial),0))
            self.assertEqual(b.decode(after),apply_action(state,action)[0])

    def test_wide_geometry_matches_reference_in_all_directions(self):
        rng=random.Random(211)
        for case in range(100):
            objects=[];used=set()
            for i in range(7):
                for _ in range(50):
                    kind='elephant' if i==0 else 'duck' if i==1 else 'pig'
                    d=rng.choice('NSEW') if kind!='duck' else ''
                    p={'id':str(i),'kind':kind,'d':d,'length':3 if i==0 else 2,'width':2 if i==0 else 1}
                    hx,hy=half(p);x,y=rng.randrange(-3,6),rng.randrange(-3,6)
                    p.update(u=2*x+hx-1,v=2*y+hy-1)
                    cells=set(occupied_cells(p))
                    if not cells & used:objects.append(p);used|=cells;break
            validate(objects);b=BitBoard(objects)
            state,bits,_=b.settle(b.initial,b.occupancy(b.initial))
            for _ in range(30):
                original=b.decode(state);moves=[]
                for i in b.pigs:
                    if state[i]<0:continue
                    p=next(p for p in original if p['id']==objects[i]['id'])
                    action=predict(original,p);to=b.move(state,bits,i)
                    after,after_bits,ducks=b.apply(state,bits,i,to)
                    expected,escaped=apply_action(original,action)
                    self.assertEqual(b.decode(after),expected,(case,action))
                    self.assertEqual(sorted(objects[j]['id'] for j in ducks),sorted(escaped))
                    self.assertEqual(after_bits,b.occupancy(after))
                    if to!=state[i]:moves.append((after,after_bits))
                if not moves:break
                state,bits=rng.choice(moves)

    def test_elephant_pixels_are_tracked_separately_from_pigs(self):
        e={'id':'e','kind':'elephant','d':'S','length':3,'width':2,'u':1,'v':2}
        grid={'x':{'origin':120,'scale':13},'y':{'origin':350,'scale':13}}
        def picture(offset=None):
            im=Image.new('RGB',(379,835),(243,198,132))
            if offset is not None:
                draw=ImageDraw.Draw(im);draw.ellipse((108,337+offset,158,415+offset),fill=(170,190,210))
            return im
        plan=solve([e]);track=MovingPig(plan,0,grid,picture(0),0)
        track.observe(picture(15),.1);track.observe(picture(30),.2)
        self.assertIsNotNone(track.speed)
        self.assertAlmostEqual(track.speed,150,delta=20)
        self.assertNotEqual(motion_key(e,1),motion_key({**e,'kind':'pig'},1))
        guard=MotionGuard(plan,0,grid,0)
        self.assertFalse(guard.observe(picture(100),.3))
        self.assertFalse(guard.observe(picture(None),.4))
        self.assertTrue(guard.observe(picture(None),.5))


if __name__=='__main__':unittest.main()
