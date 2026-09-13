package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.jspecify.annotations.Nullable;

public record ChunkStep(
   ChunkStatus targetStatus, ChunkDependencies directDependencies, ChunkDependencies accumulatedDependencies, int blockStateWriteRadius, ChunkStatusTask task
) {
   public int getAccumulatedRadiusOf(ChunkStatus p_345141_) {
      return p_345141_ == this.targetStatus ? 0 : this.accumulatedDependencies.getRadiusOf(p_345141_);
   }

   public CompletableFuture<ChunkAccess> apply(WorldGenContext p_344687_, StaticCache2D<GenerationChunkHolder> p_343159_, ChunkAccess p_344017_) {
      if (p_344017_.getPersistedStatus().isBefore(this.targetStatus)) {
         ProfiledDuration profiledduration = JvmProfiler.INSTANCE
            .onChunkGenerate(p_344017_.getPos(), p_344687_.level().dimension(), this.targetStatus.getName());
         return this.task.doWork(p_344687_, this, p_343159_, p_344017_).thenApply(p_345132_ -> this.completeChunkGeneration(p_345132_, profiledduration));
      } else {
         return this.task.doWork(p_344687_, this, p_343159_, p_344017_);
      }
   }

   private ChunkAccess completeChunkGeneration(ChunkAccess p_342706_, @Nullable ProfiledDuration p_343538_) {
      if (p_342706_ instanceof ProtoChunk protochunk && protochunk.getPersistedStatus().isBefore(this.targetStatus)) {
         protochunk.setPersistedStatus(this.targetStatus);
      }

      if (p_343538_ != null) {
         p_343538_.finish(true);
      }

      return p_342706_;
   }

   public static class Builder {
      private final ChunkStatus status;
      private final @Nullable ChunkStep parent;
      private ChunkStatus[] directDependenciesByRadius;
      private int blockStateWriteRadius = -1;
      private ChunkStatusTask task = ChunkStatusTasks::passThrough;

      protected Builder(ChunkStatus p_342893_) {
         if (p_342893_.getParent() != p_342893_) {
            throw new IllegalArgumentException("Not starting with the first status: " + p_342893_);
         }

         this.status = p_342893_;
         this.parent = null;
         this.directDependenciesByRadius = new ChunkStatus[0];
      }

      protected Builder(ChunkStatus p_343422_, ChunkStep p_345214_) {
         if (p_345214_.targetStatus.getIndex() != p_343422_.getIndex() - 1) {
            throw new IllegalArgumentException("Out of order status: " + p_343422_);
         }

         this.status = p_343422_;
         this.parent = p_345214_;
         this.directDependenciesByRadius = new ChunkStatus[]{p_345214_.targetStatus};
      }

      public ChunkStep.Builder addRequirement(ChunkStatus p_345438_, int p_342711_) {
         if (p_345438_.isOrAfter(this.status)) {
            throw new IllegalArgumentException("Status " + p_345438_ + " can not be required by " + this.status);
         }

         ChunkStatus[] achunkstatus = this.directDependenciesByRadius;
         int i = p_342711_ + 1;
         if (i > achunkstatus.length) {
            this.directDependenciesByRadius = new ChunkStatus[i];
            Arrays.fill(this.directDependenciesByRadius, p_345438_);
         }

         for (int j = 0; j < Math.min(i, achunkstatus.length); j++) {
            this.directDependenciesByRadius[j] = ChunkStatus.max(achunkstatus[j], p_345438_);
         }

         return this;
      }

      public ChunkStep.Builder blockStateWriteRadius(int p_343879_) {
         this.blockStateWriteRadius = p_343879_;
         return this;
      }

      public ChunkStep.Builder setTask(ChunkStatusTask p_342761_) {
         this.task = p_342761_;
         return this;
      }

      public ChunkStep build() {
         return new ChunkStep(
            this.status,
            new ChunkDependencies(ImmutableList.copyOf(this.directDependenciesByRadius)),
            new ChunkDependencies(ImmutableList.copyOf(this.buildAccumulatedDependencies())),
            this.blockStateWriteRadius,
            this.task
         );
      }

      private ChunkStatus[] buildAccumulatedDependencies() {
         if (this.parent == null) {
            return this.directDependenciesByRadius;
         }

         int i = this.getRadiusOfParent(this.parent.targetStatus);
         ChunkDependencies chunkdependencies = this.parent.accumulatedDependencies;
         ChunkStatus[] achunkstatus = new ChunkStatus[Math.max(i + chunkdependencies.size(), this.directDependenciesByRadius.length)];

         for (int j = 0; j < achunkstatus.length; j++) {
            int k = j - i;
            if (k < 0 || k >= chunkdependencies.size()) {
               achunkstatus[j] = this.directDependenciesByRadius[j];
            } else if (j >= this.directDependenciesByRadius.length) {
               achunkstatus[j] = chunkdependencies.get(k);
            } else {
               achunkstatus[j] = ChunkStatus.max(this.directDependenciesByRadius[j], chunkdependencies.get(k));
            }
         }

         return achunkstatus;
      }

      private int getRadiusOfParent(ChunkStatus p_344180_) {
         for (int i = this.directDependenciesByRadius.length - 1; i >= 0; i--) {
            if (this.directDependenciesByRadius[i].isOrAfter(p_344180_)) {
               return i;
            }
         }

         return 0;
      }
   }
}
