package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LookControl implements Control {
   protected final Mob mob;
   protected float yMaxRotSpeed;
   protected float xMaxRotAngle;
   protected int lookAtCooldown;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;

   public LookControl(final Mob mob) {
      this.mob = mob;
   }

   public void setLookAt(final Vec3 vec) {
      this.setLookAt(vec.x, vec.y, vec.z);
   }

   public void setLookAt(final Entity target) {
      this.setLookAt(target.getX(), target.getEyeY(), target.getZ());
   }

   public void setLookAt(final Entity target, final float yMaxRotSpeed, final float xMaxRotAngle) {
      this.setLookAt(target.getX(), target.getEyeY(), target.getZ(), yMaxRotSpeed, xMaxRotAngle);
   }

   public void setLookAt(final double x, final double y, final double z) {
      this.setLookAt(x, y, z, this.mob.getHeadRotSpeed(), this.mob.getMaxHeadXRot());
   }

   public void setLookAt(final double x, final double y, final double z, final float yMaxRotSpeed, final float xMaxRotAngle) {
      this.wantedX = x;
      this.wantedY = y;
      this.wantedZ = z;
      this.yMaxRotSpeed = yMaxRotSpeed;
      this.xMaxRotAngle = xMaxRotAngle;
      this.lookAtCooldown = 2;
   }

   public void tick() {
      if (this.resetXRotOnTick()) {
         this.mob.setXRot(0.0F);
      }

      if (this.lookAtCooldown > 0) {
         this.lookAtCooldown--;
         this.getYRotD().ifPresent(yRotD -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, yRotD, this.yMaxRotSpeed));
         this.getXRotD().ifPresent(xRotD -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), xRotD, this.xMaxRotAngle)));
      } else {
         this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
      }

      this.clampHeadRotationToBody();
   }

   protected void clampHeadRotationToBody() {
      if (!this.mob.getNavigation().isDone()) {
         this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
      }
   }

   protected boolean resetXRotOnTick() {
      return true;
   }

   public boolean isLookingAtTarget() {
      return this.lookAtCooldown > 0;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected Optional<Float> getXRotD() {
      double xd = this.wantedX - this.mob.getX();
      double yd = this.wantedY - this.mob.getEyeY();
      double zd = this.wantedZ - this.mob.getZ();
      double sd = Math.sqrt(xd * xd + zd * zd);
      return !(Math.abs(yd) > 1.0E-5F) && !(Math.abs(sd) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(-(Mth.atan2(yd, sd) * 180.0F / (float)Math.PI)));
   }

   protected Optional<Float> getYRotD() {
      double xd = this.wantedX - this.mob.getX();
      double zd = this.wantedZ - this.mob.getZ();
      return !(Math.abs(zd) > 1.0E-5F) && !(Math.abs(xd) > 1.0E-5F)
         ? Optional.empty()
         : Optional.of((float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F);
   }
}
