package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;

public class TwoLayersFeatureSize extends FeatureSize {
   public static final MapCodec<TwoLayersFeatureSize> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.intRange(0, 81).optionalFieldOf("limit", 1).forGetter(s -> s.limit),
            Codec.intRange(0, 16).optionalFieldOf("lower_size", 0).forGetter(s -> s.lowerSize),
            Codec.intRange(0, 16).optionalFieldOf("upper_size", 1).forGetter(s -> s.upperSize),
            minClippedHeightCodec()
         )
         .apply(i, TwoLayersFeatureSize::new)
   );
   private final int limit;
   private final int lowerSize;
   private final int upperSize;

   public TwoLayersFeatureSize(final int limit, final int lowerSize, final int upperSize) {
      this(limit, lowerSize, upperSize, OptionalInt.empty());
   }

   public TwoLayersFeatureSize(final int limit, final int lowerSize, final int upperSize, final OptionalInt minClippedHeight) {
      super(minClippedHeight);
      this.limit = limit;
      this.lowerSize = lowerSize;
      this.upperSize = upperSize;
   }

   @Override
   protected FeatureSizeType<?> type() {
      return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
   }

   @Override
   public int getSizeAtHeight(final int treeHeight, final int yo) {
      return yo < this.limit ? this.lowerSize : this.upperSize;
   }
}
