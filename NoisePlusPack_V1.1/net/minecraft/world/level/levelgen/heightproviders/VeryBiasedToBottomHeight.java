package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class VeryBiasedToBottomHeight extends HeightProvider {
   public static final MapCodec<VeryBiasedToBottomHeight> CODEC = RecordCodecBuilder.mapCodec(
      p_162057_ -> p_162057_.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(p_162070_ -> p_162070_.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(p_162068_ -> p_162068_.maxInclusive),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter(p_162063_ -> p_162063_.inner)
         )
         .apply(p_162057_, VeryBiasedToBottomHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int inner;

   private VeryBiasedToBottomHeight(VerticalAnchor p_162052_, VerticalAnchor p_162053_, int p_162054_) {
      this.minInclusive = p_162052_;
      this.maxInclusive = p_162053_;
      this.inner = p_162054_;
   }

   public static VeryBiasedToBottomHeight of(VerticalAnchor p_162059_, VerticalAnchor p_162060_, int p_162061_) {
      return new VeryBiasedToBottomHeight(p_162059_, p_162060_, p_162061_);
   }

   @Override
   public int sample(RandomSource p_226311_, WorldGenerationContext p_226312_) {
      int i = this.minInclusive.resolveY(p_226312_);
      int j = this.maxInclusive.resolveY(p_226312_);
      if (j - i - this.inner + 1 <= 0) {
         LOGGER.warn("Empty height range: {}", this);
         return i;
      } else {
         int k = Mth.nextInt(p_226311_, i + this.inner, j);
         int l = Mth.nextInt(p_226311_, i, k - 1);
         return Mth.nextInt(p_226311_, i, l - 1 + this.inner);
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.VERY_BIASED_TO_BOTTOM;
   }

   @Override
   public String toString() {
      return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
   }
}
