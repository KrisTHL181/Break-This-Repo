import { useEffect, useState } from "react";
import { api, type McCoreInfo, type McInstance, type McStorage, type McVersion } from "../api";
import { Dialog } from "./Dialog";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { useMcFeature } from "../mcFeature";
import { seedTasks, toast } from "../store";
import { errMsg, mcCoreNote } from "../utils";
import { CheckCircleIcon, WarningIcon } from "./icons";

/** MC 实例创建向导：填参数 → 选版本 → 选存储 → 下载服务端 → 直接可启动。
 *
 *  以前是一个单行表单，创建只写清单，装服务端要另进实例页点一次；用户看到的是
 *  「创建成功但启动不了」。向导把这两步连起来，走完最后一步时 `server.jar` 已经在
 *  磁盘上，按钮直接就是「启动」。
 *
 *  存储位置是这一步的重点：容器里默认的 `/home/hdsl/mc` 是**独立挂载点**
 *  （compose / docker-install.sh 都挂在那里），换镜像重建容器，存档与原版服务端
 *  都还在；旧的 `<数据目录>/minecraft/instances` 只有在部署确实提供时才作为备选。 */

type Step = 0 | 1 | 2 | 3 | 4;

const STEP_TITLES = ["基本信息", "游戏版本", "存储位置", "下载服务端", "完成"];

export function McCreateWizard({
  open,
  versions,
  thirdPartyCores = true,
  onClose,
  onCreated,
}: {
  open: boolean;
  versions: McVersion[];
  /** 原版那一栏传 `false`：核心选择器只剩原版，第三方入口在另一栏。 */
  thirdPartyCores?: boolean;
  onClose: () => void;
  onCreated: (instance: McInstance) => void;
}) {
  const feature = useMcFeature();
  const minMemoryMb = feature?.limits?.minMemoryMb ?? 256;
  const maxMemoryMb = feature?.limits?.maxMemoryMb ?? 8192;
  const [step, setStep] = useState<Step>(0);
  const [name, setName] = useState("");
  const [memoryMb, setMemoryMb] = useState(2048);
  const [levelName, setLevelName] = useState("world");
  const [seed, setSeed] = useState("");
  const [port, setPort] = useState(25565);
  const [rconPort, setRconPort] = useState(25575);
  const [version, setVersion] = useState("");
  const [core, setCore] = useState("vanilla");
  const [cores, setCores] = useState<McCoreInfo[]>([]);
  const [coresLoading, setCoresLoading] = useState(false);
  const [coreError, setCoreError] = useState<string | null>(null);
  const [storage, setStorage] = useState<McStorage | null>(null);
  const [storageChoice, setStorageChoice] = useState<"external" | "data">("external");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [instance, setInstance] = useState<McInstance | null>(null);
  const [worldFile, setWorldFile] = useState<File | null>(null);
  const [worldImported, setWorldImported] = useState(false);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [progress, setProgress] = useState<{ fraction: number; message: string } | null>(null);

  const releases = versions.filter((entry) => entry.type === "release").slice(0, 80);
  // 核心列表来自 `/api/mc/cores`：第二道关关闭时后端只返回 vanilla。
  // 原版那一栏只保留 vanilla，第三方核心的去处是「MC 第三方服务器实例」。
  const visibleCores = thirdPartyCores ? cores : cores.filter((entry) => !entry.thirdParty);
  const selectedCore = visibleCores.find((entry) => entry.id === core) ?? null;
  const coreVersions = selectedCore
    ? selectedCore.versions.slice(0, 200)
    : releases.map((entry) => entry.id);

  /// 核心列表包含六个上游，第一次打开可能要等几秒；失败时不要静默退回原版，
  /// 否则用户只会看到「只能下原版」。重试按钮复用同一段。
  const loadCores = (refresh = false) => {
    setCoresLoading(true);
    setCoreError(null);
    void api.mc
      .cores(refresh)
      .then((answer) => setCores(answer.cores))
      .catch((e) => {
        setCores([]);
        setCoreError(errMsg(e));
      })
      .finally(() => setCoresLoading(false));
  };

  // 打开时取一份建议端口与存储信息：端口不能和已有实例撞，路径要显示真实值。
  useEffect(() => {
    if (!open) return;
    setStep(0);
    setError("");
    setInstance(null);
    setWorldImported(false);
    setTaskId(null);
    setProgress(null);
    setCore("vanilla");
    loadCores();
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
  }, [open]);

  // 下载任务的进度：任务服务端在跑，关掉向导也不会丢。
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
            setStep(4);
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

  const startInstall = async () => {
    setBusy(true);
    setError("");
    try {
      // 重试时只重跑下载：实例已经建好，再 create 一次会多出一个服。
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
      // 旧存档先落地，再装服务端：导入不需要服务器进程，而且失败时实例已经
      // 存在，重试只会重跑这一步（worldImported 置位后不再重复导入）。
      if (worldFile && !worldImported) {
        setStep(3);
        setProgress({ fraction: -1, message: `正在导入存档 ${worldFile.name}…` });
        await elevated(() => api.mc.importWorld(created!.id, worldFile));
        setWorldImported(true);
      }
      const task = await elevated(() => api.mc.install(created.id, version));
      setTaskId(task.taskId);
      // 让实例列表立刻拿到这个任务：用户通常不会等下载完就关掉向导。
      void seedTasks();
      setStep(3);
    } catch (e) {
      setError(errMsg(e));
      setStep(3);
    } finally {
      setBusy(false);
    }
  };

  const canLeaveStep0 = name.trim().length > 0 && port > 0 && rconPort > 0 && port !== rconPort;
  const dataStorage = storage?.data ?? null;

  return (
    <Dialog
      open={open}
      wide
      title={`新建 MC 服务器 · ${STEP_TITLES[step]}`}
      onClose={() => {
        if (!busy) onClose();
      }}
      closeOnBackdrop={!busy}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 12, minWidth: 420 }}>
        <div className="dim" style={{ fontSize: 12 }}>
          {step < 4 ? `第 ${step + 1} / 4 步` : "服务器已就绪"}
        </div>

        {step === 0 && (
          <div style={{ display: "grid", gap: 8 }}>
            <Field label="实例名">
              <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="生存服" autoFocus />
            </Field>
            <Field label="游戏端口" hint="客户端连这个端口；防火墙要放行">
              <input className="input" type="number" min={1024} max={65535} value={port} onChange={(e) => setPort(Number(e.target.value) || 0)} />
            </Field>
            <Field label="RCON 端口" hint="面板用它执行命令与查在线玩家，不能和游戏端口相同">
              <input className="input" type="number" min={1024} max={65535} value={rconPort} onChange={(e) => setRconPort(Number(e.target.value) || 0)} />
            </Field>
            <Field
              label="内存（MB）"
              hint={`范围 ${minMemoryMb}–${maxMemoryMb} MB；创建后可以在实例页「服务端设置 → 高级设置」里调整，也能点「推荐值」。`}
            >
              <input
                className="input"
                type="number"
                min={minMemoryMb}
                max={maxMemoryMb}
                step={256}
                value={memoryMb}
                onChange={(e) => setMemoryMb(Number(e.target.value) || 1024)}
              />
            </Field>
            <Field label="世界目录名" hint="存档文件夹名，默认 world">
              <input className="input" value={levelName} onChange={(e) => setLevelName(e.target.value)} />
            </Field>
            <Field label="种子（可空）">
              <input className="input" value={seed} onChange={(e) => setSeed(e.target.value)} placeholder="留空随机" />
            </Field>
            <Field
              label="现有存档（可选）"
              hint="旧服的 world 目录压成 zip：顶层目录名要和上面的「世界目录名」一致，也可以直接压世界内容；只搬存档，不含插件与名单"
            >
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
                  <span className="dim" style={{ fontSize: 12 }}>不选则从一个空世界开始</span>
                )}
              </div>
            </Field>
          </div>
        )}

        {step === 1 && (
          <div style={{ display: "grid", gap: 8 }}>
            {thirdPartyCores && coresLoading && (
              <div className="hint info" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>正在读取服务端核心列表…（Paper / Purpur / Fabric / Forge 等上游）</span>
              </div>
            )}
            {thirdPartyCores && !coresLoading && (coreError || cores.length <= 1) && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  {coreError
                    ? `核心列表读取失败：${coreError}。可以重试，或先建原版。`
                    : feature == null
                      ? "正在读取 MC 功能状态…"
                      : feature.thirdParty
                        ? "第三方核心已开启，但上游版本列表还没读到（可能是网络或上游 API 超时）。可以重试，或先建原版。"
                        : "当前只提供原版核心。第三方核心需要在「设置 → 品牌实例 → MC 服务端 → 第三方核心」开启并同意条款。"}
                </span>
                <button className="btn btn-text" style={{ marginLeft: 8 }} onClick={() => loadCores(true)}>
                  重试
                </button>
              </div>
            )}
            {thirdPartyCores && cores.length > 1 && (
              <Field
                label="服务端核心"
                hint="原版 = Mojang 官方服务器；Paper/Folia/Purpur/Fabric/Forge 等为第三方核心，需单独开启并同意条款"
              >
                <select
                  className="input"
                  value={core}
                  onChange={(e) => {
                    const next = e.target.value;
                    setCore(next);
                    const nextVersions = visibleCores.find((entry) => entry.id === next)?.versions ?? [];
                    if (!nextVersions.includes(version)) setVersion("");
                  }}
                >
                  {visibleCores.map((entry) => (
                    <option key={entry.id} value={entry.id}>
                      {entry.label}
                      {entry.thirdParty ? "（第三方）" : ""}
                    </option>
                  ))}
                </select>
              </Field>
            )}
            <Field label="Minecraft 版本" hint={mcCoreNote(selectedCore?.id ?? core)}>
              <select className="input" value={version} onChange={(e) => setVersion(e.target.value)}>
                <option value="">选择一个版本</option>
                {coreVersions.map((id) => (
                  <option key={id} value={id}>
                    {id}
                  </option>
                ))}
              </select>
            </Field>
            {selectedCore?.thirdParty && selectedCore.versions.length === 0 && (
              <div className="hint warning" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>
                  {selectedCore.label} 的上游版本列表暂时没读到（可能是网络或上游 API 超时）。
                  点上面的「重试」，或换一个核心。
                </span>
              </div>
            )}
            <div className="hint info" style={{ margin: 0 }}>
              <span style={{ flex: 1 }}>
                {selectedCore?.id === "forge" || selectedCore?.id === "neoforge"
                  ? "Forge/NeoForge 没有现成的服务端 jar：面板会运行上游安装器，把 libraries 写进这个实例的目录，耗时数分钟。"
                  : "服务端程序不随本项目分发，安装时从上游下载并按上游公布的哈希校验。同一核心同一版本在面板里只存一份，多实例共用。"}
              </span>
            </div>
            <div className="hint warning" style={{ margin: 0 }}>
              <span style={{ flex: 1 }}>
                已有服务器要搬过来？把版本选成<b>和旧服一致</b>的那个，建完后到实例页「世界」
                导入旧存档的 zip——只搬存档，服务端程序仍用上游下载；插件、白名单与 OP 名单不随存档走。
              </span>
            </div>
          </div>
        )}

        {step === 2 && (
          <div style={{ display: "grid", gap: 8 }}>
            <StorageOption
              checked={storageChoice === "external"}
              disabled={storage !== null && !storage.external.writable}
              title="专用目录（推荐）"
              path={storage?.external.path ?? "…"}
              note="容器里是 /home/hdsl/mc：compose 与 docker-install.sh 都在这儿挂卷/宿主目录，重建容器不会丢存档。"
              onSelect={() => setStorageChoice("external")}
            />
            {dataStorage && (
              <StorageOption
                checked={storageChoice === "data"}
                disabled={!dataStorage.writable}
                title="面板数据目录（旧位置）"
                path={dataStorage.path}
                note="升级前的默认位置，跟 /data 一起备份。没有独立挂载时选它。"
                onSelect={() => setStorageChoice("data")}
              />
            )}
            <div className="hint warning" style={{ margin: 0 }}>
              <WarningIcon size={16} />
              <span style={{ flex: 1 }}>
                在本机改配置（`mc.dir` / `HDSL_MC_DIR`）之前，实例不会搬家；
                存档、`server.jar`、日志都放在所选目录下的实例文件夹里。
              </span>
            </div>
            <div className="dim" style={{ fontSize: 12 }}>
              EULA 已在开启 MC 功能时确认；创建时会为该实例写入 `eula.txt`。
            </div>
          </div>
        )}

        {step === 3 && (
          <div style={{ display: "grid", gap: 8 }}>
            {error ? (
              <div className="hint error" style={{ margin: 0 }}>
                <span style={{ flex: 1 }}>{error}</span>
              </div>
            ) : (
              <>
                <ProgressBar fraction={progress?.fraction ?? -1} />
                <div style={{ fontSize: 13 }}>{progress?.message || "正在准备下载…"}</div>
                <div className="dim" style={{ fontSize: 12 }}>
                  下载/安装由服务端任务执行，关掉这个对话框也会继续；实例列表里会显示进度条，
                  装完才能点启动。
                </div>
              </>
            )}
          </div>
        )}

        {step === 4 && instance && (
          <div style={{ display: "grid", gap: 8 }}>
            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <CheckCircleIcon size={18} />
              <span style={{ fontWeight: 600 }}>服务端已就绪，可以直接启动</span>
            </div>
            <div className="dim" style={{ fontSize: 12, lineHeight: 1.7 }}>
              {instance.name} · {instance.version} · 端口 {instance.port} · RCON {instance.rconPort}
              <br />
              目录：{instance.dir}
            </div>
            <div className="hint info" style={{ margin: 0 }}>
              <span style={{ flex: 1 }}>
                启动后客户端连 `{"<本机IP>"}:{instance.port}`。要公网访问，记得在防火墙放行该端口。
              </span>
            </div>
          </div>
        )}

        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", paddingTop: 4 }}>
          {step === 4 && instance ? (
            <>
              <button
                className="btn btn-text"
                onClick={() => {
                  onClose();
                }}
              >
                关闭
              </button>
              <button
                className="btn btn-raised"
                onClick={() => {
                  const created = instance;
                  void elevated(() => api.mc.start(created.id))
                    .then(() => {
                      toast("info", "正在启动");
                      onClose();
                    })
                    .catch((e) => toast("error", errMsg(e)));
                }}
              >
                立即启动
              </button>
            </>
          ) : (
            <>
              {step > 0 && step < 3 && (
                <button className="btn btn-text" onClick={() => setStep((step - 1) as Step)}>
                  上一步
                </button>
              )}
              {step < 2 && (
                <button
                  className="btn btn-raised"
                  disabled={step === 0 ? !canLeaveStep0 : !version}
                  onClick={() => setStep((step + 1) as Step)}
                >
                  下一步
                </button>
              )}
              {step === 2 && (
                <button className="btn btn-raised" disabled={busy} onClick={() => void startInstall()}>
                  {busy ? "创建中…" : "开始下载并安装"}
                </button>
              )}
              {step === 3 && !error && (
                <button className="btn btn-text" onClick={onClose}>
                  后台继续
                </button>
              )}
              {step === 3 && error && (
                <button className="btn btn-raised" onClick={() => void startInstall()}>
                  重试
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </Dialog>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label style={{ display: "grid", gap: 4 }}>
      <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>{label}</span>
      {children}
      {hint && <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>{hint}</span>}
    </label>
  );
}

function StorageOption({
  checked,
  disabled,
  title,
  path,
  note,
  onSelect,
}: {
  checked: boolean;
  disabled: boolean;
  title: string;
  path: string;
  note: string;
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
        {!disabled ? <span className="tag plain">可写</span> : <span className="tag plain">不可写</span>}
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