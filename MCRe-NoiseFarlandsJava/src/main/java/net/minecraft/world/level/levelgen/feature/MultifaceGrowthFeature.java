package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

public class MultifaceGrowthFeature extends Feature<MultifaceGrowthConfiguration> {
    public MultifaceGrowthFeature(final Codec<MultifaceGrowthConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<MultifaceGrowthConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        MultifaceGrowthConfiguration config = context.config();
        if (!isAirOrWater(level.getBlockState(origin))) {
            return false;
        } else if (!(config.placeBlock instanceof MultifaceSpreadeableBlock placerBlock)) {
            return false;
        } else {
            List<Direction> var14 = config.getShuffledDirections(random);
            if (placeGrowthIfPossible(placerBlock, level, origin, level.getBlockState(origin), config, random, var14)) {
                return true;
            }

            BlockPos.MutableBlockPos pos = origin.mutable();

            for (Direction searchDirection : var14) {
                pos.set(origin);
                List<Direction> placementDirections = config.getShuffledDirectionsExcept(random, searchDirection.getOpposite());

                for (int i = 0; i < config.searchRange; i++) {
                    pos.setWithOffset(origin, searchDirection);
                    BlockState state = level.getBlockState(pos);
                    if (!isAirOrWater(state) && !state.is(config.placeBlock)) {
                        break;
                    }

                    if (placeGrowthIfPossible(placerBlock, level, pos, state, config, random, placementDirections)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static boolean placeGrowthIfPossible(
        final MultifaceSpreadeableBlock placerBlock,
        final WorldGenLevel level,
        final BlockPos pos,
        final BlockState oldState,
        final MultifaceGrowthConfiguration config,
        final RandomSource random,
        final List<Direction> placementDirections
    ) {
        BlockPos.MutableBlockPos mutable = pos.mutable();

        for (Direction placementDirection : placementDirections) {
            BlockState neighbourState = level.getBlockState(mutable.setWithOffset(pos, placementDirection));
            if (neighbourState.is(config.canBePlacedOn)) {
                BlockState newState = placerBlock.getStateForPlacement(oldState, level, pos, placementDirection);
                if (newState == null) {
                    return false;
                }

                level.setBlock(pos, newState, 3);
                level.getChunk(pos).markPosForPostProcessing(pos);
                if (random.nextFloat() < config.chanceOfSpreading) {
                    placerBlock.getSpreader().spreadFromFaceTowardRandomDirection(newState, level, pos, placementDirection, random, true);
                }

                return true;
            }
        }

        return false;
    }

    private static boolean isAirOrWater(final BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
}
