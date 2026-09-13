package net.minecraft.world.attribute;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GaussianSampler {
   private static final int GAUSSIAN_SAMPLE_RADIUS = 2;
   private static final int GAUSSIAN_SAMPLE_BREADTH = 6;
   private static final double[] GAUSSIAN_SAMPLE_KERNEL = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

   public static <V> void sample(Vec3 p_459604_, GaussianSampler.Sampler<V> p_459950_, GaussianSampler.Accumulator<V> p_451084_) {
      p_459604_ = p_459604_.subtract(0.5, 0.5, 0.5);
      int i = Mth.floor(p_459604_.x());
      int j = Mth.floor(p_459604_.y());
      int k = Mth.floor(p_459604_.z());
      double d0 = p_459604_.x() - i;
      double d1 = p_459604_.y() - j;
      double d2 = p_459604_.z() - k;

      for (int l = 0; l < 6; l++) {
         double d3 = Mth.lerp(d2, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
         int i1 = k - 2 + l;

         for (int j1 = 0; j1 < 6; j1++) {
            double d4 = Mth.lerp(d0, GAUSSIAN_SAMPLE_KERNEL[j1 + 1], GAUSSIAN_SAMPLE_KERNEL[j1]);
            int k1 = i - 2 + j1;

            for (int l1 = 0; l1 < 6; l1++) {
               double d5 = Mth.lerp(d1, GAUSSIAN_SAMPLE_KERNEL[l1 + 1], GAUSSIAN_SAMPLE_KERNEL[l1]);
               int i2 = j - 2 + l1;
               double d6 = d4 * d5 * d3;
               V v = p_459950_.get(k1, i2, i1);
               p_451084_.accumulate(d6, v);
            }
         }
      }
   }

   @FunctionalInterface
   public interface Accumulator<V> {
      void accumulate(double var1, V var3);
   }

   @FunctionalInterface
   public interface Sampler<V> {
      V get(int var1, int var2, int var3);
   }
}
