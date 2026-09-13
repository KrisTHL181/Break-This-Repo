package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PathfindingContext {
   private final CollisionGetter level;
   private final @Nullable PathTypeCache cache;
   private final BlockPos mobPosition;
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

   public PathfindingContext(CollisionGetter p_335722_, Mob p_329527_) {
      this.level = p_335722_;
      if (p_329527_.level() instanceof ServerLevel serverlevel) {
         this.cache = serverlevel.getPathTypeCache();
      } else {
         this.cache = null;
      }

      this.mobPosition = p_329527_.blockPosition();
   }

   public PathType getPathTypeFromState(int p_332092_, int p_328372_, int p_333164_) {
      BlockPos blockpos = this.mutablePos.set(p_332092_, p_328372_, p_333164_);
      return this.cache == null ? WalkNodeEvaluator.getPathTypeFromState(this.level, blockpos) : this.cache.getOrCompute(this.level, blockpos);
   }

   public BlockState getBlockState(BlockPos p_333632_) {
      return this.level.getBlockState(p_333632_);
   }

   public CollisionGetter level() {
      return this.level;
   }

   public BlockPos mobPosition() {
      return this.mobPosition;
   }
}
