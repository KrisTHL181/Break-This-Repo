// 子路径挂载支持（面板本身被反向代理在 /panel 之类的挂载点下，HDSL_BASE_PATH）。
// 后端在 index.html 里注入 <base href> 和 window.__HDSL_BASE__；本模块是唯一读取口。
// 规则：fetch/资源用相对路径（交给 <base> 解析），导航/开窗用 withBase 显式前缀。

/** 面板挂载点："" = 根，"/panel" = 子路径。 */
export function basePath(): string {
  return (window as unknown as { __HDSL_BASE__?: string }).__HDSL_BASE__ ?? "";
}

/**
 * 给根绝对路径加挂载点前缀（"/i/<id>/" → "/panel/i/<id>/"）。
 *
 * 三个边界都要挡住，否则终端 WebSocket 这类路径会拼错：挂载点带尾斜杠、
 * 传入路径不带前导斜杠、服务端已经把前缀带上（重复会变成 /panel/panel/…）。
 */
export function withBase(path: string): string {
  const base = basePath().replace(/\/+$/, "");
  if (!base) return path;
  const rest = path.startsWith("/") ? path : "/" + path;
  if (rest === base || rest.startsWith(base + "/")) return rest;
  return base + rest;
}
