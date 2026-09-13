package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
    public static final MapCodec<ChorusPlantBlock> CODEC = simpleCodec(ChorusPlantBlock::new);

    @Override
    public MapCodec<ChorusPlantBlock> codec() {
        return CODEC;
    }

    protected ChorusPlantBlock(final BlockBehaviour.Properties properties) {
        super(10.0F, properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return getStateWithConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    public static BlockState getStateWithConnections(final BlockGetter level, final BlockPos pos, final BlockState defaultState) {
        BlockState down = level.getBlockState(pos.below());
        BlockState up = level.getBlockState(pos.above());
        BlockState north = level.getBlockState(pos.north());
        BlockState east = level.getBlockState(pos.east());
        BlockState south = level.getBlockState(pos.south());
        BlockState west = level.getBlockState(pos.west());
        Block block = defaultState.getBlock();
        return defaultState.trySetValue(DOWN, down.is(block) || down.is(Blocks.CHORUS_FLOWER) || down.is(BlockTags.SUPPORTS_CHORUS_PLANT))
            .trySetValue(UP, up.is(block) || up.is(Blocks.CHORUS_FLOWER))
            .trySetValue(NORTH, north.is(block) || north.is(Blocks.CHORUS_FLOWER))
            .trySetValue(EAST, east.is(block) || east.is(Blocks.CHORUS_FLOWER))
            .trySetValue(SOUTH, south.is(block) || south.is(Blocks.CHORUS_FLOWER))
            .trySetValue(WEST, west.is(block) || west.is(Blocks.CHORUS_FLOWER));
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
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        } else {
            boolean connect = neighbourState.is(this)
                || neighbourState.is(Blocks.CHORUS_FLOWER)
                || directionToNeighbour == Direction.DOWN && neighbourState.is(BlockTags.SUPPORTS_CHORUS_PLANT);
            return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), connect);
        }
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        boolean blockAboveOrBelow = !level.getBlockState(pos.above()).isAir() && !belowState.isAir();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(this)) {
                if (blockAboveOrBelow) {
                    return false;
                }

                BlockState below = level.getBlockState(neighborPos.below());
                if (below.is(this) || below.is(BlockTags.SUPPORTS_CHORUS_PLANT)) {
                    return true;
                }
            }
        }

        return belowState.is(this) || belowState.is(BlockTags.SUPPORTS_CHORUS_PLANT);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }
}