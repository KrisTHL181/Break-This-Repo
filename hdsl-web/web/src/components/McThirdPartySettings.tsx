import { useEffect, useMemo, useState } from "react";
import { api, type McSourcesSnapshot, type McSourceInfo } from "../api";
import { Dialog } from "./Dialog";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { toast } from "../store";
import { errMsg, mcCoreLabel } from "../utils";
import { RefreshIcon } from "./icons";

/** 第三方核心的全局设置：下载源顺序、自定义镜像、配额余量。
 *
 *  这一页只服务于「MC 第三方服务器实例」那一栏。原版永远走 Mojang，没有源可选，
 *  所以源设置放在这里而不是全局设置里——换源是第三方链路自己的事。
 *
 *  顺序的语义：第一个能应答的源获胜，`official` 永远保留在列表里（后端强制），
 *  镜像用满配额会被自动跳过并显示重置时间。 */

const CORES = ["paper", "folia", "purpur", "fabric", "forge", "neoforge"];
const BASE_KINDS: { kind: string; label: string; placeholder: string }[] = [
  { kind: "paper", label: "Paper / Folia", placeholder: "https://fill.papermc.io" },
  { kind: "purpur", label: "Purpur", placeholder: "https://api.purpurmc.org" },
  { kind: "fabric", label: "Fabric", placeholder: "https://meta.fabricmc.net" },
  { kind: "forge", label: "Forge", placeholder: "https://maven.minecraftforge.net" },
  { kind: "neoforge", label: "NeoForge", placeholder: "https://maven.neoforged.net" },
  { kind: "mslmc", label: "MSL API", placeholder: "https://api.mslmc.cn/v4" },
];

export function McThirdPartySettings({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const [snapshot, setSnapshot] = useState<McSourcesSnapshot | null>(null);
  const [order, setOrder] = useState<Record<string, string[]>>({});
  const [custom, setCustom] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = (silent = false) => {
    if (!silent) setLoading(true);
    void api.mc
      .sources()
      .then((answer) => {
        setSnapshot(answer);
        setOrder(cloneOrder(answer.order));
        setCustom({ ...answer.custom });
        setError("");
      })
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (open) load();
  }, [open]);

  const sourceById = useMemo(() => {
    const map = new Map<string, McSourceInfo>();
    for (const source of snapshot?.sources ?? []) map.set(source.id, source);
    return map;
  }, [snapshot]);

  const needsAttribution = (snapshot?.sources ?? []).some(
    (source) => source.attribution && CORES.some((core) => (order[core] ?? []).includes(source.id)),
  );

  const move = (core: string, index: number, delta: number) => {
    setOrder((current) => {
      const list = [...(current[core] ?? [])];
      const target = index + delta;
      if (target < 0 || target >= list.length) return current;
      [list[index], list[target]] = [list[target], list[index]];
      return { ...current, [core]: list };
    });
  };

  const toggle = (core: string, sourceId: string) => {
    setOrder((current) => {
      const list = [...(current[core] ?? [])];
      const at = list.indexOf(sourceId);
      if (at >= 0) {
        list.splice(at, 1);
      } else {
        list.push(sourceId);
      }
      // `official` 是后端的底线：前端也把它补回去，免得多走一次往返。
      if (!list.includes("official")) list.push("official");
      return { ...current, [core]: list };
    });
  };

  const save = async () => {
    setSaving(true);
    setError("");
    const push = async () =>
      api.mc.updateSources({ order, custom });
    try {
      let answer: McSourcesSnapshot;
      try {
        answer = await push();
      } catch (e) {
        if (!isElevationRequired(e)) throw e;
        if (!(await requestDeveloperPassword())) throw e;
        answer = await push();
      }
      setSnapshot(answer);
      setOrder(cloneOrder(answer.order));
      setCustom({ ...answer.custom });
      toast("success", "下载源设置已保存");
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} wide title="第三方核心 · 全局设置" onClose={onClose} closeOnBackdrop={!saving}>
      <div style={{ display: "flex", flexDirection: "column", gap: 14, minWidth: 520 }}>
        <div className="dim" style={{ fontSize: 12, lineHeight: 1.7 }}>
          下载源决定第三方核心从哪里拉取。顺序有意义：<b>第一个能应答的源获胜</b>；
          官方源始终保留在列表里，镜像用满配额会被跳过而不是硬撞。解析出来的下载地址会记在
          jar 旁边，同一个「核心 / 版本」只问一次。
        </div>

        {loading && <div className="hint info"><span style={{ flex: 1 }}>正在读取源设置…</span></div>}
        {error && <div className="hint warning"><span style={{ flex: 1 }}>{error}</span></div>}

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontWeight: 600, fontSize: 13 }}>源</div>
          {(snapshot?.sources ?? []).map((source) => {
            const quota = source.quota;
            const quotaText =
              quota.limit > 0
                ? `${quota.used}/${quota.limit} 次${quota.exhausted ? " · 已用满" : ""}`
                : "未计量";
            const reset = quota.resetAt > 0 && quota.exhausted
              ? new Date(quota.resetAt * 1000).toLocaleString()
              : null;
            return (
              <div
                key={source.id}
                className="card"
                style={{ padding: 10, display: "grid", gap: 4 }}
              >
                <span style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                  <b style={{ fontSize: 13 }}>{source.label}</b>
                  <code style={{ fontSize: 11 }}>{source.id}</code>
                  <span className={`tag ${quota.exhausted ? "failed" : "plain"}`}>{quotaText}</span>
                  {reset && <span className="dim" style={{ fontSize: 11 }}>重置于 {reset}</span>}
                </span>
                <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
                  {source.note}
                </span>
                <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
                  支持：{source.cores.map((core) => mcCoreLabel(core)).join(" / ")}
                </span>
              </div>
            );
          })}
        </div>

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontWeight: 600, fontSize: 13 }}>每个核心的尝试顺序</div>
          {CORES.map((core) => {
            const list = order[core] ?? [];
            const applicable = (snapshot?.sources ?? []).filter((source) => source.cores.includes(core));
            return (
              <div key={core} className="card" style={{ padding: 10, display: "grid", gap: 6 }}>
                <span style={{ fontSize: 13, fontWeight: 600 }}>{mcCoreLabel(core)}</span>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center" }}>
                  {list.map((id, index) => {
                    const source = sourceById.get(id);
                    return (
                      <span
                        key={id}
                        className="tag"
                        style={{ display: "inline-flex", gap: 4, alignItems: "center", padding: "2px 6px" }}
                      >
                        {index + 1}. {source?.label ?? id}
                        <button
                          className="btn btn-text"
                          style={{ padding: "0 4px", minWidth: 0 }}
                          disabled={index === 0}
                          title="上移"
                          onClick={() => move(core, index, -1)}
                        >
                          ↑
                        </button>
                        <button
                          className="btn btn-text"
                          style={{ padding: "0 4px", minWidth: 0 }}
                          disabled={index === list.length - 1}
                          title="下移"
                          onClick={() => move(core, index, 1)}
                        >
                          ↓
                        </button>
                        {id !== "official" && (
                          <button
                            className="btn btn-text"
                            style={{ padding: "0 4px", minWidth: 0 }}
                            title="移除"
                            onClick={() => toggle(core, id)}
                          >
                            ✕
                          </button>
                        )}
                      </span>
                    );
                  })}
                  {applicable
                    .filter((source) => !list.includes(source.id))
                    .map((source) => (
                      <button
                        key={source.id}
                        className="btn btn-text"
                        style={{ fontSize: 11 }}
                        onClick={() => toggle(core, source.id)}
                      >
                        + {source.label}
                      </button>
                    ))}
                </div>
              </div>
            );
          })}
        </div>

        <details>
          <summary style={{ cursor: "pointer", fontSize: 13, fontWeight: 600 }}>
            高级：自定义镜像地址
          </summary>
          <div style={{ display: "grid", gap: 8, marginTop: 8 }}>
            <div className="dim" style={{ fontSize: 12 }}>
              留空 = 用该源的默认地址。填了以后，官方源会改走这个地址；请确认它和上游
              API 的形状一致。
            </div>
            {BASE_KINDS.map((entry) => (
              <label key={entry.kind} style={{ display: "grid", gap: 4 }}>
                <span style={{ fontSize: 12 }}>{entry.label}</span>
                <input
                  className="input"
                  placeholder={entry.placeholder}
                  value={custom[entry.kind] ?? ""}
                  onChange={(e) => setCustom({ ...custom, [entry.kind]: e.target.value })}
                />
              </label>
            ))}
          </div>
        </details>

        {needsAttribution && (
          <div className="hint info" style={{ margin: 0 }}>
            <span style={{ flex: 1, lineHeight: 1.7 }}>
              正在使用 MSL 镜像。按其条款，使用该服务需要在软件中注明
              <b>「本服务由 MSL 开服器提供」</b>并附官网链接（www.mslmc.cn）。
              面板会在启用该源时显示这一行。
            </span>
          </div>
        )}

        <div className="dim" style={{ fontSize: 11 }}>
          请求 UA：<code>{snapshot?.userAgent ?? "HDSL-web (self-hosted panel)"}</code>
        </div>

        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button className="btn btn-text" onClick={() => load()} disabled={loading || saving}>
            <RefreshIcon size={16} /> 重新读取
          </button>
          <button className="btn btn-raised" onClick={() => void save()} disabled={saving || loading}>
            {saving ? "保存中…" : "保存"}
          </button>
        </div>
      </div>
    </Dialog>
  );
}

function cloneOrder(order: Record<string, string[]>): Record<string, string[]> {
  const copy: Record<string, string[]> = {};
  for (const [core, ids] of Object.entries(order)) copy[core] = [...ids];
  return copy;
}
