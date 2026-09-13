package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeBrownMushroomFeature extends AbstractHugeMushroomFeature {
    public HugeBrownMushroomFeature(final Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected void makeCap(
        final WorldGenLevel level,
        final RandomSource random,
        final BlockPos origin,
        final int treeHeight,
        final BlockPos.MutableBlockPos blockPos,
        final HugeMushroomFeatureConfiguration config
    ) {
        int radius = config.foliageRadius();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean minX = dx == -radius;
                boolean maxX = dx == radius;
                boolean minZ = dz == -radius;
                boolean maxZ = dz == radius;
                boolean xEdge = minX || maxX;
                boolean zEdge = minZ || maxZ;
                if (!xEdge || !zEdge) {
                    blockPos.setWithOffset(origin, dx, treeHeight, dz);
                    boolean west = minX || zEdge && dx == 1 - radius;
                    boolean east = maxX || zEdge && dx == radius - 1;
                    boolean north = minZ || xEdge && dz == 1 - radius;
                    boolean south = maxZ || xEdge && dz == radius - 1;
                    BlockState state = config.capProvider().getState(level, random, origin);
                    if (state.hasProperty(HugeMushroomBlock.WEST)
                        && state.hasProperty(HugeMushroomBlock.EAST)
                        && state.hasProperty(HugeMushroomBlock.NORTH)
                        && state.hasProperty(HugeMushroomBlock.SOUTH)) {
                        state = state.setValue(HugeMushroomBlock.WEST, west)
                            .setValue(HugeMushroomBlock.EAST, east)
                            .setValue(HugeMushroomBlock.NORTH, north)
                            .setValue(HugeMushroomBlock.SOUTH, south);
                    }

                    this.placeMushroomBlock(level, blockPos, state);
                }
            }
        }
    }

    @Override
    protected int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo) {
        return yo <= 3 ? 0 : leafRadius;
    }
}