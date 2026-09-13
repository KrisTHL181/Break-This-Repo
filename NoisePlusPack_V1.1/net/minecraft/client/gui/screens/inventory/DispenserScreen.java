package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DispenserScreen extends AbstractContainerScreen<DispenserMenu> {
   private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

   public DispenserScreen(DispenserMenu p_98685_, Inventory p_98686_, Component p_98687_) {
      super(p_98685_, p_98686_, p_98687_);
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void render(GuiGraphics p_283282_, int p_282467_, int p_282129_, float p_281965_) {
      super.render(p_283282_, p_282467_, p_282129_, p_281965_);
      this.renderTooltip(p_283282_, p_282467_, p_282129_);
   }

   @Override
   protected void renderBg(GuiGraphics p_283137_, float p_282476_, int p_281600_, int p_283194_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_283137_.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
