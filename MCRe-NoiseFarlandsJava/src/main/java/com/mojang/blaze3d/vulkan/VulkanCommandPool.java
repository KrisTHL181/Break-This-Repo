package com.mojang.blaze3d.vulkan;

import java.nio.LongBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

@OnlyIn(Dist.CLIENT)
public class VulkanCommandPool implements Destroyable {
    private static final int BUFFER_ALLOC_COUNT = 32;
    private static final int HANDLE_BUFFER_BLOCK_SIZE = 512;
    private final VulkanDevice device;
    private final long commandPool;
    private PointerBuffer allocatedBuffers;

    public VulkanCommandPool(final VulkanDevice device, final VulkanQueue queue) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default();
            commandPoolCreateInfo.flags(1);
            commandPoolCreateInfo.queueFamilyIndex(queue.queueFamilyIndex());
            LongBuffer commandPoolHandlePtr = stack.callocLong(1);
            VulkanUtils.crashIfFailure(
                device, VK12.vkCreateCommandPool(device.vkDevice(), commandPoolCreateInfo, null, commandPoolHandlePtr), "Failed to create VkCommandPool"
            );
            this.commandPool = commandPoolHandlePtr.get(0);
        }

        this.allocatedBuffers = MemoryUtil.memAllocPointer(512);
        this.allocatedBuffers.limit(0);
    }

    @Override
    public void destroy() {
        this.release();
        this.allocatedBuffers.free();
        VK12.vkDestroyCommandPool(this.device.vkDevice(), this.commandPool, null);
    }

    public void release() {
        this.allocatedBuffers.rewind();
        if (this.allocatedBuffers.hasRemaining()) {
            VK12.vkFreeCommandBuffers(this.device.vkDevice(), this.commandPool, this.allocatedBuffers);
            this.allocatedBuffers.clear();
            MemoryUtil.memSet(this.allocatedBuffers, 0);
        }

        VK12.vkResetCommandPool(this.device.vkDevice(), this.commandPool, 1);
        this.allocatedBuffers.limit(0);
    }

    public void reset() {
        VK12.vkResetCommandPool(this.device.vkDevice(), this.commandPool, 0);
        this.allocatedBuffers.rewind();
    }

    private void allocateMoreBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (this.allocatedBuffers.capacity() - this.allocatedBuffers.limit() < 32) {
                PointerBuffer newBuffer = MemoryUtil.memRealloc(this.allocatedBuffers, this.allocatedBuffers.capacity() + 512);
                newBuffer.limit(this.allocatedBuffers.limit());
                this.allocatedBuffers = newBuffer;
            }

            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default();
            allocateInfo.commandPool(this.commandPool);
            allocateInfo.level(0);
            allocateInfo.commandBufferCount(32);
            this.allocatedBuffers.limit(this.allocatedBuffers.limit() + 32);
            PointerBuffer buffers = this.allocatedBuffers.slice(0, 32);
            VulkanUtils.crashIfFailure(
                this.device, VK12.vkAllocateCommandBuffers(this.device.vkDevice(), allocateInfo, buffers), "Failed to allocate VkCommandBuffers"
            );
        }
    }

    public VkCommandBuffer allocateBuffer() {
        if (!this.allocatedBuffers.hasRemaining()) {
            this.allocateMoreBuffers();
        }

        return new VkCommandBuffer(this.allocatedBuffers.get(), this.device.vkDevice());
    }
}