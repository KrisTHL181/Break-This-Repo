package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public record ConstantFloat(float value) implements FloatProvider {
   public static final ConstantFloat ZERO = new ConstantFloat(0.0F);
   public static final MapCodec<ConstantFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantFloat::value)).apply(i, ConstantFloat::of)
   );

   public static ConstantFloat of(final float value) {
      return value == 0.0F ? ZERO : new ConstantFloat(value);
   }

   @Override
   public float sample(final RandomSource random) {
      return this.value;
   }

   @Override
   public float min() {
      return this.value;
   }

   @Override
   public float max() {
      return this.value;
   }

   @Override
   public MapCodec<ConstantFloat> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return Float.toString(this.value);
   }
}
