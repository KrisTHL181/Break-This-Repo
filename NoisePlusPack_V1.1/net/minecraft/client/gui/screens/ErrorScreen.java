package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ErrorScreen extends Screen {
   private final Component message;

   public ErrorScreen(Component p_96049_, Component p_96050_) {
      super(p_96049_);
      this.message = p_96050_;
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_280801_ -> this.minecraft.setScreen(null)).bounds(this.width / 2 - 100, 140, 200, 20).build()
      );
   }

   @Override
   public void render(GuiGraphics p_281469_, int p_96053_, int p_96054_, float p_96055_) {
      super.render(p_281469_, p_96053_, p_96054_, p_96055_);
      p_281469_.drawCenteredString(this.font, this.title, this.width / 2, 90, -1);
      p_281469_.drawCenteredString(this.font, this.message, this.width / 2, 110, -1);
   }

   @Override
   public void renderBackground(GuiGraphics p_297607_, int p_297440_, int p_299518_, float p_300407_) {
      p_297607_.fillGradient(0, 0, this.width, this.height, -12574688, -11530224);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
