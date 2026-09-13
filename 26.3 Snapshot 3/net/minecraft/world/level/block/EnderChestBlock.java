package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EnderChestBlock extends AbstractChestBlock<EnderChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);
   private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");

   protected EnderChestBlock(final BlockBehaviour.Properties properties) {
      super(properties, () -> BlockEntityTypes.ENDER_CHEST);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(
      final BlockState state, final Level level, final BlockPos pos, final boolean ignoreBeingBlocked
   ) {
      return DoubleBlockCombiner.Combiner::acceptNone;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      return this.defaultBlockState()
         .setValue(FACING, context.getHorizontalDirection().getOpposite())
         .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      PlayerEnderChestContainer container = player.getEnderChestInventory();
      if (container != null && level.getBlockEntity(pos) instanceof EnderChestBlockEntity enderChest) {
         BlockPos above = pos.above();
         if (level.getBlockState(above).isRedstoneConductor(level, above)) {
            return InteractionResult.SUCCESS;
         }

         if (level instanceof ServerLevel serverLevel) {
            container.setActiveChest(enderChest);
            player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> ChestMenu.threeRows(containerId, inventory, container), CONTAINER_TITLE));
            player.awardStat(Stats.OPEN_ENDERCHEST);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new EnderChestBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return level.isClientSide() ? createTickerHelper(type, BlockEntityTypes.ENDER_CHEST, EnderChestBlockEntity::lidAnimateTick) : null;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      for (int i = 0; i < 3; i++) {
         int flipX = random.nextInt(2) * 2 - 1;
         int flipZ = random.nextInt(2) * 2 - 1;
         double x = pos.getX() + 0.5 + 0.25 * flipX;
         double y = pos.getY() + random.nextFloat();
         double z = pos.getZ() + 0.5 + 0.25 * flipZ;
         double xa = random.nextFloat() * flipX;
         double ya = (random.nextFloat() - 0.5) * 0.125;
         double za = random.nextFloat() * flipZ;
         level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
      }
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
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getBlockEntity(pos) instanceof EnderChestBlockEntity enderChestBlockEntity) {
         enderChestBlockEntity.recheckOpen();
      }
   }
}
