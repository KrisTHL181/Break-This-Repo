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
package org.jackhuang.hmcl.web.developer;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/// The capability classes an **agent** token may hold, and the two rules that
/// keep it from doing something no agent should be able to do.
///
/// A token minted through `POST /api/developer/agent-auth` — the agent signs
/// for it with the agent password — is a bearer credential an AI agent carries
/// on its own; the panel never sees a browser session behind it. The design
/// keeps the blast radius small in the only place the agent cannot rewrite —
/// **here, in the root process**:
///
/// 1. **Deny by default.** A request that matches no class below is refused,
///    so a route added later is closed to agents until someone says otherwise.
/// 2. **Self-fatal operations are refused.** Stopping the instance whose
///    process is running the call, or opening its terminal (which stops the web
///    runtime first), would kill the caller mid-request: the tool call could
///    never return and the transcript would end in silence. The panel answers
///    `409 self_operation_refused` instead, and the agent is expected to pick
///    another route.
///
/// Credential-leaking reads are refused outright: `GET /api/instances/{id}/open`
/// answers a URL carrying the instance's access token, `/api/exports/*` serves
/// build artifacts that may embed session content, and a brand instance's
/// `admin-password` is a generated credential — the first-run window hands it
/// out without any elevation, so only this layer can close it. None of them
/// belongs in a model's context.
///
/// The scope model also stops at the API boundary: `/i/*` (the reverse proxy)
/// and `/ws/*` (the event streams) are browser channels and are never reachable
/// with a grant, whatever class it names. An agent that needs an instance's
/// state asks the API for it; the panel then records who asked.
@NotNullByDefault
public final class AgentScopes {

    /// Every `GET`/`HEAD` the panel serves, minus the leaks below.
    public static final String READ = "read";
    /// `/api/mc/*`: the only surface where a stray process would corrupt state
    /// the panel owns (port, world, console).
    public static final String MC = "mc";
    /// The MC server's **PTY console**: attaching to `/ws/terminal/{ticket}`.
    /// It sits on top of [MC] rather than inside it because attaching is not
    /// one more command — it is a live, bidirectional terminal into the server
    /// (history, output stream, everything typed). An operator who lets an
    /// agent start and stop servers may still refuse to hand it that.
    public static final String MC_CONSOLE = "mc-console";
    /// `/api/system/*`: the host's own readings (CPU, memory, disk, network).
    /// Separate from [READ] so a ticket that may read the panel does not
    /// automatically get a picture of the machine it runs on. It is not a
    /// security boundary — an agent with a shell can read `/proc` itself — but
    /// it is an explicit, auditable "yes" instead of an accident.
    public static final String SYSTEM = "system";
    /// Creating, deleting, launching and stopping **other** instances.
    public static final String INSTANCES = "instances";
    /// Accounts, models, TLS, network trust, node runtimes, wallpapers.
    public static final String CONFIG = "config";
    /// Installing or removing plugins, packs and skills.
    public static final String PLUGINS = "plugins";
    /// The transcode queue, presets and job control.
    public static final String TRANSCODE = "transcode";
    /// Raw command arguments, media browsing and destructive file operations.
    public static final String TRANSCODE_RAW = "transcode-raw";
    /// Read chat assistants, sessions, messages and stored sources.
    public static final String CHAT_READ = "chat-read";
    /// Create sessions, send/edit/regenerate messages, cancel turns and upload attachments.
    public static final String CHAT_WRITE = "chat-write";
    /// Run a provider-backed search test / query.
    public static final String CHAT_SEARCH = "chat-search";

    /// Every class a grant may name, in the order the panel presents them.
    public static final Set<String> KNOWN = Set.of(
            READ, MC, MC_CONSOLE, SYSTEM, INSTANCES, CONFIG, PLUGINS, TRANSCODE, TRANSCODE_RAW,
            CHAT_READ, CHAT_WRITE, CHAT_SEARCH);

    /// What a grant holds when the operator ticks nothing: reading the panel,
    /// and nothing that changes it.
    public static final Set<String> DEFAULT = Set.of(READ);

    private AgentScopes() {
    }

    /// Whether `scope` names a class this build knows.
    ///
    /// @param scope the claimed class
    /// @return whether it is known
    public static boolean known(@Nullable String scope) {
        return scope != null && KNOWN.contains(scope);
    }

    /// Routes an agent token may never reach, whatever it was granted.
    ///
    /// The developer routes mint and revoke credentials, so an agent holding
    /// one must not reach them; the auth routes would let it change the
    /// operator's password; the reads below hand out another credential.
    ///
    /// The path must already be the **decoded** one the servlet routes on (see
    /// `AuthFilter#pathOf`) — otherwise `%61dmin-password` passes this check and
    /// the servlet decodes it back into the closed route.
    ///
    /// @param method the request method
    /// @param path   the decoded request path
    /// @return whether the route is closed to agents
    public static boolean forbidden(@Nullable String method, @Nullable String path) {
        if (path == null) {
            return true;
        }
        String route = stripTrailingSlash(path);
        if (route.startsWith("/api/developer/") || route.equals("/api/developer")) {
            return true;
        }
        if (route.startsWith("/api/auth/")) {
            return true;
        }
        // Enabling or disabling the surface is an operator decision (it needs the
        // developer password), and an agent never gets to make it.
        if (route.startsWith("/api/transcode/feature")) {
            return true;
        }
        // Settings are an operator decision; an agent may run jobs, not change
        // the unattended failure policy behind the operator's back.
        if (route.startsWith("/api/transcode/settings")
                || route.startsWith("/api/transcode/runtime/")) {
            return true;
        }
        if (route.startsWith("/api/exports")) {
            return true;
        }
        // A generated admin password is a credential, not a setting: the panel
        // hands it to the operator's browser, never to a model's context. It is
        // closed here rather than in the servlet because the first-run window
        // (`GET .../admin-password` on a fresh instance) answers **without any
        // elevation at all** — a scope check in the servlet would never run.
        if (route.endsWith("/admin-password")) {
            return true;
        }
        return ("GET".equals(method) || "HEAD".equals(method)) && route.endsWith("/open");
    }

    /// Drops one trailing slash so a route matches whether or not the caller
    /// wrote it, while leaving "/" alone.
    private static String stripTrailingSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /// Whether `path` is the MC console's PTY ticket route.
    ///
    /// `/api/mc/instances/{id}/console/ticket` (with or without the trailing
    /// slash the dispatcher ignores). `console/input` is deliberately not here:
    /// one command through the server's stdin stays ordinary `mc` work.
    private static boolean consoleTicket(String path) {
        String route = stripTrailingSlash(path);
        return route.startsWith("/api/mc/instances/") && route.endsWith("/console/ticket");
    }

    /// The class a request needs, or `null` when no class covers it — which the
    /// caller must read as a refusal, not as "no class needed".
    ///
    /// @param method the request method
    /// @param path   the request path
    /// @return the required class, or `null` for "not covered"
    public static @Nullable String required(@Nullable String method, @Nullable String path) {
        if (path == null || method == null) {
            return null;
        }
        // Only `/api/*` is an API. The reverse proxy (`/i/*`), the event
        // streams (`/ws/*`) and the static shell are browser channels: an agent
        // reaches the panel through the API or not at all. The `GET -> READ`
        // shortcut below must not swallow them — before this guard a
        // `GET /i/<id>/...` counted as a read, so a `read` grant could have
        // pulled a proxied instance page into a model's context.
        if (!path.startsWith("/api/") && !path.equals("/api")) {
            return null;
        }
        // The machine's readings are their own class, checked before the
        // "every GET is a read" shortcut below: a read-only ticket must not
        // inherit them by accident.
        if (path.startsWith("/api/system/") || path.equals("/api/system")) {
            return SYSTEM;
        }
        if (path.startsWith("/api/transcode/")) {
            if (path.startsWith("/api/transcode/media/")) {
                return TRANSCODE_RAW;
            }
            return TRANSCODE;
        }
        if (path.equals("/api/chat") || path.startsWith("/api/chat/")) {
            return chatScope(method, path);
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return READ;
        }
        if (path.startsWith("/api/mc/") || path.equals("/api/mc")) {
            // The PTY ticket is the one MC route that hands over a live
            // terminal instead of running one command: it asks for its own
            // class, so `mc` alone keeps its old reach (start/stop, input,
            // logs) and gains no attach.
            return consoleTicket(path) ? MC_CONSOLE : MC;
        }
        if (path.startsWith("/api/instances")) {
            return path.contains("/plugins") ? PLUGINS : INSTANCES;
        }
        if (path.startsWith("/api/zcode/") || path.startsWith("/api/brands/")) {
            return INSTANCES;
        }
        if (path.startsWith("/api/plugins/") || path.startsWith("/api/packs/")) {
            return PLUGINS;
        }
        if (path.startsWith("/api/accounts") || path.startsWith("/api/models")
                || path.startsWith("/api/settings") || path.startsWith("/api/network")
                || path.startsWith("/api/node") || path.startsWith("/api/vendors")
                || path.startsWith("/api/wallpapers")) {
            return CONFIG;
        }
        if (path.startsWith("/api/tasks")) {
            return INSTANCES;
        }
        return null;
    }

    /// The scope a chat route needs. Admin/config routes stay closed to agent
    /// tickets (they also require developer elevation in the servlet); deletes
    /// are closed as well, so a `chat-write` ticket can speak but not erase.
    private static @Nullable String chatScope(String method, String path) {
        String route = stripTrailingSlash(path);
        if (route.equals("/api/chat")) {
            return null;
        }
        if (route.equals("/api/chat/settings") || route.startsWith("/api/chat/backup")
                || route.startsWith("/api/chat/accounts") || route.startsWith("/api/chat/profile")
                || route.startsWith("/api/chat/avatars")
                || (route.startsWith("/api/chat/assistants") && !"GET".equals(method))) {
            return null;
        }
        if (route.equals("/api/chat/search/status") || route.equals("/api/chat/search/test")) {
            return CHAT_SEARCH;
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return CHAT_READ;
        }
        if ("DELETE".equals(method)) {
            return null;
        }
        if (route.equals("/api/chat/sessions") || route.startsWith("/api/chat/sessions/")
                || route.startsWith("/api/chat/turns/")
                || route.startsWith("/api/chat/attachments")) {
            return CHAT_WRITE;
        }
        return null;
    }

    /// The instance a route would end or take over, with the family that owns
    /// its id space and whether the ending is conditional.
    ///
    /// `launch` is not unconditionally fatal — it closes the target's terminal
    /// first, so it ends the caller only when the caller lives in that terminal
    /// (or when the panel cannot tell). The three hard operations (`stop`,
    /// `delete`, `terminal`) end whoever is running inside the target, whatever
    /// mode it is in.
    ///
    /// @param family the id-space owner: `dsh`, a brand id, or `zcode`
    /// @param id     the instance id the route names
    /// @param launch whether this is the conditional case
    public record Fatal(String family, String id, boolean launch) {

        /// Whether a grant bound to `bound` names this target.
        ///
        /// A binding the panel derived itself is `family/id`; one the caller
        /// claimed at mint time is a bare id, matched on the id alone — the
        /// spaces do not collide in practice, and a wrong claim only produces
        /// an extra refusal.
        ///
        /// @param bound the grant's instance binding, or `null`
        /// @return whether the binding names this target
        public boolean matchesBound(@Nullable String bound) {
            if (bound == null || bound.isBlank()) {
                return false;
            }
            int slash = bound.indexOf('/');
            if (slash < 0) {
                return bound.equals(id);
            }
            return family.equals(bound.substring(0, slash)) && id.equals(bound.substring(slash + 1));
        }

        /// Whether an identity the panel derived names this target.
        ///
        /// @param refFamily the derived caller's family
        /// @param refId     the derived caller's instance id
        /// @return whether both name the same instance
        public boolean matches(String refFamily, String refId) {
            return family.equals(refFamily) && id.equals(refId);
        }
    }

    /// The instance a fatal route targets, or `null` when the route is not one.
    ///
    /// Three families write the same operations under different prefixes — dsh
    /// (`/api/instances/{id}/…`), brands (`/api/brands/{brand}/instances/{id}/…`)
    /// and ZCode (`/api/zcode/instances/{id}/…`). Every one of them must be
    /// recognised: covering only the dsh shape once left the other two free to
    /// end the instance the caller was running in, which is the same death
    /// under another URL. The path must be the **decoded** one the servlet
    /// routes on, exactly like [#forbidden].
    ///
    /// @param method the request method
    /// @param path   the decoded request path
    /// @return the target, or `null` for a route that ends nobody
    public static @Nullable Fatal fatalTarget(@Nullable String method, @Nullable String path) {
        if (method == null || path == null) {
            return null;
        }
        String route = stripTrailingSlash(path);
        String[] parts = route.split("/", -1);
        // ["", "api", family, …]
        if (parts.length < 4 || !"api".equals(parts[1])) {
            return null;
        }
        switch (parts[2]) {
            case "instances" -> {
                // dsh: ["", "api", "instances", id, op?]
                if (parts.length == 4 && "DELETE".equals(method)) {
                    return new Fatal("dsh", parts[3], false);
                }
                return parts.length == 5 ? operation("dsh", parts[3], parts[4], method) : null;
            }
            case "zcode" -> {
                // ["", "api", "zcode", "instances", id, op?]
                if (parts.length < 5 || !"instances".equals(parts[3])) {
                    return null;
                }
                String id = parts[4];
                if (parts.length == 5 && "DELETE".equals(method)) {
                    return new Fatal("zcode", id, false);
                }
                return parts.length == 6 ? operation("zcode", id, parts[5], method) : null;
            }
            case "brands" -> {
                // ["", "api", "brands", brand, "instances", id, op?]
                if (parts.length < 6 || !"instances".equals(parts[4])) {
                    return null;
                }
                String id = parts[5];
                if (parts.length == 6 && "DELETE".equals(method)) {
                    return new Fatal(parts[3], id, false);
                }
                return parts.length == 7 ? operation(parts[3], id, parts[6], method) : null;
            }
            default -> {
                return null;
            }
        }
    }

    /// The fatal operation one family serves under `{base}/{op}`.
    ///
    /// `terminal` is fatal under any method: `POST` opens it (stopping the web
    /// runtime first) and `DELETE` kills it, and the caller may be running in
    /// either.
    private static @Nullable Fatal operation(String family, String id, String op, String method) {
        switch (op) {
            case "stop" -> {
                return "POST".equals(method) ? new Fatal(family, id, false) : null;
            }
            case "terminal" -> {
                return new Fatal(family, id, false);
            }
            case "launch" -> {
                return "POST".equals(method) ? new Fatal(family, id, true) : null;
            }
            default -> {
                return null;
            }
        }
    }

    /// Whether this request would end the process making it.
    ///
    /// Only the instance the grant is bound to counts: stopping *another*
    /// instance is ordinary work, and the agent may do it when its grant holds
    /// [#INSTANCES]. Opening a terminal is included because the launcher stops
    /// the web runtime first — the same death by a quieter route. `launch` is
    /// deliberately not here: it ends the caller only when the target's
    /// terminal is open, a condition this static answer cannot see.
    ///
    /// @param method     the request method
    /// @param path       the request path
    /// @param instanceId the instance the grant is bound to, or `null`
    /// @return whether the panel must refuse the request
    public static boolean selfFatal(@Nullable String method, @Nullable String path, @Nullable String instanceId) {
        Fatal target = fatalTarget(method, path);
        return target != null && !target.launch() && target.matchesBound(instanceId);
    }
}
