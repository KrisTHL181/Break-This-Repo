package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

public abstract class VulkanGpuBuffer extends GpuBuffer implements Destroyable {
   private final long vkBuffer;

   public VulkanGpuBuffer(final long vkBuffer, final @GpuBuffer.Usage int usage, final long size) {
      super(usage, size);
      this.vkBuffer = vkBuffer;
   }

   public long vkBuffer() {
      return this.vkBuffer;
   }

   public static class Direct extends VulkanGpuBuffer {
      private boolean closed;
      protected final VulkanDevice device;
      private final long vmaAllocation;
      private int mappingRefCount;

      public Direct(
         final VulkanDevice device,
         final @Nullable Supplier<String> label,
         final @GpuBuffer.Usage int usage,
         final long size,
         final boolean forceHostVisibleAllocation
      ) {
         this.device = device;
         MemoryStack stack = MemoryStack.stackPush();

         long vkBuffer;
         try {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack).sType$Default();
            bufferCreateInfo.size(size);
            bufferCreateInfo.usage(VulkanConst.bufferUsageToVk(usage));
            bufferCreateInfo.sharingMode(0);
            bufferCreateInfo.pQueueFamilyIndices(null);
            VmaAllocationCreateInfo allocCreateInfo = VmaAllocationCreateInfo.calloc(stack);
            allocCreateInfo.usage(8);
            if (forceHostVisibleAllocation) {
               allocCreateInfo.requiredFlags(allocCreateInfo.requiredFlags() | 2);
            }

            if (VulkanUtils.hasAnyBit(usage, 3)) {
               allocCreateInfo.requiredFlags(allocCreateInfo.requiredFlags() | 2 | 4);
               if (VulkanUtils.hasAnyBit(usage, 1)) {
                  allocCreateInfo.preferredFlags(allocCreateInfo.preferredFlags() | 8);
                  allocCreateInfo.flags(allocCreateInfo.flags() | 2048);
               } else {
                  allocCreateInfo.flags(allocCreateInfo.flags() | 1024);
               }
            }

            if (VulkanUtils.hasAnyBit(usage, 5)) {
               allocCreateInfo.usage(9);
            }

            LongBuffer bufferPtr = stack.callocLong(1);
            PointerBuffer allocPtr = stack.callocPointer(1);
            int result = Vma.vmaCreateBuffer(device.vma(), bufferCreateInfo, allocCreateInfo, bufferPtr, allocPtr, null);
            VulkanUtils.crashIfFailure(device, result, "Failed to allocate VkBuffer");
            vkBuffer = bufferPtr.get(0);
            this.vmaAllocation = allocPtr.get(0);
            if (label != null) {
               device.instance().debug().setObjectName(device.vkDevice(), 9, vkBuffer, label);
            }
         } catch (Throwable var16) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var15) {
                  var16.addSuppressed(var15);
               }
            }

            throw var16;
         }

         if (stack != null) {
            stack.close();
         }

         super(vkBuffer, usage, size);
         this.closed = false;
         this.mappingRefCount = 0;
      }

      @Override
      public void destroy() {
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

         if (offset >= 0L && length >= 0L) {
            this.mappingRefCount++;
            MemoryStack stack = MemoryStack.stackPush();

            GpuBufferSlice.MappedView var10;
            try {
               PointerBuffer pointer = stack.callocPointer(1);
               VulkanUtils.crashIfFailure(this.device, Vma.vmaMapMemory(this.device.vma(), this.vmaAllocation, pointer), "Failed to map buffer");
               ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(pointer.get(0) + offset, (int)length);
               var10 = new GpuBufferSlice.MappedView(this.slice(offset, length), byteBuffer, new Runnable() {
                  private boolean closed = false;

                  @Override
                  public void run() {
                     if (!this.closed) {
                        this.closed = true;
                        Direct.this.mappingRefCount--;
                        Vma.vmaUnmapMemory(Direct.this.device.vma(), Direct.this.vmaAllocation);
                     }
                  }
               });
            } catch (Throwable var12) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (stack != null) {
               stack.close();
            }

            return var10;
         } else {
            throw new IllegalArgumentException("Offset or length must be positive integer values");
         }
      }
   }
}
