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

import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/// Which process holds a socket, read from `/proc` — on behalf of a root panel
/// that may not read an agent's descriptor table itself.
///
/// `PeerIdentity` needs one fact from the kernel: the pid on the caller side of
/// the request's connection. Scanning `/proc/<pid>/fd` answers it, but only when
/// the reader is allowed to inspect the holder's descriptors, and that permission
/// is `ptrace_may_access`: same uid, or `CAP_SYS_PTRACE`.
///
/// The container image gives the panel neither. It is root with the default
/// Docker capability set (`CAP_SYS_PTRACE` absent), and every instance runs as
/// uid 1000, so `/proc/<agent>/fd` answers `EACCES` and every derivation reads
/// as "unidentified" — the fail-closed gate then refuses every stop, delete and
/// terminal, including the ones aimed at somebody else.
///
/// The cure does not widen the panel's capabilities. The panel asks a **child
/// running as the agent's uid** to do the scan ([Privileges#asAgent], the same
/// `setpriv` prefix every instance gets). Same uid is what the kernel asks for,
/// and the panel still cannot read the agent's memory. The child is the panel's
/// own entry point ([Probe]) with a scrubbed environment; it receives an inode
/// and prints a pid, or `-`.
///
/// Deployments **without** the uid split are untouched: a bare-metal panel, or a
/// container started with `--user`, already is the agent's uid, so the scan runs
/// in-process exactly as it did before the child existed. The split is read once
/// from [Privileges#dropsChildren], which is the same decision that chooses
/// whether instances are dropped at all — the two cannot disagree.
///
/// Everything else is unchanged and stays fail-closed: no `java`, no classpath,
/// a timeout, a non-zero exit, unparsable output — all answer `null`, which the
/// gate reads as "unidentified" and refuses. Nothing here guesses.
@NotNullByDefault
public final class SocketOwnerLookup {

    /// How many probe children may run at once. Without a bound, a caller that
    /// opens a new connection per probe would turn every fatal request into a
    /// `java` process; past this many, the answer is `null` (refused) instead of
    /// a queue that grows.
    private static final Semaphore PROBES = new Semaphore(4);

    /// A scan is one `/proc` sweep; if it has not answered by now it never will,
    /// and a stopped clock must not hold a request thread open.
    private static final long PROBE_TIMEOUT_MILLIS = 4_000L;

    private SocketOwnerLookup() {
    }

    /// The pid holding `inode`, or `null` when nobody can say.
    ///
    /// @param inode the socket's inode, from `/proc/net/tcp{,6}`
    /// @return the owning pid, or `null`
    static @Nullable Long owner(long inode) {
        return owner(inode, Privileges.dropsChildren());
    }

    /// The same answer, with the branch named so a test can drive both sides.
    ///
    /// @param inode the socket's inode
    /// @param drop  whether this panel hands agent work to a different uid
    /// @return the owning pid, or `null`
    static @Nullable Long owner(long inode, boolean drop) {
        if (!drop) {
            // No uid split: the panel already is whoever the agents are, so the
            // kernel allows the scan in-process (bare-metal deployment).
            return scan(inode);
        }
        List<String> command = probeCommand(inode);
        return command == null ? null : probe(command);
    }

    /// The in-process scan: the whole descriptor table sweep, under whichever
    /// uid this JVM happens to have.
    ///
    /// @param inode the socket's inode
    /// @return the owning pid, or `null`
    static @Nullable Long scan(long inode) {
        String target = "socket:[" + inode + "]";
        File[] pids = new File("/proc").listFiles();
        if (pids == null) {
            return null;
        }
        for (File pidDir : pids) {
            long pid = pidOf(pidDir);
            if (pid <= 0) {
                continue;
            }
            File[] fds = pidDir.toPath().resolve("fd").toFile().listFiles();
            if (fds == null) {
                continue;
            }
            for (File fd : fds) {
                try {
                    if (target.equals(Files.readSymbolicLink(fd.toPath()).toString())) {
                        return pid;
                    }
                } catch (IOException | RuntimeException ignored) {
                    // A descriptor that vanished mid-scan, or a process we may
                    // not read: not the answer, and not an error.
                }
            }
        }
        return null;
    }

    /// The argv for the child that performs the scan as the agent.
    ///
    /// @param inode the socket's inode
    /// @return the argv, or `null` when the panel cannot name itself
    private static @Nullable List<String> probeCommand(long inode) {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            return null;
        }
        List<String> command = new ArrayList<>(12);
        command.add(javaExecutable());
        // One small run, one answer. Only the heap bound is pinned: the child
        // lives for milliseconds, and flags that name a particular HotSpot
        // collector would turn "this JVM is not HotSpot" into "unidentified".
        command.add("-Xmx32m");
        command.add("-cp");
        command.add(classPath);
        command.add(Probe.class.getName());
        command.add(Long.toString(inode));
        return Privileges.asAgent(command);
    }

    /// Runs the probe and parses its single line.
    ///
    /// The environment is emptied before the child starts: it runs as the agent,
    /// and nothing the panel happens to hold in its environment belongs to a
    /// process that uid can read. `PATH` and `LANG` are set back to fixed values
    /// so `setpriv` resolves and the child's encoding cannot depend on the host.
    ///
    /// @param command the argv, program first
    /// @return the pid the child printed, or `null` on any failure
    static @Nullable Long probe(List<String> command) {
        if (!PROBES.tryAcquire()) {
            return null;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().clear();
            builder.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin");
            builder.environment().put("LANG", "C.UTF-8");
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = builder.start();
            if (!process.waitFor(PROBE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || output.isEmpty() || "-".equals(output)) {
                return null;
            }
            return Long.parseLong(output);
        } catch (IOException | NumberFormatException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            PROBES.release();
        }
    }

    /// The JVM that runs this panel, as an absolute path where one can be named.
    ///
    /// @return the executable to launch
    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        if (home != null && !home.isBlank()) {
            Path java = Path.of(home, "bin", "java");
            if (Files.isExecutable(java)) {
                return java.toString();
            }
        }
        return "java";
    }

    private static long pidOf(File pidDir) {
        try {
            return Long.parseLong(pidDir.getName());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// The child entry point: one inode in, one pid (or `-`) out, then exit.
    ///
    /// It is a separate process because the permission it needs — reading
    /// another uid's `/proc/<pid>/fd` — cannot be borrowed inside the panel's own
    /// credentials without granting the panel `CAP_SYS_PTRACE`, which the image
    /// deliberately does not.
    public static final class Probe {

        private Probe() {
        }

        public static void main(String[] args) {
            if (args.length != 1) {
                System.out.println("-");
                return;
            }
            long inode;
            try {
                inode = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("-");
                return;
            }
            Long pid = scan(inode);
            System.out.println(pid == null ? "-" : pid);
        }
    }
}
