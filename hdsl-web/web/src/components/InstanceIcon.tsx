import type { Instance } from "../types";
import { normState } from "../utils";
import { BrandIcon } from "./BrandIcon";

/** 实例图标：icon 是可访问 URL 时显示图片，否则显示 dsh 的品牌标志（DeepSeek 鲸鱼）。
 *
 *  原先的兜底是 HMCL 的草方块 + 按名字取 `grad-a`…`grad-d` 做色相区分——那四个类在
 *  CSS 里从来没有定义过（桌面版移植时的残留），这次换标志时一并删掉。 */
export function InstanceIcon({ inst, size = 32 }: { inst: Instance; size?: number }) {
  const iconUrl = inst.icon;
  const showImg = !!iconUrl && /^(https?:|\/|data:)/.test(iconUrl);
  return (
    <span
      className="side-graphic lg"
      style={{ width: size, height: size, borderRadius: Math.max(2, Math.round(size * 0.12)) }}
      title={inst.name}
    >
      {showImg ? <img src={iconUrl} alt="" /> : <BrandIcon brand="dsh" size={size} />}
    </span>
  );
}

/** 实例状态徽章（实例列表行副标题用）。 */
export function StateBadge({ state }: { state: string }) {
  const st = normState(state);
  if (st === "RUNNING") return <span className="tag running">{stateLabel(st)}</span>;
  if (st === "FAILED") return <span className="tag failed">{stateLabel(st)}</span>;
  if (st === "NOT_INSTALLED") return <span className="tag">{stateLabel(st)}</span>;
  if (st === "INSTALLING" || st === "STARTING" || st === "STOPPING")
    return <span className="tag warn">{stateLabel(st)}</span>;
  return null;
}

export function stateLabel(state: string): string {
  switch (normState(state)) {
    case "NOT_INSTALLED":
      return "未安装";
    case "INSTALLING":
      return "安装中";
    case "STARTING":
      return "启动中";
    case "RUNNING":
      return "运行中";
    case "STOPPING":
      return "停止中";
    case "FAILED":
      return "失败";
    default:
      return "已停止";
  }
}
