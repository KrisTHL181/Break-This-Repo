package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public record TrapezoidFloat(float min, float max, float plateau) implements FloatProvider {
   public static final MapCodec<TrapezoidFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.FLOAT.fieldOf("min").forGetter(TrapezoidFloat::min),
               Codec.FLOAT.fieldOf("max").forGetter(TrapezoidFloat::max),
               Codec.FLOAT.fieldOf("plateau").forGetter(TrapezoidFloat::plateau)
            )
            .apply(i, TrapezoidFloat::new)
      )
      .validate(
         c -> {
            if (c.max < c.min) {
               return DataResult.error(() -> "Max must be larger than min: [" + c.min + ", " + c.max + "]");
            } else {
               return c.plateau > c.max - c.min
                  ? DataResult.error(() -> "Plateau can at most be the full span: [" + c.min + ", " + c.max + "]")
                  : DataResult.success(c);
            }
         }
      );

   public static TrapezoidFloat of(final float min, final float max, final float plateau) {
      return new TrapezoidFloat(min, max, plateau);
   }

   @Override
   public float sample(final RandomSource random) {
      float range = this.max - this.min;
      float plateauStart = (range - this.plateau) / 2.0F;
      float plateauEnd = range - plateauStart;
      return this.min + random.nextFloat() * plateauEnd + random.nextFloat() * plateauStart;
   }

   @Override
   public MapCodec<TrapezoidFloat> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "trapezoid(" + this.plateau + ") in [" + this.min + "-" + this.max + "]";
   }
}
