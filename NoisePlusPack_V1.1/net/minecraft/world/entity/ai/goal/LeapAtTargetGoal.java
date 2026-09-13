package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LeapAtTargetGoal extends Goal {
   private final Mob mob;
   private LivingEntity target;
   private final float yd;

   public LeapAtTargetGoal(Mob p_25492_, float p_25493_) {
      this.mob = p_25492_;
      this.yd = p_25493_;
      this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (this.mob.hasControllingPassenger()) {
         return false;
      } else {
         this.target = this.mob.getTarget();
         if (this.target == null) {
            return false;
         } else {
            double d0 = this.mob.distanceToSqr(this.target);
            if (!(d0 < 4.0) && !(d0 > 16.0)) {
               return !this.mob.onGround() ? false : this.mob.getRandom().nextInt(reducedTickDelay(5)) == 0;
            } else {
               return false;
            }
         }
      }
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.onGround();
   }

   @Override
   public void start() {
      Vec3 vec3 = this.mob.getDeltaMovement();
      Vec3 vec31 = new Vec3(this.target.getX() - this.mob.getX(), 0.0, this.target.getZ() - this.mob.getZ());
      if (vec31.lengthSqr() > 1.0E-7) {
         vec31 = vec31.normalize().scale(0.4).add(vec3.scale(0.2));
      }

      this.mob.setDeltaMovement(vec31.x, this.yd, vec31.z);
   }
}
