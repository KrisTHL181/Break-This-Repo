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
package org.jackhuang.hmcl.web.terminal.pty;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jackhuang.hmcl.util.platform.Privileges;

/// The POSIX pseudo terminal: `openpty` plus `posix_spawn`, glued with JNA.
///
/// Two decisions are load-bearing:
///
/// - **No `fork()`.** Forks in a JVM that has threads are unsafe; `posix_spawn`
///   is the portable, safe way to start a child. The child gets its own
///   process group (`POSIX_SPAWN_SETPGROUP` with pgroup 0), which is what
///   [#terminate] kills.
/// - **No JNA callbacks.** Everything is a blocking call made from a Java
///   thread (`poll` + `read`), so the native side never calls back into the
///   JVM and there is no callback-thread bookkeeping to get wrong.
///
/// When the child exits, the pump drains until the device reports EOF and then
/// reports that end itself; the master is closed right after, which ends the
/// session for every call that has not started yet. That is the property the
/// web terminal relies on: the moment the agent is gone, the screen can no
/// longer change. A call that is already inside the kernel when the close
/// happens is the one exception, and it is deliberate — see [MasterFd]: it
/// finishes on the descriptor it started with and hands over what it read,
/// which is exactly the tail the drain is trying to keep.
///
/// The master's *descriptor* has a lifetime rule of its own, in [MasterFd]: no
/// caller ever uses the descriptor's number directly, because the kernel
/// recycles numbers and this JVM runs several terminals at once.
@NotNullByDefault
final class PosixPty implements Pty {

    /// How long the pump may keep draining after `waitpid` returned.
    ///
    /// This is a bound, not a wait: the pump reaches EOF as soon as the child's
    /// side of the device is gone, and the drain ends there. The bound only
    /// covers a pump that cannot make progress — a reader that stopped
    /// consuming while the replay buffer is full — and covering that case is
    /// the point: the chunk the pump is holding was read off the terminal and
    /// belongs to the reader, so the master has to outlive it. Half a second
    /// was too short for a slow reader and cost the tail of the screen.
    private static final long DRAIN_MILLIS = 2000;

    /// The pump's poll timeout. It is what makes the pump notice a terminal
    /// that ended while the poll was running, and it bounds how long a lease on
    /// the descriptor can outlive the terminal's own close.
    private static final int POLL_MILLIS = 100;

    /// One read from the master. Large enough for a TUI repaint, small enough
    /// that the replay buffer's chunk list stays cheap.
    private static final int READ_BUFFER = 32 * 1024;

    /// How much output the pump may hold for a slow reader before it stops
    /// reading, which in turn blocks the child on its own writes.
    private static final int REPLAY_CAPACITY = 1024 * 1024;

    /// The pump's answer when a poll round produced no bytes: nothing this
    /// time, not the end.
    private static final byte[] NO_BYTES = new byte[0];

    /// Background struct sizes. The C types are opaque; over-allocating is
    /// safe (the callee only touches its own size) and avoids hard-coding
    /// glibc's internal layout. macOS treats both as pointers.
    private static final int POSIX_STRUCT_BYTES = 512;

    private static final short POSIX_SPAWN_SETPGROUP = 0x02;

    private static final int SIGTERM = 15;
    private static final int SIGKILL = 9;

    private static final int POLLIN = 0x001;
    private static final int POLLERR = 0x008;
    private static final int POLLHUP = 0x010;
    private static final int POLLNVAL = 0x020;

    private static final int EINTR = 4;

    private static final int F_SETFD = 2;
    private static final int FD_CLOEXEC = 1;

    /// `F_DUPFD_CLOEXEC`: the same descriptor with `FD_CLOEXEC` already set, in
    /// one call. Its number is per-platform, like the other `fcntl` commands
    /// here: Linux 1030, Darwin 67, FreeBSD 17 (`<sys/fcntl.h>`).
    private static final int F_DUPFD_CLOEXEC = switch (OperatingSystem.CURRENT_OS) {
        case LINUX -> 1030;
        case FREEBSD -> 17;
        // Darwin, and anything else that reaches this class through `Pty.spawn`.
        default -> 67;
    };

    /// `TIOCSWINSZ`; the value is an ioctl request. Linux has its own number;
    /// the BSDs (macOS, FreeBSD) share `_IOW('t', 103, struct winsize)`.
    private static final long TIOCSWINSZ =
            OperatingSystem.CURRENT_OS == OperatingSystem.LINUX ? 0x5414L : 0x80087467L;

    /// `SIGWINCH` is 28 on Linux, macOS and FreeBSD.
    private static final int SIGWINCH = 28;

    private final MasterFd master;
    private final Syscalls syscalls;
    private final int pid;
    private final Buffer output = new Buffer(REPLAY_CAPACITY);
    private final AtomicBoolean inputClosed = new AtomicBoolean();
    private final CountDownLatch finished = new CountDownLatch(1);
    private final CountDownLatch pumpEnded = new CountDownLatch(1);
    private final Thread pump;
    private volatile int exitCode = -1;

    /// Takes an already-running child: [spawn] has done `openpty` and
    /// `posix_spawn` by now, so all that is left is the master descriptor, the
    /// pid and the syscalls.
    ///
    /// Package-private rather than private for [Syscalls]: a test needs to
    /// build a terminal against a stand-in kernel without spawning anything.
    PosixPty(int masterFd, int pid, Syscalls syscalls) {
        this.master = new MasterFd(syscalls, masterFd);
        this.syscalls = syscalls;
        this.pid = pid;
        // Platform threads, not virtual ones: both of these block inside a native
        // call (`poll`+`read`, `waitpid`) and a virtual thread cannot unmount while
        // that runs. Two per terminal — four for two terminals on a 4-core box —
        // pinned every carrier, and the second terminal's pump never started: the
        // child ran, the socket was up, the screen stayed empty.
        this.pump = VirtualThreads.startPlatform(this::pump, "hdsl-pty-" + pid);
        VirtualThreads.startPlatform(this::reap, "hdsl-pty-reap-" + pid);
    }

    /// Starts `command` on a new pseudo terminal.
    ///
    /// @throws PtyException when `openpty` or `posix_spawn` fails; the errno
    ///                      is carried in the exception
    static PosixPty spawn(List<String> command, @Nullable Path cwd,
                          Map<String, String> environment, PtySize size) throws IOException {
        if (command.isEmpty()) {
            throw new PtyException("empty command");
        }
        // Agent work runs as the image's uid, not as the panel's. `setpriv` execs in
        // place, so the child is still the program itself — the process group the
        // terminal kills, and the "no shell in the chain" rule, are both unchanged.
        List<String> effective = Privileges.asAgent(command);
        String program = effective.get(0);
        IntByReference master = new IntByReference();
        IntByReference slave = new IntByReference();
        Memory winsize = winsize(size);
        int rc;
        try {
            rc = LibUtil.INSTANCE.openpty(master, slave, null, null, winsize);
        } catch (UnsatisfiedLinkError e) {
            throw new PtyException("libutil does not provide openpty: " + e.getMessage());
        }
        if (rc != 0) {
            throw new PtyException("openpty", Native.getLastError());
        }
        int masterFd = master.getValue();
        int slaveFd = slave.getValue();
        // Both ends are marked close-on-exec the moment they exist, before the
        // spawn below rather than after it returns. The difference is not
        // cosmetic: until the flag is on, an unrelated child of this JVM started
        // during the spawn inherits the master — and a child holding a pty master
        // can read the instance's terminal or type into it. On the slave the flag
        // is the second half of the rule below (the file actions close it in the
        // child as well): a copy left in the child is inherited by whatever the
        // child starts, and while any copy of the slave is open the master never
        // reports EOF.
        cloexec(masterFd);
        cloexec(slaveFd);
        Memory fileActions = new Memory(POSIX_STRUCT_BYTES);
        Memory attributes = new Memory(POSIX_STRUCT_BYTES);
        fileActions.clear();
        attributes.clear();
        boolean fileActionsReady = false;
        boolean attributesReady = false;
        try {
            // The child's stdio is the slave side (dup2 clears the close-on-exec
            // flag on the descriptor it creates, so the three copies below are the
            // child's to keep); the master never leaks into it, and neither end
            // leaks into any other child this JVM starts.
            check(LibC.INSTANCE.posix_spawn_file_actions_init(fileActions), "posix_spawn_file_actions_init");
            fileActionsReady = true;
            for (int fd = 0; fd <= 2; fd++) {
                check(LibC.INSTANCE.posix_spawn_file_actions_adddup2(fileActions, slaveFd, fd),
                        "posix_spawn_file_actions_adddup2");
            }
            check(LibC.INSTANCE.posix_spawn_file_actions_addclose(fileActions, masterFd),
                    "posix_spawn_file_actions_addclose");
            // The slave's own number goes too, after the dup2s: the child's copy at
            // `slaveFd` is what its grandchildren would inherit, and a grandchild
            // holding the slave keeps the device's slave side open — the master
            // then never reports EOF and the drain falls back to its bound.
            check(LibC.INSTANCE.posix_spawn_file_actions_addclose(fileActions, slaveFd),
                    "posix_spawn_file_actions_addclose");
            if (cwd != null) {
                try {
                    check(LibC.INSTANCE.posix_spawn_file_actions_addchdir_np(fileActions, cwd.toString()),
                            "posix_spawn_file_actions_addchdir_np");
                } catch (UnsatisfiedLinkError e) {
                    throw new PtyException("this libc has no posix_spawn_file_actions_addchdir_np");
                }
            }

            check(LibC.INSTANCE.posix_spawnattr_init(attributes), "posix_spawnattr_init");
            attributesReady = true;
            check(LibC.INSTANCE.posix_spawnattr_setflags(attributes, POSIX_SPAWN_SETPGROUP),
                    "posix_spawnattr_setflags");
            // pgroup 0 means "the child's own pid": one process group per
            // terminal, which is exactly the unit terminate() kills.
            check(LibC.INSTANCE.posix_spawnattr_setpgroup(attributes, 0), "posix_spawnattr_setpgroup");

            IntByReference childPid = new IntByReference();
            String[] argv = effective.toArray(new String[0]);
            String[] envp = environment.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toArray(String[]::new);
            int spawned = LibC.INSTANCE.posix_spawn(childPid, program, fileActions, attributes, argv, envp);
            if (spawned != 0) {
                // `posix_spawn` is `execve`: no PATH lookup. A bare program name
                // therefore fails with ENOENT even when the shell finds it, which
                // reads as "the binary is missing". Say what is actually wrong.
                String what = program.indexOf('/') >= 0
                        ? "posix_spawn " + program
                        : "posix_spawn " + program + " (the PTY spawn does not search PATH; "
                          + "pass an absolute path)";
                throw new PtyException(what, spawned);
            }
            return new PosixPty(masterFd, childPid.getValue(), new Libc());
        } catch (IOException | RuntimeException e) {
            LibC.INSTANCE.close(masterFd);
            throw e;
        } finally {
            if (attributesReady) {
                LibC.INSTANCE.posix_spawnattr_destroy(attributes);
            }
            if (fileActionsReady) {
                LibC.INSTANCE.posix_spawn_file_actions_destroy(fileActions);
            }
            // The child owns the slave side now; the parent must not.
            LibC.INSTANCE.close(slaveFd);
        }
    }

    @Override
    public OutputStream input() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                if (inputClosed.get()) {
                    throw new IOException("the terminal has ended");
                }
                if (length == 0) {
                    return;
                }
                byte[] chunk = offset == 0 && length == bytes.length
                        ? bytes
                        : Arrays.copyOfRange(bytes, offset, offset + length);
                // A lease, like the pump: the terminal can end between the check
                // above and the write, and this must not type into whatever
                // holds the number by then.
                int fd = master.acquire();
                if (fd < 0) {
                    throw new IOException("the terminal has ended");
                }
                try {
                    if (syscalls.write(fd, chunk, length) < 0) {
                        throw new PtyException("write", syscalls.lastError());
                    }
                } finally {
                    master.release(fd);
                }
            }
        };
    }

    @Override
    public InputStream output() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] one = new byte[1];
                int n = read(one, 0, 1);
                return n < 0 ? -1 : one[0] & 0xff;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                try {
                    return output.read(bytes, offset, length);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while reading the terminal", e);
                }
            }
        };
    }

    @Override
    public void resize(PtySize size) {
        int fd = master.acquire();
        if (fd < 0) {
            return;
        }
        try {
            // A failed resize is not fatal: the session keeps running at the old
            // size and the browser sends another one on the next fit.
            syscalls.ioctl(fd, TIOCSWINSZ, winsize(size));
        } finally {
            master.release(fd);
        }
        // The child is spawned without TIOCSCTTY, so the pty has no controlling
        // terminal and the kernel has no foreground process group to notify:
        // TIOCSWINSZ alone changes the size but never delivers SIGWINCH, and a
        // running TUI keeps painting the width it started with. On a reattach
        // from a different window that paints wider than the real terminal and
        // the terminal wraps every line. Deliver the signal ourselves.
        syscalls.kill(pid, SIGWINCH);
    }

    @Override
    public int waitFor() {
        try {
            finished.await();
            return exitCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public void terminate(Duration grace) {
        if (finished.getCount() == 0) {
            return;
        }
        syscalls.kill(-pid, SIGTERM);
        try {
            if (!finished.await(Math.max(1, grace.toMillis()), TimeUnit.MILLISECONDS)) {
                syscalls.kill(-pid, SIGKILL);
                finished.await(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        terminate(Duration.ofSeconds(2));
        closeMaster();
    }

    /// Waits for the pump to stop touching the terminal: by then every byte it
    /// read out of the master is in the replay buffer. [reap] waits on this
    /// before it closes the master, and a test that looks at the buffer needs
    /// the same guarantee — the pump's last [Buffer#put] can land after the
    /// session's end is already visible.
    boolean awaitPump(Duration timeout) throws InterruptedException {
        return pumpEnded.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long pid() {
        return pid;
    }

    /// `waitpid` on the root process, then the end of the session.
    ///
    /// The order is the security contract: input is refused first, then the
    /// pump gets a bounded moment to drain the last bytes, then the master is
    /// closed (no further output can be observed), and only then the process
    /// group is swept.
    private void reap() {
        IntByReference status = new IntByReference();
        int rc;
        do {
            rc = syscalls.waitpid(pid, status);
        } while (rc < 0 && syscalls.lastError() == EINTR);
        exitCode = rc < 0 ? -1 : decodeStatus(status.getValue());
        inputClosed.set(true);
        try {
            // Drain to EOF, and treat the constant as the bound it is: the pump
            // reaches EOF as soon as the device reports the child is gone, so
            // waiting for it costs nothing in the normal case, and a pump that
            // is waiting on the reader gets the chance to hand over what it
            // already read instead of losing it to the clock.
            awaitPump(Duration.ofMillis(DRAIN_MILLIS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeMaster();
        syscalls.kill(-pid, SIGKILL);
        output.finish();
        finished.countDown();
    }

    private void pump() {
        Memory buffer = new Memory(READ_BUFFER);
        try {
            byte[] chunk;
            while ((chunk = readOnce(buffer)) != null) {
                if (chunk.length > 0) {
                    output.put(chunk);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            output.finish();
            pumpEnded.countDown();
        }
    }

    /// One `poll` and the `read` it turned into, both under a single lease on
    /// the master.
    ///
    /// The lease is what makes this safe against [closeMaster]: the call runs
    /// on its own descriptor, so the terminal's number can be freed — and taken
    /// by the next session — while this is still inside the kernel, and what
    /// comes back is still this terminal's output. The lease is released before
    /// the bytes reach the buffer, so a full replay buffer never holds the
    /// descriptor open.
    ///
    /// @return the bytes read, [NO_BYTES] when this round had nothing, or
    ///         `null` when the terminal has ended or its output reached EOF
    private @Nullable byte[] readOnce(Memory buffer) throws InterruptedException {
        int fd = master.acquire();
        if (fd < 0) {
            return null;
        }
        try {
            Syscalls.PollResult ready = syscalls.poll(fd, POLL_MILLIS);
            if (ready.ready() < 0) {
                return syscalls.lastError() == EINTR ? NO_BYTES : null;
            }
            if (ready.ready() == 0) {
                return NO_BYTES;
            }
            short events = (short) ready.events();
            if ((events & POLLIN) != 0) {
                long n = syscalls.read(fd, buffer, READ_BUFFER);
                if (n > 0) {
                    return buffer.getByteArray(0, (int) n);
                }
                if (n == 0) {
                    return null;
                }
                // EINTR after a signal, or EIO because the child closed the
                // slave side: the latter means nothing can ever arrive.
                return syscalls.lastError() == EINTR ? NO_BYTES : null;
            }
            if ((events & (POLLERR | POLLHUP | POLLNVAL)) != 0) {
                return null;
            }
            return NO_BYTES;
        } finally {
            master.release(fd);
        }
    }

    /// The end of the session: no syscall can start on the master any more, and
    /// its number goes back to the kernel. A call that is already running is on
    /// its own descriptor and finishes where it started.
    private void closeMaster() {
        master.close();
    }

    /// The usual shell mapping: exit code, or `128 + signal`.
    private static int decodeStatus(int status) {
        int signal = status & 0x7f;
        if (signal == 0) {
            return (status >> 8) & 0xff;
        }
        return signal == 0x7f ? -1 : 128 + signal;
    }

    private static Memory winsize(PtySize size) {
        Memory memory = new Memory(8);
        memory.clear();
        memory.setShort(0, (short) size.rows());
        memory.setShort(2, (short) size.cols());
        return memory;
    }

    /// POSIX spawn helpers return the error number, never `-1`.
    private static void check(int result, String call) throws PtyException {
        if (result != 0) {
            throw new PtyException(call, result);
        }
    }

    /// Marks a descriptor close-on-exec. Best effort, exactly as it was when this
    /// ran after the spawn: a failure here costs the inheritance it prevents, not
    /// the terminal.
    private static void cloexec(int fd) {
        LibC.INSTANCE.fcntl(fd, F_SETFD, FD_CLOEXEC);
    }

    /// The master descriptor, and the rule that keeps it from being used after
    /// it is closed.
    ///
    /// `close(2)` frees the *number* at once, and the kernel hands that number
    /// to the next `openpty` — this JVM runs one terminal per instance, and a
    /// closed session's number goes straight to the next one. A thread that was
    /// already inside `poll`/`read`/`write` when the terminal ended would then
    /// go on using the number: it would read the next session's output, or type
    /// into it. Gating those calls on a flag was not enough, because the check
    /// and the syscall are not atomic, and a blocking syscall is slow enough to
    /// lose that race reliably.
    ///
    /// So nobody uses the terminal's own number. A caller takes a lease, which
    /// duplicates the descriptor close-on-exec in one call
    /// (`fcntl(F_DUPFD_CLOEXEC)`): the copy is a number the caller owns for the
    /// duration of one syscall, and closing the terminal's number cannot
    /// recycle it. The lease dies with the call, which is also what lets the
    /// device go for real once the last caller is out.
    ///
    /// Only the bookkeeping is locked, never the syscalls: holding the lock
    /// across a `poll` would make every close wait for a read to come back.
    private static final class MasterFd {

        private final Syscalls syscalls;
        private int fd;
        private boolean closed;

        MasterFd(Syscalls syscalls, int fd) {
            this.syscalls = syscalls;
            this.fd = fd;
        }

        /// A descriptor for one call, or `-1` when the terminal has ended. The
        /// caller must [release] it, and must not touch it after that.
        synchronized int acquire() {
            if (closed) {
                return -1;
            }
            // One call, not two: `dup` hands back a descriptor without
            // FD_CLOEXEC, and the `fcntl` that sets it afterwards is a window in
            // which a child this JVM starts inherits the lease — that child holds
            // the pty open after the session is over. The spawn path made the same
            // mistake in the same shape; this is the call that has no window.
            return syscalls.dupCloexec(fd);
        }

        void release(int lease) {
            syscalls.close(lease);
        }

        /// Ends the terminal: no new lease, and the number goes back to the
        /// kernel now. A call that already holds a lease is unaffected.
        synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            syscalls.close(fd);
            fd = -1;
        }
    }

    private static final class Buffer {
        private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
        private final long capacity;
        private byte[] head;
        private int headOffset;
        private long bytes;
        private boolean finished;

        Buffer(long capacity) {
            this.capacity = capacity;
        }

        synchronized void put(byte[] data) throws InterruptedException {
            while (bytes + data.length > capacity && !finished) {
                wait();
            }
            // Take the bytes even when the end has already been declared:
            // `finished` means "nothing more can arrive after what the pump has
            // read", not "throw away what it has". A chunk read just before the
            // child exited is the tail of the screen, and dropping it left a
            // reattached client with a picture older than the session's, which
            // is the opposite of what finish() is trying to preserve.
            chunks.addLast(data);
            bytes += data.length;
            notifyAll();
        }

        synchronized int read(byte[] target, int offset, int length) throws InterruptedException {
            while (bytes == 0 && !finished) {
                wait();
            }
            if (bytes == 0) {
                return -1;
            }
            int copied = 0;
            while (copied < length) {
                if (head == null) {
                    head = chunks.pollFirst();
                    headOffset = 0;
                    if (head == null) {
                        break;
                    }
                }
                int n = Math.min(length - copied, head.length - headOffset);
                System.arraycopy(head, headOffset, target, offset + copied, n);
                headOffset += n;
                copied += n;
                bytes -= n;
                if (headOffset == head.length) {
                    head = null;
                }
            }
            notifyAll();
            return copied;
        }

        synchronized void finish() {
            // Mark the end, but keep what is already buffered: the reader
            // drains it and only then gets EOF. Clearing here was a race the
            // first test run caught — a child that exits immediately had its
            // last line dropped before the collector could read it.
            finished = true;
            notifyAll();
        }
    }

    /// The syscalls the terminal makes once the child is running, behind an
    /// interface so a test can stand in for the kernel.
    ///
    /// The reason this seam exists: the descriptor's lifetime is a race
    /// between a blocking `poll`/`read` and `close`, and it can only be seen by
    /// recycling an fd *number* at one exact moment. That cannot be arranged
    /// with the real kernel, so the numbering is the test's.
    ///
    /// [Libc] is the production side of it and does nothing but delegate to
    /// JNA. `openpty`/`posix_spawn` are deliberately not here: they run before
    /// the terminal exists, and no test drives the spawn path.
    interface Syscalls {

        /// What one `poll` of a single descriptor reported.
        ///
        /// @param ready the number of ready descriptors, `0` on timeout, `-1`
        ///              on error with [Syscalls#lastError] set
        /// @param events the reported `revents`, meaningful only when `ready > 0`
        record PollResult(int ready, int events) {
        }

        /// One `poll` of `fd`, waiting at most `timeoutMillis`.
        PollResult poll(int fd, int timeoutMillis);

        /// One `read`; `-1` on error with [Syscalls#lastError] set.
        long read(int fd, Memory buffer, int length);

        /// One `write`; `-1` on error with [Syscalls#lastError] set.
        long write(int fd, byte[] data, int length);

        /// `fcntl(fd, F_DUPFD_CLOEXEC, 0)`: a new descriptor for the same open
        /// file, close-on-exec from the moment it exists. This is the seam's
        /// whole duplication story: there is deliberately no plain `dup` beside
        /// it, because `dup` followed by a second `fcntl` that sets the flag is
        /// not the same thing — see [MasterFd#acquire].
        int dupCloexec(int fd);

        void close(int fd);

        int ioctl(int fd, long request, Memory argument);

        int kill(int pid, int signal);

        /// `waitpid` with no options.
        int waitpid(int pid, IntByReference status);

        /// The errno of the last call that failed.
        int lastError();
    }

    /// Production [Syscalls]: every call goes straight to libc through JNA.
    private static final class Libc implements Syscalls {

        // M2 (glm-5.2): reuse the pollfd Memory across poll() calls instead of
        // allocating 8 bytes of native memory on every poll cycle.
        private final Memory pollfd = new Memory(8);

        @Override
        public PollResult poll(int fd, int timeoutMillis) {
            pollfd.setInt(0, fd);
            pollfd.setShort(4, (short) POLLIN);
            pollfd.setShort(6, (short) 0);
            int ready = LibC.INSTANCE.poll(pollfd, new NativeLong(1), timeoutMillis);
            return new PollResult(ready, ready > 0 ? pollfd.getShort(6) : 0);
        }

        @Override
        public long read(int fd, Memory buffer, int length) {
            return LibC.INSTANCE.read(fd, buffer, new NativeLong(length)).longValue();
        }

        @Override
        public long write(int fd, byte[] data, int length) {
            return LibC.INSTANCE.write(fd, data, new NativeLong(length)).longValue();
        }

        @Override
        public int dupCloexec(int fd) {
            return LibC.INSTANCE.fcntl(fd, F_DUPFD_CLOEXEC, 0);
        }

        @Override
        public void close(int fd) {
            LibC.INSTANCE.close(fd);
        }

        @Override
        public int ioctl(int fd, long request, Memory argument) {
            return LibC.INSTANCE.ioctl(fd, new NativeLong(request), argument);
        }

        @Override
        public int kill(int pid, int signal) {
            return LibC.INSTANCE.kill(pid, signal);
        }

        @Override
        public int waitpid(int pid, IntByReference status) {
            return LibC.INSTANCE.waitpid(pid, status, 0);
        }

        @Override
        public int lastError() {
            return Native.getLastError();
        }
    }

    /// `openpty` lives in libutil on glibc, macOS and FreeBSD (`/usr/lib/libutil.so`
    /// is part of the base system there).
    private interface LibUtil extends Library {
        LibUtil INSTANCE = Native.load("util", LibUtil.class);

        int openpty(IntByReference amaster, IntByReference aslave, Pointer name, Pointer termp, Pointer winp);
    }

    private interface LibC extends Library {
        LibC INSTANCE = Native.load("c", LibC.class);

        int posix_spawn_file_actions_init(Pointer fileActions);

        int posix_spawn_file_actions_destroy(Pointer fileActions);

        int posix_spawn_file_actions_adddup2(Pointer fileActions, int fd, int newFd);

        int posix_spawn_file_actions_addclose(Pointer fileActions, int fd);

        int posix_spawn_file_actions_addchdir_np(Pointer fileActions, String path);

        int posix_spawnattr_init(Pointer attributes);

        int posix_spawnattr_destroy(Pointer attributes);

        int posix_spawnattr_setflags(Pointer attributes, short flags);

        int posix_spawnattr_setpgroup(Pointer attributes, int pgroup);

        int posix_spawn(IntByReference pid, String path, Pointer fileActions, Pointer attributes,
                        String[] argv, String[] envp);

        int ioctl(int fd, NativeLong request, Pointer argument);

        int close(int fd);

        NativeLong read(int fd, Pointer buffer, NativeLong count);

        NativeLong write(int fd, byte[] buffer, NativeLong count);

        int poll(Pointer fds, NativeLong count, int timeoutMillis);

        int kill(int pid, int signal);

        int waitpid(int pid, IntByReference status, int options);

        int fcntl(int fd, int command, int argument);
    }
}