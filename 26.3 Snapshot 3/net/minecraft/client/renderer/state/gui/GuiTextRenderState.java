package net.minecraft.client.renderer.state.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public final class GuiTextRenderState implements ScreenArea {
   private final Font font;
   private final FormattedCharSequence text;
   public final Matrix3x2fc pose;
   private final int x;
   private final int y;
   private final int color;
   private final int backgroundColor;
   private final boolean dropShadow;
   private final boolean includeEmpty;
   public final @Nullable ScreenRectangle scissor;
   private Font.@Nullable PreparedText preparedText;
   private @Nullable ScreenRectangle bounds;

   public GuiTextRenderState(
      final Font font,
      final FormattedCharSequence text,
      final Matrix3x2fc pose,
      final int x,
      final int y,
      final int color,
      final int backgroundColor,
      final boolean dropShadow,
      final boolean includeEmpty,
      final @Nullable ScreenRectangle scissor
   ) {
      this.font = font;
      this.text = text;
      this.pose = pose;
      this.x = x;
      this.y = y;
      this.color = color;
      this.backgroundColor = backgroundColor;
      this.dropShadow = dropShadow;
      this.includeEmpty = includeEmpty;
      this.scissor = scissor;
   }

   public Font.PreparedText ensurePrepared() {
      if (this.preparedText == null) {
         this.preparedText = this.font.prepareText(this.text, this.x, this.y, this.color, this.dropShadow, this.includeEmpty, this.backgroundColor);
         ScreenRectangle bounds = this.preparedText.bounds();
         if (bounds != null) {
            bounds = bounds.transformMaxBounds(this.pose);
            this.bounds = this.scissor != null ? this.scissor.intersection(bounds) : bounds;
         }
      }

      return this.preparedText;
   }

   @Override
   public @Nullable ScreenRectangle bounds() {
      this.ensurePrepared();
      return this.bounds;
   }
}
