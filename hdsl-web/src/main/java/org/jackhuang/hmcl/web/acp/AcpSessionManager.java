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
import org.jackhuang.hmcl.dsh.DshAcpClient;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The bridge between the panel's `/ws` protocol and the Agent Client
/// Protocol: one live ACP conversation per instance, exposed as the
/// `acp-start` / `acp-prompt` / `acp-cancel` / `acp-stop` messages.
///
/// The ACP process is deliberately **not** the instance's web process. It is a
/// second `node … --profile acp` child with its own lifetime: stopping or
/// reinstalling the web process does not touch a console somebody is talking
/// to, and only `acp-stop` (or the server going down) ends it. A browser tab
/// that disconnects does not end it either — the panel sends `acp-start` again
/// after a reconnect and is handed the session that is already live.
///
/// Every event is published on the `instance:<id>` [EventBus] topic, which is
/// the topic the instance page's WebSocket connection already subscribes to.
/// The heavy work (resolving the runtime, spawning the child, `initialize`,
/// `session/new`, a prompt) runs on the manager's own virtual threads, so a
/// WebSocket reader never blocks on it.
@NotNullByDefault
public final class AcpSessionManager {

    /// Opens a client for one instance. The seam exists so tests can drive the
    /// bridge against a stub peer without an installed harness.
    @FunctionalInterface
    public interface Connector {
        /// Starts a client.
        ///
        /// @param instance         the instance
        /// @param workingDirectory the directory the session is scoped to
        /// @param listener         the listener for the session's events
        /// @return the connected client
        /// @throws DshException when the child cannot start or the handshake fails
        DshAcpClient connect(DshInstance instance, Path workingDirectory, DshAcpClient.Listener listener)
                throws DshException;

        /// Gives back whatever [connect] put in the instance's home for this console.
        ///
        /// Called once per console that is over — stopped, failed to come up, or found dead — so a
        /// connector that wrote something for the console can take it away again. The default does
        /// nothing: a connector that put nothing there has nothing to give back.
        ///
        /// @param instance the instance the console belonged to
        default void release(DshInstance instance) {
        }
    }

    /// The production connector: the instance's own harness, booted in ACP mode,
    /// with the panel's account for that instance handed to it.
    public static final Connector DEFAULT_CONNECTOR = new AcpAccountBridge();

    private final EventBus bus;
    private final Connector connector;
    private final ExecutorService workers = VirtualThreads.newPerTaskExecutor();
    /// Instance id → the one console that may be live for it.
    private final ConcurrentHashMap<String, Console> consoles = new ConcurrentHashMap<>();

    /// Creates the manager with the production connector.
    ///
    /// @param bus the bus the `acp-*` events are published on
    public AcpSessionManager(EventBus bus) {
        this(bus, DEFAULT_CONNECTOR);
    }

    /// Creates the manager with an explicit connector.
    ///
    /// @param bus       the bus the `acp-*` events are published on
    /// @param connector the connector that opens the ACP client
    public AcpSessionManager(EventBus bus, Connector connector) {
        this.bus = bus;
        this.connector = connector;
    }

    /// Starts, or reuses, an instance's console.
    ///
    /// A console that is already ready is reused: the caller receives another
    /// `acp-ready` with the live session id, and no second process is spawned.
    /// A console still starting is refused — the caller is told to wait rather
    /// than given a second, competing handshake.
    ///
    /// @param instanceId the instance id
    /// @param cwd        a working directory of the caller's choosing, or `null`
    ///                   for the instance's own workspace
    public void start(String instanceId, @Nullable String cwd) {
        if (instanceId.isBlank()) {
            LOG.warning("[acp] acp-start without an instance");
            return;
        }
        Console console = new Console(instanceId);
        Console existing = consoles.putIfAbsent(instanceId, console);
        if (existing != null) {
            DshAcpClient client = existing.client;
            String session = existing.sessionId;
            if (session != null && client != null && client.isRunning()) {
                publishReady(instanceId, session);
            } else {
                publishError(instanceId, "An ACP console is already starting for instance " + instanceId);
            }
            return;
        }
        workers.submit(() -> open(console, cwd));
    }

    /// Sends one prompt, if nothing else is in flight for the session.
    ///
    /// @param instanceId the instance id
    /// @param sessionId  the session the prompt belongs to
    /// @param text       the prompt text
    public void prompt(String instanceId, @Nullable String sessionId, @Nullable String text) {
        Console console = consoles.get(instanceId);
        if (console == null || console.sessionId == null) {
            publishError(instanceId, "The ACP console is not ready for instance " + instanceId);
            return;
        }
        if (sessionId == null || !sessionId.equals(console.sessionId)) {
            publishError(instanceId, "Unknown ACP session " + sessionId);
            return;
        }
        if (text == null || text.isBlank()) {
            publishError(instanceId, "An ACP prompt must carry text");
            return;
        }
        if (!console.prompting.compareAndSet(false, true)) {
            publishError(instanceId, "A prompt is already in flight for this session");
            return;
        }
        String session = console.sessionId;
        workers.submit(() -> {
            try {
                DshAcpClient client = console.client;
                if (client == null || !client.isRunning()) {
                    throw new DshException("ACP process exited");
                }
                client.prompt(session, text);
            } catch (DshException | RuntimeException e) {
                // A prompt that failed because the console was stopped is the
                // expected end of a stop, not an error to report.
                if (!console.stopping && consoles.get(instanceId) == console) {
                    publishError(instanceId, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            } finally {
                console.prompting.set(false);
            }
        });
    }

    /// Asks the agent to stop the prompt in flight.
    ///
    /// @param instanceId the instance id
    /// @param sessionId  the session to cancel
    public void cancel(String instanceId, @Nullable String sessionId) {
        Console console = consoles.get(instanceId);
        if (console == null || console.sessionId == null
                || sessionId == null || !sessionId.equals(console.sessionId)) {
            publishError(instanceId, "Unknown ACP session " + sessionId);
            return;
        }
        DshAcpClient client = console.client;
        if (client == null) {
            return;
        }
        workers.submit(() -> client.cancel(sessionId));
    }

    /// Ends an instance's console.
    ///
    /// The process goes away through the protocol's own shutdown: closing stdin
    /// is the bounded drain the harness binds EOF to, and the client force-kills
    /// the child if it does not oblige. Afterwards the connector is asked to give
    /// back whatever it wrote for this console — for the account bridge, the
    /// supplier route in the console's own profile.
    ///
    /// @param instanceId the instance id
    public void stop(String instanceId) {
        Console console = consoles.remove(instanceId);
        if (console == null) {
            return;
        }
        console.stopping = true;
        workers.submit(() -> {
            DshAcpClient client = console.client;
            if (client != null) {
                client.close();
            }
            release(console);
        });
    }

    /// Stops every console synchronously. Called on server shutdown, where
    /// waiting for each child to drain is preferable to leaving it orphaned.
    public void closeAll() {
        for (Console console : List.copyOf(consoles.values())) {
            if (consoles.remove(console.instanceId, console)) {
                console.stopping = true;
                DshAcpClient client = console.client;
                if (client != null) {
                    client.close();
                }
                release(console);
            }
        }
    }

    /// Gives back what the connector wrote into an instance's home for a console
    /// that is over.
    ///
    /// A console that never resolved its instance wrote nothing to give back, and
    /// the connector's own release is idempotent for the paths that reach it
    /// twice.
    private void release(Console console) {
        DshInstance instance = console.instance;
        if (instance == null) {
            return;
        }
        try {
            connector.release(instance);
        } catch (RuntimeException e) {
            // Tidying up after a console must not be able to take the panel down.
            LOG.warning("[acp] could not release the console of " + instance.id(), e);
        }
    }

    /// Opens one console: resolves the instance, validates the entry script,
    /// connects, and creates the ACP session.
    private void open(Console console, @Nullable String cwd) {
        DshAcpClient client = null;
        try {
            DshInstance instance = DshInstanceManager.find(console.instanceId);
            if (instance == null) {
                fail(console, "Instance " + console.instanceId + " does not exist");
                return;
            }
            console.instance = instance;
            Path entry = instance.dshEntryPoint();
            if (!Files.isRegularFile(entry)) {
                fail(console, "Instance " + console.instanceId
                        + " has no DeepSeek Harness of its own; " + entry + " is missing");
                return;
            }
            Path workingDirectory = resolveWorkingDirectory(instance, cwd);
            if (!Files.isDirectory(workingDirectory)) {
                fail(console, "The ACP working directory " + workingDirectory + " does not exist");
                return;
            }
            client = connector.connect(instance, workingDirectory, console.listener);
            console.client = client;
            if (console.stopping) {
                // acp-stop raced the handshake; the console is already gone.
                client.close();
                return;
            }
            String session = client.newSession(workingDirectory);
            console.sessionId = session;
            if (console.stopping || consoles.get(console.instanceId) != console) {
                client.close();
                return;
            }
            publishReady(console.instanceId, session);
        } catch (DshException | RuntimeException e) {
            if (client != null) {
                client.close();
            }
            fail(console, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /// Removes a console that failed to come up and tells the caller why.
    private void fail(Console console, String message) {
        consoles.remove(console.instanceId, console);
        release(console);
        if (!console.stopping) {
            publishError(console.instanceId, message);
        }
    }

    /// The directory a console's session operates on: the caller's, or the
    /// instance's own workspace.
    private static Path resolveWorkingDirectory(DshInstance instance, @Nullable String cwd) throws DshException {
        if (cwd == null || cwd.isBlank()) {
            return instance.workspacePath();
        }
        try {
            return Path.of(cwd.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new DshException("The ACP working directory is not a usable path: " + cwd, e);
        }
    }

    private void publishReady(String instanceId, String session) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "acp-ready");
        payload.addProperty("instance", instanceId);
        payload.addProperty("session", session);
        payload.addProperty("protocolVersion", DshAcpClient.PROTOCOL_VERSION);
        bus.publish(topic(instanceId), payload);
    }

    private void publishError(String instanceId, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "acp-error");
        payload.addProperty("instance", instanceId);
        payload.addProperty("error", message);
        bus.publish(topic(instanceId), payload);
    }

    private static String topic(String instanceId) {
        return "instance:" + instanceId;
    }

    /// One instance's console: its process, session, and the one-prompt-at-a-time
    /// latch.
    private final class Console {
        private final String instanceId;
        private final DshAcpClient.Listener listener;
        private volatile @Nullable DshAcpClient client;
        private volatile @Nullable String sessionId;
        /// The instance this console resolved to, kept so the connector can be asked
        /// to give back what it wrote for it once the console is over.
        private volatile @Nullable DshInstance instance;
        /// Set by [#stop]/[#closeAll] before the process is closed, so the
        /// listener's failure report knows the ending was asked for.
        private volatile boolean stopping;
        private final AtomicBoolean prompting = new AtomicBoolean();

        private Console(String instanceId) {
            this.instanceId = instanceId;
            this.listener = new ConsoleListener(this);
        }
    }

    /// Translates one client's protocol events into the WebSocket payloads.
    private final class ConsoleListener implements DshAcpClient.Listener {
        private final Console console;

        private ConsoleListener(Console console) {
            this.console = console;
        }

        @Override
        public void onUpdate(JsonObject update) {
            if (console.stopping) {
                return;
            }
            String session = console.sessionId;
            if (session == null) {
                return;
            }
            // The panel reads `kind`; the protocol spells it `sessionUpdate`.
            // The rest of the object is forwarded untouched — a tool call's id,
            // raw input and output all travel with it.
            JsonElement kind = update.get("sessionUpdate");
            if (kind != null && kind.isJsonPrimitive() && !update.has("kind")) {
                update.addProperty("kind", kind.getAsString());
            }
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "acp-update");
            payload.addProperty("instance", console.instanceId);
            payload.addProperty("session", session);
            payload.add("update", update);
            bus.publish(topic(console.instanceId), payload);
        }

        @Override
        public void onPromptFinished(String stopReason) {
            if (console.stopping) {
                return;
            }
            console.prompting.set(false);
            String session = console.sessionId;
            if (session == null) {
                return;
            }
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "acp-finished");
            payload.addProperty("instance", console.instanceId);
            payload.addProperty("session", session);
            payload.addProperty("stopReason", stopReason);
            bus.publish(topic(console.instanceId), payload);
        }

        @Override
        public void onFailure(String message) {
            if (console.stopping) {
                return;
            }
            boolean wasReady = console.sessionId != null;
            consoles.remove(console.instanceId, console);
            // The process is gone, so whatever it was handed goes back with it.
            release(console);
            if (!wasReady) {
                // The startup path reports the failure itself.
                return;
            }
            LOG.warning("[acp] " + console.instanceId + ": " + message);
            publishError(console.instanceId, "ACP process exited");
        }
    }
}
