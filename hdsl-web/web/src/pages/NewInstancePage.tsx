import { useLocation, useNavigate, Navigate } from "react-router-dom";
import { isTuiBrand } from "../brandKind";
import { BrandIcon } from "../components/BrandIcon";
import { I18N } from "../i18n";
import type { BrandId } from "../types";

/** 品牌卡片的静态说明：只讲“这会把网页/终端带到哪里”，不重复品牌页的长介绍。 */
const BRANDS: { id: BrandId | "zcode"; title: string; description: string }[] = [
  { id: "kimi", title: I18N["dsh.brand.kimi"], description: "Kimi Code 的网页界面；终端走它自己的 TUI。" },
  { id: "opencode", title: I18N["dsh.brand.opencode"], description: "OpenCode 的网页工作台；适合终端里那套 OpenCode 用法。" },
  { id: "claude-web", title: I18N["dsh.brand.claude-web"], description: "Claude Code 的社区网页工作台，可接官方账号或中转。" },
  { id: "octop", title: I18N["dsh.brand.octop"], description: "腾讯云 Octop：多用户 AI 助手面板，自带 Python 运行时。" },
  { id: "hermes", title: I18N["dsh.brand.hermes"], description: "Hermes Agent：带 OpenAI 兼容网关的网页仪表盘。" },
  { id: "pi-web", title: I18N["dsh.brand.pi-web"], description: "Pi Coding Agent 的网页版；终端为主，网页是添头。" },
  { id: "qwen", title: I18N["dsh.brand.qwen"], description: "Qwen Code 的终端 TUI；面板负责安装与实例隔离，没有网页。" },
  { id: "mimo", title: I18N["dsh.brand.mimo"], description: "MiMo Code 的终端 TUI；面板负责安装与实例隔离，没有网页。" },
  { id: "minimax", title: I18N["dsh.brand.minimax"], description: "MiniMax Code 的终端 TUI；面板负责安装与实例隔离，没有网页。" },
  { id: "zcode", title: I18N["dsh.brand.zcode"], description: "实验品类：先进 ZCode 标签构建发行包，再创建实例。" },
];

/**
 * 新建实例的第一站：先选品牌，再进入该品牌自己的创建流程。
 *
 * 以前 `/instances/new` 直接是 dsh 的版本向导，第一次来的用户会以为面板只能跑 dsh。
 * 这里不重复每个品牌的版本/参数步骤——dsh 继续走原来的向导，品牌跳到品牌页并
 * 展开「安装新版本 / 创建实例」，进入各自的链路。
 */
export function NewInstancePage() {
  const nav = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  // 旧深链（`/instances/new?version=…`、`?step=2`）仍然直达 dsh 向导。
  if (params.get("version") || params.get("step")) {
    return <Navigate to={`/instances/new/dsh${location.search}`} replace />;
  }

  return (
    <div className="page-content scroll" style={{ gap: 10 }}>
      <div className="card" style={{ padding: "14px 16px" }}>
        <h2 style={{ margin: 0 }}>创建实例</h2>
        <p className="dim" style={{ margin: "6px 0 0", lineHeight: 1.7 }}>
          先选一个品牌。dsh 是面板自带的那套；其余品牌由各自上游提供，面板只负责安装、启动和反代。
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
          gap: 10,
        }}
      >
        <button
          className="card ripple-host"
          style={{ padding: 14, textAlign: "left", display: "grid", gap: 6 }}
          onClick={() => nav("/instances/new/dsh")}
        >
          <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <BrandIcon brand="dsh" size={28} />
            <b>DeepSeek Harness</b>
          </span>
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
            dsh 原版实例：选版本、创建、启动，账户与插件都跟着实例走。
          </span>
        </button>

        {BRANDS.map((brand) => (
          <button
            key={brand.id}
            className="card ripple-host"
            style={{ padding: 14, textAlign: "left", display: "grid", gap: 6 }}
            onClick={() =>
              nav(`/instances?view=${isTuiBrand(brand.id) ? "tui" : "brands"}&brand=${brand.id}&create=1`)
            }
          >
            <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <BrandIcon brand={brand.id} size={28} />
              <b>{brand.title}</b>
            </span>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", lineHeight: 1.6 }}>
              {brand.description}
            </span>
          </button>
        ))}
      </div>

      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <button className="btn btn-text" onClick={() => nav("/instances")}>
          返回实例列表
        </button>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
          Minecraft 服务器在「实例列表 → MC 服务器实例」里创建，不在品牌列表里。
        </span>
      </div>
    </div>
  );
}
