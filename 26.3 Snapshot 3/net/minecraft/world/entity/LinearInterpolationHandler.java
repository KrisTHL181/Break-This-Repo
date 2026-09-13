package net.minecraft.world.entity;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LinearInterpolationHandler extends InterpolationHandler {
   private final LinearInterpolationHandler.InterpolationData interpolationData = new LinearInterpolationHandler.InterpolationData();

   public LinearInterpolationHandler(final Entity entity) {
      super(entity);
   }

   public LinearInterpolationHandler(final Entity entity, final int interpolationSteps) {
      super(entity, interpolationSteps);
   }

   public void setInterpolationLength(final int steps) {
      this.interpolationSteps = steps;
   }

   @Override
   protected PositionAndRotation.Mutable interpolationData() {
      return this.interpolationData;
   }

   @Override
   protected void startInterpolating(final PositionPath position, final float yRot, final float xRot) {
      this.interpolationData.set(position.endPosition(), yRot, xRot);
      this.interpolationData.remainingSteps = this.interpolationSteps;
   }

   @Override
   protected void doInterpolate() {
      double alpha = 1.0 / this.interpolationData.remainingSteps;
      Vec3 newPosition = this.entity.position().lerp(this.interpolationData.position(), alpha);
      float newYRot = (float)Mth.rotLerp(alpha, this.entity.getYRot(), this.interpolationData.yRot());
      float newXRot = (float)Mth.lerp(alpha, this.entity.getXRot(), this.interpolationData.xRot());
      this.entity.setPos(newPosition);
      this.entity.setRot(newYRot, newXRot);
      this.interpolationData.remainingSteps--;
   }

   @Override
   public boolean hasActiveInterpolation() {
      return this.interpolationData.remainingSteps > 0;
   }

   @Override
   public void cancel() {
      this.interpolationData.remainingSteps = 0;
   }

   private static class InterpolationData extends PositionAndRotation.Mutable {
      private int remainingSteps;
   }
}
