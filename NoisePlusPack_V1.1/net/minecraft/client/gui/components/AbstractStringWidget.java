package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractStringWidget extends AbstractWidget {
   private @Nullable Consumer<Style> componentClickHandler = null;
   private final Font font;

   public AbstractStringWidget(int p_270910_, int p_270297_, int p_270088_, int p_270842_, Component p_270063_, Font p_270327_) {
      super(p_270910_, p_270297_, p_270088_, p_270842_, p_270063_);
      this.font = p_270327_;
   }

   public abstract void visitLines(ActiveTextCollector var1);

   @Override
   public void renderWidget(GuiGraphics p_453638_, int p_458651_, int p_453805_, float p_455335_) {
      GuiGraphics.HoveredTextEffects guigraphics$hoveredtexteffects;
      if (this.isHovered()) {
         if (this.componentClickHandler != null) {
            guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR;
         } else {
            guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.TOOLTIP_ONLY;
         }
      } else {
         guigraphics$hoveredtexteffects = GuiGraphics.HoveredTextEffects.NONE;
      }

      this.visitLines(p_453638_.textRendererForWidget(this, guigraphics$hoveredtexteffects));
   }

   @Override
   public void onClick(MouseButtonEvent p_455484_, boolean p_451923_) {
      if (this.componentClickHandler != null) {
         ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
            this.getFont(), (int)p_455484_.x(), (int)p_455484_.y()
         );
         this.visitLines(activetextcollector$clickablestylefinder);
         Style style = activetextcollector$clickablestylefinder.result();
         if (style != null) {
            this.componentClickHandler.accept(style);
            return;
         }
      }

      super.onClick(p_455484_, p_451923_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_270859_) {
   }

   protected final Font getFont() {
      return this.font;
   }

   @Override
   public void setMessage(Component p_426286_) {
      super.setMessage(p_426286_);
      this.setWidth(this.getFont().width(p_426286_.getVisualOrderText()));
   }

   public AbstractStringWidget setComponentClickHandler(@Nullable Consumer<Style> p_458809_) {
      this.componentClickHandler = p_458809_;
      return this;
   }
}
