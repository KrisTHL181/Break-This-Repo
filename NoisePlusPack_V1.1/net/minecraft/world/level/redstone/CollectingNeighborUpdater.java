package net.minecraft.world.level.redstone;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CollectingNeighborUpdater implements NeighborUpdater {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Level level;
   private final int maxChainedNeighborUpdates;
   private final ArrayDeque<CollectingNeighborUpdater.NeighborUpdates> stack = new ArrayDeque<>();
   private final List<CollectingNeighborUpdater.NeighborUpdates> addedThisLayer = new ArrayList<>();
   private int count = 0;
   private @Nullable Consumer<BlockPos> debugListener;

   public CollectingNeighborUpdater(Level p_230643_, int p_230644_) {
      this.level = p_230643_;
      this.maxChainedNeighborUpdates = p_230644_;
   }

   public void setDebugListener(@Nullable Consumer<BlockPos> p_425764_) {
      this.debugListener = p_425764_;
   }

   @Override
   public void shapeUpdate(Direction p_230664_, BlockState p_230665_, BlockPos p_230666_, BlockPos p_230667_, @Block.UpdateFlags int p_230668_, int p_230669_) {
      this.addAndRun(
         p_230666_, new CollectingNeighborUpdater.ShapeUpdate(p_230664_, p_230665_, p_230666_.immutable(), p_230667_.immutable(), p_230668_, p_230669_)
      );
   }

   @Override
   public void neighborChanged(BlockPos p_230653_, Block p_230654_, @Nullable Orientation p_364159_) {
      this.addAndRun(p_230653_, new CollectingNeighborUpdater.SimpleNeighborUpdate(p_230653_, p_230654_, p_364159_));
   }

   @Override
   public void neighborChanged(BlockState p_230647_, BlockPos p_230648_, Block p_230649_, @Nullable Orientation p_367539_, boolean p_230651_) {
      this.addAndRun(p_230648_, new CollectingNeighborUpdater.FullNeighborUpdate(p_230647_, p_230648_.immutable(), p_230649_, p_367539_, p_230651_));
   }

   @Override
   public void updateNeighborsAtExceptFromFacing(BlockPos p_230657_, Block p_230658_, @Nullable Direction p_230659_, @Nullable Orientation p_368385_) {
      this.addAndRun(p_230657_, new CollectingNeighborUpdater.MultiNeighborUpdate(p_230657_.immutable(), p_230658_, p_368385_, p_230659_));
   }

   private void addAndRun(BlockPos p_230661_, CollectingNeighborUpdater.NeighborUpdates p_230662_) {
      boolean flag = this.count > 0;
      boolean flag1 = this.maxChainedNeighborUpdates >= 0 && this.count >= this.maxChainedNeighborUpdates;
      this.count++;
      if (!flag1) {
         if (flag) {
            this.addedThisLayer.add(p_230662_);
         } else {
            this.stack.push(p_230662_);
         }
      } else if (this.count - 1 == this.maxChainedNeighborUpdates) {
         LOGGER.error("Too many chained neighbor updates. Skipping the rest. First skipped position: {}", p_230661_.toShortString());
      }

      if (!flag) {
         this.runUpdates();
      }
   }

   private void runUpdates() {
      try {
         while (!this.stack.isEmpty() || !this.addedThisLayer.isEmpty()) {
            for (int i = this.addedThisLayer.size() - 1; i >= 0; i--) {
               this.stack.push(this.addedThisLayer.get(i));
            }

            this.addedThisLayer.clear();
            CollectingNeighborUpdater.NeighborUpdates collectingneighborupdater$neighborupdates = this.stack.peek();
            if (this.debugListener != null) {
               collectingneighborupdater$neighborupdates.forEachUpdatedPos(this.debugListener);
            }

            while (this.addedThisLayer.isEmpty()) {
               if (!collectingneighborupdater$neighborupdates.runNext(this.level)) {
                  this.stack.pop();
                  break;
               }
            }
         }
      } finally {
         this.stack.clear();
         this.addedThisLayer.clear();
         this.count = 0;
      }
   }

   record FullNeighborUpdate(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston)
      implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(Level p_230683_) {
         NeighborUpdater.executeUpdate(p_230683_, this.state, this.pos, this.block, this.orientation, this.movedByPiston);
         return false;
      }

      @Override
      public void forEachUpdatedPos(Consumer<BlockPos> p_430229_) {
         p_430229_.accept(this.pos);
      }
   }

   static final class MultiNeighborUpdate implements CollectingNeighborUpdater.NeighborUpdates {
      private final BlockPos sourcePos;
      private final Block sourceBlock;
      private @Nullable Orientation orientation;
      private final @Nullable Direction skipDirection;
      private int idx = 0;

      MultiNeighborUpdate(BlockPos p_230697_, Block p_230698_, @Nullable Orientation p_369746_, @Nullable Direction p_230699_) {
         this.sourcePos = p_230697_;
         this.sourceBlock = p_230698_;
         this.orientation = p_369746_;
         this.skipDirection = p_230699_;
         if (NeighborUpdater.UPDATE_ORDER[this.idx] == p_230699_) {
            this.idx++;
         }
      }

      @Override
      public boolean runNext(Level p_230701_) {
         Direction direction = NeighborUpdater.UPDATE_ORDER[this.idx++];
         BlockPos blockpos = this.sourcePos.relative(direction);
         BlockState blockstate = p_230701_.getBlockState(blockpos);
         Orientation orientation = null;
         if (p_230701_.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
            if (this.orientation == null) {
               this.orientation = ExperimentalRedstoneUtils.initialOrientation(
                  p_230701_, this.skipDirection == null ? null : this.skipDirection.getOpposite(), null
               );
            }

            orientation = this.orientation.withFront(direction);
         }

         NeighborUpdater.executeUpdate(p_230701_, blockstate, blockpos, this.sourceBlock, orientation, false);
         if (this.idx < NeighborUpdater.UPDATE_ORDER.length && NeighborUpdater.UPDATE_ORDER[this.idx] == this.skipDirection) {
            this.idx++;
         }

         return this.idx < NeighborUpdater.UPDATE_ORDER.length;
      }

      @Override
      public void forEachUpdatedPos(Consumer<BlockPos> p_426183_) {
         for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
            if (direction != this.skipDirection) {
               BlockPos blockpos = this.sourcePos.relative(direction);
               p_426183_.accept(blockpos);
            }
         }
      }
   }

   interface NeighborUpdates {
      boolean runNext(Level var1);

      void forEachUpdatedPos(Consumer<BlockPos> var1);
   }

   record ShapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit)
      implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(Level p_230716_) {
         NeighborUpdater.executeShapeUpdate(p_230716_, this.direction, this.pos, this.neighborPos, this.neighborState, this.updateFlags, this.updateLimit);
         return false;
      }

      @Override
      public void forEachUpdatedPos(Consumer<BlockPos> p_428049_) {
         p_428049_.accept(this.pos);
      }
   }

   record SimpleNeighborUpdate(BlockPos pos, Block block, @Nullable Orientation orientation) implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(Level p_230734_) {
         BlockState blockstate = p_230734_.getBlockState(this.pos);
         NeighborUpdater.executeUpdate(p_230734_, blockstate, this.pos, this.block, this.orientation, false);
         return false;
      }

      @Override
      public void forEachUpdatedPos(Consumer<BlockPos> p_428050_) {
         p_428050_.accept(this.pos);
      }
   }
}
