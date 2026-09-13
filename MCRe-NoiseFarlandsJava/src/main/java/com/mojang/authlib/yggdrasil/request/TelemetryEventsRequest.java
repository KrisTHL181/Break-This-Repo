package com.mojang.authlib.yggdrasil.request;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.List;

public record TelemetryEventsRequest(
    @SerializedName("events")
    List<Event> events
) {
    public record Event(
        @SerializedName("source")
        String source,
        @SerializedName("name")
        String name,
        @SerializedName("timestamp")
        long timestamp,
        @SerializedName("data")
        JsonObject data
    ) {
        public Event(final String source, final String name, final Instant timestamp, final JsonObject data) {
            this(source, name, timestamp.getEpochSecond(), data);
        }
    }
}
