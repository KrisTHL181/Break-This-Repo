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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.emergency.EmergencyStop;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jackhuang.hmcl.web.terminal.TerminalTickets;
import org.jackhuang.hmcl.web.terminal.TerminalWebSocket;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/// `/api/mc/*` — the Minecraft surface's HTTP contract.
///
/// The feature switch, the instances and the lifecycle live behind
/// [McFeatureFilter]; this servlet adds the operations: the enable flow (which
/// records the acceptance, never invents it), the stop/keep/close decisions,
/// instance CRUD, start/stop/kill, logs, `server.properties` and the console
/// ticket.
///
/// Everything that starts a process, deletes data or changes the feature needs
/// the developer elevation — the same second password the web shell uses. The
/// one exception is "start an instance": it is a daily, reversible action
/// (`docs/minecraft.md` §5.4).
@NotNullByDefault
public final class McApiServlet extends HttpServlet {

    private static final Logger LOG = Logger.LOG;

    /// The official EULA. Fetched at runtime and never bundled: the text is
    /// Mojang's, and the panel's job is to show it, not to distribute it.
    static final URI EULA_URI = URI.create("https://www.minecraft.net/en-us/eula");
    private static final Duration EULA_MAX_AGE = Duration.ofHours(24);
    private static final Set<String> MANAGED_PROPERTIES = Set.of(
            "motd", "max-players", "online-mode", "white-list", "enforce-whitelist",
            "difficulty", "gamemode", "pvp", "allow-flight", "spawn-protection",
            "view-distance", "simulation-distance", "enable-rcon");

    private final ServerConfig config;
    private final McFeatureState feature;
    private final McInstanceManager manager;
    private final McRuntime runtime;
    private final McVersions versions;
    private final McCores cores;
    private final McJavaRuntimes javaRuntimes;
    private final McMods mods = new McMods();
    private final McBackups backups;
    private final TaskService tasks;
    private final TerminalTickets tickets;
    private final AuthService auth;
    private final DeveloperAuth developerAuth;
    private final DeveloperElevation elevation;
    private final BooleanSupplier tlsActive;
    private final EmergencyStop emergency;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /// The last fetched EULA, with the hash the enable flow has to echo back.
    private volatile @Nullable CachedEula eula;

    private record CachedEula(String text, String hash, String fetchedAt, Instant at) {
    }

    /// The on-disk copy of that text, so a machine that cannot reach Mojang today can
    /// still show what it showed yesterday.
    ///
    /// This matters more than it looks: the EULA is only needed at the moment the
    /// feature is switched on, and an operator who turned the feature off (or lost it
    /// with a reinstall) would otherwise be stuck behind a network they cannot fix
    /// from the panel — the wizard has nothing to show, so there is nothing to
    /// accept, and the feature cannot be enabled at all. The text is Mojang's public
    /// document, the hash in the consent record is over exactly the text that was
    /// shown, and [CachedEula#fetchedAt] travels with it so the page can say how old
    /// the copy is.
    private record StoredEula(String text, String hash, String fetchedAt) {
    }

    public McApiServlet(ServerConfig config, McFeatureState feature, McInstanceManager manager,
                        McRuntime runtime, McVersions versions, McCores cores,
                        McJavaRuntimes javaRuntimes,
                        McBackups backups, TaskService tasks,
                        TerminalTickets tickets, AuthService auth,
                        DeveloperAuth developerAuth, DeveloperElevation elevation,
                        BooleanSupplier tlsActive, EmergencyStop emergency) {
        this.config = config;
        this.feature = feature;
        this.manager = manager;
        this.runtime = runtime;
        this.versions = versions;
        this.cores = cores;
        this.javaRuntimes = javaRuntimes;
        this.backups = backups;
        this.tasks = tasks;
        this.tickets = tickets;
        this.auth = auth;
        this.developerAuth = developerAuth;
        this.elevation = elevation;
        this.tlsActive = tlsActive;
        this.emergency = emergency;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        String method = request.getMethod();
        if (emergency.isEngaged() && !"GET".equals(method)) {
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "emergency_stop: the panel is in emergency stop; Minecraft actions are frozen");
            return;
        }
        try {
            if ("/feature".equals(path) && "GET".equals(method)) {
                featureJson(response);
                return;
            }
            if ("/eula".equals(path) && "GET".equals(method)) {
                eula(request, response);
                return;
            }
            if ("/terms".equals(path) && "GET".equals(method)) {
                terms(response);
                return;
            }
            if ("/feature/enable".equals(path) && "POST".equals(method)) {
                enable(request, response);
                return;
            }
            if ("/feature/stop".equals(path) && "POST".equals(method)) {
                stopFeature(request, response);
                return;
            }
            if ("/feature/confirm-close".equals(path) && "POST".equals(method)) {
                confirmClose(request, response);
                return;
            }
            if ("/feature/cancel-close".equals(path) && "POST".equals(method)) {
                cancelClose(request, response);
                return;
            }
            if ("/feature/decline".equals(path) && "POST".equals(method)) {
                audit("mc.feature.declined", "MC feature", "success", request);
                Json.write(response, Map.of("ok", true));
                return;
            }
            if ("/instances".equals(path)) {
                if ("GET".equals(method)) {
                    listInstances(request, response);
                    return;
                }
                if ("POST".equals(method)) {
                    createInstance(request, response);
                    return;
                }
            }
            if ("/cores".equals(path) && "GET".equals(method)) {
                listCores(request, response);
                return;
            }
            if ("/versions".equals(path) && "GET".equals(method)) {
                listVersions(response);
                return;
            }
            if ("/third-party/feature".equals(path) && "GET".equals(method)) {
                thirdPartyJson(response);
                return;
            }
            if ("/third-party/terms".equals(path) && "GET".equals(method)) {
                thirdPartyTerms(response);
                return;
            }
            if ("/third-party/enable".equals(path) && "POST".equals(method)) {
                enableThirdParty(request, response);
                return;
            }
            if ("/third-party/disable".equals(path) && "POST".equals(method)) {
                disableThirdParty(request, response);
                return;
            }
            if ("/third-party/decline".equals(path) && "POST".equals(method)) {
                audit("mc.thirdparty.declined", "third-party cores", "success", request);
                Json.write(response, Map.of("ok", true));
                return;
            }
            // 下载源设置：顺序、自定义基址、配额余量。只在第三方核心开启后存在，
            // 和其余第三方端点同一个口径。
            if ("/third-party/sources".equals(path) && "GET".equals(method)) {
                sourcesJson(response);
                return;
            }
            if ("/third-party/sources".equals(path) && "POST".equals(method)) {
                updateSources(request, response);
                return;
            }
            if ("/java/runtimes".equals(path) && "GET".equals(method)) {
                javaRuntimes(request, response);
                return;
            }
            if ("/java/install".equals(path) && "POST".equals(method)) {
                installJava(request, response);
                return;
            }
            if (path.startsWith("/java/") && path.endsWith("/remove") && "POST".equals(method)) {
                removeJava(request, response, path);
                return;
            }
            if ("/storage".equals(path) && "GET".equals(method)) {
                storageInfo(response);
                return;
            }
            if ("/ports".equals(path) && "GET".equals(method)) {
                McPorts.Pair pair = McPorts.allocate(
                        port -> manager.portTaken(port) || !McPorts.isFree(port),
                        config.mc.portStart, config.mc.portEnd);
                Json.write(response, Map.of("port", pair.game(), "rconPort", pair.rcon()));
                return;
            }
            if ("/modrinth/search".equals(path) && "GET".equals(method)) {
                modrinthSearch(request, response);
                return;
            }
            if ("/modrinth/install".equals(path) && "POST".equals(method)) {
                modrinthInstall(request, response);
                return;
            }
            if (path.startsWith("/instances/")) {
                routeInstance(request, response, method, path.substring("/instances/".length()));
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        } catch (McException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ feature --

    private void featureJson(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("state", feature.state().name());
        body.addProperty("enabled", feature.enabled());
        body.addProperty("locked", feature.locked());
        body.addProperty("lockRemainingSeconds", feature.lockRemainingSeconds());
        Boolean keep = feature.keepFeatureChoice();
        if (keep == null) {
            body.add("keepFeature", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("keepFeature", keep);
        }
        feature.consent().ifPresent(record -> {
            body.addProperty("termsVersion", record.termsVersion());
            body.addProperty("termsAcceptedAt", record.termsAcceptedAt());
            body.addProperty("acceptedBy", record.acceptedBy());
            // 同意记录回看要能对上「当时看的是哪一版文本」，所以哈希与确认方式一起给。
            // `acceptedFrom`（来源 IP）故意不给：那是审计字段，不是回看要展示的内容。
            body.addProperty("termsTextHash", record.termsTextHash());
            body.addProperty("eulaHash", record.eulaHash());
            body.addProperty("eulaRetrievedAt", record.eulaRetrievedAt());
            body.addProperty("ackMethod", record.ackMethod());
        });
        // Even while the feature is disabled these two are readable: the card
        // has to be able to say "this is where the servers will live and this is
        // the Java they will use" before the user commits to enabling it.
        body.addProperty("console", McRuntime.consoleKind());
        body.addProperty("thirdParty", feature.thirdPartyEnabled());
        body.add("java", javaJson());
        body.add("storage", storageJson());
        // The advanced panel draws the input's bounds and the recommendation from
        // here; a hardcoded 8192 in the SPA would drift from server.yaml.
        JsonObject limits = new JsonObject();
        limits.addProperty("minMemoryMb", McInstance.MIN_MEMORY_MB);
        limits.addProperty("maxMemoryMb", maxMemoryMb());
        limits.addProperty("maxInstances", config.mc.maxInstances);
        body.add("limits", limits);
        Json.write(response, body);
    }

    private JsonObject javaJson() {
        McRuntime.JavaInfo info = runtime.javaInfo();
        JsonObject java = new JsonObject();
        java.addProperty("path", info.path());
        if (info.version() == null) {
            java.add("version", com.google.gson.JsonNull.INSTANCE);
        } else {
            java.addProperty("version", info.version());
        }
        java.addProperty("major", info.major());
        java.addProperty("source", info.source());
        java.addProperty("ok", info.ok());
        if (info.error() != null) {
            java.addProperty("error", info.error());
        }
        // 面板自托管的 JRE 也在这里给：卡片要能在此之前就说清「面板自己装了哪些」。
        JsonArray managed = new JsonArray();
        for (McJavaRuntimes.Installed runtime : javaRuntimes.listInstalled()) {
            JsonObject item = new JsonObject();
            item.addProperty("feature", runtime.feature());
            item.addProperty("path", runtime.path().toString());
            managed.add(item);
        }
        java.add("managed", managed);
        return java;
    }

    private JsonObject storageJson() {
        JsonObject storage = new JsonObject();
        storage.addProperty("root", manager.root().toString());
        storage.addProperty("source", storageSource());
        boolean writable;
        try {
            writable = java.nio.file.Files.isDirectory(manager.root())
                    && java.nio.file.Files.isWritable(manager.root());
        } catch (RuntimeException e) {
            writable = false;
        }
        storage.addProperty("writable", writable);
        if (manager.legacyRoot() == null) {
            storage.add("legacy", com.google.gson.JsonNull.INSTANCE);
        } else {
            storage.addProperty("legacy", manager.legacyRoot().toString());
        }
        return storage;
    }

    private String storageSource() {
        String env = System.getenv("HDSL_MC_DIR");
        if (env != null && !env.isBlank()) {
            return "HDSL_MC_DIR";
        }
        if (config.mc.dir != null && !config.mc.dir.isBlank()) {
            return "mc.dir";
        }
        return "$HOME/mc";
    }

    private void terms(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("version", McTerms.VERSION);
        body.addProperty("text", McTerms.text());
        body.addProperty("hash", McTerms.hash());
        Json.write(response, body);
    }

    private void eula(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CachedEula cached = eula;
        if (cached == null || Instant.now().isAfter(cached.at().plus(EULA_MAX_AGE))) {
            cached = fetchEula();
            if (cached == null) {
                // No Mojang today. What was shown before is still the document the user
                // has to read, and refusing to show it locks the feature off entirely.
                cached = rememberedEula();
                if (cached != null) {
                    LOG.warning("Could not reach Mojang for the EULA; showing the copy fetched at "
                            + cached.fetchedAt() + " (" + eulaCacheFile() + ")");
                }
            } else {
                rememberEula(cached);
            }
            if (cached == null) {
                Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "eula_unavailable: could not reach Mojang to fetch the EULA — check the network/proxy and retry "
                        + "(the server download would fail the same way)");
                return;
            }
            eula = cached;
        }
        JsonObject body = new JsonObject();
        body.addProperty("text", cached.text());
        body.addProperty("hash", cached.hash());
        body.addProperty("fetchedAt", cached.fetchedAt());
        body.addProperty("source", EULA_URI.toString());
        Json.write(response, body);
    }

    /// Where the last successfully fetched EULA is kept.
    ///
    /// @return `<data>/minecraft/eula.json`
    private Path eulaCacheFile() {
        return config.dataDir.resolve("minecraft").resolve("eula.json");
    }

    private void rememberEula(CachedEula cached) {
        Path file = eulaCacheFile();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonUtils.writeToJsonFile(file, new StoredEula(cached.text(), cached.hash(), cached.fetchedAt()));
        } catch (IOException | RuntimeException e) {
            // A cache that cannot be written is a warning, not a failed request: the
            // fetch itself succeeded, which is what the caller asked for.
            LOG.warning("Could not keep the EULA copy in " + file, e);
        }
    }

    private @Nullable CachedEula rememberedEula() {
        Path file = eulaCacheFile();
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            StoredEula stored = JsonUtils.fromJsonFile(file, StoredEula.class);
            if (stored == null || stored.text() == null || stored.hash() == null) {
                return null;
            }
            return new CachedEula(stored.text(), stored.hash(),
                    stored.fetchedAt() == null ? "unknown" : stored.fetchedAt(), Instant.EPOCH);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the EULA copy in " + file, e);
            return null;
        }
    }

    private @Nullable CachedEula fetchEula() {
        try {
            HttpRequest request = HttpRequest.newBuilder(EULA_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "HDSL-web/" + Metadata.VERSION)
                    .GET()
                    .build();
            HttpResponse<byte[]> answer = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (answer.statusCode() != 200) {
                return null;
            }
            byte[] bytes = answer.body();
            String text = McHtml.legalText(new String(bytes, StandardCharsets.UTF_8));
            if (text.isBlank()) {
                return null;
            }
            // The hash is over the text that is shown, so an enable records
            // exactly the document the user read — not the page's chrome.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = "sha256:" + HexFormat.of().formatHex(
                    digest.digest(text.getBytes(StandardCharsets.UTF_8)));
            return new CachedEula(text, hash, Instant.now().toString(), Instant.now());
        } catch (Exception e) {
            return null;
        }
    }

    private void enable(HttpServletRequest request, HttpServletResponse response)
            throws IOException, McException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        boolean eulaAcknowledged = booleanField(body, "eulaAcknowledged");
        boolean termsAcknowledged = booleanField(body, "termsAcknowledged");
        boolean sawDecline = booleanField(body, "sawDeclineOption");
        String eulaHash = stringField(body, "eulaHash");
        String termsTextHash = stringField(body, "termsTextHash");
        String uiVersion = stringField(body, "uiVersion");
        int termsVersion = intField(body, "termsVersion");

        if (!eulaAcknowledged || !termsAcknowledged) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "both the EULA and the terms must be acknowledged");
            return;
        }
        if (!sawDecline) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "the decline option must have been visible (sawDeclineOption)");
            return;
        }
        if (eulaHash == null || termsTextHash == null || uiVersion == null || termsVersion < 1) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "eulaHash, termsTextHash, termsVersion and uiVersion are required");
            return;
        }
        CachedEula cached = eula;
        if (cached == null) {
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "eula_not_fetched: fetch the EULA first; the panel will not accept a hash it cannot verify");
            return;
        }
        if (!cached.hash().equals(eulaHash)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the EULA changed since it was shown; show it again");
            return;
        }
        if (termsVersion != McTerms.VERSION) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the terms changed to v" + McTerms.VERSION + "; show them again");
            return;
        }
        if (!McTerms.hash().equals(termsTextHash)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the terms text does not match what this build offers");
            return;
        }

        String now = Instant.now().toString();
        McConsent record = new McConsent(termsVersion, termsTextHash, eulaHash,
                cached.fetchedAt(), now, now,
                actorOf(request) == null ? "unknown" : actorOf(request),
                request.getRemoteAddr(),
                "scroll+checkbox", true, uiVersion,
                request.getHeader("User-Agent"), Metadata.VERSION, null);
        try {
            feature.enable(record);
        } catch (McFeatureState.LockedException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            Json.write(response, Map.of("error", "mc_locked", "remainingSeconds", e.remainingSeconds()));
            return;
        }
        // One acceptance covers every instance: the EULA is between the operator
        // and Mojang, not per world.
        for (McInstance instance : manager.list()) {
            manager.acceptEula(instance);
        }
        audit("mc.feature.enable", "terms v" + termsVersion + " (" + eulaHash + ")", "success", request);
        featureJson(response);
    }

    private void stopFeature(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        // The second gate hangs off this one; closing the surface while it is on
        // would leave a sub-feature enabled under a disabled feature.
        if (feature.thirdPartyEnabled()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "disable third-party cores before stopping the Minecraft feature");
            return;
        }
        List<String> ids = new ArrayList<>();
        for (McInstance instance : manager.list()) {
            if (runtime.isRunning(instance.id())) {
                ids.add(instance.id());
                try {
                    runtime.stop(instance.id());
                } catch (McException ignored) {
                    // It is not running any more; nothing to stop.
                }
            }
        }
        boolean started = feature.stopFeature();
        // `stop` is a request to the server, not a guarantee. Anything still
        // alive after the grace period is killed, so "all servers are down"
        // becomes true shortly after the press instead of "whenever each
        // server felt like exiting".
        if (!ids.isEmpty()) {
            org.jackhuang.hmcl.util.platform.VirtualThreads.start(() -> {
                try {
                    Thread.sleep(McRuntime.STOP_GRACE_SECONDS * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (String id : ids) {
                    if (runtime.isRunning(id)) {
                        try {
                            runtime.kill(id);
                        } catch (McException ignored) {
                            // Gone between the check and the kill.
                        }
                    }
                }
            });
        }
        audit("mc.feature.stop", "instances=" + ids.size(), started ? "started" : "already-locked", request);
        featureJson(response);
    }

    private void confirmClose(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        try {
            feature.confirmClose();
        } catch (IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        audit("mc.feature.confirm-close", "MC feature", "success", request);
        featureJson(response);
    }

    private void cancelClose(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        try {
            feature.decideKeep();
        } catch (IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        audit("mc.feature.keep", "MC feature", "success", request);
        featureJson(response);
    }

    // ---------------------------------------------------------------- instances --

    private void listInstances(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String scope = request.getParameter("scope");
        List<JsonObject> items = new ArrayList<>();
        for (McInstance instance : manager.list()) {
            if ("vanilla".equals(scope) && McCores.isThirdParty(instance.core())) {
                continue;
            }
            if ("thirdparty".equals(scope) && !McCores.isThirdParty(instance.core())) {
                continue;
            }
            items.add(instanceJson(instance));
        }
        Json.write(response, Map.of("instances", items));
    }

    private void listVersions(HttpServletResponse response) throws IOException, McException {
        List<Map<String, String>> items = new ArrayList<>();
        for (McVersions.VersionEntry entry : versions.list()) {
            items.add(Map.of("id", entry.id(), "type", entry.type()));
        }
        Json.write(response, Map.of("versions", items));
    }

    // --------------------------------------------------------------------- cores --

    /// The catalogue the create form shows: vanilla always, third-party cores
    /// only while the second gate is on.
    private void listCores(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<JsonObject> items = new ArrayList<>();
        boolean refresh = "1".equals(request.getParameter("refresh"));
        for (McCores.CoreInfo core : cores.catalog(feature.thirdPartyEnabled(), refresh)) {
            JsonObject item = new JsonObject();
            item.addProperty("id", core.id());
            item.addProperty("label", core.label());
            item.addProperty("thirdParty", McCores.isThirdParty(core.id()));
            JsonArray versions = new JsonArray();
            core.versions().forEach(versions::add);
            item.add("versions", versions);
            items.add(item);
        }
        Json.write(response, Map.of("cores", items));
    }

    // --------------------------------------------------------------- third-party --

    private void thirdPartyJson(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("state", feature.thirdPartyState().name());
        body.addProperty("enabled", feature.thirdPartyEnabled());
        body.addProperty("termsVersion", McThirdPartyTerms.VERSION);
        feature.thirdPartyConsent().ifPresent(record -> {
            body.addProperty("termsTextHash", record.termsTextHash());
            body.addProperty("acceptedAt", record.acceptedAt());
            body.addProperty("acceptedBy", record.acceptedBy());
            body.addProperty("ackMethod", record.ackMethod());
        });
        Json.write(response, body);
    }

    private void thirdPartyTerms(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("version", McThirdPartyTerms.VERSION);
        body.addProperty("text", McThirdPartyTerms.text());
        body.addProperty("hash", McThirdPartyTerms.hash());
        Json.write(response, body);
    }

    /// Where third-party cores are fetched from, and what each source has left.
    private void sourcesJson(HttpServletResponse response) throws IOException {
        if (!feature.thirdPartyEnabled()) {
            // Same rule as every other third-party route while the gate is off:
            // the surface does not exist (404, not 403).
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        McSources sources = cores.sources();
        if (sources == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        JsonArray list = new JsonArray();
        for (McSources.Source source : McSources.known()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", source.id());
            item.addProperty("label", source.label());
            item.addProperty("note", source.note());
            item.addProperty("attribution", source.attribution());
            JsonArray supported = new JsonArray();
            source.cores().forEach(supported::add);
            item.add("cores", supported);
            McSources.Quota quota = sources.quota(source.id());
            JsonObject quotaJson = new JsonObject();
            quotaJson.addProperty("limit", quota.limit());
            quotaJson.addProperty("remaining", quota.remaining());
            quotaJson.addProperty("resetAt", quota.resetAt());
            quotaJson.addProperty("used", quota.used());
            quotaJson.addProperty("exhausted", sources.exhausted(source.id()));
            item.add("quota", quotaJson);
            list.add(item);
        }
        JsonObject order = new JsonObject();
        for (Map.Entry<String, List<String>> entry : sources.order().entrySet()) {
            JsonArray ids = new JsonArray();
            entry.getValue().forEach(ids::add);
            order.add(entry.getKey(), ids);
        }
        JsonObject custom = new JsonObject();
        sources.customBases().forEach(custom::addProperty);
        JsonObject body = new JsonObject();
        body.add("sources", list);
        body.add("order", order);
        body.add("custom", custom);
        body.addProperty("userAgent", McSources.USER_AGENT);
        Json.write(response, body);
    }

    /// Rewrites the order and/or the custom bases. Every id is validated
    /// against [McSources#supports]; unknown entries are dropped rather than
    /// stored, so a typo cannot silently disable a core.
    private void updateSources(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!feature.thirdPartyEnabled()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        if (!requireElevation(request, response)) {
            return;
        }
        McSources sources = cores.sources();
        if (sources == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        if (body.has("order") && body.get("order").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry
                    : body.getAsJsonObject("order").entrySet()) {
                if (!entry.getValue().isJsonArray()) {
                    continue;
                }
                List<String> ids = new ArrayList<>();
                for (com.google.gson.JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        ids.add(element.getAsString());
                    }
                }
                sources.setOrder(entry.getKey().toLowerCase(java.util.Locale.ROOT), ids);
            }
        }
        if (body.has("custom") && body.get("custom").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry
                    : body.getAsJsonObject("custom").entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                sources.setCustomBase(entry.getKey(), entry.getValue().getAsString());
            }
        }
        audit("mc.thirdparty.sources", "download sources", "success", request);
        sourcesJson(response);
    }

    /// Enables the second gate: 提权 → 条款逐字确认（回显 hash）→ 审计.
    ///
    /// The EULA belongs to Mojang and is not re-accepted here; what this gate
    /// asks about is other people's software and the panel's lack of warranty.
    private void enableThirdParty(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String termsTextHash = stringField(body, "termsTextHash");
        String uiVersion = stringField(body, "uiVersion");
        int termsVersion = intField(body, "termsVersion");
        if (termsTextHash == null || uiVersion == null || termsVersion < 1) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "termsTextHash, termsVersion and uiVersion are required");
            return;
        }
        if (termsVersion != McThirdPartyTerms.VERSION) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the third-party terms changed to v" + McThirdPartyTerms.VERSION + "; show them again");
            return;
        }
        if (!McThirdPartyTerms.hash().equals(termsTextHash)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the third-party terms text does not match what this build offers");
            return;
        }
        String now = Instant.now().toString();
        McThirdPartyConsent record = new McThirdPartyConsent(termsVersion, termsTextHash, now,
                actorOf(request) == null ? "unknown" : actorOf(request),
                request.getRemoteAddr(), "checkbox", uiVersion,
                request.getHeader("User-Agent"), Metadata.VERSION);
        try {
            feature.enableThirdParty(record);
        } catch (McFeatureState.LockedException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            Json.write(response, Map.of("error", "mc_locked", "remainingSeconds", e.remainingSeconds()));
            return;
        } catch (IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        audit("mc.thirdparty.enable", "terms v" + termsVersion, "success", request);
        thirdPartyJson(response);
    }

    /// Closes the second gate. Running third-party servers have to be stopped
    /// first: the gate is about what may run, and flipping it under a live
    /// process would leave the panel supervising something it just disowned.
    private void disableThirdParty(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        for (McInstance instance : manager.list()) {
            if (McCores.isThirdParty(instance.core()) && runtime.isRunning(instance.id())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "stop " + instance.id() + " before disabling third-party cores");
                return;
            }
        }
        feature.disableThirdParty();
        audit("mc.thirdparty.disable", "third-party cores", "success", request);
        thirdPartyJson(response);
    }

    // --------------------------------------------------------------------- java --
    /// The Java runtimes the panel owns, plus what Adoptium offers for this host.
    ///
    /// A failing API is reported as `source: "unavailable"` rather than an error:
    /// the installed list is still exactly what a launch would use, and a card
    /// that shows it is more useful than a blank one.
    private void javaRuntimes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray installed = new JsonArray();
        for (McJavaRuntimes.Installed runtime : javaRuntimes.listInstalled()) {
            JsonObject item = new JsonObject();
            item.addProperty("feature", runtime.feature());
            item.addProperty("path", runtime.path().toString());
            item.addProperty("vendor", runtime.vendor() == null
                    ? McJavaRuntimes.Vendor.TEMURIN.id() : runtime.vendor().id());
            item.addProperty("vendorLabel", runtime.vendor() == null
                    ? McJavaRuntimes.Vendor.TEMURIN.label() : runtime.vendor().label());
            if (runtime.version() == null) {
                item.add("version", com.google.gson.JsonNull.INSTANCE);
            } else {
                item.addProperty("version", runtime.version());
            }
            item.addProperty("bytes", runtime.bytes());
            installed.add(item);
        }
        body.add("installed", installed);
        body.addProperty("root", javaRuntimes.root().toString());
        String version = request.getParameter("version");
        body.addProperty("recommended", version == null || version.isBlank()
                ? 0 : McJavaRuntimes.recommendFor(version));
        // Two Javas the panel never touches: its own JVM and whatever `java`
        // resolves to on PATH. They are here so an operator can compare, not so
        // the page can point anything at them.
        JsonObject panel = new JsonObject();
        panel.addProperty("version", System.getProperty("java.version"));
        panel.addProperty("vendor", System.getProperty("java.vm.name"));
        panel.addProperty("home", System.getProperty("java.home"));
        body.add("panel", panel);
        McRuntime.JavaInfo pathJava = McRuntime.pathJavaInfo();
        JsonObject system = new JsonObject();
        system.addProperty("path", pathJava.path());
        if (pathJava.version() == null) {
            system.add("version", com.google.gson.JsonNull.INSTANCE);
        } else {
            system.addProperty("version", pathJava.version());
        }
        system.addProperty("major", pathJava.major());
        system.addProperty("ok", pathJava.ok());
        if (pathJava.error() != null) {
            system.addProperty("error", pathJava.error());
        }
        body.add("system", system);
        // Which instances pin a managed runtime, so the page can refuse to delete
        // the one a server is started with.
        JsonArray consumers = new JsonArray();
        java.nio.file.Path managedRoot = javaRuntimes.root().toAbsolutePath().normalize();
        for (McInstance instance : manager.list()) {
            String pinned = instance.javaPath();
            if (pinned == null) {
                continue;
            }
            java.nio.file.Path pinnedPath;
            try {
                pinnedPath = java.nio.file.Path.of(pinned).toAbsolutePath().normalize();
            } catch (RuntimeException e) {
                continue;
            }
            if (!pinnedPath.startsWith(managedRoot) || pinnedPath.equals(managedRoot)) {
                continue;
            }
            int featureRelease;
            try {
                featureRelease = Integer.parseInt(managedRoot.relativize(pinnedPath).getName(0).toString());
            } catch (RuntimeException e) {
                continue;
            }
            JsonObject item = new JsonObject();
            item.addProperty("feature", featureRelease);
            item.addProperty("id", instance.id());
            item.addProperty("path", pinned);
            item.addProperty("running", runtime.isRunning(instance.id()));
            consumers.add(item);
        }
        body.add("consumers", consumers);
        JsonArray available = new JsonArray();
        JsonArray vendors = new JsonArray();
        // The settings card only lists what is installed; the catalogue costs
        // several seconds (three vendors), so it is opt-in for the wizard.
        boolean withCatalogue = !"0".equals(request.getParameter("catalog"));
        boolean anyVendor = false;
        String firstError = null;
        for (McJavaRuntimes.Vendor vendor : withCatalogue
                ? McJavaRuntimes.Vendor.values()
                : new McJavaRuntimes.Vendor[0]) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", vendor.id());
            entry.addProperty("label", vendor.label());
            try {
                for (McJavaRuntimes.Release release : javaRuntimes.available(vendor)) {
                    JsonObject item = new JsonObject();
                    item.addProperty("vendor", vendor.id());
                    item.addProperty("vendorLabel", vendor.label());
                    item.addProperty("feature", release.feature());
                    item.addProperty("version", release.version());
                    item.addProperty("size", release.size());
                    available.add(item);
                }
                entry.addProperty("ok", true);
                anyVendor = true;
            } catch (McException e) {
                entry.addProperty("ok", false);
                entry.addProperty("error", e.getMessage());
                if (firstError == null) {
                    firstError = vendor.label() + ": " + e.getMessage();
                }
            }
            vendors.add(entry);
        }
        body.add("vendors", vendors);
        body.addProperty("source", !withCatalogue ? "skipped"
                : anyVendor ? "api" : "unavailable");
        if (withCatalogue && !anyVendor && firstError != null) {
            body.addProperty("error", firstError);
        }
        body.add("available", available);
        Json.write(response, body);
    }

    /// Downloads and unpacks one JRE as a task: tens of megabytes on a possibly
    /// slow link, and the request must not stay open for it.
    private void installJava(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        int feature = body == null ? 0 : intField(body, "feature");
        if (feature < 8 || feature > 40) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "feature must be a Java feature release between 8 and 40");
            return;
        }
        String vendorId = body == null ? null : stringField(body, "vendor");
        McJavaRuntimes.Vendor vendor = vendorId == null || vendorId.isBlank()
                ? McJavaRuntimes.Vendor.TEMURIN : McJavaRuntimes.Vendor.of(vendorId);
        if (vendor == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "unknown JDK vendor `" + vendorId + "`");
            return;
        }
        try {
            TaskService.Task task = tasks.submit("mc-java-install",
                    feature + "-" + vendor.id(), live -> {
                live.update("Downloading " + vendor.label() + " " + feature, -1);
                McJavaRuntimes.Installed installed = javaRuntimes.install(feature, vendor,
                        (copied, total) -> {
                    String progress = (copied / (1024 * 1024)) + " / "
                            + (total > 0 ? (total / (1024 * 1024)) + " MB" : "? MB");
                    live.update("Downloading " + vendor.label() + " " + feature + ": " + progress,
                            total > 0 ? (double) copied / total : -1);
                });
                return "Installed " + vendor.label() + " " + installed.feature()
                        + " (" + installed.path() + ")";
            }, null);
            audit("mc.java.install", feature + "/" + vendor.id(), "submitted", request);
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            Json.write(response, Map.of("taskId", task.id()));
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    private void removeJava(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        String[] parts = path.split("/");
        int feature;
        try {
            feature = Integer.parseInt(parts.length > 2 ? parts[2] : "");
        } catch (NumberFormatException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "feature is required in the path");
            return;
        }
        // A runtime an instance was pinned to is that instance's launcher; deleting
        // it turns "start" into a mystery failure. The pin lives in the instance's
        // advanced settings, so the fix is there, not here.
        java.nio.file.Path managed = javaRuntimes.root().toAbsolutePath().normalize()
                .resolve(Integer.toString(feature));
        for (McInstance instance : manager.list()) {
            String pinned = instance.javaPath();
            if (pinned == null) {
                continue;
            }
            try {
                if (java.nio.file.Path.of(pinned).toAbsolutePath().normalize().startsWith(managed)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "instance " + instance.id() + " is pinned to this Java; change its"
                                    + " advanced settings first");
                    return;
                }
            } catch (RuntimeException e) {
                // An unparsable path cannot be this runtime; keep looking.
            }
        }
        boolean removed = javaRuntimes.remove(feature);
        audit("mc.java.remove", Integer.toString(feature), removed ? "success" : "absent", request);
        Json.write(response, Map.of("ok", true, "removed", removed));
    }

    private void createInstance(HttpServletRequest request, HttpServletResponse response)
            throws IOException, McException {
        if (!requireElevation(request, response)) {
            return;
        }
        if (manager.list().size() >= config.mc.maxInstances) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the panel already manages " + config.mc.maxInstances + " Minecraft instance(s)");
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String name = stringField(body, "name");
        String version = stringField(body, "version");
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "name and version are required");
            return;
        }
        int memoryMb = intField(body, "memoryMb");
        if (memoryMb == 0) {
            memoryMb = 2048;
        }
        if (memoryMb < McInstance.MIN_MEMORY_MB || memoryMb > maxMemoryMb()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "memoryMb must be between " + McInstance.MIN_MEMORY_MB
                            + " and " + maxMemoryMb());
            return;
        }
        String levelName = stringField(body, "levelName");
        if (levelName == null || levelName.isBlank()) {
            levelName = "world";
        }
        String seed = stringField(body, "seed");

        int port = intField(body, "port");
        int rconPort = intField(body, "rconPort");
        if (port == 0 || rconPort == 0) {
            McPorts.Pair pair = McPorts.allocate(
                    candidate -> manager.portTaken(candidate) || !McPorts.isFree(candidate),
                    config.mc.portStart, config.mc.portEnd);
            port = pair.game();
            rconPort = pair.rcon();
        } else if (!McPorts.isFree(port)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "game port " + port + " is already in use on this machine;"
                            + " stop whatever is listening on it, or pick another port");
            return;
        }

        // 存储位置：默认放专用目录（容器里是 /home/hdsl/mc，挂了卷就跟着容器走）；
        // `data` 是升级前的旧位置，只有部署提供了它才可选。
        String storage = stringField(body, "storage");
        boolean legacy = storage != null && "data".equalsIgnoreCase(storage.trim());

        // 核心类型：默认原版。第三方核心在第二道关关闭时"不存在"（404），
        // 与整个 MC 功能关闭时的口径一致——不泄露"这里有东西可以开"。
        String core = stringField(body, "core");
        if (core == null || core.isBlank()) {
            core = McInstance.CORE_VANILLA;
        }
        core = core.trim().toLowerCase(java.util.Locale.ROOT);
        if (!McCores.known(core)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "unknown server core `" + core + "`");
            return;
        }
        if (McCores.isThirdParty(core) && !feature.thirdPartyEnabled()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }

        McInstance instance = manager.create(name, version, port, rconPort, memoryMb, levelName, seed,
                legacy, core);
        if (feature.consent().isPresent()) {
            manager.acceptEula(instance);
        }
        audit("mc.instance.create", instance.id(), "success", request);
        Json.write(response, HttpServletResponse.SC_CREATED, instanceJson(instance));
    }

    /// Where servers can be created, and which one the form should preselect.
    ///
    /// Both paths are created (best effort) so the answer is `writable` for a
    /// directory the operator has not touched yet, and `data` is reported as
    /// unavailable when this deployment never had one.
    private void storageInfo(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.add("external", storageJson(manager.root()));
        java.nio.file.Path legacy = manager.legacyRoot();
        if (legacy == null) {
            body.add("data", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.add("data", storageJson(legacy));
        }
        body.addProperty("default", defaultStorage());
        Json.write(response, body);
    }

    private static JsonObject storageJson(java.nio.file.Path directory) {
        boolean writable;
        try {
            java.nio.file.Files.createDirectories(directory);
            writable = java.nio.file.Files.isWritable(directory);
        } catch (IOException e) {
            writable = false;
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("path", directory.toString());
        entry.addProperty("writable", writable);
        return entry;
    }

    private String defaultStorage() {
        if (java.nio.file.Files.isWritable(manager.root())) {
            return "external";
        }
        return manager.legacyRoot() == null ? "external" : "data";
    }

    /// Edits an instance: its name, its ports, its heap.
    ///
    /// The three are validated together because a port edit has to be checked
    /// against **other** instances (not against the one being edited), and the
    /// game reads both ports from `server.properties` at launch — so a change
    /// here writes the manifest and that file, and is refused while running.
    private void patchInstance(HttpServletRequest request, HttpServletResponse response,
                               McInstance instance) throws IOException, McException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }

        McInstance updated = instance;
        boolean changed = false;
        boolean portsChanged = false;

        String name = stringField(body, "name");
        if (name != null && !name.isBlank()) {
            updated = updated.withName(name.trim());
            changed = true;
        }

        int port = intField(body, "port");
        int rconPort = intField(body, "rconPort");
        if (port != 0 || rconPort != 0) {
            if (runtime.isRunning(instance.id())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "stop the server before changing its ports");
                return;
            }
            int newPort = port == 0 ? instance.port() : port;
            int newRcon = rconPort == 0 ? instance.rconPort() : rconPort;
            try {
                McPorts.validate(newPort, newRcon);
            } catch (IllegalArgumentException e) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            if (manager.portTaken(newPort, instance.id())) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "port " + newPort + " is already used by another instance");
                return;
            }
            if (manager.portTaken(newRcon, instance.id())) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "port " + newRcon + " is already used by another instance");
                return;
            }
            if (newPort != instance.port() || newRcon != instance.rconPort()) {
                updated = updated.withPorts(newPort, newRcon);
                changed = true;
                portsChanged = true;
            }
        }

        int memoryMb = intField(body, "memoryMb");
        if (memoryMb != 0 && memoryMb != instance.memoryMb()) {
            if (runtime.isRunning(instance.id())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "stop the server before changing its memory");
                return;
            }
            if (memoryMb < McInstance.MIN_MEMORY_MB || memoryMb > maxMemoryMb()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "memoryMb must be between " + McInstance.MIN_MEMORY_MB
                                + " and " + maxMemoryMb());
                return;
            }
            updated = updated.withMemory(memoryMb);
            changed = true;
        }

        if (body.has("javaPath")) {
            com.google.gson.JsonElement raw = body.get("javaPath");
            if (!raw.isJsonNull() && (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString())) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "javaPath must be a string or null");
                return;
            }
            String requested = stringField(body, "javaPath");
            String desired = requested == null || requested.isBlank() ? null : requested.trim();
            boolean same = desired == null
                    ? instance.javaPath() == null
                    : desired.equals(instance.javaPath());
            if (!same) {
                if (runtime.isRunning(instance.id())) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before changing its Java");
                    return;
                }
                if (desired != null && runtime.javaOptions().stream()
                        .noneMatch(option -> option.path().equals(desired))) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "that Java is not one of the runtimes this panel can use");
                    return;
                }
                updated = updated.withJava(desired);
                changed = true;
            }
        }

        if (!changed) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "nothing to update: send name, port, rconPort, memoryMb or javaPath");
            return;
        }

        if (portsChanged) {
            // 清单和 server.properties 两处都写，否则面板说 25566、游戏还在 25565 上听着。
            manager.updatePorts(updated, updated.port(), updated.rconPort());
        } else {
            manager.update(updated);
        }
        audit("mc.instance.update", instance.id(), "success", request);
        Json.write(response, instanceJson(manager.find(instance.id())));
    }

    private void routeInstance(HttpServletRequest request, HttpServletResponse response,
                               String method, String rest) throws IOException, McException {
        String[] segments = rest.split("/", -1);
        String id = segments[0];
        McInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (segments.length == 1) {
            if ("GET".equals(method)) {
                Json.write(response, instanceJson(instance));
                return;
            }
            if ("PATCH".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                patchInstance(request, response, instance);
                return;
            }
            if ("DELETE".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                if (runtime.isRunning(id)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT, "stop the server before deleting it");
                    return;
                }
                // An install may still be downloading into this directory after
                // the page was closed. Cancel it before the backup/delete, or the
                // task keeps writing into a directory that no longer exists and
                // stays "running" in the task list forever.
                tasks.activeInstallLike(id).ifPresent(info -> {
                    if (tasks.cancel(info.id())) {
                        LOG.info("Cancelled the install task of deleted instance " + id);
                    }
                });
                // The world is the only irreplaceable thing here: a delete always
                // leaves a snapshot behind, and refuses when one cannot be taken.
                McBackups.Backup safety = backups.create(instance, manager);
                manager.delete(id);
                audit("mc.instance.delete", id + " (backup " + safety.name() + ")", "success", request);
                Json.write(response, Map.of("ok", true, "backup", safety.name()));
                return;
            }
        }
        String action = segments.length > 1 ? segments[1] : "";
        switch (action) {
            case "start" -> {
                // The second gate is about the program, not about the moment it was created:
                // a third-party core created while the gate was on must not start after it
                // was switched off. 404 keeps "there is something here" from leaking.
                if (McCores.isThirdParty(instance.core()) && !feature.thirdPartyEnabled()) {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                    return;
                }
                // A world being unpacked is a directory mid-swap; starting now would read a
                // world that is about to be moved away.
                if (tasks.activeInstallLike(id)
                        .filter(info -> "mc-world-import".equals(info.kind())).isPresent()) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "a world import is running for this instance; wait for it to finish");
                    return;
                }
                runtime.launch(id);
                audit("mc.instance.start", id, "success", request);
                Json.write(response, instanceJson(instance));
            }
            case "stop" -> {
                if (!requireElevation(request, response)) {
                    return;
                }
                runtime.stop(id);
                audit("mc.instance.stop", id, "success", request);
                Json.write(response, instanceJson(instance));
            }
            case "kill" -> {
                if (!requireElevation(request, response)) {
                    return;
                }
                runtime.kill(id);
                audit("mc.instance.kill", id, "success", request);
                Json.write(response, instanceJson(instance));
            }
            case "install" -> {
                if (!requireElevation(request, response)) {
                    return;
                }
                if (runtime.isRunning(id)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before changing its version");
                    return;
                }
                JsonObject body = readJson(request);
                String version = body == null ? null : stringField(body, "version");
                if (version == null || version.isBlank()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "version is required");
                    return;
                }
                // A download is tens of megabytes on a possibly slow link: run it
                // as a task and report bytes, instead of holding the request open.
                try {
                    TaskService.Task task = tasks.submit("mc-install", id, live -> {
                        // The byte counter only covers downloads; the phase label comes
                        // from the core itself, so a slow installer download no longer
                        // reads as "Installing" for five minutes.
                        java.util.concurrent.atomic.AtomicReference<String> phase =
                                new java.util.concurrent.atomic.AtomicReference<>(
                                        "Downloading " + instance.core() + " " + version);
                        live.update(phase.get(), -1);
                        cores.install(instance, manager, version.trim(),
                                new McVersions.Progress() {
                                    @Override
                                    public void update(long copied, long total) {
                                        String progress = (copied / (1024 * 1024)) + " / "
                                                + (total > 0 ? (total / (1024 * 1024)) + " MB"
                                                : "? MB");
                                        live.update(phase.get() + ": " + progress,
                                                total > 0 ? (double) copied / total : -1);
                                    }

                                    @Override
                                    public void stage(String next) {
                                        phase.set(next);
                                        live.update(next, -1);
                                    }
                                }, runtime.javaFor(instance));
                        return "Installed " + instance.core() + " " + version;
                    }, null);
                    audit("mc.instance.install", id + "@" + version, "submitted", request);
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                    Json.write(response, Map.of("taskId", task.id()));
                } catch (TaskService.InstanceBusyException e) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
                }
            }
            case "logs" -> {
                int tail = parseInt(request.getParameter("tail"), 200);
                List<String> lines = manager.tailLog(instance, Math.max(1, Math.min(2000, tail)));
                Json.write(response, Map.of("lines", lines));
            }
            case "files" -> files(request, response, method, instance, segments);
            // 手动导入核心 jar：所有下载源都不可达时的兜底。只对第三方核心开放，
            // 原版仍然只能从 Mojang 清单装——那条路径本来就没坏过。
            case "core" -> {
                if (!"PUT".equals(method)) {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                    return;
                }
                if (!requireElevation(request, response)) {
                    return;
                }
                if (!McCores.isThirdParty(instance.core())) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "vanilla cores are installed from the Mojang manifest, not uploaded");
                    return;
                }
                if (runtime.isRunning(id)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before replacing its core");
                    return;
                }
                String version = request.getParameter("version");
                if (version == null || version.isBlank()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "version is required");
                    return;
                }
                McInstance updated = cores.installUploaded(instance, manager, version.strip(),
                        request.getInputStream());
                audit("mc.instance.core.upload", id + "/" + version.strip(), "success", request);
                Json.write(response, instanceJson(updated));
            }
            case "backups" -> backups(request, response, method, instance, segments);
            case "console" -> {
                String consoleAction = segments.length > 2 ? segments[2] : "";
                if (!requireElevation(request, response)) {
                    return;
                }
                String mode = runtime.consoleMode(id);
                if (mode == null) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT, "the server is not running");
                    return;
                }
                if ("input".equals(consoleAction)) {
                    JsonObject body = readJson(request);
                    String line = body == null ? null : stringField(body, "line");
                    if (line == null || line.isBlank()) {
                        Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "line is required");
                        return;
                    }
                    runtime.sendCommand(id, line.strip());
                    Json.write(response, Map.of("ok", true));
                    return;
                }
                if (!"ticket".equals(consoleAction)) {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                    return;
                }
                if ("pipe".equals(mode)) {
                    // Windows: no PTY, the page shows the log plus a command box.
                    JsonObject body = new JsonObject();
                    body.addProperty("mode", "pipe");
                    body.addProperty("instance", id);
                    body.addProperty("brand", McRuntime.BRAND);
                    Json.write(response, body);
                    return;
                }
                String ticket;
                if (AuthFilter.isAgent(request)) {
                    // An agent has no session cookie for the handshake, so the
                    // ticket names the grant instead; `mc-console` (checked by
                    // the request filter) is the class that allowed this call.
                    // The ticket still has to be presented together with that
                    // grant's bearer token — the URL alone attaches nothing.
                    var grant = AuthFilter.agentGrant(request);
                    if (grant == null) {
                        Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                                "agent_scope_denied: no agent grant on this request");
                        return;
                    }
                    ticket = tickets.issueForAgent(id, McRuntime.BRAND, grant.id());
                    audit("mc.instance.console.ticket", id, "agent", request);
                } else {
                    ticket = tickets.issue(id, McRuntime.BRAND, sessionToken(request));
                }
                JsonObject body = new JsonObject();
                body.addProperty("mode", "pty");
                body.addProperty("protocol", TerminalWebSocket.PROTOCOL);
                body.addProperty("instance", id);
                body.addProperty("brand", McRuntime.BRAND);
                body.addProperty("ticket", ticket);
                body.addProperty("ws", "/ws/terminal/" + ticket);
                Json.write(response, body);
            }
            case "java" -> {
                if (!"GET".equals(method)) {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                            "method not allowed");
                    return;
                }
                instanceJava(response, instance);
            }
            case "world" -> {
                String worldAction = segments.length > 2 ? segments[2] : "";
                if (!requireElevation(request, response)) {
                    return;
                }
                // The browser's own download can only carry a cookie, not the retry wrapper the
                // JSON calls use: this probe lets the page ask for the developer password first,
                // then hand the URL to the browser.
                if ("authorize".equals(worldAction) && "POST".equals(method)) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
                if (runtime.isRunning(id)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before moving the world");
                    return;
                }
                if ("export".equals(worldAction) && "GET".equals(method)) {
                    response.setContentType("application/zip");
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"" + id + "-" + instance.levelName() + ".zip\"");
                    // Streamed, not buffered: a 5 GB world leaves the panel as it is read.
                    McWorlds.export(instance, manager, response.getOutputStream());
                    audit("mc.instance.world.export", id, "success", request);
                    return;
                }
                if ("import".equals(worldAction) && "POST".equals(method)) {
                    importWorld(request, response, id, instance);
                    return;
                }
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            }
            case "player-lists" -> {
                if (!requireElevation(request, response)) {
                    return;
                }
                if (!"POST".equals(method)) {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                    return;
                }
                if (runtime.isRunning(id)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before replacing its player lists");
                    return;
                }
                Map<String, Integer> counts = McPlayerLists.importZip(instance, manager, request.getInputStream());
                audit("mc.instance.player-lists.import", id + " " + counts, "success", request);
                JsonObject body = new JsonObject();
                counts.forEach(body::addProperty);
                Json.write(response, Map.of("ok", true, "files", body));
            }
            case "players" -> {
                if ("GET".equals(method) && segments.length == 2) {
                    Json.write(response, McRcon.players(instance, manager));
                    return;
                }
                if ("POST".equals(method) && segments.length == 4) {
                    if (!requireElevation(request, response)) {
                        return;
                    }
                    String name = segments[2];
                    String verb = segments[3];
                    if (!name.matches("[A-Za-z0-9_]{1,16}")) {
                        Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "invalid player name");
                        return;
                    }
                    if (!List.of("kick", "op", "deop", "ban").contains(verb)) {
                        Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown action");
                        return;
                    }
                    String answer = McRcon.command(instance, manager, verb + " " + name);
                    audit("mc.instance.player." + verb, id + "/" + name, "success", request);
                    Json.write(response, Map.of("ok", true, "answer", answer.strip()));
                    return;
                }
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            }
            case "properties" -> properties(request, response, method, instance);
            default -> Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        }
    }

    private void backups(HttpServletRequest request, HttpServletResponse response, String method,
                         McInstance instance, String[] segments) throws IOException, McException {
        if (segments.length == 2) {
            if ("GET".equals(method)) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (McBackups.Backup backup : backups.list(instance.id())) {
                    items.add(Map.of("name", backup.name(), "createdAt", backup.createdAt(),
                            "bytes", backup.bytes()));
                }
                Json.write(response, Map.of("backups", items));
                return;
            }
            if ("POST".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                if (runtime.isRunning(instance.id())) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "stop the server before taking a backup (a hot zip can be inconsistent)");
                    return;
                }
                McBackups.Backup backup = backups.create(instance, manager);
                audit("mc.instance.backup", instance.id() + "/" + backup.name(), "success", request);
                response.setStatus(HttpServletResponse.SC_CREATED);
                Json.write(response, Map.of("name", backup.name(), "createdAt", backup.createdAt(),
                        "bytes", backup.bytes()));
                return;
            }
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET or POST");
            return;
        }
        String name = segments[2];
        if (segments.length == 3 && "DELETE".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            backups.delete(instance.id(), name);
            audit("mc.instance.backup.delete", instance.id() + "/" + name, "success", request);
            Json.write(response, Map.of("ok", true));
            return;
        }
        if (segments.length == 4 && "restore".equals(segments[3]) && "POST".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            if (runtime.isRunning(instance.id())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "stop the server before restoring a backup");
                return;
            }
            McBackups.Backup safety = backups.restore(instance, manager, name);
            audit("mc.instance.restore", instance.id() + "/" + name
                    + " (safety " + safety.name() + ")", "success", request);
            Json.write(response, Map.of("ok", true, "safety", safety.name()));
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void properties(HttpServletRequest request, HttpServletResponse response,
                            String method, McInstance instance) throws IOException, McException {
        if ("GET".equals(method)) {
            // `rcon.password` is a generated secret the panel uses itself; returning it here
            // made every read of this endpoint a credential read.
            java.util.Map<String, String> visible =
                    new java.util.LinkedHashMap<>(manager.properties(instance).asMap());
            visible.remove("rcon.password");
            Json.write(response, Map.of("properties", visible));
            return;
        }
        if (!"PUT".equals(method)) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET or PUT");
            return;
        }
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        McProperties properties = manager.properties(instance);
        for (String key : body.keySet()) {
            if (!MANAGED_PROPERTIES.contains(key)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "`" + key + "` is not a managed field");
                return;
            }
            String value = stringField(body, key);
            if (value == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "`" + key + "` must be a string");
                return;
            }
            if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                // A newline in a value writes an extra `key=value` line into server.properties.
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "`" + key + "` must be a single line");
                return;
            }
            properties.set(key, value);
        }
        manager.saveProperties(instance, properties);
        audit("mc.instance.properties", instance.id(), "success", request);
        Json.write(response, Map.of("properties", properties.asMap()));
    }

    /// `POST /instances/{id}/world/import`: save the body, unpack it on a task.
    ///
    /// The upload itself is still one request — that is what lets the browser show true byte
    /// progress. Unpacking is not: a 5 GB world is minutes of disk work, and a proxy with a
    /// 60-second read timeout would turn a finished install into a 504.
    ///
    /// @param request  the request whose body is the zip
    /// @param response the response
    /// @param id       the instance id
    /// @param instance the instance
    /// @throws IOException  when the body cannot be saved
    /// @throws McException when the upload is too large or the task cannot be submitted
    private void importWorld(HttpServletRequest request, HttpServletResponse response,
                             String id, McInstance instance) throws IOException, McException {
        if (tasks.activeInstallLike(id).isPresent()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "another install or import is already running for this instance");
            return;
        }
        long declared = request.getContentLengthLong();
        if (declared > McWorlds.MAX_IMPORT_BYTES) {
            Json.error(response, 413, "the world zip is larger than the panel accepts ("
                    + (McWorlds.MAX_IMPORT_BYTES / (1024 * 1024 * 1024)) + " GiB)");
            return;
        }
        java.nio.file.Path archive = McWorlds.archiveFile(manager, instance);
        if (declared > 0) {
            // Say no before tens of gigabytes cross the network, not after they land on a full
            // disk. The unpacked size is unknown here; this is the archive plus slack.
            McWorlds.requireFreeSpace(manager.instanceDirectory(id),
                    declared + 1024L * 1024 * 1024);
        }
        try {
            try (java.io.InputStream in = request.getInputStream()) {
                McWorlds.save(in, archive);
            }
            TaskService.Task task = tasks.submit("mc-world-import", id, live -> {
                live.update("Unpacking the world", 0.02);
                String safety = McWorlds.importArchive(instance, manager, backups, archive,
                        (files, total, bytes, totalBytes) -> live.update(
                                "Unpacking " + files + "/" + total + " files ("
                                        + (bytes / (1024 * 1024)) + " MB"
                                        + (totalBytes > 0 ? " / " + (totalBytes / (1024 * 1024)) : "")
                                        + ")",
                                totalBytes > 0 ? 0.05 + 0.9 * bytes / totalBytes : -1));
                JsonObject result = new JsonObject();
                result.addProperty("safety", safety == null ? "" : safety);
                live.attachResult(result);
                return safety == null
                        ? "World imported"
                        : "World imported; the previous world is in backup " + safety;
            });
            audit("mc.instance.world.import", id, "submitted", request);
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            Json.write(response, Map.of("taskId", task.id()));
        } catch (TaskService.InstanceBusyException e) {
            java.nio.file.Files.deleteIfExists(archive);
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (McException | IOException | RuntimeException e) {
            java.nio.file.Files.deleteIfExists(archive);
            throw e;
        }
    }

    // ------------------------------------------------------------------- helpers --

    // ------------------------------------------------------------- plugins & mods --

    /// `/instances/{id}/files…`: list, upload, toggle, delete.
    private void files(HttpServletRequest request, HttpServletResponse response, String method,
                       McInstance instance, String[] segments) throws IOException, McException {
        McMods.Folder folder = McMods.folderOf(instance.core());
        if (folder == null) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "this server core loads neither plugins nor mods");
            return;
        }
        java.nio.file.Path directory = manager.instanceDirectory(instance.id());
        String kind = McMods.requireKind(segments.length > 2 ? segments[2] : request.getParameter("kind"));
        if (segments.length == 2) {
            if (!"GET".equals(method)) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            JsonObject body = new JsonObject();
            JsonObject folderJson = new JsonObject();
            folderJson.addProperty("kind", kind);
            if (folder.loader() != null) {
                folderJson.addProperty("loader", folder.loader());
            }
            body.add("folder", folderJson);
            JsonArray items = new JsonArray();
            for (McMods.Entry entry : mods.list(directory, kind)) {
                JsonObject item = new JsonObject();
                item.addProperty("name", entry.name());
                item.addProperty("enabled", entry.enabled());
                item.addProperty("bytes", entry.bytes());
                item.addProperty("clientOnly", entry.clientOnly());
                if (entry.projectId() != null) {
                    item.addProperty("projectId", entry.projectId());
                }
                if (entry.title() != null) {
                    item.addProperty("title", entry.title());
                }
                if (entry.versionNumber() != null) {
                    item.addProperty("versionNumber", entry.versionNumber());
                }
                items.add(item);
            }
            body.add("files", items);
            Json.write(response, body);
            return;
        }
        if (segments.length == 3 && "PUT".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            String stored = mods.upload(directory, kind, request.getParameter("name"),
                    request.getInputStream());
            audit("mc.instance.mod.upload", instance.id() + "/" + stored, "success", request);
            Json.write(response, HttpServletResponse.SC_CREATED,
                    Map.of("ok", true, "name", stored));
            return;
        }
        if (segments.length == 4) {
            String name = segments[3];
            if ("DELETE".equals(method)) {
                if (!requireElevation(request, response)) {
                    return;
                }
                mods.delete(directory, kind, name);
                audit("mc.instance.mod.delete", instance.id() + "/" + name, "success", request);
                Json.write(response, Map.of("ok", true));
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        if (segments.length == 5 && "toggle".equals(segments[4]) && "POST".equals(method)) {
            if (!requireElevation(request, response)) {
                return;
            }
            JsonObject body = readJson(request);
            boolean enabled = body != null && body.has("enabled")
                    && body.get("enabled").isJsonPrimitive() && body.get("enabled").getAsBoolean();
            String renamed = mods.toggle(directory, kind, segments[3], enabled);
            audit("mc.instance.mod.toggle", instance.id() + "/" + renamed,
                    enabled ? "enabled" : "disabled", request);
            Json.write(response, Map.of("ok", true, "name", renamed, "enabled", enabled));
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    /// `GET /api/mc/modrinth/search`: what this instance could install.
    private void modrinthSearch(HttpServletRequest request, HttpServletResponse response)
            throws IOException, McException {
        McMods.Folder folder = McMods.folderOf(request.getParameter("core"));
        if (folder == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "this core loads neither plugins nor mods");
            return;
        }
        int offset = parseInt(request.getParameter("offset"), 0);
        int limit = parseInt(request.getParameter("limit"), 20);
        McMods.SearchResult found = mods.search(request.getParameter("query"), folder,
                request.getParameter("version"), Math.max(0, offset), limit);
        List<JsonObject> items = new ArrayList<>();
        for (McMods.Hit hit : found.hits()) {
            JsonObject item = new JsonObject();
            item.addProperty("projectId", hit.projectId());
            item.addProperty("slug", hit.slug());
            item.addProperty("title", hit.title());
            item.addProperty("author", hit.author());
            item.addProperty("downloads", hit.downloads());
            item.addProperty("description", hit.description());
            items.add(item);
        }
        JsonArray array = new JsonArray();
        items.forEach(array::add);
        JsonObject body = new JsonObject();
        body.add("hits", array);
        body.addProperty("totalHits", found.totalHits());
        body.addProperty("offset", Math.max(0, offset));
        body.addProperty("relaxed", found.relaxed());
        Json.write(response, body);
    }

    /// `POST /api/mc/modrinth/install`: newest compatible version into the instance.
    private void modrinthInstall(HttpServletRequest request, HttpServletResponse response)
            throws IOException, McException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        String id = body == null ? null : stringField(body, "instanceId");
        String projectId = body == null ? null : stringField(body, "projectId");
        if (id == null || projectId == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "instanceId and projectId are required");
            return;
        }
        McInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        McMods.Folder folder = McMods.folderOf(instance.core());
        if (folder == null) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "this server core loads neither plugins nor mods");
            return;
        }
        String name = mods.install(manager.instanceDirectory(instance.id()), folder,
                projectId, instance.version());
        audit("mc.instance.mod.install", instance.id() + "/" + name, "success", request);
        Json.write(response, Map.of("ok", true, "name", name));
    }

    /// `GET /instances/{id}/java`: which Java this instance will use, and the
    /// menu of runtimes the advanced setting may pin it to.
    private void instanceJava(HttpServletResponse response, McInstance instance) throws IOException {
        JsonObject body = new JsonObject();
        if (instance.javaPath() == null) {
            body.add("selected", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("selected", instance.javaPath());
        }
        body.add("effective", javaProbeJson(runtime.javaInfoFor(instance)));
        JsonArray options = new JsonArray();
        for (McRuntime.JavaInfo option : runtime.javaOptions()) {
            options.add(javaProbeJson(option));
        }
        body.add("options", options);
        Json.write(response, body);
    }

    /// One probed Java in the shape the instance page reads.
    private static JsonObject javaProbeJson(McRuntime.JavaInfo info) {
        JsonObject body = new JsonObject();
        body.addProperty("path", info.path());
        if (info.version() == null) {
            body.add("version", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("version", info.version());
        }
        body.addProperty("major", info.major());
        body.addProperty("source", info.source());
        body.addProperty("ok", info.ok());
        if (info.error() != null) {
            body.addProperty("error", info.error());
        }
        return body;
    }

    private JsonObject instanceJson(McInstance instance) {
        McRuntime.Status status = runtime.status(instance.id());
        JsonObject body = new JsonObject();
        body.addProperty("id", instance.id());
        body.addProperty("name", instance.name());
        body.addProperty("version", instance.version());
        body.addProperty("core", instance.core());
        // 官方（Mojang 原版）与第三方核心在界面与启动门上要分得开：第三方要过第二道关。
        body.addProperty("thirdParty", McCores.isThirdParty(instance.core()));
        body.addProperty("port", instance.port());
        body.addProperty("rconPort", instance.rconPort());
        body.addProperty("memoryMb", instance.memoryMb());
        // `null`/empty = automatic (the version's requirement decides); otherwise
        // the executable selected in the advanced settings.
        body.addProperty("javaPath", instance.javaPath());
        body.addProperty("levelName", instance.levelName());
        // 文件到底在哪个目录：向导与实例页都把它显示出来——「存档去哪了」这个问题
        // 应该由界面回答，而不是由用户在 issue 里问。
        body.addProperty("dir", manager.instanceDirectory(instance.id()).toString());
        body.addProperty("storage", manager.storageOf(instance.id()));
        if (instance.seed() != null) {
            body.addProperty("seed", instance.seed());
        }
        body.addProperty("createdAt", instance.createdAt());
        body.addProperty("state", status.state().name());
        if (status.error() != null) {
            body.addProperty("error", status.error());
        }
        // `console()` only names a PTY session. A Windows pipe server has its
        // console in the log + input box, and the UI must still enable the
        // button — that was the one path where the fix was invisible.
        body.addProperty("consoleAvailable", runtime.consoleMode(instance.id()) != null);
        return body;
    }

    /// The per-instance heap ceiling this deployment accepts: `mc.max_memory_mb`
    /// cannot raise the model's hard cap, only lower it.
    ///
    /// @return megabytes
    private int maxMemoryMb() {
        return Math.max(McInstance.MIN_MEMORY_MB,
                Math.min(config.mc.maxMemoryMb, McInstance.MAX_MEMORY_MB));
    }

    /// Step-up for the operations that start processes, delete data or change
    /// the feature. The pattern is the brand page's: HTTPS + developer
    /// elevation, with the reason spelled out when it is missing.
    private boolean requireElevation(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "tls_required: the developer elevation is only accepted over HTTPS");
            return false;
        }
        if (!developerAuth.isSet()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "developer_password_not_set: set the developer password before enabling the Minecraft feature");
            return false;
        }
        if (AuthFilter.elevationSatisfied(
                request, elevation, keyOf(request), sessionAlive(request))) {
            return true;
        }
        Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                "developer_elevation_required: elevate with the developer password first");
        return false;
    }

    private String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static @Nullable String sessionToken(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return token instanceof String value ? value : null;
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    private void audit(String action, String object, String outcome, HttpServletRequest request) {
        AuditLog.log(AuditLog.Channel.WEB, action, object, outcome, actorOf(request), request.getRemoteAddr());
    }

    private static @Nullable JsonObject readJson(HttpServletRequest request) throws IOException {
        String text = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String stringField(JsonObject body, String name) {
        return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsString() : null;
    }

    private static boolean booleanField(JsonObject body, String name) {
        return body.has(name) && body.get(name).isJsonPrimitive() && body.get(name).getAsBoolean();
    }

    private static int intField(JsonObject body, String name) {
        try {
            return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsInt() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static int parseInt(@Nullable String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}