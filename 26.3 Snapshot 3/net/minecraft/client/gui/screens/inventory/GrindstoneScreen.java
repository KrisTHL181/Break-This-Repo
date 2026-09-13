package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;

public class GrindstoneScreen extends AbstractContainerScreen<GrindstoneMenu> {
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/grindstone/error");
   private static final Identifier GRINDSTONE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/grindstone.png");

   public GrindstoneScreen(final GrindstoneMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, GRINDSTONE_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(2).hasItem()) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + 92, yo + 31, 28, 21);
      }
   }
}
