"""Own a cancellable child process so searches cannot freeze the native UI."""
import json
from pathlib import Path
import subprocess
import sys
import threading
import time

class SolveCancelled(RuntimeError):pass

class SolveService:
    def __init__(self):
        self.lock=threading.Lock();self.process=None;self.serial=0

    def cancel(self):
        with self.lock:
            self.serial+=1
            process=self.process
            if process is not None and process.poll() is None:process.terminate()

    def solve(self, objects, observer=None, max_seconds=20):
        with self.lock:
            generation=self.serial
            process=subprocess.Popen([sys.executable,'-u',str(Path(__file__).with_name('solve_worker.py'))],
                stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
            self.process=process
        timer=threading.Timer(max_seconds+5,lambda:process.kill() if process.poll() is None else None)
        timer.start()
        answer=None;error=None
        try:
            process.stdin.write(json.dumps({'objects':objects,'max_seconds':max_seconds})+'\n');process.stdin.close()
            for line in process.stdout:
                with self.lock:
                    if generation!=self.serial:raise SolveCancelled('计算已取消，已有方案保留')
                message=json.loads(line)
                if 'event' in message and observer:observer(message['event'])
                if 'result' in message:
                    answer=message['result']
                    # Restore retained trace in one batch when the recorder supports it.
                    owner=getattr(observer,'__self__',None)
                    if owner is not None and hasattr(owner,'replace_events'):owner.replace_events(message['events'])
                if 'error' in message:error=message['error']
            process.wait()
            with self.lock:
                if generation!=self.serial:raise SolveCancelled('计算已取消，已有方案保留')
            if error:raise RuntimeError(error)
            if answer is None:raise RuntimeError('求解进程已停止或超时；可重新计算')
            return answer
        except (BrokenPipeError, OSError, json.JSONDecodeError) as error:
            with self.lock:
                if generation!=self.serial:raise SolveCancelled('计算已取消，已有方案保留') from None
            raise RuntimeError('求解进程通讯已结束；可重新计算') from error
        finally:
            timer.cancel()
            if process.poll() is None:process.kill()
            process.wait()
            for pipe in (process.stdin,process.stdout,process.stderr):
                try:pipe.close()
                except OSError:pass
            with self.lock:
                if self.process is process:self.process=None
