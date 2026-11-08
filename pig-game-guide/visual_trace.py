"""Bounded, non-blocking solver trace. No display sleeps or solver pacing."""
from collections import deque
import threading


class TraceRecorder:
    def __init__(self, limit=4000):
        self.lock = threading.Lock()
        self.events = deque(maxlen=limit)
        self.version = 0
        self.total = 0
        self.initial = []
        self.latest = None
        self.plan = None
        self.index = 0
        self.status = "等待识别棋盘"
        self.backtracks = 0
        self.live = None

    def record(self, event):
        # States are immutable snapshots in the solver; retain references.
        # This callback never calls AppKit, performs IO, or waits for a frame.
        with self.lock:
            if event["phase"] == "begin":
                self.events.clear()
                self.total = 0
                self.initial = event["state"]
                self.plan = None
                self.backtracks = 0
            self.total += 1
            self.backtracks += event["phase"] == "backtrack"
            self.latest = {**event, "sequence": self.total, "backtracks": self.backtracks}
            self.events.append(self.latest)
            self.version += 1

    def replace_events(self, events):
        with self.lock:
            self.events.clear()
            self.events.extend(events)
            self.total = len(events)
            self.backtracks = sum(e["phase"] == "backtrack" for e in events)
            backtracks=0
            normalized=[]
            for i,e in enumerate(events):
                backtracks+=e["phase"]=="backtrack"
                normalized.append({**e,"sequence":i+1,"backtracks":backtracks})
            self.events=deque(normalized,maxlen=self.events.maxlen)
            self.latest = self.events[-1] if self.events else None
            self.version += 1

    def motion(self, live):
        with self.lock:
            self.live = live
            self.version += 1

    def guide(self, plan, index, status):
        with self.lock:
            if self.plan is not plan or self.index != index or self.status != status:
                self.plan, self.index, self.status = plan, index, status
                self.version += 1

    def snapshot(self):
        with self.lock:
            return {"version": self.version, "events": tuple(self.events), "total": self.total,
                    "initial": self.initial, "latest": self.latest, "plan": self.plan, "live":self.live,
                    "index": self.index, "status": self.status, "backtracks": self.backtracks}
