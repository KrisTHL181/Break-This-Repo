package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record PointGizmo(Vec3 pos, int color, float size) implements Gizmo {
   @Override
   public void emit(final GizmoPrimitives primitives, final float alphaMultiplier) {
      primitives.addPoint(this.pos, ARGB.multiplyAlpha(this.color, alphaMultiplier), this.size);
   }
}
