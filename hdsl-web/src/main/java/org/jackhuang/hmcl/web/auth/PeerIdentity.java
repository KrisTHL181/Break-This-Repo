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

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.brand.BrandRuntime;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalSession;
import org.jackhuang.hmcl.web.zcode.ZcodeRuntime;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Which instance a caller runs in, derived from the kernel rather than asked.
///
/// An agent grant used to carry a self-reported `instanceId`, and a caller that
/// got it wrong — or omitted it — could stop the very instance it was running
/// in. This class answers the same question from facts the caller cannot
/// rewrite: every instance's root process is a direct child of this panel, so
/// the process trees are disjoint, and the socket that carried a request names
/// exactly one of them.
///
/// The walk, all of it from the kernel, on both platforms:
///
/// 1. `request.getRemotePort()` is the caller-side socket's local port. Linux
///    finds that socket's inode in `/proc/net/tcp` and its owner in
///    `/proc/<pid>/fd`; Windows asks `GetExtendedTcpTable` for the row and
///    reads the owning pid from it directly — one step where Linux needs two.
/// 2. Walking parent pids upward (from `/proc/<pid>/stat` on Linux, from the
///    JDK's process snapshot on Windows), the first process that is a known
///    instance root names the caller's instance.
/// 3. **Membership confirms the hit.** A parent chain is snapshot semantics —
///    a dead ancestor's pid can be reused and point at a root by accident —
///    so the answer is only believed when the kernel also says the caller
///    lives under that root ([Proc#descendsFrom]). A reused pid fails the
///    check and reads as unidentified.
///
/// Reaching this panel's own pid, or losing the chain, means the caller is not
/// inside any instance: a host script, a detached relay, or a caller that
/// deliberately left its tree — and the answer is `null`, which the
/// fatal-operation gate reads as "unidentified", never as "privileged".
@NotNullByDefault
public final class PeerIdentity {

    /// One instance, as the fatal-operation gate compares it.
    ///
    /// `family` is the id-space owner — `dsh`, a brand id, or `zcode` — and
    /// matches the brand part of the terminal registry key.
    ///
    /// @param family      the instance's family
    /// @param instanceId  the instance's id
    public record Ref(String family, String instanceId) {

        /// The binding string a grant carries: `family/id`.
        ///
        /// @return the wire form
        public String wire() {
            return family + "/" + instanceId;
        }
    }

    /// The kernel surface this class reads, so the walk itself stays testable.
    public interface Proc {

        /// The pid of the process that owns the caller-side socket, or `null`
        /// when there is none.
        ///
        /// @param callerPort the caller's local port (the request's remote port)
        /// @param panelPort  the port the panel served the request on
        /// @return the owning pid, or `null`
        @Nullable Long callerPid(int callerPort, int panelPort);

        /// The parent of `pid`, or `null` when it cannot be read.
        ///
        /// @param pid the process
        /// @return its parent's pid, or `null`
        @Nullable Long parentPid(long pid);

        /// Whether `pid` lives in the process tree rooted at `ancestor` — the
        /// live fact that confirms a chain's answer, and rejects a pid that a
        /// dead ancestor's reuse pointed at.
        ///
        /// @param pid      the caller's pid
        /// @param ancestor the claimed root
        /// @return whether the kernel places the caller under that root
        boolean descendsFrom(long pid, long ancestor);
    }

    /// How long a derived identity is reused for one remote port. Short on
    /// purpose: a reused ephemeral port must not answer for its previous
    /// holder for long, and the only callers that hit the same port twice are
    /// probe loops — which the fatal gate answers, cheaply, every time.
    private static final long CACHE_MILLIS = 3_000;

    private static final int CACHE_LIMIT = 128;

    private final TerminalManager terminals;
    private final Proc proc;
    private final long selfPid = ProcessHandle.current().pid();

    private record Cached(@Nullable Ref ref, long expiresAt) {
    }

    private final ConcurrentHashMap<Integer, Cached> cache = new ConcurrentHashMap<>();

    public PeerIdentity(TerminalManager terminals) {
        this(terminals, OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                ? new WindowsProc() : new LinuxProc());
    }

    PeerIdentity(TerminalManager terminals, Proc proc) {
        this.terminals = terminals;
        this.proc = proc;
    }

    /// Which instance the caller of this connection runs in.
    ///
    /// @param remotePort the request's remote port — the caller socket's own
    ///                   local port
    /// @param localPort  the request's local port — the panel's
    /// @return the instance, or `null` when the caller is identifiable as
    ///         nobody (outside every instance's tree, or outside this network
    ///         namespace)
    public @Nullable Ref derive(int remotePort, int localPort) {
        if (remotePort <= 0 || localPort <= 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        Cached cached = cache.get(remotePort);
        if (cached != null && cached.expiresAt() > now) {
            return cached.ref();
        }
        Ref ref = deriveUncached(remotePort, localPort);
        if (cache.size() > CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(remotePort, new Cached(ref, now + CACHE_MILLIS));
        return ref;
    }

    private @Nullable Ref deriveUncached(int remotePort, int localPort) {
        Long pid = proc.callerPid(remotePort, localPort);
        if (pid == null) {
            return null;
        }
        return walkToRoot(pid);
    }

    /// Walks `pid`'s parent chain to the instance root it lives under, and
    /// only believes a hit the kernel confirms by membership.
    ///
    /// The panel's own pid ends the walk without an answer: a process that
    /// reached this panel without passing through an instance root is nobody's
    /// child but the panel's own.
    private @Nullable Ref walkToRoot(long pid) {
        Map<Long, Ref> roots = roots();
        if (roots.isEmpty()) {
            return null;
        }
        long current = pid;
        for (int depth = 0; depth < 64 && current > 1; depth++) {
            Ref ref = roots.get(current);
            if (ref != null) {
                return proc.descendsFrom(pid, current) ? ref : null;
            }
            if (current == selfPid) {
                return null;
            }
            Long parent = proc.parentPid(current);
            if (parent == null) {
                return null;
            }
            current = parent;
        }
        return null;
    }

    /// Every live instance root this panel knows, as `pid → instance`.
    ///
    /// The terminal roots and the web-mode roots are one registry each; both
    /// are direct children of the panel, so a pid can appear in at most one of
    /// them and the map cannot disagree with itself.
    private Map<Long, Ref> roots() {
        Map<Long, Ref> roots = new HashMap<>();
        for (TerminalManager.LiveTerminal terminal : terminals.liveTerminals()) {
            if (terminal.pid() > 0) {
                roots.put(terminal.pid(), new Ref(terminal.brand(), terminal.instanceId()));
            }
        }
        for (BrandRuntime.LiveRoot root : BrandRuntime.liveRoots()) {
            roots.put(root.pid(), new Ref(root.brand(), root.instanceId()));
        }
        for (Map.Entry<Long, String> root : DshProcessManager.liveRootPids().entrySet()) {
            roots.put(root.getKey(), new Ref("dsh", root.getValue()));
        }
        for (Map.Entry<Long, String> root : ZcodeRuntime.liveRootPids().entrySet()) {
            roots.put(root.getKey(), new Ref("zcode", root.getValue()));
        }
        return roots;
    }

    /// Whether the family's instance currently holds a running terminal — the
    /// condition that turns a `launch` into a takeover.
    ///
    /// @param family      the terminal registry's brand (`dsh`, a brand id, `zcode`)
    /// @param instanceId  the instance
    /// @return whether a terminal session is running for it
    public boolean terminalOpen(String family, String instanceId) {
        return terminals.find(family, instanceId)
                .map(session -> session.state() == TerminalSession.State.RUNNING)
                .orElse(false);
    }

    /// The live membership answer both platforms share: the JDK's process
    /// snapshot on either one.
    private static boolean processDescendsFrom(long pid, long ancestor) {
        if (pid == ancestor) {
            return true;
        }
        return ProcessHandle.of(ancestor)
                .map(ancestorHandle -> ancestorHandle.descendants().anyMatch(
                        descendant -> descendant.pid() == pid))
                .orElse(false);
    }

    /// The `/proc` reader, as deployed on Linux. Every method answers `null`
    /// rather than throwing: an unreadable `/proc` means "unidentified", which
    /// the callers already know how to treat safely.
    static final class LinuxProc implements Proc {

        private static final List<String> FILES = List.of("/proc/net/tcp", "/proc/net/tcp6");

        @Override
        public @Nullable Long callerPid(int callerPort, int panelPort) {
            for (String file : FILES) {
                Long inode = scan(linesOf(file), callerPort, panelPort);
                if (inode != null) {
                    return socketOwner(inode);
                }
            }
            return null;
        }

        /// Finds the caller-side socket among `/proc/net/tcp` lines: the entry
        /// whose local port is the caller's and whose remote port is the
        /// panel's, established. The panel's own side of the same connection is
        /// the mirror entry and never matches.
        ///
        /// @param lines      the file's lines
        /// @param callerPort the caller's local port
        /// @param panelPort  the panel's port
        /// @return the socket's inode, or `null`
        static @Nullable Long scan(List<String> lines, int callerPort, int panelPort) {
            for (String line : lines) {
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 10) {
                    continue;
                }
                if (portOf(fields[1]) != callerPort || portOf(fields[2]) != panelPort) {
                    continue;
                }
                if (!"01".equals(fields[3])) {
                    continue;
                }
                try {
                    return Long.parseLong(fields[9]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }

        private static List<String> linesOf(String file) {
            try {
                return Files.readAllLines(Path.of(file));
            } catch (IOException | RuntimeException e) {
                return List.of();
            }
        }

        /// The port part of a `/proc/net/tcp` address, little-endian hex.
        static int portOf(String address) {
            int colon = address.lastIndexOf(':');
            if (colon < 0 || colon + 1 >= address.length()) {
                return -1;
            }
            try {
                return Integer.parseInt(address.substring(colon + 1), 16);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        /// The descriptor scan, with the uid split handled inside
        /// [SocketOwnerLookup]: a root panel cannot read a uid-1000 agent's
        /// `/proc/<pid>/fd`, so there it asks a child that runs as that uid.
        private static @Nullable Long socketOwner(long inode) {
            return SocketOwnerLookup.owner(inode);
        }

        @Override
        public @Nullable Long parentPid(long pid) {
            try {
                String stat = Files.readString(Path.of("/proc/" + pid + "/stat"));
                // comm may contain spaces and parentheses; everything after the
                // final ')' is field 3 onward, and ppid is the second of those.
                int close = stat.lastIndexOf(')');
                if (close < 0) {
                    return null;
                }
                String[] fields = stat.substring(close + 2).split("\\s+");
                if (fields.length < 2) {
                    return null;
                }
                return Long.parseLong(fields[1]);
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }

        @Override
        public boolean descendsFrom(long pid, long ancestor) {
            return processDescendsFrom(pid, ancestor);
        }
    }

    /// The Windows reader: the TCP owner table answers "who owns this
    /// connection" in one call — `netstat -ano` is this same table.
    ///
    /// Where Linux needs the inode dance (`/proc/net/tcp` then
    /// `/proc/*/fd`), `GetExtendedTcpTable` with `TCP_TABLE_OWNER_PID_ALL`
    /// hands back every row with its owning pid attached. The parent chain
    /// and the membership check use the JDK's snapshot, which the panel
    /// already relies on for killing process trees ([org.jackhuang.hmcl.util.ProcessTree]).
    ///
    /// Every method answers `null` when the table cannot be read — an
    /// unreadable answer is "unidentified", never a guess.
    static final class WindowsProc implements Proc {

        /// `TCP_TABLE_OWNER_PID_ALL`: rows with state, addresses, ports and the
        /// owning pid, for every connection.
        private static final int TCP_TABLE_OWNER_PID_ALL = 5;
        private static final int AF_INET = 2;
        private static final int AF_INET6 = 23;
        private static final int NO_ERROR = 0;
        private static final int ERROR_INSUFFICIENT_BUFFER = 122;
        /// `MIB_TCP_STATE_ESTAB`.
        private static final int ESTABLISHED = 5;

        private interface IpHlpApi extends Library {
            int GetExtendedTcpTable(Pointer table, IntByReference size, boolean order,
                                    int family, int tableClass, int reserved);
        }

        /// Loaded on first use, not on class load: the pure row-scanning
        /// helpers below must stay callable on any platform for their tests.
        private static final class Api {
            static final @Nullable IpHlpApi INSTANCE = load();

            private static @Nullable IpHlpApi load() {
                try {
                    return Native.load("iphlpapi", IpHlpApi.class);
                } catch (RuntimeException | LinkageError e) {
                    return null;
                }
            }
        }

        @Override
        public @Nullable Long callerPid(int callerPort, int panelPort) {
            IpHlpApi api = Api.INSTANCE;
            if (api == null) {
                return null;
            }
            // MIB_TCPROW_OWNER_PID: 6 dwords; the port is the 3rd, the remote
            // port the 5th, the pid the 6th.
            Long v4 = table(api, AF_INET, 6, 2, 4, 5, callerPort, panelPort);
            if (v4 != null) {
                return v4;
            }
            // MIB_TCP6ROW_OWNER_PID: 14 dwords; local port the 7th, remote
            // port the 12th, the pid the 14th.
            return table(api, AF_INET6, 14, 6, 11, 13, callerPort, panelPort);
        }

        /// Reads one family's table and scans it.
        private static @Nullable Long table(IpHlpApi api, int family, int rowDwords,
                                            int localPortWord, int remotePortWord, int pidWord,
                                            int callerPort, int panelPort) {
            IntByReference size = new IntByReference(0);
            int rc = api.GetExtendedTcpTable(Pointer.NULL, size, false, family,
                    TCP_TABLE_OWNER_PID_ALL, 0);
            if (rc != NO_ERROR && rc != ERROR_INSUFFICIENT_BUFFER) {
                return null;
            }
            int needed = size.getValue();
            if (needed < 4) {
                return null;
            }
            Memory memory = new Memory(needed);
            size.setValue(needed);
            rc = api.GetExtendedTcpTable(memory, size, false, family,
                    TCP_TABLE_OWNER_PID_ALL, 0);
            if (rc != NO_ERROR) {
                return null;
            }
            int count = memory.getInt(0);
            int words = Math.min(count, (needed - 4) / 4 / rowDwords) * rowDwords;
            if (words <= 0) {
                return null;
            }
            int[] flat = new int[words];
            for (int i = 0; i < words; i++) {
                flat[i] = memory.getInt(4 + 4L * i);
            }
            return scan(flat, rowDwords, localPortWord, remotePortWord, pidWord, callerPort, panelPort);
        }

        /// Finds the caller-side row: established, its local port the caller's,
        /// its remote port the panel's. The ports in the table are in network
        /// byte order inside a dword.
        ///
        /// @param rows           the table's dwords after the count, rows of `rowDwords`
        /// @param rowDwords      dwords per row
        /// @param localPortWord  the row word holding the local port
        /// @param remotePortWord the row word holding the remote port
        /// @param pidWord        the row word holding the owning pid
        /// @param callerPort     the caller's local port
        /// @param panelPort      the panel's port
        /// @return the owning pid, or `null`
        static @Nullable Long scan(int[] rows, int rowDwords, int localPortWord, int remotePortWord,
                                   int pidWord, int callerPort, int panelPort) {
            for (int row = 0; row + rowDwords <= rows.length; row += rowDwords) {
                if (rows[row] != ESTABLISHED) {
                    continue;
                }
                if (networkPort(rows[row + localPortWord]) != callerPort
                        || networkPort(rows[row + remotePortWord]) != panelPort) {
                    continue;
                }
                return rows[row + pidWord] & 0xFFFFFFFFL;
            }
            return null;
        }

        /// A table port back to host order: the port sits network-byte-order
        /// in the low word of the dword.
        static int networkPort(int dword) {
            return ((dword & 0xFF) << 8) | ((dword >> 8) & 0xFF);
        }

        @Override
        public @Nullable Long parentPid(long pid) {
            return ProcessHandle.of(pid)
                    .flatMap(ProcessHandle::parent)
                    .map(ProcessHandle::pid)
                    .orElse(null);
        }

        @Override
        public boolean descendsFrom(long pid, long ancestor) {
            return processDescendsFrom(pid, ancestor);
        }
    }
}
