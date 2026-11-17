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
package org.jackhuang.hmcl.web.terminal;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jackhuang.hmcl.web.terminal.pty.ConPty;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BooleanSupplier;

/// The terminal routes' shared half: the gates, the takeover of the web
/// runtime, the spawn and the one-use ticket.
///
/// dsh, the brand categories and ZCode each own their instance records and
/// their adapter, but the security decisions and the wire contract must not
/// drift apart between them. A servlet supplies the instance-specific part —
/// what command to start and what to stop first — and this class answers the
/// same `{mode, protocol, instance, brand, ticket, ws}` body for all of them.
@NotNullByDefault
public final class InstanceTerminal {

    /// Builds the argv for one instance. `resume` is the query parameter, or
    /// `null`.
    @FunctionalInterface
    public interface TargetFactory {
        TerminalTarget create(@Nullable String resume) throws IOException;
    }

    /// A refusal, so servlets can turn it into `{"error": code}` without
    /// repeating the checks. The message is the wire text and carries the
    /// machine-readable prefix the clients branch on.
    public static final class Refused extends RuntimeException {
        private final int status;

        public Refused(int status, String code) {
            super(code);
            this.status = status;
        }

        /// The HTTP status to answer with.
        public int status() {
            return status;
        }

        /// The `code: human text` message to answer with.
        public String code() {
            return getMessage();
        }
    }

    private final ServerConfig config;
    private final TerminalManager terminals;
    private final TerminalTickets tickets;
    private final AuthService auth;
    private final DeveloperAuth developerAuth;
    private final DeveloperElevation developerElevation;
    private final BooleanSupplier tlsActive;
    private final org.jackhuang.hmcl.web.emergency.EmergencyStop emergency;

    public InstanceTerminal(ServerConfig config, TerminalManager terminals, TerminalTickets tickets,
                            AuthService auth, DeveloperAuth developerAuth,
                            DeveloperElevation developerElevation, BooleanSupplier tlsActive) {
        this(config, terminals, tickets, auth, developerAuth, developerElevation, tlsActive,
                new org.jackhuang.hmcl.web.emergency.EmergencyStop());
    }

    /// @param emergency the panel's emergency stop; while it is engaged every
    ///                  terminal request is refused, whatever the caller proved
    public InstanceTerminal(ServerConfig config, TerminalManager terminals, TerminalTickets tickets,
                            AuthService auth, DeveloperAuth developerAuth,
                            DeveloperElevation developerElevation, BooleanSupplier tlsActive,
                            org.jackhuang.hmcl.web.emergency.EmergencyStop emergency) {
        this.config = config;
        this.terminals = terminals;
        this.tickets = tickets;
        this.auth = auth;
        this.developerAuth = developerAuth;
        this.developerElevation = developerElevation;
        this.tlsActive = tlsActive;
        this.emergency = emergency;
    }

    /// The gates every terminal request passes before anything is started:
    /// the emergency stop, the web shell switch, HTTPS, a developer password
    /// and its elevation.
    ///
    /// @throws Refused with the status and code the caller should answer
    public void authorize(HttpServletRequest request) {
        if (emergency.isEngaged()) {
            throw new Refused(503, "emergency_stop: the panel is in emergency stop; terminals are frozen");
        }
        if (!config.webShell.enabled) {
            throw new Refused(403, "web_shell_disabled: the web shell is disabled in developer settings");
        }
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            throw new Refused(403,
                    "insecure_transport: the web shell requires HTTPS; enable it in the HTTPS settings");
        }
        if (!developerAuth.isSet()) {
            throw new Refused(409, "developer_password_not_set: set it before opening a terminal");
        }
        String token = sessionToken(request);
        boolean sessionAlive = token == null || token.isBlank() || auth.isAlive(token);
        String key = DeveloperElevation.key(token, request.getRemoteAddr());
        // 终端对 agent 也开放（scope 已经在 AuthFilter 验过）：它能管别的实例，
        // 就能在那个实例里开终端；自己的实例由 `selfFatal` 挡掉，因为开终端会先
        // 停掉正在服务的 web runtime。要求它输开发者密码只会让它永远做不到。
        if (!AuthFilter.elevationSatisfied(
                request, developerElevation, key, sessionAlive)) {
            throw new Refused(403, "developer_elevation_required: elevate with the developer password first");
        }
    }

    /// Stops the instance's terminal, if one is running.
    ///
    /// @return whether there was one
    public boolean close(String brand, String instanceId) {
        return terminals.terminate(brand, instanceId);
    }

    /// Whether an interactive terminal is running for this instance.
    ///
    /// The web mode and the terminal are exclusive (docs/brands.md), so the
    /// instance list carries this flag: the UI asks before one mode ends the
    /// other instead of the switch happening silently behind the click.
    public boolean isOpen(String brand, String instanceId) {
        return terminals.find(brand, instanceId)
                .map(session -> session.state() == TerminalSession.State.RUNNING)
                .orElse(false);
    }

    /// Takes the instance over and answers the terminal body.
    ///
    /// `stopWeb` brings the instance's web mode down before the TUI starts:
    /// both share the agent's state, and on POSIX both would write the same
    /// session files. It must return once the process is really gone — an
    /// asynchronous stop followed by a spawn races two processes over one home.
    ///
    /// A Windows panel without a pseudo console is refused **before** `stopWeb`:
    /// a machine that cannot start a terminal should not be the reason the
    /// instance's web mode went down. The check is a pre-check, not a branch —
    /// every platform that passes it takes the same `terminals.open` path.
    ///
    /// @throws Refused when the PTY or the agent cannot be started, or when this
    ///                 Windows build has no ConPTY
    public JsonObject open(HttpServletRequest request, String instanceId,
                           @Nullable Runnable stopWeb, TargetFactory factory) throws IOException {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && !ConPty.platformSupported()) {
            throw new Refused(409, "terminal_unsupported_platform: " + ConPty.UNSUPPORTED_MESSAGE);
        }
        if (stopWeb != null) {
            stopWeb.run();
        }
        TerminalTarget target;
        try {
            target = factory.create(request.getParameter("resume"));
        } catch (IOException | RuntimeException e) {
            AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.open", instanceId, "failure",
                    actorOf(request), request.getRemoteAddr());
            throw new Refused(409, "terminal_failed: " + e.getMessage());
        }
        // A refresh comes back to the same agent: an existing running session
        // gets a fresh ticket instead of a 409. The process outlives the tab.
        try {
            TerminalSession running = terminals.find(target.brand(), instanceId)
                    .filter(candidate -> candidate.state() == TerminalSession.State.RUNNING)
                    .orElse(null);
            if (running == null) {
                terminals.open(target, PtySize.DEFAULT);
            }
        } catch (TerminalBusyException e) {
            throw new Refused(409, "terminal_already_open: " + e.getMessage());
        } catch (IOException | RuntimeException e) {
            AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.open", instanceId, "failure",
                    actorOf(request), request.getRemoteAddr());
            throw new Refused(409, "terminal_failed: " + e.getMessage());
        }
        String ticket = tickets.issue(instanceId, target.brand(), sessionToken(request));
        JsonObject body = new JsonObject();
        body.addProperty("mode", "pty");
        body.addProperty("protocol", TerminalWebSocket.PROTOCOL);
        body.addProperty("instance", instanceId);
        body.addProperty("brand", target.brand());
        body.addProperty("ticket", ticket);
        body.addProperty("ws", "/ws/terminal/" + ticket);
        return body;
    }

    /// Answers a refusal in the shape the clients parse.
    public static void write(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                             Refused refused) throws IOException {
        Json.error(response, refused.status(), refused.code());
    }

    private static @Nullable String sessionToken(HttpServletRequest request) {
        return (String) request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
    }

    /// Who asked, for the audit trail. `null` when the panel runs with
    /// authentication disabled (loopback-only).
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session value ? value.username() : null;
    }
}