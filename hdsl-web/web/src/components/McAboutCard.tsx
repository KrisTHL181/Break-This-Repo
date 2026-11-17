import { useEffect, useState } from "react";
import { api, type McTerms } from "../api";
import { isElevationRequired, requestDeveloperPassword } from "../elevation";
import { I18N } from "../i18n";
import { refreshMcFeature, useMcFeature } from "../mcFeature";
import { errMsg } from "../utils";
import { InfoIcon } from "./icons";
import { TEXT_BOX_STYLE } from "./textBoxStyle";
import notesText from "../minecraft/notes-v1.txt?raw";

/**
 * 「关于」页里的 MC 段。
 *
 * 出现条件有两档：
 * - 功能开着 → 说明 + 条款原文 + 当前同意记录；
 * - 功能关着但留下过同意记录 → 只显示记录与文本，让用户还能回看「我当初同意的是
 *   哪一版」。关掉功能不该等于删掉证据。
 *
 * 说明（说人话版）随前端打包：不联网、不要提权。条款原文走 `/api/mc/terms`，
 * 和开启向导读的是同一份文本与哈希——它有法律效力，复制一份到前端迟早会漂移。
 */
export function McAboutCard() {
  const feature = useMcFeature();
  const [terms, setTerms] = useState<McTerms | null>(null);
  const [termsBusy, setTermsBusy] = useState(false);
  const [termsError, setTermsError] = useState<string | null>(null);

  useEffect(() => {
    void refreshMcFeature();
  }, []);

  const consent = feature?.termsAcceptedAt ? feature : null;
  if (!feature || (!feature.enabled && !consent)) return null;

  const loadTerms = async () => {
    if (terms || termsBusy) return;
    setTermsBusy(true);
    setTermsError(null);
    try {
      setTerms(await api.mc.terms());
    } catch (e) {
      if (!isElevationRequired(e)) {
        setTermsError(errMsg(e));
      } else if (await requestDeveloperPassword()) {
        try {
          setTerms(await api.mc.terms());
        } catch (retry) {
          setTermsError(errMsg(retry));
        }
      } else {
        setTermsError(I18N["about.mc.terms.cancelled"]);
      }
    } finally {
      setTermsBusy(false);
    }
  };

  return (
    <>
      <div className="section-caption" style={{ marginTop: 18 }}>
        <InfoIcon size={16} />
        {I18N["about.mc"]}
      </div>
      <div className="card">
        <div className="comp-row">
          <span className="comp-label">{I18N["about.mc.state"]}</span>
          <span className="comp-value">
            {feature.enabled ? I18N["about.mc.state.on"] : I18N["about.mc.state.off"]}
          </span>
        </div>
        {consent && (
          <>
            <div className="comp-row">
              <span className="comp-label">{I18N["about.mc.consent"]}</span>
              <span className="comp-value" style={{ fontSize: 12 }}>
                {formatTime(consent.termsAcceptedAt)} · {consent.acceptedBy ?? "—"} · 条款 v
                {consent.termsVersion ?? "—"}
                {consent.ackMethod ? ` · ${consent.ackMethod}` : ""}
              </span>
            </div>
            <div className="comp-row">
              <span className="comp-label">{I18N["about.mc.fingerprint"]}</span>
              <span className="comp-value mono" style={{ fontSize: 12 }}>
                条款 {shortHash(consent.termsTextHash)} · EULA {shortHash(consent.eulaHash)}
              </span>
            </div>
          </>
        )}
        <div style={{ padding: "2px 8px 6px" }}>
          <p className="dim" style={{ fontSize: 12, margin: "0 0 8px", lineHeight: 1.6 }}>
            {I18N["about.mc.intro"]}
          </p>
          <details>
            <summary style={{ cursor: "pointer", fontSize: 13 }}>{I18N["about.mc.notes.open"]}</summary>
            <div style={TEXT_BOX_STYLE}>{notesText}</div>
          </details>
          <details
            style={{ marginTop: 8 }}
            onToggle={(event) => {
              if (event.currentTarget.open) void loadTerms();
            }}
          >
            <summary style={{ cursor: "pointer", fontSize: 13 }}>
              {I18N["about.mc.terms.open"]}
              {terms ? `（v${terms.version}，${shortHash(terms.hash)}）` : ""}
            </summary>
            {termsBusy && (
              <div className="dim" style={{ fontSize: 12, marginTop: 6 }}>
                正在读取…
              </div>
            )}
            {termsError && (
              <div style={{ color: "var(--monet-error)", fontSize: 12, marginTop: 6 }}>{termsError}</div>
            )}
            {terms && <div style={TEXT_BOX_STYLE}>{terms.text}</div>}
          </details>
        </div>
      </div>
    </>
  );
}

/** ISO 时间 → 本地可读；解析不了就原样给出，别把证据显示成 Invalid Date。 */
function formatTime(value?: string): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
}

/** `sha256:abc…` 只留到能比对的前 12 位十六进制。 */
function shortHash(value?: string): string {
  if (!value) return "—";
  return `${value.slice(0, 19)}…`;
}
