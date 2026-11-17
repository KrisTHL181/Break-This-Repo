import { useEffect, useState } from "react";
import { api } from "../api";
import { Dialog } from "../components/Dialog";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import type { BrandAdminCredentials, BrandId } from "../types";
import { errMsg } from "../utils";
import { toast } from "../store";

/**
 * 实例自己的管理员密码（目前只有 Octop 有）。
 *
 * 密码由 Octop 首次启动时生成、写在实例的 `octop-admin.txt`；以前只能进容器跑
 * `octop user passwd`。这里给一个门：
 * - 创建后的 15 分钟内读密码不用第二因子（密码是刚生成的，总得能登录一次）；
 * - 之后读、以及任何重置都要开发者密码；明文 HTTP 下面板无法提权，此时退化为面板密码。
 *
 * 需要面板密码时，对话框里会就地要一次；需要提权时走全局的密码框并自动重试一次。
 */
export function AdminCredentialsDialog({
  brand,
  instance,
  onClose,
}: {
  brand: BrandId;
  instance: { id: string; name: string } | null;
  onClose: () => void;
}) {
  const [credentials, setCredentials] = useState<BrandAdminCredentials | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [panelPassword, setPanelPassword] = useState("");
  const [needsPanelPassword, setNeedsPanelPassword] = useState(false);
  const [customOpen, setCustomOpen] = useState(false);
  const [custom, setCustom] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async (withPanelPassword?: string) => {
    if (!instance) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api.brandAdminPassword(brand, instance.id,
        withPanelPassword ? { adminPassword: withPanelPassword } : {});
      setCredentials(result);
      setNeedsPanelPassword(false);
      setPanelPassword("");
    } catch (e) {
      const raw = errMsg(e);
      if (raw.includes("panel_password_required")) {
        setNeedsPanelPassword(true);
      } else if (isElevationRequired(e)) {
        if (await requestDeveloperPassword()) {
          return load(withPanelPassword);
        }
        setError("需要开发者密码");
      } else {
        setError(raw);
      }
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (instance) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instance?.id]);

  const reset = async (password?: string) => {
    if (!instance) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api.brandAdminPassword(brand, instance.id, {
        password: password ?? "",
        adminPassword: panelPassword || undefined,
      });
      setCredentials(result);
      setRevealed(true);
      setCustomOpen(false);
      setCustom("");
      toast("success", "密码已重置，旧密码立即失效");
    } catch (e) {
      const raw = errMsg(e);
      if (raw.includes("panel_password_required")) {
        setNeedsPanelPassword(true);
        setError("请先输入面板密码");
      } else if (isElevationRequired(e)) {
        if (await requestDeveloperPassword()) {
          return reset(password);
        }
        setError("需要开发者密码");
      } else {
        setError(raw);
      }
    } finally {
      setBusy(false);
    }
  };

  if (!instance) return null;

  return (
    <>
      <Dialog open title={`${instance.name} · 登录密码`} onClose={() => !busy && onClose()}>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          这是 Octop 自己的管理员账号。创建实例后的 15 分钟内可以直接查看；之后查看或重置都需要开发者密码
          （明文 HTTP 下改为面板密码）。
        </p>

        {needsPanelPassword && (
          <div className="form-row">
            <span className="form-label">面板密码</span>
            <input
              className="input"
              type="password"
              autoComplete="off"
              value={panelPassword}
              disabled={busy}
              onChange={(e) => setPanelPassword(e.target.value)}
            />
            <div style={{ display: "flex", gap: 8, paddingTop: 6 }}>
              <button
                className="btn btn-outlined ripple-host"
                disabled={busy || !panelPassword}
                onClick={() => void load(panelPassword)}
              >
                确认并查看
              </button>
            </div>
          </div>
        )}

        {credentials && (
          <>
            <div className="comp-row">
              <span className="comp-label">用户名</span>
              <span className="comp-value mono">{credentials.username}</span>
            </div>
            {credentials.mode === "wizard" ? (
              <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
                这个实例还没完成 Octop 的初始化向导（数据库里还没有管理员）。下面是<b>向导口令</b>：
                打开实例网页，用它进入设置向导，再在那里选管理员的用户名和密码。
              </p>
            ) : null}
            {credentials.password === null ? (
              <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
                面板没有这个实例的明文密码：Octop 只保存哈希，可能是在它的网页向导或控制台里设的。
                直接「重新生成随机密码」即可接管登录。
              </p>
            ) : (
              <div className="comp-row">
                <span className="comp-label">密码</span>
                <span className="comp-value">
                  <span className="mono" style={{ wordBreak: "break-all" }}>
                    {revealed ? credentials.password : "••••••••"}
                  </span>
                  <button className="btn btn-text ripple-host" style={{ height: 24 }} onClick={() => setRevealed((v) => !v)}>
                    {revealed ? "隐藏" : "显示"}
                  </button>
                  <button
                    className="btn btn-text ripple-host"
                    style={{ height: 24 }}
                    onClick={() => {
                      void navigator.clipboard.writeText(credentials.password ?? "").then(
                        () => toast("success", "密码已复制"),
                        () => toast("error", "复制失败：浏览器拒绝了剪贴板访问"),
                      );
                    }}
                  >
                    复制
                  </button>
                </span>
              </div>
            )}
          </>
        )}

        {error && <div className="form-error">{error}</div>}

        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", paddingTop: 8 }}>
          <button className="btn btn-raised ripple-host" disabled={busy} onClick={() => void reset()}>
            {busy ? "处理中…" : "重新生成随机密码"}
          </button>
          <button className="btn btn-outline ripple-host" disabled={busy} onClick={() => setCustomOpen(true)}>
            设为指定密码…
          </button>
          <span className="spacer" style={{ flex: 1 }} />
          <button className="btn btn-text ripple-host" disabled={busy} onClick={onClose}>
            关闭
          </button>
        </div>
      </Dialog>

      <Dialog open={customOpen} title="设置指定密码" onClose={() => !busy && setCustomOpen(false)}>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0" }}>
          至少 8 位。重置后旧密码立即失效，Octop 那边的会话不受影响。
        </p>
        <div className="form-row">
          <span className="form-label">新密码</span>
          <input
            className="input"
            type="password"
            autoComplete="new-password"
            value={custom}
            disabled={busy}
            onChange={(e) => setCustom(e.target.value)}
          />
        </div>
        <div style={{ display: "flex", gap: 8, paddingTop: 8 }}>
          <span className="spacer" style={{ flex: 1 }} />
          <button className="btn btn-text ripple-host" disabled={busy} onClick={() => setCustomOpen(false)}>
            取消
          </button>
          <button
            className="btn btn-raised ripple-host"
            disabled={busy || custom.trim().length < 8}
            onClick={() => void reset(custom.trim())}
          >
            重置
          </button>
        </div>
      </Dialog>
    </>
  );
}