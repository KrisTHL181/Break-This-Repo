package net.minecraft.world.ticks;

import java.util.function.Function;
import net.minecraft.core.BlockPos;

public class WorldGenTickAccess<T> implements LevelTickAccess<T> {
   private final Function<BlockPos, TickContainerAccess<T>> containerGetter;

   public WorldGenTickAccess(final Function<BlockPos, TickContainerAccess<T>> containerGetter) {
      this.containerGetter = containerGetter;
   }

   @Override
   public boolean hasScheduledTick(final BlockPos pos, final T type) {
      return this.containerGetter.apply(pos).hasScheduledTick(pos, type);
   }

   @Override
   public void schedule(final ScheduledTick<T> tick) {
      this.containerGetter.apply(tick.pos()).schedule(tick);
   }

   @Override
   public boolean willTickThisTick(final BlockPos pos, final T type) {
      return false;
   }

   @Override
   public int count() {
      return 0;
   }
}
