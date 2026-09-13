package net.minecraft.gizmos;

import net.minecraft.world.phys.Vec3;

public record CircleGizmo(Vec3 pos, float radius, GizmoStyle style) implements Gizmo {
   private static final int CIRCLE_VERTICES = 20;
   private static final float SEGMENT_SIZE_RADIANS = (float) (Math.PI / 10);

   @Override
   public void emit(GizmoPrimitives p_459319_, float p_459509_) {
      if (this.style.hasStroke() || this.style.hasFill()) {
         Vec3[] avec3 = new Vec3[21];

         for (int i = 0; i < 20; i++) {
            float f = i * (float) (Math.PI / 10);
            Vec3 vec3 = this.pos.add((float)(this.radius * Math.cos(f)), 0.0, (float)(this.radius * Math.sin(f)));
            avec3[i] = vec3;
         }

         avec3[20] = avec3[0];
         if (this.style.hasFill()) {
            int j = this.style.multipliedFill(p_459509_);
            p_459319_.addTriangleFan(avec3, j);
         }

         if (this.style.hasStroke()) {
            int k = this.style.multipliedStroke(p_459509_);

            for (int l = 0; l < 20; l++) {
               p_459319_.addLine(avec3[l], avec3[l + 1], k, this.style.strokeWidth());
            }
         }
      }
   }
}
