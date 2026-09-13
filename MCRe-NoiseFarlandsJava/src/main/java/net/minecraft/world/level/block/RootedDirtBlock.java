package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RootedDirtBlock extends Block implements BonemealableBlock {
    public static final MapCodec<RootedDirtBlock> CODEC = simpleCodec(RootedDirtBlock::new);

    @Override
    public MapCodec<RootedDirtBlock> codec() {
        return CODEC;
    }

    public RootedDirtBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
        return level.getBlockState(pos.below()).isAir() && level.isInsideBuildHeight(pos.below());
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
        level.setBlockAndUpdate(pos.below(), Blocks.HANGING_ROOTS.defaultBlockState());
    }

    @Override
    public BlockPos getParticlePos(final BlockPos blockPos) {
        return blockPos.below();
    }
}