package com.mojang.renderpearl.backend.vulkan.glsl;

class SpvcUtil {
   private SpvcUtil() {
   }

   public static String imageDimensionToString(final int dimension) {
      return switch (dimension) {
         case 0 -> "1D";
         case 1 -> "2D";
         case 2 -> "3D";
         case 3 -> "Cube";
         case 4 -> "Rect";
         case 5 -> "Buffer";
         case 6 -> "SubpassData";
         default -> "0x" + Integer.toHexString(dimension);
      };
   }
}
