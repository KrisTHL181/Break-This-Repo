package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface CraftingRecipe extends Recipe<CraftingInput> {
   @Override
   default RecipeType<CraftingRecipe> getType() {
      return RecipeType.CRAFTING;
   }

   @Override
   RecipeSerializer<? extends CraftingRecipe> getSerializer();

   CraftingBookCategory category();

   default NonNullList<ItemStack> getRemainingItems(CraftingInput p_367909_) {
      return defaultCraftingReminder(p_367909_);
   }

   static NonNullList<ItemStack> defaultCraftingReminder(CraftingInput p_368314_) {
      NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_368314_.size(), ItemStack.EMPTY);

      for (int i = 0; i < nonnulllist.size(); i++) {
         Item item = p_368314_.getItem(i).getItem();
         nonnulllist.set(i, item.getCraftingRemainder());
      }

      return nonnulllist;
   }

   @Override
   default RecipeBookCategory recipeBookCategory() {
      return switch (this.category()) {
         case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
         case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
         case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
         case MISC -> RecipeBookCategories.CRAFTING_MISC;
      };
   }
}
