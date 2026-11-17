import { useEffect, type ReactNode } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { SubSideBar } from "../components/SideBar";
import {
  ArrowBackIcon,
  ExtensionIcon,
  FolderOpenIcon,
  FormatListBulletedIcon,
  HomeIcon,
  InfoIcon,
  SettingsIcon,
  TuneIcon,
} from "../components/icons";
import { refreshTranscodeFeature, useTranscodeFeature } from "../transcodeFeature";
import { MediaInfoPage } from "./transcode/MediaInfoPage";
import { OverviewPage } from "./transcode/OverviewPage";
import { PreparePage } from "./transcode/PreparePage";
import { QueuePage } from "./transcode/QueuePage";
import { PresetsPage } from "./transcode/PresetsPage";
import { ToolsPage } from "./transcode/ToolsPage";
import { TranscodeSettingsPage } from "./transcode/TranscodeSettingsPage";

type TranscodeTab = "overview" | "prepare" | "queue" | "presets" | "mediainfo" | "tools" | "settings";

const TABS: Array<{ key: TranscodeTab; title: string; icon: ReactNode; iconActive?: ReactNode }> = [
  { key: "overview", title: "概览", icon: <HomeIcon size={20} /> },
  { key: "prepare", title: "准备文件", icon: <FolderOpenIcon size={20} /> },
  { key: "queue", title: "编码队列", icon: <FormatListBulletedIcon size={20} /> },
  { key: "presets", title: "参数面板", icon: <TuneIcon size={20} /> },
  { key: "mediainfo", title: "媒体信息", icon: <InfoIcon size={20} /> },
  { key: "tools", title: "工具", icon: <ExtensionIcon size={20} /> },
  { key: "settings", title: "设置", icon: <SettingsIcon size={20} /> },
];

/// The transcode module's second-level page: a fixed six-item sidebar and a content area.
/// The feature gate is in [TranscodeFeatureFilter] on the server; this page sends a
/// disabled panel back home, so a stale tab cannot keep a removed surface alive.
export function TranscodePage() {
  const feature = useTranscodeFeature();
  const nav = useNavigate();
  const [params, setParams] = useSearchParams();
  const raw = params.get("tab");
  const tab: TranscodeTab = TABS.some((entry) => entry.key === raw) ? (raw as TranscodeTab) : "overview";
  const openTab = (next: string) => setParams({ tab: next }, { replace: true });

  useEffect(() => {
    void refreshTranscodeFeature();
  }, []);

  if (!feature) {
    return (
      <div className="page-with-sidebar">
        <div className="page-content" style={{ alignItems: "center", justifyContent: "center" }}>
          <div className="spinner" />
        </div>
      </div>
    );
  }
  if (!feature.enabled) return <Navigate to="/" replace />;

  return (
    <div className="page-with-sidebar">
      <SubSideBar
        activeKey={tab}
        onSelect={(key) => openTab(key)}
        sections={[{ items: TABS }]}
        bottom={
          <button className="side-item ripple-host" onClick={() => nav("/")}>
            <span className="side-graphic">
              <ArrowBackIcon size={20} />
            </span>
            <span className="side-text">
              <span className="side-title">返回主界面</span>
            </span>
          </button>
        }
      />

      <div className="page-content scroll" style={{ gap: 8 }}>
        <div className="section-caption">
          <TuneIcon size={16} />
          转码 · {TABS.find((entry) => entry.key === tab)?.title}
        </div>
        {tab === "overview" && <OverviewPage onOpenTab={openTab} />}
        {tab === "prepare" && <PreparePage onOpenTab={openTab} />}
        {tab === "queue" && <QueuePage onOpenTab={openTab} />}
        {tab === "presets" && <PresetsPage />}
        {tab === "mediainfo" && <MediaInfoPage />}
        {tab === "tools" && <ToolsPage onOpenTab={openTab} />}
        {tab === "settings" && <TranscodeSettingsPage />}
      </div>
    </div>
  );
}