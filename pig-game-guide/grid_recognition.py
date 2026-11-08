"""Grid-first recognition: measure spacing, fill cells, then compare head/tail parts.

Whole-pig template matching is not used. Yellow animals need neither an appearance
match nor a direction: they are optional passive cell occupancy for the simulator.
"""
from pathlib import Path
import time
import cv2
import numpy as np
from PIL import Image
from animal_vision import pig_appearance, pig_body_appearance, board_elephant_mask, elephant_appearance
from recognition import RecognitionError
from solver import half, validate
from lock_vision import LockReader, masked_correlation

ROI=(20,178,355,700)


def pink_mask(rgb):
    a=np.asarray(rgb).astype(np.int16)
    r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
    return (r>g+25)&(r>b+20)&(b>90)&(b>g-35)


def clip_board(mask):
    mask=mask.copy();mask[:178]=0;mask[700:]=0;mask[:,:20]=0;mask[:,355:]=0
    return mask


def sample(mask,x,y,w,h):
    x0,x1=max(0,round(x-w/2)),min(mask.shape[1],round(x+w/2)+1)
    y0,y1=max(0,round(y-h/2)),min(mask.shape[0],round(y+h/2)+1)
    return float(mask[y0:y1,x0:x1].mean()) if x1>x0 and y1>y0 else 0.


def measure_grid(mask):
    _,_,stats,_=cv2.connectedComponentsWithStats(cv2.erode(mask.astype('uint8'),np.ones((3,3),np.uint8)),8)
    shapes=[s for s in stats[1:] if s[4]>50 and max(s[2:4])>1.5*min(s[2:4])]
    if not shapes:raise RecognitionError('没有足够的身体轮廓来估算格子')
    thickness=float(np.median([min(s[2:4]) for s in shapes]))
    result=[];residuals=[]
    for axis in (0,1):
        projection=mask.sum(axis=axis).astype(float);coord=np.arange(len(projection))
        # The eroded body width bounds a single cell and excludes row-gap
        # harmonics (1.5 or 2 cells) on sparse boards.
        periods=np.arange(max(8,thickness*.98),min(90,max(thickness+4,thickness*1.5)),.05)
        if not len(periods):raise RecognitionError('当前身体大小超出可辨识范围')
        keep=(coord>=20)&(coord<355) if axis==0 else (coord>=178)&(coord<700)
        values=projection[keep];values-=values.mean()
        coefficients=np.exp(-2j*np.pi*coord[keep][None,:]/periods[:,None])@values
        best=int(np.argmax(abs(coefficients)));pitch=float(periods[best])
        phase=float((-np.angle(coefficients[best])*pitch/(2*np.pi))%pitch)
        centres=[]
        for x,y,w,h,area in shapes:
            short,long=(w,h) if axis==0 else (h,w)
            if .65*thickness<short<1.35*thickness and long>1.5*short:
                centres.append(x+(w-1)/2 if axis==0 else y+(h-1)/2)
        centres=np.asarray(centres)
        if len(centres)<3 or np.ptp(centres)<pitch:
            # Sparse boards use full component centres and estimated cell counts.
            centres=[]
            for x,y,w,h,area in shapes:
                span=w if axis==0 else h
                count=max(1,round(span/pitch))
                centres.append((x+(w-1)/2 if axis==0 else y+(h-1)/2)-(count-1)*pitch/2)
            centres=np.asarray(centres)
        indices=np.rint((centres-phase)/pitch)
        if len(centres)>=3 and np.ptp(indices)>0:
            for _ in range(4):
                good=abs(centres-phase-pitch*indices)<pitch*.16
                if good.sum()<3 or np.ptp(indices[good])==0:break
                phase,pitch=map(float,np.linalg.lstsq(np.c_[np.ones(good.sum()),indices[good]],centres[good],rcond=None)[0])
                indices=np.rint((centres-phase)/pitch)
        elif len(centres):
            phase=float(np.median(centres-indices*pitch))
        if not 8<=pitch<=90:raise RecognitionError('无法确定当前格子大小')
        residual=abs(centres-phase-pitch*indices)
        if len(residual) and np.median(residual)>pitch*.13:raise RecognitionError('格线拟合不一致，保留已有方案')
        result.append((phase%pitch,pitch));residuals.append(float(np.median(residual)))
    if not .8<result[0][1]/result[1][1]<1.25:raise RecognitionError('横纵格子尺寸不一致')
    return result,residuals


def cell_occupancy(mask,axes,threshold=.35):
    (px,sx),(py,sy)=axes;cells={}
    for row in range(int(np.ceil((178-py)/sy)),int(np.floor((700-py)/sy))+1):
        for col in range(int(np.ceil((20-px)/sx)),int(np.floor((355-px)/sx))+1):
            value=sample(mask,px+col*sx,py+row*sy,sx*.5,sy*.5)
            if value>threshold:cells[col,row]=value
    return cells


def body_groups(mask,axes,cells):
    (px,sx),(py,sy)=axes;adj={p:set() for p in cells}
    for x,y in cells:
        for dx,dy in ((1,0),(0,1)):
            other=(x+dx,y+dy)
            if other not in cells:continue
            bridge=sample(mask,px+(x+dx/2)*sx,py+(y+dy/2)*sy,
                          sx*(.12 if dx else .5),sy*(.12 if dy else .5))
            if bridge>.60:adj[x,y].add(other);adj[other].add((x,y))
    remaining=set(cells);groups=[]
    while remaining:
        queue=[remaining.pop()];group=set(queue)
        while queue:
            for neighbour in adj[queue.pop()]&remaining:
                remaining.remove(neighbour);group.add(neighbour);queue.append(neighbour)
        groups.append(group)
    return groups


class BodyAssembler:
    def __init__(self,image,axes,templates,occluded=None):
        self.image,self.axes,self.templates=image,axes,templates
        self.occluded=occluded
        self.crops={};self.scores={}

    def part_score(self,cell,direction,part):
        key=cell,direction,part
        if key in self.scores:return self.scores[key]
        if cell not in self.crops:
            (px,sx),(py,sy)=self.axes;c,r=cell
            xx,yy=np.meshgrid(np.arange(-18,19)*sx/26+px+c*sx,np.arange(-18,19)*sy/26+py+r*sy)
            crop=cv2.remap(self.image,xx.astype('float32'),yy.astype('float32'),cv2.INTER_LINEAR)
            hidden=None if self.occluded is None else cv2.remap(self.occluded.astype('uint8'),xx.astype('float32'),yy.astype('float32'),cv2.INTER_NEAREST).astype(bool)
            self.crops[cell]=pig_appearance(crop),pig_body_appearance(crop),hidden
        gray,body,hidden=self.crops[cell];best=-1.
        if hidden is not None and hidden.any():
            best=masked_correlation(gray,body,hidden,self.templates[direction,part])
            self.scores[key]=best
            return best
        for a,b in self.templates[direction,part]:
            g=cv2.matchTemplate(gray,a,cv2.TM_CCOEFF_NORMED)
            p=cv2.matchTemplate(body,b,cv2.TM_CCOEFF_NORMED)
            best=max(best,float(np.maximum(g,p).max()))
        self.scores[key]=best
        return best

    def split(self,group):
        if len(group)>32:raise RecognitionError('身体轮廓大面积连在一起，暂不猜测占格')
        candidates=[]
        for c,r in sorted(group):
            for dx,dy,directions in ((1,0,'WE'),(0,1,'NS')):
                for length in range(2,min(8,len(group))+1):
                    cells=frozenset((c+dx*i,r+dy*i) for i in range(length))
                    if not cells<=group:break
                    ends=((c,r),(c+dx*(length-1),r+dy*(length-1)))
                    possibilities=[]
                    for d,h,t in ((directions[0],ends[0],ends[1]),(directions[1],ends[1],ends[0])):
                        hs,ts=self.part_score(h,d,'head'),self.part_score(t,d,'tail')
                        possibilities.append((.6*hs+.4*ts,hs,ts,d))
                    possibilities.sort(reverse=True)
                    quality,head,tail,d=possibilities[0];margin=quality-possibilities[1][0]
                    if head<.78 or tail<.75 or margin<.025:continue
                    candidates.append({'cells':cells,'d':d,'length':length,'quality':quality,
                                       'head_score':head,'tail_score':tail,'direction_margin':margin,
                                       'cost':length*(1-quality)})
        covering={c:[p for p in candidates if c in p['cells']] for c in group}
        solutions=[];visits=0
        def visit(left,chosen,cost):
            nonlocal visits
            visits+=1
            if visits>5000:raise RecognitionError('局部身体组合过多，保留已有方案')
            if not left:
                solutions.append((cost,list(chosen)));solutions.sort(key=lambda x:x[0]);del solutions[2:];return
            if len(solutions)>1 and cost>solutions[1][0]:return
            cell=min(left,key=lambda c:sum(p['cells']<=left for p in covering[c]))
            for candidate in covering[cell]:
                if candidate['cells']<=left:visit(left-candidate['cells'],chosen+[candidate],cost+candidate['cost'])
        visit(group,[],0.)
        if not solutions:raise RecognitionError('格内猪头或尾部不清晰，保留已有方案')
        if len(solutions)>1 and solutions[1][0]-solutions[0][0]<.012*len(group):
            raise RecognitionError('相邻猪的占格存在歧义，保留已有方案')
        return solutions[0][1]


class GridRecognizer:
    def __init__(self):
        self.locks=LockReader()
        root=Path(__file__).parent;self.parts={};self.elephants={d:[] for d in 'NSEW'}
        with np.load(root/'templates.npz') as source:
            for d in 'NSEW':
                for part in ('head','tail'):
                    front=(d in 'NW')==(part=='head');variants=[]
                    for t in source['pig_'+d]:
                        crop=(t[:25] if front else t[-25:]) if d in 'NS' else (t[:,:25] if front else t[:,-25:])
                        variants.append((pig_appearance(crop),pig_body_appearance(crop)))
                    self.parts[d,part]=variants
        # Long pigs can have a different face rendering; keep only their end
        # cells here, never the full body or a level-specific body size.
        long_path=root/'long_templates.npz'
        if long_path.exists():
            with np.load(long_path) as source:
                for d,templates in source.items():
                    for t in templates:
                        for part in ('head','tail'):
                            front=(d in 'NW')==(part=='head')
                            crop=(t[:25] if front else t[-25:]) if d in 'NS' else (t[:,:25] if front else t[:,-25:])
                            self.parts[d,part].append((pig_appearance(crop),pig_body_appearance(crop)))
        with np.load(root/'elephant_templates.npz') as source:
            for name,template in source.items():self.elephants[name.split('_')[0]].append(template)

    def elephant_objects(self,image,axes):
        scale=np.sqrt(axes[0][1]*axes[1][1])/26
        mask=board_elephant_mask(image).astype('uint8')
        _,_,stats,centres=cv2.connectedComponentsWithStats(mask,8)
        appearance=elephant_appearance(image);result=[]
        for k,(x,y,w,h,area) in enumerate(stats[1:],1):
            if area<300*scale*scale:continue
            if not (1800*scale*scale<area<9000*scale*scale and 60*scale<=max(w,h)<=115*scale and 40*scale<=min(w,h)<=85*scale):
                raise RecognitionError('蓝灰色物体占格尚未确定')
            pad=round(8*scale);x0,y0=max(0,x-pad),max(0,y-pad)
            crop=appearance[y0:y+h+pad,x0:x+w+pad];scores=[]
            for d in ('NS' if h>w else 'EW'):
                for template in self.elephants[d]:
                    for factor in (1.,1.065,.94,.88,.82):
                        th,tw=template.shape[:2];t=cv2.resize(template,(round(tw*scale*factor),round(th*scale*factor)))
                        if crop.shape[0]<t.shape[0] or crop.shape[1]<t.shape[1]:continue
                        scores.append((float(cv2.matchTemplate(crop,t,cv2.TM_CCOEFF_NORMED).max()),d))
            if not scores:raise RecognitionError('小象身体未完整显示')
            score,d=max(scores);margin=score-max(s for s,other in scores if other!=d)
            if score<.8 or margin<.1:raise RecognitionError('小象朝向尚未确定')
            x,y=map(float,centres[k]);hx,hy=(2,3) if d in 'NS' else (3,2)
            col=round((x-axes[0][0])/axes[0][1]-(hx-1)/2);row=round((y-axes[1][0])/axes[1][1]-(hy-1)/2)
            result.append({'kind':'elephant','d':d,'width':2,'length':3,'u':2*col+hx-1,'v':2*row+hy-1,
                           'x':x,'y':y,'match_score':score,'direction_margin':margin})
        return result

    def read(self,source,grid=None):
        started=time.monotonic();image=source if isinstance(source,Image.Image) else Image.open(source)
        if abs(image.width/image.height-379/835)>.035:raise RecognitionError('镜像窗口比例已改变')
        im=np.asarray(image.convert('RGB').resize((379,835)));pink=clip_board(pink_mask(im))
        if int(pink.sum())<40 and int(board_elephant_mask(im).sum())<300:
            return {'objects':[],'grid':grid,'size':list(image.size),'recognition_ms':1000*(time.monotonic()-started)}
        if grid:
            axes=[(grid[a]['origin'],grid[a]['scale']*2) for a in ('x','y')];residuals=[]
            if pink.sum()>12*axes[0][1]*axes[1][1]:
                measured,_=measure_grid(pink)
                if any(abs(measured[i][1]/axes[i][1]-1)>.06 for i in (0,1)):
                    raise RecognitionError('本关格子大小已改变，需要重新划格')
        else:axes,residuals=measure_grid(pink)
        locks,occluded=self.locks.detect(im,axes)
        silhouette=pink|occluded
        cells=cell_occupancy(silhouette,axes);assembler=BodyAssembler(im,axes,self.parts,occluded if locks else None);objects=[]
        assigned=set()
        for group in body_groups(silhouette,axes,cells):
            for p in assembler.split(group):
                c=min(x for x,y in p['cells']);r=min(y for x,y in p['cells']);vertical=p['d'] in 'NS'
                u,v=(2*c,2*r+p['length']-1) if vertical else (2*c+p['length']-1,2*r)
                x,y=axes[0][0]+u*axes[0][1]/2,axes[1][0]+v*axes[1][1]/2
                mark=im[max(0,round(y-axes[1][1]/4)):round(y+axes[1][1]/4)+1,max(0,round(x-axes[0][1]/4)):round(x+axes[0][1]/4)+1]
                hx,hy=(1,p['length']) if vertical else (p['length'],1)
                owners=[i for i,lock in enumerate(locks) if abs(lock['box'][0]+lock['box'][2]/2-x)<hx*axes[0][1]/2
                        and abs(lock['box'][1]+lock['box'][3]/2-y)<hy*axes[1][1]/2]
                if len(owners)>1 or set(owners)&assigned:raise RecognitionError('锁与猪的对应关系不明确')
                assigned.update(owners)
                if not owners and mark.size and (mark.max(2)<110).mean()>.07:raise RecognitionError('出现尚未支持的箭头或炸弹标记')
                objects.append({'kind':'pig','d':p['d'],'length':p['length'],'u':u,'v':v,'x':x,'y':y,
                                'match_score':p['quality'],'head_score':p['head_score'],'tail_score':p['tail_score'],
                                'direction_margin':p['direction_margin']})
                if owners:objects[-1]['lock_remaining']=locks[owners[0]]['count']
        if len(assigned)!=len(locks):raise RecognitionError('锁未能对应到完整猪身')
        objects+=self.elephant_objects(im,axes)
        if not objects:raise RecognitionError('尚未找到完整动物占格')
        # Birds are not identified by templates or direction and never become
        # click actions. Yellow occupied cells only protect temporary passages.
        a=im.astype(np.int16);r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
        yellow=clip_board((r>160)&(g>150)&(r>g+8)&(b<g-50)&(4*(g-b)>3*(r-b)))
        used=set()
        from solver import occupied_cells
        for p in objects:used.update(occupied_cells(p))
        for c,r in cell_occupancy(yellow,axes,.35):
            if (c,r) not in used:
                objects.append({'kind':'duck','d':'','u':2*c,'v':2*r,'x':axes[0][0]+c*axes[0][1],
                                'y':axes[1][0]+r*axes[1][1],'passive':True})
        if grid is None:
            active=[p for p in objects if p['kind']!='duck']
            shiftx=min((p['u']-half(p)[0]+1)//2 for p in active)
            shifty=min((p['v']-half(p)[1]+1)//2 for p in active)
            axes=[(axes[0][0]+shiftx*axes[0][1],axes[0][1]),(axes[1][0]+shifty*axes[1][1],axes[1][1])]
            for p in objects:p['u']-=2*shiftx;p['v']-=2*shifty
        grid={a:{'origin':axes[i][0],'scale':axes[i][1]/2} for i,a in enumerate(('x','y'))}
        objects.sort(key=lambda p:(p['v'],p['u'],p['kind']))
        for i,p in enumerate(objects):p['id']=f"{p['kind']}_{i+1:03}"
        validate(objects)
        covered=np.zeros_like(pink)
        for p in objects:
            hx,hy=half(p);x=grid['x']['origin']+p['u']*grid['x']['scale'];y=grid['y']['origin']+p['v']*grid['y']['scale']
            pad=.45 if p['kind']=='elephant' else .25
            wx,wy=(hx/2+pad)*axes[0][1],(hy/2+pad)*axes[1][1]
            covered[max(0,round(y-wy)):round(y+wy)+1,max(0,round(x-wx)):round(x+wx)+1]=True
        _,_,unknown,_=cv2.connectedComponentsWithStats((pink&~covered).astype('uint8'),8)
        if any(s[4]>.12*axes[0][1]*axes[1][1] for s in unknown[1:]):
            raise RecognitionError('格外仍有未归属的猪身，保留已有方案')
        return {'objects':objects,'grid':grid,'size':list(image.size),'recognition_ms':1000*(time.monotonic()-started),
                'lattice':{'method':'grid_then_head_tail','pitch_x':axes[0][1],'pitch_y':axes[1][1],
                           'fit_residual_px':residuals,'pig_cells':len(cells),'bird_mode':'passive_occupancy_only',
                           'locked_pigs':len(locks)}}
