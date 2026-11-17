/** 点附件时用聊天侧栏预览（默认开）；关闭后回退到新标签页。 */
const KEY = "chat.attachmentPreview";

export function readAttachmentPreview(): boolean {
  try {
    const value = localStorage.getItem(KEY);
    return value === null ? true : value === "1";
  } catch {
    return true;
  }
}

export function writeAttachmentPreview(enabled: boolean): void {
  try {
    localStorage.setItem(KEY, enabled ? "1" : "0");
  } catch {
    // 隐私模式禁用 localStorage 时保持默认行为，不阻塞保存。
  }
}
