package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public abstract class TargetGoal extends Goal {
   private static final int EMPTY_REACH_CACHE = 0;
   private static final int CAN_REACH_CACHE = 1;
   private static final int CANT_REACH_CACHE = 2;
   protected final Mob mob;
   protected final boolean mustSee;
   private final boolean mustReach;
   private int reachCache;
   private int reachCacheTime;
   private int unseenTicks;
   protected @Nullable LivingEntity targetMob;
   protected int unseenMemoryTicks = 60;

   public TargetGoal(final Mob mob, final boolean mustSee) {
      this(mob, mustSee, false);
   }

   public TargetGoal(final Mob mob, final boolean mustSee, final boolean mustReach) {
      this.mob = mob;
      this.mustSee = mustSee;
      this.mustReach = mustReach;
   }

   @Override
   public boolean canContinueToUse() {
      LivingEntity target = this.mob.getTarget();
      if (target == null) {
         target = this.targetMob;
      }

      if (target == null) {
         return false;
      }

      if (!this.mob.canAttack(target)) {
         return false;
      }

      Team mobTeam = this.mob.getTeam();
      Team targetTeam = target.getTeam();
      if (mobTeam != null && targetTeam == mobTeam) {
         return false;
      }

      double within = this.getFollowDistance();
      if (this.mob.distanceToSqr(target) > within * within) {
         return false;
      }

      if (this.mustSee) {
         if (this.mob.getSensing().hasLineOfSight(target)) {
            this.unseenTicks = 0;
         } else if (++this.unseenTicks > reducedTickDelay(this.unseenMemoryTicks)) {
            return false;
         }
      }

      this.mob.setTarget(target);
      return true;
   }

   protected double getFollowDistance() {
      return this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
   }

   @Override
   public void start() {
      this.reachCache = 0;
      this.reachCacheTime = 0;
      this.unseenTicks = 0;
   }

   @Override
   public void stop() {
      this.mob.setTarget(null);
      this.targetMob = null;
   }

   protected boolean canAttack(final @Nullable LivingEntity target, final TargetingConditions targetConditions) {
      if (target == null) {
         return false;
      }

      if (!targetConditions.test(getServerLevel(this.mob), this.mob, target)) {
         return false;
      }

      if (!this.mob.isWithinHome(target.blockPosition())) {
         return false;
      }

      if (this.mustReach) {
         if (--this.reachCacheTime <= 0) {
            this.reachCache = 0;
         }

         if (this.reachCache == 0) {
            this.reachCache = this.canReach(target) ? 1 : 2;
         }

         if (this.reachCache == 2) {
            return false;
         }
      }

      return true;
   }

   private boolean canReach(final LivingEntity target) {
      this.reachCacheTime = reducedTickDelay(10 + this.mob.getRandom().nextInt(5));
      Path path = this.mob.getNavigation().createPath(target, 0);
      if (path == null) {
         return false;
      }

      Node last = path.getEndNode();
      if (last == null) {
         return false;
      }

      int xx = last.x - target.getBlockX();
      int zz = last.z - target.getBlockZ();
      return xx * xx + zz * zz <= 2.25;
   }

   public TargetGoal setUnseenMemoryTicks(final int unseenMemoryTicks) {
      this.unseenMemoryTicks = unseenMemoryTicks;
      return this;
   }
}
