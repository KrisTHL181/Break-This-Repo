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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FurnaceRecipeBookComponent extends RecipeBookComponent<AbstractFurnaceMenu> {
   private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
      Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
   );
   private final Component recipeFilterName;

   public FurnaceRecipeBookComponent(AbstractFurnaceMenu p_360736_, Component p_363743_, List<RecipeBookComponent.TabInfo> p_365672_) {
      super(p_360736_, p_365672_);
      this.recipeFilterName = p_363743_;
   }

   @Override
   protected WidgetSprites getFilterButtonTextures() {
      return FILTER_SPRITES;
   }

   @Override
   protected boolean isCraftingSlot(Slot p_366927_) {
      return switch (p_366927_.index) {
         case 0, 1, 2 -> true;
         default -> false;
      };
   }

   @Override
   protected void fillGhostRecipe(GhostSlots p_365891_, RecipeDisplay p_366392_, ContextMap p_365727_) {
      p_365891_.setResult(this.menu.getResultSlot(), p_365727_, p_366392_.result());
      if (p_366392_ instanceof FurnaceRecipeDisplay furnacerecipedisplay) {
         p_365891_.setInput(this.menu.slots.get(0), p_365727_, furnacerecipedisplay.ingredient());
         Slot slot = this.menu.slots.get(1);
         if (slot.getItem().isEmpty()) {
            p_365891_.setInput(slot, p_365727_, furnacerecipedisplay.fuel());
         }
      }
   }

   @Override
   protected Component getRecipeFilterName() {
      return this.recipeFilterName;
   }

   @Override
   protected void selectMatchingRecipes(RecipeCollection p_361744_, StackedItemContents p_368936_) {
      p_361744_.selectRecipes(p_368936_, p_362331_ -> p_362331_ instanceof FurnaceRecipeDisplay);
   }
}
