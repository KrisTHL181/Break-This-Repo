"""阶段 6 系统辅助：角色休眠 / 崩溃恢复 / 无关键事件提醒 / 宣称冲突 / 历史忽略参考。
函数式风格，单文件。纯代码逻辑，不依赖 LLM（LLM 未配置时不阻断）。"""
import os
import re
from datetime import datetime

import storage

DORMANT_THRESHOLD = 20  # 连续 20 章未出场且未 @ → 建议休眠
CLAIM_CONFLICT_TYPE = "无依据宣称"
IGNORED_LOG_FILE = "logs/ignored_conflicts.json"


def _name_in_text(name, text, all_names=None):
    """角色名是否在文本中出场。
    B9 修复：避免「张三」匹配「张三丰」。中文无词边界，\b 不可靠；
    改用「排除更长角色名前缀」策略：若 all_names 中存在以 name 为前缀的更长名字
    （如 name=「张三」，all_names 含「张三丰」），则要求匹配后不紧跟该更长名字的剩余部分。
    中文里名字后常直接跟动词（「张三来了」），所以不能简单用 CJK 边界判断。
    升级路径：jieba 分词后精确匹配。
    """
    if not name or not text:
        return False
    if not all_names:
        return name in text
    # 找出所有以 name 为前缀的更长名字，收集它们去掉 name 后的剩余后缀
    suffixes = []
    for other in all_names:
        if other != name and other.startswith(name):
            suffixes.append(other[len(name):])
    if not suffixes:
        # 无更长名字以 name 开头，字面匹配即可
        return name in text
    # 构造负向先行断言：name 后不能紧跟任何更长名字的后缀
    # 例 name=「张三」, suffixes=["丰"] → pattern = 张三(?!丰)
    lookahead = "|" .join(re.escape(s) for s in suffixes)
    pattern = re.escape(name) + r"(?!" + lookahead + r")"
    return re.search(pattern, text) is not None


def check_dormant_characters(pid):
    """扫描所有角色，找出连续 20 章未出场且未 @ 的角色。
    出场定义：在 chapters.json 任意章节正文中出现角色名（排除更长角色名前缀，避免「张三」匹配「张三丰」）。
    @ 定义：在 messages.json 或 chapter_workspace.json 的 chapter_messages 中出现 @角色名。
    返回 {dormant: [...], awakened: [...]}。
    dormant: 满足阈值条件且当前 activation_state != 'dormant'（建议休眠）。
    awakened: 当前 activation_state == 'dormant'（已休眠，供前端唤醒）。
    不修改 characters.json，仅返回建议。
    """
    characters = storage.load_data(pid, "characters.json") or []
    chapters = storage.load_data(pid, "chapters.json") or []
    messages = storage.load_data(pid, "messages.json") or []
    workspace = storage.load_data(pid, "chapter_workspace.json") or {}
    meta = storage.get_project(pid) or {}
    current_chapter = meta.get("current_chapter", 0)

    all_messages = list(messages) + list(workspace.get("chapter_messages") or [])
    # B9：收集所有角色名，供 _name_in_text 排除更长名字前缀
    all_names = [c.get("name", "") for c in characters if c.get("name")]

    dormant = []
    awakened = []
    for char in characters:
        name = char.get("name", "")
        if not name:
            continue
        state = char.get("activation_state", "active")

        last_appear = 0
        for ch in chapters:
            if _name_in_text(name, ch.get("text") or "", all_names):
                cn = ch.get("chapter", 0)
                if cn > last_appear:
                    last_appear = cn

        last_at = 0
        at_pattern = "@" + name
        for msg in all_messages:
            # @ 提及用字面匹配即可（@ 后直接跟名字）
            if at_pattern in (msg.get("text") or ""):
                cn = msg.get("chapter", 0) or 0
                if cn > last_at:
                    last_at = cn

        last_seen = max(last_appear, last_at)

        if state == "dormant":
            awakened.append({"name": name, "last_chapter": last_seen})
        elif current_chapter - last_seen >= DORMANT_THRESHOLD:
            dormant.append({"name": name, "last_chapter": last_seen})

    return {"dormant": dormant, "awakened": awakened}


def apply_dormant(pid, names):
    """把指定角色标记为 activation_state='dormant'。"""
    characters = storage.load_data(pid, "characters.json") or []
    name_set = set(names or [])
    changed = []
    for char in characters:
        if char.get("name") in name_set:
            char["activation_state"] = "dormant"
            changed.append(char["name"])
    storage.save_data(pid, "characters.json", characters)
    return {"marked_dormant": changed}


def apply_awaken(pid, names):
    """把指定角色标记为 activation_state='active'。"""
    characters = storage.load_data(pid, "characters.json") or []
    name_set = set(names or [])
    changed = []
    for char in characters:
        if char.get("name") in name_set:
            char["activation_state"] = "active"
            changed.append(char["name"])
    storage.save_data(pid, "characters.json", characters)
    return {"marked_active": changed}


def delete_character(pid, names):
    """删除指定角色（从 characters.json 移除，同时清理 relationships.json 里涉及该角色的关系）。
    焦点角色拒绝删除（避免故事失去主线）。"""
    characters = storage.load_data(pid, "characters.json") or []
    name_set = set(names or [])
    deleted = []
    rejected_focus = []
    kept = []
    for char in characters:
        name = char.get("name", "")
        if name in name_set:
            if char.get("is_focus") is True:
                # B8 修复：用 is True 而非 truthy，避免 is_focus 字段缺失（None）或
                # 字符串 "false" 等被误判，导致焦点角色被删。
                rejected_focus.append(name)
            else:
                deleted.append(name)
        else:
            kept.append(char)
    storage.save_data(pid, "characters.json", kept)
    # 清理 relationships.json 里涉及已删角色的关系
    relationships = storage.load_data(pid, "relationships.json") or []
    cleaned_rels = [r for r in relationships
                    if r.get("char_a") not in name_set and r.get("char_b") not in name_set]
    if len(cleaned_rels) != len(relationships):
        storage.save_data(pid, "relationships.json", cleaned_rels)
    # 从 meta.focus_characters 移除已删角色（非焦点角色一般不在 focus_characters，防御性清理）
    meta = storage.get_project(pid) or {}
    if meta.get("focus_characters"):
        meta["focus_characters"] = [n for n in meta["focus_characters"] if n not in name_set]
        storage.update_project(pid, meta)
    return {"deleted": deleted, "rejected_focus": rejected_focus}


def detect_crash_recovery(pid):
    """检测是否存在未完成的 chapter_workspace.json。
    返回 {recoverable: bool, workspace: summary|None, message: "..."}。
    """
    workspace = storage.load_data(pid, "chapter_workspace.json")
    if not workspace:
        return {"recoverable": False, "workspace": None, "message": "无未完成操作"}
    summary = {
        "chapter_num": workspace.get("chapter_num"),
        "round": workspace.get("round", 0),
        "word_count": workspace.get("word_count", 0),
        "messages_count": len(workspace.get("chapter_messages") or []),
    }
    msg = (f"检测到未完成操作，已恢复到最近的保存点：第 {summary['chapter_num']} 章，"
           f"轮次 {summary['round']}，字数 {summary['word_count']}")
    return {"recoverable": True, "workspace": summary, "message": msg}


def clear_crash_recovery(pid):
    """清除崩溃恢复标记（删除工作区，用户确认放弃时调用）。"""
    workspace_path = os.path.join(storage.project_dir(pid), "chapter_workspace.json")
    if os.path.isfile(workspace_path):
        os.remove(workspace_path)
    return {"cleared": True}


def check_no_key_events(pid, chapter_messages=None):
    """统计工作区中的关键事件数。
    从 chapter_workspace.json 的 pending_state_changes 提取 key_events。
    chapter_messages 参数保留以兼容签名，实际不使用（始终读工作区）。
    返回 {has_key_events, count, events, message}。
    """
    workspace = storage.load_data(pid, "chapter_workspace.json") or {}
    events = []
    for change in workspace.get("pending_state_changes") or []:
        info = change.get("info") or {}
        events.extend(info.get("key_events") or [])
    count = len(events)
    has = count > 0
    msg = (f"本章已提取 {count} 条关键事件" if has
           else "本章无关键事件推进，可能是过渡章。保留还是重写？")
    return {"has_key_events": has, "count": count, "events": events, "message": msg}


def detect_claim_conflicts(validation_result):
    """从 inspector_validate 的结果中识别宣称冲突。
    类型为"无依据宣称"的 conflict 视为宣称冲突。
    返回 [{conflict_index, ...conflict_fields}]。
    """
    conflicts = validation_result.get("conflicts") or []
    result = []
    for i, c in enumerate(conflicts):
        if c.get("type") == CLAIM_CONFLICT_TYPE:
            entry = {"conflict_index": i}
            entry.update(c)
            result.append(entry)
    return result


def resolve_claim_conflict(pid, conflict_index, resolution, details=None):
    """用户选择真相后，修改对应角色记忆字段。
    resolution: "a_lie"|"b_lie"|"both_wrong"|"other"
    details: 用户补充说明（"另有隐情"时）
    修改冲突原文中提及的角色 memory_anchors 字段（追加一条记录）。
    # 简化：不调群主AI LLM，直接追加结构化记录；升级路径：GM LLM 生成自然语言记忆条目。
    """
    valid = {"a_lie", "b_lie", "both_wrong", "other"}
    if resolution not in valid:
        raise ValueError(f"resolution 必须为 {valid} 之一")

    workspace = storage.load_data(pid, "chapter_workspace.json") or {}
    validation = workspace.get("validation") or {}
    conflicts = validation.get("conflicts") or []
    if conflict_index < 0 or conflict_index >= len(conflicts):
        raise ValueError(f"conflict_index {conflict_index} 越界")

    conflict = conflicts[conflict_index]
    original = conflict.get("original", "") or ""

    characters = storage.load_data(pid, "characters.json") or []
    mentioned = [c for c in characters if c.get("name") and c.get("name") in original]

    anchor = {
        "type": "claim_resolution",
        "resolution": resolution,
        "details": details or "",
        "conflict_original": original,
        "conflict_suggestion": conflict.get("suggestion", ""),
        "timestamp": datetime.now().isoformat(),
    }

    for char in mentioned:
        char.setdefault("memory_anchors", []).append(anchor)

    storage.save_data(pid, "characters.json", characters)
    return {
        "resolved": True,
        "affected_characters": [c.get("name") for c in mentioned],
        "anchor": anchor,
    }


def get_ignored_conflicts(pid):
    """读取用户历史忽略记录 logs/ignored_conflicts.json。无则 []。"""
    return storage.load_data(pid, IGNORED_LOG_FILE) or []


def record_ignored_conflict(pid, conflict):
    """记录用户忽略的冲突（用于历史参考）。"""
    ignored = get_ignored_conflicts(pid)
    entry = dict(conflict) if isinstance(conflict, dict) else {"raw": conflict}
    entry["ignored_at"] = datetime.now().isoformat()
    ignored.append(entry)
    storage.save_data(pid, IGNORED_LOG_FILE, ignored)
    return {"recorded": True, "total": len(ignored)}


def _tokenize(text):
    """提取 2 字符子串作为关键词。
    # 启发式：中文无分词，用 2-gram 重叠；升级路径：jieba 分词 + 向量相似度。
    """
    if not text:
        return []
    tokens = []
    for segment in re.findall(r'[\w]+', text, flags=re.UNICODE):
        if len(segment) < 2:
            continue
        if len(segment) == 2:
            tokens.append(segment)
        else:
            for i in range(len(segment) - 1):
                tokens.append(segment[i:i + 2])
    return tokens


def find_similar_ignored(pid, conflict):
    """查询历史忽略记录中是否有同类型冲突。
    返回 [类似记录...] 或 []。
    # 启发式：相同 type + 2-gram 关键词重叠；升级路径：向量相似度。
    """
    ignored = get_ignored_conflicts(pid)
    if not ignored:
        return []
    ctype = conflict.get("type", "")
    cur_text = (conflict.get("original", "") or "") + " " + (conflict.get("suggestion", "") or "")
    cur_tokens = set(_tokenize(cur_text))
    if not cur_tokens:
        return []
    similar = []
    for ign in ignored:
        if ign.get("type", "") != ctype:
            continue
        ign_text = (ign.get("original", "") or "") + " " + (ign.get("suggestion", "") or "")
        ign_tokens = set(_tokenize(ign_text))
        if cur_tokens & ign_tokens:
            similar.append(ign)
    return similar


# ---------- 自检：纯代码逻辑校验（无 LLM、无 fixture、无框架） ----------

if __name__ == "__main__":
    # 1. check_dormant_characters：25 章，张三只在章 5 出现 → 建议休眠
    meta = storage.create_project("assistant 自检", "测试")
    pid = meta["id"]
    try:
        characters = [
            {"name": "张三", "activation_state": "active"},
            {"name": "李四", "activation_state": "active"},   # 章 25 出现，不满足阈值
            {"name": "王五", "activation_state": "dormant"},  # 已休眠
        ]
        storage.save_data(pid, "characters.json", characters)
        chapters = []
        for i in range(1, 26):
            text = ""
            if i == 5:
                text = "张三出场。"
            if i == 25:
                text = "李四出场。"
            chapters.append({"chapter": i, "text": text})
        storage.save_data(pid, "chapters.json", chapters)
        meta["current_chapter"] = 25
        storage.update_project(pid, meta)

        r = check_dormant_characters(pid)
        dn = [d["name"] for d in r["dormant"]]
        an = [a["name"] for a in r["awakened"]]
        assert "张三" in dn, r        # 25-5=20 ≥ 20
        assert "李四" not in dn, r    # 25-25=0 < 20
        assert "王五" in an, r         # 已 dormant
        assert r["dormant"][0]["last_chapter"] == 5, r

        # 1.1 apply_dormant / apply_awaken
        r = apply_dormant(pid, ["张三"])
        assert r["marked_dormant"] == ["张三"], r
        zs = next(c for c in storage.load_data(pid, "characters.json") if c["name"] == "张三")
        assert zs["activation_state"] == "dormant", zs

        r = apply_awaken(pid, ["王五"])
        assert r["marked_active"] == ["王五"], r
        ww = next(c for c in storage.load_data(pid, "characters.json") if c["name"] == "王五")
        assert ww["activation_state"] == "active", ww

        # 2. detect_crash_recovery：无工作区 False；有工作区 True
        ws_path = os.path.join(storage.project_dir(pid), "chapter_workspace.json")
        if os.path.isfile(ws_path):
            os.remove(ws_path)
        r = detect_crash_recovery(pid)
        assert r["recoverable"] is False, r
        assert r["workspace"] is None, r

        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": 3, "round": 2, "word_count": 500,
            "chapter_messages": [{"speaker": "张三", "text": "你好"}],
        })
        r = detect_crash_recovery(pid)
        assert r["recoverable"] is True, r
        assert r["workspace"]["chapter_num"] == 3, r
        assert "第 3 章" in r["message"], r

        # clear_crash_recovery
        r = clear_crash_recovery(pid)
        assert r["cleared"] is True
        assert detect_crash_recovery(pid)["recoverable"] is False

        # 3. check_no_key_events：无 key_events count=0；有 2 条 count=2
        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": 1,
            "pending_state_changes": [
                {"character": "张三", "info": {"key_events": []}},
            ],
        })
        r = check_no_key_events(pid)
        assert r["count"] == 0 and r["has_key_events"] is False, r
        assert "过渡章" in r["message"], r

        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": 1,
            "pending_state_changes": [
                {"character": "张三", "info": {"key_events": [
                    {"type": "战斗", "description": "张三与刺客交手"}]}},
                {"character": "李四", "info": {"key_events": [
                    {"type": "发现", "description": "李四发现密信"}]}},
            ],
        })
        r = check_no_key_events(pid)
        assert r["count"] == 2 and r["has_key_events"] is True, r

        # 4. detect_claim_conflicts：识别"无依据宣称"类型
        validation = {
            "conflicts": [
                {"type": "性格冲突", "location": "段1", "original": "张三愤怒", "suggestion": "应平静"},
                {"type": "无依据宣称", "location": "段2", "original": "李四宣称曾告诉王五", "suggestion": "王五否认"},
                {"type": "规则矛盾", "location": "段3", "original": "违反规则", "suggestion": "修改"},
            ],
            "passed": "",
        }
        claims = detect_claim_conflicts(validation)
        assert len(claims) == 1, claims
        assert claims[0]["conflict_index"] == 1, claims
        assert claims[0]["type"] == "无依据宣称", claims

        # 5. resolve_claim_conflict：修改 memory_anchors
        storage.save_data(pid, "chapter_workspace.json", {
            "chapter_num": 1,
            "validation": validation,
        })
        storage.save_data(pid, "characters.json", [
            {"name": "李四", "activation_state": "active", "memory_anchors": []},
            {"name": "王五", "activation_state": "active", "memory_anchors": []},
            {"name": "赵六", "activation_state": "active", "memory_anchors": []},
        ])
        r = resolve_claim_conflict(pid, 1, "b_lie", details="王五否认")
        assert r["resolved"] is True, r
        assert set(r["affected_characters"]) == {"李四", "王五"}, r
        chars = storage.load_data(pid, "characters.json")
        ls = next(c for c in chars if c["name"] == "李四")
        ws = next(c for c in chars if c["name"] == "王五")
        zl = next(c for c in chars if c["name"] == "赵六")
        assert len(ls["memory_anchors"]) == 1, ls
        assert len(ws["memory_anchors"]) == 1, ws
        assert len(zl.get("memory_anchors", [])) == 0, zl
        assert ls["memory_anchors"][0]["resolution"] == "b_lie", ls
        assert ls["memory_anchors"][0]["details"] == "王五否认", ls

        # 非法 resolution 抛异常
        try:
            resolve_claim_conflict(pid, 1, "invalid")
            assert False, "应抛异常"
        except ValueError:
            pass

        # 6. find_similar_ignored：同类型 + 2-gram 重叠
        storage.save_data(pid, IGNORED_LOG_FILE, [])
        record_ignored_conflict(pid, {
            "type": "无依据宣称",
            "original": "李四宣称曾告诉王五秘密",
            "suggestion": "王五否认",
        })
        # 同类型 + 关键词重叠（"李四"、"王五"、"告诉"等 2-gram）
        current = {
            "type": "无依据宣称",
            "original": "李四又说曾告诉王五",
            "suggestion": "王五再次否认",
        }
        similar = find_similar_ignored(pid, current)
        assert len(similar) == 1, similar
        assert similar[0]["type"] == "无依据宣称", similar

        # 不同类型 → 不匹配
        different = {"type": "性格冲突", "original": "李四性格", "suggestion": "王五建议"}
        assert find_similar_ignored(pid, different) == []

        # 同类型但无 2-gram 重叠 → 不匹配
        no_overlap = {"type": "无依据宣称", "original": "赵六完全不同的事", "suggestion": "孙七"}
        assert find_similar_ignored(pid, no_overlap) == []

        # 空忽略记录 → []
        meta2 = storage.create_project("assistant 自检2", "测试")
        pid2 = meta2["id"]
        try:
            assert find_similar_ignored(pid2, current) == []
        finally:
            storage.delete_project(pid2)

        print("ALL CHECKS PASSED")
    finally:
        storage.delete_project(pid)
