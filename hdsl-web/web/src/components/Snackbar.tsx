import { createPortal } from "react-dom";
import { CheckCircleIcon, CloseIcon, ErrorIcon, InfoIcon } from "./icons";
import { dismissToast, useAppState } from "../store";

/** Snackbar：底部居中、inverse-surface 底、inverse-on-surface 文字（store.toasts 驱动）。 */
export function SnackbarHost() {
  const toasts = useAppState().toasts;
  if (toasts.length === 0) return null;
  return createPortal(
    <div className="snackbar-host">
      {toasts.map((t) => (
        <div className="snackbar" key={t.id}>
          {t.kind === "success" ? (
            <CheckCircleIcon size={16} />
          ) : t.kind === "error" ? (
            <ErrorIcon size={16} />
          ) : (
            <InfoIcon size={16} />
          )}
          <span>{t.text}</span>
          <button className="snackbar-close" onClick={() => dismissToast(t.id)} aria-label="关闭">
            <CloseIcon size={14} />
          </button>
        </div>
      ))}
    </div>,
    document.body,
  );
}
