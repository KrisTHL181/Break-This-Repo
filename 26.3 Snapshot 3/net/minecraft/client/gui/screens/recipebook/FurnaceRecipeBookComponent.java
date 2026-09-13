package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

public class FurnaceRecipeBookComponent extends RecipeBookComponent<AbstractFurnaceMenu> {
   private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
   );
   private final Component recipeFilterName;

   public FurnaceRecipeBookComponent(final AbstractFurnaceMenu menu, final Component recipeFilterName, final List<RecipeBookComponent.TabInfo> tabInfos) {
      super(menu, tabInfos);
      this.recipeFilterName = recipeFilterName;
   }

   @Override
   protected WidgetSprites getFilterButtonTextures() {
      return FILTER_SPRITES;
   }

   @Override
   protected boolean isCraftingSlot(final Slot slot) {
      return switch (slot.index) {
         case 0, 1, 2 -> true;
         default -> false;
      };
   }

   @Override
   protected void fillGhostRecipe(final GhostSlots ghostSlots, final RecipeDisplay recipe, final ContextMap context) {
      ghostSlots.setResult(this.menu.getResultSlot(), context, recipe.result());
      if (recipe instanceof FurnaceRecipeDisplay furnaceRecipe) {
         ghostSlots.setInput(this.menu.slots.get(0), context, furnaceRecipe.ingredient());
         Slot fuelSlot = this.menu.slots.get(1);
         if (fuelSlot.getItem().isEmpty()) {
            ghostSlots.setInput(fuelSlot, context, furnaceRecipe.fuel());
         }
      }
   }

   @Override
   protected Component getRecipeFilterName() {
      return this.recipeFilterName;
   }

   @Override
   protected void selectMatchingRecipes(final RecipeCollection collection, final StackedItemContents stackedContents) {
      collection.selectRecipes(stackedContents, display -> display instanceof FurnaceRecipeDisplay);
   }
}
