package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

public class SeagrassFeature extends Feature<ProbabilityFeatureConfiguration> {
    public SeagrassFeature(final Codec<ProbabilityFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        boolean placedAny = false;
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        ProbabilityFeatureConfiguration config = context.config();
        int x = random.nextInt(8) - random.nextInt(8);
        int z = random.nextInt(8) - random.nextInt(8);
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX() + x, origin.getZ() + z);
        BlockPos grassPos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
        if (level.getBlockState(grassPos).is(Blocks.WATER)) {
            boolean isTall = random.nextDouble() < config.probability;
            BlockState state = isTall ? Blocks.TALL_SEAGRASS.defaultBlockState() : Blocks.SEAGRASS.defaultBlockState();
            if (state.canSurvive(level, grassPos)) {
                if (isTall) {
                    BlockState upperState = state.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
                    BlockPos above = grassPos.above();
                    if (level.getBlockState(above).is(Blocks.WATER)) {
                        level.setBlock(grassPos, state, 2);
                        level.setBlock(above, upperState, 2);
                    }
                } else {
                    level.setBlock(grassPos, state, 2);
                }

                placedAny = true;
            }
        }

        return placedAny;
    }
}