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
    public int getAccumulatedRadiusOf(final ChunkStatus status) {
        return status == this.targetStatus ? 0 : this.accumulatedDependencies.getRadiusOf(status);
    }

    public CompletableFuture<ChunkAccess> apply(final WorldGenContext context, final StaticCache2D<GenerationChunkHolder> cache, final ChunkAccess chunk) {
        if (chunk.getPersistedStatus().isBefore(this.targetStatus)) {
            ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onChunkGenerate(chunk.getPos(), context.level().dimension(), this.targetStatus.getName());
            return this.task.doWork(context, this, cache, chunk).thenApply(newCenterChunk -> this.completeChunkGeneration(newCenterChunk, profiledDuration));
        } else {
            return this.task.doWork(context, this, cache, chunk);
        }
    }

    private ChunkAccess completeChunkGeneration(final ChunkAccess newCenterChunk, final @Nullable ProfiledDuration profiledDuration) {
        if (newCenterChunk instanceof ProtoChunk protochunk && protochunk.getPersistedStatus().isBefore(this.targetStatus)) {
            protochunk.setPersistedStatus(this.targetStatus);
        }

        if (profiledDuration != null) {
            profiledDuration.finish(true);
        }

        return newCenterChunk;
    }

    public static class Builder {
        private final ChunkStatus status;
        private final @Nullable ChunkStep parent;
        private ChunkStatus[] directDependenciesByRadius;
        private int blockStateWriteRadius = -1;
        private ChunkStatusTask task = ChunkStatusTasks::passThrough;

        protected Builder(final ChunkStatus status) {
            if (status.getParent() != status) {
                throw new IllegalArgumentException("Not starting with the first status: " + status);
            }

            this.status = status;
            this.parent = null;
            this.directDependenciesByRadius = new ChunkStatus[0];
        }

        protected Builder(final ChunkStatus status, final ChunkStep parent) {
            if (parent.targetStatus.getIndex() != status.getIndex() - 1) {
                throw new IllegalArgumentException("Out of order status: " + status);
            }

            this.status = status;
            this.parent = parent;
            this.directDependenciesByRadius = new ChunkStatus[]{parent.targetStatus};
        }

        public ChunkStep.Builder addRequirement(final ChunkStatus status, final int radius) {
            if (status.isOrAfter(this.status)) {
                throw new IllegalArgumentException("Status " + status + " can not be required by " + this.status);
            }

            ChunkStatus[] previous = this.directDependenciesByRadius;
            int newLength = radius + 1;
            if (newLength > previous.length) {
                this.directDependenciesByRadius = new ChunkStatus[newLength];
                Arrays.fill(this.directDependenciesByRadius, status);
            }

            for (int i = 0; i < Math.min(newLength, previous.length); i++) {
                this.directDependenciesByRadius[i] = ChunkStatus.max(previous[i], status);
            }

            return this;
        }

        public ChunkStep.Builder blockStateWriteRadius(final int radius) {
            this.blockStateWriteRadius = radius;
            return this;
        }

        public ChunkStep.Builder setTask(final ChunkStatusTask task) {
            this.task = task;
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

            int radiusOfParent = this.getRadiusOfParent(this.parent.targetStatus);
            ChunkDependencies parentDependencies = this.parent.accumulatedDependencies;
            ChunkStatus[] accumulatedDependencies = new ChunkStatus[Math.max(radiusOfParent + parentDependencies.size(), this.directDependenciesByRadius.length)];

            for (int distance = 0; distance < accumulatedDependencies.length; distance++) {
                int distanceInParent = distance - radiusOfParent;
                if (distanceInParent < 0 || distanceInParent >= parentDependencies.size()) {
                    accumulatedDependencies[distance] = this.directDependenciesByRadius[distance];
                } else if (distance >= this.directDependenciesByRadius.length) {
                    accumulatedDependencies[distance] = parentDependencies.get(distanceInParent);
                } else {
                    accumulatedDependencies[distance] = ChunkStatus.max(this.directDependenciesByRadius[distance], parentDependencies.get(distanceInParent));
                }
            }

            return accumulatedDependencies;
        }

        private int getRadiusOfParent(final ChunkStatus status) {
            for (int i = this.directDependenciesByRadius.length - 1; i >= 0; i--) {
                if (this.directDependenciesByRadius[i].isOrAfter(status)) {
                    return i;
                }
            }

            return 0;
        }
    }
}