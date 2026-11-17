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

import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Deletes log files that outlived the retention window.
///
/// Two families live in the same directory and are aged by different rules:
///
/// - **审计日志** (`audit-YYYY-MM-DD.jsonl`) carry the date in the name, so the
///   window is read from the file itself rather than from `mtime`; copying the
///   data directory must not launder an old file into looking fresh.
/// - **面板日志** (`*.log`, `*.log.xz`) are rotated by [Logger] and carry no
///   date we can trust in the name, so they age by `mtime`.
///
/// The rule is "keep at most N days", never "keep at least N files": a panel
/// that idles for a month must not keep a year of silence, and a busy day must
/// not be trimmed to its last ten files. The newest file in each family is
/// always kept — a wrong clock must not delete the log being written.
///
/// [FLOOR_DAYS] is the floor: below 180 days the audit trail stops being able to
/// answer "who deleted that world last quarter", so no setting may go under it.
/// [HARD_CAP_DAYS] is four years: long enough for a dispute over a world that was
/// deleted last season, short enough that the directory cannot grow without
/// bound. Anything outside the range is refused rather than clamped, so a typo in
/// `server.yaml` fails at boot instead of quietly keeping data forever.
@NotNullByDefault
public final class LogRetention {

    /// 上限四年。任何窗口都不能超过它。
    public static final int HARD_CAP_DAYS = 4 * 365;

    /// 保底 180 天：低于这个数，审计就答不上「上季度谁删了那个世界」。
    public static final int FLOOR_DAYS = 180;

    /// 审计日志默认保留 180 天（与《使用条款》5.3 一致）。
    public static final int DEFAULT_AUDIT_DAYS = FLOOR_DAYS;

    /// 面板日志默认 180 天：排障够用，又不至于把终端输出攒成仓库。
    public static final int DEFAULT_PANEL_DAYS = FLOOR_DAYS;

    private static final String AUDIT_PREFIX = "audit-";
    private static final String AUDIT_SUFFIX = ".jsonl";

    /// 运行中唯一的一份（`Main` 启动时装上），供 `LogRetentionApiServlet` 改窗口。
    private static volatile @org.jetbrains.annotations.Nullable LogRetention installed;

    private final Path logsDir;
    // 面板可以调窗口，所以不是 final；sweep 每轮重新读一次。
    private volatile int auditDays;
    private volatile int panelDays;
    private final Clock clock;
    /// 时钟可不可信；不可信时一轮删除都不做（[ClockGuard]）。
    private final ClockGuard clockGuard;
    /// 最近一次扫描的结果（包括“因为时钟不可信而没删”）。
    private volatile SweepResult lastSweep = new SweepResult(null, 0, "has not run yet");

    /// 时钟采样间隔：半小时一次（足够抓住“突然漂移”，又不至于每分钟吵一次）。
    public static final Duration CLOCK_TICK = Duration.ofMinutes(30);

    /// 到点才真扫；被时钟挡下的那轮在下一跳重试，所以这里不是 24 小时一次的死板节奏。
    private static final Duration SWEEP_INTERVAL = Duration.ofHours(24);

    /// 日志里出现“未来时间”的容忍度：超过这么多就当墙钟往回跳过。
    private static final Duration FUTURE_GRACE = Duration.ofHours(1);

    /// 一次扫描的结果。
    ///
    /// @param at            扫描时刻（`null` 表示从没扫过）
    /// @param deleted       删掉的文件数
    /// @param skippedReason 没删的理由（`null` 表示真的扫过一轮）
    public record SweepResult(@Nullable Instant at, int deleted, @Nullable String skippedReason) {
    }

    public LogRetention(Path logsDir, int auditDays, int panelDays) {
        this(logsDir, auditDays, panelDays, Clock.systemDefaultZone(),
                new ClockGuard(Instant::now, System::nanoTime));
    }

    LogRetention(Path logsDir, int auditDays, int panelDays, Clock clock) {
        this(logsDir, auditDays, panelDays, clock, ClockGuard.trusted());
    }

    LogRetention(Path logsDir, int auditDays, int panelDays, Clock clock, ClockGuard clockGuard) {
        this.logsDir = logsDir;
        this.auditDays = requireWindow(auditDays, "audit");
        this.panelDays = requireWindow(panelDays, "panel");
        this.clock = clock;
        this.clockGuard = clockGuard;
    }

    private static int requireWindow(int days, String label) {
        if (days < FLOOR_DAYS || days > HARD_CAP_DAYS) {
            throw new IllegalArgumentException(label + " retention must be between "
                    + FLOOR_DAYS + " and " + HARD_CAP_DAYS + " days, got " + days);
        }
        return days;
    }

    /// 装上并开始计时。同一 JVM 只该有一份。
    public static void install(LogRetention retention) {
        installed = retention;
        retention.start();
    }

    /// 运行中的那一份；没有（比如单元测试）时返回 `null`。
    public static @org.jetbrains.annotations.Nullable LogRetention installed() {
        return installed;
    }

    /// 调窗口。只接受 [FLOOR_DAYS]–[HARD_CAP_DAYS] 之间的值。
    public void setWindows(int auditDays, int panelDays) {
        this.auditDays = requireWindow(auditDays, "audit");
        this.panelDays = requireWindow(panelDays, "panel");
    }

    public int auditDays() {
        return auditDays;
    }

    public int panelDays() {
        return panelDays;
    }

    /// 启动时跑一次，之后每 24 小时一次。守护线程：面板退出时它跟着走。
    public void start() {
        // 基线从“现在”算起：否则第一个观察窗会从第一次心跳才开始，等价于白等一个周期。
        clockGuard.sample();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "log-retention");
            thread.setDaemon(true);
            return thread;
        });
        // 第一次等过观察窗（否则第一次采样撞不上跳变），之后每半小时一跳：
        // 采样时钟；到点了才真扫。被时钟挡下的那轮在下一跳重试。
        scheduler.scheduleWithFixedDelay(this::tick,
                ClockGuard.BOOT_GRACE.plusSeconds(10).toSeconds(),
                CLOCK_TICK.toSeconds(), TimeUnit.SECONDS);
    }

    /// 一次心跳：采样时钟，必要时扫一轮。
    void tick() {
        try {
            clockGuard.sample();
            Instant now = clock.instant();
            SweepResult previous = lastSweep;
            if (previous.at() != null && previous.skippedReason() == null
                    && previous.at().plus(SWEEP_INTERVAL).isAfter(now)) {
                return;
            }
            int deleted = sweep();
            if (deleted > 0) {
                Logger.LOG.info("Log retention: deleted " + deleted + " file(s) older than "
                        + auditDays + "/" + panelDays + " day(s)");
            }
        } catch (RuntimeException e) {
            // 清理失败不能影响面板运行：下一秒日志照样要写。
            Logger.LOG.warning("Log retention failed: " + e, e);
        }
    }

    /// 删一轮，返回删掉的文件数。测试直接调这个，不碰调度器。
    ///
    /// **时钟不可信时一个文件都不删**：按时间删文件的工具不该信任一个会跳的钟。
    /// 这一轮会记进 [SweepResult]，界面据此说明“为什么没删”。
    public int sweep() {
        Instant now = clock.instant();
        if (!Files.isDirectory(logsDir)) {
            lastSweep = new SweepResult(now, 0, "logs directory does not exist");
            return 0;
        }
        Path newest = newestPanelLog();
        // 日志的时间在未来 = 墙钟往回跳过。这一条是跨进程重启也成立的证据：
        // 单调钟只能抓“运行中的跳变”，而“未来时间的文件”是跳变留下的痕迹。
        if (newest != null && lastModified(newest).isAfter(now.plus(FUTURE_GRACE))) {
            clockGuard.suspect("a log file is dated in the future (" + newest.getFileName()
                    + "); the clock moved backwards");
        }
        ClockGuard.Status status = clockGuard.status();
        if (!status.trusted()) {
            lastSweep = new SweepResult(now, 0, status.reason());
            Logger.LOG.warning("Log retention skipped: " + status.reason()
                    + " (nothing was deleted; " + auditDays + "/" + panelDays + " day windows stay as they are)");
            return 0;
        }
        LocalDate today = LocalDate.now(clock);
        int deleted = sweepAudit(today) + sweepPanel(newest);
        lastSweep = new SweepResult(now, deleted, null);
        return deleted;
    }

    /// 时钟状态（给「关于 → 日志」看）。
    public ClockGuard.Status clockStatus() {
        return clockGuard.status();
    }

    /// 最近一次扫描的结果。
    public SweepResult lastSweep() {
        return lastSweep;
    }

    private int sweepAudit(LocalDate today) {
        LocalDate cutoff = today.minusDays(auditDays);
        int deleted = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(logsDir, AUDIT_PREFIX + "*" + AUDIT_SUFFIX)) {
            for (Path file : files) {
                LocalDate day = auditDay(file);
                // 名字里没有日期的文件不动：宁可留着，也不按 mtime 猜一份审计记录。
                if (day == null || !day.isBefore(cutoff)) {
                    continue;
                }
                deleted += delete(file);
            }
        } catch (IOException e) {
            Logger.LOG.warning("Could not list audit logs in " + logsDir + ": " + e.getMessage());
        }
        return deleted;
    }

    /// 面板日志里最新的那一份；没有就是 `null`。
    ///
    /// 抽出来是因为有两处要用：删除时“最新那份永远留”，以及“它的时间在未来 =
    /// 时钟往回跳过”这条跨重启也成立的判断。
    private @Nullable Path newestPanelLog() {
        Path newest = null;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(logsDir)) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".log") && !name.endsWith(".log.xz")) {
                    continue;
                }
                if (newest == null || lastModified(file).isAfter(lastModified(newest))) {
                    newest = file;
                }
            }
        } catch (IOException e) {
            Logger.LOG.warning("Could not list panel logs in " + logsDir + ": " + e.getMessage());
            return null;
        }
        return newest;
    }

    private int sweepPanel(@Nullable Path newest) {
        Instant cutoff = clock.instant().minusSeconds(TimeUnit.DAYS.toSeconds(panelDays));
        // 先收集再删：最新那份永远留——时钟被调错时，它是唯一还在写的文件。
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(logsDir)) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".log") && !name.endsWith(".log.xz")) {
                    continue;
                }
                candidates.add(file);
            }
        } catch (IOException e) {
            Logger.LOG.warning("Could not list panel logs in " + logsDir + ": " + e.getMessage());
            return 0;
        }
        int deleted = 0;
        for (Path file : candidates) {
            if (file.equals(newest)) {
                continue;
            }
            if (lastModified(file).isBefore(cutoff)) {
                deleted += delete(file);
            }
        }
        return deleted;
    }

    private Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.MAX;
        }
    }

    private int delete(Path file) {
        try {
            Files.deleteIfExists(file);
            return 1;
        } catch (IOException e) {
            Logger.LOG.warning("Could not delete expired log " + file + ": " + e.getMessage());
            return 0;
        }
    }

    /// `audit-2026-10-30.jsonl` → 2026-10-30；不认识的形状返回 `null`。
    public static @org.jetbrains.annotations.Nullable LocalDate auditDay(Path file) {
        String name = file.getFileName().toString();
        if (!name.startsWith(AUDIT_PREFIX) || !name.endsWith(AUDIT_SUFFIX)) {
            return null;
        }
        String date = name.substring(AUDIT_PREFIX.length(), name.length() - AUDIT_SUFFIX.length());
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /// 测试用：固定时钟。
    static LogRetention at(Path logsDir, int auditDays, int panelDays, Instant now) {
        return new LogRetention(logsDir, auditDays, panelDays, Clock.fixed(now, ZoneId.systemDefault()));
    }
}
