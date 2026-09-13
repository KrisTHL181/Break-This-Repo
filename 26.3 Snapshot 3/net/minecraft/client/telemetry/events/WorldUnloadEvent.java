package net.minecraft.client.telemetry.events;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;

public class WorldUnloadEvent {
   private static final int NOT_TRACKING_TIME = -1;
   private Optional<Instant> worldLoadedTime = Optional.empty();
   private long totalTicks;
   private long lastGameTime;

   public void onPlayerInfoReceived() {
      this.lastGameTime = -1L;
      if (this.worldLoadedTime.isEmpty()) {
         this.worldLoadedTime = Optional.of(Instant.now());
      }
   }

   public void setTime(final long gameTime) {
      if (this.lastGameTime != -1L) {
         this.totalTicks = this.totalTicks + Math.max(0L, gameTime - this.lastGameTime);
      }

      this.lastGameTime = gameTime;
   }

   private int getTimeInSecondsSinceLoad(final Instant loadedTime) {
      Duration timeBetween = Duration.between(loadedTime, Instant.now());
      return (int)timeBetween.toSeconds();
   }

   public void send(final TelemetryEventSender eventSender) {
      this.worldLoadedTime.ifPresent(loadedTime -> eventSender.send(TelemetryEventType.WORLD_UNLOADED, properties -> {
         properties.put(TelemetryProperty.SECONDS_SINCE_LOAD, this.getTimeInSecondsSinceLoad(loadedTime));
         properties.put(TelemetryProperty.TICKS_SINCE_LOAD, (int)this.totalTicks);
      }));
   }
}
