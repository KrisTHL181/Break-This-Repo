const API = "/api/projects";

// F3 修复：统一 fetch 封装，自动检查 res.ok 并抛出含服务端 error 信息的异常。
// 避免各调用点裸用 fetch 后把 4xx/5xx 响应当成功数据用。
async function apiFetch(url, options) {
    const res = await fetch(url, options);
    if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try {
            const data = await res.json();
            if (data && data.error) msg = data.error;
        } catch (_) { /* 非 JSON 响应，用默认 msg */ }
        throw new Error(msg);
    }
    return res;
}

let currentWorkbenchPid = null;
let wbEventSource = null;
let wbCurrentBubble = null;     // {name, body, el} 当前角色消息气泡
let wbNarratorBody = null;      // 当前叙事者描写的 body 元素
// M9 修复：saveAllLlmConfig 防抖 timer，避免连续修改多字段时并发覆盖
let _saveAllLlmConfigTimer = null;

document.addEventListener("DOMContentLoaded", async () => {
    // R17: 启动时把 localStorage 里的解密配置推送到后端内存（后端进程重启需重推）
    let cfg;
    try {
        cfg = await loadStoredConfig();
        if (cfg.providers && cfg.providers.length) {
            await pushConfigToBackend(cfg);
            currentLlmCfg = cfg;  // R21: 赋值全局，streamChatResponse 前置推送用
        }
    } catch (e) {
        console.warn("启动推送 LLM 配置失败:", e);
        cfg = { providers: [], bindings: {} };
    }
    renderView("projects");
    // R18: 无供应商时弹引导模态，指引用户先配置
    if (!cfg.providers || !cfg.providers.length) {
        openLlmSettingsModal();
    }
});

async function renderView(view) {
    const main = document.getElementById("main-content");
    if (view === "projects") {
        closeWbStream();
        main.classList.remove("project-home-content");
        await renderProjects(main);
    } else if (view === "projectHome") {
        // F1 修复：切到 projectHome 前关闭旧 SSE 流，防止跨项目事件串扰
        // （旧项目推演流通过 document.getElementById("wb-stream") 写入，会污染新项目 DOM）
        closeWbStream();
        main.classList.add("project-home-content");
        await renderProjectHome(main, currentWorkbenchPid);
    } else if (view === "workbench") {
        main.classList.remove("project-home-content");
        await renderWorkbench(main, currentWorkbenchPid);
    } else if (view === "chapters") {
        closeWbStream();
        main.classList.remove("project-home-content");
        await renderChaptersView(main, currentWorkbenchPid);
    } else if (view === "completion") {
        closeWbStream();
        main.classList.remove("project-home-content");
        await renderCompletionView(main, currentWorkbenchPid);
    } else if (view === "finale") {
        closeWbStream();
        await renderFinaleView(main, currentWorkbenchPid);
    } else if (view === "assistant") {
        closeWbStream();
        await renderAssistantView(main, currentWorkbenchPid);
    } else {
        main.innerHTML = `<div class="placeholder">敬请期待</div>`;
    }
}

function enterFinale(pid) {
    currentWorkbenchPid = pid;
    renderView("finale");
}

function enterWorkbench(pid) {
    currentWorkbenchPid = pid;
    renderView("projectHome");
}

function closeWbStream() {
    if (wbEventSource) {
        wbEventSource.close();
        wbEventSource = null;
    }
    wbCurrentBubble = null;
    wbNarratorBody = null;
    // H8 修复：清理轮询 timer，避免视图切换后仍每 5s 请求已离开/已删项目
    if (chatStatusTimer) {
        clearInterval(chatStatusTimer);
        chatStatusTimer = null;
    }
}

async function renderProjects(main) {
    main.innerHTML = `
        <div class="page-header">
            <h2>项目列表</h2>
            <div style="display:flex;gap:8px;">
                <button class="btn" id="new-project-btn">新建项目</button>
                <button class="btn btn-ghost" id="import-project-btn">导入项目</button>
            </div>
        </div>
        <div id="project-list"><div class="empty">加载中...</div></div>
    `;
    document.getElementById("new-project-btn").addEventListener("click", createNewProject);
    document.getElementById("import-project-btn").addEventListener("click", importProject);
    await loadProjects();
}

async function loadProjects() {
    const listEl = document.getElementById("project-list");
    try {
        const res = await fetch(API);
        const projects = await res.json();
        if (!projects.length) {
            listEl.innerHTML = `<div class="empty">暂无项目，点击右上角新建</div>`;
            return;
        }
        listEl.innerHTML = `<div class="project-grid">` + projects.map(renderCard).join("") + `</div>`;
        listEl.querySelectorAll(".project-card").forEach(card => {
            card.addEventListener("click", () => enterWorkbench(card.dataset.pid));
        });
        listEl.querySelectorAll(".card-actions").forEach(ca => {
            const pid = ca.dataset.pid;
            ca.addEventListener("click", (e) => { e.stopPropagation(); });
            ca.querySelector(".exp-txt").addEventListener("click", () => downloadExport(pid, "txt"));
            ca.querySelector(".exp-md").addEventListener("click", () => downloadExport(pid, "markdown"));
            ca.querySelector(".exp-epub").addEventListener("click", () => downloadExport(pid, "epub"));
            ca.querySelector(".exp-zip").addEventListener("click", () => showProjectZipModal(pid));
            // Task 13.3：删除项目（破坏性操作确认）
            const delBtn = ca.querySelector(".card-del");
            if (delBtn) {
                delBtn.addEventListener("click", async () => {
                    const card = ca.closest(".project-card");
                    const title = card ? card.querySelector(".title")?.textContent : pid;
                    if (!confirmDestructive(`删除项目「${title}」将不可恢复，是否继续？`)) return;
                    try {
                        const res = await fetch(`/api/projects/${pid}`, { method: "DELETE" });
                        if (!res.ok) throw new Error("删除失败");
                        await loadProjects();
                    } catch (e) { alert(e.message); }
                });
            }
        });
    } catch (err) {
        listEl.innerHTML = `<div class="empty">加载失败: ${err.message}</div>`;
    }
}

function renderCard(p) {
    const created = (p.created_at || "").replace("T", " ").slice(0, 19);
    const completedBadge = p.is_completed
        ? `<span class="fin-badge fin-badge-fix" style="margin-left:6px">已完本</span>` : "";
    return `
        <div class="project-card" data-pid="${escapeAttr(p.id || "")}">
            <div class="title">${escapeHtml(p.title || "")}${completedBadge}</div>
            <div class="genre">${escapeHtml(p.genre || "未分类")}</div>
            <div class="meta">创建时间：${created}</div>
            <div class="meta">当前章节：${p.current_chapter || 0}</div>
            <div class="card-actions" data-pid="${escapeAttr(p.id || "")}">
                <button class="btn btn-sm exp-txt" title="导出 TXT">TXT</button>
                <button class="btn btn-sm btn-ghost exp-md" title="导出 Markdown">MD</button>
                <button class="btn btn-sm btn-ghost exp-epub" title="导出 EPUB">EPUB</button>
                <button class="btn btn-sm btn-ghost exp-zip" title="导出工程文件">工程</button>
                <button class="btn btn-sm btn-ghost card-del" title="删除项目" style="margin-left:auto;color:#b91c1c">删除</button>
            </div>
        </div>
    `;
}

async function createNewProject() {
    // R18: 无供应商时拦截，引导先配置
    const cfg = await loadStoredConfig();
    if (!cfg.providers || !cfg.providers.length) {
        alert("请先配置 LLM 供应商后再新建项目");
        openLlmSettingsModal();
        return;
    }
    // R12/R15: 直接创建空项目 + 跳转主页；initChat 检测历史为空时自动调 /api/dialogue/proactive 让对话员主动开口
    try {
        const res = await fetch(API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title: "新建项目", genre: "" }),
        });
        if (!res.ok) throw new Error((await res.json().catch(() => ({}))).error || "创建失败");
        const meta = await res.json();
        enterWorkbench(meta.id);
    } catch (e) {
        alert(e.message);
    }
}

async function importProject() {
    // R16: 导入工程 zip。保留原 pid，冲突报错。成功后进入项目主页。
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".zip,application/zip";
    input.onchange = async () => {
        if (!input.files[0]) return;
        const fd = new FormData();
        fd.append("file", input.files[0]);
        try {
            const res = await fetch("/api/projects/import", { method: "POST", body: fd });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "导入失败");
            enterWorkbench(data.id);
        } catch (e) {
            alert(e.message);
        }
    };
    input.click();
}

function escapeAttr(s) {
    return escapeHtml(s);
}

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[c]));
}

// R19: 简易 markdown 渲染（先 escapeHtml 再处理标记，防 XSS）。
// 支持：代码块/行内代码/标题/粗体/斜体/链接/段落。不支持嵌套列表（懒惰最小集）。
function renderMarkdown(text) {
    if (!text) return "";
    let s = escapeHtml(text);
    s = s.replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
    s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
    s = s.replace(/^### (.+)$/gm, '<h3>$1</h3>')
         .replace(/^## (.+)$/gm, '<h2>$1</h2>')
         .replace(/^# (.+)$/gm, '<h1>$1</h1>');
    s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    // H6 修复：链接协议白名单（防 javascript: XSS），escapeHtml 不改变 javascript:alert(1)
    s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (m, text, url) => {
        if (/^(https?:|mailto:|\/)/i.test(url)) {
            return `<a href="${url}" target="_blank" rel="noopener">${text}</a>`;
        }
        return m;  // 非白名单协议当纯文本输出
    });
    s = s.split(/\n\n+/).map(block => {
        if (/^<(h[1-6]|pre|ul|ol|blockquote)/.test(block.trim())) return block;
        return `<p>${block.replace(/\n/g, '<br>')}</p>`;
    }).join('');
    return s;
}

// Task 13.3：破坏性操作确认弹窗（最简实现，window.confirm 即可，不强制阻止）
function confirmDestructive(message) {
    return window.confirm(message);
}


// ---------- 项目主页（Task R5：双栏布局骨架）----------
// 左 40% 聊天框（Task R6 填充）+ 右 60% 面板（Task R7 填充 4 个 Tab）

async function renderProjectHome(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">未选择项目</div>`;
        return;
    }
    let meta;
    try {
        const res = await fetch(`/api/projects/${pid}`);
        if (!res.ok) throw new Error("项目不存在");
        meta = await res.json();
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    main.innerHTML = `
        <div class="project-home">
            <div class="chat-col">
                <div class="chat-col-header">
                    <span>对话员 · ${escapeHtml(meta.title || "")}</span>
                    <span class="chat-status" id="chat-status"></span>
                </div>
                <div class="chat-col-body" id="chat-body">
                    <div class="chat-empty">加载历史中...</div>
                </div>
                <div class="chat-col-footer">
                    <textarea id="chat-input" placeholder="输入消息，Enter 发送，Shift+Enter 换行" rows="2"></textarea>
                    <button class="btn" id="chat-send-btn">发送</button>
                    <button class="btn btn-start" id="chat-toggle-btn" title="开始/停止推演">开始推演</button>
                </div>
            </div>
            <div class="panel-col">
                <div class="panel-tabs">
                    <div class="panel-tab" data-panel="project-list">项目列表</div>
                    <div class="panel-tab" data-panel="settings">设置</div>
                    <div class="panel-tab active" data-panel="chatroom">聊天室</div>
                    <div class="panel-tab" data-panel="preview">预览</div>
                    <div class="panel-tab" data-panel="finalization">定稿</div>
                    <div class="panel-tab" data-panel="visualization">可视化</div>
                    <div class="panel-tab" data-panel="dev">开发者</div>
                </div>
                <div class="panel-view" data-panel-view="project-list">
                    <div class="empty">点击"项目列表"加载项目列表</div>
                </div>
                <div class="panel-view" data-panel-view="settings">
                    <div class="empty">点击"设置"加载 5 Agent 配置</div>
                </div>
                <div class="panel-view active" data-panel-view="chatroom">
                    <div class="wb-status">
                        <span>章节：<b id="wb-chapter">${meta.current_chapter || 0}</b></span>
                        <span>轮次：<b id="wb-round">0</b></span>
                        <span>字数：<b id="wb-words">0</b></span>
                        <span>场景：<b id="wb-scene">${escapeHtml(meta.current_scene || "-")}</b></span>
                        <span>候选：<b id="wb-candidates">-</b></span>
                        <span>模式：<b id="wb-mode">${meta.multithread ? "多线程" : "单线程"}</b></span>
                        <span id="wb-rotation-wrap" style="display:${meta.multithread ? "" : "none"}">
                            轮转：<b id="wb-rotation">0/3</b>
                        </span>
                    </div>
                    <div class="wb-stream" id="wb-stream">
                        <div class="empty">通过左侧对话员启动推演</div>
                    </div>
                </div>
                <div class="panel-view" data-panel-view="preview">
                    <div class="empty">点击"预览"加载章节列表</div>
                </div>
                <div class="panel-view" data-panel-view="finalization">
                    <div id="fin-panel-body"><div class="empty">推演结束后自动加载定稿流程</div></div>
                </div>
                <div class="panel-view" data-panel-view="visualization">
                    <div class="empty">点击"可视化"加载图表</div>
                </div>
                <div class="panel-view" data-panel-view="dev">
                    <div class="dev-panel">
                        <div class="dev-sub-tabs">
                            <div class="dev-sub-tab active" data-dev="dialogue">对话员</div>
                            <div class="dev-sub-tab" data-dev="character">角色推演</div>
                            <div class="dev-sub-tab" data-dev="raw">原始 LLM</div>
                        </div>
                        <div class="dev-pid-row">
                            <label>PID</label>
                            <input id="dev-pid" value="${pid}" placeholder="项目 PID" class="dev-input-small">
                            <span class="dev-hint">当前项目 PID 已自动填入</span>
                        </div>
                        <!-- 对话员模式 -->
                        <div class="dev-mode-content" data-dev-content="dialogue">
                            <div class="dev-row">
                                <label>消息</label>
                                <textarea id="dev-dialogue-prompt" rows="4" placeholder="输入发送给对话员的消息"></textarea>
                            </div>
                            <div class="dev-row">
                                <button class="btn" id="dev-dialogue-btn">发送</button>
                                <span class="dev-status" id="dev-dialogue-status"></span>
                            </div>
                            <div class="dev-row"><label>项目上下文</label>
                                <pre class="dev-context" id="dev-dialogue-context"></pre></div>
                            <div class="dev-row"><label>对话历史数</label>
                                <span class="dev-hint" id="dev-dialogue-history-count"></span></div>
                            <div class="dev-row"><label>响应</label>
                                <pre class="dev-response" id="dev-dialogue-response"></pre></div>
                        </div>
                        <!-- 角色推演模式 -->
                        <div class="dev-mode-content" data-dev-content="character" style="display:none">
                            <div class="dev-row">
                                <button class="btn" id="dev-character-load-btn">加载推演上下文</button>
                                <span class="dev-status" id="dev-character-status"></span>
                            </div>
                            <div class="dev-row"><label>当前场景</label>
                                <span class="dev-hint" id="dev-character-scene"></span></div>
                            <div class="dev-row"><label>候选名单</label>
                                <pre class="dev-context" id="dev-character-candidates"></pre></div>
                            <div class="dev-row"><label>选中角色</label>
                                <pre class="dev-context" id="dev-character-selected"></pre></div>
                            <div class="dev-row"><label>上下文包</label>
                                <pre class="dev-context" id="dev-character-context"></pre></div>
                            <div class="dev-row"><label>原始响应</label>
                                <pre class="dev-response" id="dev-character-response"></pre></div>
                            <div class="dev-row"><label>解析结果</label>
                                <pre class="dev-context" id="dev-character-parsed"></pre></div>
                        </div>
                        <!-- 原始 LLM 模式 -->
                        <div class="dev-mode-content" data-dev-content="raw" style="display:none">
                            <div class="dev-row">
                                <label>Agent</label>
                                <select id="dev-raw-agent">
                                    <option value="dialogue">对话员 (dialogue)</option>
                                    <option value="character">角色 (character)</option>
                                    <option value="gm">群主 (gm)</option>
                                    <option value="inspector">检察员 (inspector)</option>
                                    <option value="narrator">叙事者 (narrator)</option>
                                </select>
                            </div>
                            <div class="dev-row">
                                <label>System Prompt <span class="dev-hint">(可选，留空用默认)</span></label>
                                <textarea id="dev-raw-system" rows="4" placeholder="留空则只用 user prompt 调 LLM"></textarea>
                            </div>
                            <div class="dev-row">
                                <label>User Prompt</label>
                                <textarea id="dev-raw-prompt" rows="4" placeholder="输入要发给 AI 的消息"></textarea>
                            </div>
                            <div class="dev-row">
                                <button class="btn" id="dev-raw-btn">发送</button>
                                <span class="dev-status" id="dev-raw-status"></span>
                            </div>
                            <div class="dev-row"><label>响应</label>
                                <pre class="dev-response" id="dev-raw-response"></pre></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    // Tab 切换 + 懒加载（chatroom 内联渲染；finalization 每次 tab 点击都重新渲染以反映最新 finState）
    // R21 修复：loadedPanels 提升为模块级变量，switchPanel 也要访问
    loadedPanels = new Set(["chatroom"]);
    main.querySelectorAll(".panel-tab").forEach(tab => {
        tab.addEventListener("click", async () => {
            const target = tab.dataset.panel;
            main.querySelectorAll(".panel-tab").forEach(t => t.classList.toggle("active", t.dataset.panel === target));
            main.querySelectorAll(".panel-view").forEach(v => v.classList.toggle("active", v.dataset.panelView === target));
            // R21 修复：finalization 不走 loadedPanels 缓存，每次点击都重新渲染（finState 可能在工具调用后变化）
            if (target === "finalization") { renderFinalizationPanel(); return; }
            if (loadedPanels.has(target)) return;
            loadedPanels.add(target);
            if (target === "project-list") await renderProjectListPanel(pid);
            else if (target === "settings") await renderSettingsPanel(pid);
            else if (target === "preview") await renderPreviewPanel(pid);
            else if (target === "visualization") await renderVisualizationPanel(pid);
            else if (target === "dev") renderDevPanel();
        });
    });
    // 初始化聊天框
    initChat(pid);
}


// ---------- R13: 右侧面板新增「项目列表」和「设置」Tab ----------

async function renderProjectListPanel(currentPid) {
    const view = document.querySelector('.panel-view[data-panel-view="project-list"]');
    if (!view) return;
    view.innerHTML = `
        <div class="page-header">
            <h2>项目列表</h2>
            <div style="display:flex;gap:8px;">
                <button class="btn" id="panel-new-project-btn">新建项目</button>
                <button class="btn btn-ghost" id="panel-import-project-btn">导入项目</button>
            </div>
        </div>
        <div id="panel-project-list"><div class="empty">加载中...</div></div>
    `;
    document.getElementById("panel-new-project-btn").addEventListener("click", createNewProject);
    document.getElementById("panel-import-project-btn").addEventListener("click", importProject);
    try {
        const res = await fetch(API);
        const projects = await res.json();
        const listEl = document.getElementById("panel-project-list");
        if (!projects.length) {
            listEl.innerHTML = `<div class="empty">暂无项目，点击右上角新建</div>`;
            return;
        }
        listEl.innerHTML = `<div class="project-grid">` + projects.map(renderCard).join("") + `</div>`;
        listEl.querySelectorAll(".project-card").forEach(card => {
            card.addEventListener("click", () => enterWorkbench(card.dataset.pid));
        });
        listEl.querySelectorAll(".card-actions").forEach(ca => {
            const pid = ca.dataset.pid;
            ca.addEventListener("click", (e) => { e.stopPropagation(); });
            ca.querySelector(".exp-txt").addEventListener("click", () => downloadExport(pid, "txt"));
            ca.querySelector(".exp-md").addEventListener("click", () => downloadExport(pid, "markdown"));
            ca.querySelector(".exp-epub").addEventListener("click", () => downloadExport(pid, "epub"));
            ca.querySelector(".exp-zip").addEventListener("click", () => showProjectZipModal(pid));
            const delBtn = ca.querySelector(".card-del");
            if (delBtn) {
                delBtn.addEventListener("click", async () => {
                    const card = ca.closest(".project-card");
                    const title = card ? card.querySelector(".title")?.textContent : pid;
                    if (!confirmDestructive(`删除项目「${title}」将不可恢复，是否继续？`)) return;
                    try {
                        const res = await fetch(`/api/projects/${pid}`, { method: "DELETE" });
                        if (!res.ok) throw new Error("删除失败");
                        await renderProjectListPanel(currentPid);
                    } catch (e) { alert(e.message); }
                });
            }
        });
    } catch (err) {
        document.getElementById("panel-project-list").innerHTML = `<div class="empty">加载失败: ${err.message}</div>`;
    }
}

let chatSending = false;        // 是否正在发送（防重入）
let chatCurrentPid = null;       // 当前聊天所属 pid
let chatStreaming = false;       // 是否推演中（隐藏输入提示用）
let chatStatusTimer = null;      // 推演状态轮询计时器
let chatAbortController = null;  // R21: 当前对话 SSE 的 AbortController（强制停止按钮用）
let loadedPanels = new Set();    // R22: 直接初始化为空 Set，避免 switchPanel 在 renderProjectHome 之前调用时 .add() 抛错（renderProjectHome 内会重置）
let currentLlmCfg = null;        // R21: 当前 LLM 配置（settings/模态 共享；sendChatMessage 前主动推送，避免 change 事件 race condition）

async function initChat(pid) {
    chatCurrentPid = pid;
    chatStreaming = false;
    updateChatStatus();
    // 加载对话历史
    const historyLen = await loadChatHistory(pid);
    // R22: 加载推演历史到 wb-stream（刷新后回显推演聊天气泡）
    await loadWbHistory(pid);
    // 绑定输入事件
    const input = document.getElementById("chat-input");
    const sendBtn = document.getElementById("chat-send-btn");
    if (input) {
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendChatMessage(pid);
            }
        });
    }
    if (sendBtn) {
        sendBtn.addEventListener("click", () => sendChatMessage(pid));
    }
    // R22: 开始/停止推演切换按钮
    const toggleBtn = document.getElementById("chat-toggle-btn");
    if (toggleBtn) {
        toggleBtn.addEventListener("click", async () => {
            if (chatStreaming) {
                // 停止推演：中断对话 SSE + 调后端停止推演
                if (chatAbortController) {
                    chatAbortController.abort();
                    chatAbortController = null;
                }
                try {
                    await fetch(`/api/projects/${pid}/chapter/stop`, { method: "POST" });
                    appendNoticeBubble("已请求停止推演（轮次边界生效）");
                } catch (e) {
                    appendNoticeBubble(`停止失败: ${e.message}`);
                }
            } else if (!chatSending) {
                // 开始推演：调后端启动 + 打开 SSE 流
                toggleBtn.disabled = true;
                try {
                    const res = await fetch(`/api/projects/${pid}/chapter/start`, { method: "POST" });
                    const data = await res.json();
                    if (!res.ok) throw new Error(data.error || "启动失败");
                    openDeductionStream(pid, data.chapter);
                    // 乐观更新按钮状态（不等 5s 轮询；不立即调 pollChatStreaming，
                    // 避免 EventSource 异步连接未完成时读到 streaming=false 覆盖乐观状态）
                    chatStreaming = true;
                    updateChatStatus();
                } catch (e) {
                    alert(e.message);
                } finally {
                    toggleBtn.disabled = false;
                }
            }
        });
    }
    // 启动推演状态轮询（5s 一次，轻量）
    if (chatStatusTimer) clearInterval(chatStatusTimer);
    chatStatusTimer = setInterval(() => pollChatStreaming(pid), 5000);
    pollChatStreaming(pid);
    // R15: 历史为空时（新建项目或首次进入空项目）触发对话员主动开口
    if (historyLen === 0) {
        await proactiveChat(pid);
    }
}

async function proactiveChat(pid) {
    // R15: 调 /api/dialogue/proactive 让对话员主动开口，SSE 流式接收，复用 handleChatSSEEvent
    if (chatCurrentPid !== pid) return;
    const sendBtn = document.getElementById("chat-send-btn");
    const input = document.getElementById("chat-input");
    if (sendBtn) sendBtn.disabled = true;
    if (input) input.disabled = true;
    chatSending = true;
    updateChatStatus();
    // H9 修复：proactiveChat 也走 chatAbortController，让用户可通过"停止推演"按钮中断
    chatAbortController = new AbortController();
    try {
        const res = await fetch("/api/dialogue/proactive", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pid }),
            signal: chatAbortController.signal,
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || `HTTP ${res.status}`);
        }
        const reader = res.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";
        let currentAssistantBubble = null;
        while (true) {
            try {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                const parts = buffer.split("\n\n");
                buffer = parts.pop() || "";
                for (const part of parts) {
                    const line = part.split("\n").find(l => l.startsWith("data: "));
                    if (!line) continue;
                    // C4 修复：单条 SSE JSON 解析失败不应让整条消息丢失
                    // （代理 keep-alive 帧、被截断的 chunk、后端 500 HTML 错误页等）
                    let json;
                    try {
                        json = JSON.parse(line.slice(6));
                    } catch (e) {
                        console.warn("[SSE] 跳过非 JSON 数据块:", line.slice(6, 200), e);
                        continue;
                    }
                    if (!json || !json.type) continue;
                    handleChatSSEEvent(json, pid, (bubble) => { currentAssistantBubble = bubble; }, () => currentAssistantBubble);
                }
            } catch (e) {
                if (e.name === "AbortError") break;
                throw e;
            }
        }
    } catch (e) {
        if (e.name !== "AbortError") {
            appendNoticeBubble(`对话员主动开口失败: ${e.message}`);
        }
    } finally {
        chatSending = false;
        chatAbortController = null;  // H9: 清理
        updateChatStatus();
        if (sendBtn) sendBtn.disabled = false;
        if (input) input.disabled = false;
    }
}

async function pollChatStreaming(pid) {
    if (chatCurrentPid !== pid) return;
    try {
        const res = await fetch(`/api/projects/${encodeURIComponent(pid)}/chapter/state`);
        if (!res.ok) return;
        const data = await res.json();
        const wasStreaming = chatStreaming;
        // R21 修复：用 streaming（SSE 流是否在跑）判断，而非 active（有活动章节）
        // 否则推演结束后 active=true 会导致永久显示"推演中..."
        chatStreaming = !!data.streaming;
        if (wasStreaming !== chatStreaming) updateChatStatus();
    } catch (_) { /* 静默失败，下次再试 */ }
}

async function loadChatHistory(pid) {
    const body = document.getElementById("chat-body");
    if (!body) return 0;
    try {
        const res = await fetch(`/api/dialogue/history?pid=${encodeURIComponent(pid)}`);
        if (!res.ok) throw new Error("加载历史失败");
        const data = await res.json();
        const history = data.history || [];
        body.innerHTML = "";
        if (history.length === 0) {
            body.innerHTML = `<div class="chat-empty">和对话员说说你的创作想法吧</div>`;
            return 0;
        }
        for (const msg of history) {
            renderHistoryMessage(body, msg);
        }
        body.scrollTop = body.scrollHeight;
        return history.length;
    } catch (e) {
        body.innerHTML = `<div class="chat-empty">加载历史失败: ${escapeHtml(e.message)}</div>`;
        return 0;
    }
}

function renderHistoryMessage(body, msg) {
    // 历史消息可能含 system（注入的上下文）/ user / assistant / tool
    if (msg.role === "system") return;  // system 消息不展示
    if (msg.role === "user") {
        appendChatBubble("user", msg.content || "");
    } else if (msg.role === "assistant") {
        appendChatBubble("assistant", msg.content || "");
        // assistant 含 tool_calls 也展示
        if (msg.tool_calls) {
            for (const tc of msg.tool_calls) {
                appendToolBubble("call", tc.function?.name || tc.name || "tool", tc.function?.arguments || "");
            }
        }
    } else if (msg.role === "tool") {
        // tool result: 解析 content JSON
        let summary = msg.content || "";
        try { summary = JSON.parse(msg.content).summary || msg.content; } catch (_) {}
        appendToolBubble("result", msg.name || "tool", summary);
    }
}

// R22: 加载推演历史（chapter_workspace.chapter_messages）到 wb-stream，刷新后回显
async function loadWbHistory(pid) {
    const stream = document.getElementById("wb-stream");
    if (!stream) return;
    try {
        const res = await fetch(`/api/projects/${pid}/chapter/workspace`);
        if (!res.ok) return;
        const ws = await res.json();
        if (!ws.active) return;
        const msgs = ws.chapter_messages || [];
        if (!msgs.length) return;
        // 更新状态栏
        const ch = document.getElementById("wb-chapter");
        if (ch) ch.textContent = ws.chapter_num || "";
        const r = document.getElementById("wb-round");
        if (r) r.textContent = ws.round || 0;
        const w = document.getElementById("wb-words");
        if (w) w.textContent = ws.word_count || 0;
        const sc = document.getElementById("wb-scene");
        if (sc) sc.textContent = ws.current_scene || "-";
        // 渲染历史消息（按 round 分组，每轮一个分隔条 + 角色气泡 / 叙事者无独立气泡，文本已并入角色 text）
        let lastRound = 0;
        stream.innerHTML = "";
        for (const m of msgs) {
            const round = m.round || 0;
            if (round !== lastRound) {
                const sep = document.createElement("div");
                sep.className = "round-sep";
                sep.textContent = `—— 第 ${round} 轮 · 场景：${m.scene || ""} ——`;
                stream.appendChild(sep);
                lastRound = round;
            }
            const speaker = m.speaker || m.name || "未知";
            const el = document.createElement("div");
            el.className = "msg-bubble";
            el.dataset.name = speaker;
            const head = document.createElement("div");
            head.className = "msg-head";
            head.textContent = `【${speaker}】`;
            const body = document.createElement("div");
            body.className = "msg-body";
            body.textContent = m.text || "";
            el.appendChild(head);
            el.appendChild(body);
            stream.appendChild(el);
        }
        stream.scrollTop = stream.scrollHeight;
    } catch (e) { /* 静默失败 */ }
}

function appendChatBubble(role, content) {
    const body = document.getElementById("chat-body");
    if (!body) return null;
    // 清除空状态提示
    const empty = body.querySelector(".chat-empty");
    if (empty) empty.remove();
    const msg = document.createElement("div");
    msg.className = `chat-msg ${role}`;
    msg.innerHTML = `
        <div class="chat-msg-role">${role === "user" ? "我" : "对话员"}</div>
        <div class="chat-msg-bubble"></div>
    `;
    const bubble = msg.querySelector(".chat-msg-bubble");
    if (role === "assistant") {
        // R19: assistant 消息支持 markdown 渲染；_rawText 用于流式追加
        bubble._rawText = content || "";
        bubble.innerHTML = renderMarkdown(bubble._rawText);
    } else {
        bubble.textContent = content;  // user 消息不渲染 markdown（防注入）
    }
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
    return bubble;
}

// R19: 群主 AI 调用对话气泡（对话员 → 群主 / 群主 → 对话员）
function appendGmDialogueBubble(toolName, instruction, operations, newText) {
    const body = document.getElementById("chat-body");
    if (!body) return;
    const empty = body.querySelector(".chat-empty");
    if (empty) empty.remove();
    const msg = document.createElement("div");
    msg.className = "chat-msg tool";
    const instrText = (typeof instruction === "string") ? instruction : JSON.stringify(instruction, null, 2);
    const opsText = (operations || []).map(o => {
        const op = (o && typeof o === "object") ? (o.operation || "") : String(o || "");
        const tgt = (o && typeof o === "object") ? (o.target || "") : "";
        return `• ${op} ${tgt}`.trim();
    }).join("\n") || "（无操作）";
    const newTextUtils = (newText || "").slice(0, 300);
    msg.innerHTML = `
        <div class="chat-msg-bubble gm-dialogue">
            <div class="gm-turn"><span class="gm-tag">对话员 → 群主</span> <span class="gm-via">via ${escapeHtml(toolName)}</span></div>
            <pre class="gm-instruction">${escapeHtml(instrText)}</pre>
            <div class="gm-turn"><span class="gm-tag">群主 → 对话员</span></div>
            ${opsText ? `<pre class="gm-operations">${escapeHtml(opsText)}</pre>` : ""}
            ${newTextUtils ? `<div class="gm-new-text">${escapeHtml(newTextUtils)}${newText && newText.length > 300 ? "...（更多见右侧定稿面板）" : ""}</div>` : ""}
        </div>
    `;
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
}

function appendToolBubble(kind, toolName, summary) {
    // kind: "call" | "result"
    const body = document.getElementById("chat-body");
    if (!body) return;
    const empty = body.querySelector(".chat-empty");
    if (empty) empty.remove();
    const msg = document.createElement("div");
    msg.className = "chat-msg tool";
    const icon = kind === "call" ? "🔧" : "✓";
    const label = kind === "call" ? "调用" : "结果";
    const isError = summary && summary.startsWith("错误") || summary.startsWith("失败");
    // R21: 工具调用/结果 summary 截断到 100 字（用户反馈工具返回太长影响体验）
    const MAX_LEN = 100;
    let display = summary || "";
    if (display.length > MAX_LEN) display = display.slice(0, MAX_LEN) + "...";
    msg.innerHTML = `
        <div class="chat-msg-bubble${isError ? " error" : ""}">
            <span class="tool-icon">${icon}</span>
            <strong>${escapeHtml(label)} ${escapeHtml(toolName)}</strong>
            <span class="tool-summary">${escapeHtml(display)}</span>
        </div>
    `;
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
}

function appendNoticeBubble(text) {
    const body = document.getElementById("chat-body");
    if (!body) return;
    const empty = body.querySelector(".chat-empty");
    if (empty) empty.remove();
    const msg = document.createElement("div");
    msg.className = "chat-msg tool";
    msg.innerHTML = `<div class="chat-msg-bubble"><span class="tool-icon">ℹ</span>${escapeHtml(text)}</div>`;
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
}

async function sendChatMessage(pid) {
    if (chatSending) return;
    const input = document.getElementById("chat-input");
    const sendBtn = document.getElementById("chat-send-btn");
    if (!input || !sendBtn) return;
    const text = input.value.trim();
    if (!text) return;
    // 渲染用户气泡
    appendChatBubble("user", text);
    input.value = "";
    chatSending = true;
    updateChatStatus();
    sendBtn.disabled = true;
    input.disabled = true;
    try {
        await streamChatResponse(pid, text);
    } catch (e) {
        appendNoticeBubble(`发送失败: ${e.message}`);
    } finally {
        chatSending = false;
        updateChatStatus();
        sendBtn.disabled = false;
        input.disabled = false;
        input.focus();
    }
}

async function streamChatResponse(pid, message) {
    // R21 修复：发消息前主动推送 LLM 配置，避免 input change 事件 race condition 导致后端拿不到最新配置
    // R21 修复：currentLlmCfg 为 null（用户没打开设置面板）时从 localStorage 读，避免重启后端后配置丢失
    let cfg = currentLlmCfg;
    if (!cfg) {
        try { cfg = await loadStoredConfig(); } catch (e) { cfg = null; }
    }
    // R22 修复：直接 pushConfigToBackend，不走 saveAllLlmConfig（后者需要 settings 面板 DOM 存在，否则 return 不推送）
    if (cfg && cfg.providers && cfg.providers.length) {
        try { await pushConfigToBackend(cfg); } catch (e) { console.warn("前置推送 cfg 失败:", e); }
    }
    chatAbortController = new AbortController();
    let res;
    try {
        res = await fetch("/api/dialogue/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pid, message }),
            signal: chatAbortController.signal,
        });
    } catch (e) {
        if (e.name === "AbortError") return;  // 用户主动停止
        throw e;
    } finally {
        chatAbortController = null;
    }
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || `HTTP ${res.status}`);
    }
    const reader = res.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let currentAssistantBubble = null;

    while (true) {
        try {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            // SSE 事件以 "\n\n" 分隔
            const parts = buffer.split("\n\n");
            buffer = parts.pop() || "";  // 最后一段可能不完整
            for (const part of parts) {
                const line = part.split("\n").find(l => l.startsWith("data: "));
                if (!line) continue;
                // C4 修复：单条 SSE JSON 解析失败不应让整条消息丢失
                // （代理 keep-alive 帧、被截断的 chunk、后端 500 HTML 错误页等）
                let json;
                try {
                    json = JSON.parse(line.slice(6));
                } catch (e) {
                    console.warn("[SSE] 跳过非 JSON 数据块:", line.slice(6, 200), e);
                    continue;
                }
                if (!json || !json.type) continue;
                handleChatSSEEvent(json, pid, (bubble) => { currentAssistantBubble = bubble; }, () => currentAssistantBubble);
            }
        } catch (e) {
            // R21: AbortError 是用户主动停止，正常退出循环
            if (e.name === "AbortError") break;
            throw e;
        }
    }
}

function handleChatSSEEvent(ev, pid, setAssistantBubble, getAssistantBubble) {
    if (ev.type === "text") {
        let bubble = getAssistantBubble();
        if (!bubble) {
            bubble = appendChatBubble("assistant", "");
            setAssistantBubble(bubble);
        }
        // R19: 流式 markdown — 累积 _rawText 后整体 re-render（中间态可能短暂不完整，可接受）
        bubble._rawText = (bubble._rawText || "") + (ev.content || "");
        bubble.innerHTML = renderMarkdown(bubble._rawText);
        const body = document.getElementById("chat-body");
        if (body) body.scrollTop = body.scrollHeight;
    } else if (ev.type === "tool_call") {
        // R21 修复：工具调用开始时重置 currentAssistantBubble，让下一轮 text 新建 bubble，
        // 避免 text→tool_call→text 时新文本追加到旧 bubble 导致工具气泡压在最下、新文本看不见
        setAssistantBubble(null);
        appendToolBubble("call", ev.name, JSON.stringify(ev.args || {}));
    } else if (ev.type === "tool_result") {
        const summary = ev.summary || ev.error || "";
        appendToolBubble("result", ev.name, summary);
        // 工具触发面板切换
        if (ev.switch_panel) {
            switchPanel(pid, ev.switch_panel);
        }
        // start_chapter 工具：打开推演 SSE 流（POST 已由后端工具完成）
        if (ev.name === "start_chapter" && ev.data && ev.data.chapter != null) {
            openDeductionStream(pid, ev.data.chapter);
        }
        // R20: export_novel 工具：追加可点击下载链接（appendToolBubble 只渲染 summary 文本）
        if (ev.name === "export_novel" && ev.data && ev.data.download_url) {
            const body = document.getElementById("chat-body");
            if (body) {
                const empty = body.querySelector(".chat-empty");
                if (empty) empty.remove();
                const msg = document.createElement("div");
                msg.className = "chat-msg tool";
                // H7 修复：download_url 转义 + 前缀白名单（防 javascript:/伪协议注入）
                const dlUrl = String(ev.data.download_url || "");
                const safeUrl = /^(https?:|\/)/i.test(dlUrl) ? dlUrl : "#";
                msg.innerHTML = `<div class="chat-msg-bubble"><span class="tool-icon">📥</span><a href="${escapeHtml(safeUrl)}" target="_blank" rel="noopener" style="color:#2563eb;text-decoration:underline">下载 ${escapeHtml(ev.data.format || "")} 文件</a></div>`;
                body.appendChild(msg);
                body.scrollTop = body.scrollHeight;
            }
        }
        // R22: update_project_meta 工具：同步更新聊天头部显示的标题
        if (ev.name === "update_project_meta" && ev.data && ev.data.new_title) {
            const header = document.querySelector(".chat-col-header span");
            if (header) header.textContent = `对话员 · ${ev.data.new_title}`;
            document.title = `${ev.data.new_title} - 小说创作平台`;
        }
        // R22 修复：discard_chapter 工具：后端已清除 _active_chapters，前端需同步推演状态
        // 否则 UI 仍显示"停止推演"，用户以为丢弃没生效（实际后端已成功，只是前端没刷新）
        if (ev.name === "discard_chapter") {
            chatStreaming = false;
            updateChatStatus();
            pollChatStreaming(pid);
        }
    } else if (ev.type === "gm_dialogue") {
        // R19: 群主 AI 调用对话（对话员 → 群主 / 群主 → 对话员）
        appendGmDialogueBubble(ev.tool_name || "", ev.instruction, ev.operations, ev.new_text);
    } else if (ev.type === "queued") {
        appendNoticeBubble(ev.message || "消息已排队");
    } else if (ev.type === "error") {
        appendNoticeBubble(`错误: ${ev.message || ""}`);
    } else if (ev.type === "done") {
        // 结束本轮
    }
}

function updateChatStatus() {
    const status = document.getElementById("chat-status");
    if (!status) return;
    if (chatStreaming) {
        status.textContent = "推演中...";
        status.classList.add("streaming");
    } else if (chatSending) {
        status.textContent = "思考中...";
        status.classList.remove("streaming");
    } else {
        status.textContent = "";
        status.classList.remove("streaming");
    }
    // R22: 开始/停止推演切换按钮 — 仅推演流在跑时显示"停止推演"
    const toggleBtn = document.getElementById("chat-toggle-btn");
    if (toggleBtn) {
        if (chatStreaming) {
            toggleBtn.textContent = "停止推演";
            toggleBtn.classList.remove("btn-start");
            toggleBtn.classList.add("btn-stop");
            toggleBtn.disabled = false;
        } else {
            toggleBtn.textContent = "开始推演";
            toggleBtn.classList.remove("btn-stop");
            toggleBtn.classList.add("btn-start");
            // AI 思考中时禁用开始按钮（防止冲突）
            toggleBtn.disabled = chatSending;
        }
    }
}

// 提供给其他视图调用的工具：切到指定 panel
function switchPanel(pid, panelName) {
    if (currentWorkbenchPid !== pid) return;
    const main = document.getElementById("main-content");
    const tab = main.querySelector(`.panel-tab[data-panel="${panelName}"]`);
    if (!tab) return;
    // R21 修复：工具触发切换时强制重新渲染对应面板，绕过 loadedPanels 缓存
    // （否则首次渲染后 AI 再执行工具，切回该面板不会刷新数据）
    main.querySelectorAll(".panel-tab").forEach(t => t.classList.toggle("active", t.dataset.panel === panelName));
    main.querySelectorAll(".panel-view").forEach(v => v.classList.toggle("active", v.dataset.panelView === panelName));
    loadedPanels.add(panelName);
    if (panelName === "preview") renderPreviewPanel(pid);
    else if (panelName === "visualization") renderVisualizationPanel(pid);
    else if (panelName === "finalization") renderFinalizationPanel();
    else if (panelName === "project-list") renderProjectListPanel(pid);
    else if (panelName === "settings") renderSettingsPanel(pid);
    else if (panelName === "dev") renderDevPanel();
    // chatroom 无需重新渲染（聊天记录在 chat-body，由 SSE 实时追加）
}


// ---------- 右侧面板视图（Task R7）----------

// R7.3 预览视图：左章节列表 + 右导出样式正文 + "修改"按钮
async function renderPreviewPanel(pid) {
    const view = document.querySelector('.panel-view[data-panel-view="preview"]');
    if (!view) return;
    view.innerHTML = `<div class="empty">加载中...</div>`;
    let chapters = [];
    try {
        const res = await fetch(`/api/projects/${pid}/chapters`);
        if (res.ok) chapters = await res.json();
    } catch (e) { view.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`; return; }

    if (!chapters.length) {
        view.innerHTML = `<div class="empty">暂无章节</div>`;
        return;
    }
    const sorted = [...chapters].sort((a, b) => (a.chapter || 0) - (b.chapter || 0));
    view.innerHTML = `
        <div class="preview-layout">
            <div class="preview-list" id="preview-list">
                ${sorted.map(c => `
                    <div class="preview-item" data-ch="${c.chapter}">
                        <div class="preview-item-title">第${c.chapter}章 · ${escapeHtml(c.title || "")}</div>
                        <div class="preview-item-meta">${c.word_count || 0} 字 · ${c.finalized !== false ? "已定稿" : "未定稿"}</div>
                    </div>`).join("")}
            </div>
            <div class="preview-content" id="preview-content">
                <div class="empty">点击左侧章节查看正文</div>
            </div>
        </div>
    `;
    view.querySelectorAll(".preview-item").forEach(item => {
        item.addEventListener("click", () => {
            const ch = parseInt(item.dataset.ch, 10);
            view.querySelectorAll(".preview-item").forEach(x => x.classList.remove("active"));
            item.classList.add("active");
            loadChapterPreview(pid, ch, sorted);
        });
    });
}

function loadChapterPreview(pid, chapterNum, chapters) {
    const content = document.getElementById("preview-content");
    if (!content) return;
    const ch = chapters.find(c => c.chapter === chapterNum);
    if (!ch) {
        content.innerHTML = `<div class="empty">未找到第 ${chapterNum} 章</div>`;
        return;
    }
    const paragraphs = (ch.text || "").split(/\n\n+/);
    const textHtml = paragraphs.map(p =>
        `<p style="text-indent:2em;margin:0 0 1em 0;line-height:1.8">${escapeHtml(p)}</p>`
    ).join("");
    content.innerHTML = `
        <div class="preview-chapter">
            <h3 class="preview-ch-title">第${ch.chapter}章 · ${escapeHtml(ch.title || "")}</h3>
            <div class="preview-ch-text">${textHtml || '<div class="empty">（无正文）</div>'}</div>
            <div class="preview-ch-actions">
                <button class="btn" id="preview-edit-btn" data-ch="${ch.chapter}">修改</button>
            </div>
        </div>
    `;
    // R9: 点击修改按钮 → 注入 system 上下文 + 切到对话员 + 自动发送用户消息触发 locate_chapter
    const editBtn = document.getElementById("preview-edit-btn");
    if (editBtn) {
        editBtn.addEventListener("click", async () => {
            try {
                await fetch(`/api/dialogue/context`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ pid, message: `用户想修改第 ${ch.chapter} 章「${ch.title || ""}」，请引导用户描述修改意图。` }),
                });
            } catch (e) { /* ignore */ }
            switchPanel(pid, "chatroom");
            // R9.3 触发对话员响应：预填用户消息并自动发送，对话员基于注入的 system 上下文调用 locate_chapter
            const input = document.getElementById("chat-input");
            if (input) {
                input.value = `我想修改第 ${ch.chapter} 章「${ch.title || ""}」`;
                await sendChatMessage(pid);
            }
        });
    }
}

// R7.5 可视化视图：4 个子 Tab（关系图/事件轴/字数/伏笔）
async function renderVisualizationPanel(pid) {
    const view = document.querySelector('.panel-view[data-panel-view="visualization"]');
    if (!view) return;
    view.innerHTML = `
        <div class="viz-sub-tabs">
            <div class="viz-sub-tab active" data-viz="relations">角色关系图</div>
            <div class="viz-sub-tab" data-viz="events">事件时间轴</div>
            <div class="viz-sub-tab" data-viz="wordstats">字数统计</div>
            <div class="viz-sub-tab" data-viz="foreshadows">伏笔</div>
        </div>
        <div class="viz-sub-content" id="viz-sub-content">
            <div class="empty">加载中...</div>
        </div>
    `;
    const vizBody = view.querySelector("#viz-sub-content");
    async function loadViz(name) {
        if (vizBody.dataset.current === name) return;
        vizBody.dataset.current = name;
        vizBody.innerHTML = `<div class="empty">加载中...</div>`;
        try {
            if (name === "relations") await renderRelationsGraph(vizBody, pid);
            else if (name === "events") await renderEventsView(vizBody, pid);
            else if (name === "wordstats") await renderWordStatsView(vizBody, pid);
            else if (name === "foreshadows") await renderForeshadowsView(vizBody, pid);
        } catch (e) { vizBody.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`; }
    }
    view.querySelectorAll(".viz-sub-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            view.querySelectorAll(".viz-sub-tab").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            loadViz(tab.dataset.viz);
        });
    });
    loadViz("relations");
}

// 字数统计视图（ECharts bar + line 累计）
async function renderWordStatsView(main, pid) {
    let chapters = [];
    try {
        const res = await fetch(`/api/projects/${pid}/chapters`);
        if (res.ok) chapters = await res.json();
    } catch (e) { /* ignore */ }
    main.innerHTML = `
        <div class="page-header"><h2>字数统计</h2></div>
        ${chapters.length === 0
            ? `<div class="empty">暂无章节数据</div>`
            : `<div id="ws-chart" class="viz-chart"></div>`}
    `;
    if (!chapters.length) return;
    const el = document.getElementById("ws-chart");
    if (!el || typeof echarts === "undefined") return;
    const sorted = [...chapters].sort((a, b) => (a.chapter || 0) - (b.chapter || 0));
    const xData = sorted.map(c => `第${c.chapter || "?"}章`);
    const counts = sorted.map(c => c.word_count || 0);
    let cum = 0;
    const cumulative = counts.map(c => (cum += c));
    const chart = initVizChart(el);
    chart.setOption({
        tooltip: {
            trigger: "axis",
            formatter: (params) => {
                const idx = params[0].dataIndex;
                return `第${sorted[idx].chapter || "?"}章 ${escapeHtml(sorted[idx].title || "")}<br/>本章：${counts[idx]} 字<br/>累计：${cumulative[idx]} 字`;
            }
        },
        legend: { data: ["本章字数", "累计字数"], top: 6 },
        grid: { left: 60, right: 60, top: 50, bottom: 40 },
        xAxis: { type: "category", data: xData },
        yAxis: [{ type: "value", name: "本章字数" }, { type: "value", name: "累计字数" }],
        series: [
            { name: "本章字数", type: "bar", data: counts, itemStyle: { color: "#5470c6" } },
            { name: "累计字数", type: "line", yAxisIndex: 1, data: cumulative, itemStyle: { color: "#ee6666" } },
        ],
    });
}


// ---------- 开发者模式 ----------

function renderDevPanel() {
    const view = document.querySelector('.panel-view[data-panel-view="dev"]');
    if (!view) return;
    // 子 tab 切换
    view.querySelectorAll(".dev-sub-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            view.querySelectorAll(".dev-sub-tab").forEach(t => t.classList.toggle("active", t === tab));
            const mode = tab.dataset.dev;
            view.querySelectorAll(".dev-mode-content").forEach(c => c.style.display = c.dataset.devContent === mode ? "" : "none");
        });
    });
    // 对话员模式：发送消息
    bindOnce(view, "#dev-dialogue-btn", "click", async () => {
        const pid = view.querySelector("#dev-pid").value.trim();
        const prompt = view.querySelector("#dev-dialogue-prompt").value.trim();
        const status = view.querySelector("#dev-dialogue-status");
        const ctx = view.querySelector("#dev-dialogue-context");
        const hc = view.querySelector("#dev-dialogue-history-count");
        const resp = view.querySelector("#dev-dialogue-response");
        if (!pid || !prompt) { status.textContent = "需要 PID 和消息"; return; }
        status.textContent = "请求中..."; resp.textContent = "";
        try {
            const r = await fetch("/api/dev/run", {
                method: "POST", headers: {"Content-Type": "application/json"},
                body: JSON.stringify({mode: "dialogue", pid, prompt}),
            });
            const d = await r.json();
            if (d.error) { status.textContent = `错误: ${d.error}`; return; }
            ctx.textContent = d.context;
            hc.textContent = `历史消息数: ${d.history_length}`;
            resp.textContent = d.response;
            status.textContent = "完成";
        } catch (e) { status.textContent = `失败: ${e.message}`; }
    });
    // 角色推演模式：加载上下文 + 推演一轮
    bindOnce(view, "#dev-character-load-btn", "click", async () => {
        const pid = view.querySelector("#dev-pid").value.trim();
        const status = view.querySelector("#dev-character-status");
        const scene = view.querySelector("#dev-character-scene");
        const cand = view.querySelector("#dev-character-candidates");
        const sel = view.querySelector("#dev-character-selected");
        const ctx = view.querySelector("#dev-character-context");
        const resp = view.querySelector("#dev-character-response");
        const parsed = view.querySelector("#dev-character-parsed");
        if (!pid) { status.textContent = "需要 PID"; return; }
        status.textContent = "请求中...";
        scene.textContent = ""; cand.textContent = ""; sel.textContent = "";
        ctx.textContent = ""; resp.textContent = ""; parsed.textContent = "";
        try {
            const r = await fetch("/api/dev/run", {
                method: "POST", headers: {"Content-Type": "application/json"},
                body: JSON.stringify({mode: "character", pid}),
            });
            const d = await r.json();
            if (d.error) { status.textContent = `错误: ${d.error}`; return; }
            scene.textContent = d.current_scene;
            cand.textContent = JSON.stringify(d.candidates, null, 2);
            sel.textContent = JSON.stringify(d.character, null, 2);
            ctx.textContent = JSON.stringify(d.context_pack, null, 2);
            resp.textContent = d.raw_response;
            parsed.textContent = JSON.stringify(d.parsed, null, 2);
            status.textContent = "完成";
        } catch (e) { status.textContent = `失败: ${e.message}`; }
    });
    // 原始 LLM 模式：切换 agent 时自动加载默认 system prompt
    const rawAgentSel = view.querySelector("#dev-raw-agent");
    const _PROMPT_KEY_MAP = {
        dialogue: "dialogue", character: "character", gm: "gm",
        inspector: "inspector_validate", narrator: "narrator_describe",
    };
    rawAgentSel.addEventListener("change", async () => {
        const agentKey = rawAgentSel.value;
        const sysBox = view.querySelector("#dev-raw-system");
        sysBox.value = "";
        try {
            const res = await fetch("/api/prompts/defaults");
            if (!res.ok) throw new Error("加载失败");
            const defaults = await res.json();
            const promptKey = _PROMPT_KEY_MAP[agentKey] || agentKey;
            const defaultSys = defaults[promptKey] || "";
            if (defaultSys) sysBox.value = defaultSys;
        } catch (e) { /* 加载失败不阻止用户输入 */ }
    });
    // 触发初始加载当前选中
    rawAgentSel.dispatchEvent(new Event("change"));
    // 原始 LLM 模式发送
    bindOnce(view, "#dev-raw-btn", "click", async () => {
        const pid = view.querySelector("#dev-pid").value.trim();
        const agentKey = view.querySelector("#dev-raw-agent").value;
        const systemPrompt = view.querySelector("#dev-raw-system").value;
        const prompt = view.querySelector("#dev-raw-prompt").value.trim();
        const status = view.querySelector("#dev-raw-status");
        const resp = view.querySelector("#dev-raw-response");
        if (!prompt) { status.textContent = "请输入 prompt"; return; }
        status.textContent = "请求中..."; resp.textContent = "";
        try {
            const r = await fetch("/api/dev/run", {
                method: "POST", headers: {"Content-Type": "application/json"},
                body: JSON.stringify({mode: "raw", pid, agent_key: agentKey, prompt, system_prompt: systemPrompt}),
            });
            const d = await r.json();
            if (d.error) { status.textContent = `错误: ${d.error}`; return; }
            resp.textContent = d.response;
            status.textContent = "完成";
        } catch (e) { status.textContent = `失败: ${e.message}`; }
    });
}

/** 防重复绑定：移除元素的旧 listener 后绑定新 listener */
function bindOnce(parent, selector, event, handler) {
    const el = parent.querySelector(selector);
    if (!el) return;
    const clone = el.cloneNode(true);
    el.parentNode.replaceChild(clone, el);
    clone.addEventListener(event, handler);
}


// ---------- 项目工作台（推演观察页） ----------

async function renderWorkbench(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">未选择项目</div>`;
        return;
    }
    let meta;
    try {
        const res = await fetch(`/api/projects/${pid}`);
        if (!res.ok) throw new Error("项目不存在");
        meta = await res.json();
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    main.innerHTML = `
        <div class="page-header">
            <h2>项目工作台 - ${escapeHtml(meta.title || "")}</h2>
            <div class="wb-actions">
                <button class="btn" id="wb-start">开始写新章</button>
                <button class="btn btn-ghost" id="wb-stop">喊停</button>
                <button class="btn btn-ghost" id="wb-scenes">场景管理</button>
                <button class="btn btn-ghost" id="wb-chapters">章节库</button>
                <button class="btn btn-ghost" id="wb-danger">危险操作</button>
                <button class="btn btn-ghost" id="wb-back">返回项目列表</button>
            </div>
        </div>
        <div class="wb-status">
            <span>章节：<b id="wb-chapter">${meta.current_chapter || 0}</b></span>
            <span>轮次：<b id="wb-round">0</b></span>
            <span>字数：<b id="wb-words">0</b></span>
            <span>场景：<b id="wb-scene">${escapeHtml(meta.current_scene || "")}</b></span>
            <span>候选：<b id="wb-candidates">-</b></span>
            <span>模式：<b id="wb-mode">${meta.multithread ? "多线程" : "单线程"}</b></span>
            <span id="wb-rotation-wrap" style="display:${meta.multithread ? "" : "none"}">
                轮转：<b id="wb-rotation">0/3</b>
            </span>
            <span>字数目标：<b id="wb-target" class="wb-clickable" title="点击修改">${wordTargetText(meta.word_count_target)}</b></span>
        </div>
        <div class="wb-stream" id="wb-stream">
            <div class="empty">点击"开始写新章"启动推演</div>
        </div>
    `;
    document.getElementById("wb-back").addEventListener("click", () => renderView("projects"));
    document.getElementById("wb-start").addEventListener("click", () => startChapter(pid));
    document.getElementById("wb-stop").addEventListener("click", () => stopChapter(pid));
    document.getElementById("wb-scenes").addEventListener("click", () => showSceneManagerModal(pid));
    document.getElementById("wb-chapters").addEventListener("click", () => {
        renderView("chapters");
    });
    // Task 13.4：字数目标点击编辑
    document.getElementById("wb-target").addEventListener("click", () => showWordCountTargetModal(pid));
    // Task 13.1 / 13.2：危险操作区（换题材 / 推倒重写）
    document.getElementById("wb-danger").addEventListener("click", () => showDangerZoneModal(pid));
    // 阶段 6：进入项目时自动检测崩溃恢复
    checkCrashRecovery(pid);
}

// Task 13.4：字数目标显示文案
function wordTargetText(target) {
    const t = target || {};
    if (t.min == null && t.max == null) return "2000-3000（默认）";
    if (t.min == null) return `≤${t.max}`;
    if (t.max == null || t.min === t.max) return `${t.min}`;
    return `${t.min}-${t.max}`;
}

// Task 13.4：字数目标编辑弹窗
function showWordCountTargetModal(pid) {
    fetch(`/api/projects/${pid}`).then(r => r.json()).then(meta => {
        const t = meta.word_count_target || { min: 2000, max: 3000 };
        const mask = document.createElement("div");
        mask.className = "modal-mask";
        mask.innerHTML = `
            <div class="modal" style="width:360px">
                <h3>章节字数目标</h3>
                <div class="field">
                    <label>每章字数下限（min）</label>
                    <input type="number" id="wc-min" value="${t.min ?? 2000}" min="0" style="width:100%;padding:6px;border:1px solid #d1d5db;border-radius:4px">
                </div>
                <div class="field">
                    <label>每章字数上限（max，留空 = 等于 min）</label>
                    <input type="number" id="wc-max" value="${t.max ?? t.min ?? 3000}" min="0" style="width:100%;padding:6px;border:1px solid #d1d5db;border-radius:4px">
                </div>
                <div class="modal actions">
                    <button class="btn btn-ghost" id="wc-cancel">取消</button>
                    <button class="btn" id="wc-save">保存</button>
                </div>
            </div>
        `;
        document.body.appendChild(mask);
        const close = () => mask.remove();
        mask.querySelector("#wc-cancel").addEventListener("click", close);
        mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
        mask.querySelector("#wc-save").addEventListener("click", async () => {
            const mn = parseInt(mask.querySelector("#wc-min").value, 10);
            const mxRaw = mask.querySelector("#wc-max").value.trim();
            const mx = mxRaw ? parseInt(mxRaw, 10) : mn;
            if (isNaN(mn) || mn < 0) { alert("min 必须为非负整数"); return; }
            if (isNaN(mx) || mx < mn) { alert("max 必须 >= min"); return; }
            try {
                const res = await fetch(`/api/projects/${pid}/word-count-target`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ min: mn, max: mx }),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "保存失败");
                const el = document.getElementById("wb-target");
                if (el) el.textContent = wordTargetText(data.word_count_target);
                close();
            } catch (e) { alert(e.message); }
        });
    }).catch(e => alert(e.message));
}

// Task 13.1 / 13.2：危险操作弹窗（换题材 + 推倒重写）
function showDangerZoneModal(pid) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal" style="width:480px">
            <h3 style="color:#b91c1c">⚠ 危险操作</h3>
            <div class="fin-section">
                <div class="msg-notice msg-notice-warning">中途换题材：保留角色卡，重置其他所有库（世界规则 / 章节 / 事件 / 伏笔 等）。会自动备份当前状态为快照。</div>
                <div class="fin-row"><label>新题材</label><input type="text" id="dg-genre" placeholder="如：科幻 / 武侠 / 言情" style="width:100%;padding:6px;border:1px solid #d1d5db;border-radius:4px"></div>
                <div class="fin-actions">
                    <button class="btn btn-ghost" id="dg-change-genre">中途换题材</button>
                </div>
            </div>
            <div class="fin-section">
                <div class="msg-notice msg-notice-warning">推倒重写：加载历史快照覆盖当前状态。当前状态会自动备份为快照（旧档保留）。</div>
                <div id="dg-snapshots-list" class="asst-check-list" style="margin:8px 0"><div class="empty" style="padding:14px 0">加载中...</div></div>
                <div class="fin-actions">
                    <button class="btn btn-ghost" id="dg-rollback" disabled>推倒重写到选中快照</button>
                </div>
            </div>
            <div class="modal actions">
                <button class="btn" id="dg-close">关闭</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#dg-close").addEventListener("click", close);
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });

    // 加载快照列表
    fetch(`/api/projects/${pid}/snapshots`).then(r => r.json()).then(snapshots => {
        const listEl = mask.querySelector("#dg-snapshots-list");
        const rollbackBtn = mask.querySelector("#dg-rollback");
        if (!snapshots.length) {
            listEl.innerHTML = `<div class="empty" style="padding:14px 0">暂无快照</div>`;
            return;
        }
        listEl.innerHTML = snapshots.map(s => `
            <label class="asst-check"><input type="radio" name="dg-snap" value="${escapeAttr(s)}"> ${escapeHtml(s)}</label>
        `).join("");
        rollbackBtn.disabled = false;
        rollbackBtn.addEventListener("click", async () => {
            const checked = mask.querySelector('input[name="dg-snap"]:checked');
            if (!checked) { alert("请选择一个快照"); return; }
            if (!confirmDestructive(`推倒重写到快照「${checked.value}」？当前状态将自动备份，但会立即被快照覆盖。`)) return;
            try {
                const res = await fetch(`/api/projects/${pid}/rollback`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ version: checked.value }),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "操作失败");
                alert(`已恢复到快照 ${checked.value}`);
                close();
                renderView("projectHome");
            } catch (e) { alert(e.message); }
        });
    }).catch(e => alert(e.message));

    // 中途换题材
    mask.querySelector("#dg-change-genre").addEventListener("click", async () => {
        const newGenre = mask.querySelector("#dg-genre").value.trim();
        if (!newGenre) { alert("请输入新题材"); return; }
        if (!confirmDestructive(`换题材为「${newGenre}」？将保留角色卡，重置其他所有库。当前状态会自动备份为快照。`)) return;
        try {
            const res = await fetch(`/api/projects/${pid}/change-genre`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ new_genre: newGenre }),
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "操作失败");
            alert(`已切换题材为 ${data.genre}，可继续创作`);
            close();
            renderView("projectHome");
        } catch (e) { alert(e.message); }
    });
}

async function startChapter(pid) {
    // C3 修复：推演中再次点击"开始写新章"会触发后端 start_chapter，已有 H1 后端锁，
    // 但前端体验更友好——直接提示而不是发请求拿到 409
    if (chatStreaming || wbEventSource) {
        appendNoticeBubble("已在推演中，请先停止当前推演");
        return;
    }
    // F4 修复：await fetch 期间不禁用按钮会导致双击触发两次 POST。
    // 提前置位 chatStreaming 防止重入，finally 中恢复。
    chatStreaming = true;
    try {
        const res = await fetch(`/api/projects/${pid}/chapter/start`, { method: "POST" });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "启动失败");
        openDeductionStream(pid, data.chapter);
    } catch (e) {
        alert(e.message);
    } finally {
        // openDeductionStream 会设置 wbEventSource 并重置 chatStreaming；
        // 仅在未成功打开流时恢复 chatStreaming
        if (!wbEventSource) chatStreaming = false;
    }
}

// R7：对话员 start_chapter 工具结果触发打开推演 SSE 流（POST 已由后端工具完成）
async function openDeductionStream(pid, chapterNum) {
    const ch = document.getElementById("wb-chapter");
    if (ch) ch.textContent = chapterNum;
    const r = document.getElementById("wb-round");
    if (r) r.textContent = "0";
    const w = document.getElementById("wb-words");
    if (w) w.textContent = "0";
    const stream = document.getElementById("wb-stream");
    if (stream) stream.innerHTML = "";
    // R22 修复：继续推演时，先加载之前的推演历史（chapter_workspace.chapter_messages）
    // 否则"暂停→继续"后 wb-stream 被清空，之前的推演内容丢失
    await loadWbHistory(pid);
    // M13 修复：await 期间用户可能切走，再次校验 currentWorkbenchPid / wb-stream 存在
    if (currentWorkbenchPid !== pid) return;
    if (!document.getElementById("wb-stream")) return;
    closeWbStream();
    wbEventSource = new EventSource(`/api/projects/${pid}/chapter/stream`);
    wbEventSource.onopen = () => {
        chatStreaming = true;
        updateChatStatus();
    };
    wbEventSource.onmessage = (e) => handleStreamEvent(e.data);
    wbEventSource.onerror = () => {
        appendNotice("连接已关闭", "end");
        closeWbStream();
        chatStreaming = false;
        updateChatStatus();
    };
}

async function stopChapter(pid) {
    try {
        await fetch(`/api/projects/${pid}/chapter/stop`, { method: "POST" });
        appendNotice("已请求喊停（轮次边界生效）", "notice");
    } catch (e) {
        alert(e.message);
    }
}

// ---------- Task 11：场景管理弹窗（多线程切换 + 暂停/恢复） ----------

async function showSceneManagerModal(pid) {
    let meta = null, sceneState = null;
    try {
        const [mRes, sRes] = await Promise.all([
            fetch(`/api/projects/${pid}`),
            fetch(`/api/projects/${pid}/scenes/active`),
        ]);
        meta = await mRes.json();
        if (sRes.ok) sceneState = await sRes.json();
    } catch (e) { alert(e.message); return; }

    const multithread = !!meta.multithread;
    const activeScenes = (sceneState && sceneState.active_scenes) || [];
    const pausedScenes = (sceneState && sceneState.paused_scenes) || [];
    const st = (sceneState && sceneState.state) || {};
    const switchRounds = st.scene_switch_rounds || 3;
    const curIdx = st.current_scene_index || 0;
    const roundsIn = st.rounds_in_current_scene || 0;

    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal scene-manager-modal">
            <h3>场景管理</h3>
            <div class="field">
                <label><input type="checkbox" id="mt-toggle" ${multithread ? "checked" : ""}> 多线程模式（场景轮转推演）</label>
                <div class="scene-mgr-hint">单线程：场景切换时旧场景暂停；多线程：每 ${switchRounds} 轮切换至下一场景</div>
            </div>
            <div class="scene-mgr-section">
                <div class="msg-notice msg-notice-scene">当前模式：${multithread ? "多线程" : "单线程"}</div>
            </div>
            ${multithread ? `
                <div class="scene-mgr-section">
                    <div class="msg-notice">活动场景轮转顺序（${activeScenes.length} 个）</div>
                    <div class="scene-list">
                        ${activeScenes.map((s, i) => `
                            <div class="scene-item ${i === curIdx ? "scene-item-current" : ""}">
                                <span class="scene-idx">${i + 1}</span>
                                <span class="scene-name">${escapeHtml(s)}</span>
                                ${i === curIdx ? `<span class="fin-badge fin-badge-fix">当前 ${roundsIn}/${switchRounds}</span>` : ""}
                            </div>`).join("")}
                    </div>
                </div>
            ` : `
                <div class="scene-mgr-section">
                    <div class="msg-notice">当前场景：<b>${escapeHtml(meta.current_scene || "未设置")}</b></div>
                </div>
            `}
            <div class="scene-mgr-section">
                <div class="msg-notice msg-notice-warning">已暂停场景（${pausedScenes.length}）</div>
                ${pausedScenes.length === 0
                    ? `<div class="empty" style="padding:14px 0">无暂停场景</div>`
                    : pausedScenes.map(s => `
                        <div class="scene-item scene-item-paused">
                            <span class="scene-name">${escapeHtml(s)}</span>
                            <button class="btn btn-sm scene-resume-btn" data-scene="${escapeAttr(s)}">恢复</button>
                        </div>`).join("")}
            </div>
            <div class="modal actions">
                <button class="btn btn-ghost" id="scene-close">关闭</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#scene-close").addEventListener("click", close);
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
    // 切换多线程
    mask.querySelector("#mt-toggle").addEventListener("change", async (e) => {
        const enabled = e.target.checked;
        try {
            await fetch(`/api/projects/${pid}/multithread`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ enabled }),
            });
            const modeEl = document.getElementById("wb-mode");
            if (modeEl) modeEl.textContent = enabled ? "多线程" : "单线程";
            const rotWrap = document.getElementById("wb-rotation-wrap");
            if (rotWrap) rotWrap.style.display = enabled ? "" : "none";
            close();
            showSceneManagerModal(pid);  // 重新打开以刷新列表
        } catch (err) { alert(err.message); }
    });
    // 恢复暂停场景
    mask.querySelectorAll(".scene-resume-btn").forEach(btn => {
        btn.addEventListener("click", async () => {
            const sceneName = btn.dataset.scene;
            try {
                const res = await fetch(`/api/projects/${pid}/scenes/resume`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ scene_name: sceneName }),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "恢复失败");
                const sceneEl = document.getElementById("wb-scene");
                if (sceneEl) sceneEl.textContent = sceneName;
                close();
                showSceneManagerModal(pid);
            } catch (err) { alert(err.message); }
        });
    });
}

function handleStreamEvent(raw) {
    let ev;
    try { ev = JSON.parse(raw); } catch { return; }
    const stream = document.getElementById("wb-stream");
    if (!stream) return;
    const t = ev.type;

    if (t === "round_start") {
        document.getElementById("wb-round").textContent = ev.round;
        document.getElementById("wb-words").textContent = ev.word_count;
        if (ev.scene) document.getElementById("wb-scene").textContent = ev.scene;
        document.getElementById("wb-candidates").textContent = (ev.candidates || []).join("、") || "-";
        const sep = document.createElement("div");
        sep.className = "round-sep";
        sep.textContent = `—— 第 ${ev.round} 轮 · 场景：${ev.scene || ""} ——`;
        stream.appendChild(sep);
        // 多线程轮转状态更新
        if (ev.multithread && ev.scene_switch_rounds) {
            const wrap = document.getElementById("wb-rotation-wrap");
            if (wrap) wrap.style.display = "";
            const rot = document.getElementById("wb-rotation");
            if (rot) rot.textContent = `${ev.rounds_in_current_scene || 0}/${ev.scene_switch_rounds}`;
        }
    } else if (t === "character_start") {
        wbCurrentBubble = appendBubble(ev.character);
    } else if (t === "character_chunk") {
        if (wbCurrentBubble && wbCurrentBubble.name === ev.character) {
            if (wbCurrentBubble.body.textContent === "正在思考...") {
                wbCurrentBubble.body.textContent = "";
            }
            wbCurrentBubble.body.textContent += ev.text;
        }
    } else if (t === "character_end") {
        if (wbCurrentBubble && wbCurrentBubble.name === ev.character) {
            wbCurrentBubble.body.textContent = ev.full_text;
            wbCurrentBubble.el.classList.remove("thinking");
        }
        document.getElementById("wb-words").textContent = ev.word_count;
        wbCurrentBubble = null;
    } else if (t === "character_silent") {
        appendNotice(`【${ev.character}】选择了沉默`, "silent");
        wbCurrentBubble = null;
    } else if (t === "narrator_start") {
        wbNarratorBody = appendNarrator();
    } else if (t === "narrator_end") {
        if (wbNarratorBody) wbNarratorBody.textContent = ev.text;
        wbNarratorBody = null;
    } else if (t === "scene_change") {
        appendNotice(`场景切换：${ev.character} → ${ev.new_scene}`, "scene");
        document.getElementById("wb-scene").textContent = ev.new_scene;
    } else if (t === "scene_pause") {
        // Task 11：单线程模式旧场景暂停提示
        appendNotice(`${ev.message || "场景已暂停"}：${ev.scene}`, "warning");
    } else if (t === "scene_switch") {
        // Task 11：多线程场景轮转提示
        appendNotice(`多线程轮转：切换至场景 [${ev.new_scene}]`, "scene");
        document.getElementById("wb-scene").textContent = ev.new_scene;
    } else if (t === "cross_scene_at") {
        // Task 11：跨场景 @ 传递结果
        const okText = ev.success ? "已传递" : "已忽略";
        appendNotice(`跨场景 @${ev.target}（${ev.from_scene}→${ev.to_scene}）${okText}：${ev.reason} [${ev.method}]`, ev.success ? "scene" : "warning");
    } else if (t === "new_character_detected") {
        appendNotice(`检测到新角色：${ev.name}（${ev.description || ""}）`, "newchar");
    } else if (t === "chapter_end") {
        appendNotice(`章节结束：${reasonText(ev.reason)}`, "end");
        closeWbStream();
        chatStreaming = false;
        updateChatStatus();
    } else if (t === "soft_limit") {
        const mx = ev.target && ev.target.max ? ev.target.max : "?";
        appendNotice(`字数已达软上限（${ev.word_count} / ${mx}）`, "soft");
    } else if (t === "deduction_complete") {
        appendNotice(`推演完成：共 ${ev.total_rounds} 轮，${ev.word_count} 字`, "end");
        closeWbStream();
        chatStreaming = false;
        updateChatStatus();
        if (currentWorkbenchPid) loadFinalization(currentWorkbenchPid);
    } else if (t === "error") {
        appendNotice(ev.message || "错误", "error");
    } else if (t === "warning") {
        appendNotice(ev.message || "警告", "warning");
    }
    stream.scrollTop = stream.scrollHeight;
}

function reasonText(r) {
    return ({ user_stop: "用户喊停", silent_2_rounds: "连续 2 轮沉默" })[r] || r;
}

function appendBubble(name) {
    const stream = document.getElementById("wb-stream");
    const el = document.createElement("div");
    el.className = "msg-bubble thinking";
    el.dataset.name = name;
    const head = document.createElement("div");
    head.className = "msg-head";
    head.textContent = `【${name}】正在思考...`;
    const body = document.createElement("div");
    body.className = "msg-body";
    body.textContent = "正在思考...";
    el.appendChild(head);
    el.appendChild(body);
    stream.appendChild(el);
    return { name, body, el };
}

function appendNarrator() {
    const stream = document.getElementById("wb-stream");
    const el = document.createElement("div");
    el.className = "msg-narrator";
    const body = document.createElement("div");
    body.className = "msg-body";
    body.textContent = "（叙事者描写中...）";
    el.appendChild(body);
    stream.appendChild(el);
    return body;
}

function appendNotice(text, kind) {
    const stream = document.getElementById("wb-stream");
    if (!stream) return;
    const el = document.createElement("div");
    el.className = `msg-notice msg-notice-${kind}`;
    el.textContent = text;
    stream.appendChild(el);
}


// ---------- LLM 设置（供应商管理 + agent 绑定） ----------
// R17: 配置存浏览器 localStorage，AES-GCM 加密（密钥派生自本机 MachineGuid）。
// 启动时前端把解密后的配置推送到后端内存（进程生命周期内有效，重启需重推）。

const LLM_AGENTS = [
    { key: "dialogue", name: "对话 AI" },
    { key: "character", name: "角色 AI" },
    { key: "gm", name: "群主 AI" },
    { key: "inspector", name: "检察员" },
    { key: "narrator", name: "叙事者" },
];

const LLM_DEFAULT_BASE_URLS = {
    openai: "https://api.openai.com/v1",
    claude: "https://api.anthropic.com",
    gemini: "https://generativelanguage.googleapis.com/v1beta",
};

const LLM_STORAGE_KEY = "novel_llm_config_v1";
let _cachedMachineId = null;
let _cachedAesKey = null;

// ---------- 加密工具（Web Crypto API AES-GCM） ----------

async function getMachineId() {
    if (_cachedMachineId) return _cachedMachineId;
    const res = await fetch("/api/machine_id");
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "获取机器码失败");
    _cachedMachineId = data.machine_id;
    return _cachedMachineId;
}

async function getAesKey() {
    if (_cachedAesKey) return _cachedAesKey;
    const mid = await getMachineId();
    const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(mid));
    _cachedAesKey = await crypto.subtle.importKey(
        "raw", hash, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]
    );
    return _cachedAesKey;
}

function _b64encode(buf) {
    return btoa(String.fromCharCode(...new Uint8Array(buf)));
}

function _b64decode(s) {
    return Uint8Array.from(atob(s), c => c.charCodeAt(0)).buffer;
}

async function encryptConfig(obj) {
    const key = await getAesKey();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const data = new TextEncoder().encode(JSON.stringify(obj));
    const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, data);
    return { iv: _b64encode(iv), ct: _b64encode(ct) };
}

async function decryptConfig(blob) {
    if (!blob || !blob.iv || !blob.ct) return null;
    const key = await getAesKey();
    const iv = new Uint8Array(_b64decode(blob.iv));
    const data = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, _b64decode(blob.ct));
    return JSON.parse(new TextDecoder().decode(data));
}

// ---------- localStorage 存取 + 后端推送 ----------

async function loadStoredConfig() {
    try {
        const raw = localStorage.getItem(LLM_STORAGE_KEY);
        if (!raw) return { providers: [], bindings: {} };
        const cfg = await decryptConfig(JSON.parse(raw));
        return cfg || { providers: [], bindings: {} };
    } catch (e) {
        console.warn("loadStoredConfig 失败:", e);
        return { providers: [], bindings: {} };
    }
}

async function saveStoredConfig(cfg) {
    const blob = await encryptConfig(cfg);
    localStorage.setItem(LLM_STORAGE_KEY, JSON.stringify(blob));
}

async function pushConfigToBackend(cfg) {
    const res = await fetch("/api/llm/config", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cfg)
    });
    if (!res.ok) throw new Error("推送配置到后端失败");
}

// ---------- 设置面板 UI（供应商列表 + agent 绑定） ----------

async function renderSettingsPanel(pid) {
    const view = document.querySelector('.panel-view[data-panel-view="settings"]');
    if (!view) return;
    view.innerHTML = `
        <div class="page-header">
            <h2>设置</h2>
        </div>
        <div class="settings-tabs" style="display:flex;gap:4px;border-bottom:1px solid #e5e7eb;margin-bottom:12px">
            <button class="btn btn-ghost settings-tab" data-tab="llm">LLM 配置</button>
            <button class="btn btn-ghost settings-tab" data-tab="prompts">提示词</button>
        </div>
        <div id="settings-tab-llm"></div>
        <div id="settings-tab-prompts" style="display:none"></div>
    `;
    // LLM 配置 tab 内容
    const llmView = document.getElementById("settings-tab-llm");
    llmView.innerHTML = `
        <div style="display:flex;justify-content:flex-end;margin-bottom:8px">
            <button class="btn" id="llm-save-all">保存全部</button>
        </div>
        <div id="llm-settings"><div class="empty">加载中...</div></div>
    `;
    let cfg = await loadStoredConfig();
    if (!cfg.providers) cfg.providers = [];
    if (!cfg.bindings) cfg.bindings = {};
    currentLlmCfg = cfg;  // R21: 暴露给 sendChatMessage 前置推送
    renderLlmSettings(cfg);
    document.getElementById("llm-save-all").addEventListener("click", () => saveAllLlmConfig(cfg));

    // R22: 二级 tab 切换
    const llmTab = view.querySelector('.settings-tab[data-tab="llm"]');
    const promptsTab = view.querySelector('.settings-tab[data-tab="prompts"]');
    llmTab.classList.add("active");
    llmTab.addEventListener("click", () => {
        llmTab.classList.add("active");
        promptsTab.classList.remove("active");
        document.getElementById("settings-tab-llm").style.display = "";
        document.getElementById("settings-tab-prompts").style.display = "none";
    });
    promptsTab.addEventListener("click", () => {
        promptsTab.classList.add("active");
        llmTab.classList.remove("active");
        document.getElementById("settings-tab-llm").style.display = "none";
        document.getElementById("settings-tab-prompts").style.display = "";
        renderPromptsPanel(pid);
    });
}

// R22 自定义提示词：11 个 key（每个 agent 函数独立）+ 全局/项目作用域切换
const PROMPT_AGENT_KEYS = [
    "dialogue", "character", "gm",
    "inspector_validate", "inspector_arbitrate", "inspector_judge", "inspector_extract",
    "narrator_describe", "narrator_transition", "narrator_summarize", "narrator_finale",
];
const PROMPT_AGENT_LABELS = {
    dialogue: "对话员",
    character: "角色 AI",
    gm: "群主 AI",
    inspector_validate: "检察员·校验",
    inspector_arbitrate: "检察员·仲裁",
    inspector_judge: "检察员·判定",
    inspector_extract: "检察员·提取",
    narrator_describe: "叙事者·描写",
    narrator_transition: "叙事者·过渡",
    narrator_summarize: "叙事者·摘要",
    narrator_finale: "叙事者·完结",
};

async function renderPromptsPanel(pid) {
    const view = document.getElementById("settings-tab-prompts");
    if (!view) return;
    view.innerHTML = `<div class="empty">加载中...</div>`;

    // 并行加载：默认 + 全局 + 项目
    const [defaults, globalData, projectResp] = await Promise.all([
        fetch("/api/prompts/defaults").then(r => r.json()).catch(() => ({})),
        fetch("/api/prompts/global").then(r => r.json()).catch(() => ({})),
        pid ? fetch(`/api/projects/${pid}/prompts`).then(r => r.json()).catch(() => ({})) : Promise.resolve({project: {}}),
    ]);

    let scope = pid ? "project" : "global";  // 默认作用域：有项目则项目，否则全局
    let currentAgent = "dialogue";
    // 编辑中的值（未保存）：{global: {...}, project: {...}}
    const edits = {
        global: {...globalData},
        project: {...(projectResp.project || {})},
    };
    // R22 修复：记录上次 render 显示的值，切换 agent/scope 时只有 textarea 有改动才暂存
    // 避免恢复默认后切换 agent 把默认值存回 edits 导致误标"已自定义"
    let lastShowValue = "";

    function render() {
        const currentEdits = edits[scope];
        const isOverridden = currentAgent in currentEdits;
        // 显示优先级：当前 scope > fallback（项目→全局→默认；全局→默认）
        const scopedValue = currentEdits[currentAgent] || "";
        const fallbackValue = scope === "project"
            ? (edits.global[currentAgent] || defaults[currentAgent] || "")
            : (defaults[currentAgent] || "");
        const showValue = scopedValue || fallbackValue;
        lastShowValue = showValue;  // 记录当前显示值，供切换时检测改动

        // 状态徽章
        let statusHtml = "";
        if (isOverridden) {
            statusHtml = `<span class="prompt-status-badge prompt-status-overridden">已自定义（${scope === 'project' ? '项目' : '全局'}）</span>`;
        } else if (scope === "project" && currentAgent in edits.global) {
            statusHtml = `<span class="prompt-status-badge prompt-status-inherited">继承全局自定义</span>`;
        } else {
            statusHtml = `<span class="prompt-status-badge prompt-status-default">使用代码默认</span>`;
        }

        view.innerHTML = `
            <div class="prompt-agent-tabs">
                ${PROMPT_AGENT_KEYS.map(k => {
                    let dot = "";
                    if (k in edits.project) dot = `<span class="dot dot-project" title="项目已自定义"></span>`;
                    else if (k in edits.global) dot = `<span class="dot dot-global" title="全局已自定义"></span>`;
                    return `<button class="btn btn-ghost prompt-agent-tab ${k === currentAgent ? 'active' : ''}" data-agent="${k}">${PROMPT_AGENT_LABELS[k]}${dot}</button>`;
                }).join("")}
            </div>
            <div class="prompt-scope-bar">
                <span style="font-weight:500;font-size:13px">作用域：</span>
                <label><input type="radio" name="prompt-scope" value="global" ${scope === 'global' ? 'checked' : ''}> <span>全局</span></label>
                <label class="${pid ? '' : 'disabled'}"><input type="radio" name="prompt-scope" value="project" ${scope === 'project' ? 'checked' : ''} ${pid ? '' : 'disabled'}> <span>项目${pid ? '' : '（未选中）'}</span></label>
                ${statusHtml}
            </div>
            <div class="prompt-toolbar">
                <span class="prompt-toolbar-label">编辑 ${PROMPT_AGENT_LABELS[currentAgent]} 的 prompt（完全替换式覆盖）</span>
                <div class="prompt-toolbar-actions">
                    <button class="btn btn-sm btn-ghost" id="prompt-reset-default">恢复默认</button>
                    <button class="btn btn-sm" id="prompt-save">保存</button>
                </div>
            </div>
            <textarea class="prompt-editor" id="prompt-editor">${escapeHtml(showValue)}</textarea>
            <div class="prompt-help">
                每个 agent 函数独立一份 prompt，完全替换式覆盖。<br>
                自定义时请保留各函数需要的输出格式要求（如完结总结仍需返回 JSON）。<br>
                优先级：项目 &gt; 全局 &gt; 代码默认。项目提示词随工程文件 zip 导出。
            </div>
        `;

        // 绑定 agent tab 切换
        view.querySelectorAll(".prompt-agent-tab").forEach(btn => {
            btn.addEventListener("click", () => {
                const ta = document.getElementById("prompt-editor");
                if (ta && ta.value !== lastShowValue) edits[scope][currentAgent] = ta.value;  // 仅当改动才暂存
                currentAgent = btn.dataset.agent;
                render();
            });
        });
        // 作用域切换
        view.querySelectorAll('input[name="prompt-scope"]').forEach(r => {
            r.addEventListener("change", () => {
                const ta = document.getElementById("prompt-editor");
                if (ta && ta.value !== lastShowValue) edits[scope][currentAgent] = ta.value;  // 仅当改动才暂存
                scope = r.value;
                render();
            });
        });
        // 保存（单 key merge）
        document.getElementById("prompt-save").addEventListener("click", async () => {
            const ta = document.getElementById("prompt-editor");
            if (ta) edits[scope][currentAgent] = ta.value;
            const text = edits[scope][currentAgent] || "";
            const body = {[currentAgent]: text};
            const url = scope === "global" ? "/api/prompts/global" : `/api/projects/${pid}/prompts`;
            try {
                const res = await fetch(url, {
                    method: "PUT",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify(body),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "保存失败");
                // 同步本地 edits（空字符串删除 key）
                if (text.trim()) {
                    edits[scope][currentAgent] = text;
                } else {
                    delete edits[scope][currentAgent];
                }
                alert(`${PROMPT_AGENT_LABELS[currentAgent]} ${scope === 'global' ? '全局' : '项目'} prompt 已保存`);
                render();
            } catch (e) { alert(e.message); }
        });
        // 恢复默认（删除当前 scope 的该 key）
        document.getElementById("prompt-reset-default").addEventListener("click", async () => {
            if (!confirmDestructive(`恢复默认将清除 ${scope === 'global' ? '全局' : '项目'} 的 ${PROMPT_AGENT_LABELS[currentAgent]} 自定义 prompt，确认？`)) return;
            const body = {[currentAgent]: ""};  // 空字符串触发删除
            const url = scope === "global" ? "/api/prompts/global" : `/api/projects/${pid}/prompts`;
            try {
                const res = await fetch(url, {
                    method: "PUT",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify(body),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "恢复失败");
                delete edits[scope][currentAgent];
                alert(`${PROMPT_AGENT_LABELS[currentAgent]} 已恢复默认`);
                render();
            } catch (e) { alert(e.message); }
        });
    }

    render();
}

// R18: 引导模态 — 首屏无供应商时 / 新建项目前未配置时弹出
async function openLlmSettingsModal() {
    const cfg = await loadStoredConfig();
    if (!cfg.providers) cfg.providers = [];
    if (!cfg.bindings) cfg.bindings = {};
    currentLlmCfg = cfg;  // R21: 暴露给 sendChatMessage 前置推送
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal" style="width:800px;max-width:90vw;max-height:90vh;overflow-y:auto">
            <div class="page-header">
                <h2>LLM 配置</h2>
                <button class="btn" id="llm-save-all">保存全部</button>
            </div>
            <div id="llm-settings"><div class="empty">加载中...</div></div>
            <div class="modal actions" style="margin-top:12px">
                <button class="btn btn-ghost" id="llm-modal-close">关闭</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#llm-modal-close").addEventListener("click", close);
    // R18 修复：用 mousedown 而非 click，避免 select 下拉选项点选时 click 事件落到 mask 触发关闭
    mask.addEventListener("mousedown", (e) => { if (e.target === mask) close(); });
    renderLlmSettings(cfg);
    mask.querySelector("#llm-save-all").addEventListener("click", () => saveAllLlmConfig(cfg));
}

function renderLlmSettings(cfg) {
    const el = document.getElementById("llm-settings");
    el.innerHTML = `
        <div class="llm-section">
            <div class="llm-section-header" style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
                <h3>供应商</h3>
                <button class="btn btn-sm" id="llm-add-provider">+ 新增</button>
            </div>
            <div id="llm-provider-list"></div>
        </div>
        <div class="llm-section" style="margin-top:16px">
            <h3>Agent 绑定</h3>
            <div id="llm-agent-bindings"></div>
        </div>
    `;
    renderProviderList(cfg);
    renderAgentBindings(cfg);
    document.getElementById("llm-add-provider").addEventListener("click", () => {
        const id = "p_" + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
        cfg.providers.push({
            id, name: "新供应商", provider: "openai", model: "",
            api_key: "", base_url: "", temperature: 0.7, max_tokens: 4096
        });
        renderProviderList(cfg);
        renderAgentBindings(cfg);
    });
}

function renderProviderList(cfg) {
    const el = document.getElementById("llm-provider-list");
    if (!cfg.providers.length) {
        el.innerHTML = `<div class="empty">尚未创建供应商，点"+ 新增"添加</div>`;
        return;
    }
    el.innerHTML = cfg.providers.map((p, i) => `
        <details class="agent-block provider-card" data-provider-idx="${i}"${!p.api_key ? " open" : ""}>
            <summary class="provider-summary" style="display:flex;align-items:center;gap:8px;cursor:pointer">
                <span style="flex:1">
                    <strong>${escapeHtml(p.name || "未命名")}</strong>
                    <span style="color:#6b7280;font-size:13px;margin-left:6px">[${p.provider} / ${escapeHtml(p.model || "?")}]${p.api_key ? "" : " ⚠无 API Key"}</span>
                </span>
                <button class="btn btn-ghost btn-sm p-test" type="button">测试</button>
                <button class="btn btn-ghost btn-sm p-del" type="button">删除</button>
            </summary>
            <div class="provider-fields" style="margin-top:8px">
                <div class="agent-row"><label>名称</label><input class="p-name" type="text" value="${escapeHtml(p.name || "")}" placeholder="如：OpenAI 主号"></div>
                <div class="agent-row"><label>API 格式</label><select class="p-provider">
                    <option value="openai" ${p.provider === "openai" ? "selected" : ""}>OpenAI 兼容</option>
                    <option value="claude" ${p.provider === "claude" ? "selected" : ""}>Claude</option>
                    <option value="gemini" ${p.provider === "gemini" ? "selected" : ""}>Gemini</option>
                </select></div>
                <div class="agent-row"><label>Model</label><input class="p-model" type="text" value="${escapeHtml(p.model || "")}" placeholder="如 gpt-4o-mini"></div>
                <div class="agent-row"><label>API Key</label><input class="p-api-key" type="password" value="${escapeHtml(p.api_key || "")}" placeholder="sk-..."></div>
                <div class="agent-row"><label>Base URL</label><input class="p-base-url" type="text" value="${escapeHtml(p.base_url || "")}" placeholder="${LLM_DEFAULT_BASE_URLS[p.provider] || ""}"></div>
                <div class="agent-row" style="display:flex;gap:16px">
                    <div style="display:flex;align-items:center;gap:6px"><label style="margin:0">Temperature</label><input class="p-temperature" type="number" min="0" max="2" step="0.1" value="${p.temperature != null ? p.temperature : 0.7}" style="max-width:100px"></div>
                    <div style="display:flex;align-items:center;gap:6px"><label style="margin:0">Max Tokens</label><input class="p-max-tokens" type="number" min="1" step="1" value="${p.max_tokens != null ? p.max_tokens : 4096}" style="max-width:100px"></div>
                </div>
                <div class="agent-row"><span class="test-result" style="margin-left:0"></span></div>
            </div>
        </details>
    `).join("");
    cfg.providers.forEach((p, i) => {
        const block = el.querySelector(`[data-provider-idx="${i}"]`);
        block.querySelector(".p-provider").addEventListener("change", (e) => {
            block.querySelector(".p-base-url").placeholder = LLM_DEFAULT_BASE_URLS[e.target.value] || "";
        });
        // R19 修复：阻止 summary 内按钮点击触发 details toggle
        block.querySelector(".p-test").addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            testProvider(block);
        });
        block.querySelector(".p-del").addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (!confirm(`删除供应商 "${p.name || ""}"？`)) return;
            const pid = cfg.providers[i].id;
            cfg.providers.splice(i, 1);
            Object.keys(cfg.bindings).forEach(k => {
                if (cfg.bindings[k] === pid) delete cfg.bindings[k];
            });
            renderProviderList(cfg);
            renderAgentBindings(cfg);
            saveAllLlmConfigDebounced(cfg);  // R21: 删除后自动保存
        });
        // R21: 所有 input/select 的 change 事件触发自动保存（无弹窗）
        block.querySelectorAll("input, select").forEach(inp => {
            inp.addEventListener("change", () => saveAllLlmConfigDebounced(cfg));
        });
    });
}

function renderAgentBindings(cfg) {
    const el = document.getElementById("llm-agent-bindings");
    // R21 修复：清理 bindings 中引用已不存在 provider id 的项（避免刷新后 select 显示"未绑定"误导）
    const validIds = new Set(cfg.providers.map(p => p.id));
    let dirty = false;
    Object.keys(cfg.bindings).forEach(k => {
        if (!validIds.has(cfg.bindings[k])) {
            delete cfg.bindings[k];
            dirty = true;
        }
    });
    el.innerHTML = LLM_AGENTS.map(a => `
        <div class="agent-row">
            <label>${escapeHtml(a.name)}</label>
            <select class="ab-select" data-agent-key="${a.key}">
                <option value="">（未绑定）</option>
                ${cfg.providers.map(p => `
                    <option value="${escapeHtml(p.id)}" ${cfg.bindings[a.key] === p.id ? "selected" : ""}>
                        ${escapeHtml(p.name || p.id)} [${p.provider}/${escapeHtml(p.model || "?")}]
                    </option>
                `).join("")}
            </select>
        </div>
    `).join("");
    el.querySelectorAll(".ab-select").forEach(sel => {
        sel.addEventListener("change", (e) => {
            const k = e.target.dataset.agentKey;
            const v = e.target.value;
            if (v) cfg.bindings[k] = v;
            else delete cfg.bindings[k];
            saveAllLlmConfigDebounced(cfg);  // R21: 绑定变更自动保存
        });
    });
    // R21: 清理了无效 bindings 后立即保存（避免下次刷新又遇到）
    if (dirty) saveAllLlmConfig(cfg, true);  // 显式清理直接保存，不防抖
}

async function testProvider(block) {
    const result = block.querySelector(".test-result");
    result.textContent = "测试中...";
    result.className = "test-result testing";
    const config = {
        provider: block.querySelector(".p-provider").value,
        model: block.querySelector(".p-model").value.trim(),
        api_key: block.querySelector(".p-api-key").value.trim(),
        base_url: block.querySelector(".p-base-url").value.trim(),
        temperature: parseFloat(block.querySelector(".p-temperature").value) || 0.7,
        max_tokens: parseInt(block.querySelector(".p-max-tokens").value, 10) || 4096,
    };
    try {
        const res = await fetch("/api/llm/test", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ config })
        });
        const data = await res.json();
        if (data.ok) {
            result.textContent = "成功: " + (data.text || "").slice(0, 100);
            result.className = "test-result ok";
        } else {
            result.textContent = "失败: " + (data.error || "");
            result.className = "test-result fail";
        }
    } catch (e) {
        result.textContent = "失败: " + e.message;
        result.className = "test-result fail";
    }
}

// M9 修复：自动保存的防抖包装（500ms 内连续修改只触发一次）
function saveAllLlmConfigDebounced(cfg) {
    if (_saveAllLlmConfigTimer) clearTimeout(_saveAllLlmConfigTimer);
    _saveAllLlmConfigTimer = setTimeout(() => {
        _saveAllLlmConfigTimer = null;
        saveAllLlmConfig(cfg, true);
    }, 500);
}

async function saveAllLlmConfig(cfg, silent = false) {
    // 从 DOM 读回所有供应商当前编辑值
    const list = document.getElementById("llm-provider-list");
    if (!list) return;
    const blocks = list.querySelectorAll("[data-provider-idx]");
    if (!blocks.length) return;
    cfg.providers = Array.from(blocks).map(b => {
        const idx = parseInt(b.dataset.providerIdx, 10);
        return {
            id: cfg.providers[idx].id,
            name: b.querySelector(".p-name").value.trim(),
            provider: b.querySelector(".p-provider").value,
            model: b.querySelector(".p-model").value.trim(),
            api_key: b.querySelector(".p-api-key").value.trim(),
            base_url: b.querySelector(".p-base-url").value.trim(),
            temperature: parseFloat(b.querySelector(".p-temperature").value) || 0.7,
            max_tokens: parseInt(b.querySelector(".p-max-tokens").value, 10) || 4096,
        };
    });
    try {
        await saveStoredConfig(cfg);
        await pushConfigToBackend(cfg);
        if (!silent) alert("已保存并推送至后端");
    } catch (e) {
        if (!silent) alert("保存失败: " + e.message);
        else console.warn("自动保存失败:", e);
    }
}


// ---------- 阶段 2：定稿流程 ----------

let finState = null;  // 当前定稿流程状态对象

async function loadFinalization(pid) {
    // R7：定稿视图写入 #fin-panel-body（右侧定稿面板），并自动切换至该面板
    switchPanel(pid, "finalization");
    const stream = document.getElementById("fin-panel-body") || document.getElementById("wb-stream");
    if (!stream) return;
    stream.innerHTML = `<div class="empty">正在校验本章...</div>`;
    finState = {
        pid,
        step: "validating",
        chapterText: "",
        conflicts: [],
        passed: "",
        feedback: {},        // {conflict_index: {action, instruction?}}
        editingIdx: null,    // 当前正在编辑指令的冲突号
        modifiedText: "",
        editLog: [],
        affectedRanges: [],
        revalidateResult: null,
        finalizeResult: null,
        newCharacters: [],
        acceptedChars: new Set(),
        rejectedChars: new Set(),
        keyEventsCheck: null,  // 阶段 6：无关键事件检测结果
        error: "",
    };
    try {
        const wsRes = await fetch(`/api/projects/${pid}/chapter/workspace`);
        // M12 修复：未校验 wsRes.ok 时 ws 可能是错误对象，ws.active undefined 走 falsy 掩盖真实错误
        if (!wsRes.ok) throw new Error(`workspace 加载失败 (HTTP ${wsRes.status})`);
        const ws = await wsRes.json();
        if (!ws.active) {
            // R21 修复：无活动工作区时显示引导（而非错误），引导用户去预览 tab 查看已定稿章节
            finState.error = "no_workspace";
            renderFinalizationPanel();
            return;
        }
        finState.newCharacters = ws.new_characters_queue || [];
        const vRes = await fetch(`/api/projects/${pid}/chapter/validate`, { method: "POST" });
        const vData = await vRes.json();
        if (!vRes.ok) throw new Error(vData.error || "校验失败");
        finState.chapterText = vData.chapter_text || "";
        finState.conflicts = vData.conflicts || [];
        finState.passed = vData.passed || "";
        finState.step = "review";
        // 阶段 6：定稿前关键事件检测
        try {
            const keRes = await fetch(`/api/projects/${pid}/assistant/key-events-check`);
            if (keRes.ok) finState.keyEventsCheck = await keRes.json();
        } catch (e) { /* ignore */ }
        renderFinalizationPanel();
    } catch (e) {
        finState.error = e.message || String(e);
        renderFinalizationPanel();
    }
}

function renderFinalizationPanel() {
    const stream = document.getElementById("fin-panel-body") || document.getElementById("wb-stream");
    if (!stream) return;
    // R21 修复：finState 未初始化时显示入口按钮，点击触发 loadFinalization
    // （loadFinalization 内部会 fetch workspace 检查 active，无活动工作区显示错误，有则触发校验）
    if (!finState) {
        stream.innerHTML = `
            <div class="fin-panel">
                <div class="msg-notice msg-notice-scene">阶段 2 定稿</div>
                <div class="empty">点击下方按钮检查工作区并开始定稿流程</div>
                <div class="fin-actions">
                    <button class="btn" id="fin-start">检查工作区并开始定稿</button>
                </div>
            </div>`;
        const btn = document.getElementById("fin-start");
        if (btn) btn.addEventListener("click", () => {
            if (currentWorkbenchPid) loadFinalization(currentWorkbenchPid);
        });
        return;
    }
    const s = finState;
    let html = "";
    if (s.error === "no_workspace") {
        // R21 修复：无活动工作区时显示引导 + 已定稿章节列表（异步加载）
        html = `
            <div class="fin-panel">
                <div class="msg-notice msg-notice-scene">无活动工作区</div>
                <div class="empty">本章已定稿，可在【预览】tab 查看正文。要定稿新章节，请先在聊天室推演。</div>
                <div class="fin-actions">
                    <button class="btn" id="fin-go-preview">前往预览</button>
                </div>
            </div>`;
        // 异步加载已定稿章节列表（供用户确认哪些章已定稿）
        fetch(`/api/projects/${s.pid}/chapters`)
            .then(r => r.json())
            .then(chs => {
                if (!Array.isArray(chs) || chs.length === 0) return;
                const listHtml = chs.map(c => `
                    <div class="fin-newchar">
                        <span class="fin-name">第${c.chapter}章</span>
                        <span class="fin-desc">${c.word_count || 0} 字 · ${c.finalized !== false ? "已定稿" : "未定稿"}</span>
                    </div>`).join("");
                const panel = stream.querySelector(".fin-panel");
                if (panel) panel.insertAdjacentHTML("beforeend", `<div class="fin-section"><div class="msg-notice">已定稿章节（${chs.length}）</div>${listHtml}</div>`);
            })
            .catch(() => { /* ignore */ });
    } else if (s.error) {
        html = `
            <div class="fin-panel">
                <div class="msg-notice msg-notice-error">错误：${escapeHtml(s.error)}</div>
                <div class="fin-actions">
                    <button class="btn btn-ghost" id="fin-discard">放弃本章</button>
                </div>
            </div>`;
    } else if (s.step === "validating") {
        html = `<div class="empty">正在校验本章...</div>`;
    } else if (s.step === "review") {
        html = renderReviewStep(s);
    } else if (s.step === "preview") {
        html = renderPreviewStep(s);
    } else if (s.step === "revalidating") {
        html = `<div class="empty">二次校验中...</div>`;
    } else if (s.step === "final_confirm") {
        html = renderFinalConfirmStep(s);
    } else if (s.step === "done") {
        html = renderDoneStep(s);
    }
    stream.innerHTML = html;
    bindFinalizationEvents();
    stream.scrollTop = 0;
}

function renderReviewStep(s) {
    // 新角色确认
    let ncHtml = "";
    if (s.newCharacters.length > 0) {
        ncHtml = `
            <div class="fin-section">
                <div class="msg-notice msg-notice-newchar">检测到新角色（请确认是否接受为正式角色卡）</div>
                ${s.newCharacters.map((nc, i) => {
                    if (s.acceptedChars.has(i)) return `<div class="fin-newchar"><span class="fin-name">${escapeHtml(nc.name || "")}</span> <span class="fin-badge fin-badge-fix">已接受</span></div>`;
                    if (s.rejectedChars.has(i)) return `<div class="fin-newchar fin-ignored"><span class="fin-name">${escapeHtml(nc.name || "")}</span> <span class="fin-badge fin-badge-ignore">已拒绝</span></div>`;
                    return `
                        <div class="fin-newchar" data-nc-idx="${i}">
                            <span class="fin-name">${escapeHtml(nc.name || "")}</span>
                            <span class="fin-desc">${escapeHtml(nc.description || "")}</span>
                            <button class="btn btn-sm fin-accept" data-idx="${i}">接受</button>
                            <button class="btn btn-ghost btn-sm fin-reject" data-idx="${i}">拒绝</button>
                        </div>`;
                }).join("")}
            </div>`;
    }
    const passedHtml = s.passed ? `<div class="msg-notice msg-notice-end">通过条目摘要：${escapeHtml(s.passed)}</div>` : "";
    // 阶段 6：无关键事件提醒（count=0 时显示"保留 / 重写"按钮）
    const keCheck = s.keyEventsCheck;
    const noKeyEventsHtml = (keCheck && !keCheck.has_key_events)
        ? `<div class="msg-notice msg-notice-warning asst-no-key-events">
               ⚠ ${escapeHtml(keCheck.message || "本章无关键事件推进")}
               <button class="btn btn-sm" id="fin-keep-chapter">保留</button>
               <button class="btn btn-ghost btn-sm" id="fin-rewrite-chapter">重写</button>
           </div>`
        : "";
    const conflictsHtml = s.conflicts.length === 0
        ? `<div class="msg-notice msg-notice-end">无冲突，可直接定稿</div>`
        : s.conflicts.map((c, i) => renderConflictCard(c, i, s.feedback[i], s.editingIdx === i)).join("");
    const allHandled = s.conflicts.every((_, i) => s.feedback[i]);
    const submitBtn = s.conflicts.length === 0
        ? `<button class="btn" id="fin-direct-finalize">直接定稿</button>`
        : `<button class="btn" id="fin-submit-feedback" ${allHandled ? "" : "disabled"}>提交反馈</button>`;
    return `
        <div class="fin-panel">
            <div class="msg-notice msg-notice-scene">阶段 2 定稿 · 初稿校验清单</div>
            ${ncHtml}
            ${noKeyEventsHtml}
            ${passedHtml}
            <div class="fin-section">${conflictsHtml}</div>
            <div class="fin-actions">
                ${submitBtn}
                <button class="btn btn-ghost" id="fin-discard">放弃本章</button>
            </div>
        </div>`;
}

function renderConflictCard(c, idx, feedback, isEditing) {
    const handled = feedback !== undefined;
    const action = feedback && feedback.action;
    let badge = "";
    if (action === "fix") badge = ` <span class="fin-badge fin-badge-fix">已修改</span>`;
    else if (action === "ignore") badge = ` <span class="fin-badge fin-badge-ignore">已忽略</span>`;
    let editorHtml = "";
    if (isEditing) {
        editorHtml = renderInstructionEditor(idx, feedback && feedback.instruction);
    } else if (handled && action === "fix" && feedback.instruction) {
        const ins = feedback.instruction;
        editorHtml = `
            <div class="fin-instruction-display">
                修改指令：target=${escapeHtml(ins.target || "")} / position=${escapeHtml(ins.position || "")} / operation=${escapeHtml(ins.operation || "")} / content=${escapeHtml((ins.content || "").slice(0, 30))}
                <button class="btn btn-ghost btn-sm fin-edit-instruction" data-idx="${idx}">编辑</button>
            </div>`;
    }
    // 阶段 6：宣称冲突 → "选择真相"按钮
    const isClaim = c.type === "无依据宣称";
    const claimBtn = isClaim
        ? `<button class="btn btn-sm fin-claim-resolve" data-idx="${idx}">选择真相</button>`
        : "";
    // 阶段 6：历史忽略提醒（同类型冲突曾被忽略过）
    const similarIgnored = c.similar_ignored || [];
    const similarHtml = similarIgnored.length > 0
        ? `<div class="msg-notice msg-notice-warning asst-similar-ignored">⚠ 您之前曾忽略过类似问题（${similarIgnored.length} 条），请再次确认</div>`
        : "";
    return `
        <div class="contradiction fin-conflict ${action === "ignore" ? "fin-ignored" : ""}" data-idx="${idx}">
            <div><span class="tag-red">${escapeHtml(c.type || "")}</span> ${escapeHtml(c.location || "")}${badge}</div>
            <div class="fin-original">原文：${escapeHtml(c.original || "")}</div>
            <div class="suggest">建议：${escapeHtml(c.suggestion || "")}</div>
            ${similarHtml}
            ${editorHtml}
            ${!handled ? `
                <div class="fin-conflict-actions">
                    <button class="btn btn-sm fin-fix" data-idx="${idx}">修改</button>
                    <button class="btn btn-ghost btn-sm fin-ignore" data-idx="${idx}">忽略</button>
                    ${claimBtn}
                </div>` : (claimBtn ? `<div class="fin-conflict-actions">${claimBtn}</div>` : "")}
        </div>`;
}

function renderInstructionEditor(idx, existing) {
    const ins = existing || {};
    return `
        <div class="fin-instruction-editor">
            <div class="fin-row"><label>Target</label><input type="text" class="ins-target" placeholder="如 段3" value="${escapeAttr(ins.target || "")}"></div>
            <div class="fin-row"><label>Position</label><input type="text" class="ins-position" placeholder="位置描述" value="${escapeAttr(ins.position || "")}"></div>
            <div class="fin-row">
                <label>Operation</label>
                <select class="ins-operation">
                    <option value="replace" ${ins.operation === "replace" ? "selected" : ""}>replace</option>
                    <option value="insert" ${ins.operation === "insert" ? "selected" : ""}>insert</option>
                    <option value="delete" ${ins.operation === "delete" ? "selected" : ""}>delete</option>
                </select>
            </div>
            <div class="fin-row"><label>Content</label><textarea class="ins-content" rows="2" placeholder="修改内容">${escapeHtml(ins.content || "")}</textarea></div>
            <div class="fin-conflict-actions">
                <button class="btn btn-sm fin-confirm-instruction" data-idx="${idx}">确认修改</button>
                <button class="btn btn-ghost btn-sm fin-cancel-instruction" data-idx="${idx}">取消</button>
            </div>
        </div>`;
}

function renderPreviewStep(s) {
    const editLogHtml = s.editLog.length === 0
        ? `<div class="msg-notice">无修改记录</div>`
        : s.editLog.map(log => `
            <div class="contradiction">
                <div>冲突 #${log.conflict_index}：${log.operations.length} 项操作</div>
                ${log.transition_added ? `<div class="suggest">过渡句：${escapeHtml(log.transition_added)}</div>` : ""}
            </div>`).join("");
    return `
        <div class="fin-panel">
            <div class="msg-notice msg-notice-scene">修改预览</div>
            <div class="fin-section">
                <div class="msg-notice msg-notice-end">修改日志</div>
                ${editLogHtml}
            </div>
            <div class="fin-section">
                <div class="msg-notice msg-notice-scene">修改后正文</div>
                <div class="msg-bubble"><div class="msg-body">${escapeHtml(s.modifiedText)}</div></div>
            </div>
            <div class="fin-actions">
                <button class="btn" id="fin-confirm-modified">确认（进入二次校验）</button>
                <button class="btn btn-ghost" id="fin-back-to-review">再改</button>
                <button class="btn btn-ghost" id="fin-discard">放弃本章</button>
            </div>
        </div>`;
}

function renderFinalConfirmStep(s) {
    const r = s.revalidateResult || {};
    const passedHtml = r.passed
        ? `<div class="msg-notice msg-notice-end">二次校验通过</div>`
        : `<div class="msg-notice msg-notice-error">二次校验发现新冲突</div>`;
    const newConflictsHtml = (r.new_conflicts || []).length === 0
        ? ""
        : (r.new_conflicts || []).map(c => `
            <div class="contradiction">
                <div><span class="tag-red">${escapeHtml(c.type || "")}</span> ${escapeHtml(c.location || "")}</div>
                <div class="fin-original">原文：${escapeHtml(c.original || "")}</div>
                <div class="suggest">建议：${escapeHtml(c.suggestion || "")}</div>
            </div>`).join("");
    return `
        <div class="fin-panel">
            ${passedHtml}
            ${newConflictsHtml}
            <div class="fin-actions">
                ${r.passed
                    ? `<button class="btn" id="fin-finalize">定稿</button>`
                    : `<button class="btn" id="fin-finalize-force">强制定稿（忽略新冲突）</button>
                       <button class="btn btn-ghost" id="fin-back-to-review">修改</button>`}
                <button class="btn btn-ghost" id="fin-discard">放弃本章</button>
            </div>
        </div>`;
}

function renderDoneStep(s) {
    const r = s.finalizeResult || {};
    const c = r.completion;
    let completionHtml = "";
    if (c) {
        if (c.error) {
            completionHtml = `<div class="msg-notice msg-notice-warning">完本检测未执行：${escapeHtml(c.error)}</div>`;
        } else if (c.all_achieved) {
            completionHtml = `<div class="msg-notice msg-notice-end">可完本！全部 ${c.total} 个完本条件已达成</div>`;
        } else {
            completionHtml = `<div class="msg-notice">已达成 ${c.achieved_count} / ${c.total} 个完本条件（本章新达成 ${(c.achieved || []).length} 个）</div>`;
        }
    }
    return `
        <div class="fin-panel">
            <div class="wiz-success">✓ 第 ${r.chapter} 章定稿完成</div>
            <div class="msg-bubble">
                <div class="msg-body">字数：${r.word_count}</div>
                <div class="msg-body">事件数：${r.events_count}</div>
                ${r.summary ? `<div class="msg-body">摘要：${escapeHtml(r.summary)}</div>` : `<div class="msg-body">摘要：（未生成）</div>`}
            </div>
            ${completionHtml}
            <div class="fin-actions">
                <button class="btn" id="fin-back-to-list">返回项目列表</button>
            </div>
        </div>`;
}

function bindFinalizationEvents() {
    const s = finState;
    if (!s) return;
    const bind = (id, handler) => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("click", handler);
    };
    bind("fin-discard", async () => {
        if (!confirm("确认放弃本章？此操作不可回退。")) return;
        try {
            // F2 修复：检查 res.ok 后再清空本地状态，避免服务端失败时前后端不一致
            await apiFetch(`/api/projects/${s.pid}/chapter/discard`, { method: "POST" });
            finState = null;
            await renderView("projects");
        } catch (e) { alert(e.message); }
    });
    // R21 修复：无活动工作区时的"前往预览"按钮
    bind("fin-go-preview", () => {
        if (currentWorkbenchPid) switchPanel(currentWorkbenchPid, "preview");
    });
    bind("fin-back-to-list", async () => {
        finState = null;
        await renderView("projects");
    });

    if (s.step === "review") {
        // 新角色接受/拒绝
        document.querySelectorAll(".fin-accept").forEach(btn => {
            btn.addEventListener("click", async () => {
                const idx = parseInt(btn.dataset.idx, 10);
                const nc = s.newCharacters[idx];
                if (!nc) return;
                try {
                    const res = await fetch(`/api/projects/${s.pid}/chapter/confirm-characters`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ confirmed: [nc] }),
                    });
                    const data = await res.json();
                    if (!res.ok) throw new Error(data.error || "确认失败");
                    s.acceptedChars.add(idx);
                    renderFinalizationPanel();
                } catch (e) { alert(e.message); }
            });
        });
        document.querySelectorAll(".fin-reject").forEach(btn => {
            btn.addEventListener("click", () => {
                const idx = parseInt(btn.dataset.idx, 10);
                s.rejectedChars.add(idx);
                renderFinalizationPanel();
            });
        });
        // 冲突修改/忽略
        document.querySelectorAll(".fin-fix").forEach(btn => {
            btn.addEventListener("click", () => {
                s.editingIdx = parseInt(btn.dataset.idx, 10);
                renderFinalizationPanel();
            });
        });
        document.querySelectorAll(".fin-ignore").forEach(btn => {
            btn.addEventListener("click", () => {
                const idx = parseInt(btn.dataset.idx, 10);
                s.feedback[idx] = { action: "ignore" };
                renderFinalizationPanel();
            });
        });
        document.querySelectorAll(".fin-edit-instruction").forEach(btn => {
            btn.addEventListener("click", () => {
                s.editingIdx = parseInt(btn.dataset.idx, 10);
                renderFinalizationPanel();
            });
        });
        document.querySelectorAll(".fin-confirm-instruction").forEach(btn => {
            btn.addEventListener("click", () => {
                const idx = parseInt(btn.dataset.idx, 10);
                const card = btn.closest(".fin-conflict");
                const instruction = {
                    target: card.querySelector(".ins-target").value.trim(),
                    position: card.querySelector(".ins-position").value.trim(),
                    operation: card.querySelector(".ins-operation").value,
                    content: card.querySelector(".ins-content").value.trim(),
                };
                if (!instruction.target) { alert("请填写 target"); return; }
                s.feedback[idx] = { action: "fix", instruction };
                s.editingIdx = null;
                renderFinalizationPanel();
            });
        });
        document.querySelectorAll(".fin-cancel-instruction").forEach(btn => {
            btn.addEventListener("click", () => {
                s.editingIdx = null;
                renderFinalizationPanel();
            });
        });
        // 阶段 6：宣称冲突 "选择真相"
        document.querySelectorAll(".fin-claim-resolve").forEach(btn => {
            btn.addEventListener("click", () => {
                const idx = parseInt(btn.dataset.idx, 10);
                const conflict = s.conflicts[idx];
                if (conflict) showClaimResolveModal(s.pid, idx, conflict);
            });
        });
        // 阶段 6：无关键事件 "保留" / "重写"
        bind("fin-keep-chapter", () => { s.keyEventsCheck = null; renderFinalizationPanel(); });
        bind("fin-rewrite-chapter", async () => {
            if (!confirm("确认放弃本章重写？此操作不可回退。")) return;
            try {
                // F2 修复：检查 res.ok 后再清空本地状态
                await apiFetch(`/api/projects/${s.pid}/chapter/discard`, { method: "POST" });
                finState = null;
                await renderView("projects");
            } catch (e) { alert(e.message); }
        });
        bind("fin-submit-feedback", () => submitFeedback(s));
        bind("fin-direct-finalize", () => directFinalize(s));
    } else if (s.step === "preview") {
        bind("fin-confirm-modified", () => revalidateChapter(s));
        bind("fin-back-to-review", () => {
            // 回到 review，清空已收集的 feedback 但保留 chapterText
            s.step = "review";
            s.feedback = {};
            s.editingIdx = null;
            renderFinalizationPanel();
        });
    } else if (s.step === "final_confirm") {
        bind("fin-finalize", () => finalizeChapter(s));
        bind("fin-finalize-force", () => finalizeChapter(s));
        bind("fin-back-to-review", () => {
            s.step = "review";
            s.feedback = {};
            s.editingIdx = null;
            renderFinalizationPanel();
        });
    }
}

async function submitFeedback(s) {
    const feedback = Object.keys(s.feedback).map(k => {
        const idx = parseInt(k, 10);
        const f = s.feedback[idx];
        return { conflict_index: idx, action: f.action, instruction: f.instruction };
    });
    s.step = "loading";
    // M8 修复：优先用 fin-panel-body（finalization tab 可见），fallback 到 wb-stream（chatroom tab）
    const stream = document.getElementById("fin-panel-body") || document.getElementById("wb-stream");
    if (stream) stream.innerHTML = `<div class="empty">正在执行修改...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/chapter/apply-feedback`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(feedback),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "修改失败");
        s.modifiedText = data.new_text || "";
        s.editLog = data.edit_log || [];
        s.affectedRanges = data.affected_ranges || [];
        s.step = "preview";
        renderFinalizationPanel();
    } catch (e) {
        s.step = "review";
        s.error = e.message;
        renderFinalizationPanel();
        s.error = "";
    }
}

async function revalidateChapter(s) {
    s.step = "revalidating";
    renderFinalizationPanel();
    try {
        const res = await fetch(`/api/projects/${s.pid}/chapter/revalidate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ new_text: s.modifiedText, affected_ranges: s.affectedRanges }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "二次校验失败");
        s.revalidateResult = data;
        s.step = "final_confirm";
        renderFinalizationPanel();
    } catch (e) {
        s.step = "preview";
        s.error = e.message;
        renderFinalizationPanel();
        s.error = "";
    }
}

async function directFinalize(s) {
    // 无冲突或用户选择直接定稿：跳过二次校验
    s.modifiedText = s.chapterText;
    s.affectedRanges = [];
    s.revalidateResult = { passed: true, new_conflicts: [] };
    s.step = "final_confirm";
    renderFinalizationPanel();
}

async function finalizeChapter(s) {
    s.step = "loading";
    // M8 修复：优先用 fin-panel-body（finalization tab 可见），fallback 到 wb-stream（chatroom tab）
    const stream2 = document.getElementById("fin-panel-body") || document.getElementById("wb-stream");
    if (stream2) stream2.innerHTML = `<div class="empty">正在定稿...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/chapter/finalize`, { method: "POST" });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "定稿失败");
        s.finalizeResult = data;
        s.step = "done";
        renderFinalizationPanel();
    } catch (e) {
        s.step = "final_confirm";
        s.error = e.message;
        renderFinalizationPanel();
        s.error = "";
    }
}


// ---------- 阶段 3：完本条件检测视图 ----------

async function renderCompletionView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目</div>`;
        return;
    }
    let meta = null;
    try {
        const res = await fetch(`/api/projects/${pid}`);
        if (res.ok) meta = await res.json();
    } catch (e) { /* ignore */ }
    const canComplete = meta && (meta.can_complete || meta.is_completed);
    const finalizeBtn = canComplete
        ? `<button class="btn" id="cmp-finalize">${meta.is_completed ? "查看完本总结" : "完本"}</button>`
        : "";
    main.innerHTML = `
        <div class="page-header">
            <h2>完本条件检测</h2>
            <div class="wb-actions">
                ${finalizeBtn}
                <button class="btn" id="cmp-recheck">手动重检</button>
                <button class="btn btn-ghost" id="cmp-back">返回项目列表</button>
            </div>
        </div>
        <div id="cmp-status"><div class="empty">加载中...</div></div>
    `;
    document.getElementById("cmp-back").addEventListener("click", () => renderView("projects"));
    document.getElementById("cmp-recheck").addEventListener("click", () => showRecheckModal(pid));
    if (canComplete) {
        document.getElementById("cmp-finalize").addEventListener("click", () => enterFinale(pid));
    }
    await loadCompletionStatus(pid);
}

async function loadCompletionStatus(pid) {
    const el = document.getElementById("cmp-status");
    if (!el) return;
    try {
        const res = await fetch(`/api/projects/${pid}/completion-status`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "加载失败");
        const conds = data.conditions || [];
        const achievedCount = data.achieved_count !== undefined ? data.achieved_count : conds.filter(c => c.achieved).length;
        const total = data.total !== undefined ? data.total : conds.length;
        let summary;
        if (data.can_complete) {
            summary = `<div class="msg-notice msg-notice-end">可完本！全部 ${total} 个条件已达成（达成章节：${data.can_complete_chapter || "?"}）</div>`;
        } else if (total === 0) {
            summary = `<div class="msg-notice">暂无完本条件（在新建项目向导中可设置）</div>`;
        } else {
            summary = `<div class="msg-notice">已达成 ${achievedCount} / ${total} 个完本条件</div>`;
        }
        const table = conds.length === 0
            ? `<div class="empty">暂无完本条件</div>`
            : `<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%;background:#fff">
                <thead><tr><th align="left">ID</th><th align="left">标题</th><th>达成</th><th>达成章节</th></tr></thead>
                <tbody>${conds.map(c => `
                    <tr>
                        <td>${escapeHtml(c.id || "")}</td>
                        <td>${escapeHtml(c.title || "")}</td>
                        <td align="center">${c.achieved ? "是" : "否"}</td>
                        <td align="center">${c.achieved_chapter || ""}</td>
                    </tr>`).join("")}
                </tbody></table>`;
        el.innerHTML = summary + table;
    } catch (e) {
        el.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
    }
}

function showRecheckModal(pid) {
    const summary = prompt("请输入章节摘要（用于完本判定）：");
    if (summary === null) return;
    fetch(`/api/projects/${pid}/completion/check`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ chapter_summary: summary })
    }).then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "重检失败");
        let msg;
        if (data.error) msg = `检测未执行：${data.error}`;
        else if (data.all_achieved) msg = `可完本！全部 ${data.total} 个条件已达成`;
        else msg = `本次新达成 ${(data.achieved || []).length} 个条件（共 ${data.achieved_count}/${data.total}）`;
        alert(msg);
        loadCompletionStatus(pid);
    }).catch(e => alert(e.message));
}


// ---------- 阶段 4：完本与导出 ----------

async function renderFinaleView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目</div>`;
        return;
    }
    let meta = null;
    try {
        const res = await fetch(`/api/projects/${pid}`);
        if (res.ok) meta = await res.json();
    } catch (e) { /* ignore */ }
    if (!meta) {
        main.innerHTML = `<div class="empty">项目加载失败</div>`;
        return;
    }
    const isCompleted = !!meta.is_completed;
    main.innerHTML = `
        <div class="page-header">
            <h2>阶段 4 完本与导出 - ${escapeHtml(meta.title || "")}</h2>
            <div class="wb-actions">
                ${isCompleted ? `<span class="fin-badge fin-badge-fix">已完本</span>` : ""}
                <button class="btn btn-ghost" id="fin-book-back">返回</button>
            </div>
        </div>
        <div id="finale-body"><div class="empty">加载中...</div></div>
        <div class="fin-panel" style="margin-top:14px">
            <div class="msg-notice msg-notice-scene">导出</div>
            <div class="fin-actions">
                <button class="btn" id="exp-txt">导出 TXT</button>
                <button class="btn" id="exp-md">导出 Markdown</button>
                <button class="btn" id="exp-epub">导出 EPUB</button>
                <button class="btn btn-ghost" id="exp-zip">导出工程文件</button>
            </div>
        </div>
    `;
    document.getElementById("fin-book-back").addEventListener("click", () => renderView("projects"));
    document.getElementById("exp-txt").addEventListener("click", () => downloadExport(pid, "txt"));
    document.getElementById("exp-md").addEventListener("click", () => downloadExport(pid, "markdown"));
    document.getElementById("exp-epub").addEventListener("click", () => downloadExport(pid, "epub"));
    document.getElementById("exp-zip").addEventListener("click", () => showProjectZipModal(pid));

    const body = document.getElementById("finale-body");
    if (isCompleted && meta.finale_summary) {
        // 已生成完结总结：直接展示
        renderFinaleSummary(body, meta.finale_summary);
    } else {
        // 触发完本流程
        body.innerHTML = `<div class="empty">正在生成完结总结...（LLM 未配置将降级）</div>`;
        try {
            const res = await fetch(`/api/projects/${pid}/finalize-book`, { method: "POST" });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "完本失败");
            renderFinaleSummary(body, data.summary);
        } catch (e) {
            body.innerHTML = `<div class="msg-notice msg-notice-error">完本失败：${escapeHtml(e.message)}</div>`;
        }
    }
}

function renderFinaleSummary(container, summary) {
    if (!summary) {
        container.innerHTML = `<div class="empty">暂无完结总结</div>`;
        return;
    }
    const fatesHtml = (summary.character_fates || []).map(f => `
        <tr><td>${escapeHtml(f.name || "")}</td><td>${escapeHtml(f.fate || "")}</td></tr>
    `).join("");
    const recapHtml = (summary.foreshadow_recap || []).map(r => `
        <tr><td>${escapeHtml(r.foreshadow || "")}</td><td>${escapeHtml(r.status || "")}</td></tr>
    `).join("");
    const timelineHtml = (summary.timeline || []).map(t => `
        <tr><td align="center">${escapeHtml(t.chapter ?? "")}</td><td>${escapeHtml(t.event || "")}</td></tr>
    `).join("");
    container.innerHTML = `
        <div class="msg-notice msg-notice-end">全书字数：${summary.total_word_count || 0}</div>
        <div class="fin-section">
            <div class="msg-notice msg-notice-scene">角色命运</div>
            ${fatesHtml
                ? `<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%;background:#fff">
                    <thead><tr><th align="left">角色</th><th align="left">命运</th></tr></thead>
                    <tbody>${fatesHtml}</tbody></table>`
                : `<div class="empty">无角色数据</div>`}
        </div>
        <div class="fin-section">
            <div class="msg-notice msg-notice-scene">伏笔回收清单</div>
            ${recapHtml
                ? `<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%;background:#fff">
                    <thead><tr><th align="left">伏笔</th><th align="left">状态</th></tr></thead>
                    <tbody>${recapHtml}</tbody></table>`
                : `<div class="empty">无伏笔数据</div>`}
        </div>
        <div class="fin-section">
            <div class="msg-notice msg-notice-scene">时间线</div>
            ${timelineHtml
                ? `<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%;background:#fff">
                    <thead><tr><th align="center">章</th><th align="left">事件</th></tr></thead>
                    <tbody>${timelineHtml}</tbody></table>`
                : `<div class="empty">无事件数据</div>`}
        </div>
    `;
}

function downloadExport(pid, fmt) {
    // 直接通过浏览器导航触发下载（GET 路由返回 Content-Disposition: attachment）
    window.location.href = `/api/projects/${pid}/export/${fmt}`;
}

function showProjectZipModal(pid) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal" style="width:360px">
            <h3>导出工程文件</h3>
            <div class="field">
                <label><input type="checkbox" id="inc-snapshots" checked> 包含历史快照（snapshots/）</label>
            </div>
            <div class="field">
                <label><input type="checkbox" id="inc-logs" checked> 包含修改日志（logs/）</label>
            </div>
            <div class="field">
                <label><input type="checkbox" id="inc-workspace"> 包含推演中间产物（chapter_workspace.json）</label>
            </div>
            <div class="modal actions">
                <button class="btn btn-ghost" id="zip-cancel">取消</button>
                <button class="btn" id="zip-ok">导出 zip</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#zip-cancel").addEventListener("click", close);
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
    mask.querySelector("#zip-ok").addEventListener("click", () => {
        const s = mask.querySelector("#inc-snapshots").checked;
        const l = mask.querySelector("#inc-logs").checked;
        const w = mask.querySelector("#inc-workspace").checked;
        close();
        const params = new URLSearchParams({
            include_snapshots: s ? "true" : "false",
            include_logs: l ? "true" : "false",
            include_workspace: w ? "true" : "false",
        }).toString();
        window.location.href = `/api/projects/${pid}/export/project?${params}`;
    });
}


// ---------- 阶段 5：章节库 + 修改流程 ----------

let editState = null;  // 当前修改流程状态对象

async function renderChaptersView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目（点击项目卡片进入工作台后再切到章节库）</div>`;
        return;
    }
    let meta = null, chapters = [];
    try {
        const [metaRes, chRes] = await Promise.all([
            fetch(`/api/projects/${pid}`),
            fetch(`/api/projects/${pid}/chapters`),
        ]);
        meta = await metaRes.json();
        if (chRes.ok) chapters = await chRes.json();
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    const listHtml = chapters.length === 0
        ? `<div class="empty">暂无章节</div>`
        : `<table border="1" cellpadding="6" style="border-collapse:collapse;width:100%;background:#fff">
            <thead><tr><th>章号</th><th align="left">标题</th><th>字数</th><th>日期</th><th>定稿</th><th>操作</th></tr></thead>
            <tbody>${chapters.map(c => `
                <tr>
                    <td align="center">${c.chapter}</td>
                    <td>${escapeHtml(c.title || "")}</td>
                    <td align="center">${c.word_count || 0}</td>
                    <td>${(c.created_at || "").replace("T", " ").slice(0, 19)}</td>
                    <td align="center">${c.finalized !== false ? "是" : ""}</td>
                    <td>
                        <button class="btn btn-sm ch-edit" data-ch="${c.chapter}">修改</button>
                        <button class="btn btn-sm btn-ghost ch-del" data-ch="${c.chapter}" data-title="${escapeAttr(c.title || "")}" style="color:#b91c1c">删除</button>
                    </td>
                </tr>`).join("")}
            </tbody></table>`;
    main.innerHTML = `
        <div class="page-header">
            <h2>章节库 - ${escapeHtml(meta.title || "")}</h2>
            <div class="wb-actions">
                <button class="btn btn-ghost" id="ch-back">返回项目列表</button>
            </div>
        </div>
        <div class="ch-search">
            <input type="text" id="ch-keyword" placeholder="关键词全库搜索（如：张三 / 酒馆）">
            <button class="btn" id="ch-search-btn">搜索</button>
            <button class="btn btn-ghost" id="ch-list-btn">章节列表</button>
        </div>
        <div id="ch-list">${listHtml}</div>
        <div id="wc-section" style="margin-top:16px">
            <div class="msg-notice msg-notice-scene">字数统计</div>
            ${chapters.length === 0
                ? `<div class="empty">暂无章节数据</div>`
                : `<div id="wc-chart" class="viz-chart"></div>`}
        </div>
    `;
    document.getElementById("ch-back").addEventListener("click", () => renderView("projects"));
    document.getElementById("ch-search-btn").addEventListener("click", () => searchChaptersInProject(pid));
    document.getElementById("ch-list-btn").addEventListener("click", () => renderChaptersView(main, pid));
    document.querySelectorAll(".ch-edit").forEach(btn => {
        btn.addEventListener("click", () => renderEditFlow(pid, parseInt(btn.dataset.ch, 10)));
    });
    // Task 13.3：章节删除（破坏性操作确认）
    document.querySelectorAll(".ch-del").forEach(btn => {
        btn.addEventListener("click", async () => {
            const num = parseInt(btn.dataset.ch, 10);
            const title = btn.dataset.title || "";
            if (!confirmDestructive(`删除第 ${num} 章「${title}」将不可恢复，是否继续？`)) return;
            try {
                const res = await fetch(`/api/projects/${pid}/chapters/${num}`, { method: "DELETE" });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "删除失败");
                await renderChaptersView(document.getElementById("main-content"), pid);
            } catch (e) { alert(e.message); }
        });
    });
    if (chapters.length > 0) renderWordCountChart(chapters);
}

function renderWordCountChart(chapters) {
    const el = document.getElementById("wc-chart");
    if (!el || typeof echarts === "undefined") return;
    const sorted = [...chapters].sort((a, b) => (a.chapter || 0) - (b.chapter || 0));
    const xData = sorted.map(c => `第${c.chapter || "?"}章`);
    const counts = sorted.map(c => c.word_count || 0);
    let cum = 0;
    const cumulative = counts.map(c => (cum += c));
    const chart = initVizChart(el);
    chart.setOption({
        tooltip: {
            trigger: "axis",
            formatter: (params) => {
                const idx = params[0].dataIndex;
                const c = sorted[idx];
                return `第${c.chapter || "?"}章 ${escapeHtml(c.title || "")}<br/>本章：${counts[idx]} 字<br/>累计：${cumulative[idx]} 字`;
            }
        },
        legend: { data: ["本章字数", "累计字数"], top: 6 },
        grid: { left: 60, right: 60, top: 50, bottom: 40 },
        xAxis: { type: "category", data: xData },
        yAxis: [
            { type: "value", name: "本章字数" },
            { type: "value", name: "累计字数" },
        ],
        series: [
            { name: "本章字数", type: "bar", data: counts, itemStyle: { color: "#5470c6" } },
            { name: "累计字数", type: "line", yAxisIndex: 1, data: cumulative, smooth: true, itemStyle: { color: "#ee6666" } },
        ],
    });
}

async function searchChaptersInProject(pid) {
    const kw = (document.getElementById("ch-keyword").value || "").trim();
    if (!kw) { alert("请输入关键词"); return; }
    try {
        const res = await fetch(`/api/projects/${pid}/edit/locate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ keyword: kw })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "搜索失败");
        const listEl = document.getElementById("ch-list");
        const cands = data.candidates || [];
        if (cands.length === 0) {
            listEl.innerHTML = `<div class="empty">无匹配结果</div>`;
            return;
        }
        listEl.innerHTML = `<div class="msg-notice msg-notice-scene">搜索"${escapeHtml(kw)}" - ${cands.length} 条匹配</div>` +
            cands.map(c => `
                <div class="contradiction">
                    <div><span class="tag-red">${c.chapter}</span> ${escapeHtml(c.title || "")} - ${escapeHtml(c.position || "")}</div>
                    <div class="fin-original">${escapeHtml(c.snippet || "")}</div>
                    <div class="fin-conflict-actions">
                        <button class="btn btn-sm cand-edit" data-ch="${c.chapter}">修改此章</button>
                    </div>
                </div>`).join("");
        document.querySelectorAll(".cand-edit").forEach(btn => {
            btn.addEventListener("click", () => renderEditFlow(pid, parseInt(btn.dataset.ch, 10)));
        });
    } catch (e) { alert(e.message); }
}

async function renderEditFlow(pid, chapterNum) {
    let chapter = null;
    try {
        const res = await fetch(`/api/projects/${pid}/edit/locate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chapter_num: chapterNum })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "加载章节失败");
        chapter = data.chapter;
        if (!chapter) throw new Error("章节不存在");
    } catch (e) {
        alert(e.message);
        return;
    }
    editState = {
        pid, chapterNum, chapter,
        step: "intent",
        instruction: null,
        execResult: null,
        revalidateResult: null,
        commitResult: null,
        error: "",
    };
    renderEditPanel();
}

function renderEditPanel() {
    if (!editState) return;
    const s = editState;
    const main = document.getElementById("main-content");
    let body = "";
    if (s.step === "intent") {
        body = renderEditIntentStep(s);
    } else if (s.step === "instruction") {
        body = renderEditInstructionStep(s);
    } else if (s.step === "executing") {
        body = `<div class="empty">正在执行修改...</div>`;
    } else if (s.step === "preview") {
        body = renderEditPreviewStep(s);
    } else if (s.step === "revalidating") {
        body = `<div class="empty">二次校验中...</div>`;
    } else if (s.step === "committing") {
        body = `<div class="empty">提交中...</div>`;
    } else if (s.step === "final") {
        body = renderEditFinalStep(s);
    } else if (s.step === "done") {
        body = renderEditDoneStep(s);
    }
    main.innerHTML = `
        <div class="page-header">
            <h2>修改流程 - 第 ${s.chapterNum} 章 ${escapeHtml(s.chapter.title || "")}</h2>
            <button class="btn btn-ghost" id="edit-back">返回章节库</button>
        </div>
        ${s.error ? `<div class="msg-notice msg-notice-error">${escapeHtml(s.error)}</div>` : ""}
        ${body}
    `;
    bindEditEvents();
}

function renderEditIntentStep(s) {
    return `
        <div class="fin-panel">
            <div class="msg-notice msg-notice-scene">步骤 1：描述修改意图</div>
            <div class="fin-row"><label>方向</label><input type="text" class="ed-direction" placeholder="如：把张三的台词改得更冷峻"></div>
            <div class="fin-row"><label>范围</label><input type="text" class="ed-scope" placeholder="如：仅本段 / 本章后续 / 跨章"></div>
            <div class="fin-row"><label>示例</label><textarea class="ed-example" rows="3" placeholder="期望的新文本示例（可选）"></textarea></div>
            <div class="fin-actions">
                <button class="btn" id="ed-gen-instruction">生成修改指令</button>
            </div>
        </div>`;
}

function renderEditInstructionStep(s) {
    const ins = s.instruction || {};
    return `
        <div class="fin-panel">
            <div class="msg-notice msg-notice-scene">步骤 2：检查/调整指令包</div>
            <div class="fin-row"><label>Target</label><input type="text" class="ed-target" value="${escapeAttr(ins.target || "")}"></div>
            <div class="fin-row"><label>Position</label><input type="text" class="ed-position" value="${escapeAttr(ins.position || "")}"></div>
            <div class="fin-row"><label>Operation</label>
                <select class="ed-operation">
                    <option value="replace" ${ins.operation === "replace" ? "selected" : ""}>replace</option>
                    <option value="insert" ${ins.operation === "insert" ? "selected" : ""}>insert</option>
                    <option value="delete" ${ins.operation === "delete" ? "selected" : ""}>delete</option>
                    <option value="reorder" ${ins.operation === "reorder" ? "selected" : ""}>reorder</option>
                </select>
            </div>
            <div class="fin-row"><label>Content</label><textarea class="ed-content" rows="3">${escapeHtml(ins.content || "")}</textarea></div>
            <div class="fin-row"><label>Scope</label><input type="text" class="ed-scope-edit" value="${escapeAttr(ins.scope || "")}"></div>
            <div class="fin-actions">
                <button class="btn btn-ghost" id="ed-back-intent">再改意图</button>
                <button class="btn" id="ed-execute">执行修改</button>
            </div>
        </div>`;
}

function renderEditPreviewStep(s) {
    const r = s.execResult || {};
    const warnHtml = r.warning ? `<div class="msg-notice msg-notice-warning">${escapeHtml(r.warning)}</div>` : "";
    const opsHtml = (r.operations || []).map(op => `
        <div class="contradiction">
            <div><span class="tag-red">${escapeHtml(op.type || "")}</span> ${escapeHtml(op.position || "")}</div>
            ${op.before ? `<div class="fin-original">前：${escapeHtml(op.before)}</div>` : ""}
            ${op.after ? `<div class="suggest">后：${escapeHtml(op.after)}</div>` : ""}
        </div>`).join("");
    return `
        <div class="fin-panel">
            <div class="msg-notice msg-notice-scene">步骤 3：修改前后对比</div>
            ${warnHtml}
            <div class="edit-diff-container">
                <div class="edit-diff-pane">
                    <div class="msg-notice msg-notice-silent">修改前</div>
                    <div class="msg-bubble"><div class="msg-body">${escapeHtml(r.old_text || "")}</div></div>
                </div>
                <div class="edit-diff-pane">
                    <div class="msg-notice msg-notice-end">修改后</div>
                    <div class="msg-bubble"><div class="msg-body">${escapeHtml(r.new_text || "")}</div></div>
                </div>
            </div>
            <div class="fin-section">
                <div class="msg-notice msg-notice-scene">操作日志</div>
                ${opsHtml || `<div class="empty">无操作</div>`}
            </div>
            <div class="fin-actions">
                <button class="btn" id="ed-confirm">确认（二次校验）</button>
                <button class="btn btn-ghost" id="ed-redo">再改</button>
                <button class="btn btn-ghost" id="ed-discard">放弃</button>
            </div>
        </div>`;
}

function renderEditFinalStep(s) {
    const r = s.revalidateResult || {};
    const passedHtml = r.passed
        ? `<div class="msg-notice msg-notice-end">二次校验通过</div>`
        : `<div class="msg-notice msg-notice-error">二次校验发现新冲突</div>`;
    const conflictsHtml = (r.new_conflicts || []).map(c => `
        <div class="contradiction">
            <div><span class="tag-red">${escapeHtml(c.type || "")}</span> ${escapeHtml(c.location || "")}</div>
            <div class="fin-original">${escapeHtml(c.original || "")}</div>
            <div class="suggest">建议：${escapeHtml(c.suggestion || "")}</div>
        </div>`).join("");
    return `
        <div class="fin-panel">
            ${passedHtml}
            ${conflictsHtml}
            <div class="fin-actions">
                ${r.passed
                    ? `<button class="btn" id="ed-commit">提交修改</button>`
                    : `<button class="btn" id="ed-force-commit">强制定稿（忽略冲突）</button>
                       <button class="btn btn-ghost" id="ed-redo">修改</button>`}
                <button class="btn btn-ghost" id="ed-discard">放弃</button>
            </div>
        </div>`;
}

function renderEditDoneStep(s) {
    const r = s.commitResult || {};
    return `
        <div class="fin-panel">
            <div class="wiz-success">✓ 第 ${s.chapterNum} 章修改已提交</div>
            ${r.word_count ? `<div class="msg-bubble"><div class="msg-body">新字数：${r.word_count}</div></div>` : ""}
            <div class="fin-actions">
                <button class="btn" id="ed-back-list">返回章节库</button>
            </div>
        </div>`;
}

function bindEditEvents() {
    const s = editState;
    if (!s) return;
    const main = document.getElementById("main-content");
    const bind = (id, h) => { const el = document.getElementById(id); if (el) el.addEventListener("click", h); };
    bind("edit-back", () => renderChaptersView(main, s.pid));
    if (s.step === "intent") {
        bind("ed-gen-instruction", () => generateInstruction(s));
    } else if (s.step === "instruction") {
        bind("ed-back-intent", () => { s.step = "intent"; renderEditPanel(); });
        bind("ed-execute", () => executeEditInstruction(s));
    } else if (s.step === "preview") {
        bind("ed-confirm", () => revalidateEdit(s));
        bind("ed-redo", () => { s.step = "intent"; renderEditPanel(); });
        bind("ed-discard", () => discardEdit(s));
    } else if (s.step === "final") {
        const r = s.revalidateResult || {};
        if (r.passed) {
            bind("ed-commit", () => commitEdit(s));
        } else {
            bind("ed-force-commit", () => commitEdit(s));
            bind("ed-redo", () => { s.step = "intent"; renderEditPanel(); });
        }
        bind("ed-discard", () => discardEdit(s));
    } else if (s.step === "done") {
        bind("ed-back-list", () => renderChaptersView(main, s.pid));
    }
}

async function generateInstruction(s) {
    const main = document.getElementById("main-content");
    const direction = main.querySelector(".ed-direction").value.trim();
    const scope = main.querySelector(".ed-scope").value.trim();
    const example = main.querySelector(".ed-example").value.trim();
    if (!direction) { alert("请填写方向"); return; }
    s.step = "loading";
    main.innerHTML = `<div class="empty">生成指令中...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/edit/instruction`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chapter_num: s.chapterNum, direction, scope, example })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "生成失败");
        s.instruction = data;
        s.step = "instruction";
        renderEditPanel();
    } catch (e) {
        s.step = "intent";
        s.error = e.message;
        renderEditPanel();
        s.error = "";
    }
}

async function executeEditInstruction(s) {
    const main = document.getElementById("main-content");
    const instruction = {
        target: main.querySelector(".ed-target").value.trim(),
        position: main.querySelector(".ed-position").value.trim(),
        operation: main.querySelector(".ed-operation").value,
        content: main.querySelector(".ed-content").value.trim(),
        scope: main.querySelector(".ed-scope-edit").value.trim(),
    };
    s.step = "executing";
    main.innerHTML = `<div class="empty">正在执行修改...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/edit/execute`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chapter_num: s.chapterNum, instruction })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "执行失败");
        s.execResult = data;
        s.step = "preview";
        renderEditPanel();
    } catch (e) {
        s.step = "instruction";
        s.error = e.message;
        renderEditPanel();
        s.error = "";
    }
}

async function revalidateEdit(s) {
    const main = document.getElementById("main-content");
    s.step = "revalidating";
    main.innerHTML = `<div class="empty">二次校验中...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/edit/revalidate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                chapter_num: s.chapterNum,
                new_text: s.execResult.new_text,
                affected_ranges: s.execResult.affected_ranges
            })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "校验失败");
        s.revalidateResult = data;
        s.step = "final";
        renderEditPanel();
    } catch (e) {
        s.step = "preview";
        s.error = e.message;
        renderEditPanel();
        s.error = "";
    }
}

async function commitEdit(s) {
    const main = document.getElementById("main-content");
    s.step = "committing";
    main.innerHTML = `<div class="empty">提交中...</div>`;
    try {
        const res = await fetch(`/api/projects/${s.pid}/edit/commit`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chapter_num: s.chapterNum, new_text: s.execResult.new_text })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "提交失败");
        s.commitResult = data;
        s.step = "done";
        renderEditPanel();
    } catch (e) {
        s.step = "final";
        s.error = e.message;
        renderEditPanel();
        s.error = "";
    }
}

async function discardEdit(s) {
    if (!confirm("确认放弃本次修改？")) return;
    const main = document.getElementById("main-content");
    try {
        // F2 修复：检查 res.ok 后再清空本地状态
        await apiFetch(`/api/projects/${s.pid}/edit/discard`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chapter_num: s.chapterNum })
        });
        editState = null;
        await renderChaptersView(main, s.pid);
    } catch (e) { alert(e.message); }
}


// ---------- 阶段 6：系统辅助 ----------

let crashRecoveryCheckedPids = new Set();  // 会话内已检测过的项目（避免重复弹窗）

async function checkCrashRecovery(pid) {
    if (crashRecoveryCheckedPids.has(pid)) return;
    crashRecoveryCheckedPids.add(pid);
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/crash-recovery`);
        const data = await res.json();
        if (data.recoverable) showCrashRecoveryModal(pid, data);
    } catch (e) { /* ignore */ }
}

function showCrashRecoveryModal(pid, data) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal" style="width:420px">
            <h3>崩溃恢复</h3>
            <p style="font-size:13px;color:#374151;margin-bottom:8px">${escapeHtml(data.message || "检测到未完成操作")}</p>
            <p style="font-size:12px;color:#6b7280;margin-bottom:14px">已恢复到最近的保存点。是否继续？</p>
            <div class="modal actions">
                <button class="btn btn-ghost" id="cr-abandon">放弃</button>
                <button class="btn" id="cr-resume">恢复</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#cr-resume").addEventListener("click", () => {
        close();
        loadFinalization(pid);
    });
    mask.querySelector("#cr-abandon").addEventListener("click", async () => {
        try {
            await fetch(`/api/projects/${pid}/assistant/crash-recovery/clear`, { method: "POST" });
        } catch (e) { /* ignore */ }
        close();
    });
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
}

function showClaimResolveModal(pid, idx, conflict) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal" style="width:480px">
            <h3>宣称冲突 · 选择真相</h3>
            <div class="contradiction">
                <div><span class="tag-red">${escapeHtml(conflict.type || "")}</span> ${escapeHtml(conflict.location || "")}</div>
                <div class="fin-original">原文：${escapeHtml(conflict.original || "")}</div>
                <div class="suggest">建议：${escapeHtml(conflict.suggestion || "")}</div>
            </div>
            <div class="field">
                <label><input type="radio" name="claim-res" value="a_lie"> A 说谎</label>
                <label><input type="radio" name="claim-res" value="b_lie"> B 说谎</label>
                <label><input type="radio" name="claim-res" value="both_wrong"> 双方都记错了</label>
                <label><input type="radio" name="claim-res" value="other"> 另有隐情</label>
            </div>
            <div class="field" id="claim-details-field" style="display:none">
                <label>补充说明</label>
                <textarea id="claim-details" rows="2" placeholder="请描述隐情" style="width:100%;padding:6px;border:1px solid #d1d5db;border-radius:4px;font-size:13px"></textarea>
            </div>
            <div class="modal actions">
                <button class="btn btn-ghost" id="claim-cancel">取消</button>
                <button class="btn" id="claim-confirm">确认</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#claim-cancel").addEventListener("click", close);
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
    mask.querySelectorAll('input[name="claim-res"]').forEach(radio => {
        radio.addEventListener("change", () => {
            mask.querySelector("#claim-details-field").style.display = radio.checked && radio.value === "other" ? "block" : "none";
        });
    });
    mask.querySelector("#claim-confirm").addEventListener("click", async () => {
        const checked = mask.querySelector('input[name="claim-res"]:checked');
        if (!checked) { alert("请选择一个真相选项"); return; }
        const resolution = checked.value;
        const details = mask.querySelector("#claim-details").value.trim();
        try {
            const res = await fetch(`/api/projects/${pid}/assistant/claim-resolve`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ conflict_index: idx, resolution, details: details || undefined }),
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || "解决失败");
            const affected = (data.affected_characters || []).join("、") || "无";
            alert(`已记录真相选择。受影响角色：${affected}`);
            close();
            // 刷新定稿面板（claim 已处理）
            if (finState && finState.pid === pid) {
                finState.feedback[idx] = finState.feedback[idx] || {};
                finState.feedback[idx].claim_resolved = true;
                renderFinalizationPanel();
            }
        } catch (e) { alert(e.message); }
    });
}

async function renderAssistantView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目（点击项目卡片进入工作台后再切到系统辅助）</div>`;
        return;
    }
    let meta = null;
    try {
        const res = await fetch(`/api/projects/${pid}`);
        if (res.ok) meta = await res.json();
    } catch (e) { /* ignore */ }
    if (!meta) {
        main.innerHTML = `<div class="empty">项目加载失败</div>`;
        return;
    }
    main.innerHTML = `
        <div class="page-header">
            <h2>系统辅助 - ${escapeHtml(meta.title || "")}</h2>
            <button class="btn btn-ghost" id="asst-back">返回项目列表</button>
        </div>
        <div id="asst-crash" class="fin-panel" style="margin-bottom:14px"></div>
        <div id="asst-dormant" class="fin-panel" style="margin-bottom:14px"><div class="empty">加载中...</div></div>
        <div id="asst-ignored" class="fin-panel"><div class="empty">加载中...</div></div>
    `;
    document.getElementById("asst-back").addEventListener("click", () => renderView("projects"));
    await loadCrashRecoveryPanel(pid);
    await loadDormantPanel(pid);
    await loadIgnoredPanel(pid);
}

async function loadCrashRecoveryPanel(pid) {
    const el = document.getElementById("asst-crash");
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/crash-recovery`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "加载失败");
        if (data.recoverable) {
            el.innerHTML = `
                <div class="msg-notice msg-notice-warning">${escapeHtml(data.message)}</div>
                <div class="fin-actions">
                    <button class="btn btn-ghost btn-sm" id="asst-cr-abandon">放弃未完成操作</button>
                    <button class="btn btn-sm" id="asst-cr-resume">恢复定稿</button>
                </div>
            `;
            document.getElementById("asst-cr-resume").addEventListener("click", () => {
                crashRecoveryCheckedPids.add(pid);
                enterWorkbench(pid);
                setTimeout(() => loadFinalization(pid), 100);
            });
            document.getElementById("asst-cr-abandon").addEventListener("click", async () => {
                try {
                    await fetch(`/api/projects/${pid}/assistant/crash-recovery/clear`, { method: "POST" });
                    await loadCrashRecoveryPanel(pid);
                } catch (e) { alert(e.message); }
            });
        } else {
            el.innerHTML = `<div class="msg-notice">无未完成操作</div>`;
        }
    } catch (e) {
        el.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
    }
}

async function loadDormantPanel(pid) {
    const el = document.getElementById("asst-dormant");
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/dormant`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "加载失败");
        const dormant = data.dormant || [];
        const awakened = data.awakened || [];
        const dormantHtml = dormant.length === 0
            ? `<div class="empty" style="padding:14px 0">无可休眠角色</div>`
            : dormant.map(d => `
                <label class="asst-check"><input type="checkbox" class="asst-dormant-cb" value="${escapeAttr(d.name)}"> ${escapeHtml(d.name)}（最后出场：第 ${d.last_chapter || 0} 章）</label>`).join("");
        const awakenedHtml = awakened.length === 0
            ? `<div class="empty" style="padding:14px 0">无已休眠角色</div>`
            : awakened.map(a => `
                <label class="asst-check"><input type="checkbox" class="asst-awaken-cb" value="${escapeAttr(a.name)}"> ${escapeHtml(a.name)}</label>`).join("");
        el.innerHTML = `
            <div class="msg-notice msg-notice-scene">角色休眠面板（连续 20 章未出场且未 @ → 建议休眠）</div>
            <div class="asst-section">
                <div class="msg-notice">可休眠角色</div>
                <div class="asst-check-list">${dormantHtml}</div>
                <button class="btn btn-sm" id="asst-mark-dormant" ${dormant.length === 0 ? "disabled" : ""}>标记休眠</button>
            </div>
            <div class="asst-section">
                <div class="msg-notice">已休眠角色</div>
                <div class="asst-check-list">${awakenedHtml}</div>
                <button class="btn btn-sm" id="asst-awaken" ${awakened.length === 0 ? "disabled" : ""}>唤醒</button>
            </div>
        `;
        document.getElementById("asst-mark-dormant").addEventListener("click", () => markDormant(pid));
        document.getElementById("asst-awaken").addEventListener("click", () => awakenCharacters(pid));
    } catch (e) {
        el.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
    }
}

async function markDormant(pid) {
    const names = Array.from(document.querySelectorAll(".asst-dormant-cb:checked")).map(cb => cb.value);
    if (names.length === 0) { alert("请选择至少一个角色"); return; }
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/dormant`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ names }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "操作失败");
        alert(`已标记 ${data.marked_dormant.length} 个角色为休眠`);
        await loadDormantPanel(pid);
    } catch (e) { alert(e.message); }
}

async function awakenCharacters(pid) {
    const names = Array.from(document.querySelectorAll(".asst-awaken-cb:checked")).map(cb => cb.value);
    if (names.length === 0) { alert("请选择至少一个角色"); return; }
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/awaken`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ names }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "操作失败");
        alert(`已唤醒 ${data.marked_active.length} 个角色`);
        await loadDormantPanel(pid);
    } catch (e) { alert(e.message); }
}

async function loadIgnoredPanel(pid) {
    const el = document.getElementById("asst-ignored");
    try {
        const res = await fetch(`/api/projects/${pid}/assistant/ignored-conflicts`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "加载失败");
        const list = data.ignored || [];
        const html = list.length === 0
            ? `<div class="empty" style="padding:14px 0">暂无历史忽略记录</div>`
            : list.map(c => `
                <div class="contradiction">
                    <div><span class="tag-red">${escapeHtml(c.type || "")}</span> ${escapeHtml(c.location || "")}</div>
                    <div class="fin-original">原文：${escapeHtml(c.original || "")}</div>
                    <div class="suggest">建议：${escapeHtml(c.suggestion || "")}</div>
                    <div class="meta" style="font-size:11px;color:#9ca3af;margin-top:4px">忽略时间：${(c.ignored_at || "").replace("T", " ").slice(0, 19)}</div>
                </div>`).join("");
        el.innerHTML = `
            <div class="msg-notice msg-notice-scene">历史忽略记录（检察员下次校验时标注同类提醒）</div>
            ${html}
        `;
    } catch (e) {
        el.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
    }
}


// ---------- Task 12：可视化视图（角色关系图 / 事件时间轴 / 伏笔关系图 / 世界规则） ----------

// ECharts 实例复用安全：先销毁同 DOM 上的旧实例，避免视图切换后内存泄漏
function initVizChart(el) {
    if (typeof echarts === "undefined") return null;
    const existing = echarts.getInstanceByDom(el);
    if (existing) existing.dispose();
    return echarts.init(el);
}

// 通用详情弹窗（复用 .modal-mask / .modal 样式）
function showVizModal(title, bodyHtml) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal viz-modal">
            <h3>${escapeHtml(title)}</h3>
            <div class="viz-modal-body">${bodyHtml}</div>
            <div class="modal actions">
                <button class="btn" id="viz-modal-close">关闭</button>
            </div>
        </div>
    `;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector("#viz-modal-close").addEventListener("click", close);
    mask.addEventListener("click", (e) => { if (e.target === mask) close(); });
}

// 通用：拉取 viz/data + meta，返回 {meta, data} 或抛错
async function loadVizData(pid) {
    const [mRes, dRes] = await Promise.all([
        fetch(`/api/projects/${pid}`),
        fetch(`/api/projects/${pid}/viz/data`),
    ]);
    if (!mRes.ok || !dRes.ok) throw new Error("加载失败");
    return { meta: await mRes.json(), data: await dRes.json() };
}

function renderVizHeader(main, title, pid) {
    main.innerHTML = `
        <div class="page-header">
            <h2>${escapeHtml(title)}</h2>
        </div>
        <div id="viz-body"><div class="empty">加载中...</div></div>
    `;
    return document.getElementById("viz-body");
}

// 12.1 角色关系图：ECharts graph，节点=角色（大小反映出场次数），连线=关系
async function renderRelationsGraph(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目</div>`;
        return;
    }
    let meta, data;
    try {
        ({ meta, data } = await loadVizData(pid));
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    const body = renderVizHeader(main, `角色关系图 - ${meta.title || ""}`, pid);
    const characters = data.characters || [];
    const relationships = data.relationships || [];
    const chapters = data.chapters || [];

    if (characters.length === 0) {
        body.innerHTML = `<div class="empty">暂无角色数据</div>`;
        return;
    }

    // 统计每个角色在章节正文中的出场次数（正则全局匹配）
    // ponytail: O(角色数 × 章节数 × 文本长度) 的朴素扫描；章节/角色规模大时可改用一次性 token 扫描
    const counts = {};
    characters.forEach(c => { counts[c.name || ""] = 0; });
    chapters.forEach(ch => {
        const text = ch.text || "";
        if (!text) return;
        characters.forEach(c => {
            const n = c.name;
            if (!n) return;
            try {
                const m = text.match(new RegExp(n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "g"));
                if (m) counts[n] += m.length;
            } catch (e) { /* 正则异常忽略 */ }
        });
    });

    body.innerHTML = `<div id="rel-chart" class="viz-chart"></div>
        <div class="msg-notice" style="margin-top:8px">提示：节点大小反映出场次数，红色=焦点角色，灰色=休眠角色；点击节点查看角色卡</div>`;
    const chart = initVizChart(document.getElementById("rel-chart"));
    if (!chart) return;

    const nodes = characters.map(c => {
        const cnt = counts[c.name] || 0;
        const size = Math.min(80, 30 + cnt * 5);
        let color = "#5470c6";  // 默认蓝
        if (c.activation_state === "dormant") color = "#9ca3af";  // 休眠灰
        if (c.is_focus) color = "#ee6666";  // 焦点红
        return {
            name: c.name || "(无名)",
            symbolSize: size,
            itemStyle: { color },
            value: { count: cnt, character: c },
        };
    });
    const nameSet = new Set(characters.map(c => c.name));
    const links = relationships
        .map(r => ({ source: r.char_a, target: r.char_b, value: r.type || r.description || "" }))
        .filter(l => nameSet.has(l.source) && nameSet.has(l.target));

    chart.setOption({
        tooltip: {
            formatter: (p) => {
                if (p.dataType === "node") {
                    const c = p.data.value.character;
                    return `<b>${escapeHtml(c.name || "")}</b><br/>出场次数：${p.data.value.count}`;
                } else if (p.dataType === "edge") {
                    return escapeHtml(p.data.value || "（关系）");
                }
                return "";
            }
        },
        series: [{
            type: "graph",
            layout: "force",
            roam: true,
            label: { show: true, position: "right", fontSize: 12 },
            force: { repulsion: 220, edgeLength: 120, gravity: 0.1 },
            data: nodes,
            links: links,
            lineStyle: { color: "#9ca3af", width: 1.5, curveness: 0.1 },
            emphasis: { focus: "adjacency", lineStyle: { width: 3 } },
        }],
    });

    chart.on("click", (params) => {
        if (params.dataType === "node") showCharDetailModal(params.data.value.character);
    });
}

function showCharDetailModal(c) {
    const rows = [
        ["姓名", c.name],
        ["性格", c.personality],
        ["当前目标", c.current_goal],
        ["当前情绪", c.current_emotion],
        ["当前位置", c.current_location],
        ["曝光状态", c.exposure_status],
        ["激活状态", c.activation_state],
        ["焦点角色", c.is_focus ? "是" : "否"],
    ];
    if (c.inventory) rows.push(["随身物品", JSON.stringify(c.inventory)]);
    if (c.memory_anchors && c.memory_anchors.length) rows.push(["记忆锚点", c.memory_anchors.join("；")]);
    if (c.hidden_fields && Object.keys(c.hidden_fields).length) rows.push(["隐藏字段", JSON.stringify(c.hidden_fields)]);
    let body = rows.map(([k, v]) =>
        `<div class="viz-modal-row"><span class="viz-k">${escapeHtml(k)}</span><span class="viz-v">${escapeHtml(v || "—")}</span></div>`
    ).join("");
    // R22: 焦点角色不可删除；非焦点角色显示删除按钮
    if (c.is_focus) {
        body += `<div class="viz-modal-row" style="margin-top:12px;color:#6b7280">焦点角色不可删除</div>`;
    } else {
        body += `<div class="viz-modal-row" style="margin-top:12px"><button class="btn btn-ghost" id="viz-modal-del-char">删除角色</button></div>`;
    }
    showVizModal("角色卡", body);
    // 删除按钮事件
    const delBtn = document.getElementById("viz-modal-del-char");
    if (delBtn) {
        delBtn.addEventListener("click", async () => {
            if (!confirmDestructive(`删除角色「${c.name}」将同时清理其所有关系，不可恢复，是否继续？`)) return;
            const pid = currentWorkbenchPid;
            if (!pid) return;
            try {
                const res = await fetch(`/api/projects/${pid}/characters/delete`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ names: [c.name] }),
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || "删除失败");
                const del = (data.deleted || []).join(", ");
                const rej = (data.rejected_focus || []).join(", ");
                alert(del ? `已删除: ${del}` + (rej ? `\n焦点角色拒绝: ${rej}` : "") : "未删除（焦点角色或未找到）");
                // 关闭模态 + 刷新关系图
                const mask = document.querySelector(".modal-mask");
                if (mask) mask.remove();
                renderVisualizationPanel(pid);
            } catch (e) {
                alert(e.message);
            }
        });
    }
}

// 12.2 事件时间轴：ECharts scatter，x=章号，y=事件类型分类，点击查看详情
async function renderEventsView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目</div>`;
        return;
    }
    let meta, data;
    try {
        ({ meta, data } = await loadVizData(pid));
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    const body = renderVizHeader(main, `事件时间轴 - ${meta.title || ""}`, pid);
    const events = data.events || [];

    if (events.length === 0) {
        body.innerHTML = `<div class="empty">暂无事件数据</div>`;
        return;
    }

    // 收集所有事件类型（保持出现顺序）
    const types = [];
    events.forEach(e => {
        const t = e.type || "其他";
        if (!types.includes(t)) types.push(t);
    });

    body.innerHTML = `<div id="evt-chart" class="viz-chart"></div>
        <div class="msg-notice" style="margin-top:8px">提示：x 轴为章号，y 轴为事件类型；点击点查看事件详情</div>`;
    const chart = initVizChart(document.getElementById("evt-chart"));
    if (!chart) return;

    const series = types.map((t) => ({
        name: t,
        type: "scatter",
        symbolSize: 18,
        data: events
            .filter(e => (e.type || "其他") === t)
            .map(e => [e.chapter || 0, t, e]),
    }));

    chart.setOption({
        tooltip: {
            formatter: (p) => {
                const e = p.data[2];
                return `<b>第${e.chapter || "?"}章 · ${escapeHtml(e.type || "")}</b><br/>${escapeHtml(e.description || "")}`;
            }
        },
        legend: { data: types, top: 6 },
        grid: { left: 80, right: 30, top: 50, bottom: 50 },
        xAxis: { name: "章号", type: "value", min: 0, minInterval: 1 },
        yAxis: { type: "category", data: types, inverse: true },
        series: series,
    });

    chart.on("click", (params) => {
        const e = params.data[2];
        if (!e) return;
        const rows = [
            ["类型", e.type],
            ["描述", e.description],
            ["章节", e.chapter != null ? `第${e.chapter}章` : "—"],
            ["轮次", e.round != null ? e.round : "—"],
            ["时间", e.timestamp || "—"],
        ];
        const bodyHtml = rows.map(([k, v]) =>
            `<div class="viz-modal-row"><span class="viz-k">${escapeHtml(k)}</span><span class="viz-v">${escapeHtml(v || "—")}</span></div>`
        ).join("");
        showVizModal("事件详情", bodyHtml);
    });
}

// 12.4 伏笔关系图：若数据含 planted_chapter/resolved_chapter 则绘制 graph；
// 否则降级为列表视图（按状态着色，未回收红色高亮）
async function renderForeshadowsView(main, pid) {
    if (!pid) {
        main.innerHTML = `<div class="empty">请先从项目列表选择一个项目</div>`;
        return;
    }
    let meta, data;
    try {
        ({ meta, data } = await loadVizData(pid));
    } catch (e) {
        main.innerHTML = `<div class="empty">加载失败: ${e.message}</div>`;
        return;
    }
    const body = renderVizHeader(main, `伏笔关系图 - ${meta.title || ""}`, pid);
    const foreshadows = data.foreshadows || [];

    if (foreshadows.length === 0) {
        body.innerHTML = `<div class="empty">暂无伏笔数据</div>`;
        return;
    }

    // 兼容两种 schema：{foreshadow, status}（实际）和 {id, content, planted_chapter, resolved_chapter, status}（spec 假设）
    const hasChapterInfo = foreshadows.some(f => f.planted_chapter != null || f.resolved_chapter != null);

    if (hasChapterInfo) {
        renderForeshadowsGraph(body, foreshadows);
    } else {
        renderForeshadowsList(body, foreshadows);
    }
}

function isForeshadowResolved(f) {
    const s = String(f.status || "").toLowerCase();
    return s === "已回收" || s === "resolved" || s === "回收";
}

function foreshadowText(f) {
    return f.foreshadow || f.content || f.description || "(未命名伏笔)";
}

// 列表视图：实际数据 schema 下的降级渲染
function renderForeshadowsList(body, foreshadows) {
    const resolved = foreshadows.filter(isForeshadowResolved);
    const unresolved = foreshadows.filter(f => !isForeshadowResolved(f));
    body.innerHTML = `
        <div class="msg-notice msg-notice-warning">⚠ 未回收伏笔 ${unresolved.length} 条，已回收 ${resolved.length} 条</div>
        <div class="fs-list">
            ${foreshadows.map((f, i) => {
                const ok = isForeshadowResolved(f);
                return `
                    <div class="fs-card ${ok ? "fs-resolved" : "fs-unresolved"}" data-idx="${i}">
                        <span class="fs-badge ${ok ? "fs-badge-ok" : "fs-badge-warn"}">${ok ? "已回收" : "未回收"}</span>
                        <span class="fs-text">${escapeHtml(foreshadowText(f))}</span>
                        <span class="fs-status">${escapeHtml(f.status || "")}</span>
                    </div>`;
            }).join("")}
        </div>
        <div class="msg-notice" style="margin-top:8px">提示：当前伏笔数据未含章节字段，仅显示状态列表；含 planted_chapter/resolved_chapter 时将自动切换为关系图</div>
    `;
    body.querySelectorAll(".fs-card").forEach(card => {
        card.addEventListener("click", () => {
            const idx = parseInt(card.dataset.idx, 10);
            const f = foreshadows[idx];
            const rows = [
                ["内容", foreshadowText(f)],
                ["状态", f.status || "—"],
                ["ID", f.id || "—"],
            ];
            const bodyHtml = rows.map(([k, v]) =>
                `<div class="viz-modal-row"><span class="viz-k">${escapeHtml(k)}</span><span class="viz-v">${escapeHtml(v || "—")}</span></div>`
            ).join("");
            showVizModal("伏笔详情", bodyHtml);
        });
    });
}

// 关系图视图：spec 假设 schema 下的渲染
function renderForeshadowsGraph(body, foreshadows) {
    body.innerHTML = `<div id="fs-chart" class="viz-chart"></div>
        <div class="msg-notice" style="margin-top:8px">提示：左侧为埋设章节，右侧为回收章节；红色连线=未回收伏笔高亮</div>`;
    const chart = initVizChart(document.getElementById("fs-chart"));
    if (!chart) return;

    // 章节节点去重：name 形如 "埋第3章" / "收第5章"
    const nodeMap = new Map();
    const getNode = (label, isResolved) => {
        if (!nodeMap.has(label)) {
            nodeMap.set(label, { name: label, symbolSize: 40, itemStyle: { color: isResolved ? "#059669" : "#9ca3af" } });
        }
        return nodeMap.get(label);
    };
    const links = [];
    foreshadows.forEach((f, i) => {
        const planted = f.planted_chapter != null ? `埋第${f.planted_chapter}章` : null;
        const resolved = f.resolved_chapter != null ? `收第${f.resolved_chapter}章` : null;
        const ok = isForeshadowResolved(f);
        if (planted) getNode(planted, false);
        if (resolved) getNode(resolved, true);
        if (!planted && !resolved) {
            // 无章节字段，单独占位节点
            const name = `伏笔${i + 1}`;
            nodeMap.set(name, { name, symbolSize: 30, itemStyle: { color: ok ? "#059669" : "#ee6666" } });
            return;
        }
        if (planted && resolved) {
            links.push({
                source: planted,
                target: resolved,
                value: foreshadowText(f),
                lineStyle: { color: ok ? "#059669" : "#ee6666", width: ok ? 1.5 : 3, curveness: 0.2 },
            });
        } else if (planted) {
            // 仅埋设未回收：独立红色节点
            const orphan = `${planted}·未回收`;
            nodeMap.set(orphan, { name: orphan, symbolSize: 30, itemStyle: { color: "#ee6666" } });
            links.push({ source: planted, target: orphan, value: foreshadowText(f), lineStyle: { color: "#ee6666", width: 3 } });
        }
    });

    chart.setOption({
        tooltip: {
            formatter: (p) => {
                if (p.dataType === "edge") return escapeHtml(p.data.value || "");
                return escapeHtml(p.data.name || "");
            }
        },
        series: [{
            type: "graph",
            layout: "force",
            roam: true,
            label: { show: true, fontSize: 11 },
            force: { repulsion: 200, edgeLength: 100 },
            data: Array.from(nodeMap.values()),
            links: links,
            lineStyle: { curveness: 0.2 },
            emphasis: { focus: "adjacency" },
        }],
    });

    chart.on("click", (params) => {
        if (params.dataType === "edge") {
            const f = foreshadows.find(x => foreshadowText(x) === params.data.value);
            if (!f) return;
            const rows = [
                ["内容", foreshadowText(f)],
                ["状态", f.status || "—"],
                ["埋设章节", f.planted_chapter != null ? `第${f.planted_chapter}章` : "—"],
                ["回收章节", f.resolved_chapter != null ? `第${f.resolved_chapter}章` : "—"],
            ];
            const bodyHtml = rows.map(([k, v]) =>
                `<div class="viz-modal-row"><span class="viz-k">${escapeHtml(k)}</span><span class="viz-v">${escapeHtml(v || "—")}</span></div>`
            ).join("");
            showVizModal("伏笔详情", bodyHtml);
        }
    });
}


