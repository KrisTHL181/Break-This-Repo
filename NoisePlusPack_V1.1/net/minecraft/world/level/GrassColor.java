package net.minecraft.world.level;

public class GrassColor {
   private static int[] pixels = new int[65536];

   public static void init(int[] p_46419_) {
      pixels = p_46419_;
   }

   public static int get(double p_46416_, double p_46417_) {
      return ColorMapColorUtil.get(p_46416_, p_46417_, pixels, -65281);
   }

   public static int getDefaultColor() {
      return get(0.5, 1.0);
   }
}
