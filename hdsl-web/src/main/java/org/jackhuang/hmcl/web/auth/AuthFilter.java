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
package org.jackhuang.hmcl.web.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.Json;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

/// The session gate in front of `/api/*` and `/i/*`.
///
/// The public data endpoints are `POST /api/auth/login` (that is how a
/// session comes to exist), `GET /api/auth/status` and `POST /api/auth/setup`
/// (the first-run setup pair: the status tells the UI whether a user exists,
/// the setup creates the first user and is itself guarded against running
/// twice) and `GET /api/health` (liveness probes must work without
/// credentials). Everything else needs a valid `hdsl_session` cookie;
/// otherwise the answer is 401 `{"error":"unauthorized"}`. A verified request
/// carries the session as the `session` request attribute for downstream
/// handlers.
///
/// Static UI assets are not behind this filter on purpose: the login page
/// must be reachable before a session exists, and the shell carries no
/// sensitive information — this is the engineering reading of the plan's
/// "everything needs a session": all data planes do, the UI shell does not.
@NotNullByDefault
public final class AuthFilter implements Filter {

    public static final String SESSION_COOKIE = "hdsl_session";
    public static final String SESSION_ATTRIBUTE = "session";
    /// The raw token behind [SESSION_ATTRIBUTE]. The developer-mode elevation
    /// is keyed by it, so that logging one session out logs its elevation out
    /// too.
    public static final String SESSION_TOKEN_ATTRIBUTE = "session.token";
    /// The [org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant]
    /// behind an agent bearer token, once this filter has admitted one.
    public static final String AGENT_GRANT_ATTRIBUTE = "session.agent";
    public static final String LOGIN_PATH = "/api/auth/login";
    public static final String STATUS_PATH = "/api/auth/status";
    public static final String SETUP_PATH = "/api/auth/setup";
    public static final String HEALTH_PATH = "/api/health";
    /// The agent password's door. Reachable without a session on purpose: a
    /// curl one-liner must be able to use the lesser secret, and it grants
    /// strictly less than this cookie ever would.
    public static final String AGENT_AUTH_PATH = "/api/developer/agent-auth";

    private final AuthService authService;
    private final boolean authDisabled;
    private final java.util.function.Predicate<HttpServletRequest> secureRequest;
    private final int cookieMaxAgeSeconds;
    /// The grants behind agent bearer tokens: an agent has no browser, so the
    /// cookie is not a door it can use.
    private final org.jackhuang.hmcl.web.developer.DeveloperElevation elevation;
    /// Which instance a caller runs in, derived from the kernel. The
    /// fatal-operation gate reads it: a caller that names no instance
    /// identifies nobody, and nobody may not stop one.
    private final PeerIdentity identity;
    /// 非回环来源使用票时是否必须有浏览器会话；浏览器签发的票不受限
    /// （见 [org.jackhuang.hmcl.web.config.ServerConfig.AgentConfig]）。
    private final boolean agentRemoteNeedsSession;
    /// 票是否只认本机回环（默认是）。比上一条更严：非回环一律拒，不看会话。
    private final boolean agentLoopbackOnly;
    /// 会话 Cookie 的 Path：根挂载是 `/`，子路径挂载是挂载点本身。
    /// 同一个域名下挂两个面板（`/a/`、`/b/`）时，Path=/ 的同名 Cookie 会互相覆盖，
    /// 表现为「登录 A 之后 B 掉线」。
    private final String cookiePath;

    /// @param authDisabled  when true, the filter passes everything through
    ///                      without a session (only legal on loopback binds)
    /// @param secureRequest whether this request's browser leg is HTTPS; read
    ///                      per cookie, because the certificate upload can
    ///                      switch the main connector to TLS at runtime and a
    ///                      login after the switch must mint a Secure cookie.
    ///                      A TLS-terminating proxy counts when the server is
    ///                      told to trust `X-Forwarded-Proto`.
    /// @param ttlSeconds    session TTL, also the cookie Max-Age
    /// @param basePath      the mount point the panel is served under ("" for
    ///                      the root). The cookie is scoped to it, so two panels
    ///                      mounted on one origin (`/a/` and `/b/`) do not
    ///                      overwrite each other's session.
    /// @param elevation     the agent grants behind bearer tokens
    /// @param identity      the caller's instance, derived per connection
    /// @param agentRemoteNeedsSession whether a grant used off the loopback
    ///                      must be backed by a browser session (unless it was
    ///                      minted from a browser)
    public AuthFilter(AuthService authService, boolean authDisabled,
                      java.util.function.Predicate<HttpServletRequest> secureRequest, int ttlSeconds,
                      String basePath,
                      org.jackhuang.hmcl.web.developer.DeveloperElevation elevation,
                      PeerIdentity identity,
                      boolean agentRemoteNeedsSession,
                      boolean agentLoopbackOnly) {
        this.authService = authService;
        this.authDisabled = authDisabled;
        this.secureRequest = secureRequest;
        this.cookieMaxAgeSeconds = ttlSeconds;
        this.cookiePath = basePath == null || basePath.isBlank() ? "/" : basePath;
        this.elevation = elevation;
        this.identity = identity;
        this.agentRemoteNeedsSession = agentRemoteNeedsSession;
        this.agentLoopbackOnly = agentLoopbackOnly;
    }

    /// The request path as this panel's routes are written.
    ///
    /// **Decoded, because the servlet routes on the decoded path.** Security
    /// decisions made on `getRequestURI()` see `%61dmin-password`, while the
    /// servlet sees `admin-password` and serves it — a scope check on the raw
    /// URI is a check on a different string than the one being dispatched. The
    /// container's `servletPath + pathInfo` is the decoded pair the dispatcher
    /// itself used, so the gate and the route cannot disagree.
    ///
    /// A mount point is not part of it: `servletPath` excludes the context
    /// path, and a proxy that keeps its prefix has to strip it before the
    /// servlet mapping matches at all.
    private String pathOf(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            String pathInfo = request.getPathInfo();
            return pathInfo == null ? servletPath : servletPath + pathInfo;
        }
        String path = request.getRequestURI();
        return path == null ? "" : path;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (authDisabled || isPublic(req)) {
            chain.doFilter(request, response);
            return;
        }

        // A bearer grant is decided first: a request carrying both a cookie and
        // a ticket keeps the ticket's scope and its `agent:<label>` identity.
        // The cookie is then only a second factor (see
        // `agentRemoteNeedsSession`), never a way to widen the grant.
        Boolean agent = admitAgent(req, resp);
        if (agent != null) {
            if (agent) {
                try {
                    chain.doFilter(request, response);
                } finally {
                    // The context must not leak onto whatever this thread
                    // serves next, and an exception must not keep it either.
                    org.jackhuang.hmcl.web.audit.AuditLog.endAgentFields();
                }
            }
            return;
        }

        String token = cookieValue(req, SESSION_COOKIE);
        Optional<AuthService.Session> session = token == null ? Optional.empty() : authService.verify(token);
        if (session.isPresent()) {
            req.setAttribute(SESSION_ATTRIBUTE, session.get());
            req.setAttribute(SESSION_TOKEN_ATTRIBUTE, token);
            org.jackhuang.hmcl.web.system.SystemActivity.touch();
            chain.doFilter(request, response);
            return;
        }
        Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
    }

    /// Admits a request carrying an agent bearer token, or answers the refusal.
    ///
    /// This is the panel's own gate for callers that are not a browser: the
    /// grant behind the token says **what** may be reached
    /// ([org.jackhuang.hmcl.web.developer.AgentScopes]), and the refusals
    /// below say **what never may be**, whatever the grant claims.
    ///
    /// The fatal-operation gate is identity-driven, not self-reported: the
    /// instance the caller runs in is derived from the connection's process
    /// tree ([PeerIdentity]) and compared against the route's target. A
    /// caller the panel cannot place — outside every tree, or outside this
    /// network namespace — falls back to the grant's binding, and a grant
    /// that binds nothing may not stop, delete or take over anything: the
    /// operation may be aimed at the caller's own instance, and that call
    /// would never return. A caller that proves the developer password
    /// (a session) is a person's hand and never passes through here.
    ///
    /// The session attribute is filled with a synthetic `agent:<label>`
    /// session so every existing handler and the audit trail name the agent
    /// rather than the operator whose password minted the token. That is the
    /// whole point of the label: `audit.jsonl` must be able to answer "who did
    /// this" without a second table.
    ///
    /// @param req  the request
    /// @param resp the response to write a refusal to
    /// @return `null` when no agent token is present, `true` when the request
    ///         may continue, `false` when a refusal has been written
    private @Nullable Boolean admitAgent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String bearer = header.substring(7).trim();
        org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant = elevation.agentGrant(bearer);
        if (grant == null) {
            // Unknown, expired and revoked tokens all read as "not an agent":
            // the caller gets the same 401 as a missing cookie, so a probe
            // cannot tell a wrong token from a dead one.
            return null;
        }
        String method = req.getMethod();
        String path = pathOf(req);
        if (!remoteGrantAllowed(req, grant)) {
            boolean loopbackRule = agentLoopbackOnly && !isLoopback(req.getRemoteAddr());
            auditDenied(req, path, grant, loopbackRule ? "loopback_only" : "remote_session_required", null);
            Json.error(resp, HttpServletResponse.SC_FORBIDDEN, loopbackRule
                    ? "agent_loopback_only: this ticket is only usable from the panel's loopback;"
                            + " the operator can allow remote tickets in 开发者模式 → agent 访问"
                    : "agent_remote_session_required: this ticket was minted with the agent password and is"
                            + " being used from outside the loopback; sign in to the panel first, or mint"
                            + " the ticket from the panel's agent page");
            return false;
        }
        if (org.jackhuang.hmcl.web.developer.AgentScopes.forbidden(method, path)) {
            auditDenied(req, path, grant, "route_closed", null);
            Json.error(resp, HttpServletResponse.SC_FORBIDDEN, "agent_scope_denied: this route is closed to agents");
            return false;
        }
        String needed = org.jackhuang.hmcl.web.developer.AgentScopes.required(method, path);
        if (needed == null || !grant.scopes().contains(needed)) {
            auditDenied(req, path, grant, "scope_denied",
                    needed == null ? "this route has no agent scope" : needed);
            Json.error(resp, HttpServletResponse.SC_FORBIDDEN,
                    "agent_scope_denied: " + (needed == null ? "this route has no agent scope" : needed));
            return false;
        }
        org.jackhuang.hmcl.web.developer.AgentScopes.Fatal target =
                org.jackhuang.hmcl.web.developer.AgentScopes.fatalTarget(method, path);
        if (target != null) {
            String refusal = fatalRefusal(req, path, grant, target, method);
            if (refusal != null) {
                Json.error(resp, HttpServletResponse.SC_CONFLICT, refusal);
                return false;
            }
        }
        req.setAttribute(SESSION_ATTRIBUTE, new AuthService.Session("agent:" + grant.label(), grant.expiresAt()));
        req.setAttribute(AGENT_GRANT_ATTRIBUTE, grant);
        org.jackhuang.hmcl.web.system.SystemActivity.touch();
        org.jackhuang.hmcl.web.audit.AuditLog.beginAgentFields(
                new org.jackhuang.hmcl.web.audit.AuditLog.AgentFields(grant.id(), grant.scopes(), grant.instanceId()));
        return true;
    }

    /// The refusal for a fatal operation, or `null` when it may proceed.
    ///
    /// Priority is the kernel's: a derived identity outranks whatever the
    /// grant claims, in both directions — a caller proven to live elsewhere
    /// may stop the target even when the grant binds it, and a caller proven
    /// to live inside it is refused whatever the grant claims.
    ///
    /// @param req    the request
    /// @param path   the decoded path, for the message
    /// @param grant  the grant behind the token
    /// @param target the instance the route would end or take over
    /// @param method the request method, for the message
    /// @return the refusal to answer, or `null`
    private @Nullable String fatalRefusal(HttpServletRequest req, String path,
                                          org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant,
                                          org.jackhuang.hmcl.web.developer.AgentScopes.Fatal target,
                                          String method) {
        PeerIdentity.Ref self = identity.derive(req.getRemotePort(), req.getLocalPort());
        String targetName = target.family() + "/" + target.id();
        if (self != null) {
            if (target.matches(self.family(), self.instanceId())) {
                auditDenied(req, path, grant, "self_refused", null, self.wire());
                return "self_operation_refused: " + method + " " + path + " would end instance " + targetName
                        + " — the very instance this request is running in (the panel identified the calling"
                        + " process tree). Killing the process that is waiting for this answer means the tool"
                        + " call never returns, so the panel refuses. Ask the operator to do this from the panel,"
                        + " or run it from outside the instance.";
            }
            return null;
        }
        if (target.matchesBound(grant.instanceId())) {
            auditDenied(req, path, grant, "self_refused", null, grant.instanceId());
            return "self_operation_refused: " + method + " " + path + " would end instance " + targetName
                    + " — the instance this grant is bound to. Killing the process that is waiting for this"
                    + " answer means the tool call never returns, so the panel refuses. Ask the operator to do"
                    + " this from the panel, or run it from outside the instance.";
        }
        if (target.launch()) {
            if (identity.terminalOpen(target.family(), target.id())) {
                auditDenied(req, path, grant, "self_refused_launch", null, null);
                return "self_operation_refused: " + method + " " + path + " would close the running terminal of "
                        + targetName + ", and the panel cannot identify this caller — it may be the one inside"
                        + " that terminal, and that tool call would never return. Mint the token from inside your"
                        + " instance so the panel can derive your identity, or let the operator launch it.";
            }
            // A stopped instance has nobody inside it to kill; the launch may go.
            return null;
        }
        if (grant.instanceId() == null || grant.instanceId().isBlank()) {
            auditDenied(req, path, grant, "self_refused_unidentified", null, null);
            return "self_operation_refused: the panel cannot identify which instance this caller runs in and the"
                    + " grant binds none, so " + method + " " + path + " is refused — it may be aimed at the"
                    + " caller's own instance, and that tool call would never return. Mint the token from inside"
                    + " your instance (the panel then derives and binds the identity), or pass instanceId; the"
                    + " operator can always do this from the panel.";
        }
        return null;
    }

    /// Records a refusal this gate wrote, so the trail shows the attempt —
    /// the panel's most safety-relevant answers are not its quietest ones.
    private static void auditDenied(HttpServletRequest req, String path,
                                    org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant,
                                    String outcome, @Nullable String scope) {
        auditDenied(req, path, grant, outcome, scope, null);
    }

    private static void auditDenied(HttpServletRequest req, String path,
                                    org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant,
                                    String outcome, @Nullable String scope, @Nullable String identity) {
        java.util.Map<String, Object> extra = new java.util.LinkedHashMap<>();
        extra.put("route", path);
        extra.put("scopes", java.util.List.copyOf(grant.scopes()));
        if (grant.instanceId() != null) {
            extra.put("caller", grant.instanceId());
        }
        if (scope != null) {
            extra.put("scope", scope);
        }
        if (identity != null) {
            extra.put("identity", identity);
        }
        org.jackhuang.hmcl.web.audit.AuditLog.log(org.jackhuang.hmcl.web.audit.AuditLog.Channel.WEB,
                "agent.denied", path, outcome, "agent:" + grant.label(), req.getRemoteAddr(), extra);
    }

    /// Whether an admitted agent grant names `family/id` — for marking an
    /// executed operation that hit the grant's own instance, in case a route
    /// family ever slips past [org.jackhuang.hmcl.web.developer.AgentScopes#fatalTarget].
    ///
    /// @param request the request, admitted by an agent grant or not
    /// @param family  the instance's family
    /// @param id      the instance's id
    /// @return whether the grant is bound to that instance
    public static boolean selfDirected(HttpServletRequest request, String family, String id) {
        Object attribute = request.getAttribute(AGENT_GRANT_ATTRIBUTE);
        if (!(attribute instanceof org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant)) {
            return false;
        }
        return new org.jackhuang.hmcl.web.developer.AgentScopes.Fatal(family, id, false)
                .matchesBound(grant.instanceId());
    }

    /// Whether this request was admitted by an agent grant rather than a
    /// browser session.
    ///
    /// A route's own `requireElevation` exists to police **people**: it asks for
    /// the developer password. An agent never has one, and never should — its
    /// authority is the grant's scope, which this filter has already enforced
    /// before the servlet runs. So the per-action check reads this and steps
    /// aside, rather than handing the agent a "elevate first" it can never
    /// satisfy.
    ///
    /// The *closed* routes are unaffected: `AgentScopes.forbidden` refuses them
    /// here, long before any servlet sees the request.
    ///
    /// @param request the request
    /// @return whether the request carries an admitted agent grant
    public static boolean isAgent(HttpServletRequest request) {
        return request.getAttribute(AGENT_GRANT_ATTRIBUTE) != null;
    }

    /// The grant behind an admitted agent request, or `null` for a browser
    /// session.
    ///
    /// Servlet code reads this when it must carry the grant's identity into a
    /// credential of its own — the MC console ticket names the grant so the
    /// WebSocket handshake can refuse a ticket presented by anybody else.
    ///
    /// @param request the request
    /// @return the grant, or `null`
    public static @Nullable DeveloperElevation.AgentGrant agentGrant(HttpServletRequest request) {
        Object attribute = request.getAttribute(AGENT_GRANT_ATTRIBUTE);
        return attribute instanceof DeveloperElevation.AgentGrant grant ? grant : null;
    }

    /// Whether this grant may be used from this connection.
    ///
    /// Two rules, strictest first: [#grantUsable]'s loopback rule (on by
    /// default) refuses every remote ticket, and only when the operator turned
    /// that off does [remoteAllowed]'s "remote needs a person" apply.
    private boolean remoteGrantAllowed(HttpServletRequest request,
                                       org.jackhuang.hmcl.web.developer.DeveloperElevation.AgentGrant grant) {
        return grantUsable(agentLoopbackOnly, agentRemoteNeedsSession,
                isLoopback(request.getRemoteAddr()),
                grant.mintedBy() == org.jackhuang.hmcl.web.developer.DeveloperElevation.MintedBy.SESSION,
                hasLiveSession(request));
    }

    /// The source rule as a pure function, so the matrix can be exercised
    /// without a socket.
    ///
    /// @param loopbackOnly      whether tickets are loopback-only (the default)
    /// @param remoteNeedsSession whether a remote ticket needs a session
    ///                           (only consulted when `loopbackOnly` is off)
    /// @param loopback          whether the caller is the local machine
    /// @param mintedFromBrowser whether a person minted the ticket in the panel
    /// @param sessionAlive      whether the request carries a live session
    /// @return whether the grant may be used
    static boolean grantUsable(boolean loopbackOnly, boolean remoteNeedsSession, boolean loopback,
                               boolean mintedFromBrowser, boolean sessionAlive) {
        if (loopback) {
            return true;
        }
        if (loopbackOnly) {
            return false;
        }
        return remoteAllowed(remoteNeedsSession, false, mintedFromBrowser, sessionAlive);
    }

    /// The rule behind {@link #remoteGrantAllowed}, as a pure function so the
    /// three inputs can be exercised without a socket.
    ///
    /// @param enabled            whether the switch is on
    /// @param loopback           whether the caller is the local machine
    /// @param mintedFromBrowser  whether a person minted the ticket in the panel
    /// @param sessionAlive       whether the request carries a live session
    /// @return whether the grant may be used
    static boolean remoteAllowed(boolean enabled, boolean loopback,
                                 boolean mintedFromBrowser, boolean sessionAlive) {
        if (!enabled || loopback) {
            return true;
        }
        return mintedFromBrowser || sessionAlive;
    }

    private boolean hasLiveSession(HttpServletRequest request) {
        String token = cookieValue(request, SESSION_COOKIE);
        return token != null && !token.isBlank() && authService.verify(token).isPresent();
    }

    /// The answer a servlet's own `requireElevation` should use.
    ///
    /// Two callers, two proofs: a person proves the developer password (the
    /// elevation behind `key`), an agent proves its grant's scope — which this
    /// filter already checked before the servlet ran. Asking the agent for a
    /// password it cannot have turns a working scope into a dead end; that is
    /// exactly what the `mc` grant hit on `console/input`.
    ///
    /// Every servlet-side elevation check goes through here so a new one cannot
    /// forget the agent branch. The routes in
    /// [org.jackhuang.hmcl.web.developer.AgentScopes#forbidden] never get this
    /// far: the filter answers them 403 first.
    ///
    /// @param request      the request
    /// @param elevation    the elevation store
    /// @param key          the caller's elevation key
    /// @param sessionAlive whether the session behind `key` is still valid
    /// @return whether the action may proceed
    public static boolean elevationSatisfied(HttpServletRequest request,
                                             org.jackhuang.hmcl.web.developer.DeveloperElevation elevation,
                                             String key, boolean sessionAlive) {
        return isAgent(request) || elevation.require(key, sessionAlive);
    }

    /// Builds the session cookie. Shared with the login endpoint so the
    /// attributes can never drift apart between the two places that set it.
    ///
    /// @param request the request the cookie will answer, for `Secure`
    public Cookie sessionCookie(HttpServletRequest request, String token) {
        Cookie cookie = new Cookie(SESSION_COOKIE, token);
        cookie.setPath(cookiePath);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(cookieMaxAgeSeconds);
        cookie.setSecure(secureRequest.test(request));
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    /// A cookie with the same name/path and zero Max-Age, which is what makes
    /// the browser drop it.
    ///
    /// @param request the request the cookie will answer, for `Secure`
    public Cookie clearedSessionCookie(HttpServletRequest request) {
        Cookie cookie = new Cookie(SESSION_COOKIE, "");
        cookie.setPath(cookiePath);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(secureRequest.test(request));
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    /// Whether the peer is this machine's loopback.
    ///
    /// `127.0.0.0/8`, `::1`, and IPv4-mapped forms all count. Anything else —
    /// a LAN address, a docker bridge, a public address — is "remote" for
    /// every agent-ticket rule.
    ///
    /// Public so the developer routes can refuse a remote agent password with
    /// the same definition the request gate uses.
    public static boolean isLoopback(@Nullable String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            return java.net.InetAddress.getByName(address).isLoopbackAddress();
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        String method = request.getMethod();
        String path = pathOf(request);
        return ("POST".equals(method) && LOGIN_PATH.equals(path))
                || ("GET".equals(method) && STATUS_PATH.equals(path))
                || ("POST".equals(method) && SETUP_PATH.equals(path))
                || ("GET".equals(method) && HEALTH_PATH.equals(path))
                || ("POST".equals(method) && AGENT_AUTH_PATH.equals(path))
                // The terminal socket authenticates itself at the handshake:
                // a one-use ticket **plus** either the session cookie it was
                // issued to or the agent grant it names (see
                // TerminalWebSocket#onOpen). This filter cannot judge a
                // ticket — it would have to consume it — so the endpoint is
                // the gate, and skipping the cookie check here is what lets an
                // agent attach a console it was granted.
                || ("GET".equals(method) && path.startsWith("/ws/terminal/"));
    }

    private static @Nullable String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
