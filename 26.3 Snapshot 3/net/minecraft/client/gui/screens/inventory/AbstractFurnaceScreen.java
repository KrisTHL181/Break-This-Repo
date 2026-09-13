package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.FurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;

public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceMenu> extends AbstractRecipeBookScreen<T> {
   private final Identifier texture;
   private final Identifier litProgressSprite;
   private final Identifier burnProgressSprite;

   public AbstractFurnaceScreen(
      final T menu,
      final Inventory inventory,
      final Component title,
      final Component recipeFilterName,
      final Identifier texture,
      final Identifier litProgressSprite,
      final Identifier burnProgressSprite,
      final List<RecipeBookComponent.TabInfo> tabInfos
   ) {
      super(menu, new FurnaceRecipeBookComponent(menu, recipeFilterName, tabInfos), inventory, title);
      this.texture = texture;
      this.litProgressSprite = litProgressSprite;
      this.burnProgressSprite = burnProgressSprite;
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
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = this.leftPos;
      int yo = this.topPos;
      graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      if (this.menu.isLit()) {
         int litSpriteHeight = 14;
         int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            this.litProgressSprite,
            14,
            14,
            0,
            14 - litProgressHeight,
            xo + 56,
            yo + 36 + 14 - litProgressHeight,
            14,
            litProgressHeight
         );
      }

      int burnSpriteWidth = 24;
      int burnProgressWidth = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.burnProgressSprite, 24, 16, 0, 0, xo + 79, yo + 34, burnProgressWidth, 16);
   }
}
