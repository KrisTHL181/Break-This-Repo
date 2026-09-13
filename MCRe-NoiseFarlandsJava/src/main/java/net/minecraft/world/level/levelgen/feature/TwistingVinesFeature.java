package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
    public TwistingVinesFeature(final Codec<TwistingVinesConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<TwistingVinesConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (isInvalidPlacementLocation(level, origin)) {
            return false;
        }

        RandomSource random = context.random();
        TwistingVinesConfig config = context.config();
        int spreadWidth = config.spreadWidth();
        int spreadHeight = config.spreadHeight();
        int maxHeight = config.maxHeight();
        BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < spreadWidth * spreadWidth; i++) {
            placePos.set(origin)
                .move(
                    Mth.nextInt(random, -spreadWidth, spreadWidth),
                    Mth.nextInt(random, -spreadHeight, spreadHeight),
                    Mth.nextInt(random, -spreadWidth, spreadWidth)
                );
            if (findFirstAirBlockAboveGround(level, placePos) && !isInvalidPlacementLocation(level, placePos)) {
                int vineHeight = Mth.nextInt(random, 1, maxHeight);
                if (random.nextInt(6) == 0) {
                    vineHeight *= 2;
                }

                if (random.nextInt(5) == 0) {
                    vineHeight = 1;
                }

                int minAge = 17;
                int maxAge = 25;
                placeWeepingVinesColumn(level, random, placePos, vineHeight, 17, 25);
            }
        }

        return true;
    }

    private static boolean findFirstAirBlockAboveGround(final LevelAccessor level, final BlockPos.MutableBlockPos placePos) {
        do {
            placePos.move(0, -1, 0);
            if (level.isOutsideBuildHeight(placePos)) {
                return false;
            }
        } while (level.getBlockState(placePos).isAir());

        placePos.move(0, 1, 0);
        return true;
    }

    public static void placeWeepingVinesColumn(
        final LevelAccessor level,
        final RandomSource random,
        final BlockPos.MutableBlockPos placePos,
        final int totalHeight,
        final int minAge,
        final int naxAge
    ) {
        for (int height = 1; height <= totalHeight; height++) {
            if (level.isEmptyBlock(placePos)) {
                if (height == totalHeight || !level.isEmptyBlock(placePos.above())) {
                    level.setBlock(
                        placePos, Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(random, minAge, naxAge)), 2
                    );
                    break;
                }

                level.setBlock(placePos, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
            }

            placePos.move(Direction.UP);
        }
    }

    private static boolean isInvalidPlacementLocation(final LevelAccessor level, final BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return true;
        }

        BlockState stateBelow = level.getBlockState(pos.below());
        return !stateBelow.is(Blocks.NETHERRACK) && !stateBelow.is(Blocks.WARPED_NYLIUM) && !stateBelow.is(Blocks.WARPED_WART_BLOCK);
    }
}