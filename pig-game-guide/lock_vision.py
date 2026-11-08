"""Recognize upright numeric padlocks independently of animal direction."""
import base64
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import cv2
import numpy as np
from PIL import Image
from recognition import RecognitionError


def masked_correlation(gray,body,occluded,templates):
    weights=np.lib.stride_tricks.sliding_window_view(~occluded,(25,25)).astype('float32')
    n=weights.sum((-2,-1));best=-1.
    for channel,index in ((gray,0),(body,1)):
        patches=np.lib.stride_tricks.sliding_window_view(channel.astype('float32'),(25,25))
        mean=(patches*weights).sum((-2,-1))/np.maximum(n,1)
        dev=(patches-mean[:,:,None,None])*weights
        for pair in templates:
            t=pair[index].astype('float32');tm=(t*weights).sum((-2,-1))/np.maximum(n,1)
            td=(t-tm[:,:,None,None])*weights
            den=np.sqrt((dev*dev).sum((-2,-1))*(td*td).sum((-2,-1)))
            # At least 31% of the end feature must actually be visible.
            corr=np.divide((dev*td).sum((-2,-1)),den,out=np.full_like(den,-1),where=(den>1)&(n>190))
            best=max(best,float(corr.max()))
    return best


class LockReader:
    def __init__(self):self.cache={}

    def detect(self,image,axes):
        from grid_recognition import clip_board,sample
        sx,sy=axes[0][1],axes[1][1]
        a=image.astype('int16');r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
        yellow=clip_board((r>170)&(g>135)&(r>g+5)&(b<g-60))
        gray=(abs(r-g)<35)&(g>=b-5)&(r>125)&(g>135)&(b>115)
        _,_,stats,_=cv2.connectedComponentsWithStats(yellow.astype('uint8'),8)
        locks=[];pending=[]
        for x,y,w,h,area in stats[1:]:
            if not (.5*sx<w<1.2*sx and .45*sy<h<1.15*sy and .7<w/h<1.4 and area>.2*sx*sy):continue
            ring=[sample(gray,x+w*.5,y-h*.42,w*.55,h*.22),
                  sample(gray,x+w*.18,y-h*.19,w*.22,h*.34),
                  sample(gray,x+w*.82,y-h*.19,w*.22,h*.34)]
            if min(ring)<.38:continue
            face=Image.fromarray(image[y:y+h,x:x+w]);key=(face.size,face.tobytes())
            lock={'box':list(map(int,(x,y,w,h))),'ring_score':min(ring)}
            if key in self.cache:lock['count']=self.cache[key]
            else:
                data=io.BytesIO();face.save(data,format='PNG')
                pending.append((lock,key,base64.b64encode(data.getvalue()).decode('ascii')))
            locks.append(lock)
        if pending:
            try:
                result=subprocess.run([os.path.abspath(sys.executable),str(Path(__file__).with_name('lock_ocr.py'))],
                                      input=json.dumps([v[2] for v in pending]),text=True,capture_output=True,timeout=4)
                payload=json.loads(result.stdout)
                if result.returncode or 'error' in payload:raise ValueError(payload.get('error','锁数字读取失败'))
                counts=payload['counts']
                if len(counts)!=len(pending):raise ValueError('锁数量不一致')
                for (lock,key,_),count in zip(pending,counts):
                    lock['count']=count;self.cache[key]=count
                while len(self.cache)>128:self.cache.pop(next(iter(self.cache)))
            except subprocess.TimeoutExpired as error:
                raise RecognitionError('锁数字读取超时，已跳过本帧；点击保持暂停') from error
            except (ValueError,KeyError,OSError) as error:
                raise RecognitionError(f'锁数字尚未确定：{error}') from error
        occluded=np.zeros(image.shape[:2],bool)
        for lock in locks:
            x,y,w,h=lock['box'];pad=max(1,round(sx*.08))
            occluded[max(0,y-round(h*.57)):y+h+pad,max(0,x-pad):x+w+pad]=True
        return locks,occluded


def unlocked_visible(image,pig,grid):
    """A remaining yellow lock face prevents dispatch even after predicted exits."""
    from solver import half
    if image is None:return False
    im=np.asarray(image.convert('RGB').resize((379,835))).astype('int16')
    x=grid['x']['origin']+pig['u']*grid['x']['scale'];y=grid['y']['origin']+pig['v']*grid['y']['scale']
    hx,hy=half(pig)
    rx,ry=max(1,hx*grid['x']['scale']*.9),max(1,hy*grid['y']['scale']*.9)
    a=im[max(178,round(y-ry)):min(700,round(y+ry)+1),max(20,round(x-rx)):min(355,round(x+rx)+1)]
    if not a.size:return False
    r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
    yellow=(r>170)&(g>135)&(r>g+5)&(b<g-60)
    _,_,stats,_=cv2.connectedComponentsWithStats(yellow.astype('uint8'),8)
    return max((s[4] for s in stats[1:]),default=0)<.2*4*grid['x']['scale']*grid['y']['scale']
