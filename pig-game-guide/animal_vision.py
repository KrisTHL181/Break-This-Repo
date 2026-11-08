"""Foreground appearance shared by recognition and live animal tracking."""
import cv2
import numpy as np


def elephant_mask(rgb):
    a=np.asarray(rgb).astype(np.int16)
    r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
    return (b>r+12)&(g>r+5)&(r>80)&(b-g<35)


def board_elephant_mask(rgb):
    mask=elephant_mask(rgb).copy()
    mask[:178]=0;mask[700:]=0;mask[:,:20]=0;mask[:,355:]=0
    _,labels,stats,_=cv2.connectedComponentsWithStats(mask.astype('uint8'),8)
    for i,(x,y,w,h,area) in enumerate(stats[1:],1):
        # Narrow fragments cut by the board edge are terrain trim, not a
        # two-cell-wide stationary elephant. Also shared by preflight/tracking.
        if (x==20 or x+w==355) and w<=12:mask[labels==i]=False
    return mask


def pig_appearance(rgb):
    """Match shape and facial shading without tying scores to terrain hue."""
    return cv2.cvtColor(np.asarray(rgb),cv2.COLOR_RGB2GRAY)


def pig_body_appearance(rgb):
    """Exclude terrain and adjacent yellow ducks for a second direction check."""
    a=np.asarray(rgb).astype(np.int16)
    r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
    mask=(r>g+25)&(r>b+20)&(b>90)&(b>g-35)
    result=pig_appearance(rgb)
    result[~mask]=0
    return result


def sand_scene(rgb):
    a=np.asarray(rgb).astype(np.int16)[178:700,20:355]
    r,g,b=a[:,:,0],a[:,:,1],a[:,:,2]
    return ((r>g+15)&(g>b+30)&(r>170)&(g>120)).mean()>.12


def elephant_appearance(rgb):
    mask=elephant_mask(rgb)
    result=np.zeros_like(rgb)
    result[mask]=np.asarray(rgb)[mask]
    return result
