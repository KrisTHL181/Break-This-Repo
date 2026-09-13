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

public class TrapezoidHeight extends HeightProvider {
   public static final MapCodec<TrapezoidHeight> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(u -> u.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(u -> u.maxInclusive),
            Codec.INT.optionalFieldOf("plateau", 0).forGetter(u -> u.plateau)
         )
         .apply(i, TrapezoidHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int plateau;

   private TrapezoidHeight(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive, final int plateau) {
      this.minInclusive = minInclusive;
      this.maxInclusive = maxInclusive;
      this.plateau = plateau;
   }

   public static TrapezoidHeight of(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive, final int plateau) {
      return new TrapezoidHeight(minInclusive, maxInclusive, plateau);
   }

   public static TrapezoidHeight of(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
      return of(minInclusive, maxInclusive, 0);
   }

   @Override
   public int sample(final RandomSource random, final WorldGenerationContext context) {
      int min = this.minInclusive.resolveY(context);
      int max = this.maxInclusive.resolveY(context);
      if (min > max) {
         LOGGER.warn("Empty height range: {}", this);
         return min;
      }

      int range = max - min;
      if (this.plateau >= range) {
         return Mth.randomBetweenInclusive(random, min, max);
      }

      int plateauStart = (range - this.plateau) / 2;
      int plateauEnd = range - plateauStart;
      return min + Mth.randomBetweenInclusive(random, 0, plateauEnd) + Mth.randomBetweenInclusive(random, 0, plateauStart);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return this.plateau == 0
         ? "triangle (" + this.minInclusive + "-" + this.maxInclusive + ")"
         : "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
