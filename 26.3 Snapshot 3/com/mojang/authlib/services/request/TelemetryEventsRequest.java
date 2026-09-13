package com.mojang.authlib.services.request;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;

public record TelemetryEventsRequest(@SerializedName("events") List<TelemetryEventsRequest.Event> events) {
   public record Event(
      @SerializedName("source") String source,
      @SerializedName("name") String name,
      @SerializedName("timestamp") long timestamp,
      @SerializedName("data") JsonObject data
   ) {
      public Event(String source, String name, Instant timestamp, JsonObject data) {
         this(source, name, timestamp.getEpochSecond(), data);
      }
   }
}
