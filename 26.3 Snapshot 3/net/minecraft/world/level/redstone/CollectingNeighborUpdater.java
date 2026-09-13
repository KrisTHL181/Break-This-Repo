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

   public CollectingNeighborUpdater(final Level level, final int maxChainedNeighborUpdates) {
      this.level = level;
      this.maxChainedNeighborUpdates = maxChainedNeighborUpdates;
   }

   public void setDebugListener(final @Nullable Consumer<BlockPos> debugListener) {
      this.debugListener = debugListener;
   }

   @Override
   public void shapeUpdate(
      final Direction direction,
      final BlockState neighborState,
      final BlockPos pos,
      final BlockPos neighborPos,
      final @Block.UpdateFlags int updateFlags,
      final int updateLimit
   ) {
      this.addAndRun(
         pos, new CollectingNeighborUpdater.ShapeUpdate(direction, neighborState, pos.immutable(), neighborPos.immutable(), updateFlags, updateLimit)
      );
   }

   @Override
   public void neighborChanged(final BlockPos pos, final Block block, final @Nullable Orientation orientation) {
      this.addAndRun(pos, new CollectingNeighborUpdater.SimpleNeighborUpdate(pos, block, orientation));
   }

   @Override
   public void neighborChanged(
      final BlockState state, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      this.addAndRun(pos, new CollectingNeighborUpdater.FullNeighborUpdate(state, pos.immutable(), block, orientation, movedByPiston));
   }

   @Override
   public void updateNeighborsAtExceptFromFacing(
      final BlockPos pos, final Block block, final @Nullable Direction skipDirection, final @Nullable Orientation orientation
   ) {
      this.addAndRun(pos, new CollectingNeighborUpdater.MultiNeighborUpdate(pos.immutable(), block, orientation, skipDirection));
   }

   private void addAndRun(final BlockPos pos, final CollectingNeighborUpdater.NeighborUpdates update) {
      boolean runningAlready = this.count > 0;
      boolean tooManyUpdates = this.maxChainedNeighborUpdates >= 0 && this.count >= this.maxChainedNeighborUpdates;
      this.count++;
      if (!tooManyUpdates) {
         if (runningAlready) {
            this.addedThisLayer.add(update);
         } else {
            this.stack.push(update);
         }
      } else if (this.count - 1 == this.maxChainedNeighborUpdates) {
         LOGGER.error("Too many chained neighbor updates. Skipping the rest. First skipped position: {}", pos.toShortString());
      }

      if (!runningAlready) {
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
            CollectingNeighborUpdater.NeighborUpdates nextUpdates = this.stack.peek();
            if (this.debugListener != null) {
               nextUpdates.forEachUpdatedPos(this.debugListener);
            }

            while (this.addedThisLayer.isEmpty()) {
               if (!nextUpdates.runNext(this.level)) {
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

   private record FullNeighborUpdate(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston)
      implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(final Level level) {
         NeighborUpdater.executeUpdate(level, this.state, this.pos, this.block, this.orientation, this.movedByPiston);
         return false;
      }

      @Override
      public void forEachUpdatedPos(final Consumer<BlockPos> output) {
         output.accept(this.pos);
      }
   }

   private static final class MultiNeighborUpdate implements CollectingNeighborUpdater.NeighborUpdates {
      private final BlockPos sourcePos;
      private final Block sourceBlock;
      private @Nullable Orientation orientation;
      private final @Nullable Direction skipDirection;
      private int idx = 0;

      public MultiNeighborUpdate(
         final BlockPos sourcePos, final Block sourceBlock, final @Nullable Orientation orientation, final @Nullable Direction skipDirection
      ) {
         this.sourcePos = sourcePos;
         this.sourceBlock = sourceBlock;
         this.orientation = orientation;
         this.skipDirection = skipDirection;
         if (NeighborUpdater.UPDATE_ORDER[this.idx] == skipDirection) {
            this.idx++;
         }
      }

      @Override
      public boolean runNext(final Level level) {
         Direction direction = NeighborUpdater.UPDATE_ORDER[this.idx++];
         BlockPos neighborPos = this.sourcePos.relative(direction);
         BlockState state = level.getBlockState(neighborPos);
         Orientation orientation = null;
         if (level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
            if (this.orientation == null) {
               this.orientation = ExperimentalRedstoneUtils.initialOrientation(
                  level, this.skipDirection == null ? null : this.skipDirection.getOpposite(), null
               );
            }

            orientation = this.orientation.withFront(direction);
         }

         NeighborUpdater.executeUpdate(level, state, neighborPos, this.sourceBlock, orientation, false);
         if (this.idx < NeighborUpdater.UPDATE_ORDER.length && NeighborUpdater.UPDATE_ORDER[this.idx] == this.skipDirection) {
            this.idx++;
         }

         return this.idx < NeighborUpdater.UPDATE_ORDER.length;
      }

      @Override
      public void forEachUpdatedPos(final Consumer<BlockPos> output) {
         for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
            if (direction != this.skipDirection) {
               BlockPos neighborPos = this.sourcePos.relative(direction);
               output.accept(neighborPos);
            }
         }
      }
   }

   private interface NeighborUpdates {
      boolean runNext(Level level);

      void forEachUpdatedPos(Consumer<BlockPos> output);
   }

   private record ShapeUpdate(
      Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit
   ) implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(final Level level) {
         NeighborUpdater.executeShapeUpdate(level, this.direction, this.pos, this.neighborPos, this.neighborState, this.updateFlags, this.updateLimit);
         return false;
      }

      @Override
      public void forEachUpdatedPos(final Consumer<BlockPos> output) {
         output.accept(this.pos);
      }
   }

   private record SimpleNeighborUpdate(BlockPos pos, Block block, @Nullable Orientation orientation) implements CollectingNeighborUpdater.NeighborUpdates {
      @Override
      public boolean runNext(final Level level) {
         BlockState state = level.getBlockState(this.pos);
         NeighborUpdater.executeUpdate(level, state, this.pos, this.block, this.orientation, false);
         return false;
      }

      @Override
      public void forEachUpdatedPos(final Consumer<BlockPos> output) {
         output.accept(this.pos);
      }
   }
}
