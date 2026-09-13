package net.minecraft.world.entity.boss.enderdragon;

import java.util.Arrays;
import net.minecraft.util.Mth;

public class DragonFlightHistory {
   public static final int LENGTH = 64;
   private static final int MASK = 63;
   private final DragonFlightHistory.Sample[] samples = new DragonFlightHistory.Sample[64];
   private int head = -1;

   public DragonFlightHistory() {
      Arrays.fill(this.samples, new DragonFlightHistory.Sample(0.0, 0.0F));
   }

   public void copyFrom(final DragonFlightHistory history) {
      System.arraycopy(history.samples, 0, this.samples, 0, 64);
      this.head = history.head;
   }

   public void record(final double y, final float yRot) {
      DragonFlightHistory.Sample sample = new DragonFlightHistory.Sample(y, yRot);
      if (this.head < 0) {
         Arrays.fill(this.samples, sample);
      }

      if (++this.head == 64) {
         this.head = 0;
      }

      this.samples[this.head] = sample;
   }

   public DragonFlightHistory.Sample get(final int delay) {
      return this.samples[this.head - delay & 63];
   }

   public DragonFlightHistory.Sample get(final int delay, final float partialTicks) {
      DragonFlightHistory.Sample sample = this.get(delay);
      DragonFlightHistory.Sample sampleOld = this.get(delay + 1);
      return new DragonFlightHistory.Sample(Mth.lerp(partialTicks, sampleOld.y, sample.y), Mth.rotLerp(partialTicks, sampleOld.yRot, sample.yRot));
   }

   public record Sample(double y, float yRot) {
   }
}
