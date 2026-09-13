package net.minecraft.world.ticks;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;

public class ProtoChunkTicks<T> implements SerializableTickContainer<T>, TickContainerAccess<T> {
   private final List<SavedTick<T>> ticks = Lists.newArrayList();
   private final Set<SavedTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(SavedTick.UNIQUE_TICK_HASH);

   @Override
   public void schedule(ScheduledTick<T> p_193298_) {
      SavedTick<T> savedtick = new SavedTick<>(p_193298_.type(), p_193298_.pos(), 0, p_193298_.priority());
      this.schedule(savedtick);
   }

   private void schedule(SavedTick<T> p_193296_) {
      if (this.ticksPerPosition.add(p_193296_)) {
         this.ticks.add(p_193296_);
      }
   }

   @Override
   public boolean hasScheduledTick(BlockPos p_193300_, T p_193301_) {
      return this.ticksPerPosition.contains(SavedTick.probe(p_193301_, p_193300_));
   }

   @Override
   public int count() {
      return this.ticks.size();
   }

   @Override
   public List<SavedTick<T>> pack(long p_364150_) {
      return this.ticks;
   }

   public List<SavedTick<T>> scheduledTicks() {
      return List.copyOf(this.ticks);
   }

   public static <T> ProtoChunkTicks<T> load(List<SavedTick<T>> p_370194_) {
      ProtoChunkTicks<T> protochunkticks = new ProtoChunkTicks<>();
      p_370194_.forEach(protochunkticks::schedule);
      return protochunkticks;
   }
}
