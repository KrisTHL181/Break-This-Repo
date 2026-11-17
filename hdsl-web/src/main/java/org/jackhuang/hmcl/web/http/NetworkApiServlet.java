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

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.util.platform.OutboundTls;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/// The outbound-TLS acknowledgement, as an API.
///
/// - `GET /api/network/trust` — `{mode, acceptedUntil, lastTrouble?, explanation?}`
/// - `POST /api/network/trust` — `{minutes, acknowledge}`: opens a window in which
///   a certificate the machine cannot verify is accepted; `acknowledge` must be
///   the sentence below, so this is a decision and not a toggle somebody clicks
///   past
/// - `DELETE /api/network/trust` — closes it
///
/// Why a person has to say yes: the failure this answers is a proxy in the middle
/// (an antivirus, a gateway, a GitHub accelerator) whose root lives in the
/// Windows store and not in the JVM's. Accepting it is sometimes the right thing,
/// and it is never something the panel should decide on its own — the panel's own
/// words in the error are "浏览器打得开、我打不开", and closing that gap by
/// trusting everything silently would be a worse panel. So: strict by default,
/// the reason spelled out, one acknowledgement, a window that expires, an audit
/// line, and a way back.
@NotNullByDefault
public final class NetworkApiServlet extends HttpServlet {

    /// What the caller has to type. A sentence, not a boolean: a check box is
    /// clicked past, this is read.
    public static final String ACKNOWLEDGEMENT = "我确认这是我自己的网络代理";

    private final ServerConfig config;

    public NetworkApiServlet(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        JsonObject body = state();
        OutboundTls.Trouble trouble = OutboundTls.lastTrouble();
        if (trouble == null) {
            body.add("lastTrouble", JsonNull.INSTANCE);
        } else {
            JsonObject item = new JsonObject();
            item.addProperty("host", trouble.host());
            item.addProperty("reason", trouble.reason());
            item.addProperty("at", trouble.at().toString());
            body.add("lastTrouble", item);
            body.addProperty("explanation", OutboundTls.explain(trouble));
        }
        write(response, body);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        String acknowledged = stringField(body, "acknowledge");
        if (!ACKNOWLEDGEMENT.equals(acknowledged == null ? null : acknowledged.trim())) {
            Json.error(response, 400, "acknowledge must be `" + ACKNOWLEDGEMENT + "`: this is a decision, "
                    + "so it is typed rather than clicked");
            return;
        }
        int minutes = intField(body, "minutes", 60);
        if (minutes < 1 || minutes > OutboundTls.MAX_WINDOW.toMinutes()) {
            Json.error(response, 400, "minutes must be between 1 and "
                    + OutboundTls.MAX_WINDOW.toMinutes());
            return;
        }
        Instant until = Instant.now().plus(Duration.ofMinutes(minutes));
        open(until);
        AuditLog.log(AuditLog.Channel.WEB, "network.trust.accept", null, "success",
                actorOf(request), request.getRemoteAddr());
        write(response, state());
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        open(null);
        AuditLog.log(AuditLog.Channel.WEB, "network.trust.revoke", null, "success",
                actorOf(request), request.getRemoteAddr());
        write(response, state());
    }

    /// Applies the window to the JVM *and* to `server.yaml`: a consent that did not
    /// survive the restart would be asked for again the moment a new jar lands,
    /// which is exactly when the network is still the same network.
    private void open(@Nullable Instant until) throws IOException {
        OutboundTls.accept(until);
        config.network.acceptUnverifiedUntil = until;
        ServerConfigWriter.acceptUnverifiedTls(dataDir(), until);
    }

    private Path dataDir() {
        return config.dataDir;
    }

    private JsonObject state() {
        JsonObject body = new JsonObject();
        Instant until = OutboundTls.acceptedUntil();
        body.addProperty("mode", until == null ? "strict" : "accept_unverified");
        if (until == null) {
            body.add("acceptedUntil", JsonNull.INSTANCE);
        } else {
            body.addProperty("acceptedUntil", until.toString());
        }
        body.addProperty("acknowledge", ACKNOWLEDGEMENT);
        body.addProperty("maxMinutes", OutboundTls.MAX_WINDOW.toMinutes());
        return body;
    }

    /// The reply keeps explicit nulls: `lastTrouble: null` is a fact the page
    /// renders ("no failure"), and a dropped key is `undefined` there.
    private static void write(HttpServletResponse response, JsonObject body) {
        try {
            Json.writePreservingNulls(response, body);
        } catch (IOException e) {
            // The client is gone; there is nothing to report to anybody else.
        }
    }

    /// The body as JSON, decoded as UTF-8 on purpose: the acknowledgement is a
    /// Chinese sentence, and a container that defaults `getReader()` to
    /// ISO-8859-1 would turn it into mojibake that never equals it.
    private static JsonObject readJson(HttpServletRequest request) {
        try {
            byte[] raw = request.getInputStream().readAllBytes();
            if (raw.length == 0) {
                return new JsonObject();
            }
            return com.google.gson.JsonParser.parseString(new String(raw, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return new JsonObject();
        }
    }

    private static @Nullable String stringField(JsonObject body, String name) {
        try {
            return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int intField(JsonObject body, String name, int fallback) {
        try {
            return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /// Who accepted it, for the audit trail.
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session value ? value.username() : null;
    }
}
