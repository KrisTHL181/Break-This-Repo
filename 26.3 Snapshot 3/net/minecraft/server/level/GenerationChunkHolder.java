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
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.thread.BlockableEventLoop;
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

   public GenerationChunkHolder(final ChunkPos pos) {
      this.pos = pos;
      if (!pos.isValid()) {
         throw new IllegalStateException("Trying to create chunk out of reasonable bounds: " + pos);
      }
   }

   public CompletableFuture<ChunkResult<ChunkAccess>> scheduleChunkGenerationTask(final ChunkStatus status, final ChunkMap scheduler) {
      if (this.isStatusDisallowed(status)) {
         return UNLOADED_CHUNK_FUTURE;
      }

      CompletableFuture<ChunkResult<ChunkAccess>> future = this.getOrCreateFuture(status);
      if (future.isDone()) {
         return future;
      }

      ChunkGenerationTask task = this.task.get();
      if (task == null || status.isAfter(task.targetStatus)) {
         this.rescheduleChunkTask(scheduler, status);
      }

      return future;
   }

   CompletableFuture<ChunkResult<ChunkAccess>> applyStep(
      final ChunkStep step, final GeneratingChunkMap chunkMap, final StaticCache2D<GenerationChunkHolder> cache
   ) {
      if (this.isStatusDisallowed(step.targetStatus())) {
         return UNLOADED_CHUNK_FUTURE;
      } else {
         return this.acquireStatusBump(step.targetStatus()) ? chunkMap.applyStep(this, step, cache).handle((chunk, exception) -> {
            if (exception != null) {
               CrashReport report = CrashReport.forThrowable(exception, "Exception chunk generation/loading");
               BlockableEventLoop.relayDelayCrash(report);
            } else {
               this.completeFuture(step.targetStatus(), chunk);
            }

            return ChunkResult.of(chunk);
         }) : this.getOrCreateFuture(step.targetStatus());
      }
   }

   protected void updateHighestAllowedStatus(final ChunkMap scheduler) {
      ChunkStatus oldStatus = this.highestAllowedStatus;
      ChunkStatus newStatus = ChunkLevel.generationStatus(this.getTicketLevel());
      this.highestAllowedStatus = newStatus;
      boolean statusDropped = oldStatus != null && (newStatus == null || newStatus.isBefore(oldStatus));
      if (statusDropped) {
         this.failAndClearPendingFuturesBetween(newStatus, oldStatus);
         if (this.task.get() != null) {
            this.rescheduleChunkTask(scheduler, this.findHighestStatusWithPendingFuture(newStatus));
         }
      }
   }

   public void replaceProtoChunk(final ImposterProtoChunk chunk) {
      CompletableFuture<ChunkResult<ChunkAccess>> imposterFuture = CompletableFuture.completedFuture(ChunkResult.of(chunk));

      for (int i = 0; i < this.futures.length() - 1; i++) {
         CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(i);
         Objects.requireNonNull(future);
         ChunkAccess maybeProtoChunk = future.getNow(NOT_DONE_YET).orElse(null);
         if (!(maybeProtoChunk instanceof ProtoChunk)) {
            throw new IllegalStateException("Trying to replace a ProtoChunk, but found " + maybeProtoChunk);
         }

         if (!this.futures.compareAndSet(i, future, imposterFuture)) {
            throw new IllegalStateException("Future changed by other thread while trying to replace it");
         }
      }
   }

   void removeTask(final ChunkGenerationTask task) {
      this.task.compareAndSet(task, null);
   }

   private void rescheduleChunkTask(final ChunkMap scheduler, final @Nullable ChunkStatus status) {
      ChunkGenerationTask newTask;
      if (status != null) {
         newTask = scheduler.scheduleGenerationTask(status, this.getPos());
      } else {
         newTask = null;
      }

      ChunkGenerationTask oldTask = this.task.getAndSet(newTask);
      if (oldTask != null) {
         oldTask.markForCancellation();
      }
   }

   private CompletableFuture<ChunkResult<ChunkAccess>> getOrCreateFuture(final ChunkStatus status) {
      if (this.isStatusDisallowed(status)) {
         return UNLOADED_CHUNK_FUTURE;
      }

      int index = status.getIndex();
      CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(index);

      while (future == null) {
         CompletableFuture<ChunkResult<ChunkAccess>> newValue = new CompletableFuture<>();
         future = this.futures.compareAndExchange(index, null, newValue);
         if (future == null) {
            if (this.isStatusDisallowed(status)) {
               this.failAndClearPendingFuture(index, newValue);
               return UNLOADED_CHUNK_FUTURE;
            }

            return newValue;
         }
      }

      return future;
   }

   private void failAndClearPendingFuturesBetween(final @Nullable ChunkStatus fromExclusive, final ChunkStatus toInclusive) {
      int start = fromExclusive == null ? 0 : fromExclusive.getIndex() + 1;
      int end = toInclusive.getIndex();

      for (int i = start; i <= end; i++) {
         CompletableFuture<ChunkResult<ChunkAccess>> previous = this.futures.get(i);
         if (previous != null) {
            this.failAndClearPendingFuture(i, previous);
         }
      }
   }

   private void failAndClearPendingFuture(final int index, final CompletableFuture<ChunkResult<ChunkAccess>> previous) {
      if (previous.complete(UNLOADED_CHUNK) && !this.futures.compareAndSet(index, previous, null)) {
         throw new IllegalStateException("Nothing else should replace the future here");
      }
   }

   private void completeFuture(final ChunkStatus status, final ChunkAccess chunk) {
      ChunkResult<ChunkAccess> result = ChunkResult.of(chunk);
      int index = status.getIndex();

      while (true) {
         CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(index);
         if (future == null) {
            if (this.futures.compareAndSet(index, null, CompletableFuture.completedFuture(result))) {
               return;
            }
         } else {
            if (future.complete(result)) {
               return;
            }

            if (future.getNow(NOT_DONE_YET).isSuccess()) {
               throw new IllegalStateException("Trying to complete a future but found it to be completed successfully already");
            }

            Thread.yield();
         }
      }
   }

   private @Nullable ChunkStatus findHighestStatusWithPendingFuture(final @Nullable ChunkStatus newStatus) {
      if (newStatus == null) {
         return null;
      }

      ChunkStatus highestStatus = newStatus;

      for (ChunkStatus alreadyStarted = this.startedWork.get();
         alreadyStarted == null || highestStatus.isAfter(alreadyStarted);
         highestStatus = highestStatus.getParent()
      ) {
         if (this.futures.get(highestStatus.getIndex()) != null) {
            return highestStatus;
         }

         if (highestStatus == ChunkStatus.EMPTY) {
            break;
         }
      }

      return null;
   }

   private boolean acquireStatusBump(final ChunkStatus status) {
      ChunkStatus parent = status == ChunkStatus.EMPTY ? null : status.getParent();
      ChunkStatus previousStarted = this.startedWork.compareAndExchange(parent, status);
      if (previousStarted == parent) {
         return true;
      } else if (previousStarted != null && !status.isAfter(previousStarted)) {
         return false;
      } else {
         throw new IllegalStateException("Unexpected last startedWork status: " + previousStarted + " while trying to start: " + status);
      }
   }

   private boolean isStatusDisallowed(final ChunkStatus status) {
      ChunkStatus highestAllowedStatus = this.highestAllowedStatus;
      return highestAllowedStatus == null || status.isAfter(highestAllowedStatus);
   }

   protected abstract void addSaveDependency(final CompletableFuture<?> sync);

   public void increaseGenerationRefCount() {
      if (this.generationRefCount.getAndIncrement() == 0) {
         this.generationSaveSyncFuture = new CompletableFuture<>();
         this.addSaveDependency(this.generationSaveSyncFuture);
      }
   }

   public void decreaseGenerationRefCount() {
      CompletableFuture<Void> future = this.generationSaveSyncFuture;
      int newValue = this.generationRefCount.decrementAndGet();
      if (newValue == 0) {
         future.complete(null);
      }

      if (newValue < 0) {
         throw new IllegalStateException("More releases than claims. Count: " + newValue);
      }
   }

   public @Nullable ChunkAccess getChunkIfPresentUnchecked(final ChunkStatus status) {
      CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(status.getIndex());
      return future == null ? null : future.getNow(NOT_DONE_YET).orElse(null);
   }

   public @Nullable ChunkAccess getChunkIfPresent(final ChunkStatus status) {
      return this.isStatusDisallowed(status) ? null : this.getChunkIfPresentUnchecked(status);
   }

   public @Nullable ChunkAccess getLatestChunk() {
      ChunkStatus status = this.startedWork.get();
      if (status == null) {
         return null;
      }

      ChunkAccess chunk = this.getChunkIfPresentUnchecked(status);
      return chunk != null ? chunk : this.getChunkIfPresentUnchecked(status.getParent());
   }

   public @Nullable ChunkStatus getPersistedStatus() {
      CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(ChunkStatus.EMPTY.getIndex());
      ChunkAccess chunkAccess = future == null ? null : future.getNow(NOT_DONE_YET).orElse(null);
      return chunkAccess == null ? null : chunkAccess.getPersistedStatus();
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
      List<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>> result = new ArrayList<>();

      for (int i = 0; i < CHUNK_STATUSES.size(); i++) {
         result.add(Pair.of(CHUNK_STATUSES.get(i), this.futures.get(i)));
      }

      return result;
   }

   @VisibleForDebug
   public @Nullable ChunkStatus getLatestStatus() {
      ChunkStatus status = this.startedWork.get();
      if (status == null) {
         return null;
      }

      ChunkAccess chunk = this.getChunkIfPresentUnchecked(status);
      return chunk != null ? status : status.getParent();
   }
}
