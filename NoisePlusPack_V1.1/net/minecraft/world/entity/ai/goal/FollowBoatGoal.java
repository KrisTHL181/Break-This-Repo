package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FollowBoatGoal extends Goal {
   private int timeToRecalcPath;
   private final PathfinderMob mob;
   private @Nullable Player following;
   private BoatGoals currentGoal;

   public FollowBoatGoal(PathfinderMob p_25238_) {
      this.mob = p_25238_;
   }

   @Override
   public boolean canUse() {
      if (this.following != null && this.following.hasMovedHorizontallyRecently()) {
         return true;
      }

      for (AbstractBoat abstractboat : this.mob.level().getEntitiesOfClass(AbstractBoat.class, this.mob.getBoundingBox().inflate(5.0))) {
         if (abstractboat.getControllingPassenger() instanceof Player player && player.hasMovedHorizontallyRecently()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isInterruptable() {
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      return this.following != null && this.following.isPassenger() && this.following.hasMovedHorizontallyRecently();
   }

   @Override
   public void start() {
      for (AbstractBoat abstractboat : this.mob.level().getEntitiesOfClass(AbstractBoat.class, this.mob.getBoundingBox().inflate(5.0))) {
         if (abstractboat.getControllingPassenger() instanceof Player player) {
            this.following = player;
            break;
         }
      }

      this.timeToRecalcPath = 0;
      this.currentGoal = BoatGoals.GO_TO_BOAT;
   }

   @Override
   public void stop() {
      this.following = null;
   }

   @Override
   public void tick() {
      float f = this.currentGoal == BoatGoals.GO_IN_BOAT_DIRECTION ? 0.01F : 0.015F;
      this.mob.moveRelative(f, new Vec3(this.mob.xxa, this.mob.yya, this.mob.zza));
      this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         if (this.currentGoal == BoatGoals.GO_TO_BOAT) {
            BlockPos blockpos = this.following.blockPosition().relative(this.following.getDirection().getOpposite());
            blockpos = blockpos.offset(0, -1, 0);
            this.mob.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) < 4.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = BoatGoals.GO_IN_BOAT_DIRECTION;
            }
         } else if (this.currentGoal == BoatGoals.GO_IN_BOAT_DIRECTION) {
            Direction direction = this.following.getMotionDirection();
            BlockPos blockpos1 = this.following.blockPosition().relative(direction, 10);
            this.mob.getNavigation().moveTo(blockpos1.getX(), blockpos1.getY() - 1, blockpos1.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) > 12.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = BoatGoals.GO_TO_BOAT;
            }
         }
      }
   }
}
