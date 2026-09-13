package net.minecraft.world.entity;

import net.minecraft.util.Mth;

public class WalkAnimationState {
   private float speedOld;
   private float speed;
   private float position;
   private float positionScale = 1.0F;

   public void setSpeed(float p_268265_) {
      this.speed = p_268265_;
   }

   public void update(float p_267993_, float p_267967_, float p_362382_) {
      this.speedOld = this.speed;
      this.speed = this.speed + (p_267993_ - this.speed) * p_267967_;
      this.position = this.position + this.speed;
      this.positionScale = p_362382_;
   }

   public void stop() {
      this.speedOld = 0.0F;
      this.speed = 0.0F;
      this.position = 0.0F;
   }

   public float speed() {
      return this.speed;
   }

   public float speed(float p_268054_) {
      return Math.min(Mth.lerp(p_268054_, this.speedOld, this.speed), 1.0F);
   }

   public float position() {
      return this.position * this.positionScale;
   }

   public float position(float p_268007_) {
      return (this.position - this.speed * (1.0F - p_268007_)) * this.positionScale;
   }

   public boolean isMoving() {
      return this.speed > 1.0E-5F;
   }
}
