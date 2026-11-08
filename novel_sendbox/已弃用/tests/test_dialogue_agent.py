"""Task R2 自检：对话员模块 + 工具集。
不实际调用 LLM/不读写真实项目，全部 mock。
运行：python tests/test_dialogue_agent.py
"""
import json
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backend"))

import dialogue_agent as da

failures = []


def check(name, cond, ctx=None):
    if cond:
        print(f"  [OK] {name}")
    else:
        print(f"  [FAIL] {name}  ctx={ctx}")
        failures.append(name)


# ---------- 简易 mock 容器 ----------
class MockStore:
    """替换 dialogue_agent.storage。所有方法记录调用并返回预设值。"""

    def __init__(self):
        self.data = {}        # filename → content
        self.llm_config = {}
        self.project = None

    def get_project(self, pid):
        return self.project

    def update_project(self, pid, meta):
        self.project = meta
        return meta

    def load_data(self, pid, filename):
        return self.data.get(filename)

    def save_data(self, pid, filename, content):
        self.data[filename] = content

    def load_llm_config(self):
        return self.llm_config

    # R22: 自定义提示词 — 测试不关心内容，返回空字符串走默认逻辑
    def load_prompt(self, pid, key):
        return ""

    def save_snapshot(self, pid, version):
        return version

    # 对话历史封装
    def load_dialogue_history(self, pid):
        h = self.data.get("dialogue_history.json")
        return h if isinstance(h, list) else []

    def save_dialogue_history(self, pid, history):
        self.data["dialogue_history.json"] = history or []

    def clear_dialogue_history(self, pid):
        self.data["dialogue_history.json"] = []


class MockDeduction:
    def __init__(self):
        self.streaming = set()
        self.stop_called = False
        self.active_state = None
        self.active_scenes = []
        self.paused_scenes = []

    def is_streaming(self, pid):
        return pid in self.streaming

    def request_stop(self, pid):
        self.stop_called = True

    def start_chapter(self, pid):
        return 1

    def get_active_state(self, pid):
        return self.active_state

    def list_active_scenes(self, pid):
        return self.active_scenes

    def list_paused_scenes(self, pid):
        return self.paused_scenes

    def pause_scene(self, pid, name):
        return {"scene_name": name}

    def resume_scene(self, pid, name):
        return {"scene_name": name}

    def set_multithread(self, pid, enabled):
        return {"multithread": enabled}


class MockExporter:
    def __init__(self):
        # 用 OS-native 分隔符模拟真实路径
        sep = os.path.sep
        self.export_path = f"C:{sep}tmp{sep}novel.txt"

    def export_txt(self, pid):
        return self.export_path

    def export_markdown(self, pid):
        return f"C:{os.path.sep}tmp{os.path.sep}novel.md"

    def export_epub(self, pid):
        return f"C:{os.path.sep}tmp{os.path.sep}novel.epub"

    def export_project_zip(self, pid):
        return f"C:{os.path.sep}tmp{os.path.sep}novel.zip"

    def generate_finale_summary(self, pid):
        return {"summary": "完结"}


# ---------- 安装 mock ----------
mock_store = MockStore()
mock_ded = MockDeduction()
mock_exp = MockExporter()
orig_storage = da.storage
orig_deduction = da.deduction
orig_exporter = da.exporter
da.storage = mock_store
da.deduction = mock_ded
da.exporter = mock_exp


def parse_sse_events(gen):
    """从 SSE 生成器中提取所有事件 dict。"""
    events = []
    for chunk in gen:
        # chunk 形如 "data: {...}\n\n"
        for line in chunk.split("\n"):
            line = line.strip()
            if line.startswith("data: "):
                events.append(json.loads(line[6:]))
    return events


# ---------- 1. TOOLS schema 完整性 ----------
print("Test 1: TOOLS schema")
check("TOOLS 是 list",
      isinstance(da.TOOLS, list))
check(f"工具数 == 36（实际 {len(da.TOOLS)}）",
      len(da.TOOLS) == 36)
names = [t["function"]["name"] for t in da.TOOLS]
check("无重名工具",
      len(names) == len(set(names)))
check("每个工具有 name/description/parameters",
      all(t.get("type") == "function"
          and t["function"].get("name")
          and t["function"].get("description")
          and "parameters" in t["function"]
          for t in da.TOOLS))


# ---------- 2. _TOOL_HANDLERS 覆盖所有 TOOLS 工具名 ----------
print("Test 2: _TOOL_HANDLERS 覆盖")
handler_names = set(da._TOOL_HANDLERS.keys())
tool_names = set(names)
check("所有 TOOLS 都有 handler",
      tool_names.issubset(handler_names), )
check("无多余 handler",
      handler_names == tool_names)
check("每个 handler 是 callable",
      all(callable(h) for h in da._TOOL_HANDLERS.values()))


# ---------- 3. dispatch_tool 未知工具 ----------
print("Test 3: dispatch_tool 未知工具")
r = da.dispatch_tool("pid_x", "nonexistent_tool", {})
check("未知工具返回 error",
      "error" in r and "未知工具" in r["error"], r)


# ---------- 4. dispatch_tool 路由：get_project_info ----------
print("Test 4: dispatch_tool get_project_info")
mock_store.project = {"id": "pid_x", "title": "测试小说", "current_chapter": 3,
                      "genre": "修仙", "current_scene": "山门"}
r = da.dispatch_tool("pid_x", "get_project_info", {})
check("返回 summary",
      "summary" in r and "测试小说" in r["summary"], r)
check("返回 data == project meta",
      r.get("data", {}).get("title") == "测试小说", r)


# ---------- 5. dispatch_tool 路由：export_novel ----------
print("Test 5: dispatch_tool export_novel")
r = da.dispatch_tool("pid_x", "export_novel", {"format": "txt"})
check("export_novel 返回 summary",
      "summary" in r, r)
check("filename 正确解析",
      r["data"]["filename"] == "pid_x.txt", r)
check("download_url 含 pid 和 format",
      "pid_x" in r["data"]["download_url"] and "txt" in r["data"]["download_url"], r)

# 不支持的格式
r2 = da.dispatch_tool("pid_x", "export_novel", {"format": "pdf"})
check("不支持格式返回 error",
      "error" in r2, r2)


# ---------- 6. dispatch_tool list_chapters ----------
print("Test 6: dispatch_tool list_chapters")
mock_store.data["chapters.json"] = [
    {"chapter": 1, "title": "起航", "word_count": 2500, "scene": "山门", "finalized": True,
     "text": "张三站在山门前。\n\n他推开大门。"},
    {"chapter": 2, "title": "破阵", "word_count": 2800, "scene": "阵中", "finalized": True,
     "text": "李四破阵而出。\n\n他离开了阵中。"},
]
r = da.dispatch_tool("pid_x", "list_chapters", {})
check("summary 含章节数",
      "2" in r["summary"], r)
check("switch_panel == preview",
      r.get("switch_panel") == "preview", r)
check("data 含两条",
      len(r["data"]) == 2, r)


# ---------- 7. dispatch_tool get_chapter ----------
print("Test 7: dispatch_tool get_chapter")
r = da.dispatch_tool("pid_x", "get_chapter", {"chapter_num": 1})
check("返回正确章节",
      r["data"]["chapter"] == 1 and r["data"]["title"] == "起航", r)

r2 = da.dispatch_tool("pid_x", "get_chapter", {"chapter_num": 999})
check("章节不存在返回 error",
      "error" in r2, r2)


# ---------- 8. dispatch_tool search_chapters ----------
print("Test 8: dispatch_tool search_chapters")
r = da.dispatch_tool("pid_x", "search_chapters", {"keyword": "破阵"})
check("search 返回 summary 含匹配数",
      "summary" in r, r)
check("search 找到 1 条（在 title 不搜，搜 text）",
      len(r["data"]) == 1 and r["data"][0]["chapter"] == 2, r)

# 8.2 搜正文存在的词
r = da.dispatch_tool("pid_x", "search_chapters", {"keyword": "张三"})
check("搜'张三'找到 1 条",
      len(r["data"]) == 1 and r["data"][0]["chapter"] == 1, r)


# ---------- 9. dispatch_tool get_chapter_state ----------
print("Test 9: dispatch_tool get_chapter_state")
mock_ded.active_state = None
r = da.dispatch_tool("pid_x", "get_chapter_state", {})
check("无活动章节 summary 含'无活动'",
      "无活动" in r["summary"], r)

mock_ded.active_state = {"round": 5, "word_count": 1500, "stop_requested": False}
r = da.dispatch_tool("pid_x", "get_chapter_state", {})
check("有活动章节返回 active=True",
      r["data"].get("active") is True and r["data"]["round"] == 5, r)
check("switch_panel == chatroom",
      r.get("switch_panel") == "chatroom", r)


# ---------- 10. dispatch_tool start_chapter / stop_chapter ----------
print("Test 10: dispatch_tool start/stop_chapter")
r = da.dispatch_tool("pid_x", "start_chapter", {})
check("start_chapter summary 含章节号",
      "第1章" in r["summary"], r)
check("start_chapter switch_panel == chatroom",
      r.get("switch_panel") == "chatroom", r)

mock_ded.stop_called = False
r = da.dispatch_tool("pid_x", "stop_chapter", {})
check("stop_chapter 调用了 request_stop",
      mock_ded.stop_called is True, r)
check("stop_chapter 返回 stop_requested=True",
      r["data"]["stop_requested"] is True, r)


# ---------- 11. dispatch_tool set_multithread / list_active_scenes ----------
print("Test 11: dispatch_tool 多线程类工具")
r = da.dispatch_tool("pid_x", "set_multithread", {"enabled": True})
check("set_multithread summary 含'多'",
      "多" in r["summary"], r)
check("set_multithread data.multithread=True",
      r["data"]["multithread"] is True, r)

mock_ded.active_scenes = ["山门", "市集"]
mock_ded.paused_scenes = ["密室"]
r = da.dispatch_tool("pid_x", "list_active_scenes", {})
check("list_active_scenes 返回 active + paused",
      r["data"]["active"] == ["山门", "市集"] and r["data"]["paused"] == ["密室"], r)


# ---------- 12. chat_with_tools_stream 推演中拦截 ----------
print("Test 12: chat_with_tools_stream 推演中拦截")
mock_ded.streaming.add("pid_x")
mock_ded.stop_called = False

# 12.1 普通消息 → queued
events = parse_sse_events(da.chat_with_tools_stream("pid_x", "你好，今天天气不错"))
check("普通消息产生 queued 事件",
      any(e["type"] == "queued" for e in events), events)
check("普通消息不调 request_stop",
      mock_ded.stop_called is False)
check("普通消息以 done 结束",
      events[-1]["type"] == "done", events)

# 12.2 喊停消息 → text + done + 调 request_stop
mock_ded.stop_called = False
events = parse_sse_events(da.chat_with_tools_stream("pid_x", "喊停"))
check("喊停消息调用了 request_stop",
      mock_ded.stop_called is True)
check("喊停消息含 text 事件",
      any(e["type"] == "text" and "喊停" in e["content"] for e in events), events)
check("喊停消息以 done 结束",
      events[-1]["type"] == "done", events)

# 12.3 "停止" 也触发喊停
mock_ded.stop_called = False
events = parse_sse_events(da.chat_with_tools_stream("pid_x", "请停止"))
check("'停止' 也触发 request_stop",
      mock_ded.stop_called is True)

mock_ded.streaming.discard("pid_x")


# ---------- 13. chat_with_tools_stream 无 LLM 配置 ----------
print("Test 13: chat_with_tools_stream 无 LLM 配置")
mock_store.llm_config = {}  # 无 dialogue 配置
events = parse_sse_events(da.chat_with_tools_stream("pid_x", "你好"))
check("无配置产生 error 事件",
      any(e["type"] == "error" for e in events), events)
check("error 含 dialogue 提示",
      any(e["type"] == "error" and "dialogue" in e["message"] for e in events), events)


# ---------- 14. inject_context / clear_history ----------
print("Test 14: inject_context / clear_history")
mock_store.data = {}  # 清空
da.clear_history("pid_x")
check("clear_history 写入空 list",
      mock_store.data.get("dialogue_history.json") == [], mock_store.data)

mock_store.data["dialogue_history.json"] = [{"role": "user", "content": "历史1"}]
da.inject_context("pid_x", "请帮作者修改第3章")
history = mock_store.data["dialogue_history.json"]
check("inject_context 追加 system 消息",
      len(history) == 2 and history[-1]["role"] == "system" and "修改第3章" in history[-1]["content"],
      history)


# ---------- 15. commit_modification 无 pending ----------
print("Test 15: commit_modification / discard_modification 无 pending")
mock_store.data["pending_modification.json"] = None
r = da.dispatch_tool("pid_x", "commit_modification", {})
check("commit 无 pending 返回 error",
      "error" in r, r)
r = da.dispatch_tool("pid_x", "discard_modification", {})
check("discard 无 pending 返回 error",
      "error" in r, r)


# ---------- 16. commit_project_init 无 pending ----------
print("Test 16: commit_project_init 无 pending")
mock_store.data["pending_setting_package.json"] = None
r = da.dispatch_tool("pid_x", "commit_project_init", {})
check("commit_project_init 无 pending 返回 error",
      "error" in r, r)


# ---------- 还原 mock ----------
da.storage = orig_storage
da.deduction = orig_deduction
da.exporter = orig_exporter


# ---------- 总结 ----------
print()
if failures:
    print(f"FAILED: {len(failures)} 项")
    for f in failures:
        print(f"  - {f}")
    sys.exit(1)
print("ALL CHECKS PASSED")
