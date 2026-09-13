package net.minecraft.client.gui.components;

import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class FocusableTextWidget extends MultiLineTextWidget {
   public static final int DEFAULT_PADDING = 4;
   private final int padding;
   private final int maxWidth;
   private final boolean alwaysShowBorder;
   private final FocusableTextWidget.BackgroundFill backgroundFill;
   private boolean narrateMessage = true;
   private @Nullable Component focusedUsageNarration;
   private @Nullable Component hoveredUsageNarration;

   private FocusableTextWidget(
      final Component message,
      final Font font,
      final int padding,
      final int maxWidth,
      final FocusableTextWidget.BackgroundFill backgroundFill,
      final boolean alwaysShowBorder
   ) {
      super(message, font);
      this.active = true;
      this.padding = padding;
      this.maxWidth = maxWidth;
      this.alwaysShowBorder = alwaysShowBorder;
      this.backgroundFill = backgroundFill;
      this.updateWidth();
      this.updateHeight();
      this.setCentered(true);
   }

   @Override
   protected void updateWidgetNarration(final NarrationElementOutput output) {
      if (this.narrateMessage) {
         output.add(NarratedElementType.TITLE, this.getMessage());
      }

      if (this.active) {
         Component usage = this.isFocused() ? this.focusedUsageNarration : this.hoveredUsageNarration;
         if (usage != null) {
            output.add(NarratedElementType.USAGE, usage);
         }
      }
   }

   public void setNarrateMessage(final boolean narrateMessage) {
      this.narrateMessage = narrateMessage;
   }

   public void setUsageNarration(final @Nullable Component focused, final @Nullable Component hovered) {
      this.focusedUsageNarration = focused;
      this.hoveredUsageNarration = hovered;
   }

   @Override
   public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      int borderColor = this.alwaysShowBorder && !this.isFocused() ? ARGB.color(this.alpha, -6250336) : ARGB.white(this.alpha);
      switch (this.backgroundFill) {
         case ALWAYS:
            graphics.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            break;
         case ON_FOCUS:
            if (this.isFocused()) {
               graphics.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            }
         case NEVER:
      }

      if (this.isFocused() || this.alwaysShowBorder) {
         graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), borderColor);
      }

      super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
   }

   @Override
   protected int getTextX() {
      return this.getX() + this.padding;
   }

   @Override
   protected int getTextY() {
      return super.getTextY() + this.padding;
   }

   @Override
   public MultiLineTextWidget setMaxWidth(final int maxWidth) {
      return super.setMaxWidth(maxWidth - this.padding * 2);
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   public int getPadding() {
      return this.padding;
   }

   public void updateWidth() {
      if (this.maxWidth != -1) {
         this.setWidth(this.maxWidth);
         this.setMaxWidth(this.maxWidth);
      } else {
         this.setWidth(this.getFont().width(this.getMessage()) + this.padding * 2);
      }
   }

   public void updateHeight() {
      int textHeight = 9 * this.getFont().split(this.getMessage(), super.getWidth()).size();
      this.setHeight(textHeight + this.padding * 2);
   }

   @Override
   public void setMessage(final Component message) {
      this.message = message;
      int width;
      if (this.maxWidth != -1) {
         width = this.maxWidth;
      } else {
         width = this.getFont().width(message) + this.padding * 2;
      }

      this.setWidth(width);
      this.updateHeight();
   }

   @Override
   public void playDownSound(final SoundManager soundManager) {
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (this.isActive() && event.isSelection()) {
         Optional<Style> clickableStyle = this.getMessage()
            .visit((style, text) -> style.getClickEvent() != null ? Optional.of(style) : Optional.empty(), Style.EMPTY);
         if (clickableStyle.isPresent() && this.handleStyleClick(clickableStyle.get())) {
            return true;
         }
      }

      return super.keyPressed(event);
   }

   public static FocusableTextWidget.Builder builder(final Component message, final Font font) {
      return new FocusableTextWidget.Builder(message, font);
   }

   public static FocusableTextWidget.Builder builder(final Component message, final Font font, final int padding) {
      return new FocusableTextWidget.Builder(message, font, padding);
   }

   public enum BackgroundFill {
      ALWAYS,
      ON_FOCUS,
      NEVER;
   }

   public static class Builder {
      private final Component message;
      private final Font font;
      private final int padding;
      private int maxWidth = -1;
      private boolean alwaysShowBorder = true;
      private FocusableTextWidget.BackgroundFill backgroundFill = FocusableTextWidget.BackgroundFill.ALWAYS;

      private Builder(final Component message, final Font font) {
         this(message, font, 4);
      }

      private Builder(final Component message, final Font font, final int padding) {
         this.message = message;
         this.font = font;
         this.padding = padding;
      }

      public FocusableTextWidget.Builder maxWidth(final int maxWidth) {
         this.maxWidth = maxWidth;
         return this;
      }

      public FocusableTextWidget.Builder textWidth(final int textWidth) {
         this.maxWidth = textWidth + this.padding * 2;
         return this;
      }

      public FocusableTextWidget.Builder alwaysShowBorder(final boolean alwaysShowBorder) {
         this.alwaysShowBorder = alwaysShowBorder;
         return this;
      }

      public FocusableTextWidget.Builder backgroundFill(final FocusableTextWidget.BackgroundFill backgroundFill) {
         this.backgroundFill = backgroundFill;
         return this;
      }

      public FocusableTextWidget build() {
         return new FocusableTextWidget(this.message, this.font, this.padding, this.maxWidth, this.backgroundFill, this.alwaysShowBorder);
      }
   }
}
