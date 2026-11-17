import { useEffect, useState } from "react";
import { api } from "../api";
import type { SystemStorage } from "../types";
import { errMsg } from "../utils";
import { RefreshIcon, StorageIcon } from "./icons";

function fmtBytes(bytes: number): string {
  if (bytes < 0) return "—";
  const units = ["B", "KiB", "MiB", "GiB", "TiB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  const digits = value >= 100 ? 0 : value >= 10 ? 1 : 2;
  return `${value.toFixed(digits)} ${units[unit]}`;
}

/// 通用 → 存储与日志：数据目录按桶拆开，外加所在分区的剩余空间。
///
/// 遍历几万个文件是秒级的，所以后端缓存 60 秒；「重新统计」才强制再走一遍。
export function StorageCard() {
  const [report, setReport] = useState<SystemStorage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = async (refresh: boolean) => {
    setBusy(true);
    try {
      setReport(await api.systemStorage(refresh));
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void load(false);
  }, []);

  const mount = report?.mount;
  const mountRatio = mount && mount.totalBytes > 0 ? mount.usedBytes / mount.totalBytes : 0;

  return (
    <>
      <div className="section-caption">
        <StorageIcon size={16} />
        存储
      </div>
      <div className="card">
        {error && (
          <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "10px 8px" }}>
            <span style={{ color: "var(--monet-error)" }}>读取失败：{error}</span>
            <button className="btn btn-text" style={{ height: 26, marginLeft: "auto" }} onClick={() => void load(true)}>
              重试
            </button>
          </div>
        )}
        {!report ? (
          <div style={{ display: "flex", alignItems: "center", gap: 10, padding: 12 }}>
            <span className="spinner" />
            <span>正在统计数据目录…</span>
          </div>
        ) : (
          <>
            <div className="comp-row">
              <span className="comp-label">数据目录</span>
              <span className="comp-value mono" style={{ fontSize: 12, wordBreak: "break-all" }}>
                {report.dataDir}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">合计</span>
              <span className="comp-value mono">{fmtBytes(report.totalBytes)}</span>
            </div>
            {mount && mount.totalBytes > 0 && (
              <div style={{ padding: "2px 8px 8px" }}>
                <div className="comp-row" style={{ borderBottom: "none", padding: 0 }}>
                  <span className="comp-label">所在分区</span>
                  <span className="comp-value mono" style={{ fontSize: 12 }}>
                    {fmtBytes(mount.usedBytes)} / {fmtBytes(mount.totalBytes)}（
                    {(mountRatio * 100).toFixed(0)}% 已用）
                  </span>
                </div>
                <div className="progress" style={{ marginTop: 4 }}>
                  <div
                    className="bar"
                    style={{ width: `${Math.max(0, Math.min(100, mountRatio * 100))}%` }}
                  />
                </div>
              </div>
            )}

            {report.buckets.length === 0 ? (
              <div className="comp-row">
                <span className="comp-label">分项</span>
                <span className="comp-value" style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                  数据目录还是空的
                </span>
              </div>
            ) : (
              report.buckets.map((bucket) => {
                const ratio = report.totalBytes > 0 ? bucket.bytes / report.totalBytes : 0;
                return (
                  <div key={bucket.name} style={{ padding: "2px 8px 8px" }}>
                    <div className="comp-row" style={{ borderBottom: "none", padding: 0 }}>
                      <span className="comp-label">
                        {bucket.label}
                        <span
                          className="mono"
                          style={{ marginLeft: 6, fontSize: 11, color: "var(--monet-on-surface-variant)" }}
                        >
                          {bucket.name}
                        </span>
                      </span>
                      <span className="comp-value mono" style={{ fontSize: 12 }}>
                        {fmtBytes(bucket.bytes)}（{(ratio * 100).toFixed(0)}%）
                      </span>
                    </div>
                    <div className="progress" style={{ marginTop: 4 }}>
                      <div
                        className="bar"
                        style={{ width: `${Math.max(0, Math.min(100, ratio * 100))}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "6px 8px 10px",
                fontSize: 12,
                color: "var(--monet-on-surface-variant)",
              }}
            >
              <span>统计于 {new Date(report.computedAt).toLocaleString()}</span>
              <button
                className="btn btn-text ripple-host"
                style={{ height: 26, marginLeft: "auto" }}
                disabled={busy}
                onClick={() => void load(true)}
              >
                <RefreshIcon size={14} />
                {busy ? "重新统计中…" : "重新统计"}
              </button>
            </div>
          </>
        )}
      </div>
    </>
  );
}
