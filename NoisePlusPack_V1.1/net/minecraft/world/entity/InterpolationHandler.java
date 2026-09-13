package net.minecraft.world.entity;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class InterpolationHandler {
   public static final int DEFAULT_INTERPOLATION_STEPS = 3;
   private final Entity entity;
   private int interpolationSteps;
   private final InterpolationHandler.InterpolationData interpolationData = new InterpolationHandler.InterpolationData(0, Vec3.ZERO, 0.0F, 0.0F);
   private @Nullable Vec3 previousTickPosition;
   private @Nullable Vec2 previousTickRot;
   private final @Nullable Consumer<InterpolationHandler> onInterpolationStart;

   public InterpolationHandler(Entity p_393578_) {
      this(p_393578_, 3, null);
   }

   public InterpolationHandler(Entity p_394891_, int p_391973_) {
      this(p_394891_, p_391973_, null);
   }

   public InterpolationHandler(Entity p_394624_, @Nullable Consumer<InterpolationHandler> p_395379_) {
      this(p_394624_, 3, p_395379_);
   }

   public InterpolationHandler(Entity p_396416_, int p_391604_, @Nullable Consumer<InterpolationHandler> p_396596_) {
      this.interpolationSteps = p_391604_;
      this.entity = p_396416_;
      this.onInterpolationStart = p_396596_;
   }

   public Vec3 position() {
      return this.interpolationData.steps > 0 ? this.interpolationData.position : this.entity.position();
   }

   public float yRot() {
      return this.interpolationData.steps > 0 ? this.interpolationData.yRot : this.entity.getYRot();
   }

   public float xRot() {
      return this.interpolationData.steps > 0 ? this.interpolationData.xRot : this.entity.getXRot();
   }

   public void interpolateTo(Vec3 p_395342_, float p_391428_, float p_394793_) {
      if (this.interpolationSteps == 0) {
         this.entity.snapTo(p_395342_, p_391428_, p_394793_);
         this.cancel();
      } else if (!this.hasActiveInterpolation()
         || !Objects.equals(this.yRot(), p_391428_)
         || !Objects.equals(this.xRot(), p_394793_)
         || !Objects.equals(this.position(), p_395342_)) {
         this.interpolationData.steps = this.interpolationSteps;
         this.interpolationData.position = p_395342_;
         this.interpolationData.yRot = p_391428_;
         this.interpolationData.xRot = p_394793_;
         this.previousTickPosition = this.entity.position();
         this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
         if (this.onInterpolationStart != null) {
            this.onInterpolationStart.accept(this);
         }
      }
   }

   public boolean hasActiveInterpolation() {
      return this.interpolationData.steps > 0;
   }

   public void setInterpolationLength(int p_394306_) {
      this.interpolationSteps = p_394306_;
   }

   public void interpolate() {
      if (!this.hasActiveInterpolation()) {
         this.cancel();
      } else {
         double d0 = 1.0 / this.interpolationData.steps;
         if (this.previousTickPosition != null) {
            Vec3 vec3 = this.entity.position().subtract(this.previousTickPosition);
            if (this.entity.level().noCollision(this.entity, this.entity.makeBoundingBox(this.interpolationData.position.add(vec3)))) {
               this.interpolationData.addDelta(vec3);
            }
         }

         if (this.previousTickRot != null) {
            float f3 = this.entity.getYRot() - this.previousTickRot.y;
            float f = this.entity.getXRot() - this.previousTickRot.x;
            this.interpolationData.addRotation(f3, f);
         }

         double d3 = Mth.lerp(d0, this.entity.getX(), this.interpolationData.position.x);
         double d1 = Mth.lerp(d0, this.entity.getY(), this.interpolationData.position.y);
         double d2 = Mth.lerp(d0, this.entity.getZ(), this.interpolationData.position.z);
         Vec3 vec31 = new Vec3(d3, d1, d2);
         float f1 = (float)Mth.rotLerp(d0, this.entity.getYRot(), this.interpolationData.yRot);
         float f2 = (float)Mth.lerp(d0, this.entity.getXRot(), this.interpolationData.xRot);
         this.entity.setPos(vec31);
         this.entity.setRot(f1, f2);
         this.interpolationData.decrease();
         this.previousTickPosition = vec31;
         this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
      }
   }

   public void cancel() {
      this.interpolationData.steps = 0;
      this.previousTickPosition = null;
      this.previousTickRot = null;
   }

   static class InterpolationData {
      protected int steps;
      Vec3 position;
      float yRot;
      float xRot;

      InterpolationData(int p_392531_, Vec3 p_393512_, float p_392351_, float p_397412_) {
         this.steps = p_392531_;
         this.position = p_393512_;
         this.yRot = p_392351_;
         this.xRot = p_397412_;
      }

      public void decrease() {
         this.steps--;
      }

      public void addDelta(Vec3 p_395863_) {
         this.position = this.position.add(p_395863_);
      }

      public void addRotation(float p_394560_, float p_394672_) {
         this.yRot += p_394560_;
         this.xRot += p_394672_;
      }
   }
}
