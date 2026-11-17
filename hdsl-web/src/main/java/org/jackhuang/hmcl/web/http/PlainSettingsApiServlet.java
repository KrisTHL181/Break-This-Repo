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

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// `/api/settings/plain` — the legacy surface's enhancement switch.
///
/// - `GET` answers `{"enhanced":…}`: whether `/plain` is a full citizen
///   (fragment long-polling, the ES5 poller, server-rendered markdown) or only
///   the degraded fallback of forms and whole-page refreshes.
/// - `PUT` takes `{"enhanced":…}` and persists it to `server.yaml`
///   (`plain.enhanced`), exactly like the neighbouring settings servlets.
///
/// The switch takes effect on the next `/plain` request; nothing here needs a
/// restart. Writing it is a developer-elevation operation, the same rule as
/// `/api/settings/logs`: flipping the surface an operator's old browsers rely
/// on is an operator decision, and the password proof is what makes it one.
@NotNullByDefault
public final class PlainSettingsApiServlet extends HttpServlet {

    /// A settings request needs no more; anything larger is not one.
    private static final int MAX_BODY_BYTES = 4096;

    private final ServerConfig config;
    private final DeveloperElevation elevation;
    private final AuthService auth;
    private final BooleanSupplier tlsActive;
    private final org.jackhuang.hmcl.web.plain.MarkdownSsr ssr;

    public PlainSettingsApiServlet(ServerConfig config, DeveloperElevation elevation,
                                   AuthService auth, BooleanSupplier tlsActive,
                                   org.jackhuang.hmcl.web.plain.MarkdownSsr ssr) {
        this.config = config;
        this.elevation = elevation;
        this.auth = auth;
        this.tlsActive = tlsActive;
        this.ssr = ssr;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        String method = request.getMethod();
        if ("GET".equals(method)) {
            Json.write(response, describe());
            return;
        }
        if (!"PUT".equals(method) && !"POST".equals(method)) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET or PUT");
            return;
        }
        update(request, response);
    }

    private JsonObject describe() {
        JsonObject body = new JsonObject();
        body.addProperty("enhanced", config.plain.enhanced);
        // A silent <pre> fallback is the one thing an operator cannot see from
        // the page; the switch card shows this so "enhanced but no markdown"
        // has an explanation.
        body.addProperty("ssrAvailable", ssr.available());
        return body;
    }

    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "tls_required: the developer password is only accepted over HTTPS");
            return;
        }
        if (!AuthFilter.elevationSatisfied(request, elevation, keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return;
        }
        JsonObject body = readBody(request, response);
        if (body == null) {
            return;
        }
        if (!body.has("enhanced") || !body.get("enhanced").isJsonPrimitive()
                || !body.get("enhanced").getAsJsonPrimitive().isBoolean()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "enhanced must be a boolean");
            return;
        }
        boolean enhanced = body.get("enhanced").getAsBoolean();
        if (enhanced == config.plain.enhanced) {
            // 连点同一个值：不写盘，直接回答当前值。
            Json.write(response, describe());
            return;
        }
        try {
            ServerConfigWriter.updatePlainEnhanced(config.dataDir, enhanced);
        } catch (IOException e) {
            LOG.warning("Could not persist the plain-surface switch: " + e, e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "could not write server.yaml: " + e.getMessage());
            return;
        }
        config.plain.enhanced = enhanced;
        LOG.info("Plain surface enhancement " + (enhanced ? "enabled" : "disabled"));
        Json.write(response, describe());
    }

    private static String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null,
                request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static @Nullable JsonObject readBody(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "body too large");
            return null;
        }
        try {
            return com.google.gson.JsonParser.parseString(
                            new String(bytes, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "body is not a JSON object");
            return null;
        }
    }
}
