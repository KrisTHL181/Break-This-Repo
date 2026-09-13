package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockEntityTicker<T extends BlockEntity> {
   void tick(final Level level, final BlockPos pos, final BlockState state, final T entity);

   default BlockEntityTicker<T> andThen(final BlockEntityTicker<? super T> after) {
      return (level, pos, state, entity) -> {
         this.tick(level, pos, state, entity);
         after.tick(level, pos, state, entity);
      };
   }
}
