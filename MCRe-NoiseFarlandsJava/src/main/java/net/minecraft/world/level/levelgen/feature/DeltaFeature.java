package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;

public class DeltaFeature extends Feature<DeltaFeatureConfiguration> {
    private static final ImmutableList<Block> CANNOT_REPLACE = ImmutableList.of(
        Blocks.BEDROCK, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.SPAWNER
    );
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final double RIM_SPAWN_CHANCE = 0.9;

    public DeltaFeature(final Codec<DeltaFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<DeltaFeatureConfiguration> context) {
        boolean anyPlaced = false;
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        DeltaFeatureConfiguration config = context.config();
        BlockPos origin = context.origin();
        boolean spawnRim = random.nextDouble() < 0.9;
        int rimX = spawnRim ? config.rimSize().sample(random) : 0;
        int rimZ = spawnRim ? config.rimSize().sample(random) : 0;
        boolean hasRim = spawnRim && rimX != 0 && rimZ != 0;
        int radiusX = config.size().sample(random);
        int radiusZ = config.size().sample(random);
        int radiusLimit = Math.max(radiusX, radiusZ);

        for (BlockPos pos : BlockPos.withinManhattan(origin, radiusX, 0, radiusZ)) {
            if (pos.distManhattan(origin) > radiusLimit) {
                break;
            }

            if (isClear(level, pos, config)) {
                if (hasRim) {
                    anyPlaced = true;
                    this.setBlock(level, pos, config.rim());
                }

                BlockPos posOffset = pos.offset(rimX, 0, rimZ);
                if (isClear(level, posOffset, config)) {
                    anyPlaced = true;
                    this.setBlock(level, posOffset, config.contents());
                }
            }
        }

        return anyPlaced;
    }

    private static boolean isClear(final LevelAccessor level, final BlockPos pos, final DeltaFeatureConfiguration config) {
        BlockState state = level.getBlockState(pos);
        if (state.is(config.contents().getBlock())) {
            return false;
        }

        if (CANNOT_REPLACE.contains(state.getBlock())) {
            return false;
        }

        for (Direction d : DIRECTIONS) {
            boolean isAir = level.getBlockState(pos.relative(d)).isAir();
            if (isAir && d != Direction.UP || !isAir && d == Direction.UP) {
                return false;
            }
        }

        return true;
    }
}