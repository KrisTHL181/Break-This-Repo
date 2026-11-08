"""4 个 Agent 模块：角色AI / 群主AI / 检察员 / 叙事者。函数式风格，单文件。
对话AI 已在 dialogue_agent.py，此处不重复。LLM 调用统一走 llm.chat。"""
import json
import re

import llm
import sse_utils

# 兼容旧调用点：parse_json 重导出
parse_json = sse_utils.parse_json


# ---------- 角色AI ----------

CHARACTER_PROMPT = """你正在扮演小说中的一个角色。根据提供的角色卡、感知名单、上下文，输出该角色本轮的行为。
输出格式要求：
- 可以是对话、动作、内心独白、情绪反应
- 若需要环境描写，在末尾追加 【唤叙事者：描写指令】（例如：【唤叙事者：描写酒馆嘈杂的气氛】）
- 若要指定下一句接话角色，用 @角色名（例如：@李四 你怎么看？）
- 若该角色本轮选择沉默，只输出"沉默"二字
- 焦点角色可自由展现内心想法；非焦点角色不要写内心独白
只输出角色行为文本，不要解释、不要 JSON。"""

_NARRATOR_CALL_RE = re.compile(r'【唤叙事者：(.+?)】')
_AT_MENTION_RE = re.compile(r'@(\S+)')


def build_context_pack(character, perception_list, recent_messages, public_messages, conditions_summary, is_focus):
    """组装角色上下文包（7 项：系统指令 + 角色卡 + 感知名单 + 最近10次发言 + 公共池30条 + 完本条件标题 + 焦点标记）。"""
    return {
        "system_prompt": CHARACTER_PROMPT,
        "character": character,
        "perception_list": perception_list,
        "recent_messages": list(recent_messages)[-10:],
        "public_messages": list(public_messages)[-30:],
        "conditions_summary": conditions_summary,
        "is_focus": is_focus,
    }


def filter_perception_list(all_characters, target_character, relationships):
    """3 条过滤规则：
    1. exposure_status="hidden" 不出现在他人名单，除非目标角色 hidden_fields 含侦查类能力
    2. 未在 relationships 中建立互相知晓关系的角色互不可见
    3. current_emotion in ["昏迷","沉睡","死亡"] 不出现在任何名单
    返回可见角色名列表。
    # 侦查能力启发式：hidden_fields 值中含这些字样即视为穿透 hidden
    # 上限：仅中文关键词字面匹配；升级路径：换成 hidden_fields.recon: bool 显式字段
    """
    target_name = target_character.get("name")
    hidden_fields = target_character.get("hidden_fields") or {}
    recon_keywords = ("鹰眼", "侦查", "侦察", "洞察")
    has_recon = isinstance(hidden_fields, dict) and any(
        any(k in str(v) for k in recon_keywords) for v in hidden_fields.values()
    )
    rel_pairs = set()
    for r in relationships or []:
        a, b = r.get("char_a"), r.get("char_b")
        if a and b:
            rel_pairs.add((a, b))
            rel_pairs.add((b, a))
    blocked_emotions = {"昏迷", "沉睡", "死亡"}
    visible = []
    for c in all_characters:
        name = c.get("name")
        if not name or name == target_name:
            continue
        if c.get("current_emotion") in blocked_emotions:
            continue
        if c.get("exposure_status") == "hidden" and not has_recon:
            continue
        if (target_name, name) not in rel_pairs:
            continue
        visible.append(name)
    return visible


def character_speak(config, context_pack, prompt_override=None):
    """调用 LLM 输出角色行为原始文本。prompt_override 来自 storage.load_prompt(pid, 'character')。"""
    messages = [
        {"role": "system", "content": prompt_override or CHARACTER_PROMPT},
        {"role": "user", "content": json.dumps(context_pack, ensure_ascii=False, indent=2)},
    ]
    return llm.chat(config, messages)


def parse_character_output(raw):
    """解析角色 LLM 输出：唤叙事者 / @提及 / 沉默标记。
    返回 {raw, cleaned, narrator_calls, at_mentions, is_silent}。"""
    narrator_calls = _NARRATOR_CALL_RE.findall(raw)
    at_mentions = _AT_MENTION_RE.findall(raw)
    cleaned = _NARRATOR_CALL_RE.sub(' ', raw)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    is_silent = not cleaned or cleaned in ("沉默", "（沉默）")
    return {
        "raw": raw,
        "cleaned": cleaned,
        "narrator_calls": narrator_calls,
        "at_mentions": at_mentions,
        "is_silent": is_silent,
    }


# ---------- 群主AI ----------

GM_EDIT_PROMPT = """你是小说修改执行者。根据修改指令，对章节正文执行精确修改。
输入：修改指令（JSON）+ 原文
输出 JSON：
{
  "new_text": "修改后的完整正文",
  "operations": [{"type":"replace|insert|delete","position":"段落号","before":"原文本片段","after":"新文本片段"}],
  "transition_added": "自动补充的过渡句（若有，否则空字符串）",
  "affected_ranges": ["受影响波及的段落号列表"]
}
要求：
- 修改后检查前后句衔接，不通顺则自动补过渡句
- 扫描本章后续段落是否引用被改内容，若有则同步微调
- 只返回 JSON"""


def execute_edit(config, instruction, chapter_text, prompt_override=None):
    """群主AI 执行修改：返回 {new_text, operations, transition_added, affected_ranges}。
    instruction 结构：{target, position, operation, content, scope}。"""
    user_content = json.dumps(
        {"instruction": instruction, "chapter_text": chapter_text},
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or GM_EDIT_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


def protect_finalized(config, instruction, is_finalized):
    """已定稿章节保护：is_finalized=True 且 instruction.target 为空时仅提醒不修改；
    否则返回 None（允许修改）。config 保留以保持调用签名一致。"""
    if is_finalized and not instruction.get("target"):
        return {"warning": "此为已定稿章节，仅标注提醒，不自动修改"}
    return None


# ---------- 检察员（4 模式） ----------

INSPECTOR_VALIDATE_PROMPT = """你是小说校验员（校验模式）。检查本章正文是否存在以下问题：
1. 角色行为或对话明显违背其角色卡性格描述
2. 角色宣称"过去做过某事"但角色卡/历史记录中无依据
3. 与世界规则文本明显矛盾的描写
输出 JSON：
{
  "conflicts": [
    {"type":"性格冲突|无依据宣称|规则矛盾","location":"位置描述","original":"原文片段","suggestion":"修改建议"}
  ],
  "passed": "通过条目摘要"
}
只返回 JSON。"""

INSPECTOR_ARBITRATE_PROMPT = """你是小说校验员（仲裁模式）。两个角色输出了矛盾的事实。
根据双方角色卡和世界规则，裁定最终事实版本。
输出 JSON：{"final_fact": "裁定后的最终事实描述", "reason": "裁定依据"}
只返回 JSON。"""

INSPECTOR_JUDGE_PROMPT = """你是小说校验员（判定模式）。判断本章是否达成了任意完本条件。
输入：本章摘要 + 未达成完本条件列表
输出 JSON：
{
  "achieved": [
    {"id": "条件id", "title": "条件标题", "evidence": "本章摘要中的依据"}
  ]
}
若本章未达成任何条件，achieved 为空数组。只返回 JSON。"""


def inspector_validate(config, chapter_text, characters, world_rules, prompt_override=None):
    """校验模式：返回 {conflicts, passed}。"""
    user_content = json.dumps(
        {"chapter_text": chapter_text, "characters": characters, "world_rules": world_rules},
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or INSPECTOR_VALIDATE_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


def inspector_arbitrate(config, output_a, output_b, char_a, char_b, world_rules, prompt_override=None):
    """仲裁模式：返回 {final_fact, reason}。"""
    user_content = json.dumps(
        {
            "output_a": output_a, "char_a": char_a,
            "output_b": output_b, "char_b": char_b,
            "world_rules": world_rules,
        },
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or INSPECTOR_ARBITRATE_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


def inspector_judge(config, chapter_summary, unachieved_conditions, prompt_override=None):
    """判定模式：返回 {achieved: [...]}。"""
    user_content = json.dumps(
        {"chapter_summary": chapter_summary, "unachieved_conditions": unachieved_conditions},
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or INSPECTOR_JUDGE_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


def inspector_detect(conditions):
    """检测模式（纯代码，不调 LLM）：返回 {all_achieved, unachieved, signal}。
    conditions 兼容 list 或 dict（按 id 索引，因 storage 默认 {}）。"""
    if isinstance(conditions, dict):
        items = list(conditions.values())
    else:
        items = list(conditions or [])
    unachieved = [c for c in items if not c.get("achieved")]
    all_achieved = bool(items) and not unachieved
    return {
        "all_achieved": all_achieved,
        "unachieved": unachieved,
        "signal": "可完本" if all_achieved else None,
    }


# ---------- 叙事者 ----------

NARRATOR_DESCRIBE_PROMPT = """你是小说叙事者。根据指令输出一段环境/气氛描写。只输出描写文本，不要解释。"""
NARRATOR_TRANSITION_PROMPT = """你是小说叙事者。生成一句场景/焦点切换的过渡段。只输出过渡文本。"""
NARRATOR_SUMMARIZE_PROMPT = """你是小说叙事者。将本章正文压缩为约300字的摘要。只输出摘要文本。"""
NARRATOR_FINALE_PROMPT = """你是小说叙事者。生成全书完结总结。
输出 JSON：
{
  "total_word_count": 数字,
  "character_fates": [{"name":"角色名","fate":"命运总结"}],
  "foreshadow_recap": [{"foreshadow":"伏笔","status":"已回收|未回收"}],
  "timeline": [{"chapter":章号,"event":"关键事件"}]
}
只返回 JSON。"""


def narrator_describe(config, instruction, messages_summary, prompt_override=None):
    """叙事者：环境/气氛描写。"""
    user_content = json.dumps(
        {"instruction": instruction, "messages_summary": messages_summary},
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or NARRATOR_DESCRIBE_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return llm.chat(config, messages)


def narrator_transition(config, from_scene, to_scene, prompt_override=None):
    """叙事者：场景/焦点切换过渡段。"""
    user_content = json.dumps(
        {"from_scene": from_scene, "to_scene": to_scene},
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or NARRATOR_TRANSITION_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return llm.chat(config, messages)


def narrator_summarize(config, chapter_text, prompt_override=None):
    """叙事者：本章约 300 字摘要。"""
    messages = [
        {"role": "system", "content": prompt_override or NARRATOR_SUMMARIZE_PROMPT},
        {"role": "user", "content": chapter_text},
    ]
    return llm.chat(config, messages)


def narrator_finale(config, chapters_summary, characters, events, foreshadows, prompt_override=None):
    """叙事者：全书完结总结，返回 JSON。"""
    user_content = json.dumps(
        {
            "chapters_summary": chapters_summary,
            "characters": characters,
            "events": events,
            "foreshadows": foreshadows,
        },
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or NARRATOR_FINALE_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


# ---------- 信息提取工具（供推演循环用） ----------

EXTRACT_PROMPT = """你是信息提取器。从角色输出文本中提取以下信息。
输出 JSON：
{
  "location_change": {"character":"角色名","new_location":"新场景名"} 或 null,
  "new_characters": [{"name":"新角色名","description":"描述"}] 或 [],
  "emotion_changes": [{"character":"角色名","new_emotion":"新情绪"}] 或 [],
  "items_gained": [{"character":"角色名","item":"物品"}] 或 [],
  "key_events": [{"type":"战斗|发现|对话冲突|转折|死亡|离别|表白","description":"事件描述"}] 或 []
}
只返回 JSON。"""


def extract_info(config, character_name, character_output, current_characters, prompt_override=None):
    """从角色输出中提取状态变更信息，返回 JSON 结构。"""
    user_content = json.dumps(
        {
            "character_name": character_name,
            "character_output": character_output,
            "current_characters": current_characters,
        },
        ensure_ascii=False, indent=2,
    )
    messages = [
        {"role": "system", "content": prompt_override or EXTRACT_PROMPT},
        {"role": "user", "content": user_content},
    ]
    return parse_json(llm.chat(config, messages))


# ---------- 段落切片工具（供 finalization / editing 复用） ----------

def slice_affected_paragraphs(new_text, affected_ranges):
    """按 affected_ranges 切出受影响段落。无具体段落号则返回全文。
    启发式：抽取字符串中第一个数字作为段落号；上限：纯字面匹配。"""
    paragraphs = new_text.split("\n\n")
    affected_indices = set()
    for r in (affected_ranges or []):
        digits = "".join(ch for ch in str(r) if ch.isdigit())
        if digits:
            i = int(digits) - 1
            if 0 <= i < len(paragraphs):
                affected_indices.add(i)
    if not affected_indices:
        return new_text
    return "\n\n".join(paragraphs[i] for i in sorted(affected_indices))


# ---------- 自检：纯代码函数行为校验（无 LLM、无 fixture、无框架） ----------

if __name__ == "__main__":
    # 1. parse_character_output：唤叙事者 + @ + cleaned
    r = parse_character_output("张三说：'你好'【唤叙事者：描写酒馆】@李四")
    assert r["narrator_calls"] == ["描写酒馆"], r
    assert r["at_mentions"] == ["李四"], r
    assert r["cleaned"] == "张三说：'你好' @李四", r
    assert r["is_silent"] is False, r

    # 2. 沉默标记三种形态
    assert parse_character_output("沉默")["is_silent"] is True
    assert parse_character_output("（沉默）")["is_silent"] is True
    assert parse_character_output("")["is_silent"] is True

    # 3. filter_perception_list 3 条规则
    chars = [
        {"name": "张三", "current_emotion": "平静", "exposure_status": "exposed"},
        {"name": "李四", "current_emotion": "平静", "exposure_status": "hidden"},   # 规则1
        {"name": "王五", "current_emotion": "昏迷", "exposure_status": "exposed"},  # 规则3
        {"name": "赵六", "current_emotion": "平静", "exposure_status": "exposed"},
    ]
    target = {"name": "张三", "current_emotion": "平静", "exposure_status": "exposed", "hidden_fields": {}}
    rels = [{"char_a": "张三", "char_b": "赵六"}]  # 张三只与赵六建立关系
    visible = filter_perception_list(chars, target, rels)
    assert visible == ["赵六"], visible  # 李四 hidden、王五 昏迷、赵六 关系+暴露

    # 3.1 鹰眼穿透 hidden，但李四仍因无关系被排除
    target_recon = dict(target, hidden_fields={"skill": "鹰眼"})
    assert filter_perception_list(chars, target_recon, rels) == ["赵六"]

    # 3.2 鹰眼 + 与李四建立关系 → 李四可见
    rels2 = [{"char_a": "张三", "char_b": "李四"}, {"char_a": "张三", "char_b": "赵六"}]
    assert set(filter_perception_list(chars, target_recon, rels2)) == {"李四", "赵六"}

    # 4. inspector_detect：部分达成
    conds = [
        {"id": "c1", "title": "条件1", "achieved": True, "achieved_chapter": 1},
        {"id": "c2", "title": "条件2", "achieved": False, "achieved_chapter": None},
    ]
    d = inspector_detect(conds)
    assert d["all_achieved"] is False and d["signal"] is None and len(d["unachieved"]) == 1, d

    # 5. inspector_detect：全达成 → signal="可完本"
    conds_all = [
        {"id": "c1", "title": "条件1", "achieved": True, "achieved_chapter": 1},
        {"id": "c2", "title": "条件2", "achieved": True, "achieved_chapter": 2},
    ]
    d2 = inspector_detect(conds_all)
    assert d2["all_achieved"] is True and d2["signal"] == "可完本", d2

    # 5.1 inspector_detect 兼容 dict 形态
    d3 = inspector_detect({c["id"]: c for c in conds_all})
    assert d3["signal"] == "可完本", d3

    # 6. 所有 Prompt 非空
    for name in ("CHARACTER_PROMPT", "GM_EDIT_PROMPT", "INSPECTOR_VALIDATE_PROMPT",
                 "INSPECTOR_ARBITRATE_PROMPT", "INSPECTOR_JUDGE_PROMPT",
                 "NARRATOR_DESCRIBE_PROMPT", "NARRATOR_TRANSITION_PROMPT",
                 "NARRATOR_SUMMARIZE_PROMPT", "NARRATOR_FINALE_PROMPT", "EXTRACT_PROMPT"):
        assert globals()[name].strip(), f"{name} 为空"

    # 7. protect_finalized
    assert protect_finalized(None, {"target": ""}, True) is not None
    assert protect_finalized(None, {"target": "ch3"}, True) is None
    assert protect_finalized(None, {}, False) is None

    # 8. build_context_pack：7 项 + 截断到 10/30
    pack = build_context_pack(
        character={"name": "张三"},
        perception_list=["李四"],
        recent_messages=list(range(20)),
        public_messages=list(range(50)),
        conditions_summary="条件1",
        is_focus=True,
    )
    assert len(pack) == 7, pack
    assert len(pack["recent_messages"]) == 10 and len(pack["public_messages"]) == 30
    assert pack["is_focus"] is True

    # 9. slice_affected_paragraphs：无段落号→全文；有段落号→切片；越界→忽略
    text = "段一\n\n段二\n\n段三"
    assert slice_affected_paragraphs(text, []) == text  # 无段落号 → 全文兜底
    assert slice_affected_paragraphs(text, ["2"]) == "段二"  # 单段
    assert slice_affected_paragraphs(text, ["1", "3"]) == "段一\n\n段三"  # 多段排序
    assert slice_affected_paragraphs(text, ["99"]) == text  # 越界 → 全文兜底
    assert slice_affected_paragraphs(text, ["段2有改动"]) == "段二"  # 启发式抽数字

    print("ALL CHECKS PASSED")
