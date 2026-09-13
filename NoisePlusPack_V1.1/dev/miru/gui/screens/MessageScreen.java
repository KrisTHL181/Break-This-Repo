package dev.miru.gui.screens;

import dev.miru.helper.KitUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class MessageScreen extends Screen {
   private Screen parent;
   private String message;

   public MessageScreen(String message, Screen parent) {
      super(Component.literal(message));
      this.parent = parent;
      this.message = message;
   }

   @Override
   public void init() {
      super.init();
      Button btnConform = KitUtil.button(
         CommonComponents.GUI_CANCEL, null, b -> this.minecraft.setScreen(this.parent), 100, 20, this.width / 2 - 50, this.height / 2 + 20
      );
      this.addRenderableWidget(btnConform);
   }

   @Override
   public void render(GuiGraphics gg, int mx, int my, float t) {
      super.render(gg, mx, my, t);
      gg.drawString(this.font, this.message, this.width / 2 - this.font.width(this.message) / 2, this.height / 2 - 10, -1);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
