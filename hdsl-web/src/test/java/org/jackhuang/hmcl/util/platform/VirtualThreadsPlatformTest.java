/*
 * HDSL-web
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.util.platform;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The two thread kinds this launcher starts work on, and when each is the right one.
///
/// The distinction is not a style choice. A virtual thread cannot unmount from its carrier while
/// a native (foreign function) call is running, so every blocked PTY read pins one — and the
/// carrier pool is only as wide as the machine has cores. Two terminals, two native threads
/// each, took every carrier of a 4-core box and the second terminal's pump never ran: a live
/// child, a connected socket, and an empty screen. `startPlatform` is what the blocking pumps
/// use, and it has to stay a **platform** thread for that reason.
class VirtualThreadsPlatformTest {

    @Test
    void startPlatformRunsTheTaskOnADaemonPlatformThread() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        Thread[] started = new Thread[1];

        Thread thread = VirtualThreads.startPlatform(() -> {
            started[0] = Thread.currentThread();
            ran.countDown();
        }, "hdsl-test-platform");

        assertTrue(ran.await(5, TimeUnit.SECONDS), "the task must actually run");
        assertTrue(thread.isDaemon(), "a pump must not keep the JVM alive");
        assertFalse(isVirtual(thread), "a blocking native read cannot run on a virtual thread");
        assertTrue(thread.getName().startsWith("hdsl-test-platform"), thread.getName());
    }

    /// `Thread.isVirtual` is Java 21; this launcher also runs on 17, where no thread is virtual.
    ///
    /// @param thread the thread to ask about
    /// @return whether it is a virtual thread
    private static boolean isVirtual(Thread thread) {
        try {
            return (boolean) Thread.class.getMethod("isVirtual").invoke(thread);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
