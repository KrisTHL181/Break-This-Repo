import { I18N } from "../i18n";
import { InfoIcon } from "./icons";
import { TEXT_BOX_STYLE } from "./textBoxStyle";
import privacyText from "../legal/privacy-v1.txt?raw";
import aupText from "../legal/aup-v1.txt?raw";

/**
 * 「关于」页的合规段：数据与隐私、可接受使用。
 *
 * 两份都是说人话的说明，没有法律效力，和 MC 开关无关——面板始终在存东西、
 * 始终可能被开放给别人用，所以这两段常驻。有法律效力的文本是 LICENSE、
 * NOTICE 与 MC 功能的《使用条款》。
 */
export function LegalCard() {
  return (
    <>
      <div className="section-caption" style={{ marginTop: 18 }}>
        <InfoIcon size={16} />
        {I18N["about.legal"]}
      </div>
      <div className="card">
        <div style={{ padding: "2px 8px 6px" }}>
          <p className="dim" style={{ fontSize: 12, margin: "0 0 8px", lineHeight: 1.6 }}>
            {I18N["about.legal.intro"]}
          </p>
          <details>
            <summary style={{ cursor: "pointer", fontSize: 13 }}>{I18N["about.legal.privacy"]}</summary>
            <div style={TEXT_BOX_STYLE}>{privacyText}</div>
          </details>
          <details style={{ marginTop: 8 }}>
            <summary style={{ cursor: "pointer", fontSize: 13 }}>{I18N["about.legal.aup"]}</summary>
            <div style={TEXT_BOX_STYLE}>{aupText}</div>
          </details>
        </div>
      </div>
    </>
  );
}
