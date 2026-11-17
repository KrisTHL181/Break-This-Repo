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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.DeveloperGuardScope;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.emergency.EmergencyStop;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/// The two gates in front of everything an instance exposes.
///
/// **Emergency stop** comes first: while it is engaged, `/i/<id>/*` and the
/// state-changing instance APIs answer `503 emergency_stop`, whatever the
/// caller's elevation. Reads (the instance list, logs, the developer page)
/// stay open — otherwise nobody could see what happened or release the stop.
///
/// **Developer guard** ([ServerConfig#developerGuard]) is the second: when the
/// configured scope covers the request, it needs the developer elevation.
/// Both the browser's session elevation and a bearer token minted by
/// `POST /api/developer/elevate` count; the token exists so a host script or an
/// inspection agent does not have to hold a cookie jar.
///
/// Neither gate touches work that is already running. An open instance page
/// keeps running (its own requests start failing), a terminal stays attached and
/// an agent process is untouched: **the guard is a door, not a circuit
/// breaker**. Only the emergency stop kills processes.
///
/// Mounted after [AuthFilter] on `/api/*` and `/i/*`, so an unauthenticated
/// request is answered 401 by the session gate rather than 403 here.
@NotNullByDefault
public final class DeveloperGuardFilter implements Filter {

    private final ServerConfig config;
    private final DeveloperAuth devAuth;
    private final DeveloperElevation elevation;
    private final AuthService auth;
    private final EmergencyStop emergency;
    private final BooleanSupplier tlsActive;

    public DeveloperGuardFilter(ServerConfig config, DeveloperAuth devAuth, DeveloperElevation elevation,
                                AuthService auth, EmergencyStop emergency, BooleanSupplier tlsActive) {
        this.config = config;
        this.devAuth = devAuth;
        this.elevation = elevation;
        this.auth = auth;
        this.emergency = emergency;
        this.tlsActive = tlsActive;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest http) || !(response instanceof HttpServletResponse answer)) {
            chain.doFilter(request, response);
            return;
        }
        // The decoded path the dispatcher routes, not the raw URI: `%69nstances`
        // must not walk past the emergency stop or the guard because it is not
        // spelled `/instances` in the request line.
        String path = decodedPath(http);
        String method = http.getMethod();
        boolean navigation = isNavigation(http);

        if (emergency.isEngaged() && blockedByEmergency(path, method)) {
            refuse(answer, navigation, path, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "emergency_stop",
                    "紧急停机中：实例网页、终端与实例启动都被冻结。冷却结束或重启面板后才能恢复。",
                    "The panel is in emergency stop: instance pages, terminals and instance starts are frozen.",
                    false);
            return;
        }

        // This guard polices **people**: it asks for the developer password. An
        // agent never has one — its authority is the grant's scope, and
        // [AuthFilter] enforced that before this filter ran. Letting it through
        // is what keeps the `mc` and `instances` scopes from dying on a password
        // wall the agent cannot climb. The closed routes never reach here:
        // AuthFilter answers them 403 first. The emergency stop above still
        // applies to agents, because it freezes work, not a door.
        if (org.jackhuang.hmcl.web.auth.AuthFilter.isAgent(http)) {
            chain.doFilter(request, response);
            return;
        }

        DeveloperGuardScope scope = config.developerGuard.scope;
        if (scope == DeveloperGuardScope.OFF || !gated(scope, path, method)) {
            chain.doFilter(request, response);
            return;
        }
        // A developer who has not even set a password cannot be asked for one;
        // say so instead of looping on "wrong password".
        if (!devAuth.isSet()) {
            refuse(answer, navigation, path, HttpServletResponse.SC_CONFLICT, "developer_password_not_set",
                    "开发者守卫已开启，但还没有设置开发者密码。请先在「设置 → 开发者模式」里设置。",
                    "The developer guard is on but no developer password is set.", false);
            return;
        }
        // Elevation is only ever obtained over HTTPS (see DeveloperApiServlet),
        // so on a plain-HTTP panel a gated request can never be satisfied. Say
        // which policy refused it rather than asking for a password that cannot
        // work.
        if (!RequestSecurity.isSecure(http, tlsActive.getAsBoolean(), config)) {
            refuse(answer, navigation, path, HttpServletResponse.SC_FORBIDDEN, "tls_required",
                    "面板不在 HTTPS 下：开发者模式不可用。请先在「设置 → HTTPS 证书」里启用。",
                    "The panel is not on HTTPS, so developer mode is unavailable.", false);
            return;
        }
        if (elevation.require(key(http), sessionAlive(http)) || elevation.requireToken(bearerToken(http))) {
            chain.doFilter(request, response);
            return;
        }
        refuse(answer, navigation, path, HttpServletResponse.SC_FORBIDDEN, "developer_elevation_required",
                "需要开发者密码",
                "Enter the developer password to open this instance.",
                true);
    }

    /// Whether the guard's scope covers this request.
    ///
    /// Pure and static where it can be: the tests drive it directly instead of
    /// standing up a server.
    static boolean gated(DeveloperGuardScope scope, String path, String method) {
        if (scope == DeveloperGuardScope.OFF) {
            return false;
        }
        if (DeveloperGuardScope.alwaysAllowed(path)) {
            return false;
        }
        if (path.startsWith("/i/")) {
            return scope.coversProxy();
        }
        if (!path.startsWith("/api/")) {
            return false;
        }
        if (scope.coversAllApi()) {
            return true;
        }
        return scope.coversInstanceActions() && DeveloperGuardScope.instanceMutation(path, method);
    }

    /// Whether the emergency stop refuses this request.
    ///
    /// Instance pages and the mutation verbs on the instance family: the same
    /// set that can start a process or write a file. Reads stay open, and the
    /// developer routes stay open — the status has to be readable, and the
    /// release button has to reach the server.
    static boolean blockedByEmergency(String path, String method) {
        if (path.startsWith("/i/")) {
            return true;
        }
        if (!path.startsWith("/api/")) {
            return false;
        }
        if (DeveloperGuardScope.alwaysAllowed(path)) {
            return false;
        }
        return DeveloperGuardScope.instanceMutation(path, method);
    }

    /// Answers the refusal: a small HTML page for a browser navigation (a JSON
    /// body would read as a broken page), JSON for everything else.
    ///
    /// The page carries the **password form itself** when the refusal is
    /// [elevation][#elevationRequired]: being told "go to the panel settings and
    /// come back" is a dead end when the thing you clicked was a link to the
    /// instance. The form posts to the panel's own `elevate` endpoint and
    /// reloads on success; a transport refusal (`tls_required`) deliberately
    /// has no form — the password would only be refused again, in cleartext.
    private void refuse(HttpServletResponse response, boolean navigation, String path,
                        int status, String code, String zh, String en, boolean elevationRequired)
            throws IOException {
        if (navigation && path.startsWith("/i/")) {
            response.setStatus(status);
            response.setContentType("text/html; charset=utf-8");
            response.setHeader("Cache-Control", "no-store");
            String panel = (config.basePath.isEmpty() ? "" : config.basePath) + "/";
            byte[] body = gatePage(panel, zh, en, elevationRequired).getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = response.getOutputStream()) {
                out.write(body);
            }
            return;
        }
        Json.error(response, status, code + ": " + en);
    }

    /// The gate page itself.
    ///
    /// Split out from [#refuse] so the two shapes can be asserted without a
    /// booted server: with `elevationRequired` the page carries the password
    /// form (that is the case a user hits by clicking through to an instance),
    /// without it the page explains the transport policy and has no form — a
    /// password typed there would only be refused again, in cleartext.
    static String gatePage(String panel, String zh, String en, boolean elevationRequired) {
        StringBuilder page = new StringBuilder(1024);
        page.append("<!doctype html><html lang=\"zh\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>HDSL-web</title></head><body style=\"font:14px/1.6 system-ui, sans-serif;")
                .append("max-width:40rem;margin:12vh auto;padding:0 1.2rem;color:#1b1b1f\">")
                .append("<h1 style=\"font-size:1.25rem\">").append(escape(zh)).append("</h1>")
                .append("<p style=\"color:#555\">").append(escape(en)).append("</p>");
        if (elevationRequired) {
            page.append("<form id=\"hdsl-elevate\" style=\"display:flex;gap:.5rem;align-items:center;")
                    .append("flex-wrap:wrap;margin:1.2rem 0\">")
                    .append("<input id=\"hdsl-dev-password\" type=\"password\" autocomplete=\"off\" autofocus")
                    .append(" placeholder=\"开发者密码\"")
                    .append(" style=\"flex:1 1 14rem;padding:.5rem .6rem;border:1px solid #bbb;")
                    .append("border-radius:4px;font:inherit\">")
                    .append("<button type=\"submit\" style=\"padding:.5rem 1rem;border:0;border-radius:4px;")
                    .append("background:#4e6fff;color:#fff;font:inherit;cursor:pointer\">提权并继续</button>")
                    .append("</form>")
                    .append("<p id=\"hdsl-elevate-error\" style=\"color:#b3261e;min-height:1.4em\"></p>")
                    .append("<script>(function(){")
                    .append("var form=document.getElementById('hdsl-elevate');")
                    .append("var field=document.getElementById('hdsl-dev-password');")
                    .append("var err=document.getElementById('hdsl-elevate-error');")
                    .append("form.addEventListener('submit',function(event){")
                    .append("event.preventDefault();err.textContent='';")
                    .append("fetch(").append(jsString(panel + "api/developer/elevate")).append(",{method:'POST',")
                    .append("credentials:'same-origin',headers:{'Content-Type':'application/json'},")
                    .append("body:JSON.stringify({password:field.value})})")
                    .append(".then(function(response){return response.text().then(function(text){")
                    .append("if(response.ok){location.reload();return;}")
                    .append("var message='';try{message=JSON.parse(text).error||'';}catch(ignored){}")
                    .append("if(message.indexOf('tls_required')>=0){")
                    .append("err.textContent='面板不在 HTTPS 下，开发者模式不可用。';}")
                    .append("else if(message.indexOf('developer_password_not_set')>=0){")
                    .append("err.textContent='还没有设置开发者密码，请先在面板「设置 → 开发者模式」里设置。';}")
                    .append("else if(message.indexOf('too many failed attempts')>=0){")
                    .append("err.textContent='失败次数过多，已临时锁定，请稍后再试。';}")
                    .append("else{err.textContent='开发者密码不正确。';}")
                    .append("});})")
                    .append(".catch(function(){err.textContent='请求失败，请检查网络后重试。';});")
                    .append("});})();</script>");
        }
        page.append("<p><a href=\"").append(escape(panel)).append("\">返回面板</a></p>");
        page.append("</body></html>");
        return page.toString();
    }

    /// A JS string literal, for the one interpolated URL above.
    static String jsString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "") + "'";
    }

    /// Whether this looks like a browser navigation, which gets the HTML page.
    private static boolean isNavigation(HttpServletRequest request) {
        String mode = request.getHeader("Sec-Fetch-Mode");
        if (mode != null) {
            return mode.equalsIgnoreCase("navigate");
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    /// `Authorization: Bearer <token>`, or an empty string.
    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.length() < 8) {
            return "";
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        return header.substring(7).trim();
    }

    /// The decoded path the dispatcher will route, falling back to the raw URI
    /// when no servlet has matched yet.
    private static String decodedPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            String pathInfo = request.getPathInfo();
            return pathInfo == null ? servletPath : servletPath + pathInfo;
        }
        String path = request.getRequestURI();
        return path == null ? "" : path;
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static String key(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }

    /// Minimal HTML escaping for the two strings above (both are ours, but the
    /// page builder must not depend on that).
    private static String escape(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
