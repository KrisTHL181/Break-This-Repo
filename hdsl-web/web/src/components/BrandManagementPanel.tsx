import { useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import { toast } from "../store";
import type {
  BrandId,
  BrandPolicyEntry,
  BrandSettings,
  BrandSourceChoice,
  BrandSourceProbe,
  BrandSourceState,
  DeveloperStatus,
  GitHubTokenStatus,
} from "../types";
import { errMsg } from "../utils";
import { McFeatureCard } from "./McFeatureCard";
import { TranscodeFeatureCard } from "./TranscodeFeatureCard";

/** 四个源在接口里的名字。 */
type Kind = "pypi" | "python" | "archive" | "release";

/// 一个源在页面上的草稿：选中的 preset 与手填地址。
interface Draft {
  preset: string;
  url: string;
}

/// 页面上一行源：全局两类 + 每个走 GitHub Releases 的品牌各一行。
interface Row {
  /// 稳定 key，也是探测结果按行分组的键：`pypi` / `python` / `release:octop`
  key: string;
  kind: Kind;
  brand?: BrandId;
  title: string;
  hint: string;
  state: BrandSourceState;
}

const SOURCE_LABEL: Record<string, string> = {
  setting: "面板设置",
  environment: "环境变量",
  default: "上游默认",
};

const KIND_HINT: Record<Kind, string> = {
  pypi: "uv 本体与 Python 依赖都从这里取；换成本地镜像，源码编译的下载时间通常从几十分钟降到几分钟。",
  python: "uv 下载 CPython 3.12 的镜像（UV_PYTHON_INSTALL_MIRROR）；实例已有 3.12 时不会下载。",
  archive: "版本源码归档（codeload 形态：`<base>/<repo>/zip/refs/tags/<tag>`）。GitHub 的 git 传输容易被切断，"
    + "而这份 zip 通常很快；换成加速前缀时注意它是第三方，只用来取公开源码。",
  release: "品牌安装的预编译包从这里取。portable 包没有官方国内镜像，自建一份填在「自定义」里。",
};

const CAPABILITY_LABEL: Record<string, string> = {
  lockedRoute: "固定反代路径",
  sourceBuild: "本地源码编译",
};

/// 设置 → 品牌实例。
///
/// 两件事放在同一页：下载源（品牌安装慢的根因）与每个品牌的最后手段策略。
/// 未提权时整页只读——品牌列表、已装版本、当前生效的源都能看，写操作变灰，
/// 点保存/测速会弹开发者密码框。明文 HTTP 下提权本身不可用，就全部标灰并说明。
export function BrandManagementPanel() {
  const [settings, setSettings] = useState<BrandSettings | null>(null);
  const [status, setStatus] = useState<DeveloperStatus | null>(null);
  const [node, setNode] = useState<import("../api").NodeStatus | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, Draft>>({});
  const [probes, setProbes] = useState<Record<string, Record<string, BrandSourceProbe>>>({});
  const [probing, setProbing] = useState<Record<string, boolean>>({});
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoadError(null);
    try {
      const [next, developer, nodeStatus] = await Promise.all([
        api.brandSettings(),
        api.developerStatus(),
        api.nodeStatus().catch(() => null),
      ]);
      setSettings(next);
      setStatus(developer);
      setNode(nodeStatus);
      const initial: Record<string, Draft> = {
        pypi: { preset: next.sources.pypi.preset, url: next.sources.pypi.url },
        python: { preset: next.sources.python.preset, url: next.sources.python.url },
        archive: { preset: next.sources.archive.preset, url: next.sources.archive.url },
      };
      for (const [brand, state] of Object.entries(next.sources.release)) {
        initial[`release:${brand}`] = { preset: state.preset, url: state.url };
      }
      setDrafts(initial);
    } catch (e) {
      setLoadError(errMsg(e));
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const rows = useMemo<Row[]>(() => {
    if (!settings) return [];
    const list: Row[] = [
      {
        key: "pypi",
        kind: "pypi",
        title: "PyPI 索引",
        hint: KIND_HINT.pypi,
        state: settings.sources.pypi,
      },
      {
        key: "python",
        kind: "python",
        title: "CPython 运行时",
        hint: KIND_HINT.python,
        state: settings.sources.python,
      },
      {
        key: "archive",
        kind: "archive",
        title: "源码归档（codeload）",
        hint: KIND_HINT.archive,
        state: settings.sources.archive,
      },
    ];
    for (const [brand, state] of Object.entries(settings.sources.release)) {
      list.push({
        key: `release:${brand}`,
        kind: "release",
        brand: brand as BrandId,
        title: `${brand} 的 Release 源`,
        hint: KIND_HINT.release,
        state,
      });
    }
    return list;
  }, [settings]);

  if (loadError) {
    return (
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">品牌实例</span>
          <span className="comp-value">
            <span className="tag failed">加载失败</span>
          </span>
        </div>
        <p style={{ fontSize: 12, padding: "4px 8px 0" }}>{loadError}</p>
        <div style={{ display: "flex", paddingTop: 8 }}>
          <button className="btn btn-outline ripple-host" onClick={() => void load()}>
            重试
          </button>
        </div>
      </div>
    );
  }

  if (!settings || !status) {
    return (
      <div className="card">
        <p style={{ padding: 8, fontSize: 12 }}>加载中…</p>
      </div>
    );
  }

  const editable = status.https && status.elevated;

  /** 写操作的统一入口：缺提权就弹一次密码框，成功后重试一次。 */
  const withElevation = async <T,>(action: () => Promise<T>): Promise<T> => {
    try {
      return await action();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      await load();
      return await action();
    }
  };

  const setDraft = (key: string, patch: Partial<Draft>) => {
    setDrafts((current) => ({ ...current, [key]: { ...(current[key] ?? { preset: "", url: "" }), ...patch } }));
  };

  const save = async () => {
    if (!editable) return;
    const pypi = drafts.pypi ?? { preset: settings.sources.pypi.preset, url: settings.sources.pypi.url };
    const python = drafts.python ?? { preset: settings.sources.python.preset, url: settings.sources.python.url };
    const archive = drafts.archive ?? { preset: settings.sources.archive.preset, url: settings.sources.archive.url };
    const release: Record<string, BrandSourceChoice> = {};
    for (const [brand, state] of Object.entries(settings.sources.release)) {
      const draft = drafts[`release:${brand}`] ?? { preset: state.preset, url: state.url };
      release[brand] = { preset: draft.preset, url: draft.url };
    }
    setSaving(true);
    try {
      const next = await withElevation(() =>
        api.setBrandSources({ pypi, python, archive, release }),
      );
      setSettings(next);
      toast("success", "下载源已保存");
      await load();
    } catch (e) {
      toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setSaving(false);
    }
  };

  const probeRow = async (row: Row) => {
    if (!editable) return;
    setProbing((current) => ({ ...current, [row.key]: true }));
    try {
      // 每个 preset 各测一次；“跟随部署环境”没有自己的地址，测的就是当前生效的那个。
      const targets = row.state.presets.map((preset) => ({
        id: preset.id,
        url: preset.url || row.state.effective,
      }));
      const measured: Record<string, BrandSourceProbe> = {};
      const results = await withElevation(() =>
        Promise.all(
          targets.map(async (target) => {
            if (!target.url) return null;
            try {
              return { id: target.id, probe: await api.probeBrandSource(row.kind, row.brand, target.url) };
            } catch (e) {
              return null;
            }
          }),
        ),
      );
      for (const result of results) {
        if (result) measured[result.id] = result.probe;
      }
      setProbes((current) => ({ ...current, [row.key]: measured }));
      if (Object.keys(measured).length === 0) {
        toast("error", "没有可测的地址：先选一个预设或填一个自定义地址");
      }
    } finally {
      setProbing((current) => ({ ...current, [row.key]: false }));
    }
  };

  const fastest = (row: Row): string | null => {
    const measured = probes[row.key];
    if (!measured) return null;
    let best: { id: string; millis: number } | null = null;
    for (const [id, probe] of Object.entries(measured)) {
      if (!probe.ok) continue;
      if (!best || probe.millis < best.millis) best = { id, millis: probe.millis };
    }
    if (!best) return null;
    const preset = row.state.presets.find((one) => one.id === best!.id);
    return `${preset?.label ?? best.id}（${best.millis} ms）`;
  };

  return (
    <>
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">品牌实例</span>
          <span className="comp-value">
            {status.https ? (
              <span className={`tag ${status.elevated ? "running" : "plain"}`}>
                {status.elevated ? "已提权，可修改" : "只读"}
              </span>
            ) : (
              <span className="tag failed">明文 HTTP</span>
            )}
          </span>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          {status.https
            ? status.elevated
              ? "下载源与策略都可修改；改动立即生效于下一次安装。"
              : "现在只能查看：点保存或测速时会要求输入开发者密码（与打开终端用的是同一个）。"
            : "面板没有启用 HTTPS，开发者模式不可用：下载源与策略只能查看。装证书在「设置 → HTTPS 证书」。"}
        </p>
      </div>

      <div className="card">
        <div className="comp-row">
          <span className="comp-label">下载源</span>
          <span className="comp-value" style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            npm 不在这里（见「设置 → 下载源」）
          </span>
        </div>
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
          品牌安装的下载全部走这三类源。点「测速」会从面板这台机器实测每个候选，
          最快的那个标「推荐」——但只有你点「保存」才会改。
        </p>
      </div>

      {rows.map((row) => {
        const draft = drafts[row.key] ?? { preset: row.state.preset, url: row.state.url };
        const measured = probes[row.key] ?? {};
        const recommended = fastest(row);
        const custom = draft.preset === "custom";
        return (
          <div className="card" key={row.key}>
            <div className="comp-row">
              <span className="comp-label">{row.title}</span>
              <span className="comp-value">
                <span className="tag plain">{SOURCE_LABEL[row.state.source] ?? row.state.source}</span>
              </span>
            </div>
            <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
              {row.hint}
            </p>
            <div className="comp-row">
              <span className="comp-label">当前生效</span>
              <span className="comp-value mono" style={{ fontSize: 12, wordBreak: "break-all" }}>
                {row.state.effective || "—"}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">选择</span>
              <span className="comp-value">
                <select
                  className="input"
                  value={draft.preset}
                  disabled={!editable}
                  onChange={(e) => setDraft(row.key, { preset: e.target.value })}
                >
                  {row.state.presets.map((preset) => (
                    <option key={preset.id} value={preset.id}>
                      {preset.label}
                      {measured[preset.id] ? ` · ${measured[preset.id].millis} ms` : ""}
                    </option>
                  ))}
                  <option value="custom">自定义地址</option>
                </select>
              </span>
            </div>
            {custom && (
              <div className="comp-row">
                <span className="comp-label">地址</span>
                <span className="comp-value">
                  <input
                    className="input"
                    placeholder="https://…"
                    value={draft.url}
                    disabled={!editable}
                    onChange={(e) => setDraft(row.key, { url: e.target.value })}
                  />
                </span>
              </div>
            )}
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8, alignItems: "center", paddingTop: 8 }}>
              <button
                className="btn btn-outline ripple-host"
                disabled={!editable || probing[row.key]}
                title={editable ? "从面板这台机器实测每个候选" : "需要开发者密码"}
                onClick={() => void probeRow(row)}
              >
                {probing[row.key] ? "测速中…" : "测速"}
              </button>
              {recommended && <span className="tag running">推荐：{recommended}</span>}
              {Object.entries(measured).map(([id, probe]) => (
                <span key={id} className="tag plain" title={probe.error ?? ""}>
                  {row.state.presets.find((one) => one.id === id)?.label ?? id}
                  ：{probe.ok ? `${probe.millis} ms` : "不可用"}
                </span>
              ))}
            </div>
          </div>
        );
      })}

      <div style={{ display: "flex", gap: 8, alignItems: "center", padding: "0 4px" }}>
        <button
          className="btn btn-raised ripple-host"
          disabled={!editable || saving}
          title={editable ? "" : "需要开发者密码"}
          onClick={() => void save()}
        >
          {saving ? "保存中…" : "保存下载源"}
        </button>
        <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
          安装进行中也可以改，但从下一次安装开始生效。
        </span>
      </div>

      <GitHubTokenCard editable={editable} withElevation={withElevation} />

      {settings.policy.brands.map((entry) => (
        <BrandPolicyCard
          key={entry.brand}
          entry={entry}
          node={node}
          editable={editable}
          onSaved={load}
          withElevation={withElevation}
        />
      ))}

      <TranscodeFeatureCard />
      <McFeatureCard />
    </>
  );
}

/// GitHub 访问令牌：面板自己用，agent 读不到。
///
/// 版本目录的匿名额度是 60 次/小时/IP；一个只读令牌提高到 5000 次/小时。
/// 令牌只回传"是否配置 + 登录名"，本身永远不会出现在浏览器里。
function GitHubTokenCard({
  editable,
  withElevation,
}: {
  editable: boolean;
  withElevation: <T,>(action: () => Promise<T>) => Promise<T>;
}) {
  const [status, setStatus] = useState<GitHubTokenStatus | null>(null);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    api.gitHubToken()
      .then((value) => { if (alive) setStatus(value); })
      .catch(() => { if (alive) setStatus(null); });
    return () => { alive = false; };
  }, []);

  const save = async () => {
    const token = draft.trim();
    if (!token) return;
    setBusy(true);
    try {
      const next = await withElevation(() => api.saveGitHubToken(token));
      setStatus(next);
      setDraft("");
      toast("success", next.login ? `已保存 GitHub 令牌（@${next.login}）` : "已保存 GitHub 令牌");
    } catch (e) {
      toast("error", `保存失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  const clear = async () => {
    if (!window.confirm("删除面板保存的 GitHub 令牌？删除后回到匿名额度（60 次/小时）。")) return;
    setBusy(true);
    try {
      await withElevation(() => api.clearGitHubToken());
      setStatus({ configured: false, login: null });
      toast("info", "已删除 GitHub 令牌");
    } catch (e) {
      toast("error", `删除失败：${errMsg(e)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">GitHub 访问令牌</span>
        <span className="comp-value">
          {status?.configured ? (
            <span className="tag running">已配置{status.login ? ` @${status.login}` : ""}</span>
          ) : (
            <span className="tag plain">未配置（匿名 60 次/时）</span>
          )}
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        GitHub 版本列表的匿名额度是 <b>60 次/小时/IP</b>；填一个只读令牌后是
        <b> 5000 次/小时</b>。建议用 fine-grained PAT，只勾 public repositories 的只读权限；
        面板只调公开的 tags/commits/rate_limit 接口。令牌存在面板自己的
        <code> auth/github.json</code>（0600，agent 进程读不到），不会回显，也不会返回浏览器。
      </p>
      <div className="comp-row">
        <span className="comp-label">{status?.configured ? "替换令牌" : "填写令牌"}</span>
        <span className="comp-value" style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input
            className="input"
            type="password"
            autoComplete="off"
            placeholder="github_pat_… / ghp_…"
            value={draft}
            disabled={!editable || busy}
            onChange={(e) => setDraft(e.target.value)}
            style={{ flex: 1, minWidth: 220 }}
          />
          <button
            className="btn btn-outline ripple-host"
            disabled={!editable || busy || !draft.trim()}
            onClick={() => void save()}
          >
            {busy ? "保存中…" : "保存"}
          </button>
          {status?.configured && (
            <button
              className="btn btn-text danger"
              disabled={!editable || busy}
              onClick={() => void clear()}
            >
              删除
            </button>
          )}
        </span>
      </div>
      {!editable && (
        <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "0 8px 8px" }}>
          需要 HTTPS 且已用开发者密码提权才能修改。
        </p>
      )}
    </div>
  );
}

/// 一个品牌的策略卡片：能力位决定显示哪些开关，新增品牌不需要改这里。
function BrandPolicyCard({
  entry,
  node,
  editable,
  onSaved,
  withElevation,
}: {
  entry: BrandPolicyEntry;
  node: import("../api").NodeStatus | null;
  editable: boolean;
  onSaved: () => Promise<void>;
  withElevation: <T,>(action: () => Promise<T>) => Promise<T>;
}) {
  const [singleInstance, setSingleInstance] = useState(entry.singleInstance);
  const [survivor, setSurvivor] = useState<string | null>(entry.survivor);
  const [lockedRoute, setLockedRoute] = useState(entry.lockedRoute);
  const [sourceBuild, setSourceBuild] = useState(entry.sourceBuild);
  const [nodeRuntime, setNodeRuntime] = useState(entry.nodeRuntime ?? "");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setSingleInstance(entry.singleInstance);
    setSurvivor(entry.survivor);
    setLockedRoute(entry.lockedRoute);
    setSourceBuild(entry.sourceBuild);
    setNodeRuntime(entry.nodeRuntime ?? "");
  }, [entry]);

  const capabilities = entry.capabilities ?? [];
  const instances = entry.instances;
  /// 单实例/固定反代只在这两个品牌上出现：它们的 agent 卡死时，这是唯一还开着的门。
  const lastResort = capabilities.includes("singleInstance");

  const save = async () => {
    if (lastResort && singleInstance && !survivor) {
      toast("error", "单实例模式必须先选择保留实例");
      return;
    }
    setSaving(true);
    try {
      await withElevation(() =>
        api.setBrandPolicy(entry.brand, {
          singleInstance: lastResort && singleInstance,
          survivor: lastResort && singleInstance ? survivor : null,
          lockedRoute: lastResort && singleInstance && lockedRoute,
          sourceBuild,
          nodeRuntime: nodeRuntime || null,
        }),
      );
      toast("success", `${entry.brand} 的策略已保存`);
      await onSaved();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setSaving(false);
    }
  };

  const consumerFor = (instanceId: string) =>
    node?.consumers.find(
      (c) => c.kind === "brand-instance" && c.brand === entry.brand && c.id === instanceId,
    );

  const revert = async (instanceId: string) => {
    try {
      await api.nodeRevert({ kind: "brand-instance", brand: entry.brand, id: instanceId });
      toast("success", "已切回上次成功启动的 Node.js");
      await onSaved();
    } catch (e) {
      toast("error", `回退失败：${errMsg(e)}`);
    }
  };

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">{entry.brand} 策略</span>
        <span className="comp-value">
          <span className="tag plain">{lastResort ? "最后手段" : "Node.js 运行时"}</span>
        </span>
      </div>
      <div style={{ display: "grid", gap: 10, padding: "10px 8px 4px" }}>
        {lastResort && (
          <>
            <label className="input-check">
              <input
                type="checkbox"
                checked={singleInstance}
                disabled={!editable}
                onChange={(e) => {
                  const on = e.target.checked;
                  setSingleInstance(on);
                  if (on && !survivor && instances.length > 0) setSurvivor(instances[0].id);
                  if (!on) setLockedRoute(false);
                }}
              />
              单实例模式：只保留一个实例，其余灰掉并禁用
            </label>

            <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
              保留实例
              <select
                className="input"
                style={{ flex: "none", maxWidth: 260 }}
                value={survivor ?? ""}
                disabled={!editable || !singleInstance || instances.length === 0}
                onChange={(e) => setSurvivor(e.target.value || null)}
              >
                <option value="">请选择</option>
                {instances.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.name} · {i.id}
                  </option>
                ))}
              </select>
            </label>

            {capabilities.includes("lockedRoute") && (
              <label className="input-check" style={{ opacity: singleInstance ? 1 : 0.55 }}>
                <input
                  type="checkbox"
                  checked={lockedRoute}
                  disabled={!editable || !singleInstance || !survivor}
                  onChange={(e) => setLockedRoute(e.target.checked)}
                />
                固定反代路径 <span className="mono">/i/{entry.brand}/</span>（要求单实例）
              </label>
            )}
          </>
        )}

        {capabilities.includes("sourceBuild") && (
          <label className="input-check">
            <input
              type="checkbox"
              checked={sourceBuild}
              disabled={!editable}
              onChange={(e) => setSourceBuild(e.target.checked)}
            />
            {CAPABILITY_LABEL.sourceBuild}（Linux；下载源码 + uv + 前端，首次较慢）
          </label>
        )}

        <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
          默认 Node.js
          <select
            className="input"
            style={{ flex: "none", maxWidth: 280 }}
            value={nodeRuntime}
            disabled={!editable}
            onChange={(e) => setNodeRuntime(e.target.value)}
          >
            <option value="">跟随全局默认</option>
            <option value="auto">自动（PATH 优先，回落到托管）</option>
            {(node?.channels ?? []).filter((c) => c.installed).map((c) => (
              <option key={c.name} value={c.name}>
                Node {c.name}{c.lts ? "（LTS）" : ""}{c.resolved ? ` · ${c.resolved}` : ""}
              </option>
            ))}
            {(node?.runtimes ?? []).map((r) => (
              <option key={r.version} value={r.version}>精确 {r.version}</option>
            ))}
            {nodeRuntime !== "" && nodeRuntime !== "auto"
              && !(node?.channels ?? []).some((c) => c.name === nodeRuntime && c.installed)
              && !(node?.runtimes ?? []).some((r) => r.version === nodeRuntime) && (
                <option value={nodeRuntime}>{nodeRuntime}</option>
              )}
          </select>
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            实例自己没选时继承它
          </span>
        </label>

        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button
            className="btn btn-raised ripple-host"
            disabled={!editable || saving}
            title={editable ? "" : "需要开发者密码"}
            onClick={() => void save()}
          >
            {saving ? "保存中…" : "保存策略"}
          </button>
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            {lastResort
              ? "固定路径只对保留实例生效；关闭策略不会删除数据。"
              : "只影响这个品牌的默认 Node.js，不改动实例数据。"}
          </span>
        </div>

        <div style={{ borderTop: "1px solid var(--monet-outline-variant)", paddingTop: 8 }}>
          {instances.length === 0 ? (
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              还没有 {entry.brand} 实例。
            </span>
          ) : (
            instances.map((i) => (
              <div
                key={i.id}
                style={{
                  display: "flex",
                  gap: 8,
                  alignItems: "center",
                  padding: "3px 0",
                  opacity: i.disabled ? 0.5 : 1,
                  fontSize: 12,
                }}
              >
                <span className="mono">{i.id}</span>
                <span>{i.name}</span>
                <span className="tag plain">{i.state}</span>
                <span className="tag plain">{i.version}</span>
                {i.id === survivor && singleInstance && <span className="tag running">保留</span>}
                {i.disabled && <span className="tag plain">禁用</span>}
                {(() => {
                  const consumer = consumerFor(i.id);
                  if (!consumer) return null;
                  return (
                    <>
                      {consumer.requirement && (
                        <span className="tag plain" title="engines.node">要求 {consumer.requirement}</span>
                      )}
                      {!consumer.compatible && <span className="tag plain">Node 不满足</span>}
                      {consumer.lastGood && consumer.lastGood !== consumer.resolved && (
                        <button
                          className="btn btn-text ripple-host"
                          style={{ height: 22 }}
                          onClick={() => void revert(i.id)}
                        >
                          回退到 {consumer.lastGood}
                        </button>
                      )}
                    </>
                  );
                })()}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}