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
package org.jackhuang.hmcl.web.acp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.DshAcpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/// A minimal Agent Client Protocol peer for the bridge tests, modelled on
/// `org.jackhuang.hmcl.dsh.StubAcpServer`.
///
/// It answers the four calls the bridge drives and emits `session/update`
/// notifications, but it adds the two observations the bridge's own tests need:
/// it records every `session/cancel` it receives into a file, and it writes its
/// PID so a test can watch the process end. A prompt is streamed on a worker
/// thread, so a cancel sent while one is in flight is read rather than queued
/// behind it.
///
/// It is a test fixture, not a product component.
public final class AcpStubPeer {

    /// The session id the peer hands out.
    public static final String SESSION_ID = "bridge-session";

    /// The assistant text the peer streams, in chunks.
    public static final String[] CHUNKS = {"BRIDGE-", "OK"};

    /// The stop reason the peer reports.
    public static final String STOP_REASON = "end_turn";

    /// The environment variable naming the file cancels are recorded in.
    public static final String CANCEL_LOG_ENV = "ACP_STUB_CANCEL_LOG";

    /// The environment variable naming the file the PID is written to.
    public static final String PID_FILE_ENV = "ACP_STUB_PID_FILE";

    /// The environment variable delaying a prompt's response, in milliseconds —
    /// long enough for a test to send a second prompt, or a cancel.
    public static final String PROMPT_DELAY_ENV = "ACP_STUB_PROMPT_DELAY_MS";

    private static final Object OUT_LOCK = new Object();

    private AcpStubPeer() {
    }

    /// Serves one connection until stdin closes.
    ///
    /// @param args ignored
    /// @throws Exception when the streams cannot be used
    public static void main(String[] args) throws Exception {
        Path pidFile = pathFromEnv(PID_FILE_ENV);
        if (pidFile != null) {
            Files.writeString(pidFile, Long.toString(ProcessHandle.current().pid()));
        }
        Path cancelLog = pathFromEnv(CANCEL_LOG_ENV);
        long delay = longFromEnv(PROMPT_DELAY_ENV);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            JsonObject message = JsonParser.parseString(line).getAsJsonObject();
            JsonElement methodElement = message.get("method");
            if (methodElement == null) {
                continue;
            }

            switch (methodElement.getAsString()) {
                case "initialize" -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("protocolVersion", DshAcpClient.PROTOCOL_VERSION);
                    respond(out, message, result);
                }
                case "session/new" -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("sessionId", SESSION_ID);
                    respond(out, message, result);
                }
                case "session/prompt" -> {
                    String sessionId = message.getAsJsonObject("params").get("sessionId").getAsString();
                    Thread worker = new Thread(() -> streamPrompt(out, message, sessionId, delay));
                    worker.setDaemon(true);
                    worker.start();
                }
                case "session/cancel" -> {
                    String sessionId = message.getAsJsonObject("params").get("sessionId").getAsString();
                    if (cancelLog != null) {
                        Files.writeString(cancelLog, "cancel " + sessionId + "\n", StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                }
                default -> {
                    // Nothing else is part of the contract the bridge drives.
                }
            }
        }
    }

    /// Streams one prompt's chunks, then answers it.
    private static void streamPrompt(BufferedWriter out, JsonObject request, String sessionId, long delay) {
        try {
            for (String chunk : CHUNKS) {
                notifyUpdate(out, sessionId, chunk);
            }
            if (delay > 0) {
                Thread.sleep(delay);
            }
            JsonObject result = new JsonObject();
            result.addProperty("stopReason", STOP_REASON);
            respond(out, request, result);
        } catch (Exception e) {
            // stdin is gone or the peer is being killed; nothing to answer.
        }
    }

    /// Writes one JSON-RPC response.
    private static void respond(BufferedWriter out, JsonObject request, JsonObject result) throws Exception {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.add("id", request.get("id"));
        message.add("result", result);
        write(out, message);
    }

    /// Writes one `session/update` notification carrying assistant text.
    private static void notifyUpdate(BufferedWriter out, String sessionId, String text) throws Exception {
        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", text);

        JsonObject update = new JsonObject();
        update.addProperty("sessionUpdate", "agent_message_chunk");
        update.add("content", content);

        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        params.add("update", update);

        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("method", "session/update");
        message.add("params", params);
        write(out, message);
    }

    private static void write(BufferedWriter out, JsonObject message) throws Exception {
        synchronized (OUT_LOCK) {
            out.write(message.toString());
            out.write('\n');
            out.flush();
        }
    }

    private static Path pathFromEnv(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static long longFromEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
