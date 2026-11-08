import os
import uuid
import winreg
from datetime import datetime
from functools import wraps
from urllib.parse import quote

from flask import Flask, Response, jsonify, request, send_from_directory

import storage
import llm
import agents
import dialogue_agent
import deduction
import finalization
import exporter
import editing
import assistant

FRONTEND_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "frontend")

app = Flask(__name__, static_folder=None)


def require_project(f):
    """装饰器：检查 pid 对应的项目是否存在，不存在返回 404。"""
    @wraps(f)
    def wrapper(pid, *args, **kwargs):
        if storage.get_project(pid) is None:
            return jsonify({"error": "project not found"}), 404
        return f(pid, *args, **kwargs)
    return wrapper


@app.route("/")
def index():
    return send_from_directory(FRONTEND_DIR, "index.html")


@app.route("/static/<path:filename>")
def static_files(filename):
    return send_from_directory(FRONTEND_DIR, filename)


@app.route("/api/projects", methods=["GET"])
def api_list_projects():
    return jsonify(storage.list_projects())


@app.route("/api/projects", methods=["POST"])
def api_create_project():
    data = request.get_json(force=True, silent=True) or {}
    title = (data.get("title") or "").strip()
    genre = (data.get("genre") or "").strip()
    if not title:
        return jsonify({"error": "title is required"}), 400
    meta = storage.create_project(title, genre)
    return jsonify(meta), 201


@app.route("/api/projects/import", methods=["POST"])
def api_import_project():
    """导入工程 zip。multipart file 字段。保留原 pid，冲突报错。返回 meta。"""
    if "file" not in request.files:
        return jsonify({"error": "缺少文件"}), 400
    f = request.files["file"]
    try:
        meta = exporter.import_project_zip(f.read())
        return jsonify(meta)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        return jsonify({"error": f"导入失败: {e}"}), 500


@app.route("/api/projects/<pid>", methods=["GET"])
def api_get_project(pid):
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(meta)


@app.route("/api/projects/<pid>", methods=["PUT"])
def api_update_project(pid):
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    # H3 修复：白名单更新，防止客户端覆盖 created_at / current_chapter / id 等系统字段
    # 允许字段：用户可改的小说设定类字段；系统字段（id/created_at/current_chapter/can_complete*）
    # 由专属接口修改（start_chapter/finalize_book 等）
    ALLOWED_META_FIELDS = {
        "title", "genre", "focus_characters", "multithread",
        "word_count_target", "foreshadows_enabled", "current_scene",
    }
    for k, v in data.items():
        if k in ALLOWED_META_FIELDS:
            meta[k] = v
    return jsonify(storage.update_project(pid, meta))


@app.route("/api/projects/<pid>", methods=["DELETE"])
def api_delete_project(pid):
    ok = storage.delete_project(pid)
    if not ok:
        return jsonify({"error": "not found"}), 404
    return jsonify({"deleted": pid})


@app.route("/api/projects/<pid>/snapshots", methods=["GET"])
@require_project
def api_list_snapshots(pid):
    return jsonify(storage.list_snapshots(pid))


@app.route("/api/projects/<pid>/snapshots", methods=["POST"])
@require_project
def api_save_snapshot(pid):
    data = request.get_json(force=True, silent=True) or {}
    version = (data.get("version") or "").strip()
    if not version:
        return jsonify({"error": "version is required"}), 400
    storage.save_snapshot(pid, version)
    return jsonify({"saved": version}), 201


@app.route("/api/projects/<pid>/snapshots/<ver>/restore", methods=["POST"])
def api_restore_snapshot(pid, ver):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    meta = storage.restore_snapshot(pid, ver)
    if meta is None:
        return jsonify({"error": "snapshot not found"}), 404
    return jsonify(meta)


@app.route("/api/machine_id", methods=["GET"])
def api_machine_id():
    """返回机器标识（Windows 用 MachineGuid，其他平台用 uuid.getnode()）。"""
    try:
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Cryptography") as key:
            guid, _ = winreg.QueryValueEx(key, "MachineGuid")
            return jsonify({"machine_id": guid})
    except Exception:
        # 非 Windows 或注册表不可用：fallback 到网卡 MAC 的哈希
        return jsonify({"machine_id": str(uuid.getnode())})


@app.route("/api/llm/config", methods=["GET"])
def api_get_llm_config():
    """返回内存中的配置状态（脱敏：不含 api_key）。供前端检查是否已推送。"""
    cfg = storage.get_in_memory_llm_config()
    providers = [
        {
            "id": p.get("id", ""), "name": p.get("name", ""),
            "provider": p.get("provider", ""), "model": p.get("model", ""),
            "base_url": p.get("base_url", ""),
            "temperature": p.get("temperature", 0.7),
            "max_tokens": p.get("max_tokens", 4096),
            "has_key": bool(p.get("api_key")),
        }
        for p in cfg.get("providers", [])
    ]
    return jsonify({"providers": providers, "bindings": cfg.get("bindings", {})})


@app.route("/api/llm/config", methods=["PUT", "POST"])
def api_save_llm_config():
    """前端推送解密后的完整配置（含 api_key）到内存。"""
    data = request.get_json(force=True, silent=True) or {}
    if not isinstance(data, dict):
        return jsonify({"error": "config must be an object"}), 400
    storage.set_in_memory_llm_config(data.get("providers", []), data.get("bindings", {}))
    return jsonify({"ok": True})


@app.route("/api/llm/test", methods=["POST"])
def api_test_llm():
    data = request.get_json(force=True, silent=True) or {}
    config = data.get("config") or {}
    if not config.get("provider") or not config.get("model") or not config.get("api_key"):
        return jsonify({"ok": False, "error": "缺少 provider/model/api_key"}), 400
    try:
        text = llm.chat(config, [{"role": "user", "content": "你好"}])
        return jsonify({"ok": True, "text": text})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 500


# ---------- R22 自定义提示词：11 个 key（每个 agent 函数独立） ----------

@app.route("/api/prompts/defaults", methods=["GET"])
def api_prompts_defaults():
    """返回 11 个 key 的默认 prompt（代码常量）。供前端"恢复默认"使用。"""
    return jsonify(storage.get_all_default_prompts())


@app.route("/api/prompts/global", methods=["GET"])
def api_prompts_global_get():
    """返回全局提示词（缺失返回 {}）。"""
    return jsonify(storage.load_global_prompts())


@app.route("/api/prompts/global", methods=["PUT", "POST"])
def api_prompts_global_save():
    """合并保存全局提示词（单 key 更新不影响其他 key）。
    body 是 {agent_key: prompt_text}，空字符串删除该 key（回退默认）。"""
    data = request.get_json(force=True, silent=True) or {}
    if not isinstance(data, dict):
        return jsonify({"error": "prompts must be an object"}), 400
    current = storage.load_global_prompts()
    for k, v in data.items():
        if k not in storage.PROMPT_AGENT_KEYS:
            continue
        if isinstance(v, str) and v.strip():
            current[k] = v
        else:
            current.pop(k, None)  # 空字符串 → 删除 key
    storage.save_global_prompts(current)
    return jsonify({"ok": True, "saved": current})


@app.route("/api/projects/<pid>/prompts", methods=["GET"])
def api_project_prompts_get(pid):
    """返回项目提示词（缺失返回 {}）。同时附带 effective（合并全局后实际生效的）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    proj = storage.load_project_prompts(pid)
    glob = storage.load_global_prompts()
    # effective: 项目 > 全局，未覆盖的 key 不出现（调用 load_prompt 时会回退到默认）
    effective = {**glob, **proj}
    return jsonify({
        "project": proj,
        "global": glob,
        "effective": effective,
    })


@app.route("/api/projects/<pid>/prompts", methods=["PUT", "POST"])
def api_project_prompts_save(pid):
    """合并保存项目提示词（单 key 更新不影响其他 key）。
    body 是 {agent_key: prompt_text}，空字符串删除该 key（回退全局或默认）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    if not isinstance(data, dict):
        return jsonify({"error": "prompts must be an object"}), 400
    current = storage.load_project_prompts(pid)
    for k, v in data.items():
        if k not in storage.PROMPT_AGENT_KEYS:
            continue
        if isinstance(v, str) and v.strip():
            current[k] = v
        else:
            current.pop(k, None)  # 空字符串 → 删除 key
    storage.save_project_prompts(pid, current)
    return jsonify({"ok": True, "saved": current})


# ---------- 阶段 0：项目初始化向导 ----------

@app.route("/api/init/analyze", methods=["POST"])
def api_init_analyze():
    data = request.get_json(force=True, silent=True) or {}
    user_input = (data.get("user_input") or "").strip()
    if not user_input:
        return jsonify({"error": "user_input is required"}), 400
    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        return jsonify({"error": "未配置 dialogue agent 的 LLM（请在设置页配置）"}), 400
    try:
        result = dialogue_agent.analyze_requirement(user_input, cfg)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/init/generate", methods=["POST"])
def api_init_generate():
    data = request.get_json(force=True, silent=True) or {}
    user_input = (data.get("user_input") or "").strip()
    if not user_input:
        return jsonify({"error": "user_input is required"}), 400
    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        return jsonify({"error": "未配置 dialogue agent 的 LLM（请在设置页配置）"}), 400
    try:
        result = dialogue_agent.generate_setting_package(user_input, cfg)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/init/confirm", methods=["POST"])
def api_init_confirm():
    data = request.get_json(force=True, silent=True) or {}
    title = (data.get("title") or "").strip()
    genre = (data.get("genre") or "").strip()
    if not title:
        return jsonify({"error": "title is required"}), 400
    world_rules = data.get("world_rules") or {}
    raw_characters = data.get("characters") or []
    initial_scene = data.get("initial_scene") or {"name": "", "description": ""}
    conditions = data.get("completion_conditions") or []
    foreshadows_enabled = bool(data.get("foreshadows_enabled", False))

    # 创建项目（含空数据文件 + meta 默认值）
    meta = storage.create_project(title, genre)
    pid = meta["id"]

    # 写入设定包数据
    storage.save_data(pid, "world_rules.json", world_rules)

    characters = []
    focus_names = []
    for c in raw_characters:
        c = dict(c)
        if not c.get("id"):
            c["id"] = uuid.uuid4().hex
        c.setdefault("activation_state", "active")
        c.setdefault("memory_anchors", [])
        if c.get("is_focus"):
            focus_names.append(c.get("name", ""))
        characters.append(c)
    storage.save_data(pid, "characters.json", characters)

    storage.save_data(pid, "scenes.json", [initial_scene])
    storage.save_data(pid, "completion_conditions.json", {"conditions": conditions})

    meta["current_scene"] = initial_scene.get("name", "")
    meta["focus_characters"] = [n for n in focus_names if n]
    meta["foreshadows_enabled"] = foreshadows_enabled
    storage.update_project(pid, meta)

    # 初始快照 v0
    storage.save_snapshot(pid, "v0")

    return jsonify(meta), 201


# ---------- 阶段 1：推演循环 ----------

@app.route("/api/projects/<pid>/chapter/start", methods=["POST"])
def api_start_chapter(pid):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    # R22: 手动启动前置检查（避免空项目推演导致 bug）
    meta = storage.get_project(pid)
    characters = storage.load_data(pid, "characters.json") or []
    if not characters:
        return jsonify({"error": "无法开始推演：项目还没有任何角色。请先通过对话员完成项目设定。"}), 400
    if not meta.get("current_scene"):
        return jsonify({"error": "无法开始推演：项目还没有设置当前场景。"}), 400
    llm_config = storage.load_llm_config()
    char_cfg = llm_config.get("character") or {}
    if not char_cfg.get("model") or not char_cfg.get("api_key"):
        return jsonify({"error": "无法开始推演：未配置 character agent 的 LLM（请在设置页配置）。"}), 400
    chapter_num = deduction.start_chapter(pid)
    return jsonify({"chapter": chapter_num})


@app.route("/api/projects/<pid>/chapter/stream")
def api_chapter_stream(pid):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    if deduction.get_active_state(pid) is None:
        return jsonify({"error": "no active chapter, call /chapter/start first"}), 400
    # H1 修复：同一项目已在推演时拒绝（防止跨标签页/并发请求双开推演流导致数据损坏）
    if not deduction.start_streaming(pid):
        return jsonify({"error": "该项目已在推演中，请先停止当前推演"}), 409

    def generate():
        try:
            for event in deduction.run_deduction_stream(pid):
                yield event
        except Exception as e:
            # B5 修复：异常时 yield error 事件，避免客户端收到静默截断的流。
            # deduction._emit 格式：data: {json}\n\n
            yield f'data: {json.dumps({"type": "error", "data": {"message": str(e)}})}\n\n'
        finally:
            deduction.stop_streaming(pid)

    return Response(generate(), mimetype="text/event-stream",
                    headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})


@app.route("/api/projects/<pid>/chapter/stop", methods=["POST"])
def api_stop_chapter(pid):
    deduction.request_stop(pid)
    return jsonify({"stop_requested": True})


@app.route("/api/projects/<pid>/chapter/state")
def api_chapter_state(pid):
    state = deduction.get_active_state(pid)
    if state is None:
        return jsonify({"active": False, "streaming": False})
    # R21 修复：streaming 字段表示 SSE 推演流是否在跑（区别于 active=有活动章节）
    # 前端用 streaming 判断"推演中..."状态，避免推演结束后 active=true 导致永久显示"推演中..."
    return jsonify({"active": True, "streaming": deduction.is_streaming(pid), **state})


# ---------- 阶段 1 扩展：Task 11 多线程模式（场景管理） ----------

@app.route("/api/projects/<pid>/multithread", methods=["PUT"])
def api_set_multithread(pid):
    """切换多线程模式（即时生效，不重置已保存数据）。body: {enabled: bool}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    if "enabled" not in data:
        return jsonify({"error": "需要 enabled 字段"}), 400
    meta = deduction.set_multithread(pid, bool(data["enabled"]))
    return jsonify({"multithread": meta["multithread"], "active_scenes": deduction.list_active_scenes(pid)})


@app.route("/api/projects/<pid>/scenes/active")
def api_active_scenes(pid):
    """获取活动场景列表（多线程模式）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify({
        "active_scenes": deduction.list_active_scenes(pid),
        "paused_scenes": deduction.list_paused_scenes(pid),
        "state": deduction.get_active_state(pid),
    })


@app.route("/api/projects/<pid>/scenes/paused")
def api_paused_scenes(pid):
    """获取已暂停场景列表。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify({"paused_scenes": deduction.list_paused_scenes(pid)})


@app.route("/api/projects/<pid>/scenes/pause", methods=["POST"])
def api_pause_scene(pid):
    """暂停指定场景（单线程模式）。body: {scene_name}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    scene_name = (data.get("scene_name") or "").strip()
    if not scene_name:
        return jsonify({"error": "需要 scene_name"}), 400
    ws = deduction.pause_scene(pid, scene_name)
    if ws is None:
        return jsonify({"error": "无活动章节或暂停失败"}), 400
    return jsonify({"paused": scene_name, "messages_count": len(ws.get("chapter_messages", []))})


@app.route("/api/projects/<pid>/scenes/resume", methods=["POST"])
def api_resume_scene(pid):
    """恢复指定场景（单线程模式）。body: {scene_name}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    scene_name = (data.get("scene_name") or "").strip()
    if not scene_name:
        return jsonify({"error": "需要 scene_name"}), 400
    ws = deduction.resume_scene(pid, scene_name)
    if ws is None:
        return jsonify({"error": "无活动章节或无该场景工作区"}), 400
    return jsonify({"resumed": scene_name, "messages_count": len(ws.get("chapter_messages", []))})


# ---------- 阶段 2：定稿流程 ----------

@app.route("/api/projects/<pid>/chapter/workspace")
def api_chapter_workspace(pid):
    """获取临时工作区状态（章节号/字数/轮次/新角色队列/推演消息）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    ws = finalization.get_workspace(pid)
    if not ws:
        return jsonify({"active": False})
    return jsonify({
        "active": True,
        "chapter_num": ws.get("chapter_num"),
        "round": ws.get("round", 0),
        "word_count": ws.get("word_count", 0),
        "new_characters_queue": ws.get("new_characters_queue") or [],
        "has_validation": bool(ws.get("validation")),
        "has_override": bool(ws.get("chapter_text_override")),
        # R22: 返回 chapter_messages 供前端刷新后回显推演历史（speaker/text/round/scene）
        "chapter_messages": ws.get("chapter_messages") or [],
        "current_scene": ws.get("current_scene", ""),
    })


@app.route("/api/projects/<pid>/chapter/validate", methods=["POST"])
def api_chapter_validate(pid):
    """首次因果校验：检察员校验模式，返回 JSON。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    try:
        result = finalization.validate_chapter(pid)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/chapter/apply-feedback", methods=["POST"])
def api_chapter_apply_feedback(pid):
    """用户反馈处理：fix 调 execute_edit 修改，ignore 标记忽略。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    feedback = request.get_json(force=True, silent=True)
    if not isinstance(feedback, list):
        return jsonify({"error": "feedback must be a list"}), 400
    try:
        result = finalization.apply_user_feedback(pid, feedback)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/chapter/revalidate", methods=["POST"])
def api_chapter_revalidate(pid):
    """二次校验：仅校验受影响波及段落。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    new_text = data.get("new_text")
    affected_ranges = data.get("affected_ranges") or []
    if not new_text:
        return jsonify({"error": "new_text is required"}), 400
    try:
        result = finalization.revalidate(pid, new_text, affected_ranges)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/chapter/finalize", methods=["POST"])
def api_chapter_finalize(pid):
    """定稿：数据固化（不可回退）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    try:
        result = finalization.finalize_chapter(pid)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/chapter/discard", methods=["POST"])
def api_chapter_discard(pid):
    """放弃本章：删除工作区，回退 current_chapter。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    try:
        result = finalization.discard_chapter(pid)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/chapter/confirm-characters", methods=["POST"])
def api_chapter_confirm_characters(pid):
    """新角色确认：为接受的新角色创建角色卡。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    confirmed = data.get("confirmed") or []
    if not isinstance(confirmed, list):
        return jsonify({"error": "confirmed must be a list"}), 400
    try:
        result = finalization.confirm_new_characters(pid, confirmed)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/characters/delete", methods=["POST"])
def api_delete_characters(pid):
    """删除角色（前端角色卡 GUI 调用）。body: {names: [str]}。焦点角色拒绝删除。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    names = data.get("names") or []
    if not isinstance(names, list) or not names:
        return jsonify({"error": "需要 names 列表"}), 400
    try:
        result = assistant.delete_character(pid, names)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ---------- 阶段 3：完本条件检测 ----------

@app.route("/api/projects/<pid>/completion-status")
def api_completion_status(pid):
    """完本检测状态（只读）：条件列表 + 全达成标记 + meta.can_complete。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    cc_data = storage.load_data(pid, "completion_conditions.json") or {}
    conditions = cc_data.get("conditions") if isinstance(cc_data, dict) else []
    if conditions is None:
        conditions = []
    achieved_count = sum(1 for c in conditions if c.get("achieved"))
    total = len(conditions)
    all_achieved = bool(conditions) and achieved_count == total
    return jsonify({
        "conditions": conditions,
        "achieved_count": achieved_count,
        "total": total,
        "all_achieved": all_achieved,
        "can_complete": bool(meta.get("can_complete", False)),
        "can_complete_chapter": meta.get("can_complete_chapter"),
    })


@app.route("/api/projects/<pid>/completion/check", methods=["POST"])
def api_completion_check(pid):
    """手动触发完本检测（调试或重检）。body: {"chapter_summary": "..."}。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_summary = data.get("chapter_summary") or ""
    try:
        result = finalization.check_completion(pid, chapter_summary)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ---------- 阶段 4：完本与导出 ----------

def _attachment_filename(pid, ext):
    """生成 Content-Disposition 文件名（清理非法字符）。
    RFC 6266：filename ASCII fallback + filename* UTF-8 编码形式。"""
    meta = storage.get_project(pid) or {}
    safe = exporter._sanitize_filename(meta.get("title", "novel"))
    ascii_fallback = safe.encode("ascii", "ignore").decode("ascii") or "novel"
    encoded = quote(f"{safe}.{ext}")
    return f"attachment; filename=\"{ascii_fallback}.{ext}\"; filename*=UTF-8''{encoded}"


@app.route("/api/projects/<pid>/finalize-book", methods=["POST"])
def api_finalize_book(pid):
    """完本确认：生成完结总结，标记 is_completed，写入 meta.finale_summary。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    try:
        summary = exporter.generate_finale_summary(pid)
        # B11 修复：summary 为空（LLM 失败被内部 catch）时不标记完成，
        # 否则用户看到「已完本」但无总结，且无法重新生成。
        if not summary:
            return jsonify({"error": "完结总结生成失败（LLM 未配置或返回为空），请检查配置后重试"}), 500
        meta["finale_summary"] = summary
        meta["is_completed"] = True
        meta["completed_at"] = datetime.now().isoformat()
        storage.update_project(pid, meta)
        return jsonify({"summary": summary, "meta": meta})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/finale-summary")
def api_finale_summary(pid):
    """获取已生成的完结总结（meta.finale_summary）。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    return jsonify({"summary": meta.get("finale_summary"), "is_completed": bool(meta.get("is_completed"))})


@app.route("/api/projects/<pid>/export/txt")
def api_export_txt(pid):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    body = exporter.export_txt(pid)
    return Response(body, mimetype="text/plain; charset=utf-8",
                    headers={"Content-Disposition": _attachment_filename(pid, "txt")})


@app.route("/api/projects/<pid>/export/markdown")
def api_export_markdown(pid):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    body = exporter.export_markdown(pid)
    return Response(body, mimetype="text/markdown; charset=utf-8",
                    headers={"Content-Disposition": _attachment_filename(pid, "md")})


@app.route("/api/projects/<pid>/export/epub")
def api_export_epub(pid):
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    body = exporter.export_epub(pid)
    return Response(body, mimetype="application/epub+zip",
                    headers={"Content-Disposition": _attachment_filename(pid, "epub")})


@app.route("/api/projects/<pid>/export/project")
def api_export_project(pid):
    """打包工程文件 zip。查询参数 include_snapshots / include_logs / include_workspace（默认 true/true/false）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    args = request.args
    inc_snapshots = args.get("include_snapshots", "true").lower() != "false"
    inc_logs = args.get("include_logs", "true").lower() != "false"
    inc_workspace = args.get("include_workspace", "false").lower() == "true"
    body = exporter.export_project_zip(pid, inc_snapshots, inc_logs, inc_workspace)
    return Response(body, mimetype="application/zip",
                    headers={"Content-Disposition": _attachment_filename(pid, "zip")})


# ---------- 阶段 5：用户随时发起修改 ----------

@app.route("/api/projects/<pid>/chapters", methods=["GET"])
def api_list_chapters(pid):
    """章节库视图：列出所有章节（含正文）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(storage.load_data(pid, "chapters.json") or [])


@app.route("/api/projects/<pid>/edit/locate", methods=["POST"])
def api_edit_locate(pid):
    """定位目标章节：章号直接定位 / 全库关键词搜索返回候选。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    keyword = data.get("keyword")
    if chapter_num is None and not keyword:
        return jsonify({"error": "需要 chapter_num 或 keyword"}), 400
    try:
        return jsonify(editing.locate_chapter(pid, chapter_num=chapter_num, keyword=keyword))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/edit/instruction", methods=["POST"])
def api_edit_instruction(pid):
    """生成修改指令包（对话AI）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    direction = (data.get("direction") or "").strip()
    scope = data.get("scope") or ""
    example = data.get("example") or ""
    if not direction:
        return jsonify({"error": "direction is required"}), 400
    cfg = storage.load_llm_config().get("dialogue") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        return jsonify({"error": "未配置 dialogue agent 的 LLM"}), 500
    try:
        user_intent = {"chapter": chapter_num, "direction": direction, "scope": scope, "example": example}
        return jsonify(dialogue_agent.build_edit_instruction(user_intent, cfg))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/edit/execute", methods=["POST"])
def api_edit_execute(pid):
    """群主AI 执行修改。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    instruction = data.get("instruction") or {}
    if chapter_num is None or not instruction:
        return jsonify({"error": "需要 chapter_num 和 instruction"}), 400
    try:
        return jsonify(editing.execute_modification(pid, chapter_num, instruction))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/edit/revalidate", methods=["POST"])
def api_edit_revalidate(pid):
    """二次校验：仅波及段落。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    new_text = data.get("new_text")
    affected_ranges = data.get("affected_ranges") or []
    if chapter_num is None or not new_text:
        return jsonify({"error": "需要 chapter_num 和 new_text"}), 400
    try:
        return jsonify(editing.revalidate_modification(pid, chapter_num, new_text, affected_ranges))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/edit/commit", methods=["POST"])
def api_edit_commit(pid):
    """提交修改：更新 chapters.json + 增量快照 + 日志。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    new_text = data.get("new_text")
    if chapter_num is None or not new_text:
        return jsonify({"error": "需要 chapter_num 和 new_text"}), 400
    try:
        return jsonify(editing.commit_modification(pid, chapter_num, new_text))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/edit/discard", methods=["POST"])
def api_edit_discard(pid):
    """放弃修改：chapters.json 未被修改。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    chapter_num = data.get("chapter_num")
    if chapter_num is None:
        return jsonify({"error": "需要 chapter_num"}), 400
    return jsonify(editing.discard_modification(pid, chapter_num))


@app.route("/api/projects/<pid>/edit/log", methods=["GET"])
def api_edit_log(pid):
    """修改日志。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(editing.get_edit_log(pid))


# ---------- 阶段 6：系统辅助 ----------

@app.route("/api/projects/<pid>/assistant/dormant", methods=["GET"])
def api_assistant_dormant_get(pid):
    """检测可休眠角色（连续 20 章未出场且未 @）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(assistant.check_dormant_characters(pid))


@app.route("/api/projects/<pid>/assistant/dormant", methods=["POST"])
def api_assistant_dormant_post(pid):
    """标记角色休眠。body: {names: [...]}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    names = data.get("names") or []
    if not isinstance(names, list):
        return jsonify({"error": "names must be a list"}), 400
    return jsonify(assistant.apply_dormant(pid, names))


@app.route("/api/projects/<pid>/assistant/awaken", methods=["POST"])
def api_assistant_awaken(pid):
    """唤醒角色。body: {names: [...]}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    names = data.get("names") or []
    if not isinstance(names, list):
        return jsonify({"error": "names must be a list"}), 400
    return jsonify(assistant.apply_awaken(pid, names))


@app.route("/api/projects/<pid>/assistant/crash-recovery", methods=["GET"])
def api_assistant_crash_recovery(pid):
    """检测崩溃恢复（是否存在未完成的工作区）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(assistant.detect_crash_recovery(pid))


@app.route("/api/projects/<pid>/assistant/crash-recovery/clear", methods=["POST"])
def api_assistant_crash_recovery_clear(pid):
    """清除崩溃恢复标记（放弃未完成操作，删除工作区）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(assistant.clear_crash_recovery(pid))


@app.route("/api/projects/<pid>/assistant/key-events-check", methods=["GET"])
def api_assistant_key_events_check(pid):
    """无关键事件检测（读工作区统计 key_events 数）。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(assistant.check_no_key_events(pid))


@app.route("/api/projects/<pid>/assistant/claim-resolve", methods=["POST"])
def api_assistant_claim_resolve(pid):
    """宣称冲突解决。body: {conflict_index, resolution, details?}"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    conflict_index = data.get("conflict_index")
    resolution = data.get("resolution")
    details = data.get("details")
    if conflict_index is None or not resolution:
        return jsonify({"error": "需要 conflict_index 和 resolution"}), 400
    try:
        return jsonify(assistant.resolve_claim_conflict(pid, int(conflict_index), resolution, details))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/projects/<pid>/assistant/ignored-conflicts", methods=["GET"])
def api_assistant_ignored_conflicts(pid):
    """历史忽略记录。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify({"ignored": assistant.get_ignored_conflicts(pid)})


# ---------- Task 12：可视化统一数据接口 ----------

@app.route("/api/projects/<pid>/viz/data")
def api_viz_data(pid):
    """可视化所需全部数据（一次拉取，减少前端多次请求）。
    返回 {characters, relationships, chapters, events, foreshadows, scenes, world_rules}。"""
    if storage.get_project(pid) is None:
        return jsonify({"error": "not found"}), 404
    return jsonify({
        "characters": storage.load_data(pid, "characters.json") or [],
        "relationships": storage.load_data(pid, "relationships.json") or [],
        "chapters": storage.load_data(pid, "chapters.json") or [],
        "events": storage.load_data(pid, "events.json") or [],
        "foreshadows": storage.load_data(pid, "foreshadows.json") or [],
        "scenes": storage.load_data(pid, "scenes.json") or [],
        "world_rules": storage.load_data(pid, "world_rules.json") or {},
    })


# ---------- Task 13：用户权限与收尾 ----------


@app.route("/api/projects/<pid>/change-genre", methods=["POST"])
def api_change_genre(pid):
    """中途换题材：保留 characters.json + meta.focus_characters，重置其他所有库。
    自动保存备份快照 v{N}_pre_genre_change_{ts}。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    new_genre = (data.get("new_genre") or "").strip()
    if not new_genre:
        return jsonify({"error": "new_genre is required"}), 400

    # 1. 备份当前状态
    snapshots = storage.list_snapshots(pid) or []
    n = len(snapshots) + 1
    storage.save_snapshot(pid, f"v{n}_pre_genre_change_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

    # 2. 重置除 characters.json 外的所有数据文件
    storage.reset_data_files(pid, exclude=["characters.json"])

    # M2 修复：characters.json 保留但场景已清空，需清空角色的 current_location，
    # 否则下次推演 _build_candidates 用旧位置找不到匹配的场景。
    characters = storage.load_data(pid, "characters.json") or []
    for c in characters:
        if "current_location" in c:
            c["current_location"] = ""
    storage.save_data(pid, "characters.json", characters)

    # 3. 更新 meta：题材 + 重置推演进度
    meta["genre"] = new_genre
    meta["current_chapter"] = 0
    meta["current_scene"] = ""
    meta.pop("can_complete", None)
    meta.pop("can_complete_chapter", None)
    return jsonify(storage.update_project(pid, meta))


@app.route("/api/projects/<pid>/rollback", methods=["POST"])
def api_rollback(pid):
    """推倒重写：先保存当前状态为快照 v{N}_pre_rollback_{ts}，再覆盖到指定版本。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    version = (data.get("version") or "").strip()
    if not version:
        return jsonify({"error": "version is required"}), 400
    snapshots = storage.list_snapshots(pid) or []
    if version not in snapshots:
        return jsonify({"error": f"snapshot {version} not found"}), 404

    # 1. 先备份当前状态（旧档保留）
    n = len(snapshots) + 1
    storage.save_snapshot(pid, f"v{n}_pre_rollback_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

    # 2. 覆盖到目标版本
    restored = storage.restore_snapshot(pid, version)
    if restored is None:
        return jsonify({"error": "restore failed"}), 500
    return jsonify(restored)


@app.route("/api/projects/<pid>/chapters/<int:num>", methods=["DELETE"])
def api_delete_chapter(pid, num):
    """删除指定章节：从 chapters.json 移除，保存备份快照。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    chapters = storage.load_data(pid, "chapters.json") or []
    new_chapters = [c for c in chapters if c.get("chapter") != num]
    if len(new_chapters) == len(chapters):
        return jsonify({"error": f"chapter {num} not found"}), 404

    # 备份后保存
    snapshots = storage.list_snapshots(pid) or []
    n = len(snapshots) + 1
    storage.save_snapshot(pid, f"v{n}_pre_delete_chapter_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
    storage.save_data(pid, "chapters.json", new_chapters)
    return jsonify({"deleted": num, "remaining": len(new_chapters)})


@app.route("/api/projects/<pid>/word-count-target", methods=["PUT"])
def api_update_word_count_target(pid):
    """更新章节字数目标。body: {min, max}（max 可选，缺省时 = min）。"""
    meta = storage.get_project(pid)
    if meta is None:
        return jsonify({"error": "not found"}), 404
    data = request.get_json(force=True, silent=True) or {}
    if "min" not in data:
        return jsonify({"error": "min is required"}), 400
    try:
        mn = int(data["min"])
        mx = int(data["max"]) if "max" in data and data["max"] is not None else mn
    except (TypeError, ValueError):
        return jsonify({"error": "min/max must be integers"}), 400
    if mn < 0 or mx < mn:
        return jsonify({"error": "min/max 非法（要求 min>=0 且 max>=min）"}), 400
    target = meta.get("word_count_target") or {}
    target["min"] = mn
    target["max"] = mx
    meta["word_count_target"] = target
    return jsonify(storage.update_project(pid, meta))


# ===================== 对话员路由（Task R4）=====================
# 对话员作为主页统一入口，前端通过 SSE 收流式回复 + 工具调用简报。

@app.route("/api/dialogue/chat", methods=["POST"])
def api_dialogue_chat():
    """SSE 流式对话。body: {pid, message}。"""
    data = request.get_json(force=True, silent=True) or {}
    pid = data.get("pid")
    message = data.get("message", "")
    if not pid or not isinstance(pid, str):
        return jsonify({"error": "pid is required"}), 400
    if not message or not isinstance(message, str):
        return jsonify({"error": "message is required"}), 400
    if storage.get_project(pid) is None:
        return jsonify({"error": "project not found"}), 404

    def generate():
        try:
            for event in dialogue_agent.chat_with_tools_stream(pid, message):
                yield event
        except Exception as e:
            # B5 修复：对话流异常时 yield error 事件，避免客户端收到静默截断的流。
            yield f'data: {json.dumps({"type": "error", "data": {"message": str(e)}})}\n\n'

    return Response(generate(), mimetype="text/event-stream",
                    headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})


@app.route("/api/dialogue/proactive", methods=["POST"])
def api_dialogue_proactive():
    """R14: 对话员主动开口。body: {pid}。新建项目 / 进入历史空项目时调用。"""
    data = request.get_json(force=True, silent=True) or {}
    pid = data.get("pid")
    if not pid or not isinstance(pid, str):
        return jsonify({"error": "pid is required"}), 400
    if storage.get_project(pid) is None:
        return jsonify({"error": "project not found"}), 404

    def generate():
        for event in dialogue_agent.proactive_chat_stream(pid):
            yield event

    return Response(generate(), mimetype="text/event-stream",
                    headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})


@app.route("/api/dialogue/history", methods=["GET"])
def api_dialogue_history():
    """返回对话历史。?pid=<pid>。"""
    pid = request.args.get("pid", "")
    if not pid:
        return jsonify({"error": "pid is required"}), 400
    if storage.get_project(pid) is None:
        return jsonify({"error": "project not found"}), 404
    return jsonify({"history": storage.load_dialogue_history(pid)})


@app.route("/api/dialogue/clear", methods=["POST"])
def api_dialogue_clear():
    """清空对话历史。body: {pid}。"""
    data = request.get_json(force=True, silent=True) or {}
    pid = data.get("pid")
    if not pid:
        return jsonify({"error": "pid is required"}), 400
    if storage.get_project(pid) is None:
        return jsonify({"error": "project not found"}), 404
    dialogue_agent.clear_history(pid)
    return jsonify({"cleared": True})


@app.route("/api/dialogue/context", methods=["POST"])
def api_dialogue_context():
    """注入系统消息（章节库修改入口跳转用）。body: {pid, message}。"""
    data = request.get_json(force=True, silent=True) or {}
    pid = data.get("pid")
    message = data.get("message", "")
    if not pid:
        return jsonify({"error": "pid is required"}), 400
    if not message:
        return jsonify({"error": "message is required"}), 400
    if storage.get_project(pid) is None:
        return jsonify({"error": "project not found"}), 404
    dialogue_agent.inject_context(pid, message)
    return jsonify({"injected": True})


# ---------- 开发者模式 ----------
_DEV_AGENTS = ("dialogue", "character", "gm", "inspector", "narrator")


@app.route("/api/dev/run", methods=["POST"])
def api_dev_run():
    """开发者模式：基于真实项目上下文的 AI 调试。

    3 种模式：
    - mode="dialogue": 加载项目上下文+对话历史，发消息，返回完整响应
    - mode="character": 加载推演状态，构建候选名单，调用一轮角色，返回上下文包+响应
    - mode="raw": 原始 prompt 调 LLM（快速测试）
    """
    data = request.get_json(force=True, silent=True) or {}
    mode = data.get("mode", "raw")
    pid = data.get("pid", "").strip()
    agent_key = data.get("agent_key", "")
    prompt = data.get("prompt", "").strip()
    system_prompt = data.get("system_prompt", "").strip()

    if mode == "dialogue":
        if not pid or not prompt:
            return jsonify({"error": "dialogue 模式需要 pid 和 prompt"}), 400
        project = storage.get_project(pid)
        if not project:
            return jsonify({"error": "项目不存在"}), 404
        cfg = storage.load_llm_config().get("dialogue") or {}
        if not cfg.get("model") or not cfg.get("api_key"):
            return jsonify({"error": "dialogue agent 未配置 LLM"}), 400

        # 构建上下文：项目信息 + 对话历史 + 用户消息
        chapters = storage.load_data(pid, "chapters.json") or []
        context = f"当前项目：\n- 标题：{project.get('title', '')}\n- 题材：{project.get('genre', '')}\n- 当前章节号：{project.get('current_chapter', 0)}\n- 当前场景：{project.get('current_scene', '')}\n- 焦点角色：{', '.join(project.get('focus_characters', []))}\n- 已定稿章节数：{len(chapters)}"
        sys_prompt = (system_prompt or storage.load_prompt(pid, "dialogue")) + "\n\n" + context
        messages = [{"role": "system", "content": sys_prompt}]

        # 加载对话历史
        history = storage.load_dialogue_history(pid) or []
        messages.extend(history)
        messages.append({"role": "user", "content": prompt})
        try:
            text = llm.chat(cfg, messages)
            return jsonify({"response": text, "context": context, "history_length": len(history)})
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    elif mode == "character":
        if not pid:
            return jsonify({"error": "character 模式需要 pid"}), 400
        project = storage.get_project(pid)
        if not project:
            return jsonify({"error": "项目不存在"}), 404
        cfg = storage.load_llm_config().get("character") or {}
        if not cfg.get("model") or not cfg.get("api_key"):
            return jsonify({"error": "character agent 未配置 LLM"}), 400

        characters = storage.load_data(pid, "characters.json") or []
        world_rules = storage.load_data(pid, "world_rules.json") or {}
        messages_all = storage.load_data(pid, "messages.json") or []
        relationships = storage.load_data(pid, "relationships.json") or []
        conditions = storage.load_data(pid, "completion_conditions.json") or {}
        current_scene = project.get("current_scene", "")
        chapter_messages = storage.load_data(pid, "chapter_workspace.json") or {}

        # 构建真实名单
        real_list = [c for c in characters
                     if c.get("current_location") == current_scene
                     and c.get("activation_state", "active") != "dormant"]
        if not real_list:
            focus = next((c for c in characters if c.get("is_focus")
                          and c.get("activation_state", "active") != "dormant"), None)
            if not focus:
                focus = next((c for c in characters
                              if c.get("activation_state", "active") != "dormant"), None)
            if focus:
                current_scene = focus.get("current_location", current_scene)
                real_list = [c for c in characters
                             if c.get("current_location") == current_scene
                             and c.get("activation_state", "active") != "dormant"]

        if not real_list:
            return jsonify({"error": "当前场景无可推演的角色", "current_scene": current_scene}), 400

        # 选第一个角色
        char = real_list[0]
        char_name = char.get("name")
        perception = [c.get("name") for c in characters
                      if c.get("current_location") == current_scene
                      and c.get("name") != char_name
                      and c.get("activation_state", "active") != "dormant"]

        char_messages = [m for m in messages_all
                         if m.get("speaker") == char_name]
        ws_msgs = chapter_messages.get("chapter_messages", [])
        public_pool = messages_all + ws_msgs
        conditions_summary = [c.get("title") for c in (conditions.get("conditions") or [])
                              if not c.get("achieved")]
        context_pack = agents.build_context_pack(
            char, perception, char_messages, public_pool,
            conditions_summary, char.get("is_focus", False),
        )
        # 移除 system_prompt（已作为 system 角色消息单独发送，避免重复）
        context_pack.pop("system_prompt", None)

        char_prompt = system_prompt or storage.load_prompt(pid, "character")
        try:
            text = llm.chat(cfg, [
                {"role": "system", "content": char_prompt},
                {"role": "user", "content": json.dumps(context_pack, ensure_ascii=False)},
            ])
            parsed = agents.parse_character_output(text)
            return jsonify({
                "character": char,
                "context_pack": context_pack,
                "candidates": [c.get("name") for c in real_list],
                "current_scene": current_scene,
                "raw_response": text,
                "parsed": parsed,
            })
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    elif mode == "raw":
        if agent_key not in _DEV_AGENTS:
            return jsonify({"error": f"agent_key 必须是 {_DEV_AGENTS} 之一"}), 400
        if not prompt:
            return jsonify({"error": "prompt 不能为空"}), 400
        cfg = storage.load_llm_config().get(agent_key) or {}
        if not cfg.get("model") or not cfg.get("api_key"):
            return jsonify({"error": f"agent '{agent_key}' 未配置 LLM"}), 400
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        if pid:
            project = storage.get_project(pid)
            if project:
                msgs_ctx = f"当前项目：{project.get('title','')} 当前章节：{project.get('current_chapter',0)} 当前场景：{project.get('current_scene','')}"
                if system_prompt:
                    messages[0] = {"role": "system", "content": system_prompt + "\n\n" + msgs_ctx}
                else:
                    messages.append({"role": "system", "content": msgs_ctx})
        messages.append({"role": "user", "content": prompt})
        try:
            text = llm.chat(cfg, messages)
            return jsonify({"response": text})
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return jsonify({"error": f"未知 mode: {mode}"}), 400


if __name__ == "__main__":
    # 自检代码已移除（避免 reloader 双执行 + 污染 projects/ 目录）。
    # 原 Task 13 路由自检见 tests/test_task13_routes.py（如有）。
    # Werkzeug reloader 会让模块执行两次，self-check 跑两次会命中未清理的临时项目。
    app.run(host="127.0.0.1", port=5000, debug=True)
