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
package org.jackhuang.hmcl.web.audit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/// The append-only audit trail: who did what, when, from where.
///
/// One JSON line per event under `<dataDir>/logs/audit-YYYY-MM-DD.jsonl`,
/// created owner-only (0600 where POSIX exists). Writes are flushed
/// immediately — an audit line that stayed in a buffer would be the first
/// thing a crash takes away. Every entry carries:
///
/// ```
/// {ts, channel(web|terminal), action, object?, outcome(success|failure|blocked), actor?, ip?}
/// ```
///
/// Meta events only: connections, toggles, authentications, spawns. What a
/// user types or a session contains never lands here.
///
/// The log method is a no-op until [#init] runs, so unit tests that never
/// boot a server stay silent. Re-initialising (every [org.jackhuang.hmcl.web.server.HdslServer]
/// build in the test JVM) closes the previous writer first.
@NotNullByDefault
public final class AuditLog {

    /// Which door the event came through.
    public enum Channel {
        WEB("web"),
        TERMINAL("terminal");

        private final String wireName;

        Channel(String wireName) {
            this.wireName = wireName;
        }
    }

    private static final Gson GSON = new Gson();
    private static volatile AuditLog instance;

    /// Opens the trail under `logsDir` (the panel's log directory), closing a
    /// previously opened one. Idempotent across server rebuilds in one JVM.
    public static synchronized void init(Path logsDir) {
        AuditLog previous = instance;
        if (previous != null) {
            previous.closeQuietly();
        }
        instance = new AuditLog(logsDir);
    }

    public static boolean initialized() {
        return instance != null;
    }

    /// Records one event. Never throws: a failing audit write must not take
    /// the operation it observed down with it.
    public static void log(Channel channel, String action, @Nullable String object,
                           String outcome, @Nullable String actor, @Nullable String ip) {
        log(channel, action, object, outcome, actor, ip, null);
    }

    /// Records one event with extra fields.
    ///
    /// The extra fields are merged after the base ones and after whatever the
    /// current [AgentFields] context carries, so a caller naming a field
    /// explicitly is never overridden by the context.
    ///
    /// @param extra the fields, values serialised as JSON, or `null`
    public static void log(Channel channel, String action, @Nullable String object,
                           String outcome, @Nullable String actor, @Nullable String ip,
                           @Nullable java.util.Map<String, Object> extra) {
        AuditLog log = instance;
        if (log == null) {
            return;
        }
        log.write(channel, action, object, outcome, actor, ip, extra);
    }

    /// The agent context every entry written on this thread carries.
    ///
    /// Set by the session gate when it admits an agent grant and cleared when
    /// the request ends, so every action an agent takes is logged with the
    /// grant that authorized it and the instance the grant is bound to — the
    /// fields the trail needs to answer "which agent, on whose authority,
    /// running where" without a second table.
    public static final class AgentFields {

        private final java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();

        /// Adds the grant's identity and capabilities.
        ///
        /// @param grantId  the grant's id
        /// @param scopes   the capability classes the grant holds
        /// @param binding  the instance the grant is bound to (`family/id`), or `null`
        public AgentFields(String grantId, java.util.Set<String> scopes, @Nullable String binding) {
            fields.put("grant", grantId);
            fields.put("scopes", java.util.List.copyOf(scopes));
            if (binding != null && !binding.isBlank()) {
                fields.put("caller", binding);
            }
        }

        /// One more field, for a refusal's reason or a mint's expiry.
        ///
        /// @param key   the field name
        /// @param value the value
        /// @return this
        public AgentFields put(String key, Object value) {
            fields.put(key, value);
            return this;
        }
    }

    private static final ThreadLocal<@Nullable AgentFields> AGENT_FIELDS = new ThreadLocal<>();

    /// Marks this thread as serving an admitted agent request.
    ///
    /// Cleared by [#endAgentFields] when the request ends. A handler that hops
    /// threads mid-request loses the context — the synchronous routes that
    /// matter (the fatal ones) never do.
    ///
    /// @param fields the agent context
    public static void beginAgentFields(AgentFields fields) {
        AGENT_FIELDS.set(fields);
    }

    /// Drops the agent context, in a `finally` so an exception cannot leak it
    /// onto the next request this thread serves.
    public static void endAgentFields() {
        AGENT_FIELDS.set(null);
    }

    private final Path logsDir;
    private final Object lock = new Object();
    private @Nullable BufferedWriter writer;
    private @Nullable String writerDay;

    private AuditLog(Path logsDir) {
        this.logsDir = logsDir;
    }

    private void write(Channel channel, String action, @Nullable String object,
                       String outcome, @Nullable String actor, @Nullable String ip,
                       @Nullable java.util.Map<String, Object> extra) {
        JsonObject event = new JsonObject();
        event.addProperty("ts", Instant.now().toString());
        event.addProperty("channel", channel.wireName);
        event.addProperty("action", action);
        if (object != null) {
            event.addProperty("object", object);
        }
        event.addProperty("outcome", outcome);
        if (actor != null) {
            event.addProperty("actor", actor);
        }
        if (ip != null) {
            event.addProperty("ip", ip);
        }
        AgentFields context = AGENT_FIELDS.get();
        if (context != null) {
            for (var entry : context.fields.entrySet()) {
                add(event, entry.getKey(), entry.getValue());
            }
        }
        if (extra != null) {
            for (var entry : extra.entrySet()) {
                add(event, entry.getKey(), entry.getValue());
            }
        }
        synchronized (lock) {
            try {
                BufferedWriter out = writerForToday();
                out.write(GSON.toJson(event));
                out.newLine();
                out.flush();
            } catch (IOException | RuntimeException e) {
                Logger.LOG.warning("Audit write failed: " + e);
            }
        }
    }

    /// Adds one extra field, letting a null value mean "omit" so a refusal
    /// without a derived identity does not carry a misleading `"caller":null`.
    private static void add(JsonObject event, String key, @Nullable Object value) {
        if (value != null) {
            event.add(key, GSON.toJsonTree(value));
        }
    }

    /// The writer for today, opening it (and closing yesterday's) on demand.
    private BufferedWriter writerForToday() throws IOException {
        String today = LocalDate.now().toString();
        if (writer != null && today.equals(writerDay)) {
            return writer;
        }
        closeQuietly();
        Files.createDirectories(logsDir);
        Path file = logsDir.resolve("audit-" + today + ".jsonl");
        boolean fresh = !Files.exists(file);
        BufferedWriter opened = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (fresh) {
            try {
                Files.setPosixFilePermissions(file, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException | IOException e) {
                // No POSIX permission model here (Windows): nothing stricter to ask for.
            }
        }
        writer = opened;
        writerDay = today;
        return opened;
    }

    private void closeQuietly() {
        BufferedWriter w = writer;
        writer = null;
        writerDay = null;
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {
                // Nothing useful to do with a close failure on rotate.
            }
        }
    }
}
