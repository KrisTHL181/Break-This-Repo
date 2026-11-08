"""Fast pixel occupancy checks; no pig recognition or solution search per click."""
import time
import cv2
import numpy as np
from PIL import Image
from solver import half
from animal_vision import board_elephant_mask


def masks(image):
    a = np.asarray(image.convert('RGB').resize((379, 835))).astype(np.int16)
    pink = (a[:,:,0] > a[:,:,1]+25) & (a[:,:,0] > a[:,:,2]+20) & (a[:,:,2] > 90) & (a[:,:,2] > a[:,:,1]-35)
    # Ducks are warm yellow; the lime-yellow background flowers have G >= R.
    yellow = (a[:,:,0] > 160) & (a[:,:,1] > 150) & (a[:,:,0] > a[:,:,1]+8) & (a[:,:,2] < a[:,:,1]-50) & (4*(a[:,:,1]-a[:,:,2])>3*(a[:,:,0]-a[:,:,2]))
    return pink, yellow, board_elephant_mask(a)


def box(p, grid, padding=0):
    x = grid['x']['origin']+p['u']*grid['x']['scale']
    y = grid['y']['origin']+p['v']*grid['y']['scale']
    hx,hy = half(p)
    ex,ey=(4,10) if p['kind']=='elephant' and p['d'] in 'NS' else (10,4) if p['kind']=='elephant' else (0,0)
    pad_x=(padding+ex)*grid['x']['scale']/13
    pad_y=(padding+ey)*grid['y']['scale']/13
    # Ears, tail and trunk extend beyond the collision tiles.
    return (max(0,round(x-hx*grid['x']['scale']-pad_x)),
            max(178,round(y-hy*grid['y']['scale']-pad_y)),
            min(379,round(x+hx*grid['x']['scale']+pad_x)+1),
            min(700,round(y+hy*grid['y']['scale']+pad_y)+1))


def paint(mask, bounds):
    x0,y0,x1,y1 = bounds
    mask[y0:y1,x0:x1] = True


def present(p, grid, colors):
    x=grid['x']['origin']+p['u']*grid['x']['scale']
    y=grid['y']['origin']+p['v']*grid['y']['scale']
    rx,ry=grid['x']['scale']/2,grid['y']['scale']/2
    patch=colors[{'pig':0,'duck':1,'elephant':2}[p['kind']]][max(0,round(y-ry)):round(y+ry)+1,max(0,round(x-rx)):round(x+rx)+1]
    if 'lock_remaining' in p:
        # The lock covers the body center. Require pink in the footprint and
        # in the visible head edge instead of testing the covered center.
        from solver import DIRECTIONS
        axis,sign=DIRECTIONS[p['d']];hx,hy=half(p)
        if axis:y+=sign*(hy-.55)*grid['y']['scale']
        else:x+=sign*(hx-.55)*grid['x']['scale']
        head=colors[0][max(0,round(y-ry)):round(y+ry)+1,max(0,round(x-rx)):round(x+rx)+1]
        x0,y0,x1,y1=box(p,grid)
        body=colors[0][y0:y1,x0:x1]
        return head.size>0 and head.mean()>=.32 and body.size>0 and body.mean()>=.15
    return patch.size > 0 and patch.mean() >= .32


def component_area(mask):
    _,_,stats,_ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), 8)
    return max((int(s[4]) for s in stats[1:]), default=0)


def area_threshold(grid, base):
    return max(10,base*grid['x']['scale']*grid['y']['scale']/169)


def board_visible(image):
    small=np.asarray(image.convert('RGB').resize((379,835))).astype(np.int16)[178:700,20:355]
    visible=(small[:,:,1]>small[:,:,0]+8) & (small[:,:,1]>small[:,:,2]+35)
    sand=(small[:,:,0]>small[:,:,1]+15)&(small[:,:,1]>small[:,:,2]+30)&(small[:,:,0]>170)&(small[:,:,1]>120)
    visible|=sand
    frost=(small[:,:,2]>small[:,:,1]+20)&(small[:,:,1]>small[:,:,0]+8)&(small[:,:,0]>110)&(small[:,:,1]>140)
    visible|=frost
    return visible.mean()>=.12


def matches_state(image, state, grid):
    """Cheap preflight against the cached state; rejects overlays and manual changes."""
    if not board_visible(image):return False
    colors=masks(image)
    allowed=np.zeros((835,379),bool)
    for p in state:
        if not present(p,grid,colors):
            return False
        paint(allowed,box(p,grid,7))
    roi=np.zeros_like(allowed);roi[178:700,20:355]=True
    return component_area(np.logical_or.reduce(colors) & roi & ~allowed) < area_threshold(grid,80)


class MotionGuard:
    def __init__(self, plan, index, grid, started, speed_hint=None):
        self.action=plan['actions'][index]
        self.before,self.after=plan['states'][index:index+2]
        self.grid,self.started=grid,started
        self.pig=next(p for p in self.before if p['id']==self.action['id'])
        self.target=next((p for p in self.after if p['id']==self.action['id']),None)
        self.context=None
        self.set_context(self.after)
        self.roi=np.zeros_like(self.allowed)
        x0,y0,x1,y1=box(self.pig,grid,9)
        d=self.pig['d']
        if d=='E':x1=355
        elif d=='W':x0=20
        elif d=='N':y0=178
        else:y1=700
        self.roi[y0:y1,x0:x1]=True
        if self.action.get('duck_exits'):
            self.roi[178:700,20:355]=True
        start=(grid['x']['origin']+self.pig['u']*grid['x']['scale'],
               grid['y']['origin']+self.pig['v']*grid['y']['scale'])
        if self.action['type']=='slide':
            dest=self.action['to'];self.distance=abs(dest[0]-self.pig['u'])*grid['x']['scale']+abs(dest[1]-self.pig['v'])*grid['y']['scale']
        else:
            hx,hy=half(self.pig)
            self.distance={'E':355-start[0],'W':start[0]-20,'N':start[1]-178,'S':700-start[1]}[d]+max(hx*grid['x']['scale'],hy*grid['y']['scale'])
        # Estimate is informational. Actual occupancy checks release the next click.
        self.estimated_seconds=self.distance/(speed_hint or 150)
        self.timeout=max(12.,self.estimated_seconds*3+3)
        self.clear_since=None
        self.last_mask=None
        self.complete=False
        self.observed_speed=None
        self.status='正在跟踪移动中的猪'

    def set_context(self, state):
        # Another dispatched pig may legally occupy a portion of this pig's
        # already-cleared route. Check against its latest planned resting place,
        # not the obsolete position from when this movement began.
        if self.context is state:return
        self.context=state
        self.allowed=np.zeros((835,379),bool)
        for p in state:
            paint(self.allowed,box(p,self.grid,5 if p['id']==self.pig['id'] else 7))

    def observe(self, image, now):
        if self.complete:return True
        if now-self.started > self.timeout:
            raise TimeoutError('移动未到达预计位置，已停止点击并保留方案；可重新识别实际棋盘')
        colors=masks(image)
        foreground=np.logical_or.reduce(colors)
        unknown=foreground & self.roi & ~self.allowed
        clear=component_area(unknown)<area_threshold(self.grid,60)
        if self.target is not None:
            clear=clear and present(self.target,self.grid,colors)
            target_mask=colors[2 if self.pig['kind']=='elephant' else 0].copy()
            bounds=np.zeros_like(target_mask);paint(bounds,box(self.target,self.grid,2))
            target_mask &= bounds
            if self.last_mask is not None and np.count_nonzero(target_mask ^ self.last_mask)>area_threshold(self.grid,45):
                clear=False
            self.last_mask=target_mask
        # Two separately captured observations prevent a single transitional frame
        # from releasing a crossing pig. No whole-board recognition is performed.
        if clear:
            if self.clear_since is None:self.clear_since=now
            elif now-self.clear_since >= .08:
                self.complete=True
                self.observed_speed=self.distance/max(.1,now-self.started)
                self.status='已确认离场' if self.target is None else '已确认停到预计位置'
        else:
            self.clear_since=None
        return self.complete
