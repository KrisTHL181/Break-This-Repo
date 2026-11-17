import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, type McEula, type McTerms } from "../api";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import { refreshMcFeature, setMcFeature, useMcFeature } from "../mcFeature";
import { toast } from "../store";
import { errMsg } from "../utils";

/// 设置 → 品牌实例 里的「MC 服务端」开关。
///
/// 功能默认关闭；开启是唯一需要读 EULA 的动作（`docs/minecraft.md` §5）。这一块
/// 以前挂在 `/mc` 页面顶部，但开关是「一年动一次」的东西，和品牌策略同页更合适：
/// 日常运营者不该在实例列表里看到它。
///
/// 开启成功后广播到 [useMcFeature]，实例列表据此长出「MC 服务器实例」入口。
export function McFeatureCard() {
  const feature = useMcFeature();
  const nav = useNavigate();
  const [eula, setEula] = useState<McEula | null>(null);
  const [terms, setTerms] = useState<McTerms | null>(null);
  const [eulaError, setEulaError] = useState<string | null>(null);
  const [wizard, setWizard] = useState(false);
  const [scrolledToEnd, setScrolledToEnd] = useState(false);
  const [eulaAccepted, setEulaAccepted] = useState(false);
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [enableError, setEnableError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  // 第二道关：第三方核心。只在主功能开启后存在（关闭时后端 404）。
  const [thirdParty, setThirdParty] = useState<{ state: string; enabled: boolean } | null>(null);
  const [tpTerms, setTpTerms] = useState<McTerms | null>(null);
  const [tpDialog, setTpDialog] = useState(false);
  const [tpAccepted, setTpAccepted] = useState(false);
  const [tpError, setTpError] = useState<string | null>(null);
  const [tpBusy, setTpBusy] = useState(false);

  useEffect(() => {
    void refreshMcFeature();
  }, []);

  useEffect(() => {
    if (feature?.enabled !== true) {
      setThirdParty(null);
      return;
    }
    void api.mc
      .thirdParty()
      .then((answer) => setThirdParty({ state: answer.state, enabled: answer.enabled }))
      .catch(() => setThirdParty(null));
  }, [feature?.enabled, feature?.thirdParty]);

  const locked = feature?.locked === true;
  useEffect(() => {
    if (!locked) return;
    const timer = window.setInterval(() => setTick((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [locked]);

  const remaining = useMemo(() => {
    if (!feature?.locked) return 0;
    // tick 只是心跳；剩余秒数以服务端回答为锚，本地按墙钟往前推。
    return Math.max(0, feature.lockRemainingSeconds - tick);
  }, [feature, tick]);

  const withElevation = async <T,>(call: () => Promise<T>): Promise<T> => {
    try {
      return await call();
    } catch (e) {
      if (!isElevationRequired(e)) throw e;
      if (!(await requestDeveloperPassword())) throw e;
      return await call();
    }
  };

  const openWizard = async () => {
    setWizard(true);
    setEnableError(null);
    setEula(null);
    setTerms(null);
    setEulaError(null);
    setScrolledToEnd(false);
    setEulaAccepted(false);
    setTermsAccepted(false);
    try {
      const [eulaAnswer, termsAnswer] = await Promise.all([api.mc.eula(), api.mc.terms()]);
      setEula(eulaAnswer);
      setTerms(termsAnswer);
    } catch (e) {
      const message = errMsg(e);
      // 拉不到 EULA 也就下载不了服务端；不给「离线导入」这种假出路。
      setEulaError(
        message.includes("eula_unavailable")
          ? "网络问题：连不上 Mojang，拉不到 EULA（服务端也下载不了）。检查网络或代理后重试。"
          : message,
      );
    }
  };

  const enable = async () => {
    if (!eula || !terms) return;
    setBusy(true);
    setEnableError(null);
    try {
      const next = await withElevation(() =>
        api.mc.enable({
          eulaAcknowledged: eulaAccepted,
          termsAcknowledged: termsAccepted,
          eulaHash: eula.hash,
          termsVersion: terms.version,
          termsTextHash: terms.hash,
          sawDeclineOption: true,
          uiVersion: "1.0.0",
        }),
      );
      setMcFeature(next);
      setWizard(false);
      toast("success", "MC 服务端功能已开启：入口在「实例列表 → 品牌实例」下面");
      await refreshMcFeature();
    } catch (e) {
      // Inline as well as a toast: this is the button people press to turn the
      // feature on, and a toast that has already faded is indistinguishable from a
      // button that did nothing.
      setEnableError(errMsg(e));
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const stopFeature = async () => {
    const phrase = window.prompt("这会停掉所有 MC 服务器，并在 1 小时内不能重新开启。\n输入 STOP 继续：");
    if (phrase !== "STOP") return;
    setBusy(true);
    try {
      setMcFeature(await withElevation(() => api.mc.stopFeature()));
      setTick(0);
      await refreshMcFeature();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const confirmClose = async () => {
    setBusy(true);
    try {
      setMcFeature(await withElevation(() => api.mc.confirmClose()));
      await refreshMcFeature();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const keepFeature = async () => {
    setBusy(true);
    try {
      setMcFeature(await withElevation(() => api.mc.cancelClose()));
      await refreshMcFeature();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const openThirdParty = async () => {
    setTpDialog(true);
    setTpError(null);
    setTpAccepted(false);
    try {
      setTpTerms(await api.mc.thirdPartyTerms());
    } catch (e) {
      setTpError(errMsg(e));
    }
  };

  const enableThirdParty = async () => {
    if (!tpTerms) return;
    setTpBusy(true);
    setTpError(null);
    try {
      await withElevation(() =>
        api.mc.enableThirdParty({
          termsVersion: tpTerms.version,
          termsTextHash: tpTerms.hash,
          uiVersion: "1.0.0",
        }),
      );
      setTpDialog(false);
      setThirdParty({ state: "ENABLED", enabled: true });
      toast("success", "第三方核心已开启：新建服务器时可选 Paper / Folia / Purpur / Fabric");
      await refreshMcFeature();
    } catch (e) {
      setTpError(errMsg(e));
      toast("error", errMsg(e));
    } finally {
      setTpBusy(false);
    }
  };

  const disableThirdParty = async () => {
    setTpBusy(true);
    try {
      await withElevation(() => api.mc.disableThirdParty());
      setThirdParty({ state: "DISABLED", enabled: false });
      toast("info", "第三方核心已关闭：已建实例保留，新建时不再提供");
      await refreshMcFeature();
    } catch (e) {
      toast("error", errMsg(e));
    } finally {
      setTpBusy(false);
    }
  };

  const enabled = feature?.enabled === true;

  return (
    <div className="card">
      <div className="comp-row">
        <span className="comp-label">MC 服务端（第三方功能）</span>
        <span className="comp-value">
          {enabled ? (
            <span className={`tag ${locked ? "plain" : "running"}`}>{locked ? "冷却中" : "已开启"}</span>
          ) : (
            <span className="tag plain">默认关闭</span>
          )}
        </span>
      </div>
      <p style={{ fontSize: 12, color: "var(--monet-on-surface-variant)", padding: "4px 8px 0", lineHeight: 1.6 }}>
        把原版 Minecraft 服务端当成一个实例来管：版本、端口、进程、控制台、世界与备份。
        关闭时 `/api/mc/*` 全部 404、不开任何端口，面板与平时没有区别。
        {enabled && " 实例入口在「实例列表 → 品牌实例」下面。"}
      </p>

      {feature && (
        <div style={{ padding: "6px 8px 0", fontSize: 12, lineHeight: 1.8, color: "var(--monet-on-surface-variant)" }}>
          <div style={{ display: "flex", gap: 6, alignItems: "baseline", flexWrap: "wrap" }}>
            <span>服务器目录：</span>
            <span className="mono" style={{ wordBreak: "break-all" }}>{feature.storage.root}</span>
            <span className="dim">（{feature.storage.source}）</span>
            <button
              className="btn btn-text"
              style={{ padding: "0 4px", minHeight: 0 }}
              onClick={() => {
                const text = feature.storage.root;
                if (!navigator.clipboard?.writeText) {
                  toast("info", "当前页面（非 HTTPS）不允许剪贴板写入，请手动选中路径");
                  return;
                }
                void navigator.clipboard.writeText(text)
                  .then(() => toast("success", "路径已复制"))
                  .catch(() => toast("error", "复制失败，请手动选中路径"));
              }}
            >
              复制
            </button>
            {!feature.storage.writable && (
              <span style={{ color: "var(--monet-error)" }}>当前不可写，创建实例前请先修好目录权限</span>
            )}
          </div>
          <div style={{ display: "flex", gap: 6, alignItems: "baseline", flexWrap: "wrap" }}>
            <span>控制台：</span>
            <span>{feature.console === "pipe" ? "日志 + 命令输入（本平台没有 PTY）" : "PTY 交互终端"}</span>
          </div>
          <div style={{ display: "flex", gap: 6, alignItems: "baseline", flexWrap: "wrap" }}>
            <span>Java：</span>
            <span className="mono" style={{ wordBreak: "break-all" }}>{feature.java.path || "（未解析）"}</span>
            <span className={feature.java.ok ? "dim" : undefined}
                  style={feature.java.ok ? undefined : { color: "var(--monet-error)" }}>
              {feature.java.ok
                ? `（${feature.java.version ?? "版本未知"}，${feature.java.source === "panel"
                    ? "来自面板运行时"
                    : feature.java.source === "installed"
                      ? "来自本机已安装的 JDK（按 MC 版本要求挑选）"
                      : "来自 mc.java_path"}）`
                : `（不可用：${feature.java.error ?? "未知原因"}）`}
            </span>
          </div>
          <div className="dim" style={{ fontSize: 11 }}>
            面板默认复用自身的 Java；所选 MC 版本要求更高的 Java 时，会先用面板自托管的
            JRE（Adoptium，`&lt;数据目录&gt;/minecraft/java/&lt;feature&gt;/`），再用本机已装的
            JDK，都没有时才拒绝启动。
            {feature.java.managed && feature.java.managed.length > 0
              ? ` 已托管：${feature.java.managed.map((entry) => `Java ${entry.feature}`).join("、")}。`
              : " 目前没有托管 JRE，需要时可用 API 安装（`POST /api/mc/java/install`）。"}
            每个实例在 `&lt;服务器目录&gt;/&lt;id&gt;/`，存档、server.jar 与日志都在那里；
            改 `mc.dir` / `HDSL_MC_DIR` 不会搬动已建实例。
          </div>
        </div>
      )}

      {enabled && (
        <div
          style={{
            padding: "10px 8px 0",
            display: "flex",
            gap: 8,
            alignItems: "center",
            flexWrap: "wrap",
            fontSize: 13,
          }}
        >
          <b>第三方核心</b>
          <span className={`tag ${thirdParty?.enabled ? "running" : "plain"}`}>
            {thirdParty?.enabled ? "已开启" : "未开启"}
          </span>
          {thirdParty?.enabled ? (
            <button className="btn btn-text" disabled={tpBusy} onClick={() => void disableThirdParty()}>
              关闭第三方核心
            </button>
          ) : (
            <button className="btn btn-raised" disabled={tpBusy} onClick={() => void openThirdParty()}>
              开启第三方核心
            </button>
          )}
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            Paper / Folia / Purpur / Fabric：从各上游下载，面板不担保可用性。
            只有主功能开启后才能开；关闭主功能前必须先关它。
          </span>
        </div>
      )}

      {tpDialog && enabled && (
        <ThirdPartyWizard
          terms={tpTerms}
          accepted={tpAccepted}
          onAccept={setTpAccepted}
          busy={tpBusy}
          error={tpError}
          onEnable={() => void enableThirdParty()}
          onRetry={() => void openThirdParty()}
          onCancel={() => {
            if (tpBusy) return;
            void api.mc.declineThirdParty().catch(() => undefined);
            setTpDialog(false);
          }}
        />
      )}

      {locked && (
        <div style={{ padding: "8px 8px 0", display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", fontSize: 13 }}>
          <b>已按下停止功能</b>
          <span>{formatDuration(remaining)} 后才能启服 / 重新开启</span>
          {feature?.keepFeature == null && (
            <>
              <button className="btn btn-raised" disabled={busy} onClick={() => void keepFeature()}>
                我不想关掉功能
              </button>
              <button className="btn btn-raised danger" disabled={busy} onClick={() => void confirmClose()}>
                确认关闭功能
              </button>
            </>
          )}
          {feature?.keepFeature === true && <span className="dim">已选择保留功能，倒计时结束才能启服。</span>}
          {feature?.keepFeature === false && <span className="dim">功能已关闭，冷却结束后才能重新开启。</span>}
        </div>
      )}

      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", padding: "10px 8px 4px" }}>
        {enabled ? (
          <>
            <button className="btn btn-raised" onClick={() => nav("/instances?view=mc")}>
              打开 MC 实例列表
            </button>
            <button className="btn btn-text danger" disabled={busy || locked} onClick={() => void stopFeature()}>
              停止 MC 功能
            </button>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              服务端程序不随面板分发，首次安装时从 Mojang 官方下载。
            </span>
          </>
        ) : locked ? (
          <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
            功能已关闭，{formatDuration(remaining)} 后才能重新开启。重启面板进程会清掉这段冷却。
          </span>
        ) : (
          <>
            <button className="btn btn-raised" disabled={busy} onClick={() => void openWizard()}>
              开启功能
            </button>
            <span style={{ fontSize: 12, color: "var(--monet-on-surface-variant)" }}>
              需要开发者密码；开启前要读 Mojang 的 EULA 与面板自己的使用条款。
            </span>
          </>
        )}
      </div>

      {wizard && !enabled && (
        <EnableWizard
          eula={eula}
          terms={terms}
          eulaError={eulaError}
          scrolledToEnd={scrolledToEnd}
          onScroll={(atEnd) => setScrolledToEnd(atEnd)}
          eulaAccepted={eulaAccepted}
          termsAccepted={termsAccepted}
          onEula={setEulaAccepted}
          onTerms={setTermsAccepted}
          busy={busy}
          enableError={enableError}
          onEnable={() => void enable()}
          onRetry={() => void openWizard()}
          onDecline={() => {
            void api.mc.decline().catch(() => undefined);
            setWizard(false);
            toast("info", "已保持关闭");
          }}
        />
      )}
    </div>
  );
}

function formatDuration(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(total / 60);
  return minutes > 0 ? `${minutes} 分 ${total % 60} 秒` : `${total} 秒`;
}

/// 开启向导：读完 EULA（滚到底才能勾）+ 双确认。
///
/// EULA 由服务端拉取并**序列化**成纯文本（`McHtml`）：正文在 Mojang 页面中间，
/// 前后是导航与 cookie 提示，直接贴原文就是一堆标签。
function EnableWizard({
  eula,
  terms,
  eulaError,
  scrolledToEnd,
  onScroll,
  eulaAccepted,
  termsAccepted,
  onEula,
  onTerms,
  busy,
  enableError,
  onEnable,
  onRetry,
  onDecline,
}: {
  eula: McEula | null;
  terms: McTerms | null;
  eulaError: string | null;
  scrolledToEnd: boolean;
  onScroll: (atEnd: boolean) => void;
  eulaAccepted: boolean;
  termsAccepted: boolean;
  onEula: (value: boolean) => void;
  onTerms: (value: boolean) => void;
  busy: boolean;
  enableError: string | null;
  onEnable: () => void;
  onRetry: () => void;
  onDecline: () => void;
}) {
  const boxRef = useRef<HTMLDivElement | null>(null);
  const getScrollReport = useRef(onScroll);
  getScrollReport.current = onScroll;

  // 条款短到不需要滚动时，`onScroll` 永远不会触发，两个勾选框就永远是灰的——
  // 整个向导看上去是个摆设。量一次高度，装得下就算读过了。
  useEffect(() => {
    const box = boxRef.current;
    if (box && box.scrollHeight <= box.clientHeight + 4) getScrollReport.current(true);
  }, [eula]);

  return (
    <div style={{ padding: "4px 8px 8px", display: "flex", flexDirection: "column", gap: 10 }}>
      <b>开启 MC 服务端功能</b>
      {eulaError && (
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ color: "var(--monet-error)" }}>{eulaError}</div>
          <button className="btn btn-raised" disabled={busy} onClick={onRetry}>
            重试
          </button>
        </div>
      )}
      {eula && (
        <>
          <div
            ref={boxRef}
            style={{
              height: 260,
              overflow: "auto",
              border: "1px solid var(--monet-outline-variant)",
              padding: 8,
              fontSize: 12,
              lineHeight: 1.6,
              whiteSpace: "pre-wrap",
            }}
            onScroll={(event) => {
              const el = event.currentTarget;
              onScroll(el.scrollTop + el.clientHeight >= el.scrollHeight - 4);
            }}
          >
            {eula.text}
          </div>
          <label style={{ display: "flex", gap: 8, alignItems: "flex-start", fontSize: 13 }}>
            <input type="checkbox" disabled={!scrolledToEnd} checked={eulaAccepted} onChange={(e) => onEula(e.target.checked)} />
            <span>
              我已阅读并同意 Mojang 的《最终用户许可协议》（sha256:{eula.hash.slice(7, 19)}…，拉取于{" "}
              {eula.fetchedAt}）。
            </span>
          </label>
          <label style={{ display: "flex", gap: 8, alignItems: "flex-start", fontSize: 13 }}>
            <input type="checkbox" disabled={!scrolledToEnd} checked={termsAccepted} onChange={(e) => onTerms(e.target.checked)} />
            <span>
              我已阅读《MC 服务端管理功能使用条款》v{terms?.version ?? 3}
              （sha256:{terms?.hash.slice(7, 19) ?? "…"}…），理解开公网端口、插件代码执行与备份责任由我承担。
            </span>
          </label>
          {terms && (
            <details>
              <summary className="dim" style={{ cursor: "pointer", fontSize: 12 }}>查看使用条款全文</summary>
              <div
                style={{
                  maxHeight: 240,
                  overflow: "auto",
                  border: "1px solid var(--monet-outline-variant)",
                  padding: 8,
                  marginTop: 6,
                  fontSize: 12,
                  lineHeight: 1.6,
                  whiteSpace: "pre-wrap",
                }}
              >
                {terms.text}
              </div>
            </details>
          )}
          {enableError && <div style={{ color: "var(--monet-error)" }}>{enableError}</div>}
          {!scrolledToEnd && (
            <div className="dim" style={{ fontSize: 12 }}>
              读到最后一页才能勾选——条款框滚到底即可（短到不需要滚动时自动算读过）。
            </div>
          )}
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button className="btn btn-raised" disabled={busy || !eulaAccepted || !termsAccepted} onClick={onEnable}>
              开启
            </button>
          </div>
        </>
      )}
      {!eula && !eulaError && (
        <div className="dim">正在拉取官方 EULA…（要连 www.minecraft.net，最长会等 20 秒）</div>
      )}
      {/* 退出这条路必须在任何状态下都在：拉不到 EULA 时，向导里一个能点的东西都没有。 */}
      <button className="btn btn-text" onClick={onDecline}>
        我已知晓风险，暂不启用
      </button>
    </div>
  );
}

/// 第二道关的确认面板：条款正文 + 一个勾选。
///
/// 比主功能的开启向导轻：这里没有 EULA 要拉、没有滚动门槛——条款是面板自己的
/// 风险告知，读完勾上就是决定。取消会记一条 `mc.thirdparty.declined` 审计。
function ThirdPartyWizard({
  terms,
  accepted,
  onAccept,
  busy,
  error,
  onEnable,
  onRetry,
  onCancel,
}: {
  terms: McTerms | null;
  accepted: boolean;
  onAccept: (value: boolean) => void;
  busy: boolean;
  error: string | null;
  onEnable: () => void;
  onRetry: () => void;
  onCancel: () => void;
}) {
  return (
    <div style={{ padding: "4px 8px 8px", display: "flex", flexDirection: "column", gap: 10 }}>
      <b>开启第三方服务端核心</b>
      {error && !terms && (
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          <div style={{ color: "var(--monet-error)" }}>{error}</div>
          <button className="btn btn-raised" disabled={busy} onClick={onRetry}>
            重试
          </button>
        </div>
      )}
      {terms && (
        <>
          <div
            style={{
              height: 240,
              overflow: "auto",
              border: "1px solid var(--monet-outline-variant)",
              padding: 8,
              fontSize: 12,
              lineHeight: 1.6,
              whiteSpace: "pre-wrap",
            }}
          >
            {terms.text}
          </div>
          <label style={{ display: "flex", gap: 8, alignItems: "flex-start", fontSize: 13 }}>
            <input
              type="checkbox"
              checked={accepted}
              onChange={(event) => onAccept(event.target.checked)}
            />
            <span>
              我已阅读并理解上述条款（v{terms.version}，sha256:{terms.hash.slice(7, 19)}…）。
              第三方核心由各上游提供，面板不担保其可用性与安全性。
            </span>
          </label>
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
            <button className="btn btn-text" disabled={busy} onClick={onCancel}>
              取消
            </button>
            <button className="btn btn-raised" disabled={busy || !accepted} onClick={onEnable}>
              {busy ? "开启中…" : "开启第三方核心"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
