package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceWaterDepthFilter extends PlacementFilter {
    public static final MapCodec<SurfaceWaterDepthFilter> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Codec.INT.fieldOf("max_water_depth").forGetter(c -> c.maxWaterDepth)).apply(i, SurfaceWaterDepthFilter::new)
    );
    private final int maxWaterDepth;

    private SurfaceWaterDepthFilter(final int maxWaterDepth) {
        this.maxWaterDepth = maxWaterDepth;
    }

    public static SurfaceWaterDepthFilter forMaxDepth(final int maxWaterDepth) {
        return new SurfaceWaterDepthFilter(maxWaterDepth);
    }

    @Override
    protected boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        int yOceanFloor = context.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        int ySurfaceFloor = context.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX(), origin.getZ());
        return ySurfaceFloor - yOceanFloor <= this.maxWaterDepth;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_WATER_DEPTH_FILTER;
    }
}