package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;

public class SpringFeature extends Feature<SpringConfiguration> {
    public SpringFeature(final Codec<SpringConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<SpringConfiguration> context) {
        SpringConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.above()).is(config.validBlocks)) {
            return false;
        }

        if (config.requiresBlockBelow && !level.getBlockState(origin.below()).is(config.validBlocks)) {
            return false;
        }

        BlockState currentState = level.getBlockState(origin);
        if (!currentState.isAir() && !currentState.is(config.validBlocks)) {
            return false;
        }

        int placed = 0;
        int rockCount = 0;
        if (level.getBlockState(origin.west()).is(config.validBlocks)) {
            rockCount++;
        }

        if (level.getBlockState(origin.east()).is(config.validBlocks)) {
            rockCount++;
        }

        if (level.getBlockState(origin.north()).is(config.validBlocks)) {
            rockCount++;
        }

        if (level.getBlockState(origin.south()).is(config.validBlocks)) {
            rockCount++;
        }

        if (level.getBlockState(origin.below()).is(config.validBlocks)) {
            rockCount++;
        }

        int holeCount = 0;
        if (level.isEmptyBlock(origin.west())) {
            holeCount++;
        }

        if (level.isEmptyBlock(origin.east())) {
            holeCount++;
        }

        if (level.isEmptyBlock(origin.north())) {
            holeCount++;
        }

        if (level.isEmptyBlock(origin.south())) {
            holeCount++;
        }

        if (level.isEmptyBlock(origin.below())) {
            holeCount++;
        }

        if (rockCount == config.rockCount && holeCount == config.holeCount) {
            level.setBlock(origin, config.state.createLegacyBlock(), 2);
            level.scheduleTick(origin, config.state.getType(), 0);
            placed++;
        }

        return placed > 0;
    }
}