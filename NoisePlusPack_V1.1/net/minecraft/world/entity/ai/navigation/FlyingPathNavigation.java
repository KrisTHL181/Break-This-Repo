package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class FlyingPathNavigation extends PathNavigation {
   public FlyingPathNavigation(Mob p_26424_, Level p_26425_) {
      super(p_26424_, p_26425_);
   }

   @Override
   protected PathFinder createPathFinder(int p_26428_) {
      this.nodeEvaluator = new FlyNodeEvaluator();
      return new PathFinder(this.nodeEvaluator, p_26428_);
   }

   @Override
   protected boolean canMoveDirectly(Vec3 p_262585_, Vec3 p_262682_) {
      return isClearForMovementBetween(this.mob, p_262585_, p_262682_, true);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.canFloat() && this.mob.isInLiquid() || !this.mob.isPassenger();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return this.mob.position();
   }

   @Override
   public Path createPath(Entity p_26430_, int p_26431_) {
      return this.createPath(p_26430_.blockPosition(), p_26431_);
   }

   @Override
   public void tick() {
      this.tick++;
      if (this.hasDelayedRecomputation) {
         this.recomputePath();
      }

      if (!this.isDone()) {
         if (this.canUpdatePath()) {
            this.followThePath();
         } else if (this.path != null && !this.path.isDone()) {
            Vec3 vec3 = this.path.getNextEntityPos(this.mob);
            if (this.mob.getBlockX() == Mth.floor(vec3.x) && this.mob.getBlockY() == Mth.floor(vec3.y) && this.mob.getBlockZ() == Mth.floor(vec3.z)) {
               this.path.advance();
            }
         }

         if (!this.isDone()) {
            Vec3 vec31 = this.path.getNextEntityPos(this.mob);
            this.mob.getMoveControl().setWantedPosition(vec31.x, vec31.y, vec31.z, this.speedModifier);
         }
      }
   }

   @Override
   public boolean isStableDestination(BlockPos p_26439_) {
      return this.level.getBlockState(p_26439_).entityCanStandOn(this.level, p_26439_, this.mob);
   }

   @Override
   public boolean canNavigateGround() {
      return false;
   }
}
