import { useEffect, useState } from "react";
import { api } from "../api";
import { requestDeveloperPassword } from "../elevation";
import { toast } from "../store";
import { errMsg } from "../utils";
import { ChatIcon } from "./icons";

/// 通用 → 纯文本版：legacy 表面（`/plain`）是「真能用」还是「单纯降级页」。
///
/// 打开时它有 fragment 长轮询自动更新、ES5 轮询器与服务端预渲染的 markdown；
/// 关掉就回到表单 + 整页刷新的保底页，markdown 按转义 `<pre>` 显示。开关存进
/// `server.yaml` 的 `plain.enhanced`，下一次 `/plain` 请求立即生效，不用重启。
/// 写它要开发者提权，和日志保留窗口同一条规矩。
export function PlainSurfaceCard() {
  const [enhanced, setEnhanced] = useState<boolean | null>(null);
  const [ssrAvailable, setSsrAvailable] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.plain
      .settings()
      .then((value) => {
        setEnhanced(value.enhanced);
        setSsrAvailable(value.ssrAvailable);
      })
      .catch(() => setEnhanced(null));
  }, []);

  const toggle = async () => {
    if (enhanced === null || busy) return;
    const next = !enhanced;
    setBusy(true);
    try {
      try {
        const saved = await api.plain.setEnhanced(next);
        setEnhanced(saved.enhanced);
        setSsrAvailable(saved.ssrAvailable);
      } catch (e) {
        if (!(e instanceof Error) || !e.message.includes("developer_elevation_required")) throw e;
        if (!(await requestDeveloperPassword())) return;
        const saved = await api.plain.setEnhanced(next);
        setEnhanced(saved.enhanced);
        setSsrAvailable(saved.ssrAvailable);
      }
      toast("success", next ? "纯文本版增强已打开" : "纯文本版已回到降级页");
    } catch (e) {
      toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="section-caption">
        <ChatIcon size={16} />
        纯文本版（legacy 表面）
      </div>
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">增强模式</span>
          <span className="comp-value">
            <span className={`tag ${enhanced === null ? "plain" : enhanced ? "running" : "plain"}`}>
              {enhanced === null ? "—" : enhanced ? "已启用" : "已关闭"}
            </span>
            <button
              className={`switch ${enhanced ? "on" : ""}`}
              role="switch"
              aria-checked={enhanced === true}
              aria-label="纯文本版增强模式"
              style={{ border: 0, padding: 0, marginLeft: "auto" }}
              disabled={busy || enhanced === null}
              onClick={() => void toggle()}
            />
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">Markdown 预渲染</span>
          <span className="comp-value">
            <span className={`tag ${ssrAvailable === null ? "plain" : ssrAvailable ? "running" : "plain"}`}>
              {ssrAvailable === null ? "—" : ssrAvailable ? "可用" : "不可用"}
            </span>
          </span>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          打开时 <span className="mono">/plain</span> 自动更新内容（ES5 长轮询换 fragment）、
          markdown 由面板自己的前端渲染器服务端预渲染，IE11 与文本浏览器都「真能用」；
          关闭则退回单纯降级页：表单加整页刷新、正文按原样显示。两种模式下发送都要开发者提权。
          预渲染找不到 Node 时正文会退回转义 <span className="mono">&lt;pre&gt;</span>；面板
          会优先使用自己托管的 Node，其次才是 PATH。
        </p>
      </div>
    </>
  );
}
