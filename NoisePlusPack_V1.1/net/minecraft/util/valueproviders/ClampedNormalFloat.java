package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ClampedNormalFloat extends FloatProvider {
   public static final MapCodec<ClampedNormalFloat> CODEC = RecordCodecBuilder.mapCodec(
         p_146431_ -> p_146431_.group(
               Codec.FLOAT.fieldOf("mean").forGetter(p_146449_ -> p_146449_.mean),
               Codec.FLOAT.fieldOf("deviation").forGetter(p_146447_ -> p_146447_.deviation),
               Codec.FLOAT.fieldOf("min").forGetter(p_146445_ -> p_146445_.min),
               Codec.FLOAT.fieldOf("max").forGetter(p_146442_ -> p_146442_.max)
            )
            .apply(p_146431_, ClampedNormalFloat::new)
      )
      .validate(
         p_274935_ -> p_274935_.max < p_274935_.min
            ? DataResult.error(() -> "Max must be larger than min: [" + p_274935_.min + ", " + p_274935_.max + "]")
            : DataResult.success(p_274935_)
      );
   private final float mean;
   private final float deviation;
   private final float min;
   private final float max;

   public static ClampedNormalFloat of(float p_146424_, float p_146425_, float p_146426_, float p_146427_) {
      return new ClampedNormalFloat(p_146424_, p_146425_, p_146426_, p_146427_);
   }

   private ClampedNormalFloat(float p_146418_, float p_146419_, float p_146420_, float p_146421_) {
      this.mean = p_146418_;
      this.deviation = p_146419_;
      this.min = p_146420_;
      this.max = p_146421_;
   }

   @Override
   public float sample(RandomSource p_216836_) {
      return sample(p_216836_, this.mean, this.deviation, this.min, this.max);
   }

   public static float sample(RandomSource p_216838_, float p_216839_, float p_216840_, float p_216841_, float p_216842_) {
      return Mth.clamp(Mth.normal(p_216838_, p_216839_, p_216840_), p_216841_, p_216842_);
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
      return FloatProviderType.CLAMPED_NORMAL;
   }

   @Override
   public String toString() {
      return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min + "-" + this.max + "]";
   }
}
