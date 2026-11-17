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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The disk cache is what keeps a restart from spending the GitHub quota, and
/// it must be readable without a network round trip.
class BrandCatalogCacheTest {

    @TempDir
    Path dataDir;

    @BeforeEach
    void clear() {
        BrandCatalog.clearForTests();
        GitHubToken.clearForTests();
    }

    @AfterEach
    void cleanup() {
        BrandCatalog.clearForTests();
        GitHubToken.clearForTests();
    }

    private void writeCache(String json) throws IOException {
        Path file = dataDir.resolve("cache").resolve("brand-catalog.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    @Test
    void aFreshDiskEntryIsRenderedWithoutTheNetwork() throws IOException {
        long now = System.currentTimeMillis();
        writeCache("""
                {"kimi":{"etag":"","versions":["2.1.1","2.0.2"],"latest":"2.1.1",
                 "fetchedAt":%d,"checkedAt":%d,"limit":-1,"remaining":-1,"resetAt":0,
                 "failures":0,"backoffUntil":0,"lastAttemptAt":0,"rateLimitedUntil":0}}"""
                .formatted(now, now));

        BrandCatalog.bind(dataDir);

        assertEquals(List.of("2.1.1", "2.0.2"), BrandCatalog.versions(Brand.KIMI));
        assertEquals("2.1.1", BrandCatalog.latest(Brand.KIMI));
        assertEquals("disk", BrandCatalog.status(Brand.KIMI).source());
        assertEquals(now, BrandCatalog.status(Brand.KIMI).fetchedAt());
    }

    @Test
    void aCorruptCacheIsIgnoredInsteadOfFailingStartup() throws IOException {
        writeCache("{ not json");
        BrandCatalog.bind(dataDir);
        // No exception; the brand simply has no known versions.
        assertTrue(BrandCatalog.status(Brand.KIMI).fetchedAt() == 0);
    }

    @Test
    void idleRefreshWaitsThreeHoursAndStaysQuietWhileSomeoneIsUsingThePanel() throws IOException {
        long now = System.currentTimeMillis();
        long fourHoursAgo = now - 4 * 60 * 60 * 1000L;
        writeCache("""
                {"claude-web":{"etag":"","versions":["1.0.0"],"latest":"1.0.0",
                 "fetchedAt":%d,"checkedAt":%d,"limit":60,"remaining":50,"resetAt":0,
                 "failures":0,"backoffUntil":0,"lastAttemptAt":0,"rateLimitedUntil":0}}"""
                .formatted(fourHoursAgo, fourHoursAgo));

        BrandCatalog.bind(dataDir);

        assertTrue(BrandCatalog.dueForIdleRefresh(Brand.CLAUDE_WEB, now),
                "an entry older than three hours is due when nobody is around");
        BrandCatalog.noteUserAccess();
        assertFalse(BrandCatalog.dueForIdleRefresh(Brand.CLAUDE_WEB, now),
                "recent brand-surface activity hands refresh back to the on-access 1-hour TTL");
        assertFalse(BrandCatalog.dueForIdleRefresh(Brand.KIMI, now),
                "npm brands never use the GitHub idle loop");
    }

    @Test
    void aStoredTokenIsReadButNeverReturnedAsPartOfStatus() throws IOException {
        Path auth = dataDir.resolve("auth").resolve("github.json");
        Files.createDirectories(auth.getParent());
        Files.writeString(auth, "{\"token\":\"ghp_secret\",\"login\":\"octo\"}\n",
                StandardCharsets.UTF_8);

        GitHubToken.bind(dataDir);

        GitHubToken.Status status = GitHubToken.status();
        assertTrue(status.configured());
        assertEquals("octo", status.login());
        assertEquals("ghp_secret", GitHubToken.get());
        assertFalse(status.toString().contains("ghp_secret"),
                "the status record must not carry the token");
    }
}
