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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DataFetcher {
   static final Logger LOGGER = LogUtils.getLogger();
   final Executor executor;
   final TimeUnit resolution;
   final TimeSource timeSource;

   public DataFetcher(Executor p_239381_, TimeUnit p_239382_, TimeSource p_239383_) {
      this.executor = p_239381_;
      this.resolution = p_239382_;
      this.timeSource = p_239383_;
   }

   public <T> DataFetcher.Task<T> createTask(String p_239623_, Callable<T> p_239624_, Duration p_239625_, RepeatedDelayStrategy p_239626_) {
      long i = this.resolution.convert(p_239625_);
      if (i == 0L) {
         throw new IllegalArgumentException("Period of " + p_239625_ + " too short for selected resolution of " + this.resolution);
      } else {
         return new DataFetcher.Task<>(p_239623_, p_239624_, i, p_239626_);
      }
   }

   public DataFetcher.Subscription createSubscription() {
      return new DataFetcher.Subscription();
   }

   @OnlyIn(Dist.CLIENT)
   record ComputationResult<T>(Either<T, Exception> value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   class SubscribedTask<T> {
      private final DataFetcher.Task<T> task;
      private final Consumer<T> output;
      private long lastCheckTime = -1L;

      SubscribedTask(final DataFetcher.Task<T> p_239959_, final Consumer<T> p_239960_) {
         this.task = p_239959_;
         this.output = p_239960_;
      }

      void update(long p_239226_) {
         this.task.updateIfNeeded(p_239226_);
         this.runCallbackIfNeeded();
      }

      void runCallbackIfNeeded() {
         DataFetcher.SuccessfulComputationResult<T> successfulcomputationresult = this.task.lastResult;
         if (successfulcomputationresult != null && this.lastCheckTime < successfulcomputationresult.time) {
            this.output.accept(successfulcomputationresult.value);
            this.lastCheckTime = successfulcomputationresult.time;
         }
      }

      void runCallback() {
         DataFetcher.SuccessfulComputationResult<T> successfulcomputationresult = this.task.lastResult;
         if (successfulcomputationresult != null) {
            this.output.accept(successfulcomputationresult.value);
            this.lastCheckTime = successfulcomputationresult.time;
         }
      }

      void reset() {
         this.task.reset();
         this.lastCheckTime = -1L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Subscription {
      private final List<DataFetcher.SubscribedTask<?>> subscriptions = new ArrayList<>();

      public <T> void subscribe(DataFetcher.Task<T> p_239442_, Consumer<T> p_239443_) {
         DataFetcher.SubscribedTask<T> subscribedtask = DataFetcher.this.new SubscribedTask<>(p_239442_, p_239443_);
         this.subscriptions.add(subscribedtask);
         subscribedtask.runCallbackIfNeeded();
      }

      public void forceUpdate() {
         for (DataFetcher.SubscribedTask<?> subscribedtask : this.subscriptions) {
            subscribedtask.runCallback();
         }
      }

      public void tick() {
         for (DataFetcher.SubscribedTask<?> subscribedtask : this.subscriptions) {
            subscribedtask.update(DataFetcher.this.timeSource.get(DataFetcher.this.resolution));
         }
      }

      public void reset() {
         for (DataFetcher.SubscribedTask<?> subscribedtask : this.subscriptions) {
            subscribedtask.reset();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SuccessfulComputationResult<T>(T value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   public class Task<T> {
      private final String id;
      private final Callable<T> updater;
      private final long period;
      private final RepeatedDelayStrategy repeatStrategy;
      private @Nullable CompletableFuture<DataFetcher.ComputationResult<T>> pendingTask;
      DataFetcher.@Nullable SuccessfulComputationResult<T> lastResult;
      private long nextUpdate = -1L;

      Task(final String p_239074_, final Callable<T> p_239075_, final long p_239076_, final RepeatedDelayStrategy p_239077_) {
         this.id = p_239074_;
         this.updater = p_239075_;
         this.period = p_239076_;
         this.repeatStrategy = p_239077_;
      }

      void updateIfNeeded(long p_239710_) {
         if (this.pendingTask != null) {
            DataFetcher.ComputationResult<T> computationresult = this.pendingTask.getNow(null);
            if (computationresult == null) {
               return;
            }

            this.pendingTask = null;
            long i = computationresult.time;
            computationresult.value().ifLeft(p_239691_ -> {
               this.lastResult = new DataFetcher.SuccessfulComputationResult<>((T)p_239691_, i);
               this.nextUpdate = i + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
            }).ifRight(p_239281_ -> {
               long j = this.repeatStrategy.delayCyclesAfterFailure();
               DataFetcher.LOGGER.warn("Failed to process task {}, will repeat after {} cycles", new Object[]{this.id, j, p_239281_});
               this.nextUpdate = i + this.period * j;
            });
         }

         if (this.nextUpdate <= p_239710_) {
            this.pendingTask = CompletableFuture.supplyAsync(() -> {
               try {
                  T t = this.updater.call();
                  long k = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.left(t), k);
               } catch (Exception exception) {
                  long j = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.right(exception), j);
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
