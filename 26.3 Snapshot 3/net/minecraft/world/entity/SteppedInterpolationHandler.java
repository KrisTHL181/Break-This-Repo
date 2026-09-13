package net.minecraft.world.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SteppedInterpolationHandler extends InterpolationHandler {
   private final SteppedInterpolationHandler.InterpolationData interpolationData = new SteppedInterpolationHandler.InterpolationData();
   private final SteppedInterpolationTracker interpolationTracker;

   public SteppedInterpolationHandler(final Entity entity) {
      super(entity);
      this.interpolationTracker = new SteppedInterpolationTracker(entity);
   }

   @Override
   public InterpolationTracker interpolationTracker() {
      return this.interpolationTracker;
   }

   @Override
   protected PositionAndRotation.Mutable interpolationData() {
      return this.interpolationData;
   }

   @Override
   protected void startInterpolating(final PositionPath position, final float yRot, final float xRot) {
      Vec3 endPosition = position.endPosition();
      if (!Objects.equals(endPosition, this.interpolationData.position())) {
         this.interpolationData.addSteps(position, this.entity.position(), this.interpolationSteps);
      }

      int rotationSteps = this.interpolationSteps;
      // ===== 修改：直接使用模式变量，移除 var14 等 =====
      if (position instanceof PositionPath.Stepped stepped) {
         stepped.endPosition(); // 保留原副作用（若有）
         List<PositionStep> steps = stepped.steps();
         rotationSteps = 0;
         for (PositionStep step : steps) {
            rotationSteps += step.tickOffset();
         }
      }

      this.interpolationData.set(endPosition, yRot, xRot);
      this.interpolationData.remainingRotationSteps = rotationSteps;
   }

   @Override
   protected void doInterpolate() {
      Vec3 newPosition = this.interpolationData.getNewPosition();
      float alpha = 1.0F / Math.max(this.interpolationData.remainingRotationSteps, 1.0F);
      float newYRot = Mth.rotLerp(alpha, this.entity.getYRot(), this.interpolationData.yRot());
      float newXRot = Mth.lerp(alpha, this.entity.getXRot(), this.interpolationData.xRot());
      this.entity.setPos(newPosition);
      this.entity.setRot(newYRot, newXRot);
      float tick = this.entity.level().getRelativeTickSpeed();
      this.interpolationData.currentStepTicks += tick;
      this.interpolationData.remainingRotationSteps -= tick;
   }

   @Override
   public void applyPredictedMovement(final Vec3 delta) {
      super.applyPredictedMovement(delta);
      if (!this.entity.level().isClientSide()) {
         this.interpolationTracker.applyPredictedMovement(delta);
      }
   }

   @Override
   public boolean hasActiveInterpolation() {
      return this.interpolationData.remainingRotationSteps > 0.0F || !this.interpolationData.remainingSteps.isEmpty();
   }

   @Override
   public void cancel() {
      this.interpolationData.remainingSteps.clear();
      this.interpolationData.remainingRotationSteps = 0.0F;
   }

   private static class InterpolationData extends PositionAndRotation.Mutable {
      private final LinkedList<PositionStep> remainingSteps = new LinkedList<>();
      private Vec3 lastStepPosition = Vec3.ZERO;
      private float currentStepTicks;
      private float remainingRotationSteps;

      @Override
      public void addDelta(final Vec3 delta) {
         super.addDelta(delta);
         this.remainingSteps.replaceAll(step -> step.addDelta(delta));
         this.lastStepPosition = this.lastStepPosition.add(delta);
      }

      private void addSteps(final PositionPath position, final Vec3 startingPosition, final int interpolationSteps) {
         if (this.remainingSteps.isEmpty()) {
            this.lastStepPosition = startingPosition;
            this.currentStepTicks = 1.0F;
         }

         // ===== 修改：直接使用模式变量，移除 var16 等 =====
         switch (position) {
            case PositionPath.Linear(Vec3 pos):
               this.remainingSteps.add(new PositionStep(pos, interpolationSteps));
               break;
            case PositionPath.Stepped stepped:
               stepped.endPosition(); // 保留原副作用
               List<PositionStep> steps = stepped.steps();
               this.remainingSteps.addAll(steps);
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      private Vec3 getNewPosition() {
         while (!this.remainingSteps.isEmpty()) {
            PositionStep step = this.remainingSteps.getFirst();
            int offset = step.tickOffset();
            if (this.currentStepTicks < offset) {
               double a = this.currentStepTicks / offset;
               return this.lastStepPosition.lerp(step.position(), a);
            }

            this.currentStepTicks -= offset;
            this.lastStepPosition = step.position();
            this.remainingSteps.removeFirst();
         }

         return this.position();
      }
   }
}