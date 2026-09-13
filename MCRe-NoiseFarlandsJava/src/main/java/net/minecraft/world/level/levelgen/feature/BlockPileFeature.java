package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
    public BlockPileFeature(final Codec<BlockPileConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<BlockPileConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPileConfiguration config = context.config();
        if (origin.getY() < level.getMinY() + 5) {
            return false;
        }

        int xr = 2 + random.nextInt(2);
        int zr = 2 + random.nextInt(2);

        for (BlockPos blockPos : BlockPos.betweenClosed(origin.offset(-xr, 0, -zr), origin.offset(xr, 1, zr))) {
            int xd = origin.getX() - blockPos.getX();
            int zd = origin.getZ() - blockPos.getZ();
            if (xd * xd + zd * zd <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F) {
                this.tryPlaceBlock(level, blockPos, random, config);
            } else if (random.nextFloat() < 0.031) {
                this.tryPlaceBlock(level, blockPos, random, config);
            }
        }

        return true;
    }

    private boolean mayPlaceOn(final LevelAccessor level, final BlockPos blockPos, final RandomSource random) {
        BlockPos below = blockPos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.is(Blocks.DIRT_PATH) ? random.nextBoolean() : belowState.isFaceSturdy(level, below, Direction.UP);
    }

    private void tryPlaceBlock(final WorldGenLevel level, final BlockPos blockPos, final RandomSource random, final BlockPileConfiguration config) {
        if (level.isEmptyBlock(blockPos) && this.mayPlaceOn(level, blockPos, random)) {
            level.setBlock(blockPos, config.stateProvider.getState(level, random, blockPos), 260);
        }
    }
}