package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class UniformHeight extends HeightProvider {
   public static final MapCodec<UniformHeight> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(u -> u.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(u -> u.maxInclusive)
         )
         .apply(i, UniformHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final LongSet warnedFor = new LongOpenHashSet();

   private UniformHeight(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
      this.minInclusive = minInclusive;
      this.maxInclusive = maxInclusive;
   }

   public static UniformHeight of(final VerticalAnchor minInclusive, final VerticalAnchor maxInclusive) {
      return new UniformHeight(minInclusive, maxInclusive);
   }

   @Override
   public int sample(final RandomSource random, final WorldGenerationContext context) {
      int min = this.minInclusive.resolveY(context);
      int max = this.maxInclusive.resolveY(context);
      if (min > max) {
         if (this.warnedFor.add((long)min << 32 | max)) {
            LOGGER.warn("Empty height range: {}", this);
         }

         return min;
      } else {
         return Mth.randomBetweenInclusive(random, min, max);
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.UNIFORM;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
