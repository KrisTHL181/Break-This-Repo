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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Opens the system's terminal on an instance, set up the way the launcher sets it up.
///
/// A launch is all or nothing: the harness composes its whole profile before it answers
/// anything, so one plugin that cannot resolve stops the process, and what a person is left
/// with is a launch that failed rather than the tool that failed. This is the way out of
/// that — a shell in the instance's own working directory with the instance's own toolchain
/// in front of the system's, where dsh is this instance's harness and npm is the npm that
/// instance installs with, so the failure can be reproduced and read a line at a time.
///
/// What is written is a script rather than a command line, because a terminal started from a
/// running desktop inherits the desktop's environment and not the launcher's — gnome-terminal
/// in particular hands the request to a server that has been running since login. Exports in
/// a file survive both.
@NotNullByDefault
public final class DshInstanceTerminal {
    /// Where sessions are written, inside the launcher's data directory.
    private static final String DIRECTORY = "terminals";

    /// The directory the dsh shim sits in.
    private static final String BIN = "bin";

    /// What a session's script is called.
    private static final String SCRIPT = "open.sh";

    /// What an environment variable may be called to be written into the script.
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /// The terminal emulators tried, most preferred first.
    ///
    /// Most take a form of "run this command"; gnome-terminal and kgx want a bare --, and
    /// kitty and foot take the command as an argument of their own. Each is handed the
    /// script's path, which is executable and carries its own interpreter, so no shell has to
    /// be quoted twice.
    private static final List<Emulator> EMULATORS = List.of(
            new Emulator("x-terminal-emulator", List.of("-e")),
            new Emulator("kgx", List.of("--")),
            new Emulator("gnome-terminal", List.of("--")),
            new Emulator("konsole", List.of("-e")),
            new Emulator("xfce4-terminal", List.of("-e")),
            new Emulator("mate-terminal", List.of("-e")),
            new Emulator("tilix", List.of("-e")),
            new Emulator("kitty", List.of()),
            new Emulator("alacritty", List.of("-e")),
            new Emulator("wezterm", List.of("start", "--")),
            new Emulator("foot", List.of()),
            new Emulator("xterm", List.of("-e")));

    private DshInstanceTerminal() {
    }

    /// One terminal emulator and how it is told what to run.
    ///
    /// @param command   the executable's name
    /// @param arguments what has to come between it and the command
    private record Emulator(String command, List<String> arguments) {
    }

    /// Opens a terminal on an instance.
    ///
    /// @param instance the instance
    /// @param account  the account it launches with, or null
    /// @throws DshException when there is no terminal to open, or it cannot be started
    public static void open(DshInstance instance, @Nullable DshAccount account) throws DshException {
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            // Not a missing-emulator error: the whole mechanism is POSIX. What is written is a
            // `#!/usr/bin/env bash` script, it is started by handing its path to a terminal
            // emulator from a Linux list, and the shim it puts on PATH is an extensionless shell
            // script. Saying "no terminal emulator was found" about a machine that has Windows
            // Terminal would be true and useless, so this says what is missing and what to do
            // instead — with the paths, so it can be copied.
            DshNodeRuntime runtime = DshLauncher.resolveRuntime(instance);
            Path workspace = instance.workspacePath();
            throw new DshException("The instance terminal is not available on Windows."
                    + " It opens a POSIX shell through a Linux terminal emulator"
                    + " (x-terminal-emulator, gnome-terminal, xterm …), none of which exist here."
                    + " To work in this instance by hand, open a command prompt with"
                    + (workspace == null ? "" : " its working directory set to " + workspace + ",")
                    + " DSH_HOME set to " + instance.homeDirectory()
                    + ", and " + runtime.binDirectory() + " at the front of PATH.");
        }
        Path script = write(instance, account);
        Emulator emulator = find();
        if (emulator == null) {
            throw new DshException("No terminal emulator was found. Install one of: "
                    + String.join(", ", EMULATORS.stream().map(Emulator::command).toList()));
        }

        List<String> command = new ArrayList<>();
        command.add(emulator.command());
        command.addAll(emulator.arguments());
        command.add(script.toString());
        try {
            new ProcessBuilder(command).start();
            LOG.info("Opened " + emulator.command() + " on " + instance.id() + " with " + script);
        } catch (IOException e) {
            throw new DshException("Failed to start " + emulator.command(), e);
        }
    }

    /// Returns the first terminal emulator this system has.
    ///
    /// @return the emulator, or null when there is none
    private static @Nullable Emulator find() {
        for (Emulator emulator : EMULATORS) {
            if (DshNodeRuntime.which(emulator.command()).isPresent()) {
                return emulator;
            }
        }
        return null;
    }

    /// Writes a session's script and its dsh shim, and returns the script.
    ///
    /// Written beside the launcher's own data and not removed when the terminal closes: the
    /// launcher cannot tell when that happens, a session is worth being able to open again,
    /// and a file that is rewritten on every open cannot go stale.
    ///
    /// @param instance the instance
    /// @param account  the account it launches with, or null
    /// @return the script to hand the terminal
    /// @throws DshException when it cannot be written
    public static Path write(DshInstance instance, @Nullable DshAccount account) throws DshException {
        DshNodeRuntime runtime = DshLauncher.resolveRuntime(instance);
        Path directory = directory().resolve(instance.id());
        Path script = directory.resolve(SCRIPT);
        Path shim = directory.resolve(BIN).resolve("dsh");
        try {
            Files.createDirectories(shim.getParent());
            Files.writeString(script, scriptText(instance, account, runtime), StandardCharsets.UTF_8);
            Files.writeString(shim, shimText(instance, runtime), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DshException("Failed to write the terminal session for " + instance.id(), e);
        }
        executable(script);
        executable(shim);
        return script;
    }

    /// Returns where sessions are written.
    ///
    /// @return the directory, which may not exist
    static Path directory() {
        return org.jackhuang.hmcl.Metadata.HMCL_USER_HOME.resolve(DIRECTORY);
    }

    /// Builds the script that sets a shell up for an instance.
    ///
    /// @param instance the instance
    /// @param account  the account it launches with, or null
    /// @return the script
    /// @throws DshException when the instance's paths cannot be resolved
    public static String scriptText(DshInstance instance, @Nullable DshAccount account) throws DshException {
        return scriptText(instance, account, DshLauncher.resolveRuntime(instance));
    }

    /// Builds the script, with the runtime given rather than looked up.
    ///
    /// The environment is the launcher's own, in the launcher's own order: the home it runs
    /// against, whatever the launcher-wide and per-instance environment says, the account's
    /// key when there is one, and PATH last so that the instance's tools are in front of
    /// whatever any of that asked for.
    ///
    /// @param instance the instance
    /// @param account  the account it launches with, or null
    /// @param runtime  the node runtime the instance runs on
    /// @return the script
    /// @throws DshException when the instance's paths cannot be resolved
    static String scriptText(DshInstance instance, @Nullable DshAccount account, DshNodeRuntime runtime)
            throws DshException {
        Path home = instance.homeDirectory();
        // The instance directory itself, not its workspace: this shell is for looking
        // at the instance — its home, its dsh, its manifest — rather than for the work a
        // session started there would be scoped to.
        Path where = DshPaths.instanceDirectory(instance.id());
        Path bin = directory().resolve(instance.id()).resolve(BIN);

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DSH_HOME", home.toString());
        environment.putAll(DshEnvironment.of(instance));
        if (account != null && account.carriesAKey()) {
            // The same variable and the same place in the order as a launch: the key is what
            // makes the harness answer at all, and a terminal without it reproduces every
            // failure except the one being looked for.
            environment.put(DshAccountRoute.environmentVariable(account.displayName()),
                    account.apiKey().trim());
        }
        String inherited = environment.getOrDefault("PATH", System.getenv("PATH"));
        // A literal `:`, not `File.pathSeparator`: the separator belongs to the shell that
        // reads this script, and bash reads `:` everywhere it runs — including Git Bash on
        // Windows, where the host's own separator is `;`. Using the host's here produced a
        // script that named one very long directory and found nothing in it.
        environment.put("PATH", bin + ":" + runtime.binDirectory()
                + (inherited == null || inherited.isBlank() ? "" : ":" + inherited));

        StringBuilder text = new StringBuilder();
        text.append("#!/usr/bin/env bash\n");
        text.append("# Written by Hello DeepSeek! Launcher, again on every open.\n");
        text.append("#\n");
        text.append("# This shell is set up the way the launcher sets the instance up when it starts\n");
        text.append("# it: the same home, the same environment, the same toolchain. dsh here is this\n");
        text.append("# instance's own harness and npm is the npm it installs with, so running dsh web\n");
        text.append("# in this shell starts this instance's harness and nothing else's.\n");
        text.append("#\n");
        text.append("# With arguments it runs them and exits; with none it leaves a shell.\n");
        text.append('\n');
        text.append("cd ").append(quote(where.toString())).append(" || exit 1\n");
        for (Map.Entry<String, String> variable : environment.entrySet()) {
            if (!NAME.matcher(variable.getKey()).matches()) {
                // A name a shell cannot hold is dropped rather than quoted into something
                // that is no longer an export: the environment editor accepts any text.
                LOG.warning("Not exporting " + variable.getKey() + ": not a usable variable name");
                continue;
            }
            text.append("export ").append(variable.getKey()).append('=')
                    .append(quote(variable.getValue())).append('\n');
        }
        text.append('\n');
        text.append("echo ").append(quote("This shell is set up for the instance "
                + instance.id() + ".")).append('\n');
        text.append("echo ").append(quote("dsh is its own harness; npm is the one it uses. "
                + "Try: dsh web")).append('\n');
        text.append('\n');
        text.append("if [ \"$#\" -gt 0 ]; then\n");
        text.append("    exec \"$@\"\n");
        text.append("fi\n");
        text.append("exec ").append(quote(shell())).append(" -i\n");
        return text.toString();
    }

    /// Builds the dsh shim a session puts in front of the system's.
    ///
    /// @param instance the instance
    /// @param runtime  the node runtime the instance runs on
    /// @return the shim's text
    /// @throws DshException when the instance's entry script cannot be resolved
    static String shimText(DshInstance instance, DshNodeRuntime runtime) throws DshException {
        return "#!/usr/bin/env bash\n"
                + "# The instance's own dsh: what dsh means in the terminal the launcher opened on\n"
                + "# it. The instance's entry script, run by the instance's own node.\n"
                + "exec " + quote(runtime.node().toString()) + " "
                + quote(instance.dshEntryPoint().toString()) + " \"$@\"\n";
    }

    /// Quotes a value for a shell.
    ///
    /// @param value the value
    /// @return it, in single quotes, with any single quote of its own escaped
    static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    /// Returns the shell a session leaves open.
    ///
    /// @return the user's shell, or bash
    static String shell() {
        String shell = System.getenv("SHELL");
        return shell == null || shell.isBlank() ? "/bin/bash" : shell;
    }

    /// Makes a file executable.
    ///
    /// @param file the file
    private static void executable(Path file) {
        if (!file.toFile().setExecutable(true, true)) {
            LOG.warning("Could not make " + file + " executable");
        }
    }
}
