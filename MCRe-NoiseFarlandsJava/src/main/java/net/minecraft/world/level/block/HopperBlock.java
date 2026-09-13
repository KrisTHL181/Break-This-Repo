package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class HopperBlock extends BaseEntityBlock {
    public static final MapCodec<HopperBlock> CODEC = simpleCodec(HopperBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private final Function<BlockState, VoxelShape> shapes;
    private final Map<Direction, VoxelShape> interactionShapes;

    @Override
    public MapCodec<HopperBlock> codec() {
        return CODEC;
    }

    public HopperBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
        VoxelShape inside = Block.column(12.0, 11.0, 16.0);
        this.shapes = this.makeShapes(inside);
        this.interactionShapes = ImmutableMap.<Direction, VoxelShape>builderWithExpectedSize(5)
            .putAll(Shapes.rotateHorizontal(Shapes.or(inside, Block.boxZ(4.0, 8.0, 10.0, 0.0, 4.0))))
            .put(Direction.DOWN, inside)
            .build();
    }

    private Function<BlockState, VoxelShape> makeShapes(final VoxelShape inside) {
        VoxelShape spoutlessHopperOutline = Shapes.or(Block.column(16.0, 10.0, 16.0), Block.column(8.0, 4.0, 10.0));
        VoxelShape spoutlessHopper = Shapes.join(spoutlessHopperOutline, inside, BooleanOp.ONLY_FIRST);
        Map<Direction, VoxelShape> spouts = Shapes.rotateAll(Block.boxZ(4.0, 4.0, 8.0, 0.0, 8.0), new Vec3(8.0, 6.0, 8.0).scale(0.0625));
        return this.getShapeForEachState(
            state -> Shapes.or(spoutlessHopper, Shapes.join(spouts.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND)), ENABLED
        );
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return this.interactionShapes.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction direction = context.getClickedFace().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction).setValue(ENABLED, true);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new HopperBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BlockEntityTypes.HOPPER, HopperBlockEntity::pushItemsTick);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            this.checkPoweredState(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) {
            player.openMenu(hopper);
            player.awardStat(Stats.INSPECT_HOPPER);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        this.checkPoweredState(level, pos, state);
    }

    private void checkPoweredState(final Level level, final BlockPos pos, final BlockState state) {
        boolean shouldBeOn = !level.hasNeighborSignal(pos);
        if (shouldBeOn != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, shouldBeOn), 2);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
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
        builder.add(FACING, ENABLED);
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
        if (level.getBlockEntity(pos) instanceof HopperBlockEntity hopperBlockEntity) {
            HopperBlockEntity.entityInside(level, pos, state, entity, hopperBlockEntity);
        }
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }
}