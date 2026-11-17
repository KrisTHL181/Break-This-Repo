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
package org.jackhuang.hmcl.web.brand;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The only test that **runs** the generated wrapper.
///
/// Every Hermes bug found on a real machine so far lived in the seam between two
/// artifacts that the unit tests never crossed: a directory nobody created, and a
/// script that expected five arguments from a launch line that passed none. Both
/// were invisible to tests that only inspected strings.
///
/// So this one executes the real `/bin/sh` script against a **fake release**: a
/// `.venv/bin/hermes` that records its own argv instead of starting Python, and a
/// `.venv/bin/python` that makes the `/health` probe succeed immediately. It then
/// asserts what the wrapper did (two children, in order, with the ports it was
/// told), that it left pid files behind, and that a TERM to the wrapper takes the
/// children with it.
class HermesWrapperSmokeTest {

    @TempDir
    Path dir;

    @Test
    void theWrapperStartsBothChildrenAndStopsThemOnTerm() throws Exception {
        Assumptions.assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS,
                "the wrapper is a POSIX shell script");
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/sh")), "needs /bin/sh");

        Path brandRoot = Files.createDirectories(dir.resolve("brands/hermes"));
        BrandInstanceManager manager = new BrandInstanceManager(Brand.HERMES,
                brandRoot.resolve("instances"));
        BrandInstance instance = manager.create("demo", "0.22.5");

        Path pkg = Files.createDirectories(dir.resolve("release"));
        Path bin = Files.createDirectories(pkg.resolve(".venv/bin"));
        Path record = dir.resolve("argv.log");
        writeExecutable(bin.resolve("hermes"), """
                #!/bin/sh
                echo "$@" >> '%s'
                exec sleep 60
                """.formatted(record));
        // The readiness probe only needs an exit code; standing in for CPython is
        // the difference between "runs in 2 s" and "runs on a machine with 3.14".
        writeExecutable(bin.resolve("python"), "#!/bin/sh\nexit 0\n");

        BrandRuntime.LaunchPlan plan = BrandRuntime.hermesPlan(manager, instance, pkg);
        Process process = new ProcessBuilder(plan.command())
                .redirectErrorStream(true)
                .redirectOutput(dir.resolve("wrapper.log").toFile())
                .start();

        try {
            String argv = awaitRecord(record, 2, 10);
            assertTrue(argv.contains("gateway run"), argv);
            // The web mode and the terminal are exclusive: --replace is what takes
            // the host gateway back from a detached TUI copy (docs/brands.md).
            assertTrue(argv.contains("gateway run --replace"), argv);
            assertTrue(argv.contains("dashboard --skip-build --no-open --host 127.0.0.1 --port "
                    + plan.port()), argv);

            Path home = manager.homeDirectory(instance);
            Path gatewayPid = home.resolve("panel/gateway.pid");
            Path dashboardPid = home.resolve("panel/dashboard.pid");
            assertTrue(Files.isRegularFile(gatewayPid), "the wrapper records the gateway pid");
            assertTrue(Files.isRegularFile(dashboardPid), "the wrapper records the dashboard pid");
            long gateway = Long.parseLong(Files.readString(gatewayPid).trim());
            assertTrue(ProcessHandle.of(gateway).map(ProcessHandle::isAlive).orElse(false),
                    "the gateway the wrapper started is alive");
        } finally {
            // What `BrandRuntime.stop` does: one signal to the wrapper, everything
            // below it has to go.
            process.destroy();
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "the wrapper exits after TERM");
        }

        Path home = manager.homeDirectory(instance);
        assertFalse(Files.exists(home.resolve("panel/gateway.pid")),
                "the wrapper removes its pid files on the way out");
        assertFalse(Files.exists(home.resolve("panel/dashboard.pid")));
        // The children are `sleep 60`s started as descendants of the wrapper; once
        // the wrapper is gone they must be too, or every stop leaks a process.
        assertEquals(List.of(), ProcessHandle.current().children()
                        .filter(handle -> handle.info().command().orElse("").contains("sleep"))
                        .toList(),
                "no sleep left behind");
    }

    /// Waits for the fake `hermes` to have been called `lines` times.
    private static String awaitRecord(Path record, int lines, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(record)) {
                List<String> calls = Files.readAllLines(record, StandardCharsets.UTF_8);
                if (calls.size() >= lines) {
                    return String.join("\n", calls);
                }
            }
            Thread.sleep(100);
        }
        String seen = Files.isRegularFile(record)
                ? Files.readString(record, StandardCharsets.UTF_8)
                : "(never called)";
        throw new AssertionError("the fake hermes was not called " + lines
                + " times within " + seconds + " s; saw: " + seen);
    }

    private static void writeExecutable(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
