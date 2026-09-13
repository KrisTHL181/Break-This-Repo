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

   public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock p_369306_) {
      super(p_369306_);
   }

   @Override
   public void updatePowerStrength(Level p_367453_, BlockPos p_363644_, BlockState p_363406_, @Nullable Orientation p_364106_, boolean p_364023_) {
      Orientation orientation = getInitialOrientation(p_367453_, p_364106_);
      this.calculateCurrentChanges(p_367453_, p_363644_, orientation);
      ObjectIterator<Entry<BlockPos>> objectiterator = this.updatedWires.object2IntEntrySet().iterator();

      for (boolean flag = true; objectiterator.hasNext(); flag = false) {
         Entry<BlockPos> entry = (Entry<BlockPos>)objectiterator.next();
         BlockPos blockpos = (BlockPos)entry.getKey();
         int i = entry.getIntValue();
         int j = unpackPower(i);
         BlockState blockstate = p_367453_.getBlockState(blockpos);
         if (blockstate.is(this.wireBlock) && !blockstate.getValue(RedStoneWireBlock.POWER).equals(j)) {
            int k = 2;
            if (!p_364023_ || !flag) {
               k |= 128;
            }

            p_367453_.setBlock(blockpos, blockstate.setValue(RedStoneWireBlock.POWER, j), k);
         } else {
            objectiterator.remove();
         }
      }

      this.causeNeighborUpdates(p_367453_);
   }

   private void causeNeighborUpdates(Level p_361658_) {
      this.updatedWires.forEach((p_366674_, p_458388_) -> {
         Orientation orientation = unpackOrientation(p_458388_);
         BlockState blockstate = p_361658_.getBlockState(p_366674_);

         for (Direction direction : orientation.getDirections()) {
            if (isConnected(blockstate, direction)) {
               BlockPos blockpos = p_366674_.relative(direction);
               BlockState blockstate1 = p_361658_.getBlockState(blockpos);
               Orientation orientation1 = orientation.withFrontPreserveUp(direction);
               p_361658_.neighborChanged(blockstate1, blockpos, this.wireBlock, orientation1, false);
               if (blockstate1.isRedstoneConductor(p_361658_, blockpos)) {
                  for (Direction direction1 : orientation1.getDirections()) {
                     if (direction1 != direction.getOpposite()) {
                        p_361658_.neighborChanged(blockpos.relative(direction1), this.wireBlock, orientation1.withFrontPreserveUp(direction1));
                     }
                  }
               }
            }
         }
      });
      if (p_361658_ instanceof ServerLevel serverlevel && serverlevel.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)) {
         this.updatedWires
            .forEach(
               (p_450054_, p_450055_) -> serverlevel.debugSynchronizers()
                  .sendBlockValue(p_450054_, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, unpackOrientation(p_450055_))
            );
      }
   }

   private static boolean isConnected(BlockState p_361129_, Direction p_370064_) {
      EnumProperty<RedstoneSide> enumproperty = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(p_370064_);
      return enumproperty == null ? p_370064_ == Direction.DOWN : p_361129_.getValue(enumproperty).isConnected();
   }

   private static Orientation getInitialOrientation(Level p_366044_, @Nullable Orientation p_367435_) {
      Orientation orientation;
      if (p_367435_ != null) {
         orientation = p_367435_;
      } else {
         orientation = Orientation.random(p_366044_.random);
      }

      return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
   }

   private void calculateCurrentChanges(Level p_367773_, BlockPos p_368020_, Orientation p_363672_) {
      BlockState blockstate = p_367773_.getBlockState(p_368020_);
      if (blockstate.is(this.wireBlock)) {
         this.setPower(p_368020_, blockstate.getValue(RedStoneWireBlock.POWER), p_363672_);
         this.wiresToTurnOff.add(p_368020_);
      } else {
         this.propagateChangeToNeighbors(p_367773_, p_368020_, 0, p_363672_, true);
      }

      while (!this.wiresToTurnOff.isEmpty()) {
         BlockPos blockpos = this.wiresToTurnOff.removeFirst();
         int i = this.updatedWires.getInt(blockpos);
         Orientation orientation = unpackOrientation(i);
         int j = unpackPower(i);
         int k = this.getBlockSignal(p_367773_, blockpos);
         int l = this.getIncomingWireSignal(p_367773_, blockpos);
         int i1 = Math.max(k, l);
         int j1;
         if (i1 < j) {
            if (k > 0 && !this.wiresToTurnOn.contains(blockpos)) {
               this.wiresToTurnOn.add(blockpos);
            }

            j1 = 0;
         } else {
            j1 = i1;
         }

         if (j1 != j) {
            this.setPower(blockpos, j1, orientation);
         }

         this.propagateChangeToNeighbors(p_367773_, blockpos, j1, orientation, j > i1);
      }

      while (!this.wiresToTurnOn.isEmpty()) {
         BlockPos blockpos1 = this.wiresToTurnOn.removeFirst();
         int k1 = this.updatedWires.getInt(blockpos1);
         int l1 = unpackPower(k1);
         int i2 = this.getBlockSignal(p_367773_, blockpos1);
         int j2 = this.getIncomingWireSignal(p_367773_, blockpos1);
         int k2 = Math.max(i2, j2);
         Orientation orientation1 = unpackOrientation(k1);
         if (k2 > l1) {
            this.setPower(blockpos1, k2, orientation1);
         } else if (k2 < l1) {
            throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
         }

         this.propagateChangeToNeighbors(p_367773_, blockpos1, k2, orientation1, false);
      }
   }

   private static int packOrientationAndPower(Orientation p_367231_, int p_361883_) {
      return p_367231_.getIndex() << 4 | p_361883_;
   }

   private static Orientation unpackOrientation(int p_368491_) {
      return Orientation.fromIndex(p_368491_ >> 4);
   }

   private static int unpackPower(int p_368870_) {
      return p_368870_ & 15;
   }

   private void setPower(BlockPos p_367295_, int p_365268_, Orientation p_369383_) {
      this.updatedWires
         .compute(
            p_367295_,
            (p_367119_, p_364881_) -> p_364881_ == null
               ? packOrientationAndPower(p_369383_, p_365268_)
               : packOrientationAndPower(unpackOrientation(p_364881_), p_365268_)
         );
   }

   private void propagateChangeToNeighbors(Level p_367937_, BlockPos p_366464_, int p_365363_, Orientation p_362665_, boolean p_362605_) {
      for (Direction direction : p_362665_.getHorizontalDirections()) {
         BlockPos blockpos = p_366464_.relative(direction);
         this.enqueueNeighborWire(p_367937_, blockpos, p_365363_, p_362665_.withFront(direction), p_362605_);
      }

      for (Direction direction2 : p_362665_.getVerticalDirections()) {
         BlockPos blockpos3 = p_366464_.relative(direction2);
         boolean flag = p_367937_.getBlockState(blockpos3).isRedstoneConductor(p_367937_, blockpos3);

         for (Direction direction1 : p_362665_.getHorizontalDirections()) {
            BlockPos blockpos1 = p_366464_.relative(direction1);
            if (direction2 == Direction.UP && !flag) {
               BlockPos blockpos4 = blockpos3.relative(direction1);
               this.enqueueNeighborWire(p_367937_, blockpos4, p_365363_, p_362665_.withFront(direction1), p_362605_);
            } else if (direction2 == Direction.DOWN && !p_367937_.getBlockState(blockpos1).isRedstoneConductor(p_367937_, blockpos1)) {
               BlockPos blockpos2 = blockpos3.relative(direction1);
               this.enqueueNeighborWire(p_367937_, blockpos2, p_365363_, p_362665_.withFront(direction1), p_362605_);
            }
         }
      }
   }

   private void enqueueNeighborWire(Level p_366800_, BlockPos p_361668_, int p_368963_, Orientation p_362366_, boolean p_366534_) {
      BlockState blockstate = p_366800_.getBlockState(p_361668_);
      if (blockstate.is(this.wireBlock)) {
         int i = this.getWireSignal(p_361668_, blockstate);
         if (i < p_368963_ - 1 && !this.wiresToTurnOn.contains(p_361668_)) {
            this.wiresToTurnOn.add(p_361668_);
            this.setPower(p_361668_, i, p_362366_);
         }

         if (p_366534_ && i > p_368963_ && !this.wiresToTurnOff.contains(p_361668_)) {
            this.wiresToTurnOff.add(p_361668_);
            this.setPower(p_361668_, i, p_362366_);
         }
      }
   }

   @Override
   protected int getWireSignal(BlockPos p_368955_, BlockState p_368466_) {
      int i = this.updatedWires.getOrDefault(p_368955_, -1);
      return i != -1 ? unpackPower(i) : super.getWireSignal(p_368955_, p_368466_);
   }
}
