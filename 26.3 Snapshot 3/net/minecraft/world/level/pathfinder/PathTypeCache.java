package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jspecify.annotations.Nullable;

public class PathTypeCache {
   private static final int SIZE = 4096;
   private static final int MASK = 4095;
   private final long[] positions = new long[4096];
   private final PathType[] pathTypes = new PathType[4096];

   public PathType getOrCompute(final BlockGetter level, final BlockPos pos) {
      long key = pos.asLong();
      int index = index(key);
      PathType cachedPathType = this.get(index, key);
      return cachedPathType != null ? cachedPathType : this.compute(level, pos, index, key);
   }

   private @Nullable PathType get(final int index, final long key) {
      return this.positions[index] == key ? this.pathTypes[index] : null;
   }

   private PathType compute(final BlockGetter level, final BlockPos pos, final int index, final long key) {
      PathType pathType = WalkNodeEvaluator.getPathTypeFromState(level, pos);
      this.positions[index] = key;
      this.pathTypes[index] = pathType;
      return pathType;
   }

   public void invalidate(final BlockPos pos) {
      long key = pos.asLong();
      int index = index(key);
      if (this.positions[index] == key) {
         this.pathTypes[index] = null;
      }
   }

   private static int index(final long pos) {
      return (int)HashCommon.mix(pos) & 4095;
   }
}
