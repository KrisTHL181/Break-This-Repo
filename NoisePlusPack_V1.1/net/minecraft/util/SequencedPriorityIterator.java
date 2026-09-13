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

   public void add(T p_312570_, int p_312199_) {
      if (p_312199_ == this.highestPrio && this.highestPrioQueue != null) {
         this.highestPrioQueue.addLast(p_312570_);
      } else {
         Deque<T> deque = (Deque<T>)this.queuesByPriority.computeIfAbsent(p_312199_, p_310516_ -> Queues.newArrayDeque());
         deque.addLast(p_312570_);
         if (p_312199_ >= this.highestPrio) {
            this.highestPrioQueue = deque;
            this.highestPrio = p_312199_;
         }
      }
   }

   protected @Nullable T computeNext() {
      if (this.highestPrioQueue == null) {
         return (T)this.endOfData();
      }

      T t = this.highestPrioQueue.removeFirst();
      if (t == null) {
         return (T)this.endOfData();
      }

      if (this.highestPrioQueue.isEmpty()) {
         this.switchCacheToNextHighestPrioQueue();
      }

      return t;
   }

   private void switchCacheToNextHighestPrioQueue() {
      int i = Integer.MIN_VALUE;
      Deque<T> deque = null;
      ObjectIterator var3 = Int2ObjectMaps.fastIterable(this.queuesByPriority).iterator();

      while (var3.hasNext()) {
         Entry<Deque<T>> entry = (Entry<Deque<T>>)var3.next();
         Deque<T> deque1 = (Deque<T>)entry.getValue();
         int j = entry.getIntKey();
         if (j > i && !deque1.isEmpty()) {
            i = j;
            deque = deque1;
            if (j == this.highestPrio - 1) {
               break;
            }
         }
      }

      this.highestPrio = i;
      this.highestPrioQueue = deque;
   }
}
