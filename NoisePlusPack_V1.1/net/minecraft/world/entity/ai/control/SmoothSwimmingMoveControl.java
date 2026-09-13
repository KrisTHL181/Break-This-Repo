package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SmoothSwimmingMoveControl extends MoveControl {
   private static final float FULL_SPEED_TURN_THRESHOLD = 10.0F;
   private static final float STOP_TURN_THRESHOLD = 60.0F;
   private final int maxTurnX;
   private final int maxTurnY;
   private final float inWaterSpeedModifier;
   private final float outsideWaterSpeedModifier;
   private final boolean applyGravity;

   public SmoothSwimmingMoveControl(Mob p_148070_, int p_148071_, int p_148072_, float p_148073_, float p_148074_, boolean p_148075_) {
      super(p_148070_);
      this.maxTurnX = p_148071_;
      this.maxTurnY = p_148072_;
      this.inWaterSpeedModifier = p_148073_;
      this.outsideWaterSpeedModifier = p_148074_;
      this.applyGravity = p_148075_;
   }

   @Override
   public void tick() {
      if (this.applyGravity && this.mob.isInWater()) {
         this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
      }

      if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
         double d0 = this.wantedX - this.mob.getX();
         double d1 = this.wantedY - this.mob.getY();
         double d2 = this.wantedZ - this.mob.getZ();
         double d3 = d0 * d0 + d1 * d1 + d2 * d2;
         if (d3 < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
         } else {
            float f = (float)(Mth.atan2(d2, d0) * 180.0 / (float) Math.PI) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, this.maxTurnY));
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
            float f1 = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (this.mob.isInWater()) {
               this.mob.setSpeed(f1 * this.inWaterSpeedModifier);
               double d4 = Math.sqrt(d0 * d0 + d2 * d2);
               if (Math.abs(d1) > 1.0E-5F || Math.abs(d4) > 1.0E-5F) {
                  float f3 = -((float)(Mth.atan2(d1, d4) * 180.0 / (float) Math.PI));
                  f3 = Mth.clamp(Mth.wrapDegrees(f3), -this.maxTurnX, this.maxTurnX);
                  this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), f3, 5.0F));
               }

               float f6 = Mth.cos(this.mob.getXRot() * (float) (Math.PI / 180.0));
               float f4 = Mth.sin(this.mob.getXRot() * (float) (Math.PI / 180.0));
               this.mob.zza = f6 * f1;
               this.mob.yya = -f4 * f1;
            } else {
               float f5 = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - f));
               float f2 = getTurningSpeedFactor(f5);
               this.mob.setSpeed(f1 * this.outsideWaterSpeedModifier * f2);
            }
         }
      } else {
         this.mob.setSpeed(0.0F);
         this.mob.setXxa(0.0F);
         this.mob.setYya(0.0F);
         this.mob.setZza(0.0F);
      }
   }

   private static float getTurningSpeedFactor(float p_249853_) {
      return 1.0F - Mth.clamp((p_249853_ - 10.0F) / 50.0F, 0.0F, 1.0F);
   }
}
