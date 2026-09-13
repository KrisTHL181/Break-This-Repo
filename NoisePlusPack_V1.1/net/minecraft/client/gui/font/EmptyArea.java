package net.minecraft.client.gui.font;

import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record EmptyArea(float x, float y, float advance, float ascent, float height, Style style) implements ActiveArea {
   public static final float DEFAULT_HEIGHT = 9.0F;
   public static final float DEFAULT_ASCENT = 7.0F;

   @Override
   public float activeLeft() {
      return this.x;
   }

   @Override
   public float activeTop() {
      return this.y + 7.0F - this.ascent;
   }

   @Override
   public float activeRight() {
      return this.x + this.advance;
   }

   @Override
   public float activeBottom() {
      return this.activeTop() + this.height;
   }
}
