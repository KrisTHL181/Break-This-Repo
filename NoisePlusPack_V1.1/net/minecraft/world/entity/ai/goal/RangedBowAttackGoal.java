package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

public class RangedBowAttackGoal<T extends Monster & RangedAttackMob> extends Goal {
   private final T mob;
   private final double speedModifier;
   private int attackIntervalMin;
   private final float attackRadiusSqr;
   private int attackTime = -1;
   private int seeTime;
   private boolean strafingClockwise;
   private boolean strafingBackwards;
   private int strafingTime = -1;

   public RangedBowAttackGoal(T p_25792_, double p_25793_, int p_25794_, float p_25795_) {
      this.mob = p_25792_;
      this.speedModifier = p_25793_;
      this.attackIntervalMin = p_25794_;
      this.attackRadiusSqr = p_25795_ * p_25795_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   public void setMinAttackInterval(int p_25798_) {
      this.attackIntervalMin = p_25798_;
   }

   @Override
   public boolean canUse() {
      return this.mob.getTarget() == null ? false : this.isHoldingBow();
   }

   protected boolean isHoldingBow() {
      return this.mob.isHolding(Items.BOW);
   }

   @Override
   public boolean canContinueToUse() {
      return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
   }

   @Override
   public void start() {
      super.start();
      this.mob.setAggressive(true);
   }

   @Override
   public void stop() {
      super.stop();
      this.mob.setAggressive(false);
      this.seeTime = 0;
      this.attackTime = -1;
      this.mob.stopUsingItem();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      LivingEntity livingentity = this.mob.getTarget();
      if (livingentity != null) {
         double d0 = this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
         boolean flag = this.mob.getSensing().hasLineOfSight(livingentity);
         boolean flag1 = this.seeTime > 0;
         if (flag != flag1) {
            this.seeTime = 0;
         }

         if (flag) {
            this.seeTime++;
         } else {
            this.seeTime--;
         }

         if (!(d0 > this.attackRadiusSqr) && this.seeTime >= 20) {
            this.mob.getNavigation().stop();
            this.strafingTime++;
         } else {
            this.mob.getNavigation().moveTo(livingentity, this.speedModifier);
            this.strafingTime = -1;
         }

         if (this.strafingTime >= 20) {
            if (this.mob.getRandom().nextFloat() < 0.3) {
               this.strafingClockwise = !this.strafingClockwise;
            }

            if (this.mob.getRandom().nextFloat() < 0.3) {
               this.strafingBackwards = !this.strafingBackwards;
            }

            this.strafingTime = 0;
         }

         if (this.strafingTime > -1) {
            if (d0 > this.attackRadiusSqr * 0.75F) {
               this.strafingBackwards = false;
            } else if (d0 < this.attackRadiusSqr * 0.25F) {
               this.strafingBackwards = true;
            }

            this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            if (this.mob.getControlledVehicle() instanceof Mob mob) {
               mob.lookAt(livingentity, 30.0F, 30.0F);
            }

            this.mob.lookAt(livingentity, 30.0F, 30.0F);
         } else {
            this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
         }

         if (this.mob.isUsingItem()) {
            if (!flag && this.seeTime < -60) {
               this.mob.stopUsingItem();
            } else if (flag) {
               int i = this.mob.getTicksUsingItem();
               if (i >= 20) {
                  this.mob.stopUsingItem();
                  this.mob.performRangedAttack(livingentity, BowItem.getPowerForTime(i));
                  this.attackTime = this.attackIntervalMin;
               }
            }
         } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.BOW));
         }
      }
   }
}
