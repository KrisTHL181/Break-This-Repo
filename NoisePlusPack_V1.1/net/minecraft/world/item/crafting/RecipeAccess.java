package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceKey;

public interface RecipeAccess {
   RecipePropertySet propertySet(ResourceKey<RecipePropertySet> var1);

   SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes();
}
