"""Play a cached plan with live positions, measured speeds, and collision guards."""
import math
import time
from collections import deque,defaultdict
from tracking import MovingPig, route, collision_possible, motion_key
from motion_guard import matches_state,board_visible,masks,present


class PlanPlayback:
    def __init__(self, click, interval=1.0, clock=time.monotonic):
        self.click,self.clock=click,clock
        self.set_interval(interval);self.active=False;self.plan=None
        self.moving=[];self.speeds=deque(maxlen=12);self.speed_history=defaultdict(lambda:deque(maxlen=8));self.confirmed=0
        self.latest_image=None;self.latest_at=0.;self.status='自动点击未开始'
        self.generation=0

    def set_interval(self, seconds):
        seconds=float(seconds)
        if not math.isfinite(seconds) or not .1<=seconds<=3600:raise ValueError('间隔需在 0.1–3600 秒之间')
        self.interval=seconds
        if getattr(self,'active',False):self.due=self.clock()+seconds

    def start(self, plan, grid, index, frame, image=None):
        if not plan or not 0<=index<len(plan['actions']):raise ValueError('尚无可执行的完整解法')
        previous_grid=getattr(self,'grid',None)
        if previous_grid and any(abs(grid[a]['scale']/previous_grid[a]['scale']-1)>.06 for a in ('x','y')):
            self.speeds.clear();self.speed_history.clear()
        self.plan,self.grid,self.index,self.frame=plan,grid,index,dict(frame)
        self.lock_requirements={p['id']:p['lock_remaining'] for p in plan['states'][0] if p.get('lock_remaining',0)}
        pig_ids={p['id'] for p in plan['states'][0] if p['kind']=='pig'}
        self.pig_exit_indices={i for i,a in enumerate(plan['actions']) if a['type']=='exit' and a['id'] in pig_ids}
        self.confirmed=index;self.moving=[];self.active=True;self.generation+=1
        self.latest_image=image;self.latest_at=self.clock() if image else 0
        self.due=self.clock()+self.interval;self.status='准备执行已有方案'
        self.preflight=False
        self.wait_reason='等待第一帧实时画面'

    def stop(self):
        self.active=False;self.generation+=1

    def hold_for_capture(self):
        self.latest_at=0.
        self.wait_reason=self.status='镜像视频流暂时中断，正在重新连接；方案保留'

    def observe(self,image,frame,now=None):
        if not self.active:return
        generation=self.generation
        now=image.info.get('captured_at',self.clock()) if now is None else now
        if frame['id']!=self.frame['id']:
            raise RuntimeError('镜像窗口已重新连接，请确认棋盘后继续')
        if not self.preflight:
            if not matches_state(image,self.plan['states'][self.index],self.grid):
                aligned=next((i for i in range(self.index+1,min(self.index+5,len(self.plan['states'])))
                              if matches_state(image,self.plan['states'][i],self.grid)),None)
                if aligned is None:
                    self.wait_reason=self.status='等待提示消失或画面与缓存步骤一致；方案保留'
                    return
                self.index=self.confirmed=aligned
                if self.index>=len(self.plan['actions']):
                    self.active=False;self.status='缓存方案已完成';return
            self.preflight=True
        active_ids={p.route['id'] for p in self.moving}
        colors=masks(image)
        static=[p for p in self.plan['states'][self.index] if p['id'] not in active_ids]
        if not board_visible(image) or any(not present(p,self.grid,colors) for p in static):
            self.latest_at=0.;self.wait_reason=self.status='提示遮挡或实际棋盘有变化，等待追踪画面恢复；方案保留'
            return
        for pig in list(self.moving):
            pig.guard.set_context(self.plan['states'][self.index])
            pig.observe(image,now)
        if generation!=self.generation:return
        self.latest_image,self.latest_at,self.frame=image,now,dict(frame)
        self.wait_reason=None

    def safe_to_dispatch(self,now):
        if not self.preflight or self.latest_image is None or now-self.latest_at>.45:
            self.status=self.wait_reason or '实时画面已过期，等待新视频帧';return False
        if len(self.moving)>=3:self.status='等待在途猪通过';return False
        upcoming=route(self.plan,self.index,self.grid)
        action=self.plan['actions'][self.index]
        required=self.lock_requirements.get(action['id'],0)
        if required:
            actual_exits=self.actual_pig_exits()
            if actual_exits<required:
                self.status=f'等待解锁：已确认离场 {actual_exits}/{required} 只猪';return False
            from lock_vision import unlocked_visible
            if not unlocked_visible(self.latest_image,upcoming['pig'],self.grid):
                self.status='等待锁消失后再点击';return False
        key=motion_key(upcoming['pig'],upcoming['axis'])
        measured=[p.speed for p in self.moving if p.speed_range(now) and motion_key(p.route['pig'],p.route['axis'])==key] + list(self.speed_history[key])
        next_speed=(min(measured)*.55,max(measured)*1.65) if measured else None
        for pig in self.moving:
            if pig.route['id'] in (upcoming['id'],action.get('blocker')) or pig.route['ducks'] or upcoming['ducks']:
                self.status='等待相关猪或鸭子完成移动';return False
            if collision_possible(pig.predicted_route(now),upcoming,pig.speed_range(now),next_speed):
                self.status='预测路径可能相撞，等待通过冲突区域';return False
        return True

    def actual_pig_exits(self):
        pending={p.index for p in self.moving if not p.complete}
        return sum(i<self.index and i not in pending for i in self.pig_exit_indices)

    def tick(self):
        if not self.active:return None
        now=self.clock()
        for pig in list(self.moving):
            if pig.complete:
                if pig.speed:
                    self.speeds.append(pig.speed)
                    self.speed_history[motion_key(pig.route['pig'],pig.route['axis'])].append(pig.speed)
                self.moving.remove(pig)
        self.confirmed=min((p.index for p in self.moving),default=self.index)
        if self.index>=len(self.plan['actions']):
            if not self.moving:self.active=False;self.status='全部移动已确认完成';return self.index
            self.status='等待最后的猪完成移动';return None
        if now<self.due or not self.safe_to_dispatch(now):return None
        action=self.plan['actions'][self.index]
        x=self.grid['x']['origin']+action['from'][0]*self.grid['x']['scale']
        y=self.grid['y']['origin']+action['from'][1]*self.grid['y']['scale']
        pig=MovingPig(self.plan,self.index,self.grid,self.latest_image,now,
                      min(self.speeds) if self.speeds else None)
        try:self.click(x,y,self.frame)
        except Exception:self.stop();raise
        self.moving.append(pig);self.index+=1;self.due=self.clock()+self.interval
        self.status=f'已点 {self.index} 步，追踪 {len(self.moving)} 只猪'
        return self.index

    def telemetry(self):
        if self.plan is None:return None
        active_ids={p.route['id'] for p in self.moving}
        objects=[dict(p) for p in self.plan['states'][self.index] if p['id'] not in active_ids]
        objects+= [p.model_object() for p in self.moving]
        actual_exits=self.actual_pig_exits()
        for p in objects:
            if p['id'] in self.lock_requirements:
                p['lock_remaining']=max(0,self.lock_requirements[p['id']]-actual_exits)
        return {'objects':objects,'status':self.status,'confirmed':self.confirmed,
                'dispatched':self.index,'moving':len(self.moving),
                'speed_px_s':next((p.speed for p in self.moving if p.speed),None),
                'speed_cells_s':next((p.speed/(2*self.grid['y' if p.route['axis'] else 'x']['scale']) for p in self.moving if p.speed),None)}


def hit_pid(x, y):
    """Ask accessibility hit-testing which app actually receives the point.

    CGWindow bounds alone include transparent full-screen system surfaces.
    """
    import ctypes as C
    ax = C.CDLL('/System/Library/Frameworks/ApplicationServices.framework/ApplicationServices')
    cf = C.CDLL('/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation')
    cf.CFRelease.argtypes = [C.c_void_p]
    ax.AXUIElementCreateSystemWide.restype = C.c_void_p
    ax.AXUIElementCopyElementAtPosition.argtypes = [C.c_void_p, C.c_float, C.c_float, C.POINTER(C.c_void_p)]
    ax.AXUIElementGetPid.argtypes = [C.c_void_p, C.POINTER(C.c_int)]
    root = ax.AXUIElementCreateSystemWide()
    element, pid = C.c_void_p(), C.c_int()
    try:
        error = ax.AXUIElementCopyElementAtPosition(root, x, y, C.byref(element))
        if error or not element.value or ax.AXUIElementGetPid(element, C.byref(pid)):
            raise RuntimeError("无法核对点击接收窗口，自动点击已停止")
        return pid.value
    finally:
        if element.value:
            cf.CFRelease(element)
        cf.CFRelease(root)


def focus_phone(frame):
    import Quartz as Q
    import AppKit as AK
    windows=Q.CGWindowListCopyWindowInfo(Q.kCGWindowListOptionOnScreenOnly,Q.kCGNullWindowID)
    target=next((w for w in windows if w.get(Q.kCGWindowNumber)==frame['id']),None)
    if target is None:raise RuntimeError('镜像窗口已关闭')
    app=AK.NSRunningApplication.runningApplicationWithProcessIdentifier_(target[Q.kCGWindowOwnerPID])
    if app is None or app.bundleIdentifier()!='com.apple.ScreenContinuity':raise RuntimeError('目标窗口不再是 iPhone 镜像')
    app.activateWithOptions_(AK.NSApplicationActivateIgnoringOtherApps)


def native_click(x, y, frame):
    import os
    import Quartz as Q
    if not Q.CGPreflightPostEventAccess():
        raise RuntimeError("Python 尚无辅助功能点击权限")
    windows = Q.CGWindowListCopyWindowInfo(Q.kCGWindowListOptionOnScreenOnly, Q.kCGNullWindowID)
    target = next((w for w in windows if w.get(Q.kCGWindowNumber) == frame["id"]), None)
    if target is None:
        raise RuntimeError("镜像窗口已关闭，自动点击已停止")
    b = target[Q.kCGWindowBounds]
    px, py = b['X']+x*b['Width']/379, b['Y']+y*b['Height']/835
    if hit_pid(px, py) != target[Q.kCGWindowOwnerPID]:
        raise RuntimeError("点击位置由其他应用接收，自动点击已停止")
    for event_type in (Q.kCGEventMouseMoved, Q.kCGEventLeftMouseDown, Q.kCGEventLeftMouseUp):
        event = Q.CGEventCreateMouseEvent(None, event_type, (px, py), Q.kCGMouseButtonLeft)
        if event is None:
            raise RuntimeError("无法创建点击事件")
        Q.CGEventPost(Q.kCGHIDEventTap, event)
