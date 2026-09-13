package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearUseGoal<T extends Monster> extends Goal {
   private static final int MIN_REPOSITION_DISTANCE = 6;
   private static final int MAX_REPOSITION_DISTANCE = 7;
   private static final int MIN_COOLDOWN_DISTANCE = 9;
   private static final int MAX_COOLDOWN_DISTANCE = 11;
   private static final double MAX_FLEEING_TIME = reducedTickDelay(100);
   private final T mob;
   private SpearUseGoal.@Nullable SpearUseState state;
   private final double speedModifierWhenCharging;
   private final double speedModifierWhenRepositioning;
   private final float approachDistanceSq;
   private final float targetInRangeRadiusSq;

   public SpearUseGoal(
      final T mob,
      final double speedModifierWhenCharging,
      final double speedModifierWhenRepositioning,
      final float approachDistance,
      final float targetInRangeRadius
   ) {
      this.mob = mob;
      this.speedModifierWhenCharging = speedModifierWhenCharging;
      this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
      this.approachDistanceSq = approachDistance * approachDistance;
      this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.ableToAttack() && !this.mob.isUsingItem();
   }

   private boolean ableToAttack() {
      return this.mob.getTarget() != null && this.mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
   }

   private int getKineticWeaponUseDuration() {
      int durationTicks = Optional.ofNullable(this.mob.getMainHandItem().get(DataComponents.KINETIC_WEAPON))
         .map(KineticWeapon::computeDamageUseDuration)
         .orElse(0);
      return reducedTickDelay(durationTicks);
   }

   @Override
   public boolean canContinueToUse() {
      return this.state != null && !this.state.done && this.ableToAttack();
   }

   @Override
   public void start() {
      super.start();
      this.mob.setAggressive(true);
      this.state = new SpearUseGoal.SpearUseState();
   }

   @Override
   public void stop() {
      super.stop();
      this.mob.getNavigation().stop();
      this.mob.setAggressive(false);
      this.state = null;
      this.mob.stopUsingItem();
   }

   @Override
   public void tick() {
      if (this.state != null) {
         LivingEntity target = this.mob.getTarget();
         double targetDistSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
         Entity mount = this.mob.getRootVehicle();
         float speedModifier = 1.0F;
         if (mount instanceof Mob vehicleMob) {
            speedModifier = vehicleMob.chargeSpeedModifier();
         }

         int mountDistance = this.mob.isPassenger() ? 2 : 0;
         this.mob.lookAt(target, 30.0F, 30.0F);
         this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (this.state.notEngagedYet()) {
            if (targetDistSqr > this.approachDistanceSq) {
               this.mob.getNavigation().moveTo(target, speedModifier * this.speedModifierWhenRepositioning);
               return;
            }

            this.state.startEngagement(this.getKineticWeaponUseDuration());
            this.mob.startUsingItem(InteractionHand.MAIN_HAND);
         }

         if (this.state.tickAndCheckEngagement()) {
            this.mob.stopUsingItem();
            double distance = Math.sqrt(targetDistSqr);
            this.state.awayPos = LandRandomPos.getPosAway(
               this.mob, Math.max(0.0, 9 + mountDistance - distance), Math.max(1.0, 11 + mountDistance - distance), 7, target.position()
            );
            this.state.fleeingTime = 1;
         }

         if (!this.state.tickAndCheckFleeing()) {
            if (this.state.awayPos != null) {
               this.mob
                  .getNavigation()
                  .moveTo(this.state.awayPos.x, this.state.awayPos.y, this.state.awayPos.z, speedModifier * this.speedModifierWhenRepositioning);
               if (this.mob.getNavigation().isDone()) {
                  if (this.state.fleeingTime > 0) {
                     this.state.done = true;
                     return;
                  }

                  this.state.awayPos = null;
               }
            } else {
               this.mob.getNavigation().moveTo(target, speedModifier * this.speedModifierWhenCharging);
               if (targetDistSqr < this.targetInRangeRadiusSq || this.mob.getNavigation().isDone()) {
                  double distance = Math.sqrt(targetDistSqr);
                  this.state.awayPos = LandRandomPos.getPosAway(this.mob, 6 + mountDistance - distance, 7 + mountDistance - distance, 7, target.position());
               }
            }
         }
      }
   }

   public static class SpearUseState {
      private int engageTime = -1;
      private int fleeingTime = -1;
      private @Nullable Vec3 awayPos;
      private boolean done = false;

      public boolean notEngagedYet() {
         return this.engageTime < 0;
      }

      public void startEngagement(final int spearDownTime) {
         this.engageTime = spearDownTime;
      }

      public boolean tickAndCheckEngagement() {
         if (this.engageTime > 0) {
            this.engageTime--;
            if (this.engageTime == 0) {
               return true;
            }
         }

         return false;
      }

      public boolean tickAndCheckFleeing() {
         if (this.fleeingTime > 0) {
            this.fleeingTime++;
            if (this.fleeingTime > SpearUseGoal.MAX_FLEEING_TIME) {
               this.done = true;
               return true;
            }
         }

         return false;
      }
   }
}
