package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.jspecify.annotations.Nullable;

public abstract class GenerationChunkHolder {
   private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
   private static final ChunkResult<ChunkAccess> NOT_DONE_YET = ChunkResult.error("Not done yet");
   public static final ChunkResult<ChunkAccess> UNLOADED_CHUNK = ChunkResult.error("Unloaded chunk");
   public static final CompletableFuture<ChunkResult<ChunkAccess>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK);
   protected final ChunkPos pos;
   private volatile @Nullable ChunkStatus highestAllowedStatus;
   private final AtomicReference<@Nullable ChunkStatus> startedWork = new AtomicReference<>();
   private final AtomicReferenceArray<@Nullable CompletableFuture<ChunkResult<ChunkAccess>>> futures = new AtomicReferenceArray<>(CHUNK_STATUSES.size());
   private final AtomicReference<@Nullable ChunkGenerationTask> task = new AtomicReference<>();
   private final AtomicInteger generationRefCount = new AtomicInteger();
   private volatile CompletableFuture<Void> generationSaveSyncFuture = CompletableFuture.completedFuture(null);

   public GenerationChunkHolder(ChunkPos p_342238_) {
      this.pos = p_342238_;
      if (!p_342238_.isValid()) {
         throw new IllegalStateException("Trying to create chunk out of reasonable bounds: " + p_342238_);
      }
   }

   public CompletableFuture<ChunkResult<ChunkAccess>> scheduleChunkGenerationTask(ChunkStatus p_342080_, ChunkMap p_343602_) {
      if (this.isStatusDisallowed(p_342080_)) {
         return UNLOADED_CHUNK_FUTURE;
      }

      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.getOrCreateFuture(p_342080_);
      if (completablefuture.isDone()) {
         return completablefuture;
      }

      ChunkGenerationTask chunkgenerationtask = this.task.get();
      if (chunkgenerationtask == null || p_342080_.isAfter(chunkgenerationtask.targetStatus)) {
         this.rescheduleChunkTask(p_343602_, p_342080_);
      }

      return completablefuture;
   }

   CompletableFuture<ChunkResult<ChunkAccess>> applyStep(ChunkStep p_344844_, GeneratingChunkMap p_342173_, StaticCache2D<GenerationChunkHolder> p_343026_) {
      if (this.isStatusDisallowed(p_344844_.targetStatus())) {
         return UNLOADED_CHUNK_FUTURE;
      } else {
         return this.acquireStatusBump(p_344844_.targetStatus()) ? p_342173_.applyStep(this, p_344844_, p_343026_).handle((p_343850_, p_344393_) -> {
            if (p_344393_ != null) {
               CrashReport crashreport = CrashReport.forThrowable(p_344393_, "Exception chunk generation/loading");
               MinecraftServer.setFatalException(new ReportedException(crashreport));
            } else {
               this.completeFuture(p_344844_.targetStatus(), p_343850_);
            }

            return ChunkResult.of(p_343850_);
         }) : this.getOrCreateFuture(p_344844_.targetStatus());
      }
   }

   protected void updateHighestAllowedStatus(ChunkMap p_345117_) {
      ChunkStatus chunkstatus = this.highestAllowedStatus;
      ChunkStatus chunkstatus1 = ChunkLevel.generationStatus(this.getTicketLevel());
      this.highestAllowedStatus = chunkstatus1;
      boolean flag = chunkstatus != null && (chunkstatus1 == null || chunkstatus1.isBefore(chunkstatus));
      if (flag) {
         this.failAndClearPendingFuturesBetween(chunkstatus1, chunkstatus);
         if (this.task.get() != null) {
            this.rescheduleChunkTask(p_345117_, this.findHighestStatusWithPendingFuture(chunkstatus1));
         }
      }
   }

   public void replaceProtoChunk(ImposterProtoChunk p_343560_) {
      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = CompletableFuture.completedFuture(ChunkResult.of(p_343560_));

      for (int i = 0; i < this.futures.length() - 1; i++) {
         CompletableFuture<ChunkResult<ChunkAccess>> completablefuture1 = this.futures.get(i);
         Objects.requireNonNull(completablefuture1);
         ChunkAccess chunkaccess = completablefuture1.getNow(NOT_DONE_YET).orElse(null);
         if (!(chunkaccess instanceof ProtoChunk)) {
            throw new IllegalStateException("Trying to replace a ProtoChunk, but found " + chunkaccess);
         }

         if (!this.futures.compareAndSet(i, completablefuture1, completablefuture)) {
            throw new IllegalStateException("Future changed by other thread while trying to replace it");
         }
      }
   }

   void removeTask(ChunkGenerationTask p_342447_) {
      this.task.compareAndSet(p_342447_, null);
   }

   private void rescheduleChunkTask(ChunkMap p_344344_, @Nullable ChunkStatus p_343189_) {
      ChunkGenerationTask chunkgenerationtask;
      if (p_343189_ != null) {
         chunkgenerationtask = p_344344_.scheduleGenerationTask(p_343189_, this.getPos());
      } else {
         chunkgenerationtask = null;
      }

      ChunkGenerationTask chunkgenerationtask1 = this.task.getAndSet(chunkgenerationtask);
      if (chunkgenerationtask1 != null) {
         chunkgenerationtask1.markForCancellation();
      }
   }

   private CompletableFuture<ChunkResult<ChunkAccess>> getOrCreateFuture(ChunkStatus p_342279_) {
      if (this.isStatusDisallowed(p_342279_)) {
         return UNLOADED_CHUNK_FUTURE;
      }

      int i = p_342279_.getIndex();
      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(i);

      while (completablefuture == null) {
         CompletableFuture<ChunkResult<ChunkAccess>> completablefuture1 = new CompletableFuture<>();
         completablefuture = this.futures.compareAndExchange(i, null, completablefuture1);
         if (completablefuture == null) {
            if (this.isStatusDisallowed(p_342279_)) {
               this.failAndClearPendingFuture(i, completablefuture1);
               return UNLOADED_CHUNK_FUTURE;
            }

            return completablefuture1;
         }
      }

      return completablefuture;
   }

   private void failAndClearPendingFuturesBetween(@Nullable ChunkStatus p_343092_, ChunkStatus p_345118_) {
      int i = p_343092_ == null ? 0 : p_343092_.getIndex() + 1;
      int j = p_345118_.getIndex();

      for (int k = i; k <= j; k++) {
         CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(k);
         if (completablefuture != null) {
            this.failAndClearPendingFuture(k, completablefuture);
         }
      }
   }

   private void failAndClearPendingFuture(int p_342343_, CompletableFuture<ChunkResult<ChunkAccess>> p_345346_) {
      if (p_345346_.complete(UNLOADED_CHUNK) && !this.futures.compareAndSet(p_342343_, p_345346_, null)) {
         throw new IllegalStateException("Nothing else should replace the future here");
      }
   }

   private void completeFuture(ChunkStatus p_342456_, ChunkAccess p_342625_) {
      ChunkResult<ChunkAccess> chunkresult = ChunkResult.of(p_342625_);
      int i = p_342456_.getIndex();

      while (true) {
         CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(i);
         if (completablefuture == null) {
            if (this.futures.compareAndSet(i, null, CompletableFuture.completedFuture(chunkresult))) {
               return;
            }
         } else {
            if (completablefuture.complete(chunkresult)) {
               return;
            }

            if (completablefuture.getNow(NOT_DONE_YET).isSuccess()) {
               throw new IllegalStateException("Trying to complete a future but found it to be completed successfully already");
            }

            Thread.yield();
         }
      }
   }

   private @Nullable ChunkStatus findHighestStatusWithPendingFuture(@Nullable ChunkStatus p_342852_) {
      if (p_342852_ == null) {
         return null;
      }

      ChunkStatus chunkstatus = p_342852_;

      for (ChunkStatus chunkstatus1 = this.startedWork.get(); chunkstatus1 == null || chunkstatus.isAfter(chunkstatus1); chunkstatus = chunkstatus.getParent()) {
         if (this.futures.get(chunkstatus.getIndex()) != null) {
            return chunkstatus;
         }

         if (chunkstatus == ChunkStatus.EMPTY) {
            break;
         }
      }

      return null;
   }

   private boolean acquireStatusBump(ChunkStatus p_343283_) {
      ChunkStatus chunkstatus = p_343283_ == ChunkStatus.EMPTY ? null : p_343283_.getParent();
      ChunkStatus chunkstatus1 = this.startedWork.compareAndExchange(chunkstatus, p_343283_);
      if (chunkstatus1 == chunkstatus) {
         return true;
      } else if (chunkstatus1 != null && !p_343283_.isAfter(chunkstatus1)) {
         return false;
      } else {
         throw new IllegalStateException("Unexpected last startedWork status: " + chunkstatus1 + " while trying to start: " + p_343283_);
      }
   }

   private boolean isStatusDisallowed(ChunkStatus p_342117_) {
      ChunkStatus chunkstatus = this.highestAllowedStatus;
      return chunkstatus == null || p_342117_.isAfter(chunkstatus);
   }

   protected abstract void addSaveDependency(CompletableFuture<?> var1);

   public void increaseGenerationRefCount() {
      if (this.generationRefCount.getAndIncrement() == 0) {
         this.generationSaveSyncFuture = new CompletableFuture<>();
         this.addSaveDependency(this.generationSaveSyncFuture);
      }
   }

   public void decreaseGenerationRefCount() {
      CompletableFuture<Void> completablefuture = this.generationSaveSyncFuture;
      int i = this.generationRefCount.decrementAndGet();
      if (i == 0) {
         completablefuture.complete(null);
      }

      if (i < 0) {
         throw new IllegalStateException("More releases than claims. Count: " + i);
      }
   }

   public @Nullable ChunkAccess getChunkIfPresentUnchecked(ChunkStatus p_342422_) {
      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(p_342422_.getIndex());
      return completablefuture == null ? null : completablefuture.getNow(NOT_DONE_YET).orElse(null);
   }

   public @Nullable ChunkAccess getChunkIfPresent(ChunkStatus p_342964_) {
      return this.isStatusDisallowed(p_342964_) ? null : this.getChunkIfPresentUnchecked(p_342964_);
   }

   public @Nullable ChunkAccess getLatestChunk() {
      ChunkStatus chunkstatus = this.startedWork.get();
      if (chunkstatus == null) {
         return null;
      }

      ChunkAccess chunkaccess = this.getChunkIfPresentUnchecked(chunkstatus);
      return chunkaccess != null ? chunkaccess : this.getChunkIfPresentUnchecked(chunkstatus.getParent());
   }

   public @Nullable ChunkStatus getPersistedStatus() {
      CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(ChunkStatus.EMPTY.getIndex());
      ChunkAccess chunkaccess = completablefuture == null ? null : completablefuture.getNow(NOT_DONE_YET).orElse(null);
      return chunkaccess == null ? null : chunkaccess.getPersistedStatus();
   }

   public ChunkPos getPos() {
      return this.pos;
   }

   public FullChunkStatus getFullStatus() {
      return ChunkLevel.fullStatus(this.getTicketLevel());
   }

   public abstract int getTicketLevel();

   public abstract int getQueueLevel();

   @VisibleForDebug
   public List<Pair<ChunkStatus, @Nullable CompletableFuture<ChunkResult<ChunkAccess>>>> getAllFutures() {
      List<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>> list = new ArrayList<>();

      for (int i = 0; i < CHUNK_STATUSES.size(); i++) {
         list.add(Pair.of(CHUNK_STATUSES.get(i), this.futures.get(i)));
      }

      return list;
   }

   @VisibleForDebug
   public @Nullable ChunkStatus getLatestStatus() {
      ChunkStatus chunkstatus = this.startedWork.get();
      if (chunkstatus == null) {
         return null;
      }

      ChunkAccess chunkaccess = this.getChunkIfPresentUnchecked(chunkstatus);
      return chunkaccess != null ? chunkstatus : chunkstatus.getParent();
   }
}
