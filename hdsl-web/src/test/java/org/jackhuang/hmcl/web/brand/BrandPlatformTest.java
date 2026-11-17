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
package org.jackhuang.hmcl.web.brand;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which brands can be installed on which platform, for the facts that are about
/// the *upstream package* rather than about the panel.
class BrandPlatformTest {

    /// opencode's npm package is a launcher: the CLI arrives through per-platform
    /// optional dependencies, and upstream publishes none for FreeBSD. The install
    /// therefore fails in `postinstall`; the brand page should say that up front.
    @Test
    void opencodeIsNotInstallableOnFreeBsd() {
        assertFalse(Brand.OPENCODE.installableOn(OperatingSystem.FREEBSD));

        String reason = Brand.OPENCODE.notInstallableReason(OperatingSystem.FREEBSD);
        assertNotNull(reason);
        assertTrue(reason.contains("FreeBSD"), reason);

        assertTrue(Brand.OPENCODE.installableOn(OperatingSystem.LINUX));
        assertTrue(Brand.OPENCODE.installableOn(OperatingSystem.MACOS));
        assertTrue(Brand.OPENCODE.installableOn(OperatingSystem.WINDOWS));
    }

    /// Hermes keeps its Windows refusal, and the npm brands that do run on FreeBSD
    /// (Kimi, Claude Web, the TUI brands) are not swept up by the opencode rule.
    @Test
    void otherBrandsKeepTheirAnswers() {
        assertFalse(Brand.HERMES.installableOn(OperatingSystem.WINDOWS));
        assertTrue(Brand.HERMES.installableOn(OperatingSystem.FREEBSD));

        assertTrue(Brand.KIMI.installableOn(OperatingSystem.FREEBSD));
        assertTrue(Brand.CLAUDE_WEB.installableOn(OperatingSystem.FREEBSD));
        assertTrue(Brand.QWEN.installableOn(OperatingSystem.FREEBSD));
    }
}