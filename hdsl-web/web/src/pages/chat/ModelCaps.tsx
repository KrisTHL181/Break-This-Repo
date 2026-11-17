import type { ChatModelCapabilities } from "../../types";

/** 能力来源的中文说法：内置识别 = 按模型名自动判断，不用手改。 */
export function capabilitySourceLabel(source?: string | null): string {
  switch (source) {
    case "builtin": return "内置识别";
    case "catalog": return "模型目录";
    case "probe": return "识图实测";
    case "vendor": return "供应商预设";
    case "manual": return "手动覆盖";
    case "unknown": return "未识别";
    default: return source || "未知来源";
  }
}

const ITEMS = [
  ["vision", "图片", "支持图片输入"],
  ["files", "文件", "支持原生文件块（PDF）"],
  ["reasoning", "思考", "会输出思考过程"],
  ["tools", "工具", "支持工具调用"],
] as const;

/** 模型能力徽章：给模型列表和选择器一个「一眼能看见声明」的位置。 */
export function ModelCaps({ caps }: { caps: ChatModelCapabilities }) {
  const shown = ITEMS.filter(([key]) => caps[key]);
  if (shown.length === 0) {
    return (
      <span className="chat-caps-badges">
        <span className="chat-cap-badge unknown" title="名称没匹配到内置模型表，能力默认全关；可在「能力」里手动打开">
          能力未识别
        </span>
      </span>
    );
  }
  return (
    <span className="chat-caps-badges">
      {shown.map(([key, label, title]) => (
        <span className={`chat-cap-badge ${key}`} key={key} title={title}>{label}</span>
      ))}
    </span>
  );
}