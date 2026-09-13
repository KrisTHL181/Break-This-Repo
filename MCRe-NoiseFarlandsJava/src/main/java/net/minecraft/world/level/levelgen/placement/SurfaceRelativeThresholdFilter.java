package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceRelativeThresholdFilter extends PlacementFilter {
    public static final MapCodec<SurfaceRelativeThresholdFilter> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(c -> c.heightmap),
                Codec.INT.optionalFieldOf("min_inclusive", Integer.MIN_VALUE).forGetter(c -> c.minInclusive),
                Codec.INT.optionalFieldOf("max_inclusive", Integer.MAX_VALUE).forGetter(c -> c.maxInclusive)
            )
            .apply(i, SurfaceRelativeThresholdFilter::new)
    );
    private final Heightmap.Types heightmap;
    private final int minInclusive;
    private final int maxInclusive;

    private SurfaceRelativeThresholdFilter(final Heightmap.Types heightmap, final int minInclusive, final int maxInclusive) {
        this.heightmap = heightmap;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public static SurfaceRelativeThresholdFilter of(final Heightmap.Types heightmap, final int minInclusive, final int maxInclusive) {
        return new SurfaceRelativeThresholdFilter(heightmap, minInclusive, maxInclusive);
    }

    @Override
    protected boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        long surfaceY = context.getHeight(this.heightmap, origin.getX(), origin.getZ());
        long minY = surfaceY + this.minInclusive;
        long maxY = surfaceY + this.maxInclusive;
        return minY <= origin.getY() && origin.getY() <= maxY;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_RELATIVE_THRESHOLD_FILTER;
    }
}