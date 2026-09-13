package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public abstract class AbstractButton extends AbstractWidget.WithInactiveMessage {
   protected static final int TEXT_MARGIN = 2;
   private static final WidgetSprites SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/button"),
      Identifier.withDefaultNamespace("widget/button_disabled"),
      Identifier.withDefaultNamespace("widget/button_highlighted")
   );
   private @Nullable Supplier<Boolean> overrideRenderHighlightedSprite;

   public AbstractButton(final int x, final int y, final int width, final int height, final Component message) {
      super(x, y, width, height, message);
   }

   public abstract void onPress(InputWithModifiers input);

   @Override
   protected final void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      this.extractContents(graphics, mouseX, mouseY, a);
      this.handleCursor(graphics);
   }

   protected abstract void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);

   protected void extractDefaultLabel(final ActiveTextCollector output) {
      this.extractScrollingStringOverContents(output, this.getMessage(), 2);
   }

   protected final void extractDefaultSprite(final GuiGraphicsExtractor graphics) {
      graphics.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         SPRITES.get(this.active, this.overrideRenderHighlightedSprite != null ? this.overrideRenderHighlightedSprite.get() : this.isHoveredOrFocused()),
         this.getX(),
         this.getY(),
         this.getWidth(),
         this.getHeight(),
         ARGB.white(this.alpha)
      );
   }

   @Override
   public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
      this.onPress(event);
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (!this.isActive()) {
         return false;
      } else if (event.isSelection()) {
         this.playDownSound(Minecraft.getInstance().getSoundManager());
         this.onPress(event);
         return true;
      } else {
         return false;
      }
   }

   public void setOverrideRenderHighlightedSprite(final Supplier<Boolean> overrideRenderHighlightedSprite) {
      this.overrideRenderHighlightedSprite = overrideRenderHighlightedSprite;
   }
}
