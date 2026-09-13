package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlastingRecipe extends AbstractCookingRecipe {
   public BlastingRecipe(String p_251053_, CookingBookCategory p_249936_, Ingredient p_251550_, ItemStack p_251027_, float p_250843_, int p_249841_) {
      super(p_251053_, p_249936_, p_251550_, p_251027_, p_250843_, p_249841_);
   }

   @Override
   protected Item furnaceIcon() {
      return Items.BLAST_FURNACE;
   }

   @Override
   public RecipeSerializer<BlastingRecipe> getSerializer() {
      return RecipeSerializer.BLASTING_RECIPE;
   }

   @Override
   public RecipeType<BlastingRecipe> getType() {
      return RecipeType.BLASTING;
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return switch (this.category()) {
         case BLOCKS -> RecipeBookCategories.BLAST_FURNACE_BLOCKS;
         case FOOD, MISC -> RecipeBookCategories.BLAST_FURNACE_MISC;
      };
   }
}
