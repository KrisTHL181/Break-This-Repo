package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlowstoneFeature extends Feature<NoneFeatureConfiguration> {
    public GlowstoneFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.isEmptyBlock(origin)) {
            return false;
        }

        BlockState aboveState = level.getBlockState(origin.above());
        if (!aboveState.is(Blocks.NETHERRACK) && !aboveState.is(Blocks.BASALT) && !aboveState.is(Blocks.BLACKSTONE)) {
            return false;
        }

        level.setBlock(origin, Blocks.GLOWSTONE.defaultBlockState(), 2);

        for (int i = 0; i < 1500; i++) {
            BlockPos placePos = origin.offset(random.nextInt(8) - random.nextInt(8), -random.nextInt(12), random.nextInt(8) - random.nextInt(8));
            if (level.getBlockState(placePos).isAir()) {
                int neighbours = 0;

                for (Direction direction : Direction.values()) {
                    if (level.getBlockState(placePos.relative(direction)).is(Blocks.GLOWSTONE)) {
                        neighbours++;
                    }

                    if (neighbours > 1) {
                        break;
                    }
                }

                if (neighbours == 1) {
                    level.setBlock(placePos, Blocks.GLOWSTONE.defaultBlockState(), 2);
                }
            }
        }

        return true;
    }
}