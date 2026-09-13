package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NetherForestVegetationConfig;

public class NetherForestVegetationFeature extends Feature<NetherForestVegetationConfig> {
    public NetherForestVegetationFeature(final Codec<NetherForestVegetationConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NetherForestVegetationConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState belowState = level.getBlockState(origin.below());
        NetherForestVegetationConfig config = context.config();
        RandomSource random = context.random();
        if (!belowState.is(BlockTags.NYLIUM)) {
            return false;
        }

        int y = origin.getY();
        if (y >= level.getMinY() + 1 && y + 1 <= level.getMaxY()) {
            int placed = 0;

            for (int i = 0; i < config.spreadWidth * config.spreadWidth; i++) {
                BlockPos finalPos = origin.offset(
                    random.nextInt(config.spreadWidth) - random.nextInt(config.spreadWidth),
                    random.nextInt(config.spreadHeight) - random.nextInt(config.spreadHeight),
                    random.nextInt(config.spreadWidth) - random.nextInt(config.spreadWidth)
                );
                BlockState state = config.stateProvider.getState(level, random, finalPos);
                if (level.isEmptyBlock(finalPos) && finalPos.getY() > level.getMinY() && state.canSurvive(level, finalPos)) {
                    level.setBlock(finalPos, state, 2);
                    placed++;
                }
            }

            return placed > 0;
        } else {
            return false;
        }
    }
}