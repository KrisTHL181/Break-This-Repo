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

   public TargetGoal(Mob p_26140_, boolean p_26141_) {
      this(p_26140_, p_26141_, false);
   }

   public TargetGoal(Mob p_26143_, boolean p_26144_, boolean p_26145_) {
      this.mob = p_26143_;
      this.mustSee = p_26144_;
      this.mustReach = p_26145_;
   }

   @Override
   public boolean canContinueToUse() {
      LivingEntity livingentity = this.mob.getTarget();
      if (livingentity == null) {
         livingentity = this.targetMob;
      }

      if (livingentity == null) {
         return false;
      }

      if (!this.mob.canAttack(livingentity)) {
         return false;
      }

      Team team = this.mob.getTeam();
      Team team1 = livingentity.getTeam();
      if (team != null && team1 == team) {
         return false;
      }

      double d0 = this.getFollowDistance();
      if (this.mob.distanceToSqr(livingentity) > d0 * d0) {
         return false;
      }

      if (this.mustSee) {
         if (this.mob.getSensing().hasLineOfSight(livingentity)) {
            this.unseenTicks = 0;
         } else if (++this.unseenTicks > reducedTickDelay(this.unseenMemoryTicks)) {
            return false;
         }
      }

      this.mob.setTarget(livingentity);
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

   protected boolean canAttack(@Nullable LivingEntity p_26151_, TargetingConditions p_26152_) {
      if (p_26151_ == null) {
         return false;
      }

      if (!p_26152_.test(getServerLevel(this.mob), this.mob, p_26151_)) {
         return false;
      }

      if (!this.mob.isWithinHome(p_26151_.blockPosition())) {
         return false;
      }

      if (this.mustReach) {
         if (--this.reachCacheTime <= 0) {
            this.reachCache = 0;
         }

         if (this.reachCache == 0) {
            this.reachCache = this.canReach(p_26151_) ? 1 : 2;
         }

         if (this.reachCache == 2) {
            return false;
         }
      }

      return true;
   }

   private boolean canReach(LivingEntity p_26149_) {
      this.reachCacheTime = reducedTickDelay(10 + this.mob.getRandom().nextInt(5));
      Path path = this.mob.getNavigation().createPath(p_26149_, 0);
      if (path == null) {
         return false;
      }

      Node node = path.getEndNode();
      if (node == null) {
         return false;
      }

      int i = node.x - p_26149_.getBlockX();
      int j = node.z - p_26149_.getBlockZ();
      return i * i + j * j <= 2.25;
   }

   public TargetGoal setUnseenMemoryTicks(int p_26147_) {
      this.unseenMemoryTicks = p_26147_;
      return this;
   }
}
