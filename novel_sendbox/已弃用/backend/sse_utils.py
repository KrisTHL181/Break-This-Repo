"""共享工具：SSE 事件格式化 + JSON 容错解析。供 deduction / dialogue_agent / agents 等模块使用。"""
import json


def emit(event_type, data=None):
    """SSE 单事件：data: {json}\n\n"""
    payload = {"type": event_type, **(data or {})}
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


def parse_json(raw):
    """去掉 markdown 代码块标记后 json.loads。失败抛异常。"""
    s = raw.strip()
    if s.startswith("```"):
        lines = s.split("\n")
        if lines[-1].strip() == "```":
            s = "\n".join(lines[1:-1])
        else:
            s = "\n".join(lines[1:])
    return json.loads(s)


def parse_json_safe(s):
    """容错 JSON 解析：失败返回 {}。"""
    if not s:
        return {}
    try:
        return json.loads(s)
    except (json.JSONDecodeError, TypeError):
        return {}