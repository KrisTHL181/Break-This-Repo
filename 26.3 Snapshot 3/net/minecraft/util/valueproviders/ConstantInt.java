package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public record ConstantInt(int value) implements IntProvider {
   public static final ConstantInt ZERO = new ConstantInt(0);
   public static final MapCodec<ConstantInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.INT.fieldOf("value").forGetter(ConstantInt::value)).apply(i, ConstantInt::of)
   );

   public static ConstantInt of(final int value) {
      return value == 0 ? ZERO : new ConstantInt(value);
   }

   @Override
   public int sample(final RandomSource random) {
      return this.value;
   }

   @Override
   public int minInclusive() {
      return this.value;
   }

   @Override
   public int maxInclusive() {
      return this.value;
   }

   @Override
   public MapCodec<ConstantInt> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return Integer.toString(this.value);
   }
}
