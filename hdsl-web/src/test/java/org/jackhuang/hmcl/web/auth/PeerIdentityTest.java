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
package org.jackhuang.hmcl.web.auth;

import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalSession;
import org.jackhuang.hmcl.web.terminal.TerminalTarget;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The kernel-derived half of the fatal-operation gate.
///
/// The roots come from a real terminal (a real PTY, a real pid); the `/proc`
/// walk is faked so the test names its own caller pid. What this pins is the
/// contract the gate leans on: a descendant of a root is that root's
/// instance, the root itself is, and a chain that reaches the panel or breaks
/// identifies nobody — never somebody else.
class PeerIdentityTest {

    private static final Map<String, String> ENV = Map.of("TERM", "dumb");

    /// `/proc` as the test wants it: a chosen caller pid, a chosen parent map,
    /// and pids whose membership the kernel would refuse (the reused-pid case).
    private static final class FakeProc implements PeerIdentity.Proc {
        @Nullable Long caller;
        final Map<Long, Long> parents = new HashMap<>();
        /// Pids that claim a root by chain but are not under it — the stale
        /// parent a reused pid produced.
        final java.util.Set<Long> notUnder = new java.util.HashSet<>();
        int callerCalls;

        @Override
        public @Nullable Long callerPid(int callerPort, int panelPort) {
            callerCalls++;
            return caller;
        }

        @Override
        public @Nullable Long parentPid(long pid) {
            return parents.get(pid);
        }

        @Override
        public boolean descendsFrom(long pid, long ancestor) {
            if (notUnder.contains(pid)) {
                return false;
            }
            long current = pid;
            for (int depth = 0; depth < 64 && current > 0; depth++) {
                if (current == ancestor) {
                    return true;
                }
                Long parent = parents.get(current);
                if (parent == null) {
                    return false;
                }
                current = parent;
            }
            return false;
        }
    }

    /// A terminal that stays alive long enough to be a root: `cat` blocks on
    /// its input until the session is terminated.
    private static TerminalSession openProbe(TerminalManager terminals, String brand, String id)
            throws Exception {
        TerminalTarget target = new TerminalTarget(brand, id,
                List.of("/bin/cat"), Path.of("/tmp"), ENV);
        return terminals.open(target, new PtySize(80, 24));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aDescendantOfARootIsThatRootsInstance() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            TerminalSession session = openProbe(terminals, "kimi", "probe");
            long root = session.pid();
            assertTrue(root > 0, "the terminal must name its root pid");

            FakeProc proc = new FakeProc();
            proc.caller = 999L;
            proc.parents.put(999L, root);
            PeerIdentity identity = new PeerIdentity(terminals, proc);

            assertEquals(new PeerIdentity.Ref("kimi", "probe"), identity.derive(40000, 3080));
            assertEquals("kimi/probe", new PeerIdentity.Ref("kimi", "probe").wire());
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void theRootProcessItselfIsIdentified() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            TerminalSession session = openProbe(terminals, "zcode", "probe");
            long root = session.pid();

            FakeProc proc = new FakeProc();
            proc.caller = root;
            PeerIdentity identity = new PeerIdentity(terminals, proc);

            assertEquals(new PeerIdentity.Ref("zcode", "probe"), identity.derive(40000, 3080));
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aChainThatReachesThePanelIdentifiesNobody() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            openProbe(terminals, "kimi", "probe");

            FakeProc proc = new FakeProc();
            proc.caller = 999L;
            // The caller's parent is the panel itself: not an instance, and
            // never a licence.
            proc.parents.put(999L, ProcessHandle.current().pid());
            PeerIdentity identity = new PeerIdentity(terminals, proc);

            assertNull(identity.derive(40000, 3080));
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aBrokenChainOrAMissingSocketIdentifiesNobody() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            openProbe(terminals, "kimi", "probe");

            FakeProc broken = new FakeProc();
            broken.caller = 999L;
            // No parent on record: the process is gone or unreadable.
            assertNull(new PeerIdentity(terminals, broken).derive(40000, 3080));

            FakeProc noCaller = new FakeProc();
            assertNull(new PeerIdentity(terminals, noCaller).derive(40000, 3080));
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aDerivedIdentityIsCachedPerPort() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            TerminalSession session = openProbe(terminals, "kimi", "probe");

            FakeProc proc = new FakeProc();
            proc.caller = session.pid();
            PeerIdentity identity = new PeerIdentity(terminals, proc);

            assertEquals(new PeerIdentity.Ref("kimi", "probe"), identity.derive(40000, 3080));
            assertEquals(new PeerIdentity.Ref("kimi", "probe"), identity.derive(40000, 3080));
            assertEquals(1, proc.callerCalls, "the second call on one port must reuse the answer");
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void terminalOpenReflectsTheSession() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            openProbe(terminals, "kimi", "probe");
            PeerIdentity identity = new PeerIdentity(terminals, new FakeProc());

            assertTrue(identity.terminalOpen("kimi", "probe"));
            assertFalse(identity.terminalOpen("kimi", "other"));
            assertFalse(identity.terminalOpen("dsh", "probe"));
        }
    }

    /// The parsing half of [PeerIdentity.LinuxProc], against synthetic
    /// `/proc/net/tcp` lines: the caller's side matches, the panel's own side
    /// of the same connection does not, and a port is the little-endian hex
    /// after the last colon.
    @Test
    void linuxProcParsingFindsTheCallerSideOnly() {
        String callerSide = "  10: 0100007F:9C40 0100007F:0C08 01 00000000:00000000 02:0007 00000000 0 0 176883 2 0000000000000000 100 0 0 10 0";
        // The panel's end of the same connection: local 3080, remote 40000.
        String panelSide = "  11: 0100007F:0C08 0100007F:9C40 01 00000000:00000000 02:0007 00000000 0 0 199999 2 0000000000000000 100 0 0 10 0";
        // Same ports, not established: a TIME_WAIT leftover must not answer.
        String timeWait = "  12: 0100007F:9C40 0100007F:0C08 06 00000000:00000000 00:0000 00000000 0 0 111111 2 0000000000000000 100 0 0 10 0";

        assertEquals(176883L, PeerIdentity.LinuxProc.scan(List.of(panelSide, timeWait, callerSide), 40000, 3080));
        // The panel's own end of the connection never answers a lookup for the
        // caller's: its local port is the panel's, not the caller's.
        assertNull(PeerIdentity.LinuxProc.scan(List.of(panelSide), 40000, 3080));
        assertNull(PeerIdentity.LinuxProc.scan(List.of(panelSide, timeWait), 40000, 3080));

        assertEquals(0x9C40, PeerIdentity.LinuxProc.portOf("0100007F:9C40"));
        assertEquals(0x0C08, PeerIdentity.LinuxProc.portOf("0100007F:0C08"));
        assertEquals(-1, PeerIdentity.LinuxProc.portOf("0100007F"));
        assertEquals(-1, PeerIdentity.LinuxProc.portOf("0100007F:"));
        assertEquals(-1, PeerIdentity.LinuxProc.portOf("0100007F:GGGG"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aRootClaimedThroughAReusedPidIsRejectedByMembership() throws Exception {
        try (TerminalManager terminals = new TerminalManager()) {
            TerminalSession session = openProbe(terminals, "kimi", "probe");
            long root = session.pid();

            FakeProc proc = new FakeProc();
            // The chain points at the root — through a parent that died and
            // whose pid the root then reused. The chain says yes; membership
            // says the caller is not under that root, and membership wins.
            proc.caller = 998L;
            proc.parents.put(998L, root);
            proc.notUnder.add(998L);

            assertNull(new PeerIdentity(terminals, proc).derive(40000, 3080));
        }
    }

    /// The Windows row scan, against synthetic `GetExtendedTcpTable` rows.
    ///
    /// An IPv4 row is six dwords (state, local address, local port, remote
    /// address, remote port, pid) with the ports in network byte order inside
    /// the dwords; an IPv6 row is fourteen, with the local port seventh, the
    /// remote port twelfth and the pid fourteenth. The caller's row answers;
    /// the panel's own mirror row and a non-established row do not.
    @Test
    void windowsRowsNameTheirOwner() {
        int callerPort = 0x409C;  // network order for 40000
        int panelPort = 0x080C;   // network order for 3080

        int[] caller = {5, 0x0100007F, callerPort, 0x0100007F, panelPort, 4321};
        int[] panelSide = {5, 0x0100007F, panelPort, 0x0100007F, callerPort, 7};
        int[] timeWait = {6, 0x0100007F, callerPort, 0x0100007F, panelPort, 9999};

        int[] v4 = new int[caller.length + panelSide.length + timeWait.length];
        System.arraycopy(caller, 0, v4, 0, caller.length);
        System.arraycopy(panelSide, 0, v4, caller.length, panelSide.length);
        System.arraycopy(timeWait, 0, v4, caller.length + panelSide.length, timeWait.length);
        assertEquals(4321L, PeerIdentity.WindowsProc.scan(v4, 6, 2, 4, 5, 40000, 3080));
        // The panel's own mirror row is not the caller's: alone, it answers
        // nothing in the caller's direction.
        assertNull(PeerIdentity.WindowsProc.scan(panelSide, 6, 2, 4, 5, 40000, 3080));

        int[] v6Caller = new int[14];
        v6Caller[0] = 5;
        v6Caller[6] = callerPort;
        v6Caller[11] = panelPort;
        v6Caller[13] = 8765;
        assertEquals(8765L, PeerIdentity.WindowsProc.scan(v6Caller, 14, 6, 11, 13, 40000, 3080));
        assertNull(PeerIdentity.WindowsProc.scan(v6Caller, 14, 6, 11, 13, 3080, 40000));

        // A non-established row never answers, whatever its ports.
        int[] closed = {12, 0x0100007F, callerPort, 0x0100007F, panelPort, 1111};
        assertNull(PeerIdentity.WindowsProc.scan(closed, 6, 2, 4, 5, 40000, 3080));

        assertEquals(0x9C40, PeerIdentity.WindowsProc.networkPort(0x409C));
        assertEquals(0x0C08, PeerIdentity.WindowsProc.networkPort(0x080C));
        assertEquals(3080, PeerIdentity.WindowsProc.networkPort(0x080C));
    }
}
