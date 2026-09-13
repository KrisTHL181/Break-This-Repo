package net.minecraft.client.multiplayer;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeContainer implements RecipeAccess {
   private final Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets;
   private final SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes;

   public ClientRecipeContainer(Map<ResourceKey<RecipePropertySet>, RecipePropertySet> p_367500_, SelectableRecipe.SingleInputSet<StonecutterRecipe> p_364193_) {
      this.itemSets = p_367500_;
      this.stonecutterRecipes = p_364193_;
   }

   @Override
   public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> p_368651_) {
      return this.itemSets.getOrDefault(p_368651_, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }
}
