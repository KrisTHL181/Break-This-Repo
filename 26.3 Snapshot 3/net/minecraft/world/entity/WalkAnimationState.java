package net.minecraft.world.entity;

import net.minecraft.util.Mth;

public class WalkAnimationState {
   private float speedOld;
   private float speed;
   private float position;
   private float positionScale = 1.0F;

   public void setSpeed(final float speed) {
      this.speed = speed;
   }

   public void update(final float targetSpeed, final float factor, final float positionScale) {
      this.speedOld = this.speed;
      this.speed = this.speed + (targetSpeed - this.speed) * factor;
      this.position = this.position + this.speed;
      this.positionScale = positionScale;
   }

   public void stop() {
      this.speedOld = 0.0F;
      this.speed = 0.0F;
      this.position = 0.0F;
   }

   public float speed() {
      return this.speed;
   }

   public float speed(final float partialTicks) {
      return Math.min(Mth.lerp(partialTicks, this.speedOld, this.speed), 1.0F);
   }

   public float position() {
      return this.position * this.positionScale;
   }

   public float position(final float partialTicks) {
      return (this.position - this.speed * (1.0F - partialTicks)) * this.positionScale;
   }

   public boolean isMoving() {
      return this.speed > 1.0E-5F;
   }
}
