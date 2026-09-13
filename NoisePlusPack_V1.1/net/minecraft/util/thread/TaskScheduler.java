package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface TaskScheduler<R extends Runnable> extends AutoCloseable {
   String name();

   void schedule(R var1);

   @Override
   default void close() {
   }

   R wrapRunnable(Runnable var1);

   default <Source> CompletableFuture<Source> scheduleWithResult(Consumer<CompletableFuture<Source>> p_365778_) {
      CompletableFuture<Source> completablefuture = new CompletableFuture<>();
      this.schedule(this.wrapRunnable(() -> p_365778_.accept(completablefuture)));
      return completablefuture;
   }

   static TaskScheduler<Runnable> wrapExecutor(final String p_367076_, final Executor p_363384_) {
      return new TaskScheduler<Runnable>() {
         @Override
         public String name() {
            return p_367076_;
         }

         @Override
         public void schedule(Runnable p_361412_) {
            p_363384_.execute(p_361412_);
         }

         @Override
         public Runnable wrapRunnable(Runnable p_367104_) {
            return p_367104_;
         }

         @Override
         public String toString() {
            return p_367076_;
         }
      };
   }
}
