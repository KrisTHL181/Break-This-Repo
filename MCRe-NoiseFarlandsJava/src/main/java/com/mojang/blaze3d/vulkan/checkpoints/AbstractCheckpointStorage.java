package com.mojang.blaze3d.vulkan.checkpoints;

import com.mojang.blaze3d.vulkan.VulkanQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkQueue;

@OnlyIn(Dist.CLIENT)
abstract class AbstractCheckpointStorage implements CheckpointExtension.CheckpointStorage {
    protected final VkQueue queue;
    private final int maxFramesInFlight;
    private int frame;
    private final AbstractCheckpointStorage.Frame[] checkpointsByFrame;
    private int nextCheckpointId;

    protected AbstractCheckpointStorage(final VulkanQueue queue, final int maxFramesInFlight) {
        this.queue = queue.vkQueue();
        this.maxFramesInFlight = maxFramesInFlight;
        this.checkpointsByFrame = new AbstractCheckpointStorage.Frame[maxFramesInFlight];

        for (int i = 0; i < maxFramesInFlight; i++) {
            this.checkpointsByFrame[i] = new AbstractCheckpointStorage.Frame(new ArrayList<>());
        }
    }

    @Override
    public void rotate() {
        this.frame = (this.frame + 1) % this.maxFramesInFlight;
        this.checkpointsByFrame[this.frame].checkpoints.clear();
    }

    @Override
    public void recordCheckpoint(final VkCommandBuffer commandBuffer, final CheckpointExtension.CheckpointType type, final Supplier<String> label) {
        int id = this.nextCheckpointId++;
        this.checkpointsByFrame[this.frame].checkpoints.add(new AbstractCheckpointStorage.Checkpoint(id, label.get(), type));
        this.recordCheckpoint(commandBuffer, id);
    }

    protected abstract void recordCheckpoint(VkCommandBuffer commandBuffer, int id);

    protected AbstractCheckpointStorage.@Nullable Checkpoint findCheckpoint(final int id) {
        for (AbstractCheckpointStorage.Frame frame : this.checkpointsByFrame) {
            for (AbstractCheckpointStorage.Checkpoint checkpoint : frame.checkpoints) {
                if (checkpoint.id() == id) {
                    return checkpoint;
                }
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    protected record Checkpoint(int id, String label, CheckpointExtension.CheckpointType type) {
    }

    @OnlyIn(Dist.CLIENT)
    private record Frame(List<AbstractCheckpointStorage.Checkpoint> checkpoints) {
    }
}