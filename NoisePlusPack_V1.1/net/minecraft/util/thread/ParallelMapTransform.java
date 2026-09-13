package net.minecraft.util.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class ParallelMapTransform {
   private static final int DEFAULT_TASKS_PER_THREAD = 16;

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(
      Map<K, U> p_391667_, BiFunction<K, U, @Nullable V> p_396121_, int p_392374_, Executor p_392136_
   ) {
      int i = p_391667_.size();
      if (i == 0) {
         return CompletableFuture.completedFuture(Map.of());
      } else if (i == 1) {
         Entry<K, U> entry = p_391667_.entrySet().iterator().next();
         K k = entry.getKey();
         U u = entry.getValue();
         return CompletableFuture.supplyAsync(() -> {
            V v = p_396121_.apply(k, u);
            return v != null ? Map.of(k, v) : Map.of();
         }, p_392136_);
      } else {
         ParallelMapTransform.SplitterBase<K, U, V> splitterbase = i <= p_392374_
            ? new ParallelMapTransform.SingleTaskSplitter<>(p_396121_, i)
            : new ParallelMapTransform.BatchedTaskSplitter<>(p_396121_, i, p_392374_);
         return splitterbase.scheduleTasks(p_391667_, p_392136_);
      }
   }

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> p_392177_, BiFunction<K, U, @Nullable V> p_395566_, Executor p_391925_) {
      int i = Util.maxAllowedExecutorThreads() * 16;
      return schedule(p_392177_, p_395566_, i, p_391925_);
   }

   static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      private final Map<K, V> result;
      private final int batchSize;
      private final int firstUndersizedBatchIndex;

      BatchedTaskSplitter(BiFunction<K, U, V> p_397810_, int p_391927_, int p_394050_) {
         super(p_397810_, p_391927_, p_394050_);
         this.result = new HashMap<>(p_391927_);
         this.batchSize = Mth.positiveCeilDiv(p_391927_, p_394050_);
         int i = this.batchSize * p_394050_;
         int j = i - p_391927_;
         this.firstUndersizedBatchIndex = p_394050_ - j;
         assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= p_394050_;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_395039_, int p_391190_, int p_393559_, Executor p_393890_) {
         int i = p_393559_ - p_391190_;
         assert i == this.batchSize || i == this.batchSize - 1;
         return CompletableFuture.runAsync(createTask(this.result, p_391190_, p_393559_, p_395039_), p_393890_);
      }

      @Override
      protected int batchSize(int p_396019_) {
         return p_396019_ < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
      }

      private static <K, U, V> Runnable createTask(Map<K, V> p_393441_, int p_398008_, int p_397004_, ParallelMapTransform.Container<K, U, V> p_397180_) {
         return () -> {
            for (int i = p_398008_; i < p_397004_; i++) {
               p_397180_.applyOperation(i);
            }

            synchronized (p_393441_) {
               for (int j = p_398008_; j < p_397004_; j++) {
                  p_397180_.copyOut(j, p_393441_);
               }
            }
         };
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_396406_, ParallelMapTransform.Container<K, U, V> p_397157_) {
         Map<K, V> map = this.result;
         return p_396406_.thenApply(p_391758_ -> map);
      }
   }

   record Container<K, U, V>(BiFunction<K, U, V> operation, @Nullable Object[] keys, @Nullable Object[] values) {
      public Container(BiFunction<K, U, V> p_396981_, int p_394307_) {
         this(p_396981_, new Object[p_394307_], new Object[p_394307_]);
      }

      public void put(int p_392891_, K p_397977_, U p_393337_) {
         this.keys[p_392891_] = p_397977_;
         this.values[p_392891_] = p_393337_;
      }

      private @Nullable K key(int p_394735_) {
         return (K)this.keys[p_394735_];
      }

      private @Nullable V output(int p_392623_) {
         return (V)this.values[p_392623_];
      }

      private @Nullable U input(int p_397071_) {
         return (U)this.values[p_397071_];
      }

      public void applyOperation(int p_396060_) {
         this.values[p_396060_] = this.operation.apply(this.key(p_396060_), this.input(p_396060_));
      }

      public void copyOut(int p_392298_, Map<K, V> p_392589_) {
         V v = this.output(p_392298_);
         if (v != null) {
            K k = this.key(p_392298_);
            p_392589_.put(k, v);
         }
      }

      public int size() {
         return this.keys.length;
      }
   }

   static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      SingleTaskSplitter(BiFunction<K, U, V> p_395290_, int p_392198_) {
         super(p_395290_, p_392198_, p_392198_);
      }

      @Override
      protected int batchSize(int p_397895_) {
         return 1;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_394262_, int p_393896_, int p_396012_, Executor p_392458_) {
         assert p_393896_ + 1 == p_396012_;
         return CompletableFuture.runAsync(() -> p_394262_.applyOperation(p_393896_), p_392458_);
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_391498_, ParallelMapTransform.Container<K, U, V> p_397732_) {
         return p_391498_.thenApply(p_391357_ -> {
            Map<K, V> map = new HashMap<>(p_397732_.size());

            for (int i = 0; i < p_397732_.size(); i++) {
               p_397732_.copyOut(i, map);
            }

            return map;
         });
      }
   }

   abstract static class SplitterBase<K, U, V> {
      private int lastScheduledIndex;
      private int currentIndex;
      private final CompletableFuture<?>[] tasks;
      private int batchIndex;
      private final ParallelMapTransform.Container<K, U, V> container;

      SplitterBase(BiFunction<K, U, V> p_396626_, int p_397347_, int p_391371_) {
         this.container = new ParallelMapTransform.Container<>(p_396626_, p_397347_);
         this.tasks = new CompletableFuture[p_391371_];
      }

      private int pendingBatchSize() {
         return this.currentIndex - this.lastScheduledIndex;
      }

      public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> p_395063_, Executor p_395226_) {
         p_395063_.forEach((p_392446_, p_396440_) -> {
            this.container.put(this.currentIndex++, (K)p_392446_, (U)p_396440_);
            if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
               this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, p_395226_);
               this.lastScheduledIndex = this.currentIndex;
            }
         });
         assert this.currentIndex == this.container.size();
         assert this.lastScheduledIndex == this.currentIndex;
         assert this.batchIndex == this.tasks.length;
         return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
      }

      protected abstract int batchSize(int var1);

      protected abstract CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> var1, int var2, int var3, Executor var4);

      protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> var1, ParallelMapTransform.Container<K, U, V> var2);
   }
}
