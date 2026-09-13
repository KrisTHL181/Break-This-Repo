package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

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

   public RecipeBookTabButton(final int x, final int y, final RecipeBookComponent.TabInfo tabInfo, final Button.OnPress onPress) {
      super(x, y, 35, 27, SPRITES, onPress);
      this.tabInfo = tabInfo;
   }

   public void startAnimation(final ClientRecipeBook recipeBook, final boolean isFiltering) {
      RecipeCollection.CraftableStatus recipesToShow = isFiltering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY;

      for (RecipeCollection recipeCollection : recipeBook.getCollection(this.tabInfo.category())) {
         for (RecipeDisplayEntry recipe : recipeCollection.getSelectedRecipes(recipesToShow)) {
            if (recipeBook.willHighlight(recipe.id())) {
               this.animationTime = 15.0F;
               return;
            }
         }
      }
   }

   @Override
   public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      if (this.animationTime > 0.0F) {
         float squeeze = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         graphics.pose().pushMatrix();
         graphics.pose().translate(this.getX() + 8, this.getY() + 12);
         graphics.pose().scale(1.0F, squeeze);
         graphics.pose().translate(-(this.getX() + 8), -(this.getY() + 12));
      }

      Identifier sprite = this.sprites.get(true, this.selected);
      int xPos = this.getX();
      if (this.selected) {
         xPos -= 2;
      }

      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xPos, this.getY(), this.width, this.height);
      this.extractIcon(graphics);
      if (this.animationTime > 0.0F) {
         graphics.pose().popMatrix();
         this.animationTime -= a;
      }
   }

   @Override
   protected void handleCursor(final GuiGraphicsExtractor graphics) {
      if (!this.selected) {
         super.handleCursor(graphics);
      }
   }

   private void extractIcon(final GuiGraphicsExtractor graphics) {
      int moveLeft = this.selected ? -2 : 0;
      if (this.tabInfo.secondaryIcon().isPresent()) {
         graphics.fakeItem(this.tabInfo.primaryIcon(), this.getX() + 3 + moveLeft, this.getY() + 5);
         graphics.fakeItem(this.tabInfo.secondaryIcon().get(), this.getX() + 14 + moveLeft, this.getY() + 5);
      } else {
         graphics.fakeItem(this.tabInfo.primaryIcon(), this.getX() + 9 + moveLeft, this.getY() + 5);
      }
   }

   public ExtendedRecipeBookCategory getCategory() {
      return this.tabInfo.category();
   }

   public boolean updateVisibility(final ClientRecipeBook book) {
      List<RecipeCollection> collections = book.getCollection(this.tabInfo.category());
      this.visible = false;

      for (RecipeCollection collection : collections) {
         if (collection.hasAnySelected()) {
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
