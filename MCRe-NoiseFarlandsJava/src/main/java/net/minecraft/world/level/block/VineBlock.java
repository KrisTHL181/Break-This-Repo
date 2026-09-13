package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class VineBlock extends Block {
    public static final MapCodec<VineBlock> CODEC = simpleCodec(VineBlock::new);
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
        .entrySet()
        .stream()
        .filter(e -> e.getKey() != Direction.DOWN)
        .collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<VineBlock> codec() {
        return CODEC;
    }

    public VineBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
        );
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> shapes = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = Shapes.empty();

            for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (state.getValue(entry.getValue())) {
                    shape = Shapes.or(shape, shapes.get(entry.getKey()));
                }
            }

            return shape.isEmpty() ? Shapes.block() : shape;
        });
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return this.hasFaces(this.getUpdatedState(state, level, pos));
    }

    private boolean hasFaces(final BlockState blockState) {
        return this.countFaces(blockState) > 0;
    }

    private int countFaces(final BlockState blockState) {
        int count = 0;

        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            if (blockState.getValue(property)) {
                count++;
            }
        }

        return count;
    }

    private boolean canSupportAtFace(final BlockGetter level, final BlockPos pos, final Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        }

        BlockPos relative = pos.relative(direction);
        if (isAcceptableNeighbour(level, relative, direction)) {
            return true;
        }

        if (direction.getAxis() == Direction.Axis.Y) {
            return false;
        }

        BooleanProperty property = PROPERTY_BY_DIRECTION.get(direction);
        BlockState aboveState = level.getBlockState(pos.above());
        return aboveState.is(this) && aboveState.getValue(property);
    }

    public static boolean isAcceptableNeighbour(final BlockGetter level, final BlockPos neighbourPos, final Direction directionToNeighbour) {
        return MultifaceBlock.canAttachTo(level, directionToNeighbour, neighbourPos, level.getBlockState(neighbourPos));
    }

    private BlockState getUpdatedState(BlockState state, final BlockGetter level, final BlockPos pos) {
        BlockPos abovePos = pos.above();
        if (state.getValue(UP)) {
            state = state.setValue(UP, isAcceptableNeighbour(level, abovePos, Direction.DOWN));
        }

        BlockState aboveState = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty property = getPropertyForFace(direction);
            if (state.getValue(property)) {
                boolean canSupport = this.canSupportAtFace(level, pos, direction);
                if (!canSupport) {
                    if (aboveState == null) {
                        aboveState = level.getBlockState(abovePos);
                    }

                    canSupport = aboveState.is(this) && aboveState.getValue(property);
                }

                state = state.setValue(property, canSupport);
            }
        }

        return state;
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
        if (directionToNeighbour == Direction.DOWN) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }

        BlockState blockState = this.getUpdatedState(state, level, pos);
        return !this.hasFaces(blockState) ? Blocks.AIR.defaultBlockState() : blockState;
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (level.getGameRules().get(GameRules.SPREAD_VINES)) {
            if (random.nextInt(4) == 0) {
                Direction testDirection = Direction.getRandom(random);
                BlockPos abovePos = pos.above();
                if (testDirection.getAxis().isHorizontal() && !state.getValue(getPropertyForFace(testDirection))) {
                    if (this.canSpread(level, pos)) {
                        BlockPos testPos = pos.relative(testDirection);
                        BlockState edgeState = level.getBlockState(testPos);
                        if (edgeState.isAir()) {
                            Direction cwDirection = testDirection.getClockWise();
                            Direction ccwDirection = testDirection.getCounterClockWise();
                            boolean cwHasConnectingFace = state.getValue(getPropertyForFace(cwDirection));
                            boolean ccwHasConnectingFace = state.getValue(getPropertyForFace(ccwDirection));
                            BlockPos cwTestPos = testPos.relative(cwDirection);
                            BlockPos ccwTestPos = testPos.relative(ccwDirection);
                            if (cwHasConnectingFace && isAcceptableNeighbour(level, cwTestPos, cwDirection)) {
                                level.setBlock(testPos, this.defaultBlockState().setValue(getPropertyForFace(cwDirection), true), 2);
                            } else if (ccwHasConnectingFace && isAcceptableNeighbour(level, ccwTestPos, ccwDirection)) {
                                level.setBlock(testPos, this.defaultBlockState().setValue(getPropertyForFace(ccwDirection), true), 2);
                            } else {
                                Direction opposite = testDirection.getOpposite();
                                if (cwHasConnectingFace && level.isEmptyBlock(cwTestPos) && isAcceptableNeighbour(level, pos.relative(cwDirection), opposite)) {
                                    level.setBlock(cwTestPos, this.defaultBlockState().setValue(getPropertyForFace(opposite), true), 2);
                                } else if (ccwHasConnectingFace
                                    && level.isEmptyBlock(ccwTestPos)
                                    && isAcceptableNeighbour(level, pos.relative(ccwDirection), opposite)) {
                                    level.setBlock(ccwTestPos, this.defaultBlockState().setValue(getPropertyForFace(opposite), true), 2);
                                } else if (random.nextFloat() < 0.05 && isAcceptableNeighbour(level, testPos.above(), Direction.UP)) {
                                    level.setBlock(testPos, this.defaultBlockState().setValue(UP, true), 2);
                                }
                            }
                        } else if (isAcceptableNeighbour(level, testPos, testDirection)) {
                            level.setBlock(pos, state.setValue(getPropertyForFace(testDirection), true), 2);
                        }
                    }
                } else {
                    if (testDirection == Direction.UP && pos.getY() < level.getMaxY()) {
                        if (this.canSupportAtFace(level, pos, testDirection)) {
                            level.setBlock(pos, state.setValue(UP, true), 2);
                            return;
                        }

                        if (level.isEmptyBlock(abovePos)) {
                            if (!this.canSpread(level, pos)) {
                                return;
                            }

                            BlockState aboveState = state;

                            for (Direction direction : Direction.Plane.HORIZONTAL) {
                                if (random.nextBoolean() || !isAcceptableNeighbour(level, abovePos.relative(direction), direction)) {
                                    aboveState = aboveState.setValue(getPropertyForFace(direction), false);
                                }
                            }

                            if (this.hasHorizontalConnection(aboveState)) {
                                level.setBlock(abovePos, aboveState, 2);
                            }

                            return;
                        }
                    }

                    if (pos.getY() > level.getMinY()) {
                        BlockPos belowPos = pos.below();
                        BlockState belowState = level.getBlockState(belowPos);
                        if (belowState.isAir() || belowState.is(this)) {
                            BlockState before = belowState.isAir() ? this.defaultBlockState() : belowState;
                            BlockState after = this.copyRandomFaces(state, before, random);
                            if (before != after && this.hasHorizontalConnection(after)) {
                                level.setBlock(belowPos, after, 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockState copyRandomFaces(final BlockState from, BlockState to, final RandomSource random) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextBoolean()) {
                BooleanProperty propertyForFace = getPropertyForFace(direction);
                if (from.getValue(propertyForFace)) {
                    to = to.setValue(propertyForFace, true);
                }
            }
        }

        return to;
    }

    private boolean hasHorizontalConnection(final BlockState state) {
        return state.getValue(NORTH) || state.getValue(EAST) || state.getValue(SOUTH) || state.getValue(WEST);
    }

    private boolean canSpread(final BlockGetter level, final BlockPos pos) {
        int radius = 4;
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4);
        int max = 5;

        for (BlockPos blockPos : iterable) {
            if (level.getBlockState(blockPos).is(this)) {
                if (--max <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        return clickedState.is(this) ? this.countFaces(clickedState) < PROPERTY_BY_DIRECTION.size() : super.canBeReplaced(state, context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        boolean clickedVine = clickedState.is(this);
        BlockState result = clickedVine ? clickedState : this.defaultBlockState();

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction != Direction.DOWN) {
                BooleanProperty face = getPropertyForFace(direction);
                boolean faceOccupied = clickedVine && clickedState.getValue(face);
                if (!faceOccupied && this.canSupportAtFace(context.getLevel(), context.getClickedPos(), direction)) {
                    return result.setValue(face, true);
                }
            }
        }

        return clickedVine ? result : null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> (BlockState)state.setValue(NORTH, state.getValue(SOUTH))
                .setValue(EAST, state.getValue(WEST))
                .setValue(SOUTH, state.getValue(NORTH))
                .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(EAST))
                .setValue(EAST, state.getValue(SOUTH))
                .setValue(SOUTH, state.getValue(WEST))
                .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(WEST))
                .setValue(EAST, state.getValue(NORTH))
                .setValue(SOUTH, state.getValue(EAST))
                .setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT:
                return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK:
                return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default:
                return super.mirror(state, mirror);
        }
    }

    public static BooleanProperty getPropertyForFace(final Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }
}