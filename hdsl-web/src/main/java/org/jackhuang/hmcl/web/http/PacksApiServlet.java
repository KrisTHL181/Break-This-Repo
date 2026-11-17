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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshBuildScripts;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshPackInstaller;
import org.jackhuang.hmcl.dsh.DshPackMarket;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/// The modpack REST surface, mapped at `/api/packs/*`:
///
/// - `GET  /api/packs/market`       — the community index, kept in memory for
///   ten minutes; `?refresh=1` forces a fetch. A failed fetch is a 502: the
///   index lives upstream, and this server is its gateway.
/// - `GET  /api/packs/market/{id}`  — one pack's detail: every version the
///   index lists, plus its manifest and README fetched through
///   [DshPackMarket#document], which refuses an error page dressed as a 200.
/// - `POST /api/packs/install`      — `{packId, version?, name?}`, 202
///   `{taskId}`: download (length + SHA-256 checked by [DshPackInstaller]),
///   identify, install into a new instance. The done task carries
///   `result: {instanceId}` and an `instance-created` event is broadcast.
/// - `POST /api/packs/upload`       — a `.dspack` (or the format's older `.tgz`)
///   upload (multipart field `file`, at most 200 MiB; optional field
///   `name`), 202 `{taskId}`, same install flow with the staged upload as
///   the source.
///
/// The install flow mirrors the desktop's ([DshPackInstaller#installNew] for
/// the first attempt, [DshPackInstaller#finish] for the retry after a
/// build-script answer), so the approval dance parks the task exactly like a
/// plugin install's does, and a failed attempt leaves no half-made instance
/// behind. Whole-home (`dshhome`) packs are refused: their overrides belong
/// at a home root, and the domain's install-into-an-instance flow is about a
/// profile.
@NotNullByDefault
public final class PacksApiServlet extends HttpServlet {

    /// How long a fetched index is served without asking upstream again.
    private static final long CACHE_TTL_MILLIS = java.time.Duration.ofMinutes(10).toMillis();

    private final InstanceRuntime runtime;
    private final TaskService tasks;
    private final ServerConfig config;

    private final Object cacheLock = new Object();
    private @Nullable DshPackMarket.Index cached;
    private long cachedAtMillis;

    public PacksApiServlet(InstanceRuntime runtime, TaskService tasks, ServerConfig config) {
        this.runtime = runtime;
        this.tasks = tasks;
        this.config = config;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] segments = split(request.getPathInfo());
        try {
            if (segments.length == 1 && "market".equals(segments[0])) {
                if ("GET".equals(request.getMethod())) {
                    market(request, response);
                } else {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            if (segments.length == 2 && "market".equals(segments[0])) {
                if ("GET".equals(request.getMethod())) {
                    marketDetail(response, segments[1]);
                } else {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            if (segments.length == 1 && "install".equals(segments[0])) {
                if ("POST".equals(request.getMethod())) {
                    install(request, response);
                } else {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            if (segments.length == 1 && "upload".equals(segments[0])) {
                if ("POST".equals(request.getMethod())) {
                    upload(request, response);
                } else {
                    Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // ----------------------------------------------------------------- market --

    private void market(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean refresh = "1".equals(request.getParameter("refresh"))
                || "true".equals(request.getParameter("refresh"));
        DshPackMarket.Index index;
        try {
            index = index(refresh);
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
            return;
        }

        JsonArray packs = new JsonArray();
        for (DshPackMarket.Entry entry : index.entries()) {
            packs.add(entryJson(entry));
        }
        JsonObject body = new JsonObject();
        body.add("packs", packs);
        if (index.generatedAt() != null) {
            body.addProperty("generatedAt", index.generatedAt());
        }
        Json.write(response, body);
    }

    /// One pack's detail: every version the index carries under its id, with
    /// the manifest and README the market publishes for it. The id is the
    /// index's own `<owner>.<repo>` spelling — used as it stands, never taken
    /// apart, for the reason [DshPackMarket] gives.
    private void marketDetail(HttpServletResponse response, String id) throws IOException {
        DshPackMarket.Index index;
        try {
            index = index(false);
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
            return;
        }
        DshPackMarket.Entry latest = null;
        JsonArray versions = new JsonArray();
        for (DshPackMarket.Entry entry : index.entries()) {
            if (!entry.id().equals(id)) {
                continue;
            }
            if (latest == null) {
                latest = entry;
            }
            JsonObject version = new JsonObject();
            version.addProperty("version", entry.version());
            version.addProperty("downloadUrl", entry.downloadUrl());
            version.addProperty("sha256", entry.sha256());
            version.addProperty("sizeBytes", entry.size());
            if (entry.updatedAt() != null) {
                version.addProperty("updatedAt", entry.updatedAt());
            }
            if (entry.dshVersion() != null) {
                version.addProperty("dshVersion", entry.dshVersion());
            }
            if (!entry.dshVersions().isEmpty()) {
                JsonArray measured = new JsonArray();
                entry.dshVersions().forEach(measured::add);
                version.add("dshVersions", measured);
            }
            if (entry.manifestVersion() != null) {
                version.addProperty("manifestVersion", entry.manifestVersion());
            }
            if (entry.bundleCount() != null) {
                version.addProperty("bundleCount", entry.bundleCount());
            }
            if (entry.depCount() != null) {
                version.addProperty("depCount", entry.depCount());
            }
            versions.add(version);
        }
        if (latest == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "pack not found");
            return;
        }

        JsonObject pack = entryJson(latest);
        pack.add("versions", versions);
        String manifest = DshPackMarket.document(DshPackMarket.manifestUrl(latest));
        if (manifest != null) {
            try {
                pack.add("manifest", JsonParser.parseString(manifest));
            } catch (JsonParseException | IllegalStateException e) {
                pack.addProperty("manifestText", manifest);
            }
        }
        String readme = DshPackMarket.document(DshPackMarket.readmeUrl(latest));
        if (readme != null) {
            pack.addProperty("readme", readme);
        }
        JsonObject body = new JsonObject();
        body.add("pack", pack);
        Json.write(response, body);
    }

    /// The index, served from memory inside the TTL unless a refresh is
    /// forced. [DshPackMarket#fetch] keeps its own copy on disk and falls
    /// back to it, so a failure here means there was nothing anywhere.
    private DshPackMarket.Index index(boolean refresh) throws DshException {
        synchronized (cacheLock) {
            long now = System.currentTimeMillis();
            if (!refresh && cached != null && now - cachedAtMillis < CACHE_TTL_MILLIS) {
                return cached;
            }
            DshPackMarket.Index fetched = DshPackMarket.fetch();
            cached = fetched;
            cachedAtMillis = now;
            return fetched;
        }
    }

    /// The JSON of one index entry: the contract fields, plus the honest
    /// extras the index really carries (`owner`, `dshVersion`, `sizeBytes`,
    /// `type`, `category`). The index has no download counter and no
    /// per-language description, so `downloads`/`descriptionZh` are absent
    /// rather than invented.
    private static JsonObject entryJson(DshPackMarket.Entry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("id", entry.id());
        json.addProperty("name", entry.title());
        if (entry.author() != null) {
            json.addProperty("author", entry.author());
        }
        if (entry.description() != null) {
            json.addProperty("description", entry.description());
        }
        json.addProperty("version", entry.version());
        if (entry.iconUrl() != null) {
            json.addProperty("avatarUrl", entry.iconUrl());
        }
        if (entry.updatedAt() != null) {
            json.addProperty("updatedAt", entry.updatedAt());
        }
        json.addProperty("owner", entry.owner());
        json.addProperty("sizeBytes", entry.size());
        if (entry.dshVersion() != null) {
            json.addProperty("dshVersion", entry.dshVersion());
        }
        if (!entry.dshVersions().isEmpty()) {
            JsonArray versions = new JsonArray();
            entry.dshVersions().forEach(versions::add);
            json.add("dshVersions", versions);
        }
        if (entry.launcherRestricted()) {
            // The index's derived flag: the pack declares which launchers it supports, and which
            // ones are left to the lazy manifest. Shown because a pack that names launchers is one
            // somebody may want to look at more closely before installing.
            json.addProperty("launcherRestricted", true);
        }
        if (entry.type() != null) {
            json.addProperty("type", entry.type());
        }
        if (entry.category() != null) {
            json.addProperty("category", entry.category());
        }
        return json;
    }

    // ---------------------------------------------------------------- install --

    /// Installs a pack from the market: the entry is looked up in the (cached)
    /// index, and the task downloads, verifies and installs it.
    private void install(HttpServletRequest request, HttpServletResponse response)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String packId = stringField(body, "packId");
        if (packId == null || packId.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "packId is required");
            return;
        }
        String wantedVersion = stringField(body, "version");
        String name = stringField(body, "name");

        DshPackMarket.Index index;
        try {
            index = index(false);
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
            return;
        }
        DshPackMarket.Entry entry = null;
        for (DshPackMarket.Entry candidate : index.entries()) {
            if (candidate.id().equals(packId)
                    && (wantedVersion == null || wantedVersion.isBlank()
                    || wantedVersion.equals(candidate.version()))) {
                entry = candidate;
                break;
            }
        }
        if (entry == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND,
                    wantedVersion == null || wantedVersion.isBlank()
                            ? "pack not found: " + packId
                            : "pack not found: " + packId + " version " + wantedVersion);
            return;
        }
        if (entry.isWholeHome()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "whole-home (dshhome) packs are not supported through the web API");
            return;
        }

        DshPackMarket.Entry chosen = entry;
        String id = InstancesApiServlet.uniqueId(
                name != null && !name.isBlank() ? name
                        : chosen.name() != null && !chosen.name().isBlank() ? chosen.name() : chosen.id());
        submitInstall(response, id, report -> {
            report.accept("Downloading " + chosen.downloadUrl());
            Path archive = DshPackInstaller.download(chosen, report);
            report.accept("The pack matches the market's size and SHA-256");
            return archive;
        }, chosen.profileName() == null ? "pack" : chosen.profileName(), chosen.dshVersion(), null);
    }

    /// Installs an uploaded pack. The upload is staged first, so a file
    /// that is not a pack is a 400 with nothing left behind, and the task
    /// installs from the staged copy (which it removes either way).
    private void upload(HttpServletRequest request, HttpServletResponse response)
            throws IOException, DshException {
        jakarta.servlet.http.Part part;
        try {
            part = request.getPart("file");
        } catch (jakarta.servlet.ServletException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "expected a multipart form with a `file` field: " + e.getMessage());
            return;
        }
        if (part == null || part.getSize() == 0) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a `file` field is required");
            return;
        }
        String submitted = part.getSubmittedFileName() == null ? "pack.dspack" : part.getSubmittedFileName();
        String extension = submitted.toLowerCase(Locale.ROOT);
        if (!extension.endsWith(".dspack") && !extension.endsWith(".tgz")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    submitted + " is not a DeepSeek Harness pack (.dspack, or .tgz for one made with"
                            + " the format's first container)");
            return;
        }

        Path staging = config.dataDir.resolve("tmp").resolve(
                "pack-" + java.util.UUID.randomUUID() + ".dspack");
        DshPackInstaller.Container container;
        try {
            Files.createDirectories(staging.getParent());
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, staging);
            }
            container = DshPackInstaller.identify(staging);
        } catch (DshException e) {
            Files.deleteIfExists(staging);
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        if (container.wholeHome()) {
            Files.deleteIfExists(staging);
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "whole-home (dshhome) packs are not supported through the web API");
            return;
        }

        String name = request.getParameter("name");
        String base = name != null && !name.isBlank() ? name
                : firstNonBlank(container.text("name"), container.text("displayName"), "pack");
        String id = InstancesApiServlet.uniqueId(base);
        submitInstall(response, id, report -> staging, "pack", null, staging);
    }

    /// What an install task fetches before it installs: a download for a
    /// market pack, the already-staged file for an upload.
    @FunctionalInterface
    private interface ArchiveSource {
        Path fetch(java.util.function.Consumer<String> report) throws DshException;
    }

    /// Submits the pack-install task both entry points share. The instance id
    /// is computed once, here, before the submission: a retry after a
    /// build-script answer must finish the instance the question was written
    /// into, and an id computed again would be a second instance.
    ///
    /// @param stagedToRemove an upload's staging file, removed when the task
    ///                       is done with it; `null` for a market pack, whose
    ///                       archive is the cache's own
    private void submitInstall(HttpServletResponse response, String id, ArchiveSource source,
                               String profileFallback, @Nullable String versionFallback,
                               @Nullable Path stagedToRemove) throws IOException {
        try {
            TaskService.Task task = tasks.submit("pack-install", id, live -> {
                java.util.function.Consumer<String> report = line -> live.update(line, -1);
                // A build-script question keeps the staging file: the retry
                // after its answer installs from it again. Every other exit —
                // success, failure, a cancel while parked — removes it.
                boolean parked = false;
                try {
                    Path archive = source.fetch(report);
                    DshInstance installed = installPack(archive, id, profileFallback, versionFallback, report);
                    runtime.announceCreated(installed);
                    JsonObject result = new JsonObject();
                    result.addProperty("instanceId", installed.id());
                    live.attachResult(result);
                    return installedMessage(archive, installed.id());
                } catch (org.jackhuang.hmcl.dsh.DshPluginInstaller.DshBuildScriptApprovalRequired waiting) {
                    parked = true;
                    throw waiting;
                } finally {
                    if (stagedToRemove != null && !parked) {
                        Files.deleteIfExists(stagedToRemove);
                    }
                }
            }, (allow, keys) -> {
                DshInstance instance = DshInstanceManager.find(id);
                if (instance == null) {
                    throw new DshException("instance " + id + " is gone");
                }
                DshBuildScripts.answer(instance, keys, allow);
            });
            JsonObject accepted = new JsonObject();
            accepted.addProperty("taskId", task.id());
            Json.write(response, HttpServletResponse.SC_ACCEPTED, accepted);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    /// The done message of an install, carrying what the pack's launcher
    /// declaration had to say.
    ///
    /// A declaration this launcher does not satisfy is a warning and never a
    /// refusal — the format is explicit about that — and the two places it has
    /// to show up are the task's log, which the install writes, and the task's
    /// own message, which is what remains of the import once it is done.
    private static String installedMessage(Path archive, String instanceId) {
        String message = "Installed the pack into " + instanceId;
        try {
            java.util.List<String> warnings = DshPackInstaller.launcherWarnings(
                    DshPackInstaller.identify(archive).manifest());
            return warnings.isEmpty() ? message : message + " " + String.join(" ", warnings);
        } catch (DshException | RuntimeException e) {
            // The pack installed, so the declaration cannot be read for some reason of its own;
            // saying the install worked is still true and is what this is for.
            return message;
        }
    }

    /// The domain's install sequence, exactly as the desktop runs it: the
    /// container is recognised before anything is created, the first attempt
    /// makes the instance, and a retry after a build-script answer finishes
    /// the instance the question belongs to.
    private static DshInstance installPack(Path archive, String id, String profileFallback,
                                           @Nullable String versionFallback,
                                           java.util.function.Consumer<String> report) throws DshException {
        DshPackInstaller.Container container = DshPackInstaller.identify(archive);
        report.accept("Container version " + container.containerVersion()
                + ", manifest version " + container.manifestVersion());
        if (container.wholeHome()) {
            throw new DshException("whole-home (dshhome) packs are not supported through the web API");
        }
        String profile = container.profileName(profileFallback);
        // The specification's decision rather than the pack's pin alone: a pack that lists the
        // versions it was measured against is installed with one of them when this machine has one,
        // and the pinned version is what is installed when it does not — never a refusal for the
        // pin's own absence.
        DshPackInstaller.Decision decision = DshPackInstaller.decide(container.manifest());
        String version = decision.version();
        if (version == null || version.isBlank()) {
            version = container.dshVersion();
        }
        if (version == null || version.isBlank()) {
            version = versionFallback;
        }
        if (version == null || version.isBlank()) {
            throw new DshException("The pack does not say which DeepSeek Harness version it "
                    + "needs, and one must be installed before it can be", null);
        }

        DshInstance existing = DshInstanceManager.find(id);
        if (existing == null) {
            return DshPackInstaller.installNew(archive, id, profile, version, report);
        }
        report.accept("Finishing " + id + " now that the install scripts are answered");
        return DshPackInstaller.finish(archive, existing, report);
    }

    // ---------------------------------------------------------------- helpers --

    private static String firstNonBlank(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "pack";
    }

    /// Splits the path after `/api/packs` into segments.
    private static String[] split(@Nullable String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            return new String[0];
        }
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return trimmed.split("/");
    }

    /// Reads the request body as a JSON object, answering 400 itself when it
    /// is not one.
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
