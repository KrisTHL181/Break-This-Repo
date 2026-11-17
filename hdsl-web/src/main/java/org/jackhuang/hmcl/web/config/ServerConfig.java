/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.config;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;

/// The declared configuration of the HDSL-web server, as described in
/// docs/plan.md ("声明式 HTTPS 设计" and "认证设计").
///
/// Every field is optional: [ServerConfigLoader] fills the documented
/// defaults from `server.yaml` and the environment, and validates the result.
@NotNullByDefault
public final class ServerConfig {

    /// The directory all server-owned data lives in (`HDSL_DATA`, default `./data`).
    public Path dataDir = Path.of("data").toAbsolutePath().normalize();

    /// The address the main connector binds (`HDSL_BIND_HOST`, default `0.0.0.0`).
    public String bindHost = "0.0.0.0";

    /// The main connector port (`HDSL_PORT`, default 3080). 0 means "pick a
    /// free port", which tests use; the real port is then read from the
    /// running connector.
    public int port = 3080;

    /// Base path the panel is mounted under when a reverse proxy serves it
    /// from a subpath (`HDSL_BASE_PATH`, default "" = root mount). Given in
    /// "/panel" form; the empty string means root. The proxy is expected to
    /// strip the prefix before forwarding — the same mounting style as the
    /// panel's own `/i/<id>/` proxy. The value is injected into the served
    /// index.html (`<base href>` + `window.__HDSL_BASE__`) so the SPA's
    /// relative asset/API/WS paths resolve inside the mount.
    public String basePath = "";

    /// HTTPS settings; see [HttpsConfig].
    public final HttpsConfig https = new HttpsConfig();

    /// The public base URL of this instance behind a reverse proxy, e.g.
    /// `https://dsh.example.com`. Phase 1 only stores it; Phase 2 uses it for
    /// `--trusted-host` and the certificate manager already borrows its host
    /// for the self-signed SAN list.
    public String publicBaseUrl = "";

    /// Authentication settings; see [AuthConfig].
    public final AuthConfig auth = new AuthConfig();

    /// The web terminal switch; see [WebShellConfig].
    public final WebShellConfig webShell = new WebShellConfig();

    /// The experimental ZCode category; see [ZcodeConfig].
    public final ZcodeConfig zcode = new ZcodeConfig();

    /// ZCode category knobs. All of them come from the environment only — the
    /// shape of a ZCode distribution, and of the machine that builds it, is not
    /// something `server.yaml` promises.
    public static final class ZcodeConfig {
        /// Directory holding a self-built ZCode distribution
        /// (`HDSL_ZCODE_PACKAGE`); it must contain `bin/zcode.mjs`. Empty means
        /// the newest installed release below `<dataDir>/zcode/releases/`.
        public String packageDir = "";

        /// The node executable used to run ZCode (`HDSL_ZCODE_NODE`).
        /// Empty means `node` resolved from `PATH`.
        public String nodePath = "";

        /// The directory prepended to `PATH` while building
        /// (`HDSL_ZCODE_BUILD_BIN`). The image points it at a Node 24 install,
        /// because upstream's build refuses anything older. Empty keeps the
        /// panel's own `PATH`.
        public String buildBin = "";

        /// The pnpm executable used for the build (`HDSL_ZCODE_PNPM`). Tests
        /// point it at a stub; the default is whatever `PATH` resolves.
        public String pnpm = "pnpm";

        /// Source tarball template (`HDSL_ZCODE_SOURCE_URL`); `%s` is the tag or
        /// branch name. Defaults to the GitHub codeload endpoint for tags.
        public String sourceUrl = "https://codeload.github.com/zai-org/ZCode/tar.gz/refs/tags/%s";

        /// Keeps the build's source tree and dependency install afterwards
        /// (`HDSL_ZCODE_KEEP_SOURCES`) — for debugging a failing build, at the
        /// cost of gigabytes.
        public boolean keepSources = false;
    }

    public static final class HttpsConfig {
        /// Whether the main connector speaks TLS (`HDSL_HTTPS`, default false).
        public boolean enabled = false;

        /// PEM certificate chain, relative to `HDSL_DATA` or absolute.
        public String certFile = "";

        /// PEM private key, relative to `HDSL_DATA` or absolute.
        public String keyFile = "";

        /// PKCS#12 keystore, relative to `HDSL_DATA` or absolute. When both
        /// PEM and PKCS#12 are configured, the PKCS#12 file wins.
        public String pkcs12File = "";

        public String pkcs12Password = "";

        /// Whether to run a second, plain-HTTP connector that answers every
        /// request with 308 to the HTTPS URL of the same host. Only meaningful
        /// when HTTPS is enabled.
        public boolean redirectHttp = false;

        /// Whether the **main** connector itself also answers plain-HTTP
        /// requests with the same 308.
        ///
        /// On by default, because the alternative is what a bare `http://<ip>`
        /// entry runs into: the browser speaks cleartext to the TLS port and gets
        /// `ERR_INVALID_HTTP_RESPONSE` — no page, no hint. That is the common case
        /// for a LAN deployment with no domain and no reverse proxy, where the
        /// address people type is the IP and the browser assumes `http://`.
        /// (A port mapping that sends the host's port 80 to the TLS port hits the
        /// same wall, and a second connector cannot help there.)
        ///
        /// Panel content still never travels in cleartext: any request that did
        /// not arrive over TLS is answered with the redirect and nothing else —
        /// which is also why session cookies stay usable, being `Secure`.
        public boolean redirectSamePort = true;

        /// The port of that redirect connector (default 80).
        public int redirectPort = 80;

        /// Whether `X-Forwarded-Proto: https` from a reverse proxy counts as
        /// HTTPS (`https.trust_forwarded_proto`, `HDSL_TRUST_FORWARDED_PROTO`,
        /// default false). Turn this on only when the panel is reachable
        /// **solely** through a proxy that terminates TLS — a direct client can
        /// write the header itself, and then the browser leg may well be
        /// plaintext. With it on, the developer page and the Web Shell stay
        /// usable behind nginx/Caddy instead of reporting "not HTTPS".
        public boolean trustForwardedProto = false;

        /// Whether a certificate file was explicitly configured (PKCS#12 or PEM).
        public boolean hasExplicitCertificate() {
            return !pkcs12File.isBlank() || (!certFile.isBlank() && !keyFile.isBlank());
        }
    }

    public static final class AuthConfig {
        /// Whether authentication is turned off entirely. Only allowed when
        /// the server binds a loopback address (see docs/plan.md).
        public boolean disabled = false;

        /// Session time to live in hours; sessions slide forward on every
        /// verified request (default 168 = one week).
        public int sessionTtlHours = 168;
    }

    /// The instance terminal (Web Shell) switch (docs/web-shell.md). The
    /// developer page flips it; the terminal endpoints read it on every open,
    /// so a change applies without a restart.
    public static final class WebShellConfig {
        /// Whether instances may open a terminal at all (`HDSL_WEB_SHELL_ENABLED`,
        /// default true). Off means the open endpoint refuses with
        /// `web_shell_disabled`; a terminal that is already running is left
        /// alone, so the user can still stop it from its window.
        public volatile boolean enabled = true;
    }

    /// The developer guard (docs/web-shell.md §4.1): how far the developer
    /// elevation reaches beyond the web terminal. Off by default — the panel's
    /// own session is the only door unless an operator asks for more.
    ///
    /// The value is a *scope*, and each level includes the previous one:
    /// `proxy` gates the instance web pages (`/i/<id>/*`), `instance` also gates
    /// the state-changing instance APIs, `all` gates every `/api/*` request
    /// except the allowlist [DeveloperGuardScope] documents. The gates only
    /// apply to **new** requests: an already-open page, terminal or agent keeps
    /// running (that is what the emergency stop is for).
    public final DeveloperGuardConfig developerGuard = new DeveloperGuardConfig();

    public static final class DeveloperGuardConfig {
        /// The scope itself (`developer_guard.scope`, `HDSL_DEVELOPER_GUARD`),
        /// default [DeveloperGuardScope#OFF].
        public volatile DeveloperGuardScope scope = DeveloperGuardScope.OFF;
    }

    /// How far an agent grant reaches (docs/panel-agent.md).
    public final AgentConfig agent = new AgentConfig();

    public static final class AgentConfig {
        /// When on (the default), a ticket is only honoured from the panel's
        /// **loopback**: an agent runs beside the panel, so a ticket arriving
        /// from a LAN or public address is a credential that has travelled
        /// further than it should. Off restores the older "usable anywhere"
        /// posture and falls back to [#remoteNeedsSession] for the finer rule.
        ///
        /// A reverse proxy makes every request look like loopback, so this
        /// switch bounds direct exposure, not a proxied deployment. See
        /// `docs/panel-agent.md` §"来源限制".
        public volatile boolean loopbackOnly = true;

        /// When on, a ticket used from **outside the loopback** must be backed
        /// by a browser session — unless the ticket was minted from a browser
        /// in the first place (`/api/developer/agent-token`), which already
        /// proves a person opened the panel.
        ///
        /// Only consulted when [#loopbackOnly] is off; with the loopback rule
        /// on, remote tickets are refused whatever they carry.
        public volatile boolean remoteNeedsSession = false;
    }

    /// The Minecraft surface (docs/minecraft.md). The feature itself stays off
    /// until the owner enables it; these knobs only bound what may be created
    /// once it is on.
    public final McConfig mc = new McConfig();

    /// `transcode:` — runtime knobs for the transcode module. Whether the feature
    /// exists at all is **not** here: it is a runtime decision stored in
    /// `<data>/transcode/feature.json`.
    public final TranscodeConfig transcode = new TranscodeConfig();
    public static final class McConfig {
        /// First port the auto-allocator may hand out (`mc.port_start`).
        public volatile int portStart = 25565;
        /// Last port the auto-allocator may hand out (`mc.port_end`).
        public volatile int portEnd = 25665;
        /// How many servers this panel manages at once (`mc.max_instances`).
        public volatile int maxInstances = 8;
        /// The heap ceiling the create form accepts (`mc.max_memory_mb`).
        public volatile int maxMemoryMb = 8192;
        /// The java executable (`mc.java_path`); empty means `java` on PATH.
        public volatile String javaPath = "";
        /// Where servers (worlds + `server.jar`) live (`mc.dir`). Empty means
        /// `$HOME/mc` — in the container image that is `/home/hdsl/mc`, the path
        /// the image and compose file mount so a container rebuild keeps the
        /// worlds. `HDSL_MC_DIR` overrides both.
        public volatile String dir = "";
    }

    /// Transcode runtime settings (`transcode:` in `server.yaml`).
    ///
    /// The feature switch is **not** one of these fields: it lives in
    /// `<data>/transcode/feature.json` and is changed in the panel (with the
    /// developer password), so a copied `server.yaml` cannot silently turn the
    /// surface on. These are the limits that stay deployment-controlled.
    public static final class TranscodeConfig {
        /// Media root (`transcode.media_root`, `HDSL_MEDIA_ROOT`); empty means
        /// `<data>/video` for a bare deployment. The container image points it at
        /// `/home/hdsl/video`.
        public volatile String mediaRoot = "";

        /// ffmpeg executable (`transcode.ffmpeg_path`, `HDSL_FFMPEG_PATH`); empty
        /// means resolve from `PATH` and then from `<data>/transcode/tools/ffmpeg`.
        public volatile String ffmpegPath = "";

        /// How many jobs may run at once (`transcode.max_concurrent`, default 1).
        public volatile int maxConcurrent = 1;

        /// How many jobs may wait (`transcode.max_queue`, default 20).
        public volatile int maxQueue = 20;

        /// Failure policy wire name: `ask`, `timeout-auto` or `full-auto`.
        public volatile String failurePolicy = "ask";

        /// Minutes before an unanswered failure decision is handled.
        public volatile int failureTimeoutMinutes = 10;

        /// Whether pending jobs resume after a panel restart.
        public volatile boolean autoResume = true;
    }

    /// `logs:` — how long the panel keeps its own files. The window may only be set
    /// between [org.jackhuang.hmcl.web.logs.LogRetention#FLOOR_DAYS] (180 days) and
    /// [org.jackhuang.hmcl.web.logs.LogRetention#HARD_CAP_DAYS] (four years);
    /// anything outside that is refused at boot rather than clamped.
    ///
    /// 设置页只能往上调（见 `/api/settings/logs`），调小要编辑 `server.yaml`——
    /// “把保留窗口拖短让审计记录消失”正是那条规则要挡的事。
    public final LogsConfig logs = new LogsConfig();

    public static final class LogsConfig {
        /// 审计日志保留天数（`logs.audit_days`）。
        public volatile int auditDays = org.jackhuang.hmcl.web.logs.LogRetention.DEFAULT_AUDIT_DAYS;
        /// 面板日志保留天数（`logs.panel_days`）。
        public volatile int panelDays = org.jackhuang.hmcl.web.logs.LogRetention.DEFAULT_PANEL_DAYS;
    }

    /// `network:` — what the panel is allowed to assume about the machine's
    /// outbound TLS.
    ///
    /// 只有一个字段：一次"我确认这是我自己的代理"的授权何时失效。
    ///
    /// 它不是开关，是**有期限的确认**：一台装了 FastGithub / 抓包工具 / 企业网关的
    /// Windows 机器上，代理的根证书在 Windows 证书库里、不在 Java 的 cacerts 里，
    /// 于是浏览器打得开、面板报 `PKIX path building failed`。面板不自己跳过校验，
    /// 它把原因说清楚，等人确认；确认记在这里，重启后仍在期内生效，过期自动收紧。
    public final NetworkConfig network = new NetworkConfig();

    public static final class NetworkConfig {
        /// 授权到什么时候为止（`network.accept_unverified_until`，ISO-8601）。
        /// `null` 表示严格校验——这是默认，也是没有记录时的意思。
        public volatile @Nullable Instant acceptUnverifiedUntil;
    }

    /// The legacy no-JavaScript surface (`/plain`): a full citizen (fragment
    /// long-polling, server-rendered markdown) or a plain degraded fallback of
    /// forms and whole-page refreshes. Operators with no legacy clients at all
    /// can turn the enhancement off and keep only the fallback.
    public final PlainConfig plain = new PlainConfig();

    public static final class PlainConfig {
        public volatile boolean enhanced = true;

        /// Concurrent Node render processes. Each render spawns a short-lived
        /// Node process; 2 was unnecessarily strict on an 8-core NAS, while the
        /// old unbounded pool could spawn dozens. 4 is the default; operators can
        /// tune it between 1 and 16 in `server.yaml` (`plain.ssr_workers`).
        public volatile int ssrWorkers = 4;
    }
}
