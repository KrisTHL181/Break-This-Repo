"""Local template tracking and conservative time-dependent collision prediction."""
from collections import deque
import math
import cv2
import numpy as np
from solver import half, DIRECTIONS
from motion_guard import MotionGuard,masks


def motion_key(p,axis):
    return p['kind'],p.get('length',2),p.get('width',1),axis


def centre(p, grid):
    return np.array([grid['x']['origin']+p['u']*grid['x']['scale'],
                     grid['y']['origin']+p['v']*grid['y']['scale']],float)


def route(plan, index, grid):
    action=plan['actions'][index];p=next(p for p in plan['states'][index] if p['id']==action['id'])
    start=centre(p,grid);extent=np.array(half(p))*[grid['x']['scale'],grid['y']['scale']]
    axis,sign=DIRECTIONS[p['d']]
    end=start.copy()
    if action['type']=='slide':
        end=np.array([grid['x']['origin']+action['to'][0]*grid['x']['scale'],grid['y']['origin']+action['to'][1]*grid['y']['scale']])
    else:end[axis]=((355 if axis==0 else 700)+extent[axis]+4) if sign>0 else ((20 if axis==0 else 178)-extent[axis]-4)
    return {'id':p['id'],'pig':p,'start':start,'end':end,'extent':extent,'axis':axis,'sign':sign,
            'launch_delay': .6,
            'exit':action['type']=='exit','ducks':bool(action.get('duck_exits'))}


def swept(r):
    return np.minimum(r['start'],r['end'])-r['extent']-4,np.maximum(r['start'],r['end'])+r['extent']+4


def intersect(a,b):
    return bool(np.all(a[0]<b[1]) and np.all(b[0]<a[1]))


def envelope(r, speeds, t0, t1):
    distance=abs(r['end'][r['axis']]-r['start'][r['axis']])
    slow,fast=speeds
    delay=r.get('launch_delay',0)
    if r['exit'] and slow*max(0,t0-delay)>distance:return None
    a=r['start'].copy();b=r['start'].copy()
    a[r['axis']]+=r['sign']*min(distance,slow*max(0,t0-delay))
    b[r['axis']]+=r['sign']*min(distance,fast*(t1+r.get('age',0)))
    # Covers all speeds in the interval and all times in this integration bin.
    return np.minimum(a,b)-r['extent']-4,np.maximum(a,b)+r['extent']+4


def collision_possible(a, b, a_speeds=None, b_speeds=None):
    if not intersect(swept(a),swept(b)):return False
    if not a_speeds or not b_speeds:return True
    horizon=max(abs(a['end'][a['axis']]-a['start'][a['axis']])/a_speeds[0]+a.get('launch_delay',0),
                abs(b['end'][b['axis']]-b['start'][b['axis']])/b_speeds[0]+b.get('launch_delay',0))+.15
    for t in np.arange(0,min(horizon,30)+.04,.04):
        aa=envelope(a,a_speeds,t,t+.04);bb=envelope(b,b_speeds,t,t+.04)
        if aa is not None and bb is not None and intersect(aa,bb):return True
    # Longer uncertain horizons are not certified safe.
    return horizon>30


class MovingPig:
    def __init__(self, plan,index,grid,image,now,speed_hint=None):
        self.route=route(plan,index,grid);self.index=index;self.grid=grid
        self.guard=MotionGuard(plan,index,grid,now,speed_hint)
        self.position=self.route['start'].copy();self.samples=deque(maxlen=6)
        self.samples.append((now,self.position.copy()))
        self.speed=None;self.fresh_at=now;self.confidence=0.;self.complete=False
        self.last_observed=now;self.status='测量速度中';self.started=now;self.last_moved=now
        im=np.asarray(image.convert('RGB').resize((379,835)))
        x,y=np.rint(self.position).astype(int);hx,hy=np.maximum(5,np.rint(self.route['extent']-2).astype(int))
        self.template=im[max(0,y-hy):min(835,y+hy+1),max(0,x-hx):min(379,x+hx+1)].copy()

    def accept_position(self,pos,now,confidence):
        axis,sign=self.route['axis'],self.route['sign']
        advance=(pos[axis]-self.position[axis])*sign
        if advance < -3:return False
        if advance>1.5:self.last_moved=now
        self.position=pos;self.samples.append((now,pos.copy()));self.fresh_at=now;self.confidence=confidence
        if len(self.samples)>=3:
            speeds=[(b[1][axis]-a[1][axis])*sign/(b[0]-a[0]) for a,b in zip(self.samples,list(self.samples)[1:]) if b[0]>a[0]]
            positive=[v for v in speeds if 15<v<1800][-3:]
            if len(positive)>=2 and max(positive)<min(positive)*3:
                self.speed=float(np.median(positive))
        if now-self.last_moved>.2:self.speed=None
        self.status='追踪中' if self.speed else '测量速度中'
        return True

    def color_position(self,image,now):
        colors=masks(image)
        pink=colors[2 if self.route['pig']['kind']=='elephant' else 0]
        moving=pink & self.guard.roi & ~self.guard.allowed
        axis,sign=self.route['axis'],self.route['sign'];cross=1-axis
        lane=int(round(self.route['start'][cross]));radius=6
        strip=moving[:,max(0,lane-radius):lane+radius+1] if axis else moving[max(0,lane-radius):lane+radius+1,:]
        density=strip.mean(axis=1 if axis else 0)
        indices=np.where(density>.30)[0]
        if not len(indices):return None
        runs=np.split(indices,np.where(np.diff(indices)>1)[0]+1)
        candidates=[]
        reach=(self.speed or 700)*max(.06,now-self.fresh_at)*1.8+25
        expected=self.position[axis]+sign*(self.speed or 100)*max(0,now-self.fresh_at)
        for run in runs:
            if len(run)<8:continue
            centre=float(np.average(run,weights=density[run]));advance=(centre-self.position[axis])*sign
            if -4<=advance<=reach:
                # A large visible core gives a more stable centre than the
                # changing running-animation template or a head-only match.
                quality=abs(centre-expected)+max(0,20-len(run))*2
                candidates.append((quality,centre))
        if not candidates:return None
        pos=self.position.copy();pos[axis]=min(candidates)[1];pos[cross]=self.route['start'][cross]
        return pos

    def observe(self,image,now):
        if self.complete:return
        im=np.asarray(image.convert('RGB').resize((379,835)))
        axis,sign=self.route['axis'],self.route['sign'];cross=1-axis
        # Search only forward along this pig's lane. Other stationary pigs are
        # penalized using predicted position and the known one-way direction.
        reach=(self.speed or 500)*max(.05,now-self.fresh_at)*1.8+30
        lo=np.minimum(self.position,self.position+np.eye(2)[axis]*sign*reach)-self.route['extent']-7
        hi=np.maximum(self.position,self.position+np.eye(2)[axis]*sign*reach)+self.route['extent']+7
        lo[cross]=self.route['start'][cross]-self.route['extent'][cross]-5
        hi[cross]=self.route['start'][cross]+self.route['extent'][cross]+5
        x0,y0=np.maximum(0,lo.astype(int));x1,y1=np.minimum([379,835],np.ceil(hi).astype(int))
        crop=im[y0:y1,x0:x1];th,tw=self.template.shape[:2]
        color_pos=self.color_position(image,now)
        color_accepted=color_pos is not None and self.accept_position(color_pos,now,.9)
        if not color_accepted and crop.shape[0]>=th and crop.shape[1]>=tw:
            scores=cv2.matchTemplate(crop,self.template,cv2.TM_CCOEFF_NORMED)
            yy,xx=np.indices(scores.shape)
            positions=(yy+y0+th//2) if axis else (xx+x0+tw//2)
            delta=(positions-self.position[axis])*sign
            expected=(self.speed or 200)*max(0,now-self.fresh_at)
            quality=scores-.0015*np.abs(delta-expected)
            quality[delta < -4]=-10
            _,_,_,(bx,by)=cv2.minMaxLoc(quality)
            score=float(scores[by,bx]);pos=np.array([x0+bx+tw//2,y0+by+th//2],float)
            advance=(pos[axis]-self.position[axis])*sign
            # Static matches do not refresh speed confidence. If observation is
            # ambiguous the scheduler waits, rather than extrapolating forever.
            if score>=.68 and advance>=2:
                self.accept_position(pos,now,score)
        self.last_observed=now
        self.complete=self.guard.observe(image,now)
        if self.complete:self.status=self.guard.status

    def predicted_route(self,now):
        r=dict(self.route);r['start']=self.position.copy();r['age']=max(0,now-self.fresh_at)
        # A measured running pig has already passed its uncertain launch phase.
        r['launch_delay']=0 if self.speed else max(0,.6-(now-self.started))
        # Keep uncertainty about capture latency in the collision envelope.
        return r

    def speed_range(self,now):
        if not self.speed or now-self.fresh_at>.45:return None
        return self.speed*.55,self.speed*1.65

    def model_object(self):
        p=dict(self.route['pig']);p['u']=(self.position[0]-self.grid['x']['origin'])/self.grid['x']['scale'];p['v']=(self.position[1]-self.grid['y']['origin'])/self.grid['y']['scale']
        p['tracking']=True;p['speed_px_s']=self.speed
        return p
