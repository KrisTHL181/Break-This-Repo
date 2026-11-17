import { useEffect, useMemo, useRef, useState } from "react";
import {
  api,
  type TranscodeJob,
  type TranscodePresetSummary,
} from "../../api";
import { ProgressBar } from "../../components/ProgressBar";
import { Dialog } from "../../components/Dialog";
import { toast } from "../../store";
import { errMsg } from "../../utils";
import { RefreshIcon } from "../../components/icons";
import { fmtBytes, fmtDuration, pct, stateLabel, stateTone, HelpNote } from "./common";
import { valueLabel } from "./labels";
import { useQueueStream } from "./queue/useQueueStream";
import { QueueToolbar, type QueueBatchAction } from "./queue/QueueToolbar";
import { QueueDetail } from "./queue/QueueDetail";

/**
 * 编码队列：一张表 + 一条控制条 + 一个详情面板。
 *
 * 交互模型照 3FUI：先勾选，再动作。所有动词都在控制条上，行里只剩「日志」——
 * 一行不会被五六个按钮挤掉任务名，批量操作也才有落点。
 *
 * 按钮可用性全部来自服务端下发的 `caps`，前端不再维护状态机的第二份实现：
 * 之前 `ACTIVE_STATES` 与后端 `removable` 已经漂移过一次。
 */
export function QueuePage({ onOpenTab }: { onOpenTab: (tab: string) => void }) {
  const { snapshot, error, refresh, setSnapshot } = useQueueStream();
  const [selected, setSelected] = useState<string[]>([]);
  const [active, setActive] = useState<string | null>(null);
  const [log, setLog] = useState("");
  const [logDialog, setLogDialog] = useState<string | null>(null);
  const [dialogLog, setDialogLog] = useState("");
  const [logFilter, setLogFilter] = useState<"all" | "error">("all");
  const [busy, setBusy] = useState(false);
  const [presets, setPresets] = useState<TranscodePresetSummary[]>([]);
  const [resetTargets, setResetTargets] = useState<string[] | null>(null);
  const [resetPresetId, setResetPresetId] = useState("");
  const [resetError, setResetError] = useState<string | null>(null);
  const dialogLogRef = useRef<HTMLPreElement | null>(null);
  const anchorRef = useRef<string | null>(null);

  const jobs = snapshot?.jobs ?? [];
  // 任务被移除后，选择集与详情目标都可能指向已经不存在的 id。
  const selectedIds = useMemo(
    () => selected.filter((id) => jobs.some((job) => job.id === id)),
    [selected, jobs],
  );
  const finishedCount = jobs.filter(isFinished).length;
  const activeJob = jobs.find((job) => job.id === active) ?? null;
  const waiting = jobs.find((job) => job.state === "waiting_decision") ?? null;
  const onlySelected = selectedIds.length === 1
    ? jobs.find((job) => job.id === selectedIds[0]) ?? null
    : null;
  const runState = snapshot?.runState ?? "idle";
  const nextQueued = jobs.find((job) => job.state === "queued") ?? null;

  /// Why the selected job cannot move by `delta`, or null when it can. The server pins
  /// running / paused / waiting-for-decision jobs to their slot, so a swap with such a
  /// neighbour would be refused as a whole — better to disable that direction here and
  /// say why than to let the click fail with a 409.
  const moveBlocker = (delta: number): string | null => {
    if (!onlySelected) return "只选一个任务才能调整顺序";
    if (!onlySelected.caps.reorder) return "正在执行 / 等待决定的任务不能移动";
    const from = jobs.findIndex((job) => job.id === onlySelected.id);
    const to = from + delta;
    if (to < 0) return "已经在最前面";
    if (to >= jobs.length) return "已经在最后面";
    if (!jobs[to].caps.reorder) return "不能跨过正在执行 / 等待决定的任务";
    return null;
  };

  const stats = useMemo(() => ({
    running: jobs.filter((job) => job.state === "running" || job.state === "paused").length,
    queued: jobs.filter((job) => job.state === "queued").length,
    done: jobs.filter((job) => job.state === "done").length,
    failed: jobs.filter((job) => job.state === "failed" || job.state === "waiting_decision").length,
  }), [jobs]);

  // 首次加载后把详情面板落到一个真实任务上，避免右栏空着（旧版的行为）。
  useEffect(() => {
    if (active && jobs.some((job) => job.id === active)) return;
    const next = jobs.find((job) => job.state === "running")
      ?? jobs.find((job) => job.state === "waiting_decision")
      ?? jobs[0];
    if (next) setActive(next.id);
  }, [active, jobs]);

  // 详情区的实时输出：只拉当前任务的日志尾部。
  useEffect(() => {
    if (!active) {
      setLog("");
      return;
    }
    let cancelled = false;
    const pull = async () => {
      try {
        const answer = await api.transcode.jobLog(active, 80);
        if (!cancelled) setLog(answer.log);
      } catch {
        // 日志被清理时保留最后一份，不要清空。
      }
    };
    void pull();
    const timer = window.setInterval(() => void pull(), 1500);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [active]);

  /* ── 队列动作 ─────────────────────────────────────────────────── */

  const queueAction = async (action: "start" | "pause" | "stop") => {
    setBusy(true);
    try {
      const next = action === "start"
        ? await api.transcode.startQueue()
        : action === "pause"
          ? await api.transcode.pauseQueue()
          : await api.transcode.stopQueue();
      setSnapshot(next);
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const batch = async (action: QueueBatchAction, ids: string[] = selectedIds) => {
    if (ids.length === 0) return;
    if (action === "remove" && !window.confirm(
      `移除选中的 ${ids.length} 条记录？已生成的输出文件不会删除，这些任务的日志会一起删掉。`,
    )) return;
    setBusy(true);
    try {
      const result = await api.transcode.queueBatch(action, ids);
      setSnapshot(result.queue);
      const skipped = result.results.filter((item) => !item.ok).length;
      if (skipped === 0) {
        toast("success", `已处理 ${result.applied} 条任务`);
      } else {
        toast("error", `${result.applied} 条已处理，${skipped} 条被跳过（状态不允许）`);
      }
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const move = async (delta: number) => {
    if (!snapshot || moveBlocker(delta) != null) return;
    const ids = jobs.map((job) => job.id);
    const from = ids.indexOf(selectedIds[0]);
    const to = from + delta;
    [ids[from], ids[to]] = [ids[to], ids[from]];
    setBusy(true);
    try {
      setSnapshot(await api.transcode.reorderQueue(ids));
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const clearFinished = async () => {
    if (finishedCount === 0) return;
    if (!window.confirm(
      `清除 ${finishedCount} 条已完成 / 失败 / 已取消的记录？已生成的输出文件不会删除，但这些任务的日志会一起删掉。`,
    )) return;
    setBusy(true);
    try {
      setSnapshot(await api.transcode.clearFinished());
      toast("success", `已清除 ${finishedCount} 条记录`);
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const decide = async (id: string, decision: "stop" | "fallback" | "fallback-queue" | "auto") => {
    setBusy(true);
    try {
      setSnapshot(await api.transcode.jobDecision(id, decision));
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  /* ── 重置预设 ─────────────────────────────────────────────────── */

  const openReset = (ids: string[]) => {
    if (ids.length === 0) return;
    setResetError(null);
    setResetTargets(ids);
    setResetPresetId(ids.length === 1
      ? jobs.find((job) => job.id === ids[0])?.presetId ?? ""
      : "");
    void api.transcode.presets()
      .then((answer) => setPresets(answer.presets))
      .catch((e) => setResetError(errMsg(e)));
  };

  const submitReset = async () => {
    if (!resetTargets || !resetPresetId) return;
    setBusy(true);
    setResetError(null);
    try {
      if (resetTargets.length === 1) {
        setSnapshot(await api.transcode.resetJobPreset(resetTargets[0], resetPresetId));
        toast("success", "已改用新预设，任务回到排队状态");
      } else {
        const result = await api.transcode.syncQueuePreset(resetPresetId, resetTargets);
        setSnapshot(result.queue);
        const skipped = result.skippedTool + result.skippedActive + result.skippedTerminal + result.failed;
        toast(
          skipped === 0 ? "success" : "error",
          `已更新 ${result.updated} 条${skipped > 0 ? `，${skipped} 条被跳过（工具任务 / 已开始 / 已结束）` : ""}`,
        );
      }
      setResetTargets(null);
    } catch (e) {
      setResetError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  /* ── 选择与快捷键 ─────────────────────────────────────────────── */

  const toggle = (id: string) => {
    setSelected((current) => current.includes(id)
      ? current.filter((item) => item !== id)
      : [...current, id]);
    setActive(id);
    anchorRef.current = id;
  };

  const pick = (id: string, event: { metaKey: boolean; ctrlKey: boolean; shiftKey: boolean }) => {
    if (event.shiftKey && anchorRef.current) {
      const from = jobs.findIndex((job) => job.id === anchorRef.current);
      const to = jobs.findIndex((job) => job.id === id);
      if (from >= 0 && to >= 0) {
        const [start, end] = from < to ? [from, to] : [to, from];
        setSelected(jobs.slice(start, end + 1).map((job) => job.id));
        setActive(id);
        return;
      }
    }
    if (event.metaKey || event.ctrlKey) {
      toggle(id);
      return;
    }
    setSelected([id]);
    setActive(id);
    anchorRef.current = id;
  };

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable)) {
        return;
      }
      // 弹窗打开时不响应队列快捷键，避免 Delete 误删任务。
      if (logDialog || resetTargets) return;
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      if (selectedIds.length === 0) return;
      if (event.key === "Enter") {
        event.preventDefault();
        void batch("start");
      } else if (event.key === "Delete") {
        event.preventDefault();
        void batch("remove");
      } else if (event.key === " ") {
        event.preventDefault();
        const allPaused = selectedIds.every(
          (id) => jobs.find((job) => job.id === id)?.state === "paused",
        );
        void batch(allPaused ? "resume" : "pause");
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedIds, jobs, logDialog, resetTargets]);

  /* ── 日志弹窗 ─────────────────────────────────────────────────── */

  const pullDialogLog = async (id: string) => {
    try {
      const answer = await api.transcode.jobLog(id, 600);
      const lines = answer.log.split(/\r?\n/);
      const filtered = logFilter === "error"
        ? lines.filter((line) => /error|failed|invalid|cannot|not supported|退出码|错误/i.test(line))
        : lines;
      setDialogLog(lines.length === 0 ? "" : filtered.join("\n"));
      window.setTimeout(() => {
        if (dialogLogRef.current) dialogLogRef.current.scrollTop = dialogLogRef.current.scrollHeight;
      }, 30);
    } catch (e) {
      toast("error", errMsg(e));
    }
  };

  useEffect(() => {
    if (logDialog) void pullDialogLog(logDialog);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [logDialog, logFilter]);

  const copy = (text: string, label: string) => {
    if (!text) return toast("error", `${label}为空`);
    void navigator.clipboard?.writeText(text).then(
      () => toast("success", `${label}已复制`),
      () => toast("error", "复制失败（需要 HTTPS 或 localhost）"),
    );
  };

  const seqBadge = (job: TranscodeJob): string | null => {
    if (job.state === "running" || job.state === "paused" || job.state === "stopping") return "当前";
    if (job.state === "queued") return job.id === nextQueued?.id ? "下一个" : null;
    return null;
  };

  const queuedIndex = (job: TranscodeJob): number | null => {
    if (job.state !== "queued") return null;
    const index = jobs.filter((item) => item.state === "queued").findIndex((item) => item.id === job.id);
    return index >= 0 ? index + 1 : null;
  };

  const resetTargetJob = resetTargets && resetTargets.length === 1
    ? jobs.find((job) => job.id === resetTargets[0]) ?? null
    : null;

  return (
    <div className="tc-queue">
      {error && <div className="card tc-error">{error}</div>}

      <div className="card tc-queue-head">
        <div className="tc-stat-row">
          <span className={`tc-stat${stats.running > 0 ? " active" : ""}`}>
            <b>{stats.running}</b>运行
          </span>
          <span className="tc-stat"><b>{stats.queued}</b>排队</span>
          <span className="tc-stat"><b>{stats.done}</b>完成</span>
          <span className={`tc-stat${stats.failed > 0 ? " danger" : ""}`}><b>{stats.failed}</b>失败/待决定</span>
          <span className="tc-stat-meta">
            并发 {snapshot?.maxConcurrent ?? "—"} · 上限 {snapshot?.maxQueue ?? "—"} ·
            队列{runState === "running" ? "运行中" : runState === "paused" ? "已暂停" : "未启动"} · 关网页不停任务
          </span>
        </div>
        <div className="tc-queue-actions">
          <button className="btn btn-text" onClick={() => void refresh()}>
            <RefreshIcon size={16} />
            刷新
          </button>
        </div>
      </div>

      <HelpNote title="队列怎么用">
        <ul>
          <li><b>先勾选，再动作</b>：控制条上的开始 / 暂停 / 恢复 / 停止 / 移除 / 重置预设都作用于勾选的任务；快捷键 <b>Enter</b>=开始、<b>Delete</b>=移除、<b>空格</b>=暂停/恢复（弹窗打开时不生效）。</li>
          <li><b>队列开始 / 队列暂停 / 停止全部</b>作用于整个队列：暂停只是不再启动新任务，正在跑的不受影响；停止全部会终止 ffmpeg，已完成的输出保留。</li>
          <li><b>开始</b>（勾选后）把排队任务挪到队首并立刻尝试启动，仍受并发上限约束；<b>上移 / 下移</b>只改执行顺序，不改输出文件名（入队时就已定名）。正在跑的任务不能移动。</li>
          <li><b>移除</b>只删队列记录与任务日志，<b>不删磁盘上已生成的输出文件</b>；运行中或已暂停的任务必须先停止才能移除。</li>
          <li><b>重置预设</b>：任务保存的是提交那一刻的参数快照，之后改预设不会自动生效。批量重置只影响还没开始的任务；进度、日志与失败决策清空，输出名按新容器重排（旧产物不覆盖，新名自动加序号）。</li>
          <li>任务失败会停在这里等你决定，也可以直接「重置预设」换个编码参数重来。双击行：已完成复制输出路径，其他打开日志。</li>
        </ul>
      </HelpNote>

      {waiting && (
        <div className="card tc-decision">
          <div className="tc-decision-title">编码失败，等待决定</div>
          <div className="tc-decision-body mono">{waiting.input} · {waiting.encoder ?? "—"}</div>
          <pre className="tc-decision-error">{waiting.error}</pre>
          <div className="tc-decision-actions">
            <button className="btn btn-text" disabled={busy} onClick={() => void decide(waiting.id, "stop")}>
              停止此任务
            </button>
            <button className="btn btn-text" disabled={busy} onClick={() => void decide(waiting.id, "fallback")}>
              仅此任务降级
            </button>
            <button className="btn btn-raised" disabled={busy} onClick={() => void decide(waiting.id, "fallback-queue")}>
              降级并应用队列
            </button>
            <button className="btn btn-text" disabled={busy} onClick={() => void decide(waiting.id, "auto")}>
              完全自动化
            </button>
          </div>
        </div>
      )}

      <div className="tc-queue-body">
        <div className="tc-queue-left">
          <QueueToolbar
            runState={runState}
            total={jobs.length}
            selectedCount={selectedIds.length}
            finishedCount={finishedCount}
            moveUpBlocker={moveBlocker(-1)}
            moveDownBlocker={moveBlocker(1)}
            busy={busy}
            onQueueAction={(action) => void queueAction(action)}
            onBatch={(action) => void batch(action)}
            onMove={(delta) => void move(delta)}
            onSelectAll={() => setSelected(
              selectedIds.length === jobs.length && jobs.length > 0 ? [] : jobs.map((job) => job.id),
            )}
            onSelectErrors={() => {
              const failed = jobs.filter((job) => job.state === "failed" || job.state === "waiting_decision");
              setSelected(failed.map((job) => job.id));
              if (failed.length === 0) toast("success", "没有失败任务");
            }}
            onResetPreset={() => openReset(selectedIds)}
            onClearFinished={() => void clearFinished()}
          />

          <div className="card tc-queue-list">
            {jobs.length === 0 && (
              <div className="empty-state">
                <span>队列是空的</span>
                <span className="empty-sub">在「准备文件」里选择文件并加入队列</span>
              </div>
            )}
            {jobs.map((job) => {
              const badge = seqBadge(job);
              const index = queuedIndex(job);
              return (
                <div
                  key={job.id}
                  className={`tc-queue-row${selectedIds.includes(job.id) ? " picked" : ""}${active === job.id ? " selected" : ""}${job.state === "waiting_decision" ? " danger" : ""}`}
                  onClick={(event) => pick(job.id, event)}
                  onDoubleClick={() => {
                    if (job.state === "done") copy(job.output, "输出路径");
                    else setLogDialog(job.id);
                  }}
                >
                  <span
                    className="tc-queue-check"
                    onClick={(event) => { event.stopPropagation(); toggle(job.id); }}
                  >
                    <input type="checkbox" checked={selectedIds.includes(job.id)} readOnly aria-label={`选择 ${job.input}`} />
                  </span>
                  <span className={`tc-seq${badge === "当前" ? " now" : badge === "下一个" ? " next" : ""}`}>
                    {badge ?? (index != null ? `#${index}` : "")}
                  </span>
                  <span className="tc-queue-main">
                    <span className="tc-queue-name" title={job.input}>{job.input}</span>
                    <span className="tc-queue-sub">
                      <span className={`tag ${stateTone(job.state)}`}>{stateLabel(job.state)}</span>
                      <span className="mono">{job.encoder ? valueLabel("videoEncoder", job.encoder) : job.presetName ?? "—"}</span>
                      {job.fallbackAttempted && <span className="tag plain">已降级</span>}
                      {job.output && <span className="tc-queue-out mono" title={job.output}>{job.output}</span>}
                    </span>
                    <span className="tc-queue-progress">
                      <ProgressBar fraction={job.fraction >= 0 ? job.fraction : null} />
                    </span>
                  </span>
                  <span className="tc-queue-metrics mono">
                    <span>{pct(job.fraction)}</span>
                    <span>{job.speed > 0 ? `${job.speed.toFixed(1)}x` : "—"}</span>
                    <span>{job.fps > 0 ? `${job.fps.toFixed(0)} fps` : "—"}</span>
                    <span>{fmtDuration(job.outTimeMs)} / {fmtDuration(job.durationMs)}</span>
                    <span>{fmtBytes(job.sizeBytes)}</span>
                  </span>
                  <span className="tc-queue-row-actions" onClick={(event) => event.stopPropagation()}>
                    <button className="btn btn-text" onClick={() => setLogDialog(job.id)}>日志</button>
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        <aside className="card tc-queue-detail">
          {activeJob ? (
            <QueueDetail
              job={activeJob}
              log={log}
              onOpenLog={setLogDialog}
              onCopy={copy}
              onResetPreset={(id) => openReset([id])}
            />
          ) : (
            <div className="tc-empty">勾选一个任务查看详情</div>
          )}
        </aside>
      </div>

      <Dialog
        open={resetTargets !== null}
        title={resetTargets && resetTargets.length > 1 ? `重置 ${resetTargets.length} 条任务的预设` : "重置使用预设"}
        onClose={() => setResetTargets(null)}
        footer={
          <>
            <button className="btn btn-text" onClick={() => { setResetTargets(null); onOpenTab("presets"); }}>
              打开参数面板
            </button>
            <span style={{ flex: 1 }} />
            <button className="btn btn-text" onClick={() => setResetTargets(null)}>取消</button>
            <button className="btn btn-raised" disabled={busy || !resetPresetId} onClick={() => void submitReset()}>
              {busy ? "处理中…" : "应用"}
            </button>
          </>
        }
      >
        <div style={{ display: "grid", gap: 10, padding: "2px 4px 6px" }}>
          <div className="mono" style={{ fontSize: 11, overflowWrap: "anywhere" }}>
            {resetTargetJob?.input ?? (resetTargets ? `${resetTargets.length} 条选中任务` : "")}
          </div>
          {resetTargetJob && (
            <div className="comp-row">
              <span className="comp-label">当前预设</span>
              <span className="comp-value">
                {resetTargetJob.presetName || resetTargetJob.presetId || "—"}
              </span>
            </div>
          )}
          <label style={{ fontSize: 12 }}>改用预设</label>
          <select className="input" value={resetPresetId} onChange={(e) => setResetPresetId(e.target.value)}>
            <option value="">选择参数面板 / 预设…</option>
            {presets.map((preset) => (
              <option key={preset.id} value={preset.id}>
                {preset.name}{preset.builtin ? "（内置）" : "（自定义）"}
              </option>
            ))}
          </select>
          <div style={{ fontSize: 11, lineHeight: 1.7, color: "var(--monet-on-surface-variant)" }}>
            任务保留的是提交那一刻的参数快照，后来改预设不会自动生效。这里选一个预设替换它：
            进度、日志与失败决策清空，输出名按新容器重新规划，任务回到排队状态。
            批量时只影响<b>还没开始</b>的任务（工具任务与已开始 / 已结束的会跳过）；已生成的输出文件不会被覆盖。
            想用还没保存的参数，先点「打开参数面板」另存为预设，再回来选。
          </div>
          {resetError && (
            <div style={{ fontSize: 12, color: "var(--monet-error)", whiteSpace: "pre-wrap" }}>{resetError}</div>
          )}
        </div>
      </Dialog>

      <Dialog
        open={logDialog !== null}
        title="任务日志"
        onClose={() => setLogDialog(null)}
        wide
        footer={
          <>
            <button
              className={`btn ${logFilter === "all" ? "btn-raised" : "btn-text"}`}
              onClick={() => setLogFilter("all")}
            >
              全部
            </button>
            <button
              className={`btn ${logFilter === "error" ? "btn-raised" : "btn-text"}`}
              onClick={() => setLogFilter("error")}
            >
              只看错误
            </button>
            <span style={{ flex: 1 }} />
            <button className="btn btn-text" onClick={() => logDialog && void pullDialogLog(logDialog)}>
              <RefreshIcon size={16} />
              刷新
            </button>
            <button className="btn btn-text" onClick={() => copy(dialogLog, "日志")}>复制</button>
            <button className="btn btn-text" onClick={() => setLogDialog(null)}>关闭</button>
          </>
        }
      >
        <pre className="tc-log-dialog" ref={dialogLogRef}>{dialogLog || "（没有日志）"}</pre>
      </Dialog>
    </div>
  );
}

function isFinished(job: TranscodeJob): boolean {
  return job.state === "done" || job.state === "failed" || job.state === "cancelled";
}
