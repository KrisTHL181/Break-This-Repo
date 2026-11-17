import { useEffect, useMemo, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useMobileLayout } from "../hooks";
import { isTuiBrand } from "../brandKind";
import { BRAND_LABEL, useLaunchEntry, type LaunchEntry } from "../launchEntry";
import { I18N } from "../i18n";
import { closeSidebar, logout, useAppState } from "../store";
import { useTranscodeFeature } from "../transcodeFeature";
import type { Instance } from "../types";
import { normState } from "../utils";
import { BrandIcon } from "./BrandIcon";
import {
  ChatIcon,
  DownloadIcon,
  FormatListBulletedIcon,
  LogoutIcon,
  PersonIcon,
  SettingsIcon,
  TuneIcon,
} from "./icons";

/** 实例图标：icon 字段是可访问 URL 时显示图片，否则用 dsh 的品牌标志（DeepSeek 鲸鱼）。 */
export function InstanceGraphic({ inst, size = 32 }: { inst: Instance | null; size?: number }) {
  const iconUrl = inst?.icon;
  const showImg = !!iconUrl && /^(https?:|\/|data:)/.test(iconUrl);
  if (showImg) {
    return (
      <span className="side-graphic lg" style={{ width: size, height: size }}>
        <img src={iconUrl} alt="" />
      </span>
    );
  }
  return (
    <span className="side-graphic lg" style={{ width: size, height: size }}>
      <BrandIcon brand="dsh" size={size} />
    </span>
  );
}

/**
 * 主导航侧栏（宽 200，MainPage 结构）：
 * 账户分区（账户卡片）/ 实例分区（当前实例项、实例列表、下载）/ 通用分区（设置）。
 *
 * 移动版式下它变成抽屉：默认滑出屏幕外，由标题栏的汉堡按钮唤出（见 Decorator.tsx），
 * 点条目或点遮罩收起；开合状态在 store 的 sidebarOpen。
 */
export function MainSideBar() {
  const nav = useNavigate();
  const location = useLocation();
  const s = useAppState();
  const mobile = useMobileLayout();

  const account = s.accounts && s.accounts.length > 0 ? s.accounts[0] : null;
  const transcode = useTranscodeFeature();
  // 全局只有一个"当前实例"：dsh 与品牌实例共用 selection。没有选择时退回
  // 第一个运行中的 dsh，保持老行为。
  const entry = useLaunchEntry();
  const fallback = useMemo<LaunchEntry | null>(() => {
    const running = s.instances.find((i) => normState(i.state) === "RUNNING");
    const inst = running ?? s.instances[0] ?? null;
    if (inst) {
      return { id: inst.id, name: inst.name, state: normState(inst.state), kind: "dsh", dsh: inst, external: null };
    }
    // 没有 dsh 实例时也不要让侧栏空着：品牌实例同样是"当前实例"的候选。
    const ext = s.external.find((e) => normState(e.state) === "RUNNING") ?? s.external[0] ?? null;
    return ext
      ? { id: ext.id, name: ext.name, state: normState(ext.state), kind: ext.brand, dsh: null, external: ext }
      : null;
  }, [s.instances, s.external]);
  const current = entry ?? fallback;
  const currentSelected = current
    ? current.kind === "dsh"
      ? location.pathname.startsWith("/instances/") && location.pathname !== "/instances"
      : location.pathname === "/instances"
        && new URLSearchParams(location.search).get("brand") === current.kind
    : false;

  // 手机上选了就去目的地并把抽屉收起来（桌面版式下 closeSidebar 是空操作）
  const goTo = (path: string) => {
    closeSidebar();
    nav(path);
  };
  const go = (path: string) => () => goTo(path);
  const selected = (path: string) => location.pathname === path;

  return (
    <>
      {mobile && s.sidebarOpen && (
        <div className="sidebar-scrim" onClick={() => closeSidebar()} aria-hidden="true" />
      )}
      <nav className={`sidebar-main${mobile && s.sidebarOpen ? " open" : ""}`} data-open={s.sidebarOpen ? "true" : undefined}>
      <div className="side-category">{I18N["dsh.account.list"]}</div>
      <button
        className={`side-item ripple-host${selected("/accounts") ? " selected" : ""}`}
        onClick={go("/accounts")}
      >
        <span className="side-graphic lg">
          {account?.skinSet ? (
            <img src={`api/accounts/${encodeURIComponent(account.name)}/skin`} alt="" />
          ) : (
            <PersonIcon size={20} />
          )}
        </span>
        <span className="side-text">
          <span className="side-title">
            {account ? account.label || account.name : I18N["dsh.account.none.short"]}
          </span>
          <span className="side-subtitle">
            {account ? account.vendor : I18N["dsh.account.none.hint"]}
          </span>
        </span>
      </button>

      <div className="side-category">{I18N["instance"]}</div>
      <button
        className={`side-item ripple-host${currentSelected ? " selected" : ""}`}
        onClick={() => {
          if (!current) return;
          goTo(current.kind === "dsh"
            ? `/instances/${current.id}`
            : `/instances?view=${isTuiBrand(current.kind) ? "tui" : "brands"}&brand=${current.kind}`);
        }}
      >
        {current?.kind === "dsh" && current.dsh ? (
          <InstanceGraphic inst={current.dsh} />
        ) : current ? (
          <span className="side-graphic lg">
            <BrandIcon brand={current.kind} size={32} />
          </span>
        ) : (
          <InstanceGraphic inst={null} />
        )}
        <span className="side-text">
          <span className="side-title">{current?.name ?? I18N["dsh.instance.none"]}</span>
          <span className="side-subtitle">
            {current
              ? current.kind === "dsh"
                ? current.dsh?.version ?? BRAND_LABEL.dsh
                : current.external?.version || BRAND_LABEL[current.kind] || current.kind
              : I18N["dsh.instance.install"]}
          </span>
        </span>
      </button>
      <button
        className={`side-item ripple-host${selected("/instances") ? " selected" : ""}`}
        onClick={go("/instances")}
      >
        <span className="side-graphic">
          <FormatListBulletedIcon size={20} />
        </span>
        <span className="side-text">
          <span className="side-title">{I18N["dsh.instance.list"]}</span>
        </span>
      </button>
      <button
        className={`side-item ripple-host${selected("/download") ? " selected" : ""}`}
        onClick={go("/download")}
      >
        <span className="side-graphic">
          <DownloadIcon size={20} />
        </span>
        <span className="side-text">
          <span className="side-title">{I18N["download"]}</span>
        </span>
      </button>

      <div className="side-category">聊天</div>
      <button
        className={`side-item ripple-host${selected("/chat") ? " selected" : ""}`}
        onClick={go("/chat")}
      >
        <span className="side-graphic">
          <ChatIcon size={20} />
        </span>
        <span className="side-text">
          <span className="side-title">聊天</span>
          <span className="side-subtitle">与模型对话</span>
        </span>
      </button>

      {transcode?.enabled && (
        <>
          <div className="side-category">媒体</div>
          <button
            className={`side-item ripple-host${location.pathname.startsWith("/transcode") ? " selected" : ""}`}
            onClick={go("/transcode")}
          >
            <span className="side-graphic">
              <TuneIcon size={20} />
            </span>
            <span className="side-text">
              <span className="side-title">转码</span>
              <span className="side-subtitle">批量编码</span>
            </span>
          </button>
        </>
      )}

      <div className="side-category">{I18N["settings.launcher.general"]}</div>
      <button
        className={`side-item ripple-host${selected("/settings") ? " selected" : ""}`}
        onClick={go("/settings")}
      >
        <span className="side-graphic">
          <SettingsIcon size={20} />
        </span>
        <span className="side-text">
          <span className="side-title">{I18N["settings"]}</span>
        </span>
      </button>

      {/* 底部固定项：退出登录只在这个主侧栏（和「设置 → 服务器信息」里的一个按钮）。
          实例页的子侧栏曾经也复制过一份，夹在「安装新实例 / 安装整合包 / 全局设置」
          中间，看起来像第四个操作项——那份已经删掉，别再复制回来。
          同一条路径：服务端吊销会话 + 清 Cookie + 本地清理。 */}
      <span className="side-spacer" />
      <button
        className="side-item ripple-host"
        onClick={() => {
          closeSidebar();
          void logout().then(() => nav("/login", { replace: true }));
        }}
      >
        <span className="side-graphic">
          <LogoutIcon size={20} />
        </span>
        <span className="side-text">
          <span className="side-title">{I18N["dsh.logout"]}</span>
        </span>
      </button>
    </nav>
    </>
  );
}

export interface SubSideItem {
  key: string;
  title: string;
  icon: React.ReactNode;
  iconActive?: React.ReactNode;
}

export interface SubSideSection {
  title?: string;
  items: SubSideItem[];
}

/** 子页面侧栏：可选分区标题 + 条目 + 底部操作区。 */
export function SubSideBar({
  sections,
  activeKey,
  onSelect,
  bottom,
  testId,
}: {
  sections: SubSideSection[];
  activeKey?: string;
  onSelect?: (key: string) => void;
  bottom?: React.ReactNode;
  testId?: string;
}) {
  const mobile = useMobileLayout();
  const stripRef = useRef<HTMLElement | null>(null);
  const activeRef = useRef<HTMLButtonElement | null>(null);

  // 窄屏标签条是横向滚动的：切换 tab 后把当前项滚到中间，避免
  // 「参数面板」这种第 4 个之后的项只露出半截。`block: nearest`
  // 不改变页面纵向位置，只动这条横向滚动容器。
  useEffect(() => {
    if (!mobile) return;
    activeRef.current?.scrollIntoView({ inline: "center", block: "nearest" });
  }, [activeKey, mobile]);

  // 窄屏的标签条是横向滚动的。触摸有浏览器原生滚动，鼠标没有：滚轮是纵向的，拖动又不会平移
  // 一个 div——所以在桌面窄窗（包括 DevTools 的设备模拟）里看着就是「滑不动」。这里只对鼠标
  // 接管，而且只在窄屏：触摸交回浏览器自己（自己接管会和惯性滚动、和页面的纵向滚动打架），
  // 桌面宽屏根本用不到这条（那会儿它是竖的侧栏）。
  //
  // 拖完那一下不能顺带把条目点开——但也不能把「手抖了几像素的正常点击」一起吃掉，所以判据是
  // 「真的滚动了」，不是「按过鼠标」。
  useEffect(() => {
    const strip = stripRef.current;
    if (!strip || !mobile) return;
    let startX = 0;
    let startLeft = 0;
    let dragging = false;
    let moved = false;

    const down = (e: PointerEvent) => {
      if (e.pointerType !== "mouse" || e.button !== 0) return;
      dragging = true;
      moved = false;
      startX = e.clientX;
      startLeft = strip.scrollLeft;
    };
    const move = (e: PointerEvent) => {
      if (!dragging) return;
      const dx = e.clientX - startX;
      if (Math.abs(dx) > 3) moved = true;
      strip.scrollLeft = startLeft - dx;
    };
    const up = () => {
      dragging = false;
    };
    const swallowClick = (e: MouseEvent) => {
      if (!moved || strip.scrollLeft === startLeft) return;
      moved = false;
      e.stopPropagation();
      e.preventDefault();
    };

    strip.addEventListener("pointerdown", down);
    strip.addEventListener("click", swallowClick, true);
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
    window.addEventListener("pointercancel", up);
    return () => {
      strip.removeEventListener("pointerdown", down);
      strip.removeEventListener("click", swallowClick, true);
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
      window.removeEventListener("pointercancel", up);
    };
  }, [mobile]);

  return (
    <nav className="sidebar-sub" data-testid={testId} ref={stripRef}>
      {sections.map((sec, i) => (
        <div key={i}>
          {sec.title != null && <div className="side-category">{sec.title}</div>}
          {sec.items.map((it) => {
            const active = it.key === activeKey;
            return (
              <button
                key={it.key}
                ref={active ? activeRef : undefined}
                className={`side-item ripple-host${active ? " selected" : ""}`}
                onClick={() => onSelect?.(it.key)}
              >
                <span className="side-graphic">
                  {active && it.iconActive ? it.iconActive : it.icon}
                </span>
                <span className="side-text">
                  <span className="side-title">{it.title}</span>
                </span>
              </button>
            );
          })}
        </div>
      ))}
      {bottom &&
        (mobile ? (
          <div className="side-strip-tail">{bottom}</div>
        ) : (
          <>
            <div className="sidebar-spacer" />
            <div className="side-actions">{bottom}</div>
          </>
        ))}
    </nav>
  );
}
