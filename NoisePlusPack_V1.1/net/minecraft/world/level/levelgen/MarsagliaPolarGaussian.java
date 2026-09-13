package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class MarsagliaPolarGaussian {
   public final RandomSource randomSource;
   private double nextNextGaussian;
   private boolean haveNextNextGaussian;

   public MarsagliaPolarGaussian(RandomSource p_224204_) {
      this.randomSource = p_224204_;
   }

   public void reset() {
      this.haveNextNextGaussian = false;
   }

   public double nextGaussian() {
      if (this.haveNextNextGaussian) {
         this.haveNextNextGaussian = false;
         return this.nextNextGaussian;
      }

      double d0;
      double d1;
      double d2;
      do {
         d0 = 2.0 * this.randomSource.nextDouble() - 1.0;
         d1 = 2.0 * this.randomSource.nextDouble() - 1.0;
         d2 = Mth.square(d0) + Mth.square(d1);
      } while (d2 >= 1.0 || d2 == 0.0);

      double d3 = Math.sqrt(-2.0 * Math.log(d2) / d2);
      this.nextNextGaussian = d1 * d3;
      this.haveNextNextGaussian = true;
      return d0 * d3;
   }
}
