package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public record ClampedNormalFloat(float mean, float deviation, float min, float max) implements FloatProvider {
   public static final MapCodec<ClampedNormalFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.FLOAT.fieldOf("mean").forGetter(ClampedNormalFloat::mean),
               Codec.FLOAT.fieldOf("deviation").forGetter(ClampedNormalFloat::deviation),
               Codec.FLOAT.fieldOf("min").forGetter(ClampedNormalFloat::min),
               Codec.FLOAT.fieldOf("max").forGetter(ClampedNormalFloat::max)
            )
            .apply(i, ClampedNormalFloat::new)
      )
      .validate(c -> c.max < c.min ? DataResult.error(() -> "Max must be larger than min: [" + c.min + ", " + c.max + "]") : DataResult.success(c));

   public static ClampedNormalFloat of(final float mean, final float deviation, final float min, final float max) {
      return new ClampedNormalFloat(mean, deviation, min, max);
   }

   @Override
   public float sample(final RandomSource random) {
      return sample(random, this.mean, this.deviation, this.min, this.max);
   }

   public static float sample(final RandomSource random, final float mean, final float deviation, final float min, final float max) {
      return Mth.clamp(Mth.normal(random, mean, deviation), min, max);
   }

   @Override
   public MapCodec<ClampedNormalFloat> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min + "-" + this.max + "]";
   }
}
