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
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.dsh.NpmRegistry;
import org.jackhuang.hmcl.dsh.PnpmConfigFile;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The download-source setting, mapped at `/api/settings/registry/*`:
///
/// - `GET  /api/settings/registry` — what is in force, where it came from, and the mirrors the
///   page offers: `{preset, registry, effective, source, presets[]}`.
/// - `POST /api/settings/registry` — `{preset}` for a published mirror, `{preset:"custom",
///   registry}` for one typed by hand. An address that will not be used is answered with a 400 and
///   **the setting is left alone**: a save that half-worked is worse than one that did not.
/// - `POST /api/settings/registry/test` — `{registry?}` measures one address from this machine and
///   answers `{ok, millis, status}`. The reason this exists: the whole problem this setting solves
///   is a registry that is reachable but unusably slow, and the page cannot tell which by looking.
///
/// Saving writes the address into pnpm's own configuration (see [PnpmConfigFile]) as well as into
/// the settings, because pnpm does not read the environment and would otherwise keep using the
/// registry the container was started with.
@NotNullByDefault
public final class RegistrySettingsApiServlet extends HttpServlet {

    /// How long the reachability probe may take. Short: a registry nobody can answer from in five
    /// seconds is the answer.
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    /// The package the probe asks for. Small, and old enough to be in every mirror that exists.
    private static final String PROBE_PACKAGE = "is-number";

    private static final String USER_AGENT = Metadata.NAME + "/" + Metadata.VERSION
            + " (+" + Metadata.HOMEPAGE_URL + ")";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        boolean test = "/test".equals(path);
        if (!test && path != null && !path.isEmpty() && !"/".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        if ("GET".equals(request.getMethod()) && !test) {
            Json.writePreservingNulls(response, describe());
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "only GET and POST");
            return;
        }
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        if (test) {
            probe(body, response);
            return;
        }
        save(request, body, response);
    }

    /// Reads the request body.
    ///
    /// @return the object, or `null` when the answer has already been written
    private static @Nullable JsonObject body(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String text = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (text.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON body is required");
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON object is required");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "the body is not JSON");
            return null;
        }
    }

    /// Applies a choice.
    private static void save(HttpServletRequest request, JsonObject body, HttpServletResponse response)
            throws IOException {
        String preset = stringField(body, "preset");
        if (preset == null || preset.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "preset is required");
            return;
        }
        String wanted = preset.trim();

        if (NpmRegistry.CUSTOM.equals(wanted)) {
            String typed = stringField(body, "registry");
            String normalized;
            try {
                normalized = NpmRegistry.normalize(typed);
            } catch (NpmRegistry.Invalid e) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            SettingsManager.settings().setNpmRegistry(normalized);
        } else if (!NpmRegistry.ENVIRONMENT.equals(wanted) && NpmRegistry.preset(wanted) == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown download source: " + wanted);
            return;
        }
        SettingsManager.settings().setNpmRegistryPreset(wanted);
        // The setting is saved by its own listener; pnpm's file is not, and it is the one that
        // decides what the next install actually fetches.
        PnpmConfigFile.apply();
        LOG.info("The download source is now " + NpmRegistry.effective().registry()
                + " (" + NpmRegistry.effective().source() + ")");
        AuditLog.log(AuditLog.Channel.WEB, "settings.registry", NpmRegistry.effective().registry(),
                "success", actorOf(request), request.getRemoteAddr());
        Json.writePreservingNulls(response, describe());
    }

    /// The session's username for the audit trail, or null when the request
    /// carries no session.
    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// Measures one address.
    private static void probe(JsonObject body, HttpServletResponse response) throws IOException {
        String registry = stringField(body, "registry");
        if (registry == null || registry.isBlank()) {
            registry = NpmRegistry.effective().registry();
        }
        String normalized;
        try {
            normalized = NpmRegistry.normalize(registry);
        } catch (NpmRegistry.Invalid e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        String url = normalized + "/" + PROBE_PACKAGE;
        long started = System.currentTimeMillis();
        JsonObject answer = new JsonObject();
        answer.addProperty("registry", normalized);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(PROBE_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(PROBE_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                HttpResponse<Void> probe = client.send(request, HttpResponse.BodyHandlers.discarding());
                answer.addProperty("ok", probe.statusCode() >= 200 && probe.statusCode() < 300);
                answer.addProperty("status", probe.statusCode());
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException | RuntimeException e) {
            answer.addProperty("ok", false);
            answer.add("status", com.google.gson.JsonNull.INSTANCE);
            answer.addProperty("error", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            answer.addProperty("ok", false);
            answer.add("status", com.google.gson.JsonNull.INSTANCE);
            answer.addProperty("error", "interrupted");
        }
        answer.addProperty("millis", System.currentTimeMillis() - started);
        Json.writePreservingNulls(response, answer);
    }

    /// Describes the setting as the page needs it.
    private static JsonObject describe() {
        String preset = SettingsManager.settings().getNpmRegistryPreset();
        NpmRegistry.Effective effective = NpmRegistry.effective();

        JsonArray presets = new JsonArray();
        for (NpmRegistry.Preset one : NpmRegistry.presets()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", one.id());
            entry.addProperty("label", one.label());
            entry.addProperty("url", one.url());
            entry.addProperty("note", one.note());
            presets.add(entry);
        }

        JsonObject body = new JsonObject();
        body.addProperty("preset", preset);
        // The last address typed by hand, kept even while another preset is chosen: switching back
        // to `custom` should show what was there rather than an empty box.
        body.addProperty("registry", SettingsManager.settings().getNpmRegistry());
        body.addProperty("effective", effective.registry());
        body.addProperty("source", effective.source());
        body.add("presets", presets);
        return body;
    }

    private static @Nullable String stringField(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
