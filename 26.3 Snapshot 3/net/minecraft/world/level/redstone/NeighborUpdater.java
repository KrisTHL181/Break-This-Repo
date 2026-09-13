package net.minecraft.world.level.redstone;

import java.util.Locale;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface NeighborUpdater {
   Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

   void shapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit);

   void neighborChanged(BlockPos pos, Block changedBlock, @Nullable Orientation orientation);

   void neighborChanged(BlockState state, BlockPos pos, Block changedBlock, @Nullable Orientation orientation, boolean movedByPiston);

   default void updateNeighborsAtExceptFromFacing(
      final BlockPos pos, final Block block, final @Nullable Direction skipDirection, final @Nullable Orientation orientation
   ) {
      for (Direction direction : UPDATE_ORDER) {
         if (direction != skipDirection) {
            this.neighborChanged(pos.relative(direction), block, null);
         }
      }
   }

   static void executeShapeUpdate(
      final LevelAccessor level,
      final Direction direction,
      final BlockPos pos,
      final BlockPos neighborPos,
      final BlockState neighborState,
      final @Block.UpdateFlags int updateFlags,
      final int updateLimit
   ) {
      BlockState currentState = level.getBlockState(pos);
      if ((updateFlags & 128) == 0 || !currentState.is(Blocks.REDSTONE_WIRE)) {
         try {
            BlockState newState = currentState.updateShape(level, level, pos, direction, neighborPos, neighborState, level.getRandom());
            Block.updateOrDestroy(currentState, newState, level, pos, updateFlags, updateLimit);
         } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exception while updating neighbour shapes");
            CrashReportCategory ownCategory = report.addCategory("Block being updated");
            CrashReportCategory.populateBlockDetails(ownCategory, level, pos, currentState);
            CrashReportCategory neighborCategory = report.addCategory("Neighbor block");
            CrashReportCategory.populateBlockDetails(neighborCategory, level, neighborPos, neighborState);
            throw new ReportedException(report);
         }
      }
   }

   static void executeUpdate(
      final Level level,
      final BlockState state,
      final BlockPos pos,
      final Block changedBlock,
      final @Nullable Orientation orientation,
      final boolean movedByPiston
   ) {
      try {
         state.handleNeighborChanged(level, pos, changedBlock, orientation, movedByPiston);
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Exception while updating neighbours");
         CrashReportCategory category = report.addCategory("Block being updated");
         category.setDetail(
            "Source block type",
            () -> {
               try {
                  return String.format(
                     Locale.ROOT,
                     "ID #%s (%s // %s)",
                     BuiltInRegistries.BLOCK.getKey(changedBlock),
                     changedBlock.getDescriptionId(),
                     changedBlock.getClass().getCanonicalName()
                  );
               } catch (Throwable ignored) {
                  return "ID #" + BuiltInRegistries.BLOCK.getKey(changedBlock);
               }
            }
         );
         CrashReportCategory.populateBlockDetails(category, level, pos, state);
         throw new ReportedException(report);
      }
   }
}
