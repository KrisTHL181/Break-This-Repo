package net.minecraft.world.level.levelgen.placement;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public abstract class RepeatingPlacement extends PlacementModifier {
    protected abstract int count(final RandomSource random, final BlockPos origin);

    @Override
    public Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random, final BlockPos origin) {
        return IntStream.range(0, this.count(random, origin)).mapToObj(i -> origin);
    }
}