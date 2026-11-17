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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/// When the panel last had an authenticated person (or agent) behind it.
///
/// The system sampler runs in two gears: one sample per second while someone
/// is using the panel, one per thirty seconds otherwise. "Someone is using it"
/// cannot mean "a session exists" — `AuthService` slides a session's expiry to
/// a week on every request, so a browser that was closed without logging out
/// would keep a session alive for days. This clock is the narrower question:
/// was there an **authenticated request** in the last [#WINDOW]?
///
/// Touching it also nudges the sampler, so a login switches to the fast gear
/// immediately instead of waiting out a thirty-second sleep.
///
/// Static on purpose: the two places that authenticate (the session filter and
/// the agent gate) are deep in the request path and would otherwise need the
/// sampler threaded through half the servlet constructors.
@NotNullByDefault
public final class SystemActivity {

    /// How long an authenticated request keeps the fast gear on.
    public static final Duration WINDOW = Duration.ofMinutes(5);

    private static volatile Instant last = Instant.EPOCH;
    private static final AtomicReference<@Nullable Runnable> listener = new AtomicReference<>();

    private SystemActivity() {
    }

    /// Records an authenticated request and wakes the sampler.
    public static void touch() {
        last = Instant.now();
        Runnable wake = listener.get();
        if (wake != null) {
            wake.run();
        }
    }

    /// Whether the panel was used within [#WINDOW].
    public static boolean active() {
        return active(Instant.now());
    }

    /// The same question against an explicit clock, for tests.
    static boolean active(Instant now) {
        Instant seen = last;
        return !seen.equals(Instant.EPOCH)
                && Duration.between(seen, now).compareTo(WINDOW) < 0;
    }

    /// Installs what a touch should wake (the sampler's `touch`), or clears it.
    public static void install(@Nullable Runnable wake) {
        listener.set(wake);
    }

    /// Forgets the last activity, for tests.
    static void reset() {
        last = Instant.EPOCH;
    }
}
