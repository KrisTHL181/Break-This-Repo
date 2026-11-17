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
package org.jackhuang.hmcl.web.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.http.Json;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/// `/api/system/*` — what the machine looks like, and where the panel lives.
///
/// Two reads, both behind the session gate (and readable by an agent ticket
/// holding the `system` class):
///
/// - `GET /api/system/stats[?history=N]` — the newest sample, optionally with
///   up to [#SystemMetrics.HISTORY_SIZE] earlier ones so a freshly opened page
///   can draw the last minutes immediately. The sampling period is **not** a
///   request parameter: the server decides, at most once a second.
/// - `GET /api/system/info` — the panel's own coordinates (bind address, port,
///   mount, data directory, version). No certificate paths, no credentials.
///
/// This servlet never touches the developer elevation: the metrics are exactly
/// what a shell in the same container could read, so asking for a password
/// would add a prompt without adding a boundary. The developer guard agrees —
/// `/api/system/` is on its allowlist.
@NotNullByDefault
public final class SystemApiServlet extends HttpServlet {

    private final SystemMetrics metrics;
    private final ServerConfig config;
    private final LongSupplier startedAtMillis;
    private final IntSupplier boundPort;
    /// Walking the data directory is seconds, not milliseconds; the page gets a
    /// cached report and can ask for a fresh walk explicitly.
    private static final long STORAGE_TTL_MS = 60_000;
    private volatile StorageUsage.@Nullable Report storageReport;
    private volatile long storageAt;

    /// Whole responses, keyed by the sequence of the sample they end on and by
    /// `?history=`. Rebuilding this per request was the one endpoint an order of
    /// magnitude below the rest: 240 snapshots to serialise, ~250 KB to write,
    /// 811 rps against 9k on the audit box, and a cheap way for a `system`-scoped
    /// agent to burn CPU. Caching the JSON tree alone got 837 → 1.4k; the body
    /// is only built once per sample now, so the remaining cost is the write.
    private final Map<Integer, CachedBody> bodies = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, CachedBody> eldest) {
            return size() > 8;
        }
    };

    /// @param sequence the sample the body ends on
    /// @param bytes    the serialised response
    private record CachedBody(long sequence, byte[] bytes) {
    }

    public SystemApiServlet(SystemMetrics metrics, ServerConfig config, LongSupplier startedAtMillis,
                            IntSupplier boundPort) {
        this.metrics = metrics;
        this.config = config;
        this.startedAtMillis = startedAtMillis;
        this.boundPort = boundPort;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (!"GET".equals(request.getMethod())) {
            Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET");
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        if ("/stats".equals(path)) {
            stats(request, response);
            return;
        }
        if ("/info".equals(path)) {
            info(response);
            return;
        }
        if ("/storage".equals(path)) {
            storage(request, response);
            return;
        }
        if ("/platform".equals(path)) {
            Json.write(response, Json.GSON.toJsonTree(PlatformCompatibility.current()));
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void stats(HttpServletRequest request, HttpServletResponse response) throws IOException {
        metrics.start();
        int history = parseInt(request.getParameter("history"), 0);
        history = Math.max(0, Math.min(history, SystemMetrics.HISTORY_SIZE));

        // One frame for both halves of the answer: the sequence and the samples
        // come out of the same lock, so a cached body can never be filed under
        // a sample it does not end on.
        byte[] bytes = body(metrics.historyFrame(SystemMetrics.HISTORY_SIZE), history);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(Json.CONTENT_TYPE);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    /// The response for `history=max`, built at most once per sample.
    private byte[] body(SystemMetrics.Frame frame, int max) {
        List<SystemSnapshot> samples = frame.samples();
        synchronized (this) {
            CachedBody cached = bodies.get(max);
            if (cached != null && cached.sequence() == frame.sequence()) {
                return cached.bytes();
            }
            SystemSnapshot latest = samples.isEmpty() ? null : samples.get(samples.size() - 1);
            JsonObject body = new JsonObject();
            body.add("snapshot",
                    latest == null ? com.google.gson.JsonNull.INSTANCE : Json.GSON.toJsonTree(latest));
            JsonArray array = new JsonArray(max);
            for (int i = Math.max(0, samples.size() - max); i < samples.size(); i++) {
                array.add(Json.GSON.toJsonTree(samples.get(i)));
            }
            body.add("history", array);
            byte[] bytes = Json.GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
            bodies.put(max, new CachedBody(frame.sequence(), bytes));
            return bytes;
        }
    }

    private void info(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("version", Metadata.VERSION);
        body.addProperty("java", String.valueOf(Runtime.version().feature()));
        body.addProperty("bindHost", config.bindHost == null || config.bindHost.isBlank()
                ? "0.0.0.0" : config.bindHost);
        // The connector's real port, not the configured one: `port: 0` means
        // "pick a free port", and the answer must be the door that opened.
        int port = boundPort.getAsInt();
        body.addProperty("port", port > 0 ? port : config.port);
        body.addProperty("basePath", config.basePath == null ? "" : config.basePath);
        body.addProperty("dataDir", config.dataDir.toAbsolutePath().normalize().toString());
        body.addProperty("publicBaseUrl", config.publicBaseUrl == null ? "" : config.publicBaseUrl);
        body.addProperty("https", config.https.enabled);
        body.addProperty("security", config.https.hasExplicitCertificate()
                ? (config.https.pkcs12File.isBlank() ? "pem" : "pkcs12")
                : "self-signed-or-http");
        body.addProperty("startedAt", startedAtMillis.getAsLong());
        Json.write(response, body);
    }

    private void storage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean refresh = "1".equals(request.getParameter("refresh"));
        StorageUsage.Report cached = storageReport;
        if (cached == null || refresh || System.currentTimeMillis() - storageAt >= STORAGE_TTL_MS) {
            synchronized (this) {
                cached = storageReport;
                if (cached == null || refresh || System.currentTimeMillis() - storageAt >= STORAGE_TTL_MS) {
                    cached = StorageUsage.compute(config.dataDir);
                    storageReport = cached;
                    storageAt = System.currentTimeMillis();
                }
            }
        }
        Json.write(response, cached);
    }

    private static int parseInt(String text, int fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
