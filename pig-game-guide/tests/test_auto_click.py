import unittest
from unittest.mock import patch
from PIL import Image,ImageDraw
import numpy as np
from auto_click import PlanPlayback
from solver import solve
from tracking import collision_possible,MovingPig
from motion_guard import MotionGuard,matches_state

GRID={'x':{'origin':50,'scale':13},'y':{'origin':300,'scale':13}}
PIG={'id':'a','kind':'pig','d':'E','u':1,'v':0}

def picture(x=63):
    im=Image.new('RGB',(379,835),(100,180,50))
    if x is not None:
        d=ImageDraw.Draw(im);d.ellipse((x-24,288,x+24,312),fill=(245,150,165));d.ellipse((x+14,294,x+20,300),fill=(60,30,40))
    return im

def path(x,y,ex,ey):
    vertical=x==ex
    return {'start':np.array([x,y],float),'end':np.array([ex,ey],float),
            'extent':np.array([12,24] if vertical else [24,12],float),'axis':int(vertical),
            'sign':1 if ex>x or ey>y else -1,'exit':True}

class PlaybackTests(unittest.TestCase):
    def test_background_yellow_flowers_do_not_block_preflight(self):
        import json
        from pathlib import Path
        here=Path(__file__).resolve().parents[1]/'examples'
        board=json.loads((here/'level17_preflight_board.json').read_text())
        image=Image.open(here/'level17_preflight_frame.png')
        self.assertTrue(matches_state(image,board['objects'],board['grid']))

    def test_later_pig_in_cleared_route_does_not_keep_previous_pig_moving(self):
        import json
        from pathlib import Path
        here=Path(__file__).resolve().parents[1]/'examples'
        data=json.loads((here/'level17_after39_context.json').read_text())
        image=Image.open(here/'level17_after39_frame.png')
        guard=MotionGuard(data['plan'],0,data['grid'],0)
        self.assertFalse(guard.observe(image,.1))
        guard.set_context(data['plan']['states'][-1])
        self.assertFalse(guard.observe(image,.2))
        self.assertTrue(guard.observe(image,.3))

    def test_crossing_at_same_time_is_blocked(self):
        self.assertTrue(collision_possible(path(50,300,400,300),path(150,400,150,140),(100,100),(100,100)))

    def test_crossing_after_first_pig_leaves_is_safe(self):
        self.assertFalse(collision_possible(path(50,300,400,300),path(150,650,150,140),(300,300),(40,40)))

    def test_startup_delay_can_turn_separate_arrivals_into_collision(self):
        a,b=path(50,300,400,300),path(150,400,150,140)
        self.assertFalse(collision_possible(a,b,(100,100),(300,300)))
        self.assertTrue(collision_possible(a,{**b,'launch_delay':.6},(100,100),(300,300)))

    def test_unknown_speed_waits_at_intersection_but_allows_disjoint_lanes(self):
        self.assertTrue(collision_possible(path(50,300,400,300),path(150,400,150,140)))
        self.assertFalse(collision_possible(path(50,300,400,300),path(50,450,400,450)))

    def test_pixel_tracking_measures_velocity(self):
        plan=solve([PIG]);track=MovingPig(plan,0,GRID,picture(),0)
        track.observe(picture(83),.1);track.observe(picture(103),.2)
        self.assertIsNotNone(track.speed)
        self.assertAlmostEqual(track.speed,200,delta=30)
        self.assertAlmostEqual(track.position[0],103,delta=3)

    def test_running_pig_must_not_be_considered_gone(self):
        guard=MotionGuard(solve([PIG]),0,GRID,0)
        self.assertFalse(guard.observe(picture(140),.1))
        self.assertFalse(guard.observe(picture(230),.2))
        self.assertFalse(guard.observe(picture(None),.3))
        self.assertTrue(guard.observe(picture(None),.4))

    def test_interval_does_not_finish_last_pig_early(self):
        now=[0];clicks=[]
        r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        r.start(solve([PIG]),GRID,0,{'id':1})
        r.observe(picture(),{'id':1},0)
        now[0]=.1;self.assertEqual(r.tick(),1)
        self.assertTrue(r.active)
        r.observe(picture(150),{'id':1},.2);now[0]=.2;r.tick()
        self.assertTrue(r.active);self.assertEqual(len(clicks),1)
        r.observe(picture(None),{'id':1},.3);r.observe(picture(None),{'id':1},.4)
        now[0]=.4;r.tick();self.assertFalse(r.active);self.assertEqual(r.confirmed,1)

    def test_stale_tracking_frame_prevents_click(self):
        now=[0];clicks=[];r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        r.start(solve([PIG]),GRID,0,{'id':1});r.observe(picture(),{'id':1},0)
        now[0]=1;r.tick();self.assertEqual(clicks,[])

    def test_stream_reconnect_holds_clicks_and_keeps_cached_plan(self):
        now=[0];clicks=[];r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        plan=solve([PIG]);r.start(plan,GRID,0,{'id':1});r.observe(picture(),{'id':1},0)
        r.hold_for_capture();now[0]=1;r.tick()
        self.assertEqual(clicks,[]);self.assertIs(r.plan,plan)
        r.observe(picture(),{'id':1},1);r.tick()
        self.assertEqual(len(clicks),1);self.assertIs(r.plan,plan)

    def test_point_one_interval_waits_for_crossing_pig(self):
        b={'id':'b','kind':'pig','d':'N','u':8,'v':9}
        plan=solve([PIG,b]);now=[0];clicks=[]
        def frame(x):
            im=picture(x);d=ImageDraw.Draw(im);d.ellipse((142,393,166,441),fill=(245,150,165));return im
        r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        r.start(plan,GRID,0,{'id':1});r.observe(frame(63),{'id':1},0)
        now[0]=.1;r.tick();self.assertEqual(len(clicks),1)
        r.observe(frame(83),{'id':1},.2);now[0]=.2;r.tick();self.assertEqual(len(clicks),1)
        r.observe(frame(None),{'id':1},.3);r.observe(frame(None),{'id':1},.4)
        now[0]=.4;r.tick();self.assertEqual(len(clicks),2)

    def test_point_one_interval_allows_disjoint_routes(self):
        b={'id':'b','kind':'pig','d':'E','u':1,'v':12}
        plan=solve([PIG,b]);now=[0];clicks=[]
        def frame(x):
            im=picture(x);d=ImageDraw.Draw(im);d.ellipse((39,444,87,468),fill=(245,150,165));return im
        r=PlanPlayback(lambda *a:clicks.append(a),.1,lambda:now[0])
        r.start(plan,GRID,0,{'id':1});r.observe(frame(63),{'id':1},0)
        now[0]=.1;r.tick();r.observe(frame(83),{'id':1},.2)
        now[0]=.2;r.tick();self.assertEqual(len(clicks),2)

    def test_speed_estimator_handles_real_captured_acceleration(self):
        import json
        from pathlib import Path
        samples=json.loads((Path(__file__).resolve().parents[1]/'examples/live-motion-samples.json').read_text())
        b={'id':'s','kind':'pig','d':'S','u':0,'v':1}
        grid={'x':{'origin':samples['start'][0],'scale':13},'y':{'origin':samples['start'][1]-13,'scale':13}}
        track=MovingPig(solve([b]),0,grid,picture(),0)
        for s in samples['samples']:track.accept_position(np.array([s['x'],s['y']]),s['t'],.9)
        self.assertIsNotNone(track.speed)
        self.assertGreater(track.speed,250);self.assertLess(track.speed,450)

    def test_overlay_preserves_cached_plan_without_click(self):
        r=PlanPlayback(lambda *a:self.fail('unexpected click'),.1)
        plan=solve([PIG]);r.start(plan,GRID,0,{'id':1})
        r.observe(Image.new('RGB',(379,835),'black'),{'id':1})
        self.assertFalse(r.preflight);self.assertIs(r.plan,plan);self.assertTrue(r.active)

if __name__=='__main__':unittest.main()
