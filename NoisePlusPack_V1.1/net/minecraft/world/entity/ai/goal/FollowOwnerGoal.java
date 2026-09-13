package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

public class FollowOwnerGoal extends Goal {
   private final TamableAnimal tamable;
   private @Nullable LivingEntity owner;
   private final double speedModifier;
   private final PathNavigation navigation;
   private int timeToRecalcPath;
   private final float stopDistance;
   private final float startDistance;
   private float oldWaterCost;

   public FollowOwnerGoal(TamableAnimal p_25294_, double p_25295_, float p_25296_, float p_25297_) {
      this.tamable = p_25294_;
      this.speedModifier = p_25295_;
      this.navigation = p_25294_.getNavigation();
      this.startDistance = p_25296_;
      this.stopDistance = p_25297_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      if (!(p_25294_.getNavigation() instanceof GroundPathNavigation) && !(p_25294_.getNavigation() instanceof FlyingPathNavigation)) {
         throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
      }
   }

   @Override
   public boolean canUse() {
      LivingEntity livingentity = this.tamable.getOwner();
      if (livingentity == null) {
         return false;
      }

      if (this.tamable.unableToMoveToOwner()) {
         return false;
      }

      if (this.tamable.distanceToSqr(livingentity) < this.startDistance * this.startDistance) {
         return false;
      }

      this.owner = livingentity;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      if (this.navigation.isDone()) {
         return false;
      } else {
         return this.tamable.unableToMoveToOwner() ? false : !(this.tamable.distanceToSqr(this.owner) <= this.stopDistance * this.stopDistance);
      }
   }

   @Override
   public void start() {
      this.timeToRecalcPath = 0;
      this.oldWaterCost = this.tamable.getPathfindingMalus(PathType.WATER);
      this.tamable.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public void stop() {
      this.owner = null;
      this.navigation.stop();
      this.tamable.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
   }

   @Override
   public void tick() {
      boolean flag = this.tamable.shouldTryTeleportToOwner();
      if (!flag) {
         this.tamable.getLookControl().setLookAt(this.owner, 10.0F, this.tamable.getMaxHeadXRot());
      }

      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         if (flag) {
            this.tamable.tryToTeleportToOwner();
         } else {
            this.navigation.moveTo(this.owner, this.speedModifier);
         }
      }
   }
}
