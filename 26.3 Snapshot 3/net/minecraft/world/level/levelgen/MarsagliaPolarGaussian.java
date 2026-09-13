package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class MarsagliaPolarGaussian {
   public final RandomSource randomSource;
   private double nextNextGaussian;
   private boolean haveNextNextGaussian;

   public MarsagliaPolarGaussian(final RandomSource randomSource) {
      this.randomSource = randomSource;
   }

   public void reset() {
      this.haveNextNextGaussian = false;
   }

   public double nextGaussian() {
      if (this.haveNextNextGaussian) {
         this.haveNextNextGaussian = false;
         return this.nextNextGaussian;
      }

      double x;
      double y;
      double radiusSquared;
      do {
         x = 2.0 * this.randomSource.nextDouble() - 1.0;
         y = 2.0 * this.randomSource.nextDouble() - 1.0;
         radiusSquared = Mth.square(x) + Mth.square(y);
      } while (radiusSquared >= 1.0 || radiusSquared == 0.0);

      double multiplier = Math.sqrt(-2.0 * Math.log(radiusSquared) / radiusSquared);
      this.nextNextGaussian = y * multiplier;
      this.haveNextNextGaussian = true;
      return x * multiplier;
   }
}
