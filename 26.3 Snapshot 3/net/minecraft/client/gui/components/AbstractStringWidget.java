package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

public abstract class AbstractStringWidget extends AbstractWidget {
   private @Nullable Consumer<Style> componentClickHandler = null;
   private final Font font;

   public AbstractStringWidget(final int x, final int y, final int width, final int height, final Component message, final Font font) {
      super(x, y, width, height, message);
      this.font = font;
   }

   public abstract void visitLines(ActiveTextCollector output);

   @Override
   public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      GuiGraphicsExtractor.HoveredTextEffects effects;
      if (this.isHovered()) {
         if (this.componentClickHandler != null) {
            effects = GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR;
         } else {
            effects = GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY;
         }
      } else {
         effects = GuiGraphicsExtractor.HoveredTextEffects.NONE;
      }

      this.visitLines(graphics.textRendererForWidget(this, effects));
   }

   @Override
   public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
      if (this.componentClickHandler != null) {
         ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.getFont(), (int)event.x(), (int)event.y());
         this.visitLines(finder);
         Style clickedStyle = finder.result();
         if (clickedStyle != null) {
            this.componentClickHandler.accept(clickedStyle);
            return;
         }
      }

      super.onClick(event, doubleClick);
   }

   @Override
   protected void updateWidgetNarration(final NarrationElementOutput output) {
   }

   protected final Font getFont() {
      return this.font;
   }

   @Override
   public void setMessage(final Component message) {
      super.setMessage(message);
      this.setWidth(this.getFont().width(message.getVisualOrderText()));
   }

   public AbstractStringWidget setComponentClickHandler(final @Nullable Consumer<Style> clickEventConsumer) {
      this.componentClickHandler = clickEventConsumer;
      return this;
   }

   protected boolean handleStyleClick(final Style style) {
      if (this.componentClickHandler != null && style.getClickEvent() != null) {
         this.componentClickHandler.accept(style);
         return true;
      } else {
         return false;
      }
   }
}
