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
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;
   private boolean canPathToTargetsBelowSurface;

   public GroundPathNavigation(Mob p_26448_, Level p_26449_) {
      super(p_26448_, p_26449_);
   }

   @Override
   protected PathFinder createPathFinder(int p_26453_) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      return new PathFinder(this.nodeEvaluator, p_26453_);
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
   public Path createPath(BlockPos p_26475_, int p_26476_) {
      LevelChunk levelchunk = this.level
         .getChunkSource()
         .getChunkNow(SectionPos.blockToSectionCoord(p_26475_.getX()), SectionPos.blockToSectionCoord(p_26475_.getZ()));
      if (levelchunk == null) {
         return null;
      }

      if (!this.canPathToTargetsBelowSurface) {
         p_26475_ = this.findSurfacePosition(levelchunk, p_26475_, p_26476_);
      }

      return super.createPath(p_26475_, p_26476_);
   }

   final BlockPos findSurfacePosition(LevelChunk p_430120_, BlockPos p_422761_, int p_427161_) {
      if (p_430120_.getBlockState(p_422761_).isAir()) {
         BlockPos.MutableBlockPos blockpos$mutableblockpos = p_422761_.mutable().move(Direction.DOWN);

         while (blockpos$mutableblockpos.getY() >= this.level.getMinY() && p_430120_.getBlockState(blockpos$mutableblockpos).isAir()) {
            blockpos$mutableblockpos.move(Direction.DOWN);
         }

         if (blockpos$mutableblockpos.getY() >= this.level.getMinY()) {
            return blockpos$mutableblockpos.above();
         }

         blockpos$mutableblockpos.setY(p_422761_.getY() + 1);

         while (blockpos$mutableblockpos.getY() <= this.level.getMaxY() && p_430120_.getBlockState(blockpos$mutableblockpos).isAir()) {
            blockpos$mutableblockpos.move(Direction.UP);
         }

         p_422761_ = blockpos$mutableblockpos;
      }

      if (!p_430120_.getBlockState(p_422761_).isSolid()) {
         return p_422761_;
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = p_422761_.mutable().move(Direction.UP);

      while (blockpos$mutableblockpos1.getY() <= this.level.getMaxY() && p_430120_.getBlockState(blockpos$mutableblockpos1).isSolid()) {
         blockpos$mutableblockpos1.move(Direction.UP);
      }

      return blockpos$mutableblockpos1.immutable();
   }

   @Override
   public Path createPath(Entity p_26465_, int p_26466_) {
      return this.createPath(p_26465_.blockPosition(), p_26466_);
   }

   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int i = this.mob.getBlockY();
         BlockState blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), i, this.mob.getZ()));
         int j = 0;

         while (blockstate.is(Blocks.WATER)) {
            blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++i, this.mob.getZ()));
            if (++j > 16) {
               return this.mob.getBlockY();
            }
         }

         return i;
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

   protected boolean hasValidPathType(PathType p_329492_) {
      if (p_329492_ == PathType.WATER) {
         return false;
      } else {
         return p_329492_ == PathType.LAVA ? false : p_329492_ != PathType.OPEN;
      }
   }

   public void setAvoidSun(boolean p_26491_) {
      this.avoidSun = p_26491_;
   }

   public void setCanWalkOverFences(boolean p_255877_) {
      this.nodeEvaluator.setCanWalkOverFences(p_255877_);
   }

   public void setCanPathToTargetsBelowSurface(boolean p_428085_) {
      this.canPathToTargetsBelowSurface = p_428085_;
   }
}
