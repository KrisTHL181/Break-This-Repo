package com.mojang.realmsclient.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsConfirmScreen extends RealmsScreen {
   protected BooleanConsumer callback;
   private final Component title1;
   private final Component title2;

   public RealmsConfirmScreen(BooleanConsumer p_88550_, Component p_88551_, Component p_88552_) {
      super(GameNarrator.NO_TITLE);
      this.callback = p_88550_;
      this.title1 = p_88551_;
      this.title2 = p_88552_;
   }

   @Override
   public void init() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_YES, p_88562_ -> this.callback.accept(true)).bounds(this.width / 2 - 105, row(9), 100, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_NO, p_88559_ -> this.callback.accept(false)).bounds(this.width / 2 + 5, row(9), 100, 20).build()
      );
   }

   @Override
   public void render(GuiGraphics p_282610_, int p_282200_, int p_283480_, float p_281259_) {
      super.render(p_282610_, p_282200_, p_283480_, p_281259_);
      p_282610_.drawCenteredString(this.font, this.title1, this.width / 2, row(3), -1);
      p_282610_.drawCenteredString(this.font, this.title2, this.width / 2, row(5), -1);
   }
}
