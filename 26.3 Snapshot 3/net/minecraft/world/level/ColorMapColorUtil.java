package net.minecraft.world.level;

public interface ColorMapColorUtil {
   static int get(final double temp, double rain, final int[] pixels, final int defaultMapColor) {
      rain *= temp;
      int x = (int)((1.0 - temp) * 255.0);
      int y = (int)((1.0 - rain) * 255.0);
      int index = y << 8 | x;
      return index >= pixels.length ? defaultMapColor : pixels[index];
   }
}
