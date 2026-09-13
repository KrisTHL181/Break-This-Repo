package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public abstract class PathfinderMob extends Mob {
   protected static final float DEFAULT_WALK_TARGET_VALUE = 0.0F;

   protected PathfinderMob(final EntityType<? extends PathfinderMob> type, final Level level) {
      super(type, level);
   }

   public float getWalkTargetValue(final BlockPos pos) {
      return this.getWalkTargetValue(pos, this.level());
   }

   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return 0.0F;
   }

   @Override
   public boolean checkSpawnRules(final LevelAccessor level, final EntitySpawnReason spawnReason) {
      return this.getWalkTargetValue(this.blockPosition(), level) >= 0.0F;
   }

   public boolean isPathFinding() {
      return !this.getNavigation().isDone();
   }

   public boolean isPanicking() {
      if (!this.brain.isBrainDead() && this.brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)) {
         return this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent();
      }

      for (WrappedGoal wrappedGoal : this.goalSelector.getAvailableGoals()) {
         if (wrappedGoal.isRunning() && wrappedGoal.getGoal() instanceof PanicGoal) {
            return true;
         }
      }

      return false;
   }

   protected boolean shouldStayCloseToLeashHolder() {
      return true;
   }

   @Override
   public void closeRangeLeashBehaviour(final Entity leashHolder) {
      super.closeRangeLeashBehaviour(leashHolder);
      if (this.shouldStayCloseToLeashHolder() && !this.isPanicking()) {
         this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
         float wantedDistance = 2.0F;
         float distanceTo = this.distanceTo(leashHolder);
         Vec3 delta = new Vec3(leashHolder.getX() - this.getX(), leashHolder.getY() - this.getY(), leashHolder.getZ() - this.getZ())
            .normalize()
            .scale(Math.max(distanceTo - 2.0F, 0.0F));
         this.getNavigation().moveTo(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z, this.followLeashSpeed());
      }
   }

   @Override
   public void whenLeashedTo(final Entity leashHolder) {
      this.setHomeTo(leashHolder.blockPosition(), (int)this.leashElasticDistance() - 1);
      super.whenLeashedTo(leashHolder);
   }

   protected double followLeashSpeed() {
      return 1.0;
   }
}
