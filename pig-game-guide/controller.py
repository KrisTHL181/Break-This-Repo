"""Guide advances on verified visual state changes, never on a timer."""
from recognition import fingerprint
from solver import solve


class PlanTracker:
    def __init__(self, observer=None, solve_fn=solve):
        self.observer = observer
        self.solve_fn = solve_fn
        self.failed_board = None
        self.failed_error = None
        self.plan = None
        self.grid = None
        self.index = 0
        self.candidate = None
        self.stable_reads = 0
        self.revision = 0
        self.status = "正在识别棋盘"

    def reset(self):
        self.__init__(observer=self.observer, solve_fn=self.solve_fn)

    def observe(self, board):
        objects = board["objects"]
        observed = fingerprint(objects)
        if observed == self.candidate:
            self.stable_reads += 1
        else:
            self.candidate, self.stable_reads = observed, 1
        if not objects:
            self.status = "棋盘已空，等待下一关"
            if self.plan and self.index == len(self.plan["actions"])-1:
                self.index += 1
            return None
        if self.plan is not None and self.index < len(self.plan["actions"]):
            before = fingerprint(self.plan["states"][self.index])
            after = fingerprint(self.plan["states"][self.index+1])
            if observed == before:
                return self.marker()
            if observed == after:
                self.index += 1
                return self.marker()
        if self.plan is not None:
            expected = self.plan['states'][min(self.index,len(self.plan['states'])-1)]
            # A highlighted pig may wobble along its lane. Match that one back
            # to the cached position while requiring every other object to agree.
            adjusted=[]
            for p in objects:
                q=dict(p)
                if p.get('hint_animation'):
                    nearby=[e for e in expected if e['kind']==p['kind'] and e.get('d')==p.get('d')
                            and e.get('length',2)==p.get('length',2)
                            and abs(e['u']-p['u'])+abs(e['v']-p['v'])<=2]
                    if len(nearby)==1:q.update(u=nearby[0]['u'],v=nearby[0]['v'])
                adjusted.append(q)
            if fingerprint(adjusted)==fingerprint(expected):return self.marker()
            # Manual clicks or resuming mid-animation may skip several cached
            # states. Align progress without searching the board again.
            for i in range(self.index+1, len(self.plan["states"])):
                if observed == fingerprint(self.plan["states"][i]):
                    self.index = i
                    return self.marker()
            # Transient hints and partially hidden pigs retain the last plan.
            # Only an independently repeated DIFFERENT layout triggers replan.
            current_set, expected_set = set(observed), set(fingerprint(expected))
            overlap=len(current_set & expected_set)/max(1,min(len(current_set),len(expected_set)))
            if self.stable_reads < 2 or overlap >= .6:
                self.status = "画面有变化，已有方案保留；实际走法改变时可点重新识别"
                return None
        if observed == self.failed_board:
            self.status = self.failed_error+"；已有结果保留，可点重新识别重试"
            return None
        self.status = "正在计算完整解法（可按 Esc 取消）"
        try:
            plan = self.solve_fn(objects, observer=self.observer)
        except Exception as error:
            from solve_service import SolveCancelled
            if not isinstance(error, SolveCancelled):
                self.failed_board, self.failed_error = observed, str(error)
            raise
        self.plan = plan
        if board.get('lattice'):
            self.plan['lattice']=board['lattice']
        self.failed_board = None
        self.grid = board["grid"]
        self.index = 0
        self.revision += 1
        return self.marker()

    def marker(self):
        if not self.plan or self.index >= len(self.plan["actions"]):
            self.status = "已完成全部提示，等待下一关"
            return None
        action = self.plan["actions"][self.index]
        p = next(p for p in self.plan["states"][self.index] if p["id"] == action["id"])
        x = self.grid["x"]["origin"]+action["from"][0]*self.grid["x"]["scale"]
        y = self.grid["y"]["origin"]+action["from"][1]*self.grid["y"]["scale"]
        self.status = f"第 {self.index+1}/{len(self.plan['actions'])} 步：点击绿点"
        return {"x": x, "y": y, "step": self.index+1, "total": len(self.plan["actions"]),
                "id": action["id"], "direction": p["d"], "type": action["type"]}


def screen_point(marker, frame):
    return frame["x"]+marker["x"]*frame["width"]/379, frame["y"]+marker["y"]*frame["height"]/835
