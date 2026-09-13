package net.minecraft.client.multiplayer;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public class ClientRecipeContainer implements RecipeAccess {
   private final Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets;
   private final SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes;

   public ClientRecipeContainer(
      final Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets, final SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes
   ) {
      this.itemSets = itemSets;
      this.stonecutterRecipes = stonecutterRecipes;
   }

   @Override
   public RecipePropertySet propertySet(final ResourceKey<RecipePropertySet> id) {
      return this.itemSets.getOrDefault(id, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }
}
