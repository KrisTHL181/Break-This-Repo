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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kala.compress.archivers.ArchiveEntry;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Links;
import org.jackhuang.hmcl.util.io.NetworkUtils;
import org.jackhuang.hmcl.util.io.SafePaths;
import org.jackhuang.hmcl.util.platform.Platform;
import org.jackhuang.hmcl.util.tree.ArchiveFileTree;
import org.jackhuang.hmcl.util.tree.TarFileTree;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.tukaani.xz.XZInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Installs and enumerates the Node.js runtimes HDSL owns.
///
/// This mirrors HMCL's Java runtime management: instead of depending on
/// whatever the distribution ships, the launcher can fetch a specific Node
/// release into `runtimes/<version>/` and pin an instance to it. That matters
/// because DeepSeek Harness declares `engines.node` as `^22.19.0 || >=24.0.0`.
///
/// Runtimes come from the official `nodejs.org` distribution, so extraction has
/// to reproduce the archive faithfully — including the symlinks that make
/// `bin/npm` and `bin/npx` work.
@NotNullByDefault
public final class NodeRuntimeManager {
    private NodeRuntimeManager() {
    }

    /// The Node distribution index listing every published release.
    /// The download base for a specific release.
    /// Returns the platform tag used in Node distribution file names.
    ///
    /// @return `linux-x64`, `darwin-arm64`, `win-x64` …
    /// @throws DshException when the current system has no Node build
    public static String platformTag() throws DshException {
        return platformTag(Platform.SYSTEM_PLATFORM);
    }

    /// Returns the platform tag Node publishes a build under for a platform.
    ///
    /// The tag is what appears in the archive's file name, and it is the one thing that
    /// decides whether the download is a usable runtime: asking for the Linux build on
    /// Windows produces a tarball that unpacks perfectly, registers as an installed
    /// runtime, and is an ELF binary the system cannot run. That is why the platform is
    /// a parameter rather than the ambient one — a test can pin the Windows answer from
    /// any machine, and this is the fact most worth pinning.
    ///
    /// @param platform the platform to name
    /// @return the tag that appears in the archive's file name
    /// @throws DshException when Node publishes no build for that platform
    static String platformTag(Platform platform) throws DshException {
        return switch (platform.os()) {
            case WINDOWS -> switch (platform.arch()) {
                case X86_64 -> "win-x64";
                case ARM64 -> "win-arm64";
                default -> throw new DshException(
                        "Node.js publishes no build for " + platform.arch() + " on Windows");
            };
            case MACOS -> switch (platform.arch()) {
                case X86_64 -> "darwin-x64";
                case ARM64 -> "darwin-arm64";
                default -> throw new DshException(
                        "Node.js publishes no build for " + platform.arch() + " on macOS");
            };
            // FreeBSD is not "Linux with a different name": nodejs.org publishes no
            // FreeBSD archive, so the old `default` branch would download a Linux ELF,
            // unpack it, register it as installed and then fail to exec it. Refuse
            // here instead, and point at the package that does work.
            case FREEBSD -> throw new DshException(
                    "Node.js publishes no FreeBSD build; install node and pnpm from"
                            + " packages (pkg install node npm), and the panel will use them"
                            + " from PATH instead of a managed runtime");
            default -> switch (platform.arch()) {
                case X86_64 -> "linux-x64";
                case ARM64 -> "linux-arm64";
                default -> throw new DshException(
                        "Node.js publishes no build for " + platform.arch() + " on Linux");
            };
        };
    }

    /// Returns the entry the release index lists for a platform tag.
    ///
    /// The archives are named `darwin-x64`, `win-x64` and `linux-x64`, while the index
    /// calls the same builds `osx-x64-tar`, `win-x64-zip` and `linux-x64` — the suffix
    /// says which archive format the entry is for, which is what a new platform has to
    /// know before it can filter the release list.
    ///
    /// @param platform the tag from [NodeRuntimeManager#platformTag]
    /// @return the name to look for in the index `files` array
    static String indexTag(String platform) {
        return switch (platform) {
            case "darwin-x64" -> "osx-x64-tar";
            case "darwin-arm64" -> "osx-arm64-tar";
            case "win-x64" -> "win-x64-zip";
            case "win-arm64" -> "win-arm64-zip";
            default -> platform;
        };
    }

    /// Returns the archive extension Node publishes for a platform tag.
    ///
    /// Linux builds ship as `.tar.xz` and macOS builds as `.tar.gz`; Windows builds
    /// ship as `.zip` and nothing else — Node has never published a `win-*.tar.xz`.
    ///
    /// @param platform the tag from [NodeRuntimeManager#platformTag]
    /// @return `.zip`, `.tar.gz` or `.tar.xz`
    static String archiveExtension(String platform) {
        if (platform.startsWith("win-")) {
            return ".zip";
        }
        if (platform.startsWith("darwin-")) {
            return ".tar.gz";
        }
        return ".tar.xz";
    }

    /// Returns the archive file name for a version and platform tag.
    ///
    /// The extension follows the tag rather than the ambient system, so a name and the
    /// tag it was built from cannot disagree.
    ///
    /// @param version  the version without its leading `v`
    /// @param platform the tag from [NodeRuntimeManager#platformTag]
    /// @return e.g. `node-v22.19.0-win-x64.zip`
    static String archiveFileName(String version, String platform) {
        return "node-v" + version + "-" + platform + archiveExtension(platform);
    }

    /// Lists the Node runtimes installed under [DshPaths#RUNTIMES].
    ///
    /// @return the installed runtimes, newest first
    public static List<NodeRuntime> listInstalled() {
        List<NodeRuntime> runtimes = new ArrayList<>();
        Path root = DshPaths.RUNTIMES;
        if (!Files.isDirectory(root)) {
            return runtimes;
        }
        try (Stream<Path> entries = Files.list(root)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                NodeRuntime runtime = new NodeRuntime(directory.getFileName().toString(), directory);
                if (runtime.isUsable()) {
                    runtimes.add(runtime);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to enumerate installed Node runtimes in " + root, e);
        }
        runtimes.sort(Comparator.comparing(NodeRuntime::version, DshVersionManager::compareVersions).reversed());
        return runtimes;
    }

    /// Finds an installed Node runtime by version.
    ///
    /// @param version the version string
    /// @return the runtime, or `null` when it is not installed
    public static @Nullable NodeRuntime findInstalled(String version) {
        for (NodeRuntime runtime : listInstalled()) {
            if (runtime.version().equals(version)) {
                return runtime;
            }
        }
        return null;
    }

    /// The file that annotates the distribution directories: which versions the
    /// operator disabled, and when each was last used. The directories stay the
    /// source of truth for "what is installed" — this file never adds one.
    private static final Path STATE = DshPaths.RUNTIMES.resolve("state.json");

    private static final Object STATE_LOCK = new Object();

    /// Whether a version is disabled. A disabled runtime is skipped by
    /// `auto`/channel resolution; an explicit exact pin still resolves, so an
    /// operator who asks for it by name keeps it.
    ///
    /// @param version the exact version
    /// @return whether it is disabled
    public static boolean isDisabled(String version) {
        JsonObject state = readState();
        if (!state.has("disabled") || !state.get("disabled").isJsonArray()) {
            return false;
        }
        for (JsonElement element : state.getAsJsonArray("disabled")) {
            if (element.isJsonPrimitive() && version.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    /// Enables or disables a version. Disabling only changes resolution; it
    /// never stops a running instance.
    ///
    /// @param version  the exact version
    /// @param disabled whether to exclude it from automatic resolution
    /// @throws DshException when the state file cannot be written
    public static void setDisabled(String version, boolean disabled) throws DshException {
        synchronized (STATE_LOCK) {
            JsonObject state = readState();
            List<String> keep = new ArrayList<>();
            if (state.has("disabled") && state.get("disabled").isJsonArray()) {
                for (JsonElement element : state.getAsJsonArray("disabled")) {
                    if (element.isJsonPrimitive()) {
                        String value = element.getAsString();
                        if (!value.equals(version)) {
                            keep.add(value);
                        }
                    }
                }
            }
            if (disabled) {
                keep.add(version);
            }
            JsonArray next = new JsonArray();
            keep.forEach(next::add);
            state.add("disabled", next);
            writeState(state);
        }
    }

    /// Records that a version was just used to launch something. Best effort:
    /// a launch must not fail because its timestamp could not be written.
    ///
    /// @param version the exact version
    public static void touch(String version) {
        synchronized (STATE_LOCK) {
            try {
                JsonObject state = readState();
                JsonObject lastUsed = state.has("lastUsed") && state.get("lastUsed").isJsonObject()
                        ? state.getAsJsonObject("lastUsed")
                        : new JsonObject();
                lastUsed.addProperty(version, java.time.Instant.now().toString());
                state.add("lastUsed", lastUsed);
                writeState(state);
            } catch (DshException e) {
                LOG.warning("Could not record the last use of Node.js " + version, e);
            }
        }
    }

    /// The last time a version was recorded as used, or `null`.
    ///
    /// @param version the exact version
    /// @return an ISO timestamp, or `null`
    public static @Nullable String lastUsed(String version) {
        JsonObject state = readState();
        if (!state.has("lastUsed") || !state.get("lastUsed").isJsonObject()) {
            return null;
        }
        JsonObject lastUsed = state.getAsJsonObject("lastUsed");
        return lastUsed.has(version) && lastUsed.get(version).isJsonPrimitive()
                ? lastUsed.get(version).getAsString()
                : null;
    }

    /// Removes a version from the state file. Called by [#uninstall]; a failure
    /// is logged, not fatal (a stale entry only affects a reinstall).
    static void forgetState(String version) {
        synchronized (STATE_LOCK) {
            try {
                JsonObject state = readState();
                if (state.has("disabled") && state.get("disabled").isJsonArray()) {
                    JsonArray keep = new JsonArray();
                    for (JsonElement element : state.getAsJsonArray("disabled")) {
                        if (element.isJsonPrimitive() && !version.equals(element.getAsString())) {
                            keep.add(element);
                        }
                    }
                    state.add("disabled", keep);
                }
                if (state.has("lastUsed") && state.get("lastUsed").isJsonObject()) {
                    state.getAsJsonObject("lastUsed").remove(version);
                }
                if (state.has("digests") && state.get("digests").isJsonObject()) {
                    state.getAsJsonObject("digests").remove(version);
                }
                writeState(state);
            } catch (DshException e) {
                LOG.warning("Could not clean the state for Node.js " + version, e);
            }
        }
    }

    private static JsonObject readState() {
        if (!Files.isRegularFile(STATE)) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(STATE));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + STATE, e);
            return new JsonObject();
        }
    }

    private static void writeState(JsonObject state) throws DshException {
        try {
            Files.createDirectories(DshPaths.RUNTIMES);
            Path temp = STATE.resolveSibling(STATE.getFileName() + ".tmp");
            Files.writeString(temp, state.toString());
            try {
                Files.move(temp, STATE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, STATE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DshException("Failed to write " + STATE, e);
        }
    }

    /// Fetches the published Node releases that have a build for this platform.
    ///
    /// @param source where to read the index from
    /// @return the available releases, newest first
    /// @throws DshException when the index cannot be read
    public static List<NodeRelease> fetchReleases(NodeSource source) throws DshException {
        String platform = platformTag();
        String indexFile = indexTag(platform); 

        // The chosen source first, then the other one. A source is a host, and a host can be
        // unreachable for reasons that have nothing to do with the source being wrong — a route, a
        // blocked address family, a moment of downtime — while the other one answers in
        // milliseconds. Trying the other costs nothing when the first works, which is why the order
        // still says which one was asked for.
        List<NodeSource> candidates = new ArrayList<>();
        candidates.add(source);
        for (NodeSource other : NodeSource.values()) {
            if (other != source) {
                candidates.add(other);
            }
        }

        JsonElement parsed = null;
        List<String> failures = new ArrayList<>();
        for (NodeSource candidate : candidates) {
            String url = candidate.indexUrl();
            try {
                parsed = JsonParser.parseString(NetworkUtils.doGet(URI.create(url)));
                break;
            } catch (IOException | RuntimeException e) {
                // Which host failed is the one thing that says whether this is a route problem or
                // something about the index, so every address that was tried is reported.
                failures.add(url + " (" + e.getMessage() + ")");
            }
        }
        if (parsed == null) {
            throw new DshException("Failed to read the Node.js release index; tried "
                    + String.join(", ", failures));
        }
        if (!parsed.isJsonArray()) {
            throw new DshException("The Node.js release index had an unexpected shape");
        }

        List<NodeRelease> releases = new ArrayList<>();
        JsonArray array = parsed.getAsJsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String version = asString(object, "version");
            if (version == null || !version.startsWith("v")) {
                continue;
            }
            if (!hasPlatform(object, indexFile)) {
                continue;
            }
            String lts = null;
            JsonElement ltsElement = object.get("lts");
            if (ltsElement != null && ltsElement.isJsonPrimitive() && ltsElement.getAsJsonPrimitive().isString()) {
                lts = ltsElement.getAsString();
            }
            releases.add(new NodeRelease(version.substring(1), lts, asString(object, "date")));
        }

        releases.sort(Comparator.comparing(NodeRelease::version, DshVersionManager::compareVersions).reversed());
        return releases;
    }

    /// Installs a Node release into its own directory.
    ///
    /// The archive is downloaded and unpacked into a staging directory first, so
    /// an interrupted install can never be mistaken for a usable runtime.
    ///
    /// @param version the version to install, with or without a leading `v`
    /// @param source  where to fetch the archive from
    /// @param onStage receives a short progress description, or `null`
    /// @return the installed runtime
    /// @throws DshException when the download or extraction fails
    public static NodeRuntime install(String version, NodeSource source,
                                      @Nullable Consumer<String> onStage) throws DshException {
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        if (findInstalled(normalized) != null) {
            throw new DshException("Node.js " + normalized + " is already installed");
        }

        String platform = platformTag();
        Path target = DshPaths.runtimeDirectory(normalized);
        Path staging = target.resolveSibling(target.getFileName() + ".installing");
        String fileName = archiveFileName(normalized, platform);
        Path archive = DshPaths.RUNTIMES.resolve(fileName);

        try {
            Files.createDirectories(DshPaths.RUNTIMES);
            FileUtils.deleteQuietly(staging);
            Files.createDirectories(staging);
        } catch (IOException e) {
            throw new DshException("Failed to prepare " + staging, e);
        }

        String url = source.archiveUrl(normalized, fileName);

        try {
            stage(onStage, "Downloading " + fileName);
            download(url, archive, onStage);

            stage(onStage, "Verifying " + fileName);
            verifyChecksum(archive, normalized, fileName, source);

            stage(onStage, "Unpacking " + fileName);
            extractArchive(archive, staging);
        } catch (IOException e) {
            FileUtils.deleteQuietly(staging);
            FileUtils.deleteQuietly(archive);
            throw new DshException("Failed to install Node.js " + normalized + ": " + e.getMessage(), e);
        }

        // The archive contains a single top-level directory; move its contents up
        // so the runtime directory is the distribution root.
        Path unpacked = staging.resolve("node-v" + normalized + "-" + platform);
        if (!Files.isDirectory(unpacked)) {
            FileUtils.deleteQuietly(staging);
            throw new DshException("The archive did not contain node-v" + normalized + "-" + platform);
        }

        try {
            FileUtils.deleteQuietly(target);
            moveInto(unpacked, target);
            FileUtils.deleteQuietly(staging);
            Files.deleteIfExists(archive);
        } catch (IOException e) {
            FileUtils.deleteQuietly(staging);
            throw new DshException("Failed to move the Node runtime into " + target, e);
        }

        NodeRuntime runtime = new NodeRuntime(normalized, target);
        if (!runtime.isUsable()) {
            throw new DshException("Node.js " + normalized + " unpacked without a node executable");
        }
        // Unpacked is not the same as runnable: a binary whose shared libraries
        // are missing passes every check up to here and then fails everywhere
        // else, so the install is the last place that can say why.
        verifyRunnable(runtime);

        provisionPnpm(runtime, onStage);

        DshNodeRuntime.invalidateCache();
        LOG.info("Installed Node.js " + normalized + " into " + target);
        return runtime;
    }

    /// Installs the pnpm a managed runtime needs to run DeepSeek Harness.
    ///
    /// A Node distribution carries corepack but no `pnpm` binary, and corepack
    /// left to itself picks the newest pnpm — while DeepSeek Harness is written
    /// against pnpm 11, down to how that major reports blocked dependency
    /// scripts. The major is therefore pinned here so a profile behaves the same
    /// on a managed runtime as on the system one.
    ///
    /// A failure is reported but does not fail the install: the runtime is
    /// usable without pnpm, and the plugin installer explains what is missing.
    ///
    /// @param runtime the freshly installed runtime
    /// @param onStage receives progress lines, or `null`
    private static void provisionPnpm(NodeRuntime runtime, @Nullable Consumer<String> onStage) {
        // Resolved through the layout: `bin/npm` on POSIX, `npm.cmd` in the distribution
        // root on Windows. `isExecutable` is the wrong question there — NTFS has no
        // execute bit, so it answers yes for every file — hence the regular-file test.
        Path npm = runtime.npm();
        if (!Files.isRegularFile(npm)) {
            stage(onStage, "npm is missing, so pnpm was not installed");
            return;
        }

        stage(onStage, "Installing pnpm " + PNPM_MAJOR + ".x");
        // npm reads the registry from the environment and also takes it as an argument; both are
        // given. This is the first thing a freshly installed runtime downloads, and a wrong source
        // here is a wait that looks like a hang.
        String registry = NpmRegistry.effective().registry();
        List<String> command = List.of(npm.toString(), "install", "--global", "pnpm@" + PNPM_MAJOR,
                "--registry=" + registry);

        ProcessBuilder builder = new ProcessBuilder(
                org.jackhuang.hmcl.util.platform.Executables.startable(command));
        // A Windows `npm.cmd` finds `node.exe` through this directory; a POSIX `bin/npm`
        // is a `#!/usr/bin/env node` script that needs it for the same reason.
        builder.environment().put("PATH",
                runtime.binDirectory() + java.io.File.pathSeparator
                        + String.valueOf(System.getenv("PATH")));
        builder.environment().put("npm_config_registry", registry);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            try (var reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stage(onStage, line);
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                LOG.warning("Installing pnpm into " + runtime.directory() + " exited with " + exit);
                stage(onStage, "pnpm could not be installed (npm exited with " + exit + ")");
            }
        } catch (IOException e) {
            LOG.warning("Failed to install pnpm into " + runtime.directory(), e);
            stage(onStage, "pnpm could not be installed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /// The pnpm major DeepSeek Harness is written against.
    private static final String PNPM_MAJOR = "11";

    /// Reads the version of a Node installation in a directory.
    ///
    /// The version is asked of the binary rather than parsed from the directory
    /// name, because a directory the user chose has no naming contract.
    ///
    /// @param directory the directory holding `bin/node`
    /// @return the version, or `null` when there is no usable node there
    public static @Nullable String versionOfDirectory(Path directory) {
        return probe(directory).version();
    }

    /// What running a runtime's own binary said about it.
    ///
    /// Unpacking only checks that `bin/node` is *there*. A binary that is there
    /// and cannot start is the shape that used to look like a finished install:
    /// the files are in place, the version in the directory name is right, and
    /// every later step fails on its own terms — `exit 127` from `npm`, a line
    /// in the log nobody reads. The usual cause is a shared library the base
    /// image does not ship: Node's official builds link `libatomic.so.1`, which
    /// Debian's slim images leave out.
    ///
    /// @param version the version the binary reported, or `null` when it did not run
    /// @param detail  what the process said, kept verbatim for the failure message
    record NodeProbe(@Nullable String version, String detail) {

        /// Whether the binary answered.
        boolean runs() {
            return version != null;
        }
    }

    /// Runs `<runtime>/bin/node --version` and keeps its output.
    ///
    /// The read is bounded on purpose: a binary that floods its output because
    /// it is failing must not keep this thread filling a buffer while the
    /// process it is supposed to be timing stays blocked on a full pipe.
    static NodeProbe probe(Path directory) {
        Path node = RuntimeLayout.CURRENT.node(directory);
        if (!Files.isRegularFile(node)) {
            return new NodeProbe(null, "there is no " + node.getFileName() + " under " + directory);
        }
        Process process;
        try {
            process = new ProcessBuilder(
                    org.jackhuang.hmcl.util.platform.Executables.startableProgram(node.toString()),
                    "--version")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            return new NodeProbe(null, String.valueOf(e.getMessage()));
        }
        String output;
        try (var reader = process.inputReader()) {
            StringBuilder collected = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while (collected.length() < 4096 && (read = reader.read(buffer)) >= 0) {
                collected.append(buffer, 0, read);
            }
            output = collected.toString().strip();
        } catch (IOException e) {
            process.destroyForcibly();
            return new NodeProbe(null, String.valueOf(e.getMessage()));
        }
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new NodeProbe(null, "node --version did not answer within 10 seconds");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new NodeProbe(null, "interrupted while asking node for its version");
        }
        if (process.exitValue() != 0) {
            return new NodeProbe(null, output.isEmpty()
                    ? "node --version exited with " + process.exitValue()
                    : output);
        }
        String first = output.lines().findFirst().orElse("").strip();
        String version = first.replaceFirst("^v", "");
        return version.isEmpty() || !Character.isDigit(version.charAt(0))
                ? new NodeProbe(null, "node --version printed " + output)
                : new NodeProbe(version, output);
    }

    /// Probe results by runtime directory, keyed by the binary's own timestamp.
    private static final Map<Path, CachedProbe> PROBES = new ConcurrentHashMap<>();

    private record CachedProbe(long modified, NodeProbe probe) {
    }

    /// Whether this runtime's binary actually starts.
    ///
    /// Cached per directory and invalidated by the binary's modification time:
    /// the settings page asks on every poll, and spawning one process per poll
    /// per runtime is not a cost worth paying for a question whose answer only
    /// changes when the files do.
    ///
    /// @param runtime a managed runtime
    /// @return `false` when the binary is missing, cannot load, or does not answer
    public static boolean runs(NodeRuntime runtime) {
        Path node = RuntimeLayout.CURRENT.node(runtime.directory());
        long modified;
        try {
            modified = Files.getLastModifiedTime(node).toMillis();
        } catch (IOException e) {
            return false;
        }
        CachedProbe cached = PROBES.get(runtime.directory());
        if (cached != null && cached.modified() == modified) {
            return cached.probe().runs();
        }
        NodeProbe fresh = probe(runtime.directory());
        PROBES.put(runtime.directory(), new CachedProbe(modified, fresh));
        return fresh.runs();
    }

    /// Refuses a runtime whose binary does not run, removing it.
    ///
    /// Called once the archive is unpacked and before anything is installed into
    /// it: a snapshot that cannot execute is not a runtime, and leaving it in
    /// the list is what made "task done" and "every install exits 127" coexist.
    ///
    /// @param runtime the freshly unpacked runtime
    /// @throws DshException naming the binary's own words
    static void verifyRunnable(NodeRuntime runtime) throws DshException {
        NodeProbe probe = probe(runtime.directory());
        if (probe.runs()) {
            return;
        }
        FileUtils.deleteQuietly(runtime.directory());
        PROBES.remove(runtime.directory());
        throw new DshException("Node.js " + runtime.version() + " unpacked but does not run: "
                + probe.detail()
                + " — the base image is probably missing a shared library Node.js needs"
                + " (libatomic1 on Debian-based images)");
    }

    /// Adopts a Node installation the user already has.
    ///
    /// The directory is copied into the managed runtimes rather than referenced
    /// in place, so an instance pinned to it does not depend on where the
    /// installation happened to live — the same reasoning behind HMCL copying a
    /// chosen Java home into its own store.
    ///
    /// @param source  the directory holding `bin/node`
    /// @param version the version the directory reports
    /// @return the managed runtime
    /// @throws DshException when the directory is unusable or already managed
    public static NodeRuntime adopt(Path source, String version) throws DshException {
        if (findInstalled(version) != null) {
            throw new DshException("Node.js " + version + " is already managed");
        }

        Path target = DshPaths.runtimeDirectory(version);
        Path staging = target.resolveSibling(target.getFileName() + ".adopting");
        try {
            FileUtils.deleteQuietly(staging);
            copyTree(source, staging);
            FileUtils.deleteQuietly(target);
            moveInto(staging, target);
        } catch (IOException e) {
            FileUtils.deleteQuietly(staging);
            throw new DshException("Failed to adopt " + source + ": " + e.getMessage(), e);
        }

        NodeRuntime runtime = new NodeRuntime(version, target);
        if (!runtime.isUsable()) {
            throw new DshException("The chosen directory does not contain a usable Node.js installation");
        }
        // The copy is what gets run from now on, so the copy is what has to run:
        // a tree whose links did not survive the move answers here, not on the
        // first install someone starts.
        verifyRunnable(runtime);
        provisionPnpm(runtime, null);
        return runtime;
    }

    /// Copies a directory tree.
    ///
    /// @param source      the directory to copy
    /// @param destination the destination
    /// @throws IOException when the copy fails
    private static void copyTree(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else if (Files.isSymbolicLink(path)) {
                    // Preserve links: a Node distribution links npm and npx into
                    // lib/node_modules, and copying the targets would break them.
                    // `Links` keeps the link where it can and copies the file where it
                    // cannot, which is what makes adopting a tree work on Windows.
                    Links.createOrCopy(target, Files.readSymbolicLink(path));
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    /// Removes an installed Node runtime.
    ///
    /// @param version the version to remove
    /// @throws DshException when the runtime is not installed or cannot be removed
    public static void uninstall(String version) throws DshException {
        DshNodeRuntime.invalidateCache();
        NodeRuntime runtime = findInstalled(version);
        if (runtime == null) {
            throw new DshException("Node.js " + version + " is not installed");
        }
        try {
            FileUtils.deleteDirectory(runtime.directory());
        } catch (IOException e) {
            throw new DshException("Failed to remove " + runtime.directory(), e);
        }
        PROBES.remove(runtime.directory());
        forgetState(version);
        NodeChannels.forget(version);
        LOG.info("Removed Node.js " + version);
    }

    /// Verifies an archive against the release's `SHASUMS256.txt` and records its
    /// digest either way.
    ///
    /// A mirror that does not publish the list is not a reason to refuse the
    /// install — npmmirror does not — but the digest is still computed and stored
    /// with `verified: false`, so the Node page can say which installs were
    /// checked against a published hash. A **mismatch** is fatal: the archive is
    /// deleted and the install fails before anything is unpacked.
    ///
    /// @param archive  the downloaded archive
    /// @param version  the version
    /// @param fileName the archive's name in the checksum list
    /// @param source   where it was downloaded from
    /// @throws IOException when the digest cannot be read or does not match
    static void verifyChecksum(Path archive, String version, String fileName, NodeSource source) throws IOException {
        String digest = sha256(archive);
        String expected = publishedChecksum(version, fileName, source);
        if (expected == null) {
            LOG.warning("No SHASUMS256 entry for " + fileName + " on " + source.host()
                    + " or the other source; installing it unverified");
            recordDigest(version, digest, false);
            return;
        }
        if (!expected.equalsIgnoreCase(digest)) {
            throw new IOException("checksum mismatch for " + fileName + ": expected "
                    + expected + ", got " + digest);
        }
        recordDigest(version, digest, true);
    }

    /// Reads the digest `fileName` should have. The chosen source is asked first,
    /// then the other: a mirror that omits the list still gets verified against
    /// the official one when that host is reachable.
    ///
    /// @param version  the version
    /// @param fileName the archive's name
    /// @param source   the chosen source
    /// @return the published hash, or `null` when neither source has it
    static @Nullable String publishedChecksum(String version, String fileName, NodeSource source) {
        List<NodeSource> candidates = new ArrayList<>();
        candidates.add(source);
        for (NodeSource other : NodeSource.values()) {
            if (other != source) {
                candidates.add(other);
            }
        }
        for (NodeSource candidate : candidates) {
            try {
                HttpURLConnection connection = NetworkUtils.createHttpConnection(
                        candidate.checksumsUrl(version));
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                if (connection.getResponseCode() / 100 != 2) {
                    continue;
                }
                try (InputStream in = connection.getInputStream()) {
                    String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    String found = checksumFrom(text, fileName);
                    if (found != null) {
                        return found;
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.debug("Could not read " + candidate.checksumsUrl(version) + ": " + e.getMessage());
            }
        }
        return null;
    }

    /// The hash a `SHASUMS256.txt` names for `fileName`, or `null`.
    ///
    /// @param text     the checksum file's contents
    /// @param fileName the archive's name
    /// @return the hash, or `null` when the list does not name it
    static @Nullable String checksumFrom(String text, String fileName) {
        for (String line : text.split("\\R")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2 && parts[1].replaceFirst("^\\*", "").equals(fileName)) {
                return parts[0];
            }
        }
        return null;
    }

    /// The SHA-256 of a file, lowercase hex.
    ///
    /// @param file the file
    /// @return the digest
    /// @throws IOException when the file or the algorithm cannot be read
    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    private static void recordDigest(String version, String digest, boolean verified) {
        synchronized (STATE_LOCK) {
            try {
                JsonObject state = readState();
                JsonObject digests = state.has("digests") && state.get("digests").isJsonObject()
                        ? state.getAsJsonObject("digests") : new JsonObject();
                JsonObject entry = new JsonObject();
                entry.addProperty("sha256", digest);
                entry.addProperty("verified", verified);
                digests.add(version, entry);
                state.add("digests", digests);
                writeState(state);
            } catch (DshException e) {
                LOG.warning("Could not record the digest of Node.js " + version, e);
            }
        }
    }

    /// The recorded SHA-256 of a version, or `null`.
    ///
    /// @param version the exact version
    /// @return the digest
    public static @Nullable String digest(String version) {
        JsonObject state = readState();
        if (!state.has("digests") || !state.get("digests").isJsonObject()) {
            return null;
        }
        JsonElement element = state.getAsJsonObject("digests").get(version);
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject entry = element.getAsJsonObject();
        return entry.has("sha256") && entry.get("sha256").isJsonPrimitive()
                ? entry.get("sha256").getAsString()
                : null;
    }

    /// Whether a version's digest matched a published checksum.
    ///
    /// @param version the exact version
    /// @return whether it was verified
    public static boolean digestVerified(String version) {
        JsonObject state = readState();
        if (!state.has("digests") || !state.get("digests").isJsonObject()) {
            return false;
        }
        JsonElement element = state.getAsJsonObject("digests").get(version);
        if (element == null || !element.isJsonObject()) {
            return false;
        }
        JsonObject entry = element.getAsJsonObject();
        return entry.has("verified") && entry.get("verified").isJsonPrimitive()
                && entry.get("verified").getAsBoolean();
    }

    /// Streams a URL to a file, reporting progress.
    ///
    /// @param url      the download URL
    /// @param target   the destination file
    /// @param onStage  receives progress descriptions, or `null`
    /// @throws IOException when the transfer fails
    private static void download(String url, Path target, @Nullable Consumer<String> onStage) throws IOException {
        HttpURLConnection connection = NetworkUtils.createHttpConnection(url);
        connection.setInstanceFollowRedirects(true);
        try {
            connection.connect();
            int status = connection.getResponseCode();
            if (status / 100 != 2) {
                throw new IOException("HTTP " + status + " for " + url);
            }
            long total = connection.getContentLengthLong();
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[1 << 16];
                long copied = 0;
                int read;
                int lastReported = -1;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                    copied += read;
                    if (onStage != null && total > 0) {
                        int percent = (int) (copied * 100 / total);
                        if (percent / 10 != lastReported / 10) {
                            lastReported = percent;
                            onStage.accept("Downloading " + percent + "%");
                        }
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    /// Unpacks a Node distribution archive into a directory, preserving symlinks.
    ///
    /// Linux builds ship as `.tar.xz`, macOS builds as `.tar.gz` and both carry the
    /// same top-level layout, including the symlinks that make `bin/npm` and `bin/npx`
    /// work. Windows builds ship as `.zip`, which has neither.
    ///
    /// Package-visible rather than private so a test can drive it with an archive it built itself:
    /// what this refuses to write — a link pointing out of the tree, an entry naming a path above
    /// the target, and an entry reached through a directory that is a link out of it — is the part
    /// that has no other way in, since [install] downloads its archive.
    ///
    /// @param archive the `.zip`, `.tar.xz` or `.tar.gz` file
    /// @param target  the directory to unpack into
    /// @throws IOException when decompression or extraction fails
    static void extractArchive(Path archive, Path target) throws IOException {
        String name = archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip")) {
            extractZip(archive, target);
            return;
        }
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            try (TarFileTree tree = TarFileTree.open(archive)) {
                extract(tree, tree.getRoot(), target, target);
            }
            return;
        }
        extractTarXz(archive, target);
    }

    /// Unpacks a `.zip` archive into a directory.
    ///
    /// The Windows distribution is a zip, and a zip carries no symlinks — `npm.cmd` and
    /// `npx.cmd` are real files there — so extraction needs neither the link handling
    /// the tarballs do nor the privilege that recreating a link requires. The single
    /// top-level directory is **kept**, because [install] moves it up by name after the
    /// fact; stripping it here would break that step rather than shorten it.
    ///
    /// @param archive the `.zip` file
    /// @param target  the directory to unpack into
    /// @throws IOException when decompression or extraction fails
    private static void extractZip(Path archive, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(
                new java.io.BufferedInputStream(Files.newInputStream(archive)))) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                // A zip entry may name `../..`; one that lands outside the target is not
                // this archive's to write, whatever it says. A link is the second way out
                // and is not written here — a zip carries none — but a target that already
                // holds one is found rather than made, so the question is asked of the
                // filesystem too rather than only of the name.
                Path destination = SafePaths.resolveInside(root, name);
                if (destination == null) {
                    throw new IOException("the archive names a path outside its own root: " + name);
                }
                if (entry.isDirectory()) {
                    SafePaths.createDirectories(root, destination);
                    continue;
                }
                Path parent = destination.getParent();
                if (parent == null) {
                    throw new IOException("the archive names a path outside its own root: " + name);
                }
                SafePaths.createDirectories(root, parent);
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /// Unpacks a `.tar.xz` archive into a directory, preserving symlinks.
    ///
    /// Recreating symlinks matters: the Node distribution reaches `npm` and
    /// `npx` through links into `lib/node_modules`, so a naive extraction that
    /// materialised them as copies would leave a subtly broken runtime.
    ///
    /// @param archive the `.tar.xz` file
    /// @param target  the directory to unpack into
    /// @throws IOException when decompression or extraction fails
    private static void extractTarXz(Path archive, Path target) throws IOException {
        Path tarFile = Files.createTempFile("hdsl-node", ".tar");
        try {
            try (InputStream in = Files.newInputStream(archive);
                 XZInputStream xz = new XZInputStream(in)) {
                Files.copy(xz, tarFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try (TarFileTree tree = TarFileTree.open(tarFile)) {
                extract(tree, tree.getRoot(), target, target);
            }
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    /// Recursively unpacks one directory of an archive tree.
    ///
    /// @param tree   the archive being read
    /// @param dir    the directory to unpack
    /// @param target the destination directory
    /// @param root   the directory the whole archive is unpacked into
    /// @param <R>    the reader type
    /// @param <E>    the entry type
    /// @throws IOException when an entry cannot be written
    private static <R, E extends ArchiveEntry> void extract(ArchiveFileTree<R, E> tree,
                                                            ArchiveFileTree.Dir<E> dir,
                                                            Path target,
                                                            Path root) throws IOException {
        for (var entry : dir.getFiles().entrySet()) {
            Path destination = target.resolve(entry.getKey());
            E archiveEntry = entry.getValue();
            if (tree.isLink(archiveEntry)) {
                // The distribution reaches `npm` and `npx` through links into
                // `lib/node_modules`. Where the system permits a link this is that link;
                // where it does not, `Links` copies the file it points at — the
                // difference between a runtime that works and an install that fails on
                // its last step.
                //
                // A link is recreated only when it stays inside what is being unpacked: the
                // target travels in the archive, so `node-v22/bin/npm -> /etc/passwd` is a
                // link this launcher would otherwise create and then read through, and it
                // is the archive that says where it points. One that leaves is not
                // recreated at all — the entry is written as the plain file it is instead.
                //
                // The order matters, and it is why the check comes before `Links`: asked
                // first, `Links` would copy the *content* of a target that leaves the
                // archive into the tree, which is this defect read backwards.
                Path link = Path.of(tree.getLink(archiveEntry));
                if (!link.isAbsolute() && SafePaths.resolveInside(root, target.resolve(link)) != null) {
                    // The parent is made before `Links` is asked to make the link, so that where the
                    // link is *written* is judged too: the target's answer above is about the link's
                    // destination, and a directory already standing between them as a link out of the
                    // tree is the same defect approached from the other side.
                    SafePaths.createDirectories(root, destination.getParent());
                    Links.createOrCopy(destination, link);
                } else {
                    LOG.warning("Not unpacking the link " + destination + " -> " + link
                            + ": it points outside " + root);
                    SafePaths.createDirectories(root, destination.getParent());
                    tree.extractTo(archiveEntry, destination);
                }
                continue;
            }
            SafePaths.createDirectories(root, destination.getParent());
            tree.extractTo(archiveEntry, destination);
            if (tree.isExecutable(archiveEntry)) {
                destination.toFile().setExecutable(true, false);
            }
        }
        for (var subDirectory : dir.getSubDirs().entrySet()) {
            Path destination = target.resolve(subDirectory.getKey());
            SafePaths.createDirectories(root, destination);
            extract(tree, subDirectory.getValue(), destination, root);
        }
    }

    /// Reports a progress stage when a consumer is attached.
    ///
    /// @param onStage the consumer, or `null`
    /// @param message the message
    private static void stage(@Nullable Consumer<String> onStage, String message) {
        if (onStage != null) {
            onStage.accept(message);
        }
    }

    /// Reads a string field from a JSON object.
    ///
    /// @param object the object
    /// @param name   the field name
    /// @return the string value, or an empty string when absent
    private static String asString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    /// Reports whether a release publishes a build for a platform.
    ///
    /// @param object   the release object
    /// @param platform the index entry, e.g. `linux-x64` or `osx-arm64-tar`
    /// @return whether the `files` array contains the entry
    private static boolean hasPlatform(JsonObject object, String platform) {
        JsonElement files = object.get("files");
        if (files == null || !files.isJsonArray()) {
            return false;
        }
        for (JsonElement file : files.getAsJsonArray()) {
            if (file.isJsonPrimitive() && platform.equalsIgnoreCase(file.getAsString())) {
                return true;
            }
        }
        return false;
    }

    /// Moves a directory into place, atomically where the system allows it.
    ///
    /// `ATOMIC_MOVE` is a request, not a guarantee: Windows refuses it outright when the
    /// two paths are on different volumes, and a data directory on a second drive is an
    /// ordinary way to run the panel. What the caller needs is that the *staging*
    /// directory was complete before anything was published — not that the rename
    /// itself was indivisible — so the plain move is the right answer when the atomic
    /// one is not available.
    ///
    /// @param source the completed directory
    /// @param target where it belongs
    /// @throws IOException when neither move works
    private static void moveInto(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }
}
