import { useEffect, useMemo, useState } from "react";
import { api, type McBackup, type McInstance, type McJavaProbe, type McModFile, type McModHit } from "../api";
import { requestDeveloperPassword, isElevationRequired } from "../elevation";
import { refreshMcFeature, useMcFeature } from "../mcFeature";
import { toast } from "../store";
import { errMsg } from "../utils";

/// MC 实例的各块面板。
///
/// 这些以前是弹窗（`terminal-overlay` 里的卡片）。实例有了自己的二级页面之后，
/// 它们就只是页面里的一块：同样的接口、同样的动作，只是不再盖住列表。
/// 破坏性动作仍然在这里逐个确认——面板不是沙箱。

/** 提权失败（需要开发者密码）时就地弹框并重试一次。 */
async function elevated<T>(call: () => Promise<T>): Promise<T> {
  try {
    return await call();
  } catch (e) {
    if (!isElevationRequired(e)) throw e;
    if (!(await requestDeveloperPassword())) throw e;
    return await call();
  }
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** 备份：停机快照、列表、恢复（恢复前自动再拍一张）、删除。 */
export function McBackupsPanel({ instance }: { instance: McInstance }) {
  const [backups, setBackups] = useState<McBackup[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setBackups((await api.mc.backups(instance.id)).backups);
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
  }, [instance.id]);

  const create = async () => {
    setBusy(true);
    try {
      const backup = await elevated(() => api.mc.createBackup(instance.id));
      toast("success", `已备份：${backup.name}`);
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const restore = async (name: string) => {
    if (!window.confirm(`恢复 ${name}？当前世界会先自动备份一份。`)) return;
    setBusy(true);
    try {
      const answer = await elevated(() => api.mc.restoreBackup(instance.id, name));
      toast("success", `已恢复；恢复前的世界保存在 ${answer.safety}`);
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (name: string) => {
    if (!window.confirm(`删除备份 ${name}？`)) return;
    setBusy(true);
    try {
      await elevated(() => api.mc.deleteBackup(instance.id, name));
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const running = instance.state === "RUNNING";

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">世界备份</span>
        <span className="comp-value">
          <button className="btn btn-text" disabled={busy || running} onClick={() => void create()}>
            立即备份
          </button>
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        备份只在停服时拍，避免半个世界的 zip。恢复前会自动再备份当前世界。
        {running && " 先停服。"}
      </p>
      {error && <div style={{ color: "var(--monet-error)", padding: "6px 8px" }}>{error}</div>}
      <div style={{ maxHeight: 380, overflow: "auto", padding: "4px 8px 8px" }}>
        {backups.length === 0 ? (
          <div className="dim" style={{ fontSize: 13 }}>还没有备份。</div>
        ) : backups.map((backup) => (
          <div
            key={backup.name}
            style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", borderTop: "1px solid var(--monet-outline-variant)" }}
          >
            <span style={{ flex: 1, fontSize: 13 }}>{backup.name}</span>
            <span className="dim" style={{ fontSize: 12 }}>{formatBytes(backup.bytes)}</span>
            <button className="btn btn-text" disabled={busy || running} onClick={() => void restore(backup.name)}>
              恢复
            </button>
            <button className="btn btn-text danger" disabled={busy} onClick={() => void remove(backup.name)}>
              删除
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

/// `server.properties` 的受管字段，按界面上出现的顺序。
/// 左边是文件里的键（不能翻译），右边是给人看的名字与一句解释。
const PROPERTY_FIELDS: { key: string; label: string; hint: string }[] = [
  { key: "motd", label: "服务器标语（MOTD）", hint: "多人列表里显示在名字下面的一行字，可以用 § 颜色代码。" },
  { key: "max-players", label: "最大玩家数", hint: "同时在线的上限。" },
  { key: "online-mode", label: "正版验证", hint: "开启时只允许 Mojang 账号登录；关闭＝离线模式，任何名字都能进，只适合内网或自己人。" },
  { key: "white-list", label: "白名单", hint: "只允许白名单里的玩家进入。" },
  { key: "enforce-whitelist", label: "强制白名单", hint: "白名单变动后立刻踢掉不在名单里的人。" },
  { key: "difficulty", label: "游戏难度", hint: "和平不刷怪、不掉血；困难会饿死。" },
  { key: "gamemode", label: "默认游戏模式", hint: "新玩家第一次进来的模式。" },
  { key: "pvp", label: "玩家互相伤害（PvP）", hint: "关掉后玩家之间打不掉血。" },
  { key: "allow-flight", label: "允许飞行", hint: "关掉时，用飞行类插件或外挂会被服务端踢掉。" },
  { key: "spawn-protection", label: "出生点保护半径", hint: "以方块计，0 表示关闭；只挡没有 OP 的玩家。" },
  { key: "view-distance", label: "视距（区块）", hint: "客户端能看到多远，越大越吃 CPU 与带宽。" },
  { key: "simulation-distance", label: "模拟距离（区块）", hint: "超出这个距离的区块不再刷怪、跑红石。" },
];

const BOOL_LABEL: Record<string, string> = { true: "开启", false: "关闭" };
const DIFFICULTY_LABEL: Record<string, string> = {
  peaceful: "和平",
  easy: "简单",
  normal: "普通",
  hard: "困难",
};
const GAMEMODE_LABEL: Record<string, string> = {
  survival: "生存",
  creative: "创造",
  adventure: "冒险",
  spectator: "旁观",
};

const BOOL_FIELDS = new Set(["online-mode", "white-list", "enforce-whitelist", "pvp", "allow-flight"]);
const NUMBER_FIELDS = new Set(["max-players", "spawn-protection", "view-distance", "simulation-distance"]);
const EDITABLE = PROPERTY_FIELDS.map((field) => field.key);

/// 一个受管字段的输入控件；基础设置与高级设置共用同一套渲染。
function PropertyInput({
  field,
  value,
  onChange,
}: {
  field: { key: string; label: string; hint: string };
  value: string | undefined;
  onChange: (key: string, value: string) => void;
}) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
      <span>{field.label}</span>
      {BOOL_FIELDS.has(field.key) ? (
        <select className="input" value={value ?? "false"} onChange={(e) => onChange(field.key, e.target.value)}>
          <option value="true">{BOOL_LABEL.true}</option>
          <option value="false">{BOOL_LABEL.false}</option>
        </select>
      ) : field.key === "difficulty" ? (
        <select className="input" value={value ?? "normal"} onChange={(e) => onChange(field.key, e.target.value)}>
          {Object.entries(DIFFICULTY_LABEL).map(([option, label]) => (
            <option key={option} value={option}>{label}</option>
          ))}
        </select>
      ) : field.key === "gamemode" ? (
        <select className="input" value={value ?? "survival"} onChange={(e) => onChange(field.key, e.target.value)}>
          {Object.entries(GAMEMODE_LABEL).map(([option, label]) => (
            <option key={option} value={option}>{label}</option>
          ))}
        </select>
      ) : (
        <input
          className="input"
          type={NUMBER_FIELDS.has(field.key) ? "number" : "text"}
          value={value ?? ""}
          onChange={(e) => onChange(field.key, e.target.value)}
        />
      )}
      <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>{field.hint}</span>
    </label>
  );
}

/** `server.properties` 的受管字段；未知键和注释留在文件里，但这里不编辑。 */
export function McPropertiesPanel({ instance }: { instance: McInstance }) {
  const [values, setValues] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void api.mc.properties(instance.id)
      .then((answer) => setValues(answer.properties))
      .catch((e) => setError(errMsg(e)));
  }, [instance.id]);

  const save = async () => {
    const body: Record<string, string> = {};
    for (const key of EDITABLE) {
      if (values[key] !== undefined) body[key] = String(values[key]);
    }
    setBusy(true);
    try {
      await elevated(() => api.mc.saveProperties(instance.id, body));
      toast("success", "已保存；下次启动生效");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const set = (key: string, value: string) => setValues({ ...values, [key]: value });

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">服务端设置</span>
        <span className="comp-value mono" style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
          server.properties
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        只列面板管得住的这 {EDITABLE.length} 项；文件里的注释与其他键原样保留。改动下次启动生效。
      </p>
      {error && <div style={{ color: "var(--monet-error)", padding: "6px 8px" }}>{error}</div>}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 10, padding: "10px 8px" }}>
        {PROPERTY_FIELDS.map((field) => (
          <PropertyInput key={field.key} field={field} value={values[field.key]} onChange={set} />
        ))}
      </div>
      <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", padding: "0 8px 8px" }}>
        <button className="btn btn-raised" disabled={busy} onClick={() => void save()}>保存设置</button>
      </div>
    </div>
  );
}

/** 每 256 MB 一档，和创建向导的 step 一致。 */
const MEMORY_STEP_MB = 256;

/// 这个核心至少需要的堆。模组核心比原版吃得多；推荐值只会往上抬，不会低于它。
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

function formatMegabytes(mb: number): string {
  if (!Number.isFinite(mb) || mb <= 0) return "—";
  return mb >= 1024 ? `${(mb / 1024).toFixed(mb % 1024 === 0 ? 0 : 1)} GB` : `${mb} MB`;
}

function javaSourceLabel(source: string): string {
  switch (source) {
    case "panel":
      return "面板自带 JVM";
    case "managed":
      return "面板托管 JRE";
    case "installed":
      return "系统 JDK";
    case "configured":
      return "配置的 Java（mc.java_path）";
    default:
      return "未知来源";
  }
}

function javaOptionLabel(option: McJavaProbe): string {
  const version = option.major > 0 ? `Java ${option.major}` : "Java（版本未知）";
  const parts = option.path.split(/[\\/]/).filter(Boolean);
  const home = parts.length >= 3 ? parts[parts.length - 3] : option.path;
  // 托管 JRE 的目录名就是主版本，没必要再写一次；面板/系统 JDK 用 JDK 目录名区分。
  const suffix = option.source === "managed" ? "" : ` · ${home}`;
  return `${javaSourceLabel(option.source)} · ${version}${suffix}`;
}

/** 高级设置：默认不用动、需要时才来找的项——内存、端口、RCON 与 Java。
 *
 *  这些项改错会让人连不上服、让面板查不到玩家，或让服起在一个不合适的
 *  JDK 上，所以不放在日常设置里。内存、端口与 Java 写 `instance.json`（启动时
 *  读取），只能在停服时改；RCON 开关写 `server.properties`，下次启动生效。
 *  内存以手动为准，「推荐值」只填一次、不持续跟踪：堆随宿主负载漂移会让服主
 *  很难复现上次为什么卡。 */
export function McAdvancedPanel({
  instance,
  onChange,
}: {
  instance: McInstance;
  onChange: (next: McInstance) => void;
}) {
  const feature = useMcFeature();
  const minMb = feature?.limits?.minMemoryMb ?? 256;
  const maxMb = feature?.limits?.maxMemoryMb ?? 8192;

  const [port, setPort] = useState(instance.port);
  const [rconPort, setRconPort] = useState(instance.rconPort);
  const [memory, setMemory] = useState(String(instance.memoryMb));
  const [javaSelected, setJavaSelected] = useState<string | null>(instance.javaPath ?? null);
  const [javaOptions, setJavaOptions] = useState<McJavaProbe[]>([]);
  const [javaEffective, setJavaEffective] = useState<McJavaProbe | null>(null);
  const [javaError, setJavaError] = useState<string | null>(null);
  const [rconEnabled, setRconEnabled] = useState("false");
  const [rconOriginal, setRconOriginal] = useState("false");
  const [instances, setInstances] = useState<McInstance[]>([]);
  const [availableBytes, setAvailableBytes] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [rconBusy, setRconBusy] = useState(false);
  const [note, setNote] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const running = instance.state === "RUNNING" || instance.state === "STARTING" || instance.state === "STOPPING";

  // 直接打开实例页时，共享快照可能还没拉过；没有它就只能用默认上限。
  useEffect(() => {
    if (!feature) void refreshMcFeature();
  }, [feature]);

  useEffect(() => {
    setPort(instance.port);
    setRconPort(instance.rconPort);
    setMemory(String(instance.memoryMb));
    setJavaSelected(instance.javaPath ?? null);
  }, [instance.port, instance.rconPort, instance.memoryMb, instance.javaPath]);

  useEffect(() => {
    let cancelled = false;
    void api.mc.instanceJava(instance.id)
      .then((answer) => {
        if (!cancelled) {
          setJavaSelected(answer.selected ?? null);
          setJavaOptions(answer.options);
          setJavaEffective(answer.effective);
          setJavaError(null);
        }
      })
      .catch((e) => {
        if (!cancelled) setJavaError(errMsg(e));
      });
    void api.mc.properties(instance.id)
      .then((answer) => {
        if (!cancelled) {
          const current = answer.properties["enable-rcon"] === "true" ? "true" : "false";
          setRconEnabled(current);
          setRconOriginal(current);
          setError(null);
        }
      })
      .catch((e) => {
        if (!cancelled) setError(errMsg(e));
      });
    void api.mc.instances()
      .then((answer) => {
        if (!cancelled) setInstances(answer.instances);
      })
      .catch(() => undefined);
    void api.systemStats(0)
      .then((stats) => {
        if (!cancelled) setAvailableBytes(stats.snapshot?.memory.availableBytes ?? null);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [instance.id]);

  const memoryMb = Number(memory);
  const memoryValid = Number.isInteger(memoryMb) && memoryMb >= minMb && memoryMb <= maxMb;
  const heap = memoryValid ? `-Xms${Math.min(512, memoryMb)}M -Xmx${memoryMb}M` : null;
  const floorMb = memoryFloor(instance.core);
  const runningCount = instances.filter(
    (entry) => entry.state === "RUNNING" || entry.state === "STARTING",
  ).length;
  const committedMb = instances.reduce((sum, entry) => sum + entry.memoryMb, 0);

  const recommend = (): number => {
    if (availableBytes == null) {
      return Math.min(maxMb, Math.max(floorMb, 2048));
    }
    // 给这台服留 75% 的可分配份额，其余留给面板、页缓存和下一台服。
    const share = availableBytes / Math.max(1, runningCount + 1);
    const target = Math.floor((share * 0.75) / (MEMORY_STEP_MB * 1024 * 1024)) * MEMORY_STEP_MB;
    return Math.min(maxMb, Math.max(floorMb, target));
  };

  const applyRecommendation = () => {
    const value = recommend();
    setMemory(String(value));
    const availableMb = availableBytes == null ? null : Math.floor(availableBytes / (1024 * 1024));
    setNote(
      `已填入 ${value} MB：` +
        (availableMb == null
          ? `读不到宿主内存，按 ${instance.core} 的常见值填`
          : `宿主可用 ${formatMegabytes(availableMb)}，运行中 ${runningCount} 个实例，按 75% 份额估算`) +
        `；${instance.core} 的下限是 ${floorMb} MB。保存后下次启动生效。`,
    );
  };

  const javaChanged = (javaSelected ?? null) !== (instance.javaPath ?? null);
  const dirty = port !== instance.port || rconPort !== instance.rconPort
    || (memoryValid && memoryMb !== instance.memoryMb) || javaChanged;

  const saveInstance = async () => {
    if (!memoryValid) {
      toast("error", `内存必须在 ${minMb}–${maxMb} MB 之间`);
      return;
    }
    setBusy(true);
    try {
      const next = await elevated(() => api.mc.update(instance.id, {
        port, rconPort, memoryMb, javaPath: javaSelected,
      }));
      onChange(next);
      setJavaSelected(next.javaPath ?? null);
      void api.mc.instanceJava(instance.id)
        .then((answer) => {
          setJavaOptions(answer.options);
          setJavaEffective(answer.effective);
        })
        .catch(() => undefined);
      toast("success", "已保存；下次启动生效");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const saveRcon = async () => {
    setRconBusy(true);
    try {
      await elevated(() => api.mc.saveProperties(instance.id, { "enable-rcon": rconEnabled }));
      setRconOriginal(rconEnabled);
      toast("success", "已保存；下次启动生效");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setRconBusy(false);
    }
  };

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">高级设置</span>
        <span className="comp-value mono" style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
          instance.json · server.properties
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        默认不用动、需要时才来找的项：内存、端口、RCON 与 Java。端口不是「面板映射」——服务端
        直接把游戏端口绑在宿主/容器网卡上，改完记得在防火墙放行；内存、端口与 Java 要停服才能改，
        RCON 开关下次启动生效。
      </p>

      <div className="comp-row" style={{ paddingTop: 6 }}>
        <span className="comp-label">资源</span>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 10, padding: "4px 8px 8px" }}>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>内存上限（MB）</span>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <input
              className="input"
              type="number"
              min={minMb}
              max={maxMb}
              step={MEMORY_STEP_MB}
              value={memory}
              disabled={running || busy}
              onChange={(e) => setMemory(e.target.value)}
              style={{ flex: 1, minWidth: 90 }}
            />
            <button className="btn btn-text" disabled={running || busy} onClick={applyRecommendation}>
              推荐值
            </button>
          </div>
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            范围 {minMb}–{maxMb} MB（部署上限）。
            {heap ? ` 启动参数 ${heap}。` : " 请输入范围内的整数。"}
          </span>
        </label>
        <div style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>宿主内存</span>
          <span className="mono">
            {availableBytes == null ? "未知" : `${formatMegabytes(Math.floor(availableBytes / (1024 * 1024)))} 可用`}
          </span>
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            所有实例的堆上限合计 {formatMegabytes(committedMb)}；合计超过可用内存时，服之间会互相挤。
          </span>
        </div>
      </div>
      {note && (
        <div style={{ padding: "0 8px 8px", fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
          {note}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 10, padding: "0 8px 8px" }}>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>Java 运行时</span>
          <select
            className="input"
            value={javaSelected ?? ""}
            disabled={running || busy}
            onChange={(e) => setJavaSelected(e.target.value || null)}
          >
            <option value="">自动（按 MC 版本要求）</option>
            {javaOptions.map((option) => (
              <option key={option.path} value={option.path}>{javaOptionLabel(option)}</option>
            ))}
            {javaSelected && !javaOptions.some((option) => option.path === javaSelected) && (
              <option value={javaSelected}>当前选择（已不可用）· {javaSelected}</option>
            )}
          </select>
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            {javaEffective
              ? `当前使用：${javaEffective.path}（${javaSourceLabel(javaEffective.source)}，Java ${
                  javaEffective.major > 0 ? javaEffective.major : "?"
                }）${javaEffective.ok ? "" : `；${javaEffective.error ?? "无法运行"}`}`
              : "自动：面板 JVM 满足版本就用它，不够时用面板托管或系统里的 JDK。"}
            ；停服后才能改。
          </span>
        </label>
      </div>
      {javaError && (
        <div style={{ padding: "0 8px 8px", fontSize: 12, color: "var(--monet-error)" }}>{javaError}</div>
      )}

      <div className="comp-row" style={{ paddingTop: 4 }}>
        <span className="comp-label">网络</span>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 10, padding: "4px 8px 8px" }}>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>游戏端口</span>
          <input
            className="input"
            type="number"
            min={1024}
            max={65535}
            value={port}
            disabled={running || busy}
            onChange={(e) => setPort(Number(e.target.value) || 0)}
          />
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            客户端连这个端口；RCON 端口给面板用，不能相同。
          </span>
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>RCON 端口</span>
          <input
            className="input"
            type="number"
            min={1024}
            max={65535}
            value={rconPort}
            disabled={running || busy}
            onChange={(e) => setRconPort(Number(e.target.value) || 0)}
          />
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            只给面板与本地工具用，不要发布到公网。
          </span>
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 12 }}>
          <span>RCON</span>
          <select
            className="input"
            value={rconEnabled}
            disabled={rconBusy}
            onChange={(e) => setRconEnabled(e.target.value)}
          >
            <option value="true">开启</option>
            <option value="false">关闭</option>
          </select>
          <span style={{ color: "var(--monet-on-surface-variant)", lineHeight: 1.5 }}>
            面板用它执行命令、查在线玩家与导入名单；默认关闭。
          </span>
        </label>
      </div>
      {error && <div style={{ color: "var(--monet-error)", padding: "0 8px 8px", fontSize: 12 }}>{error}</div>}
      <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", padding: "0 8px 8px" }}>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", alignSelf: "center" }}>
          RCON 开关下次启动生效
        </span>
        <button
          className="btn btn-text"
          disabled={rconBusy || rconEnabled === rconOriginal}
          onClick={() => void saveRcon()}
        >
          保存 RCON 开关
        </button>
      </div>
      <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", padding: "0 8px 8px" }}>
        {running && (
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", alignSelf: "center" }}>
            停服后可改
          </span>
        )}
        <button
          className="btn btn-raised"
          disabled={busy || running || !dirty || !memoryValid}
          onClick={() => void saveInstance()}
        >
          保存端口、内存与 Java
        </button>
      </div>

      <div style={{ borderTop: "1px solid var(--monet-outline-variant)", padding: "6px 8px 8px", fontSize: 11, color: "var(--monet-on-surface-variant)", lineHeight: 1.7, wordBreak: "break-all" }}>
        <div>
          服务器目录：<span className="mono">{instance.dir}</span>
          {instance.storage === "external" ? "（专用目录，独立挂载）" : "（面板数据目录）"}
        </div>
        <div>
          Java：<span className="mono">{feature?.java.path || "—"}</span>
          {feature?.java.version ? ` (${feature.java.version})` : ""}
        </div>
      </div>
    </div>
  );
}

/** 服务端输出：`logs/latest.log` 的尾巴，2 秒一跳。 */
function LogTail({ instance, height }: { instance: McInstance; height: number }) {
  const [lines, setLines] = useState<string[]>([]);

  useEffect(() => {
    let alive = true;
    const load = async () => {
      try {
        const answer = await api.mc.logs(instance.id);
        if (alive) setLines(answer.lines);
      } catch {
        // 未就绪/没日志：下次轮询再说
      }
    };
    void load();
    const timer = window.setInterval(() => void load(), 2000);
    return () => {
      alive = false;
      window.clearInterval(timer);
    };
  }, [instance.id]);

  return (
    <pre style={{ height, overflow: "auto", fontSize: 12, margin: 0, whiteSpace: "pre-wrap", padding: 8 }}>
      {lines.length === 0 ? "（等待服务端输出…）" : lines.join("\n")}
    </pre>
  );
}

/// 控制台的两种形态：Linux 是 PTY（xterm 窗口），Windows 没有 PTY，只有
/// 「日志 + 一行命令」。两种都能看到输出，区别只在于命令怎么送进去。
export function McConsolePanel({
  instance,
  onOpenTerminal,
}: {
  instance: McInstance;
  onOpenTerminal: () => void;
}) {
  const [pipe, setPipe] = useState(false);
  const [line, setLine] = useState("");
  const [busy, setBusy] = useState(false);
  const feature = useMcFeature();
  const platformPipe = feature?.console === "pipe";

  const open = async () => {
    setBusy(true);
    try {
      const answer = await elevated(() => api.mc.consoleTicket(instance.id));
      if (answer.mode === "pipe") setPipe(true);
      else onOpenTerminal();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const send = async () => {
    const command = line.trim();
    if (!command) return;
    setBusy(true);
    try {
      await elevated(() => api.mc.consoleInput(instance.id, command));
      setLine("");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card" style={{ padding: 0, overflow: "hidden" }}>
      <div className="comp-row" style={{ padding: "8px 8px 0" }}>
        <span className="comp-label">控制台</span>
        <span className="comp-value">
          <button className="btn btn-text" disabled={busy || !instance.consoleAvailable} onClick={() => void open()}>
            {pipe ? "重新连接" : platformPipe ? "打开控制台（日志 + 命令）" : "打开交互终端"}
          </button>
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px", lineHeight: 1.6 }}>
        {instance.consoleAvailable
          ? pipe
            ? "这台服务端没有 PTY：命令从下面的输入框送进去，输出仍然在这里。"
            : platformPipe
              ? "本平台没有 PTY：打开后是日志 + 命令输入框，不是交互终端。"
              : "交互终端走 Web Shell（PTY）；这一块只是输出预览。"
          : "服务端起来之后才有控制台。"}
      </p>
      <LogTail instance={instance} height={420} />
      {pipe && (
        <div style={{ display: "flex", gap: 6, padding: 8, borderTop: "1px solid var(--monet-outline-variant)" }}>          <input
            className="input"
            value={line}
            placeholder="输入服务器命令，如 say hi / list / stop"
            onChange={(e) => setLine(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") void send(); }}
            style={{ flex: 1 }}
          />
          <button className="btn btn-raised" disabled={busy || !line.trim()} onClick={() => void send()}>
            发送
          </button>
        </div>
      )}
    </div>
  );
}

/** 日志页：同一份 latest.log，给一个更高更宽的位置慢慢看。 */
export function McLogsPanel({ instance }: { instance: McInstance }) {
  return (
    <div className="card" style={{ padding: 0, overflow: "hidden" }}>
      <div className="comp-row" style={{ padding: "8px 8px 0" }}>
        <span className="comp-label">日志</span>
        <span className="comp-value mono" style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
          logs/latest.log
        </span>
      </div>
      <LogTail instance={instance} height={560} />
    </div>
  );
}

/** RCON 玩家列表与四个管理动作。 */
export function McPlayersPanel({ instance }: { instance: McInstance }) {
  const [online, setOnline] = useState(0);
  const [max, setMax] = useState(0);
  const [names, setNames] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [listsNote, setListsNote] = useState<string | null>(null);

  const importLists = async (file: File) => {
    setBusy(true);
    setListsNote(null);
    try {
      const answer = await elevated(() => api.mc.importPlayerLists(instance.id, file));
      const parts = Object.entries(answer.files).map(([name, count]) => `${name} ${count} 条`);
      setListsNote(`已导入：${parts.join("、")}`);
      toast("success", "玩家名单已导入；重启服务器后生效");
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const load = async () => {
    try {
      const answer = await api.mc.players(instance.id);
      setOnline(answer.online);
      setMax(answer.max);
      setNames(answer.names);
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
  }, [instance.id]);

  const act = async (name: string, action: "kick" | "op" | "deop" | "ban") => {
    setBusy(true);
    try {
      const answer = await elevated(() => api.mc.playerAction(instance.id, name, action));
      toast("success", answer.answer || "已执行");
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">在线玩家</span>
        <span className="comp-value">
          <span className="dim">{online} / {max}</span>
          <button className="btn btn-text" onClick={() => void load()}>刷新</button>
        </span>
      </div>
      {instance.state !== "RUNNING" && (
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0" }}>
          服务端没在运行，这里读不到玩家。
        </p>
      )}
      {error && <div style={{ color: "var(--monet-error)", padding: "6px 8px", fontSize: 13 }}>{error}</div>}
      <div style={{ maxHeight: 380, overflow: "auto", padding: "4px 8px 8px" }}>
        {names.length === 0 ? (
          <div className="dim" style={{ fontSize: 13 }}>没有在线玩家。</div>
        ) : names.map((name) => (
          <div key={name} style={{ display: "flex", gap: 6, alignItems: "center", padding: "4px 0" }}>
            <span style={{ flex: 1 }}>{name}</span>
            <button className="btn btn-text" disabled={busy} onClick={() => void act(name, "kick")}>踢出</button>
            <button className="btn btn-text" disabled={busy} onClick={() => void act(name, "op")}>给 OP</button>
            <button className="btn btn-text" disabled={busy} onClick={() => void act(name, "deop")}>收回 OP</button>
            <button className="btn btn-text danger" disabled={busy} onClick={() => void act(name, "ban")}>封禁</button>
          </div>
        ))}
      </div>
      <div style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 8px 0", flexWrap: "wrap" }}>
        <label
          className={`btn btn-text${instance.state === "RUNNING" ? " disabled" : ""}`}
          style={{ cursor: instance.state === "RUNNING" ? "not-allowed" : "pointer" }}
        >
          导入名单 zip…
          <input
            type="file"
            accept=".zip"
            style={{ display: "none" }}
            disabled={busy || instance.state === "RUNNING"}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void importLists(file);
              e.target.value = "";
            }}
          />
        </label>
        <span className="dim" style={{ fontSize: 12 }}>
          whitelist.json / ops.json / banned-*.json，放在 zip 根上；停服才能导入
        </span>
      </div>
      {listsNote && <div style={{ padding: "4px 8px 0", fontSize: 12 }}>{listsNote}</div>}
      <p className="dim" style={{ fontSize: 12, margin: 0, padding: "0 8px 8px" }}>
        在「服务端设置 → 高级设置」里开启 RCON；默认关闭，改了下次启动生效。
      </p>
    </div>
  );
}

/** 世界导入/导出（zip）。两者都要求先停服；大世界走浏览器流式下载与后台解压。 */
export function McWorldPanel({ instance }: { instance: McInstance }) {
  const [busy, setBusy] = useState(false);
  const [phase, setPhase] = useState<"idle" | "uploading" | "working">("idle");
  const [sent, setSent] = useState(0);
  const [total, setTotal] = useState(0);
  const [taskNote, setTaskNote] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const download = async () => {
    setBusy(true);
    setError(null);
    setNote(null);
    try {
      // 下载由浏览器发起（cookie 会跟上），所以提权要先单独过一遍。
      await elevated(() => api.mc.authorizeWorld(instance.id));
      const link = document.createElement("a");
      link.href = api.mc.exportWorldUrl(instance.id);
      link.download = `${instance.id}-${instance.levelName}.zip`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setNote("下载已开始：进度看浏览器的下载栏。大世界是流式传输，不会占用页面内存。");
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const upload = async (file: File) => {
    if (!window.confirm(`用 ${file.name} 覆盖当前世界？当前世界会先自动备份。`)) return;
    setBusy(true);
    setError(null);
    setNote(null);
    setTaskNote(null);
    setPhase("uploading");
    setSent(0);
    setTotal(file.size);
    try {
      await elevated(() => api.mc.authorizeWorld(instance.id));
      const { taskId } = await api.mc.importWorld(instance.id, file, (now, all) => {
        setSent(now);
        setTotal(all);
      });
      // 上传完成，解压与备份在后台任务里；轮询文案，别让页面看起来卡死。
      setPhase("working");
      for (let i = 0; i < 3600; i += 1) {
        const task = await api.task(taskId);
        if (task.message) setTaskNote(task.message);
        if (task.state === "done") {
          const safety = task.result?.safety;
          setNote(`已导入${safety ? `；导入前的世界备份：${safety}` : "（原本没有世界）"}`);
          break;
        }
        if (task.state === "failed" || task.state === "cancelled") {
          setError(task.error || "导入失败");
          break;
        }
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
      setPhase("idle");
      setTaskNote(null);
    }
  };

  const running = instance.state === "RUNNING";
  const percent = total > 0 ? Math.min(100, Math.round((sent / total) * 100)) : 0;

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">世界</span>
        <span className="comp-value mono" style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
          {instance.levelName}
        </span>
      </div>
      {running ? (
        <p style={{ color: "var(--monet-error)", padding: "6px 8px", fontSize: 13 }}>先停服，再导入或导出世界。</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 10, padding: "10px 8px" }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button className="btn btn-raised" disabled={busy} onClick={() => void download()}>
              导出世界（zip）
            </button>
            <label className="btn btn-raised" style={{ cursor: busy ? "default" : "pointer" }}>
              导入世界…
              <input
                type="file"
                accept=".zip"
                style={{ display: "none" }}
                disabled={busy}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) void upload(file);
                  e.target.value = "";
                }}
              />
            </label>
          </div>
          {phase !== "idle" && (
            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <div
                style={{
                  height: 6,
                  borderRadius: 3,
                  background: "var(--monet-surface-variant)",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    width: phase === "uploading" ? `${percent}%` : "100%",
                    height: "100%",
                    background: "var(--monet-primary)",
                    transition: "width 0.2s",
                  }}
                />
              </div>
              <span className="dim" style={{ fontSize: 12 }}>
                {phase === "uploading"
                  ? `上传中 ${percent}%（${formatBytes(sent)} / ${formatBytes(total)}）`
                  : taskNote || "服务器正在解压…"}
              </span>
            </div>
          )}
          <p className="dim" style={{ fontSize: 12, margin: 0, lineHeight: 1.6 }}>
            导入会先备份当前世界，再整体替换；不会合并两个世界。zip 上限 128 GB，几十 GB 的世界也能导入（需要足够的磁盘空间——不可压缩的世界首次导入约 2 倍，覆盖旧世界约 4 倍）。
            导入分两步：先把 zip 传完（上面的进度条），再由后台解压——页面关掉任务也会继续，但建议等它结束。
            zip 里的目录可以叫 <span className="mono">{instance.levelName}/…</span>，也可以直接是世界内容
            （平铺或 <span className="mono">region/…</span> 这种层级）。
            <b>顶层目录名和上面的世界目录名不一致会被拒绝</b>——否则服务器找不到
            <span className="mono">level.dat</span>，会当成新世界。改世界目录名或重新打包即可。
            <br />
            这里<b>只搬存档</b>：服务端程序、插件/模组、白名单与 OP 名单都不在 zip 里。
            旧服要带过来时，版本选成和旧服一致（服务端程序走官方下载），
            名单用「玩家」页的 op/deop 或控制台 <span className="mono">whitelist add</span> 重建。
          </p>
        </div>
      )}
      {note && <div style={{ padding: "0 8px 8px", fontSize: 13 }}>{note}</div>}
      {error && <div style={{ color: "var(--monet-error)", padding: "0 8px 8px", fontSize: 13 }}>{error}</div>}
    </div>
  );
}

export function McFilesPanel({ instance }: { instance: McInstance }) {
  const kind: "mods" | "plugins" = instance.core === "fabric" ? "mods" : "plugins";
  const noun = kind === "mods" ? "模组" : "插件";
  const [files, setFiles] = useState<McModFile[]>([]);
  const [loader, setLoader] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<McModHit[]>([]);
  const [totalHits, setTotalHits] = useState(0);
  const [relaxed, setRelaxed] = useState(false);
  const [searching, setSearching] = useState(false);

  const load = async () => {
    try {
      const answer = await api.mc.files(instance.id, kind);
      setFiles(answer.files);
      setLoader(answer.folder.loader ?? null);
      setSelected((current) => current.filter((name) => answer.files.some((f) => f.name === name)));
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
  }, [instance.id, kind]);

  const visible = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return files;
    return files.filter((file) =>
      [file.name, file.title ?? "", file.versionNumber ?? ""]
        .some((text) => text.toLowerCase().includes(q)));
  }, [files, filter]);

  const enabledCount = files.filter((file) => file.enabled).length;

  const toggle = async (file: McModFile) => {
    setBusy(true);
    try {
      await elevated(() => api.mc.toggleFile(instance.id, kind, file.name, !file.enabled));
      await load();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (file: McModFile) => {
    if (!window.confirm(`删除 ${file.name}？文件会被直接删掉，不进备份。`)) return;
    setBusy(true);
    try {
      await elevated(() => api.mc.deleteFile(instance.id, kind, file.name));
      await load();
      toast("success", "已删除");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const bulkToggle = async (enabled: boolean) => {
    const targets = files.filter((file) => selected.includes(file.name) && file.enabled !== enabled);
    if (targets.length === 0) return;
    setBusy(true);
    let done = 0;
    try {
      for (const file of targets) {
        await elevated(() => api.mc.toggleFile(instance.id, kind, file.name, enabled));
        done++;
      }
      toast("success", `已${enabled ? "启用" : "停用"} ${done} 个${noun}`);
      setSelected([]);
      await load();
    } catch (e) {
      toast("error", errMsg(e));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const bulkDelete = async () => {
    const targets = files.filter((file) => selected.includes(file.name));
    if (targets.length === 0) return;
    if (!window.confirm(`删除选中的 ${targets.length} 个${noun}？文件会直接删掉，不进备份。`)) return;
    setBusy(true);
    try {
      for (const file of targets) {
        await elevated(() => api.mc.deleteFile(instance.id, kind, file.name));
      }
      toast("success", `已删除 ${targets.length} 个${noun}`);
      setSelected([]);
      await load();
    } catch (e) {
      toast("error", errMsg(e));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const upload = async (file: File) => {
    setBusy(true);
    try {
      await elevated(() => api.mc.uploadFile(instance.id, kind, file));
      await load();
      toast("success", `已上传 ${file.name}`);
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const search = async (more = false) => {
    setSearching(true);
    const offset = more ? hits.length : 0;
    try {
      const answer = await api.mc.modrinthSearch(
        instance.core, instance.version, query, offset, 20,
      );
      setHits(more ? [...hits, ...answer.hits] : answer.hits);
      setTotalHits(answer.totalHits);
      setRelaxed(answer.relaxed);
      setError(null);
    } catch (e) {
      setError(errMsg(e));
      if (!more) setHits([]);
    } finally {
      setSearching(false);
    }
  };

  const install = async (hit: McModHit) => {
    setBusy(true);
    try {
      const answer = await elevated(() => api.mc.modrinthInstall(instance.id, hit.projectId));
      await load();
      toast("success", `已安装 ${answer.name}`);
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  if (instance.core === "vanilla") {
    return (
      <div className="hint info">
        <span style={{ flex: 1 }}>
          原版服务端不加载插件或模组。要装{kind === "mods" ? "模组" : "插件"}，
          请新建一个 Paper/Folia/Purpur/Fabric 实例。
        </span>
      </div>
    );
  }

  const allVisibleSelected = visible.length > 0 && visible.every((file) => selected.includes(file.name));

  return (
    <div style={{ display: "grid", gap: 12 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <b>{noun}目录</b>
        <code style={{ fontSize: 12 }}>{kind}/</code>
        {loader && <span className="tag plain">loader: {loader}</span>}
        <span className="tag plain">{files.length} 个 · {enabledCount} 启用</span>
        <span className="dim" style={{ fontSize: 12 }}>
          停用 = 重命名为 `.jar.disabled`；启停、上传、安装都在下次启动时生效。
        </span>
        <label className="btn btn-text" style={{ cursor: busy ? "not-allowed" : "pointer", marginLeft: "auto" }}>
          上传 {noun}.jar
          <input
            type="file"
            accept=".jar"
            disabled={busy}
            style={{ display: "none" }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              e.target.value = "";
              if (file) void upload(file);
            }}
          />
        </label>
      </div>

      {error && (
        <div className="hint error">
          <span style={{ flex: 1 }}>{error}</span>
        </div>
      )}

      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <input
          className="input"
          style={{ flex: 1, minWidth: 200 }}
          placeholder={`在已装的${noun}里过滤（名字 / 标题 / 版本）`}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <label style={{ display: "flex", gap: 6, alignItems: "center", fontSize: 12 }}>
          <input
            type="checkbox"
            checked={allVisibleSelected}
            onChange={(e) => {
              setSelected(e.target.checked ? visible.map((file) => file.name) : []);
            }}
          />
          全选
        </label>
        <button
          className="btn btn-text"
          disabled={busy || selected.length === 0}
          onClick={() => void bulkToggle(true)}
        >
          启用选中
        </button>
        <button
          className="btn btn-text"
          disabled={busy || selected.length === 0}
          onClick={() => void bulkToggle(false)}
        >
          停用选中
        </button>
        <button
          className="btn btn-text danger"
          disabled={busy || selected.length === 0}
          onClick={() => void bulkDelete()}
        >
          删除选中
        </button>
        {selected.length > 0 && (
          <span className="dim" style={{ fontSize: 12 }}>已选 {selected.length} 个</span>
        )}
      </div>

      {files.length === 0 ? (
        <div className="dim" style={{ fontSize: 13 }}>目录是空的。</div>
      ) : visible.length === 0 ? (
        <div className="dim" style={{ fontSize: 13 }}>没有匹配的{ noun }。</div>
      ) : (
        <div className="card" style={{ padding: 0 }}>
          {visible.map((file) => (
            <div
              key={file.name}
              style={{
                display: "flex",
                gap: 8,
                alignItems: "center",
                padding: "8px 10px",
                borderBottom: "1px solid var(--monet-outline-variant)",
                flexWrap: "wrap",
              }}
            >
              <input
                type="checkbox"
                checked={selected.includes(file.name)}
                onChange={(e) => {
                  setSelected((current) => e.target.checked
                    ? [...current, file.name]
                    : current.filter((name) => name !== file.name));
                }}
              />
              <span style={{ display: "grid", gap: 2, flex: 1, minWidth: 180 }}>
                <span style={{ fontSize: 13, wordBreak: "break-all" }}>
                  {file.title ?? file.name}
                </span>
                <span className="dim" style={{ fontSize: 11, wordBreak: "break-all" }}>
                  {file.title ? `${file.name} · ` : ""}
                  {file.versionNumber ? `${file.versionNumber} · ` : ""}
                  {formatBytes(file.bytes)}
                </span>
              </span>
              {file.clientOnly && (
                <span className="tag plain" title="元数据自称纯客户端，服务端加载通常无效">
                  客户端专用
                </span>
              )}
              <span className={`tag ${file.enabled ? "running" : "plain"}`}>
                {file.enabled ? "已启用" : "已停用"}
              </span>
              <button className="btn btn-text" disabled={busy} onClick={() => void toggle(file)}>
                {file.enabled ? "停用" : "启用"}
              </button>
              <button className="btn btn-text danger" disabled={busy} onClick={() => void remove(file)}>
                删除
              </button>
            </div>
          ))}
        </div>
      )}

      <div style={{ display: "grid", gap: 8 }}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input
            className="input"
            style={{ flex: 1, minWidth: 220 }}
            value={query}
            placeholder={`在 Modrinth 搜索 ${noun}（中文词会先转成英文；兼容 ${instance.version} + ${loader ?? instance.core}）`}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") void search();
            }}
          />
          <button className="btn btn-raised" disabled={searching} onClick={() => void search()}>
            {searching ? "搜索中…" : "搜索"}
          </button>
        </div>

        {hits.length > 0 && (
          <>
            <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
              <span className="dim" style={{ fontSize: 12 }}>
                共 {totalHits.toLocaleString()} 个结果，已显示 {hits.length} 个
              </span>
              {relaxed && (
                <span className="tag plain" title="没有找到明确标注该游戏版本的条目，这里放宽了版本过滤；安装时仍会再校验">
                  已放宽版本过滤
                </span>
              )}
            </div>
            <div className="card" style={{ padding: 0 }}>
              {hits.map((hit) => (
                <div
                  key={hit.projectId}
                  style={{
                    display: "grid",
                    gap: 2,
                    padding: "8px 10px",
                    borderBottom: "1px solid var(--monet-outline-variant)",
                  }}
                >
                  <div style={{ display: "flex", gap: 8, alignItems: "baseline", flexWrap: "wrap" }}>
                    <b style={{ fontSize: 13 }}>{hit.title}</b>
                    <span className="dim" style={{ fontSize: 11 }}>
                      {hit.author} · {hit.downloads.toLocaleString()} 次下载
                    </span>
                    <button
                      className="btn btn-text"
                      style={{ marginLeft: "auto" }}
                      disabled={busy}
                      onClick={() => void install(hit)}
                    >
                      安装
                    </button>
                  </div>
                  <span className="dim" style={{ fontSize: 12, lineHeight: 1.5 }}>{hit.description}</span>
                </div>
              ))}
            </div>
            {hits.length < totalHits && (
              <button
                className="btn btn-text"
                disabled={searching}
                onClick={() => void search(true)}
              >
                {searching ? "加载中…" : "加载更多"}
              </button>
            )}
          </>
        )}
        {hits.length === 0 && !searching && (
          <span className="dim" style={{ fontSize: 12 }}>
            安装的是最新兼容版本，安装前会校验 Modrinth 公布的 SHA-512。
          </span>
        )}
      </div>
    </div>
  );
}
