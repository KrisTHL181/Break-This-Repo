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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Runs the commands a person asked to run around an instance.
///
/// The original offers two: one before the game starts and one after it has ended, which is
/// how a launcher is made to fit somebody's own workflow — starting a companion process,
/// syncing a directory, telling something that a session began or ended. They belong here
/// for the same reason: an instance of DeepSeek Harness is a long-running thing that people
/// wrap in their own tooling.
///
/// A command that fails before the launch stops the launch: it was asked to run first, and
/// starting anyway would be ignoring what it was for. One that fails afterwards is reported
/// and nothing more can be done about it. Commands run in the instance's own working
/// directory, and the environment the instance runs with is the environment they see, so a
/// command sees what the instance would.
@NotNullByDefault
public final class DshCustomCommands {
    private DshCustomCommands() {
    }

    /// Runs a command, if there is one.
    ///
    /// @param instance the instance the command is about
    /// @param command  the command, or `null` or blank for none
    /// @param phase    what the command is, for the message
    /// @param onLine   receives the command's output, or `null`
    /// @throws DshException when the command cannot be run or exits non-zero
    public static void run(DshInstance instance, @Nullable String command, String phase,
                           @Nullable Consumer<String> onLine) throws DshException {
        if (command == null || command.isBlank()) {
            return;
        }

        LOG.info("Running the " + phase + " command of " + instance.id() + ": " + command);
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DSH_HOME", instance.homeDirectory().toString());
        environment.put("DSH_INSTANCE", instance.id());
        environment.put("DSH_VERSION", instance.version());
        environment.putAll(DshEnvironment.of(instance));

        try {
            // A command is a shell line, not a program and its arguments: that is what a
            // person types into the box, and splitting it here would take away the pipes,
            // redirections and quoting they wrote. Which shell reads that line is the
            // platform's business — `/bin/sh` does not exist on Windows, where the
            // command interpreter is `cmd.exe` and its flag is `/c`.
            DshCommand.Result result = DshCommand.run(
                    shellCommand(command), instance.workspacePath(), environment, onLine);
            if (result.exitCode() != 0) {
                throw new DshException("The " + phase + " command exited with code "
                        + result.exitCode() + ": " + command);
            }
        } catch (IOException e) {
            throw new DshException("Failed to run the " + phase + " command: " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("The " + phase + " command was interrupted: " + command, e);
        }
    }

    /// Wraps a shell line in the command interpreter this system has.
    ///
    /// A pre-launch command that cannot be run stops the launch, so this is not a
    /// detail: `/bin/sh` is a path Windows does not have, and a Windows box given one
    /// would refuse to start every instance that had a command attached.
    ///
    /// @param command the shell line
    /// @return the program and arguments that run it
    static List<String> shellCommand(String command) {
        return shellCommand(command, OperatingSystem.CURRENT_OS);
    }

    /// Wraps a shell line in the command interpreter **of a given system**.
    ///
    /// Package-private and parameterised so the Windows form can be asserted from a test
    /// on any machine; [shellCommand] supplies the running system's form.
    ///
    /// @param command the shell line
    /// @param os      the system whose interpreter to use
    /// @return the command to hand to a `ProcessBuilder`
    static List<String> shellCommand(String command, OperatingSystem os) {
        return os == OperatingSystem.WINDOWS
                ? List.of("cmd", "/c", command)
                : List.of("/bin/sh", "-c", command);
    }

    /// Runs a command and reports a failure instead of raising it.
    ///
    /// Used after an instance has ended: there is nothing left to stop, so a failing command
    /// is something to say rather than something to refuse.
    ///
    /// @param instance the instance the command is about
    /// @param command  the command, or `null` or blank for none
    /// @param phase    what the command is, for the message
    /// @param onLine   receives the command's output, or `null`
    public static void runQuietly(DshInstance instance, @Nullable String command, String phase,
                                  @Nullable Consumer<String> onLine) {
        try {
            run(instance, command, phase, onLine);
        } catch (DshException e) {
            LOG.warning(e.getMessage());
        }
    }
}
