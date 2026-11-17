import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { ApiError } from "../api";
import { AuthScaffold } from "../components/AuthScaffold";
import { ErrorIcon } from "../components/icons";
import { setup, useAppState } from "../store";
import { errMsg } from "../utils";

/** 首次启动引导页：服务端没有任何用户时，创建第一个管理员并直接登录。
 *  与登录页共用壁纸 + 居中卡片布局（AuthScaffold），仅表单不同。 */
export function SetupPage() {
  const { username, setupRequired, authChecked } = useAppState();
  const loc = useLocation();
  const from = (loc.state as { from?: string } | null)?.from;
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [initialized, setInitialized] = useState(false);
  const [busy, setBusy] = useState(false);

  if (username) return <Navigate to={from ?? "/"} replace />;
  // 已有用户时引导页没有意义；authChecked 之前保持渲染（bootstrap 正在进行），
  // 避免直达 /setup 时卡片闪烁。
  if (authChecked && !setupRequired) return <Navigate to="/login" replace />;

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (busy) return;
    const trimmed = name.trim();
    if (!trimmed || /\s/.test(trimmed) || trimmed.length > 32) {
      setError("用户名需为 1–32 位且不含空白字符");
      return;
    }
    if (password.length < 6) {
      setError("密码至少 6 位");
      return;
    }
    if (password !== confirm) {
      setError("两次输入的密码不一致");
      return;
    }
    setBusy(true);
    setError(null);
    setInitialized(false);
    try {
      await setup(trimmed, password);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) setInitialized(true);
      setError(err instanceof ApiError && err.status === 409 ? "系统已完成初始化" : errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthScaffold title="初始化 HDSL-web">
      <form className="dialog-body" onSubmit={(e) => void submit(e)} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <div className="form-row" style={{ marginBottom: 0 }}>
          <span className="form-label">用户名</span>
          <input
            className="input"
            autoComplete="username"
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={busy}
          />
        </div>
        <div className="form-row" style={{ marginBottom: 0 }}>
          <span className="form-label">密码</span>
          <input
            className="input"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={busy}
          />
        </div>
        <div className="form-row" style={{ marginBottom: 0 }}>
          <span className="form-label">确认密码</span>
          <input
            className="input"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            disabled={busy}
          />
        </div>
        {error && (
          <div className="form-error">
            <ErrorIcon size={14} />
            {error}
          </div>
        )}
        <button className="btn btn-raised ripple-host" type="submit" disabled={busy} style={{ marginTop: 4 }}>
          {busy ? "创建中…" : "创建并进入"}
        </button>
        {initialized && (
          <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            已有账号？<Link to="/login" style={{ color: "var(--monet-primary)" }}>前往登录</Link>
          </p>
        )}
      </form>
    </AuthScaffold>
  );
}
