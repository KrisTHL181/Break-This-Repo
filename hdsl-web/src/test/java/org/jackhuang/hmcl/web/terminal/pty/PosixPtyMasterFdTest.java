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

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The master descriptor's lifetime.
///
/// `close(2)` frees the *number* at once and the kernel hands the same number
/// to the next `openpty`, so a thread that was already inside `poll`/`read`/
/// `write` when the terminal ended must not still be working on that number:
/// it would read the next session's output, or type into it.
///
/// The real kernel cannot be asked to recycle a number at the one moment that
/// matters, so these tests drive a stand-in for it. Nothing here spawns a
/// process or touches libc, which is why they run on any platform.
class PosixPtyMasterFdTest {

    /// `poll` reported a readable descriptor.
    private static final int POLLIN = 0x001;

    /// `read` failed because the child's side of the device is gone.
    private static final int EIO = 5;

    /// The pump is inside `read` on the master when the terminal ends, and the
    /// number is handed to the next terminal before that read comes back.
    /// Checking a flag was never enough: the check and the syscall are not
    /// atomic, and the syscall is slow enough to lose the race reliably.
    @Test
    void aReadInFlightStaysOnItsOwnTerminal() throws Exception {
        FakeKernel kernel = new FakeKernel();
        int master = kernel.open("A");
        kernel.watch(master);
        kernel.deliver(master, "A-OK");
        kernel.holdRead();

        try (PosixPty pty = new PosixPty(master, 4242, kernel)) {
            assertTrue(kernel.awaitReadEntered(Duration.ofSeconds(10)), "the pump is inside read");
            assertTrue(kernel.awaitMasterClosed(Duration.ofSeconds(10)), "the end of the child closed the master");

            // The number is free now, and the next terminal takes it.
            int other = kernel.open("B");
            assertEquals(master, other, "the freed number goes to the next terminal");
            kernel.deliver(other, "B-OUCH");

            try {
                kernel.resumeRead();
                assertTrue(pty.awaitPump(Duration.ofSeconds(10)), "the pump ended");
            } finally {
                kernel.resumeRead();
            }

            // The fake's log of what the kernel actually did comes first: it
            // names the terminal every call landed on, which is the defect
            // itself, whether or not the buffer happens to deliver the bytes.
            assertTrue(kernel.violations().isEmpty(), String.join("; ", kernel.violations()));

            String text = new String(readAll(pty.output()), StandardCharsets.UTF_8);
            assertFalse(text.contains("B-OUCH"), "the pump read another terminal's output: " + text);
            assertEquals("A-OK", text, "the terminal's own output still arrives");
        }
    }

    /// The pump reads a chunk off the terminal and then has to wait for the
    /// reader: the replay buffer is full because nobody is draining it. The
    /// child is gone, so the end of the session is declared while the pump is
    /// still holding that chunk. Those bytes came off the terminal and must
    /// reach the reader anyway — they are the last thing the child painted.
    @Test
    void aChunkReadBeforeTheEndStillReachesTheReader() throws Exception {
        FakeKernel kernel = new FakeKernel();
        int master = kernel.open("A");
        kernel.watch(master);
        // Far more than the replay buffer holds, so the pump fills it and then
        // blocks in `put` with one chunk in hand.
        kernel.feed(master, 4 * 1024 * 1024);

        try (PosixPty pty = new PosixPty(master, 4242, kernel)) {
            pty.waitFor();
            assertTrue(pty.awaitPump(Duration.ofSeconds(10)), "the pump ended");

            int delivered = readAll(pty.output()).length;
            assertEquals(kernel.reads() * kernel.chunkSize(), delivered,
                    "the pump read " + kernel.reads() + " chunks off the terminal, but the reader only got "
                            + delivered + " bytes");
        }
    }

    /// A lease is one syscall, not two.
    ///
    /// `dup` hands out a descriptor without `FD_CLOEXEC`, and the second call
    /// that sets it is a window: a child this JVM starts inside that window
    /// inherits the terminal's descriptor and holds the pty open after the
    /// session is over — the same mistake the spawn path used to make, written
    /// twice. `F_DUPFD_CLOEXEC` has no such window, so neither of the two calls
    /// may appear at all.
    @Test
    void aLeaseIsMadeByOneCloseOnExecDup() throws Exception {
        FakeKernel kernel = new FakeKernel();
        int master = kernel.open("A");
        kernel.watch(master);
        kernel.deliver(master, "A-OK");

        try (PosixPty pty = new PosixPty(master, 4242, kernel)) {
            assertTrue(pty.awaitPump(Duration.ofSeconds(10)), "the pump ended");
            List<String> calls = kernel.calls();
            assertTrue(calls.stream().anyMatch(call -> call.startsWith("dupCloexec(")),
                    "the lease has to be a close-on-exec dup: " + calls);
            assertFalse(calls.stream().anyMatch(call -> call.startsWith("dup(")),
                    "a plain dup leaves the lease inheritable until something sets the flag: " + calls);
            assertFalse(calls.stream().anyMatch(call -> call.startsWith("fcntl(")),
                    "nothing may set the flag after the fact — that is the window: " + calls);
        }
    }

    /// Input is refused the moment the terminal is over. The write path is the
    /// third caller of the master, and it must not be the one that keeps going
    /// on a number the next terminal now owns.
    @Test
    void aWriteAfterTheEndDoesNotReachTheNextTerminal() throws Exception {
        FakeKernel kernel = new FakeKernel();
        int master = kernel.open("A");
        kernel.watch(master);

        try (PosixPty pty = new PosixPty(master, 4242, kernel)) {
            assertTrue(kernel.awaitMasterClosed(Duration.ofSeconds(10)), "the master was closed");

            int other = kernel.open("B");
            assertEquals(master, other, "the freed number goes to the next terminal");
            assertThrows(IOException.class,
                    () -> pty.input().write("oops".getBytes(StandardCharsets.UTF_8)));

            assertFalse(kernel.calls().contains("write(" + other + ")"), kernel.calls().toString());
        }
    }

    /// Reads until EOF. Only called once the pump has ended, so the buffer is
    /// finished and this cannot block.
    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer, 0, buffer.length)) >= 0) {
            collected.write(buffer, 0, n);
        }
        return collected.toByteArray();
    }

    /// Stands in for the kernel's descriptor table and for the two ends of the
    /// device.
    ///
    /// It hands out the lowest free number the way `open(2)`/`dup(2)` do, so a
    /// freed master is immediately available to the next terminal — that reuse
    /// is the mistake under test, not an accident of the fake. A `read` that is
    /// blocked looks the number up again on the way out, exactly as the kernel
    /// does, so it lands on whatever owns it by then.
    private static final class FakeKernel implements PosixPty.Syscalls {

        /// One terminal, as the kernel sees it: a name for the tests, and the
        /// bytes the child wrote that nobody has read yet. A duplicated
        /// descriptor shares this object, the way a descriptor and its copies
        /// share the open file.
        private static final class Terminal {
            final String name;
            volatile byte[] pending = new byte[0];

            Terminal(String name) {
                this.name = name;
            }
        }

        private final Map<Integer, Terminal> byFd = new HashMap<>();
        private final List<String> calls = Collections.synchronizedList(new ArrayList<>());
        private final List<String> violations = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch readEntered = new CountDownLatch(1);
        private final CountDownLatch resumeRead = new CountDownLatch(1);
        private final CountDownLatch masterClosed = new CountDownLatch(1);
        private final AtomicBoolean holdRead = new AtomicBoolean();
        private volatile int watchedFd = -1;
        private volatile int chunkSize;
        private volatile int reads;
        private volatile int lastError;

        /// A descriptor for a new terminal, on a fresh number.
        int open(String name) {
            synchronized (byFd) {
                return allocate(new Terminal(name));
            }
        }

        /// Bytes waiting for the terminal on `fd`, as if the child had written
        /// them.
        void deliver(int fd, String text) {
            feed(fd, text.getBytes(StandardCharsets.UTF_8));
        }

        void feed(int fd, byte[] data) {
            synchronized (byFd) {
                byFd.get(fd).pending = data;
            }
        }

        /// The same, with bytes the test only counts: enough of them that the
        /// replay buffer fills and the pump blocks in `put`.
        void feed(int fd, int count) {
            byte[] data = new byte[count];
            for (int i = 0; i < count; i++) {
                data[i] = (byte) i;
            }
            feed(fd, data);
        }

        /// The descriptor the test handed to [PosixPty]: its close is the end
        /// of the terminal, and the test wants to know when that happened
        /// without sleeping.
        void watch(int fd) {
            watchedFd = fd;
        }

        /// Makes the next `read` block inside the fake until [resumeRead].
        void holdRead() {
            holdRead.set(true);
        }

        void resumeRead() {
            resumeRead.countDown();
        }

        boolean awaitReadEntered(Duration timeout) throws InterruptedException {
            return readEntered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitMasterClosed(Duration timeout) throws InterruptedException {
            return masterClosed.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        int reads() {
            return reads;
        }

        int chunkSize() {
            return chunkSize;
        }

        List<String> calls() {
            synchronized (calls) {
                return new ArrayList<>(calls);
            }
        }

        List<String> violations() {
            synchronized (violations) {
                return new ArrayList<>(violations);
            }
        }

        @Override
        public PollResult poll(int fd, int timeoutMillis) {
            record("poll(" + fd + ")");
            return new PollResult(1, POLLIN);
        }

        @Override
        public long read(int fd, Memory buffer, int length) {
            Terminal started = terminal(fd);
            record("read(" + fd + ")");
            reads++;
            if (chunkSize == 0) {
                chunkSize = length;
            }
            blockIfHeld();

            Terminal now = terminal(fd);
            if (now == null) {
                if (started != null) {
                    violations.add("read(" + fd + ") came back on a descriptor that was already gone");
                }
                lastError = EIO;
                return -1;
            }
            if (started != null && started != now) {
                violations.add("read(" + fd + ") started on " + started.name + " and returned "
                        + now.name + "'s bytes");
            }
            byte[] pending = now.pending;
            if (pending.length == 0) {
                return 0;
            }
            int n = Math.min(length, pending.length);
            buffer.write(0, pending, 0, n);
            now.pending = Arrays.copyOfRange(pending, n, pending.length);
            return n;
        }

        @Override
        public long write(int fd, byte[] data, int length) {
            record("write(" + fd + ")");
            return terminal(fd) == null ? -1 : length;
        }

        @Override
        public int dupCloexec(int fd) {
            // The flag is part of the call, so the fake only has to say that the
            // copy exists: a descriptor this method handed out is close-on-exec
            // from that moment, which is the property a plain `dup` does not have.
            synchronized (byFd) {
                Terminal terminal = byFd.get(fd);
                if (terminal == null) {
                    record("dupCloexec(" + fd + ")");
                    return -1;
                }
                int copy = allocate(terminal);
                record("dupCloexec(" + fd + ")->" + copy);
                return copy;
            }
        }

        @Override
        public void close(int fd) {
            synchronized (byFd) {
                Terminal gone = byFd.remove(fd);
                record("close(" + fd + ")" + (gone == null ? "[already gone]" : ""));
            }
            if (fd == watchedFd) {
                masterClosed.countDown();
            }
        }

        @Override
        public int ioctl(int fd, long request, Memory argument) {
            record("ioctl(" + fd + ")");
            return 0;
        }

        @Override
        public int kill(int pid, int signal) {
            return 0;
        }

        @Override
        public int waitpid(int pid, IntByReference status) {
            status.setValue(0);
            return pid;
        }

        @Override
        public int lastError() {
            return lastError;
        }

        private int allocate(Terminal terminal) {
            int fd = 3;
            while (byFd.containsKey(fd)) {
                fd++;
            }
            byFd.put(fd, terminal);
            return fd;
        }

        private Terminal terminal(int fd) {
            synchronized (byFd) {
                return byFd.get(fd);
            }
        }

        private void record(String call) {
            calls.add(call);
        }

        /// The first read blocks until the test lets it go: that is the moment
        /// where the caller has passed the flag check and is inside the call,
        /// made reachable instead of timing-dependent. It never blocks while
        /// holding the fd table's monitor.
        private void blockIfHeld() {
            if (!holdRead.compareAndSet(true, false)) {
                return;
            }
            readEntered.countDown();
            try {
                if (!resumeRead.await(30, TimeUnit.SECONDS)) {
                    violations.add("the test never released the read");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}