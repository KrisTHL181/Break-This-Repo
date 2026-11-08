"""Isolated solver process. JSON input, throttled progress and one final result."""
import json
import sys
import time
from solver import solve
from visual_trace import TraceRecorder

if __name__=='__main__':
    request=json.loads(sys.stdin.readline())
    trace=TraceRecorder();last=0
    def progress(event):
        global last
        trace.record(event)
        now=time.monotonic()
        if now-last>.1 or event['phase'] in ('begin','done','limit'):
            print(json.dumps({'event':event},ensure_ascii=False),flush=True);last=now
    try:
        plan=solve(request['objects'],max_seconds=request.get('max_seconds',20),observer=progress)
        print(json.dumps({'result':plan,'events':trace.snapshot()['events']},ensure_ascii=False),flush=True)
    except Exception as e:
        print(json.dumps({'error':str(e)},ensure_ascii=False),flush=True)
