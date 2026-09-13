package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface LevelWriter {
   boolean setBlock(BlockPos var1, BlockState var2, @Block.UpdateFlags int var3, int var4);

   default boolean setBlock(BlockPos p_46944_, BlockState p_46945_, @Block.UpdateFlags int p_46946_) {
      return this.setBlock(p_46944_, p_46945_, p_46946_, 512);
   }

   boolean removeBlock(BlockPos var1, boolean var2);

   default boolean destroyBlock(BlockPos p_46962_, boolean p_46963_) {
      return this.destroyBlock(p_46962_, p_46963_, null);
   }

   default boolean destroyBlock(BlockPos p_46954_, boolean p_46955_, @Nullable Entity p_46956_) {
      return this.destroyBlock(p_46954_, p_46955_, p_46956_, 512);
   }

   boolean destroyBlock(BlockPos var1, boolean var2, @Nullable Entity var3, int var4);

   default boolean addFreshEntity(Entity p_46964_) {
      return false;
   }
}
