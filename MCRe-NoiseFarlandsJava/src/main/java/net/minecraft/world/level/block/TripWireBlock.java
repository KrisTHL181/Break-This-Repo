package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireBlock extends Block {
    public static final MapCodec<TripWireBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hook").forGetter(b -> b.hook), propertiesCodec()).apply(i, TripWireBlock::new)
    );
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    public static final BooleanProperty DISARMED = BlockStateProperties.DISARMED;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape SHAPE_ATTACHED = Block.column(16.0, 1.0, 2.5);
    private static final VoxelShape SHAPE_NOT_ATTACHED = Block.column(16.0, 0.0, 8.0);
    private static final int RECHECK_PERIOD = 10;
    private final Block hook;

    @Override
    public MapCodec<TripWireBlock> codec() {
        return CODEC;
    }

    public TripWireBlock(final Block hook, final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(POWERED, false)
                .setValue(ATTACHED, false)
                .setValue(DISARMED, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
        );
        this.hook = hook;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return state.getValue(ATTACHED) ? SHAPE_ATTACHED : SHAPE_NOT_ATTACHED;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
            .setValue(NORTH, this.shouldConnectTo(level.getBlockState(pos.north()), Direction.NORTH))
            .setValue(EAST, this.shouldConnectTo(level.getBlockState(pos.east()), Direction.EAST))
            .setValue(SOUTH, this.shouldConnectTo(level.getBlockState(pos.south()), Direction.SOUTH))
            .setValue(WEST, this.shouldConnectTo(level.getBlockState(pos.west()), Direction.WEST));
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
        return directionToNeighbour.getAxis().isHorizontal()
            ? state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), this.shouldConnectTo(neighbourState, directionToNeighbour))
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            this.updateSource(level, pos, state);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        if (!movedByPiston) {
            this.updateSource(level, pos, state.setValue(POWERED, true));
        }
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (!level.isClientSide() && !player.getMainHandItem().isEmpty() && player.getMainHandItem().is(Items.SHEARS)) {
            level.setBlock(pos, state.setValue(DISARMED, true), 260);
            level.gameEvent(player, GameEvent.SHEAR, pos);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private void updateSource(final Level level, final BlockPos pos, final BlockState state) {
        for (Direction direction : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; i++) {
                BlockPos testPos = pos.relative(direction, i);
                BlockState block = level.getBlockState(testPos);
                if (block.is(this.hook)) {
                    if (block.getValue(TripWireHookBlock.FACING) == direction.getOpposite()) {
                        TripWireHookBlock.calculateState(level, testPos, block, false, true, i, state);
                    }
                    break;
                }

                if (!block.is(this)) {
                    break;
                }
            }
        }
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final Entity entity) {
        return state.getShape(level, pos);
    }

    @Override
    protected void entityInside(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Entity entity,
        final InsideBlockEffectApplier effectApplier,
        final boolean isPrecise
    ) {
        if (!level.isClientSide()) {
            if (!state.getValue(POWERED) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                this.checkPressed(level, pos, List.of(entity));
            }
        }
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (level.getBlockState(pos).getValue(POWERED)) {
            this.checkPressed(level, pos);
        }
    }

    private void checkPressed(final Level level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<? extends Entity> entities = level.getEntities(null, state.getShape(level, pos).bounds().move(pos));
        this.checkPressed(level, pos, entities);
    }

    private void checkPressed(final Level level, final BlockPos pos, final List<? extends Entity> entities) {
        BlockState state = level.getBlockState(pos);
        boolean wasPressed = state.getValue(POWERED);
        boolean shouldBePressed = false;
        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (!entity.isIgnoringBlockTriggers()) {
                    shouldBePressed = true;
                    break;
                }
            }
        }

        if (shouldBePressed != wasPressed) {
            state = state.setValue(POWERED, shouldBePressed);
            level.setBlock(pos, state, 3);
            this.updateSource(level, pos, state);
        }

        if (shouldBePressed) {
            level.scheduleTick(new BlockPos(pos), this, 10);
        } else if (wasPressed) {
            level.scheduleTick(new BlockPos(pos), this, 0);
        }
    }

    public boolean shouldConnectTo(final BlockState blockState, final Direction direction) {
        return blockState.is(this.hook) ? blockState.getValue(TripWireHookBlock.FACING) == direction.getOpposite() : blockState.is(this);
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

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
    }
}