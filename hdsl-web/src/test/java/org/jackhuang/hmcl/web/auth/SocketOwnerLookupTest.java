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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The uid-split half of the socket-owner lookup.
///
/// The panel is root and the agents are uid 1000, so `/proc/<agent>/fd` answers
/// `EACCES` to the panel itself; the scan has to run as the agent. These tests
/// pin the two halves that can be checked without root:
///
/// - the scan finds the pid holding a live socket when the reader is allowed to
///   look (the bare-metal case, and what the probe child does as uid 1000);
/// - the probe answers only what the child printed, and `null` for every way a
///   child can fail to answer.
class SocketOwnerLookupTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void scanFindsTheProcessHoldingALiveSocket() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket server = new ServerSocket(0, 1, loopback);
             Socket client = new Socket(loopback, server.getLocalPort());
             Socket accepted = server.accept()) {
            long inode = inodeOf(client.getLocalPort(), server.getLocalPort());
            assertEquals(ProcessHandle.current().pid(), SocketOwnerLookup.scan(inode));
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void probeReturnsThePidTheChildPrinted() {
        assertEquals(4242L, SocketOwnerLookup.probe(List.of("/bin/sh", "-c", "echo 4242")));
    }

    /// The branch a bare-metal deployment takes: no uid split, so the panel
    /// already is the agent's uid and scans its own `/proc` view directly. This
    /// is the same scan the code did before the probe child existed — the child
    /// must not be on this path at all.
    @Test
    @EnabledOnOs(OS.LINUX)
    void ownerScansInProcessWhenThereIsNoUidSplit() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket server = new ServerSocket(0, 1, loopback);
             Socket client = new Socket(loopback, server.getLocalPort());
             Socket accepted = server.accept()) {
            long inode = inodeOf(client.getLocalPort(), server.getLocalPort());
            assertEquals(SocketOwnerLookup.scan(inode), SocketOwnerLookup.owner(inode, false));
            assertEquals(ProcessHandle.current().pid(), SocketOwnerLookup.owner(inode, false));
        }
    }

    /// The whole child path: the parent builds the argv, starts a JVM with this
    /// panel's own classpath, and believes only the pid the child printed.
    ///
    /// The test is not root, so `Privileges` adds no `setpriv` prefix here — what
    /// this pins is the wiring the container relies on, not the uid change
    /// itself (which only a root test could exercise).
    @Test
    @EnabledOnOs(OS.LINUX)
    void ownerAsksTheChildWhenTheUidSplitApplies() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket server = new ServerSocket(0, 1, loopback);
             Socket client = new Socket(loopback, server.getLocalPort());
             Socket accepted = server.accept()) {
            long inode = inodeOf(client.getLocalPort(), server.getLocalPort());
            assertEquals(ProcessHandle.current().pid(), SocketOwnerLookup.owner(inode, true));
        }
    }

    /// Every failure the child can produce is "unidentified", never a guess:
    /// an explicit `-`, a non-zero exit, unparsable output, or no child at all.
    @Test
    @EnabledOnOs(OS.LINUX)
    void probeIsFailClosed() {
        assertNull(SocketOwnerLookup.probe(List.of("/bin/sh", "-c", "echo -")));
        assertNull(SocketOwnerLookup.probe(List.of("/bin/sh", "-c", "exit 3")));
        assertNull(SocketOwnerLookup.probe(List.of("/bin/sh", "-c", "echo not-a-pid")));
        assertNull(SocketOwnerLookup.probe(List.of("/definitely/not/a/binary")));
    }

    /// The caller-side row in `/proc/net/tcp{,6}`: established, its local port
    /// the caller's, its remote port the panel's.
    private static long inodeOf(int callerPort, int panelPort) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            for (String file : List.of("/proc/net/tcp", "/proc/net/tcp6")) {
                Path path = Path.of(file);
                if (!Files.exists(path)) {
                    continue;
                }
                for (String line : Files.readAllLines(path)) {
                    String[] fields = line.trim().split("\\s+");
                    if (fields.length < 10
                            || !fields[1].contains(":") || !fields[2].contains(":")
                            || !"01".equals(fields[3])) {
                        continue;
                    }
                    if (portOf(fields[1]) == callerPort && portOf(fields[2]) == panelPort) {
                        return Long.parseLong(fields[9]);
                    }
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("no established socket " + callerPort + " -> " + panelPort);
    }

    private static int portOf(String address) {
        return Integer.parseInt(address.substring(address.lastIndexOf(':') + 1), 16);
    }
}
