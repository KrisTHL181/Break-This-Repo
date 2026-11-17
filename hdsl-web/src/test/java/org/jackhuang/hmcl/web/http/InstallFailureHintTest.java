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
package org.jackhuang.hmcl.web.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The advice [InstancesApiServlet] puts in front of pnpm's raw output. The two
/// endings a retry cannot fix have to say so — the panel shows exactly this
/// text in its "not installed" card, so it is the only place the user learns
/// that the version, not the machine, is what is broken.
class InstallFailureHintTest {

    /// The ending a version with an unpublished dependency produces — the case
    /// that prompted the hint: `dsh 0.0.1-rc.1` depends on packages the registry
    /// no longer has, so it can never be installed again.
    @Test
    void missingPackageNamesThePackageAndSaysRetryingIsPointless() {
        String output = """
                Installing DSH 0.0.1-rc.1 for dsh-0.0.1-rc.1 failed: pnpm exited with code 1 while installing 0.0.1-rc.1 for dsh-0.0.1-rc.1:
                [ERR_PNPM_FETCH_404] GET https://registry.npmmirror.com/@deepseek-ai%2Fdsh-compact-tool-result-prune: Not Found - 404
                @deepseek-ai/dsh-compact-tool-result-prune is not in the npm registry, or you have no permission to fetch it.
                0 of 43 packages written (38 downloaded, 5 reused)""";
        String hint = InstancesApiServlet.registryHint(output);
        assertTrue(hint.contains("@deepseek-ai/dsh-compact-tool-result-prune"), hint);
        assertTrue(hint.contains("请换一个版本"), hint);
        assertTrue(hint.contains("重试无用"), hint);
    }

    /// The other ending that reads as "try again" and is not: the companion
    /// package for that harness version was never published.
    @Test
    void unsatisfiableRequirementSaysToPickAnotherVersion() {
        String hint = InstancesApiServlet.registryHint(
                "ERR_PNPM_NO_MATCHING_VERSION  No matching version found for @deepseek-ai/dsh-web-app@0.0.1-rc.1");
        assertTrue(hint.contains("请换一个版本"), hint);
    }

    /// Everything else keeps pnpm's own words, with nothing added.
    @Test
    void ordinaryFailuresGetNoHint() {
        assertEquals("", InstancesApiServlet.registryHint(
                "pnpm exited with code 1 while installing 0.1.7-rc.1 for work: ERR_PNPM_META_FETCH_FAIL"));
        assertEquals("", InstancesApiServlet.registryHint(""));
    }
}
