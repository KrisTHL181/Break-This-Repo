package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMountInventoryScreen<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
   protected final int inventoryColumns;
   protected float xMouse;
   protected float yMouse;
   protected LivingEntity mount;

   public AbstractMountInventoryScreen(T p_452079_, Inventory p_455522_, Component p_457705_, int p_460624_, LivingEntity p_452165_) {
      super(p_452079_, p_455522_, p_457705_);
      this.inventoryColumns = p_460624_;
      this.mount = p_452165_;
   }

   @Override
   protected void renderBg(GuiGraphics p_459859_, float p_451528_, int p_453264_, int p_460327_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_459859_.blit(RenderPipelines.GUI_TEXTURED, this.getBackgroundTextureLocation(), i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if (this.inventoryColumns > 0 && this.getChestSlotsSpriteLocation() != null) {
         p_459859_.blitSprite(RenderPipelines.GUI_TEXTURED, this.getChestSlotsSpriteLocation(), 90, 54, 0, 0, i + 79, j + 17, this.inventoryColumns * 18, 54);
      }

      if (this.shouldRenderSaddleSlot()) {
         this.drawSlot(p_459859_, i + 7, j + 35 - 18);
      }

      if (this.shouldRenderArmorSlot()) {
         this.drawSlot(p_459859_, i + 7, j + 35);
      }

      InventoryScreen.renderEntityInInventoryFollowsMouse(p_459859_, i + 26, j + 18, i + 78, j + 70, 17, 0.25F, this.xMouse, this.yMouse, this.mount);
   }

   protected void drawSlot(GuiGraphics p_451754_, int p_458050_, int p_456697_) {
      p_451754_.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSlotSpriteLocation(), p_458050_, p_456697_, 18, 18);
   }

   @Override
   public void render(GuiGraphics p_455825_, int p_451347_, int p_455617_, float p_456356_) {
      this.xMouse = p_451347_;
      this.yMouse = p_455617_;
      super.render(p_455825_, p_451347_, p_455617_, p_456356_);
      this.renderTooltip(p_455825_, p_451347_, p_455617_);
   }

   protected abstract Identifier getBackgroundTextureLocation();

   protected abstract Identifier getSlotSpriteLocation();

   protected abstract @Nullable Identifier getChestSlotsSpriteLocation();

   protected abstract boolean shouldRenderSaddleSlot();

   protected abstract boolean shouldRenderArmorSlot();
}
