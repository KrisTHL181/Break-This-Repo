package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;

public class ShulkerBoxScreen extends AbstractContainerScreen<ShulkerBoxMenu> {
   private static final Identifier CONTAINER_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/shulker_box.png");

   public ShulkerBoxScreen(final ShulkerBoxMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title, 176, 167);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
