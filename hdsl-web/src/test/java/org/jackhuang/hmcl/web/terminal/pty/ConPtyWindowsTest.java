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

import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.terminal.TerminalSession;
import org.jackhuang.hmcl.web.terminal.TerminalSink;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The ConPTY glue on a real Windows: bytes round-trip, UTF-8 survives, the size
/// reaches the child (including a resize), exit codes and EOF come back, the
/// whole process tree dies with the terminal, and a large paste is chunked.
///
/// The shell these tests start (`cmd.exe`) is the test's own, never the
/// production path's: the terminal route hands [ConPty] an agent's argv and the
/// guard refuses `.cmd`, but a test needs an interactive program that prints
/// something predictable, and `cmd /c` is the Windows equivalent of the
/// `/bin/sh` [PosixPtyTest] uses.
class ConPtyWindowsTest {

    private static final Path TMP = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();

    private static final Map<String, String> ENV = Map.copyOf(System.getenv());

    private static final Path CMD;

    static {
        String systemRoot = System.getenv("SystemRoot");
        CMD = Path.of(systemRoot == null || systemRoot.isBlank() ? "C:\\Windows" : systemRoot,
                "System32", "cmd.exe").toAbsolutePath();
    }

    @BeforeAll
    static void requireConPty() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "ConPTY is Windows-only");
        assumeTrue(ConPty.platformSupported(),
                "this machine has no ConPTY: " + ConPty.UNSUPPORTED_MESSAGE);
    }

    @Test
    void bytesRoundTrip() throws Exception {
        try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "echo hello"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            output.await("hello", Duration.ofSeconds(30));
            assertEquals(0, pty.waitFor());
            assertTrue(output.awaitEnd(Duration.ofSeconds(30)), "the pump reaches EOF: " + output.text());
        }
    }

    @Test
    void utf8OutputArrivesAsUtf8() throws Exception {
        try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "chcp 65001>nul & echo 中文"),
                TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            output.await("中文", Duration.ofSeconds(30));
            assertEquals(0, pty.waitFor());
            assertTrue(output.text().contains("中文"), output.text());
        }
    }

    @Test
    void nodeWritesUtf8WithoutConsoleTranslation() throws Exception {
        Optional<Path> node = Executables.resolve("node");
        assumeTrue(node.isPresent(), "node is not installed; this test needs a real Node.js");
        try (Pty pty = Pty.spawn(List.of(node.get().toString(), "-e", "console.log('中文')"),
                TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            output.await("中文", Duration.ofSeconds(60));
            assertEquals(0, pty.waitFor());
        }
    }

    @Test
    void resizeReachesTheChild() throws Exception {
        Path powershell = Path.of(CMD.getParent().toString(), "WindowsPowerShell", "v1.0",
                "powershell.exe").toAbsolutePath();
        assumeTrue(Files.isRegularFile(powershell), "Windows PowerShell is not installed");
        // The size is asked for in a form no locale can change: `mode con` prints
        // its labels in the system language, and this machine's is not English.
        String size = "\"" + powershell + "\" -NoProfile -Command "
                + "\"Write-Output ('W=' + [Console]::WindowWidth + ' H=' + [Console]::WindowHeight)\"";
        try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/k"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            pty.input().write((size + "\r\n").getBytes(StandardCharsets.US_ASCII));
            pty.input().flush();
            output.await("W=80 H=24", Duration.ofSeconds(60));

            pty.resize(new PtySize(120, 40));
            pty.input().write((size + "\r\n").getBytes(StandardCharsets.US_ASCII));
            pty.input().flush();
            output.await("W=120 H=40", Duration.ofSeconds(60));
        }
    }

    @Test
    void exitCodeComesBackAndOutputEnds() throws Exception {
        try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "exit 7"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            assertEquals(7, pty.waitFor());
            assertTrue(output.awaitEnd(Duration.ofSeconds(30)), "the pump reaches EOF");
            assertEquals(-1, pty.output().read(), "no bytes after the child is gone");
        }
    }

    /// The job object, not the process handle: ending the terminal must take the
    /// grandchild with it. `cmd /c ping` is a parent and a child that both outlive
    /// a killed root unless the tree is the unit of termination.
    ///
    /// The pids are the *new* descendants of this JVM — the difference across the
    /// spawn — because other tests' terminals and the build's own children are
    /// descendants too, and none of them are this terminal's.
    @Test
    void terminateTakesTheWholeProcessTree() throws Exception {
        Set<Long> before = descendants();
        Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "ping.exe -n 60 127.0.0.1"), TMP, ENV,
                new PtySize(80, 24));
        try {
            Collector output = new Collector(pty.output());
            // The address is the locale-independent part of ping's banner: the
            // words around it are in the system language.
            output.await("127.0.0.1", Duration.ofSeconds(30));
            Set<Long> pids = descendants();
            pids.removeAll(before);
            assertFalse(pids.isEmpty(), "the terminal's processes are visible to ProcessHandle");

            pty.terminate(Duration.ofSeconds(1));

            for (long pid : pids) {
                assertTrue(waitUntilGone(pid, Duration.ofSeconds(10)),
                        "process " + pid + " (" + describe(pid) + ") went with the terminal");
            }
        } finally {
            pty.close();
        }
    }

    private static String describe(long pid) {
        return ProcessHandle.of(pid)
                .map(handle -> handle.info().command().orElse("?") + " "
                        + handle.info().commandLine().orElse("?"))
                .orElse("gone");
    }

    @Test
    void aLargePasteIsChunkedAndReadBack() throws Exception {
        String line = "echo " + "x".repeat(4000) + "\r\n";
        try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/k"), TMP, ENV, new PtySize(80, 24))) {
            Collector output = new Collector(pty.output());
            pty.input().write(line.getBytes(StandardCharsets.US_ASCII));
            pty.input().flush();
            output.await("xxxxxxxxxx", Duration.ofSeconds(30));
        }
    }

    /// The layer above: `TerminalSession` pumps the same pseudo console, replays
    /// what a late viewer missed, and reports the exit through the sink. This is
    /// the path the web route actually uses — the tests above stop at [Pty].
    @Test
    void theSessionStackSeesTheSameTerminal() throws Exception {
        Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "echo stacked"), TMP, ENV,
                new PtySize(80, 24));
        TerminalSession session = new TerminalSession("conpty-probe", pty, 64 * 1024);
        try {
            StringBuilder seen = new StringBuilder();
            CountDownLatch ended = new CountDownLatch(1);
            int[] code = {-1};
            session.attach(new TerminalSink() {
                @Override
                public void data(byte[] bytes, int offset, int length) {
                    synchronized (seen) {
                        seen.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
                        seen.notifyAll();
                    }
                }

                @Override
                public void exited(int exitCode) {
                    code[0] = exitCode;
                    ended.countDown();
                }
            });
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            synchronized (seen) {
                while (!seen.toString().contains("stacked")) {
                    long remaining = deadline - System.nanoTime();
                    assertTrue(remaining > 0, "timed out waiting for 'stacked' in: " + seen);
                    TimeUnit.NANOSECONDS.timedWait(seen, remaining);
                }
            }
            assertTrue(ended.await(30, TimeUnit.SECONDS), "the session reported the exit");
            assertEquals(0, code[0]);
            assertEquals(TerminalSession.State.EXITED, session.state());
        } finally {
            session.close();
        }
    }

    /// One terminal is one `conhost.exe`. Opening and closing a terminal must
    /// give it back: a panel that opens terminals all day cannot accumulate a
    /// console host per click. This is the scriptable half of the "20 次开关"
    /// acceptance item — the browser half needs an installed agent.
    @Test
    void openingAndClosingDoesNotLeakConsoleHosts() throws Exception {
        long before = consoleHosts();
        for (int i = 0; i < 20; i++) {
            String line = "line-" + i;
            try (Pty pty = Pty.spawn(List.of(CMD.toString(), "/c", "echo " + line), TMP, ENV,
                    new PtySize(80, 24))) {
                Collector output = new Collector(pty.output());
                output.await(line, Duration.ofSeconds(30));
                assertEquals(0, pty.waitFor());
            }
        }
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        long after = consoleHosts();
        while (after > before + 2 && System.nanoTime() < deadline) {
            Thread.sleep(250);
            after = consoleHosts();
        }
        assertTrue(after - before <= 2, "conhost count grew from " + before + " to " + after);
    }

    private static long consoleHosts() {
        return ProcessHandle.allProcesses()
                .filter(handle -> handle.info().command()
                        .map(command -> command.toLowerCase(java.util.Locale.ROOT).endsWith("conhost.exe"))
                        .orElse(false))
                .count();
    }

    private static Set<Long> descendants() {
        Set<Long> pids = new TreeSet<>();
        ProcessHandle.current().descendants().forEach(handle -> pids.add(handle.pid()));
        return pids;
    }

    private static boolean waitUntilGone(long pid, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                Thread.sleep(50);
                continue;
            }
            return true;
        }
        return !ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /// Reads the terminal in the background so the tests can wait for text with a
    /// deadline instead of blocking forever on a read.
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
                        String chunk = new String(buffer, 0, n, StandardCharsets.UTF_8);
                        synchronized (this) {
                            text.append(chunk);
                            notifyAll();
                        }
                    }
                } catch (Exception ignored) {
                    // The test's terminal is closed; the collector ends here.
                }
                ended = true;
            }, "conpty-test-collector");
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
