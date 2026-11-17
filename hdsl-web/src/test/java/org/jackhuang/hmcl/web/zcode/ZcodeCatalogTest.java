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
package org.jackhuang.hmcl.web.zcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZcodeCatalogTest {

    private static ZcodeInstance instance(String version, String minVersion, long minAt, int format) {
        return new ZcodeInstance("id", "ZCode", "/tmp/ws", null, null, "token", 0,
                version, minVersion, minAt, format, 0L);
    }

    private static ZcodeCatalog.Entry entry(String ref, String version, String channel,
                                            long publishedAt, int format) {
        return new ZcodeCatalog.Entry(ref, version, channel, null, publishedAt, format,
                format >= 2, true, "installed", false, "");
    }

    @Test
    void credentialBoundaryStartsAt318() {
        assertEquals(1, ZcodeCatalog.formatOf("v3.17.2"));
        assertEquals(2, ZcodeCatalog.formatOf("v3.18.0"));
        assertEquals(2, ZcodeCatalog.formatOf("main"));
    }

    @Test
    void blocksLowerCredentialFormat() {
        ZcodeInstance instance = instance("v3.18.2", "v3.18.2", 1000, 2);
        String reason = ZcodeCatalog.downgradeReason(instance,
                entry("v3.17.2", "3.17.2", "release", 900, 1));
        assertNotNull(reason);
    }

    @Test
    void blocksLowerSemver() {
        ZcodeInstance instance = instance("v3.17.2", "v3.17.2", 1000, 1);
        String reason = ZcodeCatalog.downgradeReason(instance,
                entry("v3.17.1", "3.17.1", "release", 900, 1));
        assertNotNull(reason);
        assertNull(ZcodeCatalog.downgradeReason(instance,
                entry("v3.17.2", "3.17.2", "release", 1000, 1)));
    }

    @Test
    void mainFloorBlocksOlderReleasesAndUnknownTagDates() {
        ZcodeInstance instance = instance("main", "main", 1000, 2);
        assertNotNull(ZcodeCatalog.downgradeReason(instance,
                entry("v3.17.2", "3.17.2", "release", 900, 1)));
        assertNotNull(ZcodeCatalog.downgradeReason(instance,
                entry("v3.15.1", "3.15.1", "tag", 0, 1)));
        assertNull(ZcodeCatalog.downgradeReason(instance,
                entry("v3.18.1", "3.18.1", "release", 1100, 2)));
    }
}
