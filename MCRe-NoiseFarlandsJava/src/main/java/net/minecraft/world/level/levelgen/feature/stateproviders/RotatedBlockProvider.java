package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RotatedBlockProvider extends BlockStateProvider {
    public static final MapCodec<RotatedBlockProvider> CODEC = BlockState.CODEC
        .fieldOf("state")
        .xmap(BlockBehaviour.BlockStateBase::getBlock, Block::defaultBlockState)
        .xmap(RotatedBlockProvider::new, p -> p.block);
    private final Block block;

    public RotatedBlockProvider(final Block block) {
        this.block = block;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.ROTATED_BLOCK_PROVIDER;
    }

    @Override
    public BlockState getState(final WorldGenLevel level, final RandomSource random, final BlockPos pos) {
        Direction.Axis randomAxis = Direction.Axis.getRandom(random);
        return this.block.defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, randomAxis);
    }
}