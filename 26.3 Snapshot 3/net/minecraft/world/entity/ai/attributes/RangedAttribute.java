package net.minecraft.world.entity.ai.attributes;

import net.minecraft.util.Mth;

public class RangedAttribute extends Attribute {
   private final double minValue;
   private final double maxValue;

   public RangedAttribute(final String descriptionId, final double defaultValue, final double minValue, final double maxValue) {
      super(descriptionId, defaultValue);
      this.minValue = minValue;
      this.maxValue = maxValue;
      if (minValue > maxValue) {
         throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
      }

      if (defaultValue < minValue) {
         throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
      }

      if (defaultValue > maxValue) {
         throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
      }
   }

   public double getMinValue() {
      return this.minValue;
   }

   public double getMaxValue() {
      return this.maxValue;
   }

   @Override
   public double sanitizeValue(final double value) {
      return Double.isNaN(value) ? this.minValue : Mth.clamp(value, this.minValue, this.maxValue);
   }
}
