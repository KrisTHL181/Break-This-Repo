package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GrindstoneScreen extends AbstractContainerScreen<GrindstoneMenu> {
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/grindstone/error");
   private static final Identifier GRINDSTONE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/grindstone.png");

   public GrindstoneScreen(GrindstoneMenu p_98782_, Inventory p_98783_, Component p_98784_) {
      super(p_98782_, p_98783_, p_98784_);
   }

   @Override
   public void render(GuiGraphics p_283326_, int p_281847_, int p_283310_, float p_283486_) {
      super.render(p_283326_, p_281847_, p_283310_, p_283486_);
      this.renderTooltip(p_283326_, p_281847_, p_283310_);
   }

   @Override
   protected void renderBg(GuiGraphics p_281991_, float p_282138_, int p_282937_, int p_281956_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_281991_.blit(RenderPipelines.GUI_TEXTURED, GRINDSTONE_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(2).hasItem()) {
         p_281991_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + 92, j + 31, 28, 21);
      }
   }
}
