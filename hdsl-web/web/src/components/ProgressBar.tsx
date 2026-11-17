/** 进度条：track secondary-container、bar primary（root.css .jfx-progress-bar）。 */
export function ProgressBar({ fraction }: { fraction?: number | null }) {
  const known = typeof fraction === "number" && Number.isFinite(fraction);
  return (
    <div className={`progress${known ? "" : " indeterminate"}`} role="progressbar">
      <div
        className="bar"
        style={known ? { width: `${Math.round(Math.min(1, Math.max(0, fraction)) * 1000) / 10}%` } : undefined}
      />
    </div>
  );
}
