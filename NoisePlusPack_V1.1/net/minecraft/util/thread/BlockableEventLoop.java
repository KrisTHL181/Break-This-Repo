package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import org.slf4j.Logger;

public abstract class BlockableEventLoop<R extends Runnable> implements ProfilerMeasured, TaskScheduler<R>, Executor {
   public static final long BLOCK_TIME_NANOS = 100000L;
   private final String name;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
   private int blockingCount;

   protected BlockableEventLoop(String p_18686_) {
      this.name = p_18686_;
      MetricsRegistry.INSTANCE.add(this);
   }

   protected abstract boolean shouldRun(R var1);

   public boolean isSameThread() {
      return Thread.currentThread() == this.getRunningThread();
   }

   protected abstract Thread getRunningThread();

   protected boolean scheduleExecutables() {
      return !this.isSameThread();
   }

   public int getPendingTasksCount() {
      return this.pendingRunnables.size();
   }

   @Override
   public String name() {
      return this.name;
   }

   public <V> CompletableFuture<V> submit(Supplier<V> p_18692_) {
      return this.scheduleExecutables() ? CompletableFuture.supplyAsync(p_18692_, this) : CompletableFuture.completedFuture(p_18692_.get());
   }

   private CompletableFuture<Void> submitAsync(Runnable p_18690_) {
      return CompletableFuture.supplyAsync(() -> {
         p_18690_.run();
         return null;
      }, this);
   }

   @CheckReturnValue
   public CompletableFuture<Void> submit(Runnable p_18708_) {
      if (this.scheduleExecutables()) {
         return this.submitAsync(p_18708_);
      }

      p_18708_.run();
      return CompletableFuture.completedFuture(null);
   }

   public void executeBlocking(Runnable p_18710_) {
      if (!this.isSameThread()) {
         this.submitAsync(p_18710_).join();
      } else {
         p_18710_.run();
      }
   }

   @Override
   public void schedule(R p_18712_) {
      this.pendingRunnables.add(p_18712_);
      LockSupport.unpark(this.getRunningThread());
   }

   @Override
   public void execute(Runnable p_18706_) {
      R r = this.wrapRunnable(p_18706_);
      if (this.scheduleExecutables()) {
         this.schedule(r);
      } else {
         this.doRunTask(r);
      }
   }

   public void executeIfPossible(Runnable p_201937_) {
      this.execute(p_201937_);
   }

   protected void dropAllTasks() {
      this.pendingRunnables.clear();
   }

   protected void runAllTasks() {
      while (this.pollTask()) {
      }
   }

   protected boolean shouldRunAllTasks() {
      return this.blockingCount > 0;
   }

   public boolean pollTask() {
      R r = this.pendingRunnables.peek();
      if (r == null) {
         return false;
      }

      if (!this.shouldRunAllTasks() && !this.shouldRun(r)) {
         return false;
      }

      this.doRunTask(this.pendingRunnables.remove());
      return true;
   }

   public void managedBlock(BooleanSupplier p_18702_) {
      this.blockingCount++;

      try {
         while (!p_18702_.getAsBoolean()) {
            if (!this.pollTask()) {
               this.waitForTasks();
            }
         }
      } finally {
         this.blockingCount--;
      }
   }

   protected void waitForTasks() {
      Thread.yield();
      LockSupport.parkNanos("waiting for tasks", 100000L);
   }

   protected void doRunTask(R p_18700_) {
      try {
         Zone zone = TracyClient.beginZone("Task", SharedConstants.IS_RUNNING_IN_IDE);

         try {
            p_18700_.run();
         } catch (Throwable var6) {
            if (zone != null) {
               try {
                  zone.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (zone != null) {
            zone.close();
         }
      } catch (Exception exception) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", this.name(), exception);
         if (isNonRecoverable(exception)) {
            throw exception;
         }
      }
   }

   @Override
   public List<MetricSampler> profiledMetrics() {
      return ImmutableList.of(MetricSampler.create(this.name + "-pending-tasks", MetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
   }

   public static boolean isNonRecoverable(Throwable p_366916_) {
      return p_366916_ instanceof ReportedException reportedexception
         ? isNonRecoverable(reportedexception.getCause())
         : p_366916_ instanceof OutOfMemoryError || p_366916_ instanceof StackOverflowError;
   }
}
