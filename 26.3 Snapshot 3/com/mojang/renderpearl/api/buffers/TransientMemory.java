package com.mojang.renderpearl.api.buffers;

import java.nio.ByteBuffer;
import java.util.List;

public interface TransientMemory {
   default ByteBuffer allocateCpu(final long size, final long alignment) {
      return this.allocateCpu(size, alignment, size, 1L);
   }

   ByteBuffer allocateCpu(final long size, final long alignment, final long minimumAllocation, final long elementSize);

   default GpuBufferSlice.MappedView allocateStaging(final long size, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.allocateStaging(size, alignment, usage, size, 1L);
   }

   GpuBufferSlice.MappedView allocateStaging(
      final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   );

   default GpuBufferSlice allocateGpu(final long size, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.allocateGpu(size, alignment, usage, size, 1L);
   }

   GpuBufferSlice allocateGpu(final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize);

   default GpuBufferSlice.MappedView allocateGpuMapped(final long size, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.allocateGpuMapped(size, alignment, usage, size, 1L);
   }

   GpuBufferSlice.MappedView allocateGpuMapped(
      final long size, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   );

   default GpuBufferSlice uploadStaging(final ByteBuffer data, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.uploadStaging(data, alignment, usage, data.remaining(), 1L);
   }

   default GpuBufferSlice uploadStaging(
      final ByteBuffer data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      return this.uploadStaging(List.of(data), alignment, usage, minimumAllocation, elementSize);
   }

   default GpuBufferSlice uploadStaging(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage) {
      long totalSize = 0L;

      for (ByteBuffer buffer : data) {
         totalSize += buffer.remaining();
      }

      return this.uploadStaging(data, alignment, usage, totalSize, 1L);
   }

   GpuBufferSlice uploadStaging(
      final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   );

   default GpuBufferSlice uploadGpu(final ByteBuffer data, final long alignment, final @GpuBuffer.Usage int usage) {
      return this.uploadGpu(data, alignment, usage, data.remaining(), 1L);
   }

   default GpuBufferSlice uploadGpu(
      final ByteBuffer data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   ) {
      return this.uploadGpu(List.of(data), alignment, usage, minimumAllocation, elementSize);
   }

   default GpuBufferSlice uploadGpu(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage) {
      long totalSize = 0L;

      for (ByteBuffer buffer : data) {
         totalSize += buffer.remaining();
      }

      return this.uploadGpu(data, alignment, usage, totalSize, 1L);
   }

   GpuBufferSlice uploadGpu(
      final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage, final long minimumAllocation, final long elementSize
   );

   List<GpuBufferSlice> multiUploadStaging(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage);

   List<GpuBufferSlice> multiUploadGpu(final List<ByteBuffer> data, final long alignment, final @GpuBuffer.Usage int usage);
}
