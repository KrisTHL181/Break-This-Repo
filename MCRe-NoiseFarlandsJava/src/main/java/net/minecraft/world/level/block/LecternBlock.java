package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LecternBlock extends BaseEntityBlock {
    public static final MapCodec<LecternBlock> CODEC = simpleCodec(LecternBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    private static final VoxelShape SHAPE_COLLISION = Shapes.or(Block.column(16.0, 0.0, 2.0), Block.column(8.0, 2.0, 14.0));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
        Shapes.or(
            Block.boxZ(16.0, 10.0, 14.0, 1.0, 5.333333),
            Block.boxZ(16.0, 12.0, 16.0, 5.333333, 9.666667),
            Block.boxZ(16.0, 14.0, 18.0, 9.666667, 14.0),
            SHAPE_COLLISION
        )
    );
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    @Override
    public MapCodec<LecternBlock> codec() {
        return CODEC;
    }

    protected LecternBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(HAS_BOOK, false));
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state) {
        return SHAPE_COLLISION;
    }

    @Override
    protected boolean useShapeForLightOcclusion(final BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Level level = context.getLevel();
        ItemStack itemStack = context.getItemInHand();
        Player player = context.getPlayer();
        boolean hasBook = false;
        if (!level.isClientSide() && player != null && player.canUseGameMasterBlocks()) {
            TypedEntityData<BlockEntityType<?>> blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null && blockEntityData.contains("Book")) {
                hasBook = true;
            }
        }

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, hasBook);
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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
        builder.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new LecternBlockEntity(worldPosition, blockState);
    }

    public static boolean tryPlaceBook(
        final @Nullable LivingEntity sourceEntity, final Level level, final BlockPos pos, final BlockState state, final ItemStack item
    ) {
        if (!state.getValue(HAS_BOOK)) {
            if (!level.isClientSide()) {
                placeBook(sourceEntity, level, pos, state, item);
            }

            return true;
        } else {
            return false;
        }
    }

    private static void placeBook(
        final @Nullable LivingEntity sourceEntity, final Level level, final BlockPos pos, final BlockState state, final ItemStack book
    ) {
        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
            lectern.setBook(book.consumeAndReturn(1, sourceEntity));
            resetBookState(sourceEntity, level, pos, state, true);
            level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void resetBookState(final @Nullable Entity sourceEntity, final Level level, final BlockPos pos, final BlockState state, final boolean hasBook) {
        BlockState newState = state.setValue(POWERED, false).setValue(HAS_BOOK, hasBook);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        updateBelow(level, pos, state);
    }

    public static void signalPageChange(final Level level, final BlockPos pos, final BlockState state) {
        changePowered(level, pos, state, true);
        level.scheduleTick(pos, state.getBlock(), 2);
        level.levelEvent(1043, pos, 0);
    }

    private static void changePowered(final Level level, final BlockPos pos, final BlockState state, final boolean isPowered) {
        level.setBlock(pos, state.setValue(POWERED, isPowered), 3);
        updateBelow(level, pos, state);
    }

    private static void updateBelow(final Level level, final BlockPos pos, final BlockState state) {
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, state.getValue(FACING).getOpposite(), Direction.UP);
        level.updateNeighborsAt(pos.below(), state.getBlock(), orientation);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        changePowered(level, pos, state, false);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        if (state.getValue(POWERED)) {
            updateBelow(level, pos, state);
        }
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        return direction == Direction.UP && state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return state.getValue(HAS_BOOK) && level.getBlockEntity(pos) instanceof LecternBlockEntity lecternBlockEntity
            ? lecternBlockEntity.getRedstoneSignal()
            : 0;
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
        if (state.getValue(HAS_BOOK)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        } else if (itemStack.is(ItemTags.LECTERN_BOOKS)) {
            return tryPlaceBook(player, level, pos, state, itemStack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else {
            return itemStack.isEmpty() && hand == InteractionHand.MAIN_HAND ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (state.getValue(HAS_BOOK)) {
            if (!level.isClientSide()) {
                this.openScreen(level, pos, player);
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
        return !state.getValue(HAS_BOOK) ? null : super.getMenuProvider(state, level, pos);
    }

    private void openScreen(final Level level, final BlockPos pos, final Player player) {
        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lecternBlockEntity) {
            player.openMenu(lecternBlockEntity);
            player.awardStat(Stats.INTERACT_WITH_LECTERN);
        }
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }
}