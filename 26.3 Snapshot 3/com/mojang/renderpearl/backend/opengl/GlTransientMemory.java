package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.backend.util.TransientBlockAllocator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;

public abstract class GlTransientMemory implements TransientMemory, AutoCloseable {
   private static final long BLOCK_SIZE = 524288L;
   private static final long MAX_CPU_ALIGNMENT = 16L;
   private static final long MAX_GPU_ALIGNMENT = Long.highestOneBit(Long.MAX_VALUE);
   final GlCommandEncoder encoder;
   protected final DirectStateAccess dsa;
   protected final BufferStorage bufferStorage;
   protected final GlDebugLabel debugLabels;
   private final TransientBlockAllocator<Long> cpuBlockAllocator = new TransientBlockAllocator<>(
      524288L, 16L, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree)
   );

   GlTransientMemory(final GlDevice device, final GlCommandEncoder encoder) {
      this.encoder = encoder;
      this.dsa = device.directStateAccess();
      this.bufferStorage = device.getBufferStorage();
      this.debugLabels = device.debugLabels();
   }

   @Override
   public void close() {
   }

   @Override
   public ByteBuffer allocateCpu(final long size, final long alignment, final long minimumAllocation, final long elementSize) {
      assert size <= 2147483647L;
      TransientBlockAllocator.Allocation<Long> alloc = this.cpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
      return MemoryUtil.memByteBuffer(alloc.block() + alloc.offset(), (int)alloc.size());
   }

   public void rotate() {
      this.cpuBlockAllocator.rotate().run();
   }

   static class Fallback extends GlTransientMemory {
      private final TransientBlockAllocator<GlTransientMemory.Fallback.GlAllocation> blockAllocator = new TransientBlockAllocator<>(
         524288L, GlTransientMemory.MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(this::allocateGlBlock, this::freeGlBlock)
      );

      Fallback(final GlDevice device, final GlCommandEncoder encoder) {
         super(device, encoder);
      }

      @Override
      public void close() {
         this.rotate();
      }

      @Override
      public void rotate() {
         this.blockAllocator.rotate().run();
      }

      private GlTransientMemory.Fallback.GlAllocation allocateGlBlock(final long size) {
         GlBuffer buffer = this.bufferStorage.createBuffer(this.dsa, 40, size);
         this.debugLabels.applyLabel(buffer, () -> "OpenGL Transient Buffer");
         long hostPtr = MemoryUtil.nmemAlloc(size);
         return new GlTransientMemory.Fallback.GlAllocation(buffer, hostPtr);
      }

      private void freeGlBlock(final GlTransientMemory.Fallback.GlAllocation allocation) {
         allocation.glBuffer.close();
         MemoryUtil.nmemFree(allocation.hostBuffer);
      }

      @Override
      public GpuBufferSlice.MappedView allocateStaging(
         final long size, final long alignment, final int usage, final long minimumAllocation, final long elementSize
      ) {
         TransientBlockAllocator.Allocation<GlTransientMemory.Fallback.GlAllocation> allocation = this.blockAllocator
            .allocate(size, alignment, minimumAllocation, elementSize);
         GlTransientMemory.TransientGpuBuffer transientBuffer = new GlTransientMemory.TransientGpuBuffer(
            allocation.block().glBuffer().handle(), usage, allocation.block().glBuffer().size()
         );
         GpuBufferSlice slice = new GpuBufferSlice(transientBuffer, allocation.offset(), allocation.size());
         ByteBuffer hostBuffer = MemoryUtil.memByteBuffer(allocation.block().hostBuffer + allocation.offset(), (int)allocation.size());
         return new GpuBufferSlice.MappedView(
            slice,
            hostBuffer,
            () -> this.dsa
               .bufferSubData(
                  transientBuffer.handle(),
                  slice.offset(),
                  MemoryUtil.memByteBuffer(allocation.block().hostBuffer + allocation.offset(), (int)allocation.size()),
                  usage
               )
         );
      }

      @Override
      public GpuBufferSlice allocateGpu(final long size, final long alignment, final int usage, final long minimumAllocation, final long elementSize) {
         TransientBlockAllocator.Allocation<GlTransientMemory.Fallback.GlAllocation> allocation = this.blockAllocator
            .allocate(size, alignment, minimumAllocation, elementSize);
         GlTransientMemory.TransientGpuBuffer transientBuffer = new GlTransientMemory.TransientGpuBuffer(
            allocation.block().glBuffer().handle(), usage, allocation.block().glBuffer().size()
         );
         return new GpuBufferSlice(transientBuffer, allocation.offset(), allocation.size());
      }

      @Override
      public GpuBufferSlice.MappedView allocateGpuMapped(
         final long size, final long alignment, final int usage, final long minimumAllocation, final long elementSize
      ) {
         return this.allocateStaging(size, alignment, usage, minimumAllocation, elementSize);
      }

      @Override
      public GpuBufferSlice uploadStaging(
         final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
      ) {
         return this.uploadGpu(data, alignment, usage, minimumAllocation, elementSize);
      }

      @Override
      public GpuBufferSlice uploadGpu(final List<ByteBuffer> data, final long alignment, final int usage, final long minimumAllocation, final long elementSize) {
         long totalSize = 0L;

         for (int i = 0; i < data.size(); i++) {
            totalSize += data.get(i).remaining();
            totalSize = Mth.roundToward(totalSize, alignment);
         }

         GpuBufferSlice bufferSlice = this.allocateGpu(totalSize, alignment, usage);
         int target = GlUtil.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
         long ptr = GL33C.nglMapBufferRange(target, bufferSlice.offset(), totalSize, 38);
         long offset = 0L;

         for (int i = 0; i < data.size(); i++) {
            ByteBuffer buffer = data.get(i);
            MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), ptr + offset, buffer.remaining());
            offset += buffer.remaining();
            offset = Mth.roundToward(offset, alignment);
         }

         GL33C.glUnmapBuffer(target);
         GlStateManager._glBindBuffer(target, 0);
         return bufferSlice;
      }

      @Override
      public List<GpuBufferSlice> multiUploadStaging(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage) {
         return this.multiUploadGpu(data, alignment, usage);
      }

      @Override
      public List<GpuBufferSlice> multiUploadGpu(final List<ByteBuffer> data, final long alignment, final int usage) {
         ReferenceArrayList<GpuBufferSlice> uploadedBuffers = new ReferenceArrayList();
         uploadedBuffers.size(data.size());
         IntArrayList sortedDataIndices = IntArrayList.toList(IntStream.range(0, data.size()));
         sortedDataIndices.sort(IntComparator.comparing(index -> data.get(index).remaining()));
         int target = GlUtil.selectBufferBindTarget(usage);

         while (!sortedDataIndices.isEmpty()) {
            boolean allocatedAnything = false;

            for (int i = sortedDataIndices.size() - 1; i >= 0; i--) {
               int bufferIndex = sortedDataIndices.getInt(i);
               ByteBuffer currentBuffer = data.get(bufferIndex);
               if (this.blockAllocator.canAllocateInCurrentBlock(currentBuffer.remaining(), alignment)) {
                  sortedDataIndices.removeInt(i);
                  GpuBufferSlice bufferSlice = this.allocateGpu(currentBuffer.remaining(), alignment, usage);
                  uploadedBuffers.set(bufferIndex, bufferSlice);
                  GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
                  long ptr = GL33C.nglMapBufferRange(target, bufferSlice.offset(), bufferSlice.length(), 38);
                  MemoryUtil.memCopy(MemoryUtil.memAddress(currentBuffer), ptr, bufferSlice.length());
                  GL33C.glUnmapBuffer(target);
                  allocatedAnything = true;
                  break;
               }
            }

            if (!allocatedAnything) {
               int bufferIndex = sortedDataIndices.popInt();
               ByteBuffer currentBuffer = data.get(bufferIndex);
               GpuBufferSlice bufferSlice = this.allocateGpu(currentBuffer.remaining(), alignment, usage);
               uploadedBuffers.set(bufferIndex, bufferSlice);
               GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
               long ptr = GL33C.nglMapBufferRange(target, bufferSlice.offset(), bufferSlice.length(), 38);
               MemoryUtil.memCopy(MemoryUtil.memAddress(currentBuffer), ptr, bufferSlice.length());
               GL33C.glUnmapBuffer(target);
            }
         }

         GlStateManager._glBindBuffer(target, 0);
         return uploadedBuffers;
      }

      private record GlAllocation(GlBuffer glBuffer, long hostBuffer) {
      }
   }

   static class PersistentMapping extends GlTransientMemory {
      private final TransientBlockAllocator<GlTransientMemory.PersistentMapping.GlAllocation> stagingBlockAllocator;
      private final TransientBlockAllocator<GlTransientMemory.PersistentMapping.GlAllocation> gpuBlockAllocator;
      private final TransientBlockAllocator<GlTransientMemory.PersistentMapping.GlAllocation> gpuMappedBlockAllocator;
      private final GlTransientMemory.PersistentMapping.@Nullable Rotation[] rotations = new GlTransientMemory.PersistentMapping.Rotation[2];

      PersistentMapping(final GlDevice device, final GlCommandEncoder encoder) {
         super(device, encoder);
         this.stagingBlockAllocator = new TransientBlockAllocator<>(
            524288L,
            GlTransientMemory.MAX_GPU_ALIGNMENT,
            TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, true, true), this::freeGlBlock)
         );
         this.gpuBlockAllocator = new TransientBlockAllocator<>(
            524288L,
            GlTransientMemory.MAX_GPU_ALIGNMENT,
            TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, false, false), this::freeGlBlock)
         );
         this.gpuMappedBlockAllocator = new TransientBlockAllocator<>(
            524288L,
            GlTransientMemory.MAX_GPU_ALIGNMENT,
            TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, false, true), this::freeGlBlock)
         );
      }

      @Override
      public void close() {
         GL33C.glFinish();

         for (GlTransientMemory.PersistentMapping.Rotation rotation : this.rotations) {
            if (rotation != null) {
               rotation.run();
            }
         }
      }

      @Override
      public void rotate() {
         GlTransientMemory.PersistentMapping.Rotation previousRotation = this.rotations[this.encoder.currentSubmitSlot()];
         if (previousRotation != null) {
            previousRotation.run();
         }

         this.rotations[this.encoder.currentSubmitSlot()] = new GlTransientMemory.PersistentMapping.Rotation(
            this.stagingBlockAllocator.rotate(), this.gpuBlockAllocator.rotate(), this.gpuMappedBlockAllocator.rotate()
         );
         super.rotate();
      }

      private GlTransientMemory.PersistentMapping.GlAllocation allocateGlBlock(final long size, final boolean host, final boolean mapped) {
         int glBuffer = this.dsa.createBuffer();
         int usageFlags = 0;
         if (host) {
            usageFlags |= 512;
         }

         if (mapped) {
            usageFlags |= 2;
            usageFlags |= 64;
            usageFlags |= 128;
         }

         GlStateManager._glBindBuffer(34962, glBuffer);
         ARBBufferStorage.glBufferStorage(34962, size, usageFlags);
         long hostPtr;
         if (mapped) {
            hostPtr = GL33C.nglMapBufferRange(34962, 0L, size, 226);
         } else {
            hostPtr = 0L;
         }

         GlStateManager._glBindBuffer(34962, 0);
         this.debugLabels.applyLabel(new GlTransientMemory.TransientGpuBuffer(glBuffer, 0, size), () -> "OpenGL Transient Buffer");
         return new GlTransientMemory.PersistentMapping.GlAllocation(glBuffer, hostPtr, size);
      }

      private void freeGlBlock(final GlTransientMemory.PersistentMapping.GlAllocation allocation) {
         GlStateManager._glDeleteBuffers(allocation.glBuffer);
      }

      @Override
      public GpuBufferSlice.MappedView allocateStaging(
         final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
      ) {
         assert size <= 2147483647L;
         TransientBlockAllocator.Allocation<GlTransientMemory.PersistentMapping.GlAllocation> alloc = this.stagingBlockAllocator
            .allocate(size, alignment, minimumAllocation, elementSize);
         GlTransientMemory.TransientGpuBuffer apiBuffer = new GlTransientMemory.TransientGpuBuffer(alloc.block().glBuffer, usage, (int)alloc.block().size);
         ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer(alloc.block().hostPtr + alloc.offset(), (int)alloc.size());
         return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
      }

      @Override
      public GpuBufferSlice allocateGpu(
         final long size, final long alignment, final int usage, final @GpuBuffer.Usage long minimumAllocation, final long elementSize
      ) {
         assert size <= 2147483647L;
         TransientBlockAllocator.Allocation<GlTransientMemory.PersistentMapping.GlAllocation> alloc = this.gpuBlockAllocator
            .allocate(size, alignment, minimumAllocation, elementSize);
         GlTransientMemory.TransientGpuBuffer apiBuffer = new GlTransientMemory.TransientGpuBuffer(alloc.block().glBuffer, usage, (int)alloc.block().size);
         return new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size());
      }

      @Override
      public GpuBufferSlice.MappedView allocateGpuMapped(
         final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
      ) {
         assert size <= 2147483647L;
         TransientBlockAllocator.Allocation<GlTransientMemory.PersistentMapping.GlAllocation> alloc = this.gpuMappedBlockAllocator
            .allocate(size, alignment, minimumAllocation, elementSize);
         GlTransientMemory.TransientGpuBuffer apiBuffer = new GlTransientMemory.TransientGpuBuffer(alloc.block().glBuffer, usage, (int)alloc.block().size);
         ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer(alloc.block().hostPtr + alloc.offset(), (int)alloc.size());
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

      private record GlAllocation(int glBuffer, long hostPtr, long size) {
      }

      private record Rotation(Runnable staging, Runnable gpu, Runnable gpuMapped) implements Runnable {
         @Override
         public void run() {
            this.staging.run();
            this.gpu.run();
            this.gpuMapped.run();
         }
      }
   }

   private class TransientGpuBuffer extends GlBuffer {
      private boolean closed = false;
      private final long bufferSubmitIndex = GlTransientMemory.this.encoder.currentSubmitIndex();

      protected TransientGpuBuffer(final int handle, final @GpuBuffer.Usage int usage, final long size) {
         super(usage, size, handle, true);
      }

      @Override
      public boolean isClosed() {
         if (this.closed) {
            return true;
         }

         this.closed = this.bufferSubmitIndex < GlTransientMemory.this.encoder.currentSubmitIndex();
         return this.closed;
      }

      @Override
      public void close() {
         this.closed = true;
      }

      @Override
      public GpuBufferSlice.MappedView map(final long offset, final long length, final boolean read, final boolean write) {
         throw new IllegalStateException("Cannot map Transient buffer");
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
}
