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
package org.jackhuang.hmcl.web.transcode.runtime;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Installs ffmpeg at runtime for deployments that do not ship it.
///
/// Three paths, in the order the settings page offers them: a static build downloaded
/// to the panel's data volume (Windows/macOS and Linux), the distribution package
/// manager (container root), and a source build (last resort). Every path writes into
/// `<data>/transcode/tools/ffmpeg`, so the result survives a container rebuild and the
/// panel never depends on a system path it cannot see.
@NotNullByDefault
public final class FfmpegInstaller {

    public enum State {
        @SerializedName("running") RUNNING,
        @SerializedName("done") DONE,
        @SerializedName("failed") FAILED,
        @SerializedName("cancelled") CANCELLED
    }

    public static final class InstallJob {
        public String id = "";
        public String kind = "";
        public State state = State.RUNNING;
        public double progress = 0;
        public String message = "";
        public String log = "";
        public @Nullable String error;
        public long createdAt = System.currentTimeMillis();
        public long endedAt = -1;
        public transient @Nullable Process process;
    }

    private final TranscodeModule module;
    private final Path toolsDir;
    private final Map<String, InstallJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "transcode-installer");
        thread.setDaemon(true);
        return thread;
    });

    public FfmpegInstaller(TranscodeModule module) {
        this.module = module;
        this.toolsDir = module.dataDir().resolve("tools").resolve("ffmpeg");
    }

    public InstallJob start(String kind, @Nullable String url, @Nullable String sha256,
                            @Nullable String version) throws IOException {
        if (!List.of("static", "package", "compile").contains(kind)) {
            throw new IOException("kind must be static, package or compile");
        }
        if ("static".equals(kind) || "compile".equals(kind)) {
            if (url == null || url.isBlank()) {
                throw new IOException("url is required for " + kind);
            }
            validateUrl(url);
        }
        Files.createDirectories(toolsDir);
        InstallJob job = new InstallJob();
        job.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        job.kind = kind;
        jobs.put(job.id, job);
        executor.execute(() -> run(job, url, sha256, version));
        return job;
    }

    public Optional<InstallJob> job(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public boolean cancel(String id) {
        InstallJob job = jobs.get(id);
        if (job == null || job.state != State.RUNNING) {
            return false;
        }
        job.state = State.CANCELLED;
        job.message = "cancelled";
        job.endedAt = System.currentTimeMillis();
        Process process = job.process;
        if (process != null) {
            ProcessTree.terminate(process, 2);
        }
        return true;
    }

    private void run(InstallJob job, @Nullable String url, @Nullable String sha256, @Nullable String version) {
        try {
            switch (job.kind) {
                case "static" -> installStatic(job, url, sha256, version);
                case "package" -> installPackage(job);
                case "compile" -> compile(job, url, sha256, version);
                default -> throw new IOException("unknown kind " + job.kind);
            }
            if (job.state == State.RUNNING) {
                job.state = State.DONE;
                job.progress = 1;
                job.message = "done";
                job.endedAt = System.currentTimeMillis();
                module.runtime().refresh();
            }
        } catch (Exception e) {
            if (job.state != State.CANCELLED) {
                job.state = State.FAILED;
                job.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                job.message = "failed";
                job.endedAt = System.currentTimeMillis();
            }
        }
    }

    private void installStatic(InstallJob job, String url, String sha256, @Nullable String version)
            throws Exception {
        String selectedVersion = version == null || version.isBlank()
                ? "static-" + System.currentTimeMillis() : version;
        Path archive = download(job, url, sha256);
        Path extract = toolsDir.resolve(".extract-" + job.id);
        deleteTree(extract);
        Files.createDirectories(extract);
        log(job, "extracting " + archive.getFileName());
        extract(archive, extract);
        Path ffmpeg = find(extract, "ffmpeg");
        Path ffprobe = find(extract, "ffprobe");
        if (ffmpeg == null || ffprobe == null) {
            throw new IOException("the archive does not contain ffmpeg and ffprobe");
        }
        Path target = installTo(job, selectedVersion, ffmpeg, ffprobe);
        log(job, "installed " + target);
        deleteTree(extract);
        Files.deleteIfExists(archive);
    }

    private void installPackage(InstallJob job) throws Exception {
        String command;
        List<String> args;
        if (commandExists("apt-get")) {
            command = "apt-get";
            args = List.of("install", "-y", "--no-install-recommends", "ffmpeg");
        } else if (commandExists("dnf")) {
            command = "dnf";
            args = List.of("install", "-y", "ffmpeg");
        } else if (commandExists("zypper")) {
            command = "zypper";
            args = List.of("--non-interactive", "install", "ffmpeg-7");
        } else {
            throw new IOException("no supported package manager (apt-get/dnf/zypper)");
        }
        List<String> full = new ArrayList<>();
        full.add(command);
        full.addAll(args);
        log(job, "running " + String.join(" ", full));
        runProcess(job, full, null);
        log(job, "package manager finished; run 重新探测");
    }

    private void compile(InstallJob job, String url, String sha256, @Nullable String version)
            throws Exception {
        String selectedVersion = version == null || version.isBlank()
                ? "source-" + System.currentTimeMillis() : version;
        Path archive = download(job, url, sha256);
        Path source = toolsDir.resolve(".src-" + job.id);
        deleteTree(source);
        Files.createDirectories(source);
        extract(archive, source);
        Path configure = findConfigure(source);
        if (configure == null) {
            throw new IOException("no ./configure found in the source archive");
        }
        Path prefix = toolsDir.resolve(selectedVersion);
        Files.createDirectories(prefix);
        log(job, "configuring into " + prefix);
        runProcess(job, List.of("./configure",
                "--prefix=" + prefix,
                "--disable-doc", "--disable-debug",
                "--enable-gpl", "--enable-libx264", "--enable-libx265",
                "--enable-vaapi", "--enable-libvpl"), configure.getParent());
        int cpus = Math.max(1, Runtime.getRuntime().availableProcessors());
        log(job, "building with -j" + cpus);
        runProcess(job, List.of("make", "-j" + cpus), configure.getParent());
        log(job, "installing");
        runProcess(job, List.of("make", "install"), configure.getParent());
        Path ffmpeg = prefix.resolve("bin").resolve("ffmpeg");
        Path ffprobe = prefix.resolve("bin").resolve("ffprobe");
        if (!Files.isRegularFile(ffmpeg) || !Files.isRegularFile(ffprobe)) {
            throw new IOException("source build finished without ffmpeg/ffprobe");
        }
        copyToCurrent(job, ffmpeg, ffprobe);
        deleteTree(source);
        Files.deleteIfExists(archive);
    }

    private Path download(InstallJob job, String url, String sha256) throws Exception {
        URI uri = URI.create(url);
        Path archive = toolsDir.resolve(".download-" + job.id + suffix(uri.getPath()));
        log(job, "downloading " + url);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        validateUrl(response.uri().toString());
        if (response.statusCode() != 200) {
            throw new IOException("download returned HTTP " + response.statusCode());
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(response.body());
             var out = Files.newOutputStream(archive)) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                total += read;
                if (total % (1024 * 1024) < 8192) {
                    job.progress = Math.min(0.7, total / (200.0 * 1024 * 1024));
                    job.message = "downloading " + (total / (1024 * 1024)) + " MiB";
                }
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        String expected = expectedSha(sha256, uri, archive.getFileName().toString());
        if (!actual.equalsIgnoreCase(expected)) {
            Files.deleteIfExists(archive);
            throw new IOException("SHA-256 mismatch: expected " + expected + ", got " + actual);
        }
        job.progress = 0.7;
        return archive;
    }

    private Path installTo(InstallJob job, String version, Path ffmpeg, Path ffprobe) throws IOException {
        Path target = toolsDir.resolve(version);
        Path bin = target.resolve("bin");
        Files.createDirectories(bin);
        Files.copy(ffmpeg, bin.resolve("ffmpeg"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(ffprobe, bin.resolve("ffprobe"), StandardCopyOption.REPLACE_EXISTING);
        makeExecutable(bin.resolve("ffmpeg"));
        makeExecutable(bin.resolve("ffprobe"));
        copyToCurrent(job, bin.resolve("ffmpeg"), bin.resolve("ffprobe"));
        return target;
    }

    private void copyToCurrent(InstallJob job, Path ffmpeg, Path ffprobe) throws IOException {
        Path current = toolsDir.resolve("current").resolve("bin");
        Files.createDirectories(current);
        Files.copy(ffmpeg, current.resolve("ffmpeg"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(ffprobe, current.resolve("ffprobe"), StandardCopyOption.REPLACE_EXISTING);
        makeExecutable(current.resolve("ffmpeg"));
        makeExecutable(current.resolve("ffprobe"));
        job.progress = 0.95;
    }

    /// `auto` (or an empty field) means "read the publisher's sidecar": `<url>.sha256`,
    /// then `checksums.sha256` / `SHA256SUMS` in the same directory. If none exists the
    /// job fails with the exact URL it tried; a custom URL must still supply a hash.
    private static String expectedSha(@Nullable String sha256, URI uri, String fileName)
            throws IOException, InterruptedException {
        if (sha256 != null && !sha256.isBlank() && !"auto".equalsIgnoreCase(sha256)) {
            return sha256.trim();
        }
        for (String candidate : List.of(uri.toString() + ".sha256",
                uri.resolve("checksums.sha256").toString(),
                uri.resolve("SHA256SUMS").toString())) {
            String body = fetchSmall(candidate);
            if (body == null) {
                continue;
            }
            for (String line : body.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split("\s+");
                if (parts.length >= 2 && parts[1].contains(fileName)) {
                    String hash = parts[0].replace("*", "");
                    if (hash.matches("[0-9a-fA-F]{64}")) {
                        return hash;
                    }
                }
                if (parts.length == 1 && parts[0].matches("[0-9a-fA-F]{64}")) {
                    return parts[0];
                }
            }
        }
        throw new IOException("no SHA-256 sidecar at " + uri + ".sha256; fill the SHA-256 field");
    }

    private static @Nullable String fetchSmall(String url) throws IOException, InterruptedException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().length() < 2_000_000) {
                return response.body();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static void extract(Path archive, Path destination) throws Exception {
        String name = archive.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".zip")) {
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path target = destination.resolve(entry.getName()).normalize();
                    if (!target.startsWith(destination)) {
                        throw new IOException("zip entry escapes the destination: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return;
        }
        List<String> command = name.endsWith(".tar.xz") || name.endsWith(".txz")
                ? List.of("tar", "-xJf", archive.toString(), "-C", destination.toString())
                : List.of("tar", "-xf", archive.toString(), "-C", destination.toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IOException("archive extraction timed out");
        }
        if (process.exitValue() != 0) {
            throw new IOException("archive extraction failed with code " + process.exitValue());
        }
    }

    private void runProcess(InstallJob job, List<String> command, @Nullable Path cwd) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (cwd != null) {
            builder.directory(cwd.toFile());
        }
        Process process = builder.start();
        job.process = process;
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                log(job, new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
        }
        if (!process.waitFor(30, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IOException("command timed out: " + String.join(" ", command));
        }
        job.process = null;
        if (job.state == State.CANCELLED) {
            throw new IOException("cancelled");
        }
        if (process.exitValue() != 0) {
            throw new IOException("command failed with code " + process.exitValue()
                    + ": " + String.join(" ", command));
        }
    }

    private static @Nullable Path find(Path root, String name) throws IOException {
        try (var stream = Files.walk(root, 8)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(name)
                            || path.getFileName().toString().equals(name + ".exe"))
                    .findFirst().orElse(null);
        }
    }

    private static @Nullable Path findConfigure(Path root) throws IOException {
        try (var stream = Files.walk(root, 6)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("configure"))
                    .findFirst().orElse(null);
        }
    }

    private static String suffix(@Nullable String path) {
        if (path == null) {
            return ".bin";
        }
        if (path.endsWith(".tar.xz")) return ".tar.xz";
        if (path.endsWith(".tar.gz")) return ".tar.gz";
        if (path.endsWith(".zip")) return ".zip";
        return ".bin";
    }

    private static void makeExecutable(Path file) {
        try {
            var permissions = Files.getPosixFilePermissions(file);
            permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
            permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE);
            permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(file, permissions);
        } catch (Exception ignored) {
            // Windows: the extension decides, not the mode bits.
        }
    }

    private static boolean commandExists(String name) {
        return org.jackhuang.hmcl.util.platform.Executables.resolve(name).isPresent();
    }

    static void validateUrl(String url) throws IOException {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (RuntimeException e) {
            throw new IOException("invalid URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("only https URLs are accepted");
        }
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        if ("localhost".equals(host) || host.startsWith("127.") || host.startsWith("10.")
                || host.startsWith("192.168.") || host.startsWith("172.16.")
                || host.startsWith("172.17.") || host.startsWith("172.18.")
                || host.startsWith("172.19.") || host.startsWith("172.2")
                || host.startsWith("172.30.") || host.startsWith("172.31.")) {
            throw new IOException("private/loopback URLs are refused");
        }
    }

    private static void log(InstallJob job, String text) {
        job.log = job.log + text;
        if (job.log.length() > 200_000) {
            job.log = job.log.substring(job.log.length() - 200_000);
        }
    }

    private static void deleteTree(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}