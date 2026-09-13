package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementInfo {
   public static final int EMPTY_SLOT = -1;
   public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
   private final List<Ingredient> ingredients;
   private final IntList slotsToIngredientIndex;

   private PlacementInfo(final List<Ingredient> ingredients, final IntList slotsToIngredientIndex) {
      this.ingredients = ingredients;
      this.slotsToIngredientIndex = slotsToIngredientIndex;
   }

   public static PlacementInfo create(final Ingredient ingredient) {
      return ingredient.isEmpty() ? NOT_PLACEABLE : new PlacementInfo(List.of(ingredient), IntList.of(0));
   }

   public static PlacementInfo createFromOptionals(final List<Optional<Ingredient>> ingredients) {
      int ingredientCount = ingredients.size();
      List<Ingredient> presentIngredients = new ArrayList<>(ingredientCount);
      IntList slotsToIngredientIndex = new IntArrayList(ingredientCount);
      int placementIndex = 0;

      for (Optional<Ingredient> maybeIngredient : ingredients) {
         if (maybeIngredient.isPresent()) {
            Ingredient ingredient = maybeIngredient.get();
            if (ingredient.isEmpty()) {
               return NOT_PLACEABLE;
            }

            presentIngredients.add(ingredient);
            slotsToIngredientIndex.add(placementIndex++);
         } else {
            slotsToIngredientIndex.add(-1);
         }
      }

      return new PlacementInfo(presentIngredients, slotsToIngredientIndex);
   }

   public static PlacementInfo create(final List<Ingredient> ingredients) {
      int ingredientCount = ingredients.size();
      IntList slotsToIngredientIndex = new IntArrayList(ingredientCount);

      for (int i = 0; i < ingredientCount; i++) {
         Ingredient ingredient = ingredients.get(i);
         if (ingredient.isEmpty()) {
            return NOT_PLACEABLE;
         }

         slotsToIngredientIndex.add(i);
      }

      return new PlacementInfo(ingredients, slotsToIngredientIndex);
   }

   public IntList slotsToIngredientIndex() {
      return this.slotsToIngredientIndex;
   }

   public List<Ingredient> ingredients() {
      return this.ingredients;
   }

   public boolean isImpossibleToPlace() {
      return this.slotsToIngredientIndex.isEmpty();
   }
}
