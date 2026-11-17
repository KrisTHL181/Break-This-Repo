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
package org.jackhuang.hmcl.web.system;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Turns a stream of snapshots into the few lines an operator would want to
/// find in the log after the fact.
///
/// Three rules keep this from becoming log spam:
///
/// - a condition must hold for [#CPU_SUSTAIN] and friends before it counts;
/// - while it keeps holding, the same warning is written at most once per
///   cooldown;
/// - when it clears, one "recovered" line closes the episode.
///
/// The state is keyed per metric and lives in memory only. A panel restart
/// starts a fresh episode, which is honest: the pre-restart history is gone
/// too.
@NotNullByDefault
public final class AnomalyDetector {

    /// CPU above this fraction counts as saturated.
    public static final double CPU_THRESHOLD = 0.90;
    /// Memory above this fraction counts as tight.
    public static final double MEMORY_THRESHOLD = 0.90;
    /// A mount with less than this fraction free counts as full.
    public static final double DISK_FREE_FLOOR = 0.10;
    /// Load above this multiple of the core count counts as overloaded.
    public static final double LOAD_MULTIPLE = 2.0;

    public static final Duration CPU_SUSTAIN = Duration.ofSeconds(30);
    public static final Duration MEMORY_SUSTAIN = Duration.ofSeconds(60);
    public static final Duration LOAD_SUSTAIN = Duration.ofMinutes(5);
    public static final Duration ALERT_COOLDOWN = Duration.ofMinutes(10);
    public static final Duration DISK_COOLDOWN = Duration.ofHours(1);

    /// One line for the log.
    ///
    /// @param metric   `cpu`, `memory`, `disk` or `load`
    /// @param message  what was observed
    /// @param recovery whether this line closes an episode
    public record Alert(String metric, String message, boolean recovery) {
    }

    private static final class State {
        private Instant since;
        private Instant lastLog;
        private boolean alerted;
    }

    private final Map<String, State> states = new HashMap<>();

    /// Checks one snapshot.
    ///
    /// @param snapshot the sample
    /// @param now      the wall clock the sample was taken at
    /// @return the lines to log, empty when nothing changed
    public synchronized List<Alert> check(SystemSnapshot snapshot, Instant now) {
        List<Alert> alerts = new ArrayList<>();

        double load = snapshot.cpu().load();
        evaluate("cpu", load >= CPU_THRESHOLD, now, CPU_SUSTAIN, ALERT_COOLDOWN,
                String.format("cpu %.1f%% (threshold %.0f%%)", load * 100, CPU_THRESHOLD * 100),
                String.format("cpu back under %.0f%%", CPU_THRESHOLD * 100), alerts);

        SystemSnapshot.Memory memory = snapshot.memory();
        double memoryRatio = memory.totalBytes() > 0
                ? (double) memory.usedBytes() / memory.totalBytes()
                : 0;
        evaluate("memory", memoryRatio >= MEMORY_THRESHOLD, now, MEMORY_SUSTAIN, ALERT_COOLDOWN,
                String.format("memory %.1f%% of %d MiB used (threshold %.0f%%)",
                        memoryRatio * 100, memory.totalBytes() >> 20, MEMORY_THRESHOLD * 100),
                String.format("memory back under %.0f%%", MEMORY_THRESHOLD * 100), alerts);

        SystemSnapshot.MountUsage fullest = null;
        double fullestRatio = 0;
        for (SystemSnapshot.MountUsage mount : snapshot.mounts()) {
            if (mount.totalBytes() <= 0) {
                continue;
            }
            double ratio = (double) mount.usedBytes() / mount.totalBytes();
            if (ratio > fullestRatio) {
                fullestRatio = ratio;
                fullest = mount;
            }
        }
        evaluate("disk", fullest != null && fullestRatio >= 1 - DISK_FREE_FLOOR, now,
                Duration.ZERO, DISK_COOLDOWN,
                fullest == null ? "disk" : String.format("disk %s %.1f%% full (threshold %.0f%%)",
                        fullest.path(), fullestRatio * 100, (1 - DISK_FREE_FLOOR) * 100),
                "disk space back above the floor", alerts);

        double[] loadAverage = snapshot.cpu().loadAverage();
        int cores = snapshot.cpu().count();
        boolean overloaded = cores > 0 && loadAverage.length > 0
                && loadAverage[0] > cores * LOAD_MULTIPLE;
        evaluate("load", overloaded, now, LOAD_SUSTAIN, ALERT_COOLDOWN,
                cores > 0 && loadAverage.length > 0
                        ? String.format("load %.2f over %.0fx %d cores", loadAverage[0], LOAD_MULTIPLE, cores)
                        : "load",
                "load back under the threshold", alerts);

        return alerts;
    }

    /// Applies the sustain/cooldown/recovery rules for one metric.
    private void evaluate(String metric, boolean condition, Instant now,
                          Duration sustain, Duration cooldown,
                          String message, String recovery, List<Alert> alerts) {
        State state = states.computeIfAbsent(metric, key -> new State());
        if (condition) {
            if (state.since == null) {
                state.since = now;
            }
            if (!state.alerted && !now.isBefore(state.since.plus(sustain))) {
                state.alerted = true;
                state.lastLog = now;
                alerts.add(new Alert(metric, message, false));
            } else if (state.alerted && !now.isBefore(state.lastLog.plus(cooldown))) {
                state.lastLog = now;
                alerts.add(new Alert(metric, message, false));
            }
        } else if (state.alerted) {
            state.alerted = false;
            state.since = null;
            state.lastLog = null;
            alerts.add(new Alert(metric, recovery, true));
        } else {
            state.since = null;
        }
    }
}
