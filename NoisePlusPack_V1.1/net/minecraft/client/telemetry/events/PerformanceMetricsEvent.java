package net.minecraft.client.telemetry.events;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PerformanceMetricsEvent extends AggregatedTelemetryEvent {
   private static final long DEDICATED_MEMORY_KB = toKilobytes(Runtime.getRuntime().maxMemory());
   private final LongList fpsSamples = new LongArrayList();
   private final LongList frameTimeSamples = new LongArrayList();
   private final LongList usedMemorySamples = new LongArrayList();

   @Override
   public void tick(TelemetryEventSender p_263321_) {
      if (Minecraft.getInstance().telemetryOptInExtra()) {
         super.tick(p_263321_);
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
      long i = Runtime.getRuntime().totalMemory();
      long j = Runtime.getRuntime().freeMemory();
      long k = i - j;
      this.usedMemorySamples.add(toKilobytes(k));
   }

   @Override
   public void sendEvent(TelemetryEventSender p_261872_) {
      p_261872_.send(TelemetryEventType.PERFORMANCE_METRICS, p_261568_ -> {
         p_261568_.put(TelemetryProperty.FRAME_RATE_SAMPLES, new LongArrayList(this.fpsSamples));
         p_261568_.put(TelemetryProperty.RENDER_TIME_SAMPLES, new LongArrayList(this.frameTimeSamples));
         p_261568_.put(TelemetryProperty.USED_MEMORY_SAMPLES, new LongArrayList(this.usedMemorySamples));
         p_261568_.put(TelemetryProperty.NUMBER_OF_SAMPLES, this.getSampleCount());
         p_261568_.put(TelemetryProperty.RENDER_DISTANCE, Minecraft.getInstance().options.getEffectiveRenderDistance());
         p_261568_.put(TelemetryProperty.DEDICATED_MEMORY_KB, (int)DEDICATED_MEMORY_KB);
      });
      this.resetValues();
   }

   private static long toKilobytes(long p_261471_) {
      return p_261471_ / 1000L;
   }
}
