package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class RecipeDisplays {
   public static RecipeDisplay.Type<?> bootstrap(Registry<RecipeDisplay.Type<?>> p_368196_) {
      Registry.register(p_368196_, "crafting_shapeless", ShapelessCraftingRecipeDisplay.TYPE);
      Registry.register(p_368196_, "crafting_shaped", ShapedCraftingRecipeDisplay.TYPE);
      Registry.register(p_368196_, "furnace", FurnaceRecipeDisplay.TYPE);
      Registry.register(p_368196_, "stonecutter", StonecutterRecipeDisplay.TYPE);
      return Registry.register(p_368196_, "smithing", SmithingRecipeDisplay.TYPE);
   }
}
