package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

/**
 * ThrottlingChunkTaskDispatcher — 限流区块任务分发器（MCRe NoiseFarlands 对象化版）
 */
public class ThrottlingChunkTaskDispatcher extends ChunkTaskDispatcher {
    private final Set<ChunkPos> chunkPositionsInExecution = new HashSet<>();
    private final int maxChunksInExecution;
    private final String executorSchedulerName;

    public ThrottlingChunkTaskDispatcher(final TaskScheduler<Runnable> executor, final Executor dispatcherExecutor, final int maxChunksInExecution) {
        super(executor, dispatcherExecutor);
        this.maxChunksInExecution = maxChunksInExecution;
        this.executorSchedulerName = executor.name();
    }

    @Override
    protected void onRelease(final ChunkPos key) {
        this.chunkPositionsInExecution.remove(key);
    }

    @Override
    protected ChunkTaskPriorityQueue.@Nullable TasksForChunk popTasks() {
        return this.chunkPositionsInExecution.size() < this.maxChunksInExecution ? super.popTasks() : null;
    }

    @Override
    protected void scheduleForExecution(final ChunkTaskPriorityQueue.TasksForChunk tasksForChunk) {
        this.chunkPositionsInExecution.add(tasksForChunk.chunkPos());
        super.scheduleForExecution(tasksForChunk);
    }

    @VisibleForTesting
    public String getDebugStatus() {
        return this.executorSchedulerName
            + "=["
            + this.chunkPositionsInExecution.stream().map(String::valueOf).collect(Collectors.joining(","))
            + "], s="
            + this.sleeping;
    }
}
