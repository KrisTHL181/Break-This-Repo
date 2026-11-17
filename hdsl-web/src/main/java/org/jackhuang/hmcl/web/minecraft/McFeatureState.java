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
package org.jackhuang.hmcl.web.minecraft;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.function.LongSupplier;

/// The Minecraft feature switch: whether the surface exists, and the
/// one-hour lock that follows pressing 「停止 MC 功能」.
///
/// **Two persisted fields, two in-memory fields.** `state` and the acceptance
/// record live in `feature.json`; the lock and the keep-or-close decision live
/// only in this object, so a panel restart clears them (see `docs/minecraft.md`
/// §3.5). Writing the lock down would turn a decision aid into a lockout.
///
/// The lock is measured with a **monotonic** clock: an NTP step must not make
/// the hour jump forwards or backwards. Expiry is computed lazily on every
/// read, so no scheduler is needed and a process that was frozen for ten
/// minutes sees the truth on its next call.
@NotNullByDefault
public final class McFeatureState {

    /// How long 「停止 MC 功能」 holds the feature after the press.
    public static final Duration LOCK_DURATION = Duration.ofHours(1);

    /// The persisted half: everything that survives a restart.
    ///
    /// `state` is a `String` on purpose: the shared Gson lower-cases enum
    /// constants, and the contract in `docs/minecraft.md` spells the states
    /// `DISABLED` / `ENABLED`. Converting here keeps the file readable by hand.
    /// `thirdParty` is the second gate: it says whether the panel may run
    /// anything other than Mojang's jar, and it only ever turns on while
    /// `state` is `ENABLED`.
    private record Recorded(@SerializedName("state") String state,
                            @Nullable McConsent consent,
                            @SerializedName("thirdParty") @Nullable String thirdParty,
                            @Nullable McThirdPartyConsent thirdPartyConsent) {
    }

    /// The public state, as it appears in `feature.json`.
    public enum State {
        DISABLED,
        ENABLED
    }

    /// The lock refused an enable.
    public static final class LockedException extends Exception {
        private final long remainingSeconds;

        private LockedException(long remainingSeconds) {
            super("the Minecraft feature is locked for another " + remainingSeconds + " second(s)");
            this.remainingSeconds = remainingSeconds;
        }

        /// How long the caller has to wait.
        ///
        /// @return the remaining seconds
        public long remainingSeconds() {
            return remainingSeconds;
        }
    }

    private static final Logger LOG = Logger.LOG;

    private final Path file;
    private final LongSupplier nanoTime;
    private final Duration lockDuration;

    private volatile State state = State.DISABLED;
    private volatile @Nullable McConsent consent;
    /// The second gate: whether anything other than Mojang's jar may run.
    private volatile State thirdParty = State.DISABLED;
    private volatile @Nullable McThirdPartyConsent thirdPartyConsent;
    /// Whether the lock is running. A separate flag, not "start == 0": the
    /// monotonic clock itself may legitimately read 0 (and tests start there).
    private volatile boolean lockActive;
    /// Monotonic start of the lock; only meaningful while [lockActive].
    private volatile long lockedAtNanos;
    /// `null` until the owner decides; `TRUE` keeps the feature, `FALSE` closes it.
    private volatile @Nullable Boolean keepFeature;

    /// Opens the state stored in `<data>/minecraft/feature.json`.
    ///
    /// @param file the file to read and write
    public McFeatureState(Path file) {
        this(file, System::nanoTime, LOCK_DURATION);
    }

    /// The same, with an injectable clock and lock duration for tests.
    ///
    /// @param file         the file to read and write
    /// @param nanoTime     the monotonic clock
    /// @param lockDuration how long the lock lasts
    public McFeatureState(Path file, LongSupplier nanoTime, Duration lockDuration) {
        this.file = file.toAbsolutePath().normalize();
        this.nanoTime = nanoTime;
        this.lockDuration = lockDuration;
        load();
    }

    /// The persisted state.
    ///
    /// @return the state
    public State state() {
        sync();
        return state;
    }

    /// Whether the feature is enabled.
    ///
    /// @return whether the surface is on
    public boolean enabled() {
        return state() == State.ENABLED;
    }

    /// The acceptance record of the last enable, if any.
    ///
    /// @return the record
    public Optional<McConsent> consent() {
        return Optional.ofNullable(consent);
    }

    /// The second gate's persisted state.
    ///
    /// @return `ENABLED` only while the owner has enabled third-party cores
    public State thirdPartyState() {
        sync();
        return thirdParty;
    }

    /// Whether third-party server cores may be created and run.
    ///
    /// @return whether the second gate is on
    public boolean thirdPartyEnabled() {
        return thirdPartyState() == State.ENABLED;
    }

    /// The acceptance record of the last third-party enable, if any.
    ///
    /// @return the record
    public Optional<McThirdPartyConsent> thirdPartyConsent() {
        return Optional.ofNullable(thirdPartyConsent);
    }

    /// Turns the second gate on, with the acceptance record of this enable.
    ///
    /// The gate exists only inside an enabled Minecraft feature: asking for it
    /// while the surface is off is a programming error, not a request the panel
    /// can honour.
    ///
    /// @param record what was shown and accepted
    /// @throws IllegalStateException when the Minecraft feature is disabled
    /// @throws LockedException       when the lock is still running
    public void enableThirdParty(McThirdPartyConsent record) throws LockedException {
        sync();
        if (state != State.ENABLED) {
            throw new IllegalStateException("the Minecraft feature is disabled");
        }
        if (lockActive) {
            throw new LockedException(lockRemainingSeconds());
        }
        thirdParty = State.ENABLED;
        thirdPartyConsent = record;
        save();
    }

    /// Turns the second gate off without losing the acceptance record: the
    /// record explains the last enable, and re-enabling replaces it.
    public void disableThirdParty() {
        sync();
        if (thirdParty == State.DISABLED) {
            return;
        }
        thirdParty = State.DISABLED;
        save();
    }

    /// Whether the lock is running.
    ///
    /// @return whether the feature is locked
    public boolean locked() {
        sync();
        return lockActive;
    }

    /// How long the lock has left.
    ///
    /// @return the remaining seconds, `0` when unlocked
    public long lockRemainingSeconds() {
        sync();
        if (!lockActive) {
            return 0;
        }
        long elapsed = nanoTime.getAsLong() - lockedAtNanos;
        long remaining = lockDuration.toNanos() - elapsed;
        if (remaining <= 0) {
            return 0;
        }
        return (remaining + 999_999_999L) / 1_000_000_000L;
    }

    /// The decision the owner made while the lock runs, if any.
    ///
    /// @return `null` while undecided, `TRUE` to keep, `FALSE` to close
    public @Nullable Boolean keepFeatureChoice() {
        sync();
        return keepFeature;
    }

    /// Starts the lock for a press of 「停止 MC 功能」.
    ///
    /// A second press while the lock runs does **not** restart the hour; that
    /// would let a caller extend the lock by pressing again.
    ///
    /// @return whether this call started the lock (`false` when it was already running)
    /// @throws IllegalStateException when the feature is disabled
    public boolean stopFeature() {
        sync();
        if (state != State.ENABLED) {
            throw new IllegalStateException("the Minecraft feature is disabled");
        }
        if (lockActive) {
            return false;
        }
        lockActive = true;
        lockedAtNanos = nanoTime.getAsLong();
        keepFeature = null;
        return true;
    }

    /// Records 「确认关闭功能」: the feature goes back to `DISABLED`, while the
    /// lock keeps running (it can still not be enabled until it expires).
    ///
    /// @throws IllegalStateException when no lock is running
    public void confirmClose() {
        sync();
        if (!lockActive) {
            throw new IllegalStateException("no stop is in progress");
        }
        if (Boolean.TRUE.equals(keepFeature)) {
            throw new IllegalStateException("the feature was already kept");
        }
        state = State.DISABLED;
        // Closing the surface closes the second gate with it: a feature that
        // does not exist cannot keep a sub-feature enabled.
        thirdParty = State.DISABLED;
        keepFeature = Boolean.FALSE;
        save();
    }

    /// Records 「我不想关掉功能」: the feature is kept, but the lock keeps
    /// running — this decision never shortens the hour.
    ///
    /// @throws IllegalStateException when no lock is running
    public void decideKeep() {
        sync();
        if (!lockActive) {
            throw new IllegalStateException("no stop is in progress");
        }
        if (keepFeature != null) {
            return;
        }
        keepFeature = Boolean.TRUE;
    }

    /// Enables the feature with the acceptance record of this enable.
    ///
    /// @param record what was shown and accepted
    /// @throws LockedException when the lock is still running
    public void enable(McConsent record) throws LockedException {
        sync();
        if (lockActive) {
            throw new LockedException(lockRemainingSeconds());
        }
        state = State.ENABLED;
        consent = record;
        keepFeature = null;
        save();
    }

    /// Whether the lock is running, without the argument checks of the actions.
    private void sync() {
        if (!lockActive) {
            return;
        }
        if (nanoTime.getAsLong() - lockedAtNanos < lockDuration.toNanos()) {
            return;
        }
        // The hour is over: drop the lock and the decision with it. The state
        // already reflects the decision, so nothing else has to change.
        synchronized (this) {
            if (lockActive && nanoTime.getAsLong() - lockedAtNanos >= lockDuration.toNanos()) {
                lockActive = false;
                lockedAtNanos = 0;
                keepFeature = null;
            }
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            Recorded recorded = JsonUtils.fromJsonFile(file, Recorded.class);
            if (recorded == null) {
                return;
            }
            state = parseState(recorded.state());
            consent = recorded.consent();
            thirdParty = parseState(recorded.thirdParty());
            thirdPartyConsent = recorded.thirdPartyConsent();
            // A file that says "third party on, feature off" is not a state the
            // panel can act on: the sub-feature cannot outlive its surface.
            if (state != State.ENABLED) {
                thirdParty = State.DISABLED;
            }
        } catch (Exception e) {
            LOG.warning("Failed to read the Minecraft feature state " + file, e);
        }
    }

    private static State parseState(@Nullable String value) {
        if (value == null) {
            return State.DISABLED;
        }
        try {
            return State.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warning("Unknown Minecraft feature state `" + value + "`, treating it as DISABLED");
            return State.DISABLED;
        }
    }

    private void save() {
        Recorded recorded = new Recorded(state.name(), consent, thirdParty.name(), thirdPartyConsent);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, "mc-feature", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, recorded);
                move(temp, file);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOG.warning("Failed to write the Minecraft feature state " + file, e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}