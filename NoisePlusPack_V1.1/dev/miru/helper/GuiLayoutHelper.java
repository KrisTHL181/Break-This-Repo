package dev.miru.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;

public record GuiLayoutHelper(Minecraft mc) {
   public int centerX() {
      return this.mc.getWindow().getGuiScaledWidth() / 2;
   }

   public int centerY() {
      return this.mc.getWindow().getGuiScaledHeight() / 2;
   }

   public int centerXOffset(int offset) {
      return this.centerX() + offset;
   }

   public int centerYOffset(int offset) {
      return this.centerY() + offset;
   }

   @SafeVarargs
   public final <T extends AbstractWidget> void addAllWidgets(T... widgets) {
      for (AbstractWidget w : widgets) {
         this.mc.screen.addRenderableWidget(w);
      }
   }

   public int yOffset(int i) {
      return this.mc.screen.height + i;
   }

   public int xOffset(int i) {
      return this.mc.screen.width + i;
   }
}
