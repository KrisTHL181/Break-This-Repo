package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
   private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   final Executor mainThreadUploadExecutor = this.toUpload::add;
   final Queue<SectionMesh> toClose = Queues.newConcurrentLinkedQueue();
   final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   volatile boolean closed;
   private final ConsecutiveExecutor consecutiveExecutor;
   private final TracingExecutor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   Vec3 cameraPosition = Vec3.ZERO;
   final SectionCompiler sectionCompiler;

   public SectionRenderDispatcher(
      ClientLevel p_299878_,
      LevelRenderer p_299032_,
      TracingExecutor p_364436_,
      RenderBuffers p_310401_,
      BlockRenderDispatcher p_343142_,
      BlockEntityRenderDispatcher p_344654_
   ) {
      this.level = p_299878_;
      this.renderer = p_299032_;
      this.fixedBuffers = p_310401_.fixedBufferPack();
      this.bufferPool = p_310401_.sectionBufferPool();
      this.executor = p_364436_;
      this.consecutiveExecutor = new ConsecutiveExecutor(p_364436_, "Section Renderer");
      this.consecutiveExecutor.schedule(this::runTask);
      this.sectionCompiler = new SectionCompiler(p_343142_, p_344654_);
   }

   public void setLevel(ClientLevel p_298968_) {
      this.level = p_298968_;
   }

   private void runTask() {
      if (!this.closed && !this.bufferPool.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.cameraPosition);
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
            CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                  () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                  this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
               )
               .thenCompose(p_298155_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_298155_)
               .whenComplete((p_357938_, p_357939_) -> {
                  if (p_357939_ != null) {
                     Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_357939_, "Batching sections"));
                  } else {
                     sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                     this.consecutiveExecutor.schedule(() -> {
                        if (p_357938_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                           sectionbufferbuilderpack.clearAll();
                        } else {
                           sectionbufferbuilderpack.discardAll();
                        }

                        this.bufferPool.release(sectionbufferbuilderpack);
                        this.runTask();
                     });
                  }
               });
         }
      }
   }

   public void setCameraPosition(Vec3 p_407405_) {
      this.cameraPosition = p_407405_;
   }

   public void uploadAllPendingUploads() {
      Runnable runnable;
      while ((runnable = this.toUpload.poll()) != null) {
         runnable.run();
      }

      SectionMesh sectionmesh;
      while ((sectionmesh = this.toClose.poll()) != null) {
         sectionmesh.close();
      }
   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection p_299640_, RenderRegionCache p_297835_) {
      p_299640_.compileSync(p_297835_);
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask p_297747_) {
      if (!this.closed) {
         this.consecutiveExecutor.schedule(() -> {
            if (!this.closed) {
               this.compileQueue.add(p_297747_);
               this.runTask();
            }
         });
      }
   }

   public void clearCompileQueue() {
      this.compileQueue.clear();
   }

   public boolean isQueueEmpty() {
      return this.compileQueue.size() == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.closed = true;
      this.clearCompileQueue();
      this.uploadAllPendingUploads();
   }

   @VisibleForDebug
   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.compileQueue.size(), this.toUpload.size(), this.bufferPool.getFreeBufferCount());
   }

   @VisibleForDebug
   public int getCompileQueueSize() {
      return this.compileQueue.size();
   }

   @VisibleForDebug
   public int getToUpload() {
      return this.toUpload.size();
   }

   @VisibleForDebug
   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   @OnlyIn(Dist.CLIENT)
   public class RenderSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionMesh> sectionMesh = new AtomicReference<>(CompiledSectionMesh.UNCOMPILED);
      private SectionRenderDispatcher.RenderSection.@Nullable RebuildTask lastRebuildTask;
      private SectionRenderDispatcher.RenderSection.@Nullable ResortTransparencyTask lastResortTransparencyTask;
      private AABB bb;
      private boolean dirty = true;
      volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
      final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private boolean playerChanged;
      private long uploadedTime;
      private long fadeDuration;
      private boolean wasPreviouslyEmpty;

      public RenderSection(final int p_299358_, final long p_366281_) {
         this.index = p_299358_;
         this.setSectionNode(p_366281_);
      }

      public float getVisibility(long p_451612_) {
         long i = p_451612_ - this.uploadedTime;
         return i >= this.fadeDuration ? 1.0F : (float)i / (float)this.fadeDuration;
      }

      public void setFadeDuration(long p_460134_) {
         this.fadeDuration = p_460134_;
      }

      public void setWasPreviouslyEmpty(boolean p_453036_) {
         this.wasPreviouslyEmpty = p_453036_;
      }

      public boolean wasPreviouslyEmpty() {
         return this.wasPreviouslyEmpty;
      }

      private boolean doesChunkExistAt(long p_366776_) {
         ChunkAccess chunkaccess = SectionRenderDispatcher.this.level.getChunk(SectionPos.x(p_366776_), SectionPos.z(p_366776_), ChunkStatus.FULL, false);
         return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(p_366776_));
      }

      public boolean hasAllNeighbors() {
         return this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.WEST))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.NORTH))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.EAST))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.SOUTH))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, -1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, 1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, -1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, 1));
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public CompletableFuture<Void> upload(Map<ChunkSectionLayer, MeshData> p_409621_, CompiledSectionMesh p_409834_) {
         if (SectionRenderDispatcher.this.closed) {
            p_409621_.values().forEach(MeshData::close);
            return CompletableFuture.completedFuture(null);
         } else {
            return CompletableFuture.runAsync(() -> p_409621_.forEach((p_448222_, p_448223_) -> {
               try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                  p_409834_.uploadMeshLayer(p_448222_, p_448223_, this.sectionNode);
                  p_448223_.close();
               }

               if (this.uploadedTime == 0L) {
                  this.uploadedTime = Util.getMillis();
               }
            }), SectionRenderDispatcher.this.mainThreadUploadExecutor);
         }
      }

      public CompletableFuture<Void> uploadSectionIndexBuffer(CompiledSectionMesh p_410735_, ByteBufferBuilder.Result p_393953_, ChunkSectionLayer p_406199_) {
         if (SectionRenderDispatcher.this.closed) {
            p_393953_.close();
            return CompletableFuture.completedFuture(null);
         } else {
            return CompletableFuture.runAsync(() -> {
               try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                  p_410735_.uploadLayerIndexBuffer(p_406199_, p_393953_, this.sectionNode);
                  p_393953_.close();
               }
            }, SectionRenderDispatcher.this.mainThreadUploadExecutor);
         }
      }

      public void setSectionNode(long p_360921_) {
         this.reset();
         this.sectionNode = p_360921_;
         int i = SectionPos.sectionToBlockCoord(SectionPos.x(p_360921_));
         int j = SectionPos.sectionToBlockCoord(SectionPos.y(p_360921_));
         int k = SectionPos.sectionToBlockCoord(SectionPos.z(p_360921_));
         this.renderOrigin.set(i, j, k);
         this.bb = new AABB(i, j, k, i + 16, j + 16, k + 16);
      }

      public SectionMesh getSectionMesh() {
         return this.sectionMesh.get();
      }

      public void reset() {
         this.cancelTasks();
         this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED).close();
         this.dirty = true;
         this.uploadedTime = 0L;
         this.wasPreviouslyEmpty = false;
      }

      public BlockPos getRenderOrigin() {
         return this.renderOrigin;
      }

      public long getSectionNode() {
         return this.sectionNode;
      }

      public void setDirty(boolean p_298731_) {
         boolean flag = this.dirty;
         this.dirty = true;
         this.playerChanged = p_298731_ | (flag && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public long getNeighborSectionNode(Direction p_362694_) {
         return SectionPos.offset(this.sectionNode, p_362694_);
      }

      public void resortTransparency(SectionRenderDispatcher p_298196_) {
         if (this.getSectionMesh() instanceof CompiledSectionMesh compiledsectionmesh) {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(compiledsectionmesh);
            p_298196_.schedule(this.lastResortTransparencyTask);
         }
      }

      public boolean hasTranslucentGeometry() {
         return this.getSectionMesh().hasTranslucentGeometry();
      }

      public boolean transparencyResortingScheduled() {
         return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
      }

      protected void cancelTasks() {
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache p_300037_) {
         this.cancelTasks();
         RenderSectionRegion rendersectionregion = p_300037_.createRegion(SectionRenderDispatcher.this.level, this.sectionNode);
         boolean flag = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(rendersectionregion, flag);
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(RenderRegionCache p_297331_) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_297331_);
         SectionRenderDispatcher.this.schedule(sectionrenderdispatcher$rendersection$compiletask);
      }

      public void compileSync(RenderRegionCache p_298605_) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_298605_);
         sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      void setSectionMesh(SectionMesh p_408721_) {
         SectionMesh sectionmesh = this.sectionMesh.getAndSet(p_408721_);
         SectionRenderDispatcher.this.toClose.add(sectionmesh);
         SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
      }

      VertexSorting createVertexSorting(SectionPos p_393405_) {
         Vec3 vec3 = SectionRenderDispatcher.this.cameraPosition;
         return VertexSorting.byDistance(
            (float)(vec3.x - p_393405_.minBlockX()), (float)(vec3.y - p_393405_.minBlockY()), (float)(vec3.z - p_393405_.minBlockZ())
         );
      }

      @OnlyIn(Dist.CLIENT)
      public abstract class CompileTask {
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
         protected final boolean isRecompile;

         public CompileTask(final boolean p_299251_) {
            this.isRecompile = p_299251_;
         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack var1);

         public abstract void cancel();

         protected abstract String name();

         public boolean isRecompile() {
            return this.isRecompile;
         }

         public BlockPos getRenderOrigin() {
            return RenderSection.this.renderOrigin;
         }
      }

      @OnlyIn(Dist.CLIENT)
      class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         protected final RenderSectionRegion region;

         public RebuildTask(final RenderSectionRegion p_410538_, final boolean p_299891_) {
            super(p_299891_);
            this.region = p_410538_;
         }

         @Override
         protected String name() {
            return "rend_chk_rebuild";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_299595_) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            }

            long i = RenderSection.this.sectionNode;
            SectionPos sectionpos = SectionPos.of(i);
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            }

            SectionCompiler.Results sectioncompiler$results;
            try (Zone zone = Profiler.get().zone("Compile Section")) {
               sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                  .compile(sectionpos, this.region, RenderSection.this.createVertexSorting(sectionpos), p_299595_);
            }

            TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
            if (this.isCancelled.get()) {
               sectioncompiler$results.release();
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               CompiledSectionMesh compiledsectionmesh = new CompiledSectionMesh(translucencypointofview, sectioncompiler$results);
               CompletableFuture<Void> completablefuture = RenderSection.this.upload(sectioncompiler$results.renderedLayers, compiledsectionmesh);
               return completablefuture.handle((p_405000_, p_405001_) -> {
                  if (p_405001_ != null && !(p_405001_ instanceof CancellationException) && !(p_405001_ instanceof InterruptedException)) {
                     Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_405001_, "Rendering section"));
                  }

                  if (!this.isCancelled.get() && !SectionRenderDispatcher.this.closed) {
                     RenderSection.this.setSectionMesh(compiledsectionmesh);
                     return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                  } else {
                     SectionRenderDispatcher.this.toClose.add(compiledsectionmesh);
                     return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                  }
               });
            }
         }

         @Override
         public void cancel() {
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final CompiledSectionMesh compiledSectionMesh;

         public ResortTransparencyTask(final CompiledSectionMesh p_407319_) {
            super(true);
            this.compiledSectionMesh = p_407319_;
         }

         @Override
         protected String name() {
            return "rend_chk_sort";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_297366_) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            }

            MeshData.SortState meshdata$sortstate = this.compiledSectionMesh.getTransparencyState();
            if (meshdata$sortstate != null && !this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
               long i = RenderSection.this.sectionNode;
               VertexSorting vertexsorting = RenderSection.this.createVertexSorting(SectionPos.of(i));
               TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
               if (!this.compiledSectionMesh.isDifferentPointOfView(translucencypointofview) && !translucencypointofview.isAxisAligned()) {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               } else {
                  ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                     p_297366_.buffer(ChunkSectionLayer.TRANSLUCENT), vertexsorting
                  );
                  if (bytebufferbuilder$result == null) {
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else if (this.isCancelled.get()) {
                     bytebufferbuilder$result.close();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompletableFuture<Void> completablefuture = RenderSection.this.uploadSectionIndexBuffer(
                        this.compiledSectionMesh, bytebufferbuilder$result, ChunkSectionLayer.TRANSLUCENT
                     );
                     return completablefuture.handle((p_405003_, p_405004_) -> {
                        if (p_405004_ != null && !(p_405004_ instanceof CancellationException) && !(p_405004_ instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_405004_, "Rendering section"));
                        }

                        if (this.isCancelled.get()) {
                           return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                        }

                        this.compiledSectionMesh.setTranslucencyPointOfView(translucencypointofview);
                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     });
                  }
               }
            } else {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }
}
