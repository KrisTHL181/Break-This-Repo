package net.minecraft.world.level.block;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SideChainPart;

public interface SideChainPartBlock {
   SideChainPart getSideChainPart(BlockState var1);

   BlockState setSideChainPart(BlockState var1, SideChainPart var2);

   Direction getFacing(BlockState var1);

   boolean isConnectable(BlockState var1);

   int getMaxChainLength();

   default List<BlockPos> getAllBlocksConnectedTo(LevelAccessor p_428549_, BlockPos p_429232_) {
      BlockState blockstate = p_428549_.getBlockState(p_429232_);
      if (!this.isConnectable(blockstate)) {
         return List.of();
      }

      SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_428549_, p_429232_, this.getFacing(blockstate));
      List<BlockPos> list = new LinkedList<>();
      list.add(p_429232_);
      this.addBlocksConnectingTowards(sidechainpartblock$neighbors::left, SideChainPart.LEFT, list::addFirst);
      this.addBlocksConnectingTowards(sidechainpartblock$neighbors::right, SideChainPart.RIGHT, list::addLast);
      return list;
   }

   private void addBlocksConnectingTowards(IntFunction<SideChainPartBlock.Neighbor> p_431618_, SideChainPart p_431230_, Consumer<BlockPos> p_425650_) {
      for (int i = 1; i < this.getMaxChainLength(); i++) {
         SideChainPartBlock.Neighbor sidechainpartblock$neighbor = p_431618_.apply(i);
         if (sidechainpartblock$neighbor.connectsTowards(p_431230_)) {
            p_425650_.accept(sidechainpartblock$neighbor.pos());
         }

         if (sidechainpartblock$neighbor.isUnconnectableOrChainEnd()) {
            break;
         }
      }
   }

   default void updateNeighborsAfterPoweringDown(LevelAccessor p_427643_, BlockPos p_424179_, BlockState p_430255_) {
      SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_427643_, p_424179_, this.getFacing(p_430255_));
      sidechainpartblock$neighbors.left().disconnectFromRight();
      sidechainpartblock$neighbors.right().disconnectFromLeft();
   }

   default void updateSelfAndNeighborsOnPoweringUp(LevelAccessor p_428915_, BlockPos p_422288_, BlockState p_426121_, BlockState p_428080_) {
      if (this.isConnectable(p_426121_) && !this.isBeingUpdatedByNeighbor(p_426121_, p_428080_)) {
         SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(p_428915_, p_422288_, this.getFacing(p_426121_));
         SideChainPart sidechainpart = SideChainPart.UNCONNECTED;
         int i = sidechainpartblock$neighbors.left().isConnectable()
            ? this.getAllBlocksConnectedTo(p_428915_, sidechainpartblock$neighbors.left().pos()).size()
            : 0;
         int j = sidechainpartblock$neighbors.right().isConnectable()
            ? this.getAllBlocksConnectedTo(p_428915_, sidechainpartblock$neighbors.right().pos()).size()
            : 0;
         int k = 1;
         if (this.canConnect(i, k)) {
            sidechainpart = sidechainpart.whenConnectedToTheLeft();
            sidechainpartblock$neighbors.left().connectToTheRight();
            k += i;
         }

         if (this.canConnect(j, k)) {
            sidechainpart = sidechainpart.whenConnectedToTheRight();
            sidechainpartblock$neighbors.right().connectToTheLeft();
         }

         this.setPart(p_428915_, p_422288_, sidechainpart);
      }
   }

   private boolean canConnect(int p_431570_, int p_424641_) {
      return p_431570_ > 0 && p_424641_ + p_431570_ <= this.getMaxChainLength();
   }

   private boolean isBeingUpdatedByNeighbor(BlockState p_425781_, BlockState p_427863_) {
      boolean flag = this.getSideChainPart(p_425781_).isConnected();
      boolean flag1 = this.isConnectable(p_427863_) && this.getSideChainPart(p_427863_).isConnected();
      return flag || flag1;
   }

   private SideChainPartBlock.Neighbors getNeighbors(LevelAccessor p_426393_, BlockPos p_424109_, Direction p_424155_) {
      return new SideChainPartBlock.Neighbors(this, p_426393_, p_424155_, p_424109_, new HashMap<>());
   }

   default void setPart(LevelAccessor p_430694_, BlockPos p_424263_, SideChainPart p_427143_) {
      BlockState blockstate = p_430694_.getBlockState(p_424263_);
      if (this.getSideChainPart(blockstate) != p_427143_) {
         p_430694_.setBlock(p_424263_, this.setSideChainPart(blockstate, p_427143_), 3);
      }
   }

   record EmptyNeighbor(BlockPos pos) implements SideChainPartBlock.Neighbor {
      @Override
      public boolean isConnectable() {
         return false;
      }

      @Override
      public boolean isUnconnectableOrChainEnd() {
         return true;
      }

      @Override
      public boolean connectsTowards(SideChainPart p_428972_) {
         return false;
      }
   }

   sealed interface Neighbor permits SideChainPartBlock.EmptyNeighbor, SideChainPartBlock.SideChainNeighbor {
      BlockPos pos();

      boolean isConnectable();

      boolean isUnconnectableOrChainEnd();

      boolean connectsTowards(SideChainPart var1);

      default void connectToTheRight() {
      }

      default void connectToTheLeft() {
      }

      default void disconnectFromRight() {
      }

      default void disconnectFromLeft() {
      }
   }

   record Neighbors(SideChainPartBlock block, LevelAccessor level, Direction facing, BlockPos center, Map<BlockPos, SideChainPartBlock.Neighbor> cache) {
      private boolean isConnectableToThisBlock(BlockState p_424951_) {
         return this.block.isConnectable(p_424951_) && this.block.getFacing(p_424951_) == this.facing;
      }

      private SideChainPartBlock.Neighbor createNewNeighbor(BlockPos p_423901_) {
         BlockState blockstate = this.level.getBlockState(p_423901_);
         SideChainPart sidechainpart = this.isConnectableToThisBlock(blockstate) ? this.block.getSideChainPart(blockstate) : null;
         return (SideChainPartBlock.Neighbor)(
            sidechainpart == null
               ? new SideChainPartBlock.EmptyNeighbor(p_423901_)
               : new SideChainPartBlock.SideChainNeighbor(this.level, this.block, p_423901_, sidechainpart)
         );
      }

      private SideChainPartBlock.Neighbor getOrCreateNeighbor(Direction p_430739_, Integer p_428096_) {
         return this.cache.computeIfAbsent(this.center.relative(p_430739_, p_428096_), this::createNewNeighbor);
      }

      public SideChainPartBlock.Neighbor left(int p_431136_) {
         return this.getOrCreateNeighbor(this.facing.getClockWise(), p_431136_);
      }

      public SideChainPartBlock.Neighbor right(int p_428657_) {
         return this.getOrCreateNeighbor(this.facing.getCounterClockWise(), p_428657_);
      }

      public SideChainPartBlock.Neighbor left() {
         return this.left(1);
      }

      public SideChainPartBlock.Neighbor right() {
         return this.right(1);
      }
   }

   record SideChainNeighbor(LevelAccessor level, SideChainPartBlock block, BlockPos pos, SideChainPart part) implements SideChainPartBlock.Neighbor {
      @Override
      public boolean isConnectable() {
         return true;
      }

      @Override
      public boolean isUnconnectableOrChainEnd() {
         return this.part.isChainEnd();
      }

      @Override
      public boolean connectsTowards(SideChainPart p_423885_) {
         return this.part.isConnectionTowards(p_423885_);
      }

      @Override
      public void connectToTheRight() {
         this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheRight());
      }

      @Override
      public void connectToTheLeft() {
         this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheLeft());
      }

      @Override
      public void disconnectFromRight() {
         this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheRight());
      }

      @Override
      public void disconnectFromLeft() {
         this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheLeft());
      }
   }
}
