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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A release's own `engines.node`, read from the package that was installed.
class BrandNodeRequirementTest {

    @Test
    void readsTheDeclaration(@TempDir Path dataDir) throws Exception {
        Path packageJson = dataDir.resolve("brands/hermes/releases/0.22.7/package.json");
        Files.createDirectories(packageJson.getParent());
        Files.writeString(packageJson, "{\"engines\":{\"node\":\">=26\"}}");

        assertEquals(">=26", BrandNodeRequirement.of(dataDir, Brand.HERMES, "0.22.7"));
    }

    @Test
    void aReleaseWithoutADeclarationIsNull(@TempDir Path dataDir) throws Exception {
        Path packageJson = dataDir.resolve("brands/kimi/releases/2.3.2/package.json");
        Files.createDirectories(packageJson.getParent());
        Files.writeString(packageJson, "{\"name\":\"kimi-code\"}");

        assertNull(BrandNodeRequirement.of(dataDir, Brand.KIMI, "2.3.2"));
        assertNull(BrandNodeRequirement.of(dataDir, Brand.KIMI, "9.9.9"));
    }
}
