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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/// What a consumer (an instance, a brand default, the launcher default) says
/// about the Node.js it wants, and how that answer becomes a concrete runtime.
///
/// The stored value stays a plain string so existing manifests keep working:
///
/// - `null` / empty / `global` / `inherit` — follow the next layer;
/// - `auto` / `system` — the default: whatever `PATH` offers when it is usable,
///   otherwise the newest enabled managed runtime;
/// - `22` (a major) / `lts` / `current` — a channel, resolved against what is
///   installed right now;
/// - `22.23.1` — an exact version.
///
/// The launcher used to answer this question in two different places (the
/// instance launcher and the brand runtime), each with its own idea of what
/// `system` means. Both now call [#resolve], so a selector cannot mean one
/// thing on the DSH page and another on the brand page.
@NotNullByDefault
public final class NodeSelection {

    /// The kinds a selector can name.
    public enum Kind {
        /// Follow the next layer (brand default, then launcher default).
        INHERIT,
        /// Let the panel choose a usable runtime.
        AUTO,
        /// Resolve against installed runtimes of one major line.
        CHANNEL,
        /// Name one installed version.
        EXACT,
        /// A value this build does not understand.
        INVALID
    }

    /// Where a resolved runtime came from, for the answer's `source` field.
    public enum Source {
        SYSTEM,
        MANAGED,
        CHANNEL,
        EXACT,
        NONE
    }

    private final Kind kind;
    private final String value;

    private NodeSelection(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    /// Parses a stored selector. Never throws: an unreadable value is
    /// [#Kind#INVALID], which resolves to `null` and therefore means "refuse",
    /// not "use the system runtime".
    ///
    /// @param raw the stored value, possibly `null`
    /// @return the parsed selector
    public static NodeSelection parse(@Nullable String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || "global".equalsIgnoreCase(text) || "inherit".equalsIgnoreCase(text)) {
            return new NodeSelection(Kind.INHERIT, "");
        }
        if ("auto".equalsIgnoreCase(text) || "system".equalsIgnoreCase(text)) {
            return new NodeSelection(Kind.AUTO, "");
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if ("lts".equals(lower) || "current".equals(lower)) {
            return new NodeSelection(Kind.CHANNEL, lower);
        }
        if (text.chars().allMatch(Character::isDigit)) {
            return new NodeSelection(Kind.CHANNEL, text);
        }
        if (text.matches("\\d+\\.\\d+\\.\\d+.*")) {
            return new NodeSelection(Kind.EXACT, text);
        }
        return new NodeSelection(Kind.INVALID, text);
    }

    /// The kind this selector names.
    ///
    /// @return the kind
    public Kind kind() {
        return kind;
    }

    /// The channel name or exact version, or an empty string for
    /// [#Kind#INHERIT] and [#Kind#AUTO].
    ///
    /// @return the value
    public String value() {
        return value;
    }

    /// Whether this selector follows the next layer.
    ///
    /// @return whether it is [#Kind#INHERIT]
    public boolean isInherit() {
        return kind == Kind.INHERIT;
    }

    /// A resolved runtime and why it was chosen.
    ///
    /// @param runtime    the runtime, or `null` when nothing usable matched
    /// @param source     where it came from
    /// @param compatible whether its version satisfies the harness range
    /// @param reason     one line the UI can show
    public record Resolution(@Nullable DshNodeRuntime runtime, Source source,
                             boolean compatible, String reason) {

        /// Whether a runtime was resolved.
        ///
        /// @return whether [#runtime] is present
        public boolean present() {
            return runtime != null;
        }

        /// The resolved version, or `null`.
        ///
        /// @return the version string
        public @Nullable String version() {
            return runtime == null ? null : runtime.nodeVersion();
        }
    }

    /// Resolves a selector, following one inheritance step at most.
    ///
    /// @param selection the selector to resolve
    /// @param fallback  the next layer's selector, or `null` when there is none
    /// @param needsPnpm whether the caller needs a runtime that carries pnpm
    /// @return the resolution, never `null`
    public static Resolution resolve(NodeSelection selection, @Nullable String fallback, boolean needsPnpm) {
        return resolve(selection, fallback, needsPnpm,
                NodeRuntimeManager.listInstalled(),
                NodeRuntimeManager::isDisabled,
                DshNodeRuntime::detect,
                NodeChannels::version);
    }

    /// The resolution with its environment named, so a test can pin every
    /// input instead of reading the machine's `/proc`, data directory and PATH.
    static Resolution resolve(NodeSelection selection, @Nullable String fallback, boolean needsPnpm,
                              List<NodeRuntime> installed, Predicate<String> disabled,
                              Supplier<Optional<DshNodeRuntime>> onPath) {
        return resolve(selection, fallback, needsPnpm, installed, disabled, onPath, name -> null);
    }

    /// The same, with the channel pointer as one more injectable input.
    static Resolution resolve(NodeSelection selection, @Nullable String fallback, boolean needsPnpm,
                              List<NodeRuntime> installed, Predicate<String> disabled,
                              Supplier<Optional<DshNodeRuntime>> onPath,
                              Function<String, @Nullable String> channelPin) {
        return switch (selection.kind) {
            case INHERIT -> {
                NodeSelection inherited = parse(fallback);
                // A layer that itself says "inherit" falls through to auto: the
                // alternative is a chain that never ends.
                if (inherited.isInherit() || inherited.kind == Kind.INVALID) {
                    yield auto(needsPnpm, installed, disabled, onPath);
                }
                yield resolve(inherited, null, needsPnpm, installed, disabled, onPath, channelPin);
            }
            case AUTO -> auto(needsPnpm, installed, disabled, onPath);
            case CHANNEL -> channel(selection.value, installed, disabled, channelPin);
            case EXACT -> exact(selection.value, installed, disabled);
            case INVALID -> new Resolution(null, Source.NONE, false,
                    "无法识别的 Node.js 选择: " + selection.value);
        };
    }

    /// The default answer: a usable `PATH` runtime first, then the newest
    /// enabled managed runtime.
    private static Resolution auto(boolean needsPnpm, List<NodeRuntime> installed,
                                   Predicate<String> disabled,
                                   Supplier<Optional<DshNodeRuntime>> onPath) {
        DshNodeRuntime path = onPath.get().orElse(null);
        if (path != null && path.isNodeSupported() && (!needsPnpm || path.canManagePlugins())) {
            return new Resolution(path, Source.SYSTEM, true,
                    "使用 PATH 上的 Node.js " + path.nodeVersion());
        }
        DshNodeRuntime managed = newestManaged(needsPnpm, installed, disabled);
        if (managed != null) {
            return new Resolution(managed, Source.MANAGED, true,
                    "使用面板托管的 Node.js " + managed.nodeVersion());
        }
        if (path != null) {
            boolean supported = path.isNodeSupported();
            String why = supported
                    ? "PATH 上的 Node.js " + path.nodeVersion() + " 缺少 pnpm"
                    : "PATH 上的 Node.js " + path.nodeVersion()
                            + " 不满足 " + DshNodeRuntime.requirement();
            return new Resolution(path, Source.SYSTEM, supported, why);
        }
        return new Resolution(null, Source.NONE, false,
                "PATH 上没有 Node.js，也没有可用的托管运行时（" + DshNodeRuntime.requirement() + "）");
    }

    /// The newest supported, enabled managed runtime. A runtime with pnpm wins
    /// when the caller needs it; the first supported one is the fallback so the
    /// caller can say "this runtime has no pnpm" instead of "no runtime".
    private static @Nullable DshNodeRuntime newestManaged(boolean needsPnpm, List<NodeRuntime> installed,
                                                          Predicate<String> disabled) {
        DshNodeRuntime fallback = null;
        for (NodeRuntime runtime : installed) {
            if (!DshNodeRuntime.isSupportedNodeVersion(runtime.version())) {
                continue;
            }
            if (disabled.test(runtime.version())) {
                continue;
            }
            DshNodeRuntime described = DshNodeRuntime.fromManaged(runtime);
            if (!needsPnpm || described.canManagePlugins()) {
                return described;
            }
            if (fallback == null) {
                fallback = described;
            }
        }
        return needsPnpm ? fallback : null;
    }

    /// The highest enabled installed version on a major line.
    private static Resolution channel(String name, List<NodeRuntime> installed, Predicate<String> disabled,
                                      Function<String, @Nullable String> channelPin) {
        // A recorded pointer wins over "highest installed" — that is what makes
        // "update Node 22" mean something. It only wins while the version it
        // names is still installed, enabled and supported; otherwise the line is
        // answered from disk, so deleting a runtime never leaves a channel dead.
        String pinned = channelPin.apply(name);
        if (pinned != null) {
            NodeRuntime runtime = find(installed, pinned);
            if (runtime != null
                    && !disabled.test(pinned)
                    && DshNodeRuntime.isSupportedNodeVersion(pinned)) {
                return new Resolution(DshNodeRuntime.fromManaged(runtime), Source.CHANNEL, true,
                        "channel " + name + " → " + pinned + "（通道指针）");
            }
        }
        for (NodeRuntime runtime : installed) {
            if (!matches(runtime.version(), name)) {
                continue;
            }
            if (disabled.test(runtime.version())) {
                continue;
            }
            if (!DshNodeRuntime.isSupportedNodeVersion(runtime.version())) {
                continue;
            }
            return new Resolution(DshNodeRuntime.fromManaged(runtime), Source.CHANNEL, true,
                    "channel " + name + " → " + runtime.version());
        }
        return new Resolution(null, Source.CHANNEL, false,
                "没有已安装且启用的 Node.js " + name);
    }

    private static @Nullable NodeRuntime find(List<NodeRuntime> installed, String version) {
        for (NodeRuntime runtime : installed) {
            if (runtime.version().equals(version)) {
                return runtime;
            }
        }
        return null;
    }

    /// Whether a version belongs to a channel. `lts` is the even-major line
    /// (Node publishes LTS on even majors); `current` is every installed line.
    private static boolean matches(String version, String name) {
        if ("lts".equals(name)) {
            return major(version) % 2 == 0;
        }
        if ("current".equals(name)) {
            return true;
        }
        return version.startsWith(name + ".");
    }

    private static int major(String version) {
        int dot = version.indexOf('.');
        String head = dot < 0 ? version : version.substring(0, dot);
        try {
            return Integer.parseInt(head);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// An explicitly pinned version. A disabled pin still resolves — the
    /// operator asked for this exact version, and the UI marks it instead of
    /// silently switching the instance.
    private static Resolution exact(String version, List<NodeRuntime> installed, Predicate<String> disabled) {
        NodeRuntime found = null;
        for (NodeRuntime runtime : installed) {
            if (runtime.version().equals(version)) {
                found = runtime;
                break;
            }
        }
        if (found == null) {
            return new Resolution(null, Source.EXACT, false,
                    "Node.js " + version + " 未安装");
        }
        boolean off = disabled.test(version);
        boolean compatible = DshNodeRuntime.isSupportedNodeVersion(version);
        String why = off
                ? "Node.js " + version + " 已被禁用，但由精确选择固定"
                : compatible
                        ? "精确使用 Node.js " + version
                        : "Node.js " + version + " 不满足 " + DshNodeRuntime.requirement();
        return new Resolution(DshNodeRuntime.fromManaged(found), Source.EXACT, compatible, why);
    }
}
