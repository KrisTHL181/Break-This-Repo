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
package org.jackhuang.hmcl.web.transcode.quality;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/// One quality-comparison run: reference vs encoded, one or more metrics.
@NotNullByDefault
public final class QualityJob {

    public enum State {
        @SerializedName("running") RUNNING,
        @SerializedName("done") DONE,
        @SerializedName("failed") FAILED,
        @SerializedName("cancelled") CANCELLED
    }

    public String id = "";
    public String reference = "";
    public String encoded = "";
    public List<String> metrics = List.of();
    public State state = State.RUNNING;
    public double progress = 0;
    public String message = "";
    public @Nullable String error;
    public long createdAt = System.currentTimeMillis();
    public long endedAt = -1;
    /// metric -> result; insertion order matters to the page.
    public Map<String, MetricResult> results = Map.of();
    public transient @Nullable Process process;

    public record MetricResult(double average, List<Frame> frames) {
    }

    public record Frame(int n, double value) {
    }
}