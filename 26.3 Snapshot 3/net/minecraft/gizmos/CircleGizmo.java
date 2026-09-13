package net.minecraft.gizmos;

import net.minecraft.world.phys.Vec3;

public record CircleGizmo(Vec3 pos, float radius, GizmoStyle style) implements Gizmo {
   private static final int CIRCLE_VERTICES = 20;
   private static final float SEGMENT_SIZE_RADIANS = (float) (Math.PI / 10);

   @Override
   public void emit(final GizmoPrimitives primitives, final float alphaMultiplier) {
      if (this.style.hasStroke() || this.style.hasFill()) {
         Vec3[] points = new Vec3[21];

         for (int i = 0; i < 20; i++) {
            float theta = i * (float) (Math.PI / 10);
            Vec3 point = this.pos.add((float)(this.radius * Math.cos(theta)), 0.0, (float)(this.radius * Math.sin(theta)));
            points[i] = point;
         }

         points[20] = points[0];
         if (this.style.hasFill()) {
            int color = this.style.multipliedFill(alphaMultiplier);
            primitives.addTriangleFan(points, color);
         }

         if (this.style.hasStroke()) {
            int color = this.style.multipliedStroke(alphaMultiplier);

            for (int i = 0; i < 20; i++) {
               primitives.addLine(points[i], points[i + 1], color, this.style.strokeWidth());
            }
         }
      }
   }
}
