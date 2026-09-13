package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

public class CraftingRecipeBookComponent extends RecipeBookComponent<AbstractCraftingMenu> {
   private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
      Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
      Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
      Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted")
   );
   private static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");
   private static final List<RecipeBookComponent.TabInfo> TABS = List.of(
      new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.CRAFTING),
      new RecipeBookComponent.TabInfo(Items.IRON_AXE, Items.GOLDEN_SWORD, RecipeBookCategories.CRAFTING_EQUIPMENT),
      new RecipeBookComponent.TabInfo(Items.BRICKS, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS),
      new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.APPLE, RecipeBookCategories.CRAFTING_MISC),
      new RecipeBookComponent.TabInfo(Items.REDSTONE, RecipeBookCategories.CRAFTING_REDSTONE)
   );

   public CraftingRecipeBookComponent(final AbstractCraftingMenu menu) {
      super(menu, TABS);
   }

   @Override
   protected boolean isCraftingSlot(final Slot slot) {
      return this.menu.getResultSlot() == slot || this.menu.getInputGridSlots().contains(slot);
   }

   private boolean canDisplay(final RecipeDisplay display) {
      int gridWidth = this.menu.getGridWidth();
      int gridHeight = this.menu.getGridHeight();

      return switch (display) {
         case ShapedCraftingRecipeDisplay shaped -> gridWidth >= shaped.width() && gridHeight >= shaped.height();
         case ShapelessCraftingRecipeDisplay shapeless -> gridWidth * gridHeight >= shapeless.ingredients().size();
         default -> false;
      };
   }

   @Override
   protected void fillGhostRecipe(final GhostSlots ghostSlots, final RecipeDisplay recipe, final ContextMap context) {
      ghostSlots.setResult(this.menu.getResultSlot(), context, recipe.result());
      switch (recipe) {
         case ShapedCraftingRecipeDisplay shaped: {
            List<Slot> inputSlots = this.menu.getInputGridSlots();
            PlaceRecipeHelper.placeRecipe(
               this.menu.getGridWidth(),
               this.menu.getGridHeight(),
               shaped.width(),
               shaped.height(),
               shaped.ingredients(),
               (ingredient, gridIndex, gridXPos, gridYPos) -> {
                  Slot slot = inputSlots.get(gridIndex);
                  ghostSlots.setInput(slot, context, ingredient);
               }
            );
            break;
         }
         case ShapelessCraftingRecipeDisplay shapeless: {
            label15: {
               List<Slot> inputSlots = this.menu.getInputGridSlots();
               int slotCount = Math.min(shapeless.ingredients().size(), inputSlots.size());

               for (int i = 0; i < slotCount; i++) {
                  ghostSlots.setInput(inputSlots.get(i), context, shapeless.ingredients().get(i));
               }
               break label15;
            }
         }
         default:
      }
   }

   @Override
   protected WidgetSprites getFilterButtonTextures() {
      return FILTER_BUTTON_SPRITES;
   }

   @Override
   protected Component getRecipeFilterName() {
      return ONLY_CRAFTABLES_TOOLTIP;
   }

   @Override
   protected void selectMatchingRecipes(final RecipeCollection collection, final StackedItemContents stackedContents) {
      collection.selectRecipes(stackedContents, this::canDisplay);
   }
}
