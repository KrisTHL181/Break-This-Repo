package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record PointGizmo(Vec3 pos, int color, float size) implements Gizmo {
   @Override
   public void emit(GizmoPrimitives p_451965_, float p_455085_) {
      p_451965_.addPoint(this.pos, ARGB.multiplyAlpha(this.color, p_455085_), this.size);
   }
}
