package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipeHelper {
   static <T> void placeRecipe(
      final int gridWidth, final int gridHeight, final Recipe<?> recipe, final Iterable<T> entries, final PlaceRecipeHelper.Output<T> output
   ) {
      if (recipe instanceof ShapedRecipe shapedRecipe) {
         placeRecipe(gridWidth, gridHeight, shapedRecipe.getWidth(), shapedRecipe.getHeight(), entries, output);
      } else {
         placeRecipe(gridWidth, gridHeight, gridWidth, gridHeight, entries, output);
      }
   }

   static <T> void placeRecipe(
      final int gridWidth,
      final int gridHeight,
      final int recipeWidth,
      final int recipeHeight,
      final Iterable<T> entries,
      final PlaceRecipeHelper.Output<T> output
   ) {
      Iterator<T> iterator = entries.iterator();
      int gridIndex = 0;

      for (int gridYPos = 0; gridYPos < gridHeight; gridYPos++) {
         boolean shouldCenterRecipe = recipeHeight < gridHeight / 2.0F;
         int startPosCenterRecipe = Mth.floor(gridHeight / 2.0F - recipeHeight / 2.0F);
         if (shouldCenterRecipe && startPosCenterRecipe > gridYPos) {
            gridIndex += gridWidth;
            gridYPos++;
         }

         for (int gridXPos = 0; gridXPos < gridWidth; gridXPos++) {
            if (!iterator.hasNext()) {
               return;
            }

            shouldCenterRecipe = recipeWidth < gridWidth / 2.0F;
            startPosCenterRecipe = Mth.floor(gridWidth / 2.0F - recipeWidth / 2.0F);
            int totalRecipeWidthInGrid = recipeWidth;
            boolean addIngredientToSlot = gridXPos < recipeWidth;
            if (shouldCenterRecipe) {
               totalRecipeWidthInGrid = startPosCenterRecipe + recipeWidth;
               addIngredientToSlot = startPosCenterRecipe <= gridXPos && gridXPos < startPosCenterRecipe + recipeWidth;
            }

            if (addIngredientToSlot) {
               output.addItemToSlot(iterator.next(), gridIndex, gridXPos, gridYPos);
            } else if (totalRecipeWidthInGrid == gridXPos) {
               gridIndex += gridWidth - gridXPos;
               break;
            }

            gridIndex++;
         }
      }
   }

   @FunctionalInterface
   interface Output<T> {
      void addItemToSlot(T item, int gridIndex, int gridXPos, int gridYPos);
   }
}
