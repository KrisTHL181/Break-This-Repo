package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;

public class ThreeLayersFeatureSize extends FeatureSize {
   public static final MapCodec<ThreeLayersFeatureSize> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.intRange(0, 80).optionalFieldOf("limit", 1).forGetter(s -> s.limit),
            Codec.intRange(0, 80).optionalFieldOf("upper_limit", 1).forGetter(s -> s.upperLimit),
            Codec.intRange(0, 16).optionalFieldOf("lower_size", 0).forGetter(s -> s.lowerSize),
            Codec.intRange(0, 16).optionalFieldOf("middle_size", 1).forGetter(s -> s.middleSize),
            Codec.intRange(0, 16).optionalFieldOf("upper_size", 1).forGetter(s -> s.upperSize),
            minClippedHeightCodec()
         )
         .apply(i, ThreeLayersFeatureSize::new)
   );
   private final int limit;
   private final int upperLimit;
   private final int lowerSize;
   private final int middleSize;
   private final int upperSize;

   public ThreeLayersFeatureSize(
      final int limit, final int upperLimit, final int lowerSize, final int middleSize, final int upperSize, final OptionalInt minClippedHeight
   ) {
      super(minClippedHeight);
      this.limit = limit;
      this.upperLimit = upperLimit;
      this.lowerSize = lowerSize;
      this.middleSize = middleSize;
      this.upperSize = upperSize;
   }

   @Override
   protected FeatureSizeType<?> type() {
      return FeatureSizeType.THREE_LAYERS_FEATURE_SIZE;
   }

   @Override
   public int getSizeAtHeight(final int treeHeight, final int yo) {
      if (yo < this.limit) {
         return this.lowerSize;
      } else {
         return yo >= treeHeight - this.upperLimit ? this.upperSize : this.middleSize;
      }
   }
}
