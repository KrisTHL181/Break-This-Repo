package net.minecraft.world.level.block;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

public class DetectorRailBlock extends BaseRailBlock {
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int PRESSED_CHECK_PERIOD = 20;

   public DetectorRailBlock(final BlockBehaviour.Properties properties) {
      super(true, properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false).setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
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
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (!level.isClientSide()) {
         if (!state.getValue(POWERED)) {
            this.checkPressed(level, pos, state);
         }
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(POWERED)) {
         this.checkPressed(level, pos, state);
      }
   }

   @Override
   protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
      if (!state.getValue(POWERED)) {
         return 0;
      } else {
         return direction == Direction.UP ? 15 : 0;
      }
   }

   private void checkPressed(final Level level, final BlockPos pos, final BlockState state) {
      if (this.canSurvive(state, level, pos)) {
         boolean wasPressed = state.getValue(POWERED);
         boolean shouldBePressed = false;
         List<AbstractMinecart> entities = this.getInteractingMinecartOfType(level, pos, AbstractMinecart.class, e -> true);
         if (!entities.isEmpty()) {
            shouldBePressed = true;
         }

         if (shouldBePressed && !wasPressed) {
            BlockState newState = state.setValue(POWERED, true);
            level.setBlockAndUpdate(pos, newState);
            this.updatePowerToConnected(level, pos, newState, true);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
            level.setBlocksDirty(pos, state, newState);
         }

         if (!shouldBePressed && wasPressed) {
            BlockState newState = state.setValue(POWERED, false);
            level.setBlockAndUpdate(pos, newState);
            this.updatePowerToConnected(level, pos, newState, false);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
            level.setBlocksDirty(pos, state, newState);
         }

         if (shouldBePressed) {
            level.scheduleTick(pos, this, 20);
         }

         level.updateNeighbourForOutputSignal(pos, this);
      }
   }

   protected void updatePowerToConnected(final Level level, final BlockPos pos, final BlockState state, final boolean powered) {
      RailState rail = new RailState(level, pos, state);

      for (BlockPos connectionPos : rail.getConnections()) {
         BlockState connectionState = level.getBlockState(connectionPos);
         level.neighborChanged(connectionState, connectionPos, connectionState.getBlock(), null, false);
      }
   }

   @Override
   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      if (!oldState.is(state.getBlock())) {
         BlockState updatedState = this.updateState(state, level, pos, movedByPiston);
         this.checkPressed(level, pos, updatedState);
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      if (state.getValue(POWERED)) {
         List<MinecartCommandBlock> commandBlocks = this.getInteractingMinecartOfType(level, pos, MinecartCommandBlock.class, e -> true);
         if (!commandBlocks.isEmpty()) {
            return commandBlocks.get(0).getCommandBlock().getSuccessCount();
         }

         List<AbstractMinecart> entities = this.getInteractingMinecartOfType(level, pos, AbstractMinecart.class, EntitySelector.CONTAINER_ENTITY_SELECTOR);
         if (!entities.isEmpty()) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)entities.get(0));
         }
      }

      return 0;
   }

   private <T extends AbstractMinecart> List<T> getInteractingMinecartOfType(
      final Level level, final BlockPos pos, final Class<T> type, final Predicate<Entity> containerEntitySelector
   ) {
      return level.getEntitiesOfClass(type, this.getSearchBB(pos), containerEntitySelector);
   }

   private AABB getSearchBB(final BlockPos pos) {
      double b = 0.2;
      return new AABB(pos.getX() + 0.2, pos.getY(), pos.getZ() + 0.2, pos.getX() + 1 - 0.2, pos.getY() + 1 - 0.2, pos.getZ() + 1 - 0.2);
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      RailShape currentShape = state.getValue(SHAPE);
      RailShape newShape = this.rotate(currentShape, rotation);
      return state.setValue(SHAPE, newShape);
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      RailShape currentShape = state.getValue(SHAPE);
      RailShape newShape = this.mirror(currentShape, mirror);
      return state.setValue(SHAPE, newShape);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(SHAPE, POWERED, WATERLOGGED);
   }
}
