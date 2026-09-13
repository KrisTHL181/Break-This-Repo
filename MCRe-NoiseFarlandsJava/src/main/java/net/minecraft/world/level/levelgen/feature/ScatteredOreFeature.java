package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature extends Feature<OreConfiguration> {
    private static final int MAX_DIST_FROM_ORIGIN = 7;

    public ScatteredOreFeature(final Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<OreConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OreConfiguration config = context.config();
        BlockPos origin = context.origin();
        int numberOfTries = random.nextInt(config.size + 1);
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < numberOfTries; i++) {
            this.offsetTargetPos(targetPos, random, origin, Math.min(i, 7));
            BlockState blockState = level.getBlockState(targetPos);

            for (OreConfiguration.TargetBlockState targetState : config.targetStates) {
                if (OreFeature.canPlaceOre(blockState, level::getBlockState, random, config, targetState, targetPos)) {
                    level.setBlock(targetPos, targetState.state, 2);
                    break;
                }
            }
        }

        return true;
    }

    private void offsetTargetPos(
        final BlockPos.MutableBlockPos targetPos, final RandomSource random, final BlockPos origin, final int maxDistFromOriginForThisTry
    ) {
        int xd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        int yd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        int zd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        targetPos.setWithOffset(origin, xd, yd, zd);
    }

    private int getRandomPlacementInOneAxisRelativeToOrigin(final RandomSource random, final int maxDistanceFromOrigin) {
        return Math.round((random.nextFloat() - random.nextFloat()) * maxDistanceFromOrigin);
    }
}