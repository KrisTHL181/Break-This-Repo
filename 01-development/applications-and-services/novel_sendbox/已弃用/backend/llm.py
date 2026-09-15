"""LLM 适配层：统一接口 + OpenAI/Claude/Gemini 适配器 + 重试。"""
import json
import time
import urllib.request
import urllib.error

import sse_utils

_parse_json_safe = sse_utils.parse_json_safe

DEFAULT_BASE_URLS = {
    "openai": "https://api.openai.com/v1",
    "claude": "https://api.anthropic.com",
    "gemini": "https://generativelanguage.googleapis.com/v1beta",
}

TIMEOUT = 60
RETRY_DELAYS = (1, 2, 4)  # 3 次重试，间隔 1s/2s/4s


def chat(config, messages):
    """非流式调用，返回完整文本字符串。失败重试 3 次。"""
    last_error = None
    for attempt, delay in enumerate(RETRY_DELAYS):
        try:
            return _dispatch(config, messages)
        except Exception as e:
            last_error = e
            if attempt < len(RETRY_DELAYS) - 1:
                time.sleep(delay)
    raise RuntimeError(f"LLM 调用失败 3 次: {last_error}")


def chat_stream(config, messages):
    """流式调用，生成器逐 chunk yield 文本。重试仅作用于连接建立阶段。"""
    last_error = None
    for attempt, delay in enumerate(RETRY_DELAYS):
        try:
            yielded = False
            for chunk in _dispatch_stream(config, messages):
                yielded = True
                yield chunk
            return
        except Exception as e:
            if yielded:
                raise
            last_error = e
            if attempt < len(RETRY_DELAYS) - 1:
                time.sleep(delay)
    raise RuntimeError(f"LLM 调用失败 3 次: {last_error}")


def _dispatch(config, messages):
    p = config.get("provider")
    if p == "openai":
        return _chat_openai(config, messages)
    if p == "claude":
        return _chat_claude(config, messages)
    if p == "gemini":
        return _chat_gemini(config, messages)
    raise ValueError(f"Unknown provider: {p}")


def _dispatch_stream(config, messages):
    p = config.get("provider")
    if p == "openai":
        return _chat_openai_stream(config, messages)
    if p == "claude":
        return _chat_claude_stream(config, messages)
    if p == "gemini":
        return _chat_gemini_stream(config, messages)
    raise ValueError(f"Unknown provider: {p}")


def _post_json(url, headers, body):
    req = urllib.request.Request(url, data=json.dumps(body).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            raw = resp.read().decode("utf-8")
            try:
                return json.loads(raw)
            except json.JSONDecodeError as e:
                raise RuntimeError(f"JSON 解析失败 ({url}): {e}; 响应体前500字: {raw[:500]!r}") from e
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")[:500]
        raise RuntimeError(f"HTTP {e.code} ({url}): {err_body}") from e


def _open_stream(url, headers, body):
    req = urllib.request.Request(url, data=json.dumps(body).encode("utf-8"), headers=headers, method="POST")
    try:
        return urllib.request.urlopen(req, timeout=TIMEOUT)
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")[:500]
        raise RuntimeError(f"HTTP {e.code}: {err_body}") from e


# ---------- OpenAI ----------

def _chat_openai(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["openai"]).rstrip("/")
    headers = {"Authorization": f"Bearer {config['api_key']}", "Content-Type": "application/json"}
    body = {"model": config["model"], "messages": messages, "stream": False,
            "temperature": config.get("temperature", 0.7), "max_tokens": config.get("max_tokens", 4096)}
    data = _post_json(f"{base}/chat/completions", headers, body)
    choices = data.get("choices")
    if not choices:
        raise RuntimeError("API 返回空 choices")
    return choices[0]["message"]["content"]


def _chat_openai_stream(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["openai"]).rstrip("/")
    headers = {"Authorization": f"Bearer {config['api_key']}", "Content-Type": "application/json"}
    body = {"model": config["model"], "messages": messages, "stream": True,
            "temperature": config.get("temperature", 0.7), "max_tokens": config.get("max_tokens", 4096)}
    resp = _open_stream(f"{base}/chat/completions", headers, body)
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").strip()
            if not line.startswith("data:"):
                continue
            payload = line[5:].strip()
            if payload == "[DONE]":
                break
            if not payload:
                continue
            chunk = json.loads(payload)
            choices = chunk.get("choices") or [{}]
            text = choices[0].get("delta", {}).get("content")
            if text:
                yield text


# ---------- Claude ----------

def _split_system(messages):
    """Claude 把 system 单独放，messages 里不能有 role=system。"""
    system_parts = [m["content"] for m in messages if m["role"] == "system"]
    rest = [{"role": m["role"], "content": m["content"]} for m in messages if m["role"] != "system"]
    return "\n\n".join(system_parts), rest


def _chat_claude(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["claude"]).rstrip("/")
    headers = {
        "x-api-key": config["api_key"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    system, rest = _split_system(messages)
    body = {"model": config["model"], "max_tokens": config.get("max_tokens", 4096),
            "temperature": config.get("temperature", 0.7),
            "system": system, "messages": rest, "stream": False}
    data = _post_json(f"{base}/messages", headers, body)
    content = data.get("content")
    if not content:
        raise RuntimeError("API 返回空 content")
    return content[0]["text"]


def _chat_claude_stream(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["claude"]).rstrip("/")
    headers = {
        "x-api-key": config["api_key"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    system, rest = _split_system(messages)
    body = {"model": config["model"], "max_tokens": config.get("max_tokens", 4096),
            "temperature": config.get("temperature", 0.7),
            "system": system, "messages": rest, "stream": True}
    resp = _open_stream(f"{base}/v1/messages", headers, body)
    event = None
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").rstrip()
            if line.startswith("event:"):
                event = line[6:].strip()
            elif line.startswith("data:") and event == "content_block_delta":
                payload = line[5:].strip()
                if not payload:
                    continue
                chunk = json.loads(payload)
                text = chunk.get("delta", {}).get("text")
                if text:
                    yield text


# ---------- Gemini ----------

def _build_gemini_body(messages, config):
    """Gemini 用 'model' 而非 'assistant'，system 单独放 systemInstruction。"""
    contents = []
    system_parts = []
    for m in messages:
        if m["role"] == "system":
            system_parts.append(m["content"])
        else:
            role = "model" if m["role"] == "assistant" else "user"
            contents.append({"role": role, "parts": [{"text": m["content"]}]})
    body = {"contents": contents,
            "generationConfig": {"temperature": config.get("temperature", 0.7),
                                 "maxOutputTokens": config.get("max_tokens", 4096)}}
    if system_parts:
        body["systemInstruction"] = {"parts": [{"text": "\n\n".join(system_parts)}]}
    return body


def _chat_gemini(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["gemini"]).rstrip("/")
    url = f"{base}/models/{config['model']}:generateContent?key={config['api_key']}"
    headers = {"Content-Type": "application/json"}
    data = _post_json(url, headers, _build_gemini_body(messages, config))
    candidates = data.get("candidates")
    if not candidates:
        raise RuntimeError("API 返回空 candidates")
    return candidates[0]["content"]["parts"][0]["text"]


def _chat_gemini_stream(config, messages):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["gemini"]).rstrip("/")
    # 用 alt=sse 取 SSE 格式，统一为 data: 行解析；否则返回 chunked JSON 数组需增量解析
    url = f"{base}/models/{config['model']}:streamGenerateContent?alt=sse&key={config['api_key']}"
    headers = {"Content-Type": "application/json"}
    resp = _open_stream(url, headers, _build_gemini_body(messages, config))
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").strip()
            if not line.startswith("data:"):
                continue
            payload = line[5:].strip()
            if not payload:
                continue
            try:
                chunk = json.loads(payload)
                candidates = chunk.get("candidates") or [{}]
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    text = parts[0].get("text", "")
                    if text:
                        yield text
            except (KeyError, IndexError, json.JSONDecodeError):
                continue


# =====================================================================
# Function Calling 支持（对话员专用）
# =====================================================================
# 设计：保留 chat / chat_stream 不变（向后兼容），新增 chat_with_tools /
# chat_with_tools_stream 两个函数支持 function calling。
# 消息格式统一用 OpenAI 风格：[{role, content, tool_calls?, tool_call_id?, name?}]
# 各 provider 适配器内部转换为本格式。
# 返回统一结构：{text: str, tool_calls: [{id, name, args(dict)}]}
# 流式 yield 事件：{type: text|tool_call|done, ...}


def chat_with_tools(config, messages, tools, tool_choice=None):
    """非流式 function calling。
    返回 {text: str, tool_calls: [{id, name, args}]}。失败重试 3 次。"""
    last_error = None
    for attempt, delay in enumerate(RETRY_DELAYS):
        try:
            return _dispatch_tools(config, messages, tools, tool_choice)
        except Exception as e:
            last_error = e
            if attempt < len(RETRY_DELAYS) - 1:
                time.sleep(delay)
    raise RuntimeError(f"LLM 调用失败 3 次: {last_error}")


def chat_with_tools_stream(config, messages, tools, tool_choice=None):
    """流式 function calling。yield 事件：
        {"type": "text", "content": "..."}    文本增量
        {"type": "tool_call", "id": "...", "name": "...", "args": {...}}  完整工具调用
        {"type": "done"}                       流结束
    重试仅作用于连接建立阶段。"""
    last_error = None
    for attempt, delay in enumerate(RETRY_DELAYS):
        try:
            yielded = False
            for event in _dispatch_tools_stream(config, messages, tools, tool_choice):
                yielded = True
                yield event
            return
        except Exception as e:
            if yielded:
                raise
            last_error = e
            if attempt < len(RETRY_DELAYS) - 1:
                time.sleep(delay)
    raise RuntimeError(f"LLM 调用失败 3 次: {last_error}")


def _dispatch_tools(config, messages, tools, tool_choice):
    p = config.get("provider")
    if p == "openai":
        return _chat_openai_tools(config, messages, tools, tool_choice)
    if p == "claude":
        return _chat_claude_tools(config, messages, tools, tool_choice)
    if p == "gemini":
        return _chat_gemini_tools(config, messages, tools, tool_choice)
    raise ValueError(f"Unknown provider: {p}")


def _dispatch_tools_stream(config, messages, tools, tool_choice):
    p = config.get("provider")
    if p == "openai":
        return _chat_openai_tools_stream(config, messages, tools, tool_choice)
    if p == "claude":
        return _chat_claude_tools_stream(config, messages, tools, tool_choice)
    if p == "gemini":
        return _chat_gemini_tools_stream(config, messages, tools, tool_choice)
    raise ValueError(f"Unknown provider: {p}")


# ---------- OpenAI tools ----------

def _chat_openai_tools(config, messages, tools, tool_choice):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["openai"]).rstrip("/")
    headers = {"Authorization": f"Bearer {config['api_key']}", "Content-Type": "application/json"}
    body = {"model": config["model"], "messages": messages, "tools": tools, "stream": False,
            "temperature": config.get("temperature", 0.7), "max_tokens": config.get("max_tokens", 4096)}
    if tool_choice:
        body["tool_choice"] = tool_choice
    data = _post_json(f"{base}/chat/completions", headers, body)
    choices = data.get("choices")
    if not choices:
        raise RuntimeError("API 返回空 choices")
    msg = choices[0]["message"]
    text = msg.get("content") or ""
    tool_calls = []
    for tc in msg.get("tool_calls") or []:
        fn = tc.get("function", {})
        tool_calls.append({
            "id": tc.get("id", ""),
            "name": fn.get("name", ""),
            "args": _parse_json_safe(fn.get("arguments")),
        })
    return {"text": text, "tool_calls": tool_calls}


def _chat_openai_tools_stream(config, messages, tools, tool_choice):
    """OpenAI 流式 tool_calls：按 index 累积，结束时一次性 yield tool_call 事件。"""
    base = (config.get("base_url") or DEFAULT_BASE_URLS["openai"]).rstrip("/")
    headers = {"Authorization": f"Bearer {config['api_key']}", "Content-Type": "application/json"}
    body = {"model": config["model"], "messages": messages, "tools": tools, "stream": True,
            "temperature": config.get("temperature", 0.7), "max_tokens": config.get("max_tokens", 4096)}
    if tool_choice:
        body["tool_choice"] = tool_choice
    resp = _open_stream(f"{base}/chat/completions", headers, body)
    # 累积器：tool_calls by index
    tc_accum = {}  # {index: {id, name, arguments}}
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").strip()
            if not line.startswith("data:"):
                continue
            payload = line[5:].strip()
            if payload == "[DONE]":
                break
            if not payload:
                continue
            chunk = json.loads(payload)
            choices = chunk.get("choices") or [{}]
            delta = choices[0].get("delta", {})
            text = delta.get("content")
            if text:
                yield {"type": "text", "content": text}
            for tc in delta.get("tool_calls") or []:
                idx = tc.get("index", 0)
                slot = tc_accum.setdefault(idx, {"id": "", "name": "", "arguments": ""})
                if tc.get("id"):
                    slot["id"] = tc["id"]
                fn = tc.get("function", {})
                if fn.get("name"):
                    slot["name"] = fn["name"]
                if fn.get("arguments"):
                    slot["arguments"] += fn["arguments"]
    # 流结束后，按 index 顺序 emit tool_call 事件
    for idx in sorted(tc_accum.keys()):
        slot = tc_accum[idx]
        yield {"type": "tool_call", "id": slot["id"], "name": slot["name"],
               "args": _parse_json_safe(slot["arguments"])}
    yield {"type": "done"}


# ---------- Claude tools ----------

def _convert_tools_to_claude(tools):
    """OpenAI tools → Claude tools 格式。"""
    result = []
    for t in tools:
        fn = t.get("function", t)
        result.append({
            "name": fn["name"],
            "description": fn.get("description", ""),
            "input_schema": fn.get("parameters", {"type": "object", "properties": {}}),
        })
    return result


def _convert_messages_to_claude(messages):
    """OpenAI 消息 → Claude 消息。
    - system 提取到 system 字符串
    - assistant with tool_calls → content=[{type:text},{type:tool_use}]
    - tool → user with content=[{type:tool_result}]
    """
    system_parts = [m["content"] for m in messages if m["role"] == "system" and m.get("content")]
    rest = []
    for m in messages:
        if m["role"] == "system":
            continue
        if m["role"] == "tool":
            rest.append({
                "role": "user",
                "content": [{"type": "tool_result", "tool_use_id": m.get("tool_call_id", ""),
                             "content": m.get("content", "")}],
            })
        elif m["role"] == "assistant" and m.get("tool_calls"):
            content = []
            if m.get("content"):
                content.append({"type": "text", "text": m["content"]})
            for tc in m["tool_calls"]:
                fn = tc.get("function", {})
                content.append({
                    "type": "tool_use",
                    "id": tc.get("id", ""),
                    "name": fn.get("name", ""),
                    "input": _parse_json_safe(fn.get("arguments")),
                })
            rest.append({"role": "assistant", "content": content})
        else:
            rest.append({"role": m["role"], "content": m.get("content", "")})
    return "\n\n".join(system_parts), rest


def _chat_claude_tools(config, messages, tools, tool_choice):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["claude"]).rstrip("/")
    headers = {
        "x-api-key": config["api_key"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    system, rest = _convert_messages_to_claude(messages)
    body = {
        "model": config["model"],
        "max_tokens": config.get("max_tokens", 4096),
        "temperature": config.get("temperature", 0.7),
        "system": system,
        "messages": rest,
        "tools": _convert_tools_to_claude(tools),
        "stream": False,
    }
    if tool_choice == "auto":
        body["tool_choice"] = {"type": "auto"}
    elif tool_choice == "required":
        body["tool_choice"] = {"type": "any"}
    elif tool_choice and tool_choice != "none":
        body["tool_choice"] = {"type": "tool", "name": tool_choice}
    data = _post_json(f"{base}/v1/messages", headers, body)
    text = ""
    tool_calls = []
    for block in data.get("content", []):
        if block.get("type") == "text":
            text += block.get("text", "")
        elif block.get("type") == "tool_use":
            tool_calls.append({
                "id": block.get("id", ""),
                "name": block.get("name", ""),
                "args": block.get("input", {}),
            })
    return {"text": text, "tool_calls": tool_calls}


def _chat_claude_tools_stream(config, messages, tools, tool_choice):
    """Claude 流式 tool_use：content_block_start 启动 tool_use 块，
    content_block_delta 累积 input_json_delta，结束时 emit tool_call。"""
    base = (config.get("base_url") or DEFAULT_BASE_URLS["claude"]).rstrip("/")
    headers = {
        "x-api-key": config["api_key"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    system, rest = _convert_messages_to_claude(messages)
    body = {
        "model": config["model"],
        "max_tokens": config.get("max_tokens", 4096),
        "temperature": config.get("temperature", 0.7),
        "system": system,
        "messages": rest,
        "tools": _convert_tools_to_claude(tools),
        "stream": True,
    }
    if tool_choice == "auto":
        body["tool_choice"] = {"type": "auto"}
    elif tool_choice == "required":
        body["tool_choice"] = {"type": "any"}
    elif tool_choice and tool_choice != "none":
        body["tool_choice"] = {"type": "tool", "name": tool_choice}
    resp = _open_stream(f"{base}/v1/messages", headers, body)
    # 累积器：tool_use blocks by index
    tu_accum = {}  # {index: {id, name, input_json}}
    event = None
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").rstrip()
            if line.startswith("event:"):
                event = line[6:].strip()
            elif line.startswith("data:") and event:
                payload = line[5:].strip()
                if not payload:
                    continue
                chunk = json.loads(payload)
                if event == "content_block_start":
                    block = chunk.get("content_block", {})
                    if block.get("type") == "tool_use":
                        idx = chunk.get("index", 0)
                        tu_accum[idx] = {
                            "id": block.get("id", ""),
                            "name": block.get("name", ""),
                            "input_json": "",
                        }
                elif event == "content_block_delta":
                    delta = chunk.get("delta", {})
                    idx = chunk.get("index", 0)
                    if delta.get("type") == "text_delta":
                        text = delta.get("text", "")
                        if text:
                            yield {"type": "text", "content": text}
                    elif delta.get("type") == "input_json_delta":
                        if idx in tu_accum:
                            tu_accum[idx]["input_json"] += delta.get("partial_json", "")
                elif event == "content_block_stop":
                    idx = chunk.get("index", 0)
                    # text block 结束不需要做事；tool_use block 结束 emit
                event = None  # 重置以等待下一个 event
    # emit 所有累积的 tool_use
    for idx in sorted(tu_accum.keys()):
        slot = tu_accum[idx]
        yield {"type": "tool_call", "id": slot["id"], "name": slot["name"],
               "args": _parse_json_safe(slot["input_json"])}
    yield {"type": "done"}


# ---------- Gemini tools ----------

def _convert_tools_to_gemini(tools):
    """OpenAI tools → Gemini functionDeclarations。"""
    funcs = []
    for t in tools:
        fn = t.get("function", t)
        funcs.append({
            "name": fn["name"],
            "description": fn.get("description", ""),
            "parameters": fn.get("parameters", {"type": "object", "properties": {}}),
        })
    return [{"functionDeclarations": funcs}]


def _convert_messages_to_gemini(messages):
    """OpenAI 消息 → Gemini contents。
    - system → systemInstruction 字符串（单独返回）
    - assistant with tool_calls → model parts=[{text},{functionCall}]
    - tool → user parts=[{functionResponse: {name, response}}]
    """
    system_parts = []
    contents = []
    for m in messages:
        if m["role"] == "system":
            if m.get("content"):
                system_parts.append(m["content"])
            continue
        if m["role"] == "tool":
            # Gemini functionResponse.response 必须是 object
            content_str = m.get("content", "")
            response_data = _parse_json_safe(content_str)
            if not isinstance(response_data, dict):
                response_data = {"result": content_str}
            contents.append({
                "role": "user",
                "parts": [{"functionResponse": {"name": m.get("name", "tool"),
                                                "response": response_data}}],
            })
        elif m["role"] == "assistant" and m.get("tool_calls"):
            parts = []
            if m.get("content"):
                parts.append({"text": m["content"]})
            for tc in m["tool_calls"]:
                fn = tc.get("function", {})
                parts.append({"functionCall": {
                    "name": fn.get("name", ""),
                    "args": _parse_json_safe(fn.get("arguments")),
                }})
            contents.append({"role": "model", "parts": parts})
        else:
            role = "model" if m["role"] == "assistant" else "user"
            contents.append({"role": role, "parts": [{"text": m.get("content", "")}]})
    return "\n\n".join(system_parts), contents


def _chat_gemini_tools(config, messages, tools, tool_choice):
    base = (config.get("base_url") or DEFAULT_BASE_URLS["gemini"]).rstrip("/")
    url = f"{base}/models/{config['model']}:generateContent?key={config['api_key']}"
    headers = {"Content-Type": "application/json"}
    system, contents = _convert_messages_to_gemini(messages)
    body = {"contents": contents, "tools": _convert_tools_to_gemini(tools),
            "generationConfig": {"temperature": config.get("temperature", 0.7),
                                 "maxOutputTokens": config.get("max_tokens", 4096)}}
    if system:
        body["systemInstruction"] = {"parts": [{"text": system}]}
    if tool_choice == "none":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "NONE"}}
    elif tool_choice == "required":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "ANY"}}
    elif tool_choice and tool_choice != "auto":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "ANY", "allowedFunctionNames": [tool_choice]}}
    data = _post_json(url, headers, body)
    text = ""
    tool_calls = []
    candidates = data.get("candidates") or [{}]
    parts = candidates[0].get("content", {}).get("parts", [])
    for p in parts:
        if "text" in p:
            text += p["text"]
        elif "functionCall" in p:
            fc = p["functionCall"]
            tool_calls.append({
                "id": f"call_{len(tool_calls)}",
                "name": fc.get("name", ""),
                "args": fc.get("args", {}),
            })
    return {"text": text, "tool_calls": tool_calls}


def _chat_gemini_tools_stream(config, messages, tools, tool_choice):
    """Gemini 流式 functionCall：Gemini 通常一次 chunk 内含完整 functionCall，直接 emit。"""
    base = (config.get("base_url") or DEFAULT_BASE_URLS["gemini"]).rstrip("/")
    url = f"{base}/models/{config['model']}:streamGenerateContent?alt=sse&key={config['api_key']}"
    headers = {"Content-Type": "application/json"}
    system, contents = _convert_messages_to_gemini(messages)
    body = {"contents": contents, "tools": _convert_tools_to_gemini(tools),
            "generationConfig": {"temperature": config.get("temperature", 0.7),
                                 "maxOutputTokens": config.get("max_tokens", 4096)}}
    if system:
        body["systemInstruction"] = {"parts": [{"text": system}]}
    if tool_choice == "none":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "NONE"}}
    elif tool_choice == "required":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "ANY"}}
    elif tool_choice and tool_choice != "auto":
        body["toolConfig"] = {"functionCallingConfig": {"mode": "ANY", "allowedFunctionNames": [tool_choice]}}
    resp = _open_stream(url, headers, body)
    tc_counter = 0
    with resp:
        for raw in resp:
            line = raw.decode("utf-8", errors="replace").strip()
            if not line.startswith("data:"):
                continue
            payload = line[5:].strip()
            if not payload:
                continue
            try:
                chunk = json.loads(payload)
                candidates = chunk.get("candidates") or [{}]
                parts = candidates[0].get("content", {}).get("parts", [])
                for p in parts:
                    if "text" in p and p["text"]:
                        yield {"type": "text", "content": p["text"]}
                    elif "functionCall" in p:
                        fc = p["functionCall"]
                        yield {"type": "tool_call", "id": f"call_{tc_counter}",
                               "name": fc.get("name", ""), "args": fc.get("args", {})}
                        tc_counter += 1
            except (KeyError, IndexError, json.JSONDecodeError):
                continue
    yield {"type": "done"}
