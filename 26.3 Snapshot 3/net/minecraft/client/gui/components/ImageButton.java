package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButton extends Button {
   protected final WidgetSprites sprites;

   public ImageButton(final int x, final int y, final int width, final int height, final WidgetSprites sprites, final Button.OnPress onPress) {
      this(x, y, width, height, sprites, onPress, CommonComponents.EMPTY);
   }

   public ImageButton(final int width, final int height, final WidgetSprites sprites, final Button.OnPress onPress, final Component message) {
      this(0, 0, width, height, sprites, onPress, message);
   }

   public ImageButton(
      final int x, final int y, final int width, final int height, final WidgetSprites sprites, final Button.OnPress onPress, final Component message
   ) {
      super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      this.sprites = sprites;
   }

   @Override
   public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      Identifier sprite = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
   }
}
