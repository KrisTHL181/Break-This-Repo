package net.minecraft.util.thread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class ConsecutiveExecutor extends AbstractConsecutiveExecutor<Runnable> {
   public ConsecutiveExecutor(Executor p_368256_, String p_367600_) {
      super(new StrictQueue.QueueStrictQueue(new ConcurrentLinkedQueue<>()), p_368256_, p_367600_);
   }

   @Override
   public Runnable wrapRunnable(Runnable p_360929_) {
      return p_360929_;
   }
}
