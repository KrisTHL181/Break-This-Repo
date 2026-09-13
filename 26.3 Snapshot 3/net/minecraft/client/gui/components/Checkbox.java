package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class Checkbox extends AbstractButton {
   private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted");
   private static final Identifier CHECKBOX_SELECTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected");
   private static final Identifier CHECKBOX_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_highlighted");
   private static final Identifier CHECKBOX_SPRITE = Identifier.withDefaultNamespace("widget/checkbox");
   private static final int SPACING = 4;
   private static final int ROWS = 2;
   private static final int BOX_PADDING = 8;
   private boolean selected;
   private final Checkbox.OnValueChange onValueChange;
   private final MultiLineTextWidget textWidget;

   private Checkbox(
      final int x,
      final int y,
      final int maxWidth,
      final Component message,
      final Font font,
      final boolean selected,
      final Checkbox.OnValueChange onValueChange
   ) {
      super(x, y, 0, 0, message);
      this.textWidget = new MultiLineTextWidget(message, font);
      this.textWidget.setMaxRows(2);
      this.width = this.adjustWidth(maxWidth, font);
      this.height = this.getAdjustedHeight(font);
      this.selected = selected;
      this.onValueChange = onValueChange;
   }

   public int adjustWidth(final int maxWidth, final Font font) {
      this.width = this.getAdjustedWidth(maxWidth, this.getMessage(), font);
      this.textWidget.setMaxWidth(this.width);
      return this.width;
   }

   private int getAdjustedWidth(final int maxWidth, final Component message, final Font font) {
      return Math.min(getDefaultWidth(message, font), maxWidth);
   }

   private int getAdjustedHeight(final Font font) {
      return Math.max(getBoxSize(font), this.textWidget.getHeight());
   }

   private static int getDefaultWidth(final Component message, final Font font) {
      return getBoxSize(font) + 4 + font.width(message);
   }

   private boolean overflowsRowLimit(final Font font) {
      return font.getSplitter().splitLines(this.textWidget.getMessage(), this.width, Style.EMPTY).size() > 2;
   }

   public static Checkbox.Builder builder(final Component message, final Font font) {
      return new Checkbox.Builder(message, font);
   }

   public static int getBoxSize(final Font font) {
      return 9 + 8;
   }

   @Override
   public void onPress(final InputWithModifiers input) {
      this.selected = !this.selected;
      this.onValueChange.onValueChange(this, this.selected);
   }

   public boolean selected() {
      return this.selected;
   }

   @Override
   public void updateWidgetNarration(final NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            output.add(
               NarratedElementType.USAGE,
               Component.translatable(this.selected ? "narration.checkbox.usage.focused.uncheck" : "narration.checkbox.usage.focused.check")
            );
         } else {
            output.add(
               NarratedElementType.USAGE,
               Component.translatable(this.selected ? "narration.checkbox.usage.hovered.uncheck" : "narration.checkbox.usage.hovered.check")
            );
         }
      }
   }

   @Override
   public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;
      Identifier sprite;
      if (this.selected) {
         sprite = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
      } else {
         sprite = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
      }

      int boxSize = getBoxSize(font);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), boxSize, boxSize, ARGB.white(this.alpha));
      int textX = this.getX() + boxSize + 4;
      int textY = this.getY() + boxSize / 2 - this.textWidget.getHeight() / 2;
      this.textWidget.setPosition(textX, textY);
      this.textWidget.visitLines(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.notClickable(this.isHovered())));
   }

   public static class Builder {
      private final Component message;
      private final Font font;
      private int maxWidth;
      private int x = 0;
      private int y = 0;
      private Checkbox.OnValueChange onValueChange = Checkbox.OnValueChange.NOP;
      private boolean selected = false;
      private @Nullable OptionInstance<Boolean> option = null;
      private @Nullable Tooltip tooltip = null;

      private Builder(final Component message, final Font font) {
         this.message = message;
         this.font = font;
         this.maxWidth = Checkbox.getDefaultWidth(message, font);
      }

      public Checkbox.Builder pos(final int x, final int y) {
         this.x = x;
         this.y = y;
         return this;
      }

      public Checkbox.Builder onValueChange(final Checkbox.OnValueChange onValueChange) {
         this.onValueChange = onValueChange;
         return this;
      }

      public Checkbox.Builder selected(final boolean selected) {
         this.selected = selected;
         this.option = null;
         return this;
      }

      public Checkbox.Builder selected(final OptionInstance<Boolean> option) {
         this.option = option;
         this.selected = option.get();
         return this;
      }

      public Checkbox.Builder tooltip(final Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Checkbox.Builder maxWidth(final int maxWidth) {
         this.maxWidth = maxWidth;
         return this;
      }

      public Checkbox build() {
         Checkbox.OnValueChange onChange = this.option == null ? this.onValueChange : (checkbox, value) -> {
            this.option.set(value);
            this.onValueChange.onValueChange(checkbox, value);
         };
         Checkbox box = new Checkbox(this.x, this.y, this.maxWidth, this.message, this.font, this.selected, onChange);
         if (box.overflowsRowLimit(this.font)) {
            box.setTooltip(this.tooltip);
         }

         return box;
      }
   }

   public interface OnValueChange {
      Checkbox.OnValueChange NOP = (checkbox, value) -> {};

      void onValueChange(Checkbox checkbox, boolean value);
   }
}
