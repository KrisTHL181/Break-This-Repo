package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

public class RangedCrossbowAttackGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> extends Goal {
   public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
   private final T mob;
   private RangedCrossbowAttackGoal.CrossbowState crossbowState = RangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
   private final double speedModifier;
   private final float attackRadiusSqr;
   private int seeTime;
   private int attackDelay;
   private int updatePathDelay;

   public RangedCrossbowAttackGoal(final T mob, final double speedModifier, final float attackRadius) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.attackRadiusSqr = attackRadius * attackRadius;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.isValidTarget() && this.isHoldingCrossbow();
   }

   private boolean isHoldingCrossbow() {
      return this.mob.isHolding(Items.CROSSBOW);
   }

   @Override
   public boolean canContinueToUse() {
      return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
   }

   private boolean isValidTarget() {
      return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
   }

   @Override
   public void stop() {
      super.stop();
      this.mob.setAggressive(false);
      this.mob.setTarget(null);
      this.seeTime = 0;
      if (this.mob.isUsingItem()) {
         this.mob.stopUsingItem();
         this.mob.setChargingCrossbow(false);
         this.mob.getUseItem().set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
      }
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      LivingEntity target = this.mob.getTarget();
      if (target != null) {
         boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
         boolean hadLineOfSight = this.seeTime > 0;
         if (hasLineOfSight != hadLineOfSight) {
            this.seeTime = 0;
         }

         if (hasLineOfSight) {
            this.seeTime++;
         } else {
            this.seeTime--;
         }

         double distanceToSqr = this.mob.distanceToSqr(target);
         boolean needsToMove = (distanceToSqr > this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
         if (needsToMove) {
            this.updatePathDelay--;
            if (this.updatePathDelay <= 0) {
               this.mob.getNavigation().moveTo(target, this.canRun() ? this.speedModifier : this.speedModifier * 0.5);
               this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
            }
         } else {
            this.updatePathDelay = 0;
            this.mob.getNavigation().stop();
         }

         this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (this.crossbowState == RangedCrossbowAttackGoal.CrossbowState.UNCHARGED) {
            if (!needsToMove) {
               this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
               this.crossbowState = RangedCrossbowAttackGoal.CrossbowState.CHARGING;
               this.mob.setChargingCrossbow(true);
            }
         } else if (this.crossbowState == RangedCrossbowAttackGoal.CrossbowState.CHARGING) {
            if (!this.mob.isUsingItem()) {
               this.crossbowState = RangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
            }

            int pullTime = this.mob.getTicksUsingItem();
            ItemStack useItem = this.mob.getUseItem();
            if (pullTime >= CrossbowItem.getChargeDuration(useItem, this.mob)) {
               this.mob.releaseUsingItem();
               this.crossbowState = RangedCrossbowAttackGoal.CrossbowState.CHARGED;
               this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
               this.mob.setChargingCrossbow(false);
            }
         } else if (this.crossbowState == RangedCrossbowAttackGoal.CrossbowState.CHARGED) {
            this.attackDelay--;
            if (this.attackDelay == 0) {
               this.crossbowState = RangedCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK;
            }
         } else if (this.crossbowState == RangedCrossbowAttackGoal.CrossbowState.READY_TO_ATTACK && hasLineOfSight) {
            this.mob.performRangedAttack(target, 1.0F);
            this.crossbowState = RangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
         }
      }
   }

   private boolean canRun() {
      return this.crossbowState == RangedCrossbowAttackGoal.CrossbowState.UNCHARGED;
   }

   private enum CrossbowState {
      UNCHARGED,
      CHARGING,
      CHARGED,
      READY_TO_ATTACK;
   }
}
