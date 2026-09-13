package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

public class BambooFeature extends Feature<ProbabilityFeatureConfiguration> {
    private static final BlockState BAMBOO_TRUNK = Blocks.BAMBOO
        .defaultBlockState()
        .setValue(BambooStalkBlock.AGE, 1)
        .setValue(BambooStalkBlock.LEAVES, BambooLeaves.NONE)
        .setValue(BambooStalkBlock.STAGE, 0);
    private static final BlockState BAMBOO_FINAL_LARGE = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE, 1);
    private static final BlockState BAMBOO_TOP_LARGE = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE);
    private static final BlockState BAMBOO_TOP_SMALL = BAMBOO_TRUNK.setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL);

    public BambooFeature(final Codec<ProbabilityFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        int placed = 0;
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ProbabilityFeatureConfiguration config = context.config();
        BlockPos.MutableBlockPos bambooPos = origin.mutable();
        BlockPos.MutableBlockPos podzolPos = origin.mutable();
        if (level.isEmptyBlock(bambooPos)) {
            if (Blocks.BAMBOO.defaultBlockState().canSurvive(level, bambooPos)) {
                int height = random.nextInt(12) + 5;
                if (random.nextFloat() < config.probability) {
                    int r = random.nextInt(4) + 1;

                    for (int xx = origin.getX() - r; xx <= origin.getX() + r; xx++) {
                        for (int zz = origin.getZ() - r; zz <= origin.getZ() + r; zz++) {
                            int xd = xx - origin.getX();
                            int zd = zz - origin.getZ();
                            if (xd * xd + zd * zd <= r * r) {
                                podzolPos.set(xx, level.getHeight(Heightmap.Types.WORLD_SURFACE, xx, zz) - 1, zz);
                                if (level.getBlockState(podzolPos).is(BlockTags.BENEATH_BAMBOO_PODZOL_REPLACEABLE)) {
                                    level.setBlock(podzolPos, Blocks.PODZOL.defaultBlockState(), 2);
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < height && level.isEmptyBlock(bambooPos); i++) {
                    level.setBlock(bambooPos, BAMBOO_TRUNK, 2);
                    bambooPos.move(Direction.UP, 1);
                }

                if (bambooPos.getY() - origin.getY() >= 3) {
                    level.setBlock(bambooPos, BAMBOO_FINAL_LARGE, 2);
                    level.setBlock(bambooPos.move(Direction.DOWN, 1), BAMBOO_TOP_LARGE, 2);
                    level.setBlock(bambooPos.move(Direction.DOWN, 1), BAMBOO_TOP_SMALL, 2);
                }
            }

            placed++;
        }

        return placed > 0;
    }
}