import { useEffect, useState } from "react";
import { api } from "../api";
import { requestDeveloperPassword } from "../elevation";
import { CollapsibleNote } from "./CollapsibleNote";
import { ConfirmDialog, Dialog } from "./Dialog";
import { PopupItem, PopupMenu, PopupSep } from "./PopupMenu";
import {
  CloseIcon,
  DeleteIcon,
  EditIcon,
  ExtensionIcon,
  FormatListBulletedIcon,
  LockIcon,
  MoreVertIcon,
  PublicIcon,
  RocketLaunchIcon,
  TerminalIcon,
} from "./icons";
import { I18N } from "../i18n";
import { getSelectedInstanceId, setSelectedInstanceId, useSelectedInstanceId } from "../selection";
import { refreshExternal, refreshZcode, toast, useAppState } from "../store";
import type { ExternalInstance, InstancePlugins, ZcodeCatalogEntry } from "../types";
import { errMsg } from "../utils";
import { TerminalWindow, type TerminalTargetInfo } from "./TerminalWindow";

const DEFAULT_BASE_URL = "https://api.openai.com/v1";

const STATE_TEXT: Record<string, string> = {
  created: "未启动",
  starting: "启动中",
  running: "运行中",
  stopped: "已停止",
  error: "出错",
};

const STATE_TAG: Record<string, string> = {
  starting: "running",
  running: "running",
  error: "failed",
};

function compareSemver(left: string, right: string): number {
  const core = (value: string) => value.replace(/^v/i, "").split("-")[0].split(".").map((part) => parseInt(part, 10) || 0);
  const a = core(left);
  const b = core(right);
  for (let i = 0; i < 3; i++) {
    if ((a[i] ?? 0) !== (b[i] ?? 0)) return (a[i] ?? 0) - (b[i] ?? 0);
  }
  return 0;
}

function catalogLabel(entry: ZcodeCatalogEntry): string {
  const channel = entry.channel === "main" ? "未发布" : entry.channel === "release" ? "正式版" : "仅 tag";
  const installed = entry.installed ? "已装" : "需构建";
  const credential = entry.credentialFormat >= 2 ? "v2 需 secret" : "v1";
  return `${entry.version} · ${channel} · ${installed} · ${credential}`;
}

function versionDisabled(entry: ZcodeCatalogEntry, instance: ExternalInstance | null): boolean {
  if (!entry.installed) return true;
  if (!instance) return false;
  if (instance.version === entry.ref) return false;
  if ((instance.credentialFormat ?? 0) > 0 && entry.credentialFormat < (instance.credentialFormat ?? 0)) {
    return true;
  }
  if (instance.minVersion && instance.minVersion !== "main" && entry.channel !== "main"
      && compareSemver(entry.version, instance.minVersion) < 0) {
    return true;
  }
  if (instance.minVersion === "main" && entry.channel !== "main"
      && ((entry.publishedAt ?? 0) <= 0 || (entry.publishedAt ?? 0) < (instance.minVersionAt ?? 0))) {
    return true;
  }
  return false;
}

/// ZCode（实验性）：与 dsh 实例完全隔离的独立品牌分区。
/// 纯实现性验证功能——不保证可用、不承诺兼容，定位与限制见 docs/zcode-experimental.md。
///
/// 品牌实例层（`BrandsView`）里它是 zcode 分支的「实例」卡：发行包检测/构建在那一层的
/// 版本卡上（内置弹窗 ZcodeBuildButton 也在那边），这里只管实例：
/// 创建（要填端点与密钥，所以用弹窗而不是内联的创建行）/ 编辑 / 日志 / 启停 / 删除。
/// 导出签名保持无参：它由品牌层整块渲染，不需要外部传参。
export function ZcodeSection() {
  const s = useAppState();
  const selectedId = useSelectedInstanceId();
  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ExternalInstance | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ExternalInstance | null>(null);
  const [logTarget, setLogTarget] = useState<ExternalInstance | null>(null);
  const [logLines, setLogLines] = useState<string[]>([]);
  const [pluginsTarget, setPluginsTarget] = useState<ExternalInstance | null>(null);
  const [menuAnchor, setMenuAnchor] = useState<{ el: HTMLElement; inst: ExternalInstance } | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    name: "", baseUrl: DEFAULT_BASE_URL, apiKey: "", workspace: "",
    version: "", useSeed: false, seed: "",
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [catalog, setCatalog] = useState<ZcodeCatalogEntry[]>([]);
  const [rotateTarget, setRotateTarget] = useState<ExternalInstance | null>(null);
  const [rotateSeed, setRotateSeed] = useState("");
  const [rotateClearKey, setRotateClearKey] = useState(true);
  const [terminalTarget, setTerminalTarget] = useState<TerminalTargetInfo | null>(null);
  /// Web Shell 总开关（开发者模式里控制）与 HTTPS 状态；任一不满足就不显示终端入口
  const [webShellEnabled, setWebShellEnabled] = useState(false);

  useEffect(() => {
    let alive = true;
    api.zcodeCatalog()
      .then((result) => {
        if (alive) setCatalog(result.entries);
      })
      .catch(() => {
        // A catalog failure must not hide the instance list; the version select stays empty.
      });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (!createOpen && !editTarget) return;
    let alive = true;
    api.zcodeCatalog()
      .then((result) => {
        if (alive) setCatalog(result.entries);
      })
      .catch(() => {
        // Keep the previous list; the select still works with it.
      });
    return () => {
      alive = false;
    };
  }, [createOpen, editTarget]);

  useEffect(() => {
    let alive = true;
    api
      .developerStatus()
      .then((status) => {
        if (alive) setWebShellEnabled(status.webShellEnabled && status.https);
      })
      .catch(() => {
        if (alive) setWebShellEnabled(false);
      });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    void refreshZcode();
  }, []);

  // 实例状态唯一来源是 store.external；这里只在挂载时刷新一次，外加
  // 启动中的实例每 1.5 秒跟一次（ZCode 冷启动要等就绪行）。
  const instances = s.external.filter((e) => e.brand === "zcode");
  useEffect(() => {
    void refreshExternal();
    void refreshZcode();
  }, []);
  useEffect(() => {
    if (!instances.some((i) => i.state === "starting")) return;
    const timer = setInterval(() => void refreshExternal(), 1500);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [s.external]);

  const running = instances.filter((i) => i.state === "running").length;

  const closeForm = () => {
    if (busy) return;
    setCreateOpen(false);
    setEditTarget(null);
  };

  const openCreate = () => {
    // 一个实例都没有时预填品牌名（与品牌实例卡的创建行同规则），点一下保存就有实例
    setForm({
      name: instances.length === 0 ? I18N["dsh.brand.zcode.short"] : "",
      baseUrl: DEFAULT_BASE_URL,
      apiKey: "",
      workspace: "",
      version: catalog.find((entry) => entry.installed && entry.channel === "release")?.ref
        ?? catalog.find((entry) => entry.installed)?.ref ?? "",
      useSeed: false,
      seed: "",
    });
    setFormError(null);
    setCreateOpen(true);
  };

  const openEdit = (inst: ExternalInstance) => {
    setForm({
      name: inst.name,
      baseUrl: inst.baseUrl ?? DEFAULT_BASE_URL,
      apiKey: "",
      workspace: inst.workspacePath ?? "",
      version: inst.version ?? "",
      useSeed: false,
      seed: "",
    });
    setFormError(null);
    setEditTarget(inst);
  };

  const validate = (): boolean => {
    if (!form.name.trim()) {
      setFormError("请填写名称");
      return false;
    }
    if (form.apiKey.trim() && !form.baseUrl.trim()) {
      setFormError("填写 API Key 时必须同时填写 API Base URL");
      return false;
    }
    if (!editTarget && form.useSeed && !form.seed.trim()) {
      setFormError("选择“使用种子”时必须填写种子");
      return false;
    }
    return true;
  };

  const submitCreate = async () => {
    if (!validate()) return;
    setBusy(true);
    try {
      const r = await api.createZcodeInstance({
        name: form.name.trim(),
        baseUrl: form.baseUrl.trim(),
        apiKey: form.apiKey.trim() || undefined,
        // 空 = 面板在 zcode/workspaces 下自动建一个工作区
        workspacePath: form.workspace.trim() || undefined,
        version: form.version || undefined,
        seed: !editTarget && form.useSeed ? form.seed.trim() : undefined,
      });
      toast("success", `已创建，工作区：${r.instance.workspacePath}`);
      setCreateOpen(false);
      await refreshExternal();
    } catch (e) {
      toast("error", `创建失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const submitEdit = async () => {
    if (!editTarget || !validate()) return;
    setBusy(true);
    try {
      await api.patchZcodeInstance(editTarget.id, {
        name: form.name.trim(),
        baseUrl: form.baseUrl.trim(),
        // 留空 = 不修改已保存的 Key（后端只在字段存在时覆盖）
        ...(form.apiKey.trim() ? { apiKey: form.apiKey.trim() } : {}),
        ...(form.version && form.version !== (editTarget.version ?? "") ? { version: form.version } : {}),
      });
      toast("success", "已保存");
      setEditTarget(null);
      await refreshExternal();
    } catch (e) {
      toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const launch = async (inst: ExternalInstance) => {
    setBusy(true);
    try {
      const r = await api.launchZcode(inst.id);
      if (r.state === "error") toast("error", `启动失败：${r.error ?? "未知原因"}`);
      else toast("success", "已启动（实验性功能，不保证可用）");
      await refreshExternal();
    } catch (e) {
      toast("error", `启动失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const stop = async (inst: ExternalInstance) => {
    try {
      await api.stopZcode(inst.id);
      toast("info", "已停止");
      await refreshExternal();
    } catch (e) {
      toast("error", `停止失败：${errMsg(e)}`);
    }
  };

  const openInBrowser = async (inst: ExternalInstance) => {
    try {
      // 守卫开着时"打开"要提权：先弹密码框，成功后重试一次。
      let r;
      try {
        r = await api.zcodeOpen(inst.id);
      } catch (e) {
        if (!(e instanceof Error) || !e.message.includes("developer_elevation_required")) throw e;
        if (!(await requestDeveloperPassword())) throw e;
        r = await api.zcodeOpen(inst.id);
      }
      window.open(r.url, "_blank", "noopener");  // r.url 已带挂载点前缀
    } catch (e) {
      toast("error", `打开失败：${errMsg(e)}`);
    }
  };
  const showLogs = async (inst: ExternalInstance) => {
    try {
      const r = await api.zcodeLogs(inst.id, 300);
      setLogLines(r.lines);
      setLogTarget(inst);
    } catch (e) {
      toast("error", `读取日志失败：${errMsg(e)}`);
    }
  };

  const remove = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await api.deleteZcodeInstance(deleteTarget.id);
      toast("success", "已删除");
      // 删掉的正好是「当前实例」时把选择清掉，免得启动面板指向一个不存在的实例
      if (getSelectedInstanceId() === deleteTarget.id) setSelectedInstanceId(null);
      setDeleteTarget(null);
      await refreshExternal();
    } catch (e) {
      toast("error", `删除失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const rotateKey = async () => {
    if (!rotateTarget) return;
    setBusy(true);
    try {
      await api.rotateZcodeKey(rotateTarget.id, {
        seed: rotateSeed.trim() || undefined,
        clearProviderKey: rotateClearKey,
      });
      toast("success", "密钥已更换；请重新添加 API Key / 登录");
      setRotateTarget(null);
      setRotateSeed("");
      await refreshExternal();
    } catch (e) {
      toast("error", `更换密钥失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="card"
      style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", padding: 0 }}
    >
      <div className="toolbar-row" style={{ padding: "6px 12px", flexShrink: 0 }}>
        <span style={{ fontSize: 13, fontWeight: 600, flex: "none" }}>
          {I18N["dsh.brand.section.instances"]}
        </span>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", whiteSpace: "nowrap" }}>
          {instances.length} 个实例{running > 0 ? ` · ${running} 个运行中` : ""}
        </span>
        <span className="spacer" />
        <button className="btn btn-outline ripple-host" onClick={openCreate}>
          添加 ZCode 实例
        </button>
      </div>

      {/* 移动端长说明收成一行 + 详情展开；桌面端原样 */}
      <div style={{ flexShrink: 0, padding: "0 12px 6px" }}>
        <CollapsibleNote
          summary={
            <b style={{ color: "var(--monet-on-surface)" }}>
              纯实现性验证功能：不保证可用，也不承诺与 dsh 同等的功能与兼容
            </b>
          }
        >
          <div style={{ fontSize: 12, lineHeight: 1.7, color: "var(--monet-on-surface-variant)" }}>
            <b style={{ color: "var(--monet-on-surface)" }}>
              纯实现性验证功能：不保证可用，也不承诺与 dsh 同等的功能与兼容
            </b>
            ——能不能跑通取决于 ZCode 上游。实例只绑回环、经面板反代到 <code>/i/&lt;id&gt;/</code>（同端口同证书、
            同样要登录），凭证以明文保存在实例目录。
            <br />
            <b>web 模式的目录浏览与新建会话需要 ZCode 桌面宿主</b>（宿主经 <code>/ws/host</code>
            连到服务上）；面板只起 <code>--web</code> 这一半，没有宿主时界面会显示「无法获取系统信息 /
            无子目录」、创建会话卡住——那是 ZCode 的 host 模型，不是面板在限制它（ZCode 进程与面板同权限）。
            要真正干活请用<b>终端</b>：面板跑的是 ZCode 官方的 TUI，不需要宿主。
          </div>
        </CollapsibleNote>
      </div>

      <div className="list-body scroll-hover">
        {instances.length === 0 ? (
          <div className="empty-state" style={{ padding: "6px 12px" }}>
            <span>还没有 ZCode 实例</span>
            <span className="empty-sub">发行包要在上面的「版本」卡里构建或放置</span>
          </div>
        ) : (
          instances.map((inst, idx) => (
            <div key={inst.id}>
              <div className="tlli" style={{ padding: "6px 12px" }}>
                {/* 「当前实例」与 dsh 实例列表同款：启动面板与主侧栏都认这个选择 */}
                <label className="input-check">
                  <input
                    type="radio"
                    name="brand-instance-zcode"
                    checked={inst.id === selectedId}
                    onChange={() => setSelectedInstanceId(inst.id)}
                  />
                </label>
                <span className="tlli-text">
                  <span className="tlli-title">
                    {inst.name}
                    <span className={`tag ${STATE_TAG[inst.state] ?? "plain"}`}>
                      {STATE_TEXT[inst.state] ?? inst.state}
                    </span>
                    {!inst.hasApiKey && <span className="tag plain">无 API Key</span>}
                  </span>
                  <span className="tlli-subtitle" title={inst.workspacePath ?? undefined}>
                    {inst.id}
                    {(inst.version ?? s.zcodeDist?.version)
                      ? ` · 版本 ${inst.version ?? `current (${s.zcodeDist?.version})`}`
                      : ""}
                    {inst.credentialFormat
                      ? ` · 凭据 v${inst.credentialFormat}`
                      : (inst.requiresCredentialSecret
                        || (s.zcodeDist?.version && compareSemver(s.zcodeDist.version, "3.18.0") >= 0))
                        ? " · 凭据 v2 · 需 secret"
                        : ""}
                    {inst.keyFingerprint ? ` · key ${inst.keyFingerprint.slice(0, 8)}` : ""}
                    {inst.minVersion ? ` · 最低 ${inst.minVersion}` : ""}
                    {inst.workspacePath ? ` · 工作区 ${inst.workspacePath}` : ""}
                    {inst.state === "running" ? " · 经面板反代打开" : ""}
                    {inst.state === "error" && inst.error ? ` · ${inst.error}` : ""}
                  </span>
                </span>
                <span className="row-actions">
                  <button
                    className="icon-btn on-variant ripple-host"
                    title={inst.state === "running" ? "停止" : "启动"}
                    disabled={busy || inst.state === "starting"}
                    onClick={() => (inst.state === "running" ? void stop(inst) : void launch(inst))}
                  >
                    {inst.state === "running" ? <CloseIcon size={18} /> : <RocketLaunchIcon size={18} />}
                  </button>
                  <button
                    className="icon-btn on-variant ripple-host"
                    title="打开终端（ZCode TUI）"
                    disabled={busy || !webShellEnabled || !s.zcodeDist?.present}
                    onClick={() => setTerminalTarget({ kind: "zcode", id: inst.id, name: inst.name })}
                  >
                    <TerminalIcon size={18} />
                  </button>
                  <button
                    className="icon-btn on-variant ripple-host"
                    title="在新标签页打开（经面板 /i/<id>/ 反代）"
                    disabled={inst.state !== "running"}
                    onClick={() => void openInBrowser(inst)}
                  >
                    <PublicIcon size={18} />
                  </button>
                  <button
                    className="icon-btn on-variant ripple-host"
                    title={inst.state === "running" || inst.state === "starting"
                      ? "先停止实例再更换密钥"
                      : "更换密钥"}
                    disabled={busy || inst.state === "running" || inst.state === "starting"}
                    onClick={() => {
                      setRotateSeed("");
                      setRotateClearKey(true);
                      setRotateTarget(inst);
                    }}
                  >
                    <LockIcon size={18} />
                  </button>
                  <button
                    className="icon-btn on-variant ripple-host"
                    title={I18N["dsh.instance.menu"]}
                    onClick={(e) => setMenuAnchor({ el: e.currentTarget, inst })}
                  >
                    <MoreVertIcon size={18} />
                  </button>
                </span>
              </div>
              {idx < instances.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
            </div>
          ))
        )}
      </div>

      {/* 更多菜单：行里只留启停与打开（手机上一排按钮挤不下），其余进菜单 */}
      <PopupMenu
        open={menuAnchor !== null}
        anchor={menuAnchor?.el ?? null}
        onClose={() => setMenuAnchor(null)}
      >
        <PopupItem
          icon={<EditIcon size={16} />}
          onClick={() => {
            const inst = menuAnchor?.inst;
            setMenuAnchor(null);
            if (inst) openEdit(inst);
          }}
        >
          编辑（名称 / 端点 / 密钥）
        </PopupItem>
        <PopupItem
          icon={<ExtensionIcon size={16} />}
          onClick={() => {
            const inst = menuAnchor?.inst;
            setMenuAnchor(null);
            if (inst) setPluginsTarget(inst);
          }}
        >
          {I18N["dsh.brand.instance.plugins"]}
        </PopupItem>
        <PopupItem
          icon={<EditIcon size={16} />}
          onClick={() => {
            const inst = menuAnchor?.inst;
            setMenuAnchor(null);
            if (inst) {
              setRotateSeed("");
              setRotateClearKey(true);
              setRotateTarget(inst);
            }
          }}
        >
          更换密钥
        </PopupItem>
        <PopupItem
          icon={<FormatListBulletedIcon size={16} />}
          onClick={() => {
            const inst = menuAnchor?.inst;
            setMenuAnchor(null);
            if (inst) void showLogs(inst);
          }}
        >
          日志
        </PopupItem>
        <PopupSep />
        <PopupItem
          danger
          icon={<DeleteIcon size={16} />}
          onClick={() => {
            const inst = menuAnchor?.inst;
            setMenuAnchor(null);
            if (inst) setDeleteTarget(inst);
          }}
        >
          删除
        </PopupItem>
      </PopupMenu>

      {/* 新建 / 编辑 */}
      <Dialog
        open={createOpen || editTarget !== null}
        title={editTarget ? "编辑 ZCode 实例" : "添加 ZCode 实例（实验性）"}
        onClose={closeForm}
      >
        <div className="form-row">
          <span className="form-label">名称</span>
          <input
            className="input"
            autoFocus
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
        </div>
        <div className="form-row">
          <span className="form-label">版本</span>
          <select
            className="input"
            value={form.version}
            onChange={(e) => setForm((f) => ({ ...f, version: e.target.value }))}
          >
            <option value="">跟随 current（历史兼容）</option>
            {catalog.map((entry) => (
              <option key={entry.ref} value={entry.ref} disabled={versionDisabled(entry, editTarget)}>
                {catalogLabel(entry)}
              </option>
            ))}
          </select>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          只列已构建的版本；未构建的先去「版本」卡构建。实例用过更高版本后，低于下限的项会被禁用。
        </p>
        {!editTarget && (
          <>
            <div className="form-row">
              <span className="form-label">密钥</span>
              <label className="input-check">
                <input
                  type="radio"
                  checked={!form.useSeed}
                  onChange={() => setForm((f) => ({ ...f, useSeed: false }))}
                />
                自动生成（推荐）
              </label>
              <label className="input-check">
                <input
                  type="radio"
                  checked={form.useSeed}
                  onChange={() => setForm((f) => ({ ...f, useSeed: true }))}
                />
                使用种子
              </label>
            </div>
            {form.useSeed && (
              <div className="form-row">
                <span className="form-label">种子</span>
                <input
                  className="input"
                  type="password"
                  placeholder="面板用 PBKDF2 派生 credential-secret；丢失后需重置凭据"
                  value={form.seed}
                  onChange={(e) => setForm((f) => ({ ...f, seed: e.target.value }))}
                />
              </div>
            )}
          </>
        )}
        <div className="form-row">
          <span className="form-label">工作区</span>
          <input
            className="input"
            // 工作区决定 agent 在哪个目录里干活；建好之后不再改（改了等于换一个项目，
            // 会话与相对路径都会对不上），所以编辑时只显示、不接收输入。
            readOnly={editTarget !== null}
            placeholder={
              editTarget
                ? form.workspace
                : `留空 = 面板自动新建：${s.zcodeWorkspacesRoot ?? "<数据目录>/zcode/workspaces"}/<实例 id>`
            }
            value={form.workspace}
            onChange={(e) => setForm((f) => ({ ...f, workspace: e.target.value }))}
          />
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          工作区是 agent 干活的目录。留空时面板立刻建好一个（删实例不会删它）；要指向现有项目就填它的绝对路径。
        </p>
        <div className="form-row">
          <span className="form-label">API Base URL</span>
          <input
            className="input"
            value={form.baseUrl}
            onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))}
          />
        </div>
        <div className="form-row">
          <span className="form-label">API Key</span>
          <input
            className="input"
            type="password"
            placeholder={
              editTarget
                ? editTarget.hasApiKey
                  ? "留空则不修改（已设置）"
                  : "留空则不注入"
                : "选填；明文保存并写入实例目录"
            }
            value={form.apiKey}
            onChange={(e) => setForm((f) => ({ ...f, apiKey: e.target.value }))}
          />
        </div>
        {formError && <div className="form-error">{formError}</div>}
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          保存时会把凭证写入该实例的 <code>.zcode/v2/provider_config.json</code>（ZCode 的个人供应商配置）。
          此格式取自 ZCode 源码（v3.14.3），上游改动后可能失效——按实验性功能对待。
        </p>
        <div className="dialog-actions">
          <button className="btn btn-text dim" disabled={busy} onClick={closeForm}>
            取消
          </button>
          <button className="btn btn-text" disabled={busy} onClick={() => void (editTarget ? submitEdit() : submitCreate())}>
            保存
          </button>
        </div>
      </Dialog>

      {/* 日志 */}
      <Dialog open={logTarget !== null} title={`日志 · ${logTarget?.name ?? ""}`} wide onClose={() => setLogTarget(null)}>
        <pre style={{ maxHeight: 360, overflow: "auto", fontSize: 12, lineHeight: 1.5, margin: 0 }}>
          {logLines.length === 0 ? "（暂无日志）" : logLines.join("\n")}
        </pre>
        <div className="dialog-actions">
          <button className="btn btn-text dim" onClick={() => setLogTarget(null)}>
            关闭
          </button>
          <button className="btn btn-text" onClick={() => logTarget && void showLogs(logTarget)}>
            刷新
          </button>
        </div>
      </Dialog>

      {/* 插件通道：形状与品牌实例卡一致（弹窗各持一份，免得两个组件文件互相引用） */}
      <ZcodePluginsDialog inst={pluginsTarget} onClose={() => setPluginsTarget(null)} />

      {/* 原地终端：与品牌实例共用 TerminalWindow，协议同为 docs/terminal-protocol.md 的 v1 */}
      <TerminalWindow target={terminalTarget} onClose={() => setTerminalTarget(null)} />

      <Dialog
        open={rotateTarget !== null}
        title={`更换密钥 · ${rotateTarget?.name ?? ""}`}
        onClose={() => setRotateTarget(null)}
      >
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.7 }}>
          旧 credential-secret 和已加密凭据会被丢弃，之后需要重新添加 API Key / 登录。
          会话、工作区和任务索引不受影响。
        </p>
        <div className="form-row">
          <span className="form-label">种子</span>
          <input
            className="input"
            type="password"
            placeholder="留空 = 自动生成；填写 = 用种子派生新密钥"
            value={rotateSeed}
            onChange={(e) => setRotateSeed(e.target.value)}
          />
        </div>
        <label className="input-check">
          <input
            type="checkbox"
            checked={rotateClearKey}
            onChange={(e) => setRotateClearKey(e.target.checked)}
          />
          同时清除已保存的 API Key
        </label>
        <div className="dialog-actions">
          <button className="btn btn-text dim" disabled={busy} onClick={() => setRotateTarget(null)}>
            取消
          </button>
          <button className="btn btn-text" disabled={busy} onClick={() => void rotateKey()}>
            更换
          </button>
        </div>
      </Dialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除 ZCode 实例"
        message={`删除「${deleteTarget?.name ?? ""}」？实例目录（含会话数据与凭证）会一并删除。默认工作区在 zcode/workspaces 下，不会被一并删除。`}
        confirmLabel="删除"
        danger
        busy={busy}
        onConfirm={() => void remove()}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}

/// ZCode 的插件弹窗：走 /api/zcode/instances/<id>/plugins，回答恒为
/// `{supported: false, plugins: []}`——如实画出来，后端填上 plugins 就按数据列。
function ZcodePluginsDialog({
  inst,
  onClose,
}: {
  inst: ExternalInstance | null;
  onClose: () => void;
}) {
  const [data, setData] = useState<InstancePlugins | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!inst) return;
    let alive = true;
    setData(null);
    setError(null);
    api
      .zcodePlugins(inst.id)
      .then((d) => {
        if (alive) setData(d);
      })
      .catch((e) => {
        if (alive) setError(errMsg(e));
      });
    return () => {
      alive = false;
    };
  }, [inst]);

  return (
    <Dialog
      open={inst !== null}
      title={`${I18N["dsh.brand.instance.plugins"]} · ${inst?.name ?? ""}`}
      onClose={onClose}
    >
      {error ? (
        <div className="hint error">{error}</div>
      ) : data && data.plugins.length > 0 ? (
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          {data.plugins.map((p) => (
            <div key={p.name} className="toolbar-row" style={{ padding: 0 }}>
              <span style={{ fontSize: 13 }}>{p.name}</span>
              {p.version && <span className="tag plain">{p.version}</span>}
              <span className={`tag ${p.enabled ? "running" : "plain"}`}>
                {p.enabled ? I18N["button.enable"] : I18N["button.disable"]}
              </span>
            </div>
          ))}
        </div>
      ) : data?.supported ? (
        <p style={{ lineHeight: 1.7 }}>{I18N["dsh.brand.plugins.empty"]}</p>
      ) : data ? (
        <p style={{ lineHeight: 1.7 }}>{I18N["dsh.brand.plugins.unsupported"]}</p>
      ) : (
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span className="spinner" />
          <span>正在读取插件通道…</span>
        </div>
      )}
      <div className="dialog-actions">
        <button className="btn btn-text" onClick={onClose}>
          {I18N["button.ok"]}
        </button>
      </div>
    </Dialog>
  );
}