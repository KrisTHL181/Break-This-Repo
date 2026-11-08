package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record LineGizmo(Vec3 start, Vec3 end, int color, float width) implements Gizmo {
   public static final float DEFAULT_WIDTH = 3.0F;

   @Override
   public void emit(final GizmoPrimitives primitives, final float alphaMultiplier) {
      primitives.addLine(this.start, this.end, ARGB.multiplyAlpha(this.color, alphaMultiplier), this.width);
   }
}
