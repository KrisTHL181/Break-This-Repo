package net.minecraft.world.level;

public class FoliageColor {
   public static final int FOLIAGE_EVERGREEN = -10380959;
   public static final int FOLIAGE_BIRCH = -8345771;
   public static final int FOLIAGE_DEFAULT = -12012264;
   public static final int FOLIAGE_MANGROVE = -7158200;
   private static int[] pixels = new int[65536];

   public static void init(int[] p_46111_) {
      pixels = p_46111_;
   }

   public static int get(double p_46108_, double p_46109_) {
      return ColorMapColorUtil.get(p_46108_, p_46109_, pixels, -12012264);
   }
}
