package net.minecraft.world.level.lighting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.Mth;

/**
 * DynamicGraphMinFixedPoint — 动态图最短固定点算法（MCRe NoiseFarlands 泛型对象化版）
 * 原版以 long 节点（打包坐标），本版泛型化为任意对象节点（ChunkPos/SectionPos）。
 * 哨兵节点统一用 null（子类可覆盖 isSource 改用具名常量）。
 */
public abstract class DynamicGraphMinFixedPoint<N> {
    private static final int NO_COMPUTED_LEVEL = 255;
    protected final int levelCount;
    private final LeveledPriorityQueue<N> priorityQueue;
    private final HashMap<N, Byte> computedLevels;
    private volatile boolean hasWork;

    protected DynamicGraphMinFixedPoint(final int levelCount, final int minQueueSize, final int minMapSize) {
        if (levelCount >= 254) {
            throw new IllegalArgumentException("Level count must be < 254.");
        }

        this.levelCount = levelCount;
        this.priorityQueue = new LeveledPriorityQueue<>(levelCount, minQueueSize);
        this.computedLevels = new HashMap<>(Math.max(minMapSize, 16), 0.5F);
    }

    protected void removeFromQueue(final N node) {
        Byte removed = this.computedLevels.remove(node);
        int computedLevel = removed == null ? 255 : removed & 255;
        if (computedLevel != 255) {
            int level = this.getLevel(node);
            int priority = this.calculatePriority(level, computedLevel);
            this.priorityQueue.dequeue(node, priority, this.levelCount);
            this.hasWork = !this.priorityQueue.isEmpty();
        }
    }

    public void removeIf(final Predicate<N> pred) {
        List<N> nodesToRemove = new ArrayList<>();
        this.computedLevels.keySet().forEach(node -> {
            if (pred.test(node)) {
                nodesToRemove.add(node);
            }
        });
        nodesToRemove.forEach((Consumer<N>)this::removeFromQueue);
    }

    private int calculatePriority(final int level, final int computedLevel) {
        return Math.min(Math.min(level, computedLevel), this.levelCount - 1);
    }

    protected void checkNode(final N node) {
        this.checkEdge(node, node, this.levelCount - 1, false);
    }

    protected void checkEdge(final N from, final N to, final int newLevelFrom, final boolean onlyDecreased) {
        this.checkEdge(from, to, newLevelFrom, this.getLevel(to), this.computedLevels.getOrDefault(to, (byte)-1) & 255, onlyDecreased);
        this.hasWork = !this.priorityQueue.isEmpty();
    }

    private void checkEdge(final N from, final N to, int newLevelFrom, int levelTo, int oldComputedLevel, final boolean onlyDecreased) {
        if (!this.isSource(to)) {
            newLevelFrom = Mth.clamp(newLevelFrom, 0, this.levelCount - 1);
            levelTo = Mth.clamp(levelTo, 0, this.levelCount - 1);
            boolean wasConsistent = oldComputedLevel == 255;
            if (wasConsistent) {
                oldComputedLevel = levelTo;
            }

            int newComputedLevel;
            if (onlyDecreased) {
                newComputedLevel = Math.min(oldComputedLevel, newLevelFrom);
            } else {
                newComputedLevel = Mth.clamp(this.getComputedLevel(to, from, newLevelFrom), 0, this.levelCount - 1);
            }

            int oldPriority = this.calculatePriority(levelTo, oldComputedLevel);
            if (levelTo != newComputedLevel) {
                int newPriority = this.calculatePriority(levelTo, newComputedLevel);
                if (oldPriority != newPriority && !wasConsistent) {
                    this.priorityQueue.dequeue(to, oldPriority, newPriority);
                }

                this.priorityQueue.enqueue(to, newPriority);
                this.computedLevels.put(to, (byte)newComputedLevel);
            } else if (!wasConsistent) {
                this.priorityQueue.dequeue(to, oldPriority, this.levelCount);
                this.computedLevels.remove(to);
            }
        }
    }

    protected final void checkNeighbor(final N from, final N to, final int level, final boolean onlyDecreased) {
        int storedOldComputedLevel = this.computedLevels.getOrDefault(to, (byte)-1) & 255;
        int levelFrom = Mth.clamp(this.computeLevelFromNeighbor(from, to, level), 0, this.levelCount - 1);
        if (onlyDecreased) {
            this.checkEdge(from, to, levelFrom, this.getLevel(to), storedOldComputedLevel, onlyDecreased);
        } else {
            boolean wasConsistent = storedOldComputedLevel == 255;
            int oldComputedLevel;
            if (wasConsistent) {
                oldComputedLevel = Mth.clamp(this.getLevel(to), 0, this.levelCount - 1);
            } else {
                oldComputedLevel = storedOldComputedLevel;
            }

            if (levelFrom == oldComputedLevel) {
                this.checkEdge(from, to, this.levelCount - 1, wasConsistent ? oldComputedLevel : this.getLevel(to), storedOldComputedLevel, onlyDecreased);
            }
        }
    }

    protected final boolean hasWork() {
        return this.hasWork;
    }

    protected final int runUpdates(int count) {
        if (this.priorityQueue.isEmpty()) {
            return count;
        }

        while (!this.priorityQueue.isEmpty() && count > 0) {
            count--;
            N node = this.priorityQueue.removeFirst();
            int level = Mth.clamp(this.getLevel(node), 0, this.levelCount - 1);
            int computedLevel = this.computedLevels.remove(node) & 255;
            if (computedLevel < level) {
                this.setLevel(node, computedLevel);
                this.checkNeighborsAfterUpdate(node, computedLevel, true);
            } else if (computedLevel > level) {
                this.setLevel(node, this.levelCount - 1);
                if (computedLevel != this.levelCount - 1) {
                    this.priorityQueue.enqueue(node, this.calculatePriority(this.levelCount - 1, computedLevel));
                    this.computedLevels.put(node, (byte)computedLevel);
                }

                this.checkNeighborsAfterUpdate(node, level, false);
            }
        }

        this.hasWork = !this.priorityQueue.isEmpty();
        return count;
    }

    public int getQueueSize() {
        return this.computedLevels.size();
    }

    /** 哨兵节点判断（默认 null；子类可覆盖用具名常量） */
    protected boolean isSource(final N node) {
        return node == null;
    }

    protected abstract int getComputedLevel(final N node, final N knownParent, final int knownLevelFromParent);

    protected abstract void checkNeighborsAfterUpdate(final N node, final int level, final boolean onlyDecrease);

    protected abstract int getLevel(N node);

    protected abstract void setLevel(N node, int level);

    protected abstract int computeLevelFromNeighbor(N from, N to, final int fromLevel);
}
