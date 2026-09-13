package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WallHangingSignBlock extends SignBlock implements HangingSignBlock {
    public static final MapCodec<WallHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec()).apply(i, WallHangingSignBlock::new)
    );
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction.Axis, VoxelShape> SHAPES_PLANK = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 14.0, 16.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(
        Shapes.or(SHAPES_PLANK.get(Direction.Axis.Z), Block.column(14.0, 2.0, 0.0, 10.0))
    );

    @Override
    public MapCodec<WallHangingSignBlock> codec() {
        return CODEC;
    }

    public WallHangingSignBlock(final WoodType type, final BlockBehaviour.Properties properties) {
        super(type, properties.sound(type.hangingSignSoundType()));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected InteractionResult useItemOn(
        final ItemStack itemStack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult
    ) {
        return level.getBlockEntity(pos) instanceof SignBlockEntity signEntity
                && this.shouldTryToChainAnotherHangingSign(state, player, hitResult, signEntity, itemStack)
            ? InteractionResult.PASS
            : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    private boolean shouldTryToChainAnotherHangingSign(
        final BlockState state, final Player player, final BlockHitResult hitResult, final SignBlockEntity signEntity, final ItemStack itemStack
    ) {
        return !signEntity.canExecuteClickCommands(signEntity.isFacingFrontText(player), player)
            && itemStack.getItem() instanceof HangingSignItem
            && !this.isHittingEditableSide(hitResult, state);
    }

    private boolean isHittingEditableSide(final BlockHitResult hitResult, final BlockState state) {
        return hitResult.getDirection().getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING).getAxis());
    }

    @Override
    protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return this.getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES_PLANK.get(state.getValue(FACING).getAxis());
    }

    public boolean canPlace(final BlockState state, final LevelReader level, final BlockPos pos) {
        Direction clockwise = state.getValue(FACING).getClockWise();
        Direction counterClockwise = state.getValue(FACING).getCounterClockWise();
        return this.canAttachTo(level, state, pos.relative(clockwise), counterClockwise)
            || this.canAttachTo(level, state, pos.relative(counterClockwise), clockwise);
    }

    public boolean canAttachTo(final LevelReader level, final BlockState state, final BlockPos attachPos, final Direction attachFace) {
        BlockState attachState = level.getBlockState(attachPos);
        return attachState.is(BlockTags.WALL_HANGING_SIGNS)
            ? attachState.getValue(FACING).getAxis().test(state.getValue(FACING))
            : attachState.isFaceSturdy(level, attachPos, attachFace, SupportType.FULL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal() && !direction.getAxis().test(context.getClickedFace())) {
                Direction facing = direction.getOpposite();
                state = state.setValue(FACING, facing);
                if (state.canSurvive(level, pos) && this.canPlace(state, level, pos)) {
                    return state.setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
                }
            }
        }

        return null;
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
        return directionToNeighbour.getAxis() == state.getValue(FACING).getClockWise().getAxis() && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public float getYRotationDegrees(final BlockState state) {
        return state.getValue(FACING).toYRot();
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new HangingSignBlockEntity(worldPosition, blockState);
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityTypes.HANGING_SIGN, SignBlockEntity::tick);
    }

    @Override
    public HangingSignBlock.Attachment attachmentPoint(final BlockState state) {
        return HangingSignBlock.Attachment.WALL;
    }
}