package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public class LevelChunkTicks<T> implements TickContainerAccess<T>, SerializableTickContainer<T> {
   private static final Comparator<ScheduledTick<?>> SUB_TICK_ORDERING = Comparator.comparingLong(ScheduledTick::subTickOrder);
   private final Queue<ScheduledTick<T>> tickQueue = new PriorityQueue<>(ScheduledTick.DRAIN_ORDER);
   private @Nullable List<SavedTick<T>> pendingTicks;
   private final Set<ScheduledTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(ScheduledTick.UNIQUE_TICK_HASH);
   private @Nullable BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> onTickAdded;

   public LevelChunkTicks() {
   }

   public LevelChunkTicks(final List<SavedTick<T>> pendingTicks) {
      this.pendingTicks = pendingTicks;

      for (SavedTick<T> pendingTick : pendingTicks) {
         this.ticksPerPosition.add(ScheduledTick.probe(pendingTick.type(), pendingTick.pos()));
      }
   }

   public void setOnTickAdded(final @Nullable BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> onTickAdded) {
      this.onTickAdded = onTickAdded;
   }

   public @Nullable ScheduledTick<T> peek() {
      return this.tickQueue.peek();
   }

   public @Nullable ScheduledTick<T> poll() {
      ScheduledTick<T> result = this.tickQueue.poll();
      if (result != null) {
         this.ticksPerPosition.remove(result);
      }

      return result;
   }

   @Override
   public void schedule(final ScheduledTick<T> tick) {
      if (this.ticksPerPosition.add(tick)) {
         this.scheduleUnchecked(tick);
      }
   }

   private void scheduleUnchecked(final ScheduledTick<T> tick) {
      this.tickQueue.add(tick);
      if (this.onTickAdded != null) {
         this.onTickAdded.accept(this, tick);
      }
   }

   @Override
   public boolean hasScheduledTick(final BlockPos pos, final T type) {
      return this.ticksPerPosition.contains(ScheduledTick.probe(type, pos));
   }

   public void removeIf(final Predicate<ScheduledTick<T>> test) {
      Iterator<ScheduledTick<T>> iterator = this.tickQueue.iterator();

      while (iterator.hasNext()) {
         ScheduledTick<T> tick = iterator.next();
         if (test.test(tick)) {
            iterator.remove();
            this.ticksPerPosition.remove(tick);
         }
      }
   }

   public Stream<ScheduledTick<T>> getAll() {
      return this.tickQueue.stream();
   }

   @Override
   public int count() {
      return this.tickQueue.size() + (this.pendingTicks != null ? this.pendingTicks.size() : 0);
   }

   @Override
   public List<SavedTick<T>> pack(final long currentTick) {
      List<SavedTick<T>> ticks = new ArrayList<>(this.tickQueue.size());
      if (this.pendingTicks != null) {
         ticks.addAll(this.pendingTicks);
      }

      List<ScheduledTick<T>> sortedTicks = new ArrayList<>(this.tickQueue);
      sortedTicks.sort(SUB_TICK_ORDERING);

      for (ScheduledTick<T> tick : sortedTicks) {
         ticks.add(tick.toSavedTick(currentTick));
      }

      return ticks;
   }

   public void unpack(final long currentTick) {
      if (this.pendingTicks != null) {
         int subTickBase = -this.pendingTicks.size();

         for (SavedTick<T> pendingTick : this.pendingTicks) {
            this.scheduleUnchecked(pendingTick.unpack(currentTick, subTickBase++));
         }
      }

      this.pendingTicks = null;
   }
}
