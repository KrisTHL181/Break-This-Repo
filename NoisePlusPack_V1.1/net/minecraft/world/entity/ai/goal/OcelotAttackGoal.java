package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class OcelotAttackGoal extends Goal {
   private final Mob mob;
   private LivingEntity target;
   private int attackTime;

   public OcelotAttackGoal(Mob p_25658_) {
      this.mob = p_25658_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      LivingEntity livingentity = this.mob.getTarget();
      if (livingentity == null) {
         return false;
      }

      this.target = livingentity;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      if (!this.target.isAlive()) {
         return false;
      } else {
         return this.mob.distanceToSqr(this.target) > 225.0 ? false : !this.mob.getNavigation().isDone() || this.canUse();
      }
   }

   @Override
   public void stop() {
      this.target = null;
      this.mob.getNavigation().stop();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
      double d0 = this.mob.getBbWidth() * 2.0F * (this.mob.getBbWidth() * 2.0F);
      double d1 = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
      double d2 = 0.8;
      if (d1 > d0 && d1 < 16.0) {
         d2 = 1.33;
      } else if (d1 < 225.0) {
         d2 = 0.6;
      }

      this.mob.getNavigation().moveTo(this.target, d2);
      this.attackTime = Math.max(this.attackTime - 1, 0);
      if (!(d1 > d0) && this.attackTime <= 0) {
         this.attackTime = 20;
         this.mob.doHurtTarget(getServerLevel(this.mob), this.target);
      }
   }
}
