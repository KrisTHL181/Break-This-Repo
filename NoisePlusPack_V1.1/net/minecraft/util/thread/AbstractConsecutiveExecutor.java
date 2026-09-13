package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import org.slf4j.Logger;

public abstract class AbstractConsecutiveExecutor<T extends Runnable> implements ProfilerMeasured, TaskScheduler<T>, Runnable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicReference<AbstractConsecutiveExecutor.Status> status = new AtomicReference<>(AbstractConsecutiveExecutor.Status.SLEEPING);
   private final StrictQueue<T> queue;
   private final Executor executor;
   private final String name;

   public AbstractConsecutiveExecutor(StrictQueue<T> p_363900_, Executor p_367154_, String p_369151_) {
      this.executor = p_367154_;
      this.queue = p_363900_;
      this.name = p_369151_;
      MetricsRegistry.INSTANCE.add(this);
   }

   private boolean canBeScheduled() {
      return !this.isClosed() && !this.queue.isEmpty();
   }

   @Override
   public void close() {
      this.status.set(AbstractConsecutiveExecutor.Status.CLOSED);
   }

   private boolean pollTask() {
      if (!this.isRunning()) {
         return false;
      }

      Runnable runnable = this.queue.pop();
      if (runnable == null) {
         return false;
      }

      Util.runNamed(runnable, this.name);
      return true;
   }

   @Override
   public void run() {
      try {
         this.pollTask();
      } finally {
         this.setSleeping();
         this.registerForExecution();
      }
   }

   public void runAll() {
      try {
         while (this.pollTask()) {
         }
      } finally {
         this.setSleeping();
         this.registerForExecution();
      }
   }

   @Override
   public void schedule(T p_364361_) {
      this.queue.push(p_364361_);
      this.registerForExecution();
   }

   private void registerForExecution() {
      if (this.canBeScheduled() && this.setRunning()) {
         try {
            this.executor.execute(this);
         } catch (RejectedExecutionException rejectedexecutionexception1) {
            try {
               this.executor.execute(this);
            } catch (RejectedExecutionException rejectedexecutionexception) {
               LOGGER.error("Could not schedule ConsecutiveExecutor", rejectedexecutionexception);
            }
         }
      }
   }

   public int size() {
      return this.queue.size();
   }

   public boolean hasWork() {
      return this.isRunning() && !this.queue.isEmpty();
   }

   @Override
   public String toString() {
      return this.name + " " + this.status.get() + " " + this.queue.isEmpty();
   }

   @Override
   public String name() {
      return this.name;
   }

   @Override
   public List<MetricSampler> profiledMetrics() {
      return ImmutableList.of(MetricSampler.create(this.name + "-queue-size", MetricCategory.CONSECUTIVE_EXECUTORS, this::size));
   }

   private boolean setRunning() {
      return this.status.compareAndSet(AbstractConsecutiveExecutor.Status.SLEEPING, AbstractConsecutiveExecutor.Status.RUNNING);
   }

   private void setSleeping() {
      this.status.compareAndSet(AbstractConsecutiveExecutor.Status.RUNNING, AbstractConsecutiveExecutor.Status.SLEEPING);
   }

   private boolean isRunning() {
      return this.status.get() == AbstractConsecutiveExecutor.Status.RUNNING;
   }

   private boolean isClosed() {
      return this.status.get() == AbstractConsecutiveExecutor.Status.CLOSED;
   }

   enum Status {
      SLEEPING,
      RUNNING,
      CLOSED;
   }
}
