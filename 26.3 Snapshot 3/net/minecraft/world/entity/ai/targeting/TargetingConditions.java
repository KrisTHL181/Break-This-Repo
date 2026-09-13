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

   private TargetingConditions(final boolean isCombat) {
      this.isCombat = isCombat;
   }

   public static TargetingConditions forCombat() {
      return new TargetingConditions(true);
   }

   public static TargetingConditions forNonCombat() {
      return new TargetingConditions(false);
   }

   public TargetingConditions copy() {
      TargetingConditions clone = this.isCombat ? forCombat() : forNonCombat();
      clone.range = this.range;
      clone.checkLineOfSight = this.checkLineOfSight;
      clone.testInvisible = this.testInvisible;
      clone.selector = this.selector;
      return clone;
   }

   public TargetingConditions range(final double range) {
      this.range = range;
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

   public TargetingConditions selector(final TargetingConditions.@Nullable Selector selector) {
      this.selector = selector;
      return this;
   }

   public boolean test(final ServerLevel level, final @Nullable LivingEntity targeter, final LivingEntity target) {
      if (targeter == target) {
         return false;
      }

      if (!target.canBeSeenByAnyone()) {
         return false;
      }

      if (this.selector != null && !this.selector.test(target, level)) {
         return false;
      }

      if (targeter == null) {
         if (this.isCombat && (!target.canBeSeenAsEnemy() || level.getDifficulty() == Difficulty.PEACEFUL)) {
            return false;
         }
      } else {
         if (this.isCombat && (!targeter.canAttack(target) || targeter.isAlliedTo(target))) {
            return false;
         }

         if (this.range > 0.0) {
            double modifier = this.testInvisible ? target.getVisibilityPercent(targeter) : 1.0;
            double visibilityDistance = Math.max(this.range * modifier, 2.0);
            double distanceToSqr = targeter.distanceToSqr(target.getX(), target.getY(), target.getZ());
            if (distanceToSqr > visibilityDistance * visibilityDistance) {
               return false;
            }
         }

         if (this.checkLineOfSight && targeter instanceof Mob mob && !mob.getSensing().hasLineOfSight(target)) {
            return false;
         }
      }

      return true;
   }

   @FunctionalInterface
   public interface Selector {
      boolean test(LivingEntity target, ServerLevel level);
   }
}
