/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.util;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// Terminates a process together with the processes it spawned.
///
/// A launched web tool is rarely a single process: an npm shim, `cmd.exe`, or a
/// Node entry point starts the real server as a child. Killing only the handle
/// `ProcessBuilder` returned leaves that child running — and holding the port —
/// which is exactly what makes a "stopped" instance still answer (or refuse to
/// start again because its port is taken). Windows shows this most plainly,
/// because `destroy()` there is `TerminateProcess` for the one handle and has no
/// notion of a tree, but the same shape exists wherever the launcher is a shim.
///
/// The descendants are collected *before* the parent is destroyed: once it is
/// gone they are re-parented and [`ProcessHandle#descendants()`] no longer
/// reaches them. They are then asked to exit first, given the same grace the
/// parent had, and killed only if they are still alive afterwards.
@NotNullByDefault
public final class ProcessTree {
    private ProcessTree() {
    }

    /// Stops `process` and every process below it.
    ///
    /// @param process      the root process
    /// @param graceSeconds how long to wait for a polite exit before forcing it
    public static void terminate(Process process, long graceSeconds) {
        List<ProcessHandle> descendants = process.descendants().toList();

        process.destroy();
        try {
            if (!process.waitFor(graceSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(graceSeconds, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        for (ProcessHandle descendant : descendants) {
            if (descendant.isAlive()) {
                descendant.destroy();
            }
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(graceSeconds);
        for (ProcessHandle descendant : descendants) {
            if (!descendant.isAlive()) {
                continue;
            }
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining > 0) {
                    descendant.onExit().get(remaining, TimeUnit.NANOSECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException | ExecutionException e) {
                // Still running past the grace, or its exit state cannot be observed.
            }
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
    }

    /// Whether a command line names something **inside** `directory`.
    ///
    /// On a path boundary, not by substring: `…\instances\demo` is a prefix of
    /// `…\instances\demo-2`, and a substring test would kill the wrong instance — the one
    /// that is working while the broken one is deleted.
    ///
    /// @param command the process's command line
    /// @param needle  the directory, normalised
    /// @return whether the command line references it
    static boolean references(String command, String needle) {
        for (String separator : List.of(command.contains("\\") ? "\\" : "/", "\\", "/")) {
            if (command.contains(needle + separator)) {
                return true;
            }
        }
        return command.equals(needle);
    }

    /// Kills every process whose command line names something under `directory`, and their
    /// descendants.
    ///
    /// The case this exists for: a Windows directory cannot be removed while **any** process
    /// has it as its current directory, and an instance's agent is started with exactly that
    /// directory. A panel restarted since then has no record of that child — its registry
    /// did not survive the restart — so "delete this instance" fails with an empty error
    /// while a process nobody can see sits on the tree.
    ///
    /// **Windows only**, deliberately. Nowhere else does a directory need its users gone
    /// before it can be removed: POSIX unlinks a file — or a directory — whatever still holds
    /// it open. Without the platform check, what is left is a kill by command-line substring,
    /// and an instances tree is exactly where an operator keeps other things running: a
    /// `tail -f` on a log, an `rsync`, a nightly backup. Sweeping those so that a delete looks
    /// tidy is far worse than a directory that goes away a moment later, so every other
    /// platform returns `0` here and touches nothing.
    ///
    /// Matching is on the command line, which is where a launcher that spawns
    /// `node <instance>/dsh/... --profile web` names it. The panel's own command line names
    /// the data directory, not an instance, so it is not matched.
    ///
    /// @param directory the directory a process must reference to be killed
    /// @return how many processes were told to die; always `0` outside Windows
    public static int killReferencing(Path directory) {
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS) {
            return 0;
        }
        String needle = directory.toAbsolutePath().normalize().toString();
        List<ProcessHandle> victims = ProcessHandle.allProcesses()
                .filter(handle -> handle.info().commandLine()
                        .map(command -> references(command, needle))
                        .orElse(false))
                .toList();
        int killed = 0;
        for (ProcessHandle victim : victims) {
            List<ProcessHandle> descendants = victim.descendants().toList();
            if (victim.destroy()) {
                killed++;
            }
            try {
                victim.onExit().get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                victim.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                victim.destroyForcibly();
            } catch (ExecutionException e) {
                // It was already gone; nothing to force.
            }
            for (ProcessHandle descendant : descendants) {
                descendant.destroyForcibly();
            }
        }
        return killed;
    }
}
