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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.auth.UserStore;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The agent credential: a bearer token the agent mints with the agent
/// password, and the two walls that keep it from doing what no agent should do.
///
/// The token has no browser behind it, so it has to pass the session gate on
/// its own — which is exactly why every rule here lives in the panel's root
/// process and not in the plugin: an agent can rewrite the plugin.
class AgentGrantApiTest {

    private static final String PANEL_PASSWORD = "panel-password-1";
    private static final String DEV_PASSWORD = "dev-password-1";
    private static final String AGENT_PASSWORD = "agent-password-1";

    @TempDir
    Path dataDir;

    @Test
    void aGrantReachesOnlyWhatItWasGivenAndCanBeRevoked() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            // A read grant: the agent exchanges the agent password for it.
            JsonObject mint = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\"],\"instanceId\":\"inst-a\",\"ttlSeconds\":600}");
            String token = mint.get("token").getAsString();
            String id = mint.get("id").getAsString();
            assertTrue(mint.get("scopes").toString().contains("read"));

            // Reads work with no cookie at all — that is the point of the token.
            assertEquals(200, getWithBearer(client, base + "/api/instances", token).statusCode());

            // Writes do not: the grant never named the class.
            HttpResponse<String> launch = post(client, base + "/api/instances/inst-a/launch", "{}", null, token);
            assertEquals(403, launch.statusCode(), launch.body());
            assertTrue(launch.body().contains("agent_scope_denied"));

            // The credential routes are closed to agents outright, whatever
            // they were granted: a token that can mint tokens is a token that
            // survives its own revocation.
            HttpResponse<String> grants = getWithBearer(client, base + "/api/developer/agent-grants", token);
            assertEquals(403, grants.statusCode(), grants.body());

            // The operator sees it, and sees no token in the listing.
            HttpResponse<String> listed = get(client, base + "/api/developer/agent-grants", cookie);
            assertEquals(200, listed.statusCode(), listed.body());
            assertTrue(listed.body().contains("\"label\":\"probe\""), listed.body());
            assertFalse(listed.body().contains(token), "the listing must never carry the token");

            HttpResponse<String> revoked = delete(client, base + "/api/developer/agent-grants/" + id, cookie);
            assertEquals(200, revoked.statusCode(), revoked.body());

            // Revocation is immediate: the next request is just unauthenticated.
            assertEquals(401, getWithBearer(client, base + "/api/instances", token).statusCode());
            assertEquals(404, delete(client, base + "/api/developer/agent-grants/" + id, cookie).statusCode());
        }
    }

    @Test
    void anAgentCannotStopItsOwnInstance() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\",\"instances\"],\"instanceId\":\"inst-a\"}")
                    .get("token").getAsString();

            // The scope allows stopping instances; the instance it is bound to
            // is the one it may not touch. The call would kill the process
            // serving it, so the answer is a refusal, not a promise.
            HttpResponse<String> suicide = post(client, base + "/api/instances/inst-a/stop", "{}", null, token);
            assertEquals(409, suicide.statusCode(), suicide.body());
            assertTrue(suicide.body().contains("self_operation_refused"));

            // Another instance is ordinary work.
            HttpResponse<String> other = post(client, base + "/api/instances/inst-b/stop", "{}", null, token);
            assertTrue(other.statusCode() != 409 && other.statusCode() != 403,
                    "stopping another instance must pass the gate, got " + other.statusCode() + " " + other.body());
        }
    }

    /// An unbound token is an unidentified caller: the fatal operations are
    /// closed in **every** family, not just the dsh shape. The 0.6.60 hole was
    /// exactly here — `/api/brands/…` and `/api/zcode/…` went straight through
    /// and ended the instance the caller was running in.
    @Test
    void anUnboundGrantMayNotStopAnythingInAnyFamily() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            // No instanceId, and this test's JVM is nobody's instance: the
            // panel cannot place the caller, so nothing fatal is answered.
            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\",\"instances\"]}")
                    .get("token").getAsString();

            HttpResponse<String> dsh = post(client, base + "/api/instances/inst-a/stop", "{}", null, token);
            assertEquals(409, dsh.statusCode(), dsh.body());
            assertTrue(dsh.body().contains("self_operation_refused"), dsh.body());
            assertTrue(dsh.body().contains("cannot identify"), dsh.body());

            HttpResponse<String> brand = post(client, base + "/api/brands/kimi/instances/inst-a/stop", "{}", null, token);
            assertEquals(409, brand.statusCode(), brand.body());
            assertTrue(brand.body().contains("self_operation_refused"), brand.body());

            HttpResponse<String> zcode = post(client, base + "/api/zcode/instances/inst-a/stop", "{}", null, token);
            assertEquals(409, zcode.statusCode(), zcode.body());

            HttpResponse<String> deleted = deleteWithBearer(client,
                    base + "/api/brands/kimi/instances/inst-a", token);
            assertEquals(409, deleted.statusCode(), deleted.body());

            HttpResponse<String> terminal = post(client, base + "/api/zcode/instances/inst-a/terminal", "{}", null, token);
            assertEquals(409, terminal.statusCode(), terminal.body());

            // Launching a stopped instance ends nobody inside it: the gate
            // steps aside and the route answers for itself.
            HttpResponse<String> launch = post(client, base + "/api/brands/kimi/instances/inst-a/launch",
                    "{}", null, token);
            assertTrue(launch.statusCode() != 409, launch.body());
        }
    }

    /// A binding the caller claimed at mint time still guards the other two
    /// families: the brand and ZCode shapes answered to no binding in 0.6.60.
    @Test
    void aClaimedBindingGuardsTheBrandAndZcodeShapes() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\",\"instances\"],\"instanceId\":\"inst-a\"}")
                    .get("token").getAsString();

            HttpResponse<String> brand = post(client, base + "/api/brands/kimi/instances/inst-a/stop", "{}", null, token);
            assertEquals(409, brand.statusCode(), brand.body());
            assertTrue(brand.body().contains("self_operation_refused"), brand.body());

            HttpResponse<String> zcode = post(client, base + "/api/zcode/instances/inst-a/terminal", "{}", null, token);
            assertEquals(409, zcode.statusCode(), zcode.body());

            // The same operation on another instance is ordinary work.
            HttpResponse<String> other = post(client, base + "/api/brands/kimi/instances/inst-b/stop",
                    "{}", null, token);
            assertTrue(other.statusCode() != 409 && other.statusCode() != 403,
                    "stopping another brand instance must pass the gate, got "
                            + other.statusCode() + " " + other.body());
        }
    }

    @Test
    void theGrantCannotClaimAScopeThisBuildDoesNotKnow() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());
            HttpResponse<String> refused = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"label\":\"probe\",\"scopes\":[\"root\"]}", null);
            assertEquals(400, refused.statusCode(), refused.body());
            assertTrue(refused.body().contains("unknown_scope"));
        }
    }


    @Test
    void aScopedAgentAlsoSatisfiesTheRoutesOwnElevationCheck() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\",\"mc\"]}")
                    .get("token").getAsString();

            // `console/input` calls requireElevation itself. An agent never has
            // the developer password, so without [AuthFilter#isAgent] the route
            // answers "elevate first" — an instruction it can never follow — and
            // the `mc` scope would be worth almost nothing.
            HttpResponse<String> console = post(client, base + "/api/mc/instances/nope/console/input",
                    "{\"line\":\"list\"}", null, token);
            assertTrue(console.statusCode() != 403 || !console.body().contains("developer_elevation_required"),
                    "an agent must not be asked for the developer password: " + console.body());
        }
    }

    /// 这张票能读面板，但读不到两样东西：浏览器通道，和凭据本身。
    @Test
    void theProxyAndAGeneratedCredentialStayOutOfReach() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\",\"instances\"]}")
                    .get("token").getAsString();

            // `/i/*` is the browser's proxy into an instance, not an API. A
            // `read` grant must not turn it into "a GET, so allowed".
            HttpResponse<String> proxy = getWithBearer(client, base + "/i/whatever/", token);
            assertEquals(403, proxy.statusCode(), proxy.body());
            assertTrue(proxy.body().contains("agent_scope_denied"), proxy.body());

            // Octop's generated admin password is a credential. The first-run
            // window returns it without elevation, so only the scope gate can
            // keep it out of a model's context.
            HttpResponse<String> credential = getWithBearer(client,
                    base + "/api/brands/octop/instances/nope/admin-password", token);
            assertEquals(403, credential.statusCode(), credential.body());
            assertTrue(credential.body().contains("agent_scope_denied"), credential.body());

            // The gate must read the same decoded path the servlet routes on.
            // `%61dmin-password` is not that name in the raw URI, but the
            // dispatcher decodes it back into the closed route.
            HttpResponse<String> encoded = getWithBearer(client,
                    base + "/api/brands/octop/instances/nope/%61dmin-password", token);
            assertEquals(403, encoded.statusCode(), encoded.body());
            assertTrue(encoded.body().contains("agent_scope_denied"), encoded.body());
        }
    }

    /// 票是借来的权力：出借人走了（登出），票就不能再用。
    @Test
    void loggingOutEndsTheGrantItAuthorized() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String token = mint(client, base, cookie,
                    "\"label\":\"probe\",\"scopes\":[\"read\"],\"ttlSeconds\":3600}")
                    .get("token").getAsString();
            assertEquals(200, getWithBearer(client, base + "/api/instances", token).statusCode());

            assertEquals(200, post(client, base + "/api/auth/logout", "{}", cookie).statusCode());

            // The grant's own expiry is an hour away; the window that lent it
            // the authority is gone, so the next call is just unauthenticated.
            assertEquals(401, getWithBearer(client, base + "/api/instances", token).statusCode());
        }
    }

    @Test
    void scopesAreClassifiedHereAndDenyByDefault() {
        // Reads are reads; the credential-leaking ones are closed.
        assertEquals(AgentScopes.READ, AgentScopes.required("GET", "/api/instances"));
        assertTrue(AgentScopes.forbidden("GET", "/api/instances/inst-a/open"));
        assertTrue(AgentScopes.forbidden("GET", "/api/exports/x.dspack"));
        assertTrue(AgentScopes.forbidden("POST", "/api/developer/agent-auth"));
        assertTrue(AgentScopes.forbidden("POST", "/api/auth/password"));
        // A generated brand admin password is a credential, and the first-run
        // window answers it with no elevation at all — the gateway has to close
        // it, because a servlet-side check never runs.
        assertTrue(AgentScopes.forbidden("GET", "/api/brands/octop/instances/x/admin-password"));
        assertTrue(AgentScopes.forbidden("POST", "/api/brands/octop/instances/x/admin-password"));
        // A trailing slash is the same route: the dispatcher ignores it, so the
        // gate must too.
        assertTrue(AgentScopes.forbidden("GET", "/api/brands/octop/instances/x/admin-password/"));

        // The scope model stops at the API boundary: the reverse proxy and the
        // event streams are browser channels, never "a GET, so a read".
        assertNull(AgentScopes.required("GET", "/i/inst-a/"));
        assertNull(AgentScopes.required("GET", "/ws/events"));
        assertEquals(AgentScopes.READ, AgentScopes.required("GET", "/api/health"));

        // Writes land in their own classes.
        assertEquals(AgentScopes.MC, AgentScopes.required("POST", "/api/mc/instances/x/start"));
        // The console's PTY ticket is its own class: `mc` alone keeps the old
        // reach (start/stop, one-line input, logs) and gains no attach.
        assertEquals(AgentScopes.MC_CONSOLE,
                AgentScopes.required("POST", "/api/mc/instances/x/console/ticket"));
        assertEquals(AgentScopes.MC_CONSOLE,
                AgentScopes.required("POST", "/api/mc/instances/x/console/ticket/"),
                "a trailing slash must not fall back to the wider class");
        assertEquals(AgentScopes.MC, AgentScopes.required("POST", "/api/mc/instances/x/console/input"));
        assertEquals(AgentScopes.MC, AgentScopes.required("POST", "/api/mc/instances/x/console"));
        // The host's own readings are their own class, and a plain read ticket
        // must not inherit them through the "every GET is a read" shortcut.
        assertEquals(AgentScopes.SYSTEM, AgentScopes.required("GET", "/api/system/stats"));
        assertEquals(AgentScopes.SYSTEM, AgentScopes.required("GET", "/api/system/info"));
        // Transcode has its own pair: control/presets vs raw media access, and
        // the feature switch is always operator-only.
        assertEquals(AgentScopes.TRANSCODE, AgentScopes.required("GET", "/api/transcode/queue"));
        assertEquals(AgentScopes.TRANSCODE, AgentScopes.required("POST", "/api/transcode/queue/jobs"));
        assertEquals(AgentScopes.TRANSCODE, AgentScopes.required("GET", "/api/transcode/presets"));
        assertEquals(AgentScopes.TRANSCODE_RAW, AgentScopes.required("GET", "/api/transcode/media/list"));
        assertTrue(AgentScopes.forbidden("POST", "/api/transcode/feature/enable"));
        assertTrue(AgentScopes.forbidden("POST", "/api/transcode/feature/disable"));
        assertEquals(AgentScopes.INSTANCES, AgentScopes.required("POST", "/api/instances"));
        assertEquals(AgentScopes.PLUGINS, AgentScopes.required("POST", "/api/instances/x/plugins/install"));
        assertEquals(AgentScopes.CONFIG, AgentScopes.required("POST", "/api/settings/tls"));

        // A route nobody classified is closed, not open.
        assertNull(AgentScopes.required("POST", "/api/something-new"));

        // Self-fatal needs both the route and the matching instance.
        assertTrue(AgentScopes.selfFatal("POST", "/api/instances/a/stop", "a"));
        assertTrue(AgentScopes.selfFatal("POST", "/api/instances/a/terminal", "a"));
        assertTrue(AgentScopes.selfFatal("POST", "/api/instances/a/terminal/", "a"),
                "a trailing slash must not open the way out");
        assertTrue(AgentScopes.selfFatal("DELETE", "/api/instances/a", "a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/b/stop", "a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/a/launch", "a"));
        assertFalse(AgentScopes.selfFatal("POST", "/api/instances/a/stop", null));

        // Defaults are read-only.
        assertEquals(Set.of(AgentScopes.READ), AgentScopes.DEFAULT);
    }

    /// `mc` keeps its old reach; the PTY ticket is the one route that asks for
    /// the narrower `mc-console` class. Without it the answer is a scope
    /// refusal, not a ticket the handshake would have to sort out later.
    @Test
    void theMcConsoleTicketIsItsOwnClassOnTheWire() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String mcOnly = mint(client, base, cookie,
                    "\"label\":\"mc-only\",\"scopes\":[\"read\",\"mc\"]}").get("token").getAsString();
            HttpResponse<String> denied = post(client, base + "/api/mc/instances/x/console/ticket", "{}", null, mcOnly);
            assertEquals(403, denied.statusCode(), denied.body());
            assertTrue(denied.body().contains("agent_scope_denied"), denied.body());

            // One command through stdin is still ordinary `mc` work.
            HttpResponse<String> input = post(client, base + "/api/mc/instances/x/console/input",
                    "{\"line\":\"list\"}", null, mcOnly);
            assertTrue(input.statusCode() != 403, "console/input must stay inside mc: " + input.body());

            String withConsole = mint(client, base, cookie,
                    "\"label\":\"mc-console\",\"scopes\":[\"read\",\"mc\",\"mc-console\"]}")
                    .get("token").getAsString();
            HttpResponse<String> allowed = post(client, base + "/api/mc/instances/x/console/ticket",
                    "{}", null, withConsole);
            assertTrue(allowed.statusCode() != 403,
                    "the class must open the ticket route, got " + allowed.statusCode() + " " + allowed.body());
        }
    }

    /// 系统读数单独一类：`read` 票能看面板，但看不到宿主机。
    @Test
    void systemReadingsNeedTheirOwnScope() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            String readOnly = mint(client, base, cookie,
                    "\"label\":\"reader\",\"scopes\":[\"read\"]}").get("token").getAsString();
            HttpResponse<String> denied = getWithBearer(client, base + "/api/system/stats", readOnly);
            assertEquals(403, denied.statusCode(), denied.body());
            assertTrue(denied.body().contains("agent_scope_denied"), denied.body());

            String withSystem = mint(client, base, cookie,
                    "\"label\":\"monitor\",\"scopes\":[\"read\",\"system\"]}").get("token").getAsString();
            HttpResponse<String> stats = getWithBearer(client, base + "/api/system/stats", withSystem);
            assertEquals(200, stats.statusCode(), stats.body());
            assertTrue(stats.body().contains("\"history\""), stats.body());
        }
    }

    /// 票的来源：默认只认本机回环，改一次立即生效并落进 server.yaml。
    @Test
    void theSourceRuleDefaultsToLoopbackAndIsSaved() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            JsonObject initial = json(get(client, base + "/api/developer/agent-password", cookie));
            assertTrue(initial.get("loopbackOnly").getAsBoolean(),
                    "an agent runs beside the panel; the default is loopback-only: " + initial);

            HttpResponse<String> off = post(client, base + "/api/developer/agent-loopback",
                    "{\"loopbackOnly\":false}", cookie);
            assertEquals(200, off.statusCode(), off.body());
            assertFalse(json(off).get("loopbackOnly").getAsBoolean());

            // 状态接口读的是内存值；它已经跟着改了，说明没有只写文件。
            assertFalse(json(get(client, base + "/api/developer/agent-password", cookie))
                    .get("loopbackOnly").getAsBoolean());

            // 没有提权动不了它：这条路由和设置密码同一扇门。
            assertEquals(401, post(client, base + "/api/developer/agent-loopback",
                    "{\"loopbackOnly\":true}", null).statusCode());
        }
    }

    /// 畸形 JSON 是客户端错误，不是服务器错误。
    ///
    /// 审计里四个请求（`{"password":{}}` 打到 elevate / agent-auth、`label` 给成
    /// 对象、`ttlSeconds` 给成 `"abc"`）全部答 500：`stringField` 只挡了
    /// `isJsonNull()`，对象的 `getAsString()` 直接抛出来，没人接。这几个接口是
    /// 提权链的入口，500 既瞒下了真因，也把"输入不合法"和"面板坏了"混成一条。
    @Test
    void malformedBodiesAreClientErrorsAndNotServerErrors() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            // 必填的口令给成了对象。
            HttpResponse<String> elevate = post(client, base + "/api/developer/elevate",
                    "{\"password\":{}}", cookie);
            assertEquals(400, elevate.statusCode(), elevate.body());

            HttpResponse<String> setPassword = post(client, base + "/api/developer/agent-password",
                    "{\"password\":{}}", cookie);
            assertEquals(400, setPassword.statusCode(), setPassword.body());

            assertEquals(200, post(client, base + "/api/developer/agent-password",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\"]}", cookie).statusCode());

            HttpResponse<String> agentAuth = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":{}}", null);
            assertEquals(400, agentAuth.statusCode(), agentAuth.body());

            // 有效期给成了字符串。
            HttpResponse<String> ttl = post(client, base + "/api/developer/agent-auth",
                    "{\"password\":\"" + AGENT_PASSWORD + "\",\"label\":\"probe\",\"ttlSeconds\":\"abc\"}", null);
            assertEquals(400, ttl.statusCode(), ttl.body());
            assertTrue(ttl.body().contains("ttlSeconds"), ttl.body());

            // 可省略的字段给错了类型：忽略即可，但不能变成 500。
            HttpResponse<String> label = post(client, base + "/api/developer/agent-token",
                    "{\"label\":{},\"scopes\":[\"read\"]}", cookie);
            assertTrue(label.statusCode() < 500,
                    "a malformed optional field must not be a 500: " + label.statusCode() + " " + label.body());
        }
    }

    /// Mints a token the way the product does now: the agent exchanges the
    /// agent password for it. The cap is opened to every class so the scope
    /// under test is what decides.
    private JsonObject mint(HttpClient client, String base, String cookie, String fields) throws Exception {
        assertEquals(200, post(client, base + "/api/developer/agent-password",
                "{\"password\":\"" + AGENT_PASSWORD + "\",\"scopes\":[\"read\",\"mc\",\"mc-console\",\"system\",\"instances\",\"config\",\"plugins\"]}",
                cookie).statusCode());
        HttpResponse<String> minted = post(client, base + "/api/developer/agent-auth",
                "{\"password\":\"" + AGENT_PASSWORD + "\"," + fields, null);
        assertEquals(200, minted.statusCode(), minted.body());
        return json(minted);
    }

    private TestSupport.RunningServer start() throws Exception {
        return TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), config -> {
                    config.bindHost = "127.0.0.1";
                    config.https.enabled = true;
                    // `agent-auth` is only meaningful where the session alone
                    // would not already reach /api/*.
                    config.developerGuard.scope = org.jackhuang.hmcl.web.config.DeveloperGuardScope.ALL;
                });
    }

    /// 一键签票回来了：提权会话直接签、未绑定也敢发——因为挡住自杀的已经是
    /// 请求时的身份推导，不是签发时写进票里的那行字。
    @Test
    void anOperatorMintedTokenCannotKillItsHolderEither() throws Exception {
        try (TestSupport.RunningServer running = start()) {
            HttpClient client = TestSupport.trustAllClient();
            String base = "https://127.0.0.1:" + running.port();
            String cookie = elevatedSession(client, base);

            // 没有会话连门都进不去；提权会话可以直接签。
            assertEquals(401, post(client, base + "/api/developer/agent-token",
                    "{\"label\":\"nope\",\"scopes\":[\"read\"]}", null).statusCode());

            HttpResponse<String> minted = post(client, base + "/api/developer/agent-token",
                    "{\"label\":\"one-click\",\"scopes\":[\"read\",\"instances\"]}", cookie);
            assertEquals(200, minted.statusCode(), minted.body());
            JsonObject token = json(minted);
            String bearer = token.get("token").getAsString();
            assertTrue(token.get("scopes").toString().contains("instances"), minted.body());
            // The panel names its own address — the connector's real port, not
            // a default the browser or the prompt might have guessed.
            assertTrue(token.has("panel"), minted.body());
            assertTrue(token.get("panel").getAsString().endsWith(":" + running.port()),
                    "the answer must name the panel's own port: " + token.get("panel"));

            // 用起来与任何票一样。
            assertEquals(200, getWithBearer(client, base + "/api/instances", bearer).statusCode());

            // 致命操作也像任何票一样被拒：未绑定、且本测试的 JVM 谁的实例
            // 都不是，三个家族都答 409。
            HttpResponse<String> stop = post(client, base + "/api/instances/inst-a/stop", "{}", null, bearer);
            assertEquals(409, stop.statusCode(), stop.body());
            assertTrue(stop.body().contains("self_operation_refused"), stop.body());
            HttpResponse<String> brand = post(client, base + "/api/brands/kimi/instances/inst-a/stop", "{}", null, bearer);
            assertEquals(409, brand.statusCode(), brand.body());

            // 绑定了实例的票指名道姓：那儿拒绝，别处放行。
            HttpResponse<String> bound = post(client, base + "/api/developer/agent-token",
                    "{\"label\":\"bound\",\"scopes\":[\"instances\"],\"instanceId\":\"inst-a\"}", cookie);
            assertEquals(200, bound.statusCode(), bound.body());
            String boundBearer = json(bound).get("token").getAsString();
            HttpResponse<String> self = post(client, base + "/api/zcode/instances/inst-a/stop", "{}", null, boundBearer);
            assertEquals(409, self.statusCode(), self.body());
            HttpResponse<String> other = post(client, base + "/api/instances/inst-b/stop", "{}", null, boundBearer);
            assertTrue(other.statusCode() != 409 && other.statusCode() != 403,
                    "stopping another instance must pass the gate, got " + other.statusCode() + " " + other.body());

            // 票签不出自己的继任者：developer 路由对 agent 一律关闭。
            HttpResponse<String> relay = post(client, base + "/api/developer/agent-token",
                    "{\"label\":\"relay\",\"scopes\":[\"instances\"]}", null, bearer);
            assertEquals(403, relay.statusCode(), relay.body());
            assertTrue(relay.body().contains("agent_scope_denied"), relay.body());

            // 操作员签的票同样进列表、同样掐得掉。
            HttpResponse<String> listed = get(client, base + "/api/developer/agent-grants", cookie);
            assertEquals(200, listed.statusCode(), listed.body());
            String id = token.get("id").getAsString();
            assertTrue(listed.body().contains(id), listed.body());
            assertFalse(listed.body().contains(bearer), "the listing must never carry the token");
            assertEquals(200, delete(client, base + "/api/developer/agent-grants/" + id, cookie).statusCode());
            assertEquals(401, getWithBearer(client, base + "/api/instances", bearer).statusCode());
        }
    }

    private static HttpResponse<String> deleteWithBearer(HttpClient client, String url, String bearer)
            throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Bearer " + bearer).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String elevatedSession(HttpClient client, String base) throws Exception {
        HttpResponse<String> login = post(client, base + "/api/auth/login",
                "{\"username\":\"admin\",\"password\":\"" + PANEL_PASSWORD + "\"}", null);
        assertEquals(200, login.statusCode(), login.body());
        String cookie = login.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        assertEquals(200, post(client, base + "/api/developer/password",
                "{\"adminPassword\":\"" + PANEL_PASSWORD + "\",\"password\":\"" + DEV_PASSWORD
                        + "\",\"confirm\":\"" + DEV_PASSWORD + "\"}", cookie).statusCode());
        assertEquals(200, post(client, base + "/api/developer/elevate",
                "{\"password\":\"" + DEV_PASSWORD + "\"}", cookie).statusCode());
        return cookie;
    }

    private static HttpResponse<String> get(HttpClient client, String url, String cookie) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Cookie", cookie).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getWithBearer(HttpClient client, String url, String bearer)
            throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Bearer " + bearer).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(HttpClient client, String url, String cookie) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .header("Cookie", cookie).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body, String cookie)
            throws Exception {
        return post(client, url, body, cookie, null);
    }

    private static HttpResponse<String> post(HttpClient client, String url, String body,
                                             String cookie, String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
