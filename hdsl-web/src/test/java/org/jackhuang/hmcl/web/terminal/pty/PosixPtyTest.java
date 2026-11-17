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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The POSIX PTY glue: bytes round-trip, the size reaches the child (including
/// a resize), exit codes and signals come back, and terminating the terminal
/// takes the whole process group with it.
///
/// These tests spawn real processes through `/bin/sh` (the shell is the test's,
/// never the production path's) and only run on the platforms the glue
/// supports. macOS shares the code but has never been run here, so it is
/// skipped rather than silently "covered".
class PosixPtyTest {

    private static final Map<String, String> ENV = Map.of(
            "PATH", "/usr/local/bin:/usr/bin:/bin",
            "TERM", "xterm-256color",
            "LANG", "C.UTF-8");

    private static final Path TMP = Path.of("/tmp");

    /// One `<fd> -> <target>` line of an `ls -l /proc/<pid>/fd` listing.
    private static final Pattern LISTED_FD = Pattern.compile(" (\\d+) -> (\\S.*)$");

    @BeforeAll
    static void linuxOnly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX,
                "the PTY glue is exercised on Linux; macOS is unverified");
    }

    @Test
    void bytesRoundTrip() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/cat"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            pty.input().write("hello\n".getBytes(StandardCharsets.UTF_8));
            pty.input().flush();
            output.await("hello", Duration.ofSeconds(5));
            assertTrue(output.text().contains("hello"), output.text());
        }
    }

    @Test
    void theChildSeesTheInitialSize() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "stty size"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            output.await("24 80", Duration.ofSeconds(5));
            assertEquals(0, pty.waitFor());
        }
    }

    @Test
    void resizeReachesTheChild() throws Exception {
        try (Pty pty = Pty.spawn(
                List.of("/bin/sh", "-c", "sleep 0.5; stty size"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            Thread.sleep(150);
            pty.resize(new PtySize(120, 40));
            output.await("40 120", Duration.ofSeconds(5));
            assertEquals(0, pty.waitFor());
        }
    }

    /// A resize must also *signal* the child. The pty has no controlling
    /// terminal (nothing calls TIOCSCTTY), so the kernel has no foreground
    /// process group to notify: without an explicit SIGWINCH a running TUI
    /// keeps painting its old width — which a reattach from a different window
    /// turns into wrapped, cascading output.
    @Test
    void resizeSignalsTheChild() throws Exception {
        try (Pty pty = Pty.spawn(
                List.of("/bin/sh", "-c", "trap 'echo GOT-WINCH $(stty size)' WINCH; while :; do sleep 0.1; done"),
                TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            Thread.sleep(250);
            pty.resize(new PtySize(120, 40));
            output.await("GOT-WINCH 40 120", Duration.ofSeconds(5));
        }
    }

    @Test
    void exitCodeComesBack() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "exit 7"), TMP, ENV, new PtySize(80, 24))) {
            assertEquals(7, pty.waitFor());
        }
    }

    @Test
    void aSignalIsReportedAs128PlusTheSignal() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "kill -TERM $$"), TMP, ENV, new PtySize(80, 24))) {
            assertEquals(128 + 15, pty.waitFor());
        }
    }

    @Test
    void terminateTakesTheWholeProcessGroup() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "sleep 300 & echo $!; wait"), TMP, ENV,
                new PtySize(80, 24));
        try {
            Collector output = new Collector(pty.output());
            output.await("\n", Duration.ofSeconds(5));
            Matcher matcher = Pattern.compile("(\\d+)").matcher(output.text());
            assertTrue(matcher.find(), "the child printed its pid: " + output.text());
            long childPid = Long.parseLong(matcher.group(1));
            assertTrue(ProcessHandle.of(childPid).isPresent(), "the background child is running");

            pty.terminate(Duration.ofSeconds(2));

            assertTrue(waitUntilGone(childPid, Duration.ofSeconds(5)),
                    "the background child went with the process group");
        } finally {
            pty.close();
        }
    }

    @Test
    void outputEndsWhenTheChildExits() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "printf done"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            output.await("done", Duration.ofSeconds(5));
            assertEquals(0, pty.waitFor());
            assertTrue(output.awaitEnd(Duration.ofSeconds(5)), "the pump reaches EOF");
            assertEquals(-1, pty.output().read(), "no bytes after the child is gone");
        }
    }

    /// The child holds its terminal as its own stdio and nowhere else.
    ///
    /// The spawn gives the child the slave on three descriptors (its 0, 1, 2),
    /// and the number `openpty` handed out must not survive into it as well:
    /// whatever the child goes on to start inherits that number, and a grandchild
    /// holding it keeps the device's slave side open, so the master never reports
    /// EOF — the drain after the child's exit then falls back to its bound
    /// instead of ending when the child did.
    ///
    /// The listing is the child's own (`ls` reads `/proc/<shell pid>/fd`, so it is
    /// the shell's table that is printed), and the assertion is stated against the
    /// target of the child's stdin rather than a fixed device name: what is being
    /// pinned is that *no other descriptor points at this same terminal*, which is
    /// exactly the inheritance at issue.
    @Test
    void theChildKeepsNoCopyOfTheSlave() throws Exception {
        try (Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "ls -l /proc/$$/fd"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            assertEquals(0, pty.waitFor());
            assertTrue(output.awaitEnd(Duration.ofSeconds(5)), "the pump reaches EOF");

            Map<Integer, String> fds = descriptors(output.text());
            String terminal = fds.get(0);
            assertNotNull(terminal, "the child listed no descriptor for its stdin: " + listing(fds));
            assertTrue(terminal.startsWith("/dev/pts/") && Character.isDigit(terminal.charAt("/dev/pts/".length())),
                    "the child's stdin is the pty's slave, so the listing is about it: " + terminal);
            assertEquals(terminal, fds.get(1), "the child's stdout is not its stdin's terminal");
            assertEquals(terminal, fds.get(2), "the child's stderr is not its stdin's terminal");

            Set<Integer> holding = new TreeSet<>();
            fds.forEach((fd, target) -> {
                if (target.equals(terminal)) {
                    holding.add(fd);
                }
            });
            // The message names the holding descriptors rather than the whole
            // listing: the test JVM hands the child a hundred inherited fds, and
            // only the ones on this terminal are the subject.
            assertEquals(Set.of(0, 1, 2), holding,
                    "the child holds this terminal's slave (" + terminal + ") as its stdio and at no other number,"
                            + " but it holds it at " + holding);
        }
    }

    @Test
    void openingAndClosingDoesNotLeakFileDescriptors() throws Exception {
        long before = fileDescriptors();
        for (int i = 0; i < 20; i++) {
            try (Pty pty = Pty.spawn(List.of("/bin/true"), TMP, ENV, new PtySize(80, 24))) {
                assertEquals(0, pty.waitFor());
            }
        }
        long after = fileDescriptors();
        assertTrue(after - before <= 2, "fd count grew from " + before + " to " + after);
    }

    private static long fileDescriptors() throws Exception {
        try (Stream<Path> files = Files.list(Path.of("/proc/self/fd"))) {
            return files.count();
        }
    }

    /// The `fd -> target` pairs of an `ls -l /proc/<pid>/fd` listing. Lines that
    /// are not a descriptor (the `total` header) are skipped, and the target is
    /// taken from the arrow to the end of the line, which is where `ls` puts it.
    private static Map<Integer, String> descriptors(String listing) {
        Map<Integer, String> fds = new TreeMap<>();
        for (String line : listing.split("\\R")) {
            Matcher matcher = LISTED_FD.matcher(line);
            if (matcher.find()) {
                fds.put(Integer.parseInt(matcher.group(1)), matcher.group(2).trim());
            }
        }
        return fds;
    }

    /// The same pairs, for an assertion message: a test process hands its child
    /// every fd it holds, so the raw listing is a hundred lines of jars.
    private static String listing(Map<Integer, String> fds) {
        StringJoiner joined = new StringJoiner(", ");
        fds.forEach((fd, target) -> joined.add(fd + " -> " + target));
        return joined.toString();
    }

    private static boolean waitUntilGone(long pid, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (ProcessHandle.of(pid).isEmpty()) {
                return true;
            }
            Thread.sleep(20);
        }
        return ProcessHandle.of(pid).isEmpty();
    }

    /// Reads the terminal in the background so the tests can wait for text
    /// with a deadline instead of blocking forever on a read.
    private static final class Collector {

        private final StringBuilder text = new StringBuilder();
        private final Thread thread;
        private volatile boolean ended;

        Collector(InputStream input) {
            thread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                try {
                    int n;
                    while ((n = input.read(buffer, 0, buffer.length)) >= 0) {
                        synchronized (this) {
                            text.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
                            notifyAll();
                        }
                    }
                } catch (Exception ignored) {
                    // The test's terminal is closed; the collector ends here.
                }
                ended = true;
            }, "pty-test-collector");
            thread.setDaemon(true);
            thread.start();
        }

        synchronized void await(String needle, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!text.toString().contains(needle)) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new AssertionError("timed out waiting for '" + needle + "' in: " + text);
                }
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
        }

        synchronized boolean awaitEnd(Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!ended) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
            return true;
        }

        synchronized String text() {
            return text.toString();
        }
    }
}
