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

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.terminal.InstanceTerminal;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalTickets;
import org.jackhuang.hmcl.web.terminal.ZcodeTui;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The REST surface of the **experimental** ZCode category, mapped at
/// `/api/zcode/*` (behind the same [org.jackhuang.hmcl.web.auth.AuthFilter]
/// session gate as every other `/api/*` route):
///
/// - `GET    /api/zcode/dist`                      — `{present, path, version}`
///   for the discovered distribution (`HDSL_ZCODE_PACKAGE`, else the newest
///   installed release below `<dataDir>/zcode/releases/`, else
///   `<dataDir>/zcode/current/`; valid when it holds `bin/zcode.mjs`)
/// - `GET    /api/zcode/releases`                  — the installed releases,
///   newest first, each flagged with whether it is the current one
/// - `POST   /api/zcode/build`                     — `{version}`, an upstream
///   tag or branch: starts the in-panel build (download → patch → pnpm install
///   → pnpm build:zcode → install) and returns 202; one build at a time
/// - `GET    /api/zcode/build`                     — the build's state, current
///   step and fraction
/// - `GET    /api/zcode/build/log?tail=N`          — the tail of the build log
/// - `GET    /api/zcode/instances`                 — every instance with its
///   runtime state and, while running, its `openUrl`
/// - `POST   /api/zcode/instances`                 — create `{name, baseUrl?, apiKey?}`
/// - `GET    /api/zcode/instances/{id}`            — one instance
/// - `PATCH  /api/zcode/instances/{id}`            — edit `{name?, baseUrl?, apiKey?}`;
///   credential changes are refused while the instance is running
/// - `DELETE /api/zcode/instances/{id}`            — remove; refused while running
/// - `POST   /api/zcode/instances/{id}/launch`     — start the process, wait for
///   its readiness line; idempotent while already running
/// - `POST   /api/zcode/instances/{id}/stop`       — SIGTERM the process
/// - `GET    /api/zcode/instances/{id}/open`       — `{url}` of the running
///   instance: `/i/<id>/`, the panel's own reverse-proxied mount — same port,
///   same certificate and the same session gate as dsh
/// - `GET    /api/zcode/instances/{id}/logs?tail=N`— the trailing lines of the
///   process log
/// - `GET    /api/zcode/instances/{id}/plugins`    — the plugin channel, same
///   shape as the brand categories': `{supported: false, plugins: []}`
///
/// The category is an implementation-level experiment around ZCode
/// (zai-org/ZCode, Apache-2.0): nothing here is covered by the dsh category's
/// compatibility promises, API keys are stored in clear text in the instance
/// manifest, and distributions are built from upstream source with [ZcodePatch]
/// applied. Instances bind loopback only and are reached through the panel's
/// `/i/<id>/` reverse proxy — the loopback-less, plain-HTTP era documented
/// earlier is over, and the patch in the built distribution is what makes the
/// proxied mounting work.
@NotNullByDefault
public final class ZcodeApiServlet extends HttpServlet {

    private static final int DEFAULT_LOG_TAIL = 200;
    private static final int MAX_LOG_TAIL = 2000;
    private static final int DEFAULT_BUILD_LOG_TAIL = 400;
    private static final int MAX_BUILD_LOG_TAIL = 4000;

    private final ServerConfig config;
    private final ZcodeInstanceManager manager;
    private final ZcodeBuilder.Settings buildSettings;
    private final InstanceTerminal instanceTerminal;

    public ZcodeApiServlet(ServerConfig config, TerminalManager terminals, TerminalTickets terminalTickets,
                           AuthService auth, DeveloperAuth developerAuth,
                           DeveloperElevation developerElevation,
                           java.util.function.BooleanSupplier tlsActive,
                           org.jackhuang.hmcl.web.emergency.EmergencyStop emergency) {
        this.config = config;
        Path root = config.dataDir.resolve("zcode");
        this.manager = new ZcodeInstanceManager(root.resolve("instances"));
        // Directories a delete could not remove (something held a file inside) were
        // renamed aside; at startup whatever held them is most likely gone.
        this.manager.sweepTombstones();
        this.buildSettings = new ZcodeBuilder.Settings(root, buildBin(config.zcode.buildBin), config.zcode.pnpm,
                config.zcode.sourceUrl, config.zcode.keepSources);
        this.instanceTerminal = new InstanceTerminal(config, terminals, terminalTickets, auth,
                developerAuth, developerElevation, tlsActive, emergency);
    }

    /// The directory to put ahead of `PATH` for a build — the Node 24 that the image
    /// keeps solely for this job.
    ///
    /// Everything else is [ZcodeBuilder]'s job: a bare deployment has no `/opt/node24`,
    /// and when the setting is empty the builder looks for a managed runtime under the
    /// data directory and **installs one** (plus pnpm) when there is none. Doing that
    /// resolution at build time rather than here is what makes the one-click promise
    /// hold — the first build on an empty Windows machine provisions itself.
    ///
    /// @param configured `zcode.buildBin` from the environment, may be blank
    /// @return the directory to prepend, or an empty string to let the builder decide
    private static String buildBin(@Nullable String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        Path image = Path.of("/opt/node24/bin");
        return Files.isDirectory(image) ? image.toString() : "";
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] segments = split(request.getPathInfo());
        try {
            if (segments.length == 1 && "dist".equals(segments[0]) && "GET".equals(request.getMethod())) {
                dist(response);
                return;
            }
            if (segments.length == 1 && "releases".equals(segments[0]) && "GET".equals(request.getMethod())) {
                releases(response);
                return;
            }
            if (segments.length == 1 && "catalog".equals(segments[0]) && "GET".equals(request.getMethod())) {
                catalog(request, response);
                return;
            }
            if (segments.length >= 1 && "build".equals(segments[0])) {
                if (segments.length == 1) {
                    switch (request.getMethod()) {
                        case "GET" -> buildStatus(response);
                        case "POST" -> startBuild(request, response);
                        default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                "method not allowed");
                    }
                    return;
                }
                if (segments.length == 2 && "log".equals(segments[1]) && "GET".equals(request.getMethod())) {
                    buildLog(request, response);
                    return;
                }
            }
            if (segments.length == 1 && "instances".equals(segments[0])) {
                switch (request.getMethod()) {
                    case "GET" -> listInstances(request, response);
                    case "POST" -> createInstance(request, response);
                    default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            if (segments.length >= 2 && "instances".equals(segments[0])) {
                String id = segments[1];
                if (segments.length == 2) {
                    switch (request.getMethod()) {
                        case "GET" -> getInstance(request, response, id);
                        case "PATCH" -> patchInstance(request, response, id);
                        case "DELETE" -> deleteInstance(request, response, id);
                        default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                    }
                    return;
                }
                if (segments.length == 3 && "launch".equals(segments[2]) && "POST".equals(request.getMethod())) {
                    launch(request, response, id);
                    return;
                }
                if (segments.length == 3 && "stop".equals(segments[2]) && "POST".equals(request.getMethod())) {
                    stop(request, response, id);
                    return;
                }
                if (segments.length == 3 && "terminal".equals(segments[2])
                        && ("POST".equals(request.getMethod()) || "DELETE".equals(request.getMethod()))) {
                    terminal(request, response, id);
                    return;
                }
                if (segments.length == 3 && "open".equals(segments[2]) && "GET".equals(request.getMethod())) {
                    open(request, response, id);
                    return;
                }
                if (segments.length == 3 && "logs".equals(segments[2]) && "GET".equals(request.getMethod())) {
                    logs(request, response, id);
                    return;
                }
                if (segments.length == 3 && "plugins".equals(segments[2]) && "GET".equals(request.getMethod())) {
                    plugins(response, id);
                    return;
                }
                if (segments.length == 3 && "rotate-key".equals(segments[2]) && "POST".equals(request.getMethod())) {
                    rotateKey(request, response, id);
                    return;
                }
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        } catch (ZcodeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // -------------------------------------------------------------------- dist --

    private void dist(HttpServletResponse response) throws IOException {
        Path directory = packageDir();
        boolean present = Files.isRegularFile(directory.resolve("bin/zcode.mjs"));
        JsonObject body = new JsonObject();
        body.addProperty("present", present);
        body.addProperty("path", directory.toString());
        body.add("version", present ? versionOf(directory) : JsonNull.INSTANCE);
        Json.writePreservingNulls(response, body);
    }

    /// The installed releases, newest first.
    private void releases(HttpServletResponse response) throws IOException {
        JsonArray array = new JsonArray();
        for (ZcodeBuilder.Release release : ZcodeBuilder.releases(config.dataDir.resolve("zcode"))) {
            JsonObject item = new JsonObject();
            item.addProperty("version", release.version());
            item.addProperty("path", release.path());
            item.addProperty("builtAt", release.builtAt());
            item.addProperty("current", release.current());
            array.add(item);
        }
        JsonObject body = new JsonObject();
        body.add("releases", array);
        Json.write(response, body);
    }

    /// The upstream version catalog: official releases, tag-only tags, and the
    /// unreleased `main` snapshot, annotated with local build and credential state.
    private void catalog(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ZcodeCatalog.Snapshot snapshot;
        try {
            snapshot = ZcodeCatalog.load(config.dataDir.resolve("zcode"),
                    "1".equals(request.getParameter("refresh")));
        } catch (ZcodeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
            return;
        }
        JsonArray entries = new JsonArray();
        for (ZcodeCatalog.Entry entry : snapshot.entries()) {
            JsonObject item = new JsonObject();
            item.addProperty("ref", entry.ref());
            item.addProperty("version", entry.version());
            item.addProperty("channel", entry.channel());
            item.add("commit", entry.commit() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(entry.commit()));
            item.add("publishedAt", entry.publishedAt() > 0
                    ? new com.google.gson.JsonPrimitive(entry.publishedAt()) : JsonNull.INSTANCE);
            item.addProperty("credentialFormat", entry.credentialFormat());
            item.addProperty("requiresCredentialSecret", entry.requiresSecret());
            item.addProperty("installed", entry.installed());
            item.addProperty("buildState", entry.buildState());
            item.addProperty("prerelease", entry.prerelease());
            item.addProperty("notes", entry.notes());
            entries.add(item);
        }
        JsonObject body = new JsonObject();
        body.add("entries", entries);
        body.addProperty("fetchedAt", snapshot.fetchedAt());
        Json.write(response, body);
    }

    /// The distribution directory: the `HDSL_ZCODE_PACKAGE` override, else the
    /// newest installed release (the `current` link the builder maintains, or
    /// whatever `releases/` holds), else where the next build would land.
    private Path packageDir() {
        String configured = config.zcode.packageDir;
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return ZcodeBuilder.currentPackage(config.dataDir.resolve("zcode"));
    }

    /// The package directory an instance is pinned to. A ref is mapped through
    /// the release manifest written at build time, because releases are named
    /// after the package version (`main` builds as `3.18.2`).
    private Path packageDir(ZcodeInstance instance) {
        String version = instance.version();
        if (version == null || version.isBlank()) {
            return packageDir();
        }
        Path root = config.dataDir.resolve("zcode");
        Path direct = root.resolve("releases").resolve(version);
        if (Files.isRegularFile(direct.resolve("bin/zcode.mjs"))) {
            return direct;
        }
        String stripped = version.startsWith("v") || version.startsWith("V")
                ? version.substring(1) : version;
        Path byVersion = root.resolve("releases").resolve(stripped);
        if (Files.isRegularFile(byVersion.resolve("bin/zcode.mjs"))) {
            return byVersion;
        }
        for (ZcodeBuilder.Release release : ZcodeBuilder.releases(root)) {
            Path directory = Path.of(release.path());
            Path manifest = directory.resolve("release.json");
            if (!Files.isRegularFile(manifest)) {
                continue;
            }
            try {
                com.google.gson.JsonObject parsed = com.google.gson.JsonParser.parseString(
                        Files.readString(manifest)).getAsJsonObject();
                if (parsed.has("ref") && parsed.get("ref").isJsonPrimitive()
                        && version.equals(parsed.get("ref").getAsString())) {
                    return directory;
                }
            } catch (Exception ignored) {
                // A malformed manifest is not a match.
            }
        }
        return direct;
    }

    /// The version of the distribution `current` resolves to, for instances
    /// that still follow `current` (manifests written before version pinning).
    private @Nullable String currentVersion() {
        com.google.gson.JsonElement version = versionOf(packageDir());
        return version != null && version.isJsonPrimitive() ? version.getAsString() : null;
    }

    /// The distribution's version, from its `package.json`; `null` when the
    /// file is missing or unreadable.
    private static @Nullable com.google.gson.JsonElement versionOf(Path directory) {
        Path manifest = directory.resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return JsonNull.INSTANCE;
        }
        try {
            com.google.gson.JsonObject parsed = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
            return parsed.has("version") && parsed.get("version").isJsonPrimitive()
                    ? new com.google.gson.JsonPrimitive(parsed.get("version").getAsString())
                    : JsonNull.INSTANCE;
        } catch (Exception e) {
            return JsonNull.INSTANCE;
        }
    }

    // --------------------------------------------------------------- instances --

    private void listInstances(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray instances = new JsonArray();
        for (ZcodeInstance instance : manager.list()) {
            instances.add(instanceJson(instance, request));
        }
        JsonObject body = new JsonObject();
        body.add("instances", instances);
        body.addProperty("workspacesRoot", manager.workspacesRoot().toString());
        Json.writePreservingNulls(response, body);
    }

    private void createInstance(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZcodeException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String name = stringField(body, "name");
        String baseUrl = stringField(body, "baseUrl");
        String apiKey = stringField(body, "apiKey");
        String workspace = stringField(body, "workspacePath");
        String version = stringField(body, "version");
        if (version != null && !version.isBlank()) {
            try {
                ZcodeCatalog.Snapshot snapshot = ZcodeCatalog.load(config.dataDir.resolve("zcode"), false);
                ZcodeCatalog.Entry entry = snapshot.find(version);
                if (entry == null || !entry.installed()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "版本 " + version + " 还没有构建；先在版本卡里构建它");
                    return;
                }
                version = entry.ref();
            } catch (ZcodeException e) {
                Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
                return;
            }
        }
        // A path the operator typed is taken as given: the instance works where they say,
        // not in a directory the panel made up for it.
        java.nio.file.Path workspacePath = workspace == null || workspace.isBlank()
                ? null : java.nio.file.Path.of(workspace.trim());
        ZcodeInstance created = manager.create(name, baseUrl, apiKey, workspacePath, version);
        String seed = stringField(body, "seed");
        if (seed != null && !seed.isBlank()) {
            try {
                ZcodeCredentials.writeSeed(manager, created, seed);
            } catch (ZcodeException e) {
                try {
                    manager.delete(created.id());
                } catch (ZcodeException ignored) {
                    // The original failure is the useful one.
                }
                throw e;
            }
        }
        Json.writePreservingNulls(response, HttpServletResponse.SC_CREATED,
                Map.of("instance", instanceJson(created, request)));
    }

    private void getInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        Json.writePreservingNulls(response, Map.of("instance", instanceJson(instance, request)));
    }

    private void patchInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, ZcodeException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        boolean touchesCredentials = body.has("baseUrl") || body.has("apiKey");
        if (touchesCredentials && ZcodeRuntime.isRunning(id)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "stop the ZCode instance before changing its credentials");
            return;
        }

        ZcodeInstance patched = instance;
        String version = stringField(body, "version");
        if (version != null && !version.isBlank()) {
            if (ZcodeRuntime.isRunning(id)) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "stop the ZCode instance before switching its version");
                return;
            }
            try {
                ZcodeCatalog.Snapshot snapshot = ZcodeCatalog.load(config.dataDir.resolve("zcode"), false);
                ZcodeCatalog.Entry entry = snapshot.find(version);
                if (entry == null || !entry.installed()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "版本 " + version + " 还没有构建；先在版本卡里构建它");
                    return;
                }
                ZcodeInstance effective = instance.credentialFormat() > 0
                        ? instance
                        : instance.withCredentialFormat(
                                ZcodeCredentials.effectiveFormat(manager, instance));
                String reason = ZcodeCatalog.downgradeReason(effective, entry);
                if (reason != null) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT, reason);
                    return;
                }
                patched = patched.withVersion(entry.ref());
            } catch (ZcodeException e) {
                Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
                return;
            }
        }
        String name = stringField(body, "name");
        if (name != null && !name.trim().isEmpty() && !name.trim().equals(instance.name())) {
            patched = patched.withName(name.trim());
        }
        if (body.has("baseUrl") || body.has("apiKey")) {
            String baseUrl = stringField(body, "baseUrl");
            String apiKey = stringField(body, "apiKey");
            patched = patched.withCredentials(
                    baseUrl == null ? patched.baseUrl() : baseUrl,
                    apiKey == null ? patched.apiKey() : apiKey);
        }
        manager.update(patched);
        // Save-time credential injection, best effort by design: a future ZCode
        // that renames the file simply stops reading what we write here.
        manager.writeProviderConfig(patched);
        Json.writePreservingNulls(response, Map.of("instance", instanceJson(patched, request)));
    }

    /// Replaces the instance's credential root key. This is a reset, not a
    /// re-encryption: the old `credentials.json` is discarded, so the operator
    /// must re-authenticate / re-enter the provider key.
    private void rotateKey(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, ZcodeException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        if (ZcodeRuntime.isRunning(id)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "stop the ZCode instance before rotating its key");
            return;
        }
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String seed = stringField(body, "seed");
        boolean clearProviderKey = body.has("clearProviderKey")
                && body.get("clearProviderKey").isJsonPrimitive()
                && body.get("clearProviderKey").getAsBoolean();
        ZcodeCredentials.rotate(manager, instance, seed, clearProviderKey);
        if (clearProviderKey) {
            instance = instance.withCredentials(instance.baseUrl(), null);
            manager.update(instance);
        }
        AuditLog.log(AuditLog.Channel.WEB, "zcode.instance.rotate_key", id,
                AuthFilter.selfDirected(request, ZcodeTui.BRAND, id) ? "self_rotate" : "success",
                actorOf(request), request.getRemoteAddr());
        Json.writePreservingNulls(response, Map.of("instance", instanceJson(instance, request)));
    }

    private void deleteInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, ZcodeException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        if (ZcodeRuntime.isRunning(id)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "stop the ZCode instance before deleting it");
            return;
        }
        instanceTerminal.close(ZcodeTui.BRAND, id);
        manager.delete(id);
        AuditLog.log(AuditLog.Channel.WEB, "zcode.instance.delete", id,
                AuthFilter.selfDirected(request, ZcodeTui.BRAND, id) ? "self_delete" : "success",
                actorOf(request), request.getRemoteAddr());
        response.setStatus(204);
    }

    /// The actor behind a request, for the audit trail: the session's
    /// username, which for an agent grant is `agent:<label>`.
    ///
    /// @param request the request
    /// @return the actor, or `null` when there is no session
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session value ? value.username() : null;
    }

    // ------------------------------------------------------------ launch / stop --

    private void launch(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        Path launchDir = packageDir(instance);
        if (!Files.isRegularFile(launchDir.resolve("bin/zcode.mjs"))) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "版本 " + (instance.version() == null ? "(current)" : instance.version())
                            + " 还没有构建；先在版本卡里构建它（" + launchDir + "）");
            return;
        }
        // The TUI and the web process share the instance's data directory: the
        // web mode only starts once the terminal is gone, or two processes
        // would write the same config.
        instanceTerminal.close(ZcodeTui.BRAND, id);
        // The panel's managed runtime is a first-class answer: a bare `node` is what a
        // Windows deployment does *not* have, and this is where it used to fail
        // (`CreateProcess error=2`) while a perfectly good runtime sat in the data dir.
        Path node;
        try {
            node = nodeExecutable();
        } catch (IOException e) {
            Json.error(response, 409, "找不到可用的 Node.js：" + e.getMessage());
            return;
        }
        Map<String, String> environment = new HashMap<>(System.getenv());
        // The distribution spawns node for its own tools; PATH has to name the same one.
        environment.put("PATH", node.getParent() + java.io.File.pathSeparator
                + environment.getOrDefault("PATH", ""));
        ZcodeRuntime.Status status = ZcodeRuntime.launch(
                manager, instance, environment, node.toString(), launchDir);
        if (status.state() == ZcodeRuntime.State.RUNNING
                && instance.version() != null && !instance.version().isBlank()) {
            int format = ZcodeCatalog.formatOf(instance.version());
            long at = System.currentTimeMillis();
            try {
                ZcodeCatalog.Entry entry = ZcodeCatalog.load(config.dataDir.resolve("zcode"), false)
                        .find(instance.version());
                if (entry != null && entry.publishedAt() > 0) {
                    at = entry.publishedAt();
                }
            } catch (ZcodeException ignored) {
                // The launch already succeeded; a catalog miss only makes the floor
                // timestamp conservative.
            }
            try {
                manager.update(instance.withFloor(instance.version(), at, format));
            } catch (ZcodeException e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not record the version floor for ZCode instance " + id, e);
            }
        }
        AuditLog.log(AuditLog.Channel.WEB, "zcode.instance.launch", id,
                AuthFilter.selfDirected(request, ZcodeTui.BRAND, id) ? "self_launch" : "success",
                actorOf(request), request.getRemoteAddr());
        writeStatus(response, id, status);
    }

    private void stop(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        instanceTerminal.close(ZcodeTui.BRAND, id);
        ZcodeRuntime.stop(id);
        AuditLog.log(AuditLog.Channel.WEB, "zcode.instance.stop", id,
                AuthFilter.selfDirected(request, ZcodeTui.BRAND, id) ? "self_stop" : "success",
                actorOf(request), request.getRemoteAddr());
        writeStatus(response, id, ZcodeRuntime.status(id));
    }

    // --------------------------------------------------------------- terminal --

    /// `POST /api/zcode/instances/{id}/terminal` opens the instance's native
    /// TUI (`node bin/zcode.mjs`, no `--web`); `DELETE` stops it.
    ///
    /// Same gates, same takeover rule and same wire body as the dsh and brand
    /// terminals: [InstanceTerminal] owns them so the three surfaces cannot
    /// drift apart.
    private void terminal(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        if ("DELETE".equals(request.getMethod())) {
            JsonObject body = new JsonObject();
            body.addProperty("ok", true);
            body.addProperty("killed", instanceTerminal.close(ZcodeTui.BRAND, id));
            Json.write(response, body);
            return;
        }
        try {
            instanceTerminal.authorize(request);
            Path packageDir = packageDir(instance);
            if (!Files.isRegularFile(packageDir.resolve("bin/zcode.mjs"))) {
                throw new InstanceTerminal.Refused(409,
                        "terminal_failed: no ZCode distribution was found at " + packageDir
                                + " (build or place one with bin/zcode.mjs)");
            }
            JsonObject body = instanceTerminal.open(request, id,
                    () -> ZcodeRuntime.stop(id),
                    resume -> ZcodeTui.target(packageDir, manager.dataDirectory(instance),
                            manager.prepareHome(instance), manager.workspaceDirectory(instance),
                            id, config.zcode.nodePath));
            Json.write(response, body);
        } catch (InstanceTerminal.Refused e) {
            InstanceTerminal.write(request, response, e);
        }
    }

    /// The node executable: `zcode.nodePath` when it was set, else the system's
    /// `node`, else the runtime the panel manages.
    ///
    /// @return a node that can actually be started
    /// @throws IOException when there is none
    private Path nodeExecutable() throws IOException {
        return ZcodeTui.resolveNode(config.zcode.nodePath);
    }

    // ------------------------------------------------------------------ open --

    private void open(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        if (!ZcodeRuntime.isRunning(id)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the ZCode instance is not running");
            return;
        }
        // The instance binds loopback and is reached through the panel's own
        // mount — same origin, same certificate, same session gate as dsh.
        JsonObject body = new JsonObject();
        // Browser-consumed URL: carries the mount base (see BrandApiServlet).
        body.addProperty("url", config.basePath + "/i/" + id + "/");
        Json.write(response, body);
    }

    // ------------------------------------------------------------------ logs --

    private void logs(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        ZcodeInstance instance = manager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        int tail = DEFAULT_LOG_TAIL;
        String parameter = request.getParameter("tail");
        if (parameter != null) {
            try {
                tail = Math.min(MAX_LOG_TAIL, Math.max(1, Integer.parseInt(parameter.trim())));
            } catch (NumberFormatException ignored) {
                // The default stands.
            }
        }
        List<String> lines = manager.tailLog(instance, tail);
        JsonArray array = new JsonArray();
        lines.forEach(array::add);
        JsonObject body = new JsonObject();
        body.add("lines", array);
        Json.write(response, body);
    }

    // --------------------------------------------------------------- plugins --

    /// The per-instance plugin channel, the same shape the brand categories
    /// answer: `{supported, plugins[]}`.
    ///
    /// ZCode has no plugin concept, so `supported` is `false` and the list is
    /// empty. The route exists so the panel's plugin dialog is data-driven from
    /// day one: this is the only place that decides, and the UI already renders
    /// whatever it returns.
    private void plugins(HttpServletResponse response, String id) throws IOException {
        if (manager.find(id) == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such ZCode instance");
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("supported", false);
        body.add("plugins", new JsonArray());
        Json.write(response, body);
    }

    // ----------------------------------------------------------------- build --

    private void buildStatus(HttpServletResponse response) throws IOException {
        Json.writePreservingNulls(response, buildJson(ZcodeBuilder.status()));
    }

    private void startBuild(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZcodeException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String version = stringField(body, "version");
        ZcodeBuilder.Status started = ZcodeBuilder.start(settingsFor(version), version);
        Json.writePreservingNulls(response, HttpServletResponse.SC_ACCEPTED, buildJson(started));
    }

    private void buildLog(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int tail = DEFAULT_BUILD_LOG_TAIL;
        String parameter = request.getParameter("tail");
        if (parameter != null) {
            try {
                tail = Math.min(MAX_BUILD_LOG_TAIL, Math.max(1, Integer.parseInt(parameter.trim())));
            } catch (NumberFormatException ignored) {
                // The default stands.
            }
        }
        JsonArray array = new JsonArray();
        ZcodeBuilder.tailLog(tail).forEach(array::add);
        JsonObject body = new JsonObject();
        body.add("lines", array);
        Json.write(response, body);
    }

    /// The build settings for a ref, with the source template pointed at
    /// `refs/heads` when the ref is a branch rather than a version tag.
    private ZcodeBuilder.Settings settingsFor(@Nullable String version) {
        String ref = version == null ? "" : version.trim();
        String template = config.zcode.sourceUrl;
        if (!ref.isEmpty() && !ref.matches("v?\\d[\\w.\\-]*") && template.contains("/refs/tags/")) {
            template = template.replace("/refs/tags/", "/refs/heads/");
        }
        return new ZcodeBuilder.Settings(buildSettings.root(), buildSettings.buildBin(),
                buildSettings.pnpm(), template, buildSettings.keepSources());
    }

    private static JsonObject buildJson(ZcodeBuilder.Status status) {
        JsonObject body = new JsonObject();
        body.addProperty("state", status.state().name().toLowerCase(java.util.Locale.ROOT));
        body.addProperty("version", status.version());
        body.addProperty("message", status.message());
        body.addProperty("fraction", status.fraction());
        if (status.error() != null) {
            body.addProperty("error", status.error());
        }
        return body;
    }

    // --------------------------------------------------------------- response --

    /// The wire shape of one instance: the manifest fields minus the secret,
    /// plus runtime state. `token` travels to the panel's own admin — and only
    /// to a **browser**: `read` lets an agent see that an instance runs, but the
    /// token opens it, so the field is dropped for a bearer request. The
    /// session-gated assumption this comment used to make stopped holding when
    /// agent grants were added.
    private JsonObject instanceJson(ZcodeInstance instance, HttpServletRequest request) {
        ZcodeRuntime.Status status = ZcodeRuntime.status(instance.id());
        JsonObject body = new JsonObject();
        body.addProperty("id", instance.id());
        body.addProperty("name", instance.name());
        body.addProperty("workspacePath", instance.workspacePath());
        body.add("baseUrl", instance.baseUrl() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(instance.baseUrl()));
        body.addProperty("hasApiKey", instance.apiKey() != null && !instance.apiKey().isBlank());
        if (!AuthFilter.isAgent(request)) {
            body.addProperty("token", instance.token());
        }
        body.addProperty("lastPort", instance.lastPort());
        body.add("version", instance.version() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(instance.version()));
        body.add("minVersion", instance.minVersion() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(instance.minVersion()));
        body.addProperty("minVersionAt", instance.minVersionAt());
        String effectiveVersion = instance.version() == null || instance.version().isBlank()
                ? currentVersion() : instance.version();
        int effectiveFormat = ZcodeCredentials.effectiveFormat(manager, instance);
        if (effectiveFormat == 0 && effectiveVersion != null) {
            effectiveFormat = ZcodeCatalog.formatOf(effectiveVersion);
        }
        body.addProperty("credentialFormat", effectiveFormat);
        body.addProperty("requiresCredentialSecret",
                effectiveVersion != null && ZcodeCatalog.formatOf(effectiveVersion) >= 2);
        String fingerprint = ZcodeCredentials.fingerprint(manager, instance);
        body.add("keyFingerprint", fingerprint == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(fingerprint));
        body.addProperty("createdAt", instance.createdAt());
        body.addProperty("state", status.state().name().toLowerCase(java.util.Locale.ROOT));
        if (status.error() != null) {
            body.addProperty("error", status.error());
        }
        return body;
    }

    private static void writeStatus(HttpServletResponse response, String id, ZcodeRuntime.Status status)
            throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("state", status.state().name().toLowerCase(java.util.Locale.ROOT));
        if (status.error() != null) {
            body.addProperty("error", status.error());
        }
        Json.writePreservingNulls(response, body);
    }

    private static String[] split(@Nullable String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            return new String[0];
        }
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return trimmed.split("/");
    }

    private static @Nullable JsonObject body(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            JsonObject object = JsonParser
                    .parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (object.isJsonNull()) {
                throw new JsonParseException("not an object");
            }
            return object;
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return null;
        }
    }

    private static @Nullable String stringField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : null;
    }
}
