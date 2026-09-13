package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScaffoldingBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);
    private static final int TICK_DELAY = 1;
    private static final VoxelShape SHAPE_STABLE = Shapes.or(
        Block.column(16.0, 14.0, 16.0), Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 2.0)).values().stream().reduce(Shapes.empty(), Shapes::or)
    );
    private static final VoxelShape SHAPE_UNSTABLE_BOTTOM = Block.column(16.0, 0.0, 2.0);
    private static final VoxelShape SHAPE_UNSTABLE = Shapes.or(
        SHAPE_STABLE, SHAPE_UNSTABLE_BOTTOM, Shapes.rotateHorizontal(Block.boxZ(16.0, 0.0, 2.0, 0.0, 2.0)).values().stream().reduce(Shapes.empty(), Shapes::or)
    );
    private static final VoxelShape SHAPE_BELOW_BLOCK = Shapes.block().move(0.0, -1.0, 0.0).optimize();
    public static final int STABILITY_MAX_DISTANCE = 7;
    public static final IntegerProperty DISTANCE = BlockStateProperties.STABILITY_DISTANCE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

    @Override
    public MapCodec<ScaffoldingBlock> codec() {
        return CODEC;
    }

    protected ScaffoldingBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, WATERLOGGED, BOTTOM);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (!context.isHoldingItem(state.getBlock().asItem())) {
            return state.getValue(BOTTOM) ? SHAPE_UNSTABLE : SHAPE_STABLE;
        } else {
            return Shapes.block();
        }
    }

    @Override
    protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
        return context.getItemInHand().is(this.asItem());
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        int distance = getDistance(level, pos);
        return this.defaultBlockState()
            .setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER))
            .setValue(DISTANCE, distance)
            .setValue(BOTTOM, this.isBottom(level, pos, distance));
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
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
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (!level.isClientSide()) {
            ticks.scheduleTick(pos, this, 1);
        }

        return state;
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        int distance = getDistance(level, pos);
        BlockState newState = state.setValue(DISTANCE, distance).setValue(BOTTOM, this.isBottom(level, pos, distance));
        if (newState.getValue(DISTANCE) == 7) {
            if (state.getValue(DISTANCE) == 7) {
                FallingBlockEntity.fall(level, pos, newState);
            } else {
                level.destroyBlock(pos, true);
            }
        } else if (state != newState) {
            level.setBlock(pos, newState, 3);
        }
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return getDistance(level, pos) < 7;
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (context.isPlacement()) {
            return Shapes.empty();
        } else if (context.isAbove(Shapes.block(), pos, true) && !context.isDescending()) {
            return SHAPE_STABLE;
        } else {
            return state.getValue(DISTANCE) != 0 && state.getValue(BOTTOM) && context.isAbove(SHAPE_BELOW_BLOCK, pos, true)
                ? SHAPE_UNSTABLE_BOTTOM
                : Shapes.empty();
        }
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private boolean isBottom(final BlockGetter level, final BlockPos pos, final int distance) {
        return distance > 0 && !level.getBlockState(pos.below()).is(this);
    }

    public static int getDistance(final BlockGetter level, final BlockPos pos) {
        BlockPos.MutableBlockPos relativePos = pos.mutable().move(Direction.DOWN);
        BlockState belowState = level.getBlockState(relativePos);
        int distance = 7;
        if (belowState.is(Blocks.SCAFFOLDING)) {
            distance = belowState.getValue(DISTANCE);
        } else if (belowState.isFaceSturdy(level, relativePos, Direction.UP)) {
            return 0;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState relativeState = level.getBlockState(relativePos.setWithOffset(pos, direction));
            if (relativeState.is(Blocks.SCAFFOLDING)) {
                distance = Math.min(distance, relativeState.getValue(DISTANCE) + 1);
                if (distance == 1) {
                    break;
                }
            }
        }

        return distance;
    }
}