package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
   protected final AbstractMinecart minecart;

   protected MinecartBehavior(AbstractMinecart p_459098_) {
      this.minecart = p_459098_;
   }

   public InterpolationHandler getInterpolation() {
      return null;
   }

   public void lerpMotion(Vec3 p_456840_) {
      this.setDeltaMovement(p_456840_);
   }

   public abstract void tick();

   public Level level() {
      return this.minecart.level();
   }

   public abstract void moveAlongTrack(ServerLevel var1);

   public abstract double stepAlongTrack(BlockPos var1, RailShape var2, double var3);

   public abstract boolean pushAndPickupEntities();

   public Vec3 getDeltaMovement() {
      return this.minecart.getDeltaMovement();
   }

   public void setDeltaMovement(Vec3 p_452074_) {
      this.minecart.setDeltaMovement(p_452074_);
   }

   public void setDeltaMovement(double p_460988_, double p_460782_, double p_455178_) {
      this.minecart.setDeltaMovement(p_460988_, p_460782_, p_455178_);
   }

   public Vec3 position() {
      return this.minecart.position();
   }

   public double getX() {
      return this.minecart.getX();
   }

   public double getY() {
      return this.minecart.getY();
   }

   public double getZ() {
      return this.minecart.getZ();
   }

   public void setPos(Vec3 p_452786_) {
      this.minecart.setPos(p_452786_);
   }

   public void setPos(double p_459820_, double p_458300_, double p_452733_) {
      this.minecart.setPos(p_459820_, p_458300_, p_452733_);
   }

   public float getXRot() {
      return this.minecart.getXRot();
   }

   public void setXRot(float p_450914_) {
      this.minecart.setXRot(p_450914_);
   }

   public float getYRot() {
      return this.minecart.getYRot();
   }

   public void setYRot(float p_454907_) {
      this.minecart.setYRot(p_454907_);
   }

   public Direction getMotionDirection() {
      return this.minecart.getDirection();
   }

   public Vec3 getKnownMovement(Vec3 p_459988_) {
      return p_459988_;
   }

   public abstract double getMaxSpeed(ServerLevel var1);

   public abstract double getSlowdownFactor();
}
