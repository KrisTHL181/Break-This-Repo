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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The shell a terminal is opened with: what it exports, and in what order.
class DshInstanceTerminalTest {
    @TempDir
    private Path home;

    private DshInstance instance(Map<String, String> environment) {
        return new DshInstance("test", "0.1.6-alpha.2", "web", home.resolve("work").toString(),
                "system", DshHomeMode.CUSTOM, home.resolve("dsh-home").toString(),
                List.of(), environment, null, null, null, 0, 0L);
    }

    private static DshNodeRuntime runtime(Path bin) {
        return new DshNodeRuntime(bin.resolve("node"), "22.19.0",
                bin.resolve("npm"), "10.9.0", null, null);
    }

    @Test
    void quotesAValueForAShell() {
        assertTrue("'plain'".equals(DshInstanceTerminal.quote("plain")));
        assertTrue("'it'\\''s'".equals(DshInstanceTerminal.quote("it's")), DshInstanceTerminal.quote("it's"));
    }

    @Test
    void theScriptStandsWhereTheInstanceStands() throws Exception {
        Path bin = Files.createDirectories(home.resolve("node").resolve("bin"));
        String script = DshInstanceTerminal.scriptText(instance(Map.of()), null, runtime(bin));

        assertTrue(script.contains("cd '" + DshPaths.instanceDirectory("test") + "'"), script);
        assertTrue(script.contains("export DSH_HOME='" + home.resolve("dsh-home") + "'"), script);
        // The instance's own tools first, and its dsh shim before even those.
        String shim = DshInstanceTerminal.directory().resolve("test").resolve("bin").toString();
        assertTrue(script.contains("export PATH='" + shim + ":" + bin + ":"), script);
        // A session with no account carries no key.
        assertFalse(script.contains(DshAccountRoute.KEY_ENVIRONMENT_VARIABLE), script);
    }

    @Test
    void theAccountKeyTravelsWithTheSession() throws Exception {
        Path bin = Files.createDirectories(home.resolve("node").resolve("bin"));
        DshAccount account = new DshAccount("deepseek", "sk-test", null, null);
        String script = DshInstanceTerminal.scriptText(instance(Map.of()), account, runtime(bin));

        // Under the name the account's own route reads, which is what makes the session's harness
        // able to use that supplier: one variable per route, so no other route can pick it up.
        assertTrue(script.contains("export "
                + DshAccountRoute.environmentVariable(account.displayName()) + "='sk-test'"), script);
    }

    @Test
    void aNameAShellCannotHoldIsLeftOut() throws Exception {
        Path bin = Files.createDirectories(home.resolve("node").resolve("bin"));
        String script = DshInstanceTerminal.scriptText(
                instance(Map.of("GOOD_NAME", "yes", "not a name", "no")), null, runtime(bin));

        assertTrue(script.contains("export GOOD_NAME='yes'"), script);
        assertFalse(script.contains("not a name"), script);
    }

    @Test
    void theShimRunsTheInstancesOwnEntryScript() throws Exception {
        Path bin = Files.createDirectories(home.resolve("node").resolve("bin"));
        DshInstance instance = instance(Map.of());
        String shim = DshInstanceTerminal.shimText(instance, runtime(bin));

        assertTrue(shim.contains("exec '" + bin.resolve("node") + "' '"
                + instance.dshEntryPoint() + "'"), shim);
        assertTrue(shim.contains("\"$@\""), shim);
    }
}
