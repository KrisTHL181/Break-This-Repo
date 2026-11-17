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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.terminal.pty.Pty;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The session outlives its viewer: detaching keeps the agent running, the
/// replay buffer brings the screen back, and the exit path refuses input. A
/// child that dies without saying anything is flagged as such, and a listener
/// registered around the exit runs exactly once.
class TerminalSessionTest {

    private static final Map<String, String> ENV = Map.of(
            "PATH", "/usr/local/bin:/usr/bin:/bin",
            "TERM", "xterm-256color");

    @BeforeAll
    static void linuxOnly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "PTY tests run on Linux");
    }

    @Test
    void detachKeepsTheAgentAndReattachReplays() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/cat"), Path.of("/tmp"), ENV, new PtySize(80, 24));
        TerminalSession session = new TerminalSession("i1", pty, 4096);
        try {
            Collector first = new Collector();
            session.attach(first);
            session.input().write("hello\n".getBytes(StandardCharsets.UTF_8));
            session.input().flush();
            first.await("hello", Duration.ofSeconds(5));

            session.detach(first);
            session.input().write("world\n".getBytes(StandardCharsets.UTF_8));
            session.input().flush();
            Thread.sleep(150);

            Collector second = new Collector();
            session.attach(second);
            second.await("world", Duration.ofSeconds(5));
            assertTrue(second.text().contains("hello"), "the replay kept the earlier output: " + second.text());
            assertEquals(TerminalSession.State.RUNNING, session.state());
        } finally {
            session.close();
        }
    }

    @Test
    void exitIsReportedToEverySinkAndRefusesInput() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "printf bye; sleep 0.2"), Path.of("/tmp"), ENV,
                new PtySize(80, 24));
        TerminalSession session = new TerminalSession("i2", pty, 4096);
        Collector sink = new Collector();
        session.attach(sink);
        try {
            assertEquals(0, session.waitFor());
            assertTrue(sink.awaitExit(Duration.ofSeconds(5)), "the sink is told the exit code");
            assertEquals(0, sink.exitCode());
            assertEquals(TerminalSession.State.EXITED, session.state());
            assertTrue(sink.text().contains("bye"), "the last output was drained: " + sink.text());

            Collector late = new Collector();
            session.attach(late);
            assertTrue(late.awaitExit(Duration.ofSeconds(1)), "a late viewer is told the session is gone");
            assertEquals(0, late.exitCode());

            assertThrows(java.io.IOException.class,
                    () -> session.input().write("x".getBytes(StandardCharsets.UTF_8)),
                    "the terminal refuses input once the agent is gone");
        } finally {
            session.close();
        }
    }

    /// A child that dies instantly without writing anything leaves no other
    /// trace: the panel sees an exit code, an empty screen and nothing that
    /// says the child never spoke. The session itself is where the two facts
    /// (non-zero code, zero bytes) meet, so it hands the second one to the exit
    /// frame — TerminalExitFrameTest pins that frame.
    @Test
    void aChildThatExitsNonZeroWithoutOutputIsFlagged() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "exit 7"), Path.of("/tmp"), ENV,
                new PtySize(80, 24));
        TerminalSession session = new TerminalSession("i5", pty, 4096);
        try {
            assertEquals(7, session.waitFor());
            assertTrue(session.silentFailure(),
                    "the child printed nothing at all, and the exit frame must be able to say so");
        } finally {
            session.close();
        }
    }

    /// The same non-zero exit, but the child said something first: the failure
    /// came after output, so it is not the silent kind and must not be flagged.
    @Test
    void aChildThatPrintedSomethingIsNotFlagged() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "printf hi; exit 7"), Path.of("/tmp"), ENV,
                new PtySize(80, 24));
        TerminalSession session = new TerminalSession("i6", pty, 4096);
        Collector sink = new Collector();
        session.attach(sink);
        try {
            assertEquals(7, session.waitFor());
            assertTrue(sink.awaitExit(Duration.ofSeconds(5)), "the sink is told the exit code");
            assertTrue(sink.text().contains("hi"), "the output was still delivered: " + sink.text());
            assertFalse(session.silentFailure(),
                    "output happened before the exit: this is not the silent case");
        } finally {
            session.close();
        }
    }

    /// Ending a terminal from the panel takes a signal and prints nothing, so it
    /// has the shape of the failure above without being one. Every 停止, every
    /// delete, and every lifecycle route that closes a terminal behind the user
    /// would otherwise be diagnosed — which is the noise that makes a diagnosis
    /// worthless.
    @Test
    void aTerminalThisPanelEndedIsNotASilentFailure() throws Exception {
        Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "read line"), Path.of("/tmp"), ENV,
                new PtySize(80, 24));
        TerminalSession session = new TerminalSession("i7", pty, 4096);
        try {
            session.terminate(Duration.ofSeconds(2));
            session.waitFor();
            assertTrue(session.exitCode() != 0, "a signal is reported as a non-zero code");
            assertFalse(session.silentFailure(),
                    "the panel ended this one, so the empty screen is what was asked for");
        } finally {
            session.close();
        }
    }

    /// The manager's exit listener drops the registry entry and writes the
    /// `terminal.exit` audit line. The pump used to mark the session exited in
    /// one step and walk the listeners in another, so a listener registered in
    /// between could be run by the pump *and* by onExit's own re-check — a
    /// duplicated audit line that a live panel never happened to show.
    ///
    /// The window is the few nanoseconds between onExit's read of `state` and
    /// its registration, which cannot be aimed at from outside; this hammers it
    /// with many sessions whose exit is triggered while their racing threads are
    /// already registering. What is asserted is the contract — exactly once —
    /// not the interleaving that breaks it.
    @Test
    void aListenerRegisteredAroundTheExitRunsExactlyOnce() throws Exception {
        int sessionCount = 24;
        AtomicInteger doubleRuns = new AtomicInteger();
        List<TerminalSession> sessions = new ArrayList<>();
        List<AtomicBoolean> stops = new ArrayList<>();
        List<Thread> racers = new ArrayList<>();
        try {
            for (int i = 0; i < sessionCount; i++) {
                // `read` parks the child until the trigger below, so the exit
                // happens while the racing thread is inside its loop — not
                // whenever posix_spawn happened to be done.
                Pty pty = Pty.spawn(List.of("/bin/sh", "-c", "read line; exit 7"), Path.of("/tmp"), ENV,
                        new PtySize(80, 24));
                TerminalSession session = new TerminalSession("race-" + i, pty, 4096);
                sessions.add(session);
                AtomicBoolean stop = new AtomicBoolean();
                stops.add(stop);
                Thread racer = new Thread(() -> {
                    // Bounded: pre-fix every registration copied the whole list,
                    // so an unbounded loop would grow without end.
                    for (int sent = 0; sent < 20_000 && !stop.get(); sent++) {
                        AtomicInteger listenerRuns = new AtomicInteger();
                        session.onExit(() -> {
                            if (listenerRuns.incrementAndGet() > 1) {
                                doubleRuns.incrementAndGet();
                            }
                        });
                    }
                }, "onExit-race-" + i);
                racer.setDaemon(true);
                racer.start();
                racers.add(racer);
                Thread.sleep(5);
                session.input().write("go\n".getBytes(StandardCharsets.UTF_8));
                session.input().flush();
            }
            for (TerminalSession session : sessions) {
                assertEquals(7, session.waitFor());
            }
            // waitFor answers as the pump counts its latch down, which happens
            // before it walks the listeners: give that walk time to finish so a
            // duplicate is counted before the assertion below.
            Thread.sleep(200);
        } finally {
            stops.forEach(stop -> stop.set(true));
            for (Thread racer : racers) {
                racer.join(5000);
            }
            for (TerminalSession session : sessions) {
                session.close();
            }
        }
        assertEquals(0, doubleRuns.get(), "a listener must not be run twice");
    }

    /// Package-private so the manager's multi-terminal test can reuse it: a sink that keeps
    /// what it was sent and can be waited on.
    static final class Collector implements TerminalSink {

        private final StringBuilder text = new StringBuilder();
        private final java.util.concurrent.CountDownLatch exit = new java.util.concurrent.CountDownLatch(1);
        private volatile int exitCode = Integer.MIN_VALUE;

        @Override
        public synchronized void data(byte[] bytes, int offset, int length) {
            text.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            notifyAll();
        }

        @Override
        public void exited(int code) {
            exitCode = code;
            exit.countDown();
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

        boolean awaitExit(Duration timeout) throws InterruptedException {
            return exit.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        int exitCode() {
            return exitCode;
        }

        synchronized String text() {
            return text.toString();
        }
    }
}
