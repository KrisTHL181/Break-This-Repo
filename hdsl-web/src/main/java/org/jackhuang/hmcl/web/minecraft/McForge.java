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
package org.jackhuang.hmcl.web.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Forge and NeoForge: the two cores that are installed rather than downloaded
/// as a ready jar.
///
/// **Why an installer.** Forge and NeoForge do not publish a runnable server
/// jar. Their `installer.jar` downloads the Minecraft server, patches it, and
/// writes a launch profile (`libraries/…/unix_args.txt`, a Java argument file
/// with the classpath and main class). The panel runs the installer once per
/// instance, then launches with `java @unix_args.txt nogui` — the same shape
/// `run.sh` uses, minus the shell.
///
/// **Per instance, not shared.** A Forge install is a tree of libraries plus a
/// patched server jar that the installer writes into the working directory.
/// Sharing it between instances would mean every instance sharing one world
/// directory, so the panel installs into the instance. The installer itself is
/// cached in the instance's `.installer/` directory and deleted afterwards.
///
/// **Versions come from upstream metadata.** Forge's `promotions_slim.json`
/// says which loader build each game version gets (recommended, else latest);
/// NeoForge's Maven metadata lists builds whose first two numbers encode the
/// game version (`21.4.123` → 1.21.4). No aggregator in between.
@NotNullByDefault
public final class McForge {

    /// Forge's Maven; `HDSL_FORGE_MAVEN` overrides it for a mirror.
    public static final URI DEFAULT_FORGE_MAVEN = URI.create("https://maven.minecraftforge.net");
    /// Forge's version promotions; `HDSL_FORGE_PROMOTIONS` overrides it.
    public static final URI DEFAULT_FORGE_PROMOTIONS =
            URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json");
    /// NeoForge's Maven; `HDSL_NEOFORGE_MAVEN` overrides it.
    public static final URI DEFAULT_NEOFORGE_MAVEN = URI.create("https://maven.neoforged.net");

    /// The file the installer leaves in the instance directory.
    public static final String LAUNCH_FILE = "launch.json";
    private static final String INSTALLER_DIR = ".installer";
    private static final long MAX_INSTALLER_BYTES = 64L * 1024 * 1024;
    private static final Duration METADATA_TTL = Duration.ofMinutes(10);

    private static final Logger LOG = Logger.LOG;

    /// One installable combination.
    ///
    /// @param core          `forge` or `neoforge`
    /// @param mcVersion     the game version
    /// @param loaderVersion the loader build (`54.0.16`, `21.4.123`)
    /// @param installerUrl  the installer jar
    public record Build(String core, String mcVersion, String loaderVersion, String installerUrl) {
    }

    /// Runs the installer; injectable so a test does not need a JVM download.
    @FunctionalInterface
    public interface InstallerRunner {
        /// @param javaBin   the java executable the panel selected
        /// @param installer the installer jar
        /// @param directory the instance directory (the installer's cwd)
        /// @param mirror    a Maven mirror for the installer's libraries, or `null`
        /// @param log       receives the installer's output lines
        /// @return the exit code
        /// @throws IOException          when the process cannot start
        /// @throws InterruptedException when the wait is interrupted
        int run(String javaBin, Path installer, Path directory, @Nullable String mirror,
                java.util.function.Consumer<String> log) throws IOException, InterruptedException;
    }

    private record Cached(List<Build> builds, java.time.Instant at) {
    }

    private final URI forgeMaven;
    private final URI forgePromotions;
    private final URI neoMaven;
    private final @Nullable McSources sources;
    private final HttpClient client;
    private final InstallerRunner runner;
    private volatile @Nullable Cached cached;

    public McForge() {
        this(mavenFromEnv("HDSL_FORGE_MAVEN", DEFAULT_FORGE_MAVEN),
                mavenFromEnv("HDSL_FORGE_PROMOTIONS", DEFAULT_FORGE_PROMOTIONS),
                mavenFromEnv("HDSL_NEOFORGE_MAVEN", DEFAULT_NEOFORGE_MAVEN),
                McForge::runInstaller, null);
    }

    /// The production constructor: bases come from the operator's source order,
    /// and the fallback to MSL is allowed when that source is enabled.
    ///
    /// @param sources the source settings
    public McForge(McSources sources) {
        this(mavenFromEnv("HDSL_FORGE_MAVEN", DEFAULT_FORGE_MAVEN),
                mavenFromEnv("HDSL_FORGE_PROMOTIONS", DEFAULT_FORGE_PROMOTIONS),
                mavenFromEnv("HDSL_NEOFORGE_MAVEN", DEFAULT_NEOFORGE_MAVEN),
                McForge::runInstaller, sources);
    }

    /// The same, with overridable endpoints and an injectable runner (tests).
    ///
    /// @param forgeMaven      Forge's Maven base
    /// @param forgePromotions the promotions JSON
    /// @param neoMaven        NeoForge's Maven base
    /// @param runner          how the installer is executed
    public McForge(URI forgeMaven, URI forgePromotions, URI neoMaven, InstallerRunner runner) {
        this(forgeMaven, forgePromotions, neoMaven, runner, null);
    }

    private McForge(URI forgeMaven, URI forgePromotions, URI neoMaven, InstallerRunner runner,
                    @Nullable McSources sources) {
        this.forgeMaven = forgeMaven;
        this.forgePromotions = forgePromotions;
        this.neoMaven = neoMaven;
        this.sources = sources;
        this.runner = runner;
        // BMCLAPI answers its Maven tree with a 302 (to a CERNET/USTC mirror) and
        // JDK's default is NEVER: without this, a domestic source silently fails
        // its metadata read and the installer falls back to the slow official host.
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static URI mavenFromEnv(String variable, URI fallback) {
        String value = System.getenv(variable);
        if (value != null && !value.isBlank()) {
            try {
                return URI.create(value.strip());
            } catch (IllegalArgumentException e) {
                LOG.warning("Ignoring " + variable + "=`" + value + "`: " + e.getMessage());
            }
        }
        return fallback;
    }

    /// Whether an id is one of the two installed cores.
    ///
    /// @param core the core id
    /// @return whether it is `forge` or `neoforge`
    public static boolean isInstalledCore(@Nullable String core) {
        return "forge".equals(core) || "neoforge".equals(core);
    }

    /// The game versions one core can install, newest first.
    ///
    /// @param core `forge` or `neoforge`
    /// @return the game versions
    /// @throws McException when the metadata cannot be read
    public List<String> gameVersions(String core) throws McException {
        List<Build> builds;
        try {
            builds = cachedBuilds();
        } catch (McException e) {
            // The Maven metadata is the first thing a domestic line loses.
            // MSL publishes the same version list and is not metered for it.
            List<String> mirrored = mslmcVersions(core);
            if (!mirrored.isEmpty()) {
                return mirrored;
            }
            throw e;
        }
        List<String> versions = new ArrayList<>();
        for (Build build : builds) {
            if (build.core().equals(core) && !versions.contains(build.mcVersion())) {
                versions.add(build.mcVersion());
            }
        }
        versions.sort(McForge::compareVersions);
        java.util.Collections.reverse(versions);
        if (versions.isEmpty()) {
            throw new McException("the upstream metadata lists no " + core + " versions");
        }
        return versions;
    }

    /// The build to install for one game version.
    ///
    /// Forge prefers its `recommended` promotion and falls back to `latest`;
    /// NeoForge takes the newest stable build whose first two numbers name the
    /// game version.
    ///
    /// @param core      `forge` or `neoforge`
    /// @param mcVersion the game version
    /// @return the build
    /// @throws McException when upstream publishes nothing for it
    public Build resolve(String core, String mcVersion) throws McException {
        Build best = null;
        try {
            for (Build build : cachedBuilds()) {
                if (!build.core().equals(core) || !build.mcVersion().equals(mcVersion)) {
                    continue;
                }
                if (best == null || compareVersions(build.loaderVersion(), best.loaderVersion()) > 0) {
                    best = build;
                }
            }
        } catch (McException e) {
            best = mslmcBuild(core, mcVersion);
            if (best == null) {
                throw e;
            }
        }
        if (best == null) {
            Build mirrored = mslmcBuild(core, mcVersion);
            if (mirrored != null) {
                return mirrored;
            }
            throw new McException(core + " has no build for " + mcVersion);
        }
        return best;
    }

    /// Installs one core build into an instance directory and records how to
    /// launch it.
    ///
    /// @param instanceDir the instance directory
    /// @param core        `forge` or `neoforge`
    /// @param mcVersion   the game version
    /// @param javaBin     the java executable to run the installer with
    /// @param progress    download progress, or `null`
    /// @return the recorded launch description
    /// @throws McException when the download, the checksum, the installer or the
    ///                      launch-file discovery fails
    public Launch install(Path instanceDir, String core, String mcVersion, String javaBin,
                          @Nullable McVersions.Progress progress) throws McException {
        Build build = resolve(core, mcVersion);
        // The installer runs as the agent (see runInstaller). A Forge install happens before
        // any start, so the start-time hand-over cannot prepare this directory; it has to be
        // the agent's before the first file is written by it.
        Privileges.prepareAgentDirectory(instanceDir, Privileges.AGENT_SHARED_DIRECTORY);
        if (progress != null) {
            progress.stage("Downloading " + core + " " + mcVersion + " installer");
        }
        Path installer = installFile(instanceDir, build, progress);
        String mirror = mirrorFor(core);
        if (progress != null) {
            progress.stage("Installing " + core + " " + mcVersion
                    + (mirror == null ? "" : " (libraries via mirror)"));
        }
        try {
            List<String> tail = new ArrayList<>();
            int exit;
            try {
                exit = runner.run(javaBin, installer, instanceDir, mirror, line -> {
                    synchronized (tail) {
                        tail.add(line);
                        if (tail.size() > 40) {
                            tail.remove(0);
                        }
                    }
                });
            } catch (IOException e) {
                throw new McException("Failed to start the " + core + " installer: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new McException("the " + core + " installer was interrupted", e);
            }
            if (exit != 0) {
                String detail = String.join("\n", tail);
                throw new McException("the " + core + " installer exited with code " + exit
                        + (detail.isBlank() ? "" : ":\n" + detail));
            }
            Launch launch = discover(instanceDir, build);
            JsonObject record = new JsonObject();
            record.addProperty("core", core);
            record.addProperty("mcVersion", build.mcVersion());
            record.addProperty("loaderVersion", build.loaderVersion());
            if (launch.argsFile() != null) {
                record.addProperty("argsFile", launch.argsFile().toString());
            } else if (launch.jar() != null) {
                record.addProperty("jar", launch.jar());
            }
            record.addProperty("installedAt", java.time.Instant.now().toString());
            try {
                JsonUtils.writeToJsonFile(instanceDir.resolve(LAUNCH_FILE), record);
            } catch (IOException e) {
                throw new McException("Failed to record the launch profile: " + e.getMessage(), e);
            }
            // Everything the installer left — `libraries/`, `unix_args.txt`, `launch.json` —
            // is the server's to read and, for some loaders, to extend. Older panels created
            // the tree as root; hand it over so a launch does not trip on its own files.
            Privileges.handTreeToAgent(instanceDir);
            LOG.info("Installed " + core + " " + build.loaderVersion() + " for " + mcVersion
                    + " into " + instanceDir);
            return launch;
        } finally {
            try {
                Files.deleteIfExists(installer);
            } catch (IOException e) {
                LOG.warning("Could not delete " + installer, e);
            }
        }
    }

    /// The extra argv for a Forge/NeoForge instance, or `null` when the
    /// instance has no `launch.json` (it has not been installed yet).
    ///
    /// @param instanceDir the instance directory
    /// @return `@unix_args.txt nogui` or `-jar forge-….jar nogui`, or `null`
    public static @Nullable List<String> launchArguments(Path instanceDir) {
        Path file = instanceDir.resolve(LAUNCH_FILE);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject record = JsonUtils.fromJsonFile(file, JsonObject.class);
            if (record == null) {
                return null;
            }
            if (record.has("argsFile") && record.get("argsFile").isJsonPrimitive()) {
                String relative = record.get("argsFile").getAsString();
                Path argsFile = instanceDir.resolve(relative).normalize();
                if (!argsFile.startsWith(instanceDir.normalize())) {
                    return null;
                }
                return List.of("@" + argsFile, "nogui");
            }
            if (record.has("jar") && record.get("jar").isJsonPrimitive()) {
                String jar = record.get("jar").getAsString();
                Path jarFile = instanceDir.resolve(jar).normalize();
                if (!jarFile.startsWith(instanceDir.normalize()) || !Files.isRegularFile(jarFile)) {
                    return null;
                }
                return List.of("-jar", jarFile.toString(), "nogui");
            }
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + file + ": " + e.getMessage());
        }
        return null;
    }

    private Path installFile(Path instanceDir, Build build, @Nullable McVersions.Progress progress)
            throws McException {
        Path directory = instanceDir.resolve(INSTALLER_DIR);
        try {
            Files.createDirectories(directory);
            // A previous attempt killed mid-download leaves `.part` files that are
            // never resumed (the name is random); clearing them keeps the state
            // honest and the directory from growing across retries.
            try (var stale = Files.list(directory)) {
                for (Path leftover : stale.filter(path -> path.getFileName().toString()
                        .endsWith(".part")).toList()) {
                    Files.deleteIfExists(leftover);
                }
            }
            Path target = directory.resolve(build.core() + "-" + build.loaderVersion() + "-installer.jar");
            Path temp = Files.createTempFile(directory, "installer", ".part");
            try {
                download(build.installerUrl(), temp, progress);
                verifySha1(build.installerUrl(), temp);
                requireZip(temp);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            return target;
        } catch (IOException e) {
            throw new McException("Failed to prepare the " + build.core() + " installer: "
                    + e.getMessage(), e);
        }
    }

    private void download(String url, Path target, @Nullable McVersions.Progress progress)
            throws McException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(15))
                    .header("User-Agent", McSources.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<InputStream> answer = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (answer.statusCode() != 200) {
                throw new McException("the installer download answered HTTP " + answer.statusCode());
            }
            long copied = 0;
            long lastReport = 0;
            try (InputStream in = answer.body(); OutputStream out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        // TaskService#cancel interrupts the worker; a slow link can
                        // take minutes, and a cancel that does nothing is a lie.
                        throw new McException("the installer download was cancelled");
                    }
                    copied += read;
                    if (copied > MAX_INSTALLER_BYTES) {
                        throw new McException("the installer is larger than the panel accepts");
                    }
                    out.write(buffer, 0, read);
                    if (progress != null && copied - lastReport >= 512 * 1024) {
                        lastReport = copied;
                        progress.update(copied, -1);
                    }
                }
            }
        } catch (IOException e) {
            throw new McException("Failed to download the installer: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("the installer download was interrupted", e);
        }
    }

    /// Verifies the installer against the `.sha1` upstream publishes beside it.
    ///
    /// A mirror may not carry the companion file; in that case the download is
    /// accepted with the zip check alone, which is stated in the class comment.
    private void verifySha1(String url, Path file) throws McException {
        String text;
        try {
            text = fetchText(URI.create(url + ".sha1")).strip();
        } catch (McException e) {
            // A mirror without a `.sha1` is not a reason to refuse the install, but it is a
            // reason to say out loud that the jar was only checked as a zip.
            LOG.warning("No .sha1 published for " + url + "; the installer is unverified beyond"
                    + " its zip structure: " + e.getMessage());
            return;
        }
        if (text.isEmpty()) {
            LOG.warning("The .sha1 for " + url + " is empty; the installer is unverified beyond"
                    + " its zip structure");
            return;
        }
        String expected = text.split("\\s+")[0].toLowerCase(Locale.ROOT);
        String actual;
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            actual = HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new McException("Failed to hash the installer: " + e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new McException("SHA-1 is not available in this JVM", e);
        }
        if (!actual.equals(expected)) {
            throw new McException("the installer failed its SHA-1 check (expected " + expected
                    + ", got " + actual + ")");
        }
    }

    /// Finds what the installer left behind: the modern argument file first,
    /// then the pre-1.17 launcher jar.
    private static Launch discover(Path instanceDir, Build build) throws McException {
        Path args = findArgsFile(instanceDir);
        if (args != null) {
            Path relative = instanceDir.normalize().relativize(args.normalize());
            return new Launch(relative, null);
        }
        for (String name : List.of("forge", "neoforge")) {
            try (var stream = Files.list(instanceDir)) {
                for (Path file : stream.toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.startsWith(name + "-") && fileName.endsWith(".jar")
                            && !fileName.contains("installer") && Files.isRegularFile(file)) {
                        return new Launch(null, fileName);
                    }
                }
            } catch (IOException e) {
                throw new McException("Failed to inspect " + instanceDir + ": " + e.getMessage(), e);
            }
        }
        throw new McException("the " + build.core() + " installer finished but left no launch profile"
                + " (no libraries/…/unix_args.txt and no launcher jar)");
    }

    private static @Nullable Path findArgsFile(Path instanceDir) {
        Path libraries = instanceDir.resolve("libraries");
        if (!Files.isDirectory(libraries)) {
            return null;
        }
        try (var stream = Files.walk(libraries, 6)) {
            return stream.filter(path -> path.getFileName().toString().equals("unix_args.txt"))
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparing(Path::toString))
                    .orElse(null);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not scan " + libraries + " for unix_args.txt: " + e.getMessage());
            return null;
        }
    }

    private static void requireZip(Path file) throws McException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4 || magic[0] != 'P' || magic[1] != 'K') {
                throw new McException("the downloaded installer is not a jar (zip) archive");
            }
        } catch (IOException e) {
            throw new McException("Failed to inspect " + file, e);
        }
    }

    private List<Build> cachedBuilds() throws McException {
        Cached current = cached;
        if (current != null && java.time.Instant.now().isBefore(current.at().plus(METADATA_TTL))) {
            return current.builds();
        }
        List<Build> builds = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String source : buildSources()) {
            // Each collector gets its own try: Forge's promotions file and
            // NeoForge's Maven are different hosts, and one being unreachable
            // must not take the other's version list with it.
            if (McSources.BMCLAPI.equals(source)) {
                try {
                    collectForgeFromMaven(builds, McSources.BMCLAPI_MAVEN);
                } catch (McException e) {
                    failures.add(source + " forge: " + e.getMessage());
                    LOG.warning("The " + source + " source could not list forge builds: "
                            + e.getMessage());
                }
                try {
                    collectNeoForgeFromMaven(builds, McSources.BMCLAPI_MAVEN);
                } catch (McException e) {
                    failures.add(source + " neoforge: " + e.getMessage());
                    LOG.warning("The " + source + " source could not list neoforge builds: "
                            + e.getMessage());
                }
            } else {
                try {
                    collectForge(builds);
                } catch (McException e) {
                    failures.add(source + " forge: " + e.getMessage());
                    LOG.warning("The " + source + " source could not list forge builds: "
                            + e.getMessage());
                }
                try {
                    collectNeoForge(builds);
                } catch (McException e) {
                    failures.add(source + " neoforge: " + e.getMessage());
                    LOG.warning("The " + source + " source could not list neoforge builds: "
                            + e.getMessage());
                }
            }
        }
        if (builds.isEmpty()) {
            throw new McException("could not read the Forge/NeoForge metadata"
                    + (failures.isEmpty() ? "" : " (" + String.join("; ", failures) + ")"));
        }
        List<Build> result = List.copyOf(builds);
        cached = new Cached(result, java.time.Instant.now());
        return result;
    }

    /// Which sources can answer a *build list*. MSL can name the game versions
    /// and hand out one installer per version, but it does not publish loader
    /// builds, so it is consulted on demand in [resolve] instead.
    private List<String> buildSources() {
        if (sources == null) {
            return List.of(McSources.OFFICIAL);
        }
        java.util.LinkedHashSet<String> order = new java.util.LinkedHashSet<>();
        order.addAll(sources.orderFor("forge"));
        order.addAll(sources.orderFor("neoforge"));
        order.remove(McSources.MSLMC);
        if (order.isEmpty()) {
            order.add(McSources.OFFICIAL);
        }
        return List.copyOf(order);
    }

    /// The Maven mirror the installer should fetch its libraries from.
    ///
    /// This is about the libraries, not about where the installer jar came from:
    /// BMCLAPI proxies the whole Maven tree, so it is used whenever the operator
    /// listed it for this core — even if the *build* was found on the official
    /// host. `--mirror` needs the trailing slash (Forge concatenates strings).
    @Nullable String mirrorFor(String core) {
        if (sources == null || !sources.orderFor(core).contains(McSources.BMCLAPI)) {
            return null;
        }
        return McSources.BMCLAPI_MAVEN.toString().replaceAll("/+$", "") + "/";
    }

    /// Forge's promotions: `1.21.4-recommended` / `1.21.4-latest` → loader build.
    private void collectForge(List<Build> builds) throws McException {
        JsonObject root = fetchJson(forgePromotions);
        JsonObject promos = root.has("promos") && root.get("promos").isJsonObject()
                ? root.getAsJsonObject("promos") : null;
        if (promos == null) {
            throw new McException(forgePromotions + " has no `promos` object");
        }
        List<String> mcVersions = new ArrayList<>();
        java.util.Map<String, String> recommended = new java.util.HashMap<>();
        java.util.Map<String, String> latest = new java.util.HashMap<>();
        for (var entry : promos.entrySet()) {
            String key = entry.getKey();
            String build = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : null;
            if (build == null || build.isBlank()) {
                continue;
            }
            if (key.endsWith("-recommended")) {
                String mc = key.substring(0, key.length() - "-recommended".length());
                recommended.put(mc, build);
                mcVersions.add(mc);
            } else if (key.endsWith("-latest")) {
                String mc = key.substring(0, key.length() - "-latest".length());
                latest.put(mc, build);
                mcVersions.add(mc);
            }
        }
        for (String mc : mcVersions) {
            String build = recommended.getOrDefault(mc, latest.get(mc));
            if (build == null) {
                continue;
            }
            String full = mc + "-" + build;
            String url = base(forgeMaven) + "/net/minecraftforge/forge/" + full
                    + "/forge-" + full + "-installer.jar";
            builds.add(new Build("forge", mc, build, url));
        }
    }

    /// NeoForge's Maven metadata: `<version>21.4.123</version>` → game 1.21.4.
    private void collectNeoForge(List<Build> builds) throws McException {
        collectNeoForgeFromMaven(builds, neoMaven);
    }

    private void collectNeoForgeFromMaven(List<Build> builds, URI maven) throws McException {
        String xml = fetchText(URI.create(base(maven)
                + "/releases/net/neoforged/neoforge/maven-metadata.xml"));
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(xml);
        while (matcher.find()) {
            String version = matcher.group(1).trim();
            if (version.contains("-beta") || version.contains("-alpha")) {
                continue;
            }
            String[] parts = version.split("\\.");
            if (parts.length < 3) {
                continue;
            }
            String mc;
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                mc = "1." + major + "." + minor;
            } catch (NumberFormatException e) {
                continue;
            }
            String url = base(maven) + "/releases/net/neoforged/neoforge/" + version
                    + "/neoforge-" + version + "-installer.jar";
            builds.add(new Build("neoforge", mc, version, url));
        }
    }

    /// Forge's Maven metadata. Used when the promotions JSON is unreachable —
    /// which is the normal state on a domestic line, and the reason BMCLAPI
    /// proxies the Maven tree instead of the promotions file.
    ///
    /// `<version>1.20.4-49.2.13</version>` → game 1.20.4, loader 49.2.13.
    private void collectForgeFromMaven(List<Build> builds, URI maven) throws McException {
        String xml = fetchText(URI.create(base(maven)
                + "/net/minecraftforge/forge/maven-metadata.xml"));
        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(xml);
        while (matcher.find()) {
            String full = matcher.group(1).trim();
            int dash = full.lastIndexOf('-');
            if (dash <= 0 || dash == full.length() - 1) {
                continue;
            }
            String mc = full.substring(0, dash);
            String loader = full.substring(dash + 1);
            String url = base(maven) + "/net/minecraftforge/forge/" + full
                    + "/forge-" + full + "-installer.jar";
            builds.add(new Build("forge", mc, loader, url));
        }
    }

    // -------------------------------------------------------------------- mslmc --

    /// Whether MSL may be asked at all: enabled for one of the two cores, and
    /// not out of its download quota.
    private boolean mslmcEnabled() {
        if (sources == null || sources.exhausted(McSources.MSLMC)) {
            return false;
        }
        return sources.orderFor("forge").contains(McSources.MSLMC)
                || sources.orderFor("neoforge").contains(McSources.MSLMC);
    }

    /// The game versions MSL lists for one core. Empty when MSL is unavailable.
    private List<String> mslmcVersions(String core) throws McException {
        if (!mslmcEnabled()) {
            return List.of();
        }
        JsonObject root = mslmcGet("/mirrors/" + core, false);
        if (!root.has("data") || !root.get("data").isJsonObject()) {
            return List.of();
        }
        JsonObject data = root.getAsJsonObject("data");
        List<String> versions = new ArrayList<>();
        if (data.has("versions") && data.get("versions").isJsonArray()) {
            for (var element : data.getAsJsonArray("versions")) {
                if (element.isJsonPrimitive()) {
                    versions.add(element.getAsString());
                }
            }
        }
        return versions;
    }

    /// One installer from MSL, or `null` when the source is off or the call
    /// fails. `resolve` treats `null` as "this source has nothing".
    private @Nullable Build mslmcBuild(String core, String mcVersion) {
        if (!mslmcEnabled()) {
            return null;
        }
        try {
            JsonObject root = mslmcGet("/download/server/" + core + "/" + mcVersion, true);
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return null;
            }
            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("url") || !data.get("url").isJsonPrimitive()) {
                return null;
            }
            String url = data.get("url").getAsString();
            return new Build(core, mcVersion, loaderFromUrl(core, url), url);
        } catch (McException e) {
            LOG.warning("MSL could not resolve " + core + " " + mcVersion + ": " + e.getMessage());
            return null;
        }
    }

    /// The loader version is encoded in the installer's file name:
    /// `forge-1.20.4-49.2.13-installer.jar` → `49.2.13`,
    /// `neoforge-20.4.253-installer.jar` → `20.4.253`.
    static String loaderFromUrl(String core, String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        String prefix = core + "-";
        String suffix = "-installer.jar";
        if (name.startsWith(prefix) && name.endsWith(suffix) && name.length() > prefix.length() + suffix.length()) {
            String middle = name.substring(prefix.length(), name.length() - suffix.length());
            if ("forge".equals(core)) {
                int dash = middle.indexOf('-');
                return dash > 0 ? middle.substring(dash + 1) : middle;
            }
            return middle;
        }
        return "latest";
    }

    private JsonObject mslmcGet(String path, boolean metered) throws McException {
        URI custom = sources == null ? null : sources.customBase("mslmc");
        String api = base(custom != null ? custom : McSources.MSLMC_API);
        URI uri = URI.create(api + path);
        if (metered) {
            sources.spend(McSources.MSLMC);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", McSources.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> answer = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (sources != null) {
                sources.observe(McSources.MSLMC,
                        headerInt(answer, "x-ratelimit-limit"),
                        headerInt(answer, "x-ratelimit-remaining"),
                        headerLong(answer, "x-ratelimit-reset"));
            }
            if (answer.statusCode() != 200) {
                throw new McException(uri + " answered HTTP " + answer.statusCode());
            }
            return JsonParser.parseString(answer.body()).getAsJsonObject();
        } catch (IOException e) {
            throw new McException("Failed to read " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("Reading " + uri + " was interrupted", e);
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer JSON", e);
        }
    }

    private static int headerInt(HttpResponse<?> answer, String name) {
        String value = answer.headers().firstValue(name).orElse("");
        try {
            return value.isBlank() ? -1 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static long headerLong(HttpResponse<?> answer, String name) {
        String value = answer.headers().firstValue(name).orElse("");
        try {
            return value.isBlank() ? 0 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// Runs the installer the way upstream documents: `java -jar … --installServer`.
    /// The argv for Forge's installer.
    ///
    /// `--mirror` must end with a slash: the installer concatenates it with the
    /// artifact path, so `.../maven` + `com/...` becomes `mavencom/...` and every
    /// library 404s back to the official host.
    static List<String> installerArgv(String javaBin, Path installer, @Nullable String mirror) {
        List<String> argv = new ArrayList<>(List.of(javaBin, "-jar", installer.toString(),
                "--installServer"));
        if (mirror != null) {
            argv.add("--mirror");
            argv.add(mirror.endsWith("/") ? mirror : mirror + "/");
        }
        return argv;
    }

    private static int runInstaller(String javaBin, Path installer, Path directory,
                                    @Nullable String mirror,
                                    java.util.function.Consumer<String> log)
            throws IOException, InterruptedException {
        // The installer is third-party code, and inside the container the panel is root:
        // running it as the panel would hand a Maven mirror's jar the panel's own credentials.
        // It runs as the agent, like the server it is installing for.
        ProcessBuilder builder = new ProcessBuilder(Privileges.asAgent(
                installerArgv(javaBin, installer, mirror)));
        builder.directory(directory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        // Drained on its own thread. Reading first and waiting afterwards is how a deadline
        // never fires: an installer that hangs without closing stdout blocks the read forever,
        // and the request thread with it (the same shape as docs §5.8).
        Thread pump = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.accept(line);
                }
            } catch (IOException ignored) {
                // The process is gone or was killed; the exit code carries the answer.
            }
        }, "mc-forge-installer");
        pump.setDaemon(true);
        pump.start();
        try {
            if (!process.waitFor(20, TimeUnit.MINUTES)) {
                ProcessTree.terminate(process, 2);
                return -1;
            }
            pump.join(2_000);
            return process.exitValue();
        } catch (InterruptedException e) {
            ProcessTree.terminate(process, 2);
            throw e;
        }
    }

    /// Numeric version comparison (`1.21.4` > `1.21.1`; `54.0.16` > `54.0.9`).
    static int compareVersions(String left, String right) {
        String[] a = left.split("[^0-9]+");
        String[] b = right.split("[^0-9]+");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length && !a[i].isBlank() ? parseInt(a[i]) : 0;
            int y = i < b.length && !b[i].isBlank() ? parseInt(b[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String base(URI uri) {
        return uri.toString().replaceAll("/+$", "");
    }

    private JsonObject fetchJson(URI uri) throws McException {
        try {
            return JsonParser.parseString(fetchText(uri)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer a JSON object", e);
        }
    }

    private String fetchText(URI uri) throws McException {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", McSources.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> answer = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (answer.statusCode() != 200) {
                throw new McException(uri + " answered HTTP " + answer.statusCode());
            }
            return answer.body();
        } catch (IOException e) {
            throw new McException("Failed to read " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("Reading " + uri + " was interrupted", e);
        }
    }

    /// What the installer recorded, for the API and the tests.
    ///
    /// @param argsFile the relative argument file, or `null` for a launcher jar
    /// @param jar      the launcher jar's name, or `null` for an argument file
    public record Launch(@Nullable Path argsFile, @Nullable String jar) {
    }
}
