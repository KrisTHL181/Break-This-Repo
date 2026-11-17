import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api";
import { I18N } from "../i18n";
import { HOME_MODES } from "../constants";
import { loadVersions, toast, useAppState } from "../store";
import type { VersionInfo } from "../types";
import { channelLabel, errMsg, formatDateTime } from "../utils";
import { CloseIcon, ErrorIcon, RefreshIcon } from "../components/icons";
import { VersionListIcon } from "../components/VersionListIcon";

/**
 * 安装新实例向导（DshInstallWizardProvider，推入导航栈）：
 * 步骤一"选择版本"（过滤 + 版本行 radio 选择），步骤二"快速安装"（名称/隔离模式/端口）。
 * 底部右侧 取消 / 上一步 / 下一步（文字按钮）。
 */
export function InstallInstanceWizard() {
  const nav = useNavigate();
  const s = useAppState();
  const [searchParams, setSearchParams] = useSearchParams();

  const stepParam = searchParams.get("step");
  const step: 1 | 2 = stepParam === "2" ? 2 : 1;
  const setStep = (s: 1 | 2) =>
    setSearchParams(s === 1 ? {} : { step: "2" }, { replace: true });
  const [channel, setChannel] = useState("");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<VersionInfo | null>(null);
  const [name, setName] = useState("");
  const [homeMode, setHomeMode] = useState("ISOLATED");
  const [portMode, setPortMode] = useState<"auto" | "fixed">("auto");
  const [port, setPort] = useState("");
  const [portError, setPortError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void loadVersions();
  }, []);

  // 深链 ?version=x 预选
  useEffect(() => {
    const v = searchParams.get("version");
    if (!v || selected) return;
    const found = (s.versions ?? []).find((x) => x.version === v);
    if (found) {
      setSelected(found);
      setName((n) => n || `dsh-${found.version}`);
      setSearchParams({ step: "2" }, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [s.versions]);

  const channels = useMemo(() => {
    const set = new Set<string>();
    for (const v of s.versions ?? []) set.add(v.channel);
    return Array.from(set);
  }, [s.versions]);

  const filtered = useMemo(() => {
    let list = s.versions ?? [];
    if (channel) list = list.filter((v) => v.channel === channel);
    const q = query.trim().toLowerCase();
    if (q) list = list.filter((v) => v.version.toLowerCase().includes(q));
    return list;
  }, [s.versions, channel, query]);

  const pick = (v: VersionInfo) => {
    if (!v.installable) return;
    setSelected(v);
    if (!name) setName(`dsh-${v.version}`);
  };

  const next = () => {
    if (!selected) {
      toast("error", "请选择版本");
      return;
    }
    setStep(2);
  };

  const submit = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      toast("error", I18N["dsh.instance.name.empty"]);
      return;
    }
    let portNum: number | undefined;
    if (portMode === "fixed") {
      const n = Number(port);
      if (!Number.isInteger(n) || n < 1024 || n > 65535) {
        setPortError(I18N["dsh.instance.port.invalid"]);
        return;
      }
      portNum = n;
    }
    if (!selected) return;
    setBusy(true);
    try {
      const r = await api.createInstance({
        name: trimmed,
        version: selected.version,
        homeMode,
        portMode,
        ...(portNum !== undefined ? { port: portNum } : {}),
      });
      toast(
        "success",
        r.installTaskId
          ? I18N["dsh.instance.created.installing"].replace("%s", trimmed)
          : I18N["dsh.instance.created"].replace("%s", trimmed),
      );
      nav(`/instances/${r.instance.id}`, { replace: true });
    } catch (e) {
      toast("error", `创建失败：${errMsg(e)}`);
      setBusy(false);
    }
  };

  return (
    <div className="page-content scroll" style={{ gap: 8 }}>
      {step === 1 ? (
        <>
          <div className="card filter-bar">
            <span className="filter-label">名称</span>
            <input
              className="input"
              placeholder={I18N["search.hint.chinese"]}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <span className="filter-label">类型</span>
            <select className="input" value={channel} onChange={(e) => setChannel(e.target.value)}>
              <option value="">全部</option>
              {channels.map((c) => (
                <option key={c} value={c}>
                  {channelLabel(c)}
                </option>
              ))}
            </select>
            <button
              className="btn btn-raised ripple-host"
              disabled={s.versionsLoading}
              onClick={() => void loadVersions(true)}
            >
              <RefreshIcon size={15} />
              {I18N["button.refresh"]}
            </button>
          </div>

          <div className="card" style={{ flex: 1, minHeight: 260, display: "flex", flexDirection: "column", padding: 0 }}>
            <div className="list-body scroll-hover">
              {s.versionsLoading && !s.versions ? (
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10, padding: 48 }}>
                  <span className="spinner" />
                  <span>{I18N["dsh.versions.loading"]}</span>
                </div>
              ) : s.versionsError && !s.versions ? (
                <div className="empty-state">
                  <ErrorIcon size={30} />
                  <span>{I18N["dsh.versions.load_failed"]}</span>
                  <button className="btn btn-outline ripple-host" onClick={() => void loadVersions(true)}>
                    {I18N["button.refresh"]}
                  </button>
                </div>
              ) : filtered.length === 0 ? (
                <div className="empty-state">
                  <span style={{ fontSize: 20 }}>{I18N["dsh.versions.available.empty"]}</span>
                </div>
              ) : (
                filtered.map((v, idx) => (
                  <div key={v.version}>
                    <div
                      className="tlli ripple-host"
                      style={{
                        cursor: v.installable ? "pointer" : "default",
                        padding: "8px 12px",
                        background:
                          selected?.version === v.version
                            ? "var(--monet-secondary-container-transparent-50)"
                            : undefined,
                      }}
                      onClick={() => pick(v)}
                    >
                      <label className="input-check">
                        <input
                          type="radio"
                          name="wizard-version"
                          checked={selected?.version === v.version}
                          disabled={!v.installable}
                          onChange={() => pick(v)}
                        />
                      </label>
                      <span className="tlli-graphic">
                        <VersionListIcon size={32} />
                      </span>
                      <span className="tlli-text">
                        <span className="tlli-title">
                          {v.version}
                          <span className="tag">{channelLabel(v.channel)}</span>
                          {!v.installable && <span className="tag error">不可安装</span>}
                        </span>
                        <span className="tlli-subtitle">{formatDateTime(v.time)}</span>
                      </span>
                    </div>
                    {idx < filtered.length - 1 && <div className="divider" style={{ marginLeft: 12 }} />}
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      ) : (
        <div className="card" style={{ maxWidth: 520, width: "100%", margin: "0 auto", display: "flex", flexDirection: "column", gap: 4 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
            <span className="tag">{selected?.version}</span>
            <button className="btn btn-text" style={{ height: 26 }} onClick={() => setStep(1)}>
              更换版本
            </button>
          </div>
          <div className="form-row">
            <span className="form-label">{I18N["dsh.instance.name"]}</span>
            <div style={{ position: "relative", display: "flex", alignItems: "center" }}>
              <input
                className="input"
                style={{ paddingRight: 34 }}
                value={name}
                maxLength={64}
                aria-invalid={!name.trim()}
                onChange={(e) => setName(e.target.value)}
              />
              {name && (
                <button
                  type="button"
                  className="icon-btn on-variant ripple-host"
                  style={{ position: "absolute", right: 4 }}
                  title="清除"
                  onClick={() => setName("")}
                >
                  <CloseIcon size={16} />
                </button>
              )}
            </div>
            {/* 名字「不合法」在这里不是一种失败：实例目录用的是后端从名字派生的 id——`uniqueId`
                会净化非法字符、并在重名时自动加后缀——所以桌面版那两个对话框（非法字符、重名）在
                这里没有对应的失败模式。要当场说出来的只有「什么都没填」。 */}
            {!name.trim() && <div className="form-error">{I18N["dsh.instance.name.empty"]}</div>}
          </div>
          <div className="form-row">
            <span className="form-label">{I18N["dsh.install.home"]}</span>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {HOME_MODES.map((m) => (
                <label key={m.value} className="input-check" style={{ padding: "4px 0" }}>
                  <input
                    type="radio"
                    name="wizard-home"
                    checked={homeMode === m.value}
                    onChange={() => setHomeMode(m.value)}
                  />
                  <span>
                    {m.title}
                    <span style={{ color: "var(--monet-on-surface-variant)", fontSize: 12 }}>
                      {" "}
                      — {m.desc}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </div>
          <div className="form-row">
            <span className="form-label">{I18N["dsh.instance.port.mode"]}</span>
            <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
              <label className="input-check">
                <input type="radio" checked={portMode === "auto"} onChange={() => setPortMode("auto")} />
                <span>{I18N["dsh.instance.port.mode.auto"]}</span>
              </label>
              <label className="input-check">
                <input type="radio" checked={portMode === "fixed"} onChange={() => setPortMode("fixed")} />
                <span>{I18N["dsh.instance.port.mode.fixed"]}</span>
              </label>
              <input
                className="input"
                style={{ width: 130 }}
                type="number"
                min={1024}
                max={65535}
                placeholder="1024–65535"
                disabled={portMode !== "fixed"}
                value={port}
                onChange={(e) => {
                  setPort(e.target.value);
                  setPortError(null);
                }}
              />
            </div>
            {portError && <div className="form-error">{portError}</div>}
          </div>
        </div>
      )}

      {/* 向导底部：右侧 取消 / 上一步 / 下一步 */}
      <div style={{ display: "flex", justifyContent: "flex-end", gap: 4, padding: "2px 0" }}>
        <button className="btn btn-text dim ripple-host" onClick={() => nav(-1)}>
          {I18N["button.cancel"]}
        </button>
        {step === 2 && (
          <button className="btn btn-text dim ripple-host" onClick={() => setStep(1)}>
            {I18N["button.previous"]}
          </button>
        )}
        <button
          className="btn btn-raised ripple-host"
          disabled={(!selected && step === 1) || busy}
          onClick={() => (step === 1 ? next() : void submit())}
        >
          {step === 1 ? I18N["button.next"] : busy ? I18N["dsh.install.working"] : I18N["dsh.install.start"]}
        </button>
      </div>
    </div>
  );
}
