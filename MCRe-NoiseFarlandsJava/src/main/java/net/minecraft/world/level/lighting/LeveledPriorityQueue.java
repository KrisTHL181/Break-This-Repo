package net.minecraft.world.level.lighting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * LeveledPriorityQueue — 分层优先级队列（MCRe NoiseFarlands 泛型对象化版）
 * 原版以 long 节点存储（LongLinkedOpenHashSet），本版泛型化为任意对象节点（LinkedHashSet<N>）。
 */
public class LeveledPriorityQueue<N> {
    private final int levelCount;
    private final List<LinkedHashSet<N>> queues;
    private int firstQueuedLevel;

    public LeveledPriorityQueue(final int levelCount, final int minSize) {
        this.levelCount = levelCount;
        this.queues = new ArrayList<>(levelCount);

        for (int i = 0; i < levelCount; i++) {
            this.queues.add(new LinkedHashSet<>());
        }

        this.firstQueuedLevel = levelCount;
    }

    public N removeFirst() {
        LinkedHashSet<N> queue = this.queues.get(this.firstQueuedLevel);
        var it = queue.iterator();
        N result = it.next();
        it.remove();
        if (queue.isEmpty()) {
            this.checkFirstQueuedLevel(this.levelCount);
        }

        return result;
    }

    public boolean isEmpty() {
        return this.firstQueuedLevel >= this.levelCount;
    }

    public void dequeue(final N node, final int key, final int upperBound) {
        LinkedHashSet<N> queue = this.queues.get(key);
        queue.remove(node);
        if (queue.isEmpty() && this.firstQueuedLevel == key) {
            this.checkFirstQueuedLevel(upperBound);
        }
    }

    public void enqueue(final N node, final int key) {
        this.queues.get(key).add(node);
        if (this.firstQueuedLevel > key) {
            this.firstQueuedLevel = key;
        }
    }

    private void checkFirstQueuedLevel(final int upperBound) {
        int oldLevel = this.firstQueuedLevel;
        this.firstQueuedLevel = upperBound;

        for (int i = oldLevel + 1; i < upperBound; i++) {
            if (!this.queues.get(i).isEmpty()) {
                this.firstQueuedLevel = i;
                break;
            }
        }
    }
}
