"""Task R1 自检：验证 function calling 适配器的消息/工具格式转换 + 结构解析。
不实际调用 LLM，只验证转换逻辑与返回结构。
运行：python tests/test_llm_tools.py
"""
import json
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backend"))

import llm

failures = []


def check(name, cond):
    if cond:
        print(f"  [OK] {name}")
    else:
        print(f"  [FAIL] {name}")
        failures.append(name)


# ---------- 1. _parse_json_safe ----------
print("Test 1: _parse_json_safe")
check("empty string → {}",
      llm._parse_json_safe("") == {})
check("valid json",
      llm._parse_json_safe('{"a":1}') == {"a": 1})
check("invalid json → {}",
      llm._parse_json_safe('not json') == {})
check("None → {}",
      llm._parse_json_safe(None) == {})

# ---------- 2. OpenAI tools 适配器返回结构 ----------
print("Test 2: OpenAI tools response parsing")
# 模拟 OpenAI 非流式响应
fake_openai_resp = {
    "choices": [{
        "message": {
            "role": "assistant",
            "content": "I will call a tool",
            "tool_calls": [{
                "id": "call_abc",
                "type": "function",
                "function": {"name": "get_project_info", "arguments": '{"pid": "123"}'}
            }]
        }
    }]
}
# 模拟 _chat_openai_tools 的解析逻辑（不发起 HTTP）
msg = fake_openai_resp["choices"][0]["message"]
text = msg.get("content") or ""
tool_calls = []
for tc in msg.get("tool_calls") or []:
    fn = tc.get("function", {})
    tool_calls.append({
        "id": tc.get("id", ""),
        "name": fn.get("name", ""),
        "args": llm._parse_json_safe(fn.get("arguments")),
    })
check("text parsed", text == "I will call a tool")
check("tool_call id", tool_calls[0]["id"] == "call_abc")
check("tool_call name", tool_calls[0]["name"] == "get_project_info")
check("tool_call args", tool_calls[0]["args"] == {"pid": "123"})

# ---------- 3. Claude tools 格式转换 ----------
print("Test 3: Claude tools/messages conversion")
openai_tools = [{
    "type": "function",
    "function": {
        "name": "start_chapter",
        "description": "开始新章节",
        "parameters": {"type": "object", "properties": {"pid": {"type": "string"}}}
    }
}]
claude_tools = llm._convert_tools_to_claude(openai_tools)
check("claude tool name", claude_tools[0]["name"] == "start_chapter")
check("claude tool has input_schema", "input_schema" in claude_tools[0])
check("claude tool no 'function' wrapper", "function" not in claude_tools[0])

openai_messages = [
    {"role": "system", "content": "你是对话员"},
    {"role": "user", "content": "开始推演"},
    {"role": "assistant", "content": "好的", "tool_calls": [{
        "id": "call_1", "function": {"name": "start_chapter", "arguments": '{}'}
    }]},
    {"role": "tool", "tool_call_id": "call_1", "content": '{"chapter": 3}'}
]
system, rest = llm._convert_messages_to_claude(openai_messages)
check("system extracted", system == "你是对话员")
check("rest length 3 (no system)", len(rest) == 3)
check("tool → user role", rest[2]["role"] == "user")
check("tool_result content type", rest[2]["content"][0]["type"] == "tool_result")
check("tool_result id", rest[2]["content"][0]["tool_use_id"] == "call_1")
check("assistant tool_use content",
      any(b.get("type") == "tool_use" for b in rest[1]["content"]))

# ---------- 4. Gemini tools 格式转换 ----------
print("Test 4: Gemini tools/messages conversion")
gemini_tools = llm._convert_tools_to_gemini(openai_tools)
check("gemini tools wrapped in functionDeclarations",
      "functionDeclarations" in gemini_tools[0])
check("gemini func name", gemini_tools[0]["functionDeclarations"][0]["name"] == "start_chapter")
check("gemini func has parameters", "parameters" in gemini_tools[0]["functionDeclarations"][0])

system, contents = llm._convert_messages_to_gemini(openai_messages)
check("gemini system extracted", system == "你是对话员")
check("gemini contents length 3 (no system)", len(contents) == 3)
check("gemini user role preserved", contents[0]["role"] == "user")
check("gemini assistant → model role", contents[1]["role"] == "model")
check("gemini tool → functionResponse",
      "functionResponse" in contents[2]["parts"][0])
check("gemini functionResponse name",
      contents[2]["parts"][0]["functionResponse"]["name"] == "tool")
check("gemini functionResponse response is dict",
      isinstance(contents[2]["parts"][0]["functionResponse"]["response"], dict))

# ---------- 5. Gemini functionResponse with JSON content ----------
print("Test 5: Gemini functionResponse JSON parsing")
msgs_with_json = [
    {"role": "tool", "tool_call_id": "c1", "name": "get_chapter",
     "content": '{"title": "第3章", "word_count": 2500}'}
]
_, contents2 = llm._convert_messages_to_gemini(msgs_with_json)
check("gemini functionResponse name from msg",
      contents2[0]["parts"][0]["functionResponse"]["name"] == "get_chapter")
check("gemini functionResponse parsed json",
      contents2[0]["parts"][0]["functionResponse"]["response"] == {"title": "第3章", "word_count": 2500})

# ---------- 6. 入口函数存在性 ----------
print("Test 6: Public API exists")
check("chat_with_tools exists", callable(llm.chat_with_tools))
check("chat_with_tools_stream exists", callable(llm.chat_with_tools_stream))
check("_dispatch_tools exists", callable(llm._dispatch_tools))
check("_dispatch_tools_stream exists", callable(llm._dispatch_tools_stream))

# ---------- 7. provider 路由 ----------
print("Test 7: Provider dispatch")
try:
    llm._dispatch_tools({"provider": "unknown"}, [], [], None)
    check("unknown provider raises", False)
except ValueError:
    check("unknown provider raises ValueError", True)
except Exception:
    check("unknown provider raises ValueError", False)

# ---------- 总结 ----------
print()
if failures:
    print(f"FAILED: {len(failures)} checks failed: {failures}")
    sys.exit(1)
else:
    print("ALL CHECKS PASSED")
