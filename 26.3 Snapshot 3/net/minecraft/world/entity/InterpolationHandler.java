package net.minecraft.world.entity;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class InterpolationHandler {
   public static final int DEFAULT_INTERPOLATION_STEPS = 3;
   protected final Entity entity;
   protected int interpolationSteps;
   protected final PositionAndRotation.Mutable lastPositionAndRotation = new PositionAndRotation.Mutable();

   public InterpolationHandler(final Entity entity) {
      this(entity, 3);
   }

   public InterpolationHandler(final Entity entity, final int interpolationSteps) {
      this.entity = entity;
      this.interpolationSteps = interpolationSteps;
   }

   public @Nullable InterpolationTracker interpolationTracker() {
      return null;
   }

   protected abstract PositionAndRotation.Mutable interpolationData();

   protected abstract void startInterpolating(PositionPath position, float yRot, float xRot);

   protected abstract void doInterpolate();

   public abstract boolean hasActiveInterpolation();

   public abstract void cancel();

   public PositionAndRotation getCurrentPositionAndRotation() {
      return this.hasActiveInterpolation() ? this.interpolationData().immutable() : this.entity.storePositionAndRotation();
   }

   public void interpolateTo(final PositionPath position, final float yRot, final float xRot) {
      if (this.interpolationSteps == 0) {
         this.entity.snapTo(position.endPosition(), yRot, xRot);
         this.cancel();
      } else if (!this.hasActiveInterpolation() || !this.interpolationData().is(position.endPosition(), yRot, xRot)) {
         this.startInterpolating(position, yRot, xRot);
         this.setLastPositionAndRotation();
         this.entity.onInterpolationStart(this);
      }
   }

   public void applyPredictedMovement(final Vec3 delta) {
      this.interpolationData().addDelta(delta);
      this.lastPositionAndRotation.addDelta(delta);
   }

   public void interpolate() {
      if (!this.hasActiveInterpolation()) {
         this.cancel();
      } else {
         this.adjustInterpolationTargetFromDeltas();
         this.doInterpolate();
         this.setLastPositionAndRotation();
      }
   }

   private void adjustInterpolationTargetFromDeltas() {
      Vec3 deltaSinceLastInterpolation = this.entity.position().subtract(this.lastPositionAndRotation.position());
      if (deltaSinceLastInterpolation.lengthSqr() > 1.0E-5F) {
         Vec3 adjustedPosition = this.interpolationData().position().add(deltaSinceLastInterpolation);
         if (this.entity.level().noCollision(this.entity, this.entity.makeBoundingBox(adjustedPosition))) {
            this.interpolationData().addDelta(deltaSinceLastInterpolation);
         }
      }

      float deltaYRotSinceLastInterpolation = this.entity.getYRot() - this.lastPositionAndRotation.yRot();
      float deltaXRotSinceLastInterpolation = this.entity.getXRot() - this.lastPositionAndRotation.xRot();
      this.interpolationData().addRotation(deltaYRotSinceLastInterpolation, deltaXRotSinceLastInterpolation);
   }

   private void setLastPositionAndRotation() {
      this.lastPositionAndRotation.set(this.entity.position(), this.entity.getYRot(), this.entity.getXRot());
   }
}
