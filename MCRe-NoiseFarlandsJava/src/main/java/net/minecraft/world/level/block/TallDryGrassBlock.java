package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallDryGrassBlock extends DryVegetationBlock implements BonemealableBlock {
    public static final MapCodec<TallDryGrassBlock> CODEC = simpleCodec(TallDryGrassBlock::new);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    @Override
    public MapCodec<TallDryGrassBlock> codec() {
        return CODEC;
    }

    protected TallDryGrassBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds(level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        return BonemealableBlock.hasSpreadableNeighbourPos(level, pos, Blocks.SHORT_DRY_GRASS.defaultBlockState());
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        BonemealableBlock.findSpreadableNeighbourPos(level, pos, Blocks.SHORT_DRY_GRASS.defaultBlockState())
            .ifPresent(blockPos -> level.setBlockAndUpdate(blockPos, Blocks.SHORT_DRY_GRASS.defaultBlockState()));
    }
}