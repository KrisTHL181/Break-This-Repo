"""阶段 2 定稿流程：首次校验 → 用户反馈 → 修改执行 → 二次校验 → 数据固化。
函数式风格，单文件。所有 LLM 调用走 agents 模块。"""
import os
import uuid
from datetime import datetime

import storage
import agents
import deduction
import assistant


def get_workspace(pid):
    """读取本章临时工作区。无则返回 None。"""
    return storage.load_data(pid, "chapter_workspace.json")


def save_workspace(pid, workspace):
    """保存工作区到磁盘。"""
    storage.save_data(pid, "chapter_workspace.json", workspace)


def assemble_chapter_text(workspace):
    """把 chapter_messages 拼接为完整章节正文。
    R21: 不再加【角色名】前缀（用户反馈预览正文带角色名标签影响阅读）。
    多角色发言直接用双换行分隔；叙事者描写已由 deduction 直接拼接到角色文本末尾。"""
    parts = []
    for msg in workspace.get("chapter_messages") or []:
        text = msg.get("text", "")
        if text:
            parts.append(text)
    return "\n\n".join(parts)


def validate_chapter(pid):
    """首次校验：调 inspector_validate，返回 {conflicts, passed, chapter_text}。"""
    workspace = get_workspace(pid)
    if not workspace:
        raise RuntimeError("无活动工作区（chapter_workspace.json 不存在）")
    characters = workspace.get("characters") or storage.load_data(pid, "characters.json") or []
    world_rules = storage.load_data(pid, "world_rules.json") or {}
    cfg = storage.load_llm_config().get("inspector") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        raise RuntimeError("未配置 inspector agent 的 LLM")
    chapter_text = assemble_chapter_text(workspace)
    result = agents.inspector_validate(cfg, chapter_text, characters, world_rules, storage.load_prompt(pid, "inspector_validate"))
    result.setdefault("conflicts", [])
    result.setdefault("passed", "")
    result["chapter_text"] = chapter_text
    # 阶段 6：识别宣称冲突 + 查询同类历史忽略记录（供前端标注提醒）
    result["claim_conflicts"] = assistant.detect_claim_conflicts(result)
    for c in result["conflicts"]:
        c["similar_ignored"] = assistant.find_similar_ignored(pid, c)
    # 记录首次校验结果到工作区（供二次校验对比 + claim-resolve 查 conflict_index）
    workspace["validation"] = {"conflicts": result["conflicts"], "passed": result["passed"]}
    save_workspace(pid, workspace)
    return result


def apply_user_feedback(pid, feedback):
    """用户反馈处理。
    feedback 结构：[{"conflict_index": 0, "action": "fix"|"ignore", "instruction": {...}}]
    对 action=fix 的条目，调 agents.execute_edit 修改正文。
    返回 {new_text, edit_log, affected_ranges, ignored_conflicts}。
    edit_log 结构：[{"conflict_index": 0, "operations": [...], "transition_added": "..."}]
    """
    workspace = get_workspace(pid)
    if not workspace:
        raise RuntimeError("无活动工作区")

    cfg = storage.load_llm_config().get("gm") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        raise RuntimeError("未配置 gm agent 的 LLM")

    chapter_text = workspace.get("chapter_text_override") or assemble_chapter_text(workspace)
    edit_log = []
    ignored_conflicts = []
    affected_ranges = []

    for item in feedback:
        idx = item.get("conflict_index")
        action = item.get("action")
        if action == "fix":
            instruction = item.get("instruction") or {}
            result = agents.execute_edit(cfg, instruction, chapter_text, storage.load_prompt(pid, "gm"))
            chapter_text = result.get("new_text", chapter_text)
            edit_log.append({
                "conflict_index": idx,
                "operations": result.get("operations") or [],
                "transition_added": result.get("transition_added") or "",
            })
            affected_ranges.extend(result.get("affected_ranges") or [])
        elif action == "ignore":
            ignored_conflicts.append(idx)
            # 阶段 6：记录忽略的冲突供历史参考（检察员下次校验时标注"曾忽略过类似问题"）
            validation = workspace.get("validation") or {}
            conflicts = validation.get("conflicts") or []
            if 0 <= idx < len(conflicts):
                try:
                    assistant.record_ignored_conflict(pid, conflicts[idx])
                except Exception as e:
                    print(f"[WARNING] 记录忽略冲突失败: {e}")  # 不阻断主流程

    # 持久化修改后正文 + 修改日志
    workspace["chapter_text_override"] = chapter_text
    workspace["edit_log"] = edit_log
    workspace["ignored_conflicts"] = ignored_conflicts
    workspace["affected_ranges"] = list(set(affected_ranges))
    save_workspace(pid, workspace)

    return {
        "new_text": chapter_text,
        "edit_log": edit_log,
        "affected_ranges": list(set(affected_ranges)),
        "ignored_conflicts": ignored_conflicts,
    }


def revalidate(pid, new_text, affected_ranges):
    """二次校验：仅读取受影响段落，调 inspector_validate（传段落切片）。
    返回 {passed, new_conflicts}。"""
    workspace = get_workspace(pid)
    if not workspace:
        raise RuntimeError("无活动工作区")

    cfg = storage.load_llm_config().get("inspector") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        raise RuntimeError("未配置 inspector agent 的 LLM")
    characters = workspace.get("characters") or storage.load_data(pid, "characters.json") or []
    world_rules = storage.load_data(pid, "world_rules.json") or {}

    # 提取受影响段落：按双换行分段，affected_ranges 为段落号字符串列表
    slice_text = agents.slice_affected_paragraphs(new_text, affected_ranges)

    if not slice_text.strip():
        return {"passed": True, "new_conflicts": []}

    result = agents.inspector_validate(cfg, slice_text, characters, world_rules, storage.load_prompt(pid, "inspector_validate"))
    conflicts = result.get("conflicts") or []
    return {
        "passed": len(conflicts) == 0,
        "new_conflicts": conflicts,
    }


def finalize_chapter(pid):
    """数据固化（不可回退）：
    1. 写入 chapters.json：{chapter, title, text, summary, word_count, created_at}
    2. 叙事者生成 300字摘要 → 写回 chapters.json 该章的 summary
    3. 写入 events.json：从 pending_state_changes 提取 key_events
    4. 更新 characters.json：应用 pending_state_changes（位置/情绪/物品）
    5. 追加 chapter_messages 到 messages.json
    6. 保存增量快照 v{chapter_num}
    7. 删除 chapter_workspace.json
    8. 清除 _active_chapters[pid]
    返回 {chapter, summary, word_count, events_count}。

    B2 修复：用 storage.get_pid_lock(pid) 保护整个 read-modify-write 序列，
    防止与 editing.commit_modification 等并发覆盖。
    B2 修复：任何步骤抛异常时不删除 workspace、不清内存，让用户可重试。
    """
    # B2：pid 级锁，防止与 editing 等并发写 chapters.json
    with storage.get_pid_lock(pid):
        workspace = get_workspace(pid)
        if not workspace:
            raise RuntimeError("无活动工作区")

        meta = storage.get_project(pid)
        chapter_num = workspace.get("chapter_num") or meta.get("current_chapter", 0)

        # 1. 章节正文（优先使用 chapter_text_override，否则从 chapter_messages 拼接）
        text = workspace.get("chapter_text_override") or assemble_chapter_text(workspace)
        # M4 修复：word_count 优先用文本长度（推演中 workspace.word_count 是估算，定稿时用实际文本）
        word_count = len(text)

        # 2. 生成 300 字摘要（LLM 配置缺失则跳过，summary 为空字符串）
        summary = ""
        narrator_cfg = storage.load_llm_config().get("narrator") or {}
        if narrator_cfg.get("model") and narrator_cfg.get("api_key"):
            try:
                summary = agents.narrator_summarize(narrator_cfg, text, storage.load_prompt(pid, "narrator_summarize")) or ""
            except Exception as e:
                print(f"[WARNING] 叙事者摘要失败: {e}")
                summary = ""  # 摘要失败不阻断定稿

        chapter_entry = {
            "chapter": chapter_num,
            "title": f"第{chapter_num}章",
            "text": text,
            "summary": summary,
            "word_count": word_count,
            "created_at": datetime.now().isoformat(),
            "finalized": True,  # R21 修复：标记已定稿（前端 renderPreviewPanel 依赖此字段）
        }
        chapters = storage.load_data(pid, "chapters.json") or []
        chapters.append(chapter_entry)
        storage.save_data(pid, "chapters.json", chapters)

        # 3. 提取关键事件写入 events.json
        events = storage.load_data(pid, "events.json") or []
        for change in workspace.get("pending_state_changes") or []:
            info = change.get("info") or {}
            for ev in (info.get("key_events") or []):
                events.append({
                    "chapter": chapter_num,
                    "type": ev.get("type", ""),
                    "description": ev.get("description", ""),
                })
        storage.save_data(pid, "events.json", events)

        # 4. 应用 pending_state_changes 到 characters.json
        # H5 修复：优先用 workspace.characters（推演中已更新位置/情绪/物品的实时副本），
        # fallback 到 characters.json（推演未启动或 workspace 缺失 characters 字段时）。
        # 风险：若 extract_info 漏提取某项变更，characters.json 仍丢失该变更；但比"完全用旧 characters.json"
        # 至少保留推演中直接修改的字段（如 _apply_state_changes 已处理的 location）。
        characters = workspace.get("characters") or storage.load_data(pid, "characters.json") or []
        for change in workspace.get("pending_state_changes") or []:
            char_name = change.get("character")
            info = change.get("info") or {}
            char = next((c for c in characters if c.get("name") == char_name), None)
            if not char:
                continue
            loc = info.get("location_change")
            if loc and loc.get("character") == char_name and loc.get("new_location"):
                char["current_location"] = loc["new_location"]
            for em in (info.get("emotion_changes") or []):
                if em.get("character") == char_name and em.get("new_emotion"):
                    char["current_emotion"] = em["new_emotion"]
            for it in (info.get("items_gained") or []):
                if it.get("character") == char_name and it.get("item"):
                    char.setdefault("inventory", []).append(it["item"])
        storage.save_data(pid, "characters.json", characters)

        # R21 修复：同步 meta.current_scene 到焦点角色（或首个非休眠角色）的 current_location
        # 否则第 N+1 章启动时 real_list 用旧 current_scene 过滤，可能为空 → 推演无内容生成
        focus_char = next((c for c in characters if c.get("is_focus")), None)
        if not focus_char:
            focus_char = next((c for c in characters
                               if c.get("activation_state", "active") != "dormant"), None)
        if focus_char and focus_char.get("current_location"):
            meta["current_scene"] = focus_char["current_location"]
            storage.update_project(pid, meta)

        # 5. 追加 chapter_messages 到 messages.json
        messages = storage.load_data(pid, "messages.json") or []
        messages.extend(workspace.get("chapter_messages") or [])
        storage.save_data(pid, "messages.json", messages)

        # 6. 保存章节级增量快照
        storage.save_snapshot(pid, f"v{chapter_num}")

        # 7. 删除工作区（仅在所有写入成功后才删除）
        workspace_path = os.path.join(storage.project_dir(pid), "chapter_workspace.json")
        if os.path.isfile(workspace_path):
            os.remove(workspace_path)

    # 8. 清除内存活动状态
    deduction.clear_active_chapter(pid)

    # 9. 阶段 3 完本检测：增量判定 + 总表检测（LLM 失败不阻断定稿）
    completion_result = check_completion(pid, summary, chapter_num)

    return {
        "chapter": chapter_num,
        "summary": summary,
        "word_count": word_count,
        "events_count": len(events),
        "completion": completion_result,
    }


def check_completion(pid, chapter_summary, chapter_num=None):
    """阶段 3 完本检测：增量判定 + 总表检测。
    返回 {achieved, achieved_count, total, all_achieved, signal, error?}。
    LLM 失败/未配置不抛异常，返回 error 字段不阻断定稿。

    1. 读取 completion_conditions.json（兼容 {"conditions": [...]} 与空 {}）
    2. 调 agents.inspector_judge 判定本章是否达成任意未达成条件
    3. 更新达成条件：achieved=True, achieved_chapter=当前章号
    4. 保存回 completion_conditions.json
    5. 调 agents.inspector_detect 检测全达成
    6. 全达成时 meta.can_complete=True 并记录 can_complete_chapter
    """
    meta = storage.get_project(pid) or {}
    if chapter_num is None:
        chapter_num = meta.get("current_chapter", 0)

    cc_data = storage.load_data(pid, "completion_conditions.json") or {}
    conditions = cc_data.get("conditions") if isinstance(cc_data, dict) else None
    if conditions is None:
        conditions = []

    unachieved = [c for c in conditions if not c.get("achieved")]

    achieved = []
    # M7 修复：摘要为空（narrator 失败）时用章节正文前 500 字 fallback，避免完本条件永不检测
    effective_summary = chapter_summary
    if unachieved and not effective_summary:
        chapter = next((c for c in (storage.load_data(pid, "chapters.json") or [])
                        if c.get("chapter") == chapter_num), None)
        if chapter and chapter.get("text"):
            effective_summary = chapter["text"][:500]
    if unachieved and effective_summary:
        cfg = storage.load_llm_config().get("inspector") or {}
        if not cfg.get("model") or not cfg.get("api_key"):
            return {"achieved": [], "achieved_count": sum(1 for c in conditions if c.get("achieved")),
                    "total": len(conditions), "all_achieved": False, "signal": None,
                    "error": "未配置 inspector agent 的 LLM"}
        try:
            result = agents.inspector_judge(cfg, effective_summary, unachieved, storage.load_prompt(pid, "inspector_judge"))
            achieved = result.get("achieved") or []
        except Exception as e:
            return {"achieved": [], "achieved_count": sum(1 for c in conditions if c.get("achieved")),
                    "total": len(conditions), "all_achieved": False, "signal": None,
                    "error": f"inspector_judge 失败：{e}"}

    # 更新达成的条件（不覆盖已达成项的 achieved_chapter）
    achieved_ids = {a.get("id") for a in achieved if a.get("id")}
    for c in conditions:
        if c.get("id") in achieved_ids and not c.get("achieved"):
            c["achieved"] = True
            c["achieved_chapter"] = chapter_num

    storage.save_data(pid, "completion_conditions.json", {"conditions": conditions})

    detect = agents.inspector_detect(conditions)
    if detect["all_achieved"]:
        meta["can_complete"] = True
        meta["can_complete_chapter"] = chapter_num
        storage.update_project(pid, meta)

    return {
        "achieved": achieved,
        "achieved_count": sum(1 for c in conditions if c.get("achieved")),
        "total": len(conditions),
        "all_achieved": detect["all_achieved"],
        "signal": detect["signal"],
    }


def discard_chapter(pid):
    """放弃本章：删除 chapter_workspace.json，清除内存状态。
    不影响 messages.json/characters.json（因为没写入）。回退 current_chapter 便于复用章号。"""
    workspace_path = os.path.join(storage.project_dir(pid), "chapter_workspace.json")
    if os.path.isfile(workspace_path):
        os.remove(workspace_path)
    deduction.clear_active_chapter(pid)
    meta = storage.get_project(pid)
    if meta and meta.get("current_chapter", 0) > 0:
        meta["current_chapter"] -= 1
        storage.update_project(pid, meta)
    return {"discarded": True}


def confirm_new_characters(pid, confirmed):
    """新角色确认：confirmed 为用户接受的新角色列表。
    为每个新角色创建角色卡（默认 active/exposed/非焦点），加入 characters.json。"""
    characters = storage.load_data(pid, "characters.json") or []
    existing_names = {c.get("name") for c in characters}
    added = []
    for nc in confirmed:
        name = (nc.get("name") or "").strip()
        if not name or name in existing_names:
            continue
        char = {
            "id": uuid.uuid4().hex,
            "name": name,
            "description": nc.get("description", ""),
            "personality": "",
            "current_goal": "",
            "current_emotion": "平静",
            "current_location": nc.get("current_location", ""),
            "activation_state": "active",
            "exposure_status": "exposed",
            "is_focus": False,
            "memory_anchors": [],
        }
        characters.append(char)
        added.append(name)
        existing_names.add(name)
    storage.save_data(pid, "characters.json", characters)

    # 从工作区队列移除已确认的（接受或拒绝过的都不再提示）
    workspace = get_workspace(pid)
    if workspace:
        workspace["new_characters_queue"] = [
            nc for nc in (workspace.get("new_characters_queue") or [])
            if (nc.get("name") or "").strip() not in existing_names
        ]
        save_workspace(pid, workspace)

    return {"added": added}


# ---------- 自检：mock LLM，无 fixture，无框架 ----------

if __name__ == "__main__":
    # 1. assemble_chapter_text：R21 去掉角色名前缀，双换行分隔
    ws = {"chapter_messages": [
        {"speaker": "张三", "text": "你好。"},
        {"speaker": "李四", "text": "近来如何？"},
    ]}
    assert assemble_chapter_text(ws) == "你好。\n\n近来如何？", assemble_chapter_text(ws)
    # 空工作区
    assert assemble_chapter_text({"chapter_messages": []}) == ""
    # 无 speaker 直接拼接
    assert assemble_chapter_text({"chapter_messages": [{"speaker": "", "text": "纯叙事"}]}) == "纯叙事"

    # 2. apply_user_feedback：fix + ignore（mock execute_edit）
    original_edit = agents.execute_edit
    original_validate = agents.inspector_validate
    original_summarize = agents.narrator_summarize
    original_judge = agents.inspector_judge
    original_detect = agents.inspector_detect
    original_load_cfg = storage.load_llm_config

    call_count = {"edit": 0}
    def mock_edit(cfg, instruction, chapter_text):
        call_count["edit"] += 1
        return {
            "new_text": chapter_text + "\n\n[补过渡句]",
            "operations": [{"type": "insert", "position": "段3", "before": "", "after": "[补过渡句]"}],
            "transition_added": "[补过渡句]",
            "affected_ranges": ["3"],
        }
    agents.execute_edit = mock_edit
    # mock LLM 配置：让所有 agent 通过早期配置检查
    _mock_cfg = {k: {"provider": "openai", "model": "test", "api_key": "test", "base_url": ""}
                 for k in ("dialogue", "character", "gm", "inspector", "narrator")}
    storage.load_llm_config = lambda: _mock_cfg

    # 建临时项目构造工作区
    meta = storage.create_project("自检项目", "测试")
    pid = meta["id"]
    try:
        workspace = {
            "chapter_num": 1,
            "round": 1,
            "word_count": 100,
            "chapter_messages": [{"speaker": "张三", "text": "原文。"}],
            "pending_state_changes": [
                {"character": "张三", "info": {
                    "location_change": {"character": "张三", "new_location": "酒馆"},
                    "emotion_changes": [{"character": "张三", "new_emotion": "愤怒"}],
                    "items_gained": [{"character": "张三", "item": "短剑"}],
                    "key_events": [{"type": "战斗", "description": "张三与刺客交手"}],
                }},
            ],
            "new_characters_queue": [{"name": "神秘人", "description": "黑衣"}],
            "characters": [{"name": "张三", "current_location": "客栈", "current_emotion": "平静"}],
        }
        storage.save_data(pid, "chapter_workspace.json", workspace)
        storage.save_data(pid, "characters.json", [{"name": "张三", "current_location": "客栈", "current_emotion": "平静"}])

        feedback = [
            {"conflict_index": 0, "action": "fix", "instruction": {"target": "段3", "operation": "insert"}},
            {"conflict_index": 1, "action": "ignore"},
        ]
        result = apply_user_feedback(pid, feedback)
        assert call_count["edit"] == 1, call_count
        assert "[补过渡句]" in result["new_text"], result["new_text"]
        assert len(result["edit_log"]) == 1, result["edit_log"]
        assert result["edit_log"][0]["conflict_index"] == 0
        assert result["ignored_conflicts"] == [1], result["ignored_conflicts"]
        assert "3" in result["affected_ranges"], result["affected_ranges"]

        # 工作区已写入 chapter_text_override
        ws2 = get_workspace(pid)
        assert ws2["chapter_text_override"].endswith("[补过渡句]")

        # 3. 数据固化流程：mock inspector/narrator
        def mock_validate(cfg, text, chars, rules):
            return {"conflicts": [], "passed": "无冲突"}
        agents.inspector_validate = mock_validate
        def mock_summarize(cfg, text):
            return "这是 300 字摘要。"
        agents.narrator_summarize = mock_summarize

        # 新角色确认（接受"神秘人"）
        nc_result = confirm_new_characters(pid, [{"name": "神秘人", "description": "黑衣"}])
        assert nc_result["added"] == ["神秘人"], nc_result
        chars_before = storage.load_data(pid, "characters.json")
        assert any(c["name"] == "神秘人" for c in chars_before)

        fin = finalize_chapter(pid)
        assert fin["chapter"] == 1, fin
        assert fin["summary"] == "这是 300 字摘要。", fin
        assert fin["events_count"] == 1, fin

        # chapters.json：1 条记录
        chs = storage.load_data(pid, "chapters.json")
        assert len(chs) == 1 and chs[0]["chapter"] == 1, chs
        assert chs[0]["summary"] == "这是 300 字摘要。"
        assert "[补过渡句]" in chs[0]["text"], chs[0]["text"]

        # events.json：1 条事件
        evs = storage.load_data(pid, "events.json")
        assert len(evs) == 1 and evs[0]["type"] == "战斗", evs

        # characters.json：张三位置/情绪/物品已更新 + 神秘人
        chars_after = storage.load_data(pid, "characters.json")
        zs = next(c for c in chars_after if c["name"] == "张三")
        assert zs["current_location"] == "酒馆", zs
        assert zs["current_emotion"] == "愤怒", zs
        assert zs.get("inventory") == ["短剑"], zs
        assert any(c["name"] == "神秘人" for c in chars_after)

        # messages.json：1 条追加
        msgs = storage.load_data(pid, "messages.json")
        assert len(msgs) == 1 and msgs[0]["speaker"] == "张三", msgs

        # 增量快照 v1 已保存
        snaps = storage.list_snapshots(pid)
        assert "v1" in snaps, snaps

        # 工作区已删除
        assert get_workspace(pid) is None

        # discard_chapter 行为：新建项目 + 工作区 → discard → current_chapter 回退
        meta2 = storage.create_project("自检2", "测试")
        pid2 = meta2["id"]
        assert meta2["current_chapter"] == 0
        # 模拟 start_chapter 的 current_chapter +1
        meta2["current_chapter"] = 1
        storage.update_project(pid2, meta2)
        storage.save_data(pid2, "chapter_workspace.json", {"chapter_num": 1, "chapter_messages": []})
        d = discard_chapter(pid2)
        assert d["discarded"] is True
        assert get_workspace(pid2) is None
        meta2_after = storage.get_project(pid2)
        assert meta2_after["current_chapter"] == 0, meta2_after
        storage.delete_project(pid2)

        # 4. check_completion：增量判定 + 总表检测（mock inspector_judge）
        meta_cc = storage.create_project("完本检测自检", "测试")
        pid_cc = meta_cc["id"]
        try:
            storage.save_data(pid_cc, "completion_conditions.json", {"conditions": [
                {"id": "c1", "title": "条件1", "achieved": False, "achieved_chapter": None},
                {"id": "c2", "title": "条件2", "achieved": False, "achieved_chapter": None},
            ]})
            meta_cc["current_chapter"] = 5
            storage.update_project(pid_cc, meta_cc)

            # 4.1 第1次：只达成 c1 → 全达成为 False，meta 不标记
            def mock_judge_1(cfg, summary, conds):
                return {"achieved": [{"id": "c1", "title": "条件1", "evidence": "依据1"}]}
            agents.inspector_judge = mock_judge_1

            r1 = check_completion(pid_cc, "本章摘要", 5)
            assert len(r1["achieved"]) == 1, r1
            assert r1["all_achieved"] is False, r1
            assert r1["signal"] is None, r1
            assert r1["achieved_count"] == 1 and r1["total"] == 2, r1

            cc1 = storage.load_data(pid_cc, "completion_conditions.json")
            c1_after = next(c for c in cc1["conditions"] if c["id"] == "c1")
            c2_after = next(c for c in cc1["conditions"] if c["id"] == "c2")
            assert c1_after["achieved"] is True and c1_after["achieved_chapter"] == 5, c1_after
            assert c2_after["achieved"] is False, c2_after

            meta_cc_after = storage.get_project(pid_cc)
            assert meta_cc_after.get("can_complete") is not True, meta_cc_after

            # 4.2 第2次：达成 c2 → 全达成，meta.can_complete=True, can_complete_chapter=6
            def mock_judge_2(cfg, summary, conds):
                return {"achieved": [{"id": "c2", "title": "条件2", "evidence": "依据2"}]}
            agents.inspector_judge = mock_judge_2

            r2 = check_completion(pid_cc, "本章摘要2", 6)
            assert r2["all_achieved"] is True, r2
            assert r2["signal"] == "可完本", r2
            assert r2["achieved_count"] == 2, r2

            cc2 = storage.load_data(pid_cc, "completion_conditions.json")
            c2_after2 = next(c for c in cc2["conditions"] if c["id"] == "c2")
            assert c2_after2["achieved"] is True and c2_after2["achieved_chapter"] == 6, c2_after2
            # c1 的 achieved_chapter 不被覆盖（不覆盖已达成项）
            c1_after2 = next(c for c in cc2["conditions"] if c["id"] == "c1")
            assert c1_after2["achieved_chapter"] == 5, c1_after2

            meta_cc_after2 = storage.get_project(pid_cc)
            assert meta_cc_after2.get("can_complete") is True, meta_cc_after2
            assert meta_cc_after2.get("can_complete_chapter") == 6, meta_cc_after2

            # 4.3 inspector_judge 抛异常 → 容错返回 error，不阻断
            # 重置一个未达成条件，确保 inspector_judge 会被调用
            storage.save_data(pid_cc, "completion_conditions.json", {"conditions": [
                {"id": "c1", "title": "条件1", "achieved": True, "achieved_chapter": 5},
                {"id": "c2", "title": "条件2", "achieved": False, "achieved_chapter": None},
            ]})
            def mock_judge_err(cfg, summary, conds):
                raise RuntimeError("LLM 炸了")
            agents.inspector_judge = mock_judge_err
            r3 = check_completion(pid_cc, "本章摘要3", 7)
            assert "error" in r3 and "LLM 炸了" in r3["error"], r3
            assert r3["all_achieved"] is False and r3["signal"] is None, r3
            # 异常时不应更新条件（c2 仍为未达成）
            cc3 = storage.load_data(pid_cc, "completion_conditions.json")
            c2_after3 = next(c for c in cc3["conditions"] if c["id"] == "c2")
            assert c2_after3["achieved"] is False, c2_after3
        finally:
            storage.delete_project(pid_cc)

        # 5. finalize_chapter 已附带 completion 字段（前面的 fin 来自 pid，无条件 → all_achieved=False）
        assert "completion" in fin, fin
        assert fin["completion"]["all_achieved"] is False, fin["completion"]

        print("ALL CHECKS PASSED")
    finally:
        # 还原 monkey-patch
        agents.execute_edit = original_edit
        agents.inspector_validate = original_validate
        agents.narrator_summarize = original_summarize
        agents.inspector_judge = original_judge
        agents.inspector_detect = original_detect
        storage.load_llm_config = original_load_cfg
        # 清理临时项目（pid 在 try 之前赋值，必然存在）
        storage.delete_project(pid)
