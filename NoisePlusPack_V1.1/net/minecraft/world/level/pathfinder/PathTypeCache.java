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

   public PathType getOrCompute(BlockGetter p_328738_, BlockPos p_328240_) {
      long i = p_328240_.asLong();
      int j = index(i);
      PathType pathtype = this.get(j, i);
      return pathtype != null ? pathtype : this.compute(p_328738_, p_328240_, j, i);
   }

   private @Nullable PathType get(int p_331898_, long p_334711_) {
      return this.positions[p_331898_] == p_334711_ ? this.pathTypes[p_331898_] : null;
   }

   private PathType compute(BlockGetter p_333989_, BlockPos p_334142_, int p_329562_, long p_332989_) {
      PathType pathtype = WalkNodeEvaluator.getPathTypeFromState(p_333989_, p_334142_);
      this.positions[p_329562_] = p_332989_;
      this.pathTypes[p_329562_] = pathtype;
      return pathtype;
   }

   public void invalidate(BlockPos p_332226_) {
      long i = p_332226_.asLong();
      int j = index(i);
      if (this.positions[j] == i) {
         this.pathTypes[j] = null;
      }
   }

   private static int index(long p_328788_) {
      return (int)HashCommon.mix(p_328788_) & 4095;
   }
}
