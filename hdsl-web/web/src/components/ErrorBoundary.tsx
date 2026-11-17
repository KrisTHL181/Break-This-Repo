import { Component } from "react";
import type { ReactNode } from "react";
import { ErrorIcon } from "./icons";

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/** 渲染层最后防线：任何子组件的渲染/生命周期崩溃都被拦在这里，
 *  给出一个可恢复的错误页，而不是让整棵 React 树卸载成白屏。 */
export class ErrorBoundary extends Component<Props, State> {
  override state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  override componentDidCatch(error: Error): void {
    console.error("render crash caught by ErrorBoundary", error);
  }

  override render() {
    if (this.state.error) {
      return (
        <div className="screen-full">
          <div className="screen-error-card">
            <ErrorIcon size={30} style={{ color: "var(--monet-error)" }} />
            <h2>页面出错了</h2>
            <p>{this.state.error.message}</p>
            <button className="btn btn-raised ripple-host" onClick={() => window.location.reload()}>
              刷新页面
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
