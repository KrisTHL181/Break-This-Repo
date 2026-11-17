import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  api,
  type McCoreInfo,
  type McInstance,
  type McJavaRuntimesSnapshot,
  type McStorage,
} from "../api";
import { Dialog } from "./Dialog";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { useMcFeature } from "../mcFeature";
import { seedTasks, toast } from "../store";
import { errMsg, mcCoreLabel, mcCoreNote } from "../utils";
import { CheckCircleIcon, RefreshIcon, WarningIcon } from "./icons";

/** 第三方服务器创建向导。
 *
 *  和原版向导刻意分开：这里的每一步都在回答一个第三方链路才有的问题——
 *  核心、版本、运行环境、下载源。原版向导保持原来的四步，不因为多了镜像而变复杂。
 *
 *  步骤：核心 → 版本 → 运行环境 → 实例信息 → 安装 → 完成。
 *  下载失败时提供「手动导入 jar」：所有源都不可达时，这是唯一不会把用户困住的出口。 */

type Step = 0 | 1 | 2 | 3 | 4 | 5;

const STEP_TITLES = ["选择核心", "选择版本", "运行环境", "实例信息", "下载与安装", "完成"];

/// 每个核心至少吃的堆。推荐值只会往上抬，不会低于它。
function memoryFloor(core: string): number {
  switch (core) {
    case "forge":
    case "neoforge":
      return 4096;
    case "fabric":
    case "paper":
    case "folia":
    case "purpur":
      return 2048;
    default:
      return 1024;
  }
}

/// 版本粗推荐：1.20.5+ 要 Java 21，1.18–1.20.4 要 17，1.17 要 16，更早要 8。
export function recommendJava(version: string): number {
  const match = /^1\.(\d+)(?:\.(\d+))?/.exec(version.trim());
  if (!match) return 21;
  const minor = Number(match[1]);
  const patch = Number(match[2] ?? 0);
  if (minor >= 21) return 21;
  if (minor === 20 && patch >= 5) return 21;
  if (minor >= 18) return 17;
  if (minor === 17) return 16;
  return 8;
}

export function McThirdPartyWizard({
  open,
  onClose,
  onCreated,
  onOpenSettings,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (instance: McInstance) => void;
  onOpenSettings: () => void;
}) {
  const feature = useMcFeature();
  const minMemoryMb = feature?.limits?.minMemoryMb ?? 256;
  const maxMemoryMb = feature?.limits?.maxMemoryMb ?? 8192;

  const [step, setStep] = useState<Step>(0);
  const [cores, setCores] = useState<McCoreInfo[]>([]);
  const [coresLoading, setCoresLoading] = useState(false);
  const [coresError, setCoresError] = useState<string | null>(null);
  const [core, setCore] = useState("");
  const [version, setVersion] = useState("");
  const [versionQuery, setVersionQuery] = useState("");

  const [name, setName] = useState("");
  const [levelName, setLevelName] = useState("world");
  const [seed, setSeed] = useState("");
  const [memoryMb, setMemoryMb] = useState(4096);
  const [port, setPort] = useState(25565);
  const [rconPort, setRconPort] = useState(25575);
  const [storage, setStorage] = useState<McStorage | null>(null);
  const [storageChoice, setStorageChoice] = useState<"external" | "data">("external");
  const [worldFile, setWorldFile] = useState<File | null>(null);
  const [worldImported, setWorldImported] = useState(false);

  const [java, setJava] = useState<McJavaRuntimesSnapshot | null>(null);
  const [javaError, setJavaError] = useState<string | null>(null);

  const [instance, setInstance] = useState<McInstance | null>(null);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [progress, setProgress] = useState<{ fraction: number; message: string } | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);

  const loadCores = (refresh = false) => {
    setCoresLoading(true);
    setCoresError(null);
    void api.mc
      .cores(refresh)
      .then((answer) => {
        const thirdParty = answer.cores.filter((entry) => entry.thirdParty);
        setCores(thirdParty);
        if (thirdParty.length > 0 && !thirdParty.some((entry) => entry.id === core)) {
          // 不留一个空选择：默认落在 Paper，它是这一栏最常用的核心。
          const preferred = thirdParty.find((entry) => entry.id === "paper") ?? thirdParty[0];
          setCore(preferred.id);
          setVersion((current) => (preferred.versions.includes(current) ? current : ""));
        }
      })
      .catch((e) => {
        setCores([]);
        setCoresError(errMsg(e));
      })
      .finally(() => setCoresLoading(false));
  };

  useEffect(() => {
    if (!open) return;
    setStep(0);
    setError("");
    setInstance(null);
    setTaskId(null);
    setProgress(null);
    setWorldImported(false);
    setWorldFile(null);
    setUploadFile(null);
    setCore("");
    setVersion("");
    setVersionQuery("");
    setName("");
    setLevelName("world");
    setSeed("");
    setMemoryMb(4096);
    setJava(null);
    setJavaError(null);
    loadCores(true);
    void api.mc
      .suggestPorts()
      .then((pair) => {
        setPort(pair.port);
        setRconPort(pair.rconPort);
      })
      .catch(() => undefined);
    void api.mc
      .storage()
      .then((info) => {
        setStorage(info);
        setStorageChoice(info.default);
      })
      .catch(() => setStorage(null));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 选了版本就把「这台机器上有没有对应 Java」查清楚：第三方核心最常见的启动失败
  // 不是缺 jar，而是 Java 版本不对。
  useEffect(() => {
    if (!open || !version) return;
    let cancelled = false;
    setJavaError(null);
    void api.mc
      .javaRuntimes(version)
      .then((answer) => {
        if (!cancelled) setJava(answer);
      })
      .catch((e) => {
        if (!cancelled) {
          setJava(null);
          setJavaError(errMsg(e));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open, version]);

  useEffect(() => {
    if (!taskId) return;
    const timer = window.setInterval(() => {
      void api
        .tasks()
        .then((tasks) => {
          const task = tasks.find((t) => t.id === taskId);
          if (!task) return;
          setProgress({ fraction: task.fraction ?? -1, message: task.message ?? "" });
          if (task.state === "done") {
            setTaskId(null);
            setStep(5);
            void api.mc.instance(instance?.id ?? "").then(setInstance).catch(() => undefined);
          } else if (task.state === "failed") {
            setTaskId(null);
            setError(task.error ?? task.message ?? "下载失败");
          }
        })
        .catch(() => undefined);
    }, 800);
    return () => window.clearInterval(timer);
  }, [taskId, instance?.id]);

  const elevated = async <T,>(call: () => Promise<T>): Promise<T> => {
    try {
      return await call();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      return await call();
    }
  };

  const selectedCore = cores.find((entry) => entry.id === core) ?? null;
  const allVersions = selectedCore?.versions ?? [];
  const visibleVersions = useMemo(() => {
    const q = versionQuery.trim().toLowerCase();
    const list = q ? allVersions.filter((id) => id.toLowerCase().includes(q)) : allVersions;
    return list.slice(0, 120);
  }, [allVersions, versionQuery]);

  const recommendedMemory = Math.max(memoryFloor(core), 2048);
  const recommendedJava = version ? recommendJava(version) : 21;
  const javaReady = java?.installed.some((entry) => entry.feature === recommendedJava) ?? false;

  const canLeaveStep0 = core !== "";
  const canLeaveStep1 = version !== "";
  const canLeaveStep2 =
    Number.isInteger(memoryMb) && memoryMb >= minMemoryMb && memoryMb <= maxMemoryMb
    && port > 0 && rconPort > 0 && port !== rconPort;
  const canLeaveStep3 = name.trim().length > 0;

  const startInstall = async () => {
    setBusy(true);
    setError("");
    setStep(4);
    try {
      let created = instance;
      if (created === null) {
        created = await elevated(() =>
          api.mc.create({
            name: name.trim(),
            version,
            core,
            memoryMb,
            levelName: levelName.trim() || "world",
            seed: seed.trim() || undefined,
            port,
            rconPort,
            storage: storageChoice,
          }),
        );
        setInstance(created);
        onCreated(created);
      }
      if (worldFile && !worldImported) {
        setProgress({ fraction: -1, message: `正在导入存档 ${worldFile.name}…` });
        await elevated(() => api.mc.importWorld(created!.id, worldFile));
        setWorldImported(true);
      }
      const task = await elevated(() => api.mc.install(created.id, version));
      setTaskId(task.taskId);
      // 用户通常不会等下载完就关掉向导；先把任务放进 store，列表据此显示进度。
      void seedTasks();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const uploadCore = async () => {
    if (!instance || !uploadFile) return;
    setBusy(true);
    setError("");
    try {
      const updated = await elevated(() => api.mc.uploadCore(instance.id, version, uploadFile));
      setInstance(updated);
      setStep(5);
      toast("success", "核心 jar 已导入");
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const dataStorage = storage?.data ?? null;

  return (
    <Dialog
      open={open}
      wide
      title={`新建第三方服务器 · ${STEP_TITLES[step]}`}
      onClose={() => {
        if (!busy) onClose();
      }}
      closeOnBackdrop={!busy}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 12, minWidth: 460 }}>
        <div className="dim" style={{ fontSize: 12 }}>
          {step < 5 ? `第 ${step + 1} / 5 步` : "服务器已就绪"}
        </div>

        {step === 0 && (
          <div style={{ display: "grid", gap: 8 }}>
            {coresLoading && (
              <div className="hint info" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>正在读取核心列表…</span>
              </div>
            )}
            {!coresLoading && coresError && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>核心列表读取失败：{coresError}</span>
                <button className="btn btn-text" onClick={() => loadCores(true)}>
                  <RefreshIcon size={16} /> 重试
                </button>
              </div>
            )}
            {!coresLoading && !coresError && cores.length === 0 && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  没有读到任何第三方核心。先到「全局设置」检查下载源，或者确认上游可达。
                </span>
                <button className="btn btn-text" onClick={onOpenSettings}>
                  全局设置
                </button>
              </div>
            )}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))", gap: 8 }}>
              {cores.map((entry) => (
                <button
                  key={entry.id}
                  className="card ripple-host"
                  onClick={() => {
                    setCore(entry.id);
                    if (!entry.versions.includes(version)) setVersion("");
                  }}
                  style={{
                    padding: 10,
                    textAlign: "left",
                    display: "grid",
                    gap: 4,
                    cursor: "pointer",
                    border: entry.id === core ? "1px solid var(--monet-primary)" : undefined,
                  }}
                >
                  <span style={{ fontWeight: 600, fontSize: 13 }}>{entry.label}</span>
                  <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
                    {entry.versions.length} 个版本
                  </span>
                  <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
                    {mcCoreNote(entry.id)}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {step === 1 && (
          <div style={{ display: "grid", gap: 8 }}>
            <div className="dim" style={{ fontSize: 12 }}>
              {mcCoreLabel(core)} · {allVersions.length} 个可用版本
            </div>
            <input
              className="input"
              placeholder="搜索版本，例如 1.20"
              value={versionQuery}
              onChange={(e) => setVersionQuery(e.target.value)}
            />
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(96px, 1fr))",
                gap: 6,
                maxHeight: 280,
                overflow: "auto",
                padding: 2,
              }}
            >
              {visibleVersions.map((id) => (
                <button
                  key={id}
                  className={`btn ${id === version ? "btn-raised" : "btn-text"}`}
                  style={{ fontSize: 12 }}
                  onClick={() => setVersion(id)}
                >
                  {id}
                </button>
              ))}
            </div>
            {visibleVersions.length === 0 && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  {allVersions.length === 0
                    ? "这个核心的版本列表是空的：上游或镜像没答上来。可以回上一步换核心，或去「全局设置」调源。"
                    : "没有匹配的版本。"}
                </span>
              </div>
            )}
            {selectedCore && selectedCore.versions.length === 0 && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>{selectedCore.label} 的上游版本列表暂时没读到。</span>
                <button className="btn btn-text" onClick={() => loadCores(true)}>
                  重试
                </button>
              </div>
            )}
          </div>
        )}

        {step === 2 && (
          <div style={{ display: "grid", gap: 10 }}>
            <Field label="内存（MB）" hint={`范围 ${minMemoryMb}–${maxMemoryMb} MB`}>
              <div style={{ display: "flex", gap: 8 }}>
                <input
                  className="input"
                  type="number"
                  min={minMemoryMb}
                  max={maxMemoryMb}
                  step={256}
                  value={memoryMb}
                  onChange={(e) => setMemoryMb(Number(e.target.value) || 1024)}
                />
                <button
                  className="btn btn-text"
                  onClick={() => setMemoryMb(Math.min(maxMemoryMb, recommendedMemory))}
                >
                  推荐值 {recommendedMemory} MB
                </button>
              </div>
            </Field>
            <Field label="Java" hint="创建后可以在实例页「高级设置」里改；启动后该选项锁定">
              <div className="card" style={{ padding: 10, display: "grid", gap: 4 }}>
                <span style={{ fontSize: 12 }}>
                  推荐 Java {recommendedJava}
                  {javaReady ? " · 面板已安装" : " · 尚未安装"}
                </span>
                {java?.installed.length ? (
                  <span className="dim" style={{ fontSize: 11 }}>
                    已安装：
                    {java.installed.map((entry) => `Java ${entry.feature}`).join(" / ")}
                  </span>
                ) : null}
                {javaError && (
                  <span style={{ fontSize: 11, color: "var(--monet-error)" }}>{javaError}</span>
                )}
                <span className="dim" style={{ fontSize: 11 }}>
                  没装也能建：启动时面板会挑满足要求的最低版本；缺了会在实例页提示安装。
                </span>
              </div>
            </Field>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              <Field label="游戏端口" hint="客户端连这个">
                <input
                  className="input"
                  type="number"
                  min={1024}
                  max={65535}
                  value={port}
                  onChange={(e) => setPort(Number(e.target.value) || 0)}
                />
              </Field>
              <Field label="RCON 端口" hint="不能与游戏端口相同">
                <input
                  className="input"
                  type="number"
                  min={1024}
                  max={65535}
                  value={rconPort}
                  onChange={(e) => setRconPort(Number(e.target.value) || 0)}
                />
              </Field>
            </div>
            {port === rconPort && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>游戏端口与 RCON 端口不能相同。</span>
              </div>
            )}
          </div>
        )}

        {step === 3 && (
          <div style={{ display: "grid", gap: 8 }}>
            <Field label="实例名">
              <input
                className="input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="模组服"
                autoFocus
              />
            </Field>
            <Field label="世界目录名" hint="存档文件夹名">
              <input className="input" value={levelName} onChange={(e) => setLevelName(e.target.value)} />
            </Field>
            <Field label="种子（可空）">
              <input className="input" value={seed} onChange={(e) => setSeed(e.target.value)} placeholder="留空随机" />
            </Field>
            <Field label="存储位置" hint="容器里建议用专用目录，换镜像重建容器时存档还在">
              <div style={{ display: "grid", gap: 6 }}>
                <StorageOption
                  title="专用目录"
                  path={storage?.external.path ?? "（读取中…）"}
                  note="推荐：容器里是独立挂载点"
                  checked={storageChoice === "external"}
                  disabled={!storage?.external.writable}
                  onSelect={() => setStorageChoice("external")}
                />
                {dataStorage && (
                  <StorageOption
                    title="旧数据目录"
                    path={dataStorage.path}
                    note="升级前的布局，仍在兼容"
                    checked={storageChoice === "data"}
                    disabled={!dataStorage.writable}
                    onSelect={() => setStorageChoice("data")}
                  />
                )}
              </div>
            </Field>
            <Field label="现有存档（可选）" hint="zip：顶层目录名与「世界目录名」一致">
              <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                <label className="btn btn-text" style={{ cursor: "pointer" }}>
                  {worldFile ? "换一个 zip…" : "选择 zip…"}
                  <input
                    type="file"
                    accept=".zip"
                    style={{ display: "none" }}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) setWorldFile(file);
                      e.target.value = "";
                    }}
                  />
                </label>
                {worldFile ? (
                  <>
                    <span style={{ fontSize: 12 }}>
                      {worldFile.name} · {Math.max(1, Math.round(worldFile.size / 1024 / 1024))} MB
                    </span>
                    <button className="btn btn-text" onClick={() => setWorldFile(null)}>
                      移除
                    </button>
                  </>
                ) : (
                  <span className="dim" style={{ fontSize: 12 }}>不选则从空世界开始</span>
                )}
              </div>
            </Field>
          </div>
        )}

        {step === 4 && (
          <div style={{ display: "grid", gap: 10 }}>
            <div className="card" style={{ padding: 10, display: "grid", gap: 6 }}>
              <span style={{ fontSize: 13, fontWeight: 600 }}>
                {mcCoreLabel(core)} {version}
              </span>
              <span className="dim" style={{ fontSize: 12 }}>
                内存 {memoryMb} MB · 端口 {port} / RCON {rconPort} · Java {recommendedJava}
              </span>
              <ProgressBar fraction={progress?.fraction ?? -1} />
              <span style={{ fontSize: 12 }}>{progress?.message ?? "准备中…"}</span>
            </div>
            {error && (
              <div className="hint warning" style={{ margin: 0, display: "grid", gap: 8 }}>
                <span style={{ flex: 1 }}>
                  <WarningIcon size={16} style={{ verticalAlign: "-3px", marginRight: 4 }} />
                  {error}
                </span>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <button className="btn btn-text" onClick={() => void startInstall()} disabled={busy}>
                    重试
                  </button>
                  <button className="btn btn-text" onClick={onOpenSettings}>
                    检查下载源
                  </button>
                </div>
                <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                  <label className="btn btn-text" style={{ cursor: "pointer" }}>
                    {uploadFile ? uploadFile.name : "手动导入 server.jar…"}
                    <input
                      type="file"
                      accept=".jar"
                      style={{ display: "none" }}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) setUploadFile(file);
                        e.target.value = "";
                      }}
                    />
                  </label>
                  <button
                    className="btn btn-raised"
                    disabled={busy || !uploadFile || !instance}
                    onClick={() => void uploadCore()}
                  >
                    导入
                  </button>
                  <span className="dim" style={{ fontSize: 11 }}>
                    所有源都不可达时的兜底：文件会按当前版本号存进面板的核心目录。
                  </span>
                </div>
              </div>
            )}
          </div>
        )}

        {step === 5 && (
          <div style={{ display: "grid", gap: 10 }}>
            <div className="hint info" style={{ margin: 0 }}>
              <span style={{ flex: 1 }}>
                <CheckCircleIcon size={16} style={{ verticalAlign: "-3px", marginRight: 4 }} />
                {instance?.name ?? name} 已就绪（{mcCoreLabel(core)} {version}）。
                可以在列表里启动，装插件/模组在实例页的「插件 / 模组」页签。
              </span>
            </div>
          </div>
        )}

        <div style={{ display: "flex", gap: 8, justifyContent: "space-between", alignItems: "center" }}>
          <button
            className="btn btn-text"
            disabled={busy || step === 4 || step === 5}
            onClick={() => setStep((current) => (Math.max(0, current - 1) as Step))}
          >
            上一步
          </button>
          <div style={{ display: "flex", gap: 8 }}>
            {step === 4 && (
              <button className="btn btn-text" onClick={onClose}>
                后台继续
              </button>
            )}
            <button className="btn btn-text" disabled={busy} onClick={onClose}>
              取消
            </button>
            {step < 4 && (
              <button
                className="btn btn-raised"
                disabled={
                  busy
                  || (step === 0 && !canLeaveStep0)
                  || (step === 1 && !canLeaveStep1)
                  || (step === 2 && !canLeaveStep2)
                  || (step === 3 && !canLeaveStep3)
                }
                onClick={() => setStep((current) => ((current + 1) as Step))}
              >
                下一步
              </button>
            )}
            {step === 3 && (
              <button className="btn btn-raised" disabled={busy || !canLeaveStep3} onClick={() => void startInstall()}>
                创建并下载
              </button>
            )}
            {step === 5 && (
              <button className="btn btn-raised" onClick={onClose}>
                完成
              </button>
            )}
          </div>
        </div>
      </div>
    </Dialog>
  );
}

function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label style={{ display: "grid", gap: 4 }}>
      <span style={{ fontSize: 12, fontWeight: 600 }}>{label}</span>
      {children}
      {hint && (
        <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>{hint}</span>
      )}
    </label>
  );
}

function StorageOption({
  title,
  path,
  note,
  checked,
  disabled,
  onSelect,
}: {
  title: string;
  path: string;
  note: string;
  checked: boolean;
  disabled: boolean;
  onSelect: () => void;
}) {
  return (
    <label
      className="card"
      style={{
        display: "grid",
        gap: 4,
        padding: 10,
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.55 : 1,
        border: checked ? "1px solid var(--monet-primary)" : undefined,
      }}
    >
      <span style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <input type="radio" checked={checked} disabled={disabled} onChange={onSelect} />
        <span style={{ fontWeight: 600, fontSize: 13 }}>{title}</span>
        <span className="tag plain">{disabled ? "不可写" : "可写"}</span>
      </span>
      <code style={{ fontSize: 11, wordBreak: "break-all" }}>{path}</code>
      <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>{note}</span>
    </label>
  );
}

function ProgressBar({ fraction }: { fraction: number }) {
  const pct = fraction >= 0 ? Math.min(100, Math.round(fraction * 100)) : null;
  return (
    <div style={{ height: 8, borderRadius: 4, background: "var(--monet-surface-container-highest)" }}>
      <div
        style={{
          height: "100%",
          borderRadius: 4,
          width: pct === null ? "30%" : `${pct}%`,
          background: "var(--monet-primary)",
          transition: "width 200ms",
        }}
      />
    </div>
  );
}
