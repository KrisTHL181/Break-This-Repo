package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
    public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }

    protected IronBarsBlock(final BlockBehaviour.Properties properties) {
        super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        BlockState northState = level.getBlockState(north);
        BlockState southState = level.getBlockState(south);
        BlockState westState = level.getBlockState(west);
        BlockState eastState = level.getBlockState(east);
        return this.defaultBlockState()
            .setValue(NORTH, this.attachsTo(northState, northState.isFaceSturdy(level, north, Direction.SOUTH)))
            .setValue(SOUTH, this.attachsTo(southState, southState.isFaceSturdy(level, south, Direction.NORTH)))
            .setValue(WEST, this.attachsTo(westState, westState.isFaceSturdy(level, west, Direction.EAST)))
            .setValue(EAST, this.attachsTo(eastState, eastState.isFaceSturdy(level, east, Direction.WEST)))
            .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
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

        return directionToNeighbour.getAxis().isHorizontal()
            ? state.setValue(
                PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                this.attachsTo(neighbourState, neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour.getOpposite()))
            )
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getVisualShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean skipRendering(final BlockState state, final BlockState neighborState, final Direction direction) {
        if (neighborState.is(this)
            || neighborState.is(BlockTags.BARS) && state.is(BlockTags.BARS) && neighborState.hasProperty(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))) {
            if (!direction.getAxis().isHorizontal()) {
                return true;
            }

            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) && neighborState.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))) {
                return true;
            }
        }

        return super.skipRendering(state, neighborState, direction);
    }

    public final boolean attachsTo(final BlockState state, final boolean faceSolid) {
        return !isExceptionForConnection(state) && faceSolid || state.getBlock() instanceof IronBarsBlock || state.is(BlockTags.WALLS);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}