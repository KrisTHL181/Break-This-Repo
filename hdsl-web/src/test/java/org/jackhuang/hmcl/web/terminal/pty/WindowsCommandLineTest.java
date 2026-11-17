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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The command line `CreateProcessW` receives, and the rule that makes it
/// readable by `CommandLineToArgvW` again.
///
/// Runs on every platform: the escaping is string work, and the parser below is
/// the reference the child's own `argv` comes out of.
class WindowsCommandLineTest {

    @Test
    void aPlainArgumentIsNotQuoted() throws Exception {
        assertEquals("node.exe --version", ConPty.toCommandLine(List.of("node.exe", "--version")));
    }

    @Test
    void spacesForceQuotes() throws Exception {
        assertEquals("\"C:\\Program Files\\node.exe\" script.js",
                ConPty.toCommandLine(List.of("C:\\Program Files\\node.exe", "script.js")));
    }

    @Test
    void nonAsciiAndSpacesSurvive() throws Exception {
        assertEquals("\"C:\\工作 目录\\a.mjs\"",
                ConPty.toCommandLine(List.of("C:\\工作 目录\\a.mjs")));
    }

    @Test
    void aTrailingBackslashIsDoubled() throws Exception {
        // Inside quotes the closing quote would escape it, so it is doubled…
        assertEquals("\"C:\\my dir\\\\\"", ConPty.toCommandLine(List.of("C:\\my dir\\")));
        // …outside quotes a backslash is ordinary text and needs nothing.
        assertEquals("C:\\dir\\", ConPty.toCommandLine(List.of("C:\\dir\\")));
    }

    @Test
    void aQuoteIsEscaped() throws Exception {
        assertEquals("\"a\\\"b\"", ConPty.toCommandLine(List.of("a\"b")));
    }

    @Test
    void backslashesBeforeAQuoteAreDoubledAndOneEscapesIt() throws Exception {
        // Two backslashes and a quote: five backslashes and then the quote.
        assertEquals("\"a" + "\\".repeat(5) + "\"b\"",
                ConPty.toCommandLine(List.of("a" + "\\".repeat(2) + "\"b")));
    }

    @Test
    void consecutiveBackslashesWithoutAQuoteAreKept() throws Exception {
        assertEquals("a\\\\b", ConPty.toCommandLine(List.of("a\\\\b")));
        // In a quoted argument they are still literal, not doubled.
        assertEquals("\"a\\\\b c\"", ConPty.toCommandLine(List.of("a\\\\b c")));
    }

    @Test
    void anEmptyArgumentIsTwoQuotes() throws Exception {
        assertEquals("\"\"", ConPty.toCommandLine(List.of("")));
    }

    @Test
    void anEqualsSignIsOrdinaryText() throws Exception {
        assertEquals("-Dfoo=bar", ConPty.toCommandLine(List.of("-Dfoo=bar")));
    }

    @Test
    void aTabIsQuoted() throws Exception {
        assertEquals("\"a\tb\"", ConPty.toCommandLine(List.of("a\tb")));
    }

    @Test
    void everyArgumentComesBackFromArgvW() throws Exception {
        List<String> argv = List.of(
                "C:\\Program Files\\node.exe",
                "C:\\工作 目录\\a.mjs",
                "C:\\dir\\",
                "C:\\dir\\\\",
                "a\"b",
                "a\\\\\"b",
                "a\\b",
                "",
                "-Dfoo=bar",
                "two words",
                "tab\there",
                "plain");
        assertEquals(argv, argvW(ConPty.toCommandLine(argv)));
    }

    /// `CommandLineToArgvW`, near enough for the escaping above: whitespace
    /// splits, quotes group, and a run of backslashes is halved unless it is
    /// followed by a quote, where an odd count escapes that quote.
    private static List<String> argvW(String line) {
        List<String> argv = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                i++;
            }
            if (i >= line.length()) {
                break;
            }
            StringBuilder argument = new StringBuilder();
            boolean quoted = false;
            while (i < line.length()) {
                char c = line.charAt(i);
                if (!quoted && (c == ' ' || c == '\t')) {
                    break;
                }
                if (c == '\\') {
                    int backslashes = 0;
                    while (i < line.length() && line.charAt(i) == '\\') {
                        backslashes++;
                        i++;
                    }
                    if (i < line.length() && line.charAt(i) == '"') {
                        argument.append("\\".repeat(backslashes / 2));
                        if (backslashes % 2 == 0) {
                            quoted = !quoted;
                        } else {
                            argument.append('"');
                        }
                        i++;
                    } else {
                        argument.append("\\".repeat(backslashes));
                    }
                    continue;
                }
                if (c == '"') {
                    quoted = !quoted;
                    i++;
                    continue;
                }
                argument.append(c);
                i++;
            }
            argv.add(argument.toString());
        }
        return argv;
    }
}
