package net.minecraft.world.ticks;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;

public class ProtoChunkTicks<T> implements TickContainerAccess<T>, SerializableTickContainer<T> {
   private final List<SavedTick<T>> ticks = Lists.newArrayList();
   private final Set<SavedTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(SavedTick.UNIQUE_TICK_HASH);

   @Override
   public void schedule(final ScheduledTick<T> tick) {
      SavedTick<T> newTick = new SavedTick<>(tick.type(), tick.pos(), 0, tick.priority());
      this.schedule(newTick);
   }

   private void schedule(final SavedTick<T> newTick) {
      if (this.ticksPerPosition.add(newTick)) {
         this.ticks.add(newTick);
      }
   }

   @Override
   public boolean hasScheduledTick(final BlockPos pos, final T type) {
      return this.ticksPerPosition.contains(SavedTick.probe(type, pos));
   }

   @Override
   public int count() {
      return this.ticks.size();
   }

   @Override
   public List<SavedTick<T>> pack(final long currentTick) {
      return this.ticks;
   }

   public List<SavedTick<T>> scheduledTicks() {
      return List.copyOf(this.ticks);
   }

   public static <T> ProtoChunkTicks<T> load(final List<SavedTick<T>> ticks) {
      ProtoChunkTicks<T> result = new ProtoChunkTicks<>();
      ticks.forEach(result::schedule);
      return result;
   }
}
