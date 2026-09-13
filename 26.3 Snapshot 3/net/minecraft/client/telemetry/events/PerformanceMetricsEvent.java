package net.minecraft.client.telemetry.events;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;

public final class PerformanceMetricsEvent extends AggregatedTelemetryEvent {
   private static final long DEDICATED_MEMORY_KB = toKilobytes(Runtime.getRuntime().maxMemory());
   private final LongList fpsSamples = new LongArrayList();
   private final LongList frameTimeSamples = new LongArrayList();
   private final LongList usedMemorySamples = new LongArrayList();

   @Override
   public void tick(final TelemetryEventSender eventSender) {
      if (Minecraft.getInstance().telemetryOptInExtra()) {
         super.tick(eventSender);
      }
   }

   private void resetValues() {
      this.fpsSamples.clear();
      this.frameTimeSamples.clear();
      this.usedMemorySamples.clear();
   }

   @Override
   public void takeSample() {
      this.fpsSamples.add(Minecraft.getInstance().getFps());
      this.takeUsedMemorySample();
      this.frameTimeSamples.add(Minecraft.getInstance().getFrameTimeNs());
   }

   private void takeUsedMemorySample() {
      long totalMemory = Runtime.getRuntime().totalMemory();
      long freeMemory = Runtime.getRuntime().freeMemory();
      long usedMemorySample = totalMemory - freeMemory;
      this.usedMemorySamples.add(toKilobytes(usedMemorySample));
   }

   @Override
   public void sendEvent(final TelemetryEventSender eventSender) {
      eventSender.send(TelemetryEventType.PERFORMANCE_METRICS, properties -> {
         properties.put(TelemetryProperty.FRAME_RATE_SAMPLES, new LongArrayList(this.fpsSamples));
         properties.put(TelemetryProperty.RENDER_TIME_SAMPLES, new LongArrayList(this.frameTimeSamples));
         properties.put(TelemetryProperty.USED_MEMORY_SAMPLES, new LongArrayList(this.usedMemorySamples));
         properties.put(TelemetryProperty.NUMBER_OF_SAMPLES, this.getSampleCount());
         properties.put(TelemetryProperty.RENDER_DISTANCE, Minecraft.getInstance().options.getEffectiveRenderDistance());
         properties.put(TelemetryProperty.DEDICATED_MEMORY_KB, (int)DEDICATED_MEMORY_KB);
      });
      this.resetValues();
   }

   private static long toKilobytes(final long bytes) {
      return bytes / 1000L;
   }
}
