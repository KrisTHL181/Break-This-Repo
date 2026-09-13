package net.minecraft.world.level.chunk.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

public final class ChunkDependencies {
   private final ImmutableList<ChunkStatus> dependencyByRadius;
   private final int[] radiusByDependency;

   public ChunkDependencies(ImmutableList<ChunkStatus> p_344765_) {
      this.dependencyByRadius = p_344765_;
      int i = p_344765_.isEmpty() ? 0 : ((ChunkStatus)p_344765_.getFirst()).getIndex() + 1;
      this.radiusByDependency = new int[i];

      for (int j = 0; j < p_344765_.size(); j++) {
         ChunkStatus chunkstatus = (ChunkStatus)p_344765_.get(j);
         int k = chunkstatus.getIndex();

         for (int l = 0; l <= k; l++) {
            this.radiusByDependency[l] = j;
         }
      }
   }

   @VisibleForTesting
   public ImmutableList<ChunkStatus> asList() {
      return this.dependencyByRadius;
   }

   public int size() {
      return this.dependencyByRadius.size();
   }

   public int getRadiusOf(ChunkStatus p_343147_) {
      int i = p_343147_.getIndex();
      if (i >= this.radiusByDependency.length) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "Requesting a ChunkStatus(%s) outside of dependency range(%s)", p_343147_, this.dependencyByRadius)
         );
      } else {
         return this.radiusByDependency[i];
      }
   }

   public int getRadius() {
      return Math.max(0, this.dependencyByRadius.size() - 1);
   }

   public ChunkStatus get(int p_343463_) {
      return (ChunkStatus)this.dependencyByRadius.get(p_343463_);
   }

   @Override
   public String toString() {
      return this.dependencyByRadius.toString();
   }
}
