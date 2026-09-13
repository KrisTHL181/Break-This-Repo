package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.backend.util.TransientBlockAllocator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceReferenceImmutablePair;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkBufferCopy.Buffer;

public class VulkanTransientMemory implements TransientMemory, Destroyable {
   private static final long BLOCK_SIZE = 524288L;
   private static final long MAX_CPU_ALIGNMENT = 16L;
   private static final long MAX_GPU_ALIGNMENT = Long.highestOneBit(Long.MAX_VALUE);
   private static final int BUFFER_USAGE_BITS = 471;
   private final VulkanDevice device;
   private final VulkanCommandEncoder encoder;
   private final boolean useDeviceMemoryForMappedGpuStaging;
   private final TransientBlockAllocator<Long> cpuBlockAllocator = new TransientBlockAllocator<>(
      524288L, 16L, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree)
   );
   private final TransientBlockAllocator<VulkanTransientMemory.VulkanAllocation> stagingBlockAllocator;
   private final TransientBlockAllocator<VulkanTransientMemory.VulkanAllocation> gpuBlockAllocator;
   private final TransientBlockAllocator<Pair<VulkanTransientMemory.VulkanAllocation, VulkanTransientMemory.VulkanAllocation>> gpuMappedBlockAllocator;
   private long submitIndex = 0L;
   private boolean anyCommandRecorded = false;
   private @Nullable VkCommandBuffer commandBuffer;

   VulkanTransientMemory(final VulkanDevice device, final VulkanCommandEncoder encoder) {
      this.device = device;
      this.encoder = encoder;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkDevice vkDevice = device.vkDevice();
         VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
         VK12.vkGetPhysicalDeviceMemoryProperties(vkDevice.getPhysicalDevice(), memoryProperties);
         int heapCount = memoryProperties.memoryHeapCount();
         int typeCount = memoryProperties.memoryTypeCount();
         int largestDeviceLocalHeapIndex = -1;
         long largestDeviceLocalHeapSize = -1L;

         for (int i = 0; i < heapCount; i++) {
            VkMemoryHeap heapProperties = memoryProperties.memoryHeaps(i);
            if (VulkanUtils.hasAnyBit(heapProperties.flags(), 1) && heapProperties.size() >= largestDeviceLocalHeapSize) {
               largestDeviceLocalHeapIndex = i;
               largestDeviceLocalHeapSize = heapProperties.size();
            }
         }

         assert largestDeviceLocalHeapIndex != -1;
         boolean largestHeapIsHostVisibleAndCoherent = false;

         for (int i = 0; i < typeCount; i++) {
            VkMemoryType typeProperties = memoryProperties.memoryTypes(i);
            if (typeProperties.heapIndex() == largestDeviceLocalHeapIndex && VulkanUtils.hasAllBits(typeProperties.propertyFlags(), 6)) {
               largestHeapIsHostVisibleAndCoherent = true;
               break;
            }
         }

         this.useDeviceMemoryForMappedGpuStaging = largestHeapIsHostVisibleAndCoherent;
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      this.stagingBlockAllocator = new TransientBlockAllocator<>(
         524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateVulkanBlock(size, true), this::freeVulkanBlock)
      );
      this.gpuBlockAllocator = new TransientBlockAllocator<>(
         524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateVulkanBlock(size, false), this::queueFreeVulkanBlock)
      );
      this.gpuMappedBlockAllocator = new TransientBlockAllocator<>(
         524288L,
         MAX_GPU_ALIGNMENT,
         TransientBlockAllocator.Allocator.create(this::allocateGpuMappedVulkanBlock, this::freeGpuMappedVulkanBlock),
         this::recordGpuMappedCopy
      );
   }

   @Override
   public void destroy() {
      this.cpuBlockAllocator.close();
      this.stagingBlockAllocator.close();
      this.gpuBlockAllocator.close();
      this.gpuMappedBlockAllocator.close();
   }

   public void beginSubmit() {
      assert this.commandBuffer == null;
      this.commandBuffer = this.encoder.allocateAndBeginTransientCommandBuffer();
      this.encoder.execute(this.commandBuffer);
      this.anyCommandRecorded = false;
   }

   public void endSubmit() {
      this.cpuBlockAllocator.rotate().run();
      this.encoder.queueForDestroy(this.stagingBlockAllocator.rotate()::run);
      if (this.useDeviceMemoryForMappedGpuStaging) {
         this.encoder.queueForDestroy(this.gpuBlockAllocator.rotate()::run);
      } else {
         this.gpuBlockAllocator.rotate().run();
      }

      this.gpuMappedBlockAllocator.rotate();
      assert this.commandBuffer != null;
      if (this.anyCommandRecorded) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            VulkanCommandEncoder.memoryBarrier(this.commandBuffer, stack);
         } catch (Throwable var5) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (stack != null) {
            stack.close();
         }
      }

      VK12.vkEndCommandBuffer(this.commandBuffer);
      this.commandBuffer = null;
      this.submitIndex++;
   }

   private void recordGpuMappedCopy(final Pair<VulkanTransientMemory.VulkanAllocation, VulkanTransientMemory.VulkanAllocation> block) {
      if (block.first() != block.second()) {
         assert ((VulkanTransientMemory.VulkanAllocation)block.first()).size == ((VulkanTransientMemory.VulkanAllocation)block.second()).size;
         MemoryStack stack = MemoryStack.stackPush();

         try {
            Buffer region = VkBufferCopy.calloc(1, stack);
            region.srcOffset(0L);
            region.dstOffset(0L);
            region.size(((VulkanTransientMemory.VulkanAllocation)block.first()).size);
            assert this.commandBuffer != null;
            VK12.vkCmdCopyBuffer(
               this.commandBuffer,
               ((VulkanTransientMemory.VulkanAllocation)block.first()).vkBuffer,
               ((VulkanTransientMemory.VulkanAllocation)block.second()).vkBuffer,
               region
            );
            this.anyCommandRecorded = true;
         } catch (Throwable var6) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private VulkanTransientMemory.VulkanAllocation allocateVulkanBlock(final long size, final boolean staging) {
      MemoryStack stack = MemoryStack.stackPush();

      VulkanTransientMemory.VulkanAllocation var11;
      try {
         VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack).sType$Default();
         bufferCreateInfo.size(size);
         bufferCreateInfo.usage(471);
         bufferCreateInfo.sharingMode(0);
         bufferCreateInfo.pQueueFamilyIndices(null);
         VmaAllocationCreateInfo allocCreateInfo = VmaAllocationCreateInfo.calloc(stack);
         if (staging) {
            allocCreateInfo.usage(9);
         } else {
            allocCreateInfo.usage(8);
         }

         if (this.useDeviceMemoryForMappedGpuStaging || staging) {
            allocCreateInfo.requiredFlags(6);
            allocCreateInfo.flags(1024);
         }

         LongBuffer bufferPtr = stack.callocLong(1);
         PointerBuffer allocPtr = stack.callocPointer(1);
         int result = Vma.vmaCreateBuffer(this.device.vma(), bufferCreateInfo, allocCreateInfo, bufferPtr, allocPtr, null);
         VulkanUtils.crashIfFailure(this.device, result, "Failed to allocate VkBuffer");
         PointerBuffer hostPtrPtr = stack.callocPointer(1);
         if (staging || this.useDeviceMemoryForMappedGpuStaging) {
            VulkanUtils.crashIfFailure(this.device, Vma.vmaMapMemory(this.device.vma(), allocPtr.get(0), hostPtrPtr), "Failed to map buffer");
         }

         this.device.instance().debug().setObjectName(this.device.vkDevice(), 9, bufferPtr.get(0), "Vulkan Transient Memory Buffer");
         var11 = new VulkanTransientMemory.VulkanAllocation(bufferPtr.get(0), allocPtr.get(0), hostPtrPtr.get(0), size);
      } catch (Throwable var13) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (stack != null) {
         stack.close();
      }

      return var11;
   }

   private void queueFreeVulkanBlock(final VulkanTransientMemory.VulkanAllocation allocation) {
      this.encoder.queueForDestroy(() -> this.freeVulkanBlock(allocation));
   }

   private void freeVulkanBlock(final VulkanTransientMemory.VulkanAllocation allocation) {
      Vma.vmaDestroyBuffer(this.device.vma(), allocation.vkBuffer, allocation.vmaAllocation);
   }

   private Pair<VulkanTransientMemory.VulkanAllocation, VulkanTransientMemory.VulkanAllocation> allocateGpuMappedVulkanBlock(final long size) {
      assert size >= 524288L;
      assert size >= this.gpuBlockAllocator.blockSize();
      if (this.useDeviceMemoryForMappedGpuStaging) {
         TransientBlockAllocator.Allocation<VulkanTransientMemory.VulkanAllocation> block = this.gpuBlockAllocator.allocate(size, 16L, size, 1L);
         assert block.offset() == 0L;
         return new ReferenceReferenceImmutablePair(block.block(), block.block());
      } else {
         assert size >= this.stagingBlockAllocator.blockSize();
         TransientBlockAllocator.Allocation<VulkanTransientMemory.VulkanAllocation> stagingBlock = this.stagingBlockAllocator.allocate(size, 16L, size, 1L);
         TransientBlockAllocator.Allocation<VulkanTransientMemory.VulkanAllocation> gpuBlock = this.gpuBlockAllocator.allocate(size, 16L, size, 1L);
         return new ReferenceReferenceImmutablePair(stagingBlock.block(), gpuBlock.block());
      }
   }

   private void freeGpuMappedVulkanBlock(final Pair<VulkanTransientMemory.VulkanAllocation, VulkanTransientMemory.VulkanAllocation> allocations) {
   }

   @Override
   public ByteBuffer allocateCpu(final long size, final long alignment, final long minimumAllocation, final long elementSize) {
      assert size <= 2147483647L;
      TransientBlockAllocator.Allocation<Long> alloc = this.cpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
      return MemoryUtil.memByteBuffer(alloc.block() + alloc.offset(), (int)alloc.size());
   }

   @Override
   public GpuBufferSlice.MappedView allocateStaging(
      final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      assert size <= 2147483647L;
      TransientBlockAllocator.Allocation<VulkanTransientMemory.VulkanAllocation> alloc = this.stagingBlockAllocator
         .allocate(size, alignment, minimumAllocation, elementSize);
      VulkanTransientMemory.TransientGpuBuffer apiBuffer = new VulkanTransientMemory.TransientGpuBuffer(
         alloc.block().vkBuffer, usage, (int)alloc.block().size, this.submitIndex
      );
      ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer(alloc.block().hostPtr + alloc.offset(), (int)alloc.size());
      return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
   }

   @Override
   public GpuBufferSlice allocateGpu(
      final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      assert size <= 2147483647L;
      TransientBlockAllocator.Allocation<VulkanTransientMemory.VulkanAllocation> alloc = this.gpuBlockAllocator
         .allocate(size, alignment, minimumAllocation, elementSize);
      VulkanTransientMemory.TransientGpuBuffer apiBuffer = new VulkanTransientMemory.TransientGpuBuffer(
         alloc.block().vkBuffer, usage, (int)alloc.block().size, this.submitIndex
      );
      return new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size());
   }

   @Override
   public GpuBufferSlice.MappedView allocateGpuMapped(
      final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      assert size <= 2147483647L;
      TransientBlockAllocator.Allocation<Pair<VulkanTransientMemory.VulkanAllocation, VulkanTransientMemory.VulkanAllocation>> alloc = this.gpuMappedBlockAllocator
         .allocate(size, alignment, minimumAllocation, elementSize);
      VulkanTransientMemory.TransientGpuBuffer apiBuffer = new VulkanTransientMemory.TransientGpuBuffer(
         ((VulkanTransientMemory.VulkanAllocation)alloc.block().second()).vkBuffer,
         usage,
         (int)((VulkanTransientMemory.VulkanAllocation)alloc.block().first()).size,
         this.submitIndex
      );
      ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer(
         ((VulkanTransientMemory.VulkanAllocation)alloc.block().first()).hostPtr + alloc.offset(), (int)alloc.size()
      );
      return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
   }

   @Override
   public GpuBufferSlice uploadStaging(
      final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      return this.upload(data, alignment, usage, minimumAllocation, elementSize, true);
   }

   @Override
   public GpuBufferSlice uploadGpu(
      final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      return this.upload(data, alignment, usage, minimumAllocation, elementSize, false);
   }

   public GpuBufferSlice upload(
      final List<ByteBuffer> data,
      final long alignment,
      final @GpuBuffer.Usage int usage,
      final long minimumAllocation,
      final long elementSize,
      final boolean staging
   ) {
      long totalSize = 0L;

      for (ByteBuffer buffer : data) {
         totalSize += buffer.remaining();
         totalSize = Mth.roundToward(totalSize, alignment);
      }

      try (GpuBufferSlice.MappedView mapped = staging
            ? this.allocateStaging(totalSize, alignment, usage, minimumAllocation, elementSize)
            : this.allocateGpuMapped(totalSize, alignment, usage, minimumAllocation, elementSize)) {
         long mappedPtr = MemoryUtil.memAddress(mapped.data());
         long offset = 0L;

         for (ByteBuffer buffer : data) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), mappedPtr + offset, Math.min(mapped.slice().length() - offset, buffer.remaining()));
            offset += buffer.remaining();
            offset = Mth.roundToward(offset, alignment);
            if (offset >= mapped.slice().length()) {
               break;
            }
         }

         return mapped.slice();
      }
   }

   @Override
   public List<GpuBufferSlice> multiUploadStaging(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.multiUpload(data, alignment, usage, true);
   }

   @Override
   public List<GpuBufferSlice> multiUploadGpu(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.multiUpload(data, alignment, usage, false);
   }

   public List<GpuBufferSlice> multiUpload(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final boolean staging) {
      ReferenceArrayList<GpuBufferSlice> uploadedBuffers = new ReferenceArrayList();
      uploadedBuffers.size(data.size());
      TransientBlockAllocator<?> allocatorInUse = staging ? this.stagingBlockAllocator : this.gpuMappedBlockAllocator;
      IntArrayList sortedDataIndices = IntArrayList.toList(IntStream.range(0, data.size()));
      sortedDataIndices.sort(IntComparator.comparing(index -> data.get(index).remaining()));

      while (!sortedDataIndices.isEmpty()) {
         boolean allocatedAnything = false;

         for (int i = sortedDataIndices.size() - 1; i >= 0; i--) {
            int bufferIndex = sortedDataIndices.getInt(i);
            ByteBuffer currentBuffer = data.get(bufferIndex);
            if (allocatorInUse.canAllocateInCurrentBlock(currentBuffer.remaining(), alignment)) {
               sortedDataIndices.removeInt(i);

               try (GpuBufferSlice.MappedView view = staging
                     ? this.allocateStaging(currentBuffer.remaining(), alignment, usage)
                     : this.allocateGpuMapped(currentBuffer.remaining(), alignment, usage)) {
                  MemoryUtil.memCopy(currentBuffer, view.data());
                  uploadedBuffers.set(bufferIndex, view.slice());
               }

               allocatedAnything = true;
               break;
            }
         }

         if (!allocatedAnything) {
            int bufferIndex = sortedDataIndices.popInt();
            ByteBuffer currentBuffer = data.get(bufferIndex);

            try (GpuBufferSlice.MappedView view = this.allocateGpuMapped(currentBuffer.remaining(), alignment, usage)) {
               MemoryUtil.memCopy(currentBuffer, view.data());
               uploadedBuffers.set(bufferIndex, view.slice());
            }
         }
      }

      return uploadedBuffers;
   }

   private class TransientGpuBuffer extends VulkanGpuBuffer {
      private boolean closed = false;
      private final long bufferSubmitIndex;

      public TransientGpuBuffer(final long vkBuffer, final @GpuBuffer.Usage int usage, final int size, final long bufferSubmitIndex) {
         super(vkBuffer, usage, size);
         this.bufferSubmitIndex = bufferSubmitIndex;
      }

      @Override
      public void destroy() {
      }

      @Override
      public GpuBufferSlice.MappedView map(final long offset, final long length, final boolean read, final boolean write) {
         throw new IllegalStateException("Cannot map transient buffer");
      }

      @Override
      public boolean isClosed() {
         if (this.closed) {
            return true;
         }

         this.closed = this.bufferSubmitIndex < VulkanTransientMemory.this.submitIndex;
         return this.closed;
      }

      @Override
      public void close() {
         this.closed = true;
      }

      @Override
      public GpuBufferSlice slice(final long offset, final long length) {
         throw new IllegalStateException("Cannot slice transient buffer");
      }

      @Override
      public GpuBufferSlice slice() {
         throw new IllegalStateException("Cannot slice transient buffer");
      }
   }

   private record VulkanAllocation(long vkBuffer, long vmaAllocation, long hostPtr, long size) {
   }
}
