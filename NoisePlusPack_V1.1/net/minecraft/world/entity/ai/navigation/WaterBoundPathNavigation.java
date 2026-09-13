package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class WaterBoundPathNavigation extends PathNavigation {
   private boolean allowBreaching;

   public WaterBoundPathNavigation(Mob p_26594_, Level p_26595_) {
      super(p_26594_, p_26595_);
   }

   @Override
   protected PathFinder createPathFinder(int p_26598_) {
      this.allowBreaching = this.mob.getType() == EntityType.DOLPHIN;
      this.nodeEvaluator = new SwimNodeEvaluator(this.allowBreaching);
      this.nodeEvaluator.setCanPassDoors(false);
      return new PathFinder(this.nodeEvaluator, p_26598_);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.allowBreaching || this.mob.isInLiquid();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), this.mob.getY(0.5), this.mob.getZ());
   }

   @Override
   protected double getGroundY(Vec3 p_186136_) {
      return p_186136_.y;
   }

   @Override
   protected boolean canMoveDirectly(Vec3 p_186138_, Vec3 p_186139_) {
      return isClearForMovementBetween(this.mob, p_186138_, p_186139_, false);
   }

   @Override
   public boolean isStableDestination(BlockPos p_26608_) {
      return !this.level.getBlockState(p_26608_).isSolidRender();
   }

   @Override
   public void setCanFloat(boolean p_26612_) {
   }

   @Override
   public boolean canNavigateGround() {
      return false;
   }
}
