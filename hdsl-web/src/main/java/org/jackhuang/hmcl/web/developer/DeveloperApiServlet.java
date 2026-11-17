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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.auth.LoginThrottle;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jackhuang.hmcl.web.emergency.EmergencyStop;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/// The developer mode's own door: the second factor behind the web terminal.
///
/// `POST /api/developer/elevate` proves the password; `GET /api/developer/status`
/// answers whether the caller is elevated (and when that expires) and whether
/// the Web Shell is on; `POST /api/developer/extend` stretches an existing
/// elevation to [DeveloperElevation.EXTENDED_TTL]; `POST /api/developer/revoke`
/// drops it early; `POST /api/developer/password` sets or changes it (current
/// developer password + panel password confirmation, like the TLS settings
/// route); `POST /api/developer/password/clear` removes it;
/// `POST /api/developer/web-shell` flips the instance-terminal switch.
///
/// The routes are backed by [DeveloperElevation] and [DeveloperAuth], which
/// the terminal's own gate shares.
@NotNullByDefault
public final class DeveloperApiServlet extends HttpServlet {

    private final DeveloperAuth devAuth;
    private final DeveloperElevation elevation;
    private final AuthService auth;
    private final ServerConfig config;
    private final BooleanSupplier tlsActive;
    private final EmergencyStop emergency;
    private final TerminalManager terminals;
    private final InstanceRuntime runtime;
    /// Which instance a caller runs in, derived from the kernel: the mint
    /// binds the grant to it, so the binding is a fact rather than a claim.
    private final org.jackhuang.hmcl.web.auth.PeerIdentity identity;
    /// The panel's real serving port, live: the connector picks an ephemeral
    /// port when the configuration asks for one, and the loopback address the
    /// prompt hands to an agent must name whatever the panel actually runs on,
    /// not the default.
    private final java.util.function.IntSupplier panelPort;
    /// Failed panel-password confirmations, per source address — the second
    /// confirmation must not become an unthrottled oracle for the panel
    /// password.
    private final LoginThrottle adminPasswordThrottle = new LoginThrottle();
    /// The agent password's own store: a lesser secret with its own cap, kept
    /// in a different file so the browser's elevate form can never accept it.
    private final DeveloperAuth agentAuth;
    /// Failed agent-password attempts, per source address. The agent route is
    /// reachable without a session, so it needs the same shape of limiter the
    /// sign-in gate uses.
    private final LoginThrottle agentPasswordThrottle = new LoginThrottle();

    public DeveloperApiServlet(DeveloperAuth devAuth, DeveloperAuth agentAuth,
                               DeveloperElevation elevation,
                               AuthService auth, ServerConfig config, BooleanSupplier tlsActive,
                               EmergencyStop emergency, TerminalManager terminals, InstanceRuntime runtime,
                               org.jackhuang.hmcl.web.auth.PeerIdentity identity,
                               java.util.function.IntSupplier panelPort) {
        this.devAuth = devAuth;
        this.agentAuth = agentAuth;
        this.elevation = elevation;
        this.auth = auth;
        this.config = config;
        this.tlsActive = tlsActive;
        this.emergency = emergency;
        this.terminals = terminals;
        this.runtime = runtime;
        this.identity = identity;
        this.panelPort = panelPort;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        String method = request.getMethod();
        if ("/status".equals(path) && "GET".equals(method)) {
            String key = keyOf(request);
            JsonObject body = new JsonObject();
            body.addProperty("devPasswordSet", devAuth.isSet());
            body.addProperty("elevated", elevation.isElevated(key, sessionAlive(request)));
            Instant expiry = elevation.expiryOf(key, sessionAlive(request));
            if (expiry != null) {
                body.addProperty("expiresAt", expiry.toString());
            }
            body.addProperty("webShellEnabled", config.webShell.enabled);
            body.addProperty("https",
                    RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config));
            body.addProperty("guardScope", config.developerGuard.scope.wireName());
            body.add("estop", estopState());
            Json.write(response, body);
            return;
        }
        if ("/guard".equals(path) && "POST".equals(method)) {
            setGuard(request, response);
            return;
        }
        if ("/estop/preview".equals(path) && "GET".equals(method)) {
            estopPreview(request, response);
            return;
        }
        if ("/estop/engage".equals(path) && "POST".equals(method)) {
            estopEngage(request, response);
            return;
        }
        if ("/estop/release".equals(path) && "POST".equals(method)) {
            estopRelease(request, response);
            return;
        }
        if ("/elevate".equals(path) && "POST".equals(method)) {
            elevate(request, response);
            return;
        }
        if ("/extend".equals(path) && "POST".equals(method)) {
            extend(request, response);
            return;
        }
        if ("/revoke".equals(path) && "POST".equals(method)) {
            revoke(request, response);
            return;
        }
        if ("/agent-grants".equals(path) && "GET".equals(method)) {
            listAgentGrants(response);
            return;
        }
        if ("/agent-password".equals(path) && "GET".equals(method)) {
            agentPasswordStatus(response);
            return;
        }
        if ("/agent-password".equals(path) && "POST".equals(method)) {
            setAgentPassword(request, response);
            return;
        }
        if ("/agent-password/scopes".equals(path) && "POST".equals(method)) {
            setAgentScopes(request, response);
            return;
        }
        if ("/agent-password/clear".equals(path) && "POST".equals(method)) {
            clearAgentPassword(request, response);
            return;
        }
        if ("/agent-loopback".equals(path) && "POST".equals(method)) {
            setAgentLoopback(request, response);
            return;
        }
        if ("/agent-auth".equals(path) && "POST".equals(method)) {
            agentAuth(request, response);
            return;
        }
        if ("/agent-token".equals(path) && "POST".equals(method)) {
            mintAgentToken(request, response);
            return;
        }
        if (path != null && path.startsWith("/agent-grants/") && "DELETE".equals(method)) {
            revokeAgentGrant(response, path.substring("/agent-grants/".length()));
            return;
        }
        if ("/password".equals(path) && "POST".equals(method)) {
            setPassword(request, response);
            return;
        }
        if ("/password/clear".equals(path) && "POST".equals(method)) {
            clearPassword(request, response);
            return;
        }
        if ("/web-shell".equals(path) && "POST".equals(method)) {
            setWebShell(request, response);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void elevate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        String password = stringField(readJson(request), "password");
        if (password == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "password is required");
            return;
        }
        String key = keyOf(request);
        Instant now = Instant.now();
        if (elevation.isLocked(key, now)) {
            Json.error(response, 429, "too many failed attempts; this session is locked for "
                    + DeveloperElevation.LOCK.toMinutes() + " minutes");
            return;
        }
        if (!devAuth.isSet()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "developer_password_not_set: set it before opening a terminal");
            return;
        }
        if (!devAuth.verify(password)) {
            boolean locked = elevation.recordFailure(key, now);
            if (locked) {
                Json.error(response, 429, "too many failed attempts; this session is locked for "
                        + DeveloperElevation.LOCK.toMinutes() + " minutes");
            } else {
                Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid developer password");
            }
            return;
        }
        elevation.elevate(key);
        elevation.clearFailures(key);
        JsonObject body = new JsonObject();
        body.addProperty("elevated", true);
        body.addProperty("expiresAt", now.plus(DeveloperElevation.TTL).toString());
        // A bearer token for callers that cannot keep the session cookie: a host
        // script, an inspection agent, CI. Same TTL and sliding rules as the
        // cookie elevation, in memory only, and killed by a password change.
        body.addProperty("token", elevation.mintToken());
        Json.write(response, body);
    }

    /// The developer page's 「延长提权」: an already-elevated session spends
    /// its ticket's single extension coupon and stretches itself to
    /// [DeveloperElevation.EXTENDED_TTL] without re-typing the password. The
    /// coupon is one-shot by design — a second stretch takes a fresh password
    /// proof — and extending requires the elevation it lengthens, otherwise
    /// the endpoint would be a second, password-less mint.
    private void extend(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        String key = keyOf(request);
        if (!elevation.require(key, sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        Instant expiry = elevation.extend(key, DeveloperElevation.EXTENDED_TTL);
        if (expiry == null) {
            // require() 刚放行并滑动过有效期，能走到这的只有「延长过一次」。
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_extended: 这次提权已经延长过一次，重新输入开发者密码后可再次延长");
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "extend", "success",
                actorOf(request), request.getRemoteAddr());
        JsonObject body = new JsonObject();
        body.addProperty("elevated", true);
        body.addProperty("expiresAt", expiry.toString());
        Json.write(response, body);
    }

    /// `GET /api/developer/agent-grants`: every live agent grant, for the
    /// panel's list. The tokens themselves never leave the server.
    private void listAgentGrants(HttpServletResponse response) throws IOException {
        JsonObject answer = new JsonObject();
        JsonArray array = new JsonArray();
        for (var grant : elevation.agentGrants()) {
            JsonObject row = new JsonObject();
            row.addProperty("id", grant.id());
            row.addProperty("label", grant.label());
            row.addProperty("expiresAt", grant.expiresAt().toString());
            if (grant.instanceId() != null) {
                row.addProperty("instanceId", grant.instanceId());
            }
            JsonArray scopes = new JsonArray();
            grant.scopes().forEach(scopes::add);
            row.add("scopes", scopes);
            array.add(row);
        }
        answer.add("grants", array);
        Json.write(response, answer);
    }

    /// `DELETE /api/developer/agent-grants/{id}`: the 「掐掉」 button. The token
    /// stops working on its next request — no waiting for the expiry it was
    /// given.
    private void revokeAgentGrant(HttpServletResponse response, String id) throws IOException {
        boolean removed = elevation.revokeAgent(id);
        if (!removed) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such grant");
            return;
        }
        // A revoked credential must not keep a live MC console: the socket is
        // not re-checked per byte, so revocation has to find it by its handle.
        org.jackhuang.hmcl.web.terminal.TerminalWebSocket.closeForAgent(id);
        AuditLog.log(AuditLog.Channel.WEB, "developer.agent", id, "revoke", null, null);
        Json.write(response, java.util.Map.of("ok", true));
    }

    /// Drops every agent grant **and** the console sockets they opened.
    ///
    /// The ids are taken before the registry is emptied because the sockets are
    /// tracked by grant id, and a grant whose human elevation already ended is
    /// not in [DeveloperElevation#agentGrants] any more — but its socket is
    /// still open, so [#agentGrantIds] is the list to use.
    ///
    /// @return how many grants were dropped
    private int dropAgentGrants() {
        java.util.List<String> ids = elevation.agentGrantIds();
        int dropped = elevation.revokeAllAgents();
        for (String id : ids) {
            org.jackhuang.hmcl.web.terminal.TerminalWebSocket.closeForAgent(id);
        }
        return dropped;
    }

    /// Ends **every** elevation, human and agent, and closes the console
    /// sockets the agents opened. Used when the developer password is changed
    /// or cleared: proof of the old secret must be worthless, and a live
    /// console is proof that outlived its credential.
    private void clearAllElevations() {
        java.util.List<String> ids = elevation.agentGrantIds();
        elevation.clearAll();
        for (String id : ids) {
            org.jackhuang.hmcl.web.terminal.TerminalWebSocket.closeForAgent(id);
        }
    }

    /// `GET /api/developer/agent-password`: whether the lesser secret exists,
    /// what it may grant, and whether this guard level can use it at all.
    private void agentPasswordStatus(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("set", agentAuth.isSet());
        body.addProperty("applicable", config.developerGuard.scope.coversInstanceActions());
        JsonArray scopes = new JsonArray();
        java.util.List<String> cap = agentAuth.scopes();
        if (cap != null) {
            cap.forEach(scopes::add);
        }
        body.add("scopes", scopes);
        body.addProperty("loopbackOnly", config.agent.loopbackOnly);
        body.addProperty("remoteNeedsSession", config.agent.remoteNeedsSession);
        Json.write(response, body);
    }

    /// `POST /api/developer/agent-password {password, scopes}`: sets the lesser
    /// secret and the **cap** it may grant.
    ///
    /// The cap is the whole point. An operator who does not want to hand over
    /// the developer password hands over this one instead, and says here how
    /// far it reaches — `["read"]`, or `["read","mc"]`, and only if they really
    /// mean it `["read","mc","instances","config","plugins"]`.
    private void setAgentPassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        JsonObject body = readJson(request);
        String password = stringField(body, "password");
        if (password == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "password is required");
            return;
        }
        Set<String> scopes = new LinkedHashSet<>();
        if (body != null && body.has("scopes") && body.get("scopes").isJsonArray()) {
            for (var element : body.getAsJsonArray("scopes")) {
                if (element.isJsonPrimitive()) {
                    scopes.add(element.getAsString());
                }
            }
        }
        if (scopes.isEmpty()) {
            scopes.addAll(AgentScopes.DEFAULT);
        }
        for (String scope : scopes) {
            if (!AgentScopes.known(scope)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown_scope: " + scope + " (known: " + AgentScopes.KNOWN + ")");
                return;
            }
        }
        try {
            agentAuth.set(password, auth.userStore(), java.util.List.copyOf(scopes));
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        // A new agent password kills every grant the old one minted: rotating a
        // secret must not leave the previous holders running.
        dropAgentGrants();
        AuditLog.log(AuditLog.Channel.WEB, "developer.agentPassword", null, "set",
                actorOf(request), request.getRemoteAddr());
        agentPasswordStatus(response);
    }

    /// `POST /api/developer/agent-password/scopes {scopes}`: changes **only** the
    /// cap, without the password.
    ///
    /// Retyping the secret to narrow what it may grant was backwards: the
    /// operator is reducing risk, and the page made that the hardest edit on
    /// it. The hash is untouched; every grant the old cap minted is dropped,
    /// because those tickets were wider than the policy now says.
    private void setAgentScopes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        if (!agentAuth.isSet()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "agent_password_not_set: set the agent password first");
            return;
        }
        JsonObject body = readJson(request);
        Set<String> scopes = new LinkedHashSet<>();
        if (body != null && body.has("scopes") && body.get("scopes").isJsonArray()) {
            for (var element : body.getAsJsonArray("scopes")) {
                if (element.isJsonPrimitive()) {
                    scopes.add(element.getAsString());
                }
            }
        }
        if (scopes.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "scopes must name at least one class");
            return;
        }
        for (String scope : scopes) {
            if (!AgentScopes.known(scope)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown_scope: " + scope + " (known: " + AgentScopes.KNOWN + ")");
                return;
            }
        }
        try {
            agentAuth.setScopes(java.util.List.copyOf(scopes));
        } catch (IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "agent_password_not_set");
            return;
        }
        int dropped = dropAgentGrants();
        AuditLog.log(AuditLog.Channel.WEB, "developer.agentPassword", null,
                "scopes (" + dropped + " grant(s) dropped)", actorOf(request), request.getRemoteAddr());
        agentPasswordStatus(response);
    }

    /// `POST /api/developer/agent-loopback {loopbackOnly}`: whether tickets
    /// are honoured from the panel's loopback only.
    ///
    /// On by default, because an agent runs beside the panel: a ticket arriving
    /// from a LAN or public address has travelled further than a credential
    /// meant for a local tool should. Turning it off is a deployment decision
    /// (a remote agent the operator really wants), and the finer
    /// `agent.remote_needs_session` key still applies to what is left.
    private void setAgentLoopback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        Boolean loopbackOnly = booleanField(readJson(request), "loopbackOnly");
        if (loopbackOnly == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "loopbackOnly is required");
            return;
        }
        try {
            ServerConfigWriter.updateAgentLoopbackOnly(config.dataDir, loopbackOnly);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the agent source rule could not be stored: " + e.getMessage());
            return;
        }
        config.agent.loopbackOnly = loopbackOnly;
        AuditLog.log(AuditLog.Channel.WEB, "developer.agentLoopback", null,
                loopbackOnly ? "loopback-only" : "allow-remote",
                actorOf(request), request.getRemoteAddr());
        agentPasswordStatus(response);
    }

    /// `POST /api/developer/agent-password/clear`: removes the lesser secret
    /// and every grant it ever minted.
    private void clearAgentPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        agentAuth.clear();
        dropAgentGrants();
        AuditLog.log(AuditLog.Channel.WEB, "developer.agentPassword", null, "clear",
                actorOf(request), request.getRemoteAddr());
        agentPasswordStatus(response);
    }

    /// `POST /api/developer/agent-auth {password, label, instanceId?}`: the door
    /// a script or an agent itself knocks on.
    ///
    /// It is reachable **without a session** — that is the whole point, a curl
    /// one-liner must be able to use it — so it is deliberately narrow:
    ///
    /// - it never elevates anything; it only mints an agent grant, capped by
    ///   what the operator stored with the password;
    /// - it is refused at guard levels `off` and `proxy`, because there the
    ///   session alone already reaches `/api/*` and a minted token would add
    ///   only another way to lose a credential;
    /// - it does not accept the developer password and the developer route does
    ///   not accept this one: two stores, two doors, so typing the agent
    ///   password into the browser's elevate form cannot work.
    private void agentAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!config.developerGuard.scope.coversInstanceActions()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "agent_password_not_applicable: this panel's guard level ("
                            + config.developerGuard.scope.wireName()
                            + ") already opens /api/* to a session; an agent password would grant nothing new");
            return;
        }
        if (!agentAuth.isSet()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "agent_password_not_set: the operator must set one in the panel first");
            return;
        }
        // The lesser secret is borrowed authority, not an independent one: it
        // only works while somebody has proved the developer password. That is
        // what keeps a leaked agent password from being a way in on its own.
        Instant humanExpiry = elevation.latestHumanExpiry();
        if (humanExpiry == null) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "agent_password_not_elevated: nobody is currently elevated — the operator must "
                            + "enter the developer password in the panel first, and this password "
                            + "stops working when that elevation ends");
            return;
        }
        String address = request.getRemoteAddr();
        // The default posture: a ticket is for a tool beside the panel. A
        // remote caller asking for one would only get a credential the reuse
        // gate refuses later, so say it here.
        if (config.agent.loopbackOnly && !AuthFilter.isLoopback(address)) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "agent_loopback_only: tickets are only issued to the panel's loopback;"
                            + " the operator can allow remote tickets in 开发者模式 → agent 访问");
            return;
        }
        Instant now = Instant.now();
        if (agentPasswordThrottle.isBlocked(address, now)) {
            Json.error(response, 429, "too many failed attempts; try again later");
            return;
        }
        JsonObject body = readJson(request);
        String password = body == null ? null : stringField(body, "password");
        if (password == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "password is required");
            return;
        }
        if (!agentAuth.verify(password)) {
            agentPasswordThrottle.recordFailure(address, now);
            AuditLog.log(AuditLog.Channel.WEB, "agent.auth", null, "failure", null, address);
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid agent password");
            return;
        }
        agentPasswordThrottle.clear(address);
        String label = stringField(body, "label");
        String claimed = stringField(body, "instanceId");
        // The binding is derived, not asked for: the panel asks the kernel
        // which instance this connection's process tree belongs to and binds
        // that. A caller that cannot be placed keeps whatever it claimed —
        // a claim only ever produces extra refusals, never fewer.
        org.jackhuang.hmcl.web.auth.PeerIdentity.Ref identified =
                identity.derive(request.getRemotePort(), request.getLocalPort());
        String instanceId = identified != null ? identified.wire() : claimed;
        // The request may narrow the cap, never widen it.
        Set<String> requested = new LinkedHashSet<>();
        if (body.has("scopes") && body.get("scopes").isJsonArray()) {
            for (var element : body.getAsJsonArray("scopes")) {
                if (element.isJsonPrimitive()) {
                    requested.add(element.getAsString());
                }
            }
        }
        java.util.List<String> cap = agentAuth.scopes();
        Set<String> granted = new LinkedHashSet<>();
        for (String scope : (requested.isEmpty() ? AgentScopes.DEFAULT : requested)) {
            if (!AgentScopes.known(scope)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown_scope: " + scope);
                return;
            }
            if (cap != null && cap.contains(scope)) {
                granted.add(scope);
            }
        }
        if (granted.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "agent_scope_denied: this agent password does not allow "
                            + (requested.isEmpty() ? AgentScopes.DEFAULT : requested));
            return;
        }
        Duration ttl = DeveloperElevation.EXTENDED_TTL;
        if (body.has("ttlSeconds") && !body.get("ttlSeconds").isJsonNull()) {
            Long seconds = longField(body, "ttlSeconds");
            if (seconds == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "ttlSeconds must be a number of seconds");
                return;
            }
            if (seconds > 0) {
                ttl = Duration.ofSeconds(seconds);
            }
        }
        String token = elevation.mintAgentToken(label, granted, instanceId, ttl, humanExpiry,
                DeveloperElevation.MintedBy.PASSWORD);
        var grant = elevation.agentGrant(token);
        // The mint is the one line every later question comes back to: what
        // was granted, where it lives, how long it lasts, and whether the
        // binding was derived or merely claimed.
        java.util.Map<String, Object> minted = new java.util.LinkedHashMap<>();
        minted.put("scopes", java.util.List.copyOf(granted));
        if (instanceId != null) {
            minted.put("caller", instanceId);
        }
        if (identified == null && claimed != null && !claimed.isBlank()) {
            minted.put("claimed", claimed);
        }
        if (grant != null) {
            minted.put("expiresAt", grant.expiresAt().toString());
        }
        AuditLog.log(AuditLog.Channel.WEB, "agent.auth", grant == null ? null : grant.id(),
                "success", "agent:" + (label == null || label.isBlank() ? "agent" : label), address, minted);
        JsonObject answer = new JsonObject();
        answer.addProperty("token", token);
        if (grant != null) {
            answer.addProperty("id", grant.id());
            answer.addProperty("expiresAt", grant.expiresAt().toString());
            if (grant.instanceId() != null) {
                answer.addProperty("instanceId", grant.instanceId());
            }
        }
        JsonArray array = new JsonArray();
        granted.forEach(array::add);
        answer.add("scopes", array);
        Json.write(response, answer);
    }

    /// `POST /api/developer/agent-token`: an elevated session signs a bearer
    /// token the operator can hand to an agent directly — the one-click path,
    /// back because the fatal-operation gate no longer trusts what a token
    /// claims about its holder.
    ///
    /// The token is the same kind [#agentAuth] mints: same registry, same
    /// listing, same revocation, same death when the elevation that signed it
    /// ends. What differs is who asked — the operator, with the developer
    /// password already proved — so the scopes are the operator's own choice
    /// and the agent password's cap does not apply. Agents cannot reach this
    /// route: `AgentScopes#forbidden` closes all of `/api/developer/*` to
    /// them, so no token can sign its own successor.
    ///
    /// An `instanceId` binds the token to one instance. On Linux that is
    /// optional — the gate derives the caller's instance from the connection
    /// on every request and refuses self-directed fatal operations whatever
    /// the token says — but elsewhere the binding is the only guard left, and
    /// an unbound token there simply cannot stop, delete or take over.
    private void mintAgentToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String label = stringField(body, "label");
        String instanceId = stringField(body, "instanceId");
        Set<String> scopes = new LinkedHashSet<>();
        if (body.has("scopes") && body.get("scopes").isJsonArray()) {
            for (var element : body.getAsJsonArray("scopes")) {
                if (element.isJsonPrimitive()) {
                    scopes.add(element.getAsString());
                }
            }
        }
        if (scopes.isEmpty()) {
            scopes.addAll(AgentScopes.DEFAULT);
        }
        for (String scope : scopes) {
            if (!AgentScopes.known(scope)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown_scope: " + scope + " (known: " + AgentScopes.KNOWN + ")");
                return;
            }
        }
        // The agent's life is capped at the operator's own ceiling. A longer
        // claim is clamped, not refused: the settings page renders the result,
        // so the operator sees what they actually got.
        Duration ttl = DeveloperElevation.EXTENDED_TTL;
        if (body.has("ttlSeconds") && !body.get("ttlSeconds").isJsonNull()) {
            Long seconds = longField(body, "ttlSeconds");
            if (seconds == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "ttlSeconds must be a number of seconds");
                return;
            }
            if (seconds > 0) {
                ttl = Duration.ofSeconds(seconds);
            }
        }
        String token = elevation.mintAgentToken(label, scopes, instanceId, ttl,
                elevation.latestHumanExpiry(), DeveloperElevation.MintedBy.SESSION);
        var grant = elevation.agentGrant(token);
        java.util.Map<String, Object> minted = new java.util.LinkedHashMap<>();
        minted.put("scopes", java.util.List.copyOf(scopes));
        if (instanceId != null) {
            minted.put("caller", instanceId);
        }
        if (grant != null) {
            minted.put("expiresAt", grant.expiresAt().toString());
        }
        AuditLog.log(AuditLog.Channel.WEB, "developer.agent", grant == null ? null : grant.id(),
                "mint", actorOf(request), request.getRemoteAddr(), minted);
        JsonObject answer = new JsonObject();
        answer.addProperty("token", token);
        String panel = panelUrl();
        if (panel != null) {
            answer.addProperty("panel", panel);
        }
        if (grant != null) {
            answer.addProperty("id", grant.id());
            answer.addProperty("expiresAt", grant.expiresAt().toString());
            answer.addProperty("label", grant.label());
            if (grant.instanceId() != null) {
                answer.addProperty("instanceId", grant.instanceId());
            }
        }
        JsonArray array = new JsonArray();
        scopes.forEach(array::add);
        answer.add("scopes", array);
        Json.write(response, answer);
    }

    /// The loopback address the agent should call, as the panel itself knows
    /// it — not as the operator's browser reached it.
    ///
    /// The operator may be browsing through a reverse proxy on 443 or a
    /// hostname; the prompt must not guess the port from that. The panel names
    /// its own connector, its own scheme, and its own mount point, so a token
    /// handed across a desk still lands on the right door from inside.
    ///
    /// @return the base URL, or `null` when the port is not answerable yet
    private @Nullable String panelUrl() {
        int port = panelPort.getAsInt();
        if (port <= 0) {
            return null;
        }
        String scheme = tlsActive.getAsBoolean() ? "https" : "http";
        String path = config.basePath == null ? "" : config.basePath;
        return scheme + "://127.0.0.1:" + port + path;
    }

    /// The developer page's 「停止提权」: drops this session's elevation
    /// immediately, ahead of its expiry. Deliberately **not** gated on the
    /// elevation itself — de-escalation must work even when (or exactly
    /// because) the elevation just expired, and clearing an absent key is a
    /// no-op by nature.
    private void revoke(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        String key = keyOf(request);
        elevation.clear(key);
        // 「停止提权」 stops what the elevation authorized, not only the
        // cookie: a grant is borrowed power, and handing the source of the loan
        // back has to end it too. The agents learn through the same 401 they
        // would learn from an expiry.
        int dropped = dropAgentGrants();
        AuditLog.log(AuditLog.Channel.WEB, "developer.elevation", "revoke", "success",
                actorOf(request) + (dropped > 0 ? " (dropped " + dropped + " agent grant(s))" : ""),
                request.getRemoteAddr());
        JsonObject body = new JsonObject();
        body.addProperty("elevated", false);
        Json.write(response, body);
    }

    private void setPassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        String adminPassword = stringField(body, "adminPassword");
        String password = stringField(body, "password");
        String confirm = stringField(body, "confirm");
        String current = stringField(body, "currentDevPassword");
        if (adminPassword == null || password == null || confirm == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "adminPassword, password and confirm are required");
            return;
        }
        UserStore users = auth.userStore();
        if (!confirmPanelPassword(request, response, users, adminPassword)) {
            return;
        }
        boolean changing = devAuth.isSet();
        if (changing && (current == null || !devAuth.verify(current))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "the current developer password does not match");
            return;
        }
        if (!password.equals(confirm)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "password and confirm do not match");
            return;
        }
        try {
            devAuth.set(password, users);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the developer password could not be stored: " + e.getMessage());
            return;
        }
        // A new secret means every elevation minted against the old one dies.
        clearAllElevations();
        JsonObject answer = new JsonObject();
        answer.addProperty("ok", true);
        answer.addProperty("devPasswordSet", true);
        Json.write(response, answer);
    }

    private void clearPassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        String adminPassword = stringField(readJson(request), "adminPassword");
        if (adminPassword == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "adminPassword is required");
            return;
        }
        if (!confirmPanelPassword(request, response, auth.userStore(), adminPassword)) {
            return;
        }
        try {
            devAuth.clear();
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the developer password could not be cleared: " + e.getMessage());
            return;
        }
        clearAllElevations();
        JsonObject answer = new JsonObject();
        answer.addProperty("ok", true);
        answer.addProperty("devPasswordSet", false);
        Json.write(response, answer);
    }

    /// Flips the Web Shell switch. Requires the developer elevation (the same
    /// proof a terminal needs) and HTTPS, because this is the switch that
    /// decides whether an RCE-equivalent entry is reachable at all.
    private void setWebShell(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        Boolean enabled = booleanField(readJson(request), "enabled");
        if (enabled == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "enabled is required");
            return;
        }
        try {
            ServerConfigWriter.updateWebShell(config.dataDir, enabled);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the web shell switch could not be stored: " + e.getMessage());
            return;
        }
        config.webShell.enabled = enabled;
        AuditLog.log(AuditLog.Channel.WEB, "developer.web-shell", enabled ? "on" : "off", "success",
                actorOf(request), request.getRemoteAddr());
        JsonObject answer = new JsonObject();
        answer.addProperty("ok", true);
        answer.addProperty("enabled", enabled);
        Json.write(response, answer);
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session value ? value.username() : null;
    }

    /// Flips the developer guard's scope (docs/web-shell.md §4.1).
    ///
    /// Same proof as the Web Shell switch — TLS and an elevation — because this
    /// is the switch that decides how far an elevation reaches. Turning it on
    /// without a developer password is refused: nobody could clear it again.
    private void setGuard(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        org.jackhuang.hmcl.web.config.DeveloperGuardScope scope =
                org.jackhuang.hmcl.web.config.DeveloperGuardScope.parse(stringField(readJson(request), "scope"));
        if (scope == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "scope must be one of off, proxy, instance, all");
            return;
        }
        if (scope != org.jackhuang.hmcl.web.config.DeveloperGuardScope.OFF && !devAuth.isSet()) {
            // Without a password the gate could never be satisfied, so the
            // instances would be unreachable until someone edits server.yaml.
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "developer_password_not_set: set the developer password before turning the guard on");
            return;
        }
        try {
            ServerConfigWriter.updateDeveloperGuard(config.dataDir, scope);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "the developer guard could not be stored: " + e.getMessage());
            return;
        }
        config.developerGuard.scope = scope;
        AuditLog.log(AuditLog.Channel.WEB, "developer.guard", scope.wireName(), "success",
                actorOf(request), request.getRemoteAddr());
        JsonObject answer = new JsonObject();
        answer.addProperty("ok", true);
        answer.addProperty("scope", scope.wireName());
        Json.write(response, answer);
    }

    /// The emergency stop's state, for `/status` and the banners.
    private JsonObject estopState() {
        JsonObject state = new JsonObject();
        boolean engaged = emergency.isEngaged();
        state.addProperty("engaged", engaged);
        if (!engaged) {
            state.addProperty("remainingSeconds", 0);
            return state;
        }
        Instant engagedAt = emergency.engagedAt();
        Instant releasesAt = emergency.releasesAt();
        if (engagedAt != null) {
            state.addProperty("engagedAt", engagedAt.toString());
        }
        if (releasesAt != null) {
            state.addProperty("releasesAt", releasesAt.toString());
        }
        state.addProperty("remainingSeconds", Math.max(0, emergency.remaining(Instant.now()).toSeconds()));
        String reason = emergency.reason();
        if (reason != null && !reason.isBlank()) {
            state.addProperty("reason", reason);
        }
        return state;
    }

    /// What the E-Stop is about to kill, so the confirmation can list it.
    ///
    /// The list is a snapshot: an instance started between this call and the
    /// engage is still killed, because the gate closes before the killing
    /// starts.
    private void estopPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("engagable", !emergency.isEngaged());
        body.add("estop", estopState());

        JsonArray instances = new JsonArray();
        int processes = 0;
        for (org.jackhuang.hmcl.dsh.DshProcess process : runtime.runningProcesses()) {
            JsonObject row = new JsonObject();
            row.addProperty("id", process.instance().id());
            row.addProperty("brand", "dsh");
            row.addProperty("state", runtime.stateOf(process.instance().id()));
            row.addProperty("pids", process.processTreeSize());
            instances.add(row);
            processes += process.processTreeSize();
        }
        JsonArray terminalRows = new JsonArray();
        int terminals = 0;
        for (org.jackhuang.hmcl.web.terminal.TerminalManager.Tracked terminal : this.terminals.tracked()) {
            JsonObject row = new JsonObject();
            row.addProperty("brand", terminal.brand());
            row.addProperty("instance", terminal.instanceId());
            row.addProperty("running", terminal.running());
            terminalRows.add(row);
            if (terminal.running()) {
                terminals++;
            }
        }
        body.add("instances", instances);
        body.add("terminals", terminalRows);
        body.addProperty("processCount", processes);
        body.addProperty("terminalCount", terminals);
        Json.write(response, body);
    }

    /// The brake: state first, then the work (see [EmergencyStop]).
    private void estopEngage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        JsonObject input = readJson(request);
        if (!"ENGAGE-EMERGENCY-STOP".equals(stringField(input, "confirm"))) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "confirmation_required: send {\"confirm\":\"ENGAGE-EMERGENCY-STOP\"}");
            return;
        }
        String reason = stringField(input, "reason");
        boolean engagedNow = emergency.engage(String.valueOf(actorOf(request)), reason, Instant.now());
        if (!engagedNow) {
            // Already engaged: the hour must not restart, and nothing is killed
            // a second time.
            JsonObject answer = estopState();
            answer.addProperty("alreadyEngaged", true);
            Json.write(response, answer);
            return;
        }
        int terminalsClosed = terminals.terminateAll();
        int processes = runtime.killAllForcibly();
        AuditLog.log(AuditLog.Channel.WEB, "estop.engage",
                "terminals=" + terminalsClosed + " processes=" + processes
                        + (reason == null || reason.isBlank() ? "" : " reason=" + reason),
                "success", actorOf(request), request.getRemoteAddr());
        JsonObject answer = estopState();
        answer.addProperty("terminals", terminalsClosed);
        answer.addProperty("processes", processes);
        Json.write(response, answer);
    }

    /// Lifts the stop, but only after the hour has run out.
    private void estopRelease(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireTls(request, response)) {
            return;
        }
        if (!elevation.require(keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        Instant now = Instant.now();
        Instant endedAt = emergency.release(now);
        if (endedAt == null) {
            JsonObject refusal = new JsonObject();
            refusal.addProperty("error", emergency.isEngaged()
                    ? "estop_cooldown: the emergency stop cannot be released yet"
                    : "estop_not_engaged: the panel is not in emergency stop");
            refusal.addProperty("remainingSeconds", emergency.remaining(now).toSeconds());
            Json.write(response, HttpServletResponse.SC_CONFLICT, refusal);
            return;
        }
        AuditLog.log(AuditLog.Channel.WEB, "estop.release",
                "held for " + java.time.Duration.between(endedAt, now).toMinutes() + " minutes",
                "success", actorOf(request), request.getRemoteAddr());
        JsonObject answer = new JsonObject();
        answer.addProperty("engaged", false);
        Json.write(response, answer);
    }

    /// Confirms the panel password with a per-address budget, answering the
    /// refusal itself.
    private boolean confirmPanelPassword(HttpServletRequest request, HttpServletResponse response,
                                         UserStore users, String adminPassword) throws IOException {
        String address = request.getRemoteAddr();
        String key = address == null || address.isBlank() ? "unknown" : address;
        Instant now = Instant.now();
        if (adminPasswordThrottle.isBlocked(key, now)) {
            refuse(response, adminPasswordThrottle.block());
            return false;
        }
        UserStore.User admin = users.admin();
        if (admin != null && users.verify(admin.username(), adminPassword)) {
            adminPasswordThrottle.clear(key);
            return true;
        }
        Duration refused = adminPasswordThrottle.recordFailure(key, Instant.now());
        if (refused.isZero()) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN, "the panel password does not match");
        } else {
            refuse(response, refused);
        }
        return false;
    }

    private static void refuse(HttpServletResponse response, Duration refusedFor) throws IOException {
        response.setHeader("Retry-After", Long.toString(Math.max(1, refusedFor.toSeconds())));
        Json.error(response, 429, "too many failed panel-password attempts; try again later");
    }

    private boolean requireTls(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            return true;
        }
        Json.error(response, HttpServletResponse.SC_CONFLICT,
                "tls_required: the developer password is only accepted over HTTPS");
        return false;
    }

    private static @Nullable JsonObject readJson(HttpServletRequest request) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Reads a string field, and only a string. `{"password":{}}` used to reach
    /// `getAsString()` and come back as a 500: a body that does not have the
    /// shape the endpoint documents is a client mistake, and the caller answers
    /// it with the same 400 it uses for a missing field.
    private static @Nullable String stringField(@Nullable JsonObject body, String field) {
        JsonElement element = fieldOf(body, field);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString()
                : null;
    }

    /// Same contract for numbers: a JSON object or `"abc"` reads as absent so the
    /// caller can refuse it, instead of throwing out of `getAsLong()`.
    private static @Nullable Long longField(@Nullable JsonObject body, String field) {
        JsonElement element = fieldOf(body, field);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable JsonElement fieldOf(@Nullable JsonObject body, String field) {
        if (body == null || !body.has(field)) {
            return null;
        }
        JsonElement element = body.get(field);
        return element.isJsonNull() ? null : element;
    }

    private static @Nullable Boolean booleanField(@Nullable JsonObject body, String field) {
        if (body == null || !body.has(field) || body.get(field).isJsonNull()) {
            return null;
        }
        try {
            return body.get(field).getAsBoolean();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }
}
