package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMountInventoryScreen<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
   protected final int inventoryColumns;
   protected float xMouse;
   protected float yMouse;
   protected final LivingEntity mount;

   public AbstractMountInventoryScreen(final T menu, final Inventory inventory, final Component title, final int inventoryColumns, final LivingEntity mount) {
      super(menu, inventory, title);
      this.inventoryColumns = inventoryColumns;
      this.mount = mount;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, this.getBackgroundTextureLocation(), xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if (this.inventoryColumns > 0 && this.getChestSlotsSpriteLocation() != null) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getChestSlotsSpriteLocation(), 90, 54, 0, 0, xo + 79, yo + 17, this.inventoryColumns * 18, 54);
      }

      if (this.shouldRenderSaddleSlot()) {
         this.extractSlot(graphics, xo + 7, yo + 35 - 18);
      }

      if (this.shouldRenderArmorSlot()) {
         this.extractSlot(graphics, xo + 7, yo + 35);
      }

      InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, xo + 26, yo + 18, xo + 78, yo + 70, 17, 0.25F, this.xMouse, this.yMouse, this.mount);
   }

   protected void extractSlot(final GuiGraphicsExtractor graphics, final int x, final int y) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSlotSpriteLocation(), x, y, 18, 18);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      this.xMouse = mouseX;
      this.yMouse = mouseY;
      super.extractRenderState(graphics, mouseX, mouseY, a);
   }

   protected abstract Identifier getBackgroundTextureLocation();

   protected abstract Identifier getSlotSpriteLocation();

   protected abstract @Nullable Identifier getChestSlotsSpriteLocation();

   protected abstract boolean shouldRenderSaddleSlot();

   protected abstract boolean shouldRenderArmorSlot();
}
