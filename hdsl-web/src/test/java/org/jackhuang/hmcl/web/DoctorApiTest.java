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
package org.jackhuang.hmcl.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The diagnostics endpoint: the domain's plain-text doctor report, wrapped
/// as the structured checks the panel renders. The network-dependent rows
/// (the npm registry reachability) are not asserted on their status — the
/// shape and the section coverage are what the contract fixes.
class DoctorApiTest {

    @TempDir
    Path dataDir;

    private static final Set<String> STATUSES = Set.of("ok", "warn", "fail");

    @Test
    void doctorReportIsStructuredIntoChecks() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        })) {
            HttpClient client = TestSupport.client();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(running.baseUrl() + "/api/doctor")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            assertNotNull(body.get("generatedAt"), "the report carries its timestamp");
            JsonArray checks = body.getAsJsonArray("checks");
            assertTrue(checks.size() >= 5, "the report's sections are all checks: " + checks);

            java.util.Set<String> ids = new java.util.HashSet<>();
            for (int i = 0; i < checks.size(); i++) {
                JsonObject check = checks.get(i).getAsJsonObject();
                ids.add(check.get("id").getAsString());
                assertTrue(check.has("name"), "every check is named: " + check);
                assertTrue(STATUSES.contains(check.get("status").getAsString()),
                        "status is one of ok/warn/fail: " + check);
            }
            // The report's own sections, plus the overall verdict.
            assertTrue(ids.contains("runtime"), ids::toString);
            assertTrue(ids.contains("language"), ids::toString);
            assertTrue(ids.contains("settings"), ids::toString);
            assertTrue(ids.contains("directories"), ids::toString);
            assertTrue(ids.contains("javascript-toolchain"), ids::toString);
            assertTrue(ids.contains("file-watchers"), ids::toString);
            assertTrue(ids.contains("npm-registry"), ids::toString);
            assertTrue(ids.contains("overall"), ids::toString);
        }
    }
}
