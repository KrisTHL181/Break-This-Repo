#!/usr/bin/env python3
"""Recognize, solve, and mark the next pig with a click-through green dot.

Optional automatic mode plays the precomputed plan at the selected interval.
"""
import argparse
import json
from pathlib import Path
import queue
import tempfile
import threading
import time
import sys

from runtime import ROOT, interval_seconds, require_installation, state_directory


def analyze(path, output):
    from grid_recognition import GridRecognizer as Recognizer
    from controller import PlanTracker
    from solver import solve
    from PIL import Image, ImageDraw
    output.mkdir(parents=True, exist_ok=True)
    board = Recognizer().read(path)
    plan = solve(board["objects"])
    plan['lattice']=board.get('lattice')
    (output/"board.json").write_text(json.dumps(board, ensure_ascii=False, indent=2))
    (output/"plan.json").write_text(json.dumps(plan, ensure_ascii=False, indent=2))
    tracker = PlanTracker()
    tracker.plan, tracker.grid = plan, board["grid"]
    mark = tracker.marker()
    image = Image.open(path).convert("RGB")
    if mark:
        x, y = mark["x"]*image.width/379, mark["y"]*image.height/835
        draw = ImageDraw.Draw(image)
        draw.ellipse((x-7, y-7, x+7, y+7), fill="white")
        draw.ellipse((x-5, y-5, x+5, y+5), fill="#00ed55")
    image.save(output/"next.png")
    print(json.dumps({"pigs": sum(p["kind"] == "pig" for p in board["objects"]),
                      "ducks": sum(p["kind"] == "duck" for p in board["objects"]),
                      "elephants": sum(p["kind"] == "elephant" for p in board["objects"]),
                      "steps": len(plan["actions"]), "recognition_ms": board["recognition_ms"],
                      "solve_ms": plan["solve_ms"], "next": mark}, ensure_ascii=False), flush=True)


def run_live(args):
    from recognition import RecognitionError
    from grid_recognition import GridRecognizer as Recognizer
    from controller import PlanTracker, screen_point
    import fcntl
    import os
    import Quartz
    import AppKit as AK
    from Foundation import NSObject
    from PyObjCTools import AppHelper
    from mac_capture import PhoneCapture, CaptureError

    lock = open(Path(tempfile.gettempdir())/f"pig-game-guide-{os.getuid()}.lock", "w")
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        print("绿点工具已经在运行；可从菜单栏‘猪’暂停或退出。", flush=True)
        return
    lock.write(str(os.getpid()))
    lock.flush()

    app = AK.NSApplication.sharedApplication()
    app.setActivationPolicy_(AK.NSApplicationActivationPolicyAccessory)
    from visual_trace import TraceRecorder
    trace = TraceRecorder()
    visual = None
    if not args.no_visualization:
        from visualization import ComputationWindow
        visual = ComputationWindow(trace)
    state_dir = args.state_dir or state_directory()
    state_dir.mkdir(parents=True, exist_ok=True)
    (state_dir/"auto-status.json").write_text(json.dumps({"active":False,"status":"等待完整解法；自动点击未开始","dispatched":0,"confirmed":0,"time":time.time()},ensure_ascii=False))
    stop, paused = threading.Event(), threading.Event()
    commands = queue.Queue()
    from solve_service import SolveService, SolveCancelled
    jobs=SolveService()
    solve_hold=threading.Event()
    epoch=[0]
    cache={"plan":None}
    last_auto_status=[None]
    from auto_click import PlanPlayback, native_click, focus_phone
    def dispatch_click(x, y, frame):
        panel.orderOut_(None)
        native_click(x, y, frame)
    playback = PlanPlayback(dispatch_click, args.interval)
    auto_armed = threading.Event()
    if args.auto_start:
        auto_armed.set()
    auto_active = threading.Event()

    class MarkerView(AK.NSView):
        def isOpaque(self):
            return False

        def drawRect_(self, rect):
            if args.marker == "box":
                path = AK.NSBezierPath.bezierPathWithRoundedRect_xRadius_yRadius_(self.bounds(), 5, 5)
                AK.NSColor.whiteColor().setStroke()
                path.setLineWidth_(5)
                path.stroke()
                AK.NSColor.colorWithCalibratedRed_green_blue_alpha_(0, .93, .28, 1).setStroke()
                path.setLineWidth_(2.5)
                path.stroke()
            else:
                AK.NSColor.whiteColor().setFill()
                AK.NSBezierPath.bezierPathWithOvalInRect_(AK.NSMakeRect(2, 2, 14, 14)).fill()
                AK.NSColor.colorWithCalibratedRed_green_blue_alpha_(0, .93, .28, 1).setFill()
                AK.NSBezierPath.bezierPathWithOvalInRect_(AK.NSMakeRect(4, 4, 10, 10)).fill()

    panel = AK.NSPanel.alloc().initWithContentRect_styleMask_backing_defer_(
        AK.NSMakeRect(0, 0, 18, 18), AK.NSWindowStyleMaskBorderless | AK.NSWindowStyleMaskNonactivatingPanel,
        AK.NSBackingStoreBuffered, False)
    panel.setTitle_("Pig Guide Green Dot")
    panel.setOpaque_(False)
    panel.setBackgroundColor_(AK.NSColor.clearColor())
    panel.setHasShadow_(False)
    panel.setIgnoresMouseEvents_(True)
    click_through = bool(panel.ignoresMouseEvents())
    panel.setLevel_(AK.NSFloatingWindowLevel)
    panel.setCollectionBehavior_(AK.NSWindowCollectionBehaviorCanJoinAllSpaces | AK.NSWindowCollectionBehaviorFullScreenAuxiliary)
    view = MarkerView.alloc().initWithFrame_(AK.NSMakeRect(0, 0, 18, 18))
    panel.setContentView_(view)
    item = AK.NSStatusBar.systemStatusBar().statusItemWithLength_(AK.NSVariableStatusItemLength)
    item.button().setTitle_("猪 · 识别中")
    menu = AK.NSMenu.alloc().init()
    info = AK.NSMenuItem.alloc().initWithTitle_action_keyEquivalent_("正在识别棋盘", None, "")
    info.setEnabled_(False)
    menu.addItem_(info)
    menu.addItem_(AK.NSMenuItem.separatorItem())

    class MenuActions(NSObject):
        def toggle_(self, sender):
            if paused.is_set():
                paused.clear();solve_hold.clear()
                sender.setTitle_("暂停提示")
            else:
                stop_auto("已暂停")
                paused.set()
                panel.orderOut_(None)
                sender.setTitle_("继续提示")

        def automatic_(self, sender):
            if playback.active or auto_armed.is_set():
                stop_auto("自动点击已停止，已有方案保留")
            else:
                paused.clear();solve_hold.clear()
                auto_armed.set()
                auto_option.setTitle_("停止自动点击")
                start_cached()

        def interval_(self, sender):
            # A settings dialog pauses playback, so no clicks occur behind it.
            stop_auto("正在设置点击间隔")
            alert = AK.NSAlert.alloc().init()
            alert.setMessageText_("自动点击间隔（秒）")
            alert.setInformativeText_("这是最短点击间隔。根据实时位置、速度和碰撞预测，必要时会延后下一次点击。已有方案会保留。")
            field = AK.NSTextField.alloc().initWithFrame_(AK.NSMakeRect(0, 0, 260, 28))
            field.setStringValue_(str(playback.interval))
            alert.setAccessoryView_(field)
            alert.addButtonWithTitle_("保存")
            alert.addButtonWithTitle_("取消")
            if alert.runModal() == AK.NSAlertFirstButtonReturn:
                try:
                    playback.set_interval(field.stringValue())
                    interval_option.setTitle_(f"点击间隔：{playback.interval:g} 秒…")
                except ValueError as e:
                    info.setTitle_(str(e))

        def autoTick_(self, sender):
            if not playback.active:
                if auto_active.is_set():stop_auto(playback.status,cancel_search=False)
                return
            try:
                advanced=playback.tick()
                status=playback.status
                live=playback.telemetry()
                trace.motion(live)
                trace.guide(playback.plan,playback.index,status)
                info.setTitle_(status)
                if advanced is not None or status!=last_auto_status[0]:
                    (state_dir/"auto-status.json").write_text(json.dumps({"active":playback.active,
                        "dispatched":playback.index,"confirmed":playback.confirmed,
                        "total":len(playback.plan['actions']),"interval":playback.interval,
                        "moving":len(playback.moving),"status":status,"time":time.time()},ensure_ascii=False))
                    last_auto_status[0]=status
                if not playback.active:
                    stop_auto("全部移动已确认完成，已有方案保留",cancel_search=False)
                elif advanced is not None:
                    show_playback()
            except Exception as error:
                stop_auto(str(error))

        def refresh_(self, sender):
            stop_auto("重新识别棋盘")
            panel.orderOut_(None)
            solve_hold.clear();paused.clear()
            cache.clear();cache["plan"]=None
            commands.put(("reset",None))

        def calculation_(self, sender):
            if visual is not None:
                visual.show()

        def quit_(self, sender):
            playback.stop()
            jobs.cancel()
            stop.set()
            panel.orderOut_(None)
            app.terminate_(None)

    actions = MenuActions.alloc().init()
    for label, selector in [("暂停提示", "toggle:"), ("重新识别", "refresh:"), ("显示计算过程", "calculation:"), ("退出绿点工具", "quit:")]:
        option = AK.NSMenuItem.alloc().initWithTitle_action_keyEquivalent_(label, selector, "")
        option.setTarget_(actions)
        menu.addItem_(option)
    auto_option = AK.NSMenuItem.alloc().initWithTitle_action_keyEquivalent_("停止自动点击" if args.auto_start else "开始自动点击", "automatic:", "")
    auto_option.setTarget_(actions)
    menu.insertItem_atIndex_(auto_option, 2)
    interval_option = AK.NSMenuItem.alloc().initWithTitle_action_keyEquivalent_(f"点击间隔：{playback.interval:g} 秒…", "interval:", "")
    interval_option.setTarget_(actions)
    menu.insertItem_atIndex_(interval_option, 3)
    item.setMenu_(menu)

    def stop_auto(status,cancel_search=True):
        was_running=playback.active or auto_active.is_set()
        playback.stop();jobs.cancel();epoch[0]+=1
        if cancel_search:solve_hold.set()
        auto_armed.clear();auto_active.clear()
        auto_option.setTitle_("开始自动点击")
        panel.orderOut_(None);info.setTitle_(status)
        trace.motion(None)
        if was_running and playback.plan is not None:
            cache.update(plan=playback.plan,grid=playback.grid,index=playback.confirmed,frame=playback.frame,image=playback.latest_image)
            commands.put(("sync",dict(cache)))
        record={"active":False,"status":status,"dispatched":getattr(playback,"index",0),"confirmed":getattr(playback,"confirmed",0),"time":time.time()}
        (state_dir/"auto-status.json").write_text(json.dumps(record,ensure_ascii=False))
        print(json.dumps({"automatic_playback":record},ensure_ascii=False),flush=True)

    def start_cached():
        if cache.get("plan") and cache.get("index",0)<len(cache["plan"]["actions"]):
            auto_armed.clear()
            try:
                focus_phone(cache['frame'])
            except Exception as e:
                stop_auto(f"镜像未就绪，方案已保留：{e}")
                return
            playback.start(cache['plan'],cache['grid'],cache['index'],cache['frame'])
            auto_active.set()
            print(json.dumps({"automatic_playback":"cached_plan","index":cache['index'],"interval":playback.interval},ensure_ascii=False),flush=True)
            show_playback()

    def escape(event):
        if event.keyCode() == 53:
            stop_auto("Esc 已停止自动点击")
        return event

    key_monitor = AK.NSEvent.addGlobalMonitorForEventsMatchingMask_handler_(AK.NSEventMaskKeyDown, escape)
    local_monitor = AK.NSEvent.addLocalMonitorForEventsMatchingMask_handler_(AK.NSEventMaskKeyDown, escape)
    from Foundation import NSTimer
    auto_timer = NSTimer.scheduledTimerWithTimeInterval_target_selector_userInfo_repeats_(.02, actions, "autoTick:", None, True)

    def show_playback():
        tracker = PlanTracker()
        tracker.plan, tracker.grid, tracker.index = playback.plan, playback.grid, playback.index
        mark = tracker.marker()
        show(mark, playback.frame, playback.status)

    def present(marker,frame,status,plan,grid,index,image,generation):
        if generation!=epoch[0] or auto_active.is_set():return
        if marker and plan:
            cache.update(plan=plan,grid=grid,index=index,frame=frame,image=image)
        show(marker,frame,status)
        if marker and auto_armed.is_set() and not paused.is_set():start_cached()

    def present_error(status,generation):
        if generation!=epoch[0] or auto_active.is_set():return
        show(None,None,status)

    def tracking_error(status,generation):
        if generation==epoch[0] and auto_active.is_set():stop_auto(status)

    def show(marker, frame, status):
        info.setTitle_(status)
        item.button().setToolTip_(status)
        if not marker or paused.is_set():
            panel.orderOut_(None)
            item.button().setTitle_("猪 · 暂停" if paused.is_set() else "猪 · 识别中")
            return
        x, y = screen_point(marker, frame)
        screen_height = Quartz.CGDisplayBounds(Quartz.CGMainDisplayID()).size.height
        width, height = 18, 18
        if args.marker == "box":
            width, height = ((28, 52) if marker["direction"] in "NS" else (52, 28))
            width *= frame["width"]/379
            height *= frame["height"]/835
        panel.setFrame_display_(AK.NSMakeRect(x-width/2, screen_height-y-height/2, width, height), True)
        view.setFrame_(AK.NSMakeRect(0, 0, width, height))
        view.setNeedsDisplay_(True)
        panel.orderFrontRegardless()
        item.button().setTitle_(f"猪 {marker['step']}/{marker['total']}")

    def worker():
        capture,recognizer=PhoneCapture(),Recognizer()
        def controlled_solve(objects,observer=None):
            if solve_hold.is_set():raise SolveCancelled('计算已暂停；开始自动点击或重新识别可继续')
            AppHelper.callAfter(info.setTitle_,'正在计算完整解法，可按 Esc 取消')
            return jobs.solve(objects,observer=observer)
        tracker=PlanTracker(observer=trace.record if visual else None,solve_fn=controlled_solve)
        previous_revision,last_status=-1,None
        capture_failure_since=None
        while not stop.is_set():
            if paused.is_set():
                stop.wait(.1);continue
            while not commands.empty():
                command,payload=commands.get()
                if command=="reset":tracker.reset();previous_revision=-1
                elif command=="sync":
                    tracker.plan,tracker.grid,tracker.index=payload['plan'],payload['grid'],payload['index']
            generation=epoch[0]
            if auto_active.is_set():
                try:
                    image,frame=capture.capture()
                    playback.observe(image,frame)
                    capture_failure_since=None
                except CaptureError as error:
                    playback.hold_for_capture()
                    if capture_failure_since is None:capture_failure_since=time.monotonic()
                    if time.monotonic()-capture_failure_since>3:
                        AppHelper.callAfter(tracking_error,str(error),generation)
                except Exception as error:
                    AppHelper.callAfter(tracking_error,str(error),generation)
                stop.wait(.025)
                continue
            try:
                image,frame=capture.capture()
                try:board=recognizer.read(image,grid=tracker.grid)
                except RecognitionError:board=recognizer.read(image)
                marker=tracker.observe(board)
                if generation!=epoch[0]:continue
                status=tracker.status
                trace.guide(tracker.plan,tracker.index,status)
                if tracker.revision!=previous_revision and tracker.plan:
                    (state_dir/"board.json").write_text(json.dumps(board,ensure_ascii=False,indent=2))
                    (state_dir/"plan.json").write_text(json.dumps(tracker.plan,ensure_ascii=False,indent=2))
                    previous_revision=tracker.revision
                snapshot={"status":status,"marker":marker,"window":frame,"index":tracker.index,
                          "recognition_ms":board['recognition_ms'],"click_through":click_through,"time":time.time()}
                (state_dir/"status.json").write_text(json.dumps(snapshot,ensure_ascii=False))
                if status!=last_status:
                    print(json.dumps(snapshot,ensure_ascii=False),flush=True);last_status=status
                AppHelper.callAfter(present,marker,frame,status,tracker.plan,tracker.grid,tracker.index,image,generation)
            except Exception as error:
                if generation!=epoch[0]:continue
                status=str(error)
                if tracker.plan:status += "；已有方案保留"
                trace.guide(tracker.plan,tracker.index,status)
                if status!=last_status:
                    print(json.dumps({"status":status,"time":time.time()},ensure_ascii=False),flush=True);last_status=status
                AppHelper.callAfter(present_error,status,generation)
                (state_dir/"status.json").write_text(json.dumps({"status":status,"marker":None,"plan_retained":tracker.plan is not None},ensure_ascii=False))
            stop.wait(.25)

    thread = threading.Thread(target=worker, daemon=True)
    thread.start()
    print(json.dumps({"state_dir": str(state_dir), "marker": args.marker, "automatic_clicks": args.auto_start, "interval": args.interval}), flush=True)
    AppHelper.runEventLoop()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--analyze", type=Path, help="离线识别完整游戏截图并生成方案，不控制手机")
    mode.add_argument("--demo", action="store_true", help="分析随附棋盘，不需要连接手机")
    mode.add_argument("--doctor", action="store_true", help="检查系统、依赖、权限和镜像，不截屏或点击")
    parser.add_argument("--out", type=Path, default=Path("analysis"), help="离线分析的输出目录")
    parser.add_argument("--state-dir", type=Path, help="实时运行记录目录，默认用户 Library/Application Support/Pig Game Guide")
    parser.add_argument("--marker", choices=["dot", "box"], default="dot")
    parser.add_argument("--no-visualization", action="store_true", help="只显示绿点，不打开计算过程窗口")
    parser.add_argument("--interval", type=interval_seconds, default=1.0, help="最短点击间隔，0.1–3600 秒，默认 1 秒")
    parser.add_argument("--auto-start", action="store_true", help="得到完整解法后自动执行当前关卡；默认只提示")
    args = parser.parse_args(argv)
    if args.auto_start and (args.analyze or args.demo or args.doctor):
        parser.error("--auto-start 只用于实时模式，不能与离线分析或环境检查同时使用")
    try:
        if args.doctor:
            from doctor import check
            return check()
        require_installation()
        if args.analyze or args.demo:
            analyze(args.analyze or ROOT/"examples"/"demo.png", args.out)
        else:
            run_live(args)
    except (RuntimeError, ValueError, OSError) as error:
        print(f"无法启动或完成分析：{error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
