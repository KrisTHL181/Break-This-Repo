package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FittingMultiLineTextWidget extends AbstractTextAreaWidget {
   private final Font font;
   private final MultiLineTextWidget multilineWidget;

   public FittingMultiLineTextWidget(int p_289785_, int p_289777_, int p_289760_, int p_289801_, Component p_289788_, Font p_289781_) {
      super(p_289785_, p_289777_, p_289760_, p_289801_, p_289788_);
      this.font = p_289781_;
      this.multilineWidget = new MultiLineTextWidget(p_289788_, p_289781_).setMaxWidth(this.getWidth() - this.totalInnerPadding());
   }

   @Override
   public void setWidth(int p_289765_) {
      super.setWidth(p_289765_);
      this.multilineWidget.setMaxWidth(this.getWidth() - this.totalInnerPadding());
   }

   @Override
   protected int getInnerHeight() {
      return this.multilineWidget.getHeight();
   }

   public void minimizeHeight() {
      if (!this.showingScrollBar()) {
         this.setHeight(this.getInnerHeight() + this.totalInnerPadding());
      }
   }

   @Override
   protected double scrollRate() {
      return 9.0;
   }

   @Override
   protected void renderBackground(GuiGraphics p_289758_) {
      super.renderBackground(p_289758_);
   }

   public boolean showingScrollBar() {
      return super.scrollbarVisible();
   }

   @Override
   protected void renderContents(GuiGraphics p_289766_, int p_289790_, int p_289786_, float p_289767_) {
      p_289766_.pose().pushMatrix();
      p_289766_.pose().translate(this.getInnerLeft(), this.getInnerTop());
      this.multilineWidget.render(p_289766_, p_289790_, p_289786_, p_289767_);
      p_289766_.pose().popMatrix();
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_289784_) {
      p_289784_.add(NarratedElementType.TITLE, this.getMessage());
   }

   @Override
   public void setMessage(Component p_392762_) {
      super.setMessage(p_392762_);
      this.multilineWidget.setMessage(p_392762_);
   }
}
