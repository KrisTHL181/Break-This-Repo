package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class MoveTowardsRestrictionGoal extends Goal {
   private final PathfinderMob mob;
   private double wantedX;
   private double wantedY;
   private double wantedZ;
   private final double speedModifier;

   public MoveTowardsRestrictionGoal(final PathfinderMob mob, final double moveSpeedModifier) {
      this.mob = mob;
      this.speedModifier = moveSpeedModifier;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (this.mob.isWithinHome()) {
         return false;
      }

      Vec3 pos = DefaultRandomPos.getPosTowards(this.mob, 16, 7, Vec3.atBottomCenterOf(this.mob.getHomePosition()), (float) (Math.PI / 2));
      if (pos == null) {
         return false;
      }

      this.wantedX = pos.x;
      this.wantedY = pos.y;
      this.wantedZ = pos.z;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.getNavigation().isDone();
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
   }
}
