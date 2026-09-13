package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FleeSunGoal extends Goal {
   protected final PathfinderMob mob;
   private double wantedX;
   private double wantedY;
   private double wantedZ;
   private final double speedModifier;
   private final Level level;

   public FleeSunGoal(final PathfinderMob mob, final double speedModifier) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.level = mob.level();
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (this.mob.getTarget() != null) {
         return false;
      } else if (!this.level.isBrightOutside()) {
         return false;
      } else if (!this.mob.isOnFire()) {
         return false;
      } else if (!this.level.canSeeSky(this.mob.blockPosition())) {
         return false;
      } else {
         return !this.mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? false : this.setWantedPos();
      }
   }

   protected boolean setWantedPos() {
      Vec3 pos = this.getHidePos();
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

   protected @Nullable Vec3 getHidePos() {
      RandomSource random = this.mob.getRandom();
      BlockPos pos = this.mob.blockPosition();

      for (int i = 0; i < 10; i++) {
         BlockPos randomPos = pos.offset(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
         if (!this.level.canSeeSky(randomPos) && this.mob.getWalkTargetValue(randomPos) < 0.0F) {
            return Vec3.atBottomCenterOf(randomPos);
         }
      }

      return null;
   }
}
