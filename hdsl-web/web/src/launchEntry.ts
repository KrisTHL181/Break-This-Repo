import { useMemo } from "react";
import { I18N } from "./i18n";
import { useSelectedInstanceId } from "./selection";
import { useAppState } from "./store";
import type { ExternalInstance, Instance } from "./types";
import { normState } from "./utils";

/// 主按钮与实例选择菜单统一用的"当前实例"形状（dsh 实例或跨品牌外部实例）
export interface LaunchEntry {
  id: string;
  name: string;
  /** 已归一化（大写）：RUNNING / STARTING / STOPPED … */
  state: string;
  kind:
    | "dsh"
    | "kimi"
    | "opencode"
    | "claude-web"
    | "octop"
    | "hermes"
    | "pi-web"
    | "qwen"
    | "mimo"
    | "minimax"
    | "zcode";
  /** kind = dsh 时的原始实例；品牌/zcode 实例为 null */
  dsh: Instance | null;
  /** 品牌/zcode 条目；dsh 实例为 null */
  external: ExternalInstance | null;
}

export const BRAND_LABEL: Record<string, string> = {
  dsh: I18N["dsh.brand.dsh"],
  kimi: I18N["dsh.brand.kimi"],
  opencode: I18N["dsh.brand.opencode"],
  "claude-web": I18N["dsh.brand.claude-web"],
  octop: I18N["dsh.brand.octop"],
  hermes: I18N["dsh.brand.hermes.short"],
  "pi-web": I18N["dsh.brand.pi-web.short"],
  qwen: I18N["dsh.brand.qwen.short"],
  mimo: I18N["dsh.brand.mimo.short"],
  minimax: I18N["dsh.brand.minimax.short"],
  zcode: I18N["dsh.brand.zcode"],
};

/**
 * 按"当前选中实例 id"解析启动面板/主侧栏的当前条目：
 * 先在 dsh 实例里找，再在跨品牌外部实例（kimi/opencode/zcode）里找；
 * 无选中或选中已不存在 → null。
 */
export function useLaunchEntry(): LaunchEntry | null {
  const s = useAppState();
  const selectedId = useSelectedInstanceId();
  return useMemo(() => {
    if (!selectedId) return null;
    const dsh = s.instances.find((i) => i.id === selectedId);
    if (dsh) {
      return { id: dsh.id, name: dsh.name, state: normState(dsh.state), kind: "dsh", dsh, external: null };
    }
    const ext = s.external.find((e) => e.id === selectedId);
    if (ext) {
      return { id: ext.id, name: ext.name, state: normState(ext.state), kind: ext.brand, dsh: null, external: ext };
    }
    return null;
  }, [s.instances, s.external, selectedId]);
}
