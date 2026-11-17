import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { ApiError } from "../api";
import { AuthScaffold } from "../components/AuthScaffold";
import { login, toast, useAppState } from "../store";
import { errMsg } from "../utils";
import { ErrorIcon } from "../components/icons";

/** 登录页：壁纸 + 居中 HMCL 对话框式登录卡（样式见 ui-spec.md §13）。 */
export function LoginPage() {
  const { username, setupRequired } = useAppState();
  const loc = useLocation();
  const from = (loc.state as { from?: string } | null)?.from;
  const [target, setTarget] = useState<string>(from ?? "/");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (username) return <Navigate to={target} replace />;
  if (setupRequired) return <Navigate to="/setup" replace />;

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (busy) return;
    if (!name.trim() || !password) {
      setError("请输入用户名和密码");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setTarget((loc.state as { from?: string } | null)?.from ?? "/");
      await login(name.trim(), password);
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        // 真的被挡住时才说出来——此前不提示任何「还剩几次」：攻击者看不到的策略才是好策略，
        // 而敲错几次的人也不该被告知一个还没花完的计数。弹一条（会消失）+ 留在表单里一条。
        const throttled = "登录失败次数过多，请稍后再试（约 10 分钟后重置）";
        setError(throttled);
        toast("error", throttled);
      } else {
        setError(
          err instanceof ApiError && err.status === 401 ? "用户名或密码错误" : errMsg(err),
        );
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthScaffold title="登录 HDSL-web">
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
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
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
          {busy ? "登录中…" : "登录"}
        </button>
      </form>
    </AuthScaffold>
  );
}
