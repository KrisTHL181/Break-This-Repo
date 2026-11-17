import { useEffect, useMemo, useState } from "react";
import { api, type TranscodeMediaEntry, type TranscodePreset, type TranscodePresetSummary, type TranscodePreview, type TranscodePlan } from "../../api";
import { toast } from "../../store";
import { errMsg } from "../../utils";
import { MediaBrowserPanel, fmtBytes, probeSummary, HelpNote } from "./common";
import { DirectoryPicker } from "./DirectoryPicker";
import { valueLabel } from "./labels";

type StagedItem = {
  entry: TranscodeMediaEntry;
  output?: string;
  outputAbsolute?: string;
  error?: string;
};

export function PreparePage({ onOpenTab }: { onOpenTab: (tab: string) => void }) {
  const [presets, setPresets] = useState<TranscodePresetSummary[]>([]);
  const [presetId, setPresetId] = useState(() => sessionStorage.getItem("transcode.presetId") ?? "");
  const [selected, setSelected] = useState<TranscodeMediaEntry | null>(null);
  const [probe, setProbe] = useState<Record<string, unknown> | null>(null);
  const [outputDir, setOutputDir] = useState("");
  const [preview, setPreview] = useState<TranscodePreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [presetDetail, setPresetDetail] = useState<TranscodePreset | null>(null);
  const [staged, setStaged] = useState<StagedItem[]>([]);
  const [plan, setPlan] = useState<TranscodePlan | null>(null);
  const [mediaRoot, setMediaRoot] = useState("");
  const [scanning, setScanning] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);

  useEffect(() => {
    if (!presetId) {
      setPresetDetail(null);
      return;
    }
    let cancelled = false;
    void api.transcode.preset(presetId).then((entry) => {
      if (!cancelled) setPresetDetail(entry.preset);
    }).catch(() => {
      if (!cancelled) setPresetDetail(null);
    });
    return () => {
      cancelled = true;
    };
  }, [presetId]);

  useEffect(() => {
    void api.transcode
      .presets()
      .then((answer) => {
        setPresets(answer.presets);
        // No auto-selection: opening the page must not silently enqueue with the
        // first built-in preset. The operator picks a starting point explicitly.
      })
      .catch((e) => setError(errMsg(e)));
    void api.transcode
      .capabilities()
      .then((capabilities) => setMediaRoot(capabilities.mediaRoot))
      .catch(() => {
        // The media root is a display aid; plan/submit will report real errors.
      });
  }, []);

  const select = async (entry: TranscodeMediaEntry) => {
    setSelected(entry);
    setPreview(null);
    setProbe(null);
    setError(null);
    try {
      setProbe(await api.transcode.mediaProbe(entry.path));
    } catch {
      // The probe is a convenience; submission has its own validation.
    }
  };

  const checkFoolproof = (): boolean => {
    if (!presetDetail) return true;
    const encoder = String(presetDetail.videoEncoder ?? "");
    const container = String(presetDetail.container ?? "").toLowerCase();
    if (!encoder || !container) {
      const message = !encoder ? "预设没有视频编码器" : "预设没有输出容器";
      setError(message);
      toast("error", message);
      return false;
    }
    const restrictive = ["mp4", "mov", "avi", "flv", "ts", "m2ts"];
    const keepsTracks = presetDetail.subtitleMode === "copy"
      || presetDetail.copyAttachments === true
      || presetDetail.copyChapters === true;
    if (restrictive.includes(container) && keepsTracks) {
      const proceed = window.confirm(
        `当前容器 ${container.toUpperCase()} 可能装不下全部字幕/附件/章节。\n` +
        `建议把预设容器改成 MKV。仍然继续入队吗？`,
      );
      if (!proceed) return false;
    }
    return true;
  };

  const addEntries = (entries: TranscodeMediaEntry[]) => {
    setStaged((previous) => {
      const seen = new Set(previous.map((item) => item.entry.path));
      const next = [...previous];
      for (const entry of entries) {
        if (entry.directory || seen.has(entry.path)) continue;
        seen.add(entry.path);
        next.push({ entry });
      }
      return next;
    });
  };

  const addFolderPath = async (folder: string) => {
    setScanning(true);
    setError(null);
    try {
      const answer = await api.transcode.mediaScan(folder, true, 2000);
      addEntries(answer.entries);
      if (answer.truncated) {
        toast("info", `文件太多，只加入前 ${answer.entries.length} 个（上限 2000）`);
      } else if (answer.entries.length === 0) {
        toast("info", "这个文件夹里没有视频或音频文件");
      } else {
        toast("success", `已扫描 ${answer.entries.length} 个文件，重复项已跳过`);
      }
    } catch (e) {
      const message = errMsg(e);
      setError(message);
      toast("error", message);
    } finally {
      setScanning(false);
    }
  };

  const stagedKey = useMemo(() => staged.map((item) => item.entry.path).join("\n"), [staged]);

  // 输出路径由服务端规划：与真正入队时使用同一套命名/防撞逻辑，这样列表里显示的
  // 就是容器里实际会写出的绝对路径，而不是前端猜的。
  useEffect(() => {
    if (!presetDetail || staged.length === 0) {
      setPlan(null);
      return;
    }
    let cancelled = false;
    const timer = window.setTimeout(() => {
      void api.transcode
        .plan({
          preset: presetDetail,
          inputs: staged.map((item) => item.entry.path),
          outputDir: outputDir.trim() || undefined,
        })
        .then((answer) => {
          if (cancelled) return;
          setPlan(answer);
          setStaged((previous) => previous.map((item) => {
            const hit = answer.items.find((candidate) => candidate.input === item.entry.path);
            return hit
              ? { ...item, output: hit.output, outputAbsolute: hit.outputAbsolute, error: hit.error }
              : item;
          }));
        })
        .catch((e) => {
          if (!cancelled) setError(errMsg(e));
        });
    }, 400);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [stagedKey, presetDetail, outputDir]);

  const previewCommand = async () => {
    if (!selected || !presetDetail || !checkFoolproof()) return;
    setBusy(true);
    setError(null);
    try {
      setPreview(await api.transcode.preview({
        preset: presetDetail,
        input: selected.path,
        outputDir: outputDir.trim() || undefined,
      }));
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  // 选中文件 + 预设后自动预览命令（600ms 防抖）：3FUI 的自检入口，不用先点按钮。
  useEffect(() => {
    if (!selected || !presetDetail) {
      setPreview(null);
      return;
    }
    let cancelled = false;
    const timer = window.setTimeout(() => {
      void api.transcode
        .preview({ preset: presetDetail, input: selected.path, outputDir: outputDir.trim() || undefined })
        .then((answer) => {
          if (!cancelled) setPreview(answer);
        })
        .catch(() => {
          if (!cancelled) setPreview(null);
        });
    }, 600);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [selected, presetDetail, outputDir]);

  const enqueue = async () => {
    if (!selected || !presetId || !checkFoolproof()) return;
    setBusy(true);
    setError(null);
    try {
      await api.transcode.submitJob({
        input: selected.path,
        presetId,
        outputDir: outputDir.trim() || undefined,
      });
      toast("success", "已加入编码队列");
      onOpenTab("queue");
    } catch (e) {
      setError(errMsg(e));
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const enqueueAll = async () => {
    if (staged.length === 0 || !presetId || !checkFoolproof()) return;
    setBusy(true);
    setError(null);
    let added = 0;
    const failures: string[] = [];
    for (const item of staged) {
      try {
        await api.transcode.submitJob({
          input: item.entry.path,
          presetId,
          outputDir: outputDir.trim() || undefined,
        });
        added++;
      } catch (e) {
        failures.push(`${item.entry.name}: ${errMsg(e)}`);
      }
    }
    setBusy(false);
    if (added > 0) {
      toast("success", `已加入 ${added} 个任务`);
      setStaged([]);
      setPlan(null);
      onOpenTab("queue");
    }
    if (failures.length > 0) {
      setError(failures.slice(0, 3).join("\n") + (failures.length > 3 ? `\n…还有 ${failures.length - 3} 个` : ""));
      toast("error", `${failures.length} 个文件未加入，见右侧错误`);
    }
  };

  const removeStaged = (path: string) => {
    setStaged((previous) => previous.filter((item) => item.entry.path !== path));
  };

  const outputSelection = outputDir.trim();
  const outputLocation = outputSelection === "."
    ? (plan?.outputDir || mediaRoot)
    : outputSelection
      ? (plan?.outputDir || `${mediaRoot}/${outputSelection}`.replace(/\/+/g, "/"))
      : "每个源文件旁边（见下方准备列表）";

  return (
    <div style={{ display: "grid", gap: 8 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "flex-start", flexWrap: "wrap" }}>
        <div className="tc-prepare-main">
          <MediaBrowserPanel
            selected={selected?.path}
            onSelect={(entry) => void select(entry)}
            onAddFile={(entry) => {
              addEntries([entry]);
              void select(entry);
            }}
            onAddFolder={(entry) => void addFolderPath(entry.path)}
            onAddCurrentFolder={(path) => void addFolderPath(path)}
          />
        </div>

        <div className="tc-prepare-side">
          <HelpNote title="准备文件怎么用">
            <ul>
              <li>左边点文件「加入」，或点文件夹「添加」；也可以「添加当前文件夹」，会递归收集视频/音频。</li>
              <li>准备列表支持一次加入多个文件，最后一个「加入队列」会全部提交。</li>
              <li>「输出目录」留空 = 输出到每个源文件旁边；填相对路径 = 统一输出到媒体根下的这个目录。</li>
              <li>准备列表里显示的是服务器上的绝对输出路径；容器里看到的路径就是它，不会写到别处。</li>
              <li>预设来自「参数面板 → 保存为预设」；内置预设只读，需要改就用另存为。</li>
            </ul>
          </HelpNote>
          <div className="card">
            <div className="comp-row">
              <span className="comp-label">已选文件</span>
              <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
                {selected ? selected.path : "从左侧选择"}
              </span>
            </div>
            {selected && (
              <div className="comp-row">
                <span className="comp-label">信息</span>
                <span className="comp-value" style={{ fontSize: 12 }}>
                  {probe ? probeSummary(probe) : "—"} · {fmtBytes(selected.size)}
                </span>
              </div>
            )}
            <div className="comp-row">
              <span className="comp-label">媒体根</span>
              <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
                {mediaRoot || "—"}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">输出位置</span>
              <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
                {outputLocation}
              </span>
            </div>
            <div style={{ padding: "4px 8px 8px", display: "grid", gap: 6 }}>
              <label style={{ fontSize: 12 }}>预设</label>
              <select
                className="input"
                value={presetId}
                disabled={busy || presets.length === 0}
                onChange={(e) => {
                  setPresetId(e.target.value);
                  if (e.target.value) sessionStorage.setItem("transcode.presetId", e.target.value);
                }}
              >
                <option value="">选择参数面板 / 预设…</option>
                {presets.map((preset) => (
                  <option key={preset.id} value={preset.id}>
                    {preset.name}{preset.builtin ? "" : "（自定义）"}
                  </option>
                ))}
              </select>
              {presetDetail && (
                <div className="tc-prepare-summary">
                  <span className="tag plain" title={String(presetDetail.videoEncoder ?? "")}>
                    {valueLabel("videoEncoder", presetDetail.videoEncoder)}
                  </span>
                  <span className="tag plain">{valueLabel("container", presetDetail.container)}</span>
                  <span className="tag plain" title={String(presetDetail.qualityMode ?? "")}>
                    {valueLabel("qualityMode", presetDetail.qualityMode)} {String(presetDetail.qualityValue ?? "")}
                  </span>
                  <span className="tag plain">音频 {valueLabel("audioMode", presetDetail.audioMode)}</span>
                  <span className="tag plain">字幕 {valueLabel("subtitleMode", presetDetail.subtitleMode)}</span>
                </div>
              )}
              <label style={{ fontSize: 12 }}>输出目录</label>
              <div style={{ display: "flex", gap: 6, alignItems: "center", flexWrap: "wrap" }}>
                <input
                  className="input"
                  style={{ flex: "1 1 140px", minWidth: 0 }}
                  value={outputDir}
                  disabled={busy}
                  placeholder="留空 = 每个源文件旁边"
                  onChange={(e) => setOutputDir(e.target.value)}
                />
                <button className="btn btn-text" disabled={busy} onClick={() => setPickerOpen(true)}>
                  选择目录
                </button>
                <button className="btn btn-text" disabled={busy || outputDir === ""} onClick={() => setOutputDir("")}>
                  源目录
                </button>
              </div>
              <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)", overflowWrap: "anywhere" }}>
                当前：{outputLocation}
              </span>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", paddingTop: 4 }}>
                <button className="btn btn-text" disabled={busy} onClick={() => onOpenTab("presets")}>
                  打开参数面板
                </button>
                <button className="btn btn-text" disabled={busy || !selected || !presetId} onClick={() => void previewCommand()}>
                  预览命令
                </button>
                <button className="btn btn-raised" disabled={busy || !selected || !presetId} onClick={() => void enqueue()}>
                  {busy ? "处理中…" : "仅加入当前文件"}
                </button>
              </div>
            </div>
            {error && (
              <div style={{ padding: "0 8px 8px", fontSize: 12, color: "var(--monet-error)", whiteSpace: "pre-wrap" }}>
                {error}
              </div>
            )}
          </div>

          {preview && (
            <div className="card">
              <div className="comp-row">
                <span className="comp-label">输出</span>
                <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
                  {preview.output}
                </span>
              </div>
              <pre
                style={{
                  margin: "4px 8px 8px",
                  padding: 8,
                  maxHeight: 220,
                  overflow: "auto",
                  fontSize: 11,
                  lineHeight: 1.5,
                  background: "var(--monet-surface-container-low)",
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-all",
                }}
              >
                {preview.command}
              </pre>
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div
          className="tc-card-title"
          style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", rowGap: 6 }}
        >
          待转码列表（{staged.length}）
          <span style={{ flex: 1 }} />
          {scanning && <span style={{ fontSize: 11, color: "var(--monet-on-surface-variant)" }}>扫描中…</span>}
          <button className="btn btn-text" disabled={busy || staged.length === 0} onClick={() => setStaged([])}>
            清空
          </button>
          <button className="btn btn-raised" disabled={busy || staged.length === 0 || !presetId} onClick={() => void enqueueAll()}>
            {busy ? "提交中…" : `加入队列（${staged.length}）`}
          </button>
        </div>
        <div style={{ padding: "0 10px 6px", fontSize: 11, color: "var(--monet-on-surface-variant)" }}>
          这里只是暂存：点「加入队列」才会带着当前预设提交并开始转码。
        </div>
        <div className="comp-row">
          <span className="comp-label">输出位置</span>
          <span className="comp-value mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {outputLocation}
          </span>
        </div>
        {staged.length === 0 ? (
          <div className="empty-state" style={{ padding: 18 }}>
            <span>在左侧点文件「加入」或文件夹「添加」，也可以「添加当前文件夹」</span>
          </div>
        ) : (
          <div className="list-body scroll-hover" style={{ maxHeight: 320 }}>
            {staged.map((item) => (
              <div
                key={item.entry.path}
                className="tlli"
                style={{ padding: "7px 10px", alignItems: "flex-start", cursor: "default" }}
              >
                <span className="tlli-text" style={{ minWidth: 0 }}>
                  <span className="tlli-title">{item.entry.name}</span>
                  <span className="tlli-subtitle mono">{item.entry.path} · {fmtBytes(item.entry.size)}</span>
                  <span className="tlli-subtitle" style={{ overflowWrap: "anywhere" }}>
                    输出：
                    <span className="mono" style={{ color: item.error ? "var(--monet-error)" : undefined }}>
                      {item.error ? item.error : item.outputAbsolute || "计算中…"}
                    </span>
                  </span>
                </span>
                <button
                  className="btn btn-text"
                  style={{ padding: "2px 6px", fontSize: 11 }}
                  onClick={() => removeStaged(item.entry.path)}
                >
                  移除
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <DirectoryPicker
        open={pickerOpen}
        initial={outputDir}
        mediaRoot={mediaRoot}
        onClose={() => setPickerOpen(false)}
        onSelect={(path) => {
          setOutputDir(path);
          setPickerOpen(false);
        }}
      />
    </div>
  );
}
