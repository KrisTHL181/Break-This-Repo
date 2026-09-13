package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;
   private boolean canPathToTargetsBelowSurface;

   public GroundPathNavigation(final Mob mob, final Level level) {
      super(mob, level);
   }

   @Override
   protected PathFinder createPathFinder(final int maxVisitedNodes) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
   }

   @Override
   public Path createPath(BlockPos pos, final int reachRange) {
      LevelChunk chunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
      if (chunk == null) {
         return null;
      }

      if (!this.canPathToTargetsBelowSurface) {
         pos = this.findSurfacePosition(chunk, pos, reachRange);
      }

      return super.createPath(pos, reachRange);
   }

   private BlockPos findSurfacePosition(final LevelChunk chunk, BlockPos pos, final int reachRange) {
      if (chunk.getBlockState(pos).isAir()) {
         BlockPos.MutableBlockPos columnPos = pos.mutable().move(Direction.DOWN);

         while (columnPos.getY() >= this.level.getMinY() && chunk.getBlockState(columnPos).isAir()) {
            columnPos.move(Direction.DOWN);
         }

         if (columnPos.getY() >= this.level.getMinY()) {
            return columnPos.above();
         }

         columnPos.setY(pos.getY() + 1);

         while (columnPos.getY() <= this.level.getMaxY() && chunk.getBlockState(columnPos).isAir()) {
            columnPos.move(Direction.UP);
         }

         pos = columnPos;
      }

      if (!chunk.getBlockState(pos).isSolid()) {
         return pos;
      }

      BlockPos.MutableBlockPos columnPos = pos.mutable().move(Direction.UP);

      while (columnPos.getY() <= this.level.getMaxY() && chunk.getBlockState(columnPos).isSolid()) {
         columnPos.move(Direction.UP);
      }

      return columnPos.immutable();
   }

   @Override
   public Path createPath(final Entity target, final int reachRange) {
      return this.createPath(target.blockPosition(), reachRange);
   }

   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int surface = this.mob.getBlockY();
         BlockState state = this.level.getBlockState(BlockPos.containing(this.mob.getX(), surface, this.mob.getZ()));
         int steps = 0;

         while (state.is(Blocks.WATER)) {
            state = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++surface, this.mob.getZ()));
            if (++steps > 16) {
               return this.mob.getBlockY();
            }
         }

         return surface;
      } else {
         return Mth.floor(this.mob.getY() + 0.5);
      }
   }

   @Override
   protected void trimPath() {
      super.trimPath();
      if (this.avoidSun) {
         if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
            return;
         }

         for (int i = 0; i < this.path.getNodeCount(); i++) {
            Node node = this.path.getNode(i);
            if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
               this.path.truncateNodes(i);
               return;
            }
         }
      }
   }

   @Override
   public boolean canNavigateGround() {
      return true;
   }

   public void setAvoidSun(final boolean avoidSun) {
      this.avoidSun = avoidSun;
   }

   public void setCanWalkOverFences(final boolean canWalkOverFences) {
      this.nodeEvaluator.setCanWalkOverFences(canWalkOverFences);
   }

   public void setCanPathToTargetsBelowSurface(final boolean canPathToTargetsBelowSurface) {
      this.canPathToTargetsBelowSurface = canPathToTargetsBelowSurface;
   }
}
