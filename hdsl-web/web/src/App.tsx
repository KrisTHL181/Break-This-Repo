import { useEffect, useState } from "react";
import { flushSync } from "react-dom";
import { Navigate, Outlet, Route, Routes, useLocation, useParams } from "react-router-dom";
import { isTuiBrand, setTuiBrands } from "./brandKind";
import { ErrorIcon, InfoIcon } from "./components/icons";
import { Decorator } from "./components/Decorator";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { OfflineBanner } from "./components/OfflineBanner";
import { SnackbarHost } from "./components/Snackbar";
import { ElevationHost } from "./components/ElevationDialog";
import { AccountsPage } from "./pages/AccountsPage";
import { ChatPage } from "./pages/ChatPage";
import { ChatSettingsPage } from "./pages/chat/ChatSettingsPage";
import { DownloadPage } from "./pages/DownloadPage";
import { InstallInstanceWizard } from "./pages/InstallInstanceWizard";
import { NewInstancePage } from "./pages/NewInstancePage";
import { InstallModpackWizard } from "./pages/InstallModpackWizard";
import { InstanceDetailPage } from "./pages/InstanceDetailPage";
import { InstancesPage } from "./pages/InstancesPage";
import { LoginPage } from "./pages/LoginPage";
import { MainPage } from "./pages/MainPage";
import { McInstancePage } from "./pages/McInstancePage";
import { SettingsPage } from "./pages/SettingsPage";
import { SetupPage } from "./pages/SetupPage";
import { TranscodePage } from "./pages/TranscodePage";
import { bootstrap, useAppState } from "./store";
import { refreshWallpapers, useWallpaperUrl } from "./wallpaper";
import { api } from "./api";
import { requestDeveloperPassword } from "./elevation";
import { refreshServerVersion } from "./version";
import { refreshTranscodeFeature } from "./transcodeFeature";

function SplashScreen() {
  return (
    <div className="screen-full">
      <div className="spinner" />
      <div className="screen-title">HDSL 面板</div>
    </div>
  );
}

function ErrorScreen({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="screen-full">
      <div className="screen-error-card">
        <ErrorIcon size={30} style={{ color: "var(--monet-error)" }} />
        <h2>无法连接服务器</h2>
        <p>{message}</p>
        <button className="btn btn-raised ripple-host" onClick={onRetry}>
          重试
        </button>
      </div>
    </div>
  );
}

function RequireAuth() {
  const s = useAppState();
  const location = useLocation();
  if (!s.authChecked) return <SplashScreen />;
  if (s.authError) return <ErrorScreen message={s.authError} onRetry={() => void bootstrap()} />;
  if (!s.username) {
    return (
      <Navigate
        to={s.setupRequired ? "/setup" : "/login"}
        replace
        state={{ from: location.pathname }}
      />
    );
  }
  return <Outlet />;
}

/** `/brands/<品牌>` 旧深链：品牌区现在是实例列表页内的一个视图，这里只做重定向
 *  （URL 换成 `/instances?view=brands&brand=…`，页面不跳第二层）。 */
function BrandDeepLink() {
  const { brand } = useParams();
  const encoded = encodeURIComponent(brand ?? "");
  return <Navigate to={`/instances?view=${isTuiBrand(brand ?? "") ? "tui" : "brands"}&brand=${encoded}`} replace />;
}

/** 已登录区域：浏览器内窗口（Decorator）+ 路由动画容器。
 * key 只取 pathname：同一页内的 ?tab= 切换不重挂载（保留 ACP 会话等页内状态），
 * tab 内容的 SLIDE_UP_FADE_IN 动画由各页自行施加。
 * 内页背景 = 壁纸（侧栏/内容区的半透明板透出壁纸，见 ui-spec.md §4）。
 * 旧页退出动画（ui-spec §11）走 View Transitions API：不认识的浏览器
 * （Firefox 115 等）维持原状，只有新页入场动画。 */
function Shell() {
  const location = useLocation();
  const supportsViewTransition = "startViewTransition" in document;
  const [displayedPath, setDisplayedPath] = useState(location.pathname);
  useEffect(() => {
    if (!supportsViewTransition || location.pathname === displayedPath) {
      return;
    }
    const transition = document.startViewTransition(() => {
      flushSync(() => setDisplayedPath(location.pathname));
    });
    return () => transition.skipTransition();
  }, [location.pathname, displayedPath, supportsViewTransition]);
  const path = supportsViewTransition ? displayedPath : location.pathname;
  // 二级页的背景以前是写死的那张内置壁纸，换壁纸只有主页跟着变、点进任何子页就打回原形。
  // 列表也在这里拉一次：深链直接进 /settings 时（没经过主页）上传的那些也得在。
  const wallpaperUrl = useWallpaperUrl();
  useEffect(() => {
    void refreshWallpapers();
    void refreshTranscodeFeature();
  }, []);
  return (
    <Decorator>
      <div
        className="page-root"
        key={path}
        style={{
          flex: 1,
          minHeight: 0,
          display: "flex",
          flexDirection: "column",
          backgroundImage: `url(${wallpaperUrl})`,
          backgroundSize: "cover",
          backgroundPosition: "center bottom",
        }}
      >
        <ErrorBoundary>
          <EmergencyStopBanner />
          <Outlet />
        </ErrorBoundary>
      </div>
    </Decorator>
  );
}

/// 紧急停机期间的全局横幅：所有已登录页面都能看到，冷却结束前只显示倒计时。
///
/// 状态来自 `GET /api/developer/status`（守卫与停机都会在 /api 白名单里放行它），
/// 每 20 秒刷新一次，页面重新可见时立刻刷新——按下停机的人刷新页面就能看到。
function EmergencyStopBanner() {
  const [estop, setEstop] = useState<{ engaged: boolean; remainingSeconds: number } | null>(null);
  /** 「全部 API」档 + 未提权：读也被拦住，页面会像刚装好一样空。要解释，并给一键提权。 */
  const [readLocked, setReadLocked] = useState(false);
  const [elevating, setElevating] = useState(false);
  // glm-5.2: tick 值不再参与倒计时计算，只保留 setter 作为每秒重渲染的心跳（值用不到，故丢弃）。
  const [, setTick] = useState(0);
  // glm-5.2: 锚定上次从服务器拿到 remainingSeconds 的墙钟时间，倒计时改按 Date.now() 推算真实经过秒数，避免每 20 秒刷新跳一下。
  const [estopRefreshTime, setEstopRefreshTime] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const status = await api.developerStatus();
        if (!cancelled) {
          const stop = status.estop;
          setEstop({
            engaged: stop?.engaged === true,
            // 端点只给按下那一刻的剩余秒数；本地按读取时间往前推。
            remainingSeconds: stop?.remainingSeconds ?? 0,
          });
          // glm-5.2: 记录本次服务器刷新的墙钟时刻，供 remaining 用 Date.now() 推算。
          setEstopRefreshTime(Date.now());
          // `all` 是唯一会拦读的档：它的意思就是"连列表都要提权"。
          // 这时页面读不到实例，看起来像空面板——必须说清楚，而不是让人以为数据没了。
          setReadLocked((status.guardScope ?? "off") === "all" && !status.elevated);
        }
      } catch {
        // 未登录、旧后端、网络抖动：不显示横幅，页面自身的错误处理照旧。
      }
    };
    void load();
    const timer = window.setInterval(() => void load(), 20_000);
    const onVisible = () => {
      if (document.visibilityState === "visible") void load();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, []);

  // glm-5.2: 倒计时改按墙钟推算（见下方 remaining），这里每秒只触发一次重渲染让数字往下走；
  // 只在停机期间跑，避免整页每秒重渲染。
  useEffect(() => {
    if (estop?.engaged !== true) return;
    const timer = window.setInterval(() => setTick((value) => value + 1), 1_000);
    return () => window.clearInterval(timer);
  }, [estop?.engaged]);

  /// 「提权」：输密码，成功后整页重载（`all` 档下所有 GET 都等这一次提权）。
  const elevate = async () => {
    if (elevating) return;
    setElevating(true);
    try {
      if (await requestDeveloperPassword()) window.location.reload();
    } finally {
      setElevating(false);
    }
  };

  if (readLocked && estop?.engaged !== true) {
    return (
      <div
        role="status"
        style={{
          display: "flex",
          alignItems: "center",
          gap: 10,
          padding: "8px 14px",
          background: "var(--monet-surface-container-high, #e8e6ef)",
          color: "var(--monet-on-surface)",
          fontSize: 13,
        }}
      >
        <InfoIcon size={16} />
        <span>
          开发者守卫处于「全部 API」档：未提权时连实例列表都读不到，所以页面看起来是空的。
          这是这一档的设定，不是数据丢了。
        </span>
        <button
          className="btn btn-text"
          style={{ marginLeft: "auto", whiteSpace: "nowrap" }}
          disabled={elevating}
          onClick={() => void elevate()}
        >
          {elevating ? "提权中…" : "输入开发者密码"}
        </button>
        <a href="settings?tab=developer" style={{ color: "inherit", textDecoration: "underline", whiteSpace: "nowrap" }}>
          开发者设置
        </a>
      </div>
    );
  }

  if (estop?.engaged !== true) return null;
  // glm-5.2: 用 Date.now() 与上次服务器刷新时间的差值推算剩余秒数，不依赖会漂移且不随刷新重置的 tick 计数。
  const remaining = Math.max(0, estop.remainingSeconds - Math.floor((Date.now() - estopRefreshTime) / 1000));
  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;

  return (
    <div
      role="status"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        padding: "8px 14px",
        background: "var(--monet-error)",
        color: "#fff",
        fontSize: 13,
      }}
    >
      <ErrorIcon size={16} />
      <span>
        紧急停机中：实例网页、终端与实例启动全部冻结，剩余 {minutes} 分 {seconds} 秒；
        解除需要开发者密码，或重启面板进程/容器。
      </span>
      <a href="settings?tab=developer" style={{ marginLeft: "auto", color: "#fff", textDecoration: "underline" }}>
        去开发者模式
      </a>
    </div>
  );
}

export default function App() {
  const s = useAppState();

  useEffect(() => {
    void bootstrap();
  }, []);

  // 登录后拉一次服务端版本（标题栏 "Hello DeepSeek! Launcher vX.Y.Z"）
  useEffect(() => {
    if (s.username) void refreshServerVersion();
  }, [s.username]);

  // 品牌形态以服务端 Brand.kind() 为准：首屏用 brandKind.ts 的兜底列表，
  // 能力接口回来后替换，新增 TUI 品牌不需要再改前端。
  useEffect(() => {
    if (!s.username) return;
    void api.brandCapabilities()
      .then((caps) => setTuiBrands(caps.brands.filter((b) => b.kind === "tui").map((b) => b.id)))
      .catch(() => {
        // 能力查询失败时保留兜底列表，不影响页面本身。
      });
  }, [s.username]);

  return (
    <>
      <Routes>
        <Route
          path="/login"
          element={
            <Decorator>
              <div className="page-root" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column" }}>
                <ErrorBoundary>
                  <LoginPage />
                </ErrorBoundary>
              </div>
            </Decorator>
          }
        />
        <Route
          path="/setup"
          element={
            <Decorator>
              <div className="page-root" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column" }}>
                <ErrorBoundary>
                  <SetupPage />
                </ErrorBoundary>
              </div>
            </Decorator>
          }
        />
        <Route element={<RequireAuth />}>
          <Route element={<Shell />}>
            <Route path="/" element={<MainPage />} />
            <Route path="/instances" element={<InstancesPage />} />
            <Route path="/instances/new" element={<NewInstancePage />} />
            <Route path="/instances/new/dsh" element={<InstallInstanceWizard />} />
            <Route path="/instances/:id" element={<InstanceDetailPage />} />
            {/* 旧路径兼容：MC 列表现在是实例页里的一层视图（?view=mc） */}
            <Route path="/mc" element={<Navigate to="/instances?view=mc" replace />} />
            <Route path="/mc/:id" element={<McInstancePage />} />
            <Route path="/brands" element={<Navigate to="/instances?view=brands" replace />} />
            <Route path="/brands/:brand" element={<BrandDeepLink />} />
            <Route path="/download" element={<DownloadPage />} />
            <Route path="/install/modpack" element={<InstallModpackWizard />} />
            <Route path="/accounts" element={<AccountsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/transcode" element={<TranscodePage />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/chat/settings" element={<ChatSettingsPage />} />
            {/* 旧版路径兼容 */}
            <Route path="/packs" element={<Navigate to="/download?tab=modpack" replace />} />
            <Route path="/plugins" element={<Navigate to="/download?tab=plugin" replace />} />
            <Route path="/doctor" element={<Navigate to="/settings?tab=doctor" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Route>
      </Routes>
      <OfflineBanner />
      <SnackbarHost />
      <ElevationHost />
    </>
  );
}
