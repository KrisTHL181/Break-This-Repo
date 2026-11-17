/*
 * HDSL
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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.dsh.DshProcessManager.LaunchState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies what an instance is doing while it starts and stops.
///
/// The surface is stubbed with a script that prints the readiness line the
/// launcher listens for and then takes its time going down, which is what makes
/// the stopping window observable. A launcher that forgot an instance the moment
/// it was asked to stop would start a second server inside that window — on
/// another port, because the first one is still held — and that is the defect
/// these tests pin.
class DshProcessLifecycleTest {
    /// The instance these tests start and stop.
    private static final String INSTANCE_ID = "lifecycle";

    /// The instance, once it exists.
    private @TempDir Path workspace;

    /// Stops whatever is left running, so one failure does not affect the next test.
    @AfterEach
    void stopEverything() {
        DshProcessManager.stopAll();
        try {
            DshInstanceManager.delete(INSTANCE_ID);
        } catch (DshException e) {
            // Nothing to clean up: the test that would have made it failed first.
        }
    }

    /// Writes a browser surface that reports ready and then drains slowly.
    ///
    /// @param instance the instance to install it into
    private static void installStubSurface(DshInstance instance) throws Exception {
        Path script = instance.dshEntryPoint();
        Files.createDirectories(script.getParent());
        Files.writeString(script, """
                const args = process.argv.slice(2);
                const index = args.indexOf('--port');
                const port = index >= 0 ? args[index + 1] : '0';
                console.log('dsh web: http://127.0.0.1:' + port + '/?token=lifecycle-stub');
                // Drain when the test says so rather than after a delay: a delay is a race the
                // test can lose on a busy machine, and what the test is about is the window
                // between being asked to stop and being gone.
                const fs = require('fs');
                const release = process.env.DSH_TEST_DRAIN_FILE;
                process.on('SIGTERM', () => {
                  const poll = setInterval(() => {
                    if (!release || fs.existsSync(release)) { clearInterval(poll); process.exit(0); }
                  }, 50);
                });
                // Nothing may outlive the test that started it: a stub that keeps
                // running keeps the test's own JVM from finishing, and a suite that
                // never finishes is worse than one that fails.
                setTimeout(() => process.exit(0), 60000);
                setInterval(() => {}, 1000);
                """);
    }

    /// The file the stub waits for and the test writes.
    ///
    /// @return the file, which is inside the instance's own workspace
    private Path drainFile() {
        return workspace.resolve("drain-ok");
    }

    /// Creates the instance the tests run.
    ///
    /// @return the instance
    private DshInstance makeInstance() throws Exception {
        // A run that failed before its cleanup leaves an instance behind, and
        // creating one with the same id then fails — which turns one bad run into a
        // suite that cannot pass again. Clearing the id first is what makes each run
        // independent of the last.
        try {
            if (DshInstanceManager.find(INSTANCE_ID) != null) {
                DshInstanceManager.delete(INSTANCE_ID);
            }
        } catch (DshException e) {
            LOG.warning("Could not clear the instance a previous run left behind", e);
        }

        DshInstance instance = DshInstanceManager.create(INSTANCE_ID, "1.0.0", DshInstance.DEFAULT_PROFILE,
                workspace, DshNodeRuntime.SYSTEM, DshHomeMode.ISOLATED, null, List.of(),
                Map.of("DSH_TEST_DRAIN_FILE", drainFile().toString()));
        // Stopping is what most of these tests do on the way out, so the stub may go as soon as it
        // is asked. The test that is about the wait removes this again.
        Files.writeString(drainFile(), "go");
        installStubSurface(instance);
        return instance;
    }

    /// Waits for a condition, failing rather than hanging when it never holds.
    ///
    /// @param condition what to wait for
    /// @param what      what it means, for the failure message
    private static void await(BooleanSupplier condition, String what) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for " + what);
    }

    /// Skips a test when the launcher has no Node.js to run the stub with.
    private static void requireNode() {
        Assumptions.assumeTrue(DshNodeRuntime.detect().isPresent(),
                "Node.js is not on PATH; the launcher cannot start anything without it");
    }

    /// Skips a test that needs stopping a process to be something the process can notice.
    ///
    /// `Process.destroy()` is SIGTERM on POSIX — the child gets to finish, and there is a
    /// window between "asked to stop" and "gone" to look at. On Windows it is
    /// `TerminateProcess`: an immediate kill, no signal to handle, no window. A test about
    /// that window has nothing to assert there, and asserting it anyway would report the
    /// platform as a defect in the launcher.
    private static void requireGracefulShutdown() {
        Assumptions.assumeFalse(org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                        == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS,
                "Windows has no signal to stop a process with: destroy() kills it outright");
    }

    @Test
    void anInstanceIsStartingUntilItSaysItIsReady() throws Exception {
        requireNode();
        DshInstance instance = makeInstance();

        assertEquals(LaunchState.STOPPED, DshProcessManager.stateOf(instance.id()),
                "nothing is running before anything is started");

        DshProcess process = DshProcessManager.launch(instance);
        try {
            await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.RUNNING,
                    "the stub surface to report ready");
            assertTrue(process.webUrl().isPresent(), "the readiness line carries the address");
        } finally {
            DshProcessManager.stop(instance.id());
        }

        assertEquals(LaunchState.STOPPED, DshProcessManager.stateOf(instance.id()));
    }

    @Test
    void aSecondLaunchOfARunningInstanceHandsBackTheFirst() throws Exception {
        requireNode();
        DshInstance instance = makeInstance();

        DshProcess first = DshProcessManager.launch(instance);
        try {
            await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.RUNNING, "the stub to report ready");

            DshProcess second = DshProcessManager.launch(instance);

            assertEquals(first, second, "one instance is one server, however often it is asked for");
        } finally {
            DshProcessManager.stop(instance.id());
        }
    }

    @Test
    void anInstanceIsBusyUntilItHasFinishedStopping() throws Exception {
        requireNode();
        requireGracefulShutdown();
        DshInstance instance = makeInstance();

        DshProcessManager.launch(instance);
        await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.RUNNING, "the stub to report ready");

        // This test is about the wait, so the stub does not get to leave until it is over.
        Files.deleteIfExists(drainFile());

        DshProcess process = DshProcessManager.find(instance.id()).orElseThrow();
        CompletableFuture<Boolean> stopped = CompletableFuture.supplyAsync(() -> DshProcessManager.stop(instance.id()));

        // The window the report is about: asked to stop, not yet gone.
        await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.STOPPING, "the instance to be stopping");
        // Waited for rather than looked at once: the flag is set when the stop actually reaches the
        // process, which is a moment after the state changes, and a single look races it.
        await(process::isStopRequested, "the process to know it was stopped on purpose");
        assertTrue(DshProcessManager.isStopping(instance.id()),
                "the instance stays busy for as long as the child takes to exit");

        DshException refused = assertThrows(DshException.class, () -> DshProcessManager.launch(instance),
                "starting again while the first server still holds its port is what moved an instance's port");
        assertTrue(refused.getMessage().contains(INSTANCE_ID), refused.getMessage());

        // Everything the test is about has been seen, so the child may go.
        Files.writeString(drainFile(), "go");
        assertTrue(stopped.get(20, TimeUnit.SECONDS));
        await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.STOPPED, "the instance to be gone");
        assertEquals(LaunchState.STOPPED, DshProcessManager.stateOf(instance.id()));
    }

    @Test
    void aStoppedInstanceLeavesNothingRunning() throws Exception {
        requireNode();
        DshInstance instance = makeInstance();

        DshProcessManager.launch(instance);
        await(() -> DshProcessManager.stateOf(instance.id()) == LaunchState.RUNNING, "the stub to report ready");
        DshProcess process = DshProcessManager.find(instance.id()).orElseThrow();

        DshProcessManager.stop(instance.id());

        assertFalse(process.isRunning(), "the child has to be gone once the stop returns");
        assertTrue(DshProcessManager.find(instance.id()).isEmpty());
        assertTrue(DshProcessManager.running().isEmpty());
        assertFalse(DshProcessManager.stop(instance.id()), "stopping what is already gone is a no-op, not a failure");
    }
}
