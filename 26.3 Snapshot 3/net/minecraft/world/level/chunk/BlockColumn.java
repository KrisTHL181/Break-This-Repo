package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockColumn {
   BlockState getBlock(final int blockY);

   void setBlock(final int blockY, final BlockState state);
}
