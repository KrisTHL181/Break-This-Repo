/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The community's terminal interfaces for DeepSeek Harness, as a curated list.
///
/// [DshSurface] has no TUI member, and there is no `dsh-tui` subcommand either: a
/// terminal interface comes from a plugin, installed into a profile and started by
/// booting that profile (`dsh --profile <name>`; upstream also accepts the profile
/// name as a bare word, `dsh <name>`). What the list pins, therefore, is not only
/// *which* package — it is **which profile**, because that is the part that decides
/// whether the instance starts at all:
///
/// **A terminal plugin never goes into the web profile.** Both are *app* plugins: each
/// parses the whole argv with its own grammar, and upstream does not arbitrate between
/// two of them. Installed together they collide on the first flag the other one owns —
/// the community's own report is `error: unknown option '--no-open'` — so the instance
/// does not merely lose its terminal, it stops starting. That is why every entry names
/// the profile its own documentation installs into, why [refuseAppProfile] refuses the
/// profiles the harness ships apps for, and why a terminal is always started on the
/// plugin's own profile rather than the instance's.
///
/// Three things about an entry are decisions rather than descriptions:
///
/// - the **pinned version**. The panel installs `name@version` and not `name`, so a
///   profile keeps working when the author publishes something new;
/// - the **profile**: what the plugin's own README says to use (`tui`, `pi-tui`,
///   `dsh-tui`), never the instance's `web`;
/// - the **bin name**, when the package ships one. Everything else is a pure bundle and
///   is started by booting its profile, which is what its own documentation shows.
///
/// The list is a constant rather than a fetch: it is short, it changes when an author
/// publishes, and a page that had to reach the network to say what a terminal plugin is
/// would be unable to explain itself offline.
@NotNullByDefault
public final class DshTuiPlugins {

    /// The profile the panel's own instances boot, and the one a terminal plugin must
    /// not be installed into.
    public static final String WEB_PROFILE = "web";

    /// One terminal interface offered for installation.
    ///
    /// @param id          a stable identifier, used by the interface and by tests
    /// @param packageName the npm package name
    /// @param version     the version the panel pins
    /// @param displayName what a list row shows
    /// @param repository  the source repository, or `null` when the package names none
    /// @param license     the package's licence, as its manifest declares it
    /// @param description one sentence on what it is
    /// @param bin         the executable the package ships, or `null` for a pure bundle
    /// @param profile     the profile it is installed into and started from
    /// @param requiresDsh the harness line this plugin needs, or `null` when it starts on
    ///                    any published line — see [fitsHost]
    /// @param bundled     whether the panel ships this plugin inside its own jar (see
    ///                    [FirstPartyPlugins]) instead of fetching it from npm
    /// @param runtimeGroup a name shared by entries that are alternatives **at run time**:
    ///                    several may be installed on purpose — the ACP terminal is the
    ///                    fallback for an instance whose version moves — but only the first
    ///                    fitting one is started (see `DshTui#terminalPlugin`)
    /// @param terminal    whether this is a terminal interface. A **capability** plugin
    ///                    (`false`) shares the instance's own `web` profile, must never be
    ///                    picked as "the terminal to start", and is what the panel calls a
    ///                    first-party capability: things the instance gets, not a screen.
    public record Entry(
            String id,
            String packageName,
            String version,
            String displayName,
            @Nullable String repository,
            String license,
            String description,
            @Nullable String bin,
            String profile,
            @Nullable String requiresDsh,
            boolean bundled,
            @Nullable String runtimeGroup,
            boolean terminal) {

        /// Returns the specification the installer is given.
        ///
        /// The version is pinned, so an installation is reproducible and a later
        /// publication by the author cannot change what an instance runs.
        ///
        /// @return the `<package>@<version>` specification
        public String spec() {
            return packageName + "@" + version;
        }

        /// Reports whether this entry ships its own entry point.
        ///
        /// @return whether the package has a bin
        public boolean hasExecutable() {
            return bin != null && !bin.isBlank();
        }
    }

    /// The entries, in the order the interface lists them: the one developed against the
    /// harness's own releases first, then the rest by package name.
    ///
    /// Every field here was read from the package's own `latest` document on npm
    /// (version, licence, `bin`, `repository`, `engines`/`peerDependencies`) and from its
    /// README (the profile and the harness line), not from a description of it.
    public static final @Unmodifiable List<Entry> ENTRIES = List.of(
            new Entry(
                    "dsh-inproc-tui",
                    "@nas200-timmy/dsh-inproc-tui",
                    "0.2.3",
                    "DSH TUI（进程内）",
                    "https://github.com/nas200-timmy/HDSL-WEB",
                    "GPL-3.0-or-later",
                    "面板自带的进程内终端（dsh 0.2.x）：真·逐字流式、思考增量、dsh 自己的工具卡片、"
                            + "审批与追问、模型/推理强度切换、会话恢复与压缩。纯 bundle。与 ACP 版可以"
                            + "同时装——它跑不了时由 ACP 版接管。",
                    null,
                    "dsh-inproc-tui",
                    // A bundle over dsh-base: it uses the 0.2.x agent/stream/approval seams
                    // directly, so 0.2.0-rc.2 is the floor and 0.3.0 is where those seams
                    // have to be re-verified. Everything between is admitted on purpose:
                    // the terminal was re-run on 0.2.2-rc.3, and an enumerated list of
                    // exact prereleases only left the newest harness without a terminal
                    // every time upstream cut a new rc (0.2.1-rc.4 / -rc.5 and 0.2.2-rc.2 /
                    // -rc.3 all bounced off a gate that named the previous build).
                    //
                    // The plugin's own peers say the same (`^0.2.0-rc.1`, whose npm
                    // expansion ends at `<0.3.0-0`), which matters: dsh judges an
                    // installation by those peers with includePrerelease, so the range
                    // and this gate agree instead of drifting apart. The upper bound is
                    // `<0.3.0-0`, not `<0.3.0`: with includePrerelease the latter still
                    // admits 0.3.0-rc.1.
                    ">=0.2.0-rc.2 <0.3.0-0",
                    true,
                    "dsh-terminal",
                    true),
            new Entry(
                    "dsh-acp-tui",
                    "@nas200-timmy/dsh-acp-tui",
                    "0.2.1",
                    "DSH ACP TUI（兼容备用）",
                    "https://github.com/nas200-timmy/HDSL-WEB",
                    "GPL-3.0-or-later",
                    "面板自带的 ACP 终端：kimi-code 风格的记录区、输入框、状态栏、工具卡片与"
                            + "模型/会话选择器，零外部依赖，通过 ACP 与 dsh 通信。**绝对的兼容选项**——"
                            + "实例升级/降级后都能当退路，装完即离线可用。",
                    "dsh-acp-tui",
                    "dsh-acp-tui",
                    // It speaks ACP over stdio and imports no harness package, so it runs on
                    // every line whose CLI ships the acp profile template (0.1.2-rc.1 onwards).
                    ">=0.1.2-rc.1",
                    true,
                    "dsh-terminal",
                    true),
            new Entry(
                    "deepseek-harness-tui",
                    "@deepseek-harness-tui/dsh-tui",
                    "0.17.0",
                    "dsh-TUI",
                    "https://github.com/ccch1mneyyy/dsh-TUI",
                    "MIT",
                    "社区主力终端界面：像素鲸鱼头部、流式思考、工具卡片、会话恢复与中英界面。"
                            + "自述 zero core changes —— 只消费 dsh 的公开导出，卸载不留补丁。",
                    "dsh-tui",
                    "dsh-tui",
                    // Its peers span both lines (`^0.1.0-rc.6 … 0.2.1-rc.3`), so it is the one
                    // entry that starts on a 0.1.x harness; that is also why it carries no
                    // preset layer of its own.
                    null,
                    false,
                    null,
                    true),
            new Entry(
                    "tianshu-tui",
                    "@huiliyi37/dsh-tianshu-tui",
                    "1.0.0-rc.2",
                    "dsh-tianshu-tui（天枢）",
                    "https://github.com/huiliyi37/dsh-tianshu-tui",
                    "Apache-2.0",
                    "天枢终端面板：自研极简 ANSI 渲染引擎、16+ 主题、slash 命令与选择器、"
                            + "LSP 诊断与图像/视觉桥接。",
                    null,
                    "tui",
                    // README: 需 ≥ 0.2.0-rc.1（npm next）；peers 全是 ^0.2.0-rc.1。
                    ">=0.2.0-rc.1",
                    false,
                    null,
                    true),
            new Entry(
                    "tomowang-tui",
                    "@tomowang/dsh-tui",
                    "0.10.2",
                    "@tomowang/dsh-tui",
                    "https://github.com/tomowang/dsh-tui",
                    "MIT",
                    "树外 mode bundle：与内置的 dsh-web-app / dsh-headless 同构地叠在 dsh-base 上，"
                            + "把 agent 从浏览器搬到终端。",
                    null,
                    "tui",
                    // peers 全是 ^0.2.1-rc.3。
                    ">=0.2.1-rc.3",
                    false,
                    null,
                    true),
            new Entry(
                    "pi-tui",
                    "@xmoon76/dsh-pi-tui",
                    "0.5.3",
                    "dsh-pi-tui",
                    "https://github.com/XMoon/dsh-pi-tui",
                    "MIT",
                    "基于 Pi TUI 的终端前端，纯 bundle：流式对话、工具调用、会话管理、Subagent 与审批。",
                    null,
                    "pi-tui",
                    // 唯一把要求直接写出来的一个：`engines.dsh: >=0.2.1-rc.2`。
                    ">=0.2.1-rc.2",
                    false,
                    null,
                    true));

    private DshTuiPlugins() {
    }

    /// The harness profile a terminal plugin boots, when that is not its own profile.
    ///
    /// A plugin that ships an executable is started directly and boots the harness itself, so which
    /// profile runs is **its** choice, not the catalogue's: the panel only knows it has to match.
    /// The ACP terminal speaks to the harness's own `acp` template — the one that mounts the ACP
    /// server — and nothing else; its own profile (`dsh-acp-tui`) is where its files live.
    ///
    /// The distinction matters beyond a label: the launcher writes the account's supplier route and
    /// default model into the profile it is told the launch boots. Written into one profile while
    /// the harness boots the other, the route is invisible — the account simply does not work.
    ///
    /// A pure bundle boots its own profile (`dsh --profile <its profile>`), so it has no entry here.
    ///
    /// @param entry the catalogue entry
    /// @return the profile the plugin's own program boots, or `null` when it is the entry's profile
    public static @Nullable String harnessProfile(Entry entry) {
        return HARNESS_PROFILES.get(entry.packageName());
    }

    /// The profiles a plugin's own program boots, by package name — see [harnessProfile].
    private static final Map<String, String> HARNESS_PROFILES = Map.of(
            "@nas200-timmy/dsh-acp-tui", "acp");

    /// Looks an entry up by package name.
    ///
    /// @param packageName the npm package name, or `null`
    /// @return the entry, or empty when the name is not one of these
    public static Optional<Entry> find(@Nullable String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return Optional.empty();
        }
        String wanted = packageName.trim();
        for (Entry entry : ENTRIES) {
            if (entry.packageName().equalsIgnoreCase(wanted)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /// Reports whether a profile's declared dependencies hold this entry.
    ///
    /// Read from the profile's own manifest, which is why it is asked per entry: the
    /// profiles differ (three of these four use three different ones), and a plugin
    /// installed into its own profile is invisible in the instance's web profile.
    ///
    /// @param entry        the entry
    /// @param dependencies the profile's dependency names
    /// @return whether it is installed there
    public static boolean isInstalled(Entry entry, Iterable<String> dependencies) {
        for (String dependency : dependencies) {
            if (dependency != null && entry.packageName().equalsIgnoreCase(dependency.trim())) {
                return true;
            }
        }
        return false;
    }

    /// Whether a plugin can start on the harness an instance runs.
    ///
    /// A terminal interface's **tool surface comes from the agent-preset layer**, which the
    /// harness only grew in 0.2.0 (`dsh-agent-preset-registry` plus one `dsh-agent-preset`
    /// row per preset). The plugins written against it carry their own preset rows in their
    /// bundle patch, and on a 0.1.x harness none of those rows can mount: the terminal
    /// starts, the session comes up with no tools, and the plugin says so in its own words —
    /// `agent-presets 未装配：本会话没有工具面`. Installing such a plugin into an older
    /// instance is therefore not a failure to detect late, it is a combination to refuse:
    /// what the user would get is a working window with a useless agent.
    ///
    /// Two answers are deliberately permissive. `requiresDsh() == null` means the entry
    /// starts on any published line (its peers span both). An unknown harness version is not
    /// refused either: the version comes from the instance record, and guessing would block
    /// an instance that is probably fine.
    ///
    /// @param entry           the catalogue entry
    /// @param harnessVersion  the dsh version the instance runs, or `null` when unknown
    /// @return whether the plugin is meant for that line
    public static boolean fitsHost(Entry entry, @Nullable String harnessVersion) {
        String range = entry.requiresDsh();
        if (range == null || range.isBlank()) {
            return true;
        }
        if (harnessVersion == null || harnessVersion.isBlank()) {
            return true;
        }
        return DshPluginRequirements.satisfies(range, harnessVersion);
    }

    /// The sentence the install route and the terminal route both say for a plugin whose
    /// harness line does not match — one wording, so the two cannot drift apart.
    ///
    /// @param entry          the entry that does not fit
    /// @param harnessVersion the dsh version the instance runs, or `null` when unknown
    /// @return the refusal text
    public static String hostMismatch(Entry entry, @Nullable String harnessVersion) {
        String host = harnessVersion == null || harnessVersion.isBlank() ? "未知版本" : harnessVersion;
        if (!entry.terminal()) {
            // The refusal is short on purpose: "it installs but you cannot use it" is an
            // invitation to try, and trying is exactly what the button is there to stop.
            return entry.packageName() + " 需要 dsh " + entry.requiresDsh() + "，这个实例是 " + host
                    + "。它是能力插件（不是终端），只在 0.2.x 上提供；请把实例换成 0.2.x 的版本。";
        }
        if (entry.bundled() && entry.hasExecutable()) {
            return entry.packageName() + " 需要 dsh " + entry.requiresDsh() + "，这个实例是 " + host
                    + "。它通过 ACP 与 dsh 通信，而 ACP 的 profile 模板从 dsh 0.1.2-rc.1 起才有；"
                    + "旧宿主没有这个模板，终端会起不来。请把实例换成要求的 dsh 版本（升级或降级），或改用 "
                    + alternativeFor(entry, harnessVersion) + "。";
        }
        if (entry.bundled()) {
            return entry.packageName() + " 需要 dsh " + entry.requiresDsh() + "，这个实例是 " + host
                    + "。它跑在 harness 进程里，用的是这条线上才有的 agent 流、审批与模型选择服务；"
                    + "更旧或更新的宿主都没有同一套接缝（更新的一版改了它们，插件没有验证过，"
                    + "dsh 自己也会拒绝加载），插件装上也起不来。请把实例换成要求的 dsh 版本"
                    + "（升级或降级），或改用 "
                    + alternativeFor(entry, harnessVersion) + "（走 ACP，两条线都能跑）。";
        }
        return entry.packageName() + " 需要 dsh " + entry.requiresDsh() + "，这个实例是 "
                + host
                + "。它的工具面来自 dsh 0.2.0 起的 agent-preset 层，装在旧宿主上只会得到一个"
                + "「没有工具面」的会话。请把实例换成要求的 dsh 版本（升级或降级），或改用 "
                + spanningEntry() + "（它的 peer 跨两条线）。";
    }

    /// The first other entry that can actually run on this host.
    ///
    /// For a bundled entry the alternative should be the panel's own fallback when there is
    /// one — on a 0.1.x instance, the in-process terminal's answer is the ACP one, not a
    /// community package that merely happens to declare no floor.
    ///
    /// @param entry          the entry that does not fit
    /// @param harnessVersion the host line, or `null` when unknown
    /// @return the package name to offer
    private static String alternativeFor(Entry entry, @Nullable String harnessVersion) {
        for (Entry candidate : ENTRIES) {
            if (candidate != entry && candidate.bundled() && fitsHost(candidate, harnessVersion)) {
                return candidate.packageName();
            }
        }
        for (Entry candidate : ENTRIES) {
            if (candidate != entry && fitsHost(candidate, harnessVersion)) {
                return candidate.packageName();
            }
        }
        return spanningEntry();
    }

    /// The entry that starts on any published harness line, named in [hostMismatch] as the
    /// alternative to offer.
    private static String spanningEntry() {
        for (Entry entry : ENTRIES) {
            if (entry.requiresDsh() == null || entry.requiresDsh().isBlank()) {
                return entry.packageName();
            }
        }
        return ENTRIES.isEmpty() ? "" : ENTRIES.get(0).packageName();
    }

    /// Returns the package a specification names, without its version.
    ///
    /// @param spec the specification, as typed or as the catalogue pins it
    /// @return the package name, or null when the specification names no package
    public static @Nullable String packageNameOf(@Nullable String spec) {
        if (spec == null) {
            return null;
        }
        String raw = spec.trim();
        if (raw.isEmpty()) {
            return null;
        }
        // `@scope/name@1.2.3` keeps its leading `@`; anything from the second one on is
        // the version. A bare `name@1.2.3`, a repository (`github:o/n`), a file and an
        // archive URL all come out as themselves, and simply match no entry.
        int at = raw.indexOf('@', raw.startsWith("@") ? 1 : 0);
        return at <= 0 ? raw : raw.substring(0, at);
    }

    /// Refuses to install a terminal plugin into a profile the harness ships an app for.
    ///
    /// The refusal is the whole point of this class's `profile` field: a terminal plugin
    /// and the web app are both *app* plugins, each parsing the same argv with its own
    /// grammar while upstream arbitrates nothing between them, so the pair does not boot
    /// — the community's own report is `error: unknown option '--no-open'`. A refusal
    /// that names the profile to use instead is worth more than an install that bricks
    /// the instance.
    ///
    /// Asked of **installs**, not removals: a profile that somehow holds one of these is
    /// fixed by taking it out, and refusing the removal would leave it broken.
    ///
    /// @param spec    the specification being installed
    /// @param profile the profile it would be installed into
    /// @throws DshException when the specification is a known terminal plugin and the
    ///                       profile boots one of the harness's own apps
    public static void refuseAppProfile(@Nullable String spec, @Nullable String profile) throws DshException {
        if (profile == null || profile.isBlank()) {
            return;
        }
        // Named surfaces only: `DshSurface.ofProfile` answers OTHER for a profile
        // somebody made up, which is exactly the profile a terminal plugin belongs in.
        DshSurface surface = DshSurface.ofProfile(profile);
        if (surface == DshSurface.OTHER) {
            return;
        }
        String name = packageNameOf(spec);
        Optional<Entry> entry = find(name);
        if (entry.isEmpty()) {
            return;
        }
        throw new DshException("TUI 插件不能装进 " + surface.profileName() + " profile：它和 "
                + surface.appBundle() + " 都是 app 插件，各自按自己的语法解析同一份 argv，"
                + "上游不仲裁，装在一起启动就会失败（社区实测 `error: unknown option '--no-open'`）。"
                + "请装进 " + entry.get().packageName() + " 自己的 profile（" + entry.get().profile() + "）。");
    }
}