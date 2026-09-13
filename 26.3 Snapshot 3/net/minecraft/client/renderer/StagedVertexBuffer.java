package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class StagedVertexBuffer implements AutoCloseable {
   private final ByteBufferBuilder stagingBuffer;
   private final StagedVertexBuffer.GpuBufferPool stagingGpuBufferPool;
   private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
   private StagedVertexBuffer.@Nullable Draw lastBuildingDraw;
   private @Nullable BufferBuilder lastVertexBuilder;
   private final StagedVertexBuffer.GpuBufferPool vertexBufferPool;
   private final StagedVertexBuffer.GpuBufferPool indexBufferPool;
   private @Nullable GpuBuffer currentVertexBuffer;
   private @Nullable GpuBuffer currentIndexBuffer;

   public StagedVertexBuffer(final Supplier<String> label, final int initialCapacity) {
      this.stagingBuffer = new ByteBufferBuilder(initialCapacity);
      this.stagingGpuBufferPool = new StagedVertexBuffer.GpuBufferPool(() -> label.get() + " - Staging", 22);
      this.vertexBufferPool = new StagedVertexBuffer.GpuBufferPool(() -> label.get() + " - Vertex", 40);
      this.indexBufferPool = new StagedVertexBuffer.GpuBufferPool(() -> label.get() + " - Index", 72);
   }

   public StagedVertexBuffer.Draw appendDraw(final VertexFormat format, final PrimitiveTopology primitiveTopology) {
      return this.appendDraw(format, primitiveTopology, null);
   }

   public StagedVertexBuffer.Draw appendDraw(final VertexFormat format, final PrimitiveTopology primitiveTopology, final @Nullable VertexSorting quadSorting) {
      if (this.currentVertexBuffer != null) {
         throw new IllegalStateException("Cannot append draw after upload");
      }

      if (quadSorting != null && primitiveTopology != PrimitiveTopology.QUADS) {
         throw new IllegalArgumentException("Cannot sort draw with " + primitiveTopology);
      }

      StagedVertexBuffer.Draw draw = new StagedVertexBuffer.Draw(format, primitiveTopology, quadSorting);
      this.draws.add(draw);
      return draw;
   }

   public VertexConsumer getVertexBuilder(final StagedVertexBuffer.Draw draw) {
      if (this.currentVertexBuffer != null) {
         throw new IllegalStateException("Cannot append draw after upload");
      }

      if (this.lastBuildingDraw == draw) {
         return Objects.requireNonNull(this.lastVertexBuilder);
      }

      this.finishLastVertexBuilder();
      this.lastBuildingDraw = draw;
      this.lastVertexBuilder = new BufferBuilder(this.stagingBuffer, draw.primitiveTopology, draw.format);
      return this.lastVertexBuilder;
   }

   private void finishLastVertexBuilder() {
      if (this.lastVertexBuilder != null) {
         MeshData mesh = this.lastVertexBuilder.build();
         if (mesh != null) {
            Objects.requireNonNull(this.lastBuildingDraw).append(mesh);
         }

         this.lastVertexBuilder = null;
         this.lastBuildingDraw = null;
      }
   }

   public void upload() {
      if (this.currentVertexBuffer != null) {
         throw new IllegalStateException("Already uploaded");
      }

      if (!this.draws.isEmpty()) {
         this.finishLastVertexBuilder();
         int nextVertexOffset = 0;
         int nextIndexOffset = 0;

         for (StagedVertexBuffer.Draw draw : this.draws) {
            if (!draw.isEmpty()) {
               draw.vertexOffset = Mth.roundToward(nextVertexOffset, draw.format.getVertexSize());
               nextVertexOffset = draw.vertexOffset + draw.vertexBufferSize;
               if (draw.quadSorting != null) {
                  IndexType indexType = draw.indexType();
                  draw.indexOffset = Mth.roundToward(nextIndexOffset, indexType.bytes);
                  nextIndexOffset = draw.indexOffset + draw.indexCount * indexType.bytes;
               } else {
                  RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(draw.primitiveTopology);
                  autoIndices.getBuffer(draw.indexCount);
               }
            }
         }

         int vertexBufferSize = nextVertexOffset;
         int indexBufferSize = nextIndexOffset;
         if (vertexBufferSize != 0) {
            GpuDevice device = RenderSystem.getDevice();
            this.currentVertexBuffer = this.vertexBufferPool.acquire(device, vertexBufferSize);
            this.currentIndexBuffer = indexBufferSize > 0 ? this.indexBufferPool.acquire(device, indexBufferSize) : null;
            this.uploadDrawsToBuffers(device, this.draws, this.currentVertexBuffer, this.currentIndexBuffer, vertexBufferSize, indexBufferSize);
         }
      }
   }

   private void uploadDrawsToBuffers(
      final GpuDevice device,
      final List<StagedVertexBuffer.Draw> draws,
      final GpuBuffer vertexGpuBuffer,
      final @Nullable GpuBuffer indexGpuBuffer,
      final int vertexBufferSize,
      final int indexBufferSize
   ) {
      CommandEncoder commandEncoder = device.createCommandEncoder();
      int stagingBufferSize = vertexBufferSize + indexBufferSize;
      GpuBuffer stagingBuffer = this.stagingGpuBufferPool.acquire(device, stagingBufferSize);

      try (GpuBufferSlice.MappedView view = stagingBuffer.slice(0L, stagingBufferSize).map(false, true)) {
         ByteBuffer buffer = view.data();

         for (StagedVertexBuffer.Draw draw : draws) {
            if (!draw.isEmpty()) {
               buffer.position(draw.vertexOffset);

               for (ByteBufferBuilder.Result slice : draw.vertexBufferSlices) {
                  buffer.put(slice.byteBuffer());
               }
            }
         }

         for (StagedVertexBuffer.Draw draw : draws) {
            if (!draw.isEmpty()) {
               if (indexGpuBuffer != null && draw.quadSorting != null) {
                  MeshData.SortState sortState = new MeshData.SortState(decodeSortingPoints(draw), draw.indexType());
                  buffer.position(vertexBufferSize + draw.indexOffset);
                  sortState.writeSortedIndexBuffer(buffer, draw.quadSorting);
               }

               draw.freeVertexData();
            }
         }
      }

      commandEncoder.copyToBuffer(stagingBuffer.slice(0L, vertexBufferSize), vertexGpuBuffer.slice(0L, vertexBufferSize));
      if (indexGpuBuffer != null) {
         commandEncoder.copyToBuffer(stagingBuffer.slice(vertexBufferSize, indexBufferSize), indexGpuBuffer.slice(0L, indexBufferSize));
      }
   }

   private static CompactVectorArray decodeSortingPoints(final StagedVertexBuffer.Draw draw) {
      VertexFormat format = draw.format;
      CompactVectorArray points = new CompactVectorArray(draw.vertexCount / 4);
      int offset = 0;

      for (ByteBufferBuilder.Result vertexBuffer : draw.vertexBufferSlices) {
         int vertexCount = vertexBuffer.size() / format.getVertexSize();
         MeshData.decodeQuadCentroids(vertexBuffer.byteBuffer(), vertexCount, format, points, offset);
         offset += vertexCount / 4;
      }

      return points;
   }

   public StagedVertexBuffer.@Nullable ExecuteInfo getExecuteInfo(final StagedVertexBuffer.Draw draw) {
      if (draw.isEmpty()) {
         return null;
      } else if (this.currentVertexBuffer == null) {
         throw new IllegalStateException("Cannot execute before upload");
      } else {
         int baseVertex = draw.vertexOffset / draw.format.getVertexSize();
         if (this.currentIndexBuffer != null && draw.quadSorting != null) {
            IndexType indexType = draw.indexType();
            int firstIndex = draw.indexOffset / indexType.bytes;
            return new StagedVertexBuffer.ExecuteInfo(
               this.currentVertexBuffer, this.currentIndexBuffer, indexType, baseVertex, firstIndex, draw.indexCount, draw.primitiveTopology
            );
         } else {
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(draw.primitiveTopology);
            autoIndices.requestIndexCount(draw.indexCount);
            return new StagedVertexBuffer.ExecuteInfo(
               this.currentVertexBuffer, null, autoIndices.type(), baseVertex, 0, draw.indexCount, draw.primitiveTopology
            );
         }
      }
   }

   public void requestIndexCount(final StagedVertexBuffer.Draw draw) {
      if (this.currentIndexBuffer == null || draw.quadSorting == null) {
         RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(draw.primitiveTopology);
         autoIndices.requestIndexCount(draw.indexCount);
      }
   }

   public void endDraw() {
      this.draws.clear();
      this.currentVertexBuffer = null;
      this.currentIndexBuffer = null;
   }

   public void endFrame() {
      this.endDraw();
      GpuDevice device = RenderSystem.getDevice();
      this.stagingGpuBufferPool.endFrame(device);
      this.vertexBufferPool.endFrame(device);
      this.indexBufferPool.endFrame(device);
   }

   @Override
   public void close() {
      this.stagingBuffer.close();
      this.stagingGpuBufferPool.close();
      this.vertexBufferPool.close();
      this.indexBufferPool.close();
   }

   public static class Draw {
      private final VertexFormat format;
      private final PrimitiveTopology primitiveTopology;
      private final @Nullable VertexSorting quadSorting;
      private final List<ByteBufferBuilder.Result> vertexBufferSlices = new ArrayList<>();
      private int vertexBufferSize;
      private int vertexCount;
      private int indexCount;
      private int vertexOffset;
      private int indexOffset;

      private Draw(final VertexFormat format, final PrimitiveTopology primitiveTopology, final @Nullable VertexSorting quadSorting) {
         this.format = format;
         this.primitiveTopology = primitiveTopology;
         this.quadSorting = quadSorting;
      }

      private void append(final MeshData mesh) {
         assert mesh.indexBuffer() == null;
         this.vertexBufferSlices.add(mesh.vertexBufferSlice());
         this.vertexBufferSize = this.vertexBufferSize + mesh.vertexBuffer().remaining();
         this.vertexCount = this.vertexCount + mesh.drawState().vertexCount();
         this.indexCount = this.indexCount + mesh.drawState().indexCount();
      }

      private IndexType indexType() {
         return IndexType.least(this.vertexCount);
      }

      private void freeVertexData() {
         this.vertexBufferSlices.forEach(ByteBufferBuilder.Result::close);
         this.vertexBufferSlices.clear();
      }

      public boolean isEmpty() {
         return this.vertexCount == 0;
      }
   }

   public record ExecuteInfo(
      GpuBuffer vertexBuffer,
      @Nullable GpuBuffer customIndexBuffer,
      IndexType indexType,
      int baseVertex,
      int firstIndex,
      int indexCount,
      PrimitiveTopology topology
   ) {
      public GpuBuffer indexBuffer() {
         if (this.customIndexBuffer == null) {
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(this.topology);
            return autoIndices.getBuffer();
         } else {
            return this.customIndexBuffer;
         }
      }
   }

   private static class GpuBufferPool implements AutoCloseable {
      private static final int BUFFER_SIZE_INCREMENT = 262144;
      private static final int MAX_REUSE_SIZE_FACTOR = 4;
      private final Supplier<String> label;
      private final @GpuBuffer.Usage int usage;
      private final List<GpuBuffer> available = new ArrayList<>();
      private final List<GpuBuffer> usedThisFrame = new ArrayList<>();
      private final List<StagedVertexBuffer.GpuBufferPool.PendingRecycle> pendingRecycle = new ArrayList<>();

      private GpuBufferPool(final Supplier<String> label, final @GpuBuffer.Usage int usage) {
         this.label = label;
         this.usage = usage;
      }

      private void tryRecycleBuffers() {
         this.pendingRecycle.removeIf(buffer -> {
            List<GpuBuffer> recycled = buffer.tryRecycle();
            if (recycled != null) {
               this.available.addAll(recycled);
               return true;
            } else {
               return false;
            }
         });
      }

      public GpuBuffer acquire(final GpuDevice device, final int minSize) {
         this.tryRecycleBuffers();
         int roundedMinSize = Mth.roundToward(minSize, 262144);
         GpuBuffer buffer = this.takeBestAvailable(roundedMinSize, roundedMinSize * 4);
         if (buffer == null) {
            buffer = device.createBuffer(this.label, this.usage, roundedMinSize);
         }

         this.usedThisFrame.add(buffer);
         return buffer;
      }

      private @Nullable GpuBuffer takeBestAvailable(final int minSize, final int maxSize) {
         int bestIndex = -1;
         long bestSize = maxSize + 1;

         for (int i = 0; i < this.available.size(); i++) {
            long size = this.available.get(i).size();
            if (size == minSize) {
               return this.available.remove(i);
            }

            if (size > minSize && size < bestSize) {
               bestIndex = i;
               bestSize = size;
            }
         }

         return bestIndex == -1 ? null : this.available.remove(bestIndex);
      }

      public void endFrame(final GpuDevice device) {
         if (!this.usedThisFrame.isEmpty()) {
            GpuFence fence = device.createCommandEncoder().createFence();
            this.pendingRecycle.add(new StagedVertexBuffer.GpuBufferPool.PendingRecycle(List.copyOf(this.usedThisFrame), fence));
            this.usedThisFrame.clear();
         }

         if (!this.available.isEmpty()) {
            this.available.forEach(GpuBuffer::close);
            this.available.clear();
         }
      }

      @Override
      public void close() {
         this.available.forEach(GpuBuffer::close);
         this.usedThisFrame.forEach(GpuBuffer::close);
         this.pendingRecycle.forEach(StagedVertexBuffer.GpuBufferPool.PendingRecycle::close);
         this.available.clear();
         this.usedThisFrame.clear();
         this.pendingRecycle.clear();
      }

      private record PendingRecycle(List<GpuBuffer> buffers, GpuFence fence) implements AutoCloseable {
         public @Nullable List<GpuBuffer> tryRecycle() {
            if (this.fence.awaitCompletion(0L)) {
               this.fence.close();
               return this.buffers;
            } else {
               return null;
            }
         }

         @Override
         public void close() {
            this.buffers.forEach(GpuBuffer::close);
            this.fence.close();
         }
      }
   }
}
