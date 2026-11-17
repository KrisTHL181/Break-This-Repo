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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.platform.ManagedProcess;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;
import org.jackhuang.hmcl.util.platform.Privileges;

/// A client for DeepSeek Harness's Agent Client Protocol surface.
///
/// ACP is the launcher's only route to a *conversation* rather than a browser
/// tab. Upstream documents it as the stable automation interface: one
/// newline-delimited JSON-RPC 2.0 stream over stdio, advertising only what is
/// actually mounted. The launcher deliberately does not touch the web
/// interface's internal `/api` gateway, whose routes are composition-dependent
/// and authenticated by a browser cookie.
///
/// The client is intentionally small. It speaks the four calls a chat panel
/// needs — `initialize`, `session/new`, `session/prompt`, `session/cancel` —
/// and surfaces `session/update` notifications, rather than mirroring the whole
/// ACP schema.
@NotNullByDefault
public final class DshAcpClient implements AutoCloseable {
    /// The ACP protocol version this client speaks.
    public static final int PROTOCOL_VERSION = 1;

    /// How long a request waits for its response before giving up.
    private static final long REQUEST_TIMEOUT_SECONDS = 120;

    /// Receives the events of one session.
    public interface Listener {
        /// Called for a chunk of assistant text.
        ///
        /// @param text the chunk
        default void onText(String text) {
        }

        /// Called for a chunk of assistant reasoning.
        ///
        /// @param text the chunk
        default void onThought(String text) {
        }

        /// Called when a tool call starts or changes state.
        ///
        /// @param title  the tool title
        /// @param status the reported status
        default void onToolCall(String title, String status) {
        }

        /// Called for every `session/update` notification, with the `update`
        /// object exactly as it arrived.
        ///
        /// The callbacks above are a convenience over this one; a transport
        /// that must preserve every field — a tool call's id and raw input, a
        /// chunk's content array — reads the raw update here instead. The
        /// object is only valid for the duration of the call.
        ///
        /// @param update the notification's `update` object
        default void onUpdate(JsonObject update) {
        }

        /// Called when a prompt settles.
        ///
        /// @param stopReason the protocol stop reason
        default void onPromptFinished(String stopReason) {
        }

        /// Called when the connection fails or the child exits.
        ///
        /// @param message a description of the failure
        default void onFailure(String message) {
        }
    }

    private final DshInstance instance;
    private final ManagedProcess process;
    private final BufferedWriter writer;
    private final Thread readerThread;

    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final java.util.Deque<String> stderrTail = new java.util.ArrayDeque<>();

    private volatile @Nullable Listener listener;
    private volatile boolean closed;

    /// Starts an ACP child for an instance.
    ///
    /// @param instance    the instance whose profile speaks ACP
    /// @param command     the exact command to run
    /// @param workingDirectory the directory sessions are scoped to
    /// @param environment extra environment variables for the child
    /// @param listener    the listener for session events
    /// @throws DshException when the child cannot start
    private DshAcpClient(DshInstance instance,
                         List<String> command,
                         Path workingDirectory,
                         Map<String, String> environment,
                         Listener listener) throws DshException {
        this.instance = instance;
        this.listener = listener;

        ProcessBuilder builder = new ProcessBuilder(Privileges.asAgent(command));
        builder.directory(workingDirectory.toFile());
        builder.environment().putAll(environment);

        try {
            this.process = new ManagedProcess(builder);
        } catch (IOException e) {
            throw new DshException("Failed to start the ACP session: " + e.getMessage(), e);
        }

        this.writer = new BufferedWriter(
                new OutputStreamWriter(process.getProcess().getOutputStream(), StandardCharsets.UTF_8));

        process.pumpInputStream(this::handleLine, StandardCharsets.UTF_8);
        process.pumpErrorStream(line -> {
            synchronized (stderrTail) {
                stderrTail.addLast(line);
                while (stderrTail.size() > 200) {
                    stderrTail.removeFirst();
                }
            }
            LOG.info("[acp] " + line);
        }, StandardCharsets.UTF_8);

        this.readerThread = new Thread(this::awaitExit, "HDSL acp waiter");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /// Builds the command that boots an instance's profile in ACP mode.
    ///
    /// @param instance the instance
    /// @return the command
    /// @throws DshException when the runtime or version is unavailable
    private static List<String> commandFor(DshInstance instance) throws DshException {
        DshNodeRuntime runtime = DshLauncher.resolveRuntime(instance);
        Path script = instance.dshEntryPoint();
        if (!Files.isRegularFile(script)) {
            throw new DshException("Instance " + instance.id()
                    + " has no DeepSeek Harness of its own; " + script + " is missing");
        }
        return List.of(
                runtime.node().toString(),
                script.toString(),
                "--profile", "acp");
    }

    /// Builds the child environment for an instance.
    ///
    /// @param instance the instance
    /// @return the environment additions
    /// @throws DshException when the instance's home cannot be resolved
    private static Map<String, String> environmentFor(DshInstance instance) throws DshException {
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        // DSH_* cannot come from a .env file: upstream rejects those names there.
        environment.put("DSH_HOME", instance.homeDirectory().toString());
        environment.putAll(DshEnvironment.of(instance));
        return environment;
    }

    /// Starts a client for a DeepSeek Harness instance.
    ///
    /// @param instance the instance to talk to
    /// @param listener the listener for session events
    /// @return the connected client
    /// @throws DshException when the child cannot start or the handshake fails
    public static DshAcpClient connect(DshInstance instance, Listener listener) throws DshException {
        return connect(instance, commandFor(instance), instance.workspacePath(), environmentFor(instance), listener);
    }

    /// Starts a client for an instance in an explicit working directory.
    ///
    /// The command and environment are the instance's own, as in
    /// [#connect(DshInstance, Listener)]; only the directory the child runs in is
    /// the caller's. [#newSession(Path)] roots a session in that directory, while
    /// [#newSession()] still roots it in the instance's own workspace.
    ///
    /// @param instance         the instance to talk to
    /// @param workingDirectory the directory the child runs in
    /// @param listener         the listener for session events
    /// @return the connected client
    /// @throws DshException when the child cannot start or the handshake fails
    public static DshAcpClient connect(DshInstance instance, Path workingDirectory, Listener listener)
            throws DshException {
        return connect(instance, commandFor(instance), workingDirectory, environmentFor(instance), listener);
    }

    /// Starts a client over an explicitly supplied transport.
    ///
    /// Separated from [#connect(DshInstance, Listener)] so the protocol layer can
    /// be exercised against a stub peer — the streaming path otherwise needs a
    /// live model credential to observe.
    ///
    /// @param instance         the instance the session is attributed to
    /// @param command          the exact command to run
    /// @param workingDirectory the directory sessions are scoped to
    /// @param environment      extra environment variables for the child
    /// @param listener         the listener for session events
    /// @return the connected client
    /// @throws DshException when the child cannot start or the handshake fails
    public static DshAcpClient connect(DshInstance instance,
                                       List<String> command,
                                       Path workingDirectory,
                                       Map<String, String> environment,
                                       Listener listener) throws DshException {
        DshAcpClient client = new DshAcpClient(instance, command, workingDirectory, environment, listener);
        try {
            client.handshake();
        } catch (DshException e) {
            client.close();
            throw e;
        }
        return client;
    }

    /// Performs the `initialize` call.
    ///
    /// @throws DshException when the server rejects the handshake
    private void handshake() throws DshException {
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "HDSL");
        clientInfo.addProperty("version", org.jackhuang.hmcl.Metadata.VERSION);

        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", PROTOCOL_VERSION);
        params.add("clientCapabilities", new JsonObject());
        params.add("clientInfo", clientInfo);

        JsonObject result = request("initialize", params);
        JsonElement version = result.get("protocolVersion");
        if (version != null && version.getAsInt() != PROTOCOL_VERSION) {
            throw new DshException("DeepSeek Harness speaks ACP v" + version.getAsInt()
                    + " but this launcher speaks v" + PROTOCOL_VERSION);
        }
    }

    /// Creates a session rooted at the instance's working directory.
    ///
    /// @return the new session id
    /// @throws DshException when the server rejects the request
    public String newSession() throws DshException {
        return newSession(instance.workspacePath());
    }

    /// Creates a session rooted at an explicit directory.
    ///
    /// @param cwd the directory the session operates on
    /// @return the new session id
    /// @throws DshException when the server rejects the request
    public String newSession(Path cwd) throws DshException {
        JsonObject params = new JsonObject();
        params.addProperty("cwd", cwd.toString());
        params.add("mcpServers", new JsonArray());

        JsonObject result = request("session/new", params);
        JsonElement sessionId = result.get("sessionId");
        if (sessionId == null || !sessionId.isJsonPrimitive()) {
            throw new DshException("DeepSeek Harness returned no session id");
        }
        return sessionId.getAsString();
    }

    /// Sends one text prompt.
    ///
    /// The call returns when the agent settles; assistant text arrives through
    /// the listener while it runs.
    ///
    /// @param sessionId the session to prompt
    /// @param text      the prompt text
    /// @throws DshException when the server rejects the request
    public void prompt(String sessionId, String text) throws DshException {
        JsonObject prompt = new JsonObject();
        prompt.addProperty("type", "text");
        prompt.addProperty("text", text);
        JsonArray blocks = new JsonArray();
        blocks.add(prompt);

        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        params.add("prompt", blocks);

        JsonObject result = request("session/prompt", params);
        JsonElement stopReason = result.get("stopReason");
        Listener current = listener;
        if (current != null) {
            current.onPromptFinished(stopReason == null ? "unknown" : stopReason.getAsString());
        }
    }

    /// Asks the agent to stop the prompt in flight.
    ///
    /// @param sessionId the session to cancel
    public void cancel(String sessionId) {
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        // session/cancel is a notification, not a request: no id, no response.
        sendNotification("session/cancel", params);
    }

    /// Returns the last few lines the server wrote to stderr.
    ///
    /// ACP reserves stdout for protocol frames, so a startup failure shows up
    /// here and nowhere else.
    ///
    /// @return the trailing stderr lines
    public List<String> stderrTail() {
        synchronized (stderrTail) {
            return List.copyOf(stderrTail);
        }
    }

    /// Reports whether the child is still running.
    ///
    /// @return whether the connection is alive
    public boolean isRunning() {
        return !closed && process.isRunning();
    }

    /// Detaches the listener.
    public void detachListener() {
        listener = null;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        for (CompletableFuture<JsonObject> future : pending.values()) {
            future.completeExceptionally(new DshException("The ACP connection was closed"));
        }
        pending.clear();

        // Closing stdin is the protocol's own shutdown request: the server binds
        // EOF to its bounded drain.
        try {
            writer.close();
        } catch (IOException e) {
            LOG.warning("Failed to close the ACP input", e);
        }

        Process raw = process.getProcess();
        try {
            if (!raw.waitFor(5, TimeUnit.SECONDS)) {
                raw.destroy();
                if (!raw.waitFor(5, TimeUnit.SECONDS)) {
                    raw.destroyForcibly();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            raw.destroyForcibly();
        }
        process.destroyRelatedThreads();
    }

    /// Sends a JSON-RPC request and waits for its response.
    ///
    /// @param method the method name
    /// @param params the parameters
    /// @return the result object
    /// @throws DshException when the server returns an error or never answers
    private JsonObject request(String method, JsonObject params) throws DshException {
        long id = nextId.getAndIncrement();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);

        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", params);
        send(message);

        try {
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pending.remove(id);
            throw new DshException("DeepSeek Harness did not answer `" + method + "` within "
                    + REQUEST_TIMEOUT_SECONDS + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("Interrupted while waiting for `" + method + "`", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw cause instanceof DshException dsh ? dsh : new DshException(String.valueOf(cause.getMessage()), cause);
        }
    }

    /// Sends a JSON-RPC notification.
    ///
    /// @param method the method name
    /// @param params the parameters
    private void sendNotification(String method, JsonObject params) {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("method", method);
        message.add("params", params);
        send(message);
    }

    /// Writes one frame.
    ///
    /// @param message the JSON-RPC message
    private synchronized void send(JsonObject message) {
        if (closed) {
            return;
        }
        try {
            writer.write(message.toString());
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            LOG.warning("Failed to write to the ACP connection", e);
        }
    }

    /// Dispatches one line of server output.
    ///
    /// @param line the raw line
    private void handleLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        JsonObject message;
        try {
            JsonElement parsed = JsonParser.parseString(trimmed);
            if (!parsed.isJsonObject()) {
                return;
            }
            message = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            // Stdout is reserved for protocol frames, but a misbehaving plugin
            // can still print; ignore rather than break the connection.
            LOG.info("[acp stdout] " + trimmed);
            return;
        }

        JsonElement idElement = message.get("id");
        if (idElement != null && idElement.isJsonPrimitive()) {
            CompletableFuture<JsonObject> future = pending.remove(idElement.getAsLong());
            if (future == null) {
                return;
            }
            JsonElement error = message.get("error");
            if (error != null && error.isJsonObject()) {
                JsonElement text = error.getAsJsonObject().get("message");
                future.completeExceptionally(new DshException(
                        text == null ? "The ACP request failed" : text.getAsString()));
                return;
            }
            JsonElement result = message.get("result");
            future.complete(result != null && result.isJsonObject() ? result.getAsJsonObject() : new JsonObject());
            return;
        }

        JsonElement method = message.get("method");
        if (method != null && "session/update".equals(method.getAsString())) {
            dispatchUpdate(message.getAsJsonObject("params"));
        }
    }

    /// Dispatches a `session/update` notification.
    ///
    /// @param params the notification parameters, or `null`
    private void dispatchUpdate(@Nullable JsonObject params) {
        Listener current = listener;
        if (current == null || params == null) {
            return;
        }
        JsonObject update = params.getAsJsonObject("update");
        if (update == null) {
            return;
        }
        // The raw update first, so a listener that needs every field of it is
        // not limited to the kinds the switch below recognises.
        current.onUpdate(update);
        JsonElement kindElement = update.get("sessionUpdate");
        if (kindElement == null) {
            return;
        }

        switch (kindElement.getAsString()) {
            case "agent_message_chunk" -> current.onText(textOf(update));
            case "agent_thought_chunk" -> current.onThought(textOf(update));
            case "tool_call", "tool_call_update" -> current.onToolCall(
                    stringOf(update, "title"),
                    stringOf(update, "status"));
            default -> {
                // Other update kinds are not rendered by the session panel yet.
            }
        }
    }

    /// Extracts the text of a content chunk.
    ///
    /// @param update the update object
    /// @return the text, or an empty string
    private static String textOf(JsonObject update) {
        JsonObject content = update.getAsJsonObject("content");
        return content == null ? "" : stringOf(content, "text");
    }

    /// Reads a string field, tolerating absence and null.
    ///
    /// @param object the object
    /// @param name   the field name
    /// @return the value, or an empty string
    private static String stringOf(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() || !element.isJsonPrimitive() ? "" : element.getAsString();
    }

    /// Waits for the child to exit and reports it.
    private void awaitExit() {
        try {
            int code = process.getProcess().waitFor();
            for (CompletableFuture<JsonObject> future : pending.values()) {
                future.completeExceptionally(new DshException("DeepSeek Harness exited with code " + code));
            }
            pending.clear();
            Listener current = listener;
            if (current != null && !closed) {
                current.onFailure("DeepSeek Harness exited with code " + code);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
