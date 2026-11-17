import { useEffect, useState } from "react";
import { api, type TranscodeCapabilities, type TranscodeInstallJob } from "../../api";
import { refreshTranscodeFeature, setTranscodeFeature, useTranscodeFeature } from "../../transcodeFeature";
import { errMsg } from "../../utils";
import { toast } from "../../store";
import { ProgressBar } from "../../components/ProgressBar";
import { HelpNote } from "./common";

const INSTALL_SOURCES = [
  { id: "linux-source", label: "Linux 源码 · ffmpeg 7.1.5", kind: "compile" as const,
    url: "https://ffmpeg.org/releases/ffmpeg-7.1.5.tar.xz" },
  { id: "linux-static", label: "Linux 静态包 · BtbN GPL", kind: "static" as const,
    url: "https://github.com/BtbN/FFmpeg-Builds/releases/latest/download/ffmpeg-master-latest-linux64-gpl.tar.xz" },
  { id: "win-static", label: "Windows 静态包 · gyan.dev", kind: "static" as const,
    url: "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip" },
  { id: "mac-static", label: "macOS 静态包 · evermeet.cx", kind: "static" as const,
    url: "https://evermeet.cx/ffmpeg/getrelease/zip" },
];

export function TranscodeSettingsPage() {
  const feature = useTranscodeFeature();
  const [capabilities, setCapabilities] = useState<TranscodeCapabilities | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [policy, setPolicy] = useState("ask");
  const [timeoutMinutes, setTimeoutMinutes] = useState(10);
  const [saving, setSaving] = useState(false);
  const [installKind, setInstallKind] = useState<"static" | "package" | "compile">("package");
  const [installUrl, setInstallUrl] = useState("");
  const [installSha, setInstallSha] = useState("");
  const [installVersion, setInstallVersion] = useState("");
  const [installJob, setInstallJob] = useState<TranscodeInstallJob | null>(null);
  const [probeEncoderName, setProbeEncoderName] = useState("libx264");
  const [probeResult, setProbeResult] = useState<{ ok: boolean; encoder: string; exitCode: number; output: string } | null>(null);
  const [probing, setProbing] = useState(false);

  const load = async () => {
    setBusy(true);
    setError(null);
    try {
      setCapabilities(await api.transcode.capabilities());
      await refreshTranscodeFeature();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    if (!feature) return;
    setPolicy(feature.failurePolicy);
    setTimeoutMinutes(feature.failureTimeoutMinutes);
  }, [feature?.failurePolicy, feature?.failureTimeoutMinutes]);

  useEffect(() => {
    if (!installJob || installJob.state !== "running") return;
    const timer = window.setInterval(() => {
      void api.transcode.runtimeJob(installJob.id).then((job) => {
        setInstallJob(job);
        if (job.state === "done") void load();
      }).catch(() => undefined);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [installJob]);

  const startInstall = async () => {
    try {
      setInstallJob(await api.transcode.runtimeInstall({
        kind: installKind,
        url: installKind === "package" ? undefined : installUrl,
        sha256: installKind === "package" ? undefined : installSha,
        version: installVersion || undefined,
      }));
    } catch (e) {
      toast("error", errMsg(e));
    }
  };

  const savePolicy = async () => {
    setSaving(true);
    try {
      const next = await api.transcode.updateSettings({
        failurePolicy: policy,
        failureTimeoutMinutes: timeoutMinutes,
      });
      setTranscodeFeature(next);
      toast("success", "失败策略已保存");
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {error && (
        <div className="card">
          <div className="comp-row">
            <span className="comp-label">读取失败</span>
            <span className="comp-value" style={{ color: "var(--monet-error)" }}>{error}</span>
          </div>
        </div>
      )}

      <HelpNote title="设置项怎么理解">
        <ul>
          <li><b>失败策略</b>：<b>询问</b>每次失败都等你决定；<b>超时自动降级</b>等到超时时间后自动换等价编码器；<b>完全自动化</b>失败立即降级、不再询问。</li>
          <li><b>并发 / 队列上限</b>来自 <span className="mono">server.yaml</span> 的 <span className="mono">transcode:</span> 段或 <span className="mono">HDSL_TRANSCODE_*</span> 环境变量，改完要重启面板。</li>
          <li><b>媒体根</b>是转码读写文件的范围；<b>ffmpeg 路径</b>留空就用镜像/系统自带的。</li>
          <li>关闭转码功能后 <span className="mono">/api/transcode/*</span> 返回 404，导航入口也会消失；数据与预设保留。</li>
          <li>「探测编码器」只跑 1 帧试编码，用来区分“参数写错”和“硬件根本不可用”。</li>
        </ul>
      </HelpNote>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">ffmpeg</span>
          <span className="comp-value">
            {capabilities?.available ? <span className="tag running">可用</span> : <span className="tag plain">不可用</span>}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">版本</span>
          <span className="comp-value mono" style={{ fontSize: 12 }}>{capabilities?.version || "—"}</span>
        </div>
        <div className="comp-row">
          <span className="comp-label">ffmpeg 路径</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {capabilities?.ffmpeg || "—"}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">ffprobe 路径</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {capabilities?.ffprobe || "—"}
          </span>
        </div>
        <div style={{ display: "flex", gap: 8, padding: "8px", flexWrap: "wrap" }}>
          <button className="btn btn-text" disabled={busy} onClick={() => void load()}>
            {busy ? "探测中…" : "重新探测"}
          </button>
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", alignSelf: "center" }}>
            也可以手动配置 `transcode.ffmpeg_path` / `HDSL_FFMPEG_PATH`。
          </span>
        </div>
        <div style={{ display: "grid", gap: 6, padding: "0 8px 8px", borderTop: "1px solid var(--monet-outline-variant)" }}>
          <label style={{ fontSize: 12, paddingTop: 8 }}>在线安装</label>
          <select className="input" value={installKind}
                  onChange={(e) => setInstallKind(e.target.value as typeof installKind)}>
            <option value="package">包管理器（apt-get / dnf / zypper）</option>
            <option value="static">下载静态包（Windows/macOS/Linux）</option>
            <option value="compile">源码编译（最后手段）</option>
          </select>
          {installKind !== "package" && (
            <>
              <select className="input" value=""
                      onChange={(e) => {
                        const source = INSTALL_SOURCES.find((item) => item.id === e.target.value);
                        if (source) {
                          setInstallKind(source.kind);
                          setInstallUrl(source.url);
                        }
                      }}>
                <option value="">填入常用来源…</option>
                {INSTALL_SOURCES.map((source) => (
                  <option key={source.id} value={source.id}>{source.label}</option>
                ))}
              </select>
              <input className="input"
                     placeholder={installKind === "compile"
                       ? "https://ffmpeg.org/releases/ffmpeg-7.1.5.tar.xz"
                       : "https://…/ffmpeg-release-essentials.zip 或 .tar.xz"}
                     value={installUrl} onChange={(e) => setInstallUrl(e.target.value)} />
              <input className="input" placeholder="SHA-256（留空 = 自动读取 .sha256 / checksums.sha256）"
                     value={installSha} onChange={(e) => setInstallSha(e.target.value)} />
              <input className="input" placeholder="安装目录名（可选，例如 7.1.5）"
                     value={installVersion} onChange={(e) => setInstallVersion(e.target.value)} />
            </>
          )}
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-raised" disabled={installJob?.state === "running"
              || (installKind !== "package" && !installUrl)}
                    onClick={() => void startInstall()}>
              {installKind === "package" ? "开始安装" : installKind === "static" ? "下载并安装" : "开始编译"}
            </button>
            {installJob?.state === "running" && (
              <button className="btn btn-text danger"
                      onClick={() => void api.transcode.runtimeCancel(installJob.id).then(setInstallJob)}>
                取消
              </button>
            )}
          </div>
          <p style={{ fontSize: 11, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
            SHA-256 留空时面板会尝试读取发布方的 `.sha256` / `checksums.sha256`；读不到会失败并给
            出地址，不会跳过校验。源码编译需要 make/gcc/pkg-config 与 libx264/libx265/libva 开发包。
            包管理器安装只对当前容器有效，重建后需重装；静态包与编译结果在数据卷里。
          </p>
          {installJob && (
            <>
              <div className="comp-row">
                <span className="comp-label">安装状态</span>
                <span className="comp-value">
                  <span className={`tag ${installJob.state === "done" ? "running" : "plain"}`}>
                    {installJob.state === "done" ? "完成" : installJob.state === "failed" ? "失败"
                      : installJob.state === "cancelled" ? "已取消" : installJob.message || "运行中"}
                  </span>
                </span>
              </div>
              <ProgressBar fraction={installJob.progress} />
              {installJob.error && (
                <span style={{ fontSize: 12, color: "var(--monet-error)" }}>{installJob.error}</span>
              )}
              <pre style={{ margin: 0, padding: 8, maxHeight: 180, overflow: "auto", fontSize: 10,
                            background: "var(--monet-surface-container-low)", whiteSpace: "pre-wrap" }}>
                {installJob.log || "（暂无输出）"}
              </pre>
            </>
          )}
        </div>
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">媒体根</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {capabilities?.mediaRoot || feature?.mediaRoot || "—"}
          </span>
        </div>
        <div className="comp-row">
          <span className="comp-label">可写性</span>
          <span className="comp-value">
            {capabilities?.mediaRootExists
              ? capabilities.mediaRootWritable ? "可写" : "只读"
              : "不存在"}
          </span>
        </div>
        <p style={{ fontSize: 12, padding: "4px 8px 8px", lineHeight: 1.6, color: "var(--monet-on-surface-variant)" }}>
          容器里固定为 `/home/hdsl/video`（`HDSL_VIDEO_DIR` 决定宿主挂载）；裸部署默认
          &lt;数据目录&gt;/video，可用 `HDSL_MEDIA_ROOT` 或 `server.yaml` 的 `transcode.media_root` 修改。
        </p>
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">并发 / 队列上限</span>
          <span className="comp-value">
            {capabilities?.maxConcurrent ?? feature?.maxConcurrent ?? "—"} /{" "}
            {capabilities?.maxQueue ?? feature?.maxQueue ?? "—"}
          </span>
        </div>
        <div style={{ display: "grid", gap: 6, padding: "4px 8px 8px" }}>
          <label style={{ fontSize: 12 }}>失败策略</label>
          <select className="input" value={policy} onChange={(e) => setPolicy(e.target.value)}>
            <option value="ask">询问（默认）</option>
            <option value="timeout-auto">超时自动降级</option>
            <option value="full-auto">完全自动化（立即降级）</option>
          </select>
          <label style={{ fontSize: 12 }}>超时时间（分钟，仅询问/超时自动降级）</label>
          <input className="input" type="number" min={1} max={1440} value={timeoutMinutes}
                 onChange={(e) => setTimeoutMinutes(Number(e.target.value) || 10)} />
          <button className="btn btn-raised" disabled={saving} onClick={() => void savePolicy()}>
            {saving ? "保存中…" : "保存失败策略"}
          </button>
        </div>
        <div className="comp-row">
          <span className="comp-label">重启恢复队列</span>
          <span className="comp-value">{feature?.autoResume ? "是" : "否"}</span>
        </div>
        <p style={{ fontSize: 12, padding: "4px 8px 8px", lineHeight: 1.6, color: "var(--monet-on-surface-variant)" }}>
          这些值来自 `server.yaml` 的 `transcode:` 段与 `HDSL_TRANSCODE_*` 环境变量；改完重启面板生效。
        </p>
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">编码器</span>
          <span className="comp-value" style={{ fontSize: 12 }}>
            {capabilities ? `${capabilities.encoders.length} 个` : "—"}
          </span>
        </div>
        {capabilities && (
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap", padding: "4px 8px 8px", maxHeight: 180, overflow: "auto" }}>
            {capabilities.encoders
              .filter((name) => /264|265|av1|vp9|aac|opus|vaapi|qsv|nvenc/.test(name))
              .map((name) => (
                <span key={name} className="tag plain" style={{ fontSize: 11 }}>{name}</span>
              ))}
          </div>
        )}
        <div style={{ display: "flex", gap: 6, alignItems: "center", padding: "0 8px 8px", flexWrap: "wrap" }}>
          <select className="input" style={{ width: "auto", minWidth: 160 }} value={probeEncoderName}
                  onChange={(e) => setProbeEncoderName(e.target.value)}>
            {(capabilities?.encoders ?? [])
              .filter((name) => /264|265|av1|vp9|vaapi|qsv|nvenc/.test(name))
              .map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
          <button className="btn btn-text" disabled={probing} onClick={() => {
            setProbing(true);
            setProbeResult(null);
            void api.transcode.probeEncoder(probeEncoderName)
              .then(setProbeResult)
              .finally(() => setProbing(false));
          }}>
            {probing ? "测试中…" : "测试 1 帧"}
          </button>
          <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
            列出 ≠ 能编：QSV/VAAPI/NVENC 用 1 帧实测，失败会给出 ffmpeg 输出。
          </span>
        </div>
        {probeResult && (
          <pre style={{ margin: "0 8px 8px", padding: 8, maxHeight: 160, overflow: "auto", fontSize: 10,
                        background: "var(--monet-surface-container-low)", whiteSpace: "pre-wrap" }}>
            {probeResult.ok ? "OK" : "FAILED"} ({probeResult.exitCode})
            {"\n"}{probeResult.output || "（无输出）"}
          </pre>
        )}
      </div>
    </>
  );
}