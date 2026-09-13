package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface LevelWriter {
   boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags, int updateLimit);

   default boolean setBlockAndUpdate(final BlockPos pos, final BlockState blockState) {
      return this.setBlock(pos, blockState, 3);
   }

   default boolean setBlock(final BlockPos pos, final BlockState blockState, final @Block.UpdateFlags int updateFlags) {
      return this.setBlock(pos, blockState, updateFlags, 512);
   }

   boolean removeBlock(BlockPos pos, boolean movedByPiston);

   default boolean destroyBlock(final BlockPos pos, final boolean dropResources) {
      return this.destroyBlock(pos, dropResources, null);
   }

   default boolean destroyBlock(final BlockPos pos, final boolean dropResources, final @Nullable Entity breaker) {
      return this.destroyBlock(pos, dropResources, breaker, 512);
   }

   boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit);

   default boolean addFreshEntity(final Entity entity) {
      return false;
   }
}
