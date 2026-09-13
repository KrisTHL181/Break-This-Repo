package net.minecraft.server.level;

import com.google.common.collect.Lists;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

/**
 * ChunkTaskPriorityQueue — 区块任务优先级队列（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（ChunkPos.pack），本版直接用 ChunkPos 对象为键。
 */
public class ChunkTaskPriorityQueue {
    public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
    private final List<LinkedHashMap<ChunkPos, List<Runnable>>> queuesPerPriority = IntStream.range(0, PRIORITY_LEVEL_COUNT)
        .mapToObj(priority -> new LinkedHashMap<ChunkPos, List<Runnable>>())
        .toList();
    private volatile int topPriorityQueueIndex = PRIORITY_LEVEL_COUNT;
    private final String name;

    public ChunkTaskPriorityQueue(final String name) {
        this.name = name;
    }

    protected void resortChunkTasks(final int oldPriority, final ChunkPos pos, final int newPriority) {
        if (oldPriority < PRIORITY_LEVEL_COUNT) {
            LinkedHashMap<ChunkPos, List<Runnable>> oldQueue = this.queuesPerPriority.get(oldPriority);
            List<Runnable> oldTasks = oldQueue.remove(pos);
            if (oldPriority == this.topPriorityQueueIndex) {
                while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
                    this.topPriorityQueueIndex++;
                }
            }

            if (oldTasks != null && !oldTasks.isEmpty()) {
                this.queuesPerPriority.get(newPriority).computeIfAbsent(pos, k -> Lists.newArrayList()).addAll(oldTasks);
                this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, newPriority);
            }
        }
    }

    protected void submit(final Runnable task, final ChunkPos chunkPos, final int level) {
        this.queuesPerPriority.get(level).computeIfAbsent(chunkPos, p -> Lists.newArrayList()).add(task);
        this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, level);
    }

    protected void release(final ChunkPos pos, final boolean unschedule) {
        for (LinkedHashMap<ChunkPos, List<Runnable>> queue : this.queuesPerPriority) {
            List<Runnable> tasks = queue.get(pos);
            if (tasks != null) {
                if (unschedule) {
                    tasks.clear();
                }

                if (tasks.isEmpty()) {
                    queue.remove(pos);
                }
            }
        }

        while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
            this.topPriorityQueueIndex++;
        }
    }

    public ChunkTaskPriorityQueue.@Nullable TasksForChunk pop() {
        if (!this.hasWork()) {
            return null;
        }

        int index = this.topPriorityQueueIndex;
        LinkedHashMap<ChunkPos, List<Runnable>> queue = this.queuesPerPriority.get(index);
        var first = queue.entrySet().iterator().next();
        ChunkPos chunkPos = first.getKey();
        List<Runnable> tasks = first.getValue();
        queue.remove(chunkPos);

        while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
            this.topPriorityQueueIndex++;
        }

        return new ChunkTaskPriorityQueue.TasksForChunk(chunkPos, tasks);
    }

    public boolean hasWork() {
        return this.topPriorityQueueIndex < PRIORITY_LEVEL_COUNT;
    }

    @Override
    public String toString() {
        return this.name + " " + this.topPriorityQueueIndex + "...";
    }

    public record TasksForChunk(ChunkPos chunkPos, List<Runnable> tasks) {
    }
}
