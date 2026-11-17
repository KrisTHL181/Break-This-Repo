import { useState } from "react";
import { api } from "../api";
import { toast } from "../store";
import type { TaskRec } from "../types";
import { errMsg } from "../utils";
import { WarningIcon } from "./icons";
import { Dialog } from "./Dialog";

/**
 * build-script 审批对话框：任务进入 waiting_approval 时弹出（HMCL 弹窗样式）。
 * 不可点遮罩关闭——必须显式允许或拒绝，任务才会继续/中止。
 */
export function ApprovalDialog({ task }: { task: TaskRec | null }) {
  const [busy, setBusy] = useState(false);
  if (!task) return null;
  const keys = task.approval?.keys ?? [];

  const respond = async (allow: boolean) => {
    setBusy(true);
    try {
      await api.approveTask(task.id, allow);
      toast("info", allow ? "已允许，任务继续执行" : "已拒绝，任务将中止");
    } catch (e) {
      toast("error", `审批提交失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title="构建脚本审批"
      closeOnBackdrop={false}
      hideClose
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 10, maxWidth: 420 }}>
        <div style={{ display: "flex", gap: 8, alignItems: "flex-start", lineHeight: 1.6 }}>
          <WarningIcon size={20} style={{ color: "var(--monet-tertiary)", flex: "none", marginTop: 2 }} />
          <span>
            这些插件要求执行构建脚本。构建脚本会在安装过程中以当前用户身份执行任意命令，
            请确认你信任这些插件的来源后再放行。
          </span>
        </div>
        {keys.length > 0 && (
          <div className="card-opaque" style={{ padding: 8, display: "flex", flexDirection: "column", gap: 4 }}>
            {keys.map((k) => (
              <code key={k} style={{ fontSize: 11, wordBreak: "break-all" }}>
                {k}
              </code>
            ))}
          </div>
        )}
      </div>
      <div className="dialog-actions">
        <button className="btn btn-text danger" disabled={busy} onClick={() => void respond(false)}>
          拒绝
        </button>
        <button className="btn btn-text" disabled={busy} onClick={() => void respond(true)}>
          {busy ? "提交中…" : "允许并继续"}
        </button>
      </div>
    </Dialog>
  );
}
