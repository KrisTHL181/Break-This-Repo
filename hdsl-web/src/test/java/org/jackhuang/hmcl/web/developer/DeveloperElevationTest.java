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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The shared developer elevation: per session, sliding, and dead with its
/// session.
class DeveloperElevationTest {

    @Test
    void theKeyIsTheSessionTokenNeverTheUsername() {
        assertEquals("token:abc", DeveloperElevation.key("abc", "10.0.0.1"));
        assertEquals("anonymous:10.0.0.1", DeveloperElevation.key(null, "10.0.0.1"));
        assertEquals("anonymous:unknown", DeveloperElevation.key("", null));
    }

    @Test
    void elevationSlidesAndDiesWithItsSession() {
        DeveloperElevation elevation = new DeveloperElevation();
        String key = DeveloperElevation.key("abc", null);

        assertFalse(elevation.isElevated(key, true));
        elevation.elevate(key);
        assertTrue(elevation.isElevated(key, true));
        assertTrue(elevation.require(key, true), "a live session keeps the elevation");
        assertFalse(elevation.isElevated(key, false), "a dead session is not elevated");
        assertFalse(elevation.require(key, false));
    }

    @Test
    void clearingEndsEveryElevation() {
        DeveloperElevation elevation = new DeveloperElevation();
        String first = DeveloperElevation.key("one", null);
        String second = DeveloperElevation.key("two", null);
        elevation.elevate(first);
        elevation.elevate(second);

        elevation.clear(first);
        assertFalse(elevation.isElevated(first, true));
        assertTrue(elevation.isElevated(second, true));

        elevation.clearAll();
        assertFalse(elevation.isElevated(second, true));
    }

    @Test
    void threeWrongPasswordsLockTheKey() {
        DeveloperElevation elevation = new DeveloperElevation();
        String key = DeveloperElevation.key("abc", null);
        Instant now = Instant.now();

        assertFalse(elevation.recordFailure(key, now));
        assertFalse(elevation.recordFailure(key, now));
        assertTrue(elevation.recordFailure(key, now), "the third failure trips the lock");
        assertTrue(elevation.isLocked(key, now));

        elevation.clearFailures(key);
        assertFalse(elevation.isLocked(key, now));
    }

    /// 两种 bearer 存在同一个命名空间里，但权限差一整层：脚本 token 是开发者
    /// 自己的提权，agent 票只是一张限定范围的许可。让 `requireToken` 认后者，
    /// 守卫过滤器和 WebSocket 升级门就会把 `read` 票当成完整开发者证明。
    @Test
    void anAgentGrantIsNotADeveloperProof() {
        DeveloperElevation elevation = new DeveloperElevation();
        // 票是借来的权力：它只在有人提权的窗口里成立。`mintAgentToken` 的调用方
        // 负责验提权，这里显式建一个 session 提权来代表那个窗口。
        String humanKey = DeveloperElevation.key("probe-session", null);
        elevation.elevate(humanKey);

        String human = elevation.mintToken();
        assertTrue(elevation.requireToken(human));

        String agent = elevation.mintAgentToken("probe", java.util.Set.of(AgentScopes.READ),
                null, java.time.Duration.ofMinutes(5), null, DeveloperElevation.MintedBy.PASSWORD);
        assertFalse(elevation.requireToken(agent),
                "an agent grant must never satisfy a developer-password gate");
        assertTrue(elevation.agentGrant(agent) != null, "it is still a live agent grant");

        elevation.revokeAgent(elevation.agentGrant(agent).id());
        assertFalse(elevation.requireToken(agent));
    }

    /// 窗口结束，票立刻失效——不等它自己的到期时间。登出会清掉 session 提权，
    /// 「停止提权」也清；两者都不该留下能用的票。
    @Test
    void endingTheWindowEndsTheGrant() {
        DeveloperElevation elevation = new DeveloperElevation();
        String humanKey = DeveloperElevation.key("probe-session", null);
        elevation.elevate(humanKey);

        String agent = elevation.mintAgentToken("probe", java.util.Set.of(AgentScopes.READ),
                null, java.time.Duration.ofHours(1), null, DeveloperElevation.MintedBy.SESSION);
        assertTrue(elevation.agentGrant(agent) != null);
        assertEquals(1, elevation.agentGrants().size());

        elevation.clear(humanKey);

        assertNull(elevation.agentGrant(agent), "without a lender the loan is void");
        assertTrue(elevation.agentGrants().isEmpty(), "the panel must not show a dead grant as active");
    }

    /// The id list exists for teardown, not for display: a grant whose window
    /// already ended is not "active" any more, but its console socket may still
    /// be open and only the id can find it.
    @Test
    void agentGrantIdsOutliveTheWindowSoSocketsCanBeFound() {
        DeveloperElevation elevation = new DeveloperElevation();
        String humanKey = DeveloperElevation.key("probe-session", null);
        elevation.elevate(humanKey);

        String agent = elevation.mintAgentToken("probe", java.util.Set.of(AgentScopes.MC_CONSOLE),
                "mc/one", java.time.Duration.ofHours(1), null, DeveloperElevation.MintedBy.SESSION);
        DeveloperElevation.AgentGrant grant = elevation.agentGrant(agent);
        assertTrue(grant != null);

        elevation.clear(humanKey);

        assertNull(elevation.agentGrant(agent));
        assertTrue(elevation.agentGrantIds().contains(grant.id()),
                "teardown must still find the grant whose console is attached");

        elevation.revokeAllAgents();
        assertTrue(elevation.agentGrantIds().isEmpty());
    }
}
