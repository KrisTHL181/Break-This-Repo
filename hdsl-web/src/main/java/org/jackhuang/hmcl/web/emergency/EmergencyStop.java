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
package org.jackhuang.hmcl.web.emergency;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/// The emergency stop's state: one switch that freezes the panel's outward
/// faces (instance web pages, terminals, instance processes).
///
/// **The state lives in memory only.** Restarting the panel process or its
/// container clears it — that is the documented, physical recovery path, and
/// the only one: there is no "force release" endpoint, and while the cooldown
/// runs nobody can lift the stop, developer or not.
///
/// The class only knows *when* the stop was engaged; what it does to running
/// work is [org.jackhuang.hmcl.web.developer.DeveloperApiServlet]'s engage
/// sequence (terminals first, then every instance's process tree).
@NotNullByDefault
public final class EmergencyStop {

    /// How long the stop holds before a release is even accepted. One hour: a
    /// mistake costs an hour, which is the point.
    public static final Duration COOLDOWN = Duration.ofHours(1);

    private final Object lock = new Object();
    private @Nullable Instant engagedAt;
    private @Nullable String actor;
    private @Nullable String reason;

    /// Whether the stop is engaged.
    public boolean isEngaged() {
        synchronized (lock) {
            return engagedAt != null;
        }
    }

    /// When the stop was engaged, or `null` while the panel runs normally.
    public @Nullable Instant engagedAt() {
        synchronized (lock) {
            return engagedAt;
        }
    }

    /// When a release becomes possible, or `null` while the panel runs normally.
    public @Nullable Instant releasesAt() {
        synchronized (lock) {
            return engagedAt == null ? null : engagedAt.plus(COOLDOWN);
        }
    }

    /// The actor who engaged it, for the audit trail and the banner.
    public @Nullable String actor() {
        synchronized (lock) {
            return actor;
        }
    }

    /// The reason the operator typed, if any.
    public @Nullable String reason() {
        synchronized (lock) {
            return reason;
        }
    }

    /// How much of the cooldown is left; [Duration#ZERO] outside a stop and
    /// once the hour is up.
    public Duration remaining(Instant now) {
        synchronized (lock) {
            if (engagedAt == null) {
                return Duration.ZERO;
            }
            Duration left = Duration.between(now, engagedAt.plus(COOLDOWN));
            return left.isNegative() ? Duration.ZERO : left;
        }
    }

    /// Engages the stop.
    ///
    /// @param actor who asked for it (the panel account), for the audit trail
    /// @param reason free text shown in the banner, may be `null`
    /// @param now    the clock reading, so tests can move time
    /// @return whether this call engaged it; `false` when it already was (a
    ///         second press must not restart the hour)
    public boolean engage(String actor, @Nullable String reason, Instant now) {
        synchronized (lock) {
            if (engagedAt != null) {
                return false;
            }
            this.engagedAt = now;
            this.actor = actor;
            this.reason = reason;
            return true;
        }
    }

    /// Ends the stop, but only once the cooldown has run out.
    ///
    /// @param now the clock reading
    /// @return the instant the stop ended, or `null` when there was nothing to
    ///         release or the hour is not up yet
    public @Nullable Instant release(Instant now) {
        synchronized (lock) {
            if (engagedAt == null || now.isBefore(engagedAt.plus(COOLDOWN))) {
                return null;
            }
            Instant endedAt = engagedAt;
            engagedAt = null;
            actor = null;
            reason = null;
            return endedAt;
        }
    }
}
