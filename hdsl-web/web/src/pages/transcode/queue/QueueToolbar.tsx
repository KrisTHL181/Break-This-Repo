import type { TranscodeQueueSnapshot } from "../../../api";

export type QueueBatchAction = "start" | "stop" | "pause" | "resume" | "remove";

interface QueueToolbarProps {
  runState: TranscodeQueueSnapshot["runState"];
  total: number;
  selectedCount: number;
  finishedCount: number;
  /** Why the selection cannot move up/down, or null when it can (server-side `caps.reorder`). */
  moveUpBlocker: string | null;
  moveDownBlocker: string | null;
  busy: boolean;
  onQueueAction: (action: "start" | "pause" | "stop") => void;
  onBatch: (action: QueueBatchAction) => void;
  onMove: (delta: number) => void;
  onSelectAll: () => void;
  onSelectErrors: () => void;
  onResetPreset: () => void;
  onClearFinished: () => void;
}

/// The queue's control strip: two deliberate rows instead of a ragged wrap.
/// Row 1 is whole-queue (and the read-only counter), row 2 is everything that acts on
/// the checked jobs. Row-level buttons were removed on purpose — the model is
/// "select, then act", which is the only way a batch gesture can exist at all.
export function QueueToolbar({
  runState,
  total,
  selectedCount,
  finishedCount,
  moveUpBlocker,
  moveDownBlocker,
  busy,
  onQueueAction,
  onBatch,
  onMove,
  onSelectAll,
  onSelectErrors,
  onResetPreset,
  onClearFinished,
}: QueueToolbarProps) {
  const none = busy || selectedCount === 0;
  return (
    <div className="card tc-queue-toolbar" role="toolbar" aria-label="队列控制">
      <div className="tc-tb-row">
        <button
          className="btn btn-raised tc-tb"
          disabled={busy}
          title={runState === "running" ? "队列已在派发任务" : "允许队列派发任务"}
          onClick={() => onQueueAction("start")}
        >
          {runState === "running" ? "队列运行中" : "队列开始"}
        </button>
        <button
          className="btn btn-text tc-tb"
          disabled={busy}
          title="只是不再启动新任务，正在跑的不受影响"
          onClick={() => onQueueAction("pause")}
        >
          队列暂停
        </button>
        <button
          className="btn btn-text tc-tb danger"
          disabled={busy}
          title="终止所有正在运行的 ffmpeg，已完成的输出保留"
          onClick={() => onQueueAction("stop")}
        >
          停止全部
        </button>
        <span className="tc-tb-sep" />
        <button
          className="btn btn-text tc-tb"
          disabled={busy || finishedCount === 0}
          title={finishedCount === 0 ? "没有已完成 / 失败 / 已取消的记录" : "批量清掉终态记录（日志一起删除）"}
          onClick={onClearFinished}
        >
          清除已完成{finishedCount > 0 ? `（${finishedCount}）` : ""}
        </button>
        <span className="tc-tb-count">
          已选 <b>{selectedCount}</b>/{total}
        </span>
      </div>

      <div className="tc-tb-row">
        <button className="btn btn-text tc-tb" disabled={none} title="选中的排队任务立即开跑（仍受并发上限约束）" onClick={() => onBatch("start")}>
          开始
        </button>
        <button className="btn btn-text tc-tb" disabled={none} title="暂停选中的运行中任务（POSIX）" onClick={() => onBatch("pause")}>
          暂停
        </button>
        <button className="btn btn-text tc-tb" disabled={none} title="恢复选中的已暂停任务" onClick={() => onBatch("resume")}>
          恢复
        </button>
        <button className="btn btn-text tc-tb" disabled={none} title="终止选中任务并保留记录" onClick={() => onBatch("stop")}>
          停止
        </button>
        <button className="btn btn-text tc-tb danger" disabled={none} title="只删队列记录，不删已生成的输出文件" onClick={() => onBatch("remove")}>
          移除
        </button>
        <button className="btn btn-text tc-tb" disabled={none} title="给选中任务换一个预设，只影响还没开始的" onClick={onResetPreset}>
          重置预设
        </button>
        <span className="tc-tb-sep" />
        <button className="btn btn-text tc-tb" disabled={busy || moveUpBlocker != null} title={moveUpBlocker ?? "上移一位"} onClick={() => onMove(-1)}>
          上移
        </button>
        <button className="btn btn-text tc-tb" disabled={busy || moveDownBlocker != null} title={moveDownBlocker ?? "下移一位"} onClick={() => onMove(1)}>
          下移
        </button>
        <span className="tc-tb-sep" />
        <button className="btn btn-text tc-tb" disabled={busy || total === 0} onClick={onSelectAll}>
          {selectedCount === total && total > 0 ? "取消全选" : "全选"}
        </button>
        <button className="btn btn-text tc-tb" disabled={busy || total === 0} title="勾选所有失败 / 待决定的任务" onClick={onSelectErrors}>
          选中错误
        </button>
      </div>
    </div>
  );
}
