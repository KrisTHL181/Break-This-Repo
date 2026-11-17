import { useState } from "react";
import { api } from "../api";
import { settleElevation, useElevationPromptOpen } from "../elevation";
import { toast } from "../store";
import { Dialog } from "./Dialog";

/**
 * 开发者密码输入框的宿主：挂在 App 顶层，`requestDeveloperPassword()`
 * 一被调用就出现。
 *
 * 密码只发给 `POST /api/developer/elevate`，成功后由调用方重试原操作；
 * 这里不缓存密码，也不做任何"记住我"。
 */
export function ElevationHost() {
  const open = useElevationPromptOpen();
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const close = () => {
    if (busy) return;
    setPassword("");
    setError(null);
    settleElevation(false);
  };

  const submit = async () => {
    if (busy || password === "") return;
    setBusy(true);
    setError(null);
    try {
      await api.elevateDeveloper(password);
      setPassword("");
      setBusy(false);
      toast("success", "已提权");
      settleElevation(true);
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e);
      if (message.includes("tls_required")) {
        setError("面板不在 HTTPS 下，开发者模式不可用：请先在「设置 → HTTPS 证书」启用后重试。");
      } else if (message.includes("developer_password_not_set")) {
        setError("还没有设置开发者密码：请先在「设置 → 开发者模式」里设置。");
      } else if (message.includes("too many failed attempts")) {
        setError("失败次数过多，已临时锁定；请稍后再试。");
      } else {
        setError("开发者密码不正确。");
      }
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} title="需要开发者密码" onClose={close}>
      <div style={{ maxWidth: 420, lineHeight: 1.7 }}>
        <p style={{ marginTop: 0 }}>
          这个操作被开发者守卫挡住了。输入开发者密码后会自动继续——提权 30 分钟内有效（可延长）。
        </p>
        <input
          className="input"
          type="password"
          autoComplete="off"
          autoFocus
          placeholder="开发者密码"
          value={password}
          disabled={busy}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") void submit();
            if (e.key === "Escape") close();
          }}
        />
        {error && <div className="form-error">{error}</div>}
        <div className="dialog-actions">
          <button className="btn btn-text dim" disabled={busy} onClick={close}>
            取消
          </button>
          <button className="btn btn-raised" disabled={busy || password === ""} onClick={() => void submit()}>
            {busy ? "验证中…" : "提权并继续"}
          </button>
        </div>
      </div>
    </Dialog>
  );
}
