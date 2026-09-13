package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public class ContainerScreen extends AbstractContainerScreen<ChestMenu> {
   private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
   private final int containerRows;

   public ContainerScreen(final ChestMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
      this.containerRows = menu.getRowCount();
      this.inventoryLabelY = this.imageHeight - 94;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, this.containerRows * 18 + 17, 256, 256);
      graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo + this.containerRows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
   }
}
