"""阶段 1 推演引擎：章节启动 + SSE 流式推演循环 + 喊停。
函数式 + 内存活动状态表，单文件。Task 11：多线程模式（场景轮转 + 跨场景 @ 判断）。"""
import copy
import json
import os
import shutil
import threading

import storage
import agents
import llm
import sse_utils

_emit = sse_utils.emit  # 模块内便捷引用

# 内存中的活动章节状态（key=pid）。崩溃丢失可接受，持久化靠每轮 messages/characters 落盘。
_active_chapters = {}
# 线程锁：保护 _active_chapters / _streaming_pids 在 Flask threading 下的并发访问。
# 上限：单机单用户，并发极低；锁粒度按全局即可。升级路径：per-pid 锁字典。
_state_lock = threading.Lock()

# 多线程模式：每场景推演 SCENE_SWITCH_ROUNDS 轮后切换到下一场景
SCENE_SWITCH_ROUNDS = 3

# 通信手段关键词（与 _build_candidates 启发式一致）。升级路径：scenes.json 显式 has_comm 字段
_COMM_KEYWORDS = ["传音符", "手机", "千里传音", "通信", "传音"]


def _has_comm_means(world_rules):
    """世界规则文本是否含通信手段关键词。"""
    world_text = json.dumps(world_rules, ensure_ascii=False)
    return any(kw in world_text for kw in _COMM_KEYWORDS)


def _sanitize_scene_name(name):
    """场景名 → 文件名安全串（仅替换路径/冒号分隔符，保留中文）。"""
    return str(name).replace("/", "_").replace("\\", "_").replace(":", "_").replace(" ", "_")


def _collect_active_scenes(meta, characters):
    """多线程模式：以 current_scene 起始，补全所有角色 current_location。"""
    current_scene = meta.get("current_scene", "")
    scenes = []
    seen = set()
    if current_scene and current_scene not in seen:
        scenes.append(current_scene)
        seen.add(current_scene)
    for c in characters or []:
        loc = c.get("current_location", "")
        if loc and loc not in seen:
            scenes.append(loc)
            seen.add(loc)
    return scenes


def start_chapter(pid):
    """章节启动：章节号+1，初始化临时工作区，返回新章节号。
    R21 修复：推演流运行中时幂等返回当前章号，避免 AI 重复调用导致章节号跳跃（1→2→3→4...）。
    R22 修复：推演结束后 _active_chapters 保留用于定稿流程，stop_requested 残留会导致重启时立即停止。
    修复方案：推演流未运行但有活动章节 → 恢复该章节（重置 stop_requested，不新建章节）。
    多线程模式：active_scenes 根据角色 current_location 推导。
    C2 修复：is_streaming 时直接返回内存中的章号，不再读磁盘 workspace（避免 workspace 落盘延迟导致误判为空）。
    H1 修复：用 _state_lock 保护，避免并发请求竞态。"""
    with _state_lock:
        # C2 修复：推演流运行中 → 直接返回内存状态中的章号（不再读磁盘 workspace）
        # H2 修复：直接检查 _streaming_pids 而非调 is_streaming()，避免重复拿锁死锁
        if pid in _streaming_pids:
            state = _active_chapters.get(pid)
            if state and state.get("chapter_num"):
                return state["chapter_num"]
        # R22 修复：有未定稿章节但推演流未运行 → 恢复该章节（重置停止标志，继续推演）
        # R22 修复：后端重启后 _active_chapters 内存丢失但磁盘 chapter_workspace.json 还在，
        # 必须从 workspace 重建内存状态，否则会错误新建章节（章号+1）。
        # 旧 bug：只检查内存 _active_chapters，后端重启后内存为空 → 走到新建路径 → 章号跳跃。
        ws = storage.load_data(pid, "chapter_workspace.json")
        if ws and ws.get("chapter_num"):
            if pid not in _active_chapters or not _active_chapters[pid]:
                # 后端重启后的恢复：从磁盘 workspace 重建内存状态
                active_scenes = ws.get("active_scenes") or [ws.get("current_scene", "")]
                _active_chapters[pid] = {
                    "chapter_num": ws["chapter_num"],
                    "stop_requested": False,
                    "round": ws.get("round", 0),
                    "word_count": ws.get("word_count", 0),
                    "chapter_messages": ws.get("chapter_messages", []),
                    "pending_state_changes": ws.get("pending_state_changes", []),
                    "new_characters_queue": ws.get("new_characters_queue", []),
                    "last_at_mentions": [],
                    "silent_rounds": 0,
                    "active_scenes": active_scenes,
                    "current_scene_index": ws.get("current_scene_index", 0),
                    "rounds_in_current_scene": ws.get("rounds_in_current_scene", 0),
                    "paused_scenes": ws.get("paused_scenes", []),
                    "scene_pending_at": ws.get("scene_pending_at", {s: [] for s in active_scenes}),
                    "characters": ws.get("characters") or storage.load_data(pid, "characters.json") or [],
                }
            else:
                _active_chapters[pid]["stop_requested"] = False
            # R22 修复：同步 meta.current_chapter = workspace 章号。
            ws_chapter = ws["chapter_num"]
            meta = storage.get_project(pid)
            if meta and meta.get("current_chapter") != ws_chapter:
                meta["current_chapter"] = ws_chapter
                storage.update_project(pid, meta)
            return ws_chapter
        meta = storage.get_project(pid)
        new_chapter = meta["current_chapter"] + 1
        meta["current_chapter"] = new_chapter
        storage.update_project(pid, meta)

        current_scene = meta.get("current_scene", "")
        characters = storage.load_data(pid, "characters.json") or []
        multithread = bool(meta.get("multithread"))
        active_scenes = _collect_active_scenes(meta, characters) if multithread else [current_scene]

        _active_chapters[pid] = {
            "chapter_num": new_chapter,
            "stop_requested": False,
            "round": 0,
            "word_count": 0,
            "chapter_messages": [],
            "pending_state_changes": [],
            "new_characters_queue": [],
            "last_at_mentions": [],
            "silent_rounds": 0,
            # Task 11 多线程相关
            "active_scenes": active_scenes,
            "current_scene_index": 0,
            "rounds_in_current_scene": 0,
            "paused_scenes": [],            # 单线程模式已暂停场景列表
            "scene_pending_at": {s: [] for s in active_scenes},  # 各场景待处理跨场景 @
            "characters": characters,       # H4 修复：保存当前 characters 副本，pause_scene 用此避免读到旧位置
        }
        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": new_chapter,
            "round": 0,
            "word_count": 0,
            "chapter_messages": [],
            "pending_state_changes": [],
            "new_characters_queue": [],
            "characters": characters,
            "current_scene": current_scene,
            "multithread": multithread,
            "active_scenes": active_scenes,
        })
        return new_chapter


def request_stop(pid):
    """用户喊停（轮次边界生效，不中断当前角色发言）。"""
    with _state_lock:
        if pid in _active_chapters:
            _active_chapters[pid]["stop_requested"] = True


# 流式推演中标志（pid 集合）。供对话员判断是否仅接受喊停。
# 上限：单进程内并发推演的项目数；本设计为单机单用户，实际并发为 1。
_streaming_pids = set()


def start_streaming(pid):
    """H1 修复：返回 bool 表示是否成功获取推演锁。
    同一 pid 已在推演时返回 False（防止跨标签页/并发请求双开推演流）。
    调用方（app.api_chapter_stream）应据此返回 409。"""
    with _state_lock:
        if pid in _streaming_pids:
            return False
        _streaming_pids.add(pid)
        return True


def stop_streaming(pid):
    with _state_lock:
        _streaming_pids.discard(pid)


def is_streaming(pid):
    with _state_lock:
        return pid in _streaming_pids


def get_active_state(pid):
    """获取活动章节状态（供前端轮询 + AI 工具查询）。
    B6 修复：返回 deepcopy 快照，避免调用方在锁外读到 run_deduction_stream
    正在修改的中间态字段（如 round 已 +1 但 chapter_messages 还没 append）。"""
    with _state_lock:
        s = _active_chapters.get(pid)
        streaming = pid in _streaming_pids
    if not s:
        return None
    # B6：深拷贝快照，防止外部读到正在被推演循环修改的引用对象
    queue = copy.deepcopy(s.get("new_characters_queue", []))
    return {
        "chapter_num": s.get("chapter_num"),
        "round": s["round"],
        "word_count": s["word_count"],
        "stop_requested": s["stop_requested"],
        "streaming": streaming,  # R21: 区分"有活动章节"和"正在推演"（SSE 流是否在跑）
        "new_characters_queue": queue,
        "chapter_messages_count": len(s["chapter_messages"]),
        "multithread": len(s.get("active_scenes", [])) > 1,
        "active_scenes": list(s.get("active_scenes", [])),
        "current_scene_index": s.get("current_scene_index", 0),
        "rounds_in_current_scene": s.get("rounds_in_current_scene", 0),
        "scene_switch_rounds": SCENE_SWITCH_ROUNDS,
        "paused_scenes": list(s.get("paused_scenes", [])),
    }


def clear_active_chapter(pid):
    """清除活动章节内存状态（供 finalization / storage 等模块调用）。"""
    with _state_lock:
        _active_chapters.pop(pid, None)
        _streaming_pids.discard(pid)


def _build_candidates(real_list, last_at_mentions, all_characters, world_rules, current_scene):
    """生成候选名单（3 级优先级累加）：
    1. 上一轮被 @ 且同场景的角色（即使休眠也强制加入）
    2. 当前位置==当前场景 且 非休眠
    3. 跨场景 @（仅当世界规则含通信手段时）
    # 通信手段启发式：世界规则文本字面匹配关键词；升级路径：scenes.json 显式 has_comm 字段
    """
    candidates = []
    seen = set()

    for name in last_at_mentions:
        char = next((c for c in all_characters if c.get("name") == name), None)
        if char and char.get("current_location") == current_scene and name not in seen:
            candidates.append(char)
            seen.add(name)

    for char in real_list:
        if char.get("name") not in seen:
            candidates.append(char)
            seen.add(char.get("name"))

    if _has_comm_means(world_rules):
        for name in last_at_mentions:
            char = next((c for c in all_characters if c.get("name") == name), None)
            if char and char.get("current_location") != current_scene and name not in seen:
                candidates.append(char)
                seen.add(name)

    return candidates


def pause_scene(pid, scene_name):
    """单线程模式：保存场景状态到 scene_workspace_{name}.json。
    保留：该场景的角色 / 消息切片 / 上次 @ 列表 / 推演进度。
    H4 修复：从内存 state["characters"] 读取（推演中已更新的位置），不再读 characters.json（开章时的旧位置）。
    """
    with _state_lock:
        state = _active_chapters.get(pid)
        if not state:
            return None
        # 锁内拷贝快照，避免与 run_deduction_stream 迭代冲突
        characters = list(state.get("characters") or [])
        chapter_messages = list(state.get("chapter_messages") or [])
        last_at_mentions = list(state.get("last_at_mentions") or [])
        round_num = state["round"]
        word_count = state["word_count"]
        if scene_name not in state["paused_scenes"]:
            state["paused_scenes"].append(scene_name)

    # I/O 在锁外执行
    scene_chars = [c for c in characters if c.get("current_location") == scene_name]
    scene_char_names = {c.get("name") for c in scene_chars}
    scene_msgs = [m for m in chapter_messages
                  if m.get("speaker") in scene_char_names]
    workspace = {
        "scene_name": scene_name,
        "chapter_messages": scene_msgs,
        "characters_in_scene": scene_chars,
        "last_at_mentions": [n for n in last_at_mentions if n in scene_char_names],
        "round": round_num,
        "word_count": word_count,
    }
    storage.save_data(pid, f"scene_workspace_{_sanitize_scene_name(scene_name)}.json", workspace)
    return workspace


def resume_scene(pid, scene_name):
    """单线程模式：恢复指定场景。
    读取 scene_workspace_{name}.json，把 chapter_messages 合并回当前工作区，更新 current_scene。
    M3 修复：成功恢复后删除磁盘 scene_workspace 文件，避免残留误导 detect_crash_recovery。"""
    with _state_lock:
        state = _active_chapters.get(pid)
        if not state:
            return None

    workspace = storage.load_data(pid, f"scene_workspace_{_sanitize_scene_name(scene_name)}.json")
    if not workspace:
        return None

    # 锁内合并消息，避免与 run_deduction_stream 并发 append
    with _state_lock:
        state = _active_chapters.get(pid)  # 重新读取，状态可能已变化
        if not state:
            return None
        existing_keys = {(m.get("speaker"), m.get("round"), (m.get("text") or "")[:30])
                         for m in state["chapter_messages"]}
        for m in workspace.get("chapter_messages", []):
            key = (m.get("speaker"), m.get("round"), (m.get("text") or "")[:30])
            if key not in existing_keys:
                state["chapter_messages"].append(m)
                existing_keys.add(key)
        if scene_name in state["paused_scenes"]:
            state["paused_scenes"].remove(scene_name)

    # I/O 在锁外执行
    meta = storage.get_project(pid)
    meta["current_scene"] = scene_name
    storage.update_project(pid, meta)
    ws_path = os.path.join(storage.project_dir(pid), f"scene_workspace_{_sanitize_scene_name(scene_name)}.json")
    try:
        if os.path.exists(ws_path):
            os.remove(ws_path)
    except OSError:
        pass
    return workspace


def list_paused_scenes(pid):
    """返回已暂停场景列表。"""
    with _state_lock:
        state = _active_chapters.get(pid)
    return list(state.get("paused_scenes", [])) if state else []


def list_active_scenes(pid):
    """返回多线程模式活动场景列表。"""
    with _state_lock:
        state = _active_chapters.get(pid)
    return list(state.get("active_scenes", [])) if state else []


def set_multithread(pid, enabled):
    """切换多线程模式（即时生效，不重置已保存数据）。
    若章节正在进行：根据当前角色 current_location 重建 active_scenes。"""
    meta = storage.get_project(pid)
    if meta is None:
        return None
    meta["multithread"] = bool(enabled)
    storage.update_project(pid, meta)
    if pid in _active_chapters:
        state = _active_chapters[pid]
        current_scene = meta.get("current_scene", "")
        if enabled:
            state["active_scenes"] = _collect_active_scenes(meta, storage.load_data(pid, "characters.json") or [])
        else:
            state["active_scenes"] = [current_scene]
        state["current_scene_index"] = 0
        state["rounds_in_current_scene"] = 0
        for s in state["active_scenes"]:
            state["scene_pending_at"].setdefault(s, [])
    return meta


def judge_cross_scene_at(pid, at_mention, world_rules, llm_config=None):
    """多线程跨场景 @ 通信判断：
    1. 启发式：世界规则含通信手段关键词 → can_reach=True
    2. 否则调叙事者判断（返回 JSON: {can_reach, reason}）
    返回 {can_reach, reason, method: "heuristic"|"narrator"}。
    # 启发式与 _build_candidates 一致；不确定时才调 LLM，节省 token
    """
    if _has_comm_means(world_rules):
        return {"can_reach": True, "reason": "世界规则含通信手段", "method": "heuristic"}
    if llm_config is None:
        llm_config = storage.load_llm_config()
    narrator_cfg = (llm_config or {}).get("narrator") or {}
    if not narrator_cfg.get("model") or not narrator_cfg.get("api_key"):
        # 无 LLM 配置：保守拒绝（避免误传）
        return {"can_reach": False, "reason": "世界规则无通信手段且无叙事者 LLM", "method": "heuristic"}
    instruction = (f"判断在当前世界规则下，角色是否能通过某种方式跨场景联系到对方（@{at_mention}）。"
                   f"返回 JSON: {{\"can_reach\": true/false, \"reason\": \"...\"}}")
    try:
        text = agents.narrator_describe(narrator_cfg, instruction, [], storage.load_prompt(pid, "narrator_describe"))
        result = agents.parse_json(text) if isinstance(text, str) else None
        if isinstance(result, dict) and "can_reach" in result:
            return {"can_reach": bool(result["can_reach"]),
                    "reason": result.get("reason", ""), "method": "narrator"}
    except Exception as e:
        print(f"[WARNING] 叙事者跨场景通信判断失败: {e}")
    return {"can_reach": False, "reason": "叙事者判断失败", "method": "narrator"}


def run_deduction_stream(pid):
    """推演循环生成器，yield SSE 格式事件字符串。"""
    meta = storage.get_project(pid)
    characters = storage.load_data(pid, "characters.json") or []
    world_rules = storage.load_data(pid, "world_rules.json") or {}
    messages = storage.load_data(pid, "messages.json") or []
    relationships = storage.load_data(pid, "relationships.json") or []
    conditions = storage.load_data(pid, "completion_conditions.json") or {}
    llm_config = storage.load_llm_config()
    # R22 自定义提示词：预读 11 个 key 的 prompt（项目>全局>默认），循环内复用避免重复 IO
    prompts = {k: storage.load_prompt(pid, k) for k in storage.PROMPT_AGENT_KEYS}

    current_scene = meta.get("current_scene", "")
    word_target = meta.get("word_count_target") or {"min": 2000, "max": 3000}
    multithread = bool(meta.get("multithread"))

    # R21 修复：character agent 配置缺失时直接报错退出，避免推演循环静默失败（word_count=0 卡住）
    # 根因：重启后端后内存 LLM 配置丢失，char_config 为空 → llm.chat_stream 重试 3 次失败 → 所有候选 continue → silent_2_rounds
    char_cfg = llm_config.get("character") or {}
    if not char_cfg.get("model") or not char_cfg.get("api_key"):
        yield _emit("error", {"message": "未配置 character agent 的 LLM（请在设置页配置并确保已推送）"})
        yield _emit("chapter_end", {"reason": "no_character_llm"})
        yield _emit("deduction_complete", {"total_rounds": 0, "word_count": 0})
        return

    state = _active_chapters[pid]

    while True:
        # R21 修复：轮次开始前检查 stop_requested，避免 stop 后还进入新轮次
        if state["stop_requested"]:
            yield _emit("chapter_end", {"reason": "user_stop"})
            break
        state["round"] += 1
        round_num = state["round"]

        # 多线程轮转：达到 SCENE_SWITCH_ROUNDS 后切换到下一场景
        if multithread and len(state["active_scenes"]) > 1:
            if state["rounds_in_current_scene"] >= SCENE_SWITCH_ROUNDS:
                state["current_scene_index"] = (state["current_scene_index"] + 1) % len(state["active_scenes"])
                state["rounds_in_current_scene"] = 0
                current_scene = state["active_scenes"][state["current_scene_index"]]
                meta["current_scene"] = current_scene
                # 切换场景时：载入该场景的待处理 @，清空队列
                state["last_at_mentions"] = list(state["scene_pending_at"].get(current_scene, []))
                state["scene_pending_at"][current_scene] = []
                yield _emit("scene_switch", {
                    "new_scene": current_scene,
                    "next_index": state["current_scene_index"],
                    "active_scenes": list(state["active_scenes"]),
                })

        state["rounds_in_current_scene"] += 1

        # 1. 真实名单：当前位置==当前场景 且 非休眠
        real_list = [c for c in characters
                     if c.get("current_location") == current_scene
                     and c.get("activation_state", "active") != "dormant"]
        # R21 修复：real_list 为空（场景不匹配）时回退到焦点角色（或首个非休眠角色）的 current_location
        # 常见触发场景：finalize 时 meta.current_scene 已同步，但角色 current_location 被推演中改成更细的描述
        # 上限：仅做一次性回退，不递归；升级路径：在 start_chapter 时就校验场景一致性
        if not real_list:
            focus = next((c for c in characters if c.get("is_focus")
                          and c.get("activation_state", "active") != "dormant"), None)
            if not focus:
                focus = next((c for c in characters
                              if c.get("activation_state", "active") != "dormant"), None)
            if focus and focus.get("current_location"):
                current_scene = focus["current_location"]
                meta["current_scene"] = current_scene
                real_list = [c for c in characters
                             if c.get("current_location") == current_scene
                             and c.get("activation_state", "active") != "dormant"]

        # 2. 感知名单（每角色一份）
        perception = {c.get("name"): agents.filter_perception_list(characters, c, relationships)
                      for c in real_list}

        # 3. 候选名单
        candidates = _build_candidates(real_list, state["last_at_mentions"],
                                       characters, world_rules, current_scene)

        yield _emit("round_start", {
            "round": round_num,
            "candidates": [c.get("name") for c in candidates],
            "scene": current_scene,
            "word_count": state["word_count"],
            "multithread": multithread,
            "rounds_in_current_scene": state["rounds_in_current_scene"],
            "scene_switch_rounds": SCENE_SWITCH_ROUNDS if multithread else None,
        })

        # 4. 顺序调用每个候选角色
        round_had_output = False
        new_at_mentions = []
        for char in candidates:
            if state["stop_requested"]:
                break

            char_name = char.get("name")
            yield _emit("character_start", {"character": char_name})

            char_messages = [m for m in (messages + state["chapter_messages"])
                             if m.get("speaker") == char_name]
            conditions_summary = [c.get("title") for c in (conditions.get("conditions") or [])
                                  if not c.get("achieved")]
            context_pack = agents.build_context_pack(
                char, perception.get(char_name, []),
                char_messages, messages + state["chapter_messages"],
                conditions_summary, char.get("is_focus", False),
            )
            # 移除 system_prompt（已作为 system 角色消息单独发送，避免重复）
            context_pack.pop("system_prompt", None)

            char_config = llm_config.get("character") or {}
            full_text = ""
            try:
                for chunk in llm.chat_stream(char_config, [
                    {"role": "system", "content": prompts["character"]},
                    {"role": "user", "content": json.dumps(context_pack, ensure_ascii=False)},
                ]):
                    full_text += chunk
                    yield _emit("character_chunk", {"character": char_name, "text": chunk})
            except Exception as e:
                yield _emit("error", {"message": f"角色 {char_name} 调用失败: {e}"})
                continue

            parsed = agents.parse_character_output(full_text)

            if parsed["is_silent"]:
                yield _emit("character_silent", {"character": char_name})
                continue

            round_had_output = True

            # 唤叙事者：逐条调用拼接到角色文本末尾
            final_text = parsed["cleaned"]
            for narrator_call in parsed["narrator_calls"]:
                yield _emit("narrator_start", {"instruction": narrator_call})
                try:
                    narrator_text = agents.narrator_describe(
                        llm_config.get("narrator") or {}, narrator_call,
                        state["chapter_messages"][-10:],
                        prompts["narrator_describe"],
                    )
                    final_text += "\n" + narrator_text
                    yield _emit("narrator_end", {"text": narrator_text})
                except Exception as e:
                    yield _emit("warning", {"message": f"叙事者调用失败: {e}"})

            # 多线程跨场景 @ 判断（单线程模式：_build_candidates 启发式已覆盖）
            for at_name in parsed["at_mentions"]:
                target_char = next((c for c in characters if c.get("name") == at_name), None)
                if (multithread and target_char
                        and target_char.get("current_location") != current_scene
                        and target_char.get("current_location") in state["active_scenes"]):
                    target_scene = target_char.get("current_location")
                    judge = judge_cross_scene_at(pid, at_name, world_rules, llm_config)
                    if judge["can_reach"]:
                        state["scene_pending_at"].setdefault(target_scene, []).append(at_name)
                    yield _emit("cross_scene_at", {
                        "from_scene": current_scene,
                        "to_scene": target_scene,
                        "target": at_name,
                        "success": judge["can_reach"],
                        "reason": judge["reason"],
                        "method": judge["method"],
                    })

            new_at_mentions.extend(parsed["at_mentions"])

            msg = {
                "speaker": char_name, "text": final_text,
                "round": round_num, "chapter": meta["current_chapter"],
                "scene": current_scene,
            }
            state["chapter_messages"].append(msg)
            state["word_count"] += len(final_text)

            yield _emit("character_end", {
                "character": char_name, "full_text": final_text,
                "word_count": state["word_count"],
            })

            # 信息提取（检察员配置复用）
            try:
                info = agents.extract_info(
                    llm_config.get("inspector") or {}, char_name, full_text, characters,
                    prompts["inspector_extract"],
                )
                state["pending_state_changes"].append({"character": char_name, "info": info})

                # 场景切换：仅当提取器返回了明确的位置变化
                loc = info.get("location_change")
                if loc and loc.get("character") == char_name and loc.get("new_location"):
                    old_scene = current_scene
                    char["current_location"] = loc["new_location"]
                    if loc["new_location"] != current_scene:
                        current_scene = loc["new_location"]
                        meta["current_scene"] = current_scene
                        yield _emit("scene_change", {"character": char_name, "new_scene": current_scene})
                        # 单线程模式：旧场景暂停（保存状态到 scene_workspace）
                        if not multithread and old_scene:
                            pause_scene(pid, old_scene)
                            yield _emit("scene_pause", {
                                "scene": old_scene,
                                "message": f"场景 [{old_scene}] 已暂停，可稍后恢复",
                            })

                # 新角色入队（不打断推演，阶段 2 处理）
                for nc in (info.get("new_characters") or []):
                    state["new_characters_queue"].append(nc)
                    yield _emit("new_character_detected", {
                        "name": nc.get("name"), "description": nc.get("description"),
                    })
            except Exception as e:
                yield _emit("warning", {"message": f"信息提取失败: {e}"})

        state["last_at_mentions"] = new_at_mentions

        # 5. 持久化本轮状态到临时工作区（崩溃恢复点；不污染 messages.json/characters.json）
        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": meta["current_chapter"],
            "round": state["round"],
            "word_count": state["word_count"],
            "chapter_messages": state["chapter_messages"],
            "pending_state_changes": state["pending_state_changes"],
            "new_characters_queue": state["new_characters_queue"],
            "characters": characters,
            "current_scene": current_scene,
            "multithread": multithread,
            "active_scenes": state["active_scenes"],
            "current_scene_index": state["current_scene_index"],
            "rounds_in_current_scene": state["rounds_in_current_scene"],
            "paused_scenes": state["paused_scenes"],
            "scene_pending_at": state["scene_pending_at"],
        })
        # R22 修复：重新读取最新 meta 再更新 current_scene，避免用函数开头读取的旧 meta 覆盖
        # 其他字段（如 title 被 update_project_meta 修改）被推演循环每轮保存覆盖
        latest_meta = storage.get_project(pid) or meta
        latest_meta["current_scene"] = current_scene
        storage.update_project(pid, latest_meta)
        meta = latest_meta

        yield _emit("round_end", {
            "round": round_num, "word_count": state["word_count"],
            "silent": not round_had_output,
        })

        # 6. 结束条件检查
        if state["stop_requested"]:
            yield _emit("chapter_end", {"reason": "user_stop"})
            break

        if not round_had_output:
            state["silent_rounds"] += 1
            if state["silent_rounds"] >= 2:
                yield _emit("chapter_end", {"reason": "silent_2_rounds"})
                break
        else:
            state["silent_rounds"] = 0

        # 字数软上限：仅发提示不强行掐断（spec 说"不强行掐断"）
        if state["word_count"] >= word_target.get("max", 3000):
            yield _emit("soft_limit", {"word_count": state["word_count"], "target": word_target})

    # 循环结束：保留 _active_chapters[pid] 内存状态用于阶段 2 定稿流程
    # 工作区已落盘 chapter_workspace.json，可由 finalization 模块读取
    yield _emit("deduction_complete", {
        "total_rounds": state["round"], "word_count": state["word_count"],
    })


# ---------- 自检：候选名单 3 级优先级累加（无 LLM、无 fixture） ----------

if __name__ == "__main__":
    chars = [
        {"name": "张三", "current_location": "酒馆", "activation_state": "active"},
        {"name": "李四", "current_location": "酒馆", "activation_state": "active"},
        {"name": "王五", "current_location": "酒馆", "activation_state": "dormant"},  # 休眠
        {"name": "赵六", "current_location": "客栈", "activation_state": "active"},  # 跨场景
    ]
    real = [c for c in chars if c["current_location"] == "酒馆"
            and c.get("activation_state", "active") != "dormant"]
    assert [c["name"] for c in real] == ["张三", "李四"]

    # 无 @：候选 = 真实名单
    cand = _build_candidates(real, [], chars, {}, "酒馆")
    assert [c["name"] for c in cand] == ["张三", "李四"], cand

    # 同场景 @ 优先（王五休眠也强制加入）
    cand = _build_candidates(real, ["王五"], chars, {"rules": []}, "酒馆")
    assert [c["name"] for c in cand] == ["王五", "张三", "李四"], cand

    # 跨场景 @ 无通信手段：赵六不加入
    cand = _build_candidates(real, ["赵六"], chars, {"rules": ["普通世界"]}, "酒馆")
    assert [c["name"] for c in cand] == ["张三", "李四"], cand

    # 跨场景 @ 有通信手段（世界规则含"手机"）：赵六加入
    cand = _build_candidates(real, ["赵六"], chars, {"rules": ["现代世界有手机"]}, "酒馆")
    assert [c["name"] for c in cand] == ["张三", "李四", "赵六"], cand

    # 重复 @ 与同场景去重
    cand = _build_candidates(real, ["张三", "李四"], chars, {}, "酒馆")
    assert [c["name"] for c in cand] == ["张三", "李四"], cand

    # _emit 输出 SSE 格式
    line = _emit("round_start", {"round": 1})
    assert line.startswith("data: ") and line.endswith("\n\n"), repr(line)
    payload = json.loads(line[6:].strip())
    assert payload["type"] == "round_start" and payload["round"] == 1, payload

    # ---------- Task 11 多线程自检 ----------

    # 11.A 多线程场景轮转逻辑（构造 state 模拟 3 轮切换）
    mt_state = {
        "active_scenes": ["酒馆", "森林"],
        "current_scene_index": 0,
        "rounds_in_current_scene": 0,
    }
    # 模拟 3 轮推演
    for _ in range(SCENE_SWITCH_ROUNDS):
        mt_state["rounds_in_current_scene"] += 1
    # 轮转检查
    if mt_state["rounds_in_current_scene"] >= SCENE_SWITCH_ROUNDS:
        mt_state["current_scene_index"] = (mt_state["current_scene_index"] + 1) % len(mt_state["active_scenes"])
        mt_state["rounds_in_current_scene"] = 0
    assert mt_state["current_scene_index"] == 1, mt_state
    assert mt_state["rounds_in_current_scene"] == 0, mt_state
    assert mt_state["active_scenes"][mt_state["current_scene_index"]] == "森林", mt_state
    # 再 3 轮 → 回到酒馆（循环）
    for _ in range(SCENE_SWITCH_ROUNDS):
        mt_state["rounds_in_current_scene"] += 1
    if mt_state["rounds_in_current_scene"] >= SCENE_SWITCH_ROUNDS:
        mt_state["current_scene_index"] = (mt_state["current_scene_index"] + 1) % len(mt_state["active_scenes"])
        mt_state["rounds_in_current_scene"] = 0
    assert mt_state["current_scene_index"] == 0, mt_state
    assert mt_state["active_scenes"][mt_state["current_scene_index"]] == "酒馆", mt_state

    # 11.B 单线程场景暂停：模拟 location_change → 验证 scene_workspace_{name}.json 创建
    test_meta = storage.create_project("测试暂停", "测试")
    test_pid = test_meta["id"]
    test_dir = os.path.join(storage.PROJECTS_DIR, test_pid)
    storage.save_data(test_pid, "characters.json", [
        {"name": "张三", "current_location": "酒馆", "activation_state": "active"},
        {"name": "李四", "current_location": "森林", "activation_state": "active"},
    ])
    _active_chapters[test_pid] = {
        "stop_requested": False, "round": 1, "word_count": 100,
        "chapter_messages": [
            {"speaker": "张三", "text": "酒馆说话", "round": 1, "chapter": 1},
            {"speaker": "李四", "text": "森林说话", "round": 1, "chapter": 1},
        ],
        "pending_state_changes": [], "new_characters_queue": [],
        "last_at_mentions": ["张三"], "silent_rounds": 0,
        "active_scenes": ["酒馆"], "current_scene_index": 0,
        "rounds_in_current_scene": 1, "paused_scenes": [],
        "scene_pending_at": {"酒馆": []},
    }
    ws = pause_scene(test_pid, "酒馆")
    assert ws is not None, "pause_scene 返回 None"
    assert ws["scene_name"] == "酒馆", ws
    assert ws["characters_in_scene"][0]["name"] == "张三", ws
    assert any(m["speaker"] == "张三" for m in ws["chapter_messages"]), ws
    saved = storage.load_data(test_pid, "scene_workspace_酒馆.json")
    assert saved is not None and saved["scene_name"] == "酒馆", "scene_workspace_酒馆.json 未创建"
    assert "酒馆" in _active_chapters[test_pid]["paused_scenes"], "paused_scenes 未记录"
    # 恢复
    resumed = resume_scene(test_pid, "酒馆")
    assert resumed is not None, "resume_scene 返回 None"
    assert "酒馆" not in _active_chapters[test_pid]["paused_scenes"], "paused_scenes 未清除"
    # current_scene 已更新
    assert storage.get_project(test_pid)["current_scene"] == "酒馆"
    # 清理
    shutil.rmtree(test_dir)
    del _active_chapters[test_pid]

    # 11.C judge_cross_scene_at 启发式 + 叙事者（mock）
    # 1. 世界规则含"手机" → 启发式 True
    r = judge_cross_scene_at("any_pid", "李四", {"rules": ["现代世界有手机"]})
    assert r["can_reach"] is True and r["method"] == "heuristic", r
    # 2. 无通信手段 + 无 LLM 配置 → 保守 False（不实际调 LLM）
    r = judge_cross_scene_at("any_pid", "李四", {"rules": ["普通世界"]}, llm_config={})
    assert r["can_reach"] is False and r["method"] == "heuristic", r
    # 3. mock 叙事者判断（不实际调 LLM）
    original_narrator = agents.narrator_describe
    agents.narrator_describe = lambda cfg, inst, msgs, prompt_override=None: '{"can_reach": true, "reason": "有信鸽可传递"}'
    try:
        r = judge_cross_scene_at("any_pid", "李四", {"rules": ["古代世界"]},
                                  llm_config={"narrator": {"model": "gpt", "api_key": "x"}})
        assert r["can_reach"] is True and r["method"] == "narrator", r
        assert "信鸽" in r["reason"], r
    finally:
        agents.narrator_describe = original_narrator
    # 4. mock 叙事者返回非 JSON → 兜底 False
    agents.narrator_describe = lambda cfg, inst, msgs, prompt_override=None: '这不是 JSON'
    try:
        r = judge_cross_scene_at("any_pid", "李四", {"rules": ["古代世界"]},
                                  llm_config={"narrator": {"model": "gpt", "api_key": "x"}})
        assert r["can_reach"] is False and r["method"] == "narrator", r
    finally:
        agents.narrator_describe = original_narrator

    # 11.D _has_comm_means 关键词覆盖
    for kw in _COMM_KEYWORDS:
        assert _has_comm_means({"rules": [f"含 {kw} 字样"]}), kw
    assert not _has_comm_means({"rules": ["普通世界"]})

    print("ALL CHECKS PASSED")
