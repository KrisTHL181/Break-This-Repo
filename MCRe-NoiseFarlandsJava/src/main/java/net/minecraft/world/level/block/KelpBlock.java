package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class KelpBlock extends GrowingPlantHeadBlock implements LiquidBlockContainer {
    public static final MapCodec<KelpBlock> CODEC = simpleCodec(KelpBlock::new);
    private static final double GROW_PER_TICK_PROBABILITY = 0.14;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 9.0);

    @Override
    public MapCodec<KelpBlock> codec() {
        return CODEC;
    }

    protected KelpBlock(final BlockBehaviour.Properties properties) {
        super(properties, Direction.UP, SHAPE, true, 0.14);
    }

    @Override
    protected boolean canGrowInto(final BlockState state) {
        return state.is(Blocks.WATER);
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.KELP_PLANT;
    }

    @Override
    protected boolean canAttachTo(final BlockState state) {
        return !state.is(BlockTags.CANNOT_SUPPORT_KELP);
    }

    @Override
    public boolean canPlaceLiquid(final @Nullable LivingEntity user, final BlockGetter level, final BlockPos pos, final BlockState state, final Fluid type) {
        return false;
    }

    @Override
    public boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state, final FluidState fluidState) {
        return false;
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(final RandomSource random) {
        return 1;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return fluidState.is(FluidTags.WATER) && fluidState.isFull() ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return Fluids.WATER.getSource(false);
    }
}