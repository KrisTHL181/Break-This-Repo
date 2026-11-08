package net.minecraft.client.gui.screens.recipebook;

import net.minecraft.world.item.crafting.display.RecipeDisplay;

public interface RecipeUpdateListener {
   void recipesUpdated();

   void fillGhostRecipe(RecipeDisplay display);
}
