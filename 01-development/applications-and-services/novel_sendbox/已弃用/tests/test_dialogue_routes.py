"""Task R4 自检：对话员 4 个路由。
不调 LLM，不启动 Flask 服务器，仅用 test_client 验证参数校验 + 持久化。
运行：python tests/test_dialogue_routes.py
"""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backend"))

import storage
import app as app_module

failures = []


def check(name, cond, ctx=None):
    if cond:
        print(f"  [OK] {name}")
    else:
        print(f"  [FAIL] {name}  ctx={ctx}")
        failures.append(name)


app = app_module.app
app.config["TESTING"] = True

pid = storage.create_project("r4_test", "测试")["id"]
print(f"测试项目 pid: {pid}")

try:
    with app.test_client() as c:
        # 1. /api/dialogue/chat 参数校验
        print("Test 1: /api/dialogue/chat 参数校验")
        r = c.post("/api/dialogue/chat", json={})
        check("无 pid → 400", r.status_code == 400, r.status_code)

        r = c.post("/api/dialogue/chat", json={"pid": pid})
        check("无 message → 400", r.status_code == 400, r.status_code)

        r = c.post("/api/dialogue/chat", json={"pid": "nonexistent", "message": "x"})
        check("不存在 pid → 404", r.status_code == 404, r.status_code)

        # 2. /api/dialogue/chat 未配置 LLM → SSE 含 error 事件
        print("Test 2: /api/dialogue/chat 未配置 LLM → error")
        r = c.post("/api/dialogue/chat", json={"pid": pid, "message": "你好"})
        check("状态码 200", r.status_code == 200, r.status_code)
        check("SSE 含 error 事件",
              b'"type": "error"' in r.data or b'"type":"error"' in r.data, r.data[:200])
        check("error 提示含 dialogue",
              b"dialogue" in r.data, r.data[:200])

        # 3. /api/dialogue/history 初始为空
        print("Test 3: /api/dialogue/history 初始为空")
        r = c.get(f"/api/dialogue/history?pid={pid}")
        check("状态码 200", r.status_code == 200, r.status_code)
        check("history == []",
              r.get_json().get("history") == [], r.get_json())

        # 4. /api/dialogue/context 注入系统消息
        print("Test 4: /api/dialogue/context 注入")
        r = c.post("/api/dialogue/context", json={"pid": pid, "message": "请帮作者修改第3章"})
        check("状态码 200", r.status_code == 200, r.status_code)
        check("injected=True",
              r.get_json().get("injected") is True, r.get_json())

        # 5. /api/dialogue/history 能读到注入的 system 消息
        print("Test 5: 注入后 history 可读")
        r = c.get(f"/api/dialogue/history?pid={pid}")
        h = r.get_json().get("history") or []
        check("history 含 1 条 system 消息",
              len(h) == 1 and h[0].get("role") == "system", h)
        check("消息内容正确",
              len(h) == 1 and "修改第3章" in h[0].get("content", ""), h)

        # 6. /api/dialogue/clear 清空历史
        print("Test 6: /api/dialogue/clear 清空")
        r = c.post("/api/dialogue/clear", json={"pid": pid})
        check("状态码 200", r.status_code == 200, r.status_code)
        check("cleared=True",
              r.get_json().get("cleared") is True, r.get_json())

        r = c.get(f"/api/dialogue/history?pid={pid}")
        check("清空后 history == []",
              r.get_json().get("history") == [], r.get_json())

        # 7. 各路由参数缺失 → 400
        print("Test 7: 参数缺失")
        r = c.get("/api/dialogue/history")
        check("history 无 pid → 400", r.status_code == 400, r.status_code)

        r = c.post("/api/dialogue/clear", json={})
        check("clear 无 pid → 400", r.status_code == 400, r.status_code)

        r = c.post("/api/dialogue/context", json={"pid": pid})
        check("context 无 message → 400", r.status_code == 400, r.status_code)

        # 8. 不存在的 pid → 404
        print("Test 8: 不存在 pid → 404")
        r = c.get("/api/dialogue/history?pid=nonexistent")
        check("history 404", r.status_code == 404, r.status_code)
        r = c.post("/api/dialogue/clear", json={"pid": "nonexistent"})
        check("clear 404", r.status_code == 404, r.status_code)
        r = c.post("/api/dialogue/context", json={"pid": "nonexistent", "message": "x"})
        check("context 404", r.status_code == 404, r.status_code)

finally:
    storage.delete_project(pid)

print()
if failures:
    print(f"FAILED: {len(failures)} 项")
    for f in failures:
        print(f"  - {f}")
    sys.exit(1)
print("ALL CHECKS PASSED")
