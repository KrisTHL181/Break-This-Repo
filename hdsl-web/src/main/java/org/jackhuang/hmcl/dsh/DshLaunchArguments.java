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

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Command-line arguments somebody typed for an instance, split into the two places they belong.
///
/// DeepSeek Harness takes its arguments in two groups, and a launcher has to keep them apart:
///
/// ```
/// dsh [launcher flags] --profile <name> [app flags] [app args]
/// ```
///
/// The launcher's own flags select and patch the profile tree, and the rest go to the booted app.
/// A single text box cannot say which is which, so this decides by the flag: the ones the launcher
/// owns come first, everything else follows the profile. That is what makes a line like
///
/// ```
/// --profile rescue --from-default-profile web --no-open
/// ```
///
/// work — `--profile` and `--from-default-profile` are the launcher's, `--from-default-profile web`
/// has to stay in that order, and `--no-open` belongs to the web app.
///
/// Two things are not the user's to set, and are reported rather than silently dropped:
///
/// - **`DSH_HOME`**, because it is how the launcher keeps one instance's state away from another's.
///   It is not a flag at all — it is an environment variable — but a line imitating one
///   (`DSH_HOME=/somewhere`) is refused for the same reason.
/// - **`--port`**, because the port is part of an instance's identity: the browser interface keys
///   its stored state by origin, so a port that moved would present an empty interface and two
///   writers could corrupt one history. The launcher states it.
///
/// The profile *is* the user's to set: an instance created for one profile may be pointed at
/// another, and the harness recognises the profile by name on every start.
@NotNullByDefault
public final class DshLaunchArguments {
    /// The flags that belong to the launcher rather than to the booted app.
    ///
    /// `--profile` and `--patch` are the launcher's own; `--from-default-profile` is how a profile
    /// is created from another one's tree, and it is a launcher flag wherever it appears.
    private static final Set<String> LAUNCHER_FLAGS = Set.of(
            "--profile", "--patch", "--from-default-profile");

    /// The flags that take a value, so the tokeniser does not mistake the value for a flag.
    private static final Set<String> VALUE_FLAGS = Set.of(
            "--profile", "--patch", "--from-default-profile",
            "--host", "--port", "--trusted-host");

    /// The flags the launcher states for itself and refuses to have taken over.
    private static final Set<String> MANAGED_FLAGS = Set.of("--port");

    /// The prefix that marks an environment assignment rather than a flag.
    private static final String ENVIRONMENT_PREFIX = "DSH_HOME=";

    /// What a typed line came to.
    ///
    /// @param launcherArguments the launcher's own flags, in the order they were typed
    /// @param appArguments      the flags and arguments for the booted app, in order
    /// @param profile           the profile the line names, or `null` when it names none
    /// @param refusals          the arguments that were refused, each with why
    public record Parsed(
            @org.jetbrains.annotations.Unmodifiable List<String> launcherArguments,
            @org.jetbrains.annotations.Unmodifiable List<String> appArguments,
            @Nullable String profile,
            @org.jetbrains.annotations.Unmodifiable List<String> refusals) {

        /// Reports whether the line asks for no browser to be opened.
        ///
        /// Asked of the app arguments because that is where the flag belongs; a user who writes it
        /// is taken at their word, and one who does not gets the launcher's own answer.
        ///
        /// @return whether `--no-open` was given
        public boolean asksNoOpen() {
            return appArguments.contains("--no-open");
        }
    }

    private DshLaunchArguments() {
    }

    /// Splits a typed line into the arguments the launcher owns and the arguments the app gets.
    ///
    /// @param text the typed line, or `null` for none
    /// @return what it came to
    public static Parsed parse(@Nullable String text) {
        return parseArguments(tokenize(text));
    }

    /// Splits arguments into the ones the launcher owns and the ones the app gets.
    ///
    /// The instance stores its arguments as a list rather than as a line, because that is what a
    /// process is given; the field somebody types into turns its text into this list on the way in.
    /// Both entry points meet here so the rules are stated once.
    ///
    /// @param arguments the arguments
    /// @return what they came to
    public static Parsed parseArguments(List<String> arguments) {
        List<String> launcher = new ArrayList<>();
        List<String> app = new ArrayList<>();
        List<String> refusals = new ArrayList<>();
        String profile = null;
        // Locals, not fields: two launches can be prepared at once, and a parser that remembered
        // its place between calls would put one instance's argument into the other's command.
        boolean pendingLauncherValue = false;
        boolean pendingProfile = false;
        boolean skipValue = false;

        for (String token : arguments) {
            if (token.startsWith(ENVIRONMENT_PREFIX)) {
                refusals.add(i18n("dsh.launch.refused.home", token));
                continue;
            }

            // A flag may carry its value with `=` instead of a following word — `--port=1234` is the
            // same request as `--port 1234` — so the name is what is matched, not the whole token.
            // Matching the whole token let `--port=1234` through as an app argument, where it
            // overrode the port the launcher had reserved: the one thing this refuses to allow.
            String name = token;
            String inlineValue = null;
            if (token.startsWith("--")) {
                int equals = token.indexOf('=');
                if (equals > 0) {
                    name = token.substring(0, equals);
                    inlineValue = token.substring(equals + 1);
                }
            }

            if (MANAGED_FLAGS.contains(name)) {
                refusals.add(i18n("dsh.launch.refused.port", name));
                // Skip the value too, so it does not land in the app's arguments on its own. With
                // `=` the value was on the same token and is simply dropped with it.
                skipValue = inlineValue == null;
                continue;
            }
            if (LAUNCHER_FLAGS.contains(name)) {
                if (inlineValue != null) {
                    launcher.add(token);
                    if ("--profile".equals(name)) {
                        profile = inlineValue;
                    }
                    continue;
                }
                launcher.add(token);
                pendingLauncherValue = VALUE_FLAGS.contains(name);
                if ("--profile".equals(name)) {
                    pendingProfile = true;
                }
                continue;
            }
            if (pendingProfile) {
                profile = token;
                launcher.add(token);
                pendingProfile = false;
                pendingLauncherValue = false;
                continue;
            }
            if (pendingLauncherValue) {
                launcher.add(token);
                pendingLauncherValue = false;
                continue;
            }
            if (skipValue) {
                skipValue = false;
                continue;
            }
            app.add(token);
        }

        return new Parsed(List.copyOf(launcher), List.copyOf(app), profile, List.copyOf(refusals));
    }

    /// Splits a line into arguments, honouring quotes.
    ///
    /// A path with a space in it has to be typeable, so single and double quotes group, and a
    /// backslash escapes the next character outside single quotes. This is the shell's own rule,
    /// minus the parts a settings field has no use for (no expansion, no substitution): what is
    /// typed is what is passed.
    ///
    /// The field somebody types into stores what they typed as the arguments it names, so this is
    /// also the way in for the interface.
    ///
    /// @param text the line
    /// @return the arguments
    public static List<String> tokenize(@Nullable String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        StringBuilder current = new StringBuilder();
        boolean started = false;
        char quote = 0;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                started = true;
                continue;
            }
            if (c == '\\' && quote != '\'') {
                escaped = true;
                started = true;
                continue;
            }
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
                started = true;
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                started = true;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (started) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
                continue;
            }
            current.append(c);
            started = true;
        }
        if (started) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
