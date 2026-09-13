package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HangingSignItem extends SignItem {
   public HangingSignItem(Block p_251582_, Block p_250734_, Item.Properties p_250266_) {
      super(p_250266_, p_251582_, p_250734_, Direction.UP);
   }

   @Override
   protected boolean canPlace(LevelReader p_252032_, BlockState p_252230_, BlockPos p_252075_) {
      return p_252230_.getBlock() instanceof WallHangingSignBlock wallhangingsignblock && !wallhangingsignblock.canPlace(p_252230_, p_252032_, p_252075_)
         ? false
         : super.canPlace(p_252032_, p_252230_, p_252075_);
   }
}
