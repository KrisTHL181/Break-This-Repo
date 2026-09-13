package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FollowPlayerRiddenEntityGoal extends Goal {
   private int timeToRecalcPath;
   private final PathfinderMob mob;
   private final Class<? extends Entity> entityTypeToFollow;
   private @Nullable Player following;
   private FollowPlayerRiddenEntityGoal.FollowEntityGoal currentGoal;

   public FollowPlayerRiddenEntityGoal(final PathfinderMob mob, final Class<? extends Entity> entityTypeToFollow) {
      this.mob = mob;
      this.entityTypeToFollow = entityTypeToFollow;
   }

   @Override
   public boolean canUse() {
      if (this.following != null && this.following.hasMovedHorizontallyRecently()) {
         return true;
      }

      for (Entity entity : this.mob.level().getEntitiesOfClass(this.entityTypeToFollow, this.mob.getBoundingBox().inflate(5.0))) {
         if (entity.getControllingPassenger() instanceof Player controllingPlayer && controllingPlayer.hasMovedHorizontallyRecently()) {
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
      for (Entity entity : this.mob.level().getEntitiesOfClass(this.entityTypeToFollow, this.mob.getBoundingBox().inflate(5.0))) {
         if (entity.getControllingPassenger() instanceof Player player) {
            this.following = player;
            break;
         }
      }

      this.timeToRecalcPath = 0;
      this.currentGoal = FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_TO_ENTITY;
   }

   @Override
   public void stop() {
      this.following = null;
   }

   @Override
   public void tick() {
      float speed = this.currentGoal == FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_IN_ENTITY_DIRECTION ? 0.01F : 0.015F;
      this.mob.moveRelative(speed, new Vec3(this.mob.xxa, this.mob.yya, this.mob.zza));
      this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         if (this.currentGoal == FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_TO_ENTITY) {
            BlockPos behindEntityPos = this.following.blockPosition().relative(this.following.getDirection().getOpposite());
            behindEntityPos = behindEntityPos.offset(0, -1, 0);
            this.mob.getNavigation().moveTo(behindEntityPos.getX(), behindEntityPos.getY(), behindEntityPos.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) < 4.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_IN_ENTITY_DIRECTION;
            }
         } else if (this.currentGoal == FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_IN_ENTITY_DIRECTION) {
            Direction direction = this.following.getMotionDirection();
            BlockPos goTo = this.following.blockPosition().relative(direction, 10);
            this.mob.getNavigation().moveTo(goTo.getX(), goTo.getY() - 1, goTo.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) > 12.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = FollowPlayerRiddenEntityGoal.FollowEntityGoal.GO_TO_ENTITY;
            }
         }
      }
   }

   private enum FollowEntityGoal {
      GO_TO_ENTITY,
      GO_IN_ENTITY_DIRECTION;
   }
}
