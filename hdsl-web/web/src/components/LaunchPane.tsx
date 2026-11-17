import { useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { launchBrandInstance, launchInstance, stopBrandInstance, stopInstance } from "../actions";
import { isTuiBrand } from "../brandKind";
import { I18N } from "../i18n";
import { BRAND_LABEL, useLaunchEntry, type LaunchEntry } from "../launchEntry";
import { setSelectedInstanceId, useSelectedInstanceId } from "../selection";
import { toast, useAppState } from "../store";
import { normState } from "../utils";
import { BrandIcon } from "./BrandIcon";
import { ArrowDropUpIcon } from "./icons";
import { PopupItem, PopupMenu, PopupSep } from "./PopupMenu";
import { InstanceGraphic } from "./SideBar";

function actionLabel(state: string): string {
  switch (state) {
    case "STARTING":
      return I18N["dsh.launch.launching"];
    case "RUNNING":
      return I18N["dsh.stop"];
    case "STOPPING":
      return I18N["dsh.stopping"];
    default:
      return I18N["dsh.launch"];
  }
}

/**
 * 启动面板（右下，MainPage.launch-pane）：
 * 200×55 主钮（16px 动作 + 12px 目标，on-primary-container）+ 27×55 箭头菜单钮。
 * 主钮 = 启动/停止当前选中实例（dsh / kimi / opencode / zcode 按 kind 分发）；
 * 箭头 = 跨品牌实例选择弹出菜单。
 */
export function LaunchPane() {
  const nav = useNavigate();
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const current = useLaunchEntry();
  const running = current ? current.state === "RUNNING" : false;
  const tui = current?.external ? isTuiBrand(current.external.brand) : false;

  const onAction = () => {
    if (!current) {
      nav("/download?tab=game");
      return;
    }
    if (tui) {
      toast("info", "这是 TUI 品牌实例：在「品牌实例」页点终端按钮打开它的 TUI");
      return;
    }
    if (current.kind === "dsh") {
      if (!current.dsh) return;
      if (running) void stopInstance(current.dsh);
      else void launchInstance(current.dsh);
    } else if (current.external?.brand === "pi-web" && current.external.webEnabled === false) {
      if (running) void stopBrandInstance(current.kind, current.id);
      else toast("info", "这是仅终端实例：在「品牌实例」页点终端按钮打开 Pi 的 TUI");
    } else {
      if (running) void stopBrandInstance(current.kind, current.id);
      else void launchBrandInstance(current.kind, { id: current.id, name: current.name });
    }
  };

  return (
    <div className="launch-pane">
      <button className="launch-main ripple-host" onClick={onAction}>
        <span className="launch-action">
          {current
            ? tui
              ? I18N["terminal.open"]
              : current.external?.brand === "pi-web" && current.external.webEnabled === false
                ? I18N["dsh.brand.pi-web.terminalOnly"]
                : actionLabel(current.state)
            : I18N["dsh.launch.no_instance"]}
        </span>
        <span className="launch-target">
          {current ? current.name : I18N["dsh.launch.no_instance.hint"]}
        </span>
      </button>
      <button
        className="launch-menu-btn ripple-host"
        title={I18N["dsh.launch.select"]}
        onClick={(e) => setMenuAnchor(e.currentTarget)}
      >
        <ArrowDropUpIcon size={30} />
      </button>
      <InstancePickerMenu
        anchor={menuAnchor}
        onClose={() => setMenuAnchor(null)}
        footer={
          <PopupItem onClick={() => nav("/instances/new")}>
            {I18N["dsh.launch.no_instance"]}
          </PopupItem>
        }
      />
    </div>
  );
}

/** 跨品牌实例选择弹出菜单（jfx-popup 样式：surface 底、圆角 4）：点击条目切换选中实例，不路由跳转。 */
export function InstancePickerMenu({
  anchor,
  onClose,
  footer,
}: {
  anchor: HTMLElement | null;
  onClose: () => void;
  footer?: ReactNode;
}) {
  const s = useAppState();
  const selectedId = useSelectedInstanceId();
  const dshItems: LaunchEntry[] = s.instances.map((i) => ({
    id: i.id,
    name: i.name,
    state: normState(i.state),
    kind: "dsh",
    dsh: i,
    external: null,
  }));
  const extItems: LaunchEntry[] = s.external.map((e) => ({
    id: e.id,
    name: e.name,
    state: normState(e.state),
    kind: e.brand,
    dsh: null,
    external: e,
  }));
  const items = [...dshItems, ...extItems];
  const empty = items.length === 0;

  return (
    <PopupMenu open={anchor !== null} anchor={anchor} onClose={onClose}>
      {empty ? (
        <PopupItem onClick={onClose}>{I18N["dsh.launch.no_instance.hint"]}</PopupItem>
      ) : (
        items.map((i) => (
          <PopupItem
            key={`${i.kind}:${i.id}`}
            icon={
              i.kind === "dsh" ? (
                <InstanceGraphic inst={i.dsh} size={20} />
              ) : (
                <BrandIcon brand={i.kind} size={20} />
              )
            }
            onClick={() => {
              onClose();
              setSelectedInstanceId(i.id);
            }}
          >
            {i.name}
            <span style={{ opacity: 0.6, fontSize: 12 }}> — {BRAND_LABEL[i.kind]}</span>
            <span style={{ opacity: 0.6, fontSize: 12 }}>{i.id === selectedId ? " ·" : ""}</span>
          </PopupItem>
        ))
      )}
      {!empty && <PopupSep />}
      {footer}
    </PopupMenu>
  );
}
