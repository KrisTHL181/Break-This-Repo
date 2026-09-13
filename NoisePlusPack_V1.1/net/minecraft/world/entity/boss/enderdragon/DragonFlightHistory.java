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

   public void copyFrom(DragonFlightHistory p_366507_) {
      System.arraycopy(p_366507_.samples, 0, this.samples, 0, 64);
      this.head = p_366507_.head;
   }

   public void record(double p_362558_, float p_366669_) {
      DragonFlightHistory.Sample dragonflighthistory$sample = new DragonFlightHistory.Sample(p_362558_, p_366669_);
      if (this.head < 0) {
         Arrays.fill(this.samples, dragonflighthistory$sample);
      }

      if (++this.head == 64) {
         this.head = 0;
      }

      this.samples[this.head] = dragonflighthistory$sample;
   }

   public DragonFlightHistory.Sample get(int p_365431_) {
      return this.samples[this.head - p_365431_ & 63];
   }

   public DragonFlightHistory.Sample get(int p_369995_, float p_366809_) {
      DragonFlightHistory.Sample dragonflighthistory$sample = this.get(p_369995_);
      DragonFlightHistory.Sample dragonflighthistory$sample1 = this.get(p_369995_ + 1);
      return new DragonFlightHistory.Sample(
         Mth.lerp(p_366809_, dragonflighthistory$sample1.y, dragonflighthistory$sample.y),
         Mth.rotLerp(p_366809_, dragonflighthistory$sample1.yRot, dragonflighthistory$sample.yRot)
      );
   }

   public record Sample(double y, float yRot) {
   }
}
