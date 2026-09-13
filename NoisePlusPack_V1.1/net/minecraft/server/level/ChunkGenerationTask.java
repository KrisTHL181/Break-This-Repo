package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class ChunkGenerationTask {
   private final GeneratingChunkMap chunkMap;
   private final ChunkPos pos;
   private @Nullable ChunkStatus scheduledStatus = null;
   public final ChunkStatus targetStatus;
   private volatile boolean markedForCancellation;
   private final List<CompletableFuture<ChunkResult<ChunkAccess>>> scheduledLayer = new ArrayList<>();
   private final StaticCache2D<GenerationChunkHolder> cache;
   private boolean needsGeneration;

   private ChunkGenerationTask(GeneratingChunkMap p_344029_, ChunkStatus p_344351_, ChunkPos p_344140_, StaticCache2D<GenerationChunkHolder> p_343399_) {
      this.chunkMap = p_344029_;
      this.targetStatus = p_344351_;
      this.pos = p_344140_;
      this.cache = p_343399_;
   }

   public static ChunkGenerationTask create(GeneratingChunkMap p_344659_, ChunkStatus p_344444_, ChunkPos p_342415_) {
      int i = ChunkPyramid.GENERATION_PYRAMID.getStepTo(p_344444_).getAccumulatedRadiusOf(ChunkStatus.EMPTY);
      StaticCache2D<GenerationChunkHolder> staticcache2d = StaticCache2D.create(
         p_342415_.x, p_342415_.z, i, (p_342548_, p_344508_) -> p_344659_.acquireGeneration(ChunkPos.asLong(p_342548_, p_344508_))
      );
      return new ChunkGenerationTask(p_344659_, p_344444_, p_342415_, staticcache2d);
   }

   public @Nullable CompletableFuture<?> runUntilWait() {
      while (true) {
         CompletableFuture<?> completablefuture = this.waitForScheduledLayer();
         if (completablefuture != null) {
            return completablefuture;
         }

         if (this.markedForCancellation || this.scheduledStatus == this.targetStatus) {
            this.releaseClaim();
            return null;
         }

         this.scheduleNextLayer();
      }
   }

   private void scheduleNextLayer() {
      ChunkStatus chunkstatus;
      if (this.scheduledStatus == null) {
         chunkstatus = ChunkStatus.EMPTY;
      } else if (!this.needsGeneration && this.scheduledStatus == ChunkStatus.EMPTY && !this.canLoadWithoutGeneration()) {
         this.needsGeneration = true;
         chunkstatus = ChunkStatus.EMPTY;
      } else {
         chunkstatus = ChunkStatus.getStatusList().get(this.scheduledStatus.getIndex() + 1);
      }

      this.scheduleLayer(chunkstatus, this.needsGeneration);
      this.scheduledStatus = chunkstatus;
   }

   public void markForCancellation() {
      this.markedForCancellation = true;
   }

   private void releaseClaim() {
      GenerationChunkHolder generationchunkholder = this.cache.get(this.pos.x, this.pos.z);
      generationchunkholder.removeTask(this);
      this.cache.forEach(this.chunkMap::releaseGeneration);
   }

   private boolean canLoadWithoutGeneration() {
      if (this.targetStatus == ChunkStatus.EMPTY) {
         return true;
      }

      ChunkStatus chunkstatus = this.cache.get(this.pos.x, this.pos.z).getPersistedStatus();
      if (chunkstatus != null && !chunkstatus.isBefore(this.targetStatus)) {
         ChunkDependencies chunkdependencies = ChunkPyramid.LOADING_PYRAMID.getStepTo(this.targetStatus).accumulatedDependencies();
         int i = chunkdependencies.getRadius();

         for (int j = this.pos.x - i; j <= this.pos.x + i; j++) {
            for (int k = this.pos.z - i; k <= this.pos.z + i; k++) {
               int l = this.pos.getChessboardDistance(j, k);
               ChunkStatus chunkstatus1 = chunkdependencies.get(l);
               ChunkStatus chunkstatus2 = this.cache.get(j, k).getPersistedStatus();
               if (chunkstatus2 == null || chunkstatus2.isBefore(chunkstatus1)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public GenerationChunkHolder getCenter() {
      return this.cache.get(this.pos.x, this.pos.z);
   }

   private void scheduleLayer(ChunkStatus p_342139_, boolean p_342359_) {
      try (Zone zone = Profiler.get().zone("scheduleLayer")) {
         zone.addText(p_342139_::getName);
         int i = this.getRadiusForLayer(p_342139_, p_342359_);

         for (int j = this.pos.x - i; j <= this.pos.x + i; j++) {
            for (int k = this.pos.z - i; k <= this.pos.z + i; k++) {
               GenerationChunkHolder generationchunkholder = this.cache.get(j, k);
               if (this.markedForCancellation || !this.scheduleChunkInLayer(p_342139_, p_342359_, generationchunkholder)) {
                  return;
               }
            }
         }
      }
   }

   private int getRadiusForLayer(ChunkStatus p_343532_, boolean p_343456_) {
      ChunkPyramid chunkpyramid = p_343456_ ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
      return chunkpyramid.getStepTo(this.targetStatus).getAccumulatedRadiusOf(p_343532_);
   }

   private boolean scheduleChunkInLayer(ChunkStatus p_342275_, boolean p_344389_, GenerationChunkHolder p_343540_) {
      ChunkStatus chunkstatus = p_343540_.getPersistedStatus();
      boolean flag = chunkstatus != null && p_342275_.isAfter(chunkstatus);
      ChunkPyramid chunkpyramid = flag ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
      if (flag && !p_344389_) {
         throw new IllegalStateException("Can't load chunk, but didn't expect to need to generate");
      }

      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = p_343540_.applyStep(chunkpyramid.getStepTo(p_342275_), this.chunkMap, this.cache);
      ChunkResult<ChunkAccess> chunkresult = completablefuture.getNow(null);
      if (chunkresult == null) {
         this.scheduledLayer.add(completablefuture);
         return true;
      }

      if (chunkresult.isSuccess()) {
         return true;
      }

      this.markForCancellation();
      return false;
   }

   private @Nullable CompletableFuture<?> waitForScheduledLayer() {
      while (!this.scheduledLayer.isEmpty()) {
         CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.scheduledLayer.getLast();
         ChunkResult<ChunkAccess> chunkresult = completablefuture.getNow(null);
         if (chunkresult == null) {
            return completablefuture;
         }

         this.scheduledLayer.removeLast();
         if (!chunkresult.isSuccess()) {
            this.markForCancellation();
         }
      }

      return null;
   }
}
