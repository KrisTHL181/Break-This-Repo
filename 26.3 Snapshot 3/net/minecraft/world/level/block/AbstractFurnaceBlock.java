package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFurnaceBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   protected AbstractFurnaceBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!level.isClientSide()) {
         this.openContainer(level, pos, player);
      }

      return InteractionResult.SUCCESS;
   }

   protected abstract void openContainer(final Level level, final BlockPos pos, final Player player);

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
      builder.add(FACING, LIT);
   }

   protected static <T extends BlockEntity> @Nullable BlockEntityTicker<T> createFurnaceTicker(
      final Level level, final BlockEntityType<T> actualType, final BlockEntityType<? extends AbstractFurnaceBlockEntity> expectedType
   ) {
      return level instanceof ServerLevel serverLevel
         ? createTickerHelper(
            actualType, expectedType, (innerLevel, pos, state, entity) -> AbstractFurnaceBlockEntity.serverTick(serverLevel, pos, state, entity)
         )
         : null;
   }
}
