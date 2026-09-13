package net.minecraft.world.level.redstone;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jspecify.annotations.Nullable;

public class ExperimentalRedstoneWireEvaluator extends RedstoneWireEvaluator {
   private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<>();
   private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<>();
   private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap();

   public ExperimentalRedstoneWireEvaluator(final RedStoneWireBlock wireBlock) {
      super(wireBlock);
   }

   @Override
   public void updatePowerStrength(
      final Level level,
      final BlockPos initialPos,
      final BlockState ignored,
      final @Nullable Orientation orientation,
      final boolean shapeUpdateWiresAroundInitialPosition
   ) {
      Orientation initialOrientation = getInitialOrientation(level, orientation);
      this.calculateCurrentChanges(level, initialPos, initialOrientation);
      ObjectIterator<Entry<BlockPos>> iterator = this.updatedWires.object2IntEntrySet().iterator();

      for (boolean initialWire = true; iterator.hasNext(); initialWire = false) {
         Entry<BlockPos> next = (Entry<BlockPos>)iterator.next();
         BlockPos pos = (BlockPos)next.getKey();
         int packed = next.getIntValue();
         int newLevel = unpackPower(packed);
         BlockState state = level.getBlockState(pos);
         if (state.is(this.wireBlock) && !state.getValue(RedStoneWireBlock.POWER).equals(newLevel)) {
            int updateFlags = 2;
            if (!shapeUpdateWiresAroundInitialPosition || !initialWire) {
               updateFlags |= 128;
            }

            level.setBlock(pos, state.setValue(RedStoneWireBlock.POWER, newLevel), updateFlags);
         } else {
            iterator.remove();
         }
      }

      this.causeNeighborUpdates(level);
   }

   private void causeNeighborUpdates(final Level level) {
      this.updatedWires.forEach((wirePos, packed) -> {
         Orientation orientation = unpackOrientation(packed);
         BlockState state = level.getBlockState(wirePos);

         for (Direction neighborDirection : orientation.getDirections()) {
            if (isConnected(state, neighborDirection)) {
               BlockPos neighborPos = wirePos.relative(neighborDirection);
               BlockState neighborState = level.getBlockState(neighborPos);
               Orientation neighborOrientation = orientation.withFrontPreserveUp(neighborDirection);
               level.neighborChanged(neighborState, neighborPos, this.wireBlock, neighborOrientation, false);
               if (neighborState.isRedstoneConductor(level, neighborPos)) {
                  for (Direction direction : neighborOrientation.getDirections()) {
                     if (direction != neighborDirection.getOpposite()) {
                        level.neighborChanged(neighborPos.relative(direction), this.wireBlock, neighborOrientation.withFrontPreserveUp(direction));
                     }
                  }
               }
            }
         }
      });
      if (level instanceof ServerLevel serverLevel && serverLevel.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)) {
         this.updatedWires
            .forEach(
               (wirePos, packed) -> serverLevel.debugSynchronizers()
                  .sendBlockValue(wirePos, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, unpackOrientation(packed))
            );
      }
   }

   private static boolean isConnected(final BlockState state, final Direction direction) {
      EnumProperty<RedstoneSide> property = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
      return property == null ? direction == Direction.DOWN : state.getValue(property).isConnected();
   }

   private static Orientation getInitialOrientation(final Level level, final @Nullable Orientation incomingOrigination) {
      Orientation orientation;
      if (incomingOrigination != null) {
         orientation = incomingOrigination;
      } else {
         orientation = Orientation.random(level.getRandom());
      }

      return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
   }

   private void calculateCurrentChanges(final Level level, final BlockPos initialPosition, final Orientation initialOrientation) {
      BlockState initialState = level.getBlockState(initialPosition);
      if (initialState.is(this.wireBlock)) {
         this.setPower(initialPosition, initialState.getValue(RedStoneWireBlock.POWER), initialOrientation);
         this.wiresToTurnOff.add(initialPosition);
      } else {
         this.propagateChangeToNeighbors(level, initialPosition, 0, initialOrientation, true);
      }

      while (!this.wiresToTurnOff.isEmpty()) {
         BlockPos pos = this.wiresToTurnOff.removeFirst();
         int packed = this.updatedWires.getInt(pos);
         Orientation orientation = unpackOrientation(packed);
         int oldPower = unpackPower(packed);
         int blockPower = this.getBlockSignal(level, pos);
         int wirePower = this.getIncomingWireSignal(level, pos);
         int newPower = Math.max(blockPower, wirePower);
         int powerToSet;
         if (newPower < oldPower) {
            if (blockPower > 0 && !this.wiresToTurnOn.contains(pos)) {
               this.wiresToTurnOn.add(pos);
            }

            powerToSet = 0;
         } else {
            powerToSet = newPower;
         }

         if (powerToSet != oldPower) {
            this.setPower(pos, powerToSet, orientation);
         }

         this.propagateChangeToNeighbors(level, pos, powerToSet, orientation, oldPower > newPower);
      }

      while (!this.wiresToTurnOn.isEmpty()) {
         BlockPos pos = this.wiresToTurnOn.removeFirst();
         int packed = this.updatedWires.getInt(pos);
         int oldPower = unpackPower(packed);
         int blockPower = this.getBlockSignal(level, pos);
         int wirePower = this.getIncomingWireSignal(level, pos);
         int newPower = Math.max(blockPower, wirePower);
         Orientation orientation = unpackOrientation(packed);
         if (newPower > oldPower) {
            this.setPower(pos, newPower, orientation);
         } else if (newPower < oldPower) {
            throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
         }

         this.propagateChangeToNeighbors(level, pos, newPower, orientation, false);
      }
   }

   private static int packOrientationAndPower(final Orientation orientation, final int power) {
      return orientation.getIndex() << 4 | power;
   }

   private static Orientation unpackOrientation(final int packed) {
      return Orientation.fromIndex(packed >> 4);
   }

   private static int unpackPower(final int packed) {
      return packed & 15;
   }

   private void setPower(final BlockPos pos, final int newPower, final Orientation orientation) {
      this.updatedWires
         .compute(
            pos,
            (key, packed) -> packed == null ? packOrientationAndPower(orientation, newPower) : packOrientationAndPower(unpackOrientation(packed), newPower)
         );
   }

   private void propagateChangeToNeighbors(
      final Level level, final BlockPos pos, final int newPower, final Orientation orientation, final boolean allowTurningOff
   ) {
      for (Direction directionHorizontal : orientation.getHorizontalDirections()) {
         BlockPos offsetPos = pos.relative(directionHorizontal);
         this.enqueueNeighborWire(level, offsetPos, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
      }

      for (Direction directionVertical : orientation.getVerticalDirections()) {
         BlockPos offsetPos = pos.relative(directionVertical);
         boolean solidBlock = level.getBlockState(offsetPos).isRedstoneConductor(level, offsetPos);

         for (Direction directionHorizontal : orientation.getHorizontalDirections()) {
            BlockPos neighbor = pos.relative(directionHorizontal);
            if (directionVertical == Direction.UP && !solidBlock) {
               BlockPos neighborWire = offsetPos.relative(directionHorizontal);
               this.enqueueNeighborWire(level, neighborWire, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
            } else if (directionVertical == Direction.DOWN && !level.getBlockState(neighbor).isRedstoneConductor(level, neighbor)) {
               BlockPos neighborWire = offsetPos.relative(directionHorizontal);
               this.enqueueNeighborWire(level, neighborWire, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
            }
         }
      }
   }

   private void enqueueNeighborWire(final Level level, final BlockPos pos, final int newFromPower, final Orientation orientation, final boolean allowTurningOff) {
      BlockState state = level.getBlockState(pos);
      if (state.is(this.wireBlock)) {
         int toPower = this.getWireSignal(pos, state);
         if (toPower < newFromPower - 1 && !this.wiresToTurnOn.contains(pos)) {
            this.wiresToTurnOn.add(pos);
            this.setPower(pos, toPower, orientation);
         }

         if (allowTurningOff && toPower > newFromPower && !this.wiresToTurnOff.contains(pos)) {
            this.wiresToTurnOff.add(pos);
            this.setPower(pos, toPower, orientation);
         }
      }
   }

   @Override
   protected int getWireSignal(final BlockPos pos, final BlockState state) {
      int packed = this.updatedWires.getOrDefault(pos, -1);
      return packed != -1 ? unpackPower(packed) : super.getWireSignal(pos, state);
   }
}
