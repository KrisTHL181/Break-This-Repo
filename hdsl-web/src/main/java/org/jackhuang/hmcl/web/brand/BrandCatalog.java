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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.NpmRegistry;
import org.jackhuang.hmcl.util.platform.OutboundTls;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The published versions of each [Brand] — the version picker the panel shows.
///
/// **Cache first, network in the background.** A page render never waits for a
/// registry or GitHub: it gets whatever the memory cache, then the disk cache,
/// has, and a refresh happens behind it when the entry is stale. A failed
/// refresh never clears a list that worked before.
///
/// Two counters are deliberately separate:
///
/// - **local cooldown**: every attempt counts (success, 304, 403, timeout), so
///   a button cannot turn a user into a retry loop;
/// - **GitHub quota**: only the response headers say what is left. A 304 does
///   not consume GitHub's quota, and a DNS/TLS failure never reached GitHub, so
///   neither may be treated as "one request spent". The picker only stops
///   automatic refreshes when the header says the quota is nearly gone.
///
/// GitHub brands refresh once an hour with `If-None-Match` (a 304 is nearly
/// free); npm registries refresh every 30 minutes and are not rate limited the
/// same way. Concurrent callers share one in-flight request per brand.
@NotNullByDefault
public final class BrandCatalog {

    /// A strict semantic version: the only shape the picker admits.
    private static final Pattern CLEAN_VERSION = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    /// GitHub's version list is stable enough for an hour, and every extra call
    /// counts against an IP-wide quota. npm registries have no such limit.
    static final long GITHUB_TTL_MILLIS = Duration.ofHours(1).toMillis();
    static final long NPM_TTL_MILLIS = Duration.ofMinutes(30).toMillis();

    /// A manual refresh may not be pressed again for this long. It is a local
    /// guard, not a quota: the point is to stop repeated clicks, not to mirror
    /// `X-RateLimit-Remaining`.
    static final long MANUAL_COOLDOWN_MILLIS = Duration.ofMinutes(10).toMillis();

    /// Below this many GitHub requests left, automatic refreshes stop and only
    /// an explicit click may try. Manual clicks still ask for confirmation in
    /// the UI when the remaining count is zero.
    static final int RATE_LIMIT_STOP_THRESHOLD = 10;

    /// How many consecutive failures before automatic refreshes start backing
    /// off (1h, then 2h, then 4h).
    static final int BACKOFF_AFTER_FAILURES = 3;

    /// With nobody opening the panel, GitHub is checked this often. A release
    /// is rare, and a 304 costs nothing, so three hours is enough to keep the
    /// picker current without turning an idle panel into a poller.
    static final long IDLE_REFRESH_MILLIS = Duration.ofHours(3).toMillis();

    /// The first background check waits this long after the server is up. The
    /// startup path itself never touches the network: a panel that prints its
    /// banner immediately is a panel nobody mistakes for hung.
    static final long BACKGROUND_INITIAL_DELAY_MINUTES = 10;

    /// How often the idle check runs. The actual refresh still waits for
    /// [#IDLE_REFRESH_MILLIS] since the last access/check.
    static final long BACKGROUND_CHECK_MINUTES = 15;

    /// The HTTP timeout for one round trip.
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    /// One fetched state of a brand's version list.
    ///
    /// Package-private so [parse] is directly testable.
    record Snapshot(List<String> versions, String latest, long fetchedAt) {
    }

    /// What the page needs to render "where did this list come from and when
    /// will it next be refreshed".
    public record Status(String source, long fetchedAt, long checkedAt, long ttlMillis,
                         int limit, int remaining, long resetAt,
                         long refreshCooldownUntil, long rateLimitedUntil, long backoffUntil,
                         int failures, boolean tokenConfigured, @Nullable String tokenLogin) {
    }

    /// The cached entry. `source` is `memory`, `disk` or `network` — the last
    /// place the rendered list came from, so the UI can say "缓存 · 14:20".
    record Entry(String etag, List<String> versions, String latest,
                 long fetchedAt, long checkedAt,
                 int limit, int remaining, long resetAt,
                 int failures, long backoffUntil, long lastAttemptAt,
                 long rateLimitedUntil, String source) {

        static Entry empty() {
            return new Entry("", List.of(), "", 0, 0, -1, -1, 0,
                    0, 0, 0, 0, "memory");
        }

        Entry withSource(String value) {
            return new Entry(etag, versions, latest, fetchedAt, checkedAt, limit, remaining, resetAt,
                    failures, backoffUntil, lastAttemptAt, rateLimitedUntil, value);
        }
    }

    /// The result of one network attempt, including what the headers said.
    record FetchResult(@Nullable Snapshot snapshot, @Nullable String etag,
                       int limit, int remaining, long resetAt,
                       boolean notModified, boolean rateLimited, boolean failed,
                       @Nullable OutboundTls.Trouble trouble) {
    }

    private static final Map<Brand, Entry> CACHE = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicBoolean BACKGROUND_STARTED =
            new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile long lastUserAccessAt;
    private static final Map<Brand, CompletableFuture<Entry>> INFLIGHT = new ConcurrentHashMap<>();

    /// When the user last pressed refresh, per brand. Kept out of the persisted
    /// entry on purpose: a restart may clear a local cooldown, but it must not
    /// clear a quota window. Only a **manual** attempt sets it, so opening the
    /// page (an automatic refresh) does not disable the button for ten minutes.
    private static final Map<Brand, Long> LAST_MANUAL = new ConcurrentHashMap<>();

    /// `<data>/cache/brand-catalog.json`, set once at startup.
    private static volatile @Nullable Path cacheFile;

    /// The last certificate failure per brand, so the picker can say why it is
    /// empty instead of looking like "this project has no releases".
    private static final Map<Brand, OutboundTls.Trouble> TROUBLE = new ConcurrentHashMap<>();

    /// How long a recorded failure keeps being offered as the explanation.
    private static final long TROUBLE_MEMORY_MINUTES = 30;

    private BrandCatalog() {
    }

    /// Binds the catalog to the panel's data directory and loads its disk
    /// cache. Called once at startup; tests may omit it and stay memory-only.
    ///
    /// @param dataDir the panel data directory
    public static void bind(Path dataDir) {
        cacheFile = dataDir.resolve("cache").resolve("brand-catalog.json");
        GitHubToken.bind(dataDir);
        loadDisk();
    }

    /// The brand's clean versions, newest first. Never blocks on the network
    /// when anything is cached.
    ///
    /// @param brand the brand
    /// @return the versions, newest first
    public static List<String> versions(Brand brand) {
        return load(brand, false).versions();
    }

    /// The version the picker preselects.
    ///
    /// @param brand the brand
    /// @return the version, or `""` when nothing is known
    public static String latest(Brand brand) {
        return load(brand, false).latest();
    }

    /// The list with the current display rules applied (Octop's bundled
    /// dashboards, PI WEB's verified pairs).
    ///
    /// @param brand the brand
    /// @param force ignore the TTL and the manual cooldown (still respects
    ///              rate limits and backoff)
    /// @return the snapshot to render
    public static Snapshot load(Brand brand, boolean force) {
        Entry cached = CACHE.get(brand);
        long now = System.currentTimeMillis();
        if (!force && usable(cached, brand, now)) {
            return snapshotOf(brand, cached.withSource(cached.source()));
        }
        if (cached != null && cached.rateLimitedUntil() > now) {
            return snapshotOf(brand, cached);
        }
        if (cached != null && cached.backoffUntil() > now) {
            return snapshotOf(brand, cached);
        }
        if (force && cached != null
                && now - LAST_MANUAL.getOrDefault(brand, 0L) < MANUAL_COOLDOWN_MILLIS
                && LAST_MANUAL.getOrDefault(brand, 0L) > 0) {
            return snapshotOf(brand, cached);
        }
        // A nearly exhausted GitHub quota is the one case where automatic
        // refreshes stop even though the TTL expired; a click may still try.
        if (!force && cached != null && brand.fromGitHub()
                && cached.remaining() >= 0 && cached.remaining() <= RATE_LIMIT_STOP_THRESHOLD) {
            return snapshotOf(brand, cached);
        }
        CompletableFuture<Entry> future = INFLIGHT.computeIfAbsent(brand, key -> {
            CompletableFuture<Entry> created = CompletableFuture.supplyAsync(
                    () -> refreshSafely(key, force));
            created.whenComplete((value, error) -> INFLIGHT.remove(key, created));
            return created;
        });
        try {
            return snapshotOf(brand, future.get(TIMEOUT.toSeconds() + 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.warning("Could not wait for the " + brand.id() + " version refresh", e);
        }
        return snapshotOf(brand, cached == null ? Entry.empty() : cached);
    }

    /// The display status: source, timestamps, quota and why a refresh is not
    /// happening right now.
    ///
    /// @param brand the brand
    /// @return the status, never `null`
    public static Status status(Brand brand) {
        Entry entry = CACHE.get(brand);
        long now = System.currentTimeMillis();
        GitHubToken.Status token = GitHubToken.status();
        long ttl = brand.fromGitHub() ? GITHUB_TTL_MILLIS : NPM_TTL_MILLIS;
        if (entry == null) {
            return new Status("none", 0, 0, ttl, -1, -1, 0, 0, 0, 0, 0,
                    token.configured(), token.login());
        }
        long manualAt = LAST_MANUAL.getOrDefault(brand, 0L);
        long cooldown = manualAt > 0 ? Math.max(0, manualAt + MANUAL_COOLDOWN_MILLIS - now) : 0;
        return new Status(entry.source(), entry.fetchedAt(), entry.checkedAt(), ttl,
                entry.limit(), entry.remaining(), entry.resetAt(),
                cooldown > 0 ? manualAt + MANUAL_COOLDOWN_MILLIS : 0,
                entry.rateLimitedUntil(), entry.backoffUntil(), entry.failures(),
                token.configured(), token.login());
    }

    /// The certificate failure that emptied this brand's list, if that is what
    /// happened and it is recent enough to still be the reason.
    ///
    /// @param brand the brand
    /// @return the failure, or `null`
    public static @Nullable OutboundTls.Trouble trouble(Brand brand) {
        OutboundTls.Trouble trouble = TROUBLE.get(brand);
        if (trouble == null) {
            return null;
        }
        return Instant.now().isBefore(trouble.at().plus(Duration.ofMinutes(TROUBLE_MEMORY_MINUTES)))
                ? trouble : null;
    }

    /// Records that someone is using the brand surface. While this is recent,
    /// the on-access 1-hour TTL is the only refresh path; the idle scheduler
    /// stays out of the way.
    static void noteUserAccess() {
        lastUserAccessAt = System.currentTimeMillis();
    }

    /// Starts the idle refresh loop: first check ten minutes after the server
    /// is ready, then every fifteen minutes, refreshing a GitHub brand only
    /// when nobody has used the panel for three hours and its cache is that old.
    ///
    /// Nothing here runs during startup: the banner and the health check must
    /// not wait for a registry. Called once from [org.jackhuang.hmcl.web.server.HdslServer].
    public static void startBackgroundRefresh() {
        if (!BACKGROUND_STARTED.compareAndSet(false, true)) {
            return;
        }
        java.util.concurrent.ScheduledExecutorService scheduler =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "brand-catalog-idle");
                    thread.setDaemon(true);
                    return thread;
                });
        scheduler.scheduleWithFixedDelay(BrandCatalog::backgroundTick,
                BACKGROUND_INITIAL_DELAY_MINUTES, BACKGROUND_CHECK_MINUTES, TimeUnit.MINUTES);
    }

    /// One idle check: only GitHub brands, only when the panel has been quiet
    /// long enough for a background request to be invisible to any reader.
    private static void backgroundTick() {
        long now = System.currentTimeMillis();
        if (now - lastUserAccessAt < IDLE_REFRESH_MILLIS) {
            return;
        }
        for (Brand brand : Brand.values()) {
            if (!brand.fromGitHub() || !dueForIdleRefresh(brand, now)) {
                continue;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    load(brand, false);
                } catch (RuntimeException e) {
                    LOG.warning("Idle refresh of the " + brand.id() + " catalog failed", e);
                }
            });
        }
    }

    /// Whether the idle loop should refresh this brand now.
    static boolean dueForIdleRefresh(Brand brand, long now) {
        if (!brand.fromGitHub() || now - lastUserAccessAt < IDLE_REFRESH_MILLIS) {
            return false;
        }
        Entry entry = CACHE.get(brand);
        return entry == null || entry.fetchedAt() <= 0
                || now - entry.checkedAt() >= IDLE_REFRESH_MILLIS;
    }

    /// A token was saved or cleared: the quota and the rate-limit window
    /// belonged to the previous identity, so the next page load must ask again
    /// (with the new token and the cached ETag). Keep the data itself.
    static void afterTokenChanged() {
        CACHE.replaceAll((brand, entry) -> new Entry(entry.etag(), entry.versions(), entry.latest(),
                entry.fetchedAt(), 0, -1, -1, entry.resetAt(),
                entry.failures(), 0, 0, 0, entry.source()));
        LAST_MANUAL.clear();
        saveDisk();
    }

    /// Test seam: forget the memory cache between tests that bind different
    /// data directories.
    static void clearForTests() {
        CACHE.clear();
        INFLIGHT.clear();
        LAST_MANUAL.clear();
        lastUserAccessAt = 0;
        cacheFile = null;
    }

    private static boolean usable(@Nullable Entry entry, Brand brand, long now) {
        if (entry == null || entry.fetchedAt() <= 0) {
            return false;
        }
        long ttl = brand.fromGitHub() ? GITHUB_TTL_MILLIS : NPM_TTL_MILLIS;
        long age = now - entry.checkedAt();
        // A clock that jumped backwards must not turn a cache into "fresh for
        // three days"; treat a future timestamp as stale.
        return age >= 0 && age < ttl;
    }

    private static Snapshot snapshotOf(Brand brand, Entry entry) {
        List<String> versions = entry.versions();
        String latest = entry.latest();
        if (brand == Brand.OCTOP) {
            versions = versions.stream().filter(BrandInstaller::hasBundledOctopDashboard).toList();
            latest = versions.isEmpty() ? "" : versions.get(0);
        } else if (brand == Brand.PI_WEB) {
            versions = versions.stream().filter(brand::offersVersion).toList();
            latest = versions.isEmpty() ? "" : versions.get(0);
        }
        return new Snapshot(List.copyOf(versions), latest, entry.fetchedAt());
    }

    private static Entry refreshSafely(Brand brand, boolean force) {
        Entry cached = CACHE.get(brand);
        long now = System.currentTimeMillis();
        if (force) {
            LAST_MANUAL.put(brand, now);
        }
        // A concurrent non-forced refresh may have finished while this one was
        // queued; don't spend a second request on it.
        if (!force && usable(cached, brand, now)) {
            return cached.withSource(cached.source());
        }
        FetchResult result;
        if (brand.fromGitHub()) {
            result = fetchGitHub(brand, cached);
        } else {
            result = fetchNpm(brand);
        }
        Entry next = merge(brand, cached, result, now);
        CACHE.put(brand, next);
        saveDisk();
        return next;
    }

    /// Turns one network result into the next cache entry.
    private static Entry merge(Brand brand, @Nullable Entry cached, FetchResult result, long now) {
        long lastAttempt = now;
        int failures = result.failed() ? (cached == null ? 1 : cached.failures() + 1) : 0;
        long backoff = result.failed() ? backoffUntil(failures, now) : 0;
        long rateLimited = result.rateLimited()
                ? (result.resetAt() > now ? result.resetAt() : now + Duration.ofHours(1).toMillis())
                : (result.remaining() == 0 && result.resetAt() > now ? result.resetAt() : 0);
        Snapshot snapshot = result.snapshot();
        if (result.notModified() && cached != null) {
            return new Entry(result.etag() == null ? cached.etag() : result.etag(),
                    cached.versions(), cached.latest(), cached.fetchedAt(), now,
                    result.limit() >= 0 ? result.limit() : cached.limit(),
                    result.remaining() >= 0 ? result.remaining() : cached.remaining(),
                    result.resetAt() > 0 ? result.resetAt() : cached.resetAt(),
                    0, 0, lastAttempt, 0, "network");
        }
        if (snapshot != null && !snapshot.versions().isEmpty()) {
            return new Entry(result.etag() == null ? "" : result.etag(),
                    snapshot.versions(), snapshot.latest(), now, now,
                    result.limit(), result.remaining(), result.resetAt(),
                    0, 0, lastAttempt, 0, "network");
        }
        if (cached != null) {
            // A failed refresh keeps the old list and marks why it is not
            // moving. Never clear a working picker.
            return new Entry(cached.etag(), cached.versions(), cached.latest(),
                    cached.fetchedAt(), cached.checkedAt(),
                    result.limit() >= 0 ? result.limit() : cached.limit(),
                    result.remaining() >= 0 ? result.remaining() : cached.remaining(),
                    result.resetAt() > 0 ? result.resetAt() : cached.resetAt(),
                    failures, backoff, lastAttempt, rateLimited,
                    cached.source());
        }
        return new Entry(result.etag() == null ? "" : result.etag(),
                snapshot == null ? List.of() : snapshot.versions(),
                snapshot == null ? "" : snapshot.latest(),
                snapshot == null ? 0 : now, snapshot == null ? 0 : now,
                result.limit(), result.remaining(), result.resetAt(),
                failures, backoff, lastAttempt, rateLimited, "network");
    }

    private static long backoffUntil(int failures, long now) {
        if (failures < BACKOFF_AFTER_FAILURES) {
            return 0;
        }
        long hours = Math.min(4, 1L << Math.min(2, failures - BACKOFF_AFTER_FAILURES));
        return now + Duration.ofHours(hours).toMillis();
    }

    /// Parses a registry package document into a snapshot: the clean versions
    /// (newest first) and the `latest` dist-tag when it names one of them.
    ///
    /// Package-private so the filtering and ordering are testable without a
    /// registry round trip.
    ///
    /// @param body the registry's package document
    /// @return the snapshot; empty on any parse failure
    static Snapshot parse(String body) {
        try {
            JsonObject document = JsonParser.parseString(body).getAsJsonObject();
            List<String> clean = new ArrayList<>();
            for (String version : document.getAsJsonObject("versions").keySet()) {
                if (CLEAN_VERSION.matcher(version).matches()) {
                    clean.add(version);
                }
            }
            clean.sort(NEWEST_FIRST);
            String latest = "";
            JsonObject tags = document.has("dist-tags") ? document.getAsJsonObject("dist-tags") : null;
            if (tags != null && tags.has("latest") && tags.get("latest").isJsonPrimitive()) {
                String tagged = tags.get("latest").getAsString();
                if (clean.contains(tagged)) {
                    latest = tagged;
                }
            }
            if (latest.isEmpty() && !clean.isEmpty()) {
                latest = clean.get(0);
            }
            return new Snapshot(List.copyOf(clean), latest, System.currentTimeMillis());
        } catch (RuntimeException e) {
            return new Snapshot(List.of(), "", System.currentTimeMillis());
        }
    }

    /// One npm registry round trip.
    private static FetchResult fetchNpm(Brand brand) {
        String registry = NpmRegistry.effective().registry();
        String encoded = brand.npmPackage().replace("/", "%2f");
        HttpRequest request = HttpRequest.newBuilder(URI.create(registry + "/" + encoded))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOG.warning("Registry answered " + response.statusCode() + " for " + brand.npmPackage());
                return failed();
            }
            return new FetchResult(parse(response.body()), null, -1, -1, 0,
                    false, false, false, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failed();
        } catch (Exception e) {
            OutboundTls.Trouble trouble = recordIfTrustFailure(brand, URI.create(registry).getHost(), e);
            LOG.warning("Could not fetch versions of " + brand.npmPackage() + " from " + registry, e);
            return failedTrouble(trouble);
        }
    }

    /// One GitHub REST call for a GitHub brand: `/repos/<slug>/tags`.
    ///
    /// The ETag from the previous answer is sent back so a healthy list is a
    /// 304 (which GitHub does not charge to the quota). A token, when
    /// configured, raises the limit from 60 to 5000 per hour.
    ///
    /// 403/429 is not retried: retrying a rate limit only spends the route.
    /// 5xx and network errors get one retry — on this deployment a single
    /// failed fetch is routine.
    private static FetchResult fetchGitHub(Brand brand, @Nullable Entry cached) {
        String url = "https://api.github.com/repos/" + brand.githubSlug() + "/tags?per_page=100";
        for (int attempt = 1; attempt <= 2; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .GET();
            @Nullable String token = GitHubToken.get();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            if (cached != null && cached.etag() != null && !cached.etag().isBlank()) {
                builder.header("If-None-Match", cached.etag());
            }
            try {
                HttpResponse<String> response = client().send(builder.build(),
                        HttpResponse.BodyHandlers.ofString());
                Rate rate = rate(response.headers());
                int status = response.statusCode();
                if (status == 304) {
                    return new FetchResult(null, header(response.headers(), "ETag"),
                            rate.limit(), rate.remaining(), rate.resetAt(),
                            true, false, false, null);
                }
                if (status / 100 == 2) {
                    return new FetchResult(parseGitHubTags(response.body(), brand.tagPrefix(),
                                    brand::isReleaseVersion),
                            header(response.headers(), "ETag"),
                            rate.limit(), rate.remaining(), rate.resetAt(),
                            false, false, false, null);
                }
                if (status == 403 || status == 429) {
                    // A 403 with quota left is usually a permission problem,
                    // not a rate limit; only the latter should pause the brand.
                    boolean limited = status == 429 || rate.remaining() <= 0;
                    if (!limited) {
                        LOG.warning("GitHub refused " + url + " (HTTP " + status + ")");
                        return failed(rate);
                    }
                    LOG.warning("GitHub rate limit for " + url + " until "
                            + Instant.ofEpochSecond(rate.resetAt()));
                    return new FetchResult(null, null, rate.limit(), rate.remaining(), rate.resetAt(),
                            false, true, true, null);
                }
                LOG.warning("GitHub answered " + status + " for " + url);
                if (status < 500) {
                    return failed(rate);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return failed();
            } catch (Exception e) {
                OutboundTls.Trouble trouble = recordIfTrustFailure(brand, "api.github.com", e);
                LOG.warning("Could not fetch tags of " + brand.repoUrl()
                        + " (attempt " + attempt + " of 2)", e);
                if (attempt == 2) {
                    return new FetchResult(null, null, -1, -1, 0, false, false, true, trouble);
                }
            }
        }
        return failed();
    }

    private record Rate(int limit, int remaining, long resetAt) {
    }

    private static Rate rate(HttpHeaders headers) {
        int limit = -1;
        int remaining = -1;
        long reset = 0;
        try {
            String value = header(headers, "X-RateLimit-Limit");
            if (value != null) {
                limit = Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException ignored) {
            // A malformed header is not worth failing a fetch over.
        }
        try {
            String value = header(headers, "X-RateLimit-Remaining");
            if (value != null) {
                remaining = Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            String value = header(headers, "X-RateLimit-Reset");
            if (value != null) {
                reset = Long.parseLong(value.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return new Rate(limit, remaining, reset);
    }

    /// The first value of a header, case-insensitively.
    static @Nullable String header(HttpHeaders headers, String name) {
        return headers.firstValue(name).orElse(null);
    }

    private static FetchResult failed() {
        return new FetchResult(null, null, -1, -1, 0, false, false, true, null);
    }

    private static FetchResult failed(Rate rate) {
        return new FetchResult(null, null, rate.limit(), rate.remaining(), rate.resetAt(),
                false, false, true, null);
    }

    private static FetchResult failedTrouble(@Nullable OutboundTls.Trouble trouble) {
        return new FetchResult(null, null, -1, -1, 0, false, false, true, trouble);
    }

    /// Keeps the certificate failures a person can act on.
    private static @Nullable OutboundTls.Trouble recordIfTrustFailure(Brand brand, String host, Throwable error) {
        if (OutboundTls.isTrustFailure(error)) {
            OutboundTls.Trouble trouble = new OutboundTls.Trouble(host,
                    String.valueOf(error.getMessage()), Instant.now());
            TROUBLE.put(brand, trouble);
            return trouble;
        }
        return null;
    }

    /// A client per fetch, not a cached one: `HttpClient` captures the JVM's
    /// default `SSLContext` when it is built, and an acknowledgement given
    /// later (see [org.jackhuang.hmcl.util.platform.OutboundTls]) has to reach
    /// the very next request or the picker stays empty until a restart.
    private static HttpClient client() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // --------------------------------------------------------------- disk cache --

    /// Loads `cache/brand-catalog.json` once, tolerating a missing or corrupt
    /// file: the network is the fallback, not a failed startup.
    private static void loadDisk() {
        Path file = cacheFile;
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject document = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : document.entrySet()) {
                Brand brand = Brand.fromId(entry.getKey());
                if (brand == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject value = entry.getValue().getAsJsonObject();
                List<String> versions = new ArrayList<>();
                if (value.has("versions") && value.getAsJsonArray("versions") != null) {
                    for (JsonElement version : value.getAsJsonArray("versions")) {
                        if (version.isJsonPrimitive()) {
                            versions.add(version.getAsString());
                        }
                    }
                }
                Entry cached = new Entry(
                        string(value, "etag", ""),
                        List.copyOf(versions),
                        string(value, "latest", ""),
                        number(value, "fetchedAt", 0),
                        number(value, "checkedAt", 0),
                        (int) number(value, "limit", -1),
                        (int) number(value, "remaining", -1),
                        number(value, "resetAt", 0),
                        (int) number(value, "failures", 0),
                        number(value, "backoffUntil", 0),
                        number(value, "lastAttemptAt", 0),
                        number(value, "rateLimitedUntil", 0),
                        "disk");
                CACHE.putIfAbsent(brand, cached);
            }
        } catch (Exception e) {
            LOG.warning("Could not read the brand catalog cache " + file, e);
        }
    }

    /// Writes the cache atomically. Public data, so no special mode; a partial
    /// file would only mean the next start refetches.
    private static synchronized void saveDisk() {
        Path file = cacheFile;
        if (file == null) {
            return;
        }
        JsonObject document = new JsonObject();
        for (Map.Entry<Brand, Entry> entry : CACHE.entrySet()) {
            Entry value = entry.getValue();
            JsonObject item = new JsonObject();
            item.addProperty("etag", value.etag());
            JsonArray versions = new JsonArray();
            value.versions().forEach(versions::add);
            item.add("versions", versions);
            item.addProperty("latest", value.latest());
            item.addProperty("fetchedAt", value.fetchedAt());
            item.addProperty("checkedAt", value.checkedAt());
            item.addProperty("limit", value.limit());
            item.addProperty("remaining", value.remaining());
            item.addProperty("resetAt", value.resetAt());
            item.addProperty("failures", value.failures());
            item.addProperty("backoffUntil", value.backoffUntil());
            item.addProperty("lastAttemptAt", value.lastAttemptAt());
            item.addProperty("rateLimitedUntil", value.rateLimitedUntil());
            document.add(entry.getKey().id(), item);
        }
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, document.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOG.warning("Could not write the brand catalog cache " + file, e);
        }
    }

    private static String string(JsonObject object, String name, String fallback) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                ? object.get(name).getAsString() : fallback;
    }

    private static long number(JsonObject object, String name, long fallback) {
        try {
            return object.has(name) ? object.get(name).getAsLong() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    // --------------------------------------------------------------- parsing --

    /// Parses the `/repos/<slug>/tags` document into a snapshot: each entry's
    /// `name`, the brand's tag prefix stripped, only clean `x.y.z` kept,
    /// deduplicated and sorted newest first; `latest` is the newest.
    ///
    /// Package-private so the filtering and ordering are testable without a
    /// network round trip.
    ///
    /// @param body the API's JSON array
    /// @return the snapshot; empty on any parse failure or when no clean tag exists
    static Snapshot parseGitHubTags(String body) {
        return parseGitHubTags(body, "v");
    }

    /// [parseGitHubTags(String)] with an explicit tag prefix.
    static Snapshot parseGitHubTags(String body, String tagPrefix) {
        return parseGitHubTags(body, tagPrefix, version -> CLEAN_VERSION.matcher(version).matches());
    }

    /// The same, with a brand-specific release predicate: Octop publishes beta
    /// tags (`1.0.3b1`) that the strict semantic-version filter would hide.
    static Snapshot parseGitHubTags(String body, String tagPrefix,
                                    java.util.function.Predicate<String> accepted) {
        try {
            JsonArray array = JsonParser.parseString(body).getAsJsonArray();
            java.util.Set<String> seen = new java.util.HashSet<>();
            List<String> clean = new ArrayList<>();
            for (JsonElement element : array) {
                if (!element.isJsonObject() || !element.getAsJsonObject().has("name")) {
                    continue;
                }
                String name = element.getAsJsonObject().get("name").getAsString();
                String version = tagPrefix.isEmpty() || !name.startsWith(tagPrefix)
                        ? name
                        : name.substring(tagPrefix.length());
                if (accepted.test(version) && seen.add(version)) {
                    clean.add(version);
                }
            }
            clean.sort(NEWEST_FIRST);
            return new Snapshot(List.copyOf(clean), clean.isEmpty() ? "" : clean.get(0),
                    System.currentTimeMillis());
        } catch (RuntimeException e) {
            return new Snapshot(List.of(), "", System.currentTimeMillis());
        }
    }

    /// Numeric component-wise ordering, newest first.
    private static final Comparator<String> NEWEST_FIRST = (left, right) -> {
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

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
