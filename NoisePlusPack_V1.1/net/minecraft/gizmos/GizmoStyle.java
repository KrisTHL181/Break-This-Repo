package net.minecraft.gizmos;

import net.minecraft.util.ARGB;

public record GizmoStyle(int stroke, float strokeWidth, int fill) {
   private static final float DEFAULT_WIDTH = 2.5F;

   public static GizmoStyle stroke(int p_451660_) {
      return new GizmoStyle(p_451660_, 2.5F, 0);
   }

   public static GizmoStyle stroke(int p_454989_, float p_454537_) {
      return new GizmoStyle(p_454989_, p_454537_, 0);
   }

   public static GizmoStyle fill(int p_450177_) {
      return new GizmoStyle(0, 0.0F, p_450177_);
   }

   public static GizmoStyle strokeAndFill(int p_452603_, float p_456076_, int p_452472_) {
      return new GizmoStyle(p_452603_, p_456076_, p_452472_);
   }

   public boolean hasFill() {
      return this.fill != 0;
   }

   public boolean hasStroke() {
      return this.stroke != 0 && this.strokeWidth > 0.0F;
   }

   public int multipliedStroke(float p_455003_) {
      return ARGB.multiplyAlpha(this.stroke, p_455003_);
   }

   public int multipliedFill(float p_459483_) {
      return ARGB.multiplyAlpha(this.fill, p_459483_);
   }
}
