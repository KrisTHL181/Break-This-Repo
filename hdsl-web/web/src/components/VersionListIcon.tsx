/** 版本列表行图标：桌面版 dsh-version-list-black/white.svg（亮暗主题切换）。 */
export function VersionListIcon({ size = 32 }: { size?: number }) {
  const base = `assets-img`;
  return (
    <span className="version-list-icon" style={{ width: size, height: size, display: "inline-flex", flex: "none" }}>
      <img
        className="for-light"
        src={`${base}/dsh-version-list-black.svg`}
        alt=""
        style={{ width: "100%", height: "100%" }}
      />
      <img
        className="for-dark"
        src={`${base}/dsh-version-list-white.svg`}
        alt=""
        style={{ width: "100%", height: "100%", display: "none" }}
      />
    </span>
  );
}
