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
      final Map<K, U> input, final BiFunction<K, U, @Nullable V> operation, final int maxTaskCount, final Executor executor
   ) {
      int inputSize = input.size();
      if (inputSize == 0) {
         return CompletableFuture.completedFuture(Map.of());
      } else if (inputSize == 1) {
         Entry<K, U> element = input.entrySet().iterator().next();
         K key = element.getKey();
         U value = element.getValue();
         return CompletableFuture.supplyAsync(() -> {
            V result = operation.apply(key, value);
            return result != null ? Map.of(key, result) : Map.of();
         }, executor);
      } else {
         ParallelMapTransform.SplitterBase<K, U, V> splitter = inputSize <= maxTaskCount
            ? new ParallelMapTransform.SingleTaskSplitter<>(operation, inputSize)
            : new ParallelMapTransform.BatchedTaskSplitter<>(operation, inputSize, maxTaskCount);
         return splitter.scheduleTasks(input, executor);
      }
   }

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(final Map<K, U> input, final BiFunction<K, U, @Nullable V> operation, final Executor executor) {
      int maxTaskCount = Util.maxAllowedExecutorThreads() * 16;
      return schedule(input, operation, maxTaskCount, executor);
   }

   private static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      private final Map<K, V> result;
      private final int batchSize;
      private final int firstUndersizedBatchIndex;

      private BatchedTaskSplitter(final BiFunction<K, U, V> operation, final int size, final int maxTasks) {
         super(operation, size, maxTasks);
         this.result = new HashMap<>(size);
         this.batchSize = Mth.positiveCeilDiv(size, maxTasks);
         int fullCapacity = this.batchSize * maxTasks;
         int leftoverCapacity = fullCapacity - size;
         this.firstUndersizedBatchIndex = maxTasks - leftoverCapacity;
         assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= maxTasks;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(
         final ParallelMapTransform.Container<K, U, V> container, final int startIndex, final int endIndex, final Executor executor
      ) {
         int batchSize = endIndex - startIndex;
         assert batchSize == this.batchSize || batchSize == this.batchSize - 1;
         return CompletableFuture.runAsync(createTask(this.result, startIndex, endIndex, container), executor);
      }

      @Override
      protected int batchSize(final int index) {
         return index < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
      }

      private static <K, U, V> Runnable createTask(
         final Map<K, V> result, final int startIndex, final int endIndex, final ParallelMapTransform.Container<K, U, V> container
      ) {
         return () -> {
            for (int i = startIndex; i < endIndex; i++) {
               container.applyOperation(i);
            }

            synchronized (result) {
               for (int i = startIndex; i < endIndex; i++) {
                  container.copyOut(i, result);
               }
            }
         };
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(
         final CompletableFuture<?> allTasksDone, final ParallelMapTransform.Container<K, U, V> container
      ) {
         Map<K, V> result = this.result;
         return allTasksDone.thenApply(ignored -> result);
      }
   }

   private record Container<K, U, V>(BiFunction<K, U, V> operation, @Nullable Object[] keys, @Nullable Object[] values) {
      public Container(final BiFunction<K, U, V> operation, final int size) {
         this(operation, new Object[size], new Object[size]);
      }

      public void put(final int index, final K key, final U input) {
         this.keys[index] = key;
         this.values[index] = input;
      }

      private @Nullable K key(final int index) {
         return (K)this.keys[index];
      }

      private @Nullable V output(final int index) {
         return (V)this.values[index];
      }

      private @Nullable U input(final int index) {
         return (U)this.values[index];
      }

      public void applyOperation(final int index) {
         this.values[index] = this.operation.apply(this.key(index), this.input(index));
      }

      public void copyOut(final int index, final Map<K, V> output) {
         V value = this.output(index);
         if (value != null) {
            K key = this.key(index);
            output.put(key, value);
         }
      }

      public int size() {
         return this.keys.length;
      }
   }

   private static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      private SingleTaskSplitter(final BiFunction<K, U, V> operation, final int size) {
         super(operation, size, size);
      }

      @Override
      protected int batchSize(final int index) {
         return 1;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(
         final ParallelMapTransform.Container<K, U, V> container, final int startIndex, final int endIndex, final Executor executor
      ) {
         assert startIndex + 1 == endIndex;
         return CompletableFuture.runAsync(() -> container.applyOperation(startIndex), executor);
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(
         final CompletableFuture<?> allTasksDone, final ParallelMapTransform.Container<K, U, V> container
      ) {
         return allTasksDone.thenApply(ignored -> {
            Map<K, V> result = new HashMap<>(container.size());

            for (int i = 0; i < container.size(); i++) {
               container.copyOut(i, result);
            }

            return result;
         });
      }
   }

   private abstract static class SplitterBase<K, U, V> {
      private int lastScheduledIndex;
      private int currentIndex;
      private final CompletableFuture<?>[] tasks;
      private int batchIndex;
      private final ParallelMapTransform.Container<K, U, V> container;

      private SplitterBase(final BiFunction<K, U, V> operation, final int size, final int taskCount) {
         this.container = new ParallelMapTransform.Container<>(operation, size);
         this.tasks = new CompletableFuture[taskCount];
      }

      private int pendingBatchSize() {
         return this.currentIndex - this.lastScheduledIndex;
      }

      public CompletableFuture<Map<K, V>> scheduleTasks(final Map<K, U> input, final Executor executor) {
         input.forEach((key, inputValue) -> {
            this.container.put(this.currentIndex++, (K)key, (U)inputValue);
            if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
               this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, executor);
               this.lastScheduledIndex = this.currentIndex;
            }
         });
         assert this.currentIndex == this.container.size();
         assert this.lastScheduledIndex == this.currentIndex;
         assert this.batchIndex == this.tasks.length;
         return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
      }

      protected abstract int batchSize(int index);

      protected abstract CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> container, int startIndex, int endIndex, Executor executor);

      protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(
         CompletableFuture<?> allTasksDone, ParallelMapTransform.Container<K, U, V> container
      );
   }
}
