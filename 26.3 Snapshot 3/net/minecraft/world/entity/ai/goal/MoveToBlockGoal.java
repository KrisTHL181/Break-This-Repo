package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.LevelReader;

public abstract class MoveToBlockGoal extends Goal {
   private static final int GIVE_UP_TICKS = 1200;
   private static final int STAY_TICKS = 1200;
   private static final int INTERVAL_TICKS = 200;
   protected final PathfinderMob mob;
   public final double speedModifier;
   protected int nextStartTick;
   protected int tryTicks;
   private int maxStayTicks;
   protected BlockPos blockPos = BlockPos.ZERO;
   private boolean reachedTarget;
   private final int searchRange;
   private final int verticalSearchRange;
   protected int verticalSearchStart;

   public MoveToBlockGoal(final PathfinderMob mob, final double speedModifier, final int searchRange) {
      this(mob, speedModifier, searchRange, 1);
   }

   public MoveToBlockGoal(final PathfinderMob mob, final double speedModifier, final int searchRange, final int verticalSearchRange) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.searchRange = searchRange;
      this.verticalSearchStart = 0;
      this.verticalSearchRange = verticalSearchRange;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
   }

   @Override
   public boolean canUse() {
      if (this.nextStartTick > 0) {
         this.nextStartTick--;
         return false;
      } else {
         this.nextStartTick = this.nextStartTick(this.mob);
         return this.findNearestBlock();
      }
   }

   protected int nextStartTick(final PathfinderMob mob) {
      return reducedTickDelay(200 + mob.getRandom().nextInt(200));
   }

   @Override
   public boolean canContinueToUse() {
      return this.tryTicks >= -this.maxStayTicks && this.tryTicks <= 1200 && this.isValidTarget(this.mob.level(), this.blockPos);
   }

   @Override
   public void start() {
      this.moveMobToBlock();
      this.tryTicks = 0;
      this.maxStayTicks = this.mob.getRandom().nextInt(this.mob.getRandom().nextInt(1200) + 1200) + 1200;
   }

   protected void moveMobToBlock() {
      this.mob.getNavigation().moveTo(this.blockPos.getX() + 0.5, this.blockPos.getY() + 1, this.blockPos.getZ() + 0.5, this.speedModifier);
   }

   public double acceptedDistance() {
      return 1.0;
   }

   protected BlockPos getMoveToTarget() {
      return this.blockPos.above();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      BlockPos moveToTarget = this.getMoveToTarget();
      if (!moveToTarget.closerToCenterThan(this.mob.position(), this.acceptedDistance())) {
         this.reachedTarget = false;
         this.tryTicks++;
         if (this.shouldRecalculatePath()) {
            this.mob.getNavigation().moveTo(moveToTarget.getX() + 0.5, moveToTarget.getY(), moveToTarget.getZ() + 0.5, this.speedModifier);
         }
      } else {
         this.reachedTarget = true;
         this.tryTicks--;
      }
   }

   public boolean shouldRecalculatePath() {
      return this.tryTicks % 40 == 0;
   }

   protected boolean isReachedTarget() {
      return this.reachedTarget;
   }

   protected boolean findNearestBlock() {
      int horizontalSearch = this.searchRange;
      int verticalSearch = this.verticalSearchRange;
      BlockPos mobPos = this.mob.blockPosition();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int y = this.verticalSearchStart; y <= verticalSearch; y = y > 0 ? -y : 1 - y) {
         for (int r = 0; r < horizontalSearch; r++) {
            for (int x = 0; x <= r; x = x > 0 ? -x : 1 - x) {
               for (int z = x < r && x > -r ? r : 0; z <= r; z = z > 0 ? -z : 1 - z) {
                  pos.setWithOffset(mobPos, x, y - 1, z);
                  if (this.mob.isWithinHome(pos) && this.isValidTarget(this.mob.level(), pos)) {
                     this.blockPos = pos;
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   protected abstract boolean isValidTarget(LevelReader level, BlockPos pos);
}
