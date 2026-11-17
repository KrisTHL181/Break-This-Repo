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
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// One terminal per instance, and a terminated one can be reopened.
class TerminalManagerTest {

    private static final Map<String, String> ENV = Map.of(
            "PATH", "/usr/local/bin:/usr/bin:/bin",
            "TERM", "xterm-256color");

    @BeforeAll
    static void linuxOnly() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX, "PTY tests run on Linux");
    }

    @Test
    void aSecondTerminalForTheSameInstanceIsRefused() throws Exception {
        try (TerminalManager manager = new TerminalManager(1024)) {
            TerminalSession first = manager.open(target("test", "i1"), new PtySize(80, 24));
            assertTrue(manager.find("test", "i1").isPresent());
            assertThrows(TerminalBusyException.class,
                    () -> manager.open(target("test", "i1"), new PtySize(80, 24)));
            assertTrue(first.state() == TerminalSession.State.RUNNING);
        }
    }

    @Test
    void terminateEndsTheSessionAndAllowsReopening() throws Exception {
        try (TerminalManager manager = new TerminalManager(1024)) {
            manager.open(target("test", "i2"), new PtySize(80, 24));
            assertTrue(manager.terminate("test", "i2"));
            awaitNoSession(manager, "test", "i2");
            TerminalSession reopened = manager.open(target("test", "i2"), new PtySize(80, 24));
            assertTrue(reopened.state() == TerminalSession.State.RUNNING);
        }
    }

    /// dsh, the brand categories and ZCode each have their own id space, so the
    /// same id under two brands must not share one PTY — the second open would
    /// otherwise be refused as "already open" for an unrelated instance.
    @Test
    void theSameIdUnderTwoBrandsGetsTwoIndependentTerminals() throws Exception {
        try (TerminalManager manager = new TerminalManager(1024)) {
            manager.open(target("dsh", "same"), new PtySize(80, 24));
            manager.open(target("zcode", "same"), new PtySize(80, 24));
            assertTrue(manager.find("dsh", "same").isPresent());
            assertTrue(manager.find("zcode", "same").isPresent());
            assertTrue(manager.terminate("dsh", "same"));
            assertTrue(manager.find("zcode", "same").isPresent(), "the other brand's terminal must survive");
        }
    }

    @Test
    void closingTheManagerEndsEveryTerminal() throws Exception {
        TerminalManager manager = new TerminalManager(1024);
        TerminalSession one = manager.open(target("test", "i3"), new PtySize(80, 24));
        TerminalSession two = manager.open(target("test", "i4"), new PtySize(80, 24));
        manager.close();
        assertTrue(one.waitFor() != Integer.MIN_VALUE);
        assertTrue(two.waitFor() != Integer.MIN_VALUE);
        assertTrue(manager.size() == 0);
    }

    /// Two terminals of **different brands**, both live at the same time, each with its own
    /// bytes. The reported failure was exactly this: the first brand's terminal worked and the
    /// second sat at a blinking cursor, because the pump of the second one never got a thread
    /// (see [VirtualThreads#startPlatform] — the PTY pumps used to be virtual, and a blocked
    /// native read pins its carrier, of which a small machine has one per core).
    @Test
    void twoTerminalsOfTwoBrandsBothProduceTheirOwnOutput() throws Exception {
        try (TerminalManager manager = new TerminalManager(1024)) {
            TerminalSession kimi = manager.open(target("kimi", "i5"), new PtySize(80, 24));
            TerminalSession kimiAgain = manager.open(target("opencode", "i6"), new PtySize(80, 24));
            TerminalSessionTest.Collector first = new TerminalSessionTest.Collector();
            TerminalSessionTest.Collector second = new TerminalSessionTest.Collector();
            kimi.attach(first);
            kimiAgain.attach(second);

            kimi.input().write("first-terminal\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            kimi.input().flush();
            kimiAgain.input().write("second-terminal\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            kimiAgain.input().flush();

            first.await("first-terminal", Duration.ofSeconds(10));
            second.await("second-terminal", Duration.ofSeconds(10));
            // `/bin/cat` echoes; each session's output must not appear in the other's screen.
            assertTrue(!first.text().contains("second-terminal"), first.text());
            assertTrue(!second.text().contains("first-terminal"), second.text());
            assertTrue(kimi.state() == TerminalSession.State.RUNNING);
            assertTrue(kimiAgain.state() == TerminalSession.State.RUNNING);
        }
    }

    private static TerminalTarget target(String brand, String instanceId) {
        return new TerminalTarget(brand, instanceId, List.of("/bin/cat"),
                Path.of("/tmp"), ENV);
    }

    private static void awaitNoSession(TerminalManager manager, String brand, String instanceId)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (manager.find(brand, instanceId).isPresent() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(manager.find(brand, instanceId).isEmpty(), "the session should be gone");
    }
}