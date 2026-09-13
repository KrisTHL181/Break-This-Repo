package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.world.level.Level;

public interface SmithingRecipe extends Recipe<SmithingRecipeInput> {
   @Override
   default RecipeType<SmithingRecipe> getType() {
      return RecipeType.SMITHING;
   }

   @Override
   RecipeSerializer<? extends SmithingRecipe> getSerializer();

   default boolean matches(SmithingRecipeInput p_363830_, Level p_369755_) {
      return Ingredient.testOptionalIngredient(this.templateIngredient(), p_363830_.template())
         && this.baseIngredient().test(p_363830_.base())
         && Ingredient.testOptionalIngredient(this.additionIngredient(), p_363830_.addition());
   }

   Optional<Ingredient> templateIngredient();

   Ingredient baseIngredient();

   Optional<Ingredient> additionIngredient();

   @Override
   default RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.SMITHING;
   }
}
