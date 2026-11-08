"""Compact grid search. The public simulator independently verifies every result.

A state stores the lower tile index of each object; -1 means it has left.
Integer bit masks represent occupancy, forward rays, and duck escape regions.
"""
import heapq
import itertools
import random
import time

class BitBoard:
    def __init__(self, objects):
        self.objects = objects
        self.lock_counts = [p.get('lock_remaining',0) for p in objects]
        self.counted_pigs = [i for i,p in enumerate(objects) if p['kind']=='pig']
        from solver import occupied_cells, half
        cells = [c for p in objects for c in occupied_cells(p)]
        if not cells:
            cells = [(0, 0)]
        self.x0 = min(x for x,y in cells)-1
        self.y0 = min(y for x,y in cells)-1
        self.w = max(x for x,y in cells)-self.x0+2
        self.h = max(y for x,y in cells)-self.y0+2
        w,h = self.w,self.h
        self.full = (1 << (w*h))-1
        self.left = sum(1 << (y*w) for y in range(h))
        self.right = self.left << (w-1)
        self.edge = self.left | self.right | ((1<<w)-1) | (((1<<w)-1) << ((h-1)*w))
        self.masks=[]; self.rays=[]; self.strides=[]; self.lengths=[]; self.positive=[];self.sizes=[]
        self.pigs=[];self.ducks=[];initial=[]
        for i,p in enumerate(objects):
            cells=occupied_cells(p); x,y=cells[0];x-=self.x0;y-=self.y0
            initial.append(x+y*w)
            sx,sy=half(p);self.sizes.append((sx,sy))
            vertical=p.get('d') in ('N','S');stride=w if vertical else 1
            length=sy if vertical else sx
            positive=p.get('d') in ('E','S')
            mask={};ray={}
            if p['kind']=='duck':
                self.ducks.append(i);mask[x+y*w]=1<<(x+y*w)
            else:
                self.pigs.append(i)
                for a in range((h if vertical else w)-length+1):
                    pos=x+a*w if vertical else a+y*w
                    mask[pos]=sum(1<<(pos+dx+dy*w) for dy in range(sy) for dx in range(sx))
                    rr=range(a+length,h if vertical else w) if positive else range(a)
                    ray[pos]=sum(1<<(x+cross+k*w if vertical else k+(y+cross)*w)
                                 for k in rr for cross in range(sx if vertical else sy))
            self.masks.append(mask);self.rays.append(ray);self.strides.append(stride)
            self.lengths.append(length);self.positive.append(positive)
        self.initial=tuple(initial)

    def occupancy(self,state):
        return sum(self.masks[i][a] for i,a in enumerate(state) if a>=0)

    def settle(self,state,occ):
        ducks=[i for i in self.ducks if state[i]>=0]
        if not ducks:return state,occ,[]
        duckbits=sum(1<<state[i] for i in ducks)
        free=self.full ^ (occ ^ duckbits)
        reached=self.edge & free
        while True:
            grow=(reached | ((reached & ~self.right)<<1) | ((reached & ~self.left)>>1) | (reached<<self.w) | (reached>>self.w)) & free
            if grow==reached:break
            reached=grow
        gone=[i for i in ducks if (1<<state[i]) & reached]
        if gone:
            state=list(state)
            for i in gone:occ ^= 1<<state[i];state[i]=-1
            state=tuple(state)
        return state,occ,gone

    def lock_remaining(self,state,i):
        count=self.lock_counts[i]
        return max(0,count-sum(state[j]<0 for j in self.counted_pigs)) if count else 0

    def move(self,state,occ,i,ignore_locks=False):
        if not ignore_locks and self.lock_remaining(state,i):return state[i]
        a=state[i];hit=occ & self.rays[i][a]
        if not hit:return -1
        stride=self.strides[i]
        if self.positive[i]:
            if stride==1 and self.sizes[i][1]>1:
                # Row-major bit order is not distance order across parallel lanes.
                column=min((hit >> (row*self.w) & ((1<<self.w)-1) & -(hit >> (row*self.w) & ((1<<self.w)-1))).bit_length()-1
                           for row in range(a//self.w,a//self.w+self.sizes[i][1]) if hit >> (row*self.w) & ((1<<self.w)-1))
                return a//self.w*self.w+column-self.lengths[i]
            stop=(hit & -hit).bit_length()-1
            if stride==self.w:stop=stop//self.w*self.w+a%self.w
            return stop-self.lengths[i]*stride
        if stride==1 and self.sizes[i][1]>1:
            column=max((hit >> (row*self.w) & ((1<<self.w)-1)).bit_length()-1
                       for row in range(a//self.w,a//self.w+self.sizes[i][1]))
            return a//self.w*self.w+column+1
        stop=hit.bit_length()-1
        if stride==self.w:stop=stop//self.w*self.w+a%self.w
        return stop+stride

    def apply(self,state,occ,i,to):
        if to!=state[i] and self.lock_remaining(state,i):raise ValueError('Cannot move a locked pig')
        occ ^= self.masks[i][state[i]]
        if to>=0:occ |= self.masks[i][to]
        after=list(state);after[i]=to
        return self.settle(tuple(after),occ)

    def clean(self,state,occ):
        actions=[]
        while True:
            free=next((i for i in self.pigs if state[i]>=0 and self.move(state,occ,i)<0),None)
            if free is None:return state,occ,actions
            state,occ,ducks=self.apply(state,occ,free,-1)
            actions.append((free,-1,ducks))

    def deadlocked(self,state,occ):
        """Find occupied cells that no legal sequence can ever clear.

        Optimistically let every pig travel as far as the remaining cores allow.
        Only the intersection of its original and furthest possible body is
        guaranteed to remain occupied. Repeat while these cores shrink, also
        letting ducks escape through every optimistically cleared cell. Any
        nonempty fixed point is an invariant obstruction, so this branch cannot
        clear the board. An empty fixed point is NOT a proof of solvability.
        """
        cores=[self.masks[i][a] if a>=0 else 0 for i,a in enumerate(state)]
        while True:
            changed=False
            for i in self.pigs:
                if not cores[i]:continue
                # Locks can open after other pigs exit. Ignoring them is an
                # optimistic relaxation; fixed locks would falsely prove death.
                to=self.move(state,occ,i,ignore_locks=True)
                keep=0 if to<0 else cores[i] & self.masks[i][to]
                if keep!=cores[i]:occ ^= cores[i] ^ keep;cores[i]=keep;changed=True
            duckbits=sum(cores[i] for i in self.ducks)
            if duckbits:
                free=self.full ^ (occ ^ duckbits)
                reached=self.edge & free
                while True:
                    grow=(reached | ((reached & ~self.right)<<1) | ((reached & ~self.left)>>1) | (reached<<self.w) | (reached>>self.w)) & free
                    if grow==reached:break
                    reached=grow
                for i in self.ducks:
                    if cores[i] & reached:occ ^= cores[i];cores[i]=0;changed=True
            if not changed:return bool(occ)

    def decode(self,state):
        result=[]
        for i,pos in enumerate(state):
            if pos<0:continue
            x,y=pos%self.w+self.x0,pos//self.w+self.y0
            p=self.objects[i];sx,sy=self.sizes[i]
            q={**p,'u':2*x+sx-1,'v':2*y+sy-1}
            if 'lock_remaining' in p:q['lock_remaining']=self.lock_remaining(state,i)
            result.append(q)
        return result

    def describe(self, state, step):
        i, to, ducks = step
        before = self.decode(state)
        p = next(p for p in before if p['id'] == self.objects[i]['id'])
        from solver import predict
        return {**predict(before, p), 'duck_exits': sorted(self.objects[j]['id'] for j in ducks)}


def search(board, max_seconds, max_nodes, emit=None):
    """Fast exit-first attempts, followed by a search retaining exit alternatives.

    Clearing every free exit is an ordering heuristic, not an impossibility
    proof: a pig may be useful as a temporary stopping point. The final search
    therefore considers both exits and slides without that contraction.
    """
    start = time.monotonic()
    initial, bits, initial_ducks = board.settle(board.initial, board.occupancy(board.initial))
    expanded, pruned, best = 0, 0, sum(a >= 0 for a in initial)

    def event(phase, state, step=None, depth=0, **extra):
        if emit is not None:
            emit({'phase': phase, 'state': board.decode(state),
                  'action': board.describe(state, step) if step else None,
                  'depth': depth, 'expanded': expanded, 'deadlock_prunes': pruned,
                  'best_remaining': best, 'elapsed_ms': (time.monotonic()-start)*1000, **extra})

    def check(state):
        elapsed = time.monotonic()-start
        if expanded >= max_nodes or elapsed >= max_seconds:
            event('limit', state)
            bound = f'{max_seconds:g} 秒' if elapsed >= max_seconds else f'{max_nodes} 个状态'
            raise TimeoutError(f'搜索达到 {bound} 上限（已检查 {expanded} 个状态）；尚无完整方案，点击已暂停')

    def visit(state, occ, depth):
        nonlocal expanded, pruned, best
        check(state)
        expanded += 1
        event('visit', state, depth=depth)
        if board.deadlocked(state, occ):
            pruned += 1
            event('prune', state, depth=depth, reason='invariant_deadlock')
            return False
        best = min(best, sum(a >= 0 for a in state))
        return True

    event('begin', initial)
    root, root_bits, root_moves = board.clean(initial, bits)

    class Restart(Exception):
        pass

    for seed in range(32):
        rng, seen, base = random.Random(seed), set(), expanded

        def dfs(state, occ, actions, depth=0):
            if expanded-base >= 150 or depth >= 400:
                raise Restart()
            if state in seen:
                return None
            seen.add(state)
            if not visit(state, occ, len(actions)):
                return None
            if not occ:
                return actions
            choices = []
            for i in board.pigs:
                if state[i] < 0:
                    continue
                to = board.move(state, occ, i)
                if to == state[i]:
                    continue
                check(state)
                after, after_bits, ducks = board.apply(state, occ, i, to)
                after, after_bits, exits = board.clean(after, after_bits)
                choices.append((sum(a >= 0 for a in after), rng.random(), after, after_bits, [(i, to, ducks)]+exits))
            choices.sort(key=lambda v: v[:2])
            for _, _, after, after_bits, moves in choices:
                event('branch', state, moves[0], len(actions), choices=len(choices))
                answer = dfs(after, after_bits, actions+moves, depth+1)
                if answer is not None:
                    return answer
                event('backtrack', state, moves[0], len(actions))
            return None

        try:
            answer = dfs(root, root_bits, root_moves)
            if answer is not None:
                return answer, expanded, pruned
            # A complete exit-first traversal is independent of its tie order.
            break
        except Restart:
            continue

    # No greedy contraction here: retain every stopping point and deduplicate
    # commuting move orders. Parent links avoid copying entire action histories.
    serial = itertools.count()
    frontier = [(sum(a >= 0 for a in initial), 0, next(serial), initial, bits)]
    parents = {initial: None}
    while frontier:
        _, depth, _, state, occ = heapq.heappop(frontier)
        if not visit(state, occ, depth):
            continue
        if not occ:
            answer = []
            while parents[state] is not None:
                state, step = parents[state]
                answer.append(step)
            return list(reversed(answer)), expanded, pruned
        for i in board.pigs:
            if state[i] < 0:
                continue
            to = board.move(state, occ, i)
            if to == state[i]:
                continue
            check(state)
            after, after_bits, ducks = board.apply(state, occ, i, to)
            if after in parents:
                continue
            parents[after] = (state, (i, to, ducks))
            event('branch', state, (i, to, ducks), depth)
            heapq.heappush(frontier, (sum(a >= 0 for a in after), depth+1, next(serial), after, after_bits))
    raise ValueError('当前识别棋盘在此规则模型下无解；请检查方向、位置或特殊规则')
