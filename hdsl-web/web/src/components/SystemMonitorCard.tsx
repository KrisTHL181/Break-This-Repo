import { useEffect, useMemo, useRef, useState } from "react";
import { api } from "../api";
import type { SystemSnapshot, SystemStats } from "../types";
import { errMsg } from "../utils";
import { useMobileLayout } from "../hooks";
import { TuneIcon } from "./icons";

/// 后端环形缓冲的容量；首次打开要一次拿满，之后只取最新一帧。
const HISTORY_MAX = 240;

type PerfTab = "overview" | "cpu" | "memory" | "gpu" | "disk" | "network";

/// 顺序照任务管理器的性能页：总览打头，然后是 CPU、GPU、硬盘、网络。
const TABS: Array<{ id: PerfTab; label: string }> = [
  { id: "overview", label: "总览" },
  { id: "cpu", label: "CPU" },
  { id: "memory", label: "内存" },
  { id: "gpu", label: "GPU" },
  { id: "disk", label: "硬盘" },
  { id: "network", label: "网络" },
];

const COLOR_CPU = "#4e6fff";
const COLOR_MEMORY = "#7a5af8";
const COLOR_READ = "#2aa198";
const COLOR_WRITE = "#e8a33d";
const COLOR_RX = "#4e6fff";
const COLOR_TX = "#e05d5d";
const COLOR_CACHED = "#9aa7c7";

/// 0..1 的占用写成百分比；-1 是"还在采第一个差值"。
function pct(value: number): string {
  return value < 0 ? "—" : `${(value * 100).toFixed(0)}%`;
}

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

function fmtRate(bytesPerSecond: number): string {
  return bytesPerSecond < 0 ? "—" : `${fmtBytes(bytesPerSecond)}/s`;
}

/// 同一时刻只留一帧，按时间排好，只保留最近 HISTORY_MAX 条。
function mergeHistory(previous: SystemSnapshot[], incoming: SystemSnapshot[]): SystemSnapshot[] {
  const byTime = new Map<string, SystemSnapshot>();
  for (const snapshot of [...previous, ...incoming]) {
    byTime.set(snapshot.at, snapshot);
  }
  return [...byTime.values()]
    .sort((left, right) => Date.parse(left.at) - Date.parse(right.at))
    .slice(-HISTORY_MAX);
}

/// 一行总量：每项都能算时给速率，有一项是 -1 就整体"采集中"。
function totalRate(values: number[]): number {
  if (values.length === 0) return -1;
  let sum = 0;
  for (const value of values) {
    if (value < 0) return -1;
    sum += value;
  }
  return sum;
}

/// 每核热力块的颜色：越满越亮，未知是灰的。
function coreColor(value: number): string {
  if (value < 0) return "var(--monet-outline-variant)";
  const alpha = 0.12 + 0.88 * Math.min(1, Math.max(0, value));
  return `rgba(78, 111, 255, ${alpha.toFixed(2)})`;
}

/// 平台说明：同一句话在原生、容器、WSL 下不是一回事。
function perCoreNote(host: SystemSnapshot["host"]): string {
  if (host.environment === "container") return "容器里显示的是宿主机核心";
  if (host.environment === "wsl") return "WSL 里显示的是 Linux 虚拟机的核心";
  return "";
}

function diskNote(host: SystemSnapshot["host"]): string {
  if (host.environment === "container") return "容器里这是宿主机级读数";
  if (host.environment === "wsl") return "WSL 里这是虚拟机级读数";
  return "按物理磁盘统计";
}

function networkNote(host: SystemSnapshot["host"]): string {
  if (host.environment === "container") return "容器自己的网络命名空间";
  if (host.environment === "wsl") return "WSL 自己的网络命名空间";
  return "按网络接口统计";
}

function memoryNote(host: SystemSnapshot["host"]): string {
  if (host.os === "linux") {
    return "Linux 会主动拿页缓存和 slab 填内存、需要时再回收，所以“已缓存/可回收”不是被占用。";
  }
  if (host.os === "windows") {
    return "Windows 的已缓存/待机内存暂未采集：这里只显示“使用中”和“空闲”。";
  }
  return "当前平台只采集“使用中”和“空闲”。";
}

function missingReading(host: SystemSnapshot["host"], what: string): string {
  return host.os === "windows" ? `Windows 版暂未采集${what}` : `当前平台暂未采集${what}`;
}

/// 坐标轴的天花板：1/2/5×10^n，比原始最大值高一点，读起来像刻度。
function niceMax(value: number): number {
  if (value <= 1) return 1;
  const power = Math.pow(10, Math.floor(Math.log10(value)));
  const scaled = value / power;
  const step = scaled <= 1 ? 1 : scaled <= 2 ? 2 : scaled <= 5 ? 5 : 10;
  return step * power;
}

function withAlpha(hex: string, alpha: number): string {
  const value = hex.replace("#", "");
  const red = parseInt(value.substring(0, 2), 16);
  const green = parseInt(value.substring(2, 4), 16);
  const blue = parseInt(value.substring(4, 6), 16);
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

interface Point {
  t: number;
  v: number;
}

interface Series {
  label: string;
  color: string;
  points: Point[];
}

/// 多条折线的迷你图：网格 + 面积填充 + 左侧刻度，尺寸跟容器走。
function LineChart({ series, height = 120, max, format }: {
  series: Series[];
  height?: number;
  max?: number;
  format?: (value: number) => string;
}) {
  const ref = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;

    const draw = () => {
      const ratio = window.devicePixelRatio || 1;
      const width = Math.max(1, canvas.clientWidth);
      canvas.width = Math.floor(width * ratio);
      canvas.height = Math.floor(height * ratio);
      const context = canvas.getContext("2d");
      if (!context) return;
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      context.clearRect(0, 0, width, height);

      const points = series.flatMap((item) => item.points)
        .filter((point) => Number.isFinite(point.t) && Number.isFinite(point.v));
      const minT = points.length > 0 ? Math.min(...points.map((point) => point.t)) : 0;
      const maxT = points.length > 0 ? Math.max(...points.map((point) => point.t)) : 1;
      const spanT = Math.max(1, maxT - minT);
      const ceiling = max ?? (points.length > 0
        ? niceMax(Math.max(...points.map((point) => point.v), 0))
        : 100);
      const padLeft = 6;
      const padRight = 6;
      const padTop = 8;
      const padBottom = 10;
      const plotWidth = width - padLeft - padRight;
      const plotHeight = height - padTop - padBottom;

      // 网格：四条横线，刻度只有顶和底，够任务管理器那个味道了。
      context.strokeStyle = "rgba(128, 128, 128, 0.22)";
      context.lineWidth = 1;
      context.font = "10px system-ui, sans-serif";
      context.fillStyle = "rgba(128, 128, 128, 0.85)";
      for (let line = 0; line <= 4; line++) {
        const y = padTop + (plotHeight * line) / 4;
        context.beginPath();
        context.moveTo(padLeft, y);
        context.lineTo(padLeft + plotWidth, y);
        context.stroke();
      }
      context.fillText(format ? format(ceiling) : ceiling.toFixed(0), padLeft + 2, padTop + 9);
      context.fillText("0", padLeft + 2, padTop + plotHeight - 2);

      if (points.length < 2) {
        return;
      }
      for (const item of series) {
        const available = item.points.filter((point) => point.v >= 0);
        if (available.length < 2) continue;
        context.beginPath();
        available.forEach((point, index) => {
          const x = padLeft + ((point.t - minT) / spanT) * plotWidth;
          const y = padTop + plotHeight - (Math.min(point.v, ceiling) / ceiling) * plotHeight;
          if (index === 0) context.moveTo(x, y);
          else context.lineTo(x, y);
        });
        context.strokeStyle = item.color;
        context.lineWidth = 1.6;
        context.stroke();

        context.lineTo(padLeft + plotWidth, padTop + plotHeight);
        context.lineTo(padLeft, padTop + plotHeight);
        context.closePath();
        context.fillStyle = withAlpha(item.color, 0.16);
        context.fill();
      }
    };

    draw();
    const observer = new ResizeObserver(draw);
    observer.observe(canvas);
    return () => observer.disconnect();
  }, [series, height, max, format]);

  return <canvas ref={ref} style={{ width: "100%", height, display: "block" }} />;
}

function Legend({ items }: { items: Array<{ color: string; label: string; value: string }> }) {
  return (
    <div className="perf-legend">
      {items.map((item) => (
        <span key={item.label}>
          <span className="chip" style={{ background: item.color }} />
          {item.label} <b className="mono">{item.value}</b>
        </span>
      ))}
    </div>
  );
}

/// 每个逻辑线程一个小块——"数框框"。核不多时把百分比写在块里。
function CoreGrid({ cores }: { cores: number[] }) {
  const showText = cores.length <= 32;
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(auto-fill, minmax(${showText ? 40 : 24}px, 1fr))`,
        gap: 5,
      }}
    >
      {cores.map((value, index) => (
        <div
          key={index}
          title={`逻辑处理器 ${index} · ${pct(value)}`}
          style={{
            height: showText ? 28 : 22,
            borderRadius: 4,
            background: coreColor(value),
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 10,
            color: value > 0.55 ? "#fff" : "var(--monet-on-surface)",
          }}
        >
          {showText ? pct(value) : ""}
        </div>
      ))}
    </div>
  );
}

/// 内存构成条：使用中 | 已缓存 | 可回收 | 空闲。
///
/// `available = cached + reclaimable + free`（available 里还含 slab 这类
/// "不用到就不释放"的内核缓存），所以三块加起来正好是可用内存；把可回收单独
/// 画一段，条子才不会剩一截莫名其妙的空档。
function MemoryBar({ memory }: { memory: SystemSnapshot["memory"] }) {
  const total = Math.max(1, memory.totalBytes);
  const available = Math.max(0, memory.availableBytes);
  const cached = Math.min(Math.max(0, memory.cachedBytes), available);
  const free = Math.min(Math.max(0, memory.freeBytes), Math.max(0, available - cached));
  const reclaimable = Math.max(0, available - cached - free);
  const width = (value: number) => `${Math.max(0, Math.min(100, (value / total) * 100)).toFixed(2)}%`;
  return (
    <div
      className="mem-bar"
      title={`使用中 ${fmtBytes(memory.usedBytes)} · 已缓存 ${fmtBytes(cached)} · 可回收 ${fmtBytes(reclaimable)} · 空闲 ${fmtBytes(free)}`}
    >
      <div style={{ width: width(memory.usedBytes), background: COLOR_MEMORY }} />
      <div style={{ width: width(cached), background: COLOR_CACHED }} />
      <div style={{ width: width(reclaimable), background: "#c3cbe0" }} />
      <div style={{ width: width(free), background: "rgba(128, 128, 128, 0.22)" }} />
    </div>
  );
}

/// 通用 → 系统资源：任务管理器式的性能页。
///
/// 一排标签页（总览 / CPU / GPU / 硬盘 / 网络），默认总览。数据全部来自
/// `/api/system/stats`；平台差异由后端的能力位决定，采样频率也由后端决定
/// ——请求再快，拿到的也只是最新一帧。
export function SystemMonitorCard() {
  // 手机上只看总览：小屏幕摆不下每核框框和四张明细页，滚半页也读不完。
  const mobile = useMobileLayout();
  const [tab, setTab] = useState<PerfTab>("overview");
  const [snapshot, setSnapshot] = useState<SystemSnapshot | null>(null);
  const [history, setHistory] = useState<SystemSnapshot[]>([]);
  const [gpuIndex, setGpuIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [intervalMs, setIntervalMs] = useState(2000);
  const [paused, setPaused] = useState(false);
  const inFlight = useRef(false);

  const load = async (withHistory: boolean) => {
    if (inFlight.current) return;
    inFlight.current = true;
    try {
      const answer: SystemStats = await api.systemStats(withHistory ? HISTORY_MAX : 0);
      if (answer.snapshot) {
        setSnapshot(answer.snapshot);
        setHistory((previous) =>
          mergeHistory(previous, answer.history.length > 0 ? answer.history : [answer.snapshot!]),
        );
      } else if (answer.history.length > 0) {
        setHistory((previous) => mergeHistory(previous, answer.history));
        setSnapshot(answer.history[answer.history.length - 1]);
      }
      setError(null);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      inFlight.current = false;
    }
  };

  useEffect(() => {
    void load(true);
  }, []);

  useEffect(() => {
    if (paused) return;
    const refresh = () => {
      if (document.visibilityState === "visible") void load(false);
    };
    const timer = window.setInterval(refresh, intervalMs);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [intervalMs, paused]);

  const points = useMemo(() => {
    const cpu: Point[] = [];
    const memory: Point[] = [];
    const read: Point[] = [];
    const write: Point[] = [];
    const rx: Point[] = [];
    const tx: Point[] = [];
    // 和任务管理器一样只画最近 60 个采样点：一个历史尖峰不该把接下来两小时的
    // 纵轴顶死，让当前的活动看起来像一条平线。
    for (const sample of history.slice(-60)) {
      const t = Date.parse(sample.at);
      if (!Number.isFinite(t)) continue;
      if (sample.cpu.load >= 0) cpu.push({ t, v: sample.cpu.load });
      if (sample.memory.totalBytes > 0) {
        // 任务管理器的内存图是"0 到总量"的绝对刻度，不是百分比。
        memory.push({ t, v: sample.memory.usedBytes });
      }
      const readRate = totalRate(sample.disks.map((disk) => disk.readBytesPerSec));
      const writeRate = totalRate(sample.disks.map((disk) => disk.writeBytesPerSec));
      if (readRate >= 0) read.push({ t, v: readRate });
      if (writeRate >= 0) write.push({ t, v: writeRate });
      const rxRate = totalRate(sample.networks.map((network) => network.rxBytesPerSec));
      const txRate = totalRate(sample.networks.map((network) => network.txBytesPerSec));
      if (rxRate >= 0) rx.push({ t, v: rxRate });
      if (txRate >= 0) tx.push({ t, v: txRate });
    }
    return { cpu, memory, read, write, rx, tx };
  }, [history]);

  const gpuPoints = useMemo(() => {
    const utilization: Point[] = [];
    const encoder: Point[] = [];
    const decoder: Point[] = [];
    const memory: Point[] = [];
    for (const sample of history.slice(-60)) {
      const gpu = sample.gpus[gpuIndex] ?? sample.gpus[0];
      if (!gpu) continue;
      const t = Date.parse(sample.at);
      if (!Number.isFinite(t)) continue;
      if (gpu.utilization >= 0) utilization.push({ t, v: gpu.utilization });
      if ((gpu.encoderUtilization ?? -1) >= 0) encoder.push({ t, v: gpu.encoderUtilization! });
      if ((gpu.decoderUtilization ?? -1) >= 0) decoder.push({ t, v: gpu.decoderUtilization! });
      if (gpu.memoryUsedBytes >= 0) memory.push({ t, v: gpu.memoryUsedBytes });
    }
    return { utilization, encoder, decoder, memory };
  }, [history, gpuIndex]);

  const memoryMax = useMemo(() => {
    let max = 0;
    for (const sample of history.slice(-60)) {
      max = Math.max(max, sample.memory.totalBytes);
    }
    return max || 1;
  }, [history]);

  const cpu = snapshot?.cpu;
  const memory = snapshot?.memory;
  const capabilities = snapshot?.capabilities;
  const diskRead = snapshot ? totalRate(snapshot.disks.map((disk) => disk.readBytesPerSec)) : -1;
  const diskWrite = snapshot ? totalRate(snapshot.disks.map((disk) => disk.writeBytesPerSec)) : -1;
  const netRx = snapshot ? totalRate(snapshot.networks.map((network) => network.rxBytesPerSec)) : -1;
  const netTx = snapshot ? totalRate(snapshot.networks.map((network) => network.txBytesPerSec)) : -1;
  const memoryRatio = memory && memory.totalBytes > 0 ? memory.usedBytes / memory.totalBytes : -1;
  // 内存条的三块非使用区：页缓存、其他可回收（slab 等）、真正空闲。
  const memoryAvailable = memory ? Math.max(0, memory.availableBytes) : 0;
  const memoryCached = memory ? Math.min(Math.max(0, memory.cachedBytes), memoryAvailable) : 0;
  const memoryFree = memory
    ? Math.min(Math.max(0, memory.freeBytes), Math.max(0, memoryAvailable - memoryCached))
    : 0;
  const memoryReclaimable = Math.max(0, memoryAvailable - memoryCached - memoryFree);
  const activeTab: PerfTab = mobile ? "overview" : tab;

  return (
    <>
      <div className="section-caption">
        <TuneIcon size={16} />
        系统资源
      </div>
      <div className="card">
        {!mobile && (
          <div className="perf-tabs" role="tablist">
            {TABS.map((item) => (
              <button
                key={item.id}
                role="tab"
                aria-selected={activeTab === item.id}
                className={`perf-tab${activeTab === item.id ? " active" : ""}`}
                onClick={() => setTab(item.id)}
              >
                {item.label}
              </button>
            ))}
          </div>
        )}

        {error && (
          <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "10px 8px" }}>
            <span style={{ color: "var(--monet-error)" }}>读取失败：{error}</span>
            <button className="btn btn-text" style={{ height: 26, marginLeft: "auto" }} onClick={() => void load(true)}>
              重试
            </button>
          </div>
        )}

        {!snapshot ? (
          <div style={{ display: "flex", alignItems: "center", gap: 10, padding: 12 }}>
            <span className="spinner" />
            <span>正在采集系统信息…</span>
          </div>
        ) : (
          <>
            {activeTab === "overview" && (
              <div className="perf-layout">
                <div className="perf-main">
                  <div className="comp-row">
                    <span className="comp-label">CPU</span>
                    <span className="comp-value">
                      <b>{pct(cpu!.load)}</b>
                      <span style={{ color: "var(--monet-on-surface-variant)" }}>
                        {" "}
                        · {cpu!.count} 核
                        {cpu!.quotaCores > 0 && cpu!.quotaCores < cpu!.count
                          ? ` · 配额 ${cpu!.quotaCores.toFixed(2)} 核`
                          : ""}
                      </span>
                    </span>
                  </div>
                  <LineChart
                    series={[{ label: "CPU", color: COLOR_CPU, points: points.cpu }]}
                    height={64}
                    max={1}
                    format={(value) => `${(value * 100).toFixed(0)}%`}
                  />

                  <div className="comp-row">
                    <span className="comp-label">内存</span>
                    <span className="comp-value">
                      {fmtBytes(memory!.usedBytes)} / {fmtBytes(memory!.totalBytes)}（{pct(memoryRatio)}）
                    </span>
                  </div>
                  <div className="progress" style={{ margin: "0 8px 6px" }}>
                    <div className="bar" style={{ width: `${Math.max(0, Math.min(100, memoryRatio * 100))}%` }} />
                  </div>
                  <LineChart
                    series={[{ label: "使用中", color: COLOR_MEMORY, points: points.memory }]}
                    height={48}
                    max={memoryMax}
                    format={fmtBytes}
                  />

                  <div className="comp-row">
                    <span className="comp-label">负载</span>
                    <span className="comp-value mono">
                      {cpu!.loadAverage.length > 0
                        ? cpu!.loadAverage.map((value) => value.toFixed(2)).join(" / ")
                        : "—"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">磁盘吞吐</span>
                    <span className="comp-value mono">
                      {capabilities!.diskIo ? `读 ${fmtRate(diskRead)} · 写 ${fmtRate(diskWrite)}` : "不支持"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">网络吞吐</span>
                    <span className="comp-value mono">
                      {capabilities!.network ? `↓ ${fmtRate(netRx)} · ↑ ${fmtRate(netTx)}` : "不支持"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">面板自身</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      CPU {pct(snapshot.panel.cpuLoad)} · 内存 {fmtBytes(snapshot.panel.rssBytes)} ·{" "}
                      {snapshot.panel.threads} 线程
                    </span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "cpu" && (
              <div className="perf-layout">
                <div className="perf-main">
                  <div style={{ display: "flex", alignItems: "baseline", gap: 10, padding: "2px 2px 4px" }}>
                    <span className="perf-big">{pct(cpu!.load)}</span>
                    <span className="perf-label">总占用 · {cpu!.count} 个逻辑处理器</span>
                  </div>
                  <LineChart
                    series={[{ label: "CPU", color: COLOR_CPU, points: points.cpu }]}
                    height={190}
                    max={1}
                    format={(value) => `${(value * 100).toFixed(0)}%`}
                  />
                  {capabilities!.perCore ? (
                    <>
                      <div className="perf-label" style={{ margin: "10px 0 6px" }}>
                        逻辑处理器
                        {perCoreNote(snapshot.host) ? `（${perCoreNote(snapshot.host)}）` : ""}
                        {cpu!.quotaCores > 0 && cpu!.quotaCores < cpu!.count
                          ? ` · 配额 ${cpu!.quotaCores.toFixed(2)} 核`
                          : ""}
                      </div>
                      <CoreGrid cores={cpu!.cores} />
                    </>
                  ) : (
                    <div className="perf-label" style={{ margin: "10px 0 6px" }}>
                      {missingReading(snapshot.host, "每核占用")}，只显示总占用。
                    </div>
                  )}
                </div>
                <div className="perf-side">
                  <div className="comp-row">
                    <span className="comp-label">负载</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {cpu!.loadAverage.length > 0
                        ? cpu!.loadAverage.map((value) => value.toFixed(2)).join(" / ")
                        : "—"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">面板 CPU</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {pct(snapshot.panel.cpuLoad)}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">线程</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {snapshot.panel.threads}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">采样</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {Math.round(snapshot.intervalMs / 1000)} 秒
                    </span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "memory" && (
              <div className="perf-layout">
                <div className="perf-main">
                  <div style={{ display: "flex", alignItems: "baseline", gap: 10, padding: "2px 2px 4px" }}>
                    <span className="perf-big">{pct(memoryRatio)}</span>
                    <span className="perf-label">
                      {fmtBytes(memory!.usedBytes)} / {fmtBytes(memory!.totalBytes)} 使用中
                    </span>
                  </div>
                  <LineChart
                    series={[{ label: "使用中", color: COLOR_MEMORY, points: points.memory }]}
                    height={190}
                    max={memory!.totalBytes || memoryMax}
                    format={fmtBytes}
                  />
                  <div className="perf-label" style={{ margin: "10px 0 6px" }}>
                    内存构成
                  </div>
                  <MemoryBar memory={memory!} />
                  <div className="perf-legend" style={{ marginTop: 6 }}>
                    <span>
                      <span className="chip" style={{ background: COLOR_MEMORY }} />
                      使用中 <b className="mono">{fmtBytes(memory!.usedBytes)}</b>（{pct(memoryRatio)}）
                    </span>
                    {(memoryCached > 0 || memoryReclaimable > 0) && (
                      <>
                        <span>
                          <span className="chip" style={{ background: COLOR_CACHED }} />
                          已缓存 <b className="mono">{fmtBytes(memoryCached)}</b>
                        </span>
                        <span>
                          <span className="chip" style={{ background: "#c3cbe0" }} />
                          可回收 <b className="mono">{fmtBytes(memoryReclaimable)}</b>
                        </span>
                      </>
                    )}
                    <span>
                      <span className="chip" style={{ background: "rgba(128, 128, 128, 0.22)" }} />
                      空闲 <b className="mono">{fmtBytes(memoryFree)}</b>
                    </span>
                    <span>
                      可用 <b className="mono">{fmtBytes(memory!.availableBytes)}</b>（=
                      {memoryCached + memoryReclaimable > 0 ? "已缓存 + 可回收 + 空闲" : "空闲"}）
                    </span>
                  </div>
                  <div className="perf-label" style={{ marginTop: 4 }}>
                    {memoryNote(snapshot.host)}
                  </div>
                </div>
                <div className="perf-side">
                  <div className="comp-row">
                    <span className="comp-label">来源</span>
                    <span className="comp-value" style={{ fontSize: 12 }}>
                      {memory!.source === "cgroup" ? "容器限额" : "整机"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">
                      {snapshot.host.os === "windows" ? "页面文件" : "交换"}
                    </span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {memory!.swapTotalBytes > 0
                        ? `${fmtBytes(memory!.swapUsedBytes)} / ${fmtBytes(memory!.swapTotalBytes)}`
                        : "未启用"}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">面板占用</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {fmtBytes(snapshot.panel.rssBytes)}
                    </span>
                  </div>
                  <div className="comp-row">
                    <span className="comp-label">采样</span>
                    <span className="comp-value mono" style={{ fontSize: 12 }}>
                      {Math.round(snapshot.intervalMs / 1000)} 秒
                    </span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "gpu" && (() => {
              const gpu = snapshot.gpus[Math.min(gpuIndex, Math.max(0, snapshot.gpus.length - 1))];
              const metric = (label: string, value: string) => (
                <div className="card" style={{ padding: "8px 10px", minWidth: 0 }}>
                  <div className="perf-label">{label}</div>
                  <div style={{ fontSize: 20, fontWeight: 600, lineHeight: 1.3 }}>{value}</div>
                </div>
              );
              return (
                <div className="perf-layout">
                  <div className="perf-main">
                    <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "2px 2px 8px", flexWrap: "wrap" }}>
                      <span className="perf-label">显卡</span>
                      {snapshot.gpus.length > 0 ? (
                        <select
                          className="input"
                          style={{ width: "auto", minWidth: 180 }}
                          value={Math.min(gpuIndex, snapshot.gpus.length - 1)}
                          onChange={(e) => setGpuIndex(Number(e.target.value) || 0)}
                        >
                          {snapshot.gpus.map((entry, index) => (
                            <option key={entry.index} value={index}>
                              {index}: {entry.name}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
                          未读到 GPU；Linux 需要 /sys/class/drm + /dev/dri，Windows 目前只支持 nvidia-smi。
                        </span>
                      )}
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 8 }}>
                      {metric("GPU 利用率", gpu ? pct(gpu.utilization) : "—")}
                      {metric("编码单元", gpu ? pct(gpu.encoderUtilization ?? -1) : "—")}
                      {metric("解码单元", gpu ? pct(gpu.decoderUtilization ?? -1) : "—")}
                      {metric("显存", gpu ? `${fmtBytes(gpu.memoryUsedBytes)} / ${fmtBytes(gpu.memoryTotalBytes)}` : "—")}
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 10, paddingTop: 10 }}>
                      <div>
                        <div className="perf-label">GPU 利用率</div>
                        <LineChart series={[{ label: "GPU", color: COLOR_CPU, points: gpuPoints.utilization }]}
                                   height={110} max={1} format={pct} />
                      </div>
                      <div>
                        <div className="perf-label">编码单元</div>
                        <LineChart series={[{ label: "编码", color: COLOR_READ, points: gpuPoints.encoder }]}
                                   height={110} max={1} format={pct} />
                      </div>
                      <div>
                        <div className="perf-label">解码单元</div>
                        <LineChart series={[{ label: "解码", color: COLOR_WRITE, points: gpuPoints.decoder }]}
                                   height={110} max={1} format={pct} />
                      </div>
                      <div>
                        <div className="perf-label">显存使用</div>
                        <LineChart series={[{ label: "显存", color: COLOR_MEMORY, points: gpuPoints.memory }]}
                                   height={110} format={fmtBytes} />
                      </div>
                    </div>

                    {gpu && (
                      <div className="perf-label" style={{ paddingTop: 8 }}>
                        {gpu.name} · {gpu.vendor || "GPU"} · {gpu.backend || ""} ·{" "}
                        {gpu.temperatureC >= 0 ? `${gpu.temperatureC.toFixed(0)}°C` : "温度 —"} ·{" "}
                        {(gpu.powerW ?? -1) >= 0 ? `${gpu.powerW!.toFixed(0)} W` : "功耗 —"}
                      </div>
                    )}
                  </div>
                </div>
              );
            })()}

            {activeTab === "disk" && (
              <div className="perf-layout">
                <div className="perf-main">
                  <div style={{ display: "flex", alignItems: "baseline", gap: 10, padding: "2px 2px 4px" }}>
                    <span className="perf-big">{fmtRate(diskRead)}</span>
                    <span className="perf-label">读 · 写 {fmtRate(diskWrite)}</span>
                  </div>
                  {capabilities!.diskIo ? (
                    <>
                      <LineChart
                        series={[
                          { label: "读", color: COLOR_READ, points: points.read },
                          { label: "写", color: COLOR_WRITE, points: points.write },
                        ]}
                        height={170}
                        format={fmtRate}
                      />
                      <Legend
                        items={[
                          { color: COLOR_READ, label: "读", value: fmtRate(diskRead) },
                          { color: COLOR_WRITE, label: "写", value: fmtRate(diskWrite) },
                        ]}
                      />
                      <div className="perf-label" style={{ margin: "4px 0" }}>
                        {diskNote(snapshot.host)}
                      </div>
                      {snapshot.disks.map((disk) => (
                        <div className="comp-row" key={disk.name}>
                          <span className="comp-label mono">
                            {disk.name === "total" ? "全部磁盘" : disk.name}
                          </span>
                          <span className="comp-value mono" style={{ fontSize: 12 }}>
                            读 {fmtRate(disk.readBytesPerSec)} · 写 {fmtRate(disk.writeBytesPerSec)}
                          </span>
                        </div>
                      ))}
                    </>
                  ) : (
                    <div className="comp-row">
                      <span className="comp-label">磁盘</span>
                      <span className="comp-value" style={{ fontSize: 12 }}>
                        {missingReading(snapshot.host, "磁盘吞吐")}
                      </span>
                    </div>
                  )}
                  {snapshot.mounts.map((mount) => {
                    const ratio = mount.totalBytes > 0 ? mount.usedBytes / mount.totalBytes : 0;
                    return (
                      <div key={mount.path} style={{ padding: "6px 0 2px" }}>
                        <div className="comp-row" style={{ borderBottom: "none", padding: 0 }}>
                          <span className="comp-label mono">{mount.path}</span>
                          <span className="comp-value" style={{ fontSize: 12 }}>
                            {fmtBytes(mount.usedBytes)} / {fmtBytes(mount.totalBytes)}（{pct(ratio)}）
                          </span>
                        </div>
                        <div className="progress" style={{ marginTop: 4 }}>
                          <div className="bar" style={{ width: `${Math.max(0, Math.min(100, ratio * 100))}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {activeTab === "network" && (
              <div className="perf-layout">
                <div className="perf-main">
                  <div style={{ display: "flex", alignItems: "baseline", gap: 10, padding: "2px 2px 4px" }}>
                    <span className="perf-big">{fmtRate(netRx)}</span>
                    <span className="perf-label">接收 · 发送 {fmtRate(netTx)}</span>
                  </div>
                  {capabilities!.network ? (
                    <>
                      <LineChart
                        series={[
                          { label: "接收", color: COLOR_RX, points: points.rx },
                          { label: "发送", color: COLOR_TX, points: points.tx },
                        ]}
                        height={170}
                        format={fmtRate}
                      />
                      <Legend
                        items={[
                          { color: COLOR_RX, label: "接收", value: fmtRate(netRx) },
                          { color: COLOR_TX, label: "发送", value: fmtRate(netTx) },
                        ]}
                      />
                      <div className="perf-label" style={{ margin: "4px 0" }}>
                        {networkNote(snapshot.host)}
                      </div>
                      {snapshot.networks.map((network) => (
                        <div className="comp-row" key={network.name}>
                          <span className="comp-label mono">{network.name}</span>
                          <span className="comp-value mono" style={{ fontSize: 12 }}>
                            ↓ {fmtRate(network.rxBytesPerSec)} · ↑ {fmtRate(network.txBytesPerSec)}
                          </span>
                        </div>
                      ))}
                    </>
                  ) : (
                    <div className="comp-row">
                      <span className="comp-label">网络</span>
                      <span className="comp-value" style={{ fontSize: 12 }}>
                        {missingReading(snapshot.host, "网络吞吐")}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                flexWrap: "wrap",
                padding: "8px 8px 6px",
                borderTop: "1px solid var(--monet-outline-variant)",
                fontSize: 12,
                color: "var(--monet-on-surface-variant)",
              }}
            >
              <span>
                采样 {Math.round(snapshot.intervalMs / 1000)} 秒一帧
                {snapshot.intervalMs >= 30_000 ? "（低频守护：暂时没人使用面板）" : ""}
                {mobile ? " · 手机端只显示总览" : ""}
              </span>
              <span>· 最后更新 {new Date(snapshot.at).toLocaleTimeString()}</span>
              <span style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 6 }}>
                刷新
                <select
                  className="input"
                  style={{ width: 92, height: 26, padding: "0 6px" }}
                  value={intervalMs}
                  onChange={(e) => setIntervalMs(Number(e.target.value))}
                >
                  <option value={1000}>1 秒</option>
                  <option value={2000}>2 秒</option>
                  <option value={5000}>5 秒</option>
                </select>
                <button className="btn btn-text" style={{ height: 26 }} onClick={() => setPaused((value) => !value)}>
                  {paused ? "继续" : "暂停"}
                </button>
              </span>
            </div>
          </>
        )}
      </div>
    </>
  );
}
