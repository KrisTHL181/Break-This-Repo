package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ClampedNormalInt extends IntProvider {
   public static final MapCodec<ClampedNormalInt> CODEC = RecordCodecBuilder.mapCodec(
         p_185887_ -> p_185887_.group(
               Codec.FLOAT.fieldOf("mean").forGetter(p_185905_ -> p_185905_.mean),
               Codec.FLOAT.fieldOf("deviation").forGetter(p_185903_ -> p_185903_.deviation),
               Codec.INT.fieldOf("min_inclusive").forGetter(p_326736_ -> p_326736_.minInclusive),
               Codec.INT.fieldOf("max_inclusive").forGetter(p_326737_ -> p_326737_.maxInclusive)
            )
            .apply(p_185887_, ClampedNormalInt::new)
      )
      .validate(
         p_326735_ -> p_326735_.maxInclusive < p_326735_.minInclusive
            ? DataResult.error(() -> "Max must be larger than min: [" + p_326735_.minInclusive + ", " + p_326735_.maxInclusive + "]")
            : DataResult.success(p_326735_)
      );
   private final float mean;
   private final float deviation;
   private final int minInclusive;
   private final int maxInclusive;

   public static ClampedNormalInt of(float p_185880_, float p_185881_, int p_185882_, int p_185883_) {
      return new ClampedNormalInt(p_185880_, p_185881_, p_185882_, p_185883_);
   }

   private ClampedNormalInt(float p_185874_, float p_185875_, int p_185876_, int p_185877_) {
      this.mean = p_185874_;
      this.deviation = p_185875_;
      this.minInclusive = p_185876_;
      this.maxInclusive = p_185877_;
   }

   @Override
   public int sample(RandomSource p_216844_) {
      return sample(p_216844_, this.mean, this.deviation, this.minInclusive, this.maxInclusive);
   }

   public static int sample(RandomSource p_216846_, float p_216847_, float p_216848_, float p_216849_, float p_216850_) {
      return (int)Mth.clamp(Mth.normal(p_216846_, p_216847_, p_216848_), p_216849_, p_216850_);
   }

   @Override
   public int getMinValue() {
      return this.minInclusive;
   }

   @Override
   public int getMaxValue() {
      return this.maxInclusive;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.CLAMPED_NORMAL;
   }

   @Override
   public String toString() {
      return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
