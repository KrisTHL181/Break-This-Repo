package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VinesFeature extends Feature<NoneFeatureConfiguration> {
    public VinesFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        context.config();
        if (!level.isEmptyBlock(origin)) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && VineBlock.isAcceptableNeighbour(level, origin.relative(direction), direction)) {
                level.setBlock(origin, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(direction), true), 2);
                return true;
            }
        }

        return false;
    }
}