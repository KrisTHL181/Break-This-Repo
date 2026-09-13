package net.minecraft.world.entity.ai.targeting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

public class TargetingConditions {
   public static final TargetingConditions DEFAULT = forCombat();
   private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0;
   private final boolean isCombat;
   private double range = -1.0;
   private boolean checkLineOfSight = true;
   private boolean testInvisible = true;
   private TargetingConditions.@Nullable Selector selector;

   private TargetingConditions(boolean p_148351_) {
      this.isCombat = p_148351_;
   }

   public static TargetingConditions forCombat() {
      return new TargetingConditions(true);
   }

   public static TargetingConditions forNonCombat() {
      return new TargetingConditions(false);
   }

   public TargetingConditions copy() {
      TargetingConditions targetingconditions = this.isCombat ? forCombat() : forNonCombat();
      targetingconditions.range = this.range;
      targetingconditions.checkLineOfSight = this.checkLineOfSight;
      targetingconditions.testInvisible = this.testInvisible;
      targetingconditions.selector = this.selector;
      return targetingconditions;
   }

   public TargetingConditions range(double p_26884_) {
      this.range = p_26884_;
      return this;
   }

   public TargetingConditions ignoreLineOfSight() {
      this.checkLineOfSight = false;
      return this;
   }

   public TargetingConditions ignoreInvisibilityTesting() {
      this.testInvisible = false;
      return this;
   }

   public TargetingConditions selector(TargetingConditions.@Nullable Selector p_362620_) {
      this.selector = p_362620_;
      return this;
   }

   public boolean test(ServerLevel p_364974_, @Nullable LivingEntity p_26886_, LivingEntity p_26887_) {
      if (p_26886_ == p_26887_) {
         return false;
      }

      if (!p_26887_.canBeSeenByAnyone()) {
         return false;
      }

      if (this.selector != null && !this.selector.test(p_26887_, p_364974_)) {
         return false;
      }

      if (p_26886_ == null) {
         if (this.isCombat && (!p_26887_.canBeSeenAsEnemy() || p_364974_.getDifficulty() == Difficulty.PEACEFUL)) {
            return false;
         }
      } else {
         if (this.isCombat && (!p_26886_.canAttack(p_26887_) || !p_26886_.canAttackType(p_26887_.getType()) || p_26886_.isAlliedTo(p_26887_))) {
            return false;
         }

         if (this.range > 0.0) {
            double d0 = this.testInvisible ? p_26887_.getVisibilityPercent(p_26886_) : 1.0;
            double d1 = Math.max(this.range * d0, 2.0);
            double d2 = p_26886_.distanceToSqr(p_26887_.getX(), p_26887_.getY(), p_26887_.getZ());
            if (d2 > d1 * d1) {
               return false;
            }
         }

         if (this.checkLineOfSight && p_26886_ instanceof Mob mob && !mob.getSensing().hasLineOfSight(p_26887_)) {
            return false;
         }
      }

      return true;
   }

   @FunctionalInterface
   public interface Selector {
      boolean test(LivingEntity var1, ServerLevel var2);
   }
}
