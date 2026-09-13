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
      i -> i.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(u -> u.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(u -> u.maxInclusive),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter(u -> u.inner)
         )
         .apply(i, VeryBiasedToBottomHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int inner;

   private VeryBiasedToBottomHeight(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive, final int inner) {
      this.minInclusive = minInclusive;
      this.maxInclusive = maxInclusive;
      this.inner = inner;
   }

   public static VeryBiasedToBottomHeight of(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive, final int offset) {
      return new VeryBiasedToBottomHeight(minInclusive, maxInclusive, offset);
   }

   @Override
   public int sample(final RandomSource random, final WorldGenerationContext context) {
      int min = this.minInclusive.resolveY(context);
      int max = this.maxInclusive.resolveY(context);
      if (max - min - this.inner + 1 <= 0) {
         LOGGER.warn("Empty height range: {}", this);
         return min;
      } else {
         int upperInclusive = Mth.nextInt(random, min + this.inner, max);
         int biasedUpperInclusive = Mth.nextInt(random, min, upperInclusive - 1);
         return Mth.nextInt(random, min, biasedUpperInclusive - 1 + this.inner);
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
