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

import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.NodeRuntime;
import org.jackhuang.hmcl.dsh.NodeRuntimeManager;
import org.jackhuang.hmcl.dsh.NodeSemver;
import org.jackhuang.hmcl.dsh.NpmRegistry;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Links;
import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Installs and enumerates the tool releases of one [Brand].
///
/// Three source flavours:
///
/// - **npm brands** install a published package with the same command the dsh
///   installer uses, minus the lockstep machinery:
///   ```
///   pnpm add --dir <releases>/<version> <pkg>@<version> \
///       --reporter=append-only --registry=<effective> \
///       --config.dangerously-allow-all-builds=true
///   ```
/// - **GitHub brands** download the release tag's source zip over HTTPS
///   (`codeload.github.com/<slug>/zip/refs/tags/<tag>`, retried; no `git`
///   involved — the git transport to github.com is cut mid-handshake on this
///   deployment's network), unpack it, then `npm ci -w server -w web` (only
///   the workspaces the web mode needs — the desktop workspace's Electron
///   binary is never downloaded), then `npm run build -w web`.
/// - **Octop** downloads the prebuilt portable archive from GitHub Releases
///   (CPython 3.12 + every Python dependency included) and replaces only its
///   dashboard with the patched SPA bundled in this jar.
///
/// Two npm details are load-bearing:
///
/// - `--config.dangerously-allow-all-builds=true` — pnpm 10 and newer refuse to
///   run dependency lifecycle scripts by default, and Kimi Code's `postinstall`
///   is what downloads its native binary. Without the flag the install
///   "succeeds" into a prefix with no working executable.
/// - `--registry` is the panel's effective download source, so a mirror-first
///   deployment installs from the mirror like everything else. (Kimi Code's
///   postinstall still reaches its vendor CDN for the binary; fully offline
///   installs of that brand are impossible, which the docs say.)
///
/// Each version lands in `releases/<version>/`; a `current` symlink points at
/// the newest successful install and is what new instances default to.
@NotNullByDefault
public final class BrandInstaller {

    /// One installed release, as listed by the panel.
    public record Release(String version, String path, boolean current) {
    }

    /// What an install is allowed to include.
    ///
    /// PI WEB is the only brand with two shapes: the full pair (web + Pi SDK)
    /// and the **terminal-only** release (the Pi SDK alone). The second exists
    /// for two callers — an operator who wants the TUI and nothing else, and
    /// the degradation path when the web package cannot be installed — and it
    /// is a property of the release on disk, not a runtime toggle.
    public enum Mode {
        /// The web app and the Pi SDK it runs on.
        FULL,
        /// Only the Pi SDK: the TUI works, there is no web process to start.
        TERMINAL_ONLY
    }

    /// What a PI WEB release contains, written next to it as `release.json`.
    ///
    /// `webAvailable` is the difference between "this directory is a pair" and
    /// "this directory is a Pi terminal"; `ptyAvailable` is the install-time
    /// answer to "can the web app provide its browser terminal" (node-pty's
    /// native module, which Linux builds from source).
    public record ReleaseInfo(String version, String pairedSdk, boolean webAvailable,
                              boolean ptyAvailable, long installedAt) {
    }

    /// Where an install reports progress: one human line and a 0–1 fraction.
    ///
    /// The fraction is what drives the panel's bar and its ETA, so the installer reports it at
    /// stage boundaries and ticks it while a stage runs — an install that only ever said "running"
    /// for an hour is the thing this exists to prevent.
    @FunctionalInterface
    public interface Progress {
        void update(String message, double fraction);

        /// Reports the bar of the sub-step in flight, when that step knows a real fraction
        /// (download bytes, installed packages). Steps without a real fraction leave it to the
        /// time-based tick.
        default void stage(String stage, double fraction) {
        }

        /// The same, plus a machine-readable key for the phase (`download`, `write`, `build`,
        /// `install`, `done`) so the page can say "downloads are over" without matching text.
        default void stage(String stage, String key, double fraction) {
            stage(stage, fraction);
        }

        /// Whether the user asked to stop. The installers check this in the loops that are not a
        /// child process (downloads, unzip); a child process is killed by the task service itself.
        default boolean isCancelled() {
            return false;
        }
    }

    /// A [Progress] that also carries the current fraction for the many `message.accept` calls the
    /// installers already make: those lines are ordinary log output, and the bar must not jump back
    /// to "unknown" because one of them arrived.
    private static final class Sink implements java.util.function.Consumer<String> {
        private final Progress progress;
        private volatile double fraction = -1;

        Sink(Progress progress) {
            this.progress = progress;
        }

        @Override
        public void accept(String line) {
            lastLineAt = System.currentTimeMillis();
            progress.update(line, fraction);
        }

        void fraction(double value) {
            this.fraction = value;
        }

        /// The overall fraction as last reported, for reporters that only own the sub-step bar.
        double fraction() {
            return fraction;
        }

        void report(String line, double value) {
            fraction = value;
            lastLineAt = System.currentTimeMillis();
            progress.update(line, value);
        }

        /// Reports the sub-step's own bar, and remembers when — the time-based tick must not
        /// overwrite a real number with an estimate half a second later.
        void stage(String name, double value) {
            lastRealProgressAt = System.currentTimeMillis();
            progress.stage(name, value);
        }

        /// The same, with the phase key the page reads (`download` / `write` / `build` / `install`).
        void stage(String name, String key, double value) {
            lastRealProgressAt = System.currentTimeMillis();
            progress.stage(name, key, value);
        }

        /// Reports the sub-step's bar without claiming it is real progress: used by the ticker,
        /// which is an estimate and must not stop the ticker from updating again.
        void stageOnly(String name, double value) {
            progress.stage(name, value);
        }

        /// Whether nothing has been printed for `millis`.
        boolean quietFor(long millis) {
            return System.currentTimeMillis() - lastLineAt >= millis;
        }

        /// How many seconds nothing has been printed.
        long quietSeconds() {
            return Math.max(0, (System.currentTimeMillis() - lastLineAt) / 1000);
        }

        /// Whether no real (not estimated) sub-step fraction arrived for `millis`.
        boolean noRealProgressFor(long millis) {
            return System.currentTimeMillis() - lastRealProgressAt >= millis;
        }

        /// Whether the user asked to stop.
        boolean cancelled() {
            return progress.isCancelled();
        }

        private volatile long lastRealProgressAt;
        private volatile long lastLineAt;
    }

    @FunctionalInterface

    /// A step body that may touch the filesystem: [StepBody] reports only
    /// [BrandException], and an `IOException` here is the same kind of failure —
    /// the message the user sees, with the path in it.
    private interface IOBody {
        void run() throws IOException, BrandException;
    }

    private static void ioStep(IOBody body) throws BrandException {
        try {
            body.run();
        } catch (IOException e) {
            throw new BrandException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
        }
    }

    private interface StepBody {
        void run() throws BrandException;
    }

    /// Runs one weighted stage of an install: names it, ticks the bar while it runs (against a
    /// rough expected duration, capped below 100%), and completes it afterwards.
    ///
    /// @param message        the sink shared by the whole install
    /// @param index          zero-based stage index
    /// @param total          how many stages the install has
    /// @param name           the stage, as the panel shows it
    /// @param key            the machine-readable phase: `download` / `write` / `build` / `install`
    ///                       / `done`. The page uses it to say "downloads are finished" without
    ///                       matching the Chinese text.
    /// @param before         how much of the bar was already done (0–1)
    /// @param weight         how much of the bar this stage owns (0–1)
    /// @param expectedSeconds how long this stage usually takes; only makes the tick move
    /// @param body           the work
    private static void stage(Sink message, int index, int total, String name, String key,
                              double before, double weight, int expectedSeconds, StepBody body)
            throws BrandException {
        long started = System.currentTimeMillis();
        message.report(label(index, total, name, before), before);
        message.stage(name, key, 0);
        java.util.Timer ticker = new java.util.Timer("brand-install-progress", true);
        ticker.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // The bar keeps moving on the estimate while nothing better arrives...
                if (message.noRealProgressFor(1500)) {
                    double in = Math.min(0.95,
                            (System.currentTimeMillis() - started) / (1000.0 * Math.max(1, expectedSeconds)));
                    message.stageOnly(name, in);
                    // ...but the line itself is only replaced when the tool is quiet: a stage that
                    // prints (vite, npm, uv) must show its own output, not "55%" forever. A stage
                    // that has been silent for a while says so, because silence is exactly what
                    // looked like a dead process on this deployment's slow volume.
                    if (message.quietFor(2000)) {
                        long quiet = message.quietSeconds();
                        String suffix = quiet >= 15 ? " · 已 " + quiet + " 秒无新输出" : "";
                        message.report(label(index, total, name, before + weight * in) + suffix,
                                before + weight * in);
                    }
                }
            }
        }, 700, 700);
        try {
            body.run();
        } finally {
            ticker.cancel();
        }
        message.stage(name, key, 1);
        message.report(label(index, total, name, before + weight), before + weight);
    }

    /// The same, with the phase key derived from the stage name.
    ///
    /// The names are constants in this file, so this is a mapping, not a guess; stages that know
    /// their phase for real (uv's watcher) call the keyed version directly.
    private static void stage(Sink message, int index, int total, String name,
                              double before, double weight, int expectedSeconds, StepBody body)
            throws BrandException {
        stage(message, index, total, name, keyOf(name), before, weight, expectedSeconds, body);
    }

    /// The phase key for a stage name: `download` / `write` / `build` / `install` / `done`.
    private static String keyOf(String name) {
        if (name.contains("下载") || name.contains("准备 uv")) {
            return "download";
        }
        if (name.contains("构建") || name.contains("编译")) {
            return "build";
        }
        if (name.contains("安装")) {
            return "install";
        }
        if (name.contains("收尾")) {
            return "done";
        }
        return "write";
    }

    /// `[3/7] 构建 dashboard · 55%` — the line the panel shows next to the bar.
    private static String label(int index, int total, String name, double fraction) {
        int percent = (int) Math.floor(Math.max(0, Math.min(1, fraction)) * 100);
        return "[" + (index + 1) + "/" + total + "] " + name + " · " + percent + "%";
    }

    /// Downloads with a byte-accurate sub-step bar: the server's `Content-Length` is the total, and
    /// `12.3 MB / 260.0 MB` is a number a person can wait on. Without a length the bar stays on the
    /// time-based tick.
    private static java.io.InputStream download(String url, Sink message, String label) throws BrandException {
        return download(url, message, label, -1, 0);
    }

    /// The same, also advancing the **overall** bar: `before + weight × bytes-read`.
    ///
    /// Without it a download reports its own sub-step only, and the stage's time-based
    /// tick never takes over (a download prints a line twice a second, which reads as
    /// "still busy" to the quiet check) — so a four-minute download showed 0%.
    static java.io.InputStream download(String url, Sink message, String label, double before, double weight)
            throws BrandException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Accept", "*/*")
                .GET()
                .build();
        try {
            HttpResponse<java.io.InputStream> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new BrandException("Download answered " + response.statusCode() + ": " + url);
            }
            long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            ProgressStream stream = new ProgressStream(response.body(), message, label, total, before, weight);
            stream.watchCancellation();
            return stream;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("Download was interrupted: " + url, e);
        } catch (java.io.IOException e) {
            throw new BrandException("Download failed: " + url + " — " + e.getMessage(), e);
        }
    }

    /// An input stream that reports how many bytes went by, at most twice a second — often enough
    /// to look alive, rare enough not to flood the task feed.
    private static final class ProgressStream extends java.io.FilterInputStream {
        private final Sink message;
        private final String label;
        private final long total;
        private long read;
        private long lastReport;

        private final double before;
        private final double weight;

        ProgressStream(java.io.InputStream in, Sink message, String label, long total,
                       double before, double weight) {
            super(in);
            this.message = message;
            this.label = label;
            this.total = total;
            this.before = before;
            this.weight = weight;
        }

        @Override
        public int read() throws java.io.IOException {
            int b = super.read();
            if (b >= 0) {
                tick(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws java.io.IOException {
            int n = super.read(buffer, offset, length);
            if (n > 0) {
                tick(n);
            }
            return n;
        }

        private void tick(int n) throws java.io.IOException {
            // The task's cancel flag is the only thing a blocking read can notice here: the socket
            // belongs to an HttpClient, not to a child process the task service could kill.
            if (message.cancelled()) {
                throw new java.io.IOException("cancelled");
            }
            read += n;
            long now = System.currentTimeMillis();
            long sinceSpeed = now - lastSpeedAt;
            if (sinceSpeed >= 800) {
                double instant = (read - lastSpeedBytes) / (sinceSpeed / 1000.0);
                speed = speed <= 0 ? instant : speed * 0.5 + instant * 0.5;
                lastSpeedBytes = read;
                lastSpeedAt = now;
            }
            if (now - lastReport < 500) {
                return;
            }
            lastReport = now;
            String rate = speed > 1024 ? " · " + speedText(speed) : "";
            if (total > 0) {
                double done = Math.min(0.99, read / (double) total);
                message.stage(label, done);
                long remaining = speed > 1024 ? (long) ((total - read) / speed) : -1;
                String line = label + "：" + megabytes(read) + " / " + megabytes(total) + rate
                        + (remaining >= 0 ? " · 剩约 " + durationText(remaining) : "");
                if (weight > 0) {
                    message.report(line, before + weight * done);
                } else {
                    message.accept(line);
                }
            } else {
                message.stage(label, Math.min(0.9, read / (260.0 * 1024 * 1024)));
                message.accept(label + "：" + megabytes(read) + rate);
            }
        }

        private long lastSpeedAt = System.currentTimeMillis() - 1000;
        private long lastSpeedBytes;
        private double speed;

        /// Closes the stream as soon as the task is cancelled.
        ///
        /// A read that is waiting on a server which never sends another byte cannot notice the
        /// flag, so a cancel needs somebody to shut the socket; otherwise "stop" only takes effect
        /// when the HTTP timeout does, minutes later.
        void watchCancellation() {
            Thread watcher = new Thread(() -> {
                try {
                    while (!closed && !message.cancelled()) {
                        Thread.sleep(500);
                    }
                    if (!closed) {
                        close();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.io.IOException ignored) {
                    // Closing an already-broken stream is the outcome we wanted anyway.
                }
            }, "brand-install-cancel");
            watcher.setDaemon(true);
            watcher.start();
        }

        @Override
        public void close() throws java.io.IOException {
            closed = true;
            super.close();
        }

        private volatile boolean closed;
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /// Reads `uv sync`'s output and turns it into a phase, a rate and a bar.
    ///
    /// uv prints one line per package it fetches (`Downloading numpy (15.9MiB)`), one per sdist it
    /// compiles (`Building evdev==1.9.3`), and `Prepared` / `Installed N packages` at the end of
    /// each phase. On a slow link that is minutes of silence, which used to look exactly like a
    /// hang; this turns it into `uv：下载 12 个包 · 68.4 MB · 6.1 MB/s` and an explicit
    /// "下载完成，开始编译" — the two things the page could not tell before.
    private static final class UvWatcher implements java.util.function.Consumer<String>, AutoCloseable {
        private final Sink message;
        private final Path brandRoot;
        private final Path venvDir;
        private final Path archiveDir;
        private final java.util.Timer timer;
        private final java.util.LinkedHashSet<String> fetched = new java.util.LinkedHashSet<>();
        private final java.util.LinkedHashSet<String> building = new java.util.LinkedHashSet<>();
        private final java.util.LinkedHashSet<String> built = new java.util.LinkedHashSet<>();
        private long bytes;
        private long sampledBytes;
        private long sampledAt = System.currentTimeMillis();
        private double speed;
        private int prepared = -1;
        private int installed = -1;
        private int unpacked = -1;
        private long lastLineAt = System.currentTimeMillis();
        /// Packages already announced in the log, so each one is reported exactly once — the
        /// portage-style stream the page tails.
        private final java.util.LinkedHashSet<String> announced = new java.util.LinkedHashSet<>();
        private boolean headerAnnounced;

        UvWatcher(Sink message, Path brandRoot, Path venvDir, Path archiveDir) {
            this.message = message;
            this.brandRoot = brandRoot;
            this.venvDir = venvDir;
            this.archiveDir = archiveDir;
            this.timer = new java.util.Timer("brand-uv-progress", true);
            timer.scheduleAtFixedRate(new java.util.TimerTask() {
                @Override
                public void run() {
                    publish();
                }
            }, 1000, 1500);
        }

        @Override
        public void accept(String line) {
            lastLineAt = System.currentTimeMillis();
            String text = line.trim();
            if (text.startsWith("Downloading ")) {
                int open = text.lastIndexOf('(');
                int close = text.lastIndexOf(')');
                String name = text.substring("Downloading ".length(), open > 0 ? open : text.length()).trim();
                if (fetched.add(name)) {
                    if (open > 0 && close > open) {
                        bytes += parseSize(text.substring(open + 1, close));
                    }
                    publish();
                }
            } else if (text.startsWith("Building ")) {
                building.add(text.substring("Building ".length()).trim());
                publish();
            } else if (text.startsWith("Built ")) {
                String spec = text.substring("Built ".length()).trim();
                building.remove(spec);
                built.add(spec);
                publish();
            } else if (text.startsWith("Prepared ")) {
                prepared = leadingNumber(text);
                publish();
            } else if (text.startsWith("Installed ")) {
                installed = leadingNumber(text);
                publish();
            } else if (text.startsWith("Using CPython") || text.startsWith("Creating virtual environment")
                    || text.startsWith("Resolved ") || text.startsWith("Audited ")) {
                publish();
            }
        }

        private void publish() {
            long now = System.currentTimeMillis();
            long elapsed = now - sampledAt;
            if (elapsed >= 800) {
                double instant = (bytes - sampledBytes) / (elapsed / 1000.0);
                speed = speed <= 0 ? instant : speed * 0.6 + instant * 0.4;
                sampledBytes = bytes;
                sampledAt = now;
            }

            boolean downloadsDone = prepared >= 0 || installed >= 0 || !building.isEmpty() || !built.isEmpty();
            String summary = "下载完成（" + fetched.size() + " 个包"
                    + (bytes > 0 ? " · " + megabytes(bytes) : "") + "）";
            long quietSeconds = Math.max(0, (System.currentTimeMillis() - lastLineAt) / 1000);
            // uv's progress bars are TTY-only: in a pipe it prints one line per download and then
            // goes silent for the unpack/install phase, which on a slow volume is tens of minutes.
            // Saying how long it has been quiet is the difference between "working" and "dead".
            String quiet = quietSeconds >= 5 ? " · 已 " + quietSeconds + " 秒无新输出" : "";
            unpacked = countInstalled(venvDir);
            if (unpacked < 0) {
                unpacked = countArchives(archiveDir);
            }
            announceNewPackages();

            String phase;
            String line;
            double fraction;
            if (installed >= 0) {
                phase = "安装完成";
                fraction = 0.96;
                line = "uv：" + summary + " · 安装完成 " + installed + " 个包";
            } else if (!building.isEmpty()) {
                phase = "编译本地依赖";
                int done = built.size();
                int running = building.size();
                fraction = 0.55 + 0.1 * (done + 0.5) / (done + running + 1.5);
                line = "uv：" + summary + " · 编译 " + done + " 个完成"
                        + "，进行中 " + running + "（" + preview(building) + "）" + quiet;
            } else if (downloadsDone) {
                // Silent phase: uv is unpacking wheel archives onto this disk and will not say a
                // word until the end. Count what is on disk instead of waiting for it to speak.
                phase = "解包写入磁盘";
                String total = prepared > 0 ? "/" + prepared : "";
                double in = prepared > 0 && unpacked >= 0
                        ? Math.min(0.99, unpacked / (double) prepared)
                        : (unpacked > 0 ? 1 - 1 / (1 + unpacked / 40.0) : 0);
                fraction = 0.65 + 0.3 * in;
                line = "uv：" + summary
                        + (built.isEmpty() ? "" : " · 已编译 " + built.size() + " 个")
                        + " · 解包写入 " + (unpacked >= 0 ? unpacked : "?") + total + " 个包"
                        + quiet;
            } else {
                phase = "下载依赖";
                fraction = 0.55 * (1 - 1 / (1 + fetched.size() / 6.0));
                line = "uv：下载 " + fetched.size() + " 个包"
                        + (bytes > 0 ? " · " + megabytes(bytes) : "")
                        + (speed > 1024 ? " · " + speedText(speed) : "")
                        + quiet;
            }
            message.stage(phase, keyOf(phase), Math.min(0.97, fraction));
            message.report(line, message.fraction());
        }

        /// Packages already unpacked into the venv: one `.dist-info` directory each. A single
        /// directory read — the only kind of I/O that is cheap on a volume this slow.
        private static int countInstalled(Path venvDir) {
            Path lib = venvDir.resolve("lib");
            if (!Files.isDirectory(lib)) {
                return -1;
            }
            try (java.util.stream.Stream<Path> interpreters = Files.list(lib)) {
                for (Path dir : (Iterable<Path>) interpreters::iterator) {
                    if (!dir.getFileName().toString().startsWith("python3")) {
                        continue;
                    }
                    Path site = dir.resolve("site-packages");
                    if (!Files.isDirectory(site)) {
                        continue;
                    }
                    try (java.util.stream.Stream<Path> entries = Files.list(site)) {
                        return (int) entries
                                .filter(path -> path.getFileName().toString().endsWith(".dist-info"))
                                .count();
                    }
                }
            } catch (java.io.IOException e) {
                LOG.info("Could not count the packages in " + venvDir + ": " + e.getMessage());
            }
            return -1;
        }

        /// Portage's trick: a wrapper that reports each package the moment it lands.
        ///
        /// uv in a pipe says nothing while it unpacks, which on this volume is tens of minutes of
        /// silence. One `readdir` per tick is cheap and gives the stream the page tails:
        /// `>>> 已安装 (199 of 345) cryptography-46.0`.
        private void announceNewPackages() {
            Path lib = venvDir.resolve("lib");
            if (!Files.isDirectory(lib)) {
                return;
            }
            java.util.List<String> names = new java.util.ArrayList<>();
            try (java.util.stream.Stream<Path> interpreters = Files.list(lib)) {
                for (Path dir : (Iterable<Path>) interpreters::iterator) {
                    if (!dir.getFileName().toString().startsWith("python3")) {
                        continue;
                    }
                    Path site = dir.resolve("site-packages");
                    if (!Files.isDirectory(site)) {
                        continue;
                    }
                    try (java.util.stream.Stream<Path> entries = Files.list(site)) {
                        entries.forEach(path -> {
                            String name = path.getFileName().toString();
                            if (name.endsWith(".dist-info")) {
                                names.add(name);
                            }
                        });
                    }
                    break;
                }
            } catch (java.io.IOException e) {
                return;
            }
            if (names.isEmpty()) {
                return;
            }
            if (!headerAnnounced) {
                headerAnnounced = true;
                appendLogLine(brandRoot, ">>> 开始安装依赖"
                        + (prepared > 0 ? "（共 " + prepared + " 个包）" : "")
                        + "：uv 在管道里不出声，这里每装好一个就报一行");
            }
            java.util.Collections.sort(names);
            StringBuilder batch = new StringBuilder();
            for (String name : names) {
                if (!announced.add(name)) {
                    continue;
                }
                String clean = name.substring(0, name.length() - ".dist-info".length());
                batch.append(">>> 已安装 (").append(announced.size())
                        .append(prepared > 0 ? " of " + prepared : "")
                        .append(") ").append(clean).append('\n');
            }
            if (batch.length() > 0) {
                // One write per tick, not one per package: the volume this runs on charges for
                // every open and every flush.
                appendLogLine(brandRoot, batch.toString().stripTrailing());
            }
        }

        /// Unpacked archives in uv's cache — the fallback before a venv exists.
        private static int countArchives(Path archiveDir) {
            if (!Files.isDirectory(archiveDir)) {
                return -1;
            }
            try (java.util.stream.Stream<Path> entries = Files.list(archiveDir)) {
                return (int) entries.count();
            } catch (java.io.IOException e) {
                return -1;
            }
        }

        @Override
        public void close() {
            timer.cancel();
            publish();
        }

        /// Keeps the first two names, so the line stays one line.
        private static String preview(java.util.Set<String> names) {
            int shown = 0;
            StringBuilder text = new StringBuilder();
            for (String name : names) {
                if (shown == 2) {
                    text.append(" 等");
                    break;
                }
                if (shown > 0) {
                    text.append("、");
                }
                String simple = name.contains("==") ? name.substring(0, name.indexOf("==")) : name;
                text.append(simple);
                shown++;
            }
            return text.toString();
        }

        private static int leadingNumber(String text) {
            int start = text.indexOf(' ');
            if (start < 0) {
                return -1;
            }
            int end = start + 1;
            while (end < text.length() && Character.isDigit(text.charAt(end))) {
                end++;
            }
            return end > start + 1 ? Integer.parseInt(text.substring(start + 1, end)) : -1;
        }

        private static long parseSize(String text) {
            String value = text.trim().toUpperCase(java.util.Locale.ROOT);
            double factor = 1;
            if (value.endsWith("KIB")) {
                factor = 1024;
                value = value.substring(0, value.length() - 3);
            } else if (value.endsWith("MIB")) {
                factor = 1024 * 1024;
                value = value.substring(0, value.length() - 3);
            } else if (value.endsWith("GIB")) {
                factor = 1024.0 * 1024 * 1024;
                value = value.substring(0, value.length() - 3);
            } else if (value.endsWith("B")) {
                value = value.substring(0, value.length() - 1);
            }
            try {
                return (long) (Double.parseDouble(value) * factor);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /// `<n> MB/s`, with the unit a person reads.
    private static String speedText(double bytesPerSecond) {
        if (bytesPerSecond >= 1024 * 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
        return String.format(java.util.Locale.ROOT, "%.0f KB/s", bytesPerSecond / 1024.0);
    }

    /// `1 分 20 秒` / `40 秒`.
    private static String durationText(long seconds) {
        long total = Math.max(0, seconds);
        long minutes = total / 60;
        long rest = total % 60;
        return minutes > 0 ? minutes + " 分 " + rest + " 秒" : rest + " 秒";
    }

    /// Runs `body` while counting how many packages `node_modules` holds, against the count the
    /// lockfile declares: `已安装 762/1245 个包` is the only honest progress npm can offer, because
    /// `npm ci` prints almost nothing while it works.
    private static void runWithPackageCount(Path nodeModules, Path lockFile, Sink message, String label,
                                            StepBody body) throws BrandException {
        long expected = countLockPackages(lockFile);
        long[] lastShown = {-1};
        long startedAt = System.currentTimeMillis();
        java.util.Timer watcher = new java.util.Timer("brand-install-packages", true);
        watcher.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                int installed = countInstalled(nodeModules);
                if (installed == lastShown[0]) {
                    return;
                }
                lastShown[0] = installed;
                if (expected > 0) {
                    message.stage(label, Math.min(0.95, installed / (double) expected));
                    // Rate over the whole run, and the remaining time it implies — the one number
                    // `npm ci` itself never offers.
                    double seconds = (System.currentTimeMillis() - startedAt) / 1000.0;
                    String eta = "";
                    if (installed > 0 && seconds > 10) {
                        double perSecond = installed / seconds;
                        long remaining = (long) ((expected - installed) / perSecond);
                        eta = " · 剩约 " + durationText(remaining);
                    }
                    message.accept(label + "：已安装 " + installed + "/" + expected + " 个包" + eta);
                } else {
                    message.accept(label + "：已安装 " + installed + " 个包");
                }
            }
        }, 500, 900);
        try {
            body.run();
        } finally {
            watcher.cancel();
        }
        if (expected > 0) {
            message.stage(label, 1);
            message.accept(label + "：已安装 " + expected + "/" + expected + " 个包");
        }
    }

    /// How many packages a lockfile declares, or `-1` when it cannot be read.
    private static long countLockPackages(Path lockFile) {
        if (!Files.isRegularFile(lockFile)) {
            return -1;
        }
        try (java.io.Reader reader = Files.newBufferedReader(lockFile, java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("packages") instanceof JsonObject packages) {
                long count = 0;
                for (String key : packages.keySet()) {
                    if (!key.isEmpty()) {
                        count++;
                    }
                }
                return count;
            }
            if (root.get("dependencies") instanceof JsonObject dependencies) {
                return dependencies.size();
            }
        } catch (RuntimeException | java.io.IOException e) {
            LOG.info("Could not read " + lockFile + " for the package count: " + e.getMessage());
        }
        return -1;
    }

    /// How many packages are on disk right now (scoped packages count once each).
    private static int countInstalled(Path nodeModules) {
        if (!Files.isDirectory(nodeModules)) {
            return 0;
        }
        int count = 0;
        try (java.util.stream.Stream<Path> entries = Files.list(nodeModules)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                String name = entry.getFileName().toString();
                if (name.startsWith(".") || !Files.isDirectory(entry)) {
                    continue;
                }
                if (name.startsWith("@")) {
                    try (java.util.stream.Stream<Path> scoped = Files.list(entry)) {
                        count += (int) scoped.filter(Files::isDirectory).count();
                    }
                } else {
                    count++;
                }
            }
        } catch (java.io.IOException e) {
            LOG.info("Could not count " + nodeModules + ": " + e.getMessage());
        }
        return count;
    }

    static final String RELEASES_DIR = "releases";
    /// The staging name the source build moves a previous release to. It is
    /// **not** a release: a killed build leaves it behind (see
    /// [#reconcileOctopReleases]).
    static final String PREVIOUS_SUFFIX = ".previous";
    private static final String CURRENT_LINK = "current";
    private static final String INSTALL_LOG = "install.log";
    /// Pinned uv release for the source fallback. `latest/download` redirects
    /// through a host that times out on this network; a pinned tag serves the
    /// asset directly.
    private static final String UV_VERSION = "0.7.13";

    /// The HTTP client for GitHub source downloads — its own instance, with a
    /// longer connect timeout than the catalog's (an 11 MB body in the default
    /// 15 s budget would be tight on a slow route).
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private BrandInstaller() {
    }

    /// Installs one version of the brand's package into `releases/<version>`.
    ///
    /// Idempotent: an already-installed version reports done without running
    /// the installer again. The merged output is appended to
    /// `<brandRoot>/install.log`.
    ///
    /// @param brand the brand
    /// @param version the exact version to install
    /// @param brandRoot the brand's directory under the data directory
    /// @param message progress sink for the task record
    /// @throws BrandException when a required tool is missing, the install
    ///                          fails, or the installed release is incomplete
    public static void install(Brand brand, String version, Path brandRoot, Progress progress)
            throws BrandException {
        install(brand, version, brandRoot, progress, Mode.FULL);
    }

    /// The same, choosing what the release contains.
    ///
    /// @param mode [Mode#FULL] for the pair, [Mode#TERMINAL_ONLY] for the Pi SDK alone
    public static void install(Brand brand, String version, Path brandRoot, Progress progress, Mode mode)
            throws BrandException {
        if (mode != Mode.FULL && brand != Brand.PI_WEB) {
            throw new BrandException(brand.id() + " has no terminal-only release; only pi-web does");
        }
        Sink message = new Sink(progress);
        Path releases = brandRoot.resolve(RELEASES_DIR);
        Path target = releases.resolve(version);
        boolean installed = brand == Brand.PI_WEB
                ? releaseUsable(brand, target, mode)
                : Files.exists(target.resolve(brand.installMarker()));
        if (installed && brand == Brand.OCTOP) {
            // A source-built release has the same marker; asking for the
            // package layout must replace it, not report "already installed".
            installed = "package".equals(markerMode(target));
        }
        if (installed) {
            message.accept(brand.sourceLabel() + " " + version + " is already installed");
            handReleaseToAgent(target);
            pointCurrentAt(brandRoot, target);
            return;
        }
        if (brand == Brand.PI_WEB) {
            installPiWeb(brand, version, brandRoot, target, message, mode);
            handReleaseToAgent(target);
            pointCurrentAt(brandRoot, target);
            message.accept("Installed " + brand.sourceLabel() + " " + version
                    + (mode == Mode.TERMINAL_ONLY ? " (terminal only)" : ""));
            LOG.info("Installed " + brand.sourceLabel() + " " + version + " into " + target
                    + " (" + mode + ")");
            return;
        }
        if (brand == Brand.HERMES) {
            installHermes(brand, version, brandRoot, target, message);
            handReleaseToAgent(target);
            pointCurrentAt(brandRoot, target);
            message.accept("Installed " + brand.sourceLabel() + " " + version);
            LOG.info("Installed " + brand.sourceLabel() + " " + version + " into " + target);
            return;
        }
        if (brand == Brand.OCTOP) {
            installOctop(brand, version, brandRoot, target, message);
            handReleaseToAgent(target);
            pointCurrentAt(brandRoot, target);
            message.accept("Installed " + brand.sourceLabel() + " " + version);
            LOG.info("Installed " + brand.sourceLabel() + " " + version + " into " + target);
            return;
        }
        if (brand.source() == Brand.Source.GITHUB) {
            installGitHub(brand, version, brandRoot, target, message);
            handReleaseToAgent(target);
            pointCurrentAt(brandRoot, target);
            message.accept("Installed " + brand.sourceLabel() + " " + version);
            LOG.info("Installed " + brand.sourceLabel() + " " + version + " into " + target);
            return;
        }

        Path bin = target.resolve(brand.installMarker());
        // "A Node.js with pnpm" rather than "a Node.js": the install runs `pnpm add`, and a
        // PATH-probed runtime without one is how the managed runtime — installed *with*
        // pnpm — came to be ignored while every brand install failed on a bare `pnpm`.
        // TUI brands prefer that managed runtime so their narrow engines range cannot
        // silently pick up an older host Node.
        DshNodeRuntime runtime = (brand.prefersManagedNode()
                ? DshNodeRuntime.detectManaged()
                        .filter(DshNodeRuntime::canManagePlugins)
                        .or(DshNodeRuntime::detectWithPnpm)
                : DshNodeRuntime.detectWithPnpm())
                .orElseThrow(() -> new BrandException("Node.js was not found on PATH and no panel-managed runtime "
                        + "is installed; installing " + brand.npmPackage() + " needs one. "
                        + "Open Settings -> Node.js for a one-click managed runtime, or install Node.js yourself."));
        if (!runtime.canManagePlugins()) {
            throw new BrandException(runtime.pnpmMissing("installing " + brand.sourceLabel()).getMessage());
        }
        String pnpm = runtime.pnpm().toString();
        String registry = NpmRegistry.effective().registry();

        try {
            Files.createDirectories(target);
            // `pnpm add` runs as the agent and writes the tree here; the panel is the one that
            // made the directory. Same rule as an instance home — and the reason a brand
            // install fails with EACCES on a data directory that is not setgid.
            Privileges.prepareAgentDirectory(target, Privileges.AGENT_SHARED_DIRECTORY);
            Privileges.prepareAgentDirectory(releases, Privileges.AGENT_SHARED_DIRECTORY);
            // pnpm add wants a package.json to extend; give it a minimal private one.
            Path manifest = target.resolve("package.json");
            if (!Files.exists(manifest)) {
                Files.writeString(manifest,
                        "{\"name\":\"hdsl-brand-" + brand.id() + "\",\"private\":true}\n",
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BrandException("Failed to prepare " + target, e);
        }

        List<String> command = new ArrayList<>(List.of(
                pnpm, "add", "--dir", target.toString(),
                brand.npmPackage() + "@" + version,
                "--reporter=append-only",
                "--registry=" + registry,
                "--config.dangerously-allow-all-builds=true"));
        // pnpm's default (isolated) layout leaves a package's own dependencies as links
        // beside it inside `.pnpm`, and on Windows those links are not created for a
        // package that ships a `node_modules` directory of its own — Kimi Code does.
        // The install then reports success while the tool cannot resolve `ws` at all
        // (measured on Windows 10, pnpm 11.28.6: `ERR_MODULE_NOT_FOUND: ws`). A hoisted
        // layout puts the dependencies in the project's `node_modules`, which is what
        // these tools were written against; POSIX keeps the isolated layout it had.
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS
                || brand.prefersHoistedNodeModules()) {
            command.add("--config.node-linker=hoisted");
        }
        message.accept("Installing " + brand.npmPackage() + " " + version);
        LOG.info("Installing " + brand.npmPackage() + " " + version + ": " + String.join(" ", command));

        Path log = brandRoot.resolve(INSTALL_LOG);
        try (OutputStreamAppend output = new OutputStreamAppend(log);
                BufferedReader reader = run(command, target, runtime.pathEnvironment(), output)) {
            StringBuilder tail = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.writeLine(line);
                if (tail.length() > 0) {
                    tail.append(" | ");
                }
                tail.append(line);
                if (tail.length() > 2000) {
                    tail.delete(0, tail.length() - 1000);
                }
                message.accept(line);
            }
            int code = output.exitCode();
            if (code != 0) {
                throw new BrandException(brand.npmPackage() + " " + version + " failed to install (exit " + code
                        + "): " + tail);
            }
        } catch (IOException e) {
            throw new BrandException("Failed to run pnpm for " + brand.npmPackage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("Installing " + brand.npmPackage() + " was interrupted", e);
        }

        // The extensionless `.bin/<tool>` is a POSIX shim; on Windows the
        // startable form is `<tool>.cmd` beside it. `Executables.resolve` knows
        // both layouts, so a successful install is not reported as missing just
        // because only the `.cmd` exists there.
        if (!Files.exists(bin)
                && org.jackhuang.hmcl.util.platform.Executables.resolve(bin.toString()).isEmpty()) {
            throw new BrandException(brand.npmPackage() + " " + version
                    + " installed without its executable (" + bin + "); the package's install scripts may have "
                    + "been skipped or failed — see " + log);
        }
        handReleaseToAgent(target);
        pointCurrentAt(brandRoot, target);
        message.accept("Installed " + brand.npmPackage() + " " + version);
        LOG.info("Installed " + brand.npmPackage() + " " + version + " into " + target);
    }

    /// The PI WEB install: one pnpm command adds the web app and the Pi SDK it
    /// is verified against, then an audit records what the release can do.
    ///
    /// `node-pty` has no Linux prebuild (measured on 1.1.0: the tarball carries
    /// darwin and win32 only), so the browser terminal depends on a C++
    /// toolchain there. Upstream ships it as an *optional* dependency, which
    /// means a failed build never fails the install — the web app runs and its
    /// terminal does not. The audit therefore asks the package's own runtime
    /// check (`pi-web-sessiond --check-runtime`, which loads the bundled
    /// terminal provider) instead of guessing from file names.
    private static void installPiWeb(Brand brand, String version, Path brandRoot, Path target,
                                     Sink message, Mode mode) throws BrandException {
        String sdk = brand.pairedSdkVersion(version);
        if (sdk == null) {
            throw new BrandException("no verified Pi pairing for " + brand.sourceLabel() + " " + version
                    + "; the panel installs only the pairs its adapter was written for");
        }
        DshNodeRuntime runtime = DshNodeRuntime.detectWithPnpm()
                .orElseThrow(() -> new BrandException("Node.js was not found on PATH and no panel-managed runtime "
                        + "is installed; installing " + brand.npmPackage() + " needs one. "
                        + "Open Settings -> Node.js for a one-click managed runtime, or install Node.js yourself."));
        if (!runtime.canManagePlugins()) {
            throw new BrandException(runtime.pnpmMissing("installing " + brand.sourceLabel()).getMessage());
        }
        String pnpm = runtime.pnpm().toString();
        String registry = NpmRegistry.effective().registry();

        try {
            Files.createDirectories(target);
            Privileges.prepareAgentDirectory(target, Privileges.AGENT_SHARED_DIRECTORY);
            Privileges.prepareAgentDirectory(target.getParent(), Privileges.AGENT_SHARED_DIRECTORY);
            Path manifest = target.resolve("package.json");
            if (!Files.exists(manifest)) {
                Files.writeString(manifest,
                        "{\"name\":\"hdsl-brand-" + brand.id() + "\",\"private\":true}\n",
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BrandException("Failed to prepare " + target, e);
        }

        List<String> command = new ArrayList<>(List.of(pnpm, "add", "--dir", target.toString()));
        if (mode == Mode.FULL) {
            command.add(brand.npmPackage() + "@" + version);
        }
        command.add(Brand.PI_SDK_PACKAGE + "@" + sdk);
        command.add("--reporter=append-only");
        command.add("--registry=" + registry);
        command.add("--config.dangerously-allow-all-builds=true");
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            command.add("--config.node-linker=hoisted");
        }

        message.accept(mode == Mode.FULL
                ? "Installing " + brand.npmPackage() + " " + version + " with Pi " + sdk
                : "Installing only the Pi terminal (" + Brand.PI_SDK_PACKAGE + "@" + sdk + "), without the web app");
        LOG.info("Installing " + brand.sourceLabel() + " " + version + " (" + mode + "): "
                + String.join(" ", command));

        Path log = brandRoot.resolve(INSTALL_LOG);
        try (OutputStreamAppend output = new OutputStreamAppend(log);
                BufferedReader reader = run(command, target, runtime.pathEnvironment(), output)) {
            StringBuilder tail = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.writeLine(line);
                if (tail.length() > 0) {
                    tail.append(" | ");
                }
                tail.append(line);
                if (tail.length() > 2000) {
                    tail.delete(0, tail.length() - 1000);
                }
                message.accept(line);
            }
            int code = output.exitCode();
            if (code != 0) {
                throw new BrandException(brand.sourceLabel() + " " + version
                        + " failed to install (exit " + code + "): " + tail
                        + (mode == Mode.FULL
                                ? " — if only the web app failed, install the terminal-only release instead"
                                : ""));
            }
        } catch (IOException e) {
            throw new BrandException("Failed to run pnpm for " + brand.sourceLabel(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("Installing " + brand.sourceLabel() + " was interrupted", e);
        }

        Path sdkManifest = target.resolve("node_modules").resolve(Brand.PI_SDK_PACKAGE)
                .resolve("package.json");
        if (!Files.isRegularFile(sdkManifest)) {
            throw new BrandException("The Pi SDK (" + Brand.PI_SDK_PACKAGE + "@" + sdk
                    + ") is missing after the install; see " + log);
        }
        if (mode == Mode.FULL && !Files.exists(target.resolve("node_modules/.bin/pi-web"))) {
            throw new BrandException(brand.npmPackage() + " " + version
                    + " installed without its executable; see " + log);
        }

        boolean pty = mode == Mode.FULL && checkTerminalPrerequisite(target, brandRoot, runtime, message);
        writeReleaseInfo(target, new ReleaseInfo(version, sdk, mode == Mode.FULL, pty,
                System.currentTimeMillis()));
    }

    /// Asks pi-web's own runtime check whether the browser terminal can load.
    ///
    /// The launcher's `--check-runtime` runs `runtimePreflight --terminal`, which
    /// loads the bundled terminal provider and reports the native module. A
    /// non-zero exit is the one authoritative "no PTY here" answer; the install
    /// continues either way, and the release records what it found.
    ///
    /// @return whether the browser terminal is available
    private static boolean checkTerminalPrerequisite(Path target, Path brandRoot, DshNodeRuntime runtime,
                                                     Sink message) {
        Path launcher = target.resolve("node_modules/.bin/pi-web-sessiond");
        if (!Files.exists(launcher)) {
            message.accept("Terminal check skipped: pi-web-sessiond is missing from the release");
            return false;
        }
        List<String> command = List.of(
                org.jackhuang.hmcl.util.platform.Executables.resolve(launcher.toString()).toString(),
                "--check-runtime");
        try (OutputStreamAppend output = new OutputStreamAppend(brandRoot.resolve(INSTALL_LOG));
                BufferedReader reader = run(command, target, runtime.pathEnvironment(), output)) {
            StringBuilder detail = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.writeLine(line);
                detail.append(line).append('\n');
            }
            int code = output.exitCode();
            if (code == 0) {
                message.accept("Browser terminal ready (node-pty loaded)");
                return true;
            }
            message.accept("Browser terminal unavailable (node-pty did not load): " + bounded(detail.toString()));
            message.accept("The web app still runs; its in-browser terminal does not. "
                    + "Install a C++ toolchain (python3, make, g++) and repair the release to enable it.");
            return false;
        } catch (Exception e) {
            message.accept("Could not run the terminal prerequisite check: " + e.getMessage());
            return false;
        }
    }

    private static String bounded(String text) {
        String trimmed = text.strip();
        return trimmed.length() > 500 ? trimmed.substring(trimmed.length() - 500) : trimmed;
    }

    /// Whether a release directory is usable by this brand.
    ///
    /// PI WEB answers on the Pi SDK alone: a terminal-only release has no
    /// `pi-web` executable and is still a release. Every other brand keeps its
    /// single marker.
    static boolean releaseUsable(Brand brand, Path target) {
        if (brand != Brand.PI_WEB) {
            return Files.exists(target.resolve(brand.installMarker()));
        }
        return Files.isRegularFile(target.resolve("node_modules")
                .resolve(Brand.PI_SDK_PACKAGE).resolve("package.json"));
    }

    /// The same, for a caller that needs a specific shape: [Mode#FULL] also
    /// needs the web executable, [Mode#TERMINAL_ONLY] is satisfied by the SDK.
    private static boolean releaseUsable(Brand brand, Path target, Mode mode) {
        if (!releaseUsable(brand, target)) {
            return false;
        }
        return brand != Brand.PI_WEB || mode != Mode.FULL
                || Files.exists(target.resolve("node_modules/.bin/pi-web"));
    }

    /// Reads a PI WEB release's `release.json`, or `null` when it has none.
    ///
    /// Old releases (installed before the audit existed) answer `null`; the
    /// caller treats that as "assume the pair is there", which is what the
    /// files on disk say.
    public static @Nullable ReleaseInfo releaseInfo(Path target) {
        Path file = target.resolve("release.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return new ReleaseInfo(
                    json.has("piWeb") ? json.get("piWeb").getAsString() : "",
                    json.has("piSdk") ? json.get("piSdk").getAsString() : "",
                    !json.has("webAvailable") || json.get("webAvailable").getAsBoolean(),
                    json.has("ptyAvailable") && json.get("ptyAvailable").getAsBoolean(),
                    json.has("installedAt") ? json.get("installedAt").getAsLong() : 0L);
        } catch (RuntimeException | IOException e) {
            LOG.warning("Could not read " + file, e);
            return null;
        }
    }

    private static void writeReleaseInfo(Path target, ReleaseInfo info) throws BrandException {
        JsonObject json = new JsonObject();
        json.addProperty("piWeb", info.version());
        json.addProperty("piSdk", info.pairedSdk());
        json.addProperty("webAvailable", info.webAvailable());
        json.addProperty("ptyAvailable", info.ptyAvailable());
        json.addProperty("installedAt", info.installedAt());
        Path file = target.resolve("release.json");
        try {
            Files.writeString(file, json + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BrandException("Failed to record the release audit in " + file, e);
        }
    }

    /// The GitHub-brand install: download the tag's source zip, unpack it,
    /// install the server and web workspaces, build the web app.
    ///
    /// Deliberately not `git clone`: on this deployment's network github.com's
    /// git transport is cut mid-handshake routinely, while codeload serves the
    /// whole source in about a second and a half — a plain, retryable HTTPS
    /// download that also removes the `git` dependency entirely.
    ///
    /// The desktop workspace is deliberately **not** installed: nothing in it
    /// serves the web UI, and its Electron dependency would download a binary
    /// the panel never runs. (`ELECTRON_SKIP_BINARY_DOWNLOAD` is set anyway as
    /// insurance against a future lockfile that pulls it in through another
    /// workspace.)
    private static void installGitHub(Brand brand, String version, Path brandRoot, Path target,
                                      Sink message) throws BrandException {
        DshNodeRuntime runtime = DshNodeRuntime.detect()
                .or(DshNodeRuntime::detectManaged)
                .orElseThrow(() -> new BrandException("Node.js was not found on PATH and no panel-managed runtime "
                        + "is installed; installing " + brand.sourceLabel() + " needs one. "
                        + "Open Settings -> Node.js for a one-click managed runtime, or install Node.js yourself."));
        String registry = NpmRegistry.effective().registry();
        Path log = brandRoot.resolve(INSTALL_LOG);

        // A leftover half-unpacked attempt would mix with the new sources.
        if (Files.exists(target)) {
            FileUtils.deleteDirectoryQuietly(target);
        }

        String archive = BrandSources.archive().url();
        String url = archive.replaceAll("/$", "") + "/" + brand.githubSlug()
                + "/zip/refs/tags/" + brand.releaseTag(version);
        BrandException downloadFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (attempt > 1) {
                message.accept("Download did not finish; retrying (attempt " + attempt + " of 3)");
            }
            try {
                message.accept("Downloading " + url);
                LOG.info("Downloading " + url + " (attempt " + attempt + " of 3)");
                extractZip(download(url), target, message::cancelled);
                downloadFailure = null;
                break;
            } catch (BrandException e) {
                downloadFailure = e;
            }
        }
        if (downloadFailure != null) {
            throw downloadFailure;
        }

        java.util.Map<String, String> environment = new java.util.HashMap<>(runtime.pathEnvironment());
        environment.put("ELECTRON_SKIP_BINARY_DOWNLOAD", "1");
        // `--include-workspace-root` is load-bearing: the engine package
        // (`claude-web-engine`, the community Claude Code build that speaks
        // OpenAI/Gemini provider protocols) is a dependency of the ROOT
        // project, not of the server workspace — installing only the two
        // workspaces leaves it out, the app silently falls back to the
        // official Claude Code, and every OpenAI-type provider stops working
        // (measured 2026-10-12 on 0.2.1: the startup banner reported `runtime:
        // claude 2.1.283`, the SDK's own binary).
        // The engine tarball is an absolute GitHub URL, and this network drops
        // github.com mid-transfer routinely: npm's own retries plus a whole-run
        // retry loop. `npm ci` rebuilds node_modules from scratch, so a failed
        // run leaves nothing half-installed to reason about.
        List<String> npmCi = List.of("npm", "ci", "-w", "server", "-w", "web", "--include-workspace-root",
                "--registry=" + registry,
                "--fetch-retries=5", "--fetch-retry-maxtimeout=120000", "--fetch-timeout=600000");
        BrandException ciFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (attempt > 1) {
                message.accept("Dependency install did not finish; retrying (attempt " + attempt + " of 3)");
            }
            try {
                runChecked(npmCi, target, environment, log, message, "installing dependencies");
                ciFailure = null;
                break;
            } catch (BrandException e) {
                ciFailure = e;
            }
        }
        if (ciFailure != null) {
            throw ciFailure;
        }
        runChecked(List.of("npm", "run", "build", "-w", "web"),
                target, environment, log, message, "building the web app");

        if (!Files.exists(target.resolve(brand.installMarker()))) {
            throw new BrandException(brand.sourceLabel() + " " + version + " built without its web app ("
                    + target.resolve(brand.installMarker()) + ") — see " + log);
        }
    }

    /// The Octop install: a prebuilt portable archive from the GitHub release,
    /// then the patched dashboard on top.
    ///
    /// The archive already carries CPython 3.12 and every Python dependency, so
    /// the panel does not need a Python toolchain (or Docker) to install it.
    /// Only the SPA is replaced: upstream builds for `/`, while this instance
    /// is mounted at `/i/<token>/` and needs the runtime-base patch.
    private static void installOctop(Brand brand, String version, Path brandRoot, Path target,
                                     Sink message) throws BrandException {
        String platform = octopPlatform();
        String asset = "Octop-portable-" + platform + "-" + version + ".zip";
        BrandSources.Effective release = BrandSources.release(brand);
        String base = release.url();
        if (base.isBlank()) {
            throw new BrandException("no release base is configured for " + brand.id());
        }
        String url = base.replaceAll("/$", "") + "/" + brand.releaseTag(version) + "/" + asset;

        if (Files.exists(target)) {
            FileUtils.deleteDirectoryQuietly(target);
        }
        int total = 2;
        stage(message, 0, total, "下载 portable 包（约 260 MB）", 0.0, 0.9, 120, () -> {
            message.accept("Downloading " + url + " (source: " + release.source() + ")");
            LOG.info("Downloading Octop portable archive " + url);
            extractZip(download(url, message, "下载 portable 包"), target, message::cancelled);
        });
        stage(message, 1, total, "换成打过挂载点补丁的 dashboard", 0.9, 0.1, 20, () -> {
            markExecutable(target.resolve("runtime/bin"));
            Path dashboard = target.resolve("packages/octop/dashboard");
            if (!Files.isDirectory(dashboard)) {
                throw new BrandException("Octop archive has no packages/octop/dashboard: " + target);
            }
            FileUtils.deleteDirectoryQuietly(dashboard);
            String resource = "/octop/octop-dashboard-" + version + ".zip";
            try (InputStream patched = BrandInstaller.class.getResourceAsStream(resource)) {
                if (patched == null) {
                    throw new BrandException("This panel build has no patched Octop dashboard for " + version
                            + " (" + resource + "); use the version bundled with this panel");
                }
                extractZip(patched, dashboard, message::cancelled);
            } catch (IOException e) {
                throw new BrandException("Could not unpack the patched Octop dashboard: " + e.getMessage(), e);
            }
            if (!Files.exists(dashboard.resolve("index.html"))) {
                throw new BrandException("Octop " + version + " installed without its dashboard ("
                        + dashboard.resolve("index.html") + ")");
            }
            writeMarker(target, "package", version);
        });
        message.report("Installed Octop " + version + " with the patched dashboard · 100%", 1.0);
    }

    /// The last-resort source build: tag archive → dashboard patch/build →
    /// `uv sync` (CPython 3.12 + dependencies) → `.venv/bin/octop`.
    ///
    /// Only Linux/macOS. Windows keeps the portable path: compiling the Python
    /// dependency tree there needs a toolchain the panel deliberately does not
    /// manage, and the portable archive already works.
    static void installOctopSource(Brand brand, String version, Path brandRoot, Path target,
                                   Progress progress) throws BrandException {
        Sink message = new Sink(progress);
        // Keep the previous release until the new build is complete: a failed
        // source build must not take a working package install with it.
        Path backup = target.resolveSibling(target.getFileName() + PREVIOUS_SUFFIX);
        try {
            if (Files.exists(backup)) {
                FileUtils.deleteDirectoryQuietly(backup);
            }
            if (Files.exists(target)) {
                Files.move(target, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BrandException("Could not stage the Octop source build: " + e.getMessage(), e);
        }
        boolean complete = false;
        try {
            installOctopSourceInto(brand, version, brandRoot, target, message);
            complete = true;
        } finally {
            if (complete) {
                FileUtils.deleteDirectoryQuietly(backup);
                // The pointer is what the page shows as 「当前」 and what an older default used;
                // a source build used to leave it on the release it had just deleted, which made a
                // finished build look like it had never happened.
                pointCurrentAt(brandRoot, target);
            } else {
                FileUtils.deleteDirectoryQuietly(target);
                if (Files.exists(backup)) {
                    try {
                        Files.move(backup, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        LOG.warning("Could not restore the previous Octop release at " + target, e);
                    }
                }
            }
        }
    }

    private static void installOctopSourceInto(Brand brand, String version, Path brandRoot, Path target,
                                               Sink message) throws BrandException {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            throw new BrandException("local source compilation is not supported on Windows; "
                    + "turn sourceBuild off and use the prebuilt portable archive");
        }
        if (Files.exists(target)) {
            FileUtils.deleteDirectoryQuietly(target);
        }
        String archive = BrandSources.archive().url();
        String url = archive.replaceAll("/$", "") + "/" + brand.githubSlug()
                + "/zip/refs/tags/" + brand.releaseTag(version);
        Path log = brandRoot.resolve(INSTALL_LOG);
        int total = 7;

        stage(message, 0, total, "下载源码", 0.00, 0.05, 20, () -> {
            message.accept("Downloading source " + url);
            extractZip(download(url, message, "下载源码"), target, message::cancelled);
        });

        stage(message, 1, total, "应用 dashboard 补丁", 0.05, 0.02, 5, () -> {
            Path patch = target.resolve(".hdsl-octop.patch");
            try (InputStream in = BrandInstaller.class.getResourceAsStream("/octop/octop-runtime-base.patch")) {
                if (in == null) {
                    throw new BrandException("This panel build has no Octop dashboard patch");
                }
                Files.copy(in, patch, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new BrandException("Could not write the Octop dashboard patch: " + e.getMessage(), e);
            }
            runChecked(List.of("git", "apply", "--whitespace=nowarn", patch.toString()),
                    target, Map.of(), log, message, "applying the dashboard patch");
        });

        String registry = NpmRegistry.effective().registry();
        Path dashboard = target.resolve("dashboard");
        Map<String, String> npmEnv = new HashMap<>();
        npmEnv.put("NPM_CONFIG_REGISTRY", registry);
        stage(message, 2, total, "安装前端依赖（npm ci）", 0.07, 0.35, 240, () ->
                runWithPackageCount(dashboard.resolve("node_modules"), dashboard.resolve("package-lock.json"),
                        message, "npm ci", () ->
                                runChecked(List.of("npm", "ci", "--no-audit", "--no-fund", "--registry=" + registry),
                                        dashboard, npmEnv, log, message, "installing dashboard dependencies")));
        npmEnv.put("VITE_BASE_PATH", "./");
        npmEnv.put("NODE_OPTIONS", "--max-old-space-size=2048");
        // `build`, not `build:docker`: the former is `tsc -b && vite build`, and Octop's tsconfig
        // is strict + noUnusedLocals/Parameters. Thirty seconds of type checking during a
        // fifty-minute install buys "a missing import fails the install" instead of "the user
        // opens the page and sees a blank screen" — the TeamDrawer `Link` import was exactly that.
        stage(message, 3, total, "构建 dashboard（含类型检查）", 0.42, 0.15, 150, () ->
                runChecked(List.of("npm", "run", "build"),
                        dashboard, npmEnv, log, message, "building the dashboard"));
        // The built SPA is under src/octop/dashboard; the 4+ GiB dependency
        // tree is no longer needed by the runtime or a future launch.
        FileUtils.deleteDirectoryQuietly(dashboard.resolve("node_modules"));

        BrandSources.Effective pypi = BrandSources.pypi();
        BrandSources.Effective pythonMirror = BrandSources.python();
        Path[] uvHolder = new Path[1];
        stage(message, 4, total, "准备 uv", 0.57, 0.08, 30, () ->
                uvHolder[0] = ensureUv(brandRoot, log, message));
        Path uv = uvHolder[0];
        message.accept("下载源：PyPI=" + pypi.url() + " (" + pypi.source() + ") · CPython="
                + pythonMirror.url() + " (" + pythonMirror.source() + ")");
        Map<String, String> pythonEnv = new HashMap<>();
        pythonEnv.put("UV_CACHE_DIR", brandRoot.resolve("cache/uv").toString());
        pythonEnv.put("UV_PYTHON_INSTALL_DIR", brandRoot.resolve("toolchain/python").toString());
        pythonEnv.put("UV_LINK_MODE", "copy");
        pythonEnv.put("PYTHONDONTWRITEBYTECODE", "1");
        // uv reads both names; the deprecated one is kept for older toolchains and for the
        // build scripts some dependencies run themselves.
        pythonEnv.put("UV_DEFAULT_INDEX", pypi.url());
        pythonEnv.put("PIP_INDEX_URL", pypi.url());
        if (!BrandSources.PYTHON_DEFAULT.equals(pythonMirror.url())) {
            pythonEnv.put("UV_PYTHON_INSTALL_MIRROR", pythonMirror.url());
        }
        stage(message, 5, total, "安装 Python 依赖（uv sync + CPython 3.12）", 0.65, 0.30, 300, () -> {
            UvWatcher uvProgress = new UvWatcher(message, brandRoot,
                    target.resolve(".venv"), brandRoot.resolve("cache/uv/archive-v0"));
            try (uvProgress) {
                runChecked(List.of(uv.toString(), "sync", "--frozen", "--no-dev", "--python", "3.12"),
                        target, pythonEnv, log, uvProgress, "installing the Python environment");
            }
        });

        stage(message, 6, total, "收尾", 0.95, 0.05, 5, () -> {
            Path octop = target.resolve(".venv/bin/octop");
            if (!Files.isRegularFile(octop)) {
                throw new BrandException("source build produced no .venv/bin/octop in " + target);
            }
            writeMarker(target, "source", version);
        });
        message.report("Installed Octop " + version + " from source · 100%", 1.0);
    }


    // ---------------------------------------------------------------- Hermes --
    //
    // Hermes is installed from the tag's source zip and built locally, because
    // upstream publishes no wheel for a plain install (its `setup.py` refuses
    // wheel/sdist builds outside a Nix sandbox; editable installs are the
    // sanctioned path, which is exactly what `uv sync` does).
    //
    // Three things come from the version's **own** `pm/lock.json` rather than
    // from constants in this file: the uv release, the CPython build, and their
    // download artifacts. A pinned uv here would be the next version's bug —
    // v0.22.5's `uv.lock` is revision 5 and the panel's 0.7.13 cannot parse it
    // (measured: "invalid lock revision"), the same for 0.9.9.
    //
    // The release directory is **not** renamed into place afterwards (unlike
    // Octop): the venv is an editable install whose `direct_url.json` and PEP
    // 660 finder carry absolute paths, so a rename breaks `hermes` itself.

    /// `<brandRoot>/toolchain`, created.
    ///
    /// Every artifact this installer stages — uv, CPython — lands in here, and both
    /// of them need a directory to download into before anything else can happen.
    /// The creation belongs here rather than at the call sites: the first version of
    /// this code wrote `Files.createTempFile(toolchain, …)` without it and every
    /// install died with `NoSuchFileException` in "准备 uv".
    static Path toolchainDirectory(Path brandRoot) throws IOException {
        Path toolchain = brandRoot.resolve("toolchain");
        Files.createDirectories(toolchain);
        return toolchain;
    }

    /// The key `pm/lock.json` files its artifacts under, or `null` on a
    /// platform upstream does not ship for.
    static @Nullable String hermesPlatform() {
        String arch = switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "x64";
            case ARM64 -> "arm64";
            default -> null;
        };
        if (arch == null) {
            return null;
        }
        return switch (OperatingSystem.CURRENT_OS) {
            case LINUX -> "linux-" + arch;
            case MACOS -> "darwin-" + arch;
            default -> null;
        };
    }

    /// One downloadable artifact: content address plus the fallback URL.
    record HermesArtifact(String sha256, String url) {
    }

    /// What `pm/lock.json` pins for one Hermes version.
    record HermesLock(String uvVersion, HermesArtifact uv, String pythonVersion, HermesArtifact python) {
    }

    /// Where upstream publishes every artifact it pins, by content address
    /// (measured 1.9 MB/s against 41 KB/s from the origins themselves).
    static final String HERMES_CDN = "https://hermes-assets.nousresearch.com/upstream/sha256/";

    /// Reads the versions and artifacts one release pins.
    ///
    /// @param release the unpacked source tree
    /// @return the four values this installer needs
    /// @throws BrandException when the file is missing, unreadable, or does not
    ///                          name this platform
    static HermesLock readHermesLock(Path release) throws BrandException {
        Path file = release.resolve("pm").resolve("lock.json");
        String platform = hermesPlatform();
        if (platform == null) {
            throw new BrandException("Hermes has no build for " + OperatingSystem.CURRENT_OS
                    + "/" + Architecture.CURRENT_ARCH + " (pm/lock.json names no artifacts for it)");
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject packages = root.getAsJsonObject("packages");
            JsonObject uv = packages.getAsJsonObject("uv");
            JsonObject python = packages.getAsJsonObject("python");
            return new HermesLock(
                    uv.get("version").getAsString(),
                    artifactOf(uv, platform),
                    python.get("version").getAsString(),
                    artifactOf(python, platform));
        } catch (IOException | RuntimeException e) {
            throw new BrandException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    private static HermesArtifact artifactOf(JsonObject entry, String platform) throws BrandException {
        JsonObject artifacts = entry.getAsJsonObject("artifacts");
        JsonObject artifact = artifacts == null ? null : artifacts.getAsJsonObject(platform);
        if (artifact == null) {
            throw new BrandException("pm/lock.json pins no artifact for " + platform);
        }
        return new HermesArtifact(artifact.get("sha256").getAsString(), artifact.get("url").getAsString());
    }

    /// Downloads an artifact by content address, falling back to the URL the
    /// lock names, and verifies the digest either way.
    static void downloadArtifact(HermesArtifact artifact, Path destination, Consumer<String> message)
            throws BrandException {
        String fromCdn = HERMES_CDN + artifact.sha256();
        try {
            message.accept("Downloading " + fromCdn);
            copyVerified(download(fromCdn), destination, artifact.sha256());
            return;
        } catch (BrandException e) {
            message.accept("Upstream CDN did not serve it (" + e.getMessage() + "); trying " + artifact.url());
        }
        copyVerified(download(artifact.url()), destination, artifact.sha256());
    }

    private static void copyVerified(InputStream in, Path destination, String expectedSha)
            throws BrandException {
        try (in) {
            Files.createDirectories(destination.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (OutputStream out = Files.newOutputStream(destination,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(expectedSha)) {
                Files.deleteIfExists(destination);
                throw new BrandException("Downloaded artifact does not match its sha256 (expected "
                        + expectedSha + ", got " + actual + ")");
            }
        } catch (IOException e) {
            throw new BrandException("Could not store " + destination + ": " + e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new BrandException("SHA-256 is unavailable in this JVM", e);
        }
    }

    /// Unpacks a `.tar.gz` (the shape both of Hermes' toolchain artifacts have),
    /// keeping symbolic links — CPython's `bin/python3` is a link to the
    /// interpreter, and a copy would break `hermes`'s shebang resolution.
    static void extractTarGz(Path archive, Path destination, java.util.function.BooleanSupplier cancelled)
            throws BrandException {
        try (InputStream raw = Files.newInputStream(archive);
             InputStream gzip = new GZIPInputStream(raw);
             kala.compress.archivers.tar.TarArchiveInputStream tar =
                     new kala.compress.archivers.tar.TarArchiveInputStream(gzip)) {
            kala.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (cancelled.getAsBoolean()) {
                    throw new BrandException("Install cancelled");
                }
                Path target = resolveInside(destination, entry.getName());
                if (target == null) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                if (entry.isSymbolicLink()) {
                    Files.deleteIfExists(target);
                    Files.createSymbolicLink(target, Path.of(entry.getLinkName()));
                    continue;
                }
                try (OutputStream out = Files.newOutputStream(target,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    tar.transferTo(out);
                }
                if ((entry.getMode() & 0111) != 0) {
                    try {
                        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (UnsupportedOperationException ignored) {
                        // Windows: no mode bits.
                    }
                }
            }
        } catch (IOException e) {
            throw new BrandException("Could not unpack " + archive.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static @Nullable Path resolveInside(Path root, String name) {
        Path candidate = root.resolve(name).normalize();
        return candidate.startsWith(root.normalize()) ? candidate : null;
    }

    /// The install itself: eleven stages, each one a step the panel shows.
    ///
    /// @param brand     the brand (for messages)
    /// @param version   the exact version
    /// @param brandRoot `<data>/brands/hermes`
    /// @param target    `releases/<version>`, written **in place**
    static void installHermes(Brand brand, String version, Path brandRoot, Path target, Sink message)
            throws BrandException {
        String unsupported = brand.notInstallableReason(OperatingSystem.CURRENT_OS);
        if (unsupported != null) {
            throw new BrandException(unsupported);
        }
        String platform = hermesPlatform();
        if (platform == null) {
            throw new BrandException("Hermes has no build for " + OperatingSystem.CURRENT_OS
                    + "/" + Architecture.CURRENT_ARCH);
        }
        String knownRequirement = brand.knownNodeRequirement(version);
        if (knownRequirement != null) {
            // Fail before the four-minute source download, not after npm ci:
            // upstream's .npmrc sets engine-strict, so a wrong Node is a hard
            // failure rather than a warning.
            requireHermesNode(brand, version, knownRequirement);
        }
        if (Files.exists(target)) {
            // In place, never renamed: an editable venv carries absolute paths.
            FileUtils.deleteDirectoryQuietly(target);
        }
        Path log = brandRoot.resolve(INSTALL_LOG);
        int total = 11;

        String archive = BrandSources.archive().url();
        String url = archive.replaceAll("/$", "") + "/" + brand.githubSlug() + "/zip/refs/tags/"
                + brand.releaseTag(version);
        stage(message, 0, total, "下载源码", 0.00, 0.10, 240, () -> {
            message.accept("Downloading " + url);
            // The four minutes this takes at 0.4 MB/s are the longest single step of
            // the install; without feeding the byte counter into the *overall* bar,
            // the page sits at 0% and looks dead while it is in fact downloading.
            extractZip(download(url, message, "下载源码", 0.00, 0.10), target, message::cancelled);
        });

        HermesLock lock = readHermesLock(target);
        message.accept("Lock: uv " + lock.uvVersion() + " · CPython " + lock.pythonVersion());

        BrandSources.Effective pypi = BrandSources.pypi();
        message.accept("下载源：PyPI=" + pypi.url() + " (" + pypi.source() + ") · npm="
                + NpmRegistry.effective().registry());
        if (BrandSources.PYPI_DEFAULT.equals(pypi.url())) {
            // Measured on this deployment: pythonhosted.org answers at about 80 KB/s,
            // which turns a 285 MB environment into hours. Saying so at the start is
            // cheaper than letting somebody conclude the installer hung.
            message.accept("注意：PyPI 用的是官方源（实测约 80 KB/s，装完要数小时）。"
                    + "到「设置 → 下载源」选一个国内镜像会快一个数量级。");
        }

        Path[] uvHolder = new Path[1];
        stage(message, 1, total, "准备 uv " + lock.uvVersion(), 0.10, 0.03, 30, () -> ioStep(() -> {
            Path toolchain = toolchainDirectory(brandRoot);
            Path uv = toolchain.resolve("uv");
            if (!Files.isRegularFile(uv)) {
                Path archiveFile = Files.createTempFile(toolchain, "uv", ".tar.gz");
                try {
                    downloadArtifact(lock.uv(), archiveFile, message);
                    Path unpacked = Files.createTempDirectory(toolchain, "uv-");
                    extractTarGz(archiveFile, unpacked, message::cancelled);
                    Path binary;
                    try (Stream<Path> walk = Files.walk(unpacked, 3)) {
                        binary = walk.filter(path -> path.getFileName().toString().equals("uv"))
                                .filter(Files::isRegularFile)
                                .findFirst()
                                .orElseThrow(() -> new BrandException(
                                        "the uv archive holds no `uv` binary"));
                    }
                    Files.move(binary, uv, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.setPosixFilePermissions(uv, PosixFilePermissions.fromString("rwxr-xr-x"));
                    FileUtils.deleteDirectoryQuietly(unpacked);
                } finally {
                    Files.deleteIfExists(archiveFile);
                }
            }
            uvHolder[0] = uv;
        }));

        Path[] pythonHolder = new Path[1];
        stage(message, 2, total, "准备 CPython " + lock.pythonVersion(), 0.13, 0.12, 120, () -> ioStep(() -> {
            Path pythonRoot = toolchainDirectory(brandRoot).resolve("python");
            Files.createDirectories(pythonRoot);
            Path destination = pythonRoot.resolve(lock.pythonVersion());
            Path python = findInterpreter(destination);
            if (python == null) {
                Path archiveFile = Files.createTempFile(pythonRoot, "python", ".tar.gz");
                try {
                    downloadArtifact(lock.python(), archiveFile, message);
                    extractTarGz(archiveFile, destination, message::cancelled);
                } finally {
                    Files.deleteIfExists(archiveFile);
                }
                python = findInterpreter(destination);
            }
            if (python == null) {
                throw new BrandException("the CPython archive holds no interpreter under " + destination);
            }
            pythonHolder[0] = python;
        }));

        Path[] lockBackup = new Path[1];
        stage(message, 3, total, "改写 lock 的下载源", 0.25, 0.02, 20, () -> ioStep(() -> {
            Path lockFile = target.resolve("uv.lock");
            String mirror = BrandSources.pypi().url();
            String origin = originOf(mirror);
            if (origin.isEmpty() || mirror.startsWith("https://pypi.org")) {
                // Nothing to rewrite: the packages are already where uv looks.
                return;
            }
            String text = Files.readString(lockFile, StandardCharsets.UTF_8);
            String rewritten = text.replace("https://files.pythonhosted.org/packages/",
                    origin + "/packages/");
            if (!rewritten.equals(text)) {
                Path backup = lockFile.resolveSibling("uv.lock.orig");
                Files.copy(lockFile, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(lockFile, rewritten, StandardCharsets.UTF_8);
                lockBackup[0] = backup;
                message.accept("Rewrote the package hosts in uv.lock to " + origin);
            }
        }));

        Map<String, String> pythonEnv = new HashMap<>();
        pythonEnv.put("UV_CACHE_DIR", brandRoot.resolve("cache/uv").toString());
        pythonEnv.put("UV_LINK_MODE", "copy");
        pythonEnv.put("PYTHONDONTWRITEBYTECODE", "1");
        pythonEnv.put("UV_NO_PROGRESS", "1");
        pythonEnv.put("PIP_INDEX_URL", BrandSources.pypi().url());
        stage(message, 4, total, "安装 Python 依赖（uv sync --extra all）", 0.27, 0.49, 300, () ->
                runChecked(List.of(uvHolder[0].toString(), "sync", "--frozen", "--no-dev",
                                "--python", pythonHolder[0].toString(), "--extra", "all"),
                        target, pythonEnv, log, message, "installing the Python environment"));

        Path venv = target.resolve(".venv");
        Path venvPython = venv.resolve("bin/python");
        stage(message, 5, total, "写 install-stamp", 0.76, 0.02, 10, () -> {
            Path script = target.resolve("scripts/write_install_stamp.py");
            if (!Files.isRegularFile(script)) {
                throw new BrandException("the source has no scripts/write_install_stamp.py");
            }
            List<String> command = new ArrayList<>(List.of(venvPython.toString(), script.toString(),
                    "-o", target.resolve("install-stamp.json").toString(),
                    "--base-version", version,
                    "--update-mechanism", "external",
                    "--source", "zip"));
            String commit = tagCommit(brand, version);
            if (!commit.isEmpty()) {
                command.add("--commit");
                command.add(commit);
            }
            runChecked(command, target, Map.of(), log, message, "writing the install stamp");
        });

        // The source declares its own engines.node; prefer that over the static
        // threshold, and use the Node this brand resolves to — the same one a
        // launch will use. Silently building on another runtime would only move
        // the failure to "the server will not start".
        String declared = BrandNodeRequirement.ofDirectory(target);
        String requirement = declared != null ? declared : knownRequirement;
        DshNodeRuntime node;
        if (requirement != null) {
            node = requireHermesNode(brand, version, requirement);
            message.accept("构建用 Node " + node.nodeVersion() + "（要求 " + requirement + "）");
        } else {
            node = DshNodeRuntime.detect()
                    .or(DshNodeRuntime::detectManaged)
                    .orElseThrow(() -> new BrandException("Node.js was not found on PATH and no panel-managed "
                            + "runtime is installed; building Hermes needs one (Settings -> Node.js)"));
        }
        Map<String, String> nodeEnv = new HashMap<>(node.pathEnvironment());
        nodeEnv.put("npm_config_registry", NpmRegistry.effective().registry());
        Path products = brandRoot.resolve("products").resolve(version);
        Path icons = products.resolve("icons");
        Path tuiOut = products.resolve("tui");
        Path webOut = target.resolve("hermes_cli/web_dist");

        stage(message, 6, total, "生成图标", 0.78, 0.03, 30, () -> ioStep(() -> {
            Files.createDirectories(icons);
            Map<String, String> env = new HashMap<>(nodeEnv);
            env.put("HERMES_PYTHON", venvPython.toString());
            runChecked(List.of("node", "scripts/generate-icons.mjs",
                            "--source", target.toString(), "--out", icons.toString()),
                    target, env, log, message, "generating the icons");
        }));

        stage(message, 7, total, "安装前端依赖（node-deps）", 0.81, 0.08, 120, () ->
                runChecked(List.of("node", "scripts/build/node-deps.mjs",
                                "--source", target.toString(),
                                "--workspace", "ui-tui", "--workspace", "web"),
                        target, nodeEnv, log, message, "installing the front-end dependencies"));

        stage(message, 8, total, "构建 web 工作台", 0.89, 0.06, 120, () ->
                runChecked(List.of("node", "scripts/build/web.mjs",
                                "--source", target.toString(),
                                "--icons", icons.toString(),
                                "--out", webOut.toString()),
                        target, nodeEnv, log, message, "building the web dashboard"));

        stage(message, 9, total, "构建 TUI", 0.95, 0.03, 60, () -> ioStep(() -> {
            runChecked(List.of("node", "scripts/build/tui.mjs",
                            "--source", target.toString(), "--out", tuiOut.toString()),
                    target, nodeEnv, log, message, "building the TUI");
            Path tuiDist = target.resolve("hermes_cli/tui_dist");
            FileUtils.deleteDirectoryQuietly(tuiDist);
            Files.createDirectories(tuiDist);
            Path built = tuiOut.resolve("dist");
            if (!Files.isDirectory(built)) {
                throw new BrandException("the TUI build produced no dist under " + tuiOut);
            }
            copyTree(built, tuiDist);
            Path packageJson = tuiOut.resolve("package.json");
            if (Files.isRegularFile(packageJson)) {
                Files.copy(packageJson, tuiDist.resolve("package.json"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }));

        stage(message, 10, total, "收尾（恢复 lock、清理、断言）", 0.98, 0.02, 30, () -> ioStep(() -> {
            if (lockBackup[0] != null) {
                Files.move(lockBackup[0], target.resolve("uv.lock"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            pruneHermesBuild(target, brandRoot, products);
            for (String marker : List.of("hermes_cli/web_dist/index.html",
                    "hermes_cli/tui_dist/entry.js", ".venv/bin/hermes", "install-stamp.json")) {
                if (!Files.exists(target.resolve(marker))) {
                    throw new BrandException("Hermes " + version + " built without " + marker);
                }
            }
        }));
        message.report("Installed Hermes " + version + " from source · 100%", 1.0);
    }

    /// The Node this brand resolves to, checked against a release's declared
    /// range.
    ///
    /// The runtime must be the one a launch would use: Hermes' launcher and its
    /// build share PATH, and upstream's `engine-strict` turns the mismatch into
    /// an `npm ci` failure. Building on a different runtime would make the
    /// install succeed and the start fail — the failure the panel exists to
    /// prevent, just moved one step later.
    private static DshNodeRuntime requireHermesNode(Brand brand, String version, String range)
            throws BrandException {
        org.jackhuang.hmcl.dsh.NodeSelection.Resolution current = BrandRuntime.currentNode(brand);
        if (current.runtime() != null
                && NodeSemver.satisfies(current.runtime().nodeVersion(), range)) {
            return current.runtime();
        }
        throw new BrandException(hermesNodeGuidance(version, range, current));
    }

    /// One actionable sentence: what the release needs, what the panel would
    /// actually run, which installed runtimes would do, and where to switch.
    private static String hermesNodeGuidance(String version, String range,
                                             org.jackhuang.hmcl.dsh.NodeSelection.Resolution current) {
        List<String> compatible = new ArrayList<>();
        for (NodeRuntime runtime : NodeRuntimeManager.listInstalled()) {
            if (NodeSemver.satisfies(runtime.version(), range)) {
                compatible.add(runtime.version() + "（面板托管）");
            }
        }
        if (DshNodeRuntime.detect()
                .filter(runtime -> NodeSemver.satisfies(runtime.nodeVersion(), range)).isPresent()) {
            compatible.add("PATH 上的 Node");
        }
        String now = current.version() == null ? "未找到可用的 Node" : current.version();
        String fix = compatible.isEmpty()
                ? "请到「设置 → Node.js」安装 Node 26，再把品牌/实例的 Node 运行时设成它"
                : "面板里已有满足要求的 " + String.join("、", compatible)
                        + "：到「设置 → 品牌实例」把 hermes 的 Node 运行时切到它"
                        + "（或在该实例上单独指定）";
        return "Hermes " + version + " 要求 Node " + range
                + "（上游 .npmrc 开了 engine-strict，Node 版本不对时 npm ci 会直接失败）。"
                + "当前使用的是 " + now + "。" + fix + "，然后重新安装。";
    }

    /// The interpreter inside an unpacked CPython artifact (`python/bin/python3.14`
    /// in the `install_only` archives).
    static @Nullable Path findInterpreter(Path root) {
        if (!Files.isDirectory(root)) {
            return null;
        }
        try (Stream<Path> walk = Files.walk(root, 4)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getParent() != null
                            && "bin".equals(path.getParent().getFileName().toString()))
                    .filter(path -> path.getFileName().toString().matches("python3\\.\\d+"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /// `https://host[:port]` of a URL, or `""`.
    static String originOf(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (RuntimeException e) {
            return "";
        }
    }

    /// The commit a release tag points at, for the install stamp.
    ///
    /// The REST API's `/commits/<tag>` answers the commit itself; `git
    /// rev-parse` on an annotated tag would give the tag object's sha, which
    /// upstream's stamp treats as a fallback and discards.
    static String tagCommit(Brand brand, String version) {
        String slug = brand.githubSlug();
        if (slug.isEmpty()) {
            return "";
        }
        String url = "https://api.github.com/repos/" + slug + "/commits/" + brand.releaseTag(version);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOG.info("GitHub answered " + response.statusCode() + " for " + url);
                return "";
            }
            return JsonParser.parseString(response.body()).getAsJsonObject()
                    .get("sha").getAsString();
        } catch (IOException | RuntimeException e) {
            LOG.info("Could not resolve the commit of " + brand.releaseTag(version) + ": " + e.getMessage());
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    /// Deletes what the build needed and the runtime does not: the dependency
    /// trees and the staging products (983 MB → 570 MB, measured).
    static void pruneHermesBuild(Path target, Path brandRoot, Path products) {
        for (String relative : List.of("node_modules", "web/node_modules", "ui-tui/node_modules",
                "tests-js/node_modules")) {
            FileUtils.deleteDirectoryQuietly(target.resolve(relative));
        }
        Path apps = target.resolve("apps");
        if (Files.isDirectory(apps)) {
            try (Stream<Path> entries = Files.list(apps)) {
                for (Path app : entries.toList()) {
                    FileUtils.deleteDirectoryQuietly(app.resolve("node_modules"));
                }
            } catch (IOException e) {
                LOG.info("Could not prune " + apps + ": " + e.getMessage());
            }
        }
        FileUtils.deleteDirectoryQuietly(products);
    }

    private static void copyTree(Path source, Path destination) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path relative = source.relativize(path);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /// Finds a usable `uv`: PATH first, then a per-brand copy downloaded from
    /// the official release tarball. The copy lives under the persisted brand
    /// directory, so a later reinstall does not fetch it again.
    private static Path ensureUv(Path brandRoot, Path log, Consumer<String> message) throws BrandException {
        Path toolchain = brandRoot.resolve("toolchain");
        Path binary = toolchain.resolve("uv");
        if (Files.isRegularFile(binary)) {
            return binary;
        }
        try {
            Process probe = new ProcessBuilder("uv", "--version")
                    .redirectErrorStream(true).start();
            if (probe.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) && probe.exitValue() == 0) {
                return Path.of("uv");
            }
            probe.destroyForcibly();
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        // The configured index first: a mirror serves the same wheel, and on the deployment this was
        // written for the official CDN answers at about 40 KB/s while domestic mirrors do ~6.6 MB/s.
        BrandSources.Effective index = BrandSources.pypi();
        try {
            return installUvFromIndex(index.url(), toolchain, binary, message);
        } catch (BrandException e) {
            message.accept("uv from " + index.url() + " unavailable (" + e.getMessage() + ")");
        }
        if (!BrandSources.PYPI_DEFAULT.equals(index.url())) {
            // PyPI's JSON API is not mirrored by most mirrors, so trying it after a failed
            // mirror is only worth it when the index was not pypi.org itself.
            try {
                return installUvFromPyPI(toolchain, binary, message);
            } catch (BrandException e) {
                message.accept("PyPI uv unavailable (" + e.getMessage() + "); trying GitHub");
            }
        } else {
            message.accept("PyPI uv unavailable; trying GitHub");
        }
        String arch = switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "x86_64";
            case ARM64 -> "aarch64";
            default -> throw new BrandException("no uv release for " + Architecture.CURRENT_ARCH);
        };
        // The GitHub fallback has to name the right triple: the old hardcoded
        // `unknown-linux-gnu` downloaded a Linux binary on macOS and then failed
        // with an exec error instead of saying what was wrong.
        String triple = switch (OperatingSystem.CURRENT_OS) {
            case LINUX -> arch + "-unknown-linux-gnu";
            case MACOS -> arch + "-apple-darwin";
            default -> throw new BrandException(
                    "uv has no GitHub release for " + OperatingSystem.CURRENT_OS);
        };
        String url = "https://github.com/astral-sh/uv/releases/download/" + UV_VERSION + "/uv-"
                + triple + ".tar.gz";
        Path archive = toolchain.resolve("uv.tar.gz");
        try {
            Files.createDirectories(toolchain);
            message.accept("Downloading uv " + url);
            try (InputStream in = download(url)) {
                Files.copy(in, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            runChecked(List.of("tar", "-xzf", archive.toString(), "-C", toolchain.toString()),
                    toolchain, Map.of(), log, message, "unpacking uv");
            Path extracted = toolchain.resolve("uv-" + triple).resolve("uv");
            if (!Files.isRegularFile(extracted)) {
                throw new BrandException("uv archive did not contain uv-" + triple + "/uv");
            }
            Files.move(extracted, binary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.setPosixFilePermissions(binary,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.deleteIfExists(archive);
            return binary;
        } catch (IOException e) {
            throw new BrandException("Could not install uv: " + e.getMessage(), e);
        }
    }

    /// Downloads the uv wheel from PyPI and extracts `uv` from
    /// `uv-<version>.data/scripts/uv`. The wheel is the same static binary as
    /// the GitHub tarball, but PyPI's CDN is reachable where GitHub's
    /// release-asset redirect is not.
    private static Path installUvFromPyPI(Path toolchain, Path binary, Consumer<String> message)
            throws BrandException {
        String arch = switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "x86_64";
            case ARM64 -> "aarch64";
            default -> throw new BrandException("no uv wheel for " + Architecture.CURRENT_ARCH);
        };
        String jsonUrl = "https://pypi.org/pypi/uv/" + UV_VERSION + "/json";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(jsonUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new BrandException("PyPI answered " + response.statusCode() + " for " + jsonUrl);
            }
            JsonObject document = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray urls = document.getAsJsonArray("urls");
            String chosen = null;
            for (com.google.gson.JsonElement element : urls) {
                JsonObject file = element.getAsJsonObject();
                String filename = file.get("filename").getAsString();
                if (filename.endsWith(".whl") && filename.contains("manylinux")
                        && filename.contains(arch)) {
                    chosen = file.get("url").getAsString();
                    break;
                }
            }
            if (chosen == null) {
                throw new BrandException("no manylinux " + arch + " uv wheel on PyPI");
            }
            message.accept("Downloading uv from PyPI " + chosen);
            return extractUvWheel(chosen, toolchain, binary, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("uv download was interrupted", e);
        } catch (IOException e) {
            throw new BrandException("Could not fetch uv from PyPI: " + e.getMessage(), e);
        }
    }

    /// Downloads the uv wheel named by the configured PyPI index and extracts it.
    ///
    /// A mirror's simple page is one HTML file listing the wheel links, and reading it is what
    /// makes a mirror usable at all: most mirrors do not serve PyPI's JSON API, so the lookup
    /// itself has to come from the mirror's own index.
    private static Path installUvFromIndex(String index, Path toolchain, Path binary, Consumer<String> message)
            throws BrandException {
        String arch = switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "x86_64";
            case ARM64 -> "aarch64";
            default -> throw new BrandException("no uv wheel for " + Architecture.CURRENT_ARCH);
        };
        String pageUrl = index.replaceAll("/$", "") + "/uv/";
        message.accept("Looking for uv " + UV_VERSION + " on " + pageUrl);
        String html;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(pageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new BrandException("the index answered " + response.statusCode() + " for " + pageUrl);
            }
            html = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("the uv index lookup was interrupted", e);
        } catch (IOException e) {
            throw new BrandException("could not read " + pageUrl + ": " + e.getMessage(), e);
        }

        String prefix = "uv-" + UV_VERSION + "-";
        java.util.regex.Matcher link = java.util.regex.Pattern.compile("href=\"([^\"]+)\"").matcher(html);
        while (link.find()) {
            String href = link.group(1);
            // Simple pages may carry the hash in a fragment; mirrors may hand out relative links
            // (`../../packages/…`), which have to be resolved against the page they came from.
            String file = href.contains("#") ? href.substring(0, href.indexOf('#')) : href;
            String name = file.substring(file.lastIndexOf('/') + 1);
            if (!name.startsWith(prefix) || !name.endsWith(".whl")
                    || !name.contains("manylinux") || !name.contains(arch)) {
                continue;
            }
            String url = URI.create(pageUrl).resolve(file).toString();
            message.accept("Downloading uv from " + url);
            return extractUvWheel(url, toolchain, binary, message);
        }
        throw new BrandException("no manylinux " + arch + " uv " + UV_VERSION + " wheel on " + index);
    }

    /// Downloads one uv wheel and extracts `uv` from `uv-<version>.data/scripts/uv`.
    ///
    /// The wheel is the same static binary as GitHub's tarball, but it comes from whichever index
    /// answered — which is the whole point of pointing this at a mirror.
    private static Path extractUvWheel(String url, Path toolchain, Path binary, Consumer<String> message)
            throws BrandException {
        try {
            Files.createDirectories(toolchain);
            Path wheel = toolchain.resolve("uv.whl");
            try (InputStream in = download(url)) {
                Files.copy(in, wheel, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(wheel))) {
                ZipEntry entry;
                boolean found = false;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith("/uv")) {
                        Files.copy(zip, binary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new BrandException("uv wheel did not contain scripts/uv");
                }
            }
            Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.deleteIfExists(wheel);
            return binary;
        } catch (IOException e) {
            throw new BrandException("Could not unpack uv from " + url + ": " + e.getMessage(), e);
        }
    }

    /// The marker both Octop installers write: one existence check proves the
    /// release is complete, regardless of package-vs-source layout.
    /// Whether a release directory carries any brand's install marker.
    ///
    /// [#releases] is brand-agnostic (it only sees a brand root), so it cannot
    /// ask one brand for its marker. Octop's `octop-install.json` is checked
    /// first because that is the one this class writes; the per-brand markers of
    /// the other categories are the fallback.
    private static boolean hasInstallMarker(Brand brand, Path directory) {
        if (releaseUsable(brand, directory)) {
            return true;
        }
        // A directory shared with another brand (the Octop source build's staging
        // area) can still carry that brand's marker; the caller only asks whether
        // this directory is a complete install of *something* it can name.
        if (!markerMode(directory).isEmpty()) {
            return true;
        }
        for (Brand other : Brand.values()) {
            if (Files.exists(directory.resolve(other.installMarker()))) {
                return true;
            }
        }
        return false;
    }

    private static String markerMode(Path target) {
        try {
            String json = Files.readString(target.resolve("octop-install.json"), StandardCharsets.UTF_8);
            int index = json.indexOf("\"mode\"");
            if (index < 0) {
                return "";
            }
            int colon = json.indexOf(':', index);
            int first = json.indexOf('"', colon + 1);
            int last = json.indexOf('"', first + 1);
            return first >= 0 && last > first ? json.substring(first + 1, last) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static void writeMarker(Path target, String mode, String version) throws BrandException {
        String json = "{\"mode\":\"" + mode + "\",\"version\":\"" + version + "\"}\n";
        try {
            Files.writeString(target.resolve("octop-install.json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BrandException("Could not write the Octop install marker: " + e.getMessage(), e);
        }
    }

    /// Portable archives are unpacked with `Files.copy`, which does not carry
    /// Unix mode bits. The bundled interpreter must be executable; mark every
    /// file in `runtime/bin` (python3.12, python3, helper shims).
    private static void markExecutable(Path directory) throws BrandException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : entries.toList()) {
                if (!Files.isRegularFile(entry)) {
                    continue;
                }
                try {
                    Files.setPosixFilePermissions(entry, PosixFilePermissions.fromString("rwxr-xr-x"));
                } catch (UnsupportedOperationException ignored) {
                    // Windows: no POSIX mode; the executable bit is implicit.
                    return;
                }
            }
        } catch (IOException e) {
            throw new BrandException("Could not make Octop's bundled Python executable: " + e.getMessage(), e);
        }
    }

    /// The platform suffix in Octop's portable asset names
    /// (`linux-amd64`, `windows-arm64`, `darwin-amd64`).
    static String octopPlatform() throws BrandException {
        String os = switch (OperatingSystem.CURRENT_OS) {
            case LINUX -> "linux";
            case WINDOWS -> "windows";
            case MACOS -> "darwin";
            default -> throw new BrandException("Octop has no portable build for "
                    + OperatingSystem.CURRENT_OS);
        };
        String arch = switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "amd64";
            case ARM64 -> "arm64";
            default -> throw new BrandException("Octop has no portable build for "
                    + Architecture.CURRENT_ARCH);
        };
        return os + "-" + arch;
    }

    /// Fetches the source zip over HTTPS. Kept separate from the unpacking so
    /// the retry loop can wrap the download alone.
    private static java.io.InputStream download(String url) throws BrandException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Accept", "*/*")
                .GET()
                .build();
        try {
            HttpResponse<java.io.InputStream> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new BrandException("Download answered " + response.statusCode() + ": " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("Download was interrupted: " + url, e);
        } catch (java.io.IOException e) {
            throw new BrandException("Download failed: " + url + " — " + e.getMessage(), e);
        }
    }

    /// Unpacks a GitHub source zip into `target`, dropping the archive's single
    /// root folder (`<repo>-<tag>/…`). Entry paths are resolved against the
    /// target and rejected when they escape it (zip-slip).
    ///
    /// Package-private so the unpacking is testable with an in-memory zip.
    static void extractZip(java.io.InputStream zip, Path target) throws BrandException {
        extractZip(zip, target, () -> false);
    }

    /// Unpacks an archive, asking `cancelled` between entries: an unzip loop is not a child process,
    /// so the task service's process kill does not reach it.
    ///
    /// @param zip       the archive
    /// @param target    where to unpack
    /// @param cancelled whether the user asked to stop
    static void extractZip(java.io.InputStream zip, Path target, java.util.function.BooleanSupplier cancelled)
            throws BrandException {
        try {
            Files.createDirectories(target);
            Path root = target.toAbsolutePath().normalize();
            try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(
                    new java.io.BufferedInputStream(zip))) {
                java.util.zip.ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if (cancelled.getAsBoolean()) {
                        throw new BrandException("unpacking was cancelled");
                    }
                    String name = entry.getName();
                    int slash = name.indexOf('/');
                    if (slash < 0) {
                        // The archive's single root directory entry itself.
                        continue;
                    }
                    String relative = name.substring(slash + 1);
                    if (relative.isEmpty()) {
                        continue;
                    }
                    Path destination = root.resolve(relative).normalize();
                    if (!destination.startsWith(root)) {
                        throw new BrandException("Archive entry escapes the target: " + name);
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(destination);
                        continue;
                    }
                    Files.createDirectories(destination.getParent());
                    Files.copy(in, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (java.io.IOException e) {
            throw new BrandException("Could not unpack the source archive: " + e.getMessage(), e);
        }
    }

    /// Runs one install command, streaming its merged output to the log and
    /// the progress sink, and fails the install on a nonzero exit.
    private static void runChecked(List<String> command, Path directory, java.util.Map<String, String> environment,
                                   Path log, Consumer<String> message, String what) throws BrandException {
        message.accept("Running: " + String.join(" ", command));
        LOG.info("Brand install step (" + what + "): " + String.join(" ", command));
        try (OutputStreamAppend output = new OutputStreamAppend(log);
                BufferedReader reader = run(command, directory, environment, output)) {
            StringBuilder tail = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.writeLine(line);
                if (tail.length() > 0) {
                    tail.append(" | ");
                }
                tail.append(line);
                if (tail.length() > 2000) {
                    tail.delete(0, tail.length() - 1000);
                }
                message.accept(line);
            }
            int code = output.exitCode();
            if (code != 0) {
                // A cancel kills the tree, which the child reports as SIGKILL/SIGTERM; saying so
                // beats showing the panel "exit 137" for something the user asked for.
                if (code == 137 || code == 143) {
                    throw new BrandException("Install step was cancelled (" + what + ")");
                }
                throw new BrandException("Install step failed (" + what + ", exit " + code + "): " + tail);
            }
        } catch (IOException e) {
            throw new BrandException("Install step failed (" + what + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrandException("Install step was interrupted (" + what + ")", e);
        }
    }

    /// Whether this panel build ships a patched Octop dashboard for a version.
    /// The version picker filters on it, so a user cannot select an upstream
    /// tag whose SPA would render at the wrong base (and fail under the mount).
    public static boolean hasBundledOctopDashboard(String version) {
        return BrandInstaller.class.getResource("/octop/octop-dashboard-" + version + ".zip") != null;
    }

    /// The installed releases, newest version first.
    ///
    /// @param brandRoot the brand's directory
    /// @return the releases
    public static List<Release> releases(Brand brand, Path brandRoot) {
        Path releases = brandRoot.resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases)) {
            return List.of();
        }
        Path current = currentLink(brandRoot);
        List<Release> found = new ArrayList<>();
        try (Stream<Path> entries = Files.list(releases)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                String version = directory.getFileName().toString();
                // A `.previous` directory is the source build's staging area,
                // not a release: listing it made `defaultVersion()` point new
                // instances at an uninstallable name.
                if (version.endsWith(PREVIOUS_SUFFIX)) {
                    continue;
                }
                // A directory without an install marker is a half-finished
                // install (the JVM was killed, or the download failed). It is
                // not usable, so it must not be offered.
                if (!hasInstallMarker(brand, directory)) {
                    LOG.warning("Ignoring incomplete brand release at " + directory
                            + " (no install marker); run an install of that version to repair it");
                    continue;
                }
                found.add(new Release(version, directory.toString(),
                        current != null && current.equals(directory)));
            }
        } catch (IOException e) {
            LOG.warning("Failed to enumerate releases in " + releases, e);
        }
        found.sort(Comparator.comparing(Release::version, newestFirst()));
        return found;
    }

    /// Puts a killed source build's leftovers back in order.
    ///
    /// The source installer moves the working release aside to
    /// `<version>.previous` while it builds; the restore lives in a `finally`,
    /// so a panel that is killed mid-build leaves **both** a half-built
    /// `<version>` and a complete `<version>.previous`. Without this pass the
    /// usable release stays hidden under the staging name for ever, and new
    /// instances get told that version is installed when it is not.
    ///
    /// Rules, in order:
    /// - a `<version>` that carries the install marker wins; its `.previous`
    ///   sibling is stale and left alone (deleting a user's only other copy is
    ///   not this method's call);
    /// - otherwise the half-built `<version>` is renamed to
    ///   `<version>.broken-<millis>` (kept for diagnosis) and `.previous` is
    ///   moved back.
    ///
    /// Safe to call on every boot; it only touches directories this installer
    /// created.
    ///
    /// @param brandRoot the brand's directory
    public static void reconcileOctopReleases(Path brandRoot) {
        Path releases = brandRoot.resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases)) {
            return;
        }
        List<Path> entries;
        try (Stream<Path> list = Files.list(releases)) {
            entries = list.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            LOG.warning("Could not scan " + releases + " for interrupted installs", e);
            return;
        }
        for (Path previous : entries) {
            String name = previous.getFileName().toString();
            if (!name.endsWith(PREVIOUS_SUFFIX)) {
                continue;
            }
            String version = name.substring(0, name.length() - PREVIOUS_SUFFIX.length());
            Path target = releases.resolve(version);
            if (Files.isDirectory(target) && hasInstallMarker(Brand.OCTOP, target)) {
                // The build finished (or was never interrupted): nothing to do.
                continue;
            }
            // Only a *complete* staging copy is worth restoring: if the previous
            // build died too, `.previous` has no marker either, and moving it
            // back would just rename one broken tree.
            if (!hasInstallMarker(Brand.OCTOP, previous)) {
                LOG.warning("Ignoring an incomplete staged Octop release at " + previous
                        + "; reinstall that version to repair it");
                continue;
            }
            if (Files.exists(target)) {
                // Keep one broken tree for diagnosis, not a pile of them: each
                // is hundreds of megabytes and repeated interrupted builds would
                // grow without bound.
                pruneBrokenReleases(releases, version);
                Path broken = releases.resolve(version + ".broken-" + System.currentTimeMillis());
                try {
                    Files.move(target, broken);
                    LOG.warning("Moved an incomplete Octop release aside: " + target + " -> " + broken);
                } catch (IOException e) {
                    LOG.warning("Could not move the incomplete Octop release aside: " + target, e);
                    continue;
                }
            }
            try {
                Files.move(previous, target);
                LOG.info("Recovered the Octop release " + version + " from " + previous);
            } catch (IOException e) {
                LOG.warning("Could not restore the staged Octop release " + previous, e);
            }
        }
    }

    /// Deletes every `<version>.broken-*` directory, so the diagnostic copy we
    /// are about to keep is the only one.
    private static void pruneBrokenReleases(Path releases, String version) {
        String prefix = version + ".broken-";
        try (Stream<Path> list = Files.list(releases)) {
            for (Path candidate : list.filter(Files::isDirectory).toList()) {
                if (candidate.getFileName().toString().startsWith(prefix)) {
                    FileUtils.deleteDirectoryQuietly(candidate);
                }
            }
        } catch (IOException e) {
            LOG.warning("Could not prune old broken Octop releases in " + releases, e);
        }
    }

    /// The directory holding the given installed version, or `null` when that
    /// version is not installed.
    ///
    /// @param brand the brand
    /// @param brandRoot the brand's directory
    /// @param version the version
    /// @return the prefix, or `null`
    public static @Nullable Path packageDir(Brand brand, Path brandRoot, String version) {
        Path candidate = brandRoot.resolve(RELEASES_DIR).resolve(version);
        return releaseUsable(brand, candidate)
                ? candidate
                : null;
    }

    /// The release a new instance should default to: the `current` link's
    /// target, else the newest installed release, else `null`.
    ///
    /// @param brand the brand
    /// @param brandRoot the brand's directory
    /// @return the default package directory, or `null` when nothing is installed
    public static @Nullable Path defaultPackage(Brand brand, Path brandRoot) {
        Path current = currentLink(brandRoot);
        if (current != null && releaseUsable(brand, current)) {
            return current;
        }
        List<Release> releases = releases(brand, brandRoot);
        return releases.isEmpty() ? null : Path.of(releases.get(0).path());
    }

    /// Appends one line to the brand's install log.
    ///
    /// Used by the uv wrapper: the portage-style `>>> 已安装 (199 of 345) cryptography-46.0` lines
    /// belong in the log the page tails, not only in the task's single status line.
    ///
    /// @param brandRoot the brand's directory
    /// @param line      the line, without its terminator
    static void appendLogLine(Path brandRoot, String line) {
        Path log = brandRoot.resolve(INSTALL_LOG);
        try {
            Files.createDirectories(log.getParent());
            Files.writeString(log, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            LOG.info("Could not append to the install log " + log + ": " + e.getMessage());
        }
    }

    /// The tail of the install log, for the panel's log view.
    ///
    /// @param brandRoot the brand's directory
    /// @param maxLines the most lines to return
    /// @return the trailing lines, oldest first
    public static List<String> tailLog(Path brandRoot, int maxLines) {
        Path log = brandRoot.resolve(INSTALL_LOG);
        if (!Files.isRegularFile(log)) {
            return List.of();
        }
        try (var lines = Files.lines(log, StandardCharsets.UTF_8)) {
            List<String> all = lines.toList();
            return List.copyOf(all.subList(Math.max(0, all.size() - maxLines), all.size()));
        } catch (IOException e) {
            LOG.warning("Failed to read " + log, e);
            return List.of();
        }
    }

    /// Hands a freshly installed release to the agent.
    ///
    /// The installer runs as the panel — root inside the container — so everything it unpacks is
    /// root-owned. A brand that writes inside its own release directory (a build cache, a
    /// self-updating shim, `__pycache__` under a venv) then fails with the same `EACCES` that made
    /// the harness's composed profile unreadable. Ownership only: modes stay what the installer
    /// made them, because a release holds executables.
    ///
    /// A no-op outside a container, where the panel and the agent are the same user.
    ///
    /// @param target the release directory
    private static void handReleaseToAgent(Path target) {
        int handed = Privileges.handTreeToAgent(target);
        if (handed > 0) {
            LOG.info("Handed " + handed + " entries of " + target + " to uid " + Privileges.AGENT_UID);
        }
    }

    /// Points the `current` pointer at the given release, replacing whatever was there.
    /// Best effort: a broken pointer only affects the default for new instances, never an
    /// instance's pinned version.
    ///
    /// A link where the system permits one, a pointer file where it does not — which is
    /// the ordinary Windows case, and the reason this used to log a warning on a machine
    /// that had done nothing wrong.
    private static void pointCurrentAt(Path brandRoot, Path target) {
        Path link = brandRoot.resolve(CURRENT_LINK);
        try {
            Links.pointAt(link, target);
        } catch (IOException e) {
            LOG.warning("Could not point " + link + " at " + target, e);
        }
    }

    /// The release the `current` pointer resolves to, or `null`.
    private static @Nullable Path currentLink(Path brandRoot) {
        return Links.pointedAt(brandRoot.resolve(CURRENT_LINK)).orElse(null);
    }

    /// Runs one command in a directory, teeing its merged output to the log.
    private static BufferedReader run(List<String> command, Path directory,
                                      java.util.Map<String, String> environment, OutputStreamAppend output)
            throws IOException, InterruptedException {
        // `npm` and `pnpm` are `npm.cmd` / `pnpm.cmd` on Windows, and the bare names do
        // not name a file there — the clone's dependency step and build step are both
        // program names that have to be completed before they can be started.
        ProcessBuilder builder = new ProcessBuilder(
                org.jackhuang.hmcl.util.platform.Executables.startable(command, environment.get("PATH")));
        builder.directory(directory.toFile());
        builder.environment().putAll(environment);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        output.attach(process);
        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    /// Numeric component-wise ordering, newest first.
    private static Comparator<String> newestFirst() {
        return (left, right) -> {
            String[] a = left.split("\\.");
            String[] b = right.split("\\.");
            for (int i = 0; i < Math.max(a.length, b.length); i++) {
                int x = i < a.length ? parsePart(a[i]) : 0;
                int y = i < b.length ? parsePart(b[i]) : 0;
                if (x != y) {
                    return Integer.compare(y, x);
                }
            }
            return right.compareTo(left);
        };
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// An output stream appended to a log file, wired to a process's merged
    /// output so every install line lands in the panel-visible log.
    private static final class OutputStreamAppend implements AutoCloseable {
        private final java.io.OutputStream stream;
        private Process process;

        OutputStreamAppend(Path log) throws IOException {
            Files.createDirectories(log.getParent());
            this.stream = Files.newOutputStream(log,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        void attach(Process process) {
            this.process = process;
        }

        /// Appends one line to the install log, flushed so the log view follows the
        /// install as it happens rather than after the process exits.
        ///
        /// @param line the line, without its terminator
        /// @throws IOException when the log cannot be written
        void writeLine(String line) throws IOException {
            stream.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            stream.flush();
        }

        int exitCode() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}
