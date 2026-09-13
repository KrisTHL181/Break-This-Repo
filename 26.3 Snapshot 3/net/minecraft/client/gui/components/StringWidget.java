package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class StringWidget extends AbstractStringWidget {
   private static final int TEXT_MARGIN = 2;
   private int maxWidth = 0;
   private int cachedWidth = 0;
   private boolean cachedWidthDirty = true;
   private StringWidget.TextOverflow textOverflow = StringWidget.TextOverflow.CLAMPED;

   public StringWidget(final Component message, final Font font) {
      this(0, 0, font.width(message.getVisualOrderText()), 9, message, font);
   }

   public StringWidget(final int width, final int height, final Component message, final Font font) {
      this(0, 0, width, height, message, font);
   }

   public StringWidget(final int x, final int y, final int width, final int height, final Component message, final Font font) {
      super(x, y, width, height, message, font);
      this.active = false;
   }

   @Override
   public void setMessage(final Component message) {
      super.setMessage(message);
      this.cachedWidthDirty = true;
   }

   public StringWidget setMaxWidth(final int maxWidth) {
      return this.setMaxWidth(maxWidth, StringWidget.TextOverflow.CLAMPED);
   }

   public StringWidget setMaxWidth(final int maxWidth, final StringWidget.TextOverflow textOverflow) {
      this.maxWidth = maxWidth;
      this.textOverflow = textOverflow;
      return this;
   }

   @Override
   public int getWidth() {
      if (this.maxWidth > 0) {
         if (this.cachedWidthDirty) {
            this.cachedWidth = Math.min(this.maxWidth, this.getFont().width(this.getMessage().getVisualOrderText()));
            this.cachedWidthDirty = false;
         }

         return this.cachedWidth;
      } else {
         return super.getWidth();
      }
   }

   @Override
   public void visitLines(final ActiveTextCollector output) {
      Component message = this.getMessage();
      Font font = this.getFont();
      int maxWidth = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
      int textWidth = font.width(message);
      int x = this.getX();
      int y = this.getY() + (this.getHeight() - 9) / 2;
      boolean textOverflow = textWidth > maxWidth;
      if (textOverflow) {
         switch (this.textOverflow) {
            case CLAMPED:
               output.accept(x, y, ComponentRenderUtils.clipText(message, font, maxWidth));
               break;
            case SCROLLING:
               this.extractScrollingStringOverContents(output, message, 2);
         }
      } else {
         output.accept(x, y, message.getVisualOrderText());
      }
   }

   public enum TextOverflow {
      CLAMPED,
      SCROLLING;
   }
}
