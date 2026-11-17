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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.NodeChannels;
import org.jackhuang.hmcl.dsh.NodeRelease;
import org.jackhuang.hmcl.dsh.NodeRuntime;
import org.jackhuang.hmcl.dsh.NodeRuntimeManager;
import org.jackhuang.hmcl.dsh.NodeSelection;
import org.jackhuang.hmcl.dsh.NodeSource;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.node.NodeEnvironment;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// The Node.js toolchain page: the system probe, the panel-managed runtimes,
/// the major-line cards, and the consumers of each runtime.
///
/// - `GET  /api/node`           — `{system, requirement, default, managed[],
///                                channels[], runtimes[], consumers[]}`
/// - `GET  /api/node/resolve`   — `?selector=auto|22|22.23.1&brand=kimi`
///                                → `{version, source, compatible, reason}`
/// - `POST /api/node/install`   — `{version?, source?}` → `{taskId}`
/// - `POST /api/node/uninstall` — `{version}` → `{ok}`; refused while in use
/// - `POST /api/node/default`   — `{selection}` — the launcher-wide default
/// - `POST /api/node/disable`   — `{version, disabled}` — excluded from auto
///
/// Managed runtimes live under `<data>/hdsl/runtimes`; `state.json` beside them
/// records the disabled set and the last use. The distribution directories
/// remain the source of truth for what is installed.
@NotNullByDefault
public final class NodeApiServlet extends HttpServlet {

    private final TaskService tasks;
    private final NodeEnvironment environment;

    public NodeApiServlet(TaskService tasks, ServerConfig config) {
        this.tasks = tasks;
        this.environment = new NodeEnvironment(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if ("/resolve".equals(path)) {
            resolve(request, response);
            return;
        }
        JsonObject body = status();
        JsonObject view = environment.toJson();
        body.addProperty("default", view.get("default").getAsString());
        body.add("channels", view.get("channels"));
        body.add("runtimes", view.get("runtimes"));
        body.add("consumers", view.get("consumers"));
        Json.writePreservingNulls(response, body);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/") || path.equals("/install")) {
            install(request, response);
            return;
        }
        if (path.equals("/uninstall")) {
            uninstall(request, response);
            return;
        }
        if (path.equals("/default")) {
            setDefault(request, response);
            return;
        }
        if (path.equals("/disable")) {
            disable(request, response);
            return;
        }
        if (path.equals("/channels/update")) {
            updateChannel(request, response);
            return;
        }
        if (path.equals("/revert")) {
            revert(request, response);
            return;
        }
        if (path.equals("/gc")) {
            gc(request, response);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "unknown endpoint");
    }

    /// The pre-flight the dropdown calls before it applies a selector.
    private void resolve(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String selector = request.getParameter("selector");
        if (selector == null || selector.isBlank()) {
            selector = "auto";
        }
        NodeSelection selection = NodeSelection.parse(selector);
        if (selection.kind() == NodeSelection.Kind.INVALID) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "unknown_selector: " + selector);
            return;
        }
        NodeSelection.Resolution resolution = NodeSelection.resolve(
                selection, environment.fallbackFor(request.getParameter("brand")), false);
        JsonObject body = new JsonObject();
        body.addProperty("selector", selector);
        if (resolution.version() == null) {
            body.add("version", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("version", resolution.version());
        }
        body.addProperty("source", resolution.source().name().toLowerCase(Locale.ROOT));
        body.addProperty("compatible", resolution.compatible());
        body.addProperty("reason", resolution.reason());
        Json.writePreservingNulls(response, body);
    }

    /// Writes the launcher-wide default selector.
    private void setDefault(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String selection = stringField(body, "selection");
        if (selection == null || selection.isBlank()) {
            selection = "auto";
        }
        if (NodeSelection.parse(selection).kind() == NodeSelection.Kind.INVALID) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "unknown_selector: " + selection);
            return;
        }
        SettingsManager.settings().setDefaultNodeRuntime(selection);
        SettingsManager.save();
        AuditLog.log(AuditLog.Channel.WEB, "node.default", selection, "success",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, Map.of("ok", true, "default", selection));
    }

    /// Removes a version from automatic resolution. It never stops a running
    /// consumer; the UI marks those instead.
    private void disable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String version = stringField(body, "version");
        if (version == null || version.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "version is required");
            return;
        }
        if (NodeRuntimeManager.findInstalled(version) == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND,
                    "Node.js " + version + " is not installed");
            return;
        }
        boolean disabled = body != null && body.has("disabled")
                && body.get("disabled").isJsonPrimitive() && body.get("disabled").getAsBoolean();
        try {
            NodeRuntimeManager.setDisabled(version, disabled);
        } catch (org.jackhuang.hmcl.dsh.DshException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, disabled ? "node.disable" : "node.enable",
                version, "success", actorOf(request), request.getRemoteAddr());
        Json.write(response, Map.of("ok", true, "version", version, "disabled", disabled));
    }

    /// Moves a channel (`22`, `26`, `lts`, `current`) to the newest published
    /// release on its line. The pointer is recorded only after the version is
    /// installed and health-checked; an already-installed version just moves it.
    private void updateChannel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String raw = stringField(body, "name");
        if (raw == null || raw.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "name is required");
            return;
        }
        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (!name.matches("\\d+|lts|current")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown_channel: " + raw);
            return;
        }
        NodeSource source = body != null && body.has("source")
                ? NodeSource.of(stringField(body, "source"))
                : NodeSource.MIRROR;

        String picked;
        try {
            picked = pickForChannel(NodeRuntimeManager.fetchReleases(source), name);
        } catch (org.jackhuang.hmcl.dsh.DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
            return;
        }
        if (picked == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND,
                    "no_release: no stable Node.js " + name + " release is published");
            return;
        }
        String version = picked;
        if (NodeRuntimeManager.findInstalled(version) != null) {
            try {
                NodeChannels.record(name, version);
            } catch (org.jackhuang.hmcl.dsh.DshException e) {
                Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
            AuditLog.log(AuditLog.Channel.WEB, "node.channel", name + "=" + version, "success",
                    actorOf(request), request.getRemoteAddr());
            Json.write(response, Map.of("ok", true, "channel", name, "version", version));
            return;
        }

        TaskService.Task task = tasks.submit("node-channel-update", null, live -> {
            live.update("Installing Node.js " + version, -1);
            NodeRuntime installed = NodeRuntimeManager.install(version, source,
                    stage -> live.update(stage, -1));
            NodeChannels.record(name, installed.version());
            return "Updated Node.js " + name + " to " + installed.version();
        });
        AuditLog.log(AuditLog.Channel.WEB, "node.channel", name + "=" + version, "submitted",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, Map.of("taskId", task.id(), "channel", name, "version", version));
    }

    /// The newest stable release a channel names. `lts` is the even-major line,
    /// `current` every line, a number is that major.
    static @Nullable String pickForChannel(List<NodeRelease> releases, String channel) {
        for (NodeRelease release : releases) {
            String version = release.version();
            if (isPrerelease(version)) {
                continue;
            }
            int major = majorOf(version);
            if ("current".equals(channel)) {
                return version;
            }
            if ("lts".equals(channel)) {
                if (major % 2 == 0) {
                    return version;
                }
                continue;
            }
            if (String.valueOf(major).equals(channel)) {
                return version;
            }
        }
        return null;
    }

    private static int majorOf(String version) {
        try {
            return Integer.parseInt(version.split("\\.")[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// Switches a consumer back to the last runtime it came up on.
    private void revert(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String kind = stringField(body, "kind");
        String id = stringField(body, "id");
        String brand = stringField(body, "brand");
        if (!"instance".equals(kind) && !"brand-instance".equals(kind)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "kind must be instance or brand-instance");
            return;
        }
        if (id == null || id.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "id is required");
            return;
        }
        if ("brand-instance".equals(kind) && (brand == null || brand.isBlank())) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "brand is required");
            return;
        }
        try {
            String version = environment.revert(kind, brand, id);
            if (version == null) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND,
                        "no_last_good: this consumer has no recorded successful runtime");
                return;
            }
            AuditLog.log(AuditLog.Channel.WEB, "node.revert",
                    kind + "/" + (brand == null ? "" : brand + "/") + id,
                    "success", actorOf(request), request.getRemoteAddr());
            Json.write(response, Map.of("ok", true, "nodeRuntime", version));
        } catch (org.jackhuang.hmcl.dsh.DshException
                 | org.jackhuang.hmcl.web.brand.BrandException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /// Deletes unused runtimes beyond `keepPerMajor` per major.
    private void gc(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        int keep = 2;
        if (body != null && body.has("keepPerMajor") && body.get("keepPerMajor").isJsonPrimitive()) {
            // A `keepPerMajor` that is not a number is not worth a 500: the
            // endpoint reports what it removed, and 2 is the conservative floor.
            try {
                keep = body.get("keepPerMajor").getAsInt();
            } catch (RuntimeException e) {
                keep = 2;
            }
        }
        List<String> removed = environment.gc(keep);
        AuditLog.log(AuditLog.Channel.WEB, "node.gc",
                removed.isEmpty() ? "nothing" : String.join(",", removed),
                "success", actorOf(request), request.getRemoteAddr());
        Json.write(response, Map.of("ok", true, "removed", removed, "keepPerMajor", Math.max(1, keep)));
    }

    private void uninstall(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String version = stringField(body, "version");
        if (version == null || version.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "version is required");
            return;
        }
        version = version.trim();
        List<NodeEnvironment.Consumer> consumers = environment.consumersOf(version);
        if (!consumers.isEmpty()) {
            JsonArray array = new JsonArray();
            for (NodeEnvironment.Consumer consumer : consumers) {
                JsonObject entry = new JsonObject();
                entry.addProperty("kind", consumer.kind());
                entry.addProperty("label", consumer.label());
                array.add(entry);
            }
            JsonObject refusal = new JsonObject();
            refusal.addProperty("error", "node_runtime_in_use: Node.js " + version
                    + " is used by " + consumers.size() + " consumer(s); switch them first");
            refusal.add("consumers", array);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            Json.write(response, refusal);
            return;
        }
        if (environment.isPinnedAsDefault(version)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "node_runtime_is_default: Node.js " + version
                            + " is the default for the panel or a brand; change that first");
            return;
        }
        if (environment.isLastGood(version)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "node_runtime_is_last_good: Node.js " + version
                            + " is a consumer's last successful runtime; switch that consumer first");
            return;
        }
        try {
            NodeRuntimeManager.uninstall(version);
        } catch (org.jackhuang.hmcl.dsh.DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, "node.uninstall", version, "success",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, Map.of("ok", true));
    }

    private void install(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = parseBody(request);
        String requested = stringField(body, "version");
        String version = requested == null ? null : requested.trim();
        // 面板用户群主要在国内：默认 npmmirror，官方源可作退路（索引查询本身双源互备）。
        NodeSource source = body != null && body.has("source")
                ? NodeSource.of(stringField(body, "source"))
                : NodeSource.MIRROR;

        // 进度回调里要用任务句柄：TaskWork 把本任务直接交给任务体，句柄在
        // 第一行进度到达时就一定存在（老的一格 holder 要在 submit 返回后才填上，
        // 那之前的进度行没有收件人）。
        TaskService.Task task = tasks.submit("node-install", null, live -> {
            String picked = version == null || version.isEmpty() ? newestSupported(source) : version;
            NodeRuntime installed = NodeRuntimeManager.install(picked, source,
                    stage -> live.update(stage, -1));
            return "Installed Node.js " + installed.version();
        });
        Json.write(response, Map.of("taskId", task.id()));
        AuditLog.log(AuditLog.Channel.WEB, "node.install",
                version == null || version.isEmpty() ? "latest" : version,
                "submitted", actorOf(request), request.getRemoteAddr());
    }

    /// The status half: the system probe and the flat `managed` list the old
    /// SPA still reads, plus the fields the new page needs.
    private JsonObject status() {
        JsonObject body = new JsonObject();
        DshNodeRuntime system = DshNodeRuntime.detect().orElse(null);
        JsonObject systemJson = new JsonObject();
        if (system != null) {
            systemJson.addProperty("node", system.node().toString());
            systemJson.addProperty("nodeVersion", system.nodeVersion());
            if (system.npm() != null) {
                systemJson.addProperty("npm", system.npm().toString());
                systemJson.addProperty("npmVersion", system.npmVersion());
            }
            if (system.pnpm() != null) {
                systemJson.addProperty("pnpm", system.pnpm().toString());
                systemJson.addProperty("pnpmVersion", system.pnpmVersion());
            }
        }
        body.add("system", system == null ? com.google.gson.JsonNull.INSTANCE : systemJson);

        List<NodeRuntime> managed = NodeRuntimeManager.listInstalled();
        JsonArray array = new JsonArray();
        for (NodeRuntime runtime : managed) {
            JsonObject entry = new JsonObject();
            entry.addProperty("version", runtime.version());
            entry.addProperty("suitable", suitable(runtime));
            entry.addProperty("node", runtime.node().toString());
            entry.addProperty("pnpm", java.nio.file.Files.isRegularFile(runtime.pnpm()));
            array.add(entry);
        }
        body.add("managed", array);
        body.addProperty("requirement", DshNodeRuntime.requirement());
        return body;
    }

    /// Reads the JSON body, tolerating an empty one.
    private static @Nullable JsonObject parseBody(HttpServletRequest request) throws IOException {
        String raw = request.getReader().lines().collect(java.util.stream.Collectors.joining());
        return raw.isBlank() ? null : Json.GSON.fromJson(raw, JsonObject.class);
    }

    private static @Nullable String stringField(@Nullable JsonObject body, String name) {
        return body != null && body.has(name) && body.get(name).isJsonPrimitive()
                ? body.get(name).getAsString()
                : null;
    }

    /// The session's username for the audit trail, or null when the request
    /// carries no session (auth disabled, or a hand the filter never saw).
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// The newest published release the harness supports, from the release
    /// index (tries the chosen source, then the other).
    private static String newestSupported(NodeSource source) throws Exception {
        return pickNewestSupported(NodeRuntimeManager.fetchReleases(source));
    }

    /// Whether a managed runtime is one this panel can actually use.
    ///
    /// It used to mean "the version is inside the supported range", which is how
    /// a runtime whose binary cannot start — Node's official builds link
    /// `libatomic.so.1`, and Debian's slim images do not ship it — was
    /// advertised as `suitable: true` while every install on it exited 127.
    /// The answer is cached inside [NodeRuntimeManager#runs].
    ///
    /// @param runtime a managed runtime
    /// @return whether it is both supported and able to run
    static boolean suitable(NodeRuntime runtime) {
        return DshNodeRuntime.isSupportedNodeVersion(runtime.version())
                && NodeRuntimeManager.runs(runtime);
    }

    /// Picks the version one-click install should use.
    ///
    /// Pre-releases are skipped: `nodejs.org/dist` publishes them in the same
    /// index with the same shape (`27.0.0-alpha.0.0.0`), sorted above every
    /// stable line, and an alpha as the runtime behind every dsh instance is not
    /// something a one-click button should decide on its own. A caller that
    /// wants one can still pass `version` explicitly.
    ///
    /// @param releases the index as fetched, newest first
    /// @return the newest stable release the harness supports
    /// @throws IllegalStateException when the index carries none
    static String pickNewestSupported(List<NodeRelease> releases) {
        return releases.stream()
                .map(NodeRelease::version)
                .filter(version -> !isPrerelease(version))
                .filter(DshNodeRuntime::isSupportedNodeVersion)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "no supported Node.js release found (" + DshNodeRuntime.requirement() + ")"));
    }

    /// Reports whether a published version string is a pre-release.
    ///
    /// @param version the version without the leading `v`
    /// @return whether it carries a `-alpha` / `-rc` / `-nightly` suffix
    static boolean isPrerelease(String version) {
        return version.indexOf('-') >= 0;
    }
}
