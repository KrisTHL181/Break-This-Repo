"""Automatic recognition for the observed cardinal pig, duck and elephant skins.

Pig templates were labelled from level 5; ducks include levels 11 and 25.
Real long-body and elephant variants supplement the original appearances.
No current level coordinates or move order are embedded in the recognizer.
"""
from pathlib import Path
import time
import cv2
import numpy as np
from PIL import Image
from animal_vision import pig_appearance, pig_body_appearance, elephant_appearance, elephant_mask, sand_scene


class RecognitionError(RuntimeError):
    pass


class Recognizer:
    def __init__(self, templates=None):
        path = templates or Path(__file__).with_name("templates.npz")
        self.templates = dict(np.load(path))
        self.scaled_pigs = {d:[pig_appearance(cv2.resize(t,(round(t.shape[1]*scale),round(t.shape[0]*scale))))
            for scale in (1.0, 1.065) for t in self.templates["pig_"+d]] for d in "NSEW"}
        self.pig_bodies = {d:[pig_body_appearance(cv2.resize(t,(round(t.shape[1]*scale),round(t.shape[0]*scale))))
            for scale in (1.0, 1.065) for t in self.templates["pig_"+d]] for d in "NSEW"}
        self.long_templates = {}
        self.elephant_templates={d:[] for d in 'NSEW'}
        for name,template in np.load(Path(__file__).with_name('elephant_templates.npz')).items():
            self.elephant_templates[name.split('_')[0]].append(template)
        for direction in "NSEW":
            axis = 0 if direction in "NS" else 1
            extended = []
            for template in self.templates["pig_"+direction]:
                middle = np.take(template, range(20, 31), axis=axis)
                middle = cv2.resize(middle, (25, 37) if axis == 0 else (37, 25))
                extended.append(np.concatenate((np.take(template, range(20), axis=axis),
                    middle, np.take(template, range(31, 51), axis=axis)), axis=axis))
            self.long_templates[direction] = extended
        # Real long-body shading differs from the synthesized middle strip.
        # Keep measured full-body variants alongside the original templates.
        long_path=Path(__file__).with_name('long_templates.npz')
        self.beach_long_templates=dict(np.load(long_path)) if long_path.exists() else {}

    def _cluster_pigs(self, im, bounds):
        """Recognize mixed orientations inside a connected cluster without
        slicing all pigs along one axis (hints can join different rows)."""
        x,y,w,h=map(int,bounds)
        x0,y0=max(0,x-9),max(0,y-9)
        crop=im[y0:min(835,y+h+10),x0:min(379,x+w+10)]
        candidates=[]
        for direction in 'NSEW':
            for template in self.scaled_pigs[direction]:
                th,tw=template.shape[:2]
                if crop.shape[0]<th or crop.shape[1]<tw:continue
                scores=cv2.matchTemplate(crop,template,cv2.TM_CCOEFF_NORMED)
                peaks=np.argwhere((scores==cv2.dilate(scores,np.ones((11,11),np.uint8))) & (scores>=.72))
                for yy,xx in peaks:
                    cx,cy=int(xx)+x0+tw//2,int(yy)+y0+th//2
                    if not (x-3<=cx<x+w+3 and y-3<=cy<y+h+3):continue
                    candidates.append((float(scores[yy,xx]),direction,cx,cy))
        chosen=[]
        for score,d,cx,cy in sorted(candidates,reverse=True):
            opposite={'N':'S','S':'N','E':'W','W':'E'}[d]
            other=max((s for s,od,ox,oy in candidates if od==opposite and abs(ox-cx)<6 and abs(oy-cy)<6),default=0)
            if score-other<.04:continue
            hx,hy=(11,23) if d in 'NS' else (23,11)
            if any(abs(cx-p['x'])<hx+(11 if p['d'] in 'NS' else 23)-3 and
                   abs(cy-p['y'])<hy+(23 if p['d'] in 'NS' else 11)-3 for p in chosen):continue
            chosen.append({'kind':'pig','length':2,'d':d,'x':cx,'y':cy,
                           'match_score':round(score,4),'direction_margin':round(score-other,4)})
        return chosen

    def _long_pigs(self, im, pink, beach=False):
        """Match complete three-cell bodies, preserving head/tail appearance."""
        candidates = []
        # Later boards enlarge sprites slightly. Compare complete bodies at
        # both observed scales instead of lowering the confidence threshold.
        for scale in (1.0, 1.065):
            templates = {d:[pig_appearance(cv2.resize(t, (round(t.shape[1]*scale), round(t.shape[0]*scale))))
                           for t in ts+(list(self.beach_long_templates.get(d,[])) if beach else [])]
                         for d, ts in self.long_templates.items()}
            scores = {d: np.maximum.reduce([cv2.matchTemplate(im[178:700], t, cv2.TM_CCOEFF_NORMED)
                      for t in ts]) for d, ts in templates.items()}
            for direction, values in scores.items():
                peaks = np.argwhere((values == cv2.dilate(values, np.ones((19, 19), np.uint8))) & (values >= .84))
                for yy, xx in peaks:
                    vertical = direction in "NS"
                    h, w = templates[direction][0].shape[:2]
                    x, y = int(xx)+w//2, int(yy)+178+h//2
                    opposite = {"N":"S", "S":"N", "E":"W", "W":"E"}[direction]
                    other = scores[opposite][max(0, yy-4):yy+5, max(0, xx-4):xx+5].max()
                    margin = float(values[yy, xx]-other)
                    hx, hy = (6, round(29*scale)) if vertical else (round(29*scale), 6)
                    if margin < .06 or pink[y-hy:y+hy+1, x-hx:x+hx+1].mean() < .90:
                        continue
                    candidates.append({"kind":"pig", "d":direction, "length":3, "x":x, "y":y,
                        "sprite_scale":scale,"match_score":round(float(values[yy, xx]), 4), "direction_margin":round(margin, 4)})
        accepted = []
        for p in sorted(candidates, key=lambda p:-p["match_score"]):
            if not any(np.hypot(p["x"]-q["x"], p["y"]-q["y"]) < 24 for q in accepted):
                accepted.append(p)
        return accepted

    def _elephants(self, im):
        mask=elephant_mask(im).astype(np.uint8)
        mask[:178]=0;mask[700:]=0;mask[:,:20]=0;mask[:,355:]=0
        _,labels,stats,centres=cv2.connectedComponentsWithStats(mask,8)
        appearance=elephant_appearance(im)
        result=[]
        for k,(x,y,w,h,area) in enumerate(stats[1:],1):
            if area<300:continue
            if not (1800<area<9000 and 60<=max(w,h)<=115 and 40<=min(w,h)<=85):
                raise RecognitionError('出现尚未识别的蓝灰色物体，小象识别暂停')
            x0,y0=max(0,x-8),max(0,y-8)
            crop=appearance[y0:y+h+8,x0:x+w+8]
            scores=[]
            for d in ('NS' if h>w else 'EW'):
                for template in self.elephant_templates[d]:
                    for scale in (1.,1.065,.94,.88,.82):
                        t=cv2.resize(template,(round(template.shape[1]*scale),round(template.shape[0]*scale)))
                        if crop.shape[0]<t.shape[0] or crop.shape[1]<t.shape[1]:continue
                        _,score,_,_=cv2.minMaxLoc(cv2.matchTemplate(crop,t,cv2.TM_CCOEFF_NORMED))
                        scores.append((score,d))
            scores.sort(reverse=True)
            if not scores:raise RecognitionError('小象身体未完整显示')
            score,d=scores[0];margin=score-max(s for s,other in scores if other!=d)
            if score<.8 or margin<.1:raise RecognitionError('小象朝向不明确，暂停点击')
            cx,cy=centres[k]
            result.append({'kind':'elephant','d':d,'width':2,'length':3,
                           'x':float(cx),'y':float(cy),'match_score':round(score,4),
                           'direction_margin':round(margin,4)})
        return result

    @staticmethod
    def _axis(values, parity):
        values, parity = np.asarray(values), np.asarray(parity)
        if len(values) < 3 or np.ptp(values-parity*13.1) < 2:
            scale = 13.1
            origin = float(values[0]-parity[0]*scale)
            indices = 2*np.rint(((values-origin)/scale-parity)/2)+parity
            if np.max(np.abs(values-origin-scale*indices)) > 3.2:
                raise RecognitionError("物体正在移动")
            shift = int(2*np.floor(indices.min()/2))
            return {"origin": origin+shift*scale, "scale": scale}, (indices-shift).astype(int)
        best = None
        for initial in np.arange(11.8, 14.41, 0.05):
            period = 2 * initial
            phase = np.angle(np.exp(2j * np.pi * (values-parity*initial)/period).mean())
            origin = (phase % (2*np.pi)) * period/(2*np.pi)
            indices = 2*np.rint(((values-origin)/initial-parity)/2)+parity
            design = np.column_stack([np.ones(len(values)), indices])
            origin, scale = np.linalg.lstsq(design, values, rcond=None)[0]
            residual = values-origin-scale*indices
            score = float(np.mean(residual**2))
            if 11.6 < scale < 14.6 and (best is None or score < best[0]):
                best = (score, origin, scale, indices, residual)
        if best is None or np.max(np.abs(best[4])) > 3.2:
            raise RecognitionError("棋盘正在移动，或无法拟合方格")
        _, origin, scale, indices, residual = best
        shift = int(2*np.floor(indices.min()/2))
        return {"origin": float(origin+shift*scale), "scale": float(scale)}, (indices-shift).astype(int)

    def read(self, source, grid=None):
        started = time.monotonic()
        image = source if isinstance(source, Image.Image) else Image.open(source)
        original_size = image.size
        if abs(image.width/image.height - 379/835) > .035:
            raise RecognitionError("镜像窗口比例已改变")
        im = np.asarray(image.convert("RGB").resize((379, 835)))
        beach=sand_scene(im)
        match_im=pig_appearance(im)
        elephants=self._elephants(im)
        a = im.astype(np.int16)
        pink = ((a[:, :, 0] > a[:, :, 1]+25)
                & (a[:, :, 0] > a[:, :, 2]+20) & (a[:, :, 2] > 90)
                & (a[:, :, 2] > a[:, :, 1]-35)).astype(np.uint8)
        pink[:178] = 0
        pink[700:] = 0
        pink[:, :20] = 0
        pink[:, 355:] = 0
        eroded = cv2.erode(pink, np.ones((3, 3), np.uint8))
        _, labels, stats, _ = cv2.connectedComponentsWithStats(eroded, 8)
        long_pigs = self._long_pigs(match_im, pink,beach) if any(max(s[2:4]) > 53 and s[4] > 950 for s in stats[1:]) else []
        if long_pigs:
            eroded = eroded.copy()
            for p in long_pigs:
                hx, hy = (15, 38) if p["d"] in "NS" else (38, 15)
                eroded[p["y"]-hy:p["y"]+hy+1, p["x"]-hx:p["x"]+hx+1] = 0
            _, labels, stats, _ = cv2.connectedComponentsWithStats(eroded, 8)
        parts = []
        cluster_pigs = []
        for k, (x, y, width, height, area) in enumerate(stats[1:], 1):
            if area < 200:
                continue
            yy, xx = np.where(labels == k)
            groups = [(xx, yy)]
            if area > 950 and min(width, height) > 28:
                cluster_pigs.extend(self._cluster_pigs(match_im,(x,y,width,height)))
                continue
            for xs, ys in groups:
                if not len(xs) or max(np.ptp(xs), np.ptp(ys)) > 53:
                    raise RecognitionError("发现尚未支持的物体或重叠动画")
                parts.append((round(xs.mean()), round(ys.mean()), np.ptp(ys) > np.ptp(xs)))

        pigs = list(long_pigs)+cluster_pigs
        for p in long_pigs:
            center = a[p["y"]-6:p["y"]+7, p["x"]-6:p["x"]+7]
            if float((center.max(2) < 110).mean()) > .07:
                raise RecognitionError("长猪出现尚未支持的标记，暂停提示")
        for x, y, vertical in parts:
            hx, hy = (12, 25) if vertical else (25, 12)
            roi = match_im[y-hy-6:y+hy+7, x-hx-6:x+hx+7]
            if roi.shape[:2] != (hy*2+13, hx*2+13):
                raise RecognitionError("物体位于棋盘边缘，等待动画结束")
            scores = []
            for direction in ("NS" if vertical else "EW"):
                for template in self.scaled_pigs[direction]:
                    result = cv2.matchTemplate(roi, template, cv2.TM_CCOEFF_NORMED)
                    _, score, _, location = cv2.minMaxLoc(result)
                    scores.append((score, direction, x-hx-6+location[0]+template.shape[1]//2, y-hy-6+location[1]+template.shape[0]//2))
            scores.sort(reverse=True)
            score, direction, cx, cy = scores[0]
            margin = score-max(s[0] for s in scores if s[1] != direction)
            if score < .72 or margin < .04 or (beach and (score < .85 or margin < .08)):
                # Nearby ducks and sand can dominate the rectangular crop.
                # Require a stronger, independent silhouette match in the same
                # direction before accepting an otherwise ambiguous animal.
                body_roi=pig_body_appearance(im[y-hy-6:y+hy+7,x-hx-6:x+hx+7])
                body_scores=[]
                for d in ('NS' if vertical else 'EW'):
                    for template in self.pig_bodies[d]:
                        values=cv2.matchTemplate(body_roi,template,cv2.TM_CCOEFF_NORMED)
                        _,s,_,loc=cv2.minMaxLoc(values)
                        body_scores.append((s,d,x-hx-6+loc[0]+template.shape[1]//2,y-hy-6+loc[1]+template.shape[0]//2))
                body_scores.sort(reverse=True)
                bs,bd,bx,by=body_scores[0]
                bm=bs-max(s[0] for s in body_scores if s[1]!=bd)
                if bd==direction and bs>=.84 and bm>=.08:
                    score,margin,cx,cy=bs,bm,bx,by
            if score < .72 or margin < .04:
                raise RecognitionError("猪的方向不清晰，等待稳定画面")
            center = a[cy-6:cy+7, cx-6:cx+7]
            if float((center.max(2) < 110).mean()) > .07:
                raise RecognitionError("出现箭头、炸弹或新元素，暂停提示")
            pigs.append({"kind": "pig", "length": 2, "d": direction, "x": int(cx), "y": int(cy),
                         "match_score": round(float(score), 4), "direction_margin": round(float(margin), 4)})

        # A cluster crop may include an adjacent component's head/body. Merge
        # only near-identical detections; contradictory directions still fail
        # the overlap check below instead of silently replacing one another.
        unique=[]
        for p in sorted(pigs,key=lambda p:-p['match_score']):
            if not any(p['d']==q['d'] and p['length']==q['length'] and np.hypot(p['x']-q['x'],p['y']-q['y'])<6 for q in unique):
                unique.append(p)
        pigs=unique

        raw_duck_scores = np.maximum.reduce([cv2.matchTemplate(im, t, cv2.TM_CCOEFF_NORMED)
                                            for t in self.templates["duck"]])
        gray_duck_scores = np.maximum.reduce([cv2.matchTemplate(match_im, pig_appearance(t), cv2.TM_CCOEFF_NORMED)
                                             for t in self.templates["duck"]])
        scores=np.maximum(raw_duck_scores,gray_duck_scores)
        scores[:174] = 0
        scores[685:] = 0
        yellow_body = ((a[:, :, 0] > 160) & (a[:, :, 1] > 150)
                       & (a[:, :, 0] >= a[:, :, 1]-15) & (a[:, :, 2] < a[:, :, 1]-50)
                       & (4*(a[:,:,1]-a[:,:,2])>3*(a[:,:,0]-a[:,:,2])))
        # Gray shading survives a change from grass to sand. Its stronger
        # threshold still requires the independent yellow-body checks below.
        candidates = np.argwhere((scores == cv2.dilate(scores, np.ones((15, 15), np.uint8)))
                                 & ((raw_duck_scores > .62) | (gray_duck_scores > .78)))
        ducks = []
        for yy, xx in sorted(candidates, key=lambda q: -scores[tuple(q)]):
            x, y = int(xx)+12, int(yy)+13
            # A different surrounding layout or duck animation can lower the
            # rectangular template score. Require an independent yellow body,
            # rather than accepting a weak match on pig faces or scenery.
            if int(yellow_body[y-13:y+14, x-12:x+13].sum()) < 200:
                continue
            if float(yellow_body[y-8:y+9, x-8:x+9].mean()) < .3:
                continue
            if any(np.hypot(x-p["x"], y-p["y"]) < 18 for p in ducks):
                continue
            ducks.append({"kind": "duck", "d": "", "x": x, "y": y,
                          "match_score": round(float(scores[yy, xx]), 4)})

        covered = np.zeros(pink.shape, bool)
        for p in pigs+ducks+elephants:
            x, y = p["x"], p["y"]
            hx, hy = ((15, 15) if p["kind"] == "duck" else
                      (15, 28+13*(p.get("length", 2)-2)) if p["d"] in ("N", "S") else (28+13*(p.get("length", 2)-2), 15))
            if p['kind']=='elephant':hx,hy=(38,54) if p['d'] in 'NS' else (54,38)
            x,y=round(x),round(y)
            covered[max(0, y-hy):y+hy+1, max(0, x-hx):x+hx+1] = True
        uncovered = (pink.astype(bool) & ~covered).astype(np.uint8)
        _, _, unexplained, _ = cv2.connectedComponentsWithStats(uncovered, 8)
        if any(s[4] >= 80 for s in unexplained[1:]):
            raise RecognitionError("还有未识别的猪，暂停提示")
        # A missing duck must not silently become empty space in the simulator.
        yellow = yellow_body.copy()
        yellow[:178] = False
        yellow[700:] = False
        yellow[:, :20] = False
        yellow[:, 355:] = False
        _, _, unknown_yellow, _ = cv2.connectedComponentsWithStats((yellow & ~covered).astype(np.uint8), 8)
        if any(s[4] > 160 and min(s[2:4]) > 15 for s in unknown_yellow[1:]):
            raise RecognitionError("鸭子尚未完整识别，等待稳定画面")

        for p in pigs:
            center=a[p['y']-6:p['y']+7,p['x']-6:p['x']+7]
            if float((center.max(2)<110).mean())>.07:
                raise RecognitionError('出现尚未支持的箭头或炸弹标记，已有方案保留')
            hx, hy = (17, 30+13*(p.get("length",2)-2)) if p["d"] in "NS" else (30+13*(p.get("length",2)-2), 17)
            patch = a[max(0,p["y"]-hy):p["y"]+hy+1, max(0,p["x"]-hx):p["x"]+hx+1]
            red = (patch[:,:,0] > 210) & (patch[:,:,1] < 100) & (patch[:,:,2] < 100)
            p["hint_animation"] = int(red.sum()) >= 12
        objects = sorted(pigs+ducks+elephants, key=lambda p: (p["y"], p["x"], p["kind"]))
        if not objects:
            return {"objects": [], "grid": grid, "size": list(original_size), "recognition_ms": 1000*(time.monotonic()-started)}
        from solver import half
        px = [(half(p)[0]-1)%2 for p in objects]
        py = [(half(p)[1]-1)%2 for p in objects]
        if grid is None:
            fitting = [i for i,p in enumerate(objects) if not p.get("hint_animation") and p['kind']!='elephant'] or list(range(len(objects)))
            gx, _ = self._axis([objects[i]["x"] for i in fitting], [px[i] for i in fitting])
            gy, _ = self._axis([objects[i]["y"] for i in fitting], [py[i] for i in fitting])
            grid = {"x": gx, "y": gy}
        if grid is not None:
            u = 2*np.rint(((np.array([p["x"] for p in objects])-grid["x"]["origin"])/grid["x"]["scale"]-px)/2)+px
            v = 2*np.rint(((np.array([p["y"] for p in objects])-grid["y"]["origin"])/grid["y"]["scale"]-py)/2)+py
        occupied = set()
        for i, p in enumerate(objects):
            p.update(id=f"{p['kind']}_{i+1:03}", u=int(u[i]), v=int(v[i]))
            pred = [grid["x"]["origin"]+p["u"]*grid["x"]["scale"], grid["y"]["origin"]+p["v"]*grid["y"]["scale"]]
            dx, dy = abs(p["x"]-pred[0]), abs(p["y"]-pred[1])
            if p.get("hint_animation"):
                along, cross = (dy, dx) if p["d"] in "NS" else (dx, dy)
                scale = grid["y" if p["d"] in "NS" else "x"]["scale"]
                if along > scale*.85 or cross > 4:
                    raise RecognitionError("提示猪偏移过大，无法确定原始格位")
            elif p['kind']=='elephant':
                # The trunk and ears bias the blue body centroid on both axes.
                # Keep a per-axis 5 px bound (well below the 26 px cell pitch);
                # the resting grid is fitted independently from the pigs.
                if max(dx,dy)>5:
                    raise RecognitionError("小象位置正在改变")
            elif np.hypot(dx, dy) > 4:
                raise RecognitionError("物体位置正在改变")
            from solver import occupied_cells
            for cell in occupied_cells(p):
                if cell in occupied:
                    raise RecognitionError("检测到重叠物体，等待动画结束")
                occupied.add(cell)
        return {"objects": objects, "grid": grid, "size": list(original_size),
                "recognition_ms": 1000*(time.monotonic()-started)}


def fingerprint(objects):
    from solver import half
    return tuple(sorted((p['kind'],p.get('d',''),*half(p),p['u'],p['v'],p.get('lock_remaining',0)) for p in objects))
