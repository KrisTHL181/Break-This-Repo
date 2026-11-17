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

import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// Live terminal sockets by the panel session that opened them.
///
/// A ticket is one-use and bound to the session that asked for it, but the
/// WebSocket it opens is **not** re-checked afterwards: the session may be
/// logged out (or simply expire) while its terminal keeps streaming. Without
/// this registry a logged-out browser would keep a live, interactive channel
/// into an instance. `logout` therefore closes every socket a session opened —
/// the close is a **detach**: the agent and its PTY keep running and the next
/// elevation can attach again (docs/web-shell.md §3).
@NotNullByDefault
public final class TerminalSockets {

    /// One live socket. The endpoint implements it by closing its WebSocket
    /// with 1008 `logged out`.
    public interface Socket {
        void closeForLogout();
    }

    private final ConcurrentHashMap<String, Set<Socket>> bySession = new ConcurrentHashMap<>();

    /// Tracks `socket` under `sessionToken`. Called once per successful attach.
    public void register(String sessionToken, Socket socket) {
        bySession.computeIfAbsent(sessionToken, key -> ConcurrentHashMap.newKeySet()).add(socket);
    }

    /// Forgets `socket`. Called from every disconnect path; removal before
    /// close is fine because [closeForSession] snapshots before closing.
    public void unregister(String sessionToken, Socket socket) {
        Set<Socket> sockets = bySession.get(sessionToken);
        if (sockets == null) {
            return;
        }
        sockets.remove(socket);
        if (sockets.isEmpty()) {
            bySession.remove(sessionToken, sockets);
        }
    }

    /// Closes every socket `sessionToken` opened, answering how many. A
    /// socket that fails to close (already gone, transport hiccup) is
    /// unregistered anyway so the set cannot grow stale.
    public int closeForSession(String sessionToken) {
        Set<Socket> sockets = bySession.remove(sessionToken);
        if (sockets == null) {
            return 0;
        }
        int closed = 0;
        for (Socket socket : List.copyOf(sockets)) {
            try {
                socket.closeForLogout();
                closed++;
            } catch (RuntimeException ignored) {
                // The socket is dying on its own; detach has no caller to
                // notify, so nothing here needs to be loud about it.
            }
        }
        return closed;
    }
}
