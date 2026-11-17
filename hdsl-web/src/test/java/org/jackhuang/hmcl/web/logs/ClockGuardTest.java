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
package org.jackhuang.hmcl.web.logs;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockGuardTest {

    private Instant wall = Instant.parse("2026-10-30T12:00:00Z");
    private long mono = 1_000_000_000L;

    private ClockGuard guard() {
        return new ClockGuard(() -> wall, () -> mono);
    }

    /// 墙钟与单调钟各走各的；不一致的那部分就是“有人动了时间”。
    private void advance(Duration wallDelta, Duration monotonicDelta) {
        wall = wall.plus(wallDelta);
        mono += monotonicDelta.toNanos();
    }

    @Test
    void aFreshGuardIsNotTrustedUntilItHasWatchedLongEnough() {
        ClockGuard guard = guard();
        guard.sample();

        assertFalse(guard.status().trusted());
        assertTrue(guard.status().reason().contains("watched long enough"),
                guard.status().reason());

        advance(Duration.ofSeconds(120), Duration.ofSeconds(120));
        guard.sample();
        assertTrue(guard.status().trusted(), "两分钟一致就该可信");
    }

    @Test
    void aJumpForwardIsCaughtAtTheNextSample() {
        ClockGuard guard = guard();
        guard.sample();
        advance(Duration.ofMinutes(30), Duration.ofMinutes(30));
        guard.sample();
        assertTrue(guard.status().trusted());

        // 有人往前拨了三小时（单调钟只走了半小时）。
        advance(Duration.ofMinutes(30).plusHours(3), Duration.ofMinutes(30));
        guard.sample();

        assertFalse(guard.status().trusted());
        assertTrue(guard.status().reason().contains("system clock moved"),
                guard.status().reason());
        assertTrue(guard.status().skewMillis() >= 3 * 3600 * 1000L,
                "偏差要能报出来：" + guard.status().skewMillis());
    }

    @Test
    void aJumpBackwardsIsCaughtToo() {
        ClockGuard guard = guard();
        guard.sample();
        advance(Duration.ofMinutes(30), Duration.ofMinutes(30));
        guard.sample();

        // 往回拨一小时：墙钟只“走了”负三十分钟。
        advance(Duration.ofMinutes(30).minusHours(1), Duration.ofMinutes(30));
        guard.sample();

        assertFalse(guard.status().trusted());
        assertTrue(guard.status().skewMillis() <= -60 * 60 * 1000L, "偏差是负的：" + guard.status().skewMillis());
    }

    @Test
    void theVerdictIsSticky() {
        ClockGuard guard = guard();
        guard.sample();
        // 墙钟跳了五小时，单调钟只走了五分钟。
        advance(Duration.ofHours(5).plusMinutes(5), Duration.ofMinutes(5));
        guard.sample();
        assertFalse(guard.status().trusted());

        // 之后一直“正常”，也不该把自己洗回可信：删掉的东西不会自己回来。
        for (int i = 0; i < 5; i++) {
            advance(Duration.ofMinutes(30), Duration.ofMinutes(30));
            guard.sample();
        }

        assertFalse(guard.status().trusted());
    }

    @Test
    void smallSkewIsNotAJump() {
        ClockGuard guard = guard();
        guard.sample();
        // 两秒的差在容差之内：正常走时误差与一次 NTP 微调都可能这么大。
        advance(Duration.ofMinutes(30).plusSeconds(2), Duration.ofMinutes(30));
        guard.sample();

        assertTrue(guard.status().trusted(), "小偏差不该让清理停摆");
    }
}
