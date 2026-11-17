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
package org.jackhuang.hmcl.web.transcode.queue;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.web.transcode.preset.TranscodePreset;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// One queued/running/finished encode.
///
/// Fields are Gson-serialized into `queue.json`; the live process is `transient`, so
/// a restart cannot try to serialize a handle from a dead JVM. Paths are stored
/// **relative to the media root**, which is what makes the queue file portable when
/// the share is mounted elsewhere.
@NotNullByDefault
public final class TranscodeJob {

    public enum State {
        @SerializedName("queued") QUEUED,
        @SerializedName("running") RUNNING,
        @SerializedName("paused") PAUSED,
        @SerializedName("waiting_decision") WAITING_DECISION,
        @SerializedName("stopping") STOPPING,
        @SerializedName("done") DONE,
        @SerializedName("failed") FAILED,
        @SerializedName("cancelled") CANCELLED;

        public boolean terminal() {
            return this == DONE || this == FAILED || this == CANCELLED;
        }

        public String wireName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public String id = "";
    public String input = "";
    public String output = "";
    public String presetId = "";
    public String presetName = "";
    /// Non-empty for queue-backed tools (merge / mux / extract); the command is
    /// then already built instead of coming from the preset.
    public String tool = "";
    public TranscodePreset preset;
    public State state = State.QUEUED;
    public double fraction = -1;
    public String message = "";
    public double speed = -1;
    public double fps = -1;
    public String bitrate = "";
    public long outTimeMs = -1;
    public long durationMs = -1;
    public long sizeBytes = -1;
    public int frame = -1;
    public @Nullable String error;
    public @Nullable String encoder;
    public @Nullable String fallbackFrom;
    public @Nullable String fallbackTo;
    public boolean fallbackAttempted;
    public long decisionDeadline = -1;
    public @Nullable String logFile;
    public long createdAt = System.currentTimeMillis();
    public long startedAt = -1;
    public long endedAt = -1;
    public List<String> command = List.of();
    /// Encoder switches this job went through, newest last. Kept across retries so a
    /// fallback that failed twice can still be explained.
    public List<SwitchRecord> switchRecords = new ArrayList<>();
    /// The argv of every pass, with its own state. Rebuilt on each run.
    public List<Step> steps = new ArrayList<>();

    public transient @Nullable Process process;
    public transient long lastPublishAt;

    public TranscodeJob() {
        this.preset = new TranscodePreset();
    }

    /// One hardware-fallback hop: who triggered it, from which encoder to which, why.
    public static final class SwitchRecord {
        public long at = System.currentTimeMillis();
        public String from = "";
        public String to = "";
        /// `operator`, `timeout`, `policy` or `queue`.
        public String trigger = "";
        public String reason = "";
        public List<String> droppedArgs = List.of();
    }

    /// One ffmpeg invocation of this job. A two-pass preset has two, a tool job one.
    public static final class Step {
        public String name = "";
        /// `pending`, `running`, `done`, `failed` or `cancelled`.
        public String state = "pending";
        public List<String> args = List.of();
    }

    public void resetForRetry() {
        state = State.QUEUED;
        fraction = -1;
        message = "";
        speed = -1;
        fps = -1;
        bitrate = "";
        outTimeMs = -1;
        durationMs = -1;
        sizeBytes = -1;
        frame = -1;
        error = null;
        encoder = null;
        fallbackFrom = null;
        fallbackTo = null;
        fallbackAttempted = false;
        decisionDeadline = -1;
        tool = "";
        startedAt = -1;
        endedAt = -1;
        command = List.of();
        steps = new ArrayList<>();
    }
}