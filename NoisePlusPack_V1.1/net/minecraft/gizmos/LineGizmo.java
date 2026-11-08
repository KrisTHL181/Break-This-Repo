package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record LineGizmo(Vec3 start, Vec3 end, int color, float width) implements Gizmo {
   public static final float DEFAULT_WIDTH = 3.0F;

   @Override
   public void emit(GizmoPrimitives p_455924_, float p_452251_) {
      p_455924_.addLine(this.start, this.end, ARGB.multiplyAlpha(this.color, p_452251_), this.width);
   }
}
