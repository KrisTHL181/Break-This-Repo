package net.minecraft.client.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ClientAvatarState {
   private Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
   private float walkDist;
   private float walkDistO;
   private double xCloak;
   private double yCloak;
   private double zCloak;
   private double xCloakO;
   private double yCloakO;
   private double zCloakO;
   private float bob;
   private float bobO;

   public void tick(final Vec3 position, final Vec3 deltaMovement) {
      this.walkDistO = this.walkDist;
      this.deltaMovementOnPreviousTick = deltaMovement;
      this.moveCloak(position);
   }

   public void addWalkDistance(final float added) {
      this.walkDist += added;
   }

   public Vec3 deltaMovementOnPreviousTick() {
      return this.deltaMovementOnPreviousTick;
   }

   private void moveCloak(final Vec3 pos) {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double x = pos.x() - this.xCloak;
      double y = pos.y() - this.yCloak;
      double z = pos.z() - this.zCloak;
      double teleportThreshold = 10.0;
      if (!(x > 10.0) && !(x < -10.0)) {
         this.xCloak += x * 0.25;
      } else {
         this.xCloak = pos.x();
         this.xCloakO = this.xCloak;
      }

      if (!(y > 10.0) && !(y < -10.0)) {
         this.yCloak += y * 0.25;
      } else {
         this.yCloak = pos.y();
         this.yCloakO = this.yCloak;
      }

      if (!(z > 10.0) && !(z < -10.0)) {
         this.zCloak += z * 0.25;
      } else {
         this.zCloak = pos.z();
         this.zCloakO = this.zCloak;
      }
   }

   public double getInterpolatedCloakX(final float partialTicks) {
      return Mth.lerp(partialTicks, this.xCloakO, this.xCloak);
   }

   public double getInterpolatedCloakY(final float partialTicks) {
      return Mth.lerp(partialTicks, this.yCloakO, this.yCloak);
   }

   public double getInterpolatedCloakZ(final float partialTicks) {
      return Mth.lerp(partialTicks, this.zCloakO, this.zCloak);
   }

   public void updateBob(final float tBob) {
      this.bobO = this.bob;
      this.bob = this.bob + (tBob - this.bob) * 0.4F;
   }

   public void resetBob() {
      this.bobO = this.bob;
      this.bob = 0.0F;
   }

   public float getInterpolatedBob(final float partialTicks) {
      return Mth.lerp(partialTicks, this.bobO, this.bob);
   }

   public float getBackwardsInterpolatedWalkDistance(final float partialTicks) {
      float wda = this.walkDist - this.walkDistO;
      return -(this.walkDist + wda * partialTicks);
   }

   public float getInterpolatedWalkDistance(final float partialTicks) {
      return Mth.lerp(partialTicks, this.walkDistO, this.walkDist);
   }
}
