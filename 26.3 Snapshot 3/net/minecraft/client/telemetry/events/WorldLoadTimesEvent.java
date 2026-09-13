package net.minecraft.client.telemetry.events;

import java.time.Duration;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import org.jspecify.annotations.Nullable;

public class WorldLoadTimesEvent {
   private final boolean newWorld;
   private final @Nullable Duration worldLoadDuration;

   public WorldLoadTimesEvent(final boolean newWorld, final @Nullable Duration worldLoadDuration) {
      this.worldLoadDuration = worldLoadDuration;
      this.newWorld = newWorld;
   }

   public void send(final TelemetryEventSender eventSender) {
      if (this.worldLoadDuration != null) {
         eventSender.send(TelemetryEventType.WORLD_LOAD_TIMES, event -> {
            event.put(TelemetryProperty.WORLD_LOAD_TIME_MS, (int)this.worldLoadDuration.toMillis());
            event.put(TelemetryProperty.NEW_WORLD, this.newWorld);
         });
      }
   }
}
