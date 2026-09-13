package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookTabButton extends ImageButton {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/tab"), Identifier.withDefaultNamespace("recipe_book/tab_selected")
   );
   public static final int WIDTH = 35;
   public static final int HEIGHT = 27;
   private final RecipeBookComponent.TabInfo tabInfo;
   private static final float ANIMATION_TIME = 15.0F;
   private float animationTime;
   private boolean selected = false;

   public RecipeBookTabButton(int p_451616_, int p_456922_, RecipeBookComponent.TabInfo p_368060_, Button.OnPress p_457659_) {
      super(p_451616_, p_456922_, 35, 27, SPRITES, p_457659_);
      this.tabInfo = p_368060_;
   }

   public void startAnimation(ClientRecipeBook p_370091_, boolean p_361650_) {
      RecipeCollection.CraftableStatus recipecollection$craftablestatus = p_361650_
         ? RecipeCollection.CraftableStatus.CRAFTABLE
         : RecipeCollection.CraftableStatus.ANY;

      for (RecipeCollection recipecollection : p_370091_.getCollection(this.tabInfo.category())) {
         for (RecipeDisplayEntry recipedisplayentry : recipecollection.getSelectedRecipes(recipecollection$craftablestatus)) {
            if (p_370091_.willHighlight(recipedisplayentry.id())) {
               this.animationTime = 15.0F;
               return;
            }
         }
      }
   }

   @Override
   public void renderContents(GuiGraphics p_456514_, int p_458455_, int p_460676_, float p_456377_) {
      if (this.animationTime > 0.0F) {
         float f = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         p_456514_.pose().pushMatrix();
         p_456514_.pose().translate(this.getX() + 8, this.getY() + 12);
         p_456514_.pose().scale(1.0F, f);
         p_456514_.pose().translate(-(this.getX() + 8), -(this.getY() + 12));
      }

      Identifier identifier = this.sprites.get(true, this.selected);
      int i = this.getX();
      if (this.selected) {
         i -= 2;
      }

      p_456514_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, this.getY(), this.width, this.height);
      this.renderIcon(p_456514_);
      if (this.animationTime > 0.0F) {
         p_456514_.pose().popMatrix();
         this.animationTime -= p_456377_;
      }
   }

   @Override
   protected void handleCursor(GuiGraphics p_451960_) {
      if (!this.selected) {
         super.handleCursor(p_451960_);
      }
   }

   private void renderIcon(GuiGraphics p_281802_) {
      int i = this.selected ? -2 : 0;
      if (this.tabInfo.secondaryIcon().isPresent()) {
         p_281802_.renderFakeItem(this.tabInfo.primaryIcon(), this.getX() + 3 + i, this.getY() + 5);
         p_281802_.renderFakeItem(this.tabInfo.secondaryIcon().get(), this.getX() + 14 + i, this.getY() + 5);
      } else {
         p_281802_.renderFakeItem(this.tabInfo.primaryIcon(), this.getX() + 9 + i, this.getY() + 5);
      }
   }

   public ExtendedRecipeBookCategory getCategory() {
      return this.tabInfo.category();
   }

   public boolean updateVisibility(ClientRecipeBook p_100450_) {
      List<RecipeCollection> list = p_100450_.getCollection(this.tabInfo.category());
      this.visible = false;

      for (RecipeCollection recipecollection : list) {
         if (recipecollection.hasAnySelected()) {
            this.visible = true;
            break;
         }
      }

      return this.visible;
   }

   public void select() {
      this.selected = true;
   }

   public void unselect() {
      this.selected = false;
   }
}
