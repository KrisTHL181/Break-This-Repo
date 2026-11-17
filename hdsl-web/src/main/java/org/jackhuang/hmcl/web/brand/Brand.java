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
package org.jackhuang.hmcl.web.brand;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/// The third-party tool categories the panel can install and serve, beyond the
/// dsh and ZCode ones.
///
/// Each value is a static description of one upstream tool — what the panel
/// installs (an npm package or a git repository), what it launches, and how
/// that process announces its port. Behavioural differences (launch flags,
/// port strategy, readiness line) live in [BrandRuntime] and switch on this
/// enum; everything here is data.
///
/// These tools are **third-party**: the panel only installs, launches and
/// reverse-proxies them. Their functionality, security and billing belong to
/// their respective vendors.
@NotNullByDefault
public enum Brand {
    /// Kimi Code (Moonshot AI): `kimi web` serves its own web UI on a
    /// loopback port; `--port 0` yields an ephemeral port announced on stdout.
    /// Its client is mount-aware, so instances are served under `/i/<id>/`
    /// like dsh (verified end to end with a headless browser).
    KIMI("kimi", Source.NPM, "@moonshot-ai/kimi-code", "kimi", false),

    /// OpenCode: `opencode web` serves its own web UI; `--port 0` is NOT
    /// honoured (it falls back to 4096), so the panel pre-allocates a port.
    /// Its client **routes by `location.pathname`** (verified in its bundle
    /// and by serving it under any subpath: the shell renders, the content
    /// stays blank). Mounted like everything else, such a client needs the
    /// router shim that reads the mount away from the paths it routes by and
    /// puts it back on the addresses it writes — see [needsRouterShim].
    OPENCODE("opencode", Source.NPM, "opencode-ai", "opencode", true),

    /// Claude Web (ypyik0669/claude-web, MIT): a community workbench around
    /// the Claude Code CLI — and, through its bundled engine and provider
    /// system, around relay APIs. **Not** Anthropic's hosted "Claude Code on
    /// the web", which is a SaaS and cannot be self-hosted.
    ///
    /// Distributed as git tags, not an npm package. The panel reads them over
    /// the **GitHub REST API** and installs by downloading the tag's source
    /// zip — deliberately not the git protocol: on this deployment's network
    /// `github.com:443`'s git transport is cut mid-handshake (measured:
    /// `expected flush after ref listing`, GnuTLS resets, 130 s connect
    /// timeouts), while `api.github.com` and `codeload.github.com` answer in
    /// well under two seconds. Over HTTPS the whole source is a plain,
    /// retryable download — and the panel needs no `git` at all.
    ///
    /// The server is plain Node (`tsx server/src/index.ts`), honours a `PORT`
    /// environment variable, defaults to `127.0.0.1`, and prints `listening on
    /// http://127.0.0.1:<port>` — all verified in its source. Its web client
    /// builds with relative asset URLs (`base: './'`, chosen upstream so a
    /// phone shell can run the same build below a folder), talks to the server
    /// over a single WebSocket at `/ws`, and routes nothing by pathname, so a
    /// subpath mount under `/i/<id>/` works with the standard brand shim — no
    /// router shim.
    CLAUDE_WEB("claude-web", Source.GITHUB, "https://github.com/ypyik0669/claude-web", "v", false),

    /// Octop (TencentCloud/Octop, MIT): a self-hosted multi-user AI assistant
    /// whose dashboard is a Python/FastAPI server plus a React SPA.
    ///
    /// Its releases are **portable archives**, not a source checkout: each tag
    /// publishes `Octop-portable-<plat>-<version>.zip`, which already contains
    /// CPython 3.12, every Python dependency and the dashboard. The panel
    /// installs that archive and replaces only the dashboard directory with the
    /// patched build (`octop-dashboard-<version>.zip`), because Octop's own
    /// dashboard is built for `/` while an instance lives under `/i/<token>/`.
    ///
    /// The instance id is an opaque token (the same construction every brand
    /// uses), so several Octop servers can coexist without sharing storage
    /// keys; the patched dashboard namespaces `auth_token` by its mount.
    OCTOP("octop", Source.GITHUB_RELEASE, "https://github.com/TencentCloud/Octop", "v", false),

    /// Hermes Agent (Nous Research, MIT): a Python agent with its own web
    /// dashboard and an OpenAI-compatible gateway.
    ///
    /// Installed from the tag's source zip. Its packaging is deliberately not
    /// pip: `setup.py` raises for wheel/sdist builds outside a Nix build
    /// (editable installs are exempt, which is what `uv sync` does). Python
    /// 3.14 and the two front-end products come from the version's own
    /// `pm/lock.json` and the panel's Node; `hermes_cli/web_dist/index.html` is
    /// the marker.
    ///
    /// Two processes, one wrapper: `hermes gateway run` (the OpenAI-compatible
    /// API and the agent runtime) and `hermes dashboard` (the SPA the browser
    /// talks to). The dashboard is prefix-aware — it reads
    /// `X-Forwarded-Prefix` — so the proxy hands it the mount and stops
    /// rewriting its pages; see [#serverSidePrefix].
    HERMES("hermes", Source.GITHUB, "https://github.com/NousResearch/hermes-agent", "v", false),

    /// PI WEB (the Chinese-localization fork of pi-web, MIT): a browser
    /// control plane for the Pi Coding Agent, shipped as an npm package.
    ///
    /// **A release is a pair, not a package.** The web app reuses the host Pi
    /// SDK (`@earendil-works/pi-coding-agent`), and the Pi TUI in the same
    /// release is that same SDK's `pi` binary. The installer therefore adds
    /// both at once, and [#pairedSdkVersion] is the one table that says which
    /// Pi a given pi-web build may run on. Only pairs listed there are offered
    /// — see [#isVerifiedPair] — because a pi-web release outside its peer
    /// range loses the version-specific adapters it was built against.
    ///
    /// **Two processes, one wrapper**: `pi-web-sessiond` (sessions and tools)
    /// and `pi-web-server` (the web/API gateway). The web process connects to
    /// the daemon over a unix socket and deliberately does not start it; the
    /// generated wrapper starts the daemon first, waits for the socket, then
    /// execs the web process.
    ///
    /// **The client is deployment-relative**: Vite is built with `base: "./"`,
    /// every request is resolved once against `document.baseURI`, and routes
    /// live in the query string. A subpath mount therefore needs no HTML, shim
    /// or router patch — see [#relativeClient].
    PI_WEB("pi-web", Source.NPM, "@chainingintention/pi-web-cn", "pi-web", false),

    /// Qwen Code (Alibaba, npm package `@qwen-code/qwen-code`): a terminal
    /// coding agent with a full-screen TUI. It also has a daemon mode
    /// (`qwen serve`) that serves a Web Shell, but the panel treats it as a
    /// **TUI brand**: the instance runs `qwen` in a PTY and has no web process,
    /// no port and no `/i/<id>/` mount.
    QWEN("qwen", Source.NPM, "@qwen-code/qwen-code", "qwen", false),

    /// MiMo Code (Xiaomi, npm package `@mimo-ai/cli`): an OpenCode-derived
    /// terminal agent. The published package is a small launcher that
    /// downloads the platform binary on install/first run. Its `web` command
    /// and embedded web UI are disabled upstream, so this is a TUI-only brand.
    MIMO("mimo", Source.NPM, "@mimo-ai/cli", "mimo", false),

    /// MiniMax Code (npm package `@minimax-ai/code`): a standalone TUI with
    /// managed accounts, BYOK models, plugins and ACP. The public source has
    /// no HTTP/web UI, so this is a TUI-only brand.
    MINIMAX("minimax", Source.NPM, "@minimax-ai/code", "mcode", false);

    /// Where a brand's releases come from: an npm registry package, a GitHub
    /// repository's release tags (source zip over HTTPS, no git), or a GitHub
    /// release's prebuilt portable archive.
    public enum Source {
        NPM,
        GITHUB,
        GITHUB_RELEASE
    }

    /// The shape of a brand's instance, which decides what the panel offers:
    /// a web process behind `/i/<id>/`, a terminal only, or both.
    public enum Kind {
        /// The instance runs the brand's web server; the terminal is optional.
        WEB,
        /// The instance is a PTY running the brand's TUI. There is no web
        /// process, no port and no mount.
        TUI,
        /// The instance can be created either way (PI WEB today).
        HYBRID
    }

    private final String id;
    private final Source source;
    private final String locator;
    private final String aux;
    private final boolean needsRouterShim;

    /// The Pi SDK package a PI WEB release runs on.
    public static final String PI_SDK_PACKAGE = "@earendil-works/pi-coding-agent";

    /// The first Hermes release that pins Node 26.
    ///
    /// 0.22.5's `engines.node` was `^22.22.0 || ^24.11.0 || >=26.0.0`; 0.22.6
    /// tightened it to `>=26`, and the repository's `.npmrc` sets
    /// `engine-strict=true`, so `npm ci` **fails** on Node 22/24 instead of
    /// warning. The panel can say so before a four-minute source download.
    public static final String HERMES_NODE_26_FROM = "0.22.6";

    /// The verified pi-web ↔ Pi SDK pairs.
    ///
    /// A pair is a contract, not a preference: PI WEB's peer range covers
    /// several Pi majors, and code paths keyed on the SDK's own `VERSION`
    /// (management MCP authentication, for one) only exist for some of them.
    /// The panel therefore offers **only** the pairs listed here — one at a
    /// time in the picker — while older releases stay on disk for the instances
    /// that pinned them. Adding a new pairing is this map plus an install
    /// audit, not a UI change.
    private static final Map<String, String> PI_WEB_PAIRS = Map.of(
            "2.202611.8", "1.4.0");

    /// @param locator the npm package (NPM) or the repository URL (GITHUB)
    /// @param aux the executable inside `node_modules/.bin/` (NPM) or the
    ///            tag prefix (GITHUB, e.g. `"v"` for `v0.2.1`)
    Brand(String id, Source source, String locator, String aux, boolean needsRouterShim) {
        this.id = id;
        this.source = source;
        this.locator = locator;
        this.aux = aux;
        this.needsRouterShim = needsRouterShim;
    }

    /// The stable category id used in API paths and directory names.
    public String id() {
        return id;
    }

    /// Where this brand's releases come from.
    public Source source() {
        return source;
    }

    /// The shape of this brand's instances.
    ///
    /// TUI brands are the panel's second brand family: the instance is a PTY,
    /// there is no web process to launch, and no `/i/<id>/` mount exists. The
    /// API and the clients switch on this instead of hard-coding brand ids.
    public Kind kind() {
        return switch (this) {
            case QWEN, MIMO, MINIMAX -> Kind.TUI;
            case PI_WEB -> Kind.HYBRID;
            default -> Kind.WEB;
        };
    }

    /// Whether this brand's instances are terminal-only.
    public boolean isTui() {
        return kind() == Kind.TUI;
    }

    /// Whether an instance of this brand exposes a web process at all.
    public boolean hasWeb() {
        return kind() != Kind.TUI;
    }

    /// Whether this brand exposes an interactive terminal.
    public boolean hasTerminal() {
        return true;
    }

    /// Whether the installer should use a hoisted `node_modules` layout on
    /// every platform.
    ///
    /// The panel already hoists on Windows. MiniMax Code ships a large
    /// pre-bundled CLI with native optional dependencies and is the one brand
    /// that resolves some of them from the hoisted tree; the isolated layout
    /// works for the other npm brands and is left alone.
    public boolean prefersHoistedNodeModules() {
        return this == MINIMAX;
    }

    /// Whether the panel prefers the managed Node.js runtime for this brand's
    /// install and TUI launch.
    ///
    /// TUI brands are the ones whose upstream engines range is narrow and
    /// whose install is a plain `pnpm add`; running them on whatever `node`
    /// happens to be on the host PATH is exactly the failure mode the managed
    /// runtime exists to avoid. The launcher still falls back to PATH when no
    /// managed runtime is installed.
    public boolean prefersManagedNode() {
        return isTui();
    }

    /// Whether releases come from a GitHub repository (source zip or a
    /// prebuilt release asset), as opposed to an npm registry package.
    public boolean fromGitHub() {
        return source == Source.GITHUB || source == Source.GITHUB_RELEASE;
    }

    /// The upstream npm package the installer fetches; `null` for GitHub brands.
    public @Nullable String npmPackage() {
        return source == Source.NPM ? locator : null;
    }

    /// The executable inside the installed prefix's `node_modules/.bin/`;
    /// `null` for GitHub brands, whose launch is not a single binary.
    public @Nullable String binName() {
        return source == Source.NPM ? aux : null;
    }

    /// The upstream repository URL the installer fetches sources from; `null`
    /// for npm brands.
    public @Nullable String repoUrl() {
        return fromGitHub() ? locator : null;
    }

    /// The `owner/repo` slug of the repository — the form the GitHub REST API
    /// (`/repos/<slug>/tags`) and codeload (`/<slug>/zip/…`) address it by.
    /// Empty for npm brands.
    public String githubSlug() {
        if (!fromGitHub()) {
            return "";
        }
        String slug = locator;
        if (slug.startsWith("https://github.com/")) {
            slug = slug.substring("https://github.com/".length());
        }
        while (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.endsWith(".git")) {
            slug = slug.substring(0, slug.length() - ".git".length());
        }
        return slug;
    }

    /// The tag prefix a version is released under (`v` for `v0.2.1`). Empty
    /// for npm brands.
    public String tagPrefix() {
        return fromGitHub() ? aux : "";
    }

    /// The tag naming one version: the prefix plus the version (`v` + `0.2.1`
    /// → `v0.2.1`). The version alone for npm brands.
    public String releaseTag(String version) {
        return fromGitHub() ? aux + version : version;
    }

    /// The release-relative path whose existence proves an install: the
    /// package's executable (NPM) or the built web app (GITHUB). Octop's
    /// portable archive carries the SPA under `packages/octop/dashboard/`.
    public String installMarker() {
        if (this == HERMES) {
            // Published atomically by the upstream web build; its presence is the
            // strongest marker this installer can have (the venv and the TUI are
            // built before it).
            return "hermes_cli/web_dist/index.html";
        }
        if (this == OCTOP) {
            // Written by both installers (portable archive and local source),
            // so one marker proves either layout is complete.
            return "octop-install.json";
        }
        return source == Source.NPM ? "node_modules/.bin/" + aux : "web/dist/index.html";
    }

    /// Whether a tag/version is a release this panel should offer. Octop
    /// publishes beta tags such as `1.0.3b1`; the other brands are strict
    /// `x.y.z` to keep their CI noise out of the picker.
    public boolean isReleaseVersion(String version) {
        String pattern = switch (this) {
            case OCTOP -> "^\\d+\\.\\d+\\.\\d+(?:[A-Za-z][0-9A-Za-z.-]*)?$";
            // Hermes also publishes date tags (`v2026.9.24`) that satisfy a plain
            // three-part rule; a two-digit major keeps those and `+canary.*` out.
            case HERMES -> "^\\d{1,2}\\.\\d+\\.\\d+$";
            default -> "^\\d+\\.\\d+\\.\\d+$";
        };
        return version.matches(pattern);
    }

    /// What the panel calls the upstream in user-facing messages: the npm
    /// package or the repository URL.
    public String sourceLabel() {
        return locator;
    }

    /// Whether the tool's web client routes by URL path, making a subpath
    /// mount (like `/i/<id>/`) render a broken shell unless the proxy hands
    /// the origin root back to it. Such pages are served through the mount
    /// with an extra script that keeps `location.pathname` at `/` while the
    /// runtime shim keeps every request inside the mount.
    public boolean needsRouterShim() {
        return needsRouterShim;
    }

    /// Whether the brand's own server is prefix-aware: it reads
    /// `X-Forwarded-Prefix` and emits every reference under it, so the proxy
    /// must hand it the mount instead of rewriting its pages.
    ///
    /// A proxy that rewrote such a page anyway would double the mount
    /// (`/i/<id>/i/<id>/assets/…`) and inject a shim the SPA does not want.
    public boolean serverSidePrefix() {
        return this == HERMES;
    }

    /// Whether the brand's web client resolves every request against the
    /// document base instead of owning the origin root.
    ///
    /// PI WEB is built that way on purpose (`base: "./"`, routes in the query
    /// string), so a mount under `/i/<id>/` needs no page rewrite, no runtime
    /// shim and no router patch — the references it builds are already inside
    /// the mount. The proxy uses this to leave its HTML and scripts untouched.
    public boolean relativeClient() {
        return this == PI_WEB;
    }

    /// Whether the brand's terminal and its web mode can run at the same time.
    ///
    /// For PI WEB they share one `HOME`/Pi agent directory but run separate
    /// sessions, and the TUI does not start or replace the daemon — the same
    /// reason Hermes cannot do it (its TUI starts a detached gateway). Every
    /// other brand keeps the panel's "the terminal takes the instance over"
    /// rule.
    public boolean terminalCoexistsWithWeb() {
        return this == PI_WEB;
    }

    /// Whether the panel offers a **terminal-only** form for this brand: an
    /// instance that never starts the web process, for users who want the TUI
    /// and nothing else (or whose web install was degraded).
    public boolean supportsTerminalOnly() {
        return this == PI_WEB;
    }

    /// The Pi SDK version a PI WEB release is verified against.
    ///
    /// @param version the pi-web version
    /// @return the SDK version, or `null` for other brands and unverified pairs
    public @Nullable String pairedSdkVersion(String version) {
        return this == PI_WEB && version != null ? PI_WEB_PAIRS.get(version) : null;
    }

    /// The Node requirement a release declares, known without installing it.
    ///
    /// Only Hermes has a threshold the panel can know up front: from
    /// [#HERMES_NODE_26_FROM] on, every release requires Node 26. Older Hermes
    /// releases still accept Node 22/24, so they answer `null` (no hint), not
    /// "any version".
    ///
    /// @param version the release version
    /// @return the range, or `null` when there is nothing to warn about
    public @Nullable String knownNodeRequirement(String version) {
        return switch (this) {
            // Declared by the published packages. The installer/runtime picks a
            // Node that satisfies these; the version picker can warn before an
            // install even when the package is not on disk yet.
            case QWEN -> ">=22.0.0";
            case MIMO -> ">=20.10.0";
            case MINIMAX -> ">=22.19.0 <23 || >=24.2.0 <27";
            case HERMES -> version == null || version.isBlank()
                    ? null
                    : (org.jackhuang.hmcl.dsh.NodeSemver.satisfies(version, ">=" + HERMES_NODE_26_FROM)
                            ? ">=26" : null);
            default -> null;
        };
    }

    /// Whether this brand offers a version in its picker.
    ///
    /// Only PI WEB restricts the list: a release outside the verified pairs has
    /// no Pi to install with it, so offering it would promise an install the
    /// panel cannot complete. Older releases already installed stay usable.
    ///
    /// @param version the upstream version
    /// @return whether the picker may show it
    public boolean offersVersion(String version) {
        return this != PI_WEB || PI_WEB_PAIRS.containsKey(version);
    }

    /// Whether this brand offers the **last-resort** policy: one surviving
    /// instance and the fixed `/i/<brand>/` route to it.
    ///
    /// Only the two tools whose own agent can wedge hard enough to take the
    /// whole brand down need that fallback — Octop's dashboard and OpenCode's
    /// web client. Every other brand keeps plain multi-instance behaviour, and
    /// the policy API refuses the switches for them.
    public boolean supportsLastResort() {
        return this == OCTOP || this == OPENCODE;
    }

    /// Whether the installer builds a Python environment from source (uv) rather
    /// than unpacking an npm package or a portable archive.
    public boolean pythonSource() {
        return this == HERMES;
    }

    /// Whether the brand runs as two supervised processes behind one wrapper
    /// script — the gateway and its dashboard.
    public boolean companionProcess() {
        return this == HERMES;
    }

    /// Whether this panel can install this brand on `os`.
    ///
    /// Hermes is Linux/macOS only, and the panel says so instead of failing later:
    /// its installer builds a Python environment from source and its launcher is a
    /// POSIX shell script that supervises two processes — neither has an equivalent
    /// on Windows (`\\Scripts` instead of `bin/`, no `sh`). A Windows user who wants
    /// Hermes can run the **panel** inside WSL2, where it is just Linux.
    ///
    /// @param os the platform the panel is running on
    /// @return whether an install is offered here
    public boolean installableOn(OperatingSystem os) {
        if (this == HERMES) {
            return os != OperatingSystem.WINDOWS;
        }
        // opencode's npm package is only a launcher: the real CLI arrives as a
        // per-platform optionalDependency and upstream publishes no FreeBSD build,
        // so its postinstall fails instead of falling back to a JS implementation.
        if (this == OPENCODE) {
            return os != OperatingSystem.FREEBSD;
        }
        return true;
    }

    /// Why [#installableOn] says no, or `null` when it says yes.
    ///
    /// One sentence, user-facing: the brand page prints it verbatim under a
    /// disabled install button, so it has to be worth reading on its own.
    ///
    /// @param os the platform the panel is running on
    /// @return the reason, or `null`
    public @Nullable String notInstallableReason(OperatingSystem os) {
        if (installableOn(os)) {
            return null;
        }
        if (this == OPENCODE && os == OperatingSystem.FREEBSD) {
            return "opencode 没有 FreeBSD 构建：上游的可选依赖只有 Linux / macOS / Windows 的"
                    + "二进制包，安装会在 postinstall 阶段直接失败。";
        }
        return "Hermes 只能在 Linux / macOS 上安装：它要从源码建一套 Python 环境，"
                + "并且用一个 shell 脚本同时看住 gateway 与 dashboard 两个进程，"
                + "Windows 上没有这两样东西。想在 Windows 上用：把**面板本身**装进 WSL2，"
                + "对面板来说那就是 Linux，安装方式与文档里完全一样。";
    }

    /// Seconds the panel waits for a graceful stop.
    ///
    /// Hermes' gateway drains for up to 30 s by default (an active runtime is
    /// closed rather than killed), and the wrapper needs room to take both
    /// children down after that.
    public int stopWaitSeconds() {
        return this == HERMES ? 40 : 10;
    }

    /// Whether instances of this brand can carry **plugins** that the panel
    /// installs, enables and lists per instance — the capability behind
    /// `GET /api/brands/<brand>/instances/<id>/plugins`.
    ///
    /// No brand has a plugin story today, so every brand answers `false` and
    /// the panel's plugin dialog shows the reserved-slot note instead of a
    /// list. The endpoint and the dialog are data-driven on purpose: the day a
    /// brand grows plugins, this method (and whatever installs them) is the
    /// only thing that changes — the UI already renders what the endpoint
    /// returns.
    public boolean supportsPlugins() {
        return false;
    }

    /// Resolves a category id back to its brand.
    ///
    /// @param id the id from a URL path or manifest
    /// @return the brand, or `null` when the id names none
    public static @Nullable Brand fromId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (Brand brand : values()) {
            if (brand.id.equals(id)) {
                return brand;
            }
        }
        return null;
    }
}
