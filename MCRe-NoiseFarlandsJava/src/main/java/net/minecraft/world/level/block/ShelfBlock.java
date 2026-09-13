package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShelfBlock extends BaseEntityBlock implements SelectableSlotContainer, SideChainPartBlock, SimpleWaterloggedBlock {
    public static final MapCodec<ShelfBlock> CODEC = simpleCodec(ShelfBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART = BlockStateProperties.SIDE_CHAIN_PART;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
        Shapes.or(Block.box(0.0, 12.0, 11.0, 16.0, 16.0, 13.0), Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0), Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 13.0))
    );

    @Override
    public MapCodec<ShelfBlock> codec() {
        return CODEC;
    }

    public ShelfBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(final BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        return new ShelfBlockEntity(worldPosition, blockState);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SIDE_CHAIN_PART, WATERLOGGED);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
        this.updateNeighborsAfterPoweringDown(level, pos, state);
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        if (!level.isClientSide()) {
            boolean signal = level.hasNeighborSignal(pos);
            if (state.getValue(POWERED) != signal) {
                BlockState newState = state.setValue(POWERED, signal);
                if (!signal) {
                    newState = newState.setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED);
                }

                level.setBlock(pos, newState, 3);
                this.playSound(level, pos, signal ? SoundEvents.SHELF_ACTIVATE : SoundEvents.SHELF_DEACTIVATE);
                level.gameEvent(signal ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(newState));
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
            .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public int getRows() {
        return 1;
    }

    @Override
    public int getColumns() {
        return 3;
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
        if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelfBlockEntity && !hand.equals(InteractionHand.OFF_HAND)) {
            OptionalInt hitSlot = this.getHitSlot(hitResult, state.getValue(FACING));
            if (hitSlot.isEmpty()) {
                return InteractionResult.PASS;
            }

            Inventory inventory = player.getInventory();
            if (level.isClientSide()) {
                return inventory.getSelectedItem().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
            }

            if (!state.getValue(POWERED)) {
                boolean itemRemoved = swapSingleItem(itemStack, player, shelfBlockEntity, hitSlot.getAsInt(), inventory);
                if (itemRemoved) {
                    this.playSound(level, pos, itemStack.isEmpty() ? SoundEvents.SHELF_TAKE_ITEM : SoundEvents.SHELF_SINGLE_SWAP);
                } else {
                    if (itemStack.isEmpty()) {
                        return InteractionResult.PASS;
                    }

                    this.playSound(level, pos, SoundEvents.SHELF_PLACE_ITEM);
                }

                return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
            } else {
                ItemStack previousItem = inventory.getSelectedItem();
                boolean anySwapped = this.swapHotbar(level, pos, inventory);
                if (!anySwapped) {
                    return InteractionResult.CONSUME;
                }

                this.playSound(level, pos, SoundEvents.SHELF_MULTI_SWAP);
                return previousItem == inventory.getSelectedItem()
                    ? InteractionResult.SUCCESS
                    : InteractionResult.SUCCESS.heldItemTransformedTo(inventory.getSelectedItem());
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private static boolean swapSingleItem(
        final ItemStack itemStack, final Player player, final ShelfBlockEntity shelfBlockEntity, final int hitSlot, final Inventory inventory
    ) {
        ItemStack removedItem = shelfBlockEntity.swapItemNoUpdate(hitSlot, itemStack);
        ItemStack newInventoryItem = player.hasInfiniteMaterials() && removedItem.isEmpty() ? itemStack.copy() : removedItem;
        inventory.setItem(inventory.getSelectedSlot(), newInventoryItem);
        inventory.setChanged();
        shelfBlockEntity.setChanged(
            newInventoryItem.has(DataComponents.USE_EFFECTS) && !newInventoryItem.get(DataComponents.USE_EFFECTS).interactVibrations()
                ? null
                : GameEvent.ITEM_INTERACT_FINISH
        );
        return !removedItem.isEmpty();
    }

    private boolean swapHotbar(final Level level, final BlockPos pos, final Inventory inventory) {
        List<BlockPos> connectedBlocks = this.getAllBlocksConnectedTo(level, pos);
        if (connectedBlocks.isEmpty()) {
            return false;
        }

        boolean anySwapped = false;

        for (int shelfPartIndex = 0; shelfPartIndex < connectedBlocks.size(); shelfPartIndex++) {
            ShelfBlockEntity shelfPart = (ShelfBlockEntity)level.getBlockEntity(connectedBlocks.get(shelfPartIndex));
            if (shelfPart != null) {
                for (int slot = 0; slot < shelfPart.getContainerSize(); slot++) {
                    int inventorySlot = 9 - (connectedBlocks.size() - shelfPartIndex) * shelfPart.getContainerSize() + slot;
                    if (inventorySlot >= 0 && inventorySlot <= inventory.getContainerSize()) {
                        ItemStack placedInventoryItem = inventory.removeItemNoUpdate(inventorySlot);
                        ItemStack removedShelfItem = shelfPart.swapItemNoUpdate(slot, placedInventoryItem);
                        if (!placedInventoryItem.isEmpty() || !removedShelfItem.isEmpty()) {
                            inventory.setItem(inventorySlot, removedShelfItem);
                            anySwapped = true;
                        }
                    }
                }

                inventory.setChanged();
                shelfPart.setChanged(GameEvent.ENTITY_INTERACT);
            }
        }

        return anySwapped;
    }

    @Override
    public SideChainPart getSideChainPart(final BlockState state) {
        return state.getValue(SIDE_CHAIN_PART);
    }

    @Override
    public BlockState setSideChainPart(final BlockState state, final SideChainPart newPart) {
        return state.setValue(SIDE_CHAIN_PART, newPart);
    }

    @Override
    public Direction getFacing(final BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public boolean isConnectable(final BlockState state) {
        return state.is(BlockTags.WOODEN_SHELVES) && state.hasProperty(POWERED) && state.getValue(POWERED);
    }

    @Override
    public int getMaxChainLength() {
        return 3;
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (state.getValue(POWERED)) {
            this.updateSelfAndNeighborsOnPoweringUp(level, pos, state, oldState);
        } else {
            this.updateNeighborsAfterPoweringDown(level, pos, state);
        }
    }

    private void playSound(final LevelAccessor level, final BlockPos pos, final SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        if (level.isClientSide()) {
            return 0;
        } else if (direction != state.getValue(FACING).getOpposite()) {
            return 0;
        } else if (level.getBlockEntity(pos) instanceof ShelfBlockEntity blockEntity) {
            int item1Bit = blockEntity.getItem(0).isEmpty() ? 0 : 1;
            int item2Bit = blockEntity.getItem(1).isEmpty() ? 0 : 1;
            int item3Bit = blockEntity.getItem(2).isEmpty() ? 0 : 1;
            return item1Bit | item2Bit << 1 | item3Bit << 2;
        } else {
            return 0;
        }
    }
}