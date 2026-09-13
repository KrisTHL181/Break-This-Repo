package net.minecraft.world.level.levelgen.placement;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public abstract class PlacementFilter extends PlacementModifier {
    @Override
    public final Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        return this.shouldPlace(context, random, origin) ? Stream.of(origin) : Stream.of();
    }

    protected abstract boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin);
}