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
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.logs.ClockGuard;
import org.jackhuang.hmcl.web.logs.LogPreview;
import org.jackhuang.hmcl.web.logs.LogRetention;
import org.jackhuang.hmcl.web.logs.RetentionUpdate;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// `/api/settings/logs` — how long the panel keeps its own files.
///
/// - `GET` answers the windows in force plus the bounds the panel accepts:
///   `{auditDays, panelDays, floorDays, ceilingDays}`.
/// - `PUT` takes `{auditDays?, panelDays?}` and persists to `server.yaml`.
///
/// The rules live in [RetentionUpdate] (numbers only, exact integers, allow-list,
/// raise-only, 180–1460 days) so the HTTP layer stays a thin shell over them.
///
/// Two behaviours exist purely against a bored or impatient caller:
///
/// - **写同一个值 = 幂等**：连点同一个数字不会写盘、不会记审计、不会撞限速；
/// - **真改值 3 秒一次**：改窗口会重写 `server.yaml`，连点会让磁盘和日志一起抖，
///   所以同一会话 3 秒内只接受一次**改动**。
///
/// Nothing here takes a path, a filename or a shell string from the caller, and the
/// body is capped at [MAX_BODY_BYTES] before parsing — a 4 MB JSON bombshell never
/// reaches the parser.
@NotNullByDefault
public final class LogRetentionApiServlet extends HttpServlet {

    /// 一个设置请求不需要更多；超过就当攻击处理。
    private static final int MAX_BODY_BYTES = 4096;

    /// 连点保护窗口：同一会话里两次**改动**之间至少隔这么久。
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(3);

    /// 限速表的清理阈值；超过就顺手清掉十分钟前的记录。
    private static final int RATE_TABLE_LIMIT = 256;

    private final ServerConfig config;
    private final AuthService auth;
    private final DeveloperElevation elevation;
    private final BooleanSupplier tlsActive;

    /// key（会话令牌或来源地址）→ 上次真正改动的时间。
    private final ConcurrentHashMap<String, Instant> lastChange = new ConcurrentHashMap<>();

    public LogRetentionApiServlet(ServerConfig config, DeveloperElevation elevation, AuthService auth,
                                  BooleanSupplier tlsActive) {
        this.config = config;
        this.elevation = elevation;
        this.auth = auth;
        this.tlsActive = tlsActive;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        String method = request.getMethod();
        if ("/recent".equals(path)) {
            if (!"GET".equals(method)) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET");
                return;
            }
            recent(response);
            return;
        }
        if (path != null && path.startsWith("/recent/")) {
            if (!"GET".equals(method)) {
                Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use GET");
                return;
            }
            tail(path.substring("/recent/".length()), request, response);
            return;
        }
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
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
        body.addProperty("auditDays", config.logs.auditDays);
        body.addProperty("panelDays", config.logs.panelDays);
        body.addProperty("floorDays", LogRetention.FLOOR_DAYS);
        body.addProperty("ceilingDays", LogRetention.HARD_CAP_DAYS);
        body.addProperty("viewDays", LogPreview.VIEW_DAYS);

        LogRetention retention = LogRetention.installed();
        if (retention != null) {
            // 时钟可不可信：界面要能解释“为什么这轮一个文件都没删”。
            ClockGuard.Status clock = retention.clockStatus();
            JsonObject clockRow = new JsonObject();
            clockRow.addProperty("trusted", clock.trusted());
            clockRow.addProperty("reason", clock.reason());
            clockRow.addProperty("skewMillis", clock.skewMillis());
            clockRow.addProperty("checkedAt", clock.checkedAt() == null ? null : clock.checkedAt().toString());
            body.add("clock", clockRow);

            LogRetention.SweepResult sweep = retention.lastSweep();
            JsonObject sweepRow = new JsonObject();
            sweepRow.addProperty("deleted", sweep.deleted());
            sweepRow.addProperty("at", sweep.at() == null ? null : sweep.at().toString());
            sweepRow.addProperty("skippedReason", sweep.skippedReason());
            body.add("lastSweep", sweepRow);
        }
        return body;
    }

    /// 界面上能看的日志清单——**只有最近 [LogPreview#VIEW_DAYS] 天**。
    ///
    /// 窗口写死在服务端，不是给前端一个参数：能改的窗口等于没有窗口，
    /// 而“界面能看多久”本来就该由服务端说了算。
    private void recent(HttpServletResponse response) throws IOException {
        List<LogPreview.Entry> entries = LogPreview.recent(logsDir(), Instant.now(), LogPreview.VIEW_DAYS);
        JsonArray files = new JsonArray();
        for (LogPreview.Entry entry : entries) {
            JsonObject row = new JsonObject();
            row.addProperty("name", entry.name());
            row.addProperty("kind", entry.kind());
            row.addProperty("bytes", entry.bytes());
            row.addProperty("modified", entry.modified().toString());
            row.addProperty("compressed", entry.compressed());
            files.add(row);
        }
        JsonObject body = new JsonObject();
        body.addProperty("viewDays", LogPreview.VIEW_DAYS);
        body.add("files", files);
        Json.write(response, body);
    }

    /// 一份日志的尾巴。名字必须是目录里的一项，且仍在窗口内；越界一律同一条回答。
    private void tail(String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int lines = intParam(request.getParameter("tail"), LogPreview.DEFAULT_TAIL_LINES);
        try {
            JsonArray text = new JsonArray();
            LogPreview.tail(logsDir(), name, Instant.now(), LogPreview.VIEW_DAYS, lines).forEach(text::add);
            JsonObject body = new JsonObject();
            body.addProperty("name", name);
            body.add("lines", text);
            Json.write(response, body);
        } catch (LogPreview.Refused e) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private java.nio.file.Path logsDir() {
        return config.dataDir.resolve("logs");
    }

    private static int intParam(@Nullable String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readBody(request, response);
        if (body == null) {
            return;
        }

        int currentAudit = config.logs.auditDays;
        int currentPanel = config.logs.panelDays;
        RetentionUpdate.Windows windows;
        try {
            windows = RetentionUpdate.parse(body, currentAudit, currentPanel);
        } catch (RetentionUpdate.Invalid e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        if (windows.auditDays() == currentAudit && windows.panelDays() == currentPanel) {
            // 连点同一个值：不写盘、不记审计，直接回答当前值。
            Json.write(response, describe());
            return;
        }

        String key = rateKey(request);
        Instant now = Instant.now();
        Instant previous = lastChange.get(key);
        if (previous != null && previous.plus(MIN_INTERVAL).isAfter(now)) {
            long seconds = Math.max(1, Duration.between(now, previous.plus(MIN_INTERVAL)).toSeconds() + 1);
            response.setHeader("Retry-After", Long.toString(seconds));
            Json.error(response, 429, "too fast: retention may change once every "
                    + MIN_INTERVAL.toSeconds() + " seconds");
            return;
        }

        try {
            ServerConfigWriter.setLogRetention(config.dataDir, windows.auditDays(), windows.panelDays());
        } catch (IOException e) {
            LOG.warning("Could not persist log retention: " + e, e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "could not write server.yaml: " + e.getMessage());
            return;
        }

        config.logs.auditDays = windows.auditDays();
        config.logs.panelDays = windows.panelDays();
        LogRetention retention = LogRetention.installed();
        if (retention != null) {
            retention.setWindows(windows.auditDays(), windows.panelDays());
        }
        lastChange.put(key, now);
        pruneRateTable(now);

        AuditLog.log(AuditLog.Channel.WEB, "settings.logs.retention",
                "audit=" + windows.auditDays() + ",panel=" + windows.panelDays(),
                "success", actorOf(request), request.getRemoteAddr());
        Json.write(response, describe());
    }

    private String rateKey(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        if (token instanceof String value && !value.isBlank()) {
            return "token:" + value;
        }
        return "addr:" + request.getRemoteAddr();
    }

    private void pruneRateTable(Instant now) {
        if (lastChange.size() <= RATE_TABLE_LIMIT) {
            return;
        }
        Instant cutoff = now.minus(Duration.ofMinutes(10));
        lastChange.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    private boolean requireElevation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "tls_required: the developer password is only accepted over HTTPS");
            return false;
        }
        if (!AuthFilter.elevationSatisfied(
                request, elevation, keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return false;
        }
        return true;
    }

    private static String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// 读请求体：先卡字节数，再解析；任何一步不合格都当场回 400/413。
    private static @Nullable JsonObject readBody(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            Json.error(response, 413, "body too large");
            return null;
        }
        if (bytes.length == 0) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON body is required");
            return null;
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "malformed JSON");
            return null;
        }
        if (!parsed.isJsonObject()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON object is required");
            return null;
        }
        return parsed.getAsJsonObject();
    }
}
