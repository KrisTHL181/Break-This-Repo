"""阶段 5 修改流程：用户随时发起章节修改。
函数式风格，单文件。不修改 agents/storage/finalization。"""
from datetime import datetime

import storage
import agents
import dialogue_agent

LOG_FILE = "logs/edit_log.json"


def _load_log(pid):
    return storage.load_data(pid, LOG_FILE) or []


def _save_log(pid, log):
    storage.save_data(pid, LOG_FILE, log)


def _find_chapter(chapters, chapter_num):
    """线性查找；上限 O(n)，n 通常 < 100。"""
    for ch in chapters or []:
        if ch.get("chapter") == chapter_num:
            return ch
    return None


def locate_chapter(pid, chapter_num=None, keyword=None):
    """定位目标章节。
    chapter_num 提供：直接返回 {chapter: {...}}。
    否则调 dialogue_agent.search_chapters(keyword, chapters) 返回 {candidates: [...]}。
    """
    chapters = storage.load_data(pid, "chapters.json") or []
    if chapter_num is not None:
        ch = _find_chapter(chapters, chapter_num)
        return {"chapter": ch}
    return {"candidates": dialogue_agent.search_chapters(keyword, chapters)}


def execute_modification(pid, chapter_num, instruction):
    """群主AI 执行修改：
    1. 读取 chapters.json 找到该章
    2. 调 agents.protect_finalized 检查（已定稿且无 target → 仅提醒）
    3. 调 agents.execute_edit 修改正文
    4. 记录修改日志（写入 logs/edit_log.json，committed=False）
    5. 返回 {old_text, new_text, operations, affected_ranges, warning?}
    """
    chapters = storage.load_data(pid, "chapters.json") or []
    ch = _find_chapter(chapters, chapter_num)
    if not ch:
        raise RuntimeError(f"未找到章节 {chapter_num}")
    old_text = ch.get("text") or ""

    cfg = storage.load_llm_config().get("gm") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        raise RuntimeError("未配置 gm agent 的 LLM")

    warning_obj = agents.protect_finalized(cfg, instruction, bool(ch.get("finalized")))
    warning = warning_obj.get("warning") if warning_obj else None

    result = agents.execute_edit(cfg, instruction, old_text, storage.load_prompt(pid, "gm"))

    log = _load_log(pid)
    log.append({
        "timestamp": datetime.now().isoformat(),
        "chapter": chapter_num,
        "instruction": instruction,
        "operations": result.get("operations") or [],
        "affected_ranges": result.get("affected_ranges") or [],
        "warning": warning,
        "committed": False,
    })
    _save_log(pid, log)

    return {
        "old_text": old_text,
        "new_text": result.get("new_text", old_text),
        "operations": result.get("operations") or [],
        "affected_ranges": result.get("affected_ranges") or [],
        "warning": warning,
    }


def revalidate_modification(pid, chapter_num, new_text, affected_ranges):
    """二次校验：调 inspector_validate 校验波及段落（复用 finalization.revalidate 的段落切片启发式）。
    返回 {passed, new_conflicts}。"""
    cfg = storage.load_llm_config().get("inspector") or {}
    if not cfg.get("model") or not cfg.get("api_key"):
        raise RuntimeError("未配置 inspector agent 的 LLM")
    chapters = storage.load_data(pid, "chapters.json") or []
    if not _find_chapter(chapters, chapter_num):
        raise RuntimeError(f"未找到章节 {chapter_num}")
    characters = storage.load_data(pid, "characters.json") or []
    world_rules = storage.load_data(pid, "world_rules.json") or {}

    # 段落切片复用 agents.slice_affected_paragraphs（与 finalization.revalidate 一致）
    slice_text = agents.slice_affected_paragraphs(new_text, affected_ranges)

    if not slice_text.strip():
        return {"passed": True, "new_conflicts": []}

    result = agents.inspector_validate(cfg, slice_text, characters, world_rules, storage.load_prompt(pid, "inspector_validate"))
    conflicts = result.get("conflicts") or []
    return {"passed": len(conflicts) == 0, "new_conflicts": conflicts}


def commit_modification(pid, chapter_num, new_text):
    """提交修改：写入 chapters.json 该章的 text + word_count；保存增量快照 v{chapter_num}_edit_{ts}；写日志 committed=True。

    B3 修复：用 storage.get_pid_lock(pid) 保护 read-modify-write，防止与
    finalization.finalize_chapter 并发覆盖 chapters.json。
    """
    with storage.get_pid_lock(pid):
        chapters = storage.load_data(pid, "chapters.json") or []
        ch = _find_chapter(chapters, chapter_num)
        if not ch:
            raise RuntimeError(f"未找到章节 {chapter_num}")
        ch["text"] = new_text
        ch["word_count"] = len(new_text)
        ch["updated_at"] = datetime.now().isoformat()
        ch["finalized"] = True  # R21 修复：commit 修改后保持定稿状态（前端 renderPreviewPanel 依赖）
        storage.save_data(pid, "chapters.json", chapters)

        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        storage.save_snapshot(pid, f"v{chapter_num}_edit_{ts}")

        log = _load_log(pid)
        log.append({
            "timestamp": datetime.now().isoformat(),
            "chapter": chapter_num,
            "instruction": None,
            "operations": [],
            "affected_ranges": [],
            "warning": None,
            "committed": True,
        })
        _save_log(pid, log)

        return {"committed": True, "chapter": chapter_num, "word_count": ch["word_count"]}


def discard_modification(pid, chapter_num):
    """放弃修改：chapters.json 未被修改。返回 {discarded: True}。"""
    return {"discarded": True, "chapter": chapter_num}


def get_edit_log(pid):
    """读取修改日志。无则返回 []。"""
    return _load_log(pid)


# ---------- 自检：mock LLM，无 fixture，无框架 ----------

if __name__ == "__main__":
    orig_execute = agents.execute_edit
    orig_protect = agents.protect_finalized
    orig_validate = agents.inspector_validate
    orig_load_cfg = storage.load_llm_config

    meta = storage.create_project("editing 自检", "测试")
    pid = meta["id"]
    try:
        chapters = [
            {"chapter": 1, "title": "第1章", "text": "张三走进了酒馆。\n\n他点了一壶酒。", "word_count": 15, "created_at": "2025-01-01T00:00:00"},
            {"chapter": 2, "title": "第2章", "text": "李四在书房读书。\n\n他读到第三页。", "word_count": 12, "created_at": "2025-01-02T00:00:00", "finalized": True},
        ]
        storage.save_data(pid, "chapters.json", chapters)

        # 1. locate_chapter 直接章号定位
        r1 = locate_chapter(pid, chapter_num=1)
        assert r1["chapter"] and r1["chapter"]["chapter"] == 1, r1
        r1b = locate_chapter(pid, chapter_num=99)
        assert r1b["chapter"] is None, r1b

        # 2. locate_chapter 关键词搜索
        r2 = locate_chapter(pid, keyword="酒馆")
        assert "candidates" in r2, r2
        assert len(r2["candidates"]) == 1, r2
        assert r2["candidates"][0]["chapter"] == 1, r2
        assert "酒馆" in r2["candidates"][0]["snippet"], r2
        # 第1章段1"酒馆" + 段2"一壶酒"都含"酒"；第2章无"酒"
        r2b = locate_chapter(pid, keyword="酒")
        assert len(r2b["candidates"]) == 2, r2b
        assert all(c["chapter"] == 1 for c in r2b["candidates"]), r2b

        # 3. execute_modification（mock agents.execute_edit + protect_finalized）
        def mock_edit(cfg, ins, txt):
            return {
                "new_text": txt + "\n\n[补过渡句]",
                "operations": [{"type": "insert", "position": "段3", "before": "", "after": "[补过渡句]"}],
                "transition_added": "[补过渡句]",
                "affected_ranges": ["3"],
            }
        agents.execute_edit = mock_edit
        agents.protect_finalized = lambda cfg, ins, fin: ({"warning": "已定稿章节，仅提醒"} if (fin and not ins.get("target")) else None)
        _mock_cfg = {k: {"provider": "openai", "model": "test", "api_key": "test", "base_url": ""}
                     for k in ("dialogue", "character", "gm", "inspector", "narrator")}
        storage.load_llm_config = lambda: _mock_cfg

        # 3.1 未定稿章 → 无 warning，日志 committed=False
        ex = execute_modification(pid, 1, {"target": "段3", "position": "段3", "operation": "insert", "content": "[补]", "scope": "仅本段"})
        assert ex["old_text"] == chapters[0]["text"], ex
        assert ex["new_text"].endswith("[补过渡句]"), ex
        assert ex["operations"] and len(ex["operations"]) == 1, ex
        assert ex["affected_ranges"] == ["3"], ex
        assert ex["warning"] is None, ex
        log = get_edit_log(pid)
        assert len(log) == 1 and log[0]["committed"] is False, log
        assert log[0]["chapter"] == 1 and log[0]["operations"], log

        # 3.2 已定稿章 + target 为空 → 返回 warning，流程继续
        ex2 = execute_modification(pid, 2, {"target": "", "position": "", "operation": "replace", "content": "x", "scope": ""})
        assert ex2["warning"] and "已定稿" in ex2["warning"], ex2
        log2 = get_edit_log(pid)
        assert len(log2) == 2, log2

        # 4. commit_modification 验证 chapters.json 更新 + 快照创建
        cm = commit_modification(pid, 1, ex["new_text"])
        assert cm["committed"] is True, cm
        assert cm["word_count"] == len(ex["new_text"]), cm
        chs = storage.load_data(pid, "chapters.json")
        assert chs[0]["text"] == ex["new_text"], chs[0]
        assert chs[0]["word_count"] == len(ex["new_text"]), chs[0]
        snaps = storage.list_snapshots(pid)
        edit_snaps = [s for s in snaps if s.startswith("v1_edit_")]
        assert edit_snaps, snaps
        log3 = get_edit_log(pid)
        assert any(e["committed"] for e in log3), log3

        # 5. discard_modification 验证 chapters.json 未变
        chs_before = storage.load_data(pid, "chapters.json")
        dc = discard_modification(pid, 1)
        assert dc["discarded"] is True, dc
        chs_after = storage.load_data(pid, "chapters.json")
        assert chs_after == chs_before, "chapters.json should not change on discard"

        # 6. revalidate_modification（mock inspector_validate）
        def mock_validate(cfg, text, chars, rules):
            return {"conflicts": [{"type": "性格冲突", "location": "段3", "original": "[补过渡句]", "suggestion": "建议修改"}], "passed": ""}
        agents.inspector_validate = mock_validate
        rv = revalidate_modification(pid, 1, ex["new_text"], ["3"])
        assert rv["passed"] is False, rv
        assert len(rv["new_conflicts"]) == 1, rv

        def mock_validate_ok(cfg, text, chars, rules):
            return {"conflicts": [], "passed": "通过"}
        agents.inspector_validate = mock_validate_ok
        rv2 = revalidate_modification(pid, 1, ex["new_text"], ["3"])
        assert rv2["passed"] is True, rv2
        assert rv2["new_conflicts"] == [], rv2

        # 6.1 无 affected_ranges → 校验全文兜底
        rv3 = revalidate_modification(pid, 1, ex["new_text"], [])
        assert rv3["passed"] is True, rv3

        print("ALL CHECKS PASSED")
    finally:
        agents.execute_edit = orig_execute
        agents.protect_finalized = orig_protect
        agents.inspector_validate = orig_validate
        storage.load_llm_config = orig_load_cfg
        storage.delete_project(pid)
