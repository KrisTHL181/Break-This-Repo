package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SmeltingRecipe extends AbstractCookingRecipe {
   public SmeltingRecipe(String p_250200_, CookingBookCategory p_251114_, Ingredient p_250340_, ItemStack p_250306_, float p_249577_, int p_250030_) {
      super(p_250200_, p_251114_, p_250340_, p_250306_, p_249577_, p_250030_);
   }

   @Override
   protected Item furnaceIcon() {
      return Items.FURNACE;
   }

   @Override
   public RecipeSerializer<SmeltingRecipe> getSerializer() {
      return RecipeSerializer.SMELTING_RECIPE;
   }

   @Override
   public RecipeType<SmeltingRecipe> getType() {
      return RecipeType.SMELTING;
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return switch (this.category()) {
         case BLOCKS -> RecipeBookCategories.FURNACE_BLOCKS;
         case FOOD -> RecipeBookCategories.FURNACE_FOOD;
         case MISC -> RecipeBookCategories.FURNACE_MISC;
      };
   }
}
