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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for splitting a typed argument line.
///
/// The line is one text field standing in for two different argument groups — the launcher's flags
/// come before the profile and the booted app's after it — so a wrong split produces a command that
/// either fails to start or starts the wrong thing. Every case here is a shape somebody could type.
class DshLaunchArgumentsTest {
    /// Parses a line.
    ///
    /// @param line the line
    /// @return what it came to
    private static DshLaunchArguments.Parsed parse(String line) {
        return DshLaunchArguments.parse(line);
    }

    @Test
    void anEmptyLineSaysNothing() {
        for (String line : List.of("", "   ", "\t")) {
            DshLaunchArguments.Parsed parsed = parse(line);
            assertTrue(parsed.launcherArguments().isEmpty());
            assertTrue(parsed.appArguments().isEmpty());
            assertNull(parsed.profile());
            assertFalse(parsed.asksNoOpen());
            assertTrue(parsed.refusals().isEmpty());
        }
    }

    @Test
    void launcherFlagsGoBeforeTheProfileAndTheRestAfter() {
        // The example from the request that asked for this: the launcher's flags first, in order,
        // and the app's `--no-open` after — `--from-default-profile` has to stay beside its value.
        DshLaunchArguments.Parsed parsed =
                parse("--profile rescue --from-default-profile web --no-open");

        assertEquals(List.of("--profile", "rescue", "--from-default-profile", "web"),
                parsed.launcherArguments());
        assertEquals(List.of("--no-open"), parsed.appArguments());
        assertEquals("rescue", parsed.profile());
        assertTrue(parsed.asksNoOpen());
    }

    @Test
    void theInstanceProfileIsUsedWhenTheLineNamesNone() {
        DshLaunchArguments.Parsed parsed = parse("--no-open");
        assertNull(parsed.profile(), "the caller falls back to the instance's own profile");
        assertEquals(List.of("--no-open"), parsed.appArguments());
    }

    @Test
    void thePortIsRefusedInBothSpellings() {
        // `--port 1234` and `--port=1234` are the same request. Matching only the first spelling let
        // the second through as an app argument, where it overrode the port the launcher had
        // reserved — the one thing this refuses to allow, and a silent override at that.
        for (String line : List.of("--port 1234", "--port=1234", "--port", "--port=0")) {
            DshLaunchArguments.Parsed parsed = parse(line);
            assertTrue(parsed.appArguments().isEmpty(),
                    "no part of <" + line + "> may reach the app: " + parsed.appArguments());
            assertFalse(parsed.refusals().isEmpty(), "<" + line + "> must be reported, not dropped");
        }
        assertEquals(List.of("--host", "0.0.0.0"), parse("--port 1234 --host 0.0.0.0").appArguments(),
                "the argument after a refused one is not swallowed with it");
    }

    @Test
    void dshHomeIsRefused() {
        DshLaunchArguments.Parsed parsed = parse("DSH_HOME=/tmp/evil --no-open");
        assertTrue(parsed.refusals().stream().anyMatch(r -> r.contains("DSH_HOME")));
        assertEquals(List.of("--no-open"), parsed.appArguments(),
                "the refusal does not take the rest of the line with it");

        // Why it was refused is a sentence like every other the interface shows, read from the
        // bundles rather than written into the code: it used to be Chinese, which is what every
        // language but that one then read.
        assertEquals(List.of(org.jackhuang.hmcl.util.i18n.I18n.i18n("dsh.launch.refused.home",
                        "DSH_HOME=/tmp/evil")),
                parsed.refusals());
    }

    @Test
    void aReservedPortIsRefusedInTheReadersLanguage() {
        DshLaunchArguments.Parsed parsed = parse("--port 1234");

        assertEquals(List.of(org.jackhuang.hmcl.util.i18n.I18n.i18n("dsh.launch.refused.port", "--port")),
                parsed.refusals());
        assertNotEquals("dsh.launch.refused.port", parsed.refusals().get(0),
                "a key no bundle holds comes back as itself, which is not a sentence");
    }

    @Test
    void aFlagMayCarryItsValueWithAnEqualsSign() {
        assertEquals("rescue", parse("--profile=rescue").profile());
        assertEquals(List.of("--profile=rescue"), parse("--profile=rescue").launcherArguments());
        assertEquals(List.of("--patch=./a.yml"), parse("--patch=./a.yml").launcherArguments());
        assertTrue(parse("--profile=rescue").appArguments().isEmpty(),
                "a launcher flag does not leak into the app's arguments");
    }

    @Test
    void aValueThatLooksLikeAFlagIsStillAValue() {
        assertEquals(List.of("--patch", "--weird.yml"),
                parse("--patch --weird.yml").launcherArguments());
    }

    @Test
    void quotesGroupAPathWithSpaces() {
        assertEquals(List.of("--patch", "/home/me/my patches/a.yml"),
                parse("--patch \"/home/me/my patches/a.yml\"").launcherArguments());
        assertEquals(List.of("--patch", "/home/me/my patches/a.yml"),
                parse("--patch '/home/me/my patches/a.yml'").launcherArguments());
        assertEquals(List.of("a b"), parse("a\\ b").appArguments());
    }

    @Test
    void anUnterminatedQuoteTakesTheRestOfTheLine() {
        // Not an error to report — the harness will not recognise the argument either — but it must
        // not lose the text or throw.
        assertEquals(List.of("unterminated "), parse("\"unterminated ").appArguments());
    }

    @Test
    void appFlagsThatTakeValuesStayInOrder() {
        DshLaunchArguments.Parsed parsed = parse("--host 0.0.0.0 --trusted-host a --trusted-host b");
        assertEquals(List.of("--host", "0.0.0.0", "--trusted-host", "a", "--trusted-host", "b"),
                parsed.appArguments());
    }

    @Test
    void everyRefusalIsReported() {
        DshLaunchArguments.Parsed parsed = parse("--port 1 DSH_HOME=/x --port=2 --no-open");
        assertEquals(3, parsed.refusals().size(), parsed.refusals().toString());
        assertEquals(List.of("--no-open"), parsed.appArguments());
    }

    @Test
    void adoubleDashIsPassedThrough() {
        // Whatever the harness makes of it is its business; the split must not lose it.
        assertEquals(List.of("--", "x"), parse("-- x").appArguments());
    }
}
