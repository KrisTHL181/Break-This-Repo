package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockColumn {
   BlockState getBlock(int var1);

   void setBlock(int var1, BlockState var2);
}
