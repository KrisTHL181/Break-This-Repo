package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public class TrapezoidFloat extends FloatProvider {
   public static final MapCodec<TrapezoidFloat> CODEC = RecordCodecBuilder.mapCodec(
         p_146578_ -> p_146578_.group(
               Codec.FLOAT.fieldOf("min").forGetter(p_146588_ -> p_146588_.min),
               Codec.FLOAT.fieldOf("max").forGetter(p_146586_ -> p_146586_.max),
               Codec.FLOAT.fieldOf("plateau").forGetter(p_146583_ -> p_146583_.plateau)
            )
            .apply(p_146578_, TrapezoidFloat::new)
      )
      .validate(
         p_274953_ -> {
            if (p_274953_.max < p_274953_.min) {
               return DataResult.error(() -> "Max must be larger than min: [" + p_274953_.min + ", " + p_274953_.max + "]");
            } else {
               return p_274953_.plateau > p_274953_.max - p_274953_.min
                  ? DataResult.error(() -> "Plateau can at most be the full span: [" + p_274953_.min + ", " + p_274953_.max + "]")
                  : DataResult.success(p_274953_);
            }
         }
      );
   private final float min;
   private final float max;
   private final float plateau;

   public static TrapezoidFloat of(float p_146572_, float p_146573_, float p_146574_) {
      return new TrapezoidFloat(p_146572_, p_146573_, p_146574_);
   }

   private TrapezoidFloat(float p_146567_, float p_146568_, float p_146569_) {
      this.min = p_146567_;
      this.max = p_146568_;
      this.plateau = p_146569_;
   }

   @Override
   public float sample(RandomSource p_216864_) {
      float f = this.max - this.min;
      float f1 = (f - this.plateau) / 2.0F;
      float f2 = f - f1;
      return this.min + p_216864_.nextFloat() * f2 + p_216864_.nextFloat() * f1;
   }

   @Override
   public float getMinValue() {
      return this.min;
   }

   @Override
   public float getMaxValue() {
      return this.max;
   }

   @Override
   public FloatProviderType<?> getType() {
      return FloatProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return "trapezoid(" + this.plateau + ") in [" + this.min + "-" + this.max + "]";
   }
}
