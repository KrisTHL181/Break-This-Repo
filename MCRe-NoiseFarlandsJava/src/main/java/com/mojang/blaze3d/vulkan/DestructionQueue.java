package com.mojang.blaze3d.vulkan;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DestructionQueue<T> implements AutoCloseable {
    private final DestructionQueue.Destroyer<T> destroyCallback;
    private final ReferenceList<ReferenceArrayList<T>> destructionQueues;
    private int currentDestructionQueueIndex = 0;

    public DestructionQueue(final int internalQueueCount, final DestructionQueue.Destroyer<T> destroyCallback) {
        this.destroyCallback = destroyCallback;
        this.destructionQueues = new ReferenceArrayList<>(internalQueueCount);

        for (int i = 0; i < internalQueueCount; i++) {
            this.destructionQueues.add(new ReferenceArrayList<>());
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < this.destructionQueues.size(); i++) {
            if (this.rotate()) {
                i = 0;
            }
        }
    }

    public boolean rotate() {
        this.currentDestructionQueueIndex++;
        this.currentDestructionQueueIndex = this.currentDestructionQueueIndex % this.destructionQueues.size();
        ReferenceArrayList<T> currentQueue = this.destructionQueues.set(this.currentDestructionQueueIndex, new ReferenceArrayList<>());
        if (currentQueue.isEmpty()) {
            return false;
        }

        this.destroyCallback.begin(currentQueue.size());
        currentQueue.forEach(this.destroyCallback::destroy);
        this.destroyCallback.end();
        return true;
    }

    public void add(final T t) {
        ReferenceArrayList<T> currentQueue = this.destructionQueues.get(this.currentDestructionQueueIndex);
        currentQueue.add(t);
    }

    @OnlyIn(Dist.CLIENT)
    public interface Destroyer<T> {
        default void begin(final int count) {
        }

        void destroy(T t);

        default void end() {
        }
    }
}