package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
    public CoralFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Optional<Block> coral = BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORAL_BLOCKS, random).map(Holder::value);
        return coral.isEmpty() ? false : this.placeFeature(level, random, origin, coral.get().defaultBlockState());
    }

    protected abstract boolean placeFeature(final LevelAccessor level, final RandomSource random, final BlockPos origin, final BlockState state);

    protected boolean placeCoralBlock(final LevelAccessor level, final RandomSource random, final BlockPos pos, final BlockState state) {
        BlockPos above = pos.above();
        BlockState targetBlockState = level.getBlockState(pos);
        if ((targetBlockState.is(Blocks.WATER) || targetBlockState.is(BlockTags.CORALS)) && level.getBlockState(above).is(Blocks.WATER)) {
            level.setBlock(pos, state, 3);
            if (random.nextFloat() < 0.25F) {
                BuiltInRegistries.BLOCK
                    .getRandomElementOf(BlockTags.CORALS, random)
                    .map(Holder::value)
                    .ifPresent(block -> level.setBlock(above, block.defaultBlockState(), 2));
            } else if (random.nextFloat() < 0.05F) {
                level.setBlock(above, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, random.nextInt(4) + 1), 2);
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.2F) {
                    BlockPos relativePos = pos.relative(direction);
                    if (level.getBlockState(relativePos).is(Blocks.WATER)) {
                        BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.WALL_CORALS, random).map(Holder::value).ifPresent(coral -> {
                            BlockState coralFanState = coral.defaultBlockState();
                            if (coralFanState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                                coralFanState = coralFanState.setValue(BaseCoralWallFanBlock.FACING, direction);
                            }

                            level.setBlock(relativePos, coralFanState, 2);
                        });
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}