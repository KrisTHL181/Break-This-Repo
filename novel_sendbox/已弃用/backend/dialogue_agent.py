"""对话 AI - 对话员（Dialogue Agent）+ Function Calling 工具集。
工具定义和调度已拆分到 tools.py，本文件保留对话循环和 prompt 构建。"""
import json

import llm
import storage
import deduction
import sse_utils

# 从 tools.py 导入工具集（保持向后兼容的重导出）
from tools import (
    TOOLS, dispatch_tool,
    PENDING_SETTING_FILE, PENDING_MODIFICATION_FILE,
    analyze_requirement, generate_setting_package, search_chapters,
)

_emit = sse_utils.emit
_parse_json = sse_utils.parse_json

# =====================================================================
# 对话员（Dialogue Agent）+ Function Calling 工具集
# =====================================================================
# 设计：对话员 LLM 通过 function calling 调用工具完成所有系统操作。
# 文本修改类工具（标记 ★）内部调群主 LLM（gm 配置）执行实际修改。
# 工具结果返回 {summary, data, switch_panel?} 三段：
#   - summary: 聊天框简报文本
#   - data: 完整数据（前端右侧面板详展）
#   - switch_panel: 触发前端切换面板（chatroom/preview/finalization/visualization）

DIALOGUE_PROMPT = """你是小说创作对话员，负责与作者沟通并调度工具完成所有小说创作操作。

你的职责：
1. 与作者自然对话，理解创作意图
2. 根据意图调用合适工具执行操作
3. 用简短语言向作者汇报工具执行结果
4. 主动引导作者完成创作流程（创建项目→推演→定稿→完本导出）

工具使用原则：
- 需要获取信息时调用查询类工具（get_project_info / list_chapters / get_chapter 等）
- 需要执行操作时调用对应工具（start_chapter / validate_chapter / finalize_chapter 等）
- 工具调用后向作者简报结果，不要复述完整数据
- 一次只调用必要的工具，不要批量调用

特殊场景：
- 推演进行中：作者只能说"喊停"，其他消息会被系统拦截
- 定稿流程：先调 validate_chapter 校验，作者反馈后调 apply_feedback，最后调 finalize_chapter
- 修改流程：先调 locate_chapter 定位，再调 execute_modification 执行，作者确认后调 commit_modification
- 新建项目：先调 analyze_requirement 分析需求，再调 generate_setting_package 生成设定包，作者确认后调 commit_project_init

回答风格：
- 简洁友好，像一个经验丰富的编辑
- 中文回复
- 工具调用前简要说明意图（"让我先查看项目信息"）
- 工具调用后简报结果（"第3章已开始推演，请查看右侧聊天室"）
"""


def _build_system_prompt(pid):
    """构建系统提示：自定义 dialogue prompt（按 项目>全局>默认 优先级）+ 当前项目上下文。"""
    meta = storage.get_project(pid) or {}
    chapters = storage.load_data(pid, "chapters.json") or []
    context = f"""
当前项目上下文：
- 标题：{meta.get('title', '')}
- 题材：{meta.get('genre', '')}
- 当前章节号：{meta.get('current_chapter', 0)}
- 当前场景：{meta.get('current_scene', '')}
- 多线程模式：{meta.get('multithread', False)}
- 焦点角色：{', '.join(meta.get('focus_characters', []))}
- 已定稿章节数：{len(chapters)}
- 字数目标：{meta.get('word_count_target', {'min': 2000, 'max': 3000})}
"""
    return storage.load_prompt(pid, "dialogue") + context


# ---------- 对话员主循环 ----------

_MAX_TOOL_ITERATIONS = 10


def chat_with_tools_stream(pid, user_message):
    """对话员流式聊天（含工具调用）。"""
    if deduction.is_streaming(pid) and user_message.strip() not in ("喊停", "停止", "stop", "停"):
        yield _emit("error", {"message": "推演进行中，请喊停后再操作"})
        return

    history = storage.load_dialogue_history(pid)
    system_prompt = _build_system_prompt(pid)
    messages = [{"role": "system", "content": system_prompt}] + list(history) + [{"role": "user", "content": user_message}]

    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        yield _emit("error", {"message": "未配置 dialogue agent 的 LLM（请在设置页配置）"})
        return

    got_assistant_response = False
    pending_assistant_text = ""
    try:
        final_assistant_text = ""
        for iteration in range(_MAX_TOOL_ITERATIONS):
            assistant_text = ""
            tool_calls = []
            try:
                for event in llm.chat_with_tools_stream(cfg, messages, TOOLS):
                    if event["type"] == "text":
                        assistant_text += event["content"]
                        yield _emit("text", {"content": event["content"]})
                    elif event["type"] == "tool_call":
                        tool_calls.append(event)
                    elif event["type"] == "done":
                        break
            except Exception as e:
                yield _emit("error", {"message": f"LLM 调用失败: {e}"})
                if assistant_text:
                    messages.append({"role": "assistant", "content": assistant_text})
                    got_assistant_response = True
                break

            final_assistant_text += assistant_text
            pending_assistant_text = assistant_text

            if not tool_calls:
                if assistant_text:
                    messages.append({"role": "assistant", "content": assistant_text})
                    got_assistant_response = True
                pending_assistant_text = ""
                break

            messages.append({
                "role": "assistant",
                "content": assistant_text,
                "tool_calls": [
                    {"id": tc["id"], "type": "function",
                     "function": {"name": tc["name"], "arguments": json.dumps(tc["args"], ensure_ascii=False)}}
                    for tc in tool_calls
                ],
            })
            got_assistant_response = True
            pending_assistant_text = ""

            for tc in tool_calls:
                yield _emit("tool_call", {"id": tc["id"], "name": tc["name"], "args": tc["args"]})
                result = dispatch_tool(pid, tc["name"], tc["args"])
                gm_calls = result.pop("gm_calls", []) or []
                for gm in gm_calls:
                    yield _emit("gm_dialogue", {"tool_name": tc["name"], **gm})
                yield _emit("tool_result", {"id": tc["id"], "name": tc["name"], **result})
                tool_content = json.dumps(result.get("data", result), ensure_ascii=False)
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc["id"],
                    "name": tc["name"],
                    "content": tool_content,
                })

        yield _emit("done", {})
    finally:
        if pending_assistant_text and messages and messages[-1].get("role") != "assistant":
            messages.append({"role": "assistant", "content": pending_assistant_text})
            got_assistant_response = True
        if got_assistant_response:
            saved = [m for m in messages[1:] if m.get("role") != "tool"]
            if saved and saved[-1].get("role") == "assistant" and saved[-1].get("tool_calls"):
                saved[-1] = {k: v for k, v in saved[-1].items() if k != "tool_calls"}
                if not saved[-1].get("content"):
                    saved.pop()
            storage.save_dialogue_history(pid, saved)


def proactive_chat_stream(pid):
    """R14: 对话员主动开口。新建项目 / 进入历史空项目时调用。"""
    if deduction.is_streaming(pid):
        yield _emit("error", {"message": "推演进行中，无法主动开口"})
        return

    history = storage.load_dialogue_history(pid)
    system_prompt = _build_system_prompt(pid)
    placeholder = "（系统：请主动开口引导作者。如果是新项目，询问作者想写什么小说；如果项目已有设定但无章节，建议开始第一章推演。）"
    messages = [{"role": "system", "content": system_prompt}] + list(history) + [{"role": "user", "content": placeholder}]

    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        yield _emit("error", {"message": "未配置 dialogue agent 的 LLM（请在设置页配置）"})
        return

    got_assistant_response = False
    pending_assistant_text = ""
    try:
        for iteration in range(_MAX_TOOL_ITERATIONS):
            assistant_text = ""
            tool_calls = []
            try:
                for event in llm.chat_with_tools_stream(cfg, messages, TOOLS):
                    if event["type"] == "text":
                        assistant_text += event["content"]
                        yield _emit("text", {"content": event["content"]})
                    elif event["type"] == "tool_call":
                        tool_calls.append(event)
                    elif event["type"] == "done":
                        break
            except Exception as e:
                yield _emit("error", {"message": f"LLM 调用失败: {e}"})
                if assistant_text:
                    messages.append({"role": "assistant", "content": assistant_text})
                    got_assistant_response = True
                break

            pending_assistant_text = assistant_text

            if not tool_calls:
                if assistant_text:
                    messages.append({"role": "assistant", "content": assistant_text})
                    got_assistant_response = True
                pending_assistant_text = ""
                break

            assistant_msg = {
                "role": "assistant",
                "content": assistant_text,
                "tool_calls": [
                    {"id": tc["id"], "type": "function",
                     "function": {"name": tc["name"], "arguments": json.dumps(tc["args"], ensure_ascii=False)}}
                    for tc in tool_calls
                ],
            }
            messages.append(assistant_msg)
            got_assistant_response = True
            pending_assistant_text = ""

            for tc in tool_calls:
                yield _emit("tool_call", {"id": tc["id"], "name": tc["name"], "args": tc["args"]})
                result = dispatch_tool(pid, tc["name"], tc["args"])
                yield _emit("tool_result", {"id": tc["id"], "name": tc["name"], **result})
                tool_content = json.dumps(result.get("data", result), ensure_ascii=False)
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc["id"],
                    "name": tc["name"],
                    "content": tool_content,
                })

        yield _emit("done", {})
    finally:
        if pending_assistant_text and messages and messages[-1].get("role") != "assistant":
            messages.append({"role": "assistant", "content": pending_assistant_text})
            got_assistant_response = True
        if got_assistant_response:
            saved = [m for m in messages[1:] if m.get("role") != "tool"]
            if saved and saved[-1].get("role") == "assistant" and saved[-1].get("tool_calls"):
                saved[-1] = {k: v for k, v in saved[-1].items() if k != "tool_calls"}
                if not saved[-1].get("content"):
                    saved.pop()
            storage.save_dialogue_history(pid, saved)


def inject_context(pid, system_message):
    """注入系统上下文消息（用于章节库修改跳转）。
    在历史末尾追加一条 system 消息，让对话员识别意图。"""
    history = storage.load_dialogue_history(pid)
    history.append({"role": "system", "content": system_message})
    storage.save_dialogue_history(pid, history)