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
package org.jackhuang.hmcl.web.developer;

import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.config.DeveloperGuardScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Boots a real panel and checks that the guard is actually wired — the filter
/// has to be on `/api/*` and `/i/*`, after the session gate, or the policy that
/// [DeveloperGuardFilterTest] pins would never run.
///
/// The panel in these tests is plain HTTP, and developer mode is HTTPS-only
/// (docs/web-shell.md §4.3), so a gated request can never be satisfied: the
/// answer is `tls_required` rather than `developer_elevation_required`. That is
/// still the proof the gate fired — an ungated request reaches the servlet and
/// gets an instance-level answer instead.
class DeveloperGuardGateTest {

    @TempDir
    Path dataDir;

    private TestSupport.RunningServer start(DeveloperGuardScope scope) throws Exception {
        // A developer password has to exist: without one the gate answers
        // developer_password_not_set (409), which is a different assertion.
        org.jackhuang.hmcl.web.auth.UserStore users = org.jackhuang.hmcl.web.auth.UserStore.open(
                dataDir, Map.of("HDSL_ADMIN_PASSWORD", "panel-password"), org.jackhuang.hmcl.util.logging.Logger.LOG);
        DeveloperAuth.open(dataDir).set("developer-password", users);
        return TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
            config.developerGuard.scope = scope;
        });
    }

    private static HttpResponse<String> send(TestSupport.RunningServer running, String method, String path)
            throws Exception {
        HttpClient client = TestSupport.client();
        HttpRequest request = HttpRequest.newBuilder(URI.create(running.baseUrl() + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String error(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject().get("error").getAsString();
    }

    @Test
    void offLeavesTheInstanceApiAlone() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.OFF)) {
            HttpResponse<String> response = send(running, "GET", "/api/instances");
            assertEquals(200, response.statusCode(), response.body());
        }
    }

    @Test
    void proxyGatesTheInstanceWebPages() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.PROXY)) {
            // A browser navigation (`Accept: text/html`) gets the small HTML page
            // rather than a JSON body that would read as a broken page.
            HttpRequest navigation = HttpRequest.newBuilder(URI.create(running.baseUrl() + "/i/whatever/"))
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .build();
            HttpResponse<String> page = TestSupport.client().send(navigation, HttpResponse.BodyHandlers.ofString());
            assertEquals(403, page.statusCode());
            assertTrue(page.headers().firstValue("content-type").orElse("").contains("text/html"),
                    page.headers().toString());
            assertTrue(page.body().contains("HTTPS"), "the page says why the gate cannot be satisfied");

            // The instance's own fetch/XHR gets JSON, and the code names the
            // transport policy (plain HTTP in this test).
            HttpResponse<String> api = send(running, "GET", "/i/whatever/api/foo");
            assertEquals(403, api.statusCode());
            assertTrue(error(api).startsWith("tls_required"), error(api));

            // The instance list is not the proxy: it keeps working.
            assertEquals(200, send(running, "GET", "/api/instances").statusCode());
        }
    }

    @Test
    void instanceGatesTheMutationVerbsOnly() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.INSTANCE)) {
            HttpResponse<String> write = send(running, "POST", "/api/instances/no-such-instance/start");
            assertEquals(403, write.statusCode());
            assertTrue(error(write).startsWith("tls_required"), error(write));
            // Reads stay open, which is what keeps the list usable behind the gate.
            assertEquals(200, send(running, "GET", "/api/instances").statusCode());
        }
    }

    @Test
    void allGatesEvenTheReadsButNotTheDeveloperDoor() throws Exception {
        try (TestSupport.RunningServer running = start(DeveloperGuardScope.ALL)) {
            HttpResponse<String> list = send(running, "GET", "/api/instances");
            assertEquals(403, list.statusCode());
            // Without this the panel could not explain itself or be unlocked.
            assertEquals(200, send(running, "GET", "/api/developer/status").statusCode());
            assertEquals(200, send(running, "GET", "/api/health").statusCode());
        }
    }

    @Test
    void theGatePageCarriesAPasswordFormOnlyWhenElevationIsWhatIsMissing() {
        // The case a user hits by clicking through to an instance: the guard
        // wants the developer password, so the page asks for it right there
        // instead of sending them off to the settings page.
        String elevation = DeveloperGuardFilter.gatePage("/", "需要开发者密码", "Enter the developer password", true);
        assertTrue(elevation.contains("id=\"hdsl-dev-password\""), elevation);
        assertTrue(elevation.contains("/api/developer/elevate"), elevation);

        // A transport refusal is not a password problem: no form, just the reason.
        String transport = DeveloperGuardFilter.gatePage("/", "面板不在 HTTPS 下", "Not on HTTPS", false);
        assertFalse(transport.contains("hdsl-dev-password"), transport);
        assertTrue(transport.contains("返回面板"), transport);

        // The form's fetch target has to carry the mount point, or a sub-path
        // deployment would post to the host root.
        String mounted = DeveloperGuardFilter.gatePage("/hdsl/", "x", "x", true);
        assertTrue(mounted.contains("'/hdsl/api/developer/elevate'"), mounted);
    }
}
