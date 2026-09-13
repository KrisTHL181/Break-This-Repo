package net.minecraft.util;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class SequencedPriorityIterator<T> extends AbstractIterator<T> {
   private static final int MIN_PRIO = Integer.MIN_VALUE;
   private @Nullable Deque<T> highestPrioQueue = null;
   private int highestPrio = Integer.MIN_VALUE;
   private final Int2ObjectMap<Deque<T>> queuesByPriority = new Int2ObjectOpenHashMap();

   public void add(final T data, final int priority) {
      if (priority == this.highestPrio && this.highestPrioQueue != null) {
         this.highestPrioQueue.addLast(data);
      } else {
         Deque<T> queue = (Deque<T>)this.queuesByPriority.computeIfAbsent(priority, order -> Queues.newArrayDeque());
         queue.addLast(data);
         if (priority >= this.highestPrio) {
            this.highestPrioQueue = queue;
            this.highestPrio = priority;
         }
      }
   }

   protected @Nullable T computeNext() {
      if (this.highestPrioQueue == null) {
         return (T)this.endOfData();
      }

      T result = this.highestPrioQueue.removeFirst();
      if (result == null) {
         return (T)this.endOfData();
      }

      if (this.highestPrioQueue.isEmpty()) {
         this.switchCacheToNextHighestPrioQueue();
      }

      return result;
   }

   private void switchCacheToNextHighestPrioQueue() {
      int foundHighestPrio = Integer.MIN_VALUE;
      Deque<T> foundHighestPrioQueue = null;
      ObjectIterator var3 = Int2ObjectMaps.fastIterable(this.queuesByPriority).iterator();

      while (var3.hasNext()) {
         Entry<Deque<T>> entry = (Entry<Deque<T>>)var3.next();
         Deque<T> queue = (Deque<T>)entry.getValue();
         int prio = entry.getIntKey();
         if (prio > foundHighestPrio && !queue.isEmpty()) {
            foundHighestPrio = prio;
            foundHighestPrioQueue = queue;
            if (prio == this.highestPrio - 1) {
               break;
            }
         }
      }

      this.highestPrio = foundHighestPrio;
      this.highestPrioQueue = foundHighestPrioQueue;
   }
}
