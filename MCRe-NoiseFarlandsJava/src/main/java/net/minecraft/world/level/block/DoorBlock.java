package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DoorBlock extends Block {
    public static final MapCodec<DoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type), propertiesCodec()).apply(i, DoorBlock::new)
    );
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 13.0, 16.0));
    private final BlockSetType type;

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    protected DoorBlock(final BlockSetType type, final BlockBehaviour.Properties properties) {
        super(properties.sound(type.soundType()));
        this.type = type;
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(POWERED, false)
                .setValue(HALF, DoubleBlockHalf.LOWER)
        );
    }

    public BlockSetType type() {
        return this.type;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        Direction direction = state.getValue(FACING);
        Direction doorDirection = state.getValue(OPEN)
            ? (state.getValue(HINGE) == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise())
            : direction;
        return SHAPES.get(doorDirection);
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
        if (directionToNeighbour.getAxis() != Direction.Axis.Y || half == DoubleBlockHalf.LOWER != (directionToNeighbour == Direction.UP)) {
            return half == DoubleBlockHalf.LOWER && directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        } else {
            return neighbourState.getBlock() instanceof DoorBlock && neighbourState.getValue(HALF) != half
                ? neighbourState.setValue(HALF, half)
                : Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    protected void onExplosionHit(
        final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
    ) {
        if (explosion.canTriggerBlocks() && state.getValue(HALF) == DoubleBlockHalf.LOWER && this.type.canOpenByWindCharge() && !state.getValue(POWERED)) {
            this.setOpen(null, level, state, pos, !this.isOpen(state));
        }

        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (!level.isClientSide() && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))) {
            DoublePlantBlock.preventDropFromBottomPart(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> state.getValue(OPEN);
            case WATER -> false;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
            return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(HINGE, this.getHinge(context))
                .setValue(POWERED, powered)
                .setValue(OPEN, powered)
                .setValue(HALF, DoubleBlockHalf.LOWER);
        } else {
            return null;
        }
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
        level.setBlockAndUpdate(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER));
    }

    private DoorHingeSide getHinge(final BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction placeDirection = context.getHorizontalDirection();
        BlockPos abovePos = pos.above();
        Direction leftDirection = placeDirection.getCounterClockWise();
        BlockPos leftPos = pos.relative(leftDirection);
        BlockState leftState = level.getBlockState(leftPos);
        BlockPos leftAbovePos = abovePos.relative(leftDirection);
        BlockState leftAboveState = level.getBlockState(leftAbovePos);
        Direction rightDirection = placeDirection.getClockWise();
        BlockPos rightPos = pos.relative(rightDirection);
        BlockState rightState = level.getBlockState(rightPos);
        BlockPos rightAbovePos = abovePos.relative(rightDirection);
        BlockState rightAboveState = level.getBlockState(rightAbovePos);
        int solidBlockBalance = (leftState.isCollisionShapeFullBlock(level, leftPos) ? -1 : 0)
            + (leftAboveState.isCollisionShapeFullBlock(level, leftAbovePos) ? -1 : 0)
            + (rightState.isCollisionShapeFullBlock(level, rightPos) ? 1 : 0)
            + (rightAboveState.isCollisionShapeFullBlock(level, rightAbovePos) ? 1 : 0);
        boolean doorLeft = leftState.getBlock() instanceof DoorBlock && leftState.getValue(HALF) == DoubleBlockHalf.LOWER;
        boolean doorRight = rightState.getBlock() instanceof DoorBlock && rightState.getValue(HALF) == DoubleBlockHalf.LOWER;
        if ((!doorLeft || doorRight) && solidBlockBalance <= 0) {
            if ((!doorRight || doorLeft) && solidBlockBalance >= 0) {
                int stepX = placeDirection.getStepX();
                int stepZ = placeDirection.getStepZ();
                Vec3 clickLocation = context.getClickLocation();
                double clickX = clickLocation.x - pos.getX();
                double clickZ = clickLocation.z - pos.getZ();
                return (stepX >= 0 || !(clickZ < 0.5)) && (stepX <= 0 || !(clickZ > 0.5)) && (stepZ >= 0 || !(clickX > 0.5)) && (stepZ <= 0 || !(clickX < 0.5))
                    ? DoorHingeSide.LEFT
                    : DoorHingeSide.RIGHT;
            } else {
                return DoorHingeSide.LEFT;
            }
        } else {
            return DoorHingeSide.RIGHT;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (!this.type.canOpenByHand()) {
            return InteractionResult.PASS;
        }

        state = state.cycle(OPEN);
        level.setBlock(pos, state, 10);
        this.playSound(player, level, pos, state.getValue(OPEN));
        level.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    public boolean isOpen(final BlockState state) {
        return state.getValue(OPEN);
    }

    public void setOpen(final @Nullable Entity sourceEntity, final Level level, final BlockState state, final BlockPos pos, final boolean shouldOpen) {
        if (state.is(this) && state.getValue(OPEN) != shouldOpen) {
            level.setBlock(pos, state.setValue(OPEN, shouldOpen), 10);
            this.playSound(sourceEntity, level, pos, shouldOpen);
            level.gameEvent(sourceEntity, shouldOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        boolean signal = level.hasNeighborSignal(pos)
            || level.hasNeighborSignal(pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
        if (!this.defaultBlockState().is(block) && signal != state.getValue(POWERED)) {
            if (signal != state.getValue(OPEN)) {
                this.playSound(null, level, pos, signal);
                level.gameEvent(null, signal ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }

            level.setBlock(pos, state.setValue(POWERED, signal).setValue(OPEN, signal), 2);
        }
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? belowState.isFaceSturdy(level, below, Direction.UP) : belowState.is(this);
    }

    private void playSound(final @Nullable Entity entity, final Level level, final BlockPos pos, final boolean open) {
        level.playSound(entity, pos, open ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(HINGE);
    }

    @Override
    protected long getSeed(final BlockState state, final BlockPos pos) {
        return Mth.getSeed(pos.getX(), pos.below(state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, HINGE, POWERED);
    }

    public static boolean isWoodenDoor(final Level level, final BlockPos pos) {
        return isWoodenDoor(level.getBlockState(pos));
    }

    public static boolean isWoodenDoor(final BlockState state) {
        return state.getBlock() instanceof DoorBlock door && door.type().canOpenByHand();
    }
}