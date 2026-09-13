package net.minecraft.gizmos;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record RectGizmo(Vec3 a, Vec3 b, Vec3 c, Vec3 d, GizmoStyle style) implements Gizmo {
   public static RectGizmo fromCuboidFace(final Vec3 cuboidCornerA, final Vec3 cuboidCornerB, final Direction face, final GizmoStyle style) {
      return switch (face) {
         case DOWN -> new RectGizmo(
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z),
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z),
            style
         );
         case UP -> new RectGizmo(
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z),
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z),
            style
         );
         case NORTH -> new RectGizmo(
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z),
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z),
            style
         );
         case SOUTH -> new RectGizmo(
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z),
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z),
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z),
            style
         );
         case WEST -> new RectGizmo(
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z),
            new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z),
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z),
            new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z),
            style
         );
         case EAST -> new RectGizmo(
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z),
            new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z),
            new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z),
            style
         );
      };
   }

   @Override
   public void emit(final GizmoPrimitives primitives, final float alphaMultiplier) {
      if (this.style.hasFill()) {
         int color = this.style.multipliedFill(alphaMultiplier);
         primitives.addQuad(this.a, this.b, this.c, this.d, color);
      }

      if (this.style.hasStroke()) {
         int color = this.style.multipliedStroke(alphaMultiplier);
         primitives.addLine(this.a, this.b, color, this.style.strokeWidth());
         primitives.addLine(this.b, this.c, color, this.style.strokeWidth());
         primitives.addLine(this.c, this.d, color, this.style.strokeWidth());
         primitives.addLine(this.d, this.a, color, this.style.strokeWidth());
      }
   }
}
