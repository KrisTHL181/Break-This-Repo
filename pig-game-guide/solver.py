"""Complete forward/stop search with automatic four-direction duck escape."""
from collections import deque
import time

DIRECTIONS = {"N": (1, -1), "S": (1, 1), "E": (0, 1), "W": (0, -1)}


def occupied_cells(p):
    u, v = p["u"], p["v"]
    width, height = half(p)
    x0, y0 = (u-width+1)//2, (v-height+1)//2
    return tuple((x0+x,y0+y) for y in range(height) for x in range(width))


def validate(state):
    seen, ids = set(), set()
    for p in state:
        if p["id"] in ids or p["kind"] not in ("pig", "duck", "elephant"):
            raise ValueError("Invalid object")
        ids.add(p["id"])
        remaining = p.get('lock_remaining', 0)
        if type(remaining) is not int or remaining < 0 or (remaining and p['kind'] != 'pig'):
            raise ValueError('Invalid lock count')
        length = 1 if p["kind"] == "duck" else p.get("length", 2)
        width = p.get('width', 1)
        if not isinstance(length, int) or length < 1 or not isinstance(width,int) or width < 1:
            raise ValueError("Invalid object length")
        if p['kind']!='duck' and p.get('d') not in DIRECTIONS:
            raise ValueError('Invalid direction')
        hx,hy=half(p)
        parity = ((hx-1)%2,(hy-1)%2)
        if (p["u"] % 2, p["v"] % 2) != parity:
            raise ValueError("Invalid tile alignment")
        for cell in occupied_cells(p):
            if cell in seen:
                raise ValueError("Overlapping objects")
            seen.add(cell)


def settle_ducks(state):
    current, escaped = list(state), []
    while any(p["kind"] == "duck" for p in current):
        blocked = set(c for p in current for c in occupied_cells(p))
        minx, maxx = min(x for x, y in blocked)-1, max(x for x, y in blocked)+1
        miny, maxy = min(y for x, y in blocked)-1, max(y for x, y in blocked)+1
        exterior, queue = {(minx, miny)}, deque([(minx, miny)])
        while queue:
            x, y = queue.popleft()
            for nx, ny in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
                cell = (nx, ny)
                if minx <= nx <= maxx and miny <= ny <= maxy and cell not in blocked and cell not in exterior:
                    exterior.add(cell)
                    queue.append(cell)
        free = set()
        for p in current:
            if p["kind"] != "duck":
                continue
            x, y = occupied_cells(p)[0]
            if any(c in exterior for c in ((x-1, y), (x+1, y), (x, y-1), (x, y+1))):
                free.add(p["id"])
        if not free:
            break
        escaped.extend(sorted(free))
        current = [p for p in current if p["id"] not in free]
    return current, escaped


def half(p):
    length = p.get("length", 2)
    width = p.get('width', 1)
    return (1, 1) if p["kind"] == "duck" else (width, length) if p["d"] in ("N", "S") else (length, width)


def predict(state, p):
    if p.get('lock_remaining', 0) > 0:
        return {'id':p['id'], 'type':'blocked', 'from':[p['u'],p['v']],
                'to':[p['u'],p['v']], 'blocker':None, 'reason':'locked'}
    axis, sign = DIRECTIONS[p["d"]]
    cross, position, size = 1-axis, (p["u"], p["v"]), half(p)
    distance, blocker = float("inf"), None
    for q in state:
        if q["id"] == p["id"]:
            continue
        other, extent = (q["u"], q["v"]), half(q)
        if abs(other[cross]-position[cross]) >= size[cross]+extent[cross] or (other[axis]-position[axis])*sign <= 0:
            continue
        gap = (other[axis]-position[axis])*sign-size[axis]-extent[axis]
        if gap < 0:
            raise ValueError("Overlapping collision geometry")
        if gap < distance:
            distance, blocker = gap, q["id"]
    kind = "blocked" if distance == 0 else "exit" if distance == float("inf") else "slide"
    target = None if kind == "exit" else list(position)
    if kind == "slide":
        target[axis] += sign*distance
    return {"id": p["id"], "type": kind, "from": list(position), "to": target, "blocker": blocker}


def apply_action(state, action):
    if action["type"] == "blocked":
        return list(state), []
    target = next(p for p in state if p['id'] == action['id'])
    if target.get('lock_remaining', 0):
        raise ValueError('Cannot move a locked pig')
    pig_exit = action['type'] == 'exit' and target['kind'] == 'pig'
    after = []
    for p in state:
        q = dict(p)
        if pig_exit and 'lock_remaining' in q:
            q['lock_remaining'] = max(0, q['lock_remaining']-1)
        if p["id"] == action["id"]:
            if action["type"] == "exit":
                continue
            q["u"], q["v"] = action["to"]
        after.append(q)
    return settle_ducks(after)


def solve(objects, max_seconds=20, max_nodes=100000, observer=None):
    from search_engine import BitBoard, search
    validate(objects)
    start = time.monotonic()
    board = BitBoard(objects)
    compact, expanded, pruned = search(board, max_seconds, max_nodes, observer)
    replay, initial_ducks = settle_ducks(objects)
    states, actions = [list(replay)], []
    for i, to, duck_indices in compact:
        current = next(p for p in replay if p['id'] == objects[i]['id'])
        action = predict(replay, current)
        if action['type'] == 'blocked' or (action['type'] == 'exit') != (to < 0):
            raise ValueError('Compact search/replay mismatch')
        if to >= 0:
            target = board.decode(tuple(to if j == i else -1 for j in range(len(objects))))[0]
            if action['to'] != [target['u'], target['v']]:
                raise ValueError('Compact target/replay mismatch')
        after, ducks = apply_action(replay, action)
        if sorted(ducks) != sorted(objects[j]['id'] for j in duck_indices):
            raise ValueError('Duck replay mismatch')
        validate(after)
        action = {**action, 'duck_exits': ducks}
        if observer is not None:
            observer({'phase': 'replay', 'state': replay, 'action': action, 'depth': len(actions),
                      'expanded': expanded, 'deadlock_prunes': pruned,
                      'elapsed_ms': (time.monotonic()-start)*1000})
        actions.append(action)
        replay = after
        states.append(replay)
    if replay:
        raise ValueError('Incomplete solution')
    if observer is not None:
        observer({'phase': 'done', 'state': [], 'action': None, 'depth': len(actions),
                  'steps': len(actions), 'expanded': expanded, 'deadlock_prunes': pruned,
                  'elapsed_ms': (time.monotonic()-start)*1000})
    return {'status': 'complete_model', 'actions': actions, 'states': states,
            'initial_duck_exits': initial_ducks, 'expanded': expanded, 'deadlock_prunes': pruned,
            'solve_ms': (time.monotonic()-start)*1000, 'replay_remaining': 0}
