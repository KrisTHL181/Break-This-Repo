package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertWellFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
    private final BlockState sand = Blocks.SAND.defaultBlockState();
    private final BlockState sandSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
    private final BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
    private final BlockState water = Blocks.WATER.defaultBlockState();

    public DesertWellFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        origin = origin.above();

        while (level.isEmptyBlock(origin) && origin.getY() > level.getMinY() + 2) {
            origin = origin.below();
        }

        if (!IS_SAND.test(level.getBlockState(origin))) {
            return false;
        }

        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                if (level.isEmptyBlock(origin.offset(ox, -1, oz)) && level.isEmptyBlock(origin.offset(ox, -2, oz))) {
                    return false;
                }
            }
        }

        for (int oy = -2; oy <= 0; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                for (int oz = -2; oz <= 2; oz++) {
                    level.setBlock(origin.offset(ox, oy, oz), this.sandstone, 2);
                }
            }
        }

        level.setBlock(origin, this.water, 2);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(origin.relative(direction), this.water, 2);
        }

        BlockPos sandCenter = origin.below();
        level.setBlock(sandCenter, this.sand, 2);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(sandCenter.relative(direction), this.sand, 2);
        }

        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                if (ox == -2 || ox == 2 || oz == -2 || oz == 2) {
                    level.setBlock(origin.offset(ox, 1, oz), this.sandstone, 2);
                }
            }
        }

        level.setBlock(origin.offset(2, 1, 0), this.sandSlab, 2);
        level.setBlock(origin.offset(-2, 1, 0), this.sandSlab, 2);
        level.setBlock(origin.offset(0, 1, 2), this.sandSlab, 2);
        level.setBlock(origin.offset(0, 1, -2), this.sandSlab, 2);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (ox == 0 && oz == 0) {
                    level.setBlock(origin.offset(ox, 4, oz), this.sandstone, 2);
                } else {
                    level.setBlock(origin.offset(ox, 4, oz), this.sandSlab, 2);
                }
            }
        }

        for (int oy = 1; oy <= 3; oy++) {
            level.setBlock(origin.offset(-1, oy, -1), this.sandstone, 2);
            level.setBlock(origin.offset(-1, oy, 1), this.sandstone, 2);
            level.setBlock(origin.offset(1, oy, -1), this.sandstone, 2);
            level.setBlock(origin.offset(1, oy, 1), this.sandstone, 2);
        }

        BlockPos waterCenter = origin;
        List<BlockPos> waterPositions = List.of(waterCenter, waterCenter.east(), waterCenter.south(), waterCenter.west(), waterCenter.north());
        RandomSource random = context.random();
        placeSusSand(level, Util.getRandom(waterPositions, random).below(1));
        placeSusSand(level, Util.getRandom(waterPositions, random).below(2));
        return true;
    }

    private static void placeSusSand(final WorldGenLevel level, final BlockPos pos) {
        level.setBlock(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 3);
        level.getBlockEntity(pos, BlockEntityTypes.BRUSHABLE_BLOCK).ifPresent(e -> e.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, (long)pos.hashCode()));
    }
}