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
package org.jackhuang.hmcl.web.plain;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The legacy surface's own rules: sending spends the operator's tokens, so it
/// needs a developer elevation; the fragments carry the revision the poller
/// compares and the elevation flag that kicks it out; the static assets and the
/// two CSP profiles are what an IE11 or a text browser actually receives.
class PlainLegacyTest {

    private static final String PANEL = "legacy-panel-pass";
    private static final String DEV = "legacy-dev-pass";

    @Test
    void sendingStaysClosedUntilTheSessionElevates(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = start(dataDir)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            String sessionId = createSession(client, base, cookie);

            // Reading is open to any logged-in session; the composer is not.
            HttpResponse<String> page = get(client, base + "/plain/s/" + sessionId, cookie);
            assertEquals(200, page.statusCode(), page.body());
            assertTrue(page.body().contains("发送消息需要先"), page.body());
            assertTrue(page.body().contains("data-elev=\"no\""), page.body());

            HttpResponse<String> send = postForm(client, base + "/plain/s/" + sessionId + "/send",
                    "text=" + URLEncoder.encode("hi", StandardCharsets.UTF_8), cookie);
            assertEquals(200, send.statusCode(), send.body());
            assertTrue(send.body().contains("开发者提权"), send.body());
            assertTrue(send.body().contains("name=\"password\""), send.body());

            // Without a developer password there is nothing to elevate with.
            HttpResponse<String> noPassword = postForm(client, base + "/plain/elevate",
                    "password=" + DEV, cookie);
            assertEquals(409, noPassword.statusCode(), noPassword.body());

            setDeveloperPassword(client, base, cookie);

            HttpResponse<String> wrong = postForm(client, base + "/plain/elevate",
                    "password=not-it", cookie);
            HttpResponse<String> hijack = postForm(client, base + "/plain/elevate",
                    "password=" + DEV + "&next="
                            + URLEncoder.encode("https://evil.example/", StandardCharsets.UTF_8), cookie);
            assertEquals(303, hijack.statusCode(), hijack.body());
            assertEquals("/plain", hijack.headers().firstValue("Location").orElseThrow(),
                    "an off-panel next must not become an open redirect");
            assertEquals(200, wrong.statusCode(), wrong.body());
            assertTrue(wrong.body().contains("不对"), wrong.body());

            HttpResponse<String> elevated = postForm(client, base + "/plain/elevate",
                    "password=" + DEV, cookie);
            assertEquals(303, elevated.statusCode(), elevated.body());

            HttpResponse<String> unlocked = get(client, base + "/plain/s/" + sessionId, cookie);
            assertTrue(unlocked.body().contains("data-elev=\"ok\""), unlocked.body());
            assertTrue(unlocked.body().contains("name=\"text\""), unlocked.body());
            // Text browsers ignore the stylesheet: the nav must break lines by
            // markup alone, and submits must be inputs w3m/lynx know how to post.
            assertTrue(unlocked.body().contains("会话</a><br>"), unlocked.body());
            assertTrue(unlocked.body().contains("type=\"submit\" value=\"退出\""), unlocked.body());
            // The legacy surface can also fork and export without the SPA.
            assertTrue(unlocked.body().contains("导出</a>"), unlocked.body());
            assertTrue(unlocked.body().contains("value=\"分叉\""), unlocked.body());
            assertTrue(unlocked.body().contains("id=\"bottom\""), unlocked.body());
            assertTrue(unlocked.body().contains("href=\"#bottom\""), unlocked.body());
            assertTrue(unlocked.headers().firstValue("content-security-policy").orElseThrow()
                    .contains("font-src 'self'"), "KaTeX fonts need font-src on enhanced pages");
            HttpResponse<String> forked = postForm(client,
                    base + "/plain/s/" + sessionId + "/fork", "", cookie);
            assertEquals(303, forked.statusCode(), forked.body());
            String forkedLocation = forked.headers().firstValue("Location").orElseThrow();
            assertTrue(forkedLocation.startsWith("/plain/s/"), forkedLocation);
            assertNotEquals("/plain/s/" + sessionId, forkedLocation);
            assertEquals(200, get(client, base + forkedLocation, cookie).statusCode());
            // Model switching is a plain form: IE11 and w3m both submit selects.
            assertTrue(unlocked.body().contains("name=\"model\""), unlocked.body());
            HttpResponse<String> switched = postForm(client,
                    base + "/plain/s/" + sessionId + "/model", "model=fake-model-2", cookie);
            assertEquals(303, switched.statusCode(), switched.body());
            HttpResponse<String> afterSwitch = get(client, base + "/plain/s/" + sessionId, cookie);
            assertTrue(afterSwitch.body().contains("option value=\"fake-model-2\" selected"),
                    afterSwitch.body());

            // The extension coupon is one-shot: the second stretch needs a fresh
            // password proof.
            assertEquals(303, postForm(client, base + "/plain/elevate/extend", "", cookie).statusCode());
            HttpResponse<String> second = postForm(client, base + "/plain/elevate/extend", "", cookie);
            assertEquals(200, second.statusCode(), second.body());
            assertTrue(second.body().contains("已经延长过一次"), second.body());

            // Stopping the elevation is the kick-out the poller reacts to: the
            // next fragment carries data-elev="no" and the composer is gone.
            assertEquals(303, postForm(client, base + "/plain/elevate/stop", "", cookie).statusCode());
            HttpResponse<String> kicked = get(client, base + "/plain/f/s/" + sessionId, cookie);
            assertEquals(200, kicked.statusCode(), kicked.body());
            assertTrue(kicked.body().contains("data-elev=\"no\""), kicked.body());
            assertTrue(kicked.body().contains("发送消息需要先"), kicked.body());
        }
    }

    @Test
    void fragmentsCarryARevisionAndMoveWhenTheListMoves(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = start(dataDir)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            HttpResponse<String> first = get(client, base + "/plain/f/sessions", cookie);
            assertEquals(200, first.statusCode(), first.body());
            String rev = first.headers().firstValue("X-Rev").orElseThrow();
            assertFalse(rev.isBlank());
            assertTrue(first.body().startsWith("<div id=\"frag-sessions\""), first.body());

            // A held poll answers as soon as the revision moves, not at its deadline.
            AtomicReference<HttpResponse<String>> held = new AtomicReference<>();
            Thread poller = new Thread(() -> {
                try {
                    held.set(get(client, base + "/plain/f/sessions?wait=1&rev=" + rev, cookie));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            poller.start();
            Thread.sleep(300);
            createSession(client, base, cookie);
            poller.join(10_000);
            assertFalse(poller.isAlive(), "the held poll never woke up");
            HttpResponse<String> woken = held.get();
            assertEquals(200, woken.statusCode(), woken.body());
            assertNotEquals(rev, woken.headers().firstValue("X-Rev").orElseThrow());
        }
    }

    @Test
    void theStaticAssetsAndCspProfilesShip(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = start(dataDir)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();

            HttpResponse<String> poll = get(client, base + "/plain/poll.js", null);
            assertEquals(200, poll.statusCode(), poll.body());
            assertTrue(poll.headers().firstValue("content-type").orElseThrow()
                    .contains("javascript"), poll.headers().toString());
            assertTrue(poll.body().contains("XMLHttpRequest"), poll.body());

            HttpResponse<String> css = get(client, base + "/plain/legacy.css", null);
            assertEquals(200, css.statusCode(), css.body());
            assertTrue(css.body().contains(".hljs-keyword"), css.body());

            HttpResponse<String> enhanced = get(client, base + "/plain/old", null);
            String strict = enhanced.headers().firstValue("content-security-policy").orElseThrow();
            assertTrue(strict.contains("default-src 'none'"), strict);
            assertFalse(strict.contains("script-src"), strict);
            assertTrue(strict.contains("img-src 'self'"), strict);
            // The HMCL-style shell: wallpaper behind a two-column table card
            // layout, present on every page of the surface.
            assertTrue(enhanced.body().contains("background-image:url("), enhanced.body());
            assertTrue(enhanced.body().contains("class=\"shell"), enhanced.body());

            // KaTeX's stylesheet and fonts ride in the jar; when the build had
            // npm, the legacy surface serves them next to its own CSS.
            if (PlainChatServlet.class.getResource("/web-legacy/katex/katex.min.css") != null) {
                HttpResponse<String> katex = get(client, base + "/plain/katex/katex.min.css", null);
                assertEquals(200, katex.statusCode(), katex.body().substring(0, Math.min(200, katex.body().length())));
                assertTrue(katex.headers().firstValue("content-type").orElseThrow().contains("text/css"));
                assertTrue(katex.body().contains(".katex"), "must be the KaTeX stylesheet");
            }
        }
    }

    @Test
    void aFinalizedReplyIsPreRenderedOrFallsBack(@TempDir Path dataDir) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/v1/chat/completions", PlainLegacyTest::stream);
        provider.start();
        DshAccount account = new DshAccount(
                DshAccount.AccountKind.THIRD_PARTY, "legacy-fake", "test-key",
                "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1",
                null, "fake-model", null);
        SettingsManager.settings().getAccounts().add(account);
        try (TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(),
                config -> {
                    config.auth.disabled = true;
                    config.bindHost = "127.0.0.1";
                })) {
            HttpResponse<String> created = postForm(TestSupport.client(),
                    running.baseUrl() + "/plain/new", "assistantId=default", null);
            assertEquals(303, created.statusCode(), created.body());
            String sessionId = created.headers().firstValue("Location").orElseThrow()
                    .substring("/plain/s/".length());
            HttpRequest patch = HttpRequest.newBuilder(URI.create(
                            running.baseUrl() + "/api/chat/sessions/" + sessionId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(
                            "{\"account\":\"legacy-fake\",\"model\":\"fake-model\"}", StandardCharsets.UTF_8))
                    .build();
            assertEquals(200, TestSupport.client().send(patch,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode());

            assertEquals(303, postForm(TestSupport.client(),
                    running.baseUrl() + "/plain/s/" + sessionId + "/send",
                    "text=" + URLEncoder.encode("render me", StandardCharsets.UTF_8), null).statusCode());

            boolean ssrShipped = MarkdownSsr.class.getResourceAsStream(
                    "/web-legacy/ssr/render-markdown.cjs") != null
                    && org.jackhuang.hmcl.util.platform.Executables.resolve("node").isPresent();
            // Rendering is asynchronous (turn-finish prefetch): wait for the
            // rendered fragment, not for the raw text that appears first.
            String page = waitFor(TestSupport.client(), running.baseUrl(), sessionId,
                    ssrShipped ? "<strong>render</strong>" : "**render**",
                    Duration.ofSeconds(15));
            if (ssrShipped) {
                assertTrue(page.contains("chat-markdown legacy-md"), page);
                try (var cached = Files.list(dataDir.resolve("plain-md"))) {
                    assertTrue(cached.anyMatch(path -> path.getFileName().toString().endsWith(".html")),
                            "a rendered fragment should have been cached");
                }
            } else {
                assertTrue(page.contains("<pre class=\"msg\">"), page);
            }
        } finally {
            SettingsManager.settings().getAccounts().remove(account);
            provider.stop(0);
        }
    }

    @Test
    void theGeneralSettingsSwitchDegradesTheSurface(@TempDir Path dataDir) throws Exception {
        try (TestSupport.RunningServer running = start(dataDir)) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = login(client, base);

            HttpResponse<String> initial = get(client, base + "/api/settings/plain", cookie);
            assertEquals(200, initial.statusCode(), initial.body());
            assertTrue(initial.body().contains("\"enhanced\":true"), initial.body());
            assertTrue(initial.body().contains("\"ssrAvailable\":"), initial.body());

            // Flipping the surface an operator's old browsers rely on is an
            // operator decision: the password proof comes first.
            HttpResponse<String> denied = putJson(client, base + "/api/settings/plain",
                    "{\"enhanced\":false}", cookie);
            assertEquals(403, denied.statusCode(), denied.body());

            setDeveloperPassword(client, base, cookie);
            assertEquals(303, postForm(client, base + "/plain/elevate",
                    "password=" + DEV, cookie).statusCode());
            HttpResponse<String> off = putJson(client, base + "/api/settings/plain",
                    "{\"enhanced\":false}", cookie);
            assertEquals(200, off.statusCode(), off.body());
            assertTrue(Files.readString(dataDir.resolve("server.yaml")).contains("enhanced"),
                    "the switch must survive a restart");

            // Degraded mode: no poller, strict CSP, no fragments.
            HttpResponse<String> home = get(client, base + "/plain", cookie);
            assertEquals(200, home.statusCode(), home.body());
            assertFalse(home.body().contains("poll.js"), home.body());
            assertFalse(home.headers().firstValue("content-security-policy").orElseThrow()
                    .contains("script-src"));
            assertEquals(404, get(client, base + "/plain/f/sessions", cookie).statusCode());

            // And back on, without a restart.
            assertEquals(200, putJson(client, base + "/api/settings/plain",
                    "{\"enhanced\":true}", cookie).statusCode());
            HttpResponse<String> back = get(client, base + "/plain", cookie);
            assertTrue(back.body().contains("poll.js"), back.body());
        }
    }

    // ---------------------------------------------------------------- helpers --

    private static TestSupport.RunningServer start(Path dataDir) throws Exception {
        return TestSupport.start(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL),
                config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                });
    }

    private static String login(HttpClient client, String base) throws Exception {
        HttpResponse<String> response = postForm(client, base + "/plain/login",
                "username=admin&password=" + PANEL, null);
        assertEquals(303, response.statusCode(), response.body());
        return response.headers().firstValue("set-cookie").orElseThrow().split(";")[0];
    }

    private static void setDeveloperPassword(HttpClient client, String base, String cookie)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/developer/password"))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"adminPassword\":\"" + PANEL + "\",\"password\":\"" + DEV
                                + "\",\"confirm\":\"" + DEV + "\"}", StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), response.body());
    }

    private static String createSession(HttpClient client, String base, String cookie)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/chat/sessions"))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), response.body());
        return com.google.gson.JsonParser.parseString(response.body())
                .getAsJsonObject().get("id").getAsString();
    }

    private static void stream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (var output = exchange.getResponseBody()) {
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"**render** me\"},"
                    + "\"finish_reason\":null}]}\n\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.write(("data: {\"choices\":[{\"delta\":{\"content\":\"\"},\"finish_reason\":\"stop\"}],"
                    + "\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":2}}\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write("data: [DONE]\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static String waitFor(HttpClient client, String base, String sessionId,
                                  String needle, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String last = "";
        while (System.nanoTime() < deadline) {
            last = get(client, base + "/plain/s/" + sessionId, null).body();
            if (last.contains(needle)) {
                return last;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("the page never contained " + needle + "; last page: " + last);
    }

    private static HttpResponse<String> get(HttpClient client, String url, @org.jetbrains.annotations.Nullable String cookie)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putJson(HttpClient client, String url, String body,
                                                String cookie) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postForm(HttpClient client, String url, String body,
                                                 @org.jetbrains.annotations.Nullable String cookie)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
