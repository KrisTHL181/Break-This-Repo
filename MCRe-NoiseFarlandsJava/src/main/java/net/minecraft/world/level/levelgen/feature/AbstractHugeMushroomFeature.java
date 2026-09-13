package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
    public static final int MIN_MUSHROOM_HEIGHT = 4;

    public AbstractHugeMushroomFeature(final Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    protected void placeTrunk(
        final WorldGenLevel level,
        final RandomSource random,
        final BlockPos origin,
        final HugeMushroomFeatureConfiguration config,
        final int treeHeight,
        final BlockPos.MutableBlockPos blockPos
    ) {
        for (int dy = 0; dy < treeHeight; dy++) {
            blockPos.set(origin).move(Direction.UP, dy);
            this.placeMushroomBlock(level, blockPos, config.stemProvider().getState(level, random, origin));
        }
    }

    protected void placeMushroomBlock(final LevelAccessor level, final BlockPos.MutableBlockPos blockPos, final BlockState newState) {
        BlockState currentState = level.getBlockState(blockPos);
        if (currentState.isAir() || currentState.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
            this.setBlock(level, blockPos, newState);
        }
    }

    protected int getTreeHeight(final RandomSource random) {
        int treeHeight = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            treeHeight *= 2;
        }

        return treeHeight;
    }

    protected boolean isValidPosition(
        final WorldGenLevel level,
        final BlockPos origin,
        final int treeHeight,
        final BlockPos.MutableBlockPos blockPos,
        final HugeMushroomFeatureConfiguration config
    ) {
        int y = origin.getY();
        if (y >= level.getMinY() + 1 && y + treeHeight + 1 <= level.getMaxY()) {
            if (!config.canPlaceOn().test(level, origin.below())) {
                return false;
            }

            for (int dy = 0; dy <= treeHeight; dy++) {
                int radius = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius(), dy);

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockState state = level.getBlockState(blockPos.setWithOffset(origin, dx, dy, dz));
                        if (!state.isAir() && !state.is(BlockTags.LEAVES)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean place(final FeaturePlaceContext<HugeMushroomFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        HugeMushroomFeatureConfiguration config = context.config();
        int treeHeight = this.getTreeHeight(random);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        if (!this.isValidPosition(level, origin, treeHeight, blockPos, config)) {
            return false;
        }

        this.makeCap(level, random, origin, treeHeight, blockPos, config);
        this.placeTrunk(level, random, origin, config, treeHeight, blockPos);
        return true;
    }

    protected abstract int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo);

    protected abstract void makeCap(
        final WorldGenLevel level,
        final RandomSource random,
        final BlockPos origin,
        final int treeHeight,
        final BlockPos.MutableBlockPos blockPos,
        final HugeMushroomFeatureConfiguration config
    );
}