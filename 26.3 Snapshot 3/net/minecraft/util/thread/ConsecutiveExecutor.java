package net.minecraft.util.thread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class ConsecutiveExecutor extends AbstractConsecutiveExecutor<Runnable> {
   public ConsecutiveExecutor(final Executor dispatcher, final String name) {
      super(new StrictQueue.QueueStrictQueue(new ConcurrentLinkedQueue<>()), dispatcher, name);
   }

   @Override
   public Runnable wrapRunnable(final Runnable runnable) {
      return runnable;
   }
}
