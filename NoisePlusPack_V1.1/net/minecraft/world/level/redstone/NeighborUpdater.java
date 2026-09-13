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

   void shapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, @Block.UpdateFlags int var5, int var6);

   void neighborChanged(BlockPos var1, Block var2, @Nullable Orientation var3);

   void neighborChanged(BlockState var1, BlockPos var2, Block var3, @Nullable Orientation var4, boolean var5);

   default void updateNeighborsAtExceptFromFacing(BlockPos p_230788_, Block p_230789_, @Nullable Direction p_230790_, @Nullable Orientation p_361940_) {
      for (Direction direction : UPDATE_ORDER) {
         if (direction != p_230790_) {
            this.neighborChanged(p_230788_.relative(direction), p_230789_, null);
         }
      }
   }

   static void executeShapeUpdate(
      LevelAccessor p_230771_,
      Direction p_230772_,
      BlockPos p_230774_,
      BlockPos p_230775_,
      BlockState p_230773_,
      @Block.UpdateFlags int p_230776_,
      int p_230777_
   ) {
      BlockState blockstate = p_230771_.getBlockState(p_230774_);
      if ((p_230776_ & 128) == 0 || !blockstate.is(Blocks.REDSTONE_WIRE)) {
         BlockState blockstate1 = blockstate.updateShape(p_230771_, p_230771_, p_230774_, p_230772_, p_230775_, p_230773_, p_230771_.getRandom());
         Block.updateOrDestroy(blockstate, blockstate1, p_230771_, p_230774_, p_230776_, p_230777_);
      }
   }

   static void executeUpdate(Level p_230764_, BlockState p_230765_, BlockPos p_230766_, Block p_230767_, @Nullable Orientation p_364742_, boolean p_230769_) {
      try {
         p_230765_.handleNeighborChanged(p_230764_, p_230766_, p_230767_, p_364742_, p_230769_);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while updating neighbours");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Block being updated");
         crashreportcategory.setDetail(
            "Source block type",
            () -> {
               try {
                  return String.format(
                     Locale.ROOT,
                     "ID #%s (%s // %s)",
                     BuiltInRegistries.BLOCK.getKey(p_230767_),
                     p_230767_.getDescriptionId(),
                     p_230767_.getClass().getCanonicalName()
                  );
               } catch (Throwable throwable1) {
                  return "ID #" + BuiltInRegistries.BLOCK.getKey(p_230767_);
               }
            }
         );
         CrashReportCategory.populateBlockDetails(crashreportcategory, p_230764_, p_230766_, p_230765_);
         throw new ReportedException(crashreport);
      }
   }
}
