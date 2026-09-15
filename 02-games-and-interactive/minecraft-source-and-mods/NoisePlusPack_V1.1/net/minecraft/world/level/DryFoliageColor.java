package net.minecraft.world.level;

public class DryFoliageColor {
   public static final int FOLIAGE_DRY_DEFAULT = -10732494;
   private static int[] pixels = new int[65536];

   public static void init(int[] p_397674_) {
      pixels = p_397674_;
   }

   public static int get(double p_395780_, double p_391263_) {
      return ColorMapColorUtil.get(p_395780_, p_391263_, pixels, -10732494);
   }
}
