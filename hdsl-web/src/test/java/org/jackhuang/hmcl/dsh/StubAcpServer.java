/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/// A minimal Agent Client Protocol peer used by the client tests.
///
/// DeepSeek Harness reserves ACP stdout for protocol frames and needs a real
/// model credential before it will stream assistant text, so the streaming path
/// cannot be observed against the real server in a test. This stub answers the
/// same four calls and emits the same notifications, which is enough to verify
/// the launcher's framing, request correlation and notification dispatch.
///
/// It is a test fixture, not a product component: it never leaves the test
/// source set.
public final class StubAcpServer {
    private StubAcpServer() {
    }

    /// The session id the stub hands out.
    public static final String SESSION_ID = "stub-session";

    /// The assistant text the stub streams, in two chunks.
    public static final String[] CHUNKS = {"PON", "G"};

    /// The stop reason the stub reports.
    public static final String STOP_REASON = "end_turn";

    /// Serves one connection until stdin closes.
    ///
    /// @param args ignored
    /// @throws Exception when the streams cannot be used
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            JsonObject message = JsonParser.parseString(line).getAsJsonObject();
            JsonElement method = message.get("method");
            if (method == null) {
                continue;
            }

            switch (method.getAsString()) {
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
                    for (String chunk : CHUNKS) {
                        notifyUpdate(out, sessionId, chunk);
                    }
                    JsonObject result = new JsonObject();
                    result.addProperty("stopReason", STOP_REASON);
                    respond(out, message, result);
                }
                default -> {
                    // A notification such as session/cancel: nothing to answer.
                }
            }
        }
    }

    /// Writes one JSON-RPC response.
    ///
    /// @param out     the output stream
    /// @param request the request being answered
    /// @param result  the result object
    /// @throws Exception when the stream cannot be written
    private static void respond(BufferedWriter out, JsonObject request, JsonObject result) throws Exception {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.add("id", request.get("id"));
        message.add("result", result);
        write(out, message);
    }

    /// Writes one `session/update` notification carrying assistant text.
    ///
    /// @param out       the output stream
    /// @param sessionId the session the chunk belongs to
    /// @param text      the chunk
    /// @throws Exception when the stream cannot be written
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

    /// Writes one frame and flushes it.
    ///
    /// @param out     the output stream
    /// @param message the message
    /// @throws Exception when the stream cannot be written
    private static void write(BufferedWriter out, JsonObject message) throws Exception {
        out.write(message.toString());
        out.write('\n');
        out.flush();
    }
}
