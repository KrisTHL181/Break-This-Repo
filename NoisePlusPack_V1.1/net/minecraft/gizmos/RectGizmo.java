package net.minecraft.gizmos;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record RectGizmo(Vec3 a, Vec3 b, Vec3 c, Vec3 d, GizmoStyle style) implements Gizmo {
   public static RectGizmo fromCuboidFace(Vec3 p_455553_, Vec3 p_456721_, Direction p_453022_, GizmoStyle p_458352_) {
      return switch (p_453022_) {
         case DOWN -> new RectGizmo(
            new Vec3(p_455553_.x, p_455553_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_455553_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_455553_.y, p_456721_.z),
            new Vec3(p_455553_.x, p_455553_.y, p_456721_.z),
            p_458352_
         );
         case UP -> new RectGizmo(
            new Vec3(p_455553_.x, p_456721_.y, p_455553_.z),
            new Vec3(p_455553_.x, p_456721_.y, p_456721_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_456721_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_455553_.z),
            p_458352_
         );
         case NORTH -> new RectGizmo(
            new Vec3(p_455553_.x, p_455553_.y, p_455553_.z),
            new Vec3(p_455553_.x, p_456721_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_455553_.y, p_455553_.z),
            p_458352_
         );
         case SOUTH -> new RectGizmo(
            new Vec3(p_455553_.x, p_455553_.y, p_456721_.z),
            new Vec3(p_456721_.x, p_455553_.y, p_456721_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_456721_.z),
            new Vec3(p_455553_.x, p_456721_.y, p_456721_.z),
            p_458352_
         );
         case WEST -> new RectGizmo(
            new Vec3(p_455553_.x, p_455553_.y, p_455553_.z),
            new Vec3(p_455553_.x, p_455553_.y, p_456721_.z),
            new Vec3(p_455553_.x, p_456721_.y, p_456721_.z),
            new Vec3(p_455553_.x, p_456721_.y, p_455553_.z),
            p_458352_
         );
         case EAST -> new RectGizmo(
            new Vec3(p_456721_.x, p_455553_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_455553_.z),
            new Vec3(p_456721_.x, p_456721_.y, p_456721_.z),
            new Vec3(p_456721_.x, p_455553_.y, p_456721_.z),
            p_458352_
         );
      };
   }

   @Override
   public void emit(GizmoPrimitives p_459538_, float p_452044_) {
      if (this.style.hasFill()) {
         int i = this.style.multipliedFill(p_452044_);
         p_459538_.addQuad(this.a, this.b, this.c, this.d, i);
      }

      if (this.style.hasStroke()) {
         int j = this.style.multipliedStroke(p_452044_);
         p_459538_.addLine(this.a, this.b, j, this.style.strokeWidth());
         p_459538_.addLine(this.b, this.c, j, this.style.strokeWidth());
         p_459538_.addLine(this.c, this.d, j, this.style.strokeWidth());
         p_459538_.addLine(this.d, this.a, j, this.style.strokeWidth());
      }
   }
}
