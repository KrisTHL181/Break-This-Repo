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

import org.jackhuang.hmcl.util.platform.Executables;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/// Describes the JavaScript toolchain the launcher found on this machine.
///
/// DeepSeek Harness needs Node `^22.19.0 || >=24.0.0`. `pnpm` is only needed
/// for `dsh plugin`, so its absence degrades plugin management rather than
/// preventing the launcher from working.
///
/// @param node         the resolved `node` executable
/// @param nodeVersion  the node version string, without a leading `v`
/// @param npm          the resolved `npm` executable, or `null` when absent
/// @param npmVersion   the npm version string, or `null` when absent
/// @param pnpm         the resolved `pnpm` executable, or `null` when absent
/// @param pnpmVersion  the pnpm version string, or `null` when absent
@NotNullByDefault
public record DshNodeRuntime(
        Path node,
        String nodeVersion,
        @Nullable Path npm,
        @Nullable String npmVersion,
        @Nullable Path pnpm,
        @Nullable String pnpmVersion) {

    /// The lowest supported Node 22 release.
    private static final Version MINIMUM_NODE_22 = new Version(22, 19, 0);

    /// The next major after Node 22; the `^22.19.0` range stops before it.
    private static final int NODE_22_CEILING = 23;

    /// The lowest supported Node 24-or-newer release.
    private static final int MINIMUM_NODE_MODERN_MAJOR = 24;

    /// A parsed `major.minor.patch` version.
    ///
    /// @param major the major component
    /// @param minor the minor component
    /// @param patch the patch component
    private record Version(int major, int minor, int patch) implements Comparable<Version> {
        @Override
        public int compareTo(Version other) {
            int result = Integer.compare(major, other.major);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(minor, other.minor);
            return result != 0 ? result : Integer.compare(patch, other.patch);
        }
    }

    /// The selection value meaning "use whatever is on `PATH`".
    public static final String SYSTEM = "system";

    /// The selection meaning "whatever the launcher is set to".
    public static final String GLOBAL = "global";

    /// The oldest pnpm DeepSeek Harness can be installed with.
    ///
    /// A floor pnpm itself sets rather than one this launcher invents: from 10.9 the package manager
    /// resolves and links the way the harness's own install expects. Below it an install does not
    /// fail where it can be seen — it produces a tree the harness cannot start from — so the version
    /// is checked before the install rather than diagnosed after it.
    public static final String MINIMUM_PNPM = "10.9";

    /// Describes a launcher-managed runtime in the same shape as a probed one.
    ///
    /// `pnpm` is deliberately left unset: a managed runtime has no `pnpm` of its
    /// own, so plugin management falls back to whatever the system provides.
    ///
    /// @param runtime the installed runtime
    /// @return the descriptor
    public static DshNodeRuntime fromManaged(NodeRuntime runtime) {
        // pnpm lives beside node, not in the distribution metadata, so it is
        // probed directly — through the layout, because "beside node" is `bin/` on
        // POSIX and the distribution root on Windows. Leaving it unresolved made
        // every managed runtime look incapable of plugin management even after
        // pnpm was installed into it.
        Path pnpm = runtime.pnpm();
        if (!Files.isRegularFile(pnpm)) {
            pnpm = null;
        }
        Path bin = runtime.binDirectory();

        // npm/pnpm 是 `#!/usr/bin/env node` 脚本（Windows 上是 `%~dp0\node.exe` 的
        // 批处理）：探测和日后 spawn 都得先把运行时自己的目录塞进 PATH，否则裸部署
        // （PATH 无 node）下「装好了却报 npm 缺失」。
        java.util.Map<String, String> env = java.util.Map.of("PATH",
                bin.toString() + java.io.File.pathSeparator
                        + java.util.Optional.ofNullable(System.getenv("PATH")).orElse(""));

        return new DshNodeRuntime(
                runtime.node(),
                runtime.version(),
                runtime.npm(),
                runtime.npm() == null ? null : versionOf(runtime.npm(), env),
                pnpm,
                pnpm == null ? null : versionOf(pnpm, env));
    }

    /// How long a successful probe is reused. The toolchain does not change
    /// while the panel runs, and every launch would otherwise pay for up to
    /// three `--version` spawns.
    private static final long PROBE_CACHE_NANOS = 60L * 1_000_000_000L;

    /// The last successful probe, if it is still fresh. Failures are never
    /// cached.
    private static volatile @Nullable DshNodeRuntime lastDetected;
    private static volatile long lastDetectedAt;

    /// Drops the probe cache. Called after a managed runtime is installed or
    /// removed — a cached "no node at all" would otherwise shadow the runtime
    /// for up to a minute.
    public static void invalidateCache() {
        lastDetected = null;
        lastDetectedAt = 0;
    }

    /// The newest installed launcher-managed runtime the harness supports —
    /// what a bare deployment (no Node.js on PATH) falls back to. Managed
    /// runtimes are installed under `<data>/hdsl/runtimes` from the panel's
    /// Settings → Node.js page.
    ///
    /// A runtime that carries `pnpm` wins over one that does not. Managed runtimes
    /// are installed *with* pnpm beside them ([NodeRuntimeManager]), so that is the
    /// one toolchain on a machine guaranteed to be able to install the harness —
    /// and picking the other one is how "the panel has everything it needs" turned
    /// into "pnpm was not found on PATH".
    ///
    /// @return the managed runtime, or empty when none is installed
    public static Optional<DshNodeRuntime> detectManaged() {
        // Runtimes without a supported Node.js release are skipped either way: one that
        // cannot run the harness is no answer, pnpm or not.
        Optional<DshNodeRuntime> withPnpm = detectManaged(true);
        return withPnpm.isPresent() ? withPnpm : detectManaged(false);
    }

    /// The managed-runtime search, with "must carry pnpm" as the flag.
    ///
    /// @param needsPnpm whether the answer must have a `pnpm` of its own
    /// @return the managed runtime, or empty when none qualifies
    private static Optional<DshNodeRuntime> detectManaged(boolean needsPnpm) {
        for (NodeRuntime runtime : NodeRuntimeManager.listInstalled()) {
            if (!isSupportedNodeVersion(runtime.version())) {
                continue;
            }
            DshNodeRuntime described = fromManaged(runtime);
            if (!needsPnpm || described.canManagePlugins()) {
                return Optional.of(described);
            }
        }
        return Optional.empty();
    }

    /// The toolchain to use when the caller needs `pnpm`.
    ///
    /// The order is the point: whatever `PATH` offers **when it is a supported Node.js that
    /// carries pnpm**, then a managed runtime (which is supported and installed with pnpm),
    /// and only then the plain answer — so a caller that has to fail can say "this Node.js
    /// has no pnpm" or "this Node.js is too old" instead of "no Node.js" or, worse,
    /// "pnpm was not found on PATH" while a working toolchain was installed under the data
    /// directory.
    ///
    /// @return the runtime to use, or empty when there is no Node.js at all
    public static Optional<DshNodeRuntime> detectWithPnpm() {
        return prefer(detect(), detectManaged(),
                runtime -> runtime.isNodeSupported() && runtime.canManagePlugins());
    }

    /// The toolchain to use when the caller needs `npm` — reading the registry and the
    /// version lists.
    ///
    /// Debian and Ubuntu ship `nodejs` without npm (it is a separate `npm` package), so
    /// "a Node.js is on PATH" is routinely a runtime that cannot query a registry. Asking
    /// `PATH` first and refusing afterwards made those hosts report "npm was not found on
    /// PATH" while the panel's own runtime — installed with npm — sat in the data
    /// directory unused.
    ///
    /// @return the runtime to use, or empty when there is no Node.js at all
    public static Optional<DshNodeRuntime> detectForNpm() {
        return prefer(detect(), detectManaged(), DshNodeRuntime::canInstall);
    }

    /// The toolchain to use when the caller needs a Node.js in the supported range.
    ///
    /// "A Node.js is on PATH" is not the same answer as "a Node.js this program can use":
    /// a host whose `PATH` carries Node 20 (Debian/Ubuntu packages are routinely too old)
    /// made every dsh launch refuse with "outside the supported range" while a supported
    /// managed runtime sat unused in the data directory, and brand tools crashed inside
    /// their own ESM loader (`node:zlib has no export named createZstdDecompress`) instead
    /// of being told which Node.js they needed.
    ///
    /// @return the runtime to use, or empty when there is no Node.js at all
    public static Optional<DshNodeRuntime> detectSupported() {
        return prefer(detect(), detectManaged(), DshNodeRuntime::isNodeSupported);
    }

    /// The choice behind [#detectWithPnpm] and [#detectSupported], with both probes passed in.
    ///
    /// `PATH` keeps priority when what it offers satisfies `usable` — the operator put it
    /// there. When it does not, a managed runtime gets the turn. When neither does, `PATH`'s
    /// own comes back so the caller can name the runtime it is complaining about instead of
    /// claiming nothing was found.
    ///
    /// @param onPath  what `PATH` offered, if anything
    /// @param managed the panel's own runtime, if one is installed
    /// @param usable  what the caller needs from the answer
    /// @return the runtime to use
    static Optional<DshNodeRuntime> prefer(Optional<DshNodeRuntime> onPath,
                                          Optional<DshNodeRuntime> managed,
                                          java.util.function.Predicate<DshNodeRuntime> usable) {
        if (onPath.isPresent() && usable.test(onPath.get())) {
            return onPath;
        }
        return managed.isPresent() ? managed : onPath;
    }

    /// Probes the toolchain on the current `PATH`.
    ///
    /// A successful result is reused for [PROBE_CACHE_NANOS] — mostly to spare
    /// the spawns, but also because a spawn that fails on a loaded machine
    /// would otherwise read as "no Node.js at all" and fail a launch the
    /// toolchain was perfectly able to serve. Every `--version` probe gets one
    /// retry for the same reason.
    ///
    /// @return the detected runtime, or empty when no usable `node` was found
    public static Optional<DshNodeRuntime> detect() {
        DshNodeRuntime cached = lastDetected;
        if (cached != null && System.nanoTime() - lastDetectedAt < PROBE_CACHE_NANOS) {
            return Optional.of(cached);
        }

        Path node = which("node").orElse(null);
        if (node == null) {
            return Optional.empty();
        }

        String nodeVersion = versionOfRetrying(node);
        if (nodeVersion == null) {
            return Optional.empty();
        }

        Path npm = which("npm").orElse(null);
        Path pnpm = which("pnpm").orElse(null);

        DshNodeRuntime runtime = new DshNodeRuntime(
                node,
                nodeVersion,
                npm,
                npm == null ? null : versionOfRetrying(npm),
                pnpm,
                pnpm == null ? null : versionOfRetrying(pnpm));
        lastDetected = runtime;
        lastDetectedAt = System.nanoTime();
        return Optional.of(runtime);
    }

    /// [versionOf] with one retry: spawning a process can fail transiently on a
    /// loaded machine, and treating that as "the tool is not installed" fails
    /// work the toolchain could have done.
    private static @Nullable String versionOfRetrying(Path executable) {
        String version = versionOf(executable);
        return version != null ? version : versionOf(executable);
    }

    /// Reports whether this Node release satisfies DeepSeek Harness's `engines` range.
    ///
    /// @return whether the version is `^22.19.0` or `>=24.0.0`
    public boolean isNodeSupported() {
        return isSupportedNodeVersion(nodeVersion);
    }

    /// Reports whether this runtime can install DeepSeek Harness versions.
    ///
    /// @return whether `npm` is available
    public boolean canInstall() {
        return npm != null && npmVersion != null;
    }

    /// Reports whether plugin management through `dsh plugin` is possible.
    ///
    /// @return whether `pnpm` is available
    public boolean canManagePlugins() {
        return pnpm != null;
    }

    /// The refusal for work that needs `pnpm` and cannot have it.
    ///
    /// The runtime is named because that is the whole puzzle: "pnpm was not found
    /// on PATH" is true of a machine that has `/opt/node/bin/node` and nothing
    /// beside it, and it says nothing about the two things that fix it — installing
    /// pnpm, or using the panel's managed runtime, which is installed with pnpm
    /// beside it.
    ///
    /// @param action what was being attempted, in the caller's words
    /// @return the exception to throw
    public DshException pnpmMissing(String action) {
        return new DshException("pnpm " + MINIMUM_PNPM + "+ was not found: the Node.js at " + node
                + " has none beside it, PATH has none, and no panel-managed runtime provides one — "
                + action + " needs it. Install it (`npm install -g pnpm`) or install a managed runtime "
                + "on Settings -> Node.js, which carries pnpm.");
    }

    /// Returns the environment entries that put this runtime's tools first.
    ///
    /// DeepSeek Harness resolves `pnpm` by name through PATH, so any child the
    /// launcher starts for an instance must be given the instance's runtime
    /// ahead of the system's. Otherwise a managed runtime silently borrows the
    /// system tooling, or finds none.
    ///
    /// @return the environment additions
    public java.util.Map<String, String> pathEnvironment() {
        String existing = System.getenv("PATH");
        String bin = binDirectory().toString();
        return java.util.Map.of("PATH", existing == null || existing.isBlank()
                ? bin
                : bin + java.io.File.pathSeparator + existing);
    }

    /// Returns the directory holding this runtime's executables.
    ///
    /// Used to put the runtime ahead of the system's tooling on the child's
    /// PATH, which is what makes a managed runtime self-contained.
    ///
    /// @return the `bin` directory
    public Path binDirectory() {
        Path parent = node.getParent();
        return parent == null ? node : parent;
    }

    /// Tests a version string against DeepSeek Harness's Node range.
    ///
    /// @param version the version string, optionally prefixed with `v`
    /// @return whether the version is supported
    public static boolean isSupportedNodeVersion(String version) {
        Version parsed = parse(version);
        if (parsed == null) {
            return false;
        }
        if (parsed.major() >= MINIMUM_NODE_MODERN_MAJOR) {
            return true;
        }
        return parsed.major() == MINIMUM_NODE_22.major()
                && parsed.major() < NODE_22_CEILING
                && parsed.compareTo(MINIMUM_NODE_22) >= 0;
    }

    /// The Node.js range DeepSeek Harness runs on, as the user reads it.
    ///
    /// Kept apart from [#requirement] because it is the half that is about the *version*: a
    /// banner line saying "that Node.js is outside …" should name the range and nothing else.
    public static final String NODE_RANGE = "^22.19.0 || >=24.0.0";

    /// Returns the toolchain requirement shown to the user.
    ///
    /// @return a human-readable requirement string
    public static String requirement() {
        return "Node.js " + NODE_RANGE + ", plus pnpm " + MINIMUM_PNPM
                + " or newer for installing DeepSeek Harness; pnpm is also needed for plugin management";
    }

    /// Parses a leading `major.minor.patch` from a version string.
    ///
    /// @param version the raw version, optionally prefixed with `v`
    /// @return the parsed version, or `null` when it cannot be parsed
    private static @Nullable Version parse(String version) {
        String cleaned = version.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }
        String[] parts = cleaned.split("[.\\-+]", 3);
        if (parts.length == 0) {
            return null;
        }
        try {
            int major = Integer.parseInt(parts[0].trim());
            int minor = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2].split("[.\\-+]", 2)[0].trim()) : 0;
            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /// Asks an executable for its version.
    ///
    /// @param executable the program to run with `--version`
    /// @return the trimmed first output line, or `null` when the probe failed
    private static @Nullable String versionOf(Path executable) {
        return versionOf(executable, java.util.Map.of());
    }

    /// @param environment extra variables for the probe (PATH prepends)
    private static @Nullable String versionOf(Path executable, java.util.Map<String, String> environment) {
        try {
            DshCommand.Result result = DshCommand.run(List.of(executable.toString(), "--version"),
                    null, environment, null);
            if (!result.isSuccess() || result.output().isEmpty()) {
                return null;
            }
            String first = result.output().get(0).trim();
            return first.startsWith("v") || first.startsWith("V") ? first.substring(1) : first;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /// Resolves an executable on `PATH`.
    ///
    /// The name is completed by platform: Windows decides what is executable by its
    /// extension, so `node` is `node.exe` and `npm` is `npm.cmd`, neither of which the
    /// bare name names. Resolving here rather than in each caller is what makes a
    /// hand-installed Node.js visible on Windows — without it the panel concluded
    /// "no Node.js on this machine" about a machine that had one, and pushed the user
    /// at a managed runtime instead.
    ///
    /// @param name the executable name
    /// @return the resolved path, or empty when it is not on `PATH`
    public static Optional<Path> which(String name) {
        return Executables.resolve(name);
    }
}
