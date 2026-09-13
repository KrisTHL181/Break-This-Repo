package net.minecraft.world.level.levelgen.synth;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SimplexNoise {
   protected static final int[][] GRADIENT = new int[][]{
      {1, 1, 0},
      {-1, 1, 0},
      {1, -1, 0},
      {-1, -1, 0},
      {1, 0, 1},
      {-1, 0, 1},
      {1, 0, -1},
      {-1, 0, -1},
      {0, 1, 1},
      {0, -1, 1},
      {0, 1, -1},
      {0, -1, -1},
      {1, 1, 0},
      {0, -1, 1},
      {-1, 1, 0},
      {0, -1, -1}
   };
   private static final double SQRT_3 = Math.sqrt(3.0);
   private static final double F2 = 0.5 * (SQRT_3 - 1.0);
   private static final double G2 = (3.0 - SQRT_3) / 6.0;
   private final int[] p = new int[512];
   public final double xo;
   public final double yo;
   public final double zo;

   public SimplexNoise(RandomSource p_230549_) {
      this.xo = p_230549_.nextDouble() * 256.0;
      this.yo = p_230549_.nextDouble() * 256.0;
      this.zo = p_230549_.nextDouble() * 256.0;
      int i = 0;

      while (i < 256) {
         this.p[i] = i++;
      }

      for (int l = 0; l < 256; l++) {
         int j = p_230549_.nextInt(256 - l);
         int k = this.p[l];
         this.p[l] = this.p[j + l];
         this.p[j + l] = k;
      }
   }

   private int p(int p_75472_) {
      return this.p[p_75472_ & 0xFF];
   }

   protected static double dot(int[] p_75480_, double p_75481_, double p_75482_, double p_75483_) {
      return p_75480_[0] * p_75481_ + p_75480_[1] * p_75482_ + p_75480_[2] * p_75483_;
   }

   private double getCornerNoise3D(int p_75474_, double p_75475_, double p_75476_, double p_75477_, double p_75478_) {
      double d1 = p_75478_ - p_75475_ * p_75475_ - p_75476_ * p_75476_ - p_75477_ * p_75477_;
      double d0;
      if (d1 < 0.0) {
         d0 = 0.0;
      } else {
         d1 *= d1;
         d0 = d1 * d1 * dot(GRADIENT[p_75474_], p_75475_, p_75476_, p_75477_);
      }

      return d0;
   }

   public double getValue(double p_75465_, double p_75466_) {
      double d0 = (p_75465_ + p_75466_) * F2;
      int i = Mth.floor(p_75465_ + d0);
      int j = Mth.floor(p_75466_ + d0);
      double d1 = (i + j) * G2;
      double d2 = i - d1;
      double d3 = j - d1;
      double d4 = p_75465_ - d2;
      double d5 = p_75466_ - d3;
      int k;
      int l;
      if (d4 > d5) {
         k = 1;
         l = 0;
      } else {
         k = 0;
         l = 1;
      }

      double d6 = d4 - k + G2;
      double d7 = d5 - l + G2;
      double d8 = d4 - 1.0 + 2.0 * G2;
      double d9 = d5 - 1.0 + 2.0 * G2;
      int i1 = i & 0xFF;
      int j1 = j & 0xFF;
      int k1 = this.p(i1 + this.p(j1)) % 12;
      int l1 = this.p(i1 + k + this.p(j1 + l)) % 12;
      int i2 = this.p(i1 + 1 + this.p(j1 + 1)) % 12;
      double d10 = this.getCornerNoise3D(k1, d4, d5, 0.0, 0.5);
      double d11 = this.getCornerNoise3D(l1, d6, d7, 0.0, 0.5);
      double d12 = this.getCornerNoise3D(i2, d8, d9, 0.0, 0.5);
      return 70.0 * (d10 + d11 + d12);
   }

   public double getValue(double p_75468_, double p_75469_, double p_75470_) {
      double d0 = 0.3333333333333333;
      double d1 = (p_75468_ + p_75469_ + p_75470_) * 0.3333333333333333;
      int i = Mth.floor(p_75468_ + d1);
      int j = Mth.floor(p_75469_ + d1);
      int k = Mth.floor(p_75470_ + d1);
      double d2 = 0.16666666666666666;
      double d3 = (i + j + k) * 0.16666666666666666;
      double d4 = i - d3;
      double d5 = j - d3;
      double d6 = k - d3;
      double d7 = p_75468_ - d4;
      double d8 = p_75469_ - d5;
      double d9 = p_75470_ - d6;
      int l;
      int i1;
      int j1;
      int k1;
      int l1;
      int i2;
      if (d7 >= d8) {
         if (d8 >= d9) {
            l = 1;
            i1 = 0;
            j1 = 0;
            k1 = 1;
            l1 = 1;
            i2 = 0;
         } else if (d7 >= d9) {
            l = 1;
            i1 = 0;
            j1 = 0;
            k1 = 1;
            l1 = 0;
            i2 = 1;
         } else {
            l = 0;
            i1 = 0;
            j1 = 1;
            k1 = 1;
            l1 = 0;
            i2 = 1;
         }
      } else if (d8 < d9) {
         l = 0;
         i1 = 0;
         j1 = 1;
         k1 = 0;
         l1 = 1;
         i2 = 1;
      } else if (d7 < d9) {
         l = 0;
         i1 = 1;
         j1 = 0;
         k1 = 0;
         l1 = 1;
         i2 = 1;
      } else {
         l = 0;
         i1 = 1;
         j1 = 0;
         k1 = 1;
         l1 = 1;
         i2 = 0;
      }

      double d10 = d7 - l + 0.16666666666666666;
      double d11 = d8 - i1 + 0.16666666666666666;
      double d12 = d9 - j1 + 0.16666666666666666;
      double d13 = d7 - k1 + 0.3333333333333333;
      double d14 = d8 - l1 + 0.3333333333333333;
      double d15 = d9 - i2 + 0.3333333333333333;
      double d16 = d7 - 1.0 + 0.5;
      double d17 = d8 - 1.0 + 0.5;
      double d18 = d9 - 1.0 + 0.5;
      int j2 = i & 0xFF;
      int k2 = j & 0xFF;
      int l2 = k & 0xFF;
      int i3 = this.p(j2 + this.p(k2 + this.p(l2))) % 12;
      int j3 = this.p(j2 + l + this.p(k2 + i1 + this.p(l2 + j1))) % 12;
      int k3 = this.p(j2 + k1 + this.p(k2 + l1 + this.p(l2 + i2))) % 12;
      int l3 = this.p(j2 + 1 + this.p(k2 + 1 + this.p(l2 + 1))) % 12;
      double d19 = this.getCornerNoise3D(i3, d7, d8, d9, 0.6);
      double d20 = this.getCornerNoise3D(j3, d10, d11, d12, 0.6);
      double d21 = this.getCornerNoise3D(k3, d13, d14, d15, 0.6);
      double d22 = this.getCornerNoise3D(l3, d16, d17, d18, 0.6);
      return 32.0 * (d19 + d20 + d21 + d22);
   }
}
