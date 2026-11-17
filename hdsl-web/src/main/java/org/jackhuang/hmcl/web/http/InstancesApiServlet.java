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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshBuildScripts;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshInstanceSettings;
import org.jackhuang.hmcl.dsh.DshLocalPlugins;
import org.jackhuang.hmcl.dsh.DshPackForge;
import org.jackhuang.hmcl.dsh.DshPackInstaller;
import org.jackhuang.hmcl.dsh.DshPaths;
import org.jackhuang.hmcl.dsh.DshPluginInstaller;
import org.jackhuang.hmcl.dsh.DshPortMode;
import org.jackhuang.hmcl.dsh.DshPorts;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.dsh.DshTuiPlugins;
import org.jackhuang.hmcl.dsh.FirstPartyPlugins;
import org.jackhuang.hmcl.dsh.DshVersionManager;
import org.jackhuang.hmcl.setting.GameDirectory;
import org.jackhuang.hmcl.setting.GameDirectoryManager;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.terminal.DshTui;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalTarget;
import org.jackhuang.hmcl.web.terminal.InstanceTerminal;
import org.jackhuang.hmcl.web.terminal.TerminalTickets;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/// The instance half of the REST surface, mapped at `/api/instances/*`:
///
/// - `GET    /api/instances`            — list
/// - `POST   /api/instances`            — create `{name, version, homeMode?, portMode?, port?, gameDirectoryId?, autoInstall?}`
///   and, unless `autoInstall` is false, start installing the version right
///   away: the answer carries the instance and `installTaskId`. A launcher's
///   "new instance" is meant to be run, not to sit as an empty manifest.
/// - `GET    /api/instances/{id}`       — detail, including `installProgress` and `account`
/// - `PATCH  /api/instances/{id}`       — edit `{name?, homeMode?, portMode?, port?, autoPort?, account?}`
/// - `DELETE /api/instances/{id}`       — remove (refused while running)
/// - `POST   /api/instances/{id}/install` — install a version, 202 `{taskId}`
/// - `POST   /api/instances/{id}/launch`  — start, 202 `{state}`; an instance
///   that is not installed yet is installed first and launched when pnpm is
///   done (202 `{state: "installing", taskId}`), unless `autoInstall` is false
/// - `POST   /api/instances/{id}/stop`    — stop the process tree, 202 `{state}`
/// - `GET    /api/instances/{id}/logs`    — the retained output, `?tail=N`
/// - `GET    /api/instances/{id}/open`    — the running URL with its token
/// - `GET    /api/instances/{id}/plugins`         — the profile's dependencies and bundles
/// - `POST   /api/instances/{id}/plugins/install` — `{specs: [...]}`, 202 `{taskId}`
/// - `POST   /api/instances/{id}/plugins/remove`  — `{specs: [...]}`, 202 `{taskId}`
/// - `POST   /api/instances/{id}/plugins/local`   — a `.tgz` upload (multipart field
///   `file`, at most 50 MiB), 202 `{taskId}`
/// - `GET    /api/instances/{id}/tui-plugins`            — the curated terminal
///   interfaces with their installation state, read from **each one's own profile**
///   ([DshTuiPlugins]: a terminal plugin never shares a profile with the web app)
/// - `POST   /api/instances/{id}/tui-plugins/install`    — `{package: "@scope/name"}`,
///   202 `{taskId}`: installs the catalogue's pinned version into the profile the
///   catalogue names
/// - `POST   /api/instances/{id}/tui-plugins/remove`     — `{package}`, 202 `{taskId}`
/// - `POST   /api/instances/{id}/export`          — `{name?, includeSessions?}`, 202
///   `{taskId}`: a DSH-PackForge `.dspack` into `<HDSL_DATA>/exports/` (the
///   domain's credential filter decides what travels), the done task carries
///   `result: {filename}`
/// - `GET    /api/instances/{id}/sessions`        — the home's sessions, newest first
/// - `POST   /api/instances/{id}/sessions/export` — `{sessionIds: [...]}`, 202
///   `{taskId}`: a `.sspack` into `exports/`, `result: {filename}`
/// - `GET    /api/instances/{id}/workspaces`      — the sessions grouped the
///   harness's own way, by workspace slug
/// - `GET    /api/instances/{id}/skills`          — the home's skill packs
/// - `POST   /api/instances/{id}/skills/install`  — `{source}`, 202 `{taskId}`:
///   a GitHub `owner/repo` (optionally with a skill name) or a catalogue
///   query resolved through [org.jackhuang.hmcl.dsh.DshSkillSource]
/// - `DELETE /api/instances/{id}/skills/{skillId}` — remove one skill pack
///
/// Plugin installs that meet a build script they may not run park their task
/// in `waiting_approval`; `POST /api/tasks/{id}/approve` carries the decision.
///
/// Everything slow or stateful runs through [InstanceRuntime] on the task
/// pool; the handlers themselves only parse, validate and answer.
@NotNullByDefault
public final class InstancesApiServlet extends HttpServlet {

    private final InstanceRuntime runtime;
    private final TaskService tasks;
    private final ServerConfig config;
    private final TerminalManager terminals;
    private final TerminalTickets terminalTickets;
    private final AuthService auth;
    private final DeveloperAuth developerAuth;
    private final DeveloperElevation developerElevation;
    /// Live TLS state of the main connector; the terminal is HTTPS-only.
    private final java.util.function.BooleanSupplier tlsActive;
    private final InstanceTerminal instanceTerminal;

    public InstancesApiServlet(InstanceRuntime runtime, TaskService tasks, ServerConfig config,
                               TerminalManager terminals, TerminalTickets terminalTickets,
                               AuthService auth, DeveloperAuth developerAuth,
                               DeveloperElevation developerElevation,
                               java.util.function.BooleanSupplier tlsActive,
                               org.jackhuang.hmcl.web.emergency.EmergencyStop emergency) {
        this.runtime = runtime;
        this.tasks = tasks;
        this.config = config;
        this.terminals = terminals;
        this.terminalTickets = terminalTickets;
        this.auth = auth;
        this.developerAuth = developerAuth;
        this.developerElevation = developerElevation;
        this.tlsActive = tlsActive;
        this.instanceTerminal = new InstanceTerminal(config, terminals, terminalTickets, auth,
                developerAuth, developerElevation, tlsActive, emergency);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        String[] segments = split(pathInfo);
        try {
            if (segments.length == 0) {
                switch (request.getMethod()) {
                    case "GET" -> listInstances(request, response);
                    case "POST" -> createInstance(request, response);
                    default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            String id = segments[0];
            if (segments.length == 1) {
                switch (request.getMethod()) {
                    case "GET" -> getInstance(request, response, id);
                    case "PATCH" -> updateInstance(request, response, id);
                    case "DELETE" -> deleteInstance(request, response, id);
                    default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            switch (segments[1]) {
                case "install" -> install(request, response, id);
                case "launch" -> launch(request, response, id);
                case "stop" -> stop(request, response, id);
                case "terminal" -> terminal(request, response, id);
                case "reveal" -> reveal(request, response, id);
                case "logs" -> logs(request, response, id);
                case "open" -> open(response, id);
                case "plugins" -> plugins(request, response, id, segments);
                case "tui-plugins" -> tuiPlugins(request, response, id, segments);
                case "export" -> export(request, response, id);
                case "sessions" -> sessions(request, response, id, segments);
                case "workspaces" -> workspaces(request, response, id);
                case "skills" -> skills(request, response, id, segments);
                default -> Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            }
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ list --

    private void listInstances(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        for (DshInstance instance : DshInstanceManager.list()) {
            JsonObject json = runtime.toJson(instance);
            redactForAgent(request, json);
            array.add(json);
        }
        body.add("instances", array);
        Json.writePreservingNulls(response, body);
    }

    // ----------------------------------------------------------------- create --

    private void createInstance(HttpServletRequest request, HttpServletResponse response)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String name = stringField(body, "name");
        String version = stringField(body, "version");
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "name and version are required");
            return;
        }

        DshHomeMode homeMode = homeMode(body);
        if (homeMode == null) {
            homeMode = DshHomeMode.ISOLATED;
        }
        if (homeMode == DshHomeMode.CUSTOM) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "custom homes are not supported through the web API");
            return;
        }

        Path workspace = resolveDirectory(body).directory();

        String id = uniqueId(name);
        DshInstance instance = DshInstanceManager.create(id, version, DshInstance.DEFAULT_PROFILE,
                workspace, homeMode, null, List.of(), java.util.Map.of());
        AuditLog.log(AuditLog.Channel.WEB, "instance.create", id, "success",
                actorOf(request), request.getRemoteAddr());

        DshPortMode portMode = portMode(body, DshPortMode.AUTO);
        Integer port = intField(body, "port");
        if (portMode == DshPortMode.FIXED) {
            if (port == null || port <= 0) {
                port = instance.portOrDefault();
            }
            if (!applyFixedPort(instance, port, response)) {
                return;
            }
            instance = DshInstanceManager.find(id);
            if (instance == null) {
                // M11 (glm-5.2): the manifest did not survive creation — return
                // 500 instead of a silent empty response.
                Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "instance was not persisted");
                return;
            }
        }

        runtime.announceCreated(instance);

        JsonObject created = new JsonObject();
        created.add("instance", instanceBody(request, instance));
        // The launcher behaviour: "new instance" means "I want to run this", so
        // the install starts here instead of waiting for a second click on the
        // detail page. The instance already exists when this runs, so a failure
        // leaves it in place with the reason on its detail page.
        if (!Boolean.FALSE.equals(boolField(body, "autoInstall"))) {
            try {
                TaskService.Task task = submitInstall(instance, version, null);
                created.addProperty("installTaskId", task.id());
            } catch (TaskService.InstanceBusyException e) {
                // The instance is still created; only the head start is lost.
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not auto-install " + id + ": " + e.getMessage());
            }
        }
        Json.writePreservingNulls(response, HttpServletResponse.SC_CREATED, created);
    }

    /// Applies a fixed-port choice, answering 4xx through `response` when the
    /// port is unusable. Returns whether the choice was persisted.
    private boolean applyFixedPort(DshInstance instance, int port, HttpServletResponse response)
            throws IOException, DshException {
        String complaint = DshPorts.validate(port);
        if (complaint != null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, complaint);
            return false;
        }
        if (!DshPorts.isFree(port)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "port " + port + " is already in use");
            return false;
        }
        DshInstanceManager.update(DshPorts.withMode(instance, DshPortMode.FIXED)
                .withPortPolicy(DshPortMode.FIXED, port));
        return true;
    }

    // ------------------------------------------------------------------ detail --

    private void getInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        JsonObject body = instanceBody(request, instance);
        body.add("installProgress", runtime.installProgress(id));
        Json.writePreservingNulls(response, body);
    }

    private void updateInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        String state = runtime.stateOf(id);
        if (state.equals("RUNNING") || state.equals("STARTING") || state.equals("STOPPING")) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "instance is running");
            return;
        }

        String name = stringField(body, "name");
        if (name != null && !name.isBlank() && !name.equals(instance.id())) {
            // Renaming moves the instance directory; an install is writing into
            // it right now, so the rename would pull the ground out from under
            // pnpm. Other edits are fine while pnpm runs.
            if (state.equals("INSTALLING")) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "install in progress; rename once it is done");
                return;
            }
            instance = DshInstanceManager.rename(id, uniqueId(name));
            runtime.forget(id);
        }

        DshHomeMode homeMode = homeMode(body);
        if (homeMode == DshHomeMode.CUSTOM) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "custom homes are not supported through the web API");
            return;
        }
        if (homeMode != null) {
            instance = instance.withHome(homeMode, null);
        }

        // The instance's account: the `name` of an entry of `/api/accounts`,
        // or null to launch with none. Written to the instance's own settings
        // file; the launch injection itself is DshLauncher's.
        if (body.has("account")) {
            com.google.gson.JsonElement element = body.get("account");
            if (element.isJsonNull() || (element.isJsonPrimitive() && element.getAsString().isBlank())) {
                DshInstanceSettings.setAccountKey(instance, null);
            } else if (element.isJsonPrimitive()) {
                String key = element.getAsString().trim();
                boolean known = false;
                for (org.jackhuang.hmcl.dsh.DshAccount account
                        : SettingsManager.settings().getAccounts()) {
                    if (account.matchesKey(key)) {
                        known = true;
                        break;
                    }
                }
                if (!known) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown account: " + key);
                    return;
                }
                DshInstanceSettings.setAccountKey(instance, key);
            } else {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "account must be a string or null");
                return;
            }
        }

        Boolean autoPort = boolField(body, "autoPort");
        DshPortMode portMode = portMode(body, autoPort != null && autoPort ? DshPortMode.AUTO : null);
        if (autoPort != null && autoPort) {
            instance = DshPorts.withMode(instance, DshPortMode.AUTO);
        } else if (portMode == DshPortMode.FIXED) {
            Integer port = intField(body, "port");
            if (port != null) {
                String complaint = DshPorts.validate(port);
                if (complaint != null) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, complaint);
                    return;
                }
                if (!DshPorts.isFree(port)) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT, "port " + port + " is already in use");
                    return;
                }
                instance = DshPorts.withMode(instance, DshPortMode.FIXED).withPortPolicy(DshPortMode.FIXED, port);
            } else {
                instance = DshPorts.withMode(instance, DshPortMode.FIXED);
            }
        } else if (portMode == DshPortMode.AUTO) {
            instance = DshPorts.withMode(instance, DshPortMode.AUTO);
        }

        if (body.has("nodeRuntime")) {
            com.google.gson.JsonElement element = body.get("nodeRuntime");
            String selector = element.isJsonNull() ? null : element.getAsString().trim();
            if (selector != null && selector.isBlank()) {
                selector = null;
            }
            if (selector != null && org.jackhuang.hmcl.dsh.NodeSelection.parse(selector).kind()
                    == org.jackhuang.hmcl.dsh.NodeSelection.Kind.INVALID) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown nodeRuntime selector: " + selector);
                return;
            }
            instance = instance.withNodeRuntime(selector);
        }

        DshInstanceManager.update(instance);
        if (body.has("nodeRuntime")) {
            AuditLog.log(AuditLog.Channel.WEB, "instance.node", id, "success",
                    actorOf(request), request.getRemoteAddr());
        }
        runtime.announceUpdated(instance);
        JsonObject updated = new JsonObject();
        updated.add("instance", instanceBody(request, instance));
        Json.writePreservingNulls(response, updated);
    }

    private void deleteInstance(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        // The terminal is an instance's own process, and after the record is
        // gone the panel has no other way to reach it: `DELETE .../terminal`
        // looks the instance up first and answers 404 once it is deleted, so
        // the kill has to happen here, before the delete. The brand and ZCode
        // routes close in this order for the same reason.
        // `stateOf` only knows the web process, so an instance whose only
        // process is the terminal reads STOPPED and passes the gate below —
        // which is exactly why the close cannot wait for it.
        instanceTerminal.close("dsh", id);
        String state = runtime.stateOf(id);
        if (state.equals("RUNNING") || state.equals("STARTING") || state.equals("STOPPING")) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "instance is running");
            return;
        }
        if (tasks.hasActiveInstallLike(id)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "install in progress");
            return;
        }
        DshInstanceManager.delete(id);
        runtime.forget(id);
        runtime.announceDeleted(instance);
        AuditLog.log(AuditLog.Channel.WEB, "instance.delete", id,
                AuthFilter.selfDirected(request, "dsh", id) ? "self_delete" : "success",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, java.util.Map.of("ok", true));
    }

    // ---------------------------------------------------------------- install --

    /// How many lines of pnpm output a failure message carries.
    private static final int INSTALL_TAIL_LINES = 20;

    private void install(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String version = stringField(body, "version");
        if (version == null || version.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "version is required");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        String state = runtime.stateOf(id);
        if (state.equals("RUNNING") || state.equals("STARTING") || state.equals("STOPPING")) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "stop the instance before changing its version");
            return;
        }
        if (version.equals(instance.version()) && DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "已安装该版本");
            return;
        }
        try {
            TaskService.Task task = submitInstall(instance, version, null);
            JsonObject result = new JsonObject();
            result.addProperty("taskId", task.id());
            Json.write(response, HttpServletResponse.SC_ACCEPTED, result);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    /// Submits the install task every entry point shares: pnpm runs on the
    /// task pool, its output drives the task's progress, and the manifest is
    /// only moved to `version` after the disk agrees that is what was
    /// installed. The last output lines ride along with a failure, so the
    /// reason survives even when the registry is what misbehaved.
    ///
    /// @param instance    the instance
    /// @param version     the version to install
    /// @param onInstalled run on success, still inside the task — the launch
    ///                    path uses it to start what it has just installed;
    ///                    may be null
    /// @return the submitted task
    /// @throws TaskService.InstanceBusyException when an install is already in flight
    private TaskService.Task submitInstall(DshInstance instance, String version,
                                          @Nullable java.util.function.Consumer<DshInstance> onInstalled) {
        String id = instance.id();
        // Written by the process pump and read by the task thread when a failure is
        // explained, with no happens-before between them: `DshCommand` only interrupts
        // its pump after `waitFor` returns, never joins it, so a line can still be
        // arriving while `failedInstall` joins what is here. A deque that is safe
        // under that is the whole reason this is not an `ArrayDeque`.
        java.util.concurrent.ConcurrentLinkedDeque<String> tail = new java.util.concurrent.ConcurrentLinkedDeque<>();
        TaskService.Task task = tasks.submit("install", id, live -> {
            org.jackhuang.hmcl.dsh.DshInstallProgress progress =
                    new org.jackhuang.hmcl.dsh.DshInstallProgress();
            // The work is handed its own task, so the progress listener can
            // report straight through it; the tail is what a failure will be
            // explained with.
            progress.addListener(p -> {
                if (p.getMessage() != null && !p.getMessage().isBlank()) {
                    if (tail.size() >= INSTALL_TAIL_LINES) {
                        tail.removeFirst();
                    }
                    tail.addLast(p.getMessage().trim());
                }
                live.update(p.getMessage(), p.getFraction());
            });
            try {
                DshVersionManager.install(instance, version, progress::accept);
            } catch (DshException | RuntimeException e) {
                throw failedInstall(id, version, tail, e.getMessage(), e);
            }
            // pnpm succeeded — but the manifest must describe the disk, not the
            // request. A successful run that landed something else (a registry
            // serving a different tarball, a store gone strange) is recorded as
            // what it is and reported as a failure: claiming the requested
            // version here is exactly how an instance comes to lie about what
            // it runs.
            String actual = installedVersion(instance);
            if (actual == null) {
                throw failedInstall(id, version, tail,
                        "pnpm reported success, but the installed version could not be read back", null);
            }
            DshInstance installed = instance;
            if (!version.equals(actual)) {
                installed = instance.withVersion(actual);
                DshInstanceManager.update(installed);
                runtime.announceUpdated(installed);
                throw failedInstall(id, version, tail,
                        "pnpm installed " + actual + " instead of the requested version; "
                                + "the instance now records " + actual, null);
            }
            if (!version.equals(instance.version())) {
                installed = instance.withVersion(version);
                DshInstanceManager.update(installed);
                runtime.announceUpdated(installed);
            }
            org.jackhuang.hmcl.util.logging.Logger.LOG.info("Installed DSH " + version + " for " + id);
            if (onInstalled != null) {
                onInstalled.accept(installed);
            }
            return "Installed " + version;
        });
        // The task is registered by now, so stateOf() answers INSTALLING;
        // telling the panel right away saves it a poll.
        runtime.announceState(id);
        return task;
    }

    /// Builds the failure an install task ends with: the reason, the last lines
    /// pnpm printed, and a line in the container log — the task table lives in
    /// memory and would take the explanation with it on a restart.
    private static DshException failedInstall(String id, String version,
                                              java.util.concurrent.ConcurrentLinkedDeque<String> tail,
                                              @Nullable String reason, @Nullable Throwable cause) {
        String detail = "Installing DSH " + version + " for " + id + " failed: " + reason
                + registryHint(reason + "\n" + String.join("\n", tail))
                + (tail.isEmpty() ? "" : "\n" + String.join("\n", tail));
        org.jackhuang.hmcl.util.logging.Logger.LOG.warning(detail);
        return new DshException(detail, cause);
    }

    /// A package pnpm could not find, as pnpm itself names it.
    private static final java.util.regex.Pattern MISSING_PACKAGE =
            java.util.regex.Pattern.compile("([@a-zA-Z0-9._/-]+) is not in the npm registry");

    /// Turns the two endings that are nobody's fault into one line of advice,
    /// placed before the raw output: a dependency the upstream unpublished (the
    /// version can never be installed again — the panel's own version list
    /// cannot see this, because it only checks the companion package) and a
    /// requirement the registry cannot satisfy. Both read as "retry" otherwise,
    /// and retrying is exactly what does not help.
    ///
    /// @param output the failure reason plus pnpm's tail
    /// @return the hint line, or an empty string when the ending needs none
    static String registryHint(String output) {
        java.util.regex.Matcher missing = MISSING_PACKAGE.matcher(output);
        if (output.contains("ERR_PNPM_FETCH_404") && missing.find()) {
            return "\n[hint] registry 上已没有 " + missing.group(1)
                    + "：该版本依赖的包被上游删除，这个 dsh 版本再也装不上（不是本地问题，重试无用）——请换一个版本。";
        }
        if (output.contains("ERR_PNPM_NO_MATCHING_VERSION")) {
            return "\n[hint] 该版本要求的配套包在 registry 上没有对应版本（上游没发布这一版）——请换一个版本。";
        }
        return "";
    }

    /// The version of the harness sitting in an instance's own `dsh`
    /// directory, or null when it cannot be read.
    private static @Nullable String installedVersion(DshInstance instance) {
        try {
            Path manifest = instance.dshDirectory()
                    .resolve(org.jackhuang.hmcl.dsh.DshVersion.PACKAGE_PATH)
                    .resolve("package.json");
            if (!Files.isRegularFile(manifest)) {
                return null;
            }
            com.google.gson.JsonElement parsed =
                    JsonParser.parseString(Files.readString(manifest, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return null;
            }
            return stringField(parsed.getAsJsonObject(), "version");
        } catch (DshException | IOException | JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------- launch --

    private void launch(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        // The web runtime and the TUI share DSH_HOME: starting the web
        // process while a terminal is up would put two writers on one home.
        // The takeover is unconditional in both directions — opening the
        // terminal stops the web runtime — so the launch closes first.
        instanceTerminal.close("dsh", id);
        String state = runtime.stateOf(id);
        if (state.equals("STOPPING")) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "instance is stopping");
            return;
        }
        if (state.equals("RUNNING") || state.equals("STARTING")) {
            Json.write(response, HttpServletResponse.SC_ACCEPTED, java.util.Map.of("state", state.toLowerCase(Locale.ROOT)));
            return;
        }
        // `isUsable`, not `isInstalled`: a tree an interrupted install left behind has an
        // entry script and no packages, which starts and then fails every session with
        // `waiting for workingDirectory`. Reinstalling is the repair (pnpm alone answers
        // "already up to date"), and this is the click that should do it.
        if (!DshVersionManager.isUsable(instance)) {
            if (Boolean.FALSE.equals(boolField(body, "autoInstall"))) {
                Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
                return;
            }
            if (state.equals("INSTALLING")) {
                Json.error(response, HttpServletResponse.SC_CONFLICT, "install in progress");
                return;
            }
            // Launcher behaviour: "start this" on a version that is not here
            // yet installs it and starts it — in one move, without a second
            // click. The whole exchange is one task, so the panel's progress
            // view covers both halves.
            String host = resolvePublicHost(body, request);
            try {
                TaskService.Task task = submitInstall(instance, instance.version(),
                        installed -> runtime.launchAsync(installed, host));
                JsonObject result = new JsonObject();
                result.addProperty("state", "installing");
                result.addProperty("taskId", task.id());
                Json.write(response, HttpServletResponse.SC_ACCEPTED, result);
            } catch (TaskService.InstanceBusyException e) {
                Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            }
            return;
        }
        String publicHost = resolvePublicHost(body, request);
        runtime.launchAsync(instance, publicHost);
        AuditLog.log(AuditLog.Channel.WEB, "instance.launch", id,
                AuthFilter.selfDirected(request, "dsh", id) ? "self_launch" : "success",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, HttpServletResponse.SC_ACCEPTED, java.util.Map.of("state", "starting"));
    }

    /// The host handed to dsh as `--trusted-host`: the request body first,
    /// then `server.yaml`'s `public_base_url`, then the request's own Host
    /// header — all reduced to the bare hostname, a level whose value does not
    /// parse falling through to the next.
    private @Nullable String resolvePublicHost(JsonObject body, HttpServletRequest request) {
        String fromBody = stringField(body, "publicHost");
        if (fromBody != null && !fromBody.isBlank()) {
            String host = hostOf(fromBody);
            if (host != null) {
                return host;
            }
        }
        if (config.publicBaseUrl != null && !config.publicBaseUrl.isBlank()) {
            String host = hostOf(config.publicBaseUrl);
            if (host != null) {
                return host;
            }
        }
        return HostHeaders.stripPort(request.getHeader("Host"));
    }


    // --------------------------------------------------------------- terminal --

    /// `POST /api/instances/{id}/terminal` opens the dsh TUI for the instance;
    /// `DELETE` kills it. Same gates as the brand terminal: session, developer
    /// elevation, and (on Windows) the desktop confirmation.
    private void terminal(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if ("DELETE".equals(request.getMethod())) {
            JsonObject body = new JsonObject();
            body.addProperty("ok", true);
            body.addProperty("killed", instanceTerminal.close("dsh", id));
            Json.write(response, body);
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
            return;
        }
        try {
            instanceTerminal.authorize(request);
            // Which terminal plugin this instance holds is asked *before* the takeover:
            // opening stops the web runtime first, so an instance with no terminal
            // plugin would be taken down to be told it has nothing to run. The refusal
            // is the same sentence `DshTui.target` would raise, said earlier.
            try {
                DshTui.terminalPlugin(instance);
            } catch (DshException e) {
                // Audited, unlike an unauthorised attempt: the interesting refusal is
                // "this instance has no terminal to open", and without a line here a
                // person can only tell it apart from a terminal that opened and died by
                // the absence of a `terminal.open`.
                AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.open", id, "failure",
                        actorOf(request), request.getRemoteAddr());
                Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
                return;
            }
            // The web runtime and the TUI share DSH_HOME, so the terminal takes
            // the instance over. The stop has to be the blocking one: spawning
            // the TUI while the web process is still exiting leaves two
            // processes on one home.
            Runnable stopWeb = () -> org.jackhuang.hmcl.dsh.DshProcessManager.stop(id);
            JsonObject body = instanceTerminal.open(request, id, stopWeb,
                    resume -> {
                        try {
                            return DshTui.target(instance, resume);
                        } catch (org.jackhuang.hmcl.dsh.DshException e) {
                            throw new IOException(e.getMessage(), e);
                        }
                    });
            Json.write(response, body);
        } catch (org.jackhuang.hmcl.web.terminal.InstanceTerminal.Refused e) {
            org.jackhuang.hmcl.web.terminal.InstanceTerminal.write(request, response, e);
        }
    }

    /// `POST /api/instances/{id}/reveal` opens the instance's home or workspace
    /// in the desktop; dsh's logs are in memory, so `log` is refused.
    private void reveal(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (!org.jackhuang.hmcl.web.desktop.DesktopIntegration.supported()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "desktop_unsupported: this host has no desktop");
            return;
        }
        String target = readBodyField(request, "target");
        Path path;
        try {
            path = switch (target == null ? "" : target) {
                case "home" -> instance.homeDirectory();
                case "workspace" -> instance.workspacePath();
                case "log" -> null;
                default -> null;
            };
        } catch (DshException | RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        if (path == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "target must be home or workspace (dsh logs are in memory)");
            return;
        }
        try {
            Files.createDirectories(path);
            org.jackhuang.hmcl.web.desktop.DesktopIntegration.reveal(path);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        AuditLog.log(AuditLog.Channel.TERMINAL, "desktop.reveal", id, "success",
                actorOf(request), request.getRemoteAddr());
        JsonObject body = new JsonObject();
        body.addProperty("opened", path.toString());
        Json.write(response, body);
    }

    private static @Nullable String readBodyField(HttpServletRequest request, String field) throws IOException {
        try (java.io.InputStreamReader reader =
                     new java.io.InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject body = JsonParser.parseReader(reader).getAsJsonObject();
            return body.has(field) && !body.get(field).isJsonNull()
                    ? body.get(field).getAsString()
                    : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------- stop --

    private void stop(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        // The terminal is an instance's process too, and it is the one the
        // web runtime does not know about: `runningProcess` is empty while a
        // terminal is up, so an instance whose only process is the terminal
        // used to read "instance not running" — refusing the stop the caller
        // asked for. Close first, then decide on what is actually left.
        boolean killedTerminal = instanceTerminal.close("dsh", id);
        boolean webStopping = runtime.runningProcess(id).isPresent() || DshProcessManager.isStopping(id);
        if (!webStopping && !killedTerminal) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "instance not running");
            return;
        }
        if (webStopping) {
            runtime.stopAsync(id);
        } else {
            // No web process is stopping, so nothing is coming that would
            // change the state the panel last saw — and a browser that asked
            // for a stop has already shown "stopping". Re-announce the state so
            // it hears the truth instead of waiting for a refresh.
            runtime.announceState(id);
        }
        AuditLog.log(AuditLog.Channel.WEB, "instance.stop", id,
                AuthFilter.selfDirected(request, "dsh", id) ? "self_stop" : "success",
                actorOf(request), request.getRemoteAddr());
        if (webStopping) {
            Json.write(response, HttpServletResponse.SC_ACCEPTED, java.util.Map.of("state", "stopping"));
        } else {
            // The terminal was the only thing running and it is gone now: the
            // truthful answer is stopped, not a promise to stop later.
            Json.write(response, java.util.Map.of("state", "stopped"));
        }
    }

    // ------------------------------------------------------------------- logs --

    private void logs(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        int tail = 200;
        String tailParam = request.getParameter("tail");
        if (tailParam != null) {
            try {
                tail = Integer.parseInt(tailParam);
            } catch (NumberFormatException ignored) {
                // The default stands for an unparsable bound.
            }
        }
        tail = Math.max(1, Math.min(2000, tail));
        JsonObject body = new JsonObject();
        body.add("lines", runtime.logs(id, tail));
        Json.write(response, body);
    }

    // -------------------------------------------------------------------- open --

    private void open(HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        var url = runtime.openUrl(id);
        if (url.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "instance not running");
            return;
        }
        // The URL is consumed by the browser, so it carries the mount base:
        // a subpath-mounted panel must hand out /panel/i/<id>/, not /i/<id>/.
        Json.write(response, java.util.Map.of("url", config.basePath + url.get()));
    }

    // ---------------------------------------------------------------- plugins --

    /// Routes under `/api/instances/{id}/plugins`.
    private void plugins(HttpServletRequest request, HttpServletResponse response, String id,
                         String[] segments) throws IOException, DshException {
        if (segments.length == 2) {
            if ("GET".equals(request.getMethod())) {
                listPlugins(response, id);
            } else {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
            }
            return;
        }
        if (segments.length != 3 || !"POST".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        switch (segments[2]) {
            case "install" -> installPlugins(request, response, id);
            case "remove" -> removePlugins(request, response, id);
            case "local" -> uploadLocalPlugin(request, response, id);
            default -> Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        }
    }

    /// The profile's declared plugins: every dependency, whether it made the
    /// bundle list, and whether its build script is still unanswered (`pending`).
    private void listPlugins(HttpServletResponse response, String id) throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        java.nio.file.Path home = instance.homeDirectory();
        java.util.Map<String, String> dependencies =
                DshPluginInstaller.readDependencies(home, instance.profile());
        java.util.List<String> bundles = DshPluginInstaller.readBundles(home, instance.profile());
        java.util.List<String> pending = DshBuildScripts.unanswered(instance);

        JsonArray plugins = new JsonArray();
        for (java.util.Map.Entry<String, String> dependency : dependencies.entrySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", dependency.getKey());
            entry.addProperty("version", dependency.getValue());
            if (bundles.contains(dependency.getKey())) {
                entry.addProperty("bundled", true);
            }
            if (pending.contains(dependency.getKey())) {
                entry.addProperty("pending", true);
            }
            plugins.add(entry);
        }
        JsonArray bundleArray = new JsonArray();
        for (String bundle : bundles) {
            bundleArray.add(bundle);
        }
        JsonObject body = new JsonObject();
        body.add("plugins", plugins);
        body.add("bundles", bundleArray);
        Json.write(response, body);
    }

    // ------------------------------------------------------------ tui plugins --

    /// Routes under `/api/instances/{id}/tui-plugins`.
    ///
    /// A terminal interface is an app plugin, so it cannot share a profile with the web
    /// app: it goes into the profile its own authors document (`tui`, `dsh-tui`,
    /// `pi-tui`) and is started there — see [DshTuiPlugins]. That is the one thing these
    /// routes add, and it is the reason they exist at all: the ordinary plugin routes
    /// modify the instance's own profile, which for a terminal plugin is the wrong one.
    ///
    /// The profile is never named by the request. It comes from the catalogue, so a
    /// caller cannot ask for the combination that does not boot.
    private void tuiPlugins(HttpServletRequest request, HttpServletResponse response, String id,
                            String[] segments) throws IOException, DshException {
        if (segments.length == 2) {
            if ("GET".equals(request.getMethod())) {
                listTuiPlugins(response, id);
            } else {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
            }
            return;
        }
        if (segments.length != 3 || !"POST".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        switch (segments[2]) {
            case "install" -> changeTuiPlugin(request, response, id, true);
            case "remove" -> changeTuiPlugin(request, response, id, false);
            default -> Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        }
    }

    /// Which candidate terminal interfaces this instance has installed, and where.
    ///
    /// One row per catalogue entry, always — an entry that is not installed is what the
    /// page offers, and leaving it out would make the answer unusable as a list. The
    /// state is read from **each entry's own profile**, because that is where these live:
    /// asking the instance's `web` profile would report every one of them as absent.
    private void listTuiPlugins(HttpServletResponse response, String id) throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        java.nio.file.Path home = instance.homeDirectory();
        JsonArray plugins = new JsonArray();
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            java.util.Map<String, String> dependencies =
                    DshPluginInstaller.readDependencies(home, entry.profile());
            boolean installed = DshTuiPlugins.isInstalled(entry, dependencies.keySet());
            JsonObject json = new JsonObject();
            json.addProperty("packageName", entry.packageName());
            json.addProperty("profile", entry.profile());
            json.addProperty("kind", entry.terminal() ? "terminal" : "capability");
            json.addProperty("installed", installed);
            // Declared is not landed: pnpm writes the dependency before it fetches, so a
            // run that failed half-way leaves a profile that claims a plugin whose files
            // are absent. Booting that profile dies before printing a byte, so the page
            // has to be able to say "declared, but not on disk" and offer a reinstall
            // rather than only an uninstall.
            boolean filesPresent = false;
            if (installed) {
                try {
                    filesPresent = DshTui.filesPresent(instance, entry);
                } catch (DshException ignored) {
                    // An instance that cannot name its home cannot have the files either.
                }
            }
            json.addProperty("filesPresent", filesPresent);
            json.addProperty("bundled", entry.bundled());
            // Whether this instance's harness line can run it at all. The page greys the
            // install button out on false rather than hiding the row: "this exists but your
            // instance is too old" is what the user needs to see (see DshTuiPlugins#fitsHost).
            json.addProperty("fitsHost", DshTuiPlugins.fitsHost(entry, instance.version()));
            if (entry.requiresDsh() != null) {
                json.addProperty("requiresDsh", entry.requiresDsh());
            }
            String presentVersion = null;
            if (installed) {
                presentVersion = installedVersion(instance, entry, dependencies);
                json.addProperty("installedVersion", presentVersion);
            }
            // Installed is not "this build's copy": a source change without a version
            // bump produces two packages that both say 0.2.0, and the old panel saw
            // "installed" and never offered the newer files. A first-party package is
            // compared by the tarball digest the install recorded (no record counts as
            // outdated — reinstalling once fills it in); a registry package by the
            // version the catalogue pins.
            boolean outdated = false;
            if (installed && filesPresent) {
                if (entry.bundled()) {
                    outdated = FirstPartyPlugins.isOutdated(home, entry.packageName());
                } else if (presentVersion != null && !presentVersion.isBlank()) {
                    outdated = !entry.version().equals(presentVersion);
                }
            }
            json.addProperty("outdated", outdated);
            plugins.add(json);
        }
        // Which row the terminal button would start right now: the first installed entry
        // whose files are there and whose host line fits. Several terminals may be installed
        // on purpose (the ACP one is the fallback for a version that moves); the rest are
        // standby, not broken.
        String active = null;
        try {
            active = DshTui.terminalPlugin(instance).packageName();
        } catch (DshException ignored) {
            // Nothing is startable; no row claims to be active.
        }
        for (JsonElement element : plugins) {
            JsonObject json = element.getAsJsonObject();
            json.addProperty("active", json.get("packageName").getAsString().equals(active));
        }
        JsonObject body = new JsonObject();
        body.add("plugins", plugins);
        Json.write(response, body);
    }

    /// The version the profile really holds.
    ///
    /// A bundled plugin is installed from a tarball, and pnpm records that as `file:/…`
    /// rather than a version; reading the installed manifest is what lets the page show
    /// `0.1.0` for a plugin the jar provided, instead of a path.
    private static @Nullable String installedVersion(DshInstance instance, DshTuiPlugins.Entry entry,
                                                     java.util.Map<String, String> dependencies) {
        String recorded = declaredVersion(entry, dependencies);
        if (recorded != null && !recorded.startsWith("file:") && !recorded.startsWith("link:")) {
            return recorded;
        }
        try {
            Path manifest = DshPluginInstaller.profileNodeModules(instance.homeDirectory(), entry.profile())
                    .resolve(entry.packageName()).resolve("package.json");
            if (Files.isRegularFile(manifest)) {
                com.google.gson.JsonElement parsed =
                        com.google.gson.JsonParser.parseString(Files.readString(manifest));
                if (parsed.isJsonObject()) {
                    com.google.gson.JsonElement version = parsed.getAsJsonObject().get("version");
                    if (version != null && version.isJsonPrimitive()) {
                        return version.getAsString();
                    }
                }
            }
        } catch (IOException | RuntimeException | DshException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.debug(
                    "Could not read the installed version of " + entry.packageName(), e);
        }
        return recorded;
    }

    /// The version a profile recorded for an entry, whatever case it was written in.
    private static @Nullable String declaredVersion(
            DshTuiPlugins.Entry entry, java.util.Map<String, String> dependencies) {
        for (java.util.Map.Entry<String, String> dependency : dependencies.entrySet()) {
            if (entry.packageName().equalsIgnoreCase(dependency.getKey().trim())) {
                return dependency.getValue();
            }
        }
        return null;
    }

    /// Installs or removes one catalogue entry, in the profile the catalogue names.
    ///
    /// The body names the package and nothing else: `{package: "@xmoon76/dsh-pi-tui"}`.
    /// The specification installed is the catalogue's **pinned** one, so the version the
    /// page showed is the version that lands, and the profile comes from the same entry.
    private void changeTuiPlugin(HttpServletRequest request, HttpServletResponse response, String id,
                                 boolean install) throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String name = body.has("package") && body.get("package").isJsonPrimitive()
                ? body.get("package").getAsString() : null;
        DshTuiPlugins.Entry entry = DshTuiPlugins.find(name).orElse(null);
        if (entry == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "package must be one of: " + DshTuiPlugins.ENTRIES.stream()
                            .map(DshTuiPlugins.Entry::packageName)
                            .collect(java.util.stream.Collectors.joining(", ")));
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (!DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
            return;
        }
        // `install` takes the pinned specification; `remove` takes the name, which is what
        // the profile recorded as a key.
        // A terminal plugin's tool surface comes from the agent-preset layer, which the
        // harness only has from 0.2.0 — so a 0.2-only plugin on an older instance is a
        // combination to refuse, not to install and discover at boot. Said before the task
        // is submitted, so nobody waits for pnpm just to be told the session would be empty.
        //
        // The host line is the one hard rule: a terminal written for services the instance
        // does not have is a session that opens and cannot work. Installing *both* first-party
        // terminals is deliberately allowed — the ACP one is the fallback for an instance
        // whose version moves, and it stays dormant while the in-process one runs.
        if (install && !DshTuiPlugins.fitsHost(entry, instance.version())) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    DshTuiPlugins.hostMismatch(entry, instance.version()));
            return;
        }
        java.util.List<String> specs;
        if (!install) {
            specs = java.util.List.of(entry.packageName());
        } else if (entry.bundled()) {
            // Installed from the copy inside this jar. The tarball is written under the
            // instance's home so a reinstall does not depend on a temp directory that a
            // reboot cleaned.
            Path tarball = FirstPartyPlugins.materialize(entry.packageName(),
                    instance.homeDirectory().resolve(".first-party-plugins"));
            specs = java.util.List.of("file:" + tarball.toAbsolutePath());
        } else {
            specs = java.util.List.of(entry.spec());
        }
        submitPluginTask(install ? "tui-plugin-install" : "tui-plugin-remove",
                instance, entry.profile(), specs, install, entry, response);
    }

    /// Installs package specs as a task: `dsh plugin add` per spec, with the
    /// build-script approval dance when a package wants to run code.
    private void installPlugins(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        java.util.List<String> specs = specsField(body);
        if (specs == null || specs.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "specs must be a non-empty array");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (!DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
            return;
        }
        // The instance's own profile — which is the point of the refusal inside the
        // installer: a terminal plugin belongs in its own profile, and this route cannot
        // know that, so the installer says so instead of quietly mixing two app plugins.
        submitPluginTask("plugin-install", instance, instance.profile(), specs, true, null, response);
    }

    /// Removes package specs as a task: one `dsh plugin remove` for all of them.
    private void removePlugins(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        java.util.List<String> specs = specsField(body);
        if (specs == null || specs.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "specs must be a non-empty array");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (!DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
            return;
        }
        submitPluginTask("plugin-remove", instance, instance.profile(), specs, false, null, response);
    }

    /// Submits one plugin installation or removal as a task.
    ///
    /// One place, because `/plugins/install`, `/plugins/remove` and the two
    /// `/tui-plugins` routes differ only in the profile and the specifications they
    /// name — the task kind, the build-script approval callback, the progress holder and
    /// the busy answer are the same, and a second copy of them is how those drift apart.
    ///
    /// @param kind     the task kind the interface groups by (`plugin-install`, …)
    /// @param instance the instance whose profile is modified
    /// @param profile  the profile inside its `DSH_HOME` to modify
    /// @param specs    the package specifications
    /// @param install  whether this adds the packages or takes them out
    /// @param response the response to answer 202 on
    private void submitPluginTask(String kind, DshInstance instance, String profile,
                                  java.util.List<String> specs, boolean install,
                                  @Nullable DshTuiPlugins.Entry entry,
                                  HttpServletResponse response) throws IOException, DshException {
        try {
            TaskService.Task task = tasks.submit(kind, instance.id(), live -> {
                if (install) {
                    DshPluginInstaller.installSpecs(instance, profile, specs, line -> live.update(line, -1));
                    // Remember which tarball landed: the digest is what later tells
                    // "same version, newer files" apart from "already current".
                    if (entry != null && entry.bundled()) {
                        String sha = FirstPartyPlugins.find(entry.packageName())
                                .map(FirstPartyPlugins.Bundled::sha256).orElse(null);
                        FirstPartyPlugins.recordInstalled(instance.homeDirectory(), entry.packageName(), sha);
                    }
                } else {
                    DshPluginInstaller.removeSpecs(instance, profile, specs, line -> live.update(line, -1));
                    if (entry != null && entry.bundled()) {
                        FirstPartyPlugins.recordInstalled(instance.homeDirectory(), entry.packageName(), null);
                    }
                }
                return (install ? "Installed " : "Removed ") + specs.size() + " plugin(s)";
            }, (allow, keys) -> DshBuildScripts.answer(instance, keys, allow));
            accepted(response, task);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    /// Installs an uploaded `.tgz` as a task. The file is first stored as the
    /// instance's own copy — the name [DshLocalPlugins] would give it — so the
    /// profile's recorded dependency points at a file the instance keeps.
    private void uploadLocalPlugin(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        if (!DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
            return;
        }
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
        String submitted = part.getSubmittedFileName() == null ? "plugin.tgz" : part.getSubmittedFileName();
        String lower = submitted.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".tgz") && !lower.endsWith(".tar.gz")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    submitted + " is not a packed plugin (.tgz)");
            return;
        }

        // Inspect from a staging file, then move into the instance under the
        // name DshLocalPlugins would give the copy — the install then finds the
        // file already in place and records the instance's own path.
        Path staging = config.dataDir.resolve("tmp").resolve(
                "upload-" + java.util.UUID.randomUUID() + ".tgz");
        DshLocalPlugins.Package pkg;
        try {
            java.nio.file.Files.createDirectories(staging.getParent());
            try (java.io.InputStream in = part.getInputStream()) {
                java.nio.file.Files.copy(in, staging);
            }
            pkg = DshLocalPlugins.inspect(staging);
        } catch (DshException e) {
            java.nio.file.Files.deleteIfExists(staging);
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        Path directory = instance.instanceDirectory().resolve(DshLocalPlugins.DIRECTORY);
        Path target = directory.resolve(safeFileName(pkg.name()) + "-" + safeFileName(pkg.version()) + ".tgz");
        java.nio.file.Files.createDirectories(directory);
        java.nio.file.Files.move(staging, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try {
            TaskService.Task task = tasks.submit("plugin-local", id, live -> {
                DshLocalPlugins.install(instance, target, line -> live.update(line, -1));
                return "Installed " + pkg.name() + " " + pkg.version();
            }, (allow, keys) -> DshBuildScripts.answer(instance, keys, allow));
            accepted(response, task);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ export --

    /// The two halves of the export route: a `POST` writes the pack, and a
    /// `GET` answers what could be chosen for it.
    private void export(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        if ("GET".equals(request.getMethod())) {
            exportChoices(response, id);
            return;
        }
        exportPack(request, response, id);
    }

    /// What a pack of this instance could carry beyond the profile: the
    /// home-level content the security filter allows, and the harness versions
    /// this machine has installed.
    ///
    /// Both are selections the export takes (`home`, `dshVersions`), and both
    /// are facts about this machine rather than about the pack, so they are
    /// asked for here instead of guessed at by whoever fills the form. The
    /// home list is a **candidate** list: nothing travels unless it is named in
    /// the export request, and naming a credential does not carry it either —
    /// the filter has already left it out of this list.
    private void exportChoices(HttpServletResponse response, String id) throws IOException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        JsonObject body = new JsonObject();
        JsonArray home = new JsonArray();
        try {
            Path homeDirectory = instance.homeDirectory();
            // An instance that has never been launched has no home yet, and that is an empty
            // selection rather than a failure: there is nothing there to offer.
            if (Files.isDirectory(homeDirectory)) {
                for (DshPackForge.Entry entry : DshPackForge.scanHome(homeDirectory).files()) {
                    JsonObject file = new JsonObject();
                    file.addProperty("path", entry.relative());
                    file.addProperty("sizeBytes", entry.size());
                    home.add(file);
                }
            }
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        body.add("home", home);
        JsonArray versions = new JsonArray();
        DshPackInstaller.installedVersions().forEach(versions::add);
        body.add("dshVersions", versions);
        Json.write(response, body);
    }

    /// Exports the instance as a DSH-PackForge `.dspack` into
    /// `<HDSL_DATA>/exports/`. The domain's scan decides what travels —
    /// credentials, keys, tokens and nested archives never do — and when
    /// `includeSessions` is set the conversations are written beside the
    /// container in the domain's own session-pack layout.
    ///
    /// The body may also carry `home`, the home-level paths to carry beside the
    /// profile, and `dshVersions`, the other harness versions the pack is
    /// claimed to have been measured against. Neither is guessed at: both are
    /// absent by default, and absent means the pack carries the profile alone
    /// and claims only the version it is pinned to.
    private void exportPack(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        if (!"POST".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
            return;
        }
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        // The harness writes the profile tree on its first boot, so an instance
        // that was installed but never launched has none yet. That is not an
        // error: the profile is materialised here so the export carries what
        // exists (the pinned version, an empty plugin list), matching what
        // launching and the plugin endpoints consider "installed".
        if (!DshVersionManager.isInstalled(instance)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "not installed");
            return;
        }
        Path profileDirectory = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        try {
            Files.createDirectories(profileDirectory);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "could not prepare the profile directory: " + e.getMessage());
            return;
        }
        String name = stringField(body, "name");
        Boolean includeSessions = boolField(body, "includeSessions");
        String base = name != null && !name.isBlank() ? name : instance.id();
        boolean withSessions = includeSessions != null && includeSessions;
        java.util.List<String> homeInclude = stringList(body, "home");
        java.util.List<String> measured = stringList(body, "dshVersions");
        try {
            TaskService.Task task = tasks.submit("pack-export", id, live -> {
                java.util.function.Consumer<String> report = line -> live.update(line, -1);
                Path dir = PackExports.ensureDirectory(config.dataDir.resolve("exports"));
                Path target = PackExports.uniqueFile(dir, base, ".dspack");
                DshPackForge.Options options = new DshPackForge.Options(
                        DshPackForge.Options.kebab(base), "1.0.0", base, "", "", measured);
                java.util.List<org.jackhuang.hmcl.dsh.DshSession> sessions = withSessions
                        ? org.jackhuang.hmcl.dsh.DshSessions.list(instance.homeDirectory())
                        : java.util.List.of();
                if (sessions.isEmpty()) {
                    DshPackForge.export(instance, target, options, homeInclude, report);
                } else {
                    // PackForge writes no sessions, and a pack carrying
                    // conversations lays them out the session-pack way — same
                    // entries, same rules — so the container is written first
                    // and the session entries are appended beside it.
                    Path building = target.resolveSibling(target.getFileName() + ".building");
                    try {
                        DshPackForge.export(instance, building, options, homeInclude, report);
                        appendSessions(building, target, instance, sessions, report);
                    } finally {
                        Files.deleteIfExists(building);
                    }
                }
                PackExports.privateFile(target);
                JsonObject result = new JsonObject();
                result.addProperty("filename", target.getFileName().toString());
                live.attachResult(result);
                return "Exported " + target.getFileName();
            });
            accepted(response, task);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    /// Copies a finished `.dspack` and writes the sessions' entries (logs,
    /// projection cache, the attachments they name) after it, which is the
    /// layout [org.jackhuang.hmcl.dsh.DshSessionPacks] reads them back from —
    /// a modpack carries conversations exactly this way.
    private static void appendSessions(Path source, Path target, DshInstance instance,
                                       java.util.List<org.jackhuang.hmcl.dsh.DshSession> sessions,
                                       java.util.function.Consumer<String> report)
            throws IOException, DshException {
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(Files.newInputStream(source));
             java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                out.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                in.transferTo(out);
                out.closeEntry();
            }
            Path home = instance.homeDirectory();
            org.jackhuang.hmcl.dsh.DshSessionPacks.writeInto(out, home, sessions, report);
            org.jackhuang.hmcl.dsh.DshSessionPacks.writeAttachmentsInto(out, home, sessions, report);
        }
    }

    // --------------------------------------------------------------- sessions --

    /// Routes under `/api/instances/{id}/sessions`.
    private void sessions(HttpServletRequest request, HttpServletResponse response, String id,
                          String[] segments) throws IOException, DshException {
        if (segments.length == 2) {
            if (!"GET".equals(request.getMethod())) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            listSessions(response, id);
            return;
        }
        if (segments.length == 3 && "export".equals(segments[2])) {
            if (!"POST".equals(request.getMethod())) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            exportSessions(request, response, id);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    /// The home's sessions, newest first. `messageCount` would mean decoding
    /// every (compressed) log, which the domain layer deliberately never
    /// does, so the count is absent rather than expensive.
    private void listSessions(HttpServletResponse response, String id) throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        JsonArray sessions = new JsonArray();
        for (org.jackhuang.hmcl.dsh.DshSession session
                : org.jackhuang.hmcl.dsh.DshSessions.list(instance.homeDirectory())) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", session.id());
            if (session.title() != null) {
                entry.addProperty("title", session.title());
            }
            entry.addProperty("workspaceSlug", session.workspaceSlug());
            entry.addProperty("updatedAt", session.modifiedAt());
            entry.addProperty("sizeBytes", session.sizeBytes());
            if (session.locked()) {
                entry.addProperty("locked", true);
            }
            sessions.add(entry);
        }
        JsonObject body = new JsonObject();
        body.add("sessions", sessions);
        Json.write(response, body);
    }

    /// Packs the chosen sessions into a `.sspack` under `exports/`.
    private void exportSessions(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        java.util.List<String> wanted = stringArrayField(body, "sessionIds");
        if (wanted == null || wanted.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "sessionIds must be a non-empty array");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        java.util.List<org.jackhuang.hmcl.dsh.DshSession> all =
                org.jackhuang.hmcl.dsh.DshSessions.list(instance.homeDirectory());
        java.util.List<org.jackhuang.hmcl.dsh.DshSession> chosen = new java.util.ArrayList<>();
        for (String wantedId : wanted) {
            org.jackhuang.hmcl.dsh.DshSession found = null;
            for (org.jackhuang.hmcl.dsh.DshSession session : all) {
                if (session.id().equals(wantedId)) {
                    found = session;
                    break;
                }
            }
            if (found == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown session: " + wantedId);
                return;
            }
            chosen.add(found);
        }
        try {
            TaskService.Task task = tasks.submit("session-export", id, live -> {
                java.util.function.Consumer<String> report = line -> live.update(line, -1);
                Path dir = PackExports.ensureDirectory(config.dataDir.resolve("exports"));
                Path target = PackExports.uniqueFile(dir, instance.id() + "-sessions", ".sspack");
                org.jackhuang.hmcl.dsh.DshSessionPacks.export(instance, chosen, target, report);
                PackExports.privateFile(target);
                JsonObject result = new JsonObject();
                result.addProperty("filename", target.getFileName().toString());
                live.attachResult(result);
                return "Exported " + chosen.size() + " session(s) to " + target.getFileName();
            });
            accepted(response, task);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    // ------------------------------------------------------------- workspaces --

    /// The sessions grouped the harness's own way: one workspace per slug
    /// directory, named by the working directory its sessions recorded.
    private void workspaces(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        if (!"GET".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        JsonArray workspaces = new JsonArray();
        for (org.jackhuang.hmcl.dsh.DshWorkspace workspace
                : org.jackhuang.hmcl.dsh.DshSessions.workspaces(instance.homeDirectory())) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", workspace.slug());
            entry.addProperty("name", workspace.title());
            if (workspace.path() != null) {
                entry.addProperty("path", workspace.path());
            }
            entry.addProperty("updatedAt", workspace.modifiedAt());
            entry.addProperty("sessionCount", workspace.sessions().size());
            workspaces.add(entry);
        }
        JsonObject body = new JsonObject();
        body.add("workspaces", workspaces);
        Json.write(response, body);
    }

    // ----------------------------------------------------------------- skills --

    /// Routes under `/api/instances/{id}/skills`.
    private void skills(HttpServletRequest request, HttpServletResponse response, String id,
                        String[] segments) throws IOException, DshException {
        if (segments.length == 2) {
            if (!"GET".equals(request.getMethod())) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            listSkills(response, id);
            return;
        }
        if (segments.length == 3 && "install".equals(segments[2])) {
            if (!"POST".equals(request.getMethod())) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                return;
            }
            installSkill(request, response, id);
            return;
        }
        if (segments.length == 3 && "DELETE".equals(request.getMethod())) {
            removeSkill(response, id, segments[2]);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void listSkills(HttpServletResponse response, String id) throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        JsonArray skills = new JsonArray();
        for (org.jackhuang.hmcl.dsh.DshSkill skill
                : org.jackhuang.hmcl.dsh.DshSkills.list(instance.homeDirectory())) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", skill.fileName());
            entry.addProperty("name", skill.name());
            if (!skill.description().isBlank()) {
                entry.addProperty("description", skill.description());
            }
            entry.addProperty("enabled", skill.enabled());
            entry.addProperty("bundle", skill.bundle());
            entry.addProperty("sizeBytes", skill.sizeBytes());
            skills.add(entry);
        }
        JsonObject body = new JsonObject();
        body.add("skills", skills);
        Json.write(response, body);
    }

    /// Installs a skill as a task. The source is parsed here, in the request
    /// thread, but everything that touches the network — the catalogue
    /// search, the GitHub lookups, the fetch — runs inside the task.
    private void installSkill(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String source = stringField(body, "source");
        if (source == null || source.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "source is required");
            return;
        }
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        String trimmed = source.trim();
        RepoForm repo = RepoForm.parse(trimmed);
        try {
            TaskService.Task task = tasks.submit("skill-install", id, live -> {
                java.util.function.Consumer<String> report = line -> live.update(line, -1);
                org.jackhuang.hmcl.dsh.DshSkillSource.Bundle bundle;
                if (repo != null) {
                    bundle = org.jackhuang.hmcl.dsh.DshSkillSource.resolve(
                            new org.jackhuang.hmcl.dsh.DshSkillSource.Offering(
                                    "github", repo.source(), repo.source(), repo.name(), "",
                                    repo.ref(), repo.path(), 0));
                } else {
                    report.accept("Searching the skill catalogues for " + trimmed);
                    org.jackhuang.hmcl.dsh.DshSkillSource.Found found =
                            org.jackhuang.hmcl.dsh.DshSkillSource.find(trimmed, 10);
                    org.jackhuang.hmcl.dsh.DshSkillSource.Offering offering = null;
                    for (org.jackhuang.hmcl.dsh.DshSkillSource.Offering candidate : found.skills()) {
                        if (candidate.name().equalsIgnoreCase(trimmed)) {
                            offering = candidate;
                            break;
                        }
                    }
                    if (offering == null && !found.skills().isEmpty()) {
                        offering = found.skills().get(0);
                    }
                    if (offering == null) {
                        throw new DshException("No skill was found for \"" + trimmed + "\""
                                + (found.failures().isEmpty() ? ""
                                : " (unreachable catalogues: "
                                + String.join(", ", found.failures()) + ")"));
                    }
                    bundle = org.jackhuang.hmcl.dsh.DshSkillSource.resolve(offering);
                }
                org.jackhuang.hmcl.dsh.DshSkill installed =
                        org.jackhuang.hmcl.dsh.DshSkillSource.install(
                                instance.homeDirectory(), bundle, report);
                return "Installed skill " + installed.name() + " from " + bundle.repo().fullName();
            });
            accepted(response, task);
        } catch (TaskService.InstanceBusyException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    /// A GitHub spelling of a skill source: `owner/repo`, optionally with a
    /// third segment naming a skill inside a collection, or a full
    /// `https://github.com/owner/repo/tree/<ref>/<path>` address.
    private record RepoForm(String source, String name, @Nullable String ref, @Nullable String path) {

        /// Parses the GitHub forms, or answers `null` when the source is a
        /// catalogue query instead.
        static @Nullable RepoForm parse(String source) {
            String s = source;
            for (String prefix : new String[]{"https://github.com/", "http://github.com/", "github.com/"}) {
                if (s.startsWith(prefix)) {
                    s = s.substring(prefix.length());
                    break;
                }
            }
            if (s.endsWith(".git")) {
                s = s.substring(0, s.length() - ".git".length());
            }
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            String[] parts = s.split("/");
            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            if (parts.length == 2) {
                return new RepoForm(parts[0] + "/" + parts[1], parts[1], null, null);
            }
            if (parts.length >= 4 && "tree".equals(parts[2])) {
                String ref = parts[3];
                String path = parts.length > 4
                        ? String.join("/", java.util.Arrays.copyOfRange(parts, 4, parts.length)) : null;
                String name = path == null ? parts[1] : path.substring(path.lastIndexOf('/') + 1);
                return new RepoForm(parts[0] + "/" + parts[1], name, ref, path);
            }
            if (parts.length == 3) {
                return new RepoForm(parts[0] + "/" + parts[1], parts[2], null, null);
            }
            return null;
        }
    }

    /// Removes one skill pack. `skillId` is the pack's on-disk name (its id in
    /// the list), with its frontmatter name accepted as an alias.
    private void removeSkill(HttpServletResponse response, String id, String skillId)
            throws IOException, DshException {
        DshInstance instance = DshInstanceManager.find(id);
        if (instance == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }
        org.jackhuang.hmcl.dsh.DshSkill found = null;
        for (org.jackhuang.hmcl.dsh.DshSkill skill
                : org.jackhuang.hmcl.dsh.DshSkills.list(instance.homeDirectory())) {
            if (skill.fileName().equals(skillId) || skill.name().equals(skillId)) {
                found = skill;
                break;
            }
        }
        if (found == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "skill not found");
            return;
        }
        org.jackhuang.hmcl.dsh.DshSkills.remove(found);
        Json.write(response, java.util.Map.of("ok", true));
    }

    /// The `specs`-style string array member as a list of non-blank strings,
    /// or `null`.
    private static @Nullable java.util.List<String> stringArrayField(JsonObject body, String name) {
        if (!body.has(name) || !body.get(name).isJsonArray()) {
            return null;
        }
        java.util.List<String> values = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement element : body.getAsJsonArray(name)) {
            if (!element.isJsonPrimitive()) {
                return null;
            }
            String value = element.getAsString();
            if (value == null || value.isBlank()) {
                return null;
            }
            values.add(value.trim());
        }
        return values;
    }

    /// The 202 answer of every task-starting endpoint.
    private static void accepted(HttpServletResponse response, TaskService.Task task) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("taskId", task.id());
        Json.write(response, HttpServletResponse.SC_ACCEPTED, result);
    }

    /// The `specs` member as a list of non-blank strings, or `null`.
    private static @Nullable java.util.List<String> specsField(JsonObject body) {
        if (!body.has("specs") || !body.get("specs").isJsonArray()) {
            return null;
        }
        java.util.List<String> specs = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement element : body.getAsJsonArray("specs")) {
            if (!element.isJsonPrimitive()) {
                return null;
            }
            String spec = element.getAsString();
            if (spec == null || spec.isBlank()) {
                return null;
            }
            specs.add(spec.trim());
        }
        return specs;
    }

    /// The file-name-safe spelling of a package name or version — the same
    /// rule [DshLocalPlugins] names its copies by.
    private static String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // ----------------------------------------------------------------- helpers --

    /// The JSON envelope of one instance, plus `installProgress` where the
    /// contract calls for it.
    private JsonObject instanceBody(HttpServletRequest request, DshInstance instance) {
        JsonObject json = runtime.toJson(instance);
        redactForAgent(request, json);
        return json;
    }

    /// Drops the fields an agent must not carry away.
    ///
    /// `url` is `/i/<id>/?<query>` and dsh puts its access token in that query.
    /// `read` lets an agent see *that* an instance runs, not the credential that
    /// opens it — the same reason `.../open` is closed in
    /// [org.jackhuang.hmcl.web.developer.AgentScopes#forbidden]. A browser keeps
    /// the field; it never crosses into a model's context.
    private static void redactForAgent(HttpServletRequest request, JsonObject json) {
        if (AuthFilter.isAgent(request)) {
            json.remove("url");
        }
    }

    /// Splits the path after `/api/instances` into decoded segments.
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

    private static @Nullable Integer intField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                && object.get(name).getAsJsonPrimitive().isNumber() ? object.get(name).getAsInt() : null;
    }

    private static @Nullable Boolean boolField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                && object.get(name).getAsJsonPrimitive().isBoolean() ? object.get(name).getAsBoolean() : null;
    }

    /// Reads a field the client writes as an array of strings.
    ///
    /// A field that is absent, or that holds something other than an array, is
    /// an empty selection rather than an error: both mean "nothing was
    /// chosen", which is what these selections default to.
    private static java.util.List<String> stringList(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonArray()) {
            return java.util.List.of();
        }
        java.util.List<String> values = new java.util.ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive() && !item.getAsString().isBlank()) {
                values.add(item.getAsString().trim());
            }
        }
        return java.util.List.copyOf(values);
    }

    /// Parses the `homeMode` member; unknown values fall back to the default
    /// rather than failing the request.
    private static @Nullable DshHomeMode homeMode(JsonObject body) {
        String value = stringField(body, "homeMode");
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "global" -> DshHomeMode.GLOBAL;
            case "isolated" -> DshHomeMode.ISOLATED;
            case "version_shared", "shared" -> DshHomeMode.VERSION_SHARED;
            case "custom" -> DshHomeMode.CUSTOM;
            default -> null;
        };
    }

    /// Parses the `portMode` member; `null` when absent, so callers can tell
    /// "not mentioned" from "explicitly automatic".
    private static @Nullable DshPortMode portMode(JsonObject body, @Nullable DshPortMode fallback) {
        String value = stringField(body, "portMode");
        if (value == null) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> DshPortMode.AUTO;
            case "fixed", "manual" -> DshPortMode.FIXED;
            case "global" -> DshPortMode.GLOBAL;
            default -> fallback;
        };
    }

    /// Resolves the game directory a new instance's workspace is scoped to.
    private static GameDirectory resolveDirectory(JsonObject body) {
        String id = stringField(body, "gameDirectoryId");
        if (id != null && !id.isBlank()) {
            for (GameDirectory directory : GameDirectoryManager.getGameDirectories()) {
                if (directory.id().equals(id)) {
                    return directory;
                }
            }
        }
        return GameDirectoryManager.selected();
    }

    /// Turns a display name into a directory-safe, unique instance id.
    ///
    /// Package-visible: the pack servlet names the instance a pack install
    /// creates by the same rule, so an id means the same thing wherever it
    /// came from.
    static String uniqueId(String name) throws DshException {
        String base = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) {
            base = "instance";
        }
        String candidate = base;
        int suffix = 2;
        // A directory counts as taken even when no instance answers to the id: what a half-finished
        // delete leaves behind is a directory under the instances root and nothing else, and
        // `DshInstanceManager.create` refuses to adopt a directory that is already there — so an id
        // that resolved to one would turn a name the user typed into an error rather than a suffix.
        while (DshInstanceManager.exists(candidate)
                || Files.exists(DshPaths.instanceDirectory(candidate), LinkOption.NOFOLLOW_LINKS)
                // `/i/octop/` and `/i/opencode/` are reserved for the last-resort brand mounts.
                || "octop".equals(candidate)
                || "opencode".equals(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    /// The session's username for the audit trail, or null when the request
    /// carries no session (auth disabled, or a hand the filter never saw).
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// The host part of a URL or bare authority string: `https://h/x` and
    /// `h:8080` and `h` all answer `h`. dsh's trustedHosts entries are bare
    /// authorities, and a port-less one matches the host on any port, so the
    /// port is dropped here.
    private static @Nullable String hostOf(String url) {
        String trimmed = url.trim();
        try {
            java.net.URI uri = new java.net.URI(trimmed);
            if (uri.getHost() != null) {
                return uri.getHost();
            }
        } catch (java.net.URISyntaxException ignored) {
            // Fall through to the bare-authority parse.
        }
        try {
            return new java.net.URI("https://" + trimmed).getHost();
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }

}
