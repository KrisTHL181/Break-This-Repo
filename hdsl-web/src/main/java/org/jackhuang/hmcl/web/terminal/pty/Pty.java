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
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/// A pseudo terminal running exactly the command it was given.
///
/// There is no shell in the chain: the command's first element is the program
/// and the rest are its arguments, handed to `posix_spawn` as an argv array on
/// POSIX and to `CreateProcessW` as argv-only on Windows. The child gets its own
/// process group (POSIX) or job object (Windows), so [#terminate] can sweep the
/// whole tree the agent may have spawned.
///
/// The three pieces of the contract that matter to the web terminal:
///
/// - **The child is the session.** When it exits, [output] reaches EOF and
///   [#waitFor] answers; there is no prompt after it, because nothing else was
///   ever started.
/// - **Input stops first.** Once the child is gone, writes are refused; the
///   terminal can never be handed to something else.
/// - **Resizing is `TIOCSWINSZ` on POSIX and `ResizePseudoConsole` on Windows**;
///   either way the child learns the new size.
@NotNullByDefault
public interface Pty extends AutoCloseable {

    /// Starts `command` on a new pseudo terminal.
    ///
    /// @param command the program and its arguments; `command[0]` must be an
    ///                absolute path or resolvable by `posix_spawn`
    /// @param cwd     the working directory for the child, or `null` for this
    ///                process's directory
    /// @param environment the child's complete environment (not a delta)
    /// @param size    the initial terminal size
    /// @return the running terminal
    /// @throws PtyException when the platform cannot provide a PTY or a system
    ///                      call fails
    static Pty spawn(List<String> command, Path cwd, Map<String, String> environment, PtySize size)
            throws IOException {
        OperatingSystem os = OperatingSystem.CURRENT_OS;
        if (os == OperatingSystem.LINUX || os == OperatingSystem.MACOS
                || os == OperatingSystem.FREEBSD) {
            return PosixPty.spawn(command, cwd, environment, size);
        }
        if (os == OperatingSystem.WINDOWS) {
            return ConPty.spawn(command, cwd, environment, size);
        }
        throw new PtyException("pseudo terminals are not supported on " + os + " yet");
    }

    /// Bytes typed at the terminal, written straight to the child.
    ///
    /// Writes after the child is gone throw [IOException].
    OutputStream input();

    /// Bytes the child wrote, already decoded from the PTY by the kernel.
    ///
    /// Reaches EOF when the child has exited and its last output was drained.
    InputStream output();

    /// Tells the child the terminal changed size.
    void resize(PtySize size);

    /// Waits for the child to exit and returns its status.
    ///
    /// Exit codes are the usual shell mapping: the child's own code, or
    /// `128 + signal` when it was killed by a signal.
    int waitFor();

    /// The pid of the root process the terminal runs, or `-1` when the platform
    /// does not name one.
    ///
    /// The panel uses it to recognise which instance a connection comes from
    /// ([org.jackhuang.hmcl.web.auth.PeerIdentity]); it is a fact about the
    /// child the kernel set, never something a caller supplies.
    ///
    /// @return the child's pid, or `-1`
    long pid();

    /// Ends the child's process group: `SIGTERM`, then `SIGKILL` if it outlives
    /// `grace`. Safe to call more than once.
    void terminate(Duration grace);

    /// Terminates the child (with a short grace) and releases the terminal.
    @Override
    void close();
}
