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
      p_162033_ -> p_162033_.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(p_162043_ -> p_162043_.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(p_162038_ -> p_162038_.maxInclusive)
         )
         .apply(p_162033_, UniformHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final LongSet warnedFor = new LongOpenHashSet();

   private UniformHeight(VerticalAnchor p_162029_, VerticalAnchor p_162030_) {
      this.minInclusive = p_162029_;
      this.maxInclusive = p_162030_;
   }

   public static UniformHeight of(VerticalAnchor p_162035_, VerticalAnchor p_162036_) {
      return new UniformHeight(p_162035_, p_162036_);
   }

   @Override
   public int sample(RandomSource p_226308_, WorldGenerationContext p_226309_) {
      int i = this.minInclusive.resolveY(p_226309_);
      int j = this.maxInclusive.resolveY(p_226309_);
      if (i > j) {
         if (this.warnedFor.add((long)i << 32 | j)) {
            LOGGER.warn("Empty height range: {}", this);
         }

         return i;
      } else {
         return Mth.randomBetweenInclusive(p_226308_, i, j);
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
