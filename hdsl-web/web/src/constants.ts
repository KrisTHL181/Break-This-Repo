export interface HomeModeOption {
  value: string;
  title: string;
  desc: string;
}

export const HOME_MODES: HomeModeOption[] = [
  { value: "ISOLATED", title: "独立实例（推荐）", desc: "每个实例拥有独立的数据目录，互不影响" },
  { value: "VERSION_SHARED", title: "版本共享", desc: "相同版本的实例共享同一个数据目录" },
  { value: "GLOBAL", title: "全局共享", desc: "所有实例共享一个全局数据目录" },
  { value: "CUSTOM", title: "自定义", desc: "使用自定义路径作为实例数据目录" },
];

export const MAX_LOG_LINES = 500;
export const LOG_TAIL = 200;

/// dsh 版本对应的上游发布页。
///
/// 与桌面版同一个形状（`DownloadPage.java` 的 `RELEASES`，按钮文案「打开 release 页面」）：
/// 标签是 `dsh-v<版本>`。注意列在这里的版本来自 npm，上游若只发了 npm 包而没有打 tag，这个
/// 地址会落到 GitHub 的 404 页——要改成 npm 包页面的话，改这一处即可。
export const DSH_RELEASES = "https://github.com/deepseek-ai/deepseek-harness/releases/tag/dsh-v";
