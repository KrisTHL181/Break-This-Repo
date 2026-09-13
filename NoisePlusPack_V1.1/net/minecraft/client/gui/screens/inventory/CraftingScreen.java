package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CraftingScreen extends AbstractRecipeBookScreen<CraftingMenu> {
   private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

   public CraftingScreen(CraftingMenu p_98448_, Inventory p_98449_, Component p_98450_) {
      super(p_98448_, new CraftingRecipeBookComponent(p_98448_), p_98449_, p_98450_);
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = 29;
   }

   @Override
   protected ScreenPosition getRecipeBookButtonPosition() {
      return new ScreenPosition(this.leftPos + 5, this.height / 2 - 49);
   }

   @Override
   protected void renderBg(GuiGraphics p_283540_, float p_282132_, int p_283078_, int p_283647_) {
      int i = this.leftPos;
      int j = (this.height - this.imageHeight) / 2;
      p_283540_.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
   }
}
