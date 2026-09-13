package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature extends AbstractHugeMushroomFeature {
    public HugeRedMushroomFeature(final Codec<HugeMushroomFeatureConfiguration> codec) {
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
        for (int dy = treeHeight - 3; dy <= treeHeight; dy++) {
            int radius = dy < treeHeight ? config.foliageRadius() : config.foliageRadius() - 1;
            int center = config.foliageRadius() - 2;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean minX = dx == -radius;
                    boolean maxX = dx == radius;
                    boolean minZ = dz == -radius;
                    boolean maxZ = dz == radius;
                    boolean xEdge = minX || maxX;
                    boolean zEdge = minZ || maxZ;
                    if (dy >= treeHeight || xEdge != zEdge) {
                        blockPos.setWithOffset(origin, dx, dy, dz);
                        BlockState state = config.capProvider().getState(level, random, origin);
                        if (state.hasProperty(HugeMushroomBlock.WEST)
                            && state.hasProperty(HugeMushroomBlock.EAST)
                            && state.hasProperty(HugeMushroomBlock.NORTH)
                            && state.hasProperty(HugeMushroomBlock.SOUTH)
                            && state.hasProperty(HugeMushroomBlock.UP)) {
                            state = state.setValue(HugeMushroomBlock.UP, dy >= treeHeight - 1)
                                .setValue(HugeMushroomBlock.WEST, dx < -center)
                                .setValue(HugeMushroomBlock.EAST, dx > center)
                                .setValue(HugeMushroomBlock.NORTH, dz < -center)
                                .setValue(HugeMushroomBlock.SOUTH, dz > center);
                        }

                        this.placeMushroomBlock(level, blockPos, state);
                    }
                }
            }
        }
    }

    @Override
    protected int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo) {
        int radius = 0;
        if (yo < treeHeight && yo >= treeHeight - 3) {
            radius = leafRadius;
        } else if (yo == treeHeight) {
            radius = leafRadius;
        }

        return radius;
    }
}