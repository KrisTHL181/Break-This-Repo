import { useEffect, useState } from "react";
import { api, isNotImplemented } from "../api";
import { ConfirmDialog, Dialog } from "../components/Dialog";
import {
  AddIcon,
  CheckCircleIcon,
  ErrorIcon,
  PersonIcon,
  RefreshIcon,
  DeleteIcon,
} from "../components/icons";
import { ModelSelect } from "../components/ModelSelect";
import { I18N } from "../i18n";
import { loadAccounts, loadModelProviders, toast, useAppState } from "../store";
import type { Account, CreateAccountBody, ModelProvider, PatchAccountBody } from "../types";
import { errMsg } from "../utils";

const CUSTOM_VENDOR = "__custom";

/** 目录抓取时间 → "刚刚更新" / "5 分钟前更新" */
function agoText(at: number | null): string | null {
  if (at === null) return null;
  const min = Math.floor(Math.max(0, Date.now() - at) / 60000);
  if (min < 1) return "刚刚更新";
  if (min < 60) return `${min} 分钟前更新`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour} 小时前更新`;
  return `${Math.floor(hour / 24)} 天前更新`;
}

interface VerifyResult {
  ok: boolean;
  detail: string;
}

/** 账户页（AccountListPage 结构）：账户卡片网格 + 底部"添加账户"卡片。 */
export function AccountsPage() {
  const s = useAppState();

  const [verifyBusy, setVerifyBusy] = useState<Record<string, boolean>>({});
  const [verifyResult, setVerifyResult] = useState<Record<string, VerifyResult>>({});

  const [addOpen, setAddOpen] = useState(false);
  const [addStep, setAddStep] = useState<"vendor" | "form">("vendor");
  const [picked, setPicked] = useState<ModelProvider | typeof CUSTOM_VENDOR | null>(null);
  const [vendorQuery, setVendorQuery] = useState("");
  const [form, setForm] = useState({ vendor: "", endpoint: "", apiKey: "", label: "", model: "" });
  const [submitBusy, setSubmitBusy] = useState(false);
  const [submitWarn, setSubmitWarn] = useState<string | null>(null);

  const [editTarget, setEditTarget] = useState<Account | null>(null);
  const [editForm, setEditForm] = useState({ label: "", model: "", apiKey: "" });
  const [editBusy, setEditBusy] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<Account | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  useEffect(() => {
    void loadAccounts();
    void loadModelProviders();
  }, []);

  const openAdd = () => {
    setPicked(null);
    setVendorQuery("");
    setAddStep("vendor");
    setForm({ vendor: "", endpoint: "", apiKey: "", label: "", model: "" });
    setSubmitWarn(null);
    setAddOpen(true);
  };

  const pickVendor = (v: ModelProvider | typeof CUSTOM_VENDOR) => {
    setPicked(v);
    if (v === CUSTOM_VENDOR) {
      setForm((f) => ({ ...f, vendor: "", endpoint: "" }));
    } else {
      // 端点自动带上；目录里没有固定端点的供应商留空等用户填
      setForm((f) => ({ ...f, vendor: v.id, endpoint: v.endpoint ?? "" }));
    }
    setAddStep("form");
  };

  const doCreate = async () => {
    const apiKey = form.apiKey.trim();
    if (!apiKey) {
      toast("error", "请输入 API Key");
      return;
    }
    const custom = picked === CUSTOM_VENDOR || picked === null;
    const body: CreateAccountBody = { apiKey };
    if (custom) {
      if (form.vendor.trim()) body.vendor = form.vendor.trim();
      if (form.endpoint.trim()) body.endpoint = form.endpoint.trim();
      if (!body.vendor && !body.endpoint) {
        toast("error", "自定义供应商需填写名称或 Endpoint");
        return;
      }
    } else if (picked.known) {
      // dsh 自家目录里的供应商：端点与协议都以 dsh 的为准
      body.vendor = picked.id;
    } else {
      // 目录里新发现的供应商：dsh 不认识它，必须带上端点（协议用目录推导出的那个）
      const endpoint = form.endpoint.trim();
      if (!endpoint) {
        toast("error", "该供应商没有固定端点，请填写 Endpoint");
        return;
      }
      body.vendor = picked.id;
      body.endpoint = endpoint;
      if (picked.protocol) body.protocol = picked.protocol;
    }
    if (form.label.trim()) body.label = form.label.trim();
    if (form.model.trim()) body.model = form.model.trim();
    // `kind` 是账户的类型（official / third-party / offline），由后端按供应商推断：
    // 别再发它 —— 目录里的 `protocol` 报的是协议（openai-completions 之类），
    // 拿它当 kind 发过去会被 /api/accounts 拒绝（"kind must be one of …"）。

    setSubmitBusy(true);
    setSubmitWarn(null);
    try {
      const r = await api.createAccount(body);
      await loadAccounts(true);
      if (r.verified === false) {
        setSubmitWarn(r.verifyError || "验证失败，账户已保存，可稍后在列表中重新验证");
      } else {
        toast("success", `账户「${r.account.name}」已添加并验证通过`);
        setAddOpen(false);
      }
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持账户管理");
      else toast("error", `添加失败：${errMsg(e)}`);
    } finally {
      setSubmitBusy(false);
    }
  };

  const doVerify = async (name: string) => {
    setVerifyBusy((m) => ({ ...m, [name]: true }));
    try {
      const r = await api.verifyAccount(name);
      if (r.ok) {
        const n = r.models?.length ?? 0;
        if (n === 0 && r.note) {
          // 验证通过却一个模型都列不出来：启动时这条路由写不进去（harness 不认它，
          // 自己也没法补），所以这里必须把供应商的原话摆出来，并指路到“模型”栏。
          const detail = `读不到模型列表（${r.note}）；启动前请在账户里填一个模型名`;
          setVerifyResult((m) => ({ ...m, [name]: { ok: false, detail } }));
          toast("error", `「${name}」验证通过，但${detail}`);
          return;
        }
        setVerifyResult((m) => ({ ...m, [name]: { ok: true, detail: `${n} 个可用模型` } }));
        toast("success", `「${name}」验证通过，${n} 个可用模型`);
      } else {
        const detail = r.error ?? "验证失败";
        setVerifyResult((m) => ({ ...m, [name]: { ok: false, detail } }));
        toast("error", `「${name}」验证失败：${detail}`);
      }
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持账户验证");
      else toast("error", `验证失败：${errMsg(e)}`);
    } finally {
      setVerifyBusy((m) => ({ ...m, [name]: false }));
    }
  };

  const openEdit = (a: Account) => {
    setEditForm({ label: a.label ?? "", model: a.model ?? "", apiKey: "" });
    setEditTarget(a);
  };

  const doEdit = async () => {
    if (!editTarget) return;
    const body: PatchAccountBody = {
      label: editForm.label.trim(),
      model: editForm.model.trim(),
    };
    if (editForm.apiKey.trim()) body.apiKey = editForm.apiKey.trim();
    setEditBusy(true);
    try {
      await api.patchAccount(editTarget.name, body);
      toast("success", "账户已更新");
      setEditTarget(null);
      await loadAccounts(true);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持账户管理");
      else toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setEditBusy(false);
    }
  };

  const doDelete = async () => {
    if (!deleteTarget) return;
    setDeleteBusy(true);
    try {
      await api.deleteAccount(deleteTarget.name);
      toast("success", `账户「${deleteTarget.name}」已删除`);
      setDeleteTarget(null);
      await loadAccounts(true);
    } catch (e) {
      if (isNotImplemented(e)) toast("info", "后端尚未支持账户管理");
      else toast("error", `删除失败：${errMsg(e)}`);
    } finally {
      setDeleteBusy(false);
    }
  };

  const accounts = s.accounts;
  const providers = s.modelProviders ?? [];
  const pickedVendor = picked && picked !== CUSTOM_VENDOR ? picked : null;
  const builtin = s.modelProvidersSource === "builtin";
  const updated = builtin ? null : agoText(s.modelProvidersAt);
  const q = vendorQuery.trim().toLowerCase();
  const visibleProviders = providers.filter(
    (p) =>
      !q ||
      p.name.toLowerCase().includes(q) ||
      p.id.toLowerCase().includes(q) ||
      (p.endpoint ?? "").toLowerCase().includes(q),
  );

  return (
    <div className="page-content scroll" style={{ gap: 10 }}>
      <div className="card toolbar-row">
        <span style={{ fontSize: 14, fontWeight: 600 }}>{I18N["account"]}</span>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
          {accounts ? `共 ${accounts.length} 个账户` : s.accountsLoading ? "加载中…" : "管理各模型供应商的 API Key"}
        </span>
        <span className="spacer" />
        <button className="tool-btn ripple-host" disabled={s.accountsLoading} onClick={() => void loadAccounts(true)}>
          <RefreshIcon size={20} />
          {I18N["button.refresh"]}
        </button>
      </div>

      {s.accountsLoading && !accounts ? (
        <div className="card" style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 10, padding: 40 }}>
          <span className="spinner" />
          <span>正在获取账户列表…</span>
        </div>
      ) : !accounts ? (
        <div className="card empty-state">
          <ErrorIcon size={30} />
          <span style={{ fontSize: 20 }}>账户列表不可用</span>
          <span className="empty-sub">{s.accountsError ?? "未知错误"}</span>
          <button className="btn btn-outline ripple-host" onClick={() => void loadAccounts(true)}>
            重试
          </button>
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
            gap: 10,
          }}
        >
          {accounts.map((a) => {
            const vr = verifyResult[a.name];
            return (
              <div className="account-card" key={a.name}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span className="account-avatar">
                    {a.label ? a.label.slice(0, 1).toUpperCase() : <PersonIcon size={20} />}
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 15, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                      {a.label || a.name}
                    </div>
                    <div style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                      {a.vendor}
                      {a.model ? ` · ${a.model}` : ""}
                      {a.kind ? ` · ${a.kind}` : ""}
                    </div>
                  </div>
                </div>
                <div style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
                  <div style={{ display: "flex", gap: 8 }}>
                    <span style={{ width: 64, flex: "none", color: "var(--monet-on-surface-variant)" }}>API Key</span>
                    <code className="mono" style={{ fontSize: 11 }}>{a.maskedKey}</code>
                  </div>
                  {a.endpoint && (
                    <div style={{ display: "flex", gap: 8 }}>
                      <span style={{ width: 64, flex: "none", color: "var(--monet-on-surface-variant)" }}>Endpoint</span>
                      <span className="mono" style={{ fontSize: 11, wordBreak: "break-all" }}>{a.endpoint}</span>
                    </div>
                  )}
                  {vr && (
                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                      <span style={{ width: 64, flex: "none", color: "var(--monet-on-surface-variant)" }}>验证</span>
                      <span className={`tag ${vr.ok ? "running" : "failed"}`}>
                        {vr.ok ? <CheckCircleIcon size={12} /> : <ErrorIcon size={12} />}
                        {vr.ok ? `通过（${vr.detail}）` : vr.detail}
                      </span>
                    </div>
                  )}
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <button
                    className="btn btn-text ripple-host"
                    style={{ height: 28 }}
                    disabled={!!verifyBusy[a.name]}
                    onClick={() => void doVerify(a.name)}
                  >
                    {verifyBusy[a.name] ? "验证中…" : "验证"}
                  </button>
                  <span style={{ flex: 1 }} />
                  <button className="btn btn-text ripple-host" style={{ height: 28 }} onClick={() => openEdit(a)}>
                    编辑
                  </button>
                  <button
                    className="btn btn-text danger ripple-host"
                    style={{ height: 28 }}
                    onClick={() => setDeleteTarget(a)}
                  >
                    <DeleteIcon size={13} />
                    删除
                  </button>
                </div>
              </div>
            );
          })}

          {/* 底部"添加账户"卡片 */}
          <button
            className="account-card ripple-host"
            style={{
              alignItems: "center",
              justifyContent: "center",
              minHeight: 140,
              color: "var(--monet-primary)",
              fontSize: 14,
              gap: 8,
            }}
            onClick={openAdd}
          >
            <AddIcon size={22} />
            {I18N["dsh.account.add"]}
          </button>
        </div>
      )}

      {/* 添加账户：供应商目录 → 表单 */}
      <Dialog
        open={addOpen}
        title={addStep === "vendor" ? "选择供应商" : I18N["dsh.account.add"]}
        onClose={() => setAddOpen(false)}
        wide={addStep === "vendor"}
      >
        {addStep === "vendor" ? (
          <>
            <div className="vendor-toolbar">
              <input
                className="input"
                placeholder="搜索供应商（名称 / id / 端点）"
                value={vendorQuery}
                onChange={(e) => setVendorQuery(e.target.value)}
              />
              <button
                className="tool-btn ripple-host"
                disabled={s.modelProvidersLoading}
                onClick={() => void loadModelProviders(true)}
              >
                <RefreshIcon size={20} />
                {s.modelProvidersLoading ? "刷新中…" : I18N["button.refresh"]}
              </button>
              <span className="vendor-count">
                {providers.length > 0
                  ? `共 ${providers.length} 家${builtin ? "内置供应商（目录未就绪）" : "供应商"}${updated ? ` · ${updated}` : ""}`
                  : s.modelProvidersLoading
                    ? "正在获取供应商名单…"
                    : "共 0 家供应商"}
              </span>
            </div>
            <div className="vendor-grid">
              {visibleProviders.map((p) => {
                const blocked = p.protocol === null;
                return (
                  <button
                    key={p.id}
                    className="wizard-card ripple-host"
                    disabled={blocked}
                    onClick={() => pickVendor(p)}
                  >
                    <span className="wizard-card-text">
                      <div className="wizard-card-title">
                        {p.name}
                        {!p.known && <span className="tag plain">目录</span>}
                      </div>
                      <div className="wizard-card-subtitle">
                        {blocked
                          ? `dsh 不支持该协议（models.dev 用 ${p.npm ?? "未知协议"}）`
                          : p.endpoint ?? "需自行填写端点"}
                      </div>
                      <div style={{ display: "flex", gap: 4, marginTop: 4, flexWrap: "wrap" }}>
                        {p.protocol && <span className="tag plain">{p.protocol}</span>}
                        <span className="tag plain">{p.modelCount} 个模型</span>
                      </div>
                    </span>
                  </button>
                );
              })}
              <button className="wizard-card ripple-host" onClick={() => pickVendor(CUSTOM_VENDOR)}>
                <span className="wizard-card-text">
                  <div className="wizard-card-title">{I18N["dsh.account.add.custom"]}</div>
                  <div className="wizard-card-subtitle">兼容 OpenAI 接口的任意服务</div>
                </span>
              </button>
            </div>
            {visibleProviders.length === 0 && (
              <p className="vendor-empty">
                {providers.length > 0
                  ? `没有匹配「${vendorQuery.trim()}」的供应商。`
                  : s.modelProvidersLoading
                    ? "正在获取供应商名单…"
                    : "供应商名单为空。"}
              </p>
            )}
            {s.modelProvidersError && (
              <p style={{ fontSize: 12, marginTop: 10 }}>
                模型目录加载失败（{s.modelProvidersError}），可直接使用自定义 Endpoint 手填模型名。
              </p>
            )}
            {builtin && !s.modelProvidersError && (
              <p style={{ fontSize: 12, marginTop: 10, color: "var(--monet-on-surface-variant)" }}>
                模型目录暂时不可达，下面是面板内置的供应商（DeepSeek、Moonshot、智谱等可直接添加）。
                稍后点「刷新」重试完整目录。
              </p>
            )}
          </>
        ) : (
          <>
            <div className="form-row">
              <span className="form-label">{I18N["dsh.account.vendor"]}</span>
              {pickedVendor ? (
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span>{pickedVendor.name}</span>
                  <button className="btn btn-text" style={{ height: 26 }} onClick={() => setAddStep("vendor")}>
                    更换
                  </button>
                </div>
              ) : (
                <input
                  className="input"
                  placeholder="供应商名称（如 openai）"
                  value={form.vendor}
                  onChange={(e) => setForm((f) => ({ ...f, vendor: e.target.value }))}
                />
              )}
            </div>
            {(!pickedVendor || !pickedVendor.known) && (
              <div className="form-row">
                <span className="form-label">{I18N["dsh.account.base_url"]}</span>
                <input
                  className="input"
                  placeholder="https://api.example.com/v1"
                  value={form.endpoint}
                  onChange={(e) => setForm((f) => ({ ...f, endpoint: e.target.value }))}
                />
              </div>
            )}
            {pickedVendor && (
              <div className="form-row">
                <span className="form-label">协议</span>
                <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                  {pickedVendor.protocol ?? "未知"}（
                  {pickedVendor.protocolSource === "dsh" ? "取自 dsh 目录" : "取自 models.dev 目录"}
                  ；账户类型由后端推断）
                </span>
              </div>
            )}
            <div className="form-row">
              <span className="form-label">{I18N["dsh.account.key"]}</span>
              <input
                className="input"
                type="password"
                placeholder={pickedVendor ? `形如 ${pickedVendor.envVar} 的值` : "sk-…"}
                value={form.apiKey}
                onChange={(e) => setForm((f) => ({ ...f, apiKey: e.target.value }))}
                autoComplete="off"
              />
            </div>
            <div className="form-row">
              <span className="form-label">{I18N["dsh.account.label"]}（选填）</span>
              <input
                className="input"
                placeholder="便于分辨同一个供应商的两个密钥"
                value={form.label}
                onChange={(e) => setForm((f) => ({ ...f, label: e.target.value }))}
              />
            </div>
            <div className="form-row">
              <span className="form-label">默认模型（自建供应商建议填）</span>
              <ModelSelect
                value={form.model}
                onChange={(v) => setForm((f) => ({ ...f, model: v }))}
                vendorId={pickedVendor ? pickedVendor.id : form.vendor.trim()}
                placeholder="可搜索模型，或直接输入；读不到供应商列表时，这里填的名字就是兜底"
              />
            </div>
            {submitWarn && (
              <div className="hint warning" style={{ marginBottom: 8 }}>
                账户已保存，但现场验证失败：{submitWarn}
              </div>
            )}
            <div className="dialog-actions">
              {submitWarn ? (
                <button className="btn btn-text" onClick={() => setAddOpen(false)}>
                  完成
                </button>
              ) : (
                <>
                  <button className="btn btn-text dim" disabled={submitBusy} onClick={() => setAddOpen(false)}>
                    {I18N["button.cancel"]}
                  </button>
                  <button className="btn btn-text" disabled={submitBusy} onClick={() => void doCreate()}>
                    {submitBusy ? "验证并保存中…" : I18N["dsh.account.add.button"]}
                  </button>
                </>
              )}
            </div>
          </>
        )}
      </Dialog>

      {/* 编辑账户 */}
      <Dialog
        open={editTarget !== null}
        title={`编辑账户「${editTarget?.name ?? ""}」`}
        onClose={() => setEditTarget(null)}
      >
        <div className="form-row">
          <span className="form-label">{I18N["dsh.account.label"]}</span>
          <input
            className="input"
            value={editForm.label}
            onChange={(e) => setEditForm((f) => ({ ...f, label: e.target.value }))}
          />
        </div>
        <div className="form-row">
          <span className="form-label">默认模型</span>
          <ModelSelect
            value={editForm.model}
            onChange={(v) => setEditForm((f) => ({ ...f, model: v }))}
            vendorId={editTarget?.vendor ?? ""}
            placeholder="读不到供应商列表时，这里填的名字就是兜底"
          />
        </div>
        <div className="form-row">
          <span className="form-label">{I18N["dsh.account.change_key"]}</span>
          <input
            className="input"
            type="password"
            placeholder="留空保持不变"
            value={editForm.apiKey}
            onChange={(e) => setEditForm((f) => ({ ...f, apiKey: e.target.value }))}
            autoComplete="off"
          />
        </div>
        <div className="dialog-actions">
          <button className="btn btn-text dim" disabled={editBusy} onClick={() => setEditTarget(null)}>
            {I18N["button.cancel"]}
          </button>
          <button className="btn btn-text" disabled={editBusy} onClick={() => void doEdit()}>
            {editBusy ? "保存中…" : I18N["button.save"]}
          </button>
        </div>
      </Dialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除账户"
        message={I18N["dsh.account.remove.confirm"].replace(
          "%s",
          deleteTarget?.label || deleteTarget?.name || "",
        )}
        confirmLabel={I18N["button.delete"]}
        danger
        busy={deleteBusy}
        onConfirm={() => void doDelete()}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
