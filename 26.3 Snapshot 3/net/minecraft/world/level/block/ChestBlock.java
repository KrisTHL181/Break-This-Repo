package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ChestBlock extends AbstractChestBlock<ChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final int EVENT_SET_OPEN_COUNT = 1;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);
   private static final Map<Direction, VoxelShape> HALF_SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));
   private final SoundEvent openSound;
   private final SoundEvent closeSound;
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>> CHEST_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>>() {
      public Optional<Container> acceptDouble(final ChestBlockEntity first, final ChestBlockEntity second) {
         return Optional.of(new CompoundContainer(first, second));
      }

      public Optional<Container> acceptSingle(final ChestBlockEntity single) {
         return Optional.of(single);
      }

      public Optional<Container> acceptNone() {
         return Optional.empty();
      }
   };
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {
      public Optional<MenuProvider> acceptDouble(final ChestBlockEntity first, final ChestBlockEntity second) {
         final Container container = new CompoundContainer(first, second);
         return Optional.of(new MenuProvider() {
            @Override
            public @Nullable AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
               if (first.canOpen(player) && second.canOpen(player)) {
                  first.unpackLootTable(inventory.player);
                  second.unpackLootTable(inventory.player);
                  return ChestMenu.sixRows(containerId, inventory, container);
               } else {
                  Direction connectedDirection = ChestBlock.getConnectedDirection(first.getBlockState());
                  Vec3 firstCenter = Vec3.atCenterOf(first.getBlockPos());
                  Vec3 centerBetweenChests = firstCenter.add(connectedDirection.getStepX() / 2.0, 0.0, connectedDirection.getStepZ() / 2.0);
                  BaseContainerBlockEntity.sendChestLockedNotifications(centerBetweenChests, player, this.getDisplayName());
                  return null;
               }
            }

            @Override
            public Component getDisplayName() {
               if (first.hasCustomName()) {
                  return first.getDisplayName();
               } else {
                  return second.hasCustomName() ? second.getDisplayName() : Component.translatable("container.chestDouble");
               }
            }
         });
      }

      public Optional<MenuProvider> acceptSingle(final ChestBlockEntity single) {
         return Optional.of(single);
      }

      public Optional<MenuProvider> acceptNone() {
         return Optional.empty();
      }
   };

   protected ChestBlock(
      final Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType,
      final SoundEvent openSound,
      final SoundEvent closeSound,
      final BlockBehaviour.Properties properties
   ) {
      super(properties, blockEntityType);
      this.openSound = openSound;
      this.closeSound = closeSound;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false));
   }

   public static DoubleBlockCombiner.BlockType getBlockType(final BlockState state) {
      ChestType type = state.getValue(TYPE);
      if (type == ChestType.SINGLE) {
         return DoubleBlockCombiner.BlockType.SINGLE;
      } else {
         return type == ChestType.RIGHT ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
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

      if (this.chestCanConnectTo(neighbourState) && directionToNeighbour.getAxis().isHorizontal()) {
         ChestType neighbourType = neighbourState.getValue(TYPE);
         if (state.getValue(TYPE) == ChestType.SINGLE
            && neighbourType != ChestType.SINGLE
            && state.getValue(FACING) == neighbourState.getValue(FACING)
            && getConnectedDirection(neighbourState) == directionToNeighbour.getOpposite()) {
            return state.setValue(TYPE, neighbourType.getOpposite());
         }
      } else if (getConnectedDirection(state) == directionToNeighbour) {
         return state.setValue(TYPE, ChestType.SINGLE);
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   public boolean chestCanConnectTo(final BlockState blockState) {
      return blockState.is(this);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return switch ((ChestType)state.getValue(TYPE)) {
         case SINGLE -> SHAPE;
         case LEFT, RIGHT -> (VoxelShape)HALF_SHAPES.get(getConnectedDirection(state));
      };
   }

   public static Direction getConnectedDirection(final BlockState state) {
      Direction facing = state.getValue(FACING);
      return state.getValue(TYPE) == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
   }

   public static BlockPos getConnectedBlockPos(final BlockPos pos, final BlockState state) {
      Direction connectedDirection = getConnectedDirection(state);
      return pos.relative(connectedDirection);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      ChestType type = ChestType.SINGLE;
      Direction facingDirection = context.getHorizontalDirection().getOpposite();
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      boolean secondaryUse = context.isSecondaryUseActive();
      Direction clickedFace = context.getClickedFace();
      if (clickedFace.getAxis().isHorizontal() && secondaryUse) {
         Direction neighbourFacing = this.candidatePartnerFacing(context.getLevel(), context.getClickedPos(), clickedFace.getOpposite());
         if (neighbourFacing != null && neighbourFacing.getAxis() != clickedFace.getAxis()) {
            facingDirection = neighbourFacing;
            type = facingDirection.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
         }
      }

      if (type == ChestType.SINGLE && !secondaryUse) {
         type = this.getChestType(context.getLevel(), context.getClickedPos(), facingDirection);
      }

      return this.defaultBlockState().setValue(FACING, facingDirection).setValue(TYPE, type).setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
   }

   protected ChestType getChestType(final Level level, final BlockPos pos, final Direction facingDirection) {
      if (facingDirection == this.candidatePartnerFacing(level, pos, facingDirection.getClockWise())) {
         return ChestType.LEFT;
      } else {
         return facingDirection == this.candidatePartnerFacing(level, pos, facingDirection.getCounterClockWise()) ? ChestType.RIGHT : ChestType.SINGLE;
      }
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   private @Nullable Direction candidatePartnerFacing(final Level level, final BlockPos pos, final Direction neighbourDirection) {
      BlockState state = level.getBlockState(pos.relative(neighbourDirection));
      return this.chestCanConnectTo(state) && state.getValue(TYPE) == ChestType.SINGLE ? state.getValue(FACING) : null;
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      Containers.updateNeighboursAfterDestroy(state, level, pos);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level instanceof ServerLevel serverLevel) {
         MenuProvider menuProvider = this.getMenuProvider(state, level, pos);
         if (menuProvider != null) {
            player.openMenu(menuProvider);
            player.awardStat(this.getOpenChestStat());
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
         }
      }

      return InteractionResult.SUCCESS;
   }

   protected Stat<Identifier> getOpenChestStat() {
      return Stats.CUSTOM.get(Stats.OPEN_CHEST);
   }

   public BlockEntityType<? extends ChestBlockEntity> blockEntityType() {
      return this.blockEntityType.get();
   }

   public static @Nullable Container getContainer(
      final ChestBlock block, final BlockState state, final Level level, final BlockPos pos, final boolean ignoreBeingBlocked
   ) {
      return block.combine(state, level, pos, ignoreBeingBlocked).apply(CHEST_COMBINER).orElse(null);
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(
      final BlockState state, final Level level, final BlockPos pos, final boolean ignoreBeingBlocked
   ) {
      BiPredicate<LevelAccessor, BlockPos> predicate;
      if (ignoreBeingBlocked) {
         predicate = (levelAccessor, blockPos) -> false;
      } else {
         predicate = ChestBlock::isChestBlockedAt;
      }

      return DoubleBlockCombiner.combineWithNeigbour(
         this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, state, level, pos, predicate
      );
   }

   @Override
   protected @Nullable MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
      return this.combine(state, level, pos, false).apply(MENU_PROVIDER_COMBINER).orElse(null);
   }

   public static DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction> opennessCombiner(final LidBlockEntity entity) {
      return new DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction>() {
         public Float2FloatFunction acceptDouble(final ChestBlockEntity first, final ChestBlockEntity second) {
            return partialTickTime -> Math.max(first.getOpenNess(partialTickTime), second.getOpenNess(partialTickTime));
         }

         public Float2FloatFunction acceptSingle(final ChestBlockEntity single) {
            return single::getOpenNess;
         }

         public Float2FloatFunction acceptNone() {
            return entity::getOpenNess;
         }
      };
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new ChestBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return level.isClientSide() ? createTickerHelper(type, this.blockEntityType(), ChestBlockEntity::lidAnimateTick) : null;
   }

   public static boolean isChestBlockedAt(final LevelAccessor level, final BlockPos pos) {
      return isBlockedChestByBlock(level, pos) || isCatSittingOnChest(level, pos);
   }

   private static boolean isBlockedChestByBlock(final BlockGetter level, final BlockPos pos) {
      BlockPos above = pos.above();
      return level.getBlockState(above).isRedstoneConductor(level, above);
   }

   private static boolean isCatSittingOnChest(final LevelAccessor level, final BlockPos pos) {
      List<Cat> cats = level.getEntitiesOfClass(Cat.class, new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1));
      if (!cats.isEmpty()) {
         for (Cat cat : cats) {
            if (cat.isInSittingPose()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(this, state, level, pos, false));
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
      builder.add(FACING, TYPE, WATERLOGGED);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity) {
         chestBlockEntity.recheckOpen();
      }
   }

   public SoundEvent getOpenChestSound() {
      return this.openSound;
   }

   public SoundEvent getCloseChestSound() {
      return this.closeSound;
   }
}
