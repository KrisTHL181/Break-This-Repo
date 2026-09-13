package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import org.jspecify.annotations.Nullable;

public class RangedAttackGoal extends Goal {
   private final Mob mob;
   private final RangedAttackMob rangedAttackMob;
   private @Nullable LivingEntity target;
   private int attackTime = -1;
   private final double speedModifier;
   private int seeTime;
   private final int attackIntervalMin;
   private final int attackIntervalMax;
   private final float attackRadius;
   private final float attackRadiusSqr;

   public RangedAttackGoal(final RangedAttackMob mob, final double speedModifier, final int attackInterval, final float attackRadius) {
      this(mob, speedModifier, attackInterval, attackInterval, attackRadius);
   }

   public RangedAttackGoal(
      final RangedAttackMob mob, final double speedModifier, final int attackIntervalMin, final int attackIntervalMax, final float attackRadius
   ) {
      if (!(mob instanceof LivingEntity)) {
         throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
      }

      this.rangedAttackMob = mob;
      this.mob = (Mob)mob;
      this.speedModifier = speedModifier;
      this.attackIntervalMin = attackIntervalMin;
      this.attackIntervalMax = attackIntervalMax;
      this.attackRadius = attackRadius;
      this.attackRadiusSqr = attackRadius * attackRadius;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      LivingEntity bestTarget = this.mob.getTarget();
      if (bestTarget != null && bestTarget.isAlive()) {
         this.target = bestTarget;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse() || this.target.isAlive() && !this.mob.getNavigation().isDone();
   }

   @Override
   public void stop() {
      this.target = null;
      this.seeTime = 0;
      this.attackTime = -1;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      double targetDistSqr = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
      boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(this.target);
      if (hasLineOfSight) {
         this.seeTime++;
      } else {
         this.seeTime = 0;
      }

      if (!(targetDistSqr > this.attackRadiusSqr) && this.seeTime >= 5) {
         this.mob.getNavigation().stop();
      } else {
         this.mob.getNavigation().moveTo(this.target, this.speedModifier);
      }

      this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
      if (--this.attackTime == 0) {
         if (!hasLineOfSight) {
            return;
         }

         float dist = (float)Math.sqrt(targetDistSqr) / this.attackRadius;
         float power = Mth.clamp(dist, 0.1F, 1.0F);
         this.rangedAttackMob.performRangedAttack(this.target, power);
         this.attackTime = Mth.floor(dist * (this.attackIntervalMax - this.attackIntervalMin) + this.attackIntervalMin);
      } else if (this.attackTime < 0) {
         this.attackTime = Mth.floor(Mth.lerp(Math.sqrt(targetDistSqr) / this.attackRadius, this.attackIntervalMin, this.attackIntervalMax));
      }
   }
}
