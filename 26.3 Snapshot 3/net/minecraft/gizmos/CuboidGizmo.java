package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CuboidGizmo(AABB aabb, GizmoStyle style, boolean coloredCornerStroke) implements Gizmo {
   @Override
   public void emit(final GizmoPrimitives primitives, final float alphaMultiplier) {
      double x0 = this.aabb.minX;
      double y0 = this.aabb.minY;
      double z0 = this.aabb.minZ;
      double x1 = this.aabb.maxX;
      double y1 = this.aabb.maxY;
      double z1 = this.aabb.maxZ;
      if (this.style.hasFill()) {
         int color = this.style.multipliedFill(alphaMultiplier);
         primitives.addQuad(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), color);
         primitives.addQuad(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), color);
         primitives.addQuad(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y0, z0), color);
         primitives.addQuad(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), color);
         primitives.addQuad(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), color);
         primitives.addQuad(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1), color);
      }

      if (this.style.hasStroke()) {
         int color = this.style.multipliedStroke(alphaMultiplier);
         primitives.addLine(
            new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), this.coloredCornerStroke ? ARGB.multiply(color, -34953) : color, this.style.strokeWidth()
         );
         primitives.addLine(
            new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), this.coloredCornerStroke ? ARGB.multiply(color, -8913033) : color, this.style.strokeWidth()
         );
         primitives.addLine(
            new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), this.coloredCornerStroke ? ARGB.multiply(color, -8947713) : color, this.style.strokeWidth()
         );
         primitives.addLine(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x1, y1, z0), new Vec3(x0, y1, z0), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x0, y1, z1), new Vec3(x0, y0, z1), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), color, this.style.strokeWidth());
         primitives.addLine(new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), color, this.style.strokeWidth());
      }
   }
}
