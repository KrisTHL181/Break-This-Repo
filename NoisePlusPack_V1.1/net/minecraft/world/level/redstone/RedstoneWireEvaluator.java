package net.minecraft.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class RedstoneWireEvaluator {
   protected final RedStoneWireBlock wireBlock;

   protected RedstoneWireEvaluator(RedStoneWireBlock p_363440_) {
      this.wireBlock = p_363440_;
   }

   public abstract void updatePowerStrength(Level var1, BlockPos var2, BlockState var3, @Nullable Orientation var4, boolean var5);

   protected int getBlockSignal(Level p_366082_, BlockPos p_361408_) {
      return this.wireBlock.getBlockSignal(p_366082_, p_361408_);
   }

   protected int getWireSignal(BlockPos p_362036_, BlockState p_369122_) {
      return p_369122_.is(this.wireBlock) ? p_369122_.getValue(RedStoneWireBlock.POWER) : 0;
   }

   protected int getIncomingWireSignal(Level p_365027_, BlockPos p_369500_) {
      int i = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_369500_.relative(direction);
         BlockState blockstate = p_365027_.getBlockState(blockpos);
         i = Math.max(i, this.getWireSignal(blockpos, blockstate));
         BlockPos blockpos1 = p_369500_.above();
         if (blockstate.isRedstoneConductor(p_365027_, blockpos) && !p_365027_.getBlockState(blockpos1).isRedstoneConductor(p_365027_, blockpos1)) {
            BlockPos blockpos3 = blockpos.above();
            i = Math.max(i, this.getWireSignal(blockpos3, p_365027_.getBlockState(blockpos3)));
         } else if (!blockstate.isRedstoneConductor(p_365027_, blockpos)) {
            BlockPos blockpos2 = blockpos.below();
            i = Math.max(i, this.getWireSignal(blockpos2, p_365027_.getBlockState(blockpos2)));
         }
      }

      return Math.max(0, i - 1);
   }
}
