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
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockableEventLoop<R extends Runnable> implements Executor, TaskScheduler<R>, ProfilerMeasured {
    public static final long BLOCK_TIME_NANOS = 100000L;
    private static volatile @Nullable Supplier<CrashReport> delayedCrash;
    private final boolean propagatesCrashes;
    private final String name;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
    private int blockingCount;

    protected BlockableEventLoop(final String name, final boolean propagatesCrashes) {
        this.propagatesCrashes = propagatesCrashes;
        this.name = name;
        MetricsRegistry.INSTANCE.add(this);
    }

    protected abstract boolean shouldRun(final R task);

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

    public <V> CompletableFuture<V> submit(final Supplier<V> supplier) {
        return this.scheduleExecutables() ? CompletableFuture.supplyAsync(supplier, this) : CompletableFuture.completedFuture(supplier.get());
    }

    private CompletableFuture<Void> submitAsync(final Runnable runnable) {
        return CompletableFuture.supplyAsync(() -> {
            runnable.run();
            return null;
        }, this);
    }

    @CheckReturnValue
    public CompletableFuture<Void> submit(final Runnable runnable) {
        if (this.scheduleExecutables()) {
            return this.submitAsync(runnable);
        }

        runnable.run();
        return CompletableFuture.completedFuture(null);
    }

    public void executeBlocking(final Runnable runnable) {
        if (!this.isSameThread()) {
            this.submitAsync(runnable).join();
        } else {
            runnable.run();
        }
    }

    @Override
    public void schedule(final R r) {
        this.pendingRunnables.add(r);
        LockSupport.unpark(this.getRunningThread());
    }

    @Override
    public void execute(final Runnable command) {
        R task = this.wrapRunnable(command);
        if (this.scheduleExecutables()) {
            this.schedule(task);
        } else {
            this.doRunTask(task);
        }
    }

    public void executeIfPossible(final Runnable command) {
        this.execute(command);
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

    protected boolean pollTask() {
        this.throwDelayedException();
        R task = this.pendingRunnables.peek();
        if (task == null) {
            return false;
        }

        if (!this.shouldRunAllTasks() && !this.shouldRun(task)) {
            return false;
        }

        this.doRunTask(this.pendingRunnables.remove());
        return true;
    }

    public void managedBlock(final BooleanSupplier condition) {
        this.blockingCount++;

        try {
            while (!condition.getAsBoolean()) {
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

    protected void doRunTask(final R task) {
        try (Zone ignored = TracyClient.beginZone("Task", SharedConstants.IS_RUNNING_IN_IDE)) {
            task.run();
        } catch (Exception e) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", this.name(), e);
            if (isNonRecoverable(e)) {
                throw e;
            }
        }
    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of(MetricSampler.create(this.name + "-pending-tasks", MetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
    }

    public static boolean isNonRecoverable(final Throwable t) {
        return t instanceof ReportedException r ? isNonRecoverable(r.getCause()) : t instanceof OutOfMemoryError || t instanceof StackOverflowError;
    }

    private void throwDelayedException() {
        if (this.propagatesCrashes) {
            Supplier<CrashReport> delayedCrash = BlockableEventLoop.delayedCrash;
            if (delayedCrash != null) {
                throw new ReportedException(delayedCrash.get());
            }
        }
    }

    public void delayCrash(final CrashReport crashReport) {
        delayedCrash = () -> crashReport;
    }

    public static synchronized void relayDelayCrash(final CrashReport crashReport) {
        Supplier<CrashReport> delayedCrash = BlockableEventLoop.delayedCrash;
        if (delayedCrash == null) {
            BlockableEventLoop.delayedCrash = () -> crashReport;
        } else {
            delayedCrash.get().getException().addSuppressed(crashReport.getException());
        }
    }
}