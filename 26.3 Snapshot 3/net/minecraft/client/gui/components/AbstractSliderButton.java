package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public abstract class AbstractSliderButton extends AbstractWidget.WithInactiveMessage {
   private static final Identifier SLIDER_SPRITE = Identifier.withDefaultNamespace("widget/slider");
   private static final Identifier HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_highlighted");
   private static final Identifier SLIDER_HANDLE_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle");
   private static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
   protected static final int TEXT_MARGIN = 2;
   public static final int DEFAULT_HEIGHT = 20;
   protected static final int HANDLE_WIDTH = 8;
   private static final int HANDLE_HALF_WIDTH = 4;
   protected double value;
   protected boolean canChangeValue;
   private boolean dragging;

   public AbstractSliderButton(final int x, final int y, final int width, final int height, final Component message, final double initialValue) {
      super(x, y, width, height, message);
      this.value = initialValue;
   }

   private Identifier getSprite() {
      return this.isActive() && this.isFocused() && !this.canChangeValue ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE;
   }

   private Identifier getHandleSprite() {
      return !this.isActive() || !this.isHovered && !this.canChangeValue ? SLIDER_HANDLE_SPRITE : SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return Component.translatable("gui.narrate.slider", this.getMessage());
   }

   @Override
   public void updateWidgetNarration(final NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            if (this.canChangeValue) {
               output.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused"));
            } else {
               output.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused.keyboard_cannot_change_value"));
            }
         } else {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
         }
      }
   }

   @Override
   public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
      graphics.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         this.getHandleSprite(),
         this.getX() + (int)(this.value * (this.width - 8)),
         this.getY(),
         8,
         this.getHeight(),
         ARGB.white(this.alpha)
      );
      this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), this.getMessage(), 2);
      this.handleCursor(graphics);
   }

   @Override
   protected void handleCursor(final GuiGraphicsExtractor graphics) {
      if (this.isHovered()) {
         graphics.requestCursor(this.isActive() ? (this.dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND) : CursorTypes.NOT_ALLOWED);
      }
   }

   @Override
   public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
      this.dragging = this.active;
      this.setValueFromMouse(event);
   }

   @Override
   public void setFocused(final boolean focused) {
      super.setFocused(focused);
      if (!focused) {
         this.canChangeValue = false;
      } else {
         InputType lastInputType = Minecraft.getInstance().getLastInputType();
         if (lastInputType == InputType.MOUSE || lastInputType == InputType.KEYBOARD_TAB) {
            this.canChangeValue = true;
         }
      }
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.isSelection()) {
         this.canChangeValue = !this.canChangeValue;
         return true;
      }

      if (this.canChangeValue) {
         boolean left = event.isLeft();
         boolean right = event.isRight();
         if (left || right) {
            float direction = left ? -1.0F : 1.0F;
            this.setValue(this.value + direction / (this.width - 8));
            return true;
         }
      }

      return false;
   }

   private void setValueFromMouse(final MouseButtonEvent event) {
      this.setValue((event.x() - (this.getX() + 4)) / (this.width - 8));
   }

   protected void setValue(final double newValue) {
      double oldValue = this.value;
      this.value = Mth.clamp(newValue, 0.0, 1.0);
      if (oldValue != this.value) {
         this.applyValue();
      }

      this.updateMessage();
   }

   @Override
   protected void onDrag(final MouseButtonEvent event, final double dx, final double dy) {
      this.setValueFromMouse(event);
      super.onDrag(event, dx, dy);
   }

   @Override
   public void playDownSound(final SoundManager soundManager) {
   }

   @Override
   public void onRelease(final MouseButtonEvent event) {
      this.dragging = false;
      super.playDownSound(Minecraft.getInstance().getSoundManager());
   }

   protected abstract void updateMessage();

   protected abstract void applyValue();
}
