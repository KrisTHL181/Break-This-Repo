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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The logout sweep over live terminal sockets: only the logging-out session's
/// sockets close, unregister-race is tolerated, and one dying socket does not
/// keep the others attached.
class TerminalSocketsTest {

    @Test
    void closingASessionSeversOnlyItsOwnSockets() {
        TerminalSockets sockets = new TerminalSockets();
        AtomicInteger aClosed = new AtomicInteger();
        AtomicInteger bClosed = new AtomicInteger();
        TerminalSockets.Socket a = aClosed::incrementAndGet;
        TerminalSockets.Socket b = bClosed::incrementAndGet;

        sockets.register("token:a", a);
        sockets.register("token:a", a); // 同一连接重复注册：集合去重，不会重复掐
        sockets.register("token:b", b);

        assertEquals(1, sockets.closeForSession("token:a"));
        assertEquals(1, aClosed.get());
        assertEquals(0, bClosed.get());
        // Idempotent: a second sweep has nothing left.
        assertEquals(0, sockets.closeForSession("token:a"));
        // Other sessions are untouched.
        assertEquals(1, sockets.closeForSession("token:b"));
        assertEquals(1, bClosed.get());
    }

    @Test
    void unregisterBeforeTheSweepKeepsTheRemainingSockets() {
        TerminalSockets sockets = new TerminalSockets();
        List<String> closed = new ArrayList<>();
        TerminalSockets.Socket gone = () -> closed.add("gone");
        TerminalSockets.Socket live = () -> closed.add("live");

        sockets.register("token:a", gone);
        sockets.register("token:a", live);
        // A normal disconnect unregisters first — the sweep must skip it.
        sockets.unregister("token:a", gone);

        assertEquals(1, sockets.closeForSession("token:a"));
        assertEquals(List.of("live"), closed);
    }

    @Test
    void unregisteringTheLastSocketDropsTheWholeSet() {
        TerminalSockets sockets = new TerminalSockets();
        TerminalSockets.Socket socket = () -> {};
        sockets.register("token:a", socket);
        sockets.unregister("token:a", socket);
        // Nothing registered: no sweep, no stale empty set.
        assertEquals(0, sockets.closeForSession("token:a"));
    }

    @Test
    void aSocketThatDiesMidSweepDoesNotBlockTheOthers() {
        TerminalSockets sockets = new TerminalSockets();
        AtomicInteger closed = new AtomicInteger();
        TerminalSockets.Socket broken = () -> {
            throw new RuntimeException("transport gone mid-close");
        };
        TerminalSockets.Socket fine = closed::incrementAndGet;
        sockets.register("token:a", broken);
        sockets.register("token:a", fine);

        assertEquals(1, sockets.closeForSession("token:a"));
        assertEquals(1, closed.get());
    }
}
