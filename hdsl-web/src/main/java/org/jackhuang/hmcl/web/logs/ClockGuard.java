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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/// “系统时钟现在能不能信”。
///
/// 日志清理按时间删文件，所以**一个乱跳的时钟能把日志整片删掉**：往前跳一年，
/// 所有审计日志都成了“过期”；往回跳，又会让本该删的永远留着。这里不校时、不写时间，
/// 只回答一个问题：自上次检查以来，墙钟走的方向和量，跟单调钟对得上吗？
///
/// - `System.nanoTime()` 是单调的，不受校时、手工改时间影响；
/// - 两个钟在两次采样之间走过的毫秒数之差，就是“墙钟被动了多少”。
///
/// 差值超过 [MAX_DRIFT] 就**永久**标记不可信（不自动恢复）：一次 NTP 校时把时间
/// 拨回来不该顺手把“刚才删过什么”洗白。恢复可信只有一个办法——重启面板。
///
/// 采样由 [LogRetention] 每半小时做一次；开机后要观察满 [BOOT_GRACE] 才谈得上可信，
/// 否则第一次采样根本撞不上跳变。
///
/// 已知边界：进程**没在跑**的时候发生的跳变看不见（没有单调钟可对照）。这一条
/// 写明在 `README` 里，不假装覆盖。
@NotNullByDefault
public final class ClockGuard {

    /// 两次采样之间墙钟与单调钟的差超过这个数，就算跳变。
    public static final Duration MAX_DRIFT = Duration.ofSeconds(5);

    /// 开机后至少观察这么久，“没见跳变”才有意义。
    public static final Duration BOOT_GRACE = Duration.ofSeconds(90);

    private final Supplier<Instant> wall;
    private final LongSupplier monotonic;

    private @Nullable Instant baselineWall;
    private long baselineMono;
    private @Nullable Instant watchingSince;
    private volatile @Nullable Instant checkedAt;
    private volatile long lastSkewMillis;
    /// 第一次判定的理由；一旦写下就不再改（不可信是粘住的）。
    private volatile @Nullable String suspicion;

    /// @param wall      墙钟（生产环境就是 `Instant::now`）
    /// @param monotonic 单调钟（生产环境是 `System::nanoTime`）
    public ClockGuard(Supplier<Instant> wall, LongSupplier monotonic) {
        this.wall = wall;
        this.monotonic = monotonic;
    }

    /// 可不可信、为什么不可信、上次检查是什么时候、上次测到的偏差。
    ///
    /// @param trusted     现在能不能按时间删文件
    /// @param reason      不能的理由（可读，直接给界面看）；可信时为 `null`
    /// @param checkedAt   最近一次采样的墙钟
    /// @param skewMillis  最近一次采样测到的墙钟偏差（毫秒，正数=墙钟走快了）
    /// @param watchingSince 从什么时候开始观察（第一次采样的墙钟）
    public record Status(boolean trusted, @Nullable String reason, @Nullable Instant checkedAt,
                         long skewMillis, @Nullable Instant watchingSince) {
    }

    /// 采一次样。第一次只记基线，之后每次比较两个钟走过的量。
    public void sample() {
        Instant now = wall.get();
        long mono = monotonic.getAsLong();
        if (baselineWall == null) {
            baselineWall = now;
            baselineMono = mono;
            if (watchingSince == null) {
                watchingSince = now;
            }
            checkedAt = now;
            return;
        }
        long wallDelta = Duration.between(baselineWall, now).toMillis();
        long monoDelta = TimeUnit.NANOSECONDS.toMillis(mono - baselineMono);
        long skew = wallDelta - monoDelta;
        lastSkewMillis = skew;
        checkedAt = now;
        if (suspicion == null && Math.abs(skew) > MAX_DRIFT.toMillis()) {
            suspicion = "system clock moved " + describe(skew) + " against the monotonic clock; "
                    + "log cleanup is off until the panel restarts";
        }
        // 基线跟着走：只抓“两次采样之间的跳变”，不把正常的走时误差攒成大数。
        baselineWall = now;
        baselineMono = mono;
    }

    /// 由外部证据判定不可信（比如某份日志的时间在未来 = 时钟往回跳过）。粘住。
    ///
    /// @param reason 写进 [Status#reason] 的理由
    void suspect(String reason) {
        if (suspicion == null) {
            suspicion = reason;
        }
    }

    /// 现在能不能按时间删文件。
    public Status status() {
        String reason = suspicion;
        if (reason == null) {
            Instant since = watchingSince;
            Instant now = wall.get();
            if (since == null || Duration.between(since, now).compareTo(BOOT_GRACE) < 0) {
                reason = "the clock has not been watched long enough yet ("
                        + BOOT_GRACE.toSeconds() + " s)";
            }
        }
        return new Status(reason == null, reason, checkedAt, lastSkewMillis, watchingSince);
    }

    /// 测试用：一开始就可信（相当于“已经观察了很久、没见跳变”）。
    static ClockGuard trusted() {
        ClockGuard guard = new ClockGuard(Instant::now, System::nanoTime);
        guard.watchingSince = Instant.EPOCH;
        return guard;
    }

    private static String describe(long millis) {
        double seconds = millis / 1000.0;
        return (seconds >= 0 ? "+" : "") + String.format(java.util.Locale.ROOT, "%.1f", seconds) + " s";
    }
}
