package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;

public class HopperScreen extends AbstractContainerScreen<HopperMenu> {
   private static final Identifier HOPPER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");

   public HopperScreen(final HopperMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title, 176, 133);
      this.inventoryLabelY = this.imageHeight - 94;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
