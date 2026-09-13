package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.StagingBuffer;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SectionRenderDispatcher {
   public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
   private final SectionTaskDynamicQueue queue = new SectionTaskDynamicQueue();
   private final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   private volatile boolean closed;
   private final TracingExecutor executor;
   private final Consumer<SectionRenderDispatcher.RenderSection> onSectionMeshUpdate;
   private final AtomicReference<Vec3> cameraPosition = new AtomicReference<>(Vec3.ZERO);
   private volatile SectionCompiler sectionCompiler;
   private final StagingBuffer stagingBuffer;
   private final Map<ChunkSectionLayer, SectionRenderDispatcher.SectionUberBuffers> chunkUberBuffers;
   private final ReentrantLock copyLock = new ReentrantLock();

   public SectionRenderDispatcher(
      final TracingExecutor executor,
      final RenderBuffers renderBuffers,
      final SectionCompiler sectionCompiler,
      final Consumer<SectionRenderDispatcher.RenderSection> onSectionMeshUpdate
   ) {
      this.onSectionMeshUpdate = onSectionMeshUpdate;
      this.fixedBuffers = renderBuffers.fixedBufferPack();
      this.bufferPool = renderBuffers.sectionBufferPool();
      this.executor = executor;
      this.sectionCompiler = sectionCompiler;
      int vertexBufferHeapSize = 134217728;
      int indexBufferHeapSize = 33554432;
      int stagingBufferSize = 102760448;
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.stagingBuffer = StagingBuffer.create("Chunk", gpuDevice, 102760448);
      this.chunkUberBuffers = Util.makeEnumMap(ChunkSectionLayer.class, layer -> {
         VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
         UberGpuBuffer<SectionMesh> vertexUberBuffer = new UberGpuBuffer<>(layer.label(), 32, 134217728, vertexFormat.getVertexSize(), this.stagingBuffer);
         UberGpuBuffer<SectionMesh> indexUberBuffer = new UberGpuBuffer<>(layer.label(), 64, 33554432, 8, this.stagingBuffer);
         return new SectionRenderDispatcher.SectionUberBuffers(vertexUberBuffer, indexUberBuffer);
      });
   }

   public void setCompiler(final SectionCompiler sectionCompiler) {
      this.sectionCompiler = sectionCompiler;
   }

   private void runTask() {
      if (!this.closed) {
         SectionRenderDispatcher.RenderSection.SectionTask task = this.queue.poll(this.cameraPosition.get());
         if (task != null && !task.isCompleted.get() && !task.isCancelled.get()) {
            try {
               SectionBufferBuilderPack buffer = Objects.requireNonNull(this.bufferPool.acquire());
               SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult result = task.doTask(buffer);
               task.isCompleted.set(true);
               if (result == SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.SUCCESSFUL) {
                  buffer.clearAll();
               } else {
                  buffer.discardAll();
               }

               this.bufferPool.release(buffer);
               this.executor.execute(this::runTask);
            } catch (NullPointerException e) {
               this.queue.add(task);
            } catch (Exception e) {
               Minecraft.getInstance().delayCrash(CrashReport.forThrowable(e, "Batching sections"));
            }
         }
      }
   }

   public void setCameraPosition(final Vec3 cameraPosition) {
      this.cameraPosition.set(cameraPosition);
   }

   public SectionRenderDispatcher.@Nullable RenderSectionBufferSlice getRenderSectionSlice(final SectionMesh sectionMesh, final ChunkSectionLayer layer) {
      SectionRenderDispatcher.SectionUberBuffers uberBuffers = this.chunkUberBuffers.get(layer);
      TlsfAllocator.Allocation vertexSlice = uberBuffers.vertexBuffer.getAllocation(sectionMesh);
      if (vertexSlice == null) {
         return null;
      }

      long vertexBufferOffset = vertexSlice.getOffsetFromHeap();
      TlsfAllocator.Allocation indexSlice = uberBuffers.indexBuffer.getAllocation(sectionMesh);
      long indexBufferOffset = 0L;
      GpuBuffer indexBuffer = null;
      if (indexSlice != null) {
         indexBufferOffset = indexSlice.getOffsetFromHeap();
         indexBuffer = uberBuffers.indexBuffer.getGpuBuffer(indexSlice);
      }

      return new SectionRenderDispatcher.RenderSectionBufferSlice(
         uberBuffers.vertexBuffer.getGpuBuffer(vertexSlice), vertexBufferOffset, indexBuffer, indexBufferOffset
      );
   }

   public void lock() {
      this.copyLock.lock();
   }

   public void unlock() {
      this.copyLock.unlock();
   }

   public void uploadTerrainBuffersToGpu() {
      GpuDevice device = RenderSystem.getDevice();

      try (StagingBuffer.Uploader uploader = this.stagingBuffer.startUploading(device.createCommandEncoder())) {
         for (SectionRenderDispatcher.SectionUberBuffers buffers : this.chunkUberBuffers.values()) {
            boolean performedBufferResize = buffers.vertexBuffer.uploadStagedAllocations(device, uploader);
            buffers.indexBuffer.uploadStagedAllocations(device, uploader);
            if (performedBufferResize) {
               break;
            }
         }
      }
   }

   private void schedule(final SectionRenderDispatcher.RenderSection.SectionTask task) {
      if (!this.closed) {
         this.queue.add(task);
         this.executor.execute(this::runTask);
      }
   }

   public void clearCompileQueue() {
      this.queue.clear();
   }

   public boolean isQueueEmpty() {
      return this.queue.size() == 0;
   }

   public void dispose() {
      this.closed = true;
      this.clearCompileQueue();
      this.copyLock.lock();

      try {
         for (SectionRenderDispatcher.SectionUberBuffers buffers : this.chunkUberBuffers.values()) {
            buffers.vertexBuffer.close();
            buffers.indexBuffer.close();
         }

         this.stagingBuffer.close();
      } finally {
         this.copyLock.unlock();
      }
   }

   @VisibleForDebug
   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, aB: %02d", this.queue.size(), this.bufferPool.getFreeBufferCount());
   }

   @VisibleForDebug
   public int getCompileQueueSize() {
      return this.queue.size();
   }

   @VisibleForDebug
   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   public class RenderSection implements RotatingSectionStorage.Value {
      public final int index;
      public final AtomicReference<SectionMesh> sectionMesh = new AtomicReference<>(CompiledSectionMesh.UNCOMPILED);
      private SectionRenderDispatcher.RenderSection.@Nullable CompileTask lastCompileTask;
      private SectionRenderDispatcher.RenderSection.@Nullable ResortTransparencyTask lastResortTransparencyTask;
      private AABB bb;
      private volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
      private final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private long uploadedTime;
      private long fadeDuration;
      private boolean wasPreviouslyEmpty;

      public RenderSection(final int index, final long sectionNode) {
         this.index = index;
         this.setSectionNode(sectionNode);
      }

      public float getVisibility(final long now) {
         long elapsed = now - this.uploadedTime;
         return elapsed >= this.fadeDuration ? 1.0F : (float)elapsed / (float)this.fadeDuration;
      }

      public void setFadeDuration(final long fadeDuration) {
         this.fadeDuration = fadeDuration;
      }

      public void setWasPreviouslyEmpty(final boolean wasPreviouslyEmpty) {
         this.wasPreviouslyEmpty = wasPreviouslyEmpty;
      }

      public boolean wasPreviouslyEmpty() {
         return this.wasPreviouslyEmpty;
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      @Override
      public void setSectionNode(final long sectionNode) {
         this.reset();
         this.sectionNode = sectionNode;
         int x = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
         int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
         int z = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
         this.renderOrigin.set(x, y, z);
         this.bb = new AABB(x, y, z, x + 16, y + 16, z + 16);
      }

      public SectionMesh getSectionMesh() {
         return this.sectionMesh.get();
      }

      public void reset() {
         this.cancelTasks();
         SectionMesh mesh = this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED);
         SectionRenderDispatcher.this.copyLock.lock();

         try {
            this.releaseSectionMesh(mesh);
         } finally {
            SectionRenderDispatcher.this.copyLock.unlock();
         }

         this.uploadedTime = 0L;
         this.wasPreviouslyEmpty = false;
      }

      public BlockPos getRenderOrigin() {
         return this.renderOrigin;
      }

      @Override
      public long getSectionNode() {
         return this.sectionNode;
      }

      public long getNeighborSectionNode(final Direction direction) {
         return SectionPos.offset(this.sectionNode, direction);
      }

      public void resortTransparency() {
         if (this.getSectionMesh() instanceof CompiledSectionMesh mesh) {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(mesh);
            SectionRenderDispatcher.this.schedule(this.lastResortTransparencyTask);
         }
      }

      public boolean hasTranslucentGeometry() {
         return this.getSectionMesh().hasTranslucentGeometry();
      }

      public boolean transparencyResortingScheduled() {
         return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
      }

      private void cancelTasks() {
         if (this.lastCompileTask != null) {
            this.lastCompileTask.cancel();
            this.lastCompileTask = null;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }
      }

      private SectionRenderDispatcher.RenderSection.SectionTask createCompileTask(final RenderSectionRegion region) {
         this.cancelTasks();
         boolean isRecompile = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
         this.lastCompileTask = new SectionRenderDispatcher.RenderSection.CompileTask(region, isRecompile);
         return this.lastCompileTask;
      }

      public void compileAsync(final RenderSectionRegion region) {
         SectionRenderDispatcher.RenderSection.SectionTask task = this.createCompileTask(region);
         SectionRenderDispatcher.this.schedule(task);
      }

      public void compileSync(final RenderSectionRegion region) {
         SectionRenderDispatcher.RenderSection.SectionTask task = this.createCompileTask(region);
         task.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      private SectionMesh setSectionMesh(final SectionMesh sectionMesh) {
         SectionMesh oldMesh = this.sectionMesh.getAndSet(sectionMesh);
         SectionRenderDispatcher.this.onSectionMeshUpdate.accept(this);
         if (this.uploadedTime == 0L) {
            this.uploadedTime = Util.getMillis();
         }

         return oldMesh;
      }

      private void releaseSectionMesh(final SectionMesh oldMesh) {
         oldMesh.close();

         for (SectionRenderDispatcher.SectionUberBuffers buffers : SectionRenderDispatcher.this.chunkUberBuffers.values()) {
            buffers.vertexBuffer.removeAllocation(oldMesh);
            buffers.indexBuffer.removeAllocation(oldMesh);
         }
      }

      private VertexSorting createVertexSorting(final SectionPos sectionPos, final Vec3 cameraPos) {
         return VertexSorting.byDistance(
            (float)(cameraPos.x - sectionPos.minBlockX()), (float)(cameraPos.y - sectionPos.minBlockY()), (float)(cameraPos.z - sectionPos.minBlockZ())
         );
      }

      private void checkSectionMesh(final CompiledSectionMesh compiledSectionMesh) {
         boolean allBuffersUpdated = true;

         for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            SectionMesh.SectionDraw draw = compiledSectionMesh.getSectionDraw(layer);
            if (draw != null) {
               allBuffersUpdated &= compiledSectionMesh.isIndexBufferUploaded(layer);
               allBuffersUpdated &= compiledSectionMesh.isVertexBufferUploaded(layer);
            }
         }

         if (allBuffersUpdated && this.sectionMesh.get() != compiledSectionMesh) {
            SectionMesh oldMesh = this.setSectionMesh(compiledSectionMesh);
            this.releaseSectionMesh(oldMesh);
         }
      }

      private void vertexBufferUploadCallback(final CompiledSectionMesh sectionMesh, final ChunkSectionLayer layer) {
         sectionMesh.setVertexBufferUploaded(layer);
         this.checkSectionMesh(sectionMesh);
      }

      private void indexBufferUploadCallback(final CompiledSectionMesh sectionMesh, final ChunkSectionLayer layer, final boolean sortedIndexBuffer) {
         sectionMesh.setIndexBufferUploaded(layer);
         if (!sortedIndexBuffer) {
            this.checkSectionMesh(sectionMesh);
         }
      }

      private boolean addSectionBuffersToUberBuffer(
         final ChunkSectionLayer layer, final CompiledSectionMesh key, final @Nullable ByteBuffer vertexBuffer, final @Nullable ByteBuffer indexBuffer
      ) {
         boolean success = true;
         SectionRenderDispatcher.this.copyLock.lock();

         try {
            SectionMesh.SectionDraw draw = key.getSectionDraw(layer);
            if (draw != null) {
               SectionRenderDispatcher.SectionUberBuffers sectionBuffers = SectionRenderDispatcher.this.chunkUberBuffers.get(layer);
               assert sectionBuffers != null;
               if (vertexBuffer != null) {
                  UberGpuBuffer.UploadCallback<CompiledSectionMesh> callback = mesh -> this.vertexBufferUploadCallback(mesh, layer);
                  success &= sectionBuffers.vertexBuffer.addAllocation(key, callback, vertexBuffer);
               }

               if (indexBuffer != null) {
                  boolean sortedIndexBuffer = vertexBuffer == null;
                  UberGpuBuffer.UploadCallback<CompiledSectionMesh> callback = mesh -> this.indexBufferUploadCallback(mesh, layer, sortedIndexBuffer);
                  success &= sectionBuffers.indexBuffer.addAllocation(key, callback, indexBuffer);
               } else {
                  key.setIndexBufferUploaded(layer);
               }
            }

            if (!success && RenderSystem.isOnRenderThread()) {
               SectionRenderDispatcher.this.uploadTerrainBuffersToGpu();
            }
         } finally {
            SectionRenderDispatcher.this.copyLock.unlock();
         }

         return success;
      }

      private class CompileTask extends SectionRenderDispatcher.RenderSection.SectionTask {
         private final RenderSectionRegion region;

         public CompileTask(final RenderSectionRegion region, final boolean isRecompile) {
            super(isRecompile);
            this.region = region;
         }

         @Override
         public SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult doTask(final SectionBufferBuilderPack buffers) {
            if (this.isCancelled.get()) {
               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
            }

            long sectionNode = RenderSection.this.sectionNode;
            SectionPos sectionPos = SectionPos.of(sectionNode);
            if (this.isCancelled.get()) {
               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
            }

            Vec3 cameraPos = SectionRenderDispatcher.this.cameraPosition.get();

            SectionCompiler.Results results;
            try (Zone ignored = Profiler.get().zone("Compile Section")) {
               results = SectionRenderDispatcher.this.sectionCompiler
                  .compile(sectionPos, this.region, RenderSection.this.createVertexSorting(sectionPos, cameraPos), buffers);
            }

            TranslucencyPointOfView translucencyPointOfView = TranslucencyPointOfView.of(cameraPos, sectionNode);
            CompiledSectionMesh compiledSectionMesh = new CompiledSectionMesh(translucencyPointOfView, results);
            if (results.renderedLayers.isEmpty()) {
               SectionMesh oldMesh = RenderSection.this.setSectionMesh(compiledSectionMesh);
               SectionRenderDispatcher.this.copyLock.lock();

               try {
                  RenderSection.this.releaseSectionMesh(oldMesh);
               } finally {
                  SectionRenderDispatcher.this.copyLock.unlock();
               }

               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.SUCCESSFUL;
            } else {
               for (Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
                  MeshData meshData = entry.getValue();
                  boolean success = false;

                  while (!success) {
                     if (this.isCancelled.get()) {
                        results.release();
                        SectionRenderDispatcher.this.copyLock.lock();

                        try {
                           RenderSection.this.releaseSectionMesh(compiledSectionMesh);
                        } finally {
                           SectionRenderDispatcher.this.copyLock.unlock();
                        }

                        return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
                     }

                     success = RenderSection.this.addSectionBuffersToUberBuffer(
                        entry.getKey(), compiledSectionMesh, meshData.vertexBuffer(), meshData.indexBuffer()
                     );
                     if (!success && !RenderSystem.isOnRenderThread()) {
                        Thread.onSpinWait();
                     }
                  }

                  meshData.close();
               }

               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.SUCCESSFUL;
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.compareAndSet(false, true);
         }
      }

      private class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.SectionTask {
         private final CompiledSectionMesh compiledSectionMesh;

         public ResortTransparencyTask(final CompiledSectionMesh compiledSectionMesh) {
            super(true);
            this.compiledSectionMesh = compiledSectionMesh;
         }

         @Override
         public SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult doTask(final SectionBufferBuilderPack buffers) {
            if (this.isCancelled.get()) {
               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
            }

            MeshData.SortState state = this.compiledSectionMesh.getTransparencyState();
            if (state != null && !this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
               Vec3 cameraPos = SectionRenderDispatcher.this.cameraPosition.get();
               long sectionNode = RenderSection.this.sectionNode;
               VertexSorting vertexSorting = RenderSection.this.createVertexSorting(SectionPos.of(sectionNode), cameraPos);
               TranslucencyPointOfView translucencyPointOfView = TranslucencyPointOfView.of(cameraPos, sectionNode);
               if (!this.compiledSectionMesh.isDifferentPointOfView(translucencyPointOfView) && !translucencyPointOfView.isAxisAligned()) {
                  return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
               }

               ByteBufferBuilder.Result indexBuffer = state.buildSortedIndexBuffer(buffers.buffer(ChunkSectionLayer.TRANSLUCENT), vertexSorting);
               if (indexBuffer == null) {
                  return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
               }

               boolean success = false;

               while (!success) {
                  if (this.isCancelled.get()) {
                     indexBuffer.close();
                     return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
                  }

                  success = RenderSection.this.addSectionBuffersToUberBuffer(
                     ChunkSectionLayer.TRANSLUCENT, this.compiledSectionMesh, null, indexBuffer.byteBuffer()
                  );
                  if (!success && !RenderSystem.isOnRenderThread()) {
                     Thread.onSpinWait();
                  }
               }

               indexBuffer.close();
               this.compiledSectionMesh.setTranslucencyPointOfView(translucencyPointOfView);
               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.SUCCESSFUL;
            } else {
               return SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED;
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.set(true);
         }
      }

      public abstract class SectionTask {
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
         private final boolean isRecompile;

         public SectionTask(final boolean isRecompile) {
            this.isRecompile = isRecompile;
         }

         public abstract SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult doTask(final SectionBufferBuilderPack buffers);

         public abstract void cancel();

         public boolean isRecompile() {
            return this.isRecompile;
         }

         public BlockPos getRenderOrigin() {
            return RenderSection.this.renderOrigin;
         }

         public enum SectionTaskResult {
            SUCCESSFUL,
            CANCELLED;
         }
      }
   }

   public record RenderSectionBufferSlice(GpuBuffer vertexBuffer, long vertexBufferOffset, @Nullable GpuBuffer indexBuffer, long indexBufferOffset) {
   }

   private record SectionUberBuffers(UberGpuBuffer<SectionMesh> vertexBuffer, UberGpuBuffer<SectionMesh> indexBuffer) {
   }
}
