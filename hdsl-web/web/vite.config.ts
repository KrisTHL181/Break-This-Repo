import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// base 必须是绝对根路径：SPA 用 BrowserRouter、API/WS/静态资源也都是绝对根路径，
// 面板只挂在根下（HdslServer 的 contextPath 就是 "/"；README 里那个「剥前缀」指的是
// /i/{id}/* 反代到 dsh 实例时剥前缀，与面板自身无关）。
//
// 曾经用相对 base "./"，那会让深链接（如 /instances/xxx）把 ./assets/… 解析成
// /instances/assets/…，落到 SPA 兜底返回 text/html，模块脚本被浏览器拒绝执行 → 白屏。
// 手机端刷新页面、把某个实例的链接加到主屏幕都会踩到，所以改成绝对路径。
export default defineConfig({
  base: "/",
  plugins: [react()],
  build: {
    outDir: "dist",
    target: "es2020",
    // 不要压缩输出：esbuild 的语法压缩会破坏 xterm 6.0.0 里
    // `return f=…, p=…; var f,p` 这种写法，使 DECRQM（CSI ? Ps $ p）抛
    // ReferenceError，xterm 的写入队列随即停摆——opencode 启动时会连发 6 个
    // DECRQM，整个终端画面因此永远空白。体积代价实测约 +70 KB gzip，换来
    // 供应商代码原样执行。
    minify: false,
  },
  server: {
    proxy: {
      "/api": {
        target: "http://127.0.0.1:3080",
        // 保留 Host 头原样透传（后端与 dsh 反代都依赖 Host）
        changeOrigin: false,
      },
      "/ws": {
        target: "http://127.0.0.1:3080",
        ws: true,
        changeOrigin: false,
      },
      // 正则 key（^ 开头）：只匹配 /i、/i/…，避免把 SPA 自身的 /instances 路由误代理
      "^/i($|/)": {
        target: "http://127.0.0.1:3080",
        ws: true,
        changeOrigin: false,
      },
    },
  },
});
