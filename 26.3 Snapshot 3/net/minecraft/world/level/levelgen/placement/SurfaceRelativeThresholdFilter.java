package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public record SurfaceRelativeThresholdFilter(Heightmap.Types heightmap, int minInclusive, int maxInclusive) implements PlacementFilter {
   public static final MapCodec<SurfaceRelativeThresholdFilter> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(SurfaceRelativeThresholdFilter::heightmap),
            Codec.INT.optionalFieldOf("min_inclusive", Integer.MIN_VALUE).forGetter(SurfaceRelativeThresholdFilter::minInclusive),
            Codec.INT.optionalFieldOf("max_inclusive", Integer.MAX_VALUE).forGetter(SurfaceRelativeThresholdFilter::maxInclusive)
         )
         .apply(i, SurfaceRelativeThresholdFilter::new)
   );

   public static SurfaceRelativeThresholdFilter of(final Heightmap.Types heightmap, final int minInclusive, final int maxInclusive) {
      return new SurfaceRelativeThresholdFilter(heightmap, minInclusive, maxInclusive);
   }

   @Override
   public boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
      long surfaceY = context.getHeight(this.heightmap, origin.getX(), origin.getZ());
      long minY = surfaceY + this.minInclusive;
      long maxY = surfaceY + this.maxInclusive;
      return minY <= origin.getY() && origin.getY() <= maxY;
   }

   @Override
   public MapCodec<SurfaceRelativeThresholdFilter> codec() {
      return CODEC;
   }
}
