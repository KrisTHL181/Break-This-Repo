package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

public interface StrictQueue<T extends Runnable> {
   @Nullable Runnable pop();

   boolean push(final T t);

   boolean isEmpty();

   int size();

   final class FixedPriorityQueue implements StrictQueue<StrictQueue.RunnableWithPriority> {
      private final Queue<Runnable>[] queues;
      private final AtomicInteger size = new AtomicInteger();

      public FixedPriorityQueue(final int size) {
         this.queues = new Queue[size];

         for (int i = 0; i < size; i++) {
            this.queues[i] = Queues.newConcurrentLinkedQueue();
         }
      }

      @Override
      public @Nullable Runnable pop() {
         for (Queue<Runnable> queue : this.queues) {
            Runnable task = queue.poll();
            if (task != null) {
               this.size.decrementAndGet();
               return task;
            }
         }

         return null;
      }

      public boolean push(final StrictQueue.RunnableWithPriority task) {
         int priority = task.priority;
         if (priority < this.queues.length && priority >= 0) {
            this.queues[priority].add(task);
            this.size.incrementAndGet();
            return true;
         } else {
            throw new IndexOutOfBoundsException(
               String.format(Locale.ROOT, "Priority %d not supported. Expected range [0-%d]", priority, this.queues.length - 1)
            );
         }
      }

      @Override
      public boolean isEmpty() {
         return this.size.get() == 0;
      }

      @Override
      public int size() {
         return this.size.get();
      }
   }

   final class QueueStrictQueue implements StrictQueue<Runnable> {
      private final Queue<Runnable> queue;

      public QueueStrictQueue(final Queue<Runnable> queue) {
         this.queue = queue;
      }

      @Override
      public @Nullable Runnable pop() {
         return this.queue.poll();
      }

      @Override
      public boolean push(final Runnable t) {
         return this.queue.add(t);
      }

      @Override
      public boolean isEmpty() {
         return this.queue.isEmpty();
      }

      @Override
      public int size() {
         return this.queue.size();
      }
   }

   record RunnableWithPriority(int priority, Runnable task) implements Runnable {
      @Override
      public void run() {
         this.task.run();
      }
   }
}
