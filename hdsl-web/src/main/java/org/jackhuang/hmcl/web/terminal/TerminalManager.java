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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.terminal.pty.Pty;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
/// The registry of running terminals: one per instance, at most.
///
/// The manager owns the lifecycle the web layer cannot be trusted with:
/// opening is refused while the instance already has a running terminal,
/// terminating goes through [TerminalSession#terminate] (process group), and
/// a session that exits on its own removes itself. Closing the manager ends
/// every terminal — the panel is the parent process, not a bystander.
@NotNullByDefault
public final class TerminalManager implements AutoCloseable {

    /// How much output a reattaching viewer gets back.
    public static final int DEFAULT_REPLAY_BYTES = 256 * 1024;

    private final Map<String, TerminalSession> sessions = new ConcurrentHashMap<>();
    private final int replayBytes;

    public TerminalManager() {
        this(DEFAULT_REPLAY_BYTES);
    }

    public TerminalManager(int replayBytes) {
        if (replayBytes < 0) {
            throw new IllegalArgumentException("negative replay size");
        }
        this.replayBytes = replayBytes;
    }

    /// The registry key of one instance's terminal. The brand is part of it
    /// because dsh, the brand categories and ZCode each have their own id
    /// space: two surfaces may legitimately name an instance the same way, and
    /// they must not share a PTY.
    public static String key(String brand, String instanceId) {
        return brand + "\u0000" + instanceId;
    }

    /// Starts a terminal for `target`.
    ///
    /// @throws TerminalBusyException when the instance already has one running
    /// @throws IOException when the PTY or the agent cannot be started
    public synchronized TerminalSession open(TerminalTarget target, PtySize size) throws IOException {
        String key = key(target.brand(), target.instanceId());
        TerminalSession existing = sessions.get(key);
        if (existing != null) {
            if (existing.state() == TerminalSession.State.RUNNING) {
                throw new TerminalBusyException(target.instanceId());
            }
            sessions.remove(key, existing);
        }
        Pty pty = Pty.spawn(target.command(), target.cwd(), target.environment(), size);
        TerminalSession session = new TerminalSession(target.instanceId(), pty, replayBytes);
        sessions.put(key, session);
        AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.open", target.instanceId(), "success", null, null);
        session.onExit(() -> {
            sessions.remove(key, session);
            AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.exit", target.instanceId(),
                    String.valueOf(session.exitCode()), null, null);
        });
        return session;
    }

    /// The running terminal of an instance, if any.
    public Optional<TerminalSession> find(String brand, String instanceId) {
        return Optional.ofNullable(sessions.get(key(brand, instanceId)));
    }

    /// One live terminal as the panel's caller identification sees it
    /// ([org.jackhuang.hmcl.web.auth.PeerIdentity]).
    ///
    /// @param brand      the registry key's brand half: `dsh`, a brand id, or `zcode`
    /// @param instanceId the instance the terminal belongs to
    /// @param pid        the terminal's root process
    public record LiveTerminal(String brand, String instanceId, long pid) {
    }

    /// Every terminal this manager is running, for caller identification.
    ///
    /// Each root pid names exactly one instance, because the panel — not a
    /// shell — spawned it: the trees the fatal-operation gate walks are these
    /// keys and nothing else.
    ///
    /// @return the live terminals, never `null`
    public List<LiveTerminal> liveTerminals() {
        List<LiveTerminal> live = new ArrayList<>();
        for (Map.Entry<String, TerminalSession> entry : sessions.entrySet()) {
            String key = entry.getKey();
            int split = key.indexOf('\u0000');
            if (split <= 0 || split == key.length() - 1) {
                continue;
            }
            long pid = entry.getValue().pid();
            if (pid > 0) {
                live.add(new LiveTerminal(key.substring(0, split), key.substring(split + 1), pid));
            }
        }
        return live;
    }

    /// Kills the instance's terminal, if one is running.
    ///
    /// @return whether there was a session to kill
    public boolean terminate(String brand, String instanceId) {
        TerminalSession session = sessions.remove(key(brand, instanceId));
        if (session == null) {
            return false;
        }
        session.terminate(java.time.Duration.ofSeconds(2));
        AuditLog.log(AuditLog.Channel.TERMINAL, "terminal.kill", instanceId, "success", null, null);
        return true;
    }

    /// Kills every tracked terminal, for the emergency stop.
    ///
    /// Same contract as [terminate] per session (the PTY's process group gets
    /// SIGKILL after a two-second grace, or the job object is killed on
    /// Windows), and the same audit line, so a stop leaves the same trail as
    /// closing terminals one by one.
    ///
    /// @return how many sessions were terminated
    public int terminateAll() {
        int ended = 0;
        for (String key : java.util.List.copyOf(sessions.keySet())) {
            TerminalSession session = sessions.remove(key);
            if (session == null) {
                continue;
            }
            try {
                session.terminate(java.time.Duration.ofSeconds(2));
                ended++;
            } catch (RuntimeException ignored) {
                // A session that is already gone must not keep the rest alive.
            }
        }
        return ended;
    }

    /// How many terminals are tracked, and which instance each belongs to, for
    /// the emergency stop's preview.
    ///
    /// @return a snapshot, in no particular order
    public java.util.List<Tracked> tracked() {
        java.util.List<Tracked> rows = new java.util.ArrayList<>();
        for (Map.Entry<String, TerminalSession> entry : sessions.entrySet()) {
            int separator = entry.getKey().indexOf('\u0000');
            String brand = separator < 0 ? "" : entry.getKey().substring(0, separator);
            String instanceId = separator < 0 ? entry.getKey() : entry.getKey().substring(separator + 1);
            TerminalSession session = entry.getValue();
            rows.add(new Tracked(brand, instanceId,
                    session.state() == TerminalSession.State.RUNNING, session.exitCode()));
        }
        return rows;
    }

    /// One tracked terminal, as the E-Stop preview lists it.
    ///
    /// @param brand      the surface that owns the terminal (`dsh`, a brand id, `zcode`)
    /// @param instanceId the instance id
    /// @param running    whether the session is still alive
    /// @param exitCode   the child's exit code once it ended, `0` while running
    public record Tracked(String brand, String instanceId, boolean running, int exitCode) {
    }

    /// How many terminals are tracked (running or just exited).
    public int size() {
        return sessions.size();
    }

    /// Ends every terminal. Called when the panel stops.
    @Override
    public void close() {
        for (TerminalSession session : sessions.values()) {
            try {
                session.close();
            } catch (RuntimeException ignored) {
                // A terminal that is already gone must not keep the others
                // alive during shutdown.
            }
        }
        sessions.clear();
    }
}
