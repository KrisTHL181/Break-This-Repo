import json
import unittest
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

from grid_recognition import GridRecognizer
from recognition import RecognitionError,fingerprint
from solver import half,solve,validate
from motion_guard import matches_state,box,area_threshold

HERE=Path(__file__).resolve().parents[1]
LAYOUT=[('E',1,0,2),('W',12,0,3),('S',6,1,2),('N',0,7,2),('W',5,6,2),('E',11,6,2),('N',14,8,3),('S',4,11,2),('E',9,12,2),('N',0,15,2),('W',7,18,2),('E',13,16,2)]
OBJECTS=[dict(id=str(i),kind='pig',d=d,u=u,v=v,length=n) for i,(d,u,v,n) in enumerate(LAYOUT)]


def scene(pitch,objects=OBJECTS,vertical_pitch=None):
    sy=vertical_pitch or pitch
    image=Image.new('RGB',(379,835),(150,220,50))
    with np.load(HERE/'templates.npz') as templates:
        for p in objects:
            t=templates['pig_'+p['d']][0]
            if p['length']>2:
                axis=0 if p['d'] in 'NS' else 1
                body=np.take(t,[25],axis=axis)
                shape=(25,(p['length']-2)*26+1) if axis==0 else ((p['length']-2)*26+1,25)
                body=cv2.resize(body,shape)
                t=np.concatenate((np.take(t,range(25),axis=axis),body,np.take(t,range(26,51),axis=axis)),axis=axis)
            hx,hy=half(p);w,h=round(hx*pitch-1),round(hy*sy-1)
            x,y=40+p['u']*pitch/2,200+p['v']*sy/2
            image.paste(Image.fromarray(cv2.resize(t,(w,h))),(round(x-w//2),round(y-h//2)))
    return image


def aligned_signature(reference,new_grid):
    result=[]
    for p in reference['objects']:
        q=dict(p);hx,hy=half(p)
        for axis,coord,extent in [('x','u',hx),('y','v',hy)]:
            pixel=reference['grid'][axis]['origin']+p[coord]*reference['grid'][axis]['scale']
            parity=(extent-1)%2
            q[coord]=int(2*round(((pixel-new_grid[axis]['origin'])/new_grid[axis]['scale']-parity)/2)+parity)
        result.append(q)
    return fingerprint(result)


class GridRecognitionTests(unittest.TestCase):
    def test_real_boards_match_saved_positions_lengths_and_directions(self):
        cases=[('level7_regression.png','level7/board.json'),('level11_long_regression.png','level11/board.json'),
               ('level13_regression.png','level13/board.json'),('level14_regression.png','level14/board.json'),
               ('level21/frame.png','level21/board.json'),('level24/frame.png','level24/board.json'),
               ('level25/initial.png','level25/board.json'),('level26/frame-0.png','level26/board.json')]
        r=GridRecognizer()
        for screenshot,saved in cases:
            with self.subTest(screenshot=screenshot):
                board=r.read(HERE/'examples'/screenshot)
                expected=json.loads((HERE/'examples'/saved).read_text())
                self.assertEqual(fingerprint(board['objects']),aligned_signature(expected,board['grid']))
                plan=solve(board['objects'])
                self.assertEqual(plan['states'][-1],[])
                self.assertTrue(all(a['id'].startswith(('pig_','elephant_')) for a in plan['actions']))
                self.assertTrue(matches_state(Image.open(HERE/'examples'/screenshot),plan['states'][0],board['grid']))

    def test_same_board_at_five_cell_sizes(self):
        r=GridRecognizer();validate(OBJECTS)
        for pitch in (16,20,26,32,38):
            with self.subTest(pitch=pitch):
                im=scene(pitch);board=r.read(im)
                self.assertEqual(fingerprint(board['objects']),fingerprint(OBJECTS))
                self.assertAlmostEqual(board['lattice']['pitch_x'],pitch,delta=.4)
                self.assertAlmostEqual(board['lattice']['pitch_y'],pitch,delta=.4)
                self.assertTrue(matches_state(im,board['objects'],board['grid']))

    def test_independent_axis_spacing(self):
        board=GridRecognizer().read(scene(26,vertical_pitch=29))
        self.assertEqual(fingerprint(board['objects']),fingerprint(OBJECTS))
        self.assertAlmostEqual(board['lattice']['pitch_x'],26,delta=.4)
        self.assertAlmostEqual(board['lattice']['pitch_y'],29,delta=.4)

    def test_five_cell_pig_is_measured_not_stretched_from_two_cells(self):
        objects=[dict(p) for p in OBJECTS];objects[6].update(length=5,v=10);validate(objects)
        board=GridRecognizer().read(scene(26,objects))
        self.assertEqual(fingerprint(board['objects']),fingerprint(objects))
        validate(board['objects'])
        self.assertEqual(sum(p['length'] for p in board['objects']),28)

    def test_new_level_size_rejects_the_old_grid(self):
        r=GridRecognizer();small=r.read(scene(16))
        with self.assertRaisesRegex(RecognitionError,'格子大小已改变'):
            r.read(scene(38),grid=small['grid'])
        self.assertEqual(fingerprint(r.read(scene(38))['objects']),fingerprint(OBJECTS))

    def test_final_pig_keeps_the_existing_grid(self):
        r=GridRecognizer();first=r.read(scene(26));last=[OBJECTS[-1]]
        board=r.read(scene(26,last),grid=first['grid'])
        self.assertEqual(fingerprint(board['objects']),fingerprint(last))

    def test_birds_have_only_passive_occupancy(self):
        board=GridRecognizer().read(HERE/'examples/level25/initial.png')
        birds=[p for p in board['objects'] if p['kind']=='duck']
        self.assertEqual(len(birds),11)
        self.assertTrue(all(p['passive'] and p['d']=='' and 'match_score' not in p for p in birds))
        plan=solve(board['objects']);ids={p['id'] for p in birds}
        self.assertTrue(all(a['id'] not in ids for a in plan['actions']))
        self.assertEqual(sum(len(a['duck_exits']) for a in plan['actions'])+len(plan['initial_duck_exits']),11)

    def test_motion_margins_scale_with_cells(self):
        p={'kind':'pig','d':'E','length':2,'u':0,'v':0}
        small={'x':{'origin':180,'scale':8},'y':{'origin':400,'scale':8}}
        large={'x':{'origin':180,'scale':19},'y':{'origin':400,'scale':19}}
        self.assertGreater(box(p,large,7)[2]-180,2*(box(p,small,7)[2]-180))
        self.assertAlmostEqual(area_threshold(large,60)/area_threshold(small,60),(19/8)**2)


if __name__=='__main__':unittest.main()
