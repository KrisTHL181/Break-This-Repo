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

   public SimplexNoise(final RandomSource random) {
      this.xo = random.nextDouble() * 256.0;
      this.yo = random.nextDouble() * 256.0;
      this.zo = random.nextDouble() * 256.0;
      int i = 0;

      while (i < 256) {
         this.p[i] = i++;
      }

      for (int ix = 0; ix < 256; ix++) {
         int offset = random.nextInt(256 - ix);
         int tmp = this.p[ix];
         this.p[ix] = this.p[offset + ix];
         this.p[offset + ix] = tmp;
      }
   }

   private int p(final int x) {
      return this.p[x & 0xFF];
   }

   protected static double dot(final int[] g, final double x, final double y, final double z) {
      return g[0] * x + g[1] * y + g[2] * z;
   }

   private double getCornerNoise3D(final int index, final double x, final double y, final double z, final double base) {
      double t0 = base - x * x - y * y - z * z;
      double n0;
      if (t0 < 0.0) {
         n0 = 0.0;
      } else {
         t0 *= t0;
         n0 = t0 * t0 * dot(GRADIENT[index], x, y, z);
      }

      return n0;
   }

   public double getValue(final double xin, final double yin) {
      double s = (xin + yin) * F2;
      int i = Mth.floor(xin + s);
      int j = Mth.floor(yin + s);
      double t = (i + j) * G2;
      double X0 = i - t;
      double Y0 = j - t;
      double x0 = xin - X0;
      double y0 = yin - Y0;
      int i1;
      int j1;
      if (x0 > y0) {
         i1 = 1;
         j1 = 0;
      } else {
         i1 = 0;
         j1 = 1;
      }

      double x1 = x0 - i1 + G2;
      double y1 = y0 - j1 + G2;
      double x2 = x0 - 1.0 + 2.0 * G2;
      double y2 = y0 - 1.0 + 2.0 * G2;
      int ii = i & 0xFF;
      int jj = j & 0xFF;
      int gi0 = this.p(ii + this.p(jj)) % 12;
      int gi1 = this.p(ii + i1 + this.p(jj + j1)) % 12;
      int gi2 = this.p(ii + 1 + this.p(jj + 1)) % 12;
      double n0 = this.getCornerNoise3D(gi0, x0, y0, 0.0, 0.5);
      double n1 = this.getCornerNoise3D(gi1, x1, y1, 0.0, 0.5);
      double n2 = this.getCornerNoise3D(gi2, x2, y2, 0.0, 0.5);
      return 70.0 * (n0 + n1 + n2);
   }

   public double getValue(final double xin, final double yin, final double zin) {
      double F3 = 0.3333333333333333;
      double s = (xin + yin + zin) * 0.3333333333333333;
      int i = Mth.floor(xin + s);
      int j = Mth.floor(yin + s);
      int k = Mth.floor(zin + s);
      double G3 = 0.16666666666666666;
      double t = (i + j + k) * 0.16666666666666666;
      double X0 = i - t;
      double Y0 = j - t;
      double Z0 = k - t;
      double x0 = xin - X0;
      double y0 = yin - Y0;
      double z0 = zin - Z0;
      int i1;
      int j1;
      int k1;
      int i2;
      int j2;
      int k2;
      if (x0 >= y0) {
         if (y0 >= z0) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
         } else if (x0 >= z0) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 0;
            k2 = 1;
         } else {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 1;
            j2 = 0;
            k2 = 1;
         }
      } else if (y0 < z0) {
         i1 = 0;
         j1 = 0;
         k1 = 1;
         i2 = 0;
         j2 = 1;
         k2 = 1;
      } else if (x0 < z0) {
         i1 = 0;
         j1 = 1;
         k1 = 0;
         i2 = 0;
         j2 = 1;
         k2 = 1;
      } else {
         i1 = 0;
         j1 = 1;
         k1 = 0;
         i2 = 1;
         j2 = 1;
         k2 = 0;
      }

      double x1 = x0 - i1 + 0.16666666666666666;
      double y1 = y0 - j1 + 0.16666666666666666;
      double z1 = z0 - k1 + 0.16666666666666666;
      double x2 = x0 - i2 + 0.3333333333333333;
      double y2 = y0 - j2 + 0.3333333333333333;
      double z2 = z0 - k2 + 0.3333333333333333;
      double x3 = x0 - 1.0 + 0.5;
      double y3 = y0 - 1.0 + 0.5;
      double z3 = z0 - 1.0 + 0.5;
      int ii = i & 0xFF;
      int jj = j & 0xFF;
      int kk = k & 0xFF;
      int gi0 = this.p(ii + this.p(jj + this.p(kk))) % 12;
      int gi1 = this.p(ii + i1 + this.p(jj + j1 + this.p(kk + k1))) % 12;
      int gi2 = this.p(ii + i2 + this.p(jj + j2 + this.p(kk + k2))) % 12;
      int gi3 = this.p(ii + 1 + this.p(jj + 1 + this.p(kk + 1))) % 12;
      double n0 = this.getCornerNoise3D(gi0, x0, y0, z0, 0.6);
      double n1 = this.getCornerNoise3D(gi1, x1, y1, z1, 0.6);
      double n2 = this.getCornerNoise3D(gi2, x2, y2, z2, 0.6);
      double n3 = this.getCornerNoise3D(gi3, x3, y3, z3, 0.6);
      return 32.0 * (n0 + n1 + n2 + n3);
   }
}
