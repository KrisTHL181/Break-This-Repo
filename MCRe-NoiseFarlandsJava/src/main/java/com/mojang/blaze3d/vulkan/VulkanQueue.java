package com.mojang.blaze3d.vulkan;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.lwjgl.vulkan.VkSubmitInfo2;
import org.lwjgl.vulkan.VkSubmitInfo2.Buffer;

@OnlyIn(Dist.CLIENT)
public record VulkanQueue(VkQueue vkQueue, int queueFamilyIndex) {
    public VulkanQueue(final VulkanDevice device, final int queueFamilyIndex, final int queueIndex) {
        this(new VkQueue(fetchVkQueue(device, queueFamilyIndex, queueIndex), device.vkDevice()), queueFamilyIndex);
    }

    private static long fetchVkQueue(final VulkanDevice device, final int queueFamilyIndex, final int queueIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer queueHandlePtr = stack.callocPointer(1);
            VK12.vkGetDeviceQueue(device.vkDevice(), queueFamilyIndex, queueIndex, queueHandlePtr);
            return queueHandlePtr.get(0);
        }
    }

    public VulkanQueue.Submission beginSubmit() {
        return new VulkanQueue.Submission();
    }

    public void waitIdle() {
        VK12.vkQueueWaitIdle(this.vkQueue);
    }

    @OnlyIn(Dist.CLIENT)
    public class Submission implements AutoCloseable {
        private boolean closed = false;
        private final ReferenceArrayList<VulkanQueue.Submission.SubmitStage> stages = new ReferenceArrayList<>();

        private Submission() {
            this.stages.add(new VulkanQueue.Submission.SubmitStage());
        }

        public void waitSemaphore(final long semaphore, final long value, final long stageMask) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }

            if (!this.stages.top().commandBuffers.isEmpty() || !this.stages.top().signals.isEmpty()) {
                this.stages.add(new VulkanQueue.Submission.SubmitStage());
            }

            this.stages.top().waits.add(new VulkanQueue.Submission.SemaphoreOp(semaphore, value, stageMask));
        }

        public void executeCommands(final VkCommandBuffer commandBuffer) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }

            if (!this.stages.top().signals.isEmpty()) {
                this.stages.add(new VulkanQueue.Submission.SubmitStage());
            }

            this.stages.top().commandBuffers.add(commandBuffer);
        }

        public void signalSemaphore(final long semaphore, final long value, final long stageMask) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }

            this.stages.top().signals.add(new VulkanQueue.Submission.SemaphoreOp(semaphore, value, stageMask));
        }

        @Override
        public void close() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                Buffer submits = VkSubmitInfo2.calloc(this.stages.size(), stack);

                for (int i = 0; i < this.stages.size(); i++) {
                    VulkanQueue.Submission.SubmitStage stage = this.stages.get(i);
                    submits.position(i).sType$Default();
                    if (!stage.waits.isEmpty()) {
                        org.lwjgl.vulkan.VkSemaphoreSubmitInfo.Buffer waits = VkSemaphoreSubmitInfo.calloc(stage.waits.size(), stack);
                        submits.pWaitSemaphoreInfos(waits);

                        for (int j = 0; j < stage.waits.size(); j++) {
                            VulkanQueue.Submission.SemaphoreOp wait = stage.waits.get(j);
                            waits.position(j).sType$Default();
                            waits.semaphore(wait.vkSemaphore);
                            waits.value(wait.value);
                            waits.stageMask(wait.stageMask);
                        }
                    }

                    if (!stage.commandBuffers.isEmpty()) {
                        org.lwjgl.vulkan.VkCommandBufferSubmitInfo.Buffer buffers = VkCommandBufferSubmitInfo.calloc(stage.commandBuffers.size(), stack);
                        submits.pCommandBufferInfos(buffers);

                        for (int j = 0; j < stage.commandBuffers.size(); j++) {
                            VkCommandBuffer vkCommandBuffer = stage.commandBuffers.get(j);
                            buffers.position(j).sType$Default();
                            buffers.commandBuffer(vkCommandBuffer);
                        }
                    }

                    if (!stage.signals.isEmpty()) {
                        org.lwjgl.vulkan.VkSemaphoreSubmitInfo.Buffer signals = VkSemaphoreSubmitInfo.calloc(stage.signals.size(), stack);
                        submits.pSignalSemaphoreInfos(signals);

                        for (int j = 0; j < stage.signals.size(); j++) {
                            VulkanQueue.Submission.SemaphoreOp signal = stage.signals.get(j);
                            signals.position(j).sType$Default();
                            signals.semaphore(signal.vkSemaphore);
                            signals.value(signal.value);
                            signals.stageMask(signal.stageMask);
                        }
                    }
                }

                submits.position(0);
                KHRSynchronization2.vkQueueSubmit2KHR(VulkanQueue.this.vkQueue, submits, 0L);
                this.closed = true;
            }
        }

        @OnlyIn(Dist.CLIENT)
        private record SemaphoreOp(long vkSemaphore, long value, long stageMask) {
        }

        @OnlyIn(Dist.CLIENT)
        private record SubmitStage(
            List<VulkanQueue.Submission.SemaphoreOp> waits, List<VkCommandBuffer> commandBuffers, List<VulkanQueue.Submission.SemaphoreOp> signals
        ) {
            public SubmitStage() {
                this(new ReferenceArrayList<>(), new ReferenceArrayList<>(), new ReferenceArrayList<>());
            }
        }
    }
}