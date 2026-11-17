import { useEffect, useMemo, type ReactNode } from "react";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useMobileLayout } from "../hooks";
import { I18N } from "../i18n";
import { closeSidebar, toggleSidebar, useAppState } from "../store";
import { useServerVersion } from "../version";
import { ArrowBackIcon, HomeIcon, MenuIcon } from "./icons";

/** 路由 → 标题栏文字（仿 MainWindowPane 的标题切换）。 */
function useWindowTitle(): string {
  const location = useLocation();
  const params = useParams();
  const [searchParams] = useSearchParams();
  const s = useAppState();
  const version = useServerVersion();
  const base = version ? `${I18N.appFullName} v${version}` : I18N.appFullName;

  return useMemo(() => {
    const p = location.pathname;
    if (p === "/") return base;
    if (p === "/login" || p === "/setup") return I18N.appFullName;
    if (p === "/instances") return I18N["dsh.instance.list"];
    if (p === "/download") return I18N["download"];
    if (p === "/accounts") return I18N["account"];
    if (p === "/settings") return I18N["settings"];
    if (p.startsWith("/instances/") && params.id) {
      const inst = s.instances.find((i) => i.id === params.id);
      return I18N["instance.manage.manage.title"].replace("%s", inst?.name ?? params.id);
    }
    if (p === "/instances/new") {
      return `${I18N["dsh.instance.install"]} - 选择品牌`;
    }
    if (p === "/instances/new/dsh") {
      const step = searchParams.get("step") === "2" ? I18N["dsh.install.step.quick"] : I18N["dsh.install.step.version"];
      return `${I18N["dsh.instance.install"]} - ${step}`;
    }
    if (p === "/install/modpack") {
      return I18N["install.modpack"];
    }
    return I18N.appFullName;
  }, [location.pathname, params.id, searchParams, s.instances, base]);
}

const WIZARD_PATHS = ["/instances/new", "/install/modpack"];

/// 上一级界面：按**路由层级**往上走一层，而不是沿浏览器历史回退。
/// 历史回退给的是「上一个看过的页面」，而标题栏这个箭头要的是「回到装着这一页的地方」
/// ——从实例详情返回实例列表、从品牌实例返回实例列表、从任何二级页返回主界面。
/// 品牌实例是实例列表的一个视图（`?view=brands`），所以它的上一级是实例列表本身。
function parentLocation(pathname: string, search: string): string | null {
  if (pathname === "/" || pathname === "/login" || pathname === "/setup") {
    return null;
  }
  if (pathname.startsWith("/brands")) {
    return "/instances";
  }
  if (pathname === "/instances") {
    const view = new URLSearchParams(search).get("view");
    if (view === "brands" || view === "tui" || view === "mc" || view === "mc-third") {
      return "/instances";
    }
  }
  const cut = pathname.lastIndexOf("/");
  return cut > 0 ? pathname.slice(0, cut) : "/";
}

/**
 * 应用外框：整页铺满视口（网页化适配，不再模拟桌面窗口），
 * 结构 = 标题栏（40px，primary-container，模仿桌面版观感）+ 内容区（铺满剩余空间）。
 * 标题栏只保留导航：返回上一级（按路由层级，不是历史回退）、图标（任何时候点都回主界面）
 * 与向导页的主页按钮；
 * 桌面版的最小化/关闭/帮助三键属于窗口系统，网页里不存在，已移除。
 *
 * 移动版式（≤760px 宽或 ≤520px 高）下额外多一个汉堡按钮：
 * 主导航侧栏在手机上收成抽屉（见 styles/mobile.css），这里负责开合，
 * 并在路由变化后自动收起。主导航只挂在主页（桌面版也是这个结构），
 * 所以汉堡按钮也只在主页出现——别处点了没有抽屉可开。
 */
export function Decorator({ children }: { children: ReactNode }) {
  const nav = useNavigate();
  const location = useLocation();
  const title = useWindowTitle();
  const mobile = useMobileLayout();

  const parent = parentLocation(location.pathname, location.search);
  const isWizard = WIZARD_PATHS.some((w) => location.pathname.startsWith(w));
  const hasMainNav = location.pathname === "/";

  // 手机上导航之后抽屉不该留着挡内容
  useEffect(() => {
    closeSidebar();
  }, [location.pathname]);

  return (
    <div className="decorator" data-mobile={mobile ? "true" : undefined}>
      <header className="titlebar">
        <div className="titlebar-nav">
          {mobile && hasMainNav && (
            <button
              className="titlebar-btn ripple-host"
              title="菜单"
              aria-label="菜单"
              onClick={() => toggleSidebar()}
            >
              <MenuIcon size={20} />
            </button>
          )}
          {/* 返回 = 上一级界面（见 parentLocation），不是 history.back()：
              从品牌实例进来再点返回，不该被历史带回上一个看过的页面 */}
          <button
            className="titlebar-btn ripple-host"
            disabled={parent === null}
            title={parent === null ? undefined : "返回上一级"}
            onClick={() => parent !== null && nav(parent)}
          >
            <ArrowBackIcon size={16} />
          </button>
          {isWizard && (
            <button className="titlebar-btn ripple-host" title="主页" onClick={() => nav("/")}>
              <HomeIcon size={16} />
            </button>
          )}
        </div>
        {/* 图标即「回主界面」：任何时候点它都落在主页，符合直觉 */}
        <button
          type="button"
          className="titlebar-icon titlebar-home ripple-host"
          title="回到主界面"
          aria-label="回到主界面"
          onClick={() => nav("/")}
        >
          <img src={`assets-img/icon-title.png`} alt="" />
        </button>
        <span className="titlebar-title">{title}</span>
      </header>
      <div className="decorator-content">{children}</div>
    </div>
  );
}
