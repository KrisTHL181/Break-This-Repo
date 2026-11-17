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
package org.jackhuang.hmcl.web.zcode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.NodeRuntimeManager;
import org.jackhuang.hmcl.dsh.NodeSource;
import org.jackhuang.hmcl.util.io.Links;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Builds ZCode distributions inside the panel's data directory.
///
/// There is no official ZCode CLI distribution to download (the desktop apps
/// are the only release artifacts), so the category builds its own: this class
/// downloads the source tarball for a tag or branch, applies [ZcodePatch] so the
/// web client works behind the panel's `/i/<id>/` proxy, runs `pnpm install` and
/// `pnpm build:zcode`, verifies the `sha256` the build recorded, and installs
/// the result as `releases/<version>/` with `current` pointing at it.
///
/// It is a long, heavy job — a full monorepo install plus three builds, minutes
/// to tens of minutes and gigabytes of scratch space — so it runs on its own
/// thread with one build at a time, reports through [Status], and writes every
/// line of the child processes' output to `build/build.log` (the interface tails
/// it). The scratch tree is removed when the build ends, unless
/// `keepSources` says otherwise.
///
/// Everything is in-memory apart from the releases and the log: a panel restart
/// mid-build loses the build, which is accepted for an experimental category.
@NotNullByDefault
public final class ZcodeBuilder {

    /// What the panel can be told about the builder.
    public enum State {
        IDLE, RUNNING, DONE, ERROR
    }

    /// The builder's state, the version it is working on, its current step, how
    /// far along that is, and — for `error` — why it stopped.
    public record Status(State state, String version, String message, double fraction, @Nullable String error) {

        static final Status IDLE = new Status(State.IDLE, "", "", 0, null);
    }

    /// One installed release, as the interface lists it.
    public record Release(String version, String path, long builtAt, boolean current) {
    }

    /// Everything the builder needs that is not the data directory.
    ///
    /// @param root        `<dataDir>/zcode`
    /// @param buildBin    a directory prepended to `PATH` for the build — the
    ///                    Node 24 install (`/opt/node24/bin` in the image);
    ///                    empty keeps the panel's own `PATH`
    /// @param pnpm        the pnpm executable to run (tests point it at a stub)
    /// @param sourceUrl   a URL or path template for the source tarball; `%s`
    ///                    is the version (or branch) name
    /// @param keepSources whether to keep the scratch tree after the build
    public record Settings(Path root, String buildBin, String pnpm, String sourceUrl, boolean keepSources) {
    }

    private static final int LOG_TAIL_LINES = 400;

    private static final AtomicReference<Status> STATUS = new AtomicReference<>(Status.IDLE);
    private static final Object LOCK = new Object();
    private static volatile @Nullable Path logFile;

    private ZcodeBuilder() {
    }

    /// The build's state right now.
    public static Status status() {
        return STATUS.get();
    }

    /// Starts a build, unless one is already running.
    ///
    /// @param settings where to build and with what
    /// @param version  the upstream tag (`v3.14.3`) or branch (`main`) to build
    /// @return the state the build just entered
    /// @throws ZcodeException when the version is not a plausible ref name or a
    /// build is already running
    public static Status start(Settings settings, String version) throws ZcodeException {
        // The monorepo's `packages/desktop` postinstall downloads an Electron
        // runtime, and Electron publishes no FreeBSD build — the install step of
        // the build ends there. Refusing up front is better than a ten-minute
        // build that dies in a postinstall script.
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.FREEBSD) {
            throw new ZcodeException("ZCode 构建会安装 Electron 桌面依赖，Electron 没有 FreeBSD 构建；"
                    + "这个实验性品类在 FreeBSD 上暂不支持");
        }
        String ref = version == null ? "" : version.trim();
        if (ref.isEmpty() || ref.length() > 64 || !ref.matches("[A-Za-z0-9._/-]+")) {
            throw new ZcodeException("version must be an upstream tag or branch name (letters, digits, . _ - /)");
        }
        synchronized (LOCK) {
            if (STATUS.get().state() == State.RUNNING) {
                throw new ZcodeException("a ZCode build is already running");
            }
            STATUS.set(new Status(State.RUNNING, ref, "准备构建目录", 0.01, null));
            Thread thread = new Thread(() -> run(settings, ref), "zcode-build");
            thread.setDaemon(true);
            thread.start();
            return STATUS.get();
        }
    }

    /// The tail of the build log, for the interface.
    ///
    /// @param lines how many trailing lines to return
    /// @return the lines, empty when no build has run yet
    public static List<String> tailLog(int lines) {
        Path file = logFile;
        if (file == null || !Files.isRegularFile(file)) {
            return List.of();
        }
        try (var stream = Files.lines(file, StandardCharsets.UTF_8)) {
            List<String> all = stream.toList();
            return all.subList(Math.max(0, all.size() - Math.max(1, lines)), all.size());
        } catch (IOException e) {
            LOG.warning("Could not read the ZCode build log " + file, e);
            return List.of();
        }
    }

    /// The installed releases, newest first, each flagged with whether the
    /// `current` link resolves to it.
    public static List<Release> releases(Path root) {
        Path releases = root.resolve("releases");
        Path currentLink = root.resolve("current");
        List<Release> found = new ArrayList<>();
        try (var stream = Files.list(releases)) {
            for (Path dir : stream.filter(Files::isDirectory).toList()) {
                if (!Files.isRegularFile(dir.resolve("bin/zcode.mjs"))) {
                    continue;
                }
                found.add(new Release(dir.getFileName().toString(), dir.toString(),
                        dir.toFile().lastModified(), samePath(dir, currentLink)));
            }
        } catch (IOException e) {
            return List.of();
        }
        found.sort(Comparator.comparingLong(Release::builtAt).reversed());
        return List.copyOf(found);
    }

    /// Whether the `current` pointer points at `release`.
    ///
    /// Read through [Links] rather than by walking the path: where symbolic links are not
    /// permitted — an ordinary Windows account without Developer Mode — the pointer is a
    /// small text file naming the release, and `toRealPath()` on that resolves the *file*,
    /// never the release. Every release would then report itself as not current, which is
    /// what the installed-versions list is built from.
    ///
    /// @param release the release directory
    /// @param pointer the `current` pointer
    /// @return whether the pointer names that release
    private static boolean samePath(Path release, Path pointer) {
        return Links.pointedAt(pointer).map(target -> {
            try {
                // `isSameFile` asks the filesystem, which is exactly what Windows needs:
                // the same directory can be spelled with different case, a short (8.3)
                // name, or a `\\?\` prefix, and `toRealPath().equals` compares those
                // spellings rather than the files they name.
                return Files.isSameFile(release, target);
            } catch (IOException e) {
                return false;
            }
        }).orElse(false);
    }

    /// The package directory to run: what `current` points at when that is a package, else
    /// the newest installed release, else `current` itself (which will not exist, so the
    /// interface shows "not installed").
    public static Path currentPackage(Path root) {
        Path current = root.resolve("current");
        // The pointer is read rather than walked through. A symlink can be either, so
        // walking it worked here and would keep working; a pointer file can only be read,
        // and mixing the two is how one platform's install becomes invisible to another's.
        Path pointed = Links.pointedAt(current).orElse(null);
        if (pointed != null && Files.isRegularFile(pointed.resolve("bin/zcode.mjs"))) {
            return pointed;
        }
        List<Release> installed = releases(root);
        if (!installed.isEmpty()) {
            return Path.of(installed.get(0).path());
        }
        return current;
    }

    // ------------------------------------------------------------------ build --

    private static void run(Settings settings, String ref) {
        Path work = settings.root().resolve("build");
        Path sources = work.resolve("src");
        Path tarball = work.resolve("source.tar.gz");
        Path outDir = work.resolve("out");
        Path log = work.resolve("build.log");
        logFile = log;
        String installed = null;
        try {
            Files.createDirectories(work);
            Files.writeString(log, "", StandardCharsets.UTF_8);
            prepareShims(settings);
            deleteRecursively(sources);
            deleteRecursively(outDir);

            Step step = new Step(log, ref);
            // Resolve (and if needed install) the build toolchain before the long
            // download: a bare deployment with no Node 24 and no pnpm would otherwise
            // fail an hour in, at the step that runs pnpm.
            Settings effective = withToolchain(settings, step);
            reportCompiler(step);
            step.set(0.03, "清理旧目录");
            deleteRecursively(settings.root().resolve("current"));

            String url = String.format(effective.sourceUrl(), ref);
            step.set(0.06, "下载源码：" + url);
            download(url, tarball);

            step.set(0.15, "解包源码");
            Files.createDirectories(sources);
            step.log("=== 解包（面板自带的 tar 读取器：系统 tar 会按 ANSI 代码页解码 UTF-8 文件名）");
            unpack(tarball, sources);

            step.set(0.22, "应用反代补丁（" + ZcodePatch.size() + " 处）");
            ZcodePatch.apply(sources);

            // The registry is the one chosen in the panel's settings, which is what the settings
            // page shows and what `NPM_CONFIG_REGISTRY` falls back to. Passing it explicitly makes
            // the build independent of pnpm's own config.yaml, and is the one lever that matters on
            // a network far from npmjs.org: measured from this container, the published registry
            // answers at 28 KB/s and a mirror at 25 MB/s — the difference between an install and an
            // afternoon.
            //
            // There was a second knob here, an `.npmrc` narrowing the install to this machine's
            // platform. It is gone: pnpm takes `supportedArchitectures` from `pnpm-workspace.yaml`
            // rather than from `.npmrc`, so it never took effect, and if it had it would have broken
            // the build — the SEA staging step collects native assets for all six targets and fails
            // outright when one is missing. Upstream's own `pnpm-workspace.yaml` already asks for
            // exactly the platforms it needs.
            String registry = org.jackhuang.hmcl.dsh.NpmRegistry.effective().registry();
            List<String> install = new ArrayList<>(List.of(effective.pnpm(), "install"));
            install.add("--registry=" + registry);
            // Windows: pnpm's isolated layout does not link a package's own dependencies
            // beside it when that package ships a `node_modules` directory, and the tools
            // this panel installs are written against a flat layout — on Windows 10 with
            // pnpm 11.28.6 the install passes while the program then cannot resolve `ws`
            // (`ERR_MODULE_NOT_FOUND`). Hoisted is the layout they expect; POSIX keeps
            // the isolated one it has always used.
            if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                    == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
                install.add("--config.node-linker=hoisted");
            }
            step.set(0.28, "安装依赖：pnpm install（最慢的一步，registry=" + registry + "）");
            run(step, effective, sources, install, Duration.ofMinutes(90));

            // Upstream's own build does not produce this, and its error message says to run
            // `pnpm build`, which does not either: `packages/shared` declares no build script, so
            // `pnpm -r build` skips it, while the SEA staging step later insists on its
            // `dist/index.js` and stops the whole build there. What does produce it is the project
            // reference build — the one the root `typecheck` runs — which is why anyone who has run
            // that has the file and a clean checkout does not.
            step.set(0.62, "补 packages/shared 的 dist（上游缺的一步）");
            run(step, effective, sources, List.of(effective.pnpm(), "exec", "tsc", "-b", "packages/shared"),
                    Duration.ofMinutes(30));
            if (!Files.isRegularFile(sources.resolve("packages/shared/dist/index.js"))) {
                throw new ZcodeException("packages/shared/dist/index.js was not produced,"
                        + " and the rest of the build cannot run without it");
            }

            step.set(0.68, "构建发行包：pnpm build:zcode");
            run(step, effective, sources, List.of(effective.pnpm(), "build:zcode",
                    "--out-dir", outDir.toString(),
                    "--base-url", "https://example.invalid/zcode/"), Duration.ofMinutes(90));

            step.set(0.93, "安装发行包");
            installed = install(outDir, settings.root().resolve("releases"), ref);

            // DONE is published by the finally block, after the scratch tree is gone:
            // a caller that sees DONE reads the directory immediately, and reporting
            // success while the cleanup is still deleting files is a race (the tests
            // caught it on one JVM and not another).
            step.set(0.99, "清理构建目录");
            LOG.info("ZCode " + ref + " built and installed as " + installed);
        } catch (ZcodeException e) {
            fail(ref, e.getMessage());
        } catch (Exception e) {
            fail(ref, e.toString());
            LOG.warning("The ZCode build of " + ref + " failed", e);
        } finally {
            // Kept when the build failed, whatever the setting says. The tree is the only record of
            // why it failed — upstream's own scripts answer with their own logs, and deleting them
            // costs a whole rebuild to ask the same question twice. `HDSL_ZCODE_KEEP_SOURCES=1`
            // keeps it even on success.
            boolean built = installed != null;
            if (!settings.keepSources() && built) {
                try {
                    deleteRecursively(sources);
                    Files.deleteIfExists(tarball);
                    deleteRecursively(outDir);
                } catch (IOException e) {
                    LOG.warning("Could not clean up the ZCode build tree", e);
                }
            } else if (!built) {
                // Named because the next question is always "where did it get to".
                LOG.info("The failed ZCode build is kept at " + work);
            }
            if (built) {
                STATUS.set(new Status(State.DONE, ref, "构建完成：" + installed, 1.0, null));
            }
        }
    }

    private static void fail(String ref, @Nullable String reason) {
        String message = reason == null ? "构建失败" : reason;
        STATUS.set(new Status(State.ERROR, ref, message, 0, message));
        LOG.warning("The ZCode build of " + ref + " failed: " + message);
    }

    /// Downloads the source tarball — over HTTP(S), or copied from a local path
    /// or `file:` URL, which is how tests and offline mirrors supply it.
    private static void download(String url, Path target) throws ZcodeException {
        try {
            if (url.startsWith("file:")) {
                Files.copy(Path.of(URI.create(url)), target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            if (!url.contains("://")) {
                Files.copy(Path.of(url), target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(60))
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() / 100 != 2) {
                throw new ZcodeException("downloading " + url + " returned HTTP " + response.statusCode()
                        + " — check the tag or branch name");
            }
        } catch (IOException e) {
            throw new ZcodeException("could not download the ZCode source from " + url
                    + explainIfCertificate(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZcodeException("the source download was interrupted", e);
        }
    }

    /// The half-sentence that turns "download failed" into something a person can
    /// act on when the failure is a certificate one.
    ///
    /// A TLS-inspecting proxy makes every download from GitHub fail here, and the
    /// bare URL nobody can read is what the report said before.
    private static String explainIfCertificate(IOException error) {
        if (!org.jackhuang.hmcl.util.platform.OutboundTls.isTrustFailure(error)) {
            return "";
        }
        org.jackhuang.hmcl.util.platform.OutboundTls.recordTrouble("codeload.github.com", error);
        return "：TLS 证书没通过校验（" + error.getMessage() + "）。这台机器上可能有反向代理、抓包工具"
                + "或 GitHub 加速器在转发 HTTPS，它的根证书不在 Java 的信任库里；确认这是你自己的配置后，"
                + "在「设置 → 网络」里授权一次（一次性、最长 24 小时），否则请检查网络。";
    }

    /// Installs the built distribution: reads the `latest.json` the build wrote,
    /// verifies the recorded `sha256`, unpacks the tarball into
    /// `releases/<version>/` and points `current` at it.
    ///
    /// @return the installed version
    private static String install(Path outDir, Path releases, String ref) throws ZcodeException {
        Path latest = outDir.resolve("latest.json");
        String version;
        Path tarball;
        String expectedSha;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(latest, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            version = json.get("version").getAsString();
            expectedSha = json.has("sha256") ? json.get("sha256").getAsString() : "";
            tarball = outDir.resolve("releases").resolve(version).resolve(json.get("tarball").getAsString());
        } catch (Exception e) {
            throw new ZcodeException("the build did not produce a readable latest.json in " + outDir, e);
        }
        if (!Files.isRegularFile(tarball)) {
            Path releasesDir = outDir.resolve("releases").resolve(version);
            try (var stream = Files.list(releasesDir)) {
                tarball = stream.filter(p -> p.getFileName().toString().endsWith(".tar.gz")).findFirst()
                        .orElseThrow(() -> new ZcodeException("no release tarball below " + releasesDir));
            } catch (IOException e) {
                throw new ZcodeException("no release tarball for " + version + " below " + releasesDir, e);
            }
        }
        if (!expectedSha.isEmpty() && !sha256(tarball).equalsIgnoreCase(expectedSha)) {
            throw new ZcodeException("the built tarball's sha256 does not match latest.json — refusing to install");
        }

        Path target = releases.resolve(version);
        try {
            deleteRecursively(target);
            Files.createDirectories(target);
            unpack(tarball, target);
            if (!Files.isRegularFile(target.resolve("bin/zcode.mjs"))) {
                throw new ZcodeException("the built tarball has no bin/zcode.mjs — unexpected layout");
            }
            // The distro patch (see ZcodeServerPatch): upstream's standalone server
            // hard-codes a project-store owner no client can match, and upstream does
            // not take contributions, so the panel ships the correction as part of the
            // release it builds. Applied here, before `current` moves, so every release
            // in `releases/` is the same bytes; a moved anchor fails the build rather
            // than shipping a workbench that cannot open a folder.
            switch (ZcodeServerPatch.patch(target)) {
                case PATCHED, PATCHED_ALREADY, NOT_APPLICABLE, NOT_A_SERVER -> {
                    // NOT_APPLICABLE means the operator set HDSL_ZCODE_KEEP_REMOTE: that
                    // deployment uses the ZCode desktop application as a host, so
                    // upstream's remote owner is the one it needs and nothing is changed.
                    //
                    // NOT_A_SERVER is a build that produced no web half. It is not this
                    // build's job to insist on one: a TUI-only distribution is a release
                    // the panel can install and run, it simply has no workbench to patch.
                }
                case ANCHOR_MISSING -> throw new ZcodeException(
                        "the built ZCode server entry no longer carries the project-owner expression"
                                + " this panel patches (" + ZcodeServerPatch.SERVER_ENTRY + ");"
                                + " the web workbench would have no workspace — update ZcodeServerPatch"
                                + " and docs/zcode-experimental.md to the new anchor");
                case NEEDS_PATCH, UNREADABLE -> throw new ZcodeException(
                        "could not apply the project-owner patch to the built release ("
                                + target + ")");
            }
            // The version catalog needs to know which upstream ref produced this
            // build. `releases/<version>` is named after the package version, so a
            // `main` build would otherwise lose the fact that it came from main.
            JsonObject manifest = new JsonObject();
            manifest.addProperty("ref", ref);
            manifest.addProperty("version", version);
            manifest.addProperty("builtAt", System.currentTimeMillis());
            manifest.addProperty("credentialFormat", ZcodeCatalog.formatOf(ref));
            Files.writeString(target.resolve("release.json"), manifest.toString(),
                    StandardCharsets.UTF_8);
            // A pointer at the release just installed. Where the system permits a link
            // this is a link; where it does not — an ordinary Windows account without
            // Developer Mode — `Links` writes a pointer file instead, because the
            // alternative is to fail here, after the pnpm install and build have already
            // run, over the last step of the whole job.
            Links.pointAt(releases.getParent().resolve("current"), target);
        } catch (IOException e) {
            throw new ZcodeException("could not install the built ZCode distribution", e);
        }
        return version;
    }

    private static String sha256(Path file) throws ZcodeException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1 << 16];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new ZcodeException("could not hash " + file, e);
        }
    }

    /// Unpacks a `.tar.gz` into `target`, dropping the archive's first path component.
    ///
    /// Java, not the system's `tar`. On Windows that is libarchive (`System32	ar.exe`),
    /// which converts entry names through the ANSI code page; a GitHub source tarball
    /// stores them as UTF-8 — ZCode ships `docs/ZCode 项目架构分析与改进建议.md` — and on a
    /// GBK machine the conversion leaves libarchive with an entry it cannot name, so the
    /// whole extraction fails with `Invalid empty pathname` *after* writing most of the
    /// tree. The panel already reads tar through kala-compress, with the charset stated
    /// rather than guessed ([ArchiveFileTree]), so no external program is involved on
    /// either platform.
    ///
    /// `.tar.gz` is streamed: the archive is 90 MB compressed and several hundred
    /// unpacked, which is not something to stage in a temp file first.
    ///
    /// @param tarball the archive
    /// @param target  the directory to unpack into; created when missing
    /// @throws IOException when the archive cannot be read or written
    static void unpack(Path tarball, Path target) throws IOException {
        Files.createDirectories(target);
        Path root = target.toRealPath();
        try (java.io.InputStream file = Files.newInputStream(tarball);
             java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(file);
             kala.compress.archivers.tar.TarArchiveInputStream tar =
                     new kala.compress.archivers.tar.TarArchiveInputStream(gzip, StandardCharsets.UTF_8)) {
            kala.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                String name = entry.getName();
                // `--strip-components=1`: the archive's own top-level directory is dropped.
                int slash = name.indexOf('/');
                if (slash < 0 || slash == name.length() - 1) {
                    continue;
                }
                String relative = name.substring(slash + 1);
                Path destination = root.resolve(relative).normalize();
                if (!destination.startsWith(root)) {
                    throw new IOException("the archive holds a name that leaves the tree: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                if (entry.isSymbolicLink()) {
                    link(entry.getLinkName(), destination, root);
                    continue;
                }
                try (java.io.OutputStream out = Files.newOutputStream(destination,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                        java.nio.file.StandardOpenOption.WRITE)) {
                    tar.transferTo(out);
                }
                if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                        != org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS
                        && (entry.getMode() & 0111) != 0) {
                    destination.toFile().setExecutable(true, false);
                }
            }
        }
    }

    /// Creates a link the archive names, or says why it could not.
    ///
    /// A source tarball may legitimately hold links, but one that points outside the
    /// tree is a way to write somewhere else, so it is refused rather than followed.
    /// A Windows account without Developer Mode cannot create links at all; that is
    /// logged and the rest of the tree is still unpacked — a build that needs that
    /// path then fails visibly instead of on a silently truncated tree.
    private static void link(String linkName, Path destination, Path root) {
        Path resolved = destination.getParent().resolve(linkName).normalize();
        if (!resolved.startsWith(root)) {
            LOG.warning("Skipping an archive link that points outside the tree: " + destination
                    + " -> " + linkName);
            return;
        }
        try {
            Files.createSymbolicLink(destination, Path.of(linkName));
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warning("Could not create the link " + destination + " -> " + linkName
                    + " (" + e.getMessage() + ")");
        }
    }

    /// The lowest Node major the upstream build accepts (its `engines` asks for
    /// 24.14+, and the SEA staging and Vite 8 toolchain both need the modern line).
    private static final int MINIMUM_BUILD_NODE_MAJOR = 24;

    /// Makes sure the build has the toolchain it needs, installing what is
    /// missing into the data directory.
    ///
    /// This is the bare-deployment case, and on Windows it is the common one: no
    /// `/opt/node24` from the image, a system Node on the 22 line (the one dsh
    /// itself supports), and frequently no pnpm at all. "One click" has to mean
    /// the panel puts the toolchain under `<data>/hdsl/runtimes` instead of
    /// telling the user to install Node 24, a package manager and a compiler by
    /// hand. That directory is exactly the one Settings -> Node.js fills, so the
    /// runtime stays visible, reusable and removable there afterwards.
    ///
    /// Measured on Windows 10 (2026-10-18), a full build needs no C++ toolchain
    /// at all: `node-pty` ships `prebuilds/win32-x64/pty.node` and its postinstall
    /// logs `skip electron-rebuild (windows-prebuild-available)`, so there is no
    /// MSVC/Python requirement to satisfy — Node + pnpm is the whole chain.
    ///
    /// @param settings the configured settings
    /// @param step the build step, for progress in the log
    /// @return settings whose `buildBin` and `pnpm` point at a usable toolchain
    /// @throws ZcodeException when the toolchain cannot be provided
    private static Settings withToolchain(Settings settings, Step step) throws ZcodeException {
        if (!settings.buildBin().isBlank()) {
            step.log("=== 构建工具链：已配置的 " + settings.buildBin() + "，pnpm "
                    + (settings.pnpm().isBlank() ? "pnpm（PATH）" : settings.pnpm()));
            return settings;
        }
        Path image = Path.of("/opt/node24/bin");
        if (Files.isDirectory(image)) {
            step.log("=== 构建工具链：镜像自带的 Node 24（" + image + "），pnpm "
                    + (settings.pnpm().isBlank() ? "pnpm（PATH）" : settings.pnpm()));
            return new Settings(settings.root(), image.toString(), settings.pnpm(),
                    settings.sourceUrl(), settings.keepSources());
        }
        DshNodeRuntime runtime = DshNodeRuntime.detectManaged()
                .filter(candidate -> majorOf(candidate.nodeVersion()) >= MINIMUM_BUILD_NODE_MAJOR)
                .orElse(null);
        if (runtime == null) {
            String version = newestBuildNode();
            step.log("=== 数据目录里没有 Node 24+，自动安装一个：" + version + "（只需一次，装在 <data>/hdsl/runtimes）");
            try {
                runtime = DshNodeRuntime.fromManaged(NodeRuntimeManager.install(version, NodeSource.MIRROR,
                        line -> step.log("[node] " + line)));
                DshNodeRuntime.invalidateCache();
            } catch (DshException e) {
                // Another build may have installed the same version between our probe
                // and this call; "already installed" is not a failure in that case.
                runtime = DshNodeRuntime.detectManaged()
                        .filter(candidate -> majorOf(candidate.nodeVersion()) >= MINIMUM_BUILD_NODE_MAJOR)
                        .orElse(null);
                if (runtime == null) {
                    throw new ZcodeException("could not install the build's Node.js into the data directory: "
                            + e.getMessage(), e);
                }
            }
        }
        String pnpm = settings.pnpm();
        if (pnpm.isBlank() || "pnpm".equals(pnpm)) {
            pnpm = ensurePnpm(runtime, step);
        }
        step.log("=== 构建工具链：" + runtime.node() + "（" + runtime.nodeVersion() + "），pnpm " + pnpm);
        return new Settings(settings.root(), runtime.binDirectory().toString(), pnpm,
                settings.sourceUrl(), settings.keepSources());
    }

    /// Tells the operator what the build does and does not have in the way of a
    /// C++ compiler.
    ///
    /// A ZCode build does not need one: `node-pty` ships `prebuilds/win32-x64`, and
    /// `cpu-features` — the only dependency whose install runs `node-gyp` — is an
    /// **optional** dependency of `ssh2`, so pnpm carries on when it cannot compile
    /// (measured on Windows 10 without MSVC: the build completes, `cpufeatures.node`
    /// is simply absent and ssh2 falls back). Saying that before the install turns
    /// the expected `Unable to detect compiler type` in the log from a scare into a
    /// known skip.
    ///
    /// The panel deliberately **does not download** a compiler: Microsoft ships
    /// `cl.exe` only through the Visual Studio installer, where the minimal supported
    /// set (VC++ tools + Windows SDK) is several GB and needs an administrator. A
    /// build must not start a UAC prompt or a 3 GB download behind the operator's back.
    ///
    /// @param step the build step, for the log
    private static void reportCompiler(Step step) {
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                != org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            return;
        }
        Path compiler = msvcCompiler();
        if (compiler != null) {
            step.log("=== C++ 编译链：" + compiler + "（可选原生依赖可现场编译）");
            return;
        }
        step.log("=== 未检测到 MSVC C++ 编译器：cpu-features（ssh2 的可选依赖）会被跳过，不影响本次构建");
        step.log("    需要它时安装 VS Build Tools 的 VC++ 组件（约 2.5–3 GB，需管理员）：");
        step.log("    vs_buildtools.exe --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64"
                + " --add Microsoft.VisualStudio.Component.Windows11SDK.22621");
    }

    /// Finds `cl.exe` through the Visual Studio installer's own locator.
    ///
    /// `vswhere` answers with the installation that has the VC++ tools, whichever
    /// edition or year it is; neither an IDE nor a developer prompt is required. Only
    /// Windows is searched, and a missing `vswhere` simply means "no Visual Studio
    /// toolset here".
    ///
    /// @return the compiler, or `null` when this machine has none
    private static @Nullable Path msvcCompiler() {
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                != org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            return null;
        }
        Path vswhere = Path.of(System.getenv().getOrDefault("ProgramFiles(x86)", "C:\\Program Files (x86)"),
                "Microsoft Visual Studio", "Installer", "vswhere.exe");
        if (!Files.isRegularFile(vswhere)) {
            return null;
        }
        try {
            Process process = new ProcessBuilder(vswhere.toString(),
                    "-latest", "-products", "*",
                    "-requires", "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
                    "-find", "VC\\Tools\\MSVC\\**\\bin\\Hostx64\\x64\\cl.exe")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
                        && process.exitValue() == 0
                        && line != null && !line.isBlank()) {
                    Path compiler = Path.of(line.trim());
                    if (Files.isRegularFile(compiler)) {
                        return compiler;
                    }
                }
            }
        } catch (IOException e) {
            LOG.debug("Could not ask vswhere for the MSVC compiler: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /// The newest Node release the build may use, asked of the mirror so the
    /// answer does not depend on this panel's release cadence.
    ///
    /// @return the version, without a leading `v`
    /// @throws ZcodeException when the release list cannot be read
    private static String newestBuildNode() throws ZcodeException {
        try {
            List<String> candidates = NodeRuntimeManager.fetchReleases(NodeSource.MIRROR).stream()
                    .map(org.jackhuang.hmcl.dsh.NodeRelease::version)
                    // 预发布（-alpha / -rc）也排在索引最前，而它们不该被挑来做构建工具链：
                    // 同一个坑在 `NodeApiServlet.pickNewestSupported` 那边已经踩过（装了
                    // 27.0.0-alpha 当运行时）。要预发布请显式指定版本。
                    .filter(version -> !version.contains("-"))
                    .filter(version -> majorOf(version) >= MINIMUM_BUILD_NODE_MAJOR)
                    .toList();
            if (candidates.isEmpty()) {
                throw new ZcodeException("the Node mirror lists no 24-or-later release to build with");
            }
            return candidates.stream().max(ZcodeBuilder::compareVersions).orElseThrow();
        } catch (DshException e) {
            throw new ZcodeException("could not read the Node release list: " + e.getMessage(), e);
        }
    }

    /// The major component of a version string, or 0 when it is not a number.
    ///
    /// @param version the version, optionally prefixed with `v`
    /// @return the major version
    private static int majorOf(String version) {
        try {
            return Integer.parseInt(version.replaceFirst("^v", "").split("\\.")[0]);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /// Numeric `major.minor.patch` ordering for two version strings.
    ///
    /// @param left one version
    /// @param right the other
    /// @return the comparison
    private static int compareVersions(String left, String right) {
        String[] a = left.replaceFirst("^v", "").split("\\.");
        String[] b = right.replaceFirst("^v", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parseNumber(a[i]) : 0;
            int y = i < b.length ? parseNumber(b[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int parseNumber(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// The pnpm to build with, installed into the runtime when it has none.
    ///
    /// A managed runtime is a Node distribution, not a package manager install:
    /// it has `npm` and nothing else. pnpm goes into the same directory so the
    /// next build (and `Settings -> Node.js`) sees it too.
    ///
    /// @param runtime the build's Node
    /// @param step the build step, for progress in the log
    /// @return the pnpm executable's path
    /// @throws ZcodeException when pnpm cannot be installed
    private static String ensurePnpm(DshNodeRuntime runtime, Step step) throws ZcodeException {
        Path existing = runtime.pnpm();
        if (existing != null) {
            return existing.toString();
        }
        Path npm = runtime.npm();
        if (npm == null) {
            throw new ZcodeException("the build's Node.js has no npm, so pnpm cannot be installed");
        }
        // `--prefix` is the distribution root: on Windows that is where node.exe
        // sits, on POSIX it is the parent of `bin/`.
        Path prefix = org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS
                        ? runtime.binDirectory()
                        : runtime.binDirectory().getParent();
        step.log("=== 该运行时没有 pnpm，正在装进 " + prefix + "（只需一次）");
        String registry = org.jackhuang.hmcl.dsh.NpmRegistry.effective().registry();
        try {
            Process process = new ProcessBuilder(Executables.startable(List.of(npm.toString(),
                    "install", "--global", "pnpm@11", "--prefix", prefix.toString(), "--registry=" + registry)))
                    .redirectErrorStream(true)
                    .start();
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    step.log("[pnpm] " + line);
                }
            }
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES) || process.exitValue() != 0) {
                throw new ZcodeException("installing pnpm into " + prefix + " failed");
            }
        } catch (IOException e) {
            throw new ZcodeException("could not run npm to install pnpm: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZcodeException("installing pnpm was interrupted", e);
        }
        Path installed = org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS
                        ? prefix.resolve("pnpm.cmd")
                        : prefix.resolve("bin").resolve("pnpm");
        if (!Files.isRegularFile(installed)) {
            throw new ZcodeException("npm reported success but " + installed + " does not exist");
        }
        return installed.toString();
    }

    /// The directory of shims put ahead of `PATH` for every build step.
    ///
    /// It lives under the data directory (`<data>/zcode/build/bin`), so a bare
    /// deployment needs nothing installed system-wide: the panel writes the one
    /// program Windows spells differently and lets the build find it.
    ///
    /// @param settings the build settings
    /// @return the shim directory
    private static Path shimDirectory(Settings settings) {
        return settings.root().resolve("build").resolve("bin");
    }

    /// Writes the build shims for this platform.
    ///
    /// Windows' `tar.exe` is bsdtar and takes `C:\…` paths; Git for Windows ships
    /// GNU tar earlier on `PATH`, and GNU tar reads the drive letter as a *remote
    /// host* (`tar (child): Cannot connect to C: resolve failed`), which kills the
    /// tarball step after the whole install and web build have already run. A
    /// `tar.bat` ahead of `PATH` resolves `tar` to the system bsdtar. POSIX needs
    /// no shim: its `tar` is the system one.
    ///
    /// @param settings the build settings
    /// @throws ZcodeException when the shim cannot be written
    private static void prepareShims(Settings settings) throws ZcodeException {
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                != org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            return;
        }
        Path shim = shimDirectory(settings);
        try {
            Files.createDirectories(shim);
            // `%*` forwards the arguments as received, quotes included; `%SystemRoot%`
            // expands to the drive Windows is actually installed on.
            Files.writeString(shim.resolve("tar.bat"),
                    "@echo off\r\n\"%SystemRoot%\\System32\\tar.exe\" %*\r\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ZcodeException("could not write the build shims into " + shim, e);
        }
    }

    /// Runs a child process, copying its merged output into the build log.
    private static void run(Step step, Settings settings, Path workingDirectory, List<String> command,
                            Duration timeout) throws ZcodeException {
        // The shims come first so a bare `tar` (the build script's own call) resolves
        // to the system one, then the build toolchain, then the panel's own PATH. The
        // order matters for the *program name* too: the toolchain's pnpm has to be the
        // one found, which is why the command is resolved against this PATH rather
        // than this process's own.
        String path = System.getenv("PATH");
        // The separator is a property of the system, not of the variable's name:
        // hardcoding `:` on Windows makes the whole inherited PATH into one entry
        // that names nothing.
        StringBuilder searchPath = new StringBuilder(shimDirectory(settings).toString());
        if (!settings.buildBin().isBlank()) {
            searchPath.append(java.io.File.pathSeparator).append(settings.buildBin());
        }
        if (path != null && !path.isEmpty()) {
            searchPath.append(java.io.File.pathSeparator).append(path);
        }
        ProcessBuilder builder = new ProcessBuilder(Executables.startable(command, searchPath.toString()));
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("PATH", searchPath.toString());
        // Everything a child of the launcher is given: the proxy the settings describe, the npm
        // registry, and the knobs that keep a native build from fetching from the slow half of the
        // internet. The build runs pnpm, node-gyp and git, and all three read these — the proxy
        // most of all, which this build used to ignore entirely.
        builder.environment().putAll(org.jackhuang.hmcl.dsh.DshNetworkSettings.environment());
        // Electron's binaries are not on the registry: its postinstall fetches them from GitHub,
        // which this network cannot read at all (measured: zero bytes in twenty seconds). A mirror
        // that publishes them beside itself is the only way that step finishes.
        String electronMirror = org.jackhuang.hmcl.dsh.NpmRegistry.electronMirror();
        if (electronMirror != null) {
            builder.environment().put("ELECTRON_MIRROR", electronMirror);
        }
        step.log("$ " + String.join(" ", command));
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new ZcodeException("could not run " + command.get(0) + " — is it installed?", e);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                step.log(line);
            }
        } catch (IOException e) {
            throw new ZcodeException("could not read the output of " + command.get(0), e);
        }
        try {
            if (!process.waitFor(timeout.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ZcodeException(command.get(0) + " did not finish within " + timeout.toMinutes() + " minutes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ZcodeException("the build was interrupted", e);
        }
        if (process.exitValue() != 0) {
            throw new ZcodeException(command.get(0) + " exited with " + process.exitValue() + " — see the build log");
        }
    }

    /// A step in the build: its own progress writer, appending every line to the
    /// shared log with a timestamp so the interface's tail reads sensibly.
    private static final class Step {

        private final java.io.BufferedWriter writer;
        private final String ref;

        Step(Path log, String ref) {
            this.ref = ref;
            try {
                this.writer = Files.newBufferedWriter(log, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        void set(double fraction, String message) {
            STATUS.set(new Status(State.RUNNING, ref, message, fraction, null));
            log("=== " + message);
        }

        void log(String line) {
            try {
                synchronized (writer) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException ignored) {
                // The log is diagnostics; a write failure must not fail the build.
            }
        }
    }

    /// Removes a directory tree — or a `current`-style pointer, which is not a tree.
    ///
    /// Package-private so the pointer rule can be tested without starting a build.
    ///
    /// @param directory the tree or pointer, may be `null`
    /// @throws IOException when something inside could not be removed
    static void deleteRecursively(@Nullable Path directory) throws IOException {
        // NOFOLLOW: a dangling `current` link must still be deleted, and `Files.exists`
        // on its own follows the link and answers "nothing here".
        if (directory == null || !Files.exists(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        // `current` is a pointer, not a tree: a link where the system allows one, a
        // small text file where it does not. Deleting the pointer must never walk
        // into what it points at — that is an installed release.
        if (Files.isSymbolicLink(directory)
                || !Files.isDirectory(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(directory);
            return;
        }
        // Windows first. A pnpm tree is hundreds of thousands of files and thousands of
        // junctions; `rmdir /s` descends into every junction and can take ten minutes
        // (measured: still running at the ten-minute mark on a 2 GB tree). `robocopy
        // /MIR` from an empty directory empties it in seconds and `/XJ` keeps it out
        // of junctions entirely — which is also what made the old walk fail: the walk
        // entered `node_modules/@zcode/…` through a junction, the target was deleted
        // from the sibling branch first, and enumeration died with NoSuchFileException,
        // reported as a build failure before the download had even started.
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            Path empty = directory.toAbsolutePath().getParent().resolve(".empty");
            try {
                Files.createDirectories(empty);
                Process process = new ProcessBuilder("robocopy", empty.toString(), directory.toString(),
                        "/MIR", "/XJ", "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS", "/R:0", "/W:0")
                        .redirectErrorStream(true)
                        .start();
                process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)
                        && process.exitValue() <= 7) {
                    Files.deleteIfExists(directory);
                    if (!Files.exists(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                        return;
                    }
                }
            } catch (IOException e) {
                LOG.warning("Could not empty " + directory + " with robocopy; falling back to a walk", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while removing " + directory, e);
            } finally {
                // The empty source of the mirror is ours; do not leave `.empty` beside
                // every tree that was ever cleaned.
                try {
                    Files.deleteIfExists(empty);
                } catch (IOException ignored) {
                    // A leftover empty directory is not worth failing a build over.
                }
            }
        }
        // Fallback (and the POSIX path): walk without ever failing the build over an
        // entry that is already gone — a junction's target disappearing mid-walk is
        // normal, not an error.
        Files.walkFileTree(directory, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file,
                    java.nio.file.attribute.BasicFileAttributes attrs) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    LOG.debug("Could not delete " + file + " (already gone?)");
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) {
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
                try {
                    Files.deleteIfExists(dir);
                } catch (IOException e) {
                    LOG.debug("Could not delete " + dir + " (already gone?)");
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
        // The walk tolerates entries that vanished mid-way; it must not also tolerate
        // leaving the tree behind. A stale `node_modules` would make the next build
        // install over an old one and produce a package nobody can explain.
        if (Files.exists(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("could not remove " + directory
                    + " — some entries are still there (in use, or a link that could not be followed)");
        }
    }
}
