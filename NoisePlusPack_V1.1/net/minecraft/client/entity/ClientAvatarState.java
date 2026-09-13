package net.minecraft.client.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public void tick(Vec3 p_424410_, Vec3 p_425687_) {
      this.walkDistO = this.walkDist;
      this.deltaMovementOnPreviousTick = p_425687_;
      this.moveCloak(p_424410_);
   }

   public void addWalkDistance(float p_429111_) {
      this.walkDist += p_429111_;
   }

   public Vec3 deltaMovementOnPreviousTick() {
      return this.deltaMovementOnPreviousTick;
   }

   private void moveCloak(Vec3 p_429318_) {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double d0 = p_429318_.x() - this.xCloak;
      double d1 = p_429318_.y() - this.yCloak;
      double d2 = p_429318_.z() - this.zCloak;
      double d3 = 10.0;
      if (!(d0 > 10.0) && !(d0 < -10.0)) {
         this.xCloak += d0 * 0.25;
      } else {
         this.xCloak = p_429318_.x();
         this.xCloakO = this.xCloak;
      }

      if (!(d1 > 10.0) && !(d1 < -10.0)) {
         this.yCloak += d1 * 0.25;
      } else {
         this.yCloak = p_429318_.y();
         this.yCloakO = this.yCloak;
      }

      if (!(d2 > 10.0) && !(d2 < -10.0)) {
         this.zCloak += d2 * 0.25;
      } else {
         this.zCloak = p_429318_.z();
         this.zCloakO = this.zCloak;
      }
   }

   public double getInterpolatedCloakX(float p_430671_) {
      return Mth.lerp(p_430671_, this.xCloakO, this.xCloak);
   }

   public double getInterpolatedCloakY(float p_430966_) {
      return Mth.lerp(p_430966_, this.yCloakO, this.yCloak);
   }

   public double getInterpolatedCloakZ(float p_431316_) {
      return Mth.lerp(p_431316_, this.zCloakO, this.zCloak);
   }

   public void updateBob(float p_428433_) {
      this.bobO = this.bob;
      this.bob = this.bob + (p_428433_ - this.bob) * 0.4F;
   }

   public void resetBob() {
      this.bobO = this.bob;
      this.bob = 0.0F;
   }

   public float getInterpolatedBob(float p_422599_) {
      return Mth.lerp(p_422599_, this.bobO, this.bob);
   }

   public float getBackwardsInterpolatedWalkDistance(float p_429624_) {
      float f = this.walkDist - this.walkDistO;
      return -(this.walkDist + f * p_429624_);
   }

   public float getInterpolatedWalkDistance(float p_426726_) {
      return Mth.lerp(p_426726_, this.walkDistO, this.walkDist);
   }
}
