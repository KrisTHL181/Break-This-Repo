package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SmokingRecipe extends AbstractCookingRecipe {
   public SmokingRecipe(String p_249312_, CookingBookCategory p_251017_, Ingredient p_252345_, ItemStack p_250002_, float p_250535_, int p_251222_) {
      super(p_249312_, p_251017_, p_252345_, p_250002_, p_250535_, p_251222_);
   }

   @Override
   protected Item furnaceIcon() {
      return Items.SMOKER;
   }

   @Override
   public RecipeType<SmokingRecipe> getType() {
      return RecipeType.SMOKING;
   }

   @Override
   public RecipeSerializer<SmokingRecipe> getSerializer() {
      return RecipeSerializer.SMOKING_RECIPE;
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.SMOKER_FOOD;
   }
}
