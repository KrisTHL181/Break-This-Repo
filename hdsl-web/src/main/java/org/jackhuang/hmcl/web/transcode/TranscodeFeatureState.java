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
package org.jackhuang.hmcl.web.transcode;

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
import java.util.Optional;

/// Whether the transcode surface exists, and the acceptance record of its last
/// enable.
///
/// The feature switch is a runtime decision the owner makes in the panel, not a
/// deployment key: it lives in `<data>/transcode/feature.json`, not `server.yaml`.
/// While the state is [State#DISABLED], [TranscodeFeatureFilter] answers 404 for
/// everything except the enable flow, so a disabled panel looks exactly like one
/// that never shipped this feature.
///
/// There is no cool-down lock: unlike Minecraft, disabling transcoding stops the
/// queue and its child processes, so there is no orphan that a quick re-enable
/// could adopt.
@NotNullByDefault
public final class TranscodeFeatureState {

    /// The persisted state.
    public enum State {
        DISABLED,
        ENABLED
    }

    private static final Logger LOG = Logger.LOG;

    private final Path file;
    private volatile State state = State.DISABLED;
    private volatile @Nullable TranscodeConsent consent;

    /// Opens the state stored in `feature.json`, creating nothing.
    ///
    /// @param file the file to read and write
    public TranscodeFeatureState(Path file) {
        this.file = file.toAbsolutePath().normalize();
        load();
    }

    /// The persisted state.
    public synchronized State state() {
        return state;
    }

    /// Whether the surface is enabled.
    public synchronized boolean enabled() {
        return state == State.ENABLED;
    }

    /// The acceptance record of the last enable, if any.
    public synchronized Optional<TranscodeConsent> consent() {
        return Optional.ofNullable(consent);
    }

    /// Turns the feature on and records what the owner accepted.
    ///
    /// Calling this while already enabled replaces the record: that is what a
    /// re-enable after a notice change is.
    public synchronized void enable(TranscodeConsent record) {
        state = State.ENABLED;
        consent = record;
        save();
    }

    /// Turns the feature off. The acceptance record stays as evidence of the
    /// last enable; the next enable replaces it.
    public synchronized void disable() {
        if (state == State.DISABLED) {
            return;
        }
        state = State.DISABLED;
        save();
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
        } catch (Exception e) {
            LOG.warning("Failed to read the transcode feature state " + file, e);
        }
    }

    private static State parseState(@Nullable String value) {
        if (value == null) {
            return State.DISABLED;
        }
        try {
            return State.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warning("Unknown transcode feature state `" + value + "`, treating it as DISABLED");
            return State.DISABLED;
        }
    }

    private void save() {
        Recorded recorded = new Recorded(state.name(), consent);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, "transcode-feature", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, recorded);
                move(temp, file);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOG.warning("Failed to write the transcode feature state " + file, e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record Recorded(
            @SerializedName("state") String state,
            @SerializedName("consent") @Nullable TranscodeConsent consent) {
    }
}