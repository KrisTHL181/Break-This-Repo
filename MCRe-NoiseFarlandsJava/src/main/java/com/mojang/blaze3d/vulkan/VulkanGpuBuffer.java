package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

@OnlyIn(Dist.CLIENT)
public abstract class VulkanGpuBuffer extends GpuBuffer implements Destroyable {
    private final long vkBuffer;

    public VulkanGpuBuffer(final long vkBuffer, final @GpuBuffer.Usage int usage, final long size) {
        super(usage, size);
        this.vkBuffer = vkBuffer;
    }

    public long vkBuffer() {
        return this.vkBuffer;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Direct extends VulkanGpuBuffer {
        private boolean closed;
        protected final VulkanDevice device;
        private final long vmaAllocation;
        private int mappingRefCount;
        private final boolean isMappedPersistent;
        private final long mappedPointer; // 持久映射的指针，非持久则为 0

        public Direct(
            final VulkanDevice device,
            final @Nullable Supplier<String> label,
            final @GpuBuffer.Usage int usage,
            final long size,
            final boolean forceHostVisibleAllocation
        ) {
            this.device = device;

            int vmaUsage;
            int vmaFlags = 0;
            boolean persistentMapped = false;
            boolean needsHostAccess = (usage & (GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_MAP_WRITE)) != 0;
            
            if (needsHostAccess || forceHostVisibleAllocation) {
                vmaUsage = Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
                vmaFlags |= Vma.VMA_ALLOCATION_CREATE_MAPPED_BIT;
                persistentMapped = true;
            } else {
                vmaUsage = Vma.VMA_MEMORY_USAGE_GPU_ONLY;
                persistentMapped = false;
            }

            long vkBuffer;
            long vmaAlloc;
            long mappedPtr = 0L;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack).sType$Default();
                bufferCreateInfo.size(size);
                bufferCreateInfo.usage(VulkanConst.bufferUsageToVk(usage));
                bufferCreateInfo.sharingMode(0);
                bufferCreateInfo.pQueueFamilyIndices(null);
                
                VmaAllocationCreateInfo allocCreateInfo = VmaAllocationCreateInfo.calloc(stack);
                allocCreateInfo.usage(vmaUsage);
                allocCreateInfo.flags(vmaFlags);

                LongBuffer bufferPtr = stack.callocLong(1);
                PointerBuffer allocPtr = stack.callocPointer(1);
                int result = Vma.vmaCreateBuffer(device.vma(), bufferCreateInfo, allocCreateInfo, bufferPtr, allocPtr, null);
                VulkanUtils.crashIfFailure(device, result, "Failed to allocate VkBuffer");
                vkBuffer = bufferPtr.get(0);
                vmaAlloc = allocPtr.get(0);
                if (label != null) {
                    device.instance().debug().setObjectName(device.vkDevice(), 9, vkBuffer, label);
                }

                // 如果持久映射，立即映射获取指针
                if (persistentMapped) {
                    PointerBuffer mappedPtrBuf = stack.callocPointer(1);
                    result = Vma.vmaMapMemory(device.vma(), vmaAlloc, mappedPtrBuf);
                    VulkanUtils.crashIfFailure(device, result, "Failed to map persistent buffer");
                    mappedPtr = mappedPtrBuf.get(0);
                }
            }

            super(vkBuffer, usage, size);
            this.closed = false;
            this.vmaAllocation = vmaAlloc;
            this.mappingRefCount = 0;
            this.isMappedPersistent = persistentMapped;
            this.mappedPointer = mappedPtr;
        }

        @Override
        public void destroy() {
            // 如果是持久映射，需要 unmap
            if (this.isMappedPersistent && this.mappedPointer != 0L) {
                Vma.vmaUnmapMemory(this.device.vma(), this.vmaAllocation);
            }
            Vma.vmaDestroyBuffer(this.device.vma(), this.vkBuffer(), this.vmaAllocation);
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                if (this.mappingRefCount != 0) {
                    throw new IllegalStateException("Attempt to close a mapped buffer");
                }
                this.device.createCommandEncoder().queueForDestroy(this);
            }
        }

        @Override
        public GpuBufferSlice.MappedView map(final long offset, final long length, final boolean read, final boolean write) {
            if (this.isClosed()) {
                throw new IllegalStateException("Buffer already closed");
            }

            if (!read && !write) {
                throw new IllegalArgumentException("At least read or write must be true");
            }

            if (read && (this.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            }

            if (write && (this.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            }

            if (offset + length > this.size()) {
                throw new IllegalArgumentException(
                    "Cannot map more data than this buffer can hold (attempting to map "
                        + length
                        + " bytes at offset "
                        + offset
                        + " from "
                        + this.size()
                        + " size buffer)"
                );
            }

            if (length > 2147483647L) {
                throw new IllegalArgumentException("Mapping buffer slice larger than 2GB is not supported");
            }

            if (offset < 0L || length < 0L) {
                throw new IllegalArgumentException("Offset or length must be positive integer values");
            }

            this.mappingRefCount++;

            if (this.isMappedPersistent) {
                // 持久映射：直接使用已映射的指针切片
                ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(this.mappedPointer + offset, (int)length);
                return new GpuBufferSlice.MappedView(
                    this.slice(offset, length),
                    byteBuffer,
                    () -> {
                        // 关闭时仅减少引用计数，不真正 unmap
                        this.mappingRefCount--;
                    }
                );
            } else {
                // 非持久映射：临时映射
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer pointer = stack.callocPointer(1);
                    int result = Vma.vmaMapMemory(this.device.vma(), this.vmaAllocation, pointer);
                    VulkanUtils.crashIfFailure(this.device, result, "Failed to map buffer");
                    long ptr = pointer.get(0) + offset;
                    ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(ptr, (int)length);
                    return new GpuBufferSlice.MappedView(
                        this.slice(offset, length),
                        byteBuffer,
                        () -> {
                            this.mappingRefCount--;
                            if (this.mappingRefCount == 0) {
                                Vma.vmaUnmapMemory(this.device.vma(), this.vmaAllocation);
                            }
                        }
                    );
                }
            }
        }
    }
}