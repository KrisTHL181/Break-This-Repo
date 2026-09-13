package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
   int SCAN_DISTANCE = 4;

   Optional<BlockState> getNext(BlockState var1);

   float getChanceModifier();

   default void changeOverTime(BlockState p_311790_, ServerLevel p_309416_, BlockPos p_310092_, RandomSource p_310572_) {
      float f = 0.05688889F;
      if (p_310572_.nextFloat() < 0.05688889F) {
         this.getNextState(p_311790_, p_309416_, p_310092_, p_310572_).ifPresent(p_405685_ -> p_309416_.setBlockAndUpdate(p_310092_, p_405685_));
      }
   }

   T getAge();

   default Optional<BlockState> getNextState(BlockState p_311503_, ServerLevel p_311331_, BlockPos p_309459_, RandomSource p_312041_) {
      int i = this.getAge().ordinal();
      int j = 0;
      int k = 0;

      for (BlockPos blockpos : BlockPos.withinManhattan(p_309459_, 4, 4, 4)) {
         int l = blockpos.distManhattan(p_309459_);
         if (l > 4) {
            break;
         }

         if (!blockpos.equals(p_309459_) && p_311331_.getBlockState(blockpos).getBlock() instanceof ChangeOverTimeBlock<?> changeovertimeblock) {
            Enum<?> oenum = changeovertimeblock.getAge();
            if (this.getAge().getClass() == oenum.getClass()) {
               int i1 = oenum.ordinal();
               if (i1 < i) {
                  return Optional.empty();
               }

               if (i1 > i) {
                  k++;
               } else {
                  j++;
               }
            }
         }
      }

      float f = (float)(k + 1) / (k + j + 1);
      float f1 = f * f * this.getChanceModifier();
      return p_312041_.nextFloat() < f1 ? this.getNext(p_311503_) : Optional.empty();
   }
}
