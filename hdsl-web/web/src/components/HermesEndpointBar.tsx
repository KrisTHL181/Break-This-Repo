import { useState } from "react";
import { api } from "../api";
import { toast } from "../store";
import { errMsg } from "../utils";

/// Hermes 实例的「API 端点」条：它的 gateway 是一个 OpenAI 兼容服务，
/// 拉进面板的账户页就能当供应商用（T3）。
///
/// 端点与 key 只在点开时才取：`GET /api/brands/hermes/instances/<id>/endpoint`
/// 在面板的会话门之内，key 不落在页面源码里，也不随实例列表轮询反复传输。
/// key 默认打码——它和页面上的其他东西不同，是一份能花钱的凭据。
export function HermesEndpointBar({ instanceId, running }: { instanceId: string; running: boolean }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [endpoint, setEndpoint] = useState<{ baseUrl: string; apiPort: number; key: string; running: boolean } | null>(
    null,
  );
  const [showKey, setShowKey] = useState(false);

  const load = async () => {
    setBusy(true);
    setError(null);
    try {
      setEndpoint(await api.brandEndpoint("hermes", instanceId));
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next && endpoint === null) void load();
  };

  const copy = async (text: string, what: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast("success", `${what}已复制`);
    } catch (e) {
      toast("error", `复制失败：${errMsg(e)}`);
    }
  };

  const addToAccounts = async () => {
    if (!endpoint) return;
    setBusy(true);
    try {
      const answer = await api.createAccount({
        endpoint: endpoint.baseUrl,
        apiKey: endpoint.key,
        protocol: "openai-completions",
        label: `hermes-${instanceId}`,
      });
      if (answer.verified === false) {
        toast("info", `已添加，但面板还没验通：${answer.verifyError ?? "接口没有回答"}`);
      } else {
        toast("success", `已添加到账户：hermes-${instanceId}`);
      }
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const masked = endpoint === null ? "" : showKey ? endpoint.key : `${endpoint.key.slice(0, 6)}…${endpoint.key.slice(-4)}`;

  return (
    <span className="tlli-subtitle" style={{ display: "block" }}>
      <button className="btn btn-text" style={{ padding: "0 4px", minHeight: 0 }} onClick={toggle}>
        {open ? "收起 API 端点" : "API 端点（OpenAI 兼容）"}
      </button>
      {!running && <span className="dim"> · 实例没在跑，端点要等它起来才能用</span>}
      {open && (
        <span
          style={{
            display: "flex",
            flexDirection: "column",
            gap: 4,
            padding: "4px 4px 2px",
            fontSize: 12,
            lineHeight: 1.7,
          }}
        >
          {error && <span style={{ color: "var(--monet-error)" }}>{error}</span>}
          {busy && endpoint === null && !error && <span className="dim">正在读取…</span>}
          {endpoint && (
            <>
              <span>
                地址：<code>{endpoint.baseUrl}</code>{" "}
                <button className="btn btn-text" style={{ padding: "0 4px", minHeight: 0 }} onClick={() => void copy(endpoint.baseUrl, "地址")}>
                  复制
                </button>
              </span>
              <span>
                密钥：<code>{masked}</code>{" "}
                <button
                  className="btn btn-text"
                  style={{ padding: "0 4px", minHeight: 0 }}
                  onClick={() => setShowKey((value) => !value)}
                >
                  {showKey ? "隐藏" : "显示"}
                </button>{" "}
                <button className="btn btn-text" style={{ padding: "0 4px", minHeight: 0 }} onClick={() => void copy(endpoint.key, "密钥")}>
                  复制
                </button>
              </span>
              <span style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                <button className="btn btn-raised" disabled={busy} onClick={() => void addToAccounts()}>
                  添加到账户
                </button>
                <span className="dim">
                  key 就在实例目录的 <code>.env</code> 里；面板不会替你轮换它。
                </span>
              </span>
            </>
          )}
        </span>
      )}
    </span>
  );
}
