package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
   private final SampledFloat[] values;

   public MultipliedFloats(final SampledFloat... values) {
      this.values = values;
   }

   @Override
   public float sample(final RandomSource random) {
      float result = 1.0F;

      for (SampledFloat value : this.values) {
         result *= value.sample(random);
      }

      return result;
   }

   @Override
   public String toString() {
      return "MultipliedFloats" + Arrays.toString(this.values);
   }
}
