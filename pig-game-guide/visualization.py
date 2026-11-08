"""Native computation view. Paints asynchronously; never paces the solver."""
import AppKit as AK
from Foundation import NSObject, NSTimer
from solver import half

PHASES = {
    "begin": ("开始计算", "从自动识别的棋盘开始搜索"),
    "visit": ("检查局面", "寻找可以直接离场的猪"),
    "exit": ("直接离场", "前方没有阻挡，可以直接移走"),
    "branch": ("尝试挪位", "先停在障碍前，再检查后续路线"),
    "backtrack": ("回溯", "这条路线走不通，返回尝试其他挪位"),
    "prune": ("跳过重复局面", "这个局面已经搜索过"),
    "dead_end": ("当前路线无解", "没有可继续推进的移动"),
    "replay": ("校验完整解法", "逐步回放，确认最终棋盘能够清空"),
    "done": ("计算完成", "完整解法已通过回放校验"),
    "limit": ("搜索达到上限", "尚未找到完整解法，绿点保持暂停"),
}


def color(value):
    value = value.lstrip("#")
    return AK.NSColor.colorWithCalibratedRed_green_blue_alpha_(
        int(value[:2], 16)/255, int(value[2:4], 16)/255, int(value[4:], 16)/255, 1)


def text(value, x, y, size=14, ink="#253247", width=360, bold=False):
    font = AK.NSFont.boldSystemFontOfSize_(size) if bold else AK.NSFont.systemFontOfSize_(size)
    AK.NSString.stringWithString_(str(value)).drawInRect_withAttributes_(
        AK.NSMakeRect(x, y, width, 55), {AK.NSFontAttributeName: font, AK.NSForegroundColorAttributeName: color(ink)})


def rounded(rect, fill, radius=10, stroke=None):
    path = AK.NSBezierPath.bezierPathWithRoundedRect_xRadius_yRadius_(AK.NSMakeRect(*rect), radius, radius)
    color(fill).setFill()
    path.fill()
    if stroke:
        color(stroke).setStroke()
        path.setLineWidth_(1)
        path.stroke()


class ComputationView(AK.NSView):
    def isFlipped(self):
        return True

    def drawRect_(self, rect):
        self.owner.draw(self.bounds())


class ViewActions(NSObject):
    def tick_(self, timer):
        self.owner.tick()

    def changeMode_(self, sender):
        self.owner.mode = int(sender.selectedSegment())
        self.owner.slider.setDoubleValue_(0)
        self.owner.tick(force=True)

    def scrub_(self, sender):
        self.owner.tick(force=True)


class ComputationWindow:
    def __init__(self, recorder, show=True):
        self.recorder = recorder
        self.mode = 0
        self.snapshot = recorder.snapshot()
        self.last_version = -1
        visible = AK.NSScreen.mainScreen().visibleFrame()
        width = min(960, visible.size.width-440)
        width = max(720, width)
        height = min(760, visible.size.height-90)
        frame = AK.NSMakeRect(visible.origin.x+35, visible.origin.y+visible.size.height-height-40, width, height)
        self.window = AK.NSWindow.alloc().initWithContentRect_styleMask_backing_defer_(
            frame, AK.NSWindowStyleMaskTitled | AK.NSWindowStyleMaskClosable | AK.NSWindowStyleMaskMiniaturizable,
            AK.NSBackingStoreBuffered, False)
        self.window.setTitle_("猪棋盘 · 计算过程")
        self.window.setAppearance_(AK.NSAppearance.appearanceNamed_(AK.NSAppearanceNameAqua))
        self.window.setReleasedWhenClosed_(False)
        self.view = ComputationView.alloc().initWithFrame_(AK.NSMakeRect(0, 0, width, height))
        self.view.owner = self
        self.window.setContentView_(self.view)
        self.actions = ViewActions.alloc().init()
        self.actions.owner = self
        self.segments = AK.NSSegmentedControl.alloc().initWithFrame_(AK.NSMakeRect(24, 24, 470, 30))
        self.segments.setSegmentCount_(3)
        for i, label in enumerate(("实时计算 / 当前提示", "搜索轨迹", "完整解法")):
            self.segments.setLabel_forSegment_(label, i)
            self.segments.setWidth_forSegment_(155, i)
        self.segments.setSelectedSegment_(0)
        self.segments.setTarget_(self.actions)
        self.segments.setAction_("changeMode:")
        self.view.addSubview_(self.segments)
        self.slider = AK.NSSlider.alloc().initWithFrame_(AK.NSMakeRect(26, height-49, width-55, 24))
        self.slider.setMinValue_(0)
        self.slider.setMaxValue_(1)
        self.slider.setTarget_(self.actions)
        self.slider.setAction_("scrub:")
        self.slider.setContinuous_(True)
        self.view.addSubview_(self.slider)
        # Only the display polls at 30 Hz. The computation thread has no waits.
        self.timer = NSTimer.scheduledTimerWithTimeInterval_target_selector_userInfo_repeats_(
            1/30, self.actions, "tick:", None, True)
        if show:
            self.show()

    def show(self):
        self.window.orderFrontRegardless()
        self.tick(force=True)

    def tick(self, force=False):
        snapshot = self.recorder.snapshot()
        if not force and snapshot["version"] == self.last_version:
            return
        self.snapshot, self.last_version = snapshot, snapshot["version"]
        count = len(snapshot["events"]) if self.mode == 1 else len(snapshot["plan"]["states"]) if snapshot["plan"] else 0
        self.slider.setMaxValue_(max(0, count-1))
        self.slider.setEnabled_(self.mode != 0 and count > 0)
        if self.window.isVisible():
            self.view.setNeedsDisplay_(True)

    def draw(self, bounds):
        width, height = bounds.size.width, bounds.size.height
        rounded((0, 0, width, height), "#f4f6fa", 0)
        snap, plan = self.snapshot, self.snapshot["plan"]
        event = snap["latest"] or {"phase": "begin", "state": [], "expanded": 0, "elapsed_ms": 0}
        state, action, caption = event["state"], event.get("action"), "计算原速运行；画面异步刷新"
        title, detail = PHASES.get(event["phase"], ("等待计算", ""))
        if self.mode == 1 and snap["events"]:
            i = min(int(self.slider.doubleValue()), len(snap["events"])-1)
            event = snap["events"][i]
            state, action = event["state"], event.get("action")
            title, detail = PHASES.get(event["phase"], ("搜索轨迹", ""))
            caption = f"搜索事件 {event['sequence']} / {snap['total']} · 拖动下方滑条查看真实记录"
            if event.get('reason') == 'invariant_deadlock':
                title, detail = '排除必然死锁', '这条分支有无法清空的相互阻挡区域'
        elif plan is not None:
            i = min(int(self.slider.doubleValue()) if self.mode == 2 else snap["index"], len(plan["states"])-1)
            state = plan["states"][i]
            action = plan["actions"][i] if i < len(plan["actions"]) else None
            title = "完整解法" if self.mode == 2 else "当前绿点提示"
            detail = f"第 {min(i+1, len(plan['actions']))} / {len(plan['actions'])} 步" if action else "棋盘已清空"
            caption = "拖动滑条查看方案；不影响手机上的绿点" if self.mode == 2 else snap["status"]
        if self.mode == 0 and snap.get("live"):
            live = snap["live"]
            state = live["objects"]
            title = "实时位置与碰撞预测"
            caption = live["status"]
            speed = live.get("speed_cells_s")
            detail = f"{live['moving']} 只移动中 · {speed:.1f} 格/秒" if speed else f"{live['moving']} 只移动中 · 测量速度"
        text(title, 28, 72, 23, bold=True)
        text(detail, width*.64, 76, 13, "#5b687c", width=width*.33)
        left, top, board_width, board_height = 26, 112, width*.62-30, height-212
        rounded((left, top, board_width, board_height), "#ffffff", 14, "#dce2eb")
        all_objects = snap["initial"] or state
        if all_objects:
            min_u = min(p["u"]-half(p)[0] for p in all_objects)-1
            max_u = max(p["u"]+half(p)[0] for p in all_objects)+1
            min_v = min(p["v"]-half(p)[1] for p in all_objects)-1
            max_v = max(p["v"]+half(p)[1] for p in all_objects)+1
            scale = min((board_width-24)/(max_u-min_u), (board_height-24)/(max_v-min_v))
            ox = left+(board_width-(max_u-min_u)*scale)/2-min_u*scale
            oy = top+(board_height-(max_v-min_v)*scale)/2-min_v*scale
            grid = AK.NSBezierPath.bezierPath()
            for u in range(int(min_u)+1, int(max_u), 2):
                grid.moveToPoint_((ox+u*scale, oy+min_v*scale))
                grid.lineToPoint_((ox+u*scale, oy+max_v*scale))
            for v in range(int(min_v)+1, int(max_v), 2):
                grid.moveToPoint_((ox+min_u*scale, oy+v*scale))
                grid.lineToPoint_((ox+max_u*scale, oy+v*scale))
            color("#edf0f5").setStroke()
            grid.setLineWidth_(.5)
            grid.stroke()
            for p in state:
                x, y = ox+p["u"]*scale, oy+p["v"]*scale
                hx, hy = half(p)
                active = action is not None and p["id"] == action["id"]
                blocker = action is not None and p["id"] == action.get("blocker")
                fill = "#91c9f7" if p.get("tracking") else "#a1edc6" if active else "#ffd28d" if blocker else "#ffdf70" if p["kind"] == "duck" else "#a8bbc9" if p['kind']=='elephant' else "#f3abc0"
                rounded((x-hx*scale+2, y-hy*scale+2, hx*scale*2-4, hy*scale*2-4), fill, max(3, scale*.5), "#26a269" if active else None)
                divisions=AK.NSBezierPath.bezierPath()
                for k in range(1,hx):
                    xx=x-hx*scale+2*k*scale
                    divisions.moveToPoint_((xx,y-hy*scale+4));divisions.lineToPoint_((xx,y+hy*scale-4))
                for k in range(1,hy):
                    yy=y-hy*scale+2*k*scale
                    divisions.moveToPoint_((x-hx*scale+4,yy));divisions.lineToPoint_((x+hx*scale-4,yy))
                color('#ffffff').colorWithAlphaComponent_(.45).setStroke()
                divisions.setLineWidth_(.8);divisions.stroke()
                if p['kind']=='elephant':text('象',x-hx*scale+5,y-hy*scale+3,max(9,scale*.65),'#34495e')
                if p["kind"] == "duck":
                    for dx in (-.3, .3):
                        color("#66551b").setFill()
                        AK.NSBezierPath.bezierPathWithOvalInRect_(AK.NSMakeRect(x+dx*scale-1.5, y-3, 3, 3)).fill()
                    continue
                dx, dy = {"N": (0, -1), "S": (0, 1), "E": (1, 0), "W": (-1, 0)}[p["d"]]
                length = scale*.75
                arrow = AK.NSBezierPath.bezierPath()
                arrow.moveToPoint_((x-dx*length*.65, y-dy*length*.65))
                arrow.lineToPoint_((x+dx*length, y+dy*length))
                for side in (-1, 1):
                    arrow.moveToPoint_((x+dx*length-dx*5+dy*side*4, y+dy*length-dy*5-dx*side*4))
                    arrow.lineToPoint_((x+dx*length, y+dy*length))
                color("#5a3247").setStroke()
                arrow.setLineWidth_(1.8)
                arrow.stroke()
                if p.get('lock_remaining',0):
                    # Keep the direction at the head; the badge shows the
                    # simulator's remaining exits at this exact replay step.
                    text({'N':'↑','S':'↓','E':'→','W':'←'}[p['d']],x+dx*scale*.9-4,y+dy*scale*.9-9,12,'#5a3247')
                    rounded((x-13,y-10,26,20),'#ffe071',4,'#947127')
                    text(p['lock_remaining'],x-11,y-8,12,'#5e4a18',width=24,bold=True)
        else:
            text("等待棋盘", left+35, top+35, 20, "#8793a4")
        sx, sw = width*.64, width*.33
        latest = event if self.mode == 1 else snap["latest"] or event
        rows = [("搜索状态", latest.get("expanded", 0)), ("回溯次数", latest.get("backtracks", snap["backtracks"])),
                ("计算用时", f"{(plan['solve_ms'] if plan and self.mode != 1 else latest.get('elapsed_ms', 0)):.1f} ms"),
                ("完整方案", f"{len(plan['actions'])} 步" if plan else "计算中"),
                ("剩余猪 / 鸭 / 象", f"{sum(p['kind']=='pig' for p in state)} / {sum(p['kind']=='duck' for p in state)} / {sum(p['kind']=='elephant' for p in state)}")]
        if plan and plan.get('lattice') and self.mode!=1:
            lattice=plan['lattice']
            rows[1]=('本关格子大小',f"{lattice['pitch_x']:.1f} × {lattice['pitch_y']:.1f} px")
        for j, (label, value) in enumerate(rows):
            y = 123+j*65
            text(label, sx, y, 12, "#718096", width=sw)
            text(value, sx, y+19, 22, bold=True, width=sw)
        y = 463
        rounded((sx, y, sw, 102), "#ffffff", 10)
        text("绿色：正在检查 / 下一步", sx+12, y+13, 12, "#237951", width=sw-20)
        text("蓝：移动中  黄：自动离场  灰：象", sx+12, y+38, 11, width=sw-20)
        text("橙色：前方阻挡者", sx+12, y+63, 12, "#8c632c", width=sw-20)
        if action:
            name = "直接离场" if action["type"] == "exit" else "移动到障碍前"
            text(name, sx, y+118, 15, bold=True, width=sw)
            if action.get("duck_exits"):
                text(f"随后 {len(action['duck_exits'])} 只鸭子自动离场", sx, y+143, 12, width=sw)
        text(caption, 28, height-86, 12, "#647185", width=width-55)
