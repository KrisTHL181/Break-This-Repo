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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The instance `.env`: the panel owns three keys, the operator owns the rest.
///
/// Hermes reads provider credentials from `<HERMES_HOME>/.env` (its "密钥" page
/// and `hermes auth add` write there). A launch that rewrote the file with only
/// the API keys therefore wiped them, and the chat answered "No usable
/// credentials found for provider 'deepseek'. Set DEEPSEEK_API_KEY." — measured
/// 2026-11 on the production instance right after its first launch.
class HermesEnvFileTest {

    @Test
    void aProvidersKeySurvivesALaunch(@TempDir Path home) throws IOException {
        Files.writeString(home.resolve(".env"),
                "# set from the dashboard\nDEEPSEEK_API_KEY=sk-operator-value\n"
                        + "API_SERVER_KEY=stale\nAPI_SERVER_PORT=1\n"
                        + "OTHER_KEY=kept\n",
                StandardCharsets.UTF_8);

        BrandRuntime.writeHermesEnv(home, "fresh-key", 4242);

        String text = Files.readString(home.resolve(".env"), StandardCharsets.UTF_8);
        assertTrue(text.contains("DEEPSEEK_API_KEY=sk-operator-value"), text);
        assertTrue(text.contains("# set from the dashboard"), text);
        assertTrue(text.contains("OTHER_KEY=kept"), text);
        assertTrue(text.contains("API_SERVER_KEY=fresh-key"), text);
        assertTrue(text.contains("API_SERVER_HOST=127.0.0.1"), text);
        assertTrue(text.contains("API_SERVER_PORT=4242"), text);
        assertFalse(text.contains("API_SERVER_KEY=stale"), text);
        assertFalse(text.contains("API_SERVER_PORT=1\n"), text);
        assertEquals(1, occurrences(text, "API_SERVER_KEY="), text);
        assertEquals(1, occurrences(text, "API_SERVER_PORT="), text);
    }

    @Test
    void aFreshHomeGetsExactlyTheManagedKeys(@TempDir Path home) throws IOException {
        BrandRuntime.writeHermesEnv(home, "k", 1);

        assertEquals("API_SERVER_KEY=k\nAPI_SERVER_HOST=127.0.0.1\nAPI_SERVER_PORT=1\n",
                Files.readString(home.resolve(".env"), StandardCharsets.UTF_8));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }
}
