package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.util.profiling.metrics.MetricsRegistry;

public class PriorityConsecutiveExecutor extends AbstractConsecutiveExecutor<StrictQueue.RunnableWithPriority> {
   public PriorityConsecutiveExecutor(int p_368950_, Executor p_370095_, String p_367806_) {
      super(new StrictQueue.FixedPriorityQueue(p_368950_), p_370095_, p_367806_);
      MetricsRegistry.INSTANCE.add(this);
   }

   public StrictQueue.RunnableWithPriority wrapRunnable(Runnable p_370061_) {
      return new StrictQueue.RunnableWithPriority(0, p_370061_);
   }

   public <Source> CompletableFuture<Source> scheduleWithResult(int p_364483_, Consumer<CompletableFuture<Source>> p_367272_) {
      CompletableFuture<Source> completablefuture = new CompletableFuture<>();
      this.schedule(new StrictQueue.RunnableWithPriority(p_364483_, () -> p_367272_.accept(completablefuture)));
      return completablefuture;
   }
}
