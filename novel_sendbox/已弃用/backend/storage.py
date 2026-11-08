import json
import os
import shutil
import tempfile
import threading
import uuid
from datetime import datetime

PROJECTS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "projects")
LLM_AGENT_KEYS = ("dialogue", "character", "gm", "inspector", "narrator")

# LLM 配置内存缓存（前端启动时推送，不持久化到文件，防止 api_key 泄漏）
# 结构：{"providers": [{id, name, provider, model, api_key, base_url, temperature, max_tokens}],
#        "bindings": {agent_key: provider_id}}
# B1 修复：加锁保护，防止 set_in_memory_llm_config 写入过程中 load_llm_config 读到
# providers 已更新但 bindings 未更新的中间态。
_IN_MEMORY_LLM = {"providers": [], "bindings": {}}
_LLM_LOCK = threading.Lock()

# B7/B3 修复：每 pid 一把锁，保护涉及 read-modify-write 的关键路径
# （chapters.json/characters.json/meta.json 等），防止并发覆盖。
# 上限：单机单用户，并发极低；锁粒度按 pid 即可。升级路径：带 TTL 的锁字典。
_PID_LOCKS = {}
_PID_LOCKS_GUARD = threading.Lock()


def get_pid_lock(pid):
    """获取（或创建）指定 pid 的锁。供 finalization/editing 等模块的
    read-modify-write 序列使用。"""
    with _PID_LOCKS_GUARD:
        lock = _PID_LOCKS.get(pid)
        if lock is None:
            lock = threading.Lock()
            _PID_LOCKS[pid] = lock
        return lock

# 项目内所有数据 JSON 文件及其空默认值
DATA_FILES = {
    "world_rules.json": {},
    "characters.json": [],
    "chapters.json": [],
    "messages.json": [],
    "events.json": [],
    "foreshadows.json": [],
    "completion_conditions.json": {},
    "scenes.json": [],
    "relationships.json": [],
    "settings.json": {},
}


def _project_dir(pid):
    return os.path.join(PROJECTS_DIR, pid)


# L5 修复：公开接口供其他模块使用，避免直接访问 _project_dir 破坏封装
def project_dir(pid):
    return _project_dir(pid)


def _meta_path(pid):
    return os.path.join(_project_dir(pid), "meta.json")


def _read_json(path, default=None):
    """H2 修复：JSON 解析失败时返回 default，避免单文件损坏导致整个项目接口 500。
    覆盖场景：并发写截断、磁盘故障、用户手动编辑出错。"""
    if not os.path.exists(path):
        return default
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError) as e:
        # 损坏文件不删，让用户可手动恢复；调用方拿到 default 继续工作
        print(f"[storage] 警告：{path} JSON 解析失败，使用默认值。原因: {e}")
        return default


def _write_json(path, data):
    """A4 修复：原子写入（tempfile + os.replace），避免并发写或崩溃导致文件被截断为空/部分 JSON。
    跨平台兼容：os.replace 在 Windows/Linux 均为原子操作（同盘内）。"""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    target_dir = os.path.dirname(path)
    # 在同目录创建临时文件（os.replace 要求同盘）
    fd, tmp_path = tempfile.mkstemp(dir=target_dir, suffix=".tmp", prefix=".write_")
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp_path, path)
    except Exception:
        # 写失败：清理临时文件，原文件未被覆盖
        if os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except OSError:
                pass
        raise


def load_data(pid, filename):
    """读取项目内任意 JSON 数据文件"""
    return _read_json(os.path.join(_project_dir(pid), filename))


def save_data(pid, filename, data):
    """写入项目内任意 JSON 数据文件。data 为 None 时删除文件。"""
    path = os.path.join(_project_dir(pid), filename)
    if data is None:
        if os.path.isfile(path):
            os.remove(path)
    else:
        _write_json(path, data)


def delete_data(pid, filename):
    """删除项目内数据文件。无副作用。"""
    path = os.path.join(_project_dir(pid), filename)
    if os.path.isfile(path):
        os.remove(path)


def list_projects():
    if not os.path.isdir(PROJECTS_DIR):
        return []
    result = []
    for name in os.listdir(PROJECTS_DIR):
        meta_file = _meta_path(name)
        if os.path.isfile(meta_file):
            result.append(_read_json(meta_file))
    return result


def create_project(title, genre):
    pid = uuid.uuid4().hex
    pdir = _project_dir(pid)
    os.makedirs(pdir, exist_ok=True)
    os.makedirs(os.path.join(pdir, "snapshots"), exist_ok=True)
    os.makedirs(os.path.join(pdir, "logs"), exist_ok=True)
    meta = {
        "id": pid,
        "title": title,
        "genre": genre,
        "created_at": datetime.now().isoformat(),
        "current_chapter": 0,
        "current_scene": "",
        "focus_characters": [],
        "multithread": False,
        "word_count_target": {"min": 2000, "max": 3000},
        "foreshadows_enabled": False,
    }
    _write_json(_meta_path(pid), meta)
    for fname, default in DATA_FILES.items():
        _write_json(os.path.join(pdir, fname), default)
    return meta


def get_project(pid):
    meta_file = _meta_path(pid)
    if not os.path.isfile(meta_file):
        return None
    return _read_json(meta_file)


def update_project(pid, meta):
    pdir = _project_dir(pid)
    if not os.path.isdir(pdir):
        return None
    meta["id"] = pid
    _write_json(_meta_path(pid), meta)
    return meta


def delete_project(pid):
    pdir = _project_dir(pid)
    if not os.path.isdir(pdir):
        return False
    shutil.rmtree(pdir)
    # 清理内存泄漏：锁、prompt 缓存、活动章节状态
    with _PID_LOCKS_GUARD:
        _PID_LOCKS.pop(pid, None)
    with _PROMPT_CACHE_LOCK:
        for k in list(_PROMPT_CACHE):
            if k[0] == (pid or ""):
                _PROMPT_CACHE.pop(k, None)
    # 清理 deduction 模块的活动状态
    try:
        import deduction
        deduction.clear_active_chapter(pid)
    except ImportError:
        pass
    return True


def list_snapshots(pid):
    sdir = os.path.join(_project_dir(pid), "snapshots")
    if not os.path.isdir(sdir):
        return []
    return [d for d in os.listdir(sdir) if os.path.isdir(os.path.join(sdir, d))]


def save_snapshot(pid, version):
    pdir = _project_dir(pid)
    sdir = os.path.join(pdir, "snapshots", version)
    if os.path.isdir(sdir):
        shutil.rmtree(sdir)
    os.makedirs(sdir, exist_ok=True)
    for fname in ["meta.json"] + list(DATA_FILES.keys()):
        src = os.path.join(pdir, fname)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(sdir, fname))
    return version


def restore_snapshot(pid, version):
    pdir = _project_dir(pid)
    sdir = os.path.join(pdir, "snapshots", version)
    if not os.path.isdir(sdir):
        return None
    for fname in ["meta.json"] + list(DATA_FILES.keys()):
        src = os.path.join(sdir, fname)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(pdir, fname))
    return get_project(pid)


def reset_data_files(pid, exclude=None):
    """重置项目内数据文件为默认空值（保留 exclude 列表中的文件不重置）。
    不动 meta.json / snapshots/ / logs/。"""
    exclude = set(exclude or [])
    pdir = _project_dir(pid)
    for fname, default in DATA_FILES.items():
        if fname in exclude:
            continue
        _write_json(os.path.join(pdir, fname), default)
    # 顺带清理推演中间产物（不属于 DATA_FILES，需手动处理）
    for fname in ("chapter_workspace.json",):
        if fname in exclude:
            continue
        wp = os.path.join(pdir, fname)
        if os.path.isfile(wp):
            os.remove(wp)
    # 清理场景工作区（scene_workspace_*.json）
    if "scene_workspaces" not in exclude:
        for name in os.listdir(pdir):
            if name.startswith("scene_workspace_") and name.endswith(".json"):
                os.remove(os.path.join(pdir, name))
    return list(DATA_FILES.keys())


def _default_llm_config():
    return {
        k: {"provider": "openai", "model": "", "api_key": "", "base_url": ""}
        for k in LLM_AGENT_KEYS
    }


def load_llm_config():
    """从内存读 LLM 配置，返回 {agent_key: config} 格式（向后兼容现有调用点）。
    内存中是 {providers, bindings}，按 bindings 映射出每 agent 一份配置。
    B1 修复：持锁读取，避免读到 providers/bindings 不一致的中间态。"""
    with _LLM_LOCK:
        providers = _IN_MEMORY_LLM.get("providers", [])
        bindings = _IN_MEMORY_LLM.get("bindings", {})
    by_id = {p.get("id"): p for p in providers if isinstance(p, dict)}
    result = {}
    for k in LLM_AGENT_KEYS:
        pid = bindings.get(k)
        p = by_id.get(pid) if pid else None
        if isinstance(p, dict) and p.get("api_key"):
            result[k] = {
                "provider": p.get("provider", "openai"),
                "model": p.get("model", ""),
                "api_key": p.get("api_key", ""),
                "base_url": p.get("base_url", ""),
                "temperature": p.get("temperature", 0.7),
                "max_tokens": p.get("max_tokens", 4096),
            }
        else:
            result[k] = {"provider": "openai", "model": "", "api_key": "", "base_url": ""}
    return result


def set_in_memory_llm_config(providers, bindings):
    """前端推送的解密配置存内存（进程生命周期内有效，重启需重新推送）。
    B1 修复：持锁整体替换，保证 readers 看到一致的 providers+bindings。"""
    with _LLM_LOCK:
        _IN_MEMORY_LLM["providers"] = providers if isinstance(providers, list) else []
        _IN_MEMORY_LLM["bindings"] = bindings if isinstance(bindings, dict) else {}


def get_in_memory_llm_config():
    """返回内存中的原始结构（脱敏用：调用方负责去掉 api_key）。
    B1 修复：持锁返回浅拷贝，避免调用方在锁外读到正在被改的字典。"""
    with _LLM_LOCK:
        return {
            "providers": list(_IN_MEMORY_LLM.get("providers", [])),
            "bindings": dict(_IN_MEMORY_LLM.get("bindings", {})),
        }


# ---------- 对话历史持久化 ----------
# 对话历史不属于 DATA_FILES（核心数据），作为辅助数据单独管理。
DIALOGUE_HISTORY_FILE = "dialogue_history.json"


def load_dialogue_history(pid):
    """读取对话历史。返回 list[message]，缺失或损坏返回 []。"""
    h = _read_json(os.path.join(_project_dir(pid), DIALOGUE_HISTORY_FILE), default=[])
    return h if isinstance(h, list) else []


def save_dialogue_history(pid, history):
    """覆写对话历史。history 必须是 list。"""
    _write_json(os.path.join(_project_dir(pid), DIALOGUE_HISTORY_FILE), history or [])


def clear_dialogue_history(pid):
    """清空对话历史。"""
    save_dialogue_history(pid, [])


# ---------- 自定义提示词 ----------
# 分层优先级：项目 prompts.json > 全局 prompts_global.json > 代码默认常量
# 11 个 key，每个 agent 函数独立一份 prompt，完全替换式覆盖该函数的 system_prompt。
# narrator 拆 4 份：describe/transition/summarize/finale；
# inspector 拆 4 份：validate/arbitrate/judge/extract。

GLOBAL_PROMPTS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "prompts_global.json")
PROJECT_PROMPTS_FILE = "prompts.json"
PROMPT_AGENT_KEYS = (
    "dialogue", "character", "gm",
    "inspector_validate", "inspector_arbitrate", "inspector_judge", "inspector_extract",
    "narrator_describe", "narrator_transition", "narrator_summarize", "narrator_finale",
)


def _default_prompts():
    """延迟导入 agents 和 dialogue_agent，返回 11 个 key 的默认 prompt 字符串。"""
    import agents
    import dialogue_agent
    return {
        "dialogue": dialogue_agent.DIALOGUE_PROMPT,
        "character": agents.CHARACTER_PROMPT,
        "gm": agents.GM_EDIT_PROMPT,
        "inspector_validate": agents.INSPECTOR_VALIDATE_PROMPT,
        "inspector_arbitrate": agents.INSPECTOR_ARBITRATE_PROMPT,
        "inspector_judge": agents.INSPECTOR_JUDGE_PROMPT,
        "inspector_extract": agents.EXTRACT_PROMPT,
        "narrator_describe": agents.NARRATOR_DESCRIBE_PROMPT,
        "narrator_transition": agents.NARRATOR_TRANSITION_PROMPT,
        "narrator_summarize": agents.NARRATOR_SUMMARIZE_PROMPT,
        "narrator_finale": agents.NARRATOR_FINALE_PROMPT,
    }


# B12 修复：load_prompt 缓存。按 (pid, key) 缓存解析结果，避免每次调用都触发
# 文件 IO + 延迟 import agents/dialogue_agent。save_*_prompts 时清空对应缓存。
_PROMPT_CACHE = {}
_PROMPT_CACHE_LOCK = threading.Lock()


def _cache_key(pid, key):
    return (pid or "", key)


def load_global_prompts():
    """读取全局提示词。返回 {agent_key: prompt_text}。缺失返回 {}。"""
    return _read_json(GLOBAL_PROMPTS_FILE, default={}) or {}


def save_global_prompts(prompts):
    """覆写全局提示词。prompts 是 {agent_key: prompt_text}。"""
    _write_json(GLOBAL_PROMPTS_FILE, prompts or {})
    # B12：全局提示词变了，所有 pid 的缓存都失效
    with _PROMPT_CACHE_LOCK:
        _PROMPT_CACHE.clear()


def load_project_prompts(pid):
    """读取项目提示词。返回 {agent_key: prompt_text}。缺失返回 {}。"""
    return _read_json(os.path.join(_project_dir(pid), PROJECT_PROMPTS_FILE), default={}) or {}


def save_project_prompts(pid, prompts):
    """覆写项目提示词。prompts 是 {agent_key: prompt_text}。"""
    _write_json(os.path.join(_project_dir(pid), PROJECT_PROMPTS_FILE), prompts or {})
    # B12：该 pid 的缓存失效
    with _PROMPT_CACHE_LOCK:
        for k in list(_PROMPT_CACHE):
            if k[0] == (pid or ""):
                _PROMPT_CACHE.pop(k, None)


def load_prompt(pid, key):
    """按 项目 > 全局 > 默认 优先级返回 prompt 字符串。
    pid 为 None 时只查全局和默认。key 必须是 PROMPT_AGENT_KEYS 之一。
    B12 修复：带缓存，避免每次调用都触发文件 IO + 延迟 import。"""
    ck = _cache_key(pid, key)
    with _PROMPT_CACHE_LOCK:
        cached = _PROMPT_CACHE.get(ck)
    if cached is not None:
        return cached
    if pid:
        proj = load_project_prompts(pid)
        v = proj.get(key)
        if v:
            with _PROMPT_CACHE_LOCK:
                _PROMPT_CACHE[ck] = v
            return v
    glob = load_global_prompts()
    v = glob.get(key)
    if v:
        with _PROMPT_CACHE_LOCK:
            _PROMPT_CACHE[ck] = v
        return v
    v = _default_prompts().get(key, "")
    with _PROMPT_CACHE_LOCK:
        _PROMPT_CACHE[ck] = v
    return v


def get_all_default_prompts():
    """返回 11 个 key 的默认 prompt 字典。供前端"恢复默认"使用。"""
    return _default_prompts()
