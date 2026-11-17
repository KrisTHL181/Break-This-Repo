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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.util.platform.GitBash;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The shell hint kimi-code needs on Windows.
///
/// Without `KIMI_SHELL_PATH` the agent refuses the first tool call with
/// "failed to start shell: Git Bash was not found on this Windows host", however
/// well Git for Windows is installed. The panel knows the machine, so it names
/// the shell — and it never overrides one the operator named.
class KimiTuiShellTest {

    @Test
    void thePanelNamesTheGitBashItCanSee() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "kimi needs the hint on Windows");
        assumeTrue(!System.getenv().containsKey(KimiTui.SHELL_VARIABLE),
                "this test is about the case where the operator said nothing");
        Optional<java.nio.file.Path> bash = GitBash.find();
        assumeTrue(bash.isPresent(), "Git for Windows is not installed on this machine");

        Map<String, String> environment = new HashMap<>();
        KimiTui.applyShell(environment);

        assertEquals(bash.get().toString(), environment.get(KimiTui.SHELL_VARIABLE));
        String normalized = environment.get(KimiTui.SHELL_VARIABLE)
                .toLowerCase(java.util.Locale.ROOT).replace('\\', '/');
        assertFalse(normalized.contains("/windows/system32/"), environment.toString());
    }

    @Test
    void anOperatorsOwnShellIsNotOverridden() {
        Map<String, String> environment = new HashMap<>();
        environment.put(KimiTui.SHELL_VARIABLE, "C:\\somewhere\\bash.exe");
        KimiTui.applyShell(environment);
        assertEquals("C:\\somewhere\\bash.exe", environment.get(KimiTui.SHELL_VARIABLE));
    }

    @Test
    void nothingIsInventedOnOtherPlatforms() {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "Windows has the hint");
        Map<String, String> environment = new HashMap<>();
        KimiTui.applyShell(environment);
        assertTrue(environment.isEmpty(), environment.toString());
    }
}
