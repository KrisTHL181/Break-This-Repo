package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CuboidGizmo(AABB aabb, GizmoStyle style, boolean coloredCornerStroke) implements Gizmo {
   @Override
   public void emit(GizmoPrimitives p_454047_, float p_459103_) {
      double d0 = this.aabb.minX;
      double d1 = this.aabb.minY;
      double d2 = this.aabb.minZ;
      double d3 = this.aabb.maxX;
      double d4 = this.aabb.maxY;
      double d5 = this.aabb.maxZ;
      if (this.style.hasFill()) {
         int i = this.style.multipliedFill(p_459103_);
         p_454047_.addQuad(new Vec3(d3, d1, d2), new Vec3(d3, d4, d2), new Vec3(d3, d4, d5), new Vec3(d3, d1, d5), i);
         p_454047_.addQuad(new Vec3(d0, d1, d2), new Vec3(d0, d1, d5), new Vec3(d0, d4, d5), new Vec3(d0, d4, d2), i);
         p_454047_.addQuad(new Vec3(d0, d1, d2), new Vec3(d0, d4, d2), new Vec3(d3, d4, d2), new Vec3(d3, d1, d2), i);
         p_454047_.addQuad(new Vec3(d0, d1, d5), new Vec3(d3, d1, d5), new Vec3(d3, d4, d5), new Vec3(d0, d4, d5), i);
         p_454047_.addQuad(new Vec3(d0, d4, d2), new Vec3(d0, d4, d5), new Vec3(d3, d4, d5), new Vec3(d3, d4, d2), i);
         p_454047_.addQuad(new Vec3(d0, d1, d2), new Vec3(d3, d1, d2), new Vec3(d3, d1, d5), new Vec3(d0, d1, d5), i);
      }

      if (this.style.hasStroke()) {
         int j = this.style.multipliedStroke(p_459103_);
         p_454047_.addLine(new Vec3(d0, d1, d2), new Vec3(d3, d1, d2), this.coloredCornerStroke ? ARGB.multiply(j, -34953) : j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d1, d2), new Vec3(d0, d4, d2), this.coloredCornerStroke ? ARGB.multiply(j, -8913033) : j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d1, d2), new Vec3(d0, d1, d5), this.coloredCornerStroke ? ARGB.multiply(j, -8947713) : j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d3, d1, d2), new Vec3(d3, d4, d2), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d3, d4, d2), new Vec3(d0, d4, d2), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d4, d2), new Vec3(d0, d4, d5), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d4, d5), new Vec3(d0, d1, d5), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d1, d5), new Vec3(d3, d1, d5), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d3, d1, d5), new Vec3(d3, d1, d2), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d0, d4, d5), new Vec3(d3, d4, d5), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d3, d1, d5), new Vec3(d3, d4, d5), j, this.style.strokeWidth());
         p_454047_.addLine(new Vec3(d3, d4, d2), new Vec3(d3, d4, d5), j, this.style.strokeWidth());
      }
   }
}
