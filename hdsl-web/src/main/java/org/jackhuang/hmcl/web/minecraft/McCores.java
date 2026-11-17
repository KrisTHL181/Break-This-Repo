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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// The server cores the panel can run, and the one download each needs.
///
/// **Vanilla is the baseline; the others are what people actually run.** Paper,
/// Folia and Purpur publish a JSON API with a per-build hash, so the panel
/// verifies exactly what it downloaded. Fabric publishes no hash for its
/// launcher jar; that jar is checked to be a zip and nothing more, which is
/// stated here rather than pretended otherwise.
///
/// **Every core is fetched through [McSources].** The first source that answers
/// wins, `official` is always in the list, and a mirror is only used when the
/// upstream cannot be reached from this host. A resolved artifact — URL and
/// hash — is written beside the jar, so a restart, a second instance and a
/// second wizard visit all reuse the same answer instead of asking again.
/// That matters because a mirror that publishes a download-URL endpoint also
/// meters it (MSL: 30/hour, 60/day), and re-asking is how a panel burns a
/// quota it cannot see.
///
/// **Downloads are shared.** One `cores/<core>/<version>/server.jar` serves every
/// instance that wants it, and the instance's jar is a hard link (a copy where
/// the filesystem refuses links, e.g. across volumes on Windows).
@NotNullByDefault
public final class McCores {

    /// Paper's API also answers Folia; the two differ only in the project id.
    /// v3 (`fill.papermc.io`) is the current API — v2 answered HTTP 410 `sunset`
    /// from 2025 on, which is why a panel pinned to it showed an empty list.
    public static final URI DEFAULT_PAPER_API = McSources.OFFICIAL_PAPER;
    /// Purpur runs the same API shape under its own host.
    public static final URI DEFAULT_PURPUR_API = McSources.OFFICIAL_PURPUR;
    /// Fabric's meta API; the server launcher jar comes from it directly.
    public static final URI DEFAULT_FABRIC_API = McSources.OFFICIAL_FABRIC;

    /// The cores this class installs itself; `vanilla` is [McVersions], and
    /// `forge`/`neoforge` delegate to [McForge] because they are installed
    /// rather than downloaded as a ready jar.
    public static final List<String> THIRD_PARTY =
            List.of("paper", "folia", "purpur", "fabric", "forge", "neoforge");

    private static final Logger LOG = Logger.LOG;
    private static final long MAX_JAR_BYTES = 512L * 1024 * 1024;
    /// A source that answers the headers but then trickles below this average
    /// over a ten-second window is treated as dead, and the next source is
    /// tried. Official Purpur does exactly that from some networks: 200 OK,
    /// then single-digit KiB/s for a 50 MB jar. Without this, the task sits on
    /// the request timeout while a working mirror waits.
    private static final Duration DOWNLOAD_WINDOW = Duration.ofSeconds(10);
    private static final long DOWNLOAD_WINDOW_MIN_BYTES = 640L * 1024;
    /// The absolute ceiling for one jar; the window above catches the common
    /// "headers answer, body trickles" case, this stops a silent socket.
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
    /// A successful version list is reused for ten minutes; a failed one for two,
    /// so an unreachable upstream is not retried on every wizard open — that
    /// retry is what turned one slow API into a two-minute page.
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration FAILURE_TTL = Duration.ofMinutes(2);
    /// The budget for racing a core's version lists (official vs mirror).
    private static final Duration VERSION_RACE_TIMEOUT = Duration.ofSeconds(10);
    /// All upstreams are read in parallel; one hung API must not hold the others.
    /// Twelve seconds keeps the create form under a typical 15s proxy timeout and
    /// leaves the slow cores listed with an empty list (the UI offers a retry).
    private static final Duration CORE_FETCH_TIMEOUT = Duration.ofSeconds(12);

    private record CachedVersions(List<String> versions, Instant at) {
    }

    /// One core, as the create form shows it.
    ///
    /// @param id       the core id
    /// @param label    the human name
    /// @param versions the game versions it publishes, newest first
    public record CoreInfo(String id, String label, List<String> versions) {
    }

    /// One verified download.
    ///
    /// @param source    the source id that answered
    /// @param url       the jar's URL
    /// @param algorithm the digest algorithm, empty when upstream publishes none
    /// @param digest    the expected digest, empty when upstream publishes none
    /// @param size      the declared size, `-1` when unknown
    /// @param build     the build id, for the record beside the jar
    private record Artifact(String source, String url, String algorithm, String digest, long size,
                            String build) {
    }

    private final McVersions vanilla;
    private final McForge forge;
    private final Path coresRoot;
    private final URI paperApi;
    private final URI purpurApi;
    private final URI fabricApi;
    private final @Nullable McSources sources;
    private final HttpClient client;
    private final Map<String, CachedVersions> versionCache = new ConcurrentHashMap<>();

    /// The production constructor: every upstream is reached through the
    /// operator's source order, with the artifact cache beside the jars.
    ///
    /// @param vanilla   the Mojang catalogue, reused for the `vanilla` core
    /// @param coresRoot the shared directory (`<data>/minecraft/cores`)
    /// @param sources   the source settings
    public McCores(McVersions vanilla, Path coresRoot, McSources sources) {
        this(vanilla, coresRoot, DEFAULT_PAPER_API, DEFAULT_PURPUR_API, DEFAULT_FABRIC_API,
                new McForge(sources), sources);
    }

    /// @param vanilla   the Mojang catalogue, reused for the `vanilla` core
    /// @param coresRoot the shared directory (`<data>/minecraft/cores`)
    public McCores(McVersions vanilla, Path coresRoot) {
        this(vanilla, coresRoot, DEFAULT_PAPER_API, DEFAULT_PURPUR_API, DEFAULT_FABRIC_API);
    }

    /// The same, with the Forge/NeoForge installer wired in (tests, mirrors).
    ///
    /// @param vanilla   the Mojang catalogue
    /// @param coresRoot the shared directory
    /// @param forge     the Forge/NeoForge installer
    public McCores(McVersions vanilla, Path coresRoot, McForge forge) {
        this(vanilla, coresRoot, DEFAULT_PAPER_API, DEFAULT_PURPUR_API, DEFAULT_FABRIC_API, forge);
    }

    /// The same, with overridable API bases (mirrors, tests).
    ///
    /// @param vanilla   the Mojang catalogue
    /// @param coresRoot the shared directory
    /// @param paperApi  PaperMC's API base
    /// @param purpurApi Purpur's API base
    /// @param fabricApi Fabric's meta API base
    public McCores(McVersions vanilla, Path coresRoot, URI paperApi, URI purpurApi, URI fabricApi) {
        this(vanilla, coresRoot, paperApi, purpurApi, fabricApi, new McForge());
    }

    /// The same, with every endpoint and the installer injectable.
    ///
    /// @param vanilla   the Mojang catalogue
    /// @param coresRoot the shared directory
    /// @param paperApi  PaperMC's API base
    /// @param purpurApi Purpur's API base
    /// @param fabricApi Fabric's meta API base
    /// @param forge     the Forge/NeoForge installer
    public McCores(McVersions vanilla, Path coresRoot, URI paperApi, URI purpurApi, URI fabricApi,
                   McForge forge) {
        this(vanilla, coresRoot, paperApi, purpurApi, fabricApi, forge, null);
    }

    private McCores(McVersions vanilla, Path coresRoot, URI paperApi, URI purpurApi, URI fabricApi,
                    McForge forge, @Nullable McSources sources) {
        this.vanilla = vanilla;
        this.forge = forge;
        this.coresRoot = coresRoot.toAbsolutePath().normalize();
        this.paperApi = paperApi;
        this.purpurApi = purpurApi;
        this.fabricApi = fabricApi;
        this.sources = sources;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /// The source settings, or `null` for the test constructors.
    ///
    /// @return the settings
    public @Nullable McSources sources() {
        return sources;
    }

    /// Whether an id names a core this panel knows.
    ///
    /// @param core the core id
    /// @return whether it is `vanilla` or one of [THIRD_PARTY]
    public static boolean known(@Nullable String core) {
        return McInstance.CORE_VANILLA.equals(core) || (core != null && THIRD_PARTY.contains(core));
    }

    /// Whether an id names a core that is not Mojang's.
    ///
    /// @param core the core id
    /// @return whether the id is one of [THIRD_PARTY]
    public static boolean isThirdParty(@Nullable String core) {
        return core != null && THIRD_PARTY.contains(core);
    }

    /// The display name of a core.
    ///
    /// @param core the core id
    /// @return the label
    public static String labelOf(String core) {
        return switch (core) {
            case "paper" -> "Paper";
            case "folia" -> "Folia";
            case "purpur" -> "Purpur";
            case "fabric" -> "Fabric";
            case "forge" -> "Forge";
            case "neoforge" -> "NeoForge";
            default -> "Vanilla";
        };
    }

    /// The catalogue the create form shows.
    ///
    /// Third-party cores are only listed when the operator has enabled them:
    /// while the second gate is off the feature must look like it does not
    /// exist (`docs/minecraft.md` §5.5).
    ///
    /// @param thirdPartyEnabled whether the third-party gate is on
    /// @return vanilla first, then the enabled cores
    public List<CoreInfo> catalog(boolean thirdPartyEnabled) {
        return catalog(thirdPartyEnabled, false);
    }

    /// The catalogue, optionally bypassing the cache.
    ///
    /// @param thirdPartyEnabled whether the third-party gate is on
    /// @param refresh           read the upstreams again instead of using the cache
    /// @return vanilla first, then the enabled cores
    public List<CoreInfo> catalog(boolean thirdPartyEnabled, boolean refresh) {
        List<CoreInfo> cores = new ArrayList<>();
        List<String> vanillaVersions;
        try {
            vanillaVersions = vanilla.list().stream()
                    .filter(entry -> "release".equals(entry.type()))
                    .map(McVersions.VersionEntry::id)
                    .toList();
        } catch (McException e) {
            LOG.warning("Could not read the vanilla version list: " + e.getMessage());
            vanillaVersions = List.of();
        }
        cores.add(new CoreInfo(McInstance.CORE_VANILLA, "Vanilla", vanillaVersions));
        if (!thirdPartyEnabled) {
            return List.copyOf(cores);
        }
        // Six sequential 30-second HTTP timeouts made the create form sit on
        // "only Vanilla" for minutes. Start them together; one dead API costs
        // its own timeout, not everyone else's. Successful and failed answers
        // are both cached, so reopening the wizard is instant.
        ExecutorService executor = VirtualThreads.newPerTaskExecutor();
        Map<String, Future<List<String>>> futures = new LinkedHashMap<>();
        try {
            for (String core : THIRD_PARTY) {
                futures.put(core, executor.submit(() -> versions(core, refresh)));
            }
            long deadline = System.nanoTime() + CORE_FETCH_TIMEOUT.toNanos();
            for (Map.Entry<String, Future<List<String>>> entry : futures.entrySet()) {
                String core = entry.getKey();
                Future<List<String>> future = entry.getValue();
                List<String> versions;
                // The deadline is for the whole catalogue, not per core: waiting
                // 12 seconds six times over is exactly the two-minute page this
                // parallel read exists to avoid.
                long remaining = deadline - System.nanoTime();
                try {
                    if (future.isDone()) {
                        versions = future.get();
                    } else if (remaining <= 0) {
                        future.cancel(true);
                        LOG.warning("Reading the " + core + " version list timed out");
                        versions = List.of();
                    } else {
                        versions = future.get(remaining, TimeUnit.NANOSECONDS);
                    }
                } catch (TimeoutException e) {
                    future.cancel(true);
                    LOG.warning("Reading the " + core + " version list timed out");
                    versions = List.of();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    LOG.warning("Could not read the " + core + " version list: " + cause.getMessage());
                    versions = List.of();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warning("Reading the core version lists was interrupted");
                    versions = List.of();
                }
                cores.add(new CoreInfo(core, labelOf(core), versions));
            }
        } finally {
            executor.shutdownNow();
        }
        return List.copyOf(cores);
    }

    /// The game versions one core publishes, newest first.
    ///
    /// @param core the core id
    /// @return the versions
    /// @throws McException when the core is unknown or its API cannot be read
    public List<String> versions(String core) throws McException {
        return versions(core, false);
    }

    /// The same, optionally bypassing the cache (the wizard's retry).
    ///
    /// @param core    the core id
    /// @param refresh read the upstream again instead of using the cache
    /// @return the versions
    /// @throws McException when the core is unknown or its API cannot be read
    public List<String> versions(String core, boolean refresh) throws McException {
        CachedVersions cached = versionCache.get(core);
        if (!refresh && cached != null) {
            Duration ttl = cached.versions().isEmpty() ? FAILURE_TTL : CACHE_TTL;
            if (Instant.now().isBefore(cached.at().plus(ttl))) {
                return cached.versions();
            }
        }
        try {
            List<String> versions = loadVersions(core);
            versionCache.put(core, new CachedVersions(versions, Instant.now()));
            return versions;
        } catch (McException e) {
            // A failed read is cached briefly too: otherwise every page open
            // spends another timeout on an upstream that is down.
            versionCache.put(core, new CachedVersions(List.of(), Instant.now()));
            throw e;
        }
    }

    private List<String> loadVersions(String core) throws McException {
        if (McInstance.CORE_VANILLA.equals(core)) {
            return vanilla.list().stream()
                    .filter(entry -> "release".equals(entry.type()))
                    .map(McVersions.VersionEntry::id)
                    .toList();
        }
        List<String> order = sources == null ? List.of(McSources.OFFICIAL) : sources.orderFor(core);
        // The sources are raced, not tried in sequence. Upstreams that are
        // unreachable from a domestic line fail by *timing out* — waiting eight
        // seconds before asking the mirror is exactly the two-minute wizard this
        // rewrite exists to remove. The mirror's version listing is a different
        // counter from its metered download endpoint, so racing it costs nothing.
        List<String> labels = new ArrayList<>();
        List<java.util.concurrent.Callable<List<String>>> tasks = new ArrayList<>();
        boolean mslmc = order.contains(McSources.MSLMC) && McSources.supports(McSources.MSLMC, core);
        if (order.contains(McSources.OFFICIAL) || sources == null) {
            if (McForge.isInstalledCore(core)) {
                labels.add(McSources.OFFICIAL);
                tasks.add(() -> forge.gameVersions(core));
            } else {
                labels.add(McSources.OFFICIAL);
                tasks.add(() -> switch (core) {
                    case "paper" -> paperVersions("paper");
                    case "folia" -> paperVersions("folia");
                    case "purpur" -> purpurVersions();
                    case "fabric" -> fabricVersions();
                    default -> throw new McException("unknown server core `" + core + "`");
                });
            }
        }
        if (mslmc) {
            labels.add(McSources.MSLMC);
            tasks.add(() -> mslmcVersions(core));
        }
        if (tasks.isEmpty()) {
            throw new McException("no download source can list " + core + " versions");
        }
        if (tasks.size() == 1) {
            try {
                return tasks.get(0).call();
            } catch (McException e) {
                throw e;
            } catch (Exception e) {
                throw new McException("could not list " + core + " versions: " + e.getMessage(), e);
            }
        }
        ExecutorService executor = VirtualThreads.newPerTaskExecutor();
        try {
            java.util.concurrent.CompletionService<List<String>> service =
                    new java.util.concurrent.ExecutorCompletionService<>(executor);
            for (java.util.concurrent.Callable<List<String>> task : tasks) {
                service.submit(task);
            }
            List<String> failures = new ArrayList<>();
            long deadline = System.nanoTime() + VERSION_RACE_TIMEOUT.toNanos();
            for (int i = 0; i < tasks.size(); i++) {
                long remaining = deadline - System.nanoTime();
                Future<List<String>> done = remaining > 0
                        ? service.poll(remaining, TimeUnit.NANOSECONDS)
                        : service.poll();
                if (done == null) {
                    failures.add("timed out");
                    break;
                }
                try {
                    List<String> versions = done.get();
                    if (!versions.isEmpty()) {
                        return versions;
                    }
                    failures.add("empty list");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    failures.add(cause.getMessage());
                    LOG.warning("A source could not list " + core + " versions: " + cause.getMessage());
                }
            }
            throw new McException("no download source could list " + core + " versions"
                    + (failures.isEmpty() ? "" : " (" + String.join("; ", failures) + ")"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("listing " + core + " versions was interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /// Installs one version of the instance's core and points the instance at it.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param version  the game version
    /// @param progress the download sink, or `null`
    /// @return the instance with its version updated
    /// @throws McException when the core is unknown, the download fails or a
    ///                      hash does not match
    public McInstance install(McInstance instance, McInstanceManager manager, String version,
                              @Nullable McVersions.Progress progress) throws McException {
        return install(instance, manager, version, progress, "java");
    }

    /// The same, with the java the installer runs under.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param version  the game version
    /// @param progress the download sink, or `null`
    /// @param javaBin  the java executable for the Forge/NeoForge installer
    /// @return the instance with its version updated
    /// @throws McException when the core is unknown or the install fails
    public McInstance install(McInstance instance, McInstanceManager manager, String version,
                              @Nullable McVersions.Progress progress, String javaBin)
            throws McException {
        String core = instance.core();
        if (McInstance.CORE_VANILLA.equals(core)) {
            return vanilla.install(instance, manager, version);
        }
        if (!THIRD_PARTY.contains(core)) {
            throw new McException("unknown server core `" + core + "`");
        }
        if (McForge.isInstalledCore(core)) {
            forge.install(manager.instanceDirectory(instance.id()), core, version, javaBin, progress);
            McInstance updated = instance.withVersion(version);
            manager.update(updated);
            return updated;
        }
        Path shared = coresRoot.resolve(core).resolve(version).resolve(McInstanceManager.SERVER_JAR_NAME);
        Artifact cached = readArtifactCache(core, version);
        if (cached == null || !reusable(shared, cached)) {
            downloadFromAnySource(core, version, shared, progress);
        }
        link(shared, manager.serverJar(instance));
        McInstance updated = instance.withVersion(version);
        manager.update(updated);
        return updated;
    }

    /// Downloads one core version, trying the configured sources in order.
    ///
    /// A source that resolves the URL but then fails — 404, a bad hash, or a
    /// body that trickles below [DOWNLOAD_WINDOW_MIN_BYTES] — does not end the
    /// install: the next source is asked. The artifact cache is written only
    /// after a verified download, so a failed source cannot poison the next
    /// attempt with its URL.
    ///
    /// @param core     the core id
    /// @param version  the game version
    /// @param target   where the verified jar goes
    /// @param progress the byte sink, or `null`
    /// @return the artifact that was downloaded
    /// @throws McException when every source fails
    private Artifact downloadFromAnySource(String core, String version, Path target,
                                           @Nullable McVersions.Progress progress) throws McException {
        List<String> order = sources == null ? McSources.fallbackOrder() : sources.orderFor(core);
        List<String> failures = new ArrayList<>();
        for (String source : order) {
            if (!McSources.supports(source, core)) {
                continue;
            }
            if (sources != null && sources.exhausted(source)) {
                failures.add(source + ": quota spent until " + sources.quota(source).resetAt());
                continue;
            }
            Artifact artifact;
            try {
                artifact = resolveFrom(source, core, version);
            } catch (McException e) {
                // A cancelled task must stop, not shop for another mirror.
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                failures.add(source + ": " + e.getMessage());
                LOG.warning("The " + source + " source could not resolve " + core + " " + version
                        + ": " + e.getMessage());
                continue;
            }
            try {
                if (progress != null) {
                    progress.stage("Downloading " + core + " " + version);
                }
                download(artifact, target, progress);
                writeArtifactCache(core, version, artifact);
                return artifact;
            } catch (McException e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                failures.add(source + ": " + e.getMessage());
                LOG.warning("The " + source + " source could not download " + core + " " + version
                        + ": " + e.getMessage());
            }
        }
        throw new McException("no download source could provide " + core + " " + version
                + (failures.isEmpty() ? "" : " (" + String.join("; ", failures) + ")"));
    }

    private Artifact resolveFrom(String source, String core, String version) throws McException {
        if (McSources.MSLMC.equals(source)) {
            return mslmcArtifact(core, version);
        }
        return switch (core) {
            case "paper" -> paperArtifact("paper", version);
            case "folia" -> paperArtifact("folia", version);
            case "purpur" -> purpurArtifact(version);
            case "fabric" -> fabricArtifact(version);
            default -> throw new McException("unknown server core `" + core + "`");
        };
    }

    /// The resolved artifact for one `core/version`, if it was resolved before.
    private @Nullable Artifact readArtifactCache(String core, String version) {
        Path file = coresRoot.resolve(core).resolve(version).resolve("artifact.json");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject record = JsonUtils.fromJsonFile(file, JsonObject.class);
            if (record == null) {
                return null;
            }
            String source = string(record, "source");
            String url = string(record, "url");
            if (source == null || url == null || url.isBlank()) {
                return null;
            }
            if (sources != null && !"manual".equals(source)
                    && !sources.orderFor(core).contains(source)) {
                // The operator turned this source off; resolve again rather than
                // silently downloading from a source they removed.
                return null;
            }
            String algorithm = string(record, "algorithm");
            String digest = string(record, "digest");
            long size = record.has("size") ? record.get("size").getAsLong() : -1;
            String build = string(record, "build");
            return new Artifact(source, url,
                    algorithm == null ? "" : algorithm,
                    digest == null ? "" : digest,
                    size, build == null ? "" : build);
        } catch (Exception e) {
            LOG.warning("Ignoring the unreadable artifact cache " + file, e);
            return null;
        }
    }

    private void writeArtifactCache(String core, String version, Artifact artifact) {
        Path dir = coresRoot.resolve(core).resolve(version);
        Path file = dir.resolve("artifact.json");
        JsonObject record = new JsonObject();
        record.addProperty("source", artifact.source());
        record.addProperty("url", artifact.url());
        record.addProperty("algorithm", artifact.algorithm());
        record.addProperty("digest", artifact.digest());
        record.addProperty("size", artifact.size());
        record.addProperty("build", artifact.build());
        record.addProperty("resolvedAt", Instant.now().toString());
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, "artifact", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, record);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOG.warning("Could not cache the artifact for " + core + " " + version + ": "
                    + e.getMessage());
        }
    }

    /// Installs a jar the operator uploaded.
    ///
    /// The last resort when every source is unreachable: the file is checked to
    /// be a zip, stored beside the downloaded cores with `source: manual`, and
    /// linked into the instance exactly like a resolved download. No hash is
    /// recorded because there is no upstream to compare against — the wizard
    /// says so instead of pretending otherwise.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param version  the game version the jar is for
    /// @param in       the request body
    /// @return the instance with its version updated
    /// @throws McException when the core is vanilla, the upload is not a zip or
    ///                      it is larger than the panel accepts
    public McInstance installUploaded(McInstance instance, McInstanceManager manager,
                                      String version, InputStream in) throws McException {
        String core = instance.core();
        if (!THIRD_PARTY.contains(core)) {
            throw new McException("only third-party cores accept an uploaded jar");
        }
        Path shared = coresRoot.resolve(core).resolve(version)
                .resolve(McInstanceManager.SERVER_JAR_NAME);
        try {
            Files.createDirectories(shared.getParent());
            Path temp = Files.createTempFile(shared.getParent(), "upload", ".jar.part");
            try {
                long copied = 0;
                try (OutputStream out = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new McException("the server download was cancelled");
                        }
                        copied += read;
                        if (copied > MAX_JAR_BYTES) {
                            throw new McException("the uploaded jar is larger than the panel accepts");
                        }
                        out.write(buffer, 0, read);
                    }
                }
                if (copied == 0) {
                    throw new McException("the upload was empty");
                }
                requireZip(temp);
                Files.move(temp, shared, StandardCopyOption.REPLACE_EXISTING);
                writeArtifactCache(core, version,
                        new Artifact("manual", "file://" + shared, "", "", copied, "manual"));
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to store the uploaded jar: " + e.getMessage(), e);
        }
        link(shared, manager.serverJar(instance));
        McInstance updated = instance.withVersion(version);
        manager.update(updated);
        return updated;
    }

    // ------------------------------------------------------------------ paper --

    private List<String> paperVersions(String project) throws McException {
        URI uri = URI.create(base(paperBase()) + "/v3/projects/" + project);
        JsonObject root = fetchJson(uri);
        if (!root.has("versions") || !root.get("versions").isJsonObject()) {
            throw new McException(uri + " did not answer a version map");
        }
        // v3 groups game versions by line (`1.21` → `1.21.4`, `1.21.3`, …).
        // Flattening keeps the published order, which is newest first.
        List<String> versions = new ArrayList<>();
        for (Map.Entry<String, JsonElement> line : root.getAsJsonObject("versions").entrySet()) {
            if (!line.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement element : line.getValue().getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    versions.add(element.getAsString());
                }
            }
        }
        if (versions.isEmpty()) {
            throw new McException(uri + " lists no " + project + " versions");
        }
        return versions;
    }

    private Artifact paperArtifact(String project, String version) throws McException {
        URI uri = URI.create(base(paperBase()) + "/v3/projects/" + project + "/versions/"
                + version + "/builds");
        JsonElement element = fetchJsonElement(uri);
        if (!element.isJsonArray()) {
            throw new McException(uri + " did not answer a build list");
        }
        // Newest first. `server:default` is the runnable jar; `server:mojang`
        // is the same build remapped against Mojang mappings and is not what a
        // server wants to run.
        for (JsonElement candidate : element.getAsJsonArray()) {
            if (!candidate.isJsonObject()) {
                continue;
            }
            JsonObject build = candidate.getAsJsonObject();
            JsonObject downloads = build.has("downloads") && build.get("downloads").isJsonObject()
                    ? build.getAsJsonObject("downloads") : null;
            JsonObject server = downloads != null && downloads.has("server:default")
                    && downloads.get("server:default").isJsonObject()
                    ? downloads.getAsJsonObject("server:default") : null;
            if (server == null) {
                continue;
            }
            String url = string(server, "url");
            if (url == null || url.isBlank()) {
                continue;
            }
            String sha256 = null;
            if (server.has("checksums") && server.get("checksums").isJsonObject()) {
                sha256 = string(server.getAsJsonObject("checksums"), "sha256");
            }
            long size = server.has("size") ? server.get("size").getAsLong() : -1;
            String buildNumber = build.has("id") ? build.get("id").getAsString() : "0";
            return new Artifact(McSources.OFFICIAL, url,
                    sha256 == null ? "" : "SHA-256",
                    sha256 == null ? "" : sha256.toLowerCase(Locale.ROOT),
                    size, buildNumber);
        }
        throw new McException(project + " has no downloadable build for " + version);
    }

    // ----------------------------------------------------------------- purpur --

    private List<String> purpurVersions() throws McException {
        URI uri = URI.create(base(purpurBase()) + "/v2/purpur");
        JsonObject root = fetchJson(uri);
        List<String> versions = stringList(root, "versions");
        if (versions.isEmpty()) {
            throw new McException(uri + " lists no purpur versions");
        }
        return versions;
    }

    private Artifact purpurArtifact(String version) throws McException {
        URI meta = URI.create(base(purpurBase()) + "/v2/purpur/" + version);
        JsonObject root = fetchJson(meta);
        JsonObject builds = root.has("builds") && root.get("builds").isJsonObject()
                ? root.getAsJsonObject("builds") : null;
        String build = builds == null ? null : string(builds, "latest");
        if (build == null || build.isBlank()) {
            throw new McException("purpur has no build for " + version);
        }
        JsonObject details = fetchJson(URI.create(base(purpurBase()) + "/v2/purpur/"
                + version + "/" + build));
        String md5 = string(details, "md5");
        if (md5 == null || md5.isBlank()) {
            throw new McException("purpur does not publish an md5 for " + version + "/" + build);
        }
        String url = base(purpurBase()) + "/v2/purpur/" + version + "/" + build + "/download";
        return new Artifact(McSources.OFFICIAL, url, "MD5", md5.toLowerCase(Locale.ROOT), -1, build);
    }

    // ----------------------------------------------------------------- fabric --

    private List<String> fabricVersions() throws McException {
        URI uri = URI.create(base(fabricBase()) + "/v2/versions/game");
        JsonElement root = fetchJsonElement(uri);
        if (!root.isJsonArray()) {
            throw new McException(uri + " did not answer a version list");
        }
        List<String> stable = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String version = string(entry, "version");
            if (version == null) {
                continue;
            }
            all.add(version);
            boolean isStable = entry.has("stable") && entry.get("stable").isJsonPrimitive()
                    && entry.get("stable").getAsBoolean();
            if (isStable) {
                stable.add(version);
            }
        }
        List<String> versions = stable.isEmpty() ? all : stable;
        if (versions.isEmpty()) {
            throw new McException(uri + " lists no game versions");
        }
        return versions;
    }

    private Artifact fabricArtifact(String version) throws McException {
        String loader = latestFabricLoader();
        String url = base(fabricBase()) + "/v2/versions/loader/" + version + "/" + loader
                + "/server/jar";
        // Fabric publishes no hash for the launcher jar; the download is checked
        // to be a zip and nothing more (stated in the class comment).
        return new Artifact(McSources.OFFICIAL, url, "", "", -1, loader);
    }

    private String latestFabricLoader() throws McException {
        URI uri = URI.create(base(fabricBase()) + "/v2/versions/loader");
        JsonElement root = fetchJsonElement(uri);
        if (!root.isJsonArray()) {
            throw new McException(uri + " did not answer a loader list");
        }
        String newest = null;
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String version = string(entry, "version");
            if (version == null || version.isBlank()) {
                continue;
            }
            boolean isStable = entry.has("stable") && entry.get("stable").isJsonPrimitive()
                    && entry.get("stable").getAsBoolean();
            if (isStable) {
                return version;
            }
            if (newest == null) {
                newest = version;
            }
        }
        if (newest == null) {
            throw new McException(uri + " lists no fabric loader");
        }
        return newest;
    }

    // ------------------------------------------------------------------ mirrors --

    /// The PaperMC base in force: the operator's custom URL when one is set,
    /// otherwise the official v3 API.
    private URI paperBase() {
        URI custom = sources == null ? null : sources.customBase("paper");
        return custom != null ? custom : paperApi;
    }

    private URI purpurBase() {
        URI custom = sources == null ? null : sources.customBase("purpur");
        return custom != null ? custom : purpurApi;
    }

    private URI fabricBase() {
        URI custom = sources == null ? null : sources.customBase("fabric");
        return custom != null ? custom : fabricApi;
    }

    /// One artifact from MSL's metered endpoint.
    ///
    /// The endpoint is the only part of MSL that has a hard budget (30/hour,
    /// 60/day). It is called at most once per `core/version` — [resolve] writes
    /// the answer beside the jar — and the quota headers are recorded so the
    /// next call can skip the source instead of hitting a wall.
    private Artifact mslmcArtifact(String core, String version) throws McException {
        if (sources == null) {
            throw new McException("the MSL source is not configured");
        }
        if (sources.exhausted(McSources.MSLMC)) {
            throw new McException("the MSL download quota is spent until it resets");
        }
        URI custom = sources.customBase("mslmc");
        String api = base(custom != null ? custom : McSources.MSLMC_API);
        URI uri = URI.create(api + "/download/server/" + core + "/" + version);
        sources.spend(McSources.MSLMC);
        HttpResponse<String> answer = fetchString(uri);
        sources.observe(McSources.MSLMC,
                headerInt(answer, "x-ratelimit-limit"),
                headerInt(answer, "x-ratelimit-remaining"),
                headerLong(answer, "x-ratelimit-reset"));
        if (answer.statusCode() != 200) {
            throw new McException(uri + " answered HTTP " + answer.statusCode());
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(answer.body());
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer JSON", e);
        }
        if (!parsed.isJsonObject()) {
            throw new McException(uri + " did not answer a JSON object");
        }
        JsonObject root = parsed.getAsJsonObject();
        if (!root.has("data") || !root.get("data").isJsonObject()) {
            throw new McException(uri + " has no data object");
        }
        JsonObject data = root.getAsJsonObject("data");
        String url = string(data, "url");
        if (url == null || url.isBlank()) {
            throw new McException(uri + " returned no download URL");
        }
        // Not every core gets a hash from MSL; when there is none the download
        // is still checked to be a zip, which is stated in the class comment.
        String sha256 = string(data, "sha256");
        return new Artifact(McSources.MSLMC, url,
                sha256 == null ? "" : "SHA-256",
                sha256 == null ? "" : sha256.toLowerCase(Locale.ROOT),
                -1, "");
    }

    /// The versions one core publishes, as MSL lists them.
    ///
    /// A different counter from the download endpoint (the mirror listing is
    /// not what MSL meters at 60/day), so this is a cheap fallback.
    private List<String> mslmcVersions(String core) throws McException {
        if (sources == null || !McSources.supports(McSources.MSLMC, core)) {
            throw new McException("the MSL source cannot serve " + core);
        }
        URI custom = sources.customBase("mslmc");
        String api = base(custom != null ? custom : McSources.MSLMC_API);
        URI uri = URI.create(api + "/mirrors/" + core);
        HttpResponse<String> answer = fetchString(uri);
        sources.observe(McSources.MSLMC,
                headerInt(answer, "x-ratelimit-limit"),
                headerInt(answer, "x-ratelimit-remaining"),
                headerLong(answer, "x-ratelimit-reset"));
        if (answer.statusCode() != 200) {
            throw new McException(uri + " answered HTTP " + answer.statusCode());
        }
        JsonObject root = JsonParser.parseString(answer.body()).getAsJsonObject();
        if (!root.has("data") || !root.get("data").isJsonObject()) {
            throw new McException(uri + " has no data object");
        }
        JsonObject data = root.getAsJsonObject("data");
        List<String> versions = stringList(data, "versions");
        if (versions.isEmpty()) {
            throw new McException(uri + " lists no " + core + " versions");
        }
        return versions;
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

    // ---------------------------------------------------------------- downloads --

    /// Whether a download window has moved too few bytes to be worth keeping.
    ///
    /// Package-private so the cutoff is testable without a ten-second socket.
    static boolean tooSlow(long bytes, long elapsedNanos) {
        return elapsedNanos >= DOWNLOAD_WINDOW.toNanos()
                && bytes < DOWNLOAD_WINDOW_MIN_BYTES;
    }

    private void download(Artifact artifact, Path target, @Nullable McVersions.Progress progress)
            throws McException {
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "core", ".jar.part");
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(artifact.url()))
                        .timeout(DOWNLOAD_TIMEOUT)
                        .header("User-Agent", McSources.USER_AGENT)
                        .GET()
                        .build();
                HttpResponse<InputStream> answer = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (answer.statusCode() != 200) {
                    throw new McException("the " + artifact.url() + " download answered HTTP "
                            + answer.statusCode());
                }
                MessageDigest digest = artifact.algorithm().isEmpty()
                        ? null : MessageDigest.getInstance(artifact.algorithm());
                long copied = 0;
                long lastReport = 0;
                long windowStart = System.nanoTime();
                long windowBytes = 0;
                try (InputStream in = answer.body(); OutputStream out = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new McException("the server download was cancelled");
                        }
                        copied += read;
                        windowBytes += read;
                        if (copied > MAX_JAR_BYTES) {
                            throw new McException("the server download is larger than the panel accepts");
                        }
                        long now = System.nanoTime();
                        long elapsed = now - windowStart;
                        if (tooSlow(windowBytes, elapsed)) {
                            throw new McException("the " + artifact.source()
                                    + " download is too slow ("
                                    + (windowBytes / 1024) + " KiB in "
                                    + TimeUnit.NANOSECONDS.toSeconds(elapsed)
                                    + " s); trying the next source");
                        }
                        if (elapsed >= DOWNLOAD_WINDOW.toNanos()) {
                            windowStart = now;
                            windowBytes = 0;
                        }
                        if (digest != null) {
                            digest.update(buffer, 0, read);
                        }
                        out.write(buffer, 0, read);
                        if (progress != null && copied - lastReport >= 1024 * 1024) {
                            lastReport = copied;
                            progress.update(copied, artifact.size());
                        }
                    }
                }
                if (digest != null) {
                    String actual = HexFormat.of().formatHex(digest.digest());
                    if (!actual.equalsIgnoreCase(artifact.digest())) {
                        throw new McException("the server download failed its " + artifact.algorithm()
                                + " check (expected " + artifact.digest() + ", got " + actual + ")");
                    }
                }
                requireZip(temp);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                JsonObject record = new JsonObject();
                record.addProperty("core", target.getNameCount() > 2
                        ? target.getParent().getParent().getFileName().toString() : "");
                record.addProperty("version", target.getParent().getFileName().toString());
                record.addProperty("build", artifact.build());
                record.addProperty("source", artifact.source());
                record.addProperty("url", artifact.url());
                if (digest != null) {
                    record.addProperty(artifact.algorithm().equalsIgnoreCase("MD5") ? "md5" : "sha256",
                            artifact.digest());
                }
                record.addProperty("size", copied);
                record.addProperty("javaMajor",
                        McJavaRuntimes.recommendFor(target.getParent().getFileName().toString()));
                JsonUtils.writeToJsonFile(target.getParent().resolve("version.json"), record);
                if (progress != null) {
                    progress.update(copied, artifact.size());
                }
                LOG.info("Downloaded " + artifact.url() + " (" + copied + " bytes)");
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to download the server jar: " + e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new McException(artifact.algorithm() + " is not available in this JVM", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("the server download was interrupted", e);
        }
    }

    private boolean reusable(Path shared, Artifact artifact) {
        if (!Files.isRegularFile(shared)) {
            return false;
        }
        try {
            if (!artifact.algorithm().isEmpty()) {
                String actual = digestOf(shared, artifact.algorithm());
                if (!actual.equalsIgnoreCase(artifact.digest())) {
                    return false;
                }
            }
            requireZip(shared);
            return true;
        } catch (McException | IOException e) {
            return false;
        }
    }

    private static void requireZip(Path file) throws McException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4 || magic[0] != 'P' || magic[1] != 'K') {
                throw new McException("the downloaded server.jar is not a zip archive");
            }
        } catch (IOException e) {
            throw new McException("Failed to inspect " + file, e);
        }
    }

    private static void link(Path shared, Path instanceJar) throws McException {
        try {
            Files.deleteIfExists(instanceJar);
            try {
                Files.createLink(instanceJar, shared);
            } catch (IOException | UnsupportedOperationException e) {
                Files.copy(shared, instanceJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new McException("Failed to place server.jar in the instance: " + e.getMessage(), e);
        }
    }

    private static String digestOf(Path file, String algorithm) throws McException, IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new McException(algorithm + " is not available in this JVM", e);
        }
    }

    // ------------------------------------------------------------------ plumbing --

    private static String base(URI uri) {
        return uri.toString().replaceAll("/+$", "");
    }

    private JsonObject fetchJson(URI uri) throws McException {
        JsonElement element = fetchJsonElement(uri);
        if (!element.isJsonObject()) {
            throw new McException(uri + " did not answer a JSON object");
        }
        return element.getAsJsonObject();
    }

    private JsonElement fetchJsonElement(URI uri) throws McException {
        HttpResponse<String> answer = fetchString(uri);
        try {
            return JsonParser.parseString(answer.body());
        } catch (RuntimeException e) {
            throw new McException(uri + " did not answer JSON", e);
        }
    }

    private HttpResponse<String> fetchString(URI uri) throws McException {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    // Metadata only: a dead upstream should cost seconds, not the
                    // half minute that made the create form look like vanilla-only.
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", McSources.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> answer = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (answer.statusCode() != 200) {
                throw new McException(uri + " answered HTTP " + answer.statusCode());
            }
            return answer;
        } catch (IOException e) {
            throw new McException("Failed to read " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McException("Reading " + uri + " was interrupted", e);
        }
    }

    private static List<String> stringList(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray(name)) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static @Nullable String string(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                ? object.get(name).getAsString() : null;
    }
}
