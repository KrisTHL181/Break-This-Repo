"""对话员工具集：TOOLS schema + dispatch_tool + 所有工具处理函数 + 辅助函数。
从 dialogue_agent.py 拆分，减少单文件体积。"""
import json
import uuid as _uuid

import llm
import storage
import deduction
import finalization
import assistant
import exporter
import sse_utils

# editing 延迟导入，避免与 dialogue_agent 的循环依赖
# (editing → dialogue_agent → tools → editing)
_editing = None


def _get_editing():
    global _editing
    if _editing is None:
        import editing as _mod
        _editing = _mod
    return _editing

_emit = sse_utils.emit
_parse_json = sse_utils.parse_json

# 暂存文件名
PENDING_SETTING_FILE = "pending_setting_package.json"
PENDING_MODIFICATION_FILE = "pending_modification.json"

# ---------- 提示词 ----------

ANALYZE_PROMPT = """你是一个小说创作需求分析师。用户会给你一句话描述想写的小说。
请分析并返回 JSON（只返回 JSON，不要 markdown 代码块，不要其他文字）：
{
  "title": "项目标题（简短，如'修仙重生录'）",
  "genre": "题材分类（如修仙/科幻/悬疑/言情，可多个用逗号分隔）",
  "elements": ["核心要素1", "核心要素2"],
  "rule_hints": ["显性规则暗示1"],
  "contradictions": [
    {"item1": "矛盾点A", "item2": "矛盾点B", "suggestion": "修改建议"}
  ]
}
若用户需求无自相矛盾，contradictions 返回空数组 []。"""

GENERATE_PROMPT = """你是一个小说设定设计师。根据用户需求，生成完整的小说初始设定包。
返回 JSON（只返回 JSON，不要 markdown 代码块，不要其他文字）：
{
  "world_rules": {"rules": ["规则1（如：人类不可复活）", "规则2（如：魔法消耗体力）"]},
  "characters": [
    {
      "name": "姓名",
      "personality": "性格描述（自然语言）",
      "current_goal": "当前目标",
      "current_emotion": "当前情绪",
      "current_location": "初始位置（应与初始场景名一致）",
      "exposure_status": "exposed",
      "hidden_fields": {},
      "is_focus": true
    }
  ],
  "initial_scene": {"name": "场景名", "description": "一句场景描述"},
  "completion_conditions": [
    {"id": "cond1", "title": "完本条件1（如：主角飞升）", "achieved": false, "achieved_chapter": null}
  ]
}
要求：
- 至少 1 名主角（is_focus=true，通常首位）
- 完本条件 2-4 个
- 角色卡字段完整
- 初始场景描述简洁有画面感"""


# ---------- 辅助函数（原在 dialogue_agent.py） ----------

def analyze_requirement(user_input, llm_config):
    """分析用户需求，返回 {title, genre, elements, rule_hints, contradictions}"""
    messages = [
        {"role": "system", "content": ANALYZE_PROMPT},
        {"role": "user", "content": user_input},
    ]
    raw = llm.chat(llm_config, messages)
    return _parse_json(raw)


def generate_setting_package(user_input, llm_config):
    """生成设定包，返回 {world_rules, characters, initial_scene, completion_conditions}"""
    messages = [
        {"role": "system", "content": GENERATE_PROMPT},
        {"role": "user", "content": user_input},
    ]
    raw = llm.chat(llm_config, messages)
    return _parse_json(raw)


def search_chapters(keyword, chapters):
    """全库关键词搜索（纯代码，不调 LLM）。
    返回 [{chapter, title, snippet, position}]，最多 10 条。
    snippet 为关键词前后 30 字上下文。"""
    if not keyword:
        return []
    results = []
    for ch in chapters or []:
        text = ch.get("text") or ""
        title = ch.get("title") or ""
        ch_num = ch.get("chapter")
        paragraphs = text.split("\n\n")
        for i, p in enumerate(paragraphs):
            start = 0
            while True:
                idx = p.find(keyword, start)
                if idx == -1:
                    break
                lo = max(0, idx - 30)
                hi = min(len(p), idx + len(keyword) + 30)
                results.append({
                    "chapter": ch_num,
                    "title": title,
                    "snippet": p[lo:hi],
                    "position": f"段{i + 1}",
                })
                if len(results) >= 10:
                    return results
                start = idx + len(keyword)
    return results


# ---------- 工具 Schema ----------

def _obj(properties, required=None):
    return {"type": "object", "properties": properties, **(dict(required=required) if required else {})}


TOOLS = [
    # 项目信息类
    {"type": "function", "function": {"name": "get_project_info", "description": "获取当前项目的元信息（标题/题材/当前章节/场景/焦点角色等）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "list_chapters", "description": "列出所有已定稿章节", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "get_chapter", "description": "获取指定章节的正文", "parameters": _obj({"chapter_num": {"type": "integer", "description": "章节号"}}, ["chapter_num"])}},
    {"type": "function", "function": {"name": "search_chapters", "description": "全库关键词搜索章节内容", "parameters": _obj({"keyword": {"type": "string", "description": "搜索关键词"}}, ["keyword"])}},

    # 推演类
    {"type": "function", "function": {"name": "start_chapter", "description": "开始新章节的推演", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "stop_chapter", "description": "喊停当前推演（轮次边界生效）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "get_chapter_state", "description": "获取当前推演状态（轮次/字数/场景/候选名单）", "parameters": _obj({})}},

    # 定稿类
    {"type": "function", "function": {"name": "validate_chapter", "description": "对当前章节进行首次因果校验", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "apply_feedback", "description": "★ 应用作者反馈修改（内部调群主AI）", "parameters": _obj({"feedback": {"type": "array", "description": "反馈列表", "items": {"type": "object", "properties": {"conflict_index": {"type": "integer", "description": "冲突索引（从 validate_chapter 或 detect_claim_conflicts 结果获取）"}, "action": {"type": "string", "enum": ["fix", "ignore"], "description": "fix=调群主AI修改，ignore=忽略"}, "instruction": {"type": "object", "description": "修改指令（action=fix 时必填）", "properties": {"target": {"type": "string"}, "position": {"type": "string"}, "operation": {"type": "string", "enum": ["replace", "insert", "delete", "reorder"]}, "content": {"type": "string"}, "scope": {"type": "string"}}}}, "required": ["conflict_index", "action"]}}}, ["feedback"])}},
    {"type": "function", "function": {"name": "finalize_chapter", "description": "定稿当前章节（数据固化，不可回退）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "discard_chapter", "description": "丢弃当前章节", "parameters": _obj({})}},

    # 修改类
    {"type": "function", "function": {"name": "locate_chapter", "description": "定位目标章节（按章号或关键词）", "parameters": _obj({"chapter_num": {"type": "integer"}, "keyword": {"type": "string"}})}},
    {"type": "function", "function": {"name": "execute_modification", "description": "★ 执行章节修改（内部调群主AI）。修改结果会暂存，等待作者确认后调用 commit_modification", "parameters": _obj({"chapter_num": {"type": "integer", "description": "章节号"}, "instruction": {"type": "object", "description": "结构化修改指令", "properties": {"target": {"type": "string", "description": "操作目标（如：张三的某句台词）"}, "position": {"type": "string", "description": "精准位置（段落号或原文片段描述）"}, "operation": {"type": "string", "enum": ["replace", "insert", "delete", "reorder"], "description": "replace=替换/insert=插入/delete=删除/reorder=调序"}, "content": {"type": "string", "description": "操作内容（替换为新文本/插入文本/删除留空/调序目标位置）"}, "scope": {"type": "string", "description": "影响范围（如：仅本段/本章后续/跨章）"}}, "required": ["target", "operation", "content"]}}, ["chapter_num", "instruction"])}},
    {"type": "function", "function": {"name": "commit_modification", "description": "提交暂存的修改（二次校验+定稿）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "discard_modification", "description": "放弃暂存的修改", "parameters": _obj({})}},

    # 系统辅助类
    {"type": "function", "function": {"name": "check_dormant_characters", "description": "检查休眠角色（连续20章未出场）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "apply_dormant", "description": "休眠指定角色", "parameters": _obj({"names": {"type": "array", "items": {"type": "string"}}}, ["names"])}},
    {"type": "function", "function": {"name": "apply_awaken", "description": "唤醒指定角色", "parameters": _obj({"names": {"type": "array", "items": {"type": "string"}}}, ["names"])}},
    {"type": "function", "function": {"name": "detect_claim_conflicts", "description": "检测宣称冲突（A说/B否认）", "parameters": _obj({"validation_result": {"type": "object", "description": "校验结果（可选，不传则读取最近校验）"}})}},
    {"type": "function", "function": {"name": "resolve_claim_conflict", "description": "★ 解决宣称冲突（追加结构化记忆记录到角色 memory_anchors）", "parameters": _obj({"conflict_index": {"type": "integer", "description": "冲突索引（从 detect_claim_conflicts 结果获取）"}, "resolution": {"type": "string", "enum": ["a_lie", "b_lie", "both_wrong", "other"], "description": "a_lie=A撒谎/B否认成立；b_lie=B撒谎/A成立；both_wrong=双方都错；other=另有隐情（需填 details）"}, "details": {"type": "string", "description": "补充说明（resolution=other 时必填）"}}, ["conflict_index", "resolution"])}},

    # 完本类
    {"type": "function", "function": {"name": "check_completion", "description": "检测完本条件达成情况", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "generate_finale_summary", "description": "生成完结总结（叙事者AI）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "export_novel", "description": "导出小说/工程文件。txt/md/epub 导出的是小说正文（不同格式），zip 导出的是工程文件（含全部数据：设定/章节/角色/事件等，用于备份或迁移，非小说正文，不能直接阅读）", "parameters": _obj({"format": {"type": "string", "enum": ["txt", "md", "epub", "zip"], "description": "导出格式：txt=纯文本小说 / md=Markdown 小说 / epub=电子书小说 / zip=工程文件（非小说正文，含项目全部数据）"}}, ["format"])}},

    # 可视化类
    {"type": "function", "function": {"name": "get_relationships", "description": "获取角色关系图数据", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "get_timeline", "description": "获取事件时间轴数据", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "get_word_stats", "description": "获取字数统计数据", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "get_foreshadows", "description": "获取伏笔关系图数据", "parameters": _obj({})}},

    # 多线程类
    {"type": "function", "function": {"name": "set_multithread", "description": "切换多线程模式", "parameters": _obj({"enabled": {"type": "boolean"}}, ["enabled"])}},
    {"type": "function", "function": {"name": "list_active_scenes", "description": "列出活动场景（多线程模式）", "parameters": _obj({})}},
    {"type": "function", "function": {"name": "pause_scene", "description": "暂停指定场景（单线程模式）", "parameters": _obj({"scene_name": {"type": "string"}}, ["scene_name"])}},
    {"type": "function", "function": {"name": "resume_scene", "description": "恢复指定场景（单线程模式）", "parameters": _obj({"scene_name": {"type": "string"}}, ["scene_name"])}},

    # 项目初始化类
    {"type": "function", "function": {"name": "analyze_requirement", "description": "分析作者需求（题材/要素/矛盾），用于新建项目", "parameters": _obj({"user_input": {"type": "string", "description": "作者的一句话需求描述"}}, ["user_input"])}},
    {"type": "function", "function": {"name": "generate_setting_package", "description": "生成设定包（世界规则/角色卡/初始场景/完本条件）", "parameters": _obj({"user_input": {"type": "string", "description": "作者的需求描述"}}, ["user_input"])}},
    {"type": "function", "function": {"name": "commit_project_init", "description": "确认设定包并完成项目初始化（写入数据+保存快照v0）", "parameters": _obj({})}},

    # R22 新增：项目元信息修改 + 角色删除
    {"type": "function", "function": {"name": "update_project_meta", "description": "修改项目元信息（目前支持改标题）", "parameters": _obj({"title": {"type": "string", "description": "新的小说标题（不传则不修改）"}}, [])}},
    {"type": "function", "function": {"name": "delete_character", "description": "删除指定角色（从 characters.json 移除 + 清理 relationships.json 涉及的关系）。焦点角色拒绝删除", "parameters": _obj({"names": {"type": "array", "items": {"type": "string"}, "description": "要删除的角色姓名列表"}}, ["names"])}},
]


# ---------- 工具调度 ----------

def dispatch_tool(pid, name, args):
    """执行工具调用。返回 {summary, data, switch_panel?} 或 {error}。"""
    handler = _TOOL_HANDLERS.get(name)
    if not handler:
        return {"error": f"未知工具: {name}"}
    try:
        return handler(pid, args or {})
    except Exception as e:
        return {"error": f"工具 {name} 执行失败: {e}"}


# ---------- 工具处理函数 ----------

def _t_get_project_info(pid, args):
    meta = storage.get_project(pid)
    if not meta:
        return {"error": "项目不存在"}
    return {"summary": f"项目《{meta.get('title', '')}》当前第{meta.get('current_chapter', 0)}章",
            "data": meta}


def _t_list_chapters(pid, args):
    chapters = storage.load_data(pid, "chapters.json") or []
    data = [{"chapter": c.get("chapter"), "title": c.get("title", ""),
             "word_count": c.get("word_count", 0), "scene": c.get("scene", ""),
             "finalized": c.get("finalized", False)} for c in chapters]
    return {"summary": f"共 {len(data)} 章已定稿", "data": data, "switch_panel": "preview"}


def _t_get_chapter(pid, args):
    chapter_num = args.get("chapter_num")
    chapters = storage.load_data(pid, "chapters.json") or []
    ch = next((c for c in chapters if c.get("chapter") == chapter_num), None)
    if not ch:
        return {"error": f"第{chapter_num}章不存在"}
    return {"summary": f"第{chapter_num}章《{ch.get('title', '')}》共{ch.get('word_count', 0)}字",
            "data": ch, "switch_panel": "preview"}


def _t_search_chapters(pid, args):
    keyword = args.get("keyword", "")
    chapters = storage.load_data(pid, "chapters.json") or []
    results = search_chapters(keyword, chapters)
    return {"summary": f"搜索'{keyword}'找到 {len(results)} 条匹配", "data": results}


def _t_start_chapter(pid, args):
    chapter_num = deduction.start_chapter(pid)
    return {"summary": f"第{chapter_num}章已开始推演，请查看右侧聊天室",
            "data": {"chapter": chapter_num}, "switch_panel": "chatroom"}


def _t_stop_chapter(pid, args):
    deduction.request_stop(pid)
    return {"summary": "已喊停，将在轮次边界停止", "data": {"stop_requested": True}}


def _t_get_chapter_state(pid, args):
    state = deduction.get_active_state(pid)
    if not state:
        return {"summary": "无活动章节", "data": {"active": False}}
    return {"summary": f"第...章 round {state.get('round', 0)} 字数 {state.get('word_count', 0)}",
            "data": {"active": True, **state}, "switch_panel": "chatroom"}


def _t_validate_chapter(pid, args):
    result = finalization.validate_chapter(pid)
    issues = result.get("issues") or result.get("conflicts") or []
    return {"summary": f"校验完成，发现 {len(issues)} 处问题",
            "data": result, "switch_panel": "finalization"}


def _t_apply_feedback(pid, args):
    feedback = args.get("feedback", [])
    result = finalization.apply_user_feedback(pid, feedback)
    gm_calls = []
    edit_log = result.get("edit_log") or []
    for i, item in enumerate(feedback):
        if item.get("action") == "fix" and i < len(edit_log):
            gm_calls.append({
                "instruction": item.get("instruction", {}),
                "operations": edit_log[i].get("operations") or [],
            })
    new_text = result.get("new_text", "")
    summary = f"已应用反馈修改：{len(edit_log)} 处操作；新文本前 80 字：{new_text[:80]}"
    return {"summary": summary, "data": result, "switch_panel": "finalization",
            "gm_calls": gm_calls}


def _t_finalize_chapter(pid, args):
    if deduction.is_streaming(pid):
        return {"error": "推演进行中，不能定稿（请先喊停推演）"}
    if not finalization.get_workspace(pid):
        return {"error": "无活动工作区。请先调 start_chapter 启动推演并产出内容后再定稿。"}
    result = finalization.finalize_chapter(pid)
    chapter_num = result.get("chapter", "?")
    return {"summary": f"第{chapter_num}章已定稿", "data": result, "switch_panel": "preview"}


def _t_discard_chapter(pid, args):
    if deduction.is_streaming(pid):
        return {"error": "推演进行中，不能丢弃本章（请先喊停推演）"}
    if not deduction.get_active_state(pid):
        return {"error": "当前无活动章节可丢弃"}
    result = finalization.discard_chapter(pid)
    return {"summary": "已丢弃当前章节", "data": result}


def _t_locate_chapter(pid, args):
    chapter_num = args.get("chapter_num")
    keyword = args.get("keyword")
    result = _get_editing().locate_chapter(pid, chapter_num=chapter_num, keyword=keyword)
    if "chapter" in result and result["chapter"]:
        return {"summary": f"已定位到第{chapter_num}章", "data": result}
    return {"summary": f"找到 {len(result.get('candidates', []))} 个候选章节", "data": result}


def _t_execute_modification(pid, args):
    chapter_num = args.get("chapter_num")
    instruction = args.get("instruction")
    result = _get_editing().execute_modification(pid, chapter_num, instruction)
    storage.save_data(pid, PENDING_MODIFICATION_FILE, {
        "chapter_num": chapter_num,
        "new_text": result.get("new_text"),
        "affected_ranges": result.get("affected_ranges", []),
    })
    gm_calls = [{
        "instruction": instruction or {},
        "operations": result.get("operations") or [],
        "new_text": result.get("new_text", ""),
    }]
    new_text = result.get("new_text", "")
    summary = f"第{chapter_num}章修改已执行：{len(result.get('operations') or [])} 处操作；新文本前 80 字：{new_text[:80]}。请查看右侧对比，确认后提交。"
    return {"summary": summary, "data": result, "switch_panel": "finalization",
            "gm_calls": gm_calls}


def _t_commit_modification(pid, args):
    pending = storage.load_data(pid, PENDING_MODIFICATION_FILE)
    if not pending:
        return {"error": "无待提交的修改"}
    result = _get_editing().commit_modification(pid, pending["chapter_num"], pending["new_text"])
    storage.save_data(pid, PENDING_MODIFICATION_FILE, None)
    return {"summary": f"第{pending['chapter_num']}章修改已提交", "data": result, "switch_panel": "preview"}


def _t_discard_modification(pid, args):
    pending = storage.load_data(pid, PENDING_MODIFICATION_FILE)
    if not pending:
        return {"error": "无待放弃的修改"}
    result = _get_editing().discard_modification(pid, pending["chapter_num"])
    storage.save_data(pid, PENDING_MODIFICATION_FILE, None)
    return {"summary": "已放弃修改", "data": result}


def _t_check_dormant(pid, args):
    result = assistant.check_dormant_characters(pid)
    dormant = result if isinstance(result, list) else result.get("dormant", [])
    return {"summary": f"发现 {len(dormant)} 个休眠角色", "data": result}


def _t_apply_dormant(pid, args):
    names = args.get("names", [])
    result = assistant.apply_dormant(pid, names)
    return {"summary": f"已休眠 {len(names)} 个角色", "data": result}


def _t_apply_awaken(pid, args):
    names = args.get("names", [])
    result = assistant.apply_awaken(pid, names)
    return {"summary": f"已唤醒 {len(names)} 个角色", "data": result}


def _t_detect_claim_conflicts(pid, args):
    validation_result = args.get("validation_result")
    if not validation_result:
        ws = finalization.get_workspace(pid) or {}
        validation_result = ws.get("validation")
    result = assistant.detect_claim_conflicts(validation_result or {})
    conflicts = result if isinstance(result, list) else result.get("conflicts", [])
    return {"summary": f"检测到 {len(conflicts)} 个宣称冲突", "data": result}


def _t_resolve_claim_conflict(pid, args):
    conflict_index = args.get("conflict_index")
    resolution = args.get("resolution")
    details = args.get("details")
    result = assistant.resolve_claim_conflict(pid, conflict_index, resolution, details)
    return {"summary": f"冲突 {conflict_index} 已解决", "data": result}


def _t_check_completion(pid, args):
    chapters = storage.load_data(pid, "chapters.json") or []
    if not chapters:
        return {"summary": "无章节可检测", "data": {"conditions": [], "can_complete": False}}
    last = chapters[-1]
    chapter_summary = last.get("summary", "")
    chapter_num = last.get("chapter")
    result = finalization.check_completion(pid, chapter_summary, chapter_num)
    conditions = result.get("conditions", [])
    achieved = sum(1 for c in conditions if c.get("achieved"))
    return {"summary": f"完本条件 {achieved}/{len(conditions)} 已达成",
            "data": result, "switch_panel": "visualization"}


def _t_generate_finale_summary(pid, args):
    result = exporter.generate_finale_summary(pid)
    return {"summary": "完结总结已生成", "data": result}


def _t_export_novel(pid, args):
    fmt = args.get("format", "txt")
    route = {"txt": "txt", "md": "markdown", "epub": "epub", "zip": "project"}.get(fmt)
    if not route:
        return {"error": f"不支持的格式: {fmt}"}
    if storage.get_project(pid) is None:
        return {"error": "项目不存在"}
    filename = f"{pid}.{fmt}"
    download_url = f"/api/projects/{pid}/export/{route}"
    return {"summary": f"已导出 {fmt} 格式，前端可触发下载", "data": {"format": fmt, "filename": filename, "download_url": download_url}}


def _t_get_relationships(pid, args):
    data = storage.load_data(pid, "relationships.json") or []
    return {"summary": f"角色关系：{len(data)} 条", "data": data, "switch_panel": "visualization"}


def _t_get_timeline(pid, args):
    data = storage.load_data(pid, "events.json") or []
    return {"summary": f"事件时间轴：{len(data)} 条", "data": data, "switch_panel": "visualization"}


def _t_get_word_stats(pid, args):
    chapters = storage.load_data(pid, "chapters.json") or []
    data = [{"chapter": c.get("chapter"), "word_count": c.get("word_count", 0)} for c in chapters]
    return {"summary": f"字数统计：{len(data)} 章", "data": data, "switch_panel": "visualization"}


def _t_get_foreshadows(pid, args):
    data = storage.load_data(pid, "foreshadows.json") or []
    return {"summary": f"伏笔：{len(data)} 条", "data": data, "switch_panel": "visualization"}


def _t_set_multithread(pid, args):
    enabled = args.get("enabled", False)
    meta = deduction.set_multithread(pid, enabled)
    return {"summary": f"已切换至{'多' if enabled else '单'}线程模式",
            "data": {"multithread": meta["multithread"], "active_scenes": deduction.list_active_scenes(pid)}}


def _t_list_active_scenes(pid, args):
    active = deduction.list_active_scenes(pid)
    paused = deduction.list_paused_scenes(pid)
    return {"summary": f"活动场景 {len(active)}，暂停 {len(paused)}",
            "data": {"active": active, "paused": paused}}


def _t_pause_scene(pid, args):
    scene_name = args.get("scene_name")
    ws = deduction.pause_scene(pid, scene_name)
    if not ws:
        return {"error": "暂停失败（无活动章节或场景不存在）"}
    return {"summary": f"场景 {scene_name} 已暂停", "data": {"paused": scene_name}}


def _t_resume_scene(pid, args):
    scene_name = args.get("scene_name")
    ws = deduction.resume_scene(pid, scene_name)
    if not ws:
        return {"error": "恢复失败（无工作区或场景不存在）"}
    return {"summary": f"场景 {scene_name} 已恢复", "data": {"resumed": scene_name}}


def _t_analyze_requirement(pid, args):
    user_input = args.get("user_input", "")
    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        return {"error": "未配置 dialogue agent 的 LLM"}
    result = analyze_requirement(user_input, cfg)
    return {"summary": f"需求分析完成：题材={result.get('genre', '')}", "data": result}


def _t_generate_setting_package(pid, args):
    user_input = args.get("user_input", "")
    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        return {"error": "未配置 dialogue agent 的 LLM"}
    result = generate_setting_package(user_input, cfg)
    storage.save_data(pid, PENDING_SETTING_FILE, result)
    return {"summary": "设定包已生成，请确认后提交", "data": result}


def _t_commit_project_init(pid, args):
    pending = storage.load_data(pid, PENDING_SETTING_FILE)
    if not pending:
        return {"error": "无待确认的设定包"}
    # 写入项目数据
    storage.save_data(pid, "world_rules.json", pending.get("world_rules", {}))
    characters = []
    focus_names = []
    for c in pending.get("characters", []):
        c = dict(c)
        if not c.get("id"):
            c["id"] = _uuid.uuid4().hex
        c.setdefault("activation_state", "active")
        c.setdefault("memory_anchors", [])
        if c.get("is_focus"):
            focus_names.append(c.get("name", ""))
        characters.append(c)
    storage.save_data(pid, "characters.json", characters)
    initial_scene = pending.get("initial_scene", {"name": "", "description": ""})
    storage.save_data(pid, "scenes.json", [initial_scene])
    storage.save_data(pid, "completion_conditions.json", {"conditions": pending.get("completion_conditions", [])})
    # 更新 meta
    meta = storage.get_project(pid) or {}
    meta["current_scene"] = initial_scene.get("name", "")
    meta["focus_characters"] = [n for n in focus_names if n]
    storage.update_project(pid, meta)
    # 保存初始快照
    storage.save_snapshot(pid, "v0")
    # 清除 pending
    storage.save_data(pid, PENDING_SETTING_FILE, None)
    return {"summary": "项目初始化完成，可以开始推演了", "data": {"initialized": True, "characters_count": len(characters)}}


def _t_update_project_meta(pid, args):
    title = (args.get("title") or "").strip()
    if not title:
        return {"error": "需要 title 字段"}
    meta = storage.get_project(pid) or {}
    old_title = meta.get("title", "")
    meta["title"] = title
    storage.update_project(pid, meta)
    return {"summary": f"小说标题已修改：{old_title} → {title}",
            "data": {"old_title": old_title, "new_title": title}}


def _t_delete_character(pid, args):
    names = args.get("names") or []
    if not isinstance(names, list) or not names:
        return {"error": "需要 names 列表"}
    result = assistant.delete_character(pid, names)
    deleted = result.get("deleted", [])
    rejected = result.get("rejected_focus", [])
    if not deleted:
        return {"summary": "未删除任何角色（焦点角色拒绝删除或未找到）", "data": result}
    summary = f"已删除角色：{', '.join(deleted)}"
    if rejected:
        summary += f"；焦点角色拒绝删除：{', '.join(rejected)}"
    return {"summary": summary, "data": result, "switch_panel": "visualization"}


# 工具名 → 处理函数映射
_TOOL_HANDLERS = {
    "get_project_info": _t_get_project_info,
    "list_chapters": _t_list_chapters,
    "get_chapter": _t_get_chapter,
    "search_chapters": _t_search_chapters,
    "start_chapter": _t_start_chapter,
    "stop_chapter": _t_stop_chapter,
    "get_chapter_state": _t_get_chapter_state,
    "validate_chapter": _t_validate_chapter,
    "apply_feedback": _t_apply_feedback,
    "finalize_chapter": _t_finalize_chapter,
    "discard_chapter": _t_discard_chapter,
    "locate_chapter": _t_locate_chapter,
    "execute_modification": _t_execute_modification,
    "commit_modification": _t_commit_modification,
    "discard_modification": _t_discard_modification,
    "check_dormant_characters": _t_check_dormant,
    "apply_dormant": _t_apply_dormant,
    "apply_awaken": _t_apply_awaken,
    "detect_claim_conflicts": _t_detect_claim_conflicts,
    "resolve_claim_conflict": _t_resolve_claim_conflict,
    "check_completion": _t_check_completion,
    "generate_finale_summary": _t_generate_finale_summary,
    "export_novel": _t_export_novel,
    "get_relationships": _t_get_relationships,
    "get_timeline": _t_get_timeline,
    "get_word_stats": _t_get_word_stats,
    "get_foreshadows": _t_get_foreshadows,
    "set_multithread": _t_set_multithread,
    "list_active_scenes": _t_list_active_scenes,
    "pause_scene": _t_pause_scene,
    "resume_scene": _t_resume_scene,
    "analyze_requirement": _t_analyze_requirement,
    "generate_setting_package": _t_generate_setting_package,
    "commit_project_init": _t_commit_project_init,
    "update_project_meta": _t_update_project_meta,
    "delete_character": _t_delete_character,
}


# ---------- 阶段 5 修改流程：指令包生成 ----------

BUILD_EDIT_INSTRUCTION_PROMPT = """你是小说修改指令设计师。用户描述了修改意图，请压缩为结构化修改指令。
返回 JSON（只返回 JSON）：
{
  "target": "操作目标（如：张三的某句台词）",
  "position": "精准位置（段落号或原文片段描述）",
  "operation": "replace|insert|delete|reorder",
  "content": "操作内容（替换为新文本/插入文本/删除留空/调序目标位置）",
  "scope": "影响范围（如：仅本段/本章后续/跨章）"
}
只返回 JSON。"""


def build_edit_instruction(user_intent, llm_config):
    """用户意图 → 结构化修改指令包。
    user_intent 结构：{chapter, direction, scope, example}。
    返回 {target, position, operation, content, scope}。"""
    user_content = json.dumps(user_intent, ensure_ascii=False, indent=2)
    messages = [
        {"role": "system", "content": BUILD_EDIT_INSTRUCTION_PROMPT},
        {"role": "user", "content": user_content},
    ]
    raw = llm.chat(llm_config, messages)
    return _parse_json(raw)