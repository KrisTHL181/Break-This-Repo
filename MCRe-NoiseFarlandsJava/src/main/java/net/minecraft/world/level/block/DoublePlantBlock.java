package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class DoublePlantBlock extends VegetationBlock {
    public static final MapCodec<DoublePlantBlock> CODEC = simpleCodec(DoublePlantBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    public DoublePlantBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (directionToNeighbour.getAxis() != Direction.Axis.Y
            || half == DoubleBlockHalf.LOWER != (directionToNeighbour == Direction.UP)
            || neighbourState.is(this) && neighbourState.getValue(HALF) != half) {
            return half == DoubleBlockHalf.LOWER && directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context) ? super.getStateForPlacement(context) : null;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
        BlockPos abovePos = pos.above();
        level.setBlockAndUpdate(abovePos, copyWaterloggedFrom(level, abovePos, this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER)));
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return super.canSurvive(state, level, pos);
        }

        BlockState belowState = level.getBlockState(pos.below());
        return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    public static void placeAt(final LevelAccessor level, final BlockState state, final BlockPos lowerPos, final @Block.UpdateFlags int updateType) {
        BlockPos upperPos = lowerPos.above();
        level.setBlock(lowerPos, copyWaterloggedFrom(level, lowerPos, state.setValue(HALF, DoubleBlockHalf.LOWER)), updateType);
        level.setBlock(upperPos, copyWaterloggedFrom(level, upperPos, state.setValue(HALF, DoubleBlockHalf.UPPER)), updateType);
    }

    public static BlockState copyWaterloggedFrom(final LevelReader level, final BlockPos pos, final BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, level.isWaterAt(pos)) : state;
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (!level.isClientSide()) {
            if (player.preventsBlockDrops()) {
                preventDropFromBottomPart(level, pos, state, player);
            } else {
                dropResources(state, level, pos, null, player, player.getMainHandItem());
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(
        final Level level,
        final Player player,
        final BlockPos pos,
        final BlockState state,
        final @Nullable BlockEntity blockEntity,
        final ItemStack destroyedWith
    ) {
        super.playerDestroy(level, player, pos, Blocks.AIR.defaultBlockState(), blockEntity, destroyedWith);
    }

    protected static void preventDropFromBottomPart(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        DoubleBlockHalf part = state.getValue(HALF);
        if (part == DoubleBlockHalf.UPPER) {
            BlockPos bottomPos = pos.below();
            BlockState bottomState = level.getBlockState(bottomPos);
            if (bottomState.is(state.getBlock()) && bottomState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState blockState = bottomState.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(bottomPos, blockState, 35);
                level.levelEvent(player, 2001, bottomPos, Block.getId(bottomState));
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected long getSeed(final BlockState state, final BlockPos pos) {
        return Mth.getSeed(pos.getX(), pos.below(state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }
}