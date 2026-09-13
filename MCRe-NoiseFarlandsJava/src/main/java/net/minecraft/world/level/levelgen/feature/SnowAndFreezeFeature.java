package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SnowAndFreezeFeature extends Feature<NoneFeatureConfiguration> {
    public SnowAndFreezeFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos topPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                topPos.set(x, y, z);
                belowPos.set(topPos).move(Direction.DOWN, 1);
                Biome biome = level.getBiome(topPos).value();
                if (biome.shouldFreeze(level, belowPos, false)) {
                    level.setBlock(belowPos, Blocks.ICE.defaultBlockState(), 2);
                }

                if (biome.shouldSnow(level, topPos)) {
                    level.setBlock(topPos, Blocks.SNOW.defaultBlockState(), 2);
                    BlockState belowState = level.getBlockState(belowPos);
                    if (belowState.hasProperty(SnowyBlock.SNOWY)) {
                        level.setBlock(belowPos, belowState.setValue(SnowyBlock.SNOWY, true), 2);
                    }
                }
            }
        }

        return true;
    }
}