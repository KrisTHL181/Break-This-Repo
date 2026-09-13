package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class KelpFeature extends Feature<NoneFeatureConfiguration> {
    public KelpFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int placed = 0;
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        BlockPos kelpPos = new BlockPos(origin.getX(), y, origin.getZ());
        if (level.getBlockState(kelpPos).is(Blocks.WATER)) {
            BlockState stateTop = Blocks.KELP.defaultBlockState();
            BlockState state = Blocks.KELP_PLANT.defaultBlockState();
            int height = 1 + random.nextInt(10);

            for (int h = 0; h <= height; h++) {
                if (level.getBlockState(kelpPos).is(Blocks.WATER) && level.getBlockState(kelpPos.above()).is(Blocks.WATER) && state.canSurvive(level, kelpPos)) {
                    if (h == height) {
                        level.setBlock(kelpPos, stateTop.setValue(KelpBlock.AGE, random.nextInt(4) + 20), 2);
                        placed++;
                    } else {
                        level.setBlock(kelpPos, state, 2);
                    }
                } else if (h > 0) {
                    BlockPos below = kelpPos.below();
                    if (stateTop.canSurvive(level, below) && !level.getBlockState(below.below()).is(Blocks.KELP)) {
                        level.setBlock(below, stateTop.setValue(KelpBlock.AGE, random.nextInt(4) + 20), 2);
                        placed++;
                    }
                    break;
                }

                kelpPos = kelpPos.above();
            }
        }

        return placed > 0;
    }
}