package com.mojang.realmsclient.gui.task;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.util.TimeSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DataFetcher {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Executor executor;
   private final TimeUnit resolution;
   private final TimeSource timeSource;

   public DataFetcher(final Executor executor, final TimeUnit resolution, final TimeSource timeSource) {
      this.executor = executor;
      this.resolution = resolution;
      this.timeSource = timeSource;
   }

   public <T> DataFetcher.Task<T> createTask(final String id, final Callable<T> updater, final Duration period, final RepeatedDelayStrategy repeatStrategy) {
      long periodInUnit = this.resolution.convert(period);
      if (periodInUnit == 0L) {
         throw new IllegalArgumentException("Period of " + period + " too short for selected resolution of " + this.resolution);
      } else {
         return new DataFetcher.Task<>(id, updater, periodInUnit, repeatStrategy);
      }
   }

   public DataFetcher.Subscription createSubscription() {
      return new DataFetcher.Subscription();
   }

   private record ComputationResult<T>(Either<T, Exception> value, long time) {
   }

   private class SubscribedTask<T> {
      private final DataFetcher.Task<T> task;
      private final Consumer<T> output;
      private long lastCheckTime = -1L;

      private SubscribedTask(final DataFetcher.Task<T> task, final Consumer<T> output) {
         this.task = task;
         this.output = output;
      }

      private void update(final long currentTime) {
         this.task.updateIfNeeded(currentTime);
         this.runCallbackIfNeeded();
      }

      private void runCallbackIfNeeded() {
         DataFetcher.SuccessfulComputationResult<T> lastResult = this.task.lastResult;
         if (lastResult != null && this.lastCheckTime < lastResult.time) {
            this.output.accept(lastResult.value);
            this.lastCheckTime = lastResult.time;
         }
      }

      private void runCallback() {
         DataFetcher.SuccessfulComputationResult<T> lastResult = this.task.lastResult;
         if (lastResult != null) {
            this.output.accept(lastResult.value);
            this.lastCheckTime = lastResult.time;
         }
      }

      private void reset() {
         this.task.reset();
         this.lastCheckTime = -1L;
      }
   }

   public class Subscription {
      private final List<DataFetcher.SubscribedTask<?>> subscriptions = new ArrayList<>();

      public <T> void subscribe(final DataFetcher.Task<T> task, final Consumer<T> output) {
         DataFetcher.SubscribedTask<T> subscription = DataFetcher.this.new SubscribedTask<>(task, output);
         this.subscriptions.add(subscription);
         subscription.runCallbackIfNeeded();
      }

      public void forceUpdate() {
         for (DataFetcher.SubscribedTask<?> subscription : this.subscriptions) {
            subscription.runCallback();
         }
      }

      public void tick() {
         for (DataFetcher.SubscribedTask<?> subscription : this.subscriptions) {
            subscription.update(DataFetcher.this.timeSource.get(DataFetcher.this.resolution));
         }
      }

      public void reset() {
         for (DataFetcher.SubscribedTask<?> subscription : this.subscriptions) {
            subscription.reset();
         }
      }
   }

   private record SuccessfulComputationResult<T>(T value, long time) {
   }

   public class Task<T> {
      private final String id;
      private final Callable<T> updater;
      private final long period;
      private final RepeatedDelayStrategy repeatStrategy;
      private @Nullable CompletableFuture<DataFetcher.ComputationResult<T>> pendingTask;
      private DataFetcher.@Nullable SuccessfulComputationResult<T> lastResult;
      private long nextUpdate = -1L;

      private Task(final String id, final Callable<T> updater, final long period, final RepeatedDelayStrategy repeatStrategy) {
         this.id = id;
         this.updater = updater;
         this.period = period;
         this.repeatStrategy = repeatStrategy;
      }

      private void updateIfNeeded(final long currentTime) {
         if (this.pendingTask != null) {
            DataFetcher.ComputationResult<T> result = this.pendingTask.getNow(null);
            if (result == null) {
               return;
            }

            this.pendingTask = null;
            long completionTime = result.time;
            result.value().ifLeft(value -> {
               this.lastResult = new DataFetcher.SuccessfulComputationResult<>((T)value, completionTime);
               this.nextUpdate = completionTime + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
            }).ifRight(e -> {
               long cycles = this.repeatStrategy.delayCyclesAfterFailure();
               DataFetcher.LOGGER.warn("Failed to process task {}, will repeat after {} cycles", new Object[]{this.id, cycles, e});
               this.nextUpdate = completionTime + this.period * cycles;
            });
         }

         if (this.nextUpdate <= currentTime) {
            this.pendingTask = CompletableFuture.supplyAsync(() -> {
               try {
                  T resultx = this.updater.call();
                  long completionTimex = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.left(resultx), completionTimex);
               } catch (Exception e) {
                  long completionTimexx = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.right(e), completionTimexx);
               }
            }, DataFetcher.this.executor);
         }
      }

      public void reset() {
         this.pendingTask = null;
         this.lastResult = null;
         this.nextUpdate = -1L;
      }
   }
}
