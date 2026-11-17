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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The brand download sources: the addresses this launcher is willing to hand to `npm`/`uv` in a
/// child process, and the mirrors it offers.
///
/// The refusals are the point: these values land in a process environment, so a newline, a quote or
/// a shell metacharacter is a value nobody meant to type.
class BrandSourcesTest {

    @Test
    void anAddressLosesItsTrailingSlash() throws Exception {
        assertEquals("https://mirrors.aliyun.com/pypi/simple",
                BrandSources.normalize("https://mirrors.aliyun.com/pypi/simple///"));
        assertEquals("https://mirrors.aliyun.com/pypi/simple",
                BrandSources.normalize("  https://mirrors.aliyun.com/pypi/simple  "));
    }

    @Test
    void addressesThatCouldInjectASecondSettingAreRefused() {
        List<String> refused = new ArrayList<>(List.of(
                "",
                "   ",
                "ftp://mirrors.example.com/pypi",
                "https://mirrors.example.com/pypi simple",
                "https://mirrors.example.com/pypi\nUV_INDEX_URL=https://evil",
                "https://mirrors.example.com/pypi\"",
                "https://mirrors.example.com/pypi`whoami`",
                "https://mirrors.example.com/pypi;rm",
                "https://mirrors.example.com/pypi?x=1",
                "https://mirrors.example.com/pypi#frag",
                "https://user:pass@mirrors.example.com/pypi",
                "https://mirrors.example.com/pypi@evil",
                "https://",
                "https://mirrors.example.com/" + "a".repeat(200)));
        for (String value : refused) {
            assertThrows(BrandSources.Invalid.class, () -> BrandSources.normalize(value),
                    "should have been refused: " + value);
        }
        assertFalse(BrandSources.isValid("https://mirrors.example.com/pi\u0007py"));
        assertTrue(BrandSources.isValid("https://mirrors.example.com/pypi"));
    }

    @Test
    void theOfferedMirrorsAreTheOnesThatWereMeasured() {
        List<String> pypi = new ArrayList<>();
        for (BrandSources.Preset preset : BrandSources.pypiPresets()) {
            pypi.add(preset.id());
            assertFalse(preset.label().isBlank(), preset.toString());
            assertFalse(preset.note().isBlank(), preset.toString());
        }
        assertTrue(pypi.containsAll(List.of(BrandSources.ENVIRONMENT, "official", "aliyun", "tuna", "ustc")),
                pypi.toString());

        List<String> python = new ArrayList<>();
        for (BrandSources.Preset preset : BrandSources.pythonPresets()) {
            python.add(preset.id());
        }
        assertTrue(python.containsAll(List.of(BrandSources.ENVIRONMENT, "github", "nju")), python.toString());
    }

    @Test
    void lookupIgnoresCustomAndEnvironment() {
        assertNull(BrandSources.preset(BrandSources.pypiPresets(), BrandSources.CUSTOM));
        assertNull(BrandSources.preset(BrandSources.pypiPresets(), BrandSources.ENVIRONMENT),
                "environment names no address of its own");
        assertNull(BrandSources.preset(BrandSources.pypiPresets(), "nope"));
        assertNotNull(BrandSources.preset(BrandSources.pypiPresets(), "aliyun"));
    }

    @Test
    void aBrandsReleaseBaseIsItsGitHubReleases() {
        assertEquals("https://github.com/TencentCloud/Octop/releases/download",
                BrandSources.defaultReleaseBase(Brand.OCTOP));
        assertEquals("", BrandSources.defaultReleaseBase(Brand.KIMI),
                "an npm brand has no release assets");

        List<BrandSources.Preset> octop = BrandSources.releasePresets(Brand.OCTOP);
        List<String> ids = new ArrayList<>();
        for (BrandSources.Preset preset : octop) {
            ids.add(preset.id());
        }
        assertEquals(List.of(BrandSources.ENVIRONMENT, "github"), ids);
        assertTrue(octop.get(1).url().contains("TencentCloud/Octop"), octop.get(1).toString());
    }

    @Test
    void theDefaultsAreTheUpstreams() {
        assertTrue(BrandSources.PYPI_DEFAULT.startsWith("https://pypi.org"), BrandSources.PYPI_DEFAULT);
        assertTrue(BrandSources.PYTHON_DEFAULT.contains("python-build-standalone"), BrandSources.PYTHON_DEFAULT);
        assertEquals("https://codeload.github.com", BrandSources.ARCHIVE_DEFAULT);
    }

    @Test
    void theArchiveSourcesAreTheOnesAFastNetworkNeeds() {
        List<String> ids = new ArrayList<>();
        for (BrandSources.Preset preset : BrandSources.archivePresets()) {
            ids.add(preset.id());
            assertFalse(preset.label().isBlank(), preset.toString());
            assertFalse(preset.note().isBlank(), preset.toString());
        }
        assertEquals(List.of(BrandSources.ENVIRONMENT, "codeload"), ids);
        assertNull(BrandSources.preset(BrandSources.archivePresets(), BrandSources.CUSTOM));
    }
}