import { useEffect, useState } from "react";
import { api, type McJavaRuntimesSnapshot } from "../api";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { refreshMcFeature, useMcFeature } from "../mcFeature";
import { toast } from "../store";
import { errMsg } from "../utils";
import { DeleteIcon, RefreshIcon, WarningIcon } from "./icons";
import { McJavaInstallWizard } from "./McJavaInstallWizard";

/** 设置页里的 Java 运行时（MC 专用），放在 Node.js 托管下面。
 *
 *  只做一件事：让面板自己拥有一份 JDK，不再依赖宿主机恰好装了哪个版本。
 *  **面板自身 JVM 与 PATH 上的 java 只读展示**——这里没有、也不该有能把它们
 *  换掉或删掉的按钮；实例的「高级设置」里选的 javaPath 也一样，只在那边改。
 *
 *  安装走 `McJavaInstallWizard`：先选主版本（由 MC 需求决定），再选发行版
 *  （由许可、平台与更新节奏决定）。Oracle JDK 不在下载源里。 */

export function McJavaRuntimeCard() {
  const feature = useMcFeature();
  const enabled = feature?.enabled === true;
  const [snapshot, setSnapshot] = useState<McJavaRuntimesSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [wizardOpen, setWizardOpen] = useState(false);
  // The settings page never loads the MC snapshot on its own, and this card
  // used to refresh only when already enabled — so it stayed hidden forever on
  // a fresh /settings load. Ask once on mount, then follow the answer.
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    void refreshMcFeature().finally(() => setChecked(true));
  }, []);

  const load = () => {
    if (!enabled) return;
    setLoading(true);
    void api.mc
      .javaRuntimes()
      .then((answer) => {
        setSnapshot(answer);
        setError("");
      })
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!enabled) {
      setSnapshot(null);
      return;
    }
    load();
  }, [enabled]);

  const remove = async (featureRelease: number) => {
    if (!window.confirm(`删除面板托管的 JDK ${featureRelease}？磁盘上的目录会被整个删掉。`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const call = () => api.mc.removeJava(featureRelease);
      try {
        await call();
      } catch (e) {
        if (!isElevationRequired(e) || !(await requestDeveloperPassword())) throw e;
        await call();
      }
      toast("success", `已删除 JDK ${featureRelease}`);
      load();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  if (!enabled) {
    // Unknown yet: saying "not enabled" before the answer arrives would flash
    // on every visit. Once we know, say why the section is empty instead of
    // making the card look broken.
    if (!checked) {
      return null;
    }
    return (
      <>
        <div className="card" style={{ padding: 12, fontSize: 13, lineHeight: 1.7 }}>
          MC 功能未开启，Java 托管不可用。开关在「设置 → 品牌实例 → MC 服务端」；
          开启后这里会出现已安装的 JDK 与「安装 JDK…」。
        </div>
      </>
    );
  }

  const installed = snapshot?.installed ?? [];
  const consumers = snapshot?.consumers ?? [];

  return (
    <>
      <div className="card" style={{ display: "grid", gap: 8, padding: 12 }}>
        <div className="toolbar-row" style={{ padding: "4px 0", flexWrap: "wrap" }}>
          <span style={{ fontSize: 13, fontWeight: 600 }}>面板托管的 JDK</span>
          <span className="tag plain">{installed.length} 个</span>
          <span className="spacer" />
          <button className="btn btn-text ripple-host" disabled={loading || busy} onClick={load}>
            <RefreshIcon size={16} /> 刷新
          </button>
          <button
            className="btn btn-raised ripple-host"
            disabled={loading || busy}
            onClick={() => setWizardOpen(true)}
          >
            安装 JDK…
          </button>
        </div>

        {installed.length === 0 && (
          <span style={{ fontSize: 13, color: "var(--monet-on-surface-variant)" }}>
            还没有托管 JDK。装一个之后，MC 实例在宿主机缺对应版本时会自动用它。
          </span>
        )}

        {installed.map((runtime) => {
          const pinned = consumers.filter((c) => c.feature === runtime.feature);
          const blocked = pinned.length > 0;
          return (
            <div key={runtime.feature} className="toolbar-row" style={{ padding: "4px 0", flexWrap: "wrap", gap: 6 }}>
              <span style={{ fontSize: 13, fontWeight: 600 }}>JDK {runtime.feature}</span>
              <span className="tag plain">{runtime.vendorLabel ?? "未知发行版"}</span>
              <span className="tag plain">{runtime.version ?? "版本未知"}</span>
              {runtime.bytes ? (
                <span className="tag plain">{Math.round(runtime.bytes / 1024 / 1024)} MB</span>
              ) : null}
              <code style={{ fontSize: 11, color: "var(--monet-on-surface-variant)", wordBreak: "break-all" }}>
                {runtime.path}
              </code>
              {blocked && (
                <span className="tag plain" title={pinned.map((c) => c.id).join("、")}>
                  被 {pinned.length} 个实例固定
                </span>
              )}
              <span className="spacer" />
              <button
                className="btn btn-text ripple-host"
                style={{ height: 28 }}
                disabled={busy || blocked}
                title={blocked ? "先到实例的高级设置里改掉 javaPath" : ""}
                onClick={() => void remove(runtime.feature)}
              >
                <DeleteIcon size={16} /> 删除
              </button>
            </div>
          );
        })}

        {snapshot?.source === "unavailable" && (
          <div className="hint warning" style={{ margin: 0 }}>
            <span style={{ flex: 1 }}>
              <WarningIcon size={14} style={{ verticalAlign: "-2px", marginRight: 4 }} />
              下载目录读取失败：{snapshot.error ?? "网络或镜像不可达"}。已装的 JDK 仍可正常使用。
            </span>
          </div>
        )}
        {snapshot?.vendors?.some((v) => !v.ok) && snapshot?.source === "api" && (
          <div className="hint warning" style={{ margin: 0 }}>
            <span style={{ flex: 1 }}>
              {snapshot.vendors
                .filter((v) => !v.ok)
                .map((v) => `${v.label}：${v.error ?? "目录不可用"}`)
                .join("；")}
            </span>
          </div>
        )}
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
          托管源提供 <b>Eclipse Temurin</b>、<b>Microsoft Build of OpenJDK</b> 和
          <b> OpenJDK 官方参考构建</b>（都是 GPLv2+CE，允许再分发）。Oracle JDK 的许可不允许
          面板代下载，因此不在列表里；需要时请自行安装后用实例的高级设置指向它。
        </span>
      </div>

      {/* 只读区：这两个 Java 是面板/容器自己的，任何按钮都不该碰它们。 */}
      <div className="card" style={{ display: "grid", gap: 6, padding: 12 }}>
        <div className="toolbar-row" style={{ padding: "2px 0" }}>
          <span style={{ fontSize: 13, fontWeight: 600 }}>只读：面板与容器自己的 Java</span>
          <span className="tag plain">不会被安装、删除或切换</span>
        </div>
        <div className="comp-row" style={{ padding: "3px 0" }}>
          <span className="comp-label" style={{ width: "auto", flex: "none" }}>面板自身 JVM</span>
          <span className="comp-value" style={{ fontSize: 12 }}>
            {snapshot ? `${snapshot.panel.vendor} ${snapshot.panel.version}` : "读取中…"}
          </span>
        </div>
        <div className="comp-row" style={{ padding: "3px 0" }}>
          <span className="comp-label" style={{ width: "auto", flex: "none" }}>系统 PATH</span>
          <span className="comp-value" style={{ fontSize: 12 }}>
            {snapshot == null
              ? "读取中…"
              : snapshot.system.ok
                ? `java ${snapshot.system.version}`
                : snapshot.system.error ?? "未检测到 java"}
          </span>
        </div>
        <div className="comp-row" style={{ padding: "3px 0" }}>
          <span className="comp-label" style={{ width: "auto", flex: "none" }}>MC 当前生效</span>
          <span className="comp-value" style={{ fontSize: 12 }}>
            {feature?.java
              ? `${feature.java.path || "(空)"}${feature.java.version ? ` · ${feature.java.version}` : ""}`
              : "—"}
          </span>
        </div>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
          这三个值只用于对照。要换 MC 用的 Java，去实例的「高级设置」里选；
          面板自身的 JVM 由启动脚本决定，面板不会改。
        </span>
      </div>

      {error && (
        <div className="hint warning" style={{ margin: 0 }}>
          <span style={{ flex: 1 }}>{error}</span>
        </div>
      )}

      <McJavaInstallWizard
        open={wizardOpen}
        onClose={() => setWizardOpen(false)}
        onInstalled={load}
      />
    </>
  );
}
