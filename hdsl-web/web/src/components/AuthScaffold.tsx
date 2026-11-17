import type { ReactNode } from "react";
import { useWallpaperUrl } from "../wallpaper";

/** 认证页公共骨架：壁纸背景 + 居中 HMCL 对话框式卡片（surface-container-high、圆角 4、20px 标题）。
 *  登录页与首次启动引导页共用，仅表单内容不同。 */
export function AuthScaffold({ title, children }: { title: string; children: ReactNode }) {
  // 认证页没登录，拿不到用户上传的壁纸列表；这里只会解析到内置的那几张
  const wallpaperUrl = useWallpaperUrl();
  return (
    <div className="page-root" style={{ animation: "none" }}>
      <div
        className="wallpaper-bg"
        style={{ backgroundImage: `url(${wallpaperUrl})` }}
      />
      <div
        style={{
          position: "relative",
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <div
          className="dialog auth-card"
          style={{ position: "static", animation: "dialog-in 200ms var(--ease)" }}
        >
          <div className="dialog-title">{title}</div>
          {children}
        </div>
      </div>
    </div>
  );
}
