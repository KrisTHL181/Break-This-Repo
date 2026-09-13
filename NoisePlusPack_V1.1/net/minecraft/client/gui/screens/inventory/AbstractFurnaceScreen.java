package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.FurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceMenu> extends AbstractRecipeBookScreen<T> {
   private final Identifier texture;
   private final Identifier litProgressSprite;
   private final Identifier burnProgressSprite;

   public AbstractFurnaceScreen(
      T p_97825_,
      Inventory p_97827_,
      Component p_97828_,
      Component p_364165_,
      Identifier p_456509_,
      Identifier p_456045_,
      Identifier p_450656_,
      List<RecipeBookComponent.TabInfo> p_367246_
   ) {
      super(p_97825_, new FurnaceRecipeBookComponent(p_97825_, p_364165_, p_367246_), p_97827_, p_97828_);
      this.texture = p_456509_;
      this.litProgressSprite = p_456045_;
      this.burnProgressSprite = p_450656_;
   }

   @Override
   public void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   protected ScreenPosition getRecipeBookButtonPosition() {
      return new ScreenPosition(this.leftPos + 20, this.height / 2 - 49);
   }

   @Override
   protected void renderBg(GuiGraphics p_282928_, float p_281631_, int p_281252_, int p_281891_) {
      int i = this.leftPos;
      int j = this.topPos;
      p_282928_.blit(RenderPipelines.GUI_TEXTURED, this.texture, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if (this.menu.isLit()) {
         int k = 14;
         int l = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
         p_282928_.blitSprite(RenderPipelines.GUI_TEXTURED, this.litProgressSprite, 14, 14, 0, 14 - l, i + 56, j + 36 + 14 - l, 14, l);
      }

      int i1 = 24;
      int j1 = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
      p_282928_.blitSprite(RenderPipelines.GUI_TEXTURED, this.burnProgressSprite, 24, 16, 0, 0, i + 79, j + 34, j1, 16);
   }
}
