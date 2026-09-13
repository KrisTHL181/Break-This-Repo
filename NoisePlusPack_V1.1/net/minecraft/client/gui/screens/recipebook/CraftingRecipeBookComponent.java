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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public CraftingRecipeBookComponent(AbstractCraftingMenu p_361849_) {
      super(p_361849_, TABS);
   }

   @Override
   protected boolean isCraftingSlot(Slot p_365848_) {
      return this.menu.getResultSlot() == p_365848_ || this.menu.getInputGridSlots().contains(p_365848_);
   }

   private boolean canDisplay(RecipeDisplay p_365142_) {
      int i = this.menu.getGridWidth();
      int j = this.menu.getGridHeight();

      return switch (p_365142_) {
         case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay -> i >= shapedcraftingrecipedisplay.width() && j >= shapedcraftingrecipedisplay.height();
         case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay -> i * j >= shapelesscraftingrecipedisplay.ingredients().size();
         default -> false;
      };
   }

   @Override
   protected void fillGhostRecipe(GhostSlots p_364903_, RecipeDisplay p_368451_, ContextMap p_369232_) {
      p_364903_.setResult(this.menu.getResultSlot(), p_369232_, p_368451_.result());
      switch (p_368451_) {
         case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay:
            List<Slot> list1 = this.menu.getInputGridSlots();
            PlaceRecipeHelper.placeRecipe(
               this.menu.getGridWidth(),
               this.menu.getGridHeight(),
               shapedcraftingrecipedisplay.width(),
               shapedcraftingrecipedisplay.height(),
               shapedcraftingrecipedisplay.ingredients(),
               (p_367286_, p_369760_, p_365619_, p_365975_) -> {
                  Slot slot = list1.get(p_369760_);
                  p_364903_.setInput(slot, p_369232_, p_367286_);
               }
            );
            break;
         case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay:
            label15: {
               List<Slot> list = this.menu.getInputGridSlots();
               int i = Math.min(shapelesscraftingrecipedisplay.ingredients().size(), list.size());

               for (int j = 0; j < i; j++) {
                  p_364903_.setInput(list.get(j), p_369232_, shapelesscraftingrecipedisplay.ingredients().get(j));
               }
               break label15;
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
   protected void selectMatchingRecipes(RecipeCollection p_361196_, StackedItemContents p_360883_) {
      p_361196_.selectRecipes(p_360883_, this::canDisplay);
   }
}
