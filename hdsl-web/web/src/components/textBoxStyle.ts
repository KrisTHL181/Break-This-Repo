import type { CSSProperties } from "react";

/** 「关于」页里折叠文本的统一样式：等宽、可滚、保留空白与换行。 */
export const TEXT_BOX_STYLE: CSSProperties = {
  maxHeight: 320,
  overflow: "auto",
  border: "1px solid var(--monet-outline-variant)",
  borderRadius: 4,
  padding: 8,
  marginTop: 6,
  fontSize: 12,
  lineHeight: 1.65,
  whiteSpace: "pre-wrap",
  overflowWrap: "anywhere",
  fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
};
