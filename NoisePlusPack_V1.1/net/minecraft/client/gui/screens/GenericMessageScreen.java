package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GenericMessageScreen extends Screen {
   private @Nullable FocusableTextWidget textWidget;

   public GenericMessageScreen(Component p_334099_) {
      super(p_334099_);
   }

   @Override
   protected void init() {
      this.textWidget = this.addRenderableWidget(FocusableTextWidget.builder(this.title, this.font, 12).textWidth(this.font.width(this.title)).build());
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.textWidget != null) {
         this.textWidget.setPosition(this.width / 2 - this.textWidget.getWidth() / 2, this.height / 2 - 4);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean shouldNarrateNavigation() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics p_328774_, int p_328895_, int p_327693_, float p_328562_) {
      this.renderPanorama(p_328774_, p_328562_);
      this.renderBlurredBackground(p_328774_);
      this.renderMenuBackground(p_328774_);
   }
}
