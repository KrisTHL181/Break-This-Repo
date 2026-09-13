package dev.miru.helper;

import dev.miru.options.TppSettings;

public class NoiseMapConvertor {
   public static double[] mapPosition(double[] pos, TppSettings option) {
      if (pos == null || pos.length != 3) {
         throw new IllegalArgumentException("Invalid position array.");
      } else if (option == null) {
         throw new IllegalArgumentException("Invalid options.");
      } else {
         return new double[]{
            option.getWgen_modifyModeValue().applyFunction.apply(pos[0], option.getScalerX(), option.getOffsetX()),
            option.getWgen_modifyModeValue().applyFunction.apply(pos[1], option.getScalerY(), option.getOffsetY()),
            option.getWgen_modifyModeValue().applyFunction.apply(pos[2], option.getScalerZ(), option.getOffsetZ())
         };
      }
   }
}
