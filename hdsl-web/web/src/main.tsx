import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { installRipple } from "./components/Ripple";
import { bindSystemThemeListener, initTheme } from "./theme";
import { basePath } from "./base";
import "./styles/monet.css";
import "./styles/base.css";
import "./styles/components.css";
import "./styles/layout.css";
import "./styles/transcode.css";
import "./styles/chat.css";
import "./styles/mobile.css";
import "katex/dist/katex.min.css";

// Monet 主题：启动时用种子 #5C6BC0 生成亮/暗两套 --monet-* 变量（FIDELITY · Spec 2025）
initTheme();
bindSystemThemeListener();
installRipple();

const rootEl = document.getElementById("root");
if (!rootEl) throw new Error("找不到 #root 挂载点");

createRoot(rootEl).render(
  <StrictMode>
    <BrowserRouter basename={basePath() || undefined}>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
