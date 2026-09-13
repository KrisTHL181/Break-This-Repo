package net.minecraft.gizmos;

import java.util.OptionalDouble;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record TextGizmo(Vec3 pos, String text, TextGizmo.Style style) implements Gizmo {
   @Override
   public void emit(GizmoPrimitives p_451928_, float p_453723_) {
      TextGizmo.Style textgizmo$style;
      if (p_453723_ < 1.0F) {
         textgizmo$style = new TextGizmo.Style(ARGB.multiplyAlpha(this.style.color, p_453723_), this.style.scale, this.style.adjustLeft);
      } else {
         textgizmo$style = this.style;
      }

      p_451928_.addText(this.pos, this.text, textgizmo$style);
   }

   public record Style(int color, float scale, OptionalDouble adjustLeft) {
      public static final float DEFAULT_SCALE = 0.32F;

      public static TextGizmo.Style whiteAndCentered() {
         return new TextGizmo.Style(-1, 0.32F, OptionalDouble.empty());
      }

      public static TextGizmo.Style forColorAndCentered(int p_457273_) {
         return new TextGizmo.Style(p_457273_, 0.32F, OptionalDouble.empty());
      }

      public static TextGizmo.Style forColor(int p_451262_) {
         return new TextGizmo.Style(p_451262_, 0.32F, OptionalDouble.of(0.0));
      }

      public TextGizmo.Style withScale(float p_459020_) {
         return new TextGizmo.Style(this.color, p_459020_, this.adjustLeft);
      }

      public TextGizmo.Style withLeftAlignment(float p_460169_) {
         return new TextGizmo.Style(this.color, this.scale, OptionalDouble.of(p_460169_));
      }
   }
}
