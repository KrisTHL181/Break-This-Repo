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

import org.jetbrains.annotations.NotNullByDefault;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/// Refuses to keep answering guesses from one address.
///
/// The sign-in gate is the only way into the panel, and a password is the only thing in front of it —
/// so what matters most about that gate is how many guesses a machine is allowed to make. The budget
/// is [#MAX_FAILURES] failures in [#WINDOW]; the attempt that goes over it is refused for [#BLOCK],
/// and a sign-in that succeeds in between clears the record, because the password being right is the
/// one thing this is asking about.
///
/// **Nothing is said while the budget is intact.** An attacker learns nothing from a policy it cannot
/// see, and somebody who has simply mistyped is not told about a counter that is still theirs to
/// spend. What is said is the refusal — and only when it happens.
///
/// The key is the address the request came from. Behind a reverse proxy that is the proxy's address,
/// so the budget is shared by everyone that proxy serves: that is the deliberate trade, because the
/// header a proxy would be recognised by (`X-Forwarded-For`) is one a client can write itself, and
/// trusting it would let a single attacker both evade this and spend everybody else's budget.
///
/// A refused address is refused *before* the password is checked. Checking it is the expensive half —
/// a PBKDF2 derivation at the iteration count this panel uses — and spending that is exactly what an
/// attacker is trying to do.
@NotNullByDefault
public final class LoginThrottle {

    /// How many failures a window may hold before the next one is refused.
    public static final int MAX_FAILURES = 15;

    /// How far back failures are counted.
    public static final Duration WINDOW = Duration.ofMinutes(5);

    /// How long a refusal lasts once the budget is spent.
    public static final Duration BLOCK = Duration.ofMinutes(10);

    /// How many addresses are remembered at once.
    ///
    /// An address that has spent its budget, or has failures still inside the window, is kept;
    /// anything older is dropped when the map grows past this, so a machine that opens connections
    /// from many addresses cannot make this grow without bound.
    private static final int MAX_TRACKED = 1024;

    /// One address's record.
    private static final class Attempts {

        /// When the failures happened, oldest first, all inside the window.
        private final Deque<Instant> failures = new ArrayDeque<>();

        /// When the refusal ends, or `null` while the address is not refused.
        private Instant blockedUntil;
    }

    private final Map<String, Attempts> byAddress = new HashMap<>();

    /// The policy this instance enforces. The constants above are the panel
    /// sign-in defaults; a caller may pass a different policy.
    private final int maxFailures;
    private final Duration window;
    private final Duration block;

    /// A throttle that enforces the panel sign-in defaults above.
    public LoginThrottle() {
        this(MAX_FAILURES, WINDOW, BLOCK);
    }

    /// A throttle with its own policy.
    ///
    /// @param maxFailures failures a window may hold before the next is refused
    /// @param window      how far back failures are counted
    /// @param block       how long a refusal lasts once the budget is spent
    public LoginThrottle(int maxFailures, Duration window, Duration block) {
        if (maxFailures < 1 || window.isNegative() || window.isZero() || block.isNegative() || block.isZero()) {
            throw new IllegalArgumentException("throttle policy must have a positive budget, window and refusal");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.block = block;
    }

    /// How long this throttle refuses an address for.
    public Duration block() {
        return block;
    }

    /// How many failures this throttle's window holds.
    public int maxFailures() {
        return maxFailures;
    }

    /// Reports whether an address is being refused right now.
    ///
    /// @param address the address the request came from
    /// @param now     the current time
    /// @return whether it has spent its budget
    public synchronized boolean isBlocked(String address, Instant now) {
        Attempts attempts = byAddress.get(address);
        if (attempts == null || attempts.blockedUntil == null) {
            return false;
        }
        if (now.isBefore(attempts.blockedUntil)) {
            return true;
        }
        // The refusal has run out, and what it runs out into is an empty record: the address starts
        // again rather than carrying the failures that earned the refusal.
        byAddress.remove(address);
        return false;
    }

    /// Records a failed attempt.
    ///
    /// @param address the address the request came from
    /// @param now     the current time
    /// @return how long the address is refused for, or [Duration#ZERO] when it is not refused
    public synchronized Duration recordFailure(String address, Instant now) {
        Attempts attempts = byAddress.computeIfAbsent(address, key -> new Attempts());
        if (attempts.blockedUntil != null) {
            if (now.isBefore(attempts.blockedUntil)) {
                return Duration.between(now, attempts.blockedUntil);
            }
            attempts.blockedUntil = null;
            attempts.failures.clear();
        }

        forgetOldFailures(attempts, now);
        attempts.failures.addLast(now);
        if (attempts.failures.size() > maxFailures) {
            attempts.failures.clear();
            attempts.blockedUntil = now.plus(block);
            forgetOldAddresses(now);
            return block;
        }
        forgetOldAddresses(now);
        return Duration.ZERO;
    }

    /// Forgets an address's failures, which a sign-in that succeeded does.
    ///
    /// @param address the address the request came from
    public synchronized void clear(String address) {
        byAddress.remove(address);
    }

    /// Drops the failures that have fallen out of the window.
    ///
    /// @param attempts the record
    /// @param now      the current time
    private void forgetOldFailures(Attempts attempts, Instant now) {
        Instant oldest = now.minus(window);
        while (!attempts.failures.isEmpty() && attempts.failures.peekFirst().isBefore(oldest)) {
            attempts.failures.removeFirst();
        }
    }

    /// Keeps the map inside its bound once it grows past it.
    ///
    /// An address being refused is never given up to make room. Dropping a refusal is the one eviction
    /// an attacker can arrange — flooding from many addresses is exactly how — so what goes first is
    /// the least recently seen address that is *not* being refused. If only refusals are left the map
    /// is allowed past its bound, and is then bounded by how many addresses spent a budget inside the
    /// block window, which is the shape of the attack rather than an accident.
    ///
    /// @param now the current time
    private void forgetOldAddresses(Instant now) {
        if (byAddress.size() <= MAX_TRACKED) {
            return;
        }
        byAddress.entrySet().removeIf(entry -> {
            Attempts attempts = entry.getValue();
            if (attempts.blockedUntil != null && now.isBefore(attempts.blockedUntil)) {
                return false;
            }
            forgetOldFailures(attempts, now);
            return attempts.failures.isEmpty();
        });
        while (byAddress.size() > MAX_TRACKED) {
            String leastRecent = null;
            Instant leastRecentAt = null;
            for (Map.Entry<String, Attempts> entry : byAddress.entrySet()) {
                Attempts attempts = entry.getValue();
                if (attempts.blockedUntil != null && now.isBefore(attempts.blockedUntil)) {
                    continue;
                }
                Instant seen = attempts.failures.peekLast();
                if (seen == null) {
                    seen = Instant.MIN;
                }
                if (leastRecentAt == null || seen.isBefore(leastRecentAt)) {
                    leastRecentAt = seen;
                    leastRecent = entry.getKey();
                }
            }
            if (leastRecent == null) {
                // Only refusals left, and they are not given up.
                return;
            }
            byAddress.remove(leastRecent);
        }
    }
}