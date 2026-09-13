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
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;
import org.jspecify.annotations.Nullable;

public class BasaltColumnsFeature extends Feature<ColumnFeatureConfiguration> {
    private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of(
        Blocks.LAVA,
        Blocks.BEDROCK,
        Blocks.MAGMA_BLOCK,
        Blocks.SOUL_SAND,
        Blocks.NETHER_BRICKS,
        Blocks.NETHER_BRICK_FENCE,
        Blocks.NETHER_BRICK_STAIRS,
        Blocks.NETHER_WART,
        Blocks.CHEST,
        Blocks.SPAWNER
    );
    private static final int CLUSTERED_REACH = 5;
    private static final int CLUSTERED_SIZE = 50;
    private static final int UNCLUSTERED_REACH = 8;
    private static final int UNCLUSTERED_SIZE = 15;

    public BasaltColumnsFeature(final Codec<ColumnFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<ColumnFeatureConfiguration> context) {
        int lavaSeaLevel = context.chunkGenerator().getSeaLevel();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ColumnFeatureConfiguration config = context.config();
        if (!canPlaceAt(level, lavaSeaLevel, origin.mutable())) {
            return false;
        }

        int columnHeight = config.height().sample(random);
        boolean genereteClustered = random.nextFloat() < 0.9F;
        int reach = Math.min(columnHeight, genereteClustered ? 5 : 8);
        int count = genereteClustered ? 50 : 15;
        boolean placed = false;

        for (BlockPos pos : BlockPos.randomBetweenClosed(
            random, count, origin.getX() - reach, origin.getY(), origin.getZ() - reach, origin.getX() + reach, origin.getY(), origin.getZ() + reach
        )) {
            int blocksToPlaceY = columnHeight - pos.distManhattan(origin);
            if (blocksToPlaceY >= 0) {
                placed |= this.placeColumn(level, lavaSeaLevel, pos, blocksToPlaceY, config.reach().sample(random));
            }
        }

        return placed;
    }

    private boolean placeColumn(final LevelAccessor level, final int lavaSeaLevel, final BlockPos origin, final int columnHeight, final int reach) {
        boolean placedAny = false;

        for (BlockPos pos : BlockPos.betweenClosed(
            origin.getX() - reach, origin.getY(), origin.getZ() - reach, origin.getX() + reach, origin.getY(), origin.getZ() + reach
        )) {
            int stepLimit = pos.distManhattan(origin);
            BlockPos columnPos = isAirOrLavaOcean(level, lavaSeaLevel, pos)
                ? findSurface(level, lavaSeaLevel, pos.mutable(), stepLimit)
                : findAir(level, pos.mutable(), stepLimit);
            if (columnPos != null) {
                int blocksY = columnHeight - stepLimit / 2;
                BlockPos.MutableBlockPos cursor = columnPos.mutable();

                while (blocksY >= 0) {
                    if (isAirOrLavaOcean(level, lavaSeaLevel, cursor)) {
                        this.setBlock(level, cursor, Blocks.BASALT.defaultBlockState());
                        cursor.move(Direction.UP);
                        placedAny = true;
                    } else {
                        if (!level.getBlockState(cursor).is(Blocks.BASALT)) {
                            break;
                        }

                        cursor.move(Direction.UP);
                    }

                    blocksY--;
                }
            }
        }

        return placedAny;
    }

    private static @Nullable BlockPos findSurface(final LevelAccessor level, final int lavaSeaLevel, final BlockPos.MutableBlockPos cursor, int limit) {
        while (cursor.getY() > level.getMinY() + 1 && limit > 0) {
            limit--;
            if (canPlaceAt(level, lavaSeaLevel, cursor)) {
                return cursor;
            }

            cursor.move(Direction.DOWN);
        }

        return null;
    }

    private static boolean canPlaceAt(final LevelAccessor level, final int lavaSeaLevel, final BlockPos.MutableBlockPos cursor) {
        if (!isAirOrLavaOcean(level, lavaSeaLevel, cursor)) {
            return false;
        }

        BlockState blockState = level.getBlockState(cursor.move(Direction.DOWN));
        cursor.move(Direction.UP);
        return !blockState.isAir() && !CANNOT_PLACE_ON.contains(blockState.getBlock());
    }

    private static @Nullable BlockPos findAir(final LevelAccessor level, final BlockPos.MutableBlockPos cursor, int limit) {
        while (cursor.getY() <= level.getMaxY() && limit > 0) {
            limit--;
            BlockState blockState = level.getBlockState(cursor);
            if (CANNOT_PLACE_ON.contains(blockState.getBlock())) {
                return null;
            }

            if (blockState.isAir()) {
                return cursor;
            }

            cursor.move(Direction.UP);
        }

        return null;
    }

    private static boolean isAirOrLavaOcean(final LevelAccessor level, final int lavaSeaLevel, final BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        return blockState.isAir() || blockState.is(Blocks.LAVA) && blockPos.getY() <= lavaSeaLevel;
    }
}