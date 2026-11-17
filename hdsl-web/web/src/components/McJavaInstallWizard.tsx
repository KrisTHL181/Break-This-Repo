import { useEffect, useMemo, useState } from "react";
import { api, type McJavaRuntimesSnapshot } from "../api";
import { Dialog } from "./Dialog";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { toast } from "../store";
import { errMsg } from "../utils";
import { CheckCircleIcon, RefreshIcon, WarningIcon } from "./icons";

/** 安装 JDK 的向导：大版本 → 发行版 → 确认。
 *
 *  两个问题分开问，因为它们是两件事：**要哪个主版本**由 Minecraft 决定
 *  （右侧的对照表就是答案），**要哪家的构建**由许可、平台和更新节奏决定
 *  （每个发行版给一段说明，不做推荐排序以外的暗示）。
 *
 *  Oracle JDK 不在列表里：它的许可允许使用、不允许再分发，面板代下载就是再分发。 */

type Step = 0 | 1 | 2;

const STEP_TITLES = ["选择 Java 版本", "选择发行版", "确认并安装"];

/// MC 版本 → 最低 Java 主版本。和 `McJavaRuntimes.recommendFor` 同源，这里只负责展示。
const MC_REQUIREMENTS: { range: string; java: number; note: string }[] = [
  { range: "26.x", java: 25, note: "当前最新一代" },
  { range: "1.20.5 – 1.21.x", java: 21, note: "近几年最常用" },
  { range: "1.18 – 1.20.4", java: 17, note: "大量整合包仍在这一档" },
  { range: "1.17.x", java: 16, note: "过渡版本" },
  { range: "≤ 1.16.5", java: 8, note: "老服 / 老整合包" },
];

interface VendorInfo {
  id: string;
  name: string;
  summary: string;
  license: string;
  points: string[];
}

const VENDORS: VendorInfo[] = [
  {
    id: "temurin",
    name: "Eclipse Temurin",
    summary: "Adoptium 维护的中立构建，默认推荐。",
    license: "GPLv2 + Classpath Exception，允许再分发",
    points: [
      "平台覆盖最全：Linux / Windows / macOS，x64 与 ARM64 都有",
      "更新节奏稳定，每个补丁版本都跟",
      "官方 API 直接给 SHA-256，面板可以逐字节校验",
    ],
  },
  {
    id: "microsoft",
    name: "Microsoft Build of OpenJDK",
    summary: "微软的 OpenJDK 构建，Windows 与 Azure 场景打磨得更多。",
    license: "GPLv2 + Classpath Exception，允许再分发",
    points: [
      "TCK 认证，和 Temurin 是同一份源码的不同构建",
      "Windows 主机、或已经在用微软技术栈时比较顺手",
      "长期更新有保障，也有官方容器镜像",
    ],
  },
  {
    id: "openjdk",
    name: "OpenJDK（官方参考构建）",
    summary: "OpenJDK 项目自己发布的 GPL 构建，最贴近上游。",
    license: "GPLv2 + Classpath Exception，允许再分发",
    points: [
      "没有厂商补丁，行为最接近规格本身",
      "只发 GA 版本，补丁版本以新目录出现，长期维护要自己盯",
      "适合想跟上游保持一致、或者做兼容性对照的场景",
    ],
  },
];

export function McJavaInstallWizard({
  open,
  onClose,
  onInstalled,
}: {
  open: boolean;
  onClose: () => void;
  onInstalled: () => void;
}) {
  const [step, setStep] = useState<Step>(0);
  const [snapshot, setSnapshot] = useState<McJavaRuntimesSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [feature, setFeature] = useState(21);
  const [vendor, setVendor] = useState("temurin");
  const [busy, setBusy] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [taskMsg, setTaskMsg] = useState("");
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const load = () => {
    setLoading(true);
    void api.mc
      .javaRuntimes(undefined, true)
      .then((answer) => {
        setSnapshot(answer);
        setError("");
      })
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!open) return;
    setStep(0);
    setFeature(21);
    setVendor("temurin");
    setBusy(false);
    setTaskId(null);
    setTaskMsg("");
    setDone(false);
    setError("");
    load();
  }, [open]);

  useEffect(() => {
    if (!taskId) return;
    const timer = window.setInterval(() => {
      void api
        .tasks()
        .then((tasks) => {
          const task = tasks.find((t) => t.id === taskId);
          if (!task) return;
          setTaskMsg(task.message ?? "");
          if (task.state === "done") {
            setTaskId(null);
            setBusy(false);
            setTaskMsg("");
            setDone(true);
            toast("success", "JDK 已安装");
            load();
            onInstalled();
          } else if (task.state === "failed") {
            setTaskId(null);
            setBusy(false);
            setError(task.error ?? task.message ?? "安装失败");
          }
        })
        .catch(() => undefined);
    }, 800);
    return () => window.clearInterval(timer);
  }, [taskId]);

  const elevated = async <T,>(call: () => Promise<T>): Promise<T> => {
    try {
      return await call();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      return await call();
    }
  };

  const releases = useMemo(
    () => (snapshot?.available ?? []).filter((entry) => entry.vendor === vendor),
    [snapshot, vendor],
  );
  const chosen = releases.find((entry) => entry.feature === feature) ?? null;
  const installedForFeature = (snapshot?.installed ?? []).find((entry) => entry.feature === feature);
  const vendorInfo = VENDORS.find((entry) => entry.id === vendor) ?? VENDORS[0];
  const availableFeatures = new Set(releases.map((entry) => entry.feature));

  const install = async () => {
    setBusy(true);
    setError("");
    try {
      const answer = await elevated(() => api.mc.installJava(feature, vendor));
      setTaskId(answer.taskId);
      setTaskMsg(`正在下载 ${vendorInfo.name} ${feature}…`);
      setStep(2);
    } catch (e) {
      setBusy(false);
      setError(errMsg(e));
    }
  };

  return (
    <Dialog
      open={open}
      wide
      title={`安装 JDK · ${STEP_TITLES[step]}`}
      onClose={() => {
        if (!busy) onClose();
      }}
      closeOnBackdrop={!busy}
      footer={
        <>
          <button
            className="btn btn-text"
            disabled={busy || step === 0}
            onClick={() => setStep((current) => (Math.max(0, current - 1) as Step))}
          >
            上一步
          </button>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-text" disabled={busy} onClick={onClose}>
              {done ? "关闭" : "取消"}
            </button>
            {step === 0 && (
              <button className="btn btn-raised" onClick={() => setStep(1)}>
                下一步
              </button>
            )}
            {step === 1 && (
              <button
                className="btn btn-raised"
                disabled={!chosen}
                onClick={() => {
                  setStep(2);
                  void install();
                }}
              >
                下载并安装
              </button>
            )}
            {step === 2 && done && (
              <button className="btn btn-raised" onClick={onClose}>
                完成
              </button>
            )}
          </div>
        </>
      }
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 12, width: "100%" }}>
        <div className="dim" style={{ fontSize: 12 }}>
          {step < 2 ? `第 ${step + 1} / 2 步` : done ? "安装完成" : "正在安装"}
        </div>

        {step === 0 && (
          <div style={{ display: "grid", gap: 10 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>要哪个 LTS 主版本？</div>
            {loading && <span className="dim" style={{ fontSize: 12 }}>正在读取可用版本…</span>}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))",
                gap: 8,
              }}
            >
              {[25, 21, 17, 11, 8].map((candidate) => (
                <button
                  key={candidate}
                  className="card ripple-host"
                  onClick={() => setFeature(candidate)}
                  style={{
                    padding: 10,
                    textAlign: "left",
                    display: "grid",
                    gap: 4,
                    cursor: "pointer",
                    border: candidate === feature ? "1px solid var(--monet-primary)" : undefined,
                  }}
                >
                  <span style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                    <b style={{ fontSize: 14 }}>Java {candidate}</b>
                    <span className="tag plain">LTS</span>
                  </span>
                  <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
                    {availableFeatures.has(candidate) ? "可下载" : "该发行版暂无"}
                  </span>
                </button>
              ))}
            </div>

            <div className="card" style={{ padding: 10, display: "grid", gap: 6 }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Minecraft 需要的最低 Java</div>
              {MC_REQUIREMENTS.map((row) => (
                <div
                  key={row.range}
                  style={{ display: "flex", gap: 8, alignItems: "baseline", fontSize: 12 }}
                >
                  <code style={{ minWidth: 120 }}>{row.range}</code>
                  <b>Java {row.java}</b>
                  <span className="dim">{row.note}</span>
                </div>
              ))}
              <span className="dim" style={{ fontSize: 11, lineHeight: 1.6 }}>
                装更高版本总能跑旧服；反过来不行。面板在启动前会按版本要求挑一个满足条件的。
              </span>
            </div>
          </div>
        )}

        {step === 1 && (
          <div style={{ display: "grid", gap: 8 }}>
            {VENDORS.map((entry) => {
              const has = (snapshot?.available ?? []).some(
                (release) => release.vendor === entry.id && release.feature === feature,
              );
              const health = snapshot?.vendors?.find((v) => v.id === entry.id);
              return (
                <button
                  key={entry.id}
                  className="card ripple-host"
                  disabled={!has}
                  onClick={() => setVendor(entry.id)}
                  style={{
                    padding: 10,
                    textAlign: "left",
                    display: "flex",
                    gap: 8,
                    alignItems: "center",
                    flexWrap: "wrap",
                    cursor: has ? "pointer" : "not-allowed",
                    opacity: has ? 1 : 0.55,
                    border: entry.id === vendor ? "1px solid var(--monet-primary)" : undefined,
                  }}
                >
                  <b style={{ fontSize: 13 }}>{entry.name}</b>
                  {has ? (
                    <span className="tag running">有 Java {feature}</span>
                  ) : (
                    <span className="tag plain">没有 Java {feature}</span>
                  )}
                  {health && !health.ok && <span className="tag failed">目录读取失败</span>}
                  {entry.id === vendor && <span className="tag running">已选</span>}
                </button>
              );
            })}
            <div className="card" style={{ padding: 12, display: "grid", gap: 5 }}>
              <b style={{ fontSize: 13 }}>{vendorInfo.name}</b>
              <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                {vendorInfo.summary}
              </span>
              <span style={{ fontSize: 11 }}>{vendorInfo.license}</span>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 11, lineHeight: 1.7 }}>
                {vendorInfo.points.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
            </div>
            <span className="dim" style={{ fontSize: 11, lineHeight: 1.6 }}>
              Oracle JDK 不在此列：它的许可允许使用、不允许再分发，面板代下载就等于再分发。
              需要时请自行安装，然后在实例的高级设置里指向它。
            </span>
          </div>
        )}

        {step === 2 && (
          <div style={{ display: "grid", gap: 10 }}>
            <div className="card" style={{ padding: 12, display: "grid", gap: 5 }}>
              <span style={{ fontSize: 14, fontWeight: 600 }}>
                {vendorInfo.name} · Java {feature}
              </span>
              <span className="dim" style={{ fontSize: 12 }}>
                {chosen
                  ? `${chosen.version} · ${Math.round(chosen.size / 1024 / 1024)} MB`
                  : "等待目录…"}
              </span>
              <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
                下载后先校验 SHA-256，再解包到 &lt;data&gt;/minecraft/java/{feature}/；
                解包完成会统一设置执行位。
              </span>
              {taskMsg && <span style={{ fontSize: 12 }}>{taskMsg}</span>}
            </div>
            {done && (
              <div className="hint info" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  <CheckCircleIcon size={16} style={{ verticalAlign: "-3px", marginRight: 4 }} />
                  已安装。新实例会按版本要求自动用它；已有实例可以在高级设置里选。
                </span>
              </div>
            )}
            {error && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  <WarningIcon size={14} style={{ verticalAlign: "-2px", marginRight: 4 }} />
                  {error}
                </span>
                <button className="btn btn-text" onClick={load}>
                  <RefreshIcon size={16} /> 重试
                </button>
              </div>
            )}
            {installedForFeature && !done && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  Java {feature} 已经装了（{installedForFeature.vendorLabel ?? "未知发行版"}）。
                  继续安装会替换掉它，先确认没有实例钉在它上面。
                </span>
              </div>
            )}
          </div>
        )}

      </div>
    </Dialog>
  );
}
