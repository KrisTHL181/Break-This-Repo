package net.minecraft.world.level;

public class FoliageColor {
   public static final int FOLIAGE_EVERGREEN = -10380959;
   public static final int FOLIAGE_BIRCH = -8345771;
   public static final int FOLIAGE_DEFAULT = -12012264;
   public static final int FOLIAGE_MANGROVE = -7158200;
   private static int[] pixels = new int[65536];

   public static void init(final int[] pixels) {
      FoliageColor.pixels = pixels;
   }

   public static int get(final double temp, final double rain) {
      return ColorMapColorUtil.get(temp, rain, pixels, -12012264);
   }
}
