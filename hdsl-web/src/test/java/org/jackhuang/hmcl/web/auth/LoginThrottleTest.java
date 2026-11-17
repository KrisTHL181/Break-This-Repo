/*
 * HDSL-web
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.web.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 登录预算：五分钟内十五次失败、第十六次挡住十分钟、十分钟后从空记录重新开始、成功一次即清零。
///
/// 时间由调用方传进来，所以这些边界是算出来的而不是等出来的：窗口的边缘、拒绝的最后一秒、
/// 拒绝过后的第一秒，都在这里钉住。
class LoginThrottleTest {

    /// 测试里的「现在」。
    private static final Instant T0 = Instant.parse("2026-10-20T12:00:00Z");

    /// 一个被反复失败的地址。
    private static final String ADDRESS = "192.0.2.7";

    @Test
    void fifteenFailuresAreAllowedAndTheSixteenthIsRefused() {
        LoginThrottle throttle = new LoginThrottle();

        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertEquals(Duration.ZERO, throttle.recordFailure(ADDRESS, T0.plusSeconds(i)),
                    "第 " + (i + 1) + " 次失败仍在预算内");
            assertFalse(throttle.isBlocked(ADDRESS, T0.plusSeconds(i)));
        }

        assertEquals(LoginThrottle.BLOCK, throttle.recordFailure(ADDRESS, T0.plusSeconds(15)),
                "第十六次失败把预算花完");
        assertTrue(throttle.isBlocked(ADDRESS, T0.plusSeconds(16)));
    }

    @Test
    void aRefusalRunsOutIntoAnEmptyRecord() {
        LoginThrottle throttle = new LoginThrottle();
        spend(throttle);
        Instant blockedAt = T0.plusSeconds(LoginThrottle.MAX_FAILURES);

        assertTrue(throttle.isBlocked(ADDRESS, blockedAt.plus(LoginThrottle.BLOCK).minusSeconds(1)),
                "最后一秒还在挡");
        assertFalse(throttle.isBlocked(ADDRESS, blockedAt.plus(LoginThrottle.BLOCK)),
                "十分钟一到就不再挡");

        // 而且不是「带着那十六次失败继续」：又从满额预算开始。
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertEquals(Duration.ZERO,
                    throttle.recordFailure(ADDRESS, blockedAt.plus(LoginThrottle.BLOCK).plusSeconds(1 + i)),
                    "拒绝过后重新有满额预算");
        }
    }

    @Test
    void failuresThatFallOutOfTheWindowStopCounting() {
        LoginThrottle throttle = new LoginThrottle();
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            throttle.recordFailure(ADDRESS, T0.plusSeconds(i));
        }

        Instant later = T0.plus(LoginThrottle.WINDOW).plusSeconds(1);
        assertEquals(Duration.ZERO, throttle.recordFailure(ADDRESS, later), "窗口外的失败不再算数");
        assertFalse(throttle.isBlocked(ADDRESS, later));
    }

    @Test
    void aSignInThatSucceedsClearsTheRecord() {
        LoginThrottle throttle = new LoginThrottle();
        for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
            throttle.recordFailure(ADDRESS, T0.plusSeconds(i));
        }

        throttle.clear(ADDRESS);

        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertEquals(Duration.ZERO, throttle.recordFailure(ADDRESS, T0.plusSeconds(100 + i)),
                    "清零之后又是满额预算");
        }
    }

    @Test
    void oneAddressSpendsOnlyItsOwnBudget() {
        LoginThrottle throttle = new LoginThrottle();
        spend(throttle);

        assertTrue(throttle.isBlocked(ADDRESS, T0.plusSeconds(LoginThrottle.MAX_FAILURES + 1)));
        assertFalse(throttle.isBlocked("192.0.2.8", T0.plusSeconds(LoginThrottle.MAX_FAILURES + 1)),
                "别的地址不受影响");
    }

    @Test
    void aFloodFromManyAddressesDoesNotSpendSomebodyElsesRefusal() {
        // 拒绝记录是攻击者唯一能安排的驱逐对象——用很多地址灌进来就是干这个的——所以洪水不能把它
        // 冲掉，否则刷够地址就又能继续猜了。
        LoginThrottle throttle = new LoginThrottle();
        spend(throttle);
        Instant blockedAt = T0.plusSeconds(LoginThrottle.MAX_FAILURES);

        for (int i = 0; i < 4000; i++) {
            // 洪水发生在拒绝仍然有效的那十分钟里（所以这条测的是「拒绝不会被冲掉」，
            // 而不是「拒绝过期之后会被清掉」——那是上一条用例的事）。
            throttle.recordFailure("198.51.100." + (i % 256) + "." + (i / 256), blockedAt.plusMillis(i));
        }

        assertTrue(throttle.isBlocked(ADDRESS, blockedAt.plus(LoginThrottle.BLOCK).minusSeconds(1)),
                "两百多个地址的洪水过后，那个被拒绝的地址仍然被拒绝");
    }

    /// 把一个地址的预算花完。
    ///
    /// @param throttle the throttle
    private static void spend(LoginThrottle throttle) {
        for (int i = 0; i <= LoginThrottle.MAX_FAILURES; i++) {
            throttle.recordFailure(ADDRESS, T0.plusSeconds(i));
        }
    }
}